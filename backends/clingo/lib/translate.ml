(* regelspraak-ir-v0 + uchar-case-v0 → ASP-Core-2 (clingo).

   The systematic landing of spec/ASP-TRANSLATION.md's conventions:
   instances as constants, a role as an argument position, navigation as
   a shared variable, negated characteristic reads as
   negation-as-failure — sound because WF-045/046 make every admissible
   document stratifiable. Extensions beyond the hand exercises:

   - Exact arithmetic by declared-decimals integer scaling: a value of
     an attribute declared with d decimals is the integer value × 10^d;
     comparisons harmonise scales by multiplication. Clingo's integer
     division truncates toward zero, which on the non-negative operands
     of the allocation recurrence is exactly floor_d.
   - Aggregation as #count/#sum with the contributor skip encoded by
     the body join (an instance without the attribute contributes no
     tuple).
   - Consistency and uniqueness derive inconsistent/2 facts — the §9.5
     channel; "consistent" means the model carries none.
   - Dates are civil day numbers (the case loader's encoding).
   - Emptiness is absence: an underivable val/3 atom is leeg, and an
     ordering comparison over leeg fails by the unbound join — the
     ch. 1.2d ordering rule for free. The equality/inequality empty
     asymmetry has no such free encoding; those operators fault here
     until a corpus case demands them (a recorded finding).

   Everything outside the corpus-exercised subset faults with context:
   fail fast, never a silent SKIP. *)

open Ir_v0.Ast
module Case = Uchar_ref.Case
module Value = Uchar_ref.Value
module Eval = Uchar_ref.Eval

let norm = Ir_v0.Checks.norm
let fault = Value.fault
let str s = "\"" ^ norm s ^ "\""
let str_raw s = "\"" ^ s ^ "\"" (* labels keep their case-sensitive spelling *)

(* --- scales ------------------------------------------------------------- *)

let pow10 n =
  let rec go acc n = if n = 0 then acc else go (acc * 10) (n - 1) in
  go 1 n

let decimals_of_datatype = function
  | Some (Numeriek { decimalen; _ } | Percentage { decimalen; _ }) -> decimalen
  | Some Datum_in_dagen -> 0
  | dt ->
      ignore dt;
      fault "unscalable datatype in the ASP translation"

let scaled_of_value ~decimals (v : Value.t) : int =
  match v with
  | Num (q, _) ->
      let s = Q.mul q (Q.of_int (pow10 decimals)) in
      if Z.equal (Q.den s) Z.one then Z.to_int (Q.num s)
      else fault "value %s does not fit %d decimals" (Q.to_string q) decimals
  | Date z -> z
  | v -> fault "unscalable value %s" (Value.to_string v)

(* --- document lookups --------------------------------------------------- *)

type ctx = { doc : document; case : Case.t; buf : Buffer.t; mutable fresh : int }

(* The convention guards, mirroring ref/'s faults (IR-SPEC ch. 4.3a:
   "executors fault, never guess"): a point read needs the full-timeline
   case variant on every subject instance, and the temporal total needs
   the asOfDate-bucket scalar — a mismatch faults instead of silently
   reading leeg through an unbound join. *)
let require_attr_timeline ctx ~otype ~attr =
  Eval.SMap.iter
    (fun _ (i : Uchar_ref.World.instance) ->
      if
        Ir_v0.Checks.norm i.otype = Ir_v0.Checks.norm otype
        && Uchar_ref.World.timeline i attr = None
      then
        fault
          "the temporal read of %S needs the full timeline; the case supplies none for %S \
           (the asOfDate-bucket scalar cannot serve a point read)"
          attr i.label)
    ctx.case.world.instances

let refuse_attr_timeline ctx ~otype ~attr =
  Eval.SMap.iter
    (fun _ (i : Uchar_ref.World.instance) ->
      if
        Ir_v0.Checks.norm i.otype = Ir_v0.Checks.norm otype
        && Uchar_ref.World.timeline i attr <> None
      then
        fault
          "the temporal total of %S over a case-supplied full timeline is not exercised by any \
           corpus case; the asOfDate-bucket convention carries the total"
          attr)
    ctx.case.world.instances

let emit ctx fmt = Printf.ksprintf (fun s -> Buffer.add_string ctx.buf (s ^ "\n")) fmt

let fresh ctx prefix =
  ctx.fresh <- ctx.fresh + 1;
  Printf.sprintf "%s%d" prefix ctx.fresh

let objecttype ctx name =
  match Ir_v0.Checks.find_ci ctx.doc.objecttypen name with
  | Some ot -> ot
  | None -> fault "unknown object type %S" name

let attr_decimals ctx ~otype ~attr =
  let ot = objecttype ctx otype in
  match Ir_v0.Checks.find_ci ot.attributen attr with
  | Some dt -> decimals_of_datatype dt
  | None -> fault "unknown attribute %S on %S" attr otype

let attr_temporal ctx ~otype ~attr = Ir_v0.Checks.attr_temporal (objecttype ctx otype) attr

let attr_granularity ctx ~otype ~attr =
  match Ir_v0.Checks.find_ci (objecttype ctx otype).attribuut_granulariteit attr with
  | Some (Some g) -> g
  | _ -> fault "temporal attribute %S without granularity" attr

(* --- expressions → (body atoms, integer term, scale) -------------------- *)

let req what = function Some x -> x | None -> fault "missing %s" what

(* does any recursive rule group carry an object-creation member? —
   gates the generation-depth machinery (depth/2, oc_budget) *)
let has_recursive_creation (doc : document) =
  List.exists
    (fun (g : resolved_regel_groep) ->
      g.recursief
      && List.exists
           (function
             | Some l ->
                 List.exists
                   (fun (o : resolved_object_creatie) ->
                     match o.oc_naam with
                     | Some n ->
                         norm (Ir_v0.Checks.strip_version n)
                         = norm (Ir_v0.Checks.strip_version l)
                     | None -> false)
                   doc.object_creaties
             | None -> false)
           g.leden)
    doc.regel_groepen

(* the canonical role name: the plural for a to-many role, the singular
   otherwise — the same convention the case facts use *)
let role_canonical (r : rol) =
  match (r.rol_meervoud, r.rol_naam) with
  | Some m, _ when r.rol_meervoudig -> m
  | _, Some n -> n
  | Some m, None -> m
  | None, None -> fault "role without a name"

let lexeme_decimals lex =
  match String.index_opt lex '.' with
  | Some i -> String.length lex - i - 1
  | None -> ( match String.index_opt lex ',' with Some i -> String.length lex - i - 1 | None -> 0)

(* the year-bucket whole-units factor of the temporal total: for a
   year-granular attribute over the calendar-year bucket the factor is
   exactly 1; other granularities await a corpus case *)
let total_factor (g : granulariteit) =
  match g with Jaar -> 1 | _ -> fault "temporal total: only year granularity is exercised"

(* harmonise two scaled terms for comparison *)
let harmonised (b1, t1, s1) (b2, t2, s2) =
  let m = max s1 s2 in
  let scale t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
  (b1 @ b2, scale t1 s1, scale t2 s2)

let rec expr_term ctx ~subject ~subject_ot (e : expr) : string list * string * int =
  match e with
  | Literal { waarde; typ; _ } -> (
      let lex = req "Literal.waarde" waarde in
      match typ with
      | Some (Numeriek _ | Percentage _) | None ->
          let d = lexeme_decimals lex in
          let v = scaled_of_value ~decimals:d (Value.Num (Value.q_of_lexeme lex, Value.Unitless)) in
          ([], string_of_int v, pow10 d)
      | _ -> fault "non-numeric literal in a term position")
  | Parameter_ref { parameter; _ } ->
      let name = req "ParameterRef.parameter" parameter in
      (match Ir_v0.Checks.find_ci ctx.doc.parameters name with
      | Some { par_granulariteit = Some _; _ } ->
          fault "granular parameter %S in a scalar slot is refused (ch. 3.3/8)" name
      | Some p ->
          let d = decimals_of_datatype p.par_datatype in
          let v = fresh ctx "P" in
          ([ Printf.sprintf "param(%s,%s)" (str name) v ], v, pow10 d)
      | None -> fault "unknown parameter %S" name)
  | Attribuut_ref
      { attribuut; objecttype = ot; navigatie_rol; navigatie_pad; navigatie_bindingen;
        set = false; typ; _ }
    when not (match typ with Some (Datatype_onbekend _) -> true | _ -> false) ->
      let a = req "attribuut" attribuut in
      let ot = req "objecttype" ot in
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "temporal attribute %S in a scalar slot is refused (ch. 3.3)" a;
      let navigated =
        navigatie_rol <> None || navigatie_pad <> [] || norm ot <> norm subject_ot
      in
      if not navigated then begin
        (match navigatie_bindingen with
        | Some _ ->
            fault "navigation bindings on a reference that does not navigate (WF-053)"
        | None -> ());
        let v = fresh ctx "A" in
        ( [ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ],
          v,
          pow10 (attr_decimals ctx ~otype:ot ~attr:a) )
      end
      else begin
        (* the navigated read (ch. 5.8): one rol/3 body join per to-one
           hop — an absent link leaves the body underivable, which IS the
           leeg-on-absent-relation rule (leeg as absence, for free). The
           hop roles resolve exactly as in ref/ (shared helpers), so the
           two executors cannot drift on the walk. *)
        let env =
          { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
            as_of = None }
        in
        let roles =
          match navigatie_bindingen with
          | Some rbs -> List.map (Eval.bound_role env) rbs
          | None -> (
              match navigatie_pad with
              | _ :: _ ->
                  let last = List.length navigatie_pad - 1 in
                  let step (i, cur, acc) name =
                    let name = req "navigation path step" name in
                    let to_type = if i = last then Some ot else None in
                    let r = Eval.role_by_name ?to_type env ~self_type:cur ~name in
                    (i + 1, Option.value r.rol_objecttype_naam ~default:"", acc @ [ r ])
                  in
                  let _, _, roles =
                    List.fold_left step (0, subject_ot, []) navigatie_pad
                  in
                  roles
              | [] -> (
                  match navigatie_rol with
                  | Some n ->
                      [ Eval.role_by_name ~to_type:ot env ~self_type:subject_ot ~name:n ]
                  | None -> [ Eval.connecting_role env ~from_type:subject_ot ~to_type:ot ]))
        in
        List.iter
          (fun (r : rol) ->
            if r.rol_meervoudig then
              fault
                "navigation over the to-many role %S in a scalar slot is refused (WF-053)"
                (Option.value r.rol_naam ~default:"?"))
          roles;
        let atoms, target =
          List.fold_left
            (fun (atoms, cur) (r : rol) ->
              let n = fresh ctx "N" in
              ( atoms @ [ Printf.sprintf "rol(%s,%s,%s)" cur (str (role_canonical r)) n ],
                n ))
            ([], subject) roles
        in
        let v = fresh ctx "A" in
        ( atoms @ [ Printf.sprintf "val(%s,%s,%s)" target (str a) v ],
          v,
          pow10 (attr_decimals ctx ~otype:ot ~attr:a) )
      end
  | Functie { kind = Aantal; argumenten = [ Attribuut_ref { attribuut; typ = Some (Datatype_onbekend _); navigatie_rol = None; navigatie_pad = []; _ } ]; element_objecttype = Some _; bron_rol; _ } ->
      (* the row-1 wart: the fold source names a role; count the linked
         instances. An explicit binding overrides the written name. *)
      let role =
        match bron_rol with
        | Some rb ->
            let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None } in
            role_canonical (Eval.bound_role env rb)
        | None -> req "fold role" attribuut
      in
      let v = fresh ctx "C" in
      ( [ Printf.sprintf "%s = #count { X%s : rol(%s,%s,X%s) }" v v subject (str role) v ],
        v,
        1 )
  | Functie { kind = Som; argumenten = [ Attribuut_ref { attribuut; set = true; navigatie_rol = None; navigatie_pad = []; _ } ]; element_objecttype = Some el; leegwaarde; bron_rol; _ } ->
      let a = req "fold attribute" attribuut in
      (match leegwaarde with
      | Some (Literal { waarde = Some "0"; _ }) -> ()
      | Some _ -> fault "som fallback other than 0 is not translated yet"
      | None ->
          fault "som without the 0 fallback needs an explicit non-empty guard; no corpus case yet");
      (* the connecting role: the node's explicit binding when present,
         else derived the same way ref/ derives it *)
      let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None } in
      let role_name =
        role_canonical
          (match bron_rol with
          | Some rb -> Eval.bound_role env rb
          | None -> Eval.connecting_role env ~from_type:subject_ot ~to_type:el)
      in
      let v = fresh ctx "S" in
      let g = fresh ctx "G" and x = fresh ctx "X" in
      ( [ Printf.sprintf "%s = #sum { %s,%s : rol(%s,%s,%s), val(%s,%s,%s) }" v g x subject
            (str role_name) x x (str a) g ],
        v,
        pow10 (attr_decimals ctx ~otype:el ~attr:a) )
  | Functie { kind = Totaal; argumenten = [ Attribuut_ref { attribuut = Some a; objecttype = Some aot; _ } ]; _ }
    when attr_temporal ctx ~otype:aot ~attr:a ->
      (* ch. 3.6 over the asOfDate bucket: value × whole-units (= 1 for
         the year bucket); an absent timeline stays underivable = leeg *)
      refuse_attr_timeline ctx ~otype:aot ~attr:a;
      let f = total_factor (attr_granularity ctx ~otype:aot ~attr:a) in
      let v = fresh ctx "T" in
      let term = if f = 1 then v else Printf.sprintf "(%s*%d)" v f in
      ( [ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ],
        term,
        pow10 (attr_decimals ctx ~otype:aot ~attr:a) )
  | Temporal_read { basis; datum; typ = _; bron = _ } -> (
      (* ch. 3.3 read(tl, d): join the segment relation on containment.
         A gap or an empty date leaves the body underivable — leeg as
         absence, exactly ref/'s leeg-on-gap and empty-index rules. *)
      let db, dt, ds = expr_term ctx ~subject ~subject_ot datum in
      if ds <> 1 then fault "temporal-read date operand must be an unscaled day term";
      let v = fresh ctx "R" in
      let f = fresh ctx "F" and t = fresh ctx "T" in
      match basis with
      | Attribuut_ref
          { attribuut = Some a; objecttype = Some otn; navigatie_rol = None;
            navigatie_pad = []; set = false; _ }
        when Ir_v0.Checks.norm otn = Ir_v0.Checks.norm subject_ot ->
          if not (attr_temporal ctx ~otype:otn ~attr:a) then
            fault "temporal read of %S, which is not a temporal attribute" a;
          require_attr_timeline ctx ~otype:otn ~attr:a;
          ( db
            @ [ Printf.sprintf "tl(%s,%s,%s,%s,%s), %s <= %s, %s < %s" subject (str a) f t v f
                  dt dt t ],
            v,
            pow10 (attr_decimals ctx ~otype:otn ~attr:a) )
      | Parameter_ref { parameter = Some p; _ } -> (
          match Ir_v0.Checks.find_ci ctx.doc.parameters p with
          | Some { par_granulariteit = None; _ } ->
              fault "temporal read of parameter %S, which is not granular" p
          | Some pr ->
              if Eval.SMap.find_opt (Ir_v0.Checks.norm p) ctx.case.param_tls = None then
                fault "the temporal read of parameter %S needs its timeline in the case" p;
              ( db
                @ [ Printf.sprintf "param_tl(%s,%s,%s,%s), %s <= %s, %s < %s" (str p) f t v f
                      dt dt t ],
                v,
                pow10 (decimals_of_datatype pr.par_datatype) )
          | None -> fault "unknown parameter %S" p)
      | _ -> fault "unsupported temporal-read basis shape")
  | Binair { op = Plus; links; rechts; _ } ->
      (* integer addition on harmonised scales — the recursive chain's
         rank increment; other operators await a corpus case. Numeric
         operands only: ref faults date arithmetic here, so a
         non-numeric static type must not silently sum as day numbers *)
      let numeric_ok e =
        match e with
        | Some e -> (
            match Ir_v0.Checks.typ_of_expr e with
            | Some (Numeriek _ | Percentage _) | None -> ()
            | Some _ -> fault "non-numeric Plus operand is not translated (no corpus case)")
        | None -> ()
      in
      numeric_ok links;
      numeric_ok rechts;
      let l = expr_term ctx ~subject ~subject_ot (req "Binair.links" links) in
      let r = expr_term ctx ~subject ~subject_ot (req "Binair.rechts" rechts) in
      let _, _, s1 = l and _, _, s2 = r in
      let bindings, tl, tr = harmonised l r in
      (bindings, Printf.sprintf "(%s+%s)" tl tr, max s1 s2)
  | Constante { naam = Some n; _ } when Ir_v0.Checks.norm n = Ir_v0.Checks.norm "Rekendatum" -> (
      (* ch. 8.2: the execution-date constant is a program constant — the
         session's one evaluation date as a civil day number *)
      match ctx.case.as_of with
      | Some d -> (
          match Value.date_of_iso d with
          | Value.Date z -> ([], string_of_int z, 1)
          | _ -> fault "malformed asOfDate %S" d)
      | None ->
          fault "the Rekendatum constant needs an evaluation date; the case carries asOfDate null")
  | Expr_onopgelost _ ->
      (* the unsolved-state guard: an Onopgelost node never vanishes —
         the rule that carries it fails observably (WF-044) *)
      fault "Onopgelost reached in translation: this rule fails observably (WF-044)"
  | _ -> fault "expression kind not translated (no corpus case exercises it)"

let vergelijk_sym = function
  | Groter_of_gelijk | Later_of_gelijk -> ">="
  | Groter | Later -> ">"
  | Kleiner | Eerder -> "<"
  | Kleiner_of_gelijk | Eerder_of_gelijk -> "<="
  | Gelijk | Ongelijk ->
      fault
        "equality over possibly-empty operands has no free ASP encoding (the ch. 1.2d \
         asymmetry); no corpus case exercises it — recorded finding"

let rec cond_atoms ctx ~subject ~subject_ot (c : conditie) : string list =
  match c with
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      let l = expr_term ctx ~subject ~subject_ot (req "links" links) in
      let r = expr_term ctx ~subject ~subject_ot (req "rechts" rechts) in
      let bindings, tl, tr = harmonised l r in
      bindings @ [ Printf.sprintf "%s %s %s" tl (vergelijk_sym op) tr ]
  | Kenmerk_check { kenmerk; navigatie = None; negatie; navigatie_binding; eigenaar = _; _ } ->
      (match navigatie_binding with
      | Some _ ->
          fault "navigation binding on a characteristic check that does not navigate (WF-053)"
      | None -> ());
      let k = req "kenmerk" kenmerk in
      [ (if negatie then "not " else "") ^ Printf.sprintf "heeft(%s,%s)" subject (str k) ]
  | Kenmerk_check { kenmerk; navigatie = Some nav; negatie; navigatie_binding; eigenaar = _; _ }
    ->
      (* the navigated check (ch. 5.8): the hop is a POSITIVE literal;
         only the characteristic sits under the negation. An absent link
         therefore fails the positive and the negated form alike — the
         deliberate v0 asymmetry, encoded for free. *)
      let k = req "kenmerk" kenmerk in
      let env =
        { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
          as_of = None }
      in
      let role =
        match navigatie_binding with
        | Some rb -> Eval.bound_role env rb
        | None -> Eval.role_by_name env ~self_type:subject_ot ~name:nav
      in
      if role.rol_meervoudig then
        fault
          "the navigated check reaches the to-many role %S in a scalar condition (WF-053)"
          (Option.value role.rol_naam ~default:"?");
      let n = fresh ctx "N" in
      [ Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical role)) n;
        (if negatie then "not " else "") ^ Printf.sprintf "heeft(%s,%s)" n (str k) ]
  | Samengesteld { kwantor = Alle | De; onderdelen; _ } ->
      List.concat_map (cond_atoms ctx ~subject ~subject_ot) onderdelen
  | _ -> fault "condition kind not translated (no corpus case exercises it)"

(* --- rule forms ---------------------------------------------------------- *)

let head_body ctx head body = emit ctx "%s :- %s." head (String.concat ", " body)

let translate_regel ctx (r : resolved_regel) =
  match r.rr_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "validity windows are not translated yet"
  | _ ->
      let doel = req "doel" r.doel in
      (match doel with
      | Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ } ->
          let subject = "S" in
          let base = Printf.sprintf "is_a(%s,%s)" subject (str ot) in
          let cond =
            match r.rr_voorwaarde with
            | Some c -> cond_atoms ctx ~subject ~subject_ot:ot c
            | None -> []
          in
          (match r.rr_soort with
          | Some Kenmerktoekenning ->
              (match r.waarde with
              | Some (Literal { waarde = Some "waar"; _ }) -> ()
              | _ -> fault "characteristic attribution with a non-waar value");
              head_body ctx (Printf.sprintf "heeft(%s,%s)" subject (str a)) (base :: cond)
          | Some (Berekening | Gelijkstelling) ->
              let bindings, term, scale = expr_term ctx ~subject ~subject_ot:ot (req "waarde" r.waarde) in
              let target_scale = pow10 (attr_decimals ctx ~otype:ot ~attr:a) in
              if scale <> target_scale then
                fault "value scale %d does not match target scale %d for %S" scale target_scale a;
              head_body ctx
                (Printf.sprintf "val(%s,%s,%s)" subject (str a) term)
                ((base :: cond) @ bindings)
          | _ -> fault "rule kind not translated")
      | _ -> fault "unsupported rule target")

(* object creation (IR-SPEC ch. 3.3; RUNTIME-SEMANTICS ch. 5.5): the
   created instance is a deterministic function term ocN(Subject) — set
   semantics carries the per-(rule × subject) idempotence, exactly
   ref/'s deterministic label. Member initialisations become val/heeft
   heads on the term; the back-link relation (the explicit
   verbindendeRol binding, else the uniquely-derivable pair) lands both
   rol/3 ends. The 1:1 shape's linked-instance guard reads the INPUT
   links only (inputrol/3, the case facts) — the same input-snapshot
   narrowing ref/ records for its guard. A creation that is a member of
   a RECURSIVE rule group grounds along a generation depth (depth/2,
   bounded by oc_budget = 100, the same bound as ref/'s pass budget —
   ref counts passes, this side generations per chain; one chain
   advances one generation per pass, so the two accounts align):
   exhausting the budget derives a viol/2 witness — UNSAT where ref/'s
   pass budget faults (ch. 5.5's bounded budget, never a silent
   truncation; the bound also stays under clingo's own term-depth
   abort, recorded in the README). *)
let translate_object_creatie ctx i (oc : resolved_object_creatie) =
  (match oc.oc_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "object-creation validity windows are not translated yet"
  | _ -> ());
  let nieuw = req "ObjectCreatie.nieuwType" oc.nieuw_type in
  let subject_ot = req "ObjectCreatie.subject" oc.oc_subject in
  let subject = "S" in
  let term = Printf.sprintf "oc%d(%s)" i subject in
  let env =
    { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None }
  in
  let pair =
    match oc.oc_verbindende_rol with
    | Some rb -> (
        match Ir_v0.Checks.resolve_binding { doc = ctx.doc } rb with
        | Ok (_, bound, other) -> Some (rb.rb_feittype, bound, other)
        | Error e -> fault "%s" e)
    | None -> Eval.connecting_pair env ~from_type:subject_ot ~to_type:nieuw
  in
  let cond =
    match oc.oc_voorwaarde with
    | Some c -> cond_atoms ctx ~subject ~subject_ot c
    | None -> []
  in
  let guard =
    match pair with
    | Some (_, new_role, _) when not new_role.rol_meervoudig ->
        [ Printf.sprintf "not inputrol(%s,%s,_)" subject (str (role_canonical new_role)) ]
    | _ -> []
  in
  let recursive =
    let name = norm (Ir_v0.Checks.strip_version (Option.value oc.oc_naam ~default:"")) in
    List.exists
      (fun (g : resolved_regel_groep) ->
        g.recursief
        && List.exists
             (function
               | Some l -> norm (Ir_v0.Checks.strip_version l) = name
               | None -> false)
             g.leden)
      ctx.doc.regel_groepen
  in
  let depth_guard =
    if recursive then [ Printf.sprintf "depth(%s,D)" subject; "D < oc_budget" ] else []
  in
  let body =
    (Printf.sprintf "is_a(%s,%s)" subject (str subject_ot) :: cond) @ guard @ depth_guard
  in
  (if recursive then head_body ctx (Printf.sprintf "depth(%s,D+1)" term) body
   else if has_recursive_creation ctx.doc then
     (* a non-recursive mint still carries a depth in a document with
        recursive chains, so a chain seeded from it stays bounded *)
     head_body ctx
       (Printf.sprintf "depth(%s,D+1)" term)
       (body @ [ Printf.sprintf "depth(%s,D)" subject ]));
  head_body ctx (Printf.sprintf "is_a(%s,%s)" term (str nieuw)) body;
  List.iter
    (fun (t : toekenning) ->
      let m = req "Toekenning.attribuut" t.toe_attribuut in
      if t.toe_kenmerk then (
        match t.toe_waarde with
        | Some (Literal { waarde = Some "waar"; _ }) ->
            head_body ctx (Printf.sprintf "heeft(%s,%s)" term (str m)) body
        | Some (Literal { waarde = Some "onwaar"; _ }) -> ()
        | _ -> fault "characteristic initialisation of %S is not a boolean constant (WF-027)" m)
      else begin
        let bindings, tm, scale =
          expr_term ctx ~subject ~subject_ot (req "Toekenning.waarde" t.toe_waarde)
        in
        let target_scale = pow10 (attr_decimals ctx ~otype:nieuw ~attr:m) in
        if scale <> target_scale then
          fault "initialisation value scale %d does not match target scale %d for %S" scale
            target_scale m;
        head_body ctx (Printf.sprintf "val(%s,%s,%s)" term (str m) tm) (body @ bindings)
      end)
    oc.toekenningen;
  match pair with
  | None -> ()
  | Some (_, new_role, subj_role) ->
      head_body ctx
        (Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical new_role)) term)
        body;
      head_body ctx
        (Printf.sprintf "rol(%s,%s,%s)" term (str (role_canonical subj_role)) subject)
        body

(* fact creation (IR-SPEC ch. 3.4; RUNTIME-SEMANTICS ch. 5.5): the join
   chain becomes a body join over rol/3, and the head derives the
   created relation's rol/3 facts — both ends, exactly the two
   directions ref/'s writer lands; set semantics carries the
   idempotence (a target reached along two paths is one atom). Only the
   symbolic extension form executes; a bean-field-only v0 record faults
   (as in ref/). *)
let translate_feit_creatie ctx (fc : resolved_feit_creatie) =
  (match fc.fc_geldigheid with
  | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
      fault "fact-creation validity windows are not translated yet"
  | _ -> ());
  let name = Option.value fc.fc_naam ~default:"<feitcreatie>" in
  let ftn =
    match fc.fc_feittype with
    | Some f -> f
    | None ->
        fault
          "fact creation %S carries only bean fields; the symbolic form (IR-SPEC ch. 3.4) is \
           what executes"
          name
  in
  let env =
    { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None }
  in
  let role rname = Eval.bound_role env { rb_feittype = ftn; rb_rol = rname } in
  let anker_rol = role (req "ankerRol" fc.anker_rol) in
  let doel_rol = role (req "doelRol" fc.doel_rol) in
  let anker = req "FeitCreatie.anker" fc.anker in
  let subject = "S" in
  let cond =
    match fc.fc_voorwaarde with
    | Some c -> cond_atoms ctx ~subject ~subject_ot:anker c
    | None -> []
  in
  let hop (atoms, cur) rb =
    let v = fresh ctx "H" in
    ( atoms
      @ [ Printf.sprintf "rol(%s,%s,%s)" cur (str (role_canonical (Eval.bound_role env rb))) v ],
      v )
  in
  let mids =
    List.fold_left
      (fun acc (t : tussenstap) -> hop acc (req "Tussenstap.rolBinding" t.ts_rol))
      ([], subject) fc.tussenstappen
  in
  let chain, target = hop mids (req "joinRol" fc.join_rol) in
  let body = (Printf.sprintf "is_a(%s,%s)" subject (str anker) :: cond) @ chain in
  head_body ctx (Printf.sprintf "rol(%s,%s,%s)" subject (str (role_canonical doel_rol)) target) body;
  head_body ctx (Printf.sprintf "rol(%s,%s,%s)" target (str (role_canonical anker_rol)) subject) body;
  (* a second distinct filler on a to-one end faults in ref/
     (World.add_link, ch. 5.5); a viol/2 witness plus the global
     integrity constraint makes clingo fail identically (UNSAT) instead
     of silently holding both atoms — and the runner's UNSAT diagnosis
     re-run can name WHICH guard fired *)
  let to_one_guard (r : rol) holder_type =
    if not r.rol_meervoudig then
      emit ctx "viol(\"to-one second filler on role %s\",X) :- is_a(X,%s), 2 <= #count { Y : rol(X,%s,Y) }."
        (norm (role_canonical r)) (str holder_type)
        (str (role_canonical r))
  in
  to_one_guard doel_rol anker;
  to_one_guard anker_rol (req "FeitCreatie.doel" fc.fc_doel)

(* the ladder group (RUNTIME-SEMANTICS ch. 7, IR-SPEC ch. 6.1): the
   ch. 7.5 decides_m lowering per member — decides predicates per rung,
   an outcome predicate per member (highest deciding rung wins via
   `not decides_m` guards; on an attribute ladder the consequence
   bindings sit inside decides, so a leeg consequence is underivable —
   the gevuld gate for free) — and the ch. 7.2 group layer on top: a
   pairwise conflict rule, outcome heads and the defeasible input both
   guarded on the conflict, and the conflict deposited as
   inconsistent/2 keyed by the target form. A singleton group elides
   the conflict machinery. Sound NAF because WF-051 makes displacement
   edges negative (the verifier remains the guard that a document is
   clingo-suitable). *)
let translate_ladder_group ctx gidx (members : (int * default_ladder) list) =
  let _, l0 = List.hd members in
  List.iter
    (fun ((_, l) : int * default_ladder) ->
      match l.dl_geldigheid with
      | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
          fault "ladder validity windows are not translated yet"
      | _ -> ())
    members;
  let subject = "S" in
  let base = Printf.sprintf "is_a(%s,%s)" subject (str l0.dl_onderwerp) in
  let d i n = Printf.sprintf "dl%d_decides_%d(%s)" i n subject in
  let conflict = Printf.sprintf "grp%d_conflict(%s)" gidx subject in
  let singleton = List.length members = 1 in
  let conflict_guard = if singleton then [] else [ "not " ^ conflict ] in
  let target_form tn = l0.dl_onderwerp ^ "." ^ tn in
  match (l0.dl_kenmerk, l0.dl_attribuut) with
  | Some k, None ->
      let dec i = Printf.sprintf "dl%d_dec" i in
      List.iter
        (fun ((i, l) : int * default_ladder) ->
          List.iteri
            (fun n (r : rung) ->
              let atoms =
                cond_atoms ctx ~subject ~subject_ot:l.dl_onderwerp
                  (req "rung justification" r.rung_voorwaarde)
              in
              head_body ctx (d i n) (base :: atoms);
              let b = if req "rung besluit" r.rung_besluit then 1 else 0 in
              let displaced = List.init n (fun m -> "not " ^ d i m) in
              head_body ctx
                (Printf.sprintf "%s(%s,%d)" (dec i) subject b)
                ((d i n :: displaced) @ [ base ]))
            l.rungs)
        members;
      if not singleton then begin
        List.iter
          (fun ((i, _) : int * default_ladder) ->
            List.iter
              (fun ((j, _) : int * default_ladder) ->
                if i < j then
                  head_body ctx conflict
                    [ Printf.sprintf "%s(%s,X)" (dec i) subject;
                      Printf.sprintf "%s(%s,Y)" (dec j) subject; "X != Y" ])
              members)
          members;
        head_body ctx
          (Printf.sprintf "inconsistent(%s,%s)" (str (target_form k)) subject)
          [ conflict ]
      end;
      List.iter
        (fun ((i, l) : int * default_ladder) ->
          (* only members with an attribute-true rung can derive the
             characteristic — eliding the head keeps clingo info-clean *)
          if List.exists (fun (r : rung) -> req "rung besluit" r.rung_besluit) l.rungs then
            head_body ctx
              (Printf.sprintf "heeft(%s,%s)" subject (str k))
              ((Printf.sprintf "%s(%s,1)" (dec i) subject :: conflict_guard) @ [ base ]))
        members
  | None, Some a ->
      let out i = Printf.sprintf "dl%d_out" i in
      let target_scale = pow10 (attr_decimals ctx ~otype:l0.dl_onderwerp ~attr:a) in
      let has_input = Printf.sprintf "grp%d_has_input(%s)" gidx subject in
      head_body ctx has_input [ base; Printf.sprintf "inputval(%s,%s,_)" subject (str a) ];
      List.iter
        (fun ((i, l) : int * default_ladder) ->
          let rung_cond (r : rung) =
            if r.rung_initialisatie then [ "not " ^ has_input ]
            else
              cond_atoms ctx ~subject ~subject_ot:l.dl_onderwerp
                (req "rung justification" r.rung_voorwaarde)
          in
          List.iteri
            (fun n (r : rung) ->
              let conseq = req "rung consequentie" r.rung_consequentie in
              let bindings, _, scale =
                expr_term ctx ~subject ~subject_ot:l.dl_onderwerp conseq
              in
              if scale <> target_scale then
                fault "rung value scale %d does not match target scale %d for %S" scale
                  target_scale a;
              head_body ctx (d i n) ((base :: rung_cond r) @ bindings);
              let bindings2, term2, _ =
                expr_term ctx ~subject ~subject_ot:l.dl_onderwerp conseq
              in
              let displaced = List.init n (fun m -> "not " ^ d i m) in
              head_body ctx
                (Printf.sprintf "%s(%s,%s)" (out i) subject term2)
                ((base :: rung_cond r) @ bindings2 @ displaced))
            l.rungs)
        members;
      if not singleton then begin
        List.iter
          (fun ((i, _) : int * default_ladder) ->
            List.iter
              (fun ((j, _) : int * default_ladder) ->
                if i < j then
                  head_body ctx conflict
                    [ Printf.sprintf "%s(%s,X)" (out i) subject;
                      Printf.sprintf "%s(%s,Y)" (out j) subject; "X != Y" ])
              members)
          members;
        head_body ctx
          (Printf.sprintf "inconsistent(%s,%s)" (str (target_form a)) subject)
          [ conflict ]
      end;
      let eff = Printf.sprintf "grp%d_eff(%s)" gidx subject in
      List.iter
        (fun ((i, _) : int * default_ladder) ->
          head_body ctx
            (Printf.sprintf "val(%s,%s,V)" subject (str a))
            ((Printf.sprintf "%s(%s,V)" (out i) subject :: conflict_guard) @ [ base ]);
          head_body ctx eff
            ((Printf.sprintf "%s(%s,_)" (out i) subject :: conflict_guard) @ [ base ]))
        members;
      (* the defeasible input: the case value stands unless the group
         effectively writes (a conflicting group leaves it standing) *)
      head_body ctx
        (Printf.sprintf "val(%s,%s,V)" subject (str a))
        [ Printf.sprintf "inputval(%s,%s,V)" subject (str a); base; "not " ^ eff ]
  | _ -> fault "a ladder targets exactly one of a characteristic or an attribute"

(* allocation, proportional with cap/floor/remainder (RUNTIME-SEMANTICS
   ch. 4.2), in integer arithmetic. Scale bookkeeping: raw = pot×w/Σw
   carries the pot's scale (the weight scale cancels); the floor at the
   declared decimals must therefore coincide with the pot/target scale. *)
let translate_verdeling ctx i (v : resolved_verdeling) =
  if v.vd_methode <> Some Naar_rato then fault "only the proportional allocation is translated yet";
  if v.vd_voorwaarde <> None then fault "conditional allocation not translated yet";
  let bt = req "bronType" v.bron_type and dt = req "doelType" v.doel_type in
  let ba = req "bronAttribuut" v.bron_attribuut and da = req "doelAttribuut" v.doel_attribuut in
  let rato = req "ratoAttribuut (proportional)" v.rato_attribuut in
  let name = Option.value v.vd_naam ~default:"verdeling" in
  let env = { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None } in
  let role =
    role_canonical
      (match v.verbindende_rol with
      | Some rb -> Eval.bound_role env rb
      | None -> Eval.connecting_role env ~from_type:bt ~to_type:dt)
  in
  let pot_scale = pow10 (attr_decimals ctx ~otype:bt ~attr:ba) in
  let target_scale = pow10 (attr_decimals ctx ~otype:dt ~attr:da) in
  let floor_scale = pow10 (Option.value v.afronding_decimalen ~default:0) in
  let cap_scale =
    match v.maximum_attribuut with
    | Some cap -> pow10 (attr_decimals ctx ~otype:dt ~attr:cap)
    | None -> target_scale
  in
  let rest_scale =
    match v.restant_attribuut with
    | Some ra -> pow10 (attr_decimals ctx ~otype:bt ~attr:ra)
    | None -> pot_scale
  in
  if
    pot_scale <> target_scale || floor_scale <> target_scale || cap_scale <> target_scale
    || rest_scale <> pot_scale
  then
    fault
      "allocation scale mismatch (pot %d, target %d, floor %d, cap %d, rest %d): only the \
       aligned form is translated"
      pot_scale target_scale floor_scale cap_scale rest_scale;
  let p = Printf.sprintf "vd%d_" i in
  (* eligible recipients: filled weight; the weightless are excluded and
     surface on the channel (ch. 4.4) *)
  emit ctx "%sgewicht(X,W) :- rol(B,%s,X), is_a(B,%s), val(X,%s,W)." p (str role) (str bt)
    (str rato);
  (* fail-fast guards, mirrored on ref/'s faults: integer division is
     floor only on non-negative operands, and an empty pot is the
     ch. 4.4 v0 hole ref/ faults on — viol/2 witnesses plus the global
     integrity constraint make clingo fail identically instead of
     silently diverging, and let the runner name the fired guard *)
  emit ctx "viol(\"allocation: negative pot\",B) :- is_a(B,%s), val(B,%s,A), A < 0." (str bt)
    (str ba);
  emit ctx "viol(\"allocation: negative weight\",X) :- %sgewicht(X,W), W < 0." p;
  emit ctx "%sheeft_pot(B) :- is_a(B,%s), val(B,%s,_)." p (str bt) (str ba);
  emit ctx "viol(\"allocation: empty pot\",B) :- is_a(B,%s), rol(B,%s,_), not %sheeft_pot(B)."
    (str bt) (str role) p;
  emit ctx "inconsistent(%s,X) :- rol(B,%s,X), is_a(B,%s), not %sheeft_gewicht(X)." (str name)
    (str role) (str bt) p;
  emit ctx "%sheeft_gewicht(X) :- %sgewicht(X,_)." p p;
  emit ctx "%swsum(B,S) :- is_a(B,%s), S = #sum { W,X : rol(B,%s,X), %sgewicht(X,W) }, S != 0." p
    (str bt) (str role) p;
  emit ctx "%sraw(X,(A*W)/S) :- rol(B,%s,X), %sgewicht(X,W), %swsum(B,S), val(B,%s,A)." p
    (str role) p p (str ba);
  (match v.maximum_attribuut with
  | Some cap ->
      (* cap before floor; an empty or negative cap degrades to uncapped
         + a channel fact (ch. 4.3) *)
      emit ctx "%scap(X,C) :- %sraw(X,_), val(X,%s,C), C >= 0." p p (str cap);
      emit ctx "inconsistent(%s,X) :- %sraw(X,_), val(X,%s,C), C < 0." (str name) p (str cap);
      emit ctx "inconsistent(%s,X) :- %sraw(X,_), not %sheeft_cap(X)." (str name) p p;
      emit ctx "%sheeft_cap(X) :- val(X,%s,C), C >= 0, %sraw(X,_)." p (str cap) p;
      emit ctx "val(X,%s,R) :- %sraw(X,R), %scap(X,C), R <= C." (str da) p p;
      emit ctx "val(X,%s,C) :- %sraw(X,R), %scap(X,C), R > C." (str da) p p;
      emit ctx "val(X,%s,R) :- %sraw(X,R), not %sheeft_cap(X)." (str da) p p
  | None -> emit ctx "val(X,%s,R) :- %sraw(X,R)." (str da) p);
  (match v.restant_attribuut with
  | Some ra ->
      emit ctx "val(B,%s,A-T) :- is_a(B,%s), val(B,%s,A), %swsum(B,_), T = #sum { Sh,X : rol(B,%s,X), val(X,%s,Sh) }."
        (str ra) (str bt) (str ba) p (str role) (str da)
  | None -> ())

let translate_consistentie ctx i (c : resolved_consistentie) =
  let name = Option.value c.cons_naam ~default:(Printf.sprintf "consistentie%d" i) in
  let subject_ot = req "subject" c.cons_subject in
  let ok = Printf.sprintf "cons%d_ok(S)" i in
  let atoms = cond_atoms ctx ~subject:"S" ~subject_ot (req "assertie" c.assertie) in
  head_body ctx ok (Printf.sprintf "is_a(S,%s)" (str subject_ot) :: atoms);
  emit ctx "inconsistent(%s,S) :- is_a(S,%s), not cons%d_ok(S)." (str name) (str subject_ot) i

let translate_uniciteit ctx (u : resolved_uniciteit) =
  let name = Option.value u.uni_naam ~default:"uniciteit" in
  let subject_ot = req "subject" u.uni_subject in
  let keys = List.map (req "sleutel") u.sleutels in
  let joins label =
    List.mapi
      (fun i k -> Printf.sprintf "val(%s,%s,K%d)" label (str k) i)
      keys
  in
  (* the scope hop (ch. 5.9, WF-054): both rows join the same scope
     parent — an instance without the rol/3 atom (scope-less) never
     matches, the pinned runtime's narrowing for free *)
  let scope_atoms =
    match (u.scope_rol, u.scope_veld) with
    | None, None -> []
    | rb_opt, sv_opt ->
        let env =
          { Eval.doc = ctx.doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty;
            as_of = None }
        in
        let role =
          match rb_opt with
          | Some rb -> Eval.bound_role env rb
          | None ->
              Eval.role_by_name env ~self_type:subject_ot
                ~name:(req "scopeVeld" sv_opt)
        in
        [ Printf.sprintf "rol(S1,%s,P)" (str (role_canonical role));
          Printf.sprintf "rol(S2,%s,P)" (str (role_canonical role)) ]
  in
  emit ctx "inconsistent(%s,S1) :- is_a(S1,%s), is_a(S2,%s), S1 != S2, %s." (str name)
    (str subject_ot) (str subject_ot)
    (String.concat ", " (joins "S1" @ joins "S2" @ scope_atoms))

(* the uniqueness union (ch. 5.9): every source feeds one shared key
   predicate — a duplicate may arise across two different source types.
   Key positions harmonise to the maximum declared scale across sources,
   so 5 and 5.00 collide exactly as the runtime's value equality does. *)
let translate_uniciteit_unie ctx i (u : resolved_uniciteit_unie) =
  let name = Option.value u.uu_naam ~default:(Printf.sprintf "uniciteitsunie%d" i) in
  let arity =
    match u.bronnen with
    | b :: _ -> List.length b.uub_sleutels
    | [] -> fault "a uniqueness union carries at least one source"
  in
  let max_scale pos =
    List.fold_left
      (fun m (b : uniciteit_unie_bron) ->
        let t = req "UniciteitUnie.bron.type" b.uub_type in
        let k = req "sleutel" (List.nth b.uub_sleutels pos) in
        max m (attr_decimals ctx ~otype:t ~attr:k))
      0 u.bronnen
  in
  let scales = List.init arity max_scale in
  let pred = Printf.sprintf "uu%d_key" i in
  List.iter
    (fun (b : uniciteit_unie_bron) ->
      let t = req "UniciteitUnie.bron.type" b.uub_type in
      let head_args =
        List.mapi
          (fun pos k ->
            let k = req "sleutel" k in
            let d = attr_decimals ctx ~otype:t ~attr:k in
            let m = pow10 (List.nth scales pos - d) in
            if m = 1 then Printf.sprintf "K%d" pos else Printf.sprintf "(K%d*%d)" pos m)
          b.uub_sleutels
      in
      let body =
        Printf.sprintf "is_a(S,%s)" (str t)
        :: List.mapi
             (fun pos k -> Printf.sprintf "val(S,%s,K%d)" (str (req "sleutel" k)) pos)
             b.uub_sleutels
      in
      emit ctx "%s(S,%s) :- %s." pred (String.concat "," head_args)
        (String.concat ", " body))
    u.bronnen;
  let vars = List.init arity (fun p -> Printf.sprintf "V%d" p) in
  emit ctx "inconsistent(%s,S) :- %s(S,%s), 2 <= #count { O : %s(O,%s) }." (str name) pred
    (String.concat "," vars) pred (String.concat "," vars)

(* --- case facts ---------------------------------------------------------- *)

let translate_case ctx (case : Case.t) =
  let doc = ctx.doc in
  (* attribute-ladder targets: their input values enter defeasibly
     (inputval/3), standing unless the ladder decides *)
  let ladder_target otype aname =
    List.exists
      (fun (l : default_ladder) ->
        match l.dl_attribuut with
        | Some a -> norm l.dl_onderwerp = norm otype && norm a = norm aname
        | None -> false)
      doc.default_ladders
  in
  Eval.SMap.iter
    (fun label (i : Uchar_ref.World.instance) ->
      emit ctx "is_a(%s,%s)." (str_raw label) (str i.otype);
      (* generation depth 0 for every input instance — the recursive
         creations' grounding bound counts from here *)
      if has_recursive_creation doc then emit ctx "depth(%s,0)." (str_raw label);
      Eval.SMap.iter
        (fun aname v ->
          let d = attr_decimals ctx ~otype:i.otype ~attr:aname in
          let pred = if ladder_target i.otype aname then "inputval" else "val" in
          emit ctx "%s(%s,%s,%d)." pred (str_raw label) (str aname)
            (scaled_of_value ~decimals:d v))
        i.attrs;
      List.iter (fun k -> emit ctx "heeft(%s,%s)." (str_raw label) (str k)) i.chars;
      Eval.SMap.iter
        (fun aname (tl : Uchar_ref.Timeline.t) ->
          let d = attr_decimals ctx ~otype:i.otype ~attr:aname in
          List.iter
            (fun (s : Uchar_ref.Timeline.segment) ->
              emit ctx "tl(%s,%s,%d,%d,%d)." (str_raw label) (str aname) s.s_van s.s_tot
                (scaled_of_value ~decimals:d s.s_waarde))
            tl)
        i.tls;
      Eval.SMap.iter
        (fun key targets ->
          let canonical = role_canonical (Case.resolve_role doc ~otype:i.otype ~key) in
          List.iter
            (fun t ->
              emit ctx "rol(%s,%s,%s)." (str_raw label) (str canonical) (str_raw t);
              (* the 1:1 creation guard reads the input links only
                 (ch. 5.5's snapshot narrowing) — mirror them as
                 inputrol/3 when a creation could consult them *)
              if doc.object_creaties <> [] then
                emit ctx "inputrol(%s,%s,%s)." (str_raw label) (str canonical) (str_raw t))
            targets)
        i.links)
    case.world.instances;
  Eval.SMap.iter
    (fun pname v ->
      match Ir_v0.Checks.find_ci doc.parameters pname with
      | Some { par_granulariteit = Some _; _ } -> () (* never read; ch. 8 *)
      | Some p ->
          let d = decimals_of_datatype p.par_datatype in
          emit ctx "param(%s,%d)." (str pname) (scaled_of_value ~decimals:d v)
      | None -> fault "unknown parameter %S" pname)
    case.params;
  Eval.SMap.iter
    (fun pname (tl : Uchar_ref.Timeline.t) ->
      match Ir_v0.Checks.find_ci doc.parameters pname with
      | Some p ->
          let d = decimals_of_datatype p.par_datatype in
          List.iter
            (fun (s : Uchar_ref.Timeline.segment) ->
              emit ctx "param_tl(%s,%d,%d,%d)." (str pname) s.s_van s.s_tot
                (scaled_of_value ~decimals:d s.s_waarde))
            tl
      | None -> fault "unknown parameter %S" pname)
    case.param_tls

(* --- the program --------------------------------------------------------- *)

let translate (doc : document) (case : Case.t) : string =
  (* the ch. 7.7 v0 desugaring: initialisation-plus-computation pairs
     read as ladders here, so the ladder machinery (including the
     defeasible input) carries them — while ref/ replays the v0 forms
     natively. Gate 6's differential thereby witnesses the desugaring
     itself. Forms outside the desugaring's domain stay as they are
     and fault below like before. *)
  let doc = Ir_v0.Desugar.document doc in
  let ctx = { doc; case; buf = Buffer.create 4096; fresh = 0 } in
  (* ch. 8.4: the shared session-start Parameterset coverage check —
     data-plane validation (like the shared case loader), run before a
     program is emitted, mirroring ref/'s evaluation entry *)
  Eval.parameterset_check
    { Eval.doc; params = case.params; param_tls = case.param_tls; as_of = case.as_of };
  emit ctx "%% generated by backends/clingo from regelspraak-ir-v0 + uchar-case-v0.";
  emit ctx "#defined val/3. #defined heeft/2. #defined rol/3. #defined param/2.";
  emit ctx "#defined inconsistent/2. #defined tl/5. #defined param_tl/4.";
  emit ctx "#defined inputval/3. #defined viol/2. #defined inputrol/3.";
  if has_recursive_creation doc then begin
    (* the generation-depth bound (ch. 5.5's fire budget, grounded): a
       recursive chain grounds only as deep as its condition carries
       it, and a chain that reaches the budget derives a viol/2
       witness — UNSAT exactly where ref/'s pass budget faults *)
    emit ctx "#const oc_budget = 100. #defined depth/2.";
    emit ctx "viol(\"generation budget exhausted\",X) :- depth(X,oc_budget)."
  end;
  (* the fail-fast guards derive viol/2 witnesses; this single global
     integrity constraint turns any witness into UNSAT — the runner's
     UNSAT diagnosis re-runs without exactly this line to report which
     guard fired *)
  emit ctx ":- viol(_,_).";
  List.iter (translate_regel ctx) doc.resolved_regels;
  List.iter (translate_feit_creatie ctx) doc.feit_creaties;
  List.iteri (translate_object_creatie ctx) doc.object_creaties;
  List.iteri (translate_verdeling ctx) doc.verdelingen;
  (let key (l : default_ladder) =
     (* kind discriminant: a name-sharing attribute and characteristic
        (WF-002) are separate targets, never one group *)
     (if l.dl_kenmerk <> None then "K:" else "A:")
     ^ norm l.dl_onderwerp ^ ":"
     ^
     match (l.dl_kenmerk, l.dl_attribuut) with
     | Some k, _ -> norm k
     | _, Some a -> norm a
     | _ -> "?"
   in
   let groups =
     List.fold_left
       (fun acc (i, l) ->
         let k = key l in
         match List.assoc_opt k acc with
         | Some _ ->
             List.map
               (fun (k', ms) -> if k' = k then (k', ms @ [ (i, l) ]) else (k', ms))
               acc
         | None -> acc @ [ (k, [ (i, l) ]) ])
       []
       (List.mapi (fun i l -> (i, l)) doc.default_ladders)
   in
   List.iteri (fun gidx (_, ms) -> translate_ladder_group ctx gidx ms) groups);
  List.iteri (translate_consistentie ctx) doc.consistenties;
  List.iter (translate_uniciteit ctx) doc.uniciteiten;
  List.iteri (translate_uniciteit_unie ctx) doc.uniciteit_unies;
  translate_case ctx case;
  emit ctx "#show val/3. #show heeft/2. #show inconsistent/2.";
  Buffer.contents ctx.buf
