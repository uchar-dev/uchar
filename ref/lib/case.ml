(* Loader for uchar-case-v0 files (spec/schema/uchar-case-v0.schema.json).

   Gate 4 already enforces the JSON shape; this loader owns the
   cross-checks the schema's README assigns to the ref loader: label
   uniqueness, declared object types / attributes / characteristics /
   parameters, value variants matching the declared datatypes, role keys
   resolving to declared roles with correctly typed targets, and to-one
   roles carrying exactly one label. Any violation is a load error
   (exit 2 territory), never a silent skip.

   Values are unit-normalised to the referenced declaration's unit
   (schema contract), so loading attaches the declared unit verbatim. *)

open Ir_v0.Ast
module SMap = World.SMap

let norm = Ir_v0.Checks.norm

exception Error of string

let fail fmt = Printf.ksprintf (fun s -> raise (Error s)) fmt

type expectation =
  | Attribute_value of { label : string; attr : string; value : Value.t }
  | Attribute_empty of { label : string; attr : string }
  | Attribute_timeline of { label : string; attr : string; tl : Timeline.t }
  | Char_present of { label : string; char : string }
  | Char_absent of { label : string; char : string }
  | Consistent
  | Inconsistent of { rule : string option; label : string option }

(* the red-path case form (corpus/rood): evaluation MUST fail
   observably in every executor; each field is a substring the
   executor's own fault report must contain *)
type expected_fault = { ef_ref : string; ef_clingo : string }

type t = {
  as_of : string option;
  world : World.t;
  params : Value.t SMap.t;
  param_tls : Timeline.t SMap.t; (* granular parameters' full timelines *)
  expected : expectation list; (* empty exactly when [expected_fault] is set *)
  expected_fault : expected_fault option;
}

(* naive substring search, for matching a fault report against the
   pinned expectedFault fragment *)
let contains ~needle hay =
  let nh = String.length hay and nn = String.length needle in
  nn = 0
  ||
  let rec go i = i + nn <= nh && (String.sub hay i nn = needle || go (i + 1)) in
  go 0

(* --- json helpers (strict) --------------------------------------------- *)

let obj = function `Assoc kvs -> kvs | _ -> fail "expected an object"
let str = function `String s -> s | _ -> fail "expected a string"
let field kvs k = match List.assoc_opt k kvs with Some v -> v | None -> fail "missing field %S" k

(* --- typed values against a declared datatype -------------------------- *)

let ms_of_datetime s =
  (* YYYY-MM-DDThh:mm:ss(.mmm)? *)
  Scanf.sscanf_opt s "%4d-%2d-%2dT%2d:%2d:%2d%s"
    (fun y m d hh mm ss rest ->
      let ms =
        match rest with
        | "" -> 0
        | r -> ( try int_of_string (String.sub r 1 (String.length r - 1)) with _ -> fail "malformed dateTime %S" s)
      in
      let days = Value.days_from_civil ~y ~m ~d in
      (((((days * 24) + hh) * 60) + mm) * 60 + ss) * 1000 + ms)
  |> function Some v -> v | None -> fail "malformed dateTime %S" s

let value_of_json ~at (decl : datatype option) (j : Yojson.Safe.t) : Value.t =
  let kvs = obj j in
  let variant, v = match kvs with [ (k, v) ] -> (k, v) | _ -> fail "%s: malformed value" at in
  match (variant, decl) with
  | "number", Some (Numeriek { eenheid; _ } | Percentage { eenheid; _ }) ->
      let u = match eenheid with Some e -> Value.Unit e | None -> Value.Unitless in
      Value.Num (Value.q_of_lexeme (str v), u)
  | "number", None -> Value.Num (Value.q_of_lexeme (str v), Value.Unitless)
  | "date", Some Datum_in_dagen -> Value.date_of_iso (str v)
  | "dateTime", Some Datum_tijd -> Value.Date_time (ms_of_datetime (str v))
  | "boolean", Some Booleaan -> (
      match v with `Bool b -> Value.Bool b | _ -> fail "%s: malformed boolean" at)
  | "text", Some Tekst -> Value.Text (str v)
  | "enumeration", Some (Enumeratie { waarden; _ }) ->
      let s = str v in
      if List.exists (function Some w -> norm w = norm s | None -> false) waarden then Value.Enum s
      else fail "%s: %S is not a declared enumeration value" at s
  | _, Some _ -> fail "%s: value variant %S does not match the declared datatype" at variant
  | _, None -> fail "%s: value variant %S against an unknown datatype" at variant

(* Is this value JSON the timeline variant? *)
let is_timeline_json j =
  match j with `Assoc [ ("timeline", _) ] -> true | _ -> false

(* The full-timeline value variant: ordered, non-overlapping, day-precision
   segments aligned to the declaration's granularity grid; segment values
   are scalars of the element datatype. From inclusive, to exclusive;
   absence between segments is a gap (leeg on read, ch. 3.1). *)
let timeline_of_json ?(allow_empty = false) ~at ~(gran : granulariteit) (decl : datatype option)
    (j : Yojson.Safe.t) : Timeline.t =
  let segs =
    match j with
    | `Assoc [ ("timeline", `List l) ] -> l
    | _ -> fail "%s: malformed timeline value" at
  in
  if segs = [] && not allow_empty then
    fail "%s: a timeline value carries at least one segment" at;
  let day_of ~what sj =
    match Value.date_of_iso (str sj) with
    | Value.Date d -> d
    | _ -> fail "%s: malformed %s date" at what
  in
  let aligned d =
    let van, _ = Timeline.bucket gran d in
    van = d
  in
  let parsed =
    List.map
      (fun sj ->
        let kvs = obj sj in
        let van = day_of ~what:"from" (field kvs "from") in
        let tot = day_of ~what:"to" (field kvs "to") in
        if tot <= van then fail "%s: segment 'to' must lie after 'from'" at;
        if not (aligned van && aligned tot) then
          fail "%s: segment boundaries must sit on the declared granularity grid" at;
        { Timeline.s_van = van; s_tot = tot; s_waarde = value_of_json ~at decl (field kvs "value") })
      segs
  in
  let rec check_order = function
    | a :: (b : Timeline.segment) :: rest ->
        if a.Timeline.s_tot > b.s_van then
          fail "%s: timeline segments must be ordered and non-overlapping" at;
        check_order (b :: rest)
    | _ -> ()
  in
  check_order parsed;
  parsed

(* --- cross-checked loading --------------------------------------------- *)

let find_objecttype (doc : document) name =
  match Ir_v0.Checks.find_ci doc.objecttypen name with
  | Some ot -> ot
  | None -> fail "instance objectType %S is not declared" name

(* the fact-type role a role key on an instance of [otype] refers to:
   the key names the *other* role (by name or plural) of a fact type in
   which [otype] can fill the opposite role *)
let resolve_role_ft (doc : document) ~otype ~key =
  let hits =
    List.concat_map
      (fun (_, (ft : feittype)) ->
        let ot r = Option.value r.rol_objecttype_naam ~default:"" in
        let matches (r : rol) =
          (match r.rol_naam with Some n -> norm n = norm key | None -> false)
          || (match r.rol_meervoud with Some m -> norm m = norm key | None -> false)
        in
        match ft.rollen with
        | [ a; b ] ->
            (if norm (ot a) = norm otype && matches b then [ (ft, b) ] else [])
            @ if norm (ot b) = norm otype && matches a then [ (ft, a) ] else []
        | [ r ] when ft.wederkerig ->
            (* reciprocal: the single role is its own opposite *)
            if norm (ot r) = norm otype && matches r then [ (ft, r) ] else []
        | _ -> [])
      doc.feittypen
  in
  match hits with
  | [ p ] -> p
  | [] -> fail "role key %S does not resolve to a declared role opposite %S" key otype
  | _ -> fail "role key %S is ambiguous from %S" key otype

let resolve_role doc ~otype ~key = snd (resolve_role_ft doc ~otype ~key)

let load_instance (doc : document) (j : Yojson.Safe.t) : World.instance =
  let kvs = obj j in
  let label = str (field kvs "label") in
  (* "::" is the minted-instance namespace (Eval.run_object_creatie's
     deterministic labels); a case label there could silently suppress
     a creation — refused at load, never read wrong *)
  if contains ~needle:"::" label then
    fail "label %S: \"::\" is reserved for created instances" label;
  let otype_name = str (field kvs "objectType") in
  let ot = find_objecttype doc otype_name in
  let attrs, tls =
    List.fold_left
      (fun (attrs, tls) (name, vj) ->
        match Ir_v0.Checks.find_ci ot.attributen name with
        | Some decl ->
            let at = label ^ "." ^ name in
            if is_timeline_json vj then (
              match Ir_v0.Checks.find_ci ot.attribuut_granulariteit name with
              | Some (Some gran) ->
                  (attrs, SMap.add (norm name) (timeline_of_json ~at ~gran decl vj) tls)
              | _ -> fail "%s: a timeline value on a non-temporal attribute" at)
            else (SMap.add (norm name) (value_of_json ~at decl vj) attrs, tls)
        | None -> fail "attribute %S is not declared on %S" name otype_name)
      (SMap.empty, SMap.empty)
      (obj (field kvs "attributes"))
  in
  let chars =
    List.map
      (fun cj ->
        let c = str cj in
        if Ir_v0.Checks.mem_ci (Ir_v0.Checks.some_names ot.kenmerken) c then norm c
        else fail "characteristic %S is not declared on %S" c otype_name)
      (match field kvs "characteristics" with `List l -> l | _ -> fail "characteristics: expected a list")
  in
  let links =
    List.fold_left
      (fun (acc, seen) (key, vj) ->
        let role = resolve_role doc ~otype:otype_name ~key in
        (* one role, one key: a split delivery (the same role under its
           name AND its plural) would make the first-non-empty-key
           readers see only half the links — refuse it at load time,
           never read it silently wrong *)
        let role_id = Option.value role.rol_naam ~default:key in
        (match List.assoc_opt (norm role_id) seen with
        | Some prev ->
            fail "role keys %S and %S on %S both name the role %S; deliver one key" prev key
              label role_id
        | None -> ());
        let labels =
          match vj with
          | `List l -> List.map str l
          | _ -> fail "role %S: expected a list of labels" key
        in
        if (not role.rol_meervoudig) && List.length labels <> 1 then
          fail "to-one role %S carries %d labels" key (List.length labels);
        (SMap.add (norm key) labels acc, (norm role_id, key) :: seen))
      (SMap.empty, [])
      (obj (field kvs "roles"))
    |> fst
  in
  let ctls =
    match List.assoc_opt "characteristicTimelines" kvs with
    | None -> SMap.empty
    | Some j ->
        List.fold_left
          (fun acc (cname, pj) ->
            if not (Ir_v0.Checks.mem_ci (Ir_v0.Checks.some_names ot.kenmerken) cname) then
              fail "characteristic %S is not declared on %S" cname otype_name;
            let gran =
              match
                List.find_opt (fun (k, _) -> norm k = norm cname) ot.kenmerk_granulariteit
              with
              | Some (_, Some g) -> g
              | _ ->
                  fail "%s.%s: a characteristic timeline on a non-temporal characteristic"
                    label cname
            in
            ignore gran;
            let at = label ^ "." ^ cname in
            let day_of ~what sj =
              match Value.date_of_iso (str sj) with
              | Value.Date d -> d
              | _ -> fail "%s: malformed %s date" at what
            in
            (* true periods are DAY-precise regardless of the declared
               granularity — the pinned runtime accepts a mid-month
               boundary on a month-granular characteristic (the ch. 3.7
               pro-rata's whole point); the granularity governs the
               characteristic's bucketing, never its period grid *)
            let periods =
              match pj with
              | `List l ->
                  List.map
                    (fun sj ->
                      let skvs = obj sj in
                      let van = day_of ~what:"from" (field skvs "from") in
                      let tot = day_of ~what:"to" (field skvs "to") in
                      if tot <= van then fail "%s: period 'to' must lie after 'from'" at;
                      (van, tot))
                    l
              | _ -> fail "%s: expected a list of periods" at
            in
            let sorted = List.sort compare periods in
            List.iteri
              (fun i (v, _) ->
                if i > 0 then
                  let _, prev_t = List.nth sorted (i - 1) in
                  if v < prev_t then fail "%s: periods overlap" at)
              sorted;
            SMap.add (norm cname) sorted acc)
          SMap.empty (obj j)
  in
  { label; otype = otype_name; attrs; chars; links; tls; ctls }

(* §13.3.9.2, mirrored: one reciprocal relation entry holds in BOTH
   directions — the pinned runtime writes both to-one legs so
   navigation never reads a spuriously empty side. The loader
   materialises the reverse leg of every reciprocal link, so a
   one-sided case supply reads identically in every executor; a
   conflicting reverse leg on the to-one role (WF-013) is a load
   error, never a silent overwrite. *)
let materialise_reciprocal (doc : document) (world : World.t) =
  SMap.fold
    (fun _ (i : World.instance) w ->
      SMap.fold
        (fun key targets w ->
          let ft, role = resolve_role_ft doc ~otype:i.otype ~key in
          if not ft.wederkerig then w
          else
            List.fold_left
              (fun (w : World.t) t ->
                match SMap.find_opt t w.instances with
                | None -> w (* check_role_targets reports the unknown label *)
                | Some (ti : World.instance) ->
                    let store_key =
                      (* land the reverse leg where the target already
                         keys this role, else under the case's key *)
                      match SMap.find_opt (norm key) ti.links with
                      | Some _ -> key
                      | None -> (
                          match
                            List.find_opt
                              (fun k -> SMap.mem (norm k) ti.links)
                              (List.filter_map (fun k -> k) [ role.rol_meervoud; role.rol_naam ])
                          with
                          | Some k -> k
                          | None -> key)
                    in
                    let existing =
                      Option.value (SMap.find_opt (norm store_key) ti.links) ~default:[]
                    in
                    if List.mem i.label existing then w
                    else if (not role.rol_meervoudig) && existing <> [] then
                      fail
                        "reciprocal role %S: %S already links %s; the reverse leg of %S -> %S \
                         conflicts (WF-013 to-one)"
                        key t
                        (String.concat ", " existing)
                        i.label t
                    else
                      {
                        w with
                        instances =
                          SMap.add t
                            { ti with links = SMap.add (norm store_key) (existing @ [ i.label ]) ti.links }
                            w.instances;
                      })
              w targets)
        i.links w)
    world.instances world

let check_role_targets (doc : document) (world : World.t) =
  SMap.iter
    (fun _ (i : World.instance) ->
      SMap.iter
        (fun key labels ->
          let role = resolve_role doc ~otype:i.otype ~key in
          let expect = Option.value role.rol_objecttype_naam ~default:"" in
          List.iter
            (fun l ->
              match SMap.find_opt l world.instances with
              | None -> fail "role %S on %S links unknown label %S" key i.label l
              | Some target ->
                  if norm target.otype <> norm expect then
                    fail "role %S on %S links %S of type %S; declared filler type is %S" key
                      i.label l target.otype expect)
            labels)
        i.links)
    world.instances

let load_expectation (doc : document) (world : World.t) (j : Yojson.Safe.t) : expectation =
  let kvs = obj j in
  let get_instance () =
    let label = str (field kvs "label") in
    match SMap.find_opt label world.instances with
    | Some i -> i
    | None -> fail "expectation references unknown label %S" label
  in
  match str (field kvs "kind") with
  | "attributeValue" ->
      let i = get_instance () in
      let attr = str (field kvs "attribute") in
      let ot = find_objecttype doc i.otype in
      let decl =
        match Ir_v0.Checks.find_ci ot.attributen attr with
        | Some d -> d
        | None -> fail "expected attribute %S is not declared on %S" attr i.otype
      in
      Attribute_value
        {
          label = i.label;
          attr;
          value = value_of_json ~at:("expected " ^ i.label ^ "." ^ attr) decl (field kvs "value");
        }
  | "attributeEmpty" ->
      let i = get_instance () in
      let attr = str (field kvs "attribute") in
      let ot = find_objecttype doc i.otype in
      if not (Ir_v0.Checks.has_attr ot attr) then
        fail "expected attribute %S is not declared on %S" attr i.otype;
      Attribute_empty { label = i.label; attr }
  | "attributeTimeline" ->
      let i = get_instance () in
      let attr = str (field kvs "attribute") in
      let ot = find_objecttype doc i.otype in
      let decl =
        match Ir_v0.Checks.find_ci ot.attributen attr with
        | Some d -> d
        | None -> fail "expected attribute %S is not declared on %S" attr i.otype
      in
      let gran =
        match Ir_v0.Checks.find_ci ot.attribuut_granulariteit attr with
        | Some (Some g) -> g
        | _ -> fail "expected timeline on %S, which declares no granularity" attr
      in
      Attribute_timeline
        {
          label = i.label;
          attr;
          tl =
            timeline_of_json ~allow_empty:true
              ~at:("expected " ^ i.label ^ "." ^ attr)
              ~gran decl
              (`Assoc [ ("timeline", field kvs "timeline") ]);
        }
  | "characteristicPresent" | "characteristicAbsent" ->
      let i = get_instance () in
      let c = str (field kvs "characteristic") in
      let ot = find_objecttype doc i.otype in
      if not (Ir_v0.Checks.mem_ci (Ir_v0.Checks.some_names ot.kenmerken) c) then
        fail "expected characteristic %S is not declared on %S" c i.otype;
      if str (field kvs "kind") = "characteristicPresent" then
        Char_present { label = i.label; char = c }
      else Char_absent { label = i.label; char = c }
  | "consistent" -> Consistent
  | "inconsistent" ->
      let opt_str k = Option.map str (List.assoc_opt k kvs) in
      (* the rule pin refuses at load when NO form in the document
         carries the pinned name — symmetric with the unknown-label
         refusal below (maintainer decision 2026-08-21): a typo
         surfaces as a named load error, never as a vague red
         mismatch. Valid names: every form name (version-stripped or
         full), ladder names, and the target form key an unordered
         ladder group deposits under. *)
      let known_rule r =
        let module C = Ir_v0.Checks in
        let hit n = n <> None && (norm (Option.get n) = norm r || norm (C.strip_version (Option.get n)) = norm r) in
        List.exists (fun (x : resolved_regel) -> hit x.rr_naam) doc.resolved_regels
        || List.exists (fun (x : resolved_object_creatie) -> hit x.oc_naam) doc.object_creaties
        || List.exists (fun (x : resolved_verdeling) -> hit x.vd_naam) doc.verdelingen
        || List.exists (fun (x : resolved_consistentie) -> hit x.cons_naam) doc.consistenties
        || List.exists (fun (x : resolved_uniciteit) -> hit x.uni_naam) doc.uniciteiten
        || List.exists (fun (x : resolved_uniciteit_unie) -> hit x.uu_naam) doc.uniciteit_unies
        || List.exists (fun (x : resolved_feit_creatie) -> hit x.fc_naam) doc.feit_creaties
        || List.exists
             (fun (l : default_ladder) ->
               hit l.dl_naam
               ||
               let target_form =
                 l.dl_onderwerp ^ "."
                 ^ match (l.dl_kenmerk, l.dl_attribuut) with
                   | Some k, _ -> k
                   | _, Some a -> a
                   | _ -> "?"
               in
               norm target_form = norm r)
             doc.default_ladders
      in
      (match (opt_str "rule", opt_str "label") with
      | None, Some _ -> fail "an inconsistent label pin requires a rule pin"
      | (Some r, _) when not (known_rule r) ->
          fail
            "inconsistent expectation references unknown rule %S: no form in the document \
             carries that name"
            r
      | rule, (Some l as label) ->
          if SMap.find_opt l world.instances = None then
            fail "inconsistent expectation references unknown label %S" l
          else Inconsistent { rule; label }
      | rule, label -> Inconsistent { rule; label })
  | k -> fail "unknown expectation kind %S" k

let load_file (doc : document) path : t =
  let j = Yojson.Safe.from_file path in
  let kvs = obj j in
  (match str (field kvs "format") with
  | "uchar-case-v0" -> ()
  | f -> fail "unsupported case format %S" f);
  let as_of =
    match field kvs "asOfDate" with
    | `Null -> None
    | `String s -> Some s
    | _ -> fail "malformed asOfDate"
  in
  let input = obj (field kvs "input") in
  let world =
    match field input "instances" with
    | `List l -> List.fold_left (fun w ij -> World.add_instance w (load_instance doc ij)) World.empty l
    | _ -> fail "instances: expected a list"
  in
  let world = materialise_reciprocal doc world in
  check_role_targets doc world;
  let params, param_tls =
    List.fold_left
      (fun (params, ptls) (name, vj) ->
        match Ir_v0.Checks.find_ci doc.parameters name with
        | Some p ->
            let at = "parameter " ^ name in
            if is_timeline_json vj then (
              match p.par_granulariteit with
              | Some gran ->
                  (params, SMap.add (norm name) (timeline_of_json ~at ~gran p.par_datatype vj) ptls)
              | None -> fail "%s: a timeline value on a non-granular parameter" at)
            else if p.par_granulariteit <> None then
              (* the symmetric refusal: a granular parameter takes a
                 timeline, never a scalar — the scalar slot is unread
                 (ch. 8), so accepting one only breeds divergence *)
              fail "%s: a scalar value on a granular parameter (it takes a timeline)" at
            else (SMap.add (norm name) (value_of_json ~at p.par_datatype vj) params, ptls)
        | None -> fail "parameter %S is not declared" name)
      (SMap.empty, SMap.empty)
      (obj (field input "parameters"))
  in
  let expected_fault =
    match List.assoc_opt "expectedFault" kvs with
    | None -> None
    | Some j ->
        let fk = obj j in
        let frag what =
          let s = str (field fk what) in
          if s = "" then fail "expectedFault.%s: an empty fragment would match any fault" what;
          s
        in
        Some { ef_ref = frag "ref"; ef_clingo = frag "clingo" }
  in
  let expected =
    match (List.assoc_opt "expected" kvs, expected_fault) with
    | Some (`List l), None -> List.map (load_expectation doc world) l
    | Some _, None -> fail "expected: expected a list"
    | None, Some _ -> []
    | Some _, Some _ -> fail "a case carries exactly one of expected / expectedFault"
    | None, None -> fail "missing field %S" "expected"
  in
  if expected = [] && expected_fault = None then fail "empty expectation list";
  { as_of; world; params; param_tls; expected; expected_fault }

(* --- expectation checking ---------------------------------------------- *)

(* returns the list of mismatches, empty on success *)
let check (case : t) (final : World.t) : string list =
  List.filter_map
    (fun e ->
      match e with
      | Attribute_value { label; attr; value } ->
          let actual = World.attr (World.get final label) attr in
          if Arith.equal actual value then None
          else
            Some
              (Printf.sprintf "%s.%s = %s, expected %s" label attr (Value.to_string actual)
                 (Value.to_string value))
      | Attribute_empty { label; attr } ->
          let actual = World.attr (World.get final label) attr in
          if actual = Value.Empty then None
          else Some (Printf.sprintf "%s.%s = %s, expected leeg" label attr (Value.to_string actual))
      | Attribute_timeline { label; attr; tl } -> (
          let show (s : Timeline.segment) =
            Printf.sprintf "[%s,%s)=%s" (Value.iso_of_date s.s_van) (Value.iso_of_date s.s_tot)
              (Value.to_string s.s_waarde)
          in
          let render l = if l = [] then "(empty)" else String.concat " " (List.map show l) in
          match World.timeline (World.get final label) attr with
          | None when tl = [] ->
              (* a never-written temporal target and an explicitly empty
                 timeline read leeg on every day (ch. 3.1) — the empty
                 expectation matches both, as it does on the clingo side *)
              None
          | None ->
              Some (Printf.sprintf "%s.%s carries no timeline, expected %s" label attr (render tl))
          | Some actual ->
              let same =
                List.length actual = List.length tl
                && List.for_all2
                     (fun (a : Timeline.segment) (b : Timeline.segment) ->
                       a.s_van = b.s_van && a.s_tot = b.s_tot && Arith.equal a.s_waarde b.s_waarde)
                     actual tl
              in
              if same then None
              else
                Some
                  (Printf.sprintf "%s.%s = %s, expected %s" label attr (render actual) (render tl)))
      | Char_present { label; char } ->
          if World.has_char (World.get final label) char then None
          else Some (Printf.sprintf "%s lacks characteristic %S" label char)
      | Char_absent { label; char } ->
          if not (World.has_char (World.get final label) char) then None
          else Some (Printf.sprintf "%s carries characteristic %S, expected absent" label char)
      | Consistent ->
          if final.channel = [] then None
          else
            Some
              (Printf.sprintf "expected consistent, but the channel carries: %s"
                 (String.concat "; "
                    (List.map
                       (fun (f : World.error_fact) -> f.rule ^ " × " ^ f.subject)
                       final.channel)))
      | Inconsistent { rule; label } -> (
          let matches (f : World.error_fact) =
            (match rule with None -> true | Some r -> norm f.rule = norm r)
            && match label with None -> true | Some l -> String.equal f.subject l
          in
          match List.find_opt matches final.channel with
          | Some _ -> None
          | None ->
              let want =
                match (rule, label) with
                | None, _ -> "any channel fact"
                | Some r, None -> Printf.sprintf "a channel fact from rule %S" r
                | Some r, Some l -> Printf.sprintf "a channel fact from rule %S on %S" r l
              in
              Some
                (Printf.sprintf "expected inconsistent (%s), but the channel carries: %s" want
                   (if final.channel = [] then "(empty)"
                    else
                      String.concat "; "
                        (List.map
                           (fun (f : World.error_fact) -> f.rule ^ " × " ^ f.subject)
                           final.channel)))))
    case.expected
