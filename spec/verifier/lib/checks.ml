(* Well-formedness checks over a loaded regelspraak-ir-v0 document.

   Each check implements one WF rule of spec/IR-SPEC.md, restricted to what
   the v0 wire format can express (the WF index records per rule which
   clauses are covered here and which stay OPEN). Conventions:

   - name matching is case-insensitive over whitespace-collapsed strings
     (WF-001..003); timeline names additionally strip one leading Dutch
     article (the v0 identity rule of IR-SPEC ch. 2.8);
   - v0 flattens versioned forms into per-version entries whose names carry
     a " #i" suffix; version-aware checks group by the stripped name;
   - a violation carries the WF id, the JSON-ish path, and a reason. *)

open Ast

type violation = { wf : string; at : string; reason : string }

let v wf at reason = { wf; at; reason }

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
   non-null bound is the strict yyyy-mm-dd lexeme and a real calendar day, so
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
   stelsel are irrelevant to calendar arithmetic (the stelsel may be null):
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

(* sub-day units demand millisecond-precision (datum-tijd) operands *)
let time_unit_sub_day = function
  | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde -> true
  | Tu_jaar | Tu_kwartaal | Tu_maand | Tu_week | Tu_dag -> false

(* Numeric literal lexeme sign (Dutch decimal comma; raw source string). *)
let lexeme_negative s =
  let s = String.trim s in
  String.length s > 0 && s.[0] = '-'

let lexeme_zero s =
  let s = String.trim s in
  let s = if String.length s > 0 && (s.[0] = '-' || s.[0] = '+')
          then String.sub s 1 (String.length s - 1) else s in
  s <> "" && String.for_all (fun c -> c = '0' || c = ',' || c = '.') s

(* WF-081: the executors' shared numeric-lexeme grammar (IR-SPEC ch. 4.1).
   After mapping every ',' to '.', a lexeme is
     integer  ::= '-'? digits
     decimal  ::= '-'? digits? '.' digits
     rational ::= '-'? digits '/' digits   (denominator not all zeros)
     digits   ::= [0-9]+
   Anything else — unit or currency tokens, whitespace, '+', exponents,
   grouping separators — is not a number lexeme; both executors fault on
   it at evaluation time, so the verifier refuses it up front. *)
let lexeme_bare_number s =
  let s = String.map (fun c -> if c = ',' then '.' else c) s in
  let digits d = d <> "" && String.for_all (fun c -> c >= '0' && c <= '9') d in
  let strip_sign d =
    if String.length d > 0 && d.[0] = '-' then String.sub d 1 (String.length d - 1) else d
  in
  match String.index_opt s '/' with
  | Some i ->
      let num = strip_sign (String.sub s 0 i) in
      let den = String.sub s (i + 1) (String.length s - i - 1) in
      digits num && digits den && String.exists (fun c -> c <> '0') den
  | None -> (
      match String.index_opt s '.' with
      | None -> digits (strip_sign s)
      | Some i ->
          let whole = strip_sign (String.sub s 0 i) in
          let frac = String.sub s (i + 1) (String.length s - i - 1) in
          digits frac && String.for_all (fun c -> c >= '0' && c <= '9') whole)

(* WF-084: the executors' shared date-literal lexeme grammar (IR-SPEC ch. 4.1).
   A Datum literal's value MUST be the strict ISO form yyyy-mm-dd: fixed field
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

(* WF-095: the executors' shared datum-tijd literal lexeme grammar (IR-SPEC
   ch. 4.1). TWO spellings name one instant and every reader in the house
   admits exactly these two — the canonical ISO form
   yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' as the separator and
   never a space, the seconds field always written, no zone suffix and no
   offset, a fraction that is optional but exactly three digits, read as
   whole milliseconds) and the RegelSpraak source snippet
   [dd. ]d-m-yyyy hh:mm:ss[.SSS] (day-first, the token's variable-width day
   and month), which a v0 emitter still writes into `waarde`.

   Anything else is ill-formed on the wire. Outside these two the readers of
   a datum-tijd lexeme stop agreeing — the same three digits ".1" read as
   ONE millisecond, as a HUNDRED, and refused; a missing seconds field taken
   by one reader and refused by another — and until this rule the wire had
   no fence of its own at all: a literal reading `rommel 2024-01-31
   12:00:00Z x` was schema-valid and verifier-clean (measured 2026-09-06).
   Mirror of the ref/Drools datum-tijd readers, which now refuse identically. *)
let lexeme_datum_tijd s =
  let n = String.length s in
  let digit i = i < n && s.[i] >= '0' && s.[i] <= '9' in
  let digits_at i k =
    let rec go j = j = k || (digit (i + j) && go (j + 1)) in
    go 0
  in
  let int_at i k = int_of_string (String.sub s i k) in
  let rec skip_spaces i = if i < n && s.[i] = ' ' then skip_spaces (i + 1) else i in
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
  let iso =
    n >= 19 && digits_at 0 4 && s.[4] = '-' && digits_at 5 2 && s.[7] = '-' && digits_at 8 2
    && s.[10] = 'T' && time_ok 11
    && date_ok ~y:(int_at 0 4) ~m:(int_at 5 2) ~d:(int_at 8 2)
  in
  let snippet () =
    (* the optional de-dato prefix "dd." takes at least one space after it;
       "dd." alone is not the lexeme (i0 = n then fails every digit test) *)
    let i0 =
      if n >= 4 && String.sub s 0 3 = "dd." && s.[3] = ' ' then skip_spaces 3
      else if n >= 3 && String.sub s 0 3 = "dd." then n
      else 0
    in
    let width i = if digits_at i 2 then 2 else if digits_at i 1 then 1 else 0 in
    let dw = width i0 in
    dw > 0
    && i0 + dw < n
    && s.[i0 + dw] = '-'
    &&
    let i1 = i0 + dw + 1 in
    let mw = width i1 in
    mw > 0 && i1 + mw < n && s.[i1 + mw] = '-'
    &&
    let i2 = i1 + mw + 1 in
    digits_at i2 4 && i2 + 4 < n && s.[i2 + 4] = ' '
    &&
    let i3 = skip_spaces (i2 + 4) in
    time_ok i3 && date_ok ~y:(int_at i2 4) ~m:(int_at i1 mw) ~d:(int_at i0 dw)
  in
  iso || snippet ()

(* --- document context --------------------------------------------------- *)

type ctx = { doc : document }

(* --- the supertype chain (IR-SPEC ch. 2.2) ------------------------------- *)

(* The DECLARED record, exactly as the document writes it. Three rules read
   this raw view on purpose: WF-002 (duplicates within one declaration),
   WF-105 (shadowing, which needs to see own and inherited apart) and
   WF-034's side-map key cross (a facet belongs to the type that declares
   its member). Reading those on the chain-closed view below would make
   every inherited member a duplicate of itself. WF-103 is the fourth and
   is a different reason: a supertype reference must name a DECLARED type,
   so resolving it through the chain would be circular.

   Every other raw reader in this file is not a name LOOKUP at all but an
   iteration over the declaration table, checking each record where it is
   written — WF-001, WF-002, WF-094 (both arms), WF-003, WF-004, WF-005,
   WF-007, WF-092, WF-034's side maps, WF-057, WF-059, WF-061, WF-065,
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

(* a unit spelling declared as a day kind (dagsoortPerEenheid): admitted
   beside the nine time units in date arithmetic and tijdsduur (WF-035/
   WF-038) — the executors stay loud on those forms *)
let day_kind_unit ctx (u : eenheid) =
  match u.afkorting with
  | Some a -> List.exists (fun (k, _) -> norm k = norm a) ctx.doc.dagsoort_per_eenheid
  | None -> false

(* All named rule forms, per array: (array-name, entry-names-with-validity). *)
let form_arrays ctx =
  [ ("resolvedRegels",
     List.mapi (fun i r -> (r.rr_naam, r.rr_geldigheid, Printf.sprintf "/resolvedRegels/%d" i))
       ctx.doc.resolved_regels);
    ("objectCreaties",
     List.mapi (fun i r -> (r.oc_naam, r.oc_geldigheid, Printf.sprintf "/objectCreaties/%d" i))
       ctx.doc.object_creaties);
    ("verdelingen",
     List.mapi (fun i r -> (r.vd_naam, r.vd_geldigheid, Printf.sprintf "/verdelingen/%d" i))
       ctx.doc.verdelingen);
    ("consistenties",
     List.mapi (fun i r -> (r.cons_naam, r.cons_geldigheid, Printf.sprintf "/consistenties/%d" i))
       ctx.doc.consistenties);
    ("uniciteiten",
     List.mapi (fun i r -> (r.uni_naam, r.uni_geldigheid, Printf.sprintf "/uniciteiten/%d" i))
       ctx.doc.uniciteiten);
    ("uniciteitUnies",
     List.mapi (fun i r -> (r.uu_naam, r.uu_geldigheid, Printf.sprintf "/uniciteitUnies/%d" i))
       ctx.doc.uniciteit_unies);
    ("feitCreaties",
     List.mapi (fun i r -> (r.fc_naam, r.fc_geldigheid, Printf.sprintf "/feitCreaties/%d" i))
       ctx.doc.feit_creaties) ]

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

(* ======================================================================= *)
(* Model layer: WF-001 .. WF-018                                           *)
(* ======================================================================= *)

let wf001 ctx =
  let ns name keys =
    dups_by norm (List.map (fun k -> (k, "/" ^ name)) keys)
    |> List.map (fun (a, b, at) ->
        v "WF-001" at
          (Printf.sprintf "namespace '%s': names '%s' and '%s' collide case-insensitively" name a b))
  in
  ns "objecttypen" (List.map fst ctx.doc.objecttypen)
  @ ns "domeinen" (List.map fst ctx.doc.domeinen)
  @ ns "parameters" (List.map fst ctx.doc.parameters)
  @ ns "eenheidstelsels" (List.map fst ctx.doc.eenheidstelsels)
  @ ns "feittypen" (List.map fst ctx.doc.feittypen)
  @ ns "dimensies" (List.map fst ctx.doc.dimensies)
  @ ns "dagsoorten" (some_names ctx.doc.dagsoorten)

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

(* WF-094: a keyed declaration's redundant SELF-NAME is its map key, spelled
   VERBATIM (IR-SPEC ch. 2.1's carriage rule, ch. 4.6's naming class).

   The v0 wire stores five declaration kinds as a MAP — object types,
   parameters, unit systems, fact types, dimensions — and every value repeats
   its own name in a `naam` field. That field is not a reference: it is the
   declaration's identity written a second time, the denormalisation of the
   ONE name a v1 declaration carries on its node (ch. 2.2/2.5/2.6/2.7/2.10).
   So the question this rule answers is not "does this name resolve" but "is
   the document a faithful spelling of one declaration" — ch. 2.1's carriage
   rule, not its matching rule, which is why the comparison is byte-exact and
   not `norm`.

   FIVE ARMS, one per table, because they are five independent readerships and
   each has its own witness (`wf094-zelfnaam-<kind>`); a mutant that drops one
   arm frees exactly one document, and a mutant that relaxes the comparison to
   `norm` frees all five (every witness is a one-character recapitalisation).

   The harm is a silently DIFFERENT program, not a missing value: every
   consumer that RESOLVES anything keys off the map key, and no consumer
   crosses the two. What each does read, exactly (the spec clause says the
   same): `ref` reads none of the five self-names; the ASP backend reads
   `es_naam` only to NAME a system inside a fault message it would raise
   anyway; and THIS verifier reads three of the five — `ot_naam`, `ft_naam`
   and `dim_naam`, each once, in WF-057's target-text-safety walk, which also
   walks all five KEYS and never puts a self-name beside its key. The
   parameter and unit-system self-names it does not read at all, which is why
   those two arms below have no reader in this binary other than themselves.
   The bean-model executor is the one that loads the self-name AS the
   declaration's name and reads it back at 105 (object type), 32 (parameter),
   3 (fact type) and 1 (dimension)
   sites. Measured 2026-09-06, one self-name twisted at a time over every
   golden: a parameter self-name differing from its key in ONE CAPITAL leaves
   the verifier, `ref` and the ASP backend passing on 11 of the goldens' 34
   parameter positions while the third executor answers with WRONG VALUES
   (`jutterswet-aanvoergeld`: 322.50 becomes 310.00), because the case binds
   its globals by the written name and the transpiler registers them under the
   self-name. Cost over the four zones: none — all 1919 entries the five
   tables held before this rule spell their key byte for byte (1935 after it,
   the five witnesses bringing their whole source documents along; 1937 in the
   corpus as it now stands).

   Scope: the PRESENT self-name. `naam: null` is the empty half of the same
   field; it is closed by clause (d) below, which is why this comparison may
   pass over the absent name without leaving a hole. *)
let wf094 ctx =
  let arm table kind self entries =
    List.filter_map
      (fun (key, x) ->
        match self x with
        | Some n when n <> key ->
            Some
              (v "WF-094"
                 ("/" ^ table ^ "/" ^ key ^ "/naam")
                 (Printf.sprintf
                    "%s self-name '%s' is not the key '%s' it is stored under: a keyed \
                     declaration carries its own name verbatim"
                    kind n key))
        | _ -> None)
      entries
  in
  arm "objecttypen" "object type" (fun (o : objecttype) -> o.ot_naam) ctx.doc.objecttypen
  @ arm "parameters" "parameter" (fun (p : parameter) -> p.par_naam) ctx.doc.parameters
  @ arm "eenheidstelsels" "unit system" (fun (s : eenheidstelsel) -> s.es_naam)
      ctx.doc.eenheidstelsels
  @ arm "feittypen" "fact type" (fun (f : feittype) -> f.ft_naam) ctx.doc.feittypen
  @ arm "dimensies" "dimension" (fun (d : dimensie) -> d.dim_naam) ctx.doc.dimensies

(* WF-094 clause (d): the EMPTY half of the same field — a keyed declaration
   whose `naam` is `null` rather than disagreeing with its key (maintainer
   decision 2026-09-09).

   Same class, same field, same five tables, so the same rule number: the
   clause above asks whether the second spelling of the ONE name agrees, and
   this one asks whether it is there at all. The OMISSION flank needs no clause
   — `naam` is `required` on the wire, so gate 2 rejects a missing field and
   the shared OCaml loader (this library's own, consumed by the verifier, `ref`
   and the ASP backend alike, so it is ONE refusal reached three times rather
   than three independent ones) refuses it with a load error naming the path —
   but the field is typed ["string", "null"], so `naam: null` is schema-valid,
   round-trips, and passed this verifier, `ref` and the ASP backend cleanly
   while KILLING the bean-model executor: a NullPointerException inside the
   identifier contract, which trims the name before splitting it. That is the
   shape the ch. 4.6 (n)/(o) pair does not have — there both OCaml executors
   were loud — and it is why the wire, not one loader, has to answer.

   FIVE ARMS again, one per table, one witness each
   (`wf094-zelfnaam-null-<kind>`), so a mutant dropping one arm frees exactly
   one document and no other — including its non-null sister above, which is a
   separate function for exactly that reason.

   The honest ground per arm, since it differs and the clause does not pretend
   otherwise: on `parameters` and `dimensies` the harm is MEASURED — those two
   are the loader's terugval-less tables, where the self-name goes straight
   into the identifier contract. On `objecttypen`, `eenheidstelsels` and
   `feittypen` the same loader falls back to the map key, so those three arms
   rest on the CONSTRUCT — a declaration carries its own name — and not on a
   crash. Three of five on the construct is the same split the clause above
   writes down for its own arms, and each witness's NOTE.md states which of the
   two grounds it stands on. (The Drools loader learned the fall-back on all
   five at this card as well: route 2, so a document written before this rule
   stops crashing. That fix does not make the arms superfluous — it removes the
   one LOUD reader, and without a wire rule the next consumer chooses again.) *)
let wf094_null ctx =
  let arm table kind self entries =
    List.filter_map
      (fun (key, x) ->
        match self x with
        | None ->
            Some
              (v "WF-094"
                 ("/" ^ table ^ "/" ^ key ^ "/naam")
                 (Printf.sprintf
                    "%s under the key '%s' carries no self-name (naam is null): a keyed \
                     declaration carries its own name, and an absent name is not one"
                    kind key))
        | Some _ -> None)
      entries
  in
  arm "objecttypen" "object type" (fun (o : objecttype) -> o.ot_naam) ctx.doc.objecttypen
  @ arm "parameters" "parameter" (fun (p : parameter) -> p.par_naam) ctx.doc.parameters
  @ arm "eenheidstelsels" "unit system" (fun (s : eenheidstelsel) -> s.es_naam)
      ctx.doc.eenheidstelsels
  @ arm "feittypen" "fact type" (fun (f : feittype) -> f.ft_naam) ctx.doc.feittypen
  @ arm "dimensies" "dimension" (fun (d : dimensie) -> d.dim_naam) ctx.doc.dimensies

let wf003 ctx =
  (* Model-layer resolution: no Onbekend datatype on attributes/domains;
     enumeration domains resolve; role filler types resolve; named-timeline
     references resolve. (Parameters: WF-018; dimension refs: WF-004.) *)
  let dt_check at = function
    | Some (Datatype_onbekend { reden }) ->
        [ v "WF-003" at
            ("unresolved datatype (v0 'Onbekend' sentinel"
             ^ (match reden with Some r -> ": " ^ r | None -> "") ^ ")") ]
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
                | None -> [ v "WF-004" at "null dimension entry" ]
                | _ -> [])
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
                      | None -> 1
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

(* the declared unit systems, by name — the table every unit's `stelsel`
   back-reference resolves against (WF-034, ch. 4.6 (r)). *)
let stelsel_namen ctx = List.map fst ctx.doc.eenheidstelsels

(* WF-092 (IR-SPEC ch. 2.5): the conversion pair of a unit is ONE fact in
   two fields — the canonical unit it converts into and the factor that
   does the converting. Filled together or null together; either half
   alone is not a unit. The kale form (both null) is the legitimate
   self-standing spelling every WF-038 duration parameter and WF-035
   duration operand carries.

   The rule exists because neither half is reachable by a neighbour.
   WF-009 walks the declared systems but only ever looks at a factor it
   FINDS (positivity) and at a canonical chain it can FOLLOW; a missing
   half is simply nothing to look at. WF-035/WF-023 cross units for
   COMPATIBILITY through [units_convertible], which reads a missing
   [canoniek] as the abbreviation and a missing factor not at all — so a
   half-filled unit is compatible with its whole sibling by construction.
   WF-091 compares two units BYTE for byte, which passes the moment the
   half-form is copied consistently across the arms it crosses.

   This function also carries WF-034's arm on the unit's `stelsel`
   (maintainer decision 2026-09-05, sixth wave; IR-SPEC ch. 4.6 (r)), for the
   same reason WF-092 lives here: [check_eenheid_form] is the ONE walk that
   reaches every unit record in a document, denominators included, and the
   `stelsel` is a reference like any other — it names the declared unit system
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
       [ v "WF-034" (at ^ "/stelsel")
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
             "unit %s carries conversion factor %g but names no canonical unit to convert into: a \
              half-filled conversion pair is not a unit (both fields, or neither)"
             named f) ]
  | Some _, Some _ | None, None -> [])
  @ (match u.noemer with Some n -> check_eenheid_form ~stelsels (at ^ "/noemer") n | None -> [])

(* every unit a datatype carries, denominators and composite element/field/
   payload datatypes included — WF-092 reaches strictly further into a
   datatype than [check_number_form] does, so the form law holds at unit
   positions WF-007's row records as out of its own reach. *)
let rec check_datatype_units ~stelsels at = function
  | Numeriek { eenheid = Some u; _ } | Percentage { eenheid = Some u; _ } ->
      check_eenheid_form ~stelsels (at ^ "/eenheid") u
  | List_type { element } -> check_datatype_units ~stelsels (at ^ "/element") element
  | Struct_type { velden; _ } ->
      List.concat_map (fun (f, d) -> check_datatype_units ~stelsels (at ^ "/velden/" ^ f) d) velden
  | Sum_type { constructoren; _ } ->
      List.concat_map
        (fun (c, p) ->
          match p with
          | Some d -> check_datatype_units ~stelsels (at ^ "/constructoren/" ^ c) d
          | None -> [])
        constructoren
  | Numeriek _ | Percentage _ | Tekst | Booleaan | Datum_in_dagen | Datum_tijd | Enumeratie _
  | Datatype_onbekend _ ->
      []

(* WF-092's model half: the declared unit systems (the source of truth for
   every conversion) plus every model-layer datatype. The expression half
   rides walk_expr's type prologue and the rule-target clause, where WF-007
   collects the same "every datatype in the document" split. *)
let wf092 ctx =
  let stelsels = stelsel_namen ctx in
  let site at = function Some d -> check_datatype_units ~stelsels at d | None -> [] in
  List.concat_map
    (fun (sn, es) ->
      let at = "/eenheidstelsels/" ^ sn in
      List.concat_map
        (fun (kind, table) ->
          List.concat_map
            (fun (un, e) ->
              match e with
              | Some e -> check_eenheid_form ~stelsels (at ^ "/" ^ kind ^ "/" ^ un) e
              | None -> [])
            table)
        [ ("eenheden", es.eenheden); ("aliassen", es.aliassen) ])
    ctx.doc.eenheidstelsels
  @ List.concat_map
      (fun (otn, ot) ->
        List.concat_map
          (fun (a, dt) -> site ("/objecttypen/" ^ otn ^ "/attributen/" ^ a) dt)
          ot.attributen)
      ctx.doc.objecttypen
  @ List.concat_map (fun (d, dt) -> site ("/domeinen/" ^ d) dt) ctx.doc.domeinen
  @ List.concat_map
      (fun (p, par) -> site ("/parameters/" ^ p) par.par_datatype)
      ctx.doc.parameters
  @ List.concat
      (List.mapi
         (fun i (fd : function_def) ->
           let at = Printf.sprintf "/functionDefs/%d" i in
           List.concat_map
             (fun (pn, dt) -> check_datatype_units ~stelsels (at ^ "/parameters/" ^ pn) dt)
             fd.fd_parameters
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

let wf009 ctx =
  List.concat_map
    (fun (sn, es) ->
      let at = "/eenheidstelsels/" ^ sn in
      let lookup u = find_ci es.eenheden u |> Option.join in
      List.concat_map
        (fun (un, e) ->
          match e with
          | None -> []
          | Some e ->
              (match e.factor_naar_canoniek with
               | Some f when f <= 0.0 ->
                   [ v "WF-009" (at ^ "/eenheden/" ^ un)
                       (Printf.sprintf "conversion factor %g is not positive" f) ]
               | _ -> [])
              @
              (* Follow the canonical chain; must terminate inside the system. *)
              let rec follow seen u =
                match u.canoniek with
                | None -> []
                | Some c when u.afkorting = Some c -> [] (* self-canonical *)
                | Some c ->
                    if List.mem (norm c) seen then
                      [ v "WF-009" (at ^ "/eenheden/" ^ un)
                          ("conversion cycle through '" ^ c ^ "'") ]
                    else (
                      match lookup c with
                      | None ->
                          [ v "WF-009" (at ^ "/eenheden/" ^ un)
                              ("conversion targets unknown unit '" ^ c ^ "'") ]
                      | Some next -> follow (norm c :: seen) next)
              in
              follow [ (match e.afkorting with Some a -> norm a | None -> norm un) ] e)
        es.eenheden)
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

let builtin_units =
  [ "dag"; "dagen"; "week"; "weken"; "maand"; "maanden"; "kwartaal"; "kwartalen";
    "jaar"; "jaren"; "uur"; "uren"; "minuut"; "minuten"; "seconde"; "seconden";
    "milliseconde"; "milliseconden" ]

(* WF-017 over the whole SPELLING SPACE of a day kind (maintainer decision
   2026-09-10; IR-SPEC ch. 2.9). The rule is one promoted MUST with two
   conjuncts — a day-kind spelling collides with no built-in calendar unit and
   with no other day kind's spelling — and on the v0 wire a day kind's
   spellings live in TWO places: the singular in `dagsoorten`, and every
   REGISTERED spelling, singular and plural alike, as a KEY of
   `dagsoortPerEenheid`. Until this change the check read the first place only,
   so both conjuncts were checked for the declared name and for no other
   registered spelling. The two arms below read the second place; the clause
   over `/dagsoorten` above them is unchanged.

   ARM (a) — a registry KEY that shadows a built-in calendar unit. Such a key
   makes the document contradict itself: `RUNTIME-SEMANTICS.md` ch. 1.2 states
   normatively that a duration's unit is recognised by spelling through the
   calendar table and that "a spelling outside the table is a declared day
   kind", while a registry key declares exactly that spelling to be a day-kind
   unit. Note what this arm does NOT claim, measured 2026-09-10: `day_kind_unit`
   is consulted here (WF-035, WF-038) only in the None branch of
   `time_unit_of_eenheid`, and `ref` and the ASP backend resolve in the same
   order, so on those three a key `dagen` changes nothing — `datum + 5 dagen`
   stays calendar arithmetic. The Drools backend reverses the order at all four
   of its day-kind positions (it asks the registry before the table), so the
   same document reads as day-kind arithmetic there. That divergence — the
   executors splitting on one document — is the arm's second reason; the
   backend's lookup order is its own conformance question and does not remove
   the contradiction on the wire. Measured 2026-09-10 on the pre-arm binary (md5
   67becaba23c6d3364a9f1a080355541e), `jutterswet-schouwrooster` with one
   entry added and nothing else changed: `"dagen": "schouwdag"` beside the four
   ordinary entries was `verifier: PASS`, while the same spelling DECLARED is
   refused by the clause above. Here the IR is weaker than its own producer,
   and what carries that is the REGISTRATION GUARD, not the grammar: the
   reference frontend refuses to register a spelling that coincides with any of
   the eighteen built-in time-unit names, warns, and drops the entry. The
   grammar helps for most of them but not all — measured 2026-09-10 over all
   eighteen as a day-kind plural, FOURTEEN fail to parse (the names are
   keywords, no IR is emitted at all) and FOUR parse cleanly: `uren`,
   `minuten`, `seconden`, `milliseconden`. For those four the frontend emits
   with exit 0 and the guard alone does the work — the registry comes out
   without the spelling and the warning lands in the emitted document's
   `diagnostics` array rather than on the console. Either way no document this
   frontend emits carries the shape, so the arm is wire-only, and that is
   precisely why it must be a wire rule or nothing.
   Witness: `corpus/negatief/wf017-eenheidsleutel-schaduwt-kalendereenheid`.

   The FLANK this arm leaves open, named because it is not small: the guard's
   eighteen names are the singulars and plurals, while the calendar table
   recognises THIRTY spellings. The twelve abbreviations (`jr`, `mnd`, `kw`,
   `wk`, `dg`, `u`, `min`, `sec`, `s`, `ms`, `secondes`, `millisecondes`) pass
   both this arm and the clause above. Measured 2026-09-10: all twelve are
   verifier-clean as registry keys, and this flank is NOT wire-only — the
   frontend emits `Dagsoort de wk (mv: wks)` with exit 0 and an empty
   `diagnostics`, registering `wk`. Goldens use the abbreviations as live
   calendar units (`jutterswet-vervaltermijn`: `wk`, `mnd`, `kw`), so with the
   Drools lookup order above such a document splits the executors in earnest.
   Widening to the thirty-spelling table would have to move the frontend's
   guard with it; carded as
   `backlog/new/20260910-dagsoort-afkortingssleutels.md`.

   ARM (b) — a DECLARED day-kind name that occurs as a key must bind to
   ITSELF. This is the wire-visible form of the day-kind/day-kind collision.
   The collision itself is NOT visible on the wire: the map holds one value per
   key, so the losing registration is simply absent. What remains visible is
   the shape it leaves behind — a declared name registered as the unit spelling
   of a DIFFERENT day kind. Measured end-to-end 2026-09-10 with the reference
   frontend on two day kinds `schouwdag` (plural `schouwdagen`) and
   `schouwdagen` (plural `schouwdagens`): the frontend emits with no warning
   and no error (`registreerDagsoortEenheid` uses `putIfAbsent` — first-wins,
   silently), the registry binds `"schouwdagen": "schouwdag"`, and the pre-arm
   verifier reported PASS. So `5 schouwdagen` reckons with the definition of
   `schouwdag` while a reader of `dagsoorten` sees a day kind `schouwdagen`
   declared. WF-001 does not reach it: the two DECLARED names differ. Here the
   producer is exactly as weak as the wire, which is why the conformance side
   of the pin needs a frontend diagnostic of its own; this arm is the wire half.
   Witness: `corpus/negatief/wf017-dagsoortnaam-bindt-vreemde-dagsoort`, the
   frontend's own emission of that source.

   IDENTITY. Both arms compare under `norm` — collapse whitespace, lower case —
   the identity `day_kind_unit` already applies to the key and the clause above
   already applies to the declared name, so the three positions agree on what
   "the same spelling" means. It is pinned from the positive side:
   `corpus/golden/jutterswet-vormenkabinet` registers the declared `schouwdag`
   as `"schouwdag": "SchouwDag"`, which points to itself only through `norm`;
   a byte-exact arm (b) reports that entry and takes gate 3 down.

   BOUNDARY against WF-034 arm (s) (ch. 4.6), which reads the VALUES of this
   same map. That arm asks whether one name RESOLVES against `dagsoorten` and
   compares nothing; these arms COMPARE two spellings and resolve nothing. Arm
   (b) therefore fires only when the value names a day kind that IS declared —
   an unresolvable value, a null value and the empty string name no second day
   kind to collide with, and are WF-034 (s)'s single diagnosis, the same
   discipline WF-085 keeps for undeclared references. NO corpus document
   instantiates that touch point, so it is measured by construction: a declared
   name as key whose value is an undeclared day kind, a null, or the empty
   string — three documents built for the measurement, each reporting exactly
   one line, and each time WF-034's. (The corpus witness
   `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort` does NOT
   instantiate it: its offending key is not a declared name, so arm (b) is
   silent there through the declared-name lookup and never reaches the
   step-back.)

   COST, measured 2026-09-10 over all four zones plus the read-only parity zone
   (585 documents): the two arms move the two witnesses above and not one other
   verdict, and add no report line anywhere else. *)
let wf017 ctx =
  let declared = some_names ctx.doc.dagsoorten in
  List.concat_map
    (fun d ->
      match d with
      | Some d when List.mem (norm d) builtin_units ->
          [ v "WF-017" "/dagsoorten"
              ("day-kind '" ^ d ^ "' shadows a built-in calendar unit") ]
      | _ -> [])
    ctx.doc.dagsoorten
  @ List.filter_map
      (fun (spelling, _) ->
        if List.mem (norm spelling) builtin_units then
          Some
            (v "WF-017"
               ("/dagsoortPerEenheid/" ^ spelling)
               (Printf.sprintf
                  "registered day-kind unit spelling '%s' shadows a built-in calendar unit"
                  spelling))
        else None)
      ctx.doc.dagsoort_per_eenheid
  @ List.filter_map
      (fun (spelling, dk) ->
        match List.find_opt (fun d -> norm d = norm spelling) declared, dk with
        | Some own, Some d when norm d <> norm own && mem_ci declared d ->
            Some
              (v "WF-017"
                 ("/dagsoortPerEenheid/" ^ spelling)
                 (Printf.sprintf
                    "declared day kind '%s' is registered as a unit spelling of day kind '%s': \
                     its own spelling binds to another day kind"
                    own d))
        | _ -> None)
      ctx.doc.dagsoort_per_eenheid

let wf018 ctx =
  List.concat_map
    (fun (p, par) ->
      let at = "/parameters/" ^ p in
      match par.par_datatype with
      | None -> [ v "WF-018" at "parameter without a datatype" ]
      | Some (Datatype_onbekend { reden }) ->
          [ v "WF-018" at
              ("parameter datatype unresolved (v0 'Onbekend' sentinel"
               ^ (match reden with Some r -> ": " ^ r | None -> "") ^ ")") ]
      | Some (Enumeratie { domein = Some d; _ }) when find_ci ctx.doc.domeinen d = None ->
          [ v "WF-018" at ("parameter enumeration references undeclared domain '" ^ d ^ "'") ]
      | Some _ -> [])
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
      List.filter_map
        (fun (n, _, at) ->
          match n with
          | Some s when collapse_ws s <> "" -> None
          | Some _ ->
              Some (v "WF-107" (at ^ "/naam") ("form in " ^ arr ^ " carries a blank name" ^ why))
          | None ->
              Some (v "WF-107" (at ^ "/naam") ("form in " ^ arr ^ " carries no name" ^ why)))
        entries)
    (named_forms ctx)

let wf020 ctx =
  let arrays = named_forms ctx in
  (* cross-array: a stripped name in more than one form array *)
  let stripped =
    List.concat_map
      (fun (arr, entries) ->
        List.filter_map
          (fun (n, _, at) -> Option.map (fun n -> (norm (strip_version n), arr, n, at)) n)
          entries)
      arrays
  in
  let cross =
    List.filter_map
      (fun (k, arr, n, at) ->
        match
          List.find_opt (fun (k', arr', _, _) -> k = k' && arr <> arr') stripped
        with
        | Some (_, arr', n', _) when arr < arr' ->
            Some (v "WF-020" at
                    (Printf.sprintf
                       "name '%s' collides with '%s' in %s (unique across all named forms)"
                       n n' arr'))
        | _ -> None)
      stripped
  in
  (* within one array: same full name twice, or bare + versioned mix *)
  let within =
    List.concat_map
      (fun (arr, entries) ->
        let named =
          List.filter_map (fun (n, _, at) -> Option.map (fun n -> (n, at)) n) entries
        in
        (dups_by norm named
         |> List.map (fun (a, _, at) ->
             v "WF-020" at (Printf.sprintf "duplicate name '%s' in %s" a arr)))
        @
        let groups = List.sort_uniq compare (List.map (fun (n, _) -> norm (strip_version n)) named) in
        List.concat_map
          (fun g ->
            let members = List.filter (fun (n, _) -> norm (strip_version n) = g) named in
            let bare = List.exists (fun (n, _) -> not (is_versioned n)) members in
            (* and a versioned one to mix it WITH: without this the arm also
               fires on two entries of one bare name, which is the duplicate
               above and names no version — a false second report on a
               document the first arm already refuses (measured 2026-09-12 on
               wf020-defaultsgroep-duplicaat). It frees nothing: with no
               versioned member the stripped name IS the name, so [dups_by]
               has already caught the group. *)
            let versioned = List.exists (fun (n, _) -> is_versioned n) members in
            if List.length members > 1 && bare && versioned then
              [ v "WF-020" (snd (List.hd members))
                  (Printf.sprintf
                     "form '%s' in %s mixes a bare name with versioned entries" g arr) ]
            else [])
          groups)
      arrays
  in
  cross @ within

(* WF-101: a validity window's bounds carry WF-084's date lexeme (IR-SPEC
   ch. 4.1). Every non-null `vanaf`/`tot` on every Geldigheid — the nine
   carriers below — is the strict ISO form yyyy-mm-dd AND a real calendar
   day, checked by the very predicate WF-084 uses on a date literal.

   It exists to make the assumption stated at validity_overlaps above TRUE
   before that function relies on it: three readers (this verifier, ref and
   the ASP backend) hold a bound as an OPAQUE STRING and order it with `<=`,
   and lexicographic order agrees with calendar order only INSIDE this
   lexeme. Measured 2026-09-11 on jutterswet-vensterheffing with one bound
   twisted, no reader faulting anywhere: `+012026-01-01` and `-0500-01-01`
   sort before every four-digit year, so the window silently opens at the
   start and both executors applied a rule version outside it (125 where the
   pinned answer is leeg); `niet-een-datum` sorts after every date, closing
   the window over its own casus (leeg where the pinned answer is 125).
   Every one of those is a silent wrong answer, not a fault.

   The calendar half is the layer the schema pattern on these two fields
   cannot reach: `2026-02-30` matches that pattern, orders correctly, and
   names a day that does not exist — a reader that PARSES the bound refuses
   it where the three opaque-string readers do not. *)
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
                     (at ^ "/geldigheid/" ^ field)
                     ("validity bound '" ^ w
                      ^ "' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges \
                         — ch. 4.1)"))
            | _ -> None)
          [ ("vanaf", g.geld_vanaf); ("tot", g.geld_tot) ]
  in
  List.concat_map (fun (_, entries) -> List.concat_map (fun (_, g, at) -> bounds at g) entries)
    (named_forms ctx)
  (* The one carrier named_forms does not list. A day-kind window is refused
     outright by WF-031 (it must be ALTIJD), so on a conforming document this
     arm is unreachable; it is here so the rule holds of every Geldigheid on
     the wire rather than of eight of nine. *)
  @ List.concat
      (List.mapi
         (fun i (r : resolved_dagsoort) ->
           bounds (Printf.sprintf "/resolvedDagsoorten/%d" i) r.rd_geldigheid)
         ctx.doc.resolved_dagsoorten)

let wf021 ctx =
  List.concat_map
    (fun (arr, entries) ->
      let named =
        List.filter_map (fun (n, g, at) -> Option.map (fun n -> (n, g, at)) n) entries
      in
      let groups =
        List.sort_uniq compare (List.map (fun (n, _, _) -> norm (strip_version n)) named)
      in
      List.concat_map
        (fun g ->
          let members = List.filter (fun (n, _, _) -> norm (strip_version n) = g) named in
          let rec pairs = function
            | [] -> []
            | (n1, g1, at1) :: rest ->
                List.filter_map
                  (fun (n2, g2, _) ->
                    if validity_overlaps g1 g2 then
                      Some (v "WF-021" at1
                              (Printf.sprintf
                                 "validity windows of versions '%s' and '%s' overlap (%s)" n1 n2 arr))
                    else None)
                  rest
                @ pairs rest
          in
          if List.length members > 1 then pairs members else [])
        groups)
    (form_arrays ctx)

let wf022 ctx =
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/resolvedRegels/%d" i in
      match r.doel with
      | Some (Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ }) -> (
          match objecttype ctx otn with
          | None -> [ v "WF-022" (at ^ "/doel") ("target object type '" ^ otn ^ "' is not declared") ]
          | Some ot ->
              if has_kenmerk ot a then
                (* characteristic target: value must be the constant true *)
                match r.waarde with
                | Some (Literal { waarde = Some "waar"; typ = Some Booleaan; _ }) -> []
                | _ ->
                    [ v "WF-022" (at ^ "/waarde")
                        "characteristic target's value must be the boolean constant true" ]
              else if has_attr ot a then []
              else
                [ v "WF-022" (at ^ "/doel")
                    ("target '" ^ a ^ "' is neither an attribute nor a characteristic of '" ^ otn ^ "'") ])
      | Some _ -> [ v "WF-022" (at ^ "/doel") "rule target is not an attribute reference" ]
      | None -> [ v "WF-022" (at ^ "/doel") "rule without a target" ])
    (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_regels)

let wf024 ctx =
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/verdelingen/%d/methode" i in
      match r.vd_methode with
      | Some Complex -> [ v "WF-024" at "allocation method COMPLEX is not admissible IR" ]
      | None -> [ v "WF-024" at "allocation without a method" ]
      | Some _ -> [])
    (List.mapi (fun i r -> (i, r)) ctx.doc.verdelingen)

let wf025 ctx =
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/verdelingen/%d" i in
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
    (List.mapi (fun i r -> (i, r)) ctx.doc.verdelingen)

(* The single-writer fences (WF-026 here, WF-050 and WF-079 below) key on
   the DECLARED object-type name, and under subtyping they MUST keep doing
   so — this is a constraint on the verifier, not an oversight (IR-SPEC
   ch. 6.1, RUNTIME-SEMANTICS ch. 7). They are read on the per-type
   EFFECTIVE stack, after inheritance resolution, never on a chain-closed
   target key. The hazard runs the counter-intuitive way: today the fences
   do not fire on a supertype/subtype pair at all, because 'Strandrecht.x'
   and 'Wrakrecht.x' are two keys. Lifting the target key to the
   chain-closed set — the obvious repair once member resolution walks the
   chain — would make them fire on exactly the shape inheritance
   legitimises, a supertype base plus a subtype base on one target, and
   make the construct unreachable. In the effective stack that pair is ONE
   writer: the subtype's own base replaces the inherited one. Any
   chain-awareness added here must ask "do these two writers survive into
   one instance's effective stack", never "do these two targets close over
   the same member". *)
let wf026 ctx =
  let named =
    List.mapi
      (fun i r ->
        (Option.map strip_version r.vd_naam, r.doel_type, r.doel_attribuut,
         Printf.sprintf "/verdelingen/%d" i))
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

(* WF-034 on the VALUE half of the day-kind unit registry (maintainer decision
   2026-09-09; IR-SPEC ch. 4.6 (s)). `dagsoortPerEenheid` couples a written
   UNIT SPELLING (the key) to the NAME of a day kind (the value). The key side
   is already read here — `day_kind_unit` above asks whether a spelling occurs
   as a key, and admits it beside the nine calendar units in date arithmetic
   (WF-035) and in a tijdsduur (WF-038). The value was crossed against nothing,
   so that admission could rest on a name `dagsoorten` never declares: the
   spelling is let through as a day-kind unit while the day kind it names does
   not exist.

   The value is a REFERENCE and the only one this map carries, which is what
   separates this arm from WF-017 (ch. 2.9): WF-017 asks whether two spellings
   COLLIDE — a day kind against a built-in calendar unit, or against another
   day kind — and resolves nothing; this arm asks whether one name RESOLVES,
   and compares nothing. They cannot double: since 2026-09-10 `wf017` above
   reads this map too, but only its KEYS (arms (a) and (b)), while this arm
   reads only the VALUES. Where the two could meet — a declared name as key
   whose value names no declared day kind — arm (b) steps back and leaves the
   single diagnosis here. No corpus document instantiates that touch point (the
   witness below does not: its offending key is not a declared name), so it is
   measured by construction — the three shapes of a declared name as key with
   an unresolvable, null and empty value each report exactly one line on
   2026-09-10, and each time it is this arm's.

   The KEY side carries no RESOLUTION rule, and that is a decision, not an
   omission (WF-017's collision arms are a different question). The
   carrying argument is what the WIRE carries: a day kind's plural arrives as a
   FURTHER KEY of this same map and as no field of its own (ch. 2.9's `plural`
   exists on the v1 node), so nothing here distinguishes a DECLARED plural from
   a DERIVED one or from an abbreviation — all three are ordinary keys. A key
   rule would have to reconstruct that distinction by fixing Dutch morphology
   inside an engine-neutral IR, and would bind every producer to one language's
   plural formation. (The source language does make the plural an optional
   author field, §3.12; that is not the ground, and it does not mean a plural
   is absent when the author omits one — the reference frontend derives a `-en`
   plural in that case and cites §3.12 for it. The wire cannot tell the two
   apart, which is the ground.)

   Measured on the LANDED state (2026-09-09, 555 documents): 35 of the 70
   entries are keyed on something other than the declared name. That counts
   entries, not independent cases — the 70 entries carry four distinct
   (registry, declarations) shapes, one repeated byte-identically over 30
   parity fragments, and the 35 keys spell three distinct words, 34 of them the
   plain `<declared name>en` form the frontend itself derives. The corpus has
   zero abbreviation and zero irregular-plural witnesses.

   Both conjuncts of WF-017 reach a key since 2026-09-10: a registry key that
   shadows a built-in calendar unit, and a declared day kind whose own singular
   is registered as another day kind's spelling (`putIfAbsent`, first-wins)
   are refused there, each with its own witness. Neither is this arm's.

   Observability, measured 2026-09-09 on the twist this arm's witness carries:
   NO consumer notices. The verifier, `ref`, the ASP backend and the Drools
   backend all reproduce the host golden's pinned outcomes (PASS, PASS, PASS,
   PLAYED 2). `ref` and the ASP backend never read the map. The Drools backend
   reads it at exactly two positions, the §6.10 tijdsduur and the §6.11 date
   arithmetic, and defends itself there ad hoc: an unresolvable name yields a
   null day-kind global and the transpiler emits a MARKER, so the form is
   silently not carried rather than refused — and no golden can carry those two
   forms, because `ref` and the ASP backend both refuse day-kind durations
   outright. So this is an arm for the wire, on the wave-6 `kenmerkSoorten`
   argument: a name that crosses nothing in any executor still points at a
   declaration that has to exist for the document to mean what it says.

   THREE spellings of the defect, ONE arm with one guard: a value naming an
   undeclared day kind, a `null` value, and the empty string — none of the
   three names a declared day kind. The schema types the value
   `["string", "null"]`, so the null is schema-valid, and the one reader
   collapses null onto the naming case (its loader reads the value with
   `asText()`, which turns a JSON null into the string "null" and then fails
   the same lookup). Only the naming spelling has a corpus witness; of the 70
   entries the four zones hold, zero are null and zero are empty. Both silent
   spellings are folded into this arm rather than given clauses of their own,
   the wave-5-fix-round precedent (one list-driven arm where a sub-arm has no
   counterexample of its own).

   The IDENTITY the arm compares with is `norm` — collapse whitespace, lower
   case — the same one `day_kind_unit` applies to the key, and it is pinned:
   `corpus/golden/jutterswet-vormenkabinet` carries two entries that resolve
   ONLY through it (`SchouwDag` and a padded `  schouwdag  ` against a declared
   `schouwdag`). Replacing `mem_ci` here with a byte-exact `List.mem` takes
   gate 3 down on that golden. Before those two entries existed the
   normalisation half moved no document at all. *)
let wf034_dagsoort_eenheden ctx =
  List.filter_map
    (fun (spelling, dk) ->
      let at = "/dagsoortPerEenheid/" ^ spelling in
      match dk with
      | Some d when mem_ci (some_names ctx.doc.dagsoorten) d -> None
      | Some d ->
          Some
            (v "WF-034" at
               (Printf.sprintf
                  "day-kind unit '%s' names day kind '%s', which no declaration carries: the \
                   registry entry resolves against no day kind"
                  spelling d))
      | None ->
          Some
            (v "WF-034" at
               (Printf.sprintf
                  "day-kind unit '%s' names no day kind at all: the registry entry resolves \
                   against no day kind"
                  spelling)))
    ctx.doc.dagsoort_per_eenheid

(* WF-034 on the EIGHT SIDE-TABLE KEYS of an object type (maintainer decision
   2026-09-05, sixth wave; IR-SPEC ch. 4.6 (p)). An object type declares its
   members in two places — `attributen` (name -> datatype) and `kenmerken` (a
   name list) — and then hangs eight per-member FACET maps beside them, each
   keyed by a member name. Nothing required those keys to name a declared
   member. They are references, and they are more than that: a side-table key
   is the v0 DENORMALISATION of a facet v1 carries on the member node itself
   (IR-SPEC ch. 2.2 / `CONSTRUCTS.md` row 11 — "each v0 map entry folds into
   the corresponding member node"), so the key IS that member's identity,
   written out beside the declaration instead of on it. A key that names no
   member resolves against no declaration; a key that MISSPELLS one carries an
   identity nobody declared.

   The harm is a SILENT SEMANTIC CHANGE, not a missing value, and that is what
   separates this arm from the empty-field class of the earlier waves. A key in
   `kenmerkGranulariteit` spelled `verjaart` where the declared characteristic
   is `verjaard` says "this characteristic is time-dependent" about a
   characteristic that does not exist — and leaves the declared one
   NON-temporal, silently. No consumer complains: a case-insensitive reader
   looks the facet up by the declared member's name, finds nothing, and takes
   the default. The CASE-SENSITIVE half of the readership (below) reaches the
   same silence by the other road — it does not find the key at all. That is a
   different program, not a fault.

   EIGHT arms, not one, because the eight tables are eight independent readers
   and a mutant that removes one must free exactly one document. Measured over
   the four zones (2026-09-05): the readers per table are

     attribuutMeervoud       NO wire consumer of the KEY. WF-057 walks this
                             table but reads the VALUE — the plural's
                             target-text-safety — and discards the key; the
                             Drools loader stores the map and nothing reads it
                             back. The key's only reader is the EMITTER side
                             (the frontend's `MeervoudMatcher`, off its own
                             in-memory model), which is not a consumer of the
                             wire
     attribuutDimensies      WF-004 (the dimension list) and WF-040's
                             label/dimension cross inside [walk_expr]
     attribuutGranulariteit  [attr_temporal], WF-005's named-timeline
                             adoption, WF-043 in [walk_expr], the temporal
                             prologue of [expression_layer], WF-065 — and all
                             three executors (`ref` eval/case, the ASP
                             translator, the Drools transpiler + contract
                             validator + fact-model generator)
     attribuutTijdlijnAantal WF-005; the two Drools transpiler branches
     attribuutTijdlijnNaam   the KEY is a CROSS-TABLE JOIN key in WF-005, which
                             carries it into [attribuut_granulariteit] and
                             [attribuut_tijdlijn_aantal] to compare the adopted
                             facets against the declaration ([find_ci], so ci
                             on this side); WF-003 reads the VALUE only. The
                             Drools loader stores the map and nothing reads it
                             back; the RegelSpraak frontend reads it
                             case-sensitively at two sites off its own model —
                             the emitter again, not the wire
     kenmerkSoorten          NO reader in any of the four consumers — the
                             Drools loader parses it and stops there
     kenmerkGranulariteit    WF-043 in [walk_expr] and [walk_cond],
                             [expression_layer], WF-086 — and all three
                             executors
     kenmerkTijdlijnAantal   WF-005; the two Drools transpiler branches

   so the arms differ in how much they switch off, from "everything a temporal
   member has" down to `kenmerkSoorten` and `attribuutMeervoud`, whose KEYS no
   wire consumer reads at all and which are here for the wire: the tables are
   keyed by member name in the serialization contract, and a conforming
   consumer may read them tomorrow.
   Cost: none. All 924 keys the four zones held before this wave (24 + 9 + 74 +
   2 + 2 + 788 + 24 + 1) resolve against a declared member; the only orphans
   anywhere are this wave's own eight witnesses.

   Identity is BYTE-EXACT: the key MUST be the member's declared spelling,
   whitespace and case included (fix round 2026-09-06). That is ch. 2.1's
   CARRIAGE rule, not its matching rule — "Declared names are carried verbatim
   (whitespace-collapsed, case preserved). Name matching is case-insensitive."
   A side-table key is not a matching site: it is the member's denormalised
   identity (the paragraph above), and an identity is carried verbatim, so this
   arm asks the same question of the key that the v1 member node would answer
   by construction.

   The ground is MEASURED, not stylistic. The consumers do not agree on how
   they look these facets up. The verifier ([find_ci] / [norm]), `ref` and the
   ASP translator are case-insensitive; the bean-model executor is NOT. The
   Drools backend reaches these maps through raw, CASE-SENSITIVE lookups at
   109 sites across four of the eight tables (attribuutGranulariteit 65,
   kenmerkGranulariteit 20, attribuutTijdlijnAantal 17, kenmerkTijdlijnAantal
   7; remeasured 2026-09-11), against five case-insensitive lookups through
   its own [Namen.bevatNaam] helper. The two GRANULARITY tables are read at
   the call site with [Map.get] / [Map.containsKey]; the two COUNT tables
   are read through one shared predicate ([Tijdlijnen.meerPeriodiek],
   2026-09-11), which does the same raw [Map.get] on the map its caller
   hands it — so a grep for a table name beside those two methods now finds
   85, and the remaining 24 positions are the ones that pass the map along.
   Under a case-INsensitive arm, therefore, a key differing from its
   declaration in capitalisation alone is no orphan and the document stays
   verifier-clean, ref-clean and clingo-clean while the third executor drops
   the golden out of the differential in silence. Measured 2026-09-06 on
   `corpus/golden/synth2-clip-over-prorata`, `kenmerkGranulariteit` key
   `borgplichtig` capitalised (a one-character diff): verifier PASS, `ref` PASS
   (3 expectations), clingo PASS (3 expectations), and Drools PLAYED -> EXCLUDE
   (a transpiler SKIP: the assignment is no longer temporal), which takes gate
   12 from played 109 to played 108 and leaves it GREEN — its only floor is
   played > 0. The wire has to catch what no gate catches; the witness is
   `corpus/negatief/wf034-zijtabel-kapitalisatiewees`.

   Cost of the byte rule: none. Of the 943 keys the four zones held after the
   sixth wave and before this round, ZERO differ from the declared member in
   byte while resolving case-insensitively; before the wave, likewise zero of
   924.

   SCOPE is exactly these eight keys. The neighbouring arms stay
   case-insensitive on purpose: a characteristic check's `eigenaar` and a
   unit's `stelsel` are ordinary NAME RESOLUTION — a name pointing AT a
   declaration, not a declaration's identity written out — and every reader of
   those resolves case-insensitively ([find_ci] here, the ASP translator, and
   the Drools loader's exact-then-`equalsIgnoreCase` object-type lookup).
   `corpus/negatief/wf091-attribuutref-eenheid-kast` pins that boundary
   deliberately, carrying `stelsel: "GEWICHT"` against a declared `Gewicht`:
   name RESOLUTION is case-insensitive, copy FIDELITY is a different question,
   and a byte rule on `stelsel` would make that witness say something else. *)
let wf034_zijtabelsleutels ctx =
  let orphan otn tab kind present table =
    List.filter_map
      (fun (key, _) ->
        if present key then None
        else
          Some
            (v "WF-034"
               ("/objecttypen/" ^ otn ^ "/" ^ tab ^ "/" ^ key)
               (Printf.sprintf
                  "%s key '%s' names no declared %s of '%s': the side table resolves against no \
                   declaration"
                  tab key kind otn)))
      table
  in
  List.concat_map
    (fun (otn, ot) ->
      (* BYTE-EXACT, and passed per table so a ci-relapse on ONE table is an
         arm-exact mutant: the key IS the member's identity, and an identity is
         carried verbatim (ch. 2.1's carriage rule — see the block above). *)
      let a n = List.exists (fun (dk, _) -> dk = n) ot.attributen
      and k n = List.exists (fun x -> x = Some n) ot.kenmerken in
      orphan otn "attribuutMeervoud" "attribute" a ot.attribuut_meervoud
      @ orphan otn "attribuutDimensies" "attribute" a ot.attribuut_dimensies
      @ orphan otn "attribuutGranulariteit" "attribute" a ot.attribuut_granulariteit
      @ orphan otn "attribuutTijdlijnAantal" "attribute" a ot.attribuut_tijdlijn_aantal
      @ orphan otn "attribuutTijdlijnNaam" "attribute" a ot.attribuut_tijdlijn_naam
      @ orphan otn "kenmerkSoorten" "characteristic" k ot.kenmerk_soorten
      @ orphan otn "kenmerkGranulariteit" "characteristic" k ot.kenmerk_granulariteit
      @ orphan otn "kenmerkTijdlijnAantal" "characteristic" k ot.kenmerk_tijdlijn_aantal)
    ctx.doc.objecttypen

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
   declaration (`bronAttribuut: "bestaatniet"` was verifier-clean while `ref`
   faulted and clingo reported `unknown attribute`), and the two TYPE fields
   `bronType`/`doelType`, equally emptyable and equally clean. Both are name
   resolution against the object-type table, a wider movement than this arm's
   empty-field class — and both are CLOSED by the fourth wave seventy lines
   below, [wf034_verdeling_typen] and [wf034_verdeling_naamresolutie]
   (maintainer decision 2026-09-04, IR-SPEC ch. 4.6 (j)–(k)). What stays this
   arm's own is the NULL half: an absent field names nothing to resolve, so
   there is no scope question to ask and no message to phrase in terms of a
   declaration table. *)
let wf034_verdelingen ctx =
  List.concat
    (List.mapi
       (fun i (r : resolved_verdeling) ->
         let at = Printf.sprintf "/verdelingen/%d" i in
         (match r.bron_attribuut with
          | None ->
              [ v "WF-034" (at ^ "/bronAttribuut")
                  "allocation without a source attribute: it resolves against no declaration" ]
          | Some _ -> [])
         @
         match r.doel_attribuut with
         | None ->
             [ v "WF-034" (at ^ "/doelAttribuut")
                 "allocation without a target attribute: it resolves against no declaration" ]
         | Some _ -> [])
       ctx.doc.verdelingen)

(* WF-034 on the allocation's SIX attribute-name fields, the filled half of the
   same defect (maintainer decision 2026-09-04, third wave, fix round). The arm
   above reads `Some a` as "a name is there"; an empty string satisfies it and
   names nothing, so every rule behind `Some a` stays as silent as it is on
   null. This clause is the other half: a PRESENT attribute name is not empty.
   Absence keeps its meaning — null on `ratoAttribuut`, `maximumAttribuut`,
   `restantAttribuut` and `groepeerAttribuut` is a facet switched off, and
   `ratoAttribuut: null` under NAAR_RATO is already WF-025's business — but a
   field that is present must name something.

   Emptiness is decided by the verifier's own name identity (whitespace
   collapsed, lowercased, ch. 2.1): a blank spelling is the same empty name and
   resolves against the same nothing.

   Measured on the golden allocations, one field emptied to "" at a time, every
   one of them verifier-CLEAN before this clause and 0 after — and the executor
   consequence is worse than the null half's, because it is not uniformly loud:

     bronAttribuut     11/11  ref and clingo both fault (empty source amount /
                              unknown attribute "")
     doelAttribuut     11/11  both fault (unknown attribute "")
     restantAttribuut   9/9   both fault (unknown attribute "")
     ratoAttribuut      7/7   BOTH SILENT: no fault, a wrong value
                              (anchor jutterswet-bergingstoezicht:
                              bergingsaandeel underived, 60 pinned)
     maximumAttribuut   4/4   ref SILENT (jutterswet-berging: 600 for 300),
                              clingo faults — ref != clingo on a document the
                              verifier called well-formed
     groepeerAttribuut  3/3   BOTH SILENT (jutterswet-rangaflopend: 300 for
                              1200; the ordering key vanishes and the whole
                              pot goes to one recipient)

   The three silent rows are where the differential square is blind: only the
   hand-pinned anchor expectations catch them, never backend agreement. *)
let wf034_verdeling_namen ctx =
  List.concat
    (List.mapi
       (fun i (r : resolved_verdeling) ->
         let at = Printf.sprintf "/verdelingen/%d" i in
         let filled field = function
           | Some a when norm a = "" ->
               [ v "WF-034" (at ^ "/" ^ field)
                   (field
                  ^ " is present but empty: an attribute name that names nothing \
                     resolves against no declaration") ]
           | _ -> []
         in
         filled "bronAttribuut" r.bron_attribuut
         @ filled "doelAttribuut" r.doel_attribuut
         @ filled "ratoAttribuut" r.rato_attribuut
         @ filled "maximumAttribuut" r.maximum_attribuut
         @ filled "restantAttribuut" r.restant_attribuut
         @ filled "groepeerAttribuut" r.groepeer_attribuut)
       ctx.doc.verdelingen)

(* WF-034 on the allocation's two TYPE fields (maintainer decision 2026-09-04,
   fourth wave). The empty-reference class of the earlier waves, one field
   further along: an allocation whose `bronType` or `doelType` is null names
   the object type it distributes FROM, or TO, nowhere. An UNDECLARED type is
   already caught — WF-047 crosses it against the connecting role's fillers on
   the bound form, WF-048 against the derivable fact type on the bindingless
   one — but both of those read `Some t` and a null slips past them together
   with everything else keyed on the type: WF-025's ordering-key datatype,
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
  List.concat
    (List.mapi
       (fun i (r : resolved_verdeling) ->
         let at = Printf.sprintf "/verdelingen/%d" i in
         (match r.bron_type with
          | None ->
              [ v "WF-034" (at ^ "/bronType")
                  "allocation without a source object type: it resolves against no declaration" ]
          | Some _ -> [])
         @
         match r.doel_type with
         | None ->
             [ v "WF-034" (at ^ "/doelType")
                 "allocation without a target object type: it resolves against no declaration" ]
         | Some _ -> [])
       ctx.doc.verdelingen)

(* WF-034 on the allocation's six attribute names, the RESOLUTION half
   (maintainer decision 2026-09-04, fourth wave). The two arms above ask
   whether a name is there and whether it is more than blanks; this one asks
   the question the rule is actually about — does the name resolve? Each field
   resolves against the declaration scope of ITS OWN SIDE, the sides both
   executors read it on:

     bronType   bronAttribuut     the pot that is distributed (read)
                restantAttribuut  the remainder written back on the source
     doelType   doelAttribuut     the share written on each recipient
                ratoAttribuut     the proportional weight (read per recipient)
                maximumAttribuut  the cap (read per recipient)
                groepeerAttribuut the ordering key (read per recipient)

   An undeclared TYPE is another rule's business (WF-047 on the bound form,
   WF-048 on the bindingless one — both RULES measured loud, though the
   executors are not: an undeclared `bronType` leaves `ref` silent with every
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

     bronAttribuut     11/11  both fault (empty source amount / unknown attribute)
     doelAttribuut     11/11  both fault (unknown attribute)
     restantAttribuut   9/9   both fault (unknown attribute)
     ratoAttribuut      7/7   BOTH SILENT: the shares stay underived
                              (anchor jutterswet-bergingstoezicht: leeg where
                              60 and 40 are pinned)
     maximumAttribuut   4/4   ref SILENT (jutterswet-berging: 600 for a pinned
                              300, the cap simply gone), clingo refuses to
                              translate — ref != clingo on a document the
                              verifier called well-formed
     groepeerAttribuut  3/3   BOTH SILENT AND AGREED (jutterswet-rangaflopend:
                              300 apiece where 1200/0/0/0 is pinned)

   The three silent rows are where the differential square is blind: backend
   agreement proves nothing there, and only the hand-pinned anchor
   expectations stand between the corpus and the defect.

   Each of the six sub-arms carries its own negatief witness
   (`corpus/negatief/wf034-verdeling-onbekend-<field>`), and each is ARM-exact:
   deleting one [resolve] line frees that one document and no other over the
   four zones (486 documents compared; the `doelAttribuut` line additionally
   costs the parity casus its six incidental lines, without moving its
   verdict). Deleting the whole clause frees those six and nothing else —
   7 reports, 6 verdicts. *)
let wf034_verdeling_naamresolutie ctx =
  List.concat
    (List.mapi
       (fun i (r : resolved_verdeling) ->
         let at = Printf.sprintf "/verdelingen/%d" i in
         let resolve ~side ~scope field name =
           match (scope, name) with
           | Some tn, Some a when norm a <> "" -> (
               match objecttype ctx tn with
               | None -> [] (* the type itself is undeclared: WF-047/WF-048 *)
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
         resolve ~side:"source" ~scope:r.bron_type "bronAttribuut" r.bron_attribuut
         @ resolve ~side:"source" ~scope:r.bron_type "restantAttribuut" r.restant_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "doelAttribuut" r.doel_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "ratoAttribuut" r.rato_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "maximumAttribuut" r.maximum_attribuut
         @ resolve ~side:"target" ~scope:r.doel_type "groepeerAttribuut" r.groepeer_attribuut)
       ctx.doc.verdelingen)

(* WF-034 on an object creation's SUBJECT (maintainer decision 2026-09-04,
   fifth wave). `subject` names the object type the creation RUNS OVER (ch.
   5.5): one new instance per subject instance the condition admits, and the
   member values evaluate on it. It is a reference like any other, spelled in
   one field, and no rule crossed it against the object-type table. WF-027
   crosses the CREATED type (`nieuwType`) and nothing else; the subject was
   reached only by the rules that read THROUGH it — WF-053 when a member value
   navigates, WF-047 when an explicit `verbindendeRol` names the back-link —
   and those are incidental cover, not coverage: they stand down the moment a
   creation has neither.

   Measured over the 11 golden object creations, `subject` pointed at an
   undeclared name one at a time: 10 of 11 were already refused — all ten by
   WF-053 (the navigation in a member value loses its scope), nine of them by
   WF-047 as well — and exactly ONE stayed verifier-CLEAN: the single creation
   with no `toekenningen` at all (`anchor/jutterswet-penninguitgifte`,
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
    (List.mapi
       (fun i (r : resolved_object_creatie) ->
         let at = Printf.sprintf "/objectCreaties/%d/subject" i in
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

(* The type an expression carries, iff it is a value (a Lambda is not). The
   sole nominal source for static typing; WF-027 uses it to admit a
   boolean-TYPED characteristic value, and the expression walk (WF-034..) below
   reuses it. *)
let typ_of_expr = function
  | Literal { typ; _ } | Tekst_reeks { typ; _ } | Binair { typ; _ }
  | Functie { typ; _ } | Afgerond { typ; _ } | Begrensd { typ; _ }
  | Attribuut_ref { typ; _ } | Dimensie_cel { typ; _ } | Parameter_ref { typ; _ }
  | Variabele_ref { typ; _ } | Constante { typ; _ } | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ } | Construct { typ; _ } | Match { typ; _ }
  | Binder_ref { typ; _ } | Struct_literal { typ; _ } | Field_access { typ; _ }
  | List_literal { typ; _ } | If_then_else { typ; _ } | Let { typ; _ }
  | Invoke { typ; _ } | Fold { typ; _ } | Map_list { typ; _ }
  | Filter_list { typ; _ } | List_length { typ; _ } -> typ
  (* a Lambda is never a value: it carries no type *)
  | Lambda _ -> None

(* An AttribuutRef's serialized [typ] can lie about the attribute's actual
   datatype (a numeric attribute forced to typ=Booleaan passes typ_of_expr but
   diverges at runtime). For a value check the honest static type is the
   RESOLVED attribute datatype — the cross-validation WF-023 performs in
   rekenposities. Resolve it from the reference's declared endpoint type (the
   type the value lives on: the subject itself or the navigation endpoint) and
   member. A reference we cannot resolve (undeclared type/member, a
   characteristic read that is not in [attributen], or a non-reference node)
   yields None, and the caller falls back to the node's own typ. *)
let resolved_attr_datatype ctx = function
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ } -> (
      match objecttype ctx otn with
      | Some ot -> ( match attr_of ot a with Some (Some dt) -> Some dt | _ -> None)
      | None -> None)
  | _ -> None

let wf027 ctx =
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/objectCreaties/%d" i in
      match r.nieuw_type with
      | None -> [ v "WF-027" at "object creation without a created type" ]
      | Some tn -> (
          match objecttype ctx tn with
          | None -> [ v "WF-027" at ("created type '" ^ tn ^ "' is not declared") ]
          | Some ot ->
              List.concat_map
                (fun (j, t) ->
                  let tat = Printf.sprintf "%s/toekenningen/%d" at j in
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
                           any evidently non-boolean node) stays refused. The v0
                           unknown sentinel cannot be refuted statically, so it
                           defers to the runtime fail-fast at the write, exactly
                           as WF-023 skips Datatype_onbekend. A value that
                           carries no type at all (a Lambda — never a value)
                           stays refused. *)
                        (match t.toe_waarde with
                         | None ->
                             [ v "WF-027" tat "characteristic initialisation without a value" ]
                         | Some w ->
                             (* the honest static type: an AttribuutRef's own typ
                                may lie, so prefer the resolved attribute datatype
                                (the WF-023 cross-validation); fall back to the
                                node typ for every other expression. The v0
                                unknown sentinel stays the skip. *)
                             let effective =
                               match resolved_attr_datatype ctx w with
                               | Some dt -> Some dt
                               | None -> typ_of_expr w
                             in
                             (match effective with
                              | Some Booleaan | Some (Datatype_onbekend _) -> []
                              | _ ->
                                  [ v "WF-027" tat
                                      "characteristic initialisation's value must be a \
                                       boolean-typed expression" ]))
                      else if not (has_attr ot m) then
                        [ v "WF-027" tat ("'" ^ m ^ "' is not a declared attribute of '" ^ tn ^ "'") ]
                      else [])
                (List.mapi (fun j t -> (j, t)) r.toekenningen)))
    (List.mapi (fun i r -> (i, r)) ctx.doc.object_creaties)

let wf029 ctx =
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/consistenties/%d" i in
      (match r.cons_subject with
       | None -> [ v "WF-029" at "consistency without a subject type" ]
       | Some s when objecttype ctx s = None ->
           [ v "WF-029" at ("subject type '" ^ s ^ "' is not declared") ]
       | Some _ -> [])
      @ match r.assertie with
        | None -> [ v "WF-029" at "consistency without an assertion" ]
        | Some _ -> [])
    (List.mapi (fun i r -> (i, r)) ctx.doc.consistenties)

let datatype_kind = function
  | Numeriek _ -> "number" | Percentage _ -> "percentage" | Tekst -> "text"
  | Booleaan -> "boolean" | Datum_in_dagen -> "date-day" | Datum_tijd -> "date-ms"
  | Enumeratie _ -> "enumeration" | Sum_type _ -> "sumtype" | List_type _ -> "list"
  | Struct_type _ -> "struct" | Datatype_onbekend _ -> "unknown"

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
   declaring domain: an Enumeratie's or SumType's domain, looked up THROUGH a
   ListType's element — a list of a keuzetype names that keuzetype, and a
   drifted element domain on a list-typed reference is the wrong-answer flank
   the card measured (clingo folded 0 where ref faulted). None for every
   other datatype and for an Enumeratie carrying no domain. The variant
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

(* WF-089 (domain identity, IR-SPEC ch. 2.4): a use site's serialized [typ]
   and the datatype of the source it TAKES its value from must name the same
   keuzetype (variant and domain, ci) whenever either of them names one — the
   domain extension of the soort cross-check WF-023/WF-027/WF-078 perform.
   WF-088 and WF-061 bind a use site to the domain it NAMES; this is the read
   half of the other binding. Two use sites call it: a reference expression,
   whose source is the DECLARATION it reads (the [source] default), and an
   element-passing fold, whose source is each OPERAND it hands through — the
   operand being anchored in turn, so the fold chains to a declaration. The
   write half is WF-023's domain clause ([wf023_assignment]); all of them
   together are what make ref's "match has no arm" fault unreachable on a
   verifier-clean document — the read half alone was measured insufficient
   (fix round 1), the reference positions alone were too (fix round 2).
   Either side unknown (the v0 sentinel), an unresolved source, or a use site
   carrying no type defers, exactly as WF-023 and WF-078 do. [what] names the
   use site in the message, [source] what it reads. *)
let wf089_domain_identity ?(source = "the declaration it reads from") at ~what ~decl ~typ =
  match (decl, typ) with
  | None, _ | _, None | Some (Datatype_onbekend _), _ | _, Some (Datatype_onbekend _) -> []
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
   family term covers Enumeratie and SumType only, maintainer decision), and
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
   Either side unknown (the v0 sentinel), an unresolved source, or a use site
   carrying no type defers, exactly as WF-089 and WF-023 do. *)
let wf090_struct_identity ?(source = "the declaration it reads from") at ~what ~decl ~typ =
  match (decl, typ) with
  | None, _ | _, None | Some (Datatype_onbekend _), _ | _, Some (Datatype_onbekend _) -> []
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
    | Some f, Some c -> Printf.sprintf " (factor %g to '%s')" f c
    | Some f, None -> Printf.sprintf " (factor %g)" f
    | None, Some c -> Printf.sprintf " (canonical '%s')" c
    | None, None -> "")
  ^ match u.noemer with Some n -> " per " ^ eenheid_render n | None -> ""

let unit_render = function None -> "no unit" | Some u -> eenheid_render u

let teken_render = function
  | None -> "no sign restriction"
  | Some Onbepaald -> "ONBEPAALD"
  | Some Negatief -> "NEGATIEF"
  | Some Niet_negatief -> "NIET_NEGATIEF"
  | Some Positief -> "POSITIEF"

(* The numeric facets a type turns on. A Percentage carries no sign
   restriction on the wire, so its sign facet is vacuously "none". *)
type num_facets = {
  nf_geheel : bool;
  nf_decimalen : int;
  nf_teken : teken option;
  nf_eenheid : eenheid option;
}

let num_facets_of = function
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      Some { nf_geheel = geheel; nf_decimalen = decimalen; nf_teken = teken; nf_eenheid = eenheid }
  | Percentage { geheel; decimalen; eenheid } ->
      Some { nf_geheel = geheel; nf_decimalen = decimalen; nf_teken = None; nf_eenheid = eenheid }
  | _ -> None

(* the numeric facets are read THROUGH matching ListType layers, exactly as
   the domain facets are: a list of kilograms and a list of tonnes differ in
   their unit, not in some nameless residual *)
let rec peel_lists a b =
  match (a, b) with
  | List_type { element = x }, List_type { element = y } -> peel_lists x y
  | _ -> (a, b)

(* The residual facet's comparison form: the datatype with every DOMAIN name
   folded to the WF-001 form. Two goldens pin exactly that spelling freedom
   between a use site and its declaration. The three UNIT names are left
   alone — the same split [eenheid_identical] makes, so the backstop refuses
   exactly the spellings the unit facet refuses and lets through exactly the
   ones it lets through. *)
let rec dt_ci = function
  | Enumeratie { domein; waarden } ->
      Enumeratie
        { domein = Option.map norm domein; waarden = List.map (Option.map norm) waarden }
  | Sum_type { domein; constructoren } ->
      Sum_type
        { domein = norm domein;
          constructoren = List.map (fun (n, p) -> (norm n, Option.map dt_ci p)) constructoren }
  | Struct_type { domein; velden } ->
      Struct_type
        { domein = norm domein; velden = List.map (fun (n, d) -> (norm n, dt_ci d)) velden }
  | List_type { element } -> List_type { element = dt_ci element }
  | other -> other

(* the whole structure spelled out — what the residual facet reports, where
   [datatype_detail]'s summary would render a difference as "X vs X" *)
let rec datatype_full = function
  | Numeriek { geheel; decimalen; teken; eenheid } ->
      Printf.sprintf "number(%s, %d decimals, %s, %s)"
        (if geheel then "whole" else "fractional")
        decimalen (teken_render teken) (unit_render eenheid)
  | Percentage { geheel; decimalen; eenheid } ->
      Printf.sprintf "percentage(%s, %d decimals, %s)"
        (if geheel then "whole" else "fractional")
        decimalen (unit_render eenheid)
  | Enumeratie { domein; waarden } ->
      Printf.sprintf "enumeration '%s' [%s]"
        (Option.value domein ~default:"?")
        (String.concat ", " (List.map (Option.value ~default:"?") waarden))
  | Sum_type { domein; constructoren } ->
      Printf.sprintf "sumtype '%s' [%s]" domein
        (String.concat ", "
           (List.map
              (fun (n, p) -> match p with None -> n | Some d -> n ^ " of " ^ datatype_full d)
              constructoren))
  | Struct_type { domein; velden } ->
      Printf.sprintf "struct '%s' {%s}" domein
        (String.concat ", " (List.map (fun (n, d) -> n ^ ": " ^ datatype_full d) velden))
  | List_type { element } -> "list of " ^ datatype_full element
  | other -> datatype_kind other

(* WF-091 (reference-type identity, IR-SPEC ch. 4.1) and the numbers it
   defers to. ONE comparison of a use site's serialized [typ] against the
   declaration it takes its value from, facet by facet in one fixed order —
   soort, domain, unit, decimals, wholeness, sign, residual structure — of
   which the FIRST differing facet is reported, once: one divergence is one
   message per comparison, never two facets for one comparison. (A caller
   that compares one path repeatedly — the aggregation walk, once per
   operand — can still report more than one.)

   The rule NUMBER follows the facet and the arm, not this function. A facet
   that already carries a number on this arm keeps that number and its
   wording verbatim: the domain facet is WF-089 (keuzetype family) and
   WF-090 (struct domains) on every arm, and the soort facet is WF-078 on a
   `BinderRef` (the caller passes [soort]). Every facet without one is
   WF-091's, its name in the message — which is what lets a further facet
   land without a further rule number.

   The soort facet deliberately stops short of the domain-carrying kinds:
   `domeinen` is ONE namespace, so enumeration-versus-sum-type and
   domain-versus-no-domain are the domain facet's own clauses (WF-089's
   variant and one-sided arms, WF-090's), stated and numbered there.

   Deferrals are the family's, unchanged: either side absent or the v0
   unknown sentinel, or a declaration that does not resolve. The WF-034
   widening (2026-09-04, two waves) closes the shapes a document could
   CHOOSE one with, not the self-serving of deferrals as such: a reference
   naming no objecttype and one naming no attribuut, each in a value
   position and in a fold source (four arms — the aggregation branch
   replaces the walk, so each half needs one on both paths); a
   ParameterRef naming no parameter; and both spellings of the unknown type
   on a rule target, the sentinel and an absent type. Measured on the
   goldens, one field at a time: 409 of 751 attribuut positions, 35 of 37
   parameter positions and 295 of 295 target types stayed verifier-clean
   before, none after.

   [skip] names facets this comparison does NOT make. It exists for the
   DERIVED arm ([composite_expectation] below), where the expectation is a
   formula over the operands rather than a copy of one declaration, and a
   formula may be silent on a facet while speaking on the rest. Every entry
   is reason-carrying at its call site; a caller that skips a NAMED facet
   also skips [residual], since the backstop would otherwise re-report the
   very facet the caller declined to claim. No reference arm passes it. *)
let use_site_identity ?(source = "the declaration it reads from") ?soort ?(skip = []) at ~what
    ~decl ~typ =
  match (decl, typ) with
  | None, _ | _, None | Some (Datatype_onbekend _), _ | _, Some (Datatype_onbekend _) -> []
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
        else
          match soort with
          | Some (rule, render) -> [ v rule at (render (datatype_kind t) (datatype_kind d)) ]
          | None -> mismatch "soort" (datatype_kind t) (datatype_kind d)
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
      let sign_facet () =
        numeric (fun f -> f.nf_teken) "sign" (fun f -> teken_render f.nf_teken)
      in
      let residual_facet () =
        if dt_ci t <> dt_ci d then
          mismatch "residual structure" (datatype_full t) (datatype_full d)
        else []
      in
      let rec first = function
        | [] -> []
        | f :: rest -> ( match f () with [] -> first rest | vs -> vs)
      in
      first
        (List.filter_map
           (fun (n, f) -> if List.mem n skip then None else Some f)
           [ ("soort", soort_facet); ("domain", domain_facet); ("unit", unit_facet);
             ("decimals", decimals_facet); ("wholeness", whole_facet); ("sign", sign_facet);
             ("residual", residual_facet) ])

(* the soort facet's WF-078 wording on a `BinderRef` arm — the number and the
   sentence WF-078 has carried since the sum-type card, kept verbatim so the
   engine absorbs the rule rather than renaming it *)
let binder_soort naam =
  ( "WF-078",
    fun kt kd -> Printf.sprintf "binder reference '%s' is typed %s, but its binder is %s" naam kt kd
  )

(* --- the DERIVED expectation on composite positions (WF-091) ------------ *)

(* A reference type is a COPY of one declaration; a composite type is a
   DERIVATION over its operands (RUNTIME-SEMANTICS ch. 2.10, clause 2). The
   facet engine above needs a [decl] to compare against, and at a composite
   there is none — which is why WF-091's honesty note 4 recorded the numeric
   facets of a derived type as crossed by nothing. What follows puts the
   DERIVATION where the declaration sits: the expectation is computed from
   the operands' own serialized types by the ch. 2.3 precision table and the
   ch. 2.6 unit rules, and the node's annotation is crossed against it by the
   SAME engine, in the same facet order, under the same number-follows-the-
   facet rule — so the DOMAIN facet reports under WF-089/WF-090 at a composite
   exactly as it does at a reference (`wf089-composiet-domein`).

   How far the chain reaches. An operand's own type may be anchored in turn —
   a reference arm against its declaration, a composite against its own
   operands — and where every operand underneath is anchored that way, the
   composite is bound to the declarations beneath it. That is the good case,
   not a theorem: the chain reaches only through operands something actually
   crosses, and it BREAKS at a fourth-class operand. A GEDEELD_DOOR over two
   unitless operands may be annotated `kg`, nothing crosses it, and a rounding
   above it then derives `kg` all the way into a `kg` target, verifier-clean
   (measured 2026-09-04: 26 such operand positions over 19 of the 174
   golden+rood documents). The deferral proper is not self-servable — an
   operand carrying the unknown sentinel or no type at all is already
   WF-034-ill-formed at every value node — but a fabricated fourth-class
   annotation is the stronger escape and stays OPEN: it does not switch the
   cross off, it feeds it a premise.

   The SIGN is claimed at no arm below, and therefore neither is [residual]:
   the backstop would re-report exactly the facet every formula here declines.
   A derivation mostly under-claims a sign restriction while a document
   narrows it truthfully (a clamp with a minimum of 0 IS non-negative whatever
   its base says), and it over-claims at the AANTAL arm, where two anchor
   goldens widen NIET_NEGATIEF to ONBEPAALD. Measured 2026-09-04: crossing it
   costs 5 golden+rood documents on the additive arm, 7 over all arms. *)

let dt_decimals = function Numeriek { decimalen; _ } | Percentage { decimalen; _ } -> decimalen | _ -> 0
let dt_whole = function Numeriek { geheel; _ } | Percentage { geheel; _ } -> geheel | _ -> false

(* The ch. 2.6 MAAL join, in the two shapes the IR fixes: a unitless operand
   hands the other unit through (the dimensionless 1 absorbs), and a rate
   times its convertible denominator unit CANCELS to the numerator ("€/jr
   maal jr = €"). The third shape — two plain units — is ch. 2.5's opaque
   PRODUCT, whose algebra the spec fixes (sorted multiset of atomic factors,
   no canonical, no factor) but whose wire SPELLING it does not: an identity
   cross there would pin an `afkorting` and a `stelsel` ch. 2.5 does not
   require. The fourth — an unspellable rate factor — is the shape the
   frontend refuses outright (RUNTIME-SEMANTICS ch. 2.10). This function
   makes no claim at either, and the corpus carries zero of both (measured
   2026-09-04: no MAAL node in golden, rood, negatief or the parity zone has
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

(* The RegelSpraak frontend's `setLezing` (`ExpressieBouwer`), mirrored — the
   scheidslijn the §4.20 denominator strip below turns on, taken from the
   emitter rather than restated beside it. An aggregation operand list is a
   NON-TEMPORAL SET reading when it is ONE attribute reference carrying `set`
   over an attribute its objecttype declares NO granularity for. Both halves
   are the frontend's and both are load-bearing: a set read over a TEMPORAL
   attribute is the §7.1 per-element timeline fold (each element's tijdreeks
   summed over time FIRST), which DOES integrate, and the non-set read is the
   §4.20 rate read proper — so neither is this shape.

   [None] wherever a SET read's granularity is UNREADABLE — an objecttype
   this document does not declare, or a half of the resolving pair left
   empty. The two arms disagree exactly there, so this one defers rather
   than picking one: the family's own treatment of an unresolvable
   declaration, and no silent choice of a strip on an unread premise. The
   frontend cannot reach the case (it holds a RESOLVED objecttype, whose
   null branch means the reference did not resolve at all) and no
   verifier-admissible document can either — an empty half is WF-034-
   ill-formed in a fold source since the widening. The corpus carries none
   of the three shapes: every TOTAAL operand in golden, rood, negatief and
   the parity zone resolves (measured 2026-09-04). *)
let set_lezing ctx (args : expr list) : bool option =
  match args with
  | [ Attribuut_ref { set = true; objecttype = Some otn; attribuut = Some a; _ } ] -> (
      match objecttype ctx otn with Some ot -> Some (not (attr_temporal ot a)) | None -> None)
  | [ Attribuut_ref { set = true; _ } ] -> None
  | _ -> Some false

(* The expectation for one composite position, or [None] where v0 derives
   nothing there. The tuple is (what the message names, what the message
   calls the source, the derived datatype, the facets NOT claimed).

   What is NOT here, and why — the fourth class of RUNTIME-SEMANTICS
   ch. 2.10, kept identical to the exception set of gate 13
   (`scripts/check-reftype-derivation.sh`, which keys on (node, op) and
   (node, kind) for exactly this reason): a division, a power, a
   percentage-of, a root, an absolute value and the seven date/time
   extractors, where v0 states no result-unit rule; and TIJDSDUUR /
   ABSOLUTE_TIJDSDUUR, where the annotation IS the source (the operator's
   own parameter, WF-038). Not derived means not crossed — and named, never
   silently skipped. Two node kinds are left out for the opposite reason:
   an IfThenElse and a Let already cross their whole type against their
   branches and their body under WF-069 and WF-076. *)
let composite_expectation ctx (e : expr) : (string * string * datatype * string list) option =
  let known x = match x with Some (Datatype_onbekend _) -> None | x -> x in
  let operand_t x = known (Option.bind x typ_of_expr) in
  match e with
  | Binair { op = (Plus | Min | Verminderd_met) as op; links; rechts; _ } -> (
      let what =
        "the "
        ^ (match op with Plus -> "PLUS" | Min -> "MIN" | _ -> "VERMINDERD_MET")
        ^ " composite"
      in
      match (operand_t links, operand_t rechts) with
      | Some l, Some r ->
          if op <> Verminderd_met && is_date l && is_number_like r && unit_of r <> None then
            (* date +/- duration (ch. 2.2): the result IS the date operand's
               type, unchanged — the duration operand carries the calendar
               unit and contributes nothing to the result type *)
            Some (what, "the type derived from its date operand", l, [ "sign"; "residual" ])
          else if
            is_number_like l && is_number_like r && units_compatible (unit_of l) (unit_of r)
          then
            (* ch. 2.3: maxDec, whole iff both operands are; ch. 2.6: the
               result carries the LEFT unit. The SIGN is not claimed here or
               at any other arm — see the header note. *)
            Some
              ( what, "the type derived from its operands",
                Numeriek
                  { geheel = dt_whole l && dt_whole r;
                    decimalen = max (dt_decimals l) (dt_decimals r);
                    teken = Some Onbepaald;
                    eenheid = unit_of l },
                [ "sign"; "residual" ] )
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
                ( "the MAAL composite", "the type derived from its operands",
                  Numeriek { geheel = false; decimalen = 0; teken = Some Onbepaald; eenheid = u },
                  [ "decimals"; "wholeness"; "sign"; "residual" ] ))
      | _ -> None)
  | Afgerond { basis = Some b; decimalen; _ } -> (
      (* ch. 2.4 clause 2: a rounding changes the FACING precision to its own
         decimal count (whole when 0) and touches nothing else — ch. 2.10:
         it carries its basis's unit. *)
      match known (typ_of_expr b) with
      | Some (Numeriek { teken; eenheid; _ }) ->
          Some
            ( "the rounding", "the type derived from its base and its decimal count",
              Numeriek { geheel = decimalen = 0; decimalen; teken; eenheid },
              [ "sign"; "residual" ] )
      | Some (Percentage { eenheid; _ }) ->
          (* the percentage arm carries NO corpus witness (measured 2026-09-04:
             zero roundings over a percentage base in any zone); it keeps the
             base's unit, which is what ch. 2.10, the ASP translator and the
             Drools backend all say — the RegelSpraak frontend drops it there,
             a divergence recorded rather than followed *)
          Some
            ( "the rounding", "the type derived from its base and its decimal count",
              Percentage { geheel = decimalen = 0; decimalen; eenheid },
              [ "sign"; "residual" ] )
      | Some other ->
          Some ("the rounding", "the type derived from its base", other, [ "sign"; "residual" ])
      | None -> None)
  | Begrensd { basis = Some b; _ } -> (
      (* ch. 2.7: a clamp returns one of three values of the base's own type.
         Its SIGN is the sharpest case for the exclusion the header states: a
         clamp with a minimum of 0 IS non-negative whatever its base says. *)
      match known (typ_of_expr b) with
      | Some d -> Some ("the clamp", "the type derived from its base", d, [ "sign"; "residual" ])
      | None -> None)
  | Functie { kind = Aantal; _ } ->
      (* ch. 2.3: a count is a whole, non-negative, unitless number. Two
         anchor goldens write ONBEPAALD on a count — the header's exclusion in
         its widening direction. *)
      Some
        ( "the AANTAL aggregation", "the type a count derives",
          Numeriek { geheel = true; decimalen = 0; teken = Some Niet_negatief; eenheid = None },
          [ "sign"; "residual" ] )
  | Functie { kind = (Som | Eerste | Laatste | Totaal | Minimum | Maximum) as k;
              argumenten = a0 :: rest; _ } -> (
      let name =
        match k with
        | Som -> "SOM" | Eerste -> "EERSTE" | Laatste -> "LAATSTE"
        | Totaal -> "TOTAAL" | Minimum -> "MINIMUM" | _ -> "MAXIMUM"
      in
      let what = "the " ^ name ^ " aggregation" in
      match known (typ_of_expr a0) with
      | None -> None
      | Some t0 -> (
          match k with
          | Som ->
              (* §4.6 Eenheid1: a sum is its first operand's type, unchanged *)
              Some (what, "the type derived from its first operand", t0, [ "sign"; "residual" ])
          | Eerste | Laatste ->
              (* an element-passing fold returns a contributor unchanged. The
                 DOMAIN facet is skipped here and only here because it is
                 already crossed at this position, once per operand and under
                 its own number, by WF-089/WF-090's aggregation arm — this
                 cross adds the facets that arm deliberately left out. *)
              Some
                ( what, "the type derived from its first operand", t0,
                  [ "domain"; "sign"; "residual" ] )
          | Totaal -> (
              (* §4.20: the temporal total INTEGRATES a rate over time, so the
                 result drops the /tijdseenheid and keeps the numerator — but
                 only where the operand integrates. The scheidslijn is the
                 emitter's own, [set_lezing] above: the NON-TEMPORAL SET
                 reading ("het totaal van <x> van alle <elementen>" over a
                 plain attribute) is an ALIAS of the sum, an instance fold
                 over nothing, and carries the element type verbatim exactly
                 as the SOM arm does; every other reading — the non-set rate
                 read and the set read over a temporal attribute — keeps the
                 strip. Stripping unconditionally invented a wire type no
                 executor can produce (`jutterswet-tempototaal`, whose set
                 fold adds EUR/mnd values into a EUR/mnd target). The two
                 executor-side derivations do not strip at ALL (measured: both
                 answer the first operand's unit whole) — a divergence recorded
                 at the card, harmless on this corpus, where every TOTAAL's own
                 annotation follows the emitter. *)
              match set_lezing ctx (a0 :: rest) with
              | None -> None
              | Some true ->
                  Some (what, "the type derived from its first operand", t0, [ "sign"; "residual" ])
              | Some false ->
                  let d =
                    match t0 with
                    | Numeriek ({ eenheid = Some ({ noemer = Some _; _ } as u); _ } as n) ->
                        Numeriek { n with eenheid = Some { u with noemer = None } }
                    | other -> other
                  in
                  Some (what, "the type derived from its first operand", d, [ "sign"; "residual" ]))
          | Minimum | Maximum ->
              if rest = [] then
                Some
                  ( what, "the type derived from its operand", t0,
                    [ "domain"; "sign"; "residual" ] )
              else
                let ts = List.map typ_of_expr (a0 :: rest) in
                if List.exists (fun x -> known x = None) ts then None
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
                           [ "domain"; "sign"; "residual" ] )
                   | _ ->
                       Some
                         ( what, "the type derived from its first operand", t0,
                           [ "domain"; "sign"; "residual" ] ))
          | _ -> None))
  | _ -> None

let composite_identity ctx at (e : expr) =
  match composite_expectation ctx e with
  | None -> []
  | Some (what, source, derived, skip) ->
      use_site_identity ~source ~skip (at ^ "/type") ~what ~decl:(Some derived)
        ~typ:(typ_of_expr e)

let wf030 ctx =
  let key_checks at otn keys =
    match objecttype ctx otn with
    | None -> [ v "WF-030" at ("source type '" ^ otn ^ "' is not declared") ]
    | Some ot ->
        (if keys = [] then [ v "WF-030" at "uniqueness source without keys" ] else [])
        @ List.concat_map
            (fun k ->
              match k with
              | None -> [ v "WF-030" at "null key entry" ]
              | Some k when not (has_attr ot k) ->
                  [ v "WF-030" at ("key '" ^ k ^ "' is not a declared attribute of '" ^ otn ^ "'") ]
              | Some _ -> [])
            keys
  in
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/uniciteiten/%d" i in
      match r.uni_subject with
      | None -> [ v "WF-030" at "uniqueness without a subject type" ]
      | Some s -> key_checks at s r.sleutels)
    (List.mapi (fun i r -> (i, r)) ctx.doc.uniciteiten)
  @ List.concat_map
      (fun (i, r) ->
        let at = Printf.sprintf "/uniciteitUnies/%d" i in
        let count_v =
          (* the v0 resolver only ever produces the union shape with a
             first source plus at least one vereniging — fewer sources
             is not a mirror form, and an empty list would split the
             executors (ref no-op vs translation fault) *)
          if List.length r.bronnen < 2 then
            [ v "WF-030" at "a uniqueness union carries at least two sources" ]
          else []
        in
        let per_bron =
          List.concat_map
            (fun b ->
              match b.uub_type with
              | None -> [ v "WF-030" at "union source without a type" ]
              | Some t -> key_checks at t b.uub_sleutels)
            r.bronnen
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
        count_v @ per_bron @ arity_v @ kind_v @ unit_v)
      (List.mapi (fun i r -> (i, r)) ctx.doc.uniciteit_unies)

(* One structural pass over a day-kind voorwaarde (IR-SPEC par. 5, the
   stratum-0 framing; ch. 3.7). It collects, in one walk:
   - the WF-031 purity violations: a day-kind condition is evaluated against
     a bare date before the rule graph, with no fact base, so it may
     reference only dates, date-components, eerste-paasdag, literals,
     parameters and other day-kind names — never derived object state. An
     attribute read (AttribuutRef) or a characteristic check (Kenmerk) is
     refused (loud, where v0 neither checks nor documents this);
   - the day-kind names it references (a Predicaat DAGSOORT), for the
     WF-085 reference graph. *)
let dagsoort_scan at (c0 : conditie) : violation list * string list =
  let viols = ref [] and refs = ref [] in
  let impure kind name =
    viols :=
      v "WF-031" at
        (Printf.sprintf
           "day-kind condition reads %s '%s' — a day-kind voorwaarde references only dates, \
            date-components, eerste-paasdag, literals, parameters and other day kinds, never \
            derived object state (an attribute or characteristic read)"
           kind name)
      :: !viols
  in
  let rec ex (e : expr) =
    let opt = function Some e -> ex e | None -> () in
    match e with
    | Attribuut_ref { attribuut; _ } -> impure "attribute" (Option.value attribuut ~default:"?")
    | Literal _ | Parameter_ref _ | Variabele_ref _ | Constante _ | Expr_onopgelost _ | Binder_ref _
      ->
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
       derived fact-base reads, outside "dates, date-components, eerste-paasdag, literals,
       parameters and other day kinds" — flag them like the attribute/characteristic reads *)
    | Rollidmaatschap { rol; _ } -> impure "role membership" (Option.value rol ~default:"?")
    | Verre_rollidmaatschap { filler; _ } -> impure "role membership" (Option.value filler ~default:"?")
    | Regelversie { regel; _ } -> impure "rule-version state" (Option.value regel ~default:"?")
    | Conditie_onopgelost _ -> ()
  in
  co c0;
  (List.rev !viols, List.rev !refs)

let wf031 ctx =
  let named =
    List.filter_map
      (fun (i, r) ->
        Option.map (fun n -> (n, Printf.sprintf "/resolvedDagsoorten/%d" i)) r.rd_naam)
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)
  in
  (dups_by norm named
   |> List.map (fun (a, _, at) -> v "WF-031" at ("more than one definition for day kind '" ^ a ^ "'")))
  @ List.concat_map
      (fun (i, r) ->
        let at = Printf.sprintf "/resolvedDagsoorten/%d" i in
        (match r.rd_voorwaarde with
         | None -> [ v "WF-031" at "day-kind definition without a condition" ]
         (* the purity clause (IR-SPEC par. 5): no derived-state read *)
         | Some c -> fst (dagsoort_scan (at ^ "/voorwaarde") c))
        (* The validity MUST be ALTIJD: no executor consults a day-kind
           window, so a bounded one would be silently dead. *)
        @ (match r.rd_geldigheid with
           | Some { geld_vanaf = Some _; _ } | Some { geld_tot = Some _; _ } ->
               [ v "WF-031" at
                   "day-kind definition with a bounded validity window; must be ALTIJD (vanaf and tot both null)" ]
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
          | Some n when not (mem_ci (some_names ctx.doc.dagsoorten) n) ->
              [ v "WF-031" at ("definition for undeclared day kind '" ^ n ^ "'") ]
          | _ -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)

(* WF-085 — the day-kind reference graph MUST be acyclic (IR-SPEC par. 5,
   the stratum-0 framing / ch. 3.7). A day-kind condition may reference other
   day kinds (a Predicaat DAGSOORT); mutually recursive definitions have no
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
            (Printf.sprintf "/resolvedDagsoorten/%d" i)
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
      let at = Printf.sprintf "/resolvedStartpunten/%d" i in
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
      let at = Printf.sprintf "/regelGroepen/%d" i in
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

(* [typ_of_expr] is defined above WF-027 (it types a characteristic
   initialisation's value); the expression walk below shares it. *)

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
   an Enumeratie — the nominal source of truth for the enumeration arm of
   WF-063, the keuzetype sibling of [sum_decl] (ch. 2.4). An enumeration
   alternative is nullary by construction, so its "payload" is always None:
   returning the (label, payload) shape lets the Match totality and binder
   checks read one alternative list for both wire variants. *)
let enum_decl ctx domein : (string * datatype option) list option =
  match find_ci ctx.doc.domeinen domein with
  | Some (Some (Enumeratie { waarden; _ })) ->
      Some (List.map (fun w -> (w, None)) (some_names waarden))
  | _ -> None

(* WF-088: the enumeration sibling of WF-061. Every Enumeratie datatype
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
   whose `domein` is null or names no declared enumeration is not ours —
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

(* WF-034's sentinel fence, one level down: the v0 unknown type on a `SumType`
   CONSTRUCTOR PAYLOAD (maintainer decision 2026-09-04, second wave). The fence
   in [walk_expr]'s type prologue matches the node's type at the top level, so
   it sees the sentinel only when the WHOLE type is the sentinel; a datatype is
   a tree, and a sum type carries its payloads as children. The descent is
   [enum_use_site]'s, through the same payloads and for the same measured
   reason — a use site's inline constructor copy is what the consumers read,
   while WF-062 crosses only the DECLARATION.

   The CONTAINER half followed one wave later (maintainer decision
   2026-09-04, third wave): a payload is a datatype, so it may be a
   `ListType` or a `StructType`, and the sentinel then sits one further
   level down — measured schema-valid and verifier-clean on three
   hand-built witnesses over a sum-typed golden, with `ref` replaying to
   its pinned expectations while clingo refuses the same document
   (`TRANSLATION FAULT: unscalable datatype in the ASP translation`).
   [payload_descent] therefore walks the WHOLE payload subtree, not just
   the sum chain, and a container that contains a further sum re-enters
   [payload_sentinel] — which keeps the sum-chain message naming the
   innermost constructor, as before.

   Only the payload subtree: a sentinel at a `ListType` element or a
   `StructType` field that is NOT under a payload is caught by the
   declaration and identity rules (measured — of the goldens' datatype
   positions only the payload chain and the fold-/aggregation argument
   types ever survived sentinel injection), and the fold-/aggregation
   survivors belong to the aggregation branch, not to this descent: 10 of
   them before that branch grew its own sentinel fence (WF-034's seventh
   wave), 0 after. *)
let rec payload_sentinel at (d : datatype) : violation list =
  match d with
  | Sum_type { constructoren; _ } ->
      List.concat_map
        (function
          | n, Some p -> payload_descent at ("the payload of constructor '" ^ n ^ "'") p
          | _, None -> [])
        constructoren
  | _ -> []

(* One payload subtree, [where] naming the position in the reader's terms. *)
and payload_descent at where (d : datatype) : violation list =
  match d with
  | Datatype_onbekend _ -> [ v "WF-034" at ("unknown-type sentinel on " ^ where) ]
  | Sum_type _ -> payload_sentinel at d
  | List_type { element } -> payload_descent at ("the list element inside " ^ where) element
  | Struct_type { velden; _ } ->
      List.concat_map
        (fun (fn, fd) -> payload_descent at ("field '" ^ fn ^ "' inside " ^ where) fd)
        velden
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

let aggregation_kinds = [ Som; Totaal; Aantal; Minimum; Maximum; Eerste; Laatste ]

(* The folds that hand an ELEMENT through unchanged: they select a
   contributor, they never compute a new value, so the node's own type is
   the operand's type. (`Som`/`Totaal`/`Aantal` compute, and their result is
   numeric by construction — no keuzetype travels through them.) With one
   contributor `Arith.agg_extreme` makes no comparison at all, so a
   passthrough node whose type names another domain than its operand hands
   an alien label to whatever reads it — WF-089's aggregation position. *)
let passthrough_folds =
  [ (Eerste, "EERSTE"); (Laatste, "LAATSTE"); (Minimum, "MINIMUM"); (Maximum, "MAXIMUM") ]

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

(* Does [name] match (ci, declared name or plural) a role that [otype]
   itself fills in some declared fact type? The self-denoting fold source
   (WF-083, IR-SPEC ch. 4.3): the written form "het aantal <rol> van de
   <onderwerp>" names the fold subject by its own role name, and the wire
   carries that written role in the argument's navigatieRol —
   presentation, not navigation. *)
let self_denoting_role ctx ~otype name =
  List.exists
    (fun (_, (ft : feittype)) ->
      List.exists
        (fun (r : rol) ->
          norm (Option.value r.rol_objecttype_naam ~default:"") = norm otype
          && ((match r.rol_naam with Some n -> norm n = norm name | None -> false)
             || match r.rol_meervoud with Some m -> norm m = norm name | None -> false))
        ft.rollen)
    ctx.doc.feittypen

(* The ch. 4.1 type-pair derivation, mirrored from the executors: the
   two-role fact types relating [from_type] to [to_type] (either way).

   The pair set is CHAIN-CLOSED (IR-SPEC ch. 2.6): a role declaring filler
   T is filled by every S ⊑ T, so a fact type declared on supertypes
   relates every subtype pair below it. One consequence is stated here so
   it is not discovered later — adding a subtype can turn a previously
   well-formed bindingless navigation into a WF-048 refusal, because a pair
   that was uniquely connected may become doubly connected once the chain
   closes. WF-048's text is unchanged and its refusal stays loud; the
   answer is an explicit role binding.

   [fills] is also the JOIN test of every walk in this file — "the walk
   stands at T; does T fill this hop's near role?" — and that is worth
   saying once, because the two spellings of one hop used to answer it
   differently. A BINDINGLESS hop resolves through [nav_role_by_name],
   which has gone through [fills] since the chain landed; a BOUND hop
   compared the two type NAMES literally, so the same document was
   admitted without a role binding and refused with one — while ch. 2.6's
   own advice is to write the binding whenever a subtype is in play.

   TEN comparison sites were swept for that on 2026-09-12 (card
   subtype-verdeling-keten), and they do not all get the same answer.
   Five are the near-side join above and go through [fills] — WF-053's
   navigation chain and its characteristic check, WF-102's bronPad step,
   WF-028's creation chain and WF-054's uniqueness scope. Four are the
   "this position is used AS type Y" question, and those are ONE-SIDED,
   because a position accepts every instance at or below its declared
   type and none above it: WF-053's endpoint (⊑ the written endpoint,
   which is exactly what ref faults on per instance), WF-028's
   ankerRol/doelRol agreement and WF-034's intermediate bean type (both
   ⊑ the role's declared filler), and WF-052's created-side role, which
   is not a comparison at all any more but the executors' own
   [connecting_role_candidates] — "the first role the created type fills"
   picks the ANCESTOR's role under ⊑ and reads its cardinality, so the
   verdict followed the order the wire listed the roles in. The tenth,
   WF-102's landing on the elementObjecttype, stays TWO-SIDED on the
   WF-047 line; the comment there says why.

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

(* The ch. 4.3 fallback counts candidate ROLES, not fact types — v0's
   verbindendeRol (every role of the far type whose fact type also relates
   the near type). A self-typed two-role fact type offers BOTH its roles and
   so blocks the derivation, exactly as the executors' Eval.connecting_role
   and the ch. 4.1/5.5 connecting_pair do; WF-034 lifts that block to
   well-formedness. Mirrors ref's candidate list one-to-one. *)
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

(* The six aggregation kinds that read a VALUE per contributor — every fold
   kind except AANTAL, which reads the contributors themselves (ch. 1.2e: a
   count is never empty because it never looks inside a member). WF-093's
   discriminator; the wire spelling is the message's. *)
let value_fold_name = function
  | Som -> Some "SOM"
  | Totaal -> Some "TOTAAL"
  | Minimum -> Some "MINIMUM"
  | Maximum -> Some "MAXIMUM"
  | Eerste -> Some "EERSTE"
  | Laatste -> Some "LAATSTE"
  | _ -> None

(* The v0 instance-fold source wart (`CONSTRUCTS.md` row 1): a count/fold
   over a fact-type relation encodes its source as a pseudo-AttribuutRef
   naming a ROLE (usually the plural), typed with the unknown sentinel.
   Legitimate v0; the row-1 split removes the encoding in v1. Such an
   argument is checked here instead of by the generic walk. [element_type]
   is the fold's declared element type — the far end of the narrowed
   type-pair fallback (IR-SPEC ch. 4.3). [subject] is the fold SUBJECT (the
   walking type), which is what both executors anchor an instance fold at;
   [single] says the fold carries exactly ONE argument — the instance-fold
   source shape, the only one whose operand has an anchor at all (a listed
   value fold's operands are ordinary reads, and a foreign objecttype there
   is a legitimate type-pair hop, ref's [navigated]). *)
let aggregation_source_check ctx at ~element_type ~subject ~single ~kind e =
  (* WF-093, the wart's KIND law (maintainer decision 2026-09-06): a
     single-argument instance-fold source carrying the unknown sentinel on its
     OWN type is a COUNT source and nothing else. The rule keys on the
     SENTINEL, which is the one thing all its guises share: v0 writes it over
     a ROLE name (the bare spelling, "het aantal <rol> van de <onderwerp>"),
     over an element ATTRIBUTE (the set spelling), and — the frontend's own
     route — over a CHARACTERISTIC. Under the sentinel the source hands the
     fold linked INSTANCES instead of a value per contributor, which is
     exactly what AANTAL consumes and what every other fold kind cannot read.

     Keyed on the unknown sentinel, which is how the wart is spelled on the
     wire (ch. 4.3's normative discriminator) and how ref keys its own arm:
     [Attribuut_ref { typ = Some (Datatype_onbekend _); ... }] is ref's FIRST
     fold-source arm, matched before the set arm and regardless of [set], so
     it takes BOTH spellings — the bare wart and the set wart — and yields
     `Instances, which its [values ()] refuses with `value aggregation over a
     bare role source`. That fault is now unreachable on a well-formed
     document; it stays as the second line of defence, never a dead branch.

     Single-scoped with the rest of the family: with two or more arguments the
     node is the listed value fold (§5.8.3), whose operands are ordinary reads
     — and in the ONE listed position the four zones carry today the sentinel
     is v0's failed typing of a real attribute read (measured: one EERSTE over
     two listed operands in the parity zone). The wart VERBATIM in that
     position is a second shape again, and no executor carries it — the third
     one reaches for the getter of the ROLE name and throws — so widening this
     rule there would refuse two different shapes under one reason; the
     sentinel-at-an-ordinary-read question is tracked on its own. Kind-scoped
     to the aggregations because the rule is about folding; a builtin carrying
     an elementObjecttype is a different malformation and no document has one
     (measured: 0 of 523). *)
  (match (e, value_fold_name kind) with
   | Attribuut_ref { typ = Some (Datatype_onbekend _); set; _ }, Some kn when single ->
       [ v "WF-093" at
           ("the v0 instance-fold source (the row-1 wart, spelled with the unknown sentinel on \
             the argument's own type) is a COUNT source: a fold source whose own type is the \
             unknown sentinel yields the linked instances, which only AANTAL reads — so it \
             cannot be the source of this " ^ kn
          ^ " aggregation"
          ^ (if set then " (the set spelling reads it no differently: the sentinel decides)"
             else "")) ]
   | _ -> [])
  @
  (* a fold source never navigates: its relation is the fold's bronRol —
     stray navigation bindings in this slot are ill-formed (WF-053). Scoped
     to the single-argument shape with the rest of the family (see the
     navigatiePad arm below): only there is the argument a fold SOURCE. *)
  (match e with
   | Attribuut_ref { navigatie_bindingen = Some _; _ } when single ->
       [ v "WF-053" at
           "navigation bindings on a fold source are ill-formed (the fold's relation is \
            bronRol)" ]
   | _ -> [])
  @
  match e with
  (* WF-083 (a). Single-scoped with the whole "a fold source never
     navigates" family (maintainer decision 2026-09-05, the fix round):
     with two or more arguments the node is the listed value fold
     (§5.8.3) and its operands are ORDINARY READS — ref evaluates each
     one through the same expression evaluator that navigates anywhere
     else, so a navigating operand there is a legitimate read, not an
     anchored source that moved. Measured on a hand-built LAATSTE whose
     second operand navigates: ref 30, the ASP backend 30, Drools 30 —
     three executors agreeing on a form this arm refused unscoped. *)
  | Attribuut_ref { navigatie_pad = _ :: _; _ } when single ->
      [ v "WF-083" at
          "a fold source never navigates: a non-empty navigatiePad on a fold-source argument \
           is ill-formed (the fold's relation is bronRol)" ]
  (* the [navigatie_pad] constraint the arm above used to carry is
     deliberately absent here: with (a) scoped, a nested operand with a
     path must still reach the resolution below instead of falling
     through every arm into silence. *)
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn;
                    navigatie_rol; typ; set; _ } -> (
      (* the row-1 wart, spelled on the wire as the unknown sentinel on the
         argument's own type. Hoisted out of the [Some ot] branch below
         because the self-denotation clause needs it too: only the wart
         carries a written role name at all. *)
      let wart = match typ with Some (Datatype_onbekend _) -> true | _ -> false in
      (* the self-denoting fold source (WF-083): navigatieRol here is the
         written form naming the fold subject by its own role name — an
         away-pointing role would be navigation, which a fold source
         never does; the silent fall-through this arm replaces let it
         pass while both executors fault on it.

         The test anchors on the fold SUBJECT (maintainer decision
         2026-09-05), which is the type both executors anchor it at:
         ref's instance-fold source arm guards on
         [self_denoting_role ~self_type:self.otype], the ASP backend's
         count arm on [~self_type:subject_ot]. Before this the anchor was
         the OPERAND's own objecttype — a field neither executor reads
         here — so one wire field flipped the verdict while no executor
         moved: measured on the tempototaal shape with navigatieRol
         "vondst" added, `objecttype: "Vondst"` PASSED and
         `objecttype: "Jutter"` FAILED, with ref faulting `unsupported
         instance-fold source shape` and the ASP backend `expression kind
         not translated` on BOTH spellings.

         And the written role belongs to the WART alone: it is v0's
         presentation of "het aantal <rol> van de <onderwerp>", the one
         shape whose source names a role instead of reading an attribute.
         On any other fold source a navigatieRol is navigation, which a
         fold source never does — ref's set-source arm and its temporal
         total both demand [navigatie_rol = None] outright, so every such
         form faults there. Without this half the re-anchoring would have
         OPENED a hole it did not have: a set fold carrying a navigatieRol
         that denotes the SUBJECT (measured: "bewarende jutter" on a
         Jutter-subject fold over Vondst) is refused by both executors and
         would have become verifier-clean.

         Single-scoped with the rest of the family: on a listed value
         fold the operands are ordinary reads and a navigatieRol there is
         an ordinary written hop, governed by WF-053 in the sibling
         branch. *)
      (if not single then []
       else
         match navigatie_rol with
         | None -> []
         | Some nr when not wart ->
             [ v "WF-083" at
                 ("navigatieRol '" ^ nr ^ "' on a fold source that is not the v0 instance-fold \
                   source (the unknown-typed row-1 wart): the written role is that wart's \
                   presentation of the fold subject and nothing else — on any other fold \
                   source a navigatieRol claims navigation, which a fold source never does \
                   (its relation is bronRol)") ]
         | Some nr -> (
             match subject with
             | Some st when not (self_denoting_role ctx ~otype:st nr) ->
                 [ v "WF-083" at
                     ("navigatieRol '" ^ nr ^ "' on a fold source does not denote the fold \
                       subject: no role named so (declared name or plural, ci) is filled by \
                       the fold subject '" ^ st ^ "' itself — a fold source never navigates") ]
             | _ -> []))
      (* WF-083, the ANCHOR cross (maintainer decision 2026-09-05): the
         instance-fold source's own `objecttype` MUST name the type the
         executors anchor the read at — the `elementObjecttype` when the
         source is the set read (every read hangs on the element variable
         and every declaration is looked up on the element type: ref's
         set-source arm binds no objecttype at all, and the ASP backend
         states the same anchor as [fold_operand_otype]), the fold SUBJECT
         otherwise (ref's temporal total demands [norm aot = norm
         self.otype] outright, and the wart resolves its written role name
         from the subject).

         The two forms this closes were both verifier-clean and both
         measured: a temporal TOTAAL whose operand names another type's
         temporal attribute (ref `unsupported instance-fold source shape`,
         the ASP backend `the temporal total of … is declared on "Jutter",
         not on the subject type "Vondst"`), and a NON-temporal set TOTAAL
         whose operand names an attribute the element type does not
         declare — where the two executors DISAGREE under a well-formed
         document: ref folds every member's absent value and answers 0,
         the ASP backend faults `unknown attribute … on "Vondst"`.

         Scoped to the single-argument shape: with two or more operands the
         fold is the listed value fold (§5.8.3) whose operands are ordinary
         reads, and there a foreign objecttype is a legitimate type-pair
         hop (ref's [navigated] switch) — not an anchor lie. Measured over
         the four zones: 110 single-argument fold sources and 12 operands
         of listed folds carrying a vestigial elementObjecttype; all 122
         agree with their anchor today, so the scope hides nothing. *)
      @ (let anchor = if set then Some element_type else subject in
         match anchor with
         | Some anc when single && norm otn <> norm anc ->
             [ v "WF-083" at
                 ("fold-source objecttype '" ^ otn ^ "' is not the fold's anchor '" ^ anc
                ^ "' (" ^ (if set then "the set source reads each member of the \
                                       elementObjecttype"
                           else "a non-set fold source is read on the fold subject")
                ^ "): the executors resolve '" ^ a ^ "' against the anchor, never against this \
                   annotation, so the two spellings name different declarations") ]
         | _ -> [])
      @
      match objecttype ctx otn with
      | None -> [ v "WF-034" at ("object type '" ^ otn ^ "' is not declared") ]
      | Some ot ->
          (* IR-SPEC ch. 4.6: "the one scoped exception is the fold-source role
             name (ch. 4.3)", and ch. 4.3 scopes it to the v0 INSTANCE-fold
             source — the row-1 wart, which writes a role name in the
             argument's `attribuut` slot and carries the unknown sentinel on
             its own type BY CONSTRUCTION. That sentinel is how the wart is
             spelled on the wire and how both executors key their
             role-naming branch (ref's contributor match, the ASP backend's
             count arm); the verifier now keys it the same way instead of
             offering the exception to every fold source.

             Measured before this narrowing (maintainer decision 2026-09-04,
             fourth wave), on `corpus/golden/anchor/jutterswet-maatschap`
             whose SOM source is a genuine attribute read carrying a real
             datatype: `attribuut` set to `bestaatniet` (the type-pair
             branch — the one reciprocal self-relation over Jutter offers
             exactly one candidate role) or to `maat` (the role-name branch —
             a declared role of another position entirely) both stayed
             verifier-CLEAN, while `ref` silently summed 0 where 12 and 30 are
             pinned and the ASP backend refused to translate. Outside the
             wart the source is an ordinary attribute reference and resolves
             like one.

             The regression boundary, counted over all four zones: of 89
             named fold sources — NAMED being the argument that spells both
             its `attribuut` and its `objecttype`, the pair this arm resolves
             through; count in the one source spelling an attribute and no
             object type (a wave-1 shape, refused there) and the totals read
             90 and 58 — 32 carry the wart (24 resolving through a
             declared role name, 5 through the type pair, 3 already
             ill-formed under this rule's older arms) and 57 do not — and of
             those 57, 54 already resolve as a declared attribute and one
             names a characteristic (WF-080's arm above, unchanged), leaving
             only the two witnesses this narrowing adds. The counting folds
             that lean on the fallback (`jutterswet-kaveltelling`'s
             synthesized plural; the declared-plural route of
             `jutterswet-aanmeldertelling` and `jutterswet-vondsttelling`)
             are warts and keep both routes. *)
          if has_attr ot a || (wart && is_role_name ctx a) then []
          else if has_kenmerk ot a then
            (* WF-080: an aggregation source is a value position — a
               value-yielding fold reads each element's attribute value
               (a count names a role, kept above via is_role_name). A
               characteristic endpoint carries no value here: ref folds it
               as empty, clingo cannot translate it — the split the sibling
               walk_expr branch already refuses. The same tolerated
               has_attr||has_kenmerk leak, closed here too. *)
            [ v "WF-080" at
                ("'" ^ a ^ "' is a characteristic of '" ^ otn
               ^ "', not an attribute — a characteristic endpoint is ill-formed in an \
                  aggregation source position") ]
          else if not wart then
            [ v "WF-034" at
                ("'" ^ a ^ "' is not a declared attribute of '" ^ otn
               ^ "': outside the v0 instance-fold source (the unknown-typed row-1 wart) a \
                  fold-source argument is an ordinary attribute reference — the ch. 4.3 \
                  role-name exception and its type-pair fallback do not reach here") ]
          else (
            (* the narrowed type-pair fallback (IR-SPEC ch. 4.3): a written
               source name that resolves to no declared role — v0's
               synthesized plural over a role without a declared one — is
               admitted iff the type pair derives a unique CONNECTING ROLE;
               that role is the source, deterministically, no spelling guess.
               Zero, several, or a self-typed two-role pair (both roles are
               candidates — the executors' block, ch. 4.1) keep the rejection
               loud. Counted in candidate roles, not fact types. *)
            match connecting_role_candidates ctx ~from_type:otn ~to_type:element_type with
            | [ _ ] -> []
            | cands ->
                [ v "WF-034" at
                    ("'" ^ a ^ "' is neither a member of '" ^ otn
                   ^ "' nor a declared role, and the type pair '" ^ otn ^ "'/'"
                   ^ element_type ^ "' does not derive a unique connecting role ("
                   ^ string_of_int (List.length cands)
                   ^ " candidates) — the ch. 4.3 fallback cannot stand in") ]))
  | _ -> []

(* --- WF-047 / WF-048: symbolic role bindings (v1 extension) -------------- *)

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

(* WF-048: a bindingless allocation or instance fold is well-formed only
   if the connecting relation is uniquely derivable from the type pair —
   the ch. 4.1 runtime fault, lifted to well-formedness. *)
let wf048_derivable ctx at ~from_type ~to_type ~site =
  match connecting_candidates ctx ~from_type ~to_type with
  | [ _ ] -> []
  | [] ->
      [ v "WF-048" at
          (Printf.sprintf "bindingless %s: no fact type relates '%s' to '%s'" site from_type
             to_type) ]
  | l ->
      [ v "WF-048" at
          (Printf.sprintf
             "bindingless %s: %d fact types relate '%s' to '%s' — not uniquely derivable; \
              name the connecting relation with an explicit role binding"
             site (List.length l) from_type to_type) ]

(* --- WF-053: the navigated read (v1 extension) --------------------------- *)

(* One by-name navigation step from [from_type]: the unique reachable role
   whose declared name or plural matches [name] — the executors' hop
   resolution, lifted to well-formedness. [endpoint], when the step is
   the last (the written endpoint type is known), joins the filter: the
   v0 by-name derivation (verbindendeRolViaNaam) disambiguates
   same-named roles by target type. Its best-effort pluralisation is
   deliberately NOT mirrored — a loud refusal beats heuristic name
   formation (tracked delta, `CONSTRUCTS.md` row 4). Returns
   (fact type, reached role). *)
let nav_roles_by_name ?endpoint ctx ~from_type ~name =
  let matches (r : rol) =
    ((match r.rol_naam with Some n -> norm n = norm name | None -> false)
    || (match r.rol_meervoud with Some m -> norm m = norm name | None -> false))
    &&
    match endpoint with
    | None -> true
    | Some e -> type_compatible ctx (Option.value r.rol_objecttype_naam ~default:"") e
  in
  List.filter_map
    (fun (fn, (ft : feittype)) ->
      (* the near side is a FILLER question: an instance of [from_type]
         fills a role declared at any of its supertypes (ch. 2.6) *)
      let f role = fills ctx ~inst:from_type ~role in
      match ft.rollen with
      | [ a; b ] ->
          if f a && matches b then Some (fn, b)
          else if f b && matches a then Some (fn, a)
          else None
      | [ r ] when ft.wederkerig -> if f r && matches r then Some (fn, r) else None
      | _ -> None)
    ctx.doc.feittypen

let nav_role_by_name ?endpoint ctx ~from_type ~name =
  match nav_roles_by_name ?endpoint ctx ~from_type ~name with
  | [ p ] -> Ok p
  | [] -> Error (Printf.sprintf "no role named '%s' is reachable from '%s'" name from_type)
  | _ ->
      Error
        (Printf.sprintf
           "role name '%s' is ambiguous from '%s'; name the hop with an explicit role binding"
           name from_type)

(* The by-pair step (a directly named object type, no written role): the
   unique fact type relating the pair, and the role [to_type] fills —
   the executors' type-pair derivation (the WF-048 model). *)
let nav_role_by_pair ctx ~from_type ~to_type =
  let hits =
    List.filter_map
      (fun (fn, (ft : feittype)) ->
        let f inst role = fills ctx ~inst ~role in
        match ft.rollen with
        | [ a; b ] ->
            if f from_type a && f to_type b then Some (fn, b)
            else if f from_type b && f to_type a then Some (fn, a)
            else None
        | [ r ] when ft.wederkerig ->
            if f from_type r && norm from_type = norm to_type then Some (fn, r) else None
        | _ -> None)
      ctx.doc.feittypen
  in
  match hits with
  | [ p ] -> Ok p
  | [] -> Error (Printf.sprintf "bindingless navigation: no fact type relates '%s' to '%s'" from_type to_type)
  | l ->
      Error
        (Printf.sprintf
           "bindingless navigation: %d fact types relate '%s' to '%s' — not uniquely \
            derivable; name the hop with an explicit role binding"
           (List.length l) from_type to_type)

(* The hop list of a navigated reference, subject-outward: (fact type,
   reached role) per step. The bound route resolves each binding and
   checks the chain's near sides and the written-name denotation; the
   bindingless route requires each step to be uniquely derivable. Used
   by the WF-053 check, and best-effort (errors dropped) by the
   dependency graph. *)
let nav_hops ctx ~subject ~(bindings : rol_binding list option)
    ~(navigatie_rol : string option) ~(navigatie_pad : string option list)
    ~(endpoint : string option) : ((string * rol) list, string list) result
    =
  let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
  let written_steps =
    match navigatie_pad with
    | [] -> ( match navigatie_rol with Some n -> Some [ n ] | None -> Some [] )
    | pad ->
        if List.exists (fun s -> s = None) pad then None
        else Some (List.filter_map (fun s -> s) pad)
  in
  match written_steps with
  | None -> Error [ "the navigation path carries a null step" ]
  | Some written -> (
      let denotes (r : rol) name =
        (match r.rol_naam with Some n -> norm n = norm name | None -> false)
        || (match r.rol_meervoud with Some m -> norm m = norm name | None -> false)
      in
      (* ONE-SIDED, deliberately: the walk lands on a role whose declared
         filler is [c], so every instance it can hold is ⊑ c, and all of
         them satisfy the endpoint only when c ⊑ e. The other direction —
         a supertype filler against a subtype endpoint — is the unsound
         one, and ref refuses it at run time per instance
         (`the navigation reached %S, not the declared endpoint %S`). A
         two-sided test here would admit a document ref faults on and
         clingo answers, which is ref ≢ clingo on verifier-clean input. *)
      let endpoint_check cur =
        match (endpoint, cur) with
        | Some e, Some c when not (is_sub ctx ~sub:c ~super:e) ->
            [ Printf.sprintf "the navigation reaches '%s', not the endpoint '%s'" c e ]
        | _ -> []
      in
      match bindings with
      | Some hops -> (
          let expected = match written with [] -> 1 | l -> List.length l in
          if List.length hops <> expected then
            Error
              [ Printf.sprintf
                  "the reference carries %d navigation bindings for %d navigation steps"
                  (List.length hops) expected ]
          else
            let step (i, cur, acc, errs) (rb : rol_binding) =
              match resolve_binding ctx rb with
              | Error e -> (i + 1, None, acc, errs @ [ e ])
              | Ok (_, bound, opposite) ->
                  let near =
                    match cur with
                    | Some c when not (fills ctx ~inst:c ~role:opposite) ->
                        [ Printf.sprintf
                            "hop %d over '%s': the near side fills '%s', but the walk stands \
                             at '%s'"
                            i rb.rb_feittype (filler opposite) c ]
                    | _ -> []
                  in
                  let name_check =
                    match List.nth_opt written i with
                    | Some w when not (denotes bound w) ->
                        [ Printf.sprintf
                            "the written step '%s' does not denote the bound role '%s'" w
                            (Option.value bound.rol_naam ~default:"?") ]
                    | _ -> []
                  in
                  ( i + 1,
                    Some (filler bound),
                    acc @ [ (rb.rb_feittype, bound) ],
                    errs @ near @ name_check )
            in
            let _, cur, acc, errs =
              List.fold_left step (0, Some subject, [], []) hops
            in
            match errs @ endpoint_check cur with [] -> Ok acc | errs -> Error errs)
      | None -> (
          match written with
          | [] -> (
              match endpoint with
              | None -> Ok [] (* nothing names the walk; not a navigation *)
              | Some e -> (
                  match nav_role_by_pair ctx ~from_type:subject ~to_type:e with
                  | Ok p -> Ok [ p ]
                  | Error e -> Error [ e ]))
          | [ n ] when navigatie_pad = [] -> (
              (* the single written step (navigatieRol): a far role when
                 one resolves; at ZERO far hits, on an endpoint equal to
                 the subject type, a name that denotes a role the subject
                 itself fills is the zero-hop self-reference (ch. 4.1a) —
                 the walk is empty. Order is normative: far-role
                 resolution first (a reciprocal role stays a real hop);
                 v0's broader same-typed fall-through and its
                 ambiguous-far-match fall-through are deliberately NOT
                 mirrored — both stay loud (tracked delta, safe
                 direction). *)
              match nav_roles_by_name ?endpoint ctx ~from_type:subject ~name:n with
              | [ p ] -> Ok [ p ]
              | [] -> (
                  match endpoint with
                  | Some e
                    when norm e = norm subject && self_denoting_role ctx ~otype:subject n ->
                      Ok []
                  | Some e when norm e = norm subject ->
                      Error
                        [ Printf.sprintf
                            "no role named '%s' is reachable from '%s', and it does not \
                             name a role '%s' itself fills — not the zero-hop \
                             self-reference"
                            n subject subject ]
                  | _ ->
                      Error
                        [ Printf.sprintf "no role named '%s' is reachable from '%s'" n
                            subject ])
              | _ ->
                  Error
                    [ Printf.sprintf
                        "role name '%s' is ambiguous from '%s'; name the hop with an \
                         explicit role binding"
                        n subject ])
          | steps -> (
              let last = List.length steps - 1 in
              let step (i, cur, acc, errs) name =
                match errs with
                | _ :: _ -> (i + 1, cur, acc, errs) (* an unresolved hop stops the walk *)
                | [] -> (
                    let ep = if i = last then endpoint else None in
                    match nav_role_by_name ?endpoint:ep ctx ~from_type:cur ~name with
                    | Ok ((_, r) as p) -> (i + 1, filler r, acc @ [ p ], [])
                    | Error e -> (i + 1, cur, acc, [ e ]))
              in
              let _, cur, acc, errs = List.fold_left step (0, subject, [], []) steps in
              (* a failed hop already reported; the endpoint check would
                 only re-describe the stalled walk *)
              match errs with
              | _ :: _ -> Error errs
              | [] -> (
                  match endpoint_check (Some cur) with
                  | [] -> Ok acc
                  | errs -> Error errs))))

(* WF-053 over one navigated attribute reference: the walk resolves
   (bound or uniquely derivable), and in a scalar slot every hop is
   to-one — a to-many step needs an aggregation, never a silent first
   pick (the v0 resolver builds only to-one chains; NAAR_VEEL is its
   marker). *)
let wf053_attribuut_ref ctx at ~subject ~set ~bindings ~navigatie_rol ~navigatie_pad
    ~endpoint =
  match nav_hops ctx ~subject ~bindings ~navigatie_rol ~navigatie_pad ~endpoint with
  | Error msgs -> List.map (fun m -> v "WF-053" at m) msgs
  | Ok hops ->
      if set then []
      else
        List.concat
          (List.mapi
             (fun i ((_, (r : rol))) ->
               if r.rol_meervoudig then
                 [ v "WF-053" at
                     (Printf.sprintf
                        "hop %d reaches the to-many role '%s' in a scalar slot — a to-many \
                         step needs an aggregation, never a silent first pick"
                        i
                        (Option.value r.rol_naam ~default:"?")) ]
               else [])
             hops)

(* The position context threaded as [~body] through the expression walk
   (IR-SPEC ch. 2.11 / ch. 4.8 / ch. 4.10). It gates two independent things:
   whether an IfThenElse is admissible (everywhere but rule level) and whether
   the surrounding body is CLOSED (a function or lambda body denies attribute,
   dimension-cell, aggregation, temporal and variable reads, and conditions
   beyond Vergelijking/Predicaat). A Let body admits IfThenElse without
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
  | Literal _ | Parameter_ref _ | Variabele_ref _ | Constante _ | Attribuut_ref _
  | Dimensie_cel _ | Expr_onopgelost _ -> false
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
  | Kenmerk_check _ | Rollidmaatschap _ | Verre_rollidmaatschap _ | Regelversie _
  | Conditie_onopgelost _ -> false

(* WF-077 — the condition-operand discipline (IR-SPEC ch. 4.10). Everywhere an
   IfThenElse is admissible, its condition may READ only binders, parameters
   and literals; arithmetic combinators (Binair/Afgerond/Begrensd) over those
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
        operand (at ^ "/links") links @ operand (at ^ "/rechts") rechts
    | Predicaat { operand = o; _ } -> operand (at ^ "/operand") o
    | Periode_conditie { vanaf; tot; _ } -> operand (at ^ "/vanaf") vanaf @ operand (at ^ "/tot") tot
    | Samengesteld { onderdelen; _ } ->
        List.concat
          (List.mapi (fun i c -> walk (Printf.sprintf "%s/onderdelen/%d" at i) c) onderdelen)
    | Kenmerk_check _ | Rollidmaatschap _ | Verre_rollidmaatschap _ | Regelversie _
    | Conditie_onopgelost _ ->
        [ v "WF-077" at
            "an IfThenElse condition reads a characteristic, role or channel — not admissible (only \
             binders, parameters and literals may be read; route state reads through a Let \
             binding)" ]
  in
  walk at c

(* WF-097 (maintainer decision 2026-09-09; widened to all three clips
   2026-09-10). A `gedurende` clip member's characteristic check resolves
   against exactly ONE subject and over the WHOLE span the clip ranges over,
   and every clip reader says so by construction: each matches `eigenaar` and
   `periode` away with `_`. A qualifier no reader honours must not be written.

   ONE helper for THREE clips, deliberately: the wire form is the same node in
   the same field (`Functie.filter`) at the temporal total (ch. 3.6, WF-055),
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
   reproduced it in 60 of 60. `niet: true` was indistinguishable from an
   absent qualifier in all of them, so both polarities are refused.

   The composite ALLE clip recurses through the same arm, so a member of a
   composite is fenced exactly as a plain member is. *)
let wf097_clip_qualifications at (c : conditie) : violation list =
  let rec walk (c : conditie) =
    match c with
    | Kenmerk_check { kenmerk; eigenaar; periode; _ } ->
        let naam = Option.value kenmerk ~default:"?" in
        (match eigenaar with
         | Some e ->
             [ v "WF-097" at
                 ("owner '" ^ e
                ^ "' on a gedurende clip member: the clip reads the SUBJECT's characteristic \
                   timeline, so an owner here names a subject no reader consults") ]
         | None -> [])
        @ (match periode with
           | Some p ->
               [ v "WF-097" at
                   (Printf.sprintf
                      "whole-period qualifier (%s) on the gedurende clip member '%s': the clip \
                       already ranges over the whole timeline, so the qualifier — negated or not \
                       — names no reading the clip has"
                      (if p.gp_niet then "negated" else "affirmative") naam) ]
           | None -> [])
    | Samengesteld { onderdelen; _ } -> List.concat_map walk onderdelen
    | _ -> []
  in
  walk c

let rec walk_expr ctx ~binders ~subject ?(body = Rule_pos) at (e : expr) : violation list =
  let sub p = at ^ "/" ^ p in
  let typ_checks =
    match typ_of_expr e with
    | Some (Datatype_onbekend _) ->
        [ v "WF-034" (sub "type") "unknown-type sentinel inside a structured node" ]
    | Some d ->
        payload_sentinel (sub "type") d
        @ check_number_form (sub "type") d
        @ check_datatype_units ~stelsels:(stelsel_namen ctx) (sub "type") d
        @ enum_use_site ctx (sub "type") d
        @ struct_use_site ctx (sub "type") d
    | None -> (
        (* A value node without a static type is the unspelled twin of the
           Onbekend sentinel — refused symmetrically (WF-034). A Lambda is
           never a value: it is structurally typeless (special-cased by node
           kind, not by the [typ_of_expr] None result) and stays admissible
           here — its own position discipline is WF-069 below. *)
        match e with
        | Lambda _ -> []
        | _ -> [ v "WF-034" (sub "type") "value node without a static type" ])
  in
  typ_checks
  (* WF-091 at a COMPOSITE position: the annotation against the expectation
     its operands DERIVE (ch. 4.1, "derived-type identity"). Beside the type
     prologue, for the same reason: this is a claim about the node's own
     [type], not about its children, and it holds wherever the walk reaches
     a covered node kind. *)
  @ composite_identity ctx at e
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
         | Functie _ -> deny "an aggregation"
         | Temporal_read _ -> deny "a temporal read"
         | Variabele_ref _ -> deny "a variable read"
         | _ -> None)
   in
   match closed with
   | Some vs -> vs
   | None ->
   match e with
  | Literal { waarde; typ; _ } -> (
      match typ, waarde with
      | Some (Numeriek _ | Percentage _), Some w when not (lexeme_bare_number w) ->
          [ v "WF-081" at
              ("numeric literal lexeme '" ^ w
               ^ "' is not a bare number (sign, digits, optional fraction — ch. 4.1)") ]
      | Some Datum_in_dagen, Some w when not (lexeme_iso_date w) ->
          [ v "WF-084" at
              ("date literal lexeme '" ^ w
               ^ "' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges — ch. 4.1)") ]
      | Some Datum_tijd, Some w when not (lexeme_datum_tijd w) ->
          [ v "WF-095" at
              ("datum-tijd literal lexeme '" ^ w
               ^ "' is neither the canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] nor the source \
                  snippet form [dd. ]d-m-yyyy hh:mm:ss[.SSS] (fixed field widths, 'T' never a \
                  space, the seconds field written, no zone or offset, a fraction of exactly \
                  three digits — ch. 4.1)") ]
      | Some (Enumeratie { waarden; _ }), Some w when not (mem_ci (some_names waarden) w) ->
          [ v "WF-039" at ("enumeration label '" ^ w ^ "' is not in the declared domain") ]
      | Some (Numeriek { teken = Some t; _ }), Some w -> (
          match t with
          | Niet_negatief when lexeme_negative w ->
              [ v "WF-039" at ("negative literal '" ^ w ^ "' under a non-negative sign restriction") ]
          | Positief when lexeme_negative w || lexeme_zero w ->
              [ v "WF-039" at ("literal '" ^ w ^ "' under a positive sign restriction") ]
          | Negatief when not (lexeme_negative w) ->
              [ v "WF-039" at ("literal '" ^ w ^ "' under a negative sign restriction") ]
          | _ -> [])
      | _ -> [])
  | Tekst_reeks { delen; _ } ->
      List.concat
        (List.mapi
           (fun i d ->
             match d with
             | Letterlijk _ -> []
             | Ingevoegd { expr = Some e } ->
                 walk_expr ctx ~binders ~subject ~body (Printf.sprintf "%s/delen/%d/expr" at i) e
             | Ingevoegd { expr = None } -> [])
           delen)
  | Binair { op; links; rechts; _ } ->
      let lt = Option.bind links typ_of_expr and rt = Option.bind rechts typ_of_expr in
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
           [ ("links", lt); ("rechts", rt) ]
       else [])
      @ (match op, lt, rt with
         | Tot_de_macht, Some l, Some r
           when unit_of l <> None || unit_of r <> None ->
             [ v "WF-035" at "power operands must be unitless" ]
         | (Plus | Min | Verminderd_met), Some l, Some r when is_number_like l && is_number_like r ->
             (* verminderd-met belongs to the additive family (ch. 1.2a);
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
         | (Plus | Min), Some l, _ when is_date l -> duration_side "rechts" rt l
         | _ -> [])
      @ (match links with Some e -> walk_expr ctx ~binders ~subject ~body (sub "links") e | None -> [])
      @ (match rechts with Some e -> walk_expr ctx ~binders ~subject ~body (sub "rechts") e | None -> [])
  | Functie f ->
      let arity = List.length f.argumenten in
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
      @ (if
           List.mem f.kind aggregation_kinds && f.element_objecttype = None
           && f.dimensie_selectie = None && arity < 2 && f.kind <> Aantal
         then [ v "WF-037" at "value fold with fewer than two operands" ]
         else [])
      (* WF-034: references *)
      @ (match f.element_objecttype with
         | Some t when objecttype ctx t = None ->
             [ v "WF-034" at ("aggregation element type '" ^ t ^ "' is not declared") ]
         | _ -> [])
      (* WF-038: operand typing slices *)
      @ (match f.kind, f.argumenten with
         | Eerste_paasdag, [ a ] -> (
             match typ_of_expr a with
             | Some (Numeriek { geheel = true; eenheid = None; _ }) -> []
             | Some d ->
                 [ v "WF-038" at ("eerste-paasdag operand is " ^ datatype_kind d
                                  ^ ", must be a whole unitless number") ]
             | None -> [])
         | (Uur_uit | Minuut_uit | Seconde_uit | Milliseconde_uit), [ a ] -> (
             match typ_of_expr a with
             | Some Datum_tijd | None -> []
             | Some d ->
                 [ v "WF-038" at ("sub-day extractor operand is " ^ datatype_kind d
                                  ^ ", must be a millisecond-precision date") ])
         | (Jaar_uit | Maand_uit | Dag_uit), [ a ] -> (
             (* the day-precision extractors read civil components (ch. 3.8
                of RUNTIME-SEMANTICS): the operand must be a date — either
                precision, mirroring the v0 field read *)
             match typ_of_expr a with
             | Some d when not (is_date d) ->
                 [ v "WF-038" at ("day-precision extractor operand is " ^ datatype_kind d
                                  ^ ", must be a date") ]
             | _ -> [])
         | (Tijdsduur | Absolute_tijdsduur), [ a; b ] ->
             (* the WF-038 tijdsduur clause (RUNTIME-SEMANTICS ch. 3.8): both
                operands dates of one precision; the node type carries the
                counted unit — a recognised time-unit or declared day-kind
                spelling; a sub-day unit demands millisecond-precision
                operands *)
             let ta = typ_of_expr a and tb = typ_of_expr b in
             let operand side t =
               match t with
               | Some d when not (is_date d) ->
                   [ v "WF-038" (sub side)
                       ("tijdsduur operand is " ^ datatype_kind d ^ ", must be a date") ]
               | _ -> []
             in
             operand "argumenten/0" ta
             @ operand "argumenten/1" tb
             @ (match ta, tb with
                | Some da, Some db when is_date da && is_date db && da <> db ->
                    [ v "WF-038" at
                        "tijdsduur operands mix date precisions (both must share one \
                         precision)" ]
                | _ -> [])
             @ (match f.typ with
                | None ->
                    (* the counted unit lives on the node type: an absent
                       type cannot carry it — without this arm the wire is
                       verifier-clean and faults only at evaluation *)
                    [ v "WF-038" at
                        "tijdsduur without a node type (the counted unit is the node \
                         type's unit)" ]
                | Some t -> (
                    match unit_of t with
                    | None ->
                        [ v "WF-038" at
                            "tijdsduur without a recognised time unit on its node type" ]
                    | Some u -> (
                        match time_unit_of_eenheid u with
                        | Some tu ->
                            if time_unit_sub_day tu
                               && (ta = Some Datum_in_dagen || tb = Some Datum_in_dagen)
                            then
                              [ v "WF-038" at
                                  "sub-day tijdsduur unit on day-precision operands \
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
         semantics `RUNTIME-SEMANTICS.md` ch. 1.2): EERSTE/LAATSTE/
         MINIMUM/MAXIMUM keep the least or greatest CONTRIBUTOR, so every
         operand must come from one family an order actually runs over.
         Enumeration, SumType, StructType, text and boolean are
         equality-only (the ch. 2.4 orderability paragraph); numbers and
         dates have no common order between them; and the two DATE
         REPRESENTATIONS are two families for the same reason — a
         day-precision date names a whole day, a millisecond-precision
         one a point inside a day, and ordering the two is what ref
         faults on (`unordered comparison: 2026-06-21 vs @…ms`). Hence
         the family tag is per representation, not one `Date` bucket.

         The rule reads `argumenten` and nothing else, so it does not
         turn on the wire form: an instance fold spells its contributors
         as one set-valued argument whose type is the element attribute's
         datatype, an articulated fold as two or more listed ones — and 4
         of the corpus's 14 articulated ordering folds carry
         `elementObjecttype` besides (v0 stamps the tag vestigially), so
         form is no dichotomy a rule could have keyed on.

         The unknown sentinel and an operand carrying no type at all
         DEFER. Where that defer is unreachable it is dead, and where it
         is reachable it is load-bearing: an operand walked as an
         ordinary expression is already refused by WF-034 (`unknown-type
         sentinel inside a structured node` for the sentinel, `value node
         without a static type` for the twin, both measured) and, since
         WF-034's seventh wave (2026-09-06), so is a fold operand that is
         not a fold SOURCE — in BOTH spellings, the sentinel and the
         null, on the same guard. What still reaches this arm is
         therefore only the single argument of an AGGREGATION: the
         ch. 4.3 row-1 wart, which WF-034 tolerates by construction, plus
         the same position spelled with no type at all, which no producer
         in this repo emits (measured) and which the wart's own exemption
         covers because the guard is one predicate for both. There the
         defer holds open a shape no executor carries: an instance
         ordering fold over a bare role source is verifier-clean today
         and measured ref `value aggregation over a bare role source`,
         ASP `expression kind not translated`, Drools
         `ConstraintEvaluationException`. An open flank recorded, not a
         claim that the shape is sound. *)
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
             let typed = List.mapi (fun i a -> (i, typ_of_expr a)) f.argumenten in
             List.concat_map
               (fun (i, t) ->
                 match t with
                 | None | Some (Datatype_onbekend _) -> []
                 | Some d when family d = None ->
                     (* a bare "list" names no element, and the element is
                        what carries the unorderability here *)
                     let shown =
                       match d with List_type _ -> datatype_detail d | _ -> datatype_kind d
                     in
                     [ v "WF-038"
                         (Printf.sprintf "%s/argumenten/%d" at i)
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
             (match typ_of_expr lw with
              | Some d when not (is_number_like d) ->
                  [ v "WF-038" (sub "leegwaarde")
                      ("sum fallback is " ^ datatype_kind d ^ ", must be numeric") ]
              | _ -> [])
             @ walk_expr ctx ~binders ~subject (sub "leegwaarde") lw)
         | None -> [])
      (* WF-040: dimension selection labels *)
      @ (match f.dimensie_selectie with
         | Some ds -> (
             match ds.ds_dimensie with
             | Some dn -> (
                 match find_ci ctx.doc.dimensies dn with
                 | None ->
                     [ v "WF-040" (sub "dimensieSelectie")
                         ("selection over undeclared dimension '" ^ dn ^ "'") ]
                 | Some dim ->
                     List.concat_map
                       (fun l ->
                         match l with
                         | Some l when not (mem_ci (some_names dim.dim_waarden) l) ->
                             [ v "WF-040" (sub "dimensieSelectie")
                                 ("label '" ^ l ^ "' is not declared on dimension '" ^ dn ^ "'") ]
                         | _ -> [])
                       ds.ds_labels)
             | None -> [])
         | None -> [])
      @ (match f.filter with
         | Some c ->
             (* the die/dat subselection evaluates per contributor: the
                ELEMENT is the filter's subject, never the outer fold
                subject (the gedurende clip's elementObjecttype is the
                subject itself, so this switch is uniform) *)
             let fsubject =
               match f.element_objecttype with Some el -> Some el | None -> subject
             in
             walk_cond ctx ~binders ~subject:fsubject (sub "filter") c
         | None -> [])
      (* WF-089/WF-090 (domain identity, ch. 2.4), aggregation position: an
         element-passing fold's own type against each operand's type. The
         operand's type is itself bound to the declaration it reads (the
         reference arms below, or a structural equality for a composite),
         so crossing the node against its operand chains the node to that
         declaration — without this the fold is a laundering step: its
         value comes from a Kust attribute and its type says Vaarwater.

         The DOMAIN facet only, deliberately, where every other arm crosses
         the whole type (WF-091): a fold node's type is not a copy of one
         declaration but a DERIVED type over the operand list — the
         frontend takes MINIMUM/MAXIMUM decimals as the least operand
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
                      ~decl:(typ_of_expr a) ~typ:f.typ)
                  f.argumenten))
      @ (let source_exempt =
           (* The ch. 4.3 fold SOURCE position, and exactly it. Drawn on the
              SAME pair WF-093 keys on — a SINGLE argument, and a kind that
              folds (the six value folds of [value_fold_name] plus AANTAL) —
              so the rules that fence this branch meet edge to edge:
              single+AANTAL is the count source they leave alone,
              single+value-fold is WF-093's alone, and everything else is an
              ordinary read. Hoisted because TWO rules need the identical
              boundary — WF-034's sentinel fence below and WF-096's set
              fence — and a boundary written twice is a boundary that
              drifts. *)
           List.length f.argumenten = 1
           && (value_fold_name f.kind <> None || f.kind = Aantal)
         in
         List.concat
          (List.mapi
             (fun i a ->
               let p = Printf.sprintf "%s/argumenten/%d" at i in
               (* WF-096 (maintainer decision 2026-09-09): the set reading is
                  the fold SOURCE's field, so it is ill-formed at every fold
                  argument that is not a source. Computed ahead of the arms
                  rather than inside one of them because it must not depend on
                  which of them answers: two of the three refuse an argument
                  whose resolving pair is incomplete and return, so a set
                  reading there would go unreported only because the name
                  check answered first. Scoped to
                  `element_objecttype = Some` because that is exactly the
                  branch's own scope: without one the argument falls to
                  [walk_expr], whose own arm speaks the same sentence, and
                  reporting here as well would say it twice. *)
               (match a, f.element_objecttype with
                | Attribuut_ref { set = true; _ }, Some _ when not source_exempt ->
                    [ v "WF-096" p
                        "set reading on a fold operand that is not a fold SOURCE: a set \
                         reading is admissible on the single argument of an AGGREGATION alone \
                         (ch. 4.3) — with two or more arguments the node is the listed value \
                         fold, whose operands are ordinary reads, and at a non-aggregation \
                         kind there is no source at all" ]
                | _ -> [])
               @
               match a, f.element_objecttype with
               | Attribuut_ref { attribuut; objecttype = None; _ }, Some _ ->
                   (* WF-034 (widened, maintainer decision 2026-09-04): K1's
                      arm has to be applied here for the same reason WF-089's
                      is — this branch replaces [walk_expr] for the fold
                      source, and [aggregation_source_check] matches only on
                      `objecttype = Some`, so the absent endpoint fell through
                      BOTH. Measured: the shape plays in all three executors
                      (ref, clingo, Drools) off a unit lie no rule fenced. *)
                   [ v "WF-034" p
                       ("attribute reference '"
                       ^ Option.value attribuut ~default:"?"
                       ^ "' without an object type: it resolves against no declaration") ]
               | Attribuut_ref { attribuut = None; objecttype = Some otn; _ }, Some _ ->
                   (* the second half of the pair, in the fold source, and its
                      own arm for the same reason as the first: this branch
                      replaces [walk_expr] here, and
                      [aggregation_source_check] resolves only on
                      `attribuut = Some a`. Measured: with the generic arm
                      alone, 43 of the goldens' 751 emptied `attribuut`
                      positions stayed verifier-clean — every one of them a
                      fold-/aggregation argument. *)
                   [ v "WF-034" p
                       ("attribute reference on '" ^ otn
                      ^ "' without an attribute name: it resolves against no declaration") ]
               | Attribuut_ref { attribuut; typ; _ }, Some el ->
                   (* WF-034's SENTINEL FENCE, the seventh wave (maintainer
                      decision 2026-09-06, boundary widened in its fix round
                      2026-09-06): [walk_expr]'s type prologue refuses the v0
                      unknown sentinel on every value node — and its unspelled
                      twin, a value node with no static type at all — and this
                      branch REPLACES that prologue, so both survived at
                      exactly the positions the branch reaches. One exemption,
                      and it is the branch's whole reason for existing: the
                      SINGLE argument of an AGGREGATION is the fold SOURCE, the
                      ch. 4.3 row-1 wart, which carries the sentinel by
                      construction (a written role name has no datatype to
                      give) and whose kind WF-093 governs. Everything else the
                      branch reaches is an ORDINARY READ — no source, no role
                      name — and the sentinel is as ill-formed there as it is
                      anywhere else the walk reaches.

                      The boundary is the ARITY *and the KIND*, drawn on
                      exactly the pair WF-093 keys on, so the two rules meet
                      edge to edge with nothing between them and nothing
                      doubled: single+AANTAL is the count source both leave
                      alone; single+value-fold is WF-093's alone (it refuses a
                      non-AANTAL kind over the sentinel source); two or more
                      arguments is the listed value fold (§5.8.3), this arm's;
                      and a single argument at a NON-aggregation kind — a
                      builtin carrying an `elementObjecttype`, which has no
                      source either — is this arm's too. That last exit is the
                      fix round's: with the guard drawn on the arity alone,
                      three builtin spellings stayed verifier-clean (the
                      degraded own read, the wart verbatim and the set
                      reading), and the second of them CRASHES the third
                      executor.

                      Measured 2026-09-06 over the four zones (537 documents,
                      65 sentinel `type` positions): 61 sit at a fold argument,
                      60 of them the single-argument wart and exactly ONE a
                      listed operand — in the parity zone, on a document that
                      already carries a co-firing WF-034 at the same pointer.
                      Re-counted the same day on the rebased head (546
                      documents, main's datum-tijd card in the base): 71
                      sentinel `type` positions, 66 at a fold argument, 62 of
                      them the single-argument wart and 4 listed — the three
                      witnesses this arm brought plus that same ONE parity
                      operand, which is the invariant that carries the claim.
                      No golden, rood or negatief document moves, and neither
                      half of the fix round moves one either. What the shape
                      bought itself while it was clean is the familiar
                      self-served exemption: [use_site_identity] defers on the
                      sentinel and [composite_expectation]'s MINIMUM/MAXIMUM
                      arm defers when any operand type is unknown, so one
                      degraded annotation switched off the whole identity
                      family at that operand. And no executor carries it:
                      `ref` faults `unsupported AttribuutRef shape
                      (set/sentinel outside a fold)`, the ASP backend refuses
                      the expression kind, while the Drools backend PLAYS the
                      degraded-read spelling and answers, and THROWS on the
                      wart-verbatim spelling (it reaches for a getter of the
                      role name) — one-answers-two-refuse, the class PLAN §6
                      calls a spec bug, cut off at the wire. The unspelled twin
                      is the same fence on the same positions and is measured
                      the other way round: no frontend in this repo emits a
                      null `type` (73 of the repo's 78 `.rgs` sources emit, 0
                      occurrences), so that half closes a WIRE form rather than
                      a producer route — but the ONE listed null operand built
                      by hand is answered by `ref` (all 5 pinned expectations)
                      and by Drools while the ASP backend refuses, which is the
                      same class read from the other side. *)
                   (* the exemption is [source_exempt], hoisted above the
                      argument walk: the ch. 4.3 wart and exactly it. *)
                   (match typ with
                    | Some (Datatype_onbekend _) when not source_exempt ->
                        [ v "WF-034" (p ^ "/type")
                            ("unknown-type sentinel on a fold operand that is not a fold SOURCE: \
                              the sentinel is admissible on the single argument of an AGGREGATION \
                              alone (the v0 row-1 wart, ch. 4.3) — with two or more arguments the \
                              node is the listed value fold, whose operands are ordinary reads, \
                              and at a non-aggregation kind there is no source at all") ]
                    | None when not source_exempt ->
                        (* the unspelled twin, refused symmetrically — the same
                           sentence [walk_expr]'s type prologue speaks, on the
                           positions this branch took away from it *)
                        [ v "WF-034" (p ^ "/type") "value node without a static type" ]
                    | _ -> [])
                   @ (match typ with
                    | Some d -> check_datatype_units ~stelsels:(stelsel_namen ctx) (p ^ "/type") d
                    | None -> [])
                   @ use_site_identity (p ^ "/type")
                     ~what:
                       ("fold-source attribute reference '"
                      ^ Option.value attribuut ~default:"?" ^ "'")
                     ~decl:(resolved_attr_datatype ctx a) ~typ
                   @ aggregation_source_check ctx p ~element_type:el ~subject
                       ~single:(List.length f.argumenten = 1) ~kind:f.kind a
                   (* WF-053 rides along for the same reason WF-089 and
                      WF-092 do, and for the operands WF-083 no longer
                      speaks about: with two or more arguments the fold is
                      the listed value fold (§5.8.3) and its operands are
                      ORDINARY READS, which navigate like any other read —
                      but this branch replaces [walk_expr], so without the
                      repetition a nested operand is the one read position
                      whose hop resolves against nothing. Measured: a
                      MAXIMUM whose second operand names a type no fact
                      type relates to the subject was verifier-clean and is
                      now WF-053 `bindingless navigation: no fact type
                      relates …` — exactly what the same read is outside a
                      fold — and the to-many hop in a scalar slot comes with
                      it. *)
                   @ (match a with
                      | Attribuut_ref
                          { objecttype = Some otn; navigatie_rol; navigatie_pad;
                            navigatie_bindingen; set; _ }
                        when List.length f.argumenten <> 1 -> (
                          let navigated =
                            navigatie_rol <> None || navigatie_pad <> []
                            || (match subject with
                                | Some st -> norm st <> norm otn
                                | None -> false)
                          in
                          if not navigated then
                            match navigatie_bindingen with
                            | Some _ ->
                                [ v "WF-053" p
                                    "navigation bindings on a reference that does not \
                                     navigate" ]
                            | None -> []
                          else
                            match subject with
                            | None -> [] (* no subject type anchors the walk *)
                            | Some st ->
                                wf053_attribuut_ref ctx p ~subject:st ~set
                                  ~bindings:navigatie_bindingen ~navigatie_rol ~navigatie_pad
                                  ~endpoint:(Some otn))
                      | _ -> [])
               | _ -> walk_expr ctx ~binders ~subject p a)
             f.argumenten))
      (* WF-055: the gedurende clip — a filter on the temporal total is a
         timeline operation; its condition is an own temporal
         characteristic check or a per-segment comparison of an own
         temporal attribute against a scalar bound *)
      @ (match (f.kind, f.argumenten, f.filter) with
         | ( Totaal,
             [ Attribuut_ref
                 { attribuut = Some a; objecttype = Some aot; navigatie_rol = None;
                   navigatie_pad = []; set = false; _ } ],
             Some c )
           when (match objecttype ctx aot with
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
               | Kenmerk_check { kenmerk = Some k; navigatie = None; _ } ->
                   if temporal_char k then []
                   else
                     [ v "WF-055" (sub "filter")
                         ("gedurende over '" ^ k
                        ^ "', which is not a temporal characteristic of '" ^ aot ^ "'") ]
               | Vergelijking
                   { links = Some (Attribuut_ref
                       { attribuut = Some b; objecttype = Some botn; navigatie_rol = None;
                         navigatie_pad = []; set = false; _ });
                     rechts = Some _; periode = None; _ }
                 when norm botn = norm aot
                      && (match objecttype ctx botn with
                         | Some ot -> attr_temporal ot b
                         | None -> false) ->
                   []
               | Samengesteld { kwantor = Alle; onderdelen = (_ :: _) as onderdelen; _ } ->
                   (* the FLAT composite ALLE clip (ch. 3.6): members are
                      own-temporal-characteristic checks only. The v0
                      boundary, stated precisely: v0 partitions STATIC
                      comparison members out as value-side guards before
                      its clip runs, and markers temporal-attribute
                      comparisons and nested composite bullets as
                      Onopgelost -- v1 narrows composite members to the
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
                        bound, or an ALLE composite of characteristic checks" ]
             in
             clip_cond_ok c @ wf097_clip_qualifications (sub "filter") c)
         | _ -> [])
      (* WF-056: the pro-rata fold (tijdsevenredig-deel) MUST carry a
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
           | None -> [ v "WF-056" at "tijdsevenredig-deel without a gedurende condition" ]
           | Some (Kenmerk_check { kenmerk = Some k; navigatie = None; _ }) -> (
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
                           [ v "WF-056" (sub "argumenten/0")
                               "pro-rata base granularity is finer than the bucket" ]
                       | _ -> [])
                   | _ ->
                       [ v "WF-056" (sub "argumenten/0")
                           ("pro-rata base '" ^ b ^ "' is not a temporal attribute") ])
               | None -> [])
           | [ Parameter_ref { parameter = Some p; _ } ] -> (
               match find_ci ctx.doc.parameters p with
               | Some { par_granulariteit = Some _; _ } -> []
               | _ ->
                   [ v "WF-056" (sub "argumenten/0")
                       ("pro-rata base parameter '" ^ p ^ "' is not granular") ])
           | _ -> [ v "WF-056" at "tijdsevenredig-deel takes one temporal source argument" ])
      (* WF-047/048: the fold-source role binding (v1 extension) *)
      @ (let fold_source =
           match (f.element_objecttype, f.argumenten) with
           | ( Some el,
               [ Attribuut_ref
                   { attribuut; typ = Some (Datatype_onbekend _);
                     navigatie_rol = None; navigatie_pad = []; _ } ] ) ->
               Some (el, `Role_name attribuut)
           | ( Some el,
               [ Attribuut_ref { set = true; navigatie_rol = None; navigatie_pad = []; _ } ] )
             ->
               Some (el, `Member)
           | _ -> None
         in
         (* WF-102 (maintainer decision 2026-09-11): the fold source's range
            may be a PATH. The path IS the binding, so the bindingless
            derivability arm below (WF-048) never applies to it: WF-048
            answers for a type PAIR, and a path's intermediate types are its
            own claim rather than a derivable consequence of its endpoints.
            Placed AHEAD of the one-step logic and returning instead of
            falling through, so exactly one of the two shapes is judged and
            a document carrying both is reported once, by (a). *)
         match f.bron_pad with
         | Some pad ->
             (* (a) one meaning, one spelling *)
             (match f.bron_rol with
             | Some _ ->
                 [ v "WF-102" (sub "bronPad")
                     "bronPad beside bronRol: the one-step range IS bronRol, so the two fields \
                      are mutually exclusive" ]
             | None -> [])
             (* (b) the arity: the empty list and the single step are the
                one-step spelling's territory, not the path's *)
             @ (if List.length pad >= 2 then []
                else
                  [ v "WF-102" (sub "bronPad")
                      (Printf.sprintf
                         "a bronPad carries at least TWO steps, found %d — the one-step range is \
                          spelled bronRol"
                         (List.length pad)) ])
             (* (c) the position: the set source of a single-argument
                instance fold, exactly where bronRol stands. The non-set
                sources hand the fold something else entirely (the temporal
                total reads the subject's own timeline, the row-1 wart hands
                it the subject's linked instances), so a range does not
                apply to them. *)
             @ (match fold_source with
               | Some (_, `Member) -> []
               | _ ->
                   [ v "WF-102" (sub "bronPad")
                       "a bronPad is admissible only on the SET source of a single-argument \
                        instance fold — the position bronRol stands at" ])
             (* (d) every step resolves (WF-047) and the steps JOIN: the
                first step's opposite role is filled by the fold subject,
                each further step's opposite role by the type its
                predecessor reaches, and the last step's bound role by the
                elementObjecttype. A step that does not resolve stops the
                walk: without its fillers the rest of the chain has nothing
                to join against, and reporting a cascade of joins off an
                unresolved step would name the wrong defect. *)
             @ (let rec chain i from_t = function
                  | [] -> []
                  | (rb : rol_binding) :: rest -> (
                      let step = sub ("bronPad/" ^ string_of_int i) in
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
                                        at '%s' (%s) — a bronPad is a connected walk"
                                       (Option.value other.rol_naam ~default:"?")
                                       other_filler ft
                                       (if i = 0 then "the fold subject"
                                        else "the type the previous step reaches")) ]
                          in
                          (* TWO-SIDED, on the WF-047 line and deliberately
                             not narrowed here. The bronPad's landing asks
                             the same question the ordinary fold source
                             asks through [wf047_binding] with
                             ~bound_filler:(Some el), which the kernel made
                             ⊑-compatible in both directions; a one-sided
                             test on this arm alone would make the two
                             spellings of one fold disagree again — the very
                             split the bound/bindingless correction removed.
                             The direction that is only half-implemented —
                             an elementObjecttype STRICTLY BELOW what the
                             path reaches, which needs an exclusion filter
                             no executor has — is therefore admitted here
                             exactly as the bronRol arm already admits it,
                             and closing it is one change on both arms plus
                             the executors, not a tightening of this line. *)
                          let lands =
                            if rest <> [] then []
                            else
                              match f.element_objecttype with
                              | Some el when not (type_compatible ctx bound_filler el) ->
                                  [ v "WF-102" step
                                      (Printf.sprintf
                                         "the last step's bound role '%s' fills '%s', but the \
                                          fold's elementObjecttype is '%s' — the path must land \
                                          on the element type"
                                         (Option.value bound.rol_naam ~default:"?")
                                         bound_filler el) ]
                              | _ -> []
                          in
                          joins @ lands @ chain (i + 1) (Some bound_filler) rest)
                in
                chain 0 subject pad)
         | None -> (
         match (fold_source, f.bron_rol) with
         | None, None -> []
         | None, Some _ ->
             [ v "WF-047" (sub "bronRol")
                 "a fold-source role binding on a node that is not an instance fold over a \
                  relation" ]
         | Some (el, written), Some rb -> (
             wf047_binding ctx (sub "bronRol") rb ~bound_filler:(Some el) ~other_filler:subject
             @
             (* the written v0 source (the row-1 wart) must denote the
                bound role — one relation, not two spellings of two *)
             match (resolve_binding ctx rb, written) with
             | Ok (_, bound, _), `Role_name (Some w)
               when not
                      ((match bound.rol_naam with Some n -> norm n = norm w | None -> false)
                      || match bound.rol_meervoud with
                         | Some m -> norm m = norm w
                         | None -> false) ->
                 [ v "WF-047" (sub "bronRol")
                     (Printf.sprintf "the written fold source '%s' does not denote the bound role '%s'"
                        w
                        (Option.value bound.rol_naam ~default:"?")) ]
             | _ -> [])
         | Some (el, _), None -> (
             match subject with
             | Some st -> wf048_derivable ctx at ~from_type:st ~to_type:el ~site:"instance fold"
             | None -> [])))
  | Afgerond { basis; _ } ->
      (match basis with Some e -> walk_expr ctx ~binders ~subject ~body (sub "basis") e | None -> [])
  | Begrensd { basis; minimum; maximum; _ } ->
      (if minimum = None && maximum = None then
         [ v "WF-035" at "clamp without any bound" ]
       else [])
      @ List.concat_map
          (fun (n, e) ->
            match e with
            | Some e ->
                (match typ_of_expr e with
                 | Some d when not (is_number_like d) ->
                     [ v "WF-035" (sub n) ("clamp " ^ n ^ " is " ^ datatype_kind d ^ ", must be numeric") ]
                 | _ -> [])
                @ walk_expr ctx ~binders ~subject ~body (sub n) e
            | None -> [])
          [ ("basis", basis); ("minimum", minimum); ("maximum", maximum) ]
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
      @ (match Option.bind basis typ_of_expr with
         | Some bt when is_number_like bt ->
             List.concat_map
               (fun (n, e) ->
                 match Option.bind e typ_of_expr with
                 | Some d
                   when is_number_like d
                        && not (units_compatible_adopting basis e (unit_of bt) (unit_of d)) ->
                     [ v "WF-099" (sub n) ("clamp " ^ n ^ " and base have incompatible units") ]
                 | _ -> [])
               [ ("minimum", minimum); ("maximum", maximum) ]
         | _ -> [])
  | Attribuut_ref
      { attribuut; objecttype = otn; navigatie_rol; navigatie_pad; navigatie_bindingen;
        set; typ; _ } -> (
      (* WF-089: the reference's type names the keuzetype domain of the
         attribute it reads (resolved at the endpoint type, so a navigated
         read crosses against the attribute it lands on); an unresolvable
         reference — a role-name fold source, a characteristic — defers *)
      use_site_identity (sub "type")
        ~what:("attribute reference '" ^ Option.value attribuut ~default:"?" ^ "'")
        ~decl:(resolved_attr_datatype ctx e) ~typ
      (* WF-096 (maintainer decision 2026-09-09): `set` says the read yields
         the members' values, one per member — a COLLECTION. Every position
         this walk reaches wants ONE value, so the field has nothing here to
         be consumed by: the only node that consumes a collection is the fold
         that owns the source, and a source is reached by the aggregation
         branch above, never by this one. Placed beside the type prologue and
         before the resolution below deliberately, and for the same reason the
         fold branch computes its half ahead of its arms: the endpoint arm at
         the bottom returns WF-034 alone on a reference whose object type is
         absent, so a set reading there would go unreported only because the
         name check answered first.

         Stated as a field rule rather than as an arm of WF-053: the walk's
         to-many clause does read `set`, and reads it as an ESCAPE
         ([wf053_attribuut_ref]'s `if set then []`) — a set reading is
         precisely how a to-many hop is admitted in a slot that would
         otherwise demand to-one. That escape is what made the shape clean:
         one field switched off the rule that would have caught the hop.
         After this clause the escape is unreachable through a well-formed
         document and stays as the second line of defence, never a dead
         branch. *)
      @ (if set then
           [ v "WF-096" at
               "set reading at an ordinary read position: a set reading names the members' \
                values, one per member, and is admissible only as the SOURCE of a \
                single-argument instance fold (ch. 4.3) — here nothing consumes the \
                collection and the slot wants one value" ]
         else [])
      @
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
                        endpoint carries no attribute value here. The tolerated
                        has_attr||has_kenmerk leak: a kenmerk is read through a
                        characteristic check (ch. 4.5), never an AttribuutRef in
                        a value slot. *)
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
                      resolves through `objecttype` AND `attribuut`; with the
                      member name absent it lands on a declared type and reads
                      nothing there. The absent name silences the same rules
                      from the other side — this member check and WF-080 both
                      sit behind `Some a`, and [resolved_attr_datatype] is
                      None, on which WF-089/090/091 defer. *)
                   [ v "WF-034" at
                       ("attribute reference on '" ^ otn
                      ^ "' without an attribute name: it resolves against no declaration") ])
              @
              (* WF-053: the navigated shapes — a written role, a declared
                 chain, or a directly named off-subject type (by-pair) *)
              let navigated =
                navigatie_rol <> None || navigatie_pad <> []
                || (match subject with Some st -> norm st <> norm otn | None -> false)
              in
              if not navigated then
                match navigatie_bindingen with
                | Some _ ->
                    [ v "WF-053" at
                        "navigation bindings on a reference that does not navigate" ]
                | None -> []
              else (
                match subject with
                | None -> [] (* no subject type anchors the walk *)
                | Some st ->
                    wf053_attribuut_ref ctx at ~subject:st ~set
                      ~bindings:navigatie_bindingen ~navigatie_rol ~navigatie_pad
                      ~endpoint:(Some otn)))
      | None ->
          (* WF-034 (widened, maintainer decision 2026-09-04): the absent
             endpoint is not one rule's deferral but a generic exemption —
             it skips the whole branch above (member resolution, WF-053,
             WF-080) and makes [resolved_attr_datatype] None, which defers
             WF-089/090/091 too. Five rules fall silent on one empty field,
             and the shape is measured answer-changing: a unit lie on such
             a reference was verifier-clean while one executor played it. *)
          [ v "WF-034" at
              ("attribute reference '"
              ^ Option.value attribuut ~default:"?"
              ^ "' without an object type: it resolves against no declaration") ])
  | Dimensie_cel { attribuut; labels; typ; _ } ->
      (match attribuut with
       | Some (Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ } as inner) ->
           (* WF-089: a cell of a dimensioned attribute carries that
              attribute's datatype, so the cell read's own type crosses
              against the declaration too (the inner reference crosses its
              own type in its walk below) *)
           use_site_identity (sub "type") ~what:("dimension-cell read of '" ^ a ^ "'")
             ~decl:(resolved_attr_datatype ctx inner) ~typ
           @
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
           @ walk_expr ctx ~binders ~subject (sub "attribuut") inner
       | Some (Attribuut_ref _ as inner) ->
           (* an attribute reference with an empty half: the walk's own
              WF-034 arms report that endpoint, so this arm stays silent
              rather than reporting the same defect twice *)
           walk_expr ctx ~binders ~subject (sub "attribuut") inner
       | Some inner ->
           (* WF-034 (widened, maintainer decision 2026-09-04, third wave):
              a cell read reads ONE dimensioned attribute, so its `attribuut`
              child is a reference position, not a general expression slot.
              Anything else there resolves against no declaration and takes
              the arm above down with it: WF-040's dimension/label cross and
              the identity family's cross of the cell's OWN type both sit
              behind the `Some/Some` reference pattern. Measured on
              `wf091-dimensiecel-eenheid`, whose pinned unit lie sits on the
              cell type: replacing the reference by a literal turns that
              refusal off and the document CLEAN. *)
           [ v "WF-034" (sub "attribuut")
               "dimension-cell read whose attribute position is not an attribute reference: it \
                resolves against no declaration" ]
           @ walk_expr ctx ~binders ~subject (sub "attribuut") inner
       | None ->
           (* the empty half of the same position, and the same consequence
              one field further: measured verifier-clean on both golden cell
              positions and on `wf091-dimensiecel-eenheid`, which turns
              CLEAN when its cell's `attribuut` is emptied *)
           [ v "WF-034" at
               "dimension-cell read without an attribute reference: it resolves against no \
                declaration" ])
  | Parameter_ref { parameter; typ; _ } -> (
      match parameter with
      | Some p -> (
          match find_ci ctx.doc.parameters p with
          | None -> [ v "WF-034" at ("reference to undeclared parameter '" ^ p ^ "'") ]
          | Some (par : parameter) ->
              (* WF-089: the reference's type names the keuzetype domain of
                 the parameter it reads *)
              use_site_identity (sub "type") ~what:("parameter reference '" ^ p ^ "'")
                ~decl:par.par_datatype ~typ)
      | None ->
          (* WF-034 (widened, 2026-09-04, second wave): the same shape one node
             kind over. A parameter reference resolves through exactly one
             field; the arm above reports a name that resolves nowhere, and
             sits behind `Some p`, so an ABSENT name walked past it — and past
             the identity family with it, which crosses this node's type
             against the parameter's declared datatype and defers on an
             unresolvable declaration. *)
          [ v "WF-034" at
              "parameter reference without a parameter name: it resolves against no declaration"
          ])
  | Variabele_ref _ -> [] (* binding sites are not serialized in v0 — OPEN *)
  | Constante { naam; _ } -> (
      (* WF-098 (maintainer decision 2026-09-10): the context-value name is a
         CLOSED SET, not a reference. Nothing in the document declares it, so
         there is no table to resolve against — the three admissible names are
         fixed by ch. 4.1 and this arm enumerates them.

         The comparison is BYTE-EXACT, and that is the decision the arm carries:
         the readers below the wire disagree about case (`ref` and the ASP
         backend normalise, the Drools backend's constant reader uses `equals`
         while its day-kind reader uses `equalsIgnoreCase`), so a ci-different
         spelling is a name three consumers accept and one does not. Refusing it
         here makes that divergence unreachable through a verifier-clean
         document instead of leaving it to be discovered per executor.

         Scope: the PRESENT name. A `naam` of null is schema-valid (the field is
         typed ["string", "null"]) and is the empty half of the same field —
         its own question, recorded in ch. 4.1 with its measurement, not folded
         in here. *)
      match naam with
      | None -> []
      | Some n when List.mem n [ "Rekendatum"; "Rekenjaar"; "de dag" ] -> []
      | Some n ->
          [ v "WF-098" (sub "naam")
              ("context value '" ^ n
             ^ "' is not one of the three admissible names Rekendatum, Rekenjaar, de dag: the \
                context-value name is a closed set, spelled byte for byte") ])
  | Expr_onopgelost _ -> [] (* honest marker; WF-044 is behavioural *)
  | Temporal_read { basis; datum; typ; _ } ->
      (* WF-049: the point-in-time read (extension node, row 11) *)
      let basis_checks =
        match basis with
        | Attribuut_ref
            { attribuut = Some a; objecttype = Some otn; navigatie_rol = None;
              navigatie_pad = []; set = false; _ } -> (
            match objecttype ctx otn with
            | None -> [] (* resolution is the walk's WF-034, below *)
            | Some ot ->
                (if attr_temporal ot a then []
                 else
                   [ v "WF-049" (sub "basis")
                       ("temporal read of '" ^ a ^ "', which is not a temporal attribute of '"
                      ^ otn ^ "'") ])
                @ (match subject with
                  | Some st when norm st <> norm otn ->
                      [ v "WF-049" (sub "basis")
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
                   [ v "WF-049" (sub "basis")
                       ("temporal read of parameter '" ^ p ^ "', which is not granular") ])
                @ (match (typ, par.par_datatype) with
                  | Some t, Some decl when t <> decl ->
                      [ v "WF-049" (sub "type")
                          "the read's type must equal the timeline's element datatype" ]
                  | None, _ ->
                      [ v "WF-049" (sub "type") "the read carries no type (extension node: required)" ]
                  | _ -> []))
        | _ ->
            [ v "WF-049" (sub "basis")
                "the read's basis must be an own temporal attribute reference or a granular \
                 parameter reference (a temporal characteristic operand is OPEN)" ]
      in
      let datum_checks =
        match typ_of_expr datum with
        | Some Datum_in_dagen -> []
        | Some d ->
            [ v "WF-049" (sub "datum")
                ("the read's date operand is " ^ datatype_kind d
               ^ ", must be a day-precision date") ]
        | None ->
            [ v "WF-049" (sub "datum")
                "the read's date operand carries no type (extension node: day precision required)" ]
      in
      basis_checks @ datum_checks
      @ walk_expr ctx ~binders ~subject (sub "basis") basis
      @ walk_expr ctx ~binders ~subject (sub "datum") datum
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
                            match typ_of_expr a with
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
        match typ_of_expr scrutinee with
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
      (* WF-064: all arm bodies agree on one result datatype — the Match's own type *)
      let body_type_checks =
        match typ with
        | None -> []
        | Some mt ->
            List.filter_map
              (fun (c, _, body) ->
                match typ_of_expr body with
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
               let binders' =
                 match binder with
                 | Some b ->
                     (* the arm binder's declared datatype is the matched
                        constructor's payload (WF-078 reads it) *)
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
               walk_expr ctx ~binders:binders' ~subject ~body
                 (Printf.sprintf "%s/arms/%d/body" at i) arm_body)
             arms)
  | Binder_ref { naam; typ; _ } -> (
      (* WF-064: a binder reference resolves lexically to an enclosing binder
         (a match arm, or a lambda/function parameter — row 10); a
         VariabeleRef stays outside this rule (row 24). Inside a closed body
         the visible [binders] are its own — a lambda/function's parameters
         and whatever an inner Let/match/filter binds; the enclosing scope's
         binders were dropped at the closure boundary, so a reference that
         reaches out to one is rejected here (the closure clause), citing the
         outer name it names when we still remember it. *)
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
      | Some (_, bdt) ->
          (* the identity engine on the binder arm. The binder's declared
             datatype is a match arm's declared payload, a lambda/function
             parameter's declared type, a Let value's type or a filter's
             element type; the reference's serialized [type] repeats it on
             every facet. Soort is WF-078's facet here (its number and its
             sentence, kept), the domain facet WF-089/WF-090's, every other
             facet WF-091's — the decimals and the unit WF-078 deliberately
             left uncrossed among them. *)
          use_site_identity (sub "type") ~soort:(binder_soort naam)
            ~what:("binder reference '" ^ naam ^ "'") ~decl:bdt ~typ)
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
                         match typ_of_expr value with
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
      (match typ_of_expr base with
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
      (match typ, typ_of_expr lbody with
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
      (* the body: the binder joins the scope carrying its value's datatype
         (WF-078); a Let body admits IfThenElse (Let_pos) unless already
         inside a closed body (stay closed) *)
      @ walk_expr ctx ~binders:((binder, typ_of_expr value) :: binders) ~subject
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
                    match typ_of_expr e with
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
           match typ, typ_of_expr e with
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
                     match typ_of_expr a with
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
      let src_elem = match typ_of_expr list with Some (List_type { element }) -> Some element | _ -> None in
      fold_source_check ctx (sub "list") list
      (* WF-070: the accumulator (node type = init type) is one first-order
         non-list datatype *)
      @ (match typ with
        | Some d when is_list_type d ->
            [ v "WF-070" (sub "type")
                "fold accumulator is list-typed — it must be a first-order non-list datatype" ]
        | _ -> [])
      @ (match typ, typ_of_expr init with
        | Some at', Some it when at' <> it ->
            [ v "WF-070" (sub "init") "fold init datatype differs from the accumulator type" ]
        | _ -> [])
      (* WF-069/070: the (accumulator, element) function, its closed body *)
      @ check_fold_fn ctx ~binders ~subject (sub "function") ~params:[ typ; src_elem ] ~result:typ functie
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list
      @ walk_expr ctx ~binders ~subject ~body (sub "init") init
  | Map_list { typ; list; functie; _ } ->
      let src_elem = match typ_of_expr list with Some (List_type { element }) -> Some element | _ -> None in
      let out_elem = match typ with Some (List_type { element }) -> Some element | _ -> None in
      fold_source_check ctx (sub "list") list
      (* WF-070: the node type is the ListType of the body's datatype *)
      @ (match typ with
        | Some (List_type _) -> []
        | Some d ->
            [ v "WF-070" (sub "type") ("map result is " ^ datatype_kind d ^ ", must be a list type") ]
        | None -> [ v "WF-070" (sub "type") "map carries no result type" ])
      @ check_fold_fn ctx ~binders ~subject (sub "function") ~params:[ src_elem ] ~result:out_elem functie
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list
  | Filter_list { typ; list; binder; condition; _ } ->
      let src_elem = match typ_of_expr list with Some (List_type { element }) -> Some element | _ -> None in
      fold_source_check ctx (sub "list") list
      (* WF-070: the node type equals the source's list type *)
      @ (match typ, typ_of_expr list with
        | Some t, Some s when t <> s ->
            [ v "WF-070" (sub "type") "filter result type must equal the source list type" ]
        | _ -> [])
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list
      (* the binder is bound in the condition — the aggregation-filter precedent;
         it carries the source element datatype (WF-078) *)
      @ walk_cond ctx ~binders:((binder, src_elem) :: binders) ~subject ~body (sub "condition") condition
  | List_length { typ; list; _ } ->
      fold_source_check ctx (sub "list") list
      (* WF-070: the count is a whole number *)
      @ (match typ with
        | Some (Numeriek { geheel = true; _ }) -> []
        | Some d ->
            [ v "WF-070" (sub "type") ("length is " ^ datatype_kind d ^ ", must be a whole number") ]
        | None -> [ v "WF-070" (sub "type") "length carries no type" ])
      @ walk_expr ctx ~binders ~subject ~body (sub "list") list)

(* WF-070: the fold-family [list] operand must be list-typed. *)
and fold_source_check _ctx at list =
  match typ_of_expr list with
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
      @ List.concat_map
          (fun (pn, pt) -> check_datatype_units ~stelsels:(stelsel_namen ctx) (at ^ "/parameters/" ^ pn) pt)
          parameters
      @ (match result, typ_of_expr lbody with
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

and walk_cond ctx ~binders ~subject ?(body = Rule_pos) at (c : conditie) : violation list =
  let sub p = at ^ "/" ^ p in
  let whole_period_check periode operands =
    match periode with
    | None -> []
    | Some _ ->
        if List.exists (fun e -> match e with Some e -> expr_temporal ctx e | None -> false) operands
        then []
        else
          [ v "WF-043" at "whole-period qualifier on a non-time-dependent operand" ]
  in
  let closed =
    (* inside a closed body a condition is limited to Vergelijking/Predicaat
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
      let lt = Option.bind links typ_of_expr and rt = Option.bind rechts typ_of_expr in
      let ordering =
        match op with
        | Kleiner | Kleiner_of_gelijk | Groter | Groter_of_gelijk -> `Num
        | Eerder | Eerder_of_gelijk | Later | Later_of_gelijk -> `Date
        | Gelijk | Ongelijk -> `Eq
      in
      (match ordering, lt, rt with
       | `Num, Some l, Some r ->
           (if not (is_number_like l) then
              [ v "WF-036" (sub "links") ("ordering operand is " ^ datatype_kind l) ]
            else [])
           @ (if not (is_number_like r) then
                [ v "WF-036" (sub "rechts") ("ordering operand is " ^ datatype_kind r) ]
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
             [ ("links", l); ("rechts", r) ]
           (* WF-036, the date-ordering precision clause: the two DATE
              REPRESENTATIONS are two comparable families, exactly as
              WF-038's ordering fold reads them — a day-precision date
              names a whole day, a millisecond-precision one a point
              inside a day, so no order runs between them. Each side is
              well-formed alone; their combination is not, so it is
              reported at the comparison node. The sibling equality arm
              below crosses the same seam ("equality across date
              precisions"); this arm is its ordering half.

              NO deferral on the sentinel here — deliberately unlike
              WF-038's ordering fold. An operand with no type at all does
              defer (the outer match skips it) and WF-034 reports it
              ("value node without a static type", measured); the unknown
              SENTINEL is REPORTED by the is_date guard just above
              ("date-ordering operand is unknown", measured, beside
              WF-034's "unknown-type sentinel inside a structured node"),
              so this precision clause never meets one. The fold arm
              defers because only the ch. 4.3 row-1 wart reaches it;
              every operand here is walked as an ordinary expression. *)
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
      @ (match links with Some e -> walk_expr ctx ~binders ~subject ~body (sub "links") e | None -> [])
      @ (match rechts with Some e -> walk_expr ctx ~binders ~subject ~body (sub "rechts") e | None -> [])
  | Predicaat { soort; operand; dagsoort; periode; _ } ->
      let ot = Option.bind operand typ_of_expr in
      (match soort, ot with
       | Elfproef, Some d -> (
           match d with
           | Tekst -> []
           | Numeriek { geheel = true; eenheid = None; _ } -> []
           | _ ->
               [ v "WF-036" at
                   ("elfproef operand is " ^ datatype_kind d
                    ^ ", must be text or a whole unitless number") ])
       | Numeriek_met_exact, Some d when d <> Tekst ->
           [ v "WF-036" at ("exact-digits operand is " ^ datatype_kind d ^ ", must be text") ]
       | Dagsoort, Some d when not (is_date d) ->
           [ v "WF-036" at ("day-kind predicate operand is " ^ datatype_kind d ^ ", must be a date") ]
       | _ -> [])
      @ (match soort, dagsoort with
         | Dagsoort, Some dk when not (mem_ci (some_names ctx.doc.dagsoorten) dk) ->
             [ v "WF-034" at ("reference to undeclared day kind '" ^ dk ^ "'") ]
         | _ -> [])
      @ whole_period_check periode [ operand ]
      @ (match operand with Some e -> walk_expr ctx ~binders ~subject ~body (sub "operand") e | None -> [])
  | Kenmerk_check { kenmerk; navigatie; periode; navigatie_binding; eigenaar; _ } ->
      (* WF-034 (widened, maintainer decision 2026-09-05, sixth wave; IR-SPEC
         ch. 4.6 (q)): a characteristic check's EXPLICIT OWNER. `eigenaar`
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
         unknown name, so `eigenaar` becomes absent, the two transpiler
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
         characteristic cross below is therefore keyed on `eigenaar` where the
         field is present and on `subject` only where it is absent, and an
         owner that does not declare the named characteristic is WF-100. *)
      (match eigenaar with
       | Some e when objecttype ctx e = None ->
           [ v "WF-034" (sub "eigenaar")
               ("characteristic check's owner '" ^ e
              ^ "' is not a declared object type: the check resolves against no declaration") ]
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
           `eigenaar` where the field is present, the enclosing form's subject
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
       let keyed_at = match eigenaar with Some _ -> sub "eigenaar" | None -> at in
       (match kenmerk, navigatie, decl_key with
        | Some k, None, Some otn -> (
            match objecttype ctx otn with
            | Some ot when not (has_kenmerk ot k) ->
                [ v keyed_wf keyed_at
                    ("'" ^ k ^ "' is not a declared characteristic of '" ^ otn ^ "'") ]
            | _ -> [])
        | _ -> [])
       @ (match periode, kenmerk, navigatie, decl_key with
          | Some _, Some k, None, Some otn -> (
              match objecttype ctx otn with
              | Some ot
                when not (List.exists (fun (n, _) -> norm n = norm k) ot.kenmerk_granulariteit) ->
                  [ v "WF-043" at "whole-period qualifier on a non-time-dependent characteristic" ]
              | _ -> [])
          | _ -> []))
      @ (* WF-053: the navigated characteristic check — one hop, bound or
           uniquely derivable by the written role name; to-one only (the
           v0 resolver's NAAR_VEEL marker, lifted); the characteristic
           lives on the REACHED type *)
      (match navigatie with
       | None -> (
           match navigatie_binding with
           | Some _ ->
               [ v "WF-053" at
                   "navigation binding on a characteristic check that does not navigate" ]
           | None -> [])
       | Some nav -> (
           match subject with
           | None -> []
           | Some st -> (
               let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
               let hop =
                 match navigatie_binding with
                 | Some rb -> (
                     match resolve_binding ctx rb with
                     | Error e -> Error [ e ]
                     | Ok (_, bound, opposite) ->
                         let near =
                           if not (fills ctx ~inst:st ~role:opposite) then
                             [ Printf.sprintf
                                 "the near side of '%s' fills '%s', but the check's subject \
                                  is '%s'"
                                 rb.rb_feittype (filler opposite) st ]
                           else []
                         in
                         let name_check =
                           let denotes =
                             (match bound.rol_naam with
                              | Some n -> norm n = norm nav
                              | None -> false)
                             || match bound.rol_meervoud with
                                | Some m -> norm m = norm nav
                                | None -> false
                           in
                           if denotes then []
                           else
                             [ Printf.sprintf
                                 "the written navigation '%s' does not denote the bound role \
                                  '%s'"
                                 nav
                                 (Option.value bound.rol_naam ~default:"?") ]
                         in
                         if near @ name_check = [] then Ok bound
                         else Error (near @ name_check))
                 | None -> (
                     match nav_role_by_name ctx ~from_type:st ~name:nav with
                     | Ok (_, r) -> Ok r
                     | Error e ->
                         (* the ch. 4.1a zero-hop self-reference is scoped
                            to the attribute read; a self-denoting name on
                            the navigated check stays rejected, explicitly *)
                         if
                           nav_roles_by_name ctx ~from_type:st ~name:nav = []
                           && self_denoting_role ctx ~otype:st nav
                         then
                           Error
                             [ e ^ "; '" ^ nav ^ "' names a role '" ^ st
                               ^ "' itself fills — the zero-hop self-reference does not \
                                  extend to the characteristic check" ]
                         else Error [ e ])
               in
               match hop with
               | Error msgs -> List.map (fun m -> v "WF-053" at m) msgs
               | Ok r ->
                   (if r.rol_meervoudig then
                      [ v "WF-053" at
                          (Printf.sprintf
                             "the navigated check reaches the to-many role '%s' — a to-many \
                              step needs an aggregation, never a silent first pick"
                             (Option.value r.rol_naam ~default:"?")) ]
                    else [])
                   @
                   match kenmerk with
                   | Some k -> (
                       match objecttype ctx (filler r) with
                       | Some ot when not (has_kenmerk ot k) ->
                           [ v "WF-034" at
                               ("'" ^ k ^ "' is not a declared characteristic of '" ^ filler r
                              ^ "'") ]
                       | _ -> [])
                   | None -> [])))
  | Rollidmaatschap { ander_te_filler; _ } -> (
      (* WF-034 (widened, maintainer decision 2026-09-04, fifth wave): the
         near-side role check ("hij is een <rol>") resolves through
         `anderTeFiller`, not through the written role name — the executors
         take the connecting role from the SUBJECT to that type and null-test
         it (ch. 5.8a), so the written `rol` is presentation and this field is
         the whole of the reference. Nothing crossed it. Measured on all four
         golden positions (`jutterswet-wachtindeling` 2, `jutterswet-wachtploeg`
         2) with the field pointed at an undeclared type: 4/4 verifier-CLEAN,
         while both OCaml executors refuse loudly with the same sentence —
         `ref` "EVALUATION FAULT: no fact type relates \"Jutter\" to
         \"Bestaatniet\"", clingo the same as a TRANSLATION FAULT — and the
         bean-model executor takes a third road again: it stops carrying the
         rule (a transpiler skip, "voorwaarde buiten Phase-B scope"), so the
         document neither faults nor answers.

         One cross, against the role fillers: the ch. 4.3 candidate list
         [connecting_role_candidates], the executors' own derivation. Empty is
         this arm's report — it is exactly `ref`'s `[]` fault. A candidate
         list with SEVERAL entries is a different defect (ambiguity, ch. 4.1)
         and stays WF-048's business; a subject the walk does not know keeps
         the arm silent.

         The EMPTY half is its own arm (maintainer decision 2026-09-05, sixth
         wave; IR-SPEC ch. 4.6 (o), its empty half) and needs no subject: with
         the field absent there is no type to derive a connecting role TO, and
         the written `rol` being presentation, the node then carries no
         reference at all. Carried by 1 of the 10 role checks the four zones
         hold on 2026-09-05, and that one is a day-kind condition
         (`wf031-dagsoort-leest-rolstaat`) that WF-031 already refuses
         outright — a day-kind voorwaarde may not read role state at all,
         which is precisely why no subject was there to derive the filler
         from. So no legitimate document carries the shape; the one that does
         keeps its own verdict and gains a truthful incidental line. *)
      match (ander_te_filler, subject) with
      | Some f, Some st when connecting_role_candidates ctx ~from_type:st ~to_type:f = [] ->
          [ v "WF-034" (sub "anderTeFiller")
              (Printf.sprintf
                 "no fact type relates the subject '%s' to '%s': the role check's anderTeFiller \
                  resolves against no declaration"
                 st f) ]
      | None, _ ->
          [ v "WF-034" (sub "anderTeFiller")
              "role check without an anderTeFiller: the connecting role is derived from that \
               type and from nothing else, so the check resolves against no declaration" ]
      | _ -> [])
  | Verre_rollidmaatschap _ -> []
  | Periode_conditie { vanaf; tot; _ } ->
      (if vanaf = None && tot = None then
         [ v "WF-042" at "time window without any bound" ]
       else [])
      @ List.concat_map
          (fun (n, e) ->
            match e with
            | Some e ->
                (match typ_of_expr e with
                 | Some d when not (is_date d) ->
                     [ v "WF-042" (sub n) ("window bound is " ^ datatype_kind d ^ ", must be a date") ]
                 | _ -> [])
                @ walk_expr ctx ~binders ~subject (sub n) e
            | None -> [])
          [ ("vanaf", vanaf); ("tot", tot) ]
  | Samengesteld { kwantor; drempel; onderdelen; _ } ->
      (match kwantor with
       | Ten_minste | Ten_hoogste | Precies when drempel < 1 ->
           [ v "WF-041" at (Printf.sprintf "counting quantifier with threshold %d < 1" drempel) ]
       | _ -> [])
      @ List.concat
          (List.mapi
             (fun i c -> walk_cond ctx ~binders ~subject (Printf.sprintf "%s/onderdelen/%d" at i) c)
             onderdelen)
  | Regelversie _ -> [] (* row-30 split; admissibility OPEN *)
  | Conditie_onopgelost _ -> []

let expression_layer ctx =
  List.concat_map
    (fun (i, r) ->
      let at = Printf.sprintf "/resolvedRegels/%d" i in
      let subject =
        match r.doel with
        | Some (Attribuut_ref { objecttype; _ }) -> objecttype
        | _ -> None
      in
      (* The target lvalue is WF-022's job, not part of the WF-034 walk —
         which left the target's own [type] the one position in a document
         where the v0 unknown sentinel survived (the walk refuses it on
         every value node it enters, WF-003 on every declaration). Nobody
         READS the field, but the identity family crosses it (WF-091's
         rule-target arm) and defers on the sentinel, so writing it there
         switched the unit and domain checks off one's own write targets.
         WF-034 covers that one position too (maintainer decision
         2026-09-04).

         Both spellings of the unknown type, since the second wave of the
         same decision: the explicit sentinel and its UNSPELLED TWIN, a
         target with no type at all. The first wave left the twin out for
         want of a witness; it has one now, and it is the worse of the two —
         besides WF-091's rule-target arm it also switches off WF-007's
         target clause below (a shape law that reads `Some d`), and the
         document it produces replays clean in ref and clingo while the
         Drools backend throws on it. Measured before this arm: all 295
         golden rule-target types stay verifier-clean when emptied. *)
      (match r.doel with
      | Some (Attribuut_ref { typ = Some (Datatype_onbekend _); _ }) ->
          [ v "WF-034" (at ^ "/doel/type") "unknown-type sentinel on a rule target" ]
      | Some (Attribuut_ref { typ = None; _ }) ->
          [ v "WF-034" (at ^ "/doel/type") "rule target without a static type" ]
      | _ -> [])
      (* WF-007 at the same position, and for a different reason than its
         neighbour above: the precision form is a SHAPE law on a datatype —
         every Number in the document — not a cross against a declaration,
         so nothing else can stand in for it. Its two halves reached the
         declarations ([wf007_model]) and the types the walk enters (the
         prologue), which left the target out on the lvalue argument.
         Measured 2026-09-03: whole + 2 decimals passes here and is refused
         on every walked expression type. Where the target's declaration
         resolves the identity family masks the shape incidentally (a facet
         mismatch, WF-091's number and a comparison message); on a
         CHARACTERISTIC target it defers by construction and this clause is
         the only guard. The sentinel and an absent type fall through it —
         [check_number_form] has nothing to say about either. *)
      @ (* WF-092 rides the same lvalue position for the same reason: the
           unit's conversion pair is a SHAPE law on every unit in the
           document, and the target's own `type` is the one datatype the
           expression walk never enters. *)
      (match r.doel with
        | Some (Attribuut_ref { typ = Some d; _ }) ->
            check_number_form (at ^ "/doel/type") d
            @ check_datatype_units ~stelsels:(stelsel_namen ctx) (at ^ "/doel/type") d
        | _ -> [])
      @ (* WF-056 (rule level): a pro-rata value on a temporal target MUST
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
                  [ v wf (at ^ "/waarde")
                      (what ^ " bucket granularity differs from the target's declared granularity")
                  ]
              | _ -> [])
          | None -> [])
      | _ -> [])
      (* WF-058 (rule level): the day count MUST carry a gedurende
         condition over an own temporal characteristic and takes no
         arguments (the counting fold has no base) *)
      @ (match r.waarde with
        | Some (Functie ({ kind = Aantal_dagen_in; _ } as f)) ->
            (if f.argumenten <> [] then
               [ v "WF-058" (at ^ "/waarde/argumenten") "aantal-dagen-in takes no arguments" ]
             else [])
            @ (match f.filter with
              | None -> [ v "WF-058" (at ^ "/waarde") "aantal-dagen-in without a gedurende condition" ]
              | Some (Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ }) -> (
                  match Option.bind subject (objecttype ctx) with
                  | Some otd
                    when not
                           (List.exists
                              (fun (n, _) -> norm n = norm k)
                              otd.kenmerk_granulariteit) ->
                      [ v "WF-058" (at ^ "/waarde/filter")
                          ("gedurende over '" ^ k ^ "', which is not a temporal characteristic")
                      ]
                  | _ -> [])
              | Some _ ->
                  [ v "WF-058" (at ^ "/waarde/filter")
                      "the day-count gedurende condition is an own temporal characteristic check"
                  ])
        | _ -> [])
      @ (match r.waarde with Some e -> walk_expr ctx ~binders:[] ~subject (at ^ "/waarde") e | None -> [])
      @ (match r.rr_voorwaarde with
         | Some c -> walk_cond ctx ~binders:[] ~subject (at ^ "/voorwaarde") c
         | None -> []))
    (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_regels)
  @ List.concat_map
      (fun (i, r) ->
        let at = Printf.sprintf "/objectCreaties/%d" i in
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
                     (Printf.sprintf "%s/toekenningen/%d/waarde" at j) e
               | None -> [])
             r.toekenningen)
        @ (match r.oc_voorwaarde with
           | Some c -> walk_cond ctx ~binders:[] ~subject:r.oc_subject (at ^ "/voorwaarde") c
           | None -> [])
        @ (match r.oc_verbindende_rol with
           | Some rb ->
               (* the back-link binding (extension): the bound role is
                  the created side, the opposite the subject side *)
               wf047_binding ctx (at ^ "/verbindendeRol") rb ~bound_filler:r.nieuw_type
                 ~other_filler:r.oc_subject
           | None -> []))
      (List.mapi (fun i r -> (i, r)) ctx.doc.object_creaties)
  @ List.concat_map
      (fun (i, r) ->
        match r.vd_voorwaarde with
        | Some c ->
            walk_cond ctx ~binders:[] ~subject:r.bron_type
              (Printf.sprintf "/verdelingen/%d/voorwaarde" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.verdelingen)
  @ List.concat_map
      (fun (i, r) ->
        match r.assertie with
        | Some c ->
            walk_cond ctx ~binders:[] ~subject:r.cons_subject
              (Printf.sprintf "/consistenties/%d/assertie" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.consistenties)
  @ List.concat_map
      (fun (i, r) ->
        match r.rd_voorwaarde with
        | Some c ->
            walk_cond ctx ~binders:[] ~subject:None
              (Printf.sprintf "/resolvedDagsoorten/%d/voorwaarde" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)
  @ List.concat_map
      (fun (i, r) ->
        match r.fc_voorwaarde with
        | Some c ->
            walk_cond ctx ~binders:[] ~subject:r.anker
              (Printf.sprintf "/feitCreaties/%d/voorwaarde" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.feit_creaties)
  @ List.concat_map
      (fun (i, (l : prioritized_defaults)) ->
        (* the expression walk descends into sub-defaults (ch. 7.9): a
           sub-default's justification and consequence are checked exactly as
           a root default's, the exception group inheriting the root subject *)
        let rec default_walk prefix (r : default) =
          (match r.default_voorwaarde with
          | Some c ->
              walk_cond ctx ~binders:[] ~subject:(Some l.pd_onderwerp)
                (prefix ^ "/voorwaarde") c
          | None -> [])
          @ (match r.default_consequentie with
            | Some e ->
                walk_expr ctx ~binders:[] ~subject:(Some l.pd_onderwerp)
                  (prefix ^ "/consequentie") e
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
                                (Printf.sprintf "%s/excepties/leden/%d/defaults/%d" prefix k m)
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
   consumer; a negated/negation-quantified characteristic read and a
   regelversie channel test are negative edges. WF-045: the graph minus
   the creation members of recursive rule groups is acyclic.
   WF-046: no cycle in the full graph carries a negative edge.
   Recorded narrowings (ch. 6.1): relation membership CONDITION reads
   are treated as input; bindingless folds/allocations keep the
   population proxy; day-kind definitions contribute no edges. Bound
   relation reads (a RolBinding on a fold, an allocation, or a creation
   hop) are relation-node edges (n_rel). *)

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
   measured 2026-09-12 on a creation whose `nieuwType` is a subtype of its
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
   creations that create it, read by every symbolic hop over it and by
   every fold/allocation that names it in a role binding. Bindingless
   forms keep the population proxy — the recorded narrowing. *)
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
  | Attribuut_ref
      { attribuut = Some a; objecttype = Some ot; typ; navigatie_rol; navigatie_pad;
        navigatie_bindingen; _ } -> (
      match typ with
      | Some (Datatype_onbekend _) ->
          acc (* the fold-source role wart; the fold adds the population read *)
      | _ ->
          (* a navigated read consumes the link set of every hop relation
             (ch. 6.1) — the edge that orders navigation-after-creation.
             The bound hops name their relations; the bindingless walk is
             derived best-effort (WF-053 reports an unresolvable one, so
             dropping its edges never hides a cycle in a clean document) *)
          let rel_edges =
            match navigatie_bindingen with
            | Some hops -> List.map (fun (rb : rol_binding) -> (n_rel rb.rb_feittype, Dep_pos)) hops
            | None -> (
                match subject with
                | Some st
                  when navigatie_rol <> None || navigatie_pad <> [] || norm st <> norm ot -> (
                    match
                      nav_hops ctx ~subject:st ~bindings:None ~navigatie_rol ~navigatie_pad
                        ~endpoint:(Some ot)
                    with
                    | Ok hops -> List.map (fun (fn, _) -> (n_rel fn, Dep_pos)) hops
                    | Error _ -> [])
                | _ -> [])
          in
          rel_edges @ ((n_attr ot a, Dep_pos) :: acc))
  | Attribuut_ref _ | Literal _ | Parameter_ref _ | Variabele_ref _ | Constante _
  | Expr_onopgelost _ ->
      acc
  | Tekst_reeks { delen; _ } ->
      List.fold_left
        (fun acc -> function
          | Letterlijk _ -> acc
          | Ingevoegd { expr = Some e } -> dep_expr ctx ~subject acc e
          | Ingevoegd { expr = None } -> acc)
        acc delen
  | Binair { links; rechts; _ } -> dep_opt ctx ~subject (dep_opt ctx ~subject acc links) rechts
  | Functie { argumenten; element_objecttype; leegwaarde; filter; bron_rol; bron_pad; _ } ->
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
        | None -> (
            (* the bindingless instance fold's role-wart source names its
               relation by role name — derive it best-effort (the
               navigated-read discipline: WF-034/047 report an
               unresolvable one, so dropping its edge never hides a
               cycle in a clean document) and read its link set beside
               the population proxy *)
            match (element_objecttype, subject, argumenten) with
            | ( Some _, Some st,
                [ Attribuut_ref
                    { attribuut = Some a; typ = Some (Datatype_onbekend _); _ } ] ) -> (
                match nav_role_by_name ctx ~from_type:st ~name:a with
                | Ok (fn, _) -> (n_rel fn, Dep_pos) :: acc
                | Error _ -> acc)
            | _ -> acc)
      in
      let acc = List.fold_left (dep_expr ctx ~subject) acc argumenten in
      let acc = dep_opt ctx ~subject acc leegwaarde in
      (* the filter's reads are the ELEMENT's (ch. 6.1): a filter over a
         derived characteristic of the contributor type must sit above
         that characteristic's writer *)
      let fsubject = match element_objecttype with Some el -> Some el | None -> subject in
      (match filter with Some c -> dep_cond ctx ~subject:fsubject ~neg:false acc c | None -> acc)
  | Afgerond { basis; _ } -> dep_opt ctx ~subject acc basis
  | Begrensd { basis; minimum; maximum; _ } ->
      dep_opt ctx ~subject (dep_opt ctx ~subject (dep_opt ctx ~subject acc basis) minimum) maximum
  | Dimensie_cel { attribuut; _ } -> dep_opt ctx ~subject acc attribuut
  | Temporal_read { basis; datum; _ } ->
      (* the read consumes its timeline reference and its date operand;
         the basis is an attribute/parameter reference whose own case
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
  | Predicaat { operand; _ } -> dep_opt ctx ~subject acc operand
  | Kenmerk_check { kenmerk = Some k; eigenaar; negatie; navigatie = Some nav; navigatie_binding; _ }
    -> (
      (* the navigated check: the characteristic lives on the REACHED
         type, and the hop relation itself is read positively (only the
         characteristic sits under the negation — an absent link makes
         the check false either way, never true). An unresolvable hop
         contributes no edges; WF-053 reports it. *)
      ignore eigenaar;
      let kind = if neg || negatie then Dep_neg else Dep_pos in
      let hop =
        match navigatie_binding with
        | Some rb -> (
            match resolve_binding ctx rb with
            | Ok (_, bound, _) -> Some (rb.rb_feittype, bound)
            | Error _ -> None)
        | None -> (
            match subject with
            | Some st -> (
                match nav_role_by_name ctx ~from_type:st ~name:nav with
                | Ok p -> Some p
                | Error _ -> None)
            | None -> None)
      in
      match hop with
      | Some (fn, r) ->
          (n_rel fn, Dep_pos)
          :: (n_char (Option.value r.rol_objecttype_naam ~default:"") k, kind)
          :: acc
      | None -> acc)
  | Kenmerk_check { kenmerk = Some k; eigenaar; negatie; navigatie = None; _ } -> (
      (* once negative, conservatively negative — double negation does
         not cancel (ch. 6.1) *)
      let kind = if neg || negatie then Dep_neg else Dep_pos in
      match (match eigenaar with Some _ as e -> e | None -> subject) with
      | Some ot -> (n_char ot k, kind) :: acc
      | None -> acc)
  | Kenmerk_check _ -> acc
  | Rollidmaatschap _ | Verre_rollidmaatschap _ -> acc
  | Periode_conditie { vanaf; tot; _ } -> dep_opt ctx ~subject (dep_opt ctx ~subject acc vanaf) tot
  | Samengesteld { kwantor; onderdelen; _ } ->
      let neg' =
        match kwantor with
        | Geen | Ten_hoogste | Precies -> true (* negation-like: conservative, ch. 6.1 *)
        | De | Alle | Ten_minste -> neg
      in
      List.fold_left (fun acc c -> dep_cond ctx ~subject ~neg:neg' acc c) acc onderdelen
  | Regelversie { regel = Some r; _ } -> (n_chan r, Dep_neg) :: acc
  | Regelversie _ -> acc
  | Conditie_onopgelost _ -> acc

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
    List.mapi
      (fun i (r : resolved_regel) ->
        let at = Printf.sprintf "/resolvedRegels/%d" i in
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
    List.mapi
      (fun i (v : resolved_verdeling) ->
        let at = Printf.sprintf "/verdelingen/%d" i in
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
          :: (match v.verbindende_rol with
             | Some rb -> [ (n_rel rb.rb_feittype, Dep_pos) ]
             | None -> [])
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
    List.mapi
      (fun i (c : resolved_consistentie) ->
        let at = Printf.sprintf "/consistenties/%d" i in
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
    List.mapi
      (fun i (u : resolved_uniciteit) ->
        let at = Printf.sprintf "/uniciteiten/%d" i in
        let name = Option.value u.uni_naam ~default:at in
        let subj = Option.value u.uni_subject ~default:"" in
        (* a parent-scoped uniqueness reads the scope relation's link set
           (ch. 6.1) — bound by its named fact type, else derived from
           the written scope field (an unresolvable scope contributes no
           edge; WF-054 reports it) *)
        let scope_read =
          match (u.scope_rol, u.scope_veld) with
          | Some rb, _ -> [ (n_rel rb.rb_feittype, Dep_strict) ]
          | None, Some sv -> (
              match nav_role_by_name ctx ~from_type:subj ~name:sv with
              | Ok (fn, _) -> [ (n_rel fn, Dep_strict) ]
              | Error _ -> [])
          | None, None -> []
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
    List.mapi
      (fun i (u : resolved_uniciteit_unie) ->
        let at = Printf.sprintf "/uniciteitUnies/%d" i in
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
    List.mapi
      (fun i (o : resolved_object_creatie) ->
        let at = Printf.sprintf "/objectCreaties/%d" i in
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
    List.mapi
      (fun i (f : resolved_feit_creatie) ->
        let at = Printf.sprintf "/feitCreaties/%d" i in
        let name = Option.value f.fc_naam ~default:at in
        (* the symbolic form writes the created relation's link set and
           reads the link set of every hop it traverses (ch. 6.1) — the
           edge that orders creation-after-creation; a bean-field-only
           record carries no symbolic hops (recorded uncheckable,
           WF-028) and keeps the bare population write *)
        let rel_writes = match f.fc_feittype with Some ft -> [ n_rel ft ] | None -> [] in
        let hop_reads =
          List.filter_map
            (fun (t : tussenstap) ->
              Option.map (fun (rb : rol_binding) -> (n_rel rb.rb_feittype, Dep_pos)) t.ts_rol)
            f.tussenstappen
          @
          match f.join_rol with
          | Some rb -> [ (n_rel rb.rb_feittype, Dep_pos) ]
          | None -> []
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
                   "negative dependency edge on a cycle: %s reads, negated, what %s produces while %s is reachable from it (no strata exist)"
                   forms.(ci).df_name forms.(pi).df_name forms.(pi).df_name)
              :: !viol46)
        succ.(pi))
    forms;
  List.rev !viol45 @ List.sort_uniq compare (List.rev !viol46)

(* ======================================================================= *)

(* WF-047/048 on the allocation: the recipient-role binding agrees with
   the declared (bronType, doelType); a bindingless allocation must be
   uniquely derivable. *)
let wf047_048_verdelingen ctx =
  List.concat
    (List.mapi
       (fun i (r : resolved_verdeling) ->
         let at = Printf.sprintf "/verdelingen/%d" i in
         match r.verbindende_rol with
         | Some rb ->
             wf047_binding ctx (at ^ "/verbindendeRol") rb ~bound_filler:r.doel_type
               ~other_filler:r.bron_type
         | None -> (
             match (r.bron_type, r.doel_type) with
             | Some bt, Some dt ->
                 wf048_derivable ctx at ~from_type:bt ~to_type:dt ~site:"allocation"
             | _ -> []))
       ctx.doc.verdelingen)

(* WF-028 over the symbolic creation form (v1 extension). The form is
   all-or-nothing, every binding resolves, the ends agree, and the join chain
   reaches the target through a relation that differs from the created one.

   The NO-symbolic-field case (maintainer decision 2026-09-04, fifth wave).
   Until this wave a record carrying only the v0 bean fields — `zetVeld`,
   `joinVeld`, `tussenstappen[].veld/type` and nothing symbolic — fell through
   this clause as "uncheckable, its relational content is not on the wire".
   That was a mis-reading of what the rule is for: the content is not on the
   wire, and THAT is the defect. Measured on `jutterswet-postwacht` with all
   five extension keys removed (`feittype`, `ankerRol`, `doelRol`, `joinRol`
   and the hop's `rolBinding`), the three executors disagree about whether the
   document exists at all:

     verifier  PASS
     ref       EVALUATION FAULT: fact creation "…" carries only bean fields;
               the symbolic form (IR-SPEC ch. 3.4) is what executes
     clingo    TRANSLATION FAULT: the same sentence
     Drools    PLAYED 1 — it reads exactly those bean fields, so it runs

   Two executors refuse the document and the third replays it to the pinned
   expectations. A verifier that admits it makes the IR's meaning depend on
   which engine reads it, which is the one thing an engine-neutral IR may not
   do. The arm says the ch. 3.4 requirement out loud instead: the symbolic
   form is what executes, so a creation must carry it. Its witness is
   `corpus/negatief/wf028-bean-only-creation`; over the four zones no document
   loses it — measured at the pre-card state, all 8 fact creations in golden,
   rood, negatief and the parity zone carried the symbolic form; of the 16
   those zones hold since (the fifth wave's four witnesses, its WF-028 witness
   and the fix round's three), 15 do, and the sixteenth is this arm's own. *)
let wf028 ctx =
  List.concat
    (List.mapi
       (fun i (fc : resolved_feit_creatie) ->
         let at = Printf.sprintf "/feitCreaties/%d" i in
         let any_symbolic =
           fc.fc_feittype <> None || fc.anker_rol <> None || fc.doel_rol <> None
           || fc.join_rol <> None
           || List.exists (fun (t : tussenstap) -> t.ts_rol <> None) fc.tussenstappen
         in
         if not any_symbolic then
           [ v "WF-028" at
               "fact creation carries only bean fields; the symbolic form (ch. 3.4) is what \
                executes, and nothing on the wire says which relation is created" ]
         else
           let missing =
             List.filter_map
               (fun (n, p) -> if p then None else Some n)
               [ ("feittype", fc.fc_feittype <> None);
                 ("ankerRol", fc.anker_rol <> None);
                 ("doelRol", fc.doel_rol <> None);
                 ("joinRol", fc.join_rol <> None) ]
             @ List.concat
                 (List.mapi
                    (fun j (t : tussenstap) ->
                      if t.ts_rol = None then
                        [ Printf.sprintf "tussenstappen/%d/rolBinding" j ]
                      else [])
                    fc.tussenstappen)
           in
           if missing <> [] then
             [ v "WF-028" at
                 ("symbolic fact creation is all-or-nothing; missing: "
                 ^ String.concat ", " missing) ]
           else
             let ftn = Option.get fc.fc_feittype in
             let anker_rol = Option.get fc.anker_rol in
             let doel_rol = Option.get fc.doel_rol in
             let join = Option.get fc.join_rol in
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
                       (* ONE-SIDED. [anker]/[doel] name the types the
                          creation ranges over and targets; read
                          extensionally those are every instance ⊑ the name,
                          and the created relation's role must be able to
                          hold all of them — so the node's type sits AT OR
                          BELOW the role's declared filler, never above it.
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
                          [ v "WF-028" at "ankerRol and doelRol must be distinct roles" ]
                        else [])
                       @ end_check "ankerRol" anker_rol fc.anker
                       @ end_check "doelRol" doel_rol fc.fc_doel
                   | _ ->
                       [ v "WF-028" at
                           ("created fact type '" ^ ftn ^ "' does not have exactly two roles") ])
             in
             let chain_checks =
               (* anchor -[tussenstappen]-> ... -[joinRol]-> target; each
                  binding names the role the hop's far side fills *)
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
                     match step (at ^ "/joinRol") cur join with
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
                     match
                       step (Printf.sprintf "%s/tussenstappen/%d" at j) cur (Option.get t.ts_rol)
                     with
                     | Error vs -> vs
                     | Ok reached -> go (j + 1) reached rest)
               in
               go 0 fc.anker fc.tussenstappen
             in
             created_checks @ chain_checks)
       ctx.doc.feit_creaties)

(* WF-034 on the four BEAN-field names of a fact creation (maintainer decision
   2026-09-04, fifth wave): `zetVeld`, `joinVeld`, `tussenstappen[].veld` and
   `tussenstappen[].type`. These are the v0 half of the node — the fields the
   bean-model executor navigates and writes — and until this clause not one of
   them was held against a declaration. WF-028 above crosses only the symbolic
   sisters (`feittype`, `ankerRol`, `doelRol`, `joinRol`,
   `tussenstappen[].rolBinding`); the bean fields sat beside them, unchecked,
   spelling whatever the producer liked.

   What they mean, and therefore what they resolve against. A binary relation
   lives in the v0 bean model as ONE SCALAR reference field, on the object type
   that fills the TO-MANY role, named after the TO-ONE role. That is the
   §13.3.9 storage direction: the Drools backend's `FactModelGenerator` derives
   exactly that field and its transpiler cites the same law by name in its own
   skip reasons ("§13.3.9 opslagrichting"). So each bean field belongs to a
   relation its symbolic sister already names —

     zetVeld                the created relation      -> a role of `feittype`
     joinVeld               the final hop's relation  -> a role of `joinRol`'s
     tussenstappen[j].veld  hop j's relation          -> a role of that
                                                         `rolBinding`'s

   — and three arms hold it there, in cascade so each is separately
   falsifiable (an undeclared name would otherwise trip A and B at once, and
   no single mutant would free its witness):

     A  the name is a DECLARED role of that relation
     B  where the relation carries exactly ONE to-one role, it is THAT role
     C  it is that role's DECLARED SPELLING, byte for byte

   Arm B stops where the derivation stops (fix round, 2026-09-05). At EQUAL
   cardinality the model derives no side — two to-many roles imply no scalar
   field at all, two to-one roles imply one on each bean — and there both names
   are admissible. That is not a hedge but the corpus: `anchor/jutterswet-
   erflijn` creates `erflijn`, whose roles are both to-many, and
   `anchor/jutterswet-gemeenteberging` joins over `aanmelding`, likewise; a
   rule that picked a side there would make two anchor goldens ill-formed for a
   choice no model makes. The fifth wave admitted BOTH roles everywhere, with
   the reason "which side the bean model stores is a bean-model decision the
   wire does not carry" — refuted by the backend in this repository, which
   derives the side from cardinality alone. Its corpus evidence was also
   mis-counted: read PER CREATION, two of the four golden creations spell
   `zetVeld` with the anchor's role (`jutterswet-postwacht`,
   `anchor/jutterswet-gemeenteberging`) and two with the target's
   (`anchor/jutterswet-erflijn` carries two creations, both target-spelled) —
   two and two, not three and one. The anchor/target framing predicts nothing
   anyway: `jutterswet-postwacht` spells `joinVeld` with the OPPOSITE of its
   bound role, and it is precisely the to-one role. The to-one reading explains
   all eleven golden positions at once.

   Arm C is the one place in this rule where the ch. 2.4 case-insensitive name
   identity is not enough, and the reason is not a naming rule but what the
   name becomes: a bean-model executor turns the role name into a Java PROPERTY
   name and reaches it by getter, and that lookup is case-sensitive (measured:
   an internal capital throws "geen getter voor 'seinVak'"; a first-letter
   change is harmless, because the identifier mapping lower-cases the initial).
   Deliberate asymmetry: `tussenstappen[j].type` gets NO byte arm, because its
   consumer resolves object types with `equalsIgnoreCase` by documented
   contract — a byte arm there would be stricter than all three executors and
   would have no witness.

   The EMPTY half of the same class is one list-driven presence arm over the
   four (path, field, value) triples rather than four sub-arms: with one
   witness, three of four sub-arms move no document at all (measured, 0 of
   503), and a clause with no counterexample is not a clause. All four absences
   were measured on `jutterswet-postwacht`: `tussenstappen[].veld` and
   `tussenstappen[].type` crash the bean-model executor with exactly the
   exceptions the two filled-name witnesses already pin, `zetVeld` and
   `joinVeld` drop the whole creation as a loud transpiler skip.

   `tussenstappen[j].type` is not a role name but the intermediate object type
   the hop lands on, so it crosses against the hop itself: the filler of the
   role that binding binds, the same [resolve_binding] the chain walk in
   WF-028 uses. Where the binding does not resolve, this arm stays silent —
   that report is WF-028's.

   Measured, one field at a time set to a name no declaration carries, over
   all 14 golden positions (4 `zetVeld`, 4 `joinVeld`, 3 `type`, 3 `veld`,
   spread over `anchor/jutterswet-erflijn`, `anchor/jutterswet-gemeenteberging`
   and `jutterswet-postwacht`):

     verifier  14/14 PASS
     ref       14/14 clean — replays to the pinned expectations
     clingo    14/14 clean — the same

   Both OCaml executors are BLIND here by construction: they read the symbolic
   fields and never touch the bean fields, so a nonsense name changes no
   answer and there is no divergence to catch. The bean-model executor reads
   them, reflectively, and on `jutterswet-postwacht` (baseline: it plays)
   every one of the four throws it out of the engine:

     zetVeld                IllegalStateException: no getter for 'bestaatniet'
                            on the target fact class (via ConsequenceException)
     joinVeld               ConstraintEvaluationException: Error evaluating
                            constraint '' in  and in more rules
     tussenstappen[].veld   ConstraintEvaluationException, the same
     tussenstappen[].type   NullPointerException: Cannot read field "naam"
                            because "ot" is null

   That is the sharpest shape this rule family has: a verifier-clean document
   that two executors replay without a murmur while the third crashes on it.
   Seven witnesses in all — four on the undeclared name
   (`corpus/negatief/wf034-feitcreatie-onbekend-*`), and, from the fix round,
   `wf034-feitcreatie-zusterrol` (arm B: a DECLARED role of the created
   relation, still a getter that does not exist), `wf034-feitcreatie-
   hoofdletterveld` (arm C) and `wf034-feitcreatie-loos-tussenstapveld` (the
   presence arm) — each arm-exact against the full four-zone sweep.

   Over the four zones the clause costs nothing. Measured at the pre-card
   state: all 8 fact creations in golden, rood, negatief and the parity zone
   spelled every bean field against a declared role. Re-measured over the 45
   bean name positions the 503 documents hold since (11 golden, 3 rood, 28
   negatief, 3 parity): 27 sit on a relation with exactly one to-one role and
   every one names it byte-exactly, 8 sit at equal cardinality where arm B
   stands down, 4 sit on a fact type WF-028 already reports missing, and the
   remaining 6 are this family's own witnesses. Every one of the 13
   `tussenstappen[].type` positions is present. *)
let wf034_feitcreatie_beanvelden ctx =
  List.concat
    (List.mapi
       (fun i (fc : resolved_feit_creatie) ->
         let at = Printf.sprintf "/feitCreaties/%d" i in
         let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
         (* Three arms, in cascade so each is separately falsifiable: an
            UNDECLARED name never reaches the to-one arm (it would trip both
            and no single mutant would free its witness), and a name the
            to-one arm rejects never reaches the spelling arm. *)
         let spells_role where path ftn = function
           | None -> []
           | Some n -> (
               match find_ci ctx.doc.feittypen ftn with
               | None -> [] (* the fact type itself is WF-028's report *)
               | Some (ft : feittype) -> (
                   let declared = some_names (List.map (fun (r : rol) -> r.rol_naam) ft.rollen) in
                   let to_one =
                     List.filter_map
                       (fun (r : rol) -> if r.rol_meervoudig then None else r.rol_naam)
                       ft.rollen
                   in
                   if not (mem_ci declared n) then
                     [ v "WF-034" path
                         (Printf.sprintf
                            "'%s' is not a declared role of '%s': the %s bean field resolves \
                             against no declaration"
                            n ftn where) ]
                   else
                     match to_one with
                     | [ one ] when norm n <> norm one ->
                         [ v "WF-034" path
                             (Printf.sprintf
                                "'%s' is a to-many role of '%s': the %s bean field names the side \
                                 the v0 bean model stores the reference under, so it must name \
                                 '%s'"
                                n ftn where one) ]
                     | _ ->
                         if List.mem n declared then []
                         else
                           [ v "WF-034" path
                               (Printf.sprintf
                                  "'%s' is not the declared spelling of the role '%s' of '%s': the \
                                   %s bean field is a bean PROPERTY name, which is case-sensitive"
                                  n
                                  (List.find (fun d -> norm d = norm n) declared)
                                  ftn where) ]))
         in
         (* The EMPTY half of the same class, as ONE list-driven arm over the
            four (path, field, value) triples: four separate sub-arms would
            need four witnesses to be falsifiable, and with one witness three
            of them move no document at all. *)
         let present =
           List.filter_map
             (fun (path, where, value) ->
               if value = None then
                 Some
                   (v "WF-034" path
                      (Printf.sprintf
                         "the %s bean field is absent: the v0 half of a fact creation is what a \
                          bean-model executor navigates and writes, and an absent field names \
                          nothing"
                         where))
               else None)
             ([ (at ^ "/zetVeld", "created relation's", fc.zet_veld);
                (at ^ "/joinVeld", "final hop's", fc.join_veld) ]
             @ List.concat
                 (List.mapi
                    (fun j (t : tussenstap) ->
                      let tat = Printf.sprintf "%s/tussenstappen/%d" at j in
                      [ (tat ^ "/veld", "hop's", t.veld);
                        (tat ^ "/type", "hop's intermediate-type", t.ts_type) ])
                    fc.tussenstappen))
         in
         present
         @ (match fc.fc_feittype with
           | Some ftn -> spells_role "created relation's" (at ^ "/zetVeld") ftn fc.zet_veld
           | None -> [])
         @ (match fc.join_rol with
           | Some rb -> spells_role "final hop's" (at ^ "/joinVeld") rb.rb_feittype fc.join_veld
           | None -> [])
         @ List.concat
             (List.mapi
                (fun j (t : tussenstap) ->
                  let tat = Printf.sprintf "%s/tussenstappen/%d" at j in
                  match t.ts_rol with
                  | None -> []
                  | Some rb ->
                      (* ONE-SIDED, like WF-028's [end_check] above and for
                         the same reason: [tn] is a bean FIELD type and the
                         hop hands it every instance the reached role holds,
                         all of them ⊑ that role's filler. A field typed at
                         a strict SUBTYPE of what the hop reaches cannot
                         hold them; a field typed at an ancestor can. *)
                      (match (resolve_binding ctx rb, t.ts_type) with
                      | Ok (_, bound, _), Some tn
                        when not (is_sub ctx ~sub:(filler bound) ~super:tn) ->
                          [ v "WF-034" (tat ^ "/type")
                              (Printf.sprintf
                                 "the hop over '%s' reaches '%s', but the intermediate bean type \
                                  names '%s'"
                                 rb.rb_feittype (filler bound) tn) ]
                      | _ -> [])
                      @ spells_role "hop's" (tat ^ "/veld") rb.rb_feittype t.veld)
                fc.tussenstappen))
       ctx.doc.feit_creaties)

(* WF-050: defaults-group well-formedness (IR-SPEC ch. 6): at least one
   default; exactly one target kind (characteristic or attribute), resolving
   on the subject; default shapes match the target kind; the initialisation
   default is lowest and unique; the target has no other writer; multiple
   defaults groups on one target only when every member is declared unordered
   (ch. 7.2). *)
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
            (excepties present, no own besluit/consequentie) delegates its
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
                         [ v "WF-050" rat "a characteristic default carries no consequentie" ]
                     | false, Some _, _ ->
                         [ v "WF-050" rat "an attribute default carries no besluit" ]
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
                                    (Printf.sprintf "%s/excepties/leden/%d" rat k)
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
                        (defaults) or explicitly absent ('ongeordend'), never implicit \
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
      (fun (i, (o : resolved_object_creatie)) ->
        let at = Printf.sprintf "/objectCreaties/%d" i in
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
      (List.mapi (fun i o -> (i, o)) ctx.doc.object_creaties)
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
(* WF-079 keys on the declared type too — see the single-writer note at
   wf026. A bare rule on the supertype and a bare rule on the subtype are
   not two writers: the subtype's rule IS its base and replaces the
   inherited one, so each type's effective stack still holds a single bare
   writer and the ch. 7.8 standing norm applies per type, unchanged. *)
let wf079 ctx =
  let has_defaults otn a =
    List.exists
      (fun (l : prioritized_defaults) ->
        match l.pd_attribuut with
        | Some la -> norm l.pd_onderwerp = norm otn && norm la = norm a
        | None -> false)
      ctx.doc.prioritized_defaults
  in
  let writers =
    List.filter_map
      (fun (i, (r : resolved_regel)) ->
        match (r.rr_soort, r.doel, r.rr_voorwaarde) with
        | ( Some (Gelijkstelling | Berekening),
            Some (Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ }),
            None )
          when not (has_defaults otn a) ->
            Some
              ( Option.map strip_version r.rr_naam, norm otn, norm a, r.rr_geldigheid,
                Printf.sprintf "/resolvedRegels/%d" i )
        | _ -> None)
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_regels)
  in
  let rec pairs = function
    | [] -> []
    | (n1, ot1, a1, g1, at1) :: rest ->
        List.filter_map
          (fun (n2, ot2, a2, g2, _) ->
            let same_target = ot1 = ot2 && a1 = a2 in
            let distinct_form =
              match (n1, n2) with Some x, Some y -> norm x <> norm y | _ -> true
            in
            if same_target && distinct_form && validity_overlaps g1 g2 then
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
  pairs writers

(* WF-054: parent-scoped uniqueness (the scope hop, v1 extension). The
   scope role resolves — bound (scopeRol) or uniquely derivable from the
   written scopeVeld name — its near side fills the subject, the written
   name denotes the bound role, and the scope role is to-one (a scope
   parent is one instance; the v0 reference field). A binding on an
   unscoped uniqueness is ill-formed. Scope-less instances do not
   participate at evaluation (RUNTIME-SEMANTICS ch. 5.9). *)
let wf054 ctx =
  List.concat_map
    (fun (i, (r : resolved_uniciteit)) ->
      let at = Printf.sprintf "/uniciteiten/%d" i in
      match (r.scope_veld, r.scope_rol, r.uni_subject) with
      | None, Some _, _ ->
          [ v "WF-054" at "a scope binding on an unscoped uniqueness" ]
      | None, None, _ -> []
      | Some _, _, None -> [] (* WF-030 reports the missing subject *)
      | Some sv, binding, Some subj -> (
          let filler (r : rol) = Option.value r.rol_objecttype_naam ~default:"" in
          let denotes (r : rol) name =
            (match r.rol_naam with Some n -> norm n = norm name | None -> false)
            || match r.rol_meervoud with Some m -> norm m = norm name | None -> false
          in
          let hop =
            match binding with
            | Some rb -> (
                match resolve_binding ctx rb with
                | Error e -> Error [ e ]
                | Ok (_, bound, opposite) ->
                    let near =
                      if not (fills ctx ~inst:subj ~role:opposite) then
                        [ Printf.sprintf
                            "the near side of '%s' fills '%s', but the uniqueness subject is \
                             '%s'"
                            rb.rb_feittype (filler opposite) subj ]
                      else []
                    in
                    let name_v =
                      if denotes bound sv then []
                      else
                        [ Printf.sprintf
                            "the written scope field '%s' does not denote the bound role '%s'"
                            sv
                            (Option.value bound.rol_naam ~default:"?") ]
                    in
                    if near @ name_v = [] then Ok bound else Error (near @ name_v))
            | None -> (
                match nav_role_by_name ctx ~from_type:subj ~name:sv with
                | Ok (_, r) -> Ok r
                | Error e -> Error [ e ])
          in
          match hop with
          | Error msgs -> List.map (fun m -> v "WF-054" at m) msgs
          | Ok r ->
              if r.rol_meervoudig then
                [ v "WF-054" at
                    (Printf.sprintf
                       "the scope role '%s' is to-many — a scope parent is one instance"
                       (Option.value r.rol_naam ~default:"?")) ]
              else []))
    (List.mapi (fun i r -> (i, r)) ctx.doc.uniciteiten)

(* WF-023 (the shallow slice), at every attribute WRITE position — a rule's
   target, an object creation's attribute member, a defaults consequence:
   assignment typing between the target's DECLARED type and the value's
   serialized type — same family (numeric kinds count as one), compatible
   units (a unitless literal adopts), no contradictory sign restriction, and
   the DOMAIN clause: a keuzetype-typed side names the same keuzetype
   (variant and domain, ci, through a ListType element) as the other. The
   domain clause is the write half of WF-089's domain identity — WF-089
   crosses what a reference READS, this crosses what is WRITTEN into the
   declaration it reads — and the two together make ref's "match has no arm"
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
  match (dt, wt) with
  | Datatype_onbekend _, _ | _, Datatype_onbekend _ -> []
  | _ ->
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
  (* position 0, the write TARGET's own annotation: a rule target is a
     reference like any other, so its serialized [type] is a use site of the
     attribute declaration it WRITES and the identity engine crosses it facet
     by facet (WF-091, and WF-089/WF-090 for the domain facet). The
     expression walk deliberately never enters the target — the lvalue is
     WF-022's — which left it the one reference type in a document that
     nothing crossed: the card measured a convertible unit lie there passing
     gate 3 while a consumer honouring the annotation rescales the written
     value. The three positions below cross what is WRITTEN against the same
     declaration; this crosses what the target CLAIMS to be. *)
  List.concat_map
    (fun (i, (r : resolved_regel)) ->
      match r.doel with
      | Some (Attribuut_ref { attribuut; _ } as doel) ->
          use_site_identity
            (Printf.sprintf "/resolvedRegels/%d/doel/type" i)
            ~source:"the attribute declaration it writes"
            ~what:("rule target '" ^ Option.value attribuut ~default:"?" ^ "'")
            ~decl:(resolved_attr_datatype ctx doel)
            ~typ:(typ_of_expr doel)
      | _ -> [])
    (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_regels)
  (* position 1: the rule target — its declared datatype where the target
     resolves (WF-022), its serialized type otherwise *)
  @ List.concat_map
    (fun (i, (r : resolved_regel)) ->
      let at = Printf.sprintf "/resolvedRegels/%d" i in
      match (r.rr_soort, r.doel, r.waarde) with
      | Some (Gelijkstelling | Berekening | Initialisatie), Some doel, Some w -> (
          let dt =
            match resolved_attr_datatype ctx doel with
            | Some d -> Some d
            | None -> typ_of_expr doel
          in
          match (dt, typ_of_expr w) with
          | Some dt, Some wt -> wf023_assignment at ~target:r.doel ~value:r.waarde dt wt
          | _ -> [])
      | _ -> [])
    (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_regels)
  (* position 2: an object creation's attribute member (WF-027 owns the
     characteristic member and the member's existence) *)
  @ List.concat_map
      (fun (i, (r : resolved_object_creatie)) ->
        match Option.bind r.nieuw_type (objecttype ctx) with
        | None -> []
        | Some ot ->
            List.concat_map
              (fun (j, (t : toekenning)) ->
                let tat = Printf.sprintf "/objectCreaties/%d/toekenningen/%d" i j in
                match (t.toe_kenmerk, t.toe_attribuut, t.toe_waarde) with
                | false, Some m, Some w -> (
                    match (attr_of ot m, typ_of_expr w) with
                    | Some (Some dt), Some wt ->
                        wf023_assignment tat ~target:None ~value:(Some w) dt wt
                    | _ -> [])
                | _ -> [])
              (List.mapi (fun j t -> (j, t)) r.toekenningen))
      (List.mapi (fun i r -> (i, r)) ctx.doc.object_creaties)
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
              (match Option.bind r.default_consequentie (fun e -> Option.map (fun wt -> (e, wt)) (typ_of_expr e)) with
              | Some (e, wt) ->
                  wf023_assignment (prefix ^ "/consequentie") ~target:None ~value:(Some e) dt wt
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
                                  (Printf.sprintf "%s/excepties/leden/%d/defaults/%d" prefix k m)
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

(* WF-082: the bronAnnotaties extension field (IR-SPEC ch. 3.1) — the
   `// Bron:` statute annotations on a resolved rule. When present it is a
   non-empty list of non-empty entries that carry no surrounding
   whitespace (the emitter trims and omits blanks; an empty or blank form
   would be a trace of absence, and absence leaves no trace). *)
let wf082 ctx =
  List.concat_map
    (fun (i, (r : resolved_regel)) ->
      let at = Printf.sprintf "/resolvedRegels/%d/bronAnnotaties" i in
      match r.rr_bron_annotaties with
      | None -> []
      | Some [] ->
          [ v "WF-082" at
              "bronAnnotaties is empty — an annotation-free rule omits the field entirely" ]
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
    (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_regels)

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
   [excepties] field carries at least one exception group, and two or more
   groups require the unordered marking — the root regime (WF-050's
   declared-unordered rule) applied at every level; the flag rides the
   excepties object itself. A sub-default carries no initialisation (the
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
                           "a group-default (excepties present, no own decision or \
                            consequence) carries no justification of its own: the exception \
                            groups decide, so a condition here would be silently ignored \
                            (ch. 7.9)" ]
                     else [])
                  @ (match r.default_excepties with
                    | None -> []
                    | Some egs ->
                        (if egs.egs_leden = [] then
                           [ v "WF-075" (rat ^ "/excepties")
                               "the excepties field carries at least one exception group \
                                (ch. 7.9)" ]
                         else [])
                        @ (if List.length egs.egs_leden >= 2 && not egs.egs_ongeordend then
                             [ v "WF-075" (rat ^ "/excepties")
                                 "two or more exception groups must be declared unordered \
                                  (the root regime, ch. 7.9)" ]
                           else [])
                        @ List.concat
                            (List.mapi
                               (fun k (eg : exception_group) ->
                                 walk ~in_sub:true
                                   (Printf.sprintf "%s/excepties/leden/%d" rat k)
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
          check at "object type name" (Some key)
          @ check at "object type name" ot.ot_naam
          @ check at "plural" ot.meervoud
          @ keys at "attribute name" ot.attributen
          @ List.concat_map (fun (_, m) -> check at "attribute plural" m) ot.attribuut_meervoud
          @ List.concat_map (fun k -> check at "characteristic name" k) ot.kenmerken)
        ctx.doc.objecttypen;
      List.concat_map
        (fun (key, (ft : feittype)) ->
          let at = "/feittypen/" ^ key in
          check at "fact type name" (Some key)
          @ check at "fact type name" ft.ft_naam
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
          @ check at "dimension name" d.dim_naam
          @ check at "dimension plural" d.dim_naam_meervoud)
        ctx.doc.dimensies;
      List.concat_map (fun (tl : tijdlijn) -> check "/tijdlijnen" "timeline name" tl.tl_naam)
        ctx.doc.tijdlijnen;
      List.concat_map (fun d -> check "/dagsoorten" "day kind name" d) ctx.doc.dagsoorten;
      List.concat_map
        (fun (_, entries) ->
          List.concat_map (fun (n, _, at) -> check at "form name" n) entries)
        (form_arrays ctx);
      List.concat_map
        (fun (g : resolved_regel_groep) ->
          check "/regelGroepen" "rule group name" g.rgr_naam)
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

   SECOND ARM (maintainer decision 2026-09-09): the same shape one side
   table over. A `kenmerkGranulariteit` entry declares its characteristic
   TIME-DEPENDENT by the presence of its KEY and names the grid in its
   VALUE — and the readers split on exactly that seam. Three of them
   (this file's WF-055 clip check, ref's [kenmerk_temporal], the ASP
   translator's clip arm) decide temporality on KEY EXISTENCE; the shared
   case loader decides on the VALUE. A null value therefore declares a
   characteristic temporal to the clip and non-temporal to the loader.

   Measured 2026-09-09 on `jutterswet-tegenwacht` with
   `kenmerkGranulariteit.aangesteld` set to null and nothing else changed
   (pre-arm verifier md5 5111932e2d1e51de144290ab16abab00): the document
   was schema-valid AND verifier-PASS, and then
     - a case DELIVERING the timeline died at load in both executors with
       "a characteristic timeline on a non-temporal characteristic" — a
       message that contradicts the document it was given;
     - a case OMITTING it ran to completion and answered
       tegenwachtsom = 60 and stilwachtsom = 120 where the golden pins 30
       and 30 — silently wrong, and wrong IDENTICALLY in ref and clingo,
       so no differential gate could see it.
   The schema drops null from the enum in the same change, so the wire and
   the verifier close it together; this arm is what still catches a
   hand-built or future-emitted document that never met the schema.

   THIRD ARM (maintainer decision 2026-09-10): the same shape on the
   ATTRIBUTE side table, `attribuutGranulariteit`. Structurally identical —
   [attr_temporal] above reads KEY EXISTENCE, the shared case loader's
   attribute arm reads the VALUE (`Some (Some gran)`) — but a measurably
   DIFFERENT CLASS of fault, which is why it closes on its own evidence
   rather than on the characteristic table's.

   Measured 2026-09-10 on the pre-arm binary (md5
   f55fdba98ccd5ac68bb99492519aa580), one granularity value nulled and
   nothing else changed. There is no silent wrong answer anywhere; every
   path dies. What is wrong is the DIAGNOSIS.
     - On a pro-rata base (`anchor/jutterswet-borgdeel`, `borgtarief`) the
       document was schema-valid and the verifier caught it — but as
       WF-056, "pro-rata base 'borgtarief' is not a temporal attribute",
       which names a different defect than the one present.
     - On an ordinary clip base (`jutterswet-tegenwacht`,
       `wachtvergoeding per maand`), where WF-056 does not reach, the
       document was schema-valid AND verifier-PASS — silent here — while a
       case DELIVERING the timeline died at load in both executors with "a
       timeline value on a non-temporal attribute", a message that
       contradicts the document that declares the attribute temporal.
     - The same document with the attribute delivered SCALAR — the shape
       the loader does accept — drew two different diagnoses of one
       defect: ref "temporal attribute ... without a granularity", the ASP
       backend "the gedurende clip ... needs the full timeline".
   Closed the same way as arm 2: the schema drops null from this enum too,
   and this arm refuses it for any document that never met the schema. The
   case loader's contradicting message is corrected in the same change —
   with the enum shut that path is reachable only for a hand-built
   document, and then it must name the real reason. Cost: none — 0 of the
   577 documents across the four zones and the parity zone carried a null
   there before this arm (1 on the branch head: this arm's own witness).
   Witness: `corpus/negatief/wf059-attribuut-zonder-granulariteit`. *)
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
        ot.attributen
      @ List.concat_map
          (fun (kn, gran) ->
            match gran with
            | None ->
                [ v "WF-059"
                    ("/objecttypen/" ^ key ^ "/kenmerkGranulariteit/" ^ kn)
                    "characteristic declared time-dependent carries no granularity: the key \
                     makes the clip readers temporal while the case loader reads the value and \
                     refuses the timeline" ]
            | Some _ -> [])
          ot.kenmerk_granulariteit
      @ List.concat_map
          (fun (an, gran) ->
            match gran with
            | None ->
                [ v "WF-059"
                    ("/objecttypen/" ^ key ^ "/attribuutGranulariteit/" ^ an)
                    "attribute declared time-dependent carries no granularity: the key makes \
                     the temporal readers treat it as a timeline while the case loader reads \
                     the value and refuses that timeline" ]
            | Some _ -> [])
          ot.attribuut_granulariteit)
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
   exactly one nullary wire form — the v0 Enumeratie domain — and an
   all-nullary sum is sugar for it: it MUST normalise to an Enumeratie domain
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
                normalise it to an Enumeratie domain carrying those names as its values (ch. 2.4)")
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
           List.concat_map
             (fun (pn, dt) -> enum_use_site ctx (at ^ "/parameters/" ^ pn) dt)
             fd.fd_parameters
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
           List.concat_map
             (fun (pn, dt) -> struct_use_site ctx (at ^ "/parameters/" ^ pn) dt)
             fd.fd_parameters
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
            List.concat_map
              (fun (fname, fdt) ->
                struct_use_site ctx ("/domeinen/" ^ name ^ "/velden/" ^ fname) fdt)
              velden
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
                   List.exists
                     (fun (n, g) -> norm n = norm an && g <> None)
                     ot.attribuut_granulariteit
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
  | Expr_onopgelost _ | Binder_ref _ ->
      acc

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
  | Kenmerk_check _ | Rollidmaatschap _ | Verre_rollidmaatschap _ | Regelversie _
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
              probe P7). The v0 unknown sentinel defers, as under WF-023. *)
           @ (match (typ_of_expr fd.fd_body, fd.fd_result) with
             | Some (Datatype_onbekend _), _ | _, Datatype_onbekend _ | None, _ -> []
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

let all (doc : document) : violation list =
  let ctx = { doc } in
  List.concat
    [ wf001 ctx; wf002 ctx; wf103 ctx; wf104 ctx; wf105 ctx;
      wf094 ctx; wf094_null ctx; wf003 ctx; wf004 ctx; wf005 ctx; wf006 ctx;
      wf007_model ctx; wf009 ctx; wf092 ctx; wf011 ctx; wf012 ctx; wf013 ctx; wf014 ctx;
      wf015 ctx; wf016 ctx; wf017 ctx; wf018 ctx; wf107 ctx; wf020 ctx; wf101 ctx; wf021 ctx;
      wf022 ctx; wf023 ctx; wf024 ctx; wf025 ctx; wf026 ctx; wf027 ctx; wf028 ctx;
      wf029 ctx; wf030 ctx; wf031 ctx; wf085 ctx; wf032 ctx; wf033 ctx;
      wf034_dagsoort_eenheden ctx; wf034_zijtabelsleutels ctx;
      wf034_verdelingen ctx; wf034_verdeling_namen ctx; wf034_verdeling_typen ctx;
      wf034_verdeling_naamresolutie ctx; wf034_objectcreatie_subject ctx;
      wf034_feitcreatie_beanvelden ctx; wf045_046 ctx;
      wf047_048_verdelingen ctx; wf050 ctx; wf051 ctx; wf052 ctx; wf079 ctx; wf082 ctx; wf054 ctx;
      wf057 ctx; wf059 ctx; wf060 ctx; wf087 ctx; wf061 ctx; wf088 ctx; wf065 ctx; wf067 ctx;
      wf071 ctx; wf072 ctx; wf075 ctx; wf086 ctx; expression_layer ctx ]
