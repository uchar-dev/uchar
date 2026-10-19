(* The value domain of the reference interpreter (RUNTIME-SEMANTICS ch. 1.1):
   an exact rational, a text, a boolean, a date (day or millisecond
   precision), an enumeration value, a constructor value (ch. 9.1) — or
   empty, the distinguished absence of a value. Empty is not zero, not the
   empty string, not false, and distinct from Unresolved (an unresolvable
   construct, which faults evaluation).

   Numbers carry their unit; unit algebra lives in Arith (ch. 2.6).

   Adopted reading, recorded: date equality follows the numeric equality
   rules of ch. 1.2 (a day-precision date is a day count), not the
   non-numeric text/enum/boolean rules of §5 Tabel 38. *)

type unit_ =
  | Unitless
  | Unit of Ir_v0.Ast.measure_unit
      (* a declared unit, possibly a rate (noemer set) *)
  | Product of Ir_v0.Ast.measure_unit * Ir_v0.Ast.measure_unit
      (* opaque product unit, ch. 2.6 *)

type t =
  | Empty
  | Num of Q.t * unit_
  | Text of string
  | Bool of bool
  | Date of int (* civil days since 1970-01-01 *)
  | Date_time of int (* milliseconds since 1970-01-01T00:00:00 *)
  | Enum of string
  | Ctor of string * t option
      (* a constructor value (RUNTIME-SEMANTICS ch. 9.1): a declared tag
         and, for a unary constructor, its payload value; it never carries
         a leeg payload — a Construct with a leeg argument propagates leeg *)
  | List_value of t list
      (* a list value (RUNTIME-SEMANTICS ch. 10.1): an ordered, possibly
         empty sequence of element values. [] is a value (gevuld), distinct
         from leeg; no element position is ever leeg — a ListLiteral with a
         leeg element propagates, and the case loader rejects leeg elements *)
  | Record of (string * t) list
      (* a record value (RUNTIME-SEMANTICS ch. 11.1): a declared struct
         type's declared fields, in declaration order, each mapped to a
         value of the field's datatype. It is gevuld by construction and
         never carries a leeg field — a StructLiteral with a leeg field
         propagates, and the case loader rejects leeg fields (the no-hole
         rule, the Ctor/Lijst sibling) *)

(* A fault aborts the evaluation observably (hard runtime error, ch. 1.4
   point 4; Unresolved, WF-044; a non-representable bounded write).

   [code] is the fault kind's EF code (RUNTIME-SEMANTICS ch. 5.7b) where the
   raising site names one, beside the text and never inside it: a report
   renders it at its head ([report]), so no substring of the text moves.
   [params] are the parameters of the code (ch. 5.7b) as name/lexeme pairs, in
   the order of the table: data beside the text, never inside it. *)
exception Fault of { code : string option; params : (string * string) list; text : string }

let fault fmt = Printf.ksprintf (fun s -> raise (Fault { code = None; params = []; text = s })) fmt

(* a fault of the kind [code] *)
let faultc ?(params = []) code fmt =
  Printf.ksprintf (fun s -> raise (Fault { code = Some code; params; text = s })) fmt

(* the report of a fault: its code at the head, "EF-005: division by numeric zero" *)
let report ~code text = match code with None -> text | Some c -> c ^ ": " ^ text

(* A §9.5 deposit (RUNTIME-SEMANTICS ch. 5.6), which a producer hands to the
   [err] of the rule × subject it evaluates: the EF code of its kind (ch. 5.7b)
   beside its text, the way a fault carries one, and none for a kind that has
   no code yet. The channel fact keeps the code; the text is the producer's own
   and no report renders it yet. *)
type deposit = { code : string option; text : string }

(* --- civil-date conversion (proleptic Gregorian) ----------------------- *)

let days_from_civil ~y ~m ~d =
  let y = if m <= 2 then y - 1 else y in
  (* era arithmetic needs floor division; OCaml (/) truncates toward zero *)
  let era = (if y >= 0 then y else y - 399) / 400 in
  let yoe = y - (era * 400) in
  let mp = (m + 9) mod 12 in
  let doy = (((153 * mp) + 2) / 5) + d - 1 in
  let doe = (yoe * 365) + (yoe / 4) - (yoe / 100) + doy in
  (era * 146097) + doe - 719468

let civil_from_days z =
  let z = z + 719468 in
  let era = (if z >= 0 then z else z - 146096) / 146097 in
  let doe = z - (era * 146097) in
  let yoe = (doe - (doe / 1460) + (doe / 36524) - (doe / 146096)) / 365 in
  let y = yoe + (era * 400) in
  let doy = doe - ((365 * yoe) + (yoe / 4) - (yoe / 100)) in
  let mp = ((5 * doy) + 2) / 153 in
  let d = doy - (((153 * mp) + 2) / 5) + 1 in
  let m = if mp < 10 then mp + 3 else mp - 9 in
  ((if m <= 2 then y + 1 else y), m, d)

let is_leap y = (y mod 4 = 0 && y mod 100 <> 0) || y mod 400 = 0

let days_in_month ~y ~m =
  match m with
  | 1 | 3 | 5 | 7 | 8 | 10 | 12 -> 31
  | 4 | 6 | 9 | 11 -> 30
  | 2 -> if is_leap y then 29 else 28
  | _ -> 0

(* WF-084: a date literal's lexeme is the strict ISO form the executors parse
   (IR-SPEC ch. 4.1) — fixed field widths yyyy-mm-dd, all digits, month 1–12,
   day 1..days-in-month. A lax split (int_of_string per '-' part) read the
   Dutch source snippet "31-01-2024" as year 31 / month 1 / day 2024 (which
   rolls over to 0036-07-16) — a silent misparse both executors shared. The
   fence faults loudly instead; WF-084 refuses such a lexeme at verification
   time, ahead of any executor. *)
let date_of_iso s =
  let digits str = String.for_all (fun c -> c >= '0' && c <= '9') str in
  match String.split_on_char '-' s with
  | [ y; m; d ]
    when String.length y = 4 && String.length m = 2 && String.length d = 2
         && digits y && digits m && digits d -> (
      let y = int_of_string y and m = int_of_string m and d = int_of_string d in
      if m < 1 || m > 12 then fault "month out of range in date lexeme %S" s
      else if d < 1 || d > days_in_month ~y ~m then fault "day out of range in date lexeme %S" s
      else Date (days_from_civil ~y ~m ~d))
  | _ -> fault "malformed date lexeme %S (expected ISO yyyy-mm-dd)" s

let iso_of_date z =
  let y, m, d = civil_from_days z in
  Printf.sprintf "%04d-%02d-%02d" y m d

(* --- datum-tijd lexemes (WF-095, IR-SPEC ch. 4.1) ---------------------- *)

(* One lexeme names a millisecond-precision instant, and every reader in the
   house admits exactly this one: the canonical ISO form
   yyyy-mm-ddThh:mm:ss[.fff] — fixed field widths, 'T' as the separator (never
   a space), the seconds field always written, no zone suffix and no offset,
   a fraction that is optional but exactly three digits, read as whole
   milliseconds.

   The RegelSpraak source snippet [dd. ]d-m-yyyy hh:mm:ss[.SSS] is refused
   like any other form. It was a second admitted spelling while the v0
   emitter wrote it into `waarde`; since 2026-09-22 the emitter writes the ISO
   lexeme there and keeps the snippet as the node's source text (since the L5
   landing the source record, `source.snippet`), and WF-095 admits the ISO
   form alone. A reader that still read the snippet would answer documents no
   producer writes and the verifier rejects, so it was narrowed with them
   (2026-09-23).

   Anything else is refused WITH THE POSITION at which the grammar diverges.
   That is the whole point: a reader that guesses — dropping a zone suffix,
   taking a space for 'T', truncating a fourth fraction digit, reading ".1"
   as one millisecond where a sibling reads a hundred — answers an instant
   the producer never wrote, and no diagnostic anywhere says so.

   The CASE wire carries the same form: the `dateTime` pattern of
   uchar-case-v0 is exactly [read_iso_datetime], so the case loader reuses
   this reader rather than holding a second, laxer opinion. *)

exception Dt_bad of string

let ms_per_day = 86_400_000

let read_iso_datetime s : (int, string) result =
  let n = String.length s in
  let pos = ref 0 in
  let bad msg = raise (Dt_bad msg) in
  let because w = bad (Printf.sprintf "at offset %d: expected %s" !pos w) in
  let digit i = i < n && s.[i] >= '0' && s.[i] <= '9' in
  let ch c = if !pos < n && s.[!pos] = c then incr pos else because (Printf.sprintf "%C" c) in
  (* exactly [k] digits, no more and no fewer at this position *)
  let fixed k w =
    let rec ok j = j = k || (digit (!pos + j) && ok (j + 1)) in
    if not (ok 0) then because w;
    let v = int_of_string (String.sub s !pos k) in
    pos := !pos + k;
    v
  in
  try
    let y = fixed 4 "four year digits" in
    ch '-';
    let m = fixed 2 "two month digits" in
    ch '-';
    let d = fixed 2 "two day digits" in
    ch 'T';
    let hh = fixed 2 "two hour digits" in
    ch ':';
    let mi = fixed 2 "two minute digits" in
    ch ':';
    let ss = fixed 2 "two second digits" in
    let frac =
      if !pos < n && s.[!pos] = '.' then begin
        incr pos;
        fixed 3 "exactly three fraction digits"
      end
      else 0
    in
    if !pos <> n then because "the end of the lexeme";
    if m < 1 || m > 12 then bad (Printf.sprintf "month %d is out of range" m);
    if d < 1 || d > days_in_month ~y ~m then bad (Printf.sprintf "day %d is out of range" d);
    if hh > 23 then bad (Printf.sprintf "hour %d is out of range" hh);
    if mi > 59 then bad (Printf.sprintf "minute %d is out of range" mi);
    if ss > 59 then bad (Printf.sprintf "second %d is out of range" ss);
    Ok ((days_from_civil ~y ~m ~d * ms_per_day) + ((((hh * 60) + mi) * 60) + ss) * 1000 + frac)
  with Dt_bad msg -> Error msg

let datetime_of_lexeme s =
  match read_iso_datetime s with
  | Ok ms -> Date_time ms
  | Error why ->
      fault
        "malformed datum-tijd lexeme %S: not the canonical ISO form \
         yyyy-mm-ddThh:mm:ss[.fff] (%s)"
        s why

(* --- number lexemes ---------------------------------------------------- *)

(* Exact decimal ("12.34") or rational ("a/b") lexeme to a rational; the
   wire and the case format never carry floats. One grammar for both: the
   `number` pattern of uchar-case-v0, which a literal's number variant carries
   since the L4 landing (WF-081) — digits with an optional point and digits
   on both sides of it, or two digit strings around a '/' whose denominator
   does not start with 0, each with an optional leading '-'. A decimal comma,
   a bare leading or trailing point and a denominator such as "04" are
   therefore malformed here, and never read as the number a reader might
   guess. Validation is strict: Q.of_string alone would let "" become 0 and
   "1/0" become +inf silently — both fault here instead. *)
let q_of_lexeme s =
  let digits d = d <> "" && String.for_all (fun c -> c >= '0' && c <= '9') d in
  let split_sign d =
    if String.length d > 0 && d.[0] = '-' then (true, String.sub d 1 (String.length d - 1))
    else (false, d)
  in
  match String.index_opt s '/' with
  | Some i ->
      let num = String.sub s 0 i in
      let den = String.sub s (i + 1) (String.length s - i - 1) in
      let neg, num_body = split_sign num in
      if not (digits num_body && digits den) then fault "malformed number lexeme %S" s
      else
        let d = Z.of_string den in
        if Z.equal d Z.zero then fault "zero denominator in number lexeme %S" s
        else if den.[0] = '0' then fault "malformed number lexeme %S" s
        else
          let n = Z.of_string num_body in
          Q.make (if neg then Z.neg n else n) d
  | None -> (
      match String.index_opt s '.' with
      | None ->
          let neg, body = split_sign s in
          if not (digits body) then fault "malformed number lexeme %S" s
          else
            let n = Z.of_string body in
            Q.of_bigint (if neg then Z.neg n else n)
      | Some i ->
          let whole = String.sub s 0 i in
          let frac = String.sub s (i + 1) (String.length s - i - 1) in
          let neg, whole_body = split_sign whole in
          let body = whole_body ^ frac in
          if not (digits whole_body && digits frac) then fault "malformed number lexeme %S" s
          else
            let num = Z.of_string body in
            let num = if neg then Z.neg num else num in
            Q.make num (Z.pow (Z.of_int 10) (String.length frac)))

(* Render a rational as a decimal lexeme when it terminates (reduced
   denominator has no prime factors besides 2 and 5, ch. 2.1); as "a/b"
   otherwise. *)
let lexeme_of_q q =
  let num = Q.num q and den = Q.den q in
  let twos =
    let rec strip n k = if Z.equal (Z.rem n (Z.of_int 2)) Z.zero then strip (Z.divexact n (Z.of_int 2)) (k + 1) else (n, k) in
    strip den 0
  in
  let rest2, e2 = twos in
  let rest, e5 =
    let rec strip n k = if Z.equal (Z.rem n (Z.of_int 5)) Z.zero then strip (Z.divexact n (Z.of_int 5)) (k + 1) else (n, k) in
    strip rest2 0
  in
  if not (Z.equal rest Z.one) then Q.to_string q
  else
    let d = max e2 e5 in
    let scaled = Z.mul num (Z.divexact (Z.pow (Z.of_int 10) d) den) in
    if d = 0 then Z.to_string scaled
    else
      let s = Z.to_string (Z.abs scaled) in
      let s = if String.length s <= d then String.make (d + 1 - String.length s) '0' ^ s else s in
      let cut = String.length s - d in
      (if Z.sign scaled < 0 then "-" else "")
      ^ String.sub s 0 cut ^ "." ^ String.sub s cut d

let rec to_string = function
  | Empty -> "leeg"
  | Num (q, _) -> lexeme_of_q q
  | Text s -> Printf.sprintf "%S" s
  | Bool b -> if b then "waar" else "onwaar"
  | Date z -> iso_of_date z
  | Date_time ms -> Printf.sprintf "@%dms" ms
  | Enum s -> s
  (* the deterministic constructor rendering: Toegekend(1250.50), Afgewezen *)
  | Ctor (tag, None) -> tag
  | Ctor (tag, Some v) -> Printf.sprintf "%s(%s)" tag (to_string v)
  (* the deterministic list rendering: [12.5, 30, 7.25], [] *)
  | List_value xs -> "[" ^ String.concat ", " (List.map to_string xs) ^ "]"
  (* the deterministic record rendering: {waarde: 125.5, telling: 2} *)
  | Record fs ->
      "{" ^ String.concat ", " (List.map (fun (n, v) -> n ^ ": " ^ to_string v) fs) ^ "}"
