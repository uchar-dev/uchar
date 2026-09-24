(* Catala scopelang -> uchar-ir-v1, the 5a skeleton.

   Attachment point (PHASE5-CATALA §2, decision 1): the scopelang->dcalc
   boundary. [Driver.Passes.scopelang] runs surface -> desugared ->
   scopelang; we walk the result before scopes compile to functions, so
   scope structure and override polarity are still visible.

   The plugin builds typed [Ir_v0.Ast] values and serialises them through
   [Emit] (never hand-written JSON). Every construct outside the decided
   5a fragment refuses loudly ([Refuse]) with the construct named — the
   WF-044 house style, honest inexpressibility over silent approximation.

   Mapping (PHASE5-CATALA §3, all six §8 decisions taken):
   - scope declaration              -> object type (name = scope base name)
   - scope variable (non-boolean)   -> attribute of that object type
   - scope variable (boolean)       -> characteristic (kenmerk)
   - input variable                 -> attribute filled by the case (no rule)
   - integer                        -> Number, whole, 0 decimals
   - money                          -> Number, 2 decimals (row 34: no money type)
   - date                           -> Date at day precision
   - boolean                        -> Booleaan as a characteristic
   - decimal (rational)             -> REFUSED (no declared decimals)
   - single unconditional numeric definition -> a Berekening rule
   - single boolean definition      -> a Kenmerktoekenning (its value is the guard)
   - a chain (base + nested single exceptions) -> one attribute
                                       prioritizedDefaults, defaults high-to-low
   - root-level unordered exceptions (no unconditional base) ->
                                       a declared-unordered default group
   - below-root branching (a chain exception carrying unordered exceptions)
     -> one defaults group whose default carries nested [excepties]
     exception groups (row 5c, ch. 7.9: form-a default / group-default; WF-075)
   - a shared non-trivial base under all members of a root-level unordered
     group -> REFUSED (PHASE4-DEFAULTS §7; workable later by base-duplication)
   - context (reentrant) variable   -> REFUSED (see below; besluit 6 finding)
   - assertion                      -> consistency rule
   - enum (payload-carrying)        -> SumType + Construct/Match (row 36)
   - enum (all-nullary)             -> Enumeratie domain + value Literal/Match
                                       (WF-087, the keuzetype-familie card)
   - list-typed variable            -> ListType (row 10, WF-065)
   - map / filter / explicit fold   -> MapList / FilterList / Fold (row 10)
   - sum / maximum / minimum        -> the reduce lowering: a Fold over an
                                       Optional (row 36) with an IfThenElse
                                       reducer and an extraction Match (§3.5)
   - if-then-else (inside a body)   -> IfThenElse (WF-069; refused in a rule)
   - top-level function + call      -> FunctionDef + Invoke (row 10, DMN BKM)
   - struct type / literal / .field -> StructType / StructLiteral / FieldAccess
                                       (row 5c; a struct is a value, WF-071/074)
   - subscope call, tuple payloads, Sort/Find/Map2/argmin,
     scope-capturing bodies, plain-value topdefs -> REFUSED (§4 fences)

   The context (reentrant) finding. Besluit 6 wants a caller override of a
   context variable compiled as a TOP default (Reading B: the caller's value
   beats the scope's own computation). In a single-scope 5a program the
   only override source is the case dossier, and RUNTIME-SEMANTICS ch. 7.8
   places the virtual input default BELOW the ordinary defaults ("computation
   displaces input" — Reading A). So a defaults target with case input gives
   Reading A, never Reading B; realising Reading B needs an actual
   caller-override highest default, which needs a subscope call — refused in 5a.
   Context variables are therefore refused loudly here.

   The empty-operand rule. Catala reads every variable hard (error_empty:
   an empty value is a fatal NoValueProvided), while the IR's arithmetic
   and comparisons coalesce an empty operand (ch. 1.2, empty = 0). So a
   variable read as an arithmetic/comparison operand is admitted only when
   the variable is always filled: (i) it is an input — RECORDED ASSUMPTION:
   the case dossier fulfils Catala's input contract, i.e. every input is
   supplied — or (ii) it has an unconditional definition (a base default
   that always decides, filled by construction). Otherwise the read is
   refused loudly; picking coalesce-vs-hard is a deviation decision
   deferred beyond the skeleton. *)

open Shared_ast
module M = Catala_utils.Mark
module Global = Catala_utils.Global
module Sc = Scopelang.Ast
module Dio = Desugared.Ast
module I = Ir_v0.Ast

exception Refuse of string

let refuse fmt = Printf.ksprintf (fun s -> raise (Refuse s)) fmt

(* Catala's labeled variable states (CONSTRUCTS row 2) are sugar over chained
   assignment: the desugarer collapses each [state] of one scope variable into
   its own scopelang variable named [base#state] (a '#' joins base and state),
   and step n reads step n-1 by an ordinary variable reference. The plugin
   lowers each step as a plain internal attribute; the '#' is mangled to a
   space at the variable table so the step carries a plain declared name and
   every downstream read resolves through [lookup] unchanged. A base without
   states has no '#' and is returned verbatim (byte-identity for the corpus
   goldens). *)
let state_attr_name (vname : string) : string =
  String.map (function '#' -> ' ' | c -> c) vname

(* Enums referenced by the translated document, recorded as [keuzetype_of]
   builds each keuzetype; [file_to_document] resets it per run and reads it to
   emit exactly the used enums as domains — [ctx_enums] also carries built-in
   enums (e.g. the desugarer's Optional), which no 5b program references. *)
let used_enums = ref EnumName.Set.empty

(* Structs referenced by the translated document, recorded by [struct_type_of];
   [file_to_document] resets it per run and reads it to emit exactly the used
   structs as domains — the SumType landing pattern (a StructType is hosted by
   [domeinen], repeated whole at every use site). *)
let used_structs = ref StructName.Set.empty

(* The reduce lowering (§3.5) needs a nominal Optional sum type carrying the
   concrete element datatype; Catala's builtin [option_enum] carries a
   *universal* Present payload (Expr.option_enum_config), so [keuzetype_of] on
   it would refuse. We build the concrete Optional here and record its element
   datatype so [file_to_document] declares the domain. One element datatype per
   document — a second, different one has no nominal home (recorded flank). *)
let used_optional : I.datatype option ref = ref None
let optional_domain = EnumName.base ConstantNames.option_enum

(* --- IR building blocks ------------------------------------------------- *)

let geldig : I.geldigheid = { geld_vanaf = None; geld_tot = None }
let num_int : I.datatype = Numeriek { geheel = true; decimalen = 0; teken = Some Onbepaald; eenheid = None }
let num_money : I.datatype = Numeriek { geheel = false; decimalen = 2; teken = Some Onbepaald; eenheid = None }
(* the pinned Catala-decimal scale (PHASE5C-TUTORIAL §5, vraag 4-A): S = 4.
   A Catala decimal is an exact rational; it lands as a bounded Number at
   four decimals, and a value that does not terminate at four decimals faults
   loudly and consistently in both executors (ref's bounded-write gate,
   clingo's does-not-fit fence). *)
let rat_scale = 4
let num_rat : I.datatype = Numeriek { geheel = false; decimalen = rat_scale; teken = Some Onbepaald; eenheid = None }
let always_true : I.conditie = Samengesteld { kwantor = Alle; drempel = 0; onderdelen = []; bron = Some "altijd" }

let optional_dt (element : I.datatype) : I.datatype =
  Sum_type
    { domein = optional_domain;
      constructoren =
        [ (EnumConstructor.to_string ConstantNames.none_constr, None);
          (EnumConstructor.to_string ConstantNames.some_constr, Some element) ] }

let note_optional (element : I.datatype) : unit =
  match !used_optional with
  | None -> used_optional := Some element
  | Some e when e = element -> ()
  | Some _ ->
      refuse
        "the reduce lowering needs one Optional element datatype per document; a second, different \
         element datatype is not supported in v1 (recorded flank)"

(* --- Catala type -> IR datatype ---------------------------------------- *)

(* A Catala enum is a keuzetype (IR-SPEC ch. 2.4) and lands on ONE of its two
   wire variants — which one is decided by the payloads, not by the frontend
   (maintainer decision 2026-09-01, the keuzetype-familie card; this supersedes
   the uniform SumType mapping of PHASE5B-KERNEL §2.7). An enum with at least
   one payload-carrying constructor is the row-36 SumType. An ALL-NULLARY enum
   has NO SumType image: WF-087 declares a payloadless SumType ill-formed, so
   it normalises to the v0 Enumeratie domain — the enum's base name is the
   domain, its constructor names in declaration order are the values. Payload
   never converts; the nullary keuzetype exists in exactly one form.
   The declaring enum's base name is the domain either way; constructor
   payloads recurse through [catala_dt]; a TLit TUnit payload is nullary
   (None); a TTuple payload is an n-ary constructor — 5c struct territory —
   and refuses loudly. A [TStruct] is the row-5c record datatype
   ([struct_type_of]). *)
let rec catala_dt (enums : enum_ctx) (structs : struct_ctx) (t : typ) : I.datatype =
  match M.remove t with
  | TLit TInt -> num_int
  | TLit TBool -> Booleaan
  | TLit TMoney -> num_money
  | TLit TDate -> Datum_in_dagen
  | TLit TRat -> num_rat
  | TLit TDuration -> refuse "duration type: not supported in the 5a skeleton"
  | TLit (TUnit | TPos) -> refuse "unit/pos type: not a domain value"
  | TDefault inner -> catala_dt enums structs inner
  | TArrow _ -> refuse "function-valued variable: new kernel (row 10), designed spec-first in 5b"
  | TStruct name -> struct_type_of enums structs name
  | TEnum name -> keuzetype_of enums structs name
  | TTuple _ ->
      refuse
        "tuple payload: an n-ary constructor payload de-tuplifies to a Catala tuple — 5c struct territory, single-payload constructors only (WF); refused"
  | TArray inner ->
      let element = catala_dt enums structs inner in
      (match element with
       | Numeriek _ | Datum_in_dagen | Sum_type _ | Struct_type _ -> List_type { element }
       | _ ->
           refuse
             "WF-065: a list element must be first-order and non-list (Number, day-precision date, \
              a declared sum type, or a declared struct); this element type is inadmissible")
  | TOption _ -> refuse "option-typed variable: not supported in the 5a skeleton"
  | _ -> refuse "unsupported variable type"

(* An enum -> its keuzetype datatype: alternatives in declaration order
   (EnumConstructor ids are generative, so the map folds declaration-first).
   All-nullary normalises to Enumeratie (WF-087), otherwise SumType. *)
and keuzetype_of (enums : enum_ctx) (structs : struct_ctx) (name : EnumName.t) : I.datatype =
  used_enums := EnumName.Set.add name !used_enums;
  let sig_ =
    try EnumName.Map.find name enums
    with Not_found -> refuse "enum %S is not declared" (EnumName.base name)
  in
  let constructoren =
    List.rev
      (EnumConstructor.Map.fold
         (fun c ty acc ->
           let payload =
             match M.remove ty with TLit TUnit -> None | _ -> Some (catala_dt enums structs ty)
           in
           (EnumConstructor.to_string c, payload) :: acc)
         sig_ [])
  in
  if List.for_all (fun (_, payload) -> Option.is_none payload) constructoren then
    Enumeratie
      { domein = Some (EnumName.base name);
        waarden = List.map (fun (c, _) -> Some c) constructoren }
  else Sum_type { domein = EnumName.base name; constructoren }

(* A struct -> its StructType datatype: fields in declaration order
   (StructField ids are generative, so the map folds declaration-first). Field
   datatypes recurse through [catala_dt] and must be WF-071-admissible
   (Number / Date at day precision / SumType / StructType) — an inadmissible
   field type (Tekst, Booleaan, Enumeratie, Date at millisecond precision, a
   list) refuses loudly. *)
and struct_type_of (enums : enum_ctx) (structs : struct_ctx) (name : StructName.t) : I.datatype =
  used_structs := StructName.Set.add name !used_structs;
  let sig_ =
    try StructName.Map.find name structs
    with Not_found -> refuse "struct %S is not declared" (StructName.base name)
  in
  let velden =
    List.rev
      (StructField.Map.fold
         (fun fld ty acc ->
           let dt = catala_dt enums structs ty in
           (match dt with
            | Numeriek _ | Datum_in_dagen | Sum_type _ | Struct_type _ -> ()
            | _ ->
                refuse
                  "WF-071: struct %S field %S has an inadmissible datatype (only Number, \
                   day-precision date, a declared sum type or a declared struct are admissible \
                   struct fields)"
                  (StructName.base name) (StructField.to_string fld));
           (StructField.to_string fld, dt) :: acc)
         sig_ [])
  in
  Struct_type { domein = StructName.base name; velden }

let is_bool_dt = function I.Booleaan -> true | _ -> false

(* A reference carries no type on the wire (B13): its declaration is the one
   source. The translator knows that declaration where it BUILDS a reference,
   so it records the datatype there, keyed by the node itself (physical
   identity — two equal-looking binder reads in different scopes may differ),
   and [ir_typ] reads it back. *)
let ref_types : (I.expr * I.datatype option) list ref = ref []

let typed_ref (e : I.expr) (dt : I.datatype option) : I.expr =
  ref_types := (e, dt) :: !ref_types;
  e

(* the declared datatype riding an already-built IR expression. Used to set
   a [Let]'s type to its body's type (WF-076) and an [IfThenElse]'s type to
   its then-branch (WF-069), by construction rather than by re-inference. *)
let ir_typ (e : I.expr) : I.datatype option =
  match e with
  | Literal { typ; _ } | Tekst_reeks { typ; _ } | Binair { typ; _ }
  | Functie { typ; _ } | Afgerond { typ; _ } | Begrensd { typ; _ }
  | Constante { typ; _ } | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ } | Construct { typ; _ } | Match { typ; _ }
  | Struct_literal { typ; _ } | Field_access { typ; _ }
  | List_literal { typ; _ } | If_then_else { typ; _ } | Let { typ; _ }
  | Invoke { typ; _ } | Fold { typ; _ } | Map_list { typ; _ }
  | Filter_list { typ; _ } | List_length { typ; _ } -> typ
  | Attribuut_ref _ | Dimensie_cel _ | Parameter_ref _ | Variabele_ref _ | Binder_ref _ -> (
      match List.find_opt (fun (r, _) -> r == e) !ref_types with
      | Some (_, dt) -> dt
      | None -> failwith "Translate.ir_typ: a reference built without its declared datatype")
  (* neither a Lambda nor an instance-set union is a value: they carry no
     wire type at all *)
  | Lambda _ | Instance_set_union _ -> None

(* --- literals ----------------------------------------------------------- *)

let money_str (z : Z.t) : string =
  (* Catala money is a count of cents; the IR carries a 2-decimal Numeriek
     value lexeme (ref parses it with q_of_lexeme, clingo rescales it). *)
  let hundred = Z.of_int 100 in
  let neg = Z.sign z < 0 in
  let a = Z.abs z in
  Printf.sprintf "%s%s.%02d" (if neg then "-" else "") (Z.to_string (Z.div a hundred))
    (Z.to_int (Z.rem a hundred))

let date_str (d : Catala_runtime.date) : string =
  let y, m, dd = Catala_runtime.date_to_years_months_days d in
  Printf.sprintf "%04d-%02d-%02d" y m dd

(* an exact rational (a Catala decimal, [Q.t]) -> its four-decimal lexeme.
   The pinned scale is S = 4 (vraag 4-A); a rational that does not terminate
   at four decimals has no exact representation there and is refused loudly —
   the same barrier ref's bounded-write gate and clingo's does-not-fit fence
   enforce dynamically, applied at the source. *)
let rat_str (q : Q.t) : string =
  let scale = Z.pow (Z.of_int 10) rat_scale in
  let scaled = Q.mul q (Q.of_bigint scale) in
  if not (Z.equal (Q.den scaled) Z.one) then
    refuse
      "decimal literal %s does not terminate at %d decimals (the pinned Catala-decimal scale): it \
       has no exact four-decimal representation — refused"
      (Q.to_string q) rat_scale;
  let k = Q.num scaled in
  let neg = Z.sign k < 0 in
  let a = Z.abs k in
  Printf.sprintf "%s%s.%0*d" (if neg then "-" else "") (Z.to_string (Z.div a scale)) rat_scale
    (Z.to_int (Z.rem a scale))

let literal_expr (l : lit) : I.expr =
  match l with
  | LBool b ->
      let w = if b then "waar" else "onwaar" in
      Literal { waarde = Some w; typ = Some Booleaan; bron = Some w }
  | LInt z -> Literal { waarde = Some (Z.to_string z); typ = Some num_int; bron = Some (Z.to_string z) }
  | LMoney z -> Literal { waarde = Some (money_str z); typ = Some num_money; bron = Some (money_str z) }
  | LDate d -> Literal { waarde = Some (date_str d); typ = Some Datum_in_dagen; bron = Some (date_str d) }
  | LRat q -> Literal { waarde = Some (rat_str q); typ = Some num_rat; bron = Some (rat_str q) }
  | LDuration _ -> refuse "duration literal: not supported in the 5a skeleton"
  | LUnit -> refuse "unit literal: not a domain value"

(* --- per-scope translation --------------------------------------------- *)

(* strip the default-monad wrappers off the head of an expression *)
let rec strip e =
  match M.remove e with
  | EErrorOnEmpty inner -> strip inner
  | EPureDefault inner -> strip inner
  | _ -> e

let is_true e = match M.remove (strip e) with ELit (LBool true) -> true | _ -> false
let is_empty e = match M.remove (strip e) with EEmpty -> true | _ -> false

(* WF-077, the condition-operand discipline (IR-SPEC ch. 4.10). Everywhere an
   [IfThenElse] is admissible (a lambda, function or [Let] body), its
   condition may READ only binders, parameters and literals; arithmetic
   combinators over those are admissible. An attribute/field/characteristic/
   aggregation/temporal read as (or inside) a condition operand must be hoisted
   through a [let … in] binding first — the leeg-barrier, which since
   maintainer decision O3 sits at the READ (RUNTIME-SEMANTICS ch. 10.6
   clause 2), not at the bind. Enforced here
   as a pre-walk over the raw scopelang condition, refusing loudly with the
   hoist instruction; the translation that follows then meets only bound reads
   and literals. *)
let rec check_cond_wf077 e =
  match M.remove (strip e) with
  | ELit _ | EVar _ -> ()
  | EAppOp { args; _ } -> List.iter check_cond_wf077 args
  | EIfThenElse { cond; etrue; efalse } ->
      check_cond_wf077 cond; check_cond_wf077 etrue; check_cond_wf077 efalse
  | ELocation (ScopelangScopeVar { name; _ }) ->
      refuse
        "WF-077: the condition of an if-then-else reads the scope variable %S directly; bind it \
         through a `let … in` first and compare the binder — a condition may read only \
         binders, parameters and literals (the condition-operand discipline, the leeg-barrier)"
        (M.remove (ScopeVar.get_info (M.remove name)))
  | ELocation _ ->
      refuse
        "WF-077: an if-then-else condition may read only binders, parameters and literals; hoist \
         this read through a `let … in` binding first"
  | EStructAccess { field; _ } ->
      refuse
        "WF-077: the condition of an if-then-else reads the struct field %S directly; hoist it \
         through a `let … in` binding first (a condition reads only binders, parameters and \
         literals)"
        (StructField.to_string field)
  | _ ->
      refuse
        "WF-077: an if-then-else condition admits only binders, parameters, literals and arithmetic \
         over those; hoist any other read through a `let … in` binding first"

(* --- the default tree, parsed into a defaults tree (PHASE5C §4, ch. 7.9) --

   The two flat parsers (a linear chain, a flat root-unordered group) become
   one recursive walk over the scopelang default tree. A [pdefault] carries the
   raw Catala (justification, consequence) exprs; [translate_def] lowers it to
   [I.default] once [value_expr]/[cond_default] are in scope. A chain is
   highest priority first, mirroring the wire order. The three shapes:
   - [Plain (just, cons)]      — an ordinary default.
   - [Form_a (just, cons, g)]  — own content PLUS nested exception groups
                                 [g] (they decide first, the own
                                 consequence is the fall-back — ch. 7.9 form a).
   - [Group_default g]         — a pure group-default: no own content, only its
                                 exception groups (a multi-member base or
                                 exception position — ch. 7.9 form b).
   Each exception group is itself a chain ([pdefault list]). *)
type 'e pdefault =
  | Plain of 'e * 'e
  | Form_a of 'e * 'e * 'e pgroup
  | Group_default of 'e pgroup
and 'e pgroup = 'e pdefault list list

(* classify a stripped scopelang default node. Catala emits, per scope
   variable, one default tree: a priority node [⟨ excepts | true :- cons ⟩]
   (cons = this level's definitions, excepts = higher-priority exceptions),
   whose definition levels are groups [⟨ defs | false :- ∅ ⟩], bottoming out
   in leaf defaults [⟨ | just :- cons ⟩]. *)
let default_kind e =
  match M.remove (strip e) with
  | EDefault { excepts = []; just; cons } when not (is_empty cons) -> `Leaf (just, cons)
  | EDefault { excepts; just = _; cons } when is_empty cons -> `Group excepts
  | EDefault { excepts; just = _; cons } -> `Prio (excepts, cons)
  | _ -> `Other

(* the recursive walk: a default subtree -> a defaults chain (highest first), or
   None when the shape is outside the fragment (a loud refusal upstream). *)
let rec parse_tree e : 'e pdefault list option =
  match default_kind e with
  | `Leaf (just, cons) -> Some [ Plain (just, cons) ]
  | `Group [ single ] -> parse_tree single (* a single-def group unwraps *)
  | `Group members -> Option.map (fun g -> [ Group_default g ]) (parse_group members)
  | `Prio (excepts, cons) -> (
      match parse_tree cons with None -> None | Some lower -> attach excepts lower)
  | `Other -> None

and parse_group members : 'e pgroup option =
  let rec go acc = function
    | [] -> Some (List.rev acc)
    | m :: rest -> ( match parse_tree m with Some c -> go (c :: acc) rest | None -> None)
  in
  go [] members

(* attach higher-priority exceptions to a lower chain: a single chain exception
   stacks above (strict priority — ordered defaults); an unordered group (>=2
   alternatives) nests into the head default's exception slot (form a —
   flattening it to ordered defaults would suppress the very conflict the paper
   raises, the spurious-conflict argument of PHASE4-DEFAULTS §7). *)
and attach excepts lower =
  match excepts with
  | [] -> Some lower
  | [ single ] -> (
      match default_kind single with
      | `Group members when List.length members >= 2 -> nest members lower
      | _ -> ( match parse_tree single with Some hi -> Some (hi @ lower) | None -> None))
  | _ -> nest excepts lower

(* nest an unordered exception set as the [Form_a] excepties of the
   default it excepts. Two shapes stay refused (recorded, PHASE4-DEFAULTS §7 —
   the sub-root refusal narrows to exactly these):
   - excepting a group-default (the default it excepts is itself a multi-member
     group), and
   - excepting the UNCONDITIONAL base directly (a shared non-trivial base
     under all members — the root-level limitation, workable later by
     base-duplication under the equal-decisions rule).
   An unordered group only nests onto an ordinary conditional exception
   default. *)
and nest members lower =
  match (parse_group members, lower) with
  | Some g, Plain (j, c) :: rest when not (is_true j) -> Some (Form_a (j, c, g) :: rest)
  | _ -> None

(* the ordered-comparison polarity of a scopelang operator around a split
   point K: [`Below] is [E < K], [`AboveEq] is [E >= K], and mirrored for the
   strict/non-strict flip. A non-comparison operator has no polarity. *)
let cmp_polarity o =
  match o with
  | Op.Lt -> Some `Below | Op.Lte -> Some `BelowEq
  | Op.Gt -> Some `Above | Op.Gte -> Some `AboveEq
  | _ -> None

(* two justifications form an EXHAUSTIVE complementary split of the ordered
   domain: [E < K] with [E >= K], or [E > K] with [E <= K] — the same operand
   [E] and split point [K], strict on one side and non-strict on the other.
   Such a pair covers every input, so the group they head is never empty (the
   operand-fill analysis: a total base group is always filled). *)
let complementary_just j1 j2 =
  match M.remove (strip j1), M.remove (strip j2) with
  | ( EAppOp { op = o1; args = [ a1; b1 ]; _ },
      EAppOp { op = o2; args = [ a2; b2 ]; _ } ) ->
      (match cmp_polarity (M.remove o1), cmp_polarity (M.remove o2) with
       | Some `Below, Some `AboveEq | Some `AboveEq, Some `Below
       | Some `Above, Some `BelowEq | Some `BelowEq, Some `Above -> true
       | _ -> false)
      && Expr.equal a1 a2 && Expr.equal b1 b2
  | _ -> false

(* does the tree carry an always-deciding base (the lowest default)? Either an
   unconditional base default, or a two-member base GROUP whose members split the
   domain exhaustively (the date-complementary [article_2] shape: old [< 2000]
   / new [>= 2000] with the [false ⊢ ∅] fallback — total, never empty). Recurses
   through the nested form — used for the operand-fill analysis. *)
let base_is_filled e =
  match parse_tree (strip e) with
  | None -> false
  | Some defaults ->
      List.exists
        (function
          | Plain (j, _) | Form_a (j, _, _) -> is_true j
          | Group_default [ [ Plain (j1, _) ]; [ Plain (j2, _) ] ] -> complementary_just j1 j2
          | Group_default _ -> false)
        defaults

(* a whole-struct per-element-map definition (FUSION, PHASE5C-TUTORIAL §3a): the
   unconditional [map each x among source to (output of Callee with {…})] shape,
   [source] and the map's lambda binder/tys/body-args extracted through the
   default-tree wrapper. Returns the callee, the source list, the (single)
   binder, its declared types and the subscope call's args (callee input ->
   arg over the binder), or None when the definition is not that shape. *)
let whole_struct_map_of e =
  match parse_tree (strip e) with
  | Some [ Plain (just, cons) ] when is_true just -> (
      match M.remove (strip cons) with
      | EAppOp { op; args = [ f; source ]; _ } when M.remove op = Op.Map -> (
          match M.remove (strip f) with
          | EAbs { binder; tys; _ } ->
              let vs, body = Bindlib.unmbind binder in
              (match M.remove (strip body) with
               | EScopeCall { scope; args } when Array.length vs = 1 ->
                   Some (scope, source, vs.(0), List.nth tys 0, args)
               | _ -> None)
          | _ -> None)
      | _ -> None)
  | _ -> None

(* the per-element functie-route (besluit 1-A, 2026-08-24). The 2-4 shape
   collapses at scopelang to a MapList over the list VALUE carrying an inline
   [EScopeCall] in the lambda body — [map (λg → (S of {…}).f) xs]. When the
   callee is a PURE FUNCTION (every output a plain chain expressible as a closed
   IfThenElse/Let/Match body; no own object creation; no non-input reads; a
   total base) it lowers to a FunctionDef (row-10 machinery) and the map body to
   [MapList(xs, λg. Invoke(S, …))]. No instance, no creation key: the
   per-element identity IS the list position (clingo's existing lelem index
   reification). This record carries what the [EStructAccess]/[EScopeCall]
   handler needs to build the [Invoke]: the synthesised FunctionDef name, the
   callee input variables in FunctionDef parameter order (to line the call
   arguments up), and the accessed output field datatype (the Invoke type). The
   hook itself refuses the ratified weiger-grens loudly (own creation, non-input
   reads, unordered/conflict-group outputs, multi-output). *)
type per_element_meta = {
  pem_fname : string;
  pem_params : ScopeVar.t list;
  pem_result_ty : I.datatype;
}

(* --- expression / condition translation --------------------------------

   [value_expr] and [cond_expr] are the same walk for a scope rule body and a
   closed top-level function body; the only difference is how a scope-variable
   read resolves. The caller supplies [read_var]/[check_var]/[lookup_var]: a
   scope maps them to attribute/characteristic reads, a function body refuses
   them (the closed-body fence, WF-067). [in_body] tracks whether we are inside
   a lambda or function body — where IfThenElse is admissible (WF-069) and a
   scope-variable read is a capture (WF-067). *)
let make_exprs ~(enums : enum_ctx) ~(structs : struct_ctx)
    ~(functions : (string * I.datatype) list)
    ~(read_var : operand:bool -> in_body:bool -> ScopeVar.t M.pos -> I.expr)
    ~(check_var : in_body:bool -> ScopeVar.t M.pos -> I.conditie)
    ~(lookup_var : ScopeVar.t -> I.datatype option)
    ~(read_subscope_output : ScopeVar.t -> StructField.t -> I.expr option)
    ~(check_subscope_output : ScopeVar.t -> StructField.t -> I.conditie option)
    ~(per_element_call : ScopeName.t -> StructField.t -> per_element_meta)
    ~fused_map =
  (* an EAbs (a fold/map/filter operand or a function) -> its bound variables,
     their declared types, and its body *)
  let lambda_of e =
    match M.remove (strip e) with
    | EAbs { binder; tys; _ } ->
        let vars, body = Bindlib.unmbind binder in
        (vars, tys, body)
    | _ -> refuse "a fold-family function operand is not a lambda (unexpected scopelang shape)"
  in
  (* a fresh accumulator-parameter name for the reduce lowering, avoiding the
     reduce lambda's own parameter names *)
  let fresh_acc_name avoid =
    let base = "geaccumuleerde optie" in
    let rec go n =
      let cand = if n = 0 then base else Printf.sprintf "%s %d" base n in
      if List.mem cand avoid then go (n + 1) else cand
    in
    go 0
  in
  let rec infer_dt ?(env = Var.Map.empty) e : I.datatype option =
    match M.remove (strip e) with
    | ELit (LInt _) -> Some num_int
    | ELit (LBool _) -> Some Booleaan
    | ELit (LMoney _) -> Some num_money
    | ELit (LDate _) -> Some Datum_in_dagen
    | ELit (LRat _) -> Some num_rat
    | EVar v -> Option.map snd (Var.Map.find_opt v env)
    | ELocation (ScopelangScopeVar { name }) -> lookup_var (M.remove name)
    | EInj { name; _ } -> Some (keuzetype_of enums structs name)
    | EStruct { name; _ } -> Some (struct_type_of enums structs name)
    | EStructAccess { name; field; _ } ->
        Option.map (catala_dt enums structs)
          (Option.bind (StructName.Map.find_opt name structs) (StructField.Map.find_opt field))
    | EIfThenElse { etrue; _ } -> infer_dt ~env etrue
    | EApp { f; _ } -> (
        match M.remove (strip f) with
        | ELocation (ToplevelVar { name; _ }) ->
            List.assoc_opt (TopdefName.base (M.remove name)) functions
        | _ -> None)
    | EAppOp { op; args = a :: _; _ } -> (
        match M.remove op with
        | Op.Length -> Some num_int
        (* the decimal casts yield an exact rational (num_rat): the operand is a
           pass-through at value level, but its STATIC type is the rational scale,
           which the money × rate split (money_mult_shape) reads to route a
           computed-rate product through the split encoding *)
        | Op.ToRat_int | Op.ToRat_mon -> Some num_rat
        | _ -> infer_dt ~env a)
    | _ -> None
  in
  let rec value_expr ?(operand = false) ?(in_body = false) ?(in_let = false)
      ?(in_map_body = false) ?(env = Var.Map.empty) e : I.expr =
    (* [in_body]: a closed lambda/function body — scope reads refuse (WF-067),
       IfThenElse admissible. [in_let]: a [Let] body — scope reads are
       allowed (a Let body is NOT closed), IfThenElse admissible (WF-069, the
       5c widening). IfThenElse is admissible iff [in_body || in_let].
       [in_map_body]: this node IS the map lambda's body expression (the
       ratified per-element 2-4 position, besluit 1-A). It is set only at the
       map body call site and never propagated into a sub-expression, so it is
       true exactly for a per-element [EScopeCall] read that is DIRECTLY the map
       body — a fold-reducer/function/match body (all [in_body]) is excluded,
       since only the map path has a clingo image (its Invoke lowers via
       body_branches; a fold-nested Invoke would land as a closed_term the
       backend cannot translate). *)
    let e = strip e in
    match M.remove e with
    | ELit l -> literal_expr l
    | EVar v -> (
        (* a bound binder (match-arm payload, lambda/function parameter) ->
           BinderRef (RUNTIME-SEMANTICS ch. 9.2/10.2); deliberately distinct
           from VariabeleRef (row 24) *)
        match Var.Map.find_opt v env with
        | Some (nm, dt) -> typed_ref (Binder_ref { naam = nm; bron = Some nm }) (Some dt)
        | None -> refuse "unbound variable %S in a value position" (Bindlib.name_of v))
    | ELocation (ScopelangScopeVar { name }) -> read_var ~operand ~in_body name
    | EAppOp { op; args; _ } -> (
        let bin bop a b : I.expr =
          Binair { op = bop; links = Some (value_expr ~operand:true ~in_body ~in_let ~env a);
                   rechts = Some (value_expr ~operand:true ~in_body ~in_let ~env b);
                   typ = infer_dt ~env e; bron = None }
        in
        match M.remove op, args with
        | (Op.Add_int_int | Op.Add_mon_mon | Op.Add_dur_dur | Op.Add_rat_rat), [ a; b ] -> bin Plus a b
        | (Op.Sub_int_int | Op.Sub_mon_mon | Op.Sub_dur_dur | Op.Sub_rat_rat), [ a; b ] ->
            bin Verminderd_met a b
        | (Op.Mult_int_int | Op.Mult_dur_int | Op.Mult_rat_rat), [ a; b ] -> bin Maal a b
        | (Op.Mult_mon_int | Op.Mult_mon_rat), [ a; b ] ->
            (* Catala rounds a money product to the cent (o_mult_mon_rat, §5) —
               wrap the product in Afgerond{REKENKUNDIG,2} exactly as the money
               divisions are (the Div precedent). A money × 4-decimal rate takes
               clingo's split encoding downstream (money_mult_shape). *)
            Afgerond
              { basis = Some (bin Maal a b); modus = Rekenkundig; decimalen = 2;
                typ = Some num_money; bron = None }
        | (Op.Div_int_int | Op.Div_dur_dur | Op.Div_rat_rat), [ a; b ] -> bin Gedeeld_door a b
        | (Op.Div_mon_int | Op.Div_mon_rat), [ a; b ] ->
            (* Catala rounds money division to cents (arithmetic / nearest);
               wrap the quotient so the IR rounds identically instead of
               faulting on the remainder *)
            Afgerond
              { basis = Some (bin Gedeeld_door a b); modus = Rekenkundig;
                decimalen = 2; typ = Some num_money; bron = None }
        | Op.Minus_rat, [ a ] ->
            (* unary rational negation -> 0 - a in the additive family (MIN
               coalesces both operands to 0; the operand is not leeg here) *)
            Binair
              { op = Min;
                links =
                  Some (Literal { waarde = Some (rat_str Q.zero); typ = Some num_rat;
                                  bron = Some (rat_str Q.zero) });
                rechts = Some (value_expr ~operand:true ~in_body ~in_let ~env a);
                typ = Some num_rat; bron = None }
        | (Op.ToRat_int | Op.ToRat_mon), [ a ] ->
            (* the decimal casts (decimal of n / of $m) are pass-through: a Catala
               decimal is already an exact rational, so the operand carries through
               unchanged, its declared datatype riding the mixed-decimals family *)
            value_expr ~operand ~in_body ~in_let ~env a
        | Op.Div_mon_mon, [ _; _ ] ->
            refuse "money / money division yields a decimal (rational): refused in the 5a skeleton (no declared number of decimals)"
        | Op.Map, [ f; lst ]
          when (match M.remove (strip lst) with
                | ELocation (ScopelangScopeVar { name }) -> fused_map (M.remove name) <> None
                | _ -> false) ->
            (* FUSION (PHASE5C-TUTORIAL §3a, 2026-08-25): [lst] reads a
               whole-struct per-element-map INTERNAL (a [list of Callee], never on
               the wire) consumed PER FIELD — [map each x among shares to x.field].
               It fuses to [MapList(source, λx. Invoke(callee_field, args))]: the
               intermediate struct list never exists. [f] must be λx. x.field (a
               bare field access on the element); any other consumption of the
               fused internal has no per-field image and refuses. *)
            let fused_v =
              match M.remove (strip lst) with
              | ELocation (ScopelangScopeVar { name }) -> M.remove name
              | _ -> assert false
            in
            let callee, source, bvar, bname, elem_ty, cargs =
              match fused_map fused_v with Some m -> m | None -> assert false
            in
            let field =
              match M.remove (strip f) with
              | EAbs { binder; _ } -> (
                  let vs, body = Bindlib.unmbind binder in
                  match M.remove (strip body) with
                  | EStructAccess { e = eb; field; _ }
                    when Array.length vs = 1
                         && (match M.remove (strip eb) with EVar _ -> true | _ -> false) ->
                      field
                  | _ ->
                      refuse
                        "the whole-struct per-element-map internal %S is consumed by a map whose body \
                         is not a bare field access on the element: a fused internal must be consumed \
                         PER FIELD (never as a whole struct — no clingo image for the list)"
                        (M.remove (ScopeVar.get_info fused_v)))
              | _ -> refuse "a map operand is not a lambda (unexpected scopelang shape)"
            in
            let pem = per_element_call callee field in
            let env' = Var.Map.add bvar (bname, elem_ty) env in
            let arguments =
              List.map
                (fun iv ->
                  match ScopeVar.Map.find_opt iv cargs with
                  | Some (_, argexpr) ->
                      value_expr ~operand:true ~in_body:true ~in_let:false ~env:env' argexpr
                  | None ->
                      refuse "the fused per-element call to %S omits input %S" pem.pem_fname
                        (M.remove (ScopeVar.get_info iv)))
                pem.pem_params
            in
            let invoke : I.expr =
              Invoke
                { typ = Some pem.pem_result_ty; functie = pem.pem_fname; arguments;
                  bron = Some pem.pem_fname }
            in
            Map_list
              { typ = Some (List_type { element = pem.pem_result_ty });
                list = value_expr ~in_body ~in_let ~env source;
                functie =
                  Fn_lambda { parameters = [ (bname, elem_ty) ]; body = invoke; bron = Some bname };
                bron = None }
        | Op.Map, [ f; lst ] ->
            let vars, tys, body = lambda_of f in
            if Array.length vars <> 1 then refuse "a map lambda takes exactly one parameter";
            let pn = Bindlib.name_of vars.(0) and pt = catala_dt enums structs (List.nth tys 0) in
            let env' = Var.Map.add vars.(0) (pn, pt) env in
            let out_elt =
              match infer_dt ~env:env' body with
              | Some d -> d
              | None ->
                  (* the inference-blind shapes are all refusals: surface the
                     body's OWN reason (e.g. a whole-struct per-element subscope
                     read — the tutorial-2-4 [shares_of_household_tax] shape,
                     mapping to a WHOLE callee output rather than one output
                     field) rather than a generic inference message. Only ever
                     reached on the refusal path, so no successful golden pays
                     the extra translation. *)
                  ignore (value_expr ~in_body:true ~in_let:false ~env:env' body);
                  refuse "cannot infer the element datatype of a map body"
            in
            Map_list
              { typ = Some (List_type { element = out_elt });
                list = value_expr ~in_body ~in_let ~env lst;
                functie = Fn_lambda { parameters = [ (pn, pt) ];
                                      body = value_expr ~in_body:true ~in_let:false ~in_map_body:true ~env:env' body; bron = Some pn };
                bron = None }
        | Op.Filter, [ f; lst ] ->
            let vars, tys, body = lambda_of f in
            if Array.length vars <> 1 then refuse "a filter lambda takes exactly one parameter";
            let pn = Bindlib.name_of vars.(0) and pt = catala_dt enums structs (List.nth tys 0) in
            let env' = Var.Map.add vars.(0) (pn, pt) env in
            let src_ty =
              match infer_dt ~env lst with
              | Some (List_type _ as t) -> t
              | _ -> refuse "cannot infer the ListType of a filter source"
            in
            Filter_list
              { typ = Some src_ty; list = value_expr ~in_body ~in_let ~env lst; binder = pn;
                condition = cond_expr ~in_body ~in_let ~env:env' body; bron = None }
        | Op.Fold, [ f; init; lst ] ->
            let vars, tys, body = lambda_of f in
            if Array.length vars <> 2 then refuse "a fold lambda takes exactly two parameters";
            let an = Bindlib.name_of vars.(0) and at = catala_dt enums structs (List.nth tys 0) in
            let xn = Bindlib.name_of vars.(1) and xt = catala_dt enums structs (List.nth tys 1) in
            let env' = Var.Map.add vars.(1) (xn, xt) (Var.Map.add vars.(0) (an, at) env) in
            Fold
              { typ = Some at; list = value_expr ~in_body ~in_let ~env lst;
                functie = Fn_lambda { parameters = [ (an, at); (xn, xt) ];
                                      body = value_expr ~in_body:true ~in_let:false ~env:env' body; bron = None };
                init = value_expr ~in_body ~in_let ~env init; bron = None }
        | Op.Length, [ lst ] ->
            List_length { typ = Some num_int; list = value_expr ~in_body ~in_let ~env lst; bron = None }
        | Op.Reduce, _ ->
            refuse "a bare reduce appears only as the scrutinee of an Optional match (the sum/maximum/minimum lowering)"
        | Op.Sort _, _ ->
            refuse "row 10: list sorting (Sort) is out of the 5b kernel — argmin/argmax and ordering stay refused (§4)"
        | Op.Map2, _ ->
            refuse "row 10: Map2 (a zip-map over two lists) is out of the 5b kernel (§4)"
        | Op.Concat, _ ->
            refuse "row 10: list concatenation (Concat) has no corpus case (§4)"
        | Op.Find, _ ->
            refuse "row 10: list Find is out of the 5b kernel (§4)"
        | Op.ConstructorCheck _, _ ->
            refuse "row 10: list membership/exists/forall (a ConstructorCheck outside the Optional lowering) is out of the 5b kernel (§4)"
        | o, _ -> refuse "operator %S not supported in a value expression" (Operator.name o))
    | EApp { f; args; _ } -> (
        match M.remove (strip f) with
        | EAbs { binder; tys; _ } ->
            (* the value-level binding (IR-SPEC ch. 4.10): Catala lowers [let x = v in
               body] to [EApp{EAbs[x], [v]}], a chain nesting one per binding.
               Each binder joins the WF-064 regime for its body; the value is
               evaluated in the enclosing position (the binder is NOT visible in
               its own value, WF-076), the body in a Let position where
               IfThenElse widens in (WF-069). uchar's [Let] is call-by-need, so
               a leeg value propagates at the READ (ch. 10.6 clause 2) while
               Catala's own [let] is call-by-value — tracked delta 2 there,
               unobservable over the corpus (every binder is read).
               The node type equals the body type (WF-076), read off the body. *)
            let vars, body = Bindlib.unmbind binder in
            if Array.length vars <> List.length args then
              refuse "a let-binding applies %d value(s) to %d binder(s)" (List.length args)
                (Array.length vars);
            let argv = Array.of_list args in
            let rec build i env =
              if i = Array.length vars then value_expr ~in_body ~in_let:(not in_body) ~env body
              else
                let name = Bindlib.name_of vars.(i) in
                let bt = catala_dt enums structs (List.nth tys i) in
                let v = value_expr ~in_body ~in_let ~env argv.(i) in
                let rest = build (i + 1) (Var.Map.add vars.(i) (name, bt) env) in
                I.Let { typ = ir_typ rest; binder = name; value = v; body = rest; bron = Some name }
            in
            build 0 env
        | ELocation (ToplevelVar { name; _ }) ->
            let fname = TopdefName.base (M.remove name) in
            let rty =
              match List.assoc_opt fname functions with
              | Some d -> d
              | None -> refuse "invocation of an unknown top-level function %S" fname
            in
            Invoke
              { typ = Some rty; functie = fname;
                arguments = List.map (value_expr ~operand:true ~in_body ~in_let ~env) args; bron = Some fname }
        | _ -> refuse "application of a non-top-level expression is not supported")
    | EScopeCall _ ->
        (* a bare subscope call consumed as a whole struct value (rather than
           per output field): the declared-subscope lowering reads outputs one
           field at a time (PHASE5C-TUTORIAL §3) — a whole-struct read follows
           design 1 once both land. Refused loudly. *)
        refuse "subscope output consumed as a whole struct: only per-field reads of a declared subscope's outputs are lowered (PHASE5C-TUTORIAL §3); a whole-struct read is out of scope"
    | EStructAccess { name; e = inner; field } -> (
        (* the declared-subscope output read (PHASE5C-TUTORIAL §3.2): an
           [EStructAccess] on a subscope-instance scope variable — the field
           name IS the callee output-variable name at scopelang — lowers to a
           navigated [AttribuutRef] over the synthesised back-link. *)
        let via_subscope =
          match M.remove (strip inner) with
          | ELocation (ScopelangScopeVar { name = sv }) -> read_subscope_output (M.remove sv) field
          | _ -> None
        in
        match via_subscope with
        | Some ir -> ir
        | None -> (
        match M.remove (strip inner) with
        | EScopeCall { scope; args } when in_map_body ->
            (* an inline subscope call [(output of S with {…}).f] that is
               DIRECTLY the MAP LAMBDA body — the ratified per-element 2-4 shape
               (besluit 1-A). [in_map_body] (not the broader [in_body]) is the
               gate: it is set only at the map body site and never descends into
               a sub-expression, so a bare inline call in a rule value (the
               tutorial-2-1 shape), a call nested inside arithmetic, and a call
               in a fold-reducer/function/match body all fall to the refusal
               below — the last three [in_body] but not [in_map_body], and only
               the direct map body has a clingo image (its Invoke lowers via
               body_branches; a closed_term-nested Invoke faults in the backend).
               When the callee S is a PURE FUNCTION it lowers to a FunctionDef
               and this read to an [Invoke] of it — no instance, no creation key
               (the per-element identity is the list position). [per_element_call]
               validates purity + synthesises the FunctionDef (memoised) and
               refuses the weiger-grens loudly; here we line the call arguments up
               with the FunctionDef parameters and build the Invoke. A callee with
               an own creation, a non-input read, an unordered/conflict-group
               output or multiple outputs refuses inside the hook. *)
            let pem = per_element_call scope field in
            let arguments =
              List.map
                (fun iv ->
                  match ScopeVar.Map.find_opt iv args with
                  | Some (_, argexpr) -> value_expr ~operand:true ~in_body ~in_let ~env argexpr
                  | None ->
                      refuse
                        "the per-element call to %S omits input %S: a pure-function callee needs every input supplied"
                        pem.pem_fname (M.remove (ScopeVar.get_info iv)))
                pem.pem_params
            in
            Invoke
              { typ = Some pem.pem_result_ty; functie = pem.pem_fname; arguments;
                bron = Some pem.pem_fname }
        | EScopeCall _ when in_body ->
            (* an inline subscope call inside a closed body that is NOT directly
               the map lambda body — a fold-reducer body, a match arm, a
               function body, or nested inside arithmetic within a map body. Only
               the DIRECT map body lowers via [body_branches] (which handles the
               Invoke); a closed_term-nested Invoke has no clingo image (the
               backend's closed_term rejects it). Refused per element so the
               divergence surfaces as a loud refusal, not a ref-only lowering. *)
            refuse "inline per-element subscope call nested in a non-map closed body (a fold reducer, a match arm, a function body, or inside arithmetic): only a per-element call that is DIRECTLY the map lambda body lowers to the functie-route (besluit 1-A) — a closed-body-nested call has no clingo image and is out of scope"
        | EScopeCall _ ->
            (* a bare inline subscope call in a rule value (NOT inside a lambda
               body) — the tutorial-2-1 shape: an anonymous instance per
               occurrence, which the creation-per-subject key cannot carry. Only
               a DECLARED subscope lowers to object creation + navigation
               (PHASE5C-TUTORIAL §3), and only a per-element call directly in a
               map body lowers to the functie-route (besluit 1-A). Refused. *)
            refuse "inline subscope call (reading a subscope output of an anonymous instance): only declared subscopes lower to object creation + role navigation (PHASE5C-TUTORIAL §3), and only a per-element call directly in a map body lowers to the functie-route (besluit 1-A); a bare inline call is out of scope"
        | _ ->
            (* row-5c FieldAccess: [base] is a struct value; the node type is
               the field's declared datatype (WF-074). The empty-operand flag
               propagates to the base — reading a field of a not-always-filled
               struct in arithmetic stays refused, since Catala reads it hard
               while a FieldAccess coalesces via the surrounding operator. *)
            let veld_ty =
              match
                Option.bind (StructName.Map.find_opt name structs)
                  (StructField.Map.find_opt field)
              with
              | Some ty -> catala_dt enums structs ty
              | None ->
                  refuse "field %S is not declared on struct %S"
                    (StructField.to_string field) (StructName.base name)
            in
            Field_access
              { typ = Some veld_ty; base = value_expr ~operand ~in_body ~in_let ~env inner;
                veld = StructField.to_string field; bron = None }))
    | EInj { name; e = payload; cons } ->
        (* a constructor value. On a normalised all-nullary enum (WF-087) the
           keuzetype IS the Enumeratie domain, so the alternative is an ordinary
           enumeration-value Literal (WF-039) — Construct stays sum-only. On a
           payload enum it is the row-36 Construct; unit payload = nullary
           (argument None). *)
        let dt = keuzetype_of enums structs name in
        let cons_name = EnumConstructor.to_string cons in
        (match dt with
         | I.Enumeratie _ -> Literal { waarde = Some cons_name; typ = Some dt; bron = Some cons_name }
         | _ ->
             let sig_ = EnumName.Map.find name enums in
             let payload_ty = EnumConstructor.Map.find cons sig_ in
             let argument =
               match M.remove payload_ty with
               | TLit TUnit -> None
               | _ -> Some (value_expr ~in_body ~in_let ~env payload)
             in
             Construct { typ = Some dt; constructor_ = cons_name; argument; bron = Some cons_name })
    | EMatch { name; e = scrut; cases } when EnumName.equal name ConstantNames.option_enum ->
        (* the §3.5 reduce lowering: [match (reduce f l) with Absent -> D |
           Present x -> x] — Catala's sum/maximum/minimum desugaring
           (from_surface.ml). [reduce] itself lowers to a Fold over an
           Optional accumulator (init Absent, step a Match rebuilding
           Some(...)), and the outer match extracts it. *)
        let scrut = strip scrut in
        let f, lst =
          match M.remove scrut with
          | EAppOp { op; args = [ f; lst ]; _ } when M.remove op = Op.Reduce -> (f, lst)
          | _ ->
              refuse "an Optional match whose scrutinee is not a reduce is not in the fold kernel"
        in
        let rvars, rtys, rbody = lambda_of f in
        if Array.length rvars <> 2 then refuse "a reduce combining function takes two parameters";
        let elt = catala_dt enums structs (List.nth rtys 0) in
        let opt = optional_dt elt in
        note_optional elt;
        let none_c = EnumConstructor.to_string ConstantNames.none_constr in
        let some_c = EnumConstructor.to_string ConstantNames.some_constr in
        let some_of x : I.expr =
          Construct { typ = Some opt; constructor_ = some_c; argument = Some x; bron = Some some_c }
        in
        let an = Bindlib.name_of rvars.(0) and xn = Bindlib.name_of rvars.(1) in
        let acc_name = fresh_acc_name [ an; xn ] in
        let env_step = Var.Map.add rvars.(1) (xn, elt) (Var.Map.add rvars.(0) (an, elt) env) in
        let present_body = some_of (value_expr ~in_body:true ~in_let:false ~env:env_step rbody) in
        let absent_body = some_of (typed_ref (Binder_ref { naam = xn; bron = Some xn }) (Some elt)) in
        let step : I.expr =
          Match
            { typ = Some opt;
              scrutinee =
                typed_ref (Binder_ref { naam = acc_name; bron = Some acc_name }) (Some opt);
              arms = [ (none_c, None, absent_body); (some_c, Some an, present_body) ];
              bron = Some "de volgende vouwstap" }
        in
        let fold : I.expr =
          Fold
            { typ = Some opt; list = value_expr ~in_body ~in_let ~env lst;
              functie = Fn_lambda { parameters = [ (acc_name, opt); (xn, elt) ]; body = step;
                                    bron = Some "de vouwstap" };
              init = Construct { typ = Some opt; constructor_ = none_c; argument = None; bron = Some none_c };
              bron = Some "gevouwen tot een optionele waarde" }
        in
        let none_case = EnumConstructor.Map.find ConstantNames.none_constr cases in
        let some_case = EnumConstructor.Map.find ConstantNames.some_constr cases in
        let d_body =
          match M.remove none_case with
          | EAbs { binder; _ } ->
              let _v, b = Bindlib.unmbind binder in
              value_expr ~in_body ~in_let ~env b
          | _ -> value_expr ~in_body ~in_let ~env none_case
        in
        let sbinder, s_body =
          match M.remove some_case with
          | EAbs { binder; _ } ->
              let vs, b = Bindlib.unmbind binder in
              let n = Bindlib.name_of vs.(0) in
              (n, value_expr ~in_body ~in_let ~env:(Var.Map.add vs.(0) (n, elt) env) b)
          | _ -> refuse "the Present arm of an Optional match is not a function"
        in
        Match
          { typ = Some elt; scrutinee = fold;
            arms = [ (none_c, None, d_body); (some_c, Some sbinder, s_body) ];
            bron = Some "de gevouwen waarde uitgepakt" }
    | EMatch { name; e = scrut; cases } ->
        let sig_ = EnumName.Map.find name enums in
        (* exhaustiveness fold (mirrors dcalc/from_scopelang.ml:223–249):
           scopelang does not guarantee totality, so a partial cases map is
           refused with the missing constructors named — the IR-side WF-063
           stays the independent gate. Wildcards never reach here: the
           desugarer fans a wildcard out over the missing constructors. *)
        let missing =
          List.rev
            (EnumConstructor.Map.fold
               (fun c _ acc ->
                 if EnumConstructor.Map.mem c cases then acc
                 else EnumConstructor.to_string c :: acc)
               sig_ [])
        in
        if missing <> [] then
          refuse "match on enum %S is not total: missing constructor(s) %s"
            (EnumName.base name) (String.concat ", " missing);
        (* arms in declared-constructor order; each case is a one-argument
           EAbs (the desugarer wraps the payload binder — nullary carries a
           unit binder, which becomes binder: null). This is the ONE Match node
           for both keuzetype variants (WF-063 over both): on a normalised
           all-nullary enum every payload is unit, so every arm comes out with
           the value label in [constructor] and [binder] null — exactly the
           enumeration arm of WF-063, with the scrutinee typed Enumeratie by
           [keuzetype_of]. *)
        let arms_rev, inferred =
          EnumConstructor.Map.fold
            (fun c payload_ty (arms, inf) ->
              let case = EnumConstructor.Map.find c cases in
              let vars, body =
                match M.remove case with
                | EAbs { binder; _ } -> Bindlib.unmbind binder
                | _ ->
                    refuse
                      "match case for %S is not a function (unexpected scopelang shape)"
                      (EnumConstructor.to_string c)
              in
              let binder_name, env' =
                match M.remove payload_ty with
                | TLit TUnit -> (None, env)
                | _ ->
                    let v = vars.(0) in
                    let nm = Bindlib.name_of v in
                    (Some nm, Var.Map.add v (nm, catala_dt enums structs payload_ty) env)
              in
              let inf' =
                match inf with Some _ -> inf | None -> infer_dt ~env:env' body
              in
              ( (EnumConstructor.to_string c, binder_name, value_expr ~in_body ~in_let ~env:env' body)
                :: arms,
                inf' ))
            sig_ ([], None)
        in
        let result_dt =
          match inferred with
          | Some d -> d
          | None ->
              refuse "cannot infer the result datatype of a match on %S"
                (EnumName.base name)
        in
        Match
          { typ = Some result_dt; scrutinee = value_expr ~in_body ~in_let ~env scrut;
            arms = List.rev arms_rev; bron = Some ("match " ^ EnumName.base name) }
    | EArray elts -> (
        match elts with
        | [] -> refuse "an empty list literal has no inferable element datatype in the fold fragment"
        | e0 :: _ ->
            let elt =
              match infer_dt ~env e0 with
              | Some d -> d
              | None -> refuse "cannot infer the element datatype of a list literal"
            in
            List_literal
              { typ = Some (List_type { element = elt });
                elements = List.map (value_expr ~in_body ~in_let ~env) elts; bron = None })
    | EAbs _ -> refuse "a function value outside a fold/map operand is not first-class (WF-069): only a Fold/MapList function operand or a named function"
    | EStruct { name; fields } ->
        (* row-5c StructLiteral: fields in declaration order (WF-073). Catala's
           [from_surface] guarantees every declared field is supplied, so the
           map is exactly the declared fields. A field evaluating leeg makes the
           whole literal leeg (NO-HOLE); that is the surrounding semantics, so
           the field values are ordinary (non-operand) reads. *)
        let velden =
          List.rev
            (StructField.Map.fold
               (fun fld v acc ->
                 (StructField.to_string fld, value_expr ~in_body ~in_let ~env v) :: acc)
               fields [])
        in
        Struct_literal { typ = Some (struct_type_of enums structs name); velden; bron = None }
    | EIfThenElse { cond; etrue; efalse } ->
        if not (in_body || in_let) then
          refuse
            "if-then-else in a rule expression: a value-level conditional is admissible only inside a lambda, function or `let … in` body (WF-069); a rule keeps the defaults/voorwaarde idiom (bind through a `let … in` to carry it)";
        (* WF-077, the condition-operand discipline: a condition reads only
           binders, parameters and literals — an attribute/field read must be
           hoisted through a `let … in` first (checked before translation) *)
        check_cond_wf077 cond;
        let then_ = value_expr ~in_body ~in_let ~env etrue in
        let else_ = value_expr ~in_body ~in_let ~env efalse in
        If_then_else
          { typ = ir_typ then_; condition = cond_expr ~in_body ~in_let ~env cond;
            then_; else_; bron = None }
    | _ -> refuse "value-expression shape not supported in the 5a skeleton"
  and cmp_op ?(env = Var.Map.empty) o a b : I.vergelijk_op =
    let is_date = infer_dt ~env a = Some Datum_in_dagen || infer_dt ~env b = Some Datum_in_dagen in
    match o, is_date with
    | Op.Lt, false -> Kleiner | Op.Lt, true -> Eerder
    | Op.Lte, false -> Kleiner_of_gelijk | Op.Lte, true -> Eerder_of_gelijk
    | Op.Gt, false -> Groter | Op.Gt, true -> Later
    | Op.Gte, false -> Groter_of_gelijk | Op.Gte, true -> Later_of_gelijk
    | Op.Eq, _ -> Gelijk
    | _ -> refuse "comparison operator not supported"
  and cond_expr ?(in_body = false) ?(in_let = false) ?(env = Var.Map.empty) e : I.conditie =
    let e = strip e in
    match M.remove e with
    | ELit (LBool true) -> always_true
    | ELit (LBool false) -> refuse "a literal-false condition is not expressible as a default justification"
    | ELocation (ScopelangScopeVar { name }) -> check_var ~in_body name
    | EAppOp { op; args; _ } -> (
        let vergelijking vop a b : I.conditie =
          Vergelijking
            { op = vop; links = Some (value_expr ~operand:true ~in_body ~in_let ~env a);
              rechts = Some (value_expr ~operand:true ~in_body ~in_let ~env b); periode = None; bron = None }
        in
        match M.remove op, args with
        | (Op.Lt | Op.Lte | Op.Gt | Op.Gte | Op.Eq), [ a; b ] ->
            vergelijking (cmp_op ~env (M.remove op) a b) a b
        | Op.Not, [ inner ] -> (
            (* Catala desugars [a != b] to [not (a = b)] (from_surface:705);
               it is the only Not shape the fragment expresses — a sum-type or
               scalar inequality -> ONGELIJK (leeg-correct: Tabel 38, both
               directions false over a leeg operand). *)
            match M.remove (strip inner) with
            | EAppOp { op = iop; args = [ a; b ]; _ } -> (
                match M.remove iop with
                | Op.Eq -> vergelijking Ongelijk a b
                | _ -> refuse "operator Not is only expressible over an equality (a != b)")
            | _ -> refuse "operator Not is only expressible over an equality (a != b)")
        | Op.And, [ a; b ] ->
            Samengesteld { kwantor = Alle; drempel = 0; onderdelen = [ cond_expr ~in_body ~in_let ~env a; cond_expr ~in_body ~in_let ~env b ]; bron = Some "en" }
        | Op.Or, [ a; b ] ->
            Samengesteld { kwantor = Ten_minste; drempel = 1; onderdelen = [ cond_expr ~in_body ~in_let ~env a; cond_expr ~in_body ~in_let ~env b ]; bron = Some "of" }
        | o, _ -> refuse "operator %S not supported in a condition" (Operator.name o))
    | EStructAccess { e = inner; field; _ } -> (
        (* the navigated leaf-read in CONDITION position (IR-SPEC ch. 4.1a/5,
           WF-053): a boolean output of a declared subscope instance read as a
           voorwaarde lowers to a navigated [Kenmerk_check] over the synthesised
           back-link — the condition sibling of the value-position
           [read_subscope_output], symmetric to the plain [check_var]. Only a
           declared subscope instance carries this image; a non-subscope
           struct-field condition (a boolean record field, or the value-position
           boolean read of besluit 4-A) has none and falls to the loud refusal. *)
        match
          match M.remove (strip inner) with
          | ELocation (ScopelangScopeVar { name = sv }) -> check_subscope_output (M.remove sv) field
          | _ -> None
        with
        | Some c -> c
        | None -> refuse "condition shape not supported in the 5a skeleton")
    | _ -> refuse "condition shape not supported in the 5a skeleton"
  in
  (value_expr, cond_expr)

(* the rule-producing variable base-names of a scope: a [ScopeVarDefinition]
   that is neither a subscope instance (that becomes an object creation) nor a
   pure input (that carries no rule). Used across scopes to detect a
   variable name that produces a rule in more than one scope, so its
   rule/defaults name is qualified by subject and stays globally unique
   (WF-020). A state step's rule name is the [state_attr_name]-mangled declared
   name (that is what [rule_label] compares against [ambiguous]), so mangle here
   too — otherwise a
   state variable shared across scopes never matches and collides on WF-020. *)
let ruled_vnames_of (sd : untyped Sc.scope_decl) : string list =
  List.sort_uniq compare
    (List.filter_map
       (function
         | Sc.ScopeVarDefinition { var; e; io; _ } -> (
             match M.remove (strip e) with
             | EScopeCall _ -> None
             (* a fused whole-struct per-element map produces no rule (FUSION,
                PHASE5C-TUTORIAL §3a) — exclude it from the WF-020 ambiguity set *)
             | _ when whole_struct_map_of e <> None -> None
             | _ -> (
                 match M.remove io.Dio.io_input with
                 | Catala_runtime.OnlyInput -> None
                 | _ -> Some (state_attr_name (M.remove (ScopeVar.get_info (fst var))))))
         | Sc.Assertion _ -> None)
       sd.scope_decl_rules)

(* one scope -> its object type, rules, defaults groups, consistencies.
   [ambiguous] is the set of rule variable names that recur across scopes (rule
   names for them are qualified by subject). *)
let translate_scope (enums : enum_ctx) (structs : struct_ctx) (functions : (string * I.datatype) list)
    ~(ambiguous : string list) ~(top_default_vars : ScopeName.t -> ScopeVar.Set.t)
    ~(callee_multistate : ScopeName.t -> string list)
    ~(per_element_call : ScopeName.t -> StructField.t -> per_element_meta)
    (name : ScopeName.t) (sd : untyped Sc.scope_decl) =
  let ot_name = ScopeName.base name in
  (* a rule/defaults name for [vname]: qualified by subject when [vname] produces
     a rule in more than one scope, bare otherwise (existing single-scope names
     are unchanged — byte-identity for the corpus goldens). *)
  let rule_label vname = if List.mem vname ambiguous then ot_name ^ " " ^ vname else vname in
  (* declared subscopes (PHASE5C-TUTORIAL §3): a declared subscope collapses at
     scopelang into ONE [ScopeVarDefinition] whose value is an [EScopeCall] and
     whose variable is TStruct-typed (the callee's output struct). Recorded here
     — BEFORE [catala_dt]'s struct path — so the instance variable is never
     treated as a record attribute: it is a to-be-created callee instance,
     linked to its caller by a synthesised back-link. The stored [args] map
     callee input variables to the caller argument expressions. *)
  let subscope_meta =
    List.fold_left
      (fun acc rule ->
        match rule with
        | Sc.ScopeVarDefinition { var; typ; e; _ } -> (
            match M.remove (strip e) with
            | EScopeCall { scope; args } ->
                let out_struct =
                  match M.remove typ with
                  | TStruct n -> n
                  | _ ->
                      refuse
                        "a subscope instance variable is not struct-typed (unexpected scopelang shape)"
                in
                ScopeVar.Map.add (fst var) (scope, args, out_struct) acc
            | _ -> acc)
        | Sc.Assertion _ -> acc)
      ScopeVar.Map.empty sd.scope_decl_rules
  in
  let is_subscope v = ScopeVar.Map.mem v subscope_meta in
  (* FUSION (PHASE5C-TUTORIAL §3a, 2026-08-25): a whole-struct per-element map
     [map each x among source to (output of Callee with {…})] landing in an
     INTERNAL — a [list of Callee] that is consumed ONLY per field — never exists
     on the wire: at each per-field consumption it fuses to
     [MapList(source, λx. Invoke(Callee_field, args))]. Recorded here (instance
     var -> callee, source, binder, element type, call args), and excluded from
     [vars]/rules like a subscope. The SAME shape declared as an OUTPUT (a list
     that must exist on the wire) is refused — Direction B. *)
  let fused_meta =
    List.fold_left
      (fun acc rule ->
        match rule with
        | Sc.ScopeVarDefinition { var; io; e; _ } -> (
            match whole_struct_map_of e with
            | Some (scope, source, bvar, bty, args) ->
                if M.remove io.Dio.io_output then
                  refuse
                    "variable %S is a whole-struct per-element map defined as an OUTPUT (a list of \
                     callee instances): the struct list must exist on the wire — no clingo image; \
                     Direction B, refused"
                    (M.remove (ScopeVar.get_info (fst var)));
                let bname = Bindlib.name_of bvar in
                let elem_ty = catala_dt enums structs bty in
                ScopeVar.Map.add (fst var) (scope, source, bvar, bname, elem_ty, args) acc
            | None -> acc)
        | Sc.Assertion _ -> acc)
      ScopeVar.Map.empty sd.scope_decl_rules
  in
  let is_fused v = ScopeVar.Map.mem v fused_meta in
  let fused_lookup v = ScopeVar.Map.find_opt v fused_meta in
  (* the synthesised back-link naming (PHASE5C-TUTORIAL §3.1): one fact type per
     declared subscope, globally unique by caller + instance variable; the
     callee fills a to-one role named for the instance variable, the caller the
     opposite role. *)
  let subscope_feittype v = ot_name ^ "_" ^ M.remove (ScopeVar.get_info v) in
  let subscope_callee_role v = M.remove (ScopeVar.get_info v) in
  let subscope_caller_role v = M.remove (ScopeVar.get_info v) ^ "_houder" in
  (* variable table: ScopeVar -> (declared name, IR datatype). Subscope
     instances are excluded — they are relations, not attributes. *)
  let vars : (string * I.datatype) ScopeVar.Map.t =
    ScopeVar.Map.fold
      (fun v (sv : Sc.scope_var_ty) acc ->
        if is_subscope v || is_fused v then acc
        else begin
          (* a state step lands as an ordinary internal attribute (row 2):
             [state_attr_name] mangles the [base#state] scopelang name to a
             plain declared name, step n reads step n-1 as any other variable
             read, resolved through [lookup]. A subscope reading a callee's
             multi-state output has no single attribute to bind and stays
             refused ([read_subscope_output], the row-9 flank). *)
          let vname = state_attr_name (M.remove (ScopeVar.get_info v)) in
          (* a context (reentrant) variable is no longer refused (besluit 6):
             it is an ordinary attribute whose OWN default becomes the base of
             a defaults group, and a caller override rides above it as a
             generated highest default ([build_subscope] below; the merge is in
             [file_to_document]). The io_input is consulted through
             [top_default_vars]. *)
          ScopeVar.Map.add v (vname, catala_dt enums structs sv.svar_out_ty) acc
        end)
      sd.scope_sig ScopeVar.Map.empty
  in
  let lookup v = ScopeVar.Map.find v vars in
  (* besluit 6: the reentrant variables of THIS scope that carry an own
     default — their own default becomes a defaults-group base (never a bare
     Berekening/Kenmerktoekenning, so a caller highest default can prepend,
     ch. 7.9), and a caller override lands as a generated highest default on
     that defaults group. A reentrant variable without an own default keeps the
     groupless MemberInit route (there is no own defaults group to sit
     above). *)
  let is_reentrant_top v = ScopeVar.Set.mem v (top_default_vars name) in
  (* boolean caller variables read as a subscope-input VALUE (an arg of a
     declared subscope call). A boolean is a characteristic, and a value-position
     read of a characteristic has no expression image (read_var refuses it). The
     WF-027 relaxation (besluit 1) carries the kenmerk-init value as a
     boolean-typed [Attribuut_ref], so such a caller boolean is declared as a
     boolean ATTRIBUTE — the jutterswet-rekenkamer-overzee anchor's shape (ref
     reads it from attrs, clingo as heeft). A boolean read only as a condition
     stays a characteristic. The scope of this widening is exactly the
     kenmerk-init position; the value-position refusal holds everywhere else. *)
  let value_read_bools =
    let bare_bool_read argexpr =
      match parse_tree (strip argexpr) with
      | Some [ Plain (just, cons) ] when is_true just -> (
          match M.remove (strip cons) with
          | ELocation (ScopelangScopeVar { name }) -> (
              let cv = M.remove name in
              match ScopeVar.Map.find_opt cv vars with
              | Some (_, Booleaan) -> Some cv
              | _ -> None)
          | _ -> None)
      | _ -> None
    in
    ScopeVar.Map.fold
      (fun _ (_scope, args, _out) acc ->
        ScopeVar.Map.fold
          (fun _ (_pos, argexpr) acc ->
            match bare_bool_read argexpr with Some cv -> ScopeVar.Set.add cv acc | None -> acc)
          args acc)
      subscope_meta ScopeVar.Set.empty
  in
  let is_value_bool v = ScopeVar.Set.mem v value_read_bools in
  (* a declared subscope's per-field output read (PHASE5C-TUTORIAL §3.2): an
     [EStructAccess] on the instance variable becomes a navigated [AttribuutRef]
     over the back-link — the field name IS the callee output-variable name, so
     it is also the callee attribute name. The output field's datatype comes
     from the callee's output struct directly (never routed through
     [struct_type_of], so the output struct is not emitted as a domain). *)
  let read_subscope_output v (field : StructField.t) : I.expr option =
    match ScopeVar.Map.find_opt v subscope_meta with
    | None -> None
    | Some (scope, _args, out_struct) ->
        let callee_type = ScopeName.base scope in
        let fld_name = StructField.to_string field in
        (* the narrowed multi-state flank (row 9): a callee output that is
           itself multi-state lowers to distinct internal attributes
           ("<base> <state>"), so there is no single attribute for the bare
           struct-field output the caller reads — refused loudly (a standalone
           state chain lands; only a subscope reading one does not). *)
        if List.mem fld_name (callee_multistate scope) then
          refuse
            "subscope output %S of %S is a multi-state variable: its states lower to distinct \
             internal attributes (one per state) and there is no single output attribute for the \
             caller's bare read — refused (multi-state subscope output)"
            fld_name callee_type;
        let fld_ty =
          match
            Option.bind (StructName.Map.find_opt out_struct structs)
              (StructField.Map.find_opt field)
          with
          | Some ty -> catala_dt enums structs ty
          | None ->
              refuse "subscope output field %S is not declared on callee %S" fld_name callee_type
        in
        (* besluit 4-A: a BOOLEAN callee output is a characteristic (kenmerk) on
           the callee, so a value-position read of it has no image — it would
           emit a dangling AttribuutRef the verifier admits and the executors
           split on (ref reads it leeg, clingo faults on the unknown attribute).
           Refuse loudly, the navigated sibling of scalar_read's own-boolean
           value guard and the inverse of check_subscope_output's condition
           guard; the author is pointed at the condition read that DOES lower. *)
        (match fld_ty with
         | I.Booleaan ->
             refuse
               "boolean subscope output %S of %S read in a value position: it is a characteristic \
                (kenmerk) on the callee, and a value-position read of a characteristic has no image \
                in the 5a fragment (read it as a condition instead)"
               fld_name callee_type
         | _ -> ());
        let callee_role = subscope_callee_role v in
        let bron = Printf.sprintf "de %s van de %s" fld_name (M.remove (ScopeVar.get_info v)) in
        Some
          (typed_ref
             (Attribuut_ref
                { attribuut = Some fld_name; objecttype = Some callee_type; bron = Some bron;
                  navigatie_bindingen =
                    Some [ { rb_feittype = subscope_feittype v; rb_rol = callee_role } ] })
             (Some fld_ty))
  in
  (* a declared subscope's per-field output read in CONDITION position (IR-SPEC
     ch. 4.1a/5, WF-053): an [EStructAccess] on the instance variable whose
     callee output field is BOOLEAN — a characteristic on the callee — lowers to
     a navigated [Kenmerk_check] over the same synthesised back-link the
     value-position read navigates. A non-boolean output field read as a
     condition, and the multi-state flank, refuse exactly as the value-position
     read does. *)
  let check_subscope_output v (field : StructField.t) : I.conditie option =
    match ScopeVar.Map.find_opt v subscope_meta with
    | None -> None
    | Some (scope, _args, out_struct) ->
        let callee_type = ScopeName.base scope in
        let fld_name = StructField.to_string field in
        if List.mem fld_name (callee_multistate scope) then
          refuse
            "subscope output %S of %S is a multi-state variable: its states lower to distinct \
             internal attributes (one per state) and there is no single output characteristic for \
             the caller's condition read — refused (multi-state subscope output)"
            fld_name callee_type;
        let fld_ty =
          match
            Option.bind (StructName.Map.find_opt out_struct structs)
              (StructField.Map.find_opt field)
          with
          | Some ty -> catala_dt enums structs ty
          | None ->
              refuse "subscope output field %S is not declared on callee %S" fld_name callee_type
        in
        (match fld_ty with
         | I.Booleaan -> ()
         | _ ->
             refuse
               "non-boolean subscope output %S of %S read as a condition: only a boolean output (a \
                callee characteristic) lowers to a navigated Kenmerk check"
               fld_name callee_type);
        let callee_role = subscope_callee_role v in
        let bron = Printf.sprintf "de %s is %s" (M.remove (ScopeVar.get_info v)) fld_name in
        Some
          (Kenmerk_check
             { kenmerk = Some fld_name; negatie = false;
               eigenaar = None; periode = None; bron = Some bron;
               navigatie_binding = Some { I.rb_feittype = subscope_feittype v; rb_rol = callee_role } })
  in
  (* variables that are always filled: inputs (the case dossier fulfils
     Catala's input contract) or variables carrying an unconditional default
     (a base default that always decides). Only these are admitted as
     arithmetic/comparison operands — see the head comment's empty-operand
     rule. *)
  let filled =
    let s = ref ScopeVar.Set.empty in
    ScopeVar.Map.iter
      (fun v (sv : Sc.scope_var_ty) ->
        if M.remove sv.svar_io.Dio.io_input = Catala_runtime.OnlyInput then
          s := ScopeVar.Set.add v !s)
      sd.scope_sig;
    List.iter
      (function
        | Sc.ScopeVarDefinition { var; e; _ } ->
            if base_is_filled e then s := ScopeVar.Set.add (fst var) !s
        | Sc.Assertion _ -> ())
      sd.scope_decl_rules;
    !s
  in
  let is_filled v = ScopeVar.Set.mem v filled in
  (* object type members, in declaration order *)
  let attributen = ref [] and kenmerken = ref [] in
  ScopeVar.Map.iter
    (fun v (vname, dt) ->
      if is_bool_dt dt && not (is_value_bool v) then kenmerken := vname :: !kenmerken
      else attributen := (vname, Some dt) :: !attributen)
    vars;
  let attributen = List.rev !attributen and kenmerken = List.rev !kenmerken in
  let objecttype : I.objecttype =
    { ot_naam = Some ot_name;
      ot_supertype = None;  (* this frontend models no type hierarchy *)
      attributen;
      attribuut_meervoud = [];
      attribuut_dimensies = [];
      attribuut_granulariteit = [];
      attribuut_tijdlijn_aantal = [];
      attribuut_tijdlijn_naam = [];
      kenmerken = List.map (fun k -> Some k) kenmerken;
      kenmerk_soorten = List.map (fun k -> (k, Some I.Bijvoeglijk)) kenmerken;
      kenmerk_granulariteit = [];
      kenmerk_tijdlijn_aantal = [];
      bezield = true;
      meervoud = None }
  in
  (* scope-variable reads resolve to attribute/characteristic reads; inside a
     closed body they are captures and refuse (WF-067). *)
  (* [nav = Some (feittype, rol)] wraps the read as a navigated one-hop read
     over the synthesised back-link (ch. 5.8), used for a caller override lowered
     as a highest default on the callee's defaults group (besluit 6): the default
     lives on the callee (the defaults-group subject), so its caller reads
     navigate callee -> caller. *)
  let attr_ref ?nav vname dt : I.expr =
    let navigatie_bindingen =
      match nav with
      | None -> None
      | Some (ft, rol) -> Some [ { I.rb_feittype = ft; rb_rol = rol } ]
    in
    typed_ref
      (Attribuut_ref
         { attribuut = Some vname; objecttype = Some ot_name; bron = Some ("de " ^ vname);
           navigatie_bindingen })
      (Some dt)
  in
  (* the shared scalar-read check (subscope/closed-body/boolean-carriage/
     empty-operand), returning the declared name and datatype; [read_var] wraps
     it in a bare [attr_ref], the override reader in a navigated one. *)
  let scalar_read ~operand ~in_body name : string * I.datatype =
    let v = M.remove name in
    if is_subscope v then
      refuse
        "the subscope instance %S is read as a whole struct value: read its outputs one field at a \
         time (PHASE5C-TUTORIAL §3.2); a whole-struct read is out of scope"
        (M.remove (ScopeVar.get_info v));
    if is_fused v then
      (* the fused whole-struct per-element map internal (PHASE5C-TUTORIAL §3a):
         it lands in [fused_meta], never in [vars] — [lookup] would raise. Only
         the bare per-field map (intercepted in the [Op.Map] fusion branch)
         has a per-field image; a whole read, a list-length, or a non-field map
         reaches here and must refuse loudly (mirroring [is_subscope]), never
         crash on [lookup]. *)
      refuse
        "the whole-struct per-element map internal %S is read other than per field: the fused \
         struct list never exists on the wire, so only bare per-field reads (map each x among it to \
         x.field) are lowered (PHASE5C-TUTORIAL §3a) — a whole read, a list-length, or a non-field \
         map has no per-field image; Direction B, refused"
        (M.remove (ScopeVar.get_info v));
    let vname, dt = lookup v in
    if in_body then
      refuse
        "WF-067: a closed lambda/function body may not read the scope variable %S (an attribute or characteristic read is ill-formed in a body — the scope-capturing fence)"
        vname
    else if is_bool_dt dt && not (is_value_bool v) then
      refuse
        "boolean variable %S read in a value position: booleans are characteristics (kenmerken); a value-position read of a characteristic has no image in the 5a fragment (read it as a condition instead)"
        vname
    else if operand && not (is_filled v) then
      refuse
        "variable %S read as an arithmetic/comparison operand is not always filled (not an input, no unconditional definition): Catala reads it hard (error_empty = fatal on empty) while the IR coalesces an empty operand — refused (deviation deferred)"
        vname
    else (vname, dt)
  in
  (* the shared characteristic-read check; [check_var] wraps it in a bare
     [Kenmerk_check], the override reader in a navigated one. *)
  let kenmerk_read ~in_body name : string =
    let v = M.remove name in
    if is_subscope v then
      refuse
        "the subscope instance %S is read in a condition: read its outputs one field at a time \
         (PHASE5C-TUTORIAL §3.2)"
        (M.remove (ScopeVar.get_info v));
    if is_fused v then
      (* the fused whole-struct per-element map internal read in a condition:
         mirror the [is_subscope] guard so it refuses loudly rather than raising
         on [lookup] (PHASE5C-TUTORIAL §3a). *)
      refuse
        "the whole-struct per-element map internal %S is read in a condition: the fused struct list \
         never exists on the wire, only bare per-field reads are lowered (PHASE5C-TUTORIAL §3a) — \
         read other than per field is out of scope, refused"
        (M.remove (ScopeVar.get_info v));
    let vname, dt = lookup v in
    if in_body then
      refuse
        "WF-067: a closed lambda/function body may not read the scope characteristic %S (the scope-capturing fence)"
        vname
    else if is_value_bool v then
      (* declared as a boolean ATTRIBUTE because it feeds a subscope kenmerk-init
         (value position); reading it ALSO as a condition would need it as a
         characteristic — the two carriages are incompatible, refused loudly *)
      refuse
        "boolean variable %S is read both as a subscope-input value and as a condition: the value \
         read carries it as an attribute, a condition read needs a characteristic — mixed carriage \
         is not supported (read one way)"
        vname
    else if is_bool_dt dt then vname
    else refuse "non-boolean variable %S used as a condition" vname
  in
  let read_var ~operand ~in_body name : I.expr =
    let vname, dt = scalar_read ~operand ~in_body name in
    attr_ref vname dt
  in
  let check_var ~in_body name : I.conditie =
    let k = kenmerk_read ~in_body name in
    Kenmerk_check
      { kenmerk = Some k; negatie = false; eigenaar = None; periode = None; bron = Some k;
        navigatie_binding = None }
  in
  let lookup_var v = try Some (snd (lookup v)) with Not_found -> None in
  let value_expr, cond_expr =
    make_exprs ~enums ~structs ~functions ~read_var ~check_var ~lookup_var ~read_subscope_output
      ~check_subscope_output ~per_element_call ~fused_map:fused_lookup
  in
  (* besluit 6: the value/condition walk that lowers a caller override into a
     highest default on the callee's defaults group. Same walk as the scope's
     own, but every caller read navigates back over the back-link (feittype,
     caller role): the override default sits on the callee's defaults group yet
     reads the caller's override value and guard — Reading B expressed as
     default order, no standing machinery. *)
  let override_exprs ~feittype ~rol =
    make_exprs ~enums ~structs ~functions
      ~read_var:(fun ~operand ~in_body name ->
        let vname, dt = scalar_read ~operand ~in_body name in
        attr_ref ~nav:(feittype, rol) vname dt)
      ~check_var:(fun ~in_body name ->
        let k = kenmerk_read ~in_body name in
        Kenmerk_check
          { kenmerk = Some k; negatie = false; eigenaar = None;
            periode = None; bron = Some k;
            navigatie_binding = Some { I.rb_feittype = feittype; rb_rol = rol } })
      ~lookup_var ~read_subscope_output ~check_subscope_output ~fused_map:(fun _ -> None)
      ~per_element_call:(fun _ _ ->
        refuse
          "a per-element subscope call inside a caller override value/guard is not supported (besluit 6 override reads navigate the back-link; the per-element functie-route has no navigated form)")
  in
  (* condition option: [true] is unconditional -> None (for plain rules) *)
  let cond_opt e = if is_true e then None else Some (cond_expr e) in
  (* a default always carries a justification (WF-050): an
     unconditional base becomes the vacuously-true ALLE, never null —
     null is reserved for the initialisation default (ch. 7.3). *)
  let cond_default e = Some (if is_true e then always_true else cond_expr e) in
  (* boolean value expression -> the guard condition it stands for *)
  let bool_value_guard value =
    match M.remove (strip value) with
    | ELit (LBool true) -> None
    | ELit (LBool false) -> refuse "a boolean defined as literal false is not attributable as a characteristic"
    | _ -> Some (cond_expr value)
  in
  let combine a b =
    match a, b with
    | None, x | x, None -> x
    | Some x, Some y -> Some (I.Samengesteld { kwantor = Alle; drempel = 0; onderdelen = [ x; y ]; bron = Some "en" })
  in
  (* accumulators *)
  let regels = ref [] and defaults_groups = ref [] and consistenties = ref [] in
  let feittypen = ref [] and creaties = ref [] in
  (* besluit 6: caller override highest defaults, keyed by (callee object type,
     callee attribute); [file_to_document] prepends them onto that callee's
     own defaults group once every scope is translated. *)
  let override_contribs : (string * string * I.default list) list ref = ref [] in
  let add_regel r = regels := r :: !regels in
  let add_defaults_group g = defaults_groups := g :: !defaults_groups in
  let berekening vname dt value ~cond : I.resolved_regel =
    { rr_naam = Some ("berekening " ^ rule_label vname); rr_soort = Some Berekening;
      doel = Some (attr_ref vname dt);
      waarde = Some (value_expr value); rr_voorwaarde = cond; rr_geldigheid = Some geldig;
      rr_bron_annotaties = None }
  in
  let kenmerktoekenning vname ~voorwaarde : I.resolved_regel =
    { rr_naam = Some ("kenmerktoekenning " ^ rule_label vname); rr_soort = Some Kenmerktoekenning;
      doel = Some (attr_ref vname Booleaan);
      waarde = Some (Literal { waarde = Some "waar"; typ = Some Booleaan; bron = Some "waar" });
      rr_voorwaarde = voorwaarde; rr_geldigheid = Some geldig; rr_bron_annotaties = None }
  in
  (* dispatch one variable definition *)
  let translate_def v e =
    let vname, dt = lookup v in
    let is_bool = is_bool_dt dt in
    let bool_besluit value =
      match M.remove (strip value) with
      | ELit (LBool b) -> b
      | _ -> refuse "a characteristic default's value must be a literal true/false"
    in
    (* lower a parsed defaults tree to IR defaults (ch. 7.9): a plain default, a
       form-a default (own content plus [excepties]), or a pure group-default (no
       own content). An exception group inherits the root target kind (attribute
       vs characteristic), so [is_bool] carries through the recursion. *)
    let rec build_default (pdef : _ pdefault) : I.default =
      match pdef with
      | Plain (cond, value) -> content_default cond value None
      | Form_a (cond, value, g) -> content_default cond value (Some (build_group g))
      | Group_default g ->
          { default_besluit = None; default_voorwaarde = None; default_consequentie = None;
            default_initialisatie = false; default_excepties = Some (build_group g);
            default_bron = Some vname }
    and content_default cond value excepties : I.default =
      if is_bool then
        { default_besluit = Some (bool_besluit value); default_voorwaarde = cond_default cond;
          default_consequentie = None; default_initialisatie = false;
          default_excepties = excepties; default_bron = Some vname }
      else
        { default_besluit = None; default_voorwaarde = cond_default cond;
          default_consequentie = Some (value_expr value); default_initialisatie = false;
          default_excepties = excepties; default_bron = Some vname }
    and build_group (g : _ pgroup) : I.exception_groups =
      { egs_ongeordend = List.length g >= 2;
        egs_leden =
          List.map
            (fun member : I.exception_group ->
              { eg_defaults = List.map build_default member; eg_bron = Some vname })
            g }
    in
    let build_defaults_group ?(ongeordend = false) suffix (chain : _ pdefault list) :
        I.prioritized_defaults =
      { pd_naam = Some (rule_label vname ^ suffix); pd_onderwerp = ot_name;
        pd_kenmerk = (if is_bool then Some vname else None);
        pd_attribuut = (if is_bool then None else Some vname);
        defaults = List.map build_default chain;
        pd_ongeordend = ongeordend; pd_bron = Some vname; pd_geldigheid = Some geldig }
    in
    (* besluit 6: a reentrant variable's own default must be a DEFAULTS GROUP
       (never a bare Berekening), so a caller override can ride above it as a
       highest default. The attribute-target chain own default is covered; a
       characteristic context target and a root-unordered own default are
       recorded flanks. *)
    if is_reentrant_top v && is_bool then
      refuse
        "context (reentrant) characteristic %S: a caller boolean override is a recorded besluit-6 \
         flank (the generated highest default covers attribute targets — a characteristic \
         highest default is not wired); refused"
        vname;
    match default_kind (strip e) with
    | `Group members when List.length members >= 2 ->
        if is_reentrant_top v then
          refuse
            "context (reentrant) variable %S with a root-level unordered own default: a caller \
             highest default above an unordered group is a recorded besluit-6 flank (a chain own \
             default is covered); refused"
            vname;
        (* a root-level unordered group -> the declared-unordered default group:
           one [ongeordend] defaults group per member (ch. 7.2, unchanged) —
           distinct from a NESTED unordered group, which rides [excepties] on one
           default *)
        List.iteri
          (fun i m ->
            let suffix = " #" ^ string_of_int (i + 1) in
            match parse_tree m with
            | Some chain ->
                add_defaults_group (build_defaults_group ~ongeordend:true suffix chain)
            | None ->
                refuse
                  "variable %S: an unordered-group member is outside the fragment (besluit 4)"
                  vname)
          members
    | _ -> (
        match parse_tree (strip e) with
        | None ->
            refuse
              "variable %S: the default tree combines an unconditional base with unordered \
               exceptions (a shared non-trivial base, PHASE4-DEFAULTS §7), or its shape is \
               outside the fragment — not a chain, a nested exception tree, nor a flat \
               declared-unordered group (besluit 4/vraag 5, PHASE5C §4)"
              vname
        | Some [ Plain (cond, value) ] ->
            (* a single plain default is a bare rule, not a defaults group —
               unless the target is a reentrant context variable, whose own
               default must be a defaults-group base for the caller highest
               default to sit above (besluit 6) *)
            if is_bool then
              add_regel
                (kenmerktoekenning vname
                   ~voorwaarde:(combine (cond_opt cond) (bool_value_guard value)))
            else if is_true cond && not (is_reentrant_top v) then
              add_regel (berekening vname dt value ~cond:None)
            else add_defaults_group (build_defaults_group "" [ Plain (cond, value) ])
        | Some chain -> add_defaults_group (build_defaults_group "" chain))
  in
  (* one declared subscope -> its synthesised fact type and object creation
     (PHASE5C-TUTORIAL §3.1): the call's args bind as member initialisations
     (Toekenningen) on the callee's INPUT attributes, plus the verbindendeRol
     back-link. A subscope input is an unconditional caller value; a conditional
     target or one carrying a defaults group is out of scope (the WF-050 default
     route is deferred). *)
  let build_subscope v (scope, args, _out_struct) =
    let callee_type = ScopeName.base scope in
    let instance_var = M.remove (ScopeVar.get_info v) in
    let callee_role = subscope_callee_role v in
    let caller_role = subscope_caller_role v in
    let feittype = subscope_feittype v in
    if List.exists (fun (ft : I.feittype) -> ft.ft_naam = Some feittype) !feittypen then
      refuse
        "two declared subscopes collide on the synthesised fact type %S (creation-per-subject key); \
         only one call per caller subject is keyable — refused (PHASE5C-TUTORIAL §3 flank)"
        feittype;
    let ft : I.feittype =
      { ft_naam = Some feittype; wederkerig = false;
        rollen =
          [ { rol_naam = Some caller_role; rol_objecttype_naam = Some ot_name; rol_meervoud = None;
              rol_meervoudig = false };
            { rol_naam = Some callee_role; rol_objecttype_naam = Some callee_type;
              rol_meervoud = None; rol_meervoudig = false } ] }
    in
    (* besluit 6: which of this callee's inputs are reentrant context targets
       carrying an own default — those args become highest defaults, not member
       initialisations. *)
    let ovr_set = top_default_vars scope in
    let ve_ov, ce_ov = override_exprs ~feittype ~rol:caller_role in
    (* a subscope input definition carries the ordinary scope-variable default
       shape [error_empty ⟨ ⟨true ⊢ ⟨X⟩⟩ | false ⊢ ∅ ⟩] — one unconditional
       plain default. Anything richer (a conditional base or a defaults group) is
       a defaults target, which the WF-050 route (deferred) would carry, not a
       member initialisation. *)
    let arg_value e =
      match parse_tree (strip e) with
      | Some [ Plain (just, cons) ] when is_true just -> value_expr cons
      | _ ->
          refuse
            "a subscope input to %S is not an unconditional value: a conditional subscope input, \
             or one carrying a defaults group, enters as a default, never a member initialisation \
             (the WF-050 route is deferred, PHASE5C-TUTORIAL §3) — refused"
            instance_var
    in
    (* besluit 6: a caller override of a reentrant callee variable that carries
       an own default (in [ovr_set]) does NOT bind as a member initialisation —
       it becomes a generated highest default above the callee's own defaults
       group (Reading B as default order, the §4.1 hardship-pattern shape; the
       WF-050 constraint). Its guard and value read the caller, navigated over
       the back-link (callee -> caller); no standing machinery. *)
    let override_default callee_var (pdef : _ pdefault) : I.default =
      match pdef with
      | Plain (just, value) ->
          { I.default_besluit = None;
            default_voorwaarde = Some (if is_true just then always_true else ce_ov just);
            default_consequentie = Some (ve_ov value);
            default_initialisatie = false; default_excepties = None;
            default_bron = Some ("context-override " ^ callee_var) }
      | Form_a _ | Group_default _ ->
          refuse
            "a caller override of the context variable %S is a branching tree (a nested or \
             root-unordered override): only a chain of plain-default overrides lowers to highest \
             defaults (besluit 6 flank); refused"
            callee_var
    in
    ScopeVar.Map.iter
      (fun k (_pos, argexpr) ->
        if ScopeVar.Set.mem k ovr_set then
          let callee_var = M.remove (ScopeVar.get_info k) in
          match parse_tree (strip argexpr) with
          | Some chain ->
              override_contribs :=
                (callee_type, callee_var, List.map (override_default callee_var) chain)
                :: !override_contribs
          | None ->
              refuse
                "a caller override of the context variable %S is outside the fragment (besluit 6); \
                 refused"
                callee_var)
      args;
    let toekenningen =
      List.rev
        (ScopeVar.Map.fold
           (fun k (_pos, argexpr) acc ->
             if ScopeVar.Set.mem k ovr_set then acc
             else begin
               let attr = M.remove (ScopeVar.get_info k) in
               let waarde = arg_value argexpr in
               (* a boolean input binds as a characteristic initialisation
                  (toe_kenmerk = true): the callee's input boolean is a kenmerk,
                  and the WF-027 relaxation (besluit 1) carries the caller's
                  boolean-typed value expression as its init. Numeric/date/struct/
                  enum inputs stay attribute initialisations. *)
               let toe_kenmerk = (match ir_typ waarde with Some Booleaan -> true | _ -> false) in
               { I.toe_attribuut = Some attr; toe_waarde = Some waarde; toe_kenmerk } :: acc
             end)
           args [])
    in
    let oc : I.resolved_object_creatie =
      { oc_naam = Some ("richt " ^ instance_var ^ " op"); nieuw_type = Some callee_type;
        oc_subject = Some ot_name; toekenningen; oc_voorwaarde = None; oc_geldigheid = Some geldig;
        oc_verbindende_rol = Some { rb_feittype = feittype; rb_rol = callee_role } }
    in
    feittypen := ft :: !feittypen;
    creaties := oc :: !creaties
  in
  (* walk the scope body *)
  List.iteri
    (fun idx rule ->
      match rule with
      | Sc.ScopeVarDefinition { var; io; e; _ } -> (
          match ScopeVar.Map.find_opt (fst var) subscope_meta with
          | Some info -> build_subscope (fst var) info
          | None ->
              (* a fused whole-struct per-element map produces no rule: the struct
                 list never exists on the wire, it is inlined per field at each
                 consumption (FUSION, PHASE5C-TUTORIAL §3a) *)
              if is_fused (fst var) then ()
              else (
                match M.remove io.Dio.io_input with
                | Catala_runtime.OnlyInput -> () (* input: filled by the case, no rule *)
                | _ -> translate_def (fst var) e))
      | Sc.Assertion { e; _ } ->
          consistenties :=
            { I.cons_naam = Some ("assertie " ^ ot_name ^ " " ^ string_of_int (idx + 1));
              cons_subject = Some ot_name; assertie = Some (cond_expr e);
              cons_bron = Some "assertion"; cons_line = idx + 1; cons_geldigheid = Some geldig }
            :: !consistenties)
    sd.scope_decl_rules;
  ( ot_name, objecttype, List.rev !regels, List.rev !defaults_groups, List.rev !consistenties,
    List.rev !feittypen, List.rev !creaties, List.rev !override_contribs )

(* --- top-level definitions -> FunctionDefs (row 10, DMN's BKM) ---------- *)

(* pass 1: the invocation signature (name -> result datatype), read from the
   arrow type — a plain-value topdef (a constant, not a function) is refused
   until a case wants it (recorded, §3.5). *)
let topdef_signatures enums structs topdefs : (string * I.datatype) list =
  List.map
    (fun (name, (_e, typ, _vis, is_ext)) ->
      if is_ext then refuse "an external top-level definition is not translated";
      let result =
        match M.remove typ with
        | TArrow (_, r) -> catala_dt enums structs r
        | _ ->
            refuse
              "a plain-value top-level definition (a constant, not a function) is refused until a case wants it (recorded)"
      in
      (TopdefName.base name, result))
    (TopdefName.Map.bindings topdefs)

(* pass 2: each function body -> a closed FunctionDef; the body sees only its
   parameters (a refusing scope context enforces the closed-body fence). *)
let topdef_functions enums structs functions topdefs : I.function_def list =
  let value_expr, _cond =
    make_exprs ~enums ~structs ~functions
      ~read_var:(fun ~operand:_ ~in_body:_ _ ->
        refuse "WF-067: a closed top-level function body may not read a scope variable")
      ~check_var:(fun ~in_body:_ _ ->
        refuse "WF-067: a closed top-level function body may not read a scope characteristic")
      ~lookup_var:(fun _ -> None)
      ~read_subscope_output:(fun _ _ -> None)
      ~check_subscope_output:(fun _ _ -> None)
      ~fused_map:(fun _ -> None)
      ~per_element_call:(fun _ _ ->
        refuse
          "WF-067: a closed top-level function body may not contain a per-element subscope call")
  in
  List.map
    (fun (name, (e, _typ, _vis, _ext)) ->
      let fname = TopdefName.base name in
      match M.remove (strip e) with
      | EAbs { binder; tys; _ } ->
          let vars, body = Bindlib.unmbind binder in
          let params =
            List.mapi (fun i v -> (Bindlib.name_of v, catala_dt enums structs (List.nth tys i)))
              (Array.to_list vars)
          in
          let env =
            List.fold_left2
              (fun acc v p -> Var.Map.add v p acc)
              Var.Map.empty (Array.to_list vars) params
          in
          { I.fd_naam = fname; fd_parameters = params;
            fd_result = List.assoc fname functions;
            fd_body = value_expr ~in_body:true ~in_let:false ~env body; fd_bron = Some fname }
      | _ ->
          refuse
            "a plain-value top-level definition (a constant, not a function) is refused until a case wants it (recorded)")
    (TopdefName.Map.bindings topdefs)

(* the per-element functie-route (besluit 1-A, 2026-08-24): a callee scope that
   is a PURE FUNCTION lowers to a closed FunctionDef, and a per-element call to
   it becomes an [Invoke]. Purity is checked here and refused loudly otherwise
   (the ratified weiger-grens): (1) no own object creation — the callee makes no
   subscope call of its own; (2) exactly one output — the multi-output form is
   the FieldAccess-on-StructLiteral shape, deferred (no clingo closed-body
   image); (3) the output is an ordered plain-default chain with a total base —
   an unordered/conflict-group output refuses; (4) no non-input reads — every read
   in the body resolves to one of the callee's inputs, mapped to the FunctionDef
   parameter binder (a read of an internal/output variable refuses). Direction B
   (real per-element instances) stays deferred. Returns the FunctionDef plus the
   [per_element_meta] the call site needs to line the arguments up. *)
let scope_to_function_def (enums : enum_ctx) (structs : struct_ctx)
    (functions : (string * I.datatype) list)
    ~(per_element_call : ScopeName.t -> StructField.t -> per_element_meta)
    (callee : ScopeName.t) (sd : untyped Sc.scope_decl) (accessed : StructField.t) :
    I.function_def * per_element_meta =
  let cname = ScopeName.base callee in
  (* a declared subscope inside the callee is no longer own-creation-refused
     (PHASE5C-TUTORIAL §3a, 2026-08-25): if its OWN callee also passes this
     analysis it is nested function composition — a subscope-output read lowers
     to a nested [Invoke] ([read_subscope_output] below, [per_element_call]
     recursing under the cycle guard). Recorded here (instance var -> callee +
     args). Real creation — a sub-callee needing a standing instance (an
     unordered/conflict output, a non-input read) — surfaces as that sub-callee
     failing purity, refused where its output is read (Direction B). *)
  let subscope_meta =
    List.fold_left
      (fun acc rule ->
        match rule with
        | Sc.ScopeVarDefinition { var; e; _ } -> (
            match M.remove (strip e) with
            | EScopeCall { scope; args } -> ScopeVar.Map.add (fst var) (scope, args) acc
            | _ -> acc)
        | Sc.Assertion _ -> acc)
      ScopeVar.Map.empty sd.scope_decl_rules
  in
  let sig_bindings = ScopeVar.Map.bindings sd.scope_sig in
  let inputs =
    List.filter_map
      (fun (v, (sv : Sc.scope_var_ty)) ->
        match M.remove sv.svar_io.Dio.io_input with
        | Catala_runtime.OnlyInput -> Some v
        | _ -> None)
      sig_bindings
  in
  let outputs =
    List.filter_map
      (fun (v, (sv : Sc.scope_var_ty)) ->
        if M.remove sv.svar_io.Dio.io_output then Some v else None)
      sig_bindings
  in
  (* MULTI-OUTPUT (PHASE5C-TUTORIAL §3a, 2026-08-25): one FunctionDef per read
     output FIELD. The accessed field must be a declared output; a multi-output
     callee qualifies the function name by field ("<callee> <field>"), a
     single-output callee keeps the bare callee name (byte-identity for the
     existing single-output goldens). *)
  let out_v =
    match
      List.find_opt
        (fun v -> M.remove (ScopeVar.get_info v) = StructField.to_string accessed)
        outputs
    with
    | Some v -> v
    | None -> (
        match outputs with
        | [] -> refuse "per-element callee %S declares no output to read" cname
        | _ ->
            refuse
              "the per-element call reads field %S, which is not a declared output of callee %S"
              (StructField.to_string accessed) cname)
  in
  let out_name = M.remove (ScopeVar.get_info out_v) in
  let multi = List.length outputs > 1 in
  let fname = if multi then cname ^ " " ^ out_name else cname in
  let dt_of v = catala_dt enums structs (ScopeVar.Map.find v sd.scope_sig).svar_out_ty in
  let out_dt = dt_of out_v in
  let params = List.map (fun v -> (M.remove (ScopeVar.get_info v), dt_of v)) inputs in
  let input_set =
    List.fold_left (fun s v -> ScopeVar.Set.add v s) ScopeVar.Set.empty inputs
  in
  (* a nested subscope argument — a forward of the enclosing callee's inputs (a
     bare input read, a field of an input struct) or a literal (function
     composition threads the enclosing inputs through). Like a subscope input it
     carries the unconditional-default wrapper [⟨true ⊢ X⟩], peeled here. *)
  let rec arg_ir_inner argexpr : I.expr =
    match M.remove (strip argexpr) with
    | ELocation (ScopelangScopeVar { name }) ->
        let v = M.remove name in
        if ScopeVar.Set.mem v input_set then
          let nm = M.remove (ScopeVar.get_info v) in
          typed_ref (Binder_ref { naam = nm; bron = Some nm }) (Some (dt_of v))
        else
          refuse
            "a nested subscope in %S forwards %S, which is not one of %S's inputs: a nested \
             pure-function composition forwards only the enclosing callee's inputs (non-input read)"
            cname (M.remove (ScopeVar.get_info v)) cname
    | ELit l -> literal_expr l
    | EStructAccess { name; e = base; field } ->
        let veld_ty =
          match
            Option.bind (StructName.Map.find_opt name structs) (StructField.Map.find_opt field)
          with
          | Some ty -> catala_dt enums structs ty
          | None ->
              refuse "field %S is not declared on struct %S" (StructField.to_string field)
                (StructName.base name)
        in
        Field_access
          { typ = Some veld_ty; base = arg_ir_inner base; veld = StructField.to_string field;
            bron = None }
    | _ ->
        refuse
          "a nested subscope argument in %S is not an input forward (a bare input, a field of an \
           input struct) nor a literal (function composition needs input-forwarded arguments)"
          cname
  in
  let arg_ir argexpr =
    arg_ir_inner
      (match parse_tree (strip argexpr) with
       | Some [ Plain (just, cons) ] when is_true just -> cons
       | _ -> argexpr)
  in
  (* NESTED INVOKE (PHASE5C-TUTORIAL §3a, 2026-08-25): a subscope-output read in
     the body -> a nested Invoke of the sub-callee's field FunctionDef
     ([per_element_call] recurses; the cycle guard + WF-067 keep the callgraph
     acyclic). The read is just that Invoke's value. A non-subscope non-input read
     still refuses ([read_var] below). *)
  let read_subscope_output v (field : StructField.t) : I.expr option =
    match ScopeVar.Map.find_opt v subscope_meta with
    | None -> None
    | Some (sub_scope, args) ->
        let pem = per_element_call sub_scope field in
        let arguments =
          List.map
            (fun iv ->
              match ScopeVar.Map.find_opt iv args with
              | Some (_, argexpr) -> arg_ir argexpr
              | None ->
                  refuse "the nested subscope call in %S omits input %S of %S" cname
                    (M.remove (ScopeVar.get_info iv)) (ScopeName.base sub_scope))
            pem.pem_params
        in
        Some
          (Invoke
             { typ = Some pem.pem_result_ty; functie = pem.pem_fname; arguments;
               bron = Some pem.pem_fname })
  in
  (* the closed body: an input read -> its parameter binder; a subscope-output
     read -> a nested Invoke; any other scope read refuses (non-input read); no
     direct per-element map call inside the body. *)
  let value_expr, cond_expr =
    make_exprs ~enums ~structs ~functions
      ~read_var:(fun ~operand:_ ~in_body:_ name ->
        let v = M.remove name in
        if ScopeVar.Set.mem v input_set then
          let nm = M.remove (ScopeVar.get_info v) in
          typed_ref (Binder_ref { naam = nm; bron = Some nm }) (Some (dt_of v))
        else
          refuse
            "per-element callee %S reads %S, which is not one of its inputs (non-input read): \
             refused per element (weiger-grens)"
            cname (M.remove (ScopeVar.get_info v)))
      ~check_var:(fun ~in_body:_ name ->
        refuse
          "per-element callee %S reads %S as a bare condition: a pure-function body admits only \
           numeric-comparison guards over inputs (no corpus case for a bare boolean-input condition)"
          cname (M.remove (ScopeVar.get_info (M.remove name))))
      ~lookup_var:(fun v -> if ScopeVar.Set.mem v input_set then Some (dt_of v) else None)
      ~read_subscope_output
      ~check_subscope_output:(fun _ _ -> None)
      ~fused_map:(fun _ -> None)
      ~per_element_call:(fun _ _ ->
        refuse
          "per-element callee %S itself makes a direct per-element subscope call in a map body \
           (nested per-element map): refused — Direction B is deferred"
          cname)
  in
  (* (3) the output default tree -> a closed IfThenElse body: an ordered chain of
     [Plain] defaults (highest first) bottoming at a total base; an
     unordered/conflict-group default refuses. *)
  let out_e =
    match
      List.find_map
        (function
          | Sc.ScopeVarDefinition { var; e; _ } when ScopeVar.equal (fst var) out_v -> Some e
          | _ -> None)
        sd.scope_decl_rules
    with
    | Some e -> e
    | None -> refuse "per-element callee %S output %S has no definition" cname out_name
  in
  let out_defaults =
    match parse_tree (strip out_e) with
    | Some ds -> ds
    | None ->
        refuse
          "per-element callee %S output %S is not a plain default chain (outside the closed-body \
           fragment)"
          cname out_name
  in
  let rec body_of = function
    | [ Plain (just, cons) ] ->
        if is_true just then value_expr ~in_body:true cons
        else
          refuse
            "per-element callee %S output %S has no total base (its lowest default is \
             conditional): a pure function needs a total base"
            cname out_name
    | Plain (just, cons) :: rest ->
        if is_true just then
          refuse "per-element callee %S output %S: an unconditional default shadows lower defaults"
            cname out_name
        else
          let then_ = value_expr ~in_body:true cons in
          If_then_else
            { typ = ir_typ then_; condition = cond_expr ~in_body:true just; then_;
              else_ = body_of rest; bron = None }
    | (Form_a _ | Group_default _) :: _ ->
        refuse
          "per-element callee %S output %S carries unordered/conflict exception groups: refused \
           per element (weiger-grens; only an ordered plain chain lowers to a closed IfThenElse body)"
          cname out_name
    | [] -> refuse "per-element callee %S output %S has an empty defaults chain" cname out_name
  in
  let body = body_of out_defaults in
  (* the closed-body arithmetic fence (besluit 1-A, 2026-08-24; NARROWED
     2026-08-25 by card clingo-bodymult). clingo's closed-body term
     ([closed_term], §S15) now carries — beyond the additive [Plus]/[Min] — the
     multiplicative [Maal], the money×rate cent product ([Afgerond] over a Maal),
     and a division by a COMPILE-TIME CONSTANT divisor. It still faults on binary
     subtraction ([Verminderd_met] — the additive family is Plus/Min only), a
     RUNTIME divisor, the abs-division, a power and a percentage: a per-element
     callee whose closed body carries one computes in ref but TRANSLATION-FAULTs
     in clingo. Refuse those loudly here so the divergence surfaces as a refusal,
     not silent ref-only IR. (decimal-of is pass-through — no node — and the
     money×rate wrapper rides [Afgerond].) *)
  let rec fence_expr (e : I.expr) : unit =
    match e with
    | Binair { op = (Plus | Min | Maal); links; rechts; _ } ->
        Option.iter fence_expr links; Option.iter fence_expr rechts
    | Binair { op = Gedeeld_door; links; rechts; _ } ->
        (* clingo's closed_division carries a compile-time POSITIVE constant
           divisor only (br = [] and the scaled value a positive int). A runtime
           or non-positive divisor faults there — refuse it here so the fence
           matches clingo (card clingo-bodymult). *)
        (match rechts with
         | Some (Literal { waarde = Some w; _ })
           when (match float_of_string_opt w with Some f -> f > 0.0 | None -> false) -> ()
         | _ ->
             refuse
               "per-element callee %S divides by a non-constant or non-positive divisor in its closed \
                body: clingo's closed-body division carries only a compile-time positive constant \
                divisor (card clingo-bodymult) — refused per element"
               cname);
        Option.iter fence_expr links; Option.iter fence_expr rechts
    | Binair { op = (Verminderd_met | Gedeeld_door_abs | Tot_de_macht | Percentage_van); _ } ->
        refuse
          "per-element callee %S carries a non-carried binary operator in its closed body (binary \
           subtraction, abs-division, power or percentage): clingo's closed-body term carries +, -, \
           ×, the money×rate cent product and a constant-divisor division, but not this form (card \
           clingo-bodymult) — refused per element"
          cname
    | Literal _ | Binder_ref _ | Parameter_ref _ | Variabele_ref _ | Constante _
    | Attribuut_ref _ | Expr_onopgelost _ -> ()
    | Functie { argumenten; leegwaarde; filter; _ } ->
        List.iter fence_expr argumenten; Option.iter fence_expr leegwaarde;
        Option.iter fence_cond filter
    | Afgerond { basis = Some basis; _ } ->
        (* clingo's closed-body [Afgerond] carries ONLY the money × 4-decimal-rate
           cent product ([money_mult_shape]: a Maal of a 2-decimal money by a
           4-decimal rate). A money DIVISION ([Afgerond] over a [Gedeeld_door]) or
           a money×integer rounding has no closed-body image — refuse those, or the
           fence lets a ref-only lowering slip (card clingo-bodymult, S18(c)). *)
        let dec e =
          match ir_typ e with Some (Numeriek { decimalen; _ }) -> Some decimalen | _ -> None
        in
        let rec money_rate (b : I.expr) =
          match b with
          | Let { body; _ } -> money_rate body
          | Binair { op = Maal; links = Some m; rechts = Some r; _ } ->
              dec m = Some 2 && dec r = Some 4
          | _ -> false
        in
        if money_rate basis then fence_expr basis
        else
          refuse
            "per-element callee %S wraps a rounding (Afgerond) that is not the money × 4-decimal-rate \
             cent product: clingo's closed-body term carries only that product (card clingo-bodymult) \
             — a money division or a money×integer rounding has no closed-body image, refused per \
             element"
            cname
    | Afgerond { basis = None; _ } -> ()
    | Begrensd { basis; minimum; maximum; _ } ->
        Option.iter fence_expr basis; Option.iter fence_expr minimum;
        Option.iter fence_expr maximum
    | Tekst_reeks { delen; _ } ->
        List.iter
          (function I.Ingevoegd { expr = Some x } -> fence_expr x | _ -> ())
          delen
    | Dimensie_cel { attribuut; _ } -> Option.iter fence_expr attribuut
    | Temporal_read { basis; datum; _ } -> fence_expr basis; fence_expr datum
    | Construct { argument; _ } -> Option.iter fence_expr argument
    | Match { scrutinee; arms; _ } ->
        fence_expr scrutinee; List.iter (fun (_, _, b) -> fence_expr b) arms
    | Struct_literal { velden; _ } -> List.iter (fun (_, v) -> fence_expr v) velden
    | Field_access { base; _ } -> fence_expr base
    | Let { value; body; _ } -> fence_expr value; fence_expr body
    | List_literal { elements; _ } -> List.iter fence_expr elements
    | Lambda { body; _ } -> fence_expr body
    | If_then_else { condition; then_; else_; _ } ->
        fence_cond condition; fence_expr then_; fence_expr else_
    | Invoke { arguments; _ } -> List.iter fence_expr arguments
    | Fold { list; functie; init; _ } ->
        fence_expr list; fence_fn functie; fence_expr init
    | Map_list { list; functie; _ } -> fence_expr list; fence_fn functie
    | Filter_list { list; condition; _ } -> fence_expr list; fence_cond condition
    | List_length { list; _ } -> fence_expr list
    (* the §5.7 instance-set union (IR-SPEC ch. 4.3, WF-110) reaches a body
       only through a count this frontend does not build; the arm is named
       rather than left to a leaf so that its arrival is a loud refusal *)
    | Instance_set_union _ ->
        refuse
          "instance-set union: the §5.7 concatenation counts object INSTANCES, which this \
           frontend's value fragment has no term for"
  and fence_fn = function Fn_lambda { body; _ } -> fence_expr body | Fn_name _ -> ()
  and fence_cond (c : I.conditie) : unit =
    match c with
    | Vergelijking { links; rechts; _ } ->
        Option.iter fence_expr links; Option.iter fence_expr rechts
    | Predicaat { operand; _ } -> Option.iter fence_expr operand
    | Periode_conditie { vanaf; tot; _ } ->
        Option.iter fence_expr vanaf; Option.iter fence_expr tot
    | Samengesteld { onderdelen; _ } -> List.iter fence_cond onderdelen
    (* the quantifier fold (IR-SPEC ch. 4.5) reaches this walk only through
       its two sub-conditions; this frontend never produces one, and the
       arm is here so that adding a condition node stays a compile error
       for the walks that must see it rather than a silent leaf *)
    | Kwantorvouw { kv_filter; kv_toets; _ } ->
        Option.iter fence_cond kv_filter; Option.iter fence_cond kv_toets
    | Kenmerk_check _ | Role_membership _
    | Regelversie _ | Conditie_onopgelost _ -> ()
  in
  fence_expr body;
  ( { I.fd_naam = fname; fd_parameters = params; fd_result = out_dt; fd_body = body;
      fd_bron = Some fname },
    { pem_fname = fname; pem_params = inputs; pem_result_ty = out_dt } )

(* --- driver ------------------------------------------------------------- *)

let file_to_document (input : string) : I.document =
  let options =
    Global.enforce_options ~input_src:(Global.FileName input) ~language:(Some `En)
      ~no_stdlib:true ()
  in
  let prog = Driver.Passes.scopelang options ~includes:[] ~stdlib:None in
  if not (ModuleName.Map.is_empty prog.Sc.program_modules) then
    refuse "imported modules are not supported in the 5a skeleton";
  let enums = prog.Sc.program_ctx.ctx_enums in
  let structs = prog.Sc.program_ctx.ctx_structs in
  let scopes = ScopeName.Map.bindings prog.Sc.program_scopes in
  if scopes = [] then refuse "no scope declaration found";
  used_enums := EnumName.Set.empty;
  used_structs := StructName.Set.empty;
  used_optional := None;
  let functions = topdef_signatures enums structs prog.Sc.program_topdefs in
  (* a rule variable name that produces a rule in more than one scope collides on
     the (globally unique) rule name — qualify those, and only those, by subject
     (PHASE5C-TUTORIAL: the tutorial's two [household_tax] outputs across the
     collective and per-individual scopes). *)
  let ambiguous =
    let per_scope = List.map (fun (_, decl) -> ruled_vnames_of (M.remove decl)) scopes in
    let all = List.concat per_scope in
    List.sort_uniq compare (List.filter (fun n -> List.length (List.filter (( = ) n) all) > 1) all)
  in
  (* besluit 6: per callee scope, the reentrant context variables that carry an
     own default — those, and only those, take a caller override as a generated
     highest default on the forced defaults-group base; a reentrant target
     without an own default keeps the groupless MemberInit route. *)
  let top_default_map =
    List.fold_left
      (fun acc (name, decl) ->
        let sd : untyped Sc.scope_decl = M.remove decl in
        let reentrant =
          ScopeVar.Map.fold
            (fun v (sv : Sc.scope_var_ty) a ->
              match M.remove sv.svar_io.Dio.io_input with
              | Catala_runtime.Reentrant -> ScopeVar.Set.add v a
              | _ -> a)
            sd.scope_sig ScopeVar.Set.empty
        in
        let with_default =
          List.fold_left
            (fun a rule ->
              match rule with
              | Sc.ScopeVarDefinition { var; e; _ } when ScopeVar.Set.mem (fst var) reentrant ->
                  (match parse_tree (strip e) with
                   | Some (_ :: _) -> ScopeVar.Set.add (fst var) a
                   | _ -> a)
              | _ -> a)
            ScopeVar.Set.empty sd.scope_decl_rules
        in
        ScopeName.Map.add name with_default acc)
      ScopeName.Map.empty scopes
  in
  let top_default_vars sc =
    try ScopeName.Map.find sc top_default_map with Not_found -> ScopeVar.Set.empty
  in
  (* row 2: per scope, the base names of its multi-state variables (a scopelang
     variable name carrying a '#'). A caller reading such a variable's bare
     struct-field output has no single callee attribute to bind — the narrowed
     multi-state refusal ([read_subscope_output]); a standalone state chain has
     no caller and lands. *)
  let multistate_map =
    List.fold_left
      (fun acc (name, decl) ->
        let sd : untyped Sc.scope_decl = M.remove decl in
        let bases =
          ScopeVar.Map.fold
            (fun v _ a ->
              let nm = M.remove (ScopeVar.get_info v) in
              match String.index_opt nm '#' with
              | Some i -> String.sub nm 0 i :: a
              | None -> a)
            sd.scope_sig []
        in
        ScopeName.Map.add name (List.sort_uniq compare bases) acc)
      ScopeName.Map.empty scopes
  in
  let callee_multistate sc =
    try ScopeName.Map.find sc multistate_map with Not_found -> []
  in
  (* the per-element functie-route (besluit 1-A, 2026-08-24; MULTI-OUTPUT +
     NESTED INVOKE 2026-08-25): a per-element (or fused) read of an output FIELD
     of a pure-function callee synthesises ONE FunctionDef per (callee, field),
     memoised here (keyed by scope name + field, so a multi-output callee yields
     one FunctionDef per read field and a callee referenced from several sites
     yields one) and appended to [function_defs] below. [per_element_call]
     recurses through a nested subscope-output read ([read_subscope_output]); the
     [in_progress] stack is the cycle guard (WF-067 is the acyclic-callgraph
     gate — a genuinely recursive callgraph refuses here rather than looping). *)
  let synth_fns : ((ScopeName.t * string) * (I.function_def * per_element_meta)) list ref =
    ref []
  in
  let in_progress : ScopeName.t list ref = ref [] in
  let key_eq (s1, f1) (s2, f2) = ScopeName.equal s1 s2 && String.equal f1 f2 in
  let rec per_element_call callee (field : StructField.t) : per_element_meta =
    let key = (callee, StructField.to_string field) in
    match List.find_opt (fun (k, _) -> key_eq k key) !synth_fns with
    | Some (_, (_, meta)) -> meta
    | None ->
        if List.exists (ScopeName.equal callee) !in_progress then
          refuse
            "the per-element callgraph is cyclic through %S: the invocation graph must be acyclic \
             (WF-067)"
            (ScopeName.base callee);
        let sd =
          match List.find_opt (fun (n, _) -> ScopeName.equal n callee) scopes with
          | Some (_, decl) -> M.remove decl
          | None -> refuse "per-element call to an unknown scope %S" (ScopeName.base callee)
        in
        in_progress := callee :: !in_progress;
        let fd, meta =
          scope_to_function_def enums structs functions ~per_element_call callee sd field
        in
        in_progress := List.filter (fun c -> not (ScopeName.equal c callee)) !in_progress;
        synth_fns := (key, (fd, meta)) :: !synth_fns;
        meta
  in
  let objecttypen = ref [] and regels = ref [] and defaults_groups = ref [] and cons = ref [] in
  let feittypen = ref [] and creaties = ref [] and overrides = ref [] in
  List.iter
    (fun (name, decl) ->
      let ot_name, ot, rs, gs, cs, fts, ocs, ovs =
        translate_scope enums structs functions ~ambiguous ~top_default_vars ~callee_multistate
          ~per_element_call name (M.remove decl)
      in
      objecttypen := (ot_name, ot) :: !objecttypen;
      regels := !regels @ rs;
      defaults_groups := !defaults_groups @ gs;
      cons := !cons @ cs;
      feittypen := !feittypen @ List.map (fun (ft : I.feittype) -> (Option.get ft.ft_naam, ft)) fts;
      creaties := !creaties @ ocs;
      overrides := !overrides @ ovs)
    scopes;
  (* prepend each caller override's highest defaults onto its callee's own
     defaults group (besluit 6): the highest defaults win (Reading B as default
     order). Every contribution must find its callee defaults group — the
     reentrant target's own default is forced to a defaults-group base above. *)
  List.iter
    (fun (ot, var, _) ->
      if not (List.exists
                (fun (g : I.prioritized_defaults) ->
                  g.pd_onderwerp = ot && g.pd_attribuut = Some var)
                !defaults_groups)
      then refuse "a context override on %s.%s found no callee defaults group (besluit 6)" ot var)
    !overrides;
  let defaults_groups =
    List.map
      (fun (g : I.prioritized_defaults) ->
        let extra =
          List.concat_map
            (fun (ot, var, ds) ->
              if g.pd_onderwerp = ot && g.pd_attribuut = Some var then ds else [])
            !overrides
        in
        if extra = [] then g else { g with defaults = extra @ g.defaults })
      !defaults_groups
  in
  let function_defs =
    topdef_functions enums structs functions prog.Sc.program_topdefs
    @ List.rev_map (fun (_, (fd, _)) -> fd) !synth_fns
  in
  (* the enums and structs actually referenced above become keuzetype/StructType
     domains, in declaration order — each enum on the variant [keuzetype_of]
     decided (SumType, or Enumeratie when all-nullary, WF-087); the reduce
     lowering's Optional (payload-carrying, built with the concrete element
     datatype) joins them when used. *)
  let domeinen =
    (match !used_optional with
     | Some elt -> [ (optional_domain, Some (optional_dt elt)) ]
     | None -> [])
    @ List.map
        (fun name -> (EnumName.base name, Some (keuzetype_of enums structs name)))
        (EnumName.Set.elements !used_enums)
    @ List.map
        (fun name -> (StructName.base name, Some (struct_type_of enums structs name)))
        (StructName.Set.elements !used_structs)
  in
  { objecttypen = List.rev !objecttypen;
    domeinen; parameters = []; eenheidstelsels = []; feittypen = !feittypen;
    dimensies = []; tijdlijnen = []; dagsoorten = [];
    resolved_regels = !regels; resolved_dagsoorten = [];
    object_creaties = !creaties; verdelingen = []; consistenties = !cons;
    uniciteiten = []; uniciteit_unies = []; feit_creaties = [];
    resolved_startpunten = []; regel_groepen = []; diagnostics = [];
    prioritized_defaults = defaults_groups; function_defs; presentatie_annotaties = [] }

let run ~input ~output = Emit.to_file output (file_to_document input)
