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

(* [code] as in [Value.Fault]: the EF code of a refusal of the case input
   (RUNTIME-SEMANTICS ch. 5.7b) where the raising site names one, [params] its
   parameters as in [Value.Fault] *)
exception Error of { code : string option; params : (string * string) list; text : string }

let fail fmt = Printf.ksprintf (fun s -> raise (Error { code = None; params = []; text = s })) fmt

(* a refusal of the kind [code] *)
let failc ?(params = []) code fmt =
  Printf.ksprintf (fun s -> raise (Error { code = Some code; params; text = s })) fmt

type expectation =
  | Attribute_value of { label : string; attr : string; value : Value.t }
  | Attribute_empty of { label : string; attr : string }
  | Attribute_timeline of { label : string; attr : string; tl : Timeline.t }
  | Cell_value of { label : string; attr : string; labels : string list; value : Value.t }
  | Cell_empty of { label : string; attr : string; labels : string list }
      (* a cell of a dimensioned attribute (RUNTIME-SEMANTICS ch. 5.8c), its
         labels in the declared order of the attribute's dimensions *)
  | Char_present of { label : string; char : string }
  | Char_absent of { label : string; char : string }
  | Consistent
  | Inconsistent of { rule : string option; label : string option; code : string option }
      (* [code]: the EF code (RUNTIME-SEMANTICS ch. 5.7b) a matching channel
         fact must carry among the codes deposited for it *)

(* the red-path case form (corpus/red): LOADING OR evaluation MUST fail
   observably in every executor, with the fault kind's EF code (RUNTIME-SEMANTICS
   ch. 5.7b) and the parameters the case pins; gate 8 compares both with what
   each executor reports. No fragment of a message is pinned. *)
type expected_fault = { ef_code : string; ef_params : (string * string) list }

type t = {
  as_of : string option;
  world : World.t;
  params : Value.t SMap.t;
  param_tls : Timeline.t SMap.t; (* granular parameters' full timelines *)
  expected : expectation list; (* empty exactly when [expected_fault] is set *)
  expected_fault : expected_fault option;
}

(* naive substring search *)
let contains ~needle hay =
  let nh = String.length hay and nn = String.length needle in
  nn = 0
  ||
  let rec go i = i + nn <= nh && (String.sub hay i nn = needle || go (i + 1)) in
  go 0

(* the parameters of a code as one JSON object of strings, in the order given:
   the machine form both runners print on their CODE lines (a JSON string may
   spell any character as an escape, so nobody scrapes it with a shell) *)
let params_json params =
  Yojson.Safe.to_string (`Assoc (List.map (fun (k, v) -> (k, `String v)) params))

(* --- json helpers (strict, positioned) ---------------------------------- *)

(* Every shape refusal names the position of the JSON value it refuses and
   what it found there. A position is the dotted path the lexeme refusals
   below use: an instance by its label once the label is read, a parameter
   as "parameter <name>", an array element as "[i]", and the plain JSON path
   where no such name exists yet ("input.instances[0]", "expected[2]",
   "expectedFault"); the case root is "<root>", as the runners call the root
   of an IR document, so the author of a hand-built case finds the defect
   without searching the whole file. *)
let root = ""
let where at = if at = root then "<root>" else at
let child at k = if at = root then k else at ^ "." ^ k
(* what was found, as the JSON it is, cut after 80 bytes on a character
   boundary with "…": in full, a whole wrong subtree landed on one line (a
   root given as a list ran to 2 154 characters, an input given as a list of
   twenty copies to 26 584; measured by the lens 2026-09-23) *)
let got j =
  let s = Yojson.Safe.to_string j in
  let max = 80 in
  if String.length s <= max then s
  else
    (* back off to the first byte of the character the cut would split *)
    let rec boundary i = if i > 0 && Char.code s.[i] land 0xC0 = 0x80 then boundary (i - 1) else i in
    String.sub s 0 (boundary max) ^ "\xe2\x80\xa6"
let obj ~at = function `Assoc kvs -> kvs | j -> fail "%s: expected an object, got %s" (where at) (got j)
let str ~at = function `String s -> s | j -> fail "%s: expected a string, got %s" (where at) (got j)
let list_ ~at = function `List l -> l | j -> fail "%s: expected a list, got %s" (where at) (got j)

let field ~at kvs k =
  match List.assoc_opt k kvs with Some v -> v | None -> fail "%s: missing field %S" (where at) k

(* a field and its JSON kind in one positioned read: the common case *)
let str_field ~at kvs k = str ~at:(child at k) (field ~at kvs k)
let obj_field ~at kvs k = obj ~at:(child at k) (field ~at kvs k)
let list_field ~at kvs k = list_ ~at:(child at k) (field ~at kvs k)

(* --- typed values against a declared datatype -------------------------- *)

(* The `dateTime` value of uchar-case-v0: the CANONICAL ISO lexeme and
   nothing else — fixed field widths, 'T', the seconds field written, a
   fraction that is optional but exactly three digits. Gate 4 enforces that
   pattern on every case FILE; this reader holds the same fence ITSELF, so a
   case that reaches the loader another way (a generator, a hand run, a
   second producer) is refused rather than guessed at.

   It is not a theoretical fence. A reader that scans with
   "%4d-%2d-%2dT%2d:%2d:%2d%s" and then [int_of_string]s the fraction at ANY
   width reads ".1" as ONE millisecond where the Drools case reader reads a
   hundred, ".100000" as a hundred THOUSAND (a minute and forty seconds
   later), ".1234" as 1 234 (1.234 seconds later), and "2026-3-15T09:00:00"
   as a valid instant the Drools reader refuses: three readers, three
   answers, which only the schema keeps out of a case file. *)
let ms_of_datetime s =
  match Value.read_iso_datetime s with
  | Ok ms -> ms
  | Error why -> fail "malformed dateTime %S (%s)" s why

(* --- lexemes, read at load and refused by position ---------------------- *)

(* Every date, dateTime and number lexeme a case carries is read here, at
   load, and one its reader refuses is a CASE error naming the position, the
   lexeme and the reason: exit 2 in both runners, before either executor
   runs. One template per kind, `<position> "<lexeme>" is not the
   uchar-case-v0 <kind> lexeme <form>`, with the reader's own refusal in
   brackets where the reader gives one.

   The date reader's Value.Fault is the EVALUATION exception. Let through
   the loader, a day-first input date would reach the ref runner as
   "EVALUATION FAULT: malformed date lexeme" and the ASP runner as
   "TRANSLATION FAULT", exit 1, with neither the field nor the load phase in
   it (measured on jutterswet-bergingsklok), and a red case could not pin
   such a refusal at all, because both runners' load arm matches case errors
   only. Every date position, asOfDate included (card
   ref-geldigheid-datumlezing), speaks through the same template, so the
   positions cannot drift apart. *)
let date_at ~at s =
  try Value.date_of_iso s
  with Value.Fault { text = why; _ } ->
    failc ~params:[ ("field", at); ("lexeme", s) ] "EF-003"
      "%s %S is not the uchar-case-v0 date lexeme yyyy-mm-dd (%s)" at s why

(* a period or segment boundary, as its day number *)
let day_at ~at s = match date_at ~at s with Value.Date d -> d | _ -> assert false

let datetime_at ~at s =
  try Value.Date_time (ms_of_datetime s)
  with Error { text = why; _ } ->
    fail "%s %S is not the uchar-case-v0 dateTime lexeme yyyy-mm-ddThh:mm:ss[.fff] (%s)" at s why

(* The `number` lexeme of uchar-case-v0 is the case schema's pattern,
   -?[0-9]+(\.[0-9]+)? or -?[0-9]+/[1-9][0-9]*, and this fence matches it
   whole: no character before or after it gets through, a final newline
   included, which a validator reading `$` the Python way admits. The
   literal's number variant carries this same pattern and q_of_lexeme reads
   exactly it, so the two wires have one grammar. The fence stands here too,
   as the date and dateTime fences are held by their readers, because it names
   the case position and the case format in its refusal. Every lexeme it
   admits, q_of_lexeme reads without a fault: the zero denominator is outside
   the fence. *)
let number_at ~at s =
  let digits d = d <> "" && String.for_all (fun c -> c >= '0' && c <= '9') d in
  let body = if String.length s > 0 && s.[0] = '-' then String.sub s 1 (String.length s - 1) else s in
  let whole =
    match String.index_opt body '/' with
    | Some i ->
        let num = String.sub body 0 i and den = String.sub body (i + 1) (String.length body - i - 1) in
        digits num && digits den && den.[0] <> '0'
    | None -> (
        match String.split_on_char '.' body with
        | [ n ] -> digits n
        | [ n; f ] -> digits n && digits f
        | _ -> false)
  in
  if not whole then
    fail
      "%s %S is not the uchar-case-v0 number lexeme [-]digits[.digits] or [-]digits/digits, the \
       denominator without a leading 0"
      at s;
  Value.q_of_lexeme s

let rec value_of_json ~at (decl : datatype option) (j : Yojson.Safe.t) : Value.t =
  let kvs = obj ~at j in
  match List.assoc_opt "constructor" kvs with
  | Some ctor_j -> constructor_of_json ~at decl kvs ctor_j
  | None ->
  let variant, v =
    match kvs with [ (k, v) ] -> (k, v) | _ -> fail "%s: expected one value variant, got %s" at (got j)
  in
  (* the variant's JSON string, positioned at the variant key *)
  let as_string () = str ~at:(child at variant) v in
  match (variant, decl) with
  | "number", Some (Number { measure_unit; _ } | Percentage { measure_unit; _ }) ->
      let u = match measure_unit with Some e -> Value.Unit e | None -> Value.Unitless in
      Value.Num (number_at ~at (as_string ()), u)
  (* WF-059 backstop: a value against a datatype-less declaration would
     split the executors (ref would read a bare unitless number where
     the integer-scaling translation cannot scale) — the shared loader
     refuses it in both, last-line behind the verifier's rejection *)
  | "number", None ->
      fail "%s: a value against a declaration without a datatype" at
  | "date", Some Date_in_days -> date_at ~at (as_string ())
  | "dateTime", Some Date_time_type -> datetime_at ~at (as_string ())
  | "boolean", Some Boolean -> (
      match v with
      | `Bool b -> Value.Bool b
      | j -> fail "%s: expected true or false, got %s" (child at variant) (got j))
  | "text", Some Text_type -> Value.Text (as_string ())
  | "enumeration", Some (Enumeration { label_list; _ }) ->
      let s = as_string () in
      if List.exists (function Some w -> norm w = norm s | None -> false) label_list then Value.Enum s
      else fail "%s: %S is not a declared enumeration value" at s
  (* the list value form of §2.5 (RUNTIME-SEMANTICS ch. 10.5): each element
     loads recursively against the declared element datatype, in order; a
     null/leeg element is refused — list positions are never leeg (ch. 10.1) *)
  | "list", Some (List_type { element }) ->
      Value.List_value
        (List.mapi
           (fun i ej ->
             match ej with
             | `Null ->
                 fail "%s[%d]: a list element is leeg; list positions are never leeg (ch. 10.1)" at i
             | _ -> value_of_json ~at:(Printf.sprintf "%s[%d]" at i) (Some element) ej)
           (list_ ~at:(child at variant) v))
  (* WF-059/WF-018 backstop, widened to the list value: a value against a
     datatype-less declaration splits the executors, so the shared loader
     refuses it in both *)
  | "list", None ->
      fail "%s: a value against a declaration without a datatype" at
  (* the record value form (RUNTIME-SEMANTICS ch. 11.3): exactly the declared
     fields, in declaration order, each loaded recursively against its field
     datatype. Fail fast on a missing, extra, wrong-typed or leeg field — the
     no-hole rule at the boundary (the WF-059/WF-018 backstop, widened). *)
  | "struct", Some (Struct_type { field_list; _ }) ->
      let field_kvs = obj ~at:(child at variant) v in
      if List.length field_kvs <> List.length field_list then
        fail "%s: record supplies %d fields, the struct declares %d" at (List.length field_kvs)
          (List.length field_list);
      Value.Record
        (List.map
           (fun (fname, fdt) ->
             match List.find_opt (fun (k, _) -> norm k = norm fname) field_kvs with
             | None -> fail "%s: record is missing declared field %S" at fname
             | Some (_, `Null) ->
                 fail "%s.%s: a record field is leeg; record fields are never leeg (ch. 11.1)" at fname
             | Some (_, fj) -> (fname, value_of_json ~at:(at ^ "." ^ fname) (Some fdt) fj))
           field_list)
  (* WF-059/WF-018 backstop, widened to the record value *)
  | "struct", None ->
      fail "%s: a value against a declaration without a datatype" at
  | _, Some _ -> fail "%s: value variant %S does not match the declared datatype" at variant
  | _, None -> fail "%s: value variant %S against an unknown datatype" at variant

(* The constructor value form of §2.5: {constructor, payload?}. The
   cross-checks JSON Schema cannot express live here (the WF-059/WF-018
   backstop, widened to the sum type): the tag names a declared constructor
   (ci), the payload is present iff the constructor is unary and loads
   recursively against the declared payload datatype. *)
and constructor_of_json ~at (decl : datatype option) kvs ctor_j : Value.t =
  let tag = str ~at:(child at "constructor") ctor_j in
  match decl with
  | Some (Sum_type { constructor_list; _ }) -> (
      match List.find_opt (fun (name, _) -> norm name = norm tag) constructor_list with
      | None -> fail "%s: %S is not a declared constructor of the sum type" at tag
      | Some (_, payload_decl) -> (
          match (payload_decl, List.assoc_opt "payload" kvs) with
          | Some pdt, Some pj ->
              Value.Ctor (tag, Some (value_of_json ~at:(child at "payload") (Some pdt) pj))
          | None, None -> Value.Ctor (tag, None)
          | Some _, None -> fail "%s: unary constructor %S carries no payload" at tag
          | None, Some _ -> fail "%s: nullary constructor %S carries a payload" at tag))
  | Some _ -> fail "%s: a constructor value against a non-sum-type declaration" at
  | None ->
      fail
        "%s: a constructor value against a declaration without a datatype"
        at

(* The segment list of a timeline-variant value, or None for any other
   value form *)
let timeline_json j = match j with `Assoc [ ("timeline", t) ] -> Some t | _ -> None

(* The full-timeline value variant, read from its segment list [t] (the
   value of the "timeline" key): ordered, non-overlapping, day-precision
   segments aligned to the declaration's granularity grid; segment values
   are scalars of the element datatype. From inclusive, to exclusive;
   absence between segments is a gap (leeg on read, ch. 3.1). *)
let timeline_of_json ?(allow_empty = false) ~at ~(grid : Timeline.grid) (decl : datatype option)
    (t : Yojson.Safe.t) : Timeline.t =
  let segs = list_ ~at:(child at "timeline") t in
  if segs = [] && not allow_empty then
    fail "%s: a timeline value carries at least one segment" at;
  (* The grid is checked through the calendar bucketing, and v0 defines
     buckets for month and year only (RUNTIME-SEMANTICS ch. 3.2; bucketing by
     week or quarter is OPEN in ch. 3.12). A timeline on the calendar's week
     or quarter grid is therefore refused at load as a v0 limit, not as a
     defect of the case, and the sentence says so. Every day sits on the day
     grid, so a boundary needs no bucket there. *)
  let no_grid grid =
    fail
      "%s: the declared grid is %s, and v0 defines calendar buckets for month and year only \
       (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12), so this loader has no %s grid to check the \
       segment boundaries against: refused as a v0 limit, not as a defect of the case"
      at grid grid
  in
  (* A grid of several units carries no operation, a case input included, and
     a variable start is laid per object from an anchor this loader does not
     read (RUNTIME-SEMANTICS ch. 3.2): both are refused as a v0 limit. A
     fixed start point lays a grid of its own, whose walls the segments
     must sit on whatever the granularity. *)
  if grid.g_count <> 1 then
    fail
      "%s: the declared grid is %d %s units, a multi-period grid, which carries no operation, a \
       case input included (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, \
       not as a defect of the case"
      at grid.g_count (Ir_v0.Spelling.encode Ir_v0.Spelling.granularity grid.g_gran);
  if grid.g_start = Timeline.Variable then
    fail
      "%s: the declared grid is laid per object from the anchor of a variable start, which this \
       loader does not read (RUNTIME-SEMANTICS ch. 3.2): refused as a v0 limit, not as a defect \
       of the case"
      at;
  let gran = grid.g_gran in
  let aligned d =
    match (grid.g_start, gran) with
    | (Timeline.Fixed_date _ | Timeline.Fixed_day_month _), _ ->
        let van, _ = Timeline.grid_bucket grid d in
        van = d
    | _, (Month | Year) ->
        let van, _ = Timeline.bucket gran d in
        van = d
    | _, Day -> true
    | _, Week -> no_grid "week"
    | _, Quarter -> no_grid "quarter"
  in
  let parsed =
    List.mapi
      (fun i sj ->
        let seg = Printf.sprintf "%s.timeline[%d]" at i in
        let kvs = obj ~at:seg sj in
        (* an omitted bound is open (RUNTIME-SEMANTICS ch. 3.1): the segment
           begins before every day or runs past every day, and no grid wall
           bounds it *)
        let boundary what ~open_bound =
          if List.mem_assoc what kvs then day_at ~at:(child seg what) (str_field ~at:seg kvs what)
          else open_bound
        in
        let van = boundary "from" ~open_bound:Timeline.open_from in
        let up_to = boundary "to" ~open_bound:Timeline.open_to in
        if up_to <= van then fail "%s: segment 'to' must lie after 'from'" seg;
        let on_grid d = d = Timeline.open_from || d = Timeline.open_to || aligned d in
        if not (on_grid van && on_grid up_to) then
          fail "%s: segment boundaries must sit on the declared granularity grid" seg;
        {
          Timeline.s_from = van;
          s_to = up_to;
          s_value = value_of_json ~at:(child seg "value") decl (field ~at:seg kvs "value");
        })
      segs
  in
  let rec check_order = function
    | a :: (b : Timeline.segment) :: rest ->
        if a.Timeline.s_to > b.s_from then
          fail "%s: timeline segments must be ordered and non-overlapping" at;
        check_order (b :: rest)
    | _ -> ()
  in
  check_order parsed;
  parsed

(* --- cross-checked loading --------------------------------------------- *)

(* The EFFECTIVE object type (IR-SPEC ch. 2.2): the declaration with its
   member maps closed over the supertype chain. Every member cross in this
   loader — supplied attribute, supplied characteristic, expected value,
   expected timeline, granularity — goes through here, so an inherited
   member is delivered and expected exactly like an own one. A case
   instance names its CONCRETE type in `objectType`; the chain is what the
   document adds to it, never something the case restates. *)
let find_object_type (doc : document) name =
  match Ir_v0.Checks.effective_ot doc name with
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
      (fun (ftn, (ft : fact_type_name)) ->
        (* the declared filler type is an UPPER bound (ch. 2.6): an
           instance of S fills every role declared at a T with S ⊑ T *)
        let fills (r : role_ref) =
          Ir_v0.Checks.subtype_of doc ~sub:otype
            ~super:(Option.value r.role_object_type_name ~default:"")
        in
        let matches (r : role_ref) =
          (match r.role_identifier with Some n -> norm n = norm key | None -> false)
          || (match r.role_plural with Some m -> norm m = norm key | None -> false)
        in
        match ft.role_names with
        | [ a; b ] ->
            (if fills a && matches b then [ (ftn, b, a) ] else [])
            @ if fills b && matches a then [ (ftn, a, b) ] else []
        | [ r ] when ft.reciprocal ->
            (* reciprocal: the single role is its own opposite *)
            if fills r && matches r then [ (ftn, r, r) ] else []
        | _ -> [])
      doc.fact_types
  in
  match hits with
  | [ p ] -> p
  | [] -> fail "role key %S does not resolve to a declared role opposite %S" key otype
  | _ ->
      failc ~params:[ ("roleKey", key); ("objectType", otype) ] "EF-001"
        "role key %S is ambiguous from %S" key otype

(* whether [key] names a role of some fact type, by its name or its plural *)
let role_declared (doc : document) key =
  List.exists
    (fun (_, (ft : fact_type_name)) ->
      List.exists
        (fun (r : role_ref) ->
          (match r.role_identifier with Some n -> norm n = norm key | None -> false)
          || match r.role_plural with Some m -> norm m = norm key | None -> false)
        ft.role_names)
    doc.fact_types

(* the role a key names: a role key as the case writes it on an instance of
   [otype], or the key a loaded world stores a link under (World.link_key),
   which names its fact type and role itself *)
let resolve_role doc ~otype ~key =
  let stored =
    List.concat_map
      (fun (ftn, (ft : fact_type_name)) ->
        List.filter
          (fun (r : role_ref) ->
            (r.role_identifier <> None || r.role_plural <> None)
            && String.equal (World.link_key ~fact_type:ftn r) key)
          ft.role_names)
      doc.fact_types
  in
  match stored with
  | [ r ] -> r
  | _ :: _ -> fail "the link key %S names more than one role" key
  | [] ->
      let _, far, _ = resolve_role_ft doc ~otype ~key in
      far

(* The cells variant of an attribute input, or None for any other value form *)
let cells_json j = match j with `Assoc [ ("cells", c) ] -> Some c | _ -> None

(* The cells of a dimensioned attribute's input (RUNTIME-SEMANTICS ch. 5.8c):
   each cell names one label of each dimension of the attribute, label i a
   label of dimension i (EF-034), the case names no cell twice (EF-035), and
   a cell's value has the attribute's datatype. The labels are positional,
   as on the wire (IR-SPEC ch. 4.3), so a case spells one cell one way. *)
(* whether [labels] name a cell of an attribute dimensioned by [dims]: label i
   a label of dimension i *)
let names_cell (doc : document) ~(dims : string list) labels =
  let declared dn =
    match Ir_v0.Checks.find_ci doc.dimensions dn with
    | Some (d : dimension) -> Ir_v0.Checks.some_names d.dim_values
    | None -> []
  in
  List.length labels = List.length dims
  && List.for_all2 (fun l dn -> Ir_v0.Checks.mem_ci (declared dn) l) labels dims

let no_cell_sentence ~at ~attr ~dims labels =
  Printf.sprintf
    "%s: the labels [%s] name no cell of %S, whose dimensions are %s: a cell names one label of each \
     dimension, in declared order"
    at (String.concat ", " labels) attr (String.concat ", " dims)

let cells_of_json (doc : document) ~label ~attr ~(dims : string list) (decl : datatype option) ~at
    (cj : Yojson.Safe.t) =
  let cells =
    List.mapi
      (fun i c ->
        let cat = Printf.sprintf "%s.cells[%d]" at i in
        let kvs = obj ~at:cat c in
        let labels =
          List.mapi (fun k l -> str ~at:(Printf.sprintf "%s.labels[%d]" cat k) l) (list_field ~at:cat kvs "labels")
        in
        if not (names_cell doc ~dims labels) then
          failc
            ~params:[ ("label", label); ("attribute", attr); ("labels", String.concat ", " labels) ]
            "EF-034" "%s" (no_cell_sentence ~at:cat ~attr ~dims labels);
        (labels, value_of_json ~at:(cat ^ ".value") decl (field ~at:cat kvs "value")))
      (list_ ~at:(child at "cells") cj)
  in
  ignore
    (List.fold_left
       (fun seen (labels, _) ->
         let key = List.map norm labels in
         if List.mem key seen then
           failc
             ~params:[ ("label", label); ("attribute", attr); ("labels", String.concat ", " labels) ]
             "EF-035" "%s: the cell [%s] of %S is supplied twice" at (String.concat ", " labels) attr;
         key :: seen)
       [] cells);
  cells

let load_instance (doc : document) ~at (j : Yojson.Safe.t) : World.instance =
  let kvs = obj ~at j in
  let label = str_field ~at kvs "label" in
  (* the schema asks minLength 1, and every later position of this instance
     is written after its label: an empty one would load and send those
     positions to the root ("objectType: …", ".vondstdag.date: …") *)
  if label = "" then fail "%s: expected a non-empty string, got \"\"" (child at "label");
  (* "::" is the minted-instance namespace (Eval.run_object_creation's
     deterministic labels); a case label there could silently suppress
     a creation — refused at load, never read wrong *)
  if contains ~needle:"::" label then
    fail "label %S: \"::\" is reserved for created instances" label;
  let otype_name = str_field ~at:label kvs "objectType" in
  (* a name the input writes that the document does not declare is EF-040
     (RUNTIME-SEMANTICS ch. 5.7b), with the instance's label and the name as
     written *)
  let undeclared name fmt = failc ~params:[ ("label", label); ("name", name) ] "EF-040" fmt in
  let ot =
    match Ir_v0.Checks.effective_ot doc otype_name with
    | Some ot -> ot
    | None -> undeclared otype_name "instance objectType %S is not declared" otype_name
  in
  let attrs, tls =
    List.fold_left
      (fun (attrs, tls) (name, vj) ->
        match Ir_v0.Checks.find_ci ot.attributes name with
        | Some decl -> (
            let at = label ^ "." ^ name in
            (* a dimensioned attribute has a value per cell and none of its own
               (IR-SPEC ch. 2.7), so its input is the cells variant and nothing
               else; a scalar there would load as a value the document cannot
               read (measured: ref and clingo both returned it to an
               attributeValue expectation) *)
            match (Ir_v0.Checks.attr_dimensions ot name, cells_json vj) with
            | Some dims, Some cj ->
                (* each cell is a member of its own (World.cell); a cell the
                   input does not name stays absent, which is empty *)
                ( List.fold_left
                    (fun attrs (labels, v) -> SMap.add (norm (World.cell name labels)) v attrs)
                    attrs
                    (cells_of_json doc ~label ~attr:name ~dims decl ~at cj),
                  tls )
            | Some _, None ->
                fail "%s: a dimensioned attribute takes its input per cell (\"cells\"), never a scalar \
                      or a timeline" at
            | None, Some _ -> fail "%s: a cells input on an undimensioned attribute" at
            | None, None ->
            match timeline_json vj with
            | Some t -> (
              match Ir_v0.Checks.find_ci ot.attribute_granularity name with
              | Some (Some _) ->
                  let grid = Timeline.attr_grid doc ot name in
                  (attrs, SMap.add (norm name) (timeline_of_json ~at ~grid decl t) tls)
              | Some None ->
                  (* An internal invariant, not a load error: the loader
                     reads a timeline facet's granularity as a required value,
                     so no document it loads reaches this arm. *)
                  invalid_arg
                    (Printf.sprintf
                       "Case: %s: a time-dependent attribute without a granularity, which the \
                        loader refuses since M1; this document was not built by the loader"
                       at)
              | None -> fail "%s: a timeline value on a non-temporal attribute" at)
            | None -> (SMap.add (norm name) (value_of_json ~at decl vj) attrs, tls))
        | None -> undeclared name "attribute %S is not declared on %S" name otype_name)
      (SMap.empty, SMap.empty)
      (obj_field ~at:label kvs "attributes")
  in
  let chars =
    List.mapi
      (fun i cj ->
        let c = str ~at:(Printf.sprintf "%s.characteristics[%d]" label i) cj in
        if not (Ir_v0.Checks.mem_ci (Ir_v0.Checks.some_names ot.characteristics) c) then
          undeclared c "characteristic %S is not declared on %S" c otype_name;
        (* The two deliveries are NOT interchangeable. A characteristic
           the document declares TIME-DEPENDENT has a timeline, and every
           temporal reader looks for one: the gedurende clip, the pro-rata fold
           and the day count all read the case's characteristic timelines and
           nothing else. Asserting such a characteristic in the flat
           `characteristics` list therefore delivers a fact no temporal reader
           ever consults, and the clip intersects against an EMPTY period set
           without a word — measured on the golden that pins 30: ref answered
           0, clingo answered 0, both silently.

           So it is refused here, at load, where the case is still nameable.
           The sister arm below refuses the mirror-image mistake — a
           timeline on a non-temporal characteristic — and the two cover
           the seam from both sides. The predicate is KEY EXISTENCE in
           [characteristic_granularity], the same one the three clip readers use;
           the key is a timeline facet on the characteristic (M1), whose
           granularity the loader requires, so key and value coincide for
           every loaded document. *)
        if List.exists (fun (k, _) -> norm k = norm c) ot.characteristic_granularity then
          fail
            "%s.%s: characteristic %S is declared time-dependent on %S, so it is delivered as a \
             \"characteristicTimelines\" entry — the static \"characteristics\" list asserts a \
             scalar truth no temporal reader consults"
            label c c otype_name;
        norm c)
      (list_field ~at:label kvs "characteristics")
  in
  let left_side =
    List.fold_left
      (fun (acc, seen) (key, vj) ->
        if not (role_declared doc key) then undeclared key "role key %S names no declared role" key;
        let role = resolve_role doc ~otype:otype_name ~key in
        (* one role, one key: a split delivery (the same role under its
           name AND its plural) would land two lists on the one key of the
           relation (World.link_key), one replacing the other — refuse it
           at load time, never read it silently wrong *)
        let role_id = Option.value role.role_identifier ~default:key in
        (match List.assoc_opt (norm role_id) seen with
        | Some prev ->
            fail "role keys %S and %S on %S both name the role %S; deliver one key" prev key
              label role_id
        | None -> ());
        let role_ref = child (child label "roles") key in
        let labels = List.mapi (fun i l -> str ~at:(Printf.sprintf "%s[%d]" role_ref i) l) (list_ ~at:role_ref vj) in
        (* a label the list repeats names the one relation instance again
           (ch. 5.7a): one link, in the order of its first occurrence *)
        let labels = List.rev (List.fold_left (fun acc l -> if List.mem l acc then acc else l :: acc) [] labels) in
        if (not role.role_many) && List.length labels <> 1 then
          fail "%s: to-one role %S carries %d labels" role_ref key (List.length labels);
        (SMap.add (norm key) labels acc, (norm role_id, key) :: seen))
      (SMap.empty, [])
      (obj_field ~at:label kvs "roles")
    |> fst
  in
  let ctls =
    match List.assoc_opt "characteristicTimelines" kvs with
    | None -> SMap.empty
    | Some j ->
        List.fold_left
          (fun acc (cname, pj) ->
            if not (Ir_v0.Checks.mem_ci (Ir_v0.Checks.some_names ot.characteristics) cname) then
              undeclared cname "characteristic %S is not declared on %S" cname otype_name;
            let gran =
              match
                List.find_opt (fun (k, _) -> norm k = norm cname) ot.characteristic_granularity
              with
              | Some (_, Some g) -> g
              | Some (_, None) ->
                  (* the internal invariant of the attribute arm above *)
                  invalid_arg
                    (Printf.sprintf
                       "Case: %s.%s: a time-dependent characteristic without a granularity, \
                        which the loader refuses since M1; this document was not built by the \
                        loader"
                       label cname)
              | None ->
                  fail "%s.%s: a characteristic timeline on a non-temporal characteristic"
                    label cname
            in
            ignore gran;
            let at = label ^ "." ^ cname in
            (* true periods are DAY-precise regardless of the declared
               granularity — the pinned runtime accepts a mid-month
               boundary on a month-granular characteristic (the ch. 3.7
               pro-rata's whole point); the granularity governs the
               characteristic's bucketing, never its period grid *)
            let periods =
              List.mapi
                (fun i sj ->
                  let per = Printf.sprintf "%s[%d]" at i in
                  let skvs = obj ~at:per sj in
                  let boundary what = day_at ~at:(child per what) (str_field ~at:per skvs what) in
                  let van = boundary "from" in
                  let up_to = boundary "to" in
                  if up_to <= van then fail "%s: period 'to' must lie after 'from'" per;
                  (van, up_to))
                (list_ ~at pj)
            in
            let sorted = List.sort compare periods in
            List.iteri
              (fun i (v, _) ->
                if i > 0 then
                  let _, prev_t = List.nth sorted (i - 1) in
                  if v < prev_t then fail "%s: periods overlap" at)
              sorted;
            SMap.add (norm cname) sorted acc)
          SMap.empty
          (obj ~at:(child label "characteristicTimelines") j)
  in
  { label; otype = otype_name; attrs; chars; left_side; tls; ctls }

(* ch. 5.7a: a supplied role link ESTABLISHES the relation instance, and
   BOTH roles read it — a relation is symmetric, so the reader arriving
   from the side the case did not name reaches the same relation the
   naming side reaches, never a spuriously empty far side. The loader
   therefore materialises the reverse leg of every supplied link, under
   the SISTER role (the role the supplying instance itself fills); the
   reciprocal shape (§13.3.9.2, one role that is its own opposite) is the
   special case in which sister and named role coincide, and the pinned
   runtime's both-legs write is that case of the same law.

   [world] carries the links as the case keys them (a role's name or
   plural); the result carries them under the key of their relation
   (World.link_key), so two fact types whose roles share a name keep
   their links apart. The supplied links are rekeyed first, then the
   reverse legs are added in a separate pass over the ORIGINAL links
   only: the outcome cannot depend on the order instances appear in, and
   a materialised leg never breeds another. Idempotent — a case
   supplying both legs describes one relation, not two — and a
   conflicting reverse leg on a to-one sister role (WF-013) is a load
   error, never a silent overwrite. *)
let materialise_relations (doc : document) (world : World.t) =
  let rekeyed =
    SMap.map
      (fun (i : World.instance) ->
        {
          i with
          left_side =
            SMap.fold
              (fun key targets acc ->
                let fact_type, far, _ = resolve_role_ft doc ~otype:i.otype ~key in
                SMap.add (World.link_key ~fact_type far) targets acc)
              i.left_side SMap.empty;
        })
      world.instances
  in
  (* the boundary of ch. 5.7a: what stands in a list before this pass the case
     wrote. The pass appends in ascending label order, the order it walks the
     instances in, so a materialised leg already stands in instance order *)
  let written =
    SMap.fold
      (fun label (i : World.instance) acc ->
        SMap.fold (fun key targets acc -> World.Key.add (label, key) (List.length targets) acc) i.left_side acc)
      rekeyed World.Key.empty
  in
  SMap.fold
    (fun _ (i : World.instance) w ->
      SMap.fold
        (fun key targets w ->
          let fact_type, _, sister = resolve_role_ft doc ~otype:i.otype ~key in
          List.fold_left
            (fun (w : World.t) t ->
              match SMap.find_opt t w.instances with
              | None -> w (* check_role_targets reports the unknown label *)
              | Some (ti : World.instance) ->
                  let existing = World.links ti ~fact_type sister in
                  if List.mem i.label existing then w
                  else if (not sister.role_many) && existing <> [] then
                    (* the role is spelled by its PLURAL where it declares
                       one, so a to-one role can be reported under a plural.
                       That reads like a contradiction, so the declared name
                       is spelled out whenever it differs. *)
                    let spelling = World.role_spelling sister in
                    let how =
                      match sister.role_identifier with
                      | Some n when norm n <> norm spelling ->
                          Printf.sprintf " (the storage spelling of the declared role %S)" n
                      | _ -> ""
                    in
                    fail
                      "role %S: %S already links %s under the to-one role %S%s; the reverse leg of \
                       %S -> %S conflicts"
                      key t
                      (String.concat ", " existing)
                      spelling how i.label t
                  else
                    {
                      w with
                      instances =
                        SMap.add t
                          {
                            ti with
                            left_side =
                              SMap.add (World.link_key ~fact_type sister) (existing @ [ i.label ]) ti.left_side;
                          }
                          w.instances;
                    })
            w targets)
        i.left_side w)
    world.instances
    { world with instances = rekeyed; written }

let check_role_targets (doc : document) (world : World.t) =
  SMap.iter
    (fun _ (i : World.instance) ->
      SMap.iter
        (fun key labels ->
          let role = resolve_role doc ~otype:i.otype ~key in
          let expect = Option.value role.role_object_type_name ~default:"" in
          List.iter
            (fun l ->
              match SMap.find_opt l world.instances with
              | None ->
                  failc
                    ~params:[ ("role", key); ("label", i.label); ("target", l) ]
                    "EF-002" "role %S on %S links unknown label %S" key i.label l
              | Some target ->
                  (* upper bound, not equality: a subtype instance fills
                     a supertype-typed role (ch. 2.6) *)
                  if not (Ir_v0.Checks.subtype_of doc ~sub:target.otype ~super:expect) then
                    fail "role %S on %S links %S of type %S; declared filler type is %S" key
                      i.label l target.otype expect)
            labels)
        i.left_side)
    world.instances

(* RUNTIME-SEMANTICS ch. 6.2: the forms of an instance-ordered cycle evaluate
   per object in the order of the chain its switch relation lays, and the
   chain must end. The links of that relation are the case's alone (no form
   writes it, IR-SPEC ch. 5.3), so a cycle in them is a property of the case
   input: refused here, before any rule fires, with EF-033. Without this the
   ASP backend grounds without bound and the reference interpreter has no
   depth to order by. The cycle reported is the one whose smallest label is
   the smallest, listed from that label along the role the hop reaches (the
   predecessor's), labels compared byte by byte. *)
let check_link_cycles (doc : document) (world : World.t) =
  let walks =
    List.sort_uniq compare
      (List.map
         (fun (o : Ir_v0.Checks.instance_order) -> (o.io_relation, o.io_role))
         (Ir_v0.Checks.instance_orders (Ir_v0.Checks.ctx_of doc)))
  in
  List.iter
    (fun (relation, role) ->
      (* the fact type as the document declares it: the parameter names it so *)
      let declared, reached =
        match List.find_opt (fun (n, _) -> norm n = norm relation) doc.fact_types with
        | Some (declared, (ft : fact_type_name)) -> (
            declared,
            match
              List.find_opt
                (fun (r : role_ref) ->
                  match r.role_identifier with Some n -> norm n = norm role | None -> false)
                ft.role_names
            with
            | Some r -> r
            | None -> invalid_arg ("Case.check_link_cycles: no role " ^ role ^ " of " ^ relation))
        | None -> invalid_arg ("Case.check_link_cycles: no fact type " ^ relation)
      in
      (* the predecessor: the link of the relation over the reached role *)
      let pred (i : World.instance) = World.links i ~fact_type:declared reached in
      let state = Hashtbl.create 16 in
      let cycles = ref [] in
      let rec walk path label =
        match Hashtbl.find_opt state label with
        | Some `Done -> ()
        | Some `On_path ->
            let rec from acc = function
              | l :: rest -> if l = label then l :: acc else from (l :: acc) rest
              | [] -> acc
            in
            cycles := from [] path :: !cycles
        | None -> (
            Hashtbl.replace state label `On_path;
            (match pred (World.get world label) with
            | p :: _ -> walk (label :: path) p
            | [] -> ());
            Hashtbl.replace state label `Done)
      in
      SMap.iter (fun label _ -> walk [] label) world.instances;
      let smallest c = List.fold_left min (List.hd c) c in
      match List.sort (fun a b -> compare (smallest a) (smallest b)) !cycles with
      | [] -> ()
      | c :: _ ->
          let rec rotate c = if List.hd c = smallest c then c else rotate (List.tl c @ [ List.hd c ]) in
          let c = rotate c in
          failc
            ~params:[ ("factType", declared); ("cycle", String.concat ", " c) ]
            "EF-033"
            "the links of the fact type %S form a cycle (%s): the forms of an instance-ordered cycle \
             evaluate per object along the chain this relation lays, and the chain must end \
             (RUNTIME-SEMANTICS ch. 6.2)"
            declared
            (String.concat " -> " (c @ [ List.hd c ])))
    walks

(* An expectation on the whole of a dimensioned attribute asks for a value it
   does not have: its values are its cells (IR-SPEC ch. 2.7). *)
let not_dimensioned ~at ot attr =
  if Ir_v0.Checks.attr_dimensions ot attr <> None then
    fail "%s: %S is a dimensioned attribute, whose values are its cells: an expectation on it names a \
          cell" at attr

(* A time-dependent member has a value per period and none of its own, so a
   scalar expectation on it asks a value the document does not give it: the
   case does not fit the declarations (RUNTIME-SEMANTICS ch. 5.7 step 3,
   ch. 8.4). Reading it as the value on the evaluation date would extend the
   asOfDate-bucket convention, which serves the temporal total only. The
   predicate is key existence in the granularity maps, as for the input. *)
let not_time_dependent_attribute ~at ot attr =
  if Ir_v0.Checks.find_ci ot.attribute_granularity attr <> None then
    fail "%s: %S is a time-dependent attribute, whose value is a timeline: an expectation on it is an \
          attributeTimeline" at attr

let not_time_dependent_characteristic ~at ot c =
  if List.exists (fun (k, _) -> norm k = norm c) ot.characteristic_granularity then
    fail "%s: %S is a time-dependent characteristic, which holds per period: a characteristicPresent or \
          characteristicAbsent expectation asks a scalar truth it does not have" at c

let load_expectation (doc : document) (world : World.t) ~at (j : Yojson.Safe.t) : expectation =
  let kvs = obj ~at j in
  let get_instance () =
    let label = str_field ~at kvs "label" in
    match SMap.find_opt label world.instances with
    | Some i -> i
    | None -> fail "expectation references unknown label %S" label
  in
  let kind = str_field ~at kvs "kind" in
  match kind with
  | "attributeValue" ->
      let i = get_instance () in
      let attr = str_field ~at kvs "attribute" in
      let ot = find_object_type doc i.otype in
      not_dimensioned ~at ot attr;
      let decl =
        match Ir_v0.Checks.find_ci ot.attributes attr with
        | Some d -> d
        | None -> fail "expected attribute %S is not declared on %S" attr i.otype
      in
      not_time_dependent_attribute ~at ot attr;
      Attribute_value
        {
          label = i.label;
          attr;
          value = value_of_json ~at:("expected " ^ i.label ^ "." ^ attr) decl (field ~at kvs "value");
        }
  | "attributeEmpty" ->
      let i = get_instance () in
      let attr = str_field ~at kvs "attribute" in
      let ot = find_object_type doc i.otype in
      if not (Ir_v0.Checks.has_attr ot attr) then
        fail "expected attribute %S is not declared on %S" attr i.otype;
      not_dimensioned ~at ot attr;
      not_time_dependent_attribute ~at ot attr;
      Attribute_empty { label = i.label; attr }
  | "attributeTimeline" ->
      let i = get_instance () in
      let attr = str_field ~at kvs "attribute" in
      let ot = find_object_type doc i.otype in
      not_dimensioned ~at ot attr;
      let decl =
        match Ir_v0.Checks.find_ci ot.attributes attr with
        | Some d -> d
        | None -> fail "expected attribute %S is not declared on %S" attr i.otype
      in
      let grid =
        match Ir_v0.Checks.find_ci ot.attribute_granularity attr with
        | Some (Some _) -> Timeline.attr_grid doc ot attr
        | Some None ->
            (* the internal invariant of the input arms above *)
            invalid_arg
              (Printf.sprintf
                 "Case: expected timeline on %S: a time-dependent attribute without a \
                  granularity, which the loader refuses since M1; this document was not built \
                  by the loader"
                 attr)
        | None -> fail "expected timeline on %S, which declares no granularity" attr
      in
      Attribute_timeline
        {
          label = i.label;
          attr;
          tl =
            timeline_of_json ~allow_empty:true
              ~at:("expected " ^ i.label ^ "." ^ attr)
              ~grid decl
              (field ~at kvs "timeline");
        }
  | "cellValue" | "cellEmpty" -> (
      let i = get_instance () in
      let attr = str_field ~at kvs "attribute" in
      let ot = find_object_type doc i.otype in
      let decl =
        match Ir_v0.Checks.find_ci ot.attributes attr with
        | Some d -> d
        | None -> fail "expected attribute %S is not declared on %S" attr i.otype
      in
      let dims =
        match Ir_v0.Checks.attr_dimensions ot attr with
        | Some dims -> dims
        | None -> fail "%s: %S is no dimensioned attribute: a cell expectation names a cell of one" at attr
      in
      let labels =
        List.mapi (fun k l -> str ~at:(Printf.sprintf "%s.labels[%d]" at k) l) (list_field ~at kvs "labels")
      in
      (* labels that name no cell ask after a member the document does not
         declare, as an unknown attribute does: a case error without a code *)
      if not (names_cell doc ~dims labels) then fail "%s" (no_cell_sentence ~at ~attr ~dims labels);
      match kind with
      | "cellValue" ->
          Cell_value
            {
              label = i.label;
              attr;
              labels;
              value =
                value_of_json ~at:("expected " ^ i.label ^ "." ^ World.cell attr labels) decl (field ~at kvs "value");
            }
      | _ -> Cell_empty { label = i.label; attr; labels })
  | "characteristicPresent" | "characteristicAbsent" ->
      let i = get_instance () in
      let c = str_field ~at kvs "characteristic" in
      let ot = find_object_type doc i.otype in
      if not (Ir_v0.Checks.mem_ci (Ir_v0.Checks.some_names ot.characteristics) c) then
        fail "expected characteristic %S is not declared on %S" c i.otype;
      not_time_dependent_characteristic ~at ot c;
      if kind = "characteristicPresent" then
        Char_present { label = i.label; char = c }
      else Char_absent { label = i.label; char = c }
  | "consistent" -> Consistent
  | "inconsistent" ->
      let opt_str k = Option.map (str ~at:(child at k)) (List.assoc_opt k kvs) in
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
        List.filter_map (fun (x : resolved_rule) -> x.rr_name) doc.resolved_rules
        @ List.filter_map (fun (x : resolved_object_creation) -> x.oc_name) doc.object_creations
        @ List.filter_map (fun (x : resolved_allocation) -> x.vd_name) doc.allocations
        @ List.filter_map (fun (x : resolved_consistency) -> x.cons_name) doc.consistencies
        @ List.filter_map (fun (x : resolved_uniqueness) -> x.uni_name) doc.uniquenesses
        @ List.filter_map (fun (x : resolved_uniqueness_union) -> x.uu_name) doc.uniqueness_unions
        @ List.filter_map (fun (x : resolved_fact_creation) -> x.fc_name) doc.fact_creations
        @ List.concat_map
            (fun (l : prioritized_defaults) ->
              let target_form =
                l.pd_subject ^ "."
                ^ match (l.pd_characteristic, l.pd_attribute) with
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
                       match r.default_exceptions with
                       | None -> []
                       | Some egs ->
                           let p = prefix @ [ i ] in
                           (target_form ^ "/" ^ String.concat "/" (List.map string_of_int p))
                           :: List.concat_map
                                (fun (eg : exception_group) -> nested p eg.eg_defaults)
                                egs.egs_members)
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
      | None, Some _ -> fail "%s: an inconsistent label pin requires a rule pin" at
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
          else Inconsistent { rule; label; code = opt_str "code" }
      | rule, label -> Inconsistent { rule; label; code = opt_str "code" })
  | k -> fail "%s: unknown expectation kind %S" (child at "kind") k

let expected_fault_of_kvs kvs =
  match List.assoc_opt "expectedFault" kvs with
  | None -> None
  | Some j ->
      let at = "expectedFault" in
      let fk = obj ~at j in
      (* a leftover fragment key (ref, clingo, drools) is refused here, beside the schema *)
      List.iter
        (fun (k, _) ->
          if k <> "code" && k <> "params" then fail "%s: unknown field %S" (where at) k)
        fk;
      let params =
        List.map (fun (k, v) -> (k, str ~at:(child (child at "params") k) v)) (obj_field ~at fk "params")
      in
      Some { ef_code = str_field ~at fk "code"; ef_params = params }

(* whether an observed fault is the pinned one: the same code, and every pinned
   parameter among the observed (a subset: an executor may name more than a
   case pins, never less) *)
let pin_matches (pin : expected_fault) ~code ~params =
  code = Some pin.ef_code
  && List.for_all (fun (k, v) -> List.assoc_opt k params = Some v) pin.ef_params

(* The pinned code and parameters read on their OWN, without the world — the
   split every executor's runner performs before loading anything
   else. A case whose expected fault falls while the INPUT is being
   read (an ambiguous role key, a link to an undeclared label) never
   reaches a parse that sits behind that input, so its pin would
   be unreachable in precisely the file that names it, and the whole
   class of case-reader refusals could not be pinned at all. The field
   is the case's contract, not part of its input, so it is parsed
   without one. *)
let expected_fault_of_file path : expected_fault option =
  expected_fault_of_kvs (obj ~at:root (Yojson.Safe.from_file path))

let load_file (doc : document) path : t =
  let j = Yojson.Safe.from_file path in
  let kvs = obj ~at:root j in
  (match str_field ~at:root kvs "format" with
  | "uchar-case-v0" -> ()
  | f -> fail "unsupported case format %S" f);
  let as_of =
    match field ~at:root kvs "asOfDate" with
    | `Null -> None
    | `String s ->
        (* read here, at load, so a non-date is refused before any
           reader downstream sees it; the schema pattern alone admits
           "2026-13-01" *)
        ignore (date_at ~at:"asOfDate" s);
        Some s
    | j -> fail "asOfDate: expected a string or null, got %s" (got j)
  in
  let input = obj_field ~at:root kvs "input" in
  let world =
    List.fold_left
      (fun (w : World.t) (n, ij) ->
        let i = load_instance doc ~at:(Printf.sprintf "input.instances[%d]" n) ij in
        (* label uniqueness is this loader's cross-check (see the header);
           left to the world's own guard it surfaced as an evaluation
           fault, exit 1, from a case that never got to evaluation *)
        if SMap.mem i.label w.instances then
          failc ~params:[ ("label", i.label) ] "EF-039" "duplicate instance label %S" i.label;
        World.add_instance w i)
      World.empty
      (List.mapi (fun n ij -> (n, ij)) (list_field ~at:"input" input "instances"))
  in
  (* the targets are checked on the links as the case keys them; the reverse
     legs materialised after are typed by construction *)
  check_role_targets doc world;
  let world = materialise_relations doc world in
  check_link_cycles doc world;
  let params, param_tls =
    List.fold_left
      (fun (params, ptls) (name, vj) ->
        match Ir_v0.Checks.find_ci doc.parameters name with
        | Some p -> (
            let at = "parameter " ^ name in
            match timeline_json vj with
            | Some t -> (
              match p.par_granularity with
              | Some _ ->
                  (params,
                   SMap.add (norm name)
                     (timeline_of_json ~at ~grid:(Timeline.param_grid p) p.par_datatype t)
                     ptls)
              | None -> fail "%s: a timeline value on a non-granular parameter" at)
            | None ->
                if p.par_granularity <> None then
                  (* the symmetric refusal: a granular parameter takes a
                     timeline, never a scalar — the scalar slot is unread
                     (ch. 8), so accepting one only breeds divergence *)
                  fail "%s: a scalar value on a granular parameter (it takes a timeline)" at
                else (SMap.add (norm name) (value_of_json ~at p.par_datatype vj) params, ptls))
        | None -> fail "parameter %S is not declared" name)
      (SMap.empty, SMap.empty)
      (obj_field ~at:"input" input "parameters")
  in
  let expected_fault = expected_fault_of_kvs kvs in
  let expected =
    match (List.assoc_opt "expected" kvs, expected_fault) with
    | Some j, None ->
        List.mapi
          (fun n ej -> load_expectation doc world ~at:(Printf.sprintf "expected[%d]" n) ej)
          (list_ ~at:"expected" j)
    | None, Some _ -> []
    | Some _, Some _ -> fail "a case carries exactly one of expected / expectedFault"
    | None, None -> fail "%s: missing field %S" (where root) "expected"
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
      | Cell_value { label; attr; labels; value } ->
          let member = World.cell attr labels in
          let actual = World.attr (World.get final label) member in
          if Arith.equal actual value then None
          else
            Some
              (Printf.sprintf "%s.%s = %s, expected %s" label member (Value.to_string actual)
                 (Value.to_string value))
      | Cell_empty { label; attr; labels } ->
          let member = World.cell attr labels in
          let actual = World.attr (World.get final label) member in
          if actual = Value.Empty then None
          else Some (Printf.sprintf "%s.%s = %s, expected leeg" label member (Value.to_string actual))
      | Attribute_timeline { label; attr; tl } -> (
          let bound z =
            if z = Timeline.open_from then "-inf" else if z = Timeline.open_to then "+inf" else Value.iso_of_date z
          in
          let show (s : Timeline.segment) =
            Printf.sprintf "[%s,%s)=%s" (bound s.s_from) (bound s.s_to) (Value.to_string s.s_value)
          in
          let render l = if l = [] then "(empty)" else String.concat " " (List.map show l) in
          (* both sides in the canonical form of ch. 3.1: the expectation pins
             what the timeline reads on each day, not how a producer or the
             case divides a run of equal values into segments *)
          let tl = Timeline.canonical tl in
          match World.timeline (World.get final label) attr with
          | None when tl = [] ->
              (* a never-written temporal target and an explicitly empty
                 timeline read leeg on every day (ch. 3.1) — the empty
                 expectation matches both, as it does on the clingo side *)
              None
          | None ->
              Some (Printf.sprintf "%s.%s carries no timeline, expected %s" label attr (render tl))
          | Some actual ->
              let actual = Timeline.canonical actual in
              let same =
                List.length actual = List.length tl
                && List.for_all2
                     (fun (a : Timeline.segment) (b : Timeline.segment) ->
                       a.s_from = b.s_from && a.s_to = b.s_to && Arith.equal a.s_value b.s_value)
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
      | Inconsistent { rule; label; code } -> (
          let matches (f : World.error_fact) =
            (match rule with
            | None -> true
            | Some r ->
                (* version-stripped on both sides: the channel is keyed
                   by rule name version-stripped (ch. 5.6), so a pin
                   "X" matches a deposit from version "X #2" *)
                norm (Ir_v0.Checks.strip_version f.rule)
                = norm (Ir_v0.Checks.strip_version r))
            && (match label with None -> true | Some l -> String.equal f.subject l)
            && match code with None -> true | Some c -> List.mem c f.codes
          in
          match List.find_opt matches final.channel with
          | Some _ -> None
          | None ->
              let because =
                match (rule, label) with
                | None, _ -> "any channel fact"
                | Some r, None -> Printf.sprintf "a channel fact from rule %S" r
                | Some r, Some l -> Printf.sprintf "a channel fact from rule %S on %S" r l
              in
              (* a pinned code shows the codes of every fact; without one the
                 sentence stays as it was before facts carried codes *)
              let because, shown =
                match code with
                | None -> (because, fun (f : World.error_fact) -> f.rule ^ " × " ^ f.subject)
                | Some c ->
                    ( Printf.sprintf "%s with code %s" because c,
                      fun (f : World.error_fact) ->
                        Printf.sprintf "%s × %s [%s]" f.rule f.subject (String.concat ", " f.codes) )
              in
              Some
                (Printf.sprintf "expected inconsistent (%s), but the channel carries: %s" because
                   (if final.channel = [] then "(empty)"
                    else String.concat "; " (List.map shown final.channel)))))
    case.expected
