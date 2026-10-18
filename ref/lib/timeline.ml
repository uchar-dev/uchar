(* Timeline algebra, RUNTIME-SEMANTICS ch. 3.

   A timeline value is a finite list of segments — half-open day
   intervals [van, tot) carrying one constant value or explicit leeg —
   sorted ascending and non-overlapping; days covered by no segment read
   leeg (ch. 3.1). Semantics are defined over [read]; only the
   change-detection rule compares segment lists.

   Implemented here: read, calendar and anchored buckets with the
   month-end clamp and the grid a fixed start point lays (ch. 3.2), cut-merge arithmetic with the
   period-coalescing rule (ch. 3.4), temporal summation with the
   contributor-skipping rule (ch. 3.5), the temporal total with clipping
   (ch. 3.6), and the whole-period qualifier's characteristic-holds fold
   (ch. 3.9). Out of this card's scope, faulting where reached: pro-rata
   buckets (ch. 3.7 — the arithmetic core is Arith/WV-2.8), day kinds
   (ch. 3.8), temporal writes beyond the algebra (ch. 3.11). *)

open Value

type segment = { s_from : int; s_to : int; s_value : Value.t }
type t = segment list

(* degenerate segments (tot <= van) contribute nothing anywhere *)
let normalise (segs : t) : t =
  List.filter (fun s -> s.s_to > s.s_from) segs
  |> List.sort (fun a b -> compare a.s_from b.s_from)

(* read never depends on segment order (ch. 3.1): scan the normalised
   list, so even ill-formed overlapping input reads deterministically *)
let read (tl : t) (d : int) : Value.t =
  match List.find_opt (fun s -> s.s_from <= d && d < s.s_to) (normalise tl) with
  | Some s -> s.s_value
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
let whole_units (g : Ir_v0.Ast.grain) van up_to =
  if up_to <= van then 0
  else
    match g with
    | Day -> up_to - van
    | Week -> (up_to - van) / 7
    | Month | Quarter | Year ->
        let rec months k = if add_months van (k + 1) <= up_to then months (k + 1) else k in
        let m = months 0 in
        (match g with Month -> m | Quarter -> m / 3 | Year -> m / 12 | _ -> assert false)

(* the calendar bucket containing [d]: year buckets run 1 Jan to 1 Jan,
   month buckets first-of-month to first-of-next (ch. 3.2 job 2; only
   month and year buckets exist) *)
let bucket (g : Ir_v0.Ast.grain) d =
  let y, m, _ = civil_from_days d in
  match g with
  | Year -> (days_from_civil ~y ~m:1 ~d:1, days_from_civil ~y:(y + 1) ~m:1 ~d:1)
  | Month ->
      let van = days_from_civil ~y ~m ~d:1 in
      let y', m' = if m = 12 then (y + 1, 1) else (y, m + 1) in
      (van, days_from_civil ~y:y' ~m:m' ~d:1)
  | _ -> fault "bucketing is defined for month and year only (ch. 3.2)"

(* --- the grid a member reads (ch. 3.2) ----------------------------------- *)

(* Where a timeline's periods start: on the calendar (no start point), from
   a fixed start point (a date, or a day-month anchored in a leap year), or
   from the anchor a start-point determination binds per object. *)
type start = Calendar | Fixed_date of int | Fixed_day_month of int | Variable

type grid = { g_gran : Ir_v0.Ast.grain; g_count : int; g_start : start }

let calendar_grid g = { g_gran = g; g_count = 1; g_start = Calendar }

(* The grid of a facet: its granularity and count, and the start its fixed
   start point or its named timeline gives it. A fixed start point that is
   no WF-115 lexeme never reaches here: the loader refuses it. *)
let grid_of ~gran ~count ~(fixed : string option) ~variable =
  let g_start =
    if variable then Variable
    else
      match fixed with
      | None -> Calendar
      | Some s -> (
          match Ir_v0.Checks.read_fixed_start_point s with
          | Some (Vs_date (y, m, d)) -> Fixed_date (days_from_civil ~y ~m ~d)
          | Some (Vs_day_month (m, d)) -> Fixed_day_month (days_from_civil ~y:2000 ~m ~d)
          | None -> fault "fixed start point '%s' is no WF-115 lexeme" s)
  in
  { g_gran = gran; g_count = count; g_start }

(* The period of grid [gr] that contains day [d] (ch. 3.2): the calendar
   bucket, or the one a fixed start point lays, every wall computed from the
   start point. A multi-period grid and a variable-start grid are refused:
   the first has a grid and no operation, the second is laid per object from
   an anchor this interpreter does not execute. *)
let grid_bucket (gr : grid) d =
  if gr.g_count <> 1 then
    fault
      "a multi-period grid (%d %s periods) carries no operation (RUNTIME-SEMANTICS ch. 3.2, OPEN \
       in ch. 3.12)"
      gr.g_count (Ir_v0.Spelling.encode Ir_v0.Spelling.granularity gr.g_gran);
  match (gr.g_start, gr.g_gran) with
  | Calendar, g -> bucket g d
  | Variable, _ ->
      fault
        "a variable-start grid is laid per object from the anchor its start-point determination \
         binds, which this interpreter does not execute (ch. 3.2)"
  | Fixed_date a, Week ->
      let k = if d >= a then (d - a) / 7 else -(((a - d) + 6) / 7) in
      (a + (7 * k), a + (7 * (k + 1)))
  | Fixed_day_month anchor, ((Month | Quarter | Year) as g) ->
      let step = Ir_v0.Checks.step_months g in
      let ya, ma, _ = civil_from_days anchor and yd, md, _ = civil_from_days d in
      let months = ((yd - ya) * 12) + (md - ma) in
      let k = if months >= 0 then months / step else -(((-months) + step - 1) / step) in
      let wall k = add_months anchor (step * k) in
      let k = if wall k > d then k - 1 else k in
      (wall k, wall (k + 1))
  | (Fixed_date _ | Fixed_day_month _), _ ->
      fault "a fixed start point of this form lays no %s grid of one unit (WF-115)"
        (Ir_v0.Spelling.encode Ir_v0.Spelling.granularity gr.g_gran)

(* ch. 3.7: the time-proportional part. One segment per calendar bucket
   the condition's true periods touch, spanning the WHOLE bucket, value
   = base(bucket start) × true-days/bucket-days as an exact rational —
   v0's fold exactly: true days accumulate per bucket across condition
   periods; the base is read at the bucket START only; a bucket without
   true days, or whose start the base timeline does not cover (or
   covers with leeg), yields no segment; no summation across buckets,
   no auto-rounding, no coalescing of equal neighbours. *)
let pro_rata (g : Ir_v0.Ast.grain) (base_expr : t) (true_periods : (int * int) list) : t =
  (match g with
  | Month | Year -> ()
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
      match read base_expr bs with
      | Empty -> segs
      | Num (q, u) ->
          { s_from = bs; s_to = bn; s_value = Num (Q.mul q (Q.make (Z.of_int days) (Z.of_int (bn - bs))), u) }
          :: segs
      | v -> fault "pro-rata base is not numeric: %s" (Value.to_string v))
    acc []
  |> List.rev

(* ch. 3.8 "aantal dagen in (de maand / het jaar)": per touched
   period of the grid one segment whose value is the true-day count in
   that period (multiple true intervals in one period sum); empty
   periods emit nothing. The counting fold is pro_rata's numerator
   (ch. 3.7), value the whole-day count itself, unitless. The grid is the
   target's: the calendar grid, or the one its fixed start point lays
   (ch. 3.2); [count_days_in] below is the calendar case the whole-period
   qualifier folds over. *)
let count_days_in_grid (gr : grid) (true_periods : (int * int) list) : t =
  (match gr.g_gran with
  | Month | Year -> ()
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
              let bs, bn = grid_bucket gr d in
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
      let _, bn = grid_bucket gr bs in
      { s_from = bs; s_to = bn; s_value = Value.Num (Q.of_int days, Value.Unitless) } :: segs)
    acc []
  |> List.rev

let count_days_in (g : Ir_v0.Ast.grain) (true_periods : (int * int) list) : t =
  count_days_in_grid (calendar_grid g) true_periods

(* ch. 3.9 "gedurende de gehele maand / het gehele jaar", the
   CHARACTERISTIC-HOLDS test:

     geheel(tl, grid, toets) = for EVERY bucket B touched by tl:
                                 satisfied-days(B, toets) >= threshold(B)
     threshold(B) = days(B)          (every toets except the leeg test)

   A boolean timeline is its true-periods (ch. 3.1), so the buckets it
   touches and the days satisfying "the characteristic holds" come from
   one and the same fold — [count_days_in]'s, which already emits one
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
let entire_period_characteristic (g : Ir_v0.Ast.grain) ~(negation : bool)
    (true_periods : (int * int) list) : bool =
  List.for_all
    (fun s ->
      let bucket_days = s.s_to - s.s_from in
      let true_days =
        match s.s_value with
        | Num (q, _) -> Q.to_int q
        | v -> fault "whole-period day count is not numeric: %s" (Value.to_string v)
      in
      let satisfied = if negation then bucket_days - true_days else true_days in
      satisfied >= bucket_days)
    (count_days_in g true_periods)

(* ch. 3.9, the VALUE test — the same fold over a value timeline instead of
   a characteristic's true-periods:

     geheel(tl, grid, toets) = for EVERY bucket B touched by tl:
                                 satisfied-days(B, toets) >= days(B)

   The two [count_days_in] folds below are the whole implementation and
   the split is the point: the FIRST says which buckets the value timeline
   touches, the SECOND how many days in each of them satisfy [pred]. Taking
   the touched buckets from the SATISFYING days instead — the shape the
   characteristic arm can get away with, because there the two coincide —
   would make a timeline of 30 across the whole of 2026, tested "> 40",
   touch no bucket and pass vacuously, which is the opposite of the answer.

   A gap day is covered by no segment, so it lands in neither fold and the
   bucket falls short of its calendar length: a partially covered bucket
   fails, exactly as the chapter says. A timeline with no segments at all
   touches no bucket and passes vacuously. An explicit leeg segment is a
   gap as well (ch. 3.1, 3.9): it is dropped before either fold, so it
   touches no bucket and none of its days satisfies [pred] — not even an
   inequality, which ch. 1.2d makes true against leeg, because a gap day
   is not a comparison of a value (decided 2026-09-26).

   The QUALIFIER's own negation ("niet gedurende …") is not here: it
   complements the folded result, which the caller does. *)
let whole_period_value (g : Ir_v0.Ast.grain) ~(tl : t) (pred : Value.t -> bool) : bool =
  let segs = List.filter (fun s -> s.s_value <> Empty) (normalise tl) in
  let interval s = (s.s_from, s.s_to) in
  let day_count (b : segment) =
    match b.s_value with
    | Num (q, _) -> Q.to_int q
    | v -> fault "whole-period day count is not numeric: %s" (Value.to_string v)
  in
  let touched = count_days_in g (List.map interval segs) in
  let goed =
    count_days_in g
      (List.filter_map (fun s -> if pred s.s_value then Some (interval s) else None) segs)
  in
  List.for_all
    (fun (b : segment) ->
      let satisfied =
        match List.find_opt (fun (x : segment) -> x.s_from = b.s_from) goed with
        | Some x -> day_count x
        | None -> 0
      in
      satisfied >= b.s_to - b.s_from)
    touched

(* ch. 3.9, the LEEG and GEVULD tests over a value timeline:

     satisfied-days(B, gevuld) = the days of B on which tl reads a value
     satisfied-days(B, leeg)   = days(B) - satisfied-days(B, gevuld)

   with the threshold days(B) for both, like every other test. The buckets
   are the ones the FILLED segments touch: ch. 3.1 makes an explicit leeg
   segment and a gap the same thing under [read], so a bucket only an
   explicit leeg segment reaches is not touched, as in
   [whole_period_value] above — the ASP backend cannot write such a
   segment at all, so this is also where the two executors agree. Every bucket touched this way holds at least one filled day, so
   the leeg fold comes down to "no filled day anywhere" and the grid does
   not change its verdict; it is still computed per bucket, so the code
   reads as the formula does.

   [gevuld] selects the test after the INNER negation, which is per day:
   "niet leeg" holds on exactly the days "gevuld" does, so the caller passes
   the sister test. The qualifier's own negation is the caller's too. *)
let whole_period_empty_filled (g : Ir_v0.Ast.grain) ~(populated : bool) ~(tl : t) : bool =
  let filled_segs = List.filter (fun s -> match s.s_value with Empty -> false | _ -> true) (normalise tl) in
  List.for_all
    (fun (b : segment) ->
      let days_b = b.s_to - b.s_from in
      let filled_days =
        match b.s_value with
        | Num (q, _) -> Q.to_int q
        | v -> fault "whole-period day count is not numeric: %s" (Value.to_string v)
      in
      let satisfied = if populated then filled_days else days_b - filled_days in
      satisfied >= days_b)
    (count_days_in g (List.map (fun s -> (s.s_from, s.s_to)) filled_segs))

(* anchored bucket walls: every wall computed directly from the anchor —
   never by incrementing the previous wall — so a month-end anchor
   clamps consistently (ch. 3.2) *)
let anchored_wall (g : Ir_v0.Ast.grain) ~anchor k =
  match g with
  | Month -> add_months anchor k
  | Year -> add_months anchor (12 * k)
  | _ -> fault "anchored buckets are defined for month and year only (ch. 3.2)"

(* The grid of attribute [name] of [ot] (the chain-merged type): its facet's
   granularity, count and fixed start point, and a variable start where the
   facet names a timeline declared with one. *)
let attr_grid (doc : Ir_v0.Ast.document) (ot : Ir_v0.Ast.object_type) name =
  let gran =
    match Ir_v0.Checks.find_ci ot.attribute_granularity name with
    | Some (Some g) -> g
    | _ -> fault "temporal attribute %S without a granularity" name
  in
  let period = Ir_v0.Checks.find_ci ot.attribute_timeline_count name in
  let variable =
    match Option.join (Ir_v0.Checks.find_ci ot.attribute_timeline_names name) with
    | Some n -> (
        match Ir_v0.Checks.find_timeline { Ir_v0.Checks.doc } n with
        | Some tl -> tl.variable_start
        | None -> false)
    | None -> false
  in
  grid_of ~gran
    ~count:(Option.value (Option.bind period (fun t -> t.tv_count)) ~default:1)
    ~fixed:(Option.bind period (fun t -> t.tv_fixed_start_point))
    ~variable

(* The grid of a time-dependent parameter; a parameter names no timeline. *)
let param_grid (p : Ir_v0.Ast.parameter) =
  match p.par_granularity with
  | Some gran ->
      grid_of ~gran ~count:(Option.value p.par_timeline_count ~default:1) ~fixed:p.par_fixed_start_point
        ~variable:false
  | None -> fault "parameter %S has no timeline" (Option.value p.par_name ~default:"")

(* --- cut sets ----------------------------------------------------------- *)

let cuts (tls : t list) : int list =
  List.concat_map (fun tl -> List.concat_map (fun s -> [ s.s_from; s.s_to ]) tl) tls
  |> List.sort_uniq compare

(* adjacent result segments with equal values coalesce (ch. 3.4/3.5) *)
let coalesce (segs : t) : t =
  List.fold_left
    (fun acc s ->
      match acc with
      | prev :: rest when prev.s_to = s.s_from && Arith.equal prev.s_value s.s_value ->
          { prev with s_to = s.s_to } :: rest
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
      { s_from = c1; s_to = c2; s_value = op va vb })
    (pairs cs)
  |> coalesce

let plus = cut_merge Arith.add
let min_ = cut_merge Arith.sub_min

(* --- ch. 3.5: temporal summation (the contributor-skipping rule) -------- *)

(* [target_unit]: each contributor is restated to the target's unit
   before the fold (exact factors, ch. 2.6). Zero contributors yield the
   empty timeline, fallback or not; all-leeg periods the fallback does
   not fill are emitted as explicit leeg segments. *)
let summed ?fallback ?(target_unit = Value.Unitless) (tls : t list) : t =
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
          { s_from = c1; s_to = c2; s_value = v })
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
        (fun (van, up_to) ->
          let v = max s.s_from van and t = min s.s_to up_to in
          if t > v then Some { s with s_from = v; s_to = t } else None)
        true_periods)
    tl

(* the true-periods of a per-period predicate over a tested timeline,
   evaluated over the cut set of tested ∪ span (the ch. 1.2 comparison
   rules apply per period — leeg satisfies ≠ against a filled bound) *)
let true_periods_of (tested : t) ~(span : (int * int) list) (pred : Value.t -> bool) :
    (int * int) list =
  let span_tl = List.map (fun (v, t) -> { s_from = v; s_to = t; s_value = Bool true }) span in
  let cs = cuts [ tested; span_tl ] in
  let rec pairs = function c1 :: (c2 :: _ as rest) -> (c1, c2) :: pairs rest | _ -> [] in
  List.filter_map (fun (c1, c2) -> if pred (read tested c1) then Some (c1, c2) else None) (pairs cs)

(* complement of a boolean timeline's true-periods within a span
   (ch. 3.6: the negated gedurende form) *)
(* intersection of two true-period lists (ch. 3.6, the composite `all`
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
let granularity_time_unit : Ir_v0.Ast.grain -> Ir_v0.Checks.time_unit = function
  | Day -> Tu_day
  | Week -> Tu_week
  | Month -> Tu_month
  | Quarter -> Tu_quarter
  | Year -> Tu_year

let time_unit_name : Ir_v0.Checks.time_unit -> string = function
  | Tu_year -> "jr"
  | Tu_quarter -> "kw"
  | Tu_month -> "mnd"
  | Tu_week -> "wk"
  | Tu_day -> "dg"
  | Tu_hour -> "u"
  | Tu_minute -> "min"
  | Tu_second -> "sec"
  | Tu_millisecond -> "ms"

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
let grid_unit (doc : Ir_v0.Ast.document) (g : Ir_v0.Ast.grain) :
    (Ir_v0.Ast.measure_unit, string) result =
  let because = granularity_time_unit g in
  let candidates =
    List.concat_map
      (fun (_, (es : Ir_v0.Ast.unit_system)) ->
        List.map (fun (d : Ir_v0.Ast.declared_unit) -> d.ge_unit) es.unit_list)
      doc.unit_systems
    |> List.filter (fun (e : Ir_v0.Ast.measure_unit) ->
           e.denominator = None && Ir_v0.Checks.time_unit_of_unit e = Some because)
  in
  (* the factor as the rational it is, so two spellings of one value are
     one grid *)
  let key (e : Ir_v0.Ast.measure_unit) =
    (Option.map Ir_v0.Checks.norm e.canonical_form, Option.map Arith.rational e.factor_to_canonical)
  in
  match (candidates, List.sort_uniq compare (List.map key candidates)) with
  | e :: _, [ _ ] -> Ok e
  | _, [] ->
      Error
        (Printf.sprintf
           "the document declares no %s unit, so this timeline's grid has no unit to integrate \
            against (ch. 3.6)"
           (time_unit_name because))
  | _, ks ->
      Error
        (Printf.sprintf
           "the document declares %d different %s units, so this timeline's grid has no single \
            unit to integrate against (ch. 3.6)"
           (List.length ks) (time_unit_name because))

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
let duration ?(grid_unit : (Ir_v0.Ast.measure_unit, string) result option) units (v : Value.t) : Value.t
    =
  let plain = Num (Q.of_int units, Unitless) in
  match v with
  | Num (_, Unit ({ denominator = Some denominator; _ } as u)) -> (
      match grid_unit with
      | None ->
          fault "the temporal total of a %s value reached the collapse without a grid unit (ch. 3.6)"
            (Arith.show_unit (Unit u))
      | Some (Error why) -> fault "the temporal total of a %s value: %s" (Arith.show_unit (Unit u)) why
      | Some (Ok gu) ->
          if Arith.convertible denominator gu then Num (Q.of_int units, Unit gu)
          else
            fault
              "the temporal total of a %s value cannot integrate over a %s grid: the declarations \
               connect no conversion between its denominator and the grid unit (ch. 3.6)"
              (Arith.show_unit (Unit u))
              (Arith.display_unit gu))
  (* a value unit without a denominator keeps the plain rule, and so does
     a unitless value. A Product is built ONLY by Arith.mul out of two
     denominator-free units, so it never carries a denominator either —
     the guard below is the fail-fast for a representation change that
     would silently drop one. *)
  | Num (_, Unit { denominator = None; _ }) | Num (_, Unitless) -> plain
  | Num (_, Product (a, b)) ->
      if a.denominator <> None || b.denominator <> None then
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
let grand_total ?grid_unit (g : Ir_v0.Ast.grain) (tl : t) : Value.t =
  List.fold_left
    (fun acc s ->
      match s.s_value with
      | Empty -> acc
      | v ->
          let units = whole_units g s.s_from s.s_to in
          Arith.add acc (Arith.mul v (duration ?grid_unit units v)))
    (Num (Q.zero, Unitless))
    (normalise tl)
