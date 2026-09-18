(* Serialiser: Ir_v0.Ast values -> regelspraak-ir-v0 wire JSON.

   The lowering builds typed [Ir_v0.Ast] values and hands them to exactly
   one serialiser — this module — so schema conformity is structural
   rather than stringly-typed. Key names, node discriminators and enum
   spellings mirror [Ir_v0.Loader]'s tables, and the key ORDER is the
   corpus's own, so the output canonicalises (gate 2's serialiser shape)
   into the checked-in golden byte for byte.

   The [uchar-ir-v0] library ships a strict loader but no serialiser, so
   a frontend that emits has to carry one. This one covers exactly the
   node set the F-logic slice produces and raises [Failure] with context
   on anything else — a guessed shape would be worse than a crash. *)

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

let functie_kind_s = function
  | Som -> "SOM" | Aantal -> "AANTAL"
  | _ -> failwith "Emit: aggregation kind not produced by the F-logic slice"

let datatype (d : datatype) : json =
  match d with
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      if eenheid <> None then failwith "Emit: the slice declares no units";
      node "Numeriek"
        [ ("geheel", b geheel); ("decimalen", i decimalen);
          ("teken", opt (fun t -> s (teken_s t)) teken); ("eenheid", `Null) ]
  | Booleaan -> node "Booleaan" []
  | Tekst -> node "Tekst" []
  | Datum_in_dagen -> node "DatumInDagen" []
  | Datatype_onbekend { reden } -> node "Onbekend" [ ("reden", so reden) ]
  | _ -> failwith "Emit: datatype not produced by the F-logic slice"

let rol_binding (rb : rol_binding) : json =
  node "RolBinding" [ ("feittype", s rb.rb_feittype); ("rol", s rb.rb_rol) ]

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
  | Attribuut_ref
      { attribuut; objecttype; onderwerp_bron; navigatie_rol; typ = t; bron = br;
        set; navigatie_pad; navigatie_bindingen } ->
      if navigatie_bindingen <> None then
        failwith "Emit: the slice synthesises no navigation bindings";
      node "AttribuutRef"
        [ ("attribuut", so attribuut); ("objecttype", so objecttype);
          ("onderwerpBron", so onderwerp_bron); ("navigatieRol", so navigatie_rol);
          typ t; bron br; ("set", b set);
          ("navigatiePad", str_opt_list navigatie_pad) ]
  | Functie
      { kind; argumenten; element_objecttype; leegwaarde; filter; dimensie_selectie;
        typ = t; bron = br; granulariteit; bron_rol; bron_pad } ->
      if filter <> None || dimensie_selectie <> None || granulariteit <> None
         || bron_pad <> None then
        failwith "Emit: aggregation qualifier not produced by the F-logic slice";
      node "Functie"
        [ ("kind", s (functie_kind_s kind));
          ("argumenten", `List (List.map expr argumenten));
          ("elementObjecttype", so element_objecttype);
          ("leegwaarde", opt expr leegwaarde);
          ("filter", `Null); ("dimensieSelectie", `Null); typ t; bron br;
          ("granulariteit", `Null); ("bronRol", opt rol_binding bron_rol) ]
  | _ -> failwith "Emit: expression node not produced by the F-logic slice"

let rec conditie (c : conditie) : json =
  let bron br = ("bron", so br) in
  match c with
  | Vergelijking { op; links; rechts; periode; bron = br } ->
      if periode <> None then failwith "Emit: the slice writes no period qualifier";
      node "Vergelijking"
        [ ("op", s (vergelijk_op_s op)); ("links", opt expr links);
          ("rechts", opt expr rechts); ("periode", `Null); bron br ]
  | Samengesteld { kwantor; drempel; onderdelen; bron = br } ->
      node "Samengesteld"
        [ ("kwantor", s (kwantor_s kwantor)); ("drempel", i drempel);
          ("onderdelen", `List (List.map (fun c ->
             match c with
             | Vergelijking _ | Kenmerk_check _ -> conditie_leaf c
             (* a fold beside an ordinary condition of the subject: the two
                conjoin, and the fold keeps its own shape inside *)
             | Kwantorvouw _ -> conditie c
             | _ -> failwith "Emit: nested composite not produced by the F-logic slice")
             onderdelen));
          bron br ]
  (* The quantifier fold. [drempel] is present EXACTLY under the three
     counting quantifiers (WF-108 (e)) and absent otherwise, so it is
     written conditionally rather than as a null nobody may consult; the
     range is always the one-step [bronRol], never [bronPad]. *)
  | Kwantorvouw
      { kv_kwantor; kv_drempel; kv_element_objecttype; kv_filter; kv_toets; kv_bron;
        kv_bron_rol; kv_bron_pad } ->
      if kv_bron_pad <> None then
        failwith "Emit: the slice writes no multi-step quantifier range";
      let rb =
        match kv_bron_rol with
        | Some rb -> rb
        | None -> failwith "Emit: a quantifier fold without its role binding"
      in
      node "Kwantorvouw"
        ([ ("kwantor", s (kwantor_s kv_kwantor)) ]
        @ (match kv_drempel with None -> [] | Some d -> [ ("drempel", i d) ])
        @ [ ("elementObjecttype", so kv_element_objecttype);
            ("filter", opt conditie kv_filter);
            ("toets", opt conditie kv_toets);
            bron kv_bron;
            ("bronRol", rol_binding rb) ])
  | c -> conditie_leaf c

and conditie_leaf (c : conditie) : json =
  let bron br = ("bron", so br) in
  match c with
  | Kenmerk_check { kenmerk; navigatie; negatie; eigenaar; periode; bron = br;
                    navigatie_binding } ->
      if periode <> None || navigatie_binding <> None then
        failwith "Emit: the slice writes no period or navigation on a characteristic check";
      node "Kenmerk"
        [ ("kenmerk", so kenmerk); ("navigatie", so navigatie); ("negatie", b negatie);
          ("eigenaar", so eigenaar); ("periode", `Null); bron br ]
  | Vergelijking { op; links; rechts; periode = _; bron = br } ->
      node "Vergelijking"
        [ ("op", s (vergelijk_op_s op)); ("links", opt expr links);
          ("rechts", opt expr rechts); ("periode", `Null); bron br ]
  | _ -> failwith "Emit: condition shape not produced by the F-logic slice"

let geldigheid (g : geldigheid) : json =
  node "Geldigheid" [ ("vanaf", so g.geld_vanaf); ("tot", so g.geld_tot) ]

let objecttype (o : objecttype) : json =
  let supertype = match o.ot_supertype with None -> [] | Some p -> [ ("supertype", s p) ] in
  `Assoc
    ([ ("node", `String "Objecttype"); ("naam", so o.ot_naam) ]
    @ supertype
    @ [ ("attributen", map_ (opt datatype) o.attributen);
        ("attribuutMeervoud", map_ so o.attribuut_meervoud);
        ("attribuutDimensies", map_ str_opt_list o.attribuut_dimensies);
        ("attribuutGranulariteit", map_ (fun _ -> `Null) o.attribuut_granulariteit);
        ("attribuutTijdlijnAantal", map_ io o.attribuut_tijdlijn_aantal);
        ("attribuutTijdlijnNaam", map_ so o.attribuut_tijdlijn_naam);
        ("kenmerken", str_opt_list o.kenmerken);
        ("kenmerkSoorten", map_ (opt (fun k -> s (kenmerk_soort_s k))) o.kenmerk_soorten);
        ("kenmerkGranulariteit", map_ (fun _ -> `Null) o.kenmerk_granulariteit);
        ("kenmerkTijdlijnAantal", map_ io o.kenmerk_tijdlijn_aantal);
        ("bezield", b o.bezield); ("meervoud", so o.meervoud) ])

let rol (r : rol) : json =
  node "Rol"
    [ ("naam", so r.rol_naam); ("objecttypeNaam", so r.rol_objecttype_naam);
      ("meervoud", so r.rol_meervoud); ("meervoudig", b r.rol_meervoudig) ]

let feittype (ft : feittype) : json =
  node "Feittype"
    [ ("naam", so ft.ft_naam); ("wederkerig", b ft.wederkerig);
      ("rollen", `List (List.map rol ft.rollen)) ]

let regel_stub (r : regel_stub) : json =
  node "Regel" [ ("naam", so r.rg_naam); ("soort", so r.rg_soort) ]

let resolved_regel (r : resolved_regel) : json =
  if r.rr_bron_annotaties <> None then
    failwith "Emit: the slice writes no statute annotations";
  node "ResolvedRegel"
    [ ("naam", so r.rr_naam);
      ("soort", opt (fun x -> s (regel_soort_s x)) r.rr_soort);
      ("doel", opt expr r.doel); ("waarde", opt expr r.waarde);
      ("voorwaarde", opt conditie r.rr_voorwaarde);
      ("geldigheid", opt geldigheid r.rr_geldigheid) ]

let default (d : default) : json =
  if d.default_initialisatie || d.default_excepties <> None then
    failwith "Emit: default form not produced by the F-logic slice";
  `Assoc
    [ ("node", `String "Default");
      ("besluit", (match d.default_besluit with None -> `Null | Some x -> `Bool x));
      ("voorwaarde", opt conditie d.default_voorwaarde);
      ("consequentie", opt expr d.default_consequentie);
      ("bron", so d.default_bron) ]

let prioritized_defaults (pd : prioritized_defaults) : json =
  if pd.pd_ongeordend then failwith "Emit: the slice declares no unordered group";
  `Assoc
    [ ("node", `String "PrioritizedDefaults"); ("naam", so pd.pd_naam);
      ("onderwerp", `String pd.pd_onderwerp); ("kenmerk", so pd.pd_kenmerk);
      ("attribuut", so pd.pd_attribuut);
      ("defaults", `List (List.map default pd.defaults));
      ("bron", so pd.pd_bron); ("geldigheid", opt geldigheid pd.pd_geldigheid) ]

let document (d : document) : json =
  `Assoc
    [ ("formaat", `String "regelspraak-ir-v0");
      ("objecttypen", map_ objecttype d.objecttypen);
      ("domeinen", `Assoc []);
      ("parameters", `Assoc []);
      ("eenheidstelsels", `Assoc []);
      ("feittypen", map_ feittype d.feittypen);
      ("dimensies", `Assoc []);
      ("tijdlijnen", `List []);
      ("dagsoorten", `List []);
      ("dagsoortPerEenheid", `Assoc []);
      ("regels", `List (List.map regel_stub d.regels));
      ("resolvedRegels", `List (List.map resolved_regel d.resolved_regels));
      ("resolvedDagsoorten", `List []);
      ("objectCreaties", `List []);
      ("verdelingen", `List []);
      ("consistenties", `List []);
      ("uniciteiten", `List []);
      ("uniciteitUnies", `List []);
      ("feitCreaties", `List []);
      ("resolvedStartpunten", `List []);
      ("regelGroepen", `List []);
      ("diagnostics", `List []);
      ("prioritizedDefaults", `List (List.map prioritized_defaults d.prioritized_defaults)) ]

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
