# jutterswet-vast-startpunt — a timeline with a fixed start point off the calendar

The positive witness of WF-115 and the executed example of
`RUNTIME-SEMANTICS.md` ch. 3.2, "Fixed starting points": timelines whose
periods start where the calendar's do not, carried on the wire in
`fixedStart` (`spec/IR-SPEC.md` ch. 2.8) and bucketed on by all three
executors. The storm year is a construct of this golden, not an article of the
Jutterswet, whose penningjaar is the calendar year (art. 3b).

## Source (`source.rgs`) and wire

- `Tijdlijn het stormjaar heeft tijdvakken van een jaar startend op dag 1 van
  maand 10` — a named year grid from 1 October:
  `{"granularity": "year", "count": 1, "fixedStart": "--10-01"}`.
- `stormwachtdagen` and `stormtoelage`, both `voor elke stormjaar`: their
  facets adopt the declaration's start point with its granularity and count,
  `fixedStart: "--10-01"` beside `timelineName: "stormjaar"` (WF-115 (c)).
- `maandwachtdagen`, `voor elke maand startend op dag 31 van de maand`: an
  inline month grid on the 31st, `fixedStart: "--01-31"` — the canonical
  day-month names January, the earliest month with a wall on day 31. In a
  shorter month the wall falls on its last day.
- `wachtplichtig`, `voor elke week startend op 30-9-2024`: the date lexeme on a
  characteristic facet, `fixedStart: "2024-09-30"`. No executor reads a
  characteristic's grid (its timeline is its true periods, day-precise); the
  facet carries it so the declaration is written whole.
- `toelagesom`, a scalar.
- `maandtoelage`, `voor elke maand startend op dag 31 van de maand`: the same
  inline month grid as `maandwachtdagen`, supplied as a scalar case value;
  `maandtoelagesom`, a scalar.
- `weektoelage`, `voor elke week startend op 30-9-2024` (`fixedStart:
  "2024-09-30"`, a Monday), and `kwartaaltoelage`, `voor elk kwartaal startend
  op dag 31 van maand 2, 5, 8 en 11` (`fixedStart: "--05-31"`, the canonical
  spelling: May is the earliest month of that series with a 31st), both
  supplied as scalar case values; `weektoelagesom` and `kwartaaltoelagesom`,
  scalars.
- `stormpremie` and `maandpremie`, scalars.
- Eight rules: the day count "het aantal dagen in het jaar dat hij
  wachtplichtig is" into `stormwachtdagen`, the same count "in de maand" into
  `maandwachtdagen` (ch. 3.8; each stated bucket equals its target's
  granularity, WF-058), "het totaal van zijn stormtoelage gedurende de tijd
  dat hij wachtplichtig is" into `toelagesom` (ch. 3.6), "het totaal van zijn
  maandtoelage" into `maandtoelagesom` and the same total of `weektoelage` and
  of `kwartaaltoelage` into their sums: the subject's own total over the
  asOfDate bucket, where a scalar case value is the value of the period of the
  member's grid that holds the evaluation date (ch. 3.2, 3.6); and two
  whole-period tests over the day counts (ch. 3.9): `stormpremie` is 50
  "indien zijn stormwachtdagen gedurende het gehele jaar gevuld is", and
  `maandpremie` is 20 "indien zijn maandwachtdagen gedurende de gehele maand
  groter is dan 0".

The qualifier folds over the calendar year or month it names, not over the
grid of the tested member (ch. 3.2, 3.9). A day count on a fixed grid writes
whole periods of that grid, so its first and last period each reach into a
calendar bucket they cover only in part, and both tests fail on every
timeline that holds a period: the storm years start on 1 October and the
month grid on the 31st, never on the first day of a calendar bucket. They
pass only vacuously, on a timeline without periods.

Every case carries an evaluation date, because that total needs one: `ref`
faults without it ("needs the evaluation date"). Cases 1 to 4 supply no scalar
value, so their date changes none of their expectations.

The day count buckets on its target's periods: the October years for
`stormwachtdagen`, the months from the 31st (clamped) for `maandwachtdagen`.
The walls of the month grid are computed from the start point, never from the
previous wall: ... 2024-01-31, 2024-02-29, 2024-03-31, 2024-04-30, ...
2024-08-31, 2024-09-30, 2024-10-31, ... 2025-01-31, 2025-02-28, 2025-03-31, ...

## Cases (hand-computed)

**case1-stormjaargrens** — J1 wachtplichtig on [2024-09-28, 2024-10-03), five
days across 1 October; `stormtoelage` 100 on [2023-10-01, 2025-10-01), one
segment over two storm years.

- `stormwachtdagen`: 28, 29, 30 September fall in the storm year
  [2023-10-01, 2024-10-01) → **3**; 1 and 2 October in
  [2024-10-01, 2025-10-01) → **2**. On the calendar grid the same count is one
  segment of 5 on [2024-01-01, 2025-01-01).
- `maandwachtdagen`: 28 and 29 September in [2024-08-31, 2024-09-30) → **2**;
  30 September, 1 and 2 October in [2024-09-30, 2024-10-31) → **3**. The wall
  of 30 September is the 31st clamped.
- `toelagesom`: the clip leaves [2024-09-28, 2024-10-03), five days, zero
  whole years (ch. 3.6 floors per fragment) → **0**.

**case2-heel-stormjaar** — J1 wachtplichtig on [2024-10-01, 2025-10-01), one
whole storm year; `stormtoelage` 120 on the same period.

- `stormwachtdagen`: [2024-10-01, 2025-10-01) → **365** (October 2024 to
  September 2025 holds no 29 February).
- `maandwachtdagen`: thirteen periods, 30 + 30 + 31 + 31 + 28 + 31 + 30 + 31 +
  30 + 31 + 31 + 30 + 1 = 365: [2024-09-30, 2024-10-31) holds 1–30 October →
  30; the periods from 31 October to 30 September in full; and
  [2025-09-30, 2025-10-31) holds 30 September alone → 1. February 2025 runs
  [2025-01-31, 2025-02-28) → 28 and [2025-02-28, 2025-03-31) → 31.
- `toelagesom`: the clip keeps the whole segment, one whole year → 120 × 1 =
  **120**.

**case3-schrikkelmaand** — J1 wachtplichtig on [2024-02-27, 2024-03-02), across
the leap day; no `stormtoelage`.

- `maandwachtdagen`: 27 and 28 February in [2024-01-31, 2024-02-29) → **2**;
  29 February and 1 March in [2024-02-29, 2024-03-31) → **2**.
- `stormwachtdagen`: [2023-10-01, 2024-10-01) → **4**.
- `toelagesom`: an absent value timeline under the clip totals **0**, never
  leeg (ch. 3.6).

**case4-geen-wacht** — J1 without a wachtplichtig period; `stormtoelage` 120 on
[2024-10-01, 2025-10-01).

- Both day counts write no period: `stormwachtdagen` and `maandwachtdagen`
  are **empty** timelines.
- `toelagesom`: the clip over no period → **0**.
- `maandtoelagesom`, `weektoelagesom` and `kwartaaltoelagesom`: no scalar
  value, and the total of an absent member is **leeg** (ch. 3.6).
- `stormpremie` **50** and `maandpremie` **20**: the day counts hold no period,
  touch no bucket, and both tests pass vacuously.

**case5-geklemde-maand** — evaluation date 2024-02-15; `maandtoelage` 10 as a
scalar; J1 wachtplichtig on [2024-02-28, 2024-03-01).

- `maandtoelagesom`: the period of the `--01-31` grid that holds 2024-02-15 is
  [2024-01-31, 2024-02-29): its end is the 31st clamped to the leap day, and
  2024-01-31 plus one month is 2024-02-29 (ch. 3.8), so the period is one
  whole month (ch. 3.2, the weighting) → 10 × 1 = **10**. The duration from
  31-01-2024 to 29-02-2024 is 0 whole months (ch. 3.8); the total does not
  weigh by that rule.
- `maandwachtdagen`: 28 February in [2024-01-31, 2024-02-29) → **1**; 29
  February in [2024-02-29, 2024-03-31) → **1**.
- `stormwachtdagen`: [2023-10-01, 2024-10-01) → **2**.
- `toelagesom`: no `stormtoelage` under the clip → **0**.

**case6-heel-kalenderjaar** — evaluation date 2025-06-15; J1 wachtplichtig on
[2025-01-01, 2026-01-01), the calendar year 2025; nothing else.

- `stormwachtdagen`: 1 January to 30 September 2025 in [2024-10-01, 2025-10-01)
  → **273**; 1 October to 31 December in [2025-10-01, 2026-10-01) → **92**.
- `maandwachtdagen`: thirteen periods, 30 + 28 + 31 + 30 + 31 + 30 + 31 + 31 +
  30 + 31 + 30 + 31 + 1 = 365: [2024-12-31, 2025-01-31) holds 1–30 January →
  30, [2025-01-31, 2025-02-28) holds 31 January and 1–27 February → 28, and
  so on to [2025-12-31, 2026-01-31), which holds 31 December alone → 1.
- `toelagesom` **0** (no `stormtoelage`); `maandtoelagesom` **leeg** (no
  `maandtoelage`).
- `stormpremie` **leeg**: the period [2024-10-01, 2025-10-01) touches the
  calendar year 2024, which reads a value on 92 of its 366 days.
- `maandpremie` **leeg**: the period [2024-12-31, 2025-01-31) touches December
  2024, which reads a value (30) on 1 of its 31 days.

The case's own dates all lie in 2025, while the two periods reach into 2024
and 2026: the bucket of 2024 is one no case input touches.

**case7-week-en-kwartaal** — evaluation date 2025-02-15, a Saturday;
`maandtoelage` 10, `weektoelage` 7 and `kwartaaltoelage` 30 as scalars; J1
wachtplichtig on [2025-02-10, 2025-02-17).

- `weektoelagesom`: the week of the grid from Monday 2024-09-30 that holds
  2025-02-15 is [2025-02-10, 2025-02-17), one whole week → **7**. The calendar
  lays no week bucket; the fixed start point lays this one.
- `kwartaaltoelagesom`: the quarter of the `--05-31` grid that holds 2025-02-15
  is [2024-11-30, 2025-02-28), both walls the 31st clamped; 2024-11-30 plus
  three months is 2025-02-28, one whole quarter → **30**.
- `maandtoelagesom`: [2025-01-31, 2025-02-28), 2025-01-31 plus one month is
  2025-02-28, one whole month → **10**.
- `stormwachtdagen`: [2024-10-01, 2025-10-01) → **7**; `maandwachtdagen`:
  [2025-01-31, 2025-02-28) → **7**; `toelagesom` **0**.
- `stormpremie` **leeg** (the storm year touches 2024); `maandpremie`
  **leeg** (the month period touches January 2025 on its 31st alone).

## Executors

`ref`, clingo and both Drools branches replay the seven cases with these
values (measured 2026-09-26: `oracle: PASS` and `clingo: PASS` per case,
`PLAYED 7` in the exec-model branch and in the DRL-text branch). `ref` lays
the grid in one function (`Timeline.grid_bucket`), which the ASP backend's
bucket calendar and its total over the asOfDate bucket call as well; the
Drools branches hand the doel's constant anchor to the anchored producers a
variable start uses, and the case runner lays a case timeline on the fixed
grid through the frontend's shared `kern/FixedStartPoint`.

Case 5 was red in both Drools branches before the fix round of 2026-09-26,
which weighs a span by the clamped month step: measured on the union
`e6844fba`, `FAIL case5-geklemde-maand.json: attribuut maandtoelagesom:
verwacht 10, werkelijk 0` in each; the runtime counted whole months by the
duration's packed-month rule.

Case 6 was red in the ASP backend before the same round, which laid the
buckets of a whole-period fold between the case's own endpoints only:
measured on the union `e6844fba`, `MISMATCH: J1.stormpremie = [50], expected
leeg` and `MISMATCH: J1.maandpremie = [20], expected leeg`. The buckets of
2024 and of December 2024 were missing, and the fold passed on them
vacuously. The bucket span now reaches one period of every fixed grid
around each endpoint.

Case 7 was refused by the ASP backend before the same round, whose total over
the asOfDate bucket knew only the calendar's month and year: measured on the
union `e6844fba`, `TRANSLATION FAULT: temporal total: only month and year
granularity have a calendar bucket (ch. 3.2)`. Both Drools branches answered
it but weighed the two clamped periods as nothing: `FAIL
case7-week-en-kwartaal.json: attribuut maandtoelagesom: verwacht 10, werkelijk
0; attribuut kwartaaltoelagesom: verwacht 30, werkelijk 0`.

No case survives a no-op run: each expects a day count or a total the input
does not carry (gate 7). No annotation of this document carries a unit, so the
reftype harness twists nothing here (gate 13).

## The field is load-bearing (mutation test)

With every `fixedStart` removed the document is still verifier-clean — it then
describes calendar grids — and `ref` refuses every case at load, measured
2026-09-26:

    case1-stormjaargrens.json    CASE ERROR: J1.stormtoelage.timeline[0]: segment boundaries must sit on the declared granularity grid
    case2-heel-stormjaar.json    CASE ERROR: J1.stormtoelage.timeline[0]: segment boundaries must sit on the declared granularity grid
    case3-schrikkelmaand.json    CASE ERROR: expected J1.stormwachtdagen.timeline[0]: segment boundaries must sit on the declared granularity grid
    case4-geen-wacht.json        CASE ERROR: J1.stormtoelage.timeline[0]: segment boundaries must sit on the declared granularity grid
    case5-geklemde-maand.json    CASE ERROR: expected J1.stormwachtdagen.timeline[0]: segment boundaries must sit on the declared granularity grid
    case6-heel-kalenderjaar.json CASE ERROR: expected J1.stormwachtdagen.timeline[0]: segment boundaries must sit on the declared granularity grid
    case7-week-en-kwartaal.json  CASE ERROR: expected J1.stormwachtdagen.timeline[0]: segment boundaries must sit on the declared granularity grid

Before this card the frontend refused `source.rgs` at emission: the wire had no
field for the start point, and a timeline written without it lies on the
calendar grid.
