(* Serialiser: Ir_v0.Ast values -> uchar-ir-v1 wire JSON, in its L3 shape.

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

(* The type system in its L3 shape: a Number always carries its sign (a sign
   the in-memory form leaves open is ONBEPAALD) and, since the slice declares
   no units, no unit; a day date is a Date at day precision. *)
let datatype (d : datatype) : json =
  match d with
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      if eenheid <> None then failwith "Emit: the slice declares no units";
      node "Number"
        [ ("whole", b geheel); ("decimals", i decimalen);
          ("sign", s (teken_s (Option.value teken ~default:Onbepaald))) ]
  | Booleaan -> node "Booleaan" []
  | Tekst -> node "Tekst" []
  | Datum_in_dagen -> node "Date" [ ("precision", s "day") ]
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
  | Attribuut_ref { attribuut; objecttype; bron = br; navigatie_bindingen } ->
      if navigatie_bindingen <> None then
        failwith "Emit: the slice synthesises no navigation bindings";
      node "AttribuutRef" [ ("attribuut", so attribuut); ("objecttype", so objecttype); bron br ]
  | Functie
      { kind; argumenten; element_objecttype; leegwaarde; filter; dimensie_selectie;
        typ = t; bron = br; granulariteit; bron_rol; bron_pad } ->
      if filter <> None || dimensie_selectie <> None || granulariteit <> None
         || bron_pad <> None then
        failwith "Emit: aggregation qualifier not produced by the F-logic slice";
      (* the slice's two folds read linked instances, so the range is written
         whole on every one of them and never left to be derived (B12) *)
      let rb =
        match bron_rol with
        | Some rb -> rb
        | None -> failwith "Emit: an instance fold without its role binding"
      in
      node "Functie"
        [ ("kind", s (functie_kind_s kind));
          ("argumenten", `List (List.map expr argumenten));
          ("elementObjecttype", so element_objecttype);
          ("leegwaarde", opt expr leegwaarde);
          ("filter", `Null); ("dimensieSelectie", `Null); typ t; bron br;
          ("granulariteit", `Null); ("bronRol", rol_binding rb) ]
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
  | Kenmerk_check { kenmerk; negatie; eigenaar; periode; bron = br; navigatie_binding } ->
      if periode <> None || navigatie_binding <> None then
        failwith "Emit: the slice writes no period or navigation on a characteristic check";
      node "Kenmerk"
        [ ("kenmerk", so kenmerk); ("negatie", b negatie); ("eigenaar", so eigenaar);
          ("periode", `Null); bron br ]
  | Vergelijking { op; links; rechts; periode = _; bron = br } ->
      node "Vergelijking"
        [ ("op", s (vergelijk_op_s op)); ("links", opt expr links);
          ("rechts", opt expr rechts); ("periode", `Null); bron br ]
  | _ -> failwith "Emit: condition shape not produced by the F-logic slice"

let geldigheid (g : geldigheid) : json =
  node "Geldigheid" [ ("vanaf", so g.geld_vanaf); ("tot", so g.geld_tot) ]

(* A list entry of the M1 model carries its name once, on its node, so the
   in-memory map key and the node's own name must be one spelling. *)
let named what key (naam : string option) =
  if naam <> Some key then
    failwith
      (Printf.sprintf "Emit: %s %S carries the name %s; the uchar-ir-v1 list entry has one" what
         key (match naam with None -> "null" | Some n -> Printf.sprintf "%S" n))

(* ObjectType with its members as nodes (IR-SPEC ch. 2.2). The slice
   declares no plural, dimension or timeline, so a filled member-facet
   table refuses rather than being written in a guessed form. *)
let objecttype (key, (o : objecttype)) : json =
  named "object type" key o.ot_naam;
  let unfilled table = function
    | [] -> ()
    | _ -> failwith (Printf.sprintf "Emit: object type %S fills %s, which the slice never builds" key table)
  in
  unfilled "attribuutMeervoud" o.attribuut_meervoud;
  unfilled "attribuutDimensies" o.attribuut_dimensies;
  unfilled "attribuutGranulariteit" o.attribuut_granulariteit;
  unfilled "attribuutTijdlijnAantal" o.attribuut_tijdlijn_aantal;
  unfilled "attribuutTijdlijnNaam" o.attribuut_tijdlijn_naam;
  unfilled "kenmerkGranulariteit" o.kenmerk_granulariteit;
  unfilled "kenmerkTijdlijnAantal" o.kenmerk_tijdlijn_aantal;
  let kenmerken =
    List.map
      (function
        | Some k -> k
        | None -> failwith (Printf.sprintf "Emit: object type %S declares a null characteristic" key))
      o.kenmerken
  in
  List.iter
    (fun (k, _) ->
      if not (List.mem k kenmerken) then
        failwith (Printf.sprintf "Emit: object type %S tags %S, which it does not declare" key k))
    o.kenmerk_soorten;
  let supertype = match o.ot_supertype with None -> [] | Some p -> [ ("supertype", s p) ] in
  let plural = match o.meervoud with None -> [] | Some p -> [ ("plural", s p) ] in
  node "ObjectType"
    ([ ("name", s key) ] @ supertype @ [ ("animate", b o.bezield) ] @ plural
    @ [ ("attributes",
         `List
           (List.map
              (fun (a, dt) -> node "Attribute" [ ("name", s a); ("datatype", opt datatype dt) ])
              o.attributen));
        ("characteristics",
         `List
           (List.map
              (fun k ->
                (* untagged is nominal (ch. 2.2) *)
                let kind =
                  match List.assoc_opt k o.kenmerk_soorten with
                  | Some (Some soort) -> soort
                  | Some None | None -> Zelfstandig
                in
                node "Characteristic" [ ("name", s k); ("kind", s (kenmerk_soort_s kind)) ])
              kenmerken)) ])

let rol (r : rol) : json =
  node "Rol"
    [ ("naam", so r.rol_naam); ("objecttypeNaam", so r.rol_objecttype_naam);
      ("meervoud", so r.rol_meervoud); ("meervoudig", b r.rol_meervoudig) ]

let feittype (ft : feittype) : json =
  node "Feittype"
    [ ("naam", so ft.ft_naam); ("wederkerig", b ft.wederkerig);
      ("rollen", `List (List.map rol ft.rollen)) ]

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

(* The M1 container (decision V3 (b)): the declaration set under "model", the
   rule lists under "rules" with their v0 keys, in the schema's property
   order; prioritizedDefaults is written whole, as it always was. The slice
   builds no domain, parameter, unit system, dimension, timeline, day kind,
   function definition, other rule list or diagnostic, so those lists are
   written empty or, for functionDefs, not at all, and a filled one refuses:
   written as [] it would drop what it held without a word. Its serialiser
   has no Onopgelost arm, so no written document holds a marker a diagnostic
   would have to account for (WF-113). *)
let document (d : document) : json =
  let unfilled list = function
    | [] -> ()
    | _ -> failwith (Printf.sprintf "Emit: the document fills %s, which the slice never builds" list)
  in
  unfilled "parameters" d.parameters;
  unfilled "domains" d.domeinen;
  unfilled "unitSystems" d.eenheidstelsels;
  unfilled "dimensions" d.dimensies;
  unfilled "timelines" d.tijdlijnen;
  unfilled "dayKinds" d.dagsoorten;
  unfilled "functionDefs" d.function_defs;
  unfilled "resolvedDagsoorten" d.resolved_dagsoorten;
  unfilled "objectCreaties" d.object_creaties;
  unfilled "verdelingen" d.verdelingen;
  unfilled "consistenties" d.consistenties;
  unfilled "uniciteiten" d.uniciteiten;
  unfilled "uniciteitUnies" d.uniciteit_unies;
  unfilled "feitCreaties" d.feit_creaties;
  unfilled "resolvedStartpunten" d.resolved_startpunten;
  unfilled "regelGroepen" d.regel_groepen;
  unfilled "diagnostics" d.diagnostics;
  `Assoc
    [ ("format", `String "uchar-ir-v1");
      ("model",
       `Assoc
         [ ("objectTypes", `List (List.map objecttype d.objecttypen));
           ("factTypes",
            `List
              (List.map
                 (fun (key, (ft : feittype)) ->
                   named "fact type" key ft.ft_naam;
                   feittype ft)
                 d.feittypen));
           ("parameters", `List []);
           ("domains", `List []);
           ("unitSystems", `List []);
           ("dimensions", `List []);
           ("timelines", `List []);
           ("dayKinds", `List []) ]);
      ("rules",
       `Assoc
         [ ("resolvedRegels", `List (List.map resolved_regel d.resolved_regels));
           ("resolvedDagsoorten", `List []);
           ("objectCreaties", `List []);
           ("verdelingen", `List []);
           ("consistenties", `List []);
           ("uniciteiten", `List []);
           ("uniciteitUnies", `List []);
           ("feitCreaties", `List []);
           ("resolvedStartpunten", `List []);
           ("regelGroepen", `List []);
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
