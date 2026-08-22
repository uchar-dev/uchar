(* Serialiser: Ir_v0.Ast values -> regelspraak-ir-v0 wire JSON.

   ARCHITECTURE. The plugin builds typed [Ir_v0.Ast] values and hands
   them to exactly one serialiser — this module — so schema conformity is
   structural, not stringly-typed: the mapping code never assembles JSON
   by hand. This module is the deliberate inverse of
   [Ir_v0.Loader]: every key name, node discriminator and enum spelling
   mirrors the loader's [check_keys]/[expect_node]/[enum] tables, so a
   value that round-trips through Loader is what we emit.

   FINDING (reported on the card): the [uchar-ir-v0] library ships a
   strict Loader (JSON->AST) but NO AST->JSON serialiser — the
   byte-identical round-trip of gate 2 (scripts/check-ir-schema.sh) is
   carried by Python, and the corpus goldens arrive from the upstream
   emitter. So this serialiser had to be written here. It is not the
   byte-identical canonical emitter (2-space, lowercase \uXXXX escapes);
   it emits loader-valid JSON, which is all gates the plugin's output
   meets (verifier/ref/clingo parse it). Byte-identical canonicalisation
   is a corpus concern (kaart 2), not a skeleton concern.

   Fail-fast: node kinds the skeleton never builds raise [Failure] with
   context rather than emitting a guessed shape. *)

open Ir_v0.Ast

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

let node k rest : json = `Assoc (("node", `String k) :: rest)

let teken_s = function
  | Onbepaald -> "ONBEPAALD" | Negatief -> "NEGATIEF"
  | Niet_negatief -> "NIET_NEGATIEF" | Positief -> "POSITIEF"

let kenmerk_soort_s = function
  | Bijvoeglijk -> "BIJVOEGLIJK" | Bezittelijk -> "BEZITTELIJK"
  | Zelfstandig -> "ZELFSTANDIG"

let bin_op_s = function
  | Plus -> "PLUS" | Min -> "MIN" | Verminderd_met -> "VERMINDERD_MET"
  | Maal -> "MAAL" | Gedeeld_door -> "GEDEELD_DOOR"
  | Gedeeld_door_abs -> "GEDEELD_DOOR_ABS" | Tot_de_macht -> "TOT_DE_MACHT"
  | Percentage_van -> "PERCENTAGE_VAN"

let vergelijk_op_s = function
  | Kleiner -> "KLEINER" | Kleiner_of_gelijk -> "KLEINER_OF_GELIJK"
  | Groter -> "GROTER" | Groter_of_gelijk -> "GROTER_OF_GELIJK"
  | Gelijk -> "GELIJK" | Ongelijk -> "ONGELIJK"
  | Eerder -> "EERDER" | Eerder_of_gelijk -> "EERDER_OF_GELIJK"
  | Later -> "LATER" | Later_of_gelijk -> "LATER_OF_GELIJK"

let kwantor_s = function
  | De -> "DE" | Alle -> "ALLE" | Geen -> "GEEN" | Ten_minste -> "TEN_MINSTE"
  | Ten_hoogste -> "TEN_HOOGSTE" | Precies -> "PRECIES"

let regel_soort_s = function
  | Gelijkstelling -> "GELIJKSTELLING" | Berekening -> "BEREKENING"
  | Initialisatie -> "INITIALISATIE" | Kenmerktoekenning -> "KENMERKTOEKENNING"

let afrond_modus_s = function
  | Naar_beneden -> "NAAR_BENEDEN" | Naar_boven -> "NAAR_BOVEN"
  | Rekenkundig -> "REKENKUNDIG" | Richting_nul -> "RICHTING_NUL"
  | Weg_van_nul -> "WEG_VAN_NUL"

let rec datatype (d : datatype) : json =
  match d with
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      node "Numeriek"
        [ ("geheel", b geheel); ("decimalen", i decimalen);
          ("teken", opt (fun t -> s (teken_s t)) teken);
          ("eenheid", opt eenheid_j eenheid) ]
  | Booleaan -> node "Booleaan" []
  | Datum_in_dagen -> node "DatumInDagen" []
  | Datum_tijd -> node "DatumTijd" []
  | Tekst -> node "Tekst" []
  | Percentage { geheel; decimalen; eenheid } ->
      node "Percentage"
        [ ("geheel", b geheel); ("decimalen", i decimalen);
          ("eenheid", opt eenheid_j eenheid) ]
  | Enumeratie { domein; waarden } ->
      node "Enumeratie" [ ("domein", so domein); ("waarden", str_opt_list waarden) ]
  | Sum_type { domein; constructoren } ->
      node "SumType"
        [ ("domain", s domein);
          ("constructors",
           `List
             (List.map
                (fun (nm, payload) ->
                  `Assoc [ ("name", s nm); ("payload", opt datatype payload) ])
                constructoren)) ]
  | List_type { element } -> node "ListType" [ ("element", datatype element) ]
  | Struct_type { domein; velden } ->
      node "StructType"
        [ ("domain", s domein);
          ("fields",
           `List
             (List.map
                (fun (nm, dt) -> `Assoc [ ("name", s nm); ("datatype", datatype dt) ])
                velden)) ]
  | Datatype_onbekend { reden } -> node "Onbekend" [ ("reden", so reden) ]

and eenheid_j (e : eenheid) : json =
  node "Eenheid"
    [ ("afkorting", so e.afkorting); ("stelsel", so e.stelsel);
      ("canoniek", so e.canoniek);
      ("factorNaarCanoniek",
       (match e.factor_naar_canoniek with None -> `Null | Some f -> `Float f));
      ("noemer", opt eenheid_j e.noemer) ]

let geheel_periode (g : geheel_periode) : json =
  node "GeheelPeriode" [ ("jaar", b g.gp_jaar); ("niet", b g.gp_niet) ]

let rec expr (e : expr) : json =
  let typ t = ("type", opt datatype t) in
  let bron br = ("bron", so br) in
  match e with
  | Literal { waarde; typ = t; bron = br } ->
      node "Literal" [ ("waarde", so waarde); typ t; bron br ]
  | Binair { op; links; rechts; typ = t; bron = br } ->
      node "Binair"
        [ ("op", s (bin_op_s op)); ("links", opt expr links);
          ("rechts", opt expr rechts); typ t; bron br ]
  | Afgerond { basis; modus; decimalen; typ = t; bron = br } ->
      node "Afgerond"
        [ ("basis", opt expr basis); ("modus", s (afrond_modus_s modus));
          ("decimalen", i decimalen); typ t; bron br ]
  | Attribuut_ref
      { attribuut; objecttype; onderwerp_bron; navigatie_rol; typ = t; bron = br;
        set; navigatie_pad; navigatie_bindingen = _ } ->
      node "AttribuutRef"
        [ ("attribuut", so attribuut); ("objecttype", so objecttype);
          ("onderwerpBron", so onderwerp_bron); ("navigatieRol", so navigatie_rol);
          typ t; bron br; ("set", b set);
          ("navigatiePad", str_opt_list navigatie_pad) ]
  | Parameter_ref { parameter; typ = t; bron = br } ->
      node "ParameterRef" [ ("parameter", so parameter); typ t; bron br ]
  | Variabele_ref { naam; typ = t; bron = br } ->
      node "VariabeleRef" [ ("naam", so naam); typ t; bron br ]
  | Constante { naam; typ = t; bron = br } ->
      node "Constante" [ ("naam", so naam); typ t; bron br ]
  | Expr_onopgelost { typ = t; bron = br } -> node "Onopgelost" [ typ t; bron br ]
  | Construct { typ = t; constructor_; argument; bron = br } ->
      node "Construct"
        [ ("type", opt datatype t); ("constructor", s constructor_);
          ("argument", opt expr argument); bron br ]
  | Match { typ = t; scrutinee; arms; bron = br } ->
      node "Match"
        [ ("type", opt datatype t); ("scrutinee", expr scrutinee);
          ("arms",
           `List
             (List.map
                (fun (c, binder, body) ->
                  `Assoc
                    [ ("constructor", s c); ("binder", so binder);
                      ("body", expr body) ])
                arms));
          bron br ]
  | Binder_ref { naam; typ = t; bron = br } ->
      node "BinderRef" [ ("name", s naam); typ t; bron br ]
  | List_literal { typ = t; elements; bron = br } ->
      node "ListLiteral"
        [ ("type", opt datatype t); ("elements", `List (List.map expr elements)); bron br ]
  | If_then_else { typ = t; condition; then_; else_; bron = br } ->
      node "IfThenElse"
        [ ("type", opt datatype t); ("condition", conditie condition);
          ("then", expr then_); ("else", expr else_); bron br ]
  | Invoke { typ = t; functie; arguments; bron = br } ->
      node "Invoke"
        [ ("type", opt datatype t); ("function", s functie);
          ("arguments", `List (List.map expr arguments)); bron br ]
  | Fold { typ = t; list; functie; init; bron = br } ->
      node "Fold"
        [ ("type", opt datatype t); ("list", expr list);
          ("function", fold_fn functie); ("init", expr init); bron br ]
  | Map_list { typ = t; list; functie; bron = br } ->
      node "MapList"
        [ ("type", opt datatype t); ("list", expr list);
          ("function", fold_fn functie); bron br ]
  | Filter_list { typ = t; list; binder; condition; bron = br } ->
      node "FilterList"
        [ ("type", opt datatype t); ("list", expr list); ("binder", s binder);
          ("condition", conditie condition); bron br ]
  | List_length { typ = t; list; bron = br } ->
      node "ListLength" [ ("type", opt datatype t); ("list", expr list); bron br ]
  | Struct_literal { typ = t; velden; bron = br } ->
      node "StructLiteral"
        [ ("type", opt datatype t);
          ("fields",
           `List
             (List.map
                (fun (nm, v) -> `Assoc [ ("name", s nm); ("value", expr v) ])
                velden));
          bron br ]
  | Field_access { typ = t; base; veld; bron = br } ->
      node "FieldAccess"
        [ ("type", opt datatype t); ("base", expr base); ("field", s veld); bron br ]
  | other ->
      failwith
        ("Emit.expr: node not produced by the skeleton: "
        ^ (match other with
          | Tekst_reeks _ -> "TekstReeks" | Functie _ -> "Functie"
          | Afgerond _ -> "Afgerond" | Begrensd _ -> "Begrensd"
          | Dimensie_cel _ -> "DimensieCel" | Temporal_read _ -> "TemporalRead"
          | Lambda _ -> "Lambda (position-restricted; only a Fold/MapList operand)"
          | _ -> "?"))

(* the Fold/MapList [function] operand: an inline Lambda object or a bare
   FunctionDef-name string (the two wire forms of IR-SPEC ch. 4.8). *)
and fold_fn (f : fold_fn) : json =
  match f with
  | Fn_name n -> `String n
  | Fn_lambda { parameters; body; bron = br } ->
      node "Lambda"
        [ ("parameters", param_list parameters); ("body", expr body); ("bron", so br) ]

(* Lambda / FunctionDef parameters: a name and a datatype, both required. *)
and param_list (ps : (string * datatype) list) : json =
  `List (List.map (fun (n, dt) -> `Assoc [ ("name", s n); ("datatype", datatype dt) ]) ps)

and conditie (c : conditie) : json =
  let bron br = ("bron", so br) in
  let periode p = ("periode", opt geheel_periode p) in
  match c with
  | Vergelijking { op; links; rechts; periode = p; bron = br } ->
      node "Vergelijking"
        [ ("op", s (vergelijk_op_s op)); ("links", opt expr links);
          ("rechts", opt expr rechts); periode p; bron br ]
  | Samengesteld { kwantor; drempel; onderdelen; bron = br } ->
      node "Samengesteld"
        [ ("kwantor", s (kwantor_s kwantor)); ("drempel", i drempel);
          ("onderdelen", `List (List.map conditie onderdelen)); bron br ]
  | Kenmerk_check
      { kenmerk; navigatie; negatie; eigenaar; periode = p; bron = br;
        navigatie_binding = _ } ->
      node "Kenmerk"
        [ ("kenmerk", so kenmerk); ("navigatie", so navigatie);
          ("negatie", b negatie); ("eigenaar", so eigenaar); periode p; bron br ]
  | Conditie_onopgelost { bron = br } -> node "Onopgelost" [ bron br ]
  | _ -> failwith "Emit.conditie: condition shape not produced by the skeleton"

let geldigheid (g : geldigheid) : json =
  node "Geldigheid" [ ("vanaf", so g.geld_vanaf); ("tot", so g.geld_tot) ]

let objecttype (o : objecttype) : json =
  node "Objecttype"
    [ ("naam", so o.ot_naam);
      ("attributen", map_ (opt datatype) o.attributen);
      ("attribuutMeervoud", map_ so o.attribuut_meervoud);
      ("attribuutDimensies", map_ str_opt_list o.attribuut_dimensies);
      ("attribuutGranulariteit", map_ (fun _ -> `Null) o.attribuut_granulariteit);
      ("attribuutTijdlijnAantal", map_ io o.attribuut_tijdlijn_aantal);
      ("attribuutTijdlijnNaam", map_ so o.attribuut_tijdlijn_naam);
      ("kenmerken", str_opt_list o.kenmerken);
      ("kenmerkSoorten",
       map_ (opt (fun k -> s (kenmerk_soort_s k))) o.kenmerk_soorten);
      ("kenmerkGranulariteit", map_ (fun _ -> `Null) o.kenmerk_granulariteit);
      ("kenmerkTijdlijnAantal", map_ io o.kenmerk_tijdlijn_aantal);
      ("bezield", b o.bezield); ("meervoud", so o.meervoud) ]

let resolved_regel (r : resolved_regel) : json =
  node "ResolvedRegel"
    [ ("naam", so r.rr_naam);
      ("soort", opt (fun x -> s (regel_soort_s x)) r.rr_soort);
      ("doel", opt expr r.doel); ("waarde", opt expr r.waarde);
      ("voorwaarde", opt conditie r.rr_voorwaarde);
      ("geldigheid", opt geldigheid r.rr_geldigheid) ]

let resolved_consistentie (c : resolved_consistentie) : json =
  node "ResolvedConsistentie"
    [ ("naam", so c.cons_naam); ("subject", so c.cons_subject);
      ("assertie", opt conditie c.assertie); ("bron", so c.cons_bron);
      ("line", i c.cons_line); ("geldigheid", opt geldigheid c.cons_geldigheid) ]

let rec rung (r : rung) : json =
  (* [besluit] is a required key (null on an attribute / group rung);
     [consequentie], [initialisatie] and [excepties] are extension fields —
     written only when present, matching the loader's [ext] admission. *)
  let base =
    [ ("node", `String "Rung");
      ("besluit", (match r.rung_besluit with None -> `Null | Some x -> `Bool x));
      ("voorwaarde", opt conditie r.rung_voorwaarde) ]
  in
  let conseq =
    match r.rung_consequentie with None -> [] | Some e -> [ ("consequentie", expr e) ]
  in
  let init = if r.rung_initialisatie then [ ("initialisatie", `Bool true) ] else [] in
  let excepties =
    match r.rung_excepties with None -> [] | Some sg -> [ ("excepties", sub_group sg) ]
  in
  `Assoc (base @ conseq @ init @ [ ("bron", so r.rung_bron) ] @ excepties)

(* the nested exception sub-group riding [excepties] (v1, Phase 5c): the
   {ongeordend, leden} object (no node discriminator) and its [SubLadder]
   members, mirroring the loader's [sub_group]/[sub_ladder]. *)
and sub_group (sg : sub_group) : json =
  `Assoc
    [ ("ongeordend", b sg.sg_ongeordend);
      ("leden", `List (List.map sub_ladder sg.sg_leden)) ]

and sub_ladder (sl : sub_ladder) : json =
  node "SubLadder"
    [ ("rungs", `List (List.map rung sl.sl_rungs)); ("bron", so sl.sl_bron) ]

let default_ladder (l : default_ladder) : json =
  let attribuut = match l.dl_attribuut with None -> [] | Some a -> [ ("attribuut", `String a) ] in
  let ongeordend = if l.dl_ongeordend then [ ("ongeordend", `Bool true) ] else [] in
  `Assoc
    ([ ("node", `String "DefaultLadder"); ("naam", so l.dl_naam);
       ("onderwerp", `String l.dl_onderwerp); ("kenmerk", so l.dl_kenmerk) ]
    @ attribuut
    @ [ ("rungs", `List (List.map rung l.rungs)) ]
    @ ongeordend
    @ [ ("bron", so l.dl_bron); ("geldigheid", opt geldigheid l.dl_geldigheid) ])

let function_def (fd : function_def) : json =
  node "FunctionDef"
    [ ("name", s fd.fd_naam); ("parameters", param_list fd.fd_parameters);
      ("result", datatype fd.fd_result); ("body", expr fd.fd_body);
      ("bron", so fd.fd_bron) ]

let document (d : document) : json =
  (* functionDefs is an extension top-level key, written only when non-empty —
     the optional-field pattern the loader admits for defaultLadders. *)
  let function_defs =
    match d.function_defs with [] -> [] | fds -> [ ("functionDefs", `List (List.map function_def fds)) ]
  in
  `Assoc
    (function_defs
    @ [ ("formaat", `String "regelspraak-ir-v0");
      ("objecttypen", map_ objecttype d.objecttypen);
      ("domeinen", map_ (opt datatype) d.domeinen);
      ("parameters", `Assoc []);
      ("eenheidstelsels", `Assoc []);
      ("feittypen", `Assoc []);
      ("dimensies", `Assoc []);
      ("tijdlijnen", `List []);
      ("dagsoorten", `List []);
      ("dagsoortPerEenheid", `Assoc []);
      ("regels", `List []);
      ("resolvedRegels", `List (List.map resolved_regel d.resolved_regels));
      ("resolvedDagsoorten", `List []);
      ("objectCreaties", `List []);
      ("verdelingen", `List []);
      ("consistenties", `List (List.map resolved_consistentie d.consistenties));
      ("uniciteiten", `List []);
      ("uniciteitUnies", `List []);
      ("feitCreaties", `List []);
      ("resolvedStartpunten", `List []);
      ("regelGroepen", `List []);
      ("diagnostics", `List []);
      ("defaultLadders", `List (List.map default_ladder d.default_ladders)) ])

let to_string (d : document) : string = Yojson.Safe.pretty_to_string (document d) ^ "\n"

let to_file (path : string) (d : document) : unit =
  let oc = open_out path in
  output_string oc (to_string d);
  close_out oc
