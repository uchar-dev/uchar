(* The empty-closures inventory (verifier --empty-closures): every place of a
   document where an outcome can rest on a leeg value. It changes nothing and
   judges nothing; it lists.

   The measure. A place counts where the two-valued reading of
   RUNTIME-SEMANTICS ch. 1.2 lets a form decide (write a value, attribute a
   characteristic, report a consistency fault) in a state where a value it
   reads is leeg, while a reading in which leeg propagates and a condition
   over leeg is undetermined would decide nothing there; and where the form
   itself closes the world on leeg (an initialisation, a leeg or gevuld test,
   a fallback). A comparison that is false on leeg in a position where the
   form then does not decide is no place; the same comparison in a negative
   position is one. Those places fall in two classes, closure (the value side
   and the forms that close on leeg) and negation (a condition or a default
   that decides because what it reads could not be tested). A third class,
   absent-instance, holds the places where the absence of an instance or a
   link decides: a role check in a negative position, a quantifier fold that
   holds or fails without a member, a count or total that is 0 and a fallback
   that stands in without one. A read over an absent to-one link is leeg and
   belongs to the first two classes.

   The walk reads the document as loaded, the wire form without the ch. 7.7
   desugaring, so every pointer it reports is a pointer of the document. It
   keeps a polarity per condition node: positive where the form decides when
   the node is true, negative where it decides when the node is false, both
   where it decides either way (a filter of a value fold, the condition of an
   IfThenElse, the members of an `exactly`).

   A member is every attribute and every cell (each can be leeg: an input not
   given, a writer that did not decide) and every list-typed parameter (an
   absent one reads leeg, ch. 10.1); any other parameter is never leeg, since
   reading an unbound one faults (EF-014). The input members behind a member
   are found over the dependency graph of WF-045/046 ([Checks.dep_forms]). *)

open Ast

type polarity = Positive | Negative | Both

let flip = function Positive -> Negative | Negative -> Positive | Both -> Both
let decides_on_true = function Positive | Both -> true | Negative -> false
let decides_on_false = function Negative | Both -> true | Positive -> false

(* One read of a member that can be leeg: the member node, spelled as
   --dependency-edges spells it, and the identity of the read (the same
   attribute read, or the same cell of it), which a gevuld test of the same
   read discharges. *)
type read_key = string option * string option * (string * string) list option * anchor option * string option list option

(* The identity of a read: two reads are the same when they read one attribute
   on one type over one path with one anchor, or the same cell of it (that read
   and the same labels); any other expression has none. *)
let rec read_key_of (e : expr) : read_key option =
  match e with
  | Attribute_reference { attribute_ref; object_type; navigation_bindings; anchor; _ } ->
      Some
        ( Option.map Checks.norm attribute_ref,
          Option.map Checks.norm object_type,
          Option.map (List.map (fun (b : role_binding) -> (Checks.norm b.rb_fact_type, Checks.norm b.rb_role))) navigation_bindings,
          anchor,
          None )
  | Dimension_cell_read { attribute_ref = Some (Attribute_reference _ as a); labels; _ } ->
      Option.map
        (fun (attr, otn, path, anchor, _) -> (attr, otn, path, anchor, Some (List.map (Option.map Checks.norm) labels)))
        (read_key_of a)
  | Literal _ | Text_composition _ | Binary _ | Function_call _ | Instance_set_union _ | Rounding _ | Clamp _
  | Dimension_cell_read _ | Parameter_ref _ | Variable_reference _ | Context_value _ | Bound_day _
  | Expr_unresolved _ | Temporal_read _ | Construct _ | Match _ | Binder_ref _ | Struct_literal _ | Field_access _
  | Let _ | List_literal _ | Lambda _ | If_then_else _ | Invoke _ | Fold _ | Map_list _ | Filter_list _
  | List_length _ ->
      None

type carrier = { node : string; key : read_key option }

type form_info = {
  f_name : string;
  f_key : string; (* the pointer of the form in the dependency graph, on the wire *)
  f_chars : string list; (* the characteristics its own condition requires not to hold, as a conjunct *)
  f_skip_target : string option; (* an initialisation: its own target, read as the input it guards *)
}

type place = {
  cls : string;
  kind : string;
  at : string;
  form : form_info;
  members : (carrier * bool) list; (* a read and whether a gevuld test of it is in scope *)
}

(* The kinds in the order a line with the same pointer is printed in. *)
let kinds =
  [ ("closure", "initialisation"); ("negation", "fallthrough"); ("closure", "coalesce-operand");
    ("closure", "coalesce-subtrahend"); ("closure", "coalesce-dividend"); ("closure", "empty-divisor-fault");
    ("closure", "coalesce-duration"); ("closure", "fold-skips-empty"); ("closure", "filled-count"); ("closure", "empty-fallback");
    ("closure", "clamp-empty-bound"); ("closure", "empty-text-part"); ("closure", "allocation-empty-input");
    ("closure", "true-on-empty"); ("closure", "empty-test"); ("negation", "false-on-empty");
    ("negation", "negated-characteristic"); ("absent-instance", "negated-role-check");
    ("absent-instance", "vacuous-quantifier"); ("absent-instance", "empty-set-count");
    ("absent-instance", "empty-set-fallback") ]

(* the leaf kinds: what a higher default's justification reads when the default
   below it falls through *)
let leaf_kinds = [ "true-on-empty"; "empty-test"; "false-on-empty"; "negated-characteristic" ]

type env = {
  ctx : Checks.ctx; (* ctx.form: the subject of the form *)
  subject : string option; (* the subject of the position *)
  tests : read_key list; (* the gevuld tests in scope *)
  form : form_info;
  emit : place -> unit;
  record_test : string -> string list -> unit; (* form key, members a test closes on *)
}

(* A gevuld test of a read: the read is filled. *)
let filled_key (c : condition_expr) =
  match c with
  | Predicate { variety = Filled; negation = false; operand = Some p; time_period = None; _ }
  | Predicate { variety = Empty_predicate; negation = true; operand = Some p; time_period = None; _ } ->
      read_key_of p
  | Predicate _ -> None
  | Comparison _ | Characteristic_check _ | Role_membership _ | Time_window _ | Compound _ | Quantifier_fold _
  | Identity_check _ | Rule_status_check _ | Condition_unresolved _ ->
      None

let rec conjunct_keys (c : condition_expr) =
  match c with
  | Compound { quantifier_kind = All; components; _ } -> List.concat_map conjunct_keys components
  | Compound _ | Predicate _ | Comparison _ | Characteristic_check _ | Role_membership _ | Time_window _
  | Quantifier_fold _ | Identity_check _ | Rule_status_check _ | Condition_unresolved _ ->
      Option.to_list (filled_key c)

(* The characteristic a check reads, spelled as the dependency graph spells
   it: on the reached type of a navigated check, on the host or the subject
   otherwise (as [Checks.dep_cond_at]). *)
let characteristic_node env ~subject (c : condition_expr) =
  match c with
  | Characteristic_check { characteristic = Some k; navigation_binding = Some rb; _ } -> (
      match Checks.resolve_binding env.ctx rb with
      | Ok (_, r, _) -> Some (Checks.n_char (Option.value r.role_object_type_name ~default:"") k)
      | Error _ -> None)
  | Characteristic_check { characteristic = Some k; holder; navigation_binding = None; _ } -> (
      match (match holder with Some _ -> holder | None -> subject) with
      | Some ot -> Some (Checks.n_char ot k)
      | None -> None)
  | Characteristic_check { characteristic = None; _ } -> None
  | Predicate _ | Comparison _ | Role_membership _ | Time_window _ | Compound _ | Quantifier_fold _
  | Identity_check _ | Rule_status_check _ | Condition_unresolved _ ->
      None

(* The characteristics a condition requires not to hold, as a conjunct of the
   form: the condition itself, or a member of an `all` at any depth, checked
   where the form decides on false ("heeft geen K"). A form that requires K
   to hold decides because the writer of K closed on leeg, which is no guard
   of what the form reads. *)
let rec conjunct_chars ctx ~subject (c : condition_expr) =
  let env = { ctx; subject; tests = []; form = { f_name = ""; f_key = ""; f_chars = []; f_skip_target = None };
              emit = ignore; record_test = (fun _ _ -> ()) } in
  match c with
  | Compound { quantifier_kind = All; components; _ } -> List.concat_map (conjunct_chars ctx ~subject) components
  | Characteristic_check { negation; time_period; anchor; _ } ->
      (* a negated whole-period qualifier turns the check around once more *)
      let qualifier_negated =
        match time_period with Some { gp_negated; _ } -> gp_negated | None -> false
      in
      if negation <> qualifier_negated then
        let subject = match anchor with Some Anchor_form -> ctx.form | None -> subject in
        Option.to_list (characteristic_node env ~subject c)
      else []
  | Compound _ | Predicate _ | Comparison _ | Role_membership _ | Time_window _ | Quantifier_fold _
  | Identity_check _ | Rule_status_check _ | Condition_unresolved _ ->
      []

let numeric_type (t : datatype option) =
  match t with
  | Some (Number _ | Percentage _) -> Some true
  | Some (Text_type | Boolean | Date_in_days | Date_time_type | Enumeration _ | Sum_type _ | List_type _ | Struct_type _) ->
      Some false
  | None -> None

let static_type env e = Checks.expr_type env.ctx ~binders:[] e

let is_date env = function
  | Some e -> (
      match static_type env e with
      | Some (Date_in_days | Date_time_type) -> true
      | Some (Number _ | Percentage _ | Text_type | Boolean | Enumeration _ | Sum_type _ | List_type _ | Struct_type _)
      | None ->
          false)
  | None -> false

(* Two leeg sides are equal under numeric equality only (ch. 1.2); where
   neither side has a static type the comparison is read as numeric, so the
   place is counted rather than missed. *)
let is_numeric env l r =
  let t = function Some e -> numeric_type (static_type env e) | None -> None in
  match (t l, t r) with Some b, _ | None, Some b -> b | None, None -> true

(* A relation a range walks, as the member of an absent-instance place: the
   fact type and the role the hop reaches. *)
let rel_node (rb : role_binding) = "rel:" ^ Checks.norm rb.rb_fact_type ^ ":" ^ Checks.norm rb.rb_role

(* The relations of the branches of a union a count folds over (WF-110). *)
let union_roles (e : expr) =
  match e with
  | Instance_set_union { branches; _ } -> List.map (fun (b : instance_set_branch) -> b.isb_source_role) branches
  | Literal _ | Text_composition _ | Binary _ | Function_call _ | Rounding _ | Clamp _ | Attribute_reference _
  | Dimension_cell_read _ | Parameter_ref _ | Variable_reference _ | Context_value _ | Bound_day _
  | Expr_unresolved _ | Temporal_read _ | Construct _ | Match _ | Binder_ref _ | Struct_literal _
  | Field_access _ | Let _ | List_literal _ | Lambda _ | If_then_else _ | Invoke _ | Fold _ | Map_list _
  | Filter_list _ | List_length _ ->
      []

let relations range = List.map (fun rb -> ({ node = rel_node rb; key = None }, false)) range

let starts p node = String.length node >= String.length p && String.sub node 0 (String.length p) = p

(* A member spelled on the type that declares it: a read spells it on the
   type it is read on, the graph a write on every comparable type, and one
   member must be one name in the report and in the guards. A cell keeps its
   labels; a member no type of the chain declares keeps its spelling. *)
let canon_node doc node =
  let rebuild prefix kind =
    let rest = String.sub node (String.length prefix) (String.length node - String.length prefix) in
    match String.index_opt rest ':' with
    | None -> node
    | Some i -> (
        let t = String.sub rest 0 i and m = String.sub rest (i + 1) (String.length rest - i - 1) in
        let name, labels =
          match String.index_opt m '[' with
          | Some j -> (String.sub m 0 j, String.sub m j (String.length m - j))
          | None -> (m, "")
        in
        match Checks.declaring_type doc ~from:t ~kind ~member:name with
        | Some d -> prefix ^ Checks.norm d ^ ":" ^ name ^ labels
        | None -> node)
  in
  if starts "attr:" node then rebuild "attr:" `Attribute
  else if starts "char:" node then rebuild "char:" `Characteristic
  else node

let place env ~cls ~kind ~at members =
  match members with [] -> () | _ -> env.emit { cls; kind; at; form = env.form; members }

let guarded env carriers =
  List.map (fun c -> (c, match c.key with Some k -> List.mem k env.tests | None -> false)) carriers

let emit_carriers env ~cls ~kind ~at carriers = place env ~cls ~kind ~at (guarded env carriers)

let qualified pol (tp : entire_period option) =
  match tp with Some { gp_negated = true; _ } -> flip pol | Some { gp_negated = false; _ } | None -> pol

(* The value walk: the places inside [e], emitted, and the reads whose leeg
   reaches the value of [e] (propagate), returned. A coalescing operator, a
   fold that skips leeg and a text part return none: their value is not leeg
   when a read is, which is what makes them a place. *)
let rec ex env at (e : expr) : carrier list =
  let sub f = at ^ "/" ^ f in
  let opt f = function Some x -> ex env (sub f) x | None -> [] in
  let each f xs = List.concat (List.mapi (fun i x -> ex env (Printf.sprintf "%s/%s/%d" at f i) x) xs) in
  match e with
  | Attribute_reference { attribute_ref = Some a; object_type = Some ot; _ } ->
      [ { node = Checks.n_attr ot a; key = read_key_of e } ]
  | Attribute_reference _ -> []
  | Dimension_cell_read
      { attribute_ref = Some (Attribute_reference { attribute_ref = Some a; object_type = Some ot; _ }); labels; _ } ->
      [ { node = Checks.n_cell ot a (Checks.some_names labels); key = read_key_of e } ]
  | Dimension_cell_read { attribute_ref; _ } -> ignore (opt "attribute" attribute_ref : carrier list); []
  | Parameter_ref { parameter = Some p; _ } -> (
      match Checks.find_ci env.ctx.doc.parameters p with
      | Some { par_datatype = Some (List_type _); _ } -> [ { node = "param:" ^ Checks.norm p; key = None } ]
      | Some _ | None -> [])
  | Parameter_ref { parameter = None; _ } -> []
  (* never leeg: a literal and a session value carry a value; a bound day is
     a day; an unbound variable marker and an Unresolved are refused or fault
     where an executor meets them, so neither has a value to be leeg; a binder
     of a Let or a match arm is read only when what it binds is not leeg
     (ch. 10.6, ch. 9.2) *)
  | Literal _ | Context_value _ | Bound_day _ | Variable_reference _ | Expr_unresolved _ | Binder_ref _ -> []
  | Text_composition { pieces; _ } ->
      let parts =
        List.concat
          (List.mapi
             (fun i -> function
               | Embedded_expression { expr = Some x; _ } -> ex env (Printf.sprintf "%s/parts/%d/expr" at i) x
               | Embedded_expression { expr = None; _ } | Literal_chunk _ -> [])
             pieces)
      in
      emit_carriers env ~cls:"closure" ~kind:"empty-text-part" ~at parts;
      []
  | Binary { op; left_side; right_side; _ } -> (
      let l = opt "left" left_side in
      let r = opt "right" right_side in
      match op with
      | (Plus | Min) when is_date env left_side ->
          emit_carriers env ~cls:"closure" ~kind:"coalesce-duration" ~at r;
          l
      | Plus | Min | Times | Percentage_of ->
          emit_carriers env ~cls:"closure" ~kind:"coalesce-operand" ~at (l @ r);
          []
      | Reduced_by ->
          emit_carriers env ~cls:"closure" ~kind:"coalesce-subtrahend" ~at r;
          l
      | Divided_by | Divided_by_abs ->
          emit_carriers env ~cls:"closure" ~kind:"coalesce-dividend" ~at l;
          (* a leeg divisor under a filled dividend deposits a §9.5 fact
             (EF-031): an inconsistency reported on leeg *)
          emit_carriers env ~cls:"closure" ~kind:"empty-divisor-fault" ~at r;
          r
      | Power -> l @ r)
  | Function_call
      { kind; call_arguments; element_object_type; empty_fallback; filter; dimension_selections; source_role;
        source_path; anchor; _ } -> (
      let per_member = element_object_type <> None && (source_role <> None || source_path <> None) in
      let fsubject =
        match (element_object_type, anchor) with
        | Some _, _ -> element_object_type
        | None, Some Anchor_form -> env.ctx.form
        | None, None -> env.subject
      in
      (* the filter of a value fold decides which members count, so both of
         its verdicts decide the value *)
      (match filter with
      | Some c -> co { env with subject = fsubject; tests = (if per_member then [] else env.tests) } Both (sub "filter") c
      | None -> ());
      let arg_env =
        { env with subject = fsubject;
                   tests = (if per_member then (match filter with Some c -> conjunct_keys c | None -> []) else env.tests) }
      in
      let walked = List.concat (List.mapi (fun i x -> ex arg_env (Printf.sprintf "%s/arguments/%d" at i) x) call_arguments) in
      let args =
        match (dimension_selections, call_arguments) with
        | Some sels, [ Attribute_reference { attribute_ref = Some a; object_type = Some ot; _ } ] ->
            List.map (fun ls -> { node = Checks.n_cell ot a ls; key = None }) (Checks.selected_cells sels)
        | Some _, _ | None, _ -> walked
      in
      let args_g = List.map (fun c -> (c, match c.key with Some k -> List.mem k arg_env.tests | None -> false)) args in
      let fallback = opt "emptyFallback" empty_fallback in
      (* the relations the fold ranges over: with none of their links the
         fold has no member *)
      let range =
        relations
          ((match source_role with Some rb -> [ rb ] | None -> [])
          @ Option.value source_path ~default:[]
          @ List.concat_map union_roles call_arguments)
      in
      match kind with
      | Sum_function | Minimum | Maximum | First | Last -> (
          place env ~cls:"closure" ~kind:"fold-skips-empty" ~at args_g;
          match empty_fallback with
          | Some _ ->
              place env ~cls:"closure" ~kind:"empty-fallback" ~at:(sub "emptyFallback") args_g;
              (* without a member the fallback decides *)
              place env ~cls:"absent-instance" ~kind:"empty-set-fallback" ~at:(sub "emptyFallback") range;
              fallback
          | None -> args)
      (* a total, a filled count and a count are 0 without a member *)
      | Total ->
          place env ~cls:"closure" ~kind:"fold-skips-empty" ~at args_g;
          place env ~cls:"absent-instance" ~kind:"empty-set-count" ~at range;
          []
      | Filled_count ->
          place env ~cls:"closure" ~kind:"filled-count" ~at args_g;
          place env ~cls:"absent-instance" ~kind:"empty-set-count" ~at range;
          []
      | Count ->
          place env ~cls:"absent-instance" ~kind:"empty-set-count" ~at range;
          []
      | Concatenation ->
          place env ~cls:"closure" ~kind:"empty-text-part" ~at args_g;
          []
      (* a day count counts the days its filter holds on, walked above *)
      | Day_count -> []
      | Square_root | Absolute_value | Duration | Absolute_duration | Easter_sunday | Time_proportional_part
      | Year_of | Month_of | Day_of | Date_with | Date_time_with ->
          args)
  | Instance_set_union { branches; _ } ->
      List.iteri
        (fun i (b : instance_set_branch) ->
          match b.isb_filter with
          | Some c ->
              co { env with subject = b.isb_element_type; tests = [] } Both (Printf.sprintf "%s/branches/%d/filter" at i) c
          | None -> ())
        branches;
      []
  | Rounding { base_expr; _ } -> opt "base" base_expr
  | Clamp { base_expr; minimum; maximum; _ } ->
      let b = opt "base" base_expr in
      let mn = opt "minimum" minimum in
      let mx = opt "maximum" maximum in
      emit_carriers env ~cls:"closure" ~kind:"clamp-empty-bound" ~at (mn @ mx);
      b
  | Temporal_read { base_expr; date_value; _ } ->
      let b = ex env (sub "base") base_expr in
      b @ ex env (sub "date") date_value
  | Construct { argument; _ } -> opt "argument" argument
  | Match { scrutinee; arms; _ } ->
      let s = ex env (sub "scrutinee") scrutinee in
      s @ List.concat (List.mapi (fun i (_, _, b) -> ex env (Printf.sprintf "%s/arms/%d/body" at i) b) arms)
  | Struct_literal { field_list; _ } ->
      List.concat (List.mapi (fun i (_, x) -> ex env (Printf.sprintf "%s/fields/%d/value" at i) x) field_list)
  | Field_access { base; _ } -> ex env (sub "base") base
  | Let { value; body; _ } ->
      let v = ex env (sub "value") value in
      v @ ex env (sub "body") body
  | List_literal { elements; _ } -> each "elements" elements
  (* a closed body reads no member, only its parameters: it is not walked,
     and what its call hands it is read at the call *)
  | Lambda _ -> []
  | If_then_else { condition; then_; else_; _ } ->
      co env Both (sub "condition") condition;
      let t = ex { env with tests = env.tests @ conjunct_keys condition } (sub "then") then_ in
      t @ ex env (sub "else") else_
  | Invoke { arguments; _ } -> each "arguments" arguments
  | Fold { list; init; _ } ->
      let l = ex env (sub "list") list in
      l @ ex env (sub "init") init
  | Map_list { list; _ } -> ex env (sub "list") list
  | Filter_list { list; condition; _ } ->
      let l = ex env (sub "list") list in
      co env Both (sub "condition") condition;
      l
  | List_length { list; _ } -> ex env (sub "list") list

(* The condition walk: the places of [c] at polarity [pol]. *)
and co env pol at (c : condition_expr) : unit =
  let sub f = at ^ "/" ^ f in
  let opt f = function Some x -> ex env (sub f) x | None -> [] in
  match c with
  | Comparison { op; left_side; right_side; time_period; _ } ->
      let l = opt "left" left_side in
      let r = opt "right" right_side in
      let pol = qualified pol time_period in
      let one = l <> [] || r <> [] and both = l <> [] && r <> [] in
      let on_true, on_false =
        match op with
        | Equal -> (both && is_numeric env left_side right_side, one)
        | Not_equal -> (one, both)
        | Less | Less_or_equal | Greater | Greater_or_equal | Earlier | Earlier_or_equal | Later | Later_or_equal ->
            (false, one)
      in
      if on_true && decides_on_true pol then emit_carriers env ~cls:"closure" ~kind:"true-on-empty" ~at (l @ r);
      if on_false && decides_on_false pol then emit_carriers env ~cls:"negation" ~kind:"false-on-empty" ~at (l @ r)
  | Predicate { variety = (Empty_predicate | Filled) as variety; operand; negation; time_period; _ } ->
      let o = opt "operand" operand in
      let pol = qualified pol time_period in
      let true_on_empty = (variety = Empty_predicate) <> negation in
      if (true_on_empty && decides_on_true pol) || ((not true_on_empty) && decides_on_false pol) then begin
        (* the form closes on the leeg of what it tests: only such a test
           guards a reader elsewhere, a positive gevuld test does not *)
        env.record_test env.form.f_key (List.map (fun x -> x.node) o);
        emit_carriers env ~cls:"closure" ~kind:"empty-test" ~at o
      end
  | Predicate { variety = Exact_digits | Day_kind_predicate; operand; negation; time_period; _ } ->
      let o = opt "operand" operand in
      let pol = qualified pol time_period in
      if negation && decides_on_true pol then emit_carriers env ~cls:"closure" ~kind:"true-on-empty" ~at o;
      if (not negation) && decides_on_false pol then emit_carriers env ~cls:"negation" ~kind:"false-on-empty" ~at o
  | Characteristic_check { negation; time_period; anchor; _ } -> (
      let subject = match anchor with Some Anchor_form -> env.ctx.form | None -> env.subject in
      let eff = qualified pol time_period in
      let eff = if negation then flip eff else eff in
      match characteristic_node env ~subject c with
      | Some n when decides_on_false eff ->
          place env ~cls:"negation" ~kind:"negated-characteristic" ~at [ ({ node = n; key = None }, false) ]
      | Some _ | None -> ())
  | Role_membership { fact_type; role; negated; _ } ->
      (* without a link the check is false, negated true *)
      let eff = if negated then flip pol else pol in
      if decides_on_false eff then
        place env ~cls:"absent-instance" ~kind:"negated-role-check" ~at
          (relations [ { rb_fact_type = fact_type; rb_role = role } ])
  | Identity_check _ -> ()
  (* reads the §9.5 channel of a rule, no member *)
  | Rule_status_check _ -> ()
  (* an executor faults where it meets the marker: no verdict to rest on leeg *)
  | Condition_unresolved _ -> ()
  | Time_window { starting_at; up_to; _ } ->
      (* a bound that is leeg puts no day in the window (ch. 3.10) *)
      let f = opt "from" starting_at in
      let t = opt "to" up_to in
      if decides_on_false pol then emit_carriers env ~cls:"negation" ~kind:"false-on-empty" ~at (f @ t)
  | Compound { quantifier_kind; components; _ } -> (
      let child =
        match quantifier_kind with
        | All | The | At_least -> pol
        | None_of | At_most -> flip pol
        | Exactly -> Both
      in
      match quantifier_kind with
      | All ->
          (* the members are read in order and the first that fails stops the
             rest (ch. 5.10), so a gevuld test guards the members after it *)
          let rec go i seen = function
            | [] -> ()
            | part :: rest ->
                co { env with tests = seen } child (Printf.sprintf "%s/parts/%d" at i) part;
                go (i + 1) (seen @ conjunct_keys part) rest
          in
          go 0 env.tests components
      | The | At_least | None_of | At_most | Exactly ->
          List.iteri (fun i part -> co env child (Printf.sprintf "%s/parts/%d" at i) part) components)
  | Quantifier_fold { kv_quantifier; kv_element_object_type; kv_filter; kv_test; kv_source_role; kv_source_path; _ } ->
      (* without a contributor `all`, `none` and `atMost` hold and `atLeast`
         and `exactly` fail, their threshold being at least 1 (WF-041) *)
      let vacuous =
        match kv_quantifier with All | The | None_of | At_most -> true | At_least | Exactly -> false
      in
      if (vacuous && decides_on_true pol) || ((not vacuous) && decides_on_false pol) then
        place env ~cls:"absent-instance" ~kind:"vacuous-quantifier" ~at
          (relations ((match kv_source_role with Some rb -> [ rb ] | None -> []) @ Option.value kv_source_path ~default:[]));
      (* a member the filter drops can make `all`, `none` and `atMost` hold,
         and only `atLeast` fail (ch. 3.9a) *)
      let filter_pol =
        match kv_quantifier with
        | At_least -> pol
        | All | The | None_of | At_most -> flip pol
        | Exactly -> Both
      in
      let test_pol =
        match kv_quantifier with
        | All | The | At_least -> pol
        | None_of | At_most -> flip pol
        | Exactly -> Both
      in
      let el = { env with subject = kv_element_object_type; tests = [] } in
      (match kv_filter with Some f -> co el filter_pol (sub "filter") f | None -> ());
      (match kv_test with
      | Some t -> co { el with tests = (match kv_filter with Some f -> conjunct_keys f | None -> []) } test_pol (sub "test") t
      | None -> ())

(* --- the forms ---------------------------------------------------------- *)

(* The subject type a reference names, and the member it writes. *)
let reference_subject (e : expr option) =
  match Checks.written_reference e with
  | Some (Attribute_reference { object_type; _ }) -> object_type
  | Some
      ( Literal _ | Text_composition _ | Binary _ | Function_call _ | Instance_set_union _ | Rounding _ | Clamp _
      | Dimension_cell_read _ | Parameter_ref _ | Variable_reference _ | Context_value _ | Bound_day _
      | Expr_unresolved _ | Temporal_read _ | Construct _ | Match _ | Binder_ref _
      | Struct_literal _ | Field_access _ | Let _ | List_literal _ | Lambda _ | If_then_else _ | Invoke _ | Fold _
      | Map_list _ | Filter_list _ | List_length _ )
  | None ->
      None

let target_node (r : resolved_rule) =
  match Checks.written_reference r.objective with
  | Some (Attribute_reference { attribute_ref = Some a; object_type = Some ot; _ }) -> (
      match (r.rr_presentation, Checks.written_labels r.objective) with
      | Some Characteristic_attribution, _ -> Some (Checks.n_char ot a)
      | _, Some ls -> Some (Checks.n_cell ot a ls)
      | _, None -> Some (Checks.n_attr ot a))
  | Some (Attribute_reference { attribute_ref = None; _ } | Attribute_reference { object_type = None; _ })
  | Some
      ( Literal _ | Text_composition _ | Binary _ | Function_call _ | Instance_set_union _ | Rounding _ | Clamp _
      | Dimension_cell_read _ | Parameter_ref _ | Variable_reference _ | Context_value _ | Bound_day _
      | Expr_unresolved _ | Temporal_read _ | Construct _ | Match _ | Binder_ref _
      | Struct_literal _ | Field_access _ | Let _ | List_literal _ | Lambda _ | If_then_else _ | Invoke _ | Fold _
      | Map_list _ | Filter_list _ | List_length _ )
  | None ->
      None

(* What a default (or a computing rule beside an initialisation) reads such
   that a leeg can keep it from deciding, so that the one below decides: the
   leaves of its condition that decide on leeg when read negatively, and the
   reads its value propagates (a leeg value falls through, ch. 7.1). A gevuld
   test among those leaves is the author's own closure, so its read counts as
   guarded. *)
let falls_through env ~at_condition ~at_value condition value =
  let buf = ref [] in
  (* the condition and value belong to another position: what they test is
     recorded where their own form is walked *)
  let scratch = { env with emit = (fun p -> buf := p :: !buf); record_test = (fun _ _ -> ()) } in
  (match condition with Some c -> co scratch Negative at_condition c | None -> ());
  let leaves =
    List.concat_map
      (fun (p : place) ->
        if List.mem p.kind leaf_kinds then
          if p.kind = "empty-test" then List.map (fun (c, _) -> (c, true)) p.members else p.members
        else [])
      (List.rev !buf)
  in
  let tests = match condition with Some c -> conjunct_keys c | None -> [] in
  let values = match value with Some e -> guarded { scratch with tests } (ex scratch at_value e) | None -> [] in
  leaves @ values

(* What the lower default needs itself: the reads whose leeg makes one of its
   own conjuncts false, the characteristics a conjunct needs to hold, and the
   reads its value propagates. Where the higher default falls through on one
   of these, the lower one does not decide either, so the read is no place of
   the fall-through. The frontend conjoins an exception's justification with
   the one of the rule it is an exception to (ch. 7.1); without this, every
   member of the base rule's own condition would be reported again. *)
let needs env condition value =
  let scratch = { env with emit = ignore; record_test = (fun _ _ -> ()) } in
  let side = function Some e -> ex scratch "" e | None -> [] in
  let rec conj (c : condition_expr) =
    match c with
    | Compound { quantifier_kind = All; components; _ } -> List.concat_map conj components
    | Comparison { op; left_side; right_side; time_period = None; _ } -> (
        let l = side left_side and r = side right_side in
        match op with
        | Equal when is_numeric env left_side right_side -> if l = [] || r = [] then l @ r else []
        | Equal | Less | Less_or_equal | Greater | Greater_or_equal | Earlier | Earlier_or_equal | Later
        | Later_or_equal ->
            l @ r
        | Not_equal -> [])
    | Predicate { variety = Filled; negation = false; operand; time_period = None; _ }
    | Predicate { variety = Empty_predicate; negation = true; operand; time_period = None; _ }
    | Predicate { variety = Exact_digits | Day_kind_predicate; negation = false; operand; time_period = None; _ } ->
        side operand
    | Characteristic_check { negation = false; time_period = None; anchor; _ } -> (
        let subject = match anchor with Some Anchor_form -> env.ctx.form | None -> env.subject in
        match characteristic_node env ~subject c with Some n -> [ { node = n; key = None } ] | None -> [])
    | Time_window { starting_at; up_to; _ } -> side starting_at @ side up_to
    | Comparison _ | Predicate _ | Characteristic_check _ | Compound _ | Role_membership _ | Quantifier_fold _
    | Identity_check _ | Rule_status_check _ | Condition_unresolved _ ->
        []
  in
  (match condition with Some c -> conj c | None -> []) @ side value

let without_needs env condition value reads =
  let own = needs env condition value in
  List.filter
    (fun ((c : carrier), _) ->
      not (List.exists (fun (n : carrier) -> match (c.key, n.key) with Some a, Some b -> a = b | None, None -> canon_node env.ctx.doc c.node = canon_node env.ctx.doc n.node | Some _, None | None, Some _ -> false) own))
    reads

let rule_form env (r : resolved_rule) ~same_target =
  let name = Option.value r.rr_name ~default:r.rr_pointer in
  let subject = reference_subject r.objective in
  let ctx = { env.ctx with form = subject } in
  let target = target_node r in
  let initialisation = r.rr_presentation = Some Initialisation in
  let form =
    { f_name = name; f_key = r.rr_pointer;
      f_chars = (match r.rr_condition with Some c -> conjunct_chars ctx ~subject c | None -> []);
      f_skip_target = (if initialisation then target else None) }
  in
  let env = { env with ctx; subject; tests = []; form } in
  (match (initialisation, target) with
  | true, Some n ->
      place env ~cls:"closure" ~kind:"initialisation" ~at:r.rr_pointer [ ({ node = n; key = None }, false) ];
      (* a computing rule on the same target that does not decide leaves it
         to the initialisation, as a default falls through (ch. 7.3) *)
      place env ~cls:"negation" ~kind:"fallthrough" ~at:r.rr_pointer
        (without_needs env r.rr_condition r.value_node @@ List.concat_map
           (fun (o : resolved_rule) ->
             let oenv = { env with subject = reference_subject o.objective;
                          ctx = { env.ctx with form = reference_subject o.objective };
                          emit = ignore } in
             falls_through oenv ~at_condition:(o.rr_pointer ^ "/condition") ~at_value:(o.rr_pointer ^ "/value")
               o.rr_condition o.value_node)
           same_target)
  | true, None | false, _ -> ());
  (match r.rr_condition with Some c -> co env Positive (r.rr_pointer ^ "/condition") c | None -> ());
  match r.value_node with
  | Some e ->
      let tests = match r.rr_condition with Some c -> conjunct_keys c | None -> [] in
      ignore (ex { env with tests } (r.rr_pointer ^ "/value") e : carrier list)
  | None -> ()

let defaults_form env i (l : prioritized_defaults) =
  let key = Printf.sprintf "/rules/prioritizedDefaults/%d" i in
  let name = Option.value l.pd_name ~default:key in
  let subject = Some l.pd_subject in
  let ctx = { env.ctx with form = subject } in
  let target =
    match (l.pd_attribute, l.pd_characteristic) with
    | Some a, _ -> Some (Checks.n_attr l.pd_subject a)
    | None, Some k -> Some (Checks.n_char l.pd_subject k)
    | None, None -> None
  in
  let form_of (d : default) =
    { f_name = name; f_key = key;
      f_chars = (match d.default_justification with Some c -> conjunct_chars ctx ~subject c | None -> []);
      f_skip_target = (if d.default_initialisation then target else None) }
  in
  (* walks one default at [prefix] below the defaults [higher] read, and
     returns what it reads for the defaults below it *)
  let rec default_walk prefix higher (d : default) =
    let env = { env with ctx; subject; tests = []; form = form_of d } in
    (match d.default_justification with Some c -> co env Positive (prefix ^ "/justification") c | None -> ());
    (match d.default_consequence with
    | Some e ->
        let tests = match d.default_justification with Some c -> conjunct_keys c | None -> [] in
        ignore (ex { env with tests } (prefix ^ "/consequence") e : carrier list)
    | None -> ());
    (match (d.default_initialisation, target) with
    | true, Some n -> place env ~cls:"closure" ~kind:"initialisation" ~at:prefix [ ({ node = n; key = None }, false) ]
    | true, None | false, _ -> ());
    (* the exception groups decide first and each sits below the defaults
       above this one (ch. 7.9) *)
    let below_exceptions =
      match d.default_exceptions with
      | Some egs ->
          List.concat
            (List.mapi
               (fun k (eg : exception_group) ->
                 chain (Printf.sprintf "%s/exceptions/members/%d" prefix k) higher eg.eg_defaults)
               egs.egs_members)
      | None -> []
    in
    let own =
      d.default_justification <> None || d.default_consequence <> None || d.default_decision <> None
      || d.default_initialisation
    in
    if own then begin
      place env ~cls:"negation" ~kind:"fallthrough" ~at:prefix
        (without_needs env d.default_justification d.default_consequence (higher @ below_exceptions));
      (* the default below is reached only when the exception groups decide
         nothing and this default's own content does not decide either
         (ch. 7.9), so it rests on both *)
      below_exceptions
      @ falls_through { env with emit = ignore } ~at_condition:(prefix ^ "/justification")
          ~at_value:(prefix ^ "/consequence") d.default_justification d.default_consequence
    end
    else below_exceptions
  and chain prefix higher defaults =
    let rec go j above acc = function
      | [] -> acc
      | d :: rest ->
          let reads = default_walk (Printf.sprintf "%s/defaults/%d" prefix j) above d in
          go (j + 1) (above @ reads) (acc @ reads) rest
    in
    go 0 higher [] defaults
  in
  ignore (chain key [] l.defaults : (carrier * bool) list)

let walk_forms env (doc : document) =
  List.iter
    (fun (r : resolved_rule) ->
      let same_target =
        match target_node r with
        | Some n when r.rr_presentation = Some Initialisation ->
            List.filter
              (fun (o : resolved_rule) ->
                target_node o = Some n
                && (o.rr_presentation = Some Equation || o.rr_presentation = Some Calculation))
              doc.resolved_rules
        | Some _ | None -> []
      in
      rule_form env r ~same_target)
    doc.resolved_rules;
  List.iteri
    (fun i (d : resolved_day_kind) ->
      let at = Printf.sprintf "/rules/dayKindDefinitions/%d" i in
      let form = { f_name = Option.value d.rd_name ~default:at; f_key = at; f_chars = []; f_skip_target = None } in
      match d.rd_condition with
      | Some c -> co { env with subject = None; tests = []; form } Positive (at ^ "/condition") c
      | None -> ())
    doc.resolved_day_kind_definitions;
  List.iter
    (fun (o : resolved_object_creation) ->
      let subject = o.oc_subject in
      let ctx = { env.ctx with form = subject } in
      let form =
        { f_name = Option.value o.oc_name ~default:o.oc_pointer; f_key = o.oc_pointer;
          f_chars = (match o.oc_condition with Some c -> conjunct_chars ctx ~subject c | None -> []);
          f_skip_target = None }
      in
      let env = { env with ctx; subject; tests = []; form } in
      (match o.oc_condition with Some c -> co env Positive (o.oc_pointer ^ "/condition") c | None -> ());
      let tests = match o.oc_condition with Some c -> conjunct_keys c | None -> [] in
      List.iteri
        (fun j (t : assignment) ->
          match t.to_value with
          | Some e -> ignore (ex { env with tests } (Printf.sprintf "%s/assignments/%d/value" o.oc_pointer j) e : carrier list)
          | None -> ())
        o.assignments)
    doc.object_creations;
  List.iter
    (fun (v : resolved_allocation) ->
      let subject = v.source_type in
      let ctx = { env.ctx with form = subject } in
      let form =
        { f_name = Option.value v.vd_name ~default:v.vd_pointer; f_key = v.vd_pointer;
          f_chars = (match v.vd_condition with Some c -> conjunct_chars ctx ~subject c | None -> []);
          f_skip_target = None }
      in
      let env = { env with ctx; subject; tests = []; form } in
      (match v.vd_condition with Some c -> co env Positive (v.vd_pointer ^ "/condition") c | None -> ());
      (* a leeg weight is left out or counts as 0, a leeg cap is no cap, a leeg
         ordering key sorts last (ch. 4.4): each decides the shares *)
      let dt = Option.value v.objective_type_name ~default:"" in
      List.iter
        (fun (field, a) ->
          match a with
          | Some a ->
              place env ~cls:"closure" ~kind:"allocation-empty-input" ~at:(v.vd_pointer ^ "/" ^ field)
                [ ({ node = Checks.n_attr dt a; key = None }, false) ]
          | None -> ())
        [ ("ratioAttribute", v.ratio_attribute); ("maximumAttribute", v.maximum_attribute);
          ("orderingAttribute", v.ordering_attribute) ])
    doc.allocations;
  List.iter
    (fun (c : resolved_consistency) ->
      let subject = c.cons_subject in
      let ctx = { env.ctx with form = subject } in
      let form =
        { f_name = Option.value c.cons_name ~default:c.cons_pointer; f_key = c.cons_pointer;
          f_chars = []; f_skip_target = None }
      in
      (* the form reports when its assertion is false *)
      match c.assertion with
      | Some a -> co { env with ctx; subject; tests = []; form } Negative (c.cons_pointer ^ "/assertion") a
      | None -> ())
    doc.consistencies;
  List.iter
    (fun (f : resolved_fact_creation) ->
      let subject = f.anchor_point in
      let ctx = { env.ctx with form = subject } in
      let form =
        { f_name = Option.value f.fc_name ~default:f.fc_pointer; f_key = f.fc_pointer;
          f_chars = (match f.fc_condition with Some c -> conjunct_chars ctx ~subject c | None -> []);
          f_skip_target = None }
      in
      match f.fc_condition with
      | Some c -> co { env with ctx; subject; tests = []; form } Positive (f.fc_pointer ^ "/condition") c
      | None -> ())
    doc.fact_creations;
  List.iteri (fun i l -> defaults_form env i l) doc.prioritized_defaults

(* --- the report --------------------------------------------------------- *)

(* The nodes the input closure follows: the members for the classes closure
   and negation; for an absent instance also the relations and populations,
   which a creation writes. *)
let follows ~instances node =
  starts "attr:" node || starts "char:" node || (instances && (starts "rel:" node || starts "pop:" node))

(* The graph keys a relation by its fact type alone: the node of a relation
   member without its role. *)
let graph_node node =
  if starts "rel:" node then
    match String.index_from_opt node 4 ':' with Some i -> String.sub node 0 i | None -> node
  else node

(* The JSON value at a pointer, None where it does not resolve. *)
let resolve (j : Yojson.Safe.t) at =
  let segs = match String.split_on_char '/' at with "" :: rest -> rest | _ -> [] in
  List.fold_left
    (fun cur seg ->
      match cur with
      | Some (`Assoc kvs) -> List.assoc_opt seg kvs
      | Some (`List xs) -> ( match int_of_string_opt seg with Some n when n >= 0 && n < List.length xs -> Some (List.nth xs n) | Some _ | None -> None)
      | Some _ | None -> None)
    (Some j) segs

let parent at = match String.rindex_opt at '/' with Some 0 | None -> "" | Some i -> String.sub at 0 i

(* The nearest node at or above [at] whose source record carries [field]. *)
let rec source_field j at field =
  let here =
    match resolve j at with
    | Some (`Assoc kvs) -> (
        match List.assoc_opt "source" kvs with
        | Some (`Assoc s) -> List.assoc_opt field s
        | Some _ | None -> None)
    | Some _ | None -> None
  in
  match here with Some v -> Some v | None -> if at = "" then None else source_field j (parent at) field

let one_line s =
  String.concat " " (List.filter (fun w -> w <> "") (String.split_on_char ' ' (String.map (function '\n' | '\r' | '\t' -> ' ' | c -> c) s)))

(* Document order: the index of every pointer in a pre-order walk. *)
let document_order (j : Yojson.Safe.t) =
  let tbl = Hashtbl.create 4096 in
  let n = ref 0 in
  let rec walk at v =
    Hashtbl.replace tbl at !n;
    incr n;
    match v with
    | `Assoc kvs -> List.iter (fun (k, x) -> walk (at ^ "/" ^ k) x) kvs
    | `List xs -> List.iteri (fun i x -> walk (at ^ "/" ^ string_of_int i) x) xs
    | _ -> ()
  in
  walk "" j;
  tbl

let dedup l =
  List.rev (List.fold_left (fun acc x -> if List.mem x acc then acc else x :: acc) [] l)

let report (doc : document) (j : Yojson.Safe.t) : string list =
  let ctx = Checks.ctx_of doc in
  let forms = Array.of_list (Checks.dep_forms ctx) in
  let writers = Hashtbl.create 64 in
  let canon = canon_node doc in
  Array.iteri (fun i (f : Checks.dep_form) -> List.iter (fun n -> Hashtbl.add writers (canon n) i) f.df_writes) forms;
  let index_of_key = Hashtbl.create 64 in
  Array.iteri (fun i (f : Checks.dep_form) -> Hashtbl.replace index_of_key (Checks.m1_root f.df_at) i) forms;
  let tested = Hashtbl.create 64 in
  let record_test key nodes = List.iter (fun n -> Hashtbl.replace tested (key, canon n) ()) nodes in
  let places = ref [] in
  let env =
    { ctx; subject = None; tests = [];
      form = { f_name = ""; f_key = ""; f_chars = []; f_skip_target = None };
      emit = (fun p -> places := p :: !places); record_test }
  in
  walk_forms env doc;
  (* the input members behind a member: itself where no form writes it, else
     what its writers read, transitively; a writer is skipped where the place
     is that writer's own guard of its target *)
  let closure ~instances ?skip node =
    let seen = Hashtbl.create 16 and inputs = ref [] in
    let rec go node =
      if not (Hashtbl.mem seen node) then begin
        Hashtbl.add seen node ();
        let ws = List.filter (fun w -> Some w <> skip) (Hashtbl.find_all writers node) in
        if ws = [] then inputs := node :: !inputs
        else
          List.iter
            (fun w -> List.iter (fun (r, _) -> if follows ~instances r then go (canon r)) forms.(w).Checks.df_reads)
            (dedup ws)
      end
    in
    go node;
    !inputs
  in
  (* the forms that close on the leeg of a member, and the characteristics a
     form writes *)
  let testing_forms = Hashtbl.create 64 and testers = Hashtbl.create 64 in
  Hashtbl.iter
    (fun (key, node) () ->
      Hashtbl.replace testing_forms key ();
      match Hashtbl.find_opt index_of_key key with Some w -> Hashtbl.add testers node w | None -> ())
    tested;
  (* the characteristic a form writes, as it writes it: the dependency graph
     also keys the write on every comparable type, which would name one
     characteristic once per type of its chain *)
  let primary = Hashtbl.create 64 in
  List.iter
    (fun (r : resolved_rule) ->
      match (r.rr_presentation, target_node r) with
      | Some Characteristic_attribution, Some n -> Hashtbl.add primary r.rr_pointer (canon n)
      | _ -> ())
    doc.resolved_rules;
  List.iteri
    (fun i (l : prioritized_defaults) ->
      match l.pd_characteristic with
      | Some k -> Hashtbl.add primary (Printf.sprintf "/rules/prioritizedDefaults/%d" i) (canon (Checks.n_char l.pd_subject k))
      | None -> ())
    doc.prioritized_defaults;
  List.iter
    (fun (o : resolved_object_creation) ->
      List.iter
        (fun (t : assignment) ->
          match (t.to_characteristic, t.to_attribute, o.created_type) with
          | true, Some a, Some ct -> Hashtbl.add primary o.oc_pointer (canon (Checks.n_char ct a))
          | _ -> ())
        o.assignments)
    doc.object_creations;
  let char_writes w = List.rev (Hashtbl.find_all primary (Checks.m1_root forms.(w).Checks.df_at)) in
  let tests_leeg w = Hashtbl.mem testing_forms (Checks.m1_root forms.(w).Checks.df_at) in
  let order = document_order j in
  let kind_rank k =
    let rec find i = function [] -> List.length kinds | (_, x) :: rest -> if x = k then i else find (i + 1) rest in
    find 0 kinds
  in
  let lines =
    List.map
      (fun (p : place) ->
        let own = Hashtbl.find_opt index_of_key p.form.f_key in
        let skip_for n = if Option.map canon p.form.f_skip_target = Some n then own else None in
        let members_c = List.map (fun ((c : carrier), g) -> (canon c.node, g)) p.members in
        let nodes = dedup (List.map fst members_c) in
        let derived n = List.exists (fun w -> Some w <> skip_for n) (Hashtbl.find_all writers (graph_node n)) in
        let members =
          List.map (fun n -> n ^ "=" ^ if derived n then "derived" else "input") nodes
        in
        let instances = p.cls = "absent-instance" in
        let inputs =
          List.sort_uniq compare
            (List.concat_map
               (fun n -> if derived n then closure ~instances ?skip:(skip_for n) (graph_node n) else [ n ])
               nodes)
        in
        let filled =
          List.filter_map
            (fun n ->
              if List.for_all (fun (c, g) -> c <> n || g) members_c then Some ("filled-test:" ^ n) else None)
            nodes
        in
        let char_guards =
          List.filter_map
            (fun k ->
              if List.exists tests_leeg (Hashtbl.find_all writers k) then Some ("characteristic-check:" ^ k) else None)
            (* a check is no guard of itself *)
            (List.filter (fun k -> not (List.mem k nodes)) (dedup (List.map canon p.form.f_chars)))
        in
        let elsewhere =
          List.concat_map
            (fun i ->
              let ks =
                List.concat_map (fun w -> if Some w <> own then char_writes w else []) (dedup (Hashtbl.find_all testers i))
              in
              match List.sort_uniq compare ks with
              | [] -> []
              | ks -> [ "tested-elsewhere:" ^ i ^ ">" ^ String.concat "," ks ])
            inputs
        in
        let line =
          match source_field j p.at "line" with Some (`Int n) -> string_of_int n | Some _ | None -> "-"
        in
        let snippet =
          match source_field j p.at "snippet" with Some (`String s) -> one_line s | Some _ | None -> "-"
        in
        let col = function [] -> "-" | xs -> String.concat "; " xs in
        let text =
          String.concat "\t"
            [ p.cls; p.kind; p.at; p.form.f_name; col members; col inputs; col (filled @ char_guards @ elsewhere); line;
              snippet ]
        in
        let position =
          match Hashtbl.find_opt order p.at with
          | Some n -> n
          | None -> failwith ("Empty_closures.report: the place " ^ p.kind ^ " at " ^ p.at ^ " is no pointer of the document")
        in
        ((position, kind_rank p.kind), text))
      !places
  in
  List.map snd (List.sort_uniq compare lines)
