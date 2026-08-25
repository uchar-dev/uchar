(* Exact arithmetic with the empty-value discipline.
   RUNTIME-SEMANTICS ch. 1.2 (empty per context), ch. 1.3 (the coalesce
   whitelist), ch. 2 (exact rationals, rounding, units, clamping).

   Every operator implements three things at once: the empty rule, the
   exact rational rule, and the unit rule. §9.5 inconsistencies (division
   by empty under a filled dividend, a negative radicand) are reported
   through the [err] callback — the value degrades to empty, the error
   surfaces beside it (ch. 1.4 point 4). Division by a numeric zero is a
   hard fault. *)

open Value

(* --- units (ch. 2.6) --------------------------------------------------- *)

let unit_name (e : Ir_v0.Ast.eenheid) =
  match e.afkorting with Some a -> a | None -> "?"

let show_unit = function
  | Unitless -> "(unitless)"
  | Unit e -> unit_name e
  | Product (a, b) -> unit_name a ^ "*" ^ unit_name b

(* The wire carries the canonical factor as a JSON number, which the
   loader hands us as a float. The intended value is the *decimal* the
   author wrote (0.01, 1000), not the nearest binary double — Q.of_float
   of 0.01 would poison every conversion with a 2^-n tail. Recover the
   decimal via the shortest round-tripping decimal form; a factor with no
   short decimal reading faults rather than degrading silently. *)
let q_of_wire_number f =
  let s = Printf.sprintf "%.15g" f in
  if String.exists (fun c -> c = 'e' || c = 'E') s || float_of_string s <> f then
    fault "unit conversion factor %h has no short decimal reading" f
  else Value.q_of_lexeme s

let factor (e : Ir_v0.Ast.eenheid) =
  match e.factor_naar_canoniek with None -> Q.one | Some f -> q_of_wire_number f

let convertible a b = Ir_v0.Checks.units_convertible a b

(* multiplicative factor taking a quantity in unit [a] to unit [b];
   both must be convertible (same canonical chain, rates recursively) *)
let rec conv_factor (a : Ir_v0.Ast.eenheid) (b : Ir_v0.Ast.eenheid) =
  let base = Q.div (factor a) (factor b) in
  match (a.noemer, b.noemer) with
  | None, None -> base
  | Some na, Some nb -> Q.mul base (conv_factor nb na)
  | _ -> fault "rate/plain unit mismatch: %s vs %s" (unit_name a) (unit_name b)

(* restate a number in unit [ub] into unit [ua] (ch. 2.6: the right operand
   is restated into the left operand's unit, with exact factors) *)
let restate_into ua q ub =
  match (ua, ub) with
  | Unitless, Unitless -> q
  | Unit a, Unit b when convertible a b -> Q.mul q (conv_factor b a)
  | Unit _, Unitless | Unitless, Unit _ ->
      (* unitless side adopts the other unit, value unchanged (the
         unitless-literal adoption of IR-SPEC ch. 2.5) *)
      q
  | _ -> fault "incompatible units: %s vs %s" (show_unit ua) (show_unit ub)

(* additive results carry the left unit; a unitless side adopts the other *)
let join_units ua ub = match ua with Unitless -> ub | _ -> ua

(* --- empty coalescing helpers ------------------------------------------ *)

let num_exn ctx = function
  | Num (q, u) -> (q, u)
  | v -> fault "%s: expected a number, got %s" ctx (to_string v)

(* coalesce empty to 0 (whitelisted contexts only, ch. 1.3) *)
let coalesce0 ctx = function
  | Empty -> (Q.zero, Unitless)
  | v -> num_exn ctx v

(* --- additive / multiplicative operators (ch. 1.2a + 2.2 + 2.6) -------- *)

let add l r =
  let lq, lu = coalesce0 "plus" l and rq, ru = coalesce0 "plus" r in
  Num (Q.add lq (restate_into lu rq ru), join_units lu ru)

(* min: both operands coalesce *)
let sub_min l r =
  let lq, lu = coalesce0 "min" l and rq, ru = coalesce0 "min" r in
  Num (Q.sub lq (restate_into lu rq ru), join_units lu ru)

(* verminderd met: left propagates, right coalesces (§6.3) *)
let sub_verminderd l r =
  match l with
  | Empty -> Empty
  | _ ->
      let lq, lu = num_exn "verminderd met" l in
      let rq, ru = coalesce0 "verminderd met" r in
      Num (Q.sub lq (restate_into lu rq ru), join_units lu ru)

let mul l r =
  let lq, lu = coalesce0 "maal" l and rq, ru = coalesce0 "maal" r in
  match (lu, ru) with
  | Unitless, u | u, Unitless -> Num (Q.mul lq rq, u)
  | Unit a, Unit b -> (
      match (a.noemer, b.noemer) with
      | Some n, None when convertible n b ->
          (* a rate times its denominator unit cancels: €/jr × jr = € *)
          Num (Q.mul lq (Q.mul rq (conv_factor b n)), Unit { a with noemer = None })
      | None, Some n when convertible n a ->
          Num (Q.mul rq (Q.mul lq (conv_factor a n)), Unit { b with noemer = None })
      | None, None -> Num (Q.mul lq rq, Product (a, b))
      | _ -> fault "unit product not modelled: %s * %s" (show_unit lu) (show_unit ru))
  | _ -> fault "unit product not modelled: %s * %s" (show_unit lu) (show_unit ru)

(* percentage van: l/100 × r, exactly; result carries the right unit *)
let pct_of l r =
  let lq, _ = coalesce0 "percentage van" l and rq, ru = coalesce0 "percentage van" r in
  Num (Q.mul (Q.div lq (Q.of_int 100)) rq, ru)

(* gedeeld door: dividend (left) tested first and coalesces; an empty
   divisor under a filled dividend propagates as error-empty (§6.5);
   division by numeric zero is a hard fault *)
let div_gen ~err ~collapse l r =
  match l with
  | Empty -> Num (Q.zero, Unitless)
  | _ -> (
      let lq, lu = num_exn "gedeeld door" l in
      match r with
      | Empty ->
          err "division by empty divisor";
          Empty
      | _ ->
          let rq, ru = num_exn "gedeeld door" r in
          if Q.equal rq Q.zero then fault "division by numeric zero"
          else
            let q, u =
              match (lu, ru) with
              | u, Unitless -> (Q.div lq rq, u)
              | Unit a, Unit b when convertible a b ->
                  (Q.div lq (Q.mul rq (conv_factor b a)), Unitless)
              | Unit a, Unit b when a.noemer = None && b.noemer = None ->
                  (Q.div lq rq, Unit { a with noemer = Some b })
              | Unitless, Unit _ | _ ->
                  fault "unit quotient not modelled: %s / %s" (show_unit lu) (show_unit ru)
            in
            collapse q u)

(* --- rounding (ch. 2.5): five modes, no banker's, default rekenkundig -- *)

let round_z mode (q : Q.t) =
  let num = Q.num q and den = Q.den q in
  match (mode : Ir_v0.Ast.afrond_modus) with
  | Naar_beneden -> Z.fdiv num den
  | Naar_boven -> Z.cdiv num den
  | Richting_nul -> Z.div num den
  | Weg_van_nul -> if Z.sign num >= 0 then Z.cdiv num den else Z.fdiv num den
  | Rekenkundig ->
      (* half away from zero on both signs: |x| -> floor(|x| + 1/2) *)
      let mag = Z.fdiv (Z.add (Z.mul (Z.abs num) (Z.of_int 2)) den) (Z.mul den (Z.of_int 2)) in
      if Z.sign num < 0 then Z.neg mag else mag

let round_q mode decimals q =
  let s = Z.pow (Z.of_int 10) decimals in
  Q.make (round_z mode (Q.mul q (Q.of_bigint s))) s

let round_v mode decimals = function
  | Empty -> Empty (* propagates — adopted v0 reading, ch. 1.2a *)
  | v ->
      let q, u = num_exn "afronding" v in
      Num (round_q mode decimals q, u)

(* abs-division variant: quotient truncated toward zero at 5 decimals —
   a defined collapse point (ch. 2.2) *)
let div_ ~err l r = div_gen ~err ~collapse:(fun q u -> Num (q, u)) l r
let div_abs ~err l r = div_gen ~err ~collapse:(fun q u -> Num (round_q Richting_nul 5 q, u)) l r

(* --- roots and powers (ch. 2.2) ---------------------------------------- *)

(* exact k-th root of a rational, when it exists *)
let root_exact q k =
  if Q.sign q < 0 then None
  else
    let root_z z =
      let r = Z.root z k in
      if Z.equal (Z.pow r k) z then Some r else None
    in
    match (root_z (Q.num q), root_z (Q.den q)) with
    | Some n, Some d -> Some (Q.make n d)
    | _ -> None

(* principal square root; a negative radicand yields empty + §9.5 error;
   an irrational root only exists under an explicit rounding (the mandatory
   rounding of §13.4.17.13) — Eval handles that pattern via [sqrt_rounded] *)
let sqrt_ ~err = function
  | Empty -> Empty
  | v -> (
      let q, _u = num_exn "wortel" v in
      if Q.sign q < 0 then (
        err "negative radicand";
        Empty)
      else
        match root_exact q 2 with
        | Some r -> Num (r, Unitless)
        | None ->
            fault
              "irrational square root of %s reached without an explicit rounding \
               (§13.4.17.13 mandates one)"
              (Q.to_string q))

(* round(√q, d, mode) computed exactly: candidate k = ⌊√q·10^d⌋ via integer
   comparisons (k²·den ≤ num·10^2d), tie decided by comparing q against the
   squared boundary — no approximation anywhere *)
let sqrt_rounded ~err mode decimals = function
  | Empty -> Empty
  | v ->
      let q, _u = num_exn "wortel" v in
      if Q.sign q < 0 then (
        err "negative radicand";
        Empty)
      else
        let s = Z.pow (Z.of_int 10) decimals in
        let target = Q.mul q (Q.of_bigint (Z.mul s s)) in
        (* k = floor(sqrt(target)) with exact correction *)
        let k = Z.sqrt (Z.div (Q.num target) (Q.den target)) in
        let le z = Q.leq (Q.of_bigint (Z.mul z z)) target in
        let k = if le (Z.succ k) then Z.succ k else k in
        let k = if le k then k else Z.pred k in
        let exact = Q.equal (Q.of_bigint (Z.mul k k)) target in
        let chosen =
          match (mode : Ir_v0.Ast.afrond_modus) with
          | Naar_beneden | Richting_nul -> k
          | Naar_boven | Weg_van_nul -> if exact then k else Z.succ k
          | Rekenkundig ->
              if exact then k
              else
                (* compare √target with k + 1/2 via target vs (2k+1)²/4 *)
                let two_k1 = Z.succ (Z.mul k (Z.of_int 2)) in
                let half_sq = Q.make (Z.mul two_k1 two_k1) (Z.of_int 4) in
                if Q.geq target half_sq then Z.succ k else k
        in
        Num (Q.make chosen s, Unitless)

(* tot de macht: exact integer power (negative exponent inverts first);
   a fractional exponent must denote an exact root, else it faults (the
   ch. 2.1 OPEN — v0 approximates in binary double; ref/ refuses) *)
let pow_ l r =
  match (l, r) with
  | Empty, _ | _, Empty -> Empty (* base and runtime exponent both propagate *)
  | _ ->
      let lq, _ = num_exn "tot de macht" l and rq, _ = num_exn "tot de macht" r in
      let to_int_exp z =
        match Z.to_int z with
        | e -> e
        | exception Z.Overflow -> fault "exponent %s is out of range" (Z.to_string z)
      in
      if Z.equal (Q.den rq) Z.one then
        let e = to_int_exp (Q.num rq) in
        let p q n = Q.make (Z.pow (Q.num q) n) (Z.pow (Q.den q) n) in
        Num ((if e >= 0 then p lq e else Q.inv (p lq (-e))), Unitless)
      else
        let a = to_int_exp (Z.abs (Q.num rq)) and b = to_int_exp (Q.den rq) in
        let base = Q.make (Z.pow (Q.num lq) a) (Z.pow (Q.den lq) a) in
        let base = if Z.sign (Q.num rq) < 0 then Q.inv base else base in
        (match root_exact base b with
        | Some r -> Num (r, Unitless)
        | None ->
            fault "fractional power %s^%s is not exactly representable (ch. 2.1 OPEN)"
              (Q.to_string lq) (Q.to_string rq))

let abs_ = function
  | Empty -> Empty
  | v ->
      let q, u = num_exn "absolute waarde" v in
      Num (Q.abs q, u) (* type — unit, precision, sign — passes through *)

(* --- clamping (ch. 2.7): minimum first, absent bound is no bound ------- *)

let clamp ~min_ ~max_ v =
  match v with
  | Empty -> Empty
  | _ ->
      let q, u = num_exn "begrensd" v in
      let bound ctx b =
        match b with
        | Empty -> None
        | _ ->
            let bq, bu = num_exn ctx b in
            Some (restate_into u bq bu)
      in
      let q = match bound "minimum" min_ with Some m when Q.lt q m -> m | _ -> q in
      let q = match bound "maximum" max_ with Some m when Q.gt q m -> m | _ -> q in
      Num (q, u)

(* --- comparisons (ch. 1.2d + ch. 2.6) ---------------------------------- *)

let q_cmp l r =
  let lq, lu = num_exn "vergelijking" l and rq, ru = num_exn "vergelijking" r in
  Q.compare lq (restate_into lu rq ru)

(* is this comparison numeric? decides which empty-equality rule applies *)
let is_numeric_pair l r =
  match (l, r) with
  | (Num _ | Date _ | Date_time _), _ | _, (Num _ | Date _ | Date_time _) -> true
  | _ -> false

let scalar_cmp l r =
  match (l, r) with
  | Num _, Num _ -> q_cmp l r
  | Date a, Date b -> compare (a : int) b
  | Date_time a, Date_time b -> compare (a : int) b
  | _ -> fault "unordered comparison: %s vs %s" (to_string l) (to_string r)

(* value equality: idempotent writes (ch. 5.1 clause 2) and the structural,
   nominal equality of constructor values (ch. 9.3). It is [rec] for the
   nested-constructor payload; a payload is never leeg, so this agrees with
   the Tabel 38 reading [eq_non_numeric] uses for the same values. *)
let rec equal a b =
  match (a, b) with
  | Empty, Empty -> true
  | Num (qa, ua), Num (qb, ub) -> (
      match (ua, ub) with
      | Unitless, Unitless -> Q.equal qa qb
      | Unit x, Unit y when convertible x y -> Q.equal qa (Q.mul qb (conv_factor y x))
      | Unit _, Unitless | Unitless, Unit _ -> Q.equal qa qb
      | _ -> false)
  | Text a, Text b -> String.equal a b
  | Bool a, Bool b -> Bool.equal a b
  | Date a, Date b -> Int.equal a b
  | Date_time a, Date_time b -> Int.equal a b
  | Enum a, Enum b -> String.equal (Ir_v0.Checks.norm a) (Ir_v0.Checks.norm b)
  (* ch. 9.3: tags equal (ci-normalised) and, for unary tags, payloads
     equal under their own value equality — structural and nominal *)
  | Ctor (ta, pa), Ctor (tb, pb) ->
      String.equal (Ir_v0.Checks.norm ta) (Ir_v0.Checks.norm tb)
      && (match (pa, pb) with
          | None, None -> true
          | Some va, Some vb -> equal va vb
          | _ -> false)
  (* ch. 10.1: list equality is elementwise and in order; a position is
     never leeg, so this agrees with the Tabel 38 reading below *)
  | Lijst xs, Lijst ys -> List.length xs = List.length ys && List.for_all2 equal xs ys
  (* ch. 11.2: record equality is fieldwise and structural — positional over
     the shared declaration order, field names matched ci; a field is never
     leeg, so this agrees with the Tabel 38 reading below. WF-072 guarantees
     two compared records name the same declaring domain (same fields, order). *)
  | Record fa, Record fb ->
      List.length fa = List.length fb
      && List.for_all2
           (fun (na, va) (nb, vb) ->
             String.equal (Ir_v0.Checks.norm na) (Ir_v0.Checks.norm nb) && equal va vb)
           fa fb
  | _ -> false

let eq_non_numeric l r =
  (* §5 Tabel 38: any empty operand → false; exact value equality else.
     Enum identity goes through the verifier's normalisation, like every
     other declared name. Constructor values use the ch. 9.3 structural
     equality; a mixed pair (e.g. a constructor against a number) stays the
     existing mixed-type fault. *)
  match (l, r) with
  | Empty, _ | _, Empty -> false
  | Text a, Text b -> String.equal a b
  | Bool a, Bool b -> Bool.equal a b
  | Enum a, Enum b -> String.equal (Ir_v0.Checks.norm a) (Ir_v0.Checks.norm b)
  | (Ctor _ as a), (Ctor _ as b) -> equal a b
  | (Lijst _ as a), (Lijst _ as b) -> equal a b
  (* ch. 11.2: record values are non-numeric; a leeg operand already fell to
     false above, else the fieldwise structural equality of [equal] *)
  | (Record _ as a), (Record _ as b) -> equal a b
  | _ -> fault "mixed-type equality: %s vs %s" (to_string l) (to_string r)

(* [static_numeric] carries the comparison's statically declared kind:
   the ch. 1.2d empty-equality asymmetry (leeg = leeg is waar numerically,
   onwaar non-numerically) is decided by the operand *types*, which two
   empty runtime values cannot reveal. Absent static information, two
   empties follow the numeric rule. [static_date] narrows the numeric rule
   to the ch. 1.2d date row: v0's Tabel-38 guard on the date-vs-date shape
   (`cmp && !(l == null && r == null)`) makes a both-leeg gelijk-aan
   onwaar — never the numeric two-empties-equal cell — while ongelijk-aan
   keeps the numeric cells (both readings already agree there). *)
let compare_op ?static_numeric ?(static_date = false) (op : Ir_v0.Ast.vergelijk_op) l r =
  let ordering cmp =
    (* any empty operand → false *)
    match (l, r) with Empty, _ | _, Empty -> false | _ -> cmp (scalar_cmp l r) 0
  in
  let numeric =
    match static_numeric with
    | Some b -> b
    | None -> ( match (l, r) with Empty, Empty -> true | _ -> is_numeric_pair l r)
  in
  match op with
  | Kleiner | Eerder -> ordering ( < )
  | Kleiner_of_gelijk | Eerder_of_gelijk -> ordering ( <= )
  | Groter | Later -> ordering ( > )
  | Groter_of_gelijk | Later_of_gelijk -> ordering ( >= )
  | Gelijk ->
      if numeric then
        (* numeric equality: true iff both empty, else value equality —
           except the date row, where both-empty is onwaar (the v0 guard) *)
        match (l, r) with
        | Empty, Empty -> not static_date
        | Empty, _ | _, Empty -> false
        | _ -> scalar_cmp l r = 0
      else eq_non_numeric l r
  | Ongelijk ->
      if numeric then
        (* true iff exactly one empty, else value inequality *)
        match (l, r) with
        | Empty, Empty -> false
        | Empty, _ | _, Empty -> true
        | _ -> scalar_cmp l r <> 0
      else
        (* non-numeric inequality with an empty operand: false (the
           three-valued reading of Tabel 38's equality, mirrored) *)
        (match (l, r) with Empty, _ | _, Empty -> false | _ -> not (eq_non_numeric l r))

(* --- aggregations (ch. 1.2e: the §5.8.2/§4.20 split) ------------------- *)

(* empty contributors are skipped, never counted as 0 *)
let filled vs = List.filter (fun v -> v <> Empty) vs

let agg_som ~fallback vs =
  match filled vs with
  | [] -> ( match fallback with Some f -> f | None -> Empty)
  | first :: rest -> List.fold_left add first rest

let agg_totaal vs =
  match filled vs with
  | [] -> Num (Q.zero, Unitless) (* 0 unconditionally, §4.20 *)
  | first :: rest -> List.fold_left add first rest

let agg_aantal vs = Num (Q.of_int (List.length vs), Unitless)

let agg_extreme keep vs =
  match filled vs with
  | [] -> Empty (* no fallback form exists *)
  | first :: rest ->
      List.fold_left (fun acc v -> if keep (scalar_cmp v acc) then v else acc) first rest

let agg_min vs = agg_extreme (fun c -> c < 0) vs
let agg_max vs = agg_extreme (fun c -> c > 0) vs
let agg_eerste vs = match filled vs with [] -> Empty | v :: _ -> v
let agg_laatste vs = match filled vs with [] -> Empty | vs -> List.nth vs (List.length vs - 1)

(* --- date part extraction / date arithmetic (ch. 1.2b + ch. 3.8) ------- *)

let jaar_uit = function
  | Empty -> Empty
  | Date z -> let y, _, _ = civil_from_days z in Num (Q.of_int y, Unitless)
  | v -> fault "jaar uit: expected a date, got %s" (to_string v)

let maand_uit = function
  | Empty -> Empty
  | Date z -> let _, m, _ = civil_from_days z in Num (Q.of_int m, Unitless)
  | v -> fault "maand uit: expected a date, got %s" (to_string v)

let dag_uit = function
  | Empty -> Empty
  | Date z -> let _, _, d = civil_from_days z in Num (Q.of_int d, Unitless)
  | v -> fault "dag uit: expected a date, got %s" (to_string v)

(* eerste paasdag van a whole unitless year (ch. 3.8): the Gauss/Anonymous
   algorithm, verbatim from the spec; leeg propagates; a non-whole or
   unit-carrying year faults — the ratified delta: never a silent
   truncation to whole. OCaml (/) and (mod) truncate toward zero, exactly
   the ch. 3.8 quot/mod. *)
let eerste_paasdag = function
  | Empty -> Empty
  | Num (q, Unitless) when Z.equal (Q.den q) Z.one ->
      let j =
        match Z.to_int (Q.num q) with
        | j -> j
        | exception Z.Overflow ->
            fault "eerste paasdag: year %s is out of range" (Q.to_string q)
      in
      let a = j mod 19 and b = j / 100 and c = j mod 100 in
      let d = b / 4 and e = b mod 4 in
      let f = (b + 8) / 25 in
      let g = (b - f + 1) / 3 in
      let h = ((19 * a) + b - d - g + 15) mod 30 in
      let i = c / 4 and k = c mod 4 in
      let l = (32 + (2 * e) + (2 * i) - h - k) mod 7 in
      let m = (a + (11 * h) + (22 * l)) / 451 in
      let maand = (h + l - (7 * m) + 114) / 31 in
      let dag = ((h + l - (7 * m) + 114) mod 31) + 1 in
      Date (days_from_civil ~y:j ~m:maand ~d:dag)
  | v -> fault "eerste paasdag: expected a whole unitless year, got %s (WF-038)" (to_string v)

(* --- the calendar kernel (ch. 3.8): proleptic Gregorian, zoneless civil
   time, built on the civil helpers of Value (days_from_civil /
   civil_from_days). The counted unit arrives resolved through the shared
   spelling table (Ir_v0.Checks.time_unit_of_spelling) — declared factors
   and stelsel are irrelevant to calendar arithmetic. *)

let leap y = (y mod 4 = 0 && y mod 100 <> 0) || y mod 400 = 0

let month_length y m =
  match m with
  | 2 -> if leap y then 29 else 28
  | 4 | 6 | 9 | 11 -> 30
  | _ -> 31

(* the packed-month rule: packed(D) = (12·year + month − 1)·32 + day *)
let packed z =
  let y, m, d = civil_from_days z in
  ((((12 * y) + m) - 1) * 32) + d

(* proleptic-month split needs floor division (negative years) *)
let floor_div a b = if a >= 0 then a / b else -(((-a) + b - 1) / b)

(* date + n whole months, clamped to the target month's end (ch. 3.8) *)
let add_months n z =
  let y, m, d = civil_from_days z in
  let pm = (12 * y) + (m - 1) + n in
  let ty = floor_div pm 12 in
  let tm = (pm - (12 * ty)) + 1 in
  days_from_civil ~y:ty ~m:tm ~d:(min d (month_length ty tm))

(* whole units from A to B, signed, truncating toward zero (OCaml (/)
   truncates toward zero, exactly the ch. 3.8 quot) *)
let whole_units_between (tu : Ir_v0.Checks.time_unit) za zb =
  match tu with
  | Tu_dag -> zb - za
  | Tu_week -> (zb - za) / 7
  | Tu_maand -> (packed zb - packed za) / 32
  | Tu_kwartaal -> (packed zb - packed za) / 32 / 3
  | Tu_jaar -> (packed zb - packed za) / 32 / 12
  | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde ->
      fault "sub-day tijdsduur needs datum-tijd operands (day-precision values)"

let add_units (tu : Ir_v0.Checks.time_unit) n z =
  match tu with
  | Tu_dag -> z + n
  | Tu_week -> z + (7 * n)
  | Tu_maand -> add_months n z
  | Tu_kwartaal -> add_months (3 * n) z
  | Tu_jaar -> add_months (12 * n) z
  | Tu_uur | Tu_minuut | Tu_seconde | Tu_milliseconde ->
      fault "sub-day date arithmetic needs a datum-tijd operand (day-precision value)"

(* --- the datum-tijd millisecond layer (ch. 3.8): a datetime is its ms
   count since 1970-01-01T00:00:00 in the same zoneless civil time —
   every civil day is exactly 86 400 000 ms. The civil day and the
   time-of-day split by floor division, so the time-of-day is always
   0 ≤ msod < 86 400 000, also left of the epoch. *)

let ms_per_day = 86_400_000

let split_ms t =
  let d = floor_div t ms_per_day in
  (d, t - (d * ms_per_day))

let sub_day_ms : Ir_v0.Checks.time_unit -> int option = function
  | Tu_uur -> Some 3_600_000
  | Tu_minuut -> Some 60_000
  | Tu_seconde -> Some 1_000
  | Tu_milliseconde -> Some 1
  | _ -> None

(* datetime + n units: sub-day units are linear on the ms value; calendar
   units act on the civil day part through the day kernel above (linear
   days/weeks, the month-end clamp), preserving the time-of-day — the v0
   LocalDateTime mirror *)
let add_units_ms (tu : Ir_v0.Checks.time_unit) n t =
  match sub_day_ms tu with
  | Some u -> t + (n * u)
  | None ->
      let d, msod = split_ms t in
      (add_units tu n d * ms_per_day) + msod

(* whole units from datetime A to datetime B: a sub-day unit counts
   linearly on the ms difference (quot, toward zero); a day-based unit
   carries the BORROW rule — when B's civil day lies ahead of A's but its
   time-of-day lies strictly before A's, the last day is not yet complete,
   so one day is borrowed back off B's day part before the day-precision
   count above; mirrored on the negative side. v0's LocalDateTime
   counting — the classic silent-error corner (ch. 3.8). *)
let whole_units_between_ms (tu : Ir_v0.Checks.time_unit) ta tb =
  match sub_day_ms tu with
  | Some u -> (tb - ta) / u (* OCaml (/) truncates toward zero *)
  | None ->
      let da, msa = split_ms ta and db, msb = split_ms tb in
      let db =
        if db > da && msb < msa then db - 1
        else if db < da && msb > msa then db + 1
        else db
      in
      whole_units_between tu da db

(* a duration count is taken as whole units: a fractional count truncates
   toward zero (ch. 3.8); Z.div truncates toward zero *)
let whole_count_of q =
  match Z.to_int (Z.div (Q.num q) (Q.den q)) with
  | n -> n
  | exception Z.Overflow -> fault "duration count %s is out of range" (Q.to_string q)

(* date ± duration (ch. 3.8; empty rules ch. 1.2): empty date propagates;
   empty duration coalesces to 0 — the date unchanged, the only duration
   that coalesces. The non-empty step needs the resolved time unit. *)
let date_plus_duration ?(minus = false) ?unit_ d dur =
  match (d, dur) with
  | Empty, _ -> Empty
  | Date z, Empty -> Date z
  | Date_time t, Empty -> Date_time t
  | Date z, _ -> (
      let q, _u = num_exn (if minus then "min" else "plus") dur in
      let n = whole_count_of q in
      let n = if minus then -n else n in
      match unit_ with
      | Some tu -> Date (add_units tu n z)
      | None -> fault "date arithmetic without a resolved time unit (WF-035)")
  | Date_time t, _ -> (
      let q, _u = num_exn (if minus then "min" else "plus") dur in
      let n = whole_count_of q in
      let n = if minus then -n else n in
      match unit_ with
      | Some tu -> Date_time (add_units_ms tu n t)
      | None -> fault "date arithmetic without a resolved time unit (WF-035)")
  | v, _ -> fault "date arithmetic: expected a date, got %s" (to_string v)

(* tijdsduur van A tot B (ch. 3.8): whole declared units, signed; the
   absolute variant drops the sign; any empty operand propagates, never 0.
   The result carries the node's declared unit [result_unit]. *)
let tijdsduur ?(absolute = false) ?unit_ ?(result_unit = Unitless) a b =
  match (a, b) with
  | Empty, _ | _, Empty -> Empty
  | Date za, Date zb -> (
      match unit_ with
      | Some tu ->
          let n = whole_units_between tu za zb in
          Num (Q.of_int (if absolute then abs n else n), result_unit)
      | None -> fault "tijdsduur without a resolved time unit (WF-038)")
  | Date_time ta, Date_time tb -> (
      match unit_ with
      | Some tu ->
          let n = whole_units_between_ms tu ta tb in
          Num (Q.of_int (if absolute then abs n else n), result_unit)
      | None -> fault "tijdsduur without a resolved time unit (WF-038)")
  | (Date _, Date_time _ | Date_time _, Date _) ->
      fault "tijdsduur operands mix date precisions (WF-038)"
  | _ -> fault "tijdsduur: expected two dates, got %s and %s" (to_string a) (to_string b)

(* --- text (ch. 1.2c): empty coerces to "" ------------------------------ *)

let text_of = function
  | Empty -> ""
  | Text s -> s
  | Num (q, _) -> lexeme_of_q q
  | Bool b -> if b then "waar" else "onwaar"
  | Date z -> iso_of_date z
  | Date_time _ as v -> to_string v
  | Enum s -> s
  (* a constructor value has no text coercion — fail fast, never a
     silent stringification into a text join *)
  | Ctor _ as v -> fault "a constructor value has no text coercion: %s" (to_string v)
  (* a list value has no text coercion either — same fail-fast boundary *)
  | Lijst _ as v -> fault "a list value has no text coercion: %s" (to_string v)
  (* a record value has no text coercion either — same fail-fast boundary *)
  | Record _ as v -> fault "a record value has no text coercion: %s" (to_string v)

let concat vs = Text (String.concat "" (List.map text_of vs))
