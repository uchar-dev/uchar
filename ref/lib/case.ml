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
  | Char_present of { label : string; char : string }
  | Char_absent of { label : string; char : string }
  | Consistent
  | Inconsistent

type t = {
  as_of : string option;
  world : World.t;
  params : Value.t SMap.t;
  param_tls : Timeline.t SMap.t; (* granular parameters' full timelines *)
  expected : expectation list;
}

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
let timeline_of_json ~at ~(gran : granulariteit) (decl : datatype option) (j : Yojson.Safe.t) :
    Timeline.t =
  let segs =
    match j with
    | `Assoc [ ("timeline", `List l) ] -> l
    | _ -> fail "%s: malformed timeline value" at
  in
  if segs = [] then fail "%s: a timeline value carries at least one segment" at;
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
let resolve_role (doc : document) ~otype ~key =
  let hits =
    List.concat_map
      (fun (_, (ft : feittype)) ->
        match ft.rollen with
        | [ a; b ] ->
            let ot r = Option.value r.rol_objecttype_naam ~default:"" in
            let matches (r : rol) =
              (match r.rol_naam with Some n -> norm n = norm key | None -> false)
              || (match r.rol_meervoud with Some m -> norm m = norm key | None -> false)
            in
            (if norm (ot a) = norm otype && matches b then [ b ] else [])
            @ if norm (ot b) = norm otype && matches a then [ a ] else []
        | _ -> [])
      doc.feittypen
  in
  match hits with
  | [ r ] -> r
  | [] -> fail "role key %S does not resolve to a declared role opposite %S" key otype
  | _ -> fail "role key %S is ambiguous from %S" key otype

let load_instance (doc : document) (j : Yojson.Safe.t) : World.instance =
  let kvs = obj j in
  let label = str (field kvs "label") in
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
      (fun acc (key, vj) ->
        let role = resolve_role doc ~otype:otype_name ~key in
        let labels =
          match vj with
          | `List l -> List.map str l
          | _ -> fail "role %S: expected a list of labels" key
        in
        if (not role.rol_meervoudig) && List.length labels <> 1 then
          fail "to-one role %S carries %d labels" key (List.length labels);
        SMap.add (norm key) labels acc)
      SMap.empty
      (obj (field kvs "roles"))
  in
  { label; otype = otype_name; attrs; chars; links; tls }

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
  | "inconsistent" -> Inconsistent
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
            else (SMap.add (norm name) (value_of_json ~at p.par_datatype vj) params, ptls)
        | None -> fail "parameter %S is not declared" name)
      (SMap.empty, SMap.empty)
      (obj (field input "parameters"))
  in
  let expected =
    match field kvs "expected" with
    | `List l -> List.map (load_expectation doc world) l
    | _ -> fail "expected: expected a list"
  in
  if expected = [] then fail "empty expectation list";
  { as_of; world; params; param_tls; expected }

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
      | Inconsistent ->
          if final.channel <> [] then None
          else Some "expected inconsistent, but the error channel is empty")
    case.expected
