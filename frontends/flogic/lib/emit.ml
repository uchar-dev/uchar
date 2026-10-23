(* Serialiser: Ir_v0.Ast values -> uchar-ir-v1 wire JSON, in its L5 shape.

   The lowering builds typed [Ir_v0.Ast] values and hands them to exactly
   one serialiser — this module — so schema conformity is structural
   rather than stringly-typed. Key names and node discriminators mirror
   [Ir_v0.Loader]'s tables, every enumeration value is written through the
   one translation table the loader reads with ([Ir_v0.Spelling]), and the
   key ORDER is the corpus's own, so the output canonicalises (gate 2's
   serialiser shape) into the checked-in golden byte for byte.

   The [uchar-ir-v0] library ships a strict loader but no serialiser, so
   a frontend that emits has to carry one. This one covers exactly the
   node set the F-logic slice produces and raises [Failure] with context
   on anything else — a guessed shape would be worse than a crash. *)

open Ir_v0.Ast
module Spelling = Ir_v0.Spelling

type json = Yojson.Safe.t

let s (x : string) : json = `String x
let so : string option -> json = function None -> `Null | Some x -> `String x
let b (x : bool) : json = `Bool x
let i (x : int) : json = `Int x
let io : int option -> json = function None -> `Null | Some n -> `Int n
let opt (f : 'a -> json) : 'a option -> json = function None -> `Null | Some x -> f x
let str_opt_list (xs : string option list) : json = `List (List.map so xs)
let map_ (f : 'a -> json) (kvs : (string * 'a) list) : json =
  `Assoc (List.map (fun (k, v) -> (k, f v)) kvs)
(* The source record (delta B26): a node with source text writes it as
   {"snippet"} under "source", at the place v0 wrote "bron"; a node without
   source text writes no field and never a null, so [fields] and [node] leave
   the placeholder of an absent record out. An empty snippet has no reading
   in any reader of the wire, so it is refused here in the loader's sentence.
   The slice writes no line: it builds no consistency and no rule group. *)
let source (br : string option) : string * json =
  match br with
  | Some "" -> failwith ("Emit: " ^ Ir_v0.Loader.empty_snippet_refusal)
  | Some x -> ("source", `Assoc [ ("snippet", `String x) ])
  | None -> ("source", `Null)

let fields (kvs : (string * json) list) : (string * json) list =
  List.filter (fun (k, v) -> not (k = "source" && v = `Null)) kvs

let node k rest : json = `Assoc (("node", `String k) :: fields rest)

(* A field IR-SPEC calls optional is written whole when present and left out
   otherwise: since the L5 landing no such field carries a null. *)
let optional (k : string) (f : 'a -> json) : 'a option -> (string * json) list = function
  | None -> []
  | Some x -> [ (k, f x) ]

(* An enumeration value, spelled by its family in the translation table;
   [Spelling.encode] refuses a constructor the family does not spell. *)
let enum (fam : 'a Spelling.family) (c : 'a) : json = s (Spelling.encode fam c)

(* the slice's two folds, both an Aggregation on the wire (delta B21) *)
let aggregation_kind = function
  | (Sum_function | Count) as k -> enum Spelling.aggregation_kind k
  | _ -> failwith "Emit: aggregation kind not produced by the F-logic slice"

(* The type system in its L5 shape: a Number always carries its sign (a sign
   the in-memory form leaves open is unspecified) and, since the slice
   declares no units, no unit; a day date is a Date at day precision. *)
let datatype (d : datatype) : json =
  match d with
  | Number { whole_part; decimal_places; sign_char; measure_unit } ->
      if measure_unit <> None then failwith "Emit: the slice declares no units";
      node "Number"
        [ ("whole", b whole_part); ("decimals", i decimal_places);
          ("sign", enum Spelling.sign (Option.value sign_char ~default:Unspecified)) ]
  | Boolean -> node "Boolean" []
  | Text_type -> node "Text" []
  | Date_in_days -> node "Date" [ ("precision", enum Spelling.precision d) ]
  | _ -> failwith "Emit: datatype not produced by the F-logic slice"

let role_binding (rb : role_binding) : json =
  node "RoleBinding" [ ("factType", s rb.rb_fact_type); ("role", s rb.rb_role) ]

(* The typed value of a Literal (IR-SPEC ch. 4.1, delta B22): one key, the
   variant the literal's type names. The slice writes numbers and the
   boolean of a characteristic head only. *)
let literal_value (v : literal_variant) : json =
  match v with
  | Lv_number x -> `Assoc [ ("number", s x) ]
  | Lv_boolean x -> `Assoc [ ("boolean", b x) ]
  | _ -> failwith "Emit: literal value not produced by the F-logic slice"

let rec expr (e : expr) : json =
  let typ t = ("type", opt datatype t) in
  match e with
  | Literal { variant; typ = t; source_name = br } ->
      node "Literal" [ ("value", literal_value variant); typ t; source br ]
  | Binary { op; left_side; right_side; typ = t; source_name = br } ->
      node "Binary"
        [ ("op", enum Spelling.binary_op op); ("left", opt expr left_side);
          ("right", opt expr right_side); typ t; source br ]
  | Attribute_reference { attribute_ref; object_type; source_name = br; navigation_bindings; anchor } ->
      if navigation_bindings <> None then
        failwith "Emit: the slice synthesises no navigation path";
      if anchor <> None then failwith "Emit: the slice writes no form anchor";
      node "AttributeReference"
        [ ("attribute", so attribute_ref); ("subjectType", so object_type); source br ]
  | Function_call
      { kind; call_arguments; element_object_type; empty_fallback; filter; dimension_selection;
        typ = t; source_name = br; grain; source_role; source_path; anchor } ->
      if filter <> None || dimension_selection <> None || grain <> None
         || source_path <> None || anchor <> None then
        failwith "Emit: aggregation qualifier not produced by the F-logic slice";
      (* the slice's two folds read linked instances, so the range is written
         whole on every one of them and never left to be derived (B12) *)
      let rb =
        match source_role with
        | Some rb -> rb
        | None -> failwith "Emit: an instance fold without its role binding"
      in
      (* an absent optional field is left out (IR-SPEC ch. 4.3) *)
      node "Aggregation"
        ([ ("kind", aggregation_kind kind);
           ("arguments", `List (List.map expr call_arguments)) ]
        @ (match element_object_type with None -> [] | Some el -> [ ("elementType", s el) ])
        @ (match empty_fallback with None -> [] | Some f -> [ ("emptyFallback", expr f) ])
        @ [ ("sourceRole", role_binding rb); typ t; source br ])
  | _ -> failwith "Emit: expression node not produced by the F-logic slice"

let rec condition_expr (c : condition_expr) : json =
  match c with
  | Comparison { op; left_side; right_side; time_period; source_name = br } ->
      if time_period <> None then failwith "Emit: the slice writes no period qualifier";
      node "Comparison"
        [ ("op", enum Spelling.comparison_op op); ("left", opt expr left_side);
          ("right", opt expr right_side); source br ]
  | Compound { quantifier_kind; cutoff; components; source_name = br } ->
      node "Compound"
        [ ("quantifier", enum Spelling.quantifier quantifier_kind); ("threshold", i cutoff);
          ("parts", `List (List.map (fun c ->
             match c with
             | Comparison _ | Characteristic_check _ -> condition_leaf c
             (* a fold beside an ordinary condition of the subject: the two
                conjoin, and the fold keeps its own shape inside *)
             | Quantifier_fold _ -> condition_expr c
             | _ -> failwith "Emit: nested composite not produced by the F-logic slice")
             components));
          source br ]
  (* The quantifier fold. [threshold] is present EXACTLY under the three
     counting quantifiers (WF-108 (e)) and absent otherwise, so it is
     written conditionally rather than as a null nobody may consult; the
     subselection is absent where there is none; the range is always the
     one-step [sourceRole], never [sourcePath]. *)
  | Quantifier_fold
      { kv_quantifier; kv_threshold; kv_element_object_type; kv_filter; kv_test; kv_source;
        kv_source_role; kv_source_path; kv_anchor } ->
      if kv_source_path <> None then
        failwith "Emit: the slice writes no multi-step quantifier range";
      if kv_anchor <> None then failwith "Emit: the slice writes no form anchor";
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
  | c -> condition_leaf c

and condition_leaf (c : condition_expr) : json =
  match c with
  | Characteristic_check
      { characteristic; negation; holder; time_period; source_name = br; navigation_binding; anchor } ->
      if time_period <> None || navigation_binding <> None || anchor <> None then
        failwith "Emit: the slice writes no period, navigation or anchor on a characteristic check";
      node "CharacteristicCheck"
        ([ ("characteristic", so characteristic); ("negated", b negation) ]
        @ optional "host" s holder
        @ [ source br ])
  | Comparison { op; left_side; right_side; time_period; source_name = br } ->
      if time_period <> None then failwith "Emit: the slice writes no period qualifier";
      node "Comparison"
        [ ("op", enum Spelling.comparison_op op); ("left", opt expr left_side);
          ("right", opt expr right_side); source br ]
  | _ -> failwith "Emit: condition shape not produced by the F-logic slice"

(* an open side of a validity is an absent bound; neither bound is always *)
let validity_window (g : validity_window) : json =
  node "Validity" (optional "from" s g.valid_from @ optional "to" s g.valid_to)

(* A list entry of the M1 model carries its name once, on its node, so the
   in-memory map key and the node's own name must be one spelling. *)
let named what key (ident : string option) =
  if ident <> Some key then
    failwith
      (Printf.sprintf "Emit: %s %S carries the name %s; the uchar-ir-v1 list entry has one" what
         key (match ident with None -> "null" | Some n -> Printf.sprintf "%S" n))

(* ObjectType with its members as nodes (IR-SPEC ch. 2.2). The slice
   declares no plural, dimension or timeline, so a filled member-facet
   table refuses rather than being written in a guessed form. *)
let object_type (key, (o : object_type)) : json =
  named "object type" key o.ot_ident;
  let unfilled table = function
    | [] -> ()
    | _ -> failwith (Printf.sprintf "Emit: object type %S fills %s, which the slice never builds" key table)
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
  let supertype = match o.ot_supertype with None -> [] | Some p -> [ ("supertype", s p) ] in
  let plural = match o.plural with None -> [] | Some p -> [ ("plural", s p) ] in
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
                node "Characteristic"
                  [ ("name", s k); ("kind", enum Spelling.characteristic_kind kind) ])
              characteristics)) ])

let role_ref (r : role_ref) : json =
  node "Role"
    ([ ("name", so r.role_identifier); ("objectType", so r.role_object_type_name) ]
    @ optional "plural" s r.role_plural
    @ [ ("many", b r.role_many) ])

let fact_type_name (ft : fact_type_name) : json =
  node "FactType"
    [ ("name", so ft.ft_name); ("reciprocal", b ft.reciprocal);
      ("roles", `List (List.map role_ref ft.role_names)) ]

(* A rule (IR-SPEC ch. 3, Versioning; ch. 3.1): {node, name, versions}. The
   slice builds every rule with one version, so each in-memory record is one
   form; a record name ending in a space, '#' and digits is the in-memory
   spelling of one version of a form with several, which the slice never
   builds, and it refuses rather than writing a name no reader loads. The
   version's kind is read back onto the initialisation flag and the
   presentation tag, its validity is always written, and its target is an
   attribute reference. *)
let rule (r : resolved_rule) : json =
  if r.rr_source_annotations <> None then
    failwith "Emit: the slice writes no statute annotations";
  (match r.rr_name with
  | Some n when Ir_v0.Loader.versioned_name n ->
      failwith
        (Printf.sprintf
           "Emit: the rule name %S ends in a space, '#' and digits, the in-memory spelling of one \
            version of a form with several, and the slice builds no versions"
           n)
  | _ -> ());
  let initialisation, presentation =
    match r.rr_presentation with
    | Some ((Equation | Calculation) as k) ->
        (false, [ ("presentation", enum Spelling.presentation k) ])
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
  let validity =
    validity_window (Option.value r.rr_validity ~default:{ valid_from = None; valid_to = None })
  in
  node "Rule"
    [ ("name", so r.rr_name);
      ("versions",
       `List
         [ `Assoc
             ([ ("validity", validity); ("target", target); ("value", value) ]
             @ condition
             @ [ ("initialisation", b initialisation) ]
             @ presentation) ]) ]

let default (d : default) : json =
  if d.default_initialisation || d.default_exceptions <> None then
    failwith "Emit: default form not produced by the F-logic slice";
  `Assoc
    (fields
       ([ ("node", `String "Default") ]
       @ optional "decision" b d.default_decision
       @ optional "justification" condition_expr d.default_justification
       @ optional "consequence" expr d.default_consequence
       @ [ source d.default_source ]))

let prioritized_defaults (pd : prioritized_defaults) : json =
  if pd.pd_unordered then failwith "Emit: the slice declares no unordered group";
  `Assoc
    (fields
       ([ ("node", `String "PrioritizedDefaults"); ("name", so pd.pd_name);
          ("subject", `String pd.pd_subject) ]
       @ optional "characteristic" s pd.pd_characteristic
       @ optional "attribute" s pd.pd_attribute
       @ [ ("defaults", `List (List.map default pd.defaults));
           source pd.pd_source; ("validity", opt validity_window pd.pd_validity) ]))

(* The M1 container (decision V3 (b)): the declaration set under "model", the
   rule lists under "rules" with their L5 keys, in the schema's property
   order; prioritizedDefaults is written whole, as it always was. The slice
   builds no domain, parameter, unit system, dimension, timeline, day kind,
   function definition, other rule list or diagnostic, so those lists are
   written empty or, for functionDefs, not at all, and a filled one refuses:
   written as [] it would drop what it held without a word. Its serialiser
   has no Unresolved arm, so no written document holds a marker a diagnostic
   would have to account for (WF-113). *)
let document (d : document) : json =
  let unfilled list = function
    | [] -> ()
    | _ -> failwith (Printf.sprintf "Emit: the document fills %s, which the slice never builds" list)
  in
  unfilled "parameters" d.parameters;
  unfilled "domains" d.domain_specs;
  unfilled "unitSystems" d.unit_systems;
  unfilled "dimensions" d.dimensions;
  unfilled "timelines" d.timelines;
  unfilled "dayKinds" d.day_kinds;
  unfilled "functionDefs" d.function_defs;
  unfilled "dayKindDefinitions" d.resolved_day_kind_definitions;
  unfilled "objectCreations" d.object_creations;
  unfilled "allocations" d.allocations;
  unfilled "consistencies" d.consistencies;
  unfilled "uniquenesses" d.uniquenesses;
  unfilled "uniquenesses" d.uniqueness_unions;
  unfilled "factCreations" d.fact_creations;
  unfilled "startPointDeterminations" d.resolved_start_point_determinations;
  unfilled "ruleGroups" d.rule_groups;
  unfilled "diagnostics" d.diagnostics;
  `Assoc
    [ ("format", `String "uchar-ir-v1");
      ("model",
       `Assoc
         [ ("objectTypes", `List (List.map object_type d.object_types));
           ("factTypes",
            `List
              (List.map
                 (fun (key, (ft : fact_type_name)) ->
                   named "fact type" key ft.ft_name;
                   fact_type_name ft)
                 d.fact_types));
           ("parameters", `List []);
           ("domains", `List []);
           ("unitSystems", `List []);
           ("dimensions", `List []);
           ("timelines", `List []);
           ("dayKinds", `List []) ]);
      ("rules",
       `Assoc
         [ ("rules", `List (List.map rule d.resolved_rules));
           ("dayKindDefinitions", `List []);
           ("objectCreations", `List []);
           ("allocations", `List []);
           ("consistencies", `List []);
           ("uniquenesses", `List []);
           ("factCreations", `List []);
           ("startPointDeterminations", `List []);
           ("ruleGroups", `List []);
           ("prioritizedDefaults", `List (List.map prioritized_defaults d.prioritized_defaults)) ]);
      ("diagnostics", `List []) ]

let to_string (d : document) : string = Yojson.Safe.pretty_to_string (document d) ^ "\n"

(* Serialise WHOLE, then write. Opening the file first would leave an empty
   or half-written document behind when a node this serialiser does not
   carry raises — and a truncated IR that a later reader picks up is worse
   than no file at all. *)
let to_file (path : string) (d : document) : unit =
  let text = to_string d in
  let oc = open_out path in
  output_string oc text;
  close_out oc
