(* The v0 desugaring of RUNTIME-SEMANTICS ch. 7.7: on one
   (type, attribute) target, one version-free computing rule
   (Gelijkstelling/Berekening) plus at most one unconditional,
   version-free Initialisatie read as one attribute defaults group — the
   computing rule's condition becomes the default justification (an
   unconditional rule gets the vacuously true empty all), the
   initialisation becomes the ch. 7.3 init default. Forms outside the
   domain are left untouched; a caller that needs totality checks
   [remainder] and faults, never guesses.

   The lowering is document-to-document so a backend can consume the
   defaults machinery while the reference interpreter keeps replaying
   the v0 forms natively — the differential (gate 6) then witnesses
   the desugaring itself. *)

open Ast

let norm = Checks.norm

let version_free (g : geldigheid option) =
  match g with None | Some { geld_vanaf = None; geld_tot = None } -> true | _ -> false

(* the vacuously true justification: all over zero parts *)
let always_true : conditie =
  Samengesteld { kwantor = Alle; drempel = 0; onderdelen = []; bron = Some "altijd" }

let target_of (r : resolved_regel) =
  match r.doel with
  | Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }) -> Some (ot, a)
  | _ -> None

(* Is this rule inside the desugaring's domain for its target? *)
let is_computing (r : resolved_regel) =
  (match r.rr_soort with Some (Gelijkstelling | Berekening) -> true | _ -> false)
  && version_free r.rr_geldigheid
  && r.waarde <> None

let is_plain_init (r : resolved_regel) =
  r.rr_soort = Some Initialisatie
  && r.rr_voorwaarde = None
  && version_free r.rr_geldigheid
  && r.waarde <> None

let key (ot, a) = norm ot ^ ":" ^ norm a

(* The ch. 7.7 domain: every (type, attribute) target carried entirely
   by domain forms, with the rules that carry it. *)
let chosen_targets (doc : document) =
  (* collect rules per attribute target *)
  let targets =
    List.fold_left
      (fun acc r ->
        match target_of r with
        | Some t when r.rr_soort <> Some Kenmerktoekenning ->
            let k = key t in
            if List.mem_assoc k acc then
              List.map (fun (k', (t', rs)) -> if k' = k then (k', (t', rs @ [ r ])) else (k', (t', rs))) acc
            else acc @ [ (k, (t, [ r ])) ]
        | _ -> acc)
      [] doc.resolved_regels
  in
  let desugarable (_, (_, rs)) =
    let inits = List.filter (fun r -> r.rr_soort = Some Initialisatie) rs in
    let comps = List.filter is_computing rs in
    (* exactly one initialisation: the init default is what the desugaring
       exists for — the bare computation stays structurally outside the
       desugaring and carries the defaults reading directly through the
       ch. 7.8 standing norm (standing_targets below) *)
    List.length rs = List.length inits + List.length comps
    && List.length inits = 1
    && List.for_all is_plain_init inits
    && List.length comps <= 1
  in
  List.filter desugarable targets

(* The ch. 7.7 desugaring domain as target keys — shared with ref/,
   whose native replay gives these targets the same fall-through the
   desugared defaults carry (RS ch. 7.7/7.8: a computing rule that
   evaluates leeg writes nothing, so the init/input decides and the
   fire-but-leeg corner is defined instead of divergent). *)
let desugared_targets (doc : document) : (string * string) list =
  List.map (fun (_, ((ot, a), _)) -> (norm ot, norm a)) (chosen_targets doc)

(* Desugar every target carried entirely by domain forms; leave every
   other rule untouched. Returns the rewritten document. *)
let document (doc : document) : document =
  let chosen = chosen_targets doc in
  if chosen = [] then doc
  else
    let in_chosen r =
      match target_of r with
      | Some t -> List.exists (fun (k, _) -> k = key t) chosen
      | None -> false
    in
    let defaults_of (_, ((ot, a), rs)) : prioritized_defaults =
      let comp_defaults =
        List.filter_map
          (fun r ->
            if is_computing r then
              Some
                {
                  default_besluit = None;
                  default_voorwaarde =
                    Some (Option.value r.rr_voorwaarde ~default:always_true);
                  default_consequentie = Some (Option.get r.waarde);
                  default_initialisatie = false;
                  default_excepties = None;
                  default_bron = r.rr_naam;
                }
            else None)
          rs
      in
      let init_defaults =
        List.filter_map
          (fun r ->
            if r.rr_soort = Some Initialisatie then
              Some
                {
                  default_besluit = None;
                  default_voorwaarde = None;
                  default_consequentie = Some (Option.get r.waarde);
                  default_initialisatie = true;
                  default_excepties = None;
                  default_bron = r.rr_naam;
                }
            else None)
          rs
      in
      {
        pd_naam = Some ("desugared: " ^ a ^ " van " ^ ot);
        pd_onderwerp = ot;
        pd_kenmerk = None;
        pd_attribuut = Some a;
        defaults = comp_defaults @ init_defaults;
        pd_ongeordend = false;
        pd_bron = Some "ch. 7.7 v0 desugaring";
        pd_geldigheid = None;
      }
    in
    {
      doc with
      resolved_regels = List.filter (fun r -> not (in_chosen r)) doc.resolved_regels;
      prioritized_defaults = doc.prioritized_defaults @ List.map defaults_of chosen;
    }

(* The standing domain of RUNTIME-SEMANTICS ch. 7.8: the bare scalar
   computing target — one version-free Gelijkstelling/Berekening as its
   target's only writer (no initialisation beside it: that pair is the
   ch. 7.7 desugaring's domain above; no prioritized defaults, allocation, or
   creation-time write). On these targets the rule decides like a default —
   a leeg evaluation writes nothing and a case-supplied input value
   stands. Shared by ref/ (the no-write on leeg) and the ASP backend
   (the defeasible inputval/3 entry), so the domain has one definition. *)
let standing_targets_gen ~temporal (doc : document) : (string * string) list =
  let key (ot, a) = (norm ot, norm a) in
  let written_by_rules =
    List.fold_left
      (fun acc r ->
        match target_of r with
        | Some t when r.rr_soort <> Some Kenmerktoekenning -> key t :: acc
        | _ -> acc)
      [] doc.resolved_regels
  in
  let count k = List.length (List.filter (( = ) k) written_by_rules) in
  let other_writer (ot, a) =
    List.exists
      (fun (l : prioritized_defaults) ->
        norm l.pd_onderwerp = norm ot
        && match l.pd_attribuut with Some la -> norm la = norm a | None -> false)
      doc.prioritized_defaults
    || List.exists
         (fun (v : resolved_verdeling) ->
           (match v.doel_type with Some dt -> norm dt = norm ot | None -> false)
           && match v.doel_attribuut with Some da -> norm da = norm a | None -> false)
         doc.verdelingen
    || List.exists
         (fun (o : resolved_object_creatie) ->
           (match o.nieuw_type with Some nt -> norm nt = norm ot | None -> false)
           && List.exists
                (fun (t : toekenning) ->
                  (not t.toe_kenmerk)
                  && match t.toe_attribuut with Some ta -> norm ta = norm a | None -> false)
                o.toekenningen)
         doc.object_creaties
  in
  let wanted_shape (ot, a) =
    match List.find_opt (fun (n, _) -> norm n = norm ot) doc.objecttypen with
    | Some (_, decl) -> Checks.attr_temporal decl a = temporal
    | None -> false
  in
  (* the temporal domain is additionally shape-exact: only the ch. 3.7
     pro-rata fold executes as a temporal writer today, and a
     shape-agnostic domain would skip the backend's input fence for
     rules that then merely shape-fault — keep the fence in charge
     there instead *)
  let executable (r : resolved_regel) =
    (not temporal)
    ||
    match r.waarde with
    | Some (Functie { kind = Tijdsevenredig_deel | Aantal_dagen_in; _ }) -> true
    | _ -> false
  in
  List.filter_map
    (fun r ->
      match target_of r with
      | Some t
        when is_computing r
             && executable r
             && count (key t) = 1
             && wanted_shape t
             && not (other_writer t) ->
          Some (key t)
      | _ -> None)
    doc.resolved_regels

let standing_targets = standing_targets_gen ~temporal:false

(* --- inheritance: the effective defaults stack per type (RUNTIME-SEMANTICS
   ch. 7.10) ---------------------------------------------------------------

   A subtype inherits, per target, the whole defaults stack of its supertype;
   its own declarations fold over the parent's EFFECTIVE stack (not the
   parent's declarations, so a three-level chain needs no rule of its own).
   One fixed block order, highest first, and nothing on the wire declares it:

     1. the subtype's own exceptions, in declared order;
     2. the inherited exceptions, in inherited order — an inherited
        group-default (exceptions, no own consequence) is not a base and
        rides at the bottom of this block;
     3. the base: the subtype's own if it has one, which REPLACES the
        inherited base, else the inherited base;
     4. the initialisation and virtual input defaults, never replaced.

   The result is ONE stack per (concrete type, target), which is why a group
   runs on instances of EXACTLY its owner type: extensional quantification of
   the defaults subject is already spent here, in the composition. Reading
   the subject extensionally as well would put two writers on one instance.

   Shared by ref/ and the ASP backend, like the ch. 7.7 desugaring above, so
   the two executors cannot drift on the block order. *)

let is_init (r : default) = r.default_initialisatie
let default_has_content (r : default) = r.default_besluit <> None || r.default_consequentie <> None

(* (exceptions, base, initialisation tail) of one member's ordered stack.
   The base is the LOWEST ordinary default, and only when it decides
   something itself: a group-default delegates to its exception groups and is
   therefore not a base (rule 2), and a declared-unordered member has no
   "lowest" at all, so its whole block travels as one (rule 3). *)
let split_stack (l : prioritized_defaults) =
  let head = List.filter (fun r -> not (is_init r)) l.defaults in
  let tail = List.filter is_init l.defaults in
  if l.pd_ongeordend then (head, None, tail)
  else
    match List.rev head with
    | last :: rest when default_has_content last -> (List.rev rest, Some last, tail)
    | _ -> (head, None, tail)

let compose_member ~(own : prioritized_defaults) ~(inherited : prioritized_defaults) =
  let own_exc, own_base, own_tail = split_stack own in
  let inh_exc, inh_base, inh_tail = split_stack inherited in
  let base = match own_base with Some _ as b -> b | None -> inh_base in
  { own with defaults = own_exc @ inh_exc @ Option.to_list base @ inh_tail @ own_tail }

exception Unlowered of string

(* --- an ORDINARY rule as the base of its level (SUBTYPING §4.1 rules 1
   and 4) -----------------------------------------------------------------

   ch. 7.8 already reads a bare computing rule as a default: it decides,
   a leeg evaluation writes nothing, the input stands. Inheritance takes
   that reading at its word — the rule IS the one-member base of the level
   that declares it, so a subtype's own rule replaces an inherited base
   (rule 1) and an ancestor's rule becomes the descendant's inherited base
   (rule 4). No new wire form: the rule is folded into the stack exactly as
   the ch. 7.7 desugaring above folds one, condition and all.

   The fold is GATED, and the gate is what keeps every flat model byte for
   byte where it was: a rule is absorbed only on a target whose CHAIN
   declares a defaults group somewhere. A bare rule with no group above or
   below it is the ordinary ch. 7.8 standing norm and never enters a stack
   — which is every such rule in the corpus. The gate is
   [Checks.chain_group_keys], which WF-079 reads as well: outside it a bare
   rule on a type and one on its ancestor both fire on the type's instances,
   and the verifier refuses that pair. *)

(* The ordinary attribute writers [level] declares on [key], with their
   document index: at most one computing rule and at most one plain
   initialisation — the ch. 7.7 pair. Two computing rules on one target are
   agenda order, which is what prioritized defaults exist to replace, so
   they are left for the caller to refuse. *)
(* EVERY ordinary attribute writer [level] declares on [key] — the FILTER is
   deliberately on the target and nothing else. An earlier revision filtered
   out the forms it could not fold (a validity window, an absent value) and
   that was a silent-answer bug rather than a narrowing: [absorbed_on] takes
   the rule out of the rule circuit per (type, attribute), so a rule the fold
   skipped was silenced without ever becoming a base and its writer vanished
   from all three executors. Measured 2026-09-12 on two verifier-clean
   documents: a windowed rule on the ancestor left the target leeg where the
   loud refusal used to stand, and a windowed rule that was ACTIVE on the
   evaluation date answered from the inherited base instead of its own value.
   Collect everything here; [rule_member] refuses by name on anything it
   cannot fold. *)
let level_rules doc level key =
  List.concat
    (List.mapi
       (fun i (r : resolved_regel) ->
         match (r.rr_soort, r.doel) with
         | ( Some ((Gelijkstelling | Berekening | Initialisatie) as soort),
             Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }) )
           when norm ot = norm level && "A:" ^ norm a = key ->
             [ (soort, r, i) ]
         | _ -> [])
       doc.resolved_regels)

(* [level]'s own contribution to [key] when it declares no defaults group:
   its rules, read as the ch. 7.7 shape — the computing rule as the base,
   the initialisation below it. None when the level writes the target with
   no ordinary rule either. *)
let rule_member doc level key : (prioritized_defaults * int) option =
  let rules = level_rules doc level key in
  let comps = List.filter (fun (soort, _, _) -> soort <> Initialisatie) rules in
  let inits = List.filter (fun (soort, _, _) -> soort = Initialisatie) rules in
  if rules = [] then None
  else if List.exists (fun (_, (r : resolved_regel), _) -> not (version_free r.rr_geldigheid)) rules
  then
    (* NOT lowered, and deliberately: a base carrying a validity window would
       put a third time dimension into the stack semantics — version selection
       is temporal and defeasibility is not, and ch. 7.4 keeps them apart on
       purpose. The proposal gives no reading for the two together, so this is
       the fall-back rule: refuse by name rather than pick one. *)
    raise
      (Unlowered
         (Printf.sprintf
            "target %s on '%s' is written by a rule with a VALIDITY WINDOW beside an inherited \
             defaults stack: a windowed base would put version selection inside the stack, which \
             ch. 7.4 keeps apart from defeasibility, and the block order has no reading for it \
             (RUNTIME-SEMANTICS ch. 7.10)"
            key level))
  else if List.exists (fun (_, (r : resolved_regel), _) -> r.waarde = None) rules then
    raise
      (Unlowered
         (Printf.sprintf
            "target %s on '%s' is written by a rule without a value beside an inherited defaults \
             stack: a base with no consequence decides nothing and cannot be folded \
             (RUNTIME-SEMANTICS ch. 7.10)"
            key level))
  else if List.length comps > 1 || List.length inits > 1 then
    raise
      (Unlowered
         (Printf.sprintf
            "target %s on '%s' is written by %d computing and %d initialisation rule(s) beside an              inherited defaults stack: the v0 meaning of two writers on one target is agenda              order, which prioritized defaults exist to replace (RUNTIME-SEMANTICS ch. 7.10)"
            key level (List.length comps) (List.length inits)))
  else
    let attribuut =
      match key with k when String.length k > 2 -> String.sub k 2 (String.length k - 2) | k -> k
    in
    let a =
      match rules with
      | (_, (r : resolved_regel), _) :: _ -> (
          match r.doel with
          | Some (Attribuut_ref { attribuut = Some a; _ }) -> a
          | _ -> attribuut)
      | [] -> attribuut
    in
    let d ~init (r : resolved_regel) =
      {
        default_besluit = None;
        default_voorwaarde =
          (if init then None else Some (Option.value r.rr_voorwaarde ~default:always_true));
        default_consequentie = Some (Option.get r.waarde);
        default_initialisatie = init;
        default_excepties = None;
        default_bron = r.rr_naam;
      }
    in
    let defaults =
      List.map (fun (_, r, _) -> d ~init:false r) comps
      @ List.map (fun (_, r, _) -> d ~init:true r) inits
    in
    let idx = List.fold_left (fun acc (_, _, i) -> max acc i) 0 rules in
    Some
      ( {
          pd_naam = Some ("regel als basis: " ^ a ^ " van " ^ level);
          pd_onderwerp = level;
          pd_kenmerk = None;
          pd_attribuut = Some a;
          defaults;
          pd_ongeordend = false;
          pd_bron = Some "SUBTYPING §4.1 rules 1/4: an ordinary rule is the base of its level";
          pd_geldigheid = None;
        },
        idx )

(* Every declared group with the index it sits at in the document, so a
   consumer can still stratify a composed stack by its contributors.
   [absorb] is the key set on which an ordinary rule counts as a base — a
   property of the stack's OWNER, threaded down so the answer does not
   change with the recursion depth. *)
let rec effective_stacks_of doc ~visited ~absorb type_name :
    (string * (prioritized_defaults * int) list) list =
  let own_groups =
    List.concat
      (List.mapi
         (fun i (l : prioritized_defaults) ->
           if norm l.pd_onderwerp = norm type_name then [ (l, i) ] else [])
         doc.prioritized_defaults)
  in
  (* a level that declares a group on a key keeps it; on an ABSORBED key it
     declares none, its ordinary rule is the level's base (a level carrying
     both is WF-050's single-writer lift and is refused at the document) *)
  let own =
    own_groups
    @ List.filter_map
        (fun k ->
          if List.exists (fun (l, _) -> Checks.target_key l = k) own_groups then None
          else rule_member doc type_name k)
        absorb
  in
  let inherited =
    match Checks.find_ci doc.objecttypen type_name with
    | Some ot -> (
        match ot.ot_supertype with
        | Some s when not (List.mem (norm s) visited) ->
            effective_stacks_of doc ~visited:(norm type_name :: visited) ~absorb s
        | _ -> [])
    | None -> []
  in
  let own_by_key =
    List.fold_left
      (fun acc (l, i) ->
        let k = Checks.target_key l in
        if List.mem_assoc k acc then
          List.map (fun (k', ms) -> if k' = k then (k', ms @ [ (l, i) ]) else (k', ms)) acc
        else acc @ [ (k, [ (l, i) ]) ])
      [] own
  in
  let keys =
    List.map fst own_by_key
    @ List.filter (fun k -> not (List.mem_assoc k own_by_key)) (List.map fst inherited)
  in
  List.map
    (fun k ->
      match (List.assoc_opt k own_by_key, List.assoc_opt k inherited) with
      | Some ms, None -> (k, ms)
      | None, Some ms -> (k, ms)
      | Some [ (o, oi) ], Some [ (p, pi) ] -> (k, [ (compose_member ~own:o ~inherited:p, max oi pi) ])
      | Some [ _ ], Some ps ->
          (* ONE own group over an INHERITED DECLARED-UNORDERED SET. This is
             defined — SUBTYPING §4.1 rule 3: the inherited group stays
             internally unordered and moves as one block, above the subtype's
             base and below its own exceptions. It is not lowered here, and
             the reason is a property of the wire rather than a gap in the
             design: that reading is a THREE-LAYER priority (own exceptions >
             the unordered inherited block > the own base), and a defaults
             group's members reconcile as EQUALS with no order between them
             (ch. 7.2). The own base's justification is unconditional, so
             whichever member carried it would decide whenever an inherited
             member decides — "an inherited exception outranks the subtype's
             base" (§4.4) and "the base decides when nothing else does" become
             indistinguishable. The one mechanism that does express "reconcile
             a set, else fall back" is ch. 7.9's group-default, and wrapping
             the block in one would move the conflict key from the ROOT form
             §4.4 pins to the path-keyed form. So it needs a layered stack the
             wire does not have. *)
          raise
            (Unlowered
               (Printf.sprintf
                  "target %s on '%s' composes one own group over an inherited declared-unordered \
                   set of %d: SUBTYPING §4.1 rule 3 defines it (the inherited block moves whole, \
                   above the base and below the own exceptions) but it is not lowered in this \
                   release — the reading is a three-layer priority and a group's members \
                   reconcile as equals (RUNTIME-SEMANTICS ch. 7.10)"
                  k type_name (List.length ps)))
      | Some ms, Some ps ->
          (* A declared-unordered SET on the OWN level, over anything
             inherited. Here the design itself is silent: §4.1 rule 3 speaks
             of an unordered PARENT group, and no rule places a subtype's own
             unordered set against an inherited stack. Guessing one silently
             is the one thing this must not do. *)
          raise
            (Unlowered
               (Printf.sprintf
                  "target %s on '%s' is carried by %d declared group(s) here and %d inherited \
                   one(s): a fold between a declared-unordered set of its own and an inherited \
                   stack has no defined reading (RUNTIME-SEMANTICS ch. 7.10)"
                  k type_name (List.length ms) (List.length ps)))
      | None, None -> (k, []))
    keys

(* The effective stacks of every declared object type: (owner type, target
   key, members, contributing declaration indices). The owner is the CONCRETE
   type the stack belongs to, and its instances are exactly the instances the
   stack runs on. *)
let effective_defaults (doc : document) :
    (string * string * prioritized_defaults list * int list) list =
  List.concat_map
    (fun (name, _) ->
      let absorb = Checks.chain_group_keys doc name in
      List.filter_map
        (fun (k, ms) ->
          if ms = [] then None
          else Some (name, k, List.map fst ms, List.map snd ms))
        (effective_stacks_of doc ~visited:[] ~absorb name))
    doc.objecttypen

(* The concrete types on which an ordinary rule writing ([otype], [attr]) has
   been ABSORBED into an effective defaults stack as its base, and therefore
   MUST NOT also fire as a rule — a rule that both is a base and writes on its
   own would be two writers on one target. Every executor asks this of the
   same function, so no two of them can absorb on different instances.

   The answer is empty on every target whose chain declares no defaults group,
   which is every ordinary rule in a flat model: there the ch. 7.8 standing
   norm governs unchanged. *)
(* A one-entry memo keyed on the document's PHYSICAL identity. [absorbed_on]
   is asked once per rule per fixpoint pass and its answer is a property of
   the document alone, so recomputing objecttypes x groups on every pass is
   quadratic work where the pre-inheritance code did none. A miss simply
   recomputes — the cache can never answer for the wrong document. *)
let absorbed_memo : (document * (string * string, string list) Hashtbl.t) option ref = ref None

let absorbed_uncached (doc : document) ~otype ~attr : string list =
  let key = "A:" ^ norm attr in
  (* the rule's OWN level declaring a group on the target is not inheritance
     but WF-050's single-writer lift, refused at the document. The synthesis
     skips such a level, so the suppression must skip it too — otherwise the
     rule would be silenced without ever becoming a base, and the writer
     would vanish. Measured: this is the shape the standing-norm witness
     `StaandeScalaireNormWitnessTest` pins, and it caught the omission. *)
  if
    List.exists
      (fun (l : prioritized_defaults) ->
        norm l.pd_onderwerp = norm otype && Checks.target_key l = key)
      doc.prioritized_defaults
  then []
  else
    List.filter_map
      (fun (t, _) ->
        if
          Checks.subtype_of doc ~sub:t ~super:otype
          && List.mem key (Checks.chain_group_keys doc t)
        then
          Some t
        else None)
      doc.objecttypen

let absorbed_on (doc : document) ~otype ~attr : string list =
  let tbl =
    match !absorbed_memo with
    | Some (d, t) when d == doc -> t
    | _ ->
        let t = Hashtbl.create 16 in
        absorbed_memo := Some (doc, t);
        t
  in
  let k = (norm otype, norm attr) in
  match Hashtbl.find_opt tbl k with
  | Some v -> v
  | None ->
      let v = absorbed_uncached doc ~otype ~attr in
      Hashtbl.add tbl k v;
      v


(* The temporal standing domain (RS ch. 7.8, the timeline arm): a
   temporal target carried by exactly one version-free computing rule
   (in practice the ch. 3.7 pro-rata fold) as its only writer. The rule
   decides only when its result timeline is filled (>= 1 segment) — an
   empty result is the temporal leeg (empty == never-written, ch. 3.1) and
   writes nothing, so a case-supplied input timeline stands whole: the
   defaults reading is per target, never per bucket (a default decides once;
   ref's write_timeline replaces whole). *)
let standing_temporal_targets = standing_targets_gen ~temporal:true
