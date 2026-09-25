(* Serialiser: Ir_v0.Ast values -> uchar-ir-v1 wire JSON, in its L5 shape.

   ARCHITECTURE. The plugin builds typed [Ir_v0.Ast] values and hands
   them to exactly one serialiser — this module — so schema conformity is
   structural, not stringly-typed: the mapping code never assembles JSON
   by hand. This module is the deliberate inverse of
   [Ir_v0.Loader]: every key name and node discriminator mirrors the
   loader's [check_keys]/[expect_node] tables, and every enumeration value
   is spelled by the one translation table the loader reads with
   ([Ir_v0.Spelling]), so a value that round-trips through Loader is what
   we emit.

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
   The plugin writes no line (the consistency's in-memory line is the order of
   its assertion in the scope, not a line of a source file). *)
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

(* the built-in '%' of a percentage that names no unit of its own *)
let builtin_percent : eenheid =
  { afkorting = Some "%"; stelsel = None; canoniek = Some "%";
    factor_naar_canoniek = Some { num = 1; den = 1 }; noemer = None }

(* The type system in its L5 shape: a Number always carries its sign (a sign
   the in-memory form leaves open is unspecified) and its unit only when it has
   one; a Percentage is a Number with the unit '%' — the built-in one when the
   in-memory form carries none — and the presentation tag at a declaration
   position only ([~decl]; an expression's own type is an annotation, where
   the tag is never written); a date is one Date node with its precision. *)
let rec datatype ?(decl = true) (d : datatype) : json =
  match d with
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      node "Number"
        ([ ("whole", b geheel); ("decimals", i decimalen);
           ("sign", enum Spelling.sign (Option.value teken ~default:Onbepaald)) ]
        @ match eenheid with None -> [] | Some e -> [ ("unit", unit_j e) ])
  | Booleaan -> node "Boolean" []
  | Datum_in_dagen | Datum_tijd -> node "Date" [ ("precision", enum Spelling.precision d) ]
  | Tekst -> node "Text" []
  | Percentage { geheel; decimalen; eenheid } ->
      node "Number"
        ([ ("whole", b geheel); ("decimals", i decimalen); ("sign", enum Spelling.sign Onbepaald);
           ("unit", unit_j (Option.value eenheid ~default:builtin_percent)) ]
        @ if decl then [ ("presentation", s "percentage") ] else [])
  | Enumeratie { domein; waarden } ->
      node "Enumeration" [ ("domain", so domein); ("values", str_opt_list waarden) ]
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

(* the symbolic role binding (IR-SPEC ch. 2.6): a whole-or-nothing pair of
   plain strings — the back-link of an object creation and the hop of a
   navigated read (Phase 5c subscope lowering). *)
let rol_binding (rb : rol_binding) : json =
  node "RoleBinding" [ ("factType", s rb.rb_feittype); ("role", s rb.rb_rol) ]

(* The typed value of a Literal (IR-SPEC ch. 4.1, delta B22): one key, the
   variant the literal's type names. *)
let literal_value (v : literal_variant) : json =
  match v with
  | Lv_number x -> `Assoc [ ("number", s x) ]
  | Lv_boolean x -> `Assoc [ ("boolean", b x) ]
  | Lv_text x -> `Assoc [ ("text", s x) ]
  | Lv_date x -> `Assoc [ ("date", s x) ]
  | Lv_date_time x -> `Assoc [ ("dateTime", s x) ]
  | Lv_enumeration x -> `Assoc [ ("enumeration", s x) ]
  | Lv_constructor x -> `Assoc [ ("constructor", s x) ]

let rec expr (e : expr) : json =
  let typ t = ("type", opt annotation t) in
  match e with
  | Literal { variant; typ = t; bron = br } ->
      node "Literal" [ ("value", literal_value variant); typ t; source br ]
  | Binair { op; links; rechts; typ = t; bron = br } ->
      node "Binary"
        [ ("op", enum Spelling.binary_op op); ("left", opt expr links);
          ("right", opt expr rechts); typ t; source br ]
  | Afgerond { basis; modus; decimalen; typ = t; bron = br } ->
      node "Rounding"
        [ ("base", opt expr basis); ("mode", enum Spelling.rounding_mode modus);
          ("decimals", i decimalen); typ t; source br ]
  | Attribuut_ref { attribuut; objecttype; bron = br; navigatie_bindingen = nb } ->
      (* a reference writes no type (B13). path is written only when the
         reference actually navigates (Phase 5c subscope lowering: the caller
         reads a callee output over the back-link); without it the read is the
         subject's own (IR-SPEC ch. 4.1a) *)
      node "AttributeReference"
        ([ ("attribute", so attribuut); ("subjectType", so objecttype); source br ]
        @ optional "path" (fun hops -> `List (List.map rol_binding hops)) nb)
  (* the two context nodes (IR-SPEC ch. 4.1, delta B23): a session value
     carries its name and its datatype, the bound day of a day-kind
     definition its datatype. A name outside the closed set is refused in
     WF-098's sentence rather than written for the verifier to reject *)
  | Constante { naam; typ = t; bron = br } ->
      let name =
        match naam with
        | Some n -> (
            match Ir_v0.Checks.context_value_refusal n with
            | None -> n
            | Some sentence -> failwith ("Emit.expr: " ^ sentence))
        | None -> failwith "Emit.expr: a ContextValue carries its name as a string"
      in
      let t =
        match t with Some t -> t | None -> failwith "Emit.expr: a ContextValue carries its datatype"
      in
      node "ContextValue" [ ("name", s name); ("type", annotation t); source br ]
  | Bound_day { typ = t; bron = br } ->
      let t =
        match t with Some t -> t | None -> failwith "Emit.expr: a BoundDay carries its datatype"
      in
      node "BoundDay" [ ("type", annotation t); source br ]
  (* The translator builds no marker, and a written one would need a warning
     quoting its source snippet in [diagnostics] (WF-113 (b)), which this
     serialiser never writes: it refuses like any node the skeleton never
     builds. *)
  | Expr_onopgelost _ -> failwith "Emit.expr: node not produced by the skeleton: Unresolved"
  | Construct { typ = t; constructor_; argument; bron = br } ->
      node "Construct"
        [ ("type", opt annotation t); ("constructor", s constructor_);
          ("argument", opt expr argument); source br ]
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
          source br ]
  | Binder_ref { naam; bron = br } -> node "BinderRef" [ ("name", s naam); source br ]
  | List_literal { typ = t; elements; bron = br } ->
      node "ListLiteral"
        [ ("type", opt annotation t); ("elements", `List (List.map expr elements)); source br ]
  | If_then_else { typ = t; condition; then_; else_; bron = br } ->
      node "IfThenElse"
        [ ("type", opt annotation t); ("condition", conditie condition);
          ("then", expr then_); ("else", expr else_); source br ]
  | Invoke { typ = t; functie; arguments; bron = br } ->
      node "Invoke"
        [ ("type", opt annotation t); ("function", s functie);
          ("arguments", `List (List.map expr arguments)); source br ]
  | Fold { typ = t; list; functie; init; bron = br } ->
      node "Fold"
        [ ("type", opt annotation t); ("list", expr list);
          ("function", fold_fn functie); ("init", expr init); source br ]
  | Map_list { typ = t; list; functie; bron = br } ->
      node "MapList"
        [ ("type", opt annotation t); ("list", expr list);
          ("function", fold_fn functie); source br ]
  | Filter_list { typ = t; list; binder; condition; bron = br } ->
      node "FilterList"
        [ ("type", opt annotation t); ("list", expr list); ("binder", s binder);
          ("condition", conditie condition); source br ]
  | List_length { typ = t; list; bron = br } ->
      node "ListLength" [ ("type", opt annotation t); ("list", expr list); source br ]
  | Struct_literal { typ = t; velden; bron = br } ->
      node "StructLiteral"
        [ ("type", opt annotation t);
          ("fields",
           `List
             (List.map
                (fun (nm, v) -> `Assoc [ ("name", s nm); ("value", expr v) ])
                velden));
          source br ]
  | Field_access { typ = t; base; veld; bron = br } ->
      node "FieldAccess"
        [ ("type", opt annotation t); ("base", expr base); ("field", s veld); source br ]
  | Let { typ = t; binder; value; body; bron = br } ->
      (* the value-level binding (IR-SPEC ch. 4.10): [type] is non-null on the wire and
         equals the body type (WF-076); [binder] joins the WF-064 regime *)
      node "Let"
        [ ("type", opt annotation t); ("binder", s binder);
          ("value", expr value); ("body", expr body); source br ]
  (* The translator builds no ParameterReference and no VariableReference: the
     plugin writes no parameters, so a parameter reference would resolve
     against no declaration (WF-034), and a variable read has no executor that
     carries it. Written, either would reach the verifier or an executor as a
     refusal; the serialiser refuses first, naming the node. *)
  | other ->
      failwith
        ("Emit.expr: node not produced by the skeleton: "
        ^ (match other with
          | Parameter_ref _ -> "ParameterReference" | Variabele_ref _ -> "VariableReference"
          | Tekst_reeks _ -> "TextComposition" | Functie _ -> "Aggregation or BuiltinCall"
          | Afgerond _ -> "Rounding" | Begrensd _ -> "Clamp"
          | Dimensie_cel _ -> "DimensionCellRead" | Temporal_read _ -> "TemporalRead"
          | Lambda _ -> "Lambda (position-restricted; only a Fold/MapList operand)"
          | _ -> "?"))

(* the Fold/MapList [function] operand: an inline Lambda object or a bare
   FunctionDef-name string (the two wire forms of IR-SPEC ch. 4.8). *)
and fold_fn (f : fold_fn) : json =
  match f with
  | Fn_name n -> `String n
  | Fn_lambda { parameters; body; bron = br } ->
      node "Lambda"
        [ ("parameters", param_list parameters); ("body", expr body); source br ]

(* Lambda / FunctionDef parameters: a name and a datatype, both required. *)
and param_list (ps : (string * datatype) list) : json =
  `List (List.map (fun (n, dt) -> `Assoc [ ("name", s n); ("datatype", datatype dt) ]) ps)

and conditie (c : conditie) : json =
  (* the translator builds no whole-period qualifier, and the plugin declares
     no time-dependent attribute or characteristic for one to qualify
     (WF-043), so a filled one refuses rather than being written *)
  let whole_period = function
    | None -> []
    | Some (_ : geheel_periode) ->
        failwith "Emit.conditie: condition shape not produced by the skeleton: a wholePeriod qualifier"
  in
  match c with
  | Vergelijking { op; links; rechts; periode = p; bron = br } ->
      node "Comparison"
        ([ ("op", enum Spelling.comparison_op op); ("left", opt expr links);
           ("right", opt expr rechts) ]
        @ whole_period p @ [ source br ])
  | Samengesteld { kwantor; drempel; onderdelen; bron = br } ->
      node "Compound"
        [ ("quantifier", enum Spelling.quantifier kwantor); ("threshold", i drempel);
          ("parts", `List (List.map conditie onderdelen)); source br ]
  | Kenmerk_check
      { kenmerk; negatie; eigenaar; periode = p; bron = br; navigatie_binding = nb } ->
      (* the host is written only when the check names one (absent is the
         subject, RUNTIME-SEMANTICS ch. 5.8b); navigation is written whole or
         not at all, only when the check actually navigates (ch. 4.1a/5: a
         caller reads a callee characteristic over the synthesised back-link —
         the condition sibling of a navigated AttributeReference); it is the
         hop *)
      node "CharacteristicCheck"
        ([ ("characteristic", so kenmerk); ("negated", b negatie) ]
        @ optional "host" s eigenaar @ whole_period p @ [ source br ]
        @ optional "navigation" rol_binding nb)
  (* no marker either: see the expression arm *)
  | Conditie_onopgelost _ ->
      failwith "Emit.conditie: condition shape not produced by the skeleton: Unresolved"
  | _ -> failwith "Emit.conditie: condition shape not produced by the skeleton"

(* an open side of a validity is an absent bound; neither bound is always *)
let geldigheid (g : geldigheid) : json =
  node "Validity" (optional "from" s g.geld_vanaf @ optional "to" s g.geld_tot)

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
                node "Characteristic"
                  [ ("name", s k); ("kind", enum Spelling.characteristic_kind kind) ])
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
    | Enumeratie { domein; _ } -> Some domein
    | Sum_type { domein; _ } | Struct_type { domein; _ } -> Some (Some domein)
    | _ -> None
  in
  let body =
    match (self_name, datatype dt) with
    | None, j -> j
    | Some naam, `Assoc kvs ->
        named "domain" key naam;
        `Assoc (List.filter (fun (k, _) -> k <> "domain") kvs)
    | Some _, _ -> failwith (Printf.sprintf "Emit: domain %S did not serialise as an object" key)
  in
  node "Domain" [ ("name", s key); ("datatype", body) ]

(* A version's validity is always written (IR-SPEC ch. 3.10): a record
   without one is valid always. *)
let validity (g : geldigheid option) : json =
  geldigheid (Option.value g ~default:{ geld_vanaf = None; geld_tot = None })

(* A named form (IR-SPEC ch. 3, Versioning): {node, name, versions}. The
   plugin builds every rule, consistency and object creation with one
   version, so each in-memory record is one form. A record name ending in a
   space, '#' and digits is the in-memory spelling of one version of a form
   with several, which the plugin never builds: it refuses rather than
   writing a name no reader loads. *)
let form ~what kind (naam : string option) (version : json) : json =
  (match naam with
  | Some n when Ir_v0.Loader.versioned_name n ->
      failwith
        (Printf.sprintf
           "Emit: the %s name %S ends in a space, '#' and digits, the in-memory spelling of one \
            version of a form with several, and the plugin builds no versions"
           what n)
  | _ -> ());
  node kind [ ("name", so naam); ("versions", `List [ version ]) ]

(* A rule version (IR-SPEC ch. 3.1, delta B20): the in-memory kind is read
   back onto the initialisation flag and the presentation tag, and the target
   is an attribute reference. *)
let rule (r : resolved_regel) : json =
  if r.rr_bron_annotaties <> None then failwith "Emit: the plugin writes no statute annotations";
  let initialisation, presentation =
    match r.rr_soort with
    | Some ((Gelijkstelling | Berekening) as k) ->
        (false, [ ("presentation", enum Spelling.presentation k) ])
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
  form ~what:"rule" "Rule" r.rr_naam
    (`Assoc
      ([ ("validity", validity r.rr_geldigheid); ("target", target); ("value", value) ]
      @ condition
      @ [ ("initialisation", b initialisation) ]
      @ presentation))

let consistency (c : resolved_consistentie) : json =
  let assertion =
    match c.assertie with
    | Some a -> conditie a
    | None -> failwith "Emit: a consistency version without its assertion"
  in
  form ~what:"consistency" "Consistency" c.cons_naam
    (`Assoc
      (fields
         [ ("subject", so c.cons_subject); ("assertion", assertion);
           ("validity", validity c.cons_geldigheid); source c.cons_bron ]))

(* fact type + roles (IR-SPEC ch. 2.6): the connecting relation the plugin
   synthesises to carry a subscope back-link (Phase 5c). Each role names its
   filler object type; the plugin's roles are always to-one. *)
let rol (r : rol) : json =
  node "Role"
    ([ ("name", so r.rol_naam); ("objectType", so r.rol_objecttype_naam) ]
    @ optional "plural" s r.rol_meervoud
    @ [ ("many", b r.rol_meervoudig) ])

let feittype (ft : feittype) : json =
  node "FactType"
    [ ("name", so ft.ft_naam); ("reciprocal", b ft.wederkerig);
      ("roles", `List (List.map rol ft.rollen)) ]

let toekenning (t : toekenning) : json =
  node "MemberInit"
    [ ("member", so t.toe_attribuut); ("value", opt expr t.toe_waarde);
      ("characteristic", b t.toe_kenmerk) ]

let object_creation (oc : resolved_object_creatie) : json =
  (* connectingRole is optional (IR-SPEC ch. 3.3) — the created-side role of
     the back-link, written whole when present *)
  let condition = match oc.oc_voorwaarde with None -> [] | Some c -> [ ("condition", conditie c) ] in
  let connecting =
    match oc.oc_verbindende_rol with None -> [] | Some rb -> [ ("connectingRole", rol_binding rb) ]
  in
  form ~what:"object creation" "ObjectCreation" oc.oc_naam
    (`Assoc
      ([ ("createdType", so oc.nieuw_type); ("subject", so oc.oc_subject);
         ("initialisations", `List (List.map toekenning oc.toekenningen)) ]
      @ condition
      @ [ ("validity", validity oc.oc_geldigheid) ]
      @ connecting))

let rec default (d : default) : json =
  (* every field but the node is written only when present, matching the
     loader's [ext] admission: no decision on an attribute or group default,
     no justification on an initialisation or group default, and an
     initialisation only when it is true *)
  let init = if d.default_initialisatie then [ ("initialisation", `Bool true) ] else [] in
  `Assoc
    (fields
       ([ ("node", `String "Default") ]
       @ optional "decision" b d.default_besluit
       @ optional "justification" conditie d.default_voorwaarde
       @ optional "consequence" expr d.default_consequentie
       @ init
       @ optional "exceptions" exception_groups d.default_excepties
       @ [ source d.default_bron ]))

(* the nested exception group riding [exceptions] (v1, Phase 5c): the
   {unordered, members} object (no node discriminator) and its
   [ExceptionGroup] members, mirroring the loader's
   [exception_groups]/[exception_group]. *)
and exception_groups (egs : exception_groups) : json =
  `Assoc
    [ ("unordered", b egs.egs_ongeordend);
      ("members", `List (List.map exception_group egs.egs_leden)) ]

and exception_group (eg : exception_group) : json =
  node "ExceptionGroup"
    [ ("defaults", `List (List.map default eg.eg_defaults)); source eg.eg_bron ]

let prioritized_defaults (pd : prioritized_defaults) : json =
  let unordered = if pd.pd_ongeordend then [ ("unordered", `Bool true) ] else [] in
  `Assoc
    (fields
       ([ ("node", `String "PrioritizedDefaults"); ("name", so pd.pd_naam);
          ("subject", `String pd.pd_onderwerp) ]
       @ optional "characteristic" s pd.pd_kenmerk
       @ optional "attribute" s pd.pd_attribuut
       @ [ ("defaults", `List (List.map default pd.defaults)) ]
       @ unordered
       @ [ source pd.pd_bron; ("validity", opt geldigheid pd.pd_geldigheid) ]))

let function_def (fd : function_def) : json =
  node "FunctionDef"
    [ ("name", s fd.fd_naam); ("parameters", param_list fd.fd_parameters);
      ("result", datatype fd.fd_result); ("body", expr fd.fd_body);
      source fd.fd_bron ]

(* The M1 container (decision V3 (b)): the declaration set under "model", the
   rule lists under "rules" with their L5 keys, in the schema's property
   order. functionDefs is an extension list, written only when non-empty;
   prioritizedDefaults is written whole, as it always was. The plugin
   builds no parameter, unit system, dimension, timeline, day kind or
   diagnostic, so those lists are written empty, and a filled one refuses:
   written as [] it would drop what it held without a word. The serialiser
   refuses an Unresolved marker as well, so no written document holds a marker a
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
  unfilled "dayKindDefinitions" d.resolved_dagsoorten;
  unfilled "allocations" d.verdelingen;
  unfilled "uniquenesses" d.uniciteiten;
  unfilled "uniquenesses" d.uniciteit_unies;
  unfilled "factCreations" d.feit_creaties;
  unfilled "startPointDeterminations" d.resolved_startpunten;
  unfilled "ruleGroups" d.regel_groepen;
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
         [ ("rules", `List (List.map rule d.resolved_regels));
           ("dayKindDefinitions", `List []);
           ("objectCreations", `List (List.map object_creation d.object_creaties));
           ("allocations", `List []);
           ("consistencies", `List (List.map consistency d.consistenties));
           ("uniquenesses", `List []);
           ("factCreations", `List []);
           ("startPointDeterminations", `List []);
           ("ruleGroups", `List []);
           ("prioritizedDefaults", `List (List.map prioritized_defaults d.prioritized_defaults)) ]);
      ("diagnostics", `List []) ]

let to_string (d : document) : string = Yojson.Safe.pretty_to_string (document d) ^ "\n"

let to_file (path : string) (d : document) : unit =
  let oc = open_out path in
  output_string oc (to_string d);
  close_out oc
