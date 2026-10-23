(* The reading of a condition at the fixpoint (RUNTIME-SEMANTICS ch. 5.12):
   for every form with a condition and every object the form runs on at the
   fixpoint, the evaluation of that condition on the fixpoint world — and of
   the form's value where the condition holds and the form computes one — in
   the order of ch. 5.10 and 5.11, recorded per node: whether the evaluation
   reached it, and the verdict, the value or the fault it found there. A form
   without a condition that computes a value has the reading of its value,
   which holds where the form is at its turn. A form
   outside its validity and a form not at its turn (a displaced default, an
   initialisation whose target holds a value of the input or of another
   writer, a 1:1 creation whose subject the input links) have no nodes. A
   reading deposits nothing and writes nothing: the evaluation is the
   session's own ([Eval]), watched through [Eval.observer], with every
   deposit dropped and every world it computes discarded.

   The nodes of a form are numbered in the pre-order of the document over the
   nodes of the condition union and the expression union: first the
   condition, its root 0, then the value; a node before its fields, the
   fields in the order the document writes them. A node evaluated per member
   of a fold over a set (its source, its filter, its matrix), per element of
   a list or per period of a timeline has no reading of its own and keeps its
   number; the node of a fold over a set carries its contributors instead.
   The numbering is checked against the document itself: the kinds of the
   nodes under the form's pointer, read off the JSON, must be the kinds of
   the walk, number for number, or the reading fails naming the form.

   [lines] gives the readings in the canonical text the Drools backend's
   readings are held to (scripts/check-readings.sh). *)

open Ir_v0.Ast
module SMap = World.SMap

let norm = Ir_v0.Checks.norm

(* --- the nodes of a form -------------------------------------------------- *)

(* A node of the wire, by the in-memory record that stands for it: its
   identity is physical, since two nodes of equal content are two nodes. A
   fold's inline function is a node of the expression union on the wire
   (Lambda) and a [fold_fn] here. *)
type site = S_expr of expr | S_cond of condition_expr | S_lambda of fold_fn

let same a b =
  match (a, b) with
  | S_expr x, S_expr y -> x == y
  | S_cond x, S_cond y -> x == y
  | S_lambda x, S_lambda y -> x == y
  | _ -> false

let cond_kind (c : condition_expr) =
  match c with
  | Comparison _ -> "Comparison"
  | Predicate _ -> "Predicate"
  | Characteristic_check _ -> "CharacteristicCheck"
  | Role_membership _ -> "RoleMembership"
  | Time_window _ -> "TimeWindow"
  | Compound _ -> "Compound"
  | Quantifier_fold _ -> "QuantifierFold"
  | Rule_status_check _ -> "Regelversie"
  | Identity_check _ -> "IdentityCheck"
  | Condition_unresolved _ -> "Unresolved"

let is_aggregation kind =
  List.exists (fun (_, k) -> k = kind) (Ir_v0.Spelling.pairs Ir_v0.Spelling.aggregation_kind)

(* a fold over a set: an aggregation with a range, or over an instance-set
   union, and the quantifier fold *)
let set_fold (e : expr) =
  match e with
  | Function_call { kind; source_role; source_path; call_arguments; _ } ->
      is_aggregation kind
      && (source_role <> None || source_path <> None
         || match call_arguments with [ Instance_set_union _ ] -> true | _ -> false)
  | _ -> false

(* The nodes under [e] / [c] in pre-order, each with its kind as the wire
   spells it, whether it has a reading of its own ([own]), and the binders of
   the enclosing Lets in [scope], outermost first, each with the value it
   binds: a binder read takes the type of that value (ch. 2.10). *)
let rec walk_expr ~own ?(scope = []) acc (e : expr) =
  let acc = (S_expr e, Ir_v0.Spelling.expr_node e, own, scope) :: acc in
  let sub ?(own = own) ?(scope = scope) acc x = walk_expr ~own ~scope acc x in
  let walk_cond ~own acc c = walk_cond ~own ~scope acc c and walk_fn acc f = walk_fn ~scope acc f in
  let opt f acc = function Some x -> f acc x | None -> acc in
  match e with
  | Literal _ | Attribute_reference _ | Parameter_ref _ | Variable_reference _ | Context_value _
  | Bound_day _ | Expr_unresolved _ | Binder_ref _ ->
      acc
  | Text_composition { pieces; _ } ->
      List.fold_left
        (fun acc -> function Embedded_expression { expr = Some x } -> sub acc x | _ -> acc)
        acc pieces
  | Binary { left_side; right_side; _ } -> opt sub (opt sub acc left_side) right_side
  | Function_call { call_arguments; filter; empty_fallback; _ } ->
      (* the source and the filter of a fold over a set are read per member;
         the filter of a temporal total or of a time-proportional part is
         read over a timeline, once *)
      let own_parts = own && not (set_fold e) in
      let acc = List.fold_left (sub ~own:own_parts) acc call_arguments in
      let acc = opt (walk_cond ~own:own_parts) acc filter in
      opt sub acc empty_fallback
  | Instance_set_union { branches; _ } ->
      List.fold_left
        (fun acc (b : instance_set_branch) -> opt (walk_cond ~own:false) acc b.isb_filter)
        acc branches
  | Rounding { base_expr; _ } -> opt sub acc base_expr
  | Clamp { base_expr; minimum; maximum; _ } ->
      opt sub (opt sub (opt sub acc base_expr) minimum) maximum
  | Dimension_cell_read { attribute_ref; _ } -> opt sub acc attribute_ref
  | Temporal_read { base_expr; date_value; _ } -> sub (sub acc base_expr) date_value
  | Construct { argument; _ } -> opt sub acc argument
  | Match { scrutinee; arms; _ } ->
      List.fold_left (fun acc (_, _, body) -> sub acc body) (sub acc scrutinee) arms
  | Struct_literal { field_list; _ } -> List.fold_left (fun acc (_, x) -> sub acc x) acc field_list
  | Field_access { base; _ } -> sub acc base
  | List_literal { elements; _ } -> List.fold_left sub acc elements
  | Lambda { body; _ } -> sub ~own:false acc body
  | If_then_else { condition; then_; else_; _ } -> sub (sub (walk_cond ~own acc condition) then_) else_
  | Invoke { arguments; _ } -> List.fold_left sub acc arguments
  | Fold { list; function_name; init; _ } -> sub (walk_fn (sub acc list) function_name) init
  | Map_list { list; function_name; _ } -> walk_fn (sub acc list) function_name
  | Filter_list { list; condition; _ } -> walk_cond ~own:false (sub acc list) condition
  | List_length { list; _ } -> sub acc list
  | Let { binder; value; body; _ } -> sub ~scope:(scope @ [ (binder, value) ]) (sub acc value) body

(* a fold's function is applied per element *)
and walk_fn ?(scope = []) acc (f : fold_fn) =
  match f with
  | Fn_lambda { body; _ } -> walk_expr ~own:false ~scope ((S_lambda f, "Lambda", false, scope) :: acc) body
  | Fn_name _ -> acc

and walk_cond ~own ?(scope = []) acc (c : condition_expr) =
  let acc = (S_cond c, cond_kind c, own, scope) :: acc in
  let sube acc x = walk_expr ~own ~scope acc x in
  let opt f acc = function Some x -> f acc x | None -> acc in
  match c with
  | Comparison { left_side; right_side; _ } -> opt sube (opt sube acc left_side) right_side
  | Predicate { operand; _ } -> opt sube acc operand
  | Characteristic_check _ | Role_membership _ | Rule_status_check _ | Identity_check _
  | Condition_unresolved _ ->
      acc
  | Time_window { starting_at; up_to; _ } -> opt sube (opt sube acc starting_at) up_to
  | Compound { components; _ } -> List.fold_left (walk_cond ~own ~scope) acc components
  | Quantifier_fold { kv_filter; kv_test; _ } ->
      opt (walk_cond ~own:false ~scope) (opt (walk_cond ~own:false ~scope) acc kv_filter) kv_test

(* the node kinds of the two unions, as the wire's discriminator spells them *)
let union_kinds =
  [ "Literal"; "TextComposition"; "Binary"; "Aggregation"; "BuiltinCall"; "Rounding"; "Clamp";
    "AttributeReference"; "DimensionCellRead"; "ParameterReference"; "VariableReference";
    "ContextValue"; "BoundDay"; "Unresolved"; "TemporalRead"; "Construct"; "Match"; "BinderRef";
    "StructLiteral"; "FieldAccess"; "ListLiteral"; "Lambda"; "IfThenElse"; "Invoke"; "Fold";
    "MapList"; "FilterList"; "ListLength"; "Let"; "InstanceSetUnion"; "Comparison"; "Predicate";
    "CharacteristicCheck"; "RoleMembership"; "TimeWindow"; "Compound"; "QuantifierFold";
    "Regelversie" ]

(* the value at a JSON pointer of the document; an absent member is None *)
let at (json : Yojson.Safe.t) pointer =
  let steps = match String.split_on_char '/' pointer with "" :: rest -> rest | rest -> rest in
  List.fold_left
    (fun j step ->
      match j with
      | Some (`Assoc kvs) -> List.assoc_opt step kvs
      | Some (`List l) -> (
          match int_of_string_opt step with Some i when i < List.length l -> Some (List.nth l i) | _ -> None)
      | _ -> None)
    (Some json) steps

(* the kinds of the union nodes under [j], in document pre-order *)
let rec json_kinds acc (j : Yojson.Safe.t) =
  match j with
  | `Assoc kvs ->
      let acc =
        match List.assoc_opt "node" kvs with
        | Some (`String k) when List.mem k union_kinds -> k :: acc
        | _ -> acc
      in
      List.fold_left (fun acc (_, v) -> json_kinds acc v) acc kvs
  | `List l -> List.fold_left json_kinds acc l
  | _ -> acc

(* The numbered nodes of one form: its condition at its document pointer,
   then its value at its own; a form without a condition numbers its value
   from 0. The walk's kinds must be the document's, number for number. *)
let number_nodes json ~form ~condition ~value =
  let walked =
    let conditioned = match condition with Some (c, _) -> walk_cond ~own:true [] c | None -> [] in
    List.rev (match value with Some (v, _) -> walk_expr ~own:true conditioned v | None -> conditioned)
  in
  let subtree = function
    | None -> []
    | Some (_, pointer) -> (
        match at json pointer with
        | Some j -> List.rev (json_kinds [] j)
        | None -> failwith (Printf.sprintf "ref/lib/reading.ml: the form %s has no node at %s" form pointer))
  in
  let written = subtree condition @ subtree value in
  let kinds = List.map (fun (_, k, _, _) -> k) walked in
  if kinds <> written then
    failwith
      (Printf.sprintf
         "ref/lib/reading.ml: the numbering of %s does not follow the document: the walk gives [%s], \
          the document [%s]"
         form (String.concat " " kinds) (String.concat " " written));
  List.mapi (fun i (site, kind, own, scope) -> (i, site, kind, own, scope)) walked

(* --- the recording -------------------------------------------------------- *)

type outcome =
  | Got of Value.t (* an expression's value *)
  | Held of bool (* a condition's verdict *)
  | Whole of Timeline.t option (* an operand read as a timeline, None where it is absent *)
  | Over of Timeline.t (* a condition read over a timeline: the part in which it holds *)
  | Faulted of string option (* the fault the node raised or passed on, with its EF code *)
  | Ended (* a read of a leeg binder ended the evaluation here (ch. 10.6, clause 2) *)

type node = {
  number : int;
  kind : string;
  site : site;
  own : bool;
  scope : (string * expr) list; (* the binders of the enclosing Lets, outermost first *)
  mutable outcome : outcome option; (* None: not reached *)
  mutable members : (string * Eval.contribution) list; (* in reverse fold order *)
}

type status = Holds | Fails | Faults | Displaced | Outside_validity

type reading = { form : string; subject : string; status : status; nodes : node list }

(* [f] with the evaluation watched: every node of [nodes] that has a reading
   of its own records what the evaluation found there, and the node of a
   fold over a set what the fold reports per contributor *)
let observing (nodes : node list) f =
  let stack = ref [] in
  let pop () = stack := List.tl !stack in
  let around site k record =
    match List.find_opt (fun n -> same n.site site) nodes with
    | Some n when n.own -> (
        if n.outcome <> None then
          failwith
            (Printf.sprintf "ref/lib/reading.ml: node %d (%s) is evaluated twice in one reading"
               n.number n.kind);
        stack := `Node n :: !stack;
        match k () with
        | v ->
            pop ();
            n.outcome <- Some (record v);
            v
        | exception (Value.Fault { code; _ } as e) ->
            pop ();
            n.outcome <- Some (Faulted code);
            raise e
        | exception e ->
            pop ();
            n.outcome <- Some Ended;
            raise e)
    | _ ->
        stack := `Other :: !stack;
        Fun.protect ~finally:pop k
  in
  (* a node the evaluation reads as a timeline, without evaluating it *)
  let read site outcome =
    match (!stack, List.find_opt (fun n -> same n.site site) nodes) with
    | (`Node _ :: _ | []), Some n when n.own ->
        if n.outcome <> None then
          failwith
            (Printf.sprintf "ref/lib/reading.ml: node %d (%s) is read twice in one reading" n.number
               n.kind);
        n.outcome <- Some outcome
    | _ -> ()
  in
  let o =
    {
      Eval.expr = (fun e k -> around (S_expr e) k (fun v -> Got v));
      cond = (fun c k -> around (S_cond c) k (fun b -> Held b));
      member =
        (fun label c ->
          match !stack with `Node n :: _ -> n.members <- (label, c) :: n.members | _ -> ());
      timeline = (fun e tl -> read (S_expr e) (Whole tl));
      periods = (fun c tl -> read (S_cond c) (Over tl));
    }
  in
  let saved = !Eval.observer in
  Eval.observer := Some o;
  Fun.protect ~finally:(fun () -> Eval.observer := saved) f

let drop (_ : Value.deposit) = ()

(* A form's numbered nodes, fresh for one reading. *)
let fresh numbered =
  List.map
    (fun (number, site, kind, own, scope) -> { number; kind; site; own; scope; outcome = None; members = [] })
    numbered

(* The reading of [condition] on [self], and of the value where it holds:
   [compute] evaluates it, by default [value] as an expression. A form
   without a condition, read where it is at its turn, holds. *)
let read_on ?compute ~form ~numbered env world (self : World.instance) ~condition ~value =
  let nodes = fresh numbered in
  let compute =
    match (compute, value) with
    | Some f, _ -> Some f
    | None, Some v -> Some (fun () -> ignore (Eval.eval_expr env world self ~err:drop v))
    | None, None -> None
  in
  (* the value's fault stands at its nodes; the status is the condition's *)
  let evaluate () = match compute with Some f -> ( try f () with Value.Fault _ -> ()) | None -> () in
  let status =
    observing nodes (fun () ->
        match condition with
        | None ->
            evaluate ();
            Holds
        | Some condition -> (
            match Eval.eval_cond env world self ~err:drop condition with
            | true ->
                evaluate ();
                Holds
            | false -> Fails
            | exception Value.Fault _ -> Faults))
  in
  { form; subject = self.label; status; nodes }

(* The reading of a period window that is the whole condition of a rule on a
   timeline target (ch. 3.10's clip): it gates nothing, so the rule holds; its
   bounds are read, the window is the part of the timeline it keeps, and the
   value is the timeline the rule writes, cut to it. *)
let read_clip ~form ~numbered env world (self : World.instance) ~condition ~bounds ~compute =
  let nodes = fresh numbered in
  let starting_at, up_to, to_inclusive = bounds in
  observing nodes (fun () ->
      let window =
        match Eval.window_bounds env world self ~err:drop starting_at up_to with
        | Some None, _ | _, Some None -> (0, 0)
        | dv, dt ->
            ( (match dv with Some (Some z) -> z | _ -> min_int),
              match dt with Some (Some z) -> if to_inclusive then z + 1 else z | _ -> max_int )
      in
      Eval.held_over condition (if fst window < snd window then Eval.as_periods [ window ] else []);
      compute window);
  { form; subject = self.label; status = Holds; nodes }

let without ~form status (self : World.instance) = { form; subject = self.label; status; nodes = [] }

(* whether a validity window admits the session: Some false outside it; a
   dated window in a session without an evaluation date is a fault (ch. 8.1) *)
let admitted env g =
  match Eval.applies env g with b -> Some b | exception Value.Fault _ -> None

let in_window env g ~form subjects k =
  match admitted env g with
  | Some true -> List.concat_map k subjects
  | Some false -> List.map (without ~form Outside_validity) subjects
  | None -> List.map (without ~form Faults) subjects

let of_type env world ?(except = []) otype =
  World.of_type
    ~isa:(fun a b -> Eval.isa env a b && not (List.exists (fun t -> norm t = norm a) except))
    world otype

(* --- the forms ------------------------------------------------------------ *)

(* A rule on an attribute has a reading where it has a condition or computes
   a value: a characteristic attribution without a condition has nothing to
   read. *)
let rules json env ~world =
  List.concat_map
    (fun (r : resolved_rule) ->
      match r.objective with
      | Some (Attribute_reference { attribute_ref = Some attr; object_type = Some ot; _ })
        when r.rr_condition <> None || r.rr_presentation <> Some Characteristic_attribution ->
          let condition = r.rr_condition in
          let form = r.rr_pointer in
          let env = { env with Eval.subj = Some ot } in
          let computes, except =
            match r.rr_presentation with
            | Some Characteristic_attribution -> (false, [])
            | _ -> (true, Ir_v0.Desugar.absorbed_on env.doc ~otype:ot ~attr)
          in
          let value = if computes then r.value_node else None in
          let numbered =
            number_nodes json ~form
              ~condition:(Option.map (fun c -> (c, form ^ "/condition")) condition)
              ~value:(Option.map (fun v -> (v, form ^ "/value")) value)
          in
          (* a rule on a timeline target writes the timeline its builtin folds
             (ch. 3.7, 3.8), and a period window as its whole condition cuts
             that timeline instead of gating the rule (ch. 3.10) *)
          let temporal = computes && Ir_v0.Checks.attr_temporal (Eval.object_type env ot) attr in
          in_window env r.rr_validity ~form (of_type env world ~except ot) (fun self ->
              if r.rr_presentation = Some Initialisation
                 && not (World.init_may_write world ~label:self.label ~attr)
              then [ without ~form Displaced self ]
              else if not temporal then [ read_on ~form ~numbered env world self ~condition ~value ]
              else
                let v = Eval.req "RuleVersion.value" r.value_node in
                let write ?window () = ignore (Eval.run_pro_rata ?window env world self ~ot ~attr v) in
                match condition with
                | Some (Time_window { starting_at; up_to; to_inclusive; _ } as condition) ->
                    [ read_clip ~form ~numbered env world self ~condition
                        ~bounds:(starting_at, up_to, to_inclusive)
                        ~compute:(fun window -> write ~window ()) ]
                | _ -> [ read_on ~compute:write ~form ~numbered env world self ~condition ~value ])
      | _ -> [])
    env.Eval.doc.resolved_rules

let consistencies json env ~world =
  List.concat_map
    (fun (c : resolved_consistency) ->
      match (c.assertion, c.cons_subject) with
      | Some condition, Some ot ->
          let form = c.cons_pointer in
          let env = { env with Eval.subj = Some ot } in
          let numbered =
            number_nodes json ~form ~condition:(Some (condition, form ^ "/assertion")) ~value:None
          in
          in_window env c.cons_validity ~form (of_type env world ot) (fun self ->
              [ read_on ~form ~numbered env world self ~condition:(Some condition) ~value:None ])
      | _ -> [])
    env.Eval.doc.consistencies

let allocations json env ~world =
  List.concat_map
    (fun (v : resolved_allocation) ->
      match (v.vd_condition, v.source_type) with
      | Some condition, Some ot ->
          let form = v.vd_pointer in
          let env = { env with Eval.subj = Some ot } in
          let numbered =
            number_nodes json ~form ~condition:(Some (condition, form ^ "/condition")) ~value:None
          in
          in_window env v.vd_validity ~form (of_type env world ot) (fun self ->
              [ read_on ~form ~numbered env world self ~condition:(Some condition) ~value:None ])
      | _ -> [])
    env.Eval.doc.allocations

let object_creations json env ~world0 ~world =
  List.concat_map
    (fun (oc : resolved_object_creation) ->
      match (oc.oc_condition, oc.oc_subject) with
      | Some condition, Some ot ->
          let form = oc.oc_pointer in
          let env = { env with Eval.subj = Some ot } in
          let numbered =
            number_nodes json ~form ~condition:(Some (condition, form ^ "/condition")) ~value:None
          in
          in_window env oc.oc_validity ~form (of_type env world ot) (fun self ->
              if Eval.creation_blocked ~world0 (Eval.creation_pair env oc) self then
                [ without ~form Displaced self ]
              else [ read_on ~form ~numbered env world self ~condition:(Some condition) ~value:None ])
      | _ -> [])
    env.Eval.doc.object_creations

let fact_creations json env ~world =
  List.concat_map
    (fun (fc : resolved_fact_creation) ->
      match (fc.fc_condition, fc.anchor_point) with
      (* a condition on the target filters the targets: it is asked per
         (anchor, target), and no reading per anchor carries it *)
      | Some condition, Some ot when not (Eval.guard_on_target env.Eval.doc fc) ->
          let form = fc.fc_pointer in
          let env = { env with Eval.subj = Some ot } in
          let numbered =
            number_nodes json ~form ~condition:(Some (condition, form ^ "/condition")) ~value:None
          in
          in_window env fc.fc_validity ~form (of_type env world ot) (fun self ->
              [ read_on ~form ~numbered env world self ~condition:(Some condition) ~value:None ])
      | _ -> [])
    env.Eval.doc.fact_creations

(* Every default of the document by its pointer, and every exception default
   within it. *)
let default_pointers (doc : document) =
  let rec chain prefix defaults =
    List.concat
      (List.mapi
         (fun j (d : default) ->
           let p = Printf.sprintf "%s/%d" prefix j in
           (d, p)
           ::
           (match d.default_exceptions with
           | None -> []
           | Some egs ->
               List.concat
                 (List.mapi
                    (fun m (eg : exception_group) ->
                      chain (Printf.sprintf "%s/exceptions/members/%d/defaults" p m) eg.eg_defaults)
                    egs.egs_members)))
         defaults)
  in
  List.concat
    (List.mapi
       (fun i (l : prioritized_defaults) ->
         chain (Printf.sprintf "/rules/prioritizedDefaults/%d/defaults" i) l.defaults)
       doc.prioritized_defaults)

(* The defaults stacks (RUNTIME-SEMANTICS ch. 7.1, 7.9, 7.10): per concrete
   owner and per subject, each member's chain walked as the session walks it
   ([Eval.run_defaults_group]). A default the walk reaches has the reading of
   its justification, and of its consequence where that holds; a default
   below the one that decided, and every default within it, is displaced,
   and so is a default whose exception groups settled. An attribute default
   without a justification — the initialisation default, an ordinary rule
   without a condition folded into the stack — has the reading of its
   consequence, which holds where the default is at its turn; a
   characteristic default without one has nothing to read. An
   initialisation's turn, a default's or a folded rule's, is that of an
   initialisation: a target the input fills displaces it. *)
let defaults json env ~world0 ~world =
  let doc = env.Eval.doc in
  let pointers = default_pointers doc in
  (* the pointer of a default and the places of its justification and its
     consequence: a declared default, or an ordinary rule folded into the
     stack (ch. 7.10), which keeps its rule's pointer *)
  let place (d : default) =
    match List.find_opt (fun (x, _) -> x == d) pointers with
    | Some (_, p) -> Some (p, p ^ "/justification", p ^ "/consequence")
    | None -> (
        match
          List.find_opt
            (fun (r : resolved_rule) ->
              match (r.value_node, d.default_consequence) with Some a, Some b -> a == b | _ -> false)
            doc.resolved_rules
        with
        | Some r -> Some (r.rr_pointer, r.rr_pointer ^ "/condition", r.rr_pointer ^ "/value")
        | None -> None)
  in
  let reading_of (l : prioritized_defaults) (d : default) =
    (* an ordinary rule without a condition folded into a stack carries the
       fold's vacuous justification, which stands nowhere in the document *)
    let condition =
      match d.default_justification with
      | Some c when c == Ir_v0.Desugar.always_true -> None
      | c -> c
    in
    let value = match (l.pd_attribute, d.default_consequence) with Some _, Some v -> Some v | _ -> None in
    match (condition, value) with
    | None, None -> None
    | _ -> (
        match place d with
        | None -> failwith "ref/lib/reading.ml: a default of a stack stands at no place of the document"
        | Some (form, cond_at, value_at) ->
            let numbered =
              number_nodes json ~form
                ~condition:(Option.map (fun c -> (c, cond_at)) condition)
                ~value:(Option.map (fun v -> (v, value_at)) value)
            in
            Some (form, condition, value, numbered))
  in
  List.concat_map
    (fun (owner, _, members, _) ->
      List.concat_map
        (fun (self : World.instance) ->
          let out = ref [] in
          let emit r = out := r :: !out in
          List.iter
            (fun ((l : prioritized_defaults), origins) ->
              let rec every (d : default) =
                (match reading_of l d with Some (form, _, _, _) -> [ form ] | None -> [])
                @
                match d.default_exceptions with
                | None -> []
                | Some egs ->
                    List.concat_map
                      (fun (eg : exception_group) -> List.concat_map every eg.eg_defaults)
                      egs.egs_members
              in
              let displace ds =
                List.iter
                  (fun d -> List.iter (fun form -> emit (without ~form Displaced self)) (every d))
                  ds
              in
              match admitted env l.pd_validity with
              | (Some false | None) as a ->
                  let status = if a = None then Faults else Outside_validity in
                  List.iter
                    (fun d -> List.iter (fun form -> emit (without ~form status self)) (every d))
                    l.defaults
              | Some true ->
                  let is_char = l.pd_characteristic <> None in
                  (* whether the default's own justification and consequence decide,
                     its reading emitted *)
                  let own env (d : default) =
                    let input_filled =
                      d.default_initialisation
                      &&
                      match (l.pd_attribute, SMap.find_opt self.label world0.World.instances) with
                      | Some a, Some (i0 : World.instance) -> World.attr i0 a <> Value.Empty
                      | _ -> false
                    in
                    match reading_of l d with
                    | Some (form, _, _, _) when input_filled ->
                        emit (without ~form Displaced self);
                        false
                    | Some (form, condition, value, numbered) ->
                        let r = read_on ~form ~numbered env world self ~condition ~value in
                        emit r;
                        r.status = Holds
                        && (is_char
                           || List.exists
                                (fun n ->
                                  (match value with Some v -> same n.site (S_expr v) | None -> false)
                                  && match n.outcome with Some (Got v) -> v <> Value.Empty | _ -> false)
                                r.nodes)
                    | None -> (
                        (* no reading: the decision without one, as the session takes it *)
                        (not input_filled)
                        && (match d.default_justification with
                           | Some c -> Eval.eval_cond env world self ~err:drop c
                           | None -> true)
                        &&
                        if is_char then d.default_decision <> None
                        else
                          match d.default_consequence with
                          | Some v -> Eval.eval_expr env world self ~err:drop v <> Value.Empty
                          | None -> false)
                  in
                  (* whether the chain [ds] decides; a conflict of exception
                     groups settles them as a decision does (ch. 7.9) *)
                  let rec decide ~env_at (ds : default list) =
                    let rec go i = function
                      | [] -> false
                      | (d : default) :: rest ->
                          let env = env_at i in
                          let has_content = d.default_decision <> None || d.default_consequence <> None in
                          let settled =
                            match d.default_exceptions with
                            | None -> false
                            | Some egs ->
                                (* every exception group is walked: they reconcile as equals *)
                                List.fold_left
                                  (fun acc (eg : exception_group) ->
                                    decide ~env_at:(fun _ -> env) eg.eg_defaults || acc)
                                  false egs.egs_members
                          in
                          if settled then (
                            (match reading_of l d with
                            | Some (form, _, _, _) -> emit (without ~form Displaced self)
                            | None -> ());
                            displace rest;
                            true)
                          else if has_content && own env d then (
                            displace rest;
                            true)
                          else go (i + 1) rest
                    in
                    go 0 ds
                  in
                  ignore
                    (decide
                       ~env_at:(fun i -> { env with Eval.subj = Some (List.nth origins i) })
                       l.defaults))
            members;
          List.rev !out)
        (World.of_type ~isa:(fun a b -> norm a = norm b) world owner))
    (Eval.effective_defaults env)

(* Every reading of the session whose input world is [world0] and whose
   fixpoint is [world]. *)
let readings json env ~world0 ~world =
  rules json env ~world @ consistencies json env ~world @ allocations json env ~world
  @ object_creations json env ~world0 ~world @ fact_creations json env ~world
  @ defaults json env ~world0 ~world

(* --- the canonical text --------------------------------------------------- *)

let quote s =
  let b = Buffer.create (String.length s + 2) in
  Buffer.add_char b '"';
  String.iter
    (function
      | '\\' -> Buffer.add_string b "\\\\"
      | '"' -> Buffer.add_string b "\\\""
      | '\n' -> Buffer.add_string b "\\n"
      | '\r' -> Buffer.add_string b "\\r"
      | c -> Buffer.add_char b c)
    s;
  Buffer.add_char b '"';
  Buffer.contents b

(* [m] / 10^[s] in plain decimal notation, without trailing zeros after the
   point *)
let plain ~negative m s =
  let digits = Z.to_string m in
  let body =
    if s <= 0 then digits ^ String.make (-s) '0'
    else
      let digits =
        if String.length digits <= s then String.make (s + 1 - String.length digits) '0' ^ digits
        else digits
      in
      let k = String.length digits - s in
      let frac = String.sub digits k s in
      let n = ref s in
      while !n > 0 && frac.[!n - 1] = '0' do decr n done;
      String.sub digits 0 k ^ if !n = 0 then "" else "." ^ String.sub frac 0 !n
  in
  if negative then "-" ^ body else body

(* A number as the canonical text writes it: its exact decimal where the
   rational terminates, else its quotient to 16 significant digits rounded
   half to even, as the write boundary of the Drools backend collapses a
   rational that does not terminate (IEEE 754 decimal64) *)
let decimal (q : Q.t) =
  if Q.sign q = 0 then "0"
  else
    let negative = Q.sign q < 0 in
    let a = Z.abs (Q.num q) and b = Q.den q in
    let rec strip n p = if Z.equal (Z.rem n p) Z.zero then 1 + strip (Z.div n p) p else 0 in
    let rec without n p = if Z.equal (Z.rem n p) Z.zero then without (Z.div n p) p else n in
    if Z.equal (without (without b (Z.of_int 2)) (Z.of_int 5)) Z.one then
      let k = max (strip b (Z.of_int 2)) (strip b (Z.of_int 5)) in
      let m = Z.div (Z.mul a (Z.pow (Z.of_int 10) k)) b in
      plain ~negative m k
    else
      let ten = Z.of_int 10 in
      (* E with 10^E <= a/b < 10^(E+1) *)
      let ge p = if p >= 0 then Z.geq a (Z.mul b (Z.pow ten p)) else Z.geq (Z.mul a (Z.pow ten (-p))) b in
      let e = ref (String.length (Z.to_string a) - String.length (Z.to_string b)) in
      while not (ge !e) do decr e done;
      while ge (!e + 1) do incr e done;
      let s = 15 - !e in
      let num, den = if s >= 0 then (Z.mul a (Z.pow ten s), b) else (a, Z.mul b (Z.pow ten (-s))) in
      let quo, rem = Z.div_rem num den in
      let twice = Z.mul rem (Z.of_int 2) in
      let c = Z.compare twice den in
      let m = if c > 0 || (c = 0 && Z.is_odd quo) then Z.succ quo else quo in
      let m, s = if Z.equal m (Z.pow ten 16) then (Z.pow ten 15, s - 1) else (m, s) in
      plain ~negative m s

let rec canonical (v : Value.t) =
  match v with
  | Value.Empty -> "empty"
  | Value.Bool b -> string_of_bool b
  | Value.Num (q, _) -> decimal q
  | Value.Date z -> Value.iso_of_date z
  | Value.Date_time t ->
      let day, ms = Arith.split_ms t in
      let h = ms / 3_600_000 and mi = ms / 60_000 mod 60 and sec = ms / 1000 mod 60 and f = ms mod 1000 in
      Printf.sprintf "%sT%02d:%02d:%02d%s" (Value.iso_of_date day) h mi sec
        (if f = 0 then "" else Printf.sprintf ".%03d" f)
  | Value.Text s | Value.Enum s -> quote s
  | Value.Ctor (tag, payload) ->
      quote tag ^ (match payload with Some p -> "(" ^ canonical p ^ ")" | None -> "")
  | Value.List_value l -> "[" ^ String.concat ", " (List.map canonical l) ^ "]"
  | Value.Record fs ->
      "{" ^ String.concat ", " (List.sort compare (List.map (fun (k, x) -> quote k ^ "=" ^ canonical x) fs)) ^ "}"

let day_text z = if z = min_int || z = max_int then "-" else Value.iso_of_date z

let timeline_text (tl : Timeline.t) =
  "["
  ^ String.concat ", "
      (List.map
         (fun (sg : Timeline.segment) ->
           "[" ^ day_text sg.s_from ^ " " ^ day_text sg.s_to ^ " " ^ canonical sg.s_value ^ "]")
         tl)
  ^ "]"

let unit_text = function
  | Value.Unitless -> None
  | Value.Unit e -> Some (Arith.display_unit e)
  | Value.Product (a, b) -> Some (Arith.display_unit a ^ "*" ^ Arith.display_unit b)

(* An object of the session as the canonical text names it: an input object
   by its case label, quoted; an object a creation made by that creation's
   name, its subject's name and its number among the objects that creation
   made for that subject, which the session keeps at one *)
let rec name_of (doc : document) (world : World.t) label =
  if not (SMap.mem label world.World.mints) then quote label
  else
    let fail () = failwith (Printf.sprintf "ref/lib/reading.ml: the made object %S has no name" label) in
    match String.index_opt label ':' with
    | Some k when k > 2 && String.sub label 0 2 = "oc" && k + 1 < String.length label && label.[k + 1] = ':' -> (
        match int_of_string_opt (String.sub label 2 (k - 2)) with
        | Some idx when idx < List.length doc.object_creations ->
            let oc = List.nth doc.object_creations idx in
            let name = match oc.oc_name with Some n -> n | None -> fail () in
            let subject = String.sub label (k + 2) (String.length label - k - 2) in
            "«" ^ quote name ^ "»(" ^ name_of doc world subject ^ ")#1"
        | _ -> fail ())
    | _ -> fail ()

let status_text = function
  | Holds -> "HOLDS"
  | Fails -> "FAILS"
  | Faults -> "FAULTS"
  | Displaced -> "DISPLACED"
  | Outside_validity -> "OUTSIDE_VALIDITY"

let fault_text = function Some c -> "fault " ^ c | None -> "fault -"

(* The readings in the canonical text, sorted: per reading one line with the
   form, the object and the status, and per node with a reading of its own
   one line with the number and the kind of the node and what the
   evaluation found there — [unreached]; a verdict; a value, with the unit of
   a number; [fault] with the EF code of the fault (ch. 5.7b) or [-]; and the
   contributors of a fold over a set, sorted, each with its value, verdict
   or fault and [reached] or [unreached]. An
   expression whose value is leeg, which a read of a leeg binder ended or
   which faulted carries the unit of its declaration or its annotation. *)
let lines env ~world (rs : reading list) =
  let name = name_of env.Eval.doc world in
  let with_unit s = function Some u -> s ^ " " ^ quote u | None -> s in
  let node_line head n =
    let b = Printf.sprintf "%s %d %s" head n.number n.kind in
    let effective () =
      match n.site with
      | S_expr e ->
          (* a binder read has the type of the value its Let binds there *)
          let btypes =
            List.fold_left
              (fun bt (binder, value) ->
                SMap.add (norm binder) (Eval.static_type { env with Eval.btypes = bt } value) bt)
              SMap.empty n.scope
          in
          Eval.unit_of_datatype (Eval.static_type { env with Eval.btypes } e)
      | S_cond _ | S_lambda _ -> Value.Unitless
    in
    let static_unit () = unit_text (effective ()) in
    (* a number in the unit of its node's effective type (ch. 2.10), which a
       value the evaluation computed in another unit is restated into: a leeg
       coalesced to 0, a fold's 0 over no contributor, an addition whose left
       operand was leeg (ch. 2.6) *)
    let in_effective_unit q u =
      match (u, effective ()) with
      | _, su when su = u -> (q, u)
      | _, Value.Unitless -> (q, u)
      | Value.Unitless, su -> (q, su)
      | _, su -> (Arith.restate_into su q u, su)
    in
    let found =
      match n.outcome with
      | None -> b ^ " unreached"
      | Some (Held v) -> b ^ " " ^ string_of_bool v
      | Some (Got (Value.Num (q, u))) ->
          let q, u = in_effective_unit q u in
          with_unit (b ^ " " ^ canonical (Value.Num (q, u))) (unit_text u)
      | Some (Got Value.Empty) | Some Ended -> with_unit (b ^ " empty") (static_unit ())
      | Some (Got v) -> b ^ " " ^ canonical v
      | Some (Faulted code) -> with_unit (b ^ " " ^ fault_text code) (static_unit ())
      | Some (Whole None) -> with_unit (b ^ " empty") (static_unit ())
      | Some (Whole (Some tl)) -> with_unit (b ^ " " ^ timeline_text tl) (static_unit ())
      | Some (Over tl) -> b ^ " " ^ timeline_text tl
    in
    (* the filled count is a count (ch. 3.4c): its contributor is the member
       it counts, the one whose value is filled, and not that value *)
    let counts =
      match n.site with S_expr (Function_call { kind = Filled_count; _ }) -> true | _ -> false
    in
    if n.outcome = None || n.members = [] then found
    else
      let contributor (label, (c : Eval.contribution)) =
        name label ^ " "
        ^ (match c.found with
          | Member_value _ when counts -> canonical Value.Empty
          | Member_value v -> canonical v
          | Member_verdict v -> string_of_bool v
          | Member_fault code -> fault_text code)
        ^ if c.reached then " reached" else " unreached"
      in
      found ^ " contributors [" ^ String.concat "; " (List.sort compare (List.map contributor n.members)) ^ "]"
  in
  List.sort compare
    (List.concat_map
       (fun r ->
         let head = r.form ^ " " ^ name r.subject in
         (head ^ " " ^ status_text r.status)
         :: List.filter_map (fun n -> if n.own then Some (node_line head n) else None) r.nodes)
       rs)
