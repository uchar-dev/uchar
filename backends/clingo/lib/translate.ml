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
     Empty propagation. The equality/inequality empty asymmetry has no
     such free encoding; those operators fault here until a corpus
     case demands them (a recorded finding).
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
(* a declared name or case label lands between double quotes in the
   generated ASP; a quote, backslash or control character would break
   the program's syntax UNFENCED (a hunted crash), so such a name
   faults with its reason instead of being emitted raw. For DECLARED
   names WF-057 (IR-SPEC ch. 2.1) now rejects the document itself —
   this fence remains as the last-line backstop, and it alone covers case
   LABELS, which are case-side and outside WF-057's reach *)
let quote s =
  String.iter
    (fun c ->
      if c = '"' || c = '\\' || Char.code c < 0x20 then
        fault "name or label %S carries a character the ASP string encoding cannot quote" s)
    s;
  "\"" ^ s ^ "\""

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
  if n > 9 then fault "a scale of 10^%d exceeds clingo's integer range" n;
  let rec go acc n = if n = 0 then acc else go (acc * 10) (n - 1) in
  go 1 n

let decimals_of_datatype = function
  | Some (Numeriek { decimalen; _ } | Percentage { decimalen; _ }) -> decimalen
  | Some Datum_in_dagen -> 0
  | dt ->
      ignore dt;
      fault "unscalable datatype in the ASP translation"

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
  | Binair _ -> None
  | Afgerond { basis; _ } | Begrensd { basis; _ } -> Option.bind basis (eff_unit ctx)
  | Temporal_read { basis; _ } -> eff_unit ctx basis
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
  match g with Jaar -> 1 | _ -> fault "temporal total: only year granularity is exercised"

(* harmonise two scaled terms for comparison *)
let harmonised (b1, t1, s1) (b2, t2, s2) =
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

let vergelijk_sym = function
  | Groter_of_gelijk | Later_of_gelijk -> ">="
  | Groter | Later -> ">"
  | Kleiner | Eerder -> "<"
  | Kleiner_of_gelijk | Eerder_of_gelijk -> "<="
  | Gelijk | Ongelijk ->
      fault
        "equality over possibly-empty operands has no free ASP encoding (the ch. 1.2d \
         asymmetry); no corpus case exercises it — recorded finding"

(* arithmetic operands must be statically numeric: ref faults date
   arithmetic under the additive/multiplicative operators, so a
   non-numeric static type must not silently compute on day numbers *)
let numeric_ok e =
  match e with
  | Some e -> (
      match Ir_v0.Checks.typ_of_expr e with
      | Some (Numeriek _ | Percentage _) | None -> ()
      | Some _ -> fault "non-numeric arithmetic operand is not translated (no corpus case)")
  | None -> ()

let rec gcd a b = if b = 0 then a else gcd b (a mod b)

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

let rec expr_term ctx ~subject ~subject_ot (e : expr) : string list * string * int =
  match e with
  | Literal { waarde; typ; _ } -> (
      let lex = req "Literal.waarde" waarde in
      match typ with
      | Some (Numeriek _ | Percentage _) | None ->
          let d = lexeme_decimals lex in
          let v = scaled_of_value ~decimals:d (Value.Num (Value.q_of_lexeme lex, Value.Unitless)) in
          ([], string_of_int v, pow10 d)
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
        ( [ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ],
          v,
          pow10 (attr_decimals ctx ~otype:ot ~attr:a) )
      end
      else begin
        (* the navigated read (ch. 5.8): one rol/3 body join per to-one
           hop — an absent link leaves the body underivable, which IS the
           leeg-on-absent-relation rule (leeg as absence, for free). The
           hop roles resolve exactly as in ref/ (shared helpers), so the
           two executors cannot drift on the walk. *)
        let env =
          { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
            as_of = None }
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
                  | Some n ->
                      [ Eval.role_by_name ~to_type:ot env ~self_type:subject_ot ~name:n ]
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
  | Functie { kind = Aantal; argumenten = [ Attribuut_ref { attribuut; typ = Some (Datatype_onbekend _); navigatie_rol = None; navigatie_pad = []; _ } ]; element_objecttype = Some el; bron_rol; filter; _ } ->
      (* the row-1 wart: the fold source names a role; count the linked
         instances. An explicit binding overrides the written name. The
         die/dat subselection joins per contributor: the filter atoms
         range over the element variable (ch. 5.8-family reading — an
         unbound filter operand excludes the contributor for free). *)
      let role =
        match bron_rol with
        | Some rb ->
            let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None } in
            role_canonical (Eval.bound_role env rb)
        | None -> req "fold role" attribuut
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
  | Functie { kind = Som; argumenten = [ Attribuut_ref { attribuut; set = true; navigatie_rol = None; navigatie_pad = []; _ } ]; element_objecttype = Some el; leegwaarde; bron_rol; filter; _ } ->
      let a = req "fold attribute" attribuut in
      (match leegwaarde with
      | Some (Literal { waarde = Some "0"; _ }) -> ()
      | Some _ -> fault "som fallback other than 0 is not translated yet"
      | None ->
          fault "som without the 0 fallback needs an explicit non-empty guard; no corpus case yet");
      (* the connecting role: the node's explicit binding when present,
         else derived the same way ref/ derives it *)
      let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None } in
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
      let contrib_body =
        Printf.sprintf "rol(%s,%s,%s)" subject (str role_name) x
        :: Printf.sprintf "val(%s,%s,%s)" x (str a) g
        :: fatoms
      in
      (* the #sum accumulation stays inside 2^31 by bounding both the
         contributors and their count: 46340 × 46340 < 2^31 *)
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      range_guard ctx ~subject
        ~body:((base :: ctx.rule_guard) @ contrib_body)
        [ (g, 46340) ];
      emit ctx
        "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",%s) :- %s, \
         46340 < #count { %s : %s }."
        subject
        (String.concat ", " (base :: ctx.rule_guard))
        x
        (String.concat ", " contrib_body);
      ( [ Printf.sprintf "%s = #sum { %s,%s : %s }" v g x (String.concat ", " contrib_body) ],
        v,
        pow10 (attr_decimals ctx ~otype:el ~attr:a) )
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
      ctx.fresh <- ctx.fresh + 1;
      let p = Printf.sprintf "cl%d" ctx.fresh in
      (* the condition's period atoms over a fresh variable pair *)
      let cond_atom =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
            Printf.sprintf "ktl(S,%s,V2,T2)" (str k)
        | Kenmerk_check { negatie = true; _ } ->
            fault
              "the negated gedurende clip (the complement walk) is not translated \
               (no corpus case exercises it)"
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
            let bscale = pow10 (attr_decimals ctx ~otype:botn ~attr:b) in
            let m = max bscale gs in
            let wl = if bscale = m then "WB" else Printf.sprintf "(WB*%d)" (m / bscale) in
            let gl = if gs = m then gt else Printf.sprintf "(%s*%d)" gt (m / gs) in
            range_guard ctx ~subject:"S"
              ~body:(Printf.sprintf "tl(S,%s,V2,T2,WB)" (str b) :: gb)
              [ ("WB", max_int31 / (m / bscale)); (gt, max_int31 / (m / gs)) ];
            emit ctx "%s_cond(S,V2,T2) :- %s."
              p
              (String.concat ", "
                 (Printf.sprintf "tl(S,%s,V2,T2,WB)" (str b)
                 :: gb
                 @ [ Printf.sprintf "%s %s %s" wl (vergelijk_sym op) gl ]));
            Printf.sprintf "%s_cond(S,V2,T2)" p
        | _ ->
            fault
              "unsupported gedurende condition shape (WF-055: an own temporal \
               characteristic or a per-segment comparison of an own temporal attribute)"
      in
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
      emit ctx
        "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",S) :- \
         %s_seg(S,V,T,W), |W| > 46340."
        p;
      emit ctx "%s_tot(S,%s) :- is_a(S,%s), %s = #sum { W*N,V,T : %s_seg(S,V,T,W), %s_wu(V,T,N) }."
        p q (str subject_ot) q p p;
      ( [ Printf.sprintf "%s_tot(%s,%s)" p subject q ],
        q,
        pow10 (attr_decimals ctx ~otype:aot ~attr:a) )
  | Functie { kind = Totaal; argumenten = [ Attribuut_ref { attribuut = Some a; objecttype = Some aot; _ } ]; _ }
    when attr_temporal ctx ~otype:aot ~attr:a ->
      (* ch. 3.6 over the asOfDate bucket: value × whole-units (= 1 for
         the year bucket); an absent timeline stays underivable = leeg *)
      refuse_attr_timeline ctx ~otype:aot ~attr:a;
      let f = total_factor (attr_granularity ctx ~otype:aot ~attr:a) in
      let v = fresh ctx "T" in
      let term = if f = 1 then v else Printf.sprintf "(%s*%d)" v f in
      ( [ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ],
        term,
        pow10 (attr_decimals ctx ~otype:aot ~attr:a) )
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
      unit_free_ok ctx links;
      unit_free_ok ctx rechts;
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
and translate_division ctx ~subject ~subject_ot ~links ~rechts ~rounding =
  let links = req "Binair.links" links and rechts = req "Binair.rechts" rechts in
  numeric_ok (Some links);
  numeric_ok (Some rechts);
  unit_free_ok ctx (Some links);
  unit_free_ok ctx (Some rechts);
  let bl, tl, sl = expr_term ctx ~subject ~subject_ot links in
  let br, tr, sr = expr_term ctx ~subject ~subject_ot rechts in
  let out_scale =
    match rounding with
    | None -> max (max sl sr) ctx.write_scale
    | Some (_, d) -> pow10 d
  in
  let g = gcd (sr * out_scale) sl in
  let knum = sr * out_scale / g and kden = sl / g in
  let numt = if knum = 1 then Printf.sprintf "(%s)" tl else Printf.sprintf "((%s)*%d)" tl knum in
  let dent = if kden = 1 then Printf.sprintf "(%s)" tr else Printf.sprintf "((%s)*%d)" tr kden in
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "q%d" ctx.fresh in
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  let guard = ctx.rule_guard in
  let lbody = String.concat ", " (base :: bl) in
  let rbody = String.concat ", " (base :: br) in
  let full = String.concat ", " ((base :: guard) @ bl @ br) in
  range_guard ctx ~subject
    ~body:((base :: guard) @ bl @ br)
    [ (tl, 536870912 / knum); (tr, 536870912 / kden) ];
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
  let e =
    match rechts with
    | Literal { waarde = Some w; _ } -> (
        let q = Value.q_of_lexeme w in
        if not (Z.equal (Q.den q) Z.one) then
          fault
            "fractional exponent %s is not translated (ref takes an exact root — the \
             ch. 2.1 OPEN; no corpus case)"
            w
        else
          match Z.to_int (Q.num q) with
          | e when e >= 0 -> e
          | _ ->
              fault
                "negative exponent %s is not translated (ref inverts the exact power — a \
                 division; no corpus case)"
                w
          | exception Z.Overflow -> fault "exponent %s is out of range" w)
    | _ ->
        fault
          "a runtime exponent is not translated (the result scale is exponent-dependent, \
           and an empty or negative runtime exponent has no integer encoding; no corpus \
           case)"
  in
  let b, t, s = expr_term ctx ~subject ~subject_ot links in
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
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      units_identical_ok ~what:"a comparison"
        (Option.bind links (eff_unit ctx))
        (Option.bind rechts (eff_unit ctx));
      let l = expr_term ctx ~subject ~subject_ot (req "links" links) in
      let r = expr_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      let b1, t1, s1 = l and b2, t2, s2 = r in
      let m = max s1 s2 in
      let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
      range_guard ctx ~subject
        ~body:((base :: b1) @ b2)
        [ (t1, max_int31 / (m / s1)); (t2, max_int31 / (m / s2)) ];
      let bindings, tl, tr = harmonised l r in
      bindings @ [ Printf.sprintf "%s %s %s" tl (vergelijk_sym op) tr ]
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
          as_of = None }
      in
      let role =
        match navigatie_binding with
        | Some rb -> Eval.bound_role env rb
        | None -> Eval.role_by_name env ~self_type:subject_ot ~name:nav
      in
      if role.rol_meervoudig then
        fault
          "the navigated check reaches the to-many role %S in a scalar condition (WF-053)"
          (Option.value role.rol_naam ~default:"?");
      let n = fresh ctx "N" in
      [ Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical role)) n;
        (if negatie then "not " else "") ^ Printf.sprintf "heeft(%s,%s)" n (str k) ]
  | Samengesteld { kwantor = Alle | De; onderdelen; _ } ->
      List.concat_map (cond_atoms ctx ~subject ~subject_ot) onderdelen
  | _ -> fault "condition kind not translated (no corpus case exercises it)"

(* --- rule forms ---------------------------------------------------------- *)

let head_body ctx head body = emit ctx "%s :- %s." head (String.concat ", " body)

(* fit a scaled term to the write target's declared scale: a coarser
   value upscales exactly (both scales are powers of ten); a finer one
   downscales under a runtime divisibility guard — ref writes any exact
   rational that fits the target's declared decimals, so a value that
   FITS must land, and a value that does not is fenced by a viol/2
   witness (never silently truncated; ref would write it exactly, a
   recorded loud asymmetry like non-exact division). [body] is the
   write rule's full body, so the fence fires only where the rule
   fires. Returns the fitted term plus guard atoms for the write rule. *)
let fitted ctx ~subject ~what ~body term scale target =
  if scale = target then (term, [])
  else if target > scale && target mod scale = 0 then begin
    range_guard ctx ~subject ~body [ (term, max_int31 / (target / scale)) ];
    (Printf.sprintf "((%s)*%d)" term (target / scale), [])
  end
  else if scale > target && scale mod target = 0 then begin
    let k = scale / target in
    emit ctx "viol(\"result of %s does not fit its declared decimals\",%s) :- %s, (%s) \\ %d != 0."
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
  match r.rr_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "validity windows are not translated yet"
  | _ ->
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
              let bindings, term, scale = expr_term ctx ~subject ~subject_ot:ot (req "waarde" r.waarde) in
              ctx.rule_guard <- [];
              ctx.write_scale <- 1;
              let body = (base :: cond) @ bindings in
              let term, fit_guards = fitted ctx ~subject ~what:a ~body term scale target_scale in
              head_body ctx
                (Printf.sprintf "val(%s,%s,%s)" subject (str a) term)
                (body @ fit_guards);
              (* ch. 7.8 standing target: the rule decides like a rung.
                 The effectiveness atom carries the SAME body as the
                 val head — condition and value bindings — so a leeg
                 evaluation derives neither, and the case input (entered
                 as inputval/3) stands by negation-as-failure. *)
              if standing then begin
                let sidx =
                  let rec find i = function
                    | [] -> fault "standing target %S not in its own domain" a
                    | k :: tl -> if k = (norm ot, norm a) then i else find (i + 1) tl
                  in
                  find 0 (Ir_v0.Desugar.standing_targets ctx.doc)
                in
                let eff = Printf.sprintf "stand%d_eff(%s)" sidx subject in
                head_body ctx eff (body @ fit_guards);
                head_body ctx
                  (Printf.sprintf "val(%s,%s,V)" subject (str a))
                  [ Printf.sprintf "inputval(%s,%s,V)" subject (str a); base; "not " ^ eff ]
              end
          | _ -> fault "rule kind not translated")
      | _ -> fault "unsupported rule target")

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
  (match oc.oc_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "object-creation validity windows are not translated yet"
  | _ -> ());
  ctx.rule_name <- Option.value oc.oc_naam ~default:"?";
  let nieuw = req "ObjectCreatie.nieuwType" oc.nieuw_type in
  let subject_ot = req "ObjectCreatie.subject" oc.oc_subject in
  let subject = "S" in
  let term = Printf.sprintf "oc%d(%s)" i subject in
  let env =
    { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None }
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
        | _ -> fault "characteristic initialisation of %S is not a boolean constant (WF-027)" m)
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
  match pair with
  | None -> ()
  | Some (_, new_role, subj_role) ->
      head_body ctx
        (Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical new_role)) term)
        body;
      head_body ctx
        (Printf.sprintf "rol(%s,%s,%s)" term (str (role_canonical subj_role)) subject)
        body

(* fact creation (IR-SPEC ch. 3.4; RUNTIME-SEMANTICS ch. 5.5): the join
   chain becomes a body join over rol/3, and the head derives the
   created relation's rol/3 facts — both ends, exactly the two
   directions ref/'s writer lands; set semantics carries the
   idempotence (a target reached along two paths is one atom). Only the
   symbolic extension form executes; a bean-field-only v0 record faults
   (as in ref/). *)
let translate_feit_creatie ctx (fc : resolved_feit_creatie) =
  (match fc.fc_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "fact-creation validity windows are not translated yet"
  | _ -> ());
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
    { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None }
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

(* the ladder group (RUNTIME-SEMANTICS ch. 7, IR-SPEC ch. 6.1): the
   ch. 7.5 decides_m lowering per member — decides predicates per rung,
   an outcome predicate per member (highest deciding rung wins via
   `not decides_m` guards; on an attribute ladder the consequence
   bindings sit inside decides, so a leeg consequence is underivable —
   the gevuld gate for free) — and the ch. 7.2 group layer on top: a
   pairwise conflict rule, outcome heads and the defeasible input both
   guarded on the conflict, and the conflict deposited as
   inconsistent/2 keyed by the target form. A singleton group elides
   the conflict machinery. Sound NAF because WF-051 makes displacement
   edges negative (the verifier remains the guard that a document is
   clingo-suitable). *)
let translate_ladder_group ctx gidx (members : (int * default_ladder) list) =
  let _, l0 = List.hd members in
  List.iter
    (fun ((_, l) : int * default_ladder) ->
      match l.dl_geldigheid with
      | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
          fault "ladder validity windows are not translated yet"
      | _ -> ())
    members;
  let subject = "S" in
  let base = Printf.sprintf "is_a(%s,%s)" subject (str l0.dl_onderwerp) in
  let d i n = Printf.sprintf "dl%d_decides_%d(%s)" i n subject in
  let conflict = Printf.sprintf "grp%d_conflict(%s)" gidx subject in
  let singleton = List.length members = 1 in
  let conflict_guard = if singleton then [] else [ "not " ^ conflict ] in
  let target_form tn = l0.dl_onderwerp ^ "." ^ tn in
  match (l0.dl_kenmerk, l0.dl_attribuut) with
  | Some k, None ->
      let dec i = Printf.sprintf "dl%d_dec" i in
      List.iter
        (fun ((i, l) : int * default_ladder) ->
          List.iteri
            (fun n (r : rung) ->
              let atoms =
                cond_atoms ctx ~subject ~subject_ot:l.dl_onderwerp
                  (req "rung justification" r.rung_voorwaarde)
              in
              head_body ctx (d i n) (base :: atoms);
              let b = if req "rung besluit" r.rung_besluit then 1 else 0 in
              let displaced = List.init n (fun m -> "not " ^ d i m) in
              head_body ctx
                (Printf.sprintf "%s(%s,%d)" (dec i) subject b)
                ((d i n :: displaced) @ [ base ]))
            l.rungs)
        members;
      if not singleton then begin
        List.iter
          (fun ((i, _) : int * default_ladder) ->
            List.iter
              (fun ((j, _) : int * default_ladder) ->
                if i < j then
                  head_body ctx conflict
                    [ Printf.sprintf "%s(%s,X)" (dec i) subject;
                      Printf.sprintf "%s(%s,Y)" (dec j) subject; "X != Y" ])
              members)
          members;
        head_body ctx
          (Printf.sprintf "inconsistent(%s,%s)" (str (target_form k)) subject)
          [ conflict ]
      end;
      List.iter
        (fun ((i, l) : int * default_ladder) ->
          (* only members with an attribute-true rung can derive the
             characteristic — eliding the head keeps clingo info-clean *)
          if List.exists (fun (r : rung) -> req "rung besluit" r.rung_besluit) l.rungs then
            head_body ctx
              (Printf.sprintf "heeft(%s,%s)" subject (str k))
              ((Printf.sprintf "%s(%s,1)" (dec i) subject :: conflict_guard) @ [ base ]))
        members
  | None, Some a ->
      let out i = Printf.sprintf "dl%d_out" i in
      let target_scale = pow10 (attr_decimals ctx ~otype:l0.dl_onderwerp ~attr:a) in
      let has_input = Printf.sprintf "grp%d_has_input(%s)" gidx subject in
      head_body ctx has_input [ base; Printf.sprintf "inputval(%s,%s,_)" subject (str a) ];
      List.iter
        (fun ((i, l) : int * default_ladder) ->
          let rung_cond (r : rung) =
            if r.rung_initialisatie then [ "not " ^ has_input ]
            else
              cond_atoms ctx ~subject ~subject_ot:l.dl_onderwerp
                (req "rung justification" r.rung_voorwaarde)
          in
          List.iteri
            (fun n (r : rung) ->
              let conseq = req "rung consequentie" r.rung_consequentie in
              let bindings, _, scale =
                expr_term ctx ~subject ~subject_ot:l.dl_onderwerp conseq
              in
              if scale <> target_scale then
                fault "rung value scale %d does not match target scale %d for %S" scale
                  target_scale a;
              head_body ctx (d i n) ((base :: rung_cond r) @ bindings);
              let bindings2, term2, _ =
                expr_term ctx ~subject ~subject_ot:l.dl_onderwerp conseq
              in
              let displaced = List.init n (fun m -> "not " ^ d i m) in
              head_body ctx
                (Printf.sprintf "%s(%s,%s)" (out i) subject term2)
                ((base :: rung_cond r) @ bindings2 @ displaced))
            l.rungs)
        members;
      if not singleton then begin
        List.iter
          (fun ((i, _) : int * default_ladder) ->
            List.iter
              (fun ((j, _) : int * default_ladder) ->
                if i < j then
                  head_body ctx conflict
                    [ Printf.sprintf "%s(%s,X)" (out i) subject;
                      Printf.sprintf "%s(%s,Y)" (out j) subject; "X != Y" ])
              members)
          members;
        head_body ctx
          (Printf.sprintf "inconsistent(%s,%s)" (str (target_form a)) subject)
          [ conflict ]
      end;
      let eff = Printf.sprintf "grp%d_eff(%s)" gidx subject in
      List.iter
        (fun ((i, _) : int * default_ladder) ->
          head_body ctx
            (Printf.sprintf "val(%s,%s,V)" subject (str a))
            ((Printf.sprintf "%s(%s,V)" (out i) subject :: conflict_guard) @ [ base ]);
          head_body ctx eff
            ((Printf.sprintf "%s(%s,_)" (out i) subject :: conflict_guard) @ [ base ]))
        members;
      (* the defeasible input: the case value stands unless the group
         effectively writes (a conflicting group leaves it standing) *)
      head_body ctx
        (Printf.sprintf "val(%s,%s,V)" subject (str a))
        [ Printf.sprintf "inputval(%s,%s,V)" subject (str a); base; "not " ^ eff ]
  | _ -> fault "a ladder targets exactly one of a characteristic or an attribute"

(* allocation, proportional with cap/floor/remainder (RUNTIME-SEMANTICS
   ch. 4.2), in integer arithmetic. Scale bookkeeping: raw = pot×w/Σw
   carries the pot's scale (the weight scale cancels); the floor at the
   declared decimals must therefore coincide with the pot/target scale. *)
let translate_verdeling ctx i (v : resolved_verdeling) =
  if v.vd_methode <> Some Naar_rato then fault "only the proportional allocation is translated yet";
  if v.vd_voorwaarde <> None then fault "conditional allocation not translated yet";
  let bt = req "bronType" v.bron_type and dt = req "doelType" v.doel_type in
  let ba = req "bronAttribuut" v.bron_attribuut and da = req "doelAttribuut" v.doel_attribuut in
  (* the allocation-target input fence (the latent superset gap of the
     PHASE4-DEFAULTS verification round): a case value on the target
     would union with the allocation's own head into a superset model —
     ref overwrites it, so the split would be silent wherever no
     expectation pins the attribute. Refuse loudly, like the ch. 6d
     computed-target fence. *)
  if case_supplies ctx ~otype:dt ~attr:da then
    fault
      "the case supplies input for allocation target %S: ref (like v0) overwrites it, which \
       has no monotone encoding here (no corpus case)"
      da;
  let rato = req "ratoAttribuut (proportional)" v.rato_attribuut in
  let name = Option.value v.vd_naam ~default:"verdeling" in
  let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None } in
  let role =
    role_canonical
      (match v.verbindende_rol with
      | Some rb -> Eval.bound_role env rb
      | None -> Eval.connecting_role env ~from_type:bt ~to_type:dt)
  in
  let pot_scale = pow10 (attr_decimals ctx ~otype:bt ~attr:ba) in
  let target_scale = pow10 (attr_decimals ctx ~otype:dt ~attr:da) in
  let floor_scale = pow10 (Option.value v.afronding_decimalen ~default:0) in
  let cap_scale =
    match v.maximum_attribuut with
    | Some cap -> pow10 (attr_decimals ctx ~otype:dt ~attr:cap)
    | None -> target_scale
  in
  let rest_scale =
    match v.restant_attribuut with
    | Some ra -> pow10 (attr_decimals ctx ~otype:bt ~attr:ra)
    | None -> pot_scale
  in
  if
    pot_scale <> target_scale || floor_scale <> target_scale || cap_scale <> target_scale
    || rest_scale <> pot_scale
  then
    fault
      "allocation scale mismatch (pot %d, target %d, floor %d, cap %d, rest %d): only the \
       aligned form is translated"
      pot_scale target_scale floor_scale cap_scale rest_scale;
  let p = Printf.sprintf "vd%d_" i in
  (* eligible recipients: filled weight; the weightless are excluded and
     surface on the channel (ch. 4.4) *)
  emit ctx "%sgewicht(X,W) :- rol(B,%s,X), is_a(B,%s), val(X,%s,W)." p (str role) (str bt)
    (str rato);
  (* fail-fast guards, mirrored on ref/'s faults: integer division is
     floor only on non-negative operands, and an empty pot is the
     ch. 4.4 v0 hole ref/ faults on — viol/2 witnesses plus the global
     integrity constraint make clingo fail identically instead of
     silently diverging, and let the runner name the fired guard *)
  emit ctx "viol(\"allocation: negative pot\",B) :- is_a(B,%s), val(B,%s,A), A < 0." (str bt)
    (str ba);
  emit ctx "viol(\"allocation: negative weight\",X) :- %sgewicht(X,W), W < 0." p;
  (* the wrap fence: pot and weights ≤ 46340 and at most 46340
     recipients keep the weight sum, the pot×weight product and the
     share sum inside 2^31 (shares never exceed the pot: W ≤ S under
     the non-negative-weight guard, and a cap only lowers them) *)
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",X) :- \
     %sgewicht(X,W), |W| > 46340."
    p;
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",B) :- \
     is_a(B,%s), val(B,%s,A), |A| > 46340."
    (str bt) (str ba);
  emit ctx
    "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",B) :- \
     is_a(B,%s), 46340 < #count { X : rol(B,%s,X) }."
    (str bt) (str role);
  emit ctx "%sheeft_pot(B) :- is_a(B,%s), val(B,%s,_)." p (str bt) (str ba);
  emit ctx "viol(\"allocation: empty pot\",B) :- is_a(B,%s), rol(B,%s,_), not %sheeft_pot(B)."
    (str bt) (str role) p;
  emit ctx "inconsistent(%s,X) :- rol(B,%s,X), is_a(B,%s), not %sheeft_gewicht(X)." (str name)
    (str role) (str bt) p;
  emit ctx "%sheeft_gewicht(X) :- %sgewicht(X,_)." p p;
  emit ctx "%swsum(B,S) :- is_a(B,%s), S = #sum { W,X : rol(B,%s,X), %sgewicht(X,W) }, S != 0." p
    (str bt) (str role) p;
  emit ctx "%sraw(X,(A*W)/S) :- rol(B,%s,X), %sgewicht(X,W), %swsum(B,S), val(B,%s,A)." p
    (str role) p p (str ba);
  (match v.maximum_attribuut with
  | Some cap ->
      (* cap before floor; an empty or negative cap degrades to uncapped
         + a channel fact (ch. 4.3) *)
      emit ctx "%scap(X,C) :- %sraw(X,_), val(X,%s,C), C >= 0." p p (str cap);
      emit ctx "inconsistent(%s,X) :- %sraw(X,_), val(X,%s,C), C < 0." (str name) p (str cap);
      emit ctx "inconsistent(%s,X) :- %sraw(X,_), not %sheeft_cap(X)." (str name) p p;
      emit ctx "%sheeft_cap(X) :- val(X,%s,C), C >= 0, %sraw(X,_)." p (str cap) p;
      emit ctx "val(X,%s,R) :- %sraw(X,R), %scap(X,C), R <= C." (str da) p p;
      emit ctx "val(X,%s,C) :- %sraw(X,R), %scap(X,C), R > C." (str da) p p;
      emit ctx "val(X,%s,R) :- %sraw(X,R), not %sheeft_cap(X)." (str da) p p
  | None -> emit ctx "val(X,%s,R) :- %sraw(X,R)." (str da) p);
  (match v.restant_attribuut with
  | Some ra ->
      (* shares are ≤ pot ≤ 46340 by construction; this guard also
         covers a share written by anything other than this allocation *)
      emit ctx
        "viol(\"arithmetic range exceeded (clingo grounds in machine integers)\",X) :- \
         rol(B,%s,X), is_a(B,%s), val(X,%s,Sh), |Sh| > 46340."
        (str role) (str bt) (str da);
      emit ctx "val(B,%s,A-T) :- is_a(B,%s), val(B,%s,A), %swsum(B,_), T = #sum { Sh,X : rol(B,%s,X), val(X,%s,Sh) }."
        (str ra) (str bt) (str ba) p (str role) (str da)
  | None -> ())

let translate_consistentie ctx i (c : resolved_consistentie) =
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
            as_of = None }
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

(* --- case facts ---------------------------------------------------------- *)

let translate_case ctx (case : Case.t) =
  let doc = ctx.doc in
  (* attribute-ladder targets — and the ch. 7.8 standing targets (the
     bare scalar computing rule) — take their input values defeasibly
     (inputval/3), standing unless the ladder/rule decides *)
  let standing = Ir_v0.Desugar.standing_targets doc in
  let ladder_target otype aname =
    List.exists
      (fun (l : default_ladder) ->
        match l.dl_attribuut with
        | Some a -> norm l.dl_onderwerp = norm otype && norm a = norm aname
        | None -> false)
      doc.default_ladders
    || List.mem (norm otype, norm aname) standing
  in
  Eval.SMap.iter
    (fun label (i : Uchar_ref.World.instance) ->
      emit ctx "is_a(%s,%s)." (str_raw label) (str i.otype);
      (* generation depth 0 for every input instance — the recursive
         creations' grounding bound counts from here *)
      if has_recursive_creation doc then emit ctx "depth(%s,0)." (str_raw label);
      Eval.SMap.iter
        (fun aname v ->
          let d = attr_decimals ctx ~otype:i.otype ~attr:aname in
          let pred = if ladder_target i.otype aname then "inputval" else "val" in
          emit ctx "%s(%s,%s,%d)." pred (str_raw label) (str aname)
            (scaled_of_value ~decimals:d v))
        i.attrs;
      List.iter (fun k -> emit ctx "heeft(%s,%s)." (str_raw label) (str k)) i.chars;
      Eval.SMap.iter
        (fun aname (tl : Uchar_ref.Timeline.t) ->
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
     read as ladders here, so the ladder machinery (including the
     defeasible input) carries them — while ref/ replays the v0 forms
     natively. Gate 6's differential thereby witnesses the desugaring
     itself. Forms outside the desugaring's domain stay as they are
     and fault below like before. *)
  let doc = Ir_v0.Desugar.document doc in
  let ctx =
    { doc; case; buf = Buffer.create 4096; fresh = 0; rule_guard = []; write_scale = 1;
      rule_name = "?" }
  in
  (* ch. 8.4: the shared session-start Parameterset coverage check —
     data-plane validation (like the shared case loader), run before a
     program is emitted, mirroring ref/'s evaluation entry *)
  Eval.parameterset_check
    { Eval.doc; params = case.params; param_tls = case.param_tls; as_of = case.as_of };
  emit ctx "%% generated by backends/clingo from regelspraak-ir-v0 + uchar-case-v0.";
  emit ctx "#defined val/3. #defined heeft/2. #defined rol/3. #defined param/2.";
  emit ctx "#defined inconsistent/2. #defined tl/5. #defined param_tl/4. #defined ktl/4.";
  emit ctx "#defined inputval/3. #defined inputtl/5. #defined viol/2. #defined inputrol/3.";
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
  (let key (l : default_ladder) =
     (* kind discriminant: a name-sharing attribute and characteristic
        (WF-002) are separate targets, never one group *)
     (if l.dl_kenmerk <> None then "K:" else "A:")
     ^ norm l.dl_onderwerp ^ ":"
     ^
     match (l.dl_kenmerk, l.dl_attribuut) with
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
       (List.mapi (fun i l -> (i, l)) doc.default_ladders)
   in
   List.iteri (fun gidx (_, ms) -> translate_ladder_group ctx gidx ms) groups);
  List.iteri (translate_consistentie ctx) doc.consistenties;
  List.iter (translate_uniciteit ctx) doc.uniciteiten;
  List.iteri (translate_uniciteit_unie ctx) doc.uniciteit_unies;
  translate_case ctx case;
  emit ctx "#show val/3. #show heeft/2. #show inconsistent/2. #show tl/5.";
  Buffer.contents ctx.buf
