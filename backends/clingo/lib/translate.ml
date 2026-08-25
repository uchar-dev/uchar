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

(* the ASP scale of an accumulator/result datatype: numeric datatypes ride
   their declared decimals; a sum-typed accumulator (the reduce lowering's
   Optional, ch. 10.4/§3.5) is a ground constructor term carrying no numeric
   scale (0), exactly like a Construct value in [expr_term] *)
let value_scale = function
  | Some (Sum_type _ | Struct_type _) -> 0
  | t -> pow10 (decimals_of_datatype t)

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
      (* scalar conditions take the eqN-helper route in cond_atoms; what
         remains on this symbol path is the per-segment clip comparison,
         whose operands are timeline segments — the empty corner there
         is a different shape (a gap, not an empty operand) *)
      fault
        "equality in a per-segment clip comparison is not translated (no corpus case \
         exercises it)"

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
   closed-body division — one reduction. *)
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
            let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty } in
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
        | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
            fun v t -> Printf.sprintf "ktl(S,%s,%s,%s)" (str k) v t
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
        "a Construct payload other than a literal or an own attribute read is not translated (a \
         Match or composite payload has no corpus case)"
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
  | Let { binder; value; body; _ } ->
      (* ch. 10.6: the strict Let as a bound intermediate (S-section (a)).
         The value is translated to (atoms, term, scale); the binder resolves
         to that term, and the value's atoms are threaded into [rule_guard] so
         any helper emitted while translating the body binds the value's
         variables. A leeg value binds no atom, so every rule carrying the body
         is underivable — ref's strict-Let leeg and clingo's underivability
         coincide by construction. Returned atoms carry both the value's and
         the body's, so the caller's rule body binds every variable it uses. *)
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
      let init_atoms, init_term, init_scale =
        closed_term ctx ~guard_body:[ Printf.sprintf "lpres(%s)" lterm ] ~witness:lterm init
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
         term); Gelijk tests ==, Ongelijk tests != — never `not gelijk`: a leeg
         operand binds no term, so BOTH directions fail (Tabel 38's non-numeric
         branch, the case3 dubbel-én-afwijkend-afwezig corner). Nominal identity
         is carried by the functor itself (= the declaring domain); WF-072
         precludes a cross-domain comparison statically. *)
      let ll, tl = struct_term ctx ~subject ~subject_ot (req "links" links) in
      let rr, tr = struct_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      ll @ rr @ [ Printf.sprintf "%s %s %s" tl (if op = Gelijk then "==" else "!=") tr ]
  | Vergelijking { op = (Gelijk | Ongelijk) as op; links; rechts; periode = None; _ }
    when is_sum_operand links || is_sum_operand rechts ->
      (* ch. 9.3: sum-typed equality is POSITIVE ground-term unification.
         Both operand terms bind (an own attribute read via its val atom, a
         Construct as a bare ground term); Gelijk tests T1 == T2, Ongelijk
         tests T1 != T2. Ongelijk is NEVER `not gelijk`: a leeg operand must
         make BOTH directions false (Tabel 38, the non-numeric branch), and a
         val atom that does not bind fails the positive join in both — the
         NAF complement would instead make Ongelijk TRUE on leeg (the S5
         non-complement warning, honoured here). *)
      let ll, tl = sum_term ctx ~subject ~subject_ot (req "links" links) in
      let rr, tr = sum_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      ll @ rr @ [ Printf.sprintf "%s %s %s" tl (if op = Gelijk then "==" else "!=") tr ]
  | Vergelijking { op = (Gelijk | Ongelijk) as op; links; rechts; periode = None; _ } ->
      (* the ch. 1.2d equality asymmetry (arith.compare_op): gelijk-aan
         is TRUE on two empty operands and FALSE on exactly one;
         ongelijk-aan is its exact complement. Encoded through a
         per-comparison helper: eqN(S) holds iff both operand bodies
         bind and the harmonised values match, or neither binds.
         Ongelijk is negation-as-failure over the helper — sound for
         the same reason coalesce0's definedness negation is: the
         helper reads only the operand bodies. *)
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
          as_of = None; locals = Eval.SMap.empty }
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
   functor. A non-literal payload would need underivability propagation and
   has no corpus case. *)
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
        "a Construct payload other than a literal is not translated (a bound payload would need \
         underivability propagation; no corpus case)"

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
   any viol/2 the arm body emits carries the scrutinee atom that binds it). *)
and translate_match ctx ~subject ~subject_ot ~typ ~scrutinee ~arms =
  let constructoren = sum_constructors (Ir_v0.Checks.typ_of_expr scrutinee) in
  ctx.fresh <- ctx.fresh + 1;
  let p = Printf.sprintf "match%d" ctx.fresh in
  let base = Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) in
  let out_scale = value_scale typ in
  (* the scrutinee's pattern carrier: an own-subject sum-typed attribute read
     carries the constructor pattern in its val atom (functor unification); any
     other sum-valued scrutinee (a Fold — the reduce lowering's extraction,
     ch. 10.4) is evaluated once to a bound term matched by builtin equality. *)
  let arm_scrut =
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
      let f = ctor_functor ctor in
      let pattern, binding =
        match (ctor_payload_datatype constructoren ctor, binder) with
        | None, None -> (f, None)
        | Some d, Some b ->
            let pv = fresh ctx "P" in
            (Printf.sprintf "%s(%s)" f pv, Some (norm b, ([], pv, pow10 (decimals_of_datatype (Some d)))))
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
      (* a closed-body binder is recurrence-bound (its atoms are empty); the
         surrounding lelem/pattern join grounds the ASP variable *)
      | Some (_, var, scale) -> ([], var, scale)
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
      let m = max sl sr in
      let sc t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
      let tl' = sc tl sl and tr' = sc tr sr in
      range_guard ctx ~subject:witness
        ~body:(guard_body @ bl @ br)
        [ (tl', 1073741823 / (m / sl)); (tr', 1073741823 / (m / sr)) ];
      (bl @ br, Printf.sprintf "(%s%s%s)" tl' (if op = Plus then "+" else "-") tr', m)
  | Construct { typ; constructor_; argument; _ } ->
      ([], construct_ground_term ctx typ constructor_ argument, 0)
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
      unit_free_ok ctx links;
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

(* is [basis] a money × rate product (possibly under strict Lets)? — a
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
   term wraps silently. Strict Lets around the product are peeled with their
   binders in scope, exactly as ref evaluates the Let body. *)
and translate_money_mult ctx ~subject ~subject_ot ~basis : string list * string * int =
  let saved_b = ctx.binders and saved_g = ctx.rule_guard in
  let rec peel = function
    | Let { binder; value; body; _ } ->
        let va, vt, vs = expr_term ctx ~subject ~subject_ot value in
        ctx.binders <- (norm binder, let_binder ctx ~subject ~subject_ot (va, vt, vs)) :: ctx.binders;
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
      [ (tl, 536870912 / knum); (tr, 536870912 / kden) ],
      true )
  in
  let numt, dent, range_bounds, runtime =
    match const_div with
    | Some c ->
        let knum, kden_c = const_div_reduce ~sl ~sr ~out_scale ~c in
        (* a folded denominator past the fence would feed 2·dent unguarded past
           2^31 (S18); drop back to the two-factor form, whose divisor-term
           range_guard fires the loud UNSAT — a runtime divisor is impossible
           here (const_div is Some), so this branch never sets [runtime] *)
        if kden_c > 536870912 then two_factor ()
        else (numt_of knum, Printf.sprintf "(%d)" kden_c, [ (tl, 536870912 / knum) ], false)
    | None -> two_factor ()
  in
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

(* the rule VALUE position (§6, guarded consequences): a Let binds an
   intermediate whose value atoms are shared across the branches; an
   IfThenElse (now reachable through Let bodies, WF-069) lowers to one guarded
   outcome per branch — the condition a builtin over bound/parameter/literal
   operands (WF-077) and its exact complement, NAF-free, so the leeg barrier
   and clingo's underivability coincide by construction. Any other value is a
   single branch via [expr_term]. Returns (guard atoms, result term, scale)
   per branch, mirroring [body_branches] at value position. *)
let rec value_branches ctx ~subject ~subject_ot (e : expr) : (string list * string * int) list =
  match e with
  | Let { binder; value; body; _ } ->
      let va, vt, vs = expr_term ctx ~subject ~subject_ot value in
      let saved_b = ctx.binders and saved_g = ctx.rule_guard in
      ctx.binders <- (norm binder, let_binder ctx ~subject ~subject_ot (va, vt, vs)) :: ctx.binders;
      ctx.rule_guard <- ctx.rule_guard @ va;
      let branches = value_branches ctx ~subject ~subject_ot body in
      ctx.binders <- saved_b;
      ctx.rule_guard <- saved_g;
      List.map (fun (g, t, s) -> (va @ g, t, s)) branches
  | If_then_else { condition; then_; else_; _ } ->
      let catoms, pos, neg = value_condition ctx ~subject ~subject_ot condition in
      let arm extra branches = List.map (fun (g, t, s) -> (catoms @ (extra :: g), t, s)) branches in
      arm pos (value_branches ctx ~subject ~subject_ot then_)
      @ arm neg (value_branches ctx ~subject ~subject_ot else_)
  | _ ->
      let a, t, s = expr_term ctx ~subject ~subject_ot e in
      [ (a, t, s) ]

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
              (* ch. 7.8 standing target: the rule decides like a rung. The
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
                (fun (bindings, term, scale) ->
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

(* the ladder group (RUNTIME-SEMANTICS ch. 7.1/7.2/7.9, IR-SPEC ch. 6.1;
   ASP-TRANSLATION S17): the ch. 7.5 decides_m lowering generalised into a
   recursive rose tree (ch. 7.9). A CHAIN — a rung list, a top-level member or a sub-ladder —
   lowers to an outcome predicate `T_out(S,V)` (the highest settling rung's
   decision: the 0/1 besluit on a characteristic ladder, the scaled
   consequence term on an attribute ladder) and a conflict flag `T_cfl(S)`
   (the highest settling rung's sub-group conflict, propagated upward). A
   GROUP — a rung's exception sub-group, or the root member set —
   reconciles its member chains exactly as ch. 7.2 reconciles the root:
   differing decisions derive a path-keyed inconsistent/2 and suppress the
   clean decision, and a member conflict propagates before differing
   decisions are read (conflict-first). A rung SETTLES the walk when it
   decides or conflicts; higher settling rungs displace lower ones by
   `not T_stl_m` — the flat encoding's displacement, now carrying conflicts
   too (a higher deciding rung displaces a lower rung's sub-group conflict,
   vraag 5-A). A differing sub-group therefore propagates one conflict atom
   to the root, which suppresses EVERY write on the target (the `not
   grpG_conflict` guard) and leaves the case input standing, while its own
   group deposits the sole §9.5 fact under its rung-index path key. Every
   negation points strictly down the tree or at strictly-higher rungs
   (disjoint subtrees) — no conflict atom depends on the write it guards,
   so the program stays stratified and clingo keeps its one stable model.
   Sound NAF because WF-051 folds every descendant read into a negative
   edge toward the root target (the verifier remains the clingo-suitability
   guard). *)
let path_key target_form = function
  | [] -> target_form
  | path -> target_form ^ "/" ^ String.concat "/" (List.map string_of_int path)

let rec translate_chain ctx ~is_char ~subject ~subject_ot ~base ~path ~root
    ~target_form ~target_scale ~has_input ~outer_displaced (rungs : rung list) :
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
  let own_just (r : rung) =
    if r.rung_initialisatie then (
      if not root then
        fault "an initialisation rung inside a sub-ladder is ill-formed (WF-075)";
      match has_input with
      | Some hi -> [ Printf.sprintf "not %s(%s)" hi subject ]
      | None -> fault "initialisation rung on a characteristic ladder")
    else cond_atoms ctx ~subject ~subject_ot (req "rung justification" r.rung_voorwaarde)
  in
  (* a rung's OWN decision (a plain rung, an init rung, or a form-a
     fall-back): on the attribute arm the consequence bindings sit in the
     body, so a leeg consequence leaves the decision underivable — the
     fall-through gate for free; [extra] carries the `not U_any` guard that
     restricts a form-a fall-back to an undecided sub-group (ch. 7.9) *)
  let emit_own_dec n (r : rung) ~extra =
    if is_char then
      let b = if req "rung besluit" r.rung_besluit then 1 else 0 in
      head_body ctx (Printf.sprintf "%s(%s,%d)" (dec n) subject b)
        ((base :: own_just r) @ extra)
    else
      let bindings, term, scale =
        expr_term ctx ~subject ~subject_ot (req "rung consequentie" r.rung_consequentie)
      in
      if scale <> target_scale then
        fault "rung value scale %d does not match target scale %d" scale target_scale;
      head_body ctx (Printf.sprintf "%s(%s,%s)" (dec n) subject term)
        ((base :: own_just r) @ bindings @ extra)
  in
  List.iteri
    (fun n (r : rung) ->
      (match r.rung_excepties with
      | None -> emit_own_dec n r ~extra:[]
      | Some sg ->
          (* the displacement wait threaded into the sub-group: every
             strictly-higher (lower-index) rung of this chain, accumulated
             with the enclosing rungs' waits, so the sub-group's own §9.5
             inconsistent deposit is suppressed when a higher rung settles —
             exactly the guard the chain outcome below puts on [out]/[cfl] *)
          let rung_displaced =
            outer_displaced
            @ List.init n (fun m -> Printf.sprintf "not %s(%s)" (stl m) subject)
          in
          let u_out, u_cfl, u_any =
            translate_group ctx ~is_char ~subject ~subject_ot ~base
              ~group_path:(path @ [ n ]) ~target_form ~target_scale ~has_input
              ~outer_displaced:rung_displaced sg
          in
          (* the sub-group goes first: adopt its clean decision, propagate its
             conflict; the own justification/consequence is the fall-back only
             when the sub-group decides nothing (ch. 7.9) *)
          head_body ctx (Printf.sprintf "%s(%s,V)" (dec n) subject)
            [ Printf.sprintf "%s(%s,V)" u_out subject ];
          head_body ctx (Printf.sprintf "%s(%s)" (conf n) subject)
            [ Printf.sprintf "%s(%s)" u_cfl subject ];
          let has_own =
            if is_char then r.rung_besluit <> None else r.rung_consequentie <> None
          in
          if has_own then
            emit_own_dec n r ~extra:[ Printf.sprintf "not %s(%s)" u_any subject ]);
      head_body ctx (Printf.sprintf "%s(%s)" (stl n) subject)
        [ Printf.sprintf "%s(%s,_)" (dec n) subject ];
      if r.rung_excepties <> None then
        head_body ctx (Printf.sprintf "%s(%s)" (stl n) subject)
          [ Printf.sprintf "%s(%s)" (conf n) subject ])
    rungs;
  (* the chain outcome: the highest settling rung wins — lower rungs are
     displaced by `not stl_m` for every strictly-higher m *)
  List.iteri
    (fun n (r : rung) ->
      let displaced = List.init n (fun m -> Printf.sprintf "not %s(%s)" (stl m) subject) in
      head_body ctx (Printf.sprintf "%s(%s,V)" out subject)
        (Printf.sprintf "%s(%s,V)" (dec n) subject :: displaced);
      if r.rung_excepties <> None then
        head_body ctx (Printf.sprintf "%s(%s)" cfl subject)
          (Printf.sprintf "%s(%s)" (conf n) subject :: displaced))
    rungs;
  (out, cfl)

and translate_group ctx ~is_char ~subject ~subject_ot ~base ~group_path
    ~target_form ~target_scale ~has_input ~outer_displaced (sg : sub_group) :
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
      (fun (sl : sub_ladder) ->
        translate_chain ctx ~is_char ~subject ~subject_ot ~base ~path:group_path
          ~root:false ~target_form ~target_scale ~has_input
          ~outer_displaced sl.sl_rungs)
      sg.sg_leden
  in
  let no_member_cfl =
    List.map (fun (_, mc) -> Printf.sprintf "not %s(%s)" mc subject) members
  in
  (* differing member decisions are a conflict at THIS group, keyed by the
     group's rung-index path — unless a member itself conflicts, whose deeper
     key propagates instead (conflict-first, ch. 7.9) *)
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
     gate of a form-a parent rung *)
  List.iter
    (fun (om, _) ->
      head_body ctx (Printf.sprintf "%s(%s,V)" out subject)
        [ Printf.sprintf "%s(%s,V)" om subject; Printf.sprintf "not %s(%s)" cfl subject ])
    members;
  head_body ctx (Printf.sprintf "%s(%s)" any subject)
    [ Printf.sprintf "%s(%s,_)" out subject ];
  head_body ctx (Printf.sprintf "%s(%s)" any subject)
    [ Printf.sprintf "%s(%s)" cfl subject ];
  (* the sub-group's §9.5 deposit carries the SAME displacement wait the
     enclosing chain puts on its outcome (ch. 7.9): a strictly-higher settling
     rung displaces this whole sub-group, path-keyed fact and all, so the
     deposit must not fire on the bare [diff] atom alone (root members do not
     displace each other, so the bare-keyed root deposit stays unguarded) *)
  head_body ctx
    (Printf.sprintf "inconsistent(%s,%s)" (str (path_key target_form group_path)) subject)
    (Printf.sprintf "%s(%s)" diff subject :: outer_displaced);
  (out, cfl, any)

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
  let is_char, target_name =
    match (l0.dl_kenmerk, l0.dl_attribuut) with
    | Some k, None -> (true, k)
    | None, Some a -> (false, a)
    | _ -> fault "a ladder targets exactly one of a characteristic or an attribute"
  in
  let target_form = l0.dl_onderwerp ^ "." ^ target_name in
  (* the attribute arm's input scaffolding: the target scale, and the input
     predicate the initialisation rung and the defeasible-input rule read *)
  let target_scale, has_input =
    if is_char then (0, None)
    else begin
      let hi = Printf.sprintf "grp%d_has_input" gidx in
      head_body ctx (Printf.sprintf "%s(%s)" hi subject)
        [ base; Printf.sprintf "inputval(%s,%s,_)" subject (str target_name) ];
      (pow10 (attr_decimals ctx ~otype:l0.dl_onderwerp ~attr:target_name), Some hi)
    end
  in
  (* each top-level member is a chain reconciled at the root path [] *)
  let member_info =
    List.map
      (fun ((_, l) : int * default_ladder) ->
        let out, cfl =
          translate_chain ctx ~is_char ~subject ~subject_ot:l.dl_onderwerp ~base ~path:[]
            ~root:true ~target_form ~target_scale ~has_input ~outer_displaced:[] l.rungs
        in
        (out, cfl, List.exists (fun (r : rung) -> r.rung_excepties <> None) l.rungs))
      members
  in
  (* the root group (ch. 7.2): differing member decisions and any propagated
     sub-group conflict block every write; a differing ROOT decision deposits
     the bare-keyed §9.5 fact. A conflict is possible only with ≥2 members or
     nested excepties — otherwise the guard is absent (the flat singleton). *)
  let nested = List.exists (fun (_, _, c) -> c) member_info in
  let conflict_possible = List.length members >= 2 || nested in
  let root_cfl = Printf.sprintf "grp%d_conflict(%s)" gidx subject in
  let root_diff = Printf.sprintf "grp%d_diff(%s)" gidx subject in
  if conflict_possible then begin
    (* propagate a member (sub-group) conflict up to the root *)
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
  let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None; locals = Eval.SMap.empty } in
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
          match v with
          | Value.Bool b ->
              (* a boolean attribute is a characteristic in v0 (booleans are
                 characteristics): true → heeft/2, false → absence
                 (closed-world), the [bool_atoms] read-shape on the case side *)
              if b then emit ctx "heeft(%s,%s)." (str_raw label) (str aname)
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
          | _ ->
              let d = attr_decimals ctx ~otype:i.otype ~attr:aname in
              let pred = if ladder_target i.otype aname then "inputval" else "val" in
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
     read as ladders here, so the ladder machinery (including the
     defeasible input) carries them — while ref/ replays the v0 forms
     natively. Gate 6's differential thereby witnesses the desugaring
     itself. Forms outside the desugaring's domain stay as they are
     and fault below like before. *)
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
