# jutterswet-rekenjaar — the execution year as a value and in a condition

The carrier of the context value `evaluationYear` (RUNTIME-SEMANTICS
ch. 8.2): the calendar year of the evaluation date, a whole number
without a unit. Art. 3b of the Jutterswet makes the penningjaar the
calendar year, and art. 3a keeps a find that is reported in a later
penningjaar than the one it was found in out of the count. One `Vondst`
with three whole-number attributes and one characteristic, and three
rules:

    Het meldjaar van een Vondst moet gesteld worden op het Rekenjaar.

    Het aantal verstreken penningjaren van een Vondst moet berekend worden als het Rekenjaar min zijn vondstjaar
    indien zijn vondstjaar kleiner of gelijk is aan het Rekenjaar.

    Een Vondst is laat gemeld
    indien het Rekenjaar groter is dan zijn vondstjaar.

The node stands in four positions: as a rule value, as the left operand
of a subtraction, as the right operand of a comparison and as the left
operand of a comparison. The source is `source.rgs`; gate 10 re-emits this
document from it byte-identically.

## Hand computation (absolute)

Both cases carry the same four finds; only the session's evaluation date
differs, by one day, across the turn of the year.

| Vondst | `vondstjaar` | case1-oudjaarsdag (2026-12-31): `meldjaar` / `aantal verstreken penningjaren` / `laat gemeld` | case2-nieuwjaarsdag (2027-01-01): the same three |
|---|---|---|---|
| V1 | 2026 | **2026** / **0** / absent | **2027** / **1** / present |
| V2 | 2025 | **2026** / **1** / present | **2027** / **2** / present |
| V3 | leeg | **2026** / **leeg** / absent | **2027** / **leeg** / absent |
| V4 | 2027 | **2026** / **leeg** / absent | **2027** / **0** / absent |

V1 is the row the golden exists for: the same input one day later moves
all three outcomes, because the year follows the calendar and not a count
of days. V3 and V4 give the leeg class: a comparison with a leeg
`vondstjaar` is false, and so is 2027 ≤ 2026, so the computation does not
fire. Both cases end `consistent`.

## Measured (2026-09-26)

- verifier: `verifier: PASS`.
- `ref`: `oracle: PASS (13 expectations)` for each case.
- ASP backend: `clingo: PASS (13 expectations)` for each case.
- Drools: `PLAYED 2` in the exec-model branch and `PLAYED 2` in the
  DRL-text branch.
- Before this golden's card every executor refused the document by name:
  `ref` at evaluation, the ASP backend at translation, and both Drools
  branches before lowering (an EXCLUDE), each with one sentence for the
  whole node.

## What it pins

The year boundary. With each executor reading the year of the day before
the evaluation date (in `ref` and the ASP backend the day number minus
one, in the Drools runtime `minusDays(1)`), case1 still passes, since
2026-12-30 lies in 2026 as well, and case2 fails in all of them. `ref`
answers `MISMATCH: V1.meldjaar = 2026, expected 2027` and seven more
mismatches, the ASP backend `MISMATCH: V1.meldjaar = [2026], expected 2027`
and seven more, and both Drools branches `FAIL case2-nieuwjaarsdag.json:
attribuut meldjaar: verwacht 2027, werkelijk 2026; …`.

The absent date is not a golden: a session without `asOfDate` faults in
every executor ("the evaluationYear context value needs an evaluation
date; the case carries asOfDate null"), which `ref/test` (the ch. 8.2
worked example), the ASP backend's own suite and the Drools backend's
`EvaluationYearWitnessTest` pin, and gate 8 for all three executors through the
red case `corpus/red/evaluation-year-without-evaluation-date` (this document with a
dateless case).

## noop refutation (gate 7)

No derived attribute is in the input world, so the `--noop` run fails on
both cases: case1 on seven expectations (`MISMATCH: V1.meldjaar = leeg,
expected 2026` first) and case2 on nine (`MISMATCH: V1.meldjaar = leeg,
expected 2027` first). Not a survivor.
