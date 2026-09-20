(* The evaluation session of RUNTIME-SEMANTICS ch. 5: forward chaining to
   the least fixpoint, with freshness realised as naive re-evaluation
   passes and idempotence at the write boundary (World.write_attr). A
   bounded pass budget turns divergence into a fault.

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
}

let req what = function Some x -> x | None -> Value.fault "missing %s in IR node" what

(* The key the bound day of a day-kind definition rides on in [env.locals]
   (ch. 3.7, the `de dag` context value). It carries a NUL so that no
   declared binder name can ever collide with it: every other key in that
   map is a [norm]-ed source name, and [norm] neither produces nor preserves
   a NUL. Its presence is also the marker that evaluation is INSIDE a
   day-kind voorwaarde — which is the only place a nested day-kind predicate
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

let applies env (g : geldigheid option) =
  match g with
  | None | Some { geld_vanaf = None; geld_tot = None } -> true
  | Some { geld_vanaf; geld_tot } -> (
      match env.as_of with
      | None ->
          Value.fault "a validity-bounded form needs an evaluation date; this session is time-free"
      | Some d ->
          (match geld_vanaf with Some v -> String.compare v d <= 0 | None -> true)
          && (match geld_tot with Some t -> String.compare d t <= 0 | None -> true))

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
  | None -> Value.fault "unknown function %S (WF-068 should preclude this)" name

let kenmerk_temporal (ot : objecttype) name =
  List.exists (fun (k, _) -> norm k = norm name) ot.kenmerk_granulariteit

(* WF-100 / IR-SPEC ch. 4.6 (q1), maintainer decision 2026-09-10: on a
   characteristic check the `eigenaar` is AUTHORITATIVE for the declaration.
   WF-109 / ch. 4.6 (q2), maintainer decision 2026-09-19: it is also the
   GASTHEER — it names the object type the check LIVES ON, and that object is
   the one read. The v0 producer sets it on exactly one shape, the
   fact-creation guard that names a characteristic of the DOEL, so that the
   constraint lands on the doel "and never the anker, even when the anker
   declares a same-named kenmerk".

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
        "characteristic check %S is hosted on %S while the subject is %S: the owner names the \
         object the check is about (WF-100), and this reader has no other object to read"
        (Option.value kenmerk ~default:"?") e otype
  | _ -> ()

(* RUNTIME-SEMANTICS ch. 5.5 / ch. 5.8b, WF-109: is this fact creation's guard
   about the DOEL? The guard is the one condition position with two objects in
   scope, and the host field is what names which.

   The question is asked with the SAME two predicates the verifier uses, from
   the same shared module, so a document cannot be legal there and read
   differently here. A binding SATISFIES the host when `T ⊑ O` (every object it
   holds is one); it OVERLAPS the host when some object could be both. Exactly
   one satisfying binding and no second overlapping one is the single reading
   the wire settles — anything else designates two objects with one name.

   Three answers, and each is the chapter's:

   - DOEL — the satisfying binding is the doel. The guard filters the targets.
   - ANKER (false) — the satisfying binding is the anker, the host is absent,
     the check navigates or carries a whole-period qualifier, or the condition
     is anything else. Byte-identical to what this reader did before the rule.
     The whole-period fence is WF-109's third (ch. 4.6 (q2)) and is held here
     as well as in the verifier: under a qualifier both bean-model branches run
     the ch. 3.9 fold arm before they look at the host, so the form is off the
     wire and this reader must not answer it from a route the others lack.
   - FAULT — the host designates more than one of the two. ch. 5.8b requires a
     fault there and, until 2026-09-19, this helper returned false instead and
     let the anker answer: the one reading the decision rejects, delivered
     silently. WF-109 makes that document unreachable through the verifier, so
     this arm is fail-fast on a non-conforming input; it is a NAMED arm about
     this construct and not a fall-through, because the sentence has to say
     which two objects one name reached.

   ZERO satisfying bindings is deliberately NOT a fault here: it falls to the
   anker, where [owner_hosts] faults with its own named sentence one step
   later. One refusal per defect. *)
let guard_on_doel (doc : document) (fc : resolved_feit_creatie) =
  match (fc.anker, fc.fc_doel, fc.fc_voorwaarde) with
  | ( Some a,
      Some d,
      Some (Kenmerk_check { navigatie = None; periode = None; eigenaar = Some e; kenmerk; _ }) ) -> (
      let bound = [ ("anker", a); ("doel", d) ] in
      let hits = List.filter (fun (_, t) -> Ir_v0.Checks.subtype_of doc ~sub:t ~super:e) bound in
      let near = List.filter (fun (_, t) -> Ir_v0.Checks.populations_overlap doc t e) bound in
      let ambiguous () =
        Value.fault
          "characteristic check %S is hosted on %S, which reaches BOTH objects the fact creation \
           binds (anker %S, doel %S): the host names exactly one (WF-109), and this reader will \
           not pick for it"
          (Option.value kenmerk ~default:"?") e a d
      in
      match (hits, near) with
      | [ (r, _) ], [ _ ] -> r = "doel"
      | [ _ ], _ -> ambiguous ()
      | _ :: _ :: _, _ -> ambiguous ()
      | [], _ -> false)
  | _ -> false

(* ch. 3.9, the §8.2 whole-period qualifier over a CHARACTERISTIC, and the
   one place ch. 3.3's refusal must not reach.

   ch. 3.3 refuses "a time-dependent attribute or parameter in a scalar
   context" and names its own two exits in the same breath: "the only ways a
   timeline becomes a scalar are the dedicated collapses: the temporal total
   (ch. 3.6) and the whole-period predicate (ch. 3.9)". ch. 8.2 and ch. 3.11
   say it twice more ("the only bridges … are the dedicated collapses";
   "refused EXCEPT through the specified collapses"). A condition carrying a
   GeheelPeriode qualifier is therefore not a scalar context at all — it IS
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
      "whole-period qualifier on %S, which is not a time-dependent characteristic of %S (WF-043)"
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
          "owner on the gedurende clip member %S: the clip reads the subject's own \
           characteristic timeline (WF-097)" naam;
      if periode <> None then
        Value.fault
          "whole-period qualifier on the gedurende clip member %S: the clip already ranges \
           over the whole timeline (WF-097)" naam
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

(* the unique connecting role between two object types; deriving it —
   rather than naming the role in the IR — is the recorded v0 wart of
   ch. 4.1, mirrored here for both allocation recipients and instance
   folds. Uniqueness is counted in candidate ROLES, not fact types
   (v0's verbindendeRol, RegelSpraakModel: every role of the far type
   whose fact type also relates the near type counts), so a self-typed
   two-role fact type offers BOTH its roles and blocks the derivation —
   the pinned runtime derives null there; execution refuses to guess,
   the same block connecting_pair carries. *)
let connecting_role env ~from_type ~to_type =
  let candidates =
    List.concat_map
      (fun (_, (ft : feittype)) ->
        let f t r = fills env t r in
        match ft.rollen with
        | [ a; b ] ->
            (if f from_type a && f to_type b then [ b ] else [])
            @ (if f from_type b && f to_type a then [ a ] else [])
        | [ r ] when ft.wederkerig ->
            (* the reciprocal shape: the one role relates the type to
               itself, and is both sides of the hop *)
            if f from_type r && norm from_type = norm to_type then [ r ] else []
        | _ -> [])
      env.doc.feittypen
  in
  match candidates with
  | [ r ] -> r
  | [] -> Value.fault "no fact type relates %S to %S" from_type to_type
  | _ ->
      Value.fault
        "the connecting role from %S to %S is ambiguous (%d candidate roles); the pinned \
         runtime derives null there"
        from_type to_type (List.length candidates)

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
               verbindendeRol binding is the unambiguous route
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

(* linked instances of [to_type] seen from [self]; the connecting role
   comes from the explicit [binding] when the node carries one, and from
   the type-pair derivation otherwise. *)
let linked ?binding env world (self : World.instance) ~to_type =
  role_links world self
    (match binding with
    | Some rb -> bound_role env rb
    | None -> connecting_role env ~from_type:self.otype ~to_type)

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
   filler and elementObjecttype be the same name; since ch. 2.6 widened
   that to ⊑-compatibility the filler may be a STRICT SUPERTYPE, and the
   role then reaches instances the fold does not range over. Exactly the
   split ch. 4.1 makes for an allocation's recipients, and identity
   wherever the filler is [element] or below — which is every document
   written before the chain existed. *)
let fold_members ?binding ?path env world (self : World.instance) ~element =
  let reached =
    match path with
    | Some pad -> linked_path env world self ~path:pad (* ch. 3.4b *)
    | None -> linked ?binding env world self ~to_type:element
  in
  List.filter (fun (i : World.instance) -> isa env i.otype element) reached

(* a role reachable from [self]'s type whose name/plural matches [name] —
   the v0 instance-fold source wart: a pseudo-AttribuutRef carrying the
   sentinel type names a role, resolved against declared roles (the
   WF-034 exception). [to_type], when known (a navigation hop with a
   written endpoint), joins the filter — the v0 by-name derivation
   (verbindendeRolViaNaam) disambiguates same-named roles by target
   type; its best-effort pluralisation is deliberately NOT mirrored
   (a loud refusal beats heuristic name formation — tracked delta). The
   fold-source arm adds the narrowed type-pair fallback for a name that
   resolves NOWHERE (IR-SPEC ch. 4.3): the unique connecting fact type
   is the source; zero or multiple keep the loud fault. *)
(* the self-denoting fold source (WF-083, IR-SPEC ch. 4.3): the written
   form "het aantal <rol> van de <onderwerp>" may name the fold subject
   by its OWN role name; the wire carries that written role in the
   fold-source argument's navigatieRol. Presentation, not navigation —
   the v0 production runtime ignores the field there; mirrored safely:
   the name must denote a role (ci, name or declared plural) that
   [self_type] itself fills in some fact type, else the loud
   unsupported-shape fault stands. *)
let self_denoting_role (doc : document) ~self_type name =
  List.exists
    (fun (_, (ft : feittype)) ->
      List.exists
        (fun (r : rol) ->
          Ir_v0.Checks.subtype_of doc ~sub:self_type
            ~super:(Option.value r.rol_objecttype_naam ~default:"")
          && ((match r.rol_naam with Some n -> norm n = norm name | None -> false)
             || match r.rol_meervoud with Some m -> norm m = norm name | None -> false))
        ft.rollen)
    doc.feittypen

let reachable_roles_by_name ?to_type env ~self_type ~name =
  let matches (r : rol) =
    ((match r.rol_naam with Some n -> norm n = norm name | None -> false)
    || (match r.rol_meervoud with Some m -> norm m = norm name | None -> false))
    &&
    match to_type with
    | None -> true
    | Some t -> fills env t r
  in
  List.filter_map
    (fun (_, (ft : feittype)) ->
      let f r = fills env self_type r in
      match ft.rollen with
      | [ a; b ] ->
          if f a && matches b then Some b else if f b && matches a then Some a else None
      | [ r ] when ft.wederkerig -> if f r && matches r then Some r else None
      | _ -> None)
    env.doc.feittypen

let role_by_name ?to_type env ~self_type ~name =
  match reachable_roles_by_name ?to_type env ~self_type ~name with
  | [ r ] -> r
  | [] -> Value.fault "no role named %S reachable from %S" name self_type
  | _ -> Value.fault "ambiguous role name %S from %S" name self_type

(* the FAR-side role check's own resolution (RUNTIME-SEMANTICS ch. 5.8a, far
   half; IR-SPEC ch. 4.6 (t)): [backref] names the role the SUBJECT fills and
   [filler] the object type at the OTHER end of the same fact type. What the
   join ranges over is that other end, so that is what this returns — the role
   the FILLER fills, which is the sister of the one [backref] names.

   It cannot be [reachable_roles_by_name], and the difference is the whole
   point of the node: that function matches the name at the FAR end, while
   `backref` names the role on the SUBJECT's own side. Both ends are pinned
   at once, which is why an ambiguous pair is a fault and not a pick: two fact
   types relating the same two types through same-named subject roles denote
   two different relations, and v0 has no tie-break. Shared with the ASP
   backend so both executors resolve the node identically. *)
let far_side_role env ~self_type ~filler ~backref =
  let named (r : rol) =
    (match r.rol_naam with Some n -> norm n = norm backref | None -> false)
    || match r.rol_meervoud with Some m -> norm m = norm backref | None -> false
  in
  let candidates =
    List.concat_map
      (fun (_, (ft : feittype)) ->
        let f t r = fills env t r in
        match ft.rollen with
        | [ a; b ] ->
            (if f self_type a && named a && f filler b then [ b ] else [])
            @ (if f self_type b && named b && f filler a then [ a ] else [])
        | [ r ] when ft.wederkerig ->
            (* the reciprocal shape: one role is both ends of the hop, so it
               is its own back-reference *)
            if f self_type r && named r && f filler r then [ r ] else []
        | _ -> [])
      env.doc.feittypen
  in
  match candidates with
  | [ r ] -> r
  | [] ->
      Value.fault
        "no fact type holds %S in a role named %S with %S at the other end: the far-side role \
         check's filler and back-reference resolve against no single declaration"
        self_type backref filler
  | _ ->
      Value.fault
        "the far-side role check from %S over %S to %S is ambiguous (%d candidate relations)"
        self_type backref filler (List.length candidates)

(* the zero-hop self-reference (WF-053, IR-SPEC ch. 4.1a): a single
   bindingless written step on a reference whose endpoint type equals
   the subject type may denote the subject ITSELF, by a role the
   subject fills ("het strandtegoed van de aanmelder", read on the
   Jutter who is the aanmelder) — then the walk is empty and the own
   attribute is read, unconditionally (no link is consulted). Order is
   normative: far-role resolution FIRST (a reciprocal role stays a
   real hop); the self-reference only at zero far hits, and only under
   the WF-083 self-denotation test. v0's broader fall-throughs (ANY
   unresolved name on a same-typed reference, and the ambiguous far
   match) are deliberately NOT mirrored — both keep the loud fault
   (tracked delta in the safe direction). Returns None for the empty
   walk. *)
let nav_single_step env ~self_type ~endpoint ~name : rol option =
  match reachable_roles_by_name ~to_type:endpoint env ~self_type ~name with
  | [ r ] -> Some r
  | [] when norm endpoint = norm self_type && self_denoting_role env.doc ~self_type name ->
      None
  | [] ->
      if norm endpoint = norm self_type then
        Value.fault
          "no role named %S is reachable from %S, and it does not name a role %S itself \
           fills — not the zero-hop self-reference (WF-053)"
          name self_type self_type
      else Value.fault "no role named %S reachable from %S" name self_type
  | _ -> Value.fault "ambiguous role name %S from %S" name self_type

(* the navigated scalar read's hop walk (WF-053, RUNTIME-SEMANTICS
   ch. 5.8): resolve the hop roles — the explicit bindings when the node
   carries them, else the written names, else the type pair — then walk
   the links. An absent link at any hop short-circuits the whole read to
   None = leeg (ch. 1.2); a to-many hop in a scalar slot faults, never a
   silent first pick. A zero-hop self-reference (ch. 5.8 rule 4) walks
   nothing and reads the subject itself. *)
let nav_target env world (self : World.instance) ~bindings ~navigatie_rol ~navigatie_pad
    ~endpoint =
  let roles =
    match bindings with
    | Some rbs -> List.map (bound_role env) rbs
    | None -> (
        match navigatie_pad with
        | _ :: _ ->
            let last = List.length navigatie_pad - 1 in
            let step (i, cur, acc) name =
              let name = req "navigation path step" name in
              let to_type = if i = last then Some endpoint else None in
              let r = role_by_name ?to_type env ~self_type:cur ~name in
              (i + 1, Option.value r.rol_objecttype_naam ~default:"", acc @ [ r ])
            in
            let _, _, roles = List.fold_left step (0, self.otype, []) navigatie_pad in
            roles
        | [] -> (
            match navigatie_rol with
            | Some n -> (
                (* single written step: a far role when one resolves, the
                   zero-hop self-reference when none does and the name
                   denotes the subject itself (ch. 4.1a) *)
                match nav_single_step env ~self_type:self.otype ~endpoint ~name:n with
                | Some r -> [ r ]
                | None -> [])
            | None -> [ connecting_role env ~from_type:self.otype ~to_type:endpoint ]))
  in
  List.iter
    (fun (r : rol) ->
      if r.rol_meervoudig then
        Value.fault "navigation over the to-many role %S in a scalar slot is refused (WF-053)"
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

(* --- literals ----------------------------------------------------------- *)

let unit_of_datatype = function
  | Some (Numeriek { eenheid = Some e; _ }) | Some (Percentage { eenheid = Some e; _ }) ->
      Value.Unit e
  | _ -> Value.Unitless

let literal_value (waarde : string option) (typ : datatype option) =
  let lex = req "Literal.waarde" waarde in
  match typ with
  | Some (Numeriek _ | Percentage _) | None ->
      Value.Num (Value.q_of_lexeme lex, unit_of_datatype typ)
  | Some Booleaan -> (
      match norm lex with
      | "waar" -> Value.Bool true
      | "onwaar" -> Value.Bool false
      | _ -> Value.fault "malformed boolean literal %S" lex)
  | Some Tekst -> Value.Text lex
  | Some (Enumeratie _) -> Value.Enum lex
  | Some Datum_in_dagen -> Value.date_of_iso lex
  (* WF-095 (ch. 4.1): a datum-tijd literal reads into the SAME millisecond
     layer the case wire reads its `dateTime` into, through the same two
     admitted spellings — the canonical ISO lexeme and the day-first source
     snippet. An unreadable lexeme faults with the position, never silently
     becoming some other instant. *)
  | Some Datum_tijd -> Value.datetime_of_lexeme lex
  | Some _ -> Value.fault "literal of this datatype not implemented: %S" lex

(* --- expression evaluation --------------------------------------------- *)

(* [err] deposits a §9.5 error reason for the evaluating rule × subject *)
(* --- condition evaluation ---------------------------------------------- *)

(* the static type an expression node carries on the wire *)
let expr_typ : expr -> datatype option = function
  | Literal { typ; _ }
  | Tekst_reeks { typ; _ }
  | Binair { typ; _ }
  | Functie { typ; _ }
  | Afgerond { typ; _ }
  | Begrensd { typ; _ }
  | Attribuut_ref { typ; _ }
  | Dimensie_cel { typ; _ }
  | Parameter_ref { typ; _ }
  | Variabele_ref { typ; _ }
  | Constante { typ; _ }
  | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ }
  | Construct { typ; _ }
  | Match { typ; _ }
  | Binder_ref { typ; _ }
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
     wire [typ] and never reaches a slot where its static type is read *)
  | Lambda _ -> None

(* the ch. 1.2d empty-equality asymmetry routes on the declared operand
   kind — two empty runtime values cannot reveal it (Arith.compare_op) *)
let static_numeric l r =
  let kind e =
    match expr_typ e with
    | Some (Numeriek _ | Percentage _ | Datum_in_dagen | Datum_tijd) -> Some true
    | Some (Tekst | Booleaan | Enumeratie _ | Sum_type _ | List_type _ | Struct_type _) -> Some false
    | Some (Datatype_onbekend _) | None -> None
  in
  match kind l with Some b -> Some b | None -> kind r

(* the ch. 1.2d date row rides the numeric bucket above EXCEPT its
   both-leeg gelijk-aan cell, which is onwaar — v0's Tabel-38 guard on
   the date shape (Arith.compare_op ~static_date) *)
let static_date l r =
  let is_date e =
    match expr_typ e with Some (Datum_in_dagen | Datum_tijd) -> true | _ -> false
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
  | Literal { waarde; typ; _ } -> literal_value waarde typ
  | Parameter_ref { parameter; _ } -> (
      let name = req "ParameterRef.parameter" parameter in
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
  | Attribuut_ref
      { attribuut; objecttype = ot; navigatie_rol; navigatie_pad; navigatie_bindingen;
        set = false; typ; _ }
    when not (match typ with Some (Datatype_onbekend _) -> true | _ -> false) ->
      let name = req "AttribuutRef.attribuut" attribuut in
      let ot = req "AttribuutRef.objecttype" ot in
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
      let navigated =
        (* not navigated when the reference is anchored at the subject
           itself — which includes an INHERITED member, written at the
           declaring supertype and read on a subtype instance (ch. 2.2) *)
        navigatie_rol <> None || navigatie_pad <> [] || not (isa env self.otype ot)
      in
      if not navigated then begin
        (match navigatie_bindingen with
        | Some _ ->
            Value.fault "navigation bindings on a reference that does not navigate (WF-053)"
        | None -> ());
        World.attr self name
      end
      else begin
        (* the navigated read (ch. 5.8): walk the to-one hops; an absent
           link anywhere reads the whole operand as leeg *)
        match
          nav_target env world self ~bindings:navigatie_bindingen ~navigatie_rol
            ~navigatie_pad ~endpoint:ot
        with
        | None -> Value.Empty
        | Some target -> World.attr target name
      end
  | Attribuut_ref _ ->
      Value.fault "unsupported AttribuutRef shape (set/sentinel outside a fold)"
  | Binair { op = (Plus | Min) as op; links; rechts; _ }
    when List.exists
           (fun e ->
             match Option.bind e expr_typ with
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
      let le = req "Binair.links" links and re = req "Binair.rechts" rechts in
      let date_typed e =
        match expr_typ e with Some (Datum_in_dagen | Datum_tijd) -> true | _ -> false
      in
      let minus = op = Min in
      let dur_e =
        if date_typed le && date_typed re then
          Value.fault "date \xc2\xb1 date is ill-formed (WF-035)"
        else if date_typed le then re
        else
          Value.fault "duration %s date is ill-formed (WF-035: the date stands on the \
                       left; v0 does not commute)"
            (if minus then "\xe2\x88\x92" else "+")
      in
      let unit_ =
        match expr_typ dur_e with
        | Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ }) -> (
            match Ir_v0.Checks.time_unit_of_eenheid u with
            | Some tu ->
                if Ir_v0.Checks.time_unit_sub_day tu && expr_typ le = Some Datum_in_dagen
                then
                  Value.fault
                    "sub-day date arithmetic needs a datum-tijd operand (WF-035)"
                else `Calendar tu
            | None -> (
                (* not a calendar unit: the spelling may name a declared day
                   kind, which is the ch. 3.8 day-by-day step (§6.11) *)
                match day_kind_of_unit env u with
                | Some k -> `Day_kind k
                | None ->
                    Value.fault
                      "duration unit %S is neither a recognised time unit nor a declared day \
                       kind (WF-035)"
                      (Option.value u.afkorting ~default:"?")))
        | _ -> Value.fault "date \xc2\xb1 duration without a time unit (WF-035)"
      in
      let d = eval_expr env world self ~err le in
      let dur = eval_expr env world self ~err re in
      match unit_ with
      | `Calendar tu -> Arith.date_plus_duration ~minus ~unit_:tu d dur
      | `Day_kind k ->
          Arith.date_plus_day_kind ~minus ~matches:(day_kind_pred env world self ~err k) d dur)
  | Binair { op; links; rechts; _ } -> (
      let l = eval_expr env world self ~err (req "Binair.links" links) in
      let r = eval_expr env world self ~err (req "Binair.rechts" rechts) in
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
          Arith.sqrt_rounded ~err modus decimalen (eval_expr env world self ~err arg)
      | _ ->
          Arith.round_v modus decimalen (eval_expr env world self ~err (req "Afgerond.basis" basis)))
  | Begrensd { basis; minimum; maximum; _ } ->
      (* ch. 5.11, wire order: basis, then minimum, then maximum. The [let]s
         carry the semantics, not the style — written as two labelled
         arguments this build evaluates the MAXIMUM first, which runs the
         node's operands against the direction ch. 2.7 applies its bounds in.
         An absent bound evaluates nothing. *)
      let v = eval_expr env world self ~err (req "Begrensd.basis" basis) in
      let bound = function None -> Value.Empty | Some b -> eval_expr env world self ~err b in
      let min_ = bound minimum in
      let max_ = bound maximum in
      Arith.clamp ~min_ ~max_ v
  | Functie { kind = (Tijdsduur | Absolute_tijdsduur) as kind; argumenten; filter; typ; _ } -> (
      (* tijdsduur van A tot B (ch. 3.8), routed on the node-type unit:
         the counted unit is the node's own declared unit, resolved by
         spelling through the shared table (WF-038). *)
      if filter <> None then Value.fault "a filter on a tijdsduur has no v0 form";
      match argumenten with
      | [ a; b ] ->
          let unit_ =
            match typ with
            | Some (Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ })
              -> (
                match Ir_v0.Checks.time_unit_of_eenheid u with
                | Some tu ->
                    if
                      Ir_v0.Checks.time_unit_sub_day tu
                      && (expr_typ a = Some Datum_in_dagen || expr_typ b = Some Datum_in_dagen)
                    then
                      Value.fault
                        "sub-day tijdsduur needs datum-tijd operands (WF-038)"
                    else `Calendar tu
                | None -> (
                    (* not a calendar unit: the spelling may name a declared
                       day kind, which is the ch. 3.8 count over (A, B]
                       (§6.10) *)
                    match day_kind_of_unit env u with
                    | Some k -> `Day_kind k
                    | None ->
                        Value.fault
                          "tijdsduur unit %S is neither a recognised time unit nor a declared \
                           day kind (WF-038)"
                          (Option.value u.afkorting ~default:"?")))
            | _ ->
                Value.fault "tijdsduur without a recognised time unit on its node type \
                             (WF-038)"
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
      | _ -> Value.fault "tijdsduur expects two arguments")
  | Functie { kind; argumenten; element_objecttype; leegwaarde; filter; dimensie_selectie; granulariteit; bron_rol; bron_pad; typ = _; bron = _ } ->
      eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
        ~dimensie_selectie ~granulariteit ~bron_rol ~bron_pad
  | Tekst_reeks { delen; _ } ->
      Arith.concat
        (map_in_order
           (function
             | Letterlijk { tekst } -> Value.Text (Option.value tekst ~default:"")
             | Ingevoegd { expr = e } -> eval_expr env world self ~err (req "Ingevoegd.expr" e))
           delen)
  | Expr_onopgelost _ ->
      Value.fault "Onopgelost reached in evaluation: this rule fails observably (WF-044)"
  | Constante { naam = Some n; _ } when norm n = norm "Rekendatum" -> (
      (* ch. 8.2: the execution-date constant — the only expression-level
         access to the evaluation date *)
      match env.as_of with
      | Some d -> Value.date_of_iso d
      | None ->
          Value.fault
            "the Rekendatum constant needs an evaluation date; the case carries asOfDate null")
  | Constante { naam = Some n; _ } when norm n = norm "Rekenjaar" ->
      Value.fault "the Rekenjaar constant is not exercised by any corpus case (ch. 8.2)"
  | Constante { naam = Some n; _ } when norm n = norm "de dag" -> (
      (* ch. 3.7/3.8: the BOUND DAY of a day-kind definition — the day the
         defining predicate is being applied to. It is not a session value
         (that is Rekendatum, above) and it has a value only inside a
         day-kind evaluation, which is exactly where [day_kind_binding]
         puts one. *)
      match SMap.find_opt day_kind_binder env.locals with
      | Some (Now v) -> v
      | Some (Later f) -> f ()
      | None ->
          Value.fault
            "the 'de dag' context value is the bound day of a day-kind definition (ch. 3.7); \
             outside a day-kind evaluation it denotes nothing")
  | Dimensie_cel _ | Variabele_ref _ | Constante _ ->
      Value.fault "expression kind not implemented in this card"
  | Temporal_read { basis; datum; typ = _; bron = _ } -> (
      (* ch. 3.3, the point-in-time read: read(tl, d). The basis is never
         evaluated as a scalar (that is the refused shape); it names the
         timeline. An empty date index reads to leeg; a gap reads to leeg
         (ch. 3.1); a case that supplies only the asOfDate-bucket scalar
         cannot serve a read — fault, never a guess. *)
      match eval_expr env world self ~err datum with
      | Value.Empty -> Value.Empty
      | Value.Date d -> (
          match basis with
          | Attribuut_ref
              { attribuut = Some a; objecttype = Some otn; navigatie_rol = None;
                navigatie_pad = []; set = false; _ }
            when isa env self.otype otn ->
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
          | _ -> Value.fault "unsupported temporal-read basis shape")
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
                "match has no arm for enumeration value %S (unreachable on a verifier-clean \
                 document: WF-063/WF-088/WF-089 bind the arms to the declaration the scrutinee \
                 reads from, WF-023/WF-067 bind every value written into it)"
                label
          | Some (_, Some b, _) ->
              Value.fault "match arm for enumeration value %S carries a binder %S (WF-063)" label b
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
                "match has no arm for constructor %S (unreachable on a verifier-clean document: \
                 WF-063/WF-061/WF-089 bind the arms to the declaration the scrutinee reads from, \
                 WF-023/WF-067 bind every value written into it)"
                tag
          | Some (_, binder, body) ->
              let locals =
                match (binder, payload) with
                | None, None -> env.locals
                | Some b, Some v -> SMap.add (norm b) (Now v) env.locals
                | Some _, None ->
                    Value.fault "match arm for nullary constructor %S carries a binder (WF-063)" tag
                | None, Some _ ->
                    Value.fault "match arm for unary constructor %S lacks a binder (WF-063)" tag
              in
              eval_expr { env with locals } world self ~err body)
      | v -> Value.fault "match scrutinee is %s, not a constructor value" (Value.to_string v))
  | Binder_ref { naam; _ } -> (
      (* ch. 9.2: the nearest enclosing binder; WF-064 makes the miss
         statically impossible, so an unbound read is a fail-fast fault. A
         [Later] binding is a ch. 10.6 need: reading it forces the value (once)
         and raises the owning Let's barrier if that value is leeg. *)
      match SMap.find_opt (norm naam) env.locals with
      | Some (Now v) -> v
      | Some (Later read) -> read ()
      | None -> Value.fault "unbound binder %S (WF-064 should preclude this)" naam)
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
      (try eval_expr { env with locals } world self ~err body with Barrier -> Value.Empty)
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
                "field %S is not present on the record value (unreachable on a verifier-clean \
                 document: WF-090 binds every struct value to the struct domain of the declaration \
                 it comes from or enters, and WF-072 binds every struct type in the model and \
                 expression layers to that domain's declared field list)"
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
      Value.fault "a Lambda is position-restricted (WF-069): it is only a Fold/MapList operand"
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
        Value.fault "function %S invoked with %d arguments, expected %d (WF-068)" functie
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
          Value.Lijst
            (filter_in_order
               (fun x ->
                 let locals = SMap.add (norm binder) (Now x) env.locals in
                 eval_cond { env with locals } world self ~err condition)
               xs)
      | v -> Value.fault "filter-list operand is %s, not a list value" (Value.to_string v))
  | List_length { list; _ } -> (
      (* ch. 10.2: the element count; [] counts 0; a leeg source propagates *)
      match eval_expr env world self ~err list with
      | Value.Empty -> Value.Empty
      | Value.Lijst xs -> Value.Num (Q.of_int (List.length xs), Value.Unitless)
      | v -> Value.fault "list-length operand is %s, not a list value" (Value.to_string v))

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
    Value.fault "closed body applied to %d arguments, expected %d (WF-068/069/070)"
      (List.length args) (List.length parameters);
  let locals =
    List.fold_left2
      (fun acc (pname, _) v -> SMap.add (norm pname) (Now v) acc)
      SMap.empty parameters args
  in
  eval_expr { env with locals } world self ~err body

and eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
    ~dimensie_selectie ~granulariteit ~bron_rol ~bron_pad =
  (* the die/dat subselection executes on the instance folds below; the
     SAME wire field also carries the §7.1 gedurende timeline clip (a
     filter on the temporal total, where elementObjecttype is the
     subject itself) — that form must never run as an instance
     subselection and faults in its own arm *)
  if dimensie_selectie <> None then Value.fault "dimension selection not implemented in this card";
  if granulariteit <> None then Value.fault "granularity-bucketed aggregation not implemented in this card";
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
  (match bron_pad with
  | None -> ()
  | Some pad -> (
      if bron_rol <> None then
        Value.fault "a bronPad beside a bronRol: the one-step range IS bronRol (WF-102)";
      if List.length pad < 2 then
        Value.fault "a bronPad carries at least TWO steps, found %d (WF-102)"
          (List.length pad);
      match (element_objecttype, argumenten) with
      | ( Some _,
          [ Attribuut_ref { set = true; navigatie_rol = None; navigatie_pad = []; _ } ] ) ->
          ()
      | _ ->
          Value.fault
            "a bronPad is admissible only on the set source of a single-argument instance fold \
             (WF-102)"));
  match (kind, argumenten) with
  | ( Totaal,
      [ Attribuut_ref { attribuut = Some a; objecttype = Some aot; navigatie_rol = None; navigatie_pad = []; set = false; _ } ] )
    when isa env self.otype aot
         && Ir_v0.Checks.attr_temporal (objecttype env aot) a -> (
      (* ch. 3.6, the temporal total — the one v0-mirrored bridge from a
         timeline to a scalar. The case carries the asOfDate bucket's
         value (corpus/golden/README.md); the collapse folds that
         one-bucket timeline: value × whole-units of its own grid.
         [set = false] is load-bearing: a SET operand is the §7.1
         per-element fold, and on a self-relation (element type = subject
         type) the two shapes are otherwise indistinguishable here — this
         arm would then total the SUBJECT's own timeline and answer for
         the wrong instance. *)
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
            | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie; _ } ->
                if not (kenmerk_temporal ot_decl k) then
                  Value.fault
                    "gedurende over %S, which is not a temporal characteristic (WF-055)" k;
                let periods =
                  Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
                in
                if negatie then Timeline.complement periods ~span else periods
            | Vergelijking
                { op; links = Some (Attribuut_ref
                    { attribuut = Some b; objecttype = Some botn; navigatie_rol = None;
                      navigatie_pad = []; set = false; _ });
                  rechts = Some grens; periode = None; _ }
              when isa env self.otype botn
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
                (* ch. 3.6, the FLAT composite ALLE clip: the
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
                         check (WF-055; the static-guard and nested forms are recorded \
                         flanks)"
                in
                List.fold_left
                  (fun acc part -> Timeline.intersect_periods acc (member_periods part))
                  (member_periods (List.hd onderdelen))
                  (List.tl onderdelen)
            | _ ->
                Value.fault
                  "unsupported gedurende condition shape (WF-055: an own temporal \
                   characteristic, a per-segment comparison of an own temporal attribute, \
                   or an ALLE composite of such shapes)"
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
              let van, tot = Timeline.bucket gran day in
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
      match filter with None -> true | Some c -> eval_cond env world i ~err c
    in
    (* fold-source shapes are navigation-free — the same narrowing the
       verifier's WF-047/048 detection uses; a navigated source falls to
       the fail-fast arm below *)
    match (element_objecttype, argumenten) with
    | ( Some el,
        [ Attribuut_ref
            { attribuut; typ = Some (Datatype_onbekend _);
              navigatie_rol; navigatie_pad = []; _ } ] )
      when (match navigatie_rol with
           | None -> true
           | Some nr ->
               (* the self-denoting source (WF-083): the written form
                  names the fold subject by its own role name — no
                  navigation, the fold stays subject-anchored; an
                  away-pointing role falls to the loud fault below *)
               self_denoting_role env.doc ~self_type:self.otype nr) ->
        (* the row-1 wart: the fold source argument names a role; with an
           explicit binding the name check is the verifier's (WF-047) *)
        (match bron_rol with
        | Some _ -> ()
        | None -> (
            let name = req "fold source" attribuut in
            match reachable_roles_by_name env ~self_type:self.otype ~name with
            | [ _ ] -> ()
            | _ :: _ -> Value.fault "ambiguous role name %S from %S" name self.otype
            | [] ->
                (* the narrowed type-pair fallback (IR-SPEC ch. 4.3,
                   RUNTIME-SEMANTICS ch. 3.4a): a written name that
                   resolves to no declared role — v0's synthesized plural
                   over a role without a declared one — is admitted iff
                   exactly one fact type connects the subject to the
                   element type; [connecting_role] faults loudly on zero
                   or multiple, and the +s/+en spelling guess itself
                   stays unmirrored (tracked delta) *)
                let (_ : rol) = connecting_role env ~from_type:self.otype ~to_type:el in
                ()));
        `Instances (filter_in_order keep (fold_members ?binding:bron_rol env world self ~element:el))
    | ( Some el,
        [ Attribuut_ref { attribuut; set = true; navigatie_rol = None; navigatie_pad = []; _ } ]
      ) ->
        let name = req "fold attribute" attribuut in
        let el_decl = objecttype env el in
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
                 2026-09-20: the red document with kind MINIMUM matched the filled
                 count's pin word for word. Hence a sentence of its own, and it says
                 what is missing for THIS fold rather than what som and totaal have:
                 a count asks after PRESENCE, which is a question about an instant,
                 and a timeline answers it differently at every one.
                 The construct is named AFTER the interpolation on purpose — the
                 interpolation-free tail is all a pin can quote. *)
              Value.fault
                "the temporal attribute %S of a set has no moment at which a filled count can ask \
                 a timeline whether it is filled (ch. 3.4c); presence is a question about an \
                 instant, and only som and totaal define the per-element collapse that would \
                 supply one"
                name
          | _ ->
              Value.fault
                "this aggregation over the temporal attribute %S of a set has no defined moment \
                 to fold (ch. 3.6: only som and totaal carry the per-element timeline fold)"
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
           v0's listed family MIN/MAX/EERSTE/LAATSTE (isGelisteFold: the
           scalar concatenatieFold path, where the resolver-stamped
           elementObjecttype is vestigial). For SOM/TOTAAL/AANTAL the
           element is load-bearing: v0's accumulate consumes it (joins
           the element, reads only argument 0), so a multi-operand form
           keeps the loud fault below — never a silent list fold v0
           would compute differently. *)
        if filter <> None then
          Value.fault "a filter on a listed-operand fold has no v0 form (the subselection \
                       ranges over instances)";
        `Values (map_in_order (eval_expr env world self ~err) args)
    | None, args ->
        if filter <> None then
          Value.fault "a filter on a listed-operand fold has no v0 form (the subselection \
                       ranges over instances)";
        `Values (map_in_order (eval_expr env world self ~err) args)
    | Some _, _ -> Value.fault "unsupported instance-fold source shape"
  in
  let values () =
    match contributors () with
    | `Values vs -> vs
    | `Instances _ -> Value.fault "value aggregation over a bare role source"
  in
  match kind with
  | Aantal -> (
      (* a count, never empty (ch. 1.2e) *)
      match contributors () with
      | `Instances is -> Arith.agg_aantal is
      | `Values vs -> Arith.agg_aantal (Arith.filled vs))
  | Aantal_gevulde ->
      (* ch. 3.4c: the filled-value count. It reads a VALUE per contributor
         and counts the ones that are not leeg, so [values ()] is the whole
         of it — a set source hands them over and every other source shape
         reaches that function's loud refusal, which is where the bare role
         source lands ("value aggregation over a bare role source"). WF-037
         puts the same fence on the wire; the refusal here stays as the
         second line of defence.
         Never empty: [agg_aantal] over the empty list is 0, the identity
         element of a rule over sets (ch. 1.3), and no `leegwaarde` is
         consulted because WF-037 admits none. *)
      Arith.agg_aantal (Arith.filled (values ()))
  | Som ->
      (* ch. 5.11, wire order: `argumenten` stands before `leegwaarde` in the
         node, so the contributors evaluate first and the stand-in after them.
         WF-037 confines a leegwaarde to the som, so this is the only arm that
         ever evaluates one. *)
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
      | [ a ] -> Arith.sqrt_ ~err (eval_expr env world self ~err a)
      | _ -> Value.fault "wortel expects one argument")
  | Absolute_waarde -> (
      match argumenten with
      | [ a ] -> Arith.abs_ (eval_expr env world self ~err a)
      | _ -> Value.fault "absolute waarde expects one argument")
  | Concatenatie -> Arith.concat (map_in_order (eval_expr env world self ~err) argumenten)
  | Jaar_uit -> (
      match argumenten with
      | [ a ] -> Arith.jaar_uit (eval_expr env world self ~err a)
      | _ -> Value.fault "jaar uit expects one argument")
  | Maand_uit -> (
      match argumenten with
      | [ a ] -> Arith.maand_uit (eval_expr env world self ~err a)
      | _ -> Value.fault "maand uit expects one argument")
  | Dag_uit -> (
      match argumenten with
      | [ a ] -> Arith.dag_uit (eval_expr env world self ~err a)
      | _ -> Value.fault "dag uit expects one argument")
  | Eerste_paasdag -> (
      match argumenten with
      | [ a ] -> Arith.eerste_paasdag (eval_expr env world self ~err a)
      | _ -> Value.fault "eerste paasdag expects one argument")
  | Tijdsduur | Absolute_tijdsduur ->
      (* unreachable: eval_expr routes both variants on the node-type unit *)
      assert false
  | _ -> Value.fault "function kind not implemented in this card"

and eval_cond env world (self : World.instance) ~err (c : conditie) : bool =
  match c with
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      let le = req "Vergelijking.links" links and re = req "Vergelijking.rechts" rechts in
      let l = eval_expr env world self ~err le in
      let r = eval_expr env world self ~err re in
      Arith.compare_op ?static_numeric:(static_numeric le re) ~static_date:(static_date le re) op l
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
      let le = req "Vergelijking.links" links and re = req "Vergelijking.rechts" rechts in
      let temporal_side = function
        | Attribuut_ref
            { attribuut = Some a; objecttype = Some otn; navigatie_rol = None;
              navigatie_pad = []; set = false; _ }
          when isa env self.otype otn && Ir_v0.Checks.attr_temporal (objecttype env otn) a ->
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
               attribute of %S and a scalar bound (WF-043)"
              self.otype
      in
      let tested = Option.value (World.timeline self attr) ~default:[] in
      let grens = eval_expr env world self ~err grens_e in
      let g : Ir_v0.Ast.granulariteit = if gp.gp_jaar then Jaar else Maand in
      let verdict =
        Timeline.geheel_periode_waarde g ~tl:tested (fun v ->
            Arith.compare_op ?static_numeric:(static_numeric le re)
              ~static_date:(static_date le re) op v grens)
      in
      (* ch. 3.9: "Negation complements the folded result" *)
      if gp.gp_niet then not verdict else verdict
  | Predicaat { soort = Leeg | Gevuld; periode = Some _; _ } ->
      (* ch. 3.9's leeg/gevuld tests over a timeline. Both arms below matched
         `periode` away with `_` until 2026-09-11, so the qualifier was
         DROPPED and the operand answered its scalar reading — the silent
         shape this refusal replaces. The characteristic-holds test is built
         (above); these two are not, and a gap that says so is not a gap that
         answers the opposite. *)
      Value.fault "whole-period leeg/gevuld test is ch. 3.9 scope, not executed here"
  | Predicaat { soort = Leeg; operand; negatie; _ } ->
      let v = eval_expr env world self ~err (req "Predicaat.operand" operand) in
      let is_empty = v = Value.Empty in
      if negatie then not is_empty else is_empty
  | Predicaat { soort = Gevuld; operand; negatie; _ } ->
      let v = eval_expr env world self ~err (req "Predicaat.operand" operand) in
      let is_filled = v <> Value.Empty in
      if negatie then not is_filled else is_filled
  | Predicaat { soort = Dagsoort; dagsoort; operand; negatie; _ }
    when SMap.mem day_kind_binder env.locals ->
      (* A NESTED day-kind reference, inside a day-kind voorwaarde (ch. 3.7,
         the WF-085 reference graph): the §9.8 composition, "a day is a
         holiday when it is a Christmas day or …". It is carried on the
         BOUND DAY ONLY — a test on some other date is a recognised,
         unexecuted form, refused in the same words by the ASP backend (the
         narrow route, 2026-09-19: ref and the Drools day-kind compiler both
         answered it while the ASP backend refused it, and a form one
         executor computes and another refuses is the asymmetry this card
         exists to end). Outside a day-kind voorwaarde the predicate stays
         refused altogether (the arm below): ch. 5.4's framing is that a day
         kind is consulted as a stratum-0 classifier, never as a derived read
         in the rule fixpoint. The leeg rule is the source's own exception: a
         leeg value is never of a day kind, so the test is false rather than
         leeg-propagating. *)
      (match operand with
      | None -> ()
      | Some (Constante { naam = Some n; _ }) when norm n = norm "de dag" -> ()
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
        | Value.Date z -> day_kind_matches env world self ~err (req "Predicaat.dagsoort" dagsoort) z
        | Value.Date_time t ->
            day_kind_matches env world self ~err
              (req "Predicaat.dagsoort" dagsoort)
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
  | Kenmerk_check { kenmerk; navigatie = None; negatie; navigatie_binding; eigenaar; periode; _ }
    ->
      (match navigatie_binding with
      | Some _ ->
          Value.fault
            "navigation binding on a characteristic check that does not navigate (WF-053)"
      | None -> ());
      owner_hosts env.doc ~otype:self.otype ~kenmerk eigenaar;
      let k = req "KenmerkCheck.kenmerk" kenmerk in
      (* [owner_hosts] above has already established that an `eigenaar`, where
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
  | Kenmerk_check
      { kenmerk; navigatie = Some nav; negatie; navigatie_binding; eigenaar; periode; _ } -> (
      (* the navigated check (ch. 5.8): one to-one hop. The v0 asymmetry,
         mirrored deliberately: an ABSENT link makes the check false for
         the positive AND the negated form — unlike comparison and
         leeg/gevuld, which treat the absent relation as a leegwaarde.
         Under a ch. 3.9 qualifier that asymmetry does NOT hold: the
         chapter lifts the ch. 1.2 leeg-navigation discipline there and
         gives the failed hop a null timeline, which passes vacuously. *)
      (* the owner is a HOSTING statement about the pattern, not about the
         reached type, so it is crossed against the subject here exactly as on
         the scalar arm — the characteristic itself still lives on the far side *)
      owner_hosts env.doc ~otype:self.otype ~kenmerk eigenaar;
      let k = req "KenmerkCheck.kenmerk" kenmerk in
      let role =
        match navigatie_binding with
        | Some rb -> bound_role env rb
        | None ->
            (* deferred, loudly: the ch. 4.1a zero-hop self-reference is
               scoped to the attribute read — v0's resolver never
               produces a navigated check whose written role denotes the
               subject itself, so a self-denoting name here faults
               explicitly rather than silently reading the own
               characteristic *)
            if
              reachable_roles_by_name env ~self_type:self.otype ~name:nav = []
              && self_denoting_role env.doc ~self_type:self.otype nav
            then
              Value.fault
                "the navigated characteristic check's role %S names a role %S itself fills \
                 — the zero-hop self-reference does not extend to the characteristic check \
                 (deferred; WF-053, IR-SPEC ch. 4.1a)"
                nav self.otype
            else role_by_name env ~self_type:self.otype ~name:nav
      in
      if role.rol_meervoudig then
        Value.fault
          "the navigated check reaches the to-many role %S in a scalar condition (WF-053)"
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
  | Rollidmaatschap { ander_te_filler; negatie; _ } ->
      (* ch. 5.8a near-side rolcheck ("hij is een <rol>"): the subject fills
         the role iff its to-ONE reference to the OTHER end of the fact type
         is set — a null-test on that reference (v0 DrlTranspiler §8.1.7,
         mirrored). The written role name is presentation; the fact-type
         route is the connecting role to [ander_te_filler] (v0's
         verbindendeRol). A to-MANY connecting reference is v0's marker (the
         far-side needs an exists-join, not a scalar null-test), refused
         loudly here — never silently decided. The absent reference is leeg:
         false for the positive form, true for the "is geen" negation. *)
      let other = req "Rollidmaatschap.anderTeFiller" ander_te_filler in
      let role = connecting_role env ~from_type:self.otype ~to_type:other in
      if role.rol_meervoudig then
        Value.fault
          "the rolcheck's connecting reference to %S is the to-many role %S (ch. 5.5): the \
           near-side null-test needs a to-one reference — v0 markers it"
          other
          (Option.value role.rol_naam ~default:"?");
      let fills = role_links world self role <> [] in
      if negatie then not fills else fills
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
      let el = req "Kwantorvouw.elementObjecttype" kv_element_objecttype in
      let toets =
        match kv_toets with
        | Some t -> t
        | None ->
            Value.fault
              "quantifier fold without a toets: the matrix is what every contributor must \
               satisfy (WF-108)"
      in
      (match kv_bron_pad with
      | None -> ()
      | Some pad ->
          if kv_bron_rol <> None then
            Value.fault
              "a bronPad beside a bronRol on a quantifier fold: the one-step range IS bronRol \
               (WF-108)";
          if List.length pad < 2 then
            Value.fault
              "a quantifier fold's bronPad carries at least TWO steps, found %d (WF-108)"
              (List.length pad));
      (match kv_kwantor with
      | Alle | Geen | Ten_minste | Ten_hoogste | Precies -> ()
      | De ->
          Value.fault
            "quantifier fold with kwantor DE: the bare definite describes ONE member, not a \
             quantification over the set (WF-108)");
      let drempel =
        match (kv_kwantor, kv_drempel) with
        | (Ten_minste | Ten_hoogste | Precies), Some d -> d
        | (Ten_minste | Ten_hoogste | Precies), None ->
            Value.fault
              "counting quantifier fold without a drempel: there is no threshold to compare the \
               count of satisfying contributors with (WF-108)"
        | _, _ -> 0
      in
      let keep (i : World.instance) =
        match kv_filter with None -> true | Some c -> eval_cond env world i ~err c
      in
      let members =
        filter_in_order keep
          (fold_members ?binding:kv_bron_rol ?path:kv_bron_pad env world self ~element:el)
      in
      (* ch. 3.9a's count, and it is ch. 5.10's machinery over the
         CONTRIBUTORS instead of over a written member list — ALLE is
         [hits = n] and GEEN is [hits = 0], so all five quantifiers are the
         same comparison and the same stop triggers. GEEN stays the
         per-MEMBER complement and never the fold's: "geen van de bergers
         heeft gedurende het gehele jaar meer dan 40" asks each member to
         fail the matrix, so a member whose timeline does not cover the
         period fails it and does not block GEEN either. The empty
         contributor set needs no arm of its own: ALLE and GEEN both read
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
                let hit = eval_cond env world m ~err toets in
                read (if hit then hits + 1 else hits) (left - 1) rest)
      in
      read 0 n members
  | Verre_rollidmaatschap { filler; backref; negatie; _ } ->
      (* ch. 5.8a FAR-side rolcheck ("de Wachtpost heeft een wachtjutter"):
         the same §8.1.7 sentence read from the other end. The subject does
         NOT fill the named role; it is the relation's other end, and the
         check asks whether ANY object of [filler] stands in that relation to
         it — an existence question over the relation, never a count and
         never a first pick.

         It reads the RELATION, not a direction, and that is ch. 5.7a's law:
         a supplied link establishes the relation itself and both roles read
         it, the loader materialising the leg the case did not spell. So the
         join is the subject's own links over the role the filler fills, the
         same reading every other navigation here uses — a second,
         instance-scanning route would be a second reading of one relation
         and could come apart from it. The bean-model executors ask the
         mirror-image question (does some filler's stored reference point
         back at this subject), which is the same relation seen from the side
         their fact model happens to store.

         The FILLER is read TWICE and both readings are load-bearing. It
         resolves the pair, and it then FILTERS what the relation reached:
         an object counts only where its own type is ⊑ the named filler
         (ch. 2.2, extensionally — the same one-sided test [fills] applies
         to a role). Dropping the second reading makes the node answer the
         ROLE's whole population instead of the named one, which is a
         different sentence wherever the filler is a strict SUBTYPE of the
         role's declared type — measured 2026-09-20 on
         `jutterswet-postbezetting` cases 1 and 3, where the role-wide
         reading reports a post manned by a Strandjutter it does not have,
         verifier-clean and against both Drools branches.

         "heeft geen" is the plain complement — unlike the navigated
         characteristic check of ch. 5.8, nothing sits behind a hop here, so
         a relation with no filler OF THAT CLASS makes the negated form
         true. It rides the same filtered list, so the two forms can never
         disagree about which objects count. *)
      let f = req "VerreRollidmaatschap.filler" filler in
      let b = req "VerreRollidmaatschap.backref" backref in
      let role = far_side_role env ~self_type:self.otype ~filler:f ~backref:b in
      let has_one =
        List.exists (fun (i : World.instance) -> isa env i.otype f) (role_links world self role)
      in
      if negatie then not has_one else has_one
  | Periode_conditie { vanaf; tot; tot_inclusief; _ } ->
      (* ch. 3.10, the SCALAR target's evaluation-date gate — the chapter's
         first case, in its own six clauses: vanaf inclusive, tot exclusive,
         "tot en met" inclusive, an absent bound open, an UNSET evaluation
         date never matching, a LEEG bound never matching. The chapter's
         other case, the temporal target's clip, never reaches this arm:
         [run_rule] refuses it by name before a subject is taken, because the
         case distinction is on the rule's TARGET and a condition sees none.

         The mirror note bounds what a bound may BE (backend maturity, not IR
         scope): a literal or the subject's own scalar date attribute. WF-042
         admits any date expression, so the rest is refused by name here and
         in the ASP backend alike — one carried set for both, or the
         differential would arbitrate a form only one of them reads.

         Both bounds are evaluated, vanaf before tot (ch. 5.11), and BEFORE
         the verdict looks at the evaluation date: the order is normative and
         a fault in either bound belongs to it, not to whether today happens
         to fall inside. *)
      let bound side e =
        (match e with
         | Literal _ -> ()
         | Attribuut_ref
             { attribuut = Some a; objecttype = Some otn; navigatie_rol = None; navigatie_pad = [];
               set = false; _ }
           when isa env self.otype otn && not (Ir_v0.Checks.attr_temporal (objecttype env otn) a) ->
             ()
         | _ ->
             (* the phase tail is load-bearing, the WF-044 sisters' pattern:
                the ASP backend refuses the same bound in the same words and
                a pin that stopped before the tail could not tell the two
                phases apart, so a swap would pass unseen *)
             Value.fault
               "the %s bound of a period condition is neither a date literal nor an own scalar \
                date attribute of the subject: ch. 3.10's mirror note carries those two and defers \
                the general date expression WF-042 admits. Declined at evaluation"
               side);
        eval_expr env world self ~err e
      in
      let v = match vanaf with Some e -> Some (bound "vanaf" e) | None -> None in
      let t = match tot with Some e -> Some (bound "tot" e) | None -> None in
      if v = None && t = None then
        Value.fault "a period condition without any bound has no window to gate on (WF-042)";
      let day = function
        | None -> None (* an absent bound is open *)
        | Some Value.Empty -> Some None (* a leeg bound never matches *)
        | Some (Value.Date z) -> Some (Some z)
        | Some other ->
            Value.fault "a period condition's bound is %s, which is not a date (WF-042)"
              (Value.to_string other)
      in
      (match (env.as_of, day v, day t) with
       | None, _, _ -> false (* an unset evaluation date never matches *)
       | _, Some None, _ | _, _, Some None -> false
       | Some d, v, t -> (
           match Value.date_of_iso d with
           | Value.Date today ->
               (match v with Some (Some z) -> z <= today | _ -> true)
               && (match t with
                   | Some (Some z) -> if tot_inclusief then today <= z else today < z
                   | _ -> true)
           | _ -> Value.fault "malformed asOfDate %S" d))
  | Conditie_onopgelost _ -> Value.fault "Onopgelost condition reached in evaluation (WF-044)"

(* --- day kinds as duration units (ch. 3.7/3.8) --------------------------
   Two steps stand between a unit SPELLING on a duration node and a verdict
   about one calendar day. Resolution: the spelling names a day kind through
   `dagsoortPerEenheid` (trimmed, case-insensitive — IR-SPEC ch. 2.9), and
   the name names its definition in `resolvedDagsoorten`. Evaluation: the
   definition's voorwaarde is an ordinary condition, evaluated with the day
   bound to `de dag` and with no other binder in scope — WF-031's purity
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
  | None -> Value.fault "day kind %S has no definition (WF-031)" name

and day_kind_matches env world self ~err (name : string) (day : int) : bool =
  if List.exists (fun n -> norm n = norm name) !day_kind_stack then
    Value.fault
      "day-kind reference cycle at %S: a day-kind definition that references itself has no \
       fixpoint (WF-085)"
      name;
  let def = day_kind_definition env name in
  let c = match def.rd_voorwaarde with
    | Some c -> c
    | None -> Value.fault "day kind %S has a definition without a condition (WF-031)" name
  in
  let saved = !day_kind_stack in
  day_kind_stack := name :: saved;
  Fun.protect
    ~finally:(fun () -> day_kind_stack := saved)
    (fun () ->
      let env = { env with locals = SMap.add day_kind_binder (Now (Value.Date day)) env.locals } in
      eval_cond env world self ~err c)

(* the day kind a duration UNIT spelling names, or None when the spelling is
   not registered as one (then it is a calendar unit, or ill-formed) *)
and day_kind_of_unit env (u : eenheid) : string option =
  match u.afkorting with
  | None -> None
  | Some a -> (
      match Ir_v0.Checks.find_ci env.doc.dagsoort_per_eenheid a with
      | Some (Some k) when String.trim k <> "" -> Some k
      | _ -> None)

and day_kind_pred env world self ~err (name : string) : int -> bool =
  fun day -> day_kind_matches env world self ~err name day

let rule_name (r : resolved_regel) = Option.value r.rr_naam ~default:"<naamloos>"

(* --- rule forms --------------------------------------------------------- *)

(* run one form over its subjects; [body] gets the fresh instance and an
   error collector, returns (world, changed); collected §9.5 reasons land
   as channel facts for (rule × subject) *)
let over_subjects ?(exact = false) ?(behalve = []) env world ~otype ~rule body =
  let isa a b =
    (if exact then norm a = norm b else isa env a b)
    (* the ch. 7.10 absorption: on these concrete types the rule IS a
       defaults base and the stack writes it, so firing here too would put
       two writers on one target *)
    && not (List.exists (fun t -> norm t = norm a) behalve)
  in
  List.fold_left
    (fun (w, changed) (snapshot : World.instance) ->
      let self = World.get w snapshot.label in
      let pending = ref [] in
      let err reason = pending := reason :: !pending in
      let w, ch = body w self err in
      let w =
        List.fold_left
          (fun w (_ : string) -> World.insert_error w ~rule ~subject:self.label)
          w !pending
      in
      (w, changed || ch))
    (world, false) (World.of_type ~isa world otype)

(* ch. 3.7: the pro-rata target — the one implemented temporal write.
   The value MUST be tijdsevenredig-deel over an own temporal attribute
   with a gedurende condition on an own temporal characteristic (the
   corpus shape; everything else faults). The base must be as coarse as
   or coarser than the bucket, and the target's declared granularity
   must equal the node's. An absent base timeline or a condition
   without true periods writes the EMPTY timeline (v0: a null base
   yields the empty list — the target exists, with no segments). *)
let run_pro_rata env w (self : World.instance) ~ot ~attr waarde =
  let ot_decl = objecttype env ot in
  match waarde with
  | Functie
      { kind = Tijdsevenredig_deel;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some b; objecttype = Some bot; navigatie_rol = None;
                navigatie_pad = []; set = false; _ } ];
        filter = Some c; granulariteit = Some g; leegwaarde = None; dimensie_selectie = None; _ }
    when norm bot = norm ot ->
      let target_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit attr with
        | Some (Some tg) -> tg
        | _ -> Value.fault "pro-rata target %S without a granularity" attr
      in
      if target_gran <> g then
        Value.fault
          "pro-rata bucket granularity differs from the target's declared granularity (WF-056)";
      let base_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit b with
        | Some (Some bg) -> bg
        | _ -> Value.fault "pro-rata base %S is not a temporal attribute (WF-056)" b
      in
      if base_gran < g then
        Value.fault "pro-rata base granularity is finer than the bucket (WF-056)";
      (match Ir_v0.Checks.find_ci ot_decl.attributen b with
      | Some (Some (Numeriek { eenheid = Some _; _ } | Percentage { eenheid = Some _; _ })) ->
          (* ch. 3.7: a rate base restates by its exact factor FIRST —
             not executed here; refuse rather than prorate factor-free *)
          Value.fault "pro-rata over a unit-carrying base is not executed (no corpus case)"
      | _ -> ());
      clip_member_qualifications c;
      let true_periods =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
            if not (kenmerk_temporal ot_decl k) then
              Value.fault "gedurende over %S, which is not a temporal characteristic (WF-056)" k;
            Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
        | _ ->
            Value.fault
              "unsupported pro-rata condition shape (an own temporal characteristic; no corpus \
               case for more)"
      in
      let basis = Option.value (World.timeline self b) ~default:[] in
      let result = Timeline.pro_rata g basis true_periods in
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
          "day-count bucket granularity differs from the target's declared granularity (WF-058)";
      clip_member_qualifications c;
      let true_periods =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
            if not (kenmerk_temporal ot_decl k) then
              Value.fault "gedurende over %S, which is not a temporal characteristic (WF-058)" k;
            Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
        | _ ->
            Value.fault
              "unsupported day-count condition shape (an own temporal characteristic; no corpus \
               case for more)"
      in
      let result = Timeline.aantal_dagen_in g true_periods in
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

(* ch. 3.10's case distinction is on the rule's TARGET, and a condition cannot
   see one — so the clip case is decided here, where the target is known, and
   never inside [eval_cond]. Without this walk a period condition on a
   temporal target would silently get the SCALAR gate: all-or-nothing on the
   evaluation date where the chapter says the window speaks about the
   timeline. That is the wrong axis, not a near miss. *)
let rec bevat_periode (c : conditie) =
  match c with
  | Periode_conditie _ -> true
  | Samengesteld { onderdelen; _ } -> List.exists bevat_periode onderdelen
  | Kwantorvouw { kv_filter; kv_toets; _ } ->
      let opt = function Some c -> bevat_periode c | None -> false in
      opt kv_filter || opt kv_toets
  | Vergelijking _ | Predicaat _ | Kenmerk_check _ | Rollidmaatschap _ | Verre_rollidmaatschap _
  | Regelversie _ | Conditie_onopgelost _ ->
      false

let run_rule env world (r : resolved_regel) =
  if not (applies env r.rr_geldigheid) then (world, false)
  else
    match r.doel with
    | Some (Attribuut_ref { attribuut; objecttype = Some ot; _ }) ->
        let attr = req "doel.attribuut" attribuut in
        let temporal_target =
          match r.rr_soort with
          | Some Kenmerktoekenning -> false
          | _ -> Ir_v0.Checks.attr_temporal (objecttype env ot) attr
        in
        if temporal_target && Option.fold ~none:false ~some:bevat_periode r.rr_voorwaarde then
          Value.fault
            "a period condition on the temporal target %S is ch. 3.10's CLIP, not the \
             evaluation-date gate: the window intersects the written timeline instead of deciding \
             whether the rule applies, and that half of the chapter is not built. Declined at \
             evaluation"
            attr;
        (* ch. 3.11: the only implemented temporal write is the ch. 3.7
           pro-rata target; every other temporal target refuses — never
           silently coerce a scalar into a timeline *)
        (match r.rr_soort with
        | Some Kenmerktoekenning ->
            if kenmerk_temporal (objecttype env ot) attr then
              Value.fault "temporal characteristic %S: temporal writes are not implemented" attr
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
              match r.rr_voorwaarde with Some c -> eval_cond env w self ~err c | None -> true
            in
            if not fire then (w, false)
            else
              match r.rr_soort with
              | Some Kenmerktoekenning -> (
                  match eval_expr env w self ~err (req "waarde" r.waarde) with
                  | Value.Bool true -> World.set_char w ~label:self.label ~char:attr
                  | Value.Bool false -> (w, false)
                  | v -> Value.fault "characteristic value is not boolean: %s" (Value.to_string v))
              | Some (Gelijkstelling | Berekening) when temporal_target ->
                  run_pro_rata env w self ~ot ~attr (req "waarde" r.waarde)
              | Some (Gelijkstelling | Berekening) ->
                  let v = eval_expr env w self ~err (req "waarde" r.waarde) in
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
                  (* writes only when the target is empty (ch. 5.4) *)
                  if World.attr self attr <> Value.Empty then (w, false)
                  else
                    let v = eval_expr env w self ~err (req "waarde" r.waarde) in
                    World.write_attr w ~label:self.label ~attr ~decl:(attr_decl env ~otype:ot ~attr)
                      v
              | None -> Value.fault "rule %S has no kind" (rule_name r))
    | _ -> Value.fault "rule %S: unsupported target shape" (rule_name r)

let run_verdeling env world (v : resolved_verdeling) =
  if not (applies env v.vd_geldigheid) then (world, false)
  else begin
    let name = Option.value v.vd_naam ~default:"<verdeling>" in
    let bron_type = req "bronType" v.bron_type in
    let doel_type = req "doelType" v.doel_type in
    let bron_attr = req "bronAttribuut" v.bron_attribuut in
    let doel_attr = req "doelAttribuut" v.doel_attribuut in
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
              (linked ?binding:v.verbindende_rol env w src ~to_type:doel_type)
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
            Alloc.allocate
              (Option.value v.vd_methode ~default:Complex)
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
   navigate the join chain (anchor → tussenstappen → joinRol → target)
   over existing links, and link anchor and target in the created
   relation — both ends are written, so navigation from either side
   sees the link. Naturally idempotent: an already-present link is a
   no-op write, so a target reached along two paths links once. Only
   the symbolic extension form (IR-SPEC ch. 3.4, WF-028) carries the
   relational content; a bean-field-only v0 record faults. *)
let run_feit_creatie env world (fc : resolved_feit_creatie) =
  if not (applies env fc.fc_geldigheid) then (world, false)
  else begin
    let name = Option.value fc.fc_naam ~default:"<feitcreatie>" in
    let ftn =
      match fc.fc_feittype with
      | Some f -> f
      | None ->
          Value.fault
            "fact creation %S carries only bean fields; the symbolic form (IR-SPEC ch. 3.4) is \
             what executes"
            name
    in
    let role rname = bound_role env { rb_feittype = ftn; rb_rol = rname } in
    let anker_rol = role (req "FeitCreatie.ankerRol" fc.anker_rol) in
    let doel_rol = role (req "FeitCreatie.doelRol" fc.doel_rol) in
    (* ch. 5.5: the host splits the guard into the two readings this position
       has. On the doel it FILTERS the targets — so the chain is walked first
       and the condition asked once per target — and on the anker it GATES the
       anker, which short-circuits the chain exactly as before. *)
    let doel_guard = if guard_on_doel env.doc fc then fc.fc_voorwaarde else None in
    let anker_guard = if doel_guard = None then fc.fc_voorwaarde else None in
    over_subjects env world ~otype:(req "FeitCreatie.anker" fc.anker) ~rule:name
      (fun w self err ->
        let fire =
          match anker_guard with Some c -> eval_cond env w self ~err c | None -> true
        in
        if not fire then (w, false)
        else
          let hop insts rb =
            List.concat_map (fun i -> role_links w i (bound_role env rb)) insts
          in
          let mids =
            List.fold_left
              (fun insts (t : tussenstap) -> hop insts (req "Tussenstap.rolBinding" t.ts_rol))
              [ self ] fc.tussenstappen
          in
          let targets = hop mids (req "FeitCreatie.joinRol" fc.join_rol) in
          let targets =
            match doel_guard with
            | None -> targets
            | Some c -> List.filter (fun t -> eval_cond env w t ~err c) targets
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

(* ch. 5.5, non-recursive object creation: for each subject whose
   condition holds, mint a deterministic instance of the created type,
   apply the member initialisations (values evaluated on the subject;
   characteristic constants), and link new object and subject over the
   uniquely-derivable connecting relation — both ends. The idempotence
   contract, mirrored from the pinned runtime: a 1:1 shape (the
   created-side role is to-one) guards on the absence of an already
   linked instance in the INPUT world — a case-supplied linked instance
   blocks the creation (the snapshot narrowing is a recorded delta:
   v0's guard is a live fact-base negation) — while the to-many and
   linkless shapes guard per (rule × subject) through the
   deterministic label. *)
let run_object_creatie env ~world0 ~idx world (oc : resolved_object_creatie) =
  if not (applies env oc.oc_geldigheid) then (world, false)
  else begin
    let name = Option.value oc.oc_naam ~default:"<objectcreatie>" in
    let nieuw = req "ObjectCreatie.nieuwType" oc.nieuw_type in
    let subject = req "ObjectCreatie.subject" oc.oc_subject in
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
    over_subjects env world ~otype:subject ~rule:name (fun w self err ->
        let fire =
          match oc.oc_voorwaarde with Some c -> eval_cond env w self ~err c | None -> true
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
            let w =
              World.add_instance w
                { World.label; otype = nieuw; attrs = SMap.empty; chars = [];
                  links = SMap.empty; tls = SMap.empty; ctls = SMap.empty }
            in
            let w =
              List.fold_left
                (fun w (t : toekenning) ->
                  let m = req "Toekenning.attribuut" t.toe_attribuut in
                  if t.toe_kenmerk then (
                    match t.toe_waarde with
                    | Some (Literal { waarde = Some "waar"; _ }) ->
                        fst (World.set_char w ~label ~char:m)
                    | Some (Literal { waarde = Some "onwaar"; _ }) -> w
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
                        match eval_expr env w self ~err e with
                        | Value.Bool true -> fst (World.set_char w ~label ~char:m)
                        | Value.Bool false | Value.Empty -> w
                        | v ->
                            Value.fault
                              "characteristic initialisation of %S is not boolean: %s (WF-027)"
                              m (Value.to_string v))
                    | None ->
                        Value.fault
                          "characteristic initialisation of %S without a value (WF-027)" m)
                  else
                    let v = eval_expr env w self ~err (req "Toekenning.waarde" t.toe_waarde) in
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
   supertype's stack and the subtype's on one instance. *)
let run_defaults_group env world ~owner (members : prioritized_defaults list) =
  let l0 = List.hd members in
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
  (* the WF-086 kenmerk arm's defense-in-depth: a temporal characteristic target would
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
      let member_decision (l : prioritized_defaults) : chain_result =
        if not (applies env l.pd_geldigheid) then Undecided
        else
          let is_char = l.pd_kenmerk <> None in
          let justified (r : default) err =
            if r.default_initialisatie then (
              match l.pd_attribuut with
              | Some a -> World.attr self a = Value.Empty
              | None -> Value.fault "initialisation default on characteristic defaults")
            else eval_cond env w self ~err (req "default justification" r.default_voorwaarde)
          in
          (* the default's OWN decision, exception groups aside (a plain default, the
             initialisation default, or a form-a default's fall-back): Some when it
             decides, None when it falls through *)
          let own_decision (r : default) : defaults_decision option =
            if is_char then (
              let j, flush = speculate (fun err -> justified r err) in
              if j then (
                flush ();
                Some (Char_dec (req "default besluit" r.default_besluit)))
              else None)
            else
              let outcome, flush =
                speculate (fun err ->
                    if justified r err then
                      Some
                        (eval_expr env w self ~err
                           (req "default consequentie" r.default_consequentie))
                    else None)
              in
              match outcome with
              | Some v when v <> Value.Empty ->
                  flush ();
                  Some (Attr_dec v)
              | Some _ (* leeg: falls through *) | None -> None
          in
          let rec decide ~path (defaults : default list) : chain_result =
            let rec go i = function
              | [] -> Undecided
              | (r : default) :: rest -> (
                  let fall_through () =
                    (* a form-a default falls back to its own decision; a
                       group-default (no own content) decides nothing (ch. 7.9) *)
                    let has_content =
                      r.default_besluit <> None || r.default_consequentie <> None
                    in
                    if has_content then
                      match own_decision r with Some d -> Decided d | None -> go (i + 1) rest
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
                          (fun (eg : exception_group) -> decide ~path:sub_path eg.eg_defaults)
                          egs.egs_leden
                      in
                      match reconcile ~path:sub_path results with
                      | (Conflict _ | Decided _) as d -> d
                      | Undecided -> fall_through ())
                  | None -> (
                      match own_decision r with
                      | Some d -> Decided d
                      | None -> go (i + 1) rest))
            in
            go 0 defaults
          in
          decide ~path:[] l.defaults
      in
      match reconcile ~path:[] (List.map member_decision members) with
      | Undecided -> (w, false)
      | Conflict rule ->
          (* over-determination (ch. 7.2/7.9): no write, one channel fact —
             bare-keyed at the root, path-keyed for a nested exception group *)
          (World.insert_error w ~rule ~subject:self.label, false)
      | Decided (Char_dec true) ->
          World.set_char w ~label:self.label ~char:(req "defaults kenmerk" l0.pd_kenmerk)
      | Decided (Char_dec false) -> (w, false)
      | Decided (Attr_dec v) ->
          let a = req "defaults attribuut" l0.pd_attribuut in
          World.write_attr w ~label:self.label ~attr:a
            ~decl:(attr_decl env ~otype:owner ~attr:a)
            v)

(* consistency and uniqueness fire at the computed fixpoint; violations
   are idempotent channel facts (ch. 5.6) *)
let run_consistentie env world (c : resolved_consistentie) =
  if not (applies env c.cons_geldigheid) then world
  else
    let name = Option.value c.cons_naam ~default:"<consistentie>" in
    let world, _ =
      over_subjects env world ~otype:(req "Consistentie.subject" c.cons_subject) ~rule:name
        (fun w self err ->
          if eval_cond env w self ~err (req "assertie" c.assertie) then (w, false)
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
  tuples [] keys
  |> List.map (fun vs -> String.concat "\x00" (List.map Value.to_string vs))
  |> List.sort_uniq String.compare

let run_uniciteit env world (u : resolved_uniciteit) =
  if not (applies env u.uni_geldigheid) then world
  else begin
    let name = Option.value u.uni_naam ~default:"<uniciteit>" in
    let subject = req "Uniciteit.subject" u.uni_subject in
    (* the scope hop of a parent-scoped uniqueness (ch. 5.9, WF-054):
       keys are unique within the sub-population sharing one scope
       parent; an instance without a scope parent does not participate —
       the pinned runtime's silent narrowing, mirrored and recorded *)
    let scope_of =
      match (u.scope_rol, u.scope_veld) with
      | Some rb, _ ->
          let role = bound_role env rb in
          Some (fun i -> role_links world i role)
      | None, Some sv ->
          let role = role_by_name env ~self_type:subject ~name:sv in
          Some (fun i -> role_links world i role)
      | None, None -> None
    in
    let keys = List.map (req "sleutel") u.sleutels in
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
        "a uniqueness union carries at least two sources (WF-030; the v0 resolver never \
         produces fewer)";
    (* one row per (instance × source) — v0 SUMS the per-source counts,
       so an instance reached through two sources contributes its tuple
       twice and duplicates against itself, exactly like the runtime *)
    let keyed =
      List.concat_map
        (fun (b : uniciteit_unie_bron) ->
          let t = req "UniciteitUnie.bron.type" b.uub_type in
          let keys = List.map (req "sleutel") b.uub_sleutels in
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
   read: TemporalRead of a ParameterRef indexed by Rekendatum) must have
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
      when norm n = norm "Rekendatum" ->
        if not (List.exists (fun q -> norm q = norm p) !acc) then acc := p :: !acc
    | Temporal_read { basis; datum; _ } ->
        expr basis;
        expr datum
    | Literal _ | Attribuut_ref _ | Parameter_ref _ | Variabele_ref _ | Constante _
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
    | Kenmerk_check _ | Rollidmaatschap _ | Verre_rollidmaatschap _ | Regelversie _
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

let fixpoint ~budget step world =
  let rec go n world =
    if n > budget then
      Value.fault "no fixpoint within %d passes — divergence is a fault (ch. 5.7)" budget
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
     to converge is a cycle the verifier should have rejected. *)
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
      Value.fault "no evaluation strata exist — a dependency cycle (WF-045/046) reached ref/";
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
  let stratum_of at = match Hashtbl.find_opt stratum_by_at at with Some i -> strata.(i) | None -> 0 in
  let kmax = Array.fold_left max 0 strata in
  let rule_stratum i = stratum_of (Printf.sprintf "/resolvedRegels/%d" i) in
  let verdeling_stratum i = stratum_of (Printf.sprintf "/verdelingen/%d" i) in
  let defaults_stratum i = stratum_of (Printf.sprintf "/prioritizedDefaults/%d" i) in
  let fc_stratum i = stratum_of (Printf.sprintf "/feitCreaties/%d" i) in
  let oc_stratum i = stratum_of (Printf.sprintf "/objectCreaties/%d" i) in
  let cons_stratum i = stratum_of (Printf.sprintf "/consistenties/%d" i) in
  let uni_stratum i = stratum_of (Printf.sprintf "/uniciteiten/%d" i) in
  let uu_stratum i = stratum_of (Printf.sprintf "/uniciteitUnies/%d" i) in
  let indexed l = List.mapi (fun i x -> (i, x)) l in
  (* defaults groups (ch. 7.2): members share one (subject, target); a
     group runs at the max of its members' strata *)
  (* One stack per (CONCRETE type, target): inheritance composed the
     inherited declarations into it (RUNTIME-SEMANTICS ch. 7.10), so what
     runs here is already the effective stack and its subject range is
     exactly that type. In a document without a supertype edge this is the
     declared grouping, unchanged. A stratum is the max over the
     declarations the stack was composed from. *)
  let defaults_groups =
    List.map
      (fun (owner, _key, members, idxs) ->
        let stratum = List.fold_left (fun acc i -> max acc (defaults_stratum i)) 0 idxs in
        (stratum, owner, members))
      (effective_defaults env)
  in
  let pass ~k world =
    let world, c1 =
      List.fold_left
        (fun (w, ch) (i, r) ->
          if rule_stratum i > k then (w, ch)
          else
            let w, c = run_rule env w r in
            (w, ch || c))
        (world, false)
        (indexed env.doc.resolved_regels)
    in
    let world, c2 =
      List.fold_left
        (fun (w, ch) (i, v) ->
          if verdeling_stratum i > k then (w, ch)
          else
            let w, c = run_verdeling env w v in
            (w, ch || c))
        (world, false)
        (indexed env.doc.verdelingen)
    in
    let world, c3 =
      List.fold_left
        (fun (w, ch) (stratum, owner, members) ->
          if stratum > k then (w, ch)
          else
            let w, c = run_defaults_group env w ~owner members in
            (w, ch || c))
        (world, false) defaults_groups
    in
    let world, c4 =
      List.fold_left
        (fun (w, ch) (i, fc) ->
          if fc_stratum i > k then (w, ch)
          else
            let w, c = run_feit_creatie env w fc in
            (w, ch || c))
        (world, false)
        (indexed env.doc.feit_creaties)
    in
    let world, c5 =
      List.fold_left
        (fun (w, ch) (i, oc) ->
          if oc_stratum i > k then (w, ch)
          else
            let w, c = run_object_creatie env ~world0 ~idx:i w oc in
            (w, ch || c))
        (world, false)
        (indexed env.doc.object_creaties)
    in
    (world, c1 || c2 || c3 || c4 || c5)
  in
  (* the ch. 5.5/5.7 bounded budget: a recursive chain advances one
     generation per pass, so this also caps runaway recursion — an
     order of magnitude above any legitimate corpus fixpoint (~7
     passes), and deliberately EQUAL to the ASP backend's oc_budget so
     both executors fault a runaway chain at the same generation
     (clingo's nested creation terms cap the reachable depth anyway:
     its term handling aborts near depth ~200 — recorded in the
     backend README) *)
  let budget = 100 in
  (* for k = 0, 1, …: the forms of stratum <= k evaluate to their
     fixpoint before any form of stratum k+1 fires (ch. 6.3); the
     consistency and uniqueness forms of stratum k fire once at that
     fixpoint — their channel facts are idempotent (ch. 5.6) *)
  let world = ref world in
  for k = 0 to kmax do
    world := fixpoint ~budget (pass ~k) !world;
    world :=
      List.fold_left
        (fun w (i, c) -> if cons_stratum i = k then run_consistentie env w c else w)
        !world
        (indexed env.doc.consistenties);
    world :=
      List.fold_left
        (fun w (i, u) -> if uni_stratum i = k then run_uniciteit env w u else w)
        !world
        (indexed env.doc.uniciteiten);
    world :=
      List.fold_left
        (fun w (i, u) -> if uu_stratum i = k then run_uniciteit_unie env w u else w)
        !world
        (indexed env.doc.uniciteit_unies)
  done;
  !world
