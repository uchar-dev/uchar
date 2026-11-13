(* Serialiser: [Ast] values -> uchar-ir-v1 wire JSON, in its L5 shape.

   The OCaml frontends (Catala, F-logic) build typed [Ast] values and hand
   them to this one serialiser, so schema conformity is structural: the
   mapping code never assembles JSON by hand. It is the inverse of
   [Loader]: every key name and node discriminator mirrors the loader's
   tables, and every enumeration value is spelled by the one translation
   table the loader reads with ([Spelling]), so a value that round-trips
   through the loader is what is written. The key order is the corpus's
   own; gates 9 and 14 hold every golden a frontend writes byte-identical
   after canonicalisation (gate 2's serialiser).

   It writes the forms the frontends build, and refuses with [Failure] what
   it does not write rather than emitting a guessed shape or dropping a
   filled list without a word. [Make] takes the producer's name, which the
   refusals carry. *)

open Ast

module type PRODUCER = sig
  val producer : string
  (* the name a refusal gives the frontend: "a node not produced by the
     <producer>" *)
end

module Make (P : PRODUCER) = struct
  type json = Yojson.Safe.t

  let p = P.producer
  let s (x : string) : json = `String x
  let so : string option -> json = function None -> `Null | Some x -> `String x
  let b (x : bool) : json = `Bool x
  let i (x : int) : json = `Int x
  let opt (f : 'a -> json) : 'a option -> json = function None -> `Null | Some x -> f x
  let str_opt_list (xs : string option list) : json = `List (List.map so xs)

  (* The source record (delta B26): a node with source text writes it as
     {"snippet"} under "source", at the place v0 wrote "bron"; a node without
     source text writes no field and never a null, so [fields] and [node]
     leave the placeholder of an absent record out. An empty snippet has no
     reading in any reader of the wire, so it is refused here in the loader's
     sentence. No line is written: a consistency's in-memory line is the
     order of its assertion in the source, not a line of a source file. *)
  let source (br : string option) : string * json =
    match br with
    | Some "" -> failwith ("Emit: " ^ Loader.empty_snippet_refusal)
    | Some x -> ("source", `Assoc [ ("snippet", `String x) ])
    | None -> ("source", `Null)

  let fields (kvs : (string * json) list) : (string * json) list =
    List.filter (fun (k, v) -> not (k = "source" && v = `Null)) kvs

  let node k rest : json = `Assoc (("node", `String k) :: fields rest)

  (* A field IR-SPEC calls optional is written whole when present and left
     out otherwise: since the L5 landing no such field carries a null. *)
  let optional (k : string) (f : 'a -> json) : 'a option -> (string * json) list = function
    | None -> []
    | Some x -> [ (k, f x) ]

  (* An enumeration value, spelled by its family in the translation table;
     [Spelling.encode] refuses a constructor the family does not spell. *)
  let enum (fam : 'a Spelling.family) (c : 'a) : json = s (Spelling.encode fam c)

  let no_anchor = function
    | None -> ()
    | Some _ -> failwith (Printf.sprintf "Emit: the %s writes no form anchor" p)

  (* the two folds over linked instances, both an Aggregation on the wire
     (delta B21); a builtin call is another node, never written as one *)
  let aggregation_kind = function
    | (Sum_function | Count) as k -> enum Spelling.aggregation_kind k
    | _ -> failwith (Printf.sprintf "Emit: aggregation kind not produced by the %s" p)

  (* the built-in '%' of a percentage that names no unit of its own *)
  let builtin_percent : measure_unit =
    [ { abbr_text = Some "%"; system = None; canonical_form = Some "%";
        factor_to_canonical = Some { num = 1; den = 1 }; exponent = None } ]

  (* The type system in its L5 shape: a Number always carries its sign (a
     sign the in-memory form leaves open is unspecified) and its unit only
     when it has one; a Percentage is a Number with the unit '%' — the
     built-in one when the in-memory form carries none — and the
     presentation tag at a declaration position only ([~decl]; an
     expression's own type is an annotation, where the tag is never
     written); a date is one Date node with its precision. *)
  let rec datatype ?(decl = true) (d : datatype) : json =
    match d with
    | Number { whole_part; decimal_places; sign_char; measure_unit } ->
        node "Number"
          ([ ("whole", b whole_part); ("decimals", i decimal_places);
             ("sign", enum Spelling.sign (Option.value sign_char ~default:Unspecified)) ]
          @ match measure_unit with None -> [] | Some e -> [ ("unit", unit_j e) ])
    | Boolean -> node "Boolean" []
    | Date_in_days | Date_time_type -> node "Date" [ ("precision", enum Spelling.precision d) ]
    | Text_type -> node "Text" []
    | Percentage { whole_part; decimal_places; measure_unit } ->
        node "Number"
          ([ ("whole", b whole_part); ("decimals", i decimal_places); ("sign", enum Spelling.sign Unspecified);
             ("unit", unit_j (Option.value measure_unit ~default:builtin_percent)) ]
          @ if decl then [ ("presentation", s "percentage") ] else [])
    | Enumeration { domain_spec; label_list } ->
        node "Enumeration" [ ("domain", so domain_spec); ("values", str_opt_list label_list) ]
    | Sum_type { domain_spec; constructor_list } ->
        node "SumType"
          [ ("domain", s domain_spec);
            ("constructors",
             `List
               (List.map
                  (fun (nm, payload) ->
                    `Assoc [ ("name", s nm); ("payload", opt (datatype ~decl) payload) ])
                  constructor_list)) ]
    | List_type { element } -> node "ListType" [ ("element", datatype ~decl element) ]
    | Struct_type { domain_spec; field_list } ->
        node "StructType"
          [ ("domain", s domain_spec);
            ("fields",
             `List
               (List.map
                  (fun (nm, dt) -> `Assoc [ ("name", s nm); ("datatype", datatype ~decl dt) ])
                  field_list)) ]

  (* a Unit where it occurs: absent is the one spelling of a field not
     given, and the factor is the exact rational {num, den} *)
  and unit_j (u : measure_unit) : json = `List (List.map factor_j u)

  and factor_j (e : unit_factor) : json =
    let abbreviation =
      match e.abbr_text with Some a -> a | None -> failwith "Emit: a unit without an abbreviation"
    in
    node "Unit"
      ([ ("abbreviation", s abbreviation) ]
      @ (match e.system with Some x -> [ ("system", s x) ] | None -> [])
      @ (match e.canonical_form with Some x -> [ ("canonical", s x) ] | None -> [])
      @ (match e.factor_to_canonical with
        | Some { num; den } -> [ ("factorToCanonical", `Assoc [ ("num", i num); ("den", i den) ]) ]
        | None -> [])
      @ match e.exponent with Some x -> [ ("exponent", i x) ] | None -> [])

  (* an expression's own type: never a declaration position *)
  let annotation (d : datatype) : json = datatype ~decl:false d

  (* the symbolic role binding (IR-SPEC ch. 2.6): a whole-or-nothing pair of
     plain strings — the back-link of an object creation, the hop of a
     navigated read and the range of a fold over linked instances *)
  let role_binding (rb : role_binding) : json =
    node "RoleBinding" [ ("factType", s rb.rb_fact_type); ("role", s rb.rb_role) ]

  (* The typed value of a Literal (IR-SPEC ch. 4.1, delta B22): one key, the
     variant the literal's type names. *)
  let literal_value (v : literal_variant) : json =
    match v with
    | Lv_number x -> `Assoc [ ("number", s x) ]
    | Lv_boolean x -> `Assoc [ ("boolean", b x) ]
    | Lv_text x -> `Assoc [ ("text", s x) ]
    | Lv_date x -> `Assoc [ ("date", s x) ]
    | Lv_date_time x -> `Assoc [ ("dateTime", s x) ]
    | Lv_enumeration x -> `Assoc [ ("enumeration", s x) ]
    | Lv_constructor x -> `Assoc [ ("constructor", s x) ]

  let not_produced what = failwith (Printf.sprintf "Emit.expr: expression node not produced by the %s: %s" p what)

  let rec expr (e : expr) : json =
    let typ t = ("type", opt annotation t) in
    match e with
    | Literal { variant; typ = t; source_name = br } ->
        node "Literal" [ ("value", literal_value variant); typ t; source br ]
    | Binary { op; left_side; right_side; typ = t; source_name = br } ->
        node "Binary"
          [ ("op", enum Spelling.binary_op op); ("left", opt expr left_side);
            ("right", opt expr right_side); typ t; source br ]
    | Rounding { base_expr; mode_kind; decimal_places; typ = t; source_name = br } ->
        node "Rounding"
          [ ("base", opt expr base_expr); ("mode", enum Spelling.rounding_mode mode_kind);
            ("decimals", i decimal_places); typ t; source br ]
    | Attribute_reference { attribute_ref; object_type; source_name = br; navigation_bindings = nb; anchor } ->
        (* a reference writes no type (B13). path is written only when the
           reference navigates (a caller reads a callee output over the
           back-link); without it the read is the subject's own (IR-SPEC
           ch. 4.1a) *)
        no_anchor anchor;
        node "AttributeReference"
          ([ ("attribute", so attribute_ref); ("subjectType", so object_type); source br ]
          @ optional "path" (fun hops -> `List (List.map role_binding hops)) nb)
    (* the two context nodes (IR-SPEC ch. 4.1, delta B23): a session value
       carries its name and its datatype, the bound day of a day-kind
       definition its datatype. A name outside the closed set is refused in
       WF-098's sentence rather than written for the verifier to reject *)
    | Context_value { ident; typ = t; source_name = br } ->
        let name =
          match ident with
          | Some n -> (
              match Checks.context_value_refusal n with
              | None -> n
              | Some sentence -> failwith ("Emit.expr: " ^ sentence))
          | None -> failwith "Emit.expr: a ContextValue carries its name as a string"
        in
        let t =
          match t with Some t -> t | None -> failwith "Emit.expr: a ContextValue carries its datatype"
        in
        node "ContextValue" [ ("name", s name); ("type", annotation t); source br ]
    | Bound_day { typ = t; source_name = br } ->
        let t = match t with Some t -> t | None -> failwith "Emit.expr: a BoundDay carries its datatype" in
        node "BoundDay" [ ("type", annotation t); source br ]
    (* A written marker would need a warning quoting its source snippet in
       [diagnostics] (WF-113 (b)), which this serialiser never writes: it
       refuses like any node it does not write. *)
    | Expr_unresolved _ -> not_produced "Unresolved"
    (* a fold over linked instances: the range is written whole on every one
       and never left to be derived (B12); an absent optional field is left
       out (IR-SPEC ch. 4.3) *)
    | Function_call
        { kind; call_arguments; element_object_type; empty_fallback; filter; dimension_selections;
          typ = t; source_name = br; grain; source_role; source_path; anchor } ->
        if filter <> None || dimension_selections <> None || grain <> None || source_path <> None then
          failwith (Printf.sprintf "Emit: aggregation qualifier not produced by the %s" p);
        no_anchor anchor;
        let rb =
          match source_role with
          | Some rb -> rb
          | None -> failwith "Emit: an instance fold without its role binding"
        in
        node "Aggregation"
          ([ ("kind", aggregation_kind kind); ("arguments", `List (List.map expr call_arguments)) ]
          @ (match element_object_type with None -> [] | Some el -> [ ("elementType", s el) ])
          @ (match empty_fallback with None -> [] | Some f -> [ ("emptyFallback", expr f) ])
          @ [ ("sourceRole", role_binding rb); typ t; source br ])
    | Construct { typ = t; constructor_; argument; source_name = br } ->
        node "Construct"
          [ ("type", opt annotation t); ("constructor", s constructor_);
            ("argument", opt expr argument); source br ]
    | Match { typ = t; scrutinee; arms; source_name = br } ->
        node "Match"
          [ ("type", opt annotation t); ("scrutinee", expr scrutinee);
            ("arms",
             `List
               (List.map
                  (fun (c, binder, body) ->
                    `Assoc [ ("constructor", s c); ("binder", so binder); ("body", expr body) ])
                  arms));
            source br ]
    | Binder_ref { ident; source_name = br } -> node "BinderRef" [ ("name", s ident); source br ]
    | List_literal { typ = t; elements; source_name = br } ->
        node "ListLiteral" [ ("type", opt annotation t); ("elements", `List (List.map expr elements)); source br ]
    | If_then_else { typ = t; condition; then_; else_; source_name = br } ->
        node "IfThenElse"
          [ ("type", opt annotation t); ("condition", condition_expr condition);
            ("then", expr then_); ("else", expr else_); source br ]
    | Invoke { typ = t; function_name; arguments; source_name = br } ->
        node "Invoke"
          [ ("type", opt annotation t); ("function", s function_name);
            ("arguments", `List (List.map expr arguments)); source br ]
    | Fold { typ = t; list; function_name; init; source_name = br } ->
        node "Fold"
          [ ("type", opt annotation t); ("list", expr list);
            ("function", fold_fn function_name); ("init", expr init); source br ]
    | Map_list { typ = t; list; function_name; source_name = br } ->
        node "MapList"
          [ ("type", opt annotation t); ("list", expr list); ("function", fold_fn function_name); source br ]
    | Filter_list { typ = t; list; binder; condition; source_name = br } ->
        node "FilterList"
          [ ("type", opt annotation t); ("list", expr list); ("binder", s binder);
            ("condition", condition_expr condition); source br ]
    | List_length { typ = t; list; source_name = br } ->
        node "ListLength" [ ("type", opt annotation t); ("list", expr list); source br ]
    | Struct_literal { typ = t; field_list; source_name = br } ->
        node "StructLiteral"
          [ ("type", opt annotation t);
            ("fields",
             `List (List.map (fun (nm, v) -> `Assoc [ ("name", s nm); ("value", expr v) ]) field_list));
            source br ]
    | Field_access { typ = t; base; field_name; source_name = br } ->
        node "FieldAccess" [ ("type", opt annotation t); ("base", expr base); ("field", s field_name); source br ]
    | Let { typ = t; binder; value; body; source_name = br } ->
        (* the value-level binding (IR-SPEC ch. 4.10): [type] is non-null on
           the wire and equals the body type (WF-076); [binder] joins the
           WF-064 regime *)
        node "Let"
          [ ("type", opt annotation t); ("binder", s binder); ("value", expr value);
            ("body", expr body); source br ]
    (* No frontend builds a ParameterReference or a VariableReference: none
       writes parameters, so a parameter reference would resolve against no
       declaration (WF-034), and a variable read has no executor that
       carries it. Written, either would reach the verifier or an executor
       as a refusal; the serialiser refuses first, naming the node. *)
    | Parameter_ref _ -> not_produced "ParameterReference"
    | Variable_reference _ -> not_produced "VariableReference"
    | Text_composition _ -> not_produced "TextComposition"
    | Clamp _ -> not_produced "Clamp"
    | Dimension_cell_read _ -> not_produced "DimensionCellRead"
    | Temporal_read _ -> not_produced "TemporalRead"
    | Lambda _ -> not_produced "Lambda (position-restricted; only a Fold/MapList operand)"
    | _ -> not_produced "?"

  (* the Fold/MapList [function] operand: an inline Lambda object or a bare
     FunctionDef-name string (the two wire forms of IR-SPEC ch. 4.8). *)
  and fold_fn (f : fold_fn) : json =
    match f with
    | Fn_name n -> `String n
    | Fn_lambda { parameters; body; source_name = br } ->
        node "Lambda" [ ("parameters", param_list parameters); ("body", expr body); source br ]

  (* Lambda / FunctionDef parameters: a name and a datatype, both required. *)
  and param_list (ps : (string * datatype) list) : json =
    `List (List.map (fun (n, dt) -> `Assoc [ ("name", s n); ("datatype", datatype dt) ]) ps)

  and condition_expr (c : condition_expr) : json =
    let not_produced what =
      failwith (Printf.sprintf "Emit.condition: condition shape not produced by the %s%s" p what)
    in
    (* no frontend declares a time-dependent attribute or characteristic for
       a whole-period qualifier to qualify (WF-043), so a filled one refuses
       rather than being written *)
    let whole_period = function
      | None -> []
      | Some (_ : entire_period) -> not_produced ": a wholePeriod qualifier"
    in
    match c with
    | Comparison { op; left_side; right_side; time_period = tp; source_name = br } ->
        node "Comparison"
          ([ ("op", enum Spelling.comparison_op op); ("left", opt expr left_side);
             ("right", opt expr right_side) ]
          @ whole_period tp @ [ source br ])
    | Compound { quantifier_kind; cutoff; components; source_name = br } ->
        node "Compound"
          [ ("quantifier", enum Spelling.quantifier quantifier_kind); ("threshold", i cutoff);
            ("parts", `List (List.map condition_expr components)); source br ]
    | Characteristic_check
        { characteristic; negation; holder; time_period = tp; source_name = br; navigation_binding = nb; anchor } ->
        no_anchor anchor;
        (* the host is written only when the check names one (absent is the
           subject, RUNTIME-SEMANTICS ch. 5.8b); navigation is written whole
           or not at all, only when the check navigates (ch. 4.1a/5: a caller
           reads a callee characteristic over the synthesised back-link); it
           is the hop *)
        node "CharacteristicCheck"
          ([ ("characteristic", so characteristic); ("negated", b negation) ]
          @ optional "host" s holder @ whole_period tp @ [ source br ]
          @ optional "navigation" role_binding nb)
    (* The quantifier fold. [threshold] is present EXACTLY under the three
       counting quantifiers (WF-108 (e)) and absent otherwise, so it is
       written conditionally rather than as a null nobody may consult; the
       subselection is absent where there is none; the range is the one-step
       [sourceRole]. *)
    | Quantifier_fold
        { kv_quantifier; kv_threshold; kv_element_object_type; kv_filter; kv_test; kv_source;
          kv_source_role; kv_source_path; kv_anchor } ->
        if kv_source_path <> None then
          failwith (Printf.sprintf "Emit: the %s writes no multi-step quantifier range" p);
        no_anchor kv_anchor;
        let rb =
          match kv_source_role with
          | Some rb -> rb
          | None -> failwith "Emit: a quantifier fold without its role binding"
        in
        node "QuantifierFold"
          ([ ("quantifier", enum Spelling.quantifier kv_quantifier) ]
          @ optional "threshold" i kv_threshold
          @ [ ("elementType", so kv_element_object_type) ]
          @ optional "filter" condition_expr kv_filter
          @ [ ("test", opt condition_expr kv_test); source kv_source; ("sourceRole", role_binding rb) ])
    (* no marker either: see the expression arm *)
    | Condition_unresolved _ -> not_produced ": Unresolved"
    | _ -> not_produced ""

  (* an open side of a validity is an absent bound; neither bound is always *)
  let validity_window (g : validity_window) : json =
    node "Validity" (optional "from" s g.valid_from @ optional "to" s g.valid_to)

  (* A list entry of the M1 model carries its name once, on its node, so the
     in-memory map key and the node's own name must be one spelling; two
     spellings have no rule for choosing, so a mismatch refuses. *)
  let named what key (ident : string option) =
    if ident <> Some key then
      failwith
        (Printf.sprintf "Emit: %s %S carries the name %s; the uchar-ir-v1 list entry has one" what key
           (match ident with None -> "null" | Some n -> Printf.sprintf "%S" n))

  (* ObjectType with its members as nodes (IR-SPEC ch. 2.2). No frontend
     fills a member facet: no plural, dimension or timeline table. A filled
     one refuses rather than being written in a guessed form. *)
  let object_type (key, (o : object_type)) : json =
    named "object type" key o.ot_ident;
    let unfilled table = function
      | [] -> ()
      | _ -> failwith (Printf.sprintf "Emit: object type %S fills %s, which the %s never builds" key table p)
    in
    unfilled "attribuut_meervoud" o.attribute_plural;
    unfilled "attribuut_dimensies" o.attribute_dimensions;
    unfilled "attribuut_granulariteit" o.attribute_granularity;
    unfilled "attribuut_tijdlijn_aantal" o.attribute_timeline_count;
    unfilled "attribuut_tijdlijn_naam" o.attribute_timeline_names;
    unfilled "kenmerk_granulariteit" o.characteristic_granularity;
    unfilled "kenmerk_tijdlijn_aantal" o.characteristic_timeline_count;
    let characteristics =
      List.map
        (function
          | Some k -> k
          | None -> failwith (Printf.sprintf "Emit: object type %S declares a null characteristic" key))
        o.characteristics
    in
    List.iter
      (fun (k, _) ->
        if not (List.mem k characteristics) then
          failwith (Printf.sprintf "Emit: object type %S tags %S, which it does not declare" key k))
      o.characteristic_kinds;
    let supertype = match o.ot_supertype with None -> [] | Some st -> [ ("supertype", s st) ] in
    let plural = match o.plural with None -> [] | Some pl -> [ ("plural", s pl) ] in
    node "ObjectType"
      ([ ("name", s key) ] @ supertype @ [ ("animate", b o.animate) ] @ plural
      @ [ ("attributes",
           `List
             (List.map
                (fun (a, dt) -> node "Attribute" [ ("name", s a); ("datatype", opt datatype dt) ])
                o.attributes));
          ("characteristics",
           `List
             (List.map
                (fun k ->
                  (* untagged is nominal (ch. 2.2) *)
                  let kind =
                    match List.assoc_opt k o.characteristic_kinds with
                    | Some (Some variety) -> variety
                    | Some None | None -> Nominal
                  in
                  node "Characteristic" [ ("name", s k); ("kind", enum Spelling.characteristic_kind kind) ])
                characteristics)) ])

  (* Domain (IR-SPEC ch. 2.3): the name is written here, so the datatype at
     this declaration position drops the self-name it carries at every
     reference position. *)
  let domain (key, (d : datatype option)) : json =
    let dt = match d with Some dt -> dt | None -> failwith (Printf.sprintf "Emit: domain %S has no datatype" key) in
    let self_name =
      match dt with
      | Enumeration { domain_spec; _ } -> Some domain_spec
      | Sum_type { domain_spec; _ } | Struct_type { domain_spec; _ } -> Some (Some domain_spec)
      | _ -> None
    in
    let body =
      match (self_name, datatype dt) with
      | None, j -> j
      | Some ident, `Assoc kvs ->
          named "domain" key ident;
          `Assoc (List.filter (fun (k, _) -> k <> "domain") kvs)
      | Some _, _ -> failwith (Printf.sprintf "Emit: domain %S did not serialise as an object" key)
    in
    node "Domain" [ ("name", s key); ("datatype", body) ]

  (* A version's validity is always written (IR-SPEC ch. 3.10): a record
     without one is valid always. *)
  let validity (g : validity_window option) : json =
    validity_window (Option.value g ~default:{ valid_from = None; valid_to = None })

  (* A named form (IR-SPEC ch. 3, Versioning): {node, name, versions}. The
     frontends build every rule, consistency and object creation with one
     version, so each in-memory record is one form. A record name ending in
     a space, '#' and digits is the in-memory spelling of one version of a
     form with several, which no frontend builds: it refuses rather than
     writing a name no reader loads. *)
  let form ~what kind (ident : string option) (version : json) : json =
    (match ident with
    | Some n when Loader.versioned_name n ->
        failwith
          (Printf.sprintf
             "Emit: the %s name %S ends in a space, '#' and digits, the in-memory spelling of one \
              version of a form with several, and the %s builds no versions"
             what n p)
    | _ -> ());
    node kind [ ("name", so ident); ("versions", `List [ version ]) ]

  (* A rule version (IR-SPEC ch. 3.1, delta B20): the in-memory kind is read
     back onto the initialisation flag and the presentation tag, and the
     target is an attribute reference. *)
  let rule (r : resolved_rule) : json =
    if r.rr_source_annotations <> None then
      failwith (Printf.sprintf "Emit: the %s writes no statute annotations" p);
    let initialisation, presentation =
      match r.rr_presentation with
      | Some ((Equation | Calculation) as k) -> (false, [ ("presentation", enum Spelling.presentation k) ])
      | Some Initialisation -> (true, [])
      | Some Characteristic_attribution -> (false, [])
      | None -> failwith "Emit: a rule version without its kind"
    in
    let target =
      match r.objective with
      | Some (Attribute_reference _ as t) -> expr t
      | _ -> failwith "Emit: the target of a rule version is an AttributeReference"
    in
    let value =
      match r.value_node with Some v -> expr v | None -> failwith "Emit: a rule version without its value"
    in
    let condition = match r.rr_condition with None -> [] | Some c -> [ ("condition", condition_expr c) ] in
    form ~what:"rule" "Rule" r.rr_name
      (`Assoc
        ([ ("validity", validity r.rr_validity); ("target", target); ("value", value) ]
        @ condition
        @ [ ("initialisation", b initialisation) ]
        @ presentation))

  let consistency (c : resolved_consistency) : json =
    let assertion =
      match c.assertion with
      | Some a -> condition_expr a
      | None -> failwith "Emit: a consistency version without its assertion"
    in
    form ~what:"consistency" "Consistency" c.cons_name
      (`Assoc
        (fields
           [ ("subject", so c.cons_subject); ("assertion", assertion);
             ("validity", validity c.cons_validity); source c.cons_source ]))

  (* fact type + roles (IR-SPEC ch. 2.6): each role names its filler object
     type. *)
  let role_ref (r : role_ref) : json =
    node "Role"
      ([ ("name", so r.role_identifier); ("objectType", so r.role_object_type_name) ]
      @ optional "plural" s r.role_plural
      @ [ ("many", b r.role_many) ])

  let fact_type_name (ft : fact_type_name) : json =
    node "FactType"
      [ ("name", so ft.ft_name); ("reciprocal", b ft.reciprocal);
        ("roles", `List (List.map role_ref ft.role_names)) ]

  let assignment (t : assignment) : json =
    node "MemberAssignment"
      [ ("member", so t.to_attribute); ("value", opt expr t.to_value);
        ("characteristic", b t.to_characteristic) ]

  let object_creation (oc : resolved_object_creation) : json =
    (* connectingRole is optional (IR-SPEC ch. 3.3) — the created-side role
       of the back-link, written whole when present *)
    let condition = match oc.oc_condition with None -> [] | Some c -> [ ("condition", condition_expr c) ] in
    let connecting =
      match oc.oc_connecting_role with None -> [] | Some rb -> [ ("connectingRole", role_binding rb) ]
    in
    form ~what:"object creation" "ObjectCreation" oc.oc_name
      (`Assoc
        ([ ("createdType", so oc.created_type); ("subject", so oc.oc_subject);
           ("assignments", `List (List.map assignment oc.assignments)) ]
        @ condition
        @ [ ("validity", validity oc.oc_validity) ]
        @ connecting))

  (* every field but the node is written only when present, matching the
     loader's admission: a name only where the frontend gives one (ch. 6.1),
     no decision on an attribute or group default, no justification on an
     initialisation or group default, and an initialisation only when it is
     true *)
  let rec default ?name (d : default) : json =
    let init = if d.default_initialisation then [ ("initialisation", `Bool true) ] else [] in
    `Assoc
      (fields
         ([ ("node", `String "Default") ]
         @ optional "name" s name
         @ optional "decision" b d.default_decision
         @ optional "justification" condition_expr d.default_justification
         @ optional "consequence" expr d.default_consequence
         @ init
         @ optional "exceptions" exception_groups d.default_exceptions
         @ [ source d.default_source ]))

  (* the nested exception group riding [exceptions] (ch. 7.9): the
     {unordered, members} object (no node discriminator) and its
     [ExceptionGroup] members, mirroring the loader *)
  and exception_groups (egs : exception_groups) : json =
    `Assoc
      [ ("unordered", b egs.egs_unordered);
        ("members", `List (List.map exception_group egs.egs_members)) ]

  and exception_group (eg : exception_group) : json =
    node "ExceptionGroup" [ ("defaults", `List (List.map (fun d -> default d) eg.eg_defaults)); source eg.eg_source ]

  (* [names] holds the names of the group's defaults in their order *)
  let prioritized_defaults ((pd : prioritized_defaults), names) : json =
    if List.length names <> List.length pd.defaults then
      failwith
        (Printf.sprintf "Emit: the group %s has %d defaults but %d default names"
           (Option.value pd.pd_name ~default:"<unnamed>") (List.length pd.defaults) (List.length names));
    let unordered = if pd.pd_unordered then [ ("unordered", `Bool true) ] else [] in
    `Assoc
      (fields
         ([ ("node", `String "PrioritizedDefaults"); ("name", so pd.pd_name); ("subject", `String pd.pd_subject) ]
         @ optional "characteristic" s pd.pd_characteristic
         @ optional "attribute" s pd.pd_attribute
         @ [ ("defaults", `List (List.map2 (fun name d -> default ?name d) names pd.defaults)) ]
         @ unordered
         @ [ source pd.pd_source; ("validity", opt validity_window pd.pd_validity) ]))

  let function_def (fd : function_def) : json =
    node "FunctionDef"
      [ ("name", s fd.fd_name); ("parameters", param_list fd.fd_parameters);
        ("result", datatype fd.fd_result); ("body", expr fd.fd_body); source fd.fd_source ]

  (* The M1 container (decision V3 (b)): the declaration set under "model",
     the rule lists under "rules" with their L5 keys, in the schema's
     property order. functionDefs is an extension list, written only when
     non-empty; prioritizedDefaults is written whole. No frontend builds a
     parameter, unit system, dimension, timeline, day kind or diagnostic, so
     those lists are written empty, and a filled one refuses: written as []
     it would drop what it held without a word. The serialiser refuses an
     Unresolved marker as well, so no written document holds a marker a
     diagnostic would have to account for (WF-113). [default_names], where
     given, holds per defaults group the names of its defaults in their
     order (Default.name, IR-SPEC ch. 6.1); without it no Default carries a
     name. *)
  let document ?default_names (d : document) : json =
    let unfilled list = function
      | [] -> ()
      | _ -> failwith (Printf.sprintf "Emit: the document fills %s, which the %s never builds" list p)
    in
    unfilled "parameters" d.parameters;
    unfilled "unitSystems" d.unit_systems;
    unfilled "dimensions" d.dimensions;
    unfilled "timelines" d.timelines;
    unfilled "dayKinds" d.day_kinds;
    unfilled "dayKindDefinitions" d.resolved_day_kind_definitions;
    unfilled "allocations" d.allocations;
    unfilled "uniquenesses" d.uniquenesses;
    unfilled "uniquenesses" d.uniqueness_unions;
    unfilled "factCreations" d.fact_creations;
    unfilled "startPointDeterminations" d.resolved_start_point_determinations;
    unfilled "ruleGroups" d.rule_groups;
    unfilled "diagnostics" d.diagnostics;
    let groups =
      match default_names with
      | None -> List.map (fun (pd : prioritized_defaults) -> (pd, List.map (fun _ -> None) pd.defaults)) d.prioritized_defaults
      | Some names when List.length names = List.length d.prioritized_defaults ->
          List.combine d.prioritized_defaults names
      | Some names ->
          failwith
            (Printf.sprintf "Emit: %d defaults groups but default names for %d"
               (List.length d.prioritized_defaults) (List.length names))
    in
    let function_defs =
      match d.function_defs with [] -> [] | fds -> [ ("functionDefs", `List (List.map function_def fds)) ]
    in
    `Assoc
      [ ("format", `String "uchar-ir-v1");
        ("model",
         `Assoc
           ([ ("objectTypes", `List (List.map object_type d.object_types));
              ("factTypes",
               `List
                 (List.map
                    (fun (key, (ft : fact_type_name)) ->
                      named "fact type" key ft.ft_name;
                      fact_type_name ft)
                    d.fact_types));
              ("parameters", `List []);
              ("domains", `List (List.map domain d.domain_specs));
              ("unitSystems", `List []);
              ("dimensions", `List []);
              ("timelines", `List []);
              ("dayKinds", `List []) ]
           @ function_defs));
        ("rules",
         `Assoc
           [ ("rules", `List (List.map rule d.resolved_rules));
             ("dayKindDefinitions", `List []);
             ("objectCreations", `List (List.map object_creation d.object_creations));
             ("allocations", `List []);
             ("consistencies", `List (List.map consistency d.consistencies));
             ("uniquenesses", `List []);
             ("factCreations", `List []);
             ("startPointDeterminations", `List []);
             ("ruleGroups", `List []);
             ("prioritizedDefaults", `List (List.map prioritized_defaults groups)) ]);
        ("diagnostics", `List []) ]

  let to_string ?default_names (d : document) : string =
    Yojson.Safe.pretty_to_string (document ?default_names d) ^ "\n"

  (* Serialise whole, then write: opening the file first would leave an
     empty or half-written document behind when a node this serialiser does
     not write raises, and a truncated IR that a later reader picks up is
     worse than no file at all. *)
  let to_file ?default_names (path : string) (d : document) : unit =
    let text = to_string ?default_names d in
    let oc = open_out path in
    output_string oc text;
    close_out oc
end
