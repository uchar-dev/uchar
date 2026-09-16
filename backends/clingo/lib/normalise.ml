(* The unit-normalisation lowering pass — the clingo backend's pre-step
   (PRIORITIZED-DEFAULTS-EVOLUTIE §8.1–8.2, Q9; the unit algebra IR-SPEC
   ch. 2.5; the lowering specified in spec/ASP-TRANSLATION.md S6).

   THE WIRE IS UNTOUCHED. Source units stay on disk; this is an internal
   wire→wire transform on the resolved model, applied inside [Translate.translate]
   BEFORE emission. ref/ and Drools keep replaying the source form; only the
   clingo leg canonicalises.

   WHAT IT DOES. clingo encodes numeric values as scaled machine integers and
   carries no conversion factor, so its two unit fences —
   [Translate.units_identical_ok] on the additive/clamp/fold join and on the
   write boundary — refuse ANY factor≠1 restatement (`non-identical units …`).
   This pass lifts that embargo for the provable case: it rewrites the numeric
   literals of a write rule whose TARGET attribute is declared in its stelsel's
   FINEST base unit, restating each coarser-unit literal INTO that finest unit.

   CANONICAL = THE FINEST BASE UNIT (PRIORITIZED-DEFAULTS-EVOLUTIE §8.1).
   Restating to a finer unit MULTIPLIES by an integer (2,5 km ⇒ 2500 m, ×1000;
   2 jaar ⇒ 24 maand, ×12) — exact and integer-encodable, the property clingo
   needs. Restating to a
   COARSER unit would divide (possibly non-terminating), which the integer
   encoding cannot hold; so the pass normalises ONLY toward the finest, and only
   when the write target already IS the finest — otherwise it leaves the rule to
   the standing loud refusal.

   PROOF OBLIGATIONS, discharged in [finest_of_stelsel] and fault-loud:
     1. every declared factor has an exact short-decimal reading
        (Arith.q_of_wire_number — the same recovery ref/ uses; a factor with no
        short reading faults, never a silent binary-double tail);
     2. every declared unit restates into the finest by a positive INTEGER
        (a stelsel that admits no such finest — e.g. sibling factors 2 and 3 —
        faults, keeping the refusal rather than silently picking a divide-down
        canonical).

   THE DIFFERENTIAL GUARANTEE NEEDS NO NEW HARNESS. Gate 6 already demands that
   clingo-over-(now-normalised)-wire hit the SAME absolute expectations gate 5
   pins for ref-over-source. Because [Model.check] scales the expected value by
   the target attribute's declared decimals (the ORIGINAL, un-normalised doc)
   and ref restates on the write boundary (World.collapse), a normalised literal
   that scales exactly onto the finest-unit target reproduces ref's value bit for
   bit: ref ≡ clingo remains the proof, now over the factor≠1 goldens too.

   IDENTITY ON FACTOR-1 WIRES. The pass only ever touches a numeric literal that
   (a) carries a plain unit of the target's stelsel and (b) is coarser than that
   stelsel's finest. A rule that already translates does so with identical units,
   so no such literal is present — the transform is the identity and gate 6 stays
   green over the whole existing corpus (the no-drift proof). *)

module Value = Uchar_ref.Value
module Arith = Uchar_ref.Arith
module Checks = Ir_v0.Checks
open Ir_v0.Ast

let fault = Value.fault

let unit_abbr (u : eenheid) = Option.value u.afkorting ~default:"?"

(* the plain (non-rate) declared units of a stelsel — its base and derived
   spellings plus the alias table; a rate (noemer set) never enters stelsel
   normalisation (PRIORITIZED-DEFAULTS-EVOLUTIE §8.3: its numerator/denominator
   normalise per their own stelsels, out of scope here and left to the existing
   refusal). *)
let plain_units (s : eenheidstelsel) : eenheid list =
  List.filter_map (fun (_, u) -> u) (s.eenheden @ s.aliassen)
  |> List.filter (fun (u : eenheid) -> u.noemer = None)

(* FINEST-UNIT DERIVATION. Returns the finest base unit u_min of [s] and the
   total factor-to-finest lookup factor(u → u_min). Faults loudly (never a
   silent pick) on the two failure modes above. *)
let finest_of_stelsel (s : eenheidstelsel) : eenheid * (eenheid -> Q.t) =
  let sname = Option.value s.es_naam ~default:"?" in
  match plain_units s with
  | [] -> fault "unit stelsel %S declares no plain base unit to normalise" sname
  | first :: rest ->
      (* the finest is the unit with the smallest factor-to-declared-canonical:
         the fewer canonical units it equals, the finer it is (kg=1 < t=1000,
         maand=1 < jaar=12). factor(u) is recovered exactly (proof obligation 1). *)
      let finest =
        List.fold_left
          (fun acc u -> if Q.lt (Arith.factor u) (Arith.factor acc) then u else acc)
          first rest
      in
      let ffin = Arith.factor finest in
      let to_finest (u : eenheid) : Q.t = Q.div (Arith.factor u) ffin in
      (* proof obligation 2: multiply-only INTO the finest, exact integer *)
      List.iter
        (fun u ->
          let r = to_finest u in
          if Q.sign r <= 0 || not (Z.equal (Q.den r) Z.one) then
            fault
              "unit stelsel %S admits no finest base unit: %S restates into %S by %s, not a \
               positive integer (clingo's integer encoding cannot hold a divide-down canonical)"
              sname (unit_abbr u) (unit_abbr finest) (Q.to_string r))
        (plain_units s);
      (finest, to_finest)

(* the target attribute's declared unit (None when unit-free, non-numeric, or
   unknown — every such target is left untouched).

   The declaration is the EFFECTIVE one (ch. 2.2): a write whose subject is a
   SUBTYPE reaches a slot its ancestor declares, and off the raw map that slot
   is simply absent, so this answered None, the pass left the rule untouched and
   the additive write fence then refused a document the verifier accepts and ref
   answers (measured on `golden/jutterswet-muntweging`: clingo "non-identical
   units under an additive join are not translated", ref 1002,00, Drools the
   same). Untouched-on-unknown is still the rule for a target this document
   really does not declare; what changed is which declarations count as the
   target's. Same defect class as [Translate.attr_temporal_decl]'s raw read. *)
let target_unit (doc : document) ~ot ~attr : eenheid option =
  match Checks.effective_ot doc ot with
  | Some otd -> (
      match Checks.find_ci otd.attributen attr with
      | Some (Some (Numeriek { eenheid; _ } | Percentage { eenheid; _ })) -> eenheid
      | _ -> None)
  | None -> None

let decimals_of_lexeme lex =
  match String.index_opt lex '.' with Some i -> String.length lex - i - 1 | None -> 0

(* is [u] a plain unit of stelsel [sname]? *)
let unit_in_stelsel ~sname (u : eenheid) =
  u.noemer = None
  && (match u.stelsel with Some s -> Checks.norm s = Checks.norm sname | None -> false)

(* restate a stelsel-[sname] numeric literal INTO [finest_eenheid] by the exact
   integer [to_finest]; every other node is returned unchanged. A factor-1
   literal keeps its value and only adopts the shared finest record, so every
   operand of the join reads ONE identical unit — the fence tests record
   equality, not mere convertibility. *)
let restate_lit ~sname ~finest_eenheid ~to_finest (e : expr) : expr =
  match e with
  | Literal ({ waarde = Some lex; typ = Some (Numeriek n); _ } as lit)
    when (match n.eenheid with Some u -> unit_in_stelsel ~sname u | None -> false) ->
      let u = Option.get n.eenheid in
      let r = to_finest u in
      if Q.equal r Q.one then
        Literal { lit with typ = Some (Numeriek { n with eenheid = Some finest_eenheid }) }
      else begin
        let lex' = Value.lexeme_of_q (Q.mul (Value.q_of_lexeme lex) r) in
        let d = decimals_of_lexeme lex' in
        Literal
          { lit with
            waarde = Some lex';
            typ =
              Some
                (Numeriek
                   { n with eenheid = Some finest_eenheid; decimalen = d; geheel = d = 0 && n.geheel });
          }
      end
  | _ -> e

(* does the value carry a stelsel-[sname] literal whose declared unit RECORD
   differs from the finest target unit [tu] — i.e. a genuine restatement this
   pass must perform? Mirrors [norm_expr]'s traversal. This is the gate that
   keeps the pass the identity: a rule that already translates does so with
   units identical to the target, so it carries no such literal and the finest
   derivation (which reads every declared factor, and would fault on a stelsel
   whose factor has no short-decimal reading) is NEVER invoked for it. A
   differing-record literal in a restating position would otherwise have tripped
   [Translate.units_identical_ok] already, so a passing rule cannot hold one. *)
let rec has_restatable ~sname ~tu (e : expr) : bool =
  let opt = function Some x -> has_restatable ~sname ~tu x | None -> false in
  match e with
  | Literal { typ = Some (Numeriek { eenheid = Some u; _ }); _ } ->
      unit_in_stelsel ~sname u && u <> tu
  | Binair { op = Plus | Min | Verminderd_met; links; rechts; _ } -> opt links || opt rechts
  | Afgerond { basis; _ } -> opt basis
  | Begrensd { basis; minimum; maximum; _ } -> opt basis || opt minimum || opt maximum
  | Temporal_read { basis; _ } -> has_restatable ~sname ~tu basis
  | Functie { kind = Som | Totaal | Minimum | Maximum | Eerste | Laatste; argumenten; _ } ->
      List.exists (has_restatable ~sname ~tu) argumenten
  | _ -> false

(* walk a value expression, restating every stelsel-[sname] literal that sits in
   a UNIT-RESTATING position — the additive family, the clamp bounds and the
   ordering folds, exactly the [Translate.units_identical_ok] sites this pass
   lifts (mirroring [eff_unit]'s unit-carrying forms). Multiplicative/division
   positions are deliberately NOT descended: they demand a unit-FREE operand
   (or, for PERCENTAGE_VAN, pure-%), which restating cannot satisfy, so they
   keep the existing refusal. *)
let rec norm_expr ~sname ~finest_eenheid ~to_finest (e : expr) : expr =
  let recurse = norm_expr ~sname ~finest_eenheid ~to_finest in
  let recurse_opt = Option.map recurse in
  match e with
  | Literal _ -> restate_lit ~sname ~finest_eenheid ~to_finest e
  | Binair ({ op = Plus | Min | Verminderd_met; links; rechts; _ } as b) ->
      Binair { b with links = recurse_opt links; rechts = recurse_opt rechts }
  | Afgerond ({ basis; _ } as a) -> Afgerond { a with basis = recurse_opt basis }
  | Begrensd ({ basis; minimum; maximum; _ } as g) ->
      Begrensd
        { g with basis = recurse_opt basis; minimum = recurse_opt minimum;
          maximum = recurse_opt maximum }
  | Temporal_read ({ basis; _ } as t) -> Temporal_read { t with basis = recurse basis }
  | Functie ({ kind = Som | Totaal | Minimum | Maximum | Eerste | Laatste; argumenten; _ } as f) ->
      Functie { f with argumenten = List.map recurse argumenten }
  | _ -> e

(* normalise one write rule when its numeric target is the finest unit of a
   declared stelsel; otherwise leave it to translate's standing behaviour. *)
let norm_regel (doc : document) (r : resolved_regel) : resolved_regel =
  match (r.rr_soort, r.doel, r.waarde) with
  | ( Some (Berekening | Gelijkstelling),
      Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }),
      Some waarde ) -> (
      match target_unit doc ~ot ~attr:a with
      | Some tu when tu.noemer = None -> (
          match tu.stelsel with
          (* the identity gate: no restatable literal ⇒ leave the rule untouched
             and NEVER read the stelsel's factors (so a stelsel with an
             inexact-float factor the pass does not exercise never faults). *)
          | Some sname when has_restatable ~sname ~tu waarde -> (
              match Checks.find_ci doc.eenheidstelsels sname with
              | Some s ->
                  let finest, to_finest = finest_of_stelsel s in
                  (* only when the target IS the finest can the restated (finer)
                     value be written without a divide-down the encoding cannot
                     hold; a coarser target keeps the existing loud refusal. *)
                  if Q.equal (Arith.factor tu) (Arith.factor finest) then
                    { r with waarde = Some (norm_expr ~sname ~finest_eenheid:tu ~to_finest waarde) }
                  else r
              | None -> r)
          | _ -> r)
      | _ -> r)
  | _ -> r

(* the pass entry: normalise every resolved rule; the rest of the document is
   carried through untouched. Composed before [Ir_v0.Desugar.document] at the
   [Translate.translate] entry — the two are orthogonal (this touches only unit
   annotations and literal values; desugaring restructures prioritized
   defaults). *)
let document (doc : document) : document =
  { doc with resolved_regels = List.map (norm_regel doc) doc.resolved_regels }
