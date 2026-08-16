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

(* Numeric literal lexeme sign (Dutch decimal comma; raw source string). *)
let lexeme_negative s =
  let s = String.trim s in
  String.length s > 0 && s.[0] = '-'

let lexeme_zero s =
  let s = String.trim s in
  let s = if String.length s > 0 && (s.[0] = '-' || s.[0] = '+')
          then String.sub s 1 (String.length s - 1) else s in
  s <> "" && String.for_all (fun c -> c = '0' || c = ',' || c = '.') s

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
                        (match t.toe_waarde with
                         | Some (Literal { waarde = Some ("waar" | "onwaar"); typ = Some Booleaan; _ }) -> []
                         | _ ->
                             [ v "WF-027" tat
                                 "characteristic initialisation's value must be a boolean constant" ])
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
  | Enumeratie _ -> "enumeration" | Datatype_onbekend _ -> "unknown"

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
        per_bron @ arity_v @ kind_v)
      (List.mapi (fun i r -> (i, r)) ctx.doc.uniciteit_unies)

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
         | Some _ -> [])
        @ match r.rd_naam with
          | Some n when not (mem_ci (some_names ctx.doc.dagsoorten) n) ->
              [ v "WF-031" at ("definition for undeclared day kind '" ^ n ^ "'") ]
          | _ -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)

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
        if ocs = [] then
          [ v "WF-033" at "recursive group without an object-creation member" ]
        else
          List.concat_map
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

let typ_of_expr = function
  | Literal { typ; _ } | Tekst_reeks { typ; _ } | Binair { typ; _ }
  | Functie { typ; _ } | Afgerond { typ; _ } | Begrensd { typ; _ }
  | Attribuut_ref { typ; _ } | Dimensie_cel { typ; _ } | Parameter_ref { typ; _ }
  | Variabele_ref { typ; _ } | Constante { typ; _ } | Expr_onopgelost { typ; _ } -> typ

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

let scalar_one_arg_kinds =
  [ Wortel; Absolute_waarde; Eerste_paasdag; Jaar_uit; Maand_uit; Dag_uit;
    Uur_uit; Minuut_uit; Seconde_uit; Milliseconde_uit ]

let aggregation_kinds = [ Som; Totaal; Aantal; Minimum; Maximum; Eerste; Laatste ]

let rec walk_expr ctx ~subject at (e : expr) : violation list =
  let sub p = at ^ "/" ^ p in
  let typ_checks =
    match typ_of_expr e with
    | Some (Datatype_onbekend _) ->
        [ v "WF-034" (sub "type") "unknown-type sentinel inside a structured node" ]
    | Some d -> check_number_form (sub "type") d
    | None -> []
  in
  typ_checks
  @
  match e with
  | Literal { waarde; typ; _ } -> (
      match typ, waarde with
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
                 walk_expr ctx ~subject (Printf.sprintf "%s/delen/%d/expr" at i) e
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
         | (Plus | Min), Some l, Some r when is_number_like l && is_number_like r ->
             if not (units_compatible_adopting links rechts (unit_of l) (unit_of r)) then
               [ v "WF-035" at "additive operands have incompatible units" ]
             else []
         | _ -> [])
      @ (match links with Some e -> walk_expr ctx ~subject (sub "links") e | None -> [])
      @ (match rechts with Some e -> walk_expr ctx ~subject (sub "rechts") e | None -> [])
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
         | _ -> [])
      @ (match f.leegwaarde with
         | Some lw -> (
             (match typ_of_expr lw with
              | Some d when not (is_number_like d) ->
                  [ v "WF-038" (sub "leegwaarde")
                      ("sum fallback is " ^ datatype_kind d ^ ", must be numeric") ]
              | _ -> [])
             @ walk_expr ctx ~subject (sub "leegwaarde") lw)
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
         | Some c -> walk_cond ctx ~subject (sub "filter") c
         | None -> [])
      @ List.concat
          (List.mapi
             (fun i a -> walk_expr ctx ~subject (Printf.sprintf "%s/argumenten/%d" at i) a)
             f.argumenten)
  | Afgerond { basis; _ } ->
      (match basis with Some e -> walk_expr ctx ~subject (sub "basis") e | None -> [])
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
                @ walk_expr ctx ~subject (sub n) e
            | None -> [])
          [ ("basis", basis); ("minimum", minimum); ("maximum", maximum) ]
  | Attribuut_ref { attribuut; objecttype = otn; navigatie_rol; navigatie_pad; _ } -> (
      match otn with
      | Some otn -> (
          match objecttype ctx otn with
          | None ->
              [ v "WF-034" at ("object type '" ^ otn ^ "' is not declared") ]
          | Some ot -> (
              (* Only the navigation-free case resolves fully on the dump;
                 navigated references check the endpoint type only. *)
              match attribuut, navigatie_rol, navigatie_pad with
              | Some a, None, [] ->
                  if has_attr ot a || has_kenmerk ot a then []
                  else
                    [ v "WF-034" at
                        ("'" ^ a ^ "' is neither an attribute nor a characteristic of '" ^ otn ^ "'") ]
              | _ -> []))
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
           @ walk_expr ctx ~subject (sub "attribuut") inner
       | Some inner -> walk_expr ctx ~subject (sub "attribuut") inner
       | None -> [])
  | Parameter_ref { parameter; _ } -> (
      match parameter with
      | Some p when find_ci ctx.doc.parameters p = None ->
          [ v "WF-034" at ("reference to undeclared parameter '" ^ p ^ "'") ]
      | _ -> [])
  | Variabele_ref _ -> [] (* binding sites are not serialized in v0 — OPEN *)
  | Constante _ -> []
  | Expr_onopgelost _ -> [] (* honest marker; WF-044 is behavioural *)

and walk_cond ctx ~subject at (c : conditie) : violation list =
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
      @ (match links with Some e -> walk_expr ctx ~subject (sub "links") e | None -> [])
      @ (match rechts with Some e -> walk_expr ctx ~subject (sub "rechts") e | None -> [])
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
      @ (match operand with Some e -> walk_expr ctx ~subject (sub "operand") e | None -> [])
  | Kenmerk_check { kenmerk; navigatie; periode; _ } ->
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
                @ walk_expr ctx ~subject (sub n) e
            | None -> [])
          [ ("vanaf", vanaf); ("tot", tot) ]
  | Samengesteld { kwantor; drempel; onderdelen; _ } ->
      (match kwantor with
       | Ten_minste | Ten_hoogste | Precies when drempel < 1 ->
           [ v "WF-041" at (Printf.sprintf "counting quantifier with threshold %d < 1" drempel) ]
       | _ -> [])
      @ List.concat
          (List.mapi
             (fun i c -> walk_cond ctx ~subject (Printf.sprintf "%s/onderdelen/%d" at i) c)
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
      (match r.waarde with Some e -> walk_expr ctx ~subject (at ^ "/waarde") e | None -> [])
      @ (match r.rr_voorwaarde with
         | Some c -> walk_cond ctx ~subject (at ^ "/voorwaarde") c
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
                   walk_expr ctx ~subject:r.nieuw_type
                     (Printf.sprintf "%s/toekenningen/%d/waarde" at j) e
               | None -> [])
             r.toekenningen)
        @ (match r.oc_voorwaarde with
           | Some c -> walk_cond ctx ~subject:r.oc_subject (at ^ "/voorwaarde") c
           | None -> []))
      (List.mapi (fun i r -> (i, r)) ctx.doc.object_creaties)
  @ List.concat_map
      (fun (i, r) ->
        match r.vd_voorwaarde with
        | Some c ->
            walk_cond ctx ~subject:r.bron_type
              (Printf.sprintf "/verdelingen/%d/voorwaarde" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.verdelingen)
  @ List.concat_map
      (fun (i, r) ->
        match r.assertie with
        | Some c ->
            walk_cond ctx ~subject:r.cons_subject
              (Printf.sprintf "/consistenties/%d/assertie" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.consistenties)
  @ List.concat_map
      (fun (i, r) ->
        match r.rd_voorwaarde with
        | Some c ->
            walk_cond ctx ~subject:None
              (Printf.sprintf "/resolvedDagsoorten/%d/voorwaarde" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.resolved_dagsoorten)
  @ List.concat_map
      (fun (i, r) ->
        match r.fc_voorwaarde with
        | Some c ->
            walk_cond ctx ~subject:r.anker
              (Printf.sprintf "/feitCreaties/%d/voorwaarde" i) c
        | None -> [])
      (List.mapi (fun i r -> (i, r)) ctx.doc.feit_creaties)

(* ======================================================================= *)

let all (doc : document) : violation list =
  let ctx = { doc } in
  List.concat
    [ wf001 ctx; wf002 ctx; wf003 ctx; wf004 ctx; wf005 ctx; wf006 ctx;
      wf007_model ctx; wf009 ctx; wf011 ctx; wf012 ctx; wf013 ctx; wf014 ctx;
      wf015 ctx; wf016 ctx; wf017 ctx; wf018 ctx; wf020 ctx; wf021 ctx;
      wf022 ctx; wf024 ctx; wf025 ctx; wf026 ctx; wf027 ctx; wf029 ctx;
      wf030 ctx; wf031 ctx; wf032 ctx; wf033 ctx; expression_layer ctx ]
