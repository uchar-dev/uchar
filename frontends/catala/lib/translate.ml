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
   - sub-root branching / base-plus-unordered -> REFUSED (besluit 4, §5.1)
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
  | TLit TRat -> refuse "decimal (rational) type: refused in the 5a skeleton (no declared number of decimals to derive)"
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

let literal_expr (l : lit) : I.expr =
  match l with
  | LBool b ->
      let w = if b then "waar" else "onwaar" in
      Literal { waarde = Some w; typ = Some Booleaan; bron = Some w }
  | LInt z -> Literal { waarde = Some (Z.to_string z); typ = Some num_int; bron = Some (Z.to_string z) }
  | LMoney z -> Literal { waarde = Some (money_str z); typ = Some num_money; bron = Some (money_str z) }
  | LDate d -> Literal { waarde = Some (date_str d); typ = Some Datum_in_dagen; bron = Some (date_str d) }
  | LRat _ -> refuse "decimal (rational) literal: refused in the 5a skeleton"
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

(* a single default: EDefault{excepts=[]; just; cons} -> (just, cons) *)
let as_def e =
  match M.remove (strip e) with
  | EDefault { excepts = []; just; cons } -> Some (just, cons)
  | _ -> None

(* pure chain (no branching anywhere): priority-ordered rungs high->low *)
let rec parse_chain e =
  let e = strip e in
  match M.remove e with
  | EDefault { excepts; just = _; cons } when is_empty cons ->
      (* a definitions group (Leaf): its defs live in [excepts] *)
      (match excepts with
       | [ d ] -> (match as_def d with Some (j, c) -> Some [ (j, c) ] | None -> None)
       | _ -> None)
  | EDefault { excepts = [ exc ]; just = _; cons } ->
      (* a Node with exactly one exception: exc is higher priority, cons the base *)
      (match parse_chain exc, parse_chain cons with
       | Some hi, Some lo -> Some (hi @ lo)
       | _ -> None)
  | _ -> ( match as_def e with Some (j, c) -> Some [ (j, c) ] | None -> None )

(* flat root-level unordered group: a Leaf with >=2 simple defs and no base *)
let parse_root_unordered e =
  match M.remove (strip e) with
  | EDefault { excepts; just = _; cons } when is_empty cons -> (
      match excepts with
      | _ :: _ :: _ -> (
          try
            Some
              (List.map
                 (fun d -> match as_def d with Some jc -> jc | None -> raise Exit)
                 excepts)
          with Exit -> None)
      | _ -> None)
  | _ -> None

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
    ~(lookup_var : ScopeVar.t -> I.datatype option) =
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
        | _ -> infer_dt ~env a)
    | _ -> None
  in
  let rec value_expr ?(operand = false) ?(in_body = false) ?(env = Var.Map.empty) e : I.expr =
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
          Binair { op = bop; links = Some (value_expr ~operand:true ~in_body ~env a);
                   rechts = Some (value_expr ~operand:true ~in_body ~env b);
                   typ = infer_dt ~env e; bron = None }
        in
        match M.remove op, args with
        | (Op.Add_int_int | Op.Add_mon_mon | Op.Add_dur_dur), [ a; b ] -> bin Plus a b
        | (Op.Sub_int_int | Op.Sub_mon_mon | Op.Sub_dur_dur), [ a; b ] -> bin Verminderd_met a b
        | (Op.Mult_int_int | Op.Mult_mon_int | Op.Mult_mon_rat | Op.Mult_dur_int), [ a; b ] -> bin Maal a b
        | (Op.Div_int_int | Op.Div_dur_dur), [ a; b ] -> bin Gedeeld_door a b
        | (Op.Div_mon_int | Op.Div_mon_rat), [ a; b ] ->
            (* Catala rounds money division to cents (arithmetic / nearest);
               wrap the quotient so the IR rounds identically instead of
               faulting on the remainder *)
            Afgerond
              { basis = Some (bin Gedeeld_door a b); modus = Rekenkundig;
                decimalen = 2; typ = Some num_money; bron = None }
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
                list = value_expr ~in_body ~env lst;
                functie = Fn_lambda { parameters = [ (pn, pt) ];
                                      body = value_expr ~in_body:true ~env:env' body; bron = Some pn };
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
              { typ = Some src_ty; list = value_expr ~in_body ~env lst; binder = pn;
                condition = cond_expr ~in_body ~env:env' body; bron = None }
        | Op.Fold, [ f; init; lst ] ->
            let vars, tys, body = lambda_of f in
            if Array.length vars <> 2 then refuse "a fold lambda takes exactly two parameters";
            let an = Bindlib.name_of vars.(0) and at = catala_dt enums structs (List.nth tys 0) in
            let xn = Bindlib.name_of vars.(1) and xt = catala_dt enums structs (List.nth tys 1) in
            let env' = Var.Map.add vars.(1) (xn, xt) (Var.Map.add vars.(0) (an, at) env) in
            Fold
              { typ = Some at; list = value_expr ~in_body ~env lst;
                functie = Fn_lambda { parameters = [ (an, at); (xn, xt) ];
                                      body = value_expr ~in_body:true ~env:env' body; bron = None };
                init = value_expr ~in_body ~env init; bron = None }
        | Op.Length, [ lst ] ->
            List_length { typ = Some num_int; list = value_expr ~in_body ~env lst; bron = None }
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
        | ELocation (ToplevelVar { name; _ }) ->
            let fname = TopdefName.base (M.remove name) in
            let rty =
              match List.assoc_opt fname functions with
              | Some d -> d
              | None -> refuse "invocation of an unknown top-level function %S" fname
            in
            Invoke
              { typ = Some rty; functie = fname;
                arguments = List.map (value_expr ~operand:true ~in_body ~env) args; bron = Some fname }
        | _ -> refuse "application of a non-top-level expression is not supported")
    | EScopeCall _ ->
        refuse "subscope call (EScopeCall): object creation + role navigation (mapping row 9) is out of 5a scope"
    | EStructAccess { name; e = inner; field } -> (
        match M.remove (strip inner) with
        | EScopeCall _ ->
            (* reading a subscope output: still object creation + role
               navigation (mapping row 9), refused until kaart 4 *)
            refuse "subscope call (reading a subscope output): object creation + role navigation (mapping row 9) is out of 5a scope"
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
              { typ = Some veld_ty; base = value_expr ~operand ~in_body ~env inner;
                veld = StructField.to_string field; bron = None })
    | EInj { name; e = payload; cons } ->
        (* a constructor value; unit payload = nullary (argument None) *)
        let sig_ = EnumName.Map.find name enums in
        let payload_ty = EnumConstructor.Map.find cons sig_ in
        let argument =
          match M.remove payload_ty with
          | TLit TUnit -> None
          | _ -> Some (value_expr ~in_body ~env payload)
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
        let present_body = some_of (value_expr ~in_body:true ~env:env_step rbody) in
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
            { typ = Some opt; list = value_expr ~in_body ~env lst;
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
              value_expr ~in_body ~env b
          | _ -> value_expr ~in_body ~env none_case
        in
        let sbinder, s_body =
          match M.remove some_case with
          | EAbs { binder; _ } ->
              let vs, b = Bindlib.unmbind binder in
              let n = Bindlib.name_of vs.(0) in
              (n, value_expr ~in_body ~env:(Var.Map.add vs.(0) (n, elt) env) b)
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
              ( (EnumConstructor.to_string c, binder_name, value_expr ~in_body ~env:env' body)
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
          { typ = Some result_dt; scrutinee = value_expr ~in_body ~env scrut;
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
                elements = List.map (value_expr ~in_body ~env) elts; bron = None })
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
                 (StructField.to_string fld, value_expr ~in_body ~env v) :: acc)
               fields [])
        in
        Struct_literal { typ = Some (struct_type_of enums structs name); velden; bron = None }
    | EIfThenElse { cond; etrue; efalse } ->
        if not in_body then
          refuse
            "if-then-else in a rule expression: a value-level conditional is admissible only inside a lambda or function body (WF-069); a rule keeps the ladder/voorwaarde idiom";
        If_then_else
          { typ = infer_dt ~env etrue; condition = cond_expr ~in_body ~env cond;
            then_ = value_expr ~in_body ~env etrue; else_ = value_expr ~in_body ~env efalse;
            bron = None }
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
  and cond_expr ?(in_body = false) ?(env = Var.Map.empty) e : I.conditie =
    let e = strip e in
    match M.remove e with
    | ELit (LBool true) -> always_true
    | ELit (LBool false) -> refuse "a literal-false condition is not expressible as a rung justification"
    | ELocation (ScopelangScopeVar { name }) -> check_var ~in_body name
    | EAppOp { op; args; _ } -> (
        let vergelijking vop a b : I.conditie =
          Vergelijking
            { op = vop; links = Some (value_expr ~operand:true ~in_body ~env a);
              rechts = Some (value_expr ~operand:true ~in_body ~env b); periode = None; bron = None }
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
            Samengesteld { kwantor = Alle; drempel = 0; onderdelen = [ cond_expr ~in_body ~env a; cond_expr ~in_body ~env b ]; bron = Some "en" }
        | Op.Or, [ a; b ] ->
            Samengesteld { kwantor = Ten_minste; drempel = 1; onderdelen = [ cond_expr ~in_body ~env a; cond_expr ~in_body ~env b ]; bron = Some "of" }
        | o, _ -> refuse "operator %S not supported in a condition" (Operator.name o))
    | _ -> refuse "condition shape not supported in the 5a skeleton"
  in
  (value_expr, cond_expr)

(* one scope -> its object type, rules, ladders, consistencies *)
let translate_scope (enums : enum_ctx) (structs : struct_ctx) (functions : (string * I.datatype) list)
    (name : ScopeName.t) (sd : untyped Sc.scope_decl) =
  let ot_name = ScopeName.base name in
  (* variable table: ScopeVar -> (declared name, IR datatype) *)
  let vars : (string * I.datatype) ScopeVar.Map.t =
    ScopeVar.Map.fold
      (fun v (sv : Sc.scope_var_ty) acc ->
        let vname = M.remove (ScopeVar.get_info v) in
        (match M.remove sv.svar_io.Dio.io_input with
         | Catala_runtime.Reentrant ->
             refuse
               "context (reentrant) variable %S: besluit 6 wants Reading B (caller override as the highest rung), but ch. 7.8's input rung sits BELOW the ordinary rungs (Reading A). A single-scope 5a program cannot realise Reading B — refused."
               vname
         | Catala_runtime.NoInput | Catala_runtime.OnlyInput -> ());
        ScopeVar.Map.add v (vname, catala_dt enums structs sv.svar_out_ty) acc)
      sd.scope_sig ScopeVar.Map.empty
  in
  let lookup v = ScopeVar.Map.find v vars in
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
        | Sc.ScopeVarDefinition { var; e; _ } -> (
            match parse_chain e with
            | Some rungs when List.exists (fun (c, _) -> is_true c) rungs ->
                s := ScopeVar.Set.add (fst var) !s
            | _ -> ())
        | Sc.Assertion _ -> ())
      sd.scope_decl_rules;
    !s
  in
  let is_filled v = ScopeVar.Set.mem v filled in
  (* object type members, in declaration order *)
  let attributen = ref [] and kenmerken = ref [] in
  ScopeVar.Map.iter
    (fun _ (vname, dt) ->
      if is_bool_dt dt then kenmerken := vname :: !kenmerken
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
  let attr_ref vname dt : I.expr =
    Attribuut_ref
      { attribuut = Some vname; objecttype = Some ot_name;
        onderwerp_bron = Some ("de " ^ vname); navigatie_rol = None;
        typ = Some dt; bron = Some ("de " ^ vname); set = false;
        navigatie_pad = []; navigatie_bindingen = None }
  in
  let read_var ~operand ~in_body name : I.expr =
    let v = M.remove name in
    let vname, dt = lookup v in
    if in_body then
      refuse
        "WF-067: a closed lambda/function body may not read the scope variable %S (an attribute or characteristic read is ill-formed in a body — the scope-capturing fence)"
        vname
    else if is_bool_dt dt then
      refuse
        "boolean variable %S read in a value position: booleans are characteristics (kenmerken); a value-position read of a characteristic has no image in the 5a fragment (read it as a condition instead)"
        vname
    else if operand && not (is_filled v) then
      refuse
        "variable %S read as an arithmetic/comparison operand is not always filled (not an input, no unconditional definition): Catala reads it hard (error_empty = fatal on empty) while the IR coalesces an empty operand — refused (deviation deferred)"
        vname
    else attr_ref vname dt
  in
  let check_var ~in_body name : I.conditie =
    let vname, dt = lookup (M.remove name) in
    if in_body then
      refuse
        "WF-067: a closed lambda/function body may not read the scope characteristic %S (the scope-capturing fence)"
        vname
    else if is_bool_dt dt then
      Kenmerk_check
        { kenmerk = Some vname; navigatie = None; negatie = false;
          eigenaar = None; periode = None; bron = Some vname; navigatie_binding = None }
    else refuse "non-boolean variable %S used as a condition" vname
  in
  let lookup_var v = try Some (snd (lookup v)) with Not_found -> None in
  let value_expr, cond_expr =
    make_exprs ~enums ~structs ~functions ~read_var ~check_var ~lookup_var
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
  let add_regel r = regels := r :: !regels in
  let add_ladder l = ladders := l :: !ladders in
  let berekening vname dt value ~cond : I.resolved_regel =
    { rr_naam = Some ("berekening " ^ vname); rr_soort = Some Berekening;
      doel = Some (attr_ref vname dt);
      waarde = Some (value_expr value); rr_voorwaarde = cond; rr_geldigheid = Some geldig }
  in
  let kenmerktoekenning vname ~voorwaarde : I.resolved_regel =
    { rr_naam = Some ("kenmerktoekenning " ^ vname); rr_soort = Some Kenmerktoekenning;
      doel = Some (attr_ref vname Booleaan);
      waarde = Some (Literal { waarde = Some "waar"; typ = Some Booleaan; bron = Some "waar" });
      rr_voorwaarde = voorwaarde; rr_geldigheid = Some geldig }
  in
  let attr_ladder ?(ongeordend = false) vname suffix rungs : I.default_ladder =
    { dl_naam = Some (vname ^ suffix); dl_onderwerp = ot_name; dl_kenmerk = None;
      dl_attribuut = Some vname;
      rungs = List.map
        (fun (cond, value) : I.rung ->
          { rung_besluit = None; rung_voorwaarde = cond_rung cond;
            rung_consequentie = Some (value_expr value); rung_initialisatie = false;
            rung_bron = Some vname })
        rungs;
      dl_ongeordend = ongeordend; dl_bron = Some vname; dl_geldigheid = Some geldig }
  in
  let char_ladder ?(ongeordend = false) vname suffix rungs : I.default_ladder =
    { dl_naam = Some (vname ^ suffix); dl_onderwerp = ot_name; dl_kenmerk = Some vname;
      dl_attribuut = None;
      rungs = List.map
        (fun (cond, value) : I.rung ->
          let besluit = match M.remove (strip value) with
            | ELit (LBool b) -> b
            | _ -> refuse "characteristic ladder rung value must be a literal true/false"
          in
          { rung_besluit = Some besluit; rung_voorwaarde = cond_rung cond;
            rung_consequentie = None; rung_initialisatie = false; rung_bron = Some vname })
        rungs;
      dl_ongeordend = ongeordend; dl_bron = Some vname; dl_geldigheid = Some geldig }
  in
  (* dispatch one variable definition *)
  let translate_def v e =
    let vname, dt = lookup v in
    let is_bool = is_bool_dt dt in
    let parsed =
      match parse_chain e with
      | Some rungs -> `Chain rungs
      | None -> (
          match parse_root_unordered e with
          | Some alts -> `Unordered alts
          | None ->
              refuse
                "variable %S: the default tree branches below the root (sub-root branching) or combines an unconditional base with unordered exceptions — not a single chain nor a flat declared-unordered group (besluit 4, PHASE5-CATALA §5.1)"
                vname)
    in
    match parsed with
    | `Chain [ (cond, value) ] ->
        if is_bool then
          add_regel (kenmerktoekenning vname ~voorwaarde:(combine (cond_opt cond) (bool_value_guard value)))
        else if is_true cond then add_regel (berekening vname dt value ~cond:None)
        else add_ladder (attr_ladder vname "" [ (cond, value) ])
    | `Chain rungs ->
        if is_bool then add_ladder (char_ladder vname "" rungs)
        else add_ladder (attr_ladder vname "" rungs)
    | `Unordered alts ->
        List.iteri
          (fun i alt ->
            let suffix = " #" ^ string_of_int (i + 1) in
            if is_bool then add_ladder (char_ladder ~ongeordend:true vname suffix [ alt ])
            else add_ladder (attr_ladder ~ongeordend:true vname suffix [ alt ]))
          alts
  in
  (* walk the scope body *)
  List.iteri
    (fun idx rule ->
      match rule with
      | Sc.ScopeVarDefinition { var; io; e; _ } ->
          (match M.remove io.Dio.io_input with
           | Catala_runtime.OnlyInput -> () (* input: filled by the case, no rule *)
           | _ -> translate_def (fst var) e)
      | Sc.Assertion { e; _ } ->
          consistenties :=
            { I.cons_naam = Some ("assertie " ^ ot_name ^ " " ^ string_of_int (idx + 1));
              cons_subject = Some ot_name; assertie = Some (cond_expr e);
              cons_bron = Some "assertion"; cons_line = idx + 1; cons_geldigheid = Some geldig }
            :: !consistenties)
    sd.scope_decl_rules;
  (ot_name, objecttype, List.rev !regels, List.rev !ladders, List.rev !consistenties)

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
            fd_body = value_expr ~in_body:true ~env body; fd_bron = Some fname }
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
  let objecttypen = ref [] and regels = ref [] and ladders = ref [] and cons = ref [] in
  List.iter
    (fun (name, decl) ->
      let ot_name, ot, rs, ls, cs = translate_scope enums structs functions name (M.remove decl) in
      objecttypen := (ot_name, ot) :: !objecttypen;
      regels := !regels @ rs;
      ladders := !ladders @ ls;
      cons := !cons @ cs)
    scopes;
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
    domeinen; parameters = []; eenheidstelsels = []; feittypen = [];
    dimensies = []; tijdlijnen = []; dagsoorten = []; dagsoort_per_eenheid = [];
    regels = []; resolved_regels = !regels; resolved_dagsoorten = [];
    object_creaties = []; verdelingen = []; consistenties = !cons;
    uniciteiten = []; uniciteit_unies = []; feit_creaties = [];
    resolved_startpunten = []; regel_groepen = []; diagnostics = [];
    default_ladders = !ladders; function_defs }

let run ~input ~output = Emit.to_file output (file_to_document input)
