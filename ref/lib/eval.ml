(* The evaluation session of RUNTIME-SEMANTICS ch. 5: forward chaining to
   the least fixpoint, with freshness realised as naive re-evaluation
   passes and idempotence at the write boundary (World.write_attr). A
   bounded pass budget turns divergence into a fault.

   Negation and channel reads are staged as v0's single boundary
   (ch. 5.3): the positive part quiesces first, then negation consumers
   join, then consistency/uniqueness fire at that fixpoint, then channel
   readers (regelversie-inconsistent) run to a fixpoint with the checks
   re-fired once. The general condition — stratification as
   well-formedness — is ch. 6 and out of this card's scope.

   Coverage is deliberately partial: constructs no golden exercises yet
   (navigation paths, dimension cells, filters, timelines, object/fact
   creation, day kinds) fault with context instead of guessing — fail
   fast, never a silent fallback. *)

open Ir_v0.Ast
module SMap = World.SMap

let norm = Ir_v0.Checks.norm

type env = {
  doc : document;
  params : Value.t SMap.t; (* by norm name *)
  as_of : string option; (* ISO date; None = time-free session *)
}

let req what = function Some x -> x | None -> Value.fault "missing %s in IR node" what

(* --- validity gate (ch. 3.10 / 5.6): a version whose window excludes the
   evaluation date simply does not apply --------------------------------- *)

let applies env (g : geldigheid option) =
  match g with
  | None | Some { geld_vanaf = None; geld_tot = None } -> true
  | Some { geld_vanaf; geld_tot } -> (
      match env.as_of with
      | None ->
          Value.fault "a validity-bounded form needs an evaluation date; this session is time-free"
      | Some d ->
          (match geld_vanaf with Some v -> String.compare v d <= 0 | None -> true)
          && (match geld_tot with Some t -> String.compare d t <= 0 | None -> true))

(* --- name resolution against the document ------------------------------ *)

let objecttype env name =
  match Ir_v0.Checks.find_ci env.doc.objecttypen name with
  | Some ot -> ot
  | None -> Value.fault "unknown object type %S" name

let attr_decl env ~otype ~attr =
  let ot = objecttype env otype in
  match Ir_v0.Checks.find_ci ot.attributen attr with
  | Some dt -> dt
  | None -> Value.fault "unknown attribute %S on %S" attr otype

let kenmerk_temporal (ot : objecttype) name =
  List.exists (fun (k, _) -> norm k = norm name) ot.kenmerk_granulariteit

(* the unique fact type connecting two object types; deriving it — rather
   than naming the role in the IR — is the recorded v0 wart of ch. 4.1,
   mirrored here for both allocation recipients and instance folds *)
let connecting_role env ~from_type ~to_type =
  let candidates =
    List.filter_map
      (fun (_, (ft : feittype)) ->
        match ft.rollen with
        | [ a; b ] ->
            let ot r = Option.value r.rol_objecttype_naam ~default:"" in
            if norm (ot a) = norm from_type && norm (ot b) = norm to_type then Some b
            else if norm (ot b) = norm from_type && norm (ot a) = norm to_type then Some a
            else None
        | _ -> None)
      env.doc.feittypen
  in
  match candidates with
  | [ r ] -> r
  | [] -> Value.fault "no fact type relates %S to %S" from_type to_type
  | _ -> Value.fault "more than one fact type relates %S to %S" from_type to_type

(* linked instances of [to_type] seen from [self]; the case may key the
   link under the role's name or its plural — try both *)
let linked env world (self : World.instance) ~to_type =
  let role = connecting_role env ~from_type:self.otype ~to_type in
  let keys = List.filter_map (fun k -> k) [ role.rol_meervoud; role.rol_naam ] in
  let labels =
    List.fold_left (fun acc k -> if acc = [] then World.links self k else acc) [] keys
  in
  List.map (World.get world) labels

(* a role reachable from [self]'s type whose name/plural matches [name] —
   the v0 instance-fold source wart: a pseudo-AttribuutRef carrying the
   sentinel type names a role, resolved against declared roles (the
   WF-034 exception) *)
let role_by_name env ~self_type ~name =
  let matches (r : rol) =
    (match r.rol_naam with Some n -> norm n = norm name | None -> false)
    || (match r.rol_meervoud with Some m -> norm m = norm name | None -> false)
  in
  let hits =
    List.filter_map
      (fun (_, (ft : feittype)) ->
        match ft.rollen with
        | [ a; b ] ->
            let ot r = Option.value r.rol_objecttype_naam ~default:"" in
            if norm (ot a) = norm self_type && matches b then Some b
            else if norm (ot b) = norm self_type && matches a then Some a
            else None
        | _ -> None)
      env.doc.feittypen
  in
  match hits with
  | [ r ] -> r
  | [] -> Value.fault "no role named %S reachable from %S" name self_type
  | _ -> Value.fault "ambiguous role name %S from %S" name self_type

(* --- literals ----------------------------------------------------------- *)

let unit_of_datatype = function
  | Some (Numeriek { eenheid = Some e; _ }) | Some (Percentage { eenheid = Some e; _ }) ->
      Value.Unit e
  | _ -> Value.Unitless

let literal_value (waarde : string option) (typ : datatype option) =
  let lex = req "Literal.waarde" waarde in
  match typ with
  | Some (Numeriek _ | Percentage _) | None ->
      Value.Num (Value.q_of_lexeme lex, unit_of_datatype typ)
  | Some Booleaan -> (
      match norm lex with
      | "waar" -> Value.Bool true
      | "onwaar" -> Value.Bool false
      | _ -> Value.fault "malformed boolean literal %S" lex)
  | Some Tekst -> Value.Text lex
  | Some (Enumeratie _) -> Value.Enum lex
  | Some Datum_in_dagen -> Value.date_of_iso lex
  | Some _ -> Value.fault "literal of this datatype not implemented: %S" lex

(* --- expression evaluation --------------------------------------------- *)

(* [err] deposits a §9.5 error reason for the evaluating rule × subject *)
let rec eval_expr env world (self : World.instance) ~err (e : expr) : Value.t =
  match e with
  | Literal { waarde; typ; _ } -> literal_value waarde typ
  | Parameter_ref { parameter; _ } -> (
      let name = req "ParameterRef.parameter" parameter in
      (match Ir_v0.Checks.find_ci env.doc.parameters name with
      | Some { par_granulariteit = Some _; _ } ->
          (* v0 refuses a granular parameter in a scalar slot (the §8.2
             marker family); how a rule reads a vintage is ch. 8 scope *)
          Value.fault "parameter %S is granular; reading a vintage in a rule is ch. 8 scope" name
      | Some _ -> ()
      | None -> Value.fault "unknown parameter %S" name);
      match SMap.find_opt (norm name) env.params with
      | Some v -> v
      | None ->
          Value.fault "parameter %S is unbound in this case (binding is fail-fast, ch. 5.7)" name)
  | Attribuut_ref
      { attribuut; objecttype = ot; navigatie_rol = None; navigatie_pad = []; set = false; typ; _ }
    when not (match typ with Some (Datatype_onbekend _) -> true | _ -> false) ->
      let name = req "AttribuutRef.attribuut" attribuut in
      let ot = req "AttribuutRef.objecttype" ot in
      if norm ot <> norm self.otype then
        Value.fault "navigation from %S to %S is not implemented in this card" self.otype ot
      else begin
        (* ch. 3.3, mirrored: no point-in-time read exists — a temporal
           attribute in a scalar slot is refused; the only bridges are
           the dedicated collapses (the temporal total handles its own
           argument in eval_functie) *)
        if Ir_v0.Checks.attr_temporal (objecttype env ot) name then
          Value.fault
            "temporal attribute %S in a scalar context is refused (ch. 3.3; no point-in-time \
             read exists — the collapses are the only bridges)"
            name;
        World.attr self name
      end
  | Attribuut_ref _ ->
      Value.fault "unsupported AttribuutRef shape (navigation/set/sentinel outside a fold)"
  | Binair { op; links; rechts; _ } -> (
      let l = eval_expr env world self ~err (req "Binair.links" links) in
      let r = eval_expr env world self ~err (req "Binair.rechts" rechts) in
      match op with
      | Plus -> Arith.add l r
      | Min -> Arith.sub_min l r
      | Verminderd_met -> Arith.sub_verminderd l r
      | Maal -> Arith.mul l r
      | Gedeeld_door -> Arith.div_ ~err l r
      | Gedeeld_door_abs -> Arith.div_abs ~err l r
      | Tot_de_macht -> Arith.pow_ l r
      | Percentage_van -> Arith.pct_of l r)
  | Afgerond { basis; modus; decimalen; _ } -> (
      match basis with
      | Some (Functie { kind = Wortel; argumenten = [ arg ]; _ }) ->
          (* the mandatory rounding after a root (§13.4.17.13) rounds the
             true irrational, exactly *)
          Arith.sqrt_rounded ~err modus decimalen (eval_expr env world self ~err arg)
      | _ ->
          Arith.round_v modus decimalen (eval_expr env world self ~err (req "Afgerond.basis" basis)))
  | Begrensd { basis; minimum; maximum; _ } ->
      let v = eval_expr env world self ~err (req "Begrensd.basis" basis) in
      let bound = function None -> Value.Empty | Some b -> eval_expr env world self ~err b in
      Arith.clamp ~min_:(bound minimum) ~max_:(bound maximum) v
  | Functie { kind; argumenten; element_objecttype; leegwaarde; filter; dimensie_selectie; granulariteit; typ = _; bron = _ } ->
      eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
        ~dimensie_selectie ~granulariteit
  | Tekst_reeks { delen; _ } ->
      Arith.concat
        (List.map
           (function
             | Letterlijk { tekst } -> Value.Text (Option.value tekst ~default:"")
             | Ingevoegd { expr = e } -> eval_expr env world self ~err (req "Ingevoegd.expr" e))
           delen)
  | Expr_onopgelost _ ->
      Value.fault "Onopgelost reached in evaluation: this rule fails observably (WF-044)"
  | Dimensie_cel _ | Variabele_ref _ | Constante _ ->
      Value.fault "expression kind not implemented in this card"

and eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
    ~dimensie_selectie ~granulariteit =
  if filter <> None then Value.fault "aggregation filter not implemented in this card";
  if dimensie_selectie <> None then Value.fault "dimension selection not implemented in this card";
  if granulariteit <> None then Value.fault "granularity-bucketed aggregation not implemented in this card";
  match (kind, argumenten) with
  | ( Totaal,
      [ Attribuut_ref { attribuut = Some a; objecttype = Some aot; navigatie_rol = None; navigatie_pad = []; _ } ] )
    when norm aot = norm self.otype
         && Ir_v0.Checks.attr_temporal (objecttype env aot) a -> (
      (* ch. 3.6, the temporal total — the one v0-mirrored bridge from a
         timeline to a scalar. The case carries the asOfDate bucket's
         value (corpus/golden/README.md); the collapse folds that
         one-bucket timeline: value × whole-units of its own grid. *)
      let ot_decl = objecttype env aot in
      let gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit a with
        | Some (Some g) -> g
        | _ -> Value.fault "temporal attribute %S without a granularity" a
      in
      match env.as_of with
      | None ->
          Value.fault
            "the temporal total of %S needs the evaluation date: the case carries the asOfDate \
             bucket's value"
            a
      | Some d -> (
          match World.attr self a with
          | Value.Empty -> Value.Empty (* absent timeline totals to leeg (ch. 3.6) *)
          | v ->
              let day = match Value.date_of_iso d with Value.Date z -> z | _ -> assert false in
              let van, tot = Timeline.bucket gran day in
              Timeline.totaal gran [ { Timeline.s_van = van; s_tot = tot; s_waarde = v } ]))
  | _ ->
  (* non-temporal aggregation *)
  (* contributors: an instance fold over a role (element_objecttype set),
     or listed operand expressions *)
  let contributors () =
    match (element_objecttype, argumenten) with
    | Some el, [ Attribuut_ref { attribuut; typ = Some (Datatype_onbekend _); _ } ] ->
        (* the row-1 wart: the fold source argument names a role *)
        let (_ : rol) = role_by_name env ~self_type:self.otype ~name:(req "fold source" attribuut) in
        `Instances (linked env world self ~to_type:el)
    | Some el, [ Attribuut_ref { attribuut; set = true; _ } ] ->
        let name = req "fold attribute" attribuut in
        `Values (List.map (fun i -> World.attr i name) (linked env world self ~to_type:el))
    | None, args -> `Values (List.map (eval_expr env world self ~err) args)
    | Some _, _ -> Value.fault "unsupported instance-fold source shape"
  in
  let values () =
    match contributors () with
    | `Values vs -> vs
    | `Instances _ -> Value.fault "value aggregation over a bare role source"
  in
  let fallback = Option.map (eval_expr env world self ~err) leegwaarde in
  match kind with
  | Aantal -> (
      (* a count, never empty (ch. 1.2e) *)
      match contributors () with
      | `Instances is -> Arith.agg_aantal is
      | `Values vs -> Arith.agg_aantal (Arith.filled vs))
  | Som -> Arith.agg_som ~fallback (values ())
  | Totaal -> Arith.agg_totaal (values ())
  | Minimum -> Arith.agg_min (values ())
  | Maximum -> Arith.agg_max (values ())
  | Eerste -> Arith.agg_eerste (values ())
  | Laatste -> Arith.agg_laatste (values ())
  | Wortel -> (
      match argumenten with
      | [ a ] -> Arith.sqrt_ ~err (eval_expr env world self ~err a)
      | _ -> Value.fault "wortel expects one argument")
  | Absolute_waarde -> (
      match argumenten with
      | [ a ] -> Arith.abs_ (eval_expr env world self ~err a)
      | _ -> Value.fault "absolute waarde expects one argument")
  | Concatenatie -> Arith.concat (List.map (eval_expr env world self ~err) argumenten)
  | Jaar_uit -> (
      match argumenten with
      | [ a ] -> Arith.jaar_uit (eval_expr env world self ~err a)
      | _ -> Value.fault "jaar uit expects one argument")
  | Tijdsduur -> (
      match argumenten with
      | [ a; b ] ->
          Arith.tijdsduur (eval_expr env world self ~err a) (eval_expr env world self ~err b)
      | _ -> Value.fault "tijdsduur expects two arguments")
  | _ -> Value.fault "function kind not implemented in this card"

(* --- condition evaluation ---------------------------------------------- *)

(* the static type an expression node carries on the wire *)
let expr_typ : expr -> datatype option = function
  | Literal { typ; _ }
  | Tekst_reeks { typ; _ }
  | Binair { typ; _ }
  | Functie { typ; _ }
  | Afgerond { typ; _ }
  | Begrensd { typ; _ }
  | Attribuut_ref { typ; _ }
  | Dimensie_cel { typ; _ }
  | Parameter_ref { typ; _ }
  | Variabele_ref { typ; _ }
  | Constante { typ; _ }
  | Expr_onopgelost { typ; _ } ->
      typ

(* the ch. 1.2d empty-equality asymmetry routes on the declared operand
   kind — two empty runtime values cannot reveal it (Arith.compare_op) *)
let static_numeric l r =
  let kind e =
    match expr_typ e with
    | Some (Numeriek _ | Percentage _ | Datum_in_dagen | Datum_tijd) -> Some true
    | Some (Tekst | Booleaan | Enumeratie _) -> Some false
    | Some (Datatype_onbekend _) | None -> None
  in
  match kind l with Some b -> Some b | None -> kind r

let rec eval_cond env world (self : World.instance) ~err (c : conditie) : bool =
  match c with
  | Vergelijking { op; links; rechts; periode = None; _ } ->
      let le = req "Vergelijking.links" links and re = req "Vergelijking.rechts" rechts in
      let l = eval_expr env world self ~err le in
      let r = eval_expr env world self ~err re in
      Arith.compare_op ?static_numeric:(static_numeric le re) op l r
  | Vergelijking _ -> Value.fault "whole-period comparison is ch. 3 scope"
  | Predicaat { soort = Leeg; operand; negatie; _ } ->
      let v = eval_expr env world self ~err (req "Predicaat.operand" operand) in
      let is_empty = v = Value.Empty in
      if negatie then not is_empty else is_empty
  | Predicaat { soort = Gevuld; operand; negatie; _ } ->
      let v = eval_expr env world self ~err (req "Predicaat.operand" operand) in
      let is_filled = v <> Value.Empty in
      if negatie then not is_filled else is_filled
  | Predicaat _ -> Value.fault "predicate kind not implemented in this card"
  | Kenmerk_check { kenmerk; navigatie = None; negatie; _ } ->
      let k = req "KenmerkCheck.kenmerk" kenmerk in
      if kenmerk_temporal (objecttype env self.otype) k then
        Value.fault "temporal characteristic %S in a scalar condition is refused (ch. 3.3)" k;
      let has = World.has_char self k in
      if negatie then not has else has
  | Kenmerk_check _ -> Value.fault "navigated characteristic check not implemented in this card"
  | Samengesteld { kwantor; drempel; onderdelen; _ } -> (
      let hits = List.length (List.filter (eval_cond env world self ~err) onderdelen) in
      let n = List.length onderdelen in
      match kwantor with
      | De | Alle -> hits = n
      | Geen -> hits = 0
      | Ten_minste -> hits >= drempel
      | Ten_hoogste -> hits <= drempel
      | Precies -> hits = drempel)
  | Regelversie { regel; inconsistent = true; _ } ->
      World.channel_has world ~rule:(req "Regelversie.regel" regel)
  | Regelversie { inconsistent = false; _ } ->
      Value.fault
        "'is gevuurd' is not implemented (row 30: decided as a dialect-only fixpoint predicate)"
  | Rollidmaatschap _ | Verre_rollidmaatschap _ | Periode_conditie _ ->
      Value.fault "condition kind not implemented in this card"
  | Conditie_onopgelost _ -> Value.fault "Onopgelost condition reached in evaluation (WF-044)"

let rule_name (r : resolved_regel) = Option.value r.rr_naam ~default:"<naamloos>"

(* --- rule forms --------------------------------------------------------- *)

(* run one form over its subjects; [body] gets the fresh instance and an
   error collector, returns (world, changed); collected §9.5 reasons land
   as channel facts for (rule × subject) *)
let over_subjects world ~otype ~rule body =
  List.fold_left
    (fun (w, changed) (snapshot : World.instance) ->
      let self = World.get w snapshot.label in
      let pending = ref [] in
      let err reason = pending := reason :: !pending in
      let w, ch = body w self err in
      let w =
        List.fold_left
          (fun w (_ : string) -> World.insert_error w ~rule ~subject:self.label)
          w !pending
      in
      (w, changed || ch))
    (world, false) (World.of_type world otype)

let run_rule env world (r : resolved_regel) =
  if not (applies env r.rr_geldigheid) then (world, false)
  else
    match r.doel with
    | Some (Attribuut_ref { attribuut; objecttype = Some ot; _ }) ->
        let attr = req "doel.attribuut" attribuut in
        (* ch. 3.11: temporal targets are not implemented — refuse, never
           silently coerce a scalar into a timeline *)
        (match r.rr_soort with
        | Some Kenmerktoekenning ->
            if kenmerk_temporal (objecttype env ot) attr then
              Value.fault "temporal characteristic %S: temporal writes are not implemented" attr
        | _ ->
            if Ir_v0.Checks.attr_temporal (objecttype env ot) attr then
              Value.fault "temporal attribute %S as a target: temporal writes are not implemented"
                attr);
        over_subjects world ~otype:ot ~rule:(rule_name r) (fun w self err ->
            let fire =
              match r.rr_voorwaarde with Some c -> eval_cond env w self ~err c | None -> true
            in
            if not fire then (w, false)
            else
              match r.rr_soort with
              | Some Kenmerktoekenning -> (
                  match eval_expr env w self ~err (req "waarde" r.waarde) with
                  | Value.Bool true -> World.set_char w ~label:self.label ~char:attr
                  | Value.Bool false -> (w, false)
                  | v -> Value.fault "characteristic value is not boolean: %s" (Value.to_string v))
              | Some (Gelijkstelling | Berekening) ->
                  let v = eval_expr env w self ~err (req "waarde" r.waarde) in
                  World.write_attr w ~label:self.label ~attr ~decl:(attr_decl env ~otype:ot ~attr) v
              | Some Initialisatie ->
                  (* writes only when the target is empty (ch. 5.4) *)
                  if World.attr self attr <> Value.Empty then (w, false)
                  else
                    let v = eval_expr env w self ~err (req "waarde" r.waarde) in
                    World.write_attr w ~label:self.label ~attr ~decl:(attr_decl env ~otype:ot ~attr)
                      v
              | None -> Value.fault "rule %S has no kind" (rule_name r))
    | _ -> Value.fault "rule %S: unsupported target shape" (rule_name r)

let run_verdeling env world (v : resolved_verdeling) =
  if not (applies env v.vd_geldigheid) then (world, false)
  else begin
    let name = Option.value v.vd_naam ~default:"<verdeling>" in
    let bron_type = req "bronType" v.bron_type in
    let doel_type = req "doelType" v.doel_type in
    let bron_attr = req "bronAttribuut" v.bron_attribuut in
    let doel_attr = req "doelAttribuut" v.doel_attribuut in
    over_subjects world ~otype:bron_type ~rule:name (fun w src cond_err ->
        let cond_ok =
          match v.vd_voorwaarde with
          | Some c -> eval_cond env w src ~err:cond_err c
          | None -> true
        in
        if not cond_ok then (w, false)
        else
          let recipients = linked env w src ~to_type:doel_type in
          let recs =
            List.map
              (fun (i : World.instance) ->
                {
                  Alloc.id = i.label;
                  weight = Option.map (fun a -> World.attr i a) v.rato_attribuut;
                  cap = Option.map (fun a -> World.attr i a) v.maximum_attribuut;
                  key = Option.map (fun a -> World.attr i a) v.groepeer_attribuut;
                })
              recipients
          in
          let alloc_errs = ref [] in
          let err ~id (_ : string) = alloc_errs := id :: !alloc_errs in
          let outcome =
            Alloc.allocate
              (Option.value v.vd_methode ~default:Complex)
              ~afnemend:v.afnemend ~decimals:v.afronding_decimalen
              ~pot:(World.attr src bron_attr) ~err recs
          in
          let w =
            List.fold_left (fun w id -> World.insert_error w ~rule:name ~subject:id) w !alloc_errs
          in
          match outcome with
          | Alloc.Nothing -> (w, false)
          | Alloc.Distributed (shares, rest) ->
              let decl = attr_decl env ~otype:doel_type ~attr:doel_attr in
              let w, ch =
                List.fold_left
                  (fun (w, ch) (id, share) ->
                    match share with
                    | Alloc.Excluded -> (w, ch) (* stays leeg, never 0 *)
                    | Alloc.Assigned q ->
                        let w, c =
                          World.write_attr w ~label:id ~attr:doel_attr ~decl
                            (Value.Num (q, Value.Unitless))
                        in
                        (w, ch || c))
                  (w, false) shares
              in
              let w, ch2 =
                match v.restant_attribuut with
                | None -> (w, false) (* undeclared: the rest lapses (a SHOULD, no WF rule) *)
                | Some ra ->
                    World.write_attr w ~label:src.label ~attr:ra
                      ~decl:(attr_decl env ~otype:bron_type ~attr:ra)
                      (Value.Num (rest, Value.Unitless))
              in
              (w, ch || ch2))
  end

(* consistency and uniqueness fire at the computed fixpoint; violations
   are idempotent channel facts (ch. 5.6) *)
let run_consistentie env world (c : resolved_consistentie) =
  if not (applies env c.cons_geldigheid) then world
  else
    let name = Option.value c.cons_naam ~default:"<consistentie>" in
    let world, _ =
      over_subjects world ~otype:(req "Consistentie.subject" c.cons_subject) ~rule:name
        (fun w self err ->
          if eval_cond env w self ~err (req "assertie" c.assertie) then (w, false)
          else (World.insert_error w ~rule:name ~subject:self.label, false))
    in
    world

let run_uniciteit env world (u : resolved_uniciteit) =
  if not (applies env u.uni_geldigheid) then world
  else begin
    let name = Option.value u.uni_naam ~default:"<uniciteit>" in
    let subject = req "Uniciteit.subject" u.uni_subject in
    if u.scope_veld <> None then Value.fault "scoped uniqueness not implemented in this card";
    let keys = List.map (req "sleutel") u.sleutels in
    (* a duplicate non-empty key (ch. 5.6): rows with an empty key
       component do not participate *)
    let keyed =
      List.filter_map
        (fun (i : World.instance) ->
          let vs = List.map (World.attr i) keys in
          if List.exists (fun v -> v = Value.Empty) vs then None
          else Some (String.concat "\x00" (List.map Value.to_string vs), i.label))
        (World.of_type world subject)
    in
    let dup k = List.length (List.filter (fun (k', _) -> String.equal k k') keyed) > 1 in
    List.fold_left
      (fun w (k, label) -> if dup k then World.insert_error w ~rule:name ~subject:label else w)
      world keyed
  end

(* --- the session (ch. 5.7) --------------------------------------------- *)

let fixpoint ~budget step world =
  let rec go n world =
    if n > budget then
      Value.fault "no fixpoint within %d passes — divergence is a fault (ch. 5.7)" budget
    else
      let world, changed = step world in
      if changed then go (n + 1) world else world
  in
  go 0 world

let evaluate env world =
  if env.doc.object_creaties <> [] || env.doc.feit_creaties <> [] then
    Value.fault "object/fact creation is not implemented in this card";
  if env.doc.uniciteit_unies <> [] then Value.fault "uniqueness unions not implemented in this card";
  if env.doc.regel_groepen <> [] then Value.fault "rule groups are not implemented in this card";
  (* Strata (ch. 6.3), computed from the same dependency graph the
     verifier checks (Ir_v0.Checks.dep_forms — shared by construction,
     so ref/ and WF-045/046 cannot drift). Every form gets the smallest
     stratum that puts it at or above its positive producers and
     strictly above its negative ones; failure to converge is a
     dependency cycle the verifier should have rejected. *)
  let dforms = Ir_v0.Checks.dep_forms { doc = env.doc } in
  let n = List.length dforms in
  let arr = Array.of_list dforms in
  let strata = Array.make n 0 in
  let writers node =
    List.concat
      (List.mapi
         (fun i (f : Ir_v0.Checks.dep_form) ->
           if List.mem node f.df_writes then [ i ] else [])
         dforms)
  in
  let changed = ref true in
  let rounds = ref 0 in
  while !changed do
    incr rounds;
    if !rounds > n + 2 then
      Value.fault "no evaluation strata exist — a dependency cycle (WF-045/046) reached ref/";
    changed := false;
    Array.iteri
      (fun ci (f : Ir_v0.Checks.dep_form) ->
        List.iter
          (fun (node, kind) ->
            List.iter
              (fun pi ->
                let need =
                  strata.(pi) + match kind with Ir_v0.Checks.Dep_pos -> 0 | _ -> 1
                in
                if strata.(ci) < need then begin
                  strata.(ci) <- need;
                  changed := true
                end)
              (writers node))
          f.df_reads)
      arr
  done;
  let stratum_by_at = Hashtbl.create 16 in
  Array.iteri (fun i (f : Ir_v0.Checks.dep_form) -> Hashtbl.replace stratum_by_at f.df_at i) arr;
  let stratum_of at = match Hashtbl.find_opt stratum_by_at at with Some i -> strata.(i) | None -> 0 in
  let kmax = Array.fold_left max 0 strata in
  let rule_stratum i = stratum_of (Printf.sprintf "/resolvedRegels/%d" i) in
  let verdeling_stratum i = stratum_of (Printf.sprintf "/verdelingen/%d" i) in
  let cons_stratum i = stratum_of (Printf.sprintf "/consistenties/%d" i) in
  let uni_stratum i = stratum_of (Printf.sprintf "/uniciteiten/%d" i) in
  let indexed l = List.mapi (fun i x -> (i, x)) l in
  let pass ~k world =
    let world, c1 =
      List.fold_left
        (fun (w, ch) (i, r) ->
          if rule_stratum i > k then (w, ch)
          else
            let w, c = run_rule env w r in
            (w, ch || c))
        (world, false)
        (indexed env.doc.resolved_regels)
    in
    let world, c2 =
      List.fold_left
        (fun (w, ch) (i, v) ->
          if verdeling_stratum i > k then (w, ch)
          else
            let w, c = run_verdeling env w v in
            (w, ch || c))
        (world, false)
        (indexed env.doc.verdelingen)
    in
    (world, c1 || c2)
  in
  let budget = 10_000 in
  (* for k = 0, 1, …: the forms of stratum <= k evaluate to their
     fixpoint before any form of stratum k+1 fires (ch. 6.3); the
     consistency and uniqueness forms of stratum k fire once at that
     fixpoint — their channel facts are idempotent (ch. 5.6) *)
  let world = ref world in
  for k = 0 to kmax do
    world := fixpoint ~budget (pass ~k) !world;
    world :=
      List.fold_left
        (fun w (i, c) -> if cons_stratum i = k then run_consistentie env w c else w)
        !world
        (indexed env.doc.consistenties);
    world :=
      List.fold_left
        (fun w (i, u) -> if uni_stratum i = k then run_uniciteit env w u else w)
        !world
        (indexed env.doc.uniciteiten)
  done;
  !world
