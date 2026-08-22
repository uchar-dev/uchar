(* The v0 desugaring of RUNTIME-SEMANTICS ch. 7.7: on one
   (type, attribute) target, one version-free computing rule
   (Gelijkstelling/Berekening) plus at most one unconditional,
   version-free Initialisatie read as one attribute ladder — the
   computing rule's condition becomes the rung justification (an
   unconditional rule gets the vacuously true empty-ALLE), the
   initialisation becomes the ch. 7.3 init rung. Forms outside the
   domain are left untouched; a caller that needs totality checks
   [remainder] and faults, never guesses.

   The lowering is document-to-document so a backend can consume the
   ladder machinery while the reference interpreter keeps replaying
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
    (* exactly one initialisation: the init rung is what the desugaring
       exists for — the bare computation stays structurally outside the
       desugaring and carries the ladder reading directly through the
       ch. 7.8 standing norm (standing_targets below) *)
    List.length rs = List.length inits + List.length comps
    && List.length inits = 1
    && List.for_all is_plain_init inits
    && List.length comps <= 1
  in
  List.filter desugarable targets

(* The ch. 7.7 desugaring domain as target keys — shared with ref/,
   whose native replay gives these targets the same fall-through the
   desugared ladder carries (RS ch. 7.7/7.8: a computing rule that
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
    let ladder_of (_, ((ot, a), rs)) : default_ladder =
      let comp_rungs =
        List.filter_map
          (fun r ->
            if is_computing r then
              Some
                {
                  rung_besluit = None;
                  rung_voorwaarde =
                    Some (Option.value r.rr_voorwaarde ~default:always_true);
                  rung_consequentie = Some (Option.get r.waarde);
                  rung_initialisatie = false;
                  rung_excepties = None;
                  rung_bron = r.rr_naam;
                }
            else None)
          rs
      in
      let init_rungs =
        List.filter_map
          (fun r ->
            if r.rr_soort = Some Initialisatie then
              Some
                {
                  rung_besluit = None;
                  rung_voorwaarde = None;
                  rung_consequentie = Some (Option.get r.waarde);
                  rung_initialisatie = true;
                  rung_excepties = None;
                  rung_bron = r.rr_naam;
                }
            else None)
          rs
      in
      {
        dl_naam = Some ("desugared: " ^ a ^ " van " ^ ot);
        dl_onderwerp = ot;
        dl_kenmerk = None;
        dl_attribuut = Some a;
        rungs = comp_rungs @ init_rungs;
        dl_ongeordend = false;
        dl_bron = Some "ch. 7.7 v0 desugaring";
        dl_geldigheid = None;
      }
    in
    {
      doc with
      resolved_regels = List.filter (fun r -> not (in_chosen r)) doc.resolved_regels;
      default_ladders = doc.default_ladders @ List.map ladder_of chosen;
    }

(* The standing domain of RUNTIME-SEMANTICS ch. 7.8: the bare scalar
   computing target — one version-free Gelijkstelling/Berekening as its
   target's only writer (no initialisation beside it: that pair is the
   ch. 7.7 desugaring's domain above; no ladder, allocation, or
   creation-time write). On these targets the rule decides like a rung —
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
      (fun (l : default_ladder) ->
        norm l.dl_onderwerp = norm ot
        && match l.dl_attribuut with Some la -> norm la = norm a | None -> false)
      doc.default_ladders
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

(* The temporal standing domain (RS ch. 7.8, the timeline arm): a
   temporal target carried by exactly one version-free computing rule
   (in practice the ch. 3.7 pro-rata fold) as its only writer. The rule
   decides only when its result timeline is GEVULD (>= 1 segment) — an
   empty result is the temporal leeg (empty == never-written, ch. 3.1) and
   writes nothing, so a case-supplied input timeline stands whole: the
   ladder reading is per target, never per bucket (a rung decides once;
   ref's write_timeline replaces whole). *)
let standing_temporal_targets = standing_targets_gen ~temporal:true
