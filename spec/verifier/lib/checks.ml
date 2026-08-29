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
   inclusive (IR-SPEC ch. 3.10 Validity). ISO dates compare lexicographically. *)
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
let lexeme_iso_date s =
  let is_leap y = (y mod 4 = 0 && y mod 100 <> 0) || y mod 400 = 0 in
  let days_in_month y m =
    match m with
    | 1 | 3 | 5 | 7 | 8 | 10 | 12 -> 31
    | 4 | 6 | 9 | 11 -> 30
    | 2 -> if is_leap y then 29 else 28
    | _ -> 0
  in
  let digits str = str <> "" && String.for_all (fun c -> c >= '0' && c <= '9') str in
  match String.split_on_char '-' s with
  | [ y; m; d ]
    when String.length y = 4 && String.length m = 2 && String.length d = 2
         && digits y && digits m && digits d ->
      let y = int_of_string y and m = int_of_string m and d = int_of_string d in
      m >= 1 && m <= 12 && d >= 1 && d <= days_in_month y m
  | _ -> false

(* --- document context --------------------------------------------------- *)

type ctx = { doc : document }

let objecttype ctx name = find_ci ctx.doc.objecttypen name

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

let wf017 ctx =
  List.concat_map
    (fun d ->
      match d with
      | Some d when List.mem (norm d) builtin_units ->
          [ v "WF-017" "/dagsoorten"
              ("day-kind '" ^ d ^ "' shadows a built-in calendar unit") ]
      | _ -> [])
    ctx.doc.dagsoorten

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

let wf020 ctx =
  let arrays = form_arrays ctx in
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
                       "rule name '%s' collides with '%s' in %s (unique across all rule forms)"
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
             v "WF-020" at (Printf.sprintf "duplicate rule name '%s' in %s" a arr)))
        @
        let groups = List.sort_uniq compare (List.map (fun (n, _) -> norm (strip_version n)) named) in
        List.concat_map
          (fun g ->
            let members = List.filter (fun (n, _) -> norm (strip_version n) = g) named in
            let bare = List.exists (fun (n, _) -> not (is_versioned n)) members in
            if List.length members > 1 && bare then
              [ v "WF-020" (snd (List.hd members))
                  (Printf.sprintf
                     "form '%s' in %s mixes a bare name with versioned entries" g arr) ]
            else [])
          groups)
      arrays
  in
  cross @ within

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
   catches — so a numeric type also shows its decimals and unit. *)
let datatype_detail dt =
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
  | other -> datatype_kind other

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
        @ match r.rd_naam with
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
   two-role fact types relating [from_type] to [to_type] (either way). *)
let connecting_candidates ctx ~from_type ~to_type =
  List.filter_map
    (fun (fn, (ft : feittype)) ->
      let ot r = Option.value r.rol_objecttype_naam ~default:"" in
      match ft.rollen with
      | [ a; b ] ->
          if
            (norm (ot a) = norm from_type && norm (ot b) = norm to_type)
            || (norm (ot b) = norm from_type && norm (ot a) = norm to_type)
          then Some fn
          else None
      | [ r ] when ft.wederkerig ->
          (* the single reciprocal role relates the type to itself *)
          if norm (ot r) = norm from_type && norm from_type = norm to_type then Some fn
          else None
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
      let ot r = Option.value r.rol_objecttype_naam ~default:"" in
      match ft.rollen with
      | [ a; b ] ->
          (if norm (ot a) = norm from_type && norm (ot b) = norm to_type then [ b ]
           else [])
          @ (if norm (ot b) = norm from_type && norm (ot a) = norm to_type then [ a ]
             else [])
      | [ r ] when ft.wederkerig ->
          if norm (ot r) = norm from_type && norm from_type = norm to_type then [ r ]
          else []
      | _ -> [])
    ctx.doc.feittypen

(* The v0 instance-fold source wart (`CONSTRUCTS.md` row 1): a count/fold
   over a fact-type relation encodes its source as a pseudo-AttribuutRef
   naming a ROLE (usually the plural), typed with the unknown sentinel.
   Legitimate v0; the row-1 split removes the encoding in v1. Such an
   argument is checked here instead of by the generic walk. [element_type]
   is the fold's declared element type — the far end of the narrowed
   type-pair fallback (IR-SPEC ch. 4.3). *)
let aggregation_source_check ctx at ~element_type e =
  (* a fold source never navigates: its relation is the fold's bronRol —
     stray navigation bindings in this slot are ill-formed (WF-053) *)
  (match e with
   | Attribuut_ref { navigatie_bindingen = Some _; _ } ->
       [ v "WF-053" at
           "navigation bindings on a fold source are ill-formed (the fold's relation is \
            bronRol)" ]
   | _ -> [])
  @
  match e with
  | Attribuut_ref { navigatie_pad = _ :: _; _ } ->
      [ v "WF-083" at
          "a fold source never navigates: a non-empty navigatiePad on a fold-source argument \
           is ill-formed (the fold's relation is bronRol)" ]
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn;
                    navigatie_rol; navigatie_pad = []; _ } -> (
      (* the self-denoting fold source (WF-083): navigatieRol here is the
         written form naming the fold subject by its own role name — an
         away-pointing role would be navigation, which a fold source
         never does; the silent fall-through this arm replaces let it
         pass while both executors fault on it *)
      (match navigatie_rol with
       | Some nr when not (self_denoting_role ctx ~otype:otn nr) ->
           [ v "WF-083" at
               ("navigatieRol '" ^ nr ^ "' on a fold source does not denote the fold subject: \
                 no role named so (declared name or plural, ci) is filled by '" ^ otn
                ^ "' itself — a fold source never navigates") ]
       | _ -> [])
      @
      match objecttype ctx otn with
      | None -> [ v "WF-034" at ("object type '" ^ otn ^ "' is not declared") ]
      | Some ot ->
          if has_attr ot a || is_role_name ctx a then []
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
      let agree what (r : rol) = function
        | None -> []
        | Some expected ->
            let filler = Option.value r.rol_objecttype_naam ~default:"" in
            if norm filler = norm expected then []
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
    | Some e -> norm (Option.value r.rol_objecttype_naam ~default:"") = norm e
  in
  let ot r = Option.value r.rol_objecttype_naam ~default:"" in
  List.filter_map
    (fun (fn, (ft : feittype)) ->
      match ft.rollen with
      | [ a; b ] ->
          if norm (ot a) = norm from_type && matches b then Some (fn, b)
          else if norm (ot b) = norm from_type && matches a then Some (fn, a)
          else None
      | [ r ] when ft.wederkerig ->
          if norm (ot r) = norm from_type && matches r then Some (fn, r) else None
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
  let ot r = Option.value r.rol_objecttype_naam ~default:"" in
  let hits =
    List.filter_map
      (fun (fn, (ft : feittype)) ->
        match ft.rollen with
        | [ a; b ] ->
            if norm (ot a) = norm from_type && norm (ot b) = norm to_type then Some (fn, b)
            else if norm (ot b) = norm from_type && norm (ot a) = norm to_type then Some (fn, a)
            else None
        | [ r ] when ft.wederkerig ->
            if norm (ot r) = norm from_type && norm from_type = norm to_type then Some (fn, r)
            else None
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
      let endpoint_check cur =
        match (endpoint, cur) with
        | Some e, Some c when norm c <> norm e ->
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
                    | Some c when norm (filler opposite) <> norm c ->
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
   beyond Vergelijking/Predicaat). A strict Let body admits IfThenElse without
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
  | Let_pos               (* strict Let body: IfThenElse ok, reads open *)

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
   must pass through a strict Let binding first: that is the leeg-barrier that
   makes ref's leeg propagation and clingo's underivability coincide by
   construction, keeping the NAF-free exact-complement branches honest. *)
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
             attribute, field or characteristic read must pass through a strict Let binding first" ]
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
             binders, parameters and literals may be read; route state reads through a strict Let \
             binding)" ]
  in
  walk at c

let rec walk_expr ctx ~binders ~subject ?(body = Rule_pos) at (e : expr) : violation list =
  let sub p = at ^ "/" ^ p in
  let typ_checks =
    match typ_of_expr e with
    | Some (Datatype_onbekend _) ->
        [ v "WF-034" (sub "type") "unknown-type sentinel inside a structured node" ]
    | Some d -> check_number_form (sub "type") d
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
  @
  (let closed =
     (* inside a closed body: the reads that break closure short-circuit,
        citing the host rule (WF-067 for a function body, WF-069 for a
        lambda body). A strict Let body (Let_pos) is NOT closed — its reads
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
      @ List.concat
          (List.mapi
             (fun i a ->
               let p = Printf.sprintf "%s/argumenten/%d" at i in
               match a, f.element_objecttype with
               | Attribuut_ref _, Some el -> aggregation_source_check ctx p ~element_type:el a
               | _ -> walk_expr ctx ~binders ~subject p a)
             f.argumenten)
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
             clip_cond_ok c)
         | _ -> [])
      (* WF-056: the pro-rata fold (tijdsevenredig-deel) MUST carry a
         gedurende condition, and its base MUST be a temporal source as
         coarse as or coarser than the bucket *)
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
             | None -> []))
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
  | Attribuut_ref
      { attribuut; objecttype = otn; navigatie_rol; navigatie_pad; navigatie_bindingen;
        set; _ } -> (
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
               | None -> [])
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
      | None -> [])
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
           @ walk_expr ctx ~binders ~subject (sub "attribuut") inner
       | Some inner -> walk_expr ctx ~binders ~subject (sub "attribuut") inner
       | None -> [])
  | Parameter_ref { parameter; _ } -> (
      match parameter with
      | Some p when find_ci ctx.doc.parameters p = None ->
          [ v "WF-034" at ("reference to undeclared parameter '" ^ p ^ "'") ]
      | _ -> [])
  | Variabele_ref _ -> [] (* binding sites are not serialized in v0 — OPEN *)
  | Constante _ -> []
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
                                    ("argument type " ^ datatype_kind ad
                                   ^ " does not match the declared payload "
                                   ^ datatype_kind pd) ]
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
      let decl =
        match typ_of_expr scrutinee with
        | Some (Sum_type { domein; _ }) -> `Sum (domein, sum_decl ctx domein)
        | Some d -> `NotSum d
        | None -> `NoType
      in
      let scrutinee_checks, ctors =
        match decl with
        | `NoType -> ([ v "WF-063" (sub "scrutinee") "match scrutinee carries no type" ], None)
        | `NotSum d ->
            ([ v "WF-063" (sub "scrutinee")
                 ("match scrutinee is " ^ datatype_kind d ^ ", not a sum type") ], None)
        | `Sum (domein, None) ->
            ([ v "WF-063" (sub "scrutinee")
                 ("match scrutinee names sum-type domain '" ^ domein
                ^ "', which is not a declared sum type") ], None)
        | `Sum (_, Some cs) -> ([], Some cs)
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
                v "WF-063" at ("constructor '" ^ dup ^ "' is matched by more than one arm"))
              (dups_by norm (List.map (fun c -> (c, at)) arm_names))
            @ List.filter_map
                (fun c ->
                  if mem_ci declared c then None
                  else
                    Some
                      (v "WF-063" at
                         ("arm '" ^ c ^ "' is not a declared constructor of the sum type")))
                arm_names
            @ List.filter_map
                (fun d ->
                  if mem_ci arm_names d then None
                  else Some (v "WF-063" at ("constructor '" ^ d ^ "' is not covered by any arm")))
                declared
      in
      (* WF-063: an arm carries a binder iff its constructor is unary *)
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
                            ("nullary constructor '" ^ c ^ "' carries a binder '" ^ b ^ "'") ]
                    | Some _, None ->
                        [ v "WF-063" at ("unary constructor '" ^ c ^ "' carries no binder") ]))
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
                         ("the '" ^ c ^ "' arm body is " ^ datatype_kind bt
                        ^ ", but the match result type is " ^ datatype_kind mt))
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
      | Some (_, bdt) -> (
          (* WF-078: the serialized [type] must not lie about the binder's
             soort — the leaf-ref cross-check WF-023/WF-027 perform for a
             resolved AttribuutRef, applied to a binder's declared datatype.
             Soort only (decimals and units stay uncrossed, exactly as for the
             AttribuutRef cross-check); a binder whose declared type is unknown
             defers, as does a reference carrying no type. *)
          match (bdt, typ) with
          | Some d, Some t when datatype_kind t <> datatype_kind d ->
              [ v "WF-078" (sub "type")
                  (Printf.sprintf "binder reference '%s' is typed %s, but its binder is %s" naam
                     (datatype_kind t) (datatype_kind d)) ]
          | _ -> []))
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
                                 ("field '" ^ fname ^ "' value is " ^ datatype_kind vt
                                ^ ", but the declared field datatype is " ^ datatype_kind fdt) ]
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
                           ("field access type " ^ datatype_kind t
                          ^ " differs from the declared field datatype " ^ datatype_kind fdt) ]
                   | None -> [ v "WF-074" (sub "type") "field access carries no type" ]
                   | _ -> [])))
       | Some d ->
           [ v "WF-074" (sub "base")
               ("field access base is " ^ datatype_kind d ^ ", not a struct value") ]
       | None -> [ v "WF-074" (sub "base") "field access base carries no type" ])
      @ walk_expr ctx ~binders ~subject ~body (sub "base") base
  | Let { typ; binder; value; body = lbody; _ } ->
      (* WF-076: the strict let (IR-SPEC ch. 4.10). The node type equals the
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
         (WF-078); a strict Let body admits IfThenElse (Let_pos) unless already
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
                            ("element type " ^ datatype_kind t ^ " differs from the declared element "
                           ^ datatype_kind ed) ]
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
         widening) strict Let bodies; both branches agree on the node's own
         type. WF-077: the condition-operand discipline holds wherever an
         IfThenElse is admissible. *)
      (match body with
       | Closed_pos _ | Let_pos -> []
       | Rule_pos ->
           [ v "WF-069" at
               "an IfThenElse is admissible only inside a lambda, function or strict let body" ])
      @ wf077_condition (sub "condition") condition
      @ (let branch name e =
           match typ, typ_of_expr e with
           | Some t, Some bt when t <> bt ->
               [ v "WF-069" (sub name)
                   ("the " ^ name ^ " branch is " ^ datatype_kind bt ^ ", but the IfThenElse type is "
                  ^ datatype_kind t) ]
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
                             ("argument type " ^ datatype_kind at'
                            ^ " does not match the parameter datatype " ^ datatype_kind pt) ]
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
                        ("lambda parameter '" ^ pn ^ "' is " ^ datatype_kind pt ^ ", expected "
                       ^ datatype_kind e) ]
                | _ -> [])
              parameters params))
      @ (match result, typ_of_expr lbody with
        | Some r, Some b when r <> b ->
            [ v "WF-070" at
                ("lambda body is " ^ datatype_kind b ^ ", but the fold requires " ^ datatype_kind r) ]
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
       A strict Let body (Let_pos) is open; rule level (Rule_pos) too. *)
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
  | Kenmerk_check { kenmerk; navigatie; periode; navigatie_binding; _ } ->
      (match kenmerk, navigatie, subject with
       | Some k, None, Some otn -> (
           match objecttype ctx otn with
           | Some ot when not (has_kenmerk ot k) ->
               [ v "WF-034" at
                   ("'" ^ k ^ "' is not a declared characteristic of '" ^ otn ^ "'") ]
           | _ -> [])
       | _ -> [])
      @ (match periode, kenmerk, navigatie, subject with
         | Some _, Some k, None, Some otn -> (
             match objecttype ctx otn with
             | Some ot
               when not (List.exists (fun (n, _) -> norm n = norm k) ot.kenmerk_granulariteit) ->
                 [ v "WF-043" at "whole-period qualifier on a non-time-dependent characteristic" ]
             | _ -> [])
         | _ -> [])
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
                           if norm (filler opposite) <> norm st then
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
  | Rollidmaatschap _ | Verre_rollidmaatschap _ -> []
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
      (* The target lvalue is WF-022's job, not part of the WF-034 walk. *)
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
      (fun (i, (l : default_ladder)) ->
        (* the expression walk descends into sub-rungs (ch. 7.9): a
           sub-rung's justification and consequence are checked exactly as
           a root rung's, the sub-ladder inheriting the root subject *)
        let rec rung_walk prefix (r : rung) =
          (match r.rung_voorwaarde with
          | Some c ->
              walk_cond ctx ~binders:[] ~subject:(Some l.dl_onderwerp)
                (prefix ^ "/voorwaarde") c
          | None -> [])
          @ (match r.rung_consequentie with
            | Some e ->
                walk_expr ctx ~binders:[] ~subject:(Some l.dl_onderwerp)
                  (prefix ^ "/consequentie") e
            | None -> [])
          @ (match r.rung_excepties with
            | Some sg ->
                List.concat
                  (List.mapi
                     (fun k (sl : sub_ladder) ->
                       List.concat
                         (List.mapi
                            (fun m sr ->
                              rung_walk
                                (Printf.sprintf "%s/excepties/leden/%d/rungs/%d" prefix k m)
                                sr)
                            sl.sl_rungs))
                     sg.sg_leden)
            | None -> [])
        in
        List.concat
          (List.mapi
             (fun j r -> rung_walk (Printf.sprintf "/defaultLadders/%d/rungs/%d" i j) r)
             l.rungs))
      (List.mapi (fun i l -> (i, l)) ctx.doc.default_ladders)

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
  | Functie { argumenten; element_objecttype; leegwaarde; filter; bron_rol; _ } ->
      let acc =
        match element_objecttype with Some el -> (n_pop el, Dep_pos) :: acc | None -> acc
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
  (* the strict let (Phase 5c): its reads live in the value and the body; the
     binder is lexical, contributing no attribute/parameter edge of its own *)
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
              [ (if r.rr_soort = Some Kenmerktoekenning then n_char ot a else n_attr ot a) ]
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
          (match v.doel_attribuut with Some a -> [ n_attr dt a ] | None -> [])
          @ (match v.restant_attribuut with Some a -> [ n_attr bt a ] | None -> [])
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
          (n_pop nt)
          :: (match (o.oc_verbindende_rol, subject) with
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
          @ List.filter_map
               (fun (t : toekenning) ->
                 Option.map
                   (fun a -> if t.toe_kenmerk then n_char nt a else n_attr nt a)
                   t.toe_attribuut)
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
  let ladders =
    (* ch. 7.4/WF-051: every rung-justification read — and, on an
       attribute ladder, every consequence read (the gevuld gate makes
       it decision-bearing) — is a displacement read: a negative edge
       toward the ladder's write. The initialisation rung's implicit
       input-emptiness justification reads nothing (ch. 7.3). Ch. 7.9:
       every DESCENDANT read (justifications and consequences, recursively
       through sub-rungs) folds into the same root negative edges — so a
       cycle through a sub-rung read is caught by WF-051 unchanged. *)
    List.mapi
      (fun i (l : default_ladder) ->
        let at = Printf.sprintf "/defaultLadders/%d" i in
        let name = Option.value l.dl_naam ~default:at in
        let rec rung_reads (r : rung) =
          (match r.rung_voorwaarde with
          | Some c -> dep_cond ctx ~subject:(Some l.dl_onderwerp) ~neg:false [] c
          | None -> [])
          @ (match r.rung_consequentie with
            | Some e -> dep_expr ctx ~subject:(Some l.dl_onderwerp) [] e
            | None -> [])
          @ (match r.rung_excepties with
            | Some sg ->
                List.concat_map
                  (fun (sl : sub_ladder) -> List.concat_map rung_reads sl.sl_rungs)
                  sg.sg_leden
            | None -> [])
        in
        let reads = List.concat_map rung_reads l.rungs in
        let writes =
          (match l.dl_kenmerk with Some k -> [ n_char l.dl_onderwerp k ] | None -> [])
          @
          match l.dl_attribuut with
          | Some a -> [ n_attr l.dl_onderwerp a ]
          | None -> []
        in
        {
          df_at = at;
          df_name = name;
          df_recursive_creation = false;
          df_writes = writes;
          df_reads = List.map (fun (node, _) -> (node, Dep_neg)) reads;
        })
      ctx.doc.default_ladders
  in
  regels @ verdelingen @ consistenties @ uniciteiten @ uniciteit_unies @ creaties
  @ feit_creaties @ ladders

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

(* WF-028 over the symbolic creation form (v1 extension). A bean-field-only
   v0 record stays uncheckable (its relational content is not on the wire);
   once any symbolic field is present the form is all-or-nothing, every
   binding resolves, the ends agree, and the join chain reaches the target
   through a relation that differs from the created one. *)
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
         if not any_symbolic then []
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
                       let end_check what name expected =
                         match pick name with
                         | Error e -> [ v "WF-028" at e ]
                         | Ok r -> (
                             match expected with
                             | Some t when norm (filler r) <> norm t ->
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
                     | Some cur when norm (filler other) <> norm cur ->
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

(* WF-050: ladder well-formedness (IR-SPEC ch. 6): at least one rung;
   exactly one target kind (characteristic or attribute), resolving on
   the subject; rung shapes match the target kind; the initialisation
   rung is lowest and unique; the target has no other writer; multiple
   ladders on one target only when every member is declared unordered
   (ch. 7.2). *)
let wf050 ctx =
  List.concat
    (List.mapi
       (fun i (l : default_ladder) ->
         let at = Printf.sprintf "/defaultLadders/%d" i in
         let target_name =
           match (l.dl_kenmerk, l.dl_attribuut) with
           | Some k, None -> Some k
           | None, Some a -> Some a
           | _ -> None
         in
         (if l.rungs = [] then [ v "WF-050" at "a ladder carries at least one rung" ] else [])
         @ (match target_name with
           | None ->
               [ v "WF-050" at
                   "a ladder targets exactly one of a characteristic or an attribute" ]
           | Some _ -> [])
         @ (match objecttype ctx l.dl_onderwerp with
           | None ->
               [ v "WF-050" at
                   ("ladder subject '" ^ l.dl_onderwerp ^ "' is not a declared object type") ]
           | Some ot -> (
               (match l.dl_kenmerk with
               | Some k when not (mem_ci (some_names ot.kenmerken) k) ->
                   [ v "WF-050" at
                       ("ladder target '" ^ k ^ "' is not a declared characteristic of '"
                      ^ l.dl_onderwerp ^ "'") ]
               | _ -> [])
               @
               match l.dl_attribuut with
               | Some a when not (has_attr ot a) ->
                   [ v "WF-050" at
                       ("ladder target '" ^ a ^ "' is not a declared attribute of '"
                      ^ l.dl_onderwerp ^ "'") ]
               | _ -> []))
         (* rung shapes per target kind (ch. 7.1); the content-kind and
            justification-presence checks recurse into sub-rungs (ch. 7.9 —
            a sub-ladder inherits the root target kind). A group-rung
            (excepties present, no own besluit/consequentie) delegates its
            decision to its sub-group: it is exempt from the content and
            justification requirements. A contentless rung (neither content
            nor sub-group) is WF-075's concern. *)
         @ (let is_char = l.dl_kenmerk <> None in
            let rec shape prefix (rungs : rung list) =
              List.concat
                (List.mapi
                   (fun j (r : rung) ->
                     let rat = Printf.sprintf "%s/rungs/%d" prefix j in
                     let has_content =
                       r.rung_besluit <> None || r.rung_consequentie <> None
                     in
                     let is_group = r.rung_excepties <> None && not has_content in
                     (* present content must match the target kind *)
                     (match (is_char, r.rung_besluit, r.rung_consequentie) with
                     | true, _, Some _ ->
                         [ v "WF-050" rat "a characteristic rung carries no consequentie" ]
                     | false, Some _, _ ->
                         [ v "WF-050" rat "an attribute rung carries no besluit" ]
                     | _ -> [])
                     @ (match (r.rung_initialisatie, is_group, r.rung_voorwaarde) with
                       | true, _, Some _ ->
                           [ v "WF-050" rat
                               "the initialisation rung carries no written justification (its \
                                justification is input-emptiness, ch. 7.3)" ]
                       | false, false, None when has_content ->
                           [ v "WF-050" rat "a non-initialisation rung carries a justification" ]
                       | _ -> [])
                     @ (match r.rung_excepties with
                       | Some sg ->
                           List.concat
                             (List.mapi
                                (fun k (sl : sub_ladder) ->
                                  shape
                                    (Printf.sprintf "%s/excepties/leden/%d" rat k)
                                    sl.sl_rungs)
                                sg.sg_leden)
                       | None -> []))
                   rungs)
            in
            shape at l.rungs)
         (* the initialisation rung's position is a root-ladder property
            (a sub-rung initialisation is ill-formed, WF-075) *)
         @ List.concat
             (List.mapi
                (fun j (r : rung) ->
                  let rat = Printf.sprintf "%s/rungs/%d" at j in
                  (if r.rung_initialisatie && l.dl_kenmerk <> None then
                     [ v "WF-050" rat
                         "an initialisation rung on a characteristic ladder is meaningless \
                          (characteristics start absent)" ]
                   else [])
                  @
                  if r.rung_initialisatie && j <> List.length l.rungs - 1 then
                    [ v "WF-050" rat "the initialisation rung sits lowest (last)" ]
                  else [])
                l.rungs)
         @ (match target_name with
           | None -> []
           | Some tn -> (
               (* the group key carries the target KIND: an attribute and a
                  characteristic may share a name (WF-002), and then they
                  are two independent targets, not a group *)
               let key =
                 (if l.dl_kenmerk <> None then "K:" else "A:")
                 ^ norm l.dl_onderwerp ^ ":" ^ norm tn
               in
               let group_size =
                 List.length
                   (List.filter
                      (fun (o : default_ladder) ->
                        match
                          (o.dl_kenmerk, o.dl_attribuut)
                        with
                        | Some k, None -> "K:" ^ norm o.dl_onderwerp ^ ":" ^ norm k = key
                        | None, Some a -> "A:" ^ norm o.dl_onderwerp ^ ":" ^ norm a = key
                        | _ -> false)
                      ctx.doc.default_ladders)
               in
               (if group_size > 1 && not l.dl_ongeordend then
                  [ v "WF-050" at
                      ("multiple ladders on target '" ^ l.dl_onderwerp ^ "." ^ tn
                     ^ "' without the declared-unordered marking — priority is explicit \
                        (rungs) or explicitly absent ('ongeordend'), never implicit \
                        (ch. 7.2)") ]
                else [])
               @
               (* the ch. 7.1 single-writer lift: any plain writer beside
                  the ladder would silently defeat its decisions *)
               (if
                  List.exists
                    (fun (r : resolved_regel) ->
                      match r.doel with
                      | Some (Attribuut_ref { attribuut = Some a; objecttype = Some ot; _ }) ->
                          norm ot = norm l.dl_onderwerp
                          && norm a = norm tn
                          &&
                          (match (l.dl_kenmerk, r.rr_soort) with
                          | Some _, Some Kenmerktoekenning -> true
                          | Some _, _ -> false
                          | None, _ -> true)
                      | _ -> false)
                    ctx.doc.resolved_regels
                then
                  [ v "WF-050" at
                      ("a rule also writes ladder target '" ^ l.dl_onderwerp ^ "." ^ tn
                     ^ "' — an unordered plain writer would silently defeat the ladder's \
                        decisions") ]
                else [])
               @ (if
                    List.exists
                      (fun (oc : resolved_object_creatie) ->
                        (match oc.nieuw_type with
                        | Some nt -> norm nt = norm l.dl_onderwerp
                        | None -> false)
                        && List.exists
                             (fun (t : toekenning) ->
                               (match t.toe_attribuut with
                               | Some a -> norm a = norm tn
                               | None -> false)
                               && t.toe_kenmerk = (l.dl_kenmerk <> None))
                             oc.toekenningen)
                      ctx.doc.object_creaties
                  then
                    [ v "WF-050" at
                        ("an object creation also initialises ladder target '"
                       ^ l.dl_onderwerp ^ "." ^ tn
                       ^ "' — a creation-time write would silently defeat the ladder's \
                          decisions (and void the initialisation rung's input-emptiness \
                          justification)") ]
                  else [])
               @
               match l.dl_attribuut with
               | None -> []
               | Some a ->
                   if
                     List.exists
                       (fun (vd : resolved_verdeling) ->
                         (match (vd.doel_type, vd.doel_attribuut) with
                         | Some dt, Some da ->
                             norm dt = norm l.dl_onderwerp && norm da = norm a
                         | _ -> false)
                         ||
                         match (vd.bron_type, vd.restant_attribuut) with
                         | Some bt, Some ra ->
                             norm bt = norm l.dl_onderwerp && norm ra = norm a
                         | _ -> false)
                       ctx.doc.verdelingen
                   then
                     [ v "WF-050" at
                         ("an allocation also writes ladder target '" ^ l.dl_onderwerp ^ "."
                        ^ a ^ "'") ]
                   else []))
         )
       ctx.doc.default_ladders)

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
                    (* the created-side role: the role nt fills in fn;
                       a self-typed two-role pair is role-ambiguous and
                       writes no link (no writer) *)
                    match find_ci ctx.doc.feittypen fn with
                    | Some (ft : feittype) when norm st = norm nt && not ft.wederkerig ->
                        None
                    | Some (ft : feittype) -> (
                        let filler r = Option.value r.rol_objecttype_naam ~default:"" in
                        match
                          List.find_opt (fun (r : rol) -> norm (filler r) = norm nt) ft.rollen
                        with
                        | Some r when not r.rol_meervoudig -> Some (norm fn, at)
                        | _ -> None)
                    | None -> None)
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
   ch. 7.7): a (type, attribute) target that carries no ladder admits at most
   one unconditional, version-free computing rule (Gelijkstelling/Berekening).
   Two or more — the PHASE5D-DIFF §8.2 un-pinnable form — over-determine the
   target with no explicit priority: ref oscillates (no fixpoint, ch. 5.7),
   clingo is silently multi-valued, and the pair falls through every zone.
   Characteristic attributions (Kenmerktoekenning) are exempt — a monotone
   many-grounds-one-truth (the penningrecht pair) — and a ladder on the target
   routes multiplicity through WF-050 instead. Conditional writers (a guarded
   case split) carry their own priority and stay outside: only the bare
   (unconditional) collision is statically over-determined. Mirrors WF-026's
   version-stripped pairing. *)
let wf079 ctx =
  let laddered otn a =
    List.exists
      (fun (l : default_ladder) ->
        match l.dl_attribuut with
        | Some la -> norm l.dl_onderwerp = norm otn && norm la = norm a
        | None -> false)
      ctx.doc.default_ladders
  in
  let writers =
    List.filter_map
      (fun (i, (r : resolved_regel)) ->
        match (r.rr_soort, r.doel, r.rr_voorwaarde) with
        | ( Some (Gelijkstelling | Berekening),
            Some (Attribuut_ref { attribuut = Some a; objecttype = Some otn; _ }),
            None )
          when not (laddered otn a) ->
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
                      "two unconditional computing rules write target (%s, %s) with no ladder — \
                       priority is explicit (a ladder) or explicitly absent, never implicit agenda \
                       order (ch. 7.7)"
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
                      if norm (filler opposite) <> norm subj then
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

(* WF-023 (the shallow slice): assignment typing between the target's and
   the value's serialized types — same family (numeric kinds count as
   one), compatible units (a unitless literal adopts), no contradictory
   sign restriction. The precision gate needs expression-shape analysis
   and stays OPEN (the WF index records the reason). *)
let wf023 ctx =
  ignore ctx;
  let sign_conflict d w =
    match (d, w) with
    | Numeriek { teken = Some (Niet_negatief | Positief); _ },
      Numeriek { teken = Some Negatief; _ }
    | Numeriek { teken = Some Negatief; _ },
      Numeriek { teken = Some (Niet_negatief | Positief); _ } ->
        true
    | _ -> false
  in
  List.concat_map
    (fun (i, (r : resolved_regel)) ->
      let at = Printf.sprintf "/resolvedRegels/%d" i in
      match (r.rr_soort, r.doel, r.waarde) with
      | Some (Gelijkstelling | Berekening | Initialisatie), Some doel, Some w -> (
          match (typ_of_expr doel, typ_of_expr w) with
          | Some dt, Some wt -> (
              match (dt, wt) with
              | Datatype_onbekend _, _ | _, Datatype_onbekend _ -> []
              | _ ->
                  (if
                     datatype_kind dt <> datatype_kind wt
                     && not (is_number_like dt && is_number_like wt)
                   then
                     [ v "WF-023" at
                         (Printf.sprintf "a %s value is assigned to a %s target"
                            (datatype_kind wt) (datatype_kind dt)) ]
                   else [])
                  @ (if
                       is_number_like dt && is_number_like wt
                       && not
                            (units_compatible_adopting r.doel r.waarde (unit_of dt)
                               (unit_of wt))
                     then [ v "WF-023" at "assignment across incompatible units" ]
                     else [])
                  @
                  if sign_conflict dt wt then
                    [ v "WF-023" at
                        "the value's sign restriction contradicts the target's" ]
                  else [])
          | _ -> [])
      | _ -> [])
    (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_regels)

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

(* WF-051: no dependency cycle may pass through a ladder — its
   displacement edges are negative (dep_forms above), so any such cycle
   also violates WF-046; this check names the ladder-specific rule. *)
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
       (fun i (l : default_ladder) ->
         let at = Printf.sprintf "/defaultLadders/%d" i in
         match
           Array.to_list forms
           |> List.mapi (fun fi f -> (fi, f))
           |> List.find_opt (fun ((_ : int), (f : dep_form)) -> f.df_at = at)
         with
         | Some (fi, _) when reaches fi fi ->
             [ v "WF-051" at
                 ("a dependency cycle passes through ladder '"
                 ^ Option.value l.dl_naam ~default:at
                 ^ "' — its displacement reads are negative edges; no strata exist") ]
         | _ -> [])
       ctx.doc.default_ladders)

(* WF-075: the nested exception form (RUNTIME-SEMANTICS ch. 7.9). A rung's
   [excepties] sub-group carries at least one member, and two or more
   members require the unordered marking — the root regime (WF-050's
   declared-unordered rule) applied at every level; the flag rides the
   sub-group object itself. A sub-rung carries no initialisation (the
   initialisation and input rungs are root-only). Every rung carries its
   own decision/consequence, a nested sub-group, or is the initialisation
   rung — a contentless rung decides nothing. Recurses to any depth. (The
   content-KIND per target and the WF-051 descendant-read folding land with
   WF-050 and dep_forms; a differing sub-group's root propagation is
   evaluation semantics, ch. 7.9.) *)
let wf075 ctx =
  List.concat
    (List.mapi
       (fun i (l : default_ladder) ->
         let at = Printf.sprintf "/defaultLadders/%d" i in
         let rec walk ~in_sub prefix (rungs : rung list) =
           List.concat
             (List.mapi
                (fun j (r : rung) ->
                  let rat = Printf.sprintf "%s/rungs/%d" prefix j in
                  let has_content =
                    r.rung_besluit <> None || r.rung_consequentie <> None
                  in
                  (if in_sub && r.rung_initialisatie then
                     [ v "WF-075" rat
                         "a sub-group rung carries no initialisation (the initialisation \
                          and input rungs are root-only, ch. 7.9)" ]
                   else [])
                  @ (if (not has_content) && r.rung_excepties = None
                        && not r.rung_initialisatie
                     then
                       [ v "WF-075" rat
                           "a rung carries its own decision/consequence or a nested \
                            exception sub-group (a contentless rung decides nothing, \
                            ch. 7.9)" ]
                     else [])
                  @ (if r.rung_excepties <> None && (not has_content)
                        && r.rung_voorwaarde <> None && not r.rung_initialisatie
                     then
                       [ v "WF-075" rat
                           "a group-rung (an exception sub-group with no own decision or \
                            consequence) carries no justification of its own: the sub-group \
                            decides, so a condition here would be silently ignored (ch. 7.9)" ]
                     else [])
                  @ (match r.rung_excepties with
                    | None -> []
                    | Some sg ->
                        (if sg.sg_leden = [] then
                           [ v "WF-075" (rat ^ "/excepties")
                               "an exception sub-group carries at least one member (ch. 7.9)" ]
                         else [])
                        @ (if List.length sg.sg_leden >= 2 && not sg.sg_ongeordend then
                             [ v "WF-075" (rat ^ "/excepties")
                                 "an exception sub-group with two or more members must be \
                                  declared unordered (the root regime, ch. 7.9)" ]
                           else [])
                        @ List.concat
                            (List.mapi
                               (fun k (sl : sub_ladder) ->
                                 walk ~in_sub:true
                                   (Printf.sprintf "%s/excepties/leden/%d" rat k)
                                   sl.sl_rungs)
                               sg.sg_leden)))
                rungs)
         in
         walk ~in_sub:false at l.rungs)
       ctx.doc.default_ladders)

(* WF-057: declared names are target-text-safe (IR-SPEC ch. 2). A
   declared name — object type, attribute, characteristic, plural,
   role, fact type, parameter, domain, unit system, dimension,
   timeline, day kind, or any form/group/ladder name — MUST NOT carry
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
        (fun i (l : default_ladder) ->
          check (Printf.sprintf "/defaultLadders/%d" i) "ladder name" l.dl_naam)
        ctx.doc.default_ladders
      |> List.concat ]

(* WF-059: every declared attribute carries a datatype — the attribute
   pendant of WF-018's datatype clause. A datatype-less declaration is
   loadable (the wire admits null) but splits the executors on
   verifier-clean input: ref reads case input against it as a bare
   unitless number while the integer-scaling translation cannot scale
   it. The shared case loader refuses such input as the last-line
   backstop. *)
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

(* WF-072: a StructType use site (an attribute or parameter datatype that is a
   StructType) MUST name a declared domain whose declaration it agrees with —
   same fields, datatypes, order (IR-SPEC ch. 2.4). The nominal-comparison
   half (two struct-typed operands of one comparison name the same domain)
   rides the expression walk, walk_cond. *)
let wf072 ctx =
  let use_site at = function
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

let all (doc : document) : violation list =
  let ctx = { doc } in
  List.concat
    [ wf001 ctx; wf002 ctx; wf003 ctx; wf004 ctx; wf005 ctx; wf006 ctx;
      wf007_model ctx; wf009 ctx; wf011 ctx; wf012 ctx; wf013 ctx; wf014 ctx;
      wf015 ctx; wf016 ctx; wf017 ctx; wf018 ctx; wf020 ctx; wf021 ctx;
      wf022 ctx; wf023 ctx; wf024 ctx; wf025 ctx; wf026 ctx; wf027 ctx; wf028 ctx;
      wf029 ctx; wf030 ctx; wf031 ctx; wf085 ctx; wf032 ctx; wf033 ctx; wf045_046 ctx;
      wf047_048_verdelingen ctx; wf050 ctx; wf051 ctx; wf052 ctx; wf079 ctx; wf082 ctx; wf054 ctx;
      wf057 ctx; wf059 ctx; wf060 ctx; wf061 ctx; wf065 ctx; wf067 ctx;
      wf071 ctx; wf072 ctx; wf075 ctx; expression_layer ctx ]
