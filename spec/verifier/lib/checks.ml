(* Well-formedness checks over a loaded uchar-ir-v1 document (L5 shape).

   Each check implements one WF rule of spec/IR-SPEC.md, restricted to what
   the wire format can express (the WF index records per rule which
   clauses are covered here and which stay OPEN). Conventions:

   - name matching is case-insensitive over whitespace-collapsed strings
     (WF-001..003); timeline names additionally strip one leading Dutch
     article (the v0 identity rule of IR-SPEC ch. 2.8);
   - the loader reads a named form into one record per version ([Ast]),
     and a form with several versions names them with the suffix " #i";
     the version-aware checks read the form through the pointer each
     version carries;
   - a violation carries the WF id, a path, and a reason. The path is a
     JSON pointer into the document down to the node that is at fault
     (`/model/objectTypes/<i>/attributes/<j>/timeline/count`,
     `/rules/rules/<f>/versions/<v>/condition`), as the loader's
     errors are; below a node some checks add a segment of their own
     reading (a struct field or a function parameter by its name). A
     version of a named form is cited by the wire pointer the loader
     recorded for it, below which every segment is the field name the wire
     writes. The other positions keep the spelling the checks had before
     the root nested — a declaration by its v0 table and name
     (`/objecttypen/<name>/...`), a day-kind definition, a start-point
     determination, a rule group or a defaults group by its list and index
     — and [v] and [all] map it onto the document (see [m1_root]). *)

open Ast

type violation = { wf : string; at : string; reason : string }

(* Since M1 the rule lists sit under "rules" and the model lists under
   "model" (IR-SPEC ch. 2). The checks build their paths in the spelling
   they had before the root nested, and every violation is mapped onto the
   M1 document in two steps: [m1_root], here, moves a path under its new
   parent and renames the model lists that carry no names; [m1_named], in
   [all], needs the document, because it turns a declaration's name into
   its list index and an object type's v0 facet table into the member
   node. A form of the six named forms records the wire pointer of its
   version as [df_at] ([dep_forms]), the key ref's stratum lookup asks for;
   a pointer that already starts at "rules" passes [m1_root] unchanged. The
   four lists below are the rule lists the checks address by index, under
   their wire names; a named form is always cited by its recorded pointer. *)
let m1_rule_lists =
  [ "dayKindDefinitions"; "startPointDeterminations"; "ruleGroups"; "prioritizedDefaults" ]

let m1_model_lists =
  [ ("tijdlijnen", "timelines"); ("dagsoorten", "dayKinds"); ("functionDefs", "functionDefs") ]

let m1_root at =
  match String.split_on_char '/' at with
  | "" :: root :: rest when List.mem root m1_rule_lists ->
      String.concat "/" ("" :: "rules" :: root :: rest)
  | "" :: root :: rest when List.mem_assoc root m1_model_lists ->
      String.concat "/" ("" :: "model" :: List.assoc root m1_model_lists :: rest)
  | _ -> at

let v wf at reason = { wf; at = m1_root at; reason }

(* The pointers of a named form (IR-SPEC ch. 3, Versioning). A version is
   cited by `/rules/<list>/<f>/versions/<v>`, the pointer the loader
   recorded ([Ast]); the form's own fields — its name, a rule's statute
   annotations — sit on the form, the version pointer without its last two
   segments. *)
let form_pointer at =
  match List.rev (String.split_on_char '/' at) with
  | _ :: "versions" :: rest -> String.concat "/" (List.rev rest)
  | _ -> at

let is_version_pointer at = form_pointer at <> at

(* --- name normalisation ------------------------------------------------- *)

let collapse_ws s =
  String.split_on_char ' ' s
  |> List.concat_map (String.split_on_char '\t')
  |> List.filter (fun x -> x <> "")
  |> String.concat " "

let norm s = String.lowercase_ascii (collapse_ws s)

let strip_article s =
  let n = norm s in
  let strip p = if String.length n > String.length p
                   && String.sub n 0 (String.length p) = p
                then Some (String.sub n (String.length p)
                             (String.length n - String.length p))
                else None in
  match strip "de ", strip "het ", strip "een " with
  | Some r, _, _ | _, Some r, _ | _, _, Some r -> r
  | None, None, None -> n

(* Timeline-name identity: collapse, lowercase, one leading article. *)
let tl_norm = strip_article

(* Strip a trailing " #<digits>" version suffix (v0 flattening). *)
let strip_version name =
  match String.rindex_opt name '#' with
  | Some i
    when i >= 2
         && name.[i - 1] = ' '
         && i + 1 < String.length name
         && String.for_all
              (fun c -> c >= '0' && c <= '9')
              (String.sub name (i + 1) (String.length name - i - 1)) ->
      String.sub name 0 (i - 1)
  | _ -> name

let is_versioned name = strip_version name <> name

(* --- generic helpers ---------------------------------------------------- *)

let dups_by keyf items =
  (* items: (display, at) list; returns one violation-worthy pair per
     colliding key, keeping the first two occurrences. *)
  let tbl = Hashtbl.create 16 in
  List.filter_map
    (fun (display, at) ->
      let k = keyf display in
      match Hashtbl.find_opt tbl k with
      | None -> Hashtbl.add tbl k display; None
      | Some first -> Some (first, display, at))
    items

let find_ci assoc name =
  List.find_opt (fun (k, _) -> norm k = norm name) assoc |> Option.map snd

let mem_ci l name = List.exists (fun k -> norm k = norm name) l

let some_names l = List.filter_map (fun x -> x) l

(* Interval overlap over ISO date strings; None = open bound; both bounds
   inclusive (IR-SPEC ch. 3.10 Validity). ISO dates compare lexicographically
   — an assumption WF-101 (below) turns into a demand on the document: every
   written bound is the strict yyyy-mm-dd lexeme and a real calendar day, so
   `<=` on the raw string IS calendar order here. Without that rule the
   assumption held of nothing: a sign-prefixed extended year sorts before
   every four-digit year and a non-date sorts after every one of them, both
   silently (measured 2026-09-11). *)
let validity_overlaps (a : geldigheid option) (b : geldigheid option) =
  let bounds = function
    | None -> (None, None)
    | Some g -> (g.geld_vanaf, g.geld_tot)
  in
  let (a1, a2) = bounds a and (b1, b2) = bounds b in
  let le lo hi =
    match lo, hi with None, _ | _, None -> true | Some l, Some h -> l <= h
  in
  (* [a1,a2] and [b1,b2] intersect iff a1 <= b2 and b1 <= a2. *)
  le a1 b2 && le b1 a2

(* --- datatype predicates ------------------------------------------------ *)

let is_number_like = function Numeriek _ | Percentage _ -> true | _ -> false
let is_date = function Datum_in_dagen | Datum_tijd -> true | _ -> false

let is_orderable = function
  | Numeriek _ | Percentage _ | Datum_in_dagen | Datum_tijd -> true
  | _ -> false

let unit_of = function
  | Numeriek { eenheid; _ } | Percentage { eenheid; _ } -> eenheid
  | _ -> None

(* Convertibility at dump level: same shape (plain vs rate), canonical names
   equal within the same system, rates recursively convertible (IR-SPEC
   ch. 2.5). Conservative: unknown structure counts as compatible, so this
   check under-reports rather than mis-fires. *)
let rec units_convertible (a : eenheid) (b : eenheid) =
  let canon (u : eenheid) =
    match u.canoniek with Some c -> Some (norm c)
    | None -> Option.map norm u.afkorting
  in
  (match canon a, canon b with
   | Some x, Some y -> x = y
   | _ -> true)
  && (match a.noemer, b.noemer with
      | None, None -> true
      | Some x, Some y -> units_convertible x y
      | _ -> false)

let units_compatible a b =
  match a, b with
  | None, None -> true
  | Some x, Some y -> units_convertible x y
  | _ -> false

(* WF-035/036 mirrored convenience (IR-SPEC ch. 4.6): a unitless numeric
   literal adopts its sibling's unit before any unit-compatibility check. *)
let is_unitless_numeric_literal = function
  | Some (Literal { typ = Some (Numeriek { eenheid = None; _ }); _ })
  | Some (Literal { typ = Some (Percentage { eenheid = None; _ }); _ }) -> true
  | _ -> false

let units_compatible_adopting lexpr rexpr a b =
  is_unitless_numeric_literal lexpr
  || is_unitless_numeric_literal rexpr
  || units_compatible a b

(* --- the time-unit spelling table (RUNTIME-SEMANTICS ch. 3.8) ----------- *)

(* The nine calendar units, keyed on SPELLING (case-insensitive) — the one
   normative table the verifier's WF-035/WF-038 clauses and the executors'
   calendar arithmetic share. Declared conversion factors and the declaring
   system are irrelevant to calendar arithmetic (a unit may name no system):
   a year is a calendar year, never a second count. *)
type time_unit =
  | Tu_jaar | Tu_kwartaal | Tu_maand | Tu_week | Tu_dag
  | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde

let time_unit_of_spelling s =
  match String.lowercase_ascii (String.trim s) with
  | "jaar" | "jr" | "jaren" -> Some Tu_jaar
  | "maand" | "mnd" | "maanden" -> Some Tu_maand
  | "kwartaal" | "kw" | "kwartalen" -> Some Tu_kwartaal
  | "week" | "wk" | "weken" -> Some Tu_week
  | "dag" | "dg" | "dagen" -> Some Tu_dag
  | "uur" | "u" | "uren" -> Some Tu_uur
  | "minuut" | "min" | "minuten" -> Some Tu_minuut
  | "seconde" | "sec" | "s" | "seconden" | "secondes" -> Some Tu_seconde
  | "milliseconde" | "ms" | "milliseconden" | "millisecondes" -> Some Tu_milliseconde
  | _ -> None

let time_unit_of_eenheid (e : eenheid) =
  match e.afkorting with Some a -> time_unit_of_spelling a | None -> None

(* sub-day units demand millisecond-precision operands *)
let time_unit_sub_day = function
  | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde -> true
  | Tu_jaar | Tu_kwartaal | Tu_maand | Tu_week | Tu_dag -> false

(* The sign of a number variant's string. *)
let lexeme_negative s =
  let s = String.trim s in
  String.length s > 0 && s.[0] = '-'

let lexeme_zero s =
  let s = String.trim s in
  let s = if String.length s > 0 && (s.[0] = '-' || s.[0] = '+')
          then String.sub s 1 (String.length s - 1) else s in
  s <> "" && String.for_all (fun c -> c = '0' || c = ',' || c = '.') s

(* WF-081: the string of a literal's number variant (IR-SPEC ch. 4.1) is
   the number pattern of the case format, one grammar for one variant:
     integer  ::= '-'? digits
     decimal  ::= '-'? digits '.' digits
     rational ::= '-'? digits '/' [1-9] [0-9]*   (a denominator without a
                                                  leading zero, never zero)
     digits   ::= [0-9]+
   A decimal comma is no spelling of a number since L4 (the author's spelling
   stays in the source snippet), and neither is a fraction without a leading
   digit. Anything else — unit or currency tokens, whitespace, '+', exponents,
   grouping separators — is not a number; the executors fault on it, so the
   verifier refuses it up front. *)
let lexeme_bare_number s =
  let digits d = d <> "" && String.for_all (fun c -> c >= '0' && c <= '9') d in
  let strip_sign d =
    if String.length d > 0 && d.[0] = '-' then String.sub d 1 (String.length d - 1) else d
  in
  match String.index_opt s '/' with
  | Some i ->
      let num = strip_sign (String.sub s 0 i) in
      let den = String.sub s (i + 1) (String.length s - i - 1) in
      digits num && digits den && den.[0] <> '0'
  | None -> (
      match String.index_opt s '.' with
      | None -> digits (strip_sign s)
      | Some i ->
          let whole = strip_sign (String.sub s 0 i) in
          let frac = String.sub s (i + 1) (String.length s - i - 1) in
          digits whole && digits frac)

(* WF-084: the executors' shared date-literal lexeme grammar (IR-SPEC ch. 4.1).
   A day-precision date literal's value MUST be the strict ISO form yyyy-mm-dd: fixed field
   widths (4-2-2 digits), month 1–12, day 1..days-in-month (leap-aware). The
   Dutch source snippet "31-01-2024" or "dd. 31-01-2024" is ill-formed — the
   bare form the executors misparsed as year 31 (→ 0036-07-16), the prefixed
   form they fault on; the verifier refuses both up front. Mirror of the
   ref/clingo date_of_iso fence (ref/lib/value.ml). *)
let is_leap y = (y mod 4 = 0 && y mod 100 <> 0) || y mod 400 = 0

let days_in_month y m =
  match m with
  | 1 | 3 | 5 | 7 | 8 | 10 | 12 -> 31
  | 4 | 6 | 9 | 11 -> 30
  | 2 -> if is_leap y then 29 else 28
  | _ -> 0

let lexeme_iso_date s =
  let digits str = str <> "" && String.for_all (fun c -> c >= '0' && c <= '9') str in
  match String.split_on_char '-' s with
  | [ y; m; d ]
    when String.length y = 4 && String.length m = 2 && String.length d = 2
         && digits y && digits m && digits d ->
      let y = int_of_string y and m = int_of_string m and d = int_of_string d in
      m >= 1 && m <= 12 && d >= 1 && d <= days_in_month y m
  | _ -> false

(* WF-095: the literal lexeme of a millisecond-precision date (IR-SPEC
   ch. 4.1) is ONE spelling, the canonical ISO form yyyy-mm-ddThh:mm:ss[.fff]
   — fixed field widths, 'T' as the separator and never a space, the seconds
   field always written, no
   zone suffix and no offset, a fraction that is optional but exactly three
   digits, read as whole milliseconds, the date half calendar-valid.

   Outside it the readers of such a lexeme stop agreeing — the same
   three digits ".1" read as ONE millisecond, as a HUNDRED, and refused; a
   missing seconds field taken by one reader and refused by another — and
   until this rule a literal reading `rommel 2024-01-31 12:00:00Z x` was
   schema-valid and verifier-clean (measured 2026-09-06). The day-first
   RegelSpraak source snippet was admitted as a second spelling only while
   the emitter still wrote it into `waarde`; since the emitter writes ISO
   there (card p5-datumtijd-lexeem, 2026-09-22) the snippet belongs in the
   source record alone, as WF-084 already has it for a day-precision date. *)
let lexeme_datum_tijd s =
  let n = String.length s in
  let digit i = i < n && s.[i] >= '0' && s.[i] <= '9' in
  let digits_at i k =
    let rec go j = j = k || (digit (i + j) && go (j + 1)) in
    go 0
  in
  let int_at i k = int_of_string (String.sub s i k) in
  (* hh:mm:ss[.fff], ending EXACTLY at the end of the lexeme: the anchor is
     what keeps a zone suffix, an offset and trailing text out *)
  let time_ok i =
    (n - i = 8 || n - i = 12)
    && digits_at i 2 && s.[i + 2] = ':' && digits_at (i + 3) 2 && s.[i + 5] = ':'
    && digits_at (i + 6) 2
    && (n - i = 8 || (s.[i + 8] = '.' && digits_at (i + 9) 3))
    && int_at i 2 <= 23
    && int_at (i + 3) 2 <= 59
    && int_at (i + 6) 2 <= 59
  in
  let date_ok ~y ~m ~d = m >= 1 && m <= 12 && d >= 1 && d <= days_in_month y m in
  n >= 19 && digits_at 0 4 && s.[4] = '-' && digits_at 5 2 && s.[7] = '-' && digits_at 8 2
  && s.[10] = 'T' && time_ok 11
  && date_ok ~y:(int_at 0 4) ~m:(int_at 5 2) ~d:(int_at 8 2)

(* --- the sentences the executors' loader shares ---------------------- *)

(* WF-114 (IR-SPEC ch. 4.1): the value variant a literal's type names — a
   Number (a percentage included) names number, a Boolean boolean, a Text
   text, a day-precision Date date, a millisecond-precision Date dateTime,
   an Enumeration enumeration, a SumType constructor; a list or struct type
   names none. [literal_variant_refusal] is the rule's sentence, and the
   loader refuses with the same sentence wherever an executor loads the
   document ([Loader.reader]), so the two cannot drift apart. *)
let literal_variant_key = function
  | Lv_number _ -> "number"
  | Lv_boolean _ -> "boolean"
  | Lv_text _ -> "text"
  | Lv_date _ -> "date"
  | Lv_date_time _ -> "dateTime"
  | Lv_enumeration _ -> "enumeration"
  | Lv_constructor _ -> "constructor"

let variant_named_by = function
  | Numeriek _ | Percentage _ -> Some "number"
  | Booleaan -> Some "boolean"
  | Tekst -> Some "text"
  | Datum_in_dagen -> Some "date"
  | Datum_tijd -> Some "dateTime"
  | Enumeratie _ -> Some "enumeration"
  | Sum_type _ -> Some "constructor"
  | List_type _ | Struct_type _ -> None

let literal_variant_refusal (variant : literal_variant) (typ : datatype) =
  let key = literal_variant_key variant in
  if variant_named_by typ = Some key then None
  else
    let named =
      match typ with
      | Numeriek _ | Percentage _ -> "a Number"
      | Booleaan -> "a Boolean"
      | Tekst -> "a Text"
      | Datum_in_dagen -> "a day-precision Date"
      | Datum_tijd -> "a millisecond-precision Date"
      | Enumeratie _ -> "an Enumeration"
      | Sum_type _ -> "a SumType"
      | List_type _ -> "a ListType"
      | Struct_type _ -> "a StructType"
    in
    Some
      (Printf.sprintf "literal value is %s %s value but its type is %s: the variant is the one \
                       the type names"
         (if key = "enumeration" then "an" else "a")
         key named)

(* WF-098 (IR-SPEC ch. 4.1): a context value names one of two session
   values, compared byte for byte. The bound day of a day-kind definition is
   no context value since L4 but a node of its own (BoundDay), so a
   ContextValue naming it falls outside the set like any other name. Shared
   with the loader like the sentence above. *)
let context_value_names = Spelling.context_value_names

let context_value_refusal name =
  if List.mem name context_value_names then None
  else
    Some
      ("context value '" ^ name
     ^ "' is not one of the two admissible names evaluationDate, evaluationYear: the \
        context-value name is a closed set, spelled byte for byte")

(* WF-108 (a) (IR-SPEC ch. 4.5): the one quantifier a QuantifierFold does not
   admit. The schema admits `the` on a Compound alone; the verifier loads it
   on a fold so that the rule names it, and every executor refuses it at load
   with this sentence ([Loader.reader]). *)
let quantifier_fold_the_refusal =
  "quantifier fold with quantifier the: the bare definite is a description of ONE member, not \
   a quantification over the set — a fold over it has no reading"

(* WF-113 (a) (IR-SPEC ch. 2): the one wire severity. The schema admits
   warning alone; the verifier loads error, info and a null severity so that
   the rule names them, and every executor refuses them at load with these
   sentences ([Loader.reader]). *)
let severity_refusal : severity option -> string option = function
  | Some Sev_warning -> None
  | Some Sev_error ->
      Some
        "severity error is not a wire severity: an error means the producer refused to write \
         the document, so a document that was written carries none"
  | Some Sev_info ->
      Some
        "severity info is not a wire severity: the list is the account of what stayed \
         unresolved, and info is tooling talk"
  | None -> Some "diagnostic without a severity: the one wire severity is warning"

(* WF-081, WF-084 and WF-095 (IR-SPEC ch. 4.1): the string of a number, date
   or dateTime variant outside its grammar ([lexeme_bare_number],
   [lexeme_iso_date], [lexeme_datum_tijd]). Shared with the loader like the
   two sentences above. *)
let number_lexeme_refusal w =
  if lexeme_bare_number w then None
  else
    Some
      ("numeric literal lexeme '" ^ w
     ^ "' is not a number of the case format (an optional '-', digits, an optional '.' and \
        digits, or a rational a/b whose denominator has no leading zero — ch. 4.1)")

let date_lexeme_refusal w =
  if lexeme_iso_date w then None
  else
    Some
      ("date literal lexeme '" ^ w
     ^ "' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges — ch. 4.1)")

let date_time_lexeme_refusal w =
  if lexeme_datum_tijd w then None
  else
    Some
      ("millisecond-precision date literal lexeme '" ^ w
     ^ "' is not the canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' \
        never a space, the seconds field written, no zone or offset, a fraction of exactly \
        three digits; the day-first source snippet belongs in `source` — ch. 4.1)")

(* --- document context --------------------------------------------------- *)

type ctx = { doc : document }

(* --- the supertype chain (IR-SPEC ch. 2.2) ------------------------------- *)

(* The DECLARED record, exactly as the document writes it. Two rules read
   this raw view on purpose: WF-002 (duplicates within one declaration) and
   WF-105 (shadowing, which needs to see own and inherited apart). Reading
   those on the chain-closed view below would make every inherited member a
   duplicate of itself. (WF-034's side-map key cross was the third until M1
   folded the side maps into member nodes.) WF-103 is the next and is a
   different reason: a supertype reference must name a DECLARED type, so
   resolving it through the chain would be circular.

   Every other raw reader in this file is not a name LOOKUP at all but an
   iteration over the declaration table, checking each record where it is
   written — WF-001, WF-002, WF-003, WF-004, WF-005,
   WF-007, WF-092, WF-057, WF-059, WF-061, WF-065,
   WF-072, WF-088, WF-104 and WF-105. Those are raw BY CONSTRUCTION and
   must stay so: their subject is the declaration at that JSON path, and a
   chain-closed view would report one inherited member once per descendant,
   at paths where the document says nothing. Swept 2026-09-12 (card
   subtype-verdeling-keten); every name-keyed member cross — the allocation
   fields, an object creation's subject and created type, a uniqueness key,
   a consistency subject, every reference position in the expression walk —
   already resolves through [objecttype] below. *)
let declared_objecttype ctx name = find_ci ctx.doc.objecttypen name

(* The chain from [ot] upward, own type first, root last. Cycle-safe by
   construction — a name already on the path stops the walk — because
   WF-104 reports the cycle while every other check must still terminate
   on that same document. *)
let chain_from ctx (ot : objecttype) =
  let rec go seen (ot : objecttype) acc =
    let acc = ot :: acc in
    match ot.ot_supertype with
    | None -> List.rev acc
    | Some s when List.mem (norm s) seen -> List.rev acc
    | Some s -> (
        match declared_objecttype ctx s with
        | None -> List.rev acc
        | Some p -> go (norm s :: seen) p acc)
  in
  go [ norm (Option.value ot.ot_naam ~default:"") ] ot []

(* The EFFECTIVE object type: the declared record with every member list
   replaced by the chain-closed union, NEAREST DECLARATION FIRST, so
   [find_ci]'s first-hit answer is exactly the upward resolution of
   ch. 2.2. Identity on a root type — which is every type in a document
   without a supertype edge, so the chain-closed view costs nothing where
   there is no chain. *)
let effective_objecttype ctx (ot : objecttype) =
  match ot.ot_supertype with
  | None -> ot
  | Some _ ->
      let types = chain_from ctx ot in
      let cat f = List.concat_map f types in
      {
        ot with
        attributen = cat (fun t -> t.attributen);
        attribuut_meervoud = cat (fun t -> t.attribuut_meervoud);
        attribuut_dimensies = cat (fun t -> t.attribuut_dimensies);
        attribuut_granulariteit = cat (fun t -> t.attribuut_granulariteit);
        attribuut_tijdlijn_aantal = cat (fun t -> t.attribuut_tijdlijn_aantal);
        attribuut_tijdlijn_naam = cat (fun t -> t.attribuut_tijdlijn_naam);
        kenmerken = cat (fun t -> t.kenmerken);
        kenmerk_soorten = cat (fun t -> t.kenmerk_soorten);
        kenmerk_granulariteit = cat (fun t -> t.kenmerk_granulariteit);
        kenmerk_tijdlijn_aantal = cat (fun t -> t.kenmerk_tijdlijn_aantal);
      }

(* Resolution walks the chain UPWARD ONLY: this is the one lookup every
   member cross goes through, so a member declared on a strict subtype and
   read at the supertype still does not resolve — not every instance of the
   supertype has it. That refusal is the chain-aware WF-034. *)
let objecttype ctx name = Option.map (effective_objecttype ctx) (declared_objecttype ctx name)

(* S ⊑ T — the reflexive-transitive closure of the supertype edge. *)
let is_sub ctx ~sub ~super =
  norm sub = norm super
  ||
  match declared_objecttype ctx sub with
  | None -> false
  | Some ot ->
      List.exists
        (fun (t : objecttype) ->
          match t.ot_naam with Some n -> norm n = norm super | None -> false)
        (chain_from ctx ot)

(* Comparable in one chain, either direction — WF-047's "agree" widened
   from equality to ⊑-compatibility (IR-SPEC ch. 2.6). The role's declared
   filler type is an UPPER bound, so a node naming a subtype of it is
   sound; a node naming a supertype of it reads upward from every filler
   and is sound too. The unsound direction — reading a strict subtype's
   member through this position — is refused where it is expressible, by
   the member cross above. *)
let type_compatible ctx a b = is_sub ctx ~sub:a ~super:b || is_sub ctx ~sub:b ~super:a

(* The same three answers for a consumer that holds a DOCUMENT rather than
   a check context — the reference interpreter's loader and evaluator and
   the ASP translator resolve members through these, so the chain has one
   definition in the house and the three readers cannot drift on it. *)
let effective_ot doc name = objecttype { doc } name
let subtype_of doc ~sub ~super = is_sub { doc } ~sub ~super

(* CAN one object be both an [a] and a [b]? Populations are EXTENSIONAL — a
   declaration names a set of objects, and a subtype's objects are its
   supertype's too — so the question is whether the DECLARED type graph holds a
   type below both. `⊑` answers only half of it: `a ⊑ b` says every a IS a b,
   while this says some object MIGHT be both, which is the weaker and, for an
   ambiguity test, the right question.

   WF-109 (IR-SPEC ch. 4.6 (q2)) needs exactly this. Counting hosts by `⊑`
   alone was measured wrong on 2026-09-19: with `Sein ⊑ Kustvak` a concrete
   `Sein` fills an anchor position declared `Kustvak`, so an owner naming `Sein`
   designates the anchor AND the target at run time while a `⊑` count sees one.
   Verifier, `ref` and the ASP backend all passed that document.

   Written as the EXISTENTIAL and not as `comparable`, though the two coincide
   while `supertype` is a single bare string (a chain is linear, so a common
   descendant puts both types on one chain): the extensional question is what
   the rule means, and a wire that ever grows a second parent must not silently
   change this answer. The two direct directions ride in front as the fast path
   and as the answer for a type the table does not declare. *)
let populations_overlap doc a b =
  let ctx = { doc } in
  is_sub ctx ~sub:a ~super:b
  || is_sub ctx ~sub:b ~super:a
  || List.exists
       (fun ((name, _) : string * objecttype) ->
         is_sub ctx ~sub:name ~super:a && is_sub ctx ~sub:name ~super:b)
       doc.objecttypen

(* The type on [from]'s chain that DECLARES [member] — [from] itself when it
   declares it. The ch. 7.2 conflict key names this type and not the
   instance's concrete type: a target is declared once, and keying on the
   concrete type would make one target report under as many names as it has
   subtypes. *)
let declaring_type doc ~from ~kind ~member =
  match declared_objecttype { doc } from with
  | None -> None
  | Some ot ->
      List.find_map
        (fun (t : objecttype) ->
          let hit =
            match kind with
            | `Attribute -> List.exists (fun (k, _) -> norm k = norm member) t.attributen
            | `Characteristic -> mem_ci (some_names t.kenmerken) member
          in
          if hit then t.ot_naam else None)
        (chain_from { doc } ot)

(* A defaults group's target as the ch. 7.10 composition keys it: the target
   KIND and name, without the subject, so an attribute and a characteristic
   sharing a name (WF-002) stay two targets. *)
let target_key (l : prioritized_defaults) =
  (if l.pd_kenmerk <> None then "K:" else "A:")
  ^
  match (l.pd_kenmerk, l.pd_attribuut) with
  | Some k, _ -> norm k
  | _, Some a -> norm a
  | _ -> "?"

(* The gate of RUNTIME-SEMANTICS ch. 7.10: the target keys on which [t]'s own
   chain — [t] and its ancestors — declares a defaults GROUP. An ordinary rule
   is folded into a stack only on such a key. The fold ([Desugar]) and
   WF-079's chain arm read this one predicate, so the verifier and the three
   executors cannot disagree on which rules the fold absorbs. *)
let chain_group_keys doc t =
  List.concat_map
    (fun (s, _) ->
      if subtype_of doc ~sub:t ~super:s then
        List.filter_map
          (fun (l : prioritized_defaults) ->
            if norm l.pd_onderwerp = norm s then Some (target_key l) else None)
          doc.prioritized_defaults
      else [])
    doc.objecttypen
  |> List.sort_uniq compare

let attr_of ot name = find_ci ot.attributen name
let has_attr ot name = List.exists (fun (k, _) -> norm k = norm name) ot.attributen
let has_kenmerk ot name = mem_ci (some_names ot.kenmerken) name

let attr_temporal ot name =
  List.exists (fun (k, _) -> norm k = norm name) ot.attribuut_granulariteit

let find_tijdlijn ctx name =
  List.find_opt
    (fun tl -> match tl.tl_naam with
       | Some n -> tl_norm n = tl_norm name
       | None -> false)
    ctx.doc.tijdlijnen

(* The spellings of a day kind: its name and its plural, every spelling it
   has on the wire (IR-SPEC ch. 2.9). Both name it as a duration unit. *)
let dagsoort_spellingen (d : dagsoort) = d.dk_naam :: Option.to_list d.dk_meervoud

let dagsoort_namen ctx = List.map (fun (d : dagsoort) -> d.dk_naam) ctx.doc.dagsoorten

(* a unit spelling that names a declared day kind: admitted beside the nine
   time units in date arithmetic and tijdsduur (WF-035/WF-038) — the
   executors stay loud on those forms *)
let day_kind_unit ctx (u : eenheid) =
  match u.afkorting with
  | Some a ->
      List.exists (fun d -> List.exists (fun s -> norm s = norm a) (dagsoort_spellingen d))
        ctx.doc.dagsoorten
  | None -> false

(* The position of a version pointer in its wire list, form index first:
   the order the wire writes the versions in. *)
let wire_order at =
  match List.rev (String.split_on_char '/' at) with
  | v :: "versions" :: f :: _ -> (int_of_string f, int_of_string v)
  | _ -> (0, 0)

(* All named rule forms, per wire list: (list, versions-with-validity), one
   entry per version in wire order, cited by its version pointer. The
   single-source and union uniquenesses are one wire list (delta B27); the
   loader keeps them in two in-memory lists, merged back here. *)
let form_arrays ctx =
  [ ("rules", List.map (fun r -> (r.rr_naam, r.rr_geldigheid, r.rr_pointer)) ctx.doc.resolved_regels);
    ("objectCreations", List.map (fun r -> (r.oc_naam, r.oc_geldigheid, r.oc_pointer)) ctx.doc.object_creaties);
    ("allocations", List.map (fun r -> (r.vd_naam, r.vd_geldigheid, r.vd_pointer)) ctx.doc.verdelingen);
    ("consistencies",
     List.map (fun r -> (r.cons_naam, r.cons_geldigheid, r.cons_pointer)) ctx.doc.consistenties);
    ("uniquenesses",
     List.sort
       (fun (_, _, a) (_, _, b) -> compare (wire_order a) (wire_order b))
       (List.map (fun r -> (r.uni_naam, r.uni_geldigheid, r.uni_pointer)) ctx.doc.uniciteiten
       @ List.map (fun r -> (r.uu_naam, r.uu_geldigheid, r.uu_pointer)) ctx.doc.uniciteit_unies));
    ("factCreations", List.map (fun r -> (r.fc_naam, r.fc_geldigheid, r.fc_pointer)) ctx.doc.feit_creaties) ]

(* Every array that puts a NAME on the wire: the seven versioned rule forms
   above plus the defaults groups of ch. 6.1, which carry a name and a
   validity window but no version grouping and no rule-group membership.

   The two lists are deliberately different, and the difference is not an
   oversight either way. WF-021 (version windows) and WF-033 (group
   membership) are statements about the VERSIONED ch. 3 forms and stop at
   [form_arrays]; the two name-identity rules — WF-107 (a name is present)
   and WF-020 (names are unique) — hold of every name this wire carries and
   read this one. Before it, a defaults group's name was crossed by nothing
   at all. *)
let named_forms ctx =
  form_arrays ctx
  @ [ ("prioritizedDefaults",
       List.mapi
         (fun i (l : prioritized_defaults) ->
           (l.pd_naam, l.pd_geldigheid, Printf.sprintf "/prioritizedDefaults/%d" i))
         ctx.doc.prioritized_defaults) ]

(* The FORMS of one array as the wire writes them, one entry per form:
   (the form's own name, the form's pointer, its versions with validity and
   pointer). The versions of a form are the entries that share its form
   pointer; a form with several names them "<name> #<i>" in memory ([Ast]),
   which is not the form's name, so the suffix of its first version comes
   off. A defaults group is a form of its own with no versions to group. *)
let forms_of entries =
  let rec group acc = function
    | [] -> List.rev acc
    | ((_, _, at) as e) :: rest ->
        let key = form_pointer at in
        let mine, others = List.partition (fun (_, _, at') -> form_pointer at' = key) rest in
        group ((key, e :: mine) :: acc) others
  in
  List.map
    (fun (key, versions) ->
      let name =
        match versions with
        | [ (n, _, _) ] -> n
        | (n, _, _) :: _ ->
            Option.map
              (fun n ->
                let k = String.length n - 3 in
                if k >= 0 && String.sub n k 3 = " #1" then String.sub n 0 k else n)
              n
        | [] -> None
      in
      (name, key, versions))
    (group [] entries)

(* ======================================================================= *)
(* Model layer: WF-001 .. WF-018                                           *)
(* ======================================================================= *)

let wf001 ctx =
  (* [root] is the in-library path a report is translated from ([m1_root],
     [m1_named]); [label] is the namespace the message names: the wire list
     (`objectTypes`, `domains`, `parameters`, `unitSystems`, `factTypes`,
     `dimensions`, `dayKinds`), while the in-library paths keep their v0
     names, which [m1_root] and [m1_named] map onto the wire. *)
  let ns ?label root keys =
    let label = Option.value label ~default:root in
    dups_by norm (List.map (fun k -> (k, "/" ^ root)) keys)
    |> List.map (fun (a, b, at) ->
        v "WF-001" at
          (Printf.sprintf "namespace '%s': names '%s' and '%s' collide case-insensitively" label a b))
  in
  ns ~label:"objectTypes" "objecttypen" (List.map fst ctx.doc.objecttypen)
  @ ns ~label:"domains" "domeinen" (List.map fst ctx.doc.domeinen)
  @ ns "parameters" (List.map fst ctx.doc.parameters)
  @ ns ~label:"unitSystems" "eenheidstelsels" (List.map fst ctx.doc.eenheidstelsels)
  @ ns ~label:"factTypes" "feittypen" (List.map fst ctx.doc.feittypen)
  @ ns ~label:"dimensions" "dimensies" (List.map fst ctx.doc.dimensies)
  @ ns ~label:"dayKinds" "dagsoorten" (dagsoort_namen ctx)

let wf002 ctx =
  List.concat_map
    (fun (otn, ot) ->
      let at = "/objecttypen/" ^ otn in
      (dups_by norm (List.map (fun (k, _) -> (k, at ^ "/attributen")) ot.attributen)
       |> List.map (fun (a, b, p) ->
           v "WF-002" p (Printf.sprintf "attributes '%s' and '%s' collide case-insensitively" a b)))
      @ (dups_by norm (List.map (fun k -> (k, at ^ "/kenmerken")) (some_names ot.kenmerken))
         |> List.map (fun (a, b, p) ->
             v "WF-002" p (Printf.sprintf "characteristics '%s' and '%s' collide case-insensitively" a b))))
    ctx.doc.objecttypen

let wf003 ctx =
  (* Model-layer resolution: enumeration domains resolve (an unfindable
     domain is a declaration naming an undeclared one); role filler
     types resolve; named-timeline references resolve. (Parameters: WF-018;
     dimension refs: WF-004.) *)
  let dt_check at = function
    | Some (Enumeratie { domein = Some d; _ }) when find_ci ctx.doc.domeinen d = None ->
        [ v "WF-003" at ("enumeration references undeclared domain '" ^ d ^ "'") ]
    | _ -> []
  in
  List.concat_map
    (fun (otn, ot) ->
      let at = "/objecttypen/" ^ otn in
      List.concat_map (fun (a, dt) -> dt_check (at ^ "/attributen/" ^ a) dt) ot.attributen
      @ List.concat_map
          (fun (a, tln) ->
            match tln with
            | Some n when find_tijdlijn ctx n = None ->
                [ v "WF-003" (at ^ "/attribuutTijdlijnNaam/" ^ a)
                    ("reference to undeclared timeline '" ^ n ^ "'") ]
            | _ -> [])
          ot.attribuut_tijdlijn_naam)
    ctx.doc.objecttypen
  @ List.concat_map (fun (d, dt) -> dt_check ("/domeinen/" ^ d) dt) ctx.doc.domeinen
  @ List.concat_map
      (fun (fn, ft) ->
        List.concat_map
          (fun r ->
            match r.rol_objecttype_naam with
            | Some o when objecttype ctx o = None ->
                [ v "WF-003" ("/feittypen/" ^ fn)
                    ("role filler type '" ^ o ^ "' is not a declared object type") ]
            | _ -> [])
          ft.rollen)
      ctx.doc.feittypen

let wf004 ctx =
  List.concat_map
    (fun (otn, ot) ->
      List.concat_map
        (fun (a, dims) ->
          let at = "/objecttypen/" ^ otn ^ "/attribuutDimensies/" ^ a in
          (if dims = [] then [ v "WF-004" at "dimensioned attribute with an empty dimension list" ]
           else [])
          @ (dups_by norm (List.map (fun d -> (d, at)) (some_names dims))
             |> List.map (fun (x, _, p) -> v "WF-004" p ("duplicate dimension '" ^ x ^ "'")))
          @ List.concat_map
              (fun d ->
                match d with
                | Some d when find_ci ctx.doc.dimensies d = None ->
                    [ v "WF-004" at ("reference to undeclared dimension '" ^ d ^ "'") ]
                | Some _ -> []
                (* the loader reads every entry of a `dimensions` list as a
                   string, so a null entry cannot reach this walk since M1 *)
                | None -> assert false)
              dims)
        ot.attribuut_dimensies)
    ctx.doc.objecttypen

let wf005 ctx =
  List.concat_map
    (fun (otn, ot) ->
      let at = "/objecttypen/" ^ otn in
      let counts prefix l =
        List.concat_map
          (fun (a, n) ->
            match n with
            | Some n when n < 1 ->
                [ v "WF-005" (at ^ prefix ^ a)
                    (Printf.sprintf "timeline facet count %d < 1" n) ]
            | _ -> [])
          l
      in
      counts "/attribuutTijdlijnAantal/" ot.attribuut_tijdlijn_aantal
      @ counts "/kenmerkTijdlijnAantal/" ot.kenmerk_tijdlijn_aantal
      @ List.concat_map
          (fun (a, tln) ->
            match tln with
            | Some n -> (
                match find_tijdlijn ctx n with
                | None -> [] (* WF-003 reports the dangling reference *)
                | Some tl ->
                    let facet_gran = find_ci ot.attribuut_granulariteit a |> Option.join in
                    let facet_count =
                      match find_ci ot.attribuut_tijdlijn_aantal a |> Option.join with
                      | Some c -> c
                      (* a timeline name sits inside a TimelineFacet, whose count
                         is a required field since M1: v0's "absent = 1" has no
                         wire form left *)
                      | None -> assert false
                    in
                    (if facet_gran <> tl.tl_granulariteit then
                       [ v "WF-005" (at ^ "/attribuutTijdlijnNaam/" ^ a)
                           "named-timeline adoption is partial: granularity differs from the declaration" ]
                     else [])
                    @
                    if facet_count <> tl.tl_aantal then
                      [ v "WF-005" (at ^ "/attribuutTijdlijnNaam/" ^ a)
                          "named-timeline adoption is partial: period count differs from the declaration" ]
                    else [])
            | None -> [])
          ot.attribuut_tijdlijn_naam)
    ctx.doc.objecttypen
  (* a parameter's facet (WF-018 defers to this rule): the count clause; a
     parameter's facet names no timeline.

     Above 1 as well, while multi-period grids are OPEN (RUNTIME-SEMANTICS
     ch. 3.12): no executor evaluates a parameter's count — `ref` and the ASP
     backend take the granularity alone, the Drools loader refuses a count
     other than 1 (measured 2026-09-24) — so two executors treat a
     two-period facet as a one-period one and the third refuses it. *)
  @ List.concat_map
      (fun (p, (par : parameter)) ->
        match par.par_tijdlijn_aantal with
        | Some n when n < 1 ->
            [ v "WF-005" ("/parameters/" ^ p ^ "/timeline/count")
                (Printf.sprintf "the timeline facet of parameter '%s' has count %d < 1" p n) ]
        | Some n when n > 1 ->
            [ v "WF-005" ("/parameters/" ^ p ^ "/timeline/count")
                (Printf.sprintf
                   "the timeline facet of parameter '%s' has count %d > 1: a parameter's \
                    timeline is one period while multi-period grids are open \
                    (RUNTIME-SEMANTICS ch. 3.12)"
                   p n) ]
        | _ -> [])
      ctx.doc.parameters

let wf006 ctx =
  List.concat_map
    (fun (d, dt) ->
      match dt with
      | Some (Enumeratie { waarden; _ }) ->
          let at = "/domeinen/" ^ d in
          (if waarden = [] then [ v "WF-006" at "enumeration domain declares no values" ] else [])
          @ (if List.exists (fun x -> x = None) waarden then
               [ v "WF-006" at "enumeration domain contains a null value" ]
             else [])
          @ (dups_by norm (List.map (fun x -> (x, at)) (some_names waarden))
             |> List.map (fun (x, _, p) -> v "WF-006" p ("duplicate enumeration value '" ^ x ^ "'")))
      | _ -> [])
    ctx.doc.domeinen

(* WF-007 needs every Numeriek/Percentage in the document; collected by the
   datatype walk below (model layer) and the expression walk (types). *)
let check_number_form at = function
  | Numeriek { geheel; decimalen; _ } | Percentage { geheel; decimalen; _ } ->
      if decimalen < 0 then
        [ v "WF-007" at (Printf.sprintf "decimals %d < 0" decimalen) ]
      else if geheel && decimalen >= 1 then
        [ v "WF-007" at
            (Printf.sprintf "whole number with %d decimals: not one of the three precision forms"
               decimalen) ]
      else []
  | _ -> []

(* the declared unit systems, by name — the table every unit's `system`
   back-reference (in memory [stelsel]) resolves against (WF-034,
   ch. 4.6 (r)). *)
let stelsel_namen ctx = List.map fst ctx.doc.eenheidstelsels

(* A factor as a message writes it: a whole number bare, any other as
   num/den, the two integers exactly as the wire carries them. *)
let rationaal_render { num; den } =
  if den = 1 then string_of_int num else Printf.sprintf "%d/%d" num den

let rec ggd a b = if b = 0 then a else ggd b (a mod b)

(* WF-092 (IR-SPEC ch. 2.5): the conversion pair of a unit is ONE fact in
   two fields — the canonical unit it converts into and the factor that
   does the converting. Present together or absent together; either half
   alone is not a unit. The bare form (both absent) is the legitimate
   self-standing spelling every WF-038 duration parameter and WF-035
   duration operand carries. Arm (c) holds of the factor alone: it is an
   exact rational, and written in lowest terms, so one factor has one
   spelling — WF-091 compares it byte for byte, and two spellings of one
   value would be a lie it cannot tell from a real difference. That both
   integers lie between 1 and 2^62 - 1 is the loader's (a factor is positive
   and bounded by its wire form).

   The rule exists because neither half is reachable by a neighbour.
   WF-009 walks the declared systems but only ever follows a canonical
   chain it can FOLLOW (until L3 it also tested a factor it FOUND for
   positivity, since then the factor's wire form); a missing half is simply
   nothing to look at. WF-035/WF-023 cross units for
   COMPATIBILITY through [units_convertible], which reads a missing
   [canoniek] as the abbreviation and a missing factor not at all — so a
   half-filled unit is compatible with its whole sibling by construction.
   WF-091 compares two units BYTE for byte, which passes the moment the
   half-form is copied consistently across the arms it crosses.

   This function also carries WF-034's arm on the unit's `system`
   (maintainer decision 2026-09-05, sixth wave; IR-SPEC ch. 4.6 (r)), for the
   same reason WF-092 lives here: [check_eenheid_form] is the ONE walk that
   reaches every unit record in a document, denominators included, and the
   `system` is a reference like any other — it names the declared unit system
   whose conversion table gives the unit its meaning. [stelsels] is the
   declared-system name list, threaded from the context; it is a required
   argument rather than an optional one so a new call site cannot silently opt
   out of the cross. *)
let rec check_eenheid_form ~stelsels at (u : eenheid) =
  let named =
    match u.afkorting with Some a -> "'" ^ a ^ "'" | None -> "an unnamed unit"
  in
  (match u.stelsel with
   | Some s when not (mem_ci stelsels s) ->
       [ v "WF-034" (at ^ "/system")
           (Printf.sprintf
              "unit %s declares unit system '%s', which no declaration carries: the unit \
               resolves against no conversion table"
              named s) ]
   | _ -> [])
  @ (match (u.canoniek, u.factor_naar_canoniek) with
  | Some c, None ->
      [ v "WF-092" at
          (Printf.sprintf
             "unit %s names canonical unit '%s' but carries no conversion factor into it: a \
              half-filled conversion pair is not a unit (both fields, or neither)"
             named c) ]
  | None, Some f ->
      [ v "WF-092" at
          (Printf.sprintf
             "unit %s carries conversion factor %s but names no canonical unit to convert into: a \
              half-filled conversion pair is not a unit (both fields, or neither)"
             named (rationaal_render f)) ]
  | Some _, Some _ | None, None -> [])
  @ (match u.factor_naar_canoniek with
    | Some { num; den } when ggd num den <> 1 ->
        let g = ggd num den in
        [ v "WF-092" (at ^ "/factorToCanonical")
            (Printf.sprintf
               "unit %s carries conversion factor %d/%d, which is not in lowest terms: an exact \
                factor has one spelling, here %s"
               named num den (rationaal_render { num = num / g; den = den / g })) ]
    | _ -> [])
  @ (match u.noemer with Some n -> check_eenheid_form ~stelsels (at ^ "/denominator") n | None -> [])

(* every unit a datatype carries, denominators and composite element/field/
   payload datatypes included — WF-092 reaches strictly further into a
   datatype than [check_number_form] does, so the form law holds at unit
   positions WF-007's row records as out of its own reach. *)
let rec check_datatype_units ~stelsels at = function
  | Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ } ->
      check_eenheid_form ~stelsels (at ^ "/unit") u
  | List_type { element } -> check_datatype_units ~stelsels (at ^ "/element") element
  | Struct_type { velden; _ } ->
      List.concat
        (List.mapi
           (fun i (_, d) -> check_datatype_units ~stelsels (Printf.sprintf "%s/fields/%d/datatype" at i) d)
           velden)
  | Sum_type { constructoren; _ } ->
      List.concat
        (List.mapi
           (fun i (_, p) ->
             match p with
             | Some d ->
                 check_datatype_units ~stelsels (Printf.sprintf "%s/constructors/%d/payload" at i) d
             | None -> [])
           constructoren)
  | Numeriek _ | Percentage _ | Tekst | Booleaan | Datum_in_dagen | Datum_tijd | Enumeratie _ ->
      []

(* WF-092's model half: the declared unit systems (the source of truth for
   every conversion) plus every model-layer datatype. The expression half
   rides walk_expr's type prologue, where WF-007 collects the same "every
   datatype in the document" split. *)
let wf092 ctx =
  let stelsels = stelsel_namen ctx in
  let site at = function Some d -> check_datatype_units ~stelsels at d | None -> [] in
  List.concat_map
    (fun (sn, es) ->
      List.concat
        (List.mapi
           (fun j (d : gedeclareerde_eenheid) ->
             check_eenheid_form ~stelsels
               (Printf.sprintf "/eenheidstelsels/%s/units/%d" sn j)
               d.ge_eenheid)
           es.eenheden))
    ctx.doc.eenheidstelsels
  @ List.concat_map
      (fun (otn, ot) ->
        List.concat_map
          (fun (a, dt) -> site ("/objecttypen/" ^ otn ^ "/attributen/" ^ a) dt)
          ot.attributen)
      ctx.doc.objecttypen
  @ List.concat_map (fun (d, dt) -> site ("/domeinen/" ^ d) dt) ctx.doc.domeinen
  @ List.concat_map
      (fun (p, par) -> site ("/parameters/" ^ p ^ "/datatype") par.par_datatype)
      ctx.doc.parameters
  @ List.concat
      (List.mapi
         (fun i (fd : function_def) ->
           let at = Printf.sprintf "/functionDefs/%d" i in
           List.concat
             (List.mapi
                (fun j (_, dt) ->
                  check_datatype_units ~stelsels (Printf.sprintf "%s/parameters/%d/datatype" at j) dt)
                fd.fd_parameters)
           @ check_datatype_units ~stelsels (at ^ "/result") fd.fd_result)
         ctx.doc.function_defs)

let wf007_model ctx =
  List.concat_map
    (fun (otn, ot) ->
      List.concat_map
        (fun (a, dt) ->
          match dt with
          | Some d -> check_number_form ("/objecttypen/" ^ otn ^ "/attributen/" ^ a) d
          | None -> [])
        ot.attributen)
    ctx.doc.objecttypen
  @ List.concat_map
      (fun (d, dt) ->
        match dt with Some x -> check_number_form ("/domeinen/" ^ d) x | None -> [])
      ctx.doc.domeinen
  @ List.concat_map
      (fun (p, par) ->
        match par.par_datatype with
        | Some x -> check_number_form ("/parameters/" ^ p) x
        | None -> [])
      ctx.doc.parameters

(* WF-007's presentation arm (IR-SPEC ch. 2.4): the tag "percentage" belongs
   to a DECLARED datatype and never to a type annotation. An annotation is a
   copy the readers compare with the declaration, and a tag there would be a
   second presentation of one percentage, which the declaration alone decides.
   The loader collects the positions (Loader.presentatie_annotaties), because
   the in-memory datatype does not carry the tag. *)
let wf007_presentatie ctx =
  List.map
    (fun at ->
      v "WF-007" at
        "the presentation tag \"percentage\" on a type annotation: the tag is written only \
         where a datatype is declared")
    ctx.doc.presentatie_annotaties

(* WF-009: the canonical chain of every declared unit terminates inside its
   own system. A canonical name resolves against the system's units by
   abbreviation, under the WF-001 identity. That a factor is positive is its
   wire form since L3 (two integers of at least 1, refused at load
   otherwise), so no arm here reads the factor.

   The same identity makes an abbreviation name ONE unit of its system
   (IR-SPEC ch. 2.5: one Unit per abbreviation). The v0 map key made a second
   declaration unwritable; the L3 list can write it, and then the readers
   part: this verifier, `ref` and the ASP backend passed a second `t` with
   another factor while the Drools loader refused the document (measured
   2026-09-24). So the second declaration is refused here, at its own
   position, in the sentence the Drools loader writes too. *)
let wf009 ctx =
  List.concat_map
    (fun (sn, es) ->
      let twice =
        dups_by norm
          (List.concat
             (List.mapi
                (fun j (d : gedeclareerde_eenheid) ->
                  match d.ge_eenheid.afkorting with
                  | Some a -> [ (a, Printf.sprintf "/eenheidstelsels/%s/units/%d/abbreviation" sn j) ]
                  | None -> [])
                es.eenheden))
        |> List.map (fun (_, a, at) ->
               v "WF-009" at
                 (Printf.sprintf
                    "the unit '%s' is declared twice in the unit system '%s': one Unit per \
                     abbreviation (WF-009)"
                    a sn))
      in
      let lookup c =
        List.find_map
          (fun (d : gedeclareerde_eenheid) ->
            match d.ge_eenheid.afkorting with
            | Some a when norm a = norm c -> Some d.ge_eenheid
            | _ -> None)
          es.eenheden
      in
      List.concat
        (List.mapi
           (fun j (d : gedeclareerde_eenheid) ->
             let at = Printf.sprintf "/eenheidstelsels/%s/units/%d" sn j in
             let rec follow seen u =
               match u.canoniek with
               | None -> []
               | Some c when u.afkorting = Some c -> [] (* self-canonical *)
               | Some c ->
                   if List.mem (norm c) seen then
                     [ v "WF-009" at ("conversion cycle through '" ^ c ^ "'") ]
                   else (
                     match lookup c with
                     | None -> [ v "WF-009" at ("conversion targets unknown unit '" ^ c ^ "'") ]
                     | Some next -> follow (norm c :: seen) next)
             in
             follow (Option.to_list (Option.map norm d.ge_eenheid.afkorting)) d.ge_eenheid)
           es.eenheden)
      @ twice)
    ctx.doc.eenheidstelsels

let wf011 ctx =
  List.concat_map
    (fun (fn, ft) ->
      let n = List.length ft.rollen in
      let at = "/feittypen/" ^ fn in
      if ft.wederkerig && n <> 1 then
        [ v "WF-011" at (Printf.sprintf "reciprocal fact type with %d roles (must be 1)" n) ]
      else if (not ft.wederkerig) && n < 2 then
        [ v "WF-011" at (Printf.sprintf "fact type with %d roles (must be >= 2)" n) ]
      else [])
    ctx.doc.feittypen

let wf012 ctx =
  List.concat_map
    (fun (fn, ft) ->
      let at = "/feittypen/" ^ fn in
      let names =
        List.concat_map
          (fun r ->
            (match r.rol_naam with Some n -> [ (n, at) ] | None -> [])
            @ (match r.rol_meervoud with Some n -> [ (n, at) ] | None -> []))
          ft.rollen
      in
      (* collisions across *distinct* roles: dedupe within one role first *)
      let per_role =
        List.map
          (fun r ->
            List.sort_uniq compare
              (List.filter_map (fun x -> Option.map norm x) [ r.rol_naam; r.rol_meervoud ]))
          ft.rollen
      in
      let all = List.concat per_role in
      let colliding =
        List.filter (fun n -> List.length (List.filter (( = ) n) all) > 1)
          (List.sort_uniq compare all)
      in
      ignore names;
      List.map
        (fun n -> v "WF-012" at ("role name or plural '" ^ n ^ "' is ambiguous within the fact type"))
        colliding)
    ctx.doc.feittypen

let wf013 ctx =
  List.concat_map
    (fun (fn, ft) ->
      match ft.rollen with
      | [ r ] when ft.wederkerig && r.rol_meervoudig ->
          [ v "WF-013" ("/feittypen/" ^ fn) "reciprocal fact type's role must be to-one" ]
      | _ -> [])
    ctx.doc.feittypen

let wf014 ctx =
  List.concat_map
    (fun (dn, d) ->
      let at = "/dimensies/" ^ dn in
      (if d.dim_waarden = [] then [ v "WF-014" at "dimension with an empty label set" ] else [])
      @ (if List.exists (fun x -> x = None) d.dim_waarden then
           [ v "WF-014" at "dimension contains a null label" ]
         else [])
      @ (dups_by norm (List.map (fun x -> (x, at)) (some_names d.dim_waarden))
         |> List.map (fun (x, _, p) -> v "WF-014" p ("duplicate label '" ^ x ^ "'"))))
    ctx.doc.dimensies

let wf015 ctx =
  dups_by tl_norm
    (List.filter_map
       (fun tl -> Option.map (fun n -> (n, "/tijdlijnen")) tl.tl_naam)
       ctx.doc.tijdlijnen)
  |> List.map (fun (a, b, at) ->
      v "WF-015" at
        (Printf.sprintf "timelines '%s' and '%s' collide under normalized-name identity" a b))

let wf016 ctx =
  List.concat_map
    (fun tl ->
      match tl.tl_naam with
      | Some n when tl.variabel_startpunt ->
          if
            List.exists
              (fun sp ->
                match sp.sp_tijdlijn with
                | Some t -> tl_norm t = tl_norm n
                | None -> false)
              ctx.doc.resolved_startpunten
          then []
          else
            [ v "WF-016" "/tijdlijnen"
                ("variable-start timeline '" ^ n ^ "' has no start-point determination") ]
      | _ -> [])
    ctx.doc.tijdlijnen

(* A spelling the NORMATIVE calendar table carries (RUNTIME-SEMANTICS ch. 3.8).
   The comparison goes through `time_unit_of_spelling` — the same table WF-035
   and WF-038 route duration arithmetic on — rather than through a list of its
   own, so the rule that forbids a day-kind spelling and the semantics that
   would read it can never drift apart. Composed with `norm` (collapse
   whitespace, lower case), which is the ch. 2.1 identity every one of WF-017's
   three positions compares under. *)
let calendar_spelling s = time_unit_of_spelling (norm s) <> None

(* WF-017 over the whole SPELLING SPACE of a day kind (IR-SPEC ch. 2.9). The
   rule is one promoted MUST with two conjuncts — a day-kind spelling collides
   with no built-in calendar unit and with no other day kind's spelling — and
   a day kind has exactly two spellings on the wire, its `name` and its
   `plural`, so the rule reads both. (Until L3 the further spellings were keys
   of a separate registry, which could say only which day kind a spelling
   belonged to; the history is in ch. 2.9.)

   CALENDAR — the name (clause 1) and the plural (arm (a)). A spelling the
   calendar table carries makes the document contradict itself:
   `RUNTIME-SEMANTICS.md` ch. 3.8 recognises a duration's unit by spelling
   through that table first, and "a spelling outside the table is a declared
   day kind". Every executor reads that order, so no consumer acts on the
   contradiction, which does not make the document admissible. The
   comparison is [calendar_spelling], the table lookup itself, over all
   thirty spellings — abbreviations and the second plurals `secondes` and
   `millisecondes` included — so the rule and the arithmetic it fences cannot
   come to know different tables.

   ANOTHER DAY KIND — arm (b). A plural that is another day kind's name or
   plural makes one spelling name two day kinds, and a consumer resolving the
   spelling has no second reading to fall back on. Two NAMES that collide are
   WF-001's (the dayKinds namespace), not this rule's. A plural equal to its
   own day kind's name collides with nothing. Each colliding pair of plurals
   is reported once, at the later day kind.

   IDENTITY. Every comparison is under [norm] — collapse whitespace, lower
   case — the ch. 2.1 identity a consumer applies when it resolves a
   spelling, so the three positions agree on what "the same spelling" means.
   A byte-exact comparison would under-report, and the witness
   `corpus/negatief/wf017-meervoud-botst-met-vreemd-meervoud` pins it: its two
   plurals differ in case alone. *)
let wf017 ctx =
  let kinds = List.mapi (fun i (d : dagsoort) -> (i, d)) ctx.doc.dagsoorten in
  let at i field = Printf.sprintf "/dagsoorten/%d/%s" i field in
  List.concat_map
    (fun (i, (d : dagsoort)) ->
      (if calendar_spelling d.dk_naam then
         [ v "WF-017" (at i "name") ("day-kind '" ^ d.dk_naam ^ "' shadows a built-in calendar unit") ]
       else [])
      @
      match d.dk_meervoud with
      | Some p when calendar_spelling p ->
          [ v "WF-017" (at i "plural")
              (Printf.sprintf "the plural '%s' of day kind '%s' shadows a built-in calendar unit"
                 p d.dk_naam) ]
      | _ -> [])
    kinds
  @ List.concat_map
      (fun (i, (d : dagsoort)) ->
        match d.dk_meervoud with
        | None -> []
        | Some p ->
            List.filter_map
              (fun (j, (e : dagsoort)) ->
                if j = i then None
                else if norm p = norm e.dk_naam then
                  Some
                    (v "WF-017" (at i "plural")
                       (Printf.sprintf
                          "the plural '%s' of day kind '%s' is the name of day kind '%s': one \
                           spelling names two day kinds"
                          p d.dk_naam e.dk_naam))
                else
                  match e.dk_meervoud with
                  | Some q when j < i && norm p = norm q ->
                      Some
                        (v "WF-017" (at i "plural")
                           (Printf.sprintf
                              "the plural '%s' of day kind '%s' is the plural '%s' of day kind \
                               '%s': one spelling names two day kinds"
                              p d.dk_naam q e.dk_naam))
                  | _ -> None)
              kinds)
      kinds

(* WF-018: a parameter's datatype resolves, and its timeline facet, when
   present, satisfies WF-005 — the facet's own rule, so that clause reports
   under WF-005 (the parameter half of [wf005]). A parameter without a
   datatype cannot be written since L3: the wire requires one and the loader
   reads it as required. *)
let wf018 ctx =
  List.concat_map
    (fun (p, par) ->
      let at = "/parameters/" ^ p in
      match par.par_datatype with
      | Some (Enumeratie { domein = Some d; _ }) when find_ci ctx.doc.domeinen d = None ->
          [ v "WF-018" at ("parameter enumeration references undeclared domain '" ^ d ^ "'") ]
      | Some _ -> []
      | None -> assert false (* the loader reads the datatype as required *))
    ctx.doc.parameters

(* ======================================================================= *)
(* Rule layer: WF-020 .. WF-033                                            *)
(* ======================================================================= *)

(* WF-107: every named form carries a non-blank name (IR-SPEC ch. 3.1). The
   name IS the form's identity on this wire: ch. 8 groups versions by it,
   the ch. 5.1 dependency graph keys a channel node on it, a regelversie
   condition names a rule by it, and WF-020 keeps it unique. Without one,
   every reader invents an identity of its own — measured 2026-09-12, a
   nameless rule passed here and crashed one Drools branch's name escaper.
   Blank counts as absent because WF-020 compares over collapsed
   whitespace, so two blank names are one name. *)
let wf107 ctx =
  List.concat_map
    (fun (arr, entries) ->
      let why =
        " (a named form's identity on this wire IS its name — ch. 8 groups \
         versions by it, the ch. 5.1 dependency graph keys its channel node on \
         it, and WF-020 keeps it unique)"
      in
      (* a form's name sits on the form, a defaults group's on its node *)
      let name_at at = at ^ "/name" in
      List.filter_map
        (fun (n, at, _) ->
          match n with
          | Some s when collapse_ws s <> "" -> None
          | Some _ ->
              Some (v "WF-107" (name_at at) ("form in " ^ arr ^ " carries a blank name" ^ why))
          | None ->
              Some (v "WF-107" (name_at at) ("form in " ^ arr ^ " carries no name" ^ why)))
        (forms_of entries))
    (named_forms ctx)

(* Over the FORMS ([forms_of]): a form's versions share its one name, so
   they never collide with each other, and two forms of one name collide
   whatever number of versions each carries. *)
let wf020 ctx =
  let arrays = List.map (fun (arr, entries) -> (arr, forms_of entries)) (named_forms ctx) in
  (* A collision across two arrays is reported once, at the form of the
     array that comes first in this order, naming the other. The order is
     the one the reports followed before the lists took their wire names
     (landing L5), so the renaming moved no report. *)
  let report_order =
    [ "consistencies"; "factCreations"; "objectCreations"; "prioritizedDefaults"; "rules";
      "uniquenesses"; "allocations" ]
  in
  let rank arr =
    let rec go i = function
      | [] -> invalid_arg ("WF-020: no report order for the array " ^ arr)
      | x :: rest -> if x = arr then i else go (i + 1) rest
    in
    go 0 report_order
  in
  (* cross-array: a name in more than one form array *)
  let stripped =
    List.concat_map
      (fun (arr, forms) ->
        List.filter_map (fun (n, at, _) -> Option.map (fun n -> (norm n, arr, n, at)) n) forms)
      arrays
  in
  let cross =
    List.filter_map
      (fun (k, arr, n, at) ->
        match
          List.find_opt (fun (k', arr', _, _) -> k = k' && arr <> arr') stripped
        with
        | Some (_, arr', n', _) when rank arr < rank arr' ->
            Some (v "WF-020" at
                    (Printf.sprintf
                       "name '%s' collides with '%s' in %s (unique across all named forms)"
                       n n' arr'))
        | _ -> None)
      stripped
  in
  (* within one array: two forms of one name. A form with one version and
     a form with several are two forms as well — the wire cannot spell them
     as one, since a form's name never carries a version suffix (the loader
     refuses it) *)
  let within =
    List.concat_map
      (fun (arr, forms) ->
        List.filter_map (fun (n, at, _) -> Option.map (fun n -> (n, at)) n) forms
        |> dups_by norm
        |> List.map (fun (a, _, at) -> v "WF-020" at (Printf.sprintf "duplicate name '%s' in %s" a arr)))
      arrays
  in
  cross @ within

(* WF-101: a validity window's bounds carry WF-084's date lexeme (IR-SPEC
   ch. 4.1). Every `from`/`to` written on every Validity — the nine
   carriers below — is the strict ISO form yyyy-mm-dd AND a real calendar
   day, checked by the very predicate WF-084 uses on a date literal.

   It exists to make the assumption stated at validity_overlaps above TRUE
   before that function relies on it: this verifier holds a bound as an
   OPAQUE STRING and orders it with `<=`, and lexicographic order agrees
   with calendar order only INSIDE this lexeme. Ref and the ASP backend did
   the same until 2026-09-23 and now read the bounds through the strict
   date reader, refusing a non-date by position. Measured 2026-09-11 on jutterswet-vensterheffing with one bound
   twisted, no reader faulting anywhere: `+012026-01-01` and `-0500-01-01`
   sort before every four-digit year, so the window silently opens at the
   start and both executors applied a rule version outside it (125 where the
   pinned answer is leeg); `niet-een-datum` sorts after every date, closing
   the window over its own casus (leeg where the pinned answer is 125).
   Every one of those is a silent wrong answer, not a fault.

   The calendar half is the layer the schema pattern on these two fields
   cannot reach: `2026-02-30` matches that pattern, orders correctly, and
   names a day that does not exist — a reader that PARSES the bound refuses
   it, as the executors now do, where an opaque-string reader does not. *)
let wf101 ctx =
  let bounds at (g : geldigheid option) =
    match g with
    | None -> []
    | Some g ->
        List.filter_map
          (fun (field, w) ->
            match w with
            | Some w when not (lexeme_iso_date w) ->
                Some
                  (v "WF-101"
                     (at ^ "/validity/" ^ field)
                     ("validity bound '" ^ w
                      ^ "' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges \
                         — ch. 4.1)"))
            | _ -> None)
          [ ("from", g.geld_vanaf); ("to", g.geld_tot) ]
  in
  List.concat_map (fun (_, entries) -> List.concat_map (fun (_, g, at) -> bounds at g) entries)
    (named_forms ctx)
  (* The one carrier named_forms does not list. A day-kind window is refused
     outright by WF-031 (it must be always, a Validity with neither bound), so
     on a conforming document this arm is unreachable; it is here so the rule
     holds of every Validity on the wire rather than of eight of nine. *)
  @ List.concat
      (List.mapi
         (fun i (r : resolved_dagsoort) ->
           bounds (Printf.sprintf "/dayKindDefinitions/%d" i) r.rd_geldigheid)
         ctx.doc.resolved_dagsoorten)

(* The versions of one form are the records that share its form pointer
   ([forms_of]); two forms of one name are WF-020's, not a pair of versions. *)
let wf021 ctx =
  List.concat_map
    (fun (arr, entries) ->
      List.concat_map
        (fun (_, _, members) ->
          let label n at = match n with Some n -> n | None -> at in
          let rec pairs = function
            | [] -> []
            | (n1, g1, at1) :: rest ->
                List.filter_map
                  (fun (n2, g2, at2) ->
                    if validity_overlaps g1 g2 then
                      Some (v "WF-021" at1
                              (Printf.sprintf
                                 "validity windows of versions '%s' and '%s' overlap (%s)"
                                 (label n1 at1) (label n2 at2) arr))
                    else None)
                  rest
                @ pairs rest
          in
          pairs members)
        (forms_of entries))
    (form_arrays ctx)

(* WF-022 (IR-SPEC ch. 3.1). Which member a version writes decides its kind
   since L4 (delta B20): the version's own fields say which kind of member it
   writes — a presentation tag or initialisation true computes an attribute,
   a version with neither attributes a characteristic ([Ast], the loader's
   reading) — and the target's declaration says which member it names. The
   rule crosses the two, and a name that is an attribute AND a characteristic
   of the subject type decides nothing, so it is refused before either kind
   is read into it. *)
let wf022 ctx =
  List.concat_map
    (fun (r : resolved_regel) ->
      let at = r.rr_pointer in
      match r.doel with
      | Some (Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ }) -> (
          match objecttype ctx otn with
          | None -> [ v "WF-022" (at ^ "/target") ("target object type '" ^ otn ^ "' is not declared") ]
          | Some ot -> (
              let computes = r.rr_soort <> Some Kenmerktoekenning in
              match (has_attr ot a, has_kenmerk ot a) with
              | true, true ->
                  [ v "WF-022" (at ^ "/target")
                      ("target '" ^ a ^ "' names both an attribute and a characteristic of '" ^ otn
                     ^ "': which member a rule writes decides its kind, and this name decides \
                        nothing") ]
              | false, true when computes ->
                  [ v "WF-022" (at ^ "/target")
                      ("a version with a presentation or with initialisation true writes an \
                        attribute, so its target MUST name one, and '" ^ a
                     ^ "' is a characteristic of '" ^ otn ^ "'") ]
              | false, true -> (
                  (* characteristic target: value must be the constant true *)
                  match r.waarde with
                  | Some (Literal { variant = Lv_boolean true; typ = Some Booleaan; _ }) -> []
                  | _ ->
                      [ v "WF-022" (at ^ "/value")
                          "characteristic target's value must be the boolean constant true" ])
              | true, false when not computes ->
                  [ v "WF-022" (at ^ "/target")
                      ("a version without a presentation and without initialisation attributes a \
                        characteristic, so its target MUST name one, and '" ^ a
                     ^ "' is an attribute of '" ^ otn ^ "'") ]
              | true, false -> []
              | false, false ->
                  [ v "WF-022" (at ^ "/target")
                      ("target '" ^ a ^ "' is neither an attribute nor a characteristic of '" ^ otn ^ "'") ]))
      | Some _ -> [ v "WF-022" (at ^ "/target") "rule target is not an attribute reference" ]
      | None -> [ v "WF-022" (at ^ "/target") "rule without a target" ])
    ctx.doc.resolved_regels

let wf025 ctx =
  List.concat_map
    (fun (r : resolved_verdeling) ->
      let at = r.vd_pointer in
      (match r.vd_methode, r.rato_attribuut with
       | Some Gelijke_delen, Some _ ->
           [ v "WF-025" at "equal-shares allocation with a ratio attribute" ]
       | Some Naar_rato, None ->
           [ v "WF-025" at "proportional allocation without a ratio attribute" ]
       | _ -> [])
      @ (match r.maximum_attribuut, r.rato_attribuut with
         | Some _, None -> [ v "WF-025" at "maximum requires a ratio attribute" ]
         | _ -> [])
      @ (match r.groepeer_attribuut, r.vd_methode with
         | Some _, Some m when m <> Op_volgorde ->
             [ v "WF-025" at "ordering key on a non-ordered allocation" ]
         | _ -> [])
      @
      match r.groepeer_attribuut, r.doel_type with
      | Some key, Some dt -> (
          match objecttype ctx dt with
          | Some ot -> (
              match attr_of ot key with
              | Some (Some d) when not (is_orderable d) ->
                  [ v "WF-025" at ("ordering key '" ^ key ^ "' has a non-orderable type") ]
              | _ -> [])
          | None -> [])
      | _ -> [])
    ctx.doc.verdelingen

(* The single-writer fences (WF-026 here, WF-050 and WF-079 below) key on
   the DECLARED object-type name, and under subtyping they MUST keep doing
   so — this is a constraint on the verifier, not an oversight (IR-SPEC
   ch. 6.1, RUNTIME-SEMANTICS ch. 7). They are read on the per-type
   EFFECTIVE stack, after inheritance resolution, never on a chain-closed
   target key. The hazard runs the counter-intuitive way: the declared-type
   keys do not see a supertype/subtype pair at all, because 'Strandrecht.x'
   and 'Wrakrecht.x' are two keys. Lifting the target key to the
   chain-closed set — the obvious repair once member resolution walks the
   chain — would make them fire on exactly the shape inheritance
   legitimises, a supertype base plus a subtype base on one target, and
   make the construct unreachable. In the effective stack that pair is ONE
   writer: the subtype's own base replaces the inherited one. Any
   chain-awareness added here must ask "do these two writers survive into
   one instance's effective stack", never "do these two targets close over
   the same member". WF-079's chain arm asks exactly that of two bare
   computing rules: outside the gate of RUNTIME-SEMANTICS ch. 7.10 both
   survive onto a subtype instance, inside it the fold makes them one. *)
let wf026 ctx =
  let named =
    List.map
      (fun r -> (Option.map strip_version r.vd_naam, r.doel_type, r.doel_attribuut, r.vd_pointer))
      ctx.doc.verdelingen
  in
  let rec pairs = function
    | [] -> []
    | (n1, dt1, da1, at1) :: rest ->
        List.filter_map
          (fun (n2, dt2, da2, _) ->
            let same_target =
              match dt1, dt2, da1, da2 with
              | Some a, Some b, Some c, Some d -> norm a = norm b && norm c = norm d
              | _ -> false
            in
            let distinct_form =
              match n1, n2 with Some a, Some b -> norm a <> norm b | _ -> true
            in
            if same_target && distinct_form then
              Some (v "WF-026" at1
                      (Printf.sprintf "two allocations write target (%s, %s)"
                         (Option.value dt1 ~default:"?") (Option.value da1 ~default:"?")))
            else None)
          rest
        @ pairs rest
  in
  pairs named

(* WF-034 on the allocation's two ENDPOINT attributes (maintainer decision
   2026-09-04, third wave). An allocation is not an expression, so the
   expression walk never reaches it; its source and target attributes are
   references all the same, and each is spelled in one field. Every rule that
   reads them — WF-025's ordering-key type check, WF-026's same-target
   comparison, WF-050's defaults overlap, and the dependency graph's read and
   write edges — sits behind `Some a` and falls silent together on an empty
   one. Measured: all 11 golden allocations stay verifier-clean with either
   field emptied, while both executors fault loudly on the same document
   (`ref` "missing bronAttribuut"/"missing doelAttribuut", clingo the same two
   names).

   What this arm does NOT close, measured the same day and stated so the
   silence is not mistaken for coverage: a NAME that resolves against no
   declaration (`sourceAttribute: "bestaatniet"` was verifier-clean while `ref`
   faulted and clingo reported `unknown attribute`), and the two TYPE fields
   `sourceType`/`targetType`, equally emptyable and equally clean. Both are name
   resolution against the object-type table, a wider movement than this arm's
   empty-field class — and both are CLOSED by the fourth wave seventy lines
   below, [wf034_verdeling_typen] and [wf034_verdeling_naamresolutie]
   (maintainer decision 2026-09-04, IR-SPEC ch. 4.6 (j)–(k)). What stays this
   arm's own is the NULL half: an absent field names nothing to resolve, so
   there is no scope question to ask and no message to phrase in terms of a
   declaration table. *)
let wf034_verdelingen ctx =
  List.concat_map
    (fun (r : resolved_verdeling) ->
      let at = r.vd_pointer in
      (match r.bron_attribuut with
       | None ->
           [ v "WF-034" (at ^ "/sourceAttribute")
               "allocation without a source attribute: it resolves against no declaration" ]
       | Some _ -> [])
      @
      match r.doel_attribuut with
      | None ->
          [ v "WF-034" (at ^ "/targetAttribute")
              "allocation without a target attribute: it resolves against no declaration" ]
      | Some _ -> [])
    ctx.doc.verdelingen

(* WF-034 on the allocation's SIX attribute-name fields, the filled half of the
   same defect (maintainer decision 2026-09-04, third wave, fix round). The arm
   above reads `Some a` as "a name is there"; an empty string satisfies it and
   names nothing, so every rule behind `Some a` stays as silent as it is on
   null. This clause is the other half: a PRESENT attribute name is not empty.
   Absence keeps its meaning — absence of `ratioAttribute`, `maximumAttribute`,
   `remainderAttribute` and `orderingAttribute` is a facet switched off, and
   an absent `ratioAttribute` under proportional is already WF-025's business — but a
   field that is present must name something.

   Emptiness is decided by the verifier's own name identity (whitespace
   collapsed, lowercased, ch. 2.1): a blank spelling is the same empty name and
   resolves against the same nothing.

   Measured on the golden allocations, one field emptied to "" at a time, every
   one of them verifier-CLEAN before this clause and 0 after — and the executor
   consequence is worse than the null half's, because it is not uniformly loud:

     sourceAttribute   11/11  ref and clingo both fault (empty source amount /
                              unknown attribute "")
     targetAttribute   11/11  both fault (unknown attribute "")
     remainderAttribute 9/9   both fault (unknown attribute "")
     ratioAttribute     7/7   BOTH SILENT: no fault, a wrong value
                              (anchor jutterswet-bergingstoezicht:
                              bergingsaandeel underived, 60 pinned)
     maximumAttribute   4/4   ref SILENT (jutterswet-berging: 600 for 300),
                              clingo faults — ref != clingo on a document the
                              verifier called well-formed
     orderingAttribute  3/3   BOTH SILENT (jutterswet-rangaflopend: 300 for
                              1200; the ordering key vanishes and the whole
                              pot goes to one recipient)

   The three silent rows are where the differential square is blind: only the
   hand-pinned anchor expectations catch them, never backend agreement. *)
let wf034_verdeling_namen ctx =
  List.concat_map
    (fun (r : resolved_verdeling) ->
      let at = r.vd_pointer in
      let filled field = function
        | Some a when norm a = "" ->
            [ v "WF-034" (at ^ "/" ^ field)
                (field
               ^ " is present but empty: an attribute name that names nothing \
                  resolves against no declaration") ]
        | _ -> []
      in
      filled "sourceAttribute" r.bron_attribuut
      @ filled "targetAttribute" r.doel_attribuut
      @ filled "ratioAttribute" r.rato_attribuut
      @ filled "maximumAttribute" r.maximum_attribuut
      @ filled "remainderAttribute" r.restant_attribuut
      @ filled "orderingAttribute" r.groepeer_attribuut)
    ctx.doc.verdelingen

(* WF-034 on the allocation's two TYPE fields (maintainer decision 2026-09-04,
   fourth wave). The empty-reference class of the earlier waves, one field
   further along: an allocation whose `sourceType` or `targetType` is null names
   the object type it distributes FROM, or TO, nowhere. An UNDECLARED type is
   already caught — WF-047 crosses it against the fillers of the recipient
   role the allocation carries — but that reads `Some t` and a null slips
   past it together with everything else keyed on the type: WF-025's ordering-key datatype,
   WF-050's defaults overlap, the dependency graph's read and write edges, and
   this card's own name resolution below, which has no scope to resolve
   against without it.

   Measured: 11 of 11 golden allocations stay verifier-clean with either type
   emptied, while both executors fault loudly on the same document (`ref`
   "missing bronType in IR node" / "missing doelType in IR node", clingo
   "missing bronType" / "missing doelType"). The clause makes the verifier say
   before the run what both executors say during it.

   Two arms, two witnesses: `wf034-verdeling-zonder-brontype` and
   `wf034-verdeling-zonder-doeltype`. Each arm is exact — replacing one of the
   two matches by [] frees only its own document over the four zones (486
   compared; the type-less allocation negatiefs lose an incidental line and
   keep their verdict) — and dropping the whole clause frees those two and
   nothing else: 6 reports, 2 verdicts. *)
let wf034_verdeling_typen ctx =
  List.concat_map
    (fun (r : resolved_verdeling) ->
      let at = r.vd_pointer in
      (match r.bron_type with
       | None ->
           [ v "WF-034" (at ^ "/sourceType")
               "allocation without a source object type: it resolves against no declaration" ]
       | Some _ -> [])
      @
      match r.doel_type with
      | None ->
          [ v "WF-034" (at ^ "/targetType")
              "allocation without a target object type: it resolves against no declaration" ]
      | Some _ -> [])
    ctx.doc.verdelingen

(* WF-034 on the allocation's six attribute names, the RESOLUTION half
   (maintainer decision 2026-09-04, fourth wave). The two arms above ask
   whether a name is there and whether it is more than blanks; this one asks
   the question the rule is actually about — does the name resolve? Each field
   resolves against the declaration scope of ITS OWN SIDE, the sides both
   executors read it on:

     sourceType sourceAttribute   the pot that is distributed (read)
                remainderAttribute the remainder written back on the source
     targetType targetAttribute   the share written on each recipient
                ratioAttribute    the proportional weight (read per recipient)
                maximumAttribute  the cap (read per recipient)
                orderingAttribute the ordering key (read per recipient)

   An undeclared TYPE is another rule's business (WF-047, against the
   recipient role's fillers — measured loud, though the executors are not: an undeclared `sourceType` leaves `ref` silent with every
   share *leeg* where 300/200/300 and a 400 remainder are pinned, while clingo
   refuses to translate), and a null type is the sibling arm above; this one
   resolves only where there is a scope to resolve in. A declared KENMERK name
   lands here too and is reported as WF-034, where the sibling reference
   positions split the member kinds and report WF-080 — an allocation field
   asks only whether the name is a declared attribute. Measured harmless
   (5 of 5 refused on `jutterswet-berging` with a characteristic declared on
   either side) and deliberately worded so; IR-SPEC ch. 4.6 (j).

   Measured on the golden allocations, one field at a time set to a name no
   declaration carries: all 45 positions — the six rows tabulated below —
   stayed verifier-CLEAN before this clause, and 0 after. The executor consequence is
   the reason it is a clause and not a note, and it is not uniformly loud:

     sourceAttribute   11/11  both fault (empty source amount / unknown attribute)
     targetAttribute   11/11  both fault (unknown attribute)
     remainderAttribute 9/9   both fault (unknown attribute)
     ratioAttribute     7/7   BOTH SILENT: the shares stay underived
                              (anchor jutterswet-bergingstoezicht: leeg where
                              60 and 40 are pinned)
     maximumAttribute   4/4   ref SILENT (jutterswet-berging: 600 for a pinned
                              300, the cap simply gone), clingo refuses to
                              translate — ref != clingo on a document the
                              verifier called well-formed
     orderingAttribute  3/3   BOTH SILENT AND AGREED (jutterswet-rangaflopend:
                              300 apiece where 1200/0/0/0 is pinned)

   The three silent rows are where the differential square is blind: backend
   agreement proves nothing there, and only the hand-pinned anchor
   expectations stand between the corpus and the defect.

   Each of the six sub-arms carries its own negatief witness
   (`corpus/negatief/wf034-verdeling-onbekend-<field>`), and each is ARM-exact:
   deleting one [resolve] line frees that one document and no other over the
   four zones (486 documents compared; the `targetAttribute` line additionally
   costs the parity casus its six incidental lines, without moving its
   verdict). Deleting the whole clause frees those six and nothing else —
   7 reports, 6 verdicts. *)
let wf034_verdeling_naamresolutie ctx =
  List.concat
    (List.map
       (fun (r : resolved_verdeling) ->
         let at = r.vd_pointer in
         let resolve ~side ~scope field name =
           match (scope, name) with
           | Some tn, Some a when norm a <> "" -> (
               match objecttype ctx tn with
               | None -> [] (* the type itself is undeclared: WF-047 *)
               | Some ot ->
                   if has_attr ot a then []
                   else
                     [ v "WF-034" (at ^ "/" ^ field)
                         (Printf.sprintf
                            "'%s' is not a declared attribute of the %s type '%s': the \
                             allocation's %s resolves against no declaration"
                            a side tn field) ])
           | _ -> []
         in
         resolve ~side:"source" ~scope:r.bron_type "sourceAttribute" r.bron_attribuut
         @ resolve ~side:"source" ~scope:r.bron_type "remainderAttribute" r.restant_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "targetAttribute" r.doel_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "ratioAttribute" r.rato_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "maximumAttribute" r.maximum_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "orderingAttribute" r.groepeer_attribuut)
       ctx.doc.verdelingen)

(* WF-034 on an object creation's SUBJECT (maintainer decision 2026-09-04,
   fifth wave). `subject` names the object type the creation RUNS OVER (ch.
   5.5): one new instance per subject instance the condition admits, and the
   member values evaluate on it. It is a reference like any other, spelled in
   one field, and no rule crossed it against the object-type table. WF-027
   crosses the CREATED type (`createdType`) and nothing else; the subject was
   reached only by the rules that read THROUGH it — WF-053 when a member value
   navigates, WF-047 when an explicit `connectingRole` names the back-link —
   and those are incidental cover, not coverage: they stand down the moment a
   creation has neither.

   Measured over the 11 golden object creations, `subject` pointed at an
   undeclared name one at a time: 10 of 11 were already refused — all ten by
   WF-053 (the navigation in a member value loses its scope), nine of them by
   WF-047 as well — and exactly ONE stayed verifier-CLEAN: the single creation
   with no `initialisations` at all (`anchor/jutterswet-penninguitgifte`,
   creation 1). There the executors do not fault either; they AGREE on a wrong
   answer, `ref` "MISMATCH: J1.aantal dossiers = 0, expected 1" and clingo
   "MISMATCH: J1.aantal dossiers = [0], expected 1" — the creation simply runs
   over an empty population and no object is made. That is the row where the
   differential square is blind and only the hand-pinned anchor expectation
   stands between the corpus and the defect.

   TWO arms now, one per spelling. The second (maintainer decision 2026-09-05,
   sixth wave; IR-SPEC ch. 4.6 (n), its empty half) is `subject: null` — the
   empty-reference class of the earlier waves, which the fifth wave measured
   and left without a witness. It is a separate arm because it is a separate
   question: a name that resolves against nothing has a declaration table to
   be missing from, an ABSENT name has nothing to look up, and the two are
   observed differently — a wrong subject makes both OCaml executors agree on
   a wrong ANSWER, while an absent one makes both of them LOUD (`ref` "missing
   ObjectCreatie.subject in IR node", clingo "missing ObjectCreatie.subject").
   Carried by zero of the 30 object creations the four zones hold on
   2026-09-05; witnesses `wf034-objectcreatie-onbekend-subject` and
   `wf034-objectcreatie-loos-subject`. *)
let wf034_objectcreatie_subject ctx =
  List.concat
    (List.map
       (fun (r : resolved_object_creatie) ->
         let at = r.oc_pointer ^ "/subject" in
         match r.oc_subject with
         | Some s when objecttype ctx s = None ->
             [ v "WF-034" at
                 ("object creation's subject type '" ^ s
                ^ "' is not declared: the creation resolves against no declaration") ]
         | None ->
             [ v "WF-034" at
                 "object creation without a subject: the population it runs over, and the type \
                  its member values evaluate on, resolve against no declaration" ]
         | Some _ -> [])
       ctx.doc.object_creaties)

(* The annotation a node carries of its own. Since L2 the five reference
   classes carry none (B13): a reference's type is its declaration's, which
   [expr_type] below reads. A Lambda is never a value and an instance-set
   union denotes instances, so neither carries one either; an Unresolved
   without a type is a value of unknown type. *)
let annotation = function
  | Literal { typ; _ } | Tekst_reeks { typ; _ } | Binair { typ; _ }
  | Functie { typ; _ } | Afgerond { typ; _ } | Begrensd { typ; _ }
  | Constante { typ; _ } | Bound_day { typ; _ } | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ } | Construct { typ; _ } | Match { typ; _ }
  | Struct_literal { typ; _ } | Field_access { typ; _ }
  | List_literal { typ; _ } | If_then_else { typ; _ } | Let { typ; _ }
  | Invoke { typ; _ } | Fold { typ; _ } | Map_list { typ; _ }
  | Filter_list { typ; _ } | List_length { typ; _ } -> typ
  | Attribuut_ref _ | Dimensie_cel _ | Parameter_ref _ | Variabele_ref _ | Binder_ref _
  | Lambda _ | Instance_set_union _ ->
      None

(* The declared datatype of an attribute reference: the attribute on the
   type the reference names, which is the type the value lives on (the
   subject for its own read, the endpoint for a navigated one, the element
   for a fold's set source), resolved upward through the supertype chain.
   None where the pair does not resolve to a declared attribute — an
   undeclared type or member, a characteristic, a count source's role
   name — which the member checks report. *)
let resolved_attr_datatype ctx = function
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ } -> (
      match objecttype ctx otn with
      | Some ot -> ( match attr_of ot a with Some (Some dt) -> Some dt | _ -> None)
      | None -> None)
  | _ -> None

(* The static type of an expression (RUNTIME-SEMANTICS ch. 2.10): a
   reference takes its declaration's — an attribute's, a parameter's, the
   dimensioned attribute's for a cell read, the binder's for a binder read
   ([binders] carries the enclosing binders with their declared datatypes)
   — and every other value node its own annotation. None where nothing
   declares one: a reference that does not resolve (its own rule reports
   it), an unbound variable marker, an Unresolved of unknown type. *)
let expr_type ctx ~(binders : (string * datatype option) list) (e : expr) =
  match e with
  | Attribuut_ref _ -> resolved_attr_datatype ctx e
  | Dimensie_cel { attribuut = Some inner; _ } -> resolved_attr_datatype ctx inner
  | Dimensie_cel { attribuut = None; _ } -> None
  | Parameter_ref { parameter = Some p; _ } -> (
      match find_ci ctx.doc.parameters p with Some par -> par.par_datatype | None -> None)
  | Parameter_ref { parameter = None; _ } -> None
  | Binder_ref { naam; _ } -> (
      match List.find_opt (fun (b, _) -> norm b = norm naam) binders with
      | Some (_, dt) -> dt
      | None -> None)
  | Variabele_ref _ -> None
  | _ -> annotation e

let wf027 ctx =
  List.concat_map
    (fun (r : resolved_object_creatie) ->
      let at = r.oc_pointer in
      match r.nieuw_type with
      | None -> [ v "WF-027" at "object creation without a created type" ]
      | Some tn -> (
          match objecttype ctx tn with
          | None -> [ v "WF-027" at ("created type '" ^ tn ^ "' is not declared") ]
          | Some ot ->
              List.concat_map
                (fun (j, t) ->
                  let tat = Printf.sprintf "%s/initialisations/%d" at j in
                  match t.toe_attribuut with
                  | None -> [ v "WF-027" tat "member initialisation without a member name" ]
                  | Some m ->
                      if t.toe_kenmerk then
                        (if not (has_kenmerk ot m) then
                           [ v "WF-027" tat
                               ("'" ^ m ^ "' is not a declared characteristic of '" ^ tn ^ "'") ]
                         else [])
                        @
                        (* Phase 5c relaxation: a characteristic initialisation
                           value is a boolean-TYPED expression, not only the
                           boolean constant (which stays the v0 form). What the
                           verifier can statically type as boolean passes; a
                           statically non-boolean value (a numeric/text literal,
                           any evidently non-boolean node) stays refused. A value
                           whose type nothing declares (an Unresolved of
                           unknown type, a reference that does not resolve —
                           its own rule reports it) cannot be refuted
                           statically and defers to the runtime fail-fast at
                           the write. A node that is never a value (a Lambda)
                           stays refused. *)
                        (match t.toe_waarde with
                         | None ->
                             [ v "WF-027" tat "characteristic initialisation without a value" ]
                         | Some (Lambda _ | Instance_set_union _) ->
                             [ v "WF-027" tat
                                 "characteristic initialisation's value must be a \
                                  boolean-typed expression" ]
                         | Some w -> (
                             match expr_type ctx ~binders:[] w with
                             | Some Booleaan | None -> []
                             | Some _ ->
                                 [ v "WF-027" tat
                                     "characteristic initialisation's value must be a \
                                      boolean-typed expression" ]))
                      else if not (has_attr ot m) then
                        [ v "WF-027" tat ("'" ^ m ^ "' is not a declared attribute of '" ^ tn ^ "'") ]
                      else [])
                (List.mapi (fun j t -> (j, t)) r.toekenningen)))
    ctx.doc.object_creaties

let wf029 ctx =
  List.concat_map
    (fun (r : resolved_consistentie) ->
      let at = r.cons_pointer in
      (match r.cons_subject with
       | None -> [ v "WF-029" at "consistency without a subject type" ]
       | Some s when objecttype ctx s = None ->
           [ v "WF-029" at ("subject type '" ^ s ^ "' is not declared") ]
       | Some _ -> [])
      @ match r.assertie with
        | None -> [ v "WF-029" at "consistency without an assertion" ]
        | Some _ -> [])
    ctx.doc.consistenties

let datatype_kind = function
  | Numeriek _ -> "number" | Percentage _ -> "percentage" | Tekst -> "text"
  | Booleaan -> "boolean" | Datum_in_dagen -> "date-day" | Datum_tijd -> "date-ms"
  | Enumeratie _ -> "enumeration" | Sum_type _ -> "sumtype" | List_type _ -> "list"
  | Struct_type _ -> "struct"

(* like [datatype_kind] but carrying the decimal count and unit a numeric type
   turns on: a WF-076 mismatch on precision or unit renders as "number differs
   from number" under the bare kind, hiding exactly the discrepancy the check
   catches — so a numeric type also shows its decimals and unit. A keuzetype
   shows its domain for the same reason (a WF-064/069/070 mismatch between
   two enumeration domains rendered as "enumeration, but ... enumeration"),
   and a list shows its element. *)
let rec datatype_detail dt =
  let dec_unit decimalen eenheid =
    let u =
      match eenheid with
      | None -> ""
      | Some { afkorting = Some a; _ } -> ", " ^ a
      | Some { canoniek = Some c; _ } -> ", " ^ c
      | Some _ -> ", unit"
    in
    Printf.sprintf "(%d decimals%s)" decimalen u
  in
  match dt with
  | Numeriek { decimalen; eenheid; _ } -> "number" ^ dec_unit decimalen eenheid
  | Percentage { decimalen; eenheid; _ } -> "percentage" ^ dec_unit decimalen eenheid
  | Enumeratie { domein = Some d; _ } -> "enumeration '" ^ d ^ "'"
  | Sum_type { domein; _ } -> "sumtype '" ^ domein ^ "'"
  | Struct_type { domein; _ } -> "struct '" ^ domein ^ "'"
  | List_type { element } -> "list of " ^ datatype_detail element
  | other -> datatype_kind other

(* The keuzetype a datatype names (IR-SPEC ch. 2.4) — its VARIANT and its
   declaring domain: an Enumeration's or SumType's domain, looked up THROUGH a
   ListType's element — a list of a keuzetype names that keuzetype, and a
   drifted element domain on a list-typed reference is the wrong-answer flank
   the card measured (clingo folded 0 where ref faulted). None for every
   other datatype and for an Enumeration carrying no domain. The variant
   travels with the name because `domeinen` is ONE namespace: a domain
   declared as one variant and used as the other is never the same keuzetype
   (fix round 1: an enumeration-typed reference over a sum-typed attribute
   of the same name was verifier-clean and faulted in both executors). *)
type keuzetype = Kt_enum of string | Kt_sum of string

let rec keuzetype_domain = function
  | Enumeratie { domein = Some d; _ } -> Some (Kt_enum d)
  | Enumeratie { domein = None; _ } -> None
  | Sum_type { domein; _ } -> Some (Kt_sum domein)
  | List_type { element } -> keuzetype_domain element
  | _ -> None

let keuzetype_name = function Kt_enum d | Kt_sum d -> d

let keuzetype_variant = function
  | Kt_enum _ -> "enumeration domain"
  | Kt_sum _ -> "sum-type domain"

(* the identity: the same variant and the same name, compared ci (WF-001) *)
let keuzetype_same a b =
  norm (keuzetype_name a) = norm (keuzetype_name b)
  && match (a, b) with Kt_enum _, Kt_enum _ | Kt_sum _, Kt_sum _ -> true | _ -> false

(* WF-089 (domain identity, IR-SPEC ch. 2.4): a derived node's annotation
   and the datatype of the source it TAKES its value from must name the same
   keuzetype (variant and domain, ci) whenever either of them names one — the
   domain extension of the soort cross-check WF-023 and WF-027 perform.
   WF-088 and WF-061 bind a use site to the domain it NAMES; this is the read
   half of the other binding. Since L2 a reference carries no annotation of
   its own (B13), so the use sites are the derived ones: an element-passing
   fold, whose source is each OPERAND it hands through (an operand typed by
   its declaration, so the fold chains to one), and the composite positions
   of [composite_identity]. The write half is WF-023's domain clause
   ([wf023_assignment]); together they make ref's "match has no arm" fault
   unreachable on a verifier-clean document. Either side unknown — an
   unresolved source or a use site carrying no type — defers, exactly as
   WF-023 does. [what] names the use site in the message, [source] what it
   reads. *)
let wf089_domain_identity ?(source = "the declaration it reads from") at ~what ~decl ~typ =
  match (decl, typ) with
  | None, _ | _, None -> []
  | Some d, Some t -> (
      match (keuzetype_domain t, keuzetype_domain d) with
      | None, None -> []
      | Some a, Some b when keuzetype_same a b -> []
      | Some a, Some b when norm (keuzetype_name a) = norm (keuzetype_name b) ->
          [ v "WF-089" at
              (Printf.sprintf
                 "%s is typed as the %s '%s', but %s is the %s '%s' \
                  (one namespace: a domain has one variant)"
                 what (keuzetype_variant a) (keuzetype_name a) source (keuzetype_variant b)
                 (keuzetype_name b)) ]
      | Some a, Some b ->
          [ v "WF-089" at
              (Printf.sprintf
                 "%s is typed with keuzetype domain '%s', but %s names domain '%s'"
                 what (keuzetype_name a) source (keuzetype_name b)) ]
      | Some a, None ->
          [ v "WF-089" at
              (Printf.sprintf
                 "%s is typed with keuzetype domain '%s', but %s is %s and names no keuzetype \
                  domain"
                 what (keuzetype_name a) source (datatype_kind d)) ]
      | None, Some b ->
          [ v "WF-089" at
              (Printf.sprintf
                 "%s is typed %s, naming no keuzetype domain, but %s names domain '%s'"
                 what (datatype_kind t) source (keuzetype_name b)) ])

(* The struct domain a datatype names (IR-SPEC ch. 2.4) — a StructType's
   declaring domain, looked up THROUGH a ListType's element exactly as
   [keuzetype_domain] is. A struct domain is NOT a keuzetype (the ch. 2.4
   family term covers Enumeration and SumType only, maintainer decision), and
   `domeinen` being one namespace there is no variant to carry: a domain
   declared as a struct and named as a struct is the same domain iff the
   names agree (ci, WF-001). *)
let rec struct_domain = function
  | Struct_type { domein; _ } -> Some domein
  | List_type { element } -> struct_domain element
  | _ -> None

(* WF-090 (struct-domain identity, IR-SPEC ch. 2.4): the struct sibling of
   WF-089's read half and WF-023's domain clause, sharing their machinery and
   their call sites but carrying its own rule number (maintainer decision
   2026-09-03). A struct-typed use site and the declaration it crosses MUST
   name the same struct domain whenever either of them names one. WF-072
   binds a use site to the declaration it NAMES, WF-073 a literal to the
   domain IT names and WF-074 a field read to the domain the BASE names —
   none of them crosses a struct value against the struct-typed declaration
   it comes from or enters, which is why a StructLiteral of domain B written
   into an attribute of domain A was verifier-clean while ref faulted at the
   field read and the ASP backend gave a wrong answer (measured, the card).
   Either side unknown — an unresolved source or a use site carrying no type
   — defers, exactly as WF-089 and WF-023 do. *)
let wf090_struct_identity ?(source = "the declaration it reads from") at ~what ~decl ~typ =
  match (decl, typ) with
  | None, _ | _, None -> []
  | Some d, Some t -> (
      match (struct_domain t, struct_domain d) with
      | None, None -> []
      | Some a, Some b when norm a = norm b -> []
      | Some a, Some b ->
          [ v "WF-090" at
              (Printf.sprintf "%s is typed with struct domain '%s', but %s names struct domain '%s'"
                 what a source b) ]
      | Some a, None ->
          [ v "WF-090" at
              (Printf.sprintf
                 "%s is typed with struct domain '%s', but %s is %s and names no struct domain" what
                 a source (datatype_kind d)) ]
      | None, Some b ->
          [ v "WF-090" at
              (Printf.sprintf
                 "%s is typed %s, naming no struct domain, but %s names struct domain '%s'" what
                 (datatype_kind t) source b) ])

(* The DOMAIN facet alone: WF-089 for the keuzetype family, WF-090 for
   struct domains, first difference reported. The facet engine below uses
   this as its domain step; the element-passing aggregation arm uses it as
   its WHOLE cross, because there the node's type is not a copy of one
   declaration — see [use_site_identity]'s note on that arm. *)
let use_site_domain ?(source = "the declaration it reads from") at ~what ~decl ~typ =
  match wf089_domain_identity ~source at ~what ~decl ~typ with
  | [] -> wf090_struct_identity ~source at ~what ~decl ~typ
  | vs -> vs

(* --- the facet engine (WF-091 and the numbers it defers to) ------------- *)

(* Structural identity of a unit (ch. 2.5): abbreviation, declaring system,
   canonical unit, conversion factor and — recursively — the denominator of a
   composite rate. EXACT, never converting, which is the whole point: `kg`
   and `t` share a canonical unit and a shape, so [units_convertible] (what
   WF-035 and WF-023 cross) calls them compatible, and a lie inside the
   convertible class is compatible by construction. Only identity sees it.

   The three NAMES are compared BYTE for BYTE, where every other declared
   name in this file folds to its WF-001 form. Name RESOLUTION is
   case-insensitive — that is WF-001's domain — but this rule is about COPY
   FIDELITY, a different question: a consumer that reads these three names
   literally to pick its conversion diverges silently on a copy that lies in
   case alone. Measured: the Drools unit logic compares abbreviation and
   canonical name case-sensitively and falls back to factor one where they
   do not match, so a `KG`/`GEWICHT`/`KG` copy of a `kg`/`Gewicht`/`kg`
   declaration answers 3750.00 where ref and clingo answer 3.75. The factor
   and the denominator shape stay exact as well: those are not names. *)
let rec eenheid_identical (a : eenheid) (b : eenheid) =
  a.afkorting = b.afkorting && a.stelsel = b.stelsel && a.canoniek = b.canoniek
  && a.factor_naar_canoniek = b.factor_naar_canoniek
  &&
  match (a.noemer, b.noemer) with
  | None, None -> true
  | Some x, Some y -> eenheid_identical x y
  | _ -> false

let rec eenheid_render (u : eenheid) =
  (match u.afkorting with Some a -> "'" ^ a ^ "'" | None -> "an unnamed unit")
  ^ (match u.stelsel with Some s -> " of system '" ^ s ^ "'" | None -> "")
  ^ (match (u.factor_naar_canoniek, u.canoniek) with
    | Some f, Some c -> Printf.sprintf " (factor %s to '%s')" (rationaal_render f) c
    | Some f, None -> Printf.sprintf " (factor %s)" (rationaal_render f)
    | None, Some c -> Printf.sprintf " (canonical '%s')" c
    | None, None -> "")
  ^ match u.noemer with Some n -> " per " ^ eenheid_render n | None -> ""

let unit_render = function None -> "no unit" | Some u -> eenheid_render u

(* The numeric facets a type turns on. The sign is not among them: see the
   note on [use_site_identity]. *)
type num_facets = { nf_geheel : bool; nf_decimalen : int; nf_eenheid : eenheid option }

let num_facets_of = function
  | Numeriek { geheel; decimalen; eenheid; _ } | Percentage { geheel; decimalen; eenheid } ->
      Some { nf_geheel = geheel; nf_decimalen = decimalen; nf_eenheid = eenheid }
  | _ -> None

(* the numeric facets are read THROUGH matching ListType layers, exactly as
   the domain facets are: a list of kilograms and a list of tonnes differ in
   their unit, not in some nameless residual *)
let rec peel_lists a b =
  match (a, b) with
  | List_type { element = x }, List_type { element = y } -> peel_lists x y
  | _ -> (a, b)

(* WF-091 (derived-type identity, IR-SPEC ch. 4.1) and the numbers it
   defers to. ONE comparison of a derived node's annotation against the
   type its operands derive ([composite_expectation] below), facet by facet
   in one fixed order — soort, domain, unit, decimals, wholeness — of which
   the FIRST differing facet is reported, once: one divergence is one
   message per comparison, never two facets for one comparison.

   The rule NUMBER follows the facet, not this function. The domain facet is
   WF-089 (keuzetype family) and WF-090 (struct domains); every facet
   without a number of its own is WF-091's, its name in the message. Since
   L2 the five reference classes carry no annotation (B13) and the
   reference-type arms of all three rules left with it: a reference's type
   IS its declaration's ([expr_type]), so there is no copy to cross.

   The soort facet deliberately stops short of the domain-carrying kinds:
   `domeinen` is ONE namespace, so enumeration-versus-sum-type and
   domain-versus-no-domain are the domain facet's own clauses (WF-089's
   variant and one-sided arms, WF-090's), stated and numbered there.

   Either side absent defers.

   [skip] names facets this comparison does NOT make: the expectation is a
   formula over the operands, and a formula may be silent on a facet while
   speaking on the rest. Every entry is reason-carrying at its call site.

   Until the L2 fix round the order ended in two more facets, the sign and
   the residual structure (the whole datatype, domain names folded ci),
   which stood behind the named ones for the reference-type arms. Those
   arms left with the reference's own type (B13), and at the one caller
   left, the derived position, every arm skipped both — the sign because a
   derivation mostly under-claims it (see the note on
   [composite_expectation]), the residual because it would re-report the
   facets an arm declines. Measured at removal: all sixteen skip lists
   named both, so neither facet could report anything. *)
let use_site_identity ?(source = "the declaration it reads from") ?(skip = []) at ~what ~decl
    ~typ =
  match (decl, typ) with
  | None, _ | _, None -> []
  | Some d, Some t ->
      let mismatch facet shown_t shown_d =
        [ v "WF-091" at
            (Printf.sprintf "%s mismatch: %s is typed %s, but %s is %s" facet what shown_t source
               shown_d) ]
      in
      let names_a_domain x = keuzetype_domain x <> None || struct_domain x <> None in
      let soort_facet () =
        if names_a_domain t || names_a_domain d then []
        else if datatype_kind t = datatype_kind d then []
        else mismatch "soort" (datatype_kind t) (datatype_kind d)
      in
      let domain_facet () = use_site_domain ~source at ~what ~decl ~typ in
      let tl, dl = peel_lists t d in
      let numeric pick facet render =
        match (num_facets_of tl, num_facets_of dl) with
        | Some ft, Some fd when pick ft <> pick fd -> mismatch facet (render ft) (render fd)
        | _ -> []
      in
      let unit_facet () =
        match (num_facets_of tl, num_facets_of dl) with
        | Some ft, Some fd
          when not
                 (match (ft.nf_eenheid, fd.nf_eenheid) with
                 | None, None -> true
                 | Some a, Some b -> eenheid_identical a b
                 | _ -> false) ->
            mismatch "unit" (unit_render ft.nf_eenheid) (unit_render fd.nf_eenheid)
        | _ -> []
      in
      let decimals_facet () =
        numeric
          (fun f -> f.nf_decimalen)
          "decimals"
          (fun f -> Printf.sprintf "%d decimals" f.nf_decimalen)
      in
      let whole_facet () =
        numeric
          (fun f -> f.nf_geheel)
          "wholeness"
          (fun f -> if f.nf_geheel then "whole" else "fractional")
      in
      let rec first = function
        | [] -> []
        | f :: rest -> ( match f () with [] -> first rest | vs -> vs)
      in
      first
        (List.filter_map
           (fun (n, f) -> if List.mem n skip then None else Some f)
           [ ("soort", soort_facet); ("domain", domain_facet); ("unit", unit_facet);
             ("decimals", decimals_facet); ("wholeness", whole_facet) ])

(* --- the DERIVED expectation on composite positions (WF-091) ------------ *)

(* A reference's type is its declaration's (since L2 the reference carries
   no copy of it); a composite type is a DERIVATION over its operands (RUNTIME-SEMANTICS ch. 2.10, clause 2). The
   facet engine above needs a [decl] to compare against, and at a composite
   there is none — which is why WF-091's honesty note 4 recorded the numeric
   facets of a derived type as crossed by nothing. What follows puts the
   DERIVATION where the declaration sits: the expectation is computed from
   the operands' static types ([expr_type]) by the ch. 2.3 precision table
   and the ch. 2.6 unit rules, and the node's annotation is crossed against
   it by the facet engine, under the number-follows-the-facet rule — so the
   DOMAIN facet reports under WF-089/WF-090 at a composite
   (`wf089-composiet-domein`).

   How far the chain reaches. An operand's own type is anchored in turn — a
   reference IS its declaration's type since L2, a composite is crossed
   against its own operands — and where every operand underneath is
   anchored that way, the composite is bound to the declarations beneath
   it. That is the good case,
   not a theorem: the chain reaches only through operands something actually
   crosses, and it BREAKS at a fourth-class operand. A dividedBy over two
   unitless operands may be annotated `kg`, nothing crosses it, and a rounding
   above it then derives `kg` all the way into a `kg` target, verifier-clean
   (measured 2026-09-04: 26 such operand positions over 19 of the 174
   golden+rood documents). The deferral proper is not self-servable — a
   value node carrying no type at all is already WF-034-ill-formed, and a
   reference or an Unresolved whose type nothing declares is reported by
   its own rule — but a fabricated fourth-class annotation is the stronger
   escape and stays OPEN: it does not switch the cross off, it feeds it a
   premise.

   The SIGN is claimed at no arm below, and the facet engine has no sign
   facet since the L2 fix round, for this reason: a derivation mostly
   under-claims a sign restriction while a document
   narrows it truthfully (a clamp with a minimum of 0 IS non-negative whatever
   its base says), and it over-claims at the count arm, where two anchor
   goldens widen nonNegative to unspecified. Measured 2026-09-04: crossing it
   costs 5 golden+rood documents on the additive arm, 7 over all arms. *)

let dt_decimals = function Numeriek { decimalen; _ } | Percentage { decimalen; _ } -> decimalen | _ -> 0
let dt_whole = function Numeriek { geheel; _ } | Percentage { geheel; _ } -> geheel | _ -> false

(* A number an arm below derives is built in the loader's reading
   ([Loader.datatype], the Ast header): a unit '%' — a rate whose numerator
   is '%' among them — makes it a [Percentage], any other unit or none a
   [Numeriek]. The annotation it is crossed against went through that
   reading at load, so a '%' result built as a [Numeriek] would refuse every
   annotation its position can carry. Since L3 that is a live shape: the
   RegelSpraak frontend types a sum of two percentages as a number with the
   unit '%', its emitter writes a `Number` with the unit '%', and the loader
   reads that as a percentage (measured 2026-09-24: before this reading,
   WF-091 refused the soort of the frontend's own plus, minus and
   reducedBy over two percentage attributes). *)
let derived_number ~geheel ~decimalen (eenheid : eenheid option) =
  match eenheid with
  | Some { afkorting = Some "%"; _ } -> Percentage { geheel; decimalen; eenheid }
  | _ -> Numeriek { geheel; decimalen; teken = Some Onbepaald; eenheid }

(* The ch. 2.6 times join, in the two shapes the IR fixes: a unitless operand
   hands the other unit through (the dimensionless 1 absorbs), and a rate
   times its convertible denominator unit CANCELS to the numerator ("€/jr
   maal jr = €"). The third shape — two plain units — is ch. 2.5's opaque
   PRODUCT, whose algebra the spec fixes (sorted multiset of atomic factors,
   no canonical, no factor) but whose wire SPELLING it does not: an identity
   cross there would pin an `abbreviation` and a `system` ch. 2.5 does not
   require. The fourth — an unspellable rate factor — is the shape the
   frontend refuses outright (RUNTIME-SEMANTICS ch. 2.10). This function
   makes no claim at either, and the corpus carries zero of both (measured
   2026-09-04: no times node in golden, rood, negatief or the parity zone has
   two unit-bearing operands, so the cancel arm is pinned by a negatief and
   nothing else). *)
let maal_unit (a : eenheid option) (b : eenheid option) : eenheid option option =
  match (a, b) with
  | None, x | x, None -> Some x
  | Some x, Some y ->
      let cancel (rate : eenheid) (plain : eenheid) =
        match rate.noemer with
        | Some n when plain.noemer = None && units_convertible plain n ->
            Some (Some { rate with noemer = None })
        | _ -> None
      in
      (match cancel x y with Some u -> Some u | None -> cancel y x)

(* The RegelSpraak frontend's set reading (`ExpressieBouwer`), mirrored — the
   scheidslijn the §4.20 denominator strip below turns on, taken from the
   emitter rather than restated beside it. An aggregation operand list is a
   NON-TEMPORAL SET reading when the fold carries a range ([ranged]: its one
   argument is then the set source) and that argument reads an attribute
   its subjectType declares NO granularity for. Both halves are the
   frontend's and both are load-bearing: a set read over a TEMPORAL
   attribute is the §7.1 per-element timeline fold (each element's tijdreeks
   summed over time FIRST), which DOES integrate, and the bindingless read
   is the subject's own §4.20 rate read proper — so neither is this shape.

   [None] wherever a SET read's granularity is UNREADABLE — an object type
   this document does not declare, or a half of the resolving pair left
   empty. The two arms disagree exactly there, so this one defers rather
   than picking one: the family's own treatment of an unresolvable
   declaration, and no silent choice of a strip on an unread premise. The
   frontend cannot reach the case (it holds a RESOLVED objecttype, whose
   null branch means the reference did not resolve at all) and no
   verifier-admissible document can either — an empty half is WF-034-
   ill-formed in a fold source since the widening. The corpus carries none
   of the three shapes: every total operand in golden, rood, negatief and
   the parity zone resolves (measured 2026-09-04). *)
let set_lezing ctx ~ranged (args : expr list) : bool option =
  match args with
  | [ Attribuut_ref { objecttype = Some otn; attribuut = Some a; _ } ] when ranged -> (
      match objecttype ctx otn with Some ot -> Some (not (attr_temporal ot a)) | None -> None)
  | [ Attribuut_ref _ ] when ranged -> None
  | _ -> Some false

(* The expectation for one composite position, or [None] where v0 derives
   nothing there. The tuple is (what the message names, what the message
   calls the source, the derived datatype, the facets NOT claimed).

   What is NOT here, and why — the fourth class of RUNTIME-SEMANTICS
   ch. 2.10, kept identical to the exception set of gate 13
   (`scripts/check-reftype-derivation.sh`, which keys on (node, op) and
   (node, kind) for exactly this reason): a division, a power, a
   percentage-of, a root, an absolute value and the seven date/time
   extractors, where v0 states no result-unit rule; and duration /
   absoluteDuration, where the annotation IS the source (the operator's
   own parameter, WF-038). Not derived means not crossed — and named, never
   silently skipped. Two node kinds are left out for the opposite reason:
   an IfThenElse and a Let already cross their whole type against their
   branches and their body under WF-069 and WF-076. *)
let composite_expectation ctx ~binders (e : expr) :
    (string * string * datatype * string list) option =
  let ty x = expr_type ctx ~binders x in
  let operand_t x = Option.bind x ty in
  match e with
  | Binair { op = (Plus | Min | Verminderd_met) as op; links; rechts; _ } -> (
      let what = "the " ^ Spelling.encode Spelling.binary_op op ^ " composite" in
      match (operand_t links, operand_t rechts) with
      | Some l, Some r ->
          if op <> Verminderd_met && is_date l && is_number_like r && unit_of r <> None then
            (* date +/- duration (ch. 2.2): the result IS the date operand's
               type, unchanged — the duration operand carries the calendar
               unit and contributes nothing to the result type *)
            Some (what, "the type derived from its date operand", l, [])
          else if
            is_number_like l && is_number_like r && units_compatible (unit_of l) (unit_of r)
          then
            (* ch. 2.3: maxDec, whole iff both operands are; ch. 2.6: the
               result carries the LEFT unit, and a '%' result is a percentage
               ([derived_number]). The SIGN is not claimed here or at any
               other arm — see the header note. *)
            Some
              ( what, "the type derived from its operands",
                derived_number
                  ~geheel:(dt_whole l && dt_whole r)
                  ~decimalen:(max (dt_decimals l) (dt_decimals r))
                  (unit_of l),
                [] )
          else
            (* a one-sided unit pair: WF-035's literal-adoption clause
               (ch. 4.6) admits the shape, but the four sources of the
               result unit disagree on it — the frontend and ch. 2.6 say
               "the LEFT unit", the ASP translator and the Drools backend
               say "the left, else the right". No claim on a contested
               formula. *)
            None
      | _ -> None)
  | Binair { op = Maal; links; rechts; _ } -> (
      match (operand_t links, operand_t rechts) with
      | Some l, Some r when is_number_like l && is_number_like r -> (
          match maal_unit (unit_of l) (unit_of r) with
          | None -> None
          | Some u ->
              (* the UNIT only. The precision facets are measured
                 non-derived: ch. 2.3's "sum of the operands' decimals" and
                 the frontend's "never whole" are under-claims two other
                 producers in this very corpus do not share (the Catala
                 plugin writes its pinned ch. 2.9 scale), 9 decimals and 9
                 wholeness witnesses over golden+rood+negatief. *)
              Some
                ( "the times composite", "the type derived from its operands",
                  derived_number ~geheel:false ~decimalen:0 u,
                  [ "decimals"; "wholeness" ] ))
      | _ -> None)
  | Afgerond { basis = Some b; decimalen; _ } -> (
      (* ch. 2.4 clause 2: a rounding changes the FACING precision to its own
         decimal count (whole when 0) and touches nothing else — ch. 2.10:
         it carries its base's unit. *)
      match ty b with
      | Some (Numeriek { teken; eenheid; _ }) ->
          Some
            ( "the rounding", "the type derived from its base and its decimal count",
              Numeriek { geheel = decimalen = 0; decimalen; teken; eenheid },
              [] )
      | Some (Percentage { eenheid; _ }) ->
          (* the percentage arm carries NO corpus witness (measured 2026-09-04:
             zero roundings over a percentage base in any zone); it keeps the
             base's unit, which is what ch. 2.10, the ASP translator and the
             Drools backend all say — the RegelSpraak frontend drops it there,
             a divergence recorded rather than followed *)
          Some
            ( "the rounding", "the type derived from its base and its decimal count",
              Percentage { geheel = decimalen = 0; decimalen; eenheid },
              [] )
      | Some other ->
          Some ("the rounding", "the type derived from its base", other, [])
      | None -> None)
  | Begrensd { basis = Some b; _ } -> (
      (* ch. 2.7: a clamp returns one of three values of the base's own type.
         Its SIGN is the sharpest case for the exclusion the header states: a
         clamp with a minimum of 0 IS non-negative whatever its base says. *)
      match ty b with
      | Some d -> Some ("the clamp", "the type derived from its base", d, [])
      | None -> None)
  | Functie { kind = Aantal; _ } ->
      (* ch. 2.3: a count is a whole, non-negative, unitless number. Two
         anchor goldens write unspecified on a count — the header's exclusion in
         its widening direction. *)
      Some
        ( "the count aggregation", "the type a count derives",
          Numeriek { geheel = true; decimalen = 0; teken = Some Niet_negatief; eenheid = None },
          [] )
  | Functie { kind = Aantal_gevulde; _ } ->
      (* The SAME derivation, and it needs its own arm because this engine
         matches on the kind: the filled count answers a CARDINALITY, so
         ch. 3.4c fixes its result to a whole, unitless number whatever unit
         the folded attribute carries. Without this arm the kind was the one
         aggregation whose node type nothing crossed — measured 2026-09-20 on
         a filled count typed 'kg' with two decimals, written into an
         equally-lying kg target: verifier PASS, while the SAME lie under
         count is WF-091, and `ref` then played it (a unitless 1 landing in
         a kg-declared attribute, oracle PASS). The type is derived, not
         declared, so the annotation is the one thing here that cannot be
         allowed to say anything. *)
      Some
        ( "the filledCount aggregation", "the type a filled count derives",
          Numeriek { geheel = true; decimalen = 0; teken = Some Niet_negatief; eenheid = None },
          [] )
  | Functie { kind = (Som | Eerste | Laatste | Totaal | Minimum | Maximum) as k;
              argumenten = a0 :: rest; bron_rol; bron_pad; _ } -> (
      let what = "the " ^ Spelling.encode Spelling.aggregation_kind k ^ " aggregation" in
      match ty a0 with
      | None -> None
      | Some t0 -> (
          match k with
          | Som ->
              (* §4.6 Eenheid1: a sum is its first operand's type, unchanged *)
              Some (what, "the type derived from its first operand", t0, [])
          | Eerste | Laatste ->
              (* an element-passing fold returns a contributor unchanged. The
                 DOMAIN facet is skipped here and only here because it is
                 already crossed at this position, once per operand and under
                 its own number, by WF-089/WF-090's aggregation arm — this
                 cross adds the facets that arm deliberately left out. *)
              Some
                ( what, "the type derived from its first operand", t0,
                  [ "domain" ] )
          | Totaal -> (
              (* §4.20: the temporal total INTEGRATES a rate over time, so the
                 result drops the /tijdseenheid and keeps the numerator — but
                 only where the operand integrates. The scheidslijn is the
                 emitter's own, [set_lezing] above: the NON-TEMPORAL SET
                 reading ("het totaal van <x> van alle <elementen>" over a
                 plain attribute) is an ALIAS of the sum, an instance fold
                 over nothing, and carries the element type verbatim exactly
                 as the sum arm does; every other reading — the non-set rate
                 read and the set read over a temporal attribute — keeps the
                 strip. Stripping unconditionally invented a wire type no
                 executor can produce (`jutterswet-tempototaal`, whose set
                 fold adds EUR/mnd values into a EUR/mnd target). The two
                 executor-side derivations do not strip at ALL (measured: both
                 answer the first operand's unit whole) — a divergence recorded
                 at the card, harmless on this corpus, where every total's own
                 annotation follows the emitter. *)
              match set_lezing ctx ~ranged:(bron_rol <> None || bron_pad <> None) (a0 :: rest) with
              | None -> None
              | Some true ->
                  Some (what, "the type derived from its first operand", t0, [])
              | Some false ->
                  (* the strip keeps the constructor: a rate whose numerator
                     is '%' strips to '%' and stays the percentage the loader
                     read, as any other rate strips to a number. Since L3 a '%' rate is a
                     [Percentage] whichever producer typed it, and the
                     RegelSpraak frontend strips the rate its own sum of two
                     '%' rates carries (measured 2026-09-24: without the
                     second arm, WF-091 refused the unit of that total) *)
                  let d =
                    match t0 with
                    | Numeriek ({ eenheid = Some ({ noemer = Some _; _ } as u); _ } as n) ->
                        Numeriek { n with eenheid = Some { u with noemer = None } }
                    | Percentage ({ eenheid = Some ({ noemer = Some _; _ } as u); _ } as p) ->
                        Percentage { p with eenheid = Some { u with noemer = None } }
                    | other -> other
                  in
                  Some (what, "the type derived from its first operand", d, []))
          | Minimum | Maximum ->
              if rest = [] then
                Some
                  ( what, "the type derived from its operand", t0,
                    [ "domain" ] )
              else
                let ts = List.map ty (a0 :: rest) in
                if List.exists (fun x -> x = None) ts then None
                else
                  let ts = List.filter_map (fun x -> x) ts in
                  (* ch. 2.3 / ch. 4.1: the least operand precision, the
                     conjunction of wholeness, operand 0's sign and unit *)
                  (match t0 with
                   | Numeriek { teken; eenheid; _ } ->
                       Some
                         ( what, "the type derived from its operands",
                           Numeriek
                             { geheel = List.for_all dt_whole ts;
                               decimalen =
                                 List.fold_left (fun m x -> min m (dt_decimals x)) max_int ts;
                               teken; eenheid },
                           [ "domain" ] )
                   | Percentage { eenheid; _ } ->
                       (* the same formula over a percentage operand 0: since
                          L3 a '%' number is a [Percentage], and the RegelSpraak
                          frontend's minimum over two percentage sums writes
                          the least precision (measured 2026-09-24: passing
                          operand 0's precision through, WF-091 refused its
                          decimals). The frontend refuses the fold over a
                          declared percentage, so it writes no other reading *)
                       Some
                         ( what, "the type derived from its operands",
                           Percentage
                             { geheel = List.for_all dt_whole ts;
                               decimalen =
                                 List.fold_left (fun m x -> min m (dt_decimals x)) max_int ts;
                               eenheid },
                           [ "domain" ] )
                   | _ ->
                       Some
                         ( what, "the type derived from its first operand", t0,
                           [ "domain" ] ))
          | _ -> None))
  | _ -> None

(* WF-091, the numeric-operand arm (IR-SPEC ch. 4.1). A sum and a total
   add their operands and the pro-rata part scales its base by true days
   over bucket days (RUNTIME-SEMANTICS ch. 3.5-3.7): all three are
   arithmetic, so each is typed over a Number alone. Over any other
   declaration there is no derived type — and the pass-through derivation
   of the two folds above agreed with a Date annotation over a Date
   operand, so such a document was verifier-clean while `ref` faulted on
   the arithmetic (`maal`, `plus`, "pro-rata base is not numeric") and,
   before the card temporele-datumvouw, the ASP backend answered the day
   number. Every reading of the two folds shares the arm — the subject's
   own temporal total and its gedurende clip, the §7.1 per-element fold,
   the set fold over a plain attribute, the listed sum — because the
   arithmetic is the same in each. A list is read through its element, the
   value a list fold adds. An operand whose type nothing declares defers,
   as everywhere in this rule. Where the arm reports, the identity cross
   below does not run: there is no derived type to compare with. *)
let wire_type_name = function
  | Numeriek _ | Percentage _ -> "Number"
  | Datum_in_dagen | Datum_tijd -> "Date"
  | Tekst -> "Text"
  | Booleaan -> "Boolean"
  | Enumeratie _ -> "Enumeration"
  | Sum_type _ -> "SumType"
  | List_type _ -> "ListType"
  | Struct_type _ -> "StructType"

let rec list_element = function List_type { element } -> list_element element | d -> d

let numeric_operand_arm ctx ~binders at (e : expr) =
  let refuse ~head ~verb i a =
    match Option.map list_element (expr_type ctx ~binders a) with
    | Some d when not (is_number_like d) ->
        let operand =
          match a with
          | Attribuut_ref { attribuut = Some n; objecttype = Some ot; _ } ->
              Printf.sprintf "'%s' on '%s'" n ot
          | Parameter_ref { parameter = Some p; _ } -> Printf.sprintf "the parameter '%s'" p
          | _ -> Printf.sprintf "its operand %d" i
        in
        let t = wire_type_name d in
        let why =
          match d with
          | Datum_in_dagen | Datum_tijd -> "Date values are calendar positions"
          | _ -> t ^ " values are not amounts"
        in
        [ v "WF-091"
            (Printf.sprintf "%s/arguments/%d" at i)
            (Printf.sprintf
               "%s of %s %s %s values: a total, sum or pro-rata part is arithmetic over a \
                Number, and %s"
               head operand verb t why) ]
    | _ -> []
  in
  match e with
  | Functie { kind = (Som | Totaal) as k; argumenten; _ } ->
      let head = "the " ^ Spelling.encode Spelling.aggregation_kind k in
      List.concat (List.mapi (refuse ~head ~verb:"folds") argumenten)
  | Functie { kind = Tijdsevenredig_deel as k; argumenten = base :: _; _ } ->
      let head = "the pro-rata part (" ^ Spelling.encode Spelling.builtin_kind k ^ ")" in
      refuse ~head ~verb:"scales" 0 base
  | _ -> []

let composite_identity ctx ~binders at (e : expr) =
  match numeric_operand_arm ctx ~binders at e with
  | _ :: _ as vs -> vs
  | [] -> (
      match composite_expectation ctx ~binders e with
      | None -> []
      | Some (what, source, derived, skip) ->
          use_site_identity ~source ~skip (at ^ "/type") ~what ~decl:(Some derived)
            ~typ:(annotation e))

(* WF-030 over a uniqueness version's sources (IR-SPEC ch. 3.6). Since L4
   one construct carries both shapes (delta B27): one source is the
   single-type uniqueness, two or more the union, which the loader reads into
   the two in-memory lists. That a version has a source and a source a key
   is the loader's (a list of at least one), so what is left per source is
   resolution, and across a union's sources agreement. *)
let wf030 ctx =
  let source_checks at otn keys =
    match otn with
    | None -> [ v "WF-030" (at ^ "/objectType") "uniqueness source without an object type" ]
    | Some otn -> (
        match objecttype ctx otn with
        | None -> [ v "WF-030" (at ^ "/objectType") ("source type '" ^ otn ^ "' is not declared") ]
        | Some ot ->
            List.concat
              (List.mapi
                 (fun j k ->
                   let kat = Printf.sprintf "%s/keys/%d" at j in
                   match k with
                   | None -> [ v "WF-030" kat "null key entry" ]
                   | Some k when not (has_attr ot k) ->
                       [ v "WF-030" kat
                           ("key '" ^ k ^ "' is not a declared attribute of '" ^ otn ^ "'") ]
                   | Some _ -> [])
                 keys))
  in
  List.concat_map
    (fun (r : resolved_uniciteit) -> source_checks (r.uni_pointer ^ "/sources/0") r.uni_subject r.sleutels)
    ctx.doc.uniciteiten
  @ List.concat_map
      (fun (r : resolved_uniciteit_unie) ->
        let at = r.uu_pointer in
        let per_bron =
          List.concat
            (List.mapi
               (fun k b -> source_checks (Printf.sprintf "%s/sources/%d" at k) b.uub_type b.uub_sleutels)
               r.bronnen)
        in
        let arities = List.map (fun b -> List.length b.uub_sleutels) r.bronnen in
        let arity_v =
          match arities with
          | a :: rest when List.exists (fun x -> x <> a) rest ->
              [ v "WF-030" at "union sources disagree on key arity" ]
          | _ -> []
        in
        (* per-position datatype-kind agreement across sources *)
        let kinds_of b =
          match b.uub_type with
          | None -> None
          | Some t ->
              objecttype ctx t
              |> Option.map (fun ot ->
                  List.map
                    (fun k ->
                      match k with
                      | Some k -> attr_of ot k |> Option.join |> Option.map datatype_kind
                      | None -> None)
                    b.uub_sleutels)
        in
        let kind_v =
          match List.filter_map kinds_of r.bronnen with
          | first :: rest when arity_v = [] ->
              if
                List.exists
                  (fun other ->
                    List.exists2
                      (fun a b -> match a, b with Some x, Some y -> x <> y | _ -> false)
                      first other)
                  (List.filter (fun o -> List.length o = List.length first) rest)
              then [ v "WF-030" at "union sources disagree on a key position's datatype kind" ]
              else []
          | _ -> []
        in
        (* per-position unit agreement — the v0 resolver refuses a union
           whose key units differ (zelfdeDatatypes), so a €/$ mix could
           otherwise slip through as a silent never-duplicate *)
        let units_of b =
          match b.uub_type with
          | None -> None
          | Some t ->
              objecttype ctx t
              |> Option.map (fun ot ->
                  List.map
                    (fun k ->
                      match k with
                      | Some k -> attr_of ot k |> Option.join |> Option.map unit_of
                      | None -> None)
                    b.uub_sleutels)
        in
        let unit_v =
          match List.filter_map units_of r.bronnen with
          | first :: rest when arity_v = [] ->
              if
                List.exists
                  (fun other ->
                    List.exists2
                      (fun a b -> match a, b with Some x, Some y -> x <> y | _ -> false)
                      first other)
                  (List.filter (fun o -> List.length o = List.length first) rest)
              then [ v "WF-030" at "union sources disagree on a key position's unit" ]
              else []
          | _ -> []
        in
        per_bron @ arity_v @ kind_v @ unit_v)
      ctx.doc.uniciteit_unies

(* One structural pass over a day-kind condition (IR-SPEC par. 5, the
   stratum-0 framing; ch. 3.7). It collects, in one walk:
   - the WF-031 purity violations: a day-kind condition is evaluated against
     a bare date before the rule graph, with no fact base, so it may
     reference only dates, date-components, easterSunday, literals,
     parameters and other day-kind names — never derived object state. An
     attribute read (AttributeReference) or a characteristic check
     (CharacteristicCheck) is refused (loud, where v0 neither checks nor
     documents this);
   - the day-kind names it references (a Predicate of kind dayKind), for the
     WF-085 reference graph. *)
let dagsoort_scan at (c0 : conditie) : violation list * string list =
  let viols = ref [] and refs = ref [] in
  let impure kind name =
    viols :=
      v "WF-031" at
        (Printf.sprintf
           "day-kind condition reads %s '%s' — a day-kind condition references only dates, \
            date-components, easterSunday, literals, parameters and other day kinds, never \
            derived object state (an attribute or characteristic read)"
           kind name)
      :: !viols
  in
  let session name =
    viols :=
      v "WF-031" at
        (Printf.sprintf
           "day-kind condition reads the session value '%s' — a day kind classifies a bare date \
            at stratum 0 (ch. 5.4), so its predicate is a function of THE DAY alone; the bound \
            day (BoundDay) is admitted, a ContextValue naming a session value is refused"
           name)
      :: !viols
  in
  let rec ex (e : expr) =
    let opt = function Some e -> ex e | None -> () in
    match e with
    | Attribuut_ref { attribuut; _ } -> impure "attribute" (Option.value attribuut ~default:"?")
    (* The context value is the one place this walk must read a NAME. WF-098
       (ch. 4.1) closes the set to the two session values and says nothing
       about POSITION; the position rule belongs here, because it is a
       semantic one: the executors evaluate the condition per calendar day,
       so a session-dependent predicate would classify the same day
       differently in two sessions of the same document. The bound day IS
       that day and is admitted — since L4 it is a node of its own
       (BoundDay), never a context value; the two SESSION values are not.

       The arm names those two and nothing else, which keeps it exact: a name
       OUTSIDE the set is WF-098's to reject (and does not become a WF-031
       report as well — measured on `wf098-onbekende-contextwaarde-naam`,
       whose `de dagX` sits in exactly this position). A context value
       without a name cannot be written since L4. *)
    | Constante { naam = Some n; _ }
      when norm n = norm Spelling.evaluation_date || norm n = norm Spelling.evaluation_year ->
        session n
    | Constante _ | Bound_day _ | Literal _ | Parameter_ref _ | Variabele_ref _
    | Expr_onopgelost _ | Binder_ref _ ->
        ()
    | Dimensie_cel { attribuut; _ } -> opt attribuut
    | Binair { links; rechts; _ } -> opt links; opt rechts
    | Afgerond { basis; _ } -> opt basis
    | Begrensd { basis; minimum; maximum; _ } -> opt basis; opt minimum; opt maximum
    | Field_access { base; _ } -> ex base
    | Struct_literal { velden; _ } -> List.iter (fun (_, e) -> ex e) velden
    | List_literal { elements; _ } -> List.iter ex elements
    | Construct { argument; _ } -> opt argument
    | Temporal_read { basis; datum; _ } -> ex basis; ex datum
    | Invoke { arguments; _ } -> List.iter ex arguments
    | Tekst_reeks { delen; _ } ->
        List.iter (function Ingevoegd { expr = Some e } -> ex e | _ -> ()) delen
    | Functie { argumenten; leegwaarde; filter; _ } ->
        List.iter ex argumenten;
        opt leegwaarde;
        (match filter with Some c -> co c | None -> ())
    | If_then_else { condition; then_; else_; _ } -> co condition; ex then_; ex else_
    | List_length { list; _ } -> ex list
    | Match { scrutinee; arms; _ } -> ex scrutinee; List.iter (fun (_, _, b) -> ex b) arms
    | Lambda { body; _ } -> ex body
    | Filter_list { list; condition; _ } -> ex list; co condition
    | Fold { list; functie; init; _ } -> ex list; ex init; fold_fn functie
    | Map_list { list; functie; _ } -> ex list; fold_fn functie
    | Let { value; body; _ } -> ex value; ex body
    | Instance_set_union { branches; _ } ->
        (* a union ranges over INSTANCES, which a day kind — a function of
           the bare day (ch. 5.4) — may not reach at all; naming the
           population is what makes the refusal readable *)
        List.iter
          (fun (b : instance_set_branch) ->
            impure "instance population" (Option.value b.isb_element_type ~default:"?");
            match b.isb_filter with Some c -> co c | None -> ())
          branches
  and fold_fn = function Fn_lambda { body; _ } -> ex body | Fn_name _ -> ()
  and co (c : conditie) =
    let opt = function Some e -> ex e | None -> () in
    match c with
    | Vergelijking { links; rechts; _ } -> opt links; opt rechts
    | Predicaat { soort = Dagsoort; dagsoort; operand; _ } ->
        (match dagsoort with Some dk -> refs := dk :: !refs | None -> ());
        opt operand
    | Predicaat { operand; _ } -> opt operand
    | Periode_conditie { vanaf; tot; _ } -> opt vanaf; opt tot
    | Samengesteld { onderdelen; _ } -> List.iter co onderdelen
    | Kenmerk_check { kenmerk; _ } -> impure "characteristic" (Option.value kenmerk ~default:"?")
    (* the WF-031 admission list is closed: relation membership and rule-version state are
       derived fact-base reads, outside "dates, date-components, easterSunday, literals,
       parameters and other day kinds" — flag them like the attribute/characteristic reads *)
    | Role_membership { role; _ } -> impure "role membership" role
    | Regelversie { regel; _ } -> impure "rule-version state" (Option.value regel ~default:"?")
    (* the quantifier fold ranges over the fact base — instances reached
       through a relation — which is the same derived read the membership
       arms above are flagged for; the day-kind admission list is closed *)
    | Kwantorvouw { kv_element_objecttype; _ } ->
        impure "instance quantification" (Option.value kv_element_objecttype ~default:"?")
    | Conditie_onopgelost _ -> ()
  in
  co c0;
  (List.rev !viols, List.rev !refs)

let wf031 ctx =
  let named =
    List.filter_map
      (fun (i, r) ->
        Option.map (fun n -> (n, Printf.sprintf "/dayKindDefinitions/%d" i)) r.rd_naam)
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)
  in
  (dups_by norm named
   |> List.map (fun (a, _, at) -> v "WF-031" at ("more than one definition for day kind '" ^ a ^ "'")))
  @ List.concat_map
      (fun (i, r) ->
        let at = Printf.sprintf "/dayKindDefinitions/%d" i in
        (match r.rd_voorwaarde with
         | None -> [ v "WF-031" at "day-kind definition without a condition" ]
         (* the purity clause (IR-SPEC par. 5): no derived-state read *)
         | Some c -> fst (dagsoort_scan (at ^ "/condition") c))
        (* The validity MUST be always, neither bound written: no executor
           consults a day-kind window, so a bounded one would be silently
           dead. *)
        @ (match r.rd_geldigheid with
           | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
               [ v "WF-031" at
                   "day-kind definition with a bounded validity window; must be always (from and to both absent)" ]
           | _ -> [])
        (* The name IS which day kind is being defined, so an absent or blank
           one defines nothing and is dead in exactly the way the bounded
           window above is dead — same clause, same reason. It is NOT
           WF-107's: that rule holds of the NAMED FORMS, whose names share
           one namespace WF-020 keeps unique, and a day kind's name lives in
           its own, which the uniqueness arm at the top of this check keeps.

           Presence is tested BEFORE resolution, or the blank arm would be
           unreachable in practice: a blank name is not a declared day kind
           either, so the resolution arm would answer first and report
           "undeclared day kind ' '" — true, and the wrong diagnosis. *)
        @ match r.rd_naam with
          | None ->
              [ v "WF-031" at
                  "day-kind definition without a name; it defines no declared day kind and is \
                   silently dead" ]
          | Some n when collapse_ws n = "" ->
              [ v "WF-031" at
                  "day-kind definition with a blank name; it defines no declared day kind and is \
                   silently dead" ]
          | Some n when not (mem_ci (dagsoort_namen ctx) n) ->
              [ v "WF-031" at ("definition for undeclared day kind '" ^ n ^ "'") ]
          | _ -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)

(* WF-085 — the day-kind reference graph MUST be acyclic (IR-SPEC par. 5,
   the stratum-0 framing / ch. 3.7). A day-kind condition may reference other
   day kinds (a Predicate of kind dayKind); mutually recursive definitions have no
   fixpoint and are refused loudly, where the v0 toolchain resolves a cycle
   to a silent false (§9.10 prohibition unenforced — the diverge-from-v0 delta
   recorded on WF-031). Undeclared references are WF-034's concern and carry
   no edge here (an unresolvable reference never hides a cycle in a clean
   document, the dep_forms discipline). *)
let wf085 ctx =
  let defs = Array.of_list ctx.doc.resolved_dagsoorten in
  let n = Array.length defs in
  (* name (normalised) -> first defining index; duplicates are WF-031's concern *)
  let index = Hashtbl.create 16 in
  Array.iteri
    (fun i (d : resolved_dagsoort) ->
      match d.rd_naam with
      | Some nm when not (Hashtbl.mem index (norm nm)) -> Hashtbl.add index (norm nm) i
      | _ -> ())
    defs;
  let succ =
    Array.map
      (fun (d : resolved_dagsoort) ->
        match d.rd_voorwaarde with
        | None -> []
        | Some c ->
            snd (dagsoort_scan "" c)
            |> List.filter_map (fun r -> Hashtbl.find_opt index (norm r))
            |> List.sort_uniq compare)
      defs
  in
  let color = Array.make n 0 in
  let viols = ref [] in
  let name i = Option.value defs.(i).rd_naam ~default:(Printf.sprintf "#%d" i) in
  let rec dfs path i =
    match color.(i) with
    | 1 ->
        let rec take acc = function
          | [] -> acc
          | j :: _ when j = i -> j :: acc
          | j :: rest -> take (j :: acc) rest
        in
        let cyc = take [] path in
        let names = String.concat " -> " (List.map name (cyc @ [ i ])) in
        viols :=
          v "WF-085"
            (Printf.sprintf "/dayKindDefinitions/%d" i)
            ("day-kind reference cycle (no fixpoint; the toolchain resolves it to a silent false): "
            ^ names)
          :: !viols
    | 2 -> ()
    | _ ->
        color.(i) <- 1;
        List.iter (fun j -> dfs (i :: path) j) succ.(i);
        color.(i) <- 2
  in
  Array.iteri (fun i _ -> if color.(i) = 0 then dfs [] i) defs;
  List.rev !viols

let wf032 ctx =
  let entries = List.mapi (fun i r -> (i, r)) ctx.doc.resolved_startpunten in
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/startPointDeterminations/%d" i in
      (match r.sp_tijdlijn with
       | None -> [ v "WF-032" at "start point without a timeline" ]
       | Some t -> (
           match find_tijdlijn ctx t with
           | None -> [ v "WF-032" at ("start point for undeclared timeline '" ^ t ^ "'") ]
           | Some tl when not tl.variabel_startpunt ->
               [ v "WF-032" at ("start point for fixed-start timeline '" ^ t ^ "'") ]
           | Some _ -> []))
      @ (match r.sp_objecttype, r.lid with
         | Some otn, Some lid -> (
             match objecttype ctx otn with
             | None -> [ v "WF-032" at ("object type '" ^ otn ^ "' is not declared") ]
             | Some ot -> (
                 match attr_of ot lid with
                 | Some (Some Datum_in_dagen) -> []
                 | Some (Some d) ->
                     [ v "WF-032" at
                         ("anchor member '" ^ lid ^ "' is " ^ datatype_kind d
                          ^ ", must be a day-precision date") ]
                 | Some None | None ->
                     [ v "WF-032" at
                         ("anchor member '" ^ lid ^ "' is not a declared attribute of '" ^ otn ^ "'") ]))
         | _ -> [ v "WF-032" at "start point without object type or member" ])
      @
      match r.sp_objecttype, r.sp_tijdlijn with
      | Some otn, Some t ->
          List.filter_map
            (fun (j, r2) ->
              if j > i then
                match r2.sp_objecttype, r2.sp_tijdlijn with
                | Some otn2, Some t2 when norm otn2 = norm otn && tl_norm t2 = tl_norm t ->
                    Some (v "WF-032" at
                            (Printf.sprintf
                               "more than one start-point determination for (%s, %s)" otn t))
                | _ -> None
              else None)
            entries
      | _ -> [])
    entries

let wf033 ctx =
  let all_form_names =
    List.concat_map
      (fun (_, entries) ->
        List.filter_map (fun (n, _, _) -> Option.map (fun n -> norm (strip_version n)) n) entries)
      (form_arrays ctx)
  in
  let oc_by_name n =
    List.find_opt
      (fun r ->
        match r.oc_naam with
        | Some m -> norm (strip_version m) = norm (strip_version n)
        | None -> false)
      ctx.doc.object_creaties
  in
  List.concat_map
    (fun (i, g) ->
      let at = Printf.sprintf "/ruleGroups/%d" i in
      let leden = some_names g.leden in
      List.concat_map
        (fun lid ->
          if List.mem (norm (strip_version lid)) all_form_names then []
          else [ v "WF-033" at ("group member '" ^ lid ^ "' does not resolve to a rule form") ])
        leden
      @
      if g.recursief then
        let ocs = List.filter_map oc_by_name leden in
        let fc_member lid =
          List.exists
            (fun (f : resolved_feit_creatie) ->
              match f.fc_naam with
              | Some m -> norm (strip_version m) = norm (strip_version lid)
              | None -> false)
            ctx.doc.feit_creaties
        in
        (* the recursion carrier is a creation member of EITHER kind:
           an object creation needs its guard (each generation mints new
           instances), a relation creation is idempotent over the fixed
           universe and needs none (ch. 5.5) *)
        (if ocs = [] && not (List.exists fc_member leden) then
           [ v "WF-033" at "recursive group without a creation member" ]
         else [])
        @ List.concat_map
            (fun oc ->
              if oc.oc_voorwaarde = None then
                [ v "WF-033" at
                    ("unguarded object creation '"
                     ^ Option.value oc.oc_naam ~default:"?"
                     ^ "' in a recursive group") ]
              else [])
            ocs
      else [])
    (List.mapi (fun i g -> (i, g)) ctx.doc.regel_groepen)

(* ======================================================================= *)
(* Expression layer: WF-034 .. WF-043 (walk)                               *)
(* ======================================================================= *)

(* [annotation] and [expr_type] are defined above WF-027 (they type a
   characteristic initialisation's value); the expression walk below shares
   them. *)

(* Is this expression a time-dependent operand (for WF-043)? *)
let expr_temporal ctx = function
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ } -> (
      match objecttype ctx otn with
      | Some ot -> attr_temporal ot a
      | None -> false)
  | Parameter_ref { parameter = Some p; _ } -> (
      match find_ci ctx.doc.parameters p with
      | Some par -> par.par_granulariteit <> None
      | None -> false)
  | _ -> false

(* --- sum-type helpers (IR-SPEC ch. 2.4 / ch. 4.7) ----------------------- *)

(* The declared constructor list of a domain, iff it is declared (in
   [domeinen]) as a SumType. This is the nominal source of truth for
   WF-061..064: a use site names a domain, and the declaration is what it
   must agree with — never the inline (possibly drifted) copy. *)
let sum_decl ctx domein : (string * datatype option) list option =
  match find_ci ctx.doc.domeinen domein with
  | Some (Some (Sum_type { constructoren; _ })) -> Some constructoren
  | _ -> None

(* The declared value list of a domain, iff it is declared (in [domeinen]) as
   an Enumeration — the nominal source of truth for the enumeration arm of
   WF-063, the keuzetype sibling of [sum_decl] (ch. 2.4). An enumeration
   alternative is nullary by construction, so its "payload" is always None:
   returning the (label, payload) shape lets the Match totality and binder
   checks read one alternative list for both wire variants. *)
let enum_decl ctx domein : (string * datatype option) list option =
  match find_ci ctx.doc.domeinen domein with
  | Some (Some (Enumeratie { waarden; _ })) ->
      Some (List.map (fun w -> (w, None)) (some_names waarden))
  | _ -> None

(* WF-088: the enumeration sibling of WF-061. Every Enumeration datatype
   OUTSIDE the domain registry is a use site — an attribute or parameter
   datatype, a literal's or reference's `type`, a Match scrutinee's type — and
   it repeats the declared value list inline, exactly as a SumType use site
   repeats its constructors. That inline copy is a repetition, never a second
   opinion: it MUST carry the declared values, ci, in the declared (normative,
   WF-006) order. Without the rule a drifted copy is verifier-clean and
   reaches the executors, because WF-039 reads the LOCAL list while WF-063
   reads the DECLARATION — an alien label then passes both and ref faults at
   evaluation while clingo derives nothing.

   Nested datatypes are use sites too, so the walk recurses through list
   elements, sum payloads and struct fields (WF-060/065/071 fence enumerations
   out of those positions, so the recursion is belt-and-braces). A use site
   whose `domain` is null or names no declared enumeration is not ours —
   WF-003 and WF-063 own the unresolved reference. *)
let rec enum_use_site ctx at (d : datatype) : violation list =
  match d with
  | Enumeratie { domein = Some dn; waarden } -> (
      match find_ci ctx.doc.domeinen dn with
      | Some (Some (Enumeratie { waarden = declared; _ })) ->
          if
            List.length waarden = List.length declared
            && List.for_all2
                 (fun a b ->
                   match (a, b) with Some x, Some y -> norm x = norm y | _ -> false)
                 waarden declared
          then []
          else
            [ v "WF-088" at
                ("enumeration use site disagrees with the declaration of domain '" ^ dn
               ^ "' (the same values, in declaration order, required)") ]
      | _ -> [])
  | Enumeratie { domein = None; _ } -> []
  | List_type { element } -> enum_use_site ctx at element
  | Sum_type { constructoren; _ } ->
      List.concat_map
        (function _, Some p -> enum_use_site ctx at p | _, None -> [])
        constructoren
  | Struct_type { velden; _ } ->
      List.concat_map (fun (_, fd) -> enum_use_site ctx at fd) velden
  | _ -> []

(* Two constructor lists agree when they carry the same constructors in the
   same order with structurally equal payloads (names case-insensitively,
   the WF-001 identity rule; payloads by the datatype equality WF-049 uses). *)
let constructors_agree (a : (string * datatype option) list)
    (b : (string * datatype option) list) =
  List.length a = List.length b
  && List.for_all2 (fun (n1, p1) (n2, p2) -> norm n1 = norm n2 && p1 = p2) a b

(* The declared field list of a domain, iff it is declared (in [domeinen])
   as a StructType — the nominal source of truth for WF-072..074, the
   struct sibling of [sum_decl]. *)
let struct_decl ctx domein : (string * datatype) list option =
  match find_ci ctx.doc.domeinen domein with
  | Some (Some (Struct_type { velden; _ })) -> Some velden
  | _ -> None

(* Two field lists agree when they carry the same fields in the same order
   with structurally equal datatypes (names ci; datatypes by value equality —
   the WF-061 constructors_agree pattern). *)
let fields_agree (a : (string * datatype) list) (b : (string * datatype) list) =
  List.length a = List.length b
  && List.for_all2 (fun (n1, d1) (n2, d2) -> norm n1 = norm n2 && d1 = d2) a b

(* WF-072's use-site cross, as a datatype WALK — the struct sibling of
   [enum_use_site], and for the same measured reason. A `StructType` carries an
   inline copy of the domain's field list on the wire, and the consumers read
   that copy rather than the declaration: ref's shared case loader crosses a
   record against the inline `velden` of the use site (case.ml), and the ASP
   backend encodes struct terms POSITIONALLY out of it. So a drifted copy is
   not decoration — it splits the executors. Measured, before this walk
   existed: a `ListType`-element copy renaming the fields let ref fault at the
   field read while clingo passed green; one expression `type` with the fields
   in REVERSED order left the verifier, ref and the schema clean while clingo
   derived 5.03 for 125.50 — a wrong answer on a verifier-clean document.
   Hence every `StructType` in a datatype tree, not just a top-level attribute
   or parameter datatype: through a `ListType` element too. A use site naming
   no declared struct domain is caught at the same spot (the pre-walk clause
   of the rule).

   Two positions are deliberately NOT walked, each because no negatief could
   isolate it. A matched struct's own `velden`: [fields_agree] is an equality
   on the WHOLE field datatype, so a drifted nested copy already makes the
   enclosing copy disagree — the argument WF-073/074 make for their nested
   forms. And a sum-type PAYLOAD: WF-060 admits no struct payload in a
   declaration at all (measured — a hand-built one is refused there), so the
   only place such a payload can sit is a drifted inline sum copy, which is
   WF-061's already-recorded open flank, not this rule's. *)
let rec struct_use_site ctx at (d : datatype) : violation list =
  match d with
  | Struct_type { domein; velden } -> (
      match struct_decl ctx domein with
      | None ->
          [ v "WF-072" at
              ("struct-type use site names domain '" ^ domein
             ^ "', which is not a declared struct type") ]
      | Some decl ->
          if fields_agree decl velden then []
          else
            [ v "WF-072" at
                ("struct-type use site disagrees with the declaration of domain '" ^ domein
               ^ "' (same fields, datatypes and order required)") ])
  | List_type { element } -> struct_use_site ctx at element
  | _ -> []

(* The declared FunctionDef of [name], iff present (WF-067..068/070). *)
let function_decl ctx name : function_def option =
  List.find_opt (fun (fd : function_def) -> norm fd.fd_naam = norm name) ctx.doc.function_defs

let is_list_type = function List_type _ -> true | _ -> false

(* A first-order non-list datatype: Numeriek, day-precision Date, a declared
   SumType, or a declared StructType — the admissible list element (WF-065,
   widened by Phase 5c with struct elements) and the pattern WF-060 uses for
   constructor payloads. *)
let admissible_element ctx = function
  | Numeriek _ | Datum_in_dagen -> true
  | Sum_type { domein; _ } -> sum_decl ctx domein <> None
  | Struct_type { domein; _ } -> struct_decl ctx domein <> None
  | _ -> false

let scalar_one_arg_kinds =
  [ Wortel; Absolute_waarde; Eerste_paasdag; Jaar_uit; Maand_uit; Dag_uit;
    Uur_uit; Minuut_uit; Seconde_uit; Milliseconde_uit ]

let aggregation_kinds =
  [ Som; Totaal; Aantal; Minimum; Maximum; Eerste; Laatste; Aantal_gevulde ]

(* The folds that hand an ELEMENT through unchanged: they select a
   contributor, they never compute a new value, so the node's own type is
   the operand's type. (`Som`/`Totaal`/`Aantal` compute, and their result is
   numeric by construction — no keuzetype travels through them.) With one
   contributor `Arith.agg_extreme` makes no comparison at all, so a
   passthrough node whose type names another domain than its operand hands
   an alien label to whatever reads it — WF-089's aggregation position. *)
let passthrough_folds =
  List.map
    (fun k -> (k, Spelling.encode Spelling.aggregation_kind k))
    [ Eerste; Laatste; Minimum; Maximum ]

(* Does [name] match a declared role's name or plural (ci)? *)
let is_role_name ctx name =
  List.exists
    (fun (_, ft) ->
      List.exists
        (fun r ->
          (match r.rol_naam with Some n -> norm n = norm name | None -> false)
          || match r.rol_meervoud with Some n -> norm n = norm name | None -> false)
        ft.rollen)
    ctx.doc.feittypen

(* The ch. 4.1 type-pair derivation, mirrored from the executors: the
   two-role fact types relating [from_type] to [to_type] (either way).

   The pair set is CHAIN-CLOSED (IR-SPEC ch. 2.6): a role declaring filler
   T is filled by every S ⊑ T, so a fact type declared on supertypes
   relates every subtype pair below it. Since L2 no navigation, fold range,
   allocation or scope is derived from a type pair (B12): what still asks
   this question is WF-052's created-side role and the dependency edge of an
   object creation, whose back-link binding stays optional.

   [fills] is also the JOIN test of every walk in this file — "the walk
   stands at T; does T fill this hop's near role?" — and the role check's
   subject test (WF-034, [Role_membership]). Every near-side join goes
   through it: WF-053's navigation chain and its characteristic check,
   WF-102's sourcePath step, WF-028's creation chain and WF-054's uniqueness
   scope. The "this position is used AS type Y" questions are ONE-SIDED,
   because a position accepts every instance at or below its declared type
   and none above it: WF-053's endpoint (⊑ the written endpoint, which is
   exactly what ref faults on per instance), WF-028's anchorRole/targetRole
   agreement (⊑ the role's declared filler), and WF-052's created-side
   role, which is not a comparison at all but the executors' own
   [connecting_role_candidates] — "the first role the created type fills"
   picks the ANCESTOR's role under ⊑ and reads its cardinality, so the
   verdict would follow the order the wire lists the roles in. WF-102's
   landing on the elementType stays TWO-SIDED on the WF-047 line; the
   comment there says why.

   The rule that separates them: [fills] and the one-sided [is_sub] where
   a position must HOLD what reaches it, [type_compatible] only where a
   position is READ through, which is sound upward from every filler and
   is what WF-047 already meant by "agree". *)
let fills ctx ~inst ~(role : rol) =
  is_sub ctx ~sub:inst ~super:(Option.value role.rol_objecttype_naam ~default:"")

let connecting_candidates ctx ~from_type ~to_type =
  List.filter_map
    (fun (fn, (ft : feittype)) ->
      let f inst role = fills ctx ~inst ~role in
      match ft.rollen with
      | [ a; b ] ->
          if (f from_type a && f to_type b) || (f from_type b && f to_type a) then Some fn
          else None
      | [ r ] when ft.wederkerig ->
          (* the single reciprocal role relates the type to itself *)
          if f from_type r && norm from_type = norm to_type then Some fn else None
      | _ -> None)
    ctx.doc.feittypen

(* Candidate ROLES, not fact types — v0's verbindendeRol derivation (every
   role of the far type whose fact type also relates the near type). Since
   L2 its one reader is an object creation without a back-link binding
   (WF-052's created-side role below; the executors derive that back-link
   with ref's [Eval.connecting_pair], ch. 5.5). A self-typed two-role fact
   type offers BOTH its roles and so blocks the derivation, as
   [connecting_pair] does. Until L2 this was also the ch. 4.3 fallback of a
   bindingless fold and allocation (the executors' Eval.connecting_role,
   and WF-034 lifted the self-typed block to well-formedness there); both
   left with that fallback (B12). *)
let connecting_role_candidates ctx ~from_type ~to_type =
  List.concat_map
    (fun (_, (ft : feittype)) ->
      let f inst role = fills ctx ~inst ~role in
      match ft.rollen with
      | [ a; b ] ->
          (if f from_type a && f to_type b then [ b ] else [])
          @ (if f from_type b && f to_type a then [ a ] else [])
      | [ r ] when ft.wederkerig ->
          if f from_type r && norm from_type = norm to_type then [ r ] else []
      | _ -> [])
    ctx.doc.feittypen

(* The SEVEN aggregation kinds that read a VALUE per contributor — every fold
   kind except count, which reads the contributors themselves (ch. 1.2e: a
   count is never empty because it never looks inside a member). Until the
   L2 fix round WF-093's message named the kind through this list; since
   then that message is the sentence the executors share, which names no
   kind, and no check here reads the list. It stays as the verifier's
   statement of the value-fold kinds, which the RegelSpraak frontend's
   `VouwsoortenGrensTest` holds the frontend's own list against.

   Six until 2026-09-20, when filledCount joined them (IR-SPEC ch. 4.3,
   RUNTIME-SEMANTICS ch. 3.4c). It is a count in its RESULT and a value fold
   in its READING, and this list is about the reading: it asks each
   contributor for a value and tests that value for presence. Joining here is
   therefore not an exception to the 2026-09-19 narrowing but its application
   — what left the wire that day was a count reading VALUES under a node
   whose semantics fold CONTRIBUTORS, and this kind's semantics fold
   values. *)
let value_fold_name = function
  | (Som | Totaal | Minimum | Maximum | Eerste | Laatste | Aantal_gevulde) as k ->
      Some (Spelling.encode Spelling.aggregation_kind k)
  | _ -> None

(* The roles reachable from [from_type] whose declared name or plural
   matches [name] (ci): the far role of every two-role fact type whose near
   role [from_type] fills, and the one role of a reciprocal fact type it
   fills. The near side is a FILLER question — an instance of [from_type]
   fills a role declared at any of its supertypes (ch. 2.6). Returns
   (fact type, reached role). *)
let roles_named_from ctx ~from_type ~name =
  let matches (r : rol) =
    (match r.rol_naam with Some n -> norm n = norm name | None -> false)
    || match r.rol_meervoud with Some m -> norm m = norm name | None -> false
  in
  List.filter_map
    (fun (fn, (ft : feittype)) ->
      let f role = fills ctx ~inst:from_type ~role in
      match ft.rollen with
      | [ a; b ] ->
          if f a && matches b then Some (fn, b)
          else if f b && matches a then Some (fn, a)
          else None
      | [ r ] when ft.wederkerig -> if f r && matches r then Some (fn, r) else None
      | _ -> None)
    ctx.doc.feittypen

(* The reading of an instance fold's SOURCE — the single argument of an
   aggregation carrying an elementType (IR-SPEC ch. 4.3, the reading of the source). The
   range decides it: with one (sourceRole or sourcePath) the argument is the SET
   source, read per contributor; without one it is the subject's own read,
   which the loader admits under total alone. Under count the set source
   counts the instances the range reaches, and its `attribute` is the
   written name of the bound role (WF-047 crosses it); under every other
   aggregation it reads `attribute` on each contributor, so the name is an
   attribute of the elementType.

   The count source is recognised at the source itself: its `attribute`
   names no member of the element type. That is the whole of
   the distinction WF-093 draws in both directions — a count source is the
   source of count and of nothing else, and count reads nothing but a
   count source — so the recognition must not read the node's kind, or the
   rule would be circular. A name that is no member of the element type and
   no role reachable from the subject either is neither reading: under a
   value fold it is an unresolved member (WF-034), under count a written
   role name that denotes nothing (WF-047). *)
type source_reading =
  | Src_count (* count over a source naming no member of the element type *)
  | Src_count_of_member (* count over a member of the element type: WF-093 *)
  | Src_value (* a value fold's set source, a member of the element type *)
  | Src_role_under_value_fold (* a value fold over a role name: WF-093 *)
  | Src_unresolved (* a value fold over a name that resolves nowhere *)
  | Src_own (* no range: the subject's own read (ch. 3.6) *)
  | Src_undecided (* the element type is undeclared: WF-034 reports it *)

let source_reading ctx ~element_type ~subject ~kind ~ranged a =
  if not ranged then Src_own
  else
    match objecttype ctx element_type with
    | None -> Src_undecided
    | Some ot -> (
        let member = has_attr ot a || has_kenmerk ot a in
        match (kind, member) with
        | Aantal, true -> Src_count_of_member
        | Aantal, false -> Src_count
        | _, true -> Src_value
        | _, false ->
            let role =
              match subject with
              | Some st -> roles_named_from ctx ~from_type:st ~name:a <> []
              | None -> is_role_name ctx a
            in
            if role then Src_role_under_value_fold else Src_unresolved)

(* The fold-source rules over that reading, for an attribute reference in
   the source position ([aggregation_source_check] is called for the single
   argument of an aggregation with an elementType and nowhere else;
   every other operand is an ordinary read, walked by [walk_expr]).

   WF-093, both directions (maintainer decision 2026-09-06 for the first
   direction): a count source hands the fold the linked INSTANCES, which
   only count reads; and count folds instances, so a source reading a
   member of each contributor hands it values it does not consume.

   WF-083 (c), the anchor, per position: the source's `subjectType`
   names the type the executors read it at — the elementType for a
   value fold's set source (every declaration is looked up on the element),
   the fold SUBJECT for the count source (its written role name is the
   subject's relation) and for the subject's own read. It is a guard, not a
   selector: a spelling that disagrees names another declaration than the
   one the executors resolve, so it is refused, never read. Where WF-093
   refuses the reading itself the anchor has nothing to add and stays
   silent, so one defect keeps one rule.

   The member resolution (WF-034, WF-080) runs on the source's own
   `subjectType`, as on every attribute reference; the count source's name is
   WF-047's (it must denote the bound role), so it is exempt here. *)
let aggregation_source_check ctx at ~element_type ~subject ~kind ~ranged e =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn; navigatie_bindingen; _ } -> (
      (* a fold source never navigates: its relation is the fold's range —
         navigation bindings in this slot are ill-formed (WF-053) *)
      (match navigatie_bindingen with
       | Some _ ->
           [ v "WF-053" at
               "a path on a fold source is ill-formed (the fold's relation is its range, \
                sourceRole or sourcePath)" ]
       | None -> [])
      @
      let reading = source_reading ctx ~element_type ~subject ~kind ~ranged a in
      (match reading with
       | Src_role_under_value_fold ->
           (* the sentence ref, the ASP backend and the Drools loader share
              since the L2 fix round, so a refusal reads the same in all four *)
           [ v "WF-093" at
               (Printf.sprintf
                  "value aggregation over %S, which is no attribute of %S: a value fold reads an \
                   attribute per contributor, and a role name is a count source only a count folds"
                  a element_type) ]
       | Src_count_of_member ->
           [ v "WF-093" at
               ("the source of this count aggregation reads '" ^ a
              ^ "', a member of the element type '" ^ element_type
              ^ "': a count folds INSTANCES, so its source is a count source naming the \
                 relation's role, never a value read per contributor") ]
       | _ -> [])
      @ (let anchor =
           match reading with
           | Src_value | Src_unresolved -> Some element_type
           | Src_count | Src_own -> subject
           | Src_count_of_member | Src_role_under_value_fold | Src_undecided -> None
         in
         match anchor with
         | Some anc when norm otn <> norm anc ->
             [ v "WF-083" at
                 ("fold-source subjectType '" ^ otn ^ "' is not the fold's anchor '" ^ anc ^ "' ("
                 ^ (match reading with
                   | Src_count -> "a count source is read on the fold subject"
                   | Src_own -> "a fold without a range reads the subject's own attribute"
                   | _ -> "a set source reads each member of the elementType")
                 ^ "): the executors resolve '" ^ a
                 ^ "' against the anchor, never against this field, so the two spellings name \
                    different declarations") ]
         | _ -> [])
      @
      match objecttype ctx otn with
      | None -> [ v "WF-034" at ("object type '" ^ otn ^ "' is not declared") ]
      | Some ot -> (
          match reading with
          | Src_count | Src_count_of_member | Src_role_under_value_fold | Src_undecided -> []
          | Src_value | Src_unresolved | Src_own ->
              if has_attr ot a then []
              else if has_kenmerk ot a then
                (* WF-080: an aggregation source is a value position — a
                   characteristic endpoint carries no value here *)
                [ v "WF-080" at
                    ("'" ^ a ^ "' is a characteristic of '" ^ otn
                   ^ "', not an attribute — a characteristic endpoint is ill-formed in an \
                      aggregation source position") ]
              else
                [ v "WF-034" at
                    ("'" ^ a ^ "' is not a declared attribute of '" ^ otn
                   ^ "': a value fold's source reads an attribute of each contributor") ]))
  | _ -> []

(* --- WF-047: symbolic role bindings --------------------------------------- *)

(* Resolve a binding to (fact type, bound role, opposite role). Only the
   declared role name binds — the plural is presentation (WF-047). *)
let resolve_binding ctx (rb : rol_binding) =
  match find_ci ctx.doc.feittypen rb.rb_feittype with
  | None -> Error ("role binding names undeclared fact type '" ^ rb.rb_feittype ^ "'")
  | Some (ft : feittype) -> (
      match ft.rollen with
      | [ a; b ] -> (
          let matches r =
            match r.rol_naam with Some n -> norm n = norm rb.rb_rol | None -> false
          in
          match (matches a, matches b) with
          | true, false -> Ok (ft, a, b)
          | false, true -> Ok (ft, b, a)
          | true, true ->
              Error
                ("role name '" ^ rb.rb_rol ^ "' is ambiguous within fact type '"
               ^ rb.rb_feittype ^ "'")
          | false, false ->
              Error
                ("role binding names no declared role '" ^ rb.rb_rol ^ "' of fact type '"
               ^ rb.rb_feittype ^ "' (the plural does not bind)"))
      | [ r ] when ft.wederkerig ->
          (* the reciprocal shape (§13.3.9.2, WF-011/013): one role
             relating the type to itself — bound and opposite are the
             same role *)
          let matches (r : rol) =
            match r.rol_naam with Some n -> norm n = norm rb.rb_rol | None -> false
          in
          if matches r then Ok (ft, r, r)
          else
            Error
              ("role binding names no declared role '" ^ rb.rb_rol ^ "' of fact type '"
             ^ rb.rb_feittype ^ "' (the plural does not bind)")
      | _ ->
          Error
            ("role binding into fact type '" ^ rb.rb_feittype
           ^ "', which does not have exactly two roles (or one, reciprocal)"))

(* WF-047: the binding resolves and its fillers agree with the node's
   declared source/target types (skip a side the node leaves unnamed). *)
let wf047_binding ctx at (rb : rol_binding) ~bound_filler ~other_filler =
  match resolve_binding ctx rb with
  | Error e -> [ v "WF-047" at e ]
  | Ok (_, bound, other) ->
      (* "agree" is ⊑-COMPATIBILITY, not equality (IR-SPEC ch. 2.2/2.6): a
         role's declared filler type is an UPPER bound, so a fact type
         declared on a supertype legitimately carries subtype instances and
         a node may name either end of one chain. Two types in different
         chains still disagree. *)
      let agree what (r : rol) = function
        | None -> []
        | Some expected ->
            let filler = Option.value r.rol_objecttype_naam ~default:"" in
            if type_compatible ctx filler expected then []
            else
              [ v "WF-047" at
                  (Printf.sprintf "the %s role '%s' fills '%s', but the node names '%s'" what
                     (Option.value r.rol_naam ~default:"?")
                     filler expected) ]
      in
      agree "bound" bound bound_filler @ agree "opposite" other other_filler

(* --- WF-053: the navigated read -------------------------------------------- *)

(* The hop list of a navigated reference, subject-outward: (fact type,
   reached role) per step. Every binding resolves (WF-047's resolution: the
   declared role name binds), every hop's near side is filled by the type
   the walk stands at, and the walk lands on the endpoint. Used by the
   WF-053 check; the dependency graph reads the bindings' relations
   directly. Since L2 the bindings are the whole navigation (B11): a read
   without them is the subject's own read, so there is nothing here to
   derive. *)
let nav_hops ctx ~subject ~(bindings : rol_binding list) ~(endpoint : string option) :
    ((string * rol) list, string list) result =
  let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
  (* ONE-SIDED, deliberately: the walk lands on a role whose declared
     filler is [c], so every instance it can hold is ⊑ c, and all of them
     satisfy the endpoint only when c ⊑ e. The other direction — a
     supertype filler against a subtype endpoint — is the unsound one, and
     ref refuses it at run time per instance (`the navigation reached %S,
     not the declared endpoint %S`). A two-sided test here would admit a
     document ref faults on and clingo answers, which is ref ≢ clingo on
     verifier-clean input. *)
  let endpoint_check cur =
    match (endpoint, cur) with
    | Some e, Some c when not (is_sub ctx ~sub:c ~super:e) ->
        [ Printf.sprintf "the navigation reaches '%s', not the endpoint '%s'" c e ]
    | _ -> []
  in
  let step (i, cur, acc, errs) (rb : rol_binding) =
    match resolve_binding ctx rb with
    | Error e -> (i + 1, None, acc, errs @ [ e ])
    | Ok (_, bound, opposite) ->
        let near =
          match cur with
          | Some c when not (fills ctx ~inst:c ~role:opposite) ->
              [ Printf.sprintf
                  "hop %d over '%s': the near side fills '%s', but the walk stands at '%s'" i
                  rb.rb_feittype (filler opposite) c ]
          | _ -> []
        in
        (i + 1, Some (filler bound), acc @ [ (rb.rb_feittype, bound) ], errs @ near)
  in
  let _, cur, acc, errs = List.fold_left step (0, Some subject, [], []) bindings in
  match errs @ endpoint_check cur with [] -> Ok acc | errs -> Error errs

(* WF-053 over one navigated attribute reference: the walk resolves, and
   every hop is to-one — a to-many step needs an aggregation, never a
   silent first pick (the v0 resolver builds only to-one chains; NAAR_VEEL
   is its marker). Every position this is called for wants ONE value: a
   fold's set source never navigates (its relation is the fold's range), so
   no read reaches here that could consume a collection. *)
let wf053_attribuut_ref ctx at ~subject ~bindings ~endpoint =
  match nav_hops ctx ~subject ~bindings ~endpoint with
  | Error msgs -> List.map (fun m -> v "WF-053" at m) msgs
  | Ok hops ->
      List.concat
        (List.mapi
           (fun i ((_, (r : rol))) ->
             if r.rol_meervoudig then
               [ v "WF-053" at
                   (Printf.sprintf
                      "hop %d reaches the to-many role '%s' in a scalar slot — a to-many step \
                       needs an aggregation, never a silent first pick"
                      i
                      (Option.value r.rol_naam ~default:"?")) ]
             else [])
           hops)

(* WF-053 over one attribute reference in an ordinary read position: with
   a path, the navigated read above; without one, the subject's own read, so
   its subjectType MUST be the subject type — a foreign subjectType there
   names a type the read never reaches. A position with no subject (a
   day-kind condition) anchors nothing. *)
let wf053_read ctx at ~subject ~objecttype:otn ~bindings =
  match (subject, bindings) with
  | None, _ -> []
  | Some st, Some hops -> wf053_attribuut_ref ctx at ~subject:st ~bindings:hops ~endpoint:(Some otn)
  | Some st, None when norm st <> norm otn ->
      [ v "WF-053" at
          (Printf.sprintf
             "an attribute reference without a path is the subject's own read, so its \
              subjectType is the subject type '%s' — '%s' names another type, which only a \
              navigation reaches"
             st otn) ]
  | Some _, None -> []

(* The position context threaded as [~body] through the expression walk
   (IR-SPEC ch. 2.11 / ch. 4.8 / ch. 4.10). It gates two independent things:
   whether an IfThenElse is admissible (everywhere but rule level) and whether
   the surrounding body is CLOSED (a function or lambda body denies attribute,
   dimension-cell, aggregation, temporal and variable reads, and conditions
   beyond Comparison/Predicate). A Let body admits IfThenElse without
   closing — its reads stay open (the leeg-barrier there is WF-077, not
   closure). *)
type body_pos =
  | Rule_pos              (* rule level: IfThenElse ill-formed, reads open *)
  | Closed_pos of string * string list
      (* function/lambda body: IfThenElse ok, reads closed (host wf id); the
         [string list] carries the binder names bound OUTSIDE this closed body
         (WF-064's closure clause): a closed body sees only its own parameters
         and inner bindings, so a BinderRef to an outer binder is rejected —
         the names are kept only to sharpen that diagnostic *)
  | Let_pos               (* Let body: IfThenElse ok, reads open *)

(* Does [e] read the binder [b] anywhere (ci), skipping subtrees that lexically
   rebind the name (a match arm, lambda/fold parameter, filter or nested let of
   the same name shadows it)? WF-076: a Let binder is visible only in its body,
   never in its own value. *)
let rec mentions_binder b (e : expr) : bool =
  let same n = norm n = norm b in
  let opt = function Some e -> mentions_binder b e | None -> false in
  match e with
  | Binder_ref { naam; _ } -> same naam
  | Literal _ | Parameter_ref _ | Variabele_ref _ | Constante _ | Bound_day _
  | Attribuut_ref _ | Dimensie_cel _ | Expr_onopgelost _ -> false
  | Binair { links; rechts; _ } -> opt links || opt rechts
  | Afgerond { basis; _ } -> opt basis
  | Begrensd { basis; minimum; maximum; _ } -> opt basis || opt minimum || opt maximum
  | Field_access { base; _ } -> mentions_binder b base
  | Struct_literal { velden; _ } -> List.exists (fun (_, e) -> mentions_binder b e) velden
  | List_literal { elements; _ } -> List.exists (mentions_binder b) elements
  | Construct { argument; _ } -> opt argument
  | Temporal_read { basis; datum; _ } -> mentions_binder b basis || mentions_binder b datum
  | Invoke { arguments; _ } -> List.exists (mentions_binder b) arguments
  | Tekst_reeks { delen; _ } ->
      List.exists (function Ingevoegd { expr = Some e } -> mentions_binder b e | _ -> false) delen
  | Functie { argumenten; leegwaarde; filter; _ } ->
      List.exists (mentions_binder b) argumenten || opt leegwaarde
      || (match filter with Some c -> mentions_binder_cond b c | None -> false)
  | If_then_else { condition; then_; else_; _ } ->
      mentions_binder_cond b condition || mentions_binder b then_ || mentions_binder b else_
  | List_length { list; _ } -> mentions_binder b list
  (* the binder-introducing forms: a rebinding of [b] shadows it below *)
  | Match { scrutinee; arms; _ } ->
      mentions_binder b scrutinee
      || List.exists
           (fun (_, arm_binder, arm_body) ->
             match arm_binder with
             | Some ab when same ab -> false
             | _ -> mentions_binder b arm_body)
           arms
  | Lambda { parameters; body = lbody; _ } ->
      if List.exists (fun (pn, _) -> same pn) parameters then false else mentions_binder b lbody
  | Filter_list { list; binder; condition; _ } ->
      mentions_binder b list || (if same binder then false else mentions_binder_cond b condition)
  | Fold { list; functie; init; _ } ->
      mentions_binder b list || mentions_binder b init || mentions_binder_fold_fn b functie
  | Map_list { list; functie; _ } -> mentions_binder b list || mentions_binder_fold_fn b functie
  | Let { value; binder; body; _ } ->
      mentions_binder b value || (if same binder then false else mentions_binder b body)
  | Instance_set_union { branches; _ } ->
      (* the union's own fields are symbolic (a type name, a role binding);
         only a branch filter carries expressions, and it is read against
         the contributor exactly as a fold's filter is *)
      List.exists
        (fun (br : instance_set_branch) ->
          match br.isb_filter with Some c -> mentions_binder_cond b c | None -> false)
        branches

and mentions_binder_fold_fn b = function
  | Fn_lambda { parameters; body = lbody; _ } ->
      if List.exists (fun (pn, _) -> norm pn = norm b) parameters then false
      else mentions_binder b lbody
  | Fn_name _ -> false

and mentions_binder_cond b (c : conditie) : bool =
  let opt = function Some e -> mentions_binder b e | None -> false in
  match c with
  | Vergelijking { links; rechts; _ } -> opt links || opt rechts
  | Predicaat { operand; _ } -> opt operand
  | Periode_conditie { vanaf; tot; _ } -> opt vanaf || opt tot
  | Samengesteld { onderdelen; _ } -> List.exists (mentions_binder_cond b) onderdelen
  | Kwantorvouw { kv_filter; kv_toets; _ } ->
      let sub = function Some c -> mentions_binder_cond b c | None -> false in
      sub kv_filter || sub kv_toets
  | Kenmerk_check _ | Role_membership _ | Regelversie _
  | Conditie_onopgelost _ -> false

(* WF-077 — the condition-operand discipline (IR-SPEC ch. 4.10). Everywhere an
   IfThenElse is admissible, its condition may READ only binders, parameters
   and literals; arithmetic combinators (Binary/Rounding/Clamp) over those
   are fine — the reads are what matter. An attribute/field/characteristic read
   must pass through a Let binding first: that is the leeg-barrier — at the
   READ under ch. 10.6's call-by-need — that makes ref's leeg propagation and
   clingo's underivability coincide by construction, keeping the NAF-free
   exact-complement branches honest. *)
let rec bound_only_expr (e : expr) : bool =
  let opt = function Some e -> bound_only_expr e | None -> true in
  match e with
  | Binder_ref _ | Parameter_ref _ | Literal _ -> true
  | Binair { links; rechts; _ } -> opt links && opt rechts
  | Afgerond { basis; _ } -> opt basis
  | Begrensd { basis; minimum; maximum; _ } -> opt basis && opt minimum && opt maximum
  | Tekst_reeks { delen; _ } ->
      List.for_all (function Ingevoegd { expr = Some e } -> bound_only_expr e | _ -> true) delen
  | _ -> false

let wf077_condition at (c : conditie) : violation list =
  let operand slot = function
    | Some e when not (bound_only_expr e) ->
        [ v "WF-077" slot
            "an IfThenElse condition operand reads more than a binder, parameter or literal — an \
             attribute, field or characteristic read must pass through a Let binding first" ]
    | _ -> []
  in
  let rec walk at (c : conditie) =
    match c with
    | Vergelijking { links; rechts; _ } ->
        operand (at ^ "/left") links @ operand (at ^ "/right") rechts
    | Predicaat { operand = o; _ } -> operand (at ^ "/operand") o
    | Periode_conditie { vanaf; tot; _ } -> operand (at ^ "/from") vanaf @ operand (at ^ "/to") tot
    | Samengesteld { onderdelen; _ } ->
        List.concat
          (List.mapi (fun i c -> walk (Printf.sprintf "%s/parts/%d" at i) c) onderdelen)
    | Kenmerk_check _ | Role_membership _ | Regelversie _
    | Kwantorvouw _ | Conditie_onopgelost _ ->
        [ v "WF-077" at
            "an IfThenElse condition reads a characteristic, role or channel — not admissible (only \
             binders, parameters and literals may be read; route state reads through a Let \
             binding)" ]
  in
  walk at c

(* WF-097 (maintainer decision 2026-09-09; widened to all three clips
   2026-09-10). A `gedurende` clip member's characteristic check resolves
   against exactly ONE subject and over the WHOLE span the clip ranges over,
   and every clip reader says so by construction: each matches `host` and
   `wholePeriod` away with `_`. A qualifier no reader honours must not be written.

   ONE helper for THREE clips, deliberately: the wire form is the same node in
   the same field (`Aggregation.filter`, `BuiltinCall.filter`) at the temporal total (ch. 3.6, WF-055),
   the pro-rata fold (ch. 3.7, WF-056) and the day count (ch. 3.8, WF-058).
   Three copies of this test could drift apart one careless edit at a time —
   which is exactly how the second and third clip came to be missed when the
   rule first landed on the total alone. `ref` and the ASP backend each hold
   the same fence in one shared place for the same reason.

   Measured on the pre-arm binaries, one field twisted at a time and nothing
   else changed: the total's three twists on `jutterswet-tegenwacht` were
   verifier-clean and reproduced the untwisted answer in 36 of 36 executor
   replays; the six twists on `anchor/jutterswet-borgdeel` (pro rata) and
   `anchor/jutterswet-dagentelling` (day count) were verifier-clean and
   reproduced it in 60 of 60. `negated: true` was indistinguishable from an
   absent qualifier in all of them, so both polarities are refused.

   The composite all clip recurses through the same arm, so a member of a
   composite is fenced exactly as a plain member is. *)
let wf097_clip_qualifications at (c : conditie) : violation list =
  let rec walk (c : conditie) =
    match c with
    | Kenmerk_check { kenmerk; eigenaar; periode; _ } ->
        let naam = Option.value kenmerk ~default:"?" in
        (match eigenaar with
         | Some e ->
             [ v "WF-097" at
                 ("host '" ^ e
                ^ "' on a gedurende clip member: the clip reads the SUBJECT's characteristic \
                   timeline, so a host here names a subject no reader consults") ]
         | None -> [])
        @ (match periode with
           | Some p ->
               [ v "WF-097" at
                   (Printf.sprintf
                      "wholePeriod qualifier (%s) on the gedurende clip member '%s': the clip \
                       already ranges over the whole timeline, so the qualifier — negated or not \
                       — names no reading the clip has"
                      (if p.gp_niet then "negated" else "affirmative") naam) ]
           | None -> [])
    | Samengesteld { onderdelen; _ } -> List.concat_map walk onderdelen
    | _ -> []
  in
  walk c

(* WF-111 — the time window's BOUND SHAPE (IR-SPEC ch. 4.6, maintainer
   decision 2026-09-23). The two shapes are the pair the v0 backends carry
   and all four executors execute (golden `jutterswet-premievenster`); the
   general date expression WF-042's typing admits is an OPEN wire decision,
   so it is refused here instead of by each executor at its own phase.
   "Own" is extensional, as everywhere (ch. 2.2): an attribute declared on a
   type the subject satisfies. The day-precision clause (maintainer decision
   2026-09-23) closes a measured split: an empty millisecond-precision attribute bound
   played in ref (a leeg bound never matches; ref refused only a FILLED
   instant) while the ASP backend refused it statically — ref ≠ clingo on a
   verifier-clean document. Ch. 3.10 speaks of civil days only. The declared datatype decides for an
   attribute and the annotation for a literal, which carries no other. *)
let wf111_bound ctx ~subject at side (e : expr) =
  let refuse what =
    [ v "WF-111" at
        (Printf.sprintf
           "the %s bound of a time window is %s — a bound is a date literal or an own scalar date \
            attribute of the subject; the general date expression is an OPEN wire decision"
           side what) ]
  in
  let instant =
    [ v "WF-111" at
        (Printf.sprintf
           "the %s bound of a time window is a millisecond-precision date: the window is a range \
            of civil days and no clause reads an instant bound"
           side) ]
  in
  match e with
  | Literal { typ = Some Datum_tijd; _ } -> instant
  | Literal _ -> []
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn; navigatie_bindingen = None; _ }
    -> (
      match subject with
      | None -> refuse ("the attribute '" ^ a ^ "' at a position with no subject to own it")
      | Some st when not (is_sub ctx ~sub:st ~super:otn) ->
          refuse
            (Printf.sprintf "the attribute '%s' of '%s', which the subject '%s' does not satisfy" a
               otn st)
      | Some _ -> (
          match objecttype ctx otn with
          | Some ot when attr_temporal ot a ->
              refuse ("the time-dependent attribute '" ^ a ^ "'")
          | Some ot when attr_of ot a = Some (Some Datum_tijd) -> instant
          | _ -> []))
  | Attribuut_ref { attribuut; _ } ->
      refuse ("a navigated read of '" ^ Option.value attribuut ~default:"?" ^ "'")
  | Parameter_ref { parameter; _ } ->
      refuse ("the parameter '" ^ Option.value parameter ~default:"?" ^ "'")
  | _ -> refuse "a computed date expression"

(* WF-112 — the time window's POSITION (maintainer decision 2026-09-23): never
   under none or a counting quantifier. The walk is deep — a window inside an
   all member of a none composite is still counted or complemented by the
   outer one — and it enters a fold's filter and test, where the same
   quantifiers range over instances. It stops at nothing else, because no
   other condition node holds a condition. *)
let rec cond_has_window (c : conditie) =
  match c with
  | Periode_conditie _ -> true
  | Samengesteld { onderdelen; _ } -> List.exists cond_has_window onderdelen
  | Kwantorvouw { kv_filter; kv_toets; _ } ->
      List.exists cond_has_window (List.filter_map Fun.id [ kv_filter; kv_toets ])
  | Vergelijking _ | Predicaat _ | Kenmerk_check _ | Role_membership _ | Regelversie _
  | Conditie_onopgelost _ ->
      false

let wf112_position at (kwantor : kwantor) (parts : conditie list) =
  match kwantor with
  | Alle | De -> []
  | Geen | Ten_minste | Ten_hoogste | Precies ->
      if List.exists cond_has_window parts then
        [ v "WF-112" at
            "a time window under none or a counting quantifier: the window gates the RULE, and no \
             reading is specified for a verdict that is counted or complemented" ]
      else []

let rec walk_expr ctx ~binders ~subject ?(body = Rule_pos) ?(union_ok = false) at (e : expr) :
    violation list =
  (* [union_ok] is WF-110 (a) carried DOWN one level: the instance-set union
     is admissible at exactly one position, and only the parent knows the
     position. It is an optional argument with a false default, so it reaches
     the node the count hands over and no descendant of it — a union nested
     inside a branch filter is at a wrong position again, and says so. *)
  let sub p = at ^ "/" ^ p in
  (* the static type of an operand: a reference's declaration, every other
     value's annotation (RUNTIME-SEMANTICS ch. 2.10) *)
  let ty x = expr_type ctx ~binders x in
  let typ_checks =
    match e with
    (* A reference carries no type of its own since L2 (B13): its type is
       its declaration's, and the declaration is checked where it is
       declared. A Lambda is never a value and the instance-set union
       denotes INSTANCES, so neither carries one either — their own
       position disciplines are WF-069 and WF-110 (a). An Unresolved
       without a type is a value of unknown type, WF-044's. *)
    | Attribuut_ref _ | Dimensie_cel _ | Parameter_ref _ | Variabele_ref _ | Binder_ref _
    | Lambda _ | Instance_set_union _
    | Expr_onopgelost { typ = None; _ } ->
        []
    | _ -> (
        match annotation e with
        | Some d ->
            check_number_form (sub "type") d
            @ check_datatype_units ~stelsels:(stelsel_namen ctx) (sub "type") d
            @ enum_use_site ctx (sub "type") d
            @ struct_use_site ctx (sub "type") d
        | None ->
            (* every other value node carries its annotation: a derived
               position its derived type, a literal or a composite value its
               own (WF-034) *)
            [ v "WF-034" (sub "type") "value node without a static type" ])
  in
  typ_checks
  (* WF-091 at a COMPOSITE position: the annotation against the expectation
     its operands DERIVE (ch. 4.1, "derived-type identity"). Beside the type
     prologue, for the same reason: this is a claim about the node's own
     [type], not about its children, and it holds wherever the walk reaches
     a covered node kind. *)
  @ composite_identity ctx ~binders at e
  @
  (let closed =
     (* inside a closed body: the reads that break closure short-circuit,
        citing the host rule (WF-067 for a function body, WF-069 for a
        lambda body). A Let body (Let_pos) is NOT closed — its reads
        stay open; rule level (Rule_pos) never closes either. *)
     match body with
     | Rule_pos | Let_pos -> None
     | Closed_pos (wf, _) -> (
         let deny phrase =
           Some [ v wf at (phrase ^ " is not admissible inside a closed function or lambda body") ]
         in
         match e with
         | Attribuut_ref _ -> deny "an attribute or characteristic read"
         | Dimensie_cel _ -> deny "a dimension-cell read"
         | Functie { kind; _ } when List.mem kind aggregation_kinds -> deny "an aggregation"
         | Functie _ -> deny "a builtin call"
         | Temporal_read _ -> deny "a temporal read"
         | Variabele_ref _ -> deny "a variable read"
         | _ -> None)
   in
   match closed with
   | Some vs -> vs
   | None ->
   match e with
  | Literal { variant; typ; _ } -> (
      (* Since L4 the value is typed (delta B22): the spelling rules key on
         the VARIANT the value is written in, and WF-114 is the one rule that
         crosses the variant against the type — a literal without a type is
         WF-034's above, with nothing to cross *)
      let vat = sub "value" in
      (match typ with
      | Some t -> (
          match literal_variant_refusal variant t with
          | Some sentence -> [ v "WF-114" vat sentence ]
          | None -> [])
      | None -> [])
      @
      match (variant, typ) with
      | Lv_number w, _ when number_lexeme_refusal w <> None ->
          [ v "WF-081" vat (Option.get (number_lexeme_refusal w)) ]
      | Lv_date w, _ when date_lexeme_refusal w <> None ->
          [ v "WF-084" vat (Option.get (date_lexeme_refusal w)) ]
      | Lv_date_time w, _ when date_time_lexeme_refusal w <> None ->
          [ v "WF-095" vat (Option.get (date_time_lexeme_refusal w)) ]
      | Lv_enumeration w, Some (Enumeratie { waarden; _ }) when not (mem_ci (some_names waarden) w) ->
          [ v "WF-039" vat ("enumeration label '" ^ w ^ "' is not in the declared domain") ]
      | Lv_number w, Some (Numeriek { teken = Some t; _ }) -> (
          match t with
          | Niet_negatief when lexeme_negative w ->
              [ v "WF-039" vat ("negative literal '" ^ w ^ "' under a non-negative sign restriction") ]
          | Positief when lexeme_negative w || lexeme_zero w ->
              [ v "WF-039" vat ("literal '" ^ w ^ "' under a positive sign restriction") ]
          | Negatief when not (lexeme_negative w) ->
              [ v "WF-039" vat ("literal '" ^ w ^ "' under a negative sign restriction") ]
          | _ -> [])
      | _ -> [])
  | Tekst_reeks { delen; _ } ->
      List.concat
        (List.mapi
           (fun i d ->
             match d with
             | Letterlijk _ -> []
             | Ingevoegd { expr = Some e } ->
                 walk_expr ctx ~binders ~subject ~body (Printf.sprintf "%s/parts/%d/expr" at i) e
             | Ingevoegd { expr = None } -> [])
           delen)
  | Binair { op; links; rechts; _ } ->
      let lt = Option.bind links ty and rt = Option.bind rechts ty in
      let numeric_required =
        match op with
        | Maal | Gedeeld_door | Gedeeld_door_abs | Percentage_van | Verminderd_met
        | Tot_de_macht -> true
        | Plus | Min -> false (* may be date +/- duration *)
      in
      (if numeric_required then
         List.concat_map
           (fun (side, t) ->
             match t with
             | Some d when not (is_number_like d) ->
                 [ v "WF-035" (sub side) ("operand of a numeric operator is " ^ datatype_kind d) ]
             | _ -> [])
           [ ("left", lt); ("right", rt) ]
       else [])
      @ (match op, lt, rt with
         | Tot_de_macht, Some l, Some r
           when unit_of l <> None || unit_of r <> None ->
             [ v "WF-035" at "power operands must be unitless" ]
         | (Plus | Min | Verminderd_met), Some l, Some r when is_number_like l && is_number_like r ->
             (* reducedBy belongs to the additive family (ch. 1.2a);
                ref restates its right operand like plus/min — a hunted
                gap: the compatibility clause skipped it entirely *)
             if not (units_compatible_adopting links rechts (unit_of l) (unit_of r)) then
               [ v "WF-035" at "additive operands have incompatible units" ]
             else []
         | _ -> [])
      (* the WF-035 date±duration clause (RUNTIME-SEMANTICS ch. 3.8): the
         date stands on the LEFT of plus and min — v0's typing opens its
         date branch only for a date-left operand, so duration ± date and
         date ± date are ill-formed; the duration operand is a number whose
         unit spelling is a recognised time unit or a declared day kind; a
         sub-day unit demands a millisecond-precision date operand *)
      @ (let duration_side side t date_t =
           match t with
           | None -> []
           | Some d when not (is_number_like d) ->
               [ v "WF-035" (sub side)
                   ("date \xc2\xb1 duration: the duration operand is " ^ datatype_kind d
                    ^ ", must be a number carrying a time unit") ]
           | Some d -> (
               match unit_of d with
               | None ->
                   [ v "WF-035" (sub side)
                       "date \xc2\xb1 duration: the duration operand carries no time unit" ]
               | Some u -> (
                   match time_unit_of_eenheid u with
                   | Some tu ->
                       if time_unit_sub_day tu && date_t = Datum_in_dagen then
                         [ v "WF-035" at
                             "sub-day time unit on a day-precision date (millisecond \
                              precision required)" ]
                       else []
                   | None ->
                       if day_kind_unit ctx u then []
                       else
                         [ v "WF-035" (sub side)
                             ("'" ^ Option.value u.afkorting ~default:"?"
                              ^ "' is not a recognised time unit or declared day kind") ]))
         in
         match op, lt, rt with
         | (Plus | Min), Some l, Some r when is_date l && is_date r ->
             [ v "WF-035" at
                 "date \xc2\xb1 date is ill-formed (date arithmetic takes a date and a \
                  duration)" ]
         | Min, _, Some r when is_date r ->
             [ v "WF-035" at
                 "duration \xe2\x88\x92 date is ill-formed (subtraction admits the date on \
                  the left only)" ]
         | Plus, _, Some r when is_date r ->
             [ v "WF-035" at
                 "duration + date is ill-formed (the date stands on the left; v0 does \
                  not commute)" ]
         | (Plus | Min), Some l, _ when is_date l -> duration_side "right" rt l
         | _ -> [])
      @ (match links with Some e -> walk_expr ctx ~binders ~subject ~body (sub "left") e | None -> [])
      @ (match rechts with Some e -> walk_expr ctx ~binders ~subject ~body (sub "right") e | None -> [])
  | Functie f ->
      let arity = List.length f.argumenten in
      (* with a range the single argument is the set source; without one it
         is the subject's own read (the loader admits that under total
         alone) *)
      let ranged = f.bron_rol <> None || f.bron_pad <> None in
      (* the fold SOURCE position (ch. 4.3): the single argument of an
         aggregation carrying an elementType. Every other operand — a listed
         value fold's, a builtin's — is an ordinary read *)
      let source_position =
        arity = 1 && f.element_objecttype <> None && List.mem f.kind aggregation_kinds
      in
      (* a fold that names its population and hands no source (WF-037, the
         arm below). Computed once because the filled count's arity and
         set-source arms and WF-047's range arm step aside on it, so that the
         node is reported once, by that arm *)
      let sourceless =
        match (f.element_objecttype, f.argumenten) with
        | Some el, [] -> Some (el, `No_operand)
        | Some el, [ List_literal _ ] when f.kind = Aantal || f.kind = Aantal_gevulde ->
            Some (el, `List_literal)
        | _ -> None
      in
      (* WF-037: facet validity per kind *)
      (if f.leegwaarde <> None && f.kind <> Som then
         [ v "WF-037" at "empty-value fallback on a non-sum aggregation" ]
       else [])
      @ (if f.dimensie_selectie <> None && not (List.mem f.kind [ Som; Minimum; Maximum ]) then
           [ v "WF-037" at "dimension selection on a kind other than sum/min/max" ]
         else [])
      @ (match f.granulariteit, f.kind with
         | Some _, (Tijdsevenredig_deel | Aantal_dagen_in) -> []
         | Some _, _ -> [ v "WF-037" at "granularity facet on a non-bucketing kind" ]
         | None, (Tijdsevenredig_deel | Aantal_dagen_in) ->
             [ v "WF-037" at "bucketing builtin without a granularity" ]
         | None, _ -> [])
      @ (if List.mem f.kind scalar_one_arg_kinds && arity <> 1 then
           [ v "WF-037" at (Printf.sprintf "builtin arity %d, expected 1" arity) ]
         else if List.mem f.kind [ Tijdsduur; Absolute_tijdsduur ] && arity <> 2 then
           [ v "WF-037" at (Printf.sprintf "builtin arity %d, expected 2" arity) ]
         else [])
      (* This clause is what DEFINES the listed value fold: no element type,
         no dimension selection, two or more operands. Both counts step aside
         from it because v0 spells a count with ONE argument, its fold source;
         their own arity law lives in the two arms below, and removing either
         exclusion here would state it twice, in the wrong words. *)
      @ (if
           List.mem f.kind aggregation_kinds && f.element_objecttype = None
           && f.dimensie_selectie = None && arity < 2 && f.kind <> Aantal
           && f.kind <> Aantal_gevulde
         then [ v "WF-037" at "value fold with fewer than two operands" ]
         else [])
      (* WF-037, the count's arity (IR-SPEC ch. 4.3, semantics
         RUNTIME-SEMANTICS ch. 3.4a; maintainer decision 2026-09-20). The
         exclusion above left the listed form of a COUNT admissible, and a
         count has none: the source language defines it over instances and
         spells no operand list for it, so there is nothing to mirror. One
         PRODUCER wrote it anyway — measured 2026-09-20, the RegelSpraak
         frontend's own grammar carries an operand list at this rule, a
         widening of the one-operand source production, and three sentences
         emitted through it unblocked. So this rule cuts a live route rather
         than closing a door nothing used. What it was, measured: verifier-clean with
         exactly ONE executor answering — `ref` counted the FILLED operands,
         which is the reading the set-source decision of 2026-09-19 took off
         the wire under this very kind, while clingo and both Drools branches
         refused the node.

         The arm deliberately does NOT read elementType. A count that
         carries an element type AND an operand list is the same listed form
         wearing a second claim; measured, it too was verifier-clean, and
         there all THREE executors refused it.

         What it does not reach: a count with fewer than two operands and no
         element type. Arity 0 and arity 1 are no listed form — the clause
         above is what makes ≥ 2 the listed shape — so they are a different
         defect, a fold without a source, and the next arm carries it. *)
      (* WF-110 (c), the count's own source beside a union (ch. 4.3,
         maintainer decision 2026-09-20). The union's selections carry the
         population and the relation, one per selection, so a source on the
         COUNT is a second claim on one node — the ground the setbron
         decision of 2026-09-19 drew, applied at the node that consumes the
         union. Reported here, at the node it is about, which is also what
         keeps the union argument itself from being reported a second time
         for standing in the right position under a wrong node. *)
      @ (match f.argumenten with
         | [ Instance_set_union _ ] when f.kind = Aantal ->
             let named =
               (if f.element_objecttype <> None then [ "elementType" ] else [])
               @ (if f.bron_rol <> None then [ "sourceRole" ] else [])
               @ (if f.bron_pad <> None then [ "sourcePath" ] else [])
               @ if f.filter <> None then [ "filter" ] else []
             in
             if named = [] then []
             else
               [ v "WF-110" at
                   (Printf.sprintf
                      "a count over an instance-set union names no source of its own, and this \
                       node carries %s: the union's selections carry the population, the \
                       relation and the subselection, one set per selection, so a source here \
                       is a second claim on one node (ch. 4.3)"
                      (String.concat " + " named)) ]
         | _ -> [])
      @ (if f.kind = Aantal && arity >= 2 then
           [ v "WF-037" at
               (Printf.sprintf
                  "a count folds INSTANCES and has no LISTED form, and this node carries %d \
                   operands: the source language spells a count over a subject expression alone \
                   (ch. 4.3), so there is no articulated operand list to admit — the only reading \
                   an operand list leaves a count is the number of FILLED operands, and that \
                   meaning belongs to the filled count over its set source, never to this kind"
                  arity) ]
         else [])
      (* WF-037, the count's source (IR-SPEC ch. 4.3, semantics
         RUNTIME-SEMANTICS ch. 3.4a; maintainer decision 2026-09-23). A count
         of ONE operand without an element type, or of none, is a fold with
         no source: nothing names the instances it counts. Measured before
         this arm: verifier-clean, `ref` answered — 0 or 1 on one own read,
         0 on none, and 1 on a single ListLiteral whether that list carried
         two elements or NONE, because it counted the operand and not its
         contents — while clingo and both Drools branches refused. One sentence
         for both arities, worded as the filled count's element-type arm below,
         so the two counts carry one law at one position.

         The union is the one exit, and not a loophole: WF-110 (c) FORBIDS an
         element type beside an instance-set union, whose selections each carry
         their own, so there the source is present and sits one node down.
         Arity ≥ 2 is the listed-form arm above and is not reported twice. *)
      @ (match f.kind, f.element_objecttype, f.argumenten with
         | Aantal, None, [ Instance_set_union _ ] -> []
         | Aantal, None, ([] | [ _ ]) ->
             [ v "WF-037" at
                 "the count is an INSTANCE fold and MUST carry an element type: without one its \
                  argument is no fold source at all — and with no argument there is none — so \
                  the count has no contributors to count, only its operand, whatever that \
                  operand holds (ch. 4.3)" ]
         | _ -> [])
      (* WF-037, the fold that names its population and hands no source
         (IR-SPEC ch. 4.3; the twin of the count's source arm above, where
         the element type is missing and here the operand). An elementType
         makes the node an instance fold, and an instance fold reads its
         contributors from one source argument. Measured before this arm:
         with no argument the fold was verifier-clean under every kind but
         the filled count, whose arity arm refused it on the listed form's
         ground (the listed-form clause above reads only folds without an
         element type, the fold-source rules only a fold of one argument),
         while ref and clingo refused it with their bottom sentences and both
         Drools branches refused the rule. A single ListLiteral under a count
         was refused on WF-047 alone, for its binding and never for its
         argument. Under a value fold that list literal was already refused
         on its type (WF-091, beside WF-038 or WF-047), so the arm takes it
         under the two counts only. *)
      @ (match sourceless with
         | Some (el, `No_operand) ->
             [ v "WF-037" at
                 (Printf.sprintf
                    "a %s aggregation carries elementType \"%s\" but no source operand: an \
                     instance fold ranges over a written source"
                    (Spelling.encode Spelling.aggregation_kind f.kind)
                    el) ]
         | Some (el, `List_literal) ->
             [ v "WF-037" at
                 (Printf.sprintf
                    "a %s aggregation carries elementType \"%s\" and a ListLiteral operand: a \
                     listed value is no source of instances"
                    (Spelling.encode Spelling.aggregation_kind f.kind)
                    el) ]
         | None -> [])
      (* WF-037, the filled count's shape (IR-SPEC ch. 4.3, semantics
         RUNTIME-SEMANTICS ch. 3.4c). The law is a form demanded rather than
         refused: exactly one argument, and that one the set source. The
         arms return in order — arity, then the element type, then the
         source — because each later one presupposes what the earlier one
         established. The set source's range is the loader's since L2 (the
         schema's condition, B12): a filled count with an elementType
         and one argument carries sourceRole or sourcePath, so what is left here is
         a source that is no attribute reference at all. A filled count that
         names its population and hands no source is the arm above's: the
         arity sentence reads its ground from the listed form, which an empty
         argument list is not. *)
      @ (if f.kind <> Aantal_gevulde || sourceless <> None then []
         else if arity <> 1 then
           [ v "WF-037" at
               (Printf.sprintf
                  "the filled count takes exactly ONE argument, its set source, and this node \
                   carries %d: there is no listed form of this kind — the source language \
                   spells it nowhere, so there is no articulated operand list to admit \
                   (ch. 4.3)"
                  arity) ]
         else
           match (f.element_objecttype, f.argumenten) with
           | None, _ ->
               [ v "WF-037" at
                   "the filled count is an INSTANCE fold and MUST carry an element type: \
                    without one its argument is no fold source at all, and the count has no \
                    contributors whose values it could test for presence (ch. 4.3)" ]
           | Some _, [ Attribuut_ref _ ] -> []
           | Some _, _ ->
               [ v "WF-037" at
                   "the filled count's single argument MUST be its set source, an attribute \
                    reference read on each contributor: this kind reads a VALUE per \
                    contributor and tests that value for presence (ch. 4.3)" ])
      (* WF-034: references *)
      @ (match f.element_objecttype with
         | Some t when objecttype ctx t = None ->
             [ v "WF-034" at ("aggregation element type '" ^ t ^ "' is not declared") ]
         | _ -> [])
      (* WF-038: operand typing slices *)
      @ (match f.kind, f.argumenten with
         | Eerste_paasdag, [ a ] -> (
             match ty a with
             | Some (Numeriek { geheel = true; eenheid = None; _ }) -> []
             | Some d ->
                 [ v "WF-038" at ("easterSunday operand is " ^ datatype_kind d
                                  ^ ", must be a whole unitless number") ]
             | None -> [])
         | (Uur_uit | Minuut_uit | Seconde_uit | Milliseconde_uit), [ a ] -> (
             match ty a with
             | Some Datum_tijd | None -> []
             | Some d ->
                 [ v "WF-038" at ("sub-day extractor operand is " ^ datatype_kind d
                                  ^ ", must be a millisecond-precision date") ])
         | (Jaar_uit | Maand_uit | Dag_uit), [ a ] -> (
             (* the day-precision extractors read civil components (ch. 3.8
                of RUNTIME-SEMANTICS): the operand must be a date — either
                precision, mirroring the v0 field read *)
             match ty a with
             | Some d when not (is_date d) ->
                 [ v "WF-038" at ("day-precision extractor operand is " ^ datatype_kind d
                                  ^ ", must be a date") ]
             | _ -> [])
         | (Tijdsduur | Absolute_tijdsduur), [ a; b ] ->
             (* the WF-038 duration clause (RUNTIME-SEMANTICS ch. 3.8): both
                operands dates of one precision; the node type carries the
                counted unit — a recognised time-unit or declared day-kind
                spelling; a sub-day unit demands millisecond-precision
                operands *)
             let ta = ty a and tb = ty b in
             let operand side t =
               match t with
               | Some d when not (is_date d) ->
                   [ v "WF-038" (sub side)
                       ("duration operand is " ^ datatype_kind d ^ ", must be a date") ]
               | _ -> []
             in
             operand "arguments/0" ta
             @ operand "arguments/1" tb
             @ (match ta, tb with
                | Some da, Some db when is_date da && is_date db && da <> db ->
                    [ v "WF-038" at
                        "duration operands mix date precisions (both must share one \
                         precision)" ]
                | _ -> [])
             @ (match f.typ with
                | None ->
                    (* the counted unit lives on the node type: an absent
                       type cannot carry it — without this arm the wire is
                       verifier-clean and faults only at evaluation *)
                    [ v "WF-038" at
                        "duration without a node type (the counted unit is the node \
                         type's unit)" ]
                | Some t -> (
                    match unit_of t with
                    | None ->
                        [ v "WF-038" at
                            "duration without a recognised time unit on its node type" ]
                    | Some u -> (
                        match time_unit_of_eenheid u with
                        | Some tu ->
                            if time_unit_sub_day tu
                               && (ta = Some Datum_in_dagen || tb = Some Datum_in_dagen)
                            then
                              [ v "WF-038" at
                                  "sub-day duration unit on day-precision operands \
                                   (millisecond precision required)" ]
                            else []
                        | None ->
                            if day_kind_unit ctx u then []
                            else
                              [ v "WF-038" at
                                  ("'" ^ Option.value u.afkorting ~default:"?"
                                   ^ "' is not a recognised time unit or declared day kind") ])))
         | _ -> [])
      (* WF-038, the ordering-fold operand clause (IR-SPEC ch. 4.6,
         semantics `RUNTIME-SEMANTICS.md` ch. 1.2): first/last/
         minimum/maximum keep the least or greatest CONTRIBUTOR, so every
         operand must come from one family an order actually runs over.
         Enumeration, SumType, StructType, text and boolean are
         equality-only (the ch. 2.4 orderability paragraph); numbers and
         dates have no common order between them; and the two DATE
         REPRESENTATIONS are two families for the same reason — a
         day-precision date names a whole day, a millisecond-precision
         one a point inside a day, and ordering the two is what ref
         faults on (`unordered comparison: 2026-06-21 vs @…ms`). Hence
         the family tag is per representation, not one `Date` bucket.

         The rule reads `arguments` and nothing else, so it does not
         turn on the wire form: an instance fold spells its contributors
         as one set-valued argument whose type is the element attribute's
         datatype, an articulated fold as two or more listed ones — and 4
         of the corpus's 14 articulated ordering folds carry
         `elementType` besides (v0 stamps the tag vestigially), so
         form is no dichotomy a rule could have keyed on.

         An operand whose type nothing declares DEFERS: a reference that
         does not resolve (its own WF-034 arm reports it), an Unresolved
         of unknown type, and a count source, whose name is a role and no
         attribute — under an ordering fold that source is WF-093's. *)
      @ (match List.assoc_opt f.kind passthrough_folds with
         | None -> []
         | Some kn ->
             let family d =
               if is_number_like d then Some `Num
               else
                 match d with
                 | Datum_in_dagen -> Some `Date_day
                 | Datum_tijd -> Some `Date_ms
                 | _ -> None
             in
             let typed = List.mapi (fun i a -> (i, ty a)) f.argumenten in
             List.concat_map
               (fun (i, t) ->
                 match t with
                 | None -> []
                 | Some d when family d = None ->
                     (* a bare "list" names no element, and the element is
                        what carries the unorderability here *)
                     let shown =
                       match d with List_type _ -> datatype_detail d | _ -> datatype_kind d
                     in
                     [ v "WF-038"
                         (Printf.sprintf "%s/arguments/%d" at i)
                         ("operand of the " ^ kn ^ " ordering fold is " ^ shown
                        ^ ", must be a number or a date") ]
                 | Some _ -> [])
               typed
             @ (let fams =
                  List.filter_map
                    (fun (_, t) -> match t with Some d -> family d | None -> None)
                    typed
                in
                match fams with
                | f0 :: rest when List.exists (fun x -> x <> f0) rest ->
                    (* two crossings, two different defects: a precision mix
                       is not a number-versus-date mix and must not be
                       reported as one *)
                    let dated = function `Date_day | `Date_ms -> true | `Num -> false in
                    if List.for_all dated fams then
                      [ v "WF-038" at
                          (kn ^ " orders dates of two precisions (day and millisecond), \
                                which share no order") ]
                    else
                      [ v "WF-038" at
                          (kn ^ " orders operands of two comparable families, numbers and \
                                dates, which share no ordering") ]
                | _ -> []))
      @ (match f.leegwaarde with
         | Some lw -> (
             (match ty lw with
              | Some d when not (is_number_like d) ->
                  [ v "WF-038" (sub "emptyFallback")
                      ("sum fallback is " ^ datatype_kind d ^ ", must be numeric") ]
              | _ -> [])
             @ walk_expr ctx ~binders ~subject (sub "emptyFallback") lw)
         | None -> [])
      (* WF-040: dimension selection labels *)
      @ (match f.dimensie_selectie with
         | Some ds -> (
             match ds.ds_dimensie with
             | Some dn -> (
                 match find_ci ctx.doc.dimensies dn with
                 | None ->
                     [ v "WF-040" (sub "dimensionSelection")
                         ("selection over undeclared dimension '" ^ dn ^ "'") ]
                 | Some dim ->
                     List.concat_map
                       (fun l ->
                         match l with
                         | Some l when not (mem_ci (some_names dim.dim_waarden) l) ->
                             [ v "WF-040" (sub "dimensionSelection")
                                 ("label '" ^ l ^ "' is not declared on dimension '" ^ dn ^ "'") ]
                         | _ -> [])
                       ds.ds_labels)
             | None -> [])
         | None -> [])
      @ (match f.filter with
         | Some c ->
             (* the die/dat subselection evaluates per contributor: the
                ELEMENT is the filter's subject, never the outer fold
                subject (the gedurende clip's elementType is the subject
                itself, so this switch is uniform) *)
             let fsubject =
               match f.element_objecttype with Some el -> Some el | None -> subject
             in
             walk_cond ctx ~binders ~subject:fsubject (sub "filter") c
         | None -> [])
      (* WF-089/WF-090 (domain identity, ch. 2.4), aggregation position: an
         element-passing fold's own type against each operand's type. The
         operand's type is itself bound to the declaration it reads (a
         reference's type IS its declaration's, a composite is crossed
         against its own operands), so crossing the node against its
         operand chains the node to that declaration — without this the fold is a laundering step: its
         value comes from a Kust attribute and its type says Vaarwater.

         The DOMAIN facet only, deliberately, where every other arm crosses
         the whole type (WF-091): a fold node's type is not a copy of one
         declaration but a DERIVED type over the operand list — the
         frontend takes minimum/maximum decimals as the least operand
         precision, wholeness as the conjunction, sign and unit from
         operand 0 — so identity against EVERY operand is unsatisfiable the
         moment two operands differ, which the blessed "het minimum van het
         gewicht en 0" form (ch. 4.6, a unitless literal beside a kg
         attribute) does by construction. A domain, by contrast, is handed
         through unchanged: the winning contributor's label IS the node's
         label, whichever contributor wins. *)
      @ (match List.assoc_opt f.kind passthrough_folds with
         | None -> []
         | Some kn ->
             List.concat
               (List.mapi
                  (fun i a ->
                    use_site_domain (sub "type")
                      ~source:(Printf.sprintf "operand %d" i)
                      ~what:(Printf.sprintf "the %s aggregation" kn)
                      ~decl:(ty a) ~typ:f.typ)
                  f.argumenten))
      (* WF-083 (c), the element type of the subject's own read (IR-SPEC
         ch. 4.3, L2 fix round). A total without a range is the subject's
         own read: it totals the subject's own timeline, so the type it
         folds over IS the subject type. Before this arm a foreign type in
         the field was verifier-clean and moved no answer (measured by the
         lens on the union of the L2 card): no reader checked it. Since
         the fix round ref, the ASP backend and the Drools loader refuse it
         with this sentence. Equality in the WF-001 form, the comparison
         the source's own anchor makes in [aggregation_source_check]. *)
      @ (match (f.kind, f.element_objecttype, subject) with
         | Totaal, Some el, Some st when source_position && (not ranged) && norm el <> norm st ->
             [ v "WF-083" (sub "elementType")
                 (Printf.sprintf
                    "the elementType '%s' of a total without a range is not the fold subject \
                     '%s': without a range the fold is the subject's own read, so its \
                     elementType must name the subject type"
                    el st) ]
         | _ -> [])
      (* the operands: the source position is judged by the fold-source rules
         ([aggregation_source_check]), every other operand is walked as the
         ordinary read it is — with two or more arguments the node is the
         listed value fold (§5.8.3), whose operands are read one by one
         against the rule subject and navigate like any other read, and a
         builtin has no source at all *)
      @ List.concat
          (List.mapi
             (fun i a ->
               let p = Printf.sprintf "%s/arguments/%d" at i in
               match (a, f.element_objecttype) with
               | Attribuut_ref { attribuut; objecttype = None; _ }, Some _ when source_position ->
                   [ v "WF-034" p
                       ("attribute reference '"
                       ^ Option.value attribuut ~default:"?"
                       ^ "' without a subjectType: it resolves against no declaration") ]
               | Attribuut_ref { attribuut = None; objecttype = Some otn; _ }, Some _
                 when source_position ->
                   [ v "WF-034" p
                       ("attribute reference on '" ^ otn
                      ^ "' without an attribute name: it resolves against no declaration") ]
               | Attribuut_ref _, Some el when source_position ->
                   aggregation_source_check ctx p ~element_type:el ~subject ~kind:f.kind ~ranged a
               | _ ->
                   (* the union's admissible position (WF-110 (a)): the SINGLE
                      argument of a count. Keyed on the kind and the arity and
                      NOT on elementType, so a count that carries a source
                      of its own beside the union is reported once — by clause
                      (c), which is what is wrong with it — instead of twice. *)
                   walk_expr ctx ~binders ~subject ~union_ok:(f.kind = Aantal && arity = 1) p a)
             f.argumenten)
      (* WF-055: the gedurende clip — a filter on the temporal total is a
         timeline operation; its condition is an own temporal
         characteristic check or a per-segment comparison of an own
         temporal attribute against a scalar bound *)
      @ (match (f.kind, f.argumenten, f.filter) with
         | ( Totaal,
             [ Attribuut_ref
                 { attribuut = Some a; objecttype = Some aot; navigatie_bindingen = None; _ } ],
             Some c )
           when (not ranged)
                && (match objecttype ctx aot with
                   | Some ot -> attr_temporal ot a
                   | None -> false) -> (
             let temporal_char k =
               match objecttype ctx aot with
               | Some ot ->
                   List.exists (fun (n, _) -> norm n = norm k) ot.kenmerk_granulariteit
               | None -> false
             in
             let rec clip_cond_ok c =
               match c with
               | Kenmerk_check { kenmerk = Some k; navigatie_binding = None; _ } ->
                   if temporal_char k then []
                   else
                     [ v "WF-055" (sub "filter")
                         ("gedurende over '" ^ k
                        ^ "', which is not a temporal characteristic of '" ^ aot ^ "'") ]
               | Vergelijking
                   { links = Some (Attribuut_ref
                       { attribuut = Some b; objecttype = Some botn; navigatie_bindingen = None; _ });
                     rechts = Some _; periode = None; _ }
                 when norm botn = norm aot
                      && (match objecttype ctx botn with
                         | Some ot -> attr_temporal ot b
                         | None -> false) ->
                   []
               | Samengesteld { kwantor = Alle; onderdelen = (_ :: _) as onderdelen; _ } ->
                   (* the FLAT composite all clip (ch. 3.6): members are
                      own-temporal-characteristic checks only. The v0
                      boundary, stated precisely: v0 partitions STATIC
                      comparison members out as value-side guards before
                      its clip runs, and markers temporal-attribute
                      comparisons and nested composite bullets as
                      Unresolved -- v1 narrows composite members to the
                      characteristic checks both executors carry (the
                      static-guard form and the nested bullet are
                      recorded loud flanks, RUNTIME-SEMANTICS ch. 3.6) *)
                   List.concat_map
                     (fun part ->
                       match part with
                       | Kenmerk_check _ -> clip_cond_ok part
                       | _ ->
                           [ v "WF-055" (sub "filter")
                               "a composite gedurende member is an own temporal \
                                characteristic check (v0 partitions static comparison \
                                members into value-side guards and markers nested \
                                bullets; v1 narrows composite members to characteristic \
                                checks -- a recorded narrowing)" ])
                     onderdelen
               | _ ->
                   [ v "WF-055" (sub "filter")
                       "the gedurende condition is an own temporal characteristic check, a \
                        per-segment comparison of an own temporal attribute against a scalar \
                        bound, or an all composite of characteristic checks" ]
             in
             clip_cond_ok c @ wf097_clip_qualifications (sub "filter") c)
         | _ -> [])
      (* WF-056: the pro-rata fold (timeProportionalPart) MUST carry a
         gedurende condition, and its base MUST be a temporal source as
         coarse as or coarser than the bucket *)
      (* WF-097 at the two KIND-KEYED clips — the pro-rata fold (ch. 3.7) and
         the day count (ch. 3.8). The third clip, the temporal total's, is
         identified by SHAPE rather than by kind (its filter is a clip only
         under WF-055's guard; the same field otherwise carries the die/dat
         instance subselection, which is NOT a clip and is deliberately not
         fenced), so it calls the same helper from inside that guard. One
         helper, two call sites here — this kind-keyed branch covers both
         folds at once — and three clips behind them, with no second copy
         of the test to drift. *)
      @ (match (f.kind, f.filter) with
         | (Tijdsevenredig_deel | Aantal_dagen_in), Some c ->
             wf097_clip_qualifications (sub "filter") c
         | _ -> [])
      @ (if f.kind <> Tijdsevenredig_deel then []
         else
           (match f.filter with
           | None -> [ v "WF-056" at "timeProportionalPart without a gedurende condition" ]
           | Some (Kenmerk_check { kenmerk = Some k; navigatie_binding = None; _ }) -> (
               (* the gedurende characteristic MUST be temporal — a
                  non-temporal one has no periods to prorate over (a
                  hunted gap: ref faulted, clingo silently emptied) *)
               match f.argumenten with
               | [ Attribuut_ref { objecttype = Some bot; _ } ] -> (
                   match objecttype ctx bot with
                   | Some ot
                     when not
                            (List.exists
                               (fun (n, _) -> norm n = norm k)
                               ot.kenmerk_granulariteit) ->
                       [ v "WF-056" (sub "filter")
                           ("gedurende over '" ^ k ^ "', which is not a temporal characteristic")
                       ]
                   | _ -> [])
               | _ -> [])
           | Some _ -> [])
           @
           match f.argumenten with
           | [ Attribuut_ref { attribuut = Some b; objecttype = Some bot; _ } ] -> (
               match objecttype ctx bot with
               | Some ot -> (
                   match
                     List.find_opt (fun (n, _) -> norm n = norm b) ot.attribuut_granulariteit
                   with
                   | Some (_, Some bg) -> (
                       match f.granulariteit with
                       | Some g when bg < g ->
                           [ v "WF-056" (sub "arguments/0")
                               "pro-rata base granularity is finer than the bucket" ]
                       | _ -> [])
                   | _ ->
                       [ v "WF-056" (sub "arguments/0")
                           ("pro-rata base '" ^ b ^ "' is not a temporal attribute") ])
               | None -> [])
           | [ Parameter_ref { parameter = Some p; _ } ] -> (
               match find_ci ctx.doc.parameters p with
               | Some { par_granulariteit = Some _; _ } -> []
               | _ ->
                   [ v "WF-056" (sub "arguments/0")
                       ("pro-rata base parameter '" ^ p ^ "' is not granular") ])
           | _ -> [ v "WF-056" at "timeProportionalPart takes one temporal source argument" ])
      (* WF-047 and WF-102: the fold's range. The loader has already seen to
         its presence and to one spelling (B12) *)
      @ (let fold_source =
           match (f.element_objecttype, f.argumenten) with
           | Some el, [ Attribuut_ref { attribuut; _ } ] when source_position && ranged -> (
               match attribuut with
               | Some a -> (
                   match source_reading ctx ~element_type:el ~subject ~kind:f.kind ~ranged a with
                   | Src_count | Src_role_under_value_fold -> Some (el, `Role_name a)
                   | _ -> Some (el, `Member))
               | None -> Some (el, `Member))
           | _ -> None
         in
         (* WF-102 (maintainer decision 2026-09-11): the fold source's range
            may be a PATH. Placed AHEAD of the one-step logic and returning
            instead of falling through, so exactly one of the two shapes is
            judged. *)
         match f.bron_pad with
         | Some pad ->
             (* (b) the arity: the empty list and the single step are the
                one-step spelling's territory, not the path's *)
             (if List.length pad >= 2 then []
              else
                [ v "WF-102" (sub "sourcePath")
                    (Printf.sprintf
                       "a sourcePath carries at least TWO steps, found %d — the one-step range \
                        is spelled sourceRole"
                       (List.length pad)) ])
             (* (c) the position: the set source of a single-argument VALUE
                fold. A count's range is one step, spelled sourceRole — its
                source names the one relation whose role it counts. *)
             @ (match fold_source with
               | Some (_, `Member) -> []
               | _ ->
                   [ v "WF-102" (sub "sourcePath")
                       "a sourcePath is admissible only as the range of a single-argument VALUE \
                        fold's set source — a count's range is one step, spelled sourceRole" ])
             (* (d) every step resolves (WF-047) and the steps JOIN: the
                first step's opposite role is filled by the fold subject,
                each further step's opposite role by the type its
                predecessor reaches, and the last step's bound role by the
                elementType. A step that does not resolve stops the
                walk: without its fillers the rest of the chain has nothing
                to join against, and reporting a cascade of joins off an
                unresolved step would name the wrong defect. *)
             @ (let rec chain i from_t = function
                  | [] -> []
                  | (rb : rol_binding) :: rest -> (
                      let step = sub ("sourcePath/" ^ string_of_int i) in
                      match resolve_binding ctx rb with
                      | Error e -> [ v "WF-047" step e ]
                      | Ok (_, bound, other) ->
                          let bound_filler =
                            Option.value bound.rol_objecttype_naam ~default:""
                          in
                          let other_filler =
                            Option.value other.rol_objecttype_naam ~default:""
                          in
                          let joins =
                            match from_t with
                            | None -> []
                            | Some ft when fills ctx ~inst:ft ~role:other -> []
                            | Some ft ->
                                [ v "WF-102" step
                                    (Printf.sprintf
                                       "the opposite role '%s' fills '%s', but this step starts \
                                        at '%s' (%s) — a sourcePath is a connected walk"
                                       (Option.value other.rol_naam ~default:"?")
                                       other_filler ft
                                       (if i = 0 then "the fold subject"
                                        else "the type the previous step reaches")) ]
                          in
                          (* TWO-SIDED, on the WF-047 line and deliberately
                             not narrowed here: the sourceRole arm below asks the
                             same question through [wf047_binding], which is
                             ⊑-compatible in both directions, and a one-sided
                             test on this arm alone would make the two
                             spellings of one fold disagree. *)
                          let lands =
                            if rest <> [] then []
                            else
                              match f.element_objecttype with
                              | Some el when not (type_compatible ctx bound_filler el) ->
                                  [ v "WF-102" step
                                      (Printf.sprintf
                                         "the last step's bound role '%s' fills '%s', but the \
                                          fold's elementType is '%s' — the path must land on \
                                          the element type"
                                         (Option.value bound.rol_naam ~default:"?")
                                         bound_filler el) ]
                              | _ -> []
                          in
                          joins @ lands @ chain (i + 1) (Some bound_filler) rest)
                in
                chain 0 subject pad)
         | None -> (
             match (fold_source, f.bron_rol) with
             | _, None -> []
             (* the range beside a count's union is WF-110 (c)'s second claim, and
                the filled count's non-reference source is WF-037's, and so is a
                fold that names its population and hands no source — what is
                missing there is the operand, not an instance fold for the
                binding to belong to: one defect, one rule *)
             | None, Some _ when f.kind = Aantal && arity = 1
                                 && (match f.argumenten with
                                    | [ Instance_set_union _ ] -> true
                                    | _ -> false) ->
                 []
             | None, Some _ when f.kind = Aantal_gevulde && source_position -> []
             | None, Some _ when sourceless <> None -> []
             | None, Some _ ->
                 [ v "WF-047" (sub "sourceRole")
                     "a fold-source role binding on a node that is not an instance fold over a \
                      relation" ]
             | Some (el, written), Some rb -> (
                 wf047_binding ctx (sub "sourceRole") rb ~bound_filler:(Some el)
                   ~other_filler:subject
                 @
                 (* the count source's written name must denote the bound
                    role — one relation, not two spellings of two *)
                 match (resolve_binding ctx rb, written) with
                 | Ok (_, bound, _), `Role_name w
                   when not
                          ((match bound.rol_naam with Some n -> norm n = norm w | None -> false)
                          || match bound.rol_meervoud with
                             | Some m -> norm m = norm w
                             | None -> false) ->
                     [ v "WF-047" (sub "sourceRole")
                         (Printf.sprintf
                            "the written fold source '%s' does not denote the bound role '%s'" w
                            (Option.value bound.rol_naam ~default:"?")) ]
                 | _ -> [])))
  | Afgerond { basis; _ } ->
      (match basis with Some e -> walk_expr ctx ~binders ~subject ~body (sub "base") e | None -> [])
  | Begrensd { basis; minimum; maximum; _ } ->
      (if minimum = None && maximum = None then
         [ v "WF-035" at "clamp without any bound" ]
       else [])
      @ List.concat_map
          (fun (n, e) ->
            match e with
            | Some e ->
                (match ty e with
                 | Some d when not (is_number_like d) ->
                     [ v "WF-035" (sub n) ("clamp " ^ n ^ " is " ^ datatype_kind d ^ ", must be numeric") ]
                 | _ -> [])
                @ walk_expr ctx ~binders ~subject ~body (sub n) e
            | None -> [])
          [ ("base", basis); ("minimum", minimum); ("maximum", maximum) ]
      (* WF-099 (maintainer decision 2026-09-10): a clamp RESTATES each bound
         into the BASE's unit before it compares magnitudes, so the two units
         must be compatible — the very demand WF-035 makes of an additive join
         one arm up, and WF-036 of a comparison. Read exactly as they read it:
         [units_compatible_adopting] over the operands' own types, so
         convertibility (not identity) is the line and a unitless numeric
         literal still adopts the base's unit. Each bound is crossed against
         the BASE and not against its sibling: that is the restating ref
         performs, and it makes min-vs-max compatibility fall out for free.
         Skipped unless both sides are number-like — a non-numeric operand is
         the WF-035 arm directly above, and one node should not report the same
         defect twice. *)
      @ (match Option.bind basis ty with
         | Some bt when is_number_like bt ->
             List.concat_map
               (fun (n, e) ->
                 match Option.bind e ty with
                 | Some d
                   when is_number_like d
                        && not (units_compatible_adopting basis e (unit_of bt) (unit_of d)) ->
                     [ v "WF-099" (sub n) ("clamp " ^ n ^ " and base have incompatible units") ]
                 | _ -> [])
               [ ("minimum", minimum); ("maximum", maximum) ]
         | _ -> [])
  | Attribuut_ref { attribuut; objecttype = otn; navigatie_bindingen; _ } -> (
      match otn with
      | Some otn -> (
          match objecttype ctx otn with
          | None ->
              [ v "WF-034" at ("object type '" ^ otn ^ "' is not declared") ]
          | Some ot ->
              (* [objecttype] is the type the value lives on — the subject
                 itself or the navigation endpoint — so the member check
                 covers both shapes *)
              (match attribuut with
               | Some a ->
                   if has_attr ot a then []
                   else if has_kenmerk ot a then
                     (* WF-080: a value position reads a value; a characteristic
                        endpoint carries no attribute value here. A
                        characteristic is read through a characteristic check
                        (ch. 4.5), never an AttributeReference in a value slot. *)
                     [ v "WF-080" at
                         ("'" ^ a ^ "' is a characteristic of '" ^ otn
                        ^ "', not an attribute — a characteristic endpoint is ill-formed in a \
                           value position") ]
                   else
                     [ v "WF-034" at
                         ("'" ^ a ^ "' is neither an attribute nor a characteristic of '" ^ otn
                        ^ "'") ]
               | None ->
                   (* WF-034 (widened, maintainer decision 2026-09-04, second
                      wave): the OTHER half of the resolving pair. A reference
                      resolves through `subjectType` AND `attribute`; with the
                      member name absent it lands on a declared type and reads
                      nothing there. *)
                   [ v "WF-034" at
                       ("attribute reference on '" ^ otn
                      ^ "' without an attribute name: it resolves against no declaration") ])
              (* WF-053: the navigated read, or the subject's own *)
              @ wf053_read ctx at ~subject ~objecttype:otn ~bindings:navigatie_bindingen)
      | None ->
          (* WF-034 (widened, maintainer decision 2026-09-04): the absent
             endpoint resolves against nothing — it skips the member
             resolution, WF-053 and WF-080 at once *)
          [ v "WF-034" at
              ("attribute reference '"
              ^ Option.value attribuut ~default:"?"
              ^ "' without a subjectType: it resolves against no declaration") ])
  | Dimensie_cel { attribuut; labels; _ } ->
      (match attribuut with
       | Some (Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ } as inner) ->
           (match objecttype ctx otn with
            | Some ot -> (
                match find_ci ot.attribuut_dimensies a with
                | Some dims ->
                    let dims = some_names dims in
                    let lbls = some_names labels in
                    List.concat_map
                      (fun dn ->
                        match find_ci ctx.doc.dimensies dn with
                        | None -> [] (* WF-004 reports it *)
                        | Some dim ->
                            let hits =
                              List.filter (fun l -> mem_ci (some_names dim.dim_waarden) l) lbls
                            in
                            if List.length hits <> 1 then
                              [ v "WF-040" at
                                  (Printf.sprintf
                                     "cell read pins dimension '%s' %d times, must be exactly once"
                                     dn (List.length hits)) ]
                            else [])
                      dims
                    @
                    if List.length lbls <> List.length dims then
                      [ v "WF-040" at
                          (Printf.sprintf "cell read carries %d labels for %d dimensions"
                             (List.length lbls) (List.length dims)) ]
                    else []
                | None ->
                    [ v "WF-040" at ("cell read of undimensioned attribute '" ^ a ^ "'") ])
            | None -> [])
           @ walk_expr ctx ~binders ~subject (sub "attribute") inner
       | Some (Attribuut_ref _ as inner) ->
           (* an attribute reference with an empty half: the walk's own
              WF-034 arms report that endpoint, so this arm stays silent
              rather than reporting the same defect twice *)
           walk_expr ctx ~binders ~subject (sub "attribute") inner
       | Some inner ->
           (* WF-034 (widened, maintainer decision 2026-09-04, third wave):
              a cell read reads ONE dimensioned attribute, so its `attribute`
              child is a reference position, not a general expression slot.
              Anything else there resolves against no declaration and takes
              the arm above down with it: WF-040's dimension/label cross sits
              behind the `Some/Some` reference pattern. Until L2 the identity
              family's cross of the cell's OWN type sat there too, measured
              on `wf091-dimensiecel-eenheid`, whose pinned unit lie was on the
              cell type: replacing the reference by a literal turned that
              refusal off and the document CLEAN. Since L2 the cell carries
              no type (B13), and with this arm disabled
              `wf034-dimensiecel-geen-referentie` turns clean while no other
              report changes (measured in the L2 fix round). *)
           [ v "WF-034" (sub "attribute")
               "dimension-cell read whose attribute position is not an attribute reference: it \
                resolves against no declaration" ]
           @ walk_expr ctx ~binders ~subject (sub "attribute") inner
       | None ->
           (* the empty half of the same position, and the same consequence
              one field further: measured verifier-clean on both golden cell
              positions and, until L2, on `wf091-dimensiecel-eenheid`, which
              turned CLEAN when its cell's `attribute` was emptied; with this
              arm disabled `wf034-dimensiecel-zonder-attribuut` turns clean
              while no other report changes (measured in the L2 fix round) *)
           [ v "WF-034" at
               "dimension-cell read without an attribute reference: it resolves against no \
                declaration" ])
  | Parameter_ref { parameter; _ } -> (
      match parameter with
      | Some p -> (
          match find_ci ctx.doc.parameters p with
          | None -> [ v "WF-034" at ("reference to undeclared parameter '" ^ p ^ "'") ]
          | Some _ -> [])
      | None ->
          (* WF-034 (widened, 2026-09-04, second wave): the same shape one node
             kind over. A parameter reference resolves through exactly one
             field; the arm above reports a name that resolves nowhere, and
             sits behind `Some p`, so an ABSENT name walked past it. *)
          [ v "WF-034" at
              "parameter reference without a parameter name: it resolves against no declaration"
          ])
  | Variabele_ref _ -> [] (* binding sites are not serialized in v0 — OPEN *)
  | Constante { naam; _ } -> (
      (* WF-098 (maintainer decision 2026-09-10): the context-value name is a
         CLOSED SET, not a reference. Nothing in the document declares it, so
         there is no table to resolve against — the two admissible names are
         fixed by ch. 4.1 and [context_value_names] enumerates them. Since L4
         the bound day of a day-kind definition is a node of its own
         (BoundDay), so a ContextValue naming it falls outside the set like
         any other name.

         The comparison is BYTE-EXACT, and that is the decision the arm carries:
         the readers below the wire disagreed about case (`ref` and the ASP
         backend normalised, the Drools backend's constant reader used `equals`
         while its day-kind reader used `equalsIgnoreCase`), so a ci-different
         spelling was a name three consumers accepted and one did not. Refusing
         it here makes that divergence unreachable through a verifier-clean
         document, and every executor refuses it at load with this same
         sentence ([Loader.reader]). A context value without a name cannot be
         written since L4 (the loader refuses it). *)
      match naam with
      | None -> []
      | Some n -> (
          match context_value_refusal n with
          | Some sentence -> [ v "WF-098" (sub "name") sentence ]
          | None -> []))
  | Bound_day _ -> []
  | Expr_onopgelost _ -> [] (* honest marker; WF-044 is behavioural *)
  | Temporal_read { basis; datum; typ; _ } ->
      (* WF-049: the point-in-time read (extension node, row 11) *)
      let basis_checks =
        match basis with
        | Attribuut_ref { attribuut = Some a; objecttype = Some otn; navigatie_bindingen = None; _ }
          -> (
            match objecttype ctx otn with
            | None -> [] (* resolution is the walk's WF-034, below *)
            | Some ot ->
                (if attr_temporal ot a then []
                 else
                   [ v "WF-049" (sub "base")
                       ("temporal read of '" ^ a ^ "', which is not a temporal attribute of '"
                      ^ otn ^ "'") ])
                @ (match subject with
                  | Some st when norm st <> norm otn ->
                      [ v "WF-049" (sub "base")
                          ("the read's timeline belongs to '" ^ otn
                         ^ "', not the subject type '" ^ st ^ "'") ]
                  | _ -> [])
                @
                (* the node's type is the timeline's element datatype *)
                (match (typ, find_ci ot.attributen a) with
                | Some t, Some (Some decl) when t <> decl ->
                    [ v "WF-049" (sub "type")
                        "the read's type must equal the timeline's element datatype" ]
                | None, _ ->
                    [ v "WF-049" (sub "type") "the read carries no type (extension node: required)" ]
                | _ -> []))
        | Parameter_ref { parameter = Some p; _ } -> (
            match find_ci ctx.doc.parameters p with
            | None -> [] (* WF-034 below *)
            | Some par ->
                (if par.par_granulariteit <> None then []
                 else
                   [ v "WF-049" (sub "base")
                       ("temporal read of parameter '" ^ p ^ "', which is not granular") ])
                @ (match (typ, par.par_datatype) with
                  | Some t, Some decl when t <> decl ->
                      [ v "WF-049" (sub "type")
                          "the read's type must equal the timeline's element datatype" ]
                  | None, _ ->
                      [ v "WF-049" (sub "type") "the read carries no type (extension node: required)" ]
                  | _ -> []))
        | _ ->
            [ v "WF-049" (sub "base")
                "the read's base must be an own temporal attribute reference or a granular \
                 parameter reference (a temporal characteristic operand is OPEN)" ]
      in
      let datum_checks =
        match ty datum with
        | Some Datum_in_dagen -> []
        | Some d ->
            [ v "WF-049" (sub "date")
                ("the read's date operand is " ^ datatype_kind d
               ^ ", must be a day-precision date") ]
        | None ->
            [ v "WF-049" (sub "date")
                "the read's date operand carries no type (extension node: day precision required)" ]
      in
      basis_checks @ datum_checks
      @ walk_expr ctx ~binders ~subject (sub "base") basis
      @ walk_expr ctx ~binders ~subject (sub "date") datum
  | Construct { typ; constructor_; argument; _ } ->
      (* WF-062: the constructor is declared in the node's sum type; the
         argument is present iff the constructor is unary; the argument's
         static type equals the declared payload datatype *)
      (match typ with
       | None -> [ v "WF-062" (sub "type") "construct carries no sum type" ]
       | Some (Sum_type { domein; _ }) -> (
           match sum_decl ctx domein with
           | None ->
               [ v "WF-062" (sub "type")
                   ("construct names sum-type domain '" ^ domein
                  ^ "', which is not a declared sum type") ]
           | Some ctors -> (
               match List.find_opt (fun (n, _) -> norm n = norm constructor_) ctors with
               | None ->
                   [ v "WF-062" at
                       ("'" ^ constructor_ ^ "' is not a declared constructor of sum type '"
                      ^ domein ^ "'") ]
               | Some (_, payload) -> (
                   match payload, argument with
                   | None, None | Some _, Some _ ->
                       (match payload, argument with
                        | Some pd, Some a -> (
                            match ty a with
                            | Some ad when ad <> pd ->
                                [ v "WF-062" at
                                    ("argument type " ^ datatype_detail ad
                                   ^ " does not match the declared payload "
                                   ^ datatype_detail pd) ]
                            | _ -> [])
                        | _ -> [])
                   | None, Some _ ->
                       [ v "WF-062" at
                           ("nullary constructor '" ^ constructor_ ^ "' carries an argument") ]
                   | Some _, None ->
                       [ v "WF-062" at
                           ("unary constructor '" ^ constructor_ ^ "' carries no argument") ])))
       | Some d ->
           [ v "WF-062" (sub "type")
               ("construct type is " ^ datatype_kind d ^ ", not a sum type") ])
      @ (match argument with
         | Some a -> walk_expr ctx ~binders ~subject ~body (sub "argument") a
         | None -> [])
  | Match { typ; scrutinee; arms; _ } ->
      (* WF-063 is well-formed over BOTH wire variants of the keuzetype
         family (ch. 2.4): a sum-typed scrutinee matches declared
         constructors (a unary arm binds its payload), an enumeration-typed
         scrutinee matches the declared value list (every alternative is
         nullary, so an enum arm's binder is ALWAYS null). Both read one
         declared alternative list — the DECLARATION, never the use site's
         inline copy — so totality, duplicate and binder checks are shared;
         only the words in the messages differ. *)
      let decl =
        match ty scrutinee with
        | Some (Sum_type { domein; _ }) -> `Sum (domein, sum_decl ctx domein)
        | Some (Enumeratie { domein = Some d; _ }) -> `Enum (d, enum_decl ctx d)
        | Some (Enumeratie { domein = None; _ }) -> `NoDomain
        | Some d -> `NotKeuzetype d
        | None -> `NoType
      in
      let alt, family =
        match decl with
        | `Enum _ | `NoDomain -> ("enumeration value", "enumeration domain")
        | _ -> ("constructor", "sum type")
      in
      let scrutinee_checks, ctors =
        match decl with
        | `NoType -> ([ v "WF-063" (sub "scrutinee") "match scrutinee carries no type" ], None)
        | `NotKeuzetype d ->
            ([ v "WF-063" (sub "scrutinee")
                 ("match scrutinee is " ^ datatype_kind d
                ^ ", which is no keuzetype (a sum type or an enumeration)") ], None)
        | `NoDomain ->
            ([ v "WF-063" (sub "scrutinee")
                 "match scrutinee is an enumeration naming no declaring domain" ], None)
        | `Sum (domein, None) ->
            ([ v "WF-063" (sub "scrutinee")
                 ("match scrutinee names sum-type domain '" ^ domein
                ^ "', which is not a declared sum type") ], None)
        | `Enum (domein, None) ->
            ([ v "WF-063" (sub "scrutinee")
                 ("match scrutinee names enumeration domain '" ^ domein
                ^ "', which is not a declared enumeration domain") ], None)
        | `Sum (_, Some cs) | `Enum (_, Some cs) -> ([], Some cs)
      in
      let arm_names = List.map (fun (c, _, _) -> c) arms in
      (* WF-063: the arms cover exactly the declared constructors, each once *)
      let totality =
        match ctors with
        | None -> []
        | Some cs ->
            let declared = List.map fst cs in
            List.map
              (fun (_, dup, _) ->
                v "WF-063" at (alt ^ " '" ^ dup ^ "' is matched by more than one arm"))
              (dups_by norm (List.map (fun c -> (c, at)) arm_names))
            @ List.filter_map
                (fun c ->
                  if mem_ci declared c then None
                  else
                    Some
                      (v "WF-063" at
                         ("arm '" ^ c ^ "' is not a declared " ^ alt ^ " of the " ^ family)))
                arm_names
            @ List.filter_map
                (fun d ->
                  if mem_ci arm_names d then None
                  else Some (v "WF-063" at (alt ^ " '" ^ d ^ "' is not covered by any arm")))
                declared
      in
      (* WF-063: an arm carries a binder iff its alternative is unary — so on
         an enumeration scrutinee, whose alternatives are all nullary, never *)
      let binder_checks =
        match ctors with
        | None -> []
        | Some cs ->
            List.concat_map
              (fun (c, binder, _) ->
                match List.find_opt (fun (n, _) -> norm n = norm c) cs with
                | None -> [] (* alien arm: reported by totality *)
                | Some (_, payload) -> (
                    match payload, binder with
                    | None, None | Some _, Some _ -> []
                    | None, Some b ->
                        [ v "WF-063" at
                            ("nullary " ^ alt ^ " '" ^ c ^ "' carries a binder '" ^ b ^ "'") ]
                    | Some _, None ->
                        [ v "WF-063" at ("unary " ^ alt ^ " '" ^ c ^ "' carries no binder") ]))
              arms
      in
      (* the binders an arm body sees: the arm binder, whose declared
         datatype is the matched constructor's payload, over the enclosing
         ones *)
      let arm_binders cname binder =
        match binder with
        | Some b ->
            let bdt =
              match ctors with
              | Some cs -> (
                  match List.find_opt (fun (n, _) -> norm n = norm cname) cs with
                  | Some (_, payload) -> payload
                  | None -> None)
              | None -> None
            in
            (b, bdt) :: binders
        | None -> binders
      in
      (* WF-064: all arm bodies agree on one result datatype — the Match's own type *)
      let body_type_checks =
        match typ with
        | None -> []
        | Some mt ->
            List.filter_map
              (fun (c, binder, body) ->
                match expr_type ctx ~binders:(arm_binders c binder) body with
                | Some bt when bt <> mt ->
                    Some
                      (v "WF-064" at
                         ("the '" ^ c ^ "' arm body is " ^ datatype_detail bt
                        ^ ", but the match result type is " ^ datatype_detail mt))
                | _ -> None)
              arms
      in
      scrutinee_checks @ totality @ binder_checks @ body_type_checks
      @ walk_expr ctx ~binders ~subject ~body (sub "scrutinee") scrutinee
      @ List.concat
          (List.mapi
             (fun i (cname, binder, arm_body) ->
               walk_expr ctx ~binders:(arm_binders cname binder) ~subject ~body
                 (Printf.sprintf "%s/arms/%d/body" at i) arm_body)
             arms)
  | Binder_ref { naam; _ } -> (
      (* WF-064: a binder reference resolves lexically to an enclosing binder
         (a match arm, or a lambda/function parameter — row 10); a
         VariableReference stays outside this rule (row 24). Inside a closed body
         the visible [binders] are its own — a lambda/function's parameters
         and whatever an inner Let/match/filter binds; the enclosing scope's
         binders were dropped at the closure boundary, so a reference that
         reaches out to one is rejected here (the closure clause), citing the
         outer name it names when we still remember it. Since L2 the read
         carries no type of its own: the binder's declaration is its type
         ([expr_type]). *)
      match List.find_opt (fun (b, _) -> norm b = norm naam) binders with
      | None ->
          let outside =
            match body with
            | Closed_pos (_, outer) -> List.exists (fun n -> norm n = norm naam) outer
            | Rule_pos | Let_pos -> false
          in
          if outside then
            [ v "WF-064" at
                ("binder reference '" ^ naam
               ^ "' is bound outside the enclosing closed body — a closed function or lambda body \
                  reads only its own parameters and inner bindings") ]
          else [ v "WF-064" at ("binder reference '" ^ naam ^ "' resolves to no enclosing binder") ]
      | Some _ -> [])
  | Struct_literal { typ; velden; _ } ->
      (* WF-073: the type is a declared struct; the supplied fields are
         exactly the declared fields, each once; each value's static type
         equals the field declaration *)
      (match typ with
       | None -> [ v "WF-073" (sub "type") "struct literal carries no type" ]
       | Some (Struct_type { domein; _ }) -> (
           match struct_decl ctx domein with
           | None ->
               [ v "WF-073" (sub "type")
                   ("struct literal names struct-type domain '" ^ domein
                  ^ "', which is not a declared struct type") ]
           | Some decl ->
               let declared_names = List.map fst decl in
               let lit_names = List.map fst velden in
               List.map
                 (fun (_, dup, _) ->
                   v "WF-073" at ("field '" ^ dup ^ "' is supplied by more than one entry"))
                 (dups_by norm (List.map (fun n -> (n, at)) lit_names))
               @ List.filter_map
                   (fun n ->
                     if mem_ci declared_names n then None
                     else
                       Some
                         (v "WF-073" at
                            ("field '" ^ n ^ "' is not a declared field of struct '" ^ domein ^ "'")))
                   lit_names
               @ List.filter_map
                   (fun d ->
                     if mem_ci lit_names d then None
                     else Some (v "WF-073" at ("declared field '" ^ d ^ "' is not supplied")))
                   declared_names
               @ List.concat_map
                   (fun (fname, value) ->
                     match List.find_opt (fun (n, _) -> norm n = norm fname) decl with
                     | None -> [] (* alien: reported above *)
                     | Some (_, fdt) -> (
                         match ty value with
                         | Some vt when vt <> fdt ->
                             [ v "WF-073" at
                                 ("field '" ^ fname ^ "' value is " ^ datatype_detail vt
                                ^ ", but the declared field datatype is " ^ datatype_detail fdt) ]
                         | _ -> []))
                   velden)
       | Some d ->
           [ v "WF-073" (sub "type")
               ("struct literal type is " ^ datatype_kind d ^ ", not a struct type") ])
      @ List.concat
          (List.mapi
             (fun i (_, value) ->
               walk_expr ctx ~binders ~subject ~body (sub (Printf.sprintf "fields/%d/value" i)) value)
             velden)
  | Field_access { typ; base; veld; _ } ->
      (* WF-074: the base is struct-typed; the field is a declared field; the
         node type equals that field's declared datatype *)
      (match ty base with
       | Some (Struct_type { domein; _ }) -> (
           match struct_decl ctx domein with
           | None ->
               [ v "WF-074" (sub "base")
                   ("field access base names struct-type domain '" ^ domein
                  ^ "', which is not a declared struct type") ]
           | Some decl -> (
               match List.find_opt (fun (n, _) -> norm n = norm veld) decl with
               | None ->
                   [ v "WF-074" at
                       ("'" ^ veld ^ "' is not a declared field of struct '" ^ domein ^ "'") ]
               | Some (_, fdt) -> (
                   match typ with
                   | Some t when t <> fdt ->
                       [ v "WF-074" (sub "type")
                           ("field access type " ^ datatype_detail t
                          ^ " differs from the declared field datatype " ^ datatype_detail fdt) ]
                   | None -> [ v "WF-074" (sub "type") "field access carries no type" ]
                   | _ -> [])))
       | Some d ->
           [ v "WF-074" (sub "base")
               ("field access base is " ^ datatype_kind d ^ ", not a struct value") ]
       | None -> [ v "WF-074" (sub "base") "field access base carries no type" ])
      @ walk_expr ctx ~binders ~subject ~body (sub "base") base
  | Let { typ; binder; value; body = lbody; _ } ->
      (* WF-076: the Let binding (IR-SPEC ch. 4.10). The node type equals the
         body type; the binder is visible only in the body (never in its own
         value); the binder joins the WF-064 regime for the body. *)
      (* the body sees the binder, typed by its value *)
      let body_binders = (binder, ty value) :: binders in
      (match typ, expr_type ctx ~binders:body_binders lbody with
       | Some t, Some bt when t <> bt ->
           [ v "WF-076" (sub "type")
               (Printf.sprintf
                  "the let type (%s) differs from its body type (%s) — the let type must equal the \
                   body type"
                  (datatype_detail t) (datatype_detail bt)) ]
       | None, _ -> [ v "WF-076" (sub "type") "let carries no type" ]
       | _ -> [])
      @ (if mentions_binder binder value then
           [ v "WF-076" (sub "value")
               ("the let binder '" ^ binder
              ^ "' is referenced in its own value — a binder is visible only in the let body") ]
         else [])
      (* the value evaluates in the enclosing scope: the binder is NOT in scope
         here, and the position context is unchanged (a Let value inside a
         closed body stays closed) *)
      @ walk_expr ctx ~binders ~subject ~body (sub "value") value
      (* the body: the binder joins the scope carrying its value's datatype;
         a Let body admits IfThenElse (Let_pos) unless already inside a
         closed body (stay closed) *)
      @ walk_expr ctx ~binders:body_binders ~subject
          ~body:(match body with Closed_pos (wf, o) -> Closed_pos (wf, o) | Rule_pos | Let_pos -> Let_pos)
          (* a Let inside a closed body stays closed with the SAME outer set —
             the Let binder is an INNER binding (added to [binders] above), so a
             BinderRef to it resolves; only reads reaching past the boundary are
             the closure clause's concern *)
          (sub "body") lbody
  | List_literal { typ; elements; _ } ->
      (* WF-066: every element's static type equals the declared element datatype *)
      let elem_dt = match typ with Some (List_type { element }) -> Some element | _ -> None in
      (match typ with
       | Some (List_type _) -> []
       | Some d ->
           [ v "WF-066" (sub "type") ("list literal type is " ^ datatype_kind d ^ ", not a list type") ]
       | None -> [ v "WF-066" (sub "type") "list literal carries no type" ])
      @ (match elem_dt with
         | None -> []
         | Some ed ->
             List.concat
               (List.mapi
                  (fun i e ->
                    match ty e with
                    | Some t when t <> ed ->
                        [ v "WF-066" (sub (Printf.sprintf "elements/%d" i))
                            ("element type " ^ datatype_detail t
                           ^ " differs from the declared element " ^ datatype_detail ed) ]
                    | _ -> [])
                  elements))
      @ List.concat
          (List.mapi
             (fun i e -> walk_expr ctx ~binders ~subject ~body (sub (Printf.sprintf "elements/%d" i)) e)
             elements)
  | Lambda _ ->
      (* WF-069: a lambda is admissible only as a Fold/MapList function operand
         (there it is loaded as a fold_fn, never this expr variant) *)
      [ v "WF-069" at "a Lambda is admissible only as the function operand of a Fold or MapList" ]
  | If_then_else { typ; condition; then_; else_; _ } ->
      (* WF-069: position-restricted to lambda, function and (the Phase-5c
         widening) Let bodies; both branches agree on the node's own
         type. WF-077: the condition-operand discipline holds wherever an
         IfThenElse is admissible. *)
      (match body with
       | Closed_pos _ | Let_pos -> []
       | Rule_pos ->
           [ v "WF-069" at
               "an IfThenElse is admissible only inside a lambda, function or Let body" ])
      @ wf077_condition (sub "condition") condition
      @ (let branch name e =
           match typ, ty e with
           | Some t, Some bt when t <> bt ->
               [ v "WF-069" (sub name)
                   ("the " ^ name ^ " branch is " ^ datatype_detail bt
                  ^ ", but the IfThenElse type is " ^ datatype_detail t) ]
           | _ -> []
         in
         branch "then" then_ @ branch "else" else_)
      @ walk_cond ctx ~binders ~subject ~body (sub "condition") condition
      @ walk_expr ctx ~binders ~subject ~body (sub "then") then_
      @ walk_expr ctx ~binders ~subject ~body (sub "else") else_
  | Invoke { typ; functie; arguments; _ } ->
      (* WF-068: resolvable function; arity; argument types; result type *)
      (match function_decl ctx functie with
       | None ->
           [ v "WF-068" at ("invoke names '" ^ functie ^ "', which is not a declared function") ]
       | Some fd ->
           (if List.length arguments <> List.length fd.fd_parameters then
              [ v "WF-068" at
                  (Printf.sprintf "invoke of '%s' supplies %d arguments, but it declares %d parameters"
                     functie (List.length arguments) (List.length fd.fd_parameters)) ]
            else
              List.concat
                (List.mapi
                   (fun i (a, (_, pt)) ->
                     match ty a with
                     | Some at' when at' <> pt ->
                         [ v "WF-068" (sub (Printf.sprintf "arguments/%d" i))
                             ("argument type " ^ datatype_detail at'
                            ^ " does not match the parameter datatype " ^ datatype_detail pt) ]
                     | _ -> [])
                   (List.combine arguments fd.fd_parameters)))
           @ (match typ with
             | Some t when t <> fd.fd_result ->
                 [ v "WF-068" (sub "type")
                     "invoke result type differs from the declared function result" ]
             | _ -> []))
      @ List.concat
          (List.mapi
             (fun i a -> walk_expr ctx ~binders ~subject ~body (sub (Printf.sprintf "arguments/%d" i)) a)
             arguments)
  | Fold { typ; list; functie; init; _ } ->
      let src_elem = match ty list with Some (List_type { element }) -> Some element | _ -> None in
      fold_source_check ctx ~binders (sub "list") list
      (* WF-070: the accumulator (node type = init type) is one first-order
         non-list datatype *)
      @ (match typ with
        | Some d when is_list_type d ->
            [ v "WF-070" (sub "type")
                "fold accumulator is list-typed — it must be a first-order non-list datatype" ]
        | _ -> [])
      @ (match typ, ty init with
        | Some at', Some it when at' <> it ->
            [ v "WF-070" (sub "init") "fold init datatype differs from the accumulator type" ]
        | _ -> [])
      (* WF-069/070: the (accumulator, element) function, its closed body *)
      @ check_fold_fn ctx ~binders ~subject (sub "function") ~params:[ typ; src_elem ] ~result:typ functie
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list
      @ walk_expr ctx ~binders ~subject ~body (sub "init") init
  | Map_list { typ; list; functie; _ } ->
      let src_elem = match ty list with Some (List_type { element }) -> Some element | _ -> None in
      let out_elem = match typ with Some (List_type { element }) -> Some element | _ -> None in
      fold_source_check ctx ~binders (sub "list") list
      (* WF-070: the node type is the ListType of the body's datatype *)
      @ (match typ with
        | Some (List_type _) -> []
        | Some d ->
            [ v "WF-070" (sub "type") ("map result is " ^ datatype_kind d ^ ", must be a list type") ]
        | None -> [ v "WF-070" (sub "type") "map carries no result type" ])
      @ check_fold_fn ctx ~binders ~subject (sub "function") ~params:[ src_elem ] ~result:out_elem functie
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list
  | Filter_list { typ; list; binder; condition; _ } ->
      let src_elem = match ty list with Some (List_type { element }) -> Some element | _ -> None in
      fold_source_check ctx ~binders (sub "list") list
      (* WF-070: the node type equals the source's list type *)
      @ (match typ, ty list with
        | Some t, Some s when t <> s ->
            [ v "WF-070" (sub "type") "filter result type must equal the source list type" ]
        | _ -> [])
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list
      (* the binder is bound in the condition — the aggregation-filter precedent;
         it carries the source element datatype *)
      @ walk_cond ctx ~binders:((binder, src_elem) :: binders) ~subject ~body (sub "condition") condition
  | List_length { typ; list; _ } ->
      fold_source_check ctx ~binders (sub "list") list
      (* WF-070: the count is a whole number *)
      @ (match typ with
        | Some (Numeriek { geheel = true; _ }) -> []
        | Some d ->
            [ v "WF-070" (sub "type") ("length is " ^ datatype_kind d ^ ", must be a whole number") ]
        | None -> [ v "WF-070" (sub "type") "length carries no type" ])
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list
  | Instance_set_union { branches; _ } ->
      (* WF-110, the §5.7 concatenation of subject expressions (ch. 4.3,
         semantics RUNTIME-SEMANTICS ch. 3.4d; maintainer decision
         2026-09-20). Clause (c) — the count names no source of its own —
         is a claim about the COUNT and lives in the Functie arm above. *)
      if not union_ok then
        [ v "WF-110" at
            "an instance-set union stands where nothing consumes it: the union of subject \
             expressions denotes a set of INSTANCES, and the one node that folds instances is \
             the instance count — so this node is admissible as the SINGLE argument of a \
             count aggregation and at no other position (ch. 4.3)" ]
      else
        (* (b) two or more selections. ONE selection is the ordinary
           single-source count, which already has a spelling of its own
           (elementType beside sourceRole); admitting it here would give
           one meaning two spellings — the discipline WF-102 (b) draws for
           the one-step range and WF-087 for the nullary keuzetype. *)
        (match branches with
         | [] | [ _ ] ->
             [ v "WF-110" at
                 (Printf.sprintf
                    "an instance-set union carries at least TWO selections and this one carries \
                     %d: a single selection IS the ordinary instance count, spelled with the \
                     fold's own elementType and sourceRole, so one meaning keeps one \
                     spelling (ch. 4.3)"
                    (List.length branches)) ]
         | _ -> [])
        @ List.concat
            (List.mapi
               (fun i (b : instance_set_branch) ->
                 let bat = sub ("branches/" ^ string_of_int i) in
                 (* (d) one population for the whole union. A count answers a
                    cardinality over ONE declared type; with a population per
                    branch the membership test differs per member, and whether
                    two branches can then share an instance is a question about
                    the subtype lattice (ch. 2.6) that this rule does not
                    answer. Measured on the v0 toolchain, which refuses the
                    same shape on a mechanism ground of its own (one accumulate
                    has one source pattern); uchar refuses it on this one. *)
                 (* The reference is the first selection that NAMES a population, not
                    selection 0: a selection with no elementType is the neighbouring arm's
                    (it has its own witness) and must not also silence this one. Keyed on
                    the INDEX of that reference, so the reference selection never compares
                    against itself and the arms stay one-witness-per-defect. *)
                 (let reference =
                    List.find_index
                      (fun (b : instance_set_branch) -> b.isb_element_type <> None)
                      branches
                  in
                  match (reference, b.isb_element_type) with
                  | Some ri, Some el when i > ri ->
                      let r0 = List.nth branches ri in
                      if
                        match r0.isb_element_type with
                        | Some f0 -> norm f0 <> norm el
                        | None -> false
                      then
                        [ v "WF-110" (bat ^ "/elementType")
                            (Printf.sprintf
                               "the selections of an instance-set union range over ONE population: \
                                selection %d names '%s' and this one names '%s' (ch. 4.3)"
                               ri
                               (Option.value r0.isb_element_type ~default:"?")
                               el) ]
                      else []
                  | _ -> [])
                 (* (e) every selection names its population. Its relation is
                    schema-required (B12) and the loader reads it as required:
                    a type pair would give every selection of one union the
                    SAME derived range (clause (d)), so nothing derives it. *)
                 @ (match b.isb_element_type with
                   | None ->
                       [ v "WF-110" (bat ^ "/elementType")
                           "an instance-set union's selection MUST name the population it \
                            ranges over (ch. 4.3)" ]
                   | Some el when objecttype ctx el = None ->
                       [ v "WF-034" (bat ^ "/elementType")
                           ("instance-set selection element type '" ^ el ^ "' is not declared") ]
                   | Some _ -> [])
                 (* WF-047 at a new position rather than under a new rule — the
                    discipline WF-102 (d) applies per step *)
                 @ wf047_binding ctx (bat ^ "/sourceRole") b.isb_source_role
                     ~bound_filler:b.isb_element_type ~other_filler:subject
                 (* every selection's filter evaluates PER CONTRIBUTOR, so the
                    selection's own element type is its subject — ch. 3.4a's
                    fold-filter discipline, once per selection *)
                 @
                 match b.isb_filter with
                 | Some c -> walk_cond ctx ~binders ~subject:b.isb_element_type (bat ^ "/filter") c
                 | None -> [])
               branches))

(* WF-070: the fold-family [list] operand must be list-typed. *)
and fold_source_check ctx ~binders at list =
  match expr_type ctx ~binders list with
  | Some (List_type _) -> []
  | Some d -> [ v "WF-070" at ("fold source is " ^ datatype_kind d ^ ", must be list-typed") ]
  | None -> [ v "WF-070" at "fold source carries no type" ]

(* A Fold/MapList [function] operand (IR-SPEC ch. 4.8): [params] the expected
   parameter datatypes in order (accumulator then element for a fold, element
   for a map), [result] the expected body datatype. A lambda's body is a fresh
   closed body (WF-069); a name resolves against functionDefs (WF-070). *)
and check_fold_fn ctx ~binders ~subject at ~params ~result (fn : fold_fn) : violation list =
  let arity = List.length params in
  match fn with
  | Fn_lambda { parameters; body = lbody; _ } ->
      (if List.length parameters <> arity then
         [ v "WF-069" at
             (Printf.sprintf "lambda takes %d parameters, but its host expects %d"
                (List.length parameters) arity) ]
       else
         List.concat
           (List.map2
              (fun (pn, pt) exp ->
                match exp with
                | Some e when e <> pt ->
                    [ v "WF-070" at
                        ("lambda parameter '" ^ pn ^ "' is " ^ datatype_detail pt ^ ", expected "
                       ^ datatype_detail e) ]
                | _ -> [])
              parameters params))
      (* WF-092 at the lambda's own parameter datatypes: they are declared
         here and nowhere else — [walk_expr] never sees them (it walks the
         BODY), so without this call the unit shape law does not hold at a
         lambda parameter. Measured: 58 of the goldens' 2161 datatype
         positions were unreachable, this arm's parameters and the composite
         element/field/payload datatypes nested inside them. *)
      @ List.concat
          (List.mapi
             (fun j (_, pt) ->
               check_datatype_units ~stelsels:(stelsel_namen ctx)
                 (Printf.sprintf "%s/parameters/%d/datatype" at j)
                 pt)
             parameters)
      @ (match
           ( result,
             expr_type ctx ~binders:(List.map (fun (n, dt) -> (n, Some dt)) parameters) lbody )
         with
        | Some r, Some b when r <> b ->
            [ v "WF-070" at
                ("lambda body is " ^ datatype_detail b ^ ", but the fold requires "
               ^ datatype_detail r) ]
        | _ -> [])
      (* the lambda body is CLOSED (WF-069): it sees only its own parameters,
         never the enclosing scope's binders. Dropping the enclosing [binders]
         here is what closes WF-064's closure clause — an outer Let/fold binder
         read from inside the body no longer resolves and is rejected; their
         names ride in the [Closed_pos] payload solely to sharpen that
         diagnostic. *)
      @ walk_expr ctx
          ~binders:(List.map (fun (n, dt) -> (n, Some dt)) parameters)
          ~subject
          ~body:(Closed_pos ("WF-069", List.map fst binders))
          (at ^ "/body") lbody
  | Fn_name n ->
      (* WF-069 (the mirror-safe route): the function operand MUST be an inline
         lambda; a named function reaches a fold through a lambda body that
         Invokes it (exactly what the anchors do), so a bare FunctionDef name
         has no executor witness and is refused before translation *)
      [ v "WF-069" at
          ("the fold function operand names '" ^ n
         ^ "', but must be an inline lambda (a named function reaches a fold via a lambda body \
            that invokes it)") ]

(* [fc_doel] is the SECOND object a position binds, and exactly one position
   has one: the guard of a fact creation (IR-SPEC ch. 3.4), which iterates the
   anchor and reaches the target. It is passed only at that guard's OWN node and
   never propagated — the recursive calls below all omit it — so the composite
   arm, the expression walk and every other descent see a one-object position,
   which is the positional half of WF-109's scope (ch. 4.6 (q2)). *)
and walk_cond ctx ~binders ~subject ?(body = Rule_pos) ?fc_doel at (c : conditie) :
    violation list =
  let sub p = at ^ "/" ^ p in
  let whole_period_check periode operands =
    match periode with
    | None -> []
    | Some _ ->
        if List.exists (fun e -> match e with Some e -> expr_temporal ctx e | None -> false) operands
        then []
        else
          [ v "WF-043" at "wholePeriod qualifier on a non-time-dependent operand" ]
  in
  let closed =
    (* inside a closed body a condition is limited to Comparison/Predicate
       over admissible operands (IR-SPEC ch. 2.11): a characteristic,
       role-membership, period, composite or regelversie read breaks closure.
       A Let body (Let_pos) is open; rule level (Rule_pos) too. *)
    match body with
    | Rule_pos | Let_pos -> None
    | Closed_pos (wf, _) -> (
        match c with
        | Vergelijking _ | Predicaat _ -> None
        | _ ->
            Some
              [ v wf at
                  "this condition reads a characteristic, role, period or composite — not \
                   admissible inside a closed function or lambda body" ])
  in
  match closed with
  | Some vs -> vs
  | None ->
  match c with
  | Vergelijking { op; links; rechts; periode; _ } ->
      let lt = Option.bind links (expr_type ctx ~binders)
      and rt = Option.bind rechts (expr_type ctx ~binders) in
      let ordering =
        match op with
        | Kleiner | Kleiner_of_gelijk | Groter | Groter_of_gelijk -> `Num
        | Eerder | Eerder_of_gelijk | Later | Later_of_gelijk -> `Date
        | Gelijk | Ongelijk -> `Eq
      in
      (match ordering, lt, rt with
       | `Num, Some l, Some r ->
           (if not (is_number_like l) then
              [ v "WF-036" (sub "left") ("ordering operand is " ^ datatype_kind l) ]
            else [])
           @ (if not (is_number_like r) then
                [ v "WF-036" (sub "right") ("ordering operand is " ^ datatype_kind r) ]
              else [])
           @ (if is_number_like l && is_number_like r
                 && not (units_compatible_adopting links rechts (unit_of l) (unit_of r))
              then [ v "WF-036" at "ordering operands have incompatible units" ]
              else [])
       | `Date, Some l, Some r ->
           List.concat_map
             (fun (side, d) ->
               if is_date d then []
               else [ v "WF-036" (sub side) ("date-ordering operand is " ^ datatype_kind d) ])
             [ ("left", l); ("right", r) ]
           (* WF-036, the date-ordering precision clause: the two DATE
              REPRESENTATIONS are two comparable families, exactly as
              WF-038's ordering fold reads them — a day-precision date
              names a whole day, a millisecond-precision one a point
              inside a day, so no order runs between them. Each side is
              well-formed alone; their combination is not, so it is
              reported at the comparison node. The sibling equality arm
              below crosses the same seam ("equality across date
              precisions"); this arm is its ordering half. An operand
              whose type nothing declares defers (the outer match skips
              it); its own rule reports it. *)
           @ (match l, r with
              | Datum_in_dagen, Datum_tijd | Datum_tijd, Datum_in_dagen ->
                  [ v "WF-036" at
                      "date ordering across two date precisions (day and millisecond), which \
                       share no order" ]
              | _ -> [])
       | `Eq, Some l, Some r -> (
           match l, r with
           | Enumeratie { domein = Some d1; _ }, Enumeratie { domein = Some d2; _ }
             when norm d1 <> norm d2 ->
               [ v "WF-036" at "equality across different enumeration domains" ]
           | Sum_type { domein = d1; _ }, Sum_type { domein = d2; _ } when norm d1 <> norm d2 ->
               (* WF-061 (nominal): two sum-typed operands of one comparison
                  MUST name the same declaring domain *)
               [ v "WF-061" at
                   ("comparison across different sum-type domains '" ^ d1 ^ "' and '" ^ d2 ^ "'") ]
           | Struct_type { domein = d1; _ }, Struct_type { domein = d2; _ } when norm d1 <> norm d2 ->
               (* WF-072 (nominal): two struct-typed operands of one comparison
                  MUST name the same declaring domain *)
               [ v "WF-072" at
                   ("comparison across different struct-type domains '" ^ d1 ^ "' and '" ^ d2 ^ "'") ]
           | Datum_in_dagen, Datum_tijd | Datum_tijd, Datum_in_dagen ->
               [ v "WF-036" at "equality across date precisions" ]
           | a, b when is_number_like a && is_number_like b ->
               if not (units_compatible_adopting links rechts (unit_of a) (unit_of b)) then
                 [ v "WF-036" at "equality operands have incompatible units" ]
               else []
           | a, b when datatype_kind a <> datatype_kind b
                       && not (is_number_like a && is_number_like b) ->
               [ v "WF-036" at
                   ("equality across families: " ^ datatype_kind a ^ " vs " ^ datatype_kind b) ]
           | _ -> [])
       | _ -> [])
      @ whole_period_check periode [ links; rechts ]
      @ (match links with Some e -> walk_expr ctx ~binders ~subject ~body (sub "left") e | None -> [])
      @ (match rechts with Some e -> walk_expr ctx ~binders ~subject ~body (sub "right") e | None -> [])
  | Predicaat { soort; operand; dagsoort; periode; _ } ->
      let ot = Option.bind operand (expr_type ctx ~binders) in
      (match soort, ot with
       | Elfproef, Some d -> (
           match d with
           | Tekst -> []
           | Numeriek { geheel = true; eenheid = None; _ } -> []
           | _ ->
               [ v "WF-036" at
                   ("elevenTest operand is " ^ datatype_kind d
                    ^ ", must be text or a whole unitless number") ])
       | Numeriek_met_exact, Some d when d <> Tekst ->
           [ v "WF-036" at ("exactDigits operand is " ^ datatype_kind d ^ ", must be text") ]
       | Dagsoort, Some d when not (is_date d) ->
           [ v "WF-036" at ("day-kind predicate operand is " ^ datatype_kind d ^ ", must be a date") ]
       | _ -> [])
      @ (match soort, dagsoort with
         | Dagsoort, Some dk when not (mem_ci (dagsoort_namen ctx) dk) ->
             [ v "WF-034" at ("reference to undeclared day kind '" ^ dk ^ "'") ]
         | _ -> [])
      @ whole_period_check periode [ operand ]
      @ (match operand with Some e -> walk_expr ctx ~binders ~subject ~body (sub "operand") e | None -> [])
  | Kenmerk_check { kenmerk; periode; navigatie_binding; eigenaar; _ } ->
      (* WF-034 (widened, maintainer decision 2026-09-05, sixth wave; IR-SPEC
         ch. 4.6 (q)): a characteristic check's EXPLICIT OWNER. `host`
         names the object type the characteristic is read on when it is not
         the enclosing form's subject. It is a reference, and it is READ — as
         the node key of the dependency edge this check contributes
         ([dep_cond] below keys `n_char` on it, falling back to the subject
         when it is absent; the Drools backend does the identical thing in its
         own stratification and producer/consumer graphs) — but nothing
         crossed it against the object-type table.

         An undeclared owner therefore builds a dependency edge on a node no
         form writes: the stratification reads an empty neighbour, and the
         check's producer is invisible to it. In the Drools backend the same
         name degrades one step further and SILENTLY: its loader resolves the
         field through a by-name object-type lookup that yields null for an
         unknown name, so `host` becomes absent, the two transpiler
         branches stop emitting their "hosted elsewhere" marker, and the check
         is re-attributed to the subject — a different reading of the same
         document, with nothing said.

         Carried by 2 of the 190 characteristic checks the four zones hold
         (2026-09-05), both of them in the negatief zone, and both resolving:
         `wf031-dagsoort-leest-kenmerk` (a day-kind condition, where the walk
         knows no subject at all, so this field is the only owner there is)
         and `wf046-negated-read-in-recursive-cycle` (where it repeats the
         subject). No golden carries the form.

         Which owner is AUTHORITATIVE was settled on 2026-09-10 (maintainer
         decision, IR-SPEC ch. 4.6 (q1)): the FIELD is. The declared-
         characteristic cross below is therefore keyed on `host` where the
         field is present and on `subject` only where it is absent, and an
         owner that does not declare the named characteristic is WF-100. *)
      (match eigenaar with
       | Some e when objecttype ctx e = None ->
           [ v "WF-034" (sub "host")
               ("characteristic check's host '" ^ e
              ^ "' is not a declared object type: the check resolves against no declaration") ]
       | _ -> [])
      @ (* THE HOST (WF-109, maintainer decision 2026-09-19; IR-SPEC ch. 4.6
           (q2)). The owner names the object type the check LIVES ON, and the
           object it names must be one the position actually binds — exactly
           one. Every position binds the enclosing form's subject and nothing
           else, except the fact-creation guard, which binds the anchor AND the
           target ([fc_doel] above).

           Until this rule the ZERO case was an EXECUTOR refusal: `ref` and the
           ASP backend faulted on a host they had no object for, while the two
           Drools branches dropped or markered the form — a verifier-clean
           document no executor answered. Moving the verdict here is what lets
           the remaining forms be carried by all four.

           The TWO case is new and is not a corner: with a self-typed anchor/target
           pair (and with any common supertype of the two) the type string names
           both objects and settles nothing. No precedence is invented for it,
           because nothing wrote one.

           Two deliberate step-asides. An UNDECLARED owner belongs to WF-034
           (q) above: a name that resolves to no declaration has no satisfaction
           question to ask, and reporting both would make one defect two rules.
           A SUBJECT-LESS position (the day-kind condition, which binds no
           object at all) has nothing to cross, and WF-031 refuses that document
           for reading object state in the first place.

           The target counts as bound only for a NON-navigated check WITHOUT a
           whole-period qualifier. A navigated owner names the host while the
           characteristic lives on the type the hop reaches, a shape the
           producer never emits and no executor pair has been measured on. A
           QUALIFIED one is fenced for a measured reason (2026-09-19): in both
           bean-model branches the ch. 3.9 fold arm runs BEFORE the host test,
           so a target host under `gedurende het gehele jaar` is decided by the
           fold's own subject search and not by this field — measured on one
           verifier-clean document, `ref` and the ASP backend answered the target
           reading, the exec branch reached the same number by another route,
           and the DRL-text branch lowered the fold onto the ANKER and sank the
           package at compile (`Unable to Analyse Expression
           rsGeheelPeriode($subject.getGehesen(), "JAAR", …)`). The §8.2 family
           is another card's; until it norms the shape the anchor stays the only
           bound object here, and this fence falls the day that card lands.

           TWO questions, not one, and the difference is the whole of the
           2026-09-19 fix. A binding SATISFIES the host when every object it can
           hold is one (`T ⊑ O`) — that is the reading, and it must hold for
           exactly one binding. A binding merely OVERLAPS the host when SOME
           object could be both ([populations_overlap]) — that is the ambiguity,
           and no second binding may have it. Counting by `⊑` alone let a
           subtype edge smuggle a second candidate past: `Sein ⊑ Kustvak` makes
           a concrete `Sein` fill an anchor declared `Kustvak`, so a `Sein` host
           names two objects at run time and one on paper. Overlap alone is not
           enough either: it would ADMIT a host naming a strict subtype of the
           binding, where the reading holds for some objects and faults for
           others — a verdict the wire cannot settle, so it keeps failing. *)
      (match eigenaar with
       | Some e when objecttype ctx e <> None ->
           let bound =
             match fc_doel, navigatie_binding, periode with
             | Some d, None, None -> (
                 match subject with
                 | Some a -> [ ("anchor", a); ("target", d) ]
                 | None -> [ ("target", d) ])
             | _ -> ( match subject with Some s -> [ ("subject", s) ] | None -> [])
           in
           let named (r, t) = "the " ^ r ^ " '" ^ t ^ "'" in
           let here = String.concat " and " (List.map named bound) in
           let hits = List.filter (fun (_, t) -> is_sub ctx ~sub:t ~super:e) bound in
           let near = List.filter (fun (_, t) -> populations_overlap ctx.doc t e) bound in
           let fail m = [ v "WF-109" (sub "host") m ] in
           let hosted = "the characteristic check is hosted on '" ^ e ^ "', which " in
           (match bound, hits, near with
            | [], _, _ -> []
            (* the one reading the wire settles: one binding is it, and no other
               binding's objects can be *)
            | _, [ _ ], [ _ ] -> []
            | _, [ h ], others ->
                fail
                  (hosted ^ named h
                 ^ " is — but so can "
                  ^ String.concat " and "
                      (List.map named (List.filter (fun b -> b <> h) others))
                 ^ " be, because the model declares a type below both: the host names one object \
                    on paper and two at run time")
            | _, [], [] -> fail (hosted ^ "no object bound here can be: this position binds " ^ here)
            | _, [], others ->
                fail
                  (hosted ^ "no object bound here IS: only SOME objects of "
                  ^ String.concat " and " (List.map named others)
                 ^ " are one, so the host would hold for some of them and fault for the rest")
            | _, hs, _ ->
                fail
                  (hosted
                  ^ String.concat " and " (List.map named hs)
                 ^ " both are: the host names one object and this names two"))
       | _ -> [])
      @ (match kenmerk with
       | None ->
           (* WF-034 (widened, maintainer decision 2026-09-04, third wave):
              a characteristic check resolves through exactly one field, and
              every arm below — the declared-characteristic cross here,
              WF-043's granularity cross, WF-053's navigated hop, and the
              dependency graph's edge — sits behind `Some k`, so an ABSENT
              name walked past all of them at once. Measured: 89 of the
              goldens' 96 characteristic-check positions stayed
              verifier-clean when emptied, while both executors fault
              loudly on the same document (`ref`
              "missing KenmerkCheck.kenmerk", clingo "missing kenmerk"). *)
           [ v "WF-034" at
               "characteristic check without a characteristic name: it resolves against no \
                declaration" ]
       | Some _ -> [])
      @ (* THE DECLARATION KEY (WF-100, maintainer decision 2026-09-10). Both
           crosses below resolve the characteristic against ONE object type,
           and which one is the whole content of "the field is authoritative":
           `host` where the field is present, the enclosing form's subject
           only where it is absent. Two numbers because they are two
           obligations — WF-034 keeps the subject-keyed resolution it always
           had, WF-100 is the owner's own — and one key, computed once, so the
           existence cross and WF-043's time-dependence cross can never drift
           apart. Scoped to the NON-navigated check: on a navigated one the
           characteristic lives on the REACHED type (the WF-053 arm below
           crosses it there), so an owner is not that type and crossing it
           would reject a form nothing has decided. *)
      (let decl_key = match eigenaar with Some _ as e -> e | None -> subject in
       let keyed_wf = match eigenaar with Some _ -> "WF-100" | None -> "WF-034" in
       let keyed_at = match eigenaar with Some _ -> sub "host" | None -> at in
       (match kenmerk, navigatie_binding, decl_key with
        | Some k, None, Some otn -> (
            match objecttype ctx otn with
            | Some ot when not (has_kenmerk ot k) ->
                [ v keyed_wf keyed_at
                    ("'" ^ k ^ "' is not a declared characteristic of '" ^ otn ^ "'") ]
            | _ -> [])
        | _ -> [])
       @ (match periode, kenmerk, navigatie_binding, decl_key with
          | Some _, Some k, None, Some otn -> (
              match objecttype ctx otn with
              | Some ot
                when not (List.exists (fun (n, _) -> norm n = norm k) ot.kenmerk_granulariteit) ->
                  [ v "WF-043" at "wholePeriod qualifier on a non-time-dependent characteristic" ]
              | _ -> [])
          | _ -> []))
      @ (* WF-053: the navigated characteristic check — one hop, its binding;
           to-one only (the v0 resolver's NAAR_VEEL marker, lifted);
           the characteristic lives on the REACHED type. Without a binding
           the check reads the subject itself. *)
      (match (navigatie_binding, subject) with
       | None, _ | Some _, None -> []
       | Some rb, Some st -> (
           let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
           match resolve_binding ctx rb with
           | Error e -> [ v "WF-053" at e ]
           | Ok (_, bound, opposite) ->
               if not (fills ctx ~inst:st ~role:opposite) then
                 [ v "WF-053" at
                     (Printf.sprintf
                        "the near side of '%s' fills '%s', but the check's subject is '%s'"
                        rb.rb_feittype (filler opposite) st) ]
               else
                 (if bound.rol_meervoudig then
                    [ v "WF-053" at
                        (Printf.sprintf
                           "the navigated check reaches the to-many role '%s' — a to-many \
                            step needs an aggregation, never a silent first pick"
                           (Option.value bound.rol_naam ~default:"?")) ]
                  else [])
                 @
                 match kenmerk with
                 | Some k -> (
                     match objecttype ctx (filler bound) with
                     | Some ot when not (has_kenmerk ot k) ->
                         [ v "WF-034" at
                             ("'" ^ k ^ "' is not a declared characteristic of '"
                            ^ filler bound ^ "'") ]
                     | _ -> [])
                 | None -> []))
  | Role_membership { fact_type; role; counterpart_type; _ } -> (
      (* WF-034, the role check (IR-SPEC ch. 4.6, the v1 role arm): the
         node names the relation and the role, so the whole of the reference
         is the pair, crossed against the declarations — the fact type is
         declared, the role is one of its roles by declared name (the plural
         is presentation and does not name a role here, as in a role
         binding), and the SUBJECT fills that role. The last is the mistake a
         producer makes: a role of the right fact type on the wrong side
         names a link the subject can never be in, and the check then asks
         for nothing. One-sided, on the [fills] line of every near-side
         question in this file: an instance of the subject type fills a role
         declared at any of its supertypes (ch. 2.6). A subject-less position
         (a day-kind condition) crosses the declarations only; WF-031 refuses
         it for reading role state at all.

         The counterpart class: where the node narrows the other end
         of the link to [counterpart_type], that type is a declared object
         type ⊑ the declared filler of the other role — the narrowing a
         check such as "hij heeft een strandjutter" writes, where the role's
         filler is the supertype. A type outside that filler's chain names
         instances the relation can never hold at that end, so the check
         would ask for nothing, the same mistake one field further. A
         reciprocal fact type has one role, bound and opposite at once. *)
      match find_ci ctx.doc.feittypen fact_type with
      | None ->
          [ v "WF-034" (sub "factType")
              ("role check names fact type '" ^ fact_type
             ^ "', which is not declared: the check resolves against no declaration") ]
      | Some (ft : feittype) -> (
          let named (r : rol) =
            match r.rol_naam with Some n -> norm n = norm role | None -> false
          in
          match List.filter named ft.rollen with
          | [] ->
              [ v "WF-034" (sub "role")
                  ("'" ^ role ^ "' is not a declared role of fact type '" ^ fact_type
                 ^ "' (a role check names the role by its declared name)") ]
          | [ r ] -> (
              (match subject with
              | Some st when not (fills ctx ~inst:st ~role:r) ->
                  [ v "WF-034" (sub "role")
                      (Printf.sprintf
                         "the subject '%s' does not fill the role '%s' of fact type '%s', which \
                          is filled by '%s': the check names a link the subject can never be in"
                         st role fact_type
                         (Option.value r.rol_objecttype_naam ~default:"?")) ]
              | _ -> [])
              @
              match counterpart_type with
              | None -> []
              | Some ct -> (
                  let other =
                    match List.filter (fun x -> x != r) ft.rollen with
                    | [ o ] -> Some o
                    | [] -> Some r (* the reciprocal role is its own other end *)
                    | _ -> None
                  in
                  match (objecttype ctx ct, other) with
                  | None, _ ->
                      [ v "WF-034" (sub "counterpartType")
                          ("role check's counterpart type '" ^ ct
                         ^ "' is not a declared object type: the check resolves against no \
                            declaration") ]
                  | Some _, Some (o : rol)
                    when not
                           (is_sub ctx ~sub:ct
                              ~super:(Option.value o.rol_objecttype_naam ~default:"")) ->
                      [ v "WF-034" (sub "counterpartType")
                          (Printf.sprintf
                             "the counterpart type '%s' is no subtype of '%s', the filler of the \
                              other role '%s' of fact type '%s': the check names a link whose \
                              other end can never be one"
                             ct
                             (Option.value o.rol_objecttype_naam ~default:"?")
                             (Option.value o.rol_naam ~default:"?")
                             fact_type) ]
                  | _ -> []))
          | _ ->
              [ v "WF-034" (sub "role")
                  ("role name '" ^ role ^ "' is ambiguous within fact type '" ^ fact_type ^ "'")
              ]))
  | Periode_conditie { vanaf; tot; _ } ->
      (if vanaf = None && tot = None then
         [ v "WF-042" at "time window without any bound" ]
       else [])
      @ List.concat_map
          (fun (n, e) ->
            match e with
            | Some e ->
                (match expr_type ctx ~binders e with
                 | Some d when not (is_date d) ->
                     [ v "WF-042" (sub n) ("window bound is " ^ datatype_kind d ^ ", must be a date") ]
                 | _ -> [])
                @ wf111_bound ctx ~subject (sub n) n e
                @ walk_expr ctx ~binders ~subject (sub n) e
            | None -> [])
          [ ("from", vanaf); ("to", tot) ]
  | Samengesteld { kwantor; drempel; onderdelen; _ } ->
      (match kwantor with
       | Ten_minste | Ten_hoogste | Precies when drempel < 1 ->
           [ v "WF-041" at (Printf.sprintf "counting quantifier with threshold %d < 1" drempel) ]
       | _ -> [])
      @ wf112_position at kwantor onderdelen
      @ List.concat
          (List.mapi
             (fun i c -> walk_cond ctx ~binders ~subject (Printf.sprintf "%s/parts/%d" at i) c)
             onderdelen)
  (* WF-108 — the QUANTIFIER FOLD's shape (IR-SPEC ch. 4.5; semantics
     RUNTIME-SEMANTICS ch. 3.9a). Five arms, and each closes a way the node
     could state a quantification with nothing to quantify over.

     The range arms (c) deliberately carry their OWN number rather than
     widening WF-102: that rule is scoped to a fold's ARGUMENT — the source
     position, which this node does not have — and a negatief directory must
     be able to name the rule it violates. What is shared is the MACHINERY,
     not the number: [wf047_binding] and [resolve_binding] are the fold's,
     called from here, so a range can never be judged by two different
     engines (until L2 also [wf048_derivable], which left with the
     bindingless range, B12). An unresolvable binding therefore still reports
     WF-047, exactly as a fold's does. *)
  | Kwantorvouw k ->
      let sub p = at ^ "/" ^ p in
      (* (a) the quantifier: all, none and — since 2026-09-16 — the three
         counting forms, Compound's vocabulary over INSTANCES instead of
         parts. `the` is a definite description, not a quantification over a
         set, and is refused by name rather than ignored: a reader that took
         an unhandled quantifier for all would answer the opposite of "geen
         van de". *)
      (match k.kv_kwantor with
       | Alle | Geen | Ten_minste | Ten_hoogste | Precies -> []
       | De -> [ v "WF-108" (sub "quantifier") quantifier_fold_the_refusal ])
      @ wf112_position at k.kv_kwantor (List.filter_map Fun.id [ k.kv_filter; k.kv_toets ])
      (* (e) the threshold: present EXACTLY under a counting quantifier. A
         counting fold without one names no number to compare the count with;
         one under all/none records a quantification the quantifier field
         contradicts, and a reader is entitled to trust either. The VALUE is
         not this rule's — a bound below 1 is WF-041, reported at this
         position exactly as an unresolvable binding reports WF-047, on the
         shared-rule discipline arm (c) states. *)
      @ (match (k.kv_kwantor, k.kv_drempel) with
         | (Ten_minste | Ten_hoogste | Precies), None ->
             [ v "WF-108" at
                 "counting quantifier fold without a threshold: the verdict compares the number \
                  of contributors satisfying the matrix with a threshold, and there is none to \
                  compare it with" ]
         | (Alle | Geen | De), Some d ->
             [ v "WF-108" (sub "threshold")
                 (Printf.sprintf
                    "quantifier fold with a threshold (%d) under a NON-counting quantifier: the \
                     threshold is read only by the counting quantifiers, and a number no reader \
                     consults states a quantification the quantifier contradicts"
                    d) ]
         | (Ten_minste | Ten_hoogste | Precies), Some d when d < 1 ->
             [ v "WF-041" (sub "threshold")
                 (Printf.sprintf "counting quantifier with threshold %d < 1" d) ]
         | _ -> [])
      (* (b) the population. WF-034 owns the name resolution of every other
         type reference; this arm owns the node's REQUIREMENT that one be
         there at all, because a quantifier with no element type ranges over
         nothing a reader can name. *)
      @ (match k.kv_element_objecttype with
         | None ->
             [ v "WF-108" (sub "elementType")
                 "quantifier fold without an elementType: the population it ranges over is \
                  unnamed" ]
         | Some t when objecttype ctx t = None ->
             [ v "WF-034" (sub "elementType")
                 ("quantifier-fold element type '" ^ t ^ "' is not declared") ]
         | Some _ -> [])
      (* (d) the matrix. A quantifier with no matrix states a range and
         asks nothing of it; the loader carries the absence so this rule
         can name it. *)
      @ (match k.kv_toets with
         | None ->
             [ v "WF-108" (sub "test")
                 "quantifier fold without a test: the matrix is what every contributor must \
                  satisfy, and a quantifier over no condition has no verdict" ]
         | Some _ -> [])
      (* (c) the range, one meaning with two spellings — the WF-102
         discipline under this node's own number. Its presence and its one
         spelling are the loader's since L2 (B12): the range is written,
         never derived. *)
      @ (match k.kv_bron_pad with
         | Some pad ->
             (if List.length pad >= 2 then []
                else
                  [ v "WF-108" (sub "sourcePath")
                      (Printf.sprintf
                         "a quantifier fold's sourcePath carries at least TWO steps, found %d — \
                          the one-step range is spelled sourceRole"
                         (List.length pad)) ])
             @ (let rec chain i from_t = function
                  | [] -> []
                  | (rb : rol_binding) :: rest -> (
                      let step = sub ("sourcePath/" ^ string_of_int i) in
                      match resolve_binding ctx rb with
                      | Error e -> [ v "WF-047" step e ]
                      | Ok (_, bound, other) ->
                          let bound_filler = Option.value bound.rol_objecttype_naam ~default:"" in
                          let joins =
                            match from_t with
                            | None -> []
                            | Some ft when fills ctx ~inst:ft ~role:other -> []
                            | Some ft ->
                                [ v "WF-108" step
                                    (Printf.sprintf
                                       "the opposite role '%s' fills '%s', but this step starts \
                                        at '%s' (%s) — a quantifier range is a connected walk"
                                       (Option.value other.rol_naam ~default:"?")
                                       (Option.value other.rol_objecttype_naam ~default:"")
                                       ft
                                       (if i = 0 then "the quantifier's subject"
                                        else "the type the previous step reaches")) ]
                          in
                          let lands =
                            if rest <> [] then []
                            else
                              match k.kv_element_objecttype with
                              | Some el when not (type_compatible ctx bound_filler el) ->
                                  [ v "WF-108" step
                                      (Printf.sprintf
                                         "the last step's bound role '%s' fills '%s', but the \
                                          quantifier's elementType is '%s' — the path must \
                                          land on the element type"
                                         (Option.value bound.rol_naam ~default:"?")
                                         bound_filler el) ]
                              | _ -> []
                          in
                          joins @ lands @ chain (i + 1) (Some bound_filler) rest)
                in
                chain 0 subject pad)
         | None -> (
             match (k.kv_element_objecttype, k.kv_bron_rol) with
             | Some el, Some rb ->
                 wf047_binding ctx (sub "sourceRole") rb ~bound_filler:(Some el)
                   ~other_filler:subject
             | _ -> []))
      (* the matrix and the subselection are read with the ELEMENT as
         subject — the same re-anchoring the fold's filter gets
         (walk_expr's Functie arm), so an element-own read resolves against
         the element type and not against the outer subject *)
      @ (let esubject =
           match k.kv_element_objecttype with Some el -> Some el | None -> subject
         in
         let walk_sub p = function
           | Some c -> walk_cond ctx ~binders ~subject:esubject ~body (sub p) c
           | None -> []
         in
         walk_sub "filter" k.kv_filter @ walk_sub "test" k.kv_toets)
  | Regelversie _ -> [] (* row-30 split; admissibility OPEN *)
  | Conditie_onopgelost _ -> []

let expression_layer ctx =
  List.concat_map
    (fun (r : resolved_regel) ->
      let at = r.rr_pointer in
      let subject =
        match r.doel with
        | Some (Attribuut_ref { objecttype; _ }) -> objecttype
        | _ -> None
      in
      (* The target lvalue is WF-022's job, not part of the walk. Since L2 it
         carries no type of its own (B13): the attribute declaration it writes
         is the one source, crossed where it is declared, so the target has
         no annotation left to cross here. *)
      (* WF-056 (rule level): a pro-rata value on a temporal target MUST
         bucket at the target's declared granularity; WF-058 states the
         same facet for the ch. 3.8 day count *)
      (match (r.doel, r.waarde) with
      | ( Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }),
          Some
            (Functie
               { kind = (Tijdsevenredig_deel | Aantal_dagen_in) as kind;
                 granulariteit = Some g; _ }) ) -> (
          let wf, what =
            if kind = Tijdsevenredig_deel then ("WF-056", "pro-rata") else ("WF-058", "day-count")
          in
          match objecttype ctx ot with
          | Some otd -> (
              match List.find_opt (fun (n, _) -> norm n = norm a) otd.attribuut_granulariteit with
              | Some (_, Some tg) when tg <> g ->
                  [ v wf (at ^ "/value")
                      (what ^ " bucket granularity differs from the target's declared granularity")
                  ]
              | Some (_, Some _) | None -> []
              (* TimelineFacet.granularity is a required enum since M1 *)
              | Some (_, None) -> assert false)
          | None -> [])
      | _ -> [])
      (* WF-058 (rule level): the day count MUST carry a gedurende
         condition over an own temporal characteristic and takes no
         arguments (the counting fold has no base) *)
      @ (match r.waarde with
        | Some (Functie ({ kind = Aantal_dagen_in; _ } as f)) ->
            (if f.argumenten <> [] then
               [ v "WF-058" (at ^ "/value/arguments") "dayCount takes no arguments" ]
             else [])
            @ (match f.filter with
              | None -> [ v "WF-058" (at ^ "/value") "dayCount without a gedurende condition" ]
              | Some (Kenmerk_check { kenmerk = Some k; navigatie_binding = None; negatie = false; _ }) -> (
                  match Option.bind subject (objecttype ctx) with
                  | Some otd
                    when not
                           (List.exists
                              (fun (n, _) -> norm n = norm k)
                              otd.kenmerk_granulariteit) ->
                      [ v "WF-058" (at ^ "/value/filter")
                          ("gedurende over '" ^ k ^ "', which is not a temporal characteristic")
                      ]
                  | _ -> [])
              | Some _ ->
                  [ v "WF-058" (at ^ "/value/filter")
                      "the day-count gedurende condition is an own temporal characteristic check"
                  ])
        | _ -> [])
      @ (match r.waarde with Some e -> walk_expr ctx ~binders:[] ~subject (at ^ "/value") e | None -> [])
      @ (match r.rr_voorwaarde with
         | Some c -> walk_cond ctx ~binders:[] ~subject (at ^ "/condition") c
         | None -> []))
    ctx.doc.resolved_regels
  @ List.concat_map
      (fun (r : resolved_object_creatie) ->
        let at = r.oc_pointer in
        List.concat
          (List.mapi
             (fun j t ->
               match t.toe_waarde with
               | Some e ->
                   (* member values evaluate on the creation SUBJECT
                      (ch. 5.5) — the walking type for navigation and
                      fold derivation is oc_subject, never the created
                      type *)
                   walk_expr ctx ~binders:[] ~subject:r.oc_subject
                     (Printf.sprintf "%s/initialisations/%d/value" at j) e
               | None -> [])
             r.toekenningen)
        @ (match r.oc_voorwaarde with
           | Some c -> walk_cond ctx ~binders:[] ~subject:r.oc_subject (at ^ "/condition") c
           | None -> [])
        @ (match r.oc_verbindende_rol with
           | Some rb ->
               (* the back-link binding (extension): the bound role is
                  the created side, the opposite the subject side *)
               wf047_binding ctx (at ^ "/connectingRole") rb ~bound_filler:r.nieuw_type
                 ~other_filler:r.oc_subject
           | None -> []))
      ctx.doc.object_creaties
  @ List.concat_map
      (fun (r : resolved_verdeling) ->
        match r.vd_voorwaarde with
        | Some c -> walk_cond ctx ~binders:[] ~subject:r.bron_type (r.vd_pointer ^ "/condition") c
        | None -> [])
      ctx.doc.verdelingen
  @ List.concat_map
      (fun (r : resolved_consistentie) ->
        match r.assertie with
        | Some c ->
            walk_cond ctx ~binders:[] ~subject:r.cons_subject (r.cons_pointer ^ "/assertion") c
        | None -> [])
      ctx.doc.consistenties
  @ List.concat_map
      (fun (i, r) ->
        match r.rd_voorwaarde with
        | Some c ->
            walk_cond ctx ~binders:[] ~subject:None
              (Printf.sprintf "/dayKindDefinitions/%d/condition" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)
  @ List.concat_map
      (fun (r : resolved_feit_creatie) ->
        match r.fc_voorwaarde with
        | Some c ->
            (* the ONE position that binds two objects: the anchor it iterates
               and the target its join chain reaches (WF-109, ch. 4.6 (q2)) *)
            walk_cond ctx ~binders:[] ~subject:r.anker ?fc_doel:r.fc_doel
              (r.fc_pointer ^ "/condition") c
        | None -> [])
      ctx.doc.feit_creaties
  @ List.concat_map
      (fun (i, (l : prioritized_defaults)) ->
        (* the expression walk descends into sub-defaults (ch. 7.9): a
           sub-default's justification and consequence are checked exactly as
           a root default's, the exception group inheriting the root subject *)
        let rec default_walk prefix (r : default) =
          (match r.default_voorwaarde with
          | Some c ->
              walk_cond ctx ~binders:[] ~subject:(Some l.pd_onderwerp)
                (prefix ^ "/justification") c
          | None -> [])
          @ (match r.default_consequentie with
            | Some e ->
                walk_expr ctx ~binders:[] ~subject:(Some l.pd_onderwerp)
                  (prefix ^ "/consequence") e
            | None -> [])
          @ (match r.default_excepties with
            | Some egs ->
                List.concat
                  (List.mapi
                     (fun k (eg : exception_group) ->
                       List.concat
                         (List.mapi
                            (fun m d ->
                              default_walk
                                (Printf.sprintf "%s/exceptions/members/%d/defaults/%d" prefix k m)
                                d)
                            eg.eg_defaults))
                     egs.egs_leden)
            | None -> [])
        in
        List.concat
          (List.mapi
             (fun j r ->
               default_walk (Printf.sprintf "/prioritizedDefaults/%d/defaults/%d" i j) r)
             l.defaults))
      (List.mapi (fun i l -> (i, l)) ctx.doc.prioritized_defaults)

(* ===================================================================== *)
(* WF-045 / WF-046 — the dependency graph (RUNTIME-SEMANTICS ch. 6).
   Nodes: attribute, characteristic, channel (rule name), population
   (object type), identified up to normalisation. Edges run producer ->
   consumer; a negated/negation-quantified characteristic read, every
   read under a leeg/gevuld test (either polarity, since 2026-09-23),
   every read in a quantifier fold's range under all/none/atMost/
   exactly and in its matrix under none/atMost/exactly (since
   2026-09-23) and a regelversie channel test are negative edges.
   WF-045: the graph minus
   the creation members of recursive rule groups is acyclic.
   WF-046: no cycle in the full graph carries a negative edge.
   Recorded narrowings (ch. 6.1): relation membership CONDITION reads
   are treated as input; a fold reads the element population beside its
   relation; day-kind definitions contribute no edges. Relation reads (a
   RoleBinding on a fold, an allocation, a navigation or a creation hop)
   are relation-node edges (n_rel). *)

let n_attr ot a = "attr:" ^ norm ot ^ ":" ^ norm a
let n_char ot k = "char:" ^ norm ot ^ ":" ^ norm k
let n_chan rule = "chan:" ^ norm (strip_version rule)
let n_pop ot = "pop:" ^ norm ot

(* A population node is the set of instances of one declared type, so a
   creation of T grows T's population AND every ancestor's: every T
   instance IS an instance of each type on its chain (ch. 2.2), and a form
   that iterates an ancestor's population is handed the new instance.

   Keyed on the WRITTEN name alone the write and that read were different
   nodes, and a cycle running between them left the graph entirely —
   measured 2026-09-12 on a creation whose `createdType` is a subtype of its
   own `subject`: verifier-clean, and `ref` ending in `no fixpoint within
   100 passes`. The same creation with the two names EQUAL was already
   refused, so a subtype was an escape hatch from a rule the document in
   one type already met.

   Only the WRITE closes, and only upward. Creating an ancestor does not
   grow a subtype's population, so closing reads downward would invent
   edges and refuse the mirror document (subject `Wrakrecht`, new type
   `Strandrecht`) that terminates perfectly well. And only an OBJECT
   creation closes: a fact creation mints no instance — its population
   write is ch. 6.1's conservative proxy beside the relation node — so
   lifting a proxy up a chain buys false edges and no missed cycle. *)
let pop_writes ctx t =
  match declared_objecttype ctx t with
  | None -> [ n_pop t ]
  | Some ot ->
      List.sort_uniq compare
        (n_pop t
         :: List.filter_map (fun (a : objecttype) -> Option.map n_pop a.ot_naam) (chain_from ctx ot))

(* The same closure for a MEMBER write, and [~over] is what decides how far
   it reaches, because the two writing forms do not put the value on the
   same set of instances.

   [`Extension] — the form ITERATES a type: a rule target, an allocation's
   target or remainder, a defaults group's target. It writes `m` on every
   instance of [ot], so the write is seen wherever those instances are read,
   and the instance sets of [ot] and S overlap exactly when the two are
   ⊑-COMPARABLE — S above [ot] (an [ot] instance is an S), S below [ot] (an
   S instance is an [ot]). Both directions, over the comparable types that
   carry the member.

   [`Minted] — an object creation's member assignment. It writes `m` on ONE
   new instance whose concrete type is exactly [ot], because creation is NOT
   polymorphic (ch. 2.2), so the reach is [ot] and its ANCESTORS and never a
   strict subtype — the same asymmetry, and the same argument, as
   [pop_writes] above. Measured 2026-09-12 before the split: a creation of a
   SUPERTYPE, assigning an inherited member, drew an edge to a rule anchored
   at the subtype it can never mint, and one document went verifier-clean
   ->  WF-045 FAIL with ref refusing and clingo still answering — a latent
   over-refusal that splits the differential square.

   Incomparable types are left apart in both modes, and that is the whole
   reason [`Extension] is a comparability walk and not [declaring_type].
   Keying both sides on the declaring type is one line shorter and merges
   the two SIBLING subtypes of one parent into one node: measured
   2026-09-12, that refuses a document (two incomparable subtypes each
   computing one inherited attribute from the other) that ref replays to a
   clean PASS — no instance is of both types, so there is no cycle to
   refuse. Under the comparability reading the same document stays clean and
   the two spellings of ONE chain still meet.

   Reads stay literal on purpose: every legal read spelling of `m` is a type
   comparable with some writer's owner, so the write side alone already
   draws every real edge. *)
let member_writes ctx ~kind ~over ot m =
  let node = match kind with `Attribute -> n_attr | `Characteristic -> n_char in
  let carries s =
    match objecttype ctx s with
    | None -> false
    | Some e -> ( match kind with `Attribute -> has_attr e m | `Characteristic -> has_kenmerk e m)
  in
  let reached =
    match over with
    | `Minted -> (
        match declared_objecttype ctx ot with
        | None -> []
        | Some o -> List.filter_map (fun (a : objecttype) -> a.ot_naam) (chain_from ctx o))
    | `Extension ->
        List.map
          (fun (key, (o : objecttype)) -> Option.value o.ot_naam ~default:key)
          (List.filter
             (fun (key, (o : objecttype)) ->
               let n = Option.value o.ot_naam ~default:key in
               is_sub ctx ~sub:n ~super:ot || is_sub ctx ~sub:ot ~super:n)
             ctx.doc.objecttypen)
  in
  List.sort_uniq compare
    (node ot m :: List.filter_map (fun n -> if carries n then Some (node n m) else None) reached)

(* the link set of one declared relation (ch. 6.1): written by the fact
   creations that create it, read by every hop over it and by every
   fold/allocation that names it in a role binding. *)
let n_rel ft = "rel:" ^ norm ft

(* Edge kinds (RUNTIME-SEMANTICS ch. 6.1): positive; stratum-strict (a
   non-characteristic read by a form with irrevocable writes); negative
   (stratum-strict AND forbidden on any cycle, WF-046). *)
type dep_edge = Dep_pos | Dep_strict | Dep_neg

type dep_form = {
  df_at : string;
  df_name : string;
  df_recursive_creation : bool;
  df_writes : string list;
  df_reads : (string * dep_edge) list;
}

(* [subject] resolves owner-less characteristic reads; [ctx] resolves the
   navigated shapes' hop relations *)
let rec dep_expr ctx ~subject acc (e : expr) =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some ot; navigatie_bindingen; _ } ->
      (* a navigated read consumes the link set of every hop relation
         (ch. 6.1) — the edge that orders navigation-after-creation. Since
         L2 the bindings name every relation it walks (B11); an own read
         walks none *)
      let rel_edges =
        match navigatie_bindingen with
        | Some hops -> List.map (fun (rb : rol_binding) -> (n_rel rb.rb_feittype, Dep_pos)) hops
        | None -> []
      in
      rel_edges @ ((n_attr ot a, Dep_pos) :: acc)
  | Attribuut_ref _ | Literal _ | Parameter_ref _ | Variabele_ref _ | Constante _
  | Bound_day _ | Expr_onopgelost _ ->
      acc
  | Tekst_reeks { delen; _ } ->
      List.fold_left
        (fun acc -> function
          | Letterlijk _ -> acc
          | Ingevoegd { expr = Some e } -> dep_expr ctx ~subject acc e
          | Ingevoegd { expr = None } -> acc)
        acc delen
  | Binair { links; rechts; _ } -> dep_opt ctx ~subject (dep_opt ctx ~subject acc links) rechts
  | Functie { kind; argumenten; element_objecttype; leegwaarde; filter; bron_rol; bron_pad; _ } ->
      let acc =
        match element_objecttype with Some el -> (n_pop el, Dep_pos) :: acc | None -> acc
      in
      let acc =
        (* WF-102: a multi-step range reads EVERY step's link set — the
           same edge the one-step form draws, once per relation walked, so
           a creation that fills an intermediate relation still orders
           before the fold *)
        match bron_pad with
        | Some pad ->
            List.fold_left
              (fun acc (rb : rol_binding) -> (n_rel rb.rb_feittype, Dep_pos) :: acc)
              acc pad
        | None -> acc
      in
      let acc =
        (* a bound fold reads the named relation's link set — the edge a
           fact creation's write reaches, from either end (ch. 6.1) *)
        match bron_rol with
        | Some (rb : rol_binding) -> (n_rel rb.rb_feittype, Dep_pos) :: acc
        | None -> acc
      in
      (* the count source names the relation's role, not an attribute: it
         reads the link set drawn above and the population, never a member *)
      let count_source =
        match (kind, element_objecttype, argumenten, bron_rol, bron_pad) with
        | Aantal, Some el, [ Attribuut_ref { attribuut = Some a; _ } ], _, _
          when bron_rol <> None || bron_pad <> None -> (
            match objecttype ctx el with
            | Some ot -> not (has_attr ot a || has_kenmerk ot a)
            | None -> false)
        | _ -> false
      in
      let acc =
        if count_source then acc else List.fold_left (dep_expr ctx ~subject) acc argumenten
      in
      let acc = dep_opt ctx ~subject acc leegwaarde in
      (* the filter's reads are the ELEMENT's (ch. 6.1): a filter over a
         derived characteristic of the contributor type must sit above
         that characteristic's writer *)
      let fsubject = match element_objecttype with Some el -> Some el | None -> subject in
      (match filter with Some c -> dep_cond ctx ~subject:fsubject ~neg:false acc c | None -> acc)
  | Instance_set_union { branches; _ } ->
      (* every branch is a fold source of its own and draws a fold source's
         edges: the population it ranges over, the relation it walks, and
         its filter's reads AT THE CONTRIBUTOR (ch. 6.1) — so a union whose
         branch filters a derived characteristic still sits above that
         characteristic's writer, once per branch *)
      List.fold_left
        (fun acc (b : instance_set_branch) ->
          let acc =
            match b.isb_element_type with Some el -> (n_pop el, Dep_pos) :: acc | None -> acc
          in
          let acc = (n_rel b.isb_source_role.rb_feittype, Dep_pos) :: acc in
          match b.isb_filter with
          | Some c -> dep_cond ctx ~subject:b.isb_element_type ~neg:false acc c
          | None -> acc)
        acc branches
  | Afgerond { basis; _ } -> dep_opt ctx ~subject acc basis
  | Begrensd { basis; minimum; maximum; _ } ->
      dep_opt ctx ~subject (dep_opt ctx ~subject (dep_opt ctx ~subject acc basis) minimum) maximum
  | Dimensie_cel { attribuut; _ } -> dep_opt ctx ~subject acc attribuut
  | Temporal_read { basis; datum; _ } ->
      (* the read consumes its timeline reference and its date operand;
         the base is an attribute/parameter reference whose own case
         adds the right edge *)
      dep_expr ctx ~subject (dep_expr ctx ~subject acc basis) datum
  | Construct { argument; _ } -> dep_opt ctx ~subject acc argument
  | Match { scrutinee; arms; _ } ->
      (* the reads are the scrutinee's and every arm body's; a binder is
         lexical, contributing no attribute/parameter edge of its own *)
      List.fold_left
        (fun acc (_, _, arm_body) -> dep_expr ctx ~subject acc arm_body)
        (dep_expr ctx ~subject acc scrutinee)
        arms
  | Binder_ref _ -> acc
  (* the record forms (Phase 5c): the reads live in the field values and the
     accessed base *)
  | Struct_literal { velden; _ } ->
      List.fold_left (fun acc (_, value) -> dep_expr ctx ~subject acc value) acc velden
  | Field_access { base; _ } -> dep_expr ctx ~subject acc base
  (* the Let binding (Phase 5c): its reads live in the value and the body; the
     binder is lexical, contributing no attribute/parameter edge of its own.
     Call-by-need does not narrow this: a read that never happens still needs
     its producer stratified below (ch. 6.1 is a static over-approximation) *)
  | Let { value; body = lbody; _ } ->
      dep_expr ctx ~subject (dep_expr ctx ~subject acc value) lbody
  (* the fold family (row 10): the reads live in the list source, the init and
     the filter condition; a closed lambda/function body reads no attributes,
     so it contributes no edges — but the walk is harmless and total *)
  | List_literal { elements; _ } -> List.fold_left (dep_expr ctx ~subject) acc elements
  | Lambda { body = lbody; _ } -> dep_expr ctx ~subject acc lbody
  | If_then_else { condition; then_; else_; _ } ->
      let acc = dep_cond ctx ~subject ~neg:false acc condition in
      dep_expr ctx ~subject (dep_expr ctx ~subject acc then_) else_
  | Invoke { arguments; _ } -> List.fold_left (dep_expr ctx ~subject) acc arguments
  | Fold { list; functie; init; _ } ->
      dep_fold_fn ctx ~subject (dep_expr ctx ~subject (dep_expr ctx ~subject acc list) init) functie
  | Map_list { list; functie; _ } ->
      dep_fold_fn ctx ~subject (dep_expr ctx ~subject acc list) functie
  | Filter_list { list; condition; _ } ->
      dep_cond ctx ~subject ~neg:false (dep_expr ctx ~subject acc list) condition
  | List_length { list; _ } -> dep_expr ctx ~subject acc list

and dep_fold_fn ctx ~subject acc = function
  | Fn_lambda { body = lbody; _ } -> dep_expr ctx ~subject acc lbody
  | Fn_name _ -> acc

and dep_opt ctx ~subject acc = function Some e -> dep_expr ctx ~subject acc e | None -> acc

and dep_cond ctx ~subject ~neg acc (c : conditie) =
  match c with
  | Vergelijking { links; rechts; _ } ->
      dep_opt ctx ~subject (dep_opt ctx ~subject acc links) rechts
  (* WF-046, the leeg/gevuld test (maintainer decision 2026-09-23): every
     read under the test is a NEGATIVE edge, in BOTH polarities. The test
     depends on absence whether it carries `negated: false` or `true`, so
     the gevuld form is negative for the same reason the leeg form is, and a
     cycle through either is forbidden even where WF-045 admits the cycle
     (a recursive creation group). Until then the operand was read like any
     value, positive or stratum-strict, and the one-generation progression
     that discharges strictness on an admissible creation cycle was taken
     to discharge this read as well. It does not: the creation acts on an
     absence that the cycle fills afterwards. Measured 2026-09-23 on two
     probes, both verifier-clean before this arm. With empty in the
     creation's condition (R1), ref minted a chain in which every
     creation's justification is false in ref's own final world, and the
     ASP backend found no stable model. With filled negated there (R3), ref
     answered merk(W1) = 1 because its pass runs rules before creations;
     the opposite order leaves merk(W1) leeg, and the ASP backend found five
     stable models, one per chain length. The witnesses are
     corpus/negatief/wf046-leegtest-*.

     Every node the operand reads turns negative, the hop relations of a
     navigated operand included: an absent link makes the operand leeg, so
     the test reads the absence of the link as well. The arm reaches the
     test in every position the walk visits (a rule or creation condition,
     an assertion, a fold filter, a quantifier matrix, a composite member).
     It covers these two kinds and nothing more. The quantifier fold has
     its own arm below. Other conditions that hold on absence without a
     `negated` field (a comparison against leeg, a count tested against
     zero, a propositional none over a comparison, arithmetic that
     coalesces leeg to 0) keep their positive or stratum-strict reading
     here. They were measured on the same cycle shape and are recorded as
     open, not decided. *)
  | Predicaat { soort = Leeg | Gevuld; operand; _ } ->
      List.map (fun (node, _) -> (node, Dep_neg)) (dep_opt ctx ~subject [] operand) @ acc
  | Predicaat { operand; _ } -> dep_opt ctx ~subject acc operand
  | Kenmerk_check { kenmerk = Some k; negatie; navigatie_binding = Some rb; _ } -> (
      (* the navigated check: the characteristic lives on the REACHED
         type, and the hop relation itself is read positively (only the
         characteristic sits under the negation — an absent link makes
         the check false either way, never true). An unresolvable hop
         contributes no edges; WF-053 reports it. *)
      let kind = if neg || negatie then Dep_neg else Dep_pos in
      match resolve_binding ctx rb with
      | Ok (_, r, _) ->
          (n_rel rb.rb_feittype, Dep_pos)
          :: (n_char (Option.value r.rol_objecttype_naam ~default:"") k, kind)
          :: acc
      | Error _ -> acc)
  | Kenmerk_check { kenmerk = Some k; eigenaar; negatie; navigatie_binding = None; _ } -> (
      (* once negative, conservatively negative — double negation does
         not cancel (ch. 6.1) *)
      let kind = if neg || negatie then Dep_neg else Dep_pos in
      match (match eigenaar with Some _ as e -> e | None -> subject) with
      | Some ot -> (n_char ot k, kind) :: acc
      | None -> acc)
  | Kenmerk_check _ -> acc
  | Role_membership _ -> acc
  | Periode_conditie { vanaf; tot; _ } -> dep_opt ctx ~subject (dep_opt ctx ~subject acc vanaf) tot
  | Samengesteld { kwantor; onderdelen; _ } ->
      let neg' =
        match kwantor with
        | Geen | Ten_hoogste | Precies -> true (* negation-like: conservative, ch. 6.1 *)
        | De | Alle | Ten_minste -> neg
      in
      List.fold_left (fun acc c -> dep_cond ctx ~subject ~neg:neg' acc c) acc onderdelen
  (* the quantifier fold (ch. 3.9a), maintainer decision 2026-09-23: the
     RANGE is a negative edge under all, none, atMost and exactly,
     because the verdict can be lost when the range grows; under atLeast
     it cannot, and the range reads positively. The MATRIX, comparisons
     included, is negative only under none, atMost and exactly; under
     atLeast and all the verdict is monotone in the matrix. The range
     is the reach (the bound role or path) and the filter, which are the
     fold's range fields in ch. 4.5: a member that newly passes the filter
     joins the range. Under a negative context both follow the context, so
     a fold under a negation-like composite stays negative throughout.

     Negative here means EVERY read under that part, comparisons and
     positive characteristics included, not only the characteristic reads
     the [neg] flag reaches elsewhere. Reads that are negative on their own
     (a negated characteristic, a leeg/gevuld test, a channel test) stay
     negative in a monotone part too.

     Why not simply the context for every quantifier: a variant that walked
     filter and matrix negatively under ALL five quantifiers refused a
     recursive creation under "rang < 5 en ten minste 1 opvolger heeft
     rang > 3", on which ref and clingo agree (nothing is minted, at rang 1
     and at rang 4). That document is the golden
     corpus/golden/jutterswet-opvolgerquorum; the negative witnesses are
     corpus/negatief/wf046-kwantorvouw-*.

     The range is written since L2 (B12): the loader admits the node with
     exactly one of sourceRole and sourcePath, so the reach is always the relations
     it names. *)
  | Kwantorvouw { kv_kwantor; kv_element_objecttype; kv_filter; kv_toets; kv_bron_rol; kv_bron_pad; _ }
    ->
      let range_neg = neg || kv_kwantor <> Ten_minste in
      let matrix_neg = neg || not (kv_kwantor = Ten_minste || kv_kwantor = Alle) in
      let reach =
        List.map
          (fun (rb : rol_binding) -> n_rel rb.rb_feittype)
          ((match kv_bron_rol with Some rb -> [ rb ] | None -> [])
          @ Option.value kv_bron_pad ~default:[])
      in
      let part ~negative = function
        | Some c ->
            let reads = dep_cond ctx ~subject:kv_element_objecttype ~neg:negative [] c in
            if negative then List.map (fun (node, _) -> (node, Dep_neg)) reads else reads
        | None -> []
      in
      List.map (fun node -> (node, if range_neg then Dep_neg else Dep_pos)) reach
      @ part ~negative:range_neg kv_filter
      @ part ~negative:matrix_neg kv_toets
      @ acc
  | Regelversie { regel = Some r; _ } -> (n_chan r, Dep_neg) :: acc
  | Regelversie _ -> acc
  | Conditie_onopgelost _ -> acc

(* A form of the six named forms (IR-SPEC ch. 3, Versioning) is recorded
   at the wire pointer of its version, which the loader sets
   (`/rules/<list>/<f>/versions/<v>`): the pointer WF-045 and WF-046 cite,
   and the key an executor's stratum lookup asks for. A defaults group is
   recorded at `/prioritizedDefaults/<i>`. *)
let dep_forms ctx : dep_form list =
  let recursive_members =
    List.concat_map
      (fun g -> if g.recursief then List.map (fun n -> norm (strip_version n)) (some_names g.leden) else [])
      ctx.doc.regel_groepen
  in
  let cond ~subject c = match c with Some c -> dep_cond ctx ~subject ~neg:false [] c | None -> [] in
  (* a form with irrevocable writes (characteristic attribution, channel
     deposit, creation) must sit strictly above the producers of every
     non-characteristic value it reads — a transient it acted on would
     persist (ch. 6.1). Positive characteristic reads stay positive
     (monotone-safe). *)
  let strict_unless_char reads =
    List.map
      (fun (node, kind) ->
        if kind = Dep_pos && not (String.length node >= 5 && String.sub node 0 5 = "char:") then
          (node, Dep_strict)
        else (node, kind))
      reads
  in
  let regels =
    List.map
      (fun (r : resolved_regel) ->
        let at = r.rr_pointer in
        let name = Option.value r.rr_naam ~default:at in
        match r.doel with
        | Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }) ->
            let writes =
              member_writes ctx ~over:`Extension
                ~kind:(if r.rr_soort = Some Kenmerktoekenning then `Characteristic else `Attribute)
                ot a
            in
            let subject = Some ot in
            let reads = dep_opt ctx ~subject (cond ~subject r.rr_voorwaarde) r.waarde in
            let reads =
              if r.rr_soort = Some Kenmerktoekenning then strict_unless_char reads else reads
            in
            Some { df_at = at; df_name = name; df_recursive_creation = false; df_writes = writes; df_reads = reads }
        | _ -> None)
      ctx.doc.resolved_regels
    |> List.filter_map (fun x -> x)
  in
  let verdelingen =
    List.map
      (fun (v : resolved_verdeling) ->
        let at = v.vd_pointer in
        let name = Option.value v.vd_naam ~default:at in
        let bt = Option.value v.bron_type ~default:"" in
        let dt = Option.value v.doel_type ~default:"" in
        let writes =
          (match v.doel_attribuut with
          | Some a -> member_writes ctx ~over:`Extension ~kind:`Attribute dt a
          | None -> [])
          @ (match v.restant_attribuut with
            | Some a -> member_writes ctx ~over:`Extension ~kind:`Attribute bt a
            | None -> [])
        in
        let reads =
          (n_pop dt, Dep_pos)
          :: (n_rel v.verbindende_rol.rb_feittype, Dep_pos)
          :: []
          @ (match v.bron_attribuut with Some a -> [ (n_attr bt a, Dep_pos) ] | None -> [])
          @ List.filter_map
              (fun a -> Option.map (fun a -> (n_attr dt a, Dep_pos)) a)
              [ v.rato_attribuut; v.maximum_attribuut; v.groepeer_attribuut ]
          @ cond ~subject:v.bron_type v.vd_voorwaarde
        in
        { df_at = at; df_name = name; df_recursive_creation = false; df_writes = writes; df_reads = reads })
      ctx.doc.verdelingen
  in
  let consistenties =
    List.map
      (fun (c : resolved_consistentie) ->
        let at = c.cons_pointer in
        let name = Option.value c.cons_naam ~default:at in
        {
          df_at = at;
          df_name = name;
          df_recursive_creation = false;
          df_writes = [ n_chan name ];
          df_reads = strict_unless_char (cond ~subject:c.cons_subject c.assertie);
        })
      ctx.doc.consistenties
  in
  let uniciteiten =
    List.map
      (fun (u : resolved_uniciteit) ->
        let at = u.uni_pointer in
        let name = Option.value u.uni_naam ~default:at in
        let subj = Option.value u.uni_subject ~default:"" in
        (* a parent-scoped uniqueness reads the scope relation's link set
           (ch. 6.1), named by its binding (B8) *)
        let scope_read =
          match u.scope_rol with
          | Some rb -> [ (n_rel rb.rb_feittype, Dep_strict) ]
          | None -> []
        in
        {
          df_at = at;
          df_name = name;
          df_recursive_creation = false;
          df_writes = [ n_chan name ];
          df_reads =
            scope_read
            @ List.map
                (fun k -> (n_attr subj (Option.value k ~default:""), Dep_strict))
                u.sleutels;
        })
      ctx.doc.uniciteiten
  in
  let uniciteit_unies =
    List.map
      (fun (u : resolved_uniciteit_unie) ->
        let at = u.uu_pointer in
        let name = Option.value u.uu_naam ~default:at in
        {
          df_at = at;
          df_name = name;
          df_recursive_creation = false;
          df_writes = [ n_chan name ];
          df_reads =
            List.concat_map
              (fun (b : uniciteit_unie_bron) ->
                match b.uub_type with
                | Some t ->
                    List.map
                      (fun k -> (n_attr t (Option.value k ~default:""), Dep_strict))
                      b.uub_sleutels
                | None -> [])
              u.bronnen;
        })
      ctx.doc.uniciteit_unies
  in
  let creaties =
    List.map
      (fun (o : resolved_object_creatie) ->
        let at = o.oc_pointer in
        let name = Option.value o.oc_naam ~default:at in
        let nt = Option.value o.nieuw_type ~default:"" in
        let subject = o.oc_subject in
        let writes =
          pop_writes ctx nt
          @ (match (o.oc_verbindende_rol, subject) with
             | Some rb, _ ->
                 (* the explicit back-link binding (extension) names
                    the written relation directly *)
                 [ n_rel rb.rb_feittype ]
             | None, Some st -> (
                 (* the back-link (ch. 5.5): a uniquely-derivable
                    connecting relation is written on both ends. A
                    self-typed TWO-role pair is role-ambiguous — the
                    derivation mirrors v0's null (no link) there, so no
                    relation write either; the reciprocal shape stays
                    unambiguous *)
                 match connecting_candidates ctx ~from_type:st ~to_type:nt with
                 | [ fn ] -> (
                     match find_ci ctx.doc.feittypen fn with
                     | Some (ft : feittype)
                       when norm st = norm nt && not ft.wederkerig ->
                         []
                     | _ -> [ n_rel fn ])
                 | _ -> [])
             | None, None -> [])
          @ List.concat_map
               (fun (t : toekenning) ->
                 match t.toe_attribuut with
                 | Some a ->
                     (* `Minted, not `Extension: this value lands on the ONE
                        instance the creation makes, and its concrete type
                        is exactly nt (creation is not polymorphic) *)
                     member_writes ctx ~over:`Minted
                       ~kind:(if t.toe_kenmerk then `Characteristic else `Attribute)
                       nt a
                 | None -> [])
               o.toekenningen
        in
        let reads =
          strict_unless_char
            ((* iterating the subject population is a read (ch. 6.1):
                it orders creation-after-creation, and a creation whose
                subject is its own created type becomes a WF-045 cycle
                instead of a diverging fixpoint or an unbounded
                grounding *)
             (match subject with Some st -> [ (n_pop st, Dep_pos) ] | None -> [])
            @ List.fold_left
                (fun acc (t : toekenning) -> dep_opt ctx ~subject acc t.toe_waarde)
                (cond ~subject o.oc_voorwaarde)
                o.toekenningen)
        in
        {
          df_at = at;
          df_name = name;
          df_recursive_creation = List.mem (norm (strip_version name)) recursive_members;
          df_writes = writes;
          df_reads = reads;
        })
      ctx.doc.object_creaties
  in
  let feit_creaties =
    List.map
      (fun (f : resolved_feit_creatie) ->
        let at = f.fc_pointer in
        let name = Option.value f.fc_naam ~default:at in
        (* the creation writes the created relation's link set and reads
           the link set of every hop it traverses (ch. 6.1) — the edge that
           orders creation-after-creation *)
        let rel_writes = [ n_rel f.fc_feittype ] in
        let hop_reads =
          List.map (fun (t : tussenstap) -> (n_rel t.ts_rol.rb_feittype, Dep_pos)) f.tussenstappen
          @ [ (n_rel f.join_rol.rb_feittype, Dep_pos) ]
        in
        {
          df_at = at;
          df_name = name;
          (* ch. 5.5: a relation creation may be a member of a recursive
             rule group (idempotent, no guard needed) — WF-045 removes
             recursive-group creation members of either kind *)
          df_recursive_creation = List.mem (norm (strip_version name)) recursive_members;
          df_writes =
            (match f.fc_doel with Some d -> [ n_pop d ] | None -> []) @ rel_writes;
          df_reads =
            strict_unless_char (hop_reads @ cond ~subject:f.anker f.fc_voorwaarde);
        })
      ctx.doc.feit_creaties
  in
  let defaults_groups =
    (* ch. 7.4/WF-051: every default-justification read — and, on
       attribute defaults, every consequence read (the gevuld gate makes
       it decision-bearing) — is a displacement read: a negative edge
       toward the group's write. The initialisation default's implicit
       input-emptiness justification reads nothing (ch. 7.3). Ch. 7.9:
       every DESCENDANT read (justifications and consequences, recursively
       through sub-defaults) folds into the same root negative edges — so a
       cycle through a sub-default read is caught by WF-051 unchanged. *)
    List.mapi
      (fun i (l : prioritized_defaults) ->
        let at = Printf.sprintf "/prioritizedDefaults/%d" i in
        let name = Option.value l.pd_naam ~default:at in
        let rec default_reads (r : default) =
          (match r.default_voorwaarde with
          | Some c -> dep_cond ctx ~subject:(Some l.pd_onderwerp) ~neg:false [] c
          | None -> [])
          @ (match r.default_consequentie with
            | Some e -> dep_expr ctx ~subject:(Some l.pd_onderwerp) [] e
            | None -> [])
          @ (match r.default_excepties with
            | Some egs ->
                List.concat_map
                  (fun (eg : exception_group) -> List.concat_map default_reads eg.eg_defaults)
                  egs.egs_leden
            | None -> [])
        in
        let reads = List.concat_map default_reads l.defaults in
        let writes =
          (match l.pd_kenmerk with
          | Some k -> member_writes ctx ~over:`Extension ~kind:`Characteristic l.pd_onderwerp k
          | None -> [])
          @
          match l.pd_attribuut with
          | Some a -> member_writes ctx ~over:`Extension ~kind:`Attribute l.pd_onderwerp a
          | None -> []
        in
        {
          df_at = at;
          df_name = name;
          df_recursive_creation = false;
          df_writes = writes;
          df_reads = List.map (fun (node, _) -> (node, Dep_neg)) reads;
        })
      ctx.doc.prioritized_defaults
  in
  regels @ verdelingen @ consistenties @ uniciteiten @ uniciteit_unies @ creaties
  @ feit_creaties @ defaults_groups

let wf045_046 ctx =
  let forms = Array.of_list (dep_forms ctx) in
  let nforms = Array.length forms in
  let writers = Hashtbl.create 16 in
  Array.iteri (fun i f -> List.iter (fun n -> Hashtbl.add writers n i) f.df_writes) forms;
  let succ = Array.make nforms [] in
  Array.iteri
    (fun ci f ->
      List.iter
        (fun (node, kind) ->
          List.iter
            (fun pi -> if not (List.mem (ci, kind) succ.(pi)) then succ.(pi) <- (ci, kind) :: succ.(pi))
            (Hashtbl.find_all writers node))
        f.df_reads)
    forms;
  (* WF-045: the graph minus recursive-creation forms must be acyclic *)
  let skip i = forms.(i).df_recursive_creation in
  let color = Array.make nforms 0 in
  let viol45 = ref [] in
  let rec dfs path i =
    if skip i then ()
    else
      match color.(i) with
      | 1 ->
          let rec take acc = function
            | [] -> acc
            | j :: _ when j = i -> j :: acc
            | j :: rest -> take (j :: acc) rest
          in
          let cyc = take [] path in
          let names = String.concat " -> " (List.map (fun j -> forms.(j).df_name) (cyc @ [ i ])) in
          viol45 :=
            v "WF-045" forms.(i).df_at ("dependency cycle through no recursive creation: " ^ names)
            :: !viol45
      | 2 -> ()
      | _ ->
          color.(i) <- 1;
          List.iter (fun (j, _) -> dfs (i :: path) j) succ.(i);
          color.(i) <- 2
  in
  Array.iteri (fun i _ -> if color.(i) = 0 then dfs [] i) forms;
  (* WF-046: a negative edge p->c on a cycle of the full graph, i.e. p
     reachable from c *)
  let reaches src dst =
    let seen = Array.make nforms false in
    let rec go i =
      if seen.(i) then false
      else begin
        seen.(i) <- true;
        i = dst || List.exists (fun (j, _) -> go j) succ.(i)
      end
    in
    go src
  in
  let viol46 = ref [] in
  Array.iteri
    (fun pi _ ->
      List.iter
        (fun (ci, kind) ->
          if kind = Dep_neg && reaches ci pi then
            viol46 :=
              v "WF-046" forms.(ci).df_at
                (Printf.sprintf
                   "negative dependency edge on a cycle: %s reads what %s produces while %s is reachable from it (no strata exist)"
                   forms.(ci).df_name forms.(pi).df_name forms.(pi).df_name)
              :: !viol46)
        succ.(pi))
    forms;
  List.rev !viol45 @ List.sort_uniq compare (List.rev !viol46)

(* ======================================================================= *)

(* WF-047 on the allocation's recipient role, which it carries since L2
   (B12): the binding resolves and its fillers agree with the source and
   target types. *)
let wf047_verdelingen ctx =
  List.concat_map
    (fun (r : resolved_verdeling) ->
      wf047_binding ctx (r.vd_pointer ^ "/connectingRole") r.verbindende_rol
        ~bound_filler:r.doel_type ~other_filler:r.bron_type)
    ctx.doc.verdelingen

(* WF-028 over the fact creation (IR-SPEC ch. 3.4). Since L2 the symbolic
   block is the whole creation (B7) and the loader reads it as required:
   the created relation and the roles its two ends fill, and since L4 the
   join chain as one path, anchor outward, whose last hop reaches the target
   type — the declared filler of the target role, which the wire no longer
   writes ([Ast], [fc_doel]). What is left to cross is its meaning — every
   binding resolves, the ends agree, and the path reaches the target through
   a relation that differs from the created one. *)
let wf028 ctx =
  List.concat
    (List.map
       (fun (fc : resolved_feit_creatie) ->
         let at = fc.fc_pointer in
         let ftn = fc.fc_feittype in
         let anker_rol = fc.anker_rol in
         let doel_rol = fc.doel_rol in
         let join = fc.join_rol in
         let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
         let created_checks =
           match find_ci ctx.doc.feittypen ftn with
           | None -> [ v "WF-028" at ("created fact type '" ^ ftn ^ "' is not declared") ]
           | Some (ft : feittype) -> (
               match ft.rollen with
               | [ a; b ] ->
                   let pick name =
                     let m r =
                       match r.rol_naam with
                       | Some n -> norm n = norm name
                       | None -> false
                     in
                     match (m a, m b) with
                     | true, false -> Ok a
                     | false, true -> Ok b
                     | true, true ->
                         Error ("role '" ^ name ^ "' is ambiguous within '" ^ ftn ^ "'")
                     | false, false ->
                         Error ("'" ^ name ^ "' is not a role of '" ^ ftn ^ "'")
                   in
                   (* ONE-SIDED. [anker] names the type the creation
                      ranges over; read extensionally that is every
                      instance ⊑ the name, and the created relation's role
                      must be able to hold all of them — so the node's type
                      sits AT OR BELOW the role's declared filler, never
                      above it. The target type is that filler itself since
                      L4 ([fc_doel]), so on the target side only the role's
                      resolution is left to cross.
                      The other direction (a role filling a strict
                      subtype while the node names the parent) writes
                      instances into a role that does not accept them,
                      and both OCaml executors carry it out silently
                      instead of refusing. WF-047's [agree] is two-sided
                      for a reason that does not hold here: there the
                      node names a type to READ through, and reading
                      upward from every filler is sound. *)
                   let end_check what name expected =
                     match pick name with
                     | Error e -> [ v "WF-028" at e ]
                     | Ok r -> (
                         match expected with
                         | Some t when not (is_sub ctx ~sub:t ~super:(filler r)) ->
                             [ v "WF-028" at
                                 (Printf.sprintf "%s '%s' fills '%s', but the node names '%s'"
                                    what name (filler r) t) ]
                         | _ -> [])
                   in
                   (if norm anker_rol = norm doel_rol then
                      [ v "WF-028" at "anchorRole and targetRole must be distinct roles" ]
                    else [])
                   @ end_check "anchorRole" anker_rol fc.anker
                   @ end_check "targetRole" doel_rol fc.fc_doel
               | _ ->
                   [ v "WF-028" at
                       ("created fact type '" ^ ftn ^ "' does not have exactly two roles") ])
         in
         let chain_checks =
           (* anchor -[path]-> ... -> target; each binding names the role the
              hop's far side fills, and the last hop is the one that reaches
              the target *)
           let step at_step cur rb =
             match resolve_binding ctx rb with
             | Error e -> Error [ v "WF-028" at_step e ]
             | Ok (_, bound, other) -> (
                 match cur with
                 | Some cur when not (fills ctx ~inst:cur ~role:other) ->
                     Error
                       [ v "WF-028" at_step
                           (Printf.sprintf
                              "hop over '%s': the near side fills '%s', but the chain stands \
                               at '%s'"
                              rb.rb_feittype (filler other) cur) ]
                 | _ -> Ok (Some (filler bound)))
           in
           let rec go j cur = function
             | [] -> (
                 match step (Printf.sprintf "%s/path/%d" at j) cur join with
                 | Error vs -> vs
                 | Ok reached ->
                     (* LITERAL, and left so on purpose. The unsound
                        direction — the chain reaches an ancestor of the
                        declared target, so a plain instance would be
                        linked as one — must refuse, and this comparison
                        already does. The sound direction (the chain reaches a strict SUBTYPE of
                        the target) would be a widening, and it is held back: the
                        fold and the allocation now both narrow a reached set to
                        their declared type (ch. 2.2), so the machinery exists,
                        but a creation LINKS rather than folds — a narrowed chain
                        would silently create fewer relations instead of fewer
                        contributors — and that deserves its own measurement
                        rather than riding along here. *)
                     (match (reached, fc.fc_doel) with
                     | Some r, Some d when norm r <> norm d ->
                         [ v "WF-028" at
                             (Printf.sprintf "the join chain reaches '%s', not the target '%s'"
                                r d) ]
                     | _ -> [])
                     @
                     if norm join.rb_feittype = norm ftn then
                       [ v "WF-028" at
                           "the created relation must differ from the relation of the final \
                            hop" ]
                     else [])
             | (t : tussenstap) :: rest -> (
                 match step (Printf.sprintf "%s/path/%d" at j) cur t.ts_rol with
                 | Error vs -> vs
                 | Ok reached -> go (j + 1) reached rest)
           in
           go 0 fc.anker fc.tussenstappen
         in
         created_checks @ chain_checks)
       ctx.doc.feit_creaties)

let wf050 ctx =
  List.concat
    (List.mapi
       (fun i (l : prioritized_defaults) ->
         let at = Printf.sprintf "/prioritizedDefaults/%d" i in
         let target_name =
           match (l.pd_kenmerk, l.pd_attribuut) with
           | Some k, None -> Some k
           | None, Some a -> Some a
           | _ -> None
         in
         (if l.defaults = [] then
            [ v "WF-050" at "a defaults group carries at least one default" ]
          else [])
         @ (match target_name with
           | None ->
               [ v "WF-050" at
                   "a defaults group targets exactly one of a characteristic or an attribute" ]
           | Some _ -> [])
         @ (match objecttype ctx l.pd_onderwerp with
           | None ->
               [ v "WF-050" at
                   ("defaults-group subject '" ^ l.pd_onderwerp ^ "' is not a declared object type") ]
           | Some ot -> (
               (match l.pd_kenmerk with
               | Some k when not (mem_ci (some_names ot.kenmerken) k) ->
                   [ v "WF-050" at
                       ("defaults target '" ^ k ^ "' is not a declared characteristic of '"
                      ^ l.pd_onderwerp ^ "'") ]
               | _ -> [])
               @
               match l.pd_attribuut with
               | Some a when not (has_attr ot a) ->
                   [ v "WF-050" at
                       ("defaults target '" ^ a ^ "' is not a declared attribute of '"
                      ^ l.pd_onderwerp ^ "'") ]
               | _ -> []))
         (* default shapes per target kind (ch. 7.1); the content-kind and
            justification-presence checks recurse into sub-defaults (ch. 7.9 —
            an exception group inherits the root target kind). A group-default
            (exceptions present, no own decision/consequence) delegates its
            decision to its exception groups: it is exempt from the content and
            justification requirements. A contentless default (neither content
            nor exception group) is WF-075's concern. *)
         @ (let is_char = l.pd_kenmerk <> None in
            let rec shape prefix (defaults : default list) =
              List.concat
                (List.mapi
                   (fun j (r : default) ->
                     let rat = Printf.sprintf "%s/defaults/%d" prefix j in
                     let has_content =
                       r.default_besluit <> None || r.default_consequentie <> None
                     in
                     let is_group = r.default_excepties <> None && not has_content in
                     (* present content must match the target kind *)
                     (match (is_char, r.default_besluit, r.default_consequentie) with
                     | true, _, Some _ ->
                         [ v "WF-050" rat "a characteristic default carries no consequence" ]
                     | false, Some _, _ ->
                         [ v "WF-050" rat "an attribute default carries no decision" ]
                     | _ -> [])
                     @ (match (r.default_initialisatie, is_group, r.default_voorwaarde) with
                       | true, _, Some _ ->
                           [ v "WF-050" rat
                               "the initialisation default carries no written justification (its \
                                justification is input-emptiness, ch. 7.3)" ]
                       | false, false, None when has_content ->
                           [ v "WF-050" rat "a non-initialisation default carries a justification" ]
                       | _ -> [])
                     @ (match r.default_excepties with
                       | Some egs ->
                           List.concat
                             (List.mapi
                                (fun k (eg : exception_group) ->
                                  shape
                                    (Printf.sprintf "%s/exceptions/members/%d" rat k)
                                    eg.eg_defaults)
                                egs.egs_leden)
                       | None -> []))
                   defaults)
            in
            shape at l.defaults)
         (* the initialisation default's position is a root-group property
            (a sub-default initialisation is ill-formed, WF-075) *)
         @ List.concat
             (List.mapi
                (fun j (r : default) ->
                  let rat = Printf.sprintf "%s/defaults/%d" at j in
                  (if r.default_initialisatie && l.pd_kenmerk <> None then
                     [ v "WF-050" rat
                         "an initialisation default on characteristic defaults is meaningless \
                          (characteristics start absent)" ]
                   else [])
                  @
                  if r.default_initialisatie && j <> List.length l.defaults - 1 then
                    [ v "WF-050" rat "the initialisation default sits lowest (last)" ]
                  else [])
                l.defaults)
         @ (match target_name with
           | None -> []
           | Some tn -> (
               (* the group key carries the target KIND: an attribute and a
                  characteristic may share a name (WF-002), and then they
                  are two independent targets, not a group *)
               (* keyed on the DECLARED subject, deliberately: see the
                  single-writer note at wf026. A group on 'Strandrecht.x'
                  and one on 'Wrakrecht.x' are two targets here, because
                  inheritance folds them into ONE effective stack rather
                  than putting two writers on one target. *)
               let key =
                 (if l.pd_kenmerk <> None then "K:" else "A:")
                 ^ norm l.pd_onderwerp ^ ":" ^ norm tn
               in
               let group_size =
                 List.length
                   (List.filter
                      (fun (o : prioritized_defaults) ->
                        match
                          (o.pd_kenmerk, o.pd_attribuut)
                        with
                        | Some k, None -> "K:" ^ norm o.pd_onderwerp ^ ":" ^ norm k = key
                        | None, Some a -> "A:" ^ norm o.pd_onderwerp ^ ":" ^ norm a = key
                        | _ -> false)
                      ctx.doc.prioritized_defaults)
               in
               (if group_size > 1 && not l.pd_ongeordend then
                  [ v "WF-050" at
                      ("multiple defaults groups on target '" ^ l.pd_onderwerp ^ "." ^ tn
                     ^ "' without the declared-unordered marking — priority is explicit \
                        (defaults) or explicitly absent ('unordered'), never implicit \
                        (ch. 7.2)") ]
                else [])
               @
               (* the ch. 7.1 single-writer lift: any plain writer beside
                  the defaults group would silently defeat its decisions *)
               (if
                  List.exists
                    (fun (r : resolved_regel) ->
                      match r.doel with
                      | Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }) ->
                          norm ot = norm l.pd_onderwerp
                          && norm a = norm tn
                          &&
                          (match (l.pd_kenmerk, r.rr_soort) with
                          | Some _, Some Kenmerktoekenning -> true
                          | Some _, _ -> false
                          | None, _ -> true)
                      | _ -> false)
                    ctx.doc.resolved_regels
                then
                  [ v "WF-050" at
                      ("a rule also writes defaults target '" ^ l.pd_onderwerp ^ "." ^ tn
                     ^ "' — an unordered plain writer would silently defeat the group's \
                        decisions") ]
                else [])
               @ (if
                    List.exists
                      (fun (oc : resolved_object_creatie) ->
                        (match oc.nieuw_type with
                        | Some nt -> norm nt = norm l.pd_onderwerp
                        | None -> false)
                        && List.exists
                             (fun (t : toekenning) ->
                               (match t.toe_attribuut with
                               | Some a -> norm a = norm tn
                               | None -> false)
                               && t.toe_kenmerk = (l.pd_kenmerk <> None))
                             oc.toekenningen)
                      ctx.doc.object_creaties
                  then
                    [ v "WF-050" at
                        ("an object creation also initialises defaults target '"
                       ^ l.pd_onderwerp ^ "." ^ tn
                       ^ "' — a creation-time write would silently defeat the group's \
                          decisions (and void the initialisation default's input-emptiness \
                          justification)") ]
                  else [])
               @
               match l.pd_attribuut with
               | None -> []
               | Some a ->
                   if
                     List.exists
                       (fun (vd : resolved_verdeling) ->
                         (match (vd.doel_type, vd.doel_attribuut) with
                         | Some dt, Some da ->
                             norm dt = norm l.pd_onderwerp && norm da = norm a
                         | _ -> false)
                         ||
                         match (vd.bron_type, vd.restant_attribuut) with
                         | Some bt, Some ra ->
                             norm bt = norm l.pd_onderwerp && norm ra = norm a
                         | _ -> false)
                       ctx.doc.verdelingen
                   then
                     [ v "WF-050" at
                         ("an allocation also writes defaults target '" ^ l.pd_onderwerp ^ "."
                        ^ a ^ "'") ]
                   else []))
         )
       ctx.doc.prioritized_defaults)

(* WF-052 (the WF-026/WF-050 single-writer family): a connecting
   relation whose CREATED-side role is to-one has at most one
   object-creation writer — the second writer's link on a shared
   subject is the second filler ref/ faults while a guardless stable
   model silently holds both; the divergence closes statically. Fact
   creations are not counted: their to-one writes are fault-paired
   (writer fault ≡ cardinality-guard UNSAT), and a to-many
   created-side role makes multi-writer creation harmless. *)
let wf052 ctx =
  let writers =
    List.filter_map
      (fun (o : resolved_object_creatie) ->
        let at = o.oc_pointer in
        match o.oc_verbindende_rol with
        | Some rb -> (
            (* the explicit binding names the created-side role *)
            match resolve_binding ctx rb with
            | Ok (_, bound, _) when not bound.rol_meervoudig ->
                Some (norm rb.rb_feittype, at)
            | _ -> None)
        | None -> (
            match (o.oc_subject, o.nieuw_type) with
            | Some st, Some nt -> (
                match connecting_candidates ctx ~from_type:st ~to_type:nt with
                | [ fn ] -> (
                    (* the created-side role, derived exactly as the
                       executors derive it — [connecting_role_candidates] is
                       ref's own list, and the created-side role is the
                       unique role [nt] fills whose fact type also relates
                       [st]. "The first role of [fn] that [nt] fills" is NOT
                       that question and answers it wrongly under ⊑: a
                       subtype instance fills its ANCESTOR's role too, so a
                       relation whose roles are (parent, to-many) and
                       (subtype, to-one) would read [rol_meervoudig] off the
                       parent role and the verdict would follow the order
                       the wire happens to list the roles in. A self-typed
                       two-role pair offers BOTH roles, derives nothing and
                       writes no link — the block ref keeps as well, and the
                       reason this arm needs no separate guard for it. *)
                    match connecting_role_candidates ctx ~from_type:st ~to_type:nt with
                    | [ r ] when not r.rol_meervoudig -> Some (norm fn, at)
                    | _ -> None)
                | _ -> None)
            | _ -> None))
      ctx.doc.object_creaties
  in
  List.concat_map
    (fun (ftn, at) ->
      let same = List.filter (fun (f, _) -> f = ftn) writers in
      if List.length same > 1 && snd (List.hd same) <> at then
        [ v "WF-052" at
            (Printf.sprintf
               "connecting relation '%s' has a to-one created-side role and %d object-creation \
                writers; at most one may write it"
               ftn (List.length same)) ]
      else [])
    writers

(* WF-079 (the WF-026/WF-050/WF-052 single-writer family, RUNTIME-SEMANTICS
   ch. 7.7): a (type, attribute) target that carries no prioritized defaults
   admits at most one unconditional, version-free computing rule
   (Gelijkstelling/Berekening). Two or more — the PHASE5D-DIFF §8.2
   un-pinnable form — over-determine the target with no explicit priority: ref
   oscillates (no fixpoint, ch. 5.7), clingo is silently multi-valued, and the
   pair falls through every zone. Characteristic attributions
   (Kenmerktoekenning) are exempt — a monotone many-grounds-one-truth (the
   penningrecht pair) — and a defaults group on the target routes multiplicity
   through WF-050 instead. Conditional writers (a guarded case split) carry
   their own priority and stay outside: only the bare (unconditional)
   collision is statically over-determined. Mirrors WF-026's version-stripped
   pairing. *)
(* Two arms. The FLAT arm keys on the declared type — see the single-writer
   note at wf026 — and pairs two writers declared on one type. The CHAIN arm
   pairs a writer declared on a type with one declared on a strict ancestor,
   a pair the declared-type keys cannot see ('Strandrecht.uitkering' and
   'Wrakrecht.uitkering' are two keys). Whether that pair is two writers is
   decided by the gate of RUNTIME-SEMANTICS ch. 7.10. Where the lower type's
   chain declares a defaults group on the target, the fold makes each
   ordinary rule the base of its level and the subtype's base replaces the
   inherited one: one writer, and what the fold cannot take the executors
   refuse by name ([Desugar.Unlowered]), a rule beside its own level's group
   being WF-050's. Where the chain declares none, neither rule enters a
   stack, the ancestor's rule quantifies over the subtype's instances as
   well, and both fire on them — measured 2026-09-25 on such a pair: ref and
   the Drools exec-model branch find no fixpoint, clingo derives two values
   and the DRL-text branch answers by document order. The arm reads the gate
   from [chain_group_keys], the predicate the fold reads, so the verifier and
   the executors cannot disagree on which pairs the fold absorbs. A pair is
   reported once, at the version on the lower type: that type is the first
   concrete type both fire on, and the key set only grows down the chain, so
   a type below it sees the same pair while it stays outside the gate and
   has it folded once inside. *)
let wf079 ctx =
  let has_defaults otn a =
    List.exists
      (fun (l : prioritized_defaults) ->
        match l.pd_attribuut with
        | Some la -> norm l.pd_onderwerp = norm otn && norm la = norm a
        | None -> false)
      ctx.doc.prioritized_defaults
  in
  let distinct_form n1 n2 =
    match (n1, n2) with Some x, Some y -> norm x <> norm y | _ -> true
  in
  let computing =
    List.filter_map
      (fun (r : resolved_regel) ->
        match (r.rr_soort, r.doel, r.rr_voorwaarde) with
        | ( Some (Gelijkstelling | Berekening),
            Some (Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ }),
            None ) ->
            Some (Option.map strip_version r.rr_naam, otn, a, r.rr_geldigheid, r.rr_pointer)
        | _ -> None)
      ctx.doc.resolved_regels
  in
  let writers =
    List.filter_map
      (fun (n, otn, a, g, at) ->
        if has_defaults otn a then None else Some (n, norm otn, norm a, g, at))
      computing
  in
  let rec pairs = function
    | [] -> []
    | (n1, ot1, a1, g1, at1) :: rest ->
        List.filter_map
          (fun (n2, ot2, a2, g2, _) ->
            let same_target = ot1 = ot2 && a1 = a2 in
            if same_target && distinct_form n1 n2 && validity_overlaps g1 g2 then
              Some
                (v "WF-079" at1
                   (Printf.sprintf
                      "two unconditional computing rules write target (%s, %s) with no \
                       prioritized defaults — priority is explicit (a defaults group) or \
                       explicitly absent, never implicit agenda order (ch. 7.7)"
                      ot1 a1))
            else None)
          rest
        @ pairs rest
  in
  let rec chain_pairs = function
    | [] -> []
    | (n1, ot1, a1, g1, at1) :: rest ->
        List.filter_map
          (fun (n2, ot2, a2, g2, at2) ->
            let lower =
              if norm ot1 = norm ot2 || norm a1 <> norm a2 then None
              else if is_sub ctx ~sub:ot1 ~super:ot2 then Some (ot1, a1, at1, ot2)
              else if is_sub ctx ~sub:ot2 ~super:ot1 then Some (ot2, a2, at2, ot1)
              else None
            in
            match lower with
            | Some (low, a, at, high)
              when distinct_form n1 n2
                   && validity_overlaps g1 g2
                   && not (List.mem ("A:" ^ norm a) (chain_group_keys ctx.doc low)) ->
                Some
                  (v "WF-079" at
                     (Printf.sprintf
                        "two unconditional computing rules write target (%s, %s) through its \
                         type chain — one declared on %s, one on %s — and no defaults group \
                         folds them into one stack, so both fire on a %s instance; priority is \
                         explicit (a defaults group, whose subtype base replaces the inherited \
                         base, ch. 7.10) or explicitly absent, never implicit agenda order \
                         (ch. 7.7)"
                        low a high low low))
            | _ -> None)
          rest
        @ chain_pairs rest
  in
  pairs writers @ chain_pairs computing

(* WF-054: parent-scoped uniqueness (the scope hop). Since L2 the scope
   binding IS the scoping (B8): a uniqueness carrying one is scoped, one
   without is not. The binding resolves, its near side fills the subject,
   and the scope role is to-one (a scope parent is one instance; the v0
   reference field). Scope-less instances do not participate at evaluation
   (RUNTIME-SEMANTICS ch. 5.9). *)
let wf054 ctx =
  List.concat_map
    (fun (r : resolved_uniciteit) ->
      let at = r.uni_pointer ^ "/scope" in
      match (r.scope_rol, r.uni_subject) with
      | None, _ -> []
      | Some _, None -> [] (* WF-030 reports the missing subject *)
      | Some rb, Some subj -> (
          let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
          match resolve_binding ctx rb with
          | Error e -> [ v "WF-054" at e ]
          | Ok (_, bound, opposite) ->
              if not (fills ctx ~inst:subj ~role:opposite) then
                [ v "WF-054" at
                    (Printf.sprintf
                       "the near side of '%s' fills '%s', but the uniqueness subject is '%s'"
                       rb.rb_feittype (filler opposite) subj) ]
              else if bound.rol_meervoudig then
                [ v "WF-054" at
                    (Printf.sprintf
                       "the scope role '%s' is to-many — a scope parent is one instance"
                       (Option.value bound.rol_naam ~default:"?")) ]
              else []))
    ctx.doc.uniciteiten

(* WF-023 (the shallow slice), at every attribute WRITE position — a rule's
   target, an object creation's attribute member, a defaults consequence:
   assignment typing between the target's DECLARED type and the value's
   static type ([expr_type]: a reference's declaration, every other value's
   annotation) — same family (numeric kinds count as one), compatible
   units (a unitless literal adopts), no contradictory sign restriction, and
   the DOMAIN clause: a keuzetype-typed side names the same keuzetype
   (variant and domain, ci, through a ListType element) as the other. The
   domain clause is the write half of WF-089's domain identity — WF-089
   crosses what a fold hands through, this crosses what is WRITTEN into a
   declaration — and the two together make ref's "match has no arm"
   fault unreachable on a verifier-clean document (fix round 1 of the card:
   without it a Literal or Construct of another domain, a ghost-domain
   literal and a list of another domain entered the read declaration
   verifier-clean at all three positions, and the object-creation and
   defaults positions crossed no soort at all). The precision gate needs
   expression-shape analysis and stays OPEN (the WF index records the
   reason). [target]/[value] are the expressions, for literal unit adoption
   (a defaults consequence and a member initialisation have no target
   expression). *)
let wf023_assignment at ~target ~value (dt : datatype) (wt : datatype) =
  let sign_conflict d w =
    match (d, w) with
    | Numeriek { teken = Some (Niet_negatief | Positief); _ },
      Numeriek { teken = Some Negatief; _ }
    | Numeriek { teken = Some Negatief; _ },
      Numeriek { teken = Some (Niet_negatief | Positief); _ } ->
        true
    | _ -> false
  in
  let same_family =
    datatype_kind dt = datatype_kind wt || (is_number_like dt && is_number_like wt)
  in
  (if not same_family then
     [ v "WF-023" at
         (Printf.sprintf "a %s value is assigned to a %s target" (datatype_kind wt)
            (datatype_kind dt)) ]
   else [])
  @ (if
       is_number_like dt && is_number_like wt
       && not (units_compatible_adopting target value (unit_of dt) (unit_of wt))
     then [ v "WF-023" at "assignment across incompatible units" ]
     else [])
  @ (if sign_conflict dt wt then
       [ v "WF-023" at "the value's sign restriction contradicts the target's" ]
     else [])
  @ (* WF-090's write half (struct-domain identity, ch. 2.4): the same
       three write positions, the struct sibling of the keuzetype clause
       below. Its own rule number, so the two are falsifiable apart; the
       one-sided arms follow the keuzetype clause and report only within
       one family. *)
  (match (struct_domain dt, struct_domain wt) with
   | None, None -> []
   | Some a, Some b when norm a = norm b -> []
   | Some a, Some b ->
       [ v "WF-090" at
           (Printf.sprintf
              "a value of the struct domain '%s' is assigned to a target of the struct domain \
               '%s'"
              b a) ]
   | Some a, None when same_family ->
       [ v "WF-090" at
           (Printf.sprintf
              "a %s value naming no struct domain is assigned to a target of the struct domain \
               '%s'"
              (datatype_kind wt) a) ]
   | None, Some b when same_family ->
       [ v "WF-090" at
           (Printf.sprintf
              "a value of the struct domain '%s' is assigned to a %s target naming no struct \
               domain"
              b (datatype_kind dt)) ]
   | _ -> [])
  @
  (* the domain clause; a one-sided keuzetype is reported only within
     one family (a list of a keuzetype against a list of something else,
     an enumeration without a domain) — across families the family
     message already names the defect *)
  match (keuzetype_domain dt, keuzetype_domain wt) with
  | None, None -> []
  | Some a, Some b when keuzetype_same a b -> []
  | Some a, Some b ->
      [ v "WF-023" at
          (Printf.sprintf "a value of the %s '%s' is assigned to a target of the %s '%s'"
             (keuzetype_variant b) (keuzetype_name b) (keuzetype_variant a)
             (keuzetype_name a)) ]
  | Some a, None when same_family ->
      [ v "WF-023" at
          (Printf.sprintf
             "a %s value naming no keuzetype domain is assigned to a target of the %s '%s'"
             (datatype_kind wt) (keuzetype_variant a) (keuzetype_name a)) ]
  | None, Some b when same_family ->
      [ v "WF-023" at
          (Printf.sprintf
             "a value of the %s '%s' is assigned to a %s target naming no keuzetype domain"
             (keuzetype_variant b) (keuzetype_name b) (datatype_kind dt)) ]
  | _ -> []

let wf023 ctx =
  (* position 1: the rule target — the attribute declaration it writes
     (WF-022 owns its resolution) against the value's static type *)
  List.concat_map
    (fun (r : resolved_regel) ->
      let at = r.rr_pointer in
      match (r.rr_soort, r.doel, r.waarde) with
      | Some (Gelijkstelling | Berekening | Initialisatie), Some doel, Some w -> (
          match (resolved_attr_datatype ctx doel, expr_type ctx ~binders:[] w) with
          | Some dt, Some wt -> wf023_assignment at ~target:r.doel ~value:r.waarde dt wt
          | _ -> [])
      | _ -> [])
    ctx.doc.resolved_regels
  (* position 2: an object creation's attribute member (WF-027 owns the
     characteristic member and the member's existence) *)
  @ List.concat_map
      (fun (r : resolved_object_creatie) ->
        match Option.bind r.nieuw_type (objecttype ctx) with
        | None -> []
        | Some ot ->
            List.concat_map
              (fun (j, (t : toekenning)) ->
                let tat = Printf.sprintf "%s/initialisations/%d" r.oc_pointer j in
                match (t.toe_kenmerk, t.toe_attribuut, t.toe_waarde) with
                | false, Some m, Some w -> (
                    match (attr_of ot m, expr_type ctx ~binders:[] w) with
                    | Some (Some dt), Some wt ->
                        wf023_assignment tat ~target:None ~value:(Some w) dt wt
                    | _ -> [])
                | _ -> [])
              (List.mapi (fun j t -> (j, t)) r.toekenningen))
      ctx.doc.object_creaties
  (* position 3: a defaults consequence, recursing into sub-defaults (ch.
     7.9 — an exception group inherits the root target) *)
  @ List.concat_map
      (fun (i, (l : prioritized_defaults)) ->
        match
          Option.bind l.pd_attribuut (fun a ->
              Option.bind (objecttype ctx l.pd_onderwerp) (fun ot -> attr_of ot a))
        with
        | Some (Some dt) ->
            let rec walk prefix (r : default) =
              (match
                 Option.bind r.default_consequentie (fun e ->
                     Option.map (fun wt -> (e, wt)) (expr_type ctx ~binders:[] e))
               with
              | Some (e, wt) ->
                  wf023_assignment (prefix ^ "/consequence") ~target:None ~value:(Some e) dt wt
              | None -> [])
              @
              match r.default_excepties with
              | Some egs ->
                  List.concat
                    (List.mapi
                       (fun k (eg : exception_group) ->
                         List.concat
                           (List.mapi
                              (fun m d ->
                                walk
                                  (Printf.sprintf "%s/exceptions/members/%d/defaults/%d" prefix k m)
                                  d)
                              eg.eg_defaults))
                       egs.egs_leden)
              | None -> []
            in
            List.concat
              (List.mapi
                 (fun j r -> walk (Printf.sprintf "/prioritizedDefaults/%d/defaults/%d" i j) r)
                 l.defaults)
        | _ -> [])
      (List.mapi (fun i l -> (i, l)) ctx.doc.prioritized_defaults)

(* WF-082: the statuteAnnotations extension field (IR-SPEC ch. 3.1) — the
   `// Bron:` statute annotations on a rule. When present it is a non-empty
   list of non-empty entries that carry no surrounding whitespace (the
   emitter trims and omits blanks; an empty or blank form would be a trace of
   absence, and absence leaves no trace). The field sits on the form since
   L4, one list for all its versions ([Ast]), so it is read once per form, on
   its first version. *)
let wf082 ctx =
  List.concat_map
    (fun (r : resolved_regel) ->
      let at = form_pointer r.rr_pointer ^ "/statuteAnnotations" in
      let first_version = r.rr_pointer = form_pointer r.rr_pointer ^ "/versions/0" in
      match r.rr_bron_annotaties with
      | _ when not first_version -> []
      | None -> []
      | Some [] ->
          [ v "WF-082" at
              "statuteAnnotations is empty — an annotation-free rule omits the field entirely" ]
      | Some entries ->
          List.concat
            (List.mapi
               (fun j e ->
                 if e = "" then
                   [ v "WF-082" (Printf.sprintf "%s/%d" at j)
                       "blank statute annotation — every entry carries text" ]
                 else if String.trim e <> e then
                   [ v "WF-082" (Printf.sprintf "%s/%d" at j)
                       "statute annotation carries leading/trailing whitespace — entries are trimmed" ]
                 else [])
               entries))
    ctx.doc.resolved_regels

(* WF-051: no dependency cycle may pass through prioritized defaults — their
   displacement edges are negative (dep_forms above), so any such cycle
   also violates WF-046; this check names the defaults-specific rule. *)
let wf051 ctx =
  let forms = Array.of_list (dep_forms ctx) in
  let nf = Array.length forms in
  let writers = Hashtbl.create 16 in
  Array.iteri (fun i (f : dep_form) -> List.iter (fun n -> Hashtbl.add writers n i) f.df_writes) forms;
  let succ = Array.make nf [] in
  Array.iteri
    (fun ci (f : dep_form) ->
      List.iter
        (fun (node, _) -> List.iter (fun pi -> succ.(pi) <- ci :: succ.(pi)) (Hashtbl.find_all writers node))
        f.df_reads)
    forms;
  let reaches src dst =
    let seen = Array.make nf false in
    let rec go i =
      List.exists (fun j -> j = dst || (not seen.(j) && (seen.(j) <- true; go j))) succ.(i)
    in
    go src
  in
  List.concat
    (List.mapi
       (fun i (l : prioritized_defaults) ->
         let at = Printf.sprintf "/prioritizedDefaults/%d" i in
         match
           Array.to_list forms
           |> List.mapi (fun fi f -> (fi, f))
           |> List.find_opt (fun ((_ : int), (f : dep_form)) -> f.df_at = at)
         with
         | Some (fi, _) when reaches fi fi ->
             [ v "WF-051" at
                 ("a dependency cycle passes through defaults group '"
                 ^ Option.value l.pd_naam ~default:at
                 ^ "' — its displacement reads are negative edges; no strata exist") ]
         | _ -> [])
       ctx.doc.prioritized_defaults)

(* WF-075: the nested exception form (RUNTIME-SEMANTICS ch. 7.9). A default's
   [exceptions] field carries at least one exception group, and two or more
   groups require the unordered marking — the root regime (WF-050's
   declared-unordered rule) applied at every level; the flag rides the
   exceptions object itself. A sub-default carries no initialisation (the
   initialisation and input defaults are root-only). Every default carries its
   own decision/consequence, a nested exception group, or is the initialisation
   default — a contentless default decides nothing. Recurses to any depth. (The
   content-KIND per target and the WF-051 descendant-read folding land with
   WF-050 and dep_forms; a differing exception group's root propagation is
   evaluation semantics, ch. 7.9.) *)
let wf075 ctx =
  List.concat
    (List.mapi
       (fun i (l : prioritized_defaults) ->
         let at = Printf.sprintf "/prioritizedDefaults/%d" i in
         let rec walk ~in_sub prefix (defaults : default list) =
           List.concat
             (List.mapi
                (fun j (r : default) ->
                  let rat = Printf.sprintf "%s/defaults/%d" prefix j in
                  let has_content =
                    r.default_besluit <> None || r.default_consequentie <> None
                  in
                  (if in_sub && r.default_initialisatie then
                     [ v "WF-075" rat
                         "a default inside an exception group carries no initialisation (the \
                          initialisation and input defaults are root-only, ch. 7.9)" ]
                   else [])
                  @ (if (not has_content) && r.default_excepties = None
                        && not r.default_initialisatie
                     then
                       [ v "WF-075" rat
                           "a default carries its own decision/consequence or a nested \
                            exception group (a contentless default decides nothing, \
                            ch. 7.9)" ]
                     else [])
                  @ (if r.default_excepties <> None && (not has_content)
                        && r.default_voorwaarde <> None && not r.default_initialisatie
                     then
                       [ v "WF-075" rat
                           "a group-default (exceptions present, no own decision or \
                            consequence) carries no justification of its own: the exception \
                            groups decide, so a condition here would be silently ignored \
                            (ch. 7.9)" ]
                     else [])
                  @ (match r.default_excepties with
                    | None -> []
                    | Some egs ->
                        (if egs.egs_leden = [] then
                           [ v "WF-075" (rat ^ "/exceptions")
                               "the exceptions field carries at least one exception group \
                                (ch. 7.9)" ]
                         else [])
                        @ (if List.length egs.egs_leden >= 2 && not egs.egs_ongeordend then
                             [ v "WF-075" (rat ^ "/exceptions")
                                 "two or more exception groups must be declared unordered \
                                  (the root regime, ch. 7.9)" ]
                           else [])
                        @ List.concat
                            (List.mapi
                               (fun k (eg : exception_group) ->
                                 walk ~in_sub:true
                                   (Printf.sprintf "%s/exceptions/members/%d" rat k)
                                   eg.eg_defaults)
                               egs.egs_leden)))
                defaults)
         in
         walk ~in_sub:false at l.defaults)
       ctx.doc.prioritized_defaults)

(* WF-057: declared names are target-text-safe (IR-SPEC ch. 2). A
   declared name — object type, attribute, characteristic, plural,
   role, fact type, parameter, domain, unit system, dimension,
   timeline, day kind, or any form/group/defaults-group name — MUST NOT carry
   a double quote, a backslash, or a control character: such a name
   breaks a quoted target text (the hunted ASP crash the backend's
   quoting fence catches last-line; this rule catches it at the
   document, for every backend at once). *)
let wf057 ctx =
  let bad s =
    String.exists (fun c -> c = '"' || c = '\\' || Char.code c < 0x20) s
  in
  let show s =
    String.concat ""
      (List.map
         (fun c ->
           if c = '"' then "\\\""
           else if c = '\\' then "\\\\"
           else if Char.code c < 0x20 then Printf.sprintf "\\u%04x" (Char.code c)
           else String.make 1 c)
         (List.init (String.length s) (String.get s)))
  in
  let check at what name =
    match name with
    | Some n when bad n ->
        [ v "WF-057" at
            (Printf.sprintf
               "%s \"%s\" carries a quote, backslash or control character — declared names must \
                be target-text-safe"
               what (show n)) ]
    | _ -> []
  in
  let keys at what pairs = List.concat_map (fun (k, _) -> check at what (Some k)) pairs in
  List.concat
    [ List.concat_map
        (fun (key, (ot : objecttype)) ->
          let at = "/objecttypen/" ^ key in
          (* the key IS the node's name since M1 (the loader builds both from
             one field), so a second reading would report the name twice *)
          check at "object type name" (Some key)
          @ check at "plural" ot.meervoud
          @ keys at "attribute name" ot.attributen
          @ List.concat_map (fun (_, m) -> check at "attribute plural" m) ot.attribuut_meervoud
          @ List.concat_map (fun k -> check at "characteristic name" k) ot.kenmerken)
        ctx.doc.objecttypen;
      List.concat_map
        (fun (key, (ft : feittype)) ->
          let at = "/feittypen/" ^ key in
          check at "fact type name" (Some key)
          @ List.concat_map
              (fun (r : rol) ->
                check at "role name" r.rol_naam @ check at "role plural" r.rol_meervoud)
              ft.rollen)
        ctx.doc.feittypen;
      keys "/parameters" "parameter name" ctx.doc.parameters;
      keys "/domeinen" "domain name" ctx.doc.domeinen;
      keys "/eenheidstelsels" "unit system name" ctx.doc.eenheidstelsels;
      List.concat_map
        (fun (key, (d : dimensie)) ->
          let at = "/dimensies/" ^ key in
          check at "dimension name" (Some key)
          @ check at "dimension plural" d.dim_naam_meervoud)
        ctx.doc.dimensies;
      List.concat_map (fun (tl : tijdlijn) -> check "/tijdlijnen" "timeline name" tl.tl_naam)
        ctx.doc.tijdlijnen;
      List.concat
        (List.mapi
           (fun i (d : dagsoort) ->
             check (Printf.sprintf "/dagsoorten/%d/name" i) "day kind name" (Some d.dk_naam))
           ctx.doc.dagsoorten);
      List.concat_map
        (fun (_, entries) ->
          List.concat_map (fun (n, _, at) -> check at "form name" n) entries)
        (form_arrays ctx);
      List.concat_map
        (fun (g : resolved_regel_groep) ->
          check "/ruleGroups" "rule group name" g.rgr_naam)
        ctx.doc.regel_groepen;
      List.mapi
        (fun i (l : prioritized_defaults) ->
          check (Printf.sprintf "/prioritizedDefaults/%d" i) "defaults-group name" l.pd_naam)
        ctx.doc.prioritized_defaults
      |> List.concat ]

(* WF-059: every declared attribute carries a datatype — the attribute
   pendant of WF-018's datatype clause. A datatype-less declaration is
   loadable (the wire admits null) but splits the executors on
   verifier-clean input: ref reads case input against it as a bare
   unitless number while the integer-scaling translation cannot scale
   it. The shared case loader refuses such input as the last-line
   backstop.

   Arms 2 and 3 (a characteristic or an attribute declared time-dependent
   by a granularity KEY whose VALUE was null, maintainer decisions
   2026-09-09 and 2026-09-10) left with M1: a facet is a TimelineFacet
   node on its member, its granularity is a required non-null enum, and
   the shared loader refuses anything else, so a timeline without a grid
   cannot be written (IR-SPEC ch. 2.2, WF index). *)
let wf059 ctx =
  List.concat_map
    (fun (key, (ot : objecttype)) ->
      List.concat_map
        (fun (an, dt) ->
          match dt with
          | None ->
              [ v "WF-059"
                  ("/objecttypen/" ^ key ^ "/attributen/" ^ an)
                  "declared attribute carries no datatype" ]
          | Some _ -> [])
        ot.attributen)
    ctx.doc.objecttypen

(* The composite domains a declared domain directly references — the sum-type
   payloads and the struct fields that name another (sum or struct) domain.
   This is the shared acyclicity graph of WF-060 and WF-071: no recursive sum
   types or structs (the term-depth fence for the ASP encoding, ch. 2.4). *)
let domain_refs ctx name : string list =
  let composite_domain = function
    | Sum_type { domein; _ } | Struct_type { domein; _ } -> Some domein
    | _ -> None
  in
  match find_ci ctx.doc.domeinen name with
  | Some (Some (Sum_type { constructoren; _ })) ->
      List.filter_map (fun (_, p) -> Option.bind p composite_domain) constructoren
  | Some (Some (Struct_type { velden; _ })) ->
      List.filter_map (fun (_, d) -> composite_domain d) velden
  | _ -> []

(* Is [target] reachable from [name] through the composite-domain graph? *)
let domain_reaches ctx ~target ~visited name =
  let rec go visited name =
    List.exists
      (fun e ->
        norm e = norm target
        || ((not (List.mem (norm e) visited)) && go (norm e :: visited) e))
      (domain_refs ctx name)
  in
  go visited name

(* WF-060: a SumType declaration (a domain whose datatype is a SumType) MUST
   carry at least one constructor, unique constructor names, admissible
   payload datatypes (Number, day-precision Date, or a declared SumType),
   and an acyclic payload graph — shared with WF-071's struct fields (IR-SPEC
   ch. 2.4 — no recursive sum types). *)
let wf060 ctx =
  List.concat_map
    (fun (name, dt) ->
      match dt with
      | Some (Sum_type { constructoren; _ }) ->
          let at = "/domeinen/" ^ name in
          (if constructoren = [] then [ v "WF-060" at "sum type declares no constructor" ]
           else [])
          @ List.map
              (fun (first, dup, at') ->
                v "WF-060" at'
                  (Printf.sprintf "duplicate constructor name '%s' (already declared as '%s')"
                     dup first))
              (dups_by norm (List.map (fun (n, _) -> (n, at)) constructoren))
          @ List.concat_map
              (fun (_, payload) ->
                match payload with
                | None | Some (Numeriek _) | Some Datum_in_dagen -> []
                | Some (Sum_type { domein; _ }) -> (
                    match sum_decl ctx domein with
                    | Some _ -> []
                    | None ->
                        [ v "WF-060" at
                            ("constructor payload names sum-type domain '" ^ domein
                           ^ "', which is not a declared sum type") ])
                | Some d ->
                    [ v "WF-060" at
                        ("constructor payload is " ^ datatype_kind d
                       ^ ", which is not an admissible sum-type payload (number, day-precision \
                          date, or a declared sum type)") ])
              constructoren
          @
          if domain_reaches ctx ~target:name ~visited:[ norm name ] name then
            [ v "WF-060" at
                ("sum type '" ^ name
               ^ "' is recursive — the payload graph over sum-type and struct declarations must \
                  be acyclic")
            ]
          else []
      | _ -> [])
    ctx.doc.domeinen

(* WF-087: a SumType declaration whose constructors ALL carry a null payload
   is ill-formed ON THE WIRE. The keuzetype family (IR-SPEC ch. 2.4) has
   exactly one nullary wire form — the Enumeration domain — and an
   all-nullary sum is sugar for it: it MUST normalise to an Enumeration domain
   carrying the constructor names as its values. That is what keeps two
   frontends from expressing one meaning in two shapes, without a wire epoch
   (maintainer decision 2026-09-01). Payload-carrying sums are untouched:
   payload never converts.

   The rule is stated over the DECLARATION and needs no use-site arm: WF-061
   forces every SumType use site to agree with its declaring domain, and a use
   site naming no declared sum type is rejected there. *)
let wf087 ctx =
  List.concat_map
    (fun (name, dt) ->
      match dt with
      | Some (Sum_type { constructoren; _ })
        when constructoren <> [] && List.for_all (fun (_, p) -> p = None) constructoren ->
          [ v "WF-087" ("/domeinen/" ^ name)
              ("sum type '" ^ name
             ^ "' declares only nullary constructors — the nullary keuzetype has one wire form: \
                normalise it to an Enumeration domain carrying those names as its values (ch. 2.4)")
          ]
      | _ -> [])
    ctx.doc.domeinen

(* WF-061: a SumType use site (an attribute or parameter datatype that is a
   SumType) MUST name a declared domain whose declaration it agrees with —
   same constructors, same payloads, same order (IR-SPEC ch. 2.4). The
   nominal-comparison half of the rule (two sum-typed operands of one
   comparison name the same domain) rides the expression walk, walk_cond. *)
let wf061 ctx =
  let use_site at = function
    | Sum_type { domein; constructoren } -> (
        match sum_decl ctx domein with
        | None ->
            [ v "WF-061" at
                ("sum-type use site names domain '" ^ domein
               ^ "', which is not a declared sum type") ]
        | Some decl ->
            if constructors_agree decl constructoren then []
            else
              [ v "WF-061" at
                  ("sum-type use site disagrees with the declaration of domain '" ^ domein
                 ^ "' (same constructors, payloads and order required)") ])
    | _ -> []
  in
  List.concat_map
    (fun (otname, (ot : objecttype)) ->
      List.concat_map
        (fun (an, dt) ->
          match dt with
          | Some st -> use_site ("/objecttypen/" ^ otname ^ "/attributen/" ^ an) st
          | None -> [])
        ot.attributen)
    ctx.doc.objecttypen
  @ List.concat_map
      (fun (pn, (p : parameter)) ->
        match p.par_datatype with
        | Some st -> use_site ("/parameters/" ^ pn) st
        | None -> [])
      ctx.doc.parameters

(* WF-088: the model-layer half of the enumeration use-site rule (the
   expression half rides walk_expr's type prologue, where WF-007 collects
   every expression `type` — the same "every datatype in the document" split).
   The declarations in [domeinen] are the source of truth and are therefore
   not walked here. *)
let wf088 ctx =
  let site at = function Some d -> enum_use_site ctx at d | None -> [] in
  List.concat_map
    (fun (otname, (ot : objecttype)) ->
      List.concat_map
        (fun (an, dt) -> site ("/objecttypen/" ^ otname ^ "/attributen/" ^ an) dt)
        ot.attributen)
    ctx.doc.objecttypen
  @ List.concat_map
      (fun (pn, (p : parameter)) -> site ("/parameters/" ^ pn) p.par_datatype)
      ctx.doc.parameters
  @ List.concat
      (List.mapi
         (fun i (fd : function_def) ->
           let at = Printf.sprintf "/functionDefs/%d" i in
           List.concat
             (List.mapi
                (fun j (_, dt) -> enum_use_site ctx (Printf.sprintf "%s/parameters/%d" at j) dt)
                fd.fd_parameters)
           @ enum_use_site ctx (at ^ "/result") fd.fd_result)
         ctx.doc.function_defs)

(* WF-071: a StructType declaration (a domain whose datatype is a StructType)
   MUST carry at least one field, ci-unique field names, admissible field
   datatypes (Number, day-precision Date, a declared SumType, or a declared
   StructType), and an acyclic field graph — shared with WF-060's sum-type
   payloads (IR-SPEC ch. 2.4 — no recursive structs). *)
let wf071 ctx =
  List.concat_map
    (fun (name, dt) ->
      match dt with
      | Some (Struct_type { velden; _ }) ->
          let at = "/domeinen/" ^ name in
          (if velden = [] then [ v "WF-071" at "struct type declares no field" ] else [])
          @ List.map
              (fun (first, dup, at') ->
                v "WF-071" at'
                  (Printf.sprintf "duplicate field name '%s' (already declared as '%s')" dup first))
              (dups_by norm (List.map (fun (n, _) -> (n, at)) velden))
          @ List.concat_map
              (fun (fname, fdt) ->
                match fdt with
                | Numeriek _ | Datum_in_dagen -> []
                | Sum_type { domein; _ } -> (
                    match sum_decl ctx domein with
                    | Some _ -> []
                    | None ->
                        [ v "WF-071" at
                            ("field '" ^ fname ^ "' names sum-type domain '" ^ domein
                           ^ "', which is not a declared sum type") ])
                | Struct_type { domein; _ } -> (
                    match struct_decl ctx domein with
                    | Some _ -> []
                    | None ->
                        [ v "WF-071" at
                            ("field '" ^ fname ^ "' names struct-type domain '" ^ domein
                           ^ "', which is not a declared struct type") ])
                | d ->
                    [ v "WF-071" at
                        ("field '" ^ fname ^ "' is " ^ datatype_kind d
                       ^ ", which is not an admissible struct-field datatype (number, day-precision \
                          date, sum type, or struct type)") ])
              velden
          @
          if domain_reaches ctx ~target:name ~visited:[ norm name ] name then
            [ v "WF-071" at
                ("struct type '" ^ name
               ^ "' is recursive — the field graph over sum-type and struct declarations must be \
                  acyclic")
            ]
          else []
      | _ -> [])
    ctx.doc.domeinen

(* WF-072: a StructType use site MUST name a declared domain whose declaration
   it agrees with — same fields, datatypes, order (IR-SPEC ch. 2.4). The
   nominal-comparison half (two struct-typed operands of one comparison name
   the same domain) rides the expression walk, walk_cond.

   The model-layer half is here and reaches EVERY StructType in the datatype
   tree via [struct_use_site]: an attribute or parameter datatype, a
   `ListType` element inside one, a `FunctionDef` parameter or
   result, and — unlike WF-088, whose enumerations WF-060/065/071 fence out of
   every declaration position — a struct field datatype inside `domeinen`,
   which WF-071 admits and which is therefore a use site of ANOTHER domain's
   declaration. The expression-layer half rides walk_expr's type prologue,
   beside [enum_use_site] (the same "every datatype in the document" split
   WF-088 uses). For the positions deliberately NOT walked — a matched
   struct's own `velden` and a sum-type payload — see the comment on
   [struct_use_site], which motivates both; a rule TARGET's `type` is a third,
   outside the expression walk exactly as it is under WF-088 (IR-SPEC ch. 2.4
   records all three with their measurements). *)
let wf072 ctx =
  let site at = function Some d -> struct_use_site ctx at d | None -> [] in
  List.concat_map
    (fun (otname, (ot : objecttype)) ->
      List.concat_map
        (fun (an, dt) -> site ("/objecttypen/" ^ otname ^ "/attributen/" ^ an) dt)
        ot.attributen)
    ctx.doc.objecttypen
  @ List.concat_map
      (fun (pn, (p : parameter)) -> site ("/parameters/" ^ pn) p.par_datatype)
      ctx.doc.parameters
  @ List.concat
      (List.mapi
         (fun i (fd : function_def) ->
           let at = Printf.sprintf "/functionDefs/%d" i in
           List.concat
             (List.mapi
                (fun j (_, dt) -> struct_use_site ctx (Printf.sprintf "%s/parameters/%d" at j) dt)
                fd.fd_parameters)
           @ struct_use_site ctx (at ^ "/result") fd.fd_result)
         ctx.doc.function_defs)
  @ List.concat_map
      (fun (name, dt) ->
        (* A declaration is its own source of truth, so the node itself is not
           crossed; what is reached is a struct-typed FIELD of it — WF-071
           admits declared struct domains there, and that field carries an
           inline copy of ANOTHER domain's declaration. (A sum-type payload
           needs no walk: WF-060 admits no struct payload at all.) *)
        match dt with
        | Some (Struct_type { velden; _ }) ->
            List.concat
              (List.mapi
                 (fun j (_, fdt) ->
                   struct_use_site ctx (Printf.sprintf "/domeinen/%s/fields/%d" name j) fdt)
                 velden)
        | _ -> [])
      ctx.doc.domeinen

(* WF-065: a ListType element MUST be first-order and non-list (Number,
   day-precision Date, a declared SumType, or a declared StructType — the
   Phase-5c widening), and a list-typed attribute MUST NOT carry a
   granularity (a list-typed temporal attribute is ill-formed) — IR-SPEC
   ch. 2.4. *)
let wf065 ctx =
  let element_check at element =
    if admissible_element ctx element then []
    else
      [ v "WF-065" at
          ("list element is " ^ datatype_kind element
         ^ ", not a first-order non-list datatype (number, day-precision date, a declared \
            sum type, or a declared struct type)") ]
  in
  List.concat_map
    (fun (otname, (ot : objecttype)) ->
      List.concat_map
        (fun (an, dt) ->
          match dt with
          | Some (List_type { element }) ->
              let at = "/objecttypen/" ^ otname ^ "/attributen/" ^ an in
              element_check at element
              @ (if
                   (* a facet's presence is the temporality decision; since M1
                      it always carries its grid, so the key alone decides *)
                   List.exists (fun (n, _) -> norm n = norm an) ot.attribuut_granulariteit
                 then
                   [ v "WF-065" at
                       "a list-typed attribute carries a granularity — a list-typed temporal \
                        attribute is ill-formed" ]
                 else [])
          | _ -> [])
        ot.attributen)
    ctx.doc.objecttypen
  @ List.concat_map
      (fun (pn, (p : parameter)) ->
        match p.par_datatype with
        | Some (List_type { element }) -> element_check ("/parameters/" ^ pn) element
        | _ -> [])
      ctx.doc.parameters
  @ List.concat_map
      (fun (dn, dt) ->
        match dt with
        | Some (List_type { element }) -> element_check ("/domeinen/" ^ dn) element
        | _ -> [])
      ctx.doc.domeinen

(* The function names invoked by an expression — Invoke targets and named
   Fold/MapList operands — for the WF-067 acyclicity check. *)
let rec invoked_expr acc (e : expr) : string list =
  match e with
  | Invoke { functie; arguments; _ } -> List.fold_left invoked_expr (functie :: acc) arguments
  | Fold { list; functie; init; _ } ->
      invoked_fold_fn (invoked_expr (invoked_expr acc list) init) functie
  | Map_list { list; functie; _ } -> invoked_fold_fn (invoked_expr acc list) functie
  | Filter_list { list; condition; _ } -> invoked_cond (invoked_expr acc list) condition
  | List_length { list; _ } -> invoked_expr acc list
  | List_literal { elements; _ } -> List.fold_left invoked_expr acc elements
  | Lambda { body; _ } -> invoked_expr acc body
  | If_then_else { condition; then_; else_; _ } ->
      invoked_expr (invoked_expr (invoked_cond acc condition) then_) else_
  | Binair { links; rechts; _ } -> invoked_opt (invoked_opt acc links) rechts
  | Functie { argumenten; leegwaarde; filter; _ } ->
      let acc = List.fold_left invoked_expr acc argumenten in
      let acc = invoked_opt acc leegwaarde in
      (match filter with Some c -> invoked_cond acc c | None -> acc)
  | Afgerond { basis; _ } -> invoked_opt acc basis
  | Begrensd { basis; minimum; maximum; _ } ->
      invoked_opt (invoked_opt (invoked_opt acc basis) minimum) maximum
  | Construct { argument; _ } -> invoked_opt acc argument
  | Match { scrutinee; arms; _ } ->
      List.fold_left (fun acc (_, _, b) -> invoked_expr acc b) (invoked_expr acc scrutinee) arms
  | Struct_literal { velden; _ } ->
      List.fold_left (fun acc (_, value) -> invoked_expr acc value) acc velden
  | Field_access { base; _ } -> invoked_expr acc base
  | Let { value; body = lbody; _ } -> invoked_expr (invoked_expr acc value) lbody
  | Tekst_reeks { delen; _ } ->
      List.fold_left
        (fun acc -> function Ingevoegd { expr = Some e } -> invoked_expr acc e | _ -> acc)
        acc delen
  | Dimensie_cel { attribuut; _ } -> invoked_opt acc attribuut
  | Temporal_read { basis; datum; _ } -> invoked_expr (invoked_expr acc basis) datum
  | Literal _ | Attribuut_ref _ | Parameter_ref _ | Variabele_ref _ | Constante _
  | Bound_day _ | Expr_onopgelost _ | Binder_ref _ ->
      acc
  (* a union's branch fields are symbolic and its filter is a condition, which
     this walk over EXPRESSIONS does not enter — the same boundary the fold's
     own filter has here *)
  | Instance_set_union _ -> acc

and invoked_opt acc = function Some e -> invoked_expr acc e | None -> acc

and invoked_fold_fn acc = function
  | Fn_lambda { body; _ } -> invoked_expr acc body
  | Fn_name n -> n :: acc

and invoked_cond acc (c : conditie) : string list =
  match c with
  | Vergelijking { links; rechts; _ } -> invoked_opt (invoked_opt acc links) rechts
  | Predicaat { operand; _ } -> invoked_opt acc operand
  | Samengesteld { onderdelen; _ } -> List.fold_left invoked_cond acc onderdelen
  | Periode_conditie { vanaf; tot; _ } -> invoked_opt (invoked_opt acc vanaf) tot
  | Kwantorvouw { kv_filter; kv_toets; _ } ->
      let sub acc = function Some c -> invoked_cond acc c | None -> acc in
      sub (sub acc kv_filter) kv_toets
  | Kenmerk_check _ | Role_membership _ | Regelversie _
  | Conditie_onopgelost _ ->
      acc

(* WF-067: FunctionDef discipline (IR-SPEC ch. 2.11) — ci-unique names, typed
   uniquely-named parameters, a resolvable result, a closed body (the walk with
   [~body:(Some "WF-067")]), and an acyclic invocation graph. *)
let wf067 ctx =
  let fds = ctx.doc.function_defs in
  let name_dups =
    dups_by norm (List.mapi (fun i fd -> (fd.fd_naam, Printf.sprintf "/functionDefs/%d" i)) fds)
    |> List.map (fun (first, dup, at) ->
           v "WF-067" at
             (Printf.sprintf "duplicate function name '%s' (already declared as '%s')" dup first))
  in
  (* [name] reaches itself through the invocation graph? *)
  let rec reaches ~target ~visited name =
    match function_decl ctx name with
    | None -> false
    | Some fd ->
        List.exists
          (fun callee ->
            norm callee = norm target
            || ((not (List.mem (norm callee) visited))
               && reaches ~target ~visited:(norm callee :: visited) callee))
          (invoked_expr [] fd.fd_body)
  in
  name_dups
  @ List.concat
      (List.mapi
         (fun i (fd : function_def) ->
           let at = Printf.sprintf "/functionDefs/%d" i in
           (dups_by norm (List.map (fun (pn, _) -> (pn, at)) fd.fd_parameters)
            |> List.map (fun (first, dup, at') ->
                   v "WF-067" at'
                     (Printf.sprintf "duplicate parameter name '%s' (already declared as '%s')" dup
                        first)))
           @ (match fd.fd_result with
             | Sum_type { domein; _ } when sum_decl ctx domein = None ->
                 [ v "WF-067" at
                     ("result names sum-type domain '" ^ domein ^ "', which is not a declared sum \
                       type") ]
             | _ -> [])
           (* the body's static type IS the declared result — full datatype
              equality, soort and keuzetype domain alike, as WF-068/069/070
              cross their operands. The body is the write into the result
              WF-068 lets every Invoke read (the domain identity of ch. 2.4,
              write half): without this cross a body of another domain than
              the declared result was verifier-clean and reached ref's "no
              arm" fault through a truthful Invoke (the card's fix round 1,
              probe P7). A body whose type nothing declares defers, as under
              WF-023. *)
           @ (match
                ( expr_type ctx
                    ~binders:(List.map (fun (n, dt) -> (n, Some dt)) fd.fd_parameters)
                    fd.fd_body,
                  fd.fd_result )
              with
             | None, _ -> []
             | Some bt, r when bt <> r ->
                 [ v "WF-067" (at ^ "/body/type")
                     ("the body is " ^ datatype_detail bt ^ ", but the declared result is "
                    ^ datatype_detail r) ]
             | _ -> [])
           (* a FunctionDef body is closed and top-level: it sees only its own
              parameters, so there are no outer binders to remember (WF-064's
              closure clause has nothing to name here) *)
           @ walk_expr ctx
               ~binders:(List.map (fun (n, dt) -> (n, Some dt)) fd.fd_parameters)
               ~subject:None ~body:(Closed_pos ("WF-067", [])) (at ^ "/body") fd.fd_body
           @ (if reaches ~target:fd.fd_naam ~visited:[ norm fd.fd_naam ] fd.fd_naam then
                [ v "WF-067" at
                    ("function '" ^ fd.fd_naam
                   ^ "' is part of a recursive invocation cycle — the invocation graph must be \
                      acyclic") ]
              else []))
         fds)

(* WF-086: a PrioritizedDefaults group MUST NOT target a temporal attribute —
   one carrying a declared granularity (attribuutGranulariteit), whether
   anchor-only or on a named timeline. A defaults group is atemporal: it
   decides once, at one fixpoint (RUNTIME-SEMANTICS ch. 7), and has no
   timeline coordinate to write. The executors already refuse it — ref faults
   on a temporal defaults write (eval.ml, kept as defense-in-depth), and the
   Drools defaults service fences it as a reason-carrying Dekking.skip — so
   this document-layer rule refuses it FIRST, at the PrioritizedDefaults node,
   via the shared [attr_temporal] helper. Temporal defaults targets are not yet
   scheduled (PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a, ratified 2026-08-29);
   the fence lifts, with witnesses, when they are. *)
let wf086 ctx =
  List.concat
    (List.mapi
       (fun i (l : prioritized_defaults) ->
         let at = Printf.sprintf "/prioritizedDefaults/%d" i in
         match (l.pd_attribuut, l.pd_kenmerk, objecttype ctx l.pd_onderwerp) with
         | Some a, _, Some ot when attr_temporal ot a ->
             [ v "WF-086" at
                 ("defaults target '" ^ a
                ^ "' is a temporal attribute (declared granularity) — a defaults group decides \
                   at one fixpoint and cannot write a timeline (ch. 6)") ]
         | _, Some k, Some ot
           when List.exists (fun (n, _) -> norm n = norm k) ot.kenmerk_granulariteit ->
             [ v "WF-086" at
                 ("defaults target '" ^ k
                ^ "' is a temporal characteristic (declared granularity) — a defaults group \
                   decides at one fixpoint and cannot write a timeline (ch. 6)") ]
         | _ -> [])
       ctx.doc.prioritized_defaults)

(* --- WF-103/104/105: the supertype reference (IR-SPEC ch. 2.2) ------------
   Three rules on one field. WF-106 — an object type declares at most one
   direct supertype — is deliberately absent from this file and has no
   negatief either: `supertype` is a bare string, so a second direct parent
   is not a document a check could reject but one that cannot be written.
   Its witnesses live in the frontends that read the language above this
   wire. *)

(* WF-103: the supertype names a declared object type. WF-003 would call
   this a dangling reference; it gets its own number because its failure
   mode is specific — a chain that ends nowhere resolves no member — and
   because a witness has to be able to name it. *)
let wf103 ctx =
  List.filter_map
    (fun (key, (ot : objecttype)) ->
      match ot.ot_supertype with
      | Some s when declared_objecttype ctx s = None ->
          Some
            (v "WF-103"
               ("/objecttypen/" ^ key ^ "/supertype")
               ("supertype '" ^ s
              ^ "' is not a declared object type: the chain ends at a name nothing declares, so \
                 no member resolves through it"))
      | _ -> None)
    ctx.doc.objecttypen

(* WF-104: the supertype graph is acyclic, self-reference included. Colour
   DFS with a named cycle path — WF-085's shape, because the failure is the
   same: a cycle has no fixpoint. Here it is member resolution that has no
   root to stop at, and every reader would walk it forever or cut it off at
   a private depth of its own. *)
let wf104 ctx =
  let defs = Array.of_list ctx.doc.objecttypen in
  let n = Array.length defs in
  (* keyed by MAP KEY, which is what [declared_objecttype] resolves against *)
  let index = Hashtbl.create 16 in
  Array.iteri (fun i (key, _) -> if not (Hashtbl.mem index (norm key)) then Hashtbl.add index (norm key) i) defs;
  let succ =
    Array.map
      (fun (_, (ot : objecttype)) ->
        match ot.ot_supertype with
        | None -> []
        | Some s -> ( match Hashtbl.find_opt index (norm s) with Some j -> [ j ] | None -> []))
      defs
  in
  let color = Array.make (max n 1) 0 in
  let viols = ref [] in
  let name i = fst defs.(i) in
  let rec dfs path i =
    match color.(i) with
    | 1 ->
        let rec take acc = function
          | [] -> acc
          | j :: _ when j = i -> j :: acc
          | j :: rest -> take (j :: acc) rest
        in
        let cyc = take [] path in
        let names = String.concat " -> " (List.map name (cyc @ [ i ])) in
        viols :=
          v "WF-104"
            ("/objecttypen/" ^ name i ^ "/supertype")
            ("supertype cycle (a type would be its own ancestor, so member resolution has no \
              root to stop at): " ^ names)
          :: !viols
    | 2 -> ()
    | _ ->
        color.(i) <- 1;
        List.iter (fun j -> dfs (i :: path) j) succ.(i);
        color.(i) <- 2
  in
  Array.iteri (fun i _ -> if color.(i) = 0 then dfs [] i) defs;
  List.rev !viols

(* WF-105: no member name is declared twice along one chain — WF-002's
   per-kind uniqueness discipline lifted from the declaration to the chain.
   One instance would otherwise see two declarations of one name, and which
   one a reader finds decides that name's datatype, dimensions and timeline
   facet on every read. The measured consumer consequence is sharper than a
   compile error: the Drools bean generator deduplicates silently,
   first-wins, so a shadowed name yields a bean that generates perfectly and
   is quietly missing one member. A SIBLING collision — two incomparable
   subtypes declaring one name — is admitted and is not this rule: no
   instance is of two incomparable types, so no read is ambiguous. *)
let wf105 ctx =
  List.concat_map
    (fun (key, (ot : objecttype)) ->
      match ot.ot_supertype with
      | None -> []
      | Some _ ->
          let ancestors = match chain_from ctx ot with _ :: rest -> rest | [] -> [] in
          let above ~kind name =
            List.find_map
              (fun (a : objecttype) ->
                let hit =
                  match kind with
                  | `Attribute -> List.exists (fun (k, _) -> norm k = norm name) a.attributen
                  | `Characteristic -> mem_ci (some_names a.kenmerken) name
                in
                if hit then Some (Option.value a.ot_naam ~default:"?") else None)
              ancestors
          in
          let report ~kind ~what ~at name =
            match above ~kind name with
            | None -> None
            | Some anc ->
                Some
                  (v "WF-105" at
                     (Printf.sprintf
                        "%s '%s' is already declared on '%s' further up the chain: a subtype \
                         never redeclares an inherited name (one instance would carry two \
                         declarations of it)"
                        what name anc))
          in
          List.filter_map
            (fun (a, _) ->
              report ~kind:`Attribute ~what:"attribute"
                ~at:("/objecttypen/" ^ key ^ "/attributen/" ^ a) a)
            ot.attributen
          @ List.filter_map
              (fun k ->
                report ~kind:`Characteristic ~what:"characteristic"
                  ~at:("/objecttypen/" ^ key ^ "/kenmerken") k)
              (some_names ot.kenmerken))
    ctx.doc.objecttypen

(* WF-113: `diagnostics` is the producer's own account of what stayed
   unresolved in a document it did write (IR-SPEC ch. 2, decision V2 (ii);
   the rule since M1, 2026-09-23).

   (a) Every entry carries severity warning, a message that is neither empty
   nor blank, and an integer line >= 0. That the line is an integer is the
   loader's: it reads [line] as one or refuses the document; a negative one
   is refused here, because 0 is already the value for "no source position"
   (a hand-built document, or a frontend warning without a token) and a line
   below it names nothing. The severities error and info, and a null
   severity, are refused here BY NAME — the verifier's loader admits them so
   that a document carrying one fails on this rule and says why, not on a
   load error, while every executor refuses them at load in the same
   sentences ([severity_refusal]): an error means the producer refused to
   write the document, and info is tooling talk (zero carriers in 769
   documents, measured 2026-09-23).

   (b) Every Unresolved node — the expression marker and the condition
   marker, at every expression and condition position of the document — is
   accounted for: at least one diagnostic's message contains the snippet of
   the marker's source record verbatim (the decision of 2026-09-19 that every
   surviving marker carries a WARNING, made checkable). Any diagnostic counts;
   that it is a warning is (a)'s business, so each clause fails on its own
   witness. A marker without a source is refused, and so is one whose snippet
   is blank: blank counts as absent, as it does for a named form's name
   (WF-107). The containment is literal, so a blank snippet would be contained
   in nearly every message, and a message about another marker would account
   for it. An empty snippet never reaches this rule: every reader refuses it
   at load. The schema requires the record on a marker, and the loader reads a
   marker without one for this rule alone; every executor refuses it at load,
   in the sentence below ([marker_without_source]).

   (c) — no consumer reads the list for evaluation — is a rule on consumers
   and has no document to reject. *)
let marker_without_source =
  "Unresolved marker without a source: no diagnostic message can contain it, so it cannot be \
   accounted for"

let rec markers_expr acc (e : expr) : string option list =
  let o acc = function Some e -> markers_expr acc e | None -> acc in
  let c acc = function Some x -> markers_cond acc x | None -> acc in
  match e with
  | Expr_onopgelost { bron; _ } -> bron :: acc
  | Literal _ | Attribuut_ref _ | Parameter_ref _ | Variabele_ref _ | Constante _ | Bound_day _
  | Binder_ref _ ->
      acc
  | Tekst_reeks { delen; _ } ->
      List.fold_left
        (fun acc -> function Ingevoegd { expr } -> o acc expr | Letterlijk _ -> acc)
        acc delen
  | Binair { links; rechts; _ } -> o (o acc links) rechts
  | Functie { argumenten; leegwaarde; filter; _ } ->
      c (o (List.fold_left markers_expr acc argumenten) leegwaarde) filter
  | Instance_set_union { branches; _ } ->
      List.fold_left (fun acc (b : instance_set_branch) -> c acc b.isb_filter) acc branches
  | Afgerond { basis; _ } -> o acc basis
  | Begrensd { basis; minimum; maximum; _ } -> o (o (o acc basis) minimum) maximum
  | Dimensie_cel { attribuut; _ } -> o acc attribuut
  | Temporal_read { basis; datum; _ } -> markers_expr (markers_expr acc basis) datum
  | Construct { argument; _ } -> o acc argument
  | Match { scrutinee; arms; _ } ->
      List.fold_left (fun acc (_, _, b) -> markers_expr acc b) (markers_expr acc scrutinee) arms
  | Struct_literal { velden; _ } -> List.fold_left (fun acc (_, x) -> markers_expr acc x) acc velden
  | Field_access { base; _ } -> markers_expr acc base
  | Let { value; body; _ } -> markers_expr (markers_expr acc value) body
  | List_literal { elements; _ } -> List.fold_left markers_expr acc elements
  | Lambda { body; _ } -> markers_expr acc body
  | If_then_else { condition; then_; else_; _ } ->
      markers_expr (markers_expr (markers_cond acc condition) then_) else_
  | Invoke { arguments; _ } -> List.fold_left markers_expr acc arguments
  | Fold { list; functie; init; _ } -> markers_fn (markers_expr (markers_expr acc list) init) functie
  | Map_list { list; functie; _ } -> markers_fn (markers_expr acc list) functie
  | Filter_list { list; condition; _ } -> markers_cond (markers_expr acc list) condition
  | List_length { list; _ } -> markers_expr acc list

and markers_fn acc = function Fn_lambda { body; _ } -> markers_expr acc body | Fn_name _ -> acc

and markers_cond acc (c : conditie) : string option list =
  let o acc = function Some e -> markers_expr acc e | None -> acc in
  let oc acc = function Some x -> markers_cond acc x | None -> acc in
  match c with
  | Conditie_onopgelost { bron } -> bron :: acc
  | Vergelijking { links; rechts; _ } -> o (o acc links) rechts
  | Predicaat { operand; _ } -> o acc operand
  | Periode_conditie { vanaf; tot; _ } -> o (o acc vanaf) tot
  | Samengesteld { onderdelen; _ } -> List.fold_left markers_cond acc onderdelen
  | Kwantorvouw { kv_filter; kv_toets; _ } -> oc (oc acc kv_filter) kv_toets
  | Kenmerk_check _ | Role_membership _ | Regelversie _ -> acc

(* Every expression and condition position of the document, with the
   markers below it in document order. *)
let marker_positions ctx : (string * string option list) list =
  let e at x = [ (at, List.rev (markers_expr [] x)) ] in
  let oe at = function Some x -> e at x | None -> [] in
  let oc at = function Some x -> [ (at, List.rev (markers_cond [] x)) ] | None -> [] in
  let idx name f l = List.concat (List.mapi (fun i x -> f (Printf.sprintf "/%s/%d" name i) x) l) in
  let rec default at (d : default) =
    oc (at ^ "/justification") d.default_voorwaarde
    @ oe (at ^ "/consequence") d.default_consequentie
    @
    match d.default_excepties with
    | Some egs ->
        List.concat
          (List.mapi
             (fun k (eg : exception_group) ->
               List.concat
                 (List.mapi
                    (fun m x -> default (Printf.sprintf "%s/exceptions/members/%d/defaults/%d" at k m) x)
                    eg.eg_defaults))
             egs.egs_leden)
    | None -> []
  in
  (* a version of a named form at the pointer the loader recorded, its
     fields under their wire names *)
  List.concat_map
    (fun (r : resolved_regel) ->
      let at = r.rr_pointer in
      oe (at ^ "/target") r.doel @ oe (at ^ "/value") r.waarde @ oc (at ^ "/condition") r.rr_voorwaarde)
    ctx.doc.resolved_regels
  @ idx "dayKindDefinitions"
      (fun at (r : resolved_dagsoort) -> oc (at ^ "/condition") r.rd_voorwaarde)
      ctx.doc.resolved_dagsoorten
  @ List.concat_map
      (fun (r : resolved_object_creatie) ->
        let at = r.oc_pointer in
        idx "initialisations" (fun t (x : toekenning) -> oe (at ^ t ^ "/value") x.toe_waarde)
          r.toekenningen
        @ oc (at ^ "/condition") r.oc_voorwaarde)
      ctx.doc.object_creaties
  @ List.concat_map
      (fun (r : resolved_verdeling) -> oc (r.vd_pointer ^ "/condition") r.vd_voorwaarde)
      ctx.doc.verdelingen
  @ List.concat_map
      (fun (r : resolved_consistentie) -> oc (r.cons_pointer ^ "/assertion") r.assertie)
      ctx.doc.consistenties
  @ List.concat_map
      (fun (r : resolved_feit_creatie) -> oc (r.fc_pointer ^ "/condition") r.fc_voorwaarde)
      ctx.doc.feit_creaties
  @ idx "prioritizedDefaults"
      (fun at (l : prioritized_defaults) ->
        idx "defaults" (fun d x -> default (at ^ d) x) l.defaults)
      ctx.doc.prioritized_defaults
  @ idx "functionDefs" (fun at (fd : function_def) -> e (at ^ "/body") fd.fd_body) ctx.doc.function_defs

let blank s = String.trim s = ""

let contains_verbatim ~hay ~needle =
  let n = String.length needle and h = String.length hay in
  let rec at i = i + n <= h && (String.sub hay i n = needle || at (i + 1)) in
  at 0

let wf113 ctx =
  let diags = List.mapi (fun i d -> (Printf.sprintf "/diagnostics/%d" i, d)) ctx.doc.diagnostics in
  List.concat_map
    (fun (at, (d : diagnostic)) ->
      (match severity_refusal d.diag_severity with
      | None -> []
      | Some sentence -> [ v "WF-113" (at ^ "/severity") sentence ])
      @ (match d.message with
        | Some m when not (blank m) -> []
        | _ ->
            [ v "WF-113" (at ^ "/message")
                "diagnostic with an empty or blank message: an entry that says nothing accounts \
                 for nothing" ])
      @
      if d.diag_line >= 0 then []
      else
        [ v "WF-113" (at ^ "/line")
            (Printf.sprintf
               "diagnostic with line %d: a line is a source position, and 0 is the value for \
                none (a hand-built document, or a producer warning without a token)"
               d.diag_line) ])
    diags
  @ List.concat_map
      (fun (at, brons) ->
        List.filter_map
          (function
            | None -> Some (v "WF-113" at marker_without_source)
            | Some b when blank b ->
                Some
                  (v "WF-113" at
                     "Unresolved marker whose source snippet is blank: blank counts as absent, \
                      since nearly every message contains it and a message about another \
                      marker would account for this one")
            | Some b ->
                if
                  List.exists
                    (fun (_, (d : diagnostic)) ->
                      match d.message with
                      | Some m -> contains_verbatim ~hay:m ~needle:b
                      | None -> false)
                    diags
                then None
                else
                  Some
                    (v "WF-113" at
                       (Printf.sprintf
                          "Unresolved marker '%s' is not accounted for: no diagnostic message \
                           contains its source snippet verbatim"
                          b)))
          brons)
      (marker_positions ctx)

(* The second step of the M1 path mapping ([m1_root] is the first): a
   declaration in one of the six named model lists is addressed by its list
   index, a domain by its datatype (the v0 map value), and an object type's
   v0 facet table by the member node's field. A name resolves to the first
   declaration whose name the rest of the path equals or continues with
   "/", the longest such name winning, so a name with a space or a slash in
   it is taken whole. A name that resolves to nothing — the checks build
   these paths from declared names, so only an undeclared reference could —
   leaves the pointer at the list. *)
let m1_named (doc : document) at =
  let starts ~pre s =
    String.length s >= String.length pre && String.sub s 0 (String.length pre) = pre
  in
  let after n s = if n >= String.length s then "" else String.sub s n (String.length s - n) in
  let pick names rest =
    List.fold_left
      (fun best (i, n) ->
        if (rest = n || starts ~pre:(n ^ "/") rest)
           && (match best with None -> true | Some (_, b) -> String.length n > String.length b)
        then Some (i, n)
        else best)
      None
      (List.mapi (fun i n -> (i, n)) names)
  in
  let tables =
    [ ("objecttypen", "objectTypes", List.map fst doc.objecttypen);
      ("feittypen", "factTypes", List.map fst doc.feittypen);
      ("parameters", "parameters", List.map fst doc.parameters);
      ("domeinen", "domains", List.map fst doc.domeinen);
      ("eenheidstelsels", "unitSystems", List.map fst doc.eenheidstelsels);
      ("dimensies", "dimensions", List.map fst doc.dimensies) ]
  in
  (* the v0 facet tables the checks still name: the member list the entry
     became part of, and the field of the member node that holds it *)
  let facets =
    [ ("attributen", ("attributes", "/datatype"));
      ("attribuutDimensies", ("attributes", "/dimensions"));
      ("attribuutTijdlijnAantal", ("attributes", "/timeline/count"));
      ("attribuutTijdlijnNaam", ("attributes", "/timeline/timelineName"));
      ("kenmerken", ("characteristics", ""));
      ("kenmerkTijdlijnAantal", ("characteristics", "/timeline/count")) ]
  in
  let member_tail (ot : objecttype) tail =
    match String.split_on_char '/' tail with
    | "" :: seg :: _ when List.mem_assoc seg facets -> (
        let members, field = List.assoc seg facets in
        let names =
          if members = "attributes" then List.map fst ot.attributen else some_names ot.kenmerken
        in
        match if tail = "/" ^ seg then None else pick names (after (String.length seg + 2) tail) with
        | Some (j, n) ->
            Printf.sprintf "/%s/%d%s%s" members j field
              (after (String.length seg + 2 + String.length n) tail)
        | None -> "/" ^ members)
    | _ -> tail
  in
  match List.find_opt (fun (v0, _, _) -> at = "/" ^ v0 || starts ~pre:("/" ^ v0 ^ "/") at) tables with
  | None -> at
  | Some (v0, m1, names) -> (
      let base = "/model/" ^ m1 in
      match if at = "/" ^ v0 then None else pick names (after (String.length v0 + 2) at) with
      | None -> base
      | Some (i, n) -> (
          let decl = Printf.sprintf "%s/%d" base i in
          let tail = after (String.length v0 + 2 + String.length n) at in
          match v0 with
          | "domeinen" -> decl ^ "/datatype" ^ tail
          | "objecttypen" -> decl ^ member_tail (snd (List.nth doc.objecttypen i)) tail
          | _ -> decl ^ tail))

let all (doc : document) : violation list =
  let ctx = { doc } in
  List.map (fun x -> { x with at = m1_named doc x.at })
  @@ List.concat
    [ wf001 ctx; wf002 ctx; wf103 ctx; wf104 ctx; wf105 ctx;
      wf003 ctx; wf004 ctx; wf005 ctx; wf006 ctx;
      wf007_model ctx; wf007_presentatie ctx; wf009 ctx; wf092 ctx; wf011 ctx; wf012 ctx;
      wf013 ctx; wf014 ctx;
      wf015 ctx; wf016 ctx; wf017 ctx; wf018 ctx; wf107 ctx; wf020 ctx; wf101 ctx; wf021 ctx;
      wf022 ctx; wf023 ctx; wf025 ctx; wf026 ctx; wf027 ctx; wf028 ctx;
      wf029 ctx; wf030 ctx; wf031 ctx; wf085 ctx; wf032 ctx; wf033 ctx;
      wf034_verdelingen ctx; wf034_verdeling_namen ctx; wf034_verdeling_typen ctx;
      wf034_verdeling_naamresolutie ctx; wf034_objectcreatie_subject ctx;
      wf045_046 ctx;
      wf047_verdelingen ctx; wf050 ctx; wf051 ctx; wf052 ctx; wf079 ctx; wf082 ctx; wf054 ctx;
      wf057 ctx; wf059 ctx; wf060 ctx; wf087 ctx; wf061 ctx; wf088 ctx; wf065 ctx; wf067 ctx;
      wf071 ctx; wf072 ctx; wf075 ctx; wf086 ctx; wf113 ctx; expression_layer ctx ]
