# jutterswet-gehele-periode-leeg — the leeg and gevuld tests under the whole-period qualifier

The corpus witness of `RUNTIME-SEMANTICS.md` ch. 3.9's leeg and gevuld
tests (decided 2026-09-26): one threshold, days(B), for both tests; a day
that reads leeg satisfies the leeg test and fails the gevuld test; the
inner negation per day, so the negated leeg test is the gevuld test and
the negated gevuld test is the leeg test; the qualifier's own negation on
the folded verdict. Hand-written, with no `bron.rgs`: two of the seven
rules carry the inner negation of the test, which RegelSpraak cannot
spell. The other five are written in RegelSpraak in the twin
`jutterswet-gehele-periode-leeg-rgs`, whose emission equals these five
rules, source snippets included, so gate 10 holds the form there.

## The shape

`bemanning` is a month-granular attribute of `Schuit`, `havengeld` a
month-granular attribute of `Werf`, and the fact type `ligplaats` gives a
Schuit at most one Werf over the to-one role `werf`. Seven rules on
Schuit, each writing a constant under one condition:

| target | value | condition |
|---|---|---|
| `leegtoeslag` | 10 | de bemanning gedurende het gehele jaar leeg |
| `vultoeslag` | 20 | de bemanning gedurende het gehele jaar gevuld |
| `nietleegtoeslag` | 30 | de bemanning gedurende het gehele jaar niet leeg (`negated: true`) |
| `nietvultoeslag` | 40 | de bemanning gedurende het gehele jaar niet gevuld (`negated: true`) |
| `onderbrekingstoeslag` | 50 | de bemanning niet gedurende het gehele jaar leeg (`WholePeriod.negated: true`) |
| `maandtoeslag` | 60 | de bemanning gedurende de gehele maand gevuld |
| `werftoeslag` | 70 | het havengeld van de werf gedurende het gehele jaar gevuld |

## The three cases, by hand

2025 and 2026 have 365 days each. January through June 2025 hold 181
days, July through December 2025 hold 184, January through June 2026 hold
181. A timeline that reads a value on no day touches no bucket and passes
every test; the qualifier's negation then fails.

**case1-geheel-leeg.** No Schuit carries a bemanning. S1 lies at W1, which
carries no havengeld; S2 lies at no Werf. Both Schuiten pass every test
without a touched bucket: 10, 20, 30, 40, 60 and 70, and
`onderbrekingstoeslag` stays leeg.

**case2-geheel-gevuld.** S1 carries 4 over [2025-01-01, 2026-01-01) and lies
at W1, whose havengeld is 12 over the same year. S2 carries 4 over
[2025-01-01, 2025-07-01) and 5 over [2025-07-01, 2027-01-01), two segments
that fill 2025 (181 + 184 days) and 2026 (365 days), and lies at no Werf.

| test | S1 | S2 |
|---|---|---|
| leeg | onwaar (the year holds values) | onwaar |
| gevuld | waar, 365 of 365 | waar, 365 of 365 in 2025 and in 2026 |
| niet leeg (the gevuld test) | waar | waar |
| niet gevuld (the leeg test) | onwaar | onwaar |
| niet gedurende … leeg | waar | waar |
| gevuld, month grid | waar, every month in full | waar |
| havengeld gevuld | waar, 365 of 365 | waar, no werf: no timeline |

Both: 20, 30, 50, 60 and 70; `leegtoeslag` and `nietvultoeslag` leeg.

**case3-gemengd.** S1 carries 4 over [2025-01-01, 2025-07-01) and lies at
W1, whose havengeld is 12 over the same half year. S2 carries 4 over
[2025-07-01, 2026-07-01) and lies at no Werf.

| test | S1 | S2 |
|---|---|---|
| leeg | onwaar (the first half holds values) | onwaar |
| gevuld | onwaar, 181 of 365 | onwaar, 184 of 365 in 2025, 181 of 365 in 2026 |
| niet leeg (the gevuld test) | onwaar | onwaar |
| niet gevuld (the leeg test) | onwaar | onwaar |
| niet gedurende … leeg | waar | waar |
| gevuld, month grid | waar, January to June in full | waar, July 2025 to June 2026 in full |
| havengeld gevuld | onwaar, 181 of 365 | waar, no werf: no timeline |

S1: 50 and 60. S2: 50, 60 and 70.

## Where the two negations give different answers

On case3 every one of the four year tests is onwaar, and the qualifier's
negation of the leeg test is waar. The inner negation asks every day for
the complementary value: "niet leeg" needs a value on every day of 2025
and fails on its second half. The qualifier's negation asks whether the
folded leeg test fails, and it does, because the first half holds values.
A reader that negated the folded verdict for the inner negation answers
30 and 40 on both Schuiten of case3, where this document pins leeg, and
leeg on both Schuiten of case1, where it pins 30 and 40 (measured on a
`ref` mutated that way: case1 and case3 fail, case2 passes, because on a
year filled in full the two readings agree). A reader that gave the
negated leeg test the touched days as its threshold, the other candidate
before the decision, asks only the days a segment covers; on case3 every
such day holds a value, so by hand it answers 30 on both Schuiten there as
well.

The month grid is the qualifier's own: `maandtoeslag` holds on both
Schuiten of case3, where the year grid fails them.

## The square

`ref` (gate 5), the ASP backend (gate 6) and the Drools backend in both
transpiler branches (gate 12, PLAYED 3 in each) replay all three cases with
these values. Each case pins derived values, so each refutes a do-nothing
executor (gate 7) without a survivor entry.

Measured before the card that decided the clause, on the same three cases:
`ref` and the ASP backend faulted with "whole-period leeg/gevuld test is
ch. 3.9 scope, not executed here", and both Drools branches reported an
`EXCLUDE loader: form not carried by the v0 Drools backend: …` line, the
loader's refusal of the unnegated whole-period test, which went with that
card.

## What the case files cannot carry

- An **explicit leeg segment**: the case format has no leeg value, and only
  ch. 3.5's temporal sum writes one. ch. 3.9 reads it as a gap that touches
  no bucket; `ref`'s suite and the Drools backend's (on the runtime fold)
  pin that.
- A timeline **off the declared month grid**, such as 4 over
  [2025-01-15, 2025-03-01): a case input sits on the grid. `ref`'s suite
  pins it.
- A time-dependent **parameter** as the operand: WF-043 admits it and no
  executor folds it. All three refuse it by name in one sentence, pinned in
  `ref`'s suite, the ASP backend's and the Drools backend's.
