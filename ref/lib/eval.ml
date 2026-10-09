(* The evaluation session of RUNTIME-SEMANTICS ch. 5: forward chaining to
   the least fixpoint, with freshness realised as naive re-evaluation
   passes and idempotence at the write boundary (World.write_attr), and
   creation in generations (ch. 5.5). The creation budget, a bound on the
   depth of a chain of mints within a stratum, turns a chain of creations
   without end into a fault, and a bound on the passes between two
   generations does the same for two writers that keep overwriting each
   other.

   Negation and channel reads are staged as v0's single boundary
   (ch. 5.3): the positive part quiesces first, then negation consumers
   join, then consistency/uniqueness fire at that fixpoint, then channel
   readers (regelversie-inconsistent) run to a fixpoint with the checks
   re-fired once. The general condition — stratification as
   well-formedness — is ch. 6 and out of this card's scope.

   Coverage is deliberately partial: a construct no golden exercises
   faults with context instead of guessing — fail fast, never a silent
   fallback. Of the forms this header once listed as uncovered,
   navigation paths, filters, timelines and object/fact creation have
   since landed; dimension cells and day kinds still fault. ref/README.md
   carries the maintained list. *)

open Ir_v0.Ast
module SMap = World.SMap

let norm = Ir_v0.Checks.norm

(* what a lexical binder is bound to. Every binder form but one binds a value
   that is already computed and known non-leeg — a match-arm payload, a
   lambda/function parameter, a filter element ([Now]). A `Let` binds a NEED
   (ch. 10.6): the value is evaluated at the FIRST read of the binder and
   reused after (call-by-need), and a read whose value evaluates leeg raises
   the owning Let's barrier instead of yielding leeg. [Later] carries that
   read as a closure, so the barrier it raises is the owning Let's own local
   exception — a nested Let can never catch an enclosing one's. *)
type binding = Now of Value.t | Later of (unit -> Value.t)

type env = {
  doc : document;
  params : Value.t SMap.t; (* by norm name *)
  param_tls : Timeline.t SMap.t; (* granular parameters' full timelines (norm name) *)
  as_of : string option; (* ISO date; None = time-free session *)
  locals : binding SMap.t;
      (* lexical binders bound per match-arm evaluation (ch. 9.2), by norm
         name; the row-10 card extends this to lambda/function parameters,
         the ch. 10.6 Let adds the deferred [Later] form *)
  btypes : datatype option SMap.t;
      (* the static type of each lexical binder in [locals], by norm name:
         a binder read carries no type on the wire (B13), so its type is
         the one its binding declares — a match arm's payload, a closed
         body's parameter, a Let value's, a list filter's element *)
  subj : string option;
      (* the static subject type of the position being evaluated: a rule's
         target type, the subject of an allocation, creation, consistency or
         defaults group, the element type inside the filter of a fold, a
         quantifier fold or a union selection. An own read names exactly
         this type ([own_read]); None where the position has no subject (a
         day-kind condition) *)
}

let req what = function Some x -> x | None -> Value.fault "missing %s in IR node" what

(* The key the bound day of a day-kind definition rides on in [env.locals]
   (ch. 3.7, the BoundDay node of the wire). It carries a NUL so that no
   declared binder name can ever collide with it: every other key in that
   map is a [norm]-ed source name, and [norm] neither produces nor preserves
   a NUL. Its presence is also the marker that evaluation is INSIDE a
   day-kind condition — which is the only place a nested day-kind predicate
   is evaluated (ch. 5.4). *)
let day_kind_binder = "\x00de dag"

(* The day kinds currently being evaluated, innermost first. WF-085 makes a
   reference cycle ill-formed, so this guard never fires on a conforming
   document; it is here because a hand-built one must fault rather than
   loop. Single-threaded and exception-safe: the pop runs on every exit. *)
let day_kind_stack : string list ref = ref []

(* --- operand order (ch. 5.11) ------------------------------------------
   The order in which a node's operands are evaluated is normative, because
   it is observable: a fault and a §9.5 channel fact come into being — or
   do not — depending on what ran before them, and a ch. 10.6 barrier ends
   the enclosing Let where it fires. So ref never lets the compiler decide
   it. OCaml leaves the evaluation order of an application's arguments
   UNSPECIFIED (this build evaluates them right to left), and [List.map]
   documents its result, not its application order (only [List.iter] and
   [List.fold_left] fix theirs). Every operand list whose mapping can
   fault, deposit or raise therefore runs through these two explicitly
   sequenced walks, and every multi-operand node sequences its operands
   with [let]. ------------------------------------------------------- *)

let rec map_in_order f = function
  | [] -> []
  | x :: xs ->
      let y = f x in
      y :: map_in_order f xs

let rec filter_in_order p = function
  | [] -> []
  | x :: xs ->
      let keep = p x in
      let rest = filter_in_order p xs in
      if keep then x :: rest else rest

(* --- validity gate (ch. 3.10 / 5.6): a version whose window excludes the
   evaluation date simply does not apply --------------------------------- *)

(* All three positions go through the strict date reader and compare as day
   numbers. They used to compare as raw strings: on canonical lexemes that
   is calendar order, but a non-canonical one took part silently — asOfDate
   "01-06-2026" sorts before every 20xx bound, so the 2026 version did not
   apply and the transition version answered (measured on
   jutterswet-vensterheffing, 2026-09-23), and a bound written day-first
   moved its window the same way. WF-101 and the case schema keep such
   lexemes out of the corpus; this keeps them out of the answer. Shared with
   the clingo translator's mirror of this gate, so both speak one refusal. *)
let validity_day ~what s =
  match Value.date_of_iso s with
  | Value.Date z -> z
  | _ -> assert false
  | exception Value.Fault why ->
      Value.fault "validity gate: %s %S is not the ISO date lexeme yyyy-mm-dd (%s)" what s why

let window_admits ~as_of (g : geldigheid option) =
  match g with
  | None | Some { geld_vanaf = None; geld_tot = None } -> true
  | Some { geld_vanaf; geld_tot } -> (
      match as_of with
      | None ->
          Value.fault "a validity-bounded form needs an evaluation date; this session is time-free"
      | Some d ->
          let day = validity_day ~what:"asOfDate" d in
          let vanaf = Option.map (validity_day ~what:"validity.from") geld_vanaf in
          let tot = Option.map (validity_day ~what:"validity.to") geld_tot in
          (match vanaf with Some v -> v <= day | None -> true)
          && match tot with Some t -> day <= t | None -> true)

let applies env g = window_admits ~as_of:env.as_of g

(* --- name resolution against the document ------------------------------ *)

(* The EFFECTIVE object type (IR-SPEC ch. 2.2): the declaration with its
   member maps closed over the supertype chain, nearest declaration first.
   Every member lookup in this evaluator goes through here, so an
   inherited attribute or characteristic resolves exactly where an own one
   does — and a member declared on a strict SUBtype still does not resolve
   at the supertype, because the walk only goes up. *)
let objecttype env name =
  match Ir_v0.Checks.effective_ot env.doc name with
  | Some ot -> ot
  | None -> Value.fault "unknown object type %S" name

(* S ⊑ T. Reflexive, so this is the drop-in for every place that used to
   compare two declared type names for equality. *)
let isa env sub super = Ir_v0.Checks.subtype_of env.doc ~sub ~super

(* the subject's own read (WF-053, IR-SPEC ch. 4.1a): a reference without
   navigation bindings names the static subject type of its position — a
   supertype is another type, even where it declares the member, and only a
   navigation reaches another type. The verifier's sentence, so all
   executors refuse with one voice. The instance read must still be of the
   named type (ch. 2.2); that is the answer. *)
let own_read env (self : World.instance) otn =
  (match env.subj with
  | Some st when norm st <> norm otn ->
      Value.fault
        "an attribute reference without a path is the subject's own read, so its subjectType is \
         the subject type '%s' — '%s' names another type, which only a navigation reaches"
        st otn
  | _ -> ());
  isa env self.otype otn

(* An instance of [inst_type] fills a role declaring filler type T iff
   inst_type ⊑ T — the declared filler type is an UPPER bound (ch. 2.6). *)
let fills env inst_type (r : rol) =
  isa env inst_type (Option.value r.rol_objecttype_naam ~default:"")

(* The effective defaults stacks per concrete type (RUNTIME-SEMANTICS
   ch. 7.10). A composition the block order does not define is a NAMED
   refusal, surfaced as an evaluation fault like any other rather than as an
   escaping exception. *)
let effective_defaults env =
  try Ir_v0.Desugar.effective_defaults env.doc
  with Ir_v0.Desugar.Unlowered m -> Value.fault "%s" m

let attr_decl env ~otype ~attr =
  let ot = objecttype env otype in
  match Ir_v0.Checks.find_ci ot.attributen attr with
  | Some dt -> dt
  | None -> Value.fault "unknown attribute %S on %S" attr otype

(* a FunctionDef resolved by name (ch. 2.11); an unresolved invoke is a
   WF-068 miss made statically impossible, so it faults fail-fast *)
let function_def env name : function_def =
  match List.find_opt (fun (d : function_def) -> norm d.fd_naam = norm name) env.doc.function_defs with
  | Some d -> d
  | None -> Value.fault "unknown function %S (the verifier should preclude this)" name

let kenmerk_temporal (ot : objecttype) name =
  List.exists (fun (k, _) -> norm k = norm name) ot.kenmerk_granulariteit

(* ch. 3.10's case distinction is on the rule's TARGET, and a condition cannot
   see one — so the clip case is decided here, where the target is known, and
   never inside [eval_cond]. Without this walk a period condition on a
   temporal target would silently get the SCALAR gate: all-or-nothing on the
   evaluation date where the chapter says the window speaks about the
   timeline. That is the wrong axis, not a near miss. *)
(* HONESTY NOTE, the [guard_on_doel] precedent (card eigenaar-gastheer-object,
   advice f). The ASP backend links this library and calls THIS function, so
   on the clip's case distinction `ref ≡ clingo` is one piece of code with two
   callers, not two implementations that agree — it is no differential, and
   saying so is the point of the note. Shared deliberately all the same: a
   second walk is a second place where the boundary of ch. 3.10's two cases
   could be drawn, and one construct wrongly excluded there is a window
   silently gated on the wrong axis.
   What the sharing HID is recorded with it (reader finding 2026-09-20): the
   agreement made the surrounding target test look shared too, and it was
   not — each executor asked its own table, ref the characteristic one and
   the ASP backend the attribute one only, so a temporal CHARACTERISTIC target
   passed the test there and got the scalar gate. The fix left both target
   tests unshared, on purpose, and gave the position a red witness
   (`corpus/rood/periodevenster-tijdlijnkenmerk`). Share the syntax walk,
   never the judgement it feeds. *)
let rec bevat_periode (c : conditie) =
  match c with
  | Periode_conditie _ -> true
  | Samengesteld { onderdelen; _ } -> List.exists bevat_periode onderdelen
  | Kwantorvouw { kv_filter; kv_toets; _ } ->
      let opt = function Some c -> bevat_periode c | None -> false in
      opt kv_filter || opt kv_toets
  | Vergelijking _ | Predicaat _ | Kenmerk_check _ | Role_membership _
  | Regelversie _ | Conditie_onopgelost _ ->
      false


(* ch. 3.10's v0 mirror note carries TWO bounds on the window and this is the
   second: v0's backends "refuse the condition under counting quantifiers".
   Measured 2026-09-20, before card drools-venster-vouwtoets (2026-09-23), on
   verifier-clean documents, one per quantifier, with the window as the
   second member of a composite: the DRL-text branch then named this very
   limit ("periodevergelijking onder een tellende kwantor") under `none`,
   `atLeast`, `atMost` and `exactly`, and the exec branch dropped the rule;
   both PLAYED under `all` and `the`. So the boundary is not invented here, it is
   read off the three other executors — and the two carried quantifiers are
   exactly the two this interpreter has a corpus witness for. Since that card
   both Drools branches play the window under `all` and refuse it by name
   under `none` and the counting quantifiers, citing WF-112, in a composite and
   in a fold alike.

   What the refused position would otherwise do is a reading nothing norms:
   under `none` the window's verdict is COMPLEMENTED, so a time-free session —
   where ch. 3.10 says the window never matches — would make the composite
   TRUE and fire the rule. The chapter norms the window as a gate on the
   RULE, not as a member a quantifier may count.

   The fold node is refused by the same rule and for the same reason: its
   quantifiers are the composite's, applied to instances instead of parts.

   Since 2026-09-23 the position is WF-112 (maintainer decision): the
   verifier refuses the document, so this fault is fail-fast on a
   non-conforming input rather than the answer to a conforming one, and it
   may not be softened to a fallback. The fold measurement of that day, taken
   before card drools-venster-vouwtoets, is why the rule covers the fold as
   well: the Drools DRL-text branch then PLAYED a window under all five fold
   quantifiers, and under `none` in a time-free session it answered the
   complement — true — which is exactly the reading nothing norms. *)
let periode_onder_telkwantor (kwantor : kwantor) (parts : conditie list) =
  match kwantor with
  | Alle | De -> ()
  | Geen | Ten_minste | Ten_hoogste | Precies ->
      if List.exists bevat_periode parts then
        Value.fault
          "a period condition under none or a counting quantifier (atLeast / atMost / exactly) \
           is the position WF-112 refuses: the quantifier counts or complements the \
           window's verdict, while ch. 3.10 norms the window as a gate on the RULE, so the \
           document is not well-formed. Declined at evaluation"

(* WF-108, the quantifier fold's shape: a matrix; a range in one spelling,
   a path of at least two steps; a quantifier other than `the`; a threshold
   under a counting quantifier. On the wire the loader refuses the two range
   spellings and the verifier the rest; a node that reaches an executor past
   them is refused here in their sentences, never read with a part it lacks.
   The ASP backend calls this function too, so the two executors refuse in
   one voice, and a quantifier fold never meets the aggregation's sentences
   for sourceRole and sourcePath. Returns the matrix and the threshold (0
   under `all` and `none`, which read none). *)
let quantifier_fold_shape ~(kwantor : kwantor) ~(drempel : int option) ~(toets : conditie option)
    ~(bron_rol : rol_binding option) ~(bron_pad : rol_binding list option) =
  let toets =
    match toets with
    | Some t -> t
    | None ->
        Value.fault
          "quantifier fold without a test: the matrix is what every contributor must satisfy, \
           and a quantifier over no condition has no verdict"
  in
  (match (bron_rol, bron_pad) with
  | Some _, Some _ ->
      Value.fault "sourcePath beside sourceRole: a quantifier fold carries one range, never two"
  | None, None ->
      Value.fault
        "a quantifier fold carries its range (sourceRole or sourcePath): the range is written, \
         never derived"
  | _, Some pad when List.length pad < 2 ->
      Value.fault
        "a quantifier fold's sourcePath carries at least TWO steps, found %d — the one-step range \
         is spelled sourceRole"
        (List.length pad)
  | _ -> ());
  (match kwantor with
  | Alle | Geen | Ten_minste | Ten_hoogste | Precies -> ()
  | De -> Value.fault "%s" Ir_v0.Checks.quantifier_fold_the_refusal);
  let drempel =
    match (kwantor, drempel) with
    | (Ten_minste | Ten_hoogste | Precies), Some d -> d
    | (Ten_minste | Ten_hoogste | Precies), None ->
        Value.fault
          "counting quantifier fold without a threshold: the verdict compares the number of \
           contributors satisfying the matrix with a threshold, and there is none to compare it \
           with"
    | _, _ -> 0
  in
  (toets, drempel)

(* WF-100 / IR-SPEC ch. 4.6 (q1), maintainer decision 2026-09-10: on a
   characteristic check the `host` is AUTHORITATIVE for the declaration.
   WF-109 / ch. 4.6 (q2), maintainer decision 2026-09-19: it is also the
   GASTHEER — it names the object type the check LIVES ON, and that object is
   the one read. The v0 producer sets it on exactly one shape, the
   fact-creation guard that names a characteristic of the TARGET (the filler
   of the created relation's target role), so that the constraint lands on
   the target and never on the anchor, even when the anchor declares a
   same-named characteristic.

   This helper is the fence for the one-object positions, which is every
   position except that guard: there the host must be a type the subject
   satisfies, and a host naming anything else has no object here to be about.
   Answering from the subject anyway is precisely the reading the decision
   rejected. Since WF-109 such a document is refused by the VERIFIER, so this
   fault is fail-fast on a non-conforming input rather than the answer to a
   conforming one — which is why it may not be softened to a fallback.

   The fact-creation guard is handled at [run_feit_creatie]: that position
   binds two objects, the guard is evaluated on the one the host names, and
   this helper then sees a subject the host is satisfied by.

   Where the field is ABSENT, or names the subject's own type, nothing changes:
   the declaration consulted and the object read coincide. *)
let owner_hosts (doc : document) ~(otype : string) ~(kenmerk : string option)
    (eigenaar : string option) =
  match eigenaar with
  (* the owner is satisfied EXTENSIONALLY: a subject of a subtype IS an
     object of the owner type, so an inherited characteristic check reads
     the object it is hosted on (ch. 2.2) *)
  | Some e when not (Ir_v0.Checks.subtype_of doc ~sub:otype ~super:e) ->
      Value.fault
        "characteristic check %S is hosted on %S while the subject is %S: the host names the \
         object the check is about, and this reader has no other object to read"
        (Option.value kenmerk ~default:"?") e otype
  | _ -> ()

(* RUNTIME-SEMANTICS ch. 5.5 / ch. 5.8b, WF-109: is this fact creation's guard
   about the TARGET? The guard is the one condition position with two objects
   in scope, and the host field is what names which.

   The question is asked with the SAME two predicates the verifier uses, from
   the same shared module, so a document cannot be legal there and read
   differently here. A binding SATISFIES the host when `T ⊑ O` (every object it
   holds is one); it OVERLAPS the host when some object could be both. Exactly
   one satisfying binding and no second overlapping one is the single reading
   the wire settles — anything else designates two objects with one name.

   Three answers, and each is the chapter's:

   - TARGET — the satisfying binding is the target. The guard filters the
     targets.
   - ANCHOR (false) — the satisfying binding is the anchor, the host is absent,
     the check navigates or carries a whole-period qualifier, or the condition
     is anything else. Byte-identical to what this reader did before the rule.
     The whole-period fence is WF-109's third (ch. 4.6 (q2)) and is held here
     as well as in the verifier: under a qualifier both bean-model branches run
     the ch. 3.9 fold arm before they look at the host, so the form is off the
     wire and this reader must not answer it from a route the others lack.
   - FAULT — the host designates more than one of the two. ch. 5.8b requires a
     fault there and, until 2026-09-19, this helper returned false instead and
     let the anchor answer: the one reading the decision rejects, delivered
     silently. WF-109 makes that document unreachable through the verifier, so
     this arm is fail-fast on a non-conforming input; it is a NAMED arm about
     this construct and not a fall-through, because the sentence has to say
     which two objects one name reached.

   ZERO satisfying bindings is deliberately NOT a fault here: it falls to the
   anchor, where [owner_hosts] faults with its own named sentence one step
   later. One refusal per defect. *)
let guard_on_doel (doc : document) (fc : resolved_feit_creatie) =
  match (fc.anker, fc.fc_doel, fc.fc_voorwaarde) with
  | ( Some a,
      Some d,
      Some (Kenmerk_check { navigatie_binding = None; periode = None; eigenaar = Some e; _ }) ) -> (
      let bound = [ ("anchor", a); ("target", d) ] in
      let hits = List.filter (fun (_, t) -> Ir_v0.Checks.subtype_of doc ~sub:t ~super:e) bound in
      let near = List.filter (fun (_, t) -> Ir_v0.Checks.populations_overlap doc t e) bound in
      (* the verifier's own WF-109 sentence, from the one place both read it;
         [None] is the settled reading, whose one satisfying binding decides *)
      match Ir_v0.Checks.wf109_host_sentence ~host:e ~bound ~hits ~near with
      | None -> List.exists (fun (r, _) -> r = "target") hits
      | Some _ when hits = [] -> false
      | Some m -> Value.fault "%s" m)
  | _ -> false

(* ch. 3.9, the §8.2 whole-period qualifier over a CHARACTERISTIC, and the
   one place ch. 3.3's refusal must not reach.

   ch. 3.3 refuses "a time-dependent attribute or parameter in a scalar
   context" and names its own two exits in the same breath: "the only ways a
   timeline becomes a scalar are the dedicated collapses: the temporal total
   (ch. 3.6) and the whole-period predicate (ch. 3.9)". ch. 8.2 and ch. 3.11
   say it twice more ("the only bridges … are the dedicated collapses";
   "refused EXCEPT through the specified collapses"). A condition carrying a
   WholePeriod qualifier is therefore not a scalar context at all — it IS
   the second collapse — and the refusal below this helper, which fires where
   the qualifier is ABSENT, may not touch it. Until 2026-09-11 both
   characteristic-check arms matched `periode` away with `_`, so the
   qualified form fell into the scalar refusal (loudly, for the wrong reason)
   or, with no link to follow, into the scalar arm's absent-link `false`
   (silently, contradicting the clause).

   [inst] is None for a FAILED to-one navigation: ch. 3.9 gives it a null
   timeline, which touches no bucket and passes vacuously — the ch. 1.2
   leeg-navigation discipline lifted, and a deliberate divergence from the
   UNQUALIFIED navigated check, whose v0 asymmetry answers false for the
   positive and the negated form alike. [ot] is the type the characteristic
   is declared on (the reached type on a navigated check), never the
   subject's: the time-dependence cross is a static property of the
   document, so it is taken from the declaration and fires whether or not a
   link happens to be there.

   WF-043 covers that cross for the NON-navigated check only (it resolves
   against the subject); on a navigated one the characteristic lives on the
   reached type, which the verifier does not cross there — so this fault is
   the only thing standing between a non-temporal characteristic and a
   silently vacuous pass from an always-empty period set. *)
let whole_period_kenmerk env ~(ot : string) ~(inst : World.instance option) ~kenmerk ~negatie
    (gp : geheel_periode) =
  if not (kenmerk_temporal (objecttype env ot) kenmerk) then
    Value.fault
      "whole-period qualifier on %S, which is not a time-dependent characteristic of %S"
      kenmerk ot;
  let g : Ir_v0.Ast.granulariteit = if gp.gp_jaar then Jaar else Maand in
  let true_periods =
    match inst with
    | None -> []
    | Some (i : World.instance) -> Option.value (SMap.find_opt (norm kenmerk) i.ctls) ~default:[]
  in
  let verdict = Timeline.geheel_periode_kenmerk g ~negatie true_periods in
  (* ch. 3.9: "Negation complements the folded result" — the QUALIFIER's
     own "niet gedurende …", distinct from the inner test's negation *)
  if gp.gp_niet then not verdict else verdict

(* WF-097: the two qualifications a gedurende clip member has no reading for.
   Both were matched away with `_` in every clip arm until 2026-09-09, so a
   document carrying either got the UNQUALIFIED answer with nothing said.

   ONE helper for the THREE clips — the temporal total (ch. 3.6), the pro-rata
   fold (ch. 3.7) and the day count (ch. 3.8) — because it is the same node in
   the same field, and three copies could drift apart one edit at a time. That
   is not hypothetical: the rule first landed on the total alone and the other
   two kept answering silently for a day. The verifier and the ASP backend each
   hold the same fence in one shared place, for the same reason. *)
let rec clip_member_qualifications (c : conditie) =
  match c with
  | Kenmerk_check { kenmerk; eigenaar; periode; _ } ->
      let naam = Option.value kenmerk ~default:"?" in
      if eigenaar <> None then
        Value.fault
          "host on the gedurende clip member %S: the clip reads the subject's own \
           characteristic timeline" naam;
      if periode <> None then
        Value.fault
          "whole-period qualifier on the gedurende clip member %S: the clip already ranges \
           over the whole timeline" naam
  | Samengesteld { onderdelen; _ } -> List.iter clip_member_qualifications onderdelen
  | _ -> ()

(* the grid a temporal attribute declares (ch. 3.6): both temporal-total
   routes — the subject's own timeline and the §7.1 per-element fold —
   need it, and a temporal attribute without one is a wire defect the
   collapse must never paper over *)
let attr_granulariteit (ot : objecttype) name =
  match Ir_v0.Checks.find_ci ot.attribuut_granulariteit name with
  | Some (Some g) -> g
  | _ -> Value.fault "temporal attribute %S without a granularity" name

(* the unique fact type relating [from_type] to [to_type], with its
   name and both roles — (fact-type name, role to_type fills, role
   from_type fills). None when no fact type relates them (the linkless
   creation shape); a fault when more than one does — v0 silently
   dropped the back-link there, execution refuses to guess (recorded
   delta, ch. 5.5). Shared with the ASP backend. *)
let connecting_pair env ~from_type ~to_type =
  let candidates =
    List.filter_map
      (fun (ftn, (ft : feittype)) ->
        let f t r = fills env t r in
        match ft.rollen with
        | [ a; b ] when norm from_type = norm to_type ->
            (* a self-typed two-role fact type is role-ambiguous: it
               BLOCKS the derivation (the pinned runtime counts both
               roles as candidates and derives null) — the explicit
               connectingRole binding is the unambiguous route
               (IR-SPEC ch. 3.3) *)
            if f from_type a && f from_type b then Some `Blocked else None
        | [ a; b ] ->
            if f from_type a && f to_type b then Some (`Pair (ftn, b, a))
            else if f from_type b && f to_type a then Some (`Pair (ftn, a, b))
            else None
        | [ r ] when ft.wederkerig ->
            if f from_type r && norm from_type = norm to_type then Some (`Pair (ftn, r, r))
            else None
        | _ -> None)
      env.doc.feittypen
  in
  match candidates with
  | [] -> None
  | [ `Pair p ] -> Some p
  | _ when norm from_type = norm to_type ->
      (* any plurality on a self-typed pair — including a reciprocal
         beside a blocked two-role shape — is v0's null: no link *)
      None
  | _ ->
      Value.fault "more than one fact type relates %S to %S; the created object's back-link is \
                   ambiguous (ch. 5.5)"
        from_type to_type

(* the explicit role binding (v1 extension, IR-SPEC ch. 2.6): resolve the
   named role of the named fact type. The verifier (WF-047) guarantees
   resolution on verifier-clean input; a dangling binding still faults. *)
let bound_role env (rb : rol_binding) =
  match
    List.find_opt (fun (n, _) -> norm n = norm rb.rb_feittype) env.doc.feittypen
  with
  | None -> Value.fault "role binding names unknown fact type %S" rb.rb_feittype
  | Some (_, (ft : feittype)) -> (
      match
        List.find_opt
          (fun (r : rol) ->
            match r.rol_naam with Some n -> norm n = norm rb.rb_rol | None -> false)
          ft.rollen
      with
      | Some r -> r
      | None ->
          Value.fault "role binding names unknown role %S of fact type %S" rb.rb_rol
            rb.rb_feittype)

(* The two named roles of a fact creation (IR-SPEC ch. 3.4) resolve as
   WF-028 resolves them: the created fact type is declared and has exactly
   two roles, and exactly one of them carries the name. The verifier refuses
   anything else; a node that reaches an executor without that check is
   refused in WF-028's own sentences, which the Drools loader speaks for the
   target role too, rather than in the binding sentences above. *)
let created_role env ~fact_type ~role =
  match Ir_v0.Checks.find_ci env.doc.feittypen fact_type with
  | None -> Value.fault "created fact type '%s' is not declared" fact_type
  | Some (ft : feittype) -> (
      match ft.rollen with
      | [ a; b ] -> (
          let named (r : rol) =
            match r.rol_naam with Some n -> norm n = norm role | None -> false
          in
          match (named a, named b) with
          | true, false -> a
          | false, true -> b
          | true, true -> Value.fault "role '%s' is ambiguous within '%s'" role fact_type
          | false, false -> Value.fault "'%s' is not a role of '%s'" role fact_type)
      | _ -> Value.fault "created fact type '%s' does not have exactly two roles" fact_type)

(* the storage keys a role's links may sit under, in read-preference
   order: the case may key them by the role's plural or its name *)
let role_keys (r : rol) = List.filter_map (fun k -> k) [ r.rol_meervoud; r.rol_naam ]

(* linked instances over [role] seen from [self]: the first non-empty
   key wins — the same preference the creation writer (World.add_link)
   uses, so created and case-supplied links are read identically *)
let role_links world (self : World.instance) (role : rol) =
  let labels =
    List.fold_left
      (fun acc k -> if acc = [] then World.links self k else acc)
      [] (role_keys role)
  in
  List.map (World.get world) labels

(* linked instances seen from [self] over the bound role: the relation a
   node reads is written on it, never derived (B12) *)
let linked env world (self : World.instance) ~(binding : rol_binding) =
  role_links world self (bound_role env binding)

(* ch. 3.4b / WF-102: the multi-step fold source. The folded set is the
   FLAT UNION over the path — walk step 1 from the subject, step 2 from
   each instance step 1 reached, and so on — and it is a SET: an instance
   reachable along more than one intermediate contributes ONCE. Dedup is
   by instance LABEL, keeping first-reach order, which is the order
   [World.instance] identity is keyed on everywhere else; without it a
   diamond would double-count in a som and the ASP backend (whose
   aggregate collapses equal tuples by construction) would disagree.
   A to-ONE step needs no separate arm: [role_links] yields its
   zero-or-one instance like any other. *)
let linked_path env world (self : World.instance) ~path =
  let step acc rb =
    let role = bound_role env rb in
    List.concat_map (fun i -> role_links world i role) acc
  in
  let reached = List.fold_left step [ self ] path in
  let seen = Hashtbl.create 16 in
  filter_in_order
    (fun (i : World.instance) ->
      if Hashtbl.mem seen i.label then false
      else (
        Hashtbl.add seen i.label ();
        true))
    reached

(* the instance fold's element set (IR-SPEC ch. 2.2, the EXCLUSION
   direction of extensional quantification). The relation supplies the
   REACH, the declared [element] type supplies the POPULATION: the fold
   ranges over { i : typeOf(i) ⊑ element }, so a linked instance the
   relation reaches but the declared element type does not cover is not a
   member of it. The two coincided while WF-047 demanded the bound role's
   filler and the fold's element type be the same name; since ch. 2.6 widened
   that to ⊑-compatibility the filler may be a STRICT SUPERTYPE, and the
   role then reaches instances the fold does not range over. Exactly the
   split ch. 4.1 makes for an allocation's recipients, and identity
   wherever the filler is [element] or below — which is every document
   written before the chain existed. *)
let fold_members ?binding ?path env world (self : World.instance) ~element =
  let reached =
    match (path, binding) with
    | Some pad, _ -> linked_path env world self ~path:pad (* ch. 3.4b *)
    | None, Some rb -> linked env world self ~binding:rb
    | None, None ->
        Value.fault
          "an instance fold over %S carries no range: its relation is written as a range (sourceRole \
           or sourcePath) and never derived"
          element
  in
  List.filter (fun (i : World.instance) -> isa env i.otype element) reached

(* the navigated scalar read's hop walk (WF-053, RUNTIME-SEMANTICS
   ch. 5.8): the hop roles are the node's bindings (D10), walked link by
   link. An absent link at any hop short-circuits the whole read to
   None = leeg (ch. 1.2); a to-many hop in a scalar slot faults, never a
   silent first pick. *)
let nav_target env world (self : World.instance) ~(bindings : rol_binding list) ~endpoint =
  let roles = List.map (bound_role env) bindings in
  List.iter
    (fun (r : rol) ->
      if r.rol_meervoudig then
        Value.fault "navigation over the to-many role %S in a scalar slot is refused"
          (Option.value r.rol_naam ~default:"?"))
    roles;
  let rec walk (cur : World.instance) = function
    | [] ->
        (* fail fast on a walk the verifier would reject: a binding chain
           that lands off the declared endpoint must never silently read
           an absent attribute as leeg *)
        (* the reached instance must BE of the declared endpoint type —
           extensionally, so a subtype instance satisfies a supertype
           endpoint (ch. 2.2); the unsound direction, a supertype instance
           against a subtype endpoint, still faults *)
        if not (isa env cur.otype endpoint) then
          Value.fault "the navigation reached %S, not the declared endpoint %S" cur.otype
            endpoint;
        Some cur
    | (r : rol) :: rest -> (
        match role_links world cur r with
        | [] -> None
        | [ next ] -> walk next rest
        | _ ->
            Value.fault "to-one role %S links more than one instance on %S (ch. 5.5)"
              (Option.value r.rol_naam ~default:"?")
              cur.label)
  in
  walk self roles

(* ch. 3.9, the timeline a whole-period leeg/gevuld test folds: an own
   time-dependent attribute, or one reached over to-one hops (ch. 5.8). A
   hop that reaches nothing gives the null timeline, which touches no bucket
   and passes (ch. 3.9 lifts the ch. 1.2 leeg-navigation discipline). WF-043
   admits one operand more, a time-dependent parameter, and no executor
   folds one; it is refused by name, in the ASP backend's and the Drools
   loader's words as well. Any other operand is what WF-043 rejects, and it
   is refused here in the verifier's sentence, so a hand-built node never
   folds an empty timeline to a pass. *)
let whole_period_operand env world (self : World.instance) (e : expr) : Timeline.t =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn; navigatie_bindingen; _ }
    when Ir_v0.Checks.attr_temporal (objecttype env otn) a -> (
      match navigatie_bindingen with
      | None ->
          if not (own_read env self otn) then
            Value.fault
              "a read of %S on %S without navigation bindings is the subject's own read, but the \
               subject is %S"
              a otn self.otype;
          Option.value (World.timeline self a) ~default:[]
      | Some bindings -> (
          match nav_target env world self ~bindings ~endpoint:otn with
          | None -> []
          | Some target -> Option.value (World.timeline target a) ~default:[]))
  | Parameter_ref { parameter; _ } ->
      Value.fault
        "a whole-period leeg/gevuld test over the parameter '%s' is not executed: the fold reads the \
         timeline of an attribute"
        (Option.value parameter ~default:"?")
  | _ -> Value.fault "wholePeriod qualifier on a non-time-dependent operand"

(* --- literals ----------------------------------------------------------- *)

let unit_of_datatype = function
  | Some (Numeriek { eenheid = Some e; _ }) | Some (Percentage { eenheid = Some e; _ }) ->
      Value.Unit e
  | _ -> Value.Unitless

(* A literal's value is its variant (IR-SPEC ch. 4.1, delta B22), read
   under the literal's type: a number's string through the one number
   grammar ([Value.q_of_lexeme]), a date's and a dateTime's through the one
   date readers. The loader has crossed the variant against the type before
   any evaluation (WF-114, [Ir_v0.Loader.reader]), so a variant its type
   does not name never arrives from the wire; a node built by hand that
   carries one is refused here in the loader's own sentence, never read as
   the type's value. A literal without a type (WF-034's typeless value node)
   reads its lexeme as a unitless number, as it always did. *)
let literal_value (variant : literal_variant) (typ : datatype option) =
  (match typ with
  | Some t -> (
      match Ir_v0.Checks.literal_variant_refusal variant t with
      | Some sentence -> Value.fault "%s" sentence
      | None -> ())
  | None -> ());
  match (typ, variant) with
  | None, v -> Value.Num (Value.q_of_lexeme (Ir_v0.Loader.literal_lexeme v), Value.Unitless)
  | Some (Numeriek _ | Percentage _), Lv_number s -> Value.Num (Value.q_of_lexeme s, unit_of_datatype typ)
  | Some Booleaan, Lv_boolean b -> Value.Bool b
  | Some Tekst, Lv_text s -> Value.Text s
  | Some (Enumeratie _), Lv_enumeration s -> Value.Enum s
  | Some Datum_in_dagen, Lv_date s -> Value.date_of_iso s
  (* WF-095 (ch. 4.1): a datum-tijd literal reads into the SAME millisecond
     layer the case wire reads its `dateTime` into, through the same single
     admitted spelling — the canonical ISO lexeme; the day-first source
     snippet is refused like any other form. An unreadable lexeme faults
     with the position, never silently becoming some other instant. *)
  | Some Datum_tijd, Lv_date_time s -> Value.datetime_of_lexeme s
  | Some _, v ->
      Value.fault "literal of this datatype not implemented: %S" (Ir_v0.Loader.literal_lexeme v)

(* --- expression evaluation --------------------------------------------- *)

(* [err] deposits a §9.5 error reason for the evaluating rule × subject *)
(* --- condition evaluation ---------------------------------------------- *)

(* the static type of an expression (RUNTIME-SEMANTICS ch. 2.10). A
   reference carries no type on the wire (B13): it takes the type of the
   declaration it names — the attribute's declared datatype on the type it
   is read on, the parameter's, the dimension cell's attribute's, the
   binder's binding. A characteristic is no attribute and has no value
   type (WF-080), so its name types to None, as in the verifier. Every
   other node carries its own annotation. *)
let rec static_type env (e : expr) : datatype option =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ } -> (
      match Ir_v0.Checks.effective_ot env.doc ot with
      | None -> None
      | Some o -> (
          match Ir_v0.Checks.find_ci o.attributen a with Some dt -> dt | None -> None))
  | Parameter_ref { parameter = Some p; _ } -> (
      match Ir_v0.Checks.find_ci env.doc.parameters p with
      | Some par -> par.par_datatype
      | None -> None)
  | Dimensie_cel { attribuut = Some inner; _ } -> static_type env inner
  | Binder_ref { naam; _ } -> Option.join (SMap.find_opt (norm naam) env.btypes)
  | Attribuut_ref _ | Parameter_ref _ | Dimensie_cel _ | Variabele_ref _ -> None
  | Literal { typ; _ }
  | Tekst_reeks { typ; _ }
  | Binair { typ; _ }
  | Functie { typ; _ }
  | Afgerond { typ; _ }
  | Begrensd { typ; _ }
  | Constante { typ; _ }
  | Bound_day { typ; _ }
  | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ }
  | Construct { typ; _ }
  | Match { typ; _ }
  | Let { typ; _ }
  | Struct_literal { typ; _ }
  | Field_access { typ; _ }
  | List_literal { typ; _ }
  | If_then_else { typ; _ }
  | Invoke { typ; _ }
  | Fold { typ; _ }
  | Map_list { typ; _ }
  | Filter_list { typ; _ }
  | List_length { typ; _ } ->
      typ
  (* a Lambda is never a value (WF-069, position-restricted): it carries no
     wire [typ] and never reaches a slot where its static type is read.
     Neither is an instance-set union (WF-110): it denotes INSTANCES, and
     the node that consumes it derives the count's own whole type *)
  | Lambda _ | Instance_set_union _ -> None

(* the ch. 1.2d empty-equality asymmetry routes on the declared operand
   kind — two empty runtime values cannot reveal it (Arith.compare_op) *)
let static_numeric env l r =
  let kind e =
    match static_type env e with
    | Some (Numeriek _ | Percentage _ | Datum_in_dagen | Datum_tijd) -> Some true
    | Some (Tekst | Booleaan | Enumeratie _ | Sum_type _ | List_type _ | Struct_type _) -> Some false
    | None -> None
  in
  match kind l with Some b -> Some b | None -> kind r

(* the ch. 1.2d date row rides the numeric bucket above EXCEPT its
   both-leeg gelijk-aan cell, which is onwaar — v0's Tabel-38 guard on
   the date shape (Arith.compare_op ~static_date) *)
let static_date env l r =
  let is_date e =
    match static_type env e with Some (Datum_in_dagen | Datum_tijd) -> true | _ -> false
  in
  is_date l || is_date r

(* the OPERAND SWAP of a comparison (ch. 3.9): "40 < x" read as a test on
   x is "x > 40". Reversal, never negation — the two coincide on totally
   ordered filled values and part company on a leeg operand, which makes
   BOTH orderings false (ch. 1.2d). The equalities are symmetric and stay
   themselves. *)
let omgekeerd : Ir_v0.Ast.vergelijk_op -> Ir_v0.Ast.vergelijk_op = function
  | Kleiner -> Groter
  | Kleiner_of_gelijk -> Groter_of_gelijk
  | Groter -> Kleiner
  | Groter_of_gelijk -> Kleiner_of_gelijk
  | Eerder -> Later
  | Eerder_of_gelijk -> Later_of_gelijk
  | Later -> Eerder
  | Later_of_gelijk -> Eerder_of_gelijk
  | (Gelijk | Ongelijk) as op -> op

let rec eval_expr env world (self : World.instance) ~err (e : expr) : Value.t =
  match e with
  | Literal { variant; typ; _ } -> literal_value variant typ
  | Parameter_ref { parameter; _ } -> (
      let name = req "ParameterReference.parameter" parameter in
      let decl =
        match Ir_v0.Checks.find_ci env.doc.parameters name with
        | Some { par_granulariteit = Some _; _ } ->
            (* v0 refuses a granular parameter in a scalar slot (the §8.2
               marker family); how a rule reads a vintage is ch. 8 scope *)
            Value.fault "parameter %S is granular; reading a vintage in a rule is ch. 8 scope" name
        | Some p -> p
        | None -> Value.fault "unknown parameter %S" name
      in
      match SMap.find_opt (norm name) env.params with
      | Some v -> v
      | None -> (
          (* an absent declared parameter is fail-fast (ch. 5.7), EXCEPT a
             list-typed one: an absent list reads leeg and propagates on
             source position, the ch. 10.3 staffelvergoeding case3 the Fold
             needs. Narrowing leeg-on-absent to lists keeps a missing scalar
             parameter a fault, where ref and clingo agree (a scalar absence
             read leeg would diverge — ref's else-branch fires, clingo derives
             nothing). *)
          match decl.par_datatype with
          | Some (List_type _) -> Value.Empty
          | _ ->
              Value.fault "parameter %S is unbound in this case (binding is fail-fast, ch. 5.7)"
                name))
  | Attribuut_ref { attribuut; objecttype = ot; navigatie_bindingen; _ } -> (
      let name = req "AttributeReference.attribute" attribuut in
      let ot = req "AttributeReference.subjectType" ot in
      (* a value position reads a DECLARED attribute of the type the value
         lives on; any other name — a characteristic, or the role name a
         count source writes — has no value here, and reading it as an
         absent attribute would answer leeg for a form no reader writes *)
      if Ir_v0.Checks.find_ci (objecttype env ot).attributen name = None then
        if Ir_v0.Checks.has_kenmerk (objecttype env ot) name then
          Value.fault
            "'%s' is a characteristic of '%s', not an attribute — a characteristic endpoint is \
             ill-formed in a value position"
            name ot
        else
          Value.fault "%S is not a declared attribute of %S: a value position reads an attribute" name ot;
      (* ch. 3.3, mirrored: no point-in-time read exists — a temporal
         attribute in a scalar slot is refused; the only bridges are
         the dedicated collapses (the temporal total handles its own
         argument in eval_functie). [ot] is the type the value lives on:
         the subject itself, or the navigation endpoint. *)
      if Ir_v0.Checks.attr_temporal (objecttype env ot) name then
        Value.fault
          "temporal attribute %S in a scalar context is refused (ch. 3.3; no point-in-time \
           read exists — the collapses are the only bridges)"
          name;
      match navigatie_bindingen with
      | None ->
          (* D10: without bindings the read is the subject's own — which
             includes an INHERITED member, written at the declaring
             supertype and read on a subtype instance (ch. 2.2) *)
          if not (own_read env self ot) then
            Value.fault
              "a read of %S on %S without navigation bindings is the subject's own read, but the \
               subject is %S"
              name ot self.otype;
          World.attr self name
      | Some bindings -> (
          (* the navigated read (ch. 5.8): walk the to-one hops; an absent
             link anywhere reads the whole operand as leeg *)
          match nav_target env world self ~bindings ~endpoint:ot with
          | None -> Value.Empty
          | Some target -> World.attr target name))
  | Binair { op = (Plus | Min) as op; links; rechts; _ }
    when List.exists
           (fun e ->
             match Option.bind e (static_type env) with
             | Some (Datum_in_dagen | Datum_tijd) -> true
             | _ -> false)
           [ links; rechts ] -> (
      (* date ± duration (ch. 3.8), routed on the static date type. The
         WF-035 discipline, enforced fail-fast behind the verifier: the
         date stands on the LEFT of plus and min (v0's typing opens its
         date branch only for a date-left operand — no commuting), date ±
         date is ill-formed; the duration's unit is its static type's unit
         spelling through the shared table — sub-day units demand
         datum-tijd. *)
      let le = req "Binary.left" links and re = req "Binary.right" rechts in
      let date_typed e =
        match static_type env e with Some (Datum_in_dagen | Datum_tijd) -> true | _ -> false
      in
      let minus = op = Min in
      let dur_e =
        if date_typed le && date_typed re then
          Value.fault "%s" Ir_v0.Checks.date_plus_date_refusal
        else if date_typed le then re
        else
          Value.fault "%s" (Ir_v0.Checks.duration_before_date_refusal ~minus)
      in
      let unit_ =
        match static_type env dur_e with
        | Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ }) -> (
            match Ir_v0.Checks.time_unit_of_eenheid u with
            | Some tu ->
                if Ir_v0.Checks.time_unit_sub_day tu && static_type env le = Some Datum_in_dagen
                then
                  Value.fault "%s" Ir_v0.Checks.sub_day_unit_on_day_date
                else `Calendar tu
            | None -> (
                (* not a calendar unit: the spelling may name a declared day
                   kind, which is the ch. 3.8 day-by-day step (§6.11) *)
                match day_kind_of_unit env u with
                | Some k -> `Day_kind k
                | None ->
                    Value.fault "%s"
                      (Ir_v0.Checks.date_duration_unknown_unit
                         (Option.value u.afkorting ~default:"?"))))
        | Some d when not (Ir_v0.Checks.is_number_like d) ->
            Value.fault "%s" (Ir_v0.Checks.date_duration_operand_refusal d)
        | _ -> Value.fault "%s" Ir_v0.Checks.date_duration_without_time_unit
      in
      let d = eval_expr env world self ~err le in
      let dur = eval_expr env world self ~err re in
      match unit_ with
      | `Calendar tu -> Arith.date_plus_duration ~minus ~unit_:tu d dur
      | `Day_kind k ->
          Arith.date_plus_day_kind ~minus ~matches:(day_kind_pred env world self ~err k) d dur)
  | Binair { op; links; rechts; _ } -> (
      let l = eval_expr env world self ~err (req "Binary.left" links) in
      let r = eval_expr env world self ~err (req "Binary.right" rechts) in
      match op with
      | Plus -> Arith.add l r
      | Min -> Arith.sub_min l r
      | Verminderd_met -> Arith.sub_verminderd l r
      | Maal -> Arith.mul l r
      | Gedeeld_door -> Arith.div_ ~err l r
      | Gedeeld_door_abs -> Arith.div_abs ~err l r
      | Tot_de_macht -> Arith.pow_ l r
      | Percentage_van -> Arith.pct_of l r)
  | Afgerond { basis; modus; decimalen; _ } -> (
      match basis with
      | Some (Functie { kind = Wortel; argumenten = [ arg ]; _ }) ->
          (* the mandatory rounding after a root (§13.4.17.13) rounds the
             true irrational, exactly *)
          numeric_builtin_operand env Wortel arg;
          Arith.sqrt_rounded ~err modus decimalen (eval_expr env world self ~err arg)
      | _ ->
          Arith.round_v modus decimalen (eval_expr env world self ~err (req "Rounding.base" basis)))
  | Begrensd { basis; minimum; maximum; _ } ->
      (* ch. 5.11, wire order: base, then minimum, then maximum. The [let]s
         carry the semantics, not the style — written as two labelled
         arguments this build evaluates the MAXIMUM first, which runs the
         node's operands against the direction ch. 2.7 applies its bounds in.
         An absent bound evaluates nothing. *)
      let v = eval_expr env world self ~err (req "Clamp.base" basis) in
      let bound = function None -> Value.Empty | Some b -> eval_expr env world self ~err b in
      let min_ = bound minimum in
      let max_ = bound maximum in
      Arith.clamp ~min_ ~max_ v
  | Functie { kind = (Tijdsduur | Absolute_tijdsduur) as kind; argumenten; filter; typ; _ } -> (
      (* tijdsduur van A tot B (ch. 3.8), routed on the node-type unit:
         the counted unit is the node's own declared unit, resolved by
         spelling through the shared table (WF-038). *)
      if filter <> None then Value.fault "a filter on a duration has no v0 form";
      match argumenten with
      | [ a; b ] ->
          (* the rest of the WF-038 clause in the verifier's order and
             sentences, read off the static types before any value: two dates
             of one precision, then the counted unit on the node type *)
          let ta = static_type env a and tb = static_type env b in
          List.iter
            (function
              | Some d when not (Ir_v0.Checks.is_date d) ->
                  Value.fault "%s" (Ir_v0.Checks.duration_operand_refusal kind d)
              | _ -> ())
            [ ta; tb ];
          (match (ta, tb) with
          | Some da, Some db when da <> db ->
              Value.fault "%s" (Ir_v0.Checks.duration_mixed_precision kind)
          | _ -> ());
          let unit_ =
            match typ with
            | None -> Value.fault "%s" (Ir_v0.Checks.duration_without_node_type kind)
            | Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ })
              -> (
                match Ir_v0.Checks.time_unit_of_eenheid u with
                | Some tu ->
                    if
                      Ir_v0.Checks.time_unit_sub_day tu
                      && (ta = Some Datum_in_dagen || tb = Some Datum_in_dagen)
                    then Value.fault "%s" (Ir_v0.Checks.duration_sub_day_on_day_dates kind)
                    else `Calendar tu
                | None -> (
                    (* not a calendar unit: the spelling may name a declared
                       day kind, which is the ch. 3.8 count over (A, B]
                       (§6.10) *)
                    match day_kind_of_unit env u with
                    | Some k -> `Day_kind k
                    | None ->
                        Value.fault "%s"
                          (Ir_v0.Checks.duration_unknown_unit kind
                             (Option.value u.afkorting ~default:"?"))))
            | Some _ -> Value.fault "%s" (Ir_v0.Checks.duration_without_time_unit kind)
          in
          (* ch. 5.11, wire order: "van" (argument 0) before "tot"
             (argument 1). Written as two positional arguments this build
             evaluates "tot" first — the reverse of the phrase. *)
          let van = eval_expr env world self ~err a in
          let tot = eval_expr env world self ~err b in
          let absolute = kind = Absolute_tijdsduur in
          let result_unit = unit_of_datatype typ in
          (match unit_ with
          | `Calendar tu -> Arith.tijdsduur ~absolute ~unit_:tu ~result_unit van tot
          | `Day_kind k ->
              Arith.tijdsduur_day_kind ~absolute
                ~matches:(day_kind_pred env world self ~err k)
                ~result_unit van tot)
      | _ -> Value.fault "duration expects two arguments")
  | Functie { kind; argumenten; element_objecttype; leegwaarde; filter; dimensie_selectie; granulariteit; bron_rol; bron_pad; typ = _; bron = _ } ->
      eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
        ~dimensie_selectie ~granulariteit ~bron_rol ~bron_pad
  | Tekst_reeks { delen; _ } ->
      Arith.concat
        (map_in_order
           (function
             | Letterlijk { tekst } -> Value.Text (Option.value tekst ~default:"")
             | Ingevoegd { expr = e } -> eval_expr env world self ~err (req "EmbeddedExpression.expr" e))
           delen)
  | Expr_onopgelost _ ->
      Value.fault "Unresolved reached in evaluation: this rule fails observably"
  | Constante { naam = Some n; _ } when n = Ir_v0.Spelling.evaluation_date -> (
      (* ch. 8.2: the execution-date context value — the only
         expression-level access to the evaluation date *)
      match env.as_of with
      | Some d -> Value.date_of_iso d
      | None ->
          Value.fault
            "the evaluationDate context value needs an evaluation date; the case carries asOfDate \
             null")
  | Constante { naam = Some n; _ } when n = Ir_v0.Spelling.evaluation_year -> (
      (* ch. 8.2: the execution year is derived from the session's one date,
         never supplied beside it — yearOf(evaluationDate), a whole unitless
         number — so a session cannot carry a year its date contradicts *)
      match env.as_of with
      | Some d -> Arith.jaar_uit (Value.date_of_iso d)
      | None ->
          Value.fault
            "the evaluationYear context value needs an evaluation date; the case carries asOfDate \
             null")
  (* any other name is outside WF-098's closed set, spelled byte for byte —
     the bound day of a day-kind definition included, which is a BoundDay.
     An executor's loader refuses such a node, so only one built by hand
     arrives here, and it is refused in the loader's own sentence, never
     read as the session value or the day it resembles *)
  | Constante { naam = Some n; _ } when Ir_v0.Checks.context_value_refusal n <> None ->
      Value.fault "%s" (Option.get (Ir_v0.Checks.context_value_refusal n))
  | Bound_day _ -> (
      (* ch. 3.7/3.8: the BOUND DAY of a day-kind definition (BoundDay) —
         the day the defining predicate is being applied to. It is not a
         session value (that is evaluationDate, above) and it has a value only
         inside a day-kind evaluation, which is exactly where
         [day_kind_binding] puts one. *)
      match SMap.find_opt day_kind_binder env.locals with
      | Some (Now v) -> v
      | Some (Later f) -> f ()
      | None ->
          Value.fault
            "a BoundDay is the bound day of a day-kind definition (ch. 3.7); outside a day-kind \
             evaluation it denotes nothing")
  | Dimensie_cel _ | Variabele_ref _ | Constante _ ->
      Value.fault "expression kind not implemented in this card"
  | Temporal_read { basis; datum; typ = _; bron = _ } -> (
      (* ch. 3.3, the point-in-time read: read(tl, d). The base is never
         evaluated as a scalar (that is the refused shape); it names the
         timeline. An empty date index reads to leeg; a gap reads to leeg
         (ch. 3.1); a case that supplies only the asOfDate-bucket scalar
         cannot serve a read — fault, never a guess. *)
      match eval_expr env world self ~err datum with
      | Value.Empty -> Value.Empty
      | Value.Date d -> (
          match basis with
          | Attribuut_ref
              { attribuut = Some a; objecttype = Some otn; navigatie_bindingen = None; _ }
            when own_read env self otn ->
              if not (Ir_v0.Checks.attr_temporal (objecttype env otn) a) then
                Value.fault "temporal read of %S, which is not a temporal attribute" a;
              (match World.timeline self a with
              | Some tl -> Timeline.read tl d
              | None ->
                  Value.fault
                    "the temporal read of %S needs the full timeline; the case supplies none \
                     (the asOfDate-bucket scalar cannot serve a point read)"
                    a)
          | Parameter_ref { parameter = Some p; _ } -> (
              (match Ir_v0.Checks.find_ci env.doc.parameters p with
              | Some { par_granulariteit = Some _; _ } -> ()
              | Some _ -> Value.fault "temporal read of parameter %S, which is not granular" p
              | None -> Value.fault "unknown parameter %S" p);
              match SMap.find_opt (norm p) env.param_tls with
              | Some tl -> Timeline.read tl d
              | None ->
                  Value.fault
                    "the temporal read of parameter %S needs its timeline in the case" p)
          | _ -> Value.fault "unsupported temporal-read base shape")
      | v ->
          Value.fault "temporal-read date operand is %s, must be a day-precision date"
            (Value.to_string v))
  | Construct { constructor_; argument; _ } -> (
      (* ch. 9.1: a leeg argument propagates — a constructor value never
         carries a leeg payload; a nullary constructor (argument absent) is
         gevuld by construction *)
      match argument with
      | None -> Value.Ctor (constructor_, None)
      | Some a -> (
          match eval_expr env world self ~err a with
          | Value.Empty -> Value.Empty
          | v -> Value.Ctor (constructor_, Some v)))
  | Match { scrutinee; arms; _ } -> (
      (* ch. 9.2: evaluate the scrutinee; a leeg scrutinee propagates leeg
         (no implicit leeg-arm); otherwise the unique arm for the tag, with
         its binder bound to the payload for this evaluation only *)
      match eval_expr env world self ~err scrutinee with
      | Value.Empty -> Value.Empty
      | Value.Enum label -> (
          (* ch. 9.2, the enumeration scrutinee: the SAME two steps with the
             label in place of the tag (the keuzetype family, IR-SPEC ch. 2.4).
             The declared alternatives are nullary, so no binder is bound; the
             label match is ci, the norm WF-063 checked the arms under. *)
          match List.find_opt (fun (c, _, _) -> norm c = norm label) arms with
          | None ->
              Value.fault
                (* Unreachable on a verifier-clean document whose case input
                   the shared loader admitted (case.ml refuses a label outside
                   the declaration): WF-063 makes the arms cover the
                   declaration of the domain this node's type names, WF-088
                   makes the use site's inline copy repeat that declaration,
                   WF-089 makes that domain the one of the declaration the
                   value COMES FROM — the reference positions (attribute,
                   dimension cell, parameter, binder and the fold source,
                   IR-SPEC ch. 2.4) and the element-passing aggregation, whose
                   own type is crossed against its operand so the fold cannot
                   launder a domain — and the write half of the same identity
                   makes every value ENTERING that declaration name its domain
                   — WF-023 at a rule target, an object-creation member and a
                   defaults consequence, WF-067 for a function body. The read
                   half alone was measured insufficient (fix round 1), and the
                   reference half alone was too (fix round 2: the aggregation
                   and its source). Every composite scrutinee route is crossed
                   with full datatype equality (Let WF-076, IfThenElse WF-069,
                   Match WF-064, Invoke WF-068, struct WF-073/074, fold family
                   WF-070). Kept as an honest fault, never an assert: an
                   unverified document can still reach it. *)
                "match has no arm for enumeration value %S (unreachable on a verifier-clean document)"
                label
          | Some (_, Some b, _) ->
              Value.fault "match arm for enumeration value %S carries a binder %S" label b
          | Some (_, None, body) -> eval_expr env world self ~err body)
      | Value.Ctor (tag, payload) -> (
          match List.find_opt (fun (c, _, _) -> norm c = norm tag) arms with
          | None ->
              Value.fault
                (* the sum-side twin of the enumeration arm above, unreachable
                   on a verifier-clean document by the same rules: WF-063
                   totality over the named domain's declaration, WF-061 for the
                   use-site copy, WF-089 for the domain of the declaration the
                   value COMES FROM — the reference positions, the fold source
                   and the element-passing aggregation alike — and the write
                   half of that identity — WF-023 at a rule target, an
                   object-creation member and a defaults consequence, WF-067
                   for a function body — for every value ENTERING that
                   declaration. An honest fault for an unverified document,
                   never an assert. *)
                "match has no arm for constructor %S (unreachable on a verifier-clean document)"
                tag
          | Some (_, binder, body) ->
              let locals =
                match (binder, payload) with
                | None, None -> env.locals
                | Some b, Some v -> SMap.add (norm b) (Now v) env.locals
                | Some _, None ->
                    Value.fault "match arm for nullary constructor %S carries a binder" tag
                | None, Some _ ->
                    Value.fault "match arm for unary constructor %S lacks a binder" tag
              in
              (* the binder's static type is the matched constructor's declared
                 payload, read off the scrutinee's own type *)
              let btypes =
                match binder with
                | None -> env.btypes
                | Some b ->
                    let payload_t =
                      match static_type env scrutinee with
                      | Some (Sum_type { constructoren; _ }) -> (
                          match List.find_opt (fun (c, _) -> norm c = norm tag) constructoren with
                          | Some (_, p) -> p
                          | None -> None)
                      | _ -> None
                    in
                    SMap.add (norm b) payload_t env.btypes
              in
              eval_expr { env with locals; btypes } world self ~err body)
      | v -> Value.fault "match scrutinee is %s, not a constructor value" (Value.to_string v))
  | Binder_ref { naam; _ } -> (
      (* ch. 9.2: the nearest enclosing binder; WF-064 makes the miss
         statically impossible, so an unbound read is a fail-fast fault. A
         [Later] binding is a ch. 10.6 need: reading it forces the value (once)
         and raises the owning Let's barrier if that value is leeg. *)
      match SMap.find_opt (norm naam) env.locals with
      | Some (Now v) -> v
      | Some (Later read) -> read ()
      | None -> Value.fault "unbound binder %S (the verifier should preclude this)" naam)
  | Let { binder; value; body; _ } ->
      (* ch. 10.6, the three clauses. (1) LAZY, ONCE: [value] is evaluated in
         the ENCLOSING scope (WF-076 — the binder is invisible in its own
         value) at the first read of the binder, and [Lazy] memoises that one
         result for every further read, so a value whose evaluation deposits a
         §9.5 channel fact deposits it exactly once. (2) THE BARRIER AT THE
         READ: a read whose value is leeg raises [Barrier], caught HERE, so the
         whole Let is leeg — the body binder is never leeg, which is what keeps
         WF-077's condition operands total. [Barrier] is a LOCAL exception,
         freshly generated per evaluation of this arm, so a nested Let's
         barrier passes an enclosing handler by (it belongs to the Let that
         bound the binder that was read). (3) AN UNREAD BINDER EVALUATES
         NOTHING: a body that never reads the binder never forces [memo] — no
         leeg, no fault, no channel fact — and its outcome IS the Let's. *)
      let exception Barrier in
      let memo = lazy (eval_expr env world self ~err value) in
      let read () = match Lazy.force memo with Value.Empty -> raise Barrier | v -> v in
      let locals = SMap.add (norm binder) (Later read) env.locals in
      let btypes = SMap.add (norm binder) (static_type env value) env.btypes in
      (try eval_expr { env with locals; btypes } world self ~err body with Barrier -> Value.Empty)
  | Struct_literal { velden; _ } ->
      (* ch. 11.1: a record value; a field evaluating leeg propagates — the
         whole literal evaluates leeg (a record carries no hole). The field
         order is the wire's declaration order (WF-073). *)
      let fs = map_in_order (fun (n, e) -> (n, eval_expr env world self ~err e)) velden in
      if List.exists (fun (_, v) -> v = Value.Empty) fs then Value.Empty else Value.Record fs
  | Field_access { base; veld; _ } -> (
      (* ch. 11.1: read one declared field; a leeg base propagates leeg
         (reading a field of an absent record is leeg). The projection is by
         ci field name (WF-074), positional over the record's fields. *)
      match eval_expr env world self ~err base with
      | Value.Empty -> Value.Empty
      | Value.Record fs -> (
          match List.find_opt (fun (n, _) -> norm n = norm veld) fs with
          | Some (_, v) -> v
          | None ->
              (* Two rules have to hold together for this to be unreachable on
                 a verifier-clean document, and both landed 2026-09-03 —
                 measured, because the first alone was not enough.

                 WF-090 (struct-domain identity, IR-SPEC ch. 2.4) settles WHICH
                 DOMAIN the record belongs to: WF-074 crosses the field against
                 the struct domain the BASE NAMES and WF-073 a literal against
                 the domain IT names, and WF-090 adds the two crossings neither
                 does — every struct-typed READ (attribute, dimension cell,
                 parameter, binder, fold source, element-passing aggregation)
                 names the domain of the declaration it reads, and every struct
                 value WRITTEN into a declaration (rule target, object-creation
                 member, defaults consequence) names that declaration's domain.
                 Before it, a StructLiteral of domain B assigned to an
                 attribute of domain A was verifier-clean and landed here while
                 the ASP backend answered wrong (the wf090 cases under
                 corpus/negatief).

                 WF-072's widened walk (same chapter, same day) settles WHICH
                 FIELDS that domain has ON THE WIRE. It has to, because the
                 loader below does not consult the declaration: value_of_json
                 builds a record against the INLINE `velden` of the use site
                 (case.ml), so before the walk widened, an attribute typed
                 ListType<StructType dossier> with a renamed inline copy let a
                 case input through whose fields the declaration does not carry
                 — verifier-clean, ref faulting HERE, clingo passing green
                 (measured, fix round 1). The walk now pins every StructType in
                 the model and expression layers — through a list element,
                 inside `domeinen`, at a FunctionDef, at every expression
                 `type` — to its declaration, so the loader's cross IS the
                 declaration's. A rule TARGET's `type` stays outside that walk
                 (WF-022's lvalue, not an expression node), as it does under
                 WF-088: measured, drift there does not reach this fault —
                 the loader reads the ATTRIBUTE's declared datatype and both
                 executors write through it.

                 What stays outside the WF layer, named rather than claimed
                 away: a document that was never verified, and a case input the
                 loader itself refuses (it fails fast on a missing, extra or
                 wrong-typed field — not a WF rule, but the same fence in both
                 executors). Kept as an honest fault, never an assert. *)
              Value.fault
                "field %S is not present on the record value (unreachable on a verifier-clean document)"
                veld)
      | v -> Value.fault "field access base is %s, not a record value" (Value.to_string v))
  | List_literal { elements; _ } ->
      (* ch. 10.1: a list value; an element evaluating leeg propagates —
         the whole literal evaluates leeg (a list carries no hole) *)
      let vs = map_in_order (eval_expr env world self ~err) elements in
      if List.exists (fun v -> v = Value.Empty) vs then Value.Empty else Value.Lijst vs
  | Lambda _ ->
      (* WF-069: a Lambda is position-restricted — only the [function] operand
         of a Fold/MapList, evaluated there via [apply_fold_fn], never on its
         own as a value *)
      Value.fault "a Lambda is position-restricted: it is only a Fold/MapList operand"
  | If_then_else { condition; then_; else_; _ } ->
      (* ch. 10.2: evaluate the condition under the ordinary comparison
         discipline, then exactly the chosen branch — strict, no coalesce *)
      if eval_cond env world self ~err condition then eval_expr env world self ~err then_
      else eval_expr env world self ~err else_
  | Invoke { functie; arguments; _ } -> (
      (* ch. 10.2: evaluate arguments strictly (a leeg argument propagates
         without entering the body); bind the parameters in a fresh locals —
         the body is closed (WF-067) — and evaluate it. No recursion (WF-067),
         so termination is structural *)
      let fd = function_def env functie in
      if List.length arguments <> List.length fd.fd_parameters then
        Value.fault "function %S invoked with %d arguments, expected %d" functie
          (List.length arguments) (List.length fd.fd_parameters);
      let args = map_in_order (eval_expr env world self ~err) arguments in
      if List.exists (fun v -> v = Value.Empty) args then Value.Empty
      else eval_closed_body env world self ~err fd.fd_parameters fd.fd_body args)
  | Fold { list; functie; init; _ } -> (
      (* ch. 10.2: a left fold, order normative. A leeg list operand and a
         leeg init both propagate; fold(f, init, []) = init *)
      match eval_expr env world self ~err list with
      | Value.Empty -> Value.Empty
      | Value.Lijst xs -> (
          match eval_expr env world self ~err init with
          | Value.Empty -> Value.Empty
          | acc0 -> List.fold_left (fun acc x -> apply_fold_fn env world self ~err functie [ acc; x ]) acc0 xs)
      | v -> Value.fault "fold list operand is %s, not a list value" (Value.to_string v))
  | Map_list { list; functie; _ } -> (
      (* ch. 10.2: elementwise, order-preserving; a leeg source propagates,
         and a body yielding leeg propagates the whole map (no hole) *)
      match eval_expr env world self ~err list with
      | Value.Empty -> Value.Empty
      | Value.Lijst xs ->
          let ys = map_in_order (fun x -> apply_fold_fn env world self ~err functie [ x ]) xs in
          if List.exists (fun v -> v = Value.Empty) ys then Value.Empty else Value.Lijst ys
      | v -> Value.fault "map-list operand is %s, not a list value" (Value.to_string v))
  | Filter_list { list; binder; condition; _ } -> (
      (* ch. 10.2: keep, in order, the elements the condition holds for with
         the binder bound to the element — the aggregation-filter precedent
         (a condition per contributor); a leeg source propagates *)
      match eval_expr env world self ~err list with
      | Value.Empty -> Value.Empty
      | Value.Lijst xs ->
          let element_t =
            match static_type env list with Some (List_type { element }) -> Some element | _ -> None
          in
          let btypes = SMap.add (norm binder) element_t env.btypes in
          Value.Lijst
            (filter_in_order
               (fun x ->
                 let locals = SMap.add (norm binder) (Now x) env.locals in
                 eval_cond { env with locals; btypes } world self ~err condition)
               xs)
      | v -> Value.fault "filter-list operand is %s, not a list value" (Value.to_string v))
  | List_length { list; _ } -> (
      (* ch. 10.2: the element count; [] counts 0; a leeg source propagates *)
      match eval_expr env world self ~err list with
      | Value.Empty -> Value.Empty
      | Value.Lijst xs -> Value.Num (Q.of_int (List.length xs), Value.Unitless)
      | v -> Value.fault "list-length operand is %s, not a list value" (Value.to_string v))
  | Instance_set_union _ ->
      (* WF-110 admits this node at ONE position — the single argument of an
         instance count — and the count reads it through its own contributor
         arm, never through this one. Reaching here means a union stood
         where a VALUE was wanted; it denotes instances and has none. *)
      Value.fault
        "an instance-set union stands where a value is read: it denotes the INSTANCES of a \
         §5.7 concatenation, and only an instance count consumes them"

(* apply a fold-family [function] operand (IR-SPEC ch. 4.8) — an inline
   lambda or a named FunctionDef — to already-evaluated arguments. Bodies are
   closed (WF-069/067), so a fresh locals holds exactly the parameters; a
   fold body never sees leeg (ch. 10.1), so no leeg-argument short-circuit is
   needed here (the Invoke arm carries the strict one). *)
and apply_fold_fn env world self ~err (fn : fold_fn) (args : Value.t list) : Value.t =
  match fn with
  | Fn_lambda { parameters; body; _ } -> eval_closed_body env world self ~err parameters body args
  | Fn_name name ->
      let fd = function_def env name in
      eval_closed_body env world self ~err fd.fd_parameters fd.fd_body args

(* bind [parameters] to [args] in a fresh locals (a closed body sees only its
   own parameters, WF-067) and evaluate [body]; arity is a WF-068/069/070
   invariant, refused fail-fast here rather than raising Invalid_argument *)
and eval_closed_body env world self ~err (parameters : (string * datatype) list) body
    (args : Value.t list) : Value.t =
  if List.length parameters <> List.length args then
    Value.fault "closed body applied to %d arguments, expected %d"
      (List.length args) (List.length parameters);
  let locals =
    List.fold_left2
      (fun acc (pname, _) v -> SMap.add (norm pname) (Now v) acc)
      SMap.empty parameters args
  in
  let btypes =
    List.fold_left (fun acc (pname, dt) -> SMap.add (norm pname) (Some dt) acc) SMap.empty parameters
  in
  eval_expr { env with locals; btypes } world self ~err body

(* WF-038's operand of a square root or an absolute value, read off its
   static type before the value is: the verifier's sentence, shared *)
and numeric_builtin_operand env kind a =
  match Option.bind (static_type env a) (Ir_v0.Checks.numeric_builtin_operand_refusal kind) with
  | Some sentence -> Value.fault "%s" sentence
  | None -> ()

and eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
    ~dimensie_selectie ~granulariteit ~bron_rol ~bron_pad =
  (* the die/dat subselection executes on the instance folds below; the
     SAME wire field also carries the §7.1 gedurende timeline clip (a
     filter on the temporal total, where elementType is the
     subject itself) — that form must never run as an instance
     subselection and faults in its own arm *)
  if dimensie_selectie <> None then Value.fault "dimension selection not implemented in this card";
  (* a granularity is a BuiltinCall facet since the L4 landing, read by the
     two bucketing builtins alone, and those run only as the whole value of a
     rule on a temporal target ([run_pro_rata]). On any other kind the facet
     is WF-037's refusal, in the verifier's sentence; a bucketing builtin met
     here is refused by name, never evaluated with its facet dropped *)
  (match (granulariteit, kind) with
  | None, _ -> ()
  | Some _, (Tijdsevenredig_deel | Aantal_dagen_in) ->
      Value.fault
        "a granularity-bucketed builtin call is executed only as the whole value of a rule on a \
         temporal target (ch. 3.7, 3.8)"
  | Some _, _ -> Value.fault "granularity facet on a non-bucketing kind");
  (* WF-102's three shape arms, mirrored here rather than assumed: a
     verifier-clean document cannot reach them, and a document that
     reaches an executor without passing the verifier must fault by name
     instead of being read as the one-step form with the range dropped —
     which is the silent misbinding this whole field exists to close.
     Arm (a) is the sharpest of the three and was the one initially
     missed: with both fields present the arm below simply WINS and the
     binding is never read, so the document replays clean while the ASP
     backend faults — a two-executor disagreement under a document no
     rule in this file refused. The order matters too: (a) first, so a
     document carrying both spellings is named for carrying both rather
     than for whatever else the surviving one happens to violate. *)
  (* WF-093 (b), the verifier's sentence, judged before the range's shape
     as the verifier and the Drools loader judge it: a count source names a
     role, and a name that is a MEMBER of the element type — an attribute or a
     characteristic — reads a value per contributor, which a count does not
     fold, however its range is spelled *)
  (match (kind, element_objecttype, argumenten) with
  | Aantal, Some el, [ Attribuut_ref { attribuut = Some a; navigatie_bindingen = None; _ } ]
    when (bron_rol <> None || bron_pad <> None)
         && (Ir_v0.Checks.find_ci (objecttype env el).attributen a <> None
            || Ir_v0.Checks.has_kenmerk (objecttype env el) a) ->
      Value.fault
        "the source of this count aggregation reads '%s', a member of the element type '%s': a \
         count folds INSTANCES, so its source is a count source naming the relation's role, never \
         a value read per contributor"
        a el
  | _ -> ());
  (match bron_pad with
  | None -> ()
  | Some pad ->
      if bron_rol <> None then
        Value.fault "sourcePath beside sourceRole: a fold carries one range, never two";
      (* WF-102 (c), the verifier's sentence: the set source is the single
         argument of an instance fold that carries a range, and a path is the
         range of a VALUE fold's set source only. A count's source names the
         one relation whose role it counts, so its range is one step. Judged
         before the arity, as clingo and the Drools loader judge it *)
      (match (kind, element_objecttype, argumenten) with
      | k, Some _, [ Attribuut_ref { navigatie_bindingen = None; _ } ] when k <> Aantal -> ()
      | _ ->
          Value.fault
            "a sourcePath is admissible only as the range of a single-argument VALUE fold's set \
             source — a count's range is one step, spelled sourceRole");
      if List.length pad < 2 then
        Value.fault
          "a sourcePath carries at least TWO steps, found %d — the one-step range is spelled \
           sourceRole"
          (List.length pad));
  (* WF-083 (c), the verifier's sentence: a total without a range is the
     subject's own read, so the type it folds over is the subject type. The
     field is checked here because a foreign type in it otherwise moves no
     answer and nothing else refuses it *)
  (match (kind, element_objecttype, argumenten, env.subj) with
  | Totaal, Some el, [ _ ], Some st when bron_rol = None && bron_pad = None && norm el <> norm st ->
      Value.fault
        "the elementType '%s' of a total without a range is not the fold subject '%s': \
         without a range the fold is the subject's own read, so its elementType must name the \
         subject type"
        el st
  | _ -> ());
  match (kind, argumenten) with
  | ( Totaal,
      [ Attribuut_ref { attribuut = Some a; objecttype = Some aot; navigatie_bindingen = None; _ } ] )
    when bron_rol = None && bron_pad = None
         && own_read env self aot
         && Ir_v0.Checks.attr_temporal (objecttype env aot) a -> (
      (* ch. 3.6, the temporal total — the one v0-mirrored bridge from a
         timeline to a scalar. The case carries the asOfDate bucket's
         value (corpus/golden/README.md); the collapse folds that
         one-bucket timeline: value × whole-units of its own grid.
         The absent range is load-bearing (D8 (iv)): a fold that carries
         one reads a SET, the §7.1 per-element fold, and on a
         self-relation (element type = subject type) the two shapes are
         otherwise indistinguishable here — this arm would then total the
         SUBJECT's own timeline and answer for the wrong instance. *)
      let ot_decl = objecttype env aot in
      let gran = attr_granulariteit ot_decl a in
      let grid = Timeline.grid_unit env.doc gran in
      (match filter with
      | Some c -> (
          (* ch. 3.6, the gedurende clip: intersect the value timeline
             with the condition's true-periods, then total whole units
             per clipped segment (floor, never pro-rata). A gap in the
             condition is clipped away; a fully clipped-away — or
             absent — value timeline totals 0, never leeg (the clip's
             presence turns absence into 0, the mirrored v0 nuance).
             The clip needs the FULL value timeline: a case that
             carries only the asOfDate-bucket scalar cannot serve it. *)
          let tl =
            match World.timeline self a with
            | Some tl -> tl
            | None ->
                if World.attr self a = Value.Empty then []
                else
                  Value.fault
                    "the gedurende clip of %S needs the full timeline; the case supplies only \
                     the asOfDate-bucket scalar"
                    a
          in
          let span = List.map (fun (s : Timeline.segment) -> (s.s_van, s.s_tot)) tl in
          let rec true_periods_of_cond (c : conditie) =
            match c with
            | Kenmerk_check { kenmerk = Some k; navigatie_binding = None; negatie; _ } ->
                if not (kenmerk_temporal ot_decl k) then
                  Value.fault
                    "gedurende over %S, which is not a temporal characteristic" k;
                let periods =
                  Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
                in
                if negatie then Timeline.complement periods ~span else periods
            | Vergelijking
                { op; links = Some (Attribuut_ref
                    { attribuut = Some b; objecttype = Some botn; navigatie_bindingen = None; _ });
                  rechts = Some grens; periode = None; _ }
              when own_read env self botn
                   && Ir_v0.Checks.attr_temporal ot_decl b ->
                (* the compared timeline is evaluated PER SEGMENT against
                   the scalar bound (piecewise-constant, never per day) *)
                let tested =
                  match World.timeline self b with Some t -> t | None -> []
                in
                let bound = eval_expr env world self ~err grens in
                Timeline.true_periods_of tested ~span (fun v ->
                    Arith.compare_op op v bound)
            | Samengesteld { kwantor = Alle; onderdelen = (_ :: _ as onderdelen); _ } ->
                (* ch. 3.6, the FLAT composite `all` clip: the
                   intersection of the members' true periods — v0 nests
                   the per-member clips over exactly such flat
                   characteristic lists. Members are restricted to
                   own-temporal-characteristic checks (WF-055): v0
                   partitions STATIC comparison members out as
                   value-side guards before its clip ever runs, markers
                   temporal-attribute comparisons and nested composite
                   bullets — our v1 narrows composite members to the
                   characteristic checks both executors carry (the
                   static-guard form and the nested bullet are recorded
                   loud flanks). *)
                let member_periods part =
                  match part with
                  | Kenmerk_check _ -> true_periods_of_cond part
                  | _ ->
                      Value.fault
                        "a composite gedurende member is an own temporal characteristic \
                         check (the static-guard and nested forms are recorded flanks)"
                in
                List.fold_left
                  (fun acc part -> Timeline.intersect_periods acc (member_periods part))
                  (member_periods (List.hd onderdelen))
                  (List.tl onderdelen)
            | _ ->
                Value.fault
                  "unsupported gedurende condition shape (an own temporal \
                   characteristic, a per-segment comparison of an own temporal attribute, \
                   or an all composite of such shapes)"
          in
          clip_member_qualifications c;
          let true_periods = true_periods_of_cond c in
          Timeline.totaal ~grid_unit:grid gran (Timeline.clip tl true_periods))
      | None -> (
      if World.timeline self a <> None then
        Value.fault
          "the temporal total of %S over a case-supplied full timeline is not exercised by any \
           corpus case; the asOfDate-bucket convention carries the total"
          a;
      match env.as_of with
      | None ->
          Value.fault
            "the temporal total of %S needs the evaluation date: the case carries the asOfDate \
             bucket's value"
            a
      | Some d -> (
          match World.attr self a with
          | Value.Empty -> Value.Empty (* absent timeline totals to leeg (ch. 3.6) *)
          | v ->
              let day = match Value.date_of_iso d with Value.Date z -> z | _ -> assert false in
              (* the period of the member's own grid that holds the evaluation
                 date: the calendar bucket, or the fixed start point's (ch. 3.2) *)
              let van, tot = Timeline.grid_bucket (Timeline.attr_grid env.doc ot_decl a) day in
              Timeline.totaal ~grid_unit:grid gran
                [ { Timeline.s_van = van; s_tot = tot; s_waarde = v } ]))))
  | _ ->
  (* non-temporal aggregation *)
  (* contributors: an instance fold over a role (element_objecttype set),
     or listed operand expressions *)
  let contributors () =
    (* the die/dat subselection (§13.4.2): the filter condition evaluates
       PER CONTRIBUTOR — the element instance is the condition's subject,
       never the outer fold subject. A contributor whose filter operand
       is leeg simply fails the condition (ch. 1.2d) and drops out. *)
    let keep (i : World.instance) =
      match filter with
      | None -> true
      | Some c -> eval_cond { env with subj = element_objecttype } world i ~err c
    in
    (* D8: the single argument of an instance fold that carries a range is
       its SET source. Under `count` it names the counted role and the count
       folds the reached INSTANCES; under every value fold and the filled
       count its attribute is read per contributor, on the element type. A
       set source never navigates: the range is its whole relation. *)
    let ranged = bron_rol <> None || bron_pad <> None in
    match (element_objecttype, argumenten) with
    | Some el, [ Attribuut_ref { navigatie_bindingen = None; _ } ] when ranged && kind = Aantal ->
        (* a source that names a member of the element type was refused at
           the head of this function (WF-093 (b)), so this one names a role *)
        `Instances
          (filter_in_order keep
             (fold_members ?binding:bron_rol ?path:bron_pad env world self ~element:el))
    | Some el, [ Attribuut_ref { attribuut; navigatie_bindingen = None; _ } ] when ranged ->
        let name = req "fold attribute" attribuut in
        let el_decl = objecttype env el in
        (* D8 (iii)/D9: the source's name is read per contributor, on the
           element type; a name that is no attribute there would read leeg on
           every member and answer a silent 0, so it is refused by name, as the
           verifier's source reading and the Drools loader refuse it: a
           characteristic (WF-080), a role reachable from the fold subject,
           which is a count source only a count folds (WF-093 (a)), and any
           other name, which declares nothing on the element type (WF-034). At
           a position without a subject any declared role name counts *)
        if Ir_v0.Checks.find_ci el_decl.attributen name = None then
          if Ir_v0.Checks.has_kenmerk el_decl name then
            Value.fault
              "'%s' is a characteristic of '%s', not an attribute — a characteristic endpoint is \
               ill-formed in an aggregation source position"
              name el
          else if
            match env.subj with
            | Some st -> Ir_v0.Checks.roles_named_from { doc = env.doc } ~from_type:st ~name <> []
            | None -> Ir_v0.Checks.is_role_name { doc = env.doc } name
          then
            Value.fault
              "value aggregation over %S, which is no attribute of %S: a value fold reads an \
               attribute per contributor, and a role name is a count source only a count folds"
              name el
          else
            Value.fault
              "'%s' is not a declared attribute of '%s': a value fold's source reads an attribute \
               of each contributor"
              name el;
        let members =
          filter_in_order keep
            (fold_members ?binding:bron_rol ?path:bron_pad env world self ~element:el)
        in
        (* §7.1: a set read of a TEMPORAL attribute folds TWICE — each
           member's own timeline collapses over time first (ch. 3.6, at
           that attribute's granularity), and only those scalars fold over
           the set. Without this the fold sees the segment list as no
           value at all and silently totals 0. A ranking or counting fold
           has no defined moment to rank, so it is refused rather than
           reading a value the timeline does not have at one instant. *)
        if not (Ir_v0.Checks.attr_temporal el_decl name) then
          `Values (List.map (fun i -> World.attr i name) members)
        else begin
          (match kind with
          | Som | Totaal -> ()
          | Aantal_gevulde ->
              (* ch. 3.4c states this exclusion for the FILLED COUNT normatively,
                 so the refusal that carries it has to be identifiable. The arm
                 below refuses on the same ground but is invariant over every kind
                 that reaches it — it names the position and the two carried kinds,
                 never the construct rejected — so a red case pinning it witnesses
                 minimum, maximum, eerste, laatste and this kind alike. Measured
                 2026-09-20: the red document with kind `minimum` matched the filled
                 count's pin word for word. Hence a sentence of its own, and it says
                 what is missing for THIS fold rather than what som and totaal have:
                 a count asks after PRESENCE, which is a question about an instant,
                 and a timeline answers it differently at every one.
                 The construct is named AFTER the interpolation on purpose — the
                 interpolation-free tail is all a pin can quote. *)
              Value.fault
                "the temporal attribute %S of a set has no moment at which a filled count can ask \
                 a timeline whether it is filled (ch. 3.4c); presence is a question about an \
                 instant, and only sum and total define the per-element collapse that would \
                 supply one"
                name
          | _ ->
              Value.fault
                "this aggregation over the temporal attribute %S of a set has no defined moment \
                 to fold (ch. 3.6: only sum and total carry the per-element timeline fold)"
                name);
          let gran = attr_granulariteit el_decl name in
          let grid = Timeline.grid_unit env.doc gran in
          (* the convention of the per-element fold (ch. 3.6/§8.4): every
             member must bring its OWN horizon, because the integration
             runs per member and there is no single bucket to borrow. The
             case format's full-timeline variant is the one self-describing
             form (it states its segments' bounds); the asOfDate-bucket
             SCALAR states no period at all, so a member carrying one is
             the exact "needs the one, finds the other" case of ch. 8.4 —
             refused, naming the member, never silently bucketed at the
             session date. An ABSENT member contributes nothing (leeg), and
             timelines mix freely with any other horizon-carrying form.
             The SUBJECT's own fold above keeps the opposite convention —
             the asOfDate bucket carries that total — and the asymmetry is
             deliberate: there the session date IS the horizon. *)
          `Values
            (map_in_order
               (fun (i : World.instance) ->
                 match World.timeline i name with
                 | Some tl -> Timeline.totaal ~grid_unit:grid gran tl
                 | None ->
                     if World.attr i name = Value.Empty then Value.Empty
                     else
                       Value.fault
                         "the per-element temporal total of %S over %S: the case supplies an \
                          asOfDate-bucket scalar, which carries no period of its own; a set fold \
                          needs each member's own timeline (ch. 3.6/§7.1, ch. 8.4)"
                         name i.label)
               members)
        end
    | _, (_ :: _ :: _ as args)
      when (match kind with Eerste | Laatste | Minimum | Maximum -> true | _ -> false) ->
        (* the articulated value fold (§5.8.3: ≥2 listed operands) is
           decided by argument SHAPE, not element absence — but ONLY for
           v0's listed family `minimum`/`maximum`/`first`/`last` (isGelisteFold:
           the scalar concatenatieFold path, where the resolver-stamped
           elementType is vestigial). For `sum`/`total`/`count` the
           element is load-bearing: v0's accumulate consumes it (joins
           the element, reads only argument 0), so a multi-operand form
           keeps the loud fault below — never a silent list fold v0
           would compute differently. *)
        if filter <> None then
          Value.fault "a filter on a listed-operand fold has no v0 form (the subselection \
                       ranges over instances)";
        `Values (map_in_order (eval_expr env world self ~err) args)
    (* WF-110 (c), the verifier's sentence: a count over a union names no
       source of its own. Out of reach through a well-formed document; until
       the card aantal-bronarm-en-drools-lader this evaluator counted the union
       beside a range alone as if the range were not written *)
    | _, [ Instance_set_union _ ]
      when kind = Aantal
           && (element_objecttype <> None || bron_rol <> None || bron_pad <> None || filter <> None) ->
        let named =
          (if element_objecttype <> None then [ "elementType" ] else [])
          @ (if bron_rol <> None then [ "sourceRole" ] else [])
          @ (if bron_pad <> None then [ "sourcePath" ] else [])
          @ if filter <> None then [ "filter" ] else []
        in
        Value.fault
          "a count over an instance-set union names no source of its own, and this node carries %s: \
           the union's selections carry the population, the relation and the subselection, one set \
           per selection, so a source here is a second claim on one node (ch. 4.3)"
          (String.concat " + " named)
    | None, [ Instance_set_union { branches; _ } ] ->
        (* the §5.7 concatenation (WF-110, RUNTIME-SEMANTICS ch. 3.4d): each
           branch contributes its own reach ∩ population, narrowed by its own
           filter at the contributor, and the MEMBERS are the union of those —
           deduplicated on instance identity, so an instance two branches both
           select counts ONCE. Dedup is by instance LABEL, keeping first-reach
           order, exactly as the flat union of a multi-step range does
           (ch. 3.4b); without it the answer would be the sum of the parts. *)
        (* the SECOND LINE for WF-110 (d): one population for the whole union.
           The wire refuses the shape and so does the frontend, so this arm is
           out of reach through a well-formed document — but without it this
           evaluator ANSWERED a union over two populations (measured
           2026-09-22 on `wf110-unie-ongelijke-elementtypes`: a silent 3,
           where both Drools branches refuse by name). ch. 4.3 says the rule
           refuses instead of picking an answer by side effect, and an
           executor that answers anyway makes that sentence untrue where it
           matters most — on a document that reached it without the wire. *)
        (match branches with
        | [] -> ()
        | first :: rest ->
            let el0 = req "instance-set branch element type" first.isb_element_type in
            List.iter
              (fun (b : instance_set_branch) ->
                let el = req "instance-set branch element type" b.isb_element_type in
                if norm el <> norm el0 then
                  Value.fault
                    "an instance-set union ranges over TWO populations (%S and %S): a count answers a \
                     cardinality over ONE declared type, and whether two selections can then share an \
                     instance is a question about the subtype lattice that deduplication would have to \
                     answer"
                    el0 el)
              rest);
        let seen = Hashtbl.create 16 in
        `Instances
          (filter_in_order
             (fun (i : World.instance) ->
               if Hashtbl.mem seen i.label then false
               else (
                 Hashtbl.add seen i.label ();
                 true))
             (List.concat_map
                (fun (b : instance_set_branch) ->
                  let el = req "instance-set branch element type" b.isb_element_type in
                  let binding = b.isb_source_role in
                  let bkeep (i : World.instance) =
                    match b.isb_filter with
                    | None -> true
                    | Some c -> eval_cond { env with subj = Some el } world i ~err c
                  in
                  filter_in_order bkeep
                    (fold_members ~binding env world self ~element:el))
                branches))
    | None, args ->
        if filter <> None then
          Value.fault "a filter on a listed-operand fold has no v0 form (the subselection \
                       ranges over instances)";
        `Values (map_in_order (eval_expr env world self ~err) args)
    | Some el, [ _ ] when (not ranged) && kind <> Totaal ->
        Value.fault
          "an instance fold over %S carries no range: its relation is written as sourceRole or \
           sourcePath and never derived"
          el
    (* WF-037, the verifier's two sentences: a fold that names its population
       and hands no source. Out of reach through a well-formed document; named
       so that a document which reaches this evaluator without the wire check
       is refused for what it is, and not by the residual arm below, whose
       sentence names no construct *)
    | Some el, [] ->
        Value.fault
          "a %s aggregation carries elementType \"%s\" but no source operand: an instance fold \
           ranges over a written source"
          (Ir_v0.Spelling.encode Ir_v0.Spelling.aggregation_kind kind)
          el
    | Some el, [ List_literal _ ] when kind = Aantal || kind = Aantal_gevulde ->
        Value.fault
          "a %s aggregation carries elementType \"%s\" and a ListLiteral operand: a listed value \
           is no source of instances"
          (Ir_v0.Spelling.encode Ir_v0.Spelling.aggregation_kind kind)
          el
    (* WF-037, the fold-source arm, in the verifier's sentence: the one
       argument of an aggregation that carries an elementType is an
       AttributeReference, and any other node there is refused by name. A
       list literal under a value fold keeps the residual arm (WF-091 refuses
       it on the wire), and a union under a count is WF-110 (c)'s, above *)
    | Some el, [ a ]
      when (match a with
           | Attribuut_ref _ | List_literal _ -> false
           | Instance_set_union _ -> kind <> Aantal
           | _ -> true) ->
        Value.fault
          "a %s aggregation carries elementType \"%s\" and %s operand: the source of an instance \
           fold is an AttributeReference"
          (Ir_v0.Spelling.encode Ir_v0.Spelling.aggregation_kind kind)
          el (Ir_v0.Spelling.a_node a)
    | Some _, _ -> Value.fault "unsupported instance-fold source shape"
  in
  let values () =
    match contributors () with
    | `Values vs -> vs
    | `Instances _ -> Value.fault "value aggregation over instances: only a count folds them"
  in
  match kind with
  | Aantal -> (
      (* a count, never empty (ch. 1.2e) *)
      match contributors () with
      | `Instances is -> Arith.agg_aantal is
      | `Values vs ->
          (* The second line of defence, and a refusal since 2026-09-23. Every
             shape that hands a count VALUES is fenced on the wire: the listed
             operand list of two or more by WF-037 (2026-09-20), and the count
             of one operand or none without an element type by WF-037
             (2026-09-23); since L2 a count's single ranged source counts
             instances (D8 (ii)) and never reaches this arm. No well-formed
             document reaches this line any more. Until that last date it
             answered — the number of FILLED values, the filled count's
             meaning under the wrong kind, and 1 on a single list literal
             whether the list held two elements or none — while clingo and
             both Drools branches refused. Keeping that answer behind the wire
             would leave the one executor that answers a form nobody may write
             answering it silently, so it refuses by name. *)
          Value.fault
            "a count folds INSTANCES and this node hands it %d value(s) instead — an operand \
             list or a count without an element type, both refused on the wire: counting them \
             would count the operands, or their filled ones, and never a population (ch. 3.4a)"
            (List.length vs))
  | Aantal_gevulde ->
      (* ch. 3.4c: the filled-value count. It reads a VALUE per contributor
         and counts the ones that are not leeg, so [values ()] is the whole
         of it — a set source hands them over, a source that names a role
         and no attribute is refused in the contributor arm above (WF-093),
         and every other source shape reaches that function's loud refusal.
         WF-037 puts the same fence on the wire; the refusal here stays as
         the second line of defence.
         Never empty: [agg_aantal] over the empty list is 0, the identity
         element of a rule over sets (ch. 1.3), and no `emptyFallback` is
         consulted because WF-037 admits none. *)
      Arith.agg_aantal (Arith.filled (values ()))
  | Som ->
      (* ch. 5.11, wire order: `arguments` stands before `emptyFallback` in the
         node, so the contributors evaluate first and the stand-in after them.
         WF-037 confines an emptyFallback to the som, so this is the only arm
         that ever evaluates one. *)
      let vs = values () in
      let fallback = Option.map (eval_expr env world self ~err) leegwaarde in
      Arith.agg_som ~fallback vs
  | Totaal -> Arith.agg_totaal (values ())
  | Minimum -> Arith.agg_min (values ())
  | Maximum -> Arith.agg_max (values ())
  | Eerste -> Arith.agg_eerste (values ())
  | Laatste -> Arith.agg_laatste (values ())
  | Wortel -> (
      match argumenten with
      | [ a ] ->
          numeric_builtin_operand env Wortel a;
          Arith.sqrt_ ~err (eval_expr env world self ~err a)
      | _ -> Value.fault "squareRoot expects one argument")
  | Absolute_waarde -> (
      match argumenten with
      | [ a ] ->
          numeric_builtin_operand env Absolute_waarde a;
          Arith.abs_ (eval_expr env world self ~err a)
      | _ -> Value.fault "absoluteValue expects one argument")
  | Concatenatie -> Arith.concat (map_in_order (eval_expr env world self ~err) argumenten)
  | Jaar_uit -> (
      match argumenten with
      | [ a ] -> Arith.jaar_uit (eval_expr env world self ~err a)
      | _ -> Value.fault "yearOf expects one argument")
  | Maand_uit -> (
      match argumenten with
      | [ a ] -> Arith.maand_uit (eval_expr env world self ~err a)
      | _ -> Value.fault "monthOf expects one argument")
  | Dag_uit -> (
      match argumenten with
      | [ a ] -> Arith.dag_uit (eval_expr env world self ~err a)
      | _ -> Value.fault "dayOf expects one argument")
  | Eerste_paasdag -> (
      match argumenten with
      | [ a ] -> Arith.eerste_paasdag (eval_expr env world self ~err a)
      | _ -> Value.fault "easterSunday expects one argument")
  | Tijdsduur | Absolute_tijdsduur ->
      (* unreachable: eval_expr routes both variants on the node-type unit *)
      assert false
  | _ -> Value.fault "function kind not implemented in this card"

(* ch. 3.10's two bounds, read once for BOTH of the chapter's cases — the
   scalar gate in [eval_cond] and the clip in [run_rule] — so the two can
   never disagree about what a bound may be or what it reads. Each bound
   comes back as [None] (absent: open), [Some None] (leeg: no day matches)
   or [Some (Some day)].

   What a bound may BE is WF-111 since 2026-09-23 (maintainer decision;
   until then ch. 3.10's mirror note, backend maturity): a literal or the
   subject's own scalar date attribute. The verifier refuses the rest, so
   the refusal below is fail-fast on a non-conforming input — kept, word for
   word the ASP backend's up to the phase, so the two executors carry one
   set and the differential never arbitrates a form only one reads.

   Both bounds are evaluated, from before to (ch. 5.11), and BEFORE any
   verdict: the order is normative and a fault in either bound belongs to
   it. *)
and venster_grenzen env world (self : World.instance) ~err vanaf tot =
  let bound side e =
    (match e with
     | Literal _ -> ()
     | Attribuut_ref
         { attribuut = Some a; objecttype = Some otn; navigatie_bindingen = None; _ }
       when own_read env self otn && not (Ir_v0.Checks.attr_temporal (objecttype env otn) a) ->
         ()
     | _ ->
         (* the phase tail is load-bearing, the WF-044 sisters' pattern:
            the ASP backend refuses the same bound in the same words and
            a pin that stopped before the tail could not tell the two
            phases apart, so a swap would pass unseen *)
         Value.fault
           "the %s bound of a period condition is neither a date literal nor an own scalar \
            date attribute of the subject, the only two shapes WF-111 admits: the document is \
            not well-formed. Declined at evaluation"
           side);
    eval_expr env world self ~err e
  in
  let v = match vanaf with Some e -> Some (bound "from" e) | None -> None in
  let t = match tot with Some e -> Some (bound "to" e) | None -> None in
  if v = None && t = None then
    Value.fault "a period condition without any bound has no window to gate on";
  let day = function
    | None -> None (* an absent bound is open *)
    | Some Value.Empty -> Some None (* a leeg bound never matches *)
    | Some (Value.Date z) -> Some (Some z)
    | Some other ->
        Value.fault
          "a period condition's bound is %s, which is not a day-precision date"
          (Value.to_string other)
  in
  (* both [day] calls can FAULT (a bound that is not a date), and OCaml
     fixes no evaluation order inside a tuple — so they are forced into
     ch. 5.11's order with a let, rather than left to the compiler. Not
     reachable today (WF-111 admits no bound shape that can fault, so the
     fault is a fail-fast belt), but it becomes the live question the day
     the general date expression lands: that is the first bound shape that
     can fault at evaluation, and then WHICH bound faults first is the
     observable ch. 5.11 order. *)
  let dv = day v in
  let dt = day t in
  (dv, dt)

and eval_cond env world (self : World.instance) ~err (c : conditie) : bool =
  match c with
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      let le = req "Comparison.left" links and re = req "Comparison.right" rechts in
      let l = eval_expr env world self ~err le in
      let r = eval_expr env world self ~err re in
      Arith.compare_op ?static_numeric:(static_numeric env le re) ~static_date:(static_date env le re) op l
        r
  (* ch. 3.9, the whole-period VALUE test — the second of the chapter's two
     specified collapses, and the door ch. 3.3's scalar refusal must not
     shut. Built 2026-09-16; until then this arm faulted by name while the
     chapter specified it, which is the gap ch. 3.12 recorded.

     Exactly one side reads a time-dependent attribute of the subject and
     the other is the scalar bound. When the temporal side stands on the
     RIGHT the operator is REVERSED — an operand swap, never a negation:
     "40 < de vergoeding" and "de vergoeding > 40" are one test, while the
     negation of either is a different one (leeg operands make both
     false). *)
  | Vergelijking { op; links; rechts; periode = Some gp; _ } ->
      let le = req "Comparison.left" links and re = req "Comparison.right" rechts in
      let temporal_side = function
        | Attribuut_ref
            { attribuut = Some a; objecttype = Some otn; navigatie_bindingen = None; _ }
          when own_read env self otn && Ir_v0.Checks.attr_temporal (objecttype env otn) a ->
            Some a
        | _ -> None
      in
      let attr, grens_e, op =
        match (temporal_side le, temporal_side re) with
        | Some a, None -> (a, re, op)
        | None, Some a -> (a, le, omgekeerd op)
        | Some _, Some _ ->
            Value.fault
              "whole-period comparison between TWO time-dependent operands has no ch. 3.9 \
               reading: the fold tests one timeline against a scalar bound"
        | None, None ->
            Value.fault
              "whole-period qualifier on a comparison whose operands are not a time-dependent \
               attribute of %S and a scalar bound"
              self.otype
      in
      let tested = Option.value (World.timeline self attr) ~default:[] in
      let grens = eval_expr env world self ~err grens_e in
      let g : Ir_v0.Ast.granulariteit = if gp.gp_jaar then Jaar else Maand in
      let verdict =
        Timeline.geheel_periode_waarde g ~tl:tested (fun v ->
            Arith.compare_op ?static_numeric:(static_numeric env le re)
              ~static_date:(static_date env le re) op v grens)
      in
      (* ch. 3.9: "Negation complements the folded result" *)
      if gp.gp_niet then not verdict else verdict
  (* ch. 3.9, the leeg and gevuld tests under the whole-period qualifier. The
     two arms below match `periode` away with `_`, which is why this one
     stands before them: until 2026-09-11 the qualifier was dropped there and
     the operand answered its scalar reading, and until 2026-09-26 this arm
     refused the form by name. The inner negation is per day (ch. 1.2), so it
     picks the sister test before the fold; the qualifier's own negation
     complements the fold's verdict. *)
  | Predicaat { soort = (Leeg | Gevuld) as soort; operand; negatie; periode = Some gp; _ } ->
      let tl = whole_period_operand env world self (req "Predicate.operand" operand) in
      let g : Ir_v0.Ast.granulariteit = if gp.gp_jaar then Jaar else Maand in
      let gevuld = (soort = Gevuld) <> negatie in
      let verdict = Timeline.geheel_periode_leeg_gevuld g ~gevuld ~tl in
      if gp.gp_niet then not verdict else verdict
  | Predicaat { soort = Leeg; operand; negatie; _ } ->
      let v = eval_expr env world self ~err (req "Predicate.operand" operand) in
      let is_empty = v = Value.Empty in
      if negatie then not is_empty else is_empty
  | Predicaat { soort = Gevuld; operand; negatie; _ } ->
      let v = eval_expr env world self ~err (req "Predicate.operand" operand) in
      let is_filled = v <> Value.Empty in
      if negatie then not is_filled else is_filled
  | Predicaat { soort = Dagsoort; dagsoort; operand; negatie; _ }
    when SMap.mem day_kind_binder env.locals ->
      (* A NESTED day-kind reference, inside a day-kind condition (ch. 3.7,
         the WF-085 reference graph): the §9.8 composition, "a day is a
         holiday when it is a Christmas day or …". It is carried on the
         BOUND DAY ONLY — a test on some other date is a recognised,
         unexecuted form, refused in the same words by the ASP backend (the
         narrow route, 2026-09-19: ref and the Drools day-kind compiler both
         answered it while the ASP backend refused it, and a form one
         executor computes and another refuses is the asymmetry this card
         exists to end). Outside a day-kind condition the predicate stays
         refused altogether (the arm below): ch. 5.4's framing is that a day
         kind is consulted as a stratum-0 classifier, never as a derived read
         in the rule fixpoint. The leeg rule is the source's own exception: a
         leeg value is never of a day kind, so the test is false rather than
         leeg-propagating. *)
      (match operand with
      | None -> ()
      | Some (Bound_day _) -> ()
      | Some _ ->
          Value.fault
            "a nested day-kind test on a date other than the bound day is a recognised, \
             unexecuted form (the \xc2\xa79.8 composition is carried on the bound day only)");
      let d =
        match SMap.find_opt day_kind_binder env.locals with
        | Some (Now v) -> v
        | Some (Later f) -> f ()
        | None -> assert false (* guarded by the [when] above *)
      in
      let hit =
        match d with
        | Value.Empty -> false
        | Value.Date z -> day_kind_matches env world self ~err (req "Predicate.dayKind" dagsoort) z
        | Value.Date_time t ->
            day_kind_matches env world self ~err
              (req "Predicate.dayKind" dagsoort)
              (fst (Arith.split_ms t))
        | v -> Value.fault "a day-kind test needs a date operand, got %s" (Value.to_string v)
      in
      if negatie then not hit else hit
  | Predicaat { soort = Dagsoort; dagsoort; _ } ->
      Value.fault
        "the day-kind predicate ('is een %s') is a recognised, unexecuted form outside a \
         day-kind definition (ch. 5.4): a day kind classifies a bare date at stratum 0, and no \
         executor reads one inside the rule fixpoint"
        (Option.value dagsoort ~default:"?")
  | Predicaat _ -> Value.fault "predicate kind not implemented in this card"
  | Kenmerk_check { kenmerk; navigatie_binding = None; negatie; eigenaar; periode; _ } ->
      owner_hosts env.doc ~otype:self.otype ~kenmerk eigenaar;
      let k = req "CharacteristicCheck.characteristic" kenmerk in
      (* [owner_hosts] above has already established that a `host`, where
         present, names the subject's own type — so the declaration this
         resolves against and the instance read are one object *)
      (match periode with
      | Some gp ->
          (* ch. 3.9, the specified collapse — NOT the scalar context
             ch. 3.3 refuses below *)
          whole_period_kenmerk env ~ot:self.otype ~inst:(Some self) ~kenmerk:k ~negatie gp
      | None ->
          if kenmerk_temporal (objecttype env self.otype) k then
            Value.fault "temporal characteristic %S in a scalar condition is refused (ch. 3.3)" k;
          let has = World.has_char self k in
          if negatie then not has else has)
  | Kenmerk_check { kenmerk; negatie; navigatie_binding = Some rb; eigenaar; periode; _ } -> (
      (* the navigated check (ch. 5.8): one to-one hop. The v0 asymmetry,
         mirrored deliberately: an ABSENT link makes the check false for
         the positive AND the negated form — unlike comparison and
         leeg/gevuld, which read the absent relation as leeg.
         Under a ch. 3.9 qualifier that asymmetry does NOT hold: the
         chapter lifts the ch. 1.2 leeg-navigation discipline there and
         gives the failed hop a null timeline, which passes vacuously. *)
      (* the owner is a HOSTING statement about the pattern, not about the
         reached type, so it is crossed against the subject here exactly as on
         the scalar arm — the characteristic itself still lives on the far side *)
      owner_hosts env.doc ~otype:self.otype ~kenmerk eigenaar;
      let k = req "CharacteristicCheck.characteristic" kenmerk in
      let role = bound_role env rb (* D53: the hop is the binding *) in
      if role.rol_meervoudig then
        Value.fault
          "the navigated check reaches the to-many role %S in a scalar condition"
          (Option.value role.rol_naam ~default:"?");
      (* the reached type, taken from the DECLARATION: on the qualified form
         the time-dependence cross must fire whether or not a link is there *)
      let reached = Option.value role.rol_objecttype_naam ~default:"" in
      match role_links world self role with
      | [] -> (
          match periode with
          | Some gp -> whole_period_kenmerk env ~ot:reached ~inst:None ~kenmerk:k ~negatie gp
          | None -> false)
      | [ target ] -> (
          match periode with
          | Some gp ->
              whole_period_kenmerk env ~ot:target.otype ~inst:(Some target) ~kenmerk:k ~negatie gp
          | None ->
              if kenmerk_temporal (objecttype env target.otype) k then
                Value.fault "temporal characteristic %S in a scalar condition is refused (ch. 3.3)"
                  k;
              let has = World.has_char target k in
              if negatie then not has else has)
      | _ ->
          Value.fault "to-one role %S links more than one instance on %S (ch. 5.5)"
            (Option.value role.rol_naam ~default:"?")
            self.label)
  | Samengesteld { kwantor; drempel; onderdelen; _ } ->
      (* the mirror note's second bound, checked BEFORE the short circuit
         below: the position is refused, not the reading, so a composite that
         would stop short of the window refuses all the same *)
      periode_onder_telkwantor kwantor onderdelen;
      (* ch. 5.10, the composite's SHORT CIRCUIT: the members are evaluated
         left to right in WIRE order and evaluation stops at the first member
         after which the outcome stands. A member never reached produces no
         fault and no §9.5 channel fact — which is the whole point: an earlier
         member gates a later one that would otherwise fault. The truth value
         is never affected; [settled] only reports an outcome the members still
         unread can no longer change. *)
      let n = List.length onderdelen in
      let verdict hits =
        match kwantor with
        | De | Alle -> hits = n
        | Geen -> hits = 0
        | Ten_minste -> hits >= drempel
        | Ten_hoogste -> hits <= drempel
        | Precies -> hits = drempel
      in
      (* [hits] over the [n - left] members already read, [left] still unread.
         One rule, no exception: every arm marks the moment the outcome can no
         longer change, and the arms together are the exhaustive list of those
         moments per quantifier (maintainer decision 2026-09-07). A threshold
         that already covers the whole member list therefore settles on the
         EMPTY prefix and reads nothing at all — vacuously true for at-most at
         d >= n, vacuously false for at-least and exactly above n. *)
      let settled ~hits ~left =
        match kwantor with
        | De | Alle -> if hits < n - left then Some false else None
        | Geen -> if hits > 0 then Some false else None
        | Ten_minste ->
            if hits >= drempel then Some true
            else if hits + left < drempel then Some false
            else None
        | Ten_hoogste ->
            if hits > drempel then Some false
            else if hits + left <= drempel then Some true
            else None
        | Precies -> if hits > drempel || hits + left < drempel then Some false else None
      in
      let rec read hits left parts =
        match settled ~hits ~left with
        | Some v -> v
        | None -> (
            match parts with
            | [] -> verdict hits
            | c :: rest ->
                let hit = eval_cond env world self ~err c in
                read (if hit then hits + 1 else hits) (left - 1) rest)
      in
      read 0 n onderdelen
  | Regelversie { regel; inconsistent = true; _ } ->
      World.channel_has world ~rule:(req "Regelversie.regel" regel)
  | Regelversie { inconsistent = false; _ } ->
      Value.fault
        "'is gevuurd' is not implemented (row 30: decided as a dialect-only fixpoint predicate)"
  | Role_membership { fact_type; role; counterpart_type; negated; _ } ->
      (* ch. 5.8a, the role check (D11, U12): true iff a link of [fact_type]
         exists in which the subject fills [role] and the filler of the OTHER
         role is an instance of [counterpart_type] (⊑, ch. 2.2; absent = that
         role's declared filler, which every linked instance already is) — an
         existence question over the relation, never a count and never a
         pick, so the number of links the far end holds is no part of it;
         [negated]: no such link exists. The subject's links are read over
         the OTHER role of the fact type, the ones the loader materialises
         from either spelling of a supplied link (ch. 5.7a); a reciprocal
         fact type's one role is its own other end. *)
      let own, other =
        match
          Ir_v0.Checks.resolve_binding { doc = env.doc } { rb_feittype = fact_type; rb_rol = role }
        with
        | Ok (_, own, other) -> (own, other)
        | Error e -> Value.fault "role check: %s" e
      in
      if not (fills env self.otype own) then
        Value.fault
          "role check: the subject %S does not fill the role %S of %S, so no link of it can \
           hold the subject there"
          self.otype role fact_type;
      let linked = role_links world self other in
      let linked =
        match counterpart_type with
        | None -> linked
        | Some ct ->
            (* a counterpart outside the other role's declared filler would
               narrow to nobody and answer a silent false: refused by name *)
            if Ir_v0.Checks.effective_ot env.doc ct = None then
              Value.fault "role check: the counterpart type %S is no declared object type"
                ct;
            if not (fills env ct other) then
              Value.fault
                "role check: the counterpart type %S is no subtype of %S, the filler the role %S \
                 declares"
                ct
                (Option.value other.rol_objecttype_naam ~default:"?")
                (Option.value other.rol_naam ~default:"?");
            List.filter (fun (i : World.instance) -> isa env i.otype ct) linked
      in
      let exists = linked <> [] in
      if negated then not exists else exists
  (* ch. 3.9a, the QUANTIFIER FOLD: a condition quantified over the members
     of a linked instance set. The contributors are the instance fold's,
     down to the helper — [fold_members] decides the population once, for
     the fold and for this node alike, so "which members" can never come
     apart between the two.

     Two different disciplines meet here, each taken from the node it
     belongs to. The CONTRIBUTORS are settled in full first, filter and all,
     exactly as an instance fold settles them (ch. 3.4a): the subselection
     says which members the sentence is about, and that question is not
     short-circuited by the answer. The MATRIX then short-circuits, exactly
     as a composite condition does (ch. 5.10): evaluation stops at the first
     contributor whose verdict settles the fold, so a member never reached
     produces no fault and no §9.5 channel fact. *)
  | Kwantorvouw
      { kv_kwantor; kv_drempel; kv_element_objecttype; kv_filter; kv_toets; kv_bron_rol;
        kv_bron_pad; _ } ->
      periode_onder_telkwantor kv_kwantor
        (List.filter_map Fun.id [ kv_filter; kv_toets ]);
      let el = req "QuantifierFold.elementType" kv_element_objecttype in
      let toets, drempel =
        quantifier_fold_shape ~kwantor:kv_kwantor ~drempel:kv_drempel ~toets:kv_toets
          ~bron_rol:kv_bron_rol ~bron_pad:kv_bron_pad
      in
      let keep (i : World.instance) =
        match kv_filter with
        | None -> true
        | Some c -> eval_cond { env with subj = Some el } world i ~err c
      in
      let members =
        filter_in_order keep
          (fold_members ?binding:kv_bron_rol ?path:kv_bron_pad env world self ~element:el)
      in
      (* ch. 3.9a's count, and it is ch. 5.10's machinery over the
         CONTRIBUTORS instead of over a written member list — `all` is
         [hits = n] and `none` is [hits = 0], so all five quantifiers are the
         same comparison and the same stop triggers. `none` stays the
         per-MEMBER complement and never the fold's: "geen van de bergers
         heeft gedurende het gehele jaar meer dan 40" asks each member to
         fail the matrix, so a member whose timeline does not cover the
         period fails it and does not block `none` either. The empty
         contributor set needs no arm of its own: `all` and `none` both read
         [hits = 0 = n], at-least and exactly are out of reach, at-most can
         no longer be exceeded. *)
      let n = List.length members in
      let verdict hits =
        match kv_kwantor with
        | De | Alle -> hits = n
        | Geen -> hits = 0
        | Ten_minste -> hits >= drempel
        | Ten_hoogste -> hits <= drempel
        | Precies -> hits = drempel
      in
      let settled ~hits ~left =
        match kv_kwantor with
        | De | Alle -> if hits < n - left then Some false else None
        | Geen -> if hits > 0 then Some false else None
        | Ten_minste ->
            if hits >= drempel then Some true
            else if hits + left < drempel then Some false
            else None
        | Ten_hoogste ->
            if hits > drempel then Some false
            else if hits + left <= drempel then Some true
            else None
        | Precies -> if hits > drempel || hits + left < drempel then Some false else None
      in
      let rec read hits left parts =
        match settled ~hits ~left with
        | Some v -> v
        | None -> (
            match parts with
            | [] -> verdict hits
            | m :: rest ->
                let hit = eval_cond { env with subj = Some el } world m ~err toets in
                read (if hit then hits + 1 else hits) (left - 1) rest)
      in
      read 0 n members
  | Periode_conditie { vanaf; tot; tot_inclusief; _ } ->
      (* ch. 3.10, the SCALAR target's evaluation-date gate — the chapter's
         first case, in its own six clauses: vanaf inclusive, tot exclusive,
         "tot en met" inclusive, an absent bound open, an UNSET evaluation
         date never matching, a LEEG bound never matching. The chapter's
         other case, the temporal target's clip, never reaches this arm:
         [run_rule] takes the window off the condition position before a
         subject is taken, because the case distinction is on the rule's
         TARGET and a condition sees none. The bounds are read by
         [venster_grenzen], the one reader both cases share, and BEFORE the
         verdict looks at the evaluation date. *)
      let dv, dt = venster_grenzen env world self ~err vanaf tot in
      (match (env.as_of, dv, dt) with
       | None, _, _ -> false (* an unset evaluation date never matches *)
       | _, Some None, _ | _, _, Some None -> false
       | Some d, v, t -> (
           (* the reader returns a Date or raises its own fault, so a
              "malformed asOfDate" arm here never fired (removed
              2026-09-23); a case's non-date asOfDate is refused at load *)
           match Value.date_of_iso d with
           | Value.Date today ->
               (match v with Some (Some z) -> z <= today | _ -> true)
               && (match t with
                   | Some (Some z) -> if tot_inclusief then today <= z else today < z
                   | _ -> true)
           | _ -> assert false))
  | Conditie_onopgelost _ -> Value.fault "Unresolved condition reached in evaluation"

(* --- day kinds as duration units (ch. 3.7/3.8) --------------------------
   Two steps stand between a unit SPELLING on a duration node and a verdict
   about one calendar day. Resolution: the spelling names a day kind as its
   name or its plural (trimmed, case-insensitive — IR-SPEC ch. 2.9), and
   the name names its definition in `dayKindDefinitions`. Evaluation: the
   definition's condition is an ordinary condition, evaluated with the day
   bound as the bound day (BoundDay) and with no other binder in scope — WF-031's purity
   clause is what makes that sound, since a pure condition never reaches the
   fact base and the `self` it is handed is never read. *)

and day_kind_definition env (name : string) : resolved_dagsoort =
  match
    List.find_opt
      (fun (d : resolved_dagsoort) ->
        match d.rd_naam with Some n -> norm n = norm name | None -> false)
      env.doc.resolved_dagsoorten
  with
  | Some d -> d
  | None -> Value.fault "day kind %S has no definition" name

and day_kind_matches env world self ~err (name : string) (day : int) : bool =
  if List.exists (fun n -> norm n = norm name) !day_kind_stack then
    Value.fault
      "day-kind reference cycle at %S: a day-kind definition that references itself has no \
       fixpoint"
      name;
  let def = day_kind_definition env name in
  let c = match def.rd_voorwaarde with
    | Some c -> c
    | None -> Value.fault "day kind %S has a definition without a condition" name
  in
  let saved = !day_kind_stack in
  day_kind_stack := name :: saved;
  Fun.protect
    ~finally:(fun () -> day_kind_stack := saved)
    (fun () ->
      let env =
        { env with locals = SMap.add day_kind_binder (Now (Value.Date day)) env.locals; subj = None }
      in
      eval_cond env world self ~err c)

(* the day kind a duration UNIT spelling names — its name or its plural,
   trimmed and case-insensitive (IR-SPEC ch. 2.9) — or None when no day kind
   is spelled so (then it is a calendar unit, or ill-formed) *)
and day_kind_of_unit env (u : eenheid) : string option =
  match u.afkorting with
  | None -> None
  | Some a ->
      List.find_map
        (fun (d : dagsoort) ->
          if String.trim d.dk_naam <> ""
             && List.exists (fun s -> norm s = norm a) (Ir_v0.Checks.dagsoort_spellingen d)
          then Some d.dk_naam
          else None)
        env.doc.dagsoorten

and day_kind_pred env world self ~err (name : string) : int -> bool =
  fun day -> day_kind_matches env world self ~err name day

let rule_name (r : resolved_regel) = Option.value r.rr_naam ~default:"<naamloos>"

(* --- rule forms --------------------------------------------------------- *)

(* run one form over its subjects; [body] gets the fresh instance and an
   error collector, returns (world, changed); collected §9.5 reasons land
   as channel facts for (rule × subject). Given [read], the subjects and
   the instance [body] gets come from that world while the writes and the
   channel facts land on [world]: a creation reads the settled world of
   the generation before its own, not what its own generation makes or
   links (ch. 5.5) *)
let over_subjects ?(exact = false) ?(behalve = []) ?read env world ~otype ~rule body =
  let isa a b =
    (if exact then norm a = norm b else isa env a b)
    (* the ch. 7.10 absorption: on these concrete types the rule IS a
       defaults base and the stack writes it, so firing here too would put
       two writers on one target *)
    && not (List.exists (fun t -> norm t = norm a) behalve)
  in
  List.fold_left
    (fun (w, changed) (snapshot : World.instance) ->
      let self = match read with Some _ -> snapshot | None -> World.get w snapshot.label in
      let pending = ref [] in
      let err reason = pending := reason :: !pending in
      let w, ch = body w self err in
      let w =
        List.fold_left
          (fun w (_ : string) -> World.insert_error w ~rule ~subject:self.label)
          w !pending
      in
      (w, changed || ch))
    (world, false) (World.of_type ~isa (Option.value read ~default:world) otype)

(* ch. 3.7: the pro-rata target — the one implemented temporal write.
   The value MUST be tijdsevenredig-deel over an own temporal attribute
   with a gedurende condition on an own temporal characteristic (the
   corpus shape; everything else faults). The base must be as coarse as
   or coarser than the bucket, and the target's declared granularity
   must equal the node's. An absent base timeline or a condition
   without true periods writes the EMPTY timeline (v0: a null base
   yields the empty list — the target exists, with no segments). *)
(* ch. 3.10's clip, applied to a producer's result before anything else
   looks at it: [venster] is the subject's window as [lo, hi) in day
   numbers, an open bound at the integer's end. The restriction cuts
   segments and keeps their values — Timeline.clip does exactly that — and
   an empty window (hi <= lo) leaves nothing. *)
let clip_venster venster result =
  match venster with
  | None -> result
  | Some (lo, hi) -> if hi <= lo then [] else Timeline.clip result [ (lo, hi) ]

let run_pro_rata ?venster env w (self : World.instance) ~ot ~attr waarde =
  let ot_decl = objecttype env ot in
  match waarde with
  | Functie
      { kind = Tijdsevenredig_deel;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some b; objecttype = Some bot; navigatie_bindingen = None; _ } ];
        filter = Some c; granulariteit = Some g; leegwaarde = None; dimensie_selectie = None; _ }
    when norm bot = norm ot ->
      let target_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit attr with
        | Some (Some tg) -> tg
        | _ -> Value.fault "pro-rata target %S without a granularity" attr
      in
      if target_gran <> g then
        Value.fault
          "pro-rata bucket granularity differs from the target's declared granularity";
      let base_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit b with
        | Some (Some bg) -> bg
        | _ -> Value.fault "pro-rata base %S is not a temporal attribute" b
      in
      if base_gran < g then
        Value.fault "pro-rata base granularity is finer than the bucket";
      (* ch. 3.2: the base is constant across a bucket because its grid nests
         in the target's, which a start point off the calendar breaks — fenced
         until a corpus case lands *)
      if
        (Timeline.attr_grid env.doc ot_decl attr).g_start <> Timeline.Kalender
        || (Timeline.attr_grid env.doc ot_decl b).g_start <> Timeline.Kalender
      then
        Value.fault
          "pro-rata onto or from a grid that does not lie on the calendar is fenced \
           (RUNTIME-SEMANTICS ch. 3.2)";
      (match Ir_v0.Checks.find_ci ot_decl.attributen b with
      | Some (Some (Numeriek { eenheid = Some _; _ } | Percentage { eenheid = Some _; _ })) ->
          (* ch. 3.7: a rate base restates by its exact factor FIRST —
             not executed here; refuse rather than prorate factor-free *)
          Value.fault "pro-rata over a unit-carrying base is not executed (no corpus case)"
      | _ -> ());
      clip_member_qualifications c;
      let true_periods =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie_binding = None; negatie = false; _ } ->
            if not (kenmerk_temporal ot_decl k) then
              Value.fault "gedurende over %S, which is not a temporal characteristic" k;
            Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
        | _ ->
            Value.fault
              "unsupported pro-rata condition shape (an own temporal characteristic; no corpus \
               case for more)"
      in
      let basis = Option.value (World.timeline self b) ~default:[] in
      let result = clip_venster venster (Timeline.pro_rata g basis true_periods) in
      (* ch. 7.8, the timeline arm: on a standing temporal target the
         fold decides only when its result is gevuld — an empty result
         (the temporal leeg) writes nothing and a case-supplied input
         timeline stands whole *)
      if
        result = []
        && List.mem (norm ot, norm attr)
             (Ir_v0.Desugar.standing_temporal_targets env.doc)
      then (w, false)
      else World.write_timeline w ~label:self.label ~attr result
  | Functie
      { kind = Aantal_dagen_in; argumenten = [];
        filter = Some c; granulariteit = Some g; leegwaarde = None; dimensie_selectie = None; _ }
    ->
      (* ch. 3.8 "aantal dagen in": per touched bucket the true-day
         count; WF-058 mirrors WF-056's facets — bucket equals the
         target's declared granularity, gedurende over an own temporal
         characteristic *)
      let target_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit attr with
        | Some (Some tg) -> tg
        | _ -> Value.fault "day-count target %S without a granularity" attr
      in
      if target_gran <> g then
        Value.fault
          "day-count bucket granularity differs from the target's declared granularity";
      clip_member_qualifications c;
      let true_periods =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie_binding = None; negatie = false; _ } ->
            if not (kenmerk_temporal ot_decl k) then
              Value.fault "gedurende over %S, which is not a temporal characteristic" k;
            Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
        | _ ->
            Value.fault
              "unsupported day-count condition shape (an own temporal characteristic; no corpus \
               case for more)"
      in
      (* the target's periods: the calendar's, or its fixed start point's
         (ch. 3.2) *)
      let result =
        clip_venster venster
          (Timeline.aantal_dagen_in_grid (Timeline.attr_grid env.doc ot_decl attr) true_periods)
      in
      (* the ch. 7.8 timeline arm, as for pro-rata *)
      if
        result = []
        && List.mem (norm ot, norm attr)
             (Ir_v0.Desugar.standing_temporal_targets env.doc)
      then (w, false)
      else World.write_timeline w ~label:self.label ~attr result
  | _ ->
      Value.fault
        "a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a \
         gedurende condition over an own temporal characteristic); no corpus case for more"

(* WF-022's value clause, held for every version that attributes a
   characteristic — a version without a presentation and without
   initialisation (IR-SPEC ch. 3.1): it writes the boolean constant true and
   nothing else. On this wire a negative decision on a characteristic exists
   only inside a defaults group, where it keeps the group from attributing; a
   flat rule with another value has no reading. Until 2026-09-23 the
   attribution arm below evaluated the value and let false write nothing:
   corpus/negatief/wf022-kenmerk-onwaar, forced past the verifier, answered
   with the characteristic absent without a word, and a computed boolean was
   written like the constant, while the ASP backend refused both at
   translation.

   This check is keyed on the version's kind, never on the target's
   declaration. Measured 2026-09-23 on jutterswet-penningdrempel with the
   attribute "aantal aangemelde vondsten" as target and false as value: the
   verifier of the day passed it, this check refused it, and so did the ASP
   backend. Since the L4 landing the verifier refuses that document as well,
   because WF-022 crosses a version's kind against its target's member. Where
   the verifier refuses the document this fault is fail-fast on a
   non-conforming input; in no case may it be softened to a fallback. The
   test of the constant is the verifier's own (the boolean variant true
   typed Boolean).

   What the check does not reach: a rule outside its validity window is
   skipped before it ([applies] comes first, in the ASP backend too), and a
   version of this kind with the value true on an attribute target passes it
   and writes a characteristic of the attribute's name, which no reader of
   that attribute sees; the verifier refuses that document (WF-022). *)
let kenmerkwaarde_constant_waar (r : resolved_regel) ~ot ~attr =
  match r.waarde with
  | Some (Literal { variant = Lv_boolean true; typ = Some Booleaan; _ }) -> ()
  | w ->
      let datatype_woord (t : datatype) =
        match t with
        | Booleaan -> "Boolean"
        | Tekst -> "Text"
        | Numeriek _ | Percentage _ -> "Number"
        | Datum_in_dagen | Datum_tijd -> "Date"
        | Enumeratie _ -> "Enumeration"
        | Sum_type _ -> "SumType"
        | List_type _ -> "ListType"
        | Struct_type _ -> "StructType"
      in
      (* the literal as the wire writes its value: {"<variant>": <value>} *)
      let variant_json (v : literal_variant) =
        match v with
        | Lv_boolean b -> Printf.sprintf "{\"boolean\": %b}" b
        | v ->
            Printf.sprintf "{\"%s\": %S}" (Ir_v0.Checks.literal_variant_key v)
              (Ir_v0.Loader.literal_lexeme v)
      in
      let waarde =
        match w with
        | Some (Literal { variant; typ = Some t; _ }) ->
            Printf.sprintf "the literal %s typed %s" (variant_json variant) (datatype_woord t)
        | Some (Literal { variant; typ = None; _ }) ->
            Printf.sprintf "the untyped literal %s" (variant_json variant)
        | Some _ -> "a computed value"
        | None -> "no value"
      in
      Value.fault
        "rule %S writes %S of %S as a characteristic, with %s: a version without a presentation \
         and without initialisation writes the boolean constant true and nothing else. Declined \
         at evaluation"
        (rule_name r) attr ot waarde

let run_rule env world (r : resolved_regel) =
  if not (applies env r.rr_geldigheid) then (world, false)
  else
    match r.doel with
    | Some (Attribuut_ref { attribuut; objecttype = Some ot; _ }) ->
        let attr = req "RuleVersion.target attribute" attribuut in
        let env = { env with subj = Some ot } in
        let temporal_target =
          match r.rr_soort with
          | Some Kenmerktoekenning -> false
          | _ -> Ir_v0.Checks.attr_temporal (objecttype env ot) attr
        in
        (* ch. 3.10's case distinction asks whether the TARGET carries a
           timeline, and a CHARACTERISTIC target carries one through
           `kenmerkGranulariteit` — a different table from the one
           [temporal_target] reads. That flag is keyed on the attribute side
           alone because the pro-rata dispatch below needs it so; reusing it
           here let the characteristic target through. Measured 2026-09-20 on
           a verifier-clean characteristic assignment whose target declares a `year`
           granularity and whose whole condition is the window: the ASP
           backend PLAYED the scalar gate on a timeline target (present
           inside the window, absent outside it) while both Drools branches
           named it the §8.4.3 clip. So the two tables are asked apart. *)
        let doel_tijdlijn =
          match r.rr_soort with
          | Some Kenmerktoekenning -> kenmerk_temporal (objecttype env ot) attr
          | _ -> temporal_target
        in
        (* ch. 3.10's CLIP (built 2026-09-23): on a timeline target the window
           is taken off the condition position — it gates nothing — and cuts
           the producer's result per subject instead. Two positions stay
           refused by name, both before any subject is taken: a
           characteristic target, whose timeline this executor never writes
           (ch. 3.11), and the window as a composite MEMBER, where the chapter
           says nothing about what the other members do to a timeline. *)
        let clip =
          if doel_tijdlijn && Option.fold ~none:false ~some:bevat_periode r.rr_voorwaarde then
            match (r.rr_soort, r.rr_voorwaarde) with
            | Some Kenmerktoekenning, _ ->
                Value.fault
                  "a period condition on the temporal characteristic target %S is ch. 3.10's \
                   CLIP over a characteristic timeline, and no temporal characteristic is written \
                   here (ch. 3.11), so there is no timeline to cut. Declined at evaluation"
                  attr
            | _, Some (Periode_conditie { vanaf; tot; tot_inclusief; _ }) ->
                Some (vanaf, tot, tot_inclusief)
            | _ ->
                Value.fault
                  "a period condition inside a composite on the temporal target %S: ch. 3.10 \
                   specifies the clip for the window as the rule's whole condition and says \
                   nothing about what the other members do to a timeline. Declined at evaluation"
                  attr
          else None
        in
        (* the window for one subject: a leeg bound puts no day in it *)
        let venster_van w self ~err (vanaf, tot, incl) =
          match venster_grenzen env w self ~err vanaf tot with
          | Some None, _ | _, Some None -> (0, 0)
          | dv, dt ->
              let lo = match dv with Some (Some z) -> z | _ -> min_int in
              let hi = match dt with Some (Some z) -> if incl then z + 1 else z | _ -> max_int in
              (lo, hi)
        in
        (* ch. 3.11: the only implemented temporal write is the ch. 3.7
           pro-rata target; every other temporal target refuses — never
           silently coerce a scalar into a timeline *)
        (match r.rr_soort with
        | Some Kenmerktoekenning ->
            if kenmerk_temporal (objecttype env ot) attr then
              Value.fault "temporal characteristic %S: temporal writes are not implemented" attr;
            kenmerkwaarde_constant_waar r ~ot ~attr
        | _ ->
            if temporal_target then
              match r.waarde with
              | Some (Functie { kind = Tijdsevenredig_deel | Aantal_dagen_in; _ }) -> ()
              | _ ->
                  Value.fault
                    "temporal attribute %S as a target: temporal writes are not implemented"
                    attr);
        (* SUBTYPING §4.1 rules 1 and 4: where inheritance brings a defaults
           stack to this target, this rule is that stack's BASE and is
           written by the group, not here. The exclusion is per CONCRETE
           type and empty on every target whose chain declares no group —
           which is every ordinary rule in a flat model. *)
        let behalve =
          match r.rr_soort with
          | Some Kenmerktoekenning -> []
          | _ -> Ir_v0.Desugar.absorbed_on env.doc ~otype:ot ~attr
        in
        over_subjects ~behalve env world ~otype:ot ~rule:(rule_name r) (fun w self err ->
            let fire =
              match (clip, r.rr_voorwaarde) with
              | Some _, _ -> true
              | None, Some c -> eval_cond env w self ~err c
              | None, None -> true
            in
            if not fire then (w, false)
            else
              match r.rr_soort with
              (* the value is the constant waar, checked before any subject
                 was taken ([kenmerkwaarde_constant_waar]) *)
              | Some Kenmerktoekenning -> World.set_char w ~label:self.label ~char:attr
              | Some (Gelijkstelling | Berekening) when temporal_target ->
                  let venster = Option.map (venster_van w self ~err) clip in
                  run_pro_rata ?venster env w self ~ot ~attr (req "RuleVersion.value" r.waarde)
              | Some (Gelijkstelling | Berekening) ->
                  let v = eval_expr env w self ~err (req "RuleVersion.value" r.waarde) in
                  (* ch. 7.8: on a standing target — and on a ch. 7.7
                     desugaring-domain target, whose defaults reading ref
                     replays natively — the rule decides like a default: a
                     leeg evaluation writes nothing (the init/input
                     decides); channel deposits of the evaluation
                     persist as in any ordinary evaluation *)
                  if
                    v = Value.Empty
                    && (List.mem (norm ot, norm attr)
                          (Ir_v0.Desugar.standing_targets env.doc)
                       || List.mem (norm ot, norm attr)
                            (Ir_v0.Desugar.desugared_targets env.doc))
                  then (w, false)
                  else
                    World.write_attr w ~label:self.label ~attr ~decl:(attr_decl env ~otype:ot ~attr) v
              | Some Initialisatie ->
                  (* writes only where the target is empty (ch. 5.4) — apart
                     from its own write. A case input and a value another
                     writer placed switch the guard off, so the computed value
                     wins whichever fires first; the initialisation's own
                     earlier write does not, so its value is re-evaluated
                     under freshness like any attribute write. Read off the
                     running world alone, the guard kept the value the
                     initialisation computed against a read that another rule
                     had not yet written: the document order decided *)
                  if not (World.init_may_write w ~label:self.label ~attr) then (w, false)
                  else
                    let v = eval_expr env w self ~err (req "RuleVersion.value" r.waarde) in
                    World.write_attr_init w ~label:self.label ~attr
                      ~decl:(attr_decl env ~otype:ot ~attr) v
              | None -> Value.fault "rule %S has no kind" (rule_name r))
    | _ -> Value.fault "rule %S: unsupported target shape" (rule_name r)

let run_verdeling env world (v : resolved_verdeling) =
  if not (applies env v.vd_geldigheid) then (world, false)
  else begin
    let name = Option.value v.vd_naam ~default:"<verdeling>" in
    let bron_type = req "AllocationVersion.sourceType" v.bron_type in
    let env = { env with subj = Some bron_type } in
    let doel_type = req "AllocationVersion.targetType" v.doel_type in
    let bron_attr = req "AllocationVersion.sourceAttribute" v.bron_attribuut in
    let doel_attr = req "AllocationVersion.targetAttribute" v.doel_attribuut in
    let methode = req "AllocationVersion.method" v.vd_methode in
    over_subjects env world ~otype:bron_type ~rule:name (fun w src cond_err ->
        let cond_ok =
          match v.vd_voorwaarde with
          | Some c -> eval_cond env w src ~err:cond_err c
          | None -> true
        in
        if not cond_ok then (w, false)
        else
          (* ch. 4.1: the recipient set is every DOELTYPE instance the
             connecting relation reaches — the role only supplies the
             relation. The two used to coincide, because WF-047 demanded
             the bound role's filler and doelType be the same name; since
             ch. 2.6 widened that to ⊑-compatibility the filler may be a
             STRICT SUPERTYPE, and the role then reaches instances this
             allocation does not target. Filtering is identity wherever the
             filler is doelType or below, which is every document written
             before the chain existed. *)
          let recipients =
            List.filter
              (fun (i : World.instance) -> isa env i.otype doel_type)
              (linked env w src ~binding:v.verbindende_rol)
          in
          let recs =
            List.map
              (fun (i : World.instance) ->
                {
                  Alloc.id = i.label;
                  weight = Option.map (fun a -> World.attr i a) v.rato_attribuut;
                  cap = Option.map (fun a -> World.attr i a) v.maximum_attribuut;
                  key = Option.map (fun a -> World.attr i a) v.groepeer_attribuut;
                })
              recipients
          in
          let alloc_errs = ref [] in
          let err ~id (_ : string) = alloc_errs := id :: !alloc_errs in
          let outcome =
            Alloc.allocate methode
              ~afnemend:v.afnemend ~decimals:v.afronding_decimalen
              ~pot:(World.attr src bron_attr) ~err recs
          in
          let w =
            List.fold_left (fun w id -> World.insert_error w ~rule:name ~subject:id) w !alloc_errs
          in
          match outcome with
          | Alloc.Nothing -> (w, false)
          | Alloc.Distributed (shares, rest) ->
              let decl = attr_decl env ~otype:doel_type ~attr:doel_attr in
              let w, ch =
                List.fold_left
                  (fun (w, ch) (id, share) ->
                    match share with
                    | Alloc.Excluded -> (w, ch) (* stays leeg, never 0 *)
                    | Alloc.Assigned q ->
                        let w, c =
                          World.write_attr w ~label:id ~attr:doel_attr ~decl
                            (Value.Num (q, Value.Unitless))
                        in
                        (w, ch || c))
                  (w, false) shares
              in
              let w, ch2 =
                match v.restant_attribuut with
                | None -> (w, false) (* undeclared: the rest lapses (a SHOULD, no WF rule) *)
                | Some ra ->
                    World.write_attr w ~label:src.label ~attr:ra
                      ~decl:(attr_decl env ~otype:bron_type ~attr:ra)
                      (Value.Num (rest, Value.Unitless))
              in
              (w, ch || ch2))
  end

(* ch. 5.5, relation creation: for each anchor whose condition holds,
   navigate the join chain (anchor → the hops of its path → target)
   over existing links, and link anchor and target in the created
   relation — both ends are written, so navigation from either side
   sees the link. Naturally idempotent: an already-present link is a
   no-op write, so a target reached along two paths links once. The
   symbolic form (IR-SPEC ch. 3.4, WF-028) is the whole of the relational
   content; the wire requires it (B7). The anchors, the guard and the
   chain are read on [read], the settled world of the generation before;
   the links land on [world] (ch. 5.5, generations). *)
let run_feit_creatie env ~read world (fc : resolved_feit_creatie) =
  if not (applies env fc.fc_geldigheid) then (world, false)
  else begin
    let name = Option.value fc.fc_naam ~default:"<feitcreatie>" in
    (* the anker: the one subject the guard can read on (WF-109) — a guard on
       the doel is a characteristic check and reads no attribute *)
    let env = { env with subj = fc.anker } in
    let ftn = fc.fc_feittype in
    let role rname = created_role env ~fact_type:ftn ~role:rname in
    let anker_rol = role fc.anker_rol in
    let doel_rol = role fc.doel_rol in
    (* ch. 5.5: the host splits the guard into the two readings this position
       has. On the doel it FILTERS the targets — so the chain is walked first
       and the condition asked once per target — and on the anker it GATES the
       anker, which short-circuits the chain exactly as before. *)
    let doel_guard = if guard_on_doel env.doc fc then fc.fc_voorwaarde else None in
    let anker_guard = if doel_guard = None then fc.fc_voorwaarde else None in
    over_subjects ~read env world ~otype:(req "FactCreationVersion.anchor" fc.anker) ~rule:name
      (fun w self err ->
        let fire =
          match anker_guard with Some c -> eval_cond env read self ~err c | None -> true
        in
        if not fire then (w, false)
        else
          let hop insts rb =
            List.concat_map (fun i -> role_links read i (bound_role env rb)) insts
          in
          let mids =
            List.fold_left
              (fun insts (t : tussenstap) -> hop insts t.ts_rol)
              [ self ] fc.tussenstappen
          in
          let targets = hop mids fc.join_rol in
          let targets =
            match doel_guard with
            | None -> targets
            | Some c -> List.filter (fun t -> eval_cond env read t ~err c) targets
          in
          List.fold_left
            (fun (w, ch) (t : World.instance) ->
              let w, c1 =
                World.add_link w ~label:self.label ~keys:(role_keys doel_rol)
                  ~many:doel_rol.rol_meervoudig ~target:t.label
              in
              let w, c2 =
                World.add_link w ~label:t.label ~keys:(role_keys anker_rol)
                  ~many:anker_rol.rol_meervoudig ~target:self.label
              in
              (w, ch || c1 || c2))
            (w, false) targets)
  end

(* ch. 5.5, object creation, a member of a recursive group included
   ([evaluate] runs every creation once per generation): for each subject
   whose condition holds, mint a deterministic instance of the created type,
   apply the member initialisations (values evaluated on the subject;
   characteristic constants), and link new object and subject over the
   uniquely-derivable connecting relation — both ends. The idempotence
   contract, mirrored from the pinned runtime: a 1:1 shape (the
   created-side role is to-one) guards on the absence of an already
   linked instance in the INPUT world — a case-supplied linked instance
   blocks the creation (the snapshot narrowing is a recorded delta:
   v0's guard is a live fact-base negation) — while the to-many and
   linkless shapes guard per (rule × subject) through the
   deterministic label.

   The subjects, the condition and the member initialisations are read on
   [read], the settled world of the generation before (ch. 5.5,
   generations): the value at the moment of making, which neither the
   other creations of the same generation nor the minted instance itself
   can move. Until 2026-10-09 they were read on the running world of a
   pass, so the moment of making was a position in the pass: measured on
   a recursive creation whose initialisation reads a copy of a count the
   rules derive from its own links, the copy rule written before the
   count left the initialisation leeg and written after it gave 0. The
   instance, its initialisations and the back-link land on [world].

   The creation budget (ch. 5.5): the minted instance gets a creation depth
   in [stratum], the creation's own stratum, recorded beside its label in
   World.mints: one more than the subject's depth there, where an input
   instance and an instance a lower stratum minted stand at 0, since a
   lower stratum is complete before this one starts (ch. 6.3). A mint at
   depth [creation_budget] + 1 is the fault, at the making. The depth
   follows the chain of makings and not the generation in which a mint
   happens, so a creation whose condition waits for a link or a value
   keeps its subject's depth. Until 2026-10-09 the budget was 100
   generations per stratum that mint or link: a chain whose every mint
   waited one generation for a link a fact creation lays faulted from its
   51st mint on, and a recursive fact creation faulted on a closure of 102
   hops. *)
let creation_budget = 100

let run_object_creatie env ~world0 ~idx ~stratum ~read world (oc : resolved_object_creatie) =
  if not (applies env oc.oc_geldigheid) then (world, false)
  else begin
    let name = Option.value oc.oc_naam ~default:"<objectcreatie>" in
    let nieuw = req "ObjectCreationVersion.createdType" oc.nieuw_type in
    let subject = req "ObjectCreationVersion.subject" oc.oc_subject in
    let env = { env with subj = Some subject } in
    let pair =
      (* the explicit back-link binding (extension, IR-SPEC ch. 3.3)
         wins over the type-pair derivation — the only route for a
         self-typed pair, whose derivation mirrors v0's null *)
      match oc.oc_verbindende_rol with
      | Some rb -> (
          match Ir_v0.Checks.resolve_binding { doc = env.doc } rb with
          | Ok (_, bound, other) -> Some (rb.rb_feittype, bound, other)
          | Error e -> Value.fault "%s" e)
      | None -> connecting_pair env ~from_type:subject ~to_type:nieuw
    in
    over_subjects ~read env world ~otype:subject ~rule:name (fun w self err ->
        (* A subject that a higher stratum minted cannot occur: a creation
           reads its subject's population strictly (ch. 6.1), so it sits at
           or above every creation that makes its subjects. Counting a depth
           from a stratum that has not run would be meaningless, so a
           violation is a fault of this interpreter, not of the document. *)
        let subject_depth =
          match SMap.find_opt self.label read.World.mints with
          | None -> 0
          | Some (k, d) when k = stratum -> d
          | Some (k, _) when k < stratum -> 0
          | Some (k, _) ->
              failwith
                (Printf.sprintf
                   "ref/lib/eval.ml, run_object_creatie: object creation \"%s\" of stratum %d meets \
                    the subject %s, which a creation of stratum %d minted"
                   name stratum self.label k)
        in
        let fire =
          match oc.oc_voorwaarde with Some c -> eval_cond env read self ~err c | None -> true
        in
        if not fire then (w, false)
        else begin
          (* keyed by the creation INDEX, never the (nullable) name —
             the same key the ASP backend's ocN(Subject) term uses, so
             two name-less creations can never collide into one label;
             the case loader reserves the "::" namespace for mints *)
          let label = Printf.sprintf "oc%d::%s" idx self.label in
          let blocked =
            match pair with
            | Some (_, new_role, _) when not new_role.rol_meervoudig -> (
                match SMap.find_opt self.label world0.World.instances with
                | Some i0 -> role_links world0 i0 new_role <> []
                | None -> false)
            | _ -> false
          in
          if blocked || SMap.mem label w.World.instances then (w, false)
          else begin
            let depth = subject_depth + 1 in
            if depth > creation_budget then
              Value.fault
                "creation budget exhausted: object creation \"%s\" of stratum %d would mint at \
                 creation depth %d, on %s — a stratum mints to depth %d, and a chain of creations \
                 without end is a fault, never a silent truncation (ch. 5.5)"
                name stratum depth self.label creation_budget;
            let w =
              World.add_instance w
                { World.label; otype = nieuw; attrs = SMap.empty; chars = [];
                  links = SMap.empty; tls = SMap.empty; ctls = SMap.empty }
            in
            let w = { w with World.mints = SMap.add label (stratum, depth) w.World.mints } in
            let w =
              List.fold_left
                (fun w (t : toekenning) ->
                  let m = req "MemberInit.member" t.toe_attribuut in
                  if t.toe_kenmerk then (
                    match t.toe_waarde with
                    | Some (Literal { variant = Lv_boolean true; _ }) ->
                        fst (World.set_char w ~label ~char:m)
                    | Some (Literal { variant = Lv_boolean false; _ }) -> w
                    | Some e -> (
                        (* Phase 5c (WF-027 relaxation): a characteristic
                           initialisation may also carry a boolean-typed
                           expression, evaluated on the creation subject exactly
                           as an attribute initialisation is (the RuleVersion
                           Kenmerktoekenning mirror) — true sets the mark on the
                           minted instance, false leaves it unset (closed-world,
                           an absent init). Empty joins the false flank: an
                           attribute initialisation writes an empty value
                           silently, and the closed-world reading (a missing
                           deposit) makes leeg and false indistinguishable to a
                           characteristic check — matching the ASP backend, where
                           an absent atom is both. The boolean constant above
                           stays the v0 form; a truly non-boolean value (a
                           number, a text) still faults fast. *)
                        match eval_expr env read self ~err e with
                        | Value.Bool true -> fst (World.set_char w ~label ~char:m)
                        | Value.Bool false | Value.Empty -> w
                        | v ->
                            Value.fault
                              "characteristic initialisation of %S is not boolean: %s"
                              m (Value.to_string v))
                    | None ->
                        Value.fault
                          "characteristic initialisation of %S without a value" m)
                  else
                    let v = eval_expr env read self ~err (req "MemberInit.value" t.toe_waarde) in
                    fst
                      (World.write_attr w ~label ~attr:m
                         ~decl:(attr_decl env ~otype:nieuw ~attr:m)
                         v))
                w oc.toekenningen
            in
            let w =
              match pair with
              | None -> w
              | Some (_, new_role, subj_role) ->
                  let w, _ =
                    World.add_link w ~label:self.label ~keys:(role_keys new_role)
                      ~many:new_role.rol_meervoudig ~target:label
                  in
                  let w, _ =
                    World.add_link w ~label ~keys:(role_keys subj_role)
                      ~many:subj_role.rol_meervoudig ~target:self.label
                  in
                  w
            in
            (w, true)
          end
        end)
  end

(* ch. 7.1/7.9: a defaults-group member's decision, and the reconciled outcome
   of a default chain — [Decided] (the highest deciding default), [Conflict] (a
   differing exception group, keyed by its default-index path, propagating to
   the root with no write), or [Undecided] (nothing decided; fall-through). *)
type defaults_decision = Char_dec of bool | Attr_dec of Value.t
type chain_result = Decided of defaults_decision | Conflict of string | Undecided

(* ch. 7.1/7.2, the defaults group on one target (a singleton group is
   the plain prioritized defaults): each member decides independently —
   highest default that decides wins, with fall-through and speculative
   deposits per ch. 7.1 — then the group acts once: equal decisions write
   once; differing decisions write nothing (the target keeps what the fact
   base holds) and deposit one §9.5 fact keyed by the target form
   (ch. 7.2, an end-state observation). Ch. 7.9: a default's exception
   group reconciles by the same rule recursively; a differing exception
   group propagates the conflict to the root — no write, one §9.5
   fact keyed by the target form plus the default-index path. The strata
   machinery (WF-051) has already put every descendant justification and
   consequence producer strictly below every member. *)
(* [owner] is the CONCRETE type whose effective stack [members] is
   (RUNTIME-SEMANTICS ch. 7.10): inheritance has already folded every
   inherited declaration into it, so the group runs on instances of exactly
   that type — reading the subject extensionally here as well would put the
   supertype's stack and the subtype's on one instance. Each member comes
   with the subject type every root default of it was declared on, and that
   default is evaluated there. [world0] is the input world the
   initialisation default's justification reads (ch. 7.3). *)
let run_defaults_group env world ~world0 ~owner
    (members : (prioritized_defaults * string list) list) =
  let l0 = fst (List.hd members) in
  let env = { env with subj = Some l0.pd_onderwerp } in
  let target_name =
    match (l0.pd_kenmerk, l0.pd_attribuut) with
    | Some k, _ -> k
    | _, Some a -> a
    | _ -> "?"
  in
  (* the ch. 7.2 conflict key names the type that DECLARES the target, not
     the instance's concrete type: a target is declared once, and keying on
     the concrete type would make one target report under as many names as
     it has subtypes *)
  let declarant =
    Option.value
      (Ir_v0.Checks.declaring_type env.doc ~from:owner
         ~kind:(if l0.pd_kenmerk <> None then `Characteristic else `Attribute)
         ~member:target_name)
      ~default:l0.pd_onderwerp
  in
  let target_form = declarant ^ "." ^ target_name in
  (match l0.pd_attribuut with
  | Some a when Ir_v0.Checks.attr_temporal (objecttype env owner) a ->
      Value.fault
        "temporal attribute %S as a defaults target: temporal writes are not implemented" a
  | _ -> ());
  (* the WF-086 characteristic arm's defense-in-depth: a temporal characteristic target would
     otherwise evaluate silently atemporally *)
  (match l0.pd_kenmerk with
  | Some k
    when List.exists
           (fun (n, _) -> Ir_v0.Checks.norm n = Ir_v0.Checks.norm k)
           (objecttype env owner).kenmerk_granulariteit ->
      Value.fault
        "temporal characteristic %S as a defaults target: temporal writes are not implemented" k
  | _ -> ());
  over_subjects ~exact:true env world ~otype:owner ~rule:target_form (fun w self cond_err ->
      let speculate f =
        let buf = ref [] in
        let r = f (fun m -> buf := m :: !buf) in
        (r, fun () -> List.iter cond_err (List.rev !buf))
      in
      let equal_dec a b =
        match (a, b) with
        | Char_dec x, Char_dec y -> x = y
        | Attr_dec x, Attr_dec y -> Arith.equal x y
        | _ -> false
      in
      (* the §9.5 conflict key (ch. 7.2/7.9): the bare target form for a
         root-level over-determination, the target form plus the default-index
         path for a nested exception group conflict *)
      let path_key = function
        | [] -> target_form
        | path -> target_form ^ "/" ^ String.concat "/" (List.map string_of_int path)
      in
      (* reconcile a group's member results (ch. 7.2, applied at every level
         by ch. 7.9): a conflict from a deeper exception group propagates unchanged;
         otherwise the deciding members must agree — equal decisions are one
         decision, differing decisions are a conflict keyed at [path] (no
         write). Members that decide nothing drop out. *)
      let reconcile ~path (results : chain_result list) : chain_result =
        match List.find_opt (function Conflict _ -> true | _ -> false) results with
        | Some c -> c
        | None -> (
            match List.filter_map (function Decided d -> Some d | _ -> None) results with
            | [] -> Undecided
            | d0 :: rest ->
                if List.for_all (equal_dec d0) rest then Decided d0
                else Conflict (path_key path))
      in
      (* one member's decision (ch. 7.1), recursive per ch. 7.9: the
         highest default that decides wins; a default carrying [excepties]
         reconciles its exception groups first, and its own justification/consequence
         is the fall-back only when they decide nothing. Speculative
         deposits flush when — and only when — a default decides, composing
         through the recursion. *)
      let member_decision ((l : prioritized_defaults), origins) : chain_result =
        if not (applies env l.pd_geldigheid) then Undecided
        else
          let is_char = l.pd_kenmerk <> None in
          (* a root default, and its exception groups with it, is evaluated on
             the position it was declared on: the composition moved an
             ancestor's default into this stack, and its own reads name the
             ancestor (WF-053) *)
          let env_at i = { env with subj = Some (List.nth origins i) } in
          let justified env (r : default) err =
            if r.default_initialisatie then (
              match l.pd_attribuut with
              | Some a ->
                  (* ch. 7.3: justified where the target is empty in the INPUT
                     facts. Read off the running world instead, the default's
                     own earlier write switched it off, and the target kept a
                     value its consequence had computed against a read that
                     was not final yet. An initialisation folded with its
                     condition is justified only where that condition holds
                     as well. *)
                  (match SMap.find_opt self.label world0.World.instances with
                  | Some (i0 : World.instance) -> World.attr i0 a = Value.Empty
                  | None -> true)
                  && (match r.default_voorwaarde with
                     | Some c -> eval_cond env w self ~err c
                     | None -> true)
              | None -> Value.fault "initialisation default on characteristic defaults")
            else eval_cond env w self ~err (req "default justification" r.default_voorwaarde)
          in
          (* the default's OWN decision, exception groups aside (a plain default, the
             initialisation default, or a form-a default's fall-back): Some when it
             decides, None when it falls through *)
          let own_decision env (r : default) : defaults_decision option =
            if is_char then (
              let j, flush = speculate (fun err -> justified env r err) in
              if j then (
                flush ();
                Some (Char_dec (req "default decision" r.default_besluit)))
              else None)
            else
              let outcome, flush =
                speculate (fun err ->
                    if justified env r err then
                      Some
                        (eval_expr env w self ~err
                           (req "default consequence" r.default_consequentie))
                    else None)
              in
              match outcome with
              | Some v when v <> Value.Empty ->
                  flush ();
                  Some (Attr_dec v)
              | Some _ (* leeg: falls through *) | None -> None
          in
          let rec decide ~env_at ~path (defaults : default list) : chain_result =
            let rec go i = function
              | [] -> Undecided
              | (r : default) :: rest -> (
                  let env = env_at i in
                  let fall_through () =
                    (* a form-a default falls back to its own decision; a
                       group-default (no own content) decides nothing (ch. 7.9) *)
                    let has_content =
                      r.default_besluit <> None || r.default_consequentie <> None
                    in
                    if has_content then
                      match own_decision env r with Some d -> Decided d | None -> go (i + 1) rest
                    else go (i + 1) rest
                  in
                  match r.default_excepties with
                  | Some egs -> (
                      (* the exception groups go first: reconcile them at this default's
                         path, then propagate its conflict, adopt its decision,
                         or fall back when it decides nothing *)
                      let sub_path = path @ [ i ] in
                      let results =
                        List.map
                          (fun (eg : exception_group) ->
                            decide ~env_at:(fun _ -> env) ~path:sub_path eg.eg_defaults)
                          egs.egs_leden
                      in
                      match reconcile ~path:sub_path results with
                      | (Conflict _ | Decided _) as d -> d
                      | Undecided -> fall_through ())
                  | None -> (
                      match own_decision env r with
                      | Some d -> Decided d
                      | None -> go (i + 1) rest))
            in
            go 0 defaults
          in
          decide ~env_at ~path:[] l.defaults
      in
      match reconcile ~path:[] (List.map member_decision members) with
      | Undecided -> (w, false)
      | Conflict rule ->
          (* over-determination (ch. 7.2/7.9): no write, one channel fact —
             bare-keyed at the root, path-keyed for a nested exception group *)
          (World.insert_error w ~rule ~subject:self.label, false)
      | Decided (Char_dec true) ->
          World.set_char w ~label:self.label ~char:(req "defaults characteristic" l0.pd_kenmerk)
      | Decided (Char_dec false) -> (w, false)
      | Decided (Attr_dec v) ->
          let a = req "defaults attribute" l0.pd_attribuut in
          World.write_attr w ~label:self.label ~attr:a
            ~decl:(attr_decl env ~otype:owner ~attr:a)
            v)

(* consistency and uniqueness fire at the computed fixpoint; violations
   are idempotent channel facts (ch. 5.6) *)
let run_consistentie env world (c : resolved_consistentie) =
  if not (applies env c.cons_geldigheid) then world
  else
    let name = Option.value c.cons_naam ~default:"<consistentie>" in
    let env = { env with subj = c.cons_subject } in
    let world, _ =
      over_subjects env world ~otype:(req "ConsistencyVersion.subject" c.cons_subject) ~rule:name
        (fun w self err ->
          if eval_cond env w self ~err (req "ConsistencyVersion.assertion" c.assertie) then (w, false)
          else (World.insert_error w ~rule:name ~subject:self.label, false))
    in
    world

(* ch. 5.9 decision (2026-08-20, extended to BOTH uniqueness forms):
   a temporal key reads EVER-uniqueness — an identity, not a
   measurement: every value the timeline EVER carries is claimed,
   colliding across instances even over non-overlapping periods.
   Same-instance redeclaration is no collision: the tuple set is
   deduplicated per instance (× source, in the union). At most one
   temporal component per key list — a second would need the
   same-moment tuple join, loud until a corpus case lands. A temporal
   key needs its full timeline; a bucket scalar would silently miss
   values. One shared function carries the single form and the union
   (compositionality: the same key, the same reading, either form). *)
let ever_tuples env ~otype ~keys (i : World.instance) : string list =
  let ot_decl = objecttype env otype in
  let temporal = List.filter (Ir_v0.Checks.attr_temporal ot_decl) keys in
  if List.length temporal > 1 then
    Value.fault
      "a uniqueness key list carries at most one temporal key component (the same-moment \
       tuple join has no corpus case; ch. 5.9)";
  let carried k =
    if not (Ir_v0.Checks.attr_temporal ot_decl k) then [ World.attr i k ]
    else
      match World.timeline i k with
      | Some tl -> List.map (fun (s : Timeline.segment) -> s.s_waarde) tl
      | None ->
          if World.attr i k <> Value.Empty then
            Value.fault
              "the ever-unique key %S needs the full timeline; %S supplies only a scalar \
               (ch. 5.9)"
              k i.label
          else []
  in
  (* the tuple set: scalars paired with every value the one temporal
     component ever carries; an empty component drops the tuple (the
     v0 empty-key rule) *)
  let rec tuples acc = function
    | [] -> [ List.rev acc ]
    | k :: rest ->
        List.concat_map
          (fun v -> if v = Value.Empty then [] else tuples (v :: acc) rest)
          (carried k)
  in
  (* an enumeration key component is a declared name, compared up to
     [norm] exactly as [Arith.equal] compares two labels: a case input
     spelled "Gaaf" and one spelled "gaaf" carry ONE key, as clingo's
     normalised storage already reads them (card
     drools-enumnaam-identiteit, maintainer decision 2026-09-20) *)
  let key_lexeme = function Value.Enum s -> norm s | v -> Value.to_string v in
  tuples [] keys
  |> List.map (fun vs -> String.concat "\x00" (List.map key_lexeme vs))
  |> List.sort_uniq String.compare

let run_uniciteit env world (u : resolved_uniciteit) =
  if not (applies env u.uni_geldigheid) then world
  else begin
    let name = Option.value u.uni_naam ~default:"<uniciteit>" in
    let subject = req "Source.objectType" u.uni_subject in
    (* the scope hop of a parent-scoped uniqueness (ch. 5.9, WF-054):
       keys are unique within the sub-population sharing one scope
       parent; an instance without a scope parent does not participate —
       the pinned runtime's silent narrowing, mirrored and recorded *)
    let scope_of =
      (* D12: the scope is the binding; a uniqueness without it is unscoped *)
      match u.scope_rol with
      | Some rb ->
          let role = bound_role env rb in
          Some (fun i -> role_links world i role)
      | None -> None
    in
    let keys = List.map (req "Source.keys entry") u.sleutels in
    (* a duplicate non-empty key (ch. 5.6): rows with an empty key
       component do not participate; temporal keys read ever-uniqueness
       through the shared tuple function, composed with the scope
       prefix (scope parent × ever-tuples) *)
    let keyed =
      List.concat_map
        (fun (i : World.instance) ->
          (* the key-shape guards run on EVERY instance of the type,
             before scope participation: a temporal key supplied as a
             bucket scalar is a form error that screams regardless of
             whether the row would have participated (data integrity
             precedes the scope narrowing — and the ASP side checks
             the same way, at translation) *)
          let tuples = ever_tuples env ~otype:subject ~keys i in
          let prefix =
            match scope_of with
            | None -> Some ""
            | Some links -> (
                match links i with
                | [] -> None (* scope-less: does not participate *)
                | [ (p : World.instance) ] -> Some (p.label ^ "\x00")
                | _ ->
                    Value.fault
                      "to-one scope role links more than one instance on %S (ch. 5.5)"
                      i.label)
          in
          match prefix with
          | None -> []
          | Some pre -> List.map (fun key -> (pre ^ key, i.label)) tuples)
        (World.of_type ~isa:(isa env) world subject)
    in
    let dup k = List.length (List.filter (fun (k', _) -> String.equal k k') keyed) > 1 in
    List.fold_left
      (fun w (k, label) -> if dup k then World.insert_error w ~rule:name ~subject:label else w)
      world keyed
  end

(* ch. 5.9, the uniqueness union: the key tuples of ALL sources form one
   shared uniqueness domain — a duplicate may arise across two different
   source types; tuples compare positionally; rows with an empty key
   component do not participate; every instance whose tuple occurs more
   than once in the union deposits one channel fact under the shared
   union name. *)
let run_uniciteit_unie env world (u : resolved_uniciteit_unie) =
  if not (applies env u.uu_geldigheid) then world
  else begin
    let name = Option.value u.uu_naam ~default:"<uniciteitsunie>" in
    if List.length u.bronnen < 2 then
      Value.fault
        "a uniqueness union carries at least two sources (the v0 resolver never produces fewer)";
    (* one row per (instance × source) — v0 SUMS the per-source counts,
       so an instance reached through two sources contributes its tuple
       twice and duplicates against itself, exactly like the runtime *)
    let keyed =
      List.concat_map
        (fun (b : uniciteit_unie_bron) ->
          let t = req "Source.objectType" b.uub_type in
          let keys = List.map (req "Source.keys entry") b.uub_sleutels in
          (* the shared ever-tuple function (ch. 5.9): one reading for
             the union and the single form alike *)
          List.concat_map
            (fun (i : World.instance) ->
              List.map (fun key -> (key, i.label)) (ever_tuples env ~otype:t ~keys i))
            (World.of_type ~isa:(isa env) world t))
        u.bronnen
    in
    let dup k = List.length (List.filter (fun (k', _) -> String.equal k k') keyed) > 1 in
    List.fold_left
      (fun w (k, label) -> if dup k then World.insert_error w ~rule:name ~subject:label else w)
      world keyed
  end

(* --- the session (ch. 5.7) --------------------------------------------- *)

(* ch. 8.4: the Parameterset session-start coverage check. Every granular
   parameter the document reads at the evaluation date (a ch. 8.3 vintage
   read: TemporalRead of a ParameterReference indexed by evaluationDate) must have
   a timeline in the session that covers that date — checked before any
   rule fires; a missing vintage is a configuration error, never a
   mid-evaluation leeg. Point reads at data-dependent indices are
   deliberately outside this check (ch. 8.4). Shared with the ASP
   backend, which runs it at translation time: data-plane validation,
   not evaluation semantics. *)
let vintage_read_params (doc : document) : string list =
  let acc = ref [] in
  let rec expr (e : expr) =
    match e with
    | Temporal_read
        { basis = Parameter_ref { parameter = Some p; _ };
          datum = Constante { naam = Some n; _ }; _ }
      when n = Ir_v0.Spelling.evaluation_date ->
        if not (List.exists (fun q -> norm q = norm p) !acc) then acc := p :: !acc
    | Temporal_read { basis; datum; _ } ->
        expr basis;
        expr datum
    | Literal _ | Attribuut_ref _ | Parameter_ref _ | Variabele_ref _ | Constante _ | Bound_day _
    | Expr_onopgelost _ | Binder_ref _ ->
        ()
    | Tekst_reeks { delen; _ } ->
        List.iter (function Ingevoegd { expr = Some e } -> expr e | _ -> ()) delen
    | Binair { links; rechts; _ } -> opt links; opt rechts
    | Functie { argumenten; leegwaarde; filter; _ } ->
        List.iter expr argumenten;
        opt leegwaarde;
        copt filter
    | Afgerond { basis; _ } -> opt basis
    | Begrensd { basis; minimum; maximum; _ } -> opt basis; opt minimum; opt maximum
    | Dimensie_cel { attribuut; _ } -> opt attribuut
    | Construct { argument; _ } -> opt argument
    | Match { scrutinee; arms; _ } ->
        expr scrutinee;
        List.iter (fun (_, _, body) -> expr body) arms
    | Struct_literal { velden; _ } -> List.iter (fun (_, e) -> expr e) velden
    | Field_access { base; _ } -> expr base
    | Let { value; body; _ } -> expr value; expr body
    | List_literal { elements; _ } -> List.iter expr elements
    | Lambda { body; _ } -> expr body
    | If_then_else { condition; then_; else_; _ } -> cond condition; expr then_; expr else_
    | Invoke { functie; arguments; _ } ->
        List.iter expr arguments;
        (* recurse into the invoked FunctionDef body: a vintage read reachable
           only through named closed logic must still be discovered (WF-067
           makes this acyclic, so the walk terminates) *)
        fn_body functie
    | Fold { list; functie; init; _ } -> expr list; fold_fn functie; expr init
    | Map_list { list; functie; _ } -> expr list; fold_fn functie
    | Filter_list { list; condition; _ } -> expr list; cond condition
    | List_length { list; _ } -> expr list
    | Instance_set_union { branches; _ } ->
        List.iter (fun (b : instance_set_branch) -> copt b.isb_filter) branches
  and fn_body name =
    match List.find_opt (fun (d : function_def) -> norm d.fd_naam = norm name) doc.function_defs with
    | Some d -> expr d.fd_body
    | None -> ()
  and fold_fn = function Fn_lambda { body; _ } -> expr body | Fn_name name -> fn_body name
  and opt = function Some e -> expr e | None -> ()
  and cond (c : conditie) =
    match c with
    | Vergelijking { links; rechts; _ } -> opt links; opt rechts
    | Predicaat { operand; _ } -> opt operand
    | Kenmerk_check _ | Role_membership _ | Regelversie _
    | Conditie_onopgelost _ ->
        ()
    | Periode_conditie { vanaf; tot; _ } -> opt vanaf; opt tot
    | Samengesteld { onderdelen; _ } -> List.iter cond onderdelen
    | Kwantorvouw { kv_filter; kv_toets; _ } -> copt kv_filter; copt kv_toets
  and copt = function Some c -> cond c | None -> ()
  in
  List.iter
    (fun (r : resolved_regel) ->
      opt r.doel;
      opt r.waarde;
      copt r.rr_voorwaarde)
    doc.resolved_regels;
  List.iter (fun (v : resolved_verdeling) -> copt v.vd_voorwaarde) doc.verdelingen;
  List.iter (fun (c : resolved_consistentie) -> copt c.assertie) doc.consistenties;
  List.iter (fun (d : resolved_dagsoort) -> copt d.rd_voorwaarde) doc.resolved_dagsoorten;
  List.iter (fun (fc : resolved_feit_creatie) -> copt fc.fc_voorwaarde) doc.feit_creaties;
  List.iter
    (fun (oc : resolved_object_creatie) ->
      List.iter (fun (t : toekenning) -> opt t.toe_waarde) oc.toekenningen;
      copt oc.oc_voorwaarde)
    doc.object_creaties;
  !acc

let parameterset_check env =
  List.iter
    (fun p ->
      match env.as_of with
      | None ->
          Value.fault
            "session start: the document vintage-reads parameter %S, but the session carries \
             no evaluation date (asOfDate is null) — ch. 8.4"
            p
      | Some d -> (
          let day =
            match Value.date_of_iso d with Value.Date z -> z | _ -> assert false
          in
          match SMap.find_opt (norm p) env.param_tls with
          | None ->
              Value.fault
                "session start: the document vintage-reads parameter %S, but the Parameterset \
                 carries no timeline for it — ch. 8.4"
                p
          | Some tl ->
              if Timeline.read tl day = Value.Empty then
                Value.fault
                  "session start: the Parameterset timeline of %S does not cover the \
                   evaluation date %s — ch. 8.4"
                  p d))
    (vintage_read_params env.doc)

(* the fixpoint of the forms that make nothing: [step] is one pass, and a
   pass [budget] + 1 that still changes something is the fault. [cause]
   names the divergence where it can, from the state the bound left *)
let fixpoint ~budget ~cause step world =
  let rec go n world =
    if n > budget then
      match cause world with
      | Some why -> Value.fault "no fixpoint within %d passes — divergence is a fault (ch. 5.7): %s" budget why
      | None -> Value.fault "no fixpoint within %d passes — divergence is a fault (ch. 5.7)" budget
    else
      let world, changed = step world in
      if changed then go (n + 1) world else world
  in
  go 0 world

(* The input-precision fence: a case value whose exact rational does not
   fit the attribute's/parameter's declared decimals is refused at the
   evaluation boundary — fail fast, with the same "does not fit N
   decimals" reason the ASP backend's integer scaling faults with, so
   the two executors refuse identically instead of ref silently keeping
   precision the declaration cannot hold. *)
let validate_input_precision env world =
  let fits ~decimals q =
    Z.equal (Q.den (Q.mul q (Q.of_bigint (Z.pow (Z.of_int 10) decimals)))) Z.one
  in
  let rec check ~what dt (v : Value.t) =
    match (dt, v) with
    (* the unbounded form has no ceiling and carries any exact value (ch. 2.4
       point 3); the ASP backend holds it as a whole number and refuses a
       non-whole input on it by name, so the two part here on purpose *)
    | Some (Numeriek { geheel = false; decimalen = 0; _ } | Percentage { geheel = false; decimalen = 0; _ }), Value.Num _ -> ()
    | Some (Numeriek { decimalen; _ }), Value.Num (q, _)
    | Some (Percentage { decimalen; _ }), Value.Num (q, _) ->
        if not (fits ~decimals:decimalen q) then
          Value.fault "input %s = %s does not fit %d decimals" what (Value.lexeme_of_q q) decimalen
    (* recurse into composite values: an over-precise field of a record or
       element of a list is refused at the boundary too, exactly as the ASP
       backend scales every field/element — otherwise ref keeps a precision
       inside a struct or list that the declaration cannot hold while clingo
       faults (the same 5b gap for list elements, now closed for both) *)
    | Some (Struct_type { velden; _ }), Value.Record fs ->
        List.iter
          (fun (fname, fv) ->
            match List.find_opt (fun (n, _) -> norm n = norm fname) velden with
            | Some (_, fdt) -> check ~what:(what ^ "." ^ fname) (Some fdt) fv
            | None -> ())
          fs
    | Some (List_type { element }), Value.Lijst xs ->
        List.iteri (fun i x -> check ~what:(Printf.sprintf "%s[%d]" what i) (Some element) x) xs
    | _ -> ()
  in
  SMap.iter
    (fun _ (i : World.instance) ->
      let decl attr = Option.join (Ir_v0.Checks.find_ci (objecttype env i.otype).attributen attr) in
      SMap.iter (fun a v -> check ~what:(i.label ^ "." ^ a) (decl a) v) i.attrs;
      SMap.iter
        (fun a (tl : Timeline.t) ->
          List.iter
            (fun (s : Timeline.segment) -> check ~what:(i.label ^ "." ^ a) (decl a) s.s_waarde)
            tl)
        i.tls)
    world.World.instances;
  let param_decl p =
    match Ir_v0.Checks.find_ci env.doc.parameters p with
    | Some (par : parameter) -> par.par_datatype
    | None -> None
  in
  SMap.iter (fun p v -> check ~what:("parameter " ^ p) (param_decl p) v) env.params;
  SMap.iter
    (fun p (tl : Timeline.t) ->
      List.iter
        (fun (s : Timeline.segment) -> check ~what:("parameter " ^ p) (param_decl p) s.s_waarde)
        tl)
    env.param_tls

let evaluate env world =
  (* WF-007: a presentation tag inside a type annotation. The loader lists
     where one stands (the in-memory datatype does not carry the tag), and
     this interpreter refuses the document in the verifier's sentence rather
     than evaluate a form the verifier rejects *)
  (match env.doc.presentatie_annotaties with
  | [] -> ()
  | at :: _ ->
      Value.fault
        "%s: the presentation tag \"percentage\" on a type annotation: the tag is written only \
         where a datatype is declared"
        at);
  (* fail fast on unverified input: the cycle-tolerant strata below are
     sound only on WF-045/046-clean documents (every cycle then passes
     through a recursive-group creation and carries no negative edge) —
     ref/ re-runs those two checks itself rather than silently
     answering on a graph the verifier would reject *)
  (match Ir_v0.Checks.wf045_046 { doc = env.doc } with
  | [] -> ()
  | { Ir_v0.Checks.wf; at; reason } :: _ ->
      Value.fault "%s at %s: %s — the dependency graph is ill-formed; ref/ refuses to evaluate"
        wf at reason);
  parameterset_check env;
  validate_input_precision env world;
  (* the input-world snapshot the 1:1 creation guard reads (ch. 5.5) *)
  let world0 = world in
  (* Strata (ch. 6.3), computed from the same dependency graph the
     verifier checks (Ir_v0.Checks.dep_forms — shared by construction,
     so ref/ and WF-045/046 cannot drift). Every form gets the smallest
     stratum that puts it at or above its positive producers and
     strictly above its negative ones — except along a dependency
     cycle: on verifier-clean input a cycle is WF-045-admissible (it
     passes through a recursive-group creation), and ch. 6.3
     discharges strictness there by the one-generation-per-instance
     progression, not a boundary — cycle edges count positive. Failure
     to converge is a cycle the verifier should have rejected.

     Invariant note (2026-09-23). "Cycle edges count positive" holds only
     for reads that the generation progression settles. A leeg/gevuld
     test is not one of them, and until the verifier counted it as a
     negative edge it reached this loop on an admissible cycle and was
     answered in pass order. On the R3 shape (a recursive creation under
     "het merk is niet gevuld", a rule writing merk = 1 when the
     successor's rang is not gevuld) this interpreter answered
     merk(W1) = 1, because its pass ran rules before creations; the
     opposite order mints W2 and leaves merk(W1) leeg, and the ASP reading
     has five stable models. On the R1 shape it minted a chain whose every
     creation is justified by a leeg merk that the final world fills.
     Since the WF-046 arm such documents are refused by the re-run above
     and never get here (ref/test WV-6.4-g). The same arm moved an
     off-cycle leeg reader strictly above its writer, so the reader's
     position in the document no longer decides whether it acts on a
     transient leeg (WV-6.4-f). A quantifier fold whose range can grow
     (`all`, `none`, `atMost`, `exactly`) or whose matrix is negated (`none`,
     `atMost`, `exactly`) is a negative edge on the same terms since
     that date. Other conditions that hold on absence (a comparison
     against leeg, a count against zero, a propositional `none` over a
     comparison, arithmetic that coalesces leeg to 0) are not negative
     edges. They still reach this loop on an admissible cycle, and off a
     cycle they leave this interpreter order-dependent in the condition of
     an attribute-writing form, as the leeg reader was before: with the
     reader first, "b = 1 indien de a van het ding ongelijk is aan 5" and
     "b = 1 indien het aantal dingen waarvan a > 0 gelijk is aan 0" give
     b = 1 here and leeg in the ASP backend. RUNTIME-SEMANTICS ch. 6.1
     records both as open (the older residual "conditions on
     attribute-writing forms read positively"). *)
  let dforms = Ir_v0.Checks.dep_forms { doc = env.doc } in
  let n = List.length dforms in
  let arr = Array.of_list dforms in
  let strata = Array.make n 0 in
  let writers node =
    List.concat
      (List.mapi
         (fun i (f : Ir_v0.Checks.dep_form) ->
           if List.mem node f.df_writes then [ i ] else [])
         dforms)
  in
  (* reachability over the form graph, for the cycle test: an edge
     p -> c lies on a cycle iff p is reachable from c *)
  let succ = Array.make n [] in
  Array.iteri
    (fun ci (f : Ir_v0.Checks.dep_form) ->
      List.iter
        (fun (node, _) ->
          List.iter
            (fun pi -> if not (List.mem ci succ.(pi)) then succ.(pi) <- ci :: succ.(pi))
            (writers node))
        f.df_reads)
    arr;
  let reachable = Array.init n (fun _ -> Array.make n false) in
  Array.iteri
    (fun src _ ->
      let rec go i =
        if not reachable.(src).(i) then begin
          reachable.(src).(i) <- true;
          List.iter go succ.(i)
        end
      in
      List.iter go succ.(src))
    arr;
  let on_cycle ~producer ~consumer = reachable.(consumer).(producer) in
  let changed = ref true in
  let rounds = ref 0 in
  while !changed do
    incr rounds;
    if !rounds > n + 2 then
      Value.fault "no evaluation strata exist — a dependency cycle reached ref/";
    changed := false;
    Array.iteri
      (fun ci (f : Ir_v0.Checks.dep_form) ->
        List.iter
          (fun (node, kind) ->
            List.iter
              (fun pi ->
                let need =
                  strata.(pi)
                  +
                  match kind with
                  | Ir_v0.Checks.Dep_pos -> 0
                  | _ -> if on_cycle ~producer:pi ~consumer:ci then 0 else 1
                in
                if strata.(ci) < need then begin
                  strata.(ci) <- need;
                  changed := true
                end)
              (writers node))
          f.df_reads)
      arr
  done;
  let stratum_by_at = Hashtbl.create 16 in
  Array.iteri (fun i (f : Ir_v0.Checks.dep_form) -> Hashtbl.replace stratum_by_at f.df_at i) arr;
  (* A form is found by the path the verifier's dep_forms record for it, and a
     form absent from the graph runs at stratum 0. A form recorded under a path
     this lookup does not ask for would therefore run at stratum 0 unnoticed,
     so every recorded path is held to one it asks for: a change of the
     verifier's spelling fails here instead of flattening the strata. A
     version of the six named forms is asked for by the wire pointer the
     loader set on its record, `/rules/<list>/<f>/versions/<v>` — never by
     its in-memory index, which leaves the wire's as soon as a form carries
     two versions; a defaults group by its index. The pointers must be
     distinct, or two forms would share one stratum. *)
  let group_at i = Printf.sprintf "/prioritizedDefaults/%d" i in
  let asked = Hashtbl.create 16 in
  List.iter
    (fun at ->
      if Hashtbl.mem asked at then
        failwith
          (Printf.sprintf
             "ref/lib/eval.ml, strata: two forms carry the wire pointer %S, so their strata \
              cannot be told apart"
             at);
      Hashtbl.replace asked at ())
    (List.map (fun (r : resolved_regel) -> r.rr_pointer) env.doc.resolved_regels
    @ List.map (fun (v : resolved_verdeling) -> v.vd_pointer) env.doc.verdelingen
    @ List.mapi (fun i _ -> group_at i) env.doc.prioritized_defaults
    @ List.map (fun (f : resolved_feit_creatie) -> f.fc_pointer) env.doc.feit_creaties
    @ List.map (fun (o : resolved_object_creatie) -> o.oc_pointer) env.doc.object_creaties
    @ List.map (fun (c : resolved_consistentie) -> c.cons_pointer) env.doc.consistenties
    @ List.map (fun (u : resolved_uniciteit) -> u.uni_pointer) env.doc.uniciteiten
    @ List.map (fun (u : resolved_uniciteit_unie) -> u.uu_pointer) env.doc.uniciteit_unies);
  Array.iter
    (fun (f : Ir_v0.Checks.dep_form) ->
      if not (Hashtbl.mem asked f.df_at) then
        failwith
          (Printf.sprintf
             "ref/lib/eval.ml, strata: the verifier's dep_forms record form %S at %S, a path the \
              stratum lookup never asks for"
             f.df_name f.df_at))
    arr;
  let stratum_of at = match Hashtbl.find_opt stratum_by_at at with Some i -> strata.(i) | None -> 0 in
  let kmax = Array.fold_left max 0 strata in
  let rule_stratum (r : resolved_regel) = stratum_of r.rr_pointer in
  let verdeling_stratum (v : resolved_verdeling) = stratum_of v.vd_pointer in
  let fc_stratum (f : resolved_feit_creatie) = stratum_of f.fc_pointer in
  let oc_stratum (o : resolved_object_creatie) = stratum_of o.oc_pointer in
  let cons_stratum (c : resolved_consistentie) = stratum_of c.cons_pointer in
  let uni_stratum (u : resolved_uniciteit) = stratum_of u.uni_pointer in
  let uu_stratum (u : resolved_uniciteit_unie) = stratum_of u.uu_pointer in
  let indexed l = List.mapi (fun i x -> (i, x)) l in
  (* defaults groups (ch. 7.2): members share one (subject, target); a
     group runs at the max of its members' strata *)
  (* One stack per (CONCRETE type, target): inheritance composed the
     inherited declarations into it (RUNTIME-SEMANTICS ch. 7.10), so what
     runs here is already the effective stack and its subject range is
     exactly that type. In a document without a supertype edge this is the
     declared grouping, unchanged. A stratum is the max over the forms the
     stack was composed from — its declared groups and its folded rules, each
     found by its wire pointer; a folded rule's reads are negative edges in
     the shared graph, so its stratum lies above what it reads. *)
  let defaults_groups =
    List.map
      (fun (owner, _key, members, pointers) ->
        let stratum = List.fold_left (fun acc p -> max acc (stratum_of p)) 0 pointers in
        (stratum, owner, members))
      (effective_defaults env)
  in
  (* the forms of stratum <= k that make nothing — rules, allocations,
     defaults groups, each in document order — with the name the divergence
     diagnosis gives a writer. The forms of a lower stratum run again here:
     they write on the instances this stratum's creations mint, and no
     population read orders them after those creations *)
  let named kind name = Printf.sprintf "%s \"%s\"" kind (Option.value name ~default:"<naamloos>") in
  let settling ~k =
    List.filter_map
      (fun r ->
        if rule_stratum r > k then None
        else Some (named "rule" r.rr_naam, fun w -> run_rule env w r))
      env.doc.resolved_regels
    @ List.filter_map
        (fun (v : resolved_verdeling) ->
          if verdeling_stratum v > k then None
          else Some (named "allocation" v.vd_naam, fun w -> run_verdeling env w v))
        env.doc.verdelingen
    @ List.filter_map
        (fun (stratum, owner, members) ->
          if stratum > k then None
          else
            Some
              ( named "defaults group" (fst (List.hd members)).pd_naam,
                fun w -> run_defaults_group env w ~world0 ~owner members ))
        defaults_groups
  in
  (* the creations of stratum <= k — fact creations, then object creations,
     each in document order. A step reads the world it gets first and
     writes on the second *)
  let creations ~k =
    List.filter_map
      (fun (fc : resolved_feit_creatie) ->
        if fc_stratum fc > k then None
        else Some (named "fact creation" fc.fc_naam, fun read w -> run_feit_creatie env ~read w fc))
      env.doc.feit_creaties
    @ List.filter_map
        (fun (i, (oc : resolved_object_creatie)) ->
          if oc_stratum oc > k then None
          else
            Some
              ( named "object creation" oc.oc_naam,
                fun read w -> run_object_creatie env ~world0 ~idx:i ~stratum:(oc_stratum oc) ~read w oc ))
        (indexed env.doc.object_creaties)
  in
  (* one pass: every form that makes nothing once, each on the world the
     one before it left *)
  let pass steps world =
    List.fold_left
      (fun (w, ch) (_, step) ->
        let w, c = step w in
        (w, ch || c))
      (world, false) steps
  in
  (* one generation (ch. 5.5): every creation against the same settled
     world [read], so none reads what another creation of the generation
     makes or links, whatever their order in the document. Returns the
     world with what the generation made, and the creations that minted or
     linked something *)
  let generation steps read =
    List.fold_left
      (fun (w, makers) (who, step) ->
        let w, made = step read w in
        (w, if made then makers @ [ who ] else makers))
      (read, []) steps
  in
  (* The cause of a divergence of the forms that make nothing, where the
     state the bound left shows one (ch. 5.7). Two writers of one target
     whose equations demand different values have no fixpoint (ch. 5.1):
     each pass, each overwrites the other. WF-079 refuses two unconditional
     writers; two conditional writers whose conditions both hold on one
     instance reach this point. Two more passes are replayed form by form,
     and an attribute of one instance that two or more writers change to
     different values in both passes is named with its writers and their
     values, in the order the pass runs them. The writes are read off the
     world (World.changed_attrs), so the replay is the pass itself. *)
  let disagreement steps world =
    let writes w =
      List.fold_left
        (fun (w, log) (who, step) ->
          let w', _ = step w in
          (w', log @ List.map (fun (l, a, v) -> (who, l, a, v)) (World.changed_attrs ~before:w ~after:w')))
        (w, []) steps
    in
    let conflicts log =
      let targets =
        List.fold_left (fun acc (_, l, a, _) -> if List.mem (l, a) acc then acc else acc @ [ (l, a) ]) [] log
      in
      List.filter_map
        (fun (l, a) ->
          (* each writer of (l, a) once, at its first write, with the last value it wrote *)
          let writers =
            List.fold_left
              (fun acc (who, l', a', v) ->
                if l' <> l || a' <> a then acc
                else if List.mem_assoc who acc then
                  List.map (fun (w', v') -> if w' = who then (w', v) else (w', v')) acc
                else acc @ [ (who, v) ])
              [] log
          in
          let differ =
            List.exists (fun (_, v) -> List.exists (fun (_, v') -> not (Arith.equal v v')) writers) writers
          in
          if List.length writers >= 2 && differ then Some (l, a, writers) else None)
        targets
    in
    let w1, first = writes world in
    let _, second = writes w1 in
    let first = conflicts first in
    match
      List.find_opt (fun (l, a, _) -> List.exists (fun (l', a', _) -> l = l' && a = a') first) (conflicts second)
    with
    | None -> None
    | Some (label, attr, writers) ->
        let otype = (World.get world label).otype in
        let declared =
          Option.value ~default:attr
            (List.find_map
               (fun (n, _) -> if norm n = attr then Some n else None)
               (objecttype env otype).attributen)
        in
        let count =
          match List.length writers with 2 -> "two" | 3 -> "three" | 4 -> "four" | n -> string_of_int n
        in
        let parts = List.map (fun (who, v) -> Printf.sprintf "%s writes %s" who (Value.to_string v)) writers in
        let listed =
          match List.rev parts with
          | last :: before -> String.concat ", " (List.rev before) ^ " and " ^ last
          | [] -> ""
        in
        Some
          (Printf.sprintf "%s writers of one target disagree: %s to the attribute %s of %s, a %s" count
             listed declared label otype)
  in
  (* The forms that make nothing come to rest stratum by stratum: for j = 0,
     1, …, k those of stratum <= j reach their fixpoint before a form of
     stratum j + 1 runs (ch. 6.3). A creation of stratum k makes instances on
     which the forms of every lower stratum still have to compute, and a form
     of a higher stratum may read what they write there only once it is at
     rest. Until 2026-10-09 the forms of every stratum up to k ran together
     in one pass in document order after a creation: on a Post a creation
     had made, "gemarkeerde post" (stratum 1, a Post is gemarkeerd when its a
     is ongelijk aan 5) written before "a van een post" (stratum 0, a = 5)
     read a as leeg, took that for unequal to 5 and marked the Post for good;
     written after it, the Post stayed unmarked. A lower form runs again in
     every higher stratum's fixpoint, so two writers of one target in
     different strata still overwrite each other there and are named
     (ch. 5.7).

     The forms that make nothing carry no budget of their own: without
     creations their graph is acyclic (WF-045), and the forms of the lower
     strata are at rest when stratum j's fixpoint starts and read nothing
     stratum j writes, so unless two writers of one target keep overwriting
     each other that fixpoint is reached within as many passes as stratum j
     has forms that make nothing. The bound, the larger of 100 and that
     number plus one, only turns those two writers into the fault that
     names them.

     The consistency and uniqueness forms of stratum j fire once the
     generations of stratum j are done (below), and in the loop of every
     higher stratum k again after the fixpoint of stratum j, in generation 0
     and after every generation, so they also check what a creation of
     stratum k makes. What they read there is at rest: stratum j or a lower
     one writes it, whose forms no longer change it on an instance that
     exists and have computed it on a new instance before the check fires.
     Their channel facts are idempotent (ch. 5.6) and instances only come,
     never go, so firing again adds a fact only for a new instance or for a
     key a new instance duplicates. In the loop of their own stratum they
     wait for its last generation. A consistency reads a positive
     characteristic non-strictly (ch. 6.1), so it can share a stratum with a
     cycle that still makes that characteristic grow, and a fact it
     deposited on an earlier generation's world would never be withdrawn: on
     W1 → W2 → W3, where a recursive fact creation makes an ervaren Wachter
     the maatgever of its opvolger and a Wachter with a maatgever is ervaren,
     "elke wachter ervaren" fired in generation 0 would leave a fact on W2
     and one on W3. A uniqueness reads its keys strictly and sits above every
     writer of a key. Until 2026-10-09 these forms fired once, at the
     fixpoint of their own stratum: a consistency of stratum 1 on Posts
     ("the a of a Post is kleiner dan 5") never saw the Post a creation of
     stratum 2 made with a = 5, and the channel stayed empty. *)
  let settling_upto = Array.init (kmax + 1) (fun j -> settling ~k:j) in
  let checks j world =
    let world =
      List.fold_left
        (fun w c -> if cons_stratum c = j then run_consistentie env w c else w)
        world env.doc.consistenties
    in
    let world =
      List.fold_left
        (fun w u -> if uni_stratum u = j then run_uniciteit env w u else w)
        world env.doc.uniciteiten
    in
    List.fold_left
      (fun w u -> if uu_stratum u = j then run_uniciteit_unie env w u else w)
      world env.doc.uniciteit_unies
  in
  let settle ~k world =
    let rec upto j world =
      if j > k then world
      else
        let steps = settling_upto.(j) in
        let own = List.length steps - if j = 0 then 0 else List.length settling_upto.(j - 1) in
        let world =
          fixpoint ~budget:(max 100 (own + 1)) ~cause:(disagreement steps) (pass steps) world
        in
        upto (j + 1) (if j < k then checks j world else world)
    in
    upto 0 world
  in
  (* for k = 0, 1, …: the forms of stratum <= k evaluate to their fixpoint
     before any form of stratum k+1 fires (ch. 6.3), in generations
     (ch. 5.5). Generation 0 is the fixpoint of the forms that make nothing;
     generation G+1 is every creation against generation G's settled world,
     after which the forms that make nothing settle again, stratum by
     stratum, the consistency and uniqueness forms of each lower stratum
     firing at its fixpoint ([settle]); the stratum is done when a
     generation makes and links nothing, and then its own consistency and
     uniqueness forms fire. Such a generation changes no instance,
     attribute, characteristic or link, so they check the world the last
     settle left.

     The number of generations needs no bound of its own. Every generation
     but the last mints an instance or lays a link; a (creation, subject)
     mints at most once, and the creation budget ([run_object_creatie])
     bounds the depth of a stratum's mints, so a stratum mints finitely many
     instances and the links between them are finite too. Until 2026-10-09
     a stratum admitted 100 generations that mint or link, and that count
     stood in for the depth: it also counted the generations a mint waits
     for a link or a value, and the hops of a closure that a fact creation
     lays. Before the generations the budget was 100 passes of every form of
     the stratum together, and where a chain stopped depended on the order
     of the rules: on a chain of 99 mints with "kopie = nummer" and "kopie2 =
     kopie" in one stratum, the reader of kopie written first faulted and
     written last played. *)
  let world = ref world in
  for k = 0 to kmax do
    let creations = creations ~k in
    let settle = settle ~k in
    (* [settled] is the settled world of the generation before *)
    let rec next settled =
      match generation creations settled with
      | w, [] -> w
      | w, _ -> next (settle w)
    in
    world := checks k (next (settle !world))
  done;
  !world
