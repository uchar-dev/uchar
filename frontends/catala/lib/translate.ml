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
   - subscope call, enums-with-payload, folds, function values, lists,
     structs, if-then-else          -> REFUSED

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

(* --- IR building blocks ------------------------------------------------- *)

let geldig : I.geldigheid = { geld_vanaf = None; geld_tot = None }
let num_int : I.datatype = Numeriek { geheel = true; decimalen = 0; teken = Some Onbepaald; eenheid = None }
let num_money : I.datatype = Numeriek { geheel = false; decimalen = 2; teken = Some Onbepaald; eenheid = None }
let always_true : I.conditie = Samengesteld { kwantor = Alle; drempel = 0; onderdelen = []; bron = Some "altijd" }

(* --- Catala type -> IR datatype ---------------------------------------- *)

(* Every Catala enum maps to the row-36 SumType, uniformly — including
   all-nullary enums (PHASE5B-KERNEL §2.7): Enumeratie stays the untouched
   v0 mirror form and the two never convert. The declaring enum's base name
   is the domain; constructor payloads recurse through [catala_dt]; a
   TLit TUnit payload is nullary (None); a TTuple payload is an n-ary
   constructor — 5c struct territory — and refuses loudly. *)
let rec catala_dt (enums : enum_ctx) (t : typ) : I.datatype =
  match M.remove t with
  | TLit TInt -> num_int
  | TLit TBool -> Booleaan
  | TLit TMoney -> num_money
  | TLit TDate -> Datum_in_dagen
  | TLit TRat -> refuse "decimal (rational) type: refused in the 5a skeleton (no declared number of decimals to derive)"
  | TLit TDuration -> refuse "duration type: not supported in the 5a skeleton"
  | TLit (TUnit | TPos) -> refuse "unit/pos type: not a domain value"
  | TDefault inner -> catala_dt enums inner
  | TArrow _ -> refuse "function-valued variable: new kernel (row 10), designed spec-first in 5b"
  | TStruct _ -> refuse "struct-typed variable: not supported in the 5a skeleton"
  | TEnum name -> sum_type_of enums name
  | TTuple _ ->
      refuse
        "tuple payload: an n-ary constructor payload de-tuplifies to a Catala tuple — 5c struct territory, single-payload constructors only (WF); refused"
  | TArray _ -> refuse "list-typed variable: folds/lists are a new kernel (row 10), 5b"
  | TOption _ -> refuse "option-typed variable: not supported in the 5a skeleton"
  | _ -> refuse "unsupported variable type"

(* An enum -> its SumType datatype: constructors in declaration order
   (EnumConstructor ids are generative, so the map folds declaration-first). *)
and sum_type_of (enums : enum_ctx) (name : EnumName.t) : I.datatype =
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
             match M.remove ty with TLit TUnit -> None | _ -> Some (catala_dt enums ty)
           in
           (EnumConstructor.to_string c, payload) :: acc)
         sig_ [])
  in
  Sum_type { domein = EnumName.base name; constructoren }

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

(* one scope -> its object type, rules, ladders, consistencies *)
let translate_scope (enums : enum_ctx) (name : ScopeName.t) (sd : untyped Sc.scope_decl) =
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
        ScopeVar.Map.add v (vname, catala_dt enums sv.svar_out_ty) acc)
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
  (* --- expression / condition translation (closes over [ot_name],[vars]) *)
  let attr_ref vname dt : I.expr =
    Attribuut_ref
      { attribuut = Some vname; objecttype = Some ot_name;
        onderwerp_bron = Some ("de " ^ vname); navigatie_rol = None;
        typ = Some dt; bron = Some ("de " ^ vname); set = false;
        navigatie_pad = []; navigatie_bindingen = None }
  in
  (* [env] binds match-arm payload binders (a Bindlib var -> its declared
     name and IR datatype); it is empty outside a match body. *)
  let rec infer_dt ?(env = Var.Map.empty) e : I.datatype option =
    match M.remove (strip e) with
    | ELit (LInt _) -> Some num_int
    | ELit (LBool _) -> Some Booleaan
    | ELit (LMoney _) -> Some num_money
    | ELit (LDate _) -> Some Datum_in_dagen
    | EVar v -> Option.map snd (Var.Map.find_opt v env)
    | ELocation (ScopelangScopeVar { name }) ->
        (try Some (snd (lookup (M.remove name))) with Not_found -> None)
    | EAppOp { args = a :: _; _ } -> infer_dt ~env a
    | _ -> None
  in
  let rec value_expr ?(operand = false) ?(env = Var.Map.empty) e : I.expr =
    let e = strip e in
    match M.remove e with
    | ELit l -> literal_expr l
    | EVar v -> (
        (* a match-arm payload binder -> BinderRef (RUNTIME-SEMANTICS ch. 9.2);
           deliberately distinct from VariabeleRef (row 24) *)
        match Var.Map.find_opt v env with
        | Some (nm, dt) -> Binder_ref { naam = nm; typ = Some dt; bron = Some nm }
        | None -> refuse "unbound variable %S in a value position" (Bindlib.name_of v))
    | ELocation (ScopelangScopeVar { name }) ->
        let v = M.remove name in
        let vname, dt = lookup v in
        if is_bool_dt dt then
          refuse
            "boolean variable %S read in a value position: booleans are characteristics (kenmerken); a value-position read of a characteristic has no image in the 5a fragment (read it as a condition instead)"
            vname
        else if operand && not (is_filled v) then
          refuse
            "variable %S read as an arithmetic/comparison operand is not always filled (not an input, no unconditional definition): Catala reads it hard (error_empty = fatal on empty) while the IR coalesces an empty operand — refused (deviation deferred)"
            vname
        else attr_ref vname dt
    | EAppOp { op; args; _ } -> (
        let bin bop a b : I.expr =
          Binair { op = bop; links = Some (value_expr ~operand:true ~env a);
                   rechts = Some (value_expr ~operand:true ~env b);
                   typ = infer_dt ~env e; bron = None }
        in
        (* scopelang operators are resolved (overloaded:no): match the
           typed suffixes, not the overloaded forms. *)
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
        | o, _ -> refuse "operator %S not supported in a value expression" (Operator.name o))
    | EScopeCall _ ->
        refuse "subscope call (EScopeCall): object creation + role navigation (mapping row 9) is out of 5a scope"
    | EStructAccess { e = inner; _ } -> (
        (* a subscope output read is [(Scope of {..}).field] — a struct
           access whose subject is a scope call *)
        match M.remove (strip inner) with
        | EScopeCall _ ->
            refuse "subscope call (reading a subscope output): object creation + role navigation (mapping row 9) is out of 5a scope"
        | _ -> refuse "struct field access: not supported in the 5a skeleton")
    | EInj { name; e = payload; cons } ->
        (* a constructor value; unit payload = nullary (argument None) *)
        let sig_ = EnumName.Map.find name enums in
        let payload_ty = EnumConstructor.Map.find cons sig_ in
        let argument =
          match M.remove payload_ty with
          | TLit TUnit -> None
          | _ -> Some (value_expr ~env payload)
        in
        Construct
          { typ = Some (sum_type_of enums name);
            constructor_ = EnumConstructor.to_string cons; argument;
            bron = Some (EnumConstructor.to_string cons) }
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
                    (Some nm, Var.Map.add v (nm, catala_dt enums payload_ty) env)
              in
              let inf' =
                match inf with Some _ -> inf | None -> infer_dt ~env:env' body
              in
              ( (EnumConstructor.to_string c, binder_name, value_expr ~env:env' body)
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
          { typ = Some result_dt; scrutinee = value_expr ~env scrut;
            arms = List.rev arms_rev; bron = Some ("match " ^ EnumName.base name) }
    | EArray _ -> refuse "list literal: folds/lists are a new kernel (row 10), 5b"
    | EAbs _ -> refuse "function value: new kernel (row 10), 5b"
    | EStruct _ -> refuse "struct value: not supported in the 5a skeleton"
    | EIfThenElse _ -> refuse "if-then-else in a value: not supported in the 5a skeleton"
    | _ -> refuse "value-expression shape not supported in the 5a skeleton"
  in
  let cmp_op ?(env = Var.Map.empty) o a b : I.vergelijk_op =
    let is_date = infer_dt ~env a = Some Datum_in_dagen || infer_dt ~env b = Some Datum_in_dagen in
    match o, is_date with
    | Op.Lt, false -> Kleiner | Op.Lt, true -> Eerder
    | Op.Lte, false -> Kleiner_of_gelijk | Op.Lte, true -> Eerder_of_gelijk
    | Op.Gt, false -> Groter | Op.Gt, true -> Later
    | Op.Gte, false -> Groter_of_gelijk | Op.Gte, true -> Later_of_gelijk
    | Op.Eq, _ -> Gelijk
    | _ -> refuse "comparison operator not supported"
  in
  let rec cond_expr ?(env = Var.Map.empty) e : I.conditie =
    let e = strip e in
    match M.remove e with
    | ELit (LBool true) -> always_true
    | ELit (LBool false) -> refuse "a literal-false condition is not expressible as a rung justification"
    | ELocation (ScopelangScopeVar { name }) ->
        let vname, dt = lookup (M.remove name) in
        if is_bool_dt dt then
          Kenmerk_check
            { kenmerk = Some vname; navigatie = None; negatie = false;
              eigenaar = None; periode = None; bron = Some vname; navigatie_binding = None }
        else refuse "non-boolean variable %S used as a condition" vname
    | EAppOp { op; args; _ } -> (
        let vergelijking vop a b : I.conditie =
          Vergelijking
            { op = vop; links = Some (value_expr ~operand:true ~env a);
              rechts = Some (value_expr ~operand:true ~env b); periode = None; bron = None }
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
            Samengesteld { kwantor = Alle; drempel = 0; onderdelen = [ cond_expr ~env a; cond_expr ~env b ]; bron = Some "en" }
        | Op.Or, [ a; b ] ->
            Samengesteld { kwantor = Ten_minste; drempel = 1; onderdelen = [ cond_expr ~env a; cond_expr ~env b ]; bron = Some "of" }
        | o, _ -> refuse "operator %S not supported in a condition" (Operator.name o))
    | _ -> refuse "condition shape not supported in the 5a skeleton"
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

(* --- driver ------------------------------------------------------------- *)

let file_to_document (input : string) : I.document =
  let options =
    Global.enforce_options ~input_src:(Global.FileName input) ~language:(Some `En)
      ~no_stdlib:true ()
  in
  let prog = Driver.Passes.scopelang options ~includes:[] ~stdlib:None in
  if not (ModuleName.Map.is_empty prog.Sc.program_modules) then
    refuse "imported modules are not supported in the 5a skeleton";
  if not (TopdefName.Map.is_empty prog.Sc.program_topdefs) then
    refuse "top-level definitions (constants / functions) are not supported in the 5a skeleton";
  let enums = prog.Sc.program_ctx.ctx_enums in
  let scopes = ScopeName.Map.bindings prog.Sc.program_scopes in
  if scopes = [] then refuse "no scope declaration found";
  used_enums := EnumName.Set.empty;
  let objecttypen = ref [] and regels = ref [] and ladders = ref [] and cons = ref [] in
  List.iter
    (fun (name, decl) ->
      let ot_name, ot, rs, ls, cs = translate_scope enums name (M.remove decl) in
      objecttypen := (ot_name, ot) :: !objecttypen;
      regels := !regels @ rs;
      ladders := !ladders @ ls;
      cons := !cons @ cs)
    scopes;
  (* the enums actually referenced above become SumType domains, in
     declaration order (PHASE5B-KERNEL §2.7); no longer hard-wired empty *)
  let domeinen =
    List.map
      (fun name -> (EnumName.base name, Some (sum_type_of enums name)))
      (EnumName.Set.elements !used_enums)
  in
  { objecttypen = List.rev !objecttypen;
    domeinen; parameters = []; eenheidstelsels = []; feittypen = [];
    dimensies = []; tijdlijnen = []; dagsoorten = []; dagsoort_per_eenheid = [];
    regels = []; resolved_regels = !regels; resolved_dagsoorten = [];
    object_creaties = []; verdelingen = []; consistenties = !cons;
    uniciteiten = []; uniciteit_unies = []; feit_creaties = [];
    resolved_startpunten = []; regel_groepen = []; diagnostics = [];
    default_ladders = !ladders }

let run ~input ~output = Emit.to_file output (file_to_document input)
