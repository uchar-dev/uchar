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
  param_tls : Timeline.t SMap.t; (* granular parameters' full timelines (norm name) *)
  as_of : string option; (* ISO date; None = time-free session *)
  locals : Value.t SMap.t;
      (* lexical binders bound per match-arm evaluation (ch. 9.2), by norm
         name; the row-10 card extends this to lambda/function parameters *)
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
        let ot r = Option.value r.rol_objecttype_naam ~default:"" in
        match ft.rollen with
        | [ a; b ] ->
            if norm (ot a) = norm from_type && norm (ot b) = norm to_type then Some b
            else if norm (ot b) = norm from_type && norm (ot a) = norm to_type then Some a
            else None
        | [ r ] when ft.wederkerig ->
            (* the reciprocal shape: the one role relates the type to
               itself, and is both sides of the hop *)
            if norm (ot r) = norm from_type && norm from_type = norm to_type then Some r
            else None
        | _ -> None)
      env.doc.feittypen
  in
  match candidates with
  | [ r ] -> r
  | [] -> Value.fault "no fact type relates %S to %S" from_type to_type
  | _ -> Value.fault "more than one fact type relates %S to %S" from_type to_type

(* the unique fact type relating [from_type] to [to_type], with its
   name and both roles — (fact-type name, role to_type fills, role
   from_type fills). None when no fact type relates them (the linkless
   creation shape); a fault when more than one does — v0 silently
   dropped the back-link there, execution refuses to guess (recorded
   delta, ch. 5.5). Shared with the ASP backend. *)
let connecting_pair env ~from_type ~to_type =
  let candidates =
    List.filter_map
      (fun (ftn, (ft : feittype)) ->
        let ot r = Option.value r.rol_objecttype_naam ~default:"" in
        match ft.rollen with
        | [ a; b ] when norm from_type = norm to_type ->
            (* a self-typed two-role fact type is role-ambiguous: it
               BLOCKS the derivation (the pinned runtime counts both
               roles as candidates and derives null) — the explicit
               verbindendeRol binding is the unambiguous route
               (IR-SPEC ch. 3.3) *)
            if norm (ot a) = norm from_type && norm (ot b) = norm from_type then
              Some `Blocked
            else None
        | [ a; b ] ->
            if norm (ot a) = norm from_type && norm (ot b) = norm to_type then
              Some (`Pair (ftn, b, a))
            else if norm (ot b) = norm from_type && norm (ot a) = norm to_type then
              Some (`Pair (ftn, a, b))
            else None
        | [ r ] when ft.wederkerig ->
            if norm (ot r) = norm from_type && norm from_type = norm to_type then
              Some (`Pair (ftn, r, r))
            else None
        | _ -> None)
      env.doc.feittypen
  in
  match candidates with
  | [] -> None
  | [ `Pair p ] -> Some p
  | _ when norm from_type = norm to_type ->
      (* any plurality on a self-typed pair — including a reciprocal
         beside a blocked two-role shape — is v0's null: no link *)
      None
  | _ ->
      Value.fault "more than one fact type relates %S to %S; the created object's back-link is \
                   ambiguous (ch. 5.5)"
        from_type to_type

(* the explicit role binding (v1 extension, IR-SPEC ch. 2.6): resolve the
   named role of the named fact type. The verifier (WF-047) guarantees
   resolution on verifier-clean input; a dangling binding still faults. *)
let bound_role env (rb : rol_binding) =
  match
    List.find_opt (fun (n, _) -> norm n = norm rb.rb_feittype) env.doc.feittypen
  with
  | None -> Value.fault "role binding names unknown fact type %S" rb.rb_feittype
  | Some (_, (ft : feittype)) -> (
      match
        List.find_opt
          (fun (r : rol) ->
            match r.rol_naam with Some n -> norm n = norm rb.rb_rol | None -> false)
          ft.rollen
      with
      | Some r -> r
      | None ->
          Value.fault "role binding names unknown role %S of fact type %S" rb.rb_rol
            rb.rb_feittype)

(* the storage keys a role's links may sit under, in read-preference
   order: the case may key them by the role's plural or its name *)
let role_keys (r : rol) = List.filter_map (fun k -> k) [ r.rol_meervoud; r.rol_naam ]

(* linked instances over [role] seen from [self]: the first non-empty
   key wins — the same preference the creation writer (World.add_link)
   uses, so created and case-supplied links are read identically *)
let role_links world (self : World.instance) (role : rol) =
  let labels =
    List.fold_left
      (fun acc k -> if acc = [] then World.links self k else acc)
      [] (role_keys role)
  in
  List.map (World.get world) labels

(* linked instances of [to_type] seen from [self]; the connecting role
   comes from the explicit [binding] when the node carries one, and from
   the type-pair derivation otherwise. *)
let linked ?binding env world (self : World.instance) ~to_type =
  role_links world self
    (match binding with
    | Some rb -> bound_role env rb
    | None -> connecting_role env ~from_type:self.otype ~to_type)

(* a role reachable from [self]'s type whose name/plural matches [name] —
   the v0 instance-fold source wart: a pseudo-AttribuutRef carrying the
   sentinel type names a role, resolved against declared roles (the
   WF-034 exception). [to_type], when known (a navigation hop with a
   written endpoint), joins the filter — the v0 by-name derivation
   (verbindendeRolViaNaam) disambiguates same-named roles by target
   type; its best-effort pluralisation is deliberately NOT mirrored
   (a loud refusal beats heuristic name formation — tracked delta). *)
let role_by_name ?to_type env ~self_type ~name =
  let matches (r : rol) =
    ((match r.rol_naam with Some n -> norm n = norm name | None -> false)
    || (match r.rol_meervoud with Some m -> norm m = norm name | None -> false))
    &&
    match to_type with
    | None -> true
    | Some t -> norm (Option.value r.rol_objecttype_naam ~default:"") = norm t
  in
  let hits =
    List.filter_map
      (fun (_, (ft : feittype)) ->
        let ot r = Option.value r.rol_objecttype_naam ~default:"" in
        match ft.rollen with
        | [ a; b ] ->
            if norm (ot a) = norm self_type && matches b then Some b
            else if norm (ot b) = norm self_type && matches a then Some a
            else None
        | [ r ] when ft.wederkerig ->
            if norm (ot r) = norm self_type && matches r then Some r else None
        | _ -> None)
      env.doc.feittypen
  in
  match hits with
  | [ r ] -> r
  | [] -> Value.fault "no role named %S reachable from %S" name self_type
  | _ -> Value.fault "ambiguous role name %S from %S" name self_type

(* the navigated scalar read's hop walk (WF-053, RUNTIME-SEMANTICS
   ch. 5.8): resolve the hop roles — the explicit bindings when the node
   carries them, else the written names, else the type pair — then walk
   the links. An absent link at any hop short-circuits the whole read to
   None = leeg (ch. 1.2); a to-many hop in a scalar slot faults, never a
   silent first pick. *)
let nav_target env world (self : World.instance) ~bindings ~navigatie_rol ~navigatie_pad
    ~endpoint =
  let roles =
    match bindings with
    | Some rbs -> List.map (bound_role env) rbs
    | None -> (
        match navigatie_pad with
        | _ :: _ ->
            let last = List.length navigatie_pad - 1 in
            let step (i, cur, acc) name =
              let name = req "navigation path step" name in
              let to_type = if i = last then Some endpoint else None in
              let r = role_by_name ?to_type env ~self_type:cur ~name in
              (i + 1, Option.value r.rol_objecttype_naam ~default:"", acc @ [ r ])
            in
            let _, _, roles = List.fold_left step (0, self.otype, []) navigatie_pad in
            roles
        | [] -> (
            match navigatie_rol with
            | Some n ->
                [ role_by_name ~to_type:endpoint env ~self_type:self.otype ~name:n ]
            | None -> [ connecting_role env ~from_type:self.otype ~to_type:endpoint ]))
  in
  List.iter
    (fun (r : rol) ->
      if r.rol_meervoudig then
        Value.fault "navigation over the to-many role %S in a scalar slot is refused (WF-053)"
          (Option.value r.rol_naam ~default:"?"))
    roles;
  let rec walk (cur : World.instance) = function
    | [] ->
        (* fail fast on a walk the verifier would reject: a binding chain
           that lands off the declared endpoint must never silently read
           an absent attribute as leeg *)
        if norm cur.otype <> norm endpoint then
          Value.fault "the navigation reached %S, not the declared endpoint %S" cur.otype
            endpoint;
        Some cur
    | (r : rol) :: rest -> (
        match role_links world cur r with
        | [] -> None
        | [ next ] -> walk next rest
        | _ ->
            Value.fault "to-one role %S links more than one instance on %S (ch. 5.5)"
              (Option.value r.rol_naam ~default:"?")
              cur.label)
  in
  walk self roles

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
  | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ }
  | Construct { typ; _ }
  | Match { typ; _ }
  | Binder_ref { typ; _ } ->
      typ

(* the ch. 1.2d empty-equality asymmetry routes on the declared operand
   kind — two empty runtime values cannot reveal it (Arith.compare_op) *)
let static_numeric l r =
  let kind e =
    match expr_typ e with
    | Some (Numeriek _ | Percentage _ | Datum_in_dagen | Datum_tijd) -> Some true
    | Some (Tekst | Booleaan | Enumeratie _ | Sum_type _) -> Some false
    | Some (Datatype_onbekend _) | None -> None
  in
  match kind l with Some b -> Some b | None -> kind r

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
      { attribuut; objecttype = ot; navigatie_rol; navigatie_pad; navigatie_bindingen;
        set = false; typ; _ }
    when not (match typ with Some (Datatype_onbekend _) -> true | _ -> false) ->
      let name = req "AttribuutRef.attribuut" attribuut in
      let ot = req "AttribuutRef.objecttype" ot in
      (* ch. 3.3, mirrored: no point-in-time read exists — a temporal
         attribute in a scalar slot is refused; the only bridges are
         the dedicated collapses (the temporal total handles its own
         argument in eval_functie). [ot] is the type the value lives on:
         the subject itself, or the navigation endpoint. *)
      if Ir_v0.Checks.attr_temporal (objecttype env ot) name then
        Value.fault
          "temporal attribute %S in a scalar context is refused (ch. 3.3; no point-in-time \
           read exists — the collapses are the only bridges)"
          name;
      let navigated =
        navigatie_rol <> None || navigatie_pad <> [] || norm ot <> norm self.otype
      in
      if not navigated then begin
        (match navigatie_bindingen with
        | Some _ ->
            Value.fault "navigation bindings on a reference that does not navigate (WF-053)"
        | None -> ());
        World.attr self name
      end
      else begin
        (* the navigated read (ch. 5.8): walk the to-one hops; an absent
           link anywhere reads the whole operand as leeg *)
        match
          nav_target env world self ~bindings:navigatie_bindingen ~navigatie_rol
            ~navigatie_pad ~endpoint:ot
        with
        | None -> Value.Empty
        | Some target -> World.attr target name
      end
  | Attribuut_ref _ ->
      Value.fault "unsupported AttribuutRef shape (set/sentinel outside a fold)"
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
  | Functie { kind; argumenten; element_objecttype; leegwaarde; filter; dimensie_selectie; granulariteit; bron_rol; typ = _; bron = _ } ->
      eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
        ~dimensie_selectie ~granulariteit ~bron_rol
  | Tekst_reeks { delen; _ } ->
      Arith.concat
        (List.map
           (function
             | Letterlijk { tekst } -> Value.Text (Option.value tekst ~default:"")
             | Ingevoegd { expr = e } -> eval_expr env world self ~err (req "Ingevoegd.expr" e))
           delen)
  | Expr_onopgelost _ ->
      Value.fault "Onopgelost reached in evaluation: this rule fails observably (WF-044)"
  | Constante { naam = Some n; _ } when norm n = norm "Rekendatum" -> (
      (* ch. 8.2: the execution-date constant — the only expression-level
         access to the evaluation date *)
      match env.as_of with
      | Some d -> Value.date_of_iso d
      | None ->
          Value.fault
            "the Rekendatum constant needs an evaluation date; the case carries asOfDate null")
  | Constante { naam = Some n; _ } when norm n = norm "Rekenjaar" ->
      Value.fault "the Rekenjaar constant is not exercised by any corpus case (ch. 8.2)"
  | Dimensie_cel _ | Variabele_ref _ | Constante _ ->
      Value.fault "expression kind not implemented in this card"
  | Temporal_read { basis; datum; typ = _; bron = _ } -> (
      (* ch. 3.3, the point-in-time read: read(tl, d). The basis is never
         evaluated as a scalar (that is the refused shape); it names the
         timeline. An empty date index reads to leeg; a gap reads to leeg
         (ch. 3.1); a case that supplies only the asOfDate-bucket scalar
         cannot serve a read — fault, never a guess. *)
      match eval_expr env world self ~err datum with
      | Value.Empty -> Value.Empty
      | Value.Date d -> (
          match basis with
          | Attribuut_ref
              { attribuut = Some a; objecttype = Some otn; navigatie_rol = None;
                navigatie_pad = []; set = false; _ }
            when norm otn = norm self.otype ->
              if not (Ir_v0.Checks.attr_temporal (objecttype env otn) a) then
                Value.fault "temporal read of %S, which is not a temporal attribute" a;
              (match World.timeline self a with
              | Some tl -> Timeline.read tl d
              | None ->
                  Value.fault
                    "the temporal read of %S needs the full timeline; the case supplies none \
                     (the asOfDate-bucket scalar cannot serve a point read)"
                    a)
          | Parameter_ref { parameter = Some p; _ } -> (
              (match Ir_v0.Checks.find_ci env.doc.parameters p with
              | Some { par_granulariteit = Some _; _ } -> ()
              | Some _ -> Value.fault "temporal read of parameter %S, which is not granular" p
              | None -> Value.fault "unknown parameter %S" p);
              match SMap.find_opt (norm p) env.param_tls with
              | Some tl -> Timeline.read tl d
              | None ->
                  Value.fault
                    "the temporal read of parameter %S needs its timeline in the case" p)
          | _ -> Value.fault "unsupported temporal-read basis shape")
      | v ->
          Value.fault "temporal-read date operand is %s, must be a day-precision date"
            (Value.to_string v))
  | Construct { constructor_; argument; _ } -> (
      (* ch. 9.1: a leeg argument propagates — a constructor value never
         carries a leeg payload; a nullary constructor (argument absent) is
         gevuld by construction *)
      match argument with
      | None -> Value.Ctor (constructor_, None)
      | Some a -> (
          match eval_expr env world self ~err a with
          | Value.Empty -> Value.Empty
          | v -> Value.Ctor (constructor_, Some v)))
  | Match { scrutinee; arms; _ } -> (
      (* ch. 9.2: evaluate the scrutinee; a leeg scrutinee propagates leeg
         (no implicit leeg-arm); otherwise the unique arm for the tag, with
         its binder bound to the payload for this evaluation only *)
      match eval_expr env world self ~err scrutinee with
      | Value.Empty -> Value.Empty
      | Value.Ctor (tag, payload) -> (
          match List.find_opt (fun (c, _, _) -> norm c = norm tag) arms with
          | None ->
              Value.fault
                "match has no arm for constructor %S (WF-063 totality should preclude this)" tag
          | Some (_, binder, body) ->
              let locals =
                match (binder, payload) with
                | None, None -> env.locals
                | Some b, Some v -> SMap.add (norm b) v env.locals
                | Some _, None ->
                    Value.fault "match arm for nullary constructor %S carries a binder (WF-063)" tag
                | None, Some _ ->
                    Value.fault "match arm for unary constructor %S lacks a binder (WF-063)" tag
              in
              eval_expr { env with locals } world self ~err body)
      | v -> Value.fault "match scrutinee is %s, not a constructor value" (Value.to_string v))
  | Binder_ref { naam; _ } -> (
      (* ch. 9.2: the nearest enclosing binder; WF-064 makes the miss
         statically impossible, so an unbound read is a fail-fast fault *)
      match SMap.find_opt (norm naam) env.locals with
      | Some v -> v
      | None -> Value.fault "unbound binder %S (WF-064 should preclude this)" naam)

and eval_functie env world self ~err ~kind ~argumenten ~element_objecttype ~leegwaarde ~filter
    ~dimensie_selectie ~granulariteit ~bron_rol =
  (* the die/dat subselection executes on the instance folds below; the
     SAME wire field also carries the §7.1 gedurende timeline clip (a
     filter on the temporal total, where elementObjecttype is the
     subject itself) — that form must never run as an instance
     subselection and faults in its own arm *)
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
      (match filter with
      | Some c -> (
          (* ch. 3.6, the gedurende clip: intersect the value timeline
             with the condition's true-periods, then total whole units
             per clipped segment (floor, never pro-rata). A gap in the
             condition is clipped away; a fully clipped-away — or
             absent — value timeline totals 0, never leeg (the clip's
             presence turns absence into 0, the mirrored v0 nuance).
             The clip needs the FULL value timeline: a case that
             carries only the asOfDate-bucket scalar cannot serve it. *)
          let tl =
            match World.timeline self a with
            | Some tl -> tl
            | None ->
                if World.attr self a = Value.Empty then []
                else
                  Value.fault
                    "the gedurende clip of %S needs the full timeline; the case supplies only \
                     the asOfDate-bucket scalar"
                    a
          in
          let span = List.map (fun (s : Timeline.segment) -> (s.s_van, s.s_tot)) tl in
          let rec true_periods_of_cond (c : conditie) =
            match c with
            | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie; _ } ->
                if not (kenmerk_temporal ot_decl k) then
                  Value.fault
                    "gedurende over %S, which is not a temporal characteristic (WF-055)" k;
                let periods =
                  Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
                in
                if negatie then Timeline.complement periods ~span else periods
            | Vergelijking
                { op; links = Some (Attribuut_ref
                    { attribuut = Some b; objecttype = Some botn; navigatie_rol = None;
                      navigatie_pad = []; set = false; _ });
                  rechts = Some grens; periode = None; _ }
              when norm botn = norm self.otype
                   && Ir_v0.Checks.attr_temporal ot_decl b ->
                (* the compared timeline is evaluated PER SEGMENT against
                   the scalar bound (piecewise-constant, never per day) *)
                let tested =
                  match World.timeline self b with Some t -> t | None -> []
                in
                let bound = eval_expr env world self ~err grens in
                Timeline.true_periods_of tested ~span (fun v ->
                    Arith.compare_op op v bound)
            | Samengesteld { kwantor = Alle; onderdelen = (_ :: _ as onderdelen); _ } ->
                (* ch. 3.6, the FLAT composite ALLE clip: the
                   intersection of the members' true periods — v0 nests
                   the per-member clips over exactly such flat
                   characteristic lists. Members are restricted to
                   own-temporal-characteristic checks (WF-055): v0
                   partitions STATIC comparison members out as
                   value-side guards before its clip ever runs, markers
                   temporal-attribute comparisons and nested composite
                   bullets — our v1 narrows composite members to the
                   characteristic checks both executors carry (the
                   static-guard form and the nested bullet are recorded
                   loud flanks). *)
                let member_periods part =
                  match part with
                  | Kenmerk_check _ -> true_periods_of_cond part
                  | _ ->
                      Value.fault
                        "a composite gedurende member is an own temporal characteristic \
                         check (WF-055; the static-guard and nested forms are recorded \
                         flanks)"
                in
                List.fold_left
                  (fun acc part -> Timeline.intersect_periods acc (member_periods part))
                  (member_periods (List.hd onderdelen))
                  (List.tl onderdelen)
            | _ ->
                Value.fault
                  "unsupported gedurende condition shape (WF-055: an own temporal \
                   characteristic, a per-segment comparison of an own temporal attribute, \
                   or an ALLE composite of such shapes)"
          in
          let true_periods = true_periods_of_cond c in
          Timeline.totaal gran (Timeline.clip tl true_periods))
      | None -> (
      if World.timeline self a <> None then
        Value.fault
          "the temporal total of %S over a case-supplied full timeline is not exercised by any \
           corpus case; the asOfDate-bucket convention carries the total"
          a;
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
              Timeline.totaal gran [ { Timeline.s_van = van; s_tot = tot; s_waarde = v } ]))))
  | _ ->
  (* non-temporal aggregation *)
  (* contributors: an instance fold over a role (element_objecttype set),
     or listed operand expressions *)
  let contributors () =
    (* the die/dat subselection (§13.4.2): the filter condition evaluates
       PER CONTRIBUTOR — the element instance is the condition's subject,
       never the outer fold subject. A contributor whose filter operand
       is leeg simply fails the condition (ch. 1.2d) and drops out. *)
    let keep (i : World.instance) =
      match filter with None -> true | Some c -> eval_cond env world i ~err c
    in
    (* fold-source shapes are navigation-free — the same narrowing the
       verifier's WF-047/048 detection uses; a navigated source falls to
       the fail-fast arm below *)
    match (element_objecttype, argumenten) with
    | ( Some el,
        [ Attribuut_ref
            { attribuut; typ = Some (Datatype_onbekend _);
              navigatie_rol = None; navigatie_pad = []; _ } ] ) ->
        (* the row-1 wart: the fold source argument names a role; with an
           explicit binding the name check is the verifier's (WF-047) *)
        (match bron_rol with
        | Some _ -> ()
        | None ->
            let (_ : rol) =
              role_by_name env ~self_type:self.otype ~name:(req "fold source" attribuut)
            in
            ());
        `Instances (List.filter keep (linked ?binding:bron_rol env world self ~to_type:el))
    | ( Some el,
        [ Attribuut_ref { attribuut; set = true; navigatie_rol = None; navigatie_pad = []; _ } ]
      ) ->
        let name = req "fold attribute" attribuut in
        `Values
          (List.map
             (fun i -> World.attr i name)
             (List.filter keep (linked ?binding:bron_rol env world self ~to_type:el)))
    | None, args ->
        if filter <> None then
          Value.fault "a filter on a listed-operand fold has no v0 form (the subselection \
                       ranges over instances)";
        `Values (List.map (eval_expr env world self ~err) args)
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

and eval_cond env world (self : World.instance) ~err (c : conditie) : bool =
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
  | Kenmerk_check { kenmerk; navigatie = None; negatie; navigatie_binding; _ } ->
      (match navigatie_binding with
      | Some _ ->
          Value.fault
            "navigation binding on a characteristic check that does not navigate (WF-053)"
      | None -> ());
      let k = req "KenmerkCheck.kenmerk" kenmerk in
      if kenmerk_temporal (objecttype env self.otype) k then
        Value.fault "temporal characteristic %S in a scalar condition is refused (ch. 3.3)" k;
      let has = World.has_char self k in
      if negatie then not has else has
  | Kenmerk_check { kenmerk; navigatie = Some nav; negatie; navigatie_binding; _ } -> (
      (* the navigated check (ch. 5.8): one to-one hop. The v0 asymmetry,
         mirrored deliberately: an ABSENT link makes the check false for
         the positive AND the negated form — unlike comparison and
         leeg/gevuld, which treat the absent relation as a leegwaarde. *)
      let k = req "KenmerkCheck.kenmerk" kenmerk in
      let role =
        match navigatie_binding with
        | Some rb -> bound_role env rb
        | None -> role_by_name env ~self_type:self.otype ~name:nav
      in
      if role.rol_meervoudig then
        Value.fault
          "the navigated check reaches the to-many role %S in a scalar condition (WF-053)"
          (Option.value role.rol_naam ~default:"?");
      match role_links world self role with
      | [] -> false
      | [ target ] ->
          if kenmerk_temporal (objecttype env target.otype) k then
            Value.fault "temporal characteristic %S in a scalar condition is refused (ch. 3.3)"
              k;
          let has = World.has_char target k in
          if negatie then not has else has
      | _ ->
          Value.fault "to-one role %S links more than one instance on %S (ch. 5.5)"
            (Option.value role.rol_naam ~default:"?")
            self.label)
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

(* ch. 3.7: the pro-rata target — the one implemented temporal write.
   The value MUST be tijdsevenredig-deel over an own temporal attribute
   with a gedurende condition on an own temporal characteristic (the
   corpus shape; everything else faults). The base must be as coarse as
   or coarser than the bucket, and the target's declared granularity
   must equal the node's. An absent base timeline or a condition
   without true periods writes the EMPTY timeline (v0: a null base
   yields the empty list — the target exists, with no segments). *)
let run_pro_rata env w (self : World.instance) ~ot ~attr waarde =
  let ot_decl = objecttype env ot in
  match waarde with
  | Functie
      { kind = Tijdsevenredig_deel;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some b; objecttype = Some bot; navigatie_rol = None;
                navigatie_pad = []; set = false; _ } ];
        filter = Some c; granulariteit = Some g; leegwaarde = None; dimensie_selectie = None; _ }
    when norm bot = norm ot ->
      let target_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit attr with
        | Some (Some tg) -> tg
        | _ -> Value.fault "pro-rata target %S without a granularity" attr
      in
      if target_gran <> g then
        Value.fault
          "pro-rata bucket granularity differs from the target's declared granularity (WF-056)";
      let base_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit b with
        | Some (Some bg) -> bg
        | _ -> Value.fault "pro-rata base %S is not a temporal attribute (WF-056)" b
      in
      if base_gran < g then
        Value.fault "pro-rata base granularity is finer than the bucket (WF-056)";
      (match Ir_v0.Checks.find_ci ot_decl.attributen b with
      | Some (Some (Numeriek { eenheid = Some _; _ } | Percentage { eenheid = Some _; _ })) ->
          (* ch. 3.7: a rate base restates by its exact factor FIRST —
             not executed here; refuse rather than prorate factor-free *)
          Value.fault "pro-rata over a unit-carrying base is not executed (no corpus case)"
      | _ -> ());
      let true_periods =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
            if not (kenmerk_temporal ot_decl k) then
              Value.fault "gedurende over %S, which is not a temporal characteristic (WF-056)" k;
            Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
        | _ ->
            Value.fault
              "unsupported pro-rata condition shape (an own temporal characteristic; no corpus \
               case for more)"
      in
      let basis = Option.value (World.timeline self b) ~default:[] in
      let result = Timeline.pro_rata g basis true_periods in
      (* ch. 7.8, the timeline arm: on a standing temporal target the
         fold decides only when its result is gevuld — an empty result
         (the temporal leeg) writes nothing and a case-supplied input
         timeline stands whole *)
      if
        result = []
        && List.mem (norm ot, norm attr)
             (Ir_v0.Desugar.standing_temporal_targets env.doc)
      then (w, false)
      else World.write_timeline w ~label:self.label ~attr result
  | Functie
      { kind = Aantal_dagen_in; argumenten = [];
        filter = Some c; granulariteit = Some g; leegwaarde = None; dimensie_selectie = None; _ }
    ->
      (* ch. 3.8 "aantal dagen in": per touched bucket the true-day
         count; WF-058 mirrors WF-056's facets — bucket equals the
         target's declared granularity, gedurende over an own temporal
         characteristic *)
      let target_gran =
        match Ir_v0.Checks.find_ci ot_decl.attribuut_granulariteit attr with
        | Some (Some tg) -> tg
        | _ -> Value.fault "day-count target %S without a granularity" attr
      in
      if target_gran <> g then
        Value.fault
          "day-count bucket granularity differs from the target's declared granularity (WF-058)";
      let true_periods =
        match c with
        | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
            if not (kenmerk_temporal ot_decl k) then
              Value.fault "gedurende over %S, which is not a temporal characteristic (WF-058)" k;
            Option.value (SMap.find_opt (norm k) self.World.ctls) ~default:[]
        | _ ->
            Value.fault
              "unsupported day-count condition shape (an own temporal characteristic; no corpus \
               case for more)"
      in
      let result = Timeline.aantal_dagen_in g true_periods in
      (* the ch. 7.8 timeline arm, as for pro-rata *)
      if
        result = []
        && List.mem (norm ot, norm attr)
             (Ir_v0.Desugar.standing_temporal_targets env.doc)
      then (w, false)
      else World.write_timeline w ~label:self.label ~attr result
  | _ ->
      Value.fault
        "a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a \
         gedurende condition over an own temporal characteristic); no corpus case for more"

let run_rule env world (r : resolved_regel) =
  if not (applies env r.rr_geldigheid) then (world, false)
  else
    match r.doel with
    | Some (Attribuut_ref { attribuut; objecttype = Some ot; _ }) ->
        let attr = req "doel.attribuut" attribuut in
        let temporal_target =
          match r.rr_soort with
          | Some Kenmerktoekenning -> false
          | _ -> Ir_v0.Checks.attr_temporal (objecttype env ot) attr
        in
        (* ch. 3.11: the only implemented temporal write is the ch. 3.7
           pro-rata target; every other temporal target refuses — never
           silently coerce a scalar into a timeline *)
        (match r.rr_soort with
        | Some Kenmerktoekenning ->
            if kenmerk_temporal (objecttype env ot) attr then
              Value.fault "temporal characteristic %S: temporal writes are not implemented" attr
        | _ ->
            if temporal_target then
              match r.waarde with
              | Some (Functie { kind = Tijdsevenredig_deel | Aantal_dagen_in; _ }) -> ()
              | _ ->
                  Value.fault
                    "temporal attribute %S as a target: temporal writes are not implemented"
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
              | Some (Gelijkstelling | Berekening) when temporal_target ->
                  run_pro_rata env w self ~ot ~attr (req "waarde" r.waarde)
              | Some (Gelijkstelling | Berekening) ->
                  let v = eval_expr env w self ~err (req "waarde" r.waarde) in
                  (* ch. 7.8: on a standing target — and on a ch. 7.7
                     desugaring-domain target, whose ladder reading ref
                     replays natively — the rule decides like a rung: a
                     leeg evaluation writes nothing (the init/input
                     decides); channel deposits of the evaluation
                     persist as in any ordinary evaluation *)
                  if
                    v = Value.Empty
                    && (List.mem (norm ot, norm attr)
                          (Ir_v0.Desugar.standing_targets env.doc)
                       || List.mem (norm ot, norm attr)
                            (Ir_v0.Desugar.desugared_targets env.doc))
                  then (w, false)
                  else
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
          let recipients = linked ?binding:v.verbindende_rol env w src ~to_type:doel_type in
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

(* ch. 5.5, relation creation: for each anchor whose condition holds,
   navigate the join chain (anchor → tussenstappen → joinRol → target)
   over existing links, and link anchor and target in the created
   relation — both ends are written, so navigation from either side
   sees the link. Naturally idempotent: an already-present link is a
   no-op write, so a target reached along two paths links once. Only
   the symbolic extension form (IR-SPEC ch. 3.4, WF-028) carries the
   relational content; a bean-field-only v0 record faults. *)
let run_feit_creatie env world (fc : resolved_feit_creatie) =
  if not (applies env fc.fc_geldigheid) then (world, false)
  else begin
    let name = Option.value fc.fc_naam ~default:"<feitcreatie>" in
    let ftn =
      match fc.fc_feittype with
      | Some f -> f
      | None ->
          Value.fault
            "fact creation %S carries only bean fields; the symbolic form (IR-SPEC ch. 3.4) is \
             what executes"
            name
    in
    let role rname = bound_role env { rb_feittype = ftn; rb_rol = rname } in
    let anker_rol = role (req "FeitCreatie.ankerRol" fc.anker_rol) in
    let doel_rol = role (req "FeitCreatie.doelRol" fc.doel_rol) in
    over_subjects world ~otype:(req "FeitCreatie.anker" fc.anker) ~rule:name
      (fun w self err ->
        let fire =
          match fc.fc_voorwaarde with Some c -> eval_cond env w self ~err c | None -> true
        in
        if not fire then (w, false)
        else
          let hop insts rb =
            List.concat_map (fun i -> role_links w i (bound_role env rb)) insts
          in
          let mids =
            List.fold_left
              (fun insts (t : tussenstap) -> hop insts (req "Tussenstap.rolBinding" t.ts_rol))
              [ self ] fc.tussenstappen
          in
          let targets = hop mids (req "FeitCreatie.joinRol" fc.join_rol) in
          List.fold_left
            (fun (w, ch) (t : World.instance) ->
              let w, c1 =
                World.add_link w ~label:self.label ~keys:(role_keys doel_rol)
                  ~many:doel_rol.rol_meervoudig ~target:t.label
              in
              let w, c2 =
                World.add_link w ~label:t.label ~keys:(role_keys anker_rol)
                  ~many:anker_rol.rol_meervoudig ~target:self.label
              in
              (w, ch || c1 || c2))
            (w, false) targets)
  end

(* ch. 5.5, non-recursive object creation: for each subject whose
   condition holds, mint a deterministic instance of the created type,
   apply the member initialisations (values evaluated on the subject;
   characteristic constants), and link new object and subject over the
   uniquely-derivable connecting relation — both ends. The idempotence
   contract, mirrored from the pinned runtime: a 1:1 shape (the
   created-side role is to-one) guards on the absence of an already
   linked instance in the INPUT world — a case-supplied linked instance
   blocks the creation (the snapshot narrowing is a recorded delta:
   v0's guard is a live fact-base negation) — while the to-many and
   linkless shapes guard per (rule × subject) through the
   deterministic label. *)
let run_object_creatie env ~world0 ~idx world (oc : resolved_object_creatie) =
  if not (applies env oc.oc_geldigheid) then (world, false)
  else begin
    let name = Option.value oc.oc_naam ~default:"<objectcreatie>" in
    let nieuw = req "ObjectCreatie.nieuwType" oc.nieuw_type in
    let subject = req "ObjectCreatie.subject" oc.oc_subject in
    let pair =
      (* the explicit back-link binding (extension, IR-SPEC ch. 3.3)
         wins over the type-pair derivation — the only route for a
         self-typed pair, whose derivation mirrors v0's null *)
      match oc.oc_verbindende_rol with
      | Some rb -> (
          match Ir_v0.Checks.resolve_binding { doc = env.doc } rb with
          | Ok (_, bound, other) -> Some (rb.rb_feittype, bound, other)
          | Error e -> Value.fault "%s" e)
      | None -> connecting_pair env ~from_type:subject ~to_type:nieuw
    in
    over_subjects world ~otype:subject ~rule:name (fun w self err ->
        let fire =
          match oc.oc_voorwaarde with Some c -> eval_cond env w self ~err c | None -> true
        in
        if not fire then (w, false)
        else begin
          (* keyed by the creation INDEX, never the (nullable) name —
             the same key the ASP backend's ocN(Subject) term uses, so
             two name-less creations can never collide into one label;
             the case loader reserves the "::" namespace for mints *)
          let label = Printf.sprintf "oc%d::%s" idx self.label in
          let blocked =
            match pair with
            | Some (_, new_role, _) when not new_role.rol_meervoudig -> (
                match SMap.find_opt self.label world0.World.instances with
                | Some i0 -> role_links world0 i0 new_role <> []
                | None -> false)
            | _ -> false
          in
          if blocked || SMap.mem label w.World.instances then (w, false)
          else begin
            let w =
              World.add_instance w
                { World.label; otype = nieuw; attrs = SMap.empty; chars = [];
                  links = SMap.empty; tls = SMap.empty; ctls = SMap.empty }
            in
            let w =
              List.fold_left
                (fun w (t : toekenning) ->
                  let m = req "Toekenning.attribuut" t.toe_attribuut in
                  if t.toe_kenmerk then (
                    match t.toe_waarde with
                    | Some (Literal { waarde = Some "waar"; _ }) ->
                        fst (World.set_char w ~label ~char:m)
                    | Some (Literal { waarde = Some "onwaar"; _ }) -> w
                    | _ ->
                        Value.fault
                          "characteristic initialisation of %S is not a boolean constant (WF-027)"
                          m)
                  else
                    let v = eval_expr env w self ~err (req "Toekenning.waarde" t.toe_waarde) in
                    fst
                      (World.write_attr w ~label ~attr:m
                         ~decl:(attr_decl env ~otype:nieuw ~attr:m)
                         v))
                w oc.toekenningen
            in
            let w =
              match pair with
              | None -> w
              | Some (_, new_role, subj_role) ->
                  let w, _ =
                    World.add_link w ~label:self.label ~keys:(role_keys new_role)
                      ~many:new_role.rol_meervoudig ~target:label
                  in
                  let w, _ =
                    World.add_link w ~label ~keys:(role_keys subj_role)
                      ~many:subj_role.rol_meervoudig ~target:self.label
                  in
                  w
            in
            (w, true)
          end
        end)
  end

(* ch. 7.1/7.2, the ladder group on one target (a singleton group is
   the plain ladder): each member decides independently — highest rung
   that decides wins, with fall-through and speculative deposits per
   ch. 7.1 — then the group acts once: equal decisions write once;
   differing decisions write nothing (the target keeps what the fact
   base holds) and deposit one §9.5 fact keyed by the target form
   (ch. 7.2, an end-state observation). The strata machinery (WF-051)
   has already put every justification and consequence producer
   strictly below every member. *)
let run_ladder_group env world (members : default_ladder list) =
  let l0 = List.hd members in
  let target_form =
    l0.dl_onderwerp ^ "."
    ^
    match (l0.dl_kenmerk, l0.dl_attribuut) with
    | Some k, _ -> k
    | _, Some a -> a
    | _ -> "?"
  in
  (match l0.dl_attribuut with
  | Some a when Ir_v0.Checks.attr_temporal (objecttype env l0.dl_onderwerp) a ->
      Value.fault
        "temporal attribute %S as a ladder target: temporal writes are not implemented" a
  | _ -> ());
  over_subjects world ~otype:l0.dl_onderwerp ~rule:target_form (fun w self cond_err ->
      let speculate f =
        let buf = ref [] in
        let r = f (fun m -> buf := m :: !buf) in
        (r, fun () -> List.iter cond_err (List.rev !buf))
      in
      (* one member's decision, its speculative deposits flushed only
         when it decides *)
      let member_decision (l : default_ladder) =
        if not (applies env l.dl_geldigheid) then None
        else
          let justified (r : rung) err =
            if r.rung_initialisatie then (
              match l.dl_attribuut with
              | Some a -> World.attr self a = Value.Empty
              | None -> Value.fault "initialisation rung on a characteristic ladder")
            else eval_cond env w self ~err (req "rung justification" r.rung_voorwaarde)
          in
          let rec decide = function
            | [] -> None
            | (r : rung) :: rest -> (
                match l.dl_kenmerk with
                | Some _ ->
                    let j, flush = speculate (fun err -> justified r err) in
                    if j then (
                      flush ();
                      Some (`Char (req "rung besluit" r.rung_besluit)))
                    else decide rest
                | None -> (
                    let outcome, flush =
                      speculate (fun err ->
                          if justified r err then
                            Some
                              (eval_expr env w self ~err
                                 (req "rung consequentie" r.rung_consequentie))
                          else None)
                    in
                    match outcome with
                    | Some v when v <> Value.Empty ->
                        flush ();
                        Some (`Attr v)
                    | Some _ (* leeg: falls through *) | None -> decide rest))
          in
          decide l.rungs
      in
      let decisions = List.filter_map member_decision members in
      match decisions with
      | [] -> (w, false)
      | d0 :: rest ->
          let equal_dec a b =
            match (a, b) with
            | `Char x, `Char y -> x = y
            | `Attr x, `Attr y -> Arith.equal x y
            | _ -> false
          in
          if List.for_all (equal_dec d0) rest then (
            match d0 with
            | `Char true ->
                World.set_char w ~label:self.label ~char:(req "ladder kenmerk" l0.dl_kenmerk)
            | `Char false -> (w, false)
            | `Attr v ->
                let a = req "ladder attribuut" l0.dl_attribuut in
                World.write_attr w ~label:self.label ~attr:a
                  ~decl:(attr_decl env ~otype:l0.dl_onderwerp ~attr:a)
                  v)
          else
            (* over-determination (ch. 7.2): no write, one channel fact *)
            (World.insert_error w ~rule:target_form ~subject:self.label, false))

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

(* ch. 5.9 decision (2026-08-20, extended to BOTH uniqueness forms):
   a temporal key reads EVER-uniqueness — an identity, not a
   measurement: every value the timeline EVER carries is claimed,
   colliding across instances even over non-overlapping periods.
   Same-instance redeclaration is no collision: the tuple set is
   deduplicated per instance (× source, in the union). At most one
   temporal component per key list — a second would need the
   same-moment tuple join, loud until a corpus case lands. A temporal
   key needs its full timeline; a bucket scalar would silently miss
   values. One shared function carries the single form and the union
   (compositionality: the same key, the same reading, either form). *)
let ever_tuples env ~otype ~keys (i : World.instance) : string list =
  let ot_decl = objecttype env otype in
  let temporal = List.filter (Ir_v0.Checks.attr_temporal ot_decl) keys in
  if List.length temporal > 1 then
    Value.fault
      "a uniqueness key list carries at most one temporal key component (the same-moment \
       tuple join has no corpus case; ch. 5.9)";
  let carried k =
    if not (Ir_v0.Checks.attr_temporal ot_decl k) then [ World.attr i k ]
    else
      match World.timeline i k with
      | Some tl -> List.map (fun (s : Timeline.segment) -> s.s_waarde) tl
      | None ->
          if World.attr i k <> Value.Empty then
            Value.fault
              "the ever-unique key %S needs the full timeline; %S supplies only a scalar \
               (ch. 5.9)"
              k i.label
          else []
  in
  (* the tuple set: scalars paired with every value the one temporal
     component ever carries; an empty component drops the tuple (the
     v0 empty-key rule) *)
  let rec tuples acc = function
    | [] -> [ List.rev acc ]
    | k :: rest ->
        List.concat_map
          (fun v -> if v = Value.Empty then [] else tuples (v :: acc) rest)
          (carried k)
  in
  tuples [] keys
  |> List.map (fun vs -> String.concat "\x00" (List.map Value.to_string vs))
  |> List.sort_uniq String.compare

let run_uniciteit env world (u : resolved_uniciteit) =
  if not (applies env u.uni_geldigheid) then world
  else begin
    let name = Option.value u.uni_naam ~default:"<uniciteit>" in
    let subject = req "Uniciteit.subject" u.uni_subject in
    (* the scope hop of a parent-scoped uniqueness (ch. 5.9, WF-054):
       keys are unique within the sub-population sharing one scope
       parent; an instance without a scope parent does not participate —
       the pinned runtime's silent narrowing, mirrored and recorded *)
    let scope_of =
      match (u.scope_rol, u.scope_veld) with
      | Some rb, _ ->
          let role = bound_role env rb in
          Some (fun i -> role_links world i role)
      | None, Some sv ->
          let role = role_by_name env ~self_type:subject ~name:sv in
          Some (fun i -> role_links world i role)
      | None, None -> None
    in
    let keys = List.map (req "sleutel") u.sleutels in
    (* a duplicate non-empty key (ch. 5.6): rows with an empty key
       component do not participate; temporal keys read ever-uniqueness
       through the shared tuple function, composed with the scope
       prefix (scope parent × ever-tuples) *)
    let keyed =
      List.concat_map
        (fun (i : World.instance) ->
          (* the key-shape guards run on EVERY instance of the type,
             before scope participation: a temporal key supplied as a
             bucket scalar is a form error that screams regardless of
             whether the row would have participated (data integrity
             precedes the scope narrowing — and the ASP side checks
             the same way, at translation) *)
          let tuples = ever_tuples env ~otype:subject ~keys i in
          let prefix =
            match scope_of with
            | None -> Some ""
            | Some links -> (
                match links i with
                | [] -> None (* scope-less: does not participate *)
                | [ (p : World.instance) ] -> Some (p.label ^ "\x00")
                | _ ->
                    Value.fault
                      "to-one scope role links more than one instance on %S (ch. 5.5)"
                      i.label)
          in
          match prefix with
          | None -> []
          | Some pre -> List.map (fun key -> (pre ^ key, i.label)) tuples)
        (World.of_type world subject)
    in
    let dup k = List.length (List.filter (fun (k', _) -> String.equal k k') keyed) > 1 in
    List.fold_left
      (fun w (k, label) -> if dup k then World.insert_error w ~rule:name ~subject:label else w)
      world keyed
  end

(* ch. 5.9, the uniqueness union: the key tuples of ALL sources form one
   shared uniqueness domain — a duplicate may arise across two different
   source types; tuples compare positionally; rows with an empty key
   component do not participate; every instance whose tuple occurs more
   than once in the union deposits one channel fact under the shared
   union name. *)
let run_uniciteit_unie env world (u : resolved_uniciteit_unie) =
  if not (applies env u.uu_geldigheid) then world
  else begin
    let name = Option.value u.uu_naam ~default:"<uniciteitsunie>" in
    if List.length u.bronnen < 2 then
      Value.fault
        "a uniqueness union carries at least two sources (WF-030; the v0 resolver never \
         produces fewer)";
    (* one row per (instance × source) — v0 SUMS the per-source counts,
       so an instance reached through two sources contributes its tuple
       twice and duplicates against itself, exactly like the runtime *)
    let keyed =
      List.concat_map
        (fun (b : uniciteit_unie_bron) ->
          let t = req "UniciteitUnie.bron.type" b.uub_type in
          let keys = List.map (req "sleutel") b.uub_sleutels in
          (* the shared ever-tuple function (ch. 5.9): one reading for
             the union and the single form alike *)
          List.concat_map
            (fun (i : World.instance) ->
              List.map (fun key -> (key, i.label)) (ever_tuples env ~otype:t ~keys i))
            (World.of_type world t))
        u.bronnen
    in
    let dup k = List.length (List.filter (fun (k', _) -> String.equal k k') keyed) > 1 in
    List.fold_left
      (fun w (k, label) -> if dup k then World.insert_error w ~rule:name ~subject:label else w)
      world keyed
  end

(* --- the session (ch. 5.7) --------------------------------------------- *)

(* ch. 8.4: the Parameterset session-start coverage check. Every granular
   parameter the document reads at the evaluation date (a ch. 8.3 vintage
   read: TemporalRead of a ParameterRef indexed by Rekendatum) must have
   a timeline in the session that covers that date — checked before any
   rule fires; a missing vintage is a configuration error, never a
   mid-evaluation leeg. Point reads at data-dependent indices are
   deliberately outside this check (ch. 8.4). Shared with the ASP
   backend, which runs it at translation time: data-plane validation,
   not evaluation semantics. *)
let vintage_read_params (doc : document) : string list =
  let acc = ref [] in
  let rec expr (e : expr) =
    match e with
    | Temporal_read
        { basis = Parameter_ref { parameter = Some p; _ };
          datum = Constante { naam = Some n; _ }; _ }
      when norm n = norm "Rekendatum" ->
        if not (List.exists (fun q -> norm q = norm p) !acc) then acc := p :: !acc
    | Temporal_read { basis; datum; _ } ->
        expr basis;
        expr datum
    | Literal _ | Attribuut_ref _ | Parameter_ref _ | Variabele_ref _ | Constante _
    | Expr_onopgelost _ | Binder_ref _ ->
        ()
    | Tekst_reeks { delen; _ } ->
        List.iter (function Ingevoegd { expr = Some e } -> expr e | _ -> ()) delen
    | Binair { links; rechts; _ } -> opt links; opt rechts
    | Functie { argumenten; leegwaarde; filter; _ } ->
        List.iter expr argumenten;
        opt leegwaarde;
        copt filter
    | Afgerond { basis; _ } -> opt basis
    | Begrensd { basis; minimum; maximum; _ } -> opt basis; opt minimum; opt maximum
    | Dimensie_cel { attribuut; _ } -> opt attribuut
    | Construct { argument; _ } -> opt argument
    | Match { scrutinee; arms; _ } ->
        expr scrutinee;
        List.iter (fun (_, _, body) -> expr body) arms
  and opt = function Some e -> expr e | None -> ()
  and cond (c : conditie) =
    match c with
    | Vergelijking { links; rechts; _ } -> opt links; opt rechts
    | Predicaat { operand; _ } -> opt operand
    | Kenmerk_check _ | Rollidmaatschap _ | Verre_rollidmaatschap _ | Regelversie _
    | Conditie_onopgelost _ ->
        ()
    | Periode_conditie { vanaf; tot; _ } -> opt vanaf; opt tot
    | Samengesteld { onderdelen; _ } -> List.iter cond onderdelen
  and copt = function Some c -> cond c | None -> ()
  in
  List.iter
    (fun (r : resolved_regel) ->
      opt r.doel;
      opt r.waarde;
      copt r.rr_voorwaarde)
    doc.resolved_regels;
  List.iter (fun (v : resolved_verdeling) -> copt v.vd_voorwaarde) doc.verdelingen;
  List.iter (fun (c : resolved_consistentie) -> copt c.assertie) doc.consistenties;
  List.iter (fun (d : resolved_dagsoort) -> copt d.rd_voorwaarde) doc.resolved_dagsoorten;
  List.iter (fun (fc : resolved_feit_creatie) -> copt fc.fc_voorwaarde) doc.feit_creaties;
  List.iter
    (fun (oc : resolved_object_creatie) ->
      List.iter (fun (t : toekenning) -> opt t.toe_waarde) oc.toekenningen;
      copt oc.oc_voorwaarde)
    doc.object_creaties;
  !acc

let parameterset_check env =
  List.iter
    (fun p ->
      match env.as_of with
      | None ->
          Value.fault
            "session start: the document vintage-reads parameter %S, but the session carries \
             no evaluation date (asOfDate is null) — ch. 8.4"
            p
      | Some d -> (
          let day =
            match Value.date_of_iso d with Value.Date z -> z | _ -> assert false
          in
          match SMap.find_opt (norm p) env.param_tls with
          | None ->
              Value.fault
                "session start: the document vintage-reads parameter %S, but the Parameterset \
                 carries no timeline for it — ch. 8.4"
                p
          | Some tl ->
              if Timeline.read tl day = Value.Empty then
                Value.fault
                  "session start: the Parameterset timeline of %S does not cover the \
                   evaluation date %s — ch. 8.4"
                  p d))
    (vintage_read_params env.doc)

let fixpoint ~budget step world =
  let rec go n world =
    if n > budget then
      Value.fault "no fixpoint within %d passes — divergence is a fault (ch. 5.7)" budget
    else
      let world, changed = step world in
      if changed then go (n + 1) world else world
  in
  go 0 world

(* The input-precision fence: a case value whose exact rational does not
   fit the attribute's/parameter's declared decimals is refused at the
   evaluation boundary — fail fast, with the same "does not fit N
   decimals" reason the ASP backend's integer scaling faults with, so
   the two executors refuse identically instead of ref silently keeping
   precision the declaration cannot hold. *)
let validate_input_precision env world =
  let fits ~decimals q =
    Z.equal (Q.den (Q.mul q (Q.of_bigint (Z.pow (Z.of_int 10) decimals)))) Z.one
  in
  let check ~what dt (v : Value.t) =
    match (dt, v) with
    | Some (Numeriek { decimalen; _ }), Value.Num (q, _)
    | Some (Percentage { decimalen; _ }), Value.Num (q, _) ->
        if not (fits ~decimals:decimalen q) then
          Value.fault "input %s = %s does not fit %d decimals" what (Value.lexeme_of_q q) decimalen
    | _ -> ()
  in
  SMap.iter
    (fun _ (i : World.instance) ->
      let decl attr = Option.join (Ir_v0.Checks.find_ci (objecttype env i.otype).attributen attr) in
      SMap.iter (fun a v -> check ~what:(i.label ^ "." ^ a) (decl a) v) i.attrs;
      SMap.iter
        (fun a (tl : Timeline.t) ->
          List.iter
            (fun (s : Timeline.segment) -> check ~what:(i.label ^ "." ^ a) (decl a) s.s_waarde)
            tl)
        i.tls)
    world.World.instances;
  let param_decl p =
    match Ir_v0.Checks.find_ci env.doc.parameters p with
    | Some (par : parameter) -> par.par_datatype
    | None -> None
  in
  SMap.iter (fun p v -> check ~what:("parameter " ^ p) (param_decl p) v) env.params;
  SMap.iter
    (fun p (tl : Timeline.t) ->
      List.iter
        (fun (s : Timeline.segment) -> check ~what:("parameter " ^ p) (param_decl p) s.s_waarde)
        tl)
    env.param_tls

let evaluate env world =
  (* fail fast on unverified input: the cycle-tolerant strata below are
     sound only on WF-045/046-clean documents (every cycle then passes
     through a recursive-group creation and carries no negative edge) —
     ref/ re-runs those two checks itself rather than silently
     answering on a graph the verifier would reject *)
  (match Ir_v0.Checks.wf045_046 { doc = env.doc } with
  | [] -> ()
  | { Ir_v0.Checks.wf; at; reason } :: _ ->
      Value.fault "%s at %s: %s — the dependency graph is ill-formed; ref/ refuses to evaluate"
        wf at reason);
  parameterset_check env;
  validate_input_precision env world;
  (* the input-world snapshot the 1:1 creation guard reads (ch. 5.5) *)
  let world0 = world in
  (* Strata (ch. 6.3), computed from the same dependency graph the
     verifier checks (Ir_v0.Checks.dep_forms — shared by construction,
     so ref/ and WF-045/046 cannot drift). Every form gets the smallest
     stratum that puts it at or above its positive producers and
     strictly above its negative ones — except along a dependency
     cycle: on verifier-clean input a cycle is WF-045-admissible (it
     passes through a recursive-group creation), and ch. 6.3
     discharges strictness there by the one-generation-per-instance
     progression, not a boundary — cycle edges count positive. Failure
     to converge is a cycle the verifier should have rejected. *)
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
  (* reachability over the form graph, for the cycle test: an edge
     p -> c lies on a cycle iff p is reachable from c *)
  let succ = Array.make n [] in
  Array.iteri
    (fun ci (f : Ir_v0.Checks.dep_form) ->
      List.iter
        (fun (node, _) ->
          List.iter
            (fun pi -> if not (List.mem ci succ.(pi)) then succ.(pi) <- ci :: succ.(pi))
            (writers node))
        f.df_reads)
    arr;
  let reachable = Array.init n (fun _ -> Array.make n false) in
  Array.iteri
    (fun src _ ->
      let rec go i =
        if not reachable.(src).(i) then begin
          reachable.(src).(i) <- true;
          List.iter go succ.(i)
        end
      in
      List.iter go succ.(src))
    arr;
  let on_cycle ~producer ~consumer = reachable.(consumer).(producer) in
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
                  strata.(pi)
                  +
                  match kind with
                  | Ir_v0.Checks.Dep_pos -> 0
                  | _ -> if on_cycle ~producer:pi ~consumer:ci then 0 else 1
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
  let ladder_stratum i = stratum_of (Printf.sprintf "/defaultLadders/%d" i) in
  let fc_stratum i = stratum_of (Printf.sprintf "/feitCreaties/%d" i) in
  let oc_stratum i = stratum_of (Printf.sprintf "/objectCreaties/%d" i) in
  let cons_stratum i = stratum_of (Printf.sprintf "/consistenties/%d" i) in
  let uni_stratum i = stratum_of (Printf.sprintf "/uniciteiten/%d" i) in
  let uu_stratum i = stratum_of (Printf.sprintf "/uniciteitUnies/%d" i) in
  let indexed l = List.mapi (fun i x -> (i, x)) l in
  (* ladder groups (ch. 7.2): members share one (subject, target); a
     group runs at the max of its members' strata *)
  let ladder_groups =
    let key (l : default_ladder) =
      (* the kind discriminant keeps a name-sharing attribute and
         characteristic (WF-002) in separate groups *)
      (if l.dl_kenmerk <> None then "K:" else "A:")
      ^ norm l.dl_onderwerp ^ ":"
      ^
      match (l.dl_kenmerk, l.dl_attribuut) with
      | Some k, _ -> norm k
      | _, Some a -> norm a
      | _ -> "?"
    in
    List.fold_left
      (fun acc (i, l) ->
        let k = key l in
        match List.assoc_opt k acc with
        | Some _ ->
            List.map
              (fun (k', (s, ls)) ->
                if k' = k then (k', (max s (ladder_stratum i), ls @ [ l ])) else (k', (s, ls)))
              acc
        | None -> acc @ [ (k, (ladder_stratum i, [ l ])) ])
      []
      (indexed env.doc.default_ladders)
    |> List.map snd
  in
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
    let world, c3 =
      List.fold_left
        (fun (w, ch) (stratum, members) ->
          if stratum > k then (w, ch)
          else
            let w, c = run_ladder_group env w members in
            (w, ch || c))
        (world, false) ladder_groups
    in
    let world, c4 =
      List.fold_left
        (fun (w, ch) (i, fc) ->
          if fc_stratum i > k then (w, ch)
          else
            let w, c = run_feit_creatie env w fc in
            (w, ch || c))
        (world, false)
        (indexed env.doc.feit_creaties)
    in
    let world, c5 =
      List.fold_left
        (fun (w, ch) (i, oc) ->
          if oc_stratum i > k then (w, ch)
          else
            let w, c = run_object_creatie env ~world0 ~idx:i w oc in
            (w, ch || c))
        (world, false)
        (indexed env.doc.object_creaties)
    in
    (world, c1 || c2 || c3 || c4 || c5)
  in
  (* the ch. 5.5/5.7 bounded budget: a recursive chain advances one
     generation per pass, so this also caps runaway recursion — an
     order of magnitude above any legitimate corpus fixpoint (~7
     passes), and deliberately EQUAL to the ASP backend's oc_budget so
     both executors fault a runaway chain at the same generation
     (clingo's nested creation terms cap the reachable depth anyway:
     its term handling aborts near depth ~200 — recorded in the
     backend README) *)
  let budget = 100 in
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
        (indexed env.doc.uniciteiten);
    world :=
      List.fold_left
        (fun w (i, u) -> if uu_stratum i = k then run_uniciteit_unie env w u else w)
        !world
        (indexed env.doc.uniciteit_unies)
  done;
  !world
