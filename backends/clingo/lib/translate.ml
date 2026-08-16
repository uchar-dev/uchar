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

type ctx = { doc : document; buf : Buffer.t; mutable fresh : int }

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
      { attribuut; objecttype = ot; navigatie_rol = None; navigatie_pad = []; set = false; typ; _ }
    when not (match typ with Some (Datatype_onbekend _) -> true | _ -> false) ->
      let a = req "attribuut" attribuut in
      let ot = req "objecttype" ot in
      if attr_temporal ctx ~otype:ot ~attr:a then
        fault "temporal attribute %S in a scalar slot is refused (ch. 3.3)" a;
      let v = fresh ctx "A" in
      ( [ Printf.sprintf "val(%s,%s,%s)" subject (str a) v ],
        v,
        pow10 (attr_decimals ctx ~otype:ot ~attr:a) )
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
              ( db
                @ [ Printf.sprintf "param_tl(%s,%s,%s,%s), %s <= %s, %s < %s" (str p) f t v f
                      dt dt t ],
                v,
                pow10 (decimals_of_datatype pr.par_datatype) )
          | None -> fault "unknown parameter %S" p)
      | _ -> fault "unsupported temporal-read basis shape")
  | _ -> fault "expression kind not translated (no corpus case exercises it)"

(* harmonise two scaled terms for comparison *)
let harmonised (b1, t1, s1) (b2, t2, s2) =
  let m = max s1 s2 in
  let scale t s = if s = m then t else Printf.sprintf "(%s*%d)" t (m / s) in
  (b1 @ b2, scale t1 s1, scale t2 s2)

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
  | Kenmerk_check { kenmerk; navigatie = None; negatie; eigenaar = _; _ } ->
      let k = req "kenmerk" kenmerk in
      [ (if negatie then "not " else "") ^ Printf.sprintf "heeft(%s,%s)" subject (str k) ]
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
  (* fail-fast constraints, mirrored on ref/'s faults: integer division
     is floor only on non-negative operands, and an empty pot is the
     ch. 4.4 v0 hole ref/ faults on — make clingo fail identically
     instead of silently diverging *)
  emit ctx ":- is_a(B,%s), val(B,%s,A), A < 0." (str bt) (str ba);
  emit ctx ":- %sgewicht(_,W), W < 0." p;
  emit ctx "%sheeft_pot(B) :- is_a(B,%s), val(B,%s,_)." p (str bt) (str ba);
  emit ctx ":- is_a(B,%s), rol(B,%s,_), not %sheeft_pot(B)." (str bt) (str role) p;
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
  if u.scope_veld <> None then fault "scoped uniqueness not translated";
  let keys = List.map (req "sleutel") u.sleutels in
  let joins label =
    List.mapi
      (fun i k -> Printf.sprintf "val(%s,%s,K%d)" label (str k) i)
      keys
  in
  emit ctx "inconsistent(%s,S1) :- is_a(S1,%s), is_a(S2,%s), S1 != S2, %s, %s." (str name)
    (str subject_ot) (str subject_ot)
    (String.concat ", " (joins "S1"))
    (String.concat ", " (joins "S2"))

(* --- case facts ---------------------------------------------------------- *)

let translate_case ctx (case : Case.t) =
  let doc = ctx.doc in
  Eval.SMap.iter
    (fun label (i : Uchar_ref.World.instance) ->
      emit ctx "is_a(%s,%s)." (str_raw label) (str i.otype);
      Eval.SMap.iter
        (fun aname v ->
          let d = attr_decimals ctx ~otype:i.otype ~attr:aname in
          emit ctx "val(%s,%s,%d)." (str_raw label) (str aname) (scaled_of_value ~decimals:d v))
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
            (fun t -> emit ctx "rol(%s,%s,%s)." (str_raw label) (str canonical) (str_raw t))
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
  if doc.object_creaties <> [] || doc.feit_creaties <> [] then fault "creation not translated";
  if doc.uniciteit_unies <> [] || doc.regel_groepen <> [] then
    fault "uniqueness unions / rule groups not translated";
  let ctx = { doc; buf = Buffer.create 4096; fresh = 0 } in
  emit ctx "%% generated by backends/clingo from regelspraak-ir-v0 + uchar-case-v0.";
  emit ctx "#defined val/3. #defined heeft/2. #defined rol/3. #defined param/2.";
  emit ctx "#defined inconsistent/2. #defined tl/5. #defined param_tl/4.";
  List.iter (translate_regel ctx) doc.resolved_regels;
  List.iteri (translate_verdeling ctx) doc.verdelingen;
  List.iteri (translate_consistentie ctx) doc.consistenties;
  List.iter (translate_uniciteit ctx) doc.uniciteiten;
  translate_case ctx case;
  emit ctx "#show val/3. #show heeft/2. #show inconsistent/2.";
  Buffer.contents ctx.buf
