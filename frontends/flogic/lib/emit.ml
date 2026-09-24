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
  | (Som | Aantal) as k -> enum Spelling.aggregation_kind k
  | _ -> failwith "Emit: aggregation kind not produced by the F-logic slice"

(* The type system in its L5 shape: a Number always carries its sign (a sign
   the in-memory form leaves open is unspecified) and, since the slice
   declares no units, no unit; a day date is a Date at day precision. *)
let datatype (d : datatype) : json =
  match d with
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      if eenheid <> None then failwith "Emit: the slice declares no units";
      node "Number"
        [ ("whole", b geheel); ("decimals", i decimalen);
          ("sign", enum Spelling.sign (Option.value teken ~default:Onbepaald)) ]
  | Booleaan -> node "Boolean" []
  | Tekst -> node "Text" []
  | Datum_in_dagen -> node "Date" [ ("precision", enum Spelling.precision d) ]
  | _ -> failwith "Emit: datatype not produced by the F-logic slice"

let rol_binding (rb : rol_binding) : json =
  node "RoleBinding" [ ("factType", s rb.rb_feittype); ("role", s rb.rb_rol) ]

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
  | Literal { variant; typ = t; bron = br } ->
      node "Literal" [ ("value", literal_value variant); typ t; source br ]
  | Binair { op; links; rechts; typ = t; bron = br } ->
      node "Binary"
        [ ("op", enum Spelling.binary_op op); ("left", opt expr links);
          ("right", opt expr rechts); typ t; source br ]
  | Attribuut_ref { attribuut; objecttype; bron = br; navigatie_bindingen } ->
      if navigatie_bindingen <> None then
        failwith "Emit: the slice synthesises no navigation path";
      node "AttributeReference"
        [ ("attribute", so attribuut); ("subjectType", so objecttype); source br ]
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
      (* an absent optional field is left out (IR-SPEC ch. 4.3) *)
      node "Aggregation"
        ([ ("kind", aggregation_kind kind);
           ("arguments", `List (List.map expr argumenten)) ]
        @ (match element_objecttype with None -> [] | Some el -> [ ("elementType", s el) ])
        @ (match leegwaarde with None -> [] | Some f -> [ ("emptyFallback", expr f) ])
        @ [ ("sourceRole", rol_binding rb); typ t; source br ])
  | _ -> failwith "Emit: expression node not produced by the F-logic slice"

let rec conditie (c : conditie) : json =
  match c with
  | Vergelijking { op; links; rechts; periode; bron = br } ->
      if periode <> None then failwith "Emit: the slice writes no period qualifier";
      node "Comparison"
        [ ("op", enum Spelling.comparison_op op); ("left", opt expr links);
          ("right", opt expr rechts); source br ]
  | Samengesteld { kwantor; drempel; onderdelen; bron = br } ->
      node "Compound"
        [ ("quantifier", enum Spelling.quantifier kwantor); ("threshold", i drempel);
          ("parts", `List (List.map (fun c ->
             match c with
             | Vergelijking _ | Kenmerk_check _ -> conditie_leaf c
             (* a fold beside an ordinary condition of the subject: the two
                conjoin, and the fold keeps its own shape inside *)
             | Kwantorvouw _ -> conditie c
             | _ -> failwith "Emit: nested composite not produced by the F-logic slice")
             onderdelen));
          source br ]
  (* The quantifier fold. [threshold] is present EXACTLY under the three
     counting quantifiers (WF-108 (e)) and absent otherwise, so it is
     written conditionally rather than as a null nobody may consult; the
     subselection is absent where there is none; the range is always the
     one-step [sourceRole], never [sourcePath]. *)
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
      node "QuantifierFold"
        ([ ("quantifier", enum Spelling.quantifier kv_kwantor) ]
        @ optional "threshold" i kv_drempel
        @ [ ("elementType", so kv_element_objecttype) ]
        @ optional "filter" conditie kv_filter
        @ [ ("test", opt conditie kv_toets); source kv_bron; ("sourceRole", rol_binding rb) ])
  | c -> conditie_leaf c

and conditie_leaf (c : conditie) : json =
  match c with
  | Kenmerk_check { kenmerk; negatie; eigenaar; periode; bron = br; navigatie_binding } ->
      if periode <> None || navigatie_binding <> None then
        failwith "Emit: the slice writes no period or navigation on a characteristic check";
      node "CharacteristicCheck"
        ([ ("characteristic", so kenmerk); ("negated", b negatie) ]
        @ optional "host" s eigenaar
        @ [ source br ])
  | Vergelijking { op; links; rechts; periode; bron = br } ->
      if periode <> None then failwith "Emit: the slice writes no period qualifier";
      node "Comparison"
        [ ("op", enum Spelling.comparison_op op); ("left", opt expr links);
          ("right", opt expr rechts); source br ]
  | _ -> failwith "Emit: condition shape not produced by the F-logic slice"

(* an open side of a validity is an absent bound; neither bound is always *)
let geldigheid (g : geldigheid) : json =
  node "Validity" (optional "from" s g.geld_vanaf @ optional "to" s g.geld_tot)

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
  unfilled "attribuut_meervoud" o.attribuut_meervoud;
  unfilled "attribuut_dimensies" o.attribuut_dimensies;
  unfilled "attribuut_granulariteit" o.attribuut_granulariteit;
  unfilled "attribuut_tijdlijn_aantal" o.attribuut_tijdlijn_aantal;
  unfilled "attribuut_tijdlijn_naam" o.attribuut_tijdlijn_naam;
  unfilled "kenmerk_granulariteit" o.kenmerk_granulariteit;
  unfilled "kenmerk_tijdlijn_aantal" o.kenmerk_tijdlijn_aantal;
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
                node "Characteristic" [ ("name", s k); ("kind", enum Spelling.characteristic_kind kind) ])
              kenmerken)) ])

let rol (r : rol) : json =
  node "Role"
    ([ ("name", so r.rol_naam); ("objectType", so r.rol_objecttype_naam) ]
    @ optional "plural" s r.rol_meervoud
    @ [ ("many", b r.rol_meervoudig) ])

let feittype (ft : feittype) : json =
  node "FactType"
    [ ("name", so ft.ft_naam); ("reciprocal", b ft.wederkerig);
      ("roles", `List (List.map rol ft.rollen)) ]

(* A rule (IR-SPEC ch. 3, Versioning; ch. 3.1): {node, name, versions}. The
   slice builds every rule with one version, so each in-memory record is one
   form; a record name ending in a space, '#' and digits is the in-memory
   spelling of one version of a form with several, which the slice never
   builds, and it refuses rather than writing a name no reader loads. The
   version's kind is read back onto the initialisation flag and the
   presentation tag, its validity is always written, and its target is an
   attribute reference. *)
let rule (r : resolved_regel) : json =
  if r.rr_bron_annotaties <> None then
    failwith "Emit: the slice writes no statute annotations";
  (match r.rr_naam with
  | Some n when Ir_v0.Loader.versioned_name n ->
      failwith
        (Printf.sprintf
           "Emit: the rule name %S ends in a space, '#' and digits, the in-memory spelling of one \
            version of a form with several, and the slice builds no versions"
           n)
  | _ -> ());
  let initialisation, presentation =
    match r.rr_soort with
    | Some ((Gelijkstelling | Berekening) as k) -> (false, [ ("presentation", enum Spelling.presentation k) ])
    | Some Initialisatie -> (true, [])
    | Some Kenmerktoekenning -> (false, [])
    | None -> failwith "Emit: a rule version without its kind"
  in
  let target =
    match r.doel with
    | Some (Attribuut_ref _ as t) -> expr t
    | _ -> failwith "Emit: the target of a rule version is an AttributeReference"
  in
  let value =
    match r.waarde with Some v -> expr v | None -> failwith "Emit: a rule version without its value"
  in
  let condition = match r.rr_voorwaarde with None -> [] | Some c -> [ ("condition", conditie c) ] in
  let validity =
    geldigheid (Option.value r.rr_geldigheid ~default:{ geld_vanaf = None; geld_tot = None })
  in
  node "Rule"
    [ ("name", so r.rr_naam);
      ("versions",
       `List
         [ `Assoc
             ([ ("validity", validity); ("target", target); ("value", value) ]
             @ condition
             @ [ ("initialisation", b initialisation) ]
             @ presentation) ]) ]

let default (d : default) : json =
  if d.default_initialisatie || d.default_excepties <> None then
    failwith "Emit: default form not produced by the F-logic slice";
  `Assoc
    (fields
       ([ ("node", `String "Default") ]
       @ optional "decision" b d.default_besluit
       @ optional "justification" conditie d.default_voorwaarde
       @ optional "consequence" expr d.default_consequentie
       @ [ source d.default_bron ]))

let prioritized_defaults (pd : prioritized_defaults) : json =
  if pd.pd_ongeordend then failwith "Emit: the slice declares no unordered group";
  `Assoc
    (fields
       ([ ("node", `String "PrioritizedDefaults"); ("name", so pd.pd_naam);
          ("subject", `String pd.pd_onderwerp) ]
       @ optional "characteristic" s pd.pd_kenmerk
       @ optional "attribute" s pd.pd_attribuut
       @ [ ("defaults", `List (List.map default pd.defaults));
           source pd.pd_bron; ("validity", opt geldigheid pd.pd_geldigheid) ]))

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
  unfilled "domains" d.domeinen;
  unfilled "unitSystems" d.eenheidstelsels;
  unfilled "dimensions" d.dimensies;
  unfilled "timelines" d.tijdlijnen;
  unfilled "dayKinds" d.dagsoorten;
  unfilled "functionDefs" d.function_defs;
  unfilled "dayKindDefinitions" d.resolved_dagsoorten;
  unfilled "objectCreations" d.object_creaties;
  unfilled "allocations" d.verdelingen;
  unfilled "consistencies" d.consistenties;
  unfilled "uniquenesses" d.uniciteiten;
  unfilled "uniquenesses" d.uniciteit_unies;
  unfilled "factCreations" d.feit_creaties;
  unfilled "startPointDeterminations" d.resolved_startpunten;
  unfilled "ruleGroups" d.regel_groepen;
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
         [ ("rules", `List (List.map rule d.resolved_regels));
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
