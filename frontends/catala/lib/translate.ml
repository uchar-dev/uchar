(* Catala scopelang -> regelspraak-ir-v0, the 5a skeleton.

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
   - integer                        -> Numeriek, geheel, 0 decimals
   - money                          -> Numeriek, 2 decimals (row 34: no money type)
   - date                           -> DatumInDagen
   - boolean                        -> Booleaan as a characteristic
   - decimal (rational)             -> REFUSED (no declared decimals)
   - single unconditional numeric definition -> a Berekening rule
   - single boolean definition      -> a Kenmerktoekenning (its value is the guard)
   - a chain (base + nested single exceptions) -> one attribute defaultLadder,
                                       rungs high-to-low
   - root-level unordered exceptions (no unconditional base) ->
                                       a declared-unordered ladder group
   - below-root branching (a chain exception carrying an unordered exception
     group) -> one ladder whose rung carries a nested [excepties] sub-group
     (row 5c, ch. 7.9: form-a rung / group-rung; WF-075)
   - a shared non-trivial base under all members of a root-level unordered
     group -> REFUSED (PHASE4-DEFAULTS §7; workable later by base-duplication)
   - context (reentrant) variable   -> REFUSED (see below; besluit 6 finding)
   - assertion                      -> consistency rule
   - enum (payload or nullary)      -> SumType + Construct/Match (row 36)
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
   context variable compiled as a TOP rung (Reading B: the caller's value
   beats the scope's own computation). In a single-scope 5a program the
   only override source is the case dossier, and RUNTIME-SEMANTICS ch. 7.8
   places the virtual input rung BELOW the ordinary rungs ("computation
   displaces input" — Reading A). So a ladder-target with case input gives
   Reading A, never Reading B; realising Reading B needs an actual
   caller-override top rung, which needs a subscope call — refused in 5a.
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

(* Enums referenced by the translated document, recorded as [sum_type_of]
   builds each SumType; [file_to_document] resets it per run and reads it to
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
   *universal* Present payload (Expr.option_enum_config), so [sum_type_of] on
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
   A Catala decimal is an exact rational; it lands as a bounded Numeriek at
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

(* Every Catala enum maps to the row-36 SumType, uniformly — including
   all-nullary enums (PHASE5B-KERNEL §2.7): Enumeratie stays the untouched
   v0 mirror form and the two never convert. The declaring enum's base name
   is the domain; constructor payloads recurse through [catala_dt]; a
   TLit TUnit payload is nullary (None); a TTuple payload is an n-ary
   constructor — 5c struct territory — and refuses loudly. A [TStruct] is the
   row-5c record datatype ([struct_type_of]). *)
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
  | TEnum name -> sum_type_of enums structs name
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

(* An enum -> its SumType datatype: constructors in declaration order
   (EnumConstructor ids are generative, so the map folds declaration-first). *)
and sum_type_of (enums : enum_ctx) (structs : struct_ctx) (name : EnumName.t) : I.datatype =
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
  Sum_type { domein = EnumName.base name; constructoren }

(* A struct -> its StructType datatype: fields in declaration order
   (StructField ids are generative, so the map folds declaration-first). Field
   datatypes recurse through [catala_dt] and must be WF-071-admissible
   (Numeriek / DatumInDagen / SumType / StructType) — an inadmissible field
   type (Tekst, Booleaan, Enumeratie, DatumTijd, a list) refuses loudly. *)
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

(* the declared datatype riding an already-built IR expression — the mirror
   of the verifier's [typ_of_expr] (checks.ml). Used to set a [Let]'s type to
   its body's type (WF-076) and an [IfThenElse]'s type to its then-branch
   (WF-069), by construction rather than by re-inference. *)
let ir_typ (e : I.expr) : I.datatype option =
  match e with
  | Literal { typ; _ } | Tekst_reeks { typ; _ } | Binair { typ; _ }
  | Functie { typ; _ } | Afgerond { typ; _ } | Begrensd { typ; _ }
  | Attribuut_ref { typ; _ } | Dimensie_cel { typ; _ } | Parameter_ref { typ; _ }
  | Variabele_ref { typ; _ } | Constante { typ; _ } | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ } | Construct { typ; _ } | Match { typ; _ }
  | Binder_ref { typ; _ } | Struct_literal { typ; _ } | Field_access { typ; _ }
  | List_literal { typ; _ } | If_then_else { typ; _ } | Let { typ; _ }
  | Invoke { typ; _ } | Fold { typ; _ } | Map_list { typ; _ }
  | Filter_list { typ; _ } | List_length { typ; _ } -> typ
  | Lambda _ -> None

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
   [IfThenElse] is admissible (a lambda, function or strict [Let] body), its
   condition may READ only binders, parameters and literals; arithmetic
   combinators over those are admissible. An attribute/field/characteristic/
   aggregation/temporal read as (or inside) a condition operand must be hoisted
   through a strict [let … in] binding first — the leeg-barrier. Enforced here
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
         through a strict `let … in` first and compare the binder — a condition may read only \
         binders, parameters and literals (the condition-operand discipline, the leeg-barrier)"
        (M.remove (ScopeVar.get_info (M.remove name)))
  | ELocation _ ->
      refuse
        "WF-077: an if-then-else condition may read only binders, parameters and literals; hoist \
         this read through a strict `let … in` binding first"
  | EStructAccess { field; _ } ->
      refuse
        "WF-077: the condition of an if-then-else reads the struct field %S directly; hoist it \
         through a strict `let … in` binding first (a condition reads only binders, parameters and \
         literals)"
        (StructField.to_string field)
  | _ ->
      refuse
        "WF-077: an if-then-else condition admits only binders, parameters, literals and arithmetic \
         over those; hoist any other read through a strict `let … in` binding first"

(* --- the default tree, parsed into a rung tree (PHASE5C §4, ch. 7.9) -----

   The two flat parsers (a linear chain, a flat root-unordered group) become
   one recursive walk over the scopelang default tree. A [prung] carries the
   raw Catala (justification, consequence) exprs; [translate_def] lowers it to
   [I.rung] once [value_expr]/[cond_rung] are in scope. A chain is highest
   priority first, mirroring the ladder wire order. The three shapes:
   - [Plain (just, cons)]      — an ordinary rung.
   - [Form_a (just, cons, g)]  — own content PLUS a nested exception sub-group
                                 [g] (the sub-group decides first, the own
                                 consequence is the fall-back — ch. 7.9 form a).
   - [Group_rung g]            — a pure group-rung: no own content, only the
                                 sub-group (a multi-member base/exception group
                                 — ch. 7.9 form b).
   Each sub-group member is itself a chain ([prung list]). *)
type 'e prung =
  | Plain of 'e * 'e
  | Form_a of 'e * 'e * 'e pgroup
  | Group_rung of 'e pgroup
and 'e pgroup = 'e prung list list

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

(* the recursive walk: a default subtree -> a rung chain (highest first), or
   None when the shape is outside the fragment (a loud refusal upstream). *)
let rec parse_tree e : 'e prung list option =
  match default_kind e with
  | `Leaf (just, cons) -> Some [ Plain (just, cons) ]
  | `Group [ single ] -> parse_tree single (* a single-def group unwraps *)
  | `Group members -> Option.map (fun g -> [ Group_rung g ]) (parse_group members)
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
   stacks above (strict priority — ordered rungs); an unordered group (>=2
   alternatives) nests into the head rung's exception slot (form a — flattening
   it to ordered rungs would suppress the very conflict the paper raises, the
   spurious-conflict argument of PHASE4-DEFAULTS §7). *)
and attach excepts lower =
  match excepts with
  | [] -> Some lower
  | [ single ] -> (
      match default_kind single with
      | `Group members when List.length members >= 2 -> nest members lower
      | _ -> ( match parse_tree single with Some hi -> Some (hi @ lower) | None -> None))
  | _ -> nest excepts lower

(* nest an unordered exception group as the [Form_a] sub-group of the rung it
   excepts. Two shapes stay refused (recorded, PHASE4-DEFAULTS §7 — the
   sub-root refusal narrows to exactly these):
   - excepting a group-rung (the rung it excepts is itself a multi-member
     group), and
   - excepting the UNCONDITIONAL base directly (a shared non-trivial base
     under all members — the root-level limitation, workable later by
     base-duplication under the equal-decisions rule).
   An unordered group only nests onto an ordinary conditional exception rung. *)
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

(* does the tree carry an always-deciding base (the lowest rung)? Either an
   unconditional base rung, or a two-member base GROUP whose members split the
   domain exhaustively (the date-complementary [article_2] shape: old [< 2000]
   / new [>= 2000] with the [false ⊢ ∅] fallback — total, never empty). Recurses
   through the nested form — used for the operand-fill analysis. *)
let base_is_filled e =
  match parse_tree (strip e) with
  | None -> false
  | Some rungs ->
      List.exists
        (function
          | Plain (j, _) | Form_a (j, _, _) -> is_true j
          | Group_rung [ [ Plain (j1, _) ]; [ Plain (j2, _) ] ] -> complementary_just j1 j2
          | Group_rung _ -> false)
        rungs

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
    ~(check_subscope_output : ScopeVar.t -> StructField.t -> I.conditie option) =
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
    | EInj { name; _ } -> Some (sum_type_of enums structs name)
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
      ?(env = Var.Map.empty) e : I.expr =
    (* [in_body]: a closed lambda/function body — scope reads refuse (WF-067),
       IfThenElse admissible. [in_let]: a strict Let body — scope reads are
       allowed (a Let body is NOT closed), IfThenElse admissible (WF-069, the
       5c widening). IfThenElse is admissible iff [in_body || in_let]. *)
    let e = strip e in
    match M.remove e with
    | ELit l -> literal_expr l
    | EVar v -> (
        (* a bound binder (match-arm payload, lambda/function parameter) ->
           BinderRef (RUNTIME-SEMANTICS ch. 9.2/10.2); deliberately distinct
           from VariabeleRef (row 24) *)
        match Var.Map.find_opt v env with
        | Some (nm, dt) -> Binder_ref { naam = nm; typ = Some dt; bron = Some nm }
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
        | Op.Map, [ f; lst ] ->
            let vars, tys, body = lambda_of f in
            if Array.length vars <> 1 then refuse "a map lambda takes exactly one parameter";
            let pn = Bindlib.name_of vars.(0) and pt = catala_dt enums structs (List.nth tys 0) in
            let env' = Var.Map.add vars.(0) (pn, pt) env in
            let out_elt =
              match infer_dt ~env:env' body with
              | Some d -> d
              | None -> refuse "cannot infer the element datatype of a map body"
            in
            Map_list
              { typ = Some (List_type { element = out_elt });
                list = value_expr ~in_body ~in_let ~env lst;
                functie = Fn_lambda { parameters = [ (pn, pt) ];
                                      body = value_expr ~in_body:true ~in_let:false ~env:env' body; bron = Some pn };
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
            (* the strict Let (IR-SPEC ch. 4.10): Catala lowers [let x = v in
               body] to [EApp{EAbs[x], [v]}], a chain nesting one per binding.
               Each binder joins the WF-064 regime for its body; the value is
               evaluated in the enclosing position (the binder is NOT visible in
               its own value, WF-076), the body in a Let position where
               IfThenElse widens in (WF-069). A leeg value propagates strictly.
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
        | EScopeCall _ ->
            (* an inline (anonymous) subscope call — [(output of S with {…}).f],
               the tutorial-2-1 and the per-element 2-4 shape: an anonymous
               instance per occurrence, which the creation-per-subject key
               cannot carry. Only DECLARED subscopes are lowered (§3). Refused. *)
            refuse "inline subscope call (reading a subscope output of an anonymous instance): only declared subscopes lower to object creation + role navigation (PHASE5C-TUTORIAL §3); per-element/inline calls are out of scope"
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
        (* a constructor value; unit payload = nullary (argument None) *)
        let sig_ = EnumName.Map.find name enums in
        let payload_ty = EnumConstructor.Map.find cons sig_ in
        let argument =
          match M.remove payload_ty with
          | TLit TUnit -> None
          | _ -> Some (value_expr ~in_body ~in_let ~env payload)
        in
        Construct
          { typ = Some (sum_type_of enums structs name);
            constructor_ = EnumConstructor.to_string cons; argument;
            bron = Some (EnumConstructor.to_string cons) }
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
        let absent_body = some_of (Binder_ref { naam = xn; typ = Some elt; bron = Some xn }) in
        let step : I.expr =
          Match
            { typ = Some opt;
              scrutinee = Binder_ref { naam = acc_name; typ = Some opt; bron = Some acc_name };
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
           unit binder, which becomes binder: null) *)
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
            "if-then-else in a rule expression: a value-level conditional is admissible only inside a lambda, function or strict let body (WF-069); a rule keeps the ladder/voorwaarde idiom (bind through a `let … in` to carry it)";
        (* WF-077, the condition-operand discipline: a condition reads only
           binders, parameters and literals — an attribute/field read must be
           hoisted through a strict let first (checked before translation) *)
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
    | ELit (LBool false) -> refuse "a literal-false condition is not expressible as a rung justification"
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
   variable name that produces a rule in more than one scope, so its rule/ladder
   name is qualified by subject and stays globally unique (WF-020). A state
   step's rule name is the [state_attr_name]-mangled declared name (that is what
   [rule_label] compares against [ambiguous]), so mangle here too — otherwise a
   state variable shared across scopes never matches and collides on WF-020. *)
let ruled_vnames_of (sd : untyped Sc.scope_decl) : string list =
  List.sort_uniq compare
    (List.filter_map
       (function
         | Sc.ScopeVarDefinition { var; e; io; _ } -> (
             match M.remove (strip e) with
             | EScopeCall _ -> None
             | _ -> (
                 match M.remove io.Dio.io_input with
                 | Catala_runtime.OnlyInput -> None
                 | _ -> Some (state_attr_name (M.remove (ScopeVar.get_info (fst var))))))
         | Sc.Assertion _ -> None)
       sd.scope_decl_rules)

(* one scope -> its object type, rules, ladders, consistencies. [ambiguous] is
   the set of rule variable names that recur across scopes (rule names for them
   are qualified by subject). *)
let translate_scope (enums : enum_ctx) (structs : struct_ctx) (functions : (string * I.datatype) list)
    ~(ambiguous : string list) ~(top_rung_vars : ScopeName.t -> ScopeVar.Set.t)
    ~(callee_multistate : ScopeName.t -> string list)
    (name : ScopeName.t) (sd : untyped Sc.scope_decl) =
  let ot_name = ScopeName.base name in
  (* a rule/ladder name for [vname]: qualified by subject when [vname] produces a
     rule in more than one scope, bare otherwise (existing single-scope names
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
        if is_subscope v then acc
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
             a ladder, and a caller override rides above it as a generated top
             rung ([build_subscope] below; the merge is in [file_to_document]).
             The io_input is consulted through [top_rung_vars]. *)
          ScopeVar.Map.add v (vname, catala_dt enums structs sv.svar_out_ty) acc
        end)
      sd.scope_sig ScopeVar.Map.empty
  in
  let lookup v = ScopeVar.Map.find v vars in
  (* besluit 6: the reentrant variables of THIS scope that carry an own
     default — their own default becomes a ladder base (never a bare
     Berekening/Kenmerktoekenning, so a caller top rung can prepend, ch. 7.9),
     and a caller override lands as a generated top rung on that ladder. A
     reentrant variable without an own default keeps the ladderless MemberInit
     route (there is no own ladder to sit above). *)
  let is_reentrant_top v = ScopeVar.Set.mem v (top_rung_vars name) in
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
        let callee_role = subscope_callee_role v in
        let bron = Printf.sprintf "de %s van de %s" fld_name (M.remove (ScopeVar.get_info v)) in
        Some
          (Attribuut_ref
             { attribuut = Some fld_name; objecttype = Some callee_type; onderwerp_bron = Some bron;
               navigatie_rol = Some callee_role; typ = Some fld_ty; bron = Some bron; set = false;
               navigatie_pad = [];
               navigatie_bindingen =
                 Some [ { rb_feittype = subscope_feittype v; rb_rol = callee_role } ] })
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
             { kenmerk = Some fld_name; navigatie = Some callee_role; negatie = false;
               eigenaar = None; periode = None; bron = Some bron;
               navigatie_binding = Some { I.rb_feittype = subscope_feittype v; rb_rol = callee_role } })
  in
  (* variables that are always filled: inputs (the case dossier fulfils
     Catala's input contract) or variables carrying an unconditional rung
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
     as a top rung on the callee ladder (besluit 6): the rung lives on the callee
     (the ladder subject), so its caller reads navigate callee -> caller. *)
  let attr_ref ?nav vname dt : I.expr =
    let navigatie_rol, navigatie_bindingen =
      match nav with
      | None -> (None, None)
      | Some (ft, rol) -> (Some rol, Some [ { I.rb_feittype = ft; rb_rol = rol } ])
    in
    Attribuut_ref
      { attribuut = Some vname; objecttype = Some ot_name;
        onderwerp_bron = Some ("de " ^ vname); navigatie_rol;
        typ = Some dt; bron = Some ("de " ^ vname); set = false;
        navigatie_pad = []; navigatie_bindingen }
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
      { kenmerk = Some k; navigatie = None; negatie = false;
        eigenaar = None; periode = None; bron = Some k; navigatie_binding = None }
  in
  let lookup_var v = try Some (snd (lookup v)) with Not_found -> None in
  let value_expr, cond_expr =
    make_exprs ~enums ~structs ~functions ~read_var ~check_var ~lookup_var ~read_subscope_output
      ~check_subscope_output
  in
  (* besluit 6: the value/condition walk that lowers a caller override into a
     top rung on the callee ladder. Same walk as the scope's own, but every
     caller read navigates back over the back-link (feittype, caller role):
     the override rung sits on the callee's ladder yet reads the caller's
     override value and guard — Reading B expressed as rung order, no standing
     machinery. *)
  let override_exprs ~feittype ~rol =
    make_exprs ~enums ~structs ~functions
      ~read_var:(fun ~operand ~in_body name ->
        let vname, dt = scalar_read ~operand ~in_body name in
        attr_ref ~nav:(feittype, rol) vname dt)
      ~check_var:(fun ~in_body name ->
        let k = kenmerk_read ~in_body name in
        Kenmerk_check
          { kenmerk = Some k; navigatie = Some rol; negatie = false; eigenaar = None;
            periode = None; bron = Some k;
            navigatie_binding = Some { I.rb_feittype = feittype; rb_rol = rol } })
      ~lookup_var ~read_subscope_output ~check_subscope_output
  in
  (* condition option: [true] is unconditional -> None (for plain rules) *)
  let cond_opt e = if is_true e then None else Some (cond_expr e) in
  (* a ladder rung always carries a justification (WF-050): an
     unconditional base becomes the vacuously-true ALLE, never null —
     null is reserved for the initialisation rung (ch. 7.3). *)
  let cond_rung e = Some (if is_true e then always_true else cond_expr e) in
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
  let regels = ref [] and ladders = ref [] and consistenties = ref [] in
  let feittypen = ref [] and creaties = ref [] in
  (* besluit 6: caller override top rungs, keyed by (callee object type,
     callee attribute); [file_to_document] prepends them onto that callee's
     own ladder once every scope is translated. *)
  let override_contribs : (string * string * I.rung list) list ref = ref [] in
  let add_regel r = regels := r :: !regels in
  let add_ladder l = ladders := l :: !ladders in
  let berekening vname dt value ~cond : I.resolved_regel =
    { rr_naam = Some ("berekening " ^ rule_label vname); rr_soort = Some Berekening;
      doel = Some (attr_ref vname dt);
      waarde = Some (value_expr value); rr_voorwaarde = cond; rr_geldigheid = Some geldig }
  in
  let kenmerktoekenning vname ~voorwaarde : I.resolved_regel =
    { rr_naam = Some ("kenmerktoekenning " ^ rule_label vname); rr_soort = Some Kenmerktoekenning;
      doel = Some (attr_ref vname Booleaan);
      waarde = Some (Literal { waarde = Some "waar"; typ = Some Booleaan; bron = Some "waar" });
      rr_voorwaarde = voorwaarde; rr_geldigheid = Some geldig }
  in
  (* dispatch one variable definition *)
  let translate_def v e =
    let vname, dt = lookup v in
    let is_bool = is_bool_dt dt in
    let bool_besluit value =
      match M.remove (strip value) with
      | ELit (LBool b) -> b
      | _ -> refuse "a characteristic ladder rung value must be a literal true/false"
    in
    (* lower a parsed rung tree to IR rungs (ch. 7.9): a plain rung, a form-a
       rung (own content plus [excepties]), or a pure group-rung (no own
       content). A sub-ladder inherits the root target kind (attribute vs
       characteristic), so [is_bool] carries through the recursion. *)
    let rec build_rung (pr : _ prung) : I.rung =
      match pr with
      | Plain (cond, value) -> content_rung cond value None
      | Form_a (cond, value, g) -> content_rung cond value (Some (build_group g))
      | Group_rung g ->
          { rung_besluit = None; rung_voorwaarde = None; rung_consequentie = None;
            rung_initialisatie = false; rung_excepties = Some (build_group g);
            rung_bron = Some vname }
    and content_rung cond value excepties : I.rung =
      if is_bool then
        { rung_besluit = Some (bool_besluit value); rung_voorwaarde = cond_rung cond;
          rung_consequentie = None; rung_initialisatie = false;
          rung_excepties = excepties; rung_bron = Some vname }
      else
        { rung_besluit = None; rung_voorwaarde = cond_rung cond;
          rung_consequentie = Some (value_expr value); rung_initialisatie = false;
          rung_excepties = excepties; rung_bron = Some vname }
    and build_group (g : _ pgroup) : I.sub_group =
      { sg_ongeordend = List.length g >= 2;
        sg_leden =
          List.map
            (fun member : I.sub_ladder ->
              { sl_rungs = List.map build_rung member; sl_bron = Some vname })
            g }
    in
    let build_ladder ?(ongeordend = false) suffix (chain : _ prung list) : I.default_ladder =
      { dl_naam = Some (rule_label vname ^ suffix); dl_onderwerp = ot_name;
        dl_kenmerk = (if is_bool then Some vname else None);
        dl_attribuut = (if is_bool then None else Some vname);
        rungs = List.map build_rung chain;
        dl_ongeordend = ongeordend; dl_bron = Some vname; dl_geldigheid = Some geldig }
    in
    (* besluit 6: a reentrant variable's own default must be a LADDER (never a
       bare Berekening), so a caller override can ride above it as a top rung.
       The attribute-target chain own default is covered; a characteristic
       context target and a root-unordered own default are recorded flanks. *)
    if is_reentrant_top v && is_bool then
      refuse
        "context (reentrant) characteristic %S: a caller boolean override is a recorded besluit-6 \
         flank (the generated top rung covers attribute targets — a characteristic top rung is not \
         wired); refused"
        vname;
    match default_kind (strip e) with
    | `Group members when List.length members >= 2 ->
        if is_reentrant_top v then
          refuse
            "context (reentrant) variable %S with a root-level unordered own default: a caller top \
             rung above an unordered group is a recorded besluit-6 flank (a chain own default is \
             covered); refused"
            vname;
        (* a root-level unordered group -> the declared-unordered ladder group:
           one [ongeordend] ladder per member (ch. 7.2, unchanged) — distinct
           from a NESTED unordered group, which rides [excepties] on one rung *)
        List.iteri
          (fun i m ->
            let suffix = " #" ^ string_of_int (i + 1) in
            match parse_tree m with
            | Some chain -> add_ladder (build_ladder ~ongeordend:true suffix chain)
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
            (* a single plain rung is a bare rule, not a ladder — unless the
               target is a reentrant context variable, whose own default must be
               a ladder base for the caller top rung to sit above (besluit 6) *)
            if is_bool then
              add_regel
                (kenmerktoekenning vname
                   ~voorwaarde:(combine (cond_opt cond) (bool_value_guard value)))
            else if is_true cond && not (is_reentrant_top v) then
              add_regel (berekening vname dt value ~cond:None)
            else add_ladder (build_ladder "" [ Plain (cond, value) ])
        | Some chain -> add_ladder (build_ladder "" chain))
  in
  (* one declared subscope -> its synthesised fact type and object creation
     (PHASE5C-TUTORIAL §3.1): the call's args bind as member initialisations
     (Toekenningen) on the callee's INPUT attributes, plus the verbindendeRol
     back-link. A subscope input is an unconditional caller value; a conditional
     or laddered target is out of scope (the WF-050 rung route is deferred). *)
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
       carrying an own default — those args become top rungs, not member
       initialisations. *)
    let ovr_set = top_rung_vars scope in
    let ve_ov, ce_ov = override_exprs ~feittype ~rol:caller_role in
    (* a subscope input definition carries the ordinary scope-variable default
       shape [error_empty ⟨ ⟨true ⊢ ⟨X⟩⟩ | false ⊢ ∅ ⟩] — one unconditional
       plain rung. Anything richer (a conditional base or a ladder) is a rung
       target, which the WF-050 route (deferred) would carry, not a member
       initialisation. *)
    let arg_value e =
      match parse_tree (strip e) with
      | Some [ Plain (just, cons) ] when is_true just -> value_expr cons
      | _ ->
          refuse
            "a subscope input to %S is not an unconditional value: conditional/laddered subscope \
             inputs enter as a rung, never a member initialisation (the WF-050 route is deferred, \
             PHASE5C-TUTORIAL §3) — refused"
            instance_var
    in
    (* besluit 6: a caller override of a reentrant callee variable that carries
       an own default (in [ovr_set]) does NOT bind as a member initialisation —
       it becomes a generated top rung above the callee's own ladder (Reading B
       as rung order, the §4.1 hardship-pattern shape; the WF-050 constraint).
       Its guard and value read the caller, navigated over the back-link
       (callee -> caller); no standing machinery. *)
    let override_rung callee_var (pr : _ prung) : I.rung =
      match pr with
      | Plain (just, value) ->
          { I.rung_besluit = None;
            rung_voorwaarde = Some (if is_true just then always_true else ce_ov just);
            rung_consequentie = Some (ve_ov value);
            rung_initialisatie = false; rung_excepties = None;
            rung_bron = Some ("context-override " ^ callee_var) }
      | Form_a _ | Group_rung _ ->
          refuse
            "a caller override of the context variable %S is a branching tree (a nested or \
             root-unordered override): only a chain of plain-rung overrides lowers to top rungs \
             (besluit 6 flank); refused"
            callee_var
    in
    ScopeVar.Map.iter
      (fun k (_pos, argexpr) ->
        if ScopeVar.Set.mem k ovr_set then
          let callee_var = M.remove (ScopeVar.get_info k) in
          match parse_tree (strip argexpr) with
          | Some chain ->
              override_contribs :=
                (callee_type, callee_var, List.map (override_rung callee_var) chain)
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
          | None -> (
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
  ( ot_name, objecttype, List.rev !regels, List.rev !ladders, List.rev !consistenties,
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
     top rung on the forced ladder base; a reentrant target without an own
     default keeps the ladderless MemberInit route. *)
  let top_rung_map =
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
  let top_rung_vars sc =
    try ScopeName.Map.find sc top_rung_map with Not_found -> ScopeVar.Set.empty
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
  let objecttypen = ref [] and regels = ref [] and ladders = ref [] and cons = ref [] in
  let feittypen = ref [] and creaties = ref [] and overrides = ref [] in
  List.iter
    (fun (name, decl) ->
      let ot_name, ot, rs, ls, cs, fts, ocs, ovs =
        translate_scope enums structs functions ~ambiguous ~top_rung_vars ~callee_multistate name
          (M.remove decl)
      in
      objecttypen := (ot_name, ot) :: !objecttypen;
      regels := !regels @ rs;
      ladders := !ladders @ ls;
      cons := !cons @ cs;
      feittypen := !feittypen @ List.map (fun (ft : I.feittype) -> (Option.get ft.ft_naam, ft)) fts;
      creaties := !creaties @ ocs;
      overrides := !overrides @ ovs)
    scopes;
  (* prepend each caller override's top rungs onto its callee's own ladder
     (besluit 6): the highest rungs win (Reading B as rung order). Every
     contribution must find its callee ladder — the reentrant target's own
     default is forced to a ladder base above. *)
  List.iter
    (fun (ot, var, _) ->
      if not (List.exists
                (fun (l : I.default_ladder) -> l.dl_onderwerp = ot && l.dl_attribuut = Some var)
                !ladders)
      then refuse "a context override on %s.%s found no callee ladder (besluit 6)" ot var)
    !overrides;
  let ladders =
    List.map
      (fun (l : I.default_ladder) ->
        let extra =
          List.concat_map
            (fun (ot, var, rungs) ->
              if l.dl_onderwerp = ot && l.dl_attribuut = Some var then rungs else [])
            !overrides
        in
        if extra = [] then l else { l with rungs = extra @ l.rungs })
      !ladders
  in
  let function_defs = topdef_functions enums structs functions prog.Sc.program_topdefs in
  (* the enums and structs actually referenced above become SumType/StructType
     domains, in declaration order (PHASE5B-KERNEL §2.7); the reduce lowering's
     Optional (built with the concrete element datatype) joins them when used. *)
  let domeinen =
    (match !used_optional with
     | Some elt -> [ (optional_domain, Some (optional_dt elt)) ]
     | None -> [])
    @ List.map
        (fun name -> (EnumName.base name, Some (sum_type_of enums structs name)))
        (EnumName.Set.elements !used_enums)
    @ List.map
        (fun name -> (StructName.base name, Some (struct_type_of enums structs name)))
        (StructName.Set.elements !used_structs)
  in
  { objecttypen = List.rev !objecttypen;
    domeinen; parameters = []; eenheidstelsels = []; feittypen = !feittypen;
    dimensies = []; tijdlijnen = []; dagsoorten = []; dagsoort_per_eenheid = [];
    regels = []; resolved_regels = !regels; resolved_dagsoorten = [];
    object_creaties = !creaties; verdelingen = []; consistenties = !cons;
    uniciteiten = []; uniciteit_unies = []; feit_creaties = [];
    resolved_startpunten = []; regel_groepen = []; diagnostics = [];
    default_ladders = ladders; function_defs }

let run ~input ~output = Emit.to_file output (file_to_document input)
