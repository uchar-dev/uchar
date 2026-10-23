(* Strict loader for the uchar-ir-v1 wire format in its L5 shape.

   Strict means: every object must carry exactly the keys the format
   declares (the schema is additionalProperties:false: a required key is
   present, an optional key is absent or written whole), every node object
   must carry the expected "node" discriminator, and every enum value must be
   a known member. Any deviation raises [Error] with the JSON path — never a
   silent default.

   Two deliberate narrowings relative to bare JSON Schema, both fail-fast:
   integer fields accept only canonical integer lexemes (a float-shaped
   "2.0" or an integer beyond native range is a load error, matching the
   byte-identical round-trip discipline of gate 2, which already pins the
   canonical emitter form); and paths are built eagerly, which is
   quadratic only on pathologically deep (100k+ nesting) documents.

   One M1 rule is enforced here and in the schema alike: a declaration in
   one of the two model lists whose node is not one of L3's (factTypes,
   dimensions) carries a string `name`. There is no map key beside it, so
   that field is the declaration's only name. The other two lists of that
   rule (parameters, unitSystems) hold nodes L3 rewrote, whose `name` is a
   required string as well.

   The L2 rules enforced here and in the schema alike: every field that
   left the wire with the reference layer is an unknown field, refused by
   name, and the one unknown datatype node is an unknown node; a binding
   L2 made required is read as required — the creation block, the step
   binding, the allocation's recipient role, a union selection's relation,
   the quantifier fold's range, and the range of a single-argument fold
   with an elementType under any kind but total (B12). A reader
   of this library therefore never meets a bindingless form that it would
   have to derive a binding for.

   The L3 rules enforced here and in the schema alike (the type system):
   the datatype nodes are `Number` and `Date`, a unit is a `Unit` and a
   unit system a `UnitSystem` with a list of units, a day kind a `DayKind`
   and a parameter's time-dependence a timeline facet; every node and
   field that left the wire with them is unknown and refused by name. Four
   values the schema types are refused here in one sentence each, which
   the other readers of the wire share: a conversion factor that is not an
   exact rational {num, den} of two integers between 1 and 2^62 - 1, a presentation
   other than "percentage", the presentation on a number whose unit is not
   '%', and a sign other than unspecified on a number whose unit is '%'. The
   last two follow from the in-memory form ([Ast]): a number whose unit is
   '%' is read as a [Percentage], which carries no sign, and any other
   number as a [Number], which carries no presentation.

   The L4 rules enforced here and in the schema alike (the rule and the
   expression layer): a named form is {node, name, versions} with at least
   one version, and its name never ends in a space, '#' and digits, the
   in-memory spelling of one version ([Ast]); a version record carries no
   node; a rule version's target is an AttributeReference, and initialisation
   true never stands beside a presentation; a uniqueness version has at
   least one source and each source at least one key, and a version with
   two or more sources carries no scope; a fact creation's path has at
   least one hop; an Aggregation and a BuiltinCall carry only their own
   fields and kinds; a literal's value is exactly one variant of the case
   format's value union; a context value carries a string name and a
   datatype, the bound day a datatype. Every node and field that left the
   wire with them is unknown and refused by name, and so is an enum value
   outside a field's set (`unknown <field> value "<v>"`). The string inside
   a literal's variant is loaded as written: its spelling is a verifier
   rule's (WF-081, WF-084, WF-095). Two other defects are a verifier rule's
   and not the schema's, which cannot cross them: a value variant that is
   not the one the literal's type names (WF-114) and a context-value name
   outside the closed set (WF-098). The verifier loads all of these so the
   rule can cite them; every other reader refuses them here, in the rule's
   own sentence ([reader]).

   The source record, the first step of L5 (delta B26), enforced here and in
   the schema alike: a node's source text is the record `source` {snippet,
   line?}, optional on every node and read onto the in-memory `source_name` fields
   ([source_record]); `bron` and a `line` beside the record are unknown fields,
   and an empty snippet is refused in every stance. The schema requires the
   record on an Unresolved marker; the verifier loads a marker without one for
   WF-113 (b), and every other reader refuses it here ([reader]).

   The renaming, the second step of L5 (deltas B6 and B10), enforced here and
   in the schema alike: every node and every key carries its IR-SPEC name,
   the lists under `rules` included, and every enumeration value its English
   lowerCamel spelling, read through the one translation table [Spelling].
   The in-memory records keep their v0 constructors and fields ([Ast]): the
   wire was renamed, the in-memory model was not. Every v0 node name, v0 key
   and v0 enumeration value is unknown and refused by name. A field the
   renaming made optional is absent or written whole, and a null there is
   refused like any other defect of its type: its absence is what the v0 null
   was — an open bound of a Validity or a TimeWindow, a Clamp without that
   bound, a characteristic check on the subject itself (`host`), no
   whole-period qualifier, no plural, no preposition, no decision or
   justification on a default, no characteristic on a defaults group, no day
   kind on a predicate of another kind, no subselection of a quantifier fold.
   Three values the schema does not admit are a verifier rule's, which names
   them: the quantifier `the` on a QuantifierFold (WF-108 (a)), and the
   severities `error` and `info` and a null severity (WF-113 (a)). The
   verifier loads them; every other reader refuses them here ([reader]). *)

open Ast

exception Error of string * string (* path, message *)

let fail path msg = raise (Error (path, msg))
let ( / ) path key = path ^ "/" ^ key

type json = Yojson.Safe.t

let obj path : json -> (string * json) list = function
  | `Assoc kvs -> kvs
  | j -> fail path ("expected object, got " ^ Yojson.Safe.to_string j)

let list_ path : json -> json list = function
  | `List xs -> xs
  | j -> fail path ("expected array, got " ^ Yojson.Safe.to_string j)

let field path kvs name =
  match List.assoc_opt name kvs with
  | Some v -> v
  | None -> fail path ("missing field '" ^ name ^ "'")

(* Exact key-set check: unknown and missing keys are both load errors.
   [ext] lists the node's extension fields (v1 deltas riding the v0 wire,
   IR-SPEC § Method): admitted when present, never required — the pinned
   v0 emitter does not produce them. Anything else stays a load error. *)
let check_keys ?(ext = []) path kvs expected =
  let actual = List.map fst kvs in
  List.iter
    (fun k ->
      if not (List.mem k expected || List.mem k ext) then
        fail path ("unknown field '" ^ k ^ "'"))
    actual;
  List.iter
    (fun k ->
      if not (List.mem k actual) then fail path ("missing field '" ^ k ^ "'"))
    expected

(* Decode an extension field when present; absence is None, an explicit
   null is a load error (an extension field is written whole or not at
   all). *)
let ext_field path kvs name (f : string -> json -> 'a) : 'a option =
  match List.assoc_opt name kvs with
  | Some j -> Some (f (path / name) j)
  | None -> None

let node_kind path kvs =
  match field path kvs "node" with
  | `String s -> s
  | j -> fail (path / "node") ("expected string, got " ^ Yojson.Safe.to_string j)

let expect_node path kvs expected =
  let k = node_kind path kvs in
  if k <> expected then
    fail (path / "node") ("expected \"" ^ expected ^ "\", got \"" ^ k ^ "\"")

let str path = function
  | `String s -> s
  | j -> fail path ("expected string, got " ^ Yojson.Safe.to_string j)

let str_opt path = function
  | `String s -> Some s
  | `Null -> None
  | j -> fail path ("expected string or null, got " ^ Yojson.Safe.to_string j)

let int_ path = function
  | `Int i -> i
  | j -> fail path ("expected integer, got " ^ Yojson.Safe.to_string j)

let int_opt path = function
  | `Int i -> Some i
  | `Null -> None
  | j -> fail path ("expected integer or null, got " ^ Yojson.Safe.to_string j)

let bool_ path = function
  | `Bool b -> b
  | j -> fail path ("expected boolean, got " ^ Yojson.Safe.to_string j)

let opt path (f : string -> json -> 'a) : json -> 'a option = function
  | `Null -> None
  | j -> Some (f path j)

let str_opt_list path j =
  List.mapi (fun i x -> str_opt (path / string_of_int i) x) (list_ path j)

(* --- enums ------------------------------------------------------------- *)

(* An enumeration value, read through its family's translation table
   ([Spelling]): the constructor the wire value spells, or a refusal that
   names the field and the value. The values the verifier refuses by name
   ([Spelling.family], [refused]) are read here in both stances; an executor
   refuses them after the structural load ([executor_refusals]). *)
let enum (fam : 'a Spelling.family) path j : 'a =
  let s = str path j in
  match Spelling.decode fam s with
  | Some v -> v
  | None -> fail path ("unknown " ^ fam.field ^ " value \"" ^ s ^ "\"")

let enum_opt fam path = function `Null -> None | j -> Some (enum fam path j)

(* The symbolic role binding (extension node, IR-SPEC ch. 2.6): both
   fields are plain strings — a binding is written whole or not at all. *)
let role_binding path j : role_binding =
  let kvs = obj path j in
  expect_node path kvs "RoleBinding";
  check_keys path kvs [ "node"; "factType"; "role" ];
  {
    rb_fact_type = str (path / "factType") (field path kvs "factType");
    rb_role = str (path / "role") (field path kvs "role");
  }

(* The presentation of a Number (IR-SPEC ch. 2.4): its one value, which no
   family of the translation table holds because it has no other. *)
let presentation path j =
  match str path j with
  | "percentage" -> ()
  | s -> fail path ("unknown presentation value \"" ^ s ^ "\"")

(* --- unit and datatype ------------------------------------------------- *)

(* A unit's conversion factor: the exact rational {num, den}, both integers
   of at least 1 and at most 2^62 - 1 — the schema's bound, which is also the
   native integer here (IR-SPEC ch. 2.5). Whether it is in lowest terms is WF-092
   (c), a verifier check, so {2, 4} loads. Every other form — a JSON number,
   a string, a missing or unknown key, a value below 1 or beyond the native
   integer range — is refused in one sentence. *)
let rational path j : rational =
  let refuse () =
    fail path
      ("a conversion factor is an exact rational {num, den} whose two integers are at least \
        1 and at most 4611686018427387903 (2^62 - 1), got " ^ Yojson.Safe.to_string j)
  in
  match j with
  | `Assoc kvs ->
      if List.sort compare (List.map fst kvs) <> [ "den"; "num" ] then refuse ();
      let part k = match List.assoc k kvs with `Int i when i >= 1 -> i | _ -> refuse () in
      { num = part "num"; den = part "den" }
  | _ -> refuse ()

(* The fields of a unit at every position it occupies (IR-SPEC ch. 2.5).
   [extra] is the one further field a declaration in a unit system admits,
   its spellings: an occurrence admits none, so a spelling written there is
   an unknown field, refused by name rather than read and dropped. *)
let rec unit_fields ~extra path kvs : measure_unit =
  expect_node path kvs "Unit";
  check_keys path kvs
    ~ext:([ "system"; "canonical"; "factorToCanonical"; "denominator" ] @ extra)
    [ "node"; "abbreviation" ];
  {
    abbr_text = Some (str (path / "abbreviation") (field path kvs "abbreviation"));
    system = ext_field path kvs "system" str;
    canonical_form = ext_field path kvs "canonical" str;
    factor_to_canonical = ext_field path kvs "factorToCanonical" rational;
    denominator = ext_field path kvs "denominator" measure_unit;
  }

and measure_unit path j : measure_unit = unit_fields ~extra:[] path (obj path j)

(* A unit as its unit system declares it, with its further spellings. The
   list is written whole or not at all: absent is no spelling, and an empty
   list is refused like an explicit null. *)
let declared_unit path j : declared_unit =
  let kvs = obj path j in
  let u = unit_fields ~extra:[ "spellings" ] path kvs in
  let spellings p j =
    match list_ p j with
    | [] -> fail p "a unit's spellings are at least one spelling, or the field is absent"
    | xs -> List.mapi (fun i x -> str (p / string_of_int i) x) xs
  in
  {
    ge_unit = u;
    ge_spellings = Option.value ~default:[] (ext_field path kvs "spellings" spellings);
  }

let rec datatype path j : datatype =
  let kvs = obj path j in
  let f name = field path kvs name in
  match node_kind path kvs with
  | "Number" ->
      (* the in-memory form keeps the two v0 constructors (Ast header): a
         unit '%' makes the number a Percentage, whatever else it carries *)
      check_keys path kvs ~ext:[ "unit"; "presentation" ] [ "node"; "whole"; "decimals"; "sign" ];
      let whole_part = bool_ (path / "whole") (f "whole")
      and decimal_places = int_ (path / "decimals") (f "decimals")
      and sign = enum Spelling.sign (path / "sign") (f "sign")
      and unit_ = ext_field path kvs "unit" measure_unit
      and tag = ext_field path kvs "presentation" presentation in
      (match unit_ with
       | Some ({ abbr_text = Some "%"; _ } as u) ->
           if sign <> Unspecified then
             fail (path / "sign")
               "a percentage carries no sign restriction: a number whose unit is '%' has the \
                sign unspecified";
           Percentage { whole_part; decimal_places; measure_unit = Some u }
       | _ ->
           if tag <> None then
             fail (path / "presentation")
               "the presentation \"percentage\" on a number whose unit is not '%'";
           Number { whole_part; decimal_places; sign_char = Some sign; measure_unit = unit_ })
  | "Text" -> check_keys path kvs [ "node" ]; Text_type
  | "Boolean" -> check_keys path kvs [ "node" ]; Boolean
  | "Date" ->
      check_keys path kvs [ "node"; "precision" ];
      enum Spelling.precision (path / "precision") (f "precision")
  | "Enumeration" ->
      check_keys path kvs [ "node"; "domain"; "values" ];
      Enumeration
        {
          domain_spec = str_opt (path / "domain") (f "domain");
          label_list = str_opt_list (path / "values") (f "values");
        }
  | "SumType" ->
      (* extension node (v1, row 36): domain is a plain string; each
         constructor carries a name and a payload datatype or explicit null
         (nullary). Payloads recurse through [datatype] (nested sum types). *)
      check_keys path kvs [ "node"; "domain"; "constructors" ];
      Sum_type
        {
          domain_spec = str (path / "domain") (f "domain");
          constructor_list = sum_constructors (path / "constructors") (f "constructors");
        }
  | "ListType" ->
      (* extension node (v1, row 10): the sole child is the element datatype,
         which recurses through [datatype] *)
      check_keys path kvs [ "node"; "element" ];
      List_type { element = datatype (path / "element") (f "element") }
  | "StructType" ->
      (* extension node (v1, Phase 5c): domain is a plain string; each field
         carries a name and a non-null datatype (no nullary field — unlike a
         sum-type payload). Fields recurse through [datatype] (nested structs
         and sum types). *)
      check_keys path kvs [ "node"; "domain"; "fields" ];
      Struct_type
        { domain_spec = str (path / "domain") (f "domain"); field_list = struct_fields (path / "fields") (f "fields") }
  | k -> fail (path / "node") ("unknown datatype node \"" ^ k ^ "\"")

and sum_constructors path j =
  List.mapi
    (fun i c ->
      let cp = path / string_of_int i in
      let ckvs = obj cp c in
      check_keys cp ckvs [ "name"; "payload" ];
      (str (cp / "name") (field cp ckvs "name"), opt (cp / "payload") datatype (field cp ckvs "payload")))
    (list_ path j)

and struct_fields path j =
  List.mapi
    (fun i fld ->
      let fp = path / string_of_int i in
      let fkvs = obj fp fld in
      check_keys fp fkvs [ "name"; "datatype" ];
      (str (fp / "name") (field fp fkvs "name"), datatype (fp / "datatype") (field fp fkvs "datatype")))
    (list_ path j)

(* The datatype at the DECLARATION position, `Domain.datatype`. The three
   forms that carry a domain name at a reference position carry none here:
   the Domain wrapper holds it, and a self-name written beside it is an
   unknown field. The record's own domain field is set to the wrapper's
   name, which is what it held in v0 as well, so every reader sees one
   datatype shape at both positions. *)
let domain_datatype name path j : datatype =
  let kvs = obj path j in
  let f key = field path kvs key in
  match node_kind path kvs with
  | "Enumeration" ->
      (* the designations of the values stand here and only here (IR-SPEC
         ch. 2.12); [vocabularies] reads them *)
      check_keys path kvs ~ext:[ "valueDesignations" ] [ "node"; "values" ];
      Enumeration { domain_spec = Some name; label_list = str_opt_list (path / "values") (f "values") }
  | "SumType" ->
      check_keys path kvs [ "node"; "constructors" ];
      Sum_type
        { domain_spec = name; constructor_list = sum_constructors (path / "constructors") (f "constructors") }
  | "StructType" ->
      check_keys path kvs [ "node"; "fields" ];
      Struct_type { domain_spec = name; field_list = struct_fields (path / "fields") (f "fields") }
  | _ -> datatype path j

(* The whole-period qualifier (IR-SPEC ch. 4.5): its granularity, read onto
   the in-memory flag [gp_granularity] (year true, month false), and its negation. *)
let entire_period path j : entire_period =
  let kvs = obj path j in
  expect_node path kvs "WholePeriod";
  check_keys path kvs [ "node"; "granularity"; "negated" ];
  {
    gp_granularity =
      enum Spelling.whole_period_granularity (path / "granularity") (field path kvs "granularity");
    gp_negated = bool_ (path / "negated") (field path kvs "negated");
  }

(* --- the source record ---------------------------------------------------- *)

(* The source text of a node (delta B26, IR-SPEC ch. 4): the record under the
   key `source`, {snippet, line?}, without a node discriminator. It is optional
   on every node; its absence is what a null `bron` was before it. [snippet] is
   the text, never empty; [line] a line of at least 1, admitted on every node.
   The in-memory records keep their v0 fields ([Ast]): a node's [bron] holds the
   snippet, [None] where the node carries no record, and the line field of a
   consistency version and a rule group holds the record's line, 0 where it
   carries none. A line on any other node is read and not kept: its record has
   no line field. Every other shape is refused in both stances: a record that
   is not an object, a missing or non-string snippet, a field beside snippet and
   line (the positions file and column among them), a line that is not an
   integer of at least 1, and an empty snippet, which no reader can give a
   meaning: a node without source text carries no source. *)
let empty_snippet_refusal =
  "a source snippet is never empty: a node without source text carries no source"

let source_line_refusal = "a source line is at least 1"

let source_record path j : string * int option =
  let kvs = obj path j in
  check_keys path kvs ~ext:[ "line" ] [ "snippet" ];
  let snippet = str (path / "snippet") (field path kvs "snippet") in
  if snippet = "" then fail (path / "snippet") empty_snippet_refusal;
  let line p j =
    let n = int_ p j in
    if n < 1 then fail p source_line_refusal;
    n
  in
  (snippet, ext_field path kvs "line" line)

(* The source text of a node that has no line field in memory. *)
let source_text path kvs : string option =
  Option.map fst (ext_field path kvs "source" source_record)

(* The source text and line of a consistency version or a rule group. *)
let source_with_line path kvs : string option * int =
  match ext_field path kvs "source" source_record with
  | Some (snippet, line) -> (Some snippet, Option.value ~default:0 line)
  | None -> (None, 0)

(* --- expr and conditie -------------------------------------------------- *)

(* The value of a Literal (delta B22): exactly one variant of the case
   format's value union, without a constructor payload. The string is loaded
   as written ([Ast.literal_variant]); every other shape — a bare string, a
   JSON number, two keys, an unknown key, a payload — is refused in one
   sentence, which names the seven shapes. *)
let literal_variant path j : literal_variant =
  let refuse () =
    fail path
      "a literal value is exactly one variant: {\"number\": string}, {\"boolean\": true or \
       false}, {\"text\": string}, {\"date\": string}, {\"dateTime\": string}, \
       {\"enumeration\": string} or {\"constructor\": string}, without a payload"
  in
  match j with
  | `Assoc [ (key, v) ] -> (
      match (key, v) with
      | "number", `String s -> Lv_number s
      | "boolean", `Bool b -> Lv_boolean b
      | "text", `String s -> Lv_text s
      | "date", `String s -> Lv_date s
      | "dateTime", `String s -> Lv_date_time s
      | "enumeration", `String s -> Lv_enumeration s
      | "constructor", `String s -> Lv_constructor s
      | _ -> refuse ())
  | _ -> refuse ()

(* The v0 lexeme of a literal's variant: the variant's string, and `waar` or
   `onwaar` for a boolean. *)
let literal_lexeme = function
  | Lv_number s | Lv_text s | Lv_date s | Lv_date_time s | Lv_enumeration s | Lv_constructor s
    ->
      s
  | Lv_boolean true -> "waar"
  | Lv_boolean false -> "onwaar"

(* The datatype of a context node, which the wire requires (IR-SPEC ch. 4.1):
   anything but a node object is refused in the node's own sentence. *)
let context_type ~refusal path kvs : datatype =
  match field path kvs "type" with
  | `Assoc _ as t -> datatype (path / "type") t
  | _ -> fail (path / "type") refusal

(* The anchor of a walk-starting node (IR-SPEC ch. 4.1a, WF-122): one value,
   "form". Where it may stand is WF-122's; the loader reads its value. *)
let anchor path j : anchor =
  match j with
  | `String "form" -> Anchor_form
  | `String s -> fail path ("unknown anchor value \"" ^ s ^ "\"")
  | _ -> fail path "an anchor is the string \"form\""

let rec expr path j : expr =
  let kvs = obj path j in
  let f name = field path kvs name in
  let typ () = opt (path / "type") datatype (f "type") in
  let source_name () = source_text path kvs in
  let arguments () =
    List.mapi
      (fun i x -> expr (path / "arguments" / string_of_int i) x)
      (list_ (path / "arguments") (f "arguments"))
  in
  match node_kind path kvs with
  | "Literal" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "value"; "type" ];
      let variant = literal_variant (path / "value") (f "value") in
      Literal { variant; typ = typ (); source_name = source_name () }
  | "TextComposition" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "parts"; "type" ];
      Text_composition
        {
          pieces =
            List.mapi
              (fun i x -> text_part (path / "parts" / string_of_int i) x)
              (list_ (path / "parts") (f "parts"));
          typ = typ ();
          source_name = source_name ();
        }
  | "Binary" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "op"; "left"; "right"; "type" ];
      Binary
        {
          op = enum Spelling.binary_op (path / "op") (f "op");
          left_side = opt (path / "left") expr (f "left");
          right_side = opt (path / "right") expr (f "right");
          typ = typ ();
          source_name = source_name ();
        }
  | "Aggregation" ->
      (* the eight folds (delta B21), read into the in-memory [Function_call] with
         no granularity: that field is a builtin's and unknown here *)
      check_keys path kvs
        ~ext:[ "elementType"; "anchor"; "filter"; "emptyFallback"; "dimensionSelection";
               "sourceRole"; "sourcePath"; "source" ]
        [ "node"; "kind"; "arguments"; "type" ];
      let kind = enum Spelling.aggregation_kind (path / "kind") (f "kind") in
      let call_arguments = arguments () in
      let element_object_type = ext_field path kvs "elementType" str in
      (* the range (B12): one spelling at most, and a single-argument fold
         with an elementType carries one under every kind but total, where
         its absence is the subject's own read *)
      (match (List.mem_assoc "sourceRole" kvs, List.mem_assoc "sourcePath" kvs) with
      | true, true ->
          fail (path / "sourcePath")
            "sourcePath beside sourceRole: a fold carries one range, never two"
      | false, false
        when element_object_type <> None && List.length call_arguments = 1 && kind <> Total ->
          fail path
            "a single-argument fold with an elementType carries its range (sourceRole or \
             sourcePath) under every kind but total: the range is written, never derived"
      | _ -> ());
      Function_call
        {
          kind;
          call_arguments;
          element_object_type;
          empty_fallback = ext_field path kvs "emptyFallback" expr;
          filter = ext_field path kvs "filter" condition_expr;
          dimension_selection = ext_field path kvs "dimensionSelection" dimension_selection;
          typ = typ ();
          source_name = source_name ();
          grain = None;
          source_role = ext_field path kvs "sourceRole" role_binding;
          (* loaded verbatim, empty list included: WF-102 (b) owns the
             arity, so a defective path is a citable violation and not a
             load failure with no rule to name *)
          source_path =
            ext_field path kvs "sourcePath" (fun p j ->
                List.mapi (fun i x -> role_binding (p / string_of_int i) x) (list_ p j));
          anchor = ext_field path kvs "anchor" anchor;
        }
  | "BuiltinCall" ->
      (* the seventeen other kinds (delta B21), with plain arguments: an
         element type, a range, a fallback or a selection is a fold's field
         and unknown here *)
      check_keys path kvs ~ext:[ "filter"; "granularity"; "source" ]
        [ "node"; "kind"; "arguments"; "type" ];
      let kind = enum Spelling.builtin_kind (path / "kind") (f "kind") in
      Function_call
        {
          kind;
          call_arguments = arguments ();
          element_object_type = None;
          empty_fallback = None;
          filter = ext_field path kvs "filter" condition_expr;
          dimension_selection = None;
          typ = typ ();
          source_name = source_name ();
          grain = ext_field path kvs "granularity" (enum Spelling.granularity);
          source_role = None;
          source_path = None;
          anchor = None;
        }
  | "InstanceSetUnion" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "branches" ];
      Instance_set_union
        {
          (* loaded verbatim, one branch and none included: WF-110 owns the
             branch count and the branch fields, so a defective union is a
             citable violation and not a load failure with no rule to name *)
          branches =
            List.mapi
              (fun i x -> instance_set_branch (path / "branches" / string_of_int i) x)
              (list_ (path / "branches") (f "branches"));
          source_name = source_name ();
        }
  | "Rounding" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "base"; "mode"; "decimals"; "type" ];
      Rounding
        {
          base_expr = opt (path / "base") expr (f "base");
          mode_kind = enum Spelling.rounding_mode (path / "mode") (f "mode");
          decimal_places = int_ (path / "decimals") (f "decimals");
          typ = typ ();
          source_name = source_name ();
        }
  | "Clamp" ->
      (* a bound is written whole or not at all: absent is no bound on that
         side, the v0 null *)
      check_keys path kvs ~ext:[ "minimum"; "maximum"; "source" ] [ "node"; "base"; "type" ];
      Clamp
        {
          base_expr = opt (path / "base") expr (f "base");
          minimum = ext_field path kvs "minimum" expr;
          maximum = ext_field path kvs "maximum" expr;
          typ = typ ();
          source_name = source_name ();
        }
  | "AttributeReference" ->
      (* the path is written whole or not at all: absent is the subject's own
         read, and a path without a hop is no navigation *)
      check_keys path kvs ~ext:[ "anchor"; "path"; "source" ] [ "node"; "attribute"; "subjectType" ];
      Attribute_reference
        {
          attribute_ref = str_opt (path / "attribute") (f "attribute");
          object_type = str_opt (path / "subjectType") (f "subjectType");
          source_name = source_name ();
          navigation_bindings =
            ext_field path kvs "path" (fun p j ->
                match List.mapi (fun i x -> role_binding (p / string_of_int i) x) (list_ p j) with
                | [] -> fail p "an empty path is not a navigation"
                | hops -> hops);
          anchor = ext_field path kvs "anchor" anchor;
        }
  | "DimensionCellRead" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "attribute"; "labels" ];
      Dimension_cell_read
        {
          attribute_ref = opt (path / "attribute") expr (f "attribute");
          labels = str_opt_list (path / "labels") (f "labels");
          source_name = source_name ();
        }
  | "ParameterReference" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "parameter" ];
      Parameter_ref { parameter = str_opt (path / "parameter") (f "parameter"); source_name = source_name () }
  | "VariableReference" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "name" ];
      Variable_reference { ident = str_opt (path / "name") (f "name"); source_name = source_name () }
  | "TemporalRead" ->
      (* extension node (v1, row 11): all fields required, none nullable *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "base"; "date"; "type" ];
      Temporal_read
        {
          base_expr = expr (path / "base") (f "base");
          date_value = expr (path / "date") (f "date");
          typ = typ ();
          source_name = source_name ();
        }
  | "Construct" ->
      (* extension node (v1, row 36): argument present (an expr) iff unary,
         explicit null for a nullary constructor; type required not-null *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "constructor"; "argument" ];
      Construct
        {
          typ = typ ();
          constructor_ = str (path / "constructor") (f "constructor");
          argument = opt (path / "argument") expr (f "argument");
          source_name = source_name ();
        }
  | "Match" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "scrutinee"; "arms" ];
      Match
        {
          typ = typ ();
          scrutinee = expr (path / "scrutinee") (f "scrutinee");
          arms =
            List.mapi
              (fun i a ->
                let ap = path / "arms" / string_of_int i in
                let akvs = obj ap a in
                check_keys ap akvs [ "constructor"; "binder"; "body" ];
                ( str (ap / "constructor") (field ap akvs "constructor"),
                  str_opt (ap / "binder") (field ap akvs "binder"),
                  expr (ap / "body") (field ap akvs "body") ))
              (list_ (path / "arms") (f "arms"));
          source_name = source_name ();
        }
  | "BinderRef" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "name" ];
      Binder_ref { ident = str (path / "name") (f "name"); source_name = source_name () }
  | "StructLiteral" ->
      (* extension node (v1, Phase 5c): the fields map name to a value expr,
         in declaration order (WF-073); type required not-null *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "fields" ];
      Struct_literal
        {
          typ = typ ();
          field_list =
            List.mapi
              (fun i fld ->
                let fp = path / "fields" / string_of_int i in
                let fkvs = obj fp fld in
                check_keys fp fkvs [ "name"; "value" ];
                ( str (fp / "name") (field fp fkvs "name"),
                  expr (fp / "value") (field fp fkvs "value") ))
              (list_ (path / "fields") (f "fields"));
          source_name = source_name ();
        }
  | "FieldAccess" ->
      (* extension node (v1, Phase 5c): base struct-typed, field a plain
         string; type required not-null *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "base"; "field" ];
      Field_access
        {
          typ = typ ();
          base = expr (path / "base") (f "base");
          field_name = str (path / "field") (f "field");
          source_name = source_name ();
        }
  | "ListLiteral" ->
      (* extension node (v1, row 10): ordered element expressions, one shared
         element datatype (WF-066); type required not-null *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "elements" ];
      List_literal
        {
          typ = typ ();
          elements =
            List.mapi
              (fun i x -> expr (path / "elements" / string_of_int i) x)
              (list_ (path / "elements") (f "elements"));
          source_name = source_name ();
        }
  | "Lambda" ->
      (* extension node (v1, row 10): a lambda as an ordinary expr operand is
         ill-formed (WF-069); it loads so the verifier can reject it citing
         the rule. As a Fold/MapList function operand it is a [fold_fn]
         instead, parsed below. *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "parameters"; "body" ];
      Lambda
        {
          parameters = lambda_params (path / "parameters") (f "parameters");
          body = expr (path / "body") (f "body");
          source_name = source_name ();
        }
  | "IfThenElse" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "condition"; "then"; "else" ];
      If_then_else
        {
          typ = typ ();
          condition = condition_expr (path / "condition") (f "condition");
          then_ = expr (path / "then") (f "then");
          else_ = expr (path / "else") (f "else");
          source_name = source_name ();
        }
  | "Invoke" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "function"; "arguments" ];
      Invoke
        {
          typ = typ ();
          function_name = str (path / "function") (f "function");
          arguments =
            List.mapi
              (fun i x -> expr (path / "arguments" / string_of_int i) x)
              (list_ (path / "arguments") (f "arguments"));
          source_name = source_name ();
        }
  | "Fold" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list"; "function"; "init" ];
      Fold
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          function_name = fold_fn (path / "function") (f "function");
          init = expr (path / "init") (f "init");
          source_name = source_name ();
        }
  | "MapList" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list"; "function" ];
      Map_list
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          function_name = fold_fn (path / "function") (f "function");
          source_name = source_name ();
        }
  | "FilterList" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list"; "binder"; "condition" ];
      Filter_list
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          binder = str (path / "binder") (f "binder");
          condition = condition_expr (path / "condition") (f "condition");
          source_name = source_name ();
        }
  | "ListLength" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list" ];
      List_length { typ = typ (); list = expr (path / "list") (f "list"); source_name = source_name () }
  | "Let" ->
      (* extension node (v1, Phase 5c). Strict, in this loader's sense of the
         word (never the Let's semantics — ch. 10.6 is call-by-need): type
         required not-null, binder a plain string, value/body non-null exprs *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "binder"; "value"; "body" ];
      Let
        {
          typ = typ ();
          binder = str (path / "binder") (f "binder");
          value = expr (path / "value") (f "value");
          body = expr (path / "body") (f "body");
          source_name = source_name ();
        }
  | "ContextValue" ->
      (* a session value (delta B23): its name is a string whatever it
         spells, carried verbatim — the closed set is WF-098's, crossed below
         for an executor ([reader]) — and its datatype is required *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "name"; "type" ];
      let ident =
        match f "name" with
        | `String s -> s
        | _ -> fail (path / "name") "a ContextValue carries its name as a string"
      in
      Context_value
        {
          ident = Some ident;
          typ = Some (context_type ~refusal:"a ContextValue carries its datatype" path kvs);
          source_name = source_name ();
        }
  | "BoundDay" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type" ];
      Bound_day
        {
          typ = Some (context_type ~refusal:"a BoundDay carries its datatype" path kvs);
          source_name = source_name ();
        }
  | "Unresolved" ->
      (* the honest-marker node. Its type is written whole or not at all:
         absent means unknown, and a null is a load error. Its source is
         optional here: a marker without one is WF-113's, which the verifier
         loads it for, and an executor refuses it after the structural load
         ([executor_refusals]) *)
      check_keys path kvs ~ext:[ "type"; "source" ] [ "node" ];
      Expr_unresolved { typ = ext_field path kvs "type" datatype; source_name = source_name () }
  | k -> fail (path / "node") ("unknown expr node \"" ^ k ^ "\"")

(* One branch of an InstanceSetUnion (WF-110). *)
and instance_set_branch path j : instance_set_branch =
  let kvs = obj path j in
  let f name = field path kvs name in
  expect_node path kvs "InstanceSetBranch";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "elementType"; "sourceRole"; "filter" ];
  {
    isb_element_type = str_opt (path / "elementType") (f "elementType");
    isb_source_role = role_binding (path / "sourceRole") (f "sourceRole");
    isb_filter = opt (path / "filter") condition_expr (f "filter");
    isb_source = source_text path kvs;
  }

(* Lambda / FunctionDef parameters: each carries a name and a datatype (both
   required, non-null — a typed binding). *)
and lambda_params path j : (string * datatype) list =
  List.mapi
    (fun i p ->
      let pp = path / string_of_int i in
      let pkvs = obj pp p in
      check_keys pp pkvs [ "name"; "datatype" ];
      ( str (pp / "name") (field pp pkvs "name"),
        datatype (pp / "datatype") (field pp pkvs "datatype") ))
    (list_ path j)

(* The Fold/MapList [function] operand: a bare string is a FunctionDef name;
   an object is an inline Lambda (parsed here, not via the expr dispatcher —
   the Lambda expr variant is only for the ill-formed misplaced case). *)
and fold_fn path j : fold_fn =
  match j with
  | `String s -> Fn_name s
  | `Assoc _ ->
      let kvs = obj path j in
      expect_node path kvs "Lambda";
      check_keys path kvs ~ext:[ "source" ] [ "node"; "parameters"; "body" ];
      Fn_lambda
        {
          parameters = lambda_params (path / "parameters") (field path kvs "parameters");
          body = expr (path / "body") (field path kvs "body");
          source_name = source_text path kvs;
        }
  | j ->
      fail path
        ("expected a Lambda object or a function-name string, got " ^ Yojson.Safe.to_string j)

and text_part path j : text_part =
  let kvs = obj path j in
  let f name = field path kvs name in
  match node_kind path kvs with
  | "LiteralChunk" ->
      check_keys path kvs [ "node"; "text" ];
      Literal_chunk { body_text = str_opt (path / "text") (f "text") }
  | "EmbeddedExpression" ->
      check_keys path kvs [ "node"; "expr" ];
      Embedded_expression { expr = opt (path / "expr") expr (f "expr") }
  | k -> fail (path / "node") ("unknown TextCompositionPart node \"" ^ k ^ "\"")

and dimension_selection path j : dimension_selection =
  let kvs = obj path j in
  expect_node path kvs "DimensionSelection";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "dimension"; "labels" ];
  {
    ds_dimension = str_opt (path / "dimension") (field path kvs "dimension");
    ds_labels = str_opt_list (path / "labels") (field path kvs "labels");
    ds_source = source_text path kvs;
  }

and condition_expr path j : condition_expr =
  let kvs = obj path j in
  let f name = field path kvs name in
  let source_name () = source_text path kvs in
  (* the whole-period qualifier is written whole or not at all: absent is
     none, the v0 null *)
  let time_period () = ext_field path kvs "wholePeriod" entire_period in
  match node_kind path kvs with
  | "Comparison" ->
      check_keys path kvs ~ext:[ "wholePeriod"; "source" ] [ "node"; "op"; "left"; "right" ];
      Comparison
        {
          op = enum Spelling.comparison_op (path / "op") (f "op");
          left_side = opt (path / "left") expr (f "left");
          right_side = opt (path / "right") expr (f "right");
          time_period = time_period ();
          source_name = source_name ();
        }
  | "Predicate" ->
      (* the day kind is written on a predicate of kind dayKind alone: absent
         elsewhere, the v0 null *)
      check_keys path kvs ~ext:[ "dayKind"; "wholePeriod"; "source" ]
        [ "node"; "kind"; "operand"; "negated"; "digits" ];
      Predicate
        {
          variety = enum Spelling.predicate_kind (path / "kind") (f "kind");
          operand = opt (path / "operand") expr (f "operand");
          day_kind = ext_field path kvs "dayKind" str;
          negation = bool_ (path / "negated") (f "negated");
          digit_chars = int_ (path / "digits") (f "digits");
          time_period = time_period ();
          source_name = source_name ();
        }
  | "CharacteristicCheck" ->
      (* the host is written whole or not at all: absent is the subject
         (RUNTIME-SEMANTICS ch. 5.8b), the v0 null *)
      check_keys path kvs ~ext:[ "host"; "anchor"; "wholePeriod"; "navigation"; "source" ]
        [ "node"; "characteristic"; "negated" ];
      Characteristic_check
        {
          characteristic = str_opt (path / "characteristic") (f "characteristic");
          negation = bool_ (path / "negated") (f "negated");
          holder = ext_field path kvs "host" str;
          time_period = time_period ();
          source_name = source_name ();
          navigation_binding = ext_field path kvs "navigation" role_binding;
          anchor = ext_field path kvs "anchor" anchor;
        }
  | "IdentityCheck" ->
      (* the anchor is required and has one value: read for its value, not
         kept (Ast) *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "anchor"; "negated" ];
      let Anchor_form = anchor (path / "anchor") (f "anchor") in
      Identity_check { negation = bool_ (path / "negated") (f "negated"); source_name = source_name () }
  | "RoleMembership" ->
      check_keys path kvs ~ext:[ "counterpartType"; "source" ] [ "node"; "factType"; "role"; "negated" ];
      Role_membership
        {
          fact_type = str (path / "factType") (f "factType");
          role = str (path / "role") (f "role");
          (* written only where it narrows the other end: absent is the
             declared filler, and a null is a load error *)
          counterpart_type = ext_field path kvs "counterpartType" str;
          negated = bool_ (path / "negated") (f "negated");
          source_name = source_name ();
        }
  | "TimeWindow" ->
      (* a bound is written whole or not at all: absent is open on that
         side, the v0 null; a window with neither is WF-042's *)
      check_keys path kvs ~ext:[ "from"; "to"; "source" ] [ "node"; "toInclusive" ];
      Time_window
        {
          starting_at = ext_field path kvs "from" expr;
          up_to = ext_field path kvs "to" expr;
          to_inclusive = bool_ (path / "toInclusive") (f "toInclusive");
          source_name = source_name ();
        }
  | "Compound" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "quantifier"; "threshold"; "parts" ];
      Compound
        {
          quantifier_kind = enum Spelling.quantifier (path / "quantifier") (f "quantifier");
          cutoff = int_ (path / "threshold") (f "threshold");
          components =
            List.mapi
              (fun i x -> condition_expr (path / "parts" / string_of_int i) x)
              (list_ (path / "parts") (f "parts"));
          source_name = source_name ();
        }
  | "QuantifierFold" ->
      check_keys path kvs ~ext:[ "threshold"; "anchor"; "filter"; "sourceRole"; "sourcePath"; "source" ]
        [ "node"; "quantifier"; "elementType"; "test" ];
      (* the range, exactly one spelling (B12): written, never derived *)
      (match (List.mem_assoc "sourceRole" kvs, List.mem_assoc "sourcePath" kvs) with
      | true, true ->
          fail (path / "sourcePath")
            "sourcePath beside sourceRole: a quantifier fold carries one range, never two"
      | false, false ->
          fail path
            "a quantifier fold carries its range (sourceRole or sourcePath): the range is \
             written, never derived"
      | _ -> ());
      Quantifier_fold
        {
          (* `the` is read in both stances and refused by WF-108 (a); an
             executor refuses it after the structural load *)
          kv_quantifier = enum Spelling.quantifier (path / "quantifier") (f "quantifier");
          (* optional at the LOADER for the same reason [kv_test] is: the
             field's PRESENCE is WF-108 (e)'s demand and its value WF-041's,
             so both are citable violations rather than load failures with no
             rule to name *)
          kv_threshold = ext_field path kvs "threshold" int_;
          kv_element_object_type = str_opt (path / "elementType") (f "elementType");
          (* the subselection is written whole or not at all: absent is none,
             the v0 null *)
          kv_filter = ext_field path kvs "filter" condition_expr;
          (* nullable at the LOADER, required by WF-108 (d): a missing matrix
             is a citable violation, never a load failure with no rule to name
             — the WF-102 discipline on sourcePath, applied to this node's own
             fields *)
          kv_test = opt (path / "test") condition_expr (f "test");
          kv_source = source_name ();
          kv_source_role = ext_field path kvs "sourceRole" role_binding;
          kv_source_path =
            ext_field path kvs "sourcePath" (fun p j ->
                List.mapi (fun i x -> role_binding (p / string_of_int i) x) (list_ p j));
          kv_anchor = ext_field path kvs "anchor" anchor;
        }
  | "Regelversie" ->
      (* the one node that keeps its v0 shape: the rule-status check waits
         for its parity golden (delta B25); only its source text moved into
         the source record *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "regel"; "inconsistent" ];
      Rule_status_check
        {
          rule_def = str_opt (path / "regel") (f "regel");
          inconsistent = bool_ (path / "inconsistent") (f "inconsistent");
          source_name = source_name ();
        }
  | "Unresolved" ->
      (* as with the expression marker: a marker without a source loads for
         the verifier alone *)
      check_keys path kvs ~ext:[ "source" ] [ "node" ];
      Condition_unresolved { source_name = source_name () }
  | k -> fail (path / "node") ("unknown condition node \"" ^ k ^ "\"")

(* --- model layer -------------------------------------------------------- *)

(* An M1 ObjectType node. Its members are Attribute and Characteristic
   nodes, each carrying its own TimelineFacet; the in-memory record keeps
   the name-keyed facet tables (Ast, [object_type]), built here member by
   member in declaration order. An optional key that is absent adds no
   table entry: no plural, no dimension list, no timeline.
   Only an attribute's facet may name a timeline: v0 recorded no timeline
   name for a characteristic, and M1 keeps that position closed (IR-SPEC
   ch. 2.2), so a [timelineName] there is an unknown field, refused with
   its path rather than read and dropped. *)
(* A parameter's facet is read by the same function with [~named:false]:
   no parameter records a timeline name either (IR-SPEC ch. 2.10). *)
let timeline_facet ~named path j =
  let kvs = obj path j in
  expect_node path kvs "TimelineFacet";
  check_keys path kvs
    ~ext:(if named then [ "fixedStart"; "timelineName" ] else [ "fixedStart" ])
    [ "node"; "granularity"; "count" ];
  ( enum Spelling.granularity (path / "granularity") (field path kvs "granularity"),
    { tv_count = Some (int_ (path / "count") (field path kvs "count"));
      tv_fixed_start_point = ext_field path kvs "fixedStart" str },
    ext_field path kvs "timelineName" str )

let object_type path j : string * object_type =
  let kvs = obj path j in
  expect_node path kvs "ObjectType";
  check_keys path kvs ~ext:[ "supertype"; "plural"; "origin"; "designations" ]
    [ "node"; "name"; "animate"; "attributes"; "characteristics" ];
  let f name = field path kvs name in
  let name = str (path / "name") (f "name") in
  let attrs =
    List.mapi
      (fun i a ->
        let ap = path / "attributes" / string_of_int i in
        let akvs = obj ap a in
        expect_node ap akvs "Attribute";
        check_keys ap akvs ~ext:[ "plural"; "dimensions"; "timeline"; "origin"; "designations" ]
          [ "node"; "name"; "datatype" ];
        ( str (ap / "name") (field ap akvs "name"),
          opt (ap / "datatype") datatype (field ap akvs "datatype"),
          ext_field ap akvs "plural" str,
          ext_field ap akvs "dimensions" (fun p j ->
              List.mapi (fun k x -> Some (str (p / string_of_int k) x)) (list_ p j)),
          ext_field ap akvs "timeline" (timeline_facet ~named:true) ))
      (list_ (path / "attributes") (f "attributes"))
  in
  let chars =
    List.mapi
      (fun i c ->
        let cp = path / "characteristics" / string_of_int i in
        let ckvs = obj cp c in
        expect_node cp ckvs "Characteristic";
        check_keys cp ckvs ~ext:[ "timeline"; "origin"; "designations" ] [ "node"; "name"; "kind" ];
        ( str (cp / "name") (field cp ckvs "name"),
          enum Spelling.characteristic_kind (cp / "kind") (field cp ckvs "kind"),
          ext_field cp ckvs "timeline" (timeline_facet ~named:false) ))
      (list_ (path / "characteristics") (f "characteristics"))
  in
  let table get members = List.filter_map (fun m -> get m) members in
  let tl_attr get = table (fun (n, _, _, _, t) -> Option.map (fun t -> (n, get t)) t) attrs in
  let tl_char get = table (fun (n, _, t) -> Option.map (fun t -> (n, get t)) t) chars in
  ( name,
    {
      ot_ident = Some name;
      (* whole or not at all: the extension field is absent or a string,
         never null — a null parent would name a type called nothing *)
      ot_supertype = ext_field path kvs "supertype" str;
      attributes = List.map (fun (n, dt, _, _, _) -> (n, dt)) attrs;
      attribute_plural = table (fun (n, _, pl, _, _) -> Option.map (fun p -> (n, Some p)) pl) attrs;
      attribute_dimensions = table (fun (n, _, _, ds, _) -> Option.map (fun d -> (n, d)) ds) attrs;
      attribute_granularity = tl_attr (fun (g, _, _) -> Some g);
      attribute_timeline_count = tl_attr (fun (_, t, _) -> t);
      attribute_timeline_names =
        table
          (fun (n, _, _, _, t) ->
            match t with Some (_, _, Some tn) -> Some (n, Some tn) | _ -> None)
          attrs;
      characteristics = List.map (fun (n, _, _) -> Some n) chars;
      characteristic_kinds = List.map (fun (n, k, _) -> (n, Some k)) chars;
      characteristic_granularity = tl_char (fun (g, _, _) -> Some g);
      characteristic_timeline_count = tl_char (fun (_, t, _) -> t);
      animate = bool_ (path / "animate") (f "animate");
      plural = ext_field path kvs "plural" str;
    } )

(* A Domain wrapper: the one place a domain's name is written. *)
let domain path j : string * datatype option =
  let kvs = obj path j in
  expect_node path kvs "Domain";
  check_keys path kvs [ "node"; "name"; "datatype" ];
  let name = str (path / "name") (field path kvs "name") in
  (name, Some (domain_datatype name (path / "datatype") (field path kvs "datatype")))

(* A declaration in a model list whose node is not one of L3's: the list has
   no map key, so the node's `name` is the declaration's one name, a non-null
   string since M1 (loader header). The node decoders read `name` into the
   AST's `string option` field, the type every reader of this library uses; a
   null is refused here, so no reader gets [None] from these two lists. *)
let named path (decode : string -> json -> 'a) (ident : 'a -> string option) j : string * 'a =
  let x = decode path j in
  match ident x with
  | Some n -> (n, x)
  | None ->
      fail (path / "name")
        "a declaration in a model list carries its name on its node, and null is no name"

(* The datatype is required and non-null, so [par_datatype] is always
   [Some]; the facet, when present, fills both timeline fields. *)
let parameter path j : string * parameter =
  let kvs = obj path j in
  expect_node path kvs "Parameter";
  check_keys path kvs ~ext:[ "timeline"; "origin"; "designations" ] [ "node"; "name"; "datatype" ];
  let name = str (path / "name") (field path kvs "name") in
  let facet = ext_field path kvs "timeline" (timeline_facet ~named:false) in
  ( name,
    {
      par_name = Some name;
      par_datatype = Some (datatype (path / "datatype") (field path kvs "datatype"));
      par_granularity = Option.map (fun (g, _, _) -> g) facet;
      par_timeline_count = Option.bind facet (fun (_, t, _) -> t.tv_count);
      par_fixed_start_point = Option.bind facet (fun (_, t, _) -> t.tv_fixed_start_point);
    } )

let unit_system path j : string * unit_system =
  let kvs = obj path j in
  expect_node path kvs "UnitSystem";
  check_keys path kvs [ "node"; "name"; "units" ];
  let name = str (path / "name") (field path kvs "name") in
  ( name,
    {
      es_name = Some name;
      unit_list =
        List.mapi
          (fun i u -> declared_unit (path / "units" / string_of_int i) u)
          (list_ (path / "units") (field path kvs "units"));
    } )

let day_kind path j : day_kind =
  let kvs = obj path j in
  expect_node path kvs "DayKind";
  check_keys path kvs ~ext:[ "plural" ] [ "node"; "name" ];
  { dk_name = str (path / "name") (field path kvs "name"); dk_plural = ext_field path kvs "plural" str }

(* A role: its plural is written whole or not at all, absent the v0 null. *)
let role_ref path j : role_ref =
  let kvs = obj path j in
  expect_node path kvs "Role";
  check_keys path kvs ~ext:[ "plural"; "origin"; "designations" ] [ "node"; "name"; "objectType"; "many" ];
  {
    role_identifier = str_opt (path / "name") (field path kvs "name");
    role_object_type_name = str_opt (path / "objectType") (field path kvs "objectType");
    role_plural = ext_field path kvs "plural" str;
    role_many = bool_ (path / "many") (field path kvs "many");
  }

let fact_type_name path j : fact_type_name =
  let kvs = obj path j in
  expect_node path kvs "FactType";
  check_keys path kvs ~ext:[ "origin"; "designations" ] [ "node"; "name"; "reciprocal"; "roles" ];
  {
    ft_name = str_opt (path / "name") (field path kvs "name");
    reciprocal = bool_ (path / "reciprocal") (field path kvs "reciprocal");
    role_names =
      List.mapi
        (fun i x -> role_ref (path / "roles" / string_of_int i) x)
        (list_ (path / "roles") (field path kvs "roles"));
  }

(* A dimension: its plural and its preposition are written whole or not at
   all, absent the v0 null. *)
let dimension path j : dimension =
  let kvs = obj path j in
  expect_node path kvs "Dimension";
  check_keys path kvs ~ext:[ "plural"; "preposition" ] [ "node"; "name"; "labels" ];
  {
    dim_name = str_opt (path / "name") (field path kvs "name");
    dim_name_plural = ext_field path kvs "plural" str;
    preposition = ext_field path kvs "preposition" str;
    dim_values = str_opt_list (path / "labels") (field path kvs "labels");
  }

let timeline_state path j : timeline_state =
  let kvs = obj path j in
  expect_node path kvs "Timeline";
  check_keys path kvs ~ext:[ "fixedStart" ] [ "node"; "name"; "variableStart"; "granularity"; "count" ];
  {
    tl_name = str_opt (path / "name") (field path kvs "name");
    variable_start = bool_ (path / "variableStart") (field path kvs "variableStart");
    tl_granularity = enum_opt Spelling.granularity (path / "granularity") (field path kvs "granularity");
    tl_count = int_ (path / "count") (field path kvs "count");
    tl_fixed_start_point = ext_field path kvs "fixedStart" str;
  }

(* --- rule layer --------------------------------------------------------- *)

(* A validity: each bound is written whole or not at all, absent the v0
   null — an open side; a validity with neither bound is always. *)
let validity_window path j : validity_window =
  let kvs = obj path j in
  expect_node path kvs "Validity";
  check_keys path kvs ~ext:[ "from"; "to" ] [ "node" ];
  { valid_from = ext_field path kvs "from" str; valid_to = ext_field path kvs "to" str }

(* Whether a name ends in a space, '#' and at least one digit: the schema's
   pattern on a form name, which may not end so. *)
let versioned_name name =
  let n = String.length name in
  let is_digit c = c >= '0' && c <= '9' in
  let rec digits k = if k < n && is_digit name.[n - 1 - k] then digits (k + 1) else k in
  let k = digits 0 in
  k >= 1 && n - k >= 2 && name.[n - k - 1] = '#' && name.[n - k - 2] = ' '

(* A named form (IR-SPEC ch. 3, Versioning; delta B19): {node, name,
   versions}. The in-memory records are one per version ([Ast]): a form
   with two or more versions spells them "<name> #<i>" (i from 1), a form
   with one keeps the bare name, and each record carries the pointer of
   its version. That spelling is why a form's own name may not end in a
   space, '#' and digits: it would read as a version of another form. A
   null name is WF-107's and stays null on every version. Returns the
   form's fields (for a field it writes once for all its versions) and,
   per version, its in-memory name, its pointer and its record. *)
let named_form ~node ?(ext = []) path j =
  let kvs = obj path j in
  expect_node path kvs node;
  (* the form's designations (IR-SPEC ch. 2.12) stand on the form, as its
     name does; [vocabularies] reads them *)
  check_keys path kvs ~ext:("origin" :: "designations" :: ext) [ "node"; "name"; "versions" ];
  let name =
    match str_opt (path / "name") (field path kvs "name") with
    | Some n when versioned_name n ->
        fail (path / "name")
          "a form's name does not end in a space, '#' and digits: that spelling names one version \
           of a form with several"
    | name -> name
  in
  let vpath = path / "versions" in
  let versions =
    match list_ vpath (field path kvs "versions") with
    | [] -> fail vpath "a form carries at least one version"
    | vs -> vs
  in
  let several = List.length versions >= 2 in
  ( kvs,
    List.mapi
      (fun i x ->
        let vname =
          if several then Option.map (fun n -> n ^ " #" ^ string_of_int (i + 1)) name else name
        in
        (vname, vpath / string_of_int i, x))
      versions )

(* A rule version's kind is read, never looked up (IR-SPEC ch. 3.1; delta
   B20): a presentation tag is the kind it spells, initialisation true is
   an initialisation, and a version with neither assigns a characteristic.
   Initialisation true beside a presentation is no kind at all. Whether the
   target's member is the kind of member the version writes is WF-022's. *)
let rule_version ~annotations name path j : resolved_rule =
  let kvs = obj path j in
  check_keys path kvs ~ext:[ "condition"; "presentation" ]
    [ "validity"; "target"; "value"; "initialisation" ];
  let f name = field path kvs name in
  let initialisation = bool_ (path / "initialisation") (f "initialisation") in
  let variety =
    match (initialisation, ext_field path kvs "presentation" (enum Spelling.presentation)) with
    | true, Some _ ->
        fail (path / "presentation")
          "presentation beside initialisation true: a presentation tags a computing version, never \
           an initialisation"
    | true, None -> Initialisation
    | false, Some tag -> tag
    | false, None -> Characteristic_attribution
  in
  let target =
    match f "target" with
    | `Assoc tkvs as t when List.assoc_opt "node" tkvs = Some (`String "AttributeReference") ->
        expr (path / "target") t
    | _ -> fail (path / "target") "the target of a rule version is an AttributeReference"
  in
  {
    rr_name = name;
    rr_presentation = Some variety;
    objective = Some target;
    value_node = Some (expr (path / "value") (f "value"));
    rr_condition = ext_field path kvs "condition" condition_expr;
    rr_validity = Some (validity_window (path / "validity") (f "validity"));
    rr_source_annotations = annotations;
    rr_pointer = path;
  }

(* A Rule writes its statute annotations once, on the form; every version's
   record carries that list ([Ast]). An empty list or a blank entry loads:
   both are WF-082's. *)
let rule path j : resolved_rule list =
  let kvs, versions = named_form ~node:"Rule" ~ext:[ "statuteAnnotations" ] path j in
  let annotations =
    ext_field path kvs "statuteAnnotations" (fun p j ->
        List.mapi (fun i x -> str (p / string_of_int i) x) (list_ p j))
  in
  List.map (fun (name, p, x) -> rule_version ~annotations name p x) versions

let resolved_day_kind path j : resolved_day_kind =
  let kvs = obj path j in
  expect_node path kvs "DayKindDefinition";
  check_keys path kvs [ "node"; "dayKind"; "condition"; "validity" ];
  let f name = field path kvs name in
  {
    rd_name = str_opt (path / "dayKind") (f "dayKind");
    rd_condition = opt (path / "condition") condition_expr (f "condition");
    rd_validity = opt (path / "validity") validity_window (f "validity");
  }

let assignment path j : assignment =
  let kvs = obj path j in
  expect_node path kvs "MemberInit";
  check_keys path kvs [ "node"; "member"; "value"; "characteristic" ];
  {
    to_attribute = str_opt (path / "member") (field path kvs "member");
    to_value = opt (path / "value") expr (field path kvs "value");
    to_characteristic = bool_ (path / "characteristic") (field path kvs "characteristic");
  }

let object_creation path j : resolved_object_creation list =
  let _, versions = named_form ~node:"ObjectCreation" path j in
  List.map
    (fun (name, path, j) ->
      let kvs = obj path j in
      check_keys path kvs ~ext:[ "condition"; "connectingRole" ]
        [ "createdType"; "subject"; "initialisations"; "validity" ];
      let f name = field path kvs name in
      {
        oc_name = name;
        created_type = str_opt (path / "createdType") (f "createdType");
        oc_subject = str_opt (path / "subject") (f "subject");
        assignments =
          List.mapi
            (fun i x -> assignment (path / "initialisations" / string_of_int i) x)
            (list_ (path / "initialisations") (f "initialisations"));
        oc_condition = ext_field path kvs "condition" condition_expr;
        oc_validity = Some (validity_window (path / "validity") (f "validity"));
        oc_connecting_role = ext_field path kvs "connectingRole" role_binding;
        oc_pointer = path;
      })
    versions

let allocation path j : resolved_allocation list =
  let _, versions = named_form ~node:"Allocation" path j in
  List.map
    (fun (name, path, j) ->
      let kvs = obj path j in
      check_keys path kvs
        ~ext:[ "ratioAttribute"; "maximumAttribute"; "roundingDecimals"; "remainderAttribute";
               "orderingAttribute"; "condition" ]
        [ "sourceType"; "sourceAttribute"; "targetType"; "targetAttribute"; "method";
          "descending"; "validity"; "connectingRole" ];
      let f name = field path kvs name in
      {
        vd_name = name;
        source_type = str_opt (path / "sourceType") (f "sourceType");
        source_attribute = str_opt (path / "sourceAttribute") (f "sourceAttribute");
        objective_type_name = str_opt (path / "targetType") (f "targetType");
        target_attribute = str_opt (path / "targetAttribute") (f "targetAttribute");
        vd_method = Some (enum Spelling.method_ (path / "method") (f "method"));
        ratio_attribute = ext_field path kvs "ratioAttribute" str;
        maximum_attribute = ext_field path kvs "maximumAttribute" str;
        rounding_decimals = ext_field path kvs "roundingDecimals" int_;
        remainder_attribute = ext_field path kvs "remainderAttribute" str;
        (* the sort key of an ordered allocation (RUNTIME-SEMANTICS
           ch. 4.2), the v0 field it was *)
        ordering_attribute = ext_field path kvs "orderingAttribute" str;
        descending = bool_ (path / "descending") (f "descending");
        vd_condition = ext_field path kvs "condition" condition_expr;
        vd_validity = Some (validity_window (path / "validity") (f "validity"));
        connecting_role = role_binding (path / "connectingRole") (f "connectingRole");
        vd_pointer = path;
      })
    versions

let consistency path j : resolved_consistency list =
  let _, versions = named_form ~node:"Consistency" path j in
  List.map
    (fun (name, path, j) ->
      let kvs = obj path j in
      check_keys path kvs ~ext:[ "source" ] [ "subject"; "assertion"; "validity" ];
      let f name = field path kvs name in
      let source_name, line = source_with_line path kvs in
      {
        cons_name = name;
        cons_subject = str_opt (path / "subject") (f "subject");
        assertion = Some (condition_expr (path / "assertion") (f "assertion"));
        cons_source = source_name;
        cons_line = line;
        cons_validity = Some (validity_window (path / "validity") (f "validity"));
        cons_pointer = path;
      })
    versions

(* One source of a uniqueness version: its object type and its keys. *)
let source path j : string option * string option list =
  let kvs = obj path j in
  expect_node path kvs "Source";
  check_keys path kvs [ "node"; "objectType"; "keys" ];
  let kpath = path / "keys" in
  match list_ kpath (field path kvs "keys") with
  | [] -> fail kpath "a source carries at least one key"
  | keys ->
      ( str_opt (path / "objectType") (field path kvs "objectType"),
        List.mapi (fun i x -> str_opt (kpath / string_of_int i) x) keys )

(* A uniqueness version is one construct on the wire (delta B27) and one of
   two records here: with one source the single-type uniqueness, with two
   or more the union, which carries no scope (a scoped union is an open
   design question, IR-SPEC ch. 3.6). The document splits the two into
   their own lists; the order of the wire's list carries no meaning. *)
let uniqueness path j : (resolved_uniqueness, resolved_uniqueness_union) Either.t list =
  let _, versions = named_form ~node:"Uniqueness" path j in
  List.map
    (fun (name, path, j) ->
      let kvs = obj path j in
      check_keys path kvs ~ext:[ "scope"; "source" ] [ "sources"; "validity" ];
      let f name = field path kvs name in
      let spath = path / "sources" in
      let sources =
        match list_ spath (f "sources") with
        | [] -> fail spath "a uniqueness version carries at least one source"
        | xs -> List.mapi (fun i x -> source (spath / string_of_int i) x) xs
      in
      let validity = Some (validity_window (path / "validity") (f "validity")) in
      let source_name = source_text path kvs in
      match sources with
      | [ (subject, keys) ] ->
          Either.Left
            {
              uni_name = name;
              uni_subject = subject;
              key_list = keys;
              uni_source = source_name;
              uni_validity = validity;
              scope_role = ext_field path kvs "scope" role_binding;
              uni_pointer = path;
            }
      | _ ->
          if List.mem_assoc "scope" kvs then
            fail (path / "scope")
              "a uniqueness version with two or more sources carries no scope: a scoped union is \
               an open design question";
          Either.Right
            {
              uu_name = name;
              source_list =
                List.map (fun (t, keys) -> { uub_type = t; uub_keys = keys }) sources;
              uu_source = source_name;
              uu_validity = validity;
              uu_pointer = path;
            })
    versions

(* The target type of a fact creation, which the wire no longer writes
   (IR-SPEC ch. 3.4): the declared filler of the target role in the created
   fact type, read as WF-028 reads a role — the fact type found with the
   verifier's name identity, and the one of its two roles that the name
   matches. Where that does not resolve the target type stays empty, and
   WF-028 reports the creation. *)
let target_filler fact_types ~fact_type ~role =
  match Checks.find_ci fact_types fact_type with
  | Some ({ role_names = [ a; b ]; _ } : fact_type_name) -> (
      let named (r : role_ref) =
        match r.role_identifier with Some n -> Checks.norm n = Checks.norm role | None -> false
      in
      match (named a, named b) with
      | true, false -> a.role_object_type_name
      | false, true -> b.role_object_type_name
      | _ -> None)
  | _ -> None

(* A fact creation's path is its whole join chain (IR-SPEC ch. 3.4), anchor
   outward: every hop but the last is a [path_step], the last reaches the
   target and is the [join_role]. *)
let fact_creation fact_types path j : resolved_fact_creation list =
  let _, versions = named_form ~node:"FactCreation" path j in
  List.map
    (fun (name, path, j) ->
      let kvs = obj path j in
      check_keys path kvs ~ext:[ "condition" ]
        [ "factType"; "anchorRole"; "targetRole"; "anchor"; "path"; "validity" ];
      let f name = field path kvs name in
      let ppath = path / "path" in
      let steps, join =
        match list_ ppath (f "path") with
        | [] -> fail ppath "a fact creation's path carries at least one hop"
        | xs ->
            let hops = List.mapi (fun i x -> role_binding (ppath / string_of_int i) x) xs in
            let last = List.length hops - 1 in
            (List.filteri (fun i _ -> i < last) hops, List.nth hops last)
      in
      let fact_type = str (path / "factType") (f "factType") in
      let target_role = str (path / "targetRole") (f "targetRole") in
      {
        fc_name = name;
        anchor_point = str_opt (path / "anchor") (f "anchor");
        fc_target = target_filler fact_types ~fact_type ~role:target_role;
        intermediates = List.map (fun rb -> { ts_role = rb }) steps;
        fc_condition = ext_field path kvs "condition" condition_expr;
        fc_validity = Some (validity_window (path / "validity") (f "validity"));
        fc_fact_type = fact_type;
        anchor_role = str (path / "anchorRole") (f "anchorRole");
        objective_role = target_role;
        join_role = join;
        fc_pointer = path;
      })
    versions

let resolved_start_point_determination path j : resolved_start_point_determination =
  let kvs = obj path j in
  expect_node path kvs "StartPointDetermination";
  check_keys path kvs [ "node"; "timeline"; "objectType"; "member" ];
  {
    sp_timeline = str_opt (path / "timeline") (field path kvs "timeline");
    sp_subject_type = str_opt (path / "objectType") (field path kvs "objectType");
    one_member = str_opt (path / "member") (field path kvs "member");
  }

let resolved_rule_group path j : resolved_rule_group =
  let kvs = obj path j in
  expect_node path kvs "RuleGroup";
  check_keys path kvs ~ext:[ "source"; "origin"; "designations" ] [ "node"; "name"; "recursive"; "members" ];
  let f name = field path kvs name in
  let source_name, line = source_with_line path kvs in
  {
    rgr_name = str_opt (path / "name") (f "name");
    recursive = bool_ (path / "recursive") (f "recursive");
    member_list = str_opt_list (path / "members") (f "members");
    rgr_source = source_name;
    rgr_line = line;
  }

(* A diagnostic. Its severity is read in both stances, `error`, `info` and
   null included, which WF-113 (a) refuses by name; an executor refuses them
   after the structural load ([executor_refusals]). *)
let diagnostic path j : diagnostic =
  let kvs = obj path j in
  expect_node path kvs "Diagnostic";
  check_keys path kvs [ "node"; "severity"; "message"; "line" ];
  {
    diag_severity = enum_opt Spelling.severity (path / "severity") (field path kvs "severity");
    message = str_opt (path / "message") (field path kvs "message");
    diag_line = int_ (path / "line") (field path kvs "line");
  }

(* --- the document ------------------------------------------------------- *)

(* A list of statute annotations as the wire writes it (WF-082): strings,
   whatever their content — an empty list or a blank or padded entry loads,
   for WF-082 to name. *)
let annotation_list p j = List.mapi (fun i x -> str (p / string_of_int i) x) (list_ p j)

(* Extension form (Phase 4 first cut): one default of the characteristic
   defaults. Every field but the node is written whole or not at all: an
   absent decision or justification is the v0 null, an absent
   initialisation is false. *)
let rec default path j : default =
  let kvs = obj path j in
  expect_node path kvs "Default";
  check_keys path kvs
    ~ext:
      [ "name"; "decision"; "justification"; "consequence"; "initialisation"; "exceptions"; "source";
        "statuteAnnotations" ]
    [ "node" ];
  (* the block's name carries no semantics (IR-SPEC ch. 6.1): read as the
     string it must be, and kept by no reader here *)
  ignore (ext_field path kvs "name" str : string option);
  (* nor do the block's statute annotations: read as the list of strings
     they must be, kept by no reader here, and judged by WF-082 beside the
     document ([default_annotations]) *)
  ignore (ext_field path kvs "statuteAnnotations" annotation_list : string list option);
  {
    default_decision = ext_field path kvs "decision" bool_;
    default_justification = ext_field path kvs "justification" condition_expr;
    default_consequence = ext_field path kvs "consequence" expr;
    default_initialisation =
      (match ext_field path kvs "initialisation" bool_ with Some b -> b | None -> false);
    default_exceptions = ext_field path kvs "exceptions" exception_groups;
    default_source = source_text path kvs;
  }

(* The nested exception groups riding [exceptions] (v1, Phase 5c;
   RUNTIME-SEMANTICS ch. 7.9). The exceptions object carries no node
   discriminator (it is the extension field's value); each member is an
   [ExceptionGroup] node. Strict: both keys required. *)
and exception_groups path j : exception_groups =
  let kvs = obj path j in
  check_keys path kvs [ "unordered"; "members" ];
  {
    egs_unordered = bool_ (path / "unordered") (field path kvs "unordered");
    egs_members =
      List.mapi
        (fun i x -> exception_group (path / "members" / string_of_int i) x)
        (list_ (path / "members") (field path kvs "members"));
  }

and exception_group path j : exception_group =
  let kvs = obj path j in
  expect_node path kvs "ExceptionGroup";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "defaults" ];
  {
    eg_defaults =
      List.mapi
        (fun i x -> default (path / "defaults" / string_of_int i) x)
        (list_ (path / "defaults") (field path kvs "defaults"));
    eg_source = source_text path kvs;
  }

(* A defaults group: its target, a characteristic or an attribute, and its
   unordered marking are written whole or not at all — an absent
   characteristic is the v0 null, an absent marking false. *)
let prioritized_defaults path j : prioritized_defaults =
  let kvs = obj path j in
  expect_node path kvs "PrioritizedDefaults";
  check_keys path kvs ~ext:[ "characteristic"; "attribute"; "unordered"; "source"; "origin"; "designations" ]
    [ "node"; "name"; "subject"; "defaults"; "validity" ];
  let f name = field path kvs name in
  {
    pd_name = str_opt (path / "name") (f "name");
    pd_subject = str (path / "subject") (f "subject");
    pd_characteristic = ext_field path kvs "characteristic" str;
    pd_attribute = ext_field path kvs "attribute" str;
    defaults =
      List.mapi
        (fun i x -> default (path / "defaults" / string_of_int i) x)
        (list_ (path / "defaults") (f "defaults"));
    pd_unordered = (match ext_field path kvs "unordered" bool_ with Some b -> b | None -> false);
    pd_source = source_text path kvs;
    pd_validity = opt (path / "validity") validity_window (f "validity");
  }

(* Extension form (v1, row 10): a named reusable function (IR-SPEC ch. 2.11).
   Strict: name/parameters/result/body required, source optional. *)
let function_def path j : function_def =
  let kvs = obj path j in
  expect_node path kvs "FunctionDef";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "name"; "parameters"; "result"; "body" ];
  let f name = field path kvs name in
  {
    fd_name = str (path / "name") (f "name");
    fd_parameters = lambda_params (path / "parameters") (f "parameters");
    fd_result = datatype (path / "result") (f "result");
    fd_body = expr (path / "body") (f "body");
    fd_source = source_text path kvs;
  }

(* The JSON pointer of every presentation tag written inside a type
   annotation: the subtree under a `type` key, the key every expression node
   writes its annotation under (the only other `type` in the format is a
   string). The datatype reader validates the tag and drops it, because the
   in-memory datatype does not carry it (Ast header); WF-007 refuses it at
   these positions, so they are collected from the document as written. *)
let presentation_annotations (j : json) : string list =
  let rec walk ~annot path acc = function
    | `Assoc kvs ->
        let acc =
          if annot && List.assoc_opt "node" kvs = Some (`String "Number")
             && List.mem_assoc "presentation" kvs
          then (path / "presentation") :: acc
          else acc
        in
        List.fold_left (fun acc (k, x) -> walk ~annot:(annot || k = "type") (path / k) acc x) acc kvs
    | `List xs ->
        snd
          (List.fold_left
             (fun (i, acc) x -> (i + 1, walk ~annot (path / string_of_int i) acc x))
             (0, acc) xs)
    | _ -> acc
  in
  List.rev (walk ~annot:false "" [] j)

(* --- the vocabulary side table (IR-SPEC ch. 2.12) ------------------------ *)

(* A Vocabulary node of the root list. *)
let vocabulary path j : vocabulary =
  let kvs = obj path j in
  expect_node path kvs "Vocabulary";
  check_keys path kvs ~ext:[ "language" ] [ "node"; "name"; "default" ];
  {
    voc_name = str (path / "name") (field path kvs "name");
    voc_default = bool_ (path / "default") (field path kvs "default");
    voc_language = ext_field path kvs "language" str;
    voc_at = path;
  }

(* A list the side table writes whole or not at all: absent is none, and a
   present list holds at least one entry. *)
let ext_list path kvs key what (decode : string -> json -> 'a) : 'a list =
  match List.assoc_opt key kvs with
  | None -> []
  | Some j -> (
      let lp = path / key in
      match list_ lp j with
      | [] -> fail lp (what ^ " are at least one, or the field is absent")
      | xs -> List.mapi (fun i x -> decode (lp / string_of_int i) x) xs)

(* The designations of the declaration at [path], whose members are [kvs]. *)
let designations path kvs : designation list =
  ext_list path kvs "designations" "a declaration's designations" (fun bp x ->
      let bkvs = obj bp x in
      check_keys bp bkvs [ "vocabulary"; "designation" ];
      {
        ben_vocabulary = str (bp / "vocabulary") (field bp bkvs "vocabulary");
        ben_text = str (bp / "designation") (field bp bkvs "designation");
        ben_at = bp;
      })

(* The origin of the declaration at [path], whose members are [kvs]
   (IR-SPEC ch. 2.13): a closed record whose closed record 'from' carries
   'owner' when written. The form and the kind are read as strings: a value
   outside their sets is WF-121 (a)'s to name, and an executor's to refuse
   ([document]). *)
let origin path kvs : origin option =
  ext_field path kvs "origin" (fun op x ->
      let okvs = obj op x in
      check_keys op okvs [ "form"; "from" ];
      let fp = op / "from" in
      let fkvs = obj fp (field op okvs "from") in
      check_keys fp fkvs ~ext:[ "owner" ] [ "kind"; "name" ];
      {
        hk_form = str (op / "form") (field op okvs "form");
        hk_kind = str (fp / "kind") (field fp fkvs "kind");
        hk_owner = ext_field fp fkvs "owner" str;
        hk_name = str (fp / "name") (field fp fkvs "name");
        hk_at = op;
      })

(* The statute annotations of the defaults (IR-SPEC ch. 6.1, WF-082), read
   from the document as written: the pointer of every `statuteAnnotations`
   field of a Default, the defaults of a group and those of its exception
   groups at any depth, with its entries, in document order. Like the
   vocabulary table below it is no field of [document], on purpose: the
   Catala and F-logic frontends build the default record whole, and no
   reader but the verifier keeps the list ([default] reads the field for its
   structure, for every reader). Read after [document], so the shapes it
   walks are the ones that load. *)
let default_annotations (j : json) : (string * string list) list =
  let kvs = obj "" j in
  let rules = obj "/rules" (field "" kvs "rules") in
  let items p pkvs key =
    match List.assoc_opt key pkvs with
    | None -> []
    | Some x -> List.mapi (fun i y -> (p / key / string_of_int i, y)) (list_ (p / key) x)
  in
  let rec chain p ckvs =
    List.concat_map
      (fun (dp, d) ->
        let dkvs = obj dp d in
        let own =
          match List.assoc_opt "statuteAnnotations" dkvs with
          | None -> []
          | Some a -> [ (dp / "statuteAnnotations", annotation_list (dp / "statuteAnnotations") a) ]
        in
        let nested =
          match List.assoc_opt "exceptions" dkvs with
          | None -> []
          | Some e ->
              let ep = dp / "exceptions" in
              List.concat_map (fun (gp, g) -> chain gp (obj gp g)) (items ep (obj ep e) "members")
        in
        own @ nested)
      (items p ckvs "defaults")
  in
  List.concat_map (fun (gp, g) -> chain gp (obj gp g)) (items "/rules" rules "prioritizedDefaults")

(* The vocabulary side table, read from the document as written: the
   vocabularies of the root list, and every designatable declaration with
   its designations and its origin, those without any included (WF-119 asks
   after them). It
   is read as strictly as every node here — a closed record, a string where
   the schema has one, no empty list — and what a
   designation says is never refused here: that is the verifier's (WF-116 to
   WF-120). An executor calls this for the structure alone ([document]) and
   reads nothing of it for evaluation. A declaration without a name, which
   WF-107 or the model list's own refusal names, carries no designation this
   table can hold, and is left out of it. An enumeration value's
   designations stand in `valueDesignations` beside its list: each entry is
   attached to the first value its `value` names under the name identity of
   ch. 2.1, and an entry that names none is kept apart for WF-117 (f). *)
let vocabularies (j : json) : vocabulary_table =
  let kvs = obj "" j in
  let vocs = ext_list "" kvs "vocabularies" "a document's vocabularies" vocabulary in
  let model = obj "/model" (field "" kvs "model") in
  let rules = obj "/rules" (field "" kvs "rules") in
  let items p pkvs key =
    match List.assoc_opt key pkvs with
    | None -> []
    | Some x -> List.mapi (fun i y -> (p / key / string_of_int i, y)) (list_ (p / key) x)
  in
  let element variety holder ident at ekvs =
    match ident with
    | None -> []
    | Some n ->
        [ { bn_kind = variety; bn_owner = holder; bn_name = n; bn_at = at;
            bn_designations = designations at ekvs; bn_origin = origin at ekvs } ]
  in
  let name_of p ekvs = str_opt (p / "name") (field p ekvs "name") in
  let object_types =
    List.concat_map
      (fun (p, x) ->
        let okvs = obj p x in
        let ident = name_of p okvs in
        let member_list variety key =
          List.concat_map
            (fun (lp, l) -> let lkvs = obj lp l in element variety ident (name_of lp lkvs) lp lkvs)
            (items p okvs key)
        in
        element Bn_object_type None ident p okvs
        @ member_list Bn_attribute "attributes" @ member_list Bn_characteristic "characteristics")
      (items "/model" model "objectTypes")
  in
  let fact_types =
    List.concat_map
      (fun (p, x) ->
        let fkvs = obj p x in
        let ident = name_of p fkvs in
        element Bn_fact_type None ident p fkvs
        @ List.concat_map
            (fun (rp, r) -> let rkvs = obj rp r in element Bn_role ident (name_of rp rkvs) rp rkvs)
            (items p fkvs "roles"))
      (items "/model" model "factTypes")
  in
  let parameters =
    List.concat_map
      (fun (p, x) -> let pkvs = obj p x in element Bn_parameter None (name_of p pkvs) p pkvs)
      (items "/model" model "parameters")
  in
  let separate = ref [] in
  let label_list =
    List.concat_map
      (fun (p, x) ->
        let dkvs = obj p x in
        let domain_spec = str (p / "name") (field p dkvs "name") in
        let dp = p / "datatype" in
        let dtkvs = obj dp (field p dkvs "datatype") in
        match List.assoc_opt "node" dtkvs with
        | Some (`String "Enumeration") ->
            let label_list = str_opt_list (dp / "values") (field dp dtkvs "values") in
            let entries =
              ext_list dp dtkvs "valueDesignations" "an enumeration's value designations"
                (fun ep e ->
                  let ekvs = obj ep e in
                  check_keys ep ekvs [ "value"; "vocabulary"; "designation" ];
                  ( str (ep / "value") (field ep ekvs "value"),
                    {
                      ben_vocabulary = str (ep / "vocabulary") (field ep ekvs "vocabulary");
                      ben_text = str (ep / "designation") (field ep ekvs "designation");
                      ben_at = ep;
                    } ))
            in
            (* each entry to the first value it names *)
            let leading w =
              let rec search i = function
                | [] -> None
                | Some v :: _ when Checks.norm v = Checks.norm w -> Some i
                | _ :: rest -> search (i + 1) rest
              in
              search 0 label_list
            in
            List.iter
              (fun (w, b) ->
                if leading w = None then separate := (domain_spec, w, b.ben_at / "value") :: !separate)
              entries;
            List.concat
              (List.mapi
                 (fun i v ->
                   match v with
                   | None -> []
                   | Some v ->
                       [ { bn_kind = Bn_enumeration_value; bn_owner = Some domain_spec; bn_name = v;
                           bn_at = dp / "values" / string_of_int i; bn_origin = None;
                           bn_designations =
                             List.filter_map
                               (fun (w, b) -> if leading w = Some i then Some b else None)
                               entries } ])
                 label_list)
        | _ -> [])
      (items "/model" model "domains")
  in
  let form_list =
    List.concat_map
      (fun key ->
        List.concat_map
          (fun (p, x) -> let vkvs = obj p x in element Bn_form None (name_of p vkvs) p vkvs)
          (items "/rules" rules key))
      [ "rules"; "objectCreations"; "allocations"; "consistencies"; "uniquenesses"; "factCreations";
        "prioritizedDefaults" ]
  in
  let buckets =
    List.concat_map
      (fun (p, x) -> let gkvs = obj p x in element Bn_rule_group None (name_of p gkvs) p gkvs)
      (items "/rules" rules "ruleGroups")
  in
  {
    vt_vocabularies = vocs;
    vt_designatable = object_types @ fact_types @ parameters @ label_list @ form_list @ buckets;
    vt_separate_values = List.rev !separate;
  }

(* Who loads the document. Two defects on the wire are a verifier rule's,
   because the schema cannot cross them: a literal whose value variant is
   not the one its type names (WF-114) and a context value whose name is
   outside the closed set (WF-098). The [Verifier] loads both, so that the
   rule cites them. Every other reader of this library — an executor or a
   frontend — is an [Executor] and refuses both at load, in the rule's own
   sentence without its number ([Checks.literal_variant_refusal],
   [Checks.context_value_refusal]): no evaluator reads {"text": "5"} under
   a Number as a number, or a session value that does not exist. The string
   of a number, date or dateTime variant outside its grammar (WF-081,
   WF-084, WF-095) is refused the same way ([Checks.number_lexeme_refusal],
   [Checks.date_lexeme_refusal], [Checks.date_time_lexeme_refusal]): an
   executor that met it only at evaluation would refuse the same document in
   another phase and in other words than the verifier and the Drools loader.
   So is an Unresolved marker without a source (WF-113 (b)): the verifier
   loads it so that the rule names it, and an executor refuses it in the rule's
   sentence ([Checks.marker_without_source]), since no report can account for
   a marker that has no text to quote. And so are the three values the schema
   does not admit and a verifier rule refuses by name: the quantifier `the` on
   a QuantifierFold (WF-108 (a), [Checks.quantifier_fold_the_refusal]) and a
   severity `error`, `info` or null (WF-113 (a), [Checks.severity_refusal]).
   So is a VariableReference (WF-034, [Checks.variable_reference_refusal]):
   the marker of a variable the frontend could not bind resolves against
   nothing, and an executor that met it only at evaluation would refuse it in
   a residual arm's words. And so is a fixed start point that does not fit its
   grid (WF-115, [Checks.wf115_findings]): an executor lays the periods
   from it, and one it cannot read or that contradicts the timeline has no
   grid to lay. *)
type reader = Verifier | Executor

(* The executor's refusals, after the structural load: every Literal,
   ContextValue, VariableReference, Unresolved, QuantifierFold and Diagnostic node of the
   document as written, in document order, a literal's variant crossed
   against its type before its string is read. A literal without a type is
   WF-034's typeless value node, with no type to cross its variant against;
   its string is read all the same. *)
let executor_refusals (j : json) =
  let rec walk path = function
    | `Assoc kvs ->
        (match List.assoc_opt "node" kvs with
        | Some (`String "Literal") -> (
            let variant = literal_variant (path / "value") (field path kvs "value") in
            (match opt (path / "type") datatype (field path kvs "type") with
            | None -> ()
            | Some typ -> (
                match Checks.literal_variant_refusal variant typ with
                | Some sentence -> fail (path / "value") sentence
                | None -> ()));
            let spelling =
              match variant with
              | Lv_number w -> Checks.number_lexeme_refusal w
              | Lv_date w -> Checks.date_lexeme_refusal w
              | Lv_date_time w -> Checks.date_time_lexeme_refusal w
              | Lv_boolean _ | Lv_text _ | Lv_enumeration _ | Lv_constructor _ -> None
            in
            match spelling with Some sentence -> fail (path / "value") sentence | None -> ())
        | Some (`String "ContextValue") -> (
            match Checks.context_value_refusal (str (path / "name") (field path kvs "name")) with
            | Some sentence -> fail (path / "name") sentence
            | None -> ())
        | Some (`String "VariableReference") ->
            let name = match List.assoc_opt "name" kvs with Some (`String s) -> Some s | _ -> None in
            fail path (Checks.variable_reference_refusal name)
        | Some (`String "Unresolved") ->
            if not (List.mem_assoc "source" kvs) then fail path Checks.marker_without_source
        | Some (`String "QuantifierFold") -> (
            match field path kvs "quantifier" with
            | `String s when Spelling.is_refused Spelling.quantifier s ->
                fail (path / "quantifier") Checks.quantifier_fold_the_refusal
            | _ -> ())
        | Some (`String "Diagnostic") -> (
            let severity =
              match field path kvs "severity" with
              | `String s -> Spelling.decode Spelling.severity s
              | _ -> None
            in
            match Checks.severity_refusal severity with
            | Some sentence -> fail (path / "severity") sentence
            | None -> ())
        | _ -> ());
        List.iter (fun (k, x) -> walk (path / k) x) kvs
    | `List xs -> List.iteri (fun i x -> walk (path / string_of_int i) x) xs
    | _ -> ()
  in
  walk "" j

(* The M1 root: {format, model, rules, diagnostics}. [model] is the
   declaration set as lists, [rules] the rule lists under their L5 keys;
   each carries one optional extension list. Since L4 the six named forms
   are read version by version into the v0-shaped records, and the one wire
   list of uniquenesses into the two in-memory lists. *)
let document ?(reader = Executor) (j : json) : document =
  let path = "" in
  let kvs = obj path j in
  check_keys path kvs ~ext:[ "vocabularies" ] [ "format"; "model"; "rules"; "diagnostics" ];
  (match str (path / "format") (field path kvs "format") with
  | "uchar-ir-v1" -> ()
  | s -> fail (path / "format") ("unknown format \"" ^ s ^ "\""));
  let mpath = path / "model" in
  let model = obj mpath (field path kvs "model") in
  check_keys mpath model ~ext:[ "functionDefs" ]
    [ "objectTypes"; "factTypes"; "parameters"; "domains"; "unitSystems"; "dimensions";
      "timelines"; "dayKinds" ];
  let rpath = path / "rules" in
  let rules = obj rpath (field path kvs "rules") in
  check_keys rpath rules ~ext:[ "prioritizedDefaults" ]
    [ "rules"; "dayKindDefinitions"; "objectCreations"; "allocations"; "consistencies";
      "uniquenesses"; "factCreations"; "startPointDeterminations"; "ruleGroups" ];
  let arr p kvs name g =
    List.mapi (fun i x -> g (p / name / string_of_int i) x) (list_ (p / name) (field p kvs name))
  in
  let ext p kvs name g =
    match List.assoc_opt name kvs with
    | Some j -> List.mapi (fun i x -> g (p / name / string_of_int i) x) (list_ (p / name) j)
    | None -> []
  in
  let m name g = arr mpath model name g and r name g = arr rpath rules name g in
  let forms name g = List.concat (r name g) in
  (* the fact types first: a fact creation's target type is read from them *)
  let fact_types = m "factTypes" (fun p -> named p fact_type_name (fun (x : fact_type_name) -> x.ft_name)) in
  let uniqueness_versions = forms "uniquenesses" uniqueness in
  let doc =
    {
      object_types = m "objectTypes" object_type;
      domain_specs = m "domains" domain;
      parameters = m "parameters" parameter;
      unit_systems = m "unitSystems" unit_system;
      fact_types;
      dimensions = m "dimensions" (fun p -> named p dimension (fun (x : dimension) -> x.dim_name));
      timelines = m "timelines" timeline_state;
      day_kinds = m "dayKinds" day_kind;
      resolved_rules = forms "rules" rule;
      resolved_day_kind_definitions = r "dayKindDefinitions" resolved_day_kind;
      object_creations = forms "objectCreations" object_creation;
      allocations = forms "allocations" allocation;
      consistencies = forms "consistencies" consistency;
      uniquenesses = List.filter_map Either.find_left uniqueness_versions;
      uniqueness_unions = List.filter_map Either.find_right uniqueness_versions;
      fact_creations = forms "factCreations" (fact_creation fact_types);
      resolved_start_point_determinations = r "startPointDeterminations" resolved_start_point_determination;
      rule_groups = r "ruleGroups" resolved_rule_group;
      diagnostics = arr path kvs "diagnostics" diagnostic;
      prioritized_defaults = ext rpath rules "prioritizedDefaults" prioritized_defaults;
      function_defs = ext mpath model "functionDefs" function_def;
      presentation_annotations = presentation_annotations j;
    }
  in
  (* the side table is read structurally by every reader, and for its
     content by none here (IR-SPEC ch. 2.12 and 2.13), except that an
     executor cannot keep an origin whose form or kind is no value of its
     set (WF-121 (a)) *)
  let grid = vocabularies j in
  (match reader with
  | Executor -> (
      executor_refusals j;
      List.iter
        (fun (b : designatable) ->
          match Option.bind b.bn_origin Checks.origin_refusal with
          | Some (at, sentence) -> fail at sentence
          | None -> ())
        grid.vt_designatable;
      match Checks.wf115_findings doc with (at, sentence) :: _ -> fail at sentence | [] -> ())
  | Verifier -> ());
  doc

let load_file ?reader (filename : string) : document =
  document ?reader (Yojson.Safe.from_file filename)
