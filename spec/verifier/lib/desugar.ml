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

(* Desugar every target carried entirely by domain forms; leave every
   other rule untouched. Returns the rewritten document. *)
let document (doc : document) : document =
  let key (ot, a) = norm ot ^ ":" ^ norm a in
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
       exists for, and it is what keeps the two readings extensionally
       equal on the confluent domain — a bare computation without one
       reads differently on empty-over-input (ref writes the empty,
       ch. 1.2g; the ladder falls through) and must stay native *)
    List.length rs = List.length inits + List.length comps
    && List.length inits = 1
    && List.for_all is_plain_init inits
    && List.length comps <= 1
  in
  let chosen = List.filter desugarable targets in
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
