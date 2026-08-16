(* Strict loader for the regelspraak-ir-v0 wire format.

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
   quadratic only on pathologically deep (100k+ nesting) documents. *)

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

let float_opt path = function
  | `Float f -> Some f
  | `Int i -> Some (float_of_int i)
  | `Null -> None
  | j -> fail path ("expected number or null, got " ^ Yojson.Safe.to_string j)

let opt path (f : string -> json -> 'a) : json -> 'a option = function
  | `Null -> None
  | j -> Some (f path j)

let str_opt_list path j =
  List.mapi (fun i x -> str_opt (path / string_of_int i) x) (list_ path j)

(* JSON map -> ordered assoc list; [f] decodes each value. *)
let map_ path (f : string -> json -> 'a) j : (string * 'a) list =
  List.map (fun (k, v) -> (k, f (path / k) v)) (obj path j)

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

let teken_opt =
  enum_opt "Teken"
    [ ("ONBEPAALD", Onbepaald); ("NEGATIEF", Negatief);
      ("NIET_NEGATIEF", Niet_negatief); ("POSITIEF", Positief) ]

let kenmerk_soort_opt =
  enum_opt "KenmerkSoort"
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
      ("LAATSTE", Laatste); ("WORTEL", Wortel);
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

(* --- eenheid and datatype ---------------------------------------------- *)

let rec eenheid path j : eenheid =
  let kvs = obj path j in
  expect_node path kvs "Eenheid";
  check_keys path kvs
    [ "node"; "afkorting"; "stelsel"; "canoniek"; "factorNaarCanoniek"; "noemer" ];
  {
    afkorting = str_opt (path / "afkorting") (field path kvs "afkorting");
    stelsel = str_opt (path / "stelsel") (field path kvs "stelsel");
    canoniek = str_opt (path / "canoniek") (field path kvs "canoniek");
    factor_naar_canoniek =
      float_opt (path / "factorNaarCanoniek") (field path kvs "factorNaarCanoniek");
    noemer = opt (path / "noemer") eenheid (field path kvs "noemer");
  }

let datatype path j : datatype =
  let kvs = obj path j in
  let f name = field path kvs name in
  match node_kind path kvs with
  | "Numeriek" ->
      check_keys path kvs [ "node"; "geheel"; "decimalen"; "teken"; "eenheid" ];
      Numeriek
        {
          geheel = bool_ (path / "geheel") (f "geheel");
          decimalen = int_ (path / "decimalen") (f "decimalen");
          teken = teken_opt (path / "teken") (f "teken");
          eenheid = opt (path / "eenheid") eenheid (f "eenheid");
        }
  | "Percentage" ->
      check_keys path kvs [ "node"; "geheel"; "decimalen"; "eenheid" ];
      Percentage
        {
          geheel = bool_ (path / "geheel") (f "geheel");
          decimalen = int_ (path / "decimalen") (f "decimalen");
          eenheid = opt (path / "eenheid") eenheid (f "eenheid");
        }
  | "Tekst" -> check_keys path kvs [ "node" ]; Tekst
  | "Booleaan" -> check_keys path kvs [ "node" ]; Booleaan
  | "DatumInDagen" -> check_keys path kvs [ "node" ]; Datum_in_dagen
  | "DatumTijd" -> check_keys path kvs [ "node" ]; Datum_tijd
  | "Enumeratie" ->
      check_keys path kvs [ "node"; "domein"; "waarden" ];
      Enumeratie
        {
          domein = str_opt (path / "domein") (f "domein");
          waarden = str_opt_list (path / "waarden") (f "waarden");
        }
  | "Onbekend" ->
      check_keys path kvs [ "node"; "reden" ];
      Datatype_onbekend { reden = str_opt (path / "reden") (f "reden") }
  | k -> fail (path / "node") ("unknown datatype node \"" ^ k ^ "\"")

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
      check_keys path kvs ~ext:[ "bronRol" ]
        [ "node"; "kind"; "argumenten"; "elementObjecttype"; "leegwaarde";
          "filter"; "dimensieSelectie"; "type"; "bron"; "granulariteit" ];
      Functie
        {
          kind = functie_kind (path / "kind") (f "kind");
          argumenten =
            List.mapi
              (fun i x -> expr (path / "argumenten" / string_of_int i) x)
              (list_ (path / "argumenten") (f "argumenten"));
          element_objecttype =
            str_opt (path / "elementObjecttype") (f "elementObjecttype");
          leegwaarde = opt (path / "leegwaarde") expr (f "leegwaarde");
          filter = opt (path / "filter") conditie (f "filter");
          dimensie_selectie =
            opt (path / "dimensieSelectie") dimensie_selectie (f "dimensieSelectie");
          typ = typ ();
          bron = bron ();
          granulariteit = granulariteit_opt (path / "granulariteit") (f "granulariteit");
          bron_rol = ext_field path kvs "bronRol" rol_binding;
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
      check_keys path kvs
        [ "node"; "attribuut"; "objecttype"; "onderwerpBron"; "navigatieRol";
          "type"; "bron"; "set"; "navigatiePad" ];
      Attribuut_ref
        {
          attribuut = str_opt (path / "attribuut") (f "attribuut");
          objecttype = str_opt (path / "objecttype") (f "objecttype");
          onderwerp_bron = str_opt (path / "onderwerpBron") (f "onderwerpBron");
          navigatie_rol = str_opt (path / "navigatieRol") (f "navigatieRol");
          typ = typ ();
          bron = bron ();
          set = bool_ (path / "set") (f "set");
          navigatie_pad = str_opt_list (path / "navigatiePad") (f "navigatiePad");
        }
  | "DimensieCel" ->
      check_keys path kvs [ "node"; "attribuut"; "labels"; "type"; "bron" ];
      Dimensie_cel
        {
          attribuut = opt (path / "attribuut") expr (f "attribuut");
          labels = str_opt_list (path / "labels") (f "labels");
          typ = typ ();
          bron = bron ();
        }
  | "ParameterRef" ->
      check_keys path kvs [ "node"; "parameter"; "type"; "bron" ];
      Parameter_ref
        { parameter = str_opt (path / "parameter") (f "parameter"); typ = typ (); bron = bron () }
  | "VariabeleRef" ->
      check_keys path kvs [ "node"; "naam"; "type"; "bron" ];
      Variabele_ref { naam = str_opt (path / "naam") (f "naam"); typ = typ (); bron = bron () }
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
  | "Constante" ->
      check_keys path kvs [ "node"; "naam"; "type"; "bron" ];
      Constante { naam = str_opt (path / "naam") (f "naam"); typ = typ (); bron = bron () }
  | "Onbekend" ->
      check_keys path kvs [ "node"; "type"; "bron" ];
      Expr_onopgelost { typ = typ (); bron = bron () }
  | k -> fail (path / "node") ("unknown expr node \"" ^ k ^ "\"")

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
      check_keys path kvs
        [ "node"; "kenmerk"; "navigatie"; "negatie"; "eigenaar"; "periode"; "bron" ];
      Kenmerk_check
        {
          kenmerk = str_opt (path / "kenmerk") (f "kenmerk");
          navigatie = str_opt (path / "navigatie") (f "navigatie");
          negatie = bool_ (path / "negatie") (f "negatie");
          eigenaar = str_opt (path / "eigenaar") (f "eigenaar");
          periode = periode ();
          bron = bron ();
        }
  | "Rollidmaatschap" ->
      check_keys path kvs [ "node"; "rol"; "anderTeFiller"; "negatie"; "bron" ];
      Rollidmaatschap
        {
          rol = str_opt (path / "rol") (f "rol");
          ander_te_filler = str_opt (path / "anderTeFiller") (f "anderTeFiller");
          negatie = bool_ (path / "negatie") (f "negatie");
          bron = bron ();
        }
  | "VerreRollidmaatschap" ->
      check_keys path kvs [ "node"; "filler"; "backref"; "negatie"; "bron" ];
      Verre_rollidmaatschap
        {
          filler = str_opt (path / "filler") (f "filler");
          backref = str_opt (path / "backref") (f "backref");
          negatie = bool_ (path / "negatie") (f "negatie");
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
  | "Regelversie" ->
      check_keys path kvs [ "node"; "regel"; "inconsistent"; "bron" ];
      Regelversie
        {
          regel = str_opt (path / "regel") (f "regel");
          inconsistent = bool_ (path / "inconsistent") (f "inconsistent");
          bron = bron ();
        }
  | "Onbekend" ->
      check_keys path kvs [ "node"; "bron" ];
      Conditie_onopgelost { bron = bron () }
  | k -> fail (path / "node") ("unknown conditie node \"" ^ k ^ "\"")

(* --- model layer -------------------------------------------------------- *)

let objecttype path j : objecttype =
  let kvs = obj path j in
  expect_node path kvs "Objecttype";
  check_keys path kvs
    [ "node"; "naam"; "attributen"; "attribuutMeervoud"; "attribuutDimensies";
      "attribuutGranulariteit"; "attribuutTijdlijnAantal"; "attribuutTijdlijnNaam";
      "kenmerken"; "kenmerkSoorten"; "kenmerkGranulariteit"; "kenmerkTijdlijnAantal";
      "bezield"; "meervoud" ];
  let f name = field path kvs name in
  {
    ot_naam = str_opt (path / "naam") (f "naam");
    attributen = map_ (path / "attributen") (fun p j -> opt p datatype j) (f "attributen");
    attribuut_meervoud = map_ (path / "attribuutMeervoud") str_opt (f "attribuutMeervoud");
    attribuut_dimensies =
      map_ (path / "attribuutDimensies") str_opt_list (f "attribuutDimensies");
    attribuut_granulariteit =
      map_ (path / "attribuutGranulariteit") granulariteit_opt (f "attribuutGranulariteit");
    attribuut_tijdlijn_aantal =
      map_ (path / "attribuutTijdlijnAantal") int_opt (f "attribuutTijdlijnAantal");
    attribuut_tijdlijn_naam =
      map_ (path / "attribuutTijdlijnNaam") str_opt (f "attribuutTijdlijnNaam");
    kenmerken = str_opt_list (path / "kenmerken") (f "kenmerken");
    kenmerk_soorten = map_ (path / "kenmerkSoorten") kenmerk_soort_opt (f "kenmerkSoorten");
    kenmerk_granulariteit =
      map_ (path / "kenmerkGranulariteit") granulariteit_opt (f "kenmerkGranulariteit");
    kenmerk_tijdlijn_aantal =
      map_ (path / "kenmerkTijdlijnAantal") int_opt (f "kenmerkTijdlijnAantal");
    bezield = bool_ (path / "bezield") (f "bezield");
    meervoud = str_opt (path / "meervoud") (f "meervoud");
  }

let parameter path j : parameter =
  let kvs = obj path j in
  expect_node path kvs "Parameter";
  check_keys path kvs [ "node"; "naam"; "datatype"; "granulariteit" ];
  {
    par_naam = str_opt (path / "naam") (field path kvs "naam");
    par_datatype = opt (path / "datatype") datatype (field path kvs "datatype");
    par_granulariteit =
      granulariteit_opt (path / "granulariteit") (field path kvs "granulariteit");
  }

let eenheidstelsel path j : eenheidstelsel =
  let kvs = obj path j in
  expect_node path kvs "Eenheidstelsel";
  check_keys path kvs [ "node"; "naam"; "eenheden"; "aliassen" ];
  {
    es_naam = str_opt (path / "naam") (field path kvs "naam");
    eenheden =
      map_ (path / "eenheden") (fun p j -> opt p eenheid j) (field path kvs "eenheden");
    aliassen =
      map_ (path / "aliassen") (fun p j -> opt p eenheid j) (field path kvs "aliassen");
  }

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

let regel_stub path j : regel_stub =
  let kvs = obj path j in
  expect_node path kvs "Regel";
  check_keys path kvs [ "node"; "naam"; "soort" ];
  {
    rg_naam = str_opt (path / "naam") (field path kvs "naam");
    rg_soort = str_opt (path / "soort") (field path kvs "soort");
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
  check_keys path kvs [ "node"; "naam"; "soort"; "doel"; "waarde"; "voorwaarde"; "geldigheid" ];
  let f name = field path kvs name in
  {
    rr_naam = str_opt (path / "naam") (f "naam");
    rr_soort = regel_soort_opt (path / "soort") (f "soort");
    doel = opt (path / "doel") expr (f "doel");
    waarde = opt (path / "waarde") expr (f "waarde");
    rr_voorwaarde = opt (path / "voorwaarde") conditie (f "voorwaarde");
    rr_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
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
  check_keys path kvs
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
  }

let resolved_verdeling path j : resolved_verdeling =
  let kvs = obj path j in
  expect_node path kvs "ResolvedVerdeling";
  check_keys path kvs ~ext:[ "verbindendeRol" ]
    [ "node"; "naam"; "bronType"; "bronAttribuut"; "doelType"; "doelAttribuut";
      "methode"; "ratoAttribuut"; "maximumAttribuut"; "afrondingDecimalen";
      "restantAttribuut"; "groepeerAttribuut"; "afnemend"; "voorwaarde"; "geldigheid" ];
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
    verbindende_rol = ext_field path kvs "verbindendeRol" rol_binding;
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
  check_keys path kvs [ "node"; "naam"; "subject"; "sleutels"; "scopeVeld"; "bron"; "geldigheid" ];
  let f name = field path kvs name in
  {
    uni_naam = str_opt (path / "naam") (f "naam");
    uni_subject = str_opt (path / "subject") (f "subject");
    sleutels = str_opt_list (path / "sleutels") (f "sleutels");
    scope_veld = str_opt (path / "scopeVeld") (f "scopeVeld");
    uni_bron = str_opt (path / "bron") (f "bron");
    uni_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
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
  check_keys path kvs ~ext:[ "rolBinding" ] [ "node"; "type"; "veld"; "viaOuderVeld" ];
  {
    ts_type = str_opt (path / "type") (field path kvs "type");
    veld = str_opt (path / "veld") (field path kvs "veld");
    via_ouder_veld = bool_ (path / "viaOuderVeld") (field path kvs "viaOuderVeld");
    ts_rol = ext_field path kvs "rolBinding" rol_binding;
  }

let resolved_feit_creatie path j : resolved_feit_creatie =
  let kvs = obj path j in
  expect_node path kvs "ResolvedFeitCreatie";
  check_keys path kvs ~ext:[ "feittype"; "ankerRol"; "doelRol"; "joinRol" ]
    [ "node"; "naam"; "anker"; "doel"; "zetVeld"; "joinVeld"; "tussenstappen";
      "voorwaarde"; "geldigheid" ];
  let f name = field path kvs name in
  {
    fc_naam = str_opt (path / "naam") (f "naam");
    anker = str_opt (path / "anker") (f "anker");
    fc_doel = str_opt (path / "doel") (f "doel");
    zet_veld = str_opt (path / "zetVeld") (f "zetVeld");
    join_veld = str_opt (path / "joinVeld") (f "joinVeld");
    tussenstappen =
      List.mapi
        (fun i x -> tussenstap (path / "tussenstappen" / string_of_int i) x)
        (list_ (path / "tussenstappen") (f "tussenstappen"));
    fc_voorwaarde = opt (path / "voorwaarde") conditie (f "voorwaarde");
    fc_geldigheid = opt (path / "geldigheid") geldigheid (f "geldigheid");
    fc_feittype = ext_field path kvs "feittype" str;
    anker_rol = ext_field path kvs "ankerRol" str;
    doel_rol = ext_field path kvs "doelRol" str;
    join_rol = ext_field path kvs "joinRol" rol_binding;
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

let document (j : json) : document =
  let path = "" in
  let kvs = obj path j in
  check_keys path kvs
    [ "formaat"; "objecttypen"; "domeinen"; "parameters"; "eenheidstelsels";
      "feittypen"; "dimensies"; "tijdlijnen"; "dagsoorten"; "dagsoortPerEenheid";
      "regels"; "resolvedRegels"; "resolvedDagsoorten"; "objectCreaties";
      "verdelingen"; "consistenties"; "uniciteiten"; "uniciteitUnies";
      "feitCreaties"; "resolvedStartpunten"; "regelGroepen"; "diagnostics" ];
  let f name = field path kvs name in
  (match str (path / "formaat") (f "formaat") with
  | "regelspraak-ir-v0" -> ()
  | s -> fail (path / "formaat") ("unknown format \"" ^ s ^ "\""));
  let arr name g =
    List.mapi (fun i x -> g (path / name / string_of_int i) x) (list_ (path / name) (f name))
  in
  {
    objecttypen = map_ (path / "objecttypen") objecttype (f "objecttypen");
    domeinen = map_ (path / "domeinen") (fun p j -> opt p datatype j) (f "domeinen");
    parameters = map_ (path / "parameters") parameter (f "parameters");
    eenheidstelsels = map_ (path / "eenheidstelsels") eenheidstelsel (f "eenheidstelsels");
    feittypen = map_ (path / "feittypen") feittype (f "feittypen");
    dimensies = map_ (path / "dimensies") dimensie (f "dimensies");
    tijdlijnen = arr "tijdlijnen" tijdlijn;
    dagsoorten = str_opt_list (path / "dagsoorten") (f "dagsoorten");
    dagsoort_per_eenheid = map_ (path / "dagsoortPerEenheid") str_opt (f "dagsoortPerEenheid");
    regels = arr "regels" regel_stub;
    resolved_regels = arr "resolvedRegels" resolved_regel;
    resolved_dagsoorten = arr "resolvedDagsoorten" resolved_dagsoort;
    object_creaties = arr "objectCreaties" resolved_object_creatie;
    verdelingen = arr "verdelingen" resolved_verdeling;
    consistenties = arr "consistenties" resolved_consistentie;
    uniciteiten = arr "uniciteiten" resolved_uniciteit;
    uniciteit_unies = arr "uniciteitUnies" resolved_uniciteit_unie;
    feit_creaties = arr "feitCreaties" resolved_feit_creatie;
    resolved_startpunten = arr "resolvedStartpunten" resolved_startpunt;
    regel_groepen = arr "regelGroepen" resolved_regel_groep;
    diagnostics = arr "diagnostics" diagnostic;
  }

let load_file (filename : string) : document =
  document (Yojson.Safe.from_file filename)
