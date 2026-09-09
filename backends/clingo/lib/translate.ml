(* regelspraak-ir-v0 + uchar-case-v0 → ASP-Core-2 (clingo).

   The systematic landing of spec/ASP-TRANSLATION.md's conventions:
   instances as constants, a role as an argument position, navigation as
   a shared variable, negated characteristic reads as
   negation-as-failure — sound because WF-045/046 make every admissible
   document stratifiable. Extensions beyond the hand exercises:

   - Exact arithmetic by declared-decimals integer scaling: a value of
     an attribute declared with d decimals is the integer value × 10^d;
     comparisons harmonise scales by multiplication. Clingo's integer
     division truncates toward zero, which on the non-negative operands
     of the allocation recurrence is exactly floor_d.
   - Aggregation as #count/#sum with the contributor skip encoded by
     the body join (an instance without the attribute contributes no
     tuple).
   - Consistency and uniqueness derive inconsistent/2 facts — the §9.5
     channel; "consistent" means the model carries none.
   - Dates are civil day numbers (the case loader's encoding).
   - Emptiness is absence: an underivable val/3 atom is leeg, and an
     ordering comparison over leeg fails by the unbound join — the
     ch. 1.2d ordering rule for free. A COALESCING operand position
     (the ch. 1.2a whitelist: both sides of plus/min, the right of
     verminderd-met) additionally reads leeg as 0 through a default-0
     helper (coalesce0: value when the operand's body binds, 0 by
     negation-as-failure over a definedness helper), while a
     propagating position (verminderd-met's left) stays the bare
     unbound join — leeg leaves the rule underivable, exactly ref's
     Empty propagation. The NUMERIC equality/inequality empty
     asymmetry rides a per-comparison eqN helper (S5); the NON-numeric
     text/enum/boolean equality is a positive symbolic-term join —
     any empty operand fails both directions (Tabel 38, S5/S7).
   - The multiplicative family: maal and percentage-van coalesce both
     operands and multiply scaled integers (percentage-van folds its
     /100 into the scale — no division). Gedeeld-door carries its
     three edges (empty dividend → 0; empty divisor under a filled
     dividend → the §9.5 channel fact beside an absent value; numeric
     zero divisor → a viol/2 witness, UNSAT with the reason named)
     and translates only EXACT outcomes — a remainder derives a
     viol/2 witness, never a silently truncated value; the
     Afgerond-over-division form computes the mode's integer rounding
     exactly (non-negative operands; the negative range is fenced by
     a viol/2 witness). The abs-division variant is the same path
     collapsed at (Richting_nul, 5) — ch. 2.2's defined truncation
     point, both signs. Tot-de-macht is gringo's ** on the scaled
     base at result scale sl^e, for a compile-time non-negative
     integer exponent only (an empty base propagates by the bare
     join; runtime, negative and fractional exponents fault).
     Unit-carrying multiplicative operands are
     refused: ref's unit algebra applies conversion factors this
     integer encoding does not carry.

   Everything outside the corpus-exercised subset faults with context:
   fail fast, never a silent SKIP. *)

open Ir_v0.Ast
module Case = Uchar_ref.Case
module Value = Uchar_ref.Value
module Eval = Uchar_ref.Eval

let norm = Ir_v0.Checks.norm
let fault = Value.fault
(* a declared name or case label lands between double-quotes in the
   generated ASP. gringo's string syntax escapes an embedded
   double-quote as backslash-quote and a backslash as doubled, so both
   are lowered to their escape sequence — identically on the literal and
   value sides (S2), which is what keeps the equality join sound. A
   control character < 0x20 has no such escape and would break the
   program's syntax UNFENCED (a hunted crash), so it still faults with
   its reason. For DECLARED names WF-057 (IR-SPEC ch. 2.1) now rejects
   the document itself — this remains the last-line backstop, and it
   alone covers case LABELS, which are case-side and outside WF-057's
   reach. The expectation-check parser (model.ml) reads the SAME escaped
   shape back, escape-aware, so no independent encoding drifts. *)
let quote s =
  let b = Buffer.create (String.length s + 2) in
  Buffer.add_char b '"';
  String.iter
    (fun c ->
      if Char.code c < 0x20 then
        fault "name or label %S carries a character the ASP string encoding cannot quote" s
      else begin
        if c = '"' || c = '\\' then Buffer.add_char b '\\';
        Buffer.add_char b c
      end)
    s;
  Buffer.add_char b '"';
  Buffer.contents b

let str s = quote (norm s)
let str_raw s = quote s (* labels keep their case-sensitive spelling *)

(* --- scales ------------------------------------------------------------- *)

(* clingo grounds arithmetic in machine integers and wraps silently on
   overflow (a hunted silent split: 50000*50000 grounded NEGATIVE, and
   a wrapped comparison threshold flipped a branch). Every path into
   the integer encoding is therefore fenced: scales beyond 10^9 fault
   here, input values beyond 2^31-1 fault in [scaled_of_value], and
   emitted multiplications carry viol/2 range guards (see
   [range_guard]). *)
let max_int31 = 2147483647

let pow10 n =
  (* [go] counts n DOWN to 0, so a NEGATIVE n never terminates: measured as a
     hang with no output at all (killed at 10 s), where 10^12 faults cleanly one
     line below. The schema puts no lower bound on `decimalen`, so the floor is
     this executor's to state. *)
  if n < 0 then fault "a negative decimal count reaches a scale (10^%d)" n;
  if n > 9 then fault "a scale of 10^%d exceeds clingo's integer range" n;
  let rec go acc n = if n = 0 then acc else go (acc * 10) (n - 1) in
  go 1 n

let decimals_of_datatype = function
  | Some (Numeriek { decimalen; _ } | Percentage { decimalen; _ }) -> decimalen
  | Some Datum_in_dagen -> 0
  | dt ->
      ignore dt;
      fault "unscalable datatype in the ASP translation"

(* the ASP scale of an accumulator/result datatype: numeric datatypes ride
   their declared decimals; a sum-typed accumulator (the reduce lowering's
   Optional, ch. 10.4/§3.5) is a ground constructor term carrying no numeric
   scale (0), exactly like a Construct value in [expr_term] *)
let value_scale = function
  | Some (Sum_type _ | Struct_type _) -> 0
  | t -> pow10 (decimals_of_datatype t)

(* the harmonisation fence. Scale 0 marks a SYMBOLIC term — a sum-type
   constructor term, a struct record term, and whatever future datatype
   [value_scale] hands no numeric scale. Carrying such a term is legitimate (a
   fold accumulator, a sum-typed write, an equality join), but every
   arithmetic, ordering, rounding and fitting step HARMONISES scales by
   dividing by them, and a divisor of 0 raises Division_by_zero: an uncaught
   exception, exit 2, no rule and no reason — measured on the articulated
   ordering fold over two sum-typed operands, whose own operand fence knew the
   ch. 1.2d scalars but not SumType.

   The fence hangs on the SCALE and not on a wire `type` annotation, which is
   what makes it the smallest cut that holds. The annotation-shaped fences
   beside it ([numeric_ok], [is_symbolic_operand]) read [typ_of_expr] — the
   wire — while the scale is derived from the DECLARATION, so an absent or
   lying annotation walks straight past them into the division (§2.10: a
   consumer derives from declarations). Every divisor position routes through
   here instead, so a new unscaled datatype is refused the day it is added
   rather than the day it crashes. *)
let numeric_scale ~what s =
  if s < 1 then
    fault
      "a symbolic operand reaches %s: a value with no numeric scale (today a sum-type or record \
       value) cannot be harmonised against one (ref faults the operation; this arm refuses it \
       rather than divide by the absent scale)"
      what;
  s

let scaled_of_value ~decimals (v : Value.t) : int =
  match v with
  | Num (q, _) ->
      let s = Q.mul q (Q.of_int (pow10 decimals)) in
      if not (Z.equal (Q.den s) Z.one) then
        fault "value %s does not fit %d decimals" (Q.to_string q) decimals
      else if Z.gt (Z.abs (Q.num s)) (Z.of_int max_int31) then
        fault
          "value %s at %d decimals exceeds clingo's integer range (grounding would wrap \
           silently)"
          (Q.to_string q) decimals
      else Z.to_int (Q.num s)
  | Date z -> z
  | v -> fault "unscalable value %s" (Value.to_string v)

(* --- the datum-tijd pair term (RUNTIME-SEMANTICS ch. 3.8; the A4 pair-term
   encoding) -------------------------------------------------------------
   A millisecond-precision date rides as dt(D, MS) — the civil day D beside
   the ms-of-day MS with 0 <= MS < 86 400 000 — so a datetime's absolute ms
   value (>= 1.7e12 for any modern date) NEVER enters clingo's machine-int
   range whole: only the bounded (D, MS) parts and bounded differences do.
   The split is floor division (the ms-of-day is non-negative left of the
   epoch too), mirroring ref/'s Arith.split_ms. One source of truth for the
   case emission and the model-side expectation (Model.check parses this). *)
let ms_per_day = 86_400_000

let split_datetime t =
  let d = if t >= 0 then t / ms_per_day else -(((-t) + ms_per_day - 1) / ms_per_day) in
  (d, t - (d * ms_per_day))

let datetime_term (t : int) : string =
  let d, ms = split_datetime t in
  Printf.sprintf "dt(%d,%d)" d ms

(* --- document lookups --------------------------------------------------- *)

type ctx = {
  doc : document;
  case : Case.t;
  buf : Buffer.t;
  mutable fresh : int;
  (* the enclosing rule's condition atoms: every viol/2 or
     inconsistent/2 head a value expression emits must fire only where
     the rule itself fires — ref evaluates the value only under a true
     voorwaarde, so an excluded subject must never fault (set around
     the value translation, [] elsewhere) *)
  mutable rule_guard : string list;
  (* the write target's scale, as a lower bound for a bare division's
     out-scale: ref writes the exact rational, so a quotient that fits
     the target's declared decimals exactly must translate — testing
     exactness on max(sl,sr) alone would refuse it (1 outside a write) *)
  mutable write_scale : int;
  (* the enclosing rule's (or creation's) name: the §9.5 channel fact
     is keyed by it, exactly as ref's insert_error keys its channel —
     the reason text stays executor-internal (a hunted checker split:
     rule matching on the channel would otherwise diverge) *)
  mutable rule_name : string;
  (* the lexical binder environment for match arms (RUNTIME-SEMANTICS
     ch. 9.2): a bound binder name maps to the ASP variable the arm's
     scrutinee pattern binds and the payload's declared scale, so a
     BinderRef in the arm body resolves to that variable. Set per arm,
     [] elsewhere — WF-064 makes an unbound read statically impossible.
     The atom list is the binder's own BINDING atoms (empty for a
     recurrence-bound binder — a match payload, a lambda/fold parameter —
     whose ASP variable the surrounding join already grounds; non-empty
     for a Let binder, which reads through its self-contained helper so a
     bare value var is bound wherever it is used, B3) *)
  mutable binders : (string * (string list * string * int)) list;
}

(* The convention guards, mirroring ref/'s faults (IR-SPEC ch. 4.3a:
   "executors fault, never guess"): a point read needs the full-timeline
   case variant on every subject instance, and the temporal total needs
   the asOfDate-bucket scalar — a mismatch faults instead of silently
   reading leeg through an unbound join. *)
let require_attr_timeline ctx ~otype ~attr =
  Eval.SMap.iter
    (fun _ (i : Uchar_ref.World.instance) ->
      if
        Ir_v0.Checks.norm i.otype = Ir_v0.Checks.norm otype
        && Uchar_ref.World.timeline i attr = None
      then
        fault
          "the temporal read of %S needs the full timeline; the case supplies none for %S \
           (the asOfDate-bucket scalar cannot serve a point read)"
          attr i.label)
    ctx.case.world.instances

let refuse_attr_timeline ctx ~otype ~attr =
  Eval.SMap.iter
    (fun _ (i : Uchar_ref.World.instance) ->
      if
        Ir_v0.Checks.norm i.otype = Ir_v0.Checks.norm otype
        && Uchar_ref.World.timeline i attr <> None
      then
        fault
          "the temporal total of %S over a case-supplied full timeline is not exercised by any \
           corpus case; the asOfDate-bucket convention carries the total"
          attr)
    ctx.case.world.instances

let emit ctx fmt = Printf.ksprintf (fun s -> Buffer.add_string ctx.buf (s ^ "\n")) fmt

let fresh ctx prefix =
  ctx.fresh <- ctx.fresh + 1;
  Printf.sprintf "%s%d" prefix ctx.fresh

(* the validity gate (RUNTIME-SEMANTICS ch. 3.10/5.6, gated at runtime
   against the session's asOfDate — ch. 8.2): the mirror of ref's
   [applies] (ref/lib/eval.ml), decision for decision. The translation is
   per-case, so the case's asOfDate is a translation-time constant and
   the runtime gate is decided HERE: a form whose window excludes the
   date simply does not apply and emits nothing — no head, no helper, no
   viol/2 fence (ref never evaluates it). Bounds compare as ISO strings,
   both inclusive, exactly as ref compares them; a bounded window in a
   time-free session faults with ref's reason, so the two executors
   refuse identically. *)
let applies ctx (g : geldigheid option) =
  match g with
  | None | Some { geld_vanaf = None; geld_tot = None } -> true
  | Some { geld_vanaf; geld_tot } -> (
      match ctx.case.as_of with
      | None ->
          fault "a validity-bounded form needs an evaluation date; this session is time-free"
      | Some d ->
          (match geld_vanaf with Some v -> String.compare v d <= 0 | None -> true)
          && (match geld_tot with Some t -> String.compare d t <= 0 | None -> true))

let objecttype ctx name =
  match Ir_v0.Checks.find_ci ctx.doc.objecttypen name with
  | Some ot -> ot
  | None -> fault "unknown object type %S" name

let attr_decimals ctx ~otype ~attr =
  let ot = objecttype ctx otype in
  match Ir_v0.Checks.find_ci ot.attributen attr with
  | Some dt -> decimals_of_datatype dt
  | None -> fault "unknown attribute %S on %S" attr otype

let attr_unit ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci (objecttype ctx otype).attributen attr with
  | Some (Some (Numeriek { eenheid; _ } | Percentage { eenheid; _ })) -> eenheid
  | _ -> None

let attr_sum_typed ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci (objecttype ctx otype).attributen attr with
  | Some (Some (Sum_type _)) -> true
  | _ -> false

let attr_struct_typed ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci (objecttype ctx otype).attributen attr with
  | Some (Some (Struct_type _)) -> true
  | _ -> false

let attr_datetime_typed ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci (objecttype ctx otype).attributen attr with
  | Some (Some Datum_tijd) -> true
  | _ -> false

(* THE TEMPORALITY ANCHOR of a fold operand (decision 2026-09-05).
   A fold operand's declaration is looked up on the ELEMENT type when the
   read is a SET read — the value is read off each member, and every set
   arm below anchors its reads at the element variable — and on the
   operand's own objecttype otherwise, where the read IS the subject's.
   Before this decision the arms disagreed: the temporal Totaal refusal
   hung its temporality guard on the operand's [objecttype], the Som and
   Minimum/Maximum guards on [elementObjecttype]. On every frontend-emitted
   form the two coincide; on a MIXED annotation they do not, and the set
   fold then fell reasonlessly into the catch-all. One anchor, all arms. *)
let fold_operand_otype ~element_objecttype ~set (ot : string option) =
  if set then element_objecttype else ot

(* temporality WITHOUT [attr_temporal]'s fault: [eff_unit] below runs in
   pure ANALYSIS positions (comparisons, additive joins, the write fence)
   where a type it cannot resolve must stay an unknown unit, never a
   translation fault of its own — the faulting lookup belongs to the arm
   that actually translates the node. *)
let attr_temporal_decl ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci ctx.doc.objecttypen otype with
  | Some ot -> Ir_v0.Checks.attr_temporal ot attr
  | None -> false

(* the EFFECTIVE unit of an expression, derived from declarations
   (attribute/parameter datatypes) and the ch. 2.6 join rules — never
   from the node's own static annotation: a composite annotated
   eenheid:null still carries its operands' units at runtime, and
   reading the annotation let a unit-carrying comparison of two plus
   composites slip the fence (a hunted silent split). Multiplicative
   operands are fenced unit-free at their own translation, so those
   nodes contribute None. *)
let rec eff_unit ctx (e : expr) : eenheid option =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ } ->
      attr_unit ctx ~otype:ot ~attr:a
  | Parameter_ref { parameter = Some p; _ } -> (
      match Ir_v0.Checks.find_ci ctx.doc.parameters p with
      | Some { par_datatype = Some (Numeriek { eenheid; _ } | Percentage { eenheid; _ }); _ } ->
          eenheid
      | _ -> None)
  | Literal { typ = Some (Numeriek { eenheid; _ } | Percentage { eenheid; _ }); _ } -> eenheid
  | Binair { op = Plus | Min | Verminderd_met; links; rechts; _ } -> (
      (* additive results carry the left unit; a unitless side adopts
         the other (Arith.join_units) — a genuine mismatch faults when
         the join itself is translated *)
      match Option.bind links (eff_unit ctx) with
      | Some u -> Some u
      | None -> Option.bind rechts (eff_unit ctx))
  | Binair { op = Maal; links; rechts; _ } -> (
      (* the admitted factor-free product (ch. 2.6, Arith.mul): a unitless
         operand times a unit-carrying operand adopts THAT unit, value
         unchanged — so a restating position consuming the product sees the
         real unit and its own fence still fires on a genuine mismatch (the
         loud-witness). A two-unit product faults at its own translation
         (mul_units_ok), so reporting the left there is moot. *)
      match Option.bind links (eff_unit ctx) with
      | Some u -> Some u
      | None -> Option.bind rechts (eff_unit ctx))
  | Binair _ -> None
  | Afgerond { basis; _ } | Begrensd { basis; _ } -> Option.bind basis (eff_unit ctx)
  | Temporal_read { basis; _ } -> eff_unit ctx basis
  | Functie
      { kind = (Som | Totaal) as kind;
        argumenten = (Attribuut_ref { attribuut = Some a; objecttype; set; _ } as arg) :: _;
        element_objecttype; _ }
    when (kind = Totaal || set)
         &&
         match fold_operand_otype ~element_objecttype ~set objecttype with
         | Some ot -> attr_temporal_decl ctx ~otype:ot ~attr:a
         | None -> false ->
      (* the §4.20 Tabel 30 noemer strip, as an EFFECTIVE unit. A TIME
         aggregation integrates a rate over the grid, so the /tijdseenheid
         cancels through the declared factors and the total carries the
         numerator alone (€/mnd over a month grid → €; RUNTIME-SEMANTICS
         ch. 3.6 "Units", the ref mirror). The write fence below then sees
         the STRIPPED unit against the declared target and the two are
         identical — which is what lets the temporal totaal translate at
         all; before this the fence read €/mnd against € and refused every
         unit-carrying time aggregation.

         The strip follows the INTEGRATION, not the operator: it fires
         exactly where ref runs a Timeline.totaal — the own-subject
         temporal totaal, and the §7.1 per-element fold under BOTH som
         and totaal (there the members' timelines collapse first, so a
         som over a temporal set strips just as hard). A NON-temporal
         set totaal is the som-alias and keeps its rate: the golden pins
         that row, its set TOTAAL and its SOM carrying the identical
         €/mnd result type. Whether the strip lands EXACTLY in this
         integer encoding is the translating arm's fence
         ([grid_strip_ok]), not this analysis's. *)
      Option.map
        (fun (u : eenheid) -> { u with noemer = None })
        (eff_unit ctx arg)
  | Functie { kind = Som | Totaal | Minimum | Maximum | Eerste | Laatste; argumenten = arg :: _; _ }
    ->
      eff_unit ctx arg
  | _ -> None

let attr_temporal ctx ~otype ~attr = Ir_v0.Checks.attr_temporal (objecttype ctx otype) attr

let attr_granularity ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci (objecttype ctx otype).attribuut_granulariteit attr with
  | Some (Some g) -> g
  | _ -> fault "temporal attribute %S without granularity" attr

(* --- expressions → (body atoms, integer term, scale) -------------------- *)

let req what = function Some x -> x | None -> fault "missing %s" what

(* does a pro-rata rule write this temporal attribute? — a clip over
   such a DERIVED timeline inherits the fold's interior bucket walls,
   which the case's endpoint set does not carry (a hunted silent zero:
   the wu join found no fact and the #sum went empty) *)
let prorata_writes (doc : document) ~otype ~attr =
  List.exists
    (fun (r : resolved_regel) ->
      match (r.doel, r.waarde) with
      | ( Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }),
          Some (Functie { kind = Tijdsevenredig_deel; _ }) ) ->
          norm a = norm attr && norm ot = norm otype
      | _ -> false)
    doc.resolved_regels

(* does the case supply an input value or timeline for this attribute
   on any instance of the type? — ref (like v0) lets a computed target
   OVERWRITE its input, which has no monotone encoding here: the case
   fact and the rule head would union into a superset model (a hunted
   silent split), so the translator refuses loudly instead *)
let case_supplies ctx ~otype ~attr =
  Eval.SMap.exists
    (fun _ (i : Uchar_ref.World.instance) ->
      norm i.otype = norm otype
      && (Uchar_ref.World.attr i attr <> Value.Empty
         || Uchar_ref.World.timeline i attr <> None))
    ctx.case.world.instances

(* does any recursive rule group carry an object-creation member? —
   gates the generation-depth machinery (depth/2, oc_budget) *)
let has_recursive_creation (doc : document) =
  List.exists
    (fun (g : resolved_regel_groep) ->
      g.recursief
      && List.exists
           (function
             | Some l ->
                 List.exists
                   (fun (o : resolved_object_creatie) ->
                     match o.oc_naam with
                     | Some n ->
                         norm (Ir_v0.Checks.strip_version n)
                         = norm (Ir_v0.Checks.strip_version l)
                     | None -> false)
                   doc.object_creaties
             | None -> false)
           g.leden)
    doc.regel_groepen

(* the canonical role name: the plural for a to-many role, the singular
   otherwise — the same convention the case facts use *)
let role_canonical (r : rol) =
  match (r.rol_meervoud, r.rol_naam) with
  | Some m, _ when r.rol_meervoudig -> m
  | _, Some n -> n
  | Some m, None -> m
  | None, None -> fault "role without a name"

let lexeme_decimals lex =
  match String.index_opt lex '.' with
  | Some i -> String.length lex - i - 1
  | None -> ( match String.index_opt lex ',' with Some i -> String.length lex - i - 1 | None -> 0)

(* the year-bucket whole-units factor of the temporal total: for a
   year-granular attribute over the calendar-year bucket the factor is
   exactly 1; other granularities await a corpus case *)
let total_factor (g : granulariteit) =
  match g with
  | Jaar | Maand -> 1
  | _ -> fault "temporal total: only month and year granularity have a calendar bucket (ch. 3.2)"

(* the §4.20 noemer strip, as ARITHMETIC. ref integrates a rate by
   multiplying the segment value by a duration stated in the GRID's own
   unit, so the denominator cancels through the DECLARED factors: over a
   month grid €/mnd multiplies by 1 and €/jr by 1/12 (12 months IS one
   year by the declared factor). This integer encoding carries no
   conversion factor, so only the factor-1 case — a denominator identical
   to the grid unit by declaration — is representable; every other
   convertible denominator would need a fraction, and a denominator the
   declarations connect to nothing grid-sized is refused by ref too. Each
   is refused here by name, never rounded and never silently dropped.
   [what] names the fold site. *)
let grid_strip_ok ctx ~what ~(gran : granulariteit) (u : eenheid option) =
  match u with
  | Some ({ noemer = Some n; _ } as rate) -> (
      match Uchar_ref.Timeline.grid_unit ctx.doc gran with
      | Error why -> fault "the noemer strip of %s: %s" what why
      | Ok gu ->
          if not (Uchar_ref.Arith.convertible n gu) then
            fault
              "the noemer strip of %s cannot integrate a %s value over a %s grid: the \
               declarations connect no conversion between its denominator and the grid unit \
               (ch. 3.6); ref refuses the shape too"
              what
              (Uchar_ref.Arith.show_eenheid rate)
              (Uchar_ref.Arith.show_eenheid gu)
          else if not (Q.equal (Uchar_ref.Arith.conv_factor gu n) Q.one) then
            fault
              "the noemer strip of %s restates a %s value into a %s grid by a declared factor \
               other than 1, which this integer encoding does not carry (ref applies the exact \
               factor); only a denominator identical to the grid unit cancels here"
              what
              (Uchar_ref.Arith.show_eenheid rate)
              (Uchar_ref.Arith.show_eenheid gu))
  | _ -> ()

(* harmonise two scaled terms for comparison. Self-fenced: every caller
   already fences its own operands where it reads them (the range guards
   divide by the scales one line earlier), but the helper must not become a
   crash site for a caller that forgets. *)
let harmonised (b1, t1, s1) (b2, t2, s2) =
  let s1 = numeric_scale ~what:"a scale harmonisation" s1 in
  let s2 = numeric_scale ~what:"a scale harmonisation" s2 in
  let m = max s1 s2 in
  let scale t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
  (b1 @ b2, scale t1 s1, scale t2 s2)

(* an operand in a COALESCING position (the ch. 1.2a whitelist): leeg
   reads as 0. The operand's body atoms move into a helper predicate
   that derives the bound value, and a default-0 rule fires when the
   body has no derivation — negation-as-failure over a definedness
   helper. An atom-free operand (a literal, a constant) is never empty
   and passes through untouched. Sound on WF-045/046-clean documents:
   the negation reads exactly what the positive operand read, so any
   cycle it sits on is a creation cycle whose values are derived in the
   minimal model (the definedness is grounded, never self-supporting). *)
let coalesce0 ctx ~subject ~subject_ot (atoms, term, scale) =
  if atoms = [] then (atoms, term, scale)
  else begin
    ctx.fresh <- ctx.fresh + 1;
    let pred = Printf.sprintf "co%d" ctx.fresh in
    let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
    let body = String.concat ", " (base :: atoms) in
    emit ctx "%s(%s,%s) :- %s." pred subject term body;
    emit ctx "%s_def(%s) :- %s." pred subject body;
    emit ctx "%s(%s,0) :- %s, not %s_def(%s)." pred subject base pred subject;
    let v = fresh ctx "C" in
    ([ Printf.sprintf "%s(%s,%s)" pred subject v ], v, scale)
  end

(* --- the calendar chains (RUNTIME-SEMANTICS ch. 3.8) --------------------
   Dates are civil day numbers (B2). Whole-unit counting and the
   date-plus-months step run as pure-ASP arithmetic chains: the civil
   decomposition keeps every intermediate non-negative for any date from
   0000-03-01 on (fenced by a viol/2 witness below), so gringo's
   toward-zero division equals the floor the era arithmetic needs. The
   divisions that DO see a negative operand — the packed difference quot
   32, the week quotient, quarters/years over months, the whole-count
   scale division — truncate toward zero by gringo's C semantics, which
   IS the ch. 3.8 quot (the negative-span case of the
   jutterswet-jutleeftijd anchor pins it differentially). *)

(* the counted unit, resolved by SPELLING from a static type through the
   shared table (Ir_v0.Checks, RUNTIME-SEMANTICS ch. 3.8): day-precision
   units only — sub-day, day-kind and missing units fault loudly *)
let day_time_unit_of ~what (t : datatype option) =
  match t with
  | Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ }) -> (
      match Ir_v0.Checks.time_unit_of_eenheid u with
      | Some tu ->
          if Ir_v0.Checks.time_unit_sub_day tu then
            fault "sub-day %s needs datum-tijd operands (a day-precision date has no \
                   time-of-day)" what
          else tu
      | None ->
          fault
            "%s unit %S is not a recognised time unit (a day-kind form is a recorded \
             flank, not executed)"
            what
            (Option.value u.afkorting ~default:"?"))
  | _ -> fault "%s without a time unit on its static type" what

(* the counted unit for a datum-tijd (millisecond-precision) operand: the
   full time-unit table, sub-day units included (RUNTIME-SEMANTICS ch. 3.8,
   the millisecond layer). A day-kind (dagsoort-as-unit) spelling stays a
   loud flank; a sub-day unit is admitted here because the operand carries a
   time-of-day. *)
let dt_time_unit_of ~what (t : datatype option) =
  match t with
  | Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ }) -> (
      match Ir_v0.Checks.time_unit_of_eenheid u with
      | Some tu -> tu
      | None ->
          fault
            "%s unit %S is not a recognised time unit (a day-kind form is a recorded \
             flank, not executed)"
            what
            (Option.value u.afkorting ~default:"?"))
  | _ -> fault "%s without a time unit on its static type" what

(* the ms size of a sub-day unit, or None for a day-precision unit
   (day/week/month/quarter/year) — mirrors ref/'s Arith.sub_day_ms *)
let sub_day_ms : Ir_v0.Checks.time_unit -> int option = function
  | Tu_uur -> Some 3_600_000
  | Tu_minuut -> Some 60_000
  | Tu_seconde -> Some 1_000
  | Tu_milliseconde -> Some 1
  | _ -> None

(* the era decomposition of a day-number term into binding atoms; returns
   the atoms plus the packed-month term pieces: PM (proleptic month,
   12·year + month − 1, uniform via PM = 12·(yoe + 400·era) + mp + 2) and
   D (day of month). packed(D) = PM·32 + D (ch. 3.8). *)
let civil_pm_day ctx (t : string) =
  let zv = fresh ctx "Z" and ev = fresh ctx "E" and doev = fresh ctx "DOE" in
  let yoev = fresh ctx "YOE" and doyv = fresh ctx "DOY" and mpv = fresh ctx "MP" in
  let dv = fresh ctx "D" and pmv = fresh ctx "PM" in
  ( [ Printf.sprintf "%s = (%s)+719468" zv t;
      Printf.sprintf "%s = %s/146097" ev zv;
      Printf.sprintf "%s = %s-(%s*146097)" doev zv ev;
      Printf.sprintf "%s = ((%s-(%s/1460))+(%s/36524)-(%s/146096))/365" yoev doev doev doev
        doev;
      Printf.sprintf "%s = %s-(((365*%s)+(%s/4))-(%s/100))" doyv doev yoev yoev yoev;
      Printf.sprintf "%s = ((5*%s)+2)/153" mpv doyv;
      Printf.sprintf "%s = (%s-(((153*%s)+2)/5))+1" dv doyv mpv;
      Printf.sprintf "%s = ((12*(%s+(400*%s)))+%s)+2" pmv yoev ev mpv ],
    pmv,
    dv )

(* the pre-epoch fence: a date before 0000-03-01 would push the shifted
   day number negative and turn gringo truncation away from floor —
   fenced with a viol/2 witness, never a silent wrong month *)
let pre_epoch_fence ctx ~subject ~body t =
  emit ctx
    "viol(\"date before 0000-03-01 in calendar arithmetic (the civil decomposition needs \
     a non-negative shifted day number)\",%s) :- %s, (%s) < -719468."
    subject
    (String.concat ", " body)
    t

(* date + count whole months (ch. 3.8): re-anchor the proleptic month and
   clamp the day to the target month's length — one helper predicate per
   site, derived by the leap/month-length RULE PAIR: the non-February arm
   computes 30 + ((m + m/8) mod 2), the February arm 28 + the leap
   indicator (each (k − y mod k)/k term is 1 exactly on divisibility —
   non-negative operands, so truncation is floor). The clamp is the exact
   integer min (a + b − |a − b|)/2; the re-encode is days_from_civil with
   the (14 − m)/12 March-shift. An empty date or duration operand leaves
   the chain unbound — the helper underivable, leeg as absence. *)
let translate_date_plus_months ctx ~subject ~subject_ot ~date_atoms ~date_term ~dur_atoms
    ~count =
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  pre_epoch_fence ctx ~subject ~body:((base :: ctx.rule_guard) @ date_atoms) date_term;
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "dpm%d" ctx.fresh in
  let catoms, pmv, dv = civil_pm_day ctx date_term in
  let pmt = fresh ctx "PMT" and yt = fresh ctx "YT" and mt = fresh ctx "MT" in
  let chain =
    date_atoms @ dur_atoms @ catoms
    @ [ Printf.sprintf "%s = (%s)+(%s)" pmt pmv count;
        Printf.sprintf "%s = %s/12" yt pmt;
        Printf.sprintf "%s = (%s-(12*%s))+1" mt pmt yt ]
  in
  (* the year-zero fence: PMT < 2 reaches a March-shifted year below 0,
     where the re-encode divisions would truncate away from floor *)
  emit ctx "viol(\"date arithmetic reaches a year before 0000-03\",%s) :- %s, %s < 2."
    subject
    (String.concat ", " ((base :: ctx.rule_guard) @ chain))
    pmt;
  let finish ml_atoms mlv =
    let dt = fresh ctx "DT" and y2 = fresh ctx "Y2" and e2 = fresh ctx "E2" in
    let yoe2 = fresh ctx "YOE" and mp2 = fresh ctx "MP" and doy2 = fresh ctx "DOY" in
    let doe2 = fresh ctx "DOE" and zt = fresh ctx "ZT" in
    ( chain @ ml_atoms
      @ [ Printf.sprintf "%s = ((%s+%s)-|%s-%s|)/2" dt dv mlv dv mlv;
          Printf.sprintf "%s = %s-((14-%s)/12)" y2 yt mt;
          Printf.sprintf "%s = %s/400" e2 y2;
          Printf.sprintf "%s = %s-(%s*400)" yoe2 y2 e2;
          Printf.sprintf "%s = (%s+9) \\ 12" mp2 mt;
          Printf.sprintf "%s = ((((153*%s)+2)/5)+%s)-1" doy2 mp2 dt;
          Printf.sprintf "%s = (((%s*365)+(%s/4))-(%s/100))+%s" doe2 yoe2 yoe2 yoe2 doy2;
          Printf.sprintf "%s = ((%s*146097)+%s)-719468" zt e2 doe2 ],
      zt )
  in
  let ml = fresh ctx "ML" in
  let body1, z1 =
    finish
      [ Printf.sprintf "%s != 2" mt;
        Printf.sprintf "%s = 30+((%s+(%s/8)) \\ 2)" ml mt mt ]
      ml
  in
  emit ctx "%s(%s,%s) :- %s." p subject z1 (String.concat ", " (base :: body1));
  let lv = fresh ctx "L" in
  let body2, z2 =
    finish
      [ Printf.sprintf "%s = 2" mt;
        Printf.sprintf "%s = (((4-(%s \\ 4))/4)-((100-(%s \\ 100))/100))+((400-(%s \\ 400))/400)"
          lv yt yt yt;
        Printf.sprintf "%s = 28+%s" ml lv ]
      ml
  in
  emit ctx "%s(%s,%s) :- %s." p subject z2 (String.concat ", " (base :: body2));
  let v = fresh ctx "DP" in
  ([ Printf.sprintf "%s(%s,%s)" p subject v ], v, 1)

let vergelijk_sym = function
  | Groter_of_gelijk | Later_of_gelijk -> ">="
  | Groter | Later -> ">"
  | Kleiner | Eerder -> "<"
  | Kleiner_of_gelijk | Eerder_of_gelijk -> "<="
  | Gelijk | Ongelijk ->
      (* scalar conditions take the eqN-helper route in cond_atoms (date
         gelijk-aan its positive join, which maps its own ==); what
         remains on this symbol path is the per-segment clip comparison,
         whose operands are timeline segments — the empty corner there
         is a different shape (a gap, not an empty operand) *)
      fault
        "equality in a per-segment clip comparison is not translated (no corpus case \
         exercises it)"

(* arithmetic operands must be statically numeric: ref faults date
   arithmetic under the additive/multiplicative operators, so a
   non-numeric static type must not silently compute on day numbers.
   The Datatype_onbekend-Binair recursion arm RETIRED with the eenheid-
   identiteit typerfix (besluit 16Y+17b): the v0 frontend no longer
   degrades a unit it cannot name to Onbekend on an arithmetic Binair — it
   types the product identity (unitloos × E = E) or faults loudly at the
   resolver, so "unit unknown is never a wire type". The sole reason this
   arm ever admitted an Onbekend-typed arithmetic node ("samengestelde
   eenheid") is gone; an Onbekend arithmetic type now faults here too,
   like any other non-numeric operand — never silently computed. *)
let numeric_ok e =
  match e with
  | Some e -> (
      match Ir_v0.Checks.typ_of_expr e with
      | Some (Numeriek _ | Percentage _) | None -> ()
      | Some _ -> fault "non-numeric arithmetic operand is not translated (no corpus case)")
  | None -> ()

(* the compile-time non-negative integer exponent of a tot-de-macht, shared by
   the rule-level [translate_power] (gringo's power operator) and the closed-body
   repeated Maal (one exponent reading). ref's general form has no integer
   encoding here:
   a fractional exponent takes an exact root (ch. 2.1 OPEN), a negative exponent
   inverts (a division), a runtime exponent makes the result scale
   exponent-dependent — each faults with its reason *)
let const_pow_exponent = function
  | Literal { waarde = Some w; _ } -> (
      let q = Value.q_of_lexeme w in
      if not (Z.equal (Q.den q) Z.one) then
        fault
          "fractional exponent %s is not translated (ref takes an exact root — the ch. 2.1 OPEN; \
           no corpus case)"
          w
      else
        match Z.to_int (Q.num q) with
        | e when e >= 0 -> e
        | _ ->
            fault
              "negative exponent %s is not translated (ref inverts the exact power — a division; no \
               corpus case)"
              w
        | exception Z.Overflow -> fault "exponent %s is out of range" w)
  | _ ->
      fault
        "a runtime exponent is not translated (the result scale is exponent-dependent, and an empty \
         or negative runtime exponent has no integer encoding; no corpus case)"

let rec gcd a b = if b = 0 then a else gcd b (a mod b)

(* the gcd-reduced constant-divisor fraction (§6.5/S18): the exact scaled
   quotient of a dividend at scale [sl] by a COMPILE-TIME positive constant
   divisor [c] at scale [sr], targeting [out_scale], is tl·(sr·out_scale) /
   (c·sl). Folding that constant fraction to lowest terms keeps the divisor's
   scale from inflating the numerator; the fold is exact (dividing numerator and
   denominator constants by a common factor preserves both the integer quotient
   and its divisibility test). Shared by the rule-level bare division and the
   closed-body division — one reduction.

   No [numeric_scale] fence of its own: both callers fence [sl] and [sr] the
   moment they read their operands, so [den_c] is 0 only if [c] is, and a
   zero [c] never reaches here ([const_div] admits c > 0 only). The gcd is
   therefore never gcd 0 0, whose 0 would make this the divide-by-zero. *)
let const_div_reduce ~sl ~sr ~out_scale ~c =
  let num_c = sr * out_scale and den_c = c * sl in
  let g = gcd num_c den_c in
  (num_c / g, den_c / g)

(* multiplicative operands must be effectively unit-free: ref's unit
   algebra (rate cancellation, convertible-unit division) applies exact
   conversion factors this integer encoding does not carry *)
let unit_free_ok ctx e =
  match e with
  | Some e ->
      if eff_unit ctx e <> None then
        fault
          "a unit-carrying multiplicative operand is not translated (ref's unit \
           algebra applies conversion factors this integer encoding does not carry)"
  | None -> ()

(* positions where ref RESTATES one operand into the other's unit — the
   additive join, the comparison, the write boundary — multiply by an
   exact conversion factor (a hunted silent split: 1 m plus 50 cm
   grounded as 1+50). The integer encoding carries no factor, so only
   IDENTICAL declared units (factor 1) or a unitless side (which adopts
   the other unit, value unchanged) translate; anything else faults. *)
let units_identical_ok ~what (l : eenheid option) (r : eenheid option) =
  match (l, r) with
  | Some a, Some b when a <> b ->
      fault
        "non-identical units under %s are not translated (ref restates with an exact \
         conversion factor this integer encoding does not carry)"
        what
  | _ -> ()

(* PROVABLY unit-free: distinguishes a genuinely unitless expression from one
   eff_unit merely cannot analyse (a Let, a field/dimension access, a division,
   an Onbekend-typed declaration), mirroring eff_unit's resolved forms but
   proving the ABSENCE of a unit rather than reading one. A declaration counts
   as proven unit-free only when its datatype is explicitly Numeriek/Percentage
   with eenheid null — an Onbekend or non-numeric declaration is NOT proven
   (its unit is unknown, possibly a composite the frontend could not name). *)
let rec provably_unitless ctx (e : expr) : bool =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ } -> (
      match Ir_v0.Checks.find_ci (objecttype ctx ot).attributen a with
      | Some (Some (Numeriek { eenheid = None; _ } | Percentage { eenheid = None; _ })) -> true
      | _ -> false)
  | Parameter_ref { parameter = Some p; _ } -> (
      match Ir_v0.Checks.find_ci ctx.doc.parameters p with
      | Some { par_datatype = Some (Numeriek { eenheid = None; _ } | Percentage { eenheid = None; _ }); _ }
        ->
          true
      | _ -> false)
  | Literal { typ = Some (Numeriek { eenheid = None; _ } | Percentage { eenheid = None; _ }); _ } -> true
  | Binair { op = Plus | Min | Verminderd_met | Maal; links = Some l; rechts = Some r; _ } ->
      (* a sum/difference/product is unit-free only when BOTH operands are *)
      provably_unitless ctx l && provably_unitless ctx r
  | Afgerond { basis = Some b; _ } | Begrensd { basis = Some b; _ } -> provably_unitless ctx b
  | Temporal_read { basis; _ } -> provably_unitless ctx basis
  | Functie { kind = Som | Totaal | Minimum | Maximum | Eerste | Laatste; argumenten = arg :: _; _ } ->
      provably_unitless ctx arg
  | _ -> false

(* the singleton %-unit: afkorting % / canoniek %, factor exactly 1, no
   denominator — the one convertibility class %-space ever inhabits
   (PRIORITIZED-DEFAULTS-EVOLUTIE §8.3). A hidden or unknown unit is never
   this; a rate (%/jr) or a factor≠1 spelling is not pure-% either. *)
let is_pure_pct (u : eenheid) : bool =
  u.afkorting = Some "%" && u.canoniek = Some "%" && u.noemer = None
  && match u.factor_naar_canoniek with Some f -> f = 1.0 | None -> false

(* PROVABLY pure-%: the effective unit is decidably the singleton %-unit.
   Mirrors provably_unitless's resolved leaf and unit-preserving forms but
   PROVES the unit is % rather than absent — an unanalysable node (a Let, a
   field access, a division, an Onbekend-typed declaration) is NOT proven and
   the caller keeps faulting loudly. The multiplicative/additive arms are
   deliberately omitted: % + % stays %, but % × % is %² (no ÷100 bridge), and
   no corpus operand is a composite. *)
let rec provably_pure_pct ctx (e : expr) : bool =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ } -> (
      match Ir_v0.Checks.find_ci (objecttype ctx ot).attributen a with
      | Some (Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ })) ->
          is_pure_pct u
      | _ -> false)
  | Parameter_ref { parameter = Some p; _ } -> (
      match Ir_v0.Checks.find_ci ctx.doc.parameters p with
      | Some { par_datatype = Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ }); _ }
        ->
          is_pure_pct u
      | _ -> false)
  | Literal { typ = Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ }); _ } ->
      is_pure_pct u
  | Afgerond { basis = Some b; _ } | Begrensd { basis = Some b; _ } -> provably_pure_pct ctx b
  | Temporal_read { basis; _ } -> provably_pure_pct ctx basis
  | Functie { kind = Som | Totaal | Minimum | Maximum | Eerste | Laatste; argumenten = arg :: _; _ } ->
      provably_pure_pct ctx arg
  | _ -> false

(* PERCENTAGE_VAN's left operand: unit-free OR provably pure-% — the ÷100
   bridge out of %-space, already folded into the sl·sr·100 scale
   (PRIORITIZED-DEFAULTS-EVOLUTIE §8.3 / Q10), so the arithmetic is unchanged.
   Any other unit on the left, and any unit reasoning that cannot PROVE
   pure-%, keeps faulting loudly through unit_free_ok. The right operand rule
   is unchanged. *)
let pct_van_left_ok ctx (e : expr option) =
  match e with
  | Some ex when provably_pure_pct ctx ex -> ()
  | _ -> unit_free_ok ctx e

(* the admitted factor-free product (ch. 2.6; ASP-TRANSLATION S6): a unitless
   operand TIMES a unit-carrying operand adopts that unit with the numeric
   value unchanged (Arith.mul, (Unitless, Unit u) -> Num(v, u); NO conversion
   factor), so the integer encoding is exact and the product adopts the unit
   (eff_unit above carries it onward). Soundness fence: the operand admitted AS
   the unitless side must be PROVABLY unit-free, never merely eff_unit-blind —
   a unit hidden behind an unanalysable node (a Let, a field access, a division)
   would otherwise become a silent unit×unit product with no downstream unit to
   fence it. A product of TWO unit-carrying operands — a rate cancellation
   (€/jr × jr), an opaque product unit (€ × m) — composes units / applies
   factors this integer encoding does not carry, so it keeps faulting loudly
   (the standing fence until the defaults-round waist question). *)
let mul_units_ok ctx ~what (l : expr option) (r : expr option) =
  let ul = Option.bind l (eff_unit ctx) and ur = Option.bind r (eff_unit ctx) in
  let proven_free = function Some e -> provably_unitless ctx e | None -> true in
  let hidden () =
    fault
      "a multiplicative operand under %s carries no statically resolvable unit, so the factor-free \
       product admission cannot prove it unit-free (a unit hidden behind it — a Let, a field \
       access, a division — would compute the raw integer product silently)"
      what
  in
  match (ul, ur) with
  | Some _, Some _ ->
      fault
        "a product of two unit-carrying operands under %s is not translated (ref's unit \
         algebra composes units / applies conversion factors this integer encoding does not carry)"
        what
  | Some _, None when not (proven_free r) -> hidden ()
  | None, Some _ when not (proven_free l) -> hidden ()
  | _ -> ()

(* the viol/2 range fence on emitted arithmetic: fires when an operand
   could push the term past clingo's silently-wrapping integer range;
   [body] must ground the subject *)
let range_guard ctx ~subject ~body terms_with_bounds =
  List.iter
    (fun (t, bound) ->
      emit ctx
        "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",%s) :- %s, \
         |(%s)| > %d."
        subject
        (String.concat ", " body)
        t bound)
    terms_with_bounds

(* the split money × rate cent encoding (§5/S18(c)), shared verbatim by the
   rule-level Afgerond{money×rate} and the closed-body money multiplication (one
   encoding). Rounding a 2-decimal money M by a 4-decimal rate R to the cent: the
   naive product M·R at scale 10^6 wraps clingo's machine integers at tutorial
   magnitude, so the rate splits into two scale-2 halves — Rh = R/100, Rl = R\100
   — giving round(M·R/10^4) half-up as C = A/100 + (100·(A\100) + M·Rl + 5000)/10^4
   with A = M·Rh (REKENKUNDIG, non-negative operands). [money_mult_fences] emits
   the four viol/2 fences keyed by [subject], grounded by the caller's [gbody]
   string; [money_mult_body] returns the non-negativity guards and the A/C
   assignment atoms over the caller's fresh variables [a] and [c]. *)
let money_mult_fences ctx ~subject ~gbody ~tm ~tr =
  emit ctx
    "viol(\"negative operand under money multiplication is not translated\",%s) :- %s, (%s) < 0."
    subject gbody tm;
  emit ctx
    "viol(\"negative operand under money multiplication is not translated\",%s) :- %s, (%s) < 0."
    subject gbody tr;
  (* A = M·Rh fence: |M| <= max_int31/(|Rh|+1) keeps |M·Rh| inside 2^31; the
     |Rh|+1 divisor is >= 1 for either sign, so a negative rate (fenced UNSAT
     above) can never divide by zero during grounding *)
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",%s) :- %s, |%s| > \
     %d/(|(%s)/100|+1)."
    subject gbody tm max_int31 tr;
  (* B = M·Rl fence: Rl = R\100 < 100, so |M| <= (max_int31-20000)/99 keeps the
     rounded numerator 100·rA + M·Rl + 5000 inside 2^31 *)
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",%s) :- %s, |%s| > %d."
    subject gbody tm
    ((max_int31 - 20000) / 99)

let money_mult_body ~a ~c ~tm ~tr =
  [ Printf.sprintf "(%s) >= 0" tm;
    Printf.sprintf "(%s) >= 0" tr;
    Printf.sprintf "%s = (%s)*((%s)/100)" a tm tr;
    Printf.sprintf "%s = %s/100 + (100*(%s\\100) + (%s)*((%s)\\100) + 5000)/10000" c a a tm tr ]

(* --- sum types (RUNTIME-SEMANTICS ch. 9; ASP-TRANSLATION S14) ------------ *)

(* a constructor VALUE or PATTERN is a ground term whose functor is the
   normalised constructor name: nullary is the bare functor (afgewezen),
   unary is functor(payload). A functor is an ASP symbol, not a string —
   so the name must normalise to a valid symbolic constant [a-z][a-z0-9_]*
   or the emitted program is syntactically broken UNFENCED. norm lowercases
   and collapses whitespace; a collapsed name that is empty, digit-led or
   carries a non-word character faults here with its reason (WF-060 admits
   the declaration; this is the ASP-side backstop, the quote fence's kin). *)
let ctor_functor tag =
  let f = norm tag in
  let ok =
    f <> ""
    && (match f.[0] with 'a' .. 'z' -> true | _ -> false)
    && String.for_all (function 'a' .. 'z' | '0' .. '9' | '_' -> true | _ -> false) f
  in
  if not ok then
    fault "constructor name %S does not normalise to a valid ASP functor symbol (%S)" tag f;
  f

(* the declared constructor list of a SumType annotation, and the declared
   payload datatype of one constructor within it (the payload is scaled by
   its OWN declaration, never by any target attribute) *)
let sum_constructors = function
  | Some (Sum_type { constructoren; _ }) -> constructoren
  | _ -> fault "a sum-type node without its SumType annotation (WF-061 should preclude this)"

let ctor_payload_datatype constructoren tag =
  match List.find_opt (fun (n, _) -> norm n = norm tag) constructoren with
  | Some (_, pdt) -> pdt
  | None -> fault "constructor %S is not declared in the sum type" tag

let is_sum_operand = function
  | Some e -> ( match Ir_v0.Checks.typ_of_expr e with Some (Sum_type _) -> true | _ -> false)
  | None -> false

(* --- structs (RUNTIME-SEMANTICS ch. 11; ASP-TRANSLATION S16) -------------- *)

let is_struct_operand = function
  | Some e -> ( match Ir_v0.Checks.typ_of_expr e with Some (Struct_type _) -> true | _ -> false)
  | None -> false

(* --- text/enum/boolean scalars (RUNTIME-SEMANTICS ch. 1.2d; S2/S5/S7) ----- *)

(* the ch. 1.2d NON-numeric scalars carry ground SYMBOLIC terms beside the
   integer encoding: a text value is its verbatim quoted string (ref compares
   String.equal — spelling and case are load-bearing), an enum label is its
   NORMALISED quoted string (ref compares norm-equal, so both executors
   normalise at the same point), a boolean is the bare symbol waar/onwaar —
   NOT the closed-world heeft/2 (which cannot tell false from leeg; the
   equality read needs the three-way distinction). Day-precision dates are
   NOT here: they ride the integer day number — but their gelijk-aan takes
   the positive join in cond_atoms, not the eqN both-unbound arm (the
   ch. 1.2d date row: v0's Tabel-38 guard on the date shape). *)
let is_symbolic_operand = function
  | Some e -> (
      match Ir_v0.Checks.typ_of_expr e with
      | Some (Tekst | Booleaan | Enumeratie _) -> true
      | _ -> false)
  | None -> false

let is_date_operand = function
  | Some e -> (
      match Ir_v0.Checks.typ_of_expr e with
      | Some (Datum_in_dagen | Datum_tijd) -> true
      | _ -> false)
  | None -> false

let is_datetime_operand = function
  | Some e -> ( match Ir_v0.Checks.typ_of_expr e with Some Datum_tijd -> true | _ -> false)
  | None -> false

let attr_symbolic_datatype ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci (objecttype ctx otype).attributen attr with
  | Some (Some ((Tekst | Booleaan | Enumeratie _) as dt)) -> Some dt
  | _ -> None

(* the ground term of a text/enum/boolean VALUE (case emission and the
   model-side expectation, one source of truth) *)
let symbolic_value_term (v : Value.t) : string =
  match v with
  | Value.Text s -> quote s
  | Value.Enum s -> str s
  | Value.Bool b -> if b then "waar" else "onwaar"
  | v -> fault "not a text/enum/boolean scalar value %s" (Value.to_string v)

let symbolic_literal_term (typ : datatype) lex =
  match typ with
  | Tekst -> quote lex
  | Enumeratie _ -> str lex
  | Booleaan -> (
      match norm lex with
      | "waar" -> "waar"
      | "onwaar" -> "onwaar"
      | _ -> fault "malformed boolean literal %S" lex)
  | _ -> fault "not a text/enum/boolean literal"

(* a text/enum/boolean operand → (body atoms binding a ground term, the
   term): an own-subject attribute read binds its val atom's value, a literal
   is a bare ground term with no atom. Both are positive, so a leeg attribute
   read simply fails the join (Tabel 38's any-empty-is-false, in BOTH
   equality directions, for free) — the sum/struct operand pattern on the
   scalar datatypes. *)
let symbolic_term ctx ~subject ~subject_ot (e : expr) : string list * string =
  match e with
  | Literal { waarde; typ = Some ((Tekst | Booleaan | Enumeratie _) as t); _ } ->
      ([], symbolic_literal_term t (req "Literal.waarde" waarde))
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
        navigatie_bindingen = None; set = false; _ }
    when norm ot = norm subject_ot && attr_symbolic_datatype ctx ~otype:ot ~attr:a <> None ->
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "temporal text/enum/boolean carriage is a recorded flank; no corpus case";
      let v = fresh ctx "V" in
      ([ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ], v)
  | _ ->
      fault
        "a text/enum/boolean operand here is an own-subject attribute read or a literal (no \
         corpus case for more)"

(* the declaring domain and declared fields of a struct-typed node, from its
   StructType annotation (WF-072 repeats it whole at every use site) *)
let struct_type_of (e : expr) : string * (string * datatype) list =
  match Ir_v0.Checks.typ_of_expr e with
  | Some (Struct_type { domein; velden }) -> (domein, velden)
  | _ -> fault "a struct node without its StructType annotation (WF-072 should preclude this)"

let struct_type_of_datatype = function
  | Some (Struct_type { domein; velden }) -> (domein, velden)
  | _ -> fault "a StructLiteral without its StructType annotation (WF-073 should preclude this)"

(* the ground term for a record VALUE (RUNTIME-SEMANTICS ch. 11.1): the
   normalised declaring-domain functor over the fields in FIXED DECLARATION
   ORDER, each scaled by its OWN declared field datatype — dossier(12050,3).
   A struct always declares at least one field (WF-071), so the term is never
   the bare functor (no collision with a nullary constructor). A nested STRUCT
   field VALUE recurses — one nested functor per level, mirroring [struct_base]'s
   recursion on a struct-in-struct field ACCESS, and the WF-071 acyclicity fence
   keeps the depth statically bounded (S16). A sum- or list-typed field value
   still has no flat integer encoding here and no corpus case — it faults (S19),
   the nested-sum-payload fence's sibling. *)
let rec struct_value_term (domein, velden) (fs : (string * Value.t) list) : string =
  let f = ctor_functor domein in
  let arg (fname, fdt) =
    match List.find_opt (fun (n, _) -> norm n = norm fname) fs with
    | None -> fault "record value is missing declared field %S" fname
    | Some (_, v) -> (
        match fdt with
        | Struct_type { domein = d2; velden = v2 } -> (
            match v with
            | Value.Record fs2 -> struct_value_term (d2, v2) fs2
            | _ -> fault "a struct-typed field %S expects a record value" fname)
        | Sum_type _ | List_type _ ->
            fault
              "a nested sum/list field value has no integer encoding in the ASP struct term (no \
               corpus case)"
        | _ -> string_of_int (scaled_of_value ~decimals:(decimals_of_datatype (Some fdt)) v))
  in
  Printf.sprintf "%s(%s)" f (String.concat "," (List.map arg velden))

(* the ground term for a constructor VALUE (case emission): payload scaled
   by the constructor's declared payload datatype *)
let ctor_value_term constructoren (tag, (payload : Value.t option)) =
  let f = ctor_functor tag in
  match (ctor_payload_datatype constructoren tag, payload) with
  | None, None -> f
  | Some d, Some v -> Printf.sprintf "%s(%d)" f (scaled_of_value ~decimals:(decimals_of_datatype (Some d)) v)
  | Some _, None -> fault "unary constructor %S carries no payload value" tag
  | None, Some _ -> fault "nullary constructor %S carries a payload value" tag

(* --- the fold family (RUNTIME-SEMANTICS ch. 10; ASP-TRANSLATION S15) ------ *)

(* the scale of a list ELEMENT datatype: numeric elements ride their declared
   decimals, a sum element is a ground constructor term (scale 0 — it carries
   no numeric scale, exactly as a Construct value does in expr_term) *)
let element_scale_of = function
  | Numeriek { decimalen; _ } | Percentage { decimalen; _ } -> pow10 decimalen
  | Datum_in_dagen -> 1
  | Sum_type _ -> 0
  (* a struct element is a ground record term (RUNTIME-SEMANTICS ch. 11.1),
     carrying no numeric scale — the Sum_type sibling *)
  | Struct_type _ -> 0
  | dt ->
      ignore dt;
      fault "a list element datatype without an integer encoding (no corpus case)"

(* the builtin comparison symbol and its exact complement, for an IfThenElse
   condition over BOUND terms inside a closed body: the closed body never sees
   leeg (ch. 10.1), so `then` reads the positive builtin and `else` its
   mirror — no NAF over derived atoms (the S7 closed-body point) *)
let cmp_sym = function
  | Kleiner | Eerder -> "<"
  | Kleiner_of_gelijk | Eerder_of_gelijk -> "<="
  | Groter | Later -> ">"
  | Groter_of_gelijk | Later_of_gelijk -> ">="
  | Gelijk -> "=="
  | Ongelijk -> "!="

let cmp_complement = function
  | Kleiner | Eerder -> ">="
  | Kleiner_of_gelijk | Eerder_of_gelijk -> ">"
  | Groter | Later -> "<="
  | Groter_of_gelijk | Later_of_gelijk -> "<"
  | Gelijk -> "!="
  | Ongelijk -> "=="

(* ch. 10.6 (call-by-need): does evaluating [e] READ the binder [b]? The
   answer decides both halves of the L semantics in this backend — whether the
   value is translated at all (clause 3: an unread binder evaluates nothing)
   and which branches carry its atoms (clause 2: the barrier sits at the read).
   It refines [Ir_v0.Checks.mentions_binder] on exactly the shape the skip
   creates: a nested `Let` whose OWN binder is unread never evaluates its
   value, so a mention buried in that value is not a read — the refinement
   makes this function agree with the translation that consults it. Below a
   node this function does not descend through (a Match arm, a fold body) it
   falls back to the syntactic mention, which is conservative in the safe
   direction: an extra guard atom can only make clingo derive LESS, never a
   different value, and gate 6 would show it as a missing write. *)
let rec reads_binder b (e : expr) : bool =
  let opt = function Some x -> reads_binder b x | None -> false in
  match e with
  | Let { binder; value; body; _ } ->
      (reads_binder binder body && reads_binder b value)
      || (norm binder <> norm b && reads_binder b body)
  | If_then_else { condition; then_; else_; _ } ->
      Ir_v0.Checks.mentions_binder_cond b condition
      || reads_binder b then_ || reads_binder b else_
  | Binair { links; rechts; _ } -> opt links || opt rechts
  | Afgerond { basis; _ } -> opt basis
  | Begrensd { basis; minimum; maximum; _ } -> opt basis || opt minimum || opt maximum
  | _ -> Ir_v0.Checks.mentions_binder b e

(* B3: a Let binder must be SELF-CONTAINED wherever it is read. A bare value
   term (a ListLength count, a Fold result, an attribute read) is bound only by
   its value atoms [va]; the Let keeps those in [rule_guard], but coalesce0 (and
   the other helper-emitting positions) build their rules from the LOCAL atoms
   only, so a bare Let-bound var used inside one grounds UNSAFE (the tutorial's
   `let n = number of individuals` read in the money product). Bind the value
   through a per-subject helper `letN(S,vt) :- is_a(S,ot), va.` — for a
   ListLength that is the count aggregate over lpres/lelem — so every read
   carries the single `letN` atom and the length/value is bound before use. An
   atom-free value (a literal) is already ground and needs no helper. *)
let let_binder ctx ~subject ~subject_ot ((va, vt, vs) : string list * string * int) :
    string list * string * int =
  if va = [] then ([], vt, vs)
  else begin
    ctx.fresh <- ctx.fresh + 1;
    let p = Printf.sprintf "let%d" ctx.fresh in
    let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
    emit ctx "%s(%s,%s) :- %s." p subject vt (String.concat ", " (base :: va));
    let v = fresh ctx "LB" in
    ([ Printf.sprintf "%s(%s,%s)" p subject v ], v, vs)
  end

(* the ch. 1.2d NON-numeric inequality, Tabel 44 (uniform over every datatype):
   ongelijk-aan with EXACTLY ONE empty operand is waar, both empty is onwaar,
   else value inequality. The positive `!=` join used for gelijk-aan (both
   operands must bind) yields any-empty → false and CANNOT express the
   one-empty-true cell, so ongelijk-aan takes the numeric row's negation route:
   a per-comparison helper eqN(S) holds iff both operand bodies bind and the
   ground terms unify, OR neither binds (both empty); ongelijk-aan is `not
   eqN`. eqN reads only the operand bodies (val atoms, ground literals), so the
   NAF is stratified exactly as the numeric eqN is. Equality is untouched: it
   keeps its positive == join (any empty → false, Tabel 38). *)
let neq_helper ctx ~subject ~subject_ot (ll, tl) (rr, tr) : string list =
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "eq%d" ctx.fresh in
  emit ctx "%s_l(%s) :- %s." p subject (String.concat ", " (base :: ll));
  emit ctx "%s_r(%s) :- %s." p subject (String.concat ", " (base :: rr));
  emit ctx "%s(%s) :- %s, %s == %s." p subject (String.concat ", " (base :: (ll @ rr))) tl tr;
  emit ctx "%s(%s) :- %s, not %s_l(%s), not %s_r(%s)." p subject base p subject p subject;
  [ "not " ^ Printf.sprintf "%s(%s)" p subject ]

let rec expr_term ctx ~subject ~subject_ot (e : expr) : string list * string * int =
  match e with
  | Literal { waarde; typ; _ } -> (
      let lex = req "Literal.waarde" waarde in
      match typ with
      | Some (Numeriek _ | Percentage _) | None ->
          let d = lexeme_decimals lex in
          let v = scaled_of_value ~decimals:d (Value.Num (Value.q_of_lexeme lex, Value.Unitless)) in
          ([], string_of_int v, pow10 d)
      | Some Datum_in_dagen -> (
          (* B2: a date literal in a term position is the civil day number,
             scale 1 — the SAME representation the Rekendatum constant, a date
             attribute read (val/3) and the point read use, so a date comparison
             harmonises day numbers directly (no second date encoding). ref
             faults date ARITHMETIC (numeric_ok gates the additive/multiplicative
             families), so this only reaches comparison operands. *)
          match Value.date_of_iso lex with
          | Value.Date z -> ([], string_of_int z, 1)
          | _ -> fault "malformed date literal %S" lex)
      | _ -> fault "non-numeric literal in a term position")
  | Parameter_ref { parameter; _ } ->
      let name = req "ParameterRef.parameter" parameter in
      (match Ir_v0.Checks.find_ci ctx.doc.parameters name with
      | Some { par_granulariteit = Some _; _ } ->
          fault "granular parameter %S in a scalar slot is refused (ch. 3.3/8)" name
      | Some p ->
          let d = decimals_of_datatype p.par_datatype in
          let v = fresh ctx "P" in
          ([ Printf.sprintf "param(%s,%s)" (str name) v ], v, pow10 d)
      | None -> fault "unknown parameter %S" name)
  | Attribuut_ref
      { attribuut; objecttype = ot; navigatie_rol; navigatie_pad; navigatie_bindingen;
        set = false; typ; _ }
    when not (match typ with Some (Datatype_onbekend _) -> true | _ -> false) ->
      let a = req "attribuut" attribuut in
      let ot = req "objecttype" ot in
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "temporal attribute %S in a scalar slot is refused (ch. 3.3)" a;
      let navigated =
        navigatie_rol <> None || navigatie_pad <> [] || norm ot <> norm subject_ot
      in
      if not navigated then begin
        (match navigatie_bindingen with
        | Some _ ->
            fault "navigation bindings on a reference that does not navigate (WF-053)"
        | None -> ());
        let v = fresh ctx "A" in
        (* a sum-/struct-typed attribute holds a ground record/constructor term,
           carrying no numeric scale (value_scale 0) — the whole-value read (a
           struct passed as a subscope input), the struct_base sibling. A numeric
           attribute rides its declared scale. *)
        let scale =
          if attr_sum_typed ctx ~otype:ot ~attr:a || attr_struct_typed ctx ~otype:ot ~attr:a then 0
          else pow10 (attr_decimals ctx ~otype:ot ~attr:a)
        in
        ([ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ], v, scale)
      end
      else begin
        (* the navigated read (ch. 5.8): one rol/3 body join per to-one
           hop — an absent link leaves the body underivable, which IS the
           leeg-on-absent-relation rule (leeg as absence, for free). The
           hop roles resolve exactly as in ref/ (shared helpers), so the
           two executors cannot drift on the walk. *)
        let env =
          { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
            as_of = None; locals = Eval.SMap.empty }
        in
        let roles =
          match navigatie_bindingen with
          | Some rbs -> List.map (Eval.bound_role env) rbs
          | None -> (
              match navigatie_pad with
              | _ :: _ ->
                  let last = List.length navigatie_pad - 1 in
                  let step (i, cur, acc) name =
                    let name = req "navigation path step" name in
                    let to_type = if i = last then Some ot else None in
                    let r = Eval.role_by_name ?to_type env ~self_type:cur ~name in
                    (i + 1, Option.value r.rol_objecttype_naam ~default:"", acc @ [ r ])
                  in
                  let _, _, roles =
                    List.fold_left step (0, subject_ot, []) navigatie_pad
                  in
                  roles
              | [] -> (
                  match navigatie_rol with
                  | Some n -> (
                      (* single written step, shared with ref: a far role
                         when one resolves, the zero-hop self-reference
                         when none does and the name denotes the subject
                         itself (ch. 4.1a) — then no rol/3 join at all,
                         the val atom reads the subject directly *)
                      match
                        Eval.nav_single_step env ~self_type:subject_ot ~endpoint:ot ~name:n
                      with
                      | Some r -> [ r ]
                      | None -> [])
                  | None -> [ Eval.connecting_role env ~from_type:subject_ot ~to_type:ot ]))
        in
        List.iter
          (fun (r : rol) ->
            if r.rol_meervoudig then
              fault
                "navigation over the to-many role %S in a scalar slot is refused (WF-053)"
                (Option.value r.rol_naam ~default:"?"))
          roles;
        let atoms, target =
          List.fold_left
            (fun (atoms, cur) (r : rol) ->
              let n = fresh ctx "N" in
              ( atoms @ [ Printf.sprintf "rol(%s,%s,%s)" cur (str (role_canonical r)) n ],
                n ))
            ([], subject) roles
        in
        let v = fresh ctx "A" in
        ( atoms @ [ Printf.sprintf "val(%s,%s,%s)" target (str a) v ],
          v,
          pow10 (attr_decimals ctx ~otype:ot ~attr:a) )
      end
  | Functie { kind = Aantal; argumenten = [ Attribuut_ref { attribuut; typ = Some (Datatype_onbekend _); navigatie_rol; navigatie_pad = []; _ } ]; element_objecttype = Some el; bron_rol; filter; _ }
    when (match navigatie_rol with
         | None -> true
         | Some nr ->
             (* the self-denoting source (WF-083): the written form names
                the fold subject by its own role name — presentation, not
                navigation; the count stays subject-anchored (the ref
                mirror). An away-pointing role falls through to the loud
                translation fault. *)
             Eval.self_denoting_role ctx.doc ~self_type:subject_ot nr) ->
      (* the row-1 wart: the fold source names a role; count the linked
         instances. An explicit binding overrides the written name. The
         die/dat subselection joins per contributor: the filter atoms
         range over the element variable (ch. 5.8-family reading — an
         unbound filter operand excludes the contributor for free). *)
      let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty } in
      let role =
        role_canonical
          (match bron_rol with
          | Some rb -> Eval.bound_role env rb
          | None -> (
              (* resolve-or-fault (IR-SPEC ch. 4.3), never the raw lexeme
                 as the rol/3 key: the written name must resolve against
                 declared roles (name or plural, ci) or fall back to the
                 unique connecting fact type — the same order ref/ walks.
                 The count then keys the CONNECTING role's canonical name,
                 exactly like the som arm below: the raw-lexeme key
                 silently counted 0 whenever the written form differed
                 from the declared plural that keys rol/3 (the singular
                 of a to-many role, or an undeclared synthesized plural). *)
              let name = req "fold role" attribuut in
              match Eval.reachable_roles_by_name env ~self_type:subject_ot ~name with
              | [] | [ _ ] -> Eval.connecting_role env ~from_type:subject_ot ~to_type:el
              | _ -> fault "ambiguous role name %S from %S" name subject_ot))
      in
      let v = fresh ctx "C" in
      let x = Printf.sprintf "X%s" v in
      let fatoms =
        match filter with
        | None -> []
        | Some c -> cond_atoms ctx ~subject:x ~subject_ot:el c
      in
      ( [ Printf.sprintf "%s = #count { %s : %s }" v x
            (String.concat ", " (Printf.sprintf "rol(%s,%s,%s)" subject (str role) x :: fatoms)) ],
        v,
        1 )
  (* The set folds below carry navigatie_rol/navigatie_pad but deliberately
     NOT the temporal total's ci-control [norm aot = norm subject_ot], and
     not its sister [norm aot = norm element_objecttype] either: they never
     emit val(<subject>,…) — every read is anchored at the ELEMENT variable,
     with the attribute's declaration looked up on [el], exactly as ref
     anchors it (its set-source arm binds no objecttype either). The
     operand's own [objecttype] is therefore INERT on both sides, so a
     control on it could only refuse forms ref answers. Measured on
     jutterswet-vondsttelling's shape and on jutterswet-vondstpiek, twisting
     the operand's objecttype to a name-sharing sister type, with and
     without the explicit bron_rol binding: ref and clingo both keep
     answering the pinned value. What the twist CAN break — an
     element_objecttype whose type does not declare the folded attribute —
     is already loud on both sides (a case-load error, or attr_decimals'
     "unknown attribute"), never a silent value. The Aantal arm above is the
     same story one field over: it binds no objecttype at all and mirrors
     ref's role-source arm field for field. *)
  | Functie { kind = (Som | Totaal) as kind; argumenten = [ Attribuut_ref { attribuut; set = true; navigatie_rol = None; navigatie_pad = []; _ } ]; element_objecttype = Some el; leegwaarde; bron_rol; filter; _ } ->
      (* ONE branch for the two set folds, because ref has one: its
         contributor gathering is shared and only the empty-set rule
         differs (agg_som ~fallback vs agg_totaal). The set reading of
         "het totaal van" IS the som-alias — same operand, same result
         type on the wire (corpus/golden/jutterswet-tempototaal pins both
         rows of that table side by side) — so a mirrored pair of arms
         could only drift. *)
      let a = req "fold attribute" attribuut in
      (* THE TEMPORALITY ANCHOR: [el], the elementObjecttype (see
         [fold_operand_otype]). Every read below hangs on the element
         variable and every declaration is looked up on [el], so [el] is
         the type that decides whether the members carry timelines. *)
      let temporal = attr_temporal ctx ~otype:el ~attr:a in
      (* the empty set. TOTAAL is 0 UNCONDITIONALLY (§4.20, ref
         agg_totaal), which is exactly clingo's native #sum over no
         tuples — so no presence guard, and a leegwaarde is ignored the
         way ref ignores it (agg_totaal takes no fallback; the eager
         `let fallback = … leegwaarde` evaluates and discards it, the
         same convention the min/max arm below carries). SOM without a
         fallback is ref's Empty (agg_som None), so it needs the presence
         guard that keeps the term underivable on the empty set; with a 0
         fallback the bare #sum is already right. A fallback other than 0
         stays loud — ref would add it, which no encoding here carries. *)
      let has_fallback =
        match kind with
        | Totaal -> true
        | _ -> (
            match leegwaarde with
            | Some (Literal { waarde = Some "0"; _ }) -> true
            | Some _ -> fault "som fallback other than 0 is not translated yet"
            | None -> false)
      in
      (* the connecting role: the node's explicit binding when present,
         else derived the same way ref/ derives it *)
      let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty } in
      let role_name =
        role_canonical
          (match bron_rol with
          | Some rb -> Eval.bound_role env rb
          | None -> Eval.connecting_role env ~from_type:subject_ot ~to_type:el)
      in
      let v = fresh ctx "S" in
      let g = fresh ctx "G" and x = fresh ctx "X" in
      let fatoms =
        match filter with
        | None -> []
        | Some c -> cond_atoms ctx ~subject:x ~subject_ot:el c
      in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      let contrib_body, sum_shape =
        if not temporal then begin
          (* the SCALAR set fold: one val/3 per member, the join IS the
             empty-skip (an absent member contributes no tuple, ref's
             [filled]). The #sum accumulation stays inside 2^31 by
             bounding both the contributors and their count:
             46340 × 46340 < 2^31. *)
          let body =
            Printf.sprintf "rol(%s,%s,%s)" subject (str role_name) x
            :: Printf.sprintf "val(%s,%s,%s)" x (str a) g
            :: fatoms
          in
          range_guard ctx ~subject ~body:((base :: ctx.rule_guard) @ body) [ (g, 46340) ];
          emit ctx
            "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",%s) :- %s, \
             46340 < #count { %s : %s }."
            subject
            (String.concat ", " (base :: ctx.rule_guard))
            x
            (String.concat ", " body);
          (body, (g, x))
        end
        else begin
          (* §7.1, the PER-ELEMENT timeline fold: a set read of a TEMPORAL
             attribute folds TWICE — each member's own timeline collapses
             over time first (ch. 3.6, at that attribute's granularity),
             and only those scalars fold over the set. Summing over
             (member × segment) pairs in ONE aggregate is the same number:
             Σ_members Σ_segments = Σ_pairs, and neither fold can be empty
             in a way the other hides (an absent member brings no tuple, a
             leeg segment carries no tl fact).

             Tabel 30 as arithmetic: each segment contributes value ×
             WHOLE grid units of its own span. The calendar enters as
             DATA — wu facts computed by the shared Timeline module over
             the case's own segment bounds — so the logic stays in ASP and
             the calendar cannot drift from ref/, exactly the gedurende
             clip's convention below. *)
          let gran = attr_granularity ctx ~otype:el ~attr:a in
          grid_strip_ok ctx
            ~what:(Printf.sprintf "the per-element temporal fold of %S" a)
            ~gran (attr_unit ctx ~otype:el ~attr:a);
          (* the segment bounds must be CASE data: a member timeline some
             rule derives has no bounds at translation time, so the wu
             facts could not be computed and the join would find nothing —
             a silent 0 where ref integrates. Refused by name instead.

             EVERY derived writer of that member attribute, not just a
             rule: an ALLOCATION (verdeling) and a DEFAULTS GROUP write
             into the same slot. Measured 2026-09-05 on a verifier-PASS
             document — a GELIJKE_DELEN verdeling onto the folded
             temporal attribute, the case supplying no input for it — ref
             met the derived value as a member's horizonless scalar and
             faulted by name, while this arm found no tl fact and answered
             a silent 0. Both are loud now. The defaults arm is
             defense-in-depth: WF-086 already rejects a temporal defaults
             target at the verifier, so no verifier-clean document reaches
             it, but the silent 0 was measured there too (on a WF-086 FAIL
             document) and one scan is cheaper than trusting a
             neighbouring gate. *)
          let derived_writer =
            if
              List.exists
                (fun (r : resolved_regel) ->
                  match r.doel with
                  | Some (Attribuut_ref { attribuut = Some x; objecttype = Some o; _ }) ->
                      norm x = norm a && norm o = norm el
                  | _ -> false)
                ctx.doc.resolved_regels
            then Some "a rule"
            else if
              List.exists
                (fun (v : resolved_verdeling) ->
                  (match v.doel_attribuut with Some x -> norm x = norm a | None -> false)
                  && match v.doel_type with Some o -> norm o = norm el | None -> false)
                ctx.doc.verdelingen
            then Some "an allocation (verdeling)"
            else if
              List.exists
                (fun (l : prioritized_defaults) ->
                  (match l.pd_attribuut with Some x -> norm x = norm a | None -> false)
                  && norm l.pd_onderwerp = norm el)
                ctx.doc.prioritized_defaults
            then Some "a defaults group"
            else None
          in
          (match derived_writer with
          | None -> ()
          | Some who ->
              fault
                "the per-element temporal fold of %S over a DERIVED member timeline is not \
                 translated: %s writes %S on %S, so its segment bounds are not case data and \
                 the whole-unit facts this fold integrates against cannot be computed (no corpus \
                 case)"
                a who a el);
          (* the horizon rule (ch. 3.6/§7.1, ch. 8.4), ref's convention
             verbatim: the integration runs PER MEMBER, so every member
             must bring its OWN horizon. The asOfDate-bucket SCALAR states
             no period at all, so a member carrying one is refused BY NAME
             — never silently bucketed at the session date. An ABSENT
             member contributes nothing (leeg): its join simply finds no
             tl fact, which is ref's Empty contributor dropping out. The
             SUBJECT's own fold keeps the opposite convention (the
             asOfDate bucket carries that total) and the asymmetry is
             deliberate — there the session date IS the horizon.

             The fence walks the ACTUAL MEMBERS, through ref's own
             [linked], not every instance of the element type: a
             disconnected instance is no contributor, and ref answers 0
             (an empty totaal) where a type-wide fence would have
             refused. Measured on exactly that shape — a Vondst subject
             folding over "de bewarende jutter" whose link is declared on
             the Jutter side, so the walk finds no member — ref says 0
             and this arm must say 0 too. The residual edge is the
             die/dat FILTER: ref drops a filtered-out member before the
             integration, and a filter is not decidable at translation
             time, so a filtered-out member carrying a bucket scalar is
             refused here where ref would answer. Loud, recorded, and
             without a corpus case. *)
          List.iter
            (fun (i : Uchar_ref.World.instance) ->
              if
                Uchar_ref.World.timeline i a = None
                && Uchar_ref.World.attr i a <> Value.Empty
              then
                fault
                  "the per-element temporal fold of %S over %S: the case supplies an \
                   asOfDate-bucket scalar, which carries no period of its own; a set fold needs \
                   each member's own timeline (ch. 3.6/§7.1, ch. 8.4)"
                  a i.label)
            (Eval.SMap.fold
               (fun _ (self : Uchar_ref.World.instance) acc ->
                 if norm self.otype <> norm subject_ot then acc
                 else Eval.linked ?binding:bron_rol env ctx.case.world self ~to_type:el @ acc)
               ctx.case.world.instances []);
          (* one entry PER (member, segment) — the multiplicity is
             load-bearing for the fence below, which must count the
             tuples the #sum accumulates, not the distinct calendar
             spans among them. *)
          let spans =
            Eval.SMap.fold
              (fun _ (i : Uchar_ref.World.instance) acc ->
                if norm i.otype <> norm el then acc
                else
                  match Uchar_ref.World.timeline i a with
                  | None -> acc
                  | Some tl ->
                      List.map
                        (fun (s : Uchar_ref.Timeline.segment) -> (s.s_van, s.s_tot))
                        tl
                      @ acc)
              ctx.case.world.instances []
          in
          ctx.fresh <- ctx.fresh + 1;
          let p = Printf.sprintf "pe%d" ctx.fresh in
          (* the wrap fence, compile-time on the calendar side and
             runtime on the value side. The #sum runs over (member,
             segment) TUPLES, so the normative requirement is

               max|g| × Σ n ≤ 2^31 − 1   over the (member, segment) tuples,

             and bounding both factors at 46340 = isqrt(2^31 − 1) — the
             floor, one unit below the real root, so deliberately
             conservative — gives 46340² = 2_147_395_600 ≤ 2^31 − 1.
             COUNT WITH MULTIPLICITY: 150 members sharing one 360-month
             span are 54_000 tuples, not 360, and deduplicating here let
             a verifier-PASS document wrap silently (measured:
             150 × 399.99 €/mnd × 360 mnd reported PASS on
             −21_350_212.96 where ref and Drools give 21_599_460.00).
             The DEDUPLICATION BELOW IS THE FACT EMISSION'S ALONE — the
             wu facts are calendar facts, one per distinct span, and a
             repeated span would only re-derive the same atom. Σ n is
             taken over ALL instances of the element type, which
             over-counts a single subject's share — conservative in the
             safe direction, and loud.
             The sister calendars (the gedurende clip and
             [emit_bucket_calendar]) dedupe ENDPOINTS and then bound the
             single endpoint SPAN, never a sum of per-member segments;
             both were checked against this requirement and are sound as
             written. *)
          let total_units =
            List.fold_left
              (fun acc (van, tot) -> acc + Uchar_ref.Timeline.whole_units gran van tot)
              0 spans
          in
          if total_units > 46340 then
            fault
              "the per-element temporal fold of %S covers %d whole units across the case's \
               member timelines, beyond the guarded sum range"
              a total_units;
          List.iter
            (fun (van, tot) ->
              emit ctx "%s_wu(%d,%d,%d)." p van tot
                (Uchar_ref.Timeline.whole_units gran van tot))
            (List.sort_uniq compare spans);
          let n = fresh ctx "N" and vv = fresh ctx "V" and tt = fresh ctx "T" in
          let body =
            Printf.sprintf "rol(%s,%s,%s)" subject (str role_name) x
            :: Printf.sprintf "tl(%s,%s,%s,%s,%s)" x (str a) vv tt g
            :: Printf.sprintf "%s_wu(%s,%s,%s)" p vv tt n
            :: fatoms
          in
          range_guard ctx ~subject ~body:((base :: ctx.rule_guard) @ body) [ (g, 46340) ];
          (body, (Printf.sprintf "%s*%s" g n, Printf.sprintf "%s,%s,%s" x vv tt))
        end
      in
      let sum_term, sum_key = sum_shape in
      (* the presence guard for the fallback-less som: a helper keyed by the
         subject holds iff at least one contributor exists; the reading rule
         carries it, so an empty set leaves the whole term underivable (ref's
         Empty) instead of deriving #sum's 0. With a 0 fallback — and for
         every totaal — the bare #sum (native 0 on the empty set) is exactly
         right, so no guard is added. *)
      let guard =
        if has_fallback then []
        else begin
          ctx.fresh <- ctx.fresh + 1;
          let p = Printf.sprintf "somp%d" ctx.fresh in
          emit ctx "%s(%s) :- %s." p subject (String.concat ", " (base :: contrib_body));
          [ Printf.sprintf "%s(%s)" p subject ]
        end
      in
      ( guard
        @ [ Printf.sprintf "%s = #sum { %s,%s : %s }" v sum_term sum_key
              (String.concat ", " contrib_body) ],
        v,
        pow10 (attr_decimals ctx ~otype:el ~attr:a) )
  (* element-anchored like the som arm above, and unnarrowed on the
     operand's objecttype for the reason measured there *)
  | Functie { kind = (Minimum | Maximum) as kind; argumenten = [ Attribuut_ref { attribuut; set = true; navigatie_rol = None; navigatie_pad = []; _ } ]; element_objecttype = Some el; bron_rol; filter; _ } ->
      (* MAXIMUM/MINIMUM over a set-typed AttribuutRef (RUNTIME-SEMANTICS
         ch. 1.2e, ref agg_max/agg_min = agg_extreme): the same contributor
         gathering as the som arm, aggregated with #max/#min at the element
         attribute's scale. Empty contributors contribute no tuple (the join is
         the skip, exactly as the som), and the extreme is the max/min of the
         contributed values. No fence: unlike #sum, min/max never accumulate —
         the result IS one contributed value, already inside 2^31 by the load
         fence. Unlike the listed eerste/laatste fold there is no scale
         harmonisation (one attribute, one scale, one unit), so no per-operand
         multiplication and no range guard. A leegwaarde on min/max is ignored
         here: agg_max/agg_min take no fallback, so a BENIGN leegwaarde is
         evaluated-and-discarded by ref (eval.ml's eager `let fallback = …
         leegwaarde`, computed before the kind match) and both agree. The one
         residual asymmetry — a leegwaarde whose OWN evaluation would fault, so
         ref faults while this arm yields the extreme — has no v0 witness and no
         frontend path onto min/max; faulting-on-present would instead diverge
         on a benign leegwaarde, so ignore is the ref-faithful default for the
         representable case. The presence guard is MANDATORY: #max/#min over the
         empty set yields #inf/#sup, which must never escape into a value — ref
         derives Empty there. *)
      let a = req "fold attribute" attribuut in
      (* a TEMPORAL element attribute has no defined moment to rank at, so
         ref refuses the form outright (ch. 3.6: only som and totaal carry
         the per-element timeline fold). This arm would rank the asOfDate
         buckets instead — an answer where the other executor refuses.
         Refuse in step. The guard hangs on [el], the anchor
         ([fold_operand_otype]) — where it always hung, and where the
         som/totaal arm above and the temporal-totaal refusal arm below
         now hang theirs too. This is the one set fold that STAYS refused
         after 2026-09-05, because ref refuses it. *)
      if attr_temporal ctx ~otype:el ~attr:a then
        fault
          "a minimum/maximum over the temporal attribute %S of a set has no defined moment to \
           fold (ch. 3.6); ref refuses the form, so this arm must not answer from the asOfDate \
           buckets"
          a;
      let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty } in
      let role_name =
        role_canonical
          (match bron_rol with
          | Some rb -> Eval.bound_role env rb
          | None -> Eval.connecting_role env ~from_type:subject_ot ~to_type:el)
      in
      let v = fresh ctx "M" in
      let g = fresh ctx "G" and x = fresh ctx "X" in
      let fatoms =
        match filter with
        | None -> []
        | Some c -> cond_atoms ctx ~subject:x ~subject_ot:el c
      in
      let contrib_body =
        Printf.sprintf "rol(%s,%s,%s)" subject (str role_name) x
        :: Printf.sprintf "val(%s,%s,%s)" x (str a) g
        :: fatoms
      in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      ctx.fresh <- ctx.fresh + 1;
      let p = Printf.sprintf "extp%d" ctx.fresh in
      emit ctx "%s(%s) :- %s." p subject (String.concat ", " (base :: contrib_body));
      let agg = match kind with Minimum -> "#min" | _ -> "#max" in
      ( [ Printf.sprintf "%s(%s)" p subject;
          Printf.sprintf "%s = %s { %s : %s }" v agg g (String.concat ", " contrib_body) ],
        v,
        pow10 (attr_decimals ctx ~otype:el ~attr:a) )
  | Functie { kind = (Eerste | Laatste) as kind; argumenten = (_ :: _ :: _ as args); filter; _ }
    ->
      (* the articulated eerste/laatste fold (RUNTIME-SEMANTICS ch. 1.2e):
         the ORDERING fold — #min/#max over the listed operand terms with
         the empty-skip discipline. One helper predicate per fold site:
         each operand derives its harmonised value into the shared head;
         an empty operand derives nothing and drops out of the aggregate
         set; the definedness atom keeps the fold's body underivable when
         NO operand derives — all-empty stays leeg as absence, never a
         #sup/#inf escaping into a value. Dispatch is by argument shape
         (≥ 2 listed operands): the vestigial elementObjecttype v0 stamps
         on a listed fold names no instance source (the ref mirror). *)
      if filter <> None then
        fault "a filter on a listed-operand fold has no v0 form (the subselection ranges \
               over instances)";
      (* one comparable family only: ref's ordering comparison faults a
         mixed pair at runtime — a day number must not silently order
         against a scaled numeric *)
      let dated = List.map (fun a -> is_date_operand (Some a)) args in
      if List.exists Fun.id dated && not (List.for_all Fun.id dated) then
        fault "mixed date/non-date operands under an eerste/laatste fold are not \
               translated (ref faults the unordered comparison)";
      List.iter
        (fun a ->
          if is_symbolic_operand (Some a) then
            fault "a text/boolean/enum operand under an eerste/laatste fold has no order \
                   (ref faults the unordered comparison)";
          match Ir_v0.Checks.typ_of_expr a with
          | Some Datum_tijd ->
              fault "datum-tijd eerste/laatste is not translated (the pair term has no fold \
                     ordering; no corpus case)"
          | _ -> ())
        args;
      (* ref restates numeric operands into the first operand's unit with
         exact factors this integer encoding does not carry — identical
         (or absent, adopting) units only, the additive-join fence *)
      let first_unit =
        List.fold_left
          (fun acc a -> match acc with Some _ -> acc | None -> eff_unit ctx a)
          None args
      in
      List.iter
        (fun a ->
          units_identical_ok ~what:"an eerste/laatste fold" first_unit (eff_unit ctx a))
        args;
      (* the operand fence above reads the wire `type`, which covers the ch.
         1.2d scalars but never saw a sum/struct operand — and could not, since
         an absent or lying annotation walks past it either way. The scale is
         the declaration's own, so it fences what the annotation cannot: an
         unscaled operand here divided by 0 while harmonising, and crashed. *)
      let ops =
        List.map
          (fun a ->
            let atoms, t, s = expr_term ctx ~subject ~subject_ot a in
            (atoms, t, numeric_scale ~what:"an eerste/laatste fold" s))
          args
      in
      let m = List.fold_left (fun acc (_, _, s) -> max acc s) 1 ops in
      ctx.fresh <- ctx.fresh + 1;
      let pred = Printf.sprintf "fl%d" ctx.fresh in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      List.iter
        (fun (atoms, t, s) ->
          let body = base :: atoms in
          (* the fence carries [rule_guard] like its siblings in the min/max
             folds; the fold's own value rule keeps the bare body, which is the
             sibling convention exactly *)
          if m / s > 1 then
            range_guard ctx ~subject ~body:((base :: ctx.rule_guard) @ atoms)
              [ (t, 1073741823 / (m / s)) ];
          let t' = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
          emit ctx "%s(%s,%s) :- %s." pred subject t' (String.concat ", " body))
        ops;
      emit ctx "%s_def(%s) :- %s(%s,X)." pred subject pred subject;
      let v = fresh ctx "F" in
      let x = Printf.sprintf "X%s" v in
      let agg = match kind with Eerste -> "#min" | _ -> "#max" in
      ( [ Printf.sprintf "%s_def(%s)" pred subject;
          Printf.sprintf "%s = %s { %s : %s(%s,%s) }" v agg x pred subject x ],
        v,
        m )
  | Functie { kind = Totaal; argumenten = [ Attribuut_ref { attribuut = Some a; objecttype = Some aot; navigatie_rol = None; navigatie_pad = []; set = false; _ } ]; filter = Some c; _ }
    when norm aot = norm subject_ot && attr_temporal ctx ~otype:aot ~attr:a ->
      (* ch. 3.6, the gedurende clip: intersect the value timeline (tl/5)
         with the condition's true-periods, then total whole units per
         clipped segment. The intersection is pure integer max/min (four
         explicit cases — old grounders lack assignment aggregates); the
         CALENDAR (whole units per clipped span) enters as data: wu
         facts computed by the shared Timeline module over the finite
         cut set of the case's timeline endpoints, so the logic stays in
         ASP and the calendar cannot drift from ref/. A fully
         clipped-away or absent timeline sums to 0 — the #sum over the
         empty set, exactly the mirrored v0 nuance. *)
      (* the clip needs full timelines where the attribute is carried at
         all — an ABSENT attribute clips to the empty sum (0, the v0
         nuance), but a bucket scalar would read silently wrong *)
      Eval.SMap.iter
        (fun _ (i : Uchar_ref.World.instance) ->
          if
            Ir_v0.Checks.norm i.otype = Ir_v0.Checks.norm aot
            && Uchar_ref.World.timeline i a = None
            && Uchar_ref.World.attr i a <> Value.Empty
          then
            fault
              "the gedurende clip of %S needs the full timeline; %S supplies only the                asOfDate-bucket scalar"
              a i.label)
        ctx.case.world.instances;
      let gran = attr_granularity ctx ~otype:aot ~attr:a in
      grid_strip_ok ctx
        ~what:(Printf.sprintf "the gedurende-clipped total of %S" a)
        ~gran (attr_unit ctx ~otype:aot ~attr:a);
      ctx.fresh <- ctx.fresh + 1;
      let p = Printf.sprintf "cl%d" ctx.fresh in
      (* the condition's period atoms, as a function of the variable
         pair so a composite can join members under distinct names.
         A composite ALLE member list intersects pairwise through
         helper predicates — the same four-case integer max/min the
         final value intersection uses (ch. 3.6: v0 nests per-member
         clips; intersection is associative). *)
      let emit_period_and q a1 a2 =
        List.iter
          (fun (vcmp, vpick, tcmp, tpick) ->
            emit ctx "%s(S,%s,%s) :- %s, %s, %s, %s, %s < %s." q vpick tpick (a1 "V1" "T1")
              (a2 "V2" "T2") vcmp tcmp vpick tpick)
          [ ("V1 >= V2", "V1", "T1 <= T2", "T1");
            ("V1 >= V2", "V1", "T1 > T2", "T2");
            ("V1 < V2", "V2", "T1 <= T2", "T1");
            ("V1 < V2", "V2", "T1 > T2", "T2") ]
      in
      let rec cond_fn c : string -> string -> string =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie; _ } ->
            if
              not
                (List.exists
                   (fun (n, _) -> norm n = norm k)
                   (objecttype ctx aot).kenmerk_granulariteit)
            then
              (* mirror ref's fault, as the two sister folds already do
                 (pro rata WF-056, day count WF-058). "Temporal" is decided
                 exactly as ref decides it — KEY EXISTENCE in
                 kenmerkGranulariteit, ref/lib/eval.ml:80-81 — and WF-055
                 reads the same predicate, so the three walk in step. ktl
                 facts come from the case's characteristic timelines alone,
                 and a characteristic with no granularity KEY is refused a
                 timeline at case load, so the join finds nothing, the
                 intersection is empty and the clipped total a silent 0 (a
                 hunted gap). A key present with a NULL value is the other
                 half of that class: all three readers pass it while the
                 case loader still refuses the timeline (on the value), so
                 the silent 0 survives there — a recorded flank with its own
                 card, deliberately not this gate's business. *)
              fault "gedurende over %S, which is not a temporal characteristic (WF-055)" k;
            if not negatie then fun v t -> Printf.sprintf "ktl(S,%s,%s,%s)" (str k) v t
            else begin
              (* ch. 3.6, the NEGATED clip: the complement of the
                 characteristic's periods within the clip horizon. ref takes
                 that horizon to be the clipped attribute's OWN value
                 timeline and walks the gaps per value segment
                 (ref/lib/eval.ml, `span`; ref/lib/timeline.ml,
                 `complement`), so these rules derive one atom per maximal
                 uncovered stretch INSIDE a single tl/5 segment. Deriving it
                 over the union of the segments would land on the same
                 fold — the value intersection below re-cuts on tl anyway —
                 but the per-segment form is what ref states, so it is what
                 is translated.

                 A gap's two edges can only be a value-segment edge or the
                 edge of a covered period: ref's walk emits `sv`, `st` and
                 the clipped `v`/`t` of a period, and nothing else. Both
                 sources already sit in the wu cutset below, so the calendar
                 facts the complement segments need are emitted unchanged —
                 the negation adds no endpoint.

                 Maximality is carried by the two candidate rules rather
                 than by an aggregate. A left edge X is either the segment
                 start or the end of a period that began before X, and a
                 right edge Y is either the segment end or the start of a
                 period that runs past Y; extending an uncovered (X,Y) in
                 either direction therefore always meets a period and is
                 refused by _cov. Value-timeline segments are disjoint, so
                 no other segment's edge can sit strictly inside (SV,ST) and
                 offer a shorter, non-maximal pair.

                 The negation is stratified: _cov is defined by ktl/4 case
                 facts and the two candidate predicates over tl/5, none of
                 which reads this helper back. clingo grounds one stable
                 model, as everywhere else in this translation. *)
              ctx.fresh <- ctx.fresh + 1;
              let q = Printf.sprintf "cl%d_not" ctx.fresh in
              emit ctx "%s_lo(S,X) :- tl(S,%s,X,_,_)." q (str a);
              emit ctx "%s_lo(S,X) :- ktl(S,%s,V,X), V < X." q (str k);
              emit ctx "%s_hi(S,Y) :- tl(S,%s,_,Y,_)." q (str a);
              emit ctx "%s_hi(S,Y) :- ktl(S,%s,Y,T), Y < T." q (str k);
              emit ctx
                "%s_cov(S,X,Y) :- %s_lo(S,X), %s_hi(S,Y), X < Y, ktl(S,%s,V,T), V < Y, X < T." q q
                q (str k);
              emit ctx
                "%s(S,X,Y) :- tl(S,%s,SV,ST,_), %s_lo(S,X), %s_hi(S,Y), SV <= X, X < Y, Y <= ST, \
                 not %s_cov(S,X,Y)."
                q (str a) q q q;
              fun v t -> Printf.sprintf "%s(S,%s,%s)" q v t
            end
        | Vergelijking
            { op; links = Some (Attribuut_ref
                { attribuut = Some b; objecttype = Some botn; navigatie_rol = None;
                  navigatie_pad = []; set = false; _ });
              rechts = Some grens; periode = None; _ }
          when norm botn = norm aot && attr_temporal ctx ~otype:botn ~attr:b ->
            units_identical_ok ~what:"a clip comparison"
              (attr_unit ctx ~otype:botn ~attr:b)
              (eff_unit ctx grens);
            let gb, gt, gs = expr_term ctx ~subject ~subject_ot grens in
            let gs = numeric_scale ~what:"a gedurende clip comparison" gs in
            let bscale = pow10 (attr_decimals ctx ~otype:botn ~attr:b) in
            let m = max bscale gs in
            let wl = if bscale = m then "WB" else Printf.sprintf "(WB*%d)" (m / bscale) in
            let gl = if gs = m then gt else Printf.sprintf "(%s*%d)" gt (m / gs) in
            (* The FENCE is keyed on the caller's subject, not on the clip
               helper's own "S". Two reasons, and the second is a ch. 5.10
               correctness bug the first hides:

               1. The body already mixed the two — `tl(S,…)` binds the helper
                  variable while [gb] (the bound's own atoms) speaks of the
                  CALLER's subject, so the fence grounded over a cross product
                  of every clip subject with every bound subject.
               2. The clip is an EXPRESSION (a totalled Functie with a filter),
                  so it can sit in an operand of a member of some OTHER
                  composite — and then this fence must carry that composite's
                  prefix like every sibling does. ch. 5.10 boundary 2 exempts
                  the clip's OWN members; it says nothing about the clip as an
                  operand, which is an ordinary read. Without the guard a range
                  viol fires out of a member the outcome had already settled
                  away: measured ref PASS vs clingo UNSATISFIABLE, from
                  ordinary frontend source (jutterswet-klipwacht).

               Substituting [subject] for "S" and adding the is_a base gives
               the fence the same shape every other site has. *)
            let sub_base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
            range_guard ctx ~subject
              ~body:
                (((sub_base :: ctx.rule_guard)
                 @ [ Printf.sprintf "tl(%s,%s,V2,T2,WB)" subject (str b) ])
                @ gb)
              [ ("WB", max_int31 / (m / bscale)); (gt, max_int31 / (m / gs)) ];
            ctx.fresh <- ctx.fresh + 1;
            let q = Printf.sprintf "cl%d_cond" ctx.fresh in
            emit ctx "%s(S,V2,T2) :- %s." q
              (String.concat ", "
                 (Printf.sprintf "tl(S,%s,V2,T2,WB)" (str b)
                 :: gb
                 @ [ Printf.sprintf "%s %s %s" wl (vergelijk_sym op) gl ]));
            fun v t -> Printf.sprintf "%s(S,%s,%s)" q v t
        | Samengesteld { kwantor = Alle; onderdelen = (_ :: _ as onderdelen); _ } ->
            (* the FLAT composite only (WF-055): v0 partitions static
               comparison members out as value-side guards before its
               clip runs and markers nested bullets — v1 narrows
               composite members to characteristic checks *)
            let member_fn part =
              match part with
              | Kenmerk_check _ -> cond_fn part
              | _ ->
                  fault
                    "a composite gedurende member is an own temporal characteristic check \
                     (WF-055; the static-guard and nested forms are recorded flanks)"
            in
            List.fold_left
              (fun acc part ->
                let pf = member_fn part in
                ctx.fresh <- ctx.fresh + 1;
                let q = Printf.sprintf "cl%d_and" ctx.fresh in
                emit_period_and q acc pf;
                fun v t -> Printf.sprintf "%s(S,%s,%s)" q v t)
              (member_fn (List.hd onderdelen))
              (List.tl onderdelen)
        | _ ->
            fault
              "unsupported gedurende condition shape (WF-055: an own temporal \
               characteristic, a per-segment comparison of an own temporal attribute, or \
               an ALLE composite of such shapes)"
      in
      let cond_atom = cond_fn c "V2" "T2" in
      (* four-case integer intersection: V = max(V1,V2), T = min(T1,T2) *)
      List.iter
        (fun (vcmp, vpick, tcmp, tpick) ->
          emit ctx "%s_seg(S,%s,%s,W) :- tl(S,%s,V1,T1,W), %s, %s, %s, %s < %s." p vpick
            tpick (str a) cond_atom vcmp tcmp vpick tpick)
        [ ("V1 >= V2", "V1", "T1 <= T2", "T1");
          ("V1 >= V2", "V1", "T1 > T2", "T2");
          ("V1 < V2", "V2", "T1 <= T2", "T1");
          ("V1 < V2", "V2", "T1 > T2", "T2") ];
      (* the calendar as data: whole units per candidate clipped span *)
      let endpoints =
        Eval.SMap.fold
          (fun _ (i : Uchar_ref.World.instance) acc ->
            let tl_pts =
              Eval.SMap.fold
                (fun _ (tl : Uchar_ref.Timeline.t) acc ->
                  List.concat_map
                    (fun (s : Uchar_ref.Timeline.segment) -> [ s.s_van; s.s_tot ])
                    tl
                  @ acc)
                i.tls acc
            in
            Eval.SMap.fold
              (fun _ periods acc ->
                List.concat_map (fun (v, t) -> [ v; t ]) periods @ acc)
              i.ctls tl_pts)
          ctx.case.world.instances []
        |> List.sort_uniq compare
      in
      (* a DERIVED timeline (a pro-rata target) carries the fold's
         interior bucket walls, which the case endpoints do not: enrich
         the cutset with the granularity walls of every derived
         attribute the clip touches — the clipped source AND a
         condition-comparison attribute alike (the verify round caught
         the condition side reintroducing the silent zero) *)
      let derived_attrs =
        (if prorata_writes ctx.doc ~otype:aot ~attr:a then [ a ] else [])
        @
        match c with
        | Vergelijking
            { links = Some (Attribuut_ref { attribuut = Some b; objecttype = Some botn; _ }); _ }
          when norm botn = norm aot && prorata_writes ctx.doc ~otype:botn ~attr:b -> [ b ]
        | _ -> []
      in
      let endpoints =
        if derived_attrs = [] then endpoints
        else
          match endpoints with
          | [] -> endpoints
          | lo :: _ ->
              let hi = List.fold_left max lo endpoints in
              let walls_of attr =
                let g = attr_granularity ctx ~otype:aot ~attr in
                let rec walls d acc =
                  if d >= hi then acc
                  else
                    let _, bn = Uchar_ref.Timeline.bucket g d in
                    walls bn (bn :: acc)
                in
                let first, _ = Uchar_ref.Timeline.bucket g lo in
                first :: walls first []
              in
              List.sort_uniq compare (endpoints @ List.concat_map walls_of derived_attrs)
      in
      (* the clip total's #sum accumulates W×N terms; its segments are
         disjoint slices of one timeline, so ΣN over any subject is at
         most the whole units of the FULL endpoint span — bounding that
         span at 46340 keeps |ΣW×N| ≤ 46340² < 2^31 under the |W|
         segment guard below (the wrap fence, compile-time) *)
      (match endpoints with
      | [] -> ()
      | lo :: _ ->
          let hi = List.fold_left max lo endpoints in
          let n = Uchar_ref.Timeline.whole_units gran lo hi in
          if n > 46340 then
            fault "the clipped endpoint span covers %d whole units, beyond the guarded sum range"
              n);
      List.iter
        (fun v ->
          List.iter
            (fun t ->
              if v < t then begin
                let n = Uchar_ref.Timeline.whole_units gran v t in
                if n > 46340 then
                  fault "a clipped span of %d whole units exceeds the guarded product range" n;
                emit ctx "%s_wu(%d,%d,%d)." p v t n
              end)
            endpoints)
        endpoints;
      let q = fresh ctx "Q" in
      (* W×N inside the #sum is an emitted multiplication: guard the
         segment value like every other product operand (the wu factor
         is a compile-time constant bounded at emission above) *)
      (* keyed on the CALLER's subject with the is_a base and [rule_guard], for
         the same reason as the clip's comparison fence above: the clip is an
         expression, so it can sit in an operand of a member of some other
         composite, and then a member the outcome has already settled away must
         not fire this fence. Keying it on the helper's own free "S" also left
         it ungrounded by anything but the seg atom. Witness:
         corpus/golden/jutterswet-klipwacht. *)
      emit ctx
        "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",%s) :- %s, \
         %s_seg(%s,V,T,W), |W| > 46340."
        subject
        (String.concat ", "
           (Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) :: ctx.rule_guard))
        p subject;
      emit ctx "%s_tot(S,%s) :- is_a(S,%s), %s = #sum { W*N,V,T : %s_seg(S,V,T,W), %s_wu(V,T,N) }."
        p q (str subject_ot) q p p;
      ( [ Printf.sprintf "%s_tot(%s,%s)" p subject q ],
        q,
        pow10 (attr_decimals ctx ~otype:aot ~attr:a) )
  | Functie { kind = Totaal; argumenten = [ Attribuut_ref { attribuut = Some a; objecttype = Some aot; navigatie_rol = None; navigatie_pad = []; set = false; _ } ]; _ }
    when norm aot = norm subject_ot && attr_temporal ctx ~otype:aot ~attr:a ->
      (* ch. 3.6 over the asOfDate bucket: value × whole-units (= 1 for
         the year bucket); an absent timeline stays underivable = leeg.
         The pattern is the gedurende sister arm's above minus its filter,
         and ref's own temporal-total arm narrowing for narrowing, because
         the body emits val(<SUBJECT>,…): a form that reaches here without
         BEING the subject's own attribute reads the wrong instance.
         [set = false] keeps the §7.1 per-element fold out ([aot] is the
         ELEMENT type there, read off each member — a different instance
         even on a self-relation); navigatie_rol/navigatie_pad and
         [norm aot = norm subject_ot] keep a navigated or foreign-typed
         operand out (measured before the narrowing: 99 instead of 10 where
         the two types share the attribute name, silently leeg where they
         do not). Every excluded form is refused by reason in the arm
         below. [navigatie_bindingen] is deliberately NOT narrowed: with
         navigatie_rol = None ref reads the subject too, so the executors
         agree and a refusal here would be the divergence. *)
      refuse_attr_timeline ctx ~otype:aot ~attr:a;
      grid_strip_ok ctx
        ~what:(Printf.sprintf "the temporal total of %S" a)
        ~gran:(attr_granularity ctx ~otype:aot ~attr:a)
        (attr_unit ctx ~otype:aot ~attr:a);
      let f = total_factor (attr_granularity ctx ~otype:aot ~attr:a) in
      let v = fresh ctx "T" in
      let term = if f = 1 then v else Printf.sprintf "(%s*%d)" v f in
      ( [ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ],
        term,
        pow10 (attr_decimals ctx ~otype:aot ~attr:a) )
  | Functie
      { kind = Totaal;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some a; objecttype = Some aot; set; navigatie_rol; navigatie_pad; _ } ];
        element_objecttype; _ }
    when (match fold_operand_otype ~element_objecttype ~set (Some aot) with
         | Some ot -> attr_temporal ctx ~otype:ot ~attr:a
         | None -> false) ->
      (* every temporal totaal the arms above deliberately do not carry.
         Each of these used to fall INTO the own-subject arm and be
         answered off val(<SUBJECT>,…) — the wrong instance — so each is
         refused here by its own reason instead: never a silent value,
         never a silent leeg. A NON-temporal totaal is not this arm's
         business and reaches the catch-all below, as it always has.

         The temporality guard hangs on the ANCHOR ([fold_operand_otype]),
         not on the operand's own objecttype: a set fold's declaration
         lives on the element type. Before that decision a MIXED
         annotation — a non-temporal aot beside a temporal element type —
         missed this arm and fell reasonlessly into the catch-all; it now
         reaches the carried set-fold arm above, which folds it as the
         temporal fold its element type declares. *)
      if set then
        (* the navigation-free set fold is CARRIED above (§7.1, since
           2026-09-05); what is left here NAVIGATES, and the carried arm
           anchors every read at the element variable of a navigation-free
           source, mirroring ref — whose instance-fold source arm refuses
           a navigated source outright. *)
        fault
          "the NAVIGATED per-element temporal total over a set (§7.1) is not translated: the \
           carried set fold reads %S off each member of a navigation-free source, and ref \
           refuses a navigated fold source outright (an instance-fold source shape it does not \
           carry), so this arm must not answer either"
          a;
      if navigatie_rol <> None || navigatie_pad <> [] then
        fault
          "the temporal total of the NAVIGATED %S is not translated: the carried arm totals the \
           SUBJECT's own timeline, so it would read %S off the subject instead of off the \
           navigated object — a wrong value where the two types share the attribute name, a \
           silent leeg where they do not; ref refuses the shape outright (an instance-fold source \
           it does not carry), so this arm must not answer either"
          a a;
      fault
        "the temporal total of %S is declared on %S, not on the subject type %S: the carried arm \
         totals the SUBJECT's own timeline and would answer for the wrong object; ref refuses the \
         shape outright, so this arm must not answer either"
        a aot subject_ot
  | Temporal_read { basis; datum; typ = _; bron = _ } -> (
      (* ch. 3.3 read(tl, d): join the segment relation on containment.
         A gap or an empty date leaves the body underivable — leeg as
         absence, exactly ref/'s leeg-on-gap and empty-index rules. *)
      let db, dt, ds = expr_term ctx ~subject ~subject_ot datum in
      if ds <> 1 then fault "temporal-read date operand must be an unscaled day term";
      let v = fresh ctx "R" in
      let f = fresh ctx "F" and t = fresh ctx "T" in
      match basis with
      | Attribuut_ref
          { attribuut = Some a; objecttype = Some otn; navigatie_rol = None;
            navigatie_pad = []; set = false; _ }
        when Ir_v0.Checks.norm otn = Ir_v0.Checks.norm subject_ot ->
          if not (attr_temporal ctx ~otype:otn ~attr:a) then
            fault "temporal read of %S, which is not a temporal attribute" a;
          require_attr_timeline ctx ~otype:otn ~attr:a;
          ( db
            @ [ Printf.sprintf "tl(%s,%s,%s,%s,%s), %s <= %s, %s < %s" subject (str a) f t v f
                  dt dt t ],
            v,
            pow10 (attr_decimals ctx ~otype:otn ~attr:a) )
      | Parameter_ref { parameter = Some p; _ } -> (
          match Ir_v0.Checks.find_ci ctx.doc.parameters p with
          | Some { par_granulariteit = None; _ } ->
              fault "temporal read of parameter %S, which is not granular" p
          | Some pr ->
              if Eval.SMap.find_opt (Ir_v0.Checks.norm p) ctx.case.param_tls = None then
                fault "the temporal read of parameter %S needs its timeline in the case" p;
              ( db
                @ [ Printf.sprintf "param_tl(%s,%s,%s,%s), %s <= %s, %s < %s" (str p) f t v f
                      dt dt t ],
                v,
                pow10 (decimals_of_datatype pr.par_datatype) )
          | None -> fault "unknown parameter %S" p)
      | _ -> fault "unsupported temporal-read basis shape")
  | Functie { kind = (Tijdsduur | Absolute_tijdsduur) as kind; argumenten; filter; typ; _ }
    ->
      (* tijdsduur van A tot B (ch. 3.8): whole declared units between two
         day-precision dates, signed — the packed-month rule as a pure-ASP
         decomposition chain; the absolute variant wraps gringo's |t|. The
         counted unit is the NODE type's unit, resolved by spelling
         (WF-038). Any empty operand leaves its bare join unbound — leeg
         propagation for free, never 0. *)
      if filter <> None then fault "a filter on a tijdsduur has no v0 form";
      let a, b =
        match argumenten with
        | [ a; b ] -> (a, b)
        | _ -> fault "tijdsduur expects two arguments"
      in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      let wrap t = if kind = Absolute_tijdsduur then Printf.sprintf "|%s|" t else t in
      if is_datetime_operand (Some a) || is_datetime_operand (Some b) then begin
        (* datum-tijd tijdsduur (ch. 3.8, the millisecond layer): both
           operands are pair terms (WF-038 forbids mixed precision). A sub-day
           unit counts linearly on the bounded ms difference; a day-based unit
           carries the BORROW rule — the borrow-adjusted B-day DB' = DB +
           (-1 when DB>DA & MSB<MSA, +1 when DB<DA & MSB>MSA, else 0) fed to
           the day-precision whole-unit count, WITHOUT forming the whole ms
           difference (so a month/year span stays unbounded), exactly ref/'s
           whole_units_between_ms. *)
        if not (is_datetime_operand (Some a) && is_datetime_operand (Some b)) then
          fault "tijdsduur mixes date precisions (WF-038)";
        let tu = dt_time_unit_of ~what:"tijdsduur" typ in
        let da_atoms, da, msa = dt_term ctx ~subject ~subject_ot a in
        let db_atoms, db, msb = dt_term ctx ~subject ~subject_ot b in
        let gbody = (base :: ctx.rule_guard) @ da_atoms @ db_atoms in
        match sub_day_ms tu with
        | Some u ->
            (* T = (DB-DA)*86 400 000 + (MSB-MSA); fenced so the product stays
               inside 2^31 — a sub-day span beyond ~23 civil days faults loudly
               (ref, on 63-bit ints, reaches further; a range fence, S6a) *)
            range_guard ctx ~subject ~body:gbody
              [ (Printf.sprintf "((%s)-(%s))" db da, (max_int31 - ms_per_day) / ms_per_day) ];
            let tt = fresh ctx "T" in
            ( da_atoms @ db_atoms
              @ [ Printf.sprintf "%s = (((%s)-(%s))*%d)+((%s)-(%s))" tt db da ms_per_day msb
                    msa ],
              wrap (Printf.sprintf "((%s)/%d)" tt u),
              1 )
        | None ->
            ctx.fresh <- ctx.fresh + 1;
            let bp = Printf.sprintf "dbb%d" ctx.fresh in
            let gb = String.concat ", " gbody in
            emit ctx "%s(%s,(%s)-1) :- %s, %s > %s, %s < %s." bp subject db gb db da msb msa;
            emit ctx "%s(%s,(%s)+1) :- %s, %s < %s, %s > %s." bp subject db gb db da msb msa;
            emit ctx "%s_lo(%s) :- %s, %s > %s, %s < %s." bp subject gb db da msb msa;
            emit ctx "%s_hi(%s) :- %s, %s < %s, %s > %s." bp subject gb db da msb msa;
            emit ctx "%s(%s,%s) :- %s, not %s_lo(%s), not %s_hi(%s)." bp subject db gb bp subject
              bp subject;
            let dbp = fresh ctx "DBP" in
            let dbp_atom = Printf.sprintf "%s(%s,%s)" bp subject dbp in
            (match tu with
            | Tu_dag -> (da_atoms @ db_atoms @ [ dbp_atom ], wrap (Printf.sprintf "((%s)-(%s))" dbp da), 1)
            | Tu_week ->
                (da_atoms @ db_atoms @ [ dbp_atom ], wrap (Printf.sprintf "(((%s)-(%s))/7)" dbp da), 1)
            | Tu_maand | Tu_kwartaal | Tu_jaar ->
                pre_epoch_fence ctx ~subject ~body:gbody da;
                pre_epoch_fence ctx ~subject ~body:(gbody @ [ dbp_atom ]) dbp;
                let ca, pma, dda = civil_pm_day ctx da in
                let cb, pmb, ddb = civil_pm_day ctx dbp in
                let months = Printf.sprintf "((((%s*32)+%s)-((%s*32)+%s))/32)" pmb ddb pma dda in
                let n =
                  match tu with
                  | Tu_maand -> months
                  | Tu_kwartaal -> Printf.sprintf "(%s/3)" months
                  | Tu_jaar -> Printf.sprintf "(%s/12)" months
                  | _ -> assert false
                in
                (da_atoms @ db_atoms @ [ dbp_atom ] @ ca @ cb, wrap n, 1)
            | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde -> assert false)
      end
      else begin
        (* the day-precision family: the packed-month rule as a pure-ASP
           decomposition chain. A sub-day unit here has no time-of-day to
           count on — day_time_unit_of faults it (WF-038). *)
        let operand_day e =
          match Ir_v0.Checks.typ_of_expr e with
          | Some Datum_in_dagen -> ()
          | _ -> fault "tijdsduur operand is not statically a day-precision date"
        in
        operand_day a;
        operand_day b;
        let tu = day_time_unit_of ~what:"tijdsduur" typ in
        let ba, ta, sa = expr_term ctx ~subject ~subject_ot a in
        let bb, tb, sb = expr_term ctx ~subject ~subject_ot b in
        if sa <> 1 || sb <> 1 then fault "tijdsduur operands must be unscaled day terms";
        match tu with
        | Tu_dag -> (ba @ bb, wrap (Printf.sprintf "((%s)-(%s))" tb ta), 1)
        | Tu_week -> (ba @ bb, wrap (Printf.sprintf "(((%s)-(%s))/7)" tb ta), 1)
        | Tu_maand | Tu_kwartaal | Tu_jaar ->
            pre_epoch_fence ctx ~subject ~body:((base :: ctx.rule_guard) @ ba) ta;
            pre_epoch_fence ctx ~subject ~body:((base :: ctx.rule_guard) @ bb) tb;
            let ca, pma, da = civil_pm_day ctx ta in
            let cb, pmb, db = civil_pm_day ctx tb in
            let months = Printf.sprintf "((((%s*32)+%s)-((%s*32)+%s))/32)" pmb db pma da in
            let n =
              match tu with
              | Tu_maand -> months
              | Tu_kwartaal -> Printf.sprintf "(%s/3)" months
              | Tu_jaar -> Printf.sprintf "(%s/12)" months
              | _ -> assert false
            in
            (ba @ bb @ ca @ cb, wrap n, 1)
        | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde ->
            (* unreachable: day_time_unit_of faulted the sub-day units *)
            assert false
      end
  | Functie { kind = (Jaar_uit | Maand_uit | Dag_uit) as kind; argumenten; filter; _ } ->
      (* the day-precision part extractors (ch. 3.8): the civil components
         of the operand's day number as unitless whole terms — the same
         era decomposition the packed-month rule rides (PM = 12·year +
         month − 1, D the day of month). Leeg propagates by the bare join:
         an unbound date leaves the chain unbound. *)
      if filter <> None then fault "a filter on a part extractor has no v0 form";
      let a =
        match argumenten with
        | [ a ] -> a
        | _ -> fault "a part extractor expects one argument"
      in
      (match Ir_v0.Checks.typ_of_expr a with
      | Some Datum_in_dagen -> ()
      | Some Datum_tijd ->
          fault "datum-tijd part extraction is not translated (the sub-day extractors are not \
                 the ms layer's arithmetic; no corpus case)"
      | _ -> fault "part-extractor operand is not statically a day-precision date");
      let ba, ta, sa = expr_term ctx ~subject ~subject_ot a in
      if sa <> 1 then fault "part-extractor operand must be an unscaled day term";
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      pre_epoch_fence ctx ~subject ~body:((base :: ctx.rule_guard) @ ba) ta;
      let catoms, pmv, dv = civil_pm_day ctx ta in
      let t =
        match kind with
        | Jaar_uit -> Printf.sprintf "(%s/12)" pmv
        | Maand_uit -> Printf.sprintf "((%s \\ 12)+1)" pmv
        | Dag_uit -> dv
        | _ -> assert false
      in
      (ba @ catoms, t, 1)
  | Functie { kind = Eerste_paasdag; argumenten; filter; _ } ->
      (* eerste paasdag van a whole unitless year (ch. 3.8): the
         Gauss/Anonymous chain, then days_from_civil with the March shift —
         the month is always 3 or 4, so the shift term is 0, written anyway
         to mirror the month step's re-encode shape. For years >= 1 every
         intermediate that needs floor is non-negative (fenced below), so
         gringo's toward-zero division is exact; leeg propagates by the
         bare join. *)
      if filter <> None then fault "a filter on eerste-paasdag has no v0 form";
      let y =
        match argumenten with
        | [ y ] -> y
        | _ -> fault "eerste-paasdag expects one argument"
      in
      (match Ir_v0.Checks.typ_of_expr y with
      | Some (Numeriek { geheel = true; eenheid = None; _ }) -> ()
      | _ -> fault "eerste-paasdag operand is not statically a whole unitless year (WF-038)");
      let by, ty, sy = expr_term ctx ~subject ~subject_ot y in
      if sy <> 1 then fault "eerste-paasdag year operand must be an unscaled whole term";
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      emit ctx
        "viol(\"eerste-paasdag of a year before 1 is not translated (the Gauss chain \
         needs non-negative intermediates)\",%s) :- %s, (%s) < 1."
        subject
        (String.concat ", " ((base :: ctx.rule_guard) @ by))
        ty;
      let av = fresh ctx "A" and bv = fresh ctx "B" and cv = fresh ctx "C" in
      let dv = fresh ctx "D" and ev = fresh ctx "E" in
      let fv = fresh ctx "F" and gv = fresh ctx "G" and hv = fresh ctx "H" in
      let iv = fresh ctx "I" and kv = fresh ctx "K" and lv = fresh ctx "L" in
      let mv = fresh ctx "M" and mov = fresh ctx "MO" and dav = fresh ctx "DA" in
      let y2 = fresh ctx "Y2" and e2 = fresh ctx "E2" and yoe2 = fresh ctx "YOE" in
      let mp2 = fresh ctx "MP" and doy2 = fresh ctx "DOY" and doe2 = fresh ctx "DOE" in
      let zt = fresh ctx "ZT" in
      let chain =
        [ Printf.sprintf "%s = (%s) \\ 19" av ty;
          Printf.sprintf "%s = (%s)/100" bv ty;
          Printf.sprintf "%s = (%s) \\ 100" cv ty;
          Printf.sprintf "%s = %s/4" dv bv;
          Printf.sprintf "%s = %s \\ 4" ev bv;
          Printf.sprintf "%s = (%s+8)/25" fv bv;
          Printf.sprintf "%s = ((%s-%s)+1)/3" gv bv fv;
          Printf.sprintf "%s = ((((19*%s)+%s)-%s-%s)+15) \\ 30" hv av bv dv gv;
          Printf.sprintf "%s = %s/4" iv cv;
          Printf.sprintf "%s = %s \\ 4" kv cv;
          Printf.sprintf "%s = ((((32+(2*%s))+(2*%s))-%s)-%s) \\ 7" lv ev iv hv kv;
          Printf.sprintf "%s = ((%s+(11*%s))+(22*%s))/451" mv av hv lv;
          Printf.sprintf "%s = (((%s+%s)-(7*%s))+114)/31" mov hv lv mv;
          Printf.sprintf "%s = ((((%s+%s)-(7*%s))+114) \\ 31)+1" dav hv lv mv;
          Printf.sprintf "%s = (%s)-((14-%s)/12)" y2 ty mov;
          Printf.sprintf "%s = %s/400" e2 y2;
          Printf.sprintf "%s = %s-(%s*400)" yoe2 y2 e2;
          Printf.sprintf "%s = (%s+9) \\ 12" mp2 mov;
          Printf.sprintf "%s = ((((153*%s)+2)/5)+%s)-1" doy2 mp2 dav;
          Printf.sprintf "%s = (((%s*365)+(%s/4))-(%s/100))+%s" doe2 yoe2 yoe2 yoe2 doy2;
          Printf.sprintf "%s = ((%s*146097)+%s)-719468" zt e2 doe2 ]
      in
      (by @ chain, zt, 1)
  | Binair { op = (Plus | Min) as op; links; rechts; _ }
    when List.exists
           (fun e ->
             match Option.bind e Ir_v0.Checks.typ_of_expr with
             | Some (Datum_in_dagen | Datum_tijd) -> true
             | _ -> false)
           [ links; rechts ] ->
      (* date ± duration (ch. 3.8), routed on the static date type — the
         ref mirror: the date stands on the LEFT of plus and min (v0's
         typing opens its date branch only for a date-left operand — no
         commuting), date ± date is ill-formed (WF-035, fail-fast behind
         the verifier). The date operand PROPAGATES empty by its bare join;
         the duration COALESCES (leeg counts 0, the date unchanged); a
         fractional count truncates toward zero through the scale
         division (gringo truncates toward zero on both signs). *)
      let le = req "Binair.links" links and re = req "Binair.rechts" rechts in
      let date_typed e =
        match Ir_v0.Checks.typ_of_expr e with
        | Some Datum_in_dagen -> true
        | Some Datum_tijd ->
            fault "datum-tijd date arithmetic reaches a scalar day slot (the pair term rides \
                   dt_term, not expr_term's integer encoding)"
        | _ -> false
      in
      let minus = op = Min in
      let date_e, dur_e =
        if date_typed le && date_typed re then
          fault "date \xc2\xb1 date is ill-formed (WF-035)"
        else if date_typed le then (le, re)
        else
          fault "duration %s date is ill-formed (WF-035: the date stands on the left; \
                 v0 does not commute)"
            (if minus then "\xe2\x88\x92" else "+")
      in
      let tu =
        day_time_unit_of ~what:"date arithmetic" (Ir_v0.Checks.typ_of_expr dur_e)
      in
      let bd, td_, sd = expr_term ctx ~subject ~subject_ot date_e in
      if sd <> 1 then fault "date operand must be an unscaled day term";
      let bn, tn, sn =
        coalesce0 ctx ~subject ~subject_ot (expr_term ctx ~subject ~subject_ot dur_e)
      in
      (* [day_time_unit_of] above read the wire `type`; the scale is the
         DECLARATION's, and this arm never divides in OCaml — it WRITES the
         divisor into the ASP text. An unfenced 0 therefore grounds as `(T)/0`,
         which gringo reports as "operation undefined" and then answers the
         empty model where ref faults: a silent divergence, not a crash. *)
      let sn = numeric_scale ~what:"a date-arithmetic duration" sn in
      let n_term = if sn = 1 then tn else Printf.sprintf "((%s)/%d)" tn sn in
      let n_term = if minus then Printf.sprintf "(-(%s))" n_term else n_term in
      (match tu with
      | Tu_dag -> (bd @ bn, Printf.sprintf "((%s)+(%s))" td_ n_term, 1)
      | Tu_week -> (bd @ bn, Printf.sprintf "((%s)+(7*(%s)))" td_ n_term, 1)
      | Tu_maand ->
          translate_date_plus_months ctx ~subject ~subject_ot ~date_atoms:bd
            ~date_term:td_ ~dur_atoms:bn ~count:n_term
      | Tu_kwartaal ->
          translate_date_plus_months ctx ~subject ~subject_ot ~date_atoms:bd
            ~date_term:td_ ~dur_atoms:bn ~count:(Printf.sprintf "(3*(%s))" n_term)
      | Tu_jaar ->
          translate_date_plus_months ctx ~subject ~subject_ot ~date_atoms:bd
            ~date_term:td_ ~dur_atoms:bn ~count:(Printf.sprintf "(12*(%s))" n_term)
      | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde ->
          (* unreachable: day_time_unit_of faulted the sub-day units *)
          assert false)
  | Binair { op = (Plus | Min | Verminderd_met) as op; links; rechts; _ } ->
      (* the additive family on harmonised scales. Numeric operands
         only: ref faults date arithmetic here, so a non-numeric static
         type must not silently sum as day numbers. Emptiness follows
         the ch. 1.2a whitelist: plus/min coalesce BOTH operands to 0,
         verminderd-met coalesces only the right — its left PROPAGATES,
         which the bare unbound join encodes for free (a leeg left
         leaves the rule underivable, exactly ref's Empty result). *)
      numeric_ok links;
      numeric_ok rechts;
      units_identical_ok ~what:"an additive join"
        (Option.bind links (eff_unit ctx))
        (Option.bind rechts (eff_unit ctx));
      let l = expr_term ctx ~subject ~subject_ot (req "Binair.links" links) in
      let r = expr_term ctx ~subject ~subject_ot (req "Binair.rechts" rechts) in
      let l = if op = Verminderd_met then l else coalesce0 ctx ~subject ~subject_ot l in
      let r = coalesce0 ctx ~subject ~subject_ot r in
      let _, tl_raw, s1 = l and _, tr_raw, s2 = r in
      (* [numeric_ok] above read the wire `type`; the scale is the declaration's *)
      let s1 = numeric_scale ~what:"an additive join" s1 in
      let s2 = numeric_scale ~what:"an additive join" s2 in
      let m = max s1 s2 in
      let bindings, tl, tr = harmonised l r in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      range_guard ctx ~subject
        ~body:((base :: ctx.rule_guard) @ bindings)
        [ (tl_raw, 1073741823 / (m / s1)); (tr_raw, 1073741823 / (m / s2)) ];
      let sym = if op = Plus then "+" else "-" in
      (bindings, Printf.sprintf "(%s%s%s)" tl sym tr, max s1 s2)
  | Binair { op = (Maal | Percentage_van) as op; links; rechts; _ } ->
      (* the multiplicative family (ch. 1.2a + §6.4/§6.8): BOTH operands
         coalesce leeg → 0. The product of two scaled integers is the
         scaled integer of the product at scale sl·sr — v0's static
         decimal count dl+dr. Percentage-van is (l/100)·r with the /100
         folded into the scale (sl·sr·100): no division anywhere, so no
         truncation can diverge from ref's exact rationals. *)
      numeric_ok links;
      numeric_ok rechts;
      (* Maal admits the factor-free unitless × unit-carrying product (the
         product adopts the unit, value unchanged); a two-unit product still
         faults. Percentage-van admits a pure-% left operand (its ÷100 is
         already the scale fold — PRIORITIZED-DEFAULTS-EVOLUTIE §8.3), else
         unit-free; its right operand keeps every unit refused (the ch. 2.6
         right-unit adoption has no eff_unit witness and no corpus case
         yet). *)
      (match op with
      | Maal -> mul_units_ok ctx ~what:"a product" links rechts
      | _ ->
          pct_van_left_ok ctx links;
          unit_free_ok ctx rechts);
      let bl, tl, sl =
        coalesce0 ctx ~subject ~subject_ot
          (expr_term ctx ~subject ~subject_ot (req "Binair.links" links))
      in
      let br, tr, sr =
        coalesce0 ctx ~subject ~subject_ot
          (expr_term ctx ~subject ~subject_ot (req "Binair.rechts" rechts))
      in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      range_guard ctx ~subject
        ~body:((base :: ctx.rule_guard) @ bl @ br)
        [ (tl, 46340); (tr, 46340) ];
      ( bl @ br,
        Printf.sprintf "(%s*%s)" tl tr,
        sl * sr * (if op = Percentage_van then 100 else 1) )
  | Binair { op = Gedeeld_door; links; rechts; _ } ->
      translate_division ctx ~subject ~subject_ot ~links ~rechts ~rounding:None
  | Binair { op = Gedeeld_door_abs; links; rechts; _ } ->
      (* the abs-division variant (ch. 2.2): the quotient truncated
         toward zero at 5 decimals — a defined collapse point, so the
         rounded-division path with (Richting_nul, 5) IS the semantics
         (arith.div_abs = div_gen collapsed through round_q Richting_nul
         5), edges included: empty dividend → 0, empty divisor under a
         filled dividend → the §9.5 channel fact, numeric zero → a
         viol/2 witness. gringo's integer division truncates toward
         zero on both signs, so the negative range needs no fence. *)
      translate_division ctx ~subject ~subject_ot ~links ~rechts
        ~rounding:(Some (Richting_nul, 5))
  | Binair { op = Tot_de_macht; links; rechts; _ } ->
      translate_power ctx ~subject ~subject_ot ~links ~rechts
  | Afgerond { basis = Some (Functie { kind = Wortel; _ }); _ } ->
      (* ref rounds the true irrational exactly (§13.4.17.13); that
         computation has no integer encoding here yet *)
      fault "rounded root is not translated (no corpus case)"
  | Afgerond { basis = Some (Binair { op = Gedeeld_door; links; rechts; _ }); modus; decimalen; _ }
    ->
      translate_division ctx ~subject ~subject_ot ~links ~rechts ~rounding:(Some (modus, decimalen))
  | Afgerond { basis = Some basis; modus = Rekenkundig; decimalen = 2; _ }
    when money_mult_shape basis ->
      (* the money × rate product Catala rounds to the cent (S-section (c)) —
         encoded SPLIT because the naive scale-6 product wraps int31 at
         tutorial magnitude *)
      translate_money_mult ctx ~subject ~subject_ot ~basis
  | Afgerond { basis; modus; decimalen; _ } -> (
      (* rounding a scaled term (ch. 2.5). ref propagates an empty basis
         (round_v: Empty → Empty), which the bare body join encodes for
         free. A basis whose scale already fits 10^d is returned
         unchanged (the rounding is the identity on values carrying at
         most d decimals); a coarser target divides the scale away with
         the mode's integer rounding formula — exact for non-negative
         terms, with a viol/2 witness fencing the untranslated negative
         range (richting-nul alone matches gringo's native truncation on
         both signs). *)
      let b, t, s = expr_term ctx ~subject ~subject_ot (req "Afgerond.basis" basis) in
      (* [numeric_ok basis] read the wire `type`; the alignment tests below take
         [s] as a modulus, so an unscaled basis divides by zero here too *)
      let s = numeric_scale ~what:"a rounding basis" s in
      let target = pow10 decimalen in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      let gbody = (base :: ctx.rule_guard) @ b in
      if target >= s then
        if target mod s = 0 then begin
          if target > s then range_guard ctx ~subject ~body:gbody [ (t, max_int31 / (target / s)) ];
          (b, (if target = s then t else Printf.sprintf "(%s*%d)" t (target / s)), target)
        end
        else fault "rounding target scale %d does not align with basis scale %d" target s
      else if s mod target <> 0 then
        fault "rounding basis scale %d does not align with target scale %d" s target
      else
        let k = s / target in
        range_guard ctx ~subject ~body:gbody [ (t, 536870912) ];
        let rounded =
          match modus with
          | Richting_nul -> Printf.sprintf "((%s)/%d)" t k
          | Naar_beneden -> Printf.sprintf "((%s)/%d)" t k
          | Naar_boven | Weg_van_nul -> Printf.sprintf "(((%s)+%d)/%d)" t (k - 1) k
          | Rekenkundig -> Printf.sprintf "((2*(%s)+%d)/%d)" t k (2 * k)
        in
        (match modus with
        | Richting_nul -> ()
        | _ ->
            emit ctx "viol(\"negative operand under rounding is not translated\",%s) :- %s, (%s) < 0."
              subject
              (String.concat ", " gbody)
              t);
        (b, rounded, target))
  | Begrensd { basis; minimum; maximum; _ } ->
      (* the clamp (RUNTIME-SEMANTICS ch. 2.7, ref Arith.clamp): the minimum
         raises to at least the lower bound, THEN the maximum lowers to at most
         the upper bound, via the integer identities max(a,b) = (a+b+|a−b|)/2
         and min(a,b) = (a+b−|a−b|)/2 on the harmonised scale — a+b±|a−b| is
         always even, so the /2 is exact on either sign. An empty base
         propagates: its atoms leave the reading rule underivable, so ref's
         Empty → Empty falls out of the bare join. ref restates each bound into
         the base's UNIT by an exact factor; the integer encoding carries no
         factor, so only identical (or a unitless-side) units pass — a real
         factor faults, the additive-join fence. A bound must be non-empty by
         construction: an empty bound is ref's NO-bound, which the bare join
         would instead read as underivable (a silent divergence), so only an
         atom-free ground literal/constant bound translates — a read that could
         be empty faults loudly. *)
      let basis_e = req "Begrensd.basis" basis in
      numeric_ok basis;
      let b_atoms, b_term, b_scale = expr_term ctx ~subject ~subject_ot basis_e in
      (* the numeric_ok pair reads the wire `type`; the clamp harmonises base
         and bounds into the widest scale, dividing by each of the three *)
      let b_scale = numeric_scale ~what:"a Begrensd base" b_scale in
      let base_unit = eff_unit ctx basis_e in
      let bound name = function
        | None -> None
        | Some e ->
            numeric_ok (Some e);
            units_identical_ok ~what:"a Begrensd bound" base_unit (eff_unit ctx e);
            let atoms, term, scale = expr_term ctx ~subject ~subject_ot e in
            if atoms <> [] then
              fault
                "a Begrensd %s bound that is not a ground literal could be empty; ref reads an empty \
                 bound as no bound, which the bare join cannot express (no corpus case)"
                name;
            Some (term, numeric_scale ~what:"a Begrensd bound" scale)
      in
      (* ch. 5.11, wire order: the minimum is visited before the maximum. The
         two [let]s carry that; written as one [let … and …] the order would be
         OCaml's unspecified choice, and [bound] is effectful — it faults, it
         mutates [ctx.fresh] and it can emit — so on a node whose two bounds are
         both refusable the reported reason would be whichever the compiler
         happened to run. The visiting order is now the spec's, and the rood
         witness pins it. *)
      let lo = bound "minimum" minimum in
      let hi = bound "maximum" maximum in
      let m = List.fold_left max b_scale (List.filter_map (Option.map snd) [ lo; hi ]) in
      let scale_to t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      let fenced = (b_term, b_scale) :: List.filter_map Fun.id [ lo; hi ] in
      range_guard ctx ~subject
        ~body:((base :: ctx.rule_guard) @ b_atoms)
        (List.map (fun (t, s) -> (t, 1073741823 / (m / s))) fenced);
      let bh = scale_to b_term b_scale in
      let mx a b = Printf.sprintf "((%s+%s+|%s-%s|)/2)" a b a b in
      let mn a b = Printf.sprintf "((%s+%s-|%s-%s|)/2)" a b a b in
      let after_lo = match lo with Some (t, s) -> mx bh (scale_to t s) | None -> bh in
      let clamped = match hi with Some (t, s) -> mn after_lo (scale_to t s) | None -> after_lo in
      (b_atoms, clamped, m)
  | Constante { naam = Some n; _ } when Ir_v0.Checks.norm n = Ir_v0.Checks.norm "Rekendatum" -> (
      (* ch. 8.2: the execution-date constant is a program constant — the
         session's one evaluation date as a civil day number *)
      match ctx.case.as_of with
      | Some d -> (
          match Value.date_of_iso d with
          | Value.Date z -> ([], string_of_int z, 1)
          | _ -> fault "malformed asOfDate %S" d)
      | None ->
          fault "the Rekendatum constant needs an evaluation date; the case carries asOfDate null")
  | Construct { typ; constructor_; argument = (None | Some (Literal _)) as argument; _ } ->
      (* ch. 9.1: a constructor value is a ground term when its payload is
         ground — a nullary tag, or a literal payload scaled to the DECLARED
         payload datatype. *)
      ([], construct_ground_term ctx typ constructor_ argument, 0)
  | Construct { typ; constructor_; argument = Some (Attribuut_ref _ as arg); _ } -> (
      (* ch. 9.1: a bound payload (an own attribute read) rides the argument's
         own val atom, scaled to the DECLARED payload datatype; a leeg argument
         binds no atom, so the rule does not fire and the whole Construct is
         leeg — propagation for free. The scale of the constructor VALUE itself
         is 0 (a sum term is not numeric); it is carried by [attr_sum_typed]'s
         write path, never fitted. *)
      match ctor_payload_datatype (sum_constructors typ) constructor_ with
      | None -> fault "nullary constructor %S built with an argument (WF-062)" constructor_
      | Some d ->
          let f = ctor_functor constructor_ in
          let payload_scale = pow10 (decimals_of_datatype (Some d)) in
          let atoms, term, scale = expr_term ctx ~subject ~subject_ot arg in
          if scale <> payload_scale then
            fault
              "a Construct payload at scale %d does not match the declared payload scale %d \
               (rescaling a bound sum payload has no corpus case)"
              scale payload_scale;
          (atoms, Printf.sprintf "%s(%s)" f term, 0))
  | Construct _ ->
      fault
        "a Construct payload other than a literal or an own attribute read is not translated at \
         rule level (a parameter read, a Match or a composite payload has no corpus case)"
  | Match { typ; scrutinee; arms; _ } ->
      translate_match ctx ~subject ~subject_ot ~typ ~scrutinee ~arms
  | Binder_ref { naam; _ } -> (
      (* ch. 9.2/10.2: the ASP variable the enclosing match arm's scrutinee
         pattern or the enclosing lambda/function parameter bound, at its
         declared scale; a Let binder additionally carries its self-contained
         helper atom (B3), so the value is bound wherever the read lands *)
      match List.assoc_opt (norm naam) ctx.binders with
      | Some (atoms, var, scale) -> (atoms, var, scale)
      | None -> fault "unbound binder %S in the ASP translation (WF-064 should preclude this)" naam)
  | Let { binder; body; _ } when not (reads_binder binder body) ->
      (* ch. 10.6 clause 3: an UNREAD binder evaluates nothing. The value is
         not translated at all — no atoms, no range fence, no §9.5 deposit —
         and the body IS the Let. *)
      expr_term ctx ~subject ~subject_ot body
  | Let { binder; value; body; _ } ->
      (* ch. 10.6: the Let as a bound intermediate (S-section (a)). The value
         is translated to (atoms, term, scale); the binder resolves to that
         term, and the value's atoms are threaded into [rule_guard] so any
         helper emitted while translating the body binds the value's variables.
         A leeg value binds no atom, so — this arm being the READ case, where
         the whole body is one straight-line outcome — every rule carrying the
         body is underivable: ref's read-place barrier and clingo's
         underivability coincide by construction. Returned atoms carry both the
         value's and the body's, so the caller's rule body binds every variable
         it uses (and a coalescing position that swallows the read's own letN
         atom cannot swallow these). *)
      let va, vt, vs = expr_term ctx ~subject ~subject_ot value in
      let saved_b = ctx.binders and saved_g = ctx.rule_guard in
      ctx.binders <- (norm binder, let_binder ctx ~subject ~subject_ot (va, vt, vs)) :: ctx.binders;
      ctx.rule_guard <- ctx.rule_guard @ va;
      let ba, bt, bs = expr_term ctx ~subject ~subject_ot body in
      ctx.binders <- saved_b;
      ctx.rule_guard <- saved_g;
      (va @ ba, bt, bs)
  | Fold { typ; list; functie; init; _ } ->
      (* ch. 10.2: index recursion over the reified list (S15). The result is
         the accumulator at the list length; a present-but-empty list yields
         the init (base case fires on lpres); an absent list derives no lpres,
         so nothing is reachable — leeg propagation for free. A sum-typed
         accumulator (the Optional of the reduce lowering, ch. 10.4) carries a
         ground constructor term, scale 0. *)
      (* the I-1/I recurrence needs contiguous positions: a FilterList anywhere
         in the source chain (also under an index-preserving MapList) leaves
         index gaps the recursion would silently stall on, so it is refused
         loudly here (S19) rather than deriving nothing. ListLength over a
         filter stays fine — #count tolerates the gaps. *)
      let rec no_filter_source e =
        match e with
        | Filter_list _ ->
            fault
              "a Fold over a filtered list is not translated (index gaps need re-indexing; no \
               corpus case)"
        | Map_list { list; _ } -> no_filter_source list
        | _ -> ()
      in
      no_filter_source list;
      let out_scale = value_scale typ in
      let lterm, elem_scale = list_source ctx ~subject ~subject_ot list in
      ctx.fresh <- ctx.fresh + 1;
      let fa = Printf.sprintf "fa%d" ctx.fresh and fr = Printf.sprintf "fr%d" ctx.fresh in
      (* the init is normally a CLOSED term: the fold's own scope has no
         subject. A SUBJECT-DEPENDENT init — the reified duty folded with its
         OWN term, Openstaand(<the termijn of this duty>) — rides [expr_term],
         which carries the form at rule level, but ONLY when the fold source is
         SUBJECT-KEYED (an own attribute list, whose list id is alist(S,attr)).
         fa/fr are keyed by the LIST ID alone: over a list PARAMETER the id is
         plist(p) and the subject drops out of the head, so two subjects' init
         facts land on one key and the recursion hands every subject every
         subject's outcome — a WRONG answer, measured, not a missing one. That
         collision is refused loudly here (S19).
         [init_is_subject_read] is deliberately a SHAPE test — it names the two
         shapes [expr_term] is known to carry with a subject read, and it picks
         the ROUTE, not the semantics. An init that merely CONTAINS a subject
         read deeper down (under arithmetic, under a Let) takes the closed
         route and is refused there by [closed_term]'s attribute-read arm,
         which states this same shape rule as its reason. Widening the test
         would move those forms to a refusal with a different wording and would
         newly CARRY only forms no corpus document has — and a carried form
         without a witness is exactly the drift this fence exists to stop. *)
      let rec init_is_subject_read e =
        match e with
        | Attribuut_ref _ -> true
        | Construct { argument = Some a; _ } -> init_is_subject_read a
        | _ -> false
      in
      (* deliberately ONE shape, not a walk down derived sources: a mapped list
         over an own attribute list keeps the subject in its id (lm7(alist(S,a)))
         and would be safe, but nothing asks for it and a carried path without a
         corpus witness is a path that drifts unseen. The refusal below says so
         rather than claiming a collision that shape does not have. *)
      let source_subject_keyed e = match e with Attribuut_ref _ -> true | _ -> false in
      let init_atoms, init_term, init_scale =
        if init_is_subject_read init then begin
          if not (source_subject_keyed list) then
            fault
              "a subject-dependent Fold init is carried only over an own attribute list, whose id \
               alist(S,attr) carries the subject: over a list parameter the id is plist(p) alone, \
               so two subjects' init facts collide on that one key and every subject reads every \
               subject's outcome — a wrong answer, not a missing one; a derived source (a mapped \
               list) keeps the subject in its id but has no corpus case";
          expr_term ctx ~subject ~subject_ot init
        end
        else closed_term ctx ~guard_body:[ Printf.sprintf "lpres(%s)" lterm ] ~witness:lterm init
      in
      if init_scale <> out_scale then
        fault "fold init scale %d does not match the fold's declared scale %d" init_scale out_scale;
      let accname, xname, body =
        match functie with
        | Fn_lambda { parameters = [ (a, _); (x, _) ]; body; _ } -> (a, x, body)
        | Fn_lambda _ -> fault "a fold lambda takes exactly two parameters (WF-070)"
        | Fn_name _ ->
            (* WF-069 rejects this before translation; an unreachable backstop *)
            fault "a named FunctionDef as a fold operand is refused (WF-069: the operand must be an inline lambda)"
      in
      let base_body = Printf.sprintf "lpres(%s)" lterm in
      emit ctx "%s(%s,0,%s) :- %s." fa lterm init_term
        (String.concat ", " (base_body :: init_atoms));
      let rec_body =
        [ Printf.sprintf "%s(%s,I-1,A)" fa lterm; Printf.sprintf "lelem(%s,I,V)" lterm ]
      in
      let saved = ctx.binders in
      ctx.binders <-
        (norm accname, ([], "A", out_scale)) :: (norm xname, ([], "V", elem_scale)) :: ctx.binders;
      let branches = body_branches ctx ~guard_body:rec_body ~witness:lterm body in
      ctx.binders <- saved;
      List.iter
        (fun (g, t, s) ->
          if s <> out_scale then
            fault "a fold body branch scale %d does not match the fold's declared scale %d" s
              out_scale;
          emit ctx "%s(%s,I,%s) :- %s." fa lterm t (String.concat ", " (rec_body @ g)))
        branches;
      emit ctx "%s(%s,F) :- lpres(%s), N = #count { I : lelem(%s,I,_) }, %s(%s,N,F)." fr lterm
        lterm lterm fa lterm;
      let v = fresh ctx "F" in
      ([ Printf.sprintf "%s(%s,%s)" fr lterm v ], v, out_scale)
  | List_length { list; _ } ->
      (* ch. 10.2: #count over the reified elements, gated on presence — an
         empty list counts 0, an absent list derives no lpres and so no count
         (leeg). #count tolerates the gaps a FilterList leaves (no
         re-indexing needed; a Fold over a FilterList would and has no case). *)
      let lterm, _ = list_source ctx ~subject ~subject_ot list in
      ctx.fresh <- ctx.fresh + 1;
      let p = Printf.sprintf "ll%d" ctx.fresh in
      emit ctx "%s(%s,C) :- lpres(%s), C = #count { I : lelem(%s,I,_) }." p lterm lterm lterm;
      let v = fresh ctx "L" in
      ([ Printf.sprintf "%s(%s,%s)" p lterm v ], v, 1)
  | Field_access { base; veld; _ } ->
      (* ch. 11.1: read one declared field of a struct-typed base by positional
         projection. The base binds its record term (an own attribute read via
         its val atom); a leeg base binds nothing, so the projection propagates
         leeg for free. *)
      let batoms, bterm = struct_base ctx ~subject ~subject_ot base in
      let unif, fvar, fscale = project_field ctx ~base ~bterm ~veld in
      (batoms @ [ unif ], fvar, fscale)
  | Struct_literal { typ; velden; _ } ->
      (* ch. 11.1: a computed record value — the domain functor over the field
         terms (declaration order), no numeric scale (0). A leeg field leaves
         the literal underivable (no-hole propagation, the Construct sibling). *)
      let atoms, term = struct_literal_term ctx ~subject ~subject_ot ~typ ~velden in
      (atoms, term, 0)
  | Expr_onopgelost _ ->
      (* the unsolved-state guard: an Onopgelost node never vanishes —
         the rule that carries it fails observably (WF-044) *)
      fault "Onopgelost reached in translation: this rule fails observably (WF-044)"
  | _ -> fault "expression kind not translated (no corpus case exercises it)"

(* gedeeld-door (§6.5, arith.div_gen) — three edges plus the value:
   an empty dividend collapses to 0 before the divisor is read (also
   under a zero or empty divisor); an empty divisor under a filled
   dividend deposits the §9.5 channel fact and derives no value (leeg
   as absence); a numeric zero divisor is a hard fault — a viol/2
   witness, so the UNSAT diagnosis names the reason, exactly where
   ref faults. The value itself: the true quotient (L/sl)/(R/sr) at
   out-scale m is L·(sr·m) / (R·sl). Bare division translates only
   its EXACT outcomes — a remainder derives a viol/2 witness instead
   of a truncated value (v0's §6.1.2 static guard demands an explicit
   rounding before precision is lost; silent integer truncation would
   diverge from ref's exact rationals). Rounded division computes the
   mode's integer rounding of the true quotient, exact for
   non-negative operands; the negative range is fenced by a viol/2
   witness (richting-nul alone matches gringo's truncation on both
   signs and needs no fence). *)
(* --- the datum-tijd pair term as an EXPRESSION (RUNTIME-SEMANTICS ch. 3.8,
   the millisecond layer; the A4 pair-term encoding) ----------------------
   A datum-tijd-typed expression lowers to (atoms, D, MS): the atoms bind the
   civil-day variable D and the ms-of-day variable MS (0 <= MS < 86 400 000),
   never the absolute ms value. Two shapes carry: an own-subject datum-tijd
   attribute read (val/3 with a dt/2 payload) and datetime ± duration. Leeg
   propagates by the bare join (an unbound date leaves the pair underivable),
   exactly the day-precision date rule. *)
and dt_term ctx ~subject ~subject_ot (e : expr) : string list * string * string =
  match e with
  | Literal { waarde; typ = Some Datum_tijd; _ } ->
      (* WF-095 (ch. 4.1): the lexeme reads through the SHARED datum-tijd
         reader — one grammar, one instant, no second opinion — and the
         instant enters as the GROUND pair term dt(D, MS), the very split
         [datetime_term] emits for a case value. It is never an absolute ms
         integer, and that is not a stylistic choice: 2026-03-15T12:00:00.000
         is 1 773 576 000 000 ms, which gringo grounds as -245 493 248 (it
         wraps at 32 bits, silently), and two instants 27.7 days apart then
         compare BACKWARDS. The pair keeps both components inside the range,
         so no fence is needed here at all. *)
      let ms =
        match Value.datetime_of_lexeme (req "Literal.waarde" waarde) with
        | Value.Date_time t -> t
        | v -> fault "datum-tijd literal did not read as an instant: %s" (Value.to_string v)
      in
      let d, msod = split_datetime ms in
      ([], string_of_int d, string_of_int msod)
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
        navigatie_bindingen = None; set = false; _ }
    when norm ot = norm subject_ot ->
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "a temporal datum-tijd read has no v0 form (ch. 3.3)";
      let d = fresh ctx "DD" and ms = fresh ctx "DM" in
      ([ Printf.sprintf "val(%s,%s,dt(%s,%s))" subject (str a) d ms ], d, ms)
  | Binair { op = (Plus | Min) as op; links; rechts; _ } ->
      (* datetime ± duration (ch. 3.8): the datum-tijd operand stands on the
         LEFT of plus and min (WF-035, fail-fast behind the verifier); the
         duration is a number carrying a time unit. Sub-day units are linear
         on the ms value with carry across the day boundary; calendar units
         act on the civil day part preserving the ms-of-day. The date operand
         PROPAGATES empty by its bare join; the duration COALESCES (leeg
         counts 0, the datetime unchanged); a fractional count truncates
         toward zero through its scale division. *)
      let le = req "Binair.links" links and re = req "Binair.rechts" rechts in
      let minus = op = Min in
      let dtl = is_datetime_operand (Some le) and dtr = is_datetime_operand (Some re) in
      let date_e, dur_e =
        if dtl && dtr then fault "datum-tijd \xc2\xb1 datum-tijd is ill-formed (WF-035)"
        else if dtl then (le, re)
        else
          fault "duration %s datum-tijd is ill-formed (WF-035: the date stands on the left; \
                 v0 does not commute)"
            (if minus then "\xe2\x88\x92" else "+")
      in
      let tu = dt_time_unit_of ~what:"datum-tijd arithmetic" (Ir_v0.Checks.typ_of_expr dur_e) in
      let da, dd, dm = dt_term ctx ~subject ~subject_ot date_e in
      let bn, tn, sn =
        coalesce0 ctx ~subject ~subject_ot (expr_term ctx ~subject ~subject_ot dur_e)
      in
      (* [dt_time_unit_of] above read the wire `type`; the scale is the
         DECLARATION's, and the divisor is WRITTEN into the ASP text — the
         date-arithmetic sibling, same silent `(T)/0` grounding. *)
      let sn = numeric_scale ~what:"a datum-tijd duration" sn in
      let n_term = if sn = 1 then tn else Printf.sprintf "((%s)/%d)" tn sn in
      let n_term = if minus then Printf.sprintf "(-(%s))" n_term else n_term in
      (match sub_day_ms tu with
      | Some u ->
          datetime_plus_subday ctx ~subject ~subject_ot ~date_atoms:da ~dd ~dm ~dur_atoms:bn
            ~n:n_term ~u
      | None ->
          datetime_plus_calendar ctx ~subject ~subject_ot ~tu ~date_atoms:da ~dd ~dm
            ~dur_atoms:bn ~n:n_term)
  | Functie { kind = (Eerste | Laatste) as kind; argumenten = (_ :: _ :: _ as args); filter; _ } ->
      (* the datum-tijd ordering fold (RUNTIME-SEMANTICS ch. 1.2e + ch. 3.8):
         EERSTE/LAATSTE over >= 2 datum-tijd operands. ref orders the pair
         values totally — civil day first, ms-of-day as tie-break, one instant
         being one (D, MS) pair — and the fold returns VALUES, so a tie is
         value-neutral (agg_eerste/agg_laatste over scalar_cmp). The pair term
         cannot be linearised into one integer (D*86 400 000+MS overflows
         clingo's int range — the very reason dt(D, MS) exists), so the extreme
         is two-stage and lexicographic: the extreme over the civil day, then
         the extreme over the ms-of-day among the operands carrying that day.
         Both stages range over the already-fenced pair components (D and MS
         are each inside 2^31 by the pair-term layer), so no product forms and
         no range guard is needed. An empty operand contributes no tuple (the
         bare join skips it, never 0); all-empty derives no tuple and the
         definedness atom keeps the pair underivable — leeg as absence, never a
         #sup/#inf escaping into a value. *)
      if filter <> None then
        fault "a filter on a listed-operand datum-tijd fold has no v0 form (the subselection \
               ranges over instances)";
      List.iter
        (fun a ->
          if not (is_datetime_operand (Some a)) then
            fault "a non-datum-tijd (or mixed-granularity) operand under a datum-tijd \
                   eerste/laatste fold has no order here (ref faults the unordered comparison)")
        args;
      ctx.fresh <- ctx.fresh + 1;
      let pred = Printf.sprintf "dtf%d" ctx.fresh in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      List.iter
        (fun a ->
          let atoms, d, ms = dt_term ctx ~subject ~subject_ot a in
          emit ctx "%s(%s,%s,%s) :- %s." pred subject d ms
            (String.concat ", " (base :: atoms)))
        args;
      emit ctx "%s_def(%s) :- %s(%s,_,_)." pred subject pred subject;
      let agg = match kind with Eerste -> "#min" | _ -> "#max" in
      let dd = fresh ctx "DFD" and dm = fresh ctx "DFM" in
      let ld = fresh ctx "DLD" and lm = fresh ctx "DLM" in
      ( [ Printf.sprintf "%s_def(%s)" pred subject;
          Printf.sprintf "%s = %s { %s : %s(%s,%s,_) }" dd agg ld pred subject ld;
          Printf.sprintf "%s = %s { %s : %s(%s,%s,%s) }" dm agg lm pred subject dd lm ],
        dd, dm )
  | _ ->
      (* The reason names BOTH sides, because a reader of this line needs to
         know what it may write as well as what it may not. Carried: the
         literal, an own-subject read, datetime ± duration, and the
         eerste/laatste fold over listed operands. Every flank below is a
         SHAPE with no corpus case, each measured verifier-clean and replayed
         by ref (2026-09-06) — so the refusal here is this backend declining a
         form the wire admits, never a claim the form is ill-formed. *)
      fault
        "datum-tijd here is not the pair term's arithmetic. Carried: a datum-tijd literal, an \
         own-subject read, datetime \xc2\xb1 duration, and eerste/laatste over listed operands. \
         A loud flank, no corpus case: a datum-tijd PARAMETER read, a Let/BinderRef-bound \
         datum-tijd (and the residual VariabeleRef), a set-typed fold, a datum-met or \
         datumtijd-met constructor, and a navigated datum-tijd read"

(* datetime + n sub-day units: linear on the ms value with the carry across
   the day boundary. The absolute ms value never forms — the step rides the
   ms-of-day and re-splits by floor division (the floormod trick over gringo's
   toward-zero \\ and /). Fenced so the ms-of-day sum stays inside 2^31. *)
and datetime_plus_subday ctx ~subject ~subject_ot ~date_atoms ~dd ~dm ~dur_atoms ~n ~u =
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  range_guard ctx ~subject
    ~body:((base :: ctx.rule_guard) @ date_atoms @ dur_atoms)
    [ (Printf.sprintf "(%s)" n, (max_int31 - ms_per_day) / u) ];
  let tot = fresh ctx "TOT" and fm = fresh ctx "FMS" and nd = fresh ctx "ND" in
  let atoms =
    date_atoms @ dur_atoms
    @ [ Printf.sprintf "%s = (%s)+((%s)*%d)" tot dm n u;
        Printf.sprintf "%s = ((%s \\ %d)+%d) \\ %d" fm tot ms_per_day ms_per_day ms_per_day;
        Printf.sprintf "%s = (%s)+(((%s)-%s)/%d)" nd dd tot fm ms_per_day ]
  in
  (atoms, nd, fm)

(* datetime + n calendar units: the civil day part steps exactly as a
   day-precision date (linear days/weeks, the month-end clamp via the shared
   dpm helper) and the ms-of-day rides through unchanged (the S6a day
   kernel). *)
and datetime_plus_calendar ctx ~subject ~subject_ot ~tu ~date_atoms ~dd ~dm ~dur_atoms ~n =
  let step_atoms, nd =
    match tu with
    | Tu_dag ->
        let v = fresh ctx "ND" in
        (dur_atoms @ [ Printf.sprintf "%s = (%s)+(%s)" v dd n ], v)
    | Tu_week ->
        let v = fresh ctx "ND" in
        (dur_atoms @ [ Printf.sprintf "%s = (%s)+(7*(%s))" v dd n ], v)
    | Tu_maand ->
        let a, v, _ =
          translate_date_plus_months ctx ~subject ~subject_ot ~date_atoms ~date_term:dd
            ~dur_atoms ~count:n
        in
        (a, v)
    | Tu_kwartaal ->
        let a, v, _ =
          translate_date_plus_months ctx ~subject ~subject_ot ~date_atoms ~date_term:dd
            ~dur_atoms ~count:(Printf.sprintf "(3*(%s))" n)
        in
        (a, v)
    | Tu_jaar ->
        let a, v, _ =
          translate_date_plus_months ctx ~subject ~subject_ot ~date_atoms ~date_term:dd
            ~dur_atoms ~count:(Printf.sprintf "(12*(%s))" n)
        in
        (a, v)
    | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde ->
        (* unreachable: sub_day_ms routed the sub-day units to the linear arm *)
        assert false
  in
  (date_atoms @ step_atoms, nd, dm)

and translate_division ctx ~subject ~subject_ot ~links ~rechts ~rounding =
  let links = req "Binair.links" links and rechts = req "Binair.rechts" rechts in
  numeric_ok (Some links);
  numeric_ok (Some rechts);
  unit_free_ok ctx (Some links);
  unit_free_ok ctx (Some rechts);
  let bl, tl, sl = expr_term ctx ~subject ~subject_ot links in
  let br, tr, sr = expr_term ctx ~subject ~subject_ot rechts in
  (* the numeric_ok pair reads the wire `type`; both scales become gcd
     operands and divisors below, where a 0 is the crash, not a fault *)
  let sl = numeric_scale ~what:"a division dividend" sl in
  let sr = numeric_scale ~what:"a division divisor" sr in
  let out_scale =
    match rounding with
    | None -> max (max sl sr) ctx.write_scale
    | Some (_, d) -> pow10 d
  in
  (* the exact scaled quotient is  tl·(sr·out_scale) / (tr·sl).  A RUNTIME
     divisor reduces only the two constant multipliers by their gcd and rides
     its own term tr·kden, so the scale-(sr·out_scale) numerator can reach 10^8
     at a 4-decimal divisor (the tutorial's n / 2.0000), fencing the dividend at
     5.  A COMPILE-TIME-CONSTANT positive divisor (an atom-free integer term c)
     instead folds the whole constant fraction (sr·out_scale)/(c·sl) by its FULL
     gcd, so the divisor's scale never inflates the numerator — the intermediate
     shrinks (10^8 → 5000 for n / 2.0000) and the fence loosens accordingly.
     The fold is exact: dividing numerator and denominator constants by a common
     factor preserves both the integer quotient (floor of one rational) and its
     divisibility test, so every existing outcome — value, non-exact viol, zero
     fence — is unchanged (S18). *)
  let const_div =
    if br = [] then
      match int_of_string_opt (String.trim tr) with Some c when c > 0 -> Some c | _ -> None
    else None
  in
  let numt, dent, range_bounds =
    let numt_of knum = if knum = 1 then Printf.sprintf "(%s)" tl else Printf.sprintf "((%s)*%d)" tl knum in
    let two_factor () =
      let g = gcd (sr * out_scale) sl in
      let knum = sr * out_scale / g and kden = sl / g in
      ( numt_of knum,
        (if kden = 1 then Printf.sprintf "(%s)" tr else Printf.sprintf "((%s)*%d)" tr kden),
        [ (tl, 536870912 / knum); (tr, 536870912 / kden) ] )
    in
    match const_div with
    | Some c ->
        let knum, kden_c = const_div_reduce ~sl ~sr ~out_scale ~c in
        (* the fold shrinks the denominator to the GROUND constant kden_c, which
           the rounding formulas feed UNGUARDED into 2*dent, numt+dent-1 and
           2*numt+dent. numt is fenced to 2^29 below, but nothing bounds kden_c,
           so a kden_c past 2^29 lets 2*dent or 2*numt+dent leave int31 and wrap
           silently — the very ref!=clingo split the fold otherwise closes. A
           ground guard on the already-wrapped literal cannot fire, so when the
           fold leaves the fence, drop back to the two-factor form: its
           (tr,536870912/kden) range_guard fences the divisor term and fires the
           loud "arithmetic range exceeded" UNSAT, exactly as every constant
           divisor did before the fold existed. *)
        if kden_c > 536870912 then two_factor ()
        else (numt_of knum, Printf.sprintf "(%d)" kden_c, [ (tl, 536870912 / knum) ])
    | None -> two_factor ()
  in
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "q%d" ctx.fresh in
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  let guard = ctx.rule_guard in
  let lbody = String.concat ", " (base :: bl) in
  let rbody = String.concat ", " (base :: br) in
  let full = String.concat ", " ((base :: guard) @ bl @ br) in
  range_guard ctx ~subject ~body:((base :: guard) @ bl @ br) range_bounds;
  emit ctx "%s_ldef(%s) :- %s." p subject lbody;
  emit ctx "%s_rdef(%s) :- %s." p subject rbody;
  emit ctx "%s(%s,0) :- %s, not %s_ldef(%s)." p subject base p subject;
  emit ctx "inconsistent(%s,%s) :- %s, %s_ldef(%s), not %s_rdef(%s)."
    (str ctx.rule_name) subject
    (String.concat ", " (base :: guard))
    p subject p subject;
  emit ctx "viol(\"division by numeric zero\",%s) :- %s, (%s) == 0." subject full tr;
  (match rounding with
  | None ->
      emit ctx "%s(%s,V) :- %s, (%s) != 0, %s \\ %s == 0, V = %s/%s." p subject full tr numt
        dent numt dent;
      emit ctx
        "viol(\"non-exact division (an explicit rounding is mandated before precision is \
         lost)\",%s) :- %s, (%s) != 0, %s \\ %s != 0."
        subject full tr numt dent
  | Some (modus, _) ->
      let value =
        match modus with
        | Richting_nul -> Printf.sprintf "%s/%s" numt dent
        | Naar_beneden -> Printf.sprintf "%s/%s" numt dent
        | Naar_boven | Weg_van_nul -> Printf.sprintf "(%s+%s-1)/%s" numt dent dent
        | Rekenkundig -> Printf.sprintf "(2*%s+%s)/(2*%s)" numt dent dent
      in
      (match modus with
      | Richting_nul -> ()
      | _ ->
          emit ctx
            "viol(\"negative operand under rounded division is not translated\",%s) :- %s, %s < 0."
            subject full numt;
          emit ctx
            "viol(\"negative operand under rounded division is not translated\",%s) :- %s, %s < 0."
            subject full dent);
      let guards =
        match modus with
        | Richting_nul -> Printf.sprintf "(%s) != 0" tr
        | _ -> Printf.sprintf "%s >= 0, %s > 0" numt dent
      in
      emit ctx "%s(%s,V) :- %s, %s, V = %s." p subject full guards value);
  let v = fresh ctx "Q" in
  ([ Printf.sprintf "%s(%s,%s)" p subject v ], v, out_scale)

(* tot de macht (ch. 2.2, arith.pow_): exact integer power. Only a
   compile-time non-negative integer exponent translates: the result
   scale sl^e must be a compile-time constant, and ref's general form
   (a negative exponent inverts first — a division; a fractional
   exponent takes an exact root — the ch. 2.1 OPEN) has no integer
   encoding here. Emptiness: the base PROPAGATES (ch. 1.2 — pow_
   returns Empty on an empty base, no coalesce), which the bare body
   join encodes for free. gringo's ** matches Z.pow on the exercised
   range — (-3)**3 = -27, x**0 = 1 including 0**0 — so signed bases
   translate unfenced; the operand itself is fenced at the largest
   base whose e-th power stays inside 2^31 (the wrap would happen
   during grounding, BEFORE any guard on the result could see it). *)
and translate_power ctx ~subject ~subject_ot ~links ~rechts =
  let links = req "Binair.links" links and rechts = req "Binair.rechts" rechts in
  numeric_ok (Some links);
  numeric_ok (Some rechts);
  unit_free_ok ctx (Some links);
  unit_free_ok ctx (Some rechts);
  let e = const_pow_exponent rechts in
  let b, t, s = expr_term ctx ~subject ~subject_ot links in
  (* [numeric_ok links] read the wire `type`; the range test below divides by
     the declaration's scale *)
  let s = numeric_scale ~what:"a power base" s in
  (* the result scale sl^e, fenced like pow10: beyond 10^9 the scaled
     integers themselves leave clingo's range *)
  let out_scale =
    let rec go acc i =
      if i = 0 then acc
      else if acc > 1_000_000_000 / s then
        fault "the power's result scale %d^%d exceeds clingo's integer range" s e
      else go (acc * s) (i - 1)
    in
    go 1 e
  in
  (* the largest |base| whose e-th power stays inside 2^31, exactly *)
  let pow_le_max k i =
    let rec go acc i = if i = 0 then true else if acc > max_int31 / k then false else go (acc * k) (i - 1) in
    k = 0 || go 1 i
  in
  if e >= 2 then begin
    let bound =
      let rec go k = if pow_le_max (k + 1) e then go (k + 1) else k in
      go 0
    in
    let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
    range_guard ctx ~subject ~body:((base :: ctx.rule_guard) @ b) [ (t, bound) ]
  end;
  (b, Printf.sprintf "((%s)**%d)" t e, out_scale)

and cond_atoms ctx ~subject ~subject_ot (c : conditie) : string list =
  match c with
  | Vergelijking { op = (Gelijk | Ongelijk) as op; links; rechts; periode = None; _ }
    when is_struct_operand links || is_struct_operand rechts ->
      (* ch. 11.2: record equality is POSITIVE ground-term unification, the
         sum-type pattern (S14). Both operands bind their record term (an own
         attribute read via its val atom, a StructLiteral as its computed
         term); Gelijk tests == — a leeg operand binds no term, so equality
         fails (Tabel 38's non-numeric branch, the case3 dubbel-afwezig corner).
         Ongelijk is now the Tabel-44 uniform inequality (neq_helper): exactly
         one empty → waar (case3 afwijkend now aanwezig), both empty → onwaar.
         Nominal identity is carried by the functor itself (= the declaring
         domain); WF-072 precludes a cross-domain comparison statically. *)
      let ll, tl = struct_term ctx ~subject ~subject_ot (req "links" links) in
      let rr, tr = struct_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      if op = Gelijk then ll @ rr @ [ Printf.sprintf "%s == %s" tl tr ]
      else neq_helper ctx ~subject ~subject_ot (ll, tl) (rr, tr)
  | Vergelijking { op = (Gelijk | Ongelijk) as op; links; rechts; periode = None; _ }
    when is_sum_operand links || is_sum_operand rechts ->
      (* ch. 9.3: sum-typed equality is POSITIVE ground-term unification.
         Both operand terms bind (an own attribute read via its val atom, a
         Construct as a bare ground term); Gelijk tests T1 == T2 — a leeg
         operand fails the positive join, so equality is false (Tabel 38, the
         non-numeric branch). Ongelijk is the Tabel-44 uniform inequality
         (neq_helper): exactly one empty → waar (beschikking leeg ≠ Aangehouden
         fires afgehandeld), both empty → onwaar. The one-empty-true cell needs
         the eqN-negation route — a bare `not gelijk` positive join could not
         express it (S5's non-complement warning is why the numeric row already
         goes through eqN, and the inequality is now uniform with it). *)
      let ll, tl = sum_term ctx ~subject ~subject_ot (req "links" links) in
      let rr, tr = sum_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      if op = Gelijk then ll @ rr @ [ Printf.sprintf "%s == %s" tl tr ]
      else neq_helper ctx ~subject ~subject_ot (ll, tl) (rr, tr)
  | Vergelijking { op = (Gelijk | Ongelijk) as op; links; rechts; periode = None; _ }
    when is_symbolic_operand links || is_symbolic_operand rechts ->
      (* ch. 1.2d text/enum/boolean comparison. Gelijk is POSITIVE ground-term
         comparison — both operands bind (an own attribute read via its val
         atom, a literal as a ground term) and Gelijk tests ==; ANY empty
         operand fails the unbound positive join, so equality is false (Tabel
         38, unchanged — the equality asymmetry between numeric leeg=leeg-waar
         and this non-numeric leeg=leeg-onwaar stays). Ongelijk is now uniform
         with the numeric row (Tabel 44): exactly one empty → waar (etiket leeg
         ≠ 'zeevondst' fires omstreden), both empty → onwaar. It takes the
         eqN-negation route (neq_helper) because a positive != join cannot
         express the one-empty-true cell. *)
      let ll, tl = symbolic_term ctx ~subject ~subject_ot (req "links" links) in
      let rr, tr = symbolic_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      if op = Gelijk then ll @ rr @ [ Printf.sprintf "%s == %s" tl tr ]
      else neq_helper ctx ~subject ~subject_ot (ll, tl) (rr, tr)
  | Vergelijking { op; links; rechts; periode = None; _ }
    when is_datetime_operand links || is_datetime_operand rechts ->
      (* THE DATUM-TIJD ORDERING (RUNTIME-SEMANTICS ch. 1.2e + ch. 3.8).
         Every other comparison arm below harmonises INTEGER SCALES; a
         datum-tijd has none — [decimals_of_datatype] refuses it, which is
         why a datetime ordering carrying no literal at all used to fault
         `unscalable datatype` here while ref replayed the same document
         exactly. The scale question has one answer and the pair term
         already gives it: an instant rides dt(D, MS), the civil day beside
         the ms-of-day, because the absolute ms value does not fit gringo's
         32-bit grounding (see the [dt_term] Literal arm for the measured
         wrap). So the ordering is LEXICOGRAPHIC over the pair: the civil
         day decides, the ms-of-day breaks the tie — exactly ref's total
         order on Date_time.

         A disjunction cannot stand in an ASP body, so the two cases become
         two rules for one helper atom. Both are POSITIVE joins over the
         operands' own binding atoms, so an empty operand simply fails to
         bind and the comparison is false — the ch. 1.2d date row, for free,
         the same way the day-precision ordering below gets it. *)
      let le = req "Vergelijking.links" links and re = req "Vergelijking.rechts" rechts in
      if not (is_datetime_operand (Some le) && is_datetime_operand (Some re)) then
        fault
          "a datum-tijd comparison against a non-datum-tijd operand has no order here (WF-036: \
           one instant, one representation; ref faults the unordered comparison)";
      let day_sym, ms_sym =
        match op with
        | Eerder | Kleiner -> ("<", "<")
        | Eerder_of_gelijk | Kleiner_of_gelijk -> ("<", "<=")
        | Later | Groter -> (">", ">")
        | Later_of_gelijk | Groter_of_gelijk -> (">", ">=")
        | Gelijk | Ongelijk ->
            (* the equality row is a different shape, not a tighter one: the
               numeric arm's eqN helper harmonises scales the pair term does
               not have, and Tabel 44's one-empty-true cell needs the
               negation route over it. No corpus case, so it is a loud flank
               rather than an untested encoding. *)
            fault
              "datum-tijd (on)gelijk-aan is not translated (the pair term carries no scale for \
               the eqN harmonisation; no corpus case exercises it)"
      in
      let a1, d1, m1 = dt_term ctx ~subject ~subject_ot le in
      let a2, d2, m2 = dt_term ctx ~subject ~subject_ot re in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      let body = String.concat ", " ((base :: a1) @ a2) in
      ctx.fresh <- ctx.fresh + 1;
      let p = Printf.sprintf "dtcmp%d" ctx.fresh in
      emit ctx "%s(%s) :- %s, %s %s %s." p subject body d1 day_sym d2;
      emit ctx "%s(%s) :- %s, %s == %s, %s %s %s." p subject body d1 d2 m1 ms_sym m2;
      [ Printf.sprintf "%s(%s)" p subject ]
  | Vergelijking { op = (Gelijk | Ongelijk) as op; links; rechts; periode = None; _ }
    when not (op = Gelijk && (is_date_operand links || is_date_operand rechts)) ->
      (* the ch. 1.2d NUMERIC equality asymmetry (arith.compare_op):
         gelijk-aan is TRUE on two empty operands and FALSE on exactly
         one; ongelijk-aan is its exact complement. Encoded through a
         per-comparison helper: eqN(S) holds iff both operand bodies
         bind and the harmonised values match, or neither binds.
         Ongelijk is negation-as-failure over the helper — sound for
         the same reason coalesce0's definedness negation is: the
         helper reads only the operand bodies. The DATE row narrows
         this (RS ch. 1.2d, v0's Tabel-38 guard on the date shape):
         date-typed gelijk-aan must be onwaar on two empty operands, so
         the when-guard routes it past the both-unbound arm to the bare
         positive join below (any empty operand false — the guard for
         free). Date-typed ongelijk-aan STAYS here: the complement's
         cells (exactly-one-empty waar, both-empty onwaar) are the date
         row's own. *)
      units_identical_ok ~what:"a comparison"
        (Option.bind links (eff_unit ctx))
        (Option.bind rechts (eff_unit ctx));
      let l = expr_term ctx ~subject ~subject_ot (req "links" links) in
      let r = expr_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      let b1, t1, s1 = l and b2, t2, s2 = r in
      (* the sum/struct/symbolic arms above route by the wire `type`; the scale
         is the declaration's, so it catches what an absent or lying annotation
         let through — the numeric equality arm cannot harmonise a ground
         constructor term against an integer *)
      let s1 = numeric_scale ~what:"a numeric equality" s1 in
      let s2 = numeric_scale ~what:"a numeric equality" s2 in
      let m = max s1 s2 in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      (* [rule_guard] rides the fence body: inside a composite it carries the
         ch. 5.10 prefix, so a member the outcome no longer reads cannot fire
         this range viol either (S7's claim is about EVERY emission, not only
         the arithmetic ones). Outside a composite the guard is empty and the
         body is unchanged. *)
      range_guard ctx ~subject
        ~body:((base :: ctx.rule_guard) @ b1 @ b2)
        [ (t1, max_int31 / (m / s1)); (t2, max_int31 / (m / s2)) ];
      let bindings, tl, tr = harmonised l r in
      ctx.fresh <- ctx.fresh + 1;
      let p = Printf.sprintf "eq%d" ctx.fresh in
      emit ctx "%s_l(%s) :- %s." p subject (String.concat ", " (base :: b1));
      emit ctx "%s_r(%s) :- %s." p subject (String.concat ", " (base :: b2));
      emit ctx "%s(%s) :- %s, %s == %s." p subject
        (String.concat ", " (base :: bindings))
        tl tr;
      emit ctx "%s(%s) :- %s, not %s_l(%s), not %s_r(%s)." p subject base p subject p subject;
      [ (if op = Gelijk then "" else "not ") ^ Printf.sprintf "%s(%s)" p subject ]
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      (* the orderings, plus date-typed gelijk-aan (routed here by the
         when-guard above): bare positive joins, any empty operand false *)
      units_identical_ok ~what:"a comparison"
        (Option.bind links (eff_unit ctx))
        (Option.bind rechts (eff_unit ctx));
      let l = expr_term ctx ~subject ~subject_ot (req "links" links) in
      let r = expr_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      let b1, t1, s1 = l and b2, t2, s2 = r in
      (* an ORDERING over a ground constructor term has no meaning: clingo's
         term order would answer where ref faults the unordered comparison.
         With both scales 0 the harmonisation width m is 0 too, and the range
         guard below divides max_int31 by (m / s1) — 0/0. Measured with the
         fence removed: `Fatal error: exception Division_by_zero`, exit 2. The
         two silent siblings, where the same 0/0 never forms because their
         scaling helper skips the division at s = m, are [body_condition] and
         [value_condition]. *)
      let s1 = numeric_scale ~what:"an ordering comparison" s1 in
      let s2 = numeric_scale ~what:"an ordering comparison" s2 in
      let m = max s1 s2 in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      (* [rule_guard] rides the fence body — the ch. 5.10 prefix inside a
         composite, empty and behaviour-neutral outside one (see the equality
         arm above) *)
      range_guard ctx ~subject
        ~body:((base :: ctx.rule_guard) @ b1 @ b2)
        [ (t1, max_int31 / (m / s1)); (t2, max_int31 / (m / s2)) ];
      let bindings, tl, tr = harmonised l r in
      let sym = match op with Gelijk -> "==" | op -> vergelijk_sym op in
      bindings @ [ Printf.sprintf "%s %s %s" tl sym tr ]
  | Kenmerk_check { kenmerk; navigatie = None; negatie; navigatie_binding; eigenaar = _; _ } ->
      (match navigatie_binding with
      | Some _ ->
          fault "navigation binding on a characteristic check that does not navigate (WF-053)"
      | None -> ());
      let k = req "kenmerk" kenmerk in
      [ (if negatie then "not " else "") ^ Printf.sprintf "heeft(%s,%s)" subject (str k) ]
  | Kenmerk_check { kenmerk; navigatie = Some nav; negatie; navigatie_binding; eigenaar = _; _ }
    ->
      (* the navigated check (ch. 5.8): the hop is a POSITIVE literal;
         only the characteristic sits under the negation. An absent link
         therefore fails the positive and the negated form alike — the
         deliberate v0 asymmetry, encoded for free. *)
      let k = req "kenmerk" kenmerk in
      let env =
        { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
          as_of = None; locals = Eval.SMap.empty }
      in
      let role =
        match navigatie_binding with
        | Some rb -> Eval.bound_role env rb
        | None ->
            (* deferred, loudly — mirroring ref: the ch. 4.1a zero-hop
               self-reference is scoped to the attribute read; a
               self-denoting name on the navigated check faults
               explicitly rather than silently reading the own
               characteristic *)
            if
              Eval.reachable_roles_by_name env ~self_type:subject_ot ~name:nav = []
              && Eval.self_denoting_role ctx.doc ~self_type:subject_ot nav
            then
              fault
                "the navigated characteristic check's role %S names a role %S itself fills \
                 — the zero-hop self-reference does not extend to the characteristic check \
                 (deferred; WF-053, IR-SPEC ch. 4.1a)"
                nav subject_ot
            else Eval.role_by_name env ~self_type:subject_ot ~name:nav
      in
      if role.rol_meervoudig then
        fault
          "the navigated check reaches the to-many role %S in a scalar condition (WF-053)"
          (Option.value role.rol_naam ~default:"?");
      let n = fresh ctx "N" in
      [ Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical role)) n;
        (if negatie then "not " else "") ^ Printf.sprintf "heeft(%s,%s)" n (str k) ]
  | Rollidmaatschap { ander_te_filler; negatie; _ } ->
      (* the near-side rolcheck (RUNTIME-SEMANTICS ch. 5.8a): the subject fills
         the role iff its to-one reference to the OTHER end is set. The
         connecting role to [ander_te_filler] is the fact-type route (v0's
         verbindendeRol, shared with ref via Eval.connecting_role); an input
         link is a rol/3 fact. Positive: the join binds a filler N — absent
         link, no fire (leeg-on-absence for free). Negation ("is geen"): a
         helper derives the link's presence, negated, with the subject kept
         bound by the caller's is_a base — the inverse null-test. A to-many
         connecting reference is v0's marker, refused loudly, mirroring ref. *)
      let other = req "Rollidmaatschap.anderTeFiller" ander_te_filler in
      let env =
        { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
          as_of = None; locals = Eval.SMap.empty }
      in
      let role = Eval.connecting_role env ~from_type:subject_ot ~to_type:other in
      if role.rol_meervoudig then
        fault
          "the rolcheck's connecting reference to %S is the to-many role %S (ch. 5.5): the \
           near-side null-test needs a to-one reference — v0 markers it"
          other
          (Option.value role.rol_naam ~default:"?");
      let canonical = role_canonical role in
      if negatie then begin
        ctx.fresh <- ctx.fresh + 1;
        let p = Printf.sprintf "rm%d" ctx.fresh in
        let n = fresh ctx "N" in
        emit ctx "%s(%s) :- is_a(%s,%s), rol(%s,%s,%s)." p subject subject (str subject_ot) subject
          (str canonical) n;
        [ Printf.sprintf "not %s(%s)" p subject ]
      end
      else
        let n = fresh ctx "N" in
        [ Printf.sprintf "rol(%s,%s,%s)" subject (str canonical) n ]
  | Samengesteld { kwantor = Alle | De; onderdelen; _ } ->
      (* the conjunctive short circuit (RUNTIME-SEMANTICS ch. 5.10): member i is
         read only where members 1..i-1 all held, so the accumulated PREFIX
         atoms are exactly the "outcome does not stand yet" guard. Threading
         them through [rule_guard] makes every fence, viol and §9.5 deposit the
         member emits prefix-aware — a divisor test in member 1 now gates the
         division in member 2, ref's ch. 5.10 laziness on the wire. The
         returned conjunction is the same list as before, so the composite's
         TRUTH VALUE is untouched; only what the unread members deposit is. *)
      let saved = ctx.rule_guard in
      let atoms =
        List.fold_left
          (fun prefix c ->
            ctx.rule_guard <- saved @ prefix;
            let a = cond_atoms ctx ~subject ~subject_ot c in
            prefix @ a)
          [] onderdelen
      in
      ctx.rule_guard <- saved;
      atoms
  | Samengesteld
      { kwantor = (Geen | Ten_minste | Ten_hoogste | Precies) as kwantor; drempel; onderdelen; _ }
    ->
      (* the counting quantifiers (ref eval_cond: hits over the member list —
         GEEN hits = 0, TEN MINSTE hits >= d, TEN HOOGSTE hits <= d, PRECIES
         hits = d; the decision-table desugar's disjunctive row is TEN_MINSTE
         1). Each member becomes its own helper rule — cqK_m<i>(S) :-
         is_a(S,ot), ⟨member atoms⟩, derivable exactly where ref counts the
         member as a hit (an unbound operand fails the positive join: a leeg
         member counts false, Tabel 38 for free) — and a PREFIX CHAIN
         cqK_h<i>(S,H) carries the hits among the first i members, link by
         link, so the composite itself contributes only plain atoms (the count
         read plus its comparison): it serves every position cond_atoms serves,
         aggregate elements included, where a body aggregate could not.

         The chain replaces the earlier single #count aggregate because the
         ch. 5.10 SHORT CIRCUIT needs the prefix count as a GUARD on member i's
         own emissions ("the outcome does not stand yet after the first i-1
         members" — the stop triggers, negated), and a guard reading a count
         over one shared cq(S,I) predicate would close a predicate-level loop
         through the chain's negative link. Naming every link and every member
         after its index keeps each guard edge pointing at a strictly smaller
         index, so the dependency graph is acyclic by construction and the
         ground program stays stratified — as does the counted value: the chain
         over cqK_m<i> counts exactly what #count over cq(S,I) counted.

         Suppressing a settled member's emissions cannot move the verdict.
         Writing h_k for the hits after k members and r = n - k for the unread
         rest, every trigger below is stable under later members: h only grows
         and h + r only shrinks, so h_k ≥ d, h_k > d, h_k > 0 and h_k + r < d
         each already decide the final comparison. A member the guard silences
         therefore only ever loses a hit the verdict no longer reads.
         Soundness of the non-monotone quantifiers rides the verifier's
         conservative dependency edges (GEEN/TEN_HOOGSTE/PRECIES members are
         negative in the WF-045/046 stratification walk). *)
      if onderdelen = [] then
        fault "a counting composite without members is not translated (no corpus case)";
      ctx.fresh <- ctx.fresh + 1;
      let p = Printf.sprintf "cq%d" ctx.fresh in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      let n = List.length onderdelen in
      let saved = ctx.rule_guard in
      emit ctx "%s_h0(%s,0) :- %s." p subject base;
      List.iteri
        (fun i c ->
          (* [i] members already read, [n - i] still unread. The guard is the
             ch. 5.10 stop triggers NEGATED over the prefix count, and it is
             built for EVERY i — member 1 included. Member 1 is not always
             read: at TEN MINSTE / PRECIES with a threshold ABOVE the member
             count the out-of-reach trigger fires on the EMPTY prefix, and ref
             then reads nothing at all. Reading cq*_h0(S,0) — the chain's own
             base, derivable for every subject — makes that case fall out of
             the same comparison as every later one (H = 0, so `H >= d - n`
             holds exactly when d <= n) instead of needing its own arm. *)
          let unsettled =
              let h = fresh ctx "H" in
              let left = n - i in
              let cmp =
                match kwantor with
                | Geen -> [ Printf.sprintf "%s == 0" h ]
                | Ten_minste ->
                    [ Printf.sprintf "%s < %d" h drempel;
                      Printf.sprintf "%s >= %d" h (drempel - left) ]
                | Ten_hoogste ->
                    (* two-sided since the at-most TRUE trigger landed
                       (maintainer decision 2026-09-07): read member i only
                       while the outcome can still go BOTH ways — hits have not
                       exceeded d, and the unread rest can still push them past
                       it. H + left > d is written H >= d - left + 1. At i = 0
                       with d >= n that second half already fails, so a
                       vacuously-true at-most reads nothing. *)
                    [ Printf.sprintf "%s <= %d" h drempel;
                      Printf.sprintf "%s >= %d" h (drempel - left + 1) ]
                | Precies ->
                    [ Printf.sprintf "%s <= %d" h drempel;
                      Printf.sprintf "%s >= %d" h (drempel - left) ]
                | Alle | De ->
                    fault "unreachable: a conjunctive quantifier in the counting branch"
              in
              Printf.sprintf "%s_h%d(%s,%s)" p i subject h :: cmp
          in
          ctx.rule_guard <- saved @ unsettled;
          let atoms = cond_atoms ctx ~subject ~subject_ot c in
          emit ctx "%s_m%d(%s) :- %s." p (i + 1) subject (String.concat ", " (base :: atoms));
          emit ctx "%s_h%d(%s,H1) :- %s_h%d(%s,H0), %s_m%d(%s), H1 = H0+1." p (i + 1) subject p i
            subject p (i + 1) subject;
          emit ctx "%s_h%d(%s,H0) :- %s_h%d(%s,H0), not %s_m%d(%s)." p (i + 1) subject p i subject
            p (i + 1) subject)
        onderdelen;
      ctx.rule_guard <- saved;
      let h = fresh ctx "H" in
      let compare =
        match kwantor with
        | Geen -> Printf.sprintf "%s == 0" h
        | Ten_minste -> Printf.sprintf "%s >= %d" h drempel
        | Ten_hoogste -> Printf.sprintf "%s <= %d" h drempel
        | Precies -> Printf.sprintf "%s == %d" h drempel
        | Alle | De -> fault "unreachable: a conjunctive quantifier in the counting branch"
      in
      [ Printf.sprintf "%s_h%d(%s,%s)" p n subject h; compare ]
  | Predicaat { soort = Gevuld; operand; negatie = false; dagsoort = None; periode = None; _ } ->
      (* the gevuld gate (ref eval: value <> leeg): the operand's binding atoms
         as a positive join. A struct operand rides struct_term (its val atom),
         any other operand rides expr_term — either way absence leaves the atoms
         unbound, so the gated rule does not fire (leeg-on-absence for free). The
         value itself is discarded; only presence matters. *)
      let operand = req "Predicaat.operand" operand in
      if is_struct_operand (Some operand) then fst (struct_term ctx ~subject ~subject_ot operand)
      else
        let atoms, _, _ = expr_term ctx ~subject ~subject_ot operand in
        atoms
  | _ -> fault "condition kind not translated (no corpus case exercises it)"

(* the ground term of a Construct (ch. 9.1): a Literal payload scaled to the
   constructor's DECLARED payload datatype; the nullary case is the bare
   functor. This route returns a term and NO binding atoms, so it admits only
   a payload that is already ground; a payload that must be BOUND (an
   attribute read, a parameter, an arithmetic combination) needs an atom the
   calling position has nowhere to put — the callers that can carry atoms
   ([expr_term], [closed_term]) lower such a payload themselves. *)
and construct_ground_term ctx typ constructor_ argument =
  let constructoren = sum_constructors typ in
  let f = ctor_functor constructor_ in
  match (ctor_payload_datatype constructoren constructor_, argument) with
  | None, None -> f
  | None, Some _ -> fault "nullary constructor %S built with an argument (WF-062)" constructor_
  | Some _, None -> fault "unary constructor %S built without an argument (WF-062)" constructor_
  | Some d, Some (Literal { waarde = Some lex; _ }) ->
      ignore ctx;
      Printf.sprintf "%s(%d)" f
        (scaled_of_value ~decimals:(decimals_of_datatype (Some d))
           (Value.Num (Value.q_of_lexeme lex, Value.Unitless)))
  | Some _, Some _ ->
      fault
        "a Construct payload other than a literal is not a ground term (a payload that must be \
         bound needs a body atom this position cannot carry; no corpus case)"

(* a sum-typed comparison operand → (body atoms binding a term, the term):
   an own-subject attribute read binds its val atom's value; a Construct is a
   bare ground term with no atom. Both are positive, so a leeg attribute read
   simply fails the join (Tabel 38 for free). *)
and sum_term ctx ~subject ~subject_ot (e : expr) : string list * string =
  match e with
  | Construct { typ; constructor_; argument; _ } ->
      ([], construct_ground_term ctx typ constructor_ argument)
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
        navigatie_bindingen = None; set = false; _ }
    when norm ot = norm subject_ot ->
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "temporal sum carriage is a recorded flank; no corpus case";
      let v = fresh ctx "V" in
      ([ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ], v)
  | _ ->
      fault
        "a sum-typed operand here is an own-subject attribute read or a Construct (no corpus case \
         for more)"

(* positional field projection (RUNTIME-SEMANTICS ch. 11.1): given the base's
   already-bound struct TERM and the base expr (for its StructType), unify the
   term against the domain functor with the declared field's 1-based position
   carrying a fresh variable and an anonymous variable elsewhere — [bterm] =
   dossier(W,_) binds W. Returns the unification atom, the field variable and
   the FIELD's declared scale (never the base's — a struct term is scale 0). *)
and project_field ctx ~base ~bterm ~veld : string * string * int =
  let domein, velden = struct_type_of base in
  let idx, fdt =
    let rec find i = function
      | [] -> fault "field %S is not a declared field of struct %S (WF-074)" veld domein
      | (n, dt) :: tl -> if norm n = norm veld then (i, dt) else find (i + 1) tl
    in
    find 0 velden
  in
  let fvar = fresh ctx "W" in
  let pat = List.mapi (fun i _ -> if i = idx then fvar else "_") velden in
  ( Printf.sprintf "%s = %s(%s)" bterm (ctor_functor domein) (String.concat "," pat),
    fvar,
    value_scale (Some fdt) )

(* resolve a struct-typed base/operand to (atoms binding its ground term, the
   term): an own-subject struct-typed attribute read rides its val atom (a leeg
   read binds nothing → Tabel 38's both-false corner and FieldAccess-on-leeg
   propagation for free), a bound binder (a lambda parameter over a struct list
   element) resolves to its ASP variable, and a struct-typed FieldAccess (a
   struct-in-struct: a field access whose base is itself a struct-typed field
   access) resolves RECURSIVELY — the inner base binds its record term, that
   term projects the struct field (project_field, the intermediate field's own
   scale 0), and the projected variable is the record term the outer access
   projects against. A leeg anywhere in the chain binds nothing, so the whole
   projection is underivable — leeg propagation for free at every depth. *)
and struct_base ctx ~subject ~subject_ot (e : expr) : string list * string =
  match e with
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
        navigatie_bindingen = None; set = false; _ }
    when norm ot = norm subject_ot ->
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "temporal struct carriage is a recorded flank; no corpus case";
      let v = fresh ctx "B" in
      ([ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ], v)
  | Binder_ref { naam; _ } -> (
      match List.assoc_opt (norm naam) ctx.binders with
      | Some (atoms, var, _) -> (atoms, var)
      | None -> fault "unbound binder %S in a struct field access (WF-064 should preclude this)" naam)
  | Field_access { base; veld; _ } ->
      let batoms, bterm = struct_base ctx ~subject ~subject_ot base in
      let unif, fvar, _fscale = project_field ctx ~base ~bterm ~veld in
      (batoms @ [ unif ], fvar)
  | _ ->
      fault
        "a struct field-access base here is an own-subject attribute read, a bound binder or a \
         struct-typed field access (no corpus case for more)"

(* the term for a StructLiteral in value/equality position (the computed
   record — the 5b computed-Construct pattern): each declared field's value is
   translated in FIXED DECLARATION ORDER and wrapped in the domain functor; a
   field evaluating leeg binds no atom, so the whole literal is underivable —
   the no-hole propagation. Each field's static scale must equal its declared
   scale (WF-073 guarantees the shape; this pins the encoding). *)
and struct_literal_term ctx ~subject ~subject_ot ~typ ~velden : string list * string =
  let domein, decls = struct_type_of_datatype typ in
  let atoms, terms =
    List.fold_left
      (fun (atoms, terms) (fname, fdt) ->
        let fexpr =
          match List.find_opt (fun (n, _) -> norm n = norm fname) velden with
          | Some (_, e) -> e
          | None -> fault "StructLiteral is missing declared field %S (WF-073)" fname
        in
        let a, t, s = expr_term ctx ~subject ~subject_ot fexpr in
        if s <> value_scale (Some fdt) then
          fault "a StructLiteral field %S scale %d does not match its declared scale %d" fname s
            (value_scale (Some fdt));
        (atoms @ a, terms @ [ t ]))
      ([], []) decls
  in
  (atoms, Printf.sprintf "%s(%s)" (ctor_functor domein) (String.concat "," terms))

(* a struct-typed comparison operand → (atoms binding its ground term, the
   term): an own-subject attribute read via its val atom, a StructLiteral as
   its computed/ground term. Both are positive, so a leeg attribute read fails
   the join (Tabel 38 for free), the sum-type pattern. *)
and struct_term ctx ~subject ~subject_ot (e : expr) : string list * string =
  match e with
  | Struct_literal { typ; velden; _ } -> struct_literal_term ctx ~subject ~subject_ot ~typ ~velden
  | _ -> struct_base ctx ~subject ~subject_ot e

(* the scrutinee as a pattern atom for one match arm: the val atom carries
   the constructor pattern; a unary arm's payload variable is bound by it *)
and scrutinee_pattern ctx ~subject ~subject_ot ~scrutinee ~pattern =
  match scrutinee with
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
        navigatie_bindingen = None; set = false; _ }
    when norm ot = norm subject_ot ->
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "temporal sum carriage is a recorded flank; no corpus case";
      [ Printf.sprintf "val(%s,%s,%s)" subject (str a) pattern ]
  | _ ->
      fault
        "a match scrutinee here is an own-subject sum-typed attribute read (no corpus case for more)"

(* total match (ch. 9.2): one rule per arm into a matchN(S,Result) helper —
   no NAF (WF-063 totality makes the functor in the scrutinee's val atom
   select the arm), so the stratification posture is untouched. A leeg
   scrutinee binds no val atom, no arm rule fires, matchN stays underived —
   propagation for free. The unary arm's payload variable rides both the
   binder environment (so a BinderRef resolves to it) and the rule guard (so
   any viol/2 the arm body emits carries the scrutinee atom that binds it).

   The ENUMERATION scrutinee (the keuzetype family's other wire variant,
   IR-SPEC ch. 2.4) reaches the SAME guarantees by a DIFFERENT route, and the
   two rule shapes differ (S14). The sum arm puts the constructor pattern
   INSIDE the val atom (val(S,scrut,ctor(P)) — term unification selects the
   arm). The enum arm cannot: [symbolic_term] binds the label OUT of the val
   atom into a fresh variable and the arm's ground label term is matched
   against it by a builtin equality (val(S,scrut,V), V = "label"). Shared is
   what the semantics rests on: one rule per arm, mutually exclusive ground
   patterns, no NAF, leeg propagates by underivability. That is why the ch. 9.2
   comparison-chain lowering needs no encoding of its own here: mutually
   exclusive ground equalities ARE the chain. The route costs a narrower
   carrier — [symbolic_term] takes a literal or an own-subject attribute read
   only, so an enum-typed PARAMETER scrutinee refuses there where the sum
   route's expr_term fallback would carry it (S19; no corpus case). *)
and translate_match ctx ~subject ~subject_ot ~typ ~scrutinee ~arms =
  let scrut_typ = Ir_v0.Checks.typ_of_expr scrutinee in
  let enum_scrutinee = match scrut_typ with Some (Enumeratie _) -> true | _ -> false in
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "match%d" ctx.fresh in
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  let out_scale = value_scale typ in
  (* the scrutinee's pattern carrier. Sum: an own-subject attribute read
     carries the constructor pattern in its val atom (functor unification);
     any other sum-valued scrutinee (a Fold — the reduce lowering's
     extraction, ch. 10.4) is evaluated once to a bound term matched by
     builtin equality. Enumeration: the S2 symbolic operand — it binds the
     label out of the val atom (or is a ground literal) and carries the
     text/enum/boolean fences, temporal carriage among them — and the arm's
     label is matched against it by builtin equality, which is the ch. 9.2
     comparison chain written out. *)
  let arm_scrut =
    if enum_scrutinee then (
      let satoms, sterm = symbolic_term ctx ~subject ~subject_ot scrutinee in
      fun pattern -> satoms @ [ Printf.sprintf "%s = %s" sterm pattern ])
    else
      match scrutinee with
      | Attribuut_ref
          { attribuut = Some _; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
            navigatie_bindingen = None; set = false; _ }
        when norm ot = norm subject_ot ->
          fun pattern -> scrutinee_pattern ctx ~subject ~subject_ot ~scrutinee ~pattern
      | _ ->
          let satoms, sterm, _ = expr_term ctx ~subject ~subject_ot scrutinee in
          fun pattern -> satoms @ [ Printf.sprintf "%s = %s" sterm pattern ]
  in
  List.iter
    (fun (ctor, binder, body) ->
      let pattern, binding =
        if enum_scrutinee then (
          (* WF-063: an enumeration alternative is nullary, so its arm binds
             nothing; the pattern is the label's own ground term *)
          match binder with
          | Some b -> fault "enumeration match arm %S carries a binder %S (WF-063)" ctor b
          | None -> (symbolic_literal_term (Option.get scrut_typ) ctor, None))
        else
          let constructoren = sum_constructors scrut_typ in
          let f = ctor_functor ctor in
          match (ctor_payload_datatype constructoren ctor, binder) with
          | None, None -> (f, None)
          | Some d, Some b ->
              let pv = fresh ctx "P" in
              ( Printf.sprintf "%s(%s)" f pv,
                Some (norm b, ([], pv, pow10 (decimals_of_datatype (Some d)))) )
          | Some _, None -> fault "unary match arm %S lacks a binder (WF-063)" ctor
          | None, Some _ -> fault "nullary match arm %S carries a binder (WF-063)" ctor
      in
      let scrut_atoms = arm_scrut pattern in
      let saved_guard = ctx.rule_guard and saved_binders = ctx.binders in
      ctx.rule_guard <- ctx.rule_guard @ scrut_atoms;
      (match binding with Some b -> ctx.binders <- b :: ctx.binders | None -> ());
      let body_atoms, body_term, body_scale = expr_term ctx ~subject ~subject_ot body in
      ctx.rule_guard <- saved_guard;
      ctx.binders <- saved_binders;
      if body_scale <> out_scale then
        fault "match arm %S result scale %d does not match the match's declared scale %d" ctor
          body_scale out_scale;
      emit ctx "%s(%s,%s) :- %s." p subject body_term
        (String.concat ", " ((base :: scrut_atoms) @ body_atoms)))
    arms;
  let v = fresh ctx "M" in
  ([ Printf.sprintf "%s(%s,%s)" p subject v ], v, out_scale)

(* a list-valued operand → (list-id term, element scale). A list value is
   reified as indexed facts (S15): [lelem(L,I,V)] for the element at 1-based
   index [I], and [lpres(L)] marking the list present (a present-but-empty
   list carries lpres and no lelem — the value/absence distinction ch. 10.1
   needs). Own list-typed attributes and list parameters take their facts
   from [translate_case]; a MapList/FilterList derives its own, keyed by a
   fresh functor over the inner list-id so distinct subjects' lists never
   collide. The subject variable rides an attribute list-id, so the same
   derivation rules serve every subject at once. *)
and list_source ctx ~subject ~subject_ot (e : expr) : string * int =
  match e with
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
        navigatie_bindingen = None; set = false; _ }
    when norm ot = norm subject_ot ->
      let element =
        match Ir_v0.Checks.find_ci (objecttype ctx ot).attributen a with
        | Some (Some (List_type { element })) -> element
        | _ -> fault "the fold source %S is not a list-typed own attribute (no corpus case)" a
      in
      (Printf.sprintf "alist(%s,%s)" subject (str a), element_scale_of element)
  | Parameter_ref { parameter = Some p; _ } -> (
      match Ir_v0.Checks.find_ci ctx.doc.parameters p with
      | Some { par_datatype = Some (List_type { element }); _ } ->
          (Printf.sprintf "plist(%s)" (str p), element_scale_of element)
      | Some _ -> fault "the fold source parameter %S is not list-typed" p
      | None -> fault "unknown parameter %S" p)
  | Map_list { typ; list; functie; _ } ->
      let inner, inner_es = list_source ctx ~subject ~subject_ot list in
      let out_es =
        element_scale_of
          (match typ with
          | Some (List_type { element }) -> element
          | _ -> fault "a MapList without its ListType annotation (WF-070)")
      in
      ctx.fresh <- ctx.fresh + 1;
      let mapid = Printf.sprintf "lm%d(%s)" ctx.fresh inner in
      let pname, body =
        match functie with
        | Fn_lambda { parameters = [ (pn, _) ]; body; _ } -> (pn, body)
        | Fn_lambda _ -> fault "a MapList lambda takes exactly one parameter (WF-070)"
        | Fn_name _ ->
            (* WF-069 rejects this before translation; an unreachable backstop *)
            fault "a named FunctionDef as a MapList operand is refused (WF-069: the operand must be an inline lambda)"
      in
      let base = [ Printf.sprintf "lelem(%s,I,V)" inner ] in
      let saved = ctx.binders in
      ctx.binders <- (norm pname, ([], "V", inner_es)) :: ctx.binders;
      let branches = body_branches ctx ~guard_body:base ~witness:mapid body in
      ctx.binders <- saved;
      List.iter
        (fun (g, t, s) ->
          if s <> out_es then
            fault "a MapList body scale %d does not match its declared element scale %d" s out_es;
          emit ctx "lelem(%s,I,%s) :- %s." mapid t (String.concat ", " (base @ g)))
        branches;
      emit ctx "lpres(%s) :- lpres(%s)." mapid inner;
      (mapid, out_es)
  | Filter_list { list; binder; condition; _ } ->
      let inner, inner_es = list_source ctx ~subject ~subject_ot list in
      ctx.fresh <- ctx.fresh + 1;
      let fltid = Printf.sprintf "lf%d(%s)" ctx.fresh inner in
      let saved = ctx.binders in
      ctx.binders <- (norm binder, ([], "V", inner_es)) :: ctx.binders;
      let catoms = filter_condition ctx condition in
      ctx.binders <- saved;
      emit ctx "lelem(%s,I,V) :- %s." fltid
        (String.concat ", " (Printf.sprintf "lelem(%s,I,V)" inner :: catoms));
      emit ctx "lpres(%s) :- lpres(%s)." fltid inner;
      (fltid, inner_es)
  | List_literal _ ->
      fault "a ListLiteral list source is not translated (no corpus case; S19 inventory)"
  | Fold _ -> fault "a Fold is a scalar, not a list source"
  | _ ->
      fault
        "a list source here is an own list-typed attribute, a list parameter, a MapList or a \
         FilterList (no corpus case for more)"

(* a closed lambda/function body → the branches (guard atoms, result term,
   scale) it lowers to: an IfThenElse splits into a positive and a
   complemented branch (S7 builtin complements, no NAF), a Match on a bound
   term into one branch per arm (functor unification in a builtin equality),
   an Invoke inlines the acyclic FunctionDef body, and any other closed term
   is a single branch. The [guard_body] grounds the emitted range fences. *)
and body_branches ctx ~guard_body ~witness (e : expr) : (string list * string * int) list =
  match e with
  | If_then_else { condition; then_; else_; _ } ->
      let catoms, pos, neg = body_condition ctx ~guard_body ~witness condition in
      let arm extra branches =
        List.map (fun (g, t, s) -> (catoms @ (extra :: g), t, s)) branches
      in
      arm pos (body_branches ctx ~guard_body ~witness then_)
      @ arm neg (body_branches ctx ~guard_body ~witness else_)
  | Match { scrutinee; arms; _ } ->
      (* the enumeration variant of the keuzetype family is carried at rule
         level (translate_match, via the S2 symbolic operand) but not inside a
         closed body: a closed body has no subject to read the val atom from,
         and the S2 operand has no closed-body carrier. Fenced loudly here so
         the reason names the shape rather than [sum_operand]'s sum wording
         (S19; no corpus case). *)
      (match Ir_v0.Checks.typ_of_expr scrutinee with
      | Some (Enumeratie _) ->
          fault
            "an enumeration match inside a closed lambda/function body is not translated (the \
             symbolic operand has no closed-body carrier; no corpus case)"
      | _ -> ());
      let sv = sum_operand ctx scrutinee in
      let constructoren = sum_constructors (Ir_v0.Checks.typ_of_expr scrutinee) in
      List.concat_map
        (fun (ctor, binder, body) ->
          let f = ctor_functor ctor in
          let pattern, binding =
            match (ctor_payload_datatype constructoren ctor, binder) with
            | None, None -> (Printf.sprintf "%s = %s" sv f, None)
            | Some d, Some b ->
                let pv = fresh ctx "P" in
                ( Printf.sprintf "%s = %s(%s)" sv f pv,
                  Some (norm b, ([], pv, pow10 (decimals_of_datatype (Some d)))) )
            | Some _, None -> fault "unary match arm %S lacks a binder (WF-063)" ctor
            | None, Some _ -> fault "nullary match arm %S carries a binder (WF-063)" ctor
          in
          let saved = ctx.binders in
          (match binding with Some b -> ctx.binders <- b :: ctx.binders | None -> ());
          let branches =
            body_branches ctx ~guard_body:(guard_body @ [ pattern ]) ~witness body
          in
          ctx.binders <- saved;
          List.map (fun (g, t, s) -> (pattern :: g, t, s)) branches)
        arms
  | Let { binder; body; _ } when not (reads_binder binder body) ->
      (* ch. 10.6 clause 3: an unread binder evaluates nothing, closed body or
         not — no inlined value, no fence *)
      body_branches ctx ~guard_body ~witness body
  | Let { binder; value; body; _ } ->
      (* a Let whose body still branches (an IfThenElse reducer, a Match under a
         Let): the value is inlined by the closed_term Let discipline, the binder
         bound with empty atoms, and the value's binding atoms thread into every
         branch's body. The per-branch barrier of ch. 10.6 has nothing to do
         here — a closed body is leeg-free (ch. 10.1), so a value that binds at
         all binds on every branch. *)
      let va, vt, vs = closed_term ctx ~guard_body ~witness value in
      let saved = ctx.binders in
      ctx.binders <- (norm binder, ([], vt, vs)) :: ctx.binders;
      let branches = body_branches ctx ~guard_body:(guard_body @ va) ~witness body in
      ctx.binders <- saved;
      List.map (fun (g, t, s) -> (va @ g, t, s)) branches
  | Invoke { functie; arguments; _ } ->
      let fd =
        match
          List.find_opt
            (fun (d : function_def) -> norm d.fd_naam = norm functie)
            ctx.doc.function_defs
        with
        | Some d -> d
        | None -> fault "unknown function %S (WF-068)" functie
      in
      if List.length arguments <> List.length fd.fd_parameters then
        fault "function %S invoked with the wrong arity (WF-068)" functie;
      let argbinds =
        List.map2
          (fun (pn, _) arg ->
            let a, t, s = closed_term ctx ~guard_body ~witness arg in
            (a, (norm pn, ([], t, s))))
          fd.fd_parameters arguments
      in
      let arg_atoms = List.concat_map fst argbinds in
      let saved = ctx.binders in
      ctx.binders <- List.map snd argbinds @ ctx.binders;
      let branches =
        body_branches ctx ~guard_body:(guard_body @ arg_atoms) ~witness fd.fd_body
      in
      ctx.binders <- saved;
      List.map (fun (g, t, s) -> (arg_atoms @ g, t, s)) branches
  | Construct { typ; constructor_; argument = Some payload; _ } ->
      (* ch. 10.4/§3.5: a Construct with a computed payload inside a closed
         body — the reduce lowering's Some(...) step. The payload itself may
         still branch (an IfThenElse reducer), so it goes through
         [body_branches]; each branch wraps in the constructor functor. The
         constructor VALUE carries no numeric scale (0); its payload rides the
         declared payload scale. *)
      let constructoren = sum_constructors typ in
      (match ctor_payload_datatype constructoren constructor_ with
      | None -> fault "nullary constructor %S built with an argument (WF-062)" constructor_
      | Some d ->
          let pscale = pow10 (decimals_of_datatype (Some d)) in
          let f = ctor_functor constructor_ in
          List.map
            (fun (g, t, s) ->
              if s <> pscale then
                fault "a Construct payload branch scale %d does not match the declared payload scale %d"
                  s pscale;
              (g, Printf.sprintf "%s(%s)" f t, 0))
            (body_branches ctx ~guard_body ~witness payload))
  | _ ->
      let a, t, s = closed_term ctx ~guard_body ~witness e in
      [ (a, t, s) ]

(* an IfThenElse condition over bound terms → (binding atoms, positive
   builtin, complement builtin). Closed bodies read only bound terms, so the
   comparison is a builtin over harmonised scales and the else-branch is its
   exact mirror — no NAF over derived atoms (the S7 closed-body point). *)
and body_condition ctx ~guard_body ~witness (c : conditie) : string list * string * string =
  match c with
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      let la, lt, ls = closed_term ctx ~guard_body ~witness (req "links" links) in
      let ra, rt, rs = closed_term ctx ~guard_body ~witness (req "rechts" rechts) in
      (* no numeric_ok stands in front of this arm at all, and a Construct is a
         first-class closed-body term (scale 0): mixed, it divided by zero;
         both-symbolic, it silently ordered constructor terms *)
      let ls = numeric_scale ~what:"a closed-body comparison" ls in
      let rs = numeric_scale ~what:"a closed-body comparison" rs in
      let m = max ls rs in
      let sc t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
      let lt = sc lt ls and rt = sc rt rs in
      ( la @ ra,
        Printf.sprintf "%s %s %s" lt (cmp_sym op) rt,
        Printf.sprintf "%s %s %s" lt (cmp_complement op) rt )
  | _ ->
      fault
        "an IfThenElse condition in a closed body is a comparison over bound terms (no corpus \
         case for more)"

(* a closed-body TERM → (binding atoms, term, scale). Operands are bound
   binders (a lambda/function parameter or a match payload), literals,
   document parameters, additive combinations of those, and Constructs — the
   closed-body grammar (WF-067/069). Every emitted arithmetic step carries a
   range fence grounded by [guard_body] and witnessed by [witness] (the
   list-id), so the running accumulator cannot wrap silently past 2^31. *)
and closed_term ctx ~guard_body ~witness (e : expr) : string list * string * int =
  match e with
  | Binder_ref { naam; _ } -> (
      match List.assoc_opt (norm naam) ctx.binders with
      (* a closed-body binder is either recurrence-bound (a lambda/fold parameter
         or a match payload) or bound by a Let INSIDE this body: both carry EMPTY
         atoms — the surrounding lelem/pattern join, or the Let's inlined value,
         grounds the ASP variable. A binder carrying binding atoms was bound by a
         Let OUTSIDE the closed body (a rule-level let_binder helper); dropping
         those atoms here would ground an unsafe rule (gringo crashes), so this
         reading is refused loudly. The parallel verifier card makes the shape
         WF-ill-formed (WF-064); until it merges this fence catches it — a clean
         TRANSLATION FAULT in place of clingo's grounding crash (ref already
         faults cleanly with its own unbound-binder note). *)
      | Some ([], var, scale) -> ([], var, scale)
      | Some (_ :: _, _, _) ->
          fault
            "the closed body reads binder %S bound by a Let OUTSIDE it; this reading is refused (a \
             closed-body read must be self-contained — the parallel verifier card makes it \
             WF-ill-formed)"
            naam
      | None -> fault "unbound binder %S in a closed body (WF-064 should preclude this)" naam)
  | Literal { waarde; typ; _ } -> (
      let lex = req "Literal.waarde" waarde in
      match typ with
      | Some (Numeriek _ | Percentage _) | None ->
          let d = lexeme_decimals lex in
          let v = scaled_of_value ~decimals:d (Value.Num (Value.q_of_lexeme lex, Value.Unitless)) in
          ([], string_of_int v, pow10 d)
      | _ -> fault "a non-numeric literal in a closed-body term position")
  | Parameter_ref { parameter; _ } -> (
      let name = req "ParameterRef.parameter" parameter in
      match Ir_v0.Checks.find_ci ctx.doc.parameters name with
      | Some { par_granulariteit = Some _; _ } ->
          fault "a granular parameter %S in a closed body is refused (ch. 3.3/8)" name
      | Some p ->
          let d = decimals_of_datatype p.par_datatype in
          let v = fresh ctx "P" in
          ([ Printf.sprintf "param(%s,%s)" (str name) v ], v, pow10 d)
      | None -> fault "unknown parameter %S" name)
  | Binair { op = (Plus | Min) as op; links; rechts; _ } ->
      numeric_ok links;
      numeric_ok rechts;
      let bl, tl, sl = closed_term ctx ~guard_body ~witness (req "Binair.links" links) in
      let br, tr, sr = closed_term ctx ~guard_body ~witness (req "Binair.rechts" rechts) in
      (* the numeric_ok pair reads the wire `type` (and passes an unannotated
         binder); the scale is what the harmonisation actually divides by *)
      let sl = numeric_scale ~what:"a closed-body additive join" sl in
      let sr = numeric_scale ~what:"a closed-body additive join" sr in
      let m = max sl sr in
      let sc t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
      let tl' = sc tl sl and tr' = sc tr sr in
      range_guard ctx ~subject:witness
        ~body:(guard_body @ bl @ br)
        [ (tl', 1073741823 / (m / sl)); (tr', 1073741823 / (m / sr)) ];
      (bl @ br, Printf.sprintf "(%s%s%s)" tl' (if op = Plus then "+" else "-") tr', m)
  | Construct { typ; constructor_; argument = (None | Some (Literal _)) as argument; _ } ->
      ([], construct_ground_term ctx typ constructor_ argument, 0)
  | Construct { typ; constructor_; argument = Some arg; _ } -> (
      (* ch. 9.1 in a closed body: a NON-literal payload is itself a
         closed-body term — a document parameter, a bound binder, an additive
         or multiplicative combination of those — so it lowers RECURSIVELY
         through this same function and the constructor wraps the resulting
         term. Scaled to the DECLARED payload datatype; the constructor value's
         own scale is 0 (a sum term is not numeric), the [expr_term] posture.
         The payload's binding atoms travel out with the term, so the caller's
         rule body binds them (the fold base rule is the corpus site). *)
      match ctor_payload_datatype (sum_constructors typ) constructor_ with
      | None -> fault "nullary constructor %S built with an argument (WF-062)" constructor_
      | Some d ->
          let payload_scale = pow10 (decimals_of_datatype (Some d)) in
          let atoms, term, scale = closed_term ctx ~guard_body ~witness arg in
          if scale <> payload_scale then
            fault
              "a Construct payload at scale %d does not match the declared payload scale %d \
               (rescaling a bound sum payload has no corpus case)"
              scale payload_scale;
          (atoms, Printf.sprintf "%s(%s)" (ctor_functor constructor_) term, 0))
  | Field_access { base; veld; _ } ->
      (* ch. 11.1 inside a closed body (the MapList-over-struct-field lelem
         route, λd. d.veld): the base is a bound struct binder; project its
         field positionally by unification over the binder's ASP variable *)
      let bterm =
        match base with
        | Binder_ref { naam; _ } -> (
            match List.assoc_opt (norm naam) ctx.binders with
            | Some (_, var, _) -> var
            | None ->
                fault "unbound binder %S in a closed-body field access (WF-064 should preclude this)"
                  naam)
        | _ -> fault "a closed-body field access reads a bound struct binder (no corpus case for more)"
      in
      let unif, fvar, fscale = project_field ctx ~base ~bterm ~veld in
      ([ unif ], fvar, fscale)
  | Binair { op = (Maal | Percentage_van) as op; links; rechts; _ } ->
      (* the multiplicative family in a closed body (§6.4/§6.8): the scaled
         product at scale sl·sr, both operands bound (a closed-body binder never
         goes leeg, so no coalesce — the additive-family posture of this
         function). Percentage-van is (l/100)·r with the /100 folded into the
         scale (sl·sr·100): no division anywhere, the rule-level fold carried into
         the body context (one encoding). The rule-level product encoding and its
         46340 range fence, grounded by [guard_body] and keyed by [witness]. *)
      numeric_ok links;
      numeric_ok rechts;
      (match op with
      | Percentage_van -> pct_van_left_ok ctx links
      | _ -> unit_free_ok ctx links);
      unit_free_ok ctx rechts;
      let bl, tl, sl = closed_term ctx ~guard_body ~witness (req "Binair.links" links) in
      let br, tr, sr = closed_term ctx ~guard_body ~witness (req "Binair.rechts" rechts) in
      range_guard ctx ~subject:witness ~body:(guard_body @ bl @ br) [ (tl, 46340); (tr, 46340) ];
      (bl @ br, Printf.sprintf "(%s*%s)" tl tr, sl * sr * (if op = Percentage_van then 100 else 1))
  | Afgerond { basis = Some basis; modus = Rekenkundig; decimalen = 2; _ }
    when money_mult_shape basis ->
      (* the money × rate product Catala rounds to the cent (§5/S18(c)) inside a
         closed body — the split encoding, shared with the rule level *)
      closed_money_mult ctx ~guard_body ~witness ~basis
  | Binair { op = Gedeeld_door; links; rechts; _ } ->
      (* a closed-body bare division (§6.5): a compile-time positive constant
         divisor rides the gcd-reduced form (the tutorial's decimal of n /
         2.0000); a RUNTIME divisor (a bound binder — a list element) rides the
         two-factor form with its numeric-zero fence, the empty-operand §9.5
         edges gone because a closed-body binder is never leeg. Divisor-side
         range fence and non-exact viol both in body context. *)
      closed_division ctx ~guard_body ~witness ~rounding:None ~links ~rechts
  | Binair { op = Gedeeld_door_abs; links; rechts; _ } ->
      (* the abs-division variant (ch. 2.2): the quotient truncated toward zero
         at 5 decimals — gringo's integer `/` IS that collapse on both signs, so
         the round_z-scale-5 form carried into the body context needs no
         exactness guard and no negative fence *)
      closed_division ctx ~guard_body ~witness ~rounding:(Some (Richting_nul, 5)) ~links ~rechts
  | Binair { op = Tot_de_macht; links; rechts; _ } ->
      (* tot de macht in a closed body (ch. 2.2): a compile-time non-negative
         integer exponent unfolds to repeated Maal under the SAME 46340
         multiplicative fence — each partial product bounded so the running
         accumulator stays inside 2^31 (conservative — it fences t^(e-1) ≤ 46340,
         tighter than the rule-level base bound — but always loud, never a silent
         wrap). The base propagates leeg by the bare join (ref's pow_ on an empty
         base); a runtime, negative or fractional exponent faults. *)
      numeric_ok links;
      numeric_ok rechts;
      unit_free_ok ctx links;
      unit_free_ok ctx rechts;
      let e = const_pow_exponent (req "Binair.rechts" rechts) in
      let bl, tl, sl = closed_term ctx ~guard_body ~witness (req "Binair.links" links) in
      (* the range test below divides by the base scale; [numeric_ok links] only
         read the wire `type` *)
      let sl = numeric_scale ~what:"a closed-body power base" sl in
      if e = 0 then ([], "1", 1)
      else begin
        let rec go acc_term acc_scale k =
          if k = 0 then (acc_term, acc_scale)
          else begin
            if acc_scale > 1_000_000_000 / sl then
              fault "the closed-body power's result scale %d^%d exceeds clingo's integer range" sl e;
            range_guard ctx ~subject:witness ~body:(guard_body @ bl) [ (acc_term, 46340); (tl, 46340) ];
            go (Printf.sprintf "(%s*%s)" acc_term tl) (acc_scale * sl) (k - 1)
          end
        in
        let term, scale = go tl sl (e - 1) in
        (bl, term, scale)
      end
  | Let { binder; body; _ } when not (reads_binder binder body) ->
      (* ch. 10.6 clause 3: an unread binder evaluates nothing — the value is
         not inlined and its fences are not emitted *)
      closed_term ctx ~guard_body ~witness body
  | Let { binder; value; body; _ } ->
      (* a Let INSIDE a closed body (ch. 10.6, the let_binder discipline carried
         into the body context): the value is itself a closed-body term, so it is
         INLINED — the binder maps to the value's term with EMPTY atoms (the
         recurrence-bound convention, so a nested read is self-contained), and the
         value's binding atoms thread into every read site through the returned
         atom list. No subject-keyed let helper (that is the rule level's shape,
         where a leeg value must break the reading positions); a closed-body
         binder is never leeg (ch. 10.1), so the read-place barrier is vacuous
         here and call-by-need coincides with call-by-value. *)
      let va, vt, vs = closed_term ctx ~guard_body ~witness value in
      let saved = ctx.binders in
      ctx.binders <- (norm binder, ([], vt, vs)) :: ctx.binders;
      let ba, bt, bs = closed_term ctx ~guard_body:(guard_body @ va) ~witness body in
      ctx.binders <- saved;
      (va @ ba, bt, bs)
  | Attribuut_ref _ ->
      (* the fourth refusing place of the S19 inventory, split out of the
         catch-all below so the reason names the SHAPE instead of listing forms
         (a Construct, a Let, a sum) that this position does carry — the read
         that fails is the LEAF, reached through them. A closed body has no
         subject: a lambda/function body reads its binders, a fold init is
         evaluated once beside the list, so there is no S to hang a val atom
         on. The one subject-reading init that IS carried never arrives here —
         it takes the rule-level route of S19 ([expr_term]) when its own SHAPE
         is an attribute read, bare or directly under a Construct, and the fold
         source is subject-keyed. *)
      fault
        "an attribute read inside a closed body has no subject to read from (a lambda or function \
         body and a fold init are subject-free); only a Fold init whose own SHAPE is an attribute \
         read, bare or directly under a Construct, takes the subject-keyed rule-level route"
  | _ ->
      fault
        "a closed-body term here is a bound binder, a literal, a parameter, a +/-/×/percentage-of \
         combination, a division (bare or abs), a power by a constant integer exponent, a Construct \
         or a Let (no corpus case for more)"

(* a sum-typed operand inside a closed body → its ground term: a bound binder
   (a lambda/function parameter or a match payload) resolves to its ASP
   variable, a Construct to its ground constructor term. Both are positive,
   so a comparison over them is builtin functor (in)equality (ch. 10.4). *)
and sum_operand ctx (e : expr) : string =
  match e with
  | Binder_ref { naam; _ } -> (
      match List.assoc_opt (norm naam) ctx.binders with
      | Some (_, var, _) -> var
      | None -> fault "unbound binder %S in a closed body (WF-064 should preclude this)" naam)
  | Construct { typ; constructor_; argument; _ } -> construct_ground_term ctx typ constructor_ argument
  | _ ->
      fault
        "a sum-typed operand in a closed body is a bound binder or a Construct (no corpus case \
         for more)"

(* a FilterList condition → the builtin atoms it adds to the keep rule
   (beside the element's own lelem join). A sum-typed (in)equality is
   POSITIVE functor (in)equality over the bound element and a ground
   Construct — never `not gelijk` (the ch. 10.4/S14 non-complement rule);
   the element is always bound, so Tabel 38's leeg corner cannot arise. *)
and filter_condition ctx (c : conditie) : string list =
  match c with
  | Vergelijking { op = (Gelijk | Ongelijk) as op; links; rechts; periode = None; _ }
    when is_sum_operand links || is_sum_operand rechts ->
      let lt = sum_operand ctx (req "links" links) in
      let rt = sum_operand ctx (req "rechts" rechts) in
      [ Printf.sprintf "%s %s %s" lt (if op = Gelijk then "==" else "!=") rt ]
  | _ ->
      fault
        "a FilterList condition here is a sum-typed (in)equality over the bound element (no \
         corpus case for more)"

(* is [basis] a money × rate product (possibly under ch. 10.6 Lets)? — a
   2-decimal money multiplied by a 4-decimal rate, the [Mult_mon_rat] shape
   the plugin wraps in Afgerond{REKENKUNDIG,2} (§5). The naive scale-6 product
   wraps int31 at tutorial magnitude, so it takes the split encoding. *)
and money_mult_shape (basis : expr) : bool =
  let dec e =
    match Ir_v0.Checks.typ_of_expr e with
    | Some (Numeriek { decimalen; _ } | Percentage { decimalen; _ }) -> Some decimalen
    | _ -> None
  in
  let rec go = function
    | Let { body; _ } -> go body
    | Binair { op = Maal; links = Some m; rechts = Some r; _ } -> dec m = Some 2 && dec r = Some 4
    | _ -> false
  in
  go basis

(* the split money multiplication (S-section (c)): rounding a 2-decimal money
   M times a 4-decimal rate R to the cent. The naive product M·R at scale 10^6
   wraps clingo's machine integers at tutorial magnitude (2000000·2100 > 2^31),
   so the rate is split into two scale-2 halves — Rh = R/100, Rl = R\100 —
   giving M·R/10^4 = M·Rh/10^2 + M·Rl/10^4. The rounded cent is
   C = qA + (100·rA + M·Rl + 5000)/10^4 with A = M·Rh, qA = A/100, rA = A\100:
   demonstrably round(M·R/10^4) half-up for non-negative operands (ref's
   round_z Rekenkundig), each partial product fenced by a viol/2 witness so no
   term wraps silently. Lets around the product are peeled under call-by-need
   (ch. 10.6): an UNREAD binder is dropped unevaluated (clause 3) and a read
   one is bound with its value's atoms riding [rule_guard], so the barrier sits
   at the READ (clause 2) — exactly as ref evaluates the Let body. *)
and translate_money_mult ctx ~subject ~subject_ot ~basis : string list * string * int =
  let saved_b = ctx.binders and saved_g = ctx.rule_guard in
  let rec peel = function
    | Let { binder; body; _ } when not (reads_binder binder body) ->
        (* ch. 10.6 clause 3: an unread binder evaluates nothing *)
        peel body
    | Let { binder; value; body; _ } ->
        let va, vt, vs = expr_term ctx ~subject ~subject_ot value in
        ctx.binders <- (norm binder, let_binder ctx ~subject ~subject_ot (va, vt, vs)) :: ctx.binders;
        (* the peeled value's atoms ride [rule_guard] into the product helper's
           own body (below): a coalescing operand swallows the read's letN atom,
           so this is where the read-place barrier survives the money product *)
        ctx.rule_guard <- ctx.rule_guard @ va;
        peel body
    | Binair { op = Maal; links; rechts; _ } ->
        (req "money multiplication links" links, req "money multiplication rechts" rechts)
    | _ -> fault "money multiplication basis is not a MAAL (money_mult_shape gated this)"
  in
  let m, r = peel basis in
  numeric_ok (Some m);
  numeric_ok (Some r);
  unit_free_ok ctx (Some m);
  unit_free_ok ctx (Some r);
  (* ch. 1.2a: the multiplicative operands coalesce leeg → 0, exactly as ref's
     [Arith.mul] does — a leeg rate makes the product 0, not leeg (the strict
     Let barrier that makes a leeg money leeg lives one level up, in the peeled
     value atoms, never in these direct operands) *)
  let ma, tm, sm = coalesce0 ctx ~subject ~subject_ot (expr_term ctx ~subject ~subject_ot m) in
  let ra, tr, sr = coalesce0 ctx ~subject ~subject_ot (expr_term ctx ~subject ~subject_ot r) in
  if sm <> 100 || sr <> 10000 then
    fault "the split money multiplication expects a 2-decimal money and a 4-decimal rate (%d,%d)" sm
      sr;
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  let gbody = String.concat ", " ((base :: ctx.rule_guard) @ ma @ ra) in
  (* the fences and REKENKUNDIG half-away encoding are shared with the closed
     body (one encoding); here the value rides a subject-keyed helper predicate,
     the operand atoms baked into its body *)
  money_mult_fences ctx ~subject ~gbody ~tm ~tr;
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "mm%d" ctx.fresh in
  emit ctx "%s(%s,C) :- %s, %s." p subject gbody
    (String.concat ", " (money_mult_body ~a:"A" ~c:"C" ~tm ~tr));
  let v = fresh ctx "MM" in
  ctx.binders <- saved_b;
  ctx.rule_guard <- saved_g;
  ([ Printf.sprintf "%s(%s,%s)" p subject v ], v, 100)

(* the money × rate cent product inside a CLOSED body (S15/S18(c)): the same
   split encoding as [translate_money_mult], but the operands are bound binders
   (a closed-body binder never goes leeg, so no coalesce and no Let peeling — a
   closed body carries no Let), and the A/C assignments are INLINE body atoms
   over fresh variables rather than a subject-keyed helper predicate, so the fold
   recurrence that consumes them binds the per-element index directly. The four
   fences ride [witness] and [guard_body] — every fence moves to body context. *)
and closed_money_mult ctx ~guard_body ~witness ~basis : string list * string * int =
  let m, r =
    match basis with
    | Binair { op = Maal; links; rechts; _ } ->
        (req "money multiplication links" links, req "money multiplication rechts" rechts)
    | _ -> fault "a closed-body money multiplication basis is not a bare MAAL"
  in
  numeric_ok (Some m);
  numeric_ok (Some r);
  unit_free_ok ctx (Some m);
  unit_free_ok ctx (Some r);
  let ma, tm, sm = closed_term ctx ~guard_body ~witness m in
  let ra, tr, sr = closed_term ctx ~guard_body ~witness r in
  if sm <> 100 || sr <> 10000 then
    fault "the split money multiplication expects a 2-decimal money and a 4-decimal rate (%d,%d)" sm
      sr;
  let gbody = String.concat ", " (guard_body @ ma @ ra) in
  money_mult_fences ctx ~subject:witness ~gbody ~tm ~tr;
  let a = fresh ctx "A" and c = fresh ctx "C" in
  (ma @ ra @ money_mult_body ~a ~c ~tm ~tr, c, 100)

(* a closed-body division (§6.5/S18), the rule-level [translate_division]
   carried into the body context. A COMPILE-TIME positive constant divisor folds
   the whole constant fraction (sr·out_scale)/(c·sl) by its full gcd; a RUNTIME
   divisor (a bound binder — a list element) keeps the two-factor form and rides
   a numeric-zero fence. The empty-operand edges the rule level carries (empty
   dividend → 0, empty divisor → the §9.5 channel) never arise here — a
   closed-body binder is never leeg (ch. 10.1) — so no coalesce and no
   inconsistent/2 fact, only the divisor-side fences keyed by [witness] and
   grounded by [guard_body]. [rounding] None is the exact bare division (value
   guarded [numt \ dent == 0], a remainder derives a viol/2 and UNSAT — ref
   stays exact-rational, then its bounded write refuses; the differential holds
   where it runs); [Some (Richting_nul, d)] is the abs-division truncation at d
   decimals, which gringo's integer `/` computes on both signs, needing no
   exactness guard and no negative fence. The exactness test runs at the
   precision the fold/write will STORE ([ctx.write_scale] for the bare form, the
   collapse scale for abs), not merely at the operand scales. *)
and closed_division ctx ~guard_body ~witness ~rounding ~links ~rechts : string list * string * int =
  let links = req "Binair.links" links and rechts = req "Binair.rechts" rechts in
  numeric_ok (Some links);
  numeric_ok (Some rechts);
  unit_free_ok ctx (Some links);
  unit_free_ok ctx (Some rechts);
  let bl, tl, sl = closed_term ctx ~guard_body ~witness links in
  let br, tr, sr = closed_term ctx ~guard_body ~witness rechts in
  (* both scales become gcd operands and divisors below — the
     [translate_division] fence, on the closed-body sibling *)
  let sl = numeric_scale ~what:"a closed-body division dividend" sl in
  let sr = numeric_scale ~what:"a closed-body division divisor" sr in
  let out_scale =
    match rounding with None -> max (max sl sr) ctx.write_scale | Some (_, d) -> pow10 d
  in
  let const_div =
    if br = [] then
      match int_of_string_opt (String.trim tr) with Some c when c > 0 -> Some c | _ -> None
    else None
  in
  let numt_of knum = if knum = 1 then Printf.sprintf "(%s)" tl else Printf.sprintf "((%s)*%d)" tl knum in
  let two_factor () =
    let g = gcd (sr * out_scale) sl in
    let knum = sr * out_scale / g and kden = sl / g in
    ( numt_of knum,
      (if kden = 1 then Printf.sprintf "(%s)" tr else Printf.sprintf "((%s)*%d)" tr kden),
      [ (tl, 536870912 / knum); (tr, 536870912 / kden) ] )
  in
  let numt, dent, range_bounds =
    match const_div with
    | Some c ->
        let knum, kden_c = const_div_reduce ~sl ~sr ~out_scale ~c in
        (* a folded denominator past the fence would feed 2·dent unguarded past
           2^31 (S18); drop back to the two-factor form, whose divisor-term
           range_guard fires the loud UNSAT — exactly the rule-level fallback *)
        if kden_c > 536870912 then two_factor ()
        else (numt_of knum, Printf.sprintf "(%d)" kden_c, [ (tl, 536870912 / knum) ])
    | None -> two_factor ()
  in
  (* only a genuine runtime divisor (a bound binder) can be numeric zero; a
     constant divisor is fenced positive, even on the two-factor fallback *)
  let runtime = const_div = None in
  let gbody = guard_body @ bl @ br in
  range_guard ctx ~subject:witness ~body:gbody range_bounds;
  (* a runtime divisor can be numeric zero (never leeg — the §9.5 empty-divisor
     edge cannot arise in a closed body); a constant divisor is fenced positive *)
  if runtime then
    emit ctx "viol(\"division by numeric zero\",%s) :- %s, (%s) == 0." witness
      (String.concat ", " gbody) tr;
  let nonzero = if runtime then [ Printf.sprintf "(%s) != 0" tr ] else [] in
  match rounding with
  | None ->
      emit ctx
        "viol(\"non-exact division (an explicit rounding is mandated before precision is lost)\",%s) \
         :- %s, %s \\ %s != 0."
        witness
        (String.concat ", " (gbody @ nonzero))
        numt dent;
      ( bl @ br @ nonzero @ [ Printf.sprintf "%s \\ %s == 0" numt dent ],
        Printf.sprintf "(%s/%s)" numt dent,
        out_scale )
  | Some (Richting_nul, _) ->
      (bl @ br @ nonzero, Printf.sprintf "(%s/%s)" numt dent, out_scale)
  | Some (modus, _) ->
      ignore modus;
      fault
        "a closed-body rounded division supports only the richting-nul abs-division collapse (no \
         corpus case for another rounding mode)"

(* --- rule forms ---------------------------------------------------------- *)

let head_body ctx head body = emit ctx "%s :- %s." head (String.concat ", " body)

(* one guarded outcome of a rule value: the atoms its rule body joins, the
   result term and its scale, plus [vb_reads] — does THIS branch read the
   named Let binder? Under ch. 10.6 the leeg-barrier sits at the READ, so a
   branch that never reads a binder must not carry that binder's value atoms;
   [vb_reads] is what tells the [Let] arm below which branches to guard. *)
type vbranch = {
  vb_guard : string list;
  vb_term : string;
  vb_scale : int;
  vb_reads : string -> bool;
}

(* the rule VALUE position (§6, guarded consequences): a Let binds an
   intermediate whose value atoms guard the branches that READ it; an
   IfThenElse (now reachable through Let bodies, WF-069) lowers to one guarded
   outcome per branch — the condition a builtin over bound/parameter/literal
   operands (WF-077) and its exact complement, NAF-free, so the leeg barrier
   and clingo's underivability coincide by construction. Any other value is a
   single branch via [expr_term]. Mirrors [body_branches] at value position. *)
let rec value_branches ctx ~subject ~subject_ot (e : expr) : vbranch list =
  match e with
  | Let { binder; body; _ } when not (reads_binder binder body) ->
      (* ch. 10.6 clause 3: an unread binder evaluates nothing — the value is
         not translated, so it deposits no atoms, no fence and no §9.5 channel
         fact, and the body's branches ARE the Let's *)
      value_branches ctx ~subject ~subject_ot body
  | Let { binder; value; body; _ } ->
      let va, vt, vs = expr_term ctx ~subject ~subject_ot value in
      let saved_b = ctx.binders and saved_g = ctx.rule_guard in
      ctx.binders <- (norm binder, let_binder ctx ~subject ~subject_ot (va, vt, vs)) :: ctx.binders;
      ctx.rule_guard <- ctx.rule_guard @ va;
      let branches = value_branches ctx ~subject ~subject_ot body in
      ctx.binders <- saved_b;
      ctx.rule_guard <- saved_g;
      (* ch. 10.6 clause 2: the value atoms guard exactly the READING branches
         — a branch that never reads the binder survives a leeg value, which is
         where L parts company with the old strict lowering. The read set moves
         outward too: an outer binder is read on this branch either directly,
         or because THIS binder is read and its value reads it. *)
      List.map
        (fun b ->
          let read = b.vb_reads binder in
          { b with
            vb_guard = (if read then va else []) @ b.vb_guard;
            vb_reads =
              (fun o ->
                (norm o <> norm binder && b.vb_reads o) || (read && reads_binder o value)) })
        branches
  | If_then_else { condition; then_; else_; _ } ->
      let catoms, pos, neg = value_condition ctx ~subject ~subject_ot condition in
      (* the condition is evaluated on EVERY branch, so a binder it reads is
         read on every branch — the exactness the per-branch guard needs *)
      let arm extra branches =
        List.map
          (fun b ->
            { b with
              vb_guard = catoms @ (extra :: b.vb_guard);
              vb_reads =
                (fun o -> Ir_v0.Checks.mentions_binder_cond o condition || b.vb_reads o) })
          branches
      in
      arm pos (value_branches ctx ~subject ~subject_ot then_)
      @ arm neg (value_branches ctx ~subject ~subject_ot else_)
  | _ ->
      let a, t, s = expr_term ctx ~subject ~subject_ot e in
      [ { vb_guard = a; vb_term = t; vb_scale = s; vb_reads = (fun o -> reads_binder o e) } ]

(* a value-position IfThenElse condition (WF-077): a comparison whose operands
   are bound binders, parameters or literals — never a bare attribute read.
   That is exactly what keeps the else-branch the positive builtin's EXACT
   complement (the operands are total, never leeg), so the two branches need no
   NAF over derived atoms. Returns (binding atoms, positive builtin, complement
   builtin). *)
and value_condition ctx ~subject ~subject_ot (c : conditie) : string list * string * string =
  (* WF-077 admits arithmetic combinators (Binair/Afgerond/Begrensd/Tekst_reeks)
     over binders, parameters and literals as condition operands — the READS are
     what stay total (never leeg), which is what keeps the else-branch the
     positive builtin's EXACT complement. [expr_term] lowers each operand below
     and the two operand scales harmonise there exactly as the closed-body
     comparison route ([body_condition]) does, so only the read SHAPE needs
     gating here; an attribute/field/characteristic read — WF-077-rejected
     before translation — is all this backstop still refuses. Mirrors
     [Ir_v0.Checks.bound_only_expr]. *)
  let rec bound_only = function
    | Binder_ref _ | Parameter_ref _ | Literal _ -> true
    | Binair { links; rechts; _ } -> opt links && opt rechts
    | Afgerond { basis; _ } -> opt basis
    | Begrensd { basis; minimum; maximum; _ } -> opt basis && opt minimum && opt maximum
    | Tekst_reeks { delen; _ } ->
        List.for_all (function Ingevoegd { expr = Some e } -> bound_only e | _ -> true) delen
    | _ -> false
  and opt = function Some e -> bound_only e | None -> true in
  let operand_ok o =
    if not (opt o) then
      fault
        "a value-position IfThenElse condition operand must read only binders, parameters or \
         literals (WF-077)"
  in
  match c with
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      operand_ok links;
      operand_ok rechts;
      let la, lt, ls = expr_term ctx ~subject ~subject_ot (req "Vergelijking.links" links) in
      let ra, rt, rs = expr_term ctx ~subject ~subject_ot (req "Vergelijking.rechts" rechts) in
      (* [operand_ok] admits a bare BinderRef, whose scale is the binder's own —
         0 for a sum/struct binder; the body_condition sibling *)
      let ls = numeric_scale ~what:"a value-position IfThenElse comparison" ls in
      let rs = numeric_scale ~what:"a value-position IfThenElse comparison" rs in
      let m = max ls rs in
      let sc t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
      let lt = sc lt ls and rt = sc rt rs in
      ( la @ ra,
        Printf.sprintf "%s %s %s" lt (cmp_sym op) rt,
        Printf.sprintf "%s %s %s" lt (cmp_complement op) rt )
  | _ ->
      fault
        "a value-position IfThenElse condition is a comparison over bound operands (WF-077; no \
         corpus case for more)"

(* fit a scaled term to the write target's declared scale: a coarser
   value upscales exactly (both scales are powers of ten); a finer one
   downscales under a runtime divisibility guard — ref writes any exact
   rational that fits the target's declared decimals, so a value that
   FITS must land, and a value that does not is fenced by a viol/2
   witness (never silently truncated; ref would write it exactly, a
   recorded loud asymmetry like non-exact division). [body] is the
   write rule's full body, so the fence fires only where the rule
   fires. Returns the fitted term plus guard atoms for the write rule.

   The identity case comes first as BOOKKEEPING, not as semantics: [target] is
   always [pow10 (attr_decimals ...)] and therefore >= 1, and a sum- or
   struct-typed target never reaches here at all (its own write arm runs
   first), so [scale = target = 0] cannot arise. Measured: hoisting the fence
   ABOVE the identity test changes nothing — suite 47/0, gate 6 405/405. The
   fence stays where the division is, which is where it reads. *)
let fitted ctx ~subject ~what ~body term scale target =
  if scale = target then (term, [])
  else
    let scale = numeric_scale ~what:(Printf.sprintf "the write to %S" what) scale in
    if target > scale && target mod scale = 0 then begin
      range_guard ctx ~subject ~body [ (term, max_int31 / (target / scale)) ];
      (Printf.sprintf "((%s)*%d)" term (target / scale), [])
    end
    else if scale > target && scale mod target = 0 then begin
      let k = scale / target in
      emit ctx
        "viol(\"result of %s does not fit its declared decimals\",%s) :- %s, (%s) \\ %d != 0."
        (norm what) subject
        (String.concat ", " body)
        term k;
      (Printf.sprintf "((%s)/%d)" term k, [ Printf.sprintf "(%s) \\ %d == 0" term k ])
    end
    else fault "value scale %d does not align with target scale %d for %S" scale target what

(* ch. 3.7: the pro-rata temporal target. The whole calendar is
   compile-time data (the shared Timeline module, like the clip's wu
   facts): bucket facts carry start, end and day count over the case's
   endpoint span; the condition's true days accumulate per bucket from
   the ktl facts by four-case interval intersection; the head derives
   one tl/5 segment per bucket with true days, value = base(bucket
   start) × days/bucket-days on the target scale — exact only, a
   remainder derives a viol/2 witness (ref computes the exact rational;
   v0 approximates a non-terminating fraction in DECIMAL64 — the
   recorded ch. 3.7 delta), fenced like every division. *)
(* calendar as data: buckets over the case's endpoint span (shared by
   the ch. 3.7 pro-rata fold and the ch. 3.8 day count) *)
let emit_bucket_calendar ctx ~p ~(g : granulariteit) =
  let endpoints =
    Eval.SMap.fold
      (fun _ (i : Uchar_ref.World.instance) acc ->
        let tl_pts =
          Eval.SMap.fold
            (fun _ (tl : Uchar_ref.Timeline.t) acc ->
              List.concat_map
                (fun (s : Uchar_ref.Timeline.segment) -> [ s.s_van; s.s_tot ])
                tl
              @ acc)
            i.tls acc
        in
        Eval.SMap.fold
          (fun _ periods acc -> List.concat_map (fun (v, t) -> [ v; t ]) periods @ acc)
          i.ctls tl_pts)
      ctx.case.world.instances []
    |> List.sort_uniq compare
  in
  match endpoints with
  | [] -> ()
  | lo :: _ ->
      let hi = List.fold_left max lo endpoints in
      let rec walk d n =
        if d < hi then begin
          if n > 46340 then fault "the bucket endpoint span exceeds the guarded range";
          let bs, bn = Uchar_ref.Timeline.bucket g d in
          emit ctx "%s_b(%d,%d,%d)." p bs bn (bn - bs);
          walk bn (n + 1)
        end
      in
      let first, _ = Uchar_ref.Timeline.bucket g lo in
      walk first 0

(* true days per (bucket × condition segment): four-case intersection
   over the gedurende characteristic's timeline (shared likewise) *)
let emit_true_days ctx ~p ~k =
  List.iter
    (fun (vcmp, vpick, tcmp, tpick) ->
      emit ctx "%s_d(S,B,KV,KT,%s-%s) :- ktl(S,%s,KV,KT), %s_b(B,N,_), %s, %s, %s < %s." p tpick
        vpick (str k) p vcmp tcmp vpick tpick)
    [ ("KV >= B", "KV", "KT <= N", "KT");
      ("KV >= B", "KV", "KT > N", "N");
      ("KV < B", "B", "KT <= N", "KT");
      ("KV < B", "B", "KT > N", "N") ]

let translate_pro_rata ctx ~subject ~ot ~a ~cond ~b ~k ~(g : granulariteit) =
  let base = Printf.sprintf "is_a(%s,%s)" subject (str ot) in
  if not (attr_temporal ctx ~otype:ot ~attr:b) then
    fault "pro-rata base %S is not a temporal attribute (WF-056)" b;
  if attr_unit ctx ~otype:ot ~attr:b <> None || attr_unit ctx ~otype:ot ~attr:a <> None then
    (* ch. 3.7: a rate base restates by its exact factor first — not
       executed; refuse rather than prorate factor-free *)
    fault "pro-rata over a unit-carrying base or target is not translated (no corpus case)";
  if
    not
      (List.exists
         (fun (n, _) -> norm n = norm k)
         (objecttype ctx ot).kenmerk_granulariteit)
  then
    (* mirror ref's fault — a non-temporal characteristic must never
       silently prorate to an empty timeline (a hunted gap) *)
    fault "gedurende over %S, which is not a temporal characteristic (WF-056)" k;
  (match (Ir_v0.Checks.find_ci (objecttype ctx ot).attribuut_granulariteit a,
          Ir_v0.Checks.find_ci (objecttype ctx ot).attribuut_granulariteit b) with
  | Some (Some tg), Some (Some bg) ->
      if tg <> g then
        fault "pro-rata bucket granularity differs from the target's declared granularity (WF-056)";
      if bg < g then fault "pro-rata base granularity is finer than the bucket (WF-056)"
  | _ -> fault "pro-rata target and base carry declared granularities (WF-056)");
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "pr%d" ctx.fresh in
  emit_bucket_calendar ctx ~p ~g;
  emit_true_days ctx ~p ~k;
  let ta = pow10 (attr_decimals ctx ~otype:ot ~attr:a) in
  let tb = pow10 (attr_decimals ctx ~otype:ot ~attr:b) in
  let gg = gcd ta tb in
  let kn = ta / gg and kd = tb / gg in
  let numt = if kn = 1 then "(W*TD)" else Printf.sprintf "(W*TD*%d)" kn in
  let dent = if kd = 1 then "Tot" else Printf.sprintf "(Tot*%d)" kd in
  let body =
    (base :: ctx.rule_guard)
    @ cond
    @ [ Printf.sprintf "%s_b(B,N,Tot)" p;
        Printf.sprintf "TD = #sum { D,KV,KT : %s_d(S,B,KV,KT,D) }" p;
        "TD > 0";
        Printf.sprintf "tl(S,%s,BV,BT,W), BV <= B, B < BT" (str b) ]
  in
  (* the wrap fence: |W×TD×kn| ≤ 2^31 with TD ≤ 366 per bucket, and the
     #sum's contributors (D ≤ 366) count-bounded like every sum *)
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",S) :- %s, |W| > %d."
    (String.concat ", " body)
    (max_int31 / (366 * kn));
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",S) :- %s, \
     46340 < #count { KV,KT : %s_d(S,B,KV,KT,_) }."
    (String.concat ", " ((base :: ctx.rule_guard) @ cond @ [ Printf.sprintf "%s_b(B,_,_)" p ]))
    p;
  emit ctx
    "viol(\"non-exact pro-rata part (an explicit rounding is mandated before precision is \
     lost)\",S) :- %s, %s \\ %s != 0."
    (String.concat ", " body)
    numt dent;
  head_body ctx
    (Printf.sprintf "tl(S,%s,B,N,%s/%s)" (str a) numt dent)
    (body @ [ Printf.sprintf "%s \\ %s == 0" numt dent ]);
  (* ch. 7.8, the timeline arm: on a standing temporal target the fold
     decides only when it derives at least one bucket — the case's
     input timeline (entered as inputtl/5) stands whole by NAF
     otherwise, matching ref's no-write on an empty result *)
  if List.mem (norm ot, norm a) (Ir_v0.Desugar.standing_temporal_targets ctx.doc) then begin
    let eff = Printf.sprintf "%s_eff(S)" p in
    head_body ctx eff (body @ [ Printf.sprintf "%s \\ %s == 0" numt dent ]);
    head_body ctx
      (Printf.sprintf "tl(S,%s,Van,Tot,V)" (str a))
      [ Printf.sprintf "inputtl(S,%s,Van,Tot,V)" (str a); base; "not " ^ eff ]
  end

(* ch. 3.8 "aantal dagen in (de maand / het jaar)": per touched bucket
   one segment carrying the true-day count — the pro-rata numerator
   without the base join. WF-058 mirrors WF-056's facets. *)
let translate_aantal_dagen ctx ~subject ~ot ~a ~cond ~k ~(g : granulariteit) =
  let base = Printf.sprintf "is_a(%s,%s)" subject (str ot) in
  if attr_unit ctx ~otype:ot ~attr:a <> None then
    fault "day-count into a unit-carrying target is not translated (no corpus case)";
  if
    not
      (List.exists
         (fun (n, _) -> norm n = norm k)
         (objecttype ctx ot).kenmerk_granulariteit)
  then fault "gedurende over %S, which is not a temporal characteristic (WF-058)" k;
  (match Ir_v0.Checks.find_ci (objecttype ctx ot).attribuut_granulariteit a with
  | Some (Some tg) ->
      if tg <> g then
        fault "day-count bucket granularity differs from the target's declared granularity (WF-058)"
  | _ -> fault "day-count target carries a declared granularity (WF-058)");
  let ta = pow10 (attr_decimals ctx ~otype:ot ~attr:a) in
  if ta > max_int31 / 366 then
    fault "day-count target decimals exceed the guarded machine-integer range";
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "adg%d" ctx.fresh in
  emit_bucket_calendar ctx ~p ~g;
  emit_true_days ctx ~p ~k;
  let vterm = if ta = 1 then "TD" else Printf.sprintf "(TD*%d)" ta in
  let body =
    (base :: ctx.rule_guard)
    @ cond
    @ [ Printf.sprintf "%s_b(B,N,_)" p;
        Printf.sprintf "TD = #sum { D,KV,KT : %s_d(S,B,KV,KT,D) }" p;
        "TD > 0" ]
  in
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",S) :- %s, \
     46340 < #count { KV,KT : %s_d(S,B,KV,KT,_) }."
    (String.concat ", " ((base :: ctx.rule_guard) @ cond @ [ Printf.sprintf "%s_b(B,_,_)" p ]))
    p;
  head_body ctx (Printf.sprintf "tl(S,%s,B,N,%s)" (str a) vterm) body;
  (* the ch. 7.8 timeline arm, as for pro-rata *)
  if List.mem (norm ot, norm a) (Ir_v0.Desugar.standing_temporal_targets ctx.doc) then begin
    let eff = Printf.sprintf "%s_eff(S)" p in
    head_body ctx eff body;
    head_body ctx
      (Printf.sprintf "tl(S,%s,Van,Tot,V)" (str a))
      [ Printf.sprintf "inputtl(S,%s,Van,Tot,V)" (str a); base; "not " ^ eff ]
  end

let translate_regel ctx (r : resolved_regel) =
  (* the ch. 8.2 validity gate: an excluded rule does not apply — nothing
     is emitted, so its target's case input stands untouched as the plain
     val/3 fact. No inputval machinery waits on it: a windowed rule is in
     neither the ch. 7.7 desugaring domain nor the ch. 7.8 standing domain
     (both demand version-free forms), so the defeasible-input entry never
     keys on a windowed rule's target. An in-window rule translates as any
     version-free rule — including the ch. 6d fence when the case supplies
     its (never-standing) computed target. *)
  if applies ctx r.rr_geldigheid then begin
      let doel = req "doel" r.doel in
      ctx.rule_name <- Option.value r.rr_naam ~default:"?";
      (match doel with
      | Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ } ->
          let subject = "S" in
          let base = Printf.sprintf "is_a(%s,%s)" subject (str ot) in
          let cond =
            match r.rr_voorwaarde with
            | Some c -> cond_atoms ctx ~subject ~subject_ot:ot c
            | None -> []
          in
          (match r.rr_soort with
          | Some Kenmerktoekenning ->
              (match r.waarde with
              | Some (Literal { waarde = Some "waar"; _ }) -> ()
              | _ -> fault "characteristic attribution with a non-waar value");
              head_body ctx (Printf.sprintf "heeft(%s,%s)" subject (str a)) (base :: cond)
          | Some (Berekening | Gelijkstelling) when attr_sum_typed ctx ~otype:ot ~attr:a ->
              (* ch. 9.1: a Construct write to a sum-typed target. A sum value
                 carries no numeric scale, no unit and no fit — the constructor
                 term rides straight into the val head. A leeg argument binds no
                 atom, so the rule does not fire and the target is leeg
                 (propagation). A case-supplied sum computed target has no
                 monotone encoding here (the standing/inputval path is
                 numeric-shaped; no corpus case), so it is fenced loudly. *)
              if case_supplies ctx ~otype:ot ~attr:a then
                fault
                  "a case-supplied sum-typed computed target %S has no encoding here (no corpus \
                   case)"
                  a;
              ctx.rule_guard <- cond;
              let bindings, term, _ = expr_term ctx ~subject ~subject_ot:ot (req "waarde" r.waarde) in
              ctx.rule_guard <- [];
              head_body ctx (Printf.sprintf "val(%s,%s,%s)" subject (str a) term)
                ((base :: cond) @ bindings)
          | Some (Berekening | Gelijkstelling) when attr_struct_typed ctx ~otype:ot ~attr:a ->
              (* ch. 11.1: a StructLiteral write to a struct-typed target. The
                 record term rides straight into the val head — no numeric scale,
                 no unit, no fit (value_scale 0), the Construct-write sibling. A
                 leeg field leaves the literal underivable, so the target is leeg
                 (no-hole propagation). A case-supplied struct computed target has
                 no monotone encoding here (the sum-target sibling), fenced. *)
              if case_supplies ctx ~otype:ot ~attr:a then
                fault
                  "a case-supplied struct-typed computed target %S has no encoding here (no corpus \
                   case)"
                  a;
              ctx.rule_guard <- cond;
              let bindings, term, _ = expr_term ctx ~subject ~subject_ot:ot (req "waarde" r.waarde) in
              ctx.rule_guard <- [];
              head_body ctx (Printf.sprintf "val(%s,%s,%s)" subject (str a) term)
                ((base :: cond) @ bindings)
          | Some (Berekening | Gelijkstelling) when attr_datetime_typed ctx ~otype:ot ~attr:a ->
              (* ch. 3.8 the millisecond layer: a datum-tijd computed target
                 (datetime ± duration) rides the pair term dt(D, MS) straight
                 into the val head — no numeric scale, no unit, no fit (the
                 sum/struct-target sibling). A leeg date operand leaves the
                 pair underivable, so the target is leeg (propagation). A
                 case-supplied datum-tijd computed target has no monotone
                 encoding here (the sum-target sibling), fenced loudly. *)
              if case_supplies ctx ~otype:ot ~attr:a then
                fault
                  "a case-supplied datum-tijd computed target %S has no encoding here (no corpus \
                   case)"
                  a;
              ctx.rule_guard <- cond;
              let atoms, dd, dm = dt_term ctx ~subject ~subject_ot:ot (req "waarde" r.waarde) in
              ctx.rule_guard <- [];
              head_body ctx (Printf.sprintf "val(%s,%s,dt(%s,%s))" subject (str a) dd dm)
                ((base :: cond) @ atoms)
          | Some (Berekening | Gelijkstelling)
            when (match attr_symbolic_datatype ctx ~otype:ot ~attr:a with
                 | Some (Tekst | Enumeratie _) -> true
                 | _ -> false)
                 && not (attr_temporal ctx ~otype:ot ~attr:a) ->
              (* ch. 1.2d scalars in write position: a text/enum ground term
                 rides straight into the val head — no numeric scale, no unit,
                 no fit (the sum/struct-target pattern). A leeg source read
                 binds no atom, so the rule does not fire and the target is
                 leeg (propagation). A case-supplied text/enum computed target
                 has no monotone encoding here (the sum-target sibling),
                 fenced loudly. *)
              if case_supplies ctx ~otype:ot ~attr:a then
                fault
                  "a case-supplied text/enum computed target %S has no encoding here (no corpus \
                   case)"
                  a;
              ctx.rule_guard <- cond;
              let bindings, term = symbolic_term ctx ~subject ~subject_ot:ot (req "waarde" r.waarde) in
              ctx.rule_guard <- [];
              head_body ctx (Printf.sprintf "val(%s,%s,%s)" subject (str a) term)
                ((base :: cond) @ bindings)
          | Some (Berekening | Gelijkstelling)
            when attr_symbolic_datatype ctx ~otype:ot ~attr:a = Some Booleaan ->
              (* a boolean is a characteristic in v0: rule forms READ it
                 closed-world through heeft/2 (bool_atoms, the WF-027 guard),
                 which a derived val/3 would not feed — the two encodings
                 would split. No corpus case computes a boolean attribute;
                 refuse loudly rather than encode half of it. *)
              fault
                "a boolean computed target %S has no encoding here (the closed-world heeft/2 \
                 read and a derived val/3 would split; no corpus case)"
                a
          | Some (Berekening | Gelijkstelling) when attr_temporal ctx ~otype:ot ~attr:a -> (
              if
                (not (List.mem (norm ot, norm a) (Ir_v0.Desugar.standing_temporal_targets ctx.doc)))
                && case_supplies ctx ~otype:ot ~attr:a
              then
                fault
                  "the case supplies input for computed target %S: ref (like v0) overwrites it, \
                   which has no monotone encoding here (no corpus case)"
                  a;
              match r.waarde with
              | Some
                  (Functie
                     { kind = Tijdsevenredig_deel;
                       argumenten =
                         [ Attribuut_ref
                             { attribuut = Some b; objecttype = Some bot; navigatie_rol = None;
                               navigatie_pad = []; set = false; _ } ];
                       filter =
                         Some
                           (Kenmerk_check
                              { kenmerk = Some k; navigatie = None; negatie = false; _ });
                       granulariteit = Some g; leegwaarde = None; dimensie_selectie = None; _ })
                when norm bot = norm ot ->
                  translate_pro_rata ctx ~subject ~ot ~a ~cond ~b ~k ~g
              | Some
                  (Functie
                     { kind = Aantal_dagen_in; argumenten = [];
                       filter =
                         Some
                           (Kenmerk_check
                              { kenmerk = Some k; navigatie = None; negatie = false; _ });
                       granulariteit = Some g; leegwaarde = None; dimensie_selectie = None; _ }) ->
                  translate_aantal_dagen ctx ~subject ~ot ~a ~cond ~k ~g
              | _ ->
                  fault
                    "a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count \
                     shape (a gedurende condition over an own temporal characteristic); no \
                     corpus case for more")
          | Some (Berekening | Gelijkstelling) ->
              let standing =
                List.mem (norm ot, norm a) (Ir_v0.Desugar.standing_targets ctx.doc)
              in
              if (not standing) && case_supplies ctx ~otype:ot ~attr:a then
                fault
                  "the case supplies input for computed target %S: ref (like v0) overwrites it, \
                   which has no monotone encoding here (no corpus case)"
                  a;
              let target_scale = pow10 (attr_decimals ctx ~otype:ot ~attr:a) in
              units_identical_ok
                ~what:(Printf.sprintf "the write to %S" a)
                (attr_unit ctx ~otype:ot ~attr:a)
                (Option.bind r.waarde (eff_unit ctx));
              ctx.rule_guard <- cond;
              ctx.write_scale <- target_scale;
              (* the value lowers to one or more guarded branches (§6): a
                 non-branching value is one branch, identical to the single-head
                 emission; a Let-nested IfThenElse is one val head per branch,
                 the shared Let value atoms in each branch's body *)
              let branches = value_branches ctx ~subject ~subject_ot:ot (req "waarde" r.waarde) in
              ctx.rule_guard <- [];
              ctx.write_scale <- 1;
              (* ch. 7.8 standing target: the rule decides like a default. The
                 effectiveness atom carries the SAME body as each val head —
                 condition and value bindings — so a leeg evaluation derives
                 neither, and the case input (entered as inputval/3) stands by
                 negation-as-failure. With a Let-nested IfThenElse the value is
                 several mutually-exclusive branches; the rule decides if ANY
                 branch fires, so eff is emitted per branch and the input-stands
                 rule once. *)
              let eff =
                if standing then begin
                  let sidx =
                    let rec find i = function
                      | [] -> fault "standing target %S not in its own domain" a
                      | k :: tl -> if k = (norm ot, norm a) then i else find (i + 1) tl
                    in
                    find 0 (Ir_v0.Desugar.standing_targets ctx.doc)
                  in
                  Some (Printf.sprintf "stand%d_eff(%s)" sidx subject)
                end
                else None
              in
              List.iter
                (fun { vb_guard = bindings; vb_term = term; vb_scale = scale; _ } ->
                  let body = (base :: cond) @ bindings in
                  let term, fit_guards = fitted ctx ~subject ~what:a ~body term scale target_scale in
                  head_body ctx
                    (Printf.sprintf "val(%s,%s,%s)" subject (str a) term)
                    (body @ fit_guards);
                  match eff with Some e -> head_body ctx e (body @ fit_guards) | None -> ())
                branches;
              (match eff with
              | Some e ->
                  head_body ctx
                    (Printf.sprintf "val(%s,%s,V)" subject (str a))
                    [ Printf.sprintf "inputval(%s,%s,V)" subject (str a); base; "not " ^ e ]
              | None -> ())
          | _ -> fault "rule kind not translated")
      | _ -> fault "unsupported rule target")
  end

(* a boolean-typed characteristic-initialisation EXPRESSION → the body atoms
   that hold exactly when it is TRUE (WF-027 relaxation, besluit 1). A boolean
   is a characteristic in v0 (booleans are characteristics), so an own-subject
   boolean-attribute read is [heeft(S,a)] — present iff true, absent iff false
   (closed-world, ref's reading). The boolean constant forms stay the v0 shape,
   handled at the call site; this covers the expression form the tutorial's
   navigated subscope input needs. *)
let bool_atoms ctx ~subject ~subject_ot (e : expr) : string list =
  ignore ctx;
  match e with
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; navigatie_rol = None; navigatie_pad = [];
        navigatie_bindingen = None; set = false; _ }
    when norm ot = norm subject_ot ->
      [ Printf.sprintf "heeft(%s,%s)" subject (str a) ]
  | _ ->
      fault
        "a characteristic-initialisation expression here is an own-subject boolean attribute read \
         (no corpus case for more)"

(* object creation (IR-SPEC ch. 3.3; RUNTIME-SEMANTICS ch. 5.5): the
   created instance is a deterministic function term ocN(Subject) — set
   semantics carries the per-(rule × subject) idempotence, exactly
   ref/'s deterministic label. Member initialisations become val/heeft
   heads on the term; the back-link relation (the explicit
   verbindendeRol binding, else the uniquely-derivable pair) lands both
   rol/3 ends. The 1:1 shape's linked-instance guard reads the INPUT
   links only (inputrol/3, the case facts) — the same input-snapshot
   narrowing ref/ records for its guard. A creation that is a member of
   a RECURSIVE rule group grounds along a generation depth (depth/2,
   bounded by oc_budget = 100, the same bound as ref/'s pass budget —
   ref counts passes, this side generations per chain; one chain
   advances one generation per pass, so the two accounts align):
   exhausting the budget derives a viol/2 witness — UNSAT where ref/'s
   pass budget faults (ch. 5.5's bounded budget, never a silent
   truncation; the bound also stays under clingo's own term-depth
   abort, recorded in the README). *)
let translate_object_creatie ctx i (oc : resolved_object_creatie) =
  (* the ch. 8.2 validity gate: an excluded creation does not apply —
     nothing is minted, no back-link lands, no guard fires (ref skips the
     whole form). The index [i] stays the iteration index, so the other
     creations' oc%d terms are unchanged by a skipped neighbour. *)
  if applies ctx oc.oc_geldigheid then begin
  ctx.rule_name <- Option.value oc.oc_naam ~default:"?";
  let nieuw = req "ObjectCreatie.nieuwType" oc.nieuw_type in
  let subject_ot = req "ObjectCreatie.subject" oc.oc_subject in
  let subject = "S" in
  let term = Printf.sprintf "oc%d(%s)" i subject in
  let env =
    { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty }
  in
  let pair =
    match oc.oc_verbindende_rol with
    | Some rb -> (
        match Ir_v0.Checks.resolve_binding { doc = ctx.doc } rb with
        | Ok (_, bound, other) -> Some (rb.rb_feittype, bound, other)
        | Error e -> fault "%s" e)
    | None -> Eval.connecting_pair env ~from_type:subject_ot ~to_type:nieuw
  in
  let cond =
    match oc.oc_voorwaarde with
    | Some c -> cond_atoms ctx ~subject ~subject_ot c
    | None -> []
  in
  let guard =
    match pair with
    | Some (_, new_role, _) when not new_role.rol_meervoudig ->
        [ Printf.sprintf "not inputrol(%s,%s,_)" subject (str (role_canonical new_role)) ]
    | _ -> []
  in
  let recursive =
    let name = norm (Ir_v0.Checks.strip_version (Option.value oc.oc_naam ~default:"")) in
    List.exists
      (fun (g : resolved_regel_groep) ->
        g.recursief
        && List.exists
             (function
               | Some l -> norm (Ir_v0.Checks.strip_version l) = name
               | None -> false)
             g.leden)
      ctx.doc.regel_groepen
  in
  let depth_guard =
    if recursive then [ Printf.sprintf "depth(%s,D)" subject; "D < oc_budget" ] else []
  in
  let body =
    (Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) :: cond) @ guard @ depth_guard
  in
  (if recursive then head_body ctx (Printf.sprintf "depth(%s,D+1)" term) body
   else if has_recursive_creation ctx.doc then
     (* a non-recursive mint still carries a depth in a document with
        recursive chains, so a chain seeded from it stays bounded *)
     head_body ctx
       (Printf.sprintf "depth(%s,D+1)" term)
       (body @ [ Printf.sprintf "depth(%s,D)" subject ]));
  head_body ctx (Printf.sprintf "is_a(%s,%s)" term (str nieuw)) body;
  List.iter
    (fun (t : toekenning) ->
      let m = req "Toekenning.attribuut" t.toe_attribuut in
      if t.toe_kenmerk then (
        match t.toe_waarde with
        | Some (Literal { waarde = Some "waar"; _ }) ->
            head_body ctx (Printf.sprintf "heeft(%s,%s)" term (str m)) body
        | Some (Literal { waarde = Some "onwaar"; _ }) -> ()
        | Some e ->
            (* WF-027 relaxation (besluit 1): a boolean-typed EXPRESSION guards
               the deposit — evaluated on the creation SUBJECT (the same
               caller-context as an attribute Toekenning, ref's closed-world
               reading): true → the mark is set on the minted instance, false →
               the guard fails and the mark stays unset (an absent init). The two
               boolean-constant arms above are the v0 form, byte-for-byte. *)
            let batoms = bool_atoms ctx ~subject ~subject_ot e in
            head_body ctx (Printf.sprintf "heeft(%s,%s)" term (str m)) (body @ batoms)
        | None -> fault "characteristic initialisation of %S without a value (WF-027)" m)
      else if attr_sum_typed ctx ~otype:nieuw ~attr:m || attr_struct_typed ctx ~otype:nieuw ~attr:m
      then begin
        (* a sum-/struct-typed initialisation (the tutorial passes an [Individual]
           struct as a subscope input): the record/constructor term carries no
           numeric scale, no unit and no fit — it rides straight into the val head,
           the translate_regel struct/sum-target sibling (ch. 9.1/11.1). *)
        ctx.rule_guard <- cond @ guard @ depth_guard;
        let bindings, tm, _ = expr_term ctx ~subject ~subject_ot (req "Toekenning.waarde" t.toe_waarde) in
        ctx.rule_guard <- [];
        head_body ctx (Printf.sprintf "val(%s,%s,%s)" term (str m) tm) (body @ bindings)
      end
      else begin
        let target_scale = pow10 (attr_decimals ctx ~otype:nieuw ~attr:m) in
        units_identical_ok
          ~what:(Printf.sprintf "the initialisation of %S" m)
          (attr_unit ctx ~otype:nieuw ~attr:m)
          (Option.bind t.toe_waarde (eff_unit ctx));
        ctx.rule_guard <- cond @ guard @ depth_guard;
        ctx.write_scale <- target_scale;
        let bindings, tm, scale =
          expr_term ctx ~subject ~subject_ot (req "Toekenning.waarde" t.toe_waarde)
        in
        ctx.rule_guard <- [];
        ctx.write_scale <- 1;
        let wbody = body @ bindings in
        let tm, fit_guards = fitted ctx ~subject ~what:m ~body:wbody tm scale target_scale in
        head_body ctx (Printf.sprintf "val(%s,%s,%s)" term (str m) tm) (wbody @ fit_guards)
      end)
    oc.toekenningen;
  (match pair with
  | None -> ()
  | Some (_, new_role, subj_role) ->
      head_body ctx
        (Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical new_role)) term)
        body;
      head_body ctx
        (Printf.sprintf "rol(%s,%s,%s)" term (str (role_canonical subj_role)) subject)
        body)
  end

(* fact creation (IR-SPEC ch. 3.4; RUNTIME-SEMANTICS ch. 5.5): the join
   chain becomes a body join over rol/3, and the head derives the
   created relation's rol/3 facts — both ends, exactly the two
   directions ref/'s writer lands; set semantics carries the
   idempotence (a target reached along two paths is one atom). Only the
   symbolic extension form executes; a bean-field-only v0 record faults
   (as in ref/). *)
let translate_feit_creatie ctx (fc : resolved_feit_creatie) =
  (* the ch. 8.2 validity gate: an excluded fact creation does not apply —
     no rol/3 lands and its to-one guards stay unemitted (ref adds no
     link, so no second-filler fault can arise from this form) *)
  if applies ctx fc.fc_geldigheid then begin
  let name = Option.value fc.fc_naam ~default:"<feitcreatie>" in
  let ftn =
    match fc.fc_feittype with
    | Some f -> f
    | None ->
        fault
          "fact creation %S carries only bean fields; the symbolic form (IR-SPEC ch. 3.4) is \
           what executes"
          name
  in
  let env =
    { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty }
  in
  let role rname = Eval.bound_role env { rb_feittype = ftn; rb_rol = rname } in
  let anker_rol = role (req "ankerRol" fc.anker_rol) in
  let doel_rol = role (req "doelRol" fc.doel_rol) in
  let anker = req "FeitCreatie.anker" fc.anker in
  let subject = "S" in
  let cond =
    match fc.fc_voorwaarde with
    | Some c -> cond_atoms ctx ~subject ~subject_ot:anker c
    | None -> []
  in
  let hop (atoms, cur) rb =
    let v = fresh ctx "H" in
    ( atoms
      @ [ Printf.sprintf "rol(%s,%s,%s)" cur (str (role_canonical (Eval.bound_role env rb))) v ],
      v )
  in
  let mids =
    List.fold_left
      (fun acc (t : tussenstap) -> hop acc (req "Tussenstap.rolBinding" t.ts_rol))
      ([], subject) fc.tussenstappen
  in
  let chain, target = hop mids (req "joinRol" fc.join_rol) in
  let body = (Printf.sprintf "is_a(%s,%s)" subject (str anker) :: cond) @ chain in
  head_body ctx (Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical doel_rol)) target) body;
  head_body ctx (Printf.sprintf "rol(%s,%s,%s)" target (str (role_canonical anker_rol)) subject) body;
  (* a second distinct filler on a to-one end faults in ref/
     (World.add_link, ch. 5.5); a viol/2 witness plus the global
     integrity constraint makes clingo fail identically (UNSAT) instead
     of silently holding both atoms — and the runner's UNSAT diagnosis
     re-run can name WHICH guard fired *)
  let to_one_guard (r : rol) holder_type =
    if not r.rol_meervoudig then
      emit ctx "viol(\"to-one second filler on role %s\",X) :- is_a(X,%s), 2 <= #count { Y : rol(X,%s,Y) }."
        (norm (role_canonical r)) (str holder_type)
        (str (role_canonical r))
  in
  to_one_guard doel_rol anker;
  to_one_guard anker_rol (req "FeitCreatie.doel" fc.fc_doel)
  end

(* the defaults group (RUNTIME-SEMANTICS ch. 7.1/7.2/7.9, IR-SPEC ch. 6.1;
   ASP-TRANSLATION S17): the ch. 7.5 decides_m lowering generalised into a
   recursive rose tree (ch. 7.9). A CHAIN — a default list, a top-level member
   or an exception group — lowers to an outcome predicate `T_out(S,V)` (the
   highest settling default's decision: the 0/1 besluit on characteristic
   defaults, the scaled consequence term on attribute defaults) and a conflict
   flag `T_cfl(S)` (the highest settling default's exception-group conflict,
   propagated upward). A GROUP — a default's excepties, or the root
   member set —
   reconciles its member chains exactly as ch. 7.2 reconciles the root:
   differing decisions derive a path-keyed inconsistent/2 and suppress the
   clean decision, and a member conflict propagates before differing
   decisions are read (conflict-first). A default SETTLES the walk when it
   decides or conflicts; higher settling defaults displace lower ones by
   `not T_stl_m` — the flat encoding's displacement, now carrying conflicts
   too (a higher deciding default displaces a lower default's exception-group
   conflict, vraag 5-A). A differing exception group therefore propagates one
   conflict atom to the root, which suppresses EVERY write on the target (the
   `not grpG_conflict` guard) and leaves the case input standing, while its
   own group deposits the sole §9.5 fact under its default-index path key.
   Every negation points strictly down the tree or at strictly-higher defaults
   (disjoint subtrees) — no conflict atom depends on the write it guards,
   so the program stays stratified and clingo keeps its one stable model.
   Sound NAF because WF-051 folds every descendant read into a negative
   edge toward the root target (the verifier remains the clingo-suitability
   guard). *)
let path_key target_form = function
  | [] -> target_form
  | path -> target_form ^ "/" ^ String.concat "/" (List.map string_of_int path)

let rec translate_chain ctx ~is_char ~subject ~subject_ot ~base ~path ~root
    ~target_form ~target_scale ~has_input ~outer_displaced (defaults : default list) :
    string * string =
  let tag = fresh ctx "nl" in
  let out = Printf.sprintf "%s_out" tag in
  let cfl = Printf.sprintf "%s_cfl" tag in
  (* a flat chain never conflicts, so [cfl] may carry no rule — declare it so
     the root/parent's `not cfl` reference raises no undefined-atom warning *)
  emit ctx "#defined %s/1." cfl;
  let stl n = Printf.sprintf "%s_stl%d" tag n in
  let dec n = Printf.sprintf "%s_dec%d" tag n in
  let conf n = Printf.sprintf "%s_conf%d" tag n in
  let own_just (r : default) =
    if r.default_initialisatie then (
      if not root then
        fault "an initialisation default inside an exception group is ill-formed (WF-075)";
      match has_input with
      | Some hi -> [ Printf.sprintf "not %s(%s)" hi subject ]
      | None -> fault "initialisation default on characteristic defaults")
    else cond_atoms ctx ~subject ~subject_ot (req "default justification" r.default_voorwaarde)
  in
  (* a default's OWN decision (a plain default, an init default, or a form-a
     fall-back): on the attribute arm the consequence bindings sit in the
     body, so a leeg consequence leaves the decision underivable — the
     fall-through gate for free; [extra] carries the `not U_any` guard that
     restricts a form-a fall-back to exception groups that decide nothing (ch. 7.9) *)
  let emit_own_dec n (r : default) ~extra =
    if is_char then
      let b = if req "default besluit" r.default_besluit then 1 else 0 in
      head_body ctx (Printf.sprintf "%s(%s,%d)" (dec n) subject b)
        ((base :: own_just r) @ extra)
    else
      let bindings, term, scale =
        expr_term ctx ~subject ~subject_ot (req "default consequentie" r.default_consequentie)
      in
      if scale <> target_scale then
        fault "default value scale %d does not match target scale %d" scale target_scale;
      head_body ctx (Printf.sprintf "%s(%s,%s)" (dec n) subject term)
        ((base :: own_just r) @ bindings @ extra)
  in
  List.iteri
    (fun n (r : default) ->
      (match r.default_excepties with
      | None -> emit_own_dec n r ~extra:[]
      | Some egs ->
          (* the displacement wait threaded into the exception groups: every
             strictly-higher (lower-index) default of this chain, accumulated
             with the enclosing defaults' waits, so the exception groups' own
             §9.5 inconsistent deposit is suppressed when a higher default
             settles — exactly the guard the chain outcome below puts on
             [out]/[cfl] *)
          let default_displaced =
            outer_displaced
            @ List.init n (fun m -> Printf.sprintf "not %s(%s)" (stl m) subject)
          in
          let u_out, u_cfl, u_any =
            translate_group ctx ~is_char ~subject ~subject_ot ~base
              ~group_path:(path @ [ n ]) ~target_form ~target_scale ~has_input
              ~outer_displaced:default_displaced egs
          in
          (* the exception groups go first: adopt their clean decision,
             propagate its conflict; the own justification/consequence is the
             fall-back only when they decide nothing
             (ch. 7.9) *)
          head_body ctx (Printf.sprintf "%s(%s,V)" (dec n) subject)
            [ Printf.sprintf "%s(%s,V)" u_out subject ];
          head_body ctx (Printf.sprintf "%s(%s)" (conf n) subject)
            [ Printf.sprintf "%s(%s)" u_cfl subject ];
          let has_own =
            if is_char then r.default_besluit <> None else r.default_consequentie <> None
          in
          if has_own then
            emit_own_dec n r ~extra:[ Printf.sprintf "not %s(%s)" u_any subject ]);
      head_body ctx (Printf.sprintf "%s(%s)" (stl n) subject)
        [ Printf.sprintf "%s(%s,_)" (dec n) subject ];
      if r.default_excepties <> None then
        head_body ctx (Printf.sprintf "%s(%s)" (stl n) subject)
          [ Printf.sprintf "%s(%s)" (conf n) subject ])
    defaults;
  (* the chain outcome: the highest settling default wins — lower defaults are
     displaced by `not stl_m` for every strictly-higher m *)
  List.iteri
    (fun n (r : default) ->
      let displaced = List.init n (fun m -> Printf.sprintf "not %s(%s)" (stl m) subject) in
      head_body ctx (Printf.sprintf "%s(%s,V)" out subject)
        (Printf.sprintf "%s(%s,V)" (dec n) subject :: displaced);
      if r.default_excepties <> None then
        head_body ctx (Printf.sprintf "%s(%s)" cfl subject)
          (Printf.sprintf "%s(%s)" (conf n) subject :: displaced))
    defaults;
  (out, cfl)

and translate_group ctx ~is_char ~subject ~subject_ot ~base ~group_path
    ~target_form ~target_scale ~has_input ~outer_displaced (egs : exception_groups) :
    string * string * string =
  let tag = fresh ctx "ng" in
  let out = Printf.sprintf "%s_out" tag in
  let cfl = Printf.sprintf "%s_cfl" tag in
  let diff = Printf.sprintf "%s_diff" tag in
  let any = Printf.sprintf "%s_any" tag in
  (* a single-member group has no differing pair, so [diff] may carry no
     rule — declare it against the `cfl :- diff` / inconsistent references *)
  emit ctx "#defined %s/1." diff;
  let members =
    List.map
      (fun (eg : exception_group) ->
        translate_chain ctx ~is_char ~subject ~subject_ot ~base ~path:group_path
          ~root:false ~target_form ~target_scale ~has_input
          ~outer_displaced eg.eg_defaults)
      egs.egs_leden
  in
  let no_member_cfl =
    List.map (fun (_, mc) -> Printf.sprintf "not %s(%s)" mc subject) members
  in
  (* differing member decisions are a conflict at THIS group, keyed by the
     group's default-index path — unless a member itself conflicts, whose
     deeper key propagates instead (conflict-first, ch. 7.9) *)
  List.iteri
    (fun i (oi, _) ->
      List.iteri
        (fun j (oj, _) ->
          if i < j then
            head_body ctx (Printf.sprintf "%s(%s)" diff subject)
              ([ Printf.sprintf "%s(%s,X)" oi subject;
                 Printf.sprintf "%s(%s,Y)" oj subject; "X != Y" ]
              @ no_member_cfl))
        members)
    members;
  List.iter
    (fun (_, mc) ->
      head_body ctx (Printf.sprintf "%s(%s)" cfl subject)
        [ Printf.sprintf "%s(%s)" mc subject ])
    members;
  head_body ctx (Printf.sprintf "%s(%s)" cfl subject)
    [ Printf.sprintf "%s(%s)" diff subject ];
  (* the clean group decision (equal decisions collapse to one, differing are
     suppressed by the conflict); [any] = decided-or-conflicted, the fall-back
     gate of a form-a parent default *)
  List.iter
    (fun (om, _) ->
      head_body ctx (Printf.sprintf "%s(%s,V)" out subject)
        [ Printf.sprintf "%s(%s,V)" om subject; Printf.sprintf "not %s(%s)" cfl subject ])
    members;
  head_body ctx (Printf.sprintf "%s(%s)" any subject)
    [ Printf.sprintf "%s(%s,_)" out subject ];
  head_body ctx (Printf.sprintf "%s(%s)" any subject)
    [ Printf.sprintf "%s(%s)" cfl subject ];
  (* the exception groups' §9.5 deposit carries the SAME displacement wait the
     enclosing chain puts on its outcome (ch. 7.9): a strictly-higher settling
     default displaces these exception groups entirely, path-keyed fact and all, so
     the deposit must not fire on the bare [diff] atom alone (root members do
     not displace each other, so the bare-keyed root deposit stays
     unguarded) *)
  head_body ctx
    (Printf.sprintf "inconsistent(%s,%s)" (str (path_key target_form group_path)) subject)
    (Printf.sprintf "%s(%s)" diff subject :: outer_displaced);
  (out, cfl, any)

let translate_defaults_group ctx gidx (members : (int * prioritized_defaults) list) =
  (* the ch. 8.2 validity gate, per member: an excluded member is
     Undecided — no decision, no conflict, no §9.5 deposit (ref's
     member_decision returns Undecided before a single default is
     evaluated), so it simply leaves the group's member set. The group
     reconciles the members that remain. *)
  let _, l0 = List.hd members in
  let members =
    List.filter
      (fun ((_, l) : int * prioritized_defaults) -> applies ctx l.pd_geldigheid)
      members
  in
  if members = [] then (
    (* every member excluded: the group decides nothing. The attribute
       target's case input entered defeasibly (inputval/3 — translate_case
       keys on the UNFILTERED defaults list), so the pass-through must still
       be emitted, unconditionally, or the standing input would silently
       vanish. A characteristic target has no defeasible entry; nothing
       remains to emit. *)
    match l0.pd_attribuut with
    | Some a ->
        head_body ctx
          (Printf.sprintf "val(S,%s,V)" (str a))
          [ Printf.sprintf "inputval(S,%s,V)" (str a);
            Printf.sprintf "is_a(S,%s)" (str l0.pd_onderwerp) ]
    | None -> ())
  else begin
  let _, l0 = List.hd members in
  let subject = "S" in
  let base = Printf.sprintf "is_a(%s,%s)" subject (str l0.pd_onderwerp) in
  let is_char, target_name =
    match (l0.pd_kenmerk, l0.pd_attribuut) with
    | Some k, None -> (true, k)
    | None, Some a -> (false, a)
    | _ ->
        fault "prioritized defaults target exactly one of a characteristic or an attribute"
  in
  let target_form = l0.pd_onderwerp ^ "." ^ target_name in
  (* the attribute arm's input scaffolding: the target scale, and the input
     predicate the initialisation default and the defeasible-input rule read *)
  let target_scale, has_input =
    if is_char then (0, None)
    else begin
      let hi = Printf.sprintf "grp%d_has_input" gidx in
      head_body ctx (Printf.sprintf "%s(%s)" hi subject)
        [ base; Printf.sprintf "inputval(%s,%s,_)" subject (str target_name) ];
      (pow10 (attr_decimals ctx ~otype:l0.pd_onderwerp ~attr:target_name), Some hi)
    end
  in
  (* each top-level member is a chain reconciled at the root path [] *)
  let member_info =
    List.map
      (fun ((_, l) : int * prioritized_defaults) ->
        let out, cfl =
          translate_chain ctx ~is_char ~subject ~subject_ot:l.pd_onderwerp ~base ~path:[]
            ~root:true ~target_form ~target_scale ~has_input ~outer_displaced:[] l.defaults
        in
        (out, cfl, List.exists (fun (r : default) -> r.default_excepties <> None) l.defaults))
      members
  in
  (* the root group (ch. 7.2): differing member decisions and any propagated
     exception-group conflict block every write; a differing ROOT decision deposits
     the bare-keyed §9.5 fact. A conflict is possible only with ≥2 members or
     nested excepties — otherwise the guard is absent (the flat singleton). *)
  let nested = List.exists (fun (_, _, c) -> c) member_info in
  let conflict_possible = List.length members >= 2 || nested in
  let root_cfl = Printf.sprintf "grp%d_conflict(%s)" gidx subject in
  let root_diff = Printf.sprintf "grp%d_diff(%s)" gidx subject in
  if conflict_possible then begin
    (* propagate a member exception group's conflict up to the root *)
    List.iter
      (fun (_, cfl, can) ->
        if can then head_body ctx root_cfl [ Printf.sprintf "%s(%s)" cfl subject ])
      member_info;
    if List.length members >= 2 then begin
      let no_member_cfl =
        List.map (fun (_, cfl, _) -> Printf.sprintf "not %s(%s)" cfl subject) member_info
      in
      List.iteri
        (fun i (oi, _, _) ->
          List.iteri
            (fun j (oj, _, _) ->
              if i < j then
                head_body ctx root_diff
                  ([ Printf.sprintf "%s(%s,X)" oi subject;
                     Printf.sprintf "%s(%s,Y)" oj subject; "X != Y" ]
                  @ no_member_cfl))
            member_info)
        member_info;
      head_body ctx root_cfl [ root_diff ];
      head_body ctx (Printf.sprintf "inconsistent(%s,%s)" (str target_form) subject)
        [ root_diff ]
    end
  end;
  let conflict_guard = if conflict_possible then [ "not " ^ root_cfl ] else [] in
  if is_char then
    List.iter
      (fun (out, _, _) ->
        head_body ctx (Printf.sprintf "heeft(%s,%s)" subject (str target_name))
          ((Printf.sprintf "%s(%s,1)" out subject :: conflict_guard) @ [ base ]))
      member_info
  else begin
    let eff = Printf.sprintf "grp%d_eff(%s)" gidx subject in
    List.iter
      (fun (out, _, _) ->
        head_body ctx (Printf.sprintf "val(%s,%s,V)" subject (str target_name))
          ((Printf.sprintf "%s(%s,V)" out subject :: conflict_guard) @ [ base ]);
        head_body ctx eff
          ((Printf.sprintf "%s(%s,_)" out subject :: conflict_guard) @ [ base ]))
      member_info;
    (* the defeasible input: the case value stands unless the group
       effectively writes (a conflicting group leaves it standing) *)
    head_body ctx (Printf.sprintf "val(%s,%s,V)" subject (str target_name))
      [ Printf.sprintf "inputval(%s,%s,V)" subject (str target_name); base; "not " ^ eff ]
  end
  end

(* allocation (RUNTIME-SEMANTICS ch. 4): the ch. 4.2 recurrence over
   ordered recipient groups, in integer arithmetic — all three methods.
   Scale layer: shares are floored at the declared rounding scale d ≤ t
   by the integer division itself — the pot rides into the numerator
   upscaled by 10^(d−p), or the divisor by 10^(p−d), so division on
   non-negative operands is exactly floor_d (the weight scale cancels
   in the ratio); the written share upscales to the target scale t, and
   the remainder lives at its own scale r ≥ max(p, d). Without declared
   rounding ref writes the EXACT rational share and faults a
   non-fitting write — here an exactness viol/2 at the target scale,
   never a silent floor (the aligned form used to floor silently). *)
let translate_verdeling ctx i (v : resolved_verdeling) =
  (* windows on the untranslated-window forms fault LOUDLY (they were
     silently ignored before the ch. 8.2 gate landed for rules, prioritized
     defaults and creations): an unwindowed translation of a windowed allocation
     would silently apply outside its window *)
  (match v.vd_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "allocation validity windows are not translated (no corpus case)"
  | _ -> ());
  let methode =
    match v.vd_methode with
    | Some Complex | None ->
        (* WF-024 rejects both before translation; the backstop mirrors ref *)
        fault "COMPLEX allocation is not admissible IR (WF-024)"
    | Some m -> m
  in
  let bt = req "bronType" v.bron_type and dt = req "doelType" v.doel_type in
  let ba = req "bronAttribuut" v.bron_attribuut and da = req "doelAttribuut" v.doel_attribuut in
  (* case input on the allocation target: ref (like v0) OVERWRITES the
     target where the verdeling reaches an instance and the input stands
     where it does not (World.write_attr, ch. 4). The S10 defeasible-input
     layer carries exactly that — the case value enters as inputval/3
     (translate_case), the allocation's own head wins on reached instances,
     and the pass-through (below, once p is bound) re-admits the input on
     unreached ones. Engaged only where the case actually supplies the
     target, so a verdeling with no target input translates as before. *)
  let default_input = case_supplies ctx ~otype:dt ~attr:da in
  if default_input then begin
    (* STRATIFICATION FENCE (the narrowest safe form): the pass-through
       adds `not vdI_eff`, so if the target attribute feeds back into the
       allocation's own inputs (pot, weights, cap, key or condition) the
       negation has no stratified encoding. The verdeling never reads its
       own target, so ANY read elsewhere is refused loudly — only a target
       read nowhere else is provably acyclic. Read detection reuses the
       verifier's dependency reads (the machinery WF-045/046 trusts), so
       the fence cannot drift from the graph the verifier checks. *)
    let target_node = Ir_v0.Checks.n_attr dt da in
    let dforms = Ir_v0.Checks.dep_forms { doc = ctx.doc } in
    if
      List.exists
        (fun (f : Ir_v0.Checks.dep_form) ->
          List.exists (fun (node, _) -> node = target_node) f.df_reads)
        dforms
    then
      fault
        "the case supplies input for allocation target %S, which is read elsewhere: a feedback \
         into the allocation's pot, weights, criteria or condition has no stratified \
         negation-as-failure encoding (the S10 input default admits only a target read nowhere \
         else)"
        da;
    (* a second writer of the same target — another verdeling, a rule,
       prioritized defaults or a creation — leaves the overwrite order
       unpinned: ref's fixpoint decides it, no witness pins the ASP union, so
       refuse. *)
    if
      List.length
        (List.filter (fun (f : Ir_v0.Checks.dep_form) -> List.mem target_node f.df_writes) dforms)
      > 1
    then
      fault
        "the case supplies input for allocation target %S, which a second form also writes: the \
         overlap's overwrite order is unpinned (no witness), so the S10 input default is refused"
        da
  end;
  (* WF-025 field-validity backstops, rejected before translation on
     verifier-clean input *)
  (match (methode, v.rato_attribuut, v.maximum_attribuut) with
  | Gelijke_delen, Some _, _ ->
      fault "an equal-shares allocation with a ratio attribute is ill-formed (WF-025)"
  | (Gelijke_delen | Op_volgorde), None, Some _ ->
      fault "an allocation maximum requires the weighted split (WF-025)"
  | _ -> ());
  (match (methode, v.groepeer_attribuut) with
  | (Gelijke_delen | Naar_rato), Some _ ->
      fault "an ordering key on a non-ordered allocation is ill-formed (WF-025)"
  | _ -> ());
  let name = Option.value v.vd_naam ~default:"verdeling" in
  (* the §9.5 channel key: a deposit out of the voorwaarde (a division
     edge inside the condition) rides the allocation's name, exactly as
     ref's cond_err does (S13) *)
  ctx.rule_name <- name;
  let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty } in
  let role =
    role_canonical
      (match v.verbindende_rol with
      | Some rb -> Eval.bound_role env rb
      | None -> Eval.connecting_role env ~from_type:bt ~to_type:dt)
  in
  let p = Printf.sprintf "vd%d_" i in
  (* the S10 pass-through: the case value stands on any target instance the
     verdeling does not effectively write — a non-recipient the role never
     reaches, or a recipient the method excludes (a weightless proportional
     share, ref's leeg/standing, never 0). vdI_eff(X) fires with the SAME
     body as each val head (emit_write below), so the input yields exactly
     where the allocation decides. *)
  if default_input then
    emit ctx "val(D,%s,V) :- inputval(D,%s,V), is_a(D,%s), not %seff(D)." (str da) (str da)
      (str dt) p;
  (* --- the scale layer ------------------------------------------------ *)
  let pdec = attr_decimals ctx ~otype:bt ~attr:ba in
  let tdec = attr_decimals ctx ~otype:dt ~attr:da in
  (match v.afronding_decimalen with
  | Some d when d > tdec ->
      fault
        "allocation rounding at %d decimals is finer than its %d-decimal target (no corpus \
         case)"
        d tdec
  | _ -> ());
  (* exact mode: no declared rounding — ref keeps the exact rational
     share and faults a write that does not fit the target's decimals;
     the share division gets an exactness viol below, never a silent
     floor *)
  let exact = v.afronding_decimalen = None in
  let ddec = Option.value v.afronding_decimalen ~default:tdec in
  (* an unrounded (exact-share) allocation with a maximum: ref writes
     min(exact_share, cap) and faults a non-fitting write. Carried here for
     the proportional method with a cap no finer than its target:
     cdec ≤ tdec ⟹ capd is an exact integer at the target scale, so a
     clipped write always fits. The exactness viol (below) fires on the RAW
     proportional share whenever it is inexact, independent of the cap — so
     where the cap clips an inexact raw to a fitting integer, clingo refuses
     LOUDLY though ref writes the fitting cap and passes: clingo is stricter
     in that corner, a conservative loud refusal, never a silent floor (no
     corpus case exercises it). A cap finer than the target (whose clipped
     write might itself not fit, a silent floor) and the ordered method
     keep faulting: no witness pins that corner. Only a fence narrows here;
     the emitted allocation machinery is unchanged. *)
  (match (exact, v.maximum_attribuut) with
  | true, Some cap when methode = Naar_rato && attr_decimals ctx ~otype:dt ~attr:cap <= tdec -> ()
  | true, Some _ ->
      fault
        "an unrounded allocation with a maximum is not translated here (only the proportional \
         method with a cap no finer than its target is carried; otherwise whether the exact share \
         or the cap is the written minimum decides the exactness fault; no corpus case)"
  | _ -> ());
  let cdec = Option.map (fun cap -> attr_decimals ctx ~otype:dt ~attr:cap) v.maximum_attribuut in
  let rdec = Option.map (fun ra -> attr_decimals ctx ~otype:bt ~attr:ra) v.restant_attribuut in
  (match rdec with
  | Some r when r < pdec || r < ddec ->
      fault
        "an allocation remainder at %d decimals under a %d-decimal pot and %d-decimal \
         rounding is not exactly representable (no corpus case)"
        r pdec ddec
  | _ -> ());
  let m_num = pow10 (max 0 (ddec - pdec)) in
  let m_den = pow10 (max 0 (pdec - ddec)) in
  let m_val = pow10 (tdec - ddec) in
  let scaled t m = if m = 1 then t else Printf.sprintf "%s*%d" t m in
  let divd t m = if m = 1 then t else Printf.sprintf "(%s*%d)" t m in
  (* the conditional gate (ch. 4.1): a source distributes only where its
     voorwaarde holds — ref evaluates the condition before it touches
     pot or recipients, so every semantic guard and §9.5 deposit below
     rides this gate. The wrap fences do NOT: grounding computes the
     scaled products whether or not the condition turns out true. *)
  head_body ctx
    (Printf.sprintf "%ssrc(B)" p)
    (Printf.sprintf "is_a(B,%s)" (str bt)
    ::
    (match v.vd_voorwaarde with
    | None -> []
    | Some c -> cond_atoms ctx ~subject:"B" ~subject_ot:bt c));
  let src = Printf.sprintf "%ssrc(B)" p in
  (* the wrap fences: operand bounds shrink by exactly the scale
     multipliers a misaligned form introduces, so every product — the
     pot upscale to rounding/target/remainder scale, pot×weight, the
     weight sum times the divisor upscale, and the share sums — provably
     stays inside 2^31 (46340² < 2^31; equal scales keep the historical
     46340 everywhere) *)
  let pot_bound =
    46340
    / max
        (pow10 (max 0 (tdec - pdec)))
        (match rdec with Some r -> pow10 (r - pdec) | None -> 1)
  in
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",B) :- \
     is_a(B,%s), val(B,%s,A), |A| > %d."
    (str bt) (str ba) pot_bound;
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",B) :- \
     is_a(B,%s), 46340 < #count { X : rol(B,%s,X) }."
    (str bt) (str role);
  (match v.rato_attribuut with
  | Some rato ->
      emit ctx
        "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",X) :- \
         rol(B,%s,X), is_a(B,%s), val(X,%s,W), |W| > %d."
        (str role) (str bt) (str rato) (46340 / m_den)
  | None -> ());
  (match (v.maximum_attribuut, cdec) with
  | Some cap, Some c when pow10 (max 0 (ddec - c)) > 1 ->
      emit ctx
        "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",X) :- \
         rol(B,%s,X), is_a(B,%s), val(X,%s,C), |C| > %d."
        (str role) (str bt) (str cap) (46340 / pow10 (ddec - c))
  | _ -> ());
  emit ctx "%sheeft_pot(B) :- is_a(B,%s), val(B,%s,_)." p (str bt) (str ba);
  (* the target write and the remainder, shared across the methods; the
     remainder guard carries each method's Nothing case (ch. 4.4: zero
     recipients, or an absent/zero weight sum, write NO rest) *)
  let emit_write ~eff_atom share_atom =
    emit ctx "val(X,%s,%s) :- %s." (str da) (scaled "S" m_val) share_atom;
    (* the effectiveness helper for the S10 pass-through above: same body as
       the val head (the share atom, value anonymised), so it derives
       exactly where the verdeling writes the target instance X *)
    if default_input then emit ctx "%seff(X) :- %s." p eff_atom
  in
  let emit_rest ~guard ~sum =
    match (v.restant_attribuut, rdec) with
    | Some ra, Some r ->
        emit ctx "val(B,%s,%s-%s) :- %s, val(B,%s,A), %s, T = #sum { %s }." (str ra)
          (scaled "A" (pow10 (r - pdec)))
          (scaled "T" (pow10 (r - ddec)))
          src (str ba) guard sum
    | _ -> () (* undeclared: the rest lapses (a SHOULD, no WF rule) *)
  in
  (* cap-before-floor (ch. 4.3): share = min(floor_d raw, floor_d cap) —
     the floor commutes with min — with the degrade edges: an empty or
     negative cap is uncapped + a channel fact. [raw_pat] is the
     method's raw atom with X free; the declared cap floors to the
     rounding scale (an upscale is exact, a downscale is the floor). *)
  let emit_cap_atoms ~raw_pat =
    let cap = req "maximumAttribuut" v.maximum_attribuut in
    let c = match cdec with Some c -> c | None -> assert false in
    let capd =
      if ddec >= c then scaled "C" (pow10 (ddec - c))
      else Printf.sprintf "C/%d" (pow10 (c - ddec))
    in
    emit ctx "%scapd(X,%s) :- %s, val(X,%s,C), C >= 0." p capd raw_pat (str cap);
    emit ctx "inconsistent(%s,X) :- %s, val(X,%s,C), C < 0." (str name) raw_pat (str cap);
    emit ctx "inconsistent(%s,X) :- %s, not %sheeft_cap(X)." (str name) raw_pat p;
    emit ctx "%sheeft_cap(X) :- val(X,%s,C), C >= 0, %s." p (str cap) raw_pat
  in
  let exact_viol = "allocation: a share does not fit the target scale (an explicit rounding is mandated before precision is lost)" in
  match methode with
  | Complex -> assert false (* fenced above *)
  | Naar_rato ->
      let rato = req "ratoAttribuut (proportional)" v.rato_attribuut in
      (* fail-fast guards, mirrored on ref/'s faults and on floor
         semantics: integer division is floor only on non-negative
         operands, and an empty pot is the ch. 4.4 v0 hole ref/ faults
         on — viol/2 witnesses plus the global integrity constraint
         make clingo fail identically instead of silently diverging *)
      emit ctx "viol(\"allocation: negative pot\",B) :- %s, val(B,%s,A), A < 0." src (str ba);
      emit ctx "viol(\"allocation: empty pot\",B) :- %s, rol(B,%s,_), not %sheeft_pot(B)." src
        (str role) p;
      (* eligible recipients: filled weight; the weightless are excluded
         and surface on the channel (ch. 4.4) *)
      emit ctx "%sgewicht(X,W) :- rol(B,%s,X), %s, val(X,%s,W)." p (str role) src (str rato);
      emit ctx "viol(\"allocation: negative weight\",X) :- %sgewicht(X,W), W < 0." p;
      emit ctx "inconsistent(%s,X) :- rol(B,%s,X), %s, not %sheeft_gewicht(X)." (str name)
        (str role) src p;
      emit ctx "%sheeft_gewicht(X) :- %sgewicht(X,_)." p p;
      emit ctx "%swsum(B,S) :- %s, S = #sum { W,X : rol(B,%s,X), %sgewicht(X,W) }, S != 0." p
        src (str role) p;
      let num = Printf.sprintf "%s*W" (scaled "A" m_num) in
      let den = divd "S" m_den in
      let raw_body =
        Printf.sprintf "rol(B,%s,X), %sgewicht(X,W), %swsum(B,S), val(B,%s,A)" (str role) p p
          (str ba)
      in
      emit ctx "%sraw(X,(%s)/%s) :- %s." p num den raw_body;
      if exact then
        emit ctx "viol(\"%s\",X) :- %s, (%s) \\ %s != 0." exact_viol raw_body num den;
      (match v.maximum_attribuut with
      | Some _ ->
          emit_cap_atoms ~raw_pat:(Printf.sprintf "%sraw(X,_)" p);
          emit ctx "%sshare(X,R) :- %sraw(X,R), %scapd(X,C), R <= C." p p p;
          emit ctx "%sshare(X,C) :- %sraw(X,R), %scapd(X,C), R > C." p p p;
          emit ctx "%sshare(X,R) :- %sraw(X,R), not %sheeft_cap(X)." p p p
      | None -> emit ctx "%sshare(X,R) :- %sraw(X,R)." p p);
      emit_write ~eff_atom:(Printf.sprintf "%sshare(X,_)" p) (Printf.sprintf "%sshare(X,S)" p);
      emit_rest ~guard:(Printf.sprintf "%swsum(B,_)" p)
        ~sum:(Printf.sprintf "S,X : rol(B,%s,X), %sshare(X,S)" (str role) p)
  | Gelijke_delen ->
      (* one group, all recipients, unweighted (ch. 4.2); the negative
         and empty pot guards as in the proportional method (the empty
         pot is the same ch. 4.4 v0 mirror hole) *)
      emit ctx "viol(\"allocation: negative pot\",B) :- %s, val(B,%s,A), A < 0." src (str ba);
      emit ctx "viol(\"allocation: empty pot\",B) :- %s, rol(B,%s,_), not %sheeft_pot(B)." src
        (str role) p;
      emit ctx "%sn(B,N) :- %s, rol(B,%s,_), N = #count { X : rol(B,%s,X) }." p src (str role)
        (str role);
      let num = scaled "A" m_num in
      let den = divd "N" m_den in
      let raw_body = Printf.sprintf "rol(B,%s,X), %sn(B,N), val(B,%s,A)" (str role) p (str ba) in
      emit ctx "%sshare(X,(%s)/%s) :- %s." p num den raw_body;
      if exact then
        emit ctx "viol(\"%s\",X) :- %s, (%s) \\ %s != 0." exact_viol raw_body num den;
      emit_write ~eff_atom:(Printf.sprintf "%sshare(X,_)" p) (Printf.sprintf "%sshare(X,S)" p);
      emit_rest ~guard:(Printf.sprintf "%sn(B,_)" p)
        ~sum:(Printf.sprintf "S,X : rol(B,%s,X), %sshare(X,S)" (str role) p)
  | Op_volgorde ->
      (* groups are the equal-key classes of the ordering attribute,
         ascending/descending as declared, an EMPTY key sorting last in
         both directions (ch. 4.2): a filled key's rank counts the
         distinct keys before it, the keyless group sits at the filled
         group count. vdN_pot(B,R,P) IS the recurrence: rank R divides
         the pot as it stands at entry, shares are capped, floored and
         clamped at 0, and max(0, P−ΣS) carries forward — the recursion
         through the assignment aggregates grounds because the rank
         strictly increases under the ranks bound. Negative raws (a
         negative pot) clamp to the real 0 ref assigns, so the
         trunc-vs-floor split of negative division never surfaces and a
         negative pot needs no viol here; an empty pot is Nothing (the
         §9.7.6 discipline), never the equal/proportional fault. *)
      let rolx = Printf.sprintf "rol(B,%s,X)" (str role) in
      (match v.groepeer_attribuut with
      | Some ga ->
          let cmp = if v.afnemend then ">" else "<" in
          emit ctx "%skey(B,X,K) :- %s, %s, val(X,%s,K)." p rolx src (str ga);
          emit ctx "%shaskey(B,X) :- %skey(B,X,_)." p p;
          emit ctx "%snokey(B,X) :- %s, %s, not %shaskey(B,X)." p rolx src p;
          emit ctx "%sgkey(B,K) :- %skey(B,_,K)." p p;
          emit ctx "%sgrank(B,K,R) :- %sgkey(B,K), R = #count { K2 : %sgkey(B,K2), K2 %s K }."
            p p p cmp;
          emit ctx "%sng(B,N) :- %s, rol(B,%s,_), N = #count { K : %sgkey(B,K) }." p src
            (str role) p;
          emit ctx "%smrank(B,X,R) :- %skey(B,X,K), %sgrank(B,K,R)." p p p;
          emit ctx "%smrank(B,X,N) :- %snokey(B,X), %sng(B,N)." p p p;
          emit ctx "%shasnokey(B) :- %snokey(B,_)." p p;
          emit ctx "%sranks(B,N+1) :- %sng(B,N), %shasnokey(B)." p p p;
          emit ctx "%sranks(B,N) :- %sng(B,N), not %shasnokey(B)." p p p
      | None ->
          (* keyless: ONE group — ref sorts an all-empty key list into a
             single equal-key class *)
          emit ctx "%smrank(B,X,0) :- %s, %s." p rolx src;
          emit ctx "%sranks(B,1) :- %s, rol(B,%s,_)." p src (str role));
      emit ctx "%spot(B,0,%s) :- %s, val(B,%s,A), rol(B,%s,_)." p (scaled "A" m_num) src
        (str ba) (str role);
      (match v.rato_attribuut with
      | Some rato ->
          (* the weighted tie-break (ch. 4.2/4.4): an empty weight is a
             real 0 + a §9.5 deposit, a negative one counts as 0
             silently; a tie group whose weights sum to 0 assigns 0 to
             every member (the recurrence's 0/0 case) *)
          emit ctx "%shasw(X) :- val(X,%s,_)." p (str rato);
          emit ctx "%stw(B,X,W) :- %smrank(B,X,_), val(X,%s,W), W >= 0." p p (str rato);
          emit ctx "%stw(B,X,0) :- %smrank(B,X,_), val(X,%s,W), W < 0." p p (str rato);
          emit ctx "%stw(B,X,0) :- %smrank(B,X,_), not %shasw(X)." p p p;
          emit ctx "inconsistent(%s,X) :- %smrank(B,X,_), %sheeft_pot(B), not %shasw(X)."
            (str name) p p p;
          emit ctx
            "%sgwsum(B,R,S) :- %spot(B,R,_), %smrank(B,_,R), S = #sum { W,X : %smrank(B,X,R), \
             %stw(B,X,W) }."
            p p p p p;
          emit ctx
            "%sraw(B,X,(P*W)/%s) :- %smrank(B,X,R), %spot(B,R,P), %stw(B,X,W), %sgwsum(B,R,S), \
             S != 0."
            p (divd "S" m_den) p p p p;
          emit ctx "%sraw(B,X,0) :- %smrank(B,X,R), %spot(B,R,_), %sgwsum(B,R,0)." p p p p;
          if exact then
            emit ctx
              "viol(\"%s\",X) :- %smrank(B,X,R), %spot(B,R,P), %stw(B,X,W), %sgwsum(B,R,S), \
               S != 0, P*W > 0, (P*W) \\ %s != 0."
              exact_viol p p p p (divd "S" m_den)
      | None ->
          emit ctx
            "%sgsize(B,R,N) :- %spot(B,R,_), %smrank(B,_,R), N = #count { X : %smrank(B,X,R) }."
            p p p p;
          emit ctx "%sraw(B,X,P/%s) :- %smrank(B,X,R), %spot(B,R,P), %sgsize(B,R,N)." p
            (divd "N" m_den) p p p;
          if exact then
            emit ctx
              "viol(\"%s\",X) :- %smrank(B,X,R), %spot(B,R,P), %sgsize(B,R,N), P > 0, \
               P \\ %s != 0."
              exact_viol p p p (divd "N" m_den));
      (match v.maximum_attribuut with
      | Some _ ->
          emit_cap_atoms ~raw_pat:(Printf.sprintf "%sraw(_,X,_)" p);
          emit ctx "%ssh0(B,X,S) :- %sraw(B,X,S), %scapd(X,C), S <= C." p p p;
          emit ctx "%ssh0(B,X,C) :- %sraw(B,X,S), %scapd(X,C), S > C." p p p;
          emit ctx "%ssh0(B,X,S) :- %sraw(B,X,S), not %sheeft_cap(X)." p p p
      | None -> emit ctx "%ssh0(B,X,S) :- %sraw(B,X,S)." p p);
      (* the ordered-only max(0,·) clamps of ch. 4.2 *)
      emit ctx "%sshare(B,X,S) :- %ssh0(B,X,S), S >= 0." p p;
      emit ctx "%sshare(B,X,0) :- %ssh0(B,X,S), S < 0." p p;
      emit ctx
        "%sspent(B,R,T) :- %spot(B,R,_), T = #sum { %s,X : %smrank(B,X,R), %sshare(B,X,S) }."
        p p (scaled "S" m_den) p p;
      emit ctx
        "%spot(B,R+1,P-T) :- %spot(B,R,P), %sspent(B,R,T), %sranks(B,N), R+1 < N, P-T >= 0."
        p p p p;
      emit ctx "%spot(B,R+1,0) :- %spot(B,R,P), %sspent(B,R,T), %sranks(B,N), R+1 < N, P-T < 0."
        p p p p;
      emit_write ~eff_atom:(Printf.sprintf "%sshare(B,X,_)" p) (Printf.sprintf "%sshare(B,X,S)" p);
      emit_rest ~guard:(Printf.sprintf "rol(B,%s,_)" (str role))
        ~sum:(Printf.sprintf "S,X : %sshare(B,X,S)" p)

let translate_consistentie ctx i (c : resolved_consistentie) =
  (* the allocation-window sibling: loud, never silently unwindowed *)
  (match c.cons_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "consistency-check validity windows are not translated (no corpus case)"
  | _ -> ());
  let name = Option.value c.cons_naam ~default:(Printf.sprintf "consistentie%d" i) in
  (* a division inside the assertion deposits its channel fact under
     THIS name, like ref's err collector (the verify lens caught the
     stale rule_name of the last-translated rule leaking in) *)
  ctx.rule_name <- name;
  let subject_ot = req "subject" c.cons_subject in
  let ok = Printf.sprintf "cons%d_ok(S)" i in
  let atoms = cond_atoms ctx ~subject:"S" ~subject_ot (req "assertie" c.assertie) in
  head_body ctx ok (Printf.sprintf "is_a(S,%s)" (str subject_ot) :: atoms);
  emit ctx "inconsistent(%s,S) :- is_a(S,%s), not cons%d_ok(S)." (str name) (str subject_ot) i

(* ch. 5.9 ever-uniqueness, shared by BOTH uniqueness forms
   (compositionality: the same key, the same reading, either form).
   The guards: at most one temporal component per key list, and a
   temporal key supplied as a bucket scalar faults — ever-uniqueness
   needs the full timeline. *)
let uni_key_guards ctx ~otype ~keys =
  let temporal = List.filter (fun k -> attr_temporal ctx ~otype ~attr:k) keys in
  if List.length temporal > 1 then
    fault
      "a uniqueness key list carries at most one temporal key component (the same-moment \
       tuple join has no corpus case; ch. 5.9)";
  List.iter
    (fun k ->
      if attr_temporal ctx ~otype ~attr:k then
        Eval.SMap.iter
          (fun _ (inst : Uchar_ref.World.instance) ->
            if
              norm inst.otype = norm otype
              && Uchar_ref.World.timeline inst k = None
              && Uchar_ref.World.attr inst k <> Value.Empty
            then
              fault
                "the ever-unique key %S needs the full timeline; %S supplies only a scalar \
                 (ch. 5.9)"
                k inst.label)
          ctx.case.world.instances)
    keys

(* the key atom for one position: a val join for a scalar key, a tl
   join for a temporal one — ANY segment's value, which IS the ever
   reading (set semantics deduplicates same-instance redeclaration) *)
let uni_key_atom ctx ~otype ~label ~pos k =
  if attr_temporal ctx ~otype ~attr:k then Printf.sprintf "tl(%s,%s,_,_,K%d)" label (str k) pos
  else Printf.sprintf "val(%s,%s,K%d)" label (str k) pos

let translate_uniciteit ctx (u : resolved_uniciteit) =
  (* the allocation-window sibling: loud, never silently unwindowed *)
  (match u.uni_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "uniqueness validity windows are not translated (no corpus case)"
  | _ -> ());
  let name = Option.value u.uni_naam ~default:"uniciteit" in
  let subject_ot = req "subject" u.uni_subject in
  let keys = List.map (req "sleutel") u.sleutels in
  uni_key_guards ctx ~otype:subject_ot ~keys;
  let joins label =
    List.mapi (fun i k -> uni_key_atom ctx ~otype:subject_ot ~label ~pos:i k) keys
  in
  (* the scope hop (ch. 5.9, WF-054): both rows join the same scope
     parent — an instance without the rol/3 atom (scope-less) never
     matches, the pinned runtime's narrowing for free *)
  let scope_atoms =
    match (u.scope_rol, u.scope_veld) with
    | None, None -> []
    | rb_opt, sv_opt ->
        let env =
          { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
            as_of = None; locals = Eval.SMap.empty }
        in
        let role =
          match rb_opt with
          | Some rb -> Eval.bound_role env rb
          | None ->
              Eval.role_by_name env ~self_type:subject_ot
                ~name:(req "scopeVeld" sv_opt)
        in
        [ Printf.sprintf "rol(S1,%s,P)" (str (role_canonical role));
          Printf.sprintf "rol(S2,%s,P)" (str (role_canonical role)) ]
  in
  emit ctx "inconsistent(%s,S1) :- is_a(S1,%s), is_a(S2,%s), S1 != S2, %s." (str name)
    (str subject_ot) (str subject_ot)
    (String.concat ", " (joins "S1" @ joins "S2" @ scope_atoms))

(* the uniqueness union (ch. 5.9): every source feeds one shared key
   predicate — a duplicate may arise across two different source types.
   Key positions harmonise to the maximum declared scale across sources,
   so 5 and 5.00 collide exactly as the runtime's value equality does. *)
let translate_uniciteit_unie ctx i (u : resolved_uniciteit_unie) =
  (* the allocation-window sibling: loud, never silently unwindowed *)
  (match u.uu_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "uniqueness-union validity windows are not translated (no corpus case)"
  | _ -> ());
  let name = Option.value u.uu_naam ~default:(Printf.sprintf "uniciteitsunie%d" i) in
  let arity =
    match u.bronnen with
    | b :: _ -> List.length b.uub_sleutels
    | [] -> fault "a uniqueness union carries at least one source"
  in
  let max_scale pos =
    List.fold_left
      (fun m (b : uniciteit_unie_bron) ->
        let t = req "UniciteitUnie.bron.type" b.uub_type in
        let k = req "sleutel" (List.nth b.uub_sleutels pos) in
        max m (attr_decimals ctx ~otype:t ~attr:k))
      0 u.bronnen
  in
  let scales = List.init arity max_scale in
  let pred = Printf.sprintf "uu%d_key" i in
  (* the source INDEX rides the key atom: v0 SUMS the per-source counts,
     so an instance contributing one tuple through two sources counts
     twice — #count over (instance, source) pairs is exactly that sum *)
  List.iteri
    (fun j (b : uniciteit_unie_bron) ->
      let t = req "UniciteitUnie.bron.type" b.uub_type in
      (* the shared ever-uniqueness guards and key atoms (ch. 5.9):
         one reading for the union and the single form alike *)
      uni_key_guards ctx ~otype:t ~keys:(List.map (req "sleutel") b.uub_sleutels);
      let head_args =
        List.mapi
          (fun pos k ->
            let k = req "sleutel" k in
            let d = attr_decimals ctx ~otype:t ~attr:k in
            let m = pow10 (List.nth scales pos - d) in
            if m = 1 then Printf.sprintf "K%d" pos else Printf.sprintf "(K%d*%d)" pos m)
          b.uub_sleutels
      in
      let body =
        Printf.sprintf "is_a(S,%s)" (str t)
        :: List.mapi
             (fun pos k -> uni_key_atom ctx ~otype:t ~label:"S" ~pos (req "sleutel" k))
             b.uub_sleutels
      in
      (* the per-position upscale is an emitted multiplication: fence
         the raw key against the compile-time factor *)
      List.iteri
        (fun pos k ->
          let k = req "sleutel" k in
          let d = attr_decimals ctx ~otype:t ~attr:k in
          let m = pow10 (List.nth scales pos - d) in
          if m > 1 then
            emit ctx
              "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",S) :- \
               %s, |K%d| > %d."
              (String.concat ", " body)
              pos (max_int31 / m))
        b.uub_sleutels;
      emit ctx "%s(S,%d,%s) :- %s." pred j (String.concat "," head_args)
        (String.concat ", " body))
    u.bronnen;
  let vars = List.init arity (fun p -> Printf.sprintf "V%d" p) in
  emit ctx "inconsistent(%s,S) :- %s(S,_,%s), 2 <= #count { O,J : %s(O,J,%s) }." (str name)
    pred (String.concat "," vars) pred (String.concat "," vars)

(* reify a list VALUE as the indexed facts the fold family reads (S15):
   [lpres(L)] marks the list present (so a present-but-empty list still fires
   the fold base and ListLength's count — the value/absence distinction of
   ch. 10.1), and [lelem(L,I,V)] carries each element at its 1-based position,
   scaled by the ELEMENT datatype (numeric elements by their decimals, a sum
   element as its ground constructor term). Order is load-bearing and kept. *)
let reify_list ctx ~lid ~element (xs : Value.t list) =
  emit ctx "lpres(%s)." lid;
  List.iteri
    (fun n (v : Value.t) ->
      let term =
        match element with
        | Sum_type { constructoren; _ } -> (
            match v with
            | Value.Ctor (tag, payload) -> ctor_value_term constructoren (tag, payload)
            | _ -> fault "a non-constructor value in a sum-typed list element")
        | Struct_type { domein; velden } -> (
            match v with
            | Value.Record fs -> struct_value_term (domein, velden) fs
            | _ -> fault "a non-record value in a struct-typed list element")
        | _ -> string_of_int (scaled_of_value ~decimals:(decimals_of_datatype (Some element)) v)
      in
      emit ctx "lelem(%s,%d,%s)." lid (n + 1) term)
    xs

(* --- case facts ---------------------------------------------------------- *)

let translate_case ctx (case : Case.t) =
  let doc = ctx.doc in
  (* attribute-defaults targets — the ch. 7.8 standing targets (the bare
     scalar computing rule) and the allocation targets (S12, the verdeling
     whose fence translate_verdeling has already cleared) — take their
     input values defeasibly (inputval/3), standing unless the
     defaults/rule/verdeling decides *)
  let standing = Ir_v0.Desugar.standing_targets doc in
  let defaults_target otype aname =
    List.exists
      (fun (l : prioritized_defaults) ->
        match l.pd_attribuut with
        | Some a -> norm l.pd_onderwerp = norm otype && norm a = norm aname
        | None -> false)
      doc.prioritized_defaults
    || List.mem (norm otype, norm aname) standing
    || List.exists
         (fun (v : resolved_verdeling) ->
           (match v.doel_type with Some dt -> norm dt = norm otype | None -> false)
           && match v.doel_attribuut with Some da -> norm da = norm aname | None -> false)
         doc.verdelingen
  in
  Eval.SMap.iter
    (fun label (i : Uchar_ref.World.instance) ->
      emit ctx "is_a(%s,%s)." (str_raw label) (str i.otype);
      (* generation depth 0 for every input instance — the recursive
         creations' grounding bound counts from here *)
      if has_recursive_creation doc then emit ctx "depth(%s,0)." (str_raw label);
      Eval.SMap.iter
        (fun aname v ->
          match v with
          | Value.Bool b ->
              (* a boolean attribute is a characteristic in v0 (booleans are
                 characteristics): true → heeft/2, false → absence
                 (closed-world), the [bool_atoms] read-shape on the case side.
                 The ch. 1.2d equality read additionally needs the three-way
                 distinction (waar / onwaar / leeg) the closed-world heeft/2
                 cannot carry, so the value ALSO enters as val/3 with the
                 bare symbol waar/onwaar — false is a VALUE there, distinct
                 from absence (S2/S5). *)
              if b then emit ctx "heeft(%s,%s)." (str_raw label) (str aname);
              emit ctx "val(%s,%s,%s)." (str_raw label) (str aname) (symbolic_value_term v)
          | Value.Text _ | Value.Enum _ ->
              (* ch. 1.2d scalar carriage: a text value is its verbatim quoted
                 string, an enum label its normalised quoted string — the same
                 ground terms symbolic_term reads and the literal emission
                 writes, so equality is direct term identity (S2). *)
              emit ctx "val(%s,%s,%s)." (str_raw label) (str aname) (symbolic_value_term v)
          | Value.Ctor (tag, payload) ->
              (* ch. 9.4 case carriage: a sum-typed input value is a ground
                 constructor term, payload scaled by its declared payload
                 datatype. A sum-typed COMPUTED target is fenced against
                 case-supplied input in translate_regel, so a sum value in the
                 input is input-only here and enters as a plain val/3. *)
              let constructoren =
                match Ir_v0.Checks.find_ci (objecttype ctx i.otype).attributen aname with
                | Some (Some (Sum_type { constructoren; _ })) -> constructoren
                | _ -> fault "constructor value on non-sum-typed attribute %S" aname
              in
              emit ctx "val(%s,%s,%s)." (str_raw label) (str aname)
                (ctor_value_term constructoren (tag, payload))
          | Value.Lijst xs ->
              (* ch. 10.5 case carriage: a list-typed input attribute reifies
                 as lpres/lelem keyed by [alist(label,attr)] — the same list-id
                 the fold family reads. A list-typed COMPUTED target has no
                 monotone encoding here, fenced in translate_regel. *)
              let element =
                match Ir_v0.Checks.find_ci (objecttype ctx i.otype).attributen aname with
                | Some (Some (List_type { element })) -> element
                | _ -> fault "list value on non-list-typed attribute %S" aname
              in
              reify_list ctx ~lid:(Printf.sprintf "alist(%s,%s)" (str_raw label) (str aname))
                ~element xs
          | Value.Record fs ->
              (* ch. 11.3 case carriage: a struct-typed input attribute is a
                 ground record term, fields scaled by their declared datatypes.
                 A struct-typed COMPUTED target is fenced against case-supplied
                 input in translate_regel, so a struct value is input-only here
                 and enters as a plain val/3 (the Ctor sibling). *)
              let domein, velden =
                match Ir_v0.Checks.find_ci (objecttype ctx i.otype).attributen aname with
                | Some (Some (Struct_type { domein; velden })) -> (domein, velden)
                | _ -> fault "record value on non-struct-typed attribute %S" aname
              in
              emit ctx "val(%s,%s,%s)." (str_raw label) (str aname)
                (struct_value_term (domein, velden) fs)
          | Value.Date_time t ->
              (* ch. 3.8 the millisecond layer: a datum-tijd input rides the
                 pair term dt(D, MS) — the civil day beside the ms-of-day — so
                 its absolute ms value never enters clingo's integer range (the
                 A4 pair-term encoding) *)
              emit ctx "val(%s,%s,%s)." (str_raw label) (str aname) (datetime_term t)
          | _ ->
              let d = attr_decimals ctx ~otype:i.otype ~attr:aname in
              let pred = if defaults_target i.otype aname then "inputval" else "val" in
              emit ctx "%s(%s,%s,%d)." pred (str_raw label) (str aname)
                (scaled_of_value ~decimals:d v))
        i.attrs;
      List.iter (fun k -> emit ctx "heeft(%s,%s)." (str_raw label) (str k)) i.chars;
      Eval.SMap.iter
        (fun aname (tl : Uchar_ref.Timeline.t) ->
          (* ch. 9.4: sum-typed temporal carriage is a recorded flank — the
             value model admits it but no case exercises it, so the ASP case
             emission faults loudly rather than silently omitting segments *)
          (match Ir_v0.Checks.find_ci (objecttype ctx i.otype).attributen aname with
          | Some (Some (Sum_type _)) ->
              fault "temporal sum carriage is a recorded flank; no corpus case"
          | Some (Some (Struct_type _)) ->
              fault "temporal struct carriage is a recorded flank; no corpus case"
          | _ -> ());
          let d = attr_decimals ctx ~otype:i.otype ~attr:aname in
          (* a standing temporal target's input timeline enters
             defeasibly (inputtl/5), standing whole unless the fold
             decides (ch. 7.8, the timeline arm) *)
          let pred =
            if List.mem (norm i.otype, norm aname) (Ir_v0.Desugar.standing_temporal_targets doc)
            then "inputtl"
            else "tl"
          in
          List.iter
            (fun (s : Uchar_ref.Timeline.segment) ->
              emit ctx "%s(%s,%s,%d,%d,%d)." pred (str_raw label) (str aname) s.s_van s.s_tot
                (scaled_of_value ~decimals:d s.s_waarde))
            tl)
        i.tls;
      Eval.SMap.iter
        (fun cname periods ->
          List.iter
            (fun (v, t) -> emit ctx "ktl(%s,%s,%d,%d)." (str_raw label) (str cname) v t)
            periods)
        i.ctls;
      Eval.SMap.iter
        (fun key targets ->
          let canonical = role_canonical (Case.resolve_role doc ~otype:i.otype ~key) in
          List.iter
            (fun t ->
              emit ctx "rol(%s,%s,%s)." (str_raw label) (str canonical) (str_raw t);
              (* the 1:1 creation guard reads the input links only
                 (ch. 5.5's snapshot narrowing) — mirror them as
                 inputrol/3 when a creation could consult them *)
              if doc.object_creaties <> [] then
                emit ctx "inputrol(%s,%s,%s)." (str_raw label) (str canonical) (str_raw t))
            targets)
        i.links)
    case.world.instances;
  Eval.SMap.iter
    (fun pname v ->
      match Ir_v0.Checks.find_ci doc.parameters pname with
      | Some { par_granulariteit = Some _; _ } -> () (* never read; ch. 8 *)
      | Some { par_datatype = Some (List_type { element }); _ } -> (
          (* ch. 10.5: a list-typed parameter reifies as lpres/lelem keyed by
             [plist(name)] — the same list-id the fold family reads *)
          match v with
          | Value.Lijst xs -> reify_list ctx ~lid:(Printf.sprintf "plist(%s)" (str pname)) ~element xs
          | _ -> fault "list-typed parameter %S carries a non-list value" pname)
      | Some p ->
          let d = decimals_of_datatype p.par_datatype in
          emit ctx "param(%s,%d)." (str pname) (scaled_of_value ~decimals:d v)
      | None -> fault "unknown parameter %S" pname)
    case.params;
  Eval.SMap.iter
    (fun pname (tl : Uchar_ref.Timeline.t) ->
      match Ir_v0.Checks.find_ci doc.parameters pname with
      | Some p ->
          let d = decimals_of_datatype p.par_datatype in
          List.iter
            (fun (s : Uchar_ref.Timeline.segment) ->
              emit ctx "param_tl(%s,%d,%d,%d)." (str pname) s.s_van s.s_tot
                (scaled_of_value ~decimals:d s.s_waarde))
            tl
      | None -> fault "unknown parameter %S" pname)
    case.param_tls

(* --- the program --------------------------------------------------------- *)

let translate (doc : document) (case : Case.t) : string =
  (* the ch. 7.7 v0 desugaring: initialisation-plus-computation pairs
     read as prioritized defaults here, so the defaults machinery (including
     the defeasible input) carries them — while ref/ replays the v0 forms
     natively. Gate 6's differential thereby witnesses the desugaring
     itself. Forms outside the desugaring's domain stay as they are
     and fault below like before. *)
  (* the unit-normalisation lowering (PRIORITIZED-DEFAULTS-EVOLUTIE §8;
     Normalise): source units stay on the wire, but the clingo leg
     canonicalises each declared stelsel to its finest base unit
     (multiply-only, integer-encodable) so the factor≠1 unit fences below
     translate. Run before the desugaring — the two are orthogonal
     (units/literals vs prioritized-defaults structure). *)
  let doc = Normalise.document doc in
  let doc = Ir_v0.Desugar.document doc in
  let ctx =
    { doc; case; buf = Buffer.create 4096; fresh = 0; rule_guard = []; write_scale = 1;
      rule_name = "?"; binders = [] }
  in
  (* ch. 8.4: the shared session-start Parameterset coverage check —
     data-plane validation (like the shared case loader), run before a
     program is emitted, mirroring ref/'s evaluation entry *)
  Eval.parameterset_check
    { Eval.doc; params = case.params; param_tls = case.param_tls; as_of = case.as_of;
      locals = Eval.SMap.empty };
  emit ctx "%% generated by backends/clingo from regelspraak-ir-v0 + uchar-case-v0.";
  emit ctx "#defined val/3. #defined heeft/2. #defined rol/3. #defined param/2.";
  emit ctx "#defined inconsistent/2. #defined tl/5. #defined param_tl/4. #defined ktl/4.";
  emit ctx "#defined inputval/3. #defined inputtl/5. #defined viol/2. #defined inputrol/3.";
  (* the fold family's reified lists (S15): declared so a case with an absent
     list source leaves lpres/lelem head-less without a grounding warning *)
  emit ctx "#defined lpres/1. #defined lelem/3.";
  if has_recursive_creation doc then begin
    (* the generation-depth bound (ch. 5.5's fire budget, grounded): a
       recursive chain grounds only as deep as its condition carries
       it, and a chain that reaches the budget derives a viol/2
       witness — UNSAT exactly where ref/'s pass budget faults *)
    emit ctx "#const oc_budget = 100. #defined depth/2.";
    emit ctx "viol(\"generation budget exhausted\",X) :- depth(X,oc_budget)."
  end;
  (* the fail-fast guards derive viol/2 witnesses; this single global
     integrity constraint turns any witness into UNSAT — the runner's
     UNSAT diagnosis re-runs without exactly this line to report which
     guard fired *)
  emit ctx ":- viol(_,_).";
  List.iter (translate_regel ctx) doc.resolved_regels;
  List.iter (translate_feit_creatie ctx) doc.feit_creaties;
  List.iteri (translate_object_creatie ctx) doc.object_creaties;
  List.iteri (translate_verdeling ctx) doc.verdelingen;
  (let key (l : prioritized_defaults) =
     (* kind discriminant: a name-sharing attribute and characteristic
        (WF-002) are separate targets, never one group *)
     (if l.pd_kenmerk <> None then "K:" else "A:")
     ^ norm l.pd_onderwerp ^ ":"
     ^
     match (l.pd_kenmerk, l.pd_attribuut) with
     | Some k, _ -> norm k
     | _, Some a -> norm a
     | _ -> "?"
   in
   let groups =
     List.fold_left
       (fun acc (i, l) ->
         let k = key l in
         match List.assoc_opt k acc with
         | Some _ ->
             List.map
               (fun (k', ms) -> if k' = k then (k', ms @ [ (i, l) ]) else (k', ms))
               acc
         | None -> acc @ [ (k, [ (i, l) ]) ])
       []
       (List.mapi (fun i l -> (i, l)) doc.prioritized_defaults)
   in
   List.iteri (fun gidx (_, ms) -> translate_defaults_group ctx gidx ms) groups);
  List.iteri (translate_consistentie ctx) doc.consistenties;
  List.iter (translate_uniciteit ctx) doc.uniciteiten;
  List.iteri (translate_uniciteit_unie ctx) doc.uniciteit_unies;
  translate_case ctx case;
  emit ctx "#show val/3. #show heeft/2. #show inconsistent/2. #show tl/5.";
  Buffer.contents ctx.buf
