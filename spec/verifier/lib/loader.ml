(* Strict loader for the uchar-ir-v1 wire format in its L3 shape.

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
   with an elementObjecttype under any kind but TOTAAL (B12, U2). A reader
   of this library therefore never meets a bindingless form that it would
   have to derive a binding for.

   The L3 rules enforced here and in the schema alike (the type system):
   the datatype nodes are `Number` and `Date`, a unit is a `Unit` and a
   unit system a `UnitSystem` with a list of units, a day kind a `DayKind`
   and a parameter's time-dependence a timeline facet; every node and
   field that left the wire with them is unknown and refused by name. Four
   values the schema types are refused here in one sentence each, which
   the other readers of the wire share: a conversion factor that is not an
   exact rational {num, den} of two integers of at least 1, a presentation
   other than "percentage", the presentation on a number whose unit is not
   '%', and a sign other than ONBEPAALD on a number whose unit is '%'. The
   last two follow from the in-memory form ([Ast]): a number whose unit is
   '%' is read as a [Percentage], which carries no sign, and any other
   number as a [Numeriek], which carries no presentation. *)

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

let functie_kind =
  enum "FunctieKind"
    [ ("SOM", Som); ("TOTAAL", Totaal); ("AANTAL", Aantal);
      ("MINIMUM", Minimum); ("MAXIMUM", Maximum); ("EERSTE", Eerste);
      ("LAATSTE", Laatste); ("AANTAL_GEVULDE", Aantal_gevulde);
      ("WORTEL", Wortel);
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

let regel_soort_opt =
  enum_opt "Soort"
    [ ("GELIJKSTELLING", Gelijkstelling); ("BEREKENING", Berekening);
      ("INITIALISATIE", Initialisatie); ("KENMERKTOEKENNING", Kenmerktoekenning) ]

let methode_opt =
  enum_opt "Methode"
    [ ("GELIJKE_DELEN", Gelijke_delen); ("NAAR_RATO", Naar_rato);
      ("OP_VOLGORDE", Op_volgorde); ("COMPLEX", Complex) ]

let severity_opt =
  enum_opt "Severity" [ ("ERROR", Sev_error); ("WARNING", Sev_warning); ("INFO", Sev_info) ]

(* --- unit and datatype ------------------------------------------------- *)

(* A unit's conversion factor: the exact rational {num, den}, both integers
   of at least 1 (IR-SPEC ch. 2.5). Whether it is in lowest terms is WF-092
   (c), a verifier check, so {2, 4} loads. Every other form — a JSON number,
   a string, a missing or unknown key, a value below 1 or beyond the native
   integer range — is refused in one sentence. *)
let rationaal path j : rationaal =
  let refuse () =
    fail path
      ("a conversion factor is an exact rational {num, den} whose two integers are at least \
        1, got " ^ Yojson.Safe.to_string j)
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

(* --- expr and conditie -------------------------------------------------- *)

let rec expr path j : expr =
  let kvs = obj path j in
  let f name = field path kvs name in
  let typ () = opt (path / "type") datatype (f "type") in
  let bron () = str_opt (path / "bron") (f "bron") in
  match node_kind path kvs with
  | "Literal" ->
      check_keys path kvs [ "node"; "waarde"; "type"; "bron" ];
      Literal { waarde = str_opt (path / "waarde") (f "waarde"); typ = typ (); bron = bron () }
  | "TekstReeks" ->
      check_keys path kvs [ "node"; "delen"; "type"; "bron" ];
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
      check_keys path kvs [ "node"; "op"; "links"; "rechts"; "type"; "bron" ];
      Binair
        {
          op = bin_op (path / "op") (f "op");
          links = opt (path / "links") expr (f "links");
          rechts = opt (path / "rechts") expr (f "rechts");
          typ = typ ();
          bron = bron ();
        }
  | "Functie" ->
      check_keys path kvs ~ext:[ "bronRol"; "bronPad" ]
        [ "node"; "kind"; "argumenten"; "elementObjecttype"; "leegwaarde";
          "filter"; "dimensieSelectie"; "type"; "bron"; "granulariteit" ];
      let kind = functie_kind (path / "kind") (f "kind") in
      let argumenten =
        List.mapi
          (fun i x -> expr (path / "argumenten" / string_of_int i) x)
          (list_ (path / "argumenten") (f "argumenten"))
      in
      let element_objecttype = str_opt (path / "elementObjecttype") (f "elementObjecttype") in
      (* the range (B12, U2): one spelling at most, and a single-argument
         fold with an elementObjecttype carries one under every kind but
         TOTAAL, where its absence is the subject's own read *)
      (match (List.mem_assoc "bronRol" kvs, List.mem_assoc "bronPad" kvs) with
      | true, true ->
          fail (path / "bronPad") "bronPad beside bronRol: a fold carries one range, never two"
      | false, false
        when element_objecttype <> None && List.length argumenten = 1 && kind <> Totaal ->
          fail path
            "a single-argument fold with an elementObjecttype carries its range (bronRol or \
             bronPad) under every kind but TOTAAL: the range is written, never derived"
      | _ -> ());
      Functie
        {
          kind;
          argumenten;
          element_objecttype;
          leegwaarde = opt (path / "leegwaarde") expr (f "leegwaarde");
          filter = opt (path / "filter") conditie (f "filter");
          dimensie_selectie =
            opt (path / "dimensieSelectie") dimensie_selectie (f "dimensieSelectie");
          typ = typ ();
          bron = bron ();
          granulariteit = granulariteit_opt (path / "granulariteit") (f "granulariteit");
          bron_rol = ext_field path kvs "bronRol" rol_binding;
          (* loaded verbatim, empty list included: WF-102 (b) owns the
             arity, so a defective path is a citable violation and not a
             load failure with no rule to name *)
          bron_pad =
            ext_field path kvs "bronPad" (fun p j ->
                List.mapi (fun i x -> rol_binding (p / string_of_int i) x) (list_ p j));
        }
  | "InstanceSetUnion" ->
      check_keys path kvs [ "node"; "branches"; "bron" ];
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
      check_keys path kvs [ "node"; "basis"; "modus"; "decimalen"; "type"; "bron" ];
      Afgerond
        {
          basis = opt (path / "basis") expr (f "basis");
          modus = afrond_modus (path / "modus") (f "modus");
          decimalen = int_ (path / "decimalen") (f "decimalen");
          typ = typ ();
          bron = bron ();
        }
  | "Begrensd" ->
      check_keys path kvs [ "node"; "basis"; "minimum"; "maximum"; "type"; "bron" ];
      Begrensd
        {
          basis = opt (path / "basis") expr (f "basis");
          minimum = opt (path / "minimum") expr (f "minimum");
          maximum = opt (path / "maximum") expr (f "maximum");
          typ = typ ();
          bron = bron ();
        }
  | "AttribuutRef" ->
      check_keys path kvs ~ext:[ "navigatieBindingen" ] [ "node"; "attribuut"; "objecttype"; "bron" ];
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
      check_keys path kvs [ "node"; "attribuut"; "labels"; "bron" ];
      Dimensie_cel
        {
          attribuut = opt (path / "attribuut") expr (f "attribuut");
          labels = str_opt_list (path / "labels") (f "labels");
          bron = bron ();
        }
  | "ParameterRef" ->
      check_keys path kvs [ "node"; "parameter"; "bron" ];
      Parameter_ref { parameter = str_opt (path / "parameter") (f "parameter"); bron = bron () }
  | "VariabeleRef" ->
      check_keys path kvs [ "node"; "naam"; "bron" ];
      Variabele_ref { naam = str_opt (path / "naam") (f "naam"); bron = bron () }
  | "TemporalRead" ->
      (* extension node (v1, row 11): all fields required, none nullable *)
      check_keys path kvs [ "node"; "basis"; "datum"; "type"; "bron" ];
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
      check_keys path kvs [ "node"; "type"; "constructor"; "argument"; "bron" ];
      Construct
        {
          typ = typ ();
          constructor_ = str (path / "constructor") (f "constructor");
          argument = opt (path / "argument") expr (f "argument");
          bron = bron ();
        }
  | "Match" ->
      check_keys path kvs [ "node"; "type"; "scrutinee"; "arms"; "bron" ];
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
      check_keys path kvs [ "node"; "name"; "bron" ];
      Binder_ref { naam = str (path / "name") (f "name"); bron = bron () }
  | "StructLiteral" ->
      (* extension node (v1, Phase 5c): the fields map name to a value expr,
         in declaration order (WF-073); type required not-null *)
      check_keys path kvs [ "node"; "type"; "fields"; "bron" ];
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
      check_keys path kvs [ "node"; "type"; "base"; "field"; "bron" ];
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
      check_keys path kvs [ "node"; "type"; "elements"; "bron" ];
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
      check_keys path kvs [ "node"; "parameters"; "body"; "bron" ];
      Lambda
        {
          parameters = lambda_params (path / "parameters") (f "parameters");
          body = expr (path / "body") (f "body");
          bron = bron ();
        }
  | "IfThenElse" ->
      check_keys path kvs [ "node"; "type"; "condition"; "then"; "else"; "bron" ];
      If_then_else
        {
          typ = typ ();
          condition = conditie (path / "condition") (f "condition");
          then_ = expr (path / "then") (f "then");
          else_ = expr (path / "else") (f "else");
          bron = bron ();
        }
  | "Invoke" ->
      check_keys path kvs [ "node"; "type"; "function"; "arguments"; "bron" ];
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
      check_keys path kvs [ "node"; "type"; "list"; "function"; "init"; "bron" ];
      Fold
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          functie = fold_fn (path / "function") (f "function");
          init = expr (path / "init") (f "init");
          bron = bron ();
        }
  | "MapList" ->
      check_keys path kvs [ "node"; "type"; "list"; "function"; "bron" ];
      Map_list
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          functie = fold_fn (path / "function") (f "function");
          bron = bron ();
        }
  | "FilterList" ->
      check_keys path kvs [ "node"; "type"; "list"; "binder"; "condition"; "bron" ];
      Filter_list
        {
          typ = typ ();
          list = expr (path / "list") (f "list");
          binder = str (path / "binder") (f "binder");
          condition = conditie (path / "condition") (f "condition");
          bron = bron ();
        }
  | "ListLength" ->
      check_keys path kvs [ "node"; "type"; "list"; "bron" ];
      List_length { typ = typ (); list = expr (path / "list") (f "list"); bron = bron () }
  | "Let" ->
      (* extension node (v1, Phase 5c). Strict, in this loader's sense of the
         word (never the Let's semantics — ch. 10.6 is call-by-need): type
         required not-null, binder a plain string, value/body non-null exprs *)
      check_keys path kvs [ "node"; "type"; "binder"; "value"; "body"; "bron" ];
      Let
        {
          typ = typ ();
          binder = str (path / "binder") (f "binder");
          value = expr (path / "value") (f "value");
          body = expr (path / "body") (f "body");
          bron = bron ();
        }
  | "Constante" ->
      check_keys path kvs [ "node"; "naam"; "type"; "bron" ];
      Constante { naam = str_opt (path / "naam") (f "naam"); typ = typ (); bron = bron () }
  | "Onopgelost" ->
      (* the honest-marker node. Its type is written whole or not at all:
         absent means unknown, and a null is a load error (D14) *)
      check_keys path kvs ~ext:[ "type" ] [ "node"; "bron" ];
      Expr_onopgelost { typ = ext_field path kvs "type" datatype; bron = bron () }
  | k -> fail (path / "node") ("unknown expr node \"" ^ k ^ "\"")

(* One branch of an InstanceSetUnion (WF-110). *)
and instance_set_branch path j : instance_set_branch =
  let kvs = obj path j in
  let f name = field path kvs name in
  expect_node path kvs "InstanceSetBranch";
  check_keys path kvs [ "node"; "elementType"; "sourceRole"; "filter"; "bron" ];
  {
    isb_element_type = str_opt (path / "elementType") (f "elementType");
    isb_source_role = rol_binding (path / "sourceRole") (f "sourceRole");
    isb_filter = opt (path / "filter") conditie (f "filter");
    isb_bron = str_opt (path / "bron") (f "bron");
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
      check_keys path kvs [ "node"; "parameters"; "body"; "bron" ];
      Fn_lambda
        {
          parameters = lambda_params (path / "parameters") (field path kvs "parameters");
          body = expr (path / "body") (field path kvs "body");
          bron = str_opt (path / "bron") (field path kvs "bron");
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
  check_keys path kvs [ "node"; "dimensie"; "labels"; "bron" ];
  {
    ds_dimensie = str_opt (path / "dimensie") (field path kvs "dimensie");
    ds_labels = str_opt_list (path / "labels") (field path kvs "labels");
    ds_bron = str_opt (path / "bron") (field path kvs "bron");
  }

and conditie path j : conditie =
  let kvs = obj path j in
  let f name = field path kvs name in
  let bron () = str_opt (path / "bron") (f "bron") in
  let periode () = opt (path / "periode") geheel_periode (f "periode") in
  match node_kind path kvs with
  | "Vergelijking" ->
      check_keys path kvs [ "node"; "op"; "links"; "rechts"; "periode"; "bron" ];
      Vergelijking
        {
          op = vergelijk_op (path / "op") (f "op");
          links = opt (path / "links") expr (f "links");
          rechts = opt (path / "rechts") expr (f "rechts");
          periode = periode ();
          bron = bron ();
        }
  | "Predicaat" ->
      check_keys path kvs
        [ "node"; "soort"; "operand"; "dagsoort"; "negatie"; "cijfers"; "periode"; "bron" ];
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
      check_keys path kvs ~ext:[ "navigatieBinding" ]
        [ "node"; "kenmerk"; "negatie"; "eigenaar"; "periode"; "bron" ];
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
      check_keys path kvs ~ext:[ "counterpartType" ] [ "node"; "factType"; "role"; "negated"; "bron" ];
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
      check_keys path kvs [ "node"; "vanaf"; "tot"; "totInclusief"; "bron" ];
      Periode_conditie
        {
          vanaf = opt (path / "vanaf") expr (f "vanaf");
          tot = opt (path / "tot") expr (f "tot");
          tot_inclusief = bool_ (path / "totInclusief") (f "totInclusief");
          bron = bron ();
        }
  | "Samengesteld" ->
      check_keys path kvs [ "node"; "kwantor"; "drempel"; "onderdelen"; "bron" ];
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
      check_keys path kvs ~ext:[ "bronRol"; "bronPad"; "drempel" ]
        [ "node"; "kwantor"; "elementObjecttype"; "filter"; "toets"; "bron" ];
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
             — the WF-102 discipline on bronPad, applied to this node's own
             fields *)
          kv_toets = opt (path / "toets") conditie (f "toets");
          kv_bron = bron ();
          kv_bron_rol = ext_field path kvs "bronRol" rol_binding;
          kv_bron_pad =
            ext_field path kvs "bronPad" (fun p j ->
                List.mapi (fun i x -> rol_binding (p / string_of_int i) x) (list_ p j));
        }
  | "Regelversie" ->
      check_keys path kvs [ "node"; "regel"; "inconsistent"; "bron" ];
      Regelversie
        {
          regel = str_opt (path / "regel") (f "regel");
          inconsistent = bool_ (path / "inconsistent") (f "inconsistent");
          bron = bron ();
        }
  | "Onopgelost" ->
      (* as with the expr marker: the emitter names it Onopgelost *)
      check_keys path kvs [ "node"; "bron" ];
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

let resolved_regel path j : resolved_regel =
  let kvs = obj path j in
  expect_node path kvs "ResolvedRegel";
  check_keys path kvs ~ext:[ "bronAnnotaties" ]
    [ "node"; "naam"; "soort"; "doel"; "waarde"; "voorwaarde"; "geldigheid" ];
  let f name = field path kvs name in
  {
    rr_naam = str_opt (path / "naam") (f "naam");
    rr_soort = regel_soort_opt (path / "soort") (f "soort");
    doel = opt (path / "doel") expr (f "doel");
    waarde = opt (path / "waarde") expr (f "waarde");
    rr_voorwaarde = opt (path / "voorwaarde") conditie (f "voorwaarde");
    rr_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
    rr_bron_annotaties =
      ext_field path kvs "bronAnnotaties" (fun p j ->
          List.mapi (fun i x -> str (p / string_of_int i) x) (list_ p j));
  }

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

let resolved_object_creatie path j : resolved_object_creatie =
  let kvs = obj path j in
  expect_node path kvs "ResolvedObjectCreatie";
  check_keys path kvs ~ext:[ "verbindendeRol" ]
    [ "node"; "naam"; "nieuwType"; "subject"; "toekenningen"; "voorwaarde"; "geldigheid" ];
  let f name = field path kvs name in
  {
    oc_naam = str_opt (path / "naam") (f "naam");
    nieuw_type = str_opt (path / "nieuwType") (f "nieuwType");
    oc_subject = str_opt (path / "subject") (f "subject");
    toekenningen =
      List.mapi
        (fun i x -> toekenning (path / "toekenningen" / string_of_int i) x)
        (list_ (path / "toekenningen") (f "toekenningen"));
    oc_voorwaarde = opt (path / "voorwaarde") conditie (f "voorwaarde");
    oc_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
    oc_verbindende_rol = ext_field path kvs "verbindendeRol" rol_binding;
  }

let resolved_verdeling path j : resolved_verdeling =
  let kvs = obj path j in
  expect_node path kvs "ResolvedVerdeling";
  check_keys path kvs
    [ "node"; "naam"; "bronType"; "bronAttribuut"; "doelType"; "doelAttribuut";
      "methode"; "ratoAttribuut"; "maximumAttribuut"; "afrondingDecimalen";
      "restantAttribuut"; "groepeerAttribuut"; "afnemend"; "voorwaarde"; "geldigheid";
      "verbindendeRol" ];
  let f name = field path kvs name in
  {
    vd_naam = str_opt (path / "naam") (f "naam");
    bron_type = str_opt (path / "bronType") (f "bronType");
    bron_attribuut = str_opt (path / "bronAttribuut") (f "bronAttribuut");
    doel_type = str_opt (path / "doelType") (f "doelType");
    doel_attribuut = str_opt (path / "doelAttribuut") (f "doelAttribuut");
    vd_methode = methode_opt (path / "methode") (f "methode");
    rato_attribuut = str_opt (path / "ratoAttribuut") (f "ratoAttribuut");
    maximum_attribuut = str_opt (path / "maximumAttribuut") (f "maximumAttribuut");
    afronding_decimalen = int_opt (path / "afrondingDecimalen") (f "afrondingDecimalen");
    restant_attribuut = str_opt (path / "restantAttribuut") (f "restantAttribuut");
    groepeer_attribuut = str_opt (path / "groepeerAttribuut") (f "groepeerAttribuut");
    afnemend = bool_ (path / "afnemend") (f "afnemend");
    vd_voorwaarde = opt (path / "voorwaarde") conditie (f "voorwaarde");
    vd_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
    verbindende_rol = rol_binding (path / "verbindendeRol") (f "verbindendeRol");
  }

let resolved_consistentie path j : resolved_consistentie =
  let kvs = obj path j in
  expect_node path kvs "ResolvedConsistentie";
  check_keys path kvs [ "node"; "naam"; "subject"; "assertie"; "bron"; "line"; "geldigheid" ];
  let f name = field path kvs name in
  {
    cons_naam = str_opt (path / "naam") (f "naam");
    cons_subject = str_opt (path / "subject") (f "subject");
    assertie = opt (path / "assertie") conditie (f "assertie");
    cons_bron = str_opt (path / "bron") (f "bron");
    cons_line = int_ (path / "line") (f "line");
    cons_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
  }

let resolved_uniciteit path j : resolved_uniciteit =
  let kvs = obj path j in
  expect_node path kvs "ResolvedUniciteit";
  check_keys path kvs ~ext:[ "scopeRol" ]
    [ "node"; "naam"; "subject"; "sleutels"; "bron"; "geldigheid" ];
  let f name = field path kvs name in
  {
    uni_naam = str_opt (path / "naam") (f "naam");
    uni_subject = str_opt (path / "subject") (f "subject");
    sleutels = str_opt_list (path / "sleutels") (f "sleutels");
    uni_bron = str_opt (path / "bron") (f "bron");
    uni_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
    scope_rol = ext_field path kvs "scopeRol" rol_binding;
  }

let uniciteit_unie_bron path j : uniciteit_unie_bron =
  let kvs = obj path j in
  expect_node path kvs "Bron";
  check_keys path kvs [ "node"; "type"; "sleutels" ];
  {
    uub_type = str_opt (path / "type") (field path kvs "type");
    uub_sleutels = str_opt_list (path / "sleutels") (field path kvs "sleutels");
  }

let resolved_uniciteit_unie path j : resolved_uniciteit_unie =
  let kvs = obj path j in
  expect_node path kvs "ResolvedUniciteitUnie";
  check_keys path kvs [ "node"; "naam"; "bronnen"; "bron"; "geldigheid" ];
  let f name = field path kvs name in
  {
    uu_naam = str_opt (path / "naam") (f "naam");
    bronnen =
      List.mapi
        (fun i x -> uniciteit_unie_bron (path / "bronnen" / string_of_int i) x)
        (list_ (path / "bronnen") (f "bronnen"));
    uu_bron = str_opt (path / "bron") (f "bron");
    uu_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
  }

let tussenstap path j : tussenstap =
  let kvs = obj path j in
  expect_node path kvs "Tussenstap";
  check_keys path kvs [ "node"; "rolBinding" ];
  { ts_rol = rol_binding (path / "rolBinding") (field path kvs "rolBinding") }

let resolved_feit_creatie path j : resolved_feit_creatie =
  let kvs = obj path j in
  expect_node path kvs "ResolvedFeitCreatie";
  check_keys path kvs
    [ "node"; "naam"; "anker"; "doel"; "tussenstappen"; "voorwaarde"; "geldigheid";
      "feittype"; "ankerRol"; "doelRol"; "joinRol" ];
  let f name = field path kvs name in
  {
    fc_naam = str_opt (path / "naam") (f "naam");
    anker = str_opt (path / "anker") (f "anker");
    fc_doel = str_opt (path / "doel") (f "doel");
    tussenstappen =
      List.mapi
        (fun i x -> tussenstap (path / "tussenstappen" / string_of_int i) x)
        (list_ (path / "tussenstappen") (f "tussenstappen"));
    fc_voorwaarde = opt (path / "voorwaarde") conditie (f "voorwaarde");
    fc_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
    fc_feittype = str (path / "feittype") (f "feittype");
    anker_rol = str (path / "ankerRol") (f "ankerRol");
    doel_rol = str (path / "doelRol") (f "doelRol");
    join_rol = rol_binding (path / "joinRol") (f "joinRol");
  }

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
  check_keys path kvs [ "node"; "naam"; "recursief"; "leden"; "bron"; "line" ];
  let f name = field path kvs name in
  {
    rgr_naam = str_opt (path / "naam") (f "naam");
    recursief = bool_ (path / "recursief") (f "recursief");
    leden = str_opt_list (path / "leden") (f "leden");
    rgr_bron = str_opt (path / "bron") (f "bron");
    rgr_line = int_ (path / "line") (f "line");
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
  check_keys path kvs ~ext:[ "consequentie"; "initialisatie"; "excepties" ]
    [ "node"; "besluit"; "voorwaarde"; "bron" ];
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
    default_bron = str_opt (path / "bron") (field path kvs "bron");
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
  check_keys path kvs [ "node"; "defaults"; "bron" ];
  {
    eg_defaults =
      List.mapi
        (fun i x -> default (path / "defaults" / string_of_int i) x)
        (list_ (path / "defaults") (field path kvs "defaults"));
    eg_bron = str_opt (path / "bron") (field path kvs "bron");
  }

let prioritized_defaults path j : prioritized_defaults =
  let kvs = obj path j in
  expect_node path kvs "PrioritizedDefaults";
  check_keys path kvs ~ext:[ "attribuut"; "ongeordend" ]
    [ "node"; "naam"; "onderwerp"; "kenmerk"; "defaults"; "bron"; "geldigheid" ];
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
    pd_bron = str_opt (path / "bron") (f "bron");
    pd_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
  }

(* Extension form (v1, row 10): a named reusable function (IR-SPEC ch. 2.11).
   Strict: name/parameters/result/body required, bron nullable. *)
let function_def path j : function_def =
  let kvs = obj path j in
  expect_node path kvs "FunctionDef";
  check_keys path kvs [ "node"; "name"; "parameters"; "result"; "body"; "bron" ];
  let f name = field path kvs name in
  {
    fd_naam = str (path / "name") (f "name");
    fd_parameters = lambda_params (path / "parameters") (f "parameters");
    fd_result = datatype (path / "result") (f "result");
    fd_body = expr (path / "body") (f "body");
    fd_bron = str_opt (path / "bron") (f "bron");
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

(* The M1 root: {format, model, rules, diagnostics}. [model] is the
   declaration set as lists, [rules] the v0 rule lists under their v0 keys;
   each carries one optional extension list. *)
let document (j : json) : document =
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
      "consistenties"; "uniciteiten"; "uniciteitUnies"; "feitCreaties";
      "resolvedStartpunten"; "regelGroepen" ];
  let arr p kvs name g =
    List.mapi (fun i x -> g (p / name / string_of_int i) x) (list_ (p / name) (field p kvs name))
  in
  let ext p kvs name g =
    match List.assoc_opt name kvs with
    | Some j -> List.mapi (fun i x -> g (p / name / string_of_int i) x) (list_ (p / name) j)
    | None -> []
  in
  let m name g = arr mpath model name g and r name g = arr rpath rules name g in
  {
    objecttypen = m "objectTypes" objecttype;
    domeinen = m "domains" domain;
    parameters = m "parameters" parameter;
    eenheidstelsels = m "unitSystems" eenheidstelsel;
    feittypen = m "factTypes" (fun p -> named p feittype (fun (x : feittype) -> x.ft_naam));
    dimensies = m "dimensions" (fun p -> named p dimensie (fun (x : dimensie) -> x.dim_naam));
    tijdlijnen = m "timelines" tijdlijn;
    dagsoorten = m "dayKinds" dagsoort;
    resolved_regels = r "resolvedRegels" resolved_regel;
    resolved_dagsoorten = r "resolvedDagsoorten" resolved_dagsoort;
    object_creaties = r "objectCreaties" resolved_object_creatie;
    verdelingen = r "verdelingen" resolved_verdeling;
    consistenties = r "consistenties" resolved_consistentie;
    uniciteiten = r "uniciteiten" resolved_uniciteit;
    uniciteit_unies = r "uniciteitUnies" resolved_uniciteit_unie;
    feit_creaties = r "feitCreaties" resolved_feit_creatie;
    resolved_startpunten = r "resolvedStartpunten" resolved_startpunt;
    regel_groepen = r "regelGroepen" resolved_regel_groep;
    diagnostics = arr path kvs "diagnostics" diagnostic;
    prioritized_defaults = ext rpath rules "prioritizedDefaults" prioritized_defaults;
    function_defs = ext mpath model "functionDefs" function_def;
    presentatie_annotaties = presentatie_annotaties j;
  }

let load_file (filename : string) : document =
  document (Yojson.Safe.from_file filename)
