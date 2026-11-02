(* The scale of the unbounded form — the clingo backend's pre-step for an
   attribute or parameter declared as a number without decimals (IR-SPEC
   ch. 2.4 (c), `whole: false`, `decimals: 0`; spec/ASP-TRANSLATION.md S2).

   THE WIRE IS UNTOUCHED. Like [Normalise], this is a document → document
   transform on the resolved model, run inside [Translate.translate] and
   [Model.check] alike, so the program and the reading of its model see one
   declaration. ref and the Drools backend carry the exact value of the form;
   only the clingo leg needs a scale.

   WHY. This encoding holds a number as an integer at the scale 10^d its
   declaration fixes, and the unbounded form fixes none: at scale 1 a value of
   the form that is not whole cannot be held. The precision
   bound of RUNTIME-SEMANTICS ch. 2.3 says how many decimals a value can
   carry, read from the declarations and the shape of the expression: where
   every write of the attribute and every value the case gives it has a finite
   bound, the widest of them is a scale at which every value the attribute
   takes is a whole number of units, so declaring the attribute at that scale
   changes no value and lets the encoding hold all of them.

   THE BOUND. Per place (an attribute on the type that declares it, or a
   parameter): the widest of the decimals of its case values and the bound of
   each value written into it — a rule version, an object creation's member,
   a defaults consequence of any depth — restated into its unit, as the
   verifier's precision gate reads it ([Ir_v0.Checks.precision], one
   definition of the bound in the house). A write that makes no claim, and the
   share of an allocation without a rounding (the division by the recipients
   is not typed, ch. 2.4 point 4), have no finite bound. A write that reads an
   unbounded place reads it at the bound found so far, so the bounds are the
   least fixpoint from the case values upward; a bound past 9 decimals is
   past the scales this encoding carries ([Translate.pow10]) and is no finite
   bound here.

   THE OUTCOME. A place with a finite bound d > 0 is declared with d decimals;
   one with bound 0 keeps its declaration, at scale 1 (every value it takes is
   whole); one without a finite bound keeps it too, and stays the named limit
   of S2 and S19 (a value that is not whole is refused in that form's words).
   A struct field, a constructor payload and a list element of the form keep
   their declaration: they are no place here. *)

open Ir_v0.Ast
module Checks = Ir_v0.Checks
module Case = Uchar_ref.Case
module Value = Uchar_ref.Value
module World = Uchar_ref.World

let norm = Checks.norm

(* the largest decimal count [Translate.pow10] scales by *)
let max_decimals = 9

let unbounded_datatype = function
  | Some (Number { whole_part = false; decimal_places = 0; _ } | Percentage { whole_part = false; decimal_places = 0; _ })
    ->
      true
  | _ -> false

type place = Attribute_place of string * string (* declaring type, attribute; both normalised *) | Parameter_place of string

(* the type on [from]'s supertype chain whose declaration lists [member], as
   the document's own key names it (normalised) *)
let declaring (doc : document) ~from ~member =
  let rec go seen name =
    match Checks.find_ci doc.object_types name with
    | None -> None
    | Some (ot : object_type) -> (
        if List.exists (fun (a, _) -> norm a = norm member) ot.attributes then Some (norm name)
        else
          match ot.ot_supertype with
          | Some s when not (List.mem (norm s) seen) -> go (norm s :: seen) s
          | _ -> None)
  in
  go [ norm from ] from

let attribute_place doc ~from ~member =
  Option.map (fun t -> Attribute_place (t, norm member)) (declaring doc ~from ~member)

(* every place of the unbounded form the document declares *)
let unbounded_places (doc : document) : place list =
  List.concat_map
    (fun (tn, (ot : object_type)) ->
      List.filter_map
        (fun (a, dt) -> if unbounded_datatype dt then Some (Attribute_place (norm tn, norm a)) else None)
        ot.attributes)
    doc.object_types
  @ List.filter_map
      (fun (n, (p : parameter)) -> if unbounded_datatype p.par_datatype then Some (Parameter_place (norm n)) else None)
      doc.parameters

(* [dt] declared at the bound [b]. While the bounds are searched, a bound 0 is
   declared whole, so that a read of the place is bounded by 0 and not by the
   unbounded form; the outcome keeps the declaration there ([~search:false]). *)
let at_bound ~search (b : Checks.precision) (dt : datatype) : datatype =
  match (b, dt) with
  | Checks.Finite 0, Number n -> if search then Number { n with whole_part = true } else dt
  | Checks.Finite 0, Percentage n -> if search then Percentage { n with whole_part = true } else dt
  | Checks.Finite d, Number n -> Number { n with decimal_places = d }
  | Checks.Finite d, Percentage n -> Percentage { n with decimal_places = d }
  | _ -> dt

let declared_at ~search (doc : document) (bounds : (place * Checks.precision) list) : document =
  let bound_of p = List.assoc p bounds in
  {
    doc with
    object_types =
      List.map
        (fun (tn, (ot : object_type)) ->
          ( tn,
            {
              ot with
              attributes =
                List.map
                  (fun (a, dt) ->
                    let p = Attribute_place (norm tn, norm a) in
                    match dt with
                    | Some d when List.mem_assoc p bounds -> (a, Some (at_bound ~search (bound_of p) d))
                    | _ -> (a, dt))
                  ot.attributes;
            } ))
        doc.object_types;
    parameters =
      List.map
        (fun (n, (par : parameter)) ->
          let p = Parameter_place (norm n) in
          match par.par_datatype with
          | Some d when List.mem_assoc p bounds -> (n, { par with par_datatype = Some (at_bound ~search (bound_of p) d) })
          | _ -> (n, par))
        doc.parameters;
  }

let widest = Checks.widest

(* the decimals of one case value: of a number, its exact decimals; of a
   timeline, the widest of its segments *)
let value_bound (v : Value.t) : Checks.precision =
  match v with Num (q, _) -> Checks.rational_precision q | _ -> Checks.Finite 0

let timeline_bound (tl : Uchar_ref.Timeline.t) : Checks.precision =
  List.fold_left (fun acc (s : Uchar_ref.Timeline.segment) -> widest acc (value_bound s.s_value)) (Checks.Finite 0) tl

(* the place a case key of an instance of [otype] names: an attribute, or the
   dimensioned attribute a cell belongs to *)
let input_place doc ~otype key =
  match attribute_place doc ~from:otype ~member:key with
  | Some p -> Some p
  | None -> (
      match Checks.effective_ot doc otype with
      | Some ot -> (
          match List.filter (fun (a, _) -> World.cell_of a key) ot.attribute_dimensions with
          | [ (a, _) ] -> attribute_place doc ~from:otype ~member:a
          | _ -> None)
      | None -> None)

(* the widest decimals the case gives each place *)
let input_bounds doc (case : Case.t) : (place * Checks.precision) list =
  let acc = ref [] in
  let add p b =
    acc := (p, widest b (Option.value (List.assoc_opt p !acc) ~default:(Checks.Finite 0))) :: List.remove_assoc p !acc
  in
  World.SMap.iter
    (fun _ (i : World.instance) ->
      World.SMap.iter (fun k v -> Option.iter (fun p -> add p (value_bound v)) (input_place doc ~otype:i.otype k)) i.attrs;
      World.SMap.iter (fun k tl -> Option.iter (fun p -> add p (timeline_bound tl)) (input_place doc ~otype:i.otype k)) i.tls)
    case.world.instances;
  World.SMap.iter (fun k v -> add (Parameter_place k) (value_bound v)) case.params;
  World.SMap.iter (fun k tl -> add (Parameter_place k) (timeline_bound tl)) case.param_tls;
  !acc

(* the bound of a value written into a target declared [dt]: in the target's
   unit, and none where the value makes no claim *)
let written_bound ctx (dt : datatype option) (e : expr) : Checks.precision =
  match Checks.precision ctx ~env:[] e with
  | None -> Checks.Unbounded
  | Some x -> (
      match Checks.in_unit x (Option.bind dt Checks.unit_of) with Some b -> b | None -> Checks.Unbounded)

(* every write of the document, as its place and the bound of its value under a
   context whose declarations carry the bounds found so far *)
let writes (doc : document) : (place * (Checks.ctx -> Checks.precision)) list =
  let target_dt ctx place =
    match place with
    | Attribute_place (t, a) -> Option.bind (Checks.find_ci ctx.Checks.doc.object_types t) (fun ot -> Option.join (Checks.find_ci ot.attributes a))
    | Parameter_place n -> Option.bind (Checks.find_ci ctx.Checks.doc.parameters n) (fun p -> p.par_datatype)
  in
  let value place e = (place, fun ctx -> written_bound ctx (target_dt ctx place) e) in
  let rules =
    List.filter_map
      (fun (r : resolved_rule) ->
        match (r.rr_presentation, r.objective, r.value_node) with
        | Some (Equation | Calculation | Initialisation), Some objective, Some w -> (
            let read = match objective with Dimension_cell_read { attribute_ref = Some inner; _ } -> inner | o -> o in
            match read with
            | Attribute_reference { object_type = Some ot; attribute_ref = Some a; _ } ->
                Option.map (fun p -> value p w) (attribute_place doc ~from:ot ~member:a)
            | _ -> None)
        | _ -> None)
      doc.resolved_rules
  in
  let creations =
    List.concat_map
      (fun (o : resolved_object_creation) ->
        match o.created_type with
        | None -> []
        | Some ct ->
            List.filter_map
              (fun (t : assignment) ->
                match (t.to_characteristic, t.to_attribute, t.to_value) with
                | false, Some m, Some w -> Option.map (fun p -> value p w) (attribute_place doc ~from:ct ~member:m)
                | _ -> None)
              o.assignments)
      doc.object_creations
  in
  let defaults =
    List.concat_map
      (fun (l : prioritized_defaults) ->
        match l.pd_attribute with
        | None -> []
        | Some a -> (
            match attribute_place doc ~from:l.pd_subject ~member:a with
            | None -> []
            | Some p ->
                let rec walk (d : default) =
                  Option.to_list (Option.map (value p) d.default_consequence)
                  @
                  match d.default_exceptions with
                  | Some egs -> List.concat_map (fun (eg : exception_group) -> List.concat_map walk eg.eg_defaults) egs.egs_members
                  | None -> []
                in
                List.concat_map walk l.defaults))
      doc.prioritized_defaults
  in
  let allocations =
    List.concat_map
      (fun (r : resolved_allocation) ->
        match r.objective_type_name with
        | None -> []
        | Some tt ->
            let share = match r.rounding_decimals with Some n -> Checks.Finite n | None -> Checks.Unbounded in
            (* the remainder is the pot less the rounded shares *)
            let remainder ctx =
              match (r.rounding_decimals, r.source_type, r.source_attribute) with
              | Some n, Some st, Some sa -> (
                  match Checks.effective_ot ctx.Checks.doc st with
                  | Some ot -> (
                      match Option.bind (Option.join (Checks.find_ci ot.attributes sa)) Checks.declared_precision with
                      | Some b -> widest (Checks.Finite n) b
                      | None -> Checks.Unbounded)
                  | None -> Checks.Unbounded)
              | _ -> Checks.Unbounded
            in
            List.filter_map
              (fun (attr, f) -> Option.bind attr (fun a -> Option.map (fun p -> (p, f)) (attribute_place doc ~from:tt ~member:a)))
              [ (r.target_attribute, fun _ -> share); (r.remainder_attribute, remainder) ])
      doc.allocations
  in
  rules @ creations @ defaults @ allocations

let capped = function Checks.Finite d when d > max_decimals -> Checks.Unbounded | b -> b

let document (doc : document) (case : Case.t) : document =
  match unbounded_places doc with
  | [] -> doc
  | places ->
      let inputs = input_bounds doc case in
      let writes = List.filter (fun (p, _) -> List.mem p places) (writes doc) in
      let start =
        List.map (fun p -> (p, capped (Option.value (List.assoc_opt p inputs) ~default:(Checks.Finite 0)))) places
      in
      (* each round widens a bound or ends: a bound only grows, and it grows past
         [max_decimals] to no finite bound, so the rounds are finitely many *)
      let rec fix bounds =
        let ctx = Checks.ctx_of (declared_at ~search:true doc bounds) in
        let next =
          List.map
            (fun (p, b) ->
              (p, capped (List.fold_left (fun acc (q, f) -> if q = p then widest acc (f ctx) else acc) b writes)))
            bounds
        in
        if next = bounds then bounds else fix next
      in
      declared_at ~search:false doc (fix start)
