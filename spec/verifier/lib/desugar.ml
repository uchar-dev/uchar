(* The v0 desugaring of RUNTIME-SEMANTICS ch. 7.7: on one
   (type, attribute) target, one version-free computing rule
   (Gelijkstelling/Berekening) plus at most one unconditional,
   version-free Initialisatie read as one attribute defaults group — the
   computing rule's condition becomes the default justification (an
   unconditional rule gets the vacuously true empty-ALLE), the
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

(* the vacuously true justification: ALLE over zero parts *)
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

let target_key (l : prioritized_defaults) =
  (if l.pd_kenmerk <> None then "K:" else "A:")
  ^
  match (l.pd_kenmerk, l.pd_attribuut) with
  | Some k, _ -> norm k
  | _, Some a -> norm a
  | _ -> "?"

exception Unlowered of string

(* Every declared group with the index it sits at in the document, so a
   consumer can still stratify a composed stack by its contributors. *)
let rec effective_stacks_of doc ~visited type_name :
    (string * (prioritized_defaults * int) list) list =
  let own =
    List.concat
      (List.mapi
         (fun i (l : prioritized_defaults) ->
           if norm l.pd_onderwerp = norm type_name then [ (l, i) ] else [])
         doc.prioritized_defaults)
  in
  let inherited =
    match Checks.find_ci doc.objecttypen type_name with
    | Some ot -> (
        match ot.ot_supertype with
        | Some s when not (List.mem (norm s) visited) ->
            effective_stacks_of doc ~visited:(norm type_name :: visited) s
        | _ -> [])
    | None -> []
  in
  let own_by_key =
    List.fold_left
      (fun acc (l, i) ->
        let k = target_key l in
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
      | Some ms, Some ps ->
          (* the fold is defined member-to-member; a declared-unordered SET
             on either level would need a rule the design does not give, and
             guessing one silently is the one thing this must not do *)
          raise
            (Unlowered
               (Printf.sprintf
                  "target %s on '%s' is carried by %d declared group(s) here and %d inherited \
                   one(s): the block order composes one stack over one stack, and a \
                   declared-unordered set across a supertype boundary has no defined fold \
                   (RUNTIME-SEMANTICS ch. 7.10)"
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
      List.filter_map
        (fun (k, ms) ->
          if ms = [] then None
          else Some (name, k, List.map fst ms, List.map snd ms))
        (effective_stacks_of doc ~visited:[] name))
    doc.objecttypen

(* RUNTIME-SEMANTICS ch. 7.10, the corner this release does not lower: an
   ORDINARY rule on an ancestor's target that a descendant also covers with a
   defaults group. Under ch. 7.1 that rule would be the inherited base, but it
   is also a writer in its own right and fires extensionally on the
   descendant's instances, so document and execution would part company
   silently. Reported here, once, so all three executors refuse it with one
   voice instead of each diverging its own way. *)
let inherited_rule_clashes (doc : document) : (string * string * string) list =
  let covered = List.map (fun (o, k, _, _) -> (norm o, k)) (effective_defaults doc) in
  List.concat_map
    (fun (r : resolved_regel) ->
      match (r.rr_soort, r.doel) with
      | ( Some (Gelijkstelling | Berekening | Initialisatie),
          Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }) ) ->
          List.filter_map
            (fun (owner, _) ->
              if
                norm owner <> norm ot
                && Checks.subtype_of doc ~sub:owner ~super:ot
                && List.mem (norm owner, "A:" ^ norm a) covered
              then Some (owner, ot, a)
              else None)
            doc.objecttypen
      | _ -> [])
    doc.resolved_regels
  |> List.sort_uniq compare

(* The temporal standing domain (RS ch. 7.8, the timeline arm): a
   temporal target carried by exactly one version-free computing rule
   (in practice the ch. 3.7 pro-rata fold) as its only writer. The rule
   decides only when its result timeline is GEVULD (>= 1 segment) — an
   empty result is the temporal leeg (empty == never-written, ch. 3.1) and
   writes nothing, so a case-supplied input timeline stands whole: the
   defaults reading is per target, never per bucket (a default decides once;
   ref's write_timeline replaces whole). *)
let standing_temporal_targets = standing_targets_gen ~temporal:true
