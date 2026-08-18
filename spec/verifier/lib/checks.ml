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
  | Variabele_ref { typ; _ } | Constante { typ; _ } | Expr_onopgelost { typ; _ }
  | Temporal_read { typ; _ } -> typ

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

(* The v0 instance-fold source wart (`CONSTRUCTS.md` row 1): a count/fold
   over a fact-type relation encodes its source as a pseudo-AttribuutRef
   naming a ROLE (usually the plural), typed with the unknown sentinel.
   Legitimate v0; the row-1 split removes the encoding in v1. Such an
   argument is checked here instead of by the generic walk. *)
let aggregation_source_check ctx at = function
  | Attribuut_ref { attribuut = Some a; objecttype = Some otn;
                    navigatie_rol = None; navigatie_pad = []; _ } -> (
      match objecttype ctx otn with
      | None -> [ v "WF-034" at ("object type '" ^ otn ^ "' is not declared") ]
      | Some ot ->
          if has_attr ot a || has_kenmerk ot a || is_role_name ctx a then []
          else
            [ v "WF-034" at
                ("'" ^ a ^ "' is neither a member of '" ^ otn ^ "' nor a declared role") ])
  | _ -> []

(* --- WF-047 / WF-048: symbolic role bindings (v1 extension) -------------- *)

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
             (fun i a ->
               let p = Printf.sprintf "%s/argumenten/%d" at i in
               match a, f.element_objecttype with
               | Attribuut_ref _, Some _ -> aggregation_source_check ctx p a
               | _ -> walk_expr ctx ~subject p a)
             f.argumenten)
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
      @ walk_expr ctx ~subject (sub "basis") basis
      @ walk_expr ctx ~subject (sub "datum") datum

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
  @ List.concat_map
      (fun (i, (l : default_ladder)) ->
        List.concat
          (List.mapi
             (fun j (r : rung) ->
               (match r.rung_voorwaarde with
               | Some c ->
                   walk_cond ctx ~subject:(Some l.dl_onderwerp)
                     (Printf.sprintf "/defaultLadders/%d/rungs/%d/voorwaarde" i j)
                     c
               | None -> [])
               @
               match r.rung_consequentie with
               | Some e ->
                   walk_expr ctx ~subject:(Some l.dl_onderwerp)
                     (Printf.sprintf "/defaultLadders/%d/rungs/%d/consequentie" i j)
                     e
               | None -> [])
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

(* [subject] resolves owner-less characteristic reads *)
let rec dep_expr ~subject acc (e : expr) =
  match e with
  | Attribuut_ref { attribuut = Some a; objecttype = Some ot; typ; _ } -> (
      match typ with
      | Some (Datatype_onbekend _) ->
          acc (* the fold-source role wart; the fold adds the population read *)
      | _ -> (n_attr ot a, Dep_pos) :: acc)
  | Attribuut_ref _ | Literal _ | Parameter_ref _ | Variabele_ref _ | Constante _
  | Expr_onopgelost _ ->
      acc
  | Tekst_reeks { delen; _ } ->
      List.fold_left
        (fun acc -> function
          | Letterlijk _ -> acc
          | Ingevoegd { expr = Some e } -> dep_expr ~subject acc e
          | Ingevoegd { expr = None } -> acc)
        acc delen
  | Binair { links; rechts; _ } -> dep_opt ~subject (dep_opt ~subject acc links) rechts
  | Functie { argumenten; element_objecttype; leegwaarde; filter; bron_rol; _ } ->
      let acc =
        match element_objecttype with Some el -> (n_pop el, Dep_pos) :: acc | None -> acc
      in
      let acc =
        (* a bound fold reads the named relation's link set — the edge a
           fact creation's write reaches, from either end (ch. 6.1) *)
        match bron_rol with
        | Some (rb : rol_binding) -> (n_rel rb.rb_feittype, Dep_pos) :: acc
        | None -> acc
      in
      let acc = List.fold_left (dep_expr ~subject) acc argumenten in
      let acc = dep_opt ~subject acc leegwaarde in
      (match filter with Some c -> dep_cond ~subject ~neg:false acc c | None -> acc)
  | Afgerond { basis; _ } -> dep_opt ~subject acc basis
  | Begrensd { basis; minimum; maximum; _ } ->
      dep_opt ~subject (dep_opt ~subject (dep_opt ~subject acc basis) minimum) maximum
  | Dimensie_cel { attribuut; _ } -> dep_opt ~subject acc attribuut
  | Temporal_read { basis; datum; _ } ->
      (* the read consumes its timeline reference and its date operand;
         the basis is an attribute/parameter reference whose own case
         adds the right edge *)
      dep_expr ~subject (dep_expr ~subject acc basis) datum

and dep_opt ~subject acc = function Some e -> dep_expr ~subject acc e | None -> acc

and dep_cond ~subject ~neg acc (c : conditie) =
  match c with
  | Vergelijking { links; rechts; _ } ->
      dep_opt ~subject (dep_opt ~subject acc links) rechts
  | Predicaat { operand; _ } -> dep_opt ~subject acc operand
  | Kenmerk_check { kenmerk = Some k; eigenaar; negatie; _ } -> (
      (* once negative, conservatively negative — double negation does
         not cancel (ch. 6.1) *)
      let kind = if neg || negatie then Dep_neg else Dep_pos in
      match (match eigenaar with Some _ as e -> e | None -> subject) with
      | Some ot -> (n_char ot k, kind) :: acc
      | None -> acc)
  | Kenmerk_check _ -> acc
  | Rollidmaatschap _ | Verre_rollidmaatschap _ -> acc
  | Periode_conditie { vanaf; tot; _ } -> dep_opt ~subject (dep_opt ~subject acc vanaf) tot
  | Samengesteld { kwantor; onderdelen; _ } ->
      let neg' =
        match kwantor with
        | Geen | Ten_hoogste | Precies -> true (* negation-like: conservative, ch. 6.1 *)
        | De | Alle | Ten_minste -> neg
      in
      List.fold_left (fun acc c -> dep_cond ~subject ~neg:neg' acc c) acc onderdelen
  | Regelversie { regel = Some r; _ } -> (n_chan r, Dep_neg) :: acc
  | Regelversie _ -> acc
  | Conditie_onopgelost _ -> acc

let dep_forms ctx : dep_form list =
  let recursive_members =
    List.concat_map
      (fun g -> if g.recursief then List.map (fun n -> norm (strip_version n)) (some_names g.leden) else [])
      ctx.doc.regel_groepen
  in
  let cond ~subject c = match c with Some c -> dep_cond ~subject ~neg:false [] c | None -> [] in
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
            let reads = dep_opt ~subject (cond ~subject r.rr_voorwaarde) r.waarde in
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
        {
          df_at = at;
          df_name = name;
          df_recursive_creation = false;
          df_writes = [ n_chan name ];
          df_reads =
            List.map (fun k -> (n_attr subj (Option.value k ~default:""), Dep_strict)) u.sleutels;
        })
      ctx.doc.uniciteiten
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
                (fun acc (t : toekenning) -> dep_opt ~subject acc t.toe_waarde)
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
       input-emptiness justification reads nothing (ch. 7.3). *)
    List.mapi
      (fun i (l : default_ladder) ->
        let at = Printf.sprintf "/defaultLadders/%d" i in
        let name = Option.value l.dl_naam ~default:at in
        let reads =
          List.concat_map
            (fun (r : rung) ->
              (match r.rung_voorwaarde with
              | Some c -> dep_cond ~subject:(Some l.dl_onderwerp) ~neg:false [] c
              | None -> [])
              @
              match r.rung_consequentie with
              | Some e -> dep_expr ~subject:(Some l.dl_onderwerp) [] e
              | None -> [])
            l.rungs
        in
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
  regels @ verdelingen @ consistenties @ uniciteiten @ creaties @ feit_creaties @ ladders

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
         (* rung shapes per target kind (ch. 7.1) *)
         @ List.concat
             (List.mapi
                (fun j (r : rung) ->
                  let rat = Printf.sprintf "%s/rungs/%d" at j in
                  (match (l.dl_kenmerk, r.rung_besluit, r.rung_consequentie) with
                  | Some _, None, _ ->
                      [ v "WF-050" rat "a characteristic rung carries a boolean besluit" ]
                  | Some _, _, Some _ ->
                      [ v "WF-050" rat "a characteristic rung carries no consequentie" ]
                  | None, Some _, _ ->
                      [ v "WF-050" rat "an attribute rung carries no besluit" ]
                  | None, _, None when l.dl_attribuut <> None ->
                      [ v "WF-050" rat "an attribute rung carries a consequentie" ]
                  | _ -> [])
                  @ (match (r.rung_initialisatie, r.rung_voorwaarde) with
                    | true, Some _ ->
                        [ v "WF-050" rat
                            "the initialisation rung carries no written justification (its \
                             justification is input-emptiness, ch. 7.3)" ]
                    | false, None ->
                        [ v "WF-050" rat "a non-initialisation rung carries a justification" ]
                    | _ -> [])
                  @ (if r.rung_initialisatie && l.dl_kenmerk <> None then
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

let all (doc : document) : violation list =
  let ctx = { doc } in
  List.concat
    [ wf001 ctx; wf002 ctx; wf003 ctx; wf004 ctx; wf005 ctx; wf006 ctx;
      wf007_model ctx; wf009 ctx; wf011 ctx; wf012 ctx; wf013 ctx; wf014 ctx;
      wf015 ctx; wf016 ctx; wf017 ctx; wf018 ctx; wf020 ctx; wf021 ctx;
      wf022 ctx; wf024 ctx; wf025 ctx; wf026 ctx; wf027 ctx; wf028 ctx;
      wf029 ctx; wf030 ctx; wf031 ctx; wf032 ctx; wf033 ctx; wf045_046 ctx;
      wf047_048_verdelingen ctx; wf050 ctx; wf051 ctx; wf052 ctx; expression_layer ctx ]
