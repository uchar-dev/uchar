# wf115-fixed-start-point-outside-grid

Violates WF-115 arm (b) (`spec/IR-SPEC.md` ch. 2.8): a fixed start point in the
lexeme its grid does not take.

    WF-115 FAIL /model/objectTypes/0/attributes/1/timeline/fixedStart: fixed
    start point '2024-01-31' does not fit a month grid of count 1: a month,
    quarter or year grid of one unit starts on a day-month, written --mm-dd, a
    day of month mm in a leap year

A month grid of one unit repeats every month, so its start point is a day of
the month, not a date: the date names one of its walls and leaves the reader to
guess which day-month it meant. The canonical spelling of this grid is
`--01-31`, which the golden carries.

The document is `corpus/golden/jutterswet-vast-startpunt` with the
`fixedStart` of the facet of `maandwachtdagen` set from `--01-31` to
`2024-01-31`. `expect.txt` pins the arm. The arm's other reasons — no start
point on a day grid of one unit, a date on a week grid or a grid of several
units, `--01-01` as the calendar's own grid, the canonical month — are pinned
sentence by sentence in `ref/test` (`WF-115-b-*`), since the interpreter's
loader refuses in them.

Red first (measured 2026-09-26 before the arm existed): `verifier: PASS`.
Mutation test: with arm (b) removed from the verifier this witness passes and
the two other WF-115 witnesses still fail on their own arms.
