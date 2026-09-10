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

(* The `dateTime` value of uchar-case-v0: the CANONICAL ISO lexeme and
   nothing else — fixed field widths, 'T', the seconds field written, a
   fraction that is optional but exactly three digits. Gate 4 enforces that
   pattern on every case FILE; this reader holds the same fence ITSELF, so a
   case that reaches the loader another way (a generator, a hand run, a
   second producer) is refused rather than guessed at.

   It is not a theoretical fence. The reader here used to scan with
   "%4d-%2d-%2dT%2d:%2d:%2d%s" and then [int_of_string] the fraction at ANY
   width: ".1" read as ONE millisecond where the Drools case reader reads a
   hundred, ".100000" as a hundred THOUSAND (a minute and forty seconds
   later), ".1234" as 1 234 (1.234 seconds later), and "2026-3-15T09:00:00"
   as a valid instant the Drools reader refuses. Three readers, three
   answers, and the schema was the only reason it never showed. *)
let ms_of_datetime s =
  match Value.read_iso_datetime s with
  | Ok ms -> ms
  | Error why -> fail "malformed dateTime %S (%s)" s why

let rec value_of_json ~at (decl : datatype option) (j : Yojson.Safe.t) : Value.t =
  let kvs = obj j in
  match List.assoc_opt "constructor" kvs with
  | Some ctor_j -> constructor_of_json ~at decl kvs ctor_j
  | None ->
  let variant, v = match kvs with [ (k, v) ] -> (k, v) | _ -> fail "%s: malformed value" at in
  match (variant, decl) with
  | "number", Some (Numeriek { eenheid; _ } | Percentage { eenheid; _ }) ->
      let u = match eenheid with Some e -> Value.Unit e | None -> Value.Unitless in
      Value.Num (Value.q_of_lexeme (str v), u)
  (* WF-059 backstop: a value against a datatype-less declaration would
     split the executors (ref would read a bare unitless number where
     the integer-scaling translation cannot scale) — the shared loader
     refuses it in both, last-line behind the verifier's rejection *)
  | "number", None ->
      fail "%s: a value against a declaration without a datatype (WF-059 for attributes, WF-018 for parameters)" at
  | "date", Some Datum_in_dagen -> Value.date_of_iso (str v)
  | "dateTime", Some Datum_tijd -> Value.Date_time (ms_of_datetime (str v))
  | "boolean", Some Booleaan -> (
      match v with `Bool b -> Value.Bool b | _ -> fail "%s: malformed boolean" at)
  | "text", Some Tekst -> Value.Text (str v)
  | "enumeration", Some (Enumeratie { waarden; _ }) ->
      let s = str v in
      if List.exists (function Some w -> norm w = norm s | None -> false) waarden then Value.Enum s
      else fail "%s: %S is not a declared enumeration value" at s
  (* the list value form of §2.5 (RUNTIME-SEMANTICS ch. 10.5): each element
     loads recursively against the declared element datatype, in order; a
     null/leeg element is refused — list positions are never leeg (ch. 10.1) *)
  | "list", Some (List_type { element }) -> (
      match v with
      | `List elems ->
          Value.Lijst
            (List.mapi
               (fun i ej ->
                 match ej with
                 | `Null ->
                     fail "%s[%d]: a list element is leeg; list positions are never leeg (ch. 10.1)"
                       at i
                 | _ -> value_of_json ~at:(Printf.sprintf "%s[%d]" at i) (Some element) ej)
               elems)
      | _ -> fail "%s: a list value's \"list\" field is not an array" at)
  (* WF-059/WF-018 backstop, widened to the list value: a value against a
     datatype-less declaration splits the executors, so the shared loader
     refuses it in both *)
  | "list", None ->
      fail "%s: a value against a declaration without a datatype (WF-059 for attributes, WF-018 for parameters)" at
  (* the record value form (RUNTIME-SEMANTICS ch. 11.3): exactly the declared
     fields, in declaration order, each loaded recursively against its field
     datatype. Fail fast on a missing, extra, wrong-typed or leeg field — the
     no-hole rule at the boundary (the WF-059/WF-018 backstop, widened). *)
  | "struct", Some (Struct_type { velden; _ }) -> (
      match v with
      | `Assoc field_kvs ->
          if List.length field_kvs <> List.length velden then
            fail "%s: record supplies %d fields, the struct declares %d" at (List.length field_kvs)
              (List.length velden);
          Value.Record
            (List.map
               (fun (fname, fdt) ->
                 match List.find_opt (fun (k, _) -> norm k = norm fname) field_kvs with
                 | None -> fail "%s: record is missing declared field %S" at fname
                 | Some (_, `Null) ->
                     fail "%s.%s: a record field is leeg; record fields are never leeg (ch. 11.1)"
                       at fname
                 | Some (_, fj) -> (fname, value_of_json ~at:(at ^ "." ^ fname) (Some fdt) fj))
               velden)
      | _ -> fail "%s: a record value's \"struct\" field is not an object" at)
  (* WF-059/WF-018 backstop, widened to the record value *)
  | "struct", None ->
      fail "%s: a value against a declaration without a datatype (WF-059 for attributes, WF-018 for parameters)" at
  | _, Some _ -> fail "%s: value variant %S does not match the declared datatype" at variant
  | _, None -> fail "%s: value variant %S against an unknown datatype" at variant

(* The constructor value form of §2.5: {constructor, payload?}. The
   cross-checks JSON Schema cannot express live here (the WF-059/WF-018
   backstop, widened to the sum type): the tag names a declared constructor
   (ci), the payload is present iff the constructor is unary and loads
   recursively against the declared payload datatype. *)
and constructor_of_json ~at (decl : datatype option) kvs ctor_j : Value.t =
  let tag = str ctor_j in
  match decl with
  | Some (Sum_type { constructoren; _ }) -> (
      match List.find_opt (fun (name, _) -> norm name = norm tag) constructoren with
      | None -> fail "%s: %S is not a declared constructor of the sum type" at tag
      | Some (_, payload_decl) -> (
          match (payload_decl, List.assoc_opt "payload" kvs) with
          | Some pdt, Some pj -> Value.Ctor (tag, Some (value_of_json ~at (Some pdt) pj))
          | None, None -> Value.Ctor (tag, None)
          | Some _, None -> fail "%s: unary constructor %S carries no payload" at tag
          | None, Some _ -> fail "%s: nullary constructor %S carries a payload" at tag))
  | Some _ -> fail "%s: a constructor value against a non-sum-type declaration" at
  | None ->
      fail
        "%s: a constructor value against a declaration without a datatype (WF-059 for \
         attributes, WF-018 for parameters)"
        at

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

(* the fact-type roles a role key on an instance of [otype] refers to:
   the key names the *other* role (by name or plural) of a fact type in
   which [otype] can fill the opposite role. Returns (fact type, FAR
   role — the one the key names and the targets fill, SISTER role — the
   one the keying instance itself fills). On the reciprocal shape the
   single role is both. *)
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
            (if norm (ot a) = norm otype && matches b then [ (ft, b, a) ] else [])
            @ if norm (ot b) = norm otype && matches a then [ (ft, a, b) ] else []
        | [ r ] when ft.wederkerig ->
            (* reciprocal: the single role is its own opposite *)
            if norm (ot r) = norm otype && matches r then [ (ft, r, r) ] else []
        | _ -> [])
      doc.feittypen
  in
  match hits with
  | [ p ] -> p
  | [] -> fail "role key %S does not resolve to a declared role opposite %S" key otype
  | _ -> fail "role key %S is ambiguous from %S" key otype

let resolve_role doc ~otype ~key =
  let _, far, _ = resolve_role_ft doc ~otype ~key in
  far

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
              | Some None ->
                  (* The KEY is present, so the document declares this attribute
                     time-dependent — every other reader (the verifier's
                     attr_temporal, this interpreter's evaluator, the ASP
                     translator) already treats it as such. Only the grid is
                     missing. Until 2026-09-10 this fell into the arm below and
                     was refused as "non-temporal", contradicting the document
                     that produced it. The schema enum and WF-059's third arm
                     now shut the shape on the wire, so only a hand-built
                     document reaches here — and it must be told the real
                     reason. *)
                  fail
                    "%s: the attribute is declared time-dependent but its granularity is null: \
                     the timeline has no grid to sit on (WF-059)"
                    at
              | None -> fail "%s: a timeline value on a non-temporal attribute" at)
            else (SMap.add (norm name) (value_of_json ~at decl vj) attrs, tls)
        | None -> fail "attribute %S is not declared on %S" name otype_name)
      (SMap.empty, SMap.empty)
      (obj (field kvs "attributes"))
  in
  let chars =
    List.map
      (fun cj ->
        let c = str cj in
        if not (Ir_v0.Checks.mem_ci (Ir_v0.Checks.some_names ot.kenmerken) c) then
          fail "characteristic %S is not declared on %S" c otype_name;
        (* The two deliveries are NOT interchangeable, and until 2026-09-09
           this arm asked only whether the name was declared. A characteristic
           the document declares TIME-DEPENDENT has a timeline, and every
           temporal reader looks for one: the gedurende clip, the pro-rata fold
           and the day count all read the case's characteristic timelines and
           nothing else. Asserting such a characteristic in the flat
           `characteristics` list therefore delivered a fact no temporal reader
           ever consults, and the clip intersected against an EMPTY period set
           without a word — measured on the golden that pins 30: ref answered
           0, clingo answered 0, both silently.

           So it is refused here, at load, where the case is still nameable.
           The sister arm below already refuses the mirror-image mistake — a
           timeline on a non-temporal characteristic — and the two now cover
           the seam from both sides. The predicate is KEY EXISTENCE in
           kenmerkGranulariteit, the same one the three clip readers use;
           WF-059's second arm makes key and value coincide for any document
           that passed the verifier. *)
        if List.exists (fun (k, _) -> norm k = norm c) ot.kenmerk_granulariteit then
          fail
            "%s.%s: characteristic %S is declared time-dependent on %S, so it is delivered as a \
             \"characteristicTimelines\" entry — the static \"characteristics\" list asserts a \
             scalar truth no temporal reader consults"
            label c c otype_name;
        norm c)
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

(* ch. 5.7a: a supplied role link ESTABLISHES the relation instance, and
   BOTH roles read it — a relation is symmetric, so the reader arriving
   from the side the case did not name reaches the same relation the
   naming side reaches, never a spuriously empty far side. The loader
   therefore materialises the reverse leg of every supplied link, keyed
   by the SISTER role (the role the supplying instance itself fills);
   the reciprocal shape (§13.3.9.2, one role that is its own opposite)
   is the special case in which sister and named role coincide, and the
   pinned runtime's both-legs write is that case of the same law.

   A separate pass over the finished input, reading the ORIGINAL links
   only: the outcome cannot depend on the order instances appear in, and
   a materialised leg never breeds another. Idempotent — a case
   supplying both legs describes one relation, not two — and a
   conflicting reverse leg on a to-one sister role (WF-013) is a load
   error, never a silent overwrite. *)
let materialise_relations (doc : document) (world : World.t) =
  SMap.fold
    (fun _ (i : World.instance) w ->
      SMap.fold
        (fun key targets w ->
          let _, _, sister = resolve_role_ft doc ~otype:i.otype ~key in
          (* the keys the sister role's links may sit under, in the same
             ORDER Eval.role_keys and World.add_link use (plural first,
             then name), so a materialised leg lands where every reader
             looks. The PREDICATE is deliberately not identical: here
             "the target already keys this role" is key PRESENCE
             (SMap.mem), in add_link it is a NON-EMPTY value. They can
             only diverge on a key that is present with an empty label
             list, which no admissible case carries — load_instance
             refuses it on a to-one role (exactly one label), and the
             case schema's minItems of 1 refuses it on the rest (gate 4).
             Were one to reach here anyway, both would still pick the
             plural, the first key in the list; the readings differ only
             if the SINGULAR key were the empty-but-present one. Stated
             rather than assumed, and not worth a third predicate. *)
          let sister_keys =
            List.filter_map (fun k -> k) [ sister.rol_meervoud; sister.rol_naam ]
          in
          List.fold_left
            (fun (w : World.t) t ->
              match SMap.find_opt t w.instances with
              | None -> w (* check_role_targets reports the unknown label *)
              | Some (ti : World.instance) ->
                  let store_key =
                    (* land the reverse leg where the target already keys
                       the sister role, else under its first key *)
                    match List.find_opt (fun k -> SMap.mem (norm k) ti.links) sister_keys with
                    | Some k -> k
                    | None -> ( match sister_keys with k :: _ -> k | [] -> key)
                  in
                  let existing =
                    Option.value (SMap.find_opt (norm store_key) ti.links) ~default:[]
                  in
                  if List.mem i.label existing then w
                  else if (not sister.rol_meervoudig) && existing <> [] then
                    fail
                      "role %S: %S already links %s under the to-one role %S; the reverse leg of \
                       %S -> %S conflicts (WF-013 to-one)"
                      key t
                      (String.concat ", " existing)
                      store_key i.label t
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
         full), prioritized-defaults names, and the target form key an
         unordered default group deposits under. *)
      let channel_names () =
        (* every name a channel fact can carry: form names (the check
           matches version-stripped, per ch. 5.6's version-stripped
           key), the bare target-form key a root-level unordered default
           group deposits under, and — since ch. 7.9 — the target form
           plus a default-index path for every nested exception group
           (the path-keyed conflict a differing exception group propagates
           to the root). Defaults MEMBER names never reach the channel and
           are deliberately absent. *)
        List.filter_map (fun (x : resolved_regel) -> x.rr_naam) doc.resolved_regels
        @ List.filter_map (fun (x : resolved_object_creatie) -> x.oc_naam) doc.object_creaties
        @ List.filter_map (fun (x : resolved_verdeling) -> x.vd_naam) doc.verdelingen
        @ List.filter_map (fun (x : resolved_consistentie) -> x.cons_naam) doc.consistenties
        @ List.filter_map (fun (x : resolved_uniciteit) -> x.uni_naam) doc.uniciteiten
        @ List.filter_map (fun (x : resolved_uniciteit_unie) -> x.uu_naam) doc.uniciteit_unies
        @ List.filter_map (fun (x : resolved_feit_creatie) -> x.fc_naam) doc.feit_creaties
        @ List.concat_map
            (fun (l : prioritized_defaults) ->
              let target_form =
                l.pd_onderwerp ^ "."
                ^ match (l.pd_kenmerk, l.pd_attribuut) with
                  | Some k, _ -> k
                  | _, Some a -> a
                  | _ -> "?"
              in
              (* the nested path keys, mirroring [Eval.path_key]: every default
                 carrying an exception group can deposit a conflict under
                 the target form plus its default-index path (ch. 7.9) *)
              let rec nested prefix (defaults : default list) =
                List.concat
                  (List.mapi
                     (fun i (r : default) ->
                       match r.default_excepties with
                       | None -> []
                       | Some egs ->
                           let p = prefix @ [ i ] in
                           (target_form ^ "/" ^ String.concat "/" (List.map string_of_int p))
                           :: List.concat_map
                                (fun (eg : exception_group) -> nested p eg.eg_defaults)
                                egs.egs_leden)
                     defaults)
              in
              target_form :: nested [] l.defaults)
            doc.prioritized_defaults
      in
      let known_rule r =
        let module C = Ir_v0.Checks in
        List.exists
          (fun n -> norm (C.strip_version n) = norm (C.strip_version r))
          (channel_names ())
      in
      (match (opt_str "rule", opt_str "label") with
      | None, Some _ -> fail "an inconsistent label pin requires a rule pin"
      | (Some r, _) when not (known_rule r) ->
          let cands = List.sort_uniq compare (channel_names ()) in
          let shown = List.filteri (fun i _ -> i < 8) cands in
          fail
            "inconsistent expectation references unknown rule %S: no form in the document \
             carries that name (present, non-exhaustive: %s%s)"
            r
            (String.concat ", " (List.map (Printf.sprintf "%S") shown))
            (if List.length cands > 8 then ", …" else "")
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
  let world = materialise_relations doc world in
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
            (match rule with
            | None -> true
            | Some r ->
                (* version-stripped on both sides: the channel is keyed
                   by rule name version-stripped (ch. 5.6), so a pin
                   "X" matches a deposit from version "X #2" *)
                norm (Ir_v0.Checks.strip_version f.rule)
                = norm (Ir_v0.Checks.strip_version r))
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
