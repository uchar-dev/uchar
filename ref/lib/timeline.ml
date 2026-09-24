(* Timeline algebra, RUNTIME-SEMANTICS ch. 3.

   A timeline value is a finite list of segments — half-open day
   intervals [van, tot) carrying one constant value or explicit leeg —
   sorted ascending and non-overlapping; days covered by no segment read
   leeg (ch. 3.1). Semantics are defined over [read]; only the
   change-detection rule compares segment lists.

   Implemented here: read, calendar and anchored buckets with the
   month-end clamp (ch. 3.2), cut-merge arithmetic with the
   period-coalescing rule (ch. 3.4), temporal summation with the
   contributor-skipping rule (ch. 3.5), the temporal total with clipping
   (ch. 3.6), and the whole-period qualifier's characteristic-holds fold
   (ch. 3.9). Out of this card's scope, faulting where reached: pro-rata
   buckets (ch. 3.7 — the arithmetic core is Arith/WV-2.8), day kinds
   (ch. 3.8), temporal writes beyond the algebra (ch. 3.11). *)

open Value

type segment = { s_van : int; s_tot : int; s_waarde : Value.t }
type t = segment list

(* degenerate segments (tot <= van) contribute nothing anywhere *)
let normalise (segs : t) : t =
  List.filter (fun s -> s.s_tot > s.s_van) segs
  |> List.sort (fun a b -> compare a.s_van b.s_van)

(* read never depends on segment order (ch. 3.1): scan the normalised
   list, so even ill-formed overlapping input reads deterministically *)
let read (tl : t) (d : int) : Value.t =
  match List.find_opt (fun s -> s.s_van <= d && d < s.s_tot) (normalise tl) with
  | Some s -> s.s_waarde
  | None -> Empty

(* --- calendar arithmetic (ch. 3.2/3.8): month-end clamped -------------- *)

let is_leap y = (y mod 4 = 0 && y mod 100 <> 0) || y mod 400 = 0

let days_in_month y m =
  match m with
  | 1 | 3 | 5 | 7 | 8 | 10 | 12 -> 31
  | 4 | 6 | 9 | 11 -> 30
  | 2 -> if is_leap y then 29 else 28
  | _ -> fault "month out of range: %d" m

(* date-plus-months clamps to the last valid day of the target month *)
let add_months d k =
  let y, m, day = civil_from_days d in
  let mm = m - 1 + k in
  let y = y + (if mm >= 0 then mm / 12 else (mm - 11) / 12) in
  let m = ((mm mod 12) + 12) mod 12 + 1 in
  days_from_civil ~y ~m ~d:(min day (days_in_month y m))

(* whole calendar units between two days (ch. 3.2 job 1: weighting);
   quarters are whole months / 3 *)
let whole_units (g : Ir_v0.Ast.granulariteit) van tot =
  if tot <= van then 0
  else
    match g with
    | Dag -> tot - van
    | Week -> (tot - van) / 7
    | Maand | Kwartaal | Jaar ->
        let rec months k = if add_months van (k + 1) <= tot then months (k + 1) else k in
        let m = months 0 in
        (match g with Maand -> m | Kwartaal -> m / 3 | Jaar -> m / 12 | _ -> assert false)

(* the calendar bucket containing [d]: year buckets run 1 Jan to 1 Jan,
   month buckets first-of-month to first-of-next (ch. 3.2 job 2; only
   month and year buckets exist) *)
let bucket (g : Ir_v0.Ast.granulariteit) d =
  let y, m, _ = civil_from_days d in
  match g with
  | Jaar -> (days_from_civil ~y ~m:1 ~d:1, days_from_civil ~y:(y + 1) ~m:1 ~d:1)
  | Maand ->
      let van = days_from_civil ~y ~m ~d:1 in
      let y', m' = if m = 12 then (y + 1, 1) else (y, m + 1) in
      (van, days_from_civil ~y:y' ~m:m' ~d:1)
  | _ -> fault "bucketing is defined for month and year only (ch. 3.2)"

(* ch. 3.7: the time-proportional part. One segment per calendar bucket
   the condition's true periods touch, spanning the WHOLE bucket, value
   = base(bucket start) × true-days/bucket-days as an exact rational —
   v0's fold exactly: true days accumulate per bucket across condition
   periods; the base is read at the bucket START only; a bucket without
   true days, or whose start the base timeline does not cover (or
   covers with leeg), yields no segment; no summation across buckets,
   no auto-rounding, no coalescing of equal neighbours. *)
let pro_rata (g : Ir_v0.Ast.granulariteit) (basis : t) (true_periods : (int * int) list) : t =
  (match g with
  | Maand | Jaar -> ()
  | _ -> fault "pro-rata buckets are month or year only (ch. 3.7)");
  let module IMap = Map.Make (Int) in
  let acc =
    List.fold_left
      (fun acc (pv, pt) ->
        if pt <= pv then acc
        else
          let rec walk acc d =
            if d >= pt then acc
            else
              let bs, bn = bucket g d in
              let days = min pt bn - max pv bs in
              let acc =
                if days > 0 then
                  IMap.update bs (fun p -> Some (Option.value p ~default:0 + days)) acc
                else acc
              in
              walk acc bn
          in
          walk acc pv)
      IMap.empty true_periods
  in
  IMap.fold
    (fun bs days segs ->
      let _, bn = bucket g bs in
      match read basis bs with
      | Empty -> segs
      | Num (q, u) ->
          { s_van = bs; s_tot = bn; s_waarde = Num (Q.mul q (Q.make (Z.of_int days) (Z.of_int (bn - bs))), u) }
          :: segs
      | v -> fault "pro-rata base is not numeric: %s" (Value.to_string v))
    acc []
  |> List.rev

(* ch. 3.8 "aantal dagen in (de maand / het jaar)": per touched
   calendar bucket one segment whose value is the true-day count in
   that bucket (multiple true intervals in one bucket sum); empty
   buckets emit nothing. The counting fold is pro_rata's numerator
   (ch. 3.7), value the whole-day count itself, unitless. *)
let aantal_dagen_in (g : Ir_v0.Ast.granulariteit) (true_periods : (int * int) list) : t =
  (match g with
  | Maand | Jaar -> ()
  | _ -> fault "day-count buckets are month or year only (ch. 3.8)");
  let module IMap = Map.Make (Int) in
  let acc =
    List.fold_left
      (fun acc (pv, pt) ->
        if pt <= pv then acc
        else
          let rec walk acc d =
            if d >= pt then acc
            else
              let bs, bn = bucket g d in
              let days = min pt bn - max pv bs in
              let acc =
                if days > 0 then
                  IMap.update bs (fun p -> Some (Option.value p ~default:0 + days)) acc
                else acc
              in
              walk acc bn
          in
          walk acc pv)
      IMap.empty true_periods
  in
  IMap.fold
    (fun bs days segs ->
      let _, bn = bucket g bs in
      { s_van = bs; s_tot = bn; s_waarde = Value.Num (Q.of_int days, Value.Unitless) } :: segs)
    acc []
  |> List.rev

(* ch. 3.9 "gedurende de gehele maand / het gehele jaar", the
   CHARACTERISTIC-HOLDS test:

     geheel(tl, grid, toets) = for EVERY bucket B touched by tl:
                                 satisfied-days(B, toets) >= threshold(B)
     threshold(B) = days(B)          (every toets except the leeg test)

   A boolean timeline is its true-periods (ch. 3.1), so the buckets it
   touches and the days satisfying "the characteristic holds" come from
   one and the same fold — [aantal_dagen_in]'s, which already emits one
   whole-bucket segment per touched bucket carrying that bucket's true-day
   count. Reusing it is not thrift: it is what keeps "touched" meaning the
   same thing in ch. 3.8 and here.

   Two consequences the chapter states outright and this fold gets for
   free: a timeline with NO segments touches no bucket, so the [for_all]
   is vacuously true; and a bucket the timeline touches only partly has
   fewer true days than calendar days, so it fails.

   [negatie] is the inner test's own negation ("gedurende het gehele jaar
   NIET bedrijvig"): the touched buckets are unchanged — they are still
   the buckets the characteristic's timeline touches — while the
   satisfying days become the complement inside each of them. The
   QUALIFIER's negation ("niet gedurende …") is not here: it complements
   the folded result, which the caller does. *)
let geheel_periode_kenmerk (g : Ir_v0.Ast.granulariteit) ~(negatie : bool)
    (true_periods : (int * int) list) : bool =
  List.for_all
    (fun s ->
      let bucket_days = s.s_tot - s.s_van in
      let true_days =
        match s.s_waarde with
        | Num (q, _) -> Q.to_int q
        | v -> fault "whole-period day count is not numeric: %s" (Value.to_string v)
      in
      let satisfied = if negatie then bucket_days - true_days else true_days in
      satisfied >= bucket_days)
    (aantal_dagen_in g true_periods)

(* ch. 3.9, the VALUE test — the same fold over a value timeline instead of
   a characteristic's true-periods:

     geheel(tl, grid, toets) = for EVERY bucket B touched by tl:
                                 satisfied-days(B, toets) >= days(B)

   The two [aantal_dagen_in] folds below are the whole implementation and
   the split is the point: the FIRST says which buckets the value timeline
   touches, the SECOND how many days in each of them satisfy [pred]. Taking
   the touched buckets from the SATISFYING days instead — the shape the
   characteristic arm can get away with, because there the two coincide —
   would make a timeline of 30 across the whole of 2026, tested "> 40",
   touch no bucket and pass vacuously, which is the opposite of the answer.

   A gap day is covered by no segment, so it lands in neither fold and the
   bucket falls short of its calendar length: a partially covered bucket
   fails, exactly as the chapter says. A timeline with no segments at all
   touches no bucket and passes vacuously. [pred] decides what a leeg
   segment does — for a comparison ch. 1.2d makes it false, so a leeg day
   fails the test without faulting.

   The QUALIFIER's own negation ("niet gedurende …") is not here: it
   complements the folded result, which the caller does. *)
let geheel_periode_waarde (g : Ir_v0.Ast.granulariteit) ~(tl : t) (pred : Value.t -> bool) : bool =
  let segs = normalise tl in
  let interval s = (s.s_van, s.s_tot) in
  let dagen (b : segment) =
    match b.s_waarde with
    | Num (q, _) -> Q.to_int q
    | v -> fault "whole-period day count is not numeric: %s" (Value.to_string v)
  in
  let geraakt = aantal_dagen_in g (List.map interval segs) in
  let goed =
    aantal_dagen_in g
      (List.filter_map (fun s -> if pred s.s_waarde then Some (interval s) else None) segs)
  in
  List.for_all
    (fun (b : segment) ->
      let satisfied =
        match List.find_opt (fun (x : segment) -> x.s_van = b.s_van) goed with
        | Some x -> dagen x
        | None -> 0
      in
      satisfied >= b.s_tot - b.s_van)
    geraakt

(* anchored bucket walls: every wall computed directly from the anchor —
   never by incrementing the previous wall — so a month-end anchor
   clamps consistently (ch. 3.2) *)
let anchored_wall (g : Ir_v0.Ast.granulariteit) ~anchor k =
  match g with
  | Maand -> add_months anchor k
  | Jaar -> add_months anchor (12 * k)
  | _ -> fault "anchored buckets are defined for month and year only (ch. 3.2)"

(* --- cut sets ----------------------------------------------------------- *)

let cuts (tls : t list) : int list =
  List.concat_map (fun tl -> List.concat_map (fun s -> [ s.s_van; s.s_tot ]) tl) tls
  |> List.sort_uniq compare

(* adjacent result segments with equal values coalesce (ch. 3.4/3.5) *)
let coalesce (segs : t) : t =
  List.fold_left
    (fun acc s ->
      match acc with
      | prev :: rest when prev.s_tot = s.s_van && Arith.equal prev.s_waarde s.s_waarde ->
          { prev with s_tot = s.s_tot } :: rest
      | _ -> s :: acc)
    [] (normalise segs)
  |> List.rev

(* --- ch. 3.4: cut-merge arithmetic (the period-coalescing rule) --------- *)

(* [op] is the scalar operator (Arith.add / Arith.sub_min); empty operands
   coalesce to 0 per period — inside the union's span only *)
let cut_merge op (a : t) (b : t) : t =
  let cs = cuts [ a; b ] in
  let rec pairs = function c1 :: (c2 :: _ as rest) -> (c1, c2) :: pairs rest | _ -> [] in
  List.map
    (fun (c1, c2) ->
      let va = read a c1 and vb = read b c1 in
      { s_van = c1; s_tot = c2; s_waarde = op va vb })
    (pairs cs)
  |> coalesce

let plus = cut_merge Arith.add
let min_ = cut_merge Arith.sub_min

(* --- ch. 3.5: temporal summation (the contributor-skipping rule) -------- *)

(* [target_unit]: each contributor is restated to the target's unit
   before the fold (exact factors, ch. 2.6). Zero contributors yield the
   empty timeline, fallback or not; all-leeg periods the fallback does
   not fill are emitted as explicit leeg segments. *)
let som ?fallback ?(target_unit = Value.Unitless) (tls : t list) : t =
  match tls with
  | [] -> []
  | _ ->
      let restate v =
        match v with
        | Num (q, u) -> Num (Arith.restate_into target_unit q u, target_unit)
        | v -> v
      in
      let cs = cuts tls in
      let rec pairs = function c1 :: (c2 :: _ as rest) -> (c1, c2) :: pairs rest | _ -> [] in
      List.map
        (fun (c1, c2) ->
          let filled =
            List.filter_map
              (fun tl -> match read tl c1 with Empty -> None | v -> Some (restate v))
              tls
          in
          let v =
            match filled with
            | [] -> ( match fallback with Some f -> f | None -> Empty)
            | first :: rest -> List.fold_left Arith.add first rest
          in
          { s_van = c1; s_tot = c2; s_waarde = v })
        (pairs cs)
      |> coalesce

(* --- ch. 3.6: the temporal total ---------------------------------------- *)

(* clip a value timeline to the periods where [true_periods] holds: one
   fragment per (value segment × condition period) overlap, NOT
   coalesced — the pinned runtime floors whole units per raw clipped
   fragment, so a condition boundary that splits a bucket genuinely
   loses the partial units on both sides (mirrored; the ASP encoding
   floors per fragment identically) *)
let clip (tl : t) (true_periods : (int * int) list) : t =
  List.concat_map
    (fun s ->
      List.filter_map
        (fun (van, tot) ->
          let v = max s.s_van van and t = min s.s_tot tot in
          if t > v then Some { s with s_van = v; s_tot = t } else None)
        true_periods)
    tl

(* the true-periods of a per-period predicate over a tested timeline,
   evaluated over the cut set of tested ∪ span (the ch. 1.2 comparison
   rules apply per period — leeg satisfies ≠ against a filled bound) *)
let true_periods_of (tested : t) ~(span : (int * int) list) (pred : Value.t -> bool) :
    (int * int) list =
  let span_tl = List.map (fun (v, t) -> { s_van = v; s_tot = t; s_waarde = Bool true }) span in
  let cs = cuts [ tested; span_tl ] in
  let rec pairs = function c1 :: (c2 :: _ as rest) -> (c1, c2) :: pairs rest | _ -> [] in
  List.filter_map (fun (c1, c2) -> if pred (read tested c1) then Some (c1, c2) else None) (pairs cs)

(* complement of a boolean timeline's true-periods within a span
   (ch. 3.6: the negated gedurende form) *)
(* intersection of two true-period lists (ch. 3.6, the composite ALLE
   clip): pairwise overlap [max from, min to) — v0 reaches the same set
   by nesting per-member clips; intersection is associative, so the
   fold over a composite's members lands on identical periods *)
let intersect_periods (a : (int * int) list) (b : (int * int) list) : (int * int) list =
  List.concat_map
    (fun (av, at) ->
      List.filter_map
        (fun (bv, bt) ->
          let v = max av bv and t = min at bt in
          if t > v then Some (v, t) else None)
        b)
    a
  |> List.sort compare

let complement (true_periods : (int * int) list) ~(span : (int * int) list) : (int * int) list =
  List.concat_map
    (fun (sv, st) ->
      let inside =
        List.filter_map
          (fun (v, t) ->
            let v = max v sv and t = min t st in
            if t > v then Some (v, t) else None)
          true_periods
        |> List.sort compare
      in
      let rec walk pos acc = function
        | [] -> if st > pos then (pos, st) :: acc else acc
        | (v, t) :: rest -> walk t (if v > pos then (pos, v) :: acc else acc) rest
      in
      List.rev (walk sv [] inside))
    span

(* the time unit the granularity's grid counts in (ch. 3.2 job 1) *)
let granularity_time_unit : Ir_v0.Ast.granulariteit -> Ir_v0.Checks.time_unit = function
  | Dag -> Tu_dag
  | Week -> Tu_week
  | Maand -> Tu_maand
  | Kwartaal -> Tu_kwartaal
  | Jaar -> Tu_jaar

let time_unit_name : Ir_v0.Checks.time_unit -> string = function
  | Tu_jaar -> "jr"
  | Tu_kwartaal -> "kw"
  | Tu_maand -> "mnd"
  | Tu_week -> "wk"
  | Tu_dag -> "dg"
  | Tu_uur -> "u"
  | Tu_minuut -> "min"
  | Tu_seconde -> "sec"
  | Tu_milliseconde -> "ms"

(* The unit RECORD of a granularity's grid (ch. 3.6, the integration
   below). The grid counts in a calendar unit, but the exact conversion
   factors that let a foreign denominator cancel live in the DOCUMENT's
   declared unit systems, not in the granularity — so the grid unit is
   looked up there, keyed by the ch. 3.8 spelling table, once per fold.
   [Error] carries the reason, and the caller raises it only where a rate
   value actually needs the grid: a unitless or plain-unit timeline
   totals in a document that declares no time unit at all. Two systems
   spelling the same grid unit with different canonical chains give the
   fold no single grid — loud, never a first-match guess. *)
let grid_unit (doc : Ir_v0.Ast.document) (g : Ir_v0.Ast.granulariteit) :
    (Ir_v0.Ast.eenheid, string) result =
  let want = granularity_time_unit g in
  let candidates =
    List.concat_map
      (fun (_, (es : Ir_v0.Ast.eenheidstelsel)) ->
        List.map (fun (d : Ir_v0.Ast.gedeclareerde_eenheid) -> d.ge_eenheid) es.eenheden)
      doc.eenheidstelsels
    |> List.filter (fun (e : Ir_v0.Ast.eenheid) ->
           e.noemer = None && Ir_v0.Checks.time_unit_of_eenheid e = Some want)
  in
  (* the factor as the rational it is, so two spellings of one value are
     one grid *)
  let key (e : Ir_v0.Ast.eenheid) =
    (Option.map Ir_v0.Checks.norm e.canoniek, Option.map Arith.rational e.factor_naar_canoniek)
  in
  match (candidates, List.sort_uniq compare (List.map key candidates)) with
  | e :: _, [ _ ] -> Ok e
  | _, [] ->
      Error
        (Printf.sprintf
           "the document declares no %s unit, so this timeline's grid has no unit to integrate \
            against (ch. 3.6)"
           (time_unit_name want))
  | _, ks ->
      Error
        (Printf.sprintf
           "the document declares %d different %s units, so this timeline's grid has no single \
            unit to integrate against (ch. 3.6)"
           (List.length ks) (time_unit_name want))

(* the multiplier of one segment (ch. 3.6 "Units"): a plain or unitless
   value is weighted by a bare count, but a RATE value is INTEGRATED —
   the count is a duration stated in the GRID's own unit, so a
   denominator convertible to it cancels through the declared factors and
   the total carries the numerator alone (€/mnd × mnd = €, the §4.20
   Tabel 30 row-2 result the frontend types statically; €/jr over a
   per-month grid cancels exactly as well — 12 months IS one year by the
   declared factor). A denominator the declarations do not connect to the
   grid unit is refused loudly, naming BOTH units: no executor may invent
   a factor nobody declared. *)
let duration ?(grid_unit : (Ir_v0.Ast.eenheid, string) result option) units (v : Value.t) : Value.t
    =
  let plain = Num (Q.of_int units, Unitless) in
  match v with
  | Num (_, Unit ({ noemer = Some noemer; _ } as u)) -> (
      match grid_unit with
      | None ->
          fault "the temporal total of a %s value reached the collapse without a grid unit (ch. 3.6)"
            (Arith.show_unit (Unit u))
      | Some (Error why) -> fault "the temporal total of a %s value: %s" (Arith.show_unit (Unit u)) why
      | Some (Ok gu) ->
          if Arith.convertible noemer gu then Num (Q.of_int units, Unit gu)
          else
            fault
              "the temporal total of a %s value cannot integrate over a %s grid: the declarations \
               connect no conversion between its denominator and the grid unit (ch. 3.6)"
              (Arith.show_unit (Unit u))
              (Arith.show_eenheid gu))
  (* a value unit without a denominator keeps the plain rule, and so does
     a unitless value. A Product is built ONLY by Arith.mul out of two
     denominator-free units, so it never carries a denominator either —
     the guard below is the fail-fast for a representation change that
     would silently drop one. *)
  | Num (_, Unit { noemer = None; _ }) | Num (_, Unitless) -> plain
  | Num (_, Product (a, b)) ->
      if a.noemer <> None || b.noemer <> None then
        fault "the temporal total of a %s value: a unit product with a denominator has no \
               integration rule (ch. 3.6)"
          (Arith.show_unit (Product (a, b)));
      plain
  | _ -> plain

(* totaal(tl) = Σ value(s) × duration(van, tot); a leeg segment
   contributes 0; a segmentless timeline totals to 0 (the absent-timeline
   leeg case is the caller's, ch. 3.6). [grid_unit] is resolved once per
   fold by the caller (it needs the document); its absence only bites a
   rate-valued segment. *)
let totaal ?grid_unit (g : Ir_v0.Ast.granulariteit) (tl : t) : Value.t =
  List.fold_left
    (fun acc s ->
      match s.s_waarde with
      | Empty -> acc
      | v ->
          let units = whole_units g s.s_van s.s_tot in
          Arith.add acc (Arith.mul v (duration ?grid_unit units v)))
    (Num (Q.zero, Unitless))
    (normalise tl)
