(* The end state of a session (RUNTIME-SEMANTICS ch. 5.12a): what the fixpoint
   world holds, one line per fact, in the canonical value text and with the
   object names of the readings (ch. 5.12). `runner --trace` prints it beside
   the readings, and gate 17 holds it to the Drools backend's, which shares no
   code with this one. Every line is read off the world as it stands; nothing
   is evaluated again. *)

open Ir_v0.Ast
module SMap = World.SMap

let norm = Ir_v0.Checks.norm
let quote = Reading.quote

(* a declared name of the document; the wire requires it where it stands *)
let named what = function
  | Some n -> n
  | None -> failwith (Printf.sprintf "ref/lib/end_state.ml: a %s without a name" what)

(* A number in the unit of its member's declaration, restated where the world
   holds it in another, and that unit after the text, as the readings write the
   value of a node (ch. 5.12) *)
let in_unit declared (v : Value.t) =
  match v with
  | Value.Num (q, u) -> (
      match (declared, u) with
      | Value.Unitless, _ -> Value.Num (q, u)
      | du, Value.Unitless -> Value.Num (q, du)
      | du, u when du = u -> Value.Num (q, u)
      | du, u -> Value.Num (Arith.restate_into du q u, du))
  | v -> v

let with_unit declared text =
  match Reading.unit_text declared with Some u -> text ^ " " ^ quote u | None -> text

(* the cells of a dimensioned attribute: every combination of the labels of its
   dimensions, in their declared order *)
let cells (doc : document) dims =
  let labels dim =
    match List.assoc_opt dim doc.dimensions with
    | Some d -> List.map (named "dimension label") d.dim_values
    | None -> failwith (Printf.sprintf "ref/lib/end_state.ml: the dimension %S is not declared" dim)
  in
  List.fold_right
    (fun dim acc -> List.concat_map (fun l -> List.map (fun rest -> l :: rest) acc) (labels dim))
    dims [ [] ]

(* the true periods of a time-dependent characteristic as a timeline of periods
   without a value, adjacent periods joined *)
let periods_text ps =
  let ps = List.sort compare ps in
  let joined =
    List.fold_left
      (fun acc (f, t) ->
        match acc with
        | (f0, t0) :: rest when t0 = f -> (f0, t) :: rest
        | _ -> (f, t) :: acc)
      [] ps
  in
  "["
  ^ String.concat ", "
      (List.rev_map (fun (f, t) -> "[" ^ Reading.day_text f ^ " " ^ Reading.day_text t ^ " empty]") joined)
  ^ "]"

(* the members of one object *)
let object_lines (env : Eval.env) name (i : World.instance) =
  let doc = env.Eval.doc in
  let ot =
    match Ir_v0.Checks.effective_ot doc i.otype with
    | Some ot -> ot
    | None -> failwith (Printf.sprintf "ref/lib/end_state.ml: the object type %S is not declared" i.otype)
  in
  let self = name i.label in
  (* the type by its declared name: a case may spell it otherwise *)
  let head = "object " ^ self ^ " " ^ quote (named "object type" ot.ot_ident) in
  let attribute (a, decl) =
    let unit = Eval.unit_of_datatype decl in
    let line suffix value = Printf.sprintf "attribute %s %s%s %s" self (quote a) suffix value in
    let scalar suffix member =
      match World.attr i member with
      | Value.Empty -> []
      | v -> [ line suffix (with_unit unit (Reading.canonical (in_unit unit v))) ]
    in
    match List.assoc_opt a ot.attribute_dimensions with
    | Some dims when dims <> [] ->
        let dims = List.map (named "dimension") dims in
        List.concat_map
          (fun labels ->
            scalar (" [" ^ String.concat ", " (List.map quote labels) ^ "]") (World.cell a labels))
          (cells doc dims)
    | _ -> (
        let timeline tl =
          let tl =
            Timeline.canonical (List.map (fun (s : Timeline.segment) -> { s with s_value = in_unit unit s.s_value }) tl)
          in
          match tl with [] -> [] | tl -> [ line "" (with_unit unit (Reading.timeline_text tl)) ]
        in
        match (World.timeline i a, env.as_of) with
        | Some tl, _ -> timeline tl
        | None, Some d when Ir_v0.Checks.attr_temporal ot a && World.attr i a <> Value.Empty -> (
            (* the asOfDate-bucket scalar of a time-dependent attribute is the
               timeline of its one period, the period of the attribute's own
               grid that holds the evaluation date (ch. 5.12, 8.4); where the
               grid lays no bucket it stands as the case gave it (ch. 5.12a) *)
            let day = match Value.date_of_iso d with Value.Date z -> z | _ -> assert false in
            match Timeline.grid_bucket_opt (Timeline.attr_grid doc ot a) day with
            | Some (from, up_to) -> timeline [ { Timeline.s_from = from; s_to = up_to; s_value = World.attr i a } ]
            | None -> scalar "" a)
        (* without an evaluation date the scalar has no period, and stands as
           the case gave it (ch. 5.12a) *)
        | None, _ -> scalar "" a)
  in
  let characteristic c =
    let c = named "characteristic" c in
    if List.mem_assoc c ot.characteristic_granularity then
      match SMap.find_opt (norm c) i.ctls with
      | None | Some [] -> []
      | Some ps -> [ Printf.sprintf "characteristic %s %s %s" self (quote c) (periods_text ps) ]
    else if World.has_char i c then [ Printf.sprintf "characteristic %s %s" self (quote c) ]
    else []
  in
  (head :: List.concat_map attribute ot.attributes) @ List.concat_map characteristic ot.characteristics

(* every link, once, its objects in the order of the roles the fact type
   declares; a reciprocal fact type's two objects in the order of their names *)
let link_lines (doc : document) name (world : World.t) =
  List.concat_map
    (fun (ftn, (ft : fact_type_name)) ->
      let role_name (r : role_ref) =
        match (r.role_identifier, r.role_plural) with
        | Some n, _ -> n
        | None, Some m -> m
        | None, None -> failwith (Printf.sprintf "ref/lib/end_state.ml: a role of %S without a name" ftn)
      in
      let line r0 a r1 b =
        Printf.sprintf "link %s %s %s %s %s" (quote ftn) (quote (role_name r0)) a (quote (role_name r1)) b
      in
      (* the objects that fill [far] opposite the object [i] fills its sister in *)
      let over (far : role_ref) (i : World.instance) = World.links i ~fact_type:ftn far in
      SMap.fold
        (fun _ (i : World.instance) acc ->
          match ft.role_names with
          | [ r0; r1 ] ->
              (* i fills r0 and the targets r1, or i fills r1 and the targets r0 *)
              List.map (fun t -> line r0 (name i.label) r1 (name t)) (over r1 i)
              @ List.map (fun t -> line r0 (name t) r1 (name i.label)) (over r0 i)
              @ acc
          | [ r ] when ft.reciprocal ->
              List.map
                (fun t ->
                  let a = name i.label and b = name t in
                  if compare a b <= 0 then line r a r b else line r b r a)
                (over r i)
              @ acc
          | _ -> acc)
        world.instances [])
    doc.fact_types

(* the §9.5 facts: the rule, the subject and the EF codes of the kinds
   deposited for that pair, ascending, or - for none (ch. 5.6) *)
let channel_lines name (world : World.t) =
  List.map
    (fun (f : World.error_fact) ->
      Printf.sprintf "channel %s %s %s" (quote f.rule) (name f.subject)
        (match f.codes with [] -> "-" | cs -> String.concat "," cs))
    world.channel

let lines (env : Eval.env) ~(world : World.t) =
  let doc = env.Eval.doc in
  let name = Reading.name_of doc world in
  let objects =
    SMap.fold (fun _ (i : World.instance) acc -> object_lines env name i @ acc) world.instances []
  in
  List.sort_uniq compare (objects @ link_lines doc name world @ channel_lines name world)
