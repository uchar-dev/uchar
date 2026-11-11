(* The calendar and the period grids, as this backend lays them
   (RUNTIME-SEMANTICS ch. 3.2 grids, buckets and start points; ch. 3.6 the
   calendar measure; ch. 3.8 the date-plus-unit step). The translator reads
   every wall, bucket and length from here, so the differential compares two
   readings of those chapters, not one.

   Dates are civil day numbers, day 0 being 1970-01-01, the encoding in which
   the case reader hands dates over. *)

module Value = Uchar_ref.Value
open Ir_v0.Ast

let fault = Value.fault

(* --- civil dates (proleptic Gregorian) --------------------------------- *)

let fdiv a b = if a >= 0 then a / b else -(((-a) + b - 1) / b)
let is_leap y = (y mod 4 = 0 && y mod 100 <> 0) || y mod 400 = 0

let days_in_month y m =
  match m with
  | 2 -> if is_leap y then 29 else 28
  | 4 | 6 | 9 | 11 -> 30
  | 1 | 3 | 5 | 7 | 8 | 10 | 12 -> 31
  | _ -> fault "month out of range: %d" m

(* days from 0001-01-01 to 1 January of year [y] *)
let days_before_year y =
  let p = y - 1 in
  (365 * p) + fdiv p 4 - fdiv p 100 + fdiv p 400

let cumulative_days = [| 0; 31; 59; 90; 120; 151; 181; 212; 243; 273; 304; 334 |]

let days_before_month y m = cumulative_days.(m - 1) + if m > 2 && is_leap y then 1 else 0
let epoch = days_before_year 1970

let days_from_civil ~y ~m ~d = days_before_year y + days_before_month y m + (d - 1) - epoch

let civil_from_days z =
  let n = z + epoch in
  (* a first guess from the mean year of 146097/400 days, then corrected *)
  let rec settle y =
    if days_before_year y > n then settle (y - 1)
    else if days_before_year (y + 1) <= n then settle (y + 1)
    else y
  in
  let y = settle (fdiv (n * 400) 146097 + 1) in
  let doy = n - days_before_year y in
  let rec month m = if m < 12 && days_before_month y (m + 1) <= doy then month (m + 1) else m in
  let m = month 1 in
  (y, m, doy - days_before_month y m + 1)

(* the date-plus-months step (ch. 3.8): the day of month is kept, and clamps
   to the last day of the target month *)
let add_months z k =
  let y, m, d = civil_from_days z in
  let index = (y * 12) + (m - 1) + k in
  let y' = fdiv index 12 and m' = index - (fdiv index 12 * 12) + 1 in
  days_from_civil ~y:y' ~m:m' ~d:(min d (days_in_month y' m'))

let months_of = function Month -> 1 | Quarter -> 3 | Year -> 12 | Day | Week -> 0

(* the whole units of [van, up_to) counted from [van] (ch. 3.2, weighting):
   for a month the greatest k with van + k months <= up_to, a quarter k / 3
   and a year k / 12 of those, a week seven days *)
let whole_units (g : grain) van up_to =
  if up_to <= van then 0
  else
    match g with
    | Day -> up_to - van
    | Week -> (up_to - van) / 7
    | Month | Quarter | Year ->
        let y1, m1, _ = civil_from_days van and y2, m2, _ = civil_from_days up_to in
        let rec fit k =
          if add_months van k > up_to then fit (k - 1)
          else if add_months van (k + 1) <= up_to then fit (k + 1)
          else k
        in
        fit (((y2 - y1) * 12) + (m2 - m1)) / months_of g

(* the calendar bucket that holds day [d] (ch. 3.2, bucketing): only month and
   year buckets exist on the calendar *)
let calendar_bucket (g : grain) d =
  let y, m, _ = civil_from_days d in
  match g with
  | Month ->
      let first = days_from_civil ~y ~m ~d:1 in
      (first, first + days_in_month y m)
  | Year -> (days_from_civil ~y ~m:1 ~d:1, days_from_civil ~y:(y + 1) ~m:1 ~d:1)
  | Day | Week | Quarter -> fault "bucketing is defined for month and year only (ch. 3.2)"

(* --- grids (ch. 3.2) ---------------------------------------------------- *)

type start =
  | On_calendar
  | From_date of int  (** a fixed start point yyyy-mm-dd, as a day number *)
  | From_day_month of int * int  (** a fixed start point --mm-dd: month, day *)
  | Per_object  (** a variable start, laid from an anchor per object *)

type grid = { gran : grain; count : int; start : start }

let calendar gran = { gran; count = 1; start = On_calendar }

let grid_of ~gran ~count ~(fixed : string option) ~variable =
  let start =
    if variable then Per_object
    else
      match fixed with
      | None -> On_calendar
      | Some s -> (
          match Ir_v0.Checks.read_fixed_start_point s with
          | Some (Vs_date (y, m, d)) -> From_date (days_from_civil ~y ~m ~d)
          | Some (Vs_day_month (m, d)) -> From_day_month (m, d)
          | None -> fault "fixed start point '%s' is no WF-115 lexeme" s)
  in
  { gran; count; start }

(* the grid of attribute [name] of the chain-merged type [ot]: the facet's
   granularity, count and fixed start point, and a variable start where the
   facet names a timeline declared with one *)
let of_attribute (doc : document) (ot : object_type) name =
  let gran =
    match Ir_v0.Checks.find_ci ot.attribute_granularity name with
    | Some (Some g) -> g
    | _ -> fault "temporal attribute %S without a granularity" name
  in
  let facet = Ir_v0.Checks.find_ci ot.attribute_timeline_count name in
  let variable =
    match Option.join (Ir_v0.Checks.find_ci ot.attribute_timeline_names name) with
    | Some n -> (
        match Ir_v0.Checks.find_timeline (Ir_v0.Checks.ctx_of doc) n with
        | Some tl -> tl.variable_start
        | None -> false)
    | None -> false
  in
  grid_of ~gran
    ~count:(Option.value (Option.bind facet (fun t -> t.tv_count)) ~default:1)
    ~fixed:(Option.bind facet (fun t -> t.tv_fixed_start_point))
    ~variable

(* the grid of a time-dependent parameter; a parameter names no timeline *)
let of_parameter (p : parameter) =
  match p.par_granularity with
  | Some gran ->
      grid_of ~gran ~count:(Option.value p.par_timeline_count ~default:1) ~fixed:p.par_fixed_start_point
        ~variable:false
  | None -> fault "parameter %S has no timeline" (Option.value p.par_name ~default:"")

(* the start of a grid as WF-126 compares it: None on the calendar *)
let start_key (g : grid) =
  match g.start with
  | On_calendar -> None
  | From_date z ->
      let y, m, d = civil_from_days z in
      Some (Printf.sprintf "%04d-%02d-%02d" y m d)
  | From_day_month (m, d) -> Some (Printf.sprintf "--%02d-%02d" m d)
  | Per_object -> Some "variable"

let gran_name g = Ir_v0.Spelling.encode Ir_v0.Spelling.granularity g

(* the period of grid [gr] that holds day [d]: the calendar bucket, or the
   period a fixed start point lays, every wall computed from the start point
   itself (ch. 3.2). A grid of several units and a variable start lay no
   period here. *)
let period (gr : grid) d =
  if gr.count <> 1 then
    fault "a multi-period grid (%d %s periods) carries no operation (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12)"
      gr.count (gran_name gr.gran);
  match (gr.start, gr.gran) with
  | On_calendar, g -> calendar_bucket g d
  | Per_object, _ ->
      fault
        "a variable-start grid is laid per object from the anchor its start-point determination binds, which \
         this interpreter does not execute (ch. 3.2)"
  | From_date a, Week ->
      let k = fdiv (d - a) 7 in
      (a + (7 * k), a + (7 * (k + 1)))
  | From_day_month (m, dd), ((Month | Quarter | Year) as g) ->
      (* any year that has day [dd] of month [m] lays the same walls; 2000 has
         every such day *)
      let anchor = days_from_civil ~y:2000 ~m ~d:dd in
      let step = months_of g in
      let wall k = add_months anchor (step * k) in
      let yd, md, _ = civil_from_days d in
      let rec settle k = if wall k > d then settle (k - 1) else if wall (k + 1) <= d then settle (k + 1) else k in
      let k = settle (fdiv (((yd - 2000) * 12) + (md - m)) step) in
      (wall k, wall (k + 1))
  | (From_date _ | From_day_month _), _ ->
      fault "a fixed start point of this form lays no %s grid of one unit (WF-115)" (gran_name gr.gran)

(* --- the calendar measure (ch. 3.6) ------------------------------------- *)

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

(* Σ over the days of [van, up_to) of 1/len, len the length of the unit that
   [unit_of] says holds the day *)
let share_of_units unit_of van up_to =
  let rec go acc d =
    if d >= up_to then acc
    else
      let first, next = unit_of d in
      let stop = min next up_to in
      go (Q.add acc (Q.make (Z.of_int (stop - d)) (Z.of_int (next - first)))) stop
  in
  go Q.zero van

(* the calendar month, quarter or year that holds day [d] *)
let calendar_unit (tu : Ir_v0.Checks.time_unit) d =
  match tu with
  | Tu_month -> calendar_bucket Month d
  | Tu_year -> calendar_bucket Year d
  | Tu_quarter ->
      let y, m, _ = civil_from_days d in
      let first = days_from_civil ~y ~m:((((m - 1) / 3) * 3) + 1) ~d:1 in
      (first, add_months first 3)
  | tu -> fault "the calendar lays no %s of its own that a day lies in (ch. 3.6)" (time_unit_name tu)

(* how many [tu] the days of [van, up_to) make by the calendar: a day is one dg
   and a seventh of a wk, and 1/len of the calendar month, quarter or year it
   lies in, len its actual length in days *)
let calendar_measure (tu : Ir_v0.Checks.time_unit) van up_to : Q.t =
  if up_to <= van then Q.zero
  else
    match tu with
    | Tu_day -> Q.of_int (up_to - van)
    | Tu_week -> Q.make (Z.of_int (up_to - van)) (Z.of_int 7)
    | Tu_month | Tu_quarter | Tu_year -> share_of_units (calendar_unit tu) van up_to
    | tu -> fault "the calendar gives a %s no length in days (ch. 3.6)" (time_unit_name tu)

(* how many units of grid [gr] the days of [van, up_to) make: a day counts 1/len
   of the period of the grid that holds it *)
let grid_measure (gr : grid) van up_to : Q.t =
  if up_to <= van then Q.zero
  else
    match gr with
    | { start = On_calendar; count = 1; gran } ->
        calendar_measure (Ir_v0.Checks.granularity_time_unit gran) van up_to
    | _ -> share_of_units (period gr) van up_to

(* The unit a grid of grain [g] counts in, looked up among the document's
   declared units by the ch. 3.8 spelling table (ch. 3.6): the declared
   factors are what lets a rate's denominator cancel against the grid. [Error]
   says why there is none: no such unit, or two that differ in canonical or
   factor. *)
let grid_unit (doc : document) (g : grain) : (measure_unit, string) result =
  let tu = Ir_v0.Checks.granularity_time_unit g in
  let candidates = Ir_v0.Checks.grid_candidates doc g in
  let key (f : unit_factor) =
    (Option.map Ir_v0.Checks.norm f.canonical_form, Option.map Factors.of_rational f.factor_to_canonical)
  in
  let keys =
    List.fold_left (fun acc f -> let k = key f in if List.exists (fun x -> fst x = fst k && Option.equal Q.equal (snd x) (snd k)) acc then acc else k :: acc) [] candidates
  in
  match (candidates, keys) with
  | f :: _, [ _ ] -> Ok (Ir_v0.Units.of_declared f)
  | _, [] ->
      Error
        (Printf.sprintf
           "the document declares no %s unit, so this timeline's grid has no unit to integrate against (ch. 3.6)"
           (time_unit_name tu))
  | _, ks ->
      Error
        (Printf.sprintf
           "the document declares %d different %s units, so this timeline's grid has no single unit to \
            integrate against (ch. 3.6)"
           (List.length ks) (time_unit_name tu))
