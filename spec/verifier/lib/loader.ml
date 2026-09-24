(* Strict loader for the uchar-ir-v1 wire format in its L4 shape, with the
   source record of L5.

   Strict means: every object must carry exactly the keys the format
   declares (the schema is additionalProperties:false with every property
   required), every node object must carry the expected "node"
   discriminator, and every enum value must be a known member. Any
   deviation raises [Error] with the JSON path — never a silent default.

   Two deliberate narrowings relative to bare JSON Schema, both fail-fast:
   integer fields accept only canonical integer lexemes (a float-shaped
   "2.0" or an integer beyond native range is a load error, matching the
   byte-identical round-trip discipline of gate 2, which already pins the
   canonical emitter form); and paths are built eagerly, which is
   quadratic only on pathologically deep (100k+ nesting) documents.

   One M1 rule is enforced here and in the schema alike: a declaration in
   one of the two model lists whose node otherwise keeps its v0 shape
   (factTypes, dimensions) carries a string `naam`. There is no map key
   beside it any more, so that field is the declaration's only name. The
   other two lists of that rule (parameters, unitSystems) hold nodes L3
   rewrote, whose `name` is a required string.

   The L2 rules enforced here and in the schema alike: every field that
   left the wire with the reference layer is an unknown field, refused by
   name, and the one unknown datatype node is an unknown node; a binding
   L2 made required is read as required — the creation block, the step
   binding, the allocation's recipient role, a union selection's relation,
   the quantifier fold's range, and the range of a single-argument fold
   with an elementType under any kind but TOTAAL (B12). A reader
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
   '%', and a sign other than ONBEPAALD on a number whose unit is '%'. The
   last two follow from the in-memory form ([Ast]): a number whose unit is
   '%' is read as a [Percentage], which carries no sign, and any other
   number as a [Numeriek], which carries no presentation.

   The L4 rules enforced here and in the schema alike (the rule and the
   expression layer): a named form is {node, name, versions} with at least
   one version, and its name never ends in a space, '#' and digits, the
   in-memory spelling of one version ([Ast]); a version record carries no
   node; a rule version's target is an AttribuutRef, and initialisation
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
   record on an Onopgelost marker; the verifier loads a marker without one for
   WF-113 (b), and every other reader refuses it here ([reader]). *)

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

let enum name table path j =
  let s = str path j in
  match List.assoc_opt s table with
  | Some v -> v
  | None -> fail path ("unknown " ^ name ^ " value \"" ^ s ^ "\"")

let enum_opt name table path = function
  | `Null -> None
  | j -> Some (enum name table path j)

let granulariteit_tbl =
  [ ("DAG", Dag); ("WEEK", Week); ("MAAND", Maand); ("KWARTAAL", Kwartaal); ("JAAR", Jaar) ]

let granulariteit_opt = enum_opt "Granulariteit" granulariteit_tbl
let granulariteit = enum "Granulariteit" granulariteit_tbl

(* The symbolic role binding (extension node, IR-SPEC ch. 2.6): both
   fields are plain strings — a binding is written whole or not at all. *)
let rol_binding path j : rol_binding =
  let kvs = obj path j in
  expect_node path kvs "RolBinding";
  check_keys path kvs [ "node"; "feittype"; "rol" ];
  {
    rb_feittype = str (path / "feittype") (field path kvs "feittype");
    rb_rol = str (path / "rol") (field path kvs "rol");
  }

let teken =
  enum "sign"
    [ ("ONBEPAALD", Onbepaald); ("NEGATIEF", Negatief);
      ("NIET_NEGATIEF", Niet_negatief); ("POSITIEF", Positief) ]

let precision =
  enum "precision" [ ("day", Datum_in_dagen); ("millisecond", Datum_tijd) ]

let presentation = enum "presentation" [ ("percentage", ()) ]

let kenmerk_soort =
  enum "KenmerkSoort"
    [ ("BIJVOEGLIJK", Bijvoeglijk); ("BEZITTELIJK", Bezittelijk); ("ZELFSTANDIG", Zelfstandig) ]

let afrond_modus =
  enum "AfrondModus"
    [ ("NAAR_BENEDEN", Naar_beneden); ("NAAR_BOVEN", Naar_boven);
      ("REKENKUNDIG", Rekenkundig); ("RICHTING_NUL", Richting_nul);
      ("WEG_VAN_NUL", Weg_van_nul) ]

let bin_op =
  enum "BinOp"
    [ ("PLUS", Plus); ("MIN", Min); ("VERMINDERD_MET", Verminderd_met);
      ("MAAL", Maal); ("GEDEELD_DOOR", Gedeeld_door);
      ("GEDEELD_DOOR_ABS", Gedeeld_door_abs); ("TOT_DE_MACHT", Tot_de_macht);
      ("PERCENTAGE_VAN", Percentage_van) ]

(* The kind decides the node (delta B21): the eight folds are an
   Aggregation's and the seventeen other kinds a BuiltinCall's, so a kind of
   the other node is an unknown value of this one. *)
let aggregation_kind =
  enum "kind"
    [ ("SOM", Som); ("TOTAAL", Totaal); ("AANTAL", Aantal);
      ("MINIMUM", Minimum); ("MAXIMUM", Maximum); ("EERSTE", Eerste);
      ("LAATSTE", Laatste); ("AANTAL_GEVULDE", Aantal_gevulde) ]

let builtin_kind =
  enum "kind"
    [ ("WORTEL", Wortel);
      ("ABSOLUTE_WAARDE", Absolute_waarde); ("CONCATENATIE", Concatenatie);
      ("TIJDSDUUR", Tijdsduur); ("ABSOLUTE_TIJDSDUUR", Absolute_tijdsduur);
      ("EERSTE_PAASDAG", Eerste_paasdag);
      ("TIJDSEVENREDIG_DEEL", Tijdsevenredig_deel);
      ("AANTAL_DAGEN_IN", Aantal_dagen_in); ("JAAR_UIT", Jaar_uit);
      ("MAAND_UIT", Maand_uit); ("DAG_UIT", Dag_uit); ("UUR_UIT", Uur_uit);
      ("MINUUT_UIT", Minuut_uit); ("SECONDE_UIT", Seconde_uit);
      ("MILLISECONDE_UIT", Milliseconde_uit); ("DATUM_MET", Datum_met);
      ("DATUMTIJD_MET", Datumtijd_met) ]

let vergelijk_op =
  enum "Operator"
    [ ("KLEINER", Kleiner); ("KLEINER_OF_GELIJK", Kleiner_of_gelijk);
      ("GROTER", Groter); ("GROTER_OF_GELIJK", Groter_of_gelijk);
      ("GELIJK", Gelijk); ("ONGELIJK", Ongelijk); ("EERDER", Eerder);
      ("EERDER_OF_GELIJK", Eerder_of_gelijk); ("LATER", Later);
      ("LATER_OF_GELIJK", Later_of_gelijk) ]

let predicaat_soort =
  enum "Predicaatsoort"
    [ ("LEEG", Leeg); ("GEVULD", Gevuld); ("ELFPROEF", Elfproef);
      ("NUMERIEK_MET_EXACT", Numeriek_met_exact); ("DAGSOORT", Dagsoort) ]

let kwantor =
  enum "Kwantor"
    [ ("DE", De); ("ALLE", Alle); ("GEEN", Geen); ("TEN_MINSTE", Ten_minste);
      ("TEN_HOOGSTE", Ten_hoogste); ("PRECIES", Precies) ]

let builtin_granularity = enum "granularity" granulariteit_tbl

(* The presentation tag of a computing rule version, in its v0 spelling
   (IR-SPEC ch. 3.1). The two other v0 kinds are no tag: an initialisation
   is the version's flag, and a characteristic assignment is a version that
   carries neither (the kind follows from the member its target names). *)
let rule_presentation =
  enum "presentation" [ ("GELIJKSTELLING", Gelijkstelling); ("BEREKENING", Berekening) ]

(* The three real methods (IR-SPEC ch. 3.2). The v0 placeholder of an
   unhandled form, COMPLEX, is no method: an unknown value, refused like any
   other, and the in-memory type has no constructor for it. *)
let methode =
  enum "method"
    [ ("GELIJKE_DELEN", Gelijke_delen); ("NAAR_RATO", Naar_rato); ("OP_VOLGORDE", Op_volgorde) ]

let severity_opt =
  enum_opt "Severity" [ ("ERROR", Sev_error); ("WARNING", Sev_warning); ("INFO", Sev_info) ]

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
      and sign = teken (path / "sign") (f "sign")
      and unit_ = ext_field path kvs "unit" eenheid
      and tag = ext_field path kvs "presentation" presentation in
      (match unit_ with
       | Some ({ afkorting = Some "%"; _ } as u) ->
           if sign <> Onbepaald then
             fail (path / "sign")
               "a percentage carries no sign restriction: a number whose unit is '%' has the \
                sign ONBEPAALD";
           Percentage { geheel; decimalen; eenheid = Some u }
       | _ ->
           if tag <> None then
             fail (path / "presentation")
               "the presentation \"percentage\" on a number whose unit is not '%'";
           Numeriek { geheel; decimalen; teken = Some sign; eenheid = unit_ })
  | "Tekst" -> check_keys path kvs [ "node" ]; Tekst
  | "Booleaan" -> check_keys path kvs [ "node" ]; Booleaan
  | "Date" ->
      check_keys path kvs [ "node"; "precision" ];
      precision (path / "precision") (f "precision")
  | "Enumeratie" ->
      check_keys path kvs [ "node"; "domein"; "waarden" ];
      Enumeratie
        {
          domein = str_opt (path / "domein") (f "domein");
          waarden = str_opt_list (path / "waarden") (f "waarden");
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
  | "Enumeratie" ->
      check_keys path kvs [ "node"; "waarden" ];
      Enumeratie { domein = Some name; waarden = str_opt_list (path / "waarden") (f "waarden") }
  | "SumType" ->
      check_keys path kvs [ "node"; "constructors" ];
      Sum_type
        { domein = name; constructoren = sum_constructors (path / "constructors") (f "constructors") }
  | "StructType" ->
      check_keys path kvs [ "node"; "fields" ];
      Struct_type { domein = name; velden = struct_fields (path / "fields") (f "fields") }
  | _ -> datatype path j

let geheel_periode path j : geheel_periode =
  let kvs = obj path j in
  expect_node path kvs "GeheelPeriode";
  check_keys path kvs [ "node"; "jaar"; "niet" ];
  {
    gp_jaar = bool_ (path / "jaar") (field path kvs "jaar");
    gp_niet = bool_ (path / "niet") (field path kvs "niet");
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
  | "TekstReeks" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "delen"; "type" ];
      Tekst_reeks
        {
          delen =
            List.mapi
              (fun i x -> tekst_deel (path / "delen" / string_of_int i) x)
              (list_ (path / "delen") (f "delen"));
          typ = typ ();
          bron = bron ();
        }
  | "Binair" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "op"; "links"; "rechts"; "type" ];
      Binair
        {
          op = bin_op (path / "op") (f "op");
          links = opt (path / "links") expr (f "links");
          rechts = opt (path / "rechts") expr (f "rechts");
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
      let kind = aggregation_kind (path / "kind") (f "kind") in
      let argumenten = arguments () in
      let element_objecttype = ext_field path kvs "elementType" str in
      (* the range (B12): one spelling at most, and a single-argument fold
         with an elementType carries one under every kind but TOTAAL, where
         its absence is the subject's own read *)
      (match (List.mem_assoc "sourceRole" kvs, List.mem_assoc "sourcePath" kvs) with
      | true, true ->
          fail (path / "sourcePath")
            "sourcePath beside sourceRole: a fold carries one range, never two"
      | false, false
        when element_objecttype <> None && List.length argumenten = 1 && kind <> Totaal ->
          fail path
            "a single-argument fold with an elementType carries its range (sourceRole or \
             sourcePath) under every kind but TOTAAL: the range is written, never derived"
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
      let kind = builtin_kind (path / "kind") (f "kind") in
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
          granulariteit = ext_field path kvs "granularity" builtin_granularity;
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
  | "Afgerond" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "basis"; "modus"; "decimalen"; "type" ];
      Afgerond
        {
          basis = opt (path / "basis") expr (f "basis");
          modus = afrond_modus (path / "modus") (f "modus");
          decimalen = int_ (path / "decimalen") (f "decimalen");
          typ = typ ();
          bron = bron ();
        }
  | "Begrensd" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "basis"; "minimum"; "maximum"; "type" ];
      Begrensd
        {
          basis = opt (path / "basis") expr (f "basis");
          minimum = opt (path / "minimum") expr (f "minimum");
          maximum = opt (path / "maximum") expr (f "maximum");
          typ = typ ();
          bron = bron ();
        }
  | "AttribuutRef" ->
      check_keys path kvs ~ext:[ "navigatieBindingen"; "source" ] [ "node"; "attribuut"; "objecttype" ];
      Attribuut_ref
        {
          attribuut = str_opt (path / "attribuut") (f "attribuut");
          objecttype = str_opt (path / "objecttype") (f "objecttype");
          bron = bron ();
          navigatie_bindingen =
            ext_field path kvs "navigatieBindingen" (fun p j ->
                match List.mapi (fun i x -> rol_binding (p / string_of_int i) x) (list_ p j) with
                | [] -> fail p "an empty navigation-binding list is not a navigation"
                | hops -> hops);
        }
  | "DimensieCel" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "attribuut"; "labels" ];
      Dimensie_cel
        {
          attribuut = opt (path / "attribuut") expr (f "attribuut");
          labels = str_opt_list (path / "labels") (f "labels");
          bron = bron ();
        }
  | "ParameterRef" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "parameter" ];
      Parameter_ref { parameter = str_opt (path / "parameter") (f "parameter"); bron = bron () }
  | "VariabeleRef" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "naam" ];
      Variabele_ref { naam = str_opt (path / "naam") (f "naam"); bron = bron () }
  | "TemporalRead" ->
      (* extension node (v1, row 11): all fields required, none nullable *)
      check_keys path kvs ~ext:[ "source" ] [ "node"; "basis"; "datum"; "type" ];
      Temporal_read
        {
          basis = expr (path / "basis") (f "basis");
          datum = expr (path / "datum") (f "datum");
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
         spells — the closed set is WF-098's, crossed below for an executor
         ([reader]) — and its datatype is required *)
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
  | "Onopgelost" ->
      (* the honest-marker node. Its type is written whole or not at all:
         absent means unknown, and a null is a load error (D14). Its source
         is optional here: a marker without one is WF-113's, which the verifier
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
  | "Letterlijk" ->
      check_keys path kvs [ "node"; "tekst" ];
      Letterlijk { tekst = str_opt (path / "tekst") (f "tekst") }
  | "Ingevoegd" ->
      check_keys path kvs [ "node"; "expr" ];
      Ingevoegd { expr = opt (path / "expr") expr (f "expr") }
  | k -> fail (path / "node") ("unknown TekstReeksDeel node \"" ^ k ^ "\"")

and dimensie_selectie path j : dimensie_selectie =
  let kvs = obj path j in
  expect_node path kvs "DimensieSelectie";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "dimensie"; "labels" ];
  {
    ds_dimensie = str_opt (path / "dimensie") (field path kvs "dimensie");
    ds_labels = str_opt_list (path / "labels") (field path kvs "labels");
    ds_bron = source_text path kvs;
  }

and conditie path j : conditie =
  let kvs = obj path j in
  let f name = field path kvs name in
  let bron () = source_text path kvs in
  let periode () = opt (path / "periode") geheel_periode (f "periode") in
  match node_kind path kvs with
  | "Vergelijking" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "op"; "links"; "rechts"; "periode" ];
      Vergelijking
        {
          op = vergelijk_op (path / "op") (f "op");
          links = opt (path / "links") expr (f "links");
          rechts = opt (path / "rechts") expr (f "rechts");
          periode = periode ();
          bron = bron ();
        }
  | "Predicaat" ->
      check_keys path kvs ~ext:[ "source" ]
        [ "node"; "soort"; "operand"; "dagsoort"; "negatie"; "cijfers"; "periode" ];
      Predicaat
        {
          soort = predicaat_soort (path / "soort") (f "soort");
          operand = opt (path / "operand") expr (f "operand");
          dagsoort = str_opt (path / "dagsoort") (f "dagsoort");
          negatie = bool_ (path / "negatie") (f "negatie");
          cijfers = int_ (path / "cijfers") (f "cijfers");
          periode = periode ();
          bron = bron ();
        }
  | "Kenmerk" ->
      check_keys path kvs ~ext:[ "navigatieBinding"; "source" ]
        [ "node"; "kenmerk"; "negatie"; "eigenaar"; "periode" ];
      Kenmerk_check
        {
          kenmerk = str_opt (path / "kenmerk") (f "kenmerk");
          negatie = bool_ (path / "negatie") (f "negatie");
          eigenaar = str_opt (path / "eigenaar") (f "eigenaar");
          periode = periode ();
          bron = bron ();
          navigatie_binding = ext_field path kvs "navigatieBinding" rol_binding;
        }
  | "RoleMembership" ->
      check_keys path kvs ~ext:[ "counterpartType"; "source" ] [ "node"; "factType"; "role"; "negated" ];
      Role_membership
        {
          fact_type = str (path / "factType") (f "factType");
          role = str (path / "role") (f "role");
          (* written only where it narrows the other end (U12): absent is the
             declared filler, and a null is a load error *)
          counterpart_type = ext_field path kvs "counterpartType" str;
          negated = bool_ (path / "negated") (f "negated");
          bron = bron ();
        }
  | "Periode" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "vanaf"; "tot"; "totInclusief" ];
      Periode_conditie
        {
          vanaf = opt (path / "vanaf") expr (f "vanaf");
          tot = opt (path / "tot") expr (f "tot");
          tot_inclusief = bool_ (path / "totInclusief") (f "totInclusief");
          bron = bron ();
        }
  | "Samengesteld" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "kwantor"; "drempel"; "onderdelen" ];
      Samengesteld
        {
          kwantor = kwantor (path / "kwantor") (f "kwantor");
          drempel = int_ (path / "drempel") (f "drempel");
          onderdelen =
            List.mapi
              (fun i x -> conditie (path / "onderdelen" / string_of_int i) x)
              (list_ (path / "onderdelen") (f "onderdelen"));
          bron = bron ();
        }
  | "Kwantorvouw" ->
      check_keys path kvs ~ext:[ "bronRol"; "bronPad"; "drempel"; "source" ]
        [ "node"; "kwantor"; "elementObjecttype"; "filter"; "toets" ];
      (* the range, exactly one spelling (B12, U2): written, never derived *)
      (match (List.mem_assoc "bronRol" kvs, List.mem_assoc "bronPad" kvs) with
      | true, true ->
          fail (path / "bronPad")
            "bronPad beside bronRol: a quantifier fold carries one range, never two"
      | false, false ->
          fail path
            "a quantifier fold carries its range (bronRol or bronPad): the range is written, \
             never derived"
      | _ -> ());
      Kwantorvouw
        {
          kv_kwantor = kwantor (path / "kwantor") (f "kwantor");
          (* optional at the LOADER for the same reason [kv_toets] is: the
             field's PRESENCE is WF-108 (e)'s demand and its value WF-041's,
             so both are citable violations rather than load failures with no
             rule to name *)
          kv_drempel = ext_field path kvs "drempel" int_;
          kv_element_objecttype =
            str_opt (path / "elementObjecttype") (f "elementObjecttype");
          kv_filter = opt (path / "filter") conditie (f "filter");
          (* optional at the LOADER, required by WF-108 (d): a missing matrix
             is a citable violation, never a load failure with no rule to name
             — the WF-102 discipline on sourcePath, applied to this node's own
             fields *)
          kv_toets = opt (path / "toets") conditie (f "toets");
          kv_bron = bron ();
          kv_bron_rol = ext_field path kvs "bronRol" rol_binding;
          kv_bron_pad =
            ext_field path kvs "bronPad" (fun p j ->
                List.mapi (fun i x -> rol_binding (p / string_of_int i) x) (list_ p j));
        }
  | "Regelversie" ->
      check_keys path kvs ~ext:[ "source" ] [ "node"; "regel"; "inconsistent" ];
      Regelversie
        {
          regel = str_opt (path / "regel") (f "regel");
          inconsistent = bool_ (path / "inconsistent") (f "inconsistent");
          bron = bron ();
        }
  | "Onopgelost" ->
      (* as with the expr marker: the emitter names it Onopgelost, and a
         marker without a source loads for the verifier alone *)
      check_keys path kvs ~ext:[ "source" ] [ "node" ];
      Conditie_onopgelost { bron = bron () }
  | k -> fail (path / "node") ("unknown conditie node \"" ^ k ^ "\"")

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
    ~ext:(if named then [ "timelineName" ] else [])
    [ "node"; "granularity"; "count" ];
  ( granulariteit (path / "granularity") (field path kvs "granularity"),
    int_ (path / "count") (field path kvs "count"),
    ext_field path kvs "timelineName" str )

let objecttype path j : string * objecttype =
  let kvs = obj path j in
  expect_node path kvs "ObjectType";
  check_keys path kvs ~ext:[ "supertype"; "plural" ]
    [ "node"; "name"; "animate"; "attributes"; "characteristics" ];
  let f name = field path kvs name in
  let name = str (path / "name") (f "name") in
  let attrs =
    List.mapi
      (fun i a ->
        let ap = path / "attributes" / string_of_int i in
        let akvs = obj ap a in
        expect_node ap akvs "Attribute";
        check_keys ap akvs ~ext:[ "plural"; "dimensions"; "timeline" ] [ "node"; "name"; "datatype" ];
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
        check_keys cp ckvs ~ext:[ "timeline" ] [ "node"; "name"; "kind" ];
        ( str (cp / "name") (field cp ckvs "name"),
          kenmerk_soort (cp / "kind") (field cp ckvs "kind"),
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
      attribuut_tijdlijn_aantal = tl_attr (fun (_, c, _) -> Some c);
      attribuut_tijdlijn_naam =
        table
          (fun (n, _, _, _, t) ->
            match t with Some (_, _, Some tn) -> Some (n, Some tn) | _ -> None)
          attrs;
      kenmerken = List.map (fun (n, _, _) -> Some n) chars;
      kenmerk_soorten = List.map (fun (n, k, _) -> (n, Some k)) chars;
      kenmerk_granulariteit = tl_char (fun (g, _, _) -> Some g);
      kenmerk_tijdlijn_aantal = tl_char (fun (_, c, _) -> Some c);
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

(* A declaration in a model list whose node otherwise keeps its v0 shape:
   the list has no map key, so the node's `naam` is the declaration's one
   name, a non-null string since M1 (loader header). The node decoders keep
   reading `naam` into the AST's `string option` field, the type every
   reader of this library uses; a null is refused here, so no reader gets
   [None] from these two lists. *)
let named path (decode : string -> json -> 'a) (naam : 'a -> string option) j : string * 'a =
  let x = decode path j in
  match naam x with
  | Some n -> (n, x)
  | None ->
      fail (path / "naam")
        "a declaration in a model list carries its name on its node, and null is no name"

(* The datatype is required and non-null, so [par_datatype] is always
   [Some]; the facet, when present, fills both timeline fields. *)
let parameter path j : string * parameter =
  let kvs = obj path j in
  expect_node path kvs "Parameter";
  check_keys path kvs ~ext:[ "timeline" ] [ "node"; "name"; "datatype" ];
  let name = str (path / "name") (field path kvs "name") in
  let facet = ext_field path kvs "timeline" (timeline_facet ~named:false) in
  ( name,
    {
      par_naam = Some name;
      par_datatype = Some (datatype (path / "datatype") (field path kvs "datatype"));
      par_granulariteit = Option.map (fun (g, _, _) -> g) facet;
      par_tijdlijn_aantal = Option.map (fun (_, c, _) -> c) facet;
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

let rol path j : rol =
  let kvs = obj path j in
  expect_node path kvs "Rol";
  check_keys path kvs [ "node"; "naam"; "objecttypeNaam"; "meervoud"; "meervoudig" ];
  {
    rol_naam = str_opt (path / "naam") (field path kvs "naam");
    rol_objecttype_naam = str_opt (path / "objecttypeNaam") (field path kvs "objecttypeNaam");
    rol_meervoud = str_opt (path / "meervoud") (field path kvs "meervoud");
    rol_meervoudig = bool_ (path / "meervoudig") (field path kvs "meervoudig");
  }

let feittype path j : feittype =
  let kvs = obj path j in
  expect_node path kvs "Feittype";
  check_keys path kvs [ "node"; "naam"; "wederkerig"; "rollen" ];
  {
    ft_naam = str_opt (path / "naam") (field path kvs "naam");
    wederkerig = bool_ (path / "wederkerig") (field path kvs "wederkerig");
    rollen =
      List.mapi
        (fun i x -> rol (path / "rollen" / string_of_int i) x)
        (list_ (path / "rollen") (field path kvs "rollen"));
  }

let dimensie path j : dimensie =
  let kvs = obj path j in
  expect_node path kvs "Dimensie";
  check_keys path kvs [ "node"; "naam"; "naamMeervoud"; "voorzetsel"; "waarden" ];
  {
    dim_naam = str_opt (path / "naam") (field path kvs "naam");
    dim_naam_meervoud = str_opt (path / "naamMeervoud") (field path kvs "naamMeervoud");
    voorzetsel = str_opt (path / "voorzetsel") (field path kvs "voorzetsel");
    dim_waarden = str_opt_list (path / "waarden") (field path kvs "waarden");
  }

let tijdlijn path j : tijdlijn =
  let kvs = obj path j in
  expect_node path kvs "Tijdlijn";
  check_keys path kvs [ "node"; "naam"; "variabelStartpunt"; "granulariteit"; "aantal" ];
  {
    tl_naam = str_opt (path / "naam") (field path kvs "naam");
    variabel_startpunt = bool_ (path / "variabelStartpunt") (field path kvs "variabelStartpunt");
    tl_granulariteit =
      granulariteit_opt (path / "granulariteit") (field path kvs "granulariteit");
    tl_aantal = int_ (path / "aantal") (field path kvs "aantal");
  }

(* --- rule layer --------------------------------------------------------- *)

let geldigheid path j : geldigheid =
  let kvs = obj path j in
  expect_node path kvs "Geldigheid";
  check_keys path kvs [ "node"; "vanaf"; "tot" ];
  {
    geld_vanaf = str_opt (path / "vanaf") (field path kvs "vanaf");
    geld_tot = str_opt (path / "tot") (field path kvs "tot");
  }

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
  check_keys path kvs ~ext [ "node"; "name"; "versions" ];
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
    match (initialisation, ext_field path kvs "presentation" rule_presentation) with
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
    | `Assoc tkvs as t when List.assoc_opt "node" tkvs = Some (`String "AttribuutRef") ->
        expr (path / "target") t
    | _ -> fail (path / "target") "the target of a rule version is an AttribuutRef"
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
  expect_node path kvs "ResolvedDagsoort";
  check_keys path kvs [ "node"; "naam"; "voorwaarde"; "geldigheid" ];
  let f name = field path kvs name in
  {
    rd_naam = str_opt (path / "naam") (f "naam");
    rd_voorwaarde = opt (path / "voorwaarde") conditie (f "voorwaarde");
    rd_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
  }

let toekenning path j : toekenning =
  let kvs = obj path j in
  expect_node path kvs "Toekenning";
  check_keys path kvs [ "node"; "attribuut"; "waarde"; "kenmerk" ];
  {
    toe_attribuut = str_opt (path / "attribuut") (field path kvs "attribuut");
    toe_waarde = opt (path / "waarde") expr (field path kvs "waarde");
    toe_kenmerk = bool_ (path / "kenmerk") (field path kvs "kenmerk");
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
        vd_methode = Some (methode (path / "method") (f "method"));
        rato_attribuut = ext_field path kvs "ratioAttribute" str;
        maximum_attribuut = ext_field path kvs "maximumAttribute" str;
        afronding_decimalen = ext_field path kvs "roundingDecimals" int_;
        restant_attribuut = ext_field path kvs "remainderAttribute" str;
        (* the sort key of an OP_VOLGORDE allocation (RUNTIME-SEMANTICS
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
  expect_node path kvs "ResolvedStartpunt";
  check_keys path kvs [ "node"; "tijdlijn"; "objecttype"; "lid" ];
  {
    sp_tijdlijn = str_opt (path / "tijdlijn") (field path kvs "tijdlijn");
    sp_objecttype = str_opt (path / "objecttype") (field path kvs "objecttype");
    lid = str_opt (path / "lid") (field path kvs "lid");
  }

let resolved_regel_groep path j : resolved_regel_groep =
  let kvs = obj path j in
  expect_node path kvs "ResolvedRegelGroep";
  check_keys path kvs ~ext:[ "source" ] [ "node"; "naam"; "recursief"; "leden" ];
  let f name = field path kvs name in
  let bron, line = source_with_line path kvs in
  {
    rgr_naam = str_opt (path / "naam") (f "naam");
    recursief = bool_ (path / "recursief") (f "recursief");
    leden = str_opt_list (path / "leden") (f "leden");
    rgr_bron = bron;
    rgr_line = line;
  }

let diagnostic path j : diagnostic =
  let kvs = obj path j in
  expect_node path kvs "Diagnostic";
  check_keys path kvs [ "node"; "severity"; "message"; "line" ];
  {
    diag_severity = severity_opt (path / "severity") (field path kvs "severity");
    message = str_opt (path / "message") (field path kvs "message");
    diag_line = int_ (path / "line") (field path kvs "line");
  }

(* --- the document ------------------------------------------------------- *)

(* Extension form (Phase 4 first cut): one default of the characteristic
   defaults. Strict: no nullable children beyond naam/bron/geldigheid. *)
let rec default path j : default =
  let kvs = obj path j in
  expect_node path kvs "Default";
  check_keys path kvs ~ext:[ "consequentie"; "initialisatie"; "excepties"; "source" ]
    [ "node"; "besluit"; "voorwaarde" ];
  let bool_opt path = function
    | `Bool b -> Some b
    | `Null -> None
    | j -> fail path ("expected boolean or null, got " ^ Yojson.Safe.to_string j)
  in
  {
    default_besluit = bool_opt (path / "besluit") (field path kvs "besluit");
    default_voorwaarde = opt (path / "voorwaarde") conditie (field path kvs "voorwaarde");
    default_consequentie = ext_field path kvs "consequentie" expr;
    default_initialisatie =
      (match ext_field path kvs "initialisatie" (fun p j -> bool_ p j) with
      | Some b -> b
      | None -> false);
    default_excepties = ext_field path kvs "excepties" exception_groups;
    default_bron = source_text path kvs;
  }

(* The nested exception groups riding [excepties] (v1, Phase 5c;
   RUNTIME-SEMANTICS ch. 7.9). The excepties object carries no node
   discriminator (it is the extension field's value); each member is an
   [ExceptionGroup] node. Strict: both keys required. *)
and exception_groups path j : exception_groups =
  let kvs = obj path j in
  check_keys path kvs [ "ongeordend"; "leden" ];
  {
    egs_ongeordend = bool_ (path / "ongeordend") (field path kvs "ongeordend");
    egs_leden =
      List.mapi
        (fun i x -> exception_group (path / "leden" / string_of_int i) x)
        (list_ (path / "leden") (field path kvs "leden"));
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

let prioritized_defaults path j : prioritized_defaults =
  let kvs = obj path j in
  expect_node path kvs "PrioritizedDefaults";
  check_keys path kvs ~ext:[ "attribuut"; "ongeordend"; "source" ]
    [ "node"; "naam"; "onderwerp"; "kenmerk"; "defaults"; "geldigheid" ];
  let f name = field path kvs name in
  {
    pd_naam = str_opt (path / "naam") (f "naam");
    pd_onderwerp = str (path / "onderwerp") (f "onderwerp");
    pd_kenmerk = str_opt (path / "kenmerk") (f "kenmerk");
    pd_attribuut = ext_field path kvs "attribuut" str;
    defaults =
      List.mapi
        (fun i x -> default (path / "defaults" / string_of_int i) x)
        (list_ (path / "defaults") (f "defaults"));
    pd_ongeordend =
      (match ext_field path kvs "ongeordend" (fun p j -> bool_ p j) with
      | Some b -> b
      | None -> false);
    pd_bron = source_text path kvs;
    pd_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
  }

(* Extension form (v1, row 10): a named reusable function (IR-SPEC ch. 2.11).
   Strict: name/parameters/result/body required, bron nullable. *)
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
   So is an Onopgelost marker without a source (WF-113 (b)): the verifier
   loads it so that the rule names it, and an executor refuses it in the rule's
   sentence ([Checks.marker_without_source]), since no report can account for
   a marker that has no text to quote. *)
type reader = Verifier | Executor

(* The executor's refusals, after the structural load: every Literal,
   ContextValue and Onopgelost node of the document as written, in document
   order, a literal's variant crossed against its type before its string is
   read. A literal without a type is WF-034's typeless value node, with no type
   to cross its variant against; its string is read all the same. *)
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
        | Some (`String "Onopgelost") ->
            if not (List.mem_assoc "source" kvs) then fail path Checks.marker_without_source
        | _ -> ());
        List.iter (fun (k, x) -> walk (path / k) x) kvs
    | `List xs -> List.iteri (fun i x -> walk (path / string_of_int i) x) xs
    | _ -> ()
  in
  walk "" j

(* The M1 root: {format, model, rules, diagnostics}. [model] is the
   declaration set as lists, [rules] the rule lists under their v0 keys;
   each carries one optional extension list. Since L4 the six named forms
   are read version by version into the v0-shaped records, and the one wire
   list of uniquenesses into the two in-memory lists. *)
let document ?(reader = Executor) (j : json) : document =
  let path = "" in
  let kvs = obj path j in
  check_keys path kvs [ "format"; "model"; "rules"; "diagnostics" ];
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
    [ "resolvedRegels"; "resolvedDagsoorten"; "objectCreaties"; "verdelingen";
      "consistenties"; "uniciteiten"; "feitCreaties"; "resolvedStartpunten"; "regelGroepen" ];
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
  let uniqueness_versions = forms "uniciteiten" uniqueness in
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
      resolved_regels = forms "resolvedRegels" rule;
      resolved_dagsoorten = r "resolvedDagsoorten" resolved_dagsoort;
      object_creaties = forms "objectCreaties" object_creation;
      verdelingen = forms "verdelingen" allocation;
      consistenties = forms "consistenties" consistency;
      uniciteiten = List.filter_map Either.find_left uniqueness_versions;
      uniciteit_unies = List.filter_map Either.find_right uniqueness_versions;
      feit_creaties = forms "feitCreaties" (fact_creation feittypen);
      resolved_startpunten = r "resolvedStartpunten" resolved_startpunt;
      regel_groepen = r "regelGroepen" resolved_regel_groep;
      diagnostics = arr path kvs "diagnostics" diagnostic;
      prioritized_defaults = ext rpath rules "prioritizedDefaults" prioritized_defaults;
      function_defs = ext mpath model "functionDefs" function_def;
      presentatie_annotaties = presentatie_annotaties j;
    }
  in
  (match reader with Executor -> executor_refusals j | Verifier -> ());
  doc

let load_file ?reader (filename : string) : document =
  document ?reader (Yojson.Safe.from_file filename)
