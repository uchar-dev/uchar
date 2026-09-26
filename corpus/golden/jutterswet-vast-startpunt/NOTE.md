# jutterswet-vast-startpunt — a timeline with a fixed start point off the calendar

The positive witness of WF-115 and the executed example of
`RUNTIME-SEMANTICS.md` ch. 3.2, "Fixed starting points": timelines whose
periods start where the calendar's do not, carried on the wire in
`fixedStart` (`spec/IR-SPEC.md` ch. 2.8) and bucketed on by all three
executors. The storm year is a construct of this golden, not an article of the
Jutterswet, whose penningjaar is the calendar year (art. 3b).

## Source (`bron.rgs`) and wire

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
- Three rules: the day count "het aantal dagen in het jaar dat hij
  wachtplichtig is" into `stormwachtdagen`, the same count "in de maand" into
  `maandwachtdagen` (ch. 3.8; each stated bucket equals its target's
  granularity, WF-058), and "het totaal van zijn stormtoelage gedurende de tijd
  dat hij wachtplichtig is" into `toelagesom` (ch. 3.6).

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

## Executors

`ref`, clingo and both Drools branches replay the four cases with these
values (measured 2026-09-26: `oracle: PASS (4 expectations)` and
`clingo: PASS (4 expectations)` per case, `PLAYED 4` in the exec-model branch
and in the DRL-text branch). `ref` lays the grid in one function
(`Timeline.grid_bucket`), which the ASP backend's bucket calendar calls as
well; the Drools branches hand the doel's constant anchor to the anchored
producers a variable start uses, and the case runner lays a case timeline on
the fixed grid through the frontend's shared `kern/VastStartpunt`.

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

Before this card the frontend refused `bron.rgs` at emission: the wire had no
field for the start point, and a timeline written without it lies on the
calendar grid.
