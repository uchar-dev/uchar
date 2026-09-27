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
   number as a [Numeriek], which carries no presentation.

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
   line?}, optional on every node and read onto the in-memory `bron` fields
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
let rol_binding path j : rol_binding =
  let kvs = obj path j in
  expect_node path kvs "RoleBinding";
  check_keys path kvs [ "node"; "factType"; "role" ];
  {
    rb_feittype = str (path / "factType") (field path kvs "factType");
    rb_rol = str (path / "role") (field path kvs "role");
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
let rationaal path j : rationaal =
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
let rec unit_fields ~extra path kvs : eenheid =
  expect_node path kvs "Unit";
  check_keys path kvs
    ~ext:([ "system"; "canonical"; "factorToCanonical"; "denominator" ] @ extra)
    [ "node"; "abbreviation" ];
  {
    afkorting = Some (str (path / "abbreviation") (field path kvs "abbreviation"));
    stelsel = ext_field path kvs "system" str;
    canoniek = ext_field path kvs "canonical" str;
    factor_naar_canoniek = ext_field path kvs "factorToCanonical" rationaal;
    noemer = ext_field path kvs "denominator" eenheid;
  }

and eenheid path j : eenheid = unit_fields ~extra:[] path (obj path j)

(* A unit as its unit system declares it, with its further spellings. The
   list is written whole or not at all: absent is no spelling, and an empty
   list is refused like an explicit null. *)
let gedeclareerde_eenheid path j : gedeclareerde_eenheid =
  let kvs = obj path j in
  let u = unit_fields ~extra:[ "spellings" ] path kvs in
  let spellings p j =
    match list_ p j with
    | [] -> fail p "a unit's spellings are at least one spelling, or the field is absent"
    | xs -> List.mapi (fun i x -> str (p / string_of_int i) x) xs
  in
  {
    ge_eenheid = u;
    ge_spellingen = Option.value ~default:[] (ext_field path kvs "spellings" spellings);
  }

let rec datatype path j : datatype =
  let kvs = obj path j in
  let f name = field path kvs name in
  match node_kind path kvs with
  | "Number" ->
      (* the in-memory form keeps the two v0 constructors (Ast header): a
         unit '%' makes the number a Percentage, whatever else it carries *)
      check_keys path kvs ~ext:[ "unit"; "presentation" ] [ "node"; "whole"; "decimals"; "sign" ];
      let geheel = bool_ (path / "whole") (f "whole")
      and decimalen = int_ (path / "decimals") (f "decimals")
      and sign = enum Spelling.sign (path / "sign") (f "sign")
      and unit_ = ext_field path kvs "unit" eenheid
      and tag = ext_field path kvs "presentation" presentation in
      (match unit_ with
       | Some ({ afkorting = Some "%"; _ } as u) ->
           if sign <> Onbepaald then
             fail (path / "sign")
               "a percentage carries no sign restriction: a number whose unit is '%' has the \
                sign unspecified";
           Percentage { geheel; decimalen; eenheid = Some u }
       | _ ->
           if tag <> None then
             fail (path / "presentation")
               "the presentation \"percentage\" on a number whose unit is not '%'";
           Numeriek { geheel; decimalen; teken = Some sign; eenheid = unit_ })
  | "Text" -> check_keys path kvs [ "node" ]; Tekst
  | "Boolean" -> check_keys path kvs [ "node" ]; Booleaan
  | "Date" ->
      check_keys path kvs [ "node"; "precision" ];
      enum Spelling.precision (path / "precision") (f "precision")
  | "Enumeration" ->
      check_keys path kvs [ "node"; "domain"; "values" ];
      Enumeratie
        {
          domein = str_opt (path / "domain") (f "domain");
          waarden = str_opt_list (path / "values") (f "values");
        }
  | "SumType" ->
      (* extension node (v1, row 36): domain is a plain string; each
         constructor carries a name and a payload datatype or explicit null
         (nullary). Payloads recurse through [datatype] (nested sum types). *)
      check_keys path kvs [ "node"; "domain"; "constructors" ];
      Sum_type
        {
          domein = str (path / "domain") (f "domain");
          constructoren = sum_constructors (path / "constructors") (f "constructors");
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
        { domein = str (path / "domain") (f "domain"); velden = struct_fields (path / "fields") (f "fields") }
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
         ch. 2.12); [vocabulaires] reads them *)
      check_keys path kvs ~ext:[ "valueDesignations" ] [ "node"; "values" ];
      Enumeratie { domein = Some name; waarden = str_opt_list (path / "values") (f "values") }
  | "SumType" ->
      check_keys path kvs [ "node"; "constructors" ];
      Sum_type
        { domein = name; constructoren = sum_constructors (path / "constructors") (f "constructors") }
  | "StructType" ->
      check_keys path kvs [ "node"; "fields" ];
      Struct_type { domein = name; velden = struct_fields (path / "fields") (f "fields") }
  | _ -> datatype path j

(* The whole-period qualifier (IR-SPEC ch. 4.5): its granularity, read onto
   the in-memory flag [gp_jaar] (year true, month false), and its negation. *)
let geheel_periode path j : geheel_periode =
  let kvs = obj path j in
  expect_node path kvs "WholePeriod";
  check_keys path kvs [ "node"; "granularity"; "negated" ];
  {
    gp_jaar =
      enum Spelling.whole_period_granularity (path / "granularity") (field path kvs "granularity");
    gp_niet = bool_ (path / "negated") (field path kvs "negated");
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

let rec expr path j : expr =
  let kvs = obj path j in
  let f name = field path kvs name in
  let typ () = opt (path / "type") datatype (f "type") in
  let bron () = source_text path kvs in
  let arguments () =
    List.mapi
      (fun i x -> expr (path / "arguments" / string_of_int i) x)
      (list_ (path / "arguments") (f "arguments"))
  in
  match node_kind path kvs with
  | "Literal" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "value"; "type" ];
      let variant = literal_variant (path / "value") (f "value") in
      Literal { variant; typ = typ (); bron = bron () }
  | "TextComposition" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "parts"; "type" ];
      Tekst_reeks
        {
          delen =
            List.mapi
              (fun i x -> tekst_deel (path / "parts" / string_of_int i) x)
              (list_ (path / "parts") (f "parts"));
          typ = typ ();
          bron = bron ();
        }
  | "Binary" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "op"; "left"; "right"; "type" ];
      Binair
        {
          op = enum Spelling.binary_op (path / "op") (f "op");
          links = opt (path / "left") expr (f "left");
          rechts = opt (path / "right") expr (f "right");
          typ = typ ();
          bron = bron ();
        }
  | "Aggregation" ->
      (* the eight folds (delta B21), read into the in-memory [Functie] with
         no granularity: that field is a builtin's and unknown here *)
      check_keys path kvs
        ~ext:[ "elementType"; "filter"; "emptyFallback"; "dimensionSelection"; "sourceRole";
               "sourcePath"; "source" ]
        [ "node"; "kind"; "arguments"; "type" ];
      let kind = enum Spelling.aggregation_kind (path / "kind") (f "kind") in
      let argumenten = arguments () in
      let element_objecttype = ext_field path kvs "elementType" str in
      (* the range (B12): one spelling at most, and a single-argument fold
         with an elementType carries one under every kind but total, where
         its absence is the subject's own read *)
      (match (List.mem_assoc "sourceRole" kvs, List.mem_assoc "sourcePath" kvs) with
      | true, true ->
          fail (path / "sourcePath")
            "sourcePath beside sourceRole: a fold carries one range, never two"
      | false, false
        when element_objecttype <> None && List.length argumenten = 1 && kind <> Totaal ->
          fail path
            "a single-argument fold with an elementType carries its range (sourceRole or \
             sourcePath) under every kind but total: the range is written, never derived"
      | _ -> ());
      Functie
        {
          kind;
          argumenten;
          element_objecttype;
          leegwaarde = ext_field path kvs "emptyFallback" expr;
          filter = ext_field path kvs "filter" conditie;
          dimensie_selectie = ext_field path kvs "dimensionSelection" dimensie_selectie;
          typ = typ ();
          bron = bron ();
          granulariteit = None;
          bron_rol = ext_field path kvs "sourceRole" rol_binding;
          (* loaded verbatim, empty list included: WF-102 (b) owns the
             arity, so a defective path is a citable violation and not a
             load failure with no rule to name *)
          bron_pad =
            ext_field path kvs "sourcePath" (fun p j ->
                List.mapi (fun i x -> rol_binding (p / string_of_int i) x) (list_ p j));
        }
  | "BuiltinCall" ->
      (* the seventeen other kinds (delta B21), with plain arguments: an
         element type, a range, a fallback or a selection is a fold's field
         and unknown here *)
      check_keys path kvs ~ext:[ "filter"; "granularity"; "source" ]
        [ "node"; "kind"; "arguments"; "type" ];
      let kind = enum Spelling.builtin_kind (path / "kind") (f "kind") in
      Functie
        {
          kind;
          argumenten = arguments ();
          element_objecttype = None;
          leegwaarde = None;
          filter = ext_field path kvs "filter" conditie;
          dimensie_selectie = None;
          typ = typ ();
          bron = bron ();
          granulariteit = ext_field path kvs "granularity" (enum Spelling.granularity);
          bron_rol = None;
          bron_pad = None;
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
          bron = bron ();
        }
  | "Rounding" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "base"; "mode"; "decimals"; "type" ];
      Afgerond
        {
          basis = opt (path / "base") expr (f "base");
          modus = enum Spelling.rounding_mode (path / "mode") (f "mode");
          decimalen = int_ (path / "decimals") (f "decimals");
          typ = typ ();
          bron = bron ();
        }
  | "Clamp" ->
      (* a bound is written whole or not at all: absent is no bound on that
         side, the v0 null *)
      check_keys path kvs ~ext:[ "minimum"; "maximum"; "source" ] [ "node"; "base"; "type" ];
      Begrensd
        {
          basis = opt (path / "base") expr (f "base");
          minimum = ext_field path kvs "minimum" expr;
          maximum = ext_field path kvs "maximum" expr;
          typ = typ ();
          bron = bron ();
        }
  | "AttributeReference" ->
      (* the path is written whole or not at all: absent is the subject's own
         read, and a path without a hop is no navigation *)
      check_keys path kvs ~ext:[ "path"; "source" ] [ "node"; "attribute"; "subjectType" ];
      Attribuut_ref
        {
          attribuut = str_opt (path / "attribute") (f "attribute");
          objecttype = str_opt (path / "subjectType") (f "subjectType");
          bron = bron ();
          navigatie_bindingen =
            ext_field path kvs "path" (fun p j ->
                match List.mapi (fun i x -> rol_binding (p / string_of_int i) x) (list_ p j) with
                | [] -> fail p "an empty path is not a navigation"
                | hops -> hops);
        }
  | "DimensionCellRead" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "attribute"; "labels" ];
      Dimensie_cel
        {
          attribuut = opt (path / "attribute") expr (f "attribute");
          labels = str_opt_list (path / "labels") (f "labels");
          bron = bron ();
        }
  | "ParameterReference" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "parameter" ];
      Parameter_ref { parameter = str_opt (path / "parameter") (f "parameter"); bron = bron () }
  | "VariableReference" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "name" ];
      Variabele_ref { naam = str_opt (path / "name") (f "name"); bron = bron () }
  | "TemporalRead" ->
      (* extension node (v1, row 11): all fields required, none nullable *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "base"; "date"; "type" ];
      Temporal_read
        {
          basis = expr (path / "base") (f "base");
          datum = expr (path / "date") (f "date");
          typ = typ ();
          bron = bron ();
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
          bron = bron ();
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
          bron = bron ();
        }
  | "BinderRef" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "name" ];
      Binder_ref { naam = str (path / "name") (f "name"); bron = bron () }
  | "StructLiteral" ->
      (* extension node (v1, Phase 5c): the fields map name to a value expr,
         in declaration order (WF-073); type required not-null *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "fields" ];
      Struct_literal
        {
          typ = typ ();
          velden =
            List.mapi
              (fun i fld ->
                let fp = path / "fields" / string_of_int i in
                let fkvs = obj fp fld in
                check_keys fp fkvs [ "name"; "value" ];
                ( str (fp / "name") (field fp fkvs "name"),
                  expr (fp / "value") (field fp fkvs "value") ))
              (list_ (path / "fields") (f "fields"));
          bron = bron ();
        }
  | "FieldAccess" ->
      (* extension node (v1, Phase 5c): base struct-typed, field a plain
         string; type required not-null *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "base"; "field" ];
      Field_access
        {
          typ = typ ();
          base = expr (path / "base") (f "base");
          veld = str (path / "field") (f "field");
          bron = bron ();
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
          bron = bron ();
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
          bron = bron ();
        }
  | "IfThenElse" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "condition"; "then"; "else" ];
      If_then_else
        {
          typ = typ ();
          condition = conditie (path / "condition") (f "condition");
          then_ = expr (path / "then") (f "then");
          else_ = expr (path / "else") (f "else");
          bron = bron ();
        }
  | "Invoke" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "function"; "arguments" ];
      Invoke
        {
          typ = typ ();
          functie = str (path / "function") (f "function");
          arguments =
            List.mapi
              (fun i x -> expr (path / "arguments" / string_of_int i) x)
              (list_ (path / "arguments") (f "arguments"));
          bron = bron ();
        }
  | "Fold" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list"; "function"; "init" ];
      Fold
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          functie = fold_fn (path / "function") (f "function");
          init = expr (path / "init") (f "init");
          bron = bron ();
        }
  | "MapList" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list"; "function" ];
      Map_list
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          functie = fold_fn (path / "function") (f "function");
          bron = bron ();
        }
  | "FilterList" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list"; "binder"; "condition" ];
      Filter_list
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          binder = str (path / "binder") (f "binder");
          condition = conditie (path / "condition") (f "condition");
          bron = bron ();
        }
  | "ListLength" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type"; "list" ];
      List_length { typ = typ (); list = expr (path / "list") (f "list"); bron = bron () }
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
          bron = bron ();
        }
  | "ContextValue" ->
      (* a session value (delta B23): its name is a string whatever it
         spells, carried verbatim — the closed set is WF-098's, crossed below
         for an executor ([reader]) — and its datatype is required *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "name"; "type" ];
      let naam =
        match f "name" with
        | `String s -> s
        | _ -> fail (path / "name") "a ContextValue carries its name as a string"
      in
      Constante
        {
          naam = Some naam;
          typ = Some (context_type ~refusal:"a ContextValue carries its datatype" path kvs);
          bron = bron ();
        }
  | "BoundDay" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "type" ];
      Bound_day
        {
          typ = Some (context_type ~refusal:"a BoundDay carries its datatype" path kvs);
          bron = bron ();
        }
  | "Unresolved" ->
      (* the honest-marker node. Its type is written whole or not at all:
         absent means unknown, and a null is a load error. Its source is
         optional here: a marker without one is WF-113's, which the verifier
         loads it for, and an executor refuses it after the structural load
         ([executor_refusals]) *)
      check_keys path kvs ~ext:[ "type"; "source" ] [ "node" ];
      Expr_onopgelost { typ = ext_field path kvs "type" datatype; bron = bron () }
  | k -> fail (path / "node") ("unknown expr node \"" ^ k ^ "\"")

(* One branch of an InstanceSetUnion (WF-110). *)
and instance_set_branch path j : instance_set_branch =
  let kvs = obj path j in
  let f name = field path kvs name in
  expect_node path kvs "InstanceSetBranch";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "elementType"; "sourceRole"; "filter" ];
  {
    isb_element_type = str_opt (path / "elementType") (f "elementType");
    isb_source_role = rol_binding (path / "sourceRole") (f "sourceRole");
    isb_filter = opt (path / "filter") conditie (f "filter");
    isb_bron = source_text path kvs;
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
          bron = source_text path kvs;
        }
  | j ->
      fail path
        ("expected a Lambda object or a function-name string, got " ^ Yojson.Safe.to_string j)

and tekst_deel path j : tekst_deel =
  let kvs = obj path j in
  let f name = field path kvs name in
  match node_kind path kvs with
  | "LiteralChunk" ->
      check_keys path kvs [ "node"; "text" ];
      Letterlijk { tekst = str_opt (path / "text") (f "text") }
  | "EmbeddedExpression" ->
      check_keys path kvs [ "node"; "expr" ];
      Ingevoegd { expr = opt (path / "expr") expr (f "expr") }
  | k -> fail (path / "node") ("unknown TextCompositionPart node \"" ^ k ^ "\"")

and dimensie_selectie path j : dimensie_selectie =
  let kvs = obj path j in
  expect_node path kvs "DimensionSelection";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "dimension"; "labels" ];
  {
    ds_dimensie = str_opt (path / "dimension") (field path kvs "dimension");
    ds_labels = str_opt_list (path / "labels") (field path kvs "labels");
    ds_bron = source_text path kvs;
  }

and conditie path j : conditie =
  let kvs = obj path j in
  let f name = field path kvs name in
  let bron () = source_text path kvs in
  (* the whole-period qualifier is written whole or not at all: absent is
     none, the v0 null *)
  let periode () = ext_field path kvs "wholePeriod" geheel_periode in
  match node_kind path kvs with
  | "Comparison" ->
      check_keys path kvs ~ext:[ "wholePeriod"; "source" ] [ "node"; "op"; "left"; "right" ];
      Vergelijking
        {
          op = enum Spelling.comparison_op (path / "op") (f "op");
          links = opt (path / "left") expr (f "left");
          rechts = opt (path / "right") expr (f "right");
          periode = periode ();
          bron = bron ();
        }
  | "Predicate" ->
      (* the day kind is written on a predicate of kind dayKind alone: absent
         elsewhere, the v0 null *)
      check_keys path kvs ~ext:[ "dayKind"; "wholePeriod"; "source" ]
        [ "node"; "kind"; "operand"; "negated"; "digits" ];
      Predicaat
        {
          soort = enum Spelling.predicate_kind (path / "kind") (f "kind");
          operand = opt (path / "operand") expr (f "operand");
          dagsoort = ext_field path kvs "dayKind" str;
          negatie = bool_ (path / "negated") (f "negated");
          cijfers = int_ (path / "digits") (f "digits");
          periode = periode ();
          bron = bron ();
        }
  | "CharacteristicCheck" ->
      (* the host is written whole or not at all: absent is the subject
         (RUNTIME-SEMANTICS ch. 5.8b), the v0 null *)
      check_keys path kvs ~ext:[ "host"; "wholePeriod"; "navigation"; "source" ]
        [ "node"; "characteristic"; "negated" ];
      Kenmerk_check
        {
          kenmerk = str_opt (path / "characteristic") (f "characteristic");
          negatie = bool_ (path / "negated") (f "negated");
          eigenaar = ext_field path kvs "host" str;
          periode = periode ();
          bron = bron ();
          navigatie_binding = ext_field path kvs "navigation" rol_binding;
        }
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
          bron = bron ();
        }
  | "TimeWindow" ->
      (* a bound is written whole or not at all: absent is open on that
         side, the v0 null; a window with neither is WF-042's *)
      check_keys path kvs ~ext:[ "from"; "to"; "source" ] [ "node"; "toInclusive" ];
      Periode_conditie
        {
          vanaf = ext_field path kvs "from" expr;
          tot = ext_field path kvs "to" expr;
          tot_inclusief = bool_ (path / "toInclusive") (f "toInclusive");
          bron = bron ();
        }
  | "Compound" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "quantifier"; "threshold"; "parts" ];
      Samengesteld
        {
          kwantor = enum Spelling.quantifier (path / "quantifier") (f "quantifier");
          drempel = int_ (path / "threshold") (f "threshold");
          onderdelen =
            List.mapi
              (fun i x -> conditie (path / "parts" / string_of_int i) x)
              (list_ (path / "parts") (f "parts"));
          bron = bron ();
        }
  | "QuantifierFold" ->
      check_keys path kvs ~ext:[ "threshold"; "filter"; "sourceRole"; "sourcePath"; "source" ]
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
      Kwantorvouw
        {
          (* `the` is read in both stances and refused by WF-108 (a); an
             executor refuses it after the structural load *)
          kv_kwantor = enum Spelling.quantifier (path / "quantifier") (f "quantifier");
          (* optional at the LOADER for the same reason [kv_toets] is: the
             field's PRESENCE is WF-108 (e)'s demand and its value WF-041's,
             so both are citable violations rather than load failures with no
             rule to name *)
          kv_drempel = ext_field path kvs "threshold" int_;
          kv_element_objecttype = str_opt (path / "elementType") (f "elementType");
          (* the subselection is written whole or not at all: absent is none,
             the v0 null *)
          kv_filter = ext_field path kvs "filter" conditie;
          (* nullable at the LOADER, required by WF-108 (d): a missing matrix
             is a citable violation, never a load failure with no rule to name
             — the WF-102 discipline on sourcePath, applied to this node's own
             fields *)
          kv_toets = opt (path / "test") conditie (f "test");
          kv_bron = bron ();
          kv_bron_rol = ext_field path kvs "sourceRole" rol_binding;
          kv_bron_pad =
            ext_field path kvs "sourcePath" (fun p j ->
                List.mapi (fun i x -> rol_binding (p / string_of_int i) x) (list_ p j));
        }
  | "Regelversie" ->
      (* the one node that keeps its v0 shape: the rule-status check waits
         for its parity golden (delta B25); only its source text moved into
         the source record *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "regel"; "inconsistent" ];
      Regelversie
        {
          regel = str_opt (path / "regel") (f "regel");
          inconsistent = bool_ (path / "inconsistent") (f "inconsistent");
          bron = bron ();
        }
  | "Unresolved" ->
      (* as with the expression marker: a marker without a source loads for
         the verifier alone *)
      check_keys path kvs ~ext:[ "source" ] [ "node" ];
      Conditie_onopgelost { bron = bron () }
  | k -> fail (path / "node") ("unknown condition node \"" ^ k ^ "\"")

(* --- model layer -------------------------------------------------------- *)

(* An M1 ObjectType node. Its members are Attribute and Characteristic
   nodes, each carrying its own TimelineFacet; the in-memory record keeps
   the name-keyed facet tables (Ast, [objecttype]), built here member by
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
    { tv_aantal = Some (int_ (path / "count") (field path kvs "count"));
      tv_vast_startpunt = ext_field path kvs "fixedStart" str },
    ext_field path kvs "timelineName" str )

let objecttype path j : string * objecttype =
  let kvs = obj path j in
  expect_node path kvs "ObjectType";
  check_keys path kvs ~ext:[ "supertype"; "plural"; "designations" ]
    [ "node"; "name"; "animate"; "attributes"; "characteristics" ];
  let f name = field path kvs name in
  let name = str (path / "name") (f "name") in
  let attrs =
    List.mapi
      (fun i a ->
        let ap = path / "attributes" / string_of_int i in
        let akvs = obj ap a in
        expect_node ap akvs "Attribute";
        check_keys ap akvs ~ext:[ "plural"; "dimensions"; "timeline"; "designations" ]
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
        check_keys cp ckvs ~ext:[ "timeline"; "designations" ] [ "node"; "name"; "kind" ];
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
      ot_naam = Some name;
      (* whole or not at all: the extension field is absent or a string,
         never null — a null parent would name a type called nothing *)
      ot_supertype = ext_field path kvs "supertype" str;
      attributen = List.map (fun (n, dt, _, _, _) -> (n, dt)) attrs;
      attribuut_meervoud = table (fun (n, _, pl, _, _) -> Option.map (fun p -> (n, Some p)) pl) attrs;
      attribuut_dimensies = table (fun (n, _, _, ds, _) -> Option.map (fun d -> (n, d)) ds) attrs;
      attribuut_granulariteit = tl_attr (fun (g, _, _) -> Some g);
      attribuut_tijdlijn_aantal = tl_attr (fun (_, t, _) -> t);
      attribuut_tijdlijn_naam =
        table
          (fun (n, _, _, _, t) ->
            match t with Some (_, _, Some tn) -> Some (n, Some tn) | _ -> None)
          attrs;
      kenmerken = List.map (fun (n, _, _) -> Some n) chars;
      kenmerk_soorten = List.map (fun (n, k, _) -> (n, Some k)) chars;
      kenmerk_granulariteit = tl_char (fun (g, _, _) -> Some g);
      kenmerk_tijdlijn_aantal = tl_char (fun (_, t, _) -> t);
      bezield = bool_ (path / "animate") (f "animate");
      meervoud = ext_field path kvs "plural" str;
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
let named path (decode : string -> json -> 'a) (naam : 'a -> string option) j : string * 'a =
  let x = decode path j in
  match naam x with
  | Some n -> (n, x)
  | None ->
      fail (path / "name")
        "a declaration in a model list carries its name on its node, and null is no name"

(* The datatype is required and non-null, so [par_datatype] is always
   [Some]; the facet, when present, fills both timeline fields. *)
let parameter path j : string * parameter =
  let kvs = obj path j in
  expect_node path kvs "Parameter";
  check_keys path kvs ~ext:[ "timeline"; "designations" ] [ "node"; "name"; "datatype" ];
  let name = str (path / "name") (field path kvs "name") in
  let facet = ext_field path kvs "timeline" (timeline_facet ~named:false) in
  ( name,
    {
      par_naam = Some name;
      par_datatype = Some (datatype (path / "datatype") (field path kvs "datatype"));
      par_granulariteit = Option.map (fun (g, _, _) -> g) facet;
      par_tijdlijn_aantal = Option.bind facet (fun (_, t, _) -> t.tv_aantal);
      par_vast_startpunt = Option.bind facet (fun (_, t, _) -> t.tv_vast_startpunt);
    } )

let eenheidstelsel path j : string * eenheidstelsel =
  let kvs = obj path j in
  expect_node path kvs "UnitSystem";
  check_keys path kvs [ "node"; "name"; "units" ];
  let name = str (path / "name") (field path kvs "name") in
  ( name,
    {
      es_naam = Some name;
      eenheden =
        List.mapi
          (fun i u -> gedeclareerde_eenheid (path / "units" / string_of_int i) u)
          (list_ (path / "units") (field path kvs "units"));
    } )

let dagsoort path j : dagsoort =
  let kvs = obj path j in
  expect_node path kvs "DayKind";
  check_keys path kvs ~ext:[ "plural" ] [ "node"; "name" ];
  { dk_naam = str (path / "name") (field path kvs "name"); dk_meervoud = ext_field path kvs "plural" str }

(* A role: its plural is written whole or not at all, absent the v0 null. *)
let rol path j : rol =
  let kvs = obj path j in
  expect_node path kvs "Role";
  check_keys path kvs ~ext:[ "plural"; "designations" ] [ "node"; "name"; "objectType"; "many" ];
  {
    rol_naam = str_opt (path / "name") (field path kvs "name");
    rol_objecttype_naam = str_opt (path / "objectType") (field path kvs "objectType");
    rol_meervoud = ext_field path kvs "plural" str;
    rol_meervoudig = bool_ (path / "many") (field path kvs "many");
  }

let feittype path j : feittype =
  let kvs = obj path j in
  expect_node path kvs "FactType";
  check_keys path kvs ~ext:[ "designations" ] [ "node"; "name"; "reciprocal"; "roles" ];
  {
    ft_naam = str_opt (path / "name") (field path kvs "name");
    wederkerig = bool_ (path / "reciprocal") (field path kvs "reciprocal");
    rollen =
      List.mapi
        (fun i x -> rol (path / "roles" / string_of_int i) x)
        (list_ (path / "roles") (field path kvs "roles"));
  }

(* A dimension: its plural and its preposition are written whole or not at
   all, absent the v0 null. *)
let dimensie path j : dimensie =
  let kvs = obj path j in
  expect_node path kvs "Dimension";
  check_keys path kvs ~ext:[ "plural"; "preposition" ] [ "node"; "name"; "labels" ];
  {
    dim_naam = str_opt (path / "name") (field path kvs "name");
    dim_naam_meervoud = ext_field path kvs "plural" str;
    voorzetsel = ext_field path kvs "preposition" str;
    dim_waarden = str_opt_list (path / "labels") (field path kvs "labels");
  }

let tijdlijn path j : tijdlijn =
  let kvs = obj path j in
  expect_node path kvs "Timeline";
  check_keys path kvs ~ext:[ "fixedStart" ] [ "node"; "name"; "variableStart"; "granularity"; "count" ];
  {
    tl_naam = str_opt (path / "name") (field path kvs "name");
    variabel_startpunt = bool_ (path / "variableStart") (field path kvs "variableStart");
    tl_granulariteit = enum_opt Spelling.granularity (path / "granularity") (field path kvs "granularity");
    tl_aantal = int_ (path / "count") (field path kvs "count");
    tl_vast_startpunt = ext_field path kvs "fixedStart" str;
  }

(* --- rule layer --------------------------------------------------------- *)

(* A validity: each bound is written whole or not at all, absent the v0
   null — an open side; a validity with neither bound is always. *)
let geldigheid path j : geldigheid =
  let kvs = obj path j in
  expect_node path kvs "Validity";
  check_keys path kvs ~ext:[ "from"; "to" ] [ "node" ];
  { geld_vanaf = ext_field path kvs "from" str; geld_tot = ext_field path kvs "to" str }

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
     name does; [vocabulaires] reads them *)
  check_keys path kvs ~ext:("designations" :: ext) [ "node"; "name"; "versions" ];
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
let rule_version ~annotations name path j : resolved_regel =
  let kvs = obj path j in
  check_keys path kvs ~ext:[ "condition"; "presentation" ]
    [ "validity"; "target"; "value"; "initialisation" ];
  let f name = field path kvs name in
  let initialisation = bool_ (path / "initialisation") (f "initialisation") in
  let soort =
    match (initialisation, ext_field path kvs "presentation" (enum Spelling.presentation)) with
    | true, Some _ ->
        fail (path / "presentation")
          "presentation beside initialisation true: a presentation tags a computing version, never \
           an initialisation"
    | true, None -> Initialisatie
    | false, Some tag -> tag
    | false, None -> Kenmerktoekenning
  in
  let target =
    match f "target" with
    | `Assoc tkvs as t when List.assoc_opt "node" tkvs = Some (`String "AttributeReference") ->
        expr (path / "target") t
    | _ -> fail (path / "target") "the target of a rule version is an AttributeReference"
  in
  {
    rr_naam = name;
    rr_soort = Some soort;
    doel = Some target;
    waarde = Some (expr (path / "value") (f "value"));
    rr_voorwaarde = ext_field path kvs "condition" conditie;
    rr_geldigheid = Some (geldigheid (path / "validity") (f "validity"));
    rr_bron_annotaties = annotations;
    rr_pointer = path;
  }

(* A Rule writes its statute annotations once, on the form; every version's
   record carries that list ([Ast]). An empty list or a blank entry loads:
   both are WF-082's. *)
let rule path j : resolved_regel list =
  let kvs, versions = named_form ~node:"Rule" ~ext:[ "statuteAnnotations" ] path j in
  let annotations =
    ext_field path kvs "statuteAnnotations" (fun p j ->
        List.mapi (fun i x -> str (p / string_of_int i) x) (list_ p j))
  in
  List.map (fun (name, p, x) -> rule_version ~annotations name p x) versions

let resolved_dagsoort path j : resolved_dagsoort =
  let kvs = obj path j in
  expect_node path kvs "DayKindDefinition";
  check_keys path kvs [ "node"; "dayKind"; "condition"; "validity" ];
  let f name = field path kvs name in
  {
    rd_naam = str_opt (path / "dayKind") (f "dayKind");
    rd_voorwaarde = opt (path / "condition") conditie (f "condition");
    rd_geldigheid = opt (path / "validity") geldigheid (f "validity");
  }

let toekenning path j : toekenning =
  let kvs = obj path j in
  expect_node path kvs "MemberInit";
  check_keys path kvs [ "node"; "member"; "value"; "characteristic" ];
  {
    toe_attribuut = str_opt (path / "member") (field path kvs "member");
    toe_waarde = opt (path / "value") expr (field path kvs "value");
    toe_kenmerk = bool_ (path / "characteristic") (field path kvs "characteristic");
  }

let object_creation path j : resolved_object_creatie list =
  let _, versions = named_form ~node:"ObjectCreation" path j in
  List.map
    (fun (name, path, j) ->
      let kvs = obj path j in
      check_keys path kvs ~ext:[ "condition"; "connectingRole" ]
        [ "createdType"; "subject"; "initialisations"; "validity" ];
      let f name = field path kvs name in
      {
        oc_naam = name;
        nieuw_type = str_opt (path / "createdType") (f "createdType");
        oc_subject = str_opt (path / "subject") (f "subject");
        toekenningen =
          List.mapi
            (fun i x -> toekenning (path / "initialisations" / string_of_int i) x)
            (list_ (path / "initialisations") (f "initialisations"));
        oc_voorwaarde = ext_field path kvs "condition" conditie;
        oc_geldigheid = Some (geldigheid (path / "validity") (f "validity"));
        oc_verbindende_rol = ext_field path kvs "connectingRole" rol_binding;
        oc_pointer = path;
      })
    versions

let allocation path j : resolved_verdeling list =
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
        vd_naam = name;
        bron_type = str_opt (path / "sourceType") (f "sourceType");
        bron_attribuut = str_opt (path / "sourceAttribute") (f "sourceAttribute");
        doel_type = str_opt (path / "targetType") (f "targetType");
        doel_attribuut = str_opt (path / "targetAttribute") (f "targetAttribute");
        vd_methode = Some (enum Spelling.method_ (path / "method") (f "method"));
        rato_attribuut = ext_field path kvs "ratioAttribute" str;
        maximum_attribuut = ext_field path kvs "maximumAttribute" str;
        afronding_decimalen = ext_field path kvs "roundingDecimals" int_;
        restant_attribuut = ext_field path kvs "remainderAttribute" str;
        (* the sort key of an ordered allocation (RUNTIME-SEMANTICS
           ch. 4.2), the v0 field it was *)
        groepeer_attribuut = ext_field path kvs "orderingAttribute" str;
        afnemend = bool_ (path / "descending") (f "descending");
        vd_voorwaarde = ext_field path kvs "condition" conditie;
        vd_geldigheid = Some (geldigheid (path / "validity") (f "validity"));
        verbindende_rol = rol_binding (path / "connectingRole") (f "connectingRole");
        vd_pointer = path;
      })
    versions

let consistency path j : resolved_consistentie list =
  let _, versions = named_form ~node:"Consistency" path j in
  List.map
    (fun (name, path, j) ->
      let kvs = obj path j in
      check_keys path kvs ~ext:[ "source" ] [ "subject"; "assertion"; "validity" ];
      let f name = field path kvs name in
      let bron, line = source_with_line path kvs in
      {
        cons_naam = name;
        cons_subject = str_opt (path / "subject") (f "subject");
        assertie = Some (conditie (path / "assertion") (f "assertion"));
        cons_bron = bron;
        cons_line = line;
        cons_geldigheid = Some (geldigheid (path / "validity") (f "validity"));
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
let uniqueness path j : (resolved_uniciteit, resolved_uniciteit_unie) Either.t list =
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
      let validity = Some (geldigheid (path / "validity") (f "validity")) in
      let bron = source_text path kvs in
      match sources with
      | [ (subject, keys) ] ->
          Either.Left
            {
              uni_naam = name;
              uni_subject = subject;
              sleutels = keys;
              uni_bron = bron;
              uni_geldigheid = validity;
              scope_rol = ext_field path kvs "scope" rol_binding;
              uni_pointer = path;
            }
      | _ ->
          if List.mem_assoc "scope" kvs then
            fail (path / "scope")
              "a uniqueness version with two or more sources carries no scope: a scoped union is \
               an open design question";
          Either.Right
            {
              uu_naam = name;
              bronnen =
                List.map (fun (t, keys) -> { uub_type = t; uub_sleutels = keys }) sources;
              uu_bron = bron;
              uu_geldigheid = validity;
              uu_pointer = path;
            })
    versions

(* The target type of a fact creation, which the wire no longer writes
   (IR-SPEC ch. 3.4): the declared filler of the target role in the created
   fact type, read as WF-028 reads a role — the fact type found with the
   verifier's name identity, and the one of its two roles that the name
   matches. Where that does not resolve the target type stays empty, and
   WF-028 reports the creation. *)
let target_filler feittypen ~fact_type ~role =
  match Checks.find_ci feittypen fact_type with
  | Some ({ rollen = [ a; b ]; _ } : feittype) -> (
      let named (r : rol) =
        match r.rol_naam with Some n -> Checks.norm n = Checks.norm role | None -> false
      in
      match (named a, named b) with
      | true, false -> a.rol_objecttype_naam
      | false, true -> b.rol_objecttype_naam
      | _ -> None)
  | _ -> None

(* A fact creation's path is its whole join chain (IR-SPEC ch. 3.4), anchor
   outward: every hop but the last is a [tussenstap], the last reaches the
   target and is the [join_rol]. *)
let fact_creation feittypen path j : resolved_feit_creatie list =
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
            let hops = List.mapi (fun i x -> rol_binding (ppath / string_of_int i) x) xs in
            let last = List.length hops - 1 in
            (List.filteri (fun i _ -> i < last) hops, List.nth hops last)
      in
      let fact_type = str (path / "factType") (f "factType") in
      let target_role = str (path / "targetRole") (f "targetRole") in
      {
        fc_naam = name;
        anker = str_opt (path / "anchor") (f "anchor");
        fc_doel = target_filler feittypen ~fact_type ~role:target_role;
        tussenstappen = List.map (fun rb -> { ts_rol = rb }) steps;
        fc_voorwaarde = ext_field path kvs "condition" conditie;
        fc_geldigheid = Some (geldigheid (path / "validity") (f "validity"));
        fc_feittype = fact_type;
        anker_rol = str (path / "anchorRole") (f "anchorRole");
        doel_rol = target_role;
        join_rol = join;
        fc_pointer = path;
      })
    versions

let resolved_startpunt path j : resolved_startpunt =
  let kvs = obj path j in
  expect_node path kvs "StartPointDetermination";
  check_keys path kvs [ "node"; "timeline"; "objectType"; "member" ];
  {
    sp_tijdlijn = str_opt (path / "timeline") (field path kvs "timeline");
    sp_objecttype = str_opt (path / "objectType") (field path kvs "objectType");
    lid = str_opt (path / "member") (field path kvs "member");
  }

let resolved_regel_groep path j : resolved_regel_groep =
  let kvs = obj path j in
  expect_node path kvs "RuleGroup";
  check_keys path kvs ~ext:[ "source"; "designations" ] [ "node"; "name"; "recursive"; "members" ];
  let f name = field path kvs name in
  let bron, line = source_with_line path kvs in
  {
    rgr_naam = str_opt (path / "name") (f "name");
    recursief = bool_ (path / "recursive") (f "recursive");
    leden = str_opt_list (path / "members") (f "members");
    rgr_bron = bron;
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

(* Extension form (Phase 4 first cut): one default of the characteristic
   defaults. Every field but the node is written whole or not at all: an
   absent decision or justification is the v0 null, an absent
   initialisation is false. *)
let rec default path j : default =
  let kvs = obj path j in
  expect_node path kvs "Default";
  check_keys path kvs
    ~ext:[ "decision"; "justification"; "consequence"; "initialisation"; "exceptions"; "source" ]
    [ "node" ];
  {
    default_besluit = ext_field path kvs "decision" bool_;
    default_voorwaarde = ext_field path kvs "justification" conditie;
    default_consequentie = ext_field path kvs "consequence" expr;
    default_initialisatie =
      (match ext_field path kvs "initialisation" bool_ with Some b -> b | None -> false);
    default_excepties = ext_field path kvs "exceptions" exception_groups;
    default_bron = source_text path kvs;
  }

(* The nested exception groups riding [exceptions] (v1, Phase 5c;
   RUNTIME-SEMANTICS ch. 7.9). The exceptions object carries no node
   discriminator (it is the extension field's value); each member is an
   [ExceptionGroup] node. Strict: both keys required. *)
and exception_groups path j : exception_groups =
  let kvs = obj path j in
  check_keys path kvs [ "unordered"; "members" ];
  {
    egs_ongeordend = bool_ (path / "unordered") (field path kvs "unordered");
    egs_leden =
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
    eg_bron = source_text path kvs;
  }

(* A defaults group: its target, a characteristic or an attribute, and its
   unordered marking are written whole or not at all — an absent
   characteristic is the v0 null, an absent marking false. *)
let prioritized_defaults path j : prioritized_defaults =
  let kvs = obj path j in
  expect_node path kvs "PrioritizedDefaults";
  check_keys path kvs ~ext:[ "characteristic"; "attribute"; "unordered"; "source"; "designations" ]
    [ "node"; "name"; "subject"; "defaults"; "validity" ];
  let f name = field path kvs name in
  {
    pd_naam = str_opt (path / "name") (f "name");
    pd_onderwerp = str (path / "subject") (f "subject");
    pd_kenmerk = ext_field path kvs "characteristic" str;
    pd_attribuut = ext_field path kvs "attribute" str;
    defaults =
      List.mapi
        (fun i x -> default (path / "defaults" / string_of_int i) x)
        (list_ (path / "defaults") (f "defaults"));
    pd_ongeordend = (match ext_field path kvs "unordered" bool_ with Some b -> b | None -> false);
    pd_bron = source_text path kvs;
    pd_geldigheid = opt (path / "validity") geldigheid (f "validity");
  }

(* Extension form (v1, row 10): a named reusable function (IR-SPEC ch. 2.11).
   Strict: name/parameters/result/body required, source optional. *)
let function_def path j : function_def =
  let kvs = obj path j in
  expect_node path kvs "FunctionDef";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "name"; "parameters"; "result"; "body" ];
  let f name = field path kvs name in
  {
    fd_naam = str (path / "name") (f "name");
    fd_parameters = lambda_params (path / "parameters") (f "parameters");
    fd_result = datatype (path / "result") (f "result");
    fd_body = expr (path / "body") (f "body");
    fd_bron = source_text path kvs;
  }

(* The JSON pointer of every presentation tag written inside a type
   annotation: the subtree under a `type` key, the key every expression node
   writes its annotation under (the only other `type` in the format is a
   string). The datatype reader validates the tag and drops it, because the
   in-memory datatype does not carry it (Ast header); WF-007 refuses it at
   these positions, so they are collected from the document as written. *)
let presentatie_annotaties (j : json) : string list =
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
let vocabulaire path j : vocabulaire =
  let kvs = obj path j in
  expect_node path kvs "Vocabulary";
  check_keys path kvs ~ext:[ "language" ] [ "node"; "name"; "default" ];
  {
    voc_naam = str (path / "name") (field path kvs "name");
    voc_standaard = bool_ (path / "default") (field path kvs "default");
    voc_taal = ext_field path kvs "language" str;
    voc_at = path;
  }

(* A list the side table writes whole or not at all: absent is none, and a
   present list holds at least one entry. *)
let ext_lijst path kvs key what (decode : string -> json -> 'a) : 'a list =
  match List.assoc_opt key kvs with
  | None -> []
  | Some j -> (
      let lp = path / key in
      match list_ lp j with
      | [] -> fail lp (what ^ " are at least one, or the field is absent")
      | xs -> List.mapi (fun i x -> decode (lp / string_of_int i) x) xs)

(* The designations of the declaration at [path], whose members are [kvs]. *)
let benamingen path kvs : benaming list =
  ext_lijst path kvs "designations" "a declaration's designations" (fun bp x ->
      let bkvs = obj bp x in
      check_keys bp bkvs [ "vocabulary"; "designation" ];
      {
        ben_vocabulaire = str (bp / "vocabulary") (field bp bkvs "vocabulary");
        ben_tekst = str (bp / "designation") (field bp bkvs "designation");
        ben_at = bp;
      })

(* The vocabulary side table, read from the document as written: the
   vocabularies of the root list, and every designatable declaration with
   its designations, those without any included (WF-119 asks after them). It
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
let vocabulaires (j : json) : vocabulaire_tafel =
  let kvs = obj "" j in
  let vocs = ext_lijst "" kvs "vocabularies" "a document's vocabularies" vocabulaire in
  let model = obj "/model" (field "" kvs "model") in
  let rules = obj "/rules" (field "" kvs "rules") in
  let items p pkvs key =
    match List.assoc_opt key pkvs with
    | None -> []
    | Some x -> List.mapi (fun i y -> (p / key / string_of_int i, y)) (list_ (p / key) x)
  in
  let element soort eigenaar naam at ekvs =
    match naam with
    | None -> []
    | Some n ->
        [ { bn_soort = soort; bn_eigenaar = eigenaar; bn_naam = n; bn_at = at;
            bn_benamingen = benamingen at ekvs } ]
  in
  let naam_van p ekvs = str_opt (p / "name") (field p ekvs "name") in
  let objecttypen =
    List.concat_map
      (fun (p, x) ->
        let okvs = obj p x in
        let naam = naam_van p okvs in
        let leden soort key =
          List.concat_map
            (fun (lp, l) -> let lkvs = obj lp l in element soort naam (naam_van lp lkvs) lp lkvs)
            (items p okvs key)
        in
        element Bn_objecttype None naam p okvs
        @ leden Bn_attribuut "attributes" @ leden Bn_kenmerk "characteristics")
      (items "/model" model "objectTypes")
  in
  let feittypen =
    List.concat_map
      (fun (p, x) ->
        let fkvs = obj p x in
        let naam = naam_van p fkvs in
        element Bn_feittype None naam p fkvs
        @ List.concat_map
            (fun (rp, r) -> let rkvs = obj rp r in element Bn_rol naam (naam_van rp rkvs) rp rkvs)
            (items p fkvs "roles"))
      (items "/model" model "factTypes")
  in
  let parameters =
    List.concat_map
      (fun (p, x) -> let pkvs = obj p x in element Bn_parameter None (naam_van p pkvs) p pkvs)
      (items "/model" model "parameters")
  in
  let losse = ref [] in
  let waarden =
    List.concat_map
      (fun (p, x) ->
        let dkvs = obj p x in
        let domein = str (p / "name") (field p dkvs "name") in
        let dp = p / "datatype" in
        let dtkvs = obj dp (field p dkvs "datatype") in
        match List.assoc_opt "node" dtkvs with
        | Some (`String "Enumeration") ->
            let waarden = str_opt_list (dp / "values") (field dp dtkvs "values") in
            let entries =
              ext_lijst dp dtkvs "valueDesignations" "an enumeration's value designations"
                (fun ep e ->
                  let ekvs = obj ep e in
                  check_keys ep ekvs [ "value"; "vocabulary"; "designation" ];
                  ( str (ep / "value") (field ep ekvs "value"),
                    {
                      ben_vocabulaire = str (ep / "vocabulary") (field ep ekvs "vocabulary");
                      ben_tekst = str (ep / "designation") (field ep ekvs "designation");
                      ben_at = ep;
                    } ))
            in
            (* each entry to the first value it names *)
            let eerste w =
              let rec zoek i = function
                | [] -> None
                | Some v :: _ when Checks.norm v = Checks.norm w -> Some i
                | _ :: rest -> zoek (i + 1) rest
              in
              zoek 0 waarden
            in
            List.iter
              (fun (w, b) ->
                if eerste w = None then losse := (domein, w, b.ben_at / "value") :: !losse)
              entries;
            List.concat
              (List.mapi
                 (fun i v ->
                   match v with
                   | None -> []
                   | Some v ->
                       [ { bn_soort = Bn_enumeratiewaarde; bn_eigenaar = Some domein; bn_naam = v;
                           bn_at = dp / "values" / string_of_int i;
                           bn_benamingen =
                             List.filter_map
                               (fun (w, b) -> if eerste w = Some i then Some b else None)
                               entries } ])
                 waarden)
        | _ -> [])
      (items "/model" model "domains")
  in
  let vormen =
    List.concat_map
      (fun key ->
        List.concat_map
          (fun (p, x) -> let vkvs = obj p x in element Bn_vorm None (naam_van p vkvs) p vkvs)
          (items "/rules" rules key))
      [ "rules"; "objectCreations"; "allocations"; "consistencies"; "uniquenesses"; "factCreations";
        "prioritizedDefaults" ]
  in
  let groepen =
    List.concat_map
      (fun (p, x) -> let gkvs = obj p x in element Bn_regelgroep None (naam_van p gkvs) p gkvs)
      (items "/rules" rules "ruleGroups")
  in
  {
    vt_vocabulaires = vocs;
    vt_benoembaar = objecttypen @ feittypen @ parameters @ waarden @ vormen @ groepen;
    vt_losse_waarden = List.rev !losse;
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
   grid (WF-115, [Checks.wf115_bevindingen]): an executor lays the periods
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
  let feittypen = m "factTypes" (fun p -> named p feittype (fun (x : feittype) -> x.ft_naam)) in
  let uniqueness_versions = forms "uniquenesses" uniqueness in
  let doc =
    {
      objecttypen = m "objectTypes" objecttype;
      domeinen = m "domains" domain;
      parameters = m "parameters" parameter;
      eenheidstelsels = m "unitSystems" eenheidstelsel;
      feittypen;
      dimensies = m "dimensions" (fun p -> named p dimensie (fun (x : dimensie) -> x.dim_naam));
      tijdlijnen = m "timelines" tijdlijn;
      dagsoorten = m "dayKinds" dagsoort;
      resolved_regels = forms "rules" rule;
      resolved_dagsoorten = r "dayKindDefinitions" resolved_dagsoort;
      object_creaties = forms "objectCreations" object_creation;
      verdelingen = forms "allocations" allocation;
      consistenties = forms "consistencies" consistency;
      uniciteiten = List.filter_map Either.find_left uniqueness_versions;
      uniciteit_unies = List.filter_map Either.find_right uniqueness_versions;
      feit_creaties = forms "factCreations" (fact_creation feittypen);
      resolved_startpunten = r "startPointDeterminations" resolved_startpunt;
      regel_groepen = r "ruleGroups" resolved_regel_groep;
      diagnostics = arr path kvs "diagnostics" diagnostic;
      prioritized_defaults = ext rpath rules "prioritizedDefaults" prioritized_defaults;
      function_defs = ext mpath model "functionDefs" function_def;
      presentatie_annotaties = presentatie_annotaties j;
    }
  in
  (* the side table is read structurally by every reader, and for its
     content by none here (IR-SPEC ch. 2.12) *)
  ignore (vocabulaires j);
  (match reader with
  | Executor -> (
      executor_refusals j;
      match Checks.wf115_bevindingen doc with (at, sentence) :: _ -> fail at sentence | [] -> ())
  | Verifier -> ());
  doc

let load_file ?reader (filename : string) : document =
  document ?reader (Yojson.Safe.from_file filename)
