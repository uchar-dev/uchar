(* Serialiser: Ir_v0.Ast values -> uchar-ir-v1 wire JSON, in its L3 shape.

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

(* the built-in '%' of a percentage that names no unit of its own *)
let builtin_percent : eenheid =
  { afkorting = Some "%"; stelsel = None; canoniek = Some "%";
    factor_naar_canoniek = Some { num = 1; den = 1 }; noemer = None }

(* The type system in its L3 shape: a Number always carries its sign (a sign
   the in-memory form leaves open is ONBEPAALD) and its unit only when it has
   one; a Percentage is a Number with the unit '%' — the built-in one when the
   in-memory form carries none — and the presentation tag at a declaration
   position only ([~decl]; an expression's own type is an annotation, where
   the tag is never written); a date is one Date node with its precision. *)
let rec datatype ?(decl = true) (d : datatype) : json =
  match d with
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      node "Number"
        ([ ("whole", b geheel); ("decimals", i decimalen);
           ("sign", s (teken_s (Option.value teken ~default:Onbepaald))) ]
        @ match eenheid with None -> [] | Some e -> [ ("unit", unit_j e) ])
  | Booleaan -> node "Booleaan" []
  | Datum_in_dagen -> node "Date" [ ("precision", s "day") ]
  | Datum_tijd -> node "Date" [ ("precision", s "millisecond") ]
  | Tekst -> node "Tekst" []
  | Percentage { geheel; decimalen; eenheid } ->
      node "Number"
        ([ ("whole", b geheel); ("decimals", i decimalen); ("sign", s "ONBEPAALD");
           ("unit", unit_j (Option.value eenheid ~default:builtin_percent)) ]
        @ if decl then [ ("presentation", s "percentage") ] else [])
  | Enumeratie { domein; waarden } ->
      node "Enumeratie" [ ("domein", so domein); ("waarden", str_opt_list waarden) ]
  | Sum_type { domein; constructoren } ->
      node "SumType"
        [ ("domain", s domein);
          ("constructors",
           `List
             (List.map
                (fun (nm, payload) ->
                  `Assoc [ ("name", s nm); ("payload", opt (datatype ~decl) payload) ])
                constructoren)) ]
  | List_type { element } -> node "ListType" [ ("element", datatype ~decl element) ]
  | Struct_type { domein; velden } ->
      node "StructType"
        [ ("domain", s domein);
          ("fields",
           `List
             (List.map
                (fun (nm, dt) -> `Assoc [ ("name", s nm); ("datatype", datatype ~decl dt) ])
                velden)) ]

(* a Unit where it occurs: absent is the one spelling of a field not given,
   and the factor is the exact rational {num, den} *)
and unit_j (e : eenheid) : json =
  let abbreviation =
    match e.afkorting with Some a -> a | None -> failwith "Emit: a unit without an abbreviation"
  in
  node "Unit"
    ([ ("abbreviation", s abbreviation) ]
    @ (match e.stelsel with Some x -> [ ("system", s x) ] | None -> [])
    @ (match e.canoniek with Some x -> [ ("canonical", s x) ] | None -> [])
    @ (match e.factor_naar_canoniek with
      | Some { num; den } -> [ ("factorToCanonical", `Assoc [ ("num", i num); ("den", i den) ]) ]
      | None -> [])
    @ match e.noemer with Some n -> [ ("denominator", unit_j n) ] | None -> [])

(* an expression's own type: never a declaration position *)
let annotation (d : datatype) : json = datatype ~decl:false d

let geheel_periode (g : geheel_periode) : json =
  node "GeheelPeriode" [ ("jaar", b g.gp_jaar); ("niet", b g.gp_niet) ]

(* the symbolic role binding (IR-SPEC ch. 2.6): a whole-or-nothing pair of
   plain strings — the back-link of an object creation and the hop of a
   navigated read (Phase 5c subscope lowering). *)
let rol_binding (rb : rol_binding) : json =
  node "RolBinding" [ ("feittype", s rb.rb_feittype); ("rol", s rb.rb_rol) ]

let rec expr (e : expr) : json =
  let typ t = ("type", opt annotation t) in
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
  | Attribuut_ref { attribuut; objecttype; bron = br; navigatie_bindingen = nb } ->
      (* a reference writes no type (B13). navigatieBindingen is written only
         when the reference actually navigates (Phase 5c subscope lowering:
         the caller reads a callee output over the back-link); without it the
         read is the subject's own (D10) *)
      let ext =
        match nb with
        | None -> []
        | Some hops -> [ ("navigatieBindingen", `List (List.map rol_binding hops)) ]
      in
      node "AttribuutRef"
        ([ ("attribuut", so attribuut); ("objecttype", so objecttype); bron br ] @ ext)
  | Parameter_ref { parameter; bron = br } ->
      node "ParameterRef" [ ("parameter", so parameter); bron br ]
  | Variabele_ref { naam; bron = br } -> node "VariabeleRef" [ ("naam", so naam); bron br ]
  | Constante { naam; typ = t; bron = br } ->
      node "Constante" [ ("naam", so naam); typ t; bron br ]
  (* The translator builds no marker, and a written one would need a WARNING
     naming its bron in [diagnostics] (WF-113 (b)), which this serialiser
     never writes: it refuses like any node the skeleton never builds. *)
  | Expr_onopgelost _ -> failwith "Emit.expr: node not produced by the skeleton: Onopgelost"
  | Construct { typ = t; constructor_; argument; bron = br } ->
      node "Construct"
        [ ("type", opt annotation t); ("constructor", s constructor_);
          ("argument", opt expr argument); bron br ]
  | Match { typ = t; scrutinee; arms; bron = br } ->
      node "Match"
        [ ("type", opt annotation t); ("scrutinee", expr scrutinee);
          ("arms",
           `List
             (List.map
                (fun (c, binder, body) ->
                  `Assoc
                    [ ("constructor", s c); ("binder", so binder);
                      ("body", expr body) ])
                arms));
          bron br ]
  | Binder_ref { naam; bron = br } -> node "BinderRef" [ ("name", s naam); bron br ]
  | List_literal { typ = t; elements; bron = br } ->
      node "ListLiteral"
        [ ("type", opt annotation t); ("elements", `List (List.map expr elements)); bron br ]
  | If_then_else { typ = t; condition; then_; else_; bron = br } ->
      node "IfThenElse"
        [ ("type", opt annotation t); ("condition", conditie condition);
          ("then", expr then_); ("else", expr else_); bron br ]
  | Invoke { typ = t; functie; arguments; bron = br } ->
      node "Invoke"
        [ ("type", opt annotation t); ("function", s functie);
          ("arguments", `List (List.map expr arguments)); bron br ]
  | Fold { typ = t; list; functie; init; bron = br } ->
      node "Fold"
        [ ("type", opt annotation t); ("list", expr list);
          ("function", fold_fn functie); ("init", expr init); bron br ]
  | Map_list { typ = t; list; functie; bron = br } ->
      node "MapList"
        [ ("type", opt annotation t); ("list", expr list);
          ("function", fold_fn functie); bron br ]
  | Filter_list { typ = t; list; binder; condition; bron = br } ->
      node "FilterList"
        [ ("type", opt annotation t); ("list", expr list); ("binder", s binder);
          ("condition", conditie condition); bron br ]
  | List_length { typ = t; list; bron = br } ->
      node "ListLength" [ ("type", opt annotation t); ("list", expr list); bron br ]
  | Struct_literal { typ = t; velden; bron = br } ->
      node "StructLiteral"
        [ ("type", opt annotation t);
          ("fields",
           `List
             (List.map
                (fun (nm, v) -> `Assoc [ ("name", s nm); ("value", expr v) ])
                velden));
          bron br ]
  | Field_access { typ = t; base; veld; bron = br } ->
      node "FieldAccess"
        [ ("type", opt annotation t); ("base", expr base); ("field", s veld); bron br ]
  | Let { typ = t; binder; value; body; bron = br } ->
      (* the value-level binding (IR-SPEC ch. 4.10): [type] is non-null on the wire and
         equals the body type (WF-076); [binder] joins the WF-064 regime *)
      node "Let"
        [ ("type", opt annotation t); ("binder", s binder);
          ("value", expr value); ("body", expr body); bron br ]
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
      { kenmerk; negatie; eigenaar; periode = p; bron = br; navigatie_binding = nb } ->
      (* navigatieBinding is written whole or not at all (schema: never null),
         only when the check actually navigates (ch. 4.1a/5: a caller reads a
         callee characteristic over the synthesised back-link — the condition
         sibling of a navigated AttribuutRef); it is the hop (D53) *)
      let ext =
        match nb with None -> [] | Some rb -> [ ("navigatieBinding", rol_binding rb) ]
      in
      node "Kenmerk"
        ([ ("kenmerk", so kenmerk); ("negatie", b negatie); ("eigenaar", so eigenaar);
           periode p; bron br ]
        @ ext)
  (* no marker either: see the expression arm *)
  | Conditie_onopgelost _ ->
      failwith "Emit.conditie: condition shape not produced by the skeleton: Onopgelost"
  | _ -> failwith "Emit.conditie: condition shape not produced by the skeleton"

let geldigheid (g : geldigheid) : json =
  node "Geldigheid" [ ("vanaf", so g.geld_vanaf); ("tot", so g.geld_tot) ]

(* A list entry of the M1 model carries its name once, on its node, so the
   in-memory map key and the node's own name must be one spelling; two
   spellings have no rule for choosing, so a mismatch refuses. *)
let named what key (naam : string option) =
  if naam <> Some key then
    failwith
      (Printf.sprintf "Emit: %s %S carries the name %s; the uchar-ir-v1 list entry has one" what
         key (match naam with None -> "null" | Some n -> Printf.sprintf "%S" n))

(* ObjectType with its members as nodes (IR-SPEC ch. 2.2). The translator
   fills no member facet: no plural, dimension or timeline table. A filled
   one refuses rather than being written in a guessed form, which also
   retires the old constant-null granularity branch. *)
let objecttype (key, (o : objecttype)) : json =
  named "object type" key o.ot_naam;
  let unfilled table = function
    | [] -> ()
    | _ -> failwith (Printf.sprintf "Emit: object type %S fills %s, which the plugin never builds" key table)
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

(* Domain (IR-SPEC ch. 2.3): the name is written here, so the datatype at
   this declaration position drops the self-name it carries at every
   reference position. *)
let domain (key, (d : datatype option)) : json =
  let dt =
    match d with
    | Some dt -> dt
    | None -> failwith (Printf.sprintf "Emit: domain %S has no datatype" key)
  in
  let self_name =
    match dt with
    | Enumeratie { domein; _ } -> Some ("domein", domein)
    | Sum_type { domein; _ } | Struct_type { domein; _ } -> Some ("domain", Some domein)
    | _ -> None
  in
  let body =
    match (self_name, datatype dt) with
    | None, j -> j
    | Some (field, naam), `Assoc kvs ->
        named "domain" key naam;
        `Assoc (List.filter (fun (k, _) -> k <> field) kvs)
    | Some _, _ -> failwith (Printf.sprintf "Emit: domain %S did not serialise as an object" key)
  in
  node "Domain" [ ("name", s key); ("datatype", body) ]

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

(* fact type + roles (IR-SPEC ch. 2.6): the connecting relation the plugin
   synthesises to carry a subscope back-link (Phase 5c). Each role names its
   filler object type; the plugin's roles are always to-one. *)
let rol (r : rol) : json =
  node "Rol"
    [ ("naam", so r.rol_naam); ("objecttypeNaam", so r.rol_objecttype_naam);
      ("meervoud", so r.rol_meervoud); ("meervoudig", b r.rol_meervoudig) ]

let feittype (ft : feittype) : json =
  node "Feittype"
    [ ("naam", so ft.ft_naam); ("wederkerig", b ft.wederkerig);
      ("rollen", `List (List.map rol ft.rollen)) ]

let toekenning (t : toekenning) : json =
  node "Toekenning"
    [ ("attribuut", so t.toe_attribuut); ("waarde", opt expr t.toe_waarde);
      ("kenmerk", b t.toe_kenmerk) ]

let resolved_object_creatie (oc : resolved_object_creatie) : json =
  (* verbindendeRol is an extension field (the loader admits it via [ext]) —
     the created-side role of the back-link, written whole when present *)
  let vrb =
    match oc.oc_verbindende_rol with
    | None -> []
    | Some rb -> [ ("verbindendeRol", rol_binding rb) ]
  in
  `Assoc
    ([ ("node", `String "ResolvedObjectCreatie");
       ("naam", so oc.oc_naam); ("nieuwType", so oc.nieuw_type);
       ("subject", so oc.oc_subject);
       ("toekenningen", `List (List.map toekenning oc.toekenningen));
       ("voorwaarde", opt conditie oc.oc_voorwaarde);
       ("geldigheid", opt geldigheid oc.oc_geldigheid) ]
    @ vrb)

let rec default (d : default) : json =
  (* [besluit] is a required key (null on an attribute / group default);
     [consequentie], [initialisatie] and [excepties] are extension fields —
     written only when present, matching the loader's [ext] admission. *)
  let base =
    [ ("node", `String "Default");
      ("besluit", (match d.default_besluit with None -> `Null | Some x -> `Bool x));
      ("voorwaarde", opt conditie d.default_voorwaarde) ]
  in
  let conseq =
    match d.default_consequentie with None -> [] | Some e -> [ ("consequentie", expr e) ]
  in
  let init = if d.default_initialisatie then [ ("initialisatie", `Bool true) ] else [] in
  let excepties =
    match d.default_excepties with
    | None -> [] | Some egs -> [ ("excepties", exception_groups egs) ]
  in
  `Assoc (base @ conseq @ init @ [ ("bron", so d.default_bron) ] @ excepties)

(* the nested exception group riding [excepties] (v1, Phase 5c): the
   {ongeordend, leden} object (no node discriminator) and its [ExceptionGroup]
   members, mirroring the loader's [exception_groups]/[exception_group]. *)
and exception_groups (egs : exception_groups) : json =
  `Assoc
    [ ("ongeordend", b egs.egs_ongeordend);
      ("leden", `List (List.map exception_group egs.egs_leden)) ]

and exception_group (eg : exception_group) : json =
  node "ExceptionGroup"
    [ ("defaults", `List (List.map default eg.eg_defaults)); ("bron", so eg.eg_bron) ]

let prioritized_defaults (pd : prioritized_defaults) : json =
  let attribuut = match pd.pd_attribuut with None -> [] | Some a -> [ ("attribuut", `String a) ] in
  let ongeordend = if pd.pd_ongeordend then [ ("ongeordend", `Bool true) ] else [] in
  `Assoc
    ([ ("node", `String "PrioritizedDefaults"); ("naam", so pd.pd_naam);
       ("onderwerp", `String pd.pd_onderwerp); ("kenmerk", so pd.pd_kenmerk) ]
    @ attribuut
    @ [ ("defaults", `List (List.map default pd.defaults)) ]
    @ ongeordend
    @ [ ("bron", so pd.pd_bron); ("geldigheid", opt geldigheid pd.pd_geldigheid) ])

let function_def (fd : function_def) : json =
  node "FunctionDef"
    [ ("name", s fd.fd_naam); ("parameters", param_list fd.fd_parameters);
      ("result", datatype fd.fd_result); ("body", expr fd.fd_body);
      ("bron", so fd.fd_bron) ]

(* The M1 container (decision V3 (b)): the declaration set under "model", the
   rule lists under "rules" with their v0 keys, in the schema's property
   order. functionDefs is an extension list, written only when non-empty;
   prioritizedDefaults is written whole, as it always was. The plugin
   builds no parameter, unit system, dimension, timeline, day kind or
   diagnostic, so those lists are written empty, and a filled one refuses:
   written as [] it would drop what it held without a word. The serialiser
   refuses an Onopgelost as well, so no written document holds a marker a
   diagnostic would have to account for (WF-113). *)
let document (d : document) : json =
  let unfilled list = function
    | [] -> ()
    | _ -> failwith (Printf.sprintf "Emit: the document fills %s, which the plugin never builds" list)
  in
  unfilled "parameters" d.parameters;
  unfilled "unitSystems" d.eenheidstelsels;
  unfilled "dimensions" d.dimensies;
  unfilled "timelines" d.tijdlijnen;
  unfilled "dayKinds" d.dagsoorten;
  unfilled "resolvedDagsoorten" d.resolved_dagsoorten;
  unfilled "verdelingen" d.verdelingen;
  unfilled "uniciteiten" d.uniciteiten;
  unfilled "uniciteitUnies" d.uniciteit_unies;
  unfilled "feitCreaties" d.feit_creaties;
  unfilled "resolvedStartpunten" d.resolved_startpunten;
  unfilled "regelGroepen" d.regel_groepen;
  unfilled "diagnostics" d.diagnostics;
  let function_defs =
    match d.function_defs with [] -> [] | fds -> [ ("functionDefs", `List (List.map function_def fds)) ]
  in
  `Assoc
    [ ("format", `String "uchar-ir-v1");
      ("model",
       `Assoc
         ([ ("objectTypes", `List (List.map objecttype d.objecttypen));
            ("factTypes",
             `List
               (List.map
                  (fun (key, (ft : feittype)) ->
                    named "fact type" key ft.ft_naam;
                    feittype ft)
                  d.feittypen));
            ("parameters", `List []);
            ("domains", `List (List.map domain d.domeinen));
            ("unitSystems", `List []);
            ("dimensions", `List []);
            ("timelines", `List []);
            ("dayKinds", `List []) ]
         @ function_defs));
      ("rules",
       `Assoc
         [ ("resolvedRegels", `List (List.map resolved_regel d.resolved_regels));
           ("resolvedDagsoorten", `List []);
           ("objectCreaties", `List (List.map resolved_object_creatie d.object_creaties));
           ("verdelingen", `List []);
           ("consistenties", `List (List.map resolved_consistentie d.consistenties));
           ("uniciteiten", `List []);
           ("uniciteitUnies", `List []);
           ("feitCreaties", `List []);
           ("resolvedStartpunten", `List []);
           ("regelGroepen", `List []);
           ("prioritizedDefaults", `List (List.map prioritized_defaults d.prioritized_defaults)) ]);
      ("diagnostics", `List []) ]

let to_string (d : document) : string = Yojson.Safe.pretty_to_string (document d) ^ "\n"

let to_file (path : string) (d : document) : unit =
  let oc = open_out path in
  output_string oc (to_string d);
  close_out oc
