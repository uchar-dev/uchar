# jutterswet-ploegpremie — the quantifier fold over a linked set

The §8.2 sentence shape, in its own Jutterswet casus:

```
Een Bergingsploeg is premiegerechtigd indien de dagvergoeding van ALLE
bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40.
Een Bergingsploeg is wisselvallig indien de dagvergoeding van GEEN VAN DE
bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40.
```

A forall (and its GEEN twin) over the members of a linked set, each member
tested on its own whole-period timeline value against a scalar bound.
`IR-SPEC.md` ch. 4.5 (`QuantifierFold`, WF-108); semantics
`RUNTIME-SEMANTICS.md` ch. 3.9a, whose worked-example table this golden pins
cell for cell.

## Why two rules and not one

`ALLE` and `GEEN` are the two v0 quantifiers and the corpus carries both in one
document on purpose. They are not each other's negation: `GEEN` complements the
matrix PER MEMBER, so the cases below can hold a ploeg for which neither
characteristic is attributed (case 2 and 3 — one berger is above the bound, so
`ALLE` fails; another is not, so `GEEN` fails too) and one for which both are
(case 4, the empty set, vacuous under both). A reader that computed `GEEN` as
`not ALLE` fails case 2, case 3 and case 4 alike.

## The six cells

| case | the ploeg's bergers | premiegerechtigd | wisselvallig |
|---|---|---|---|
| 1 | 50 and 60, each the whole of 2026 | **ja** | nee |
| 2 | 50 and 30, each the whole of 2026 | nee | nee |
| 3 | 50 the whole year; 60 on the first half only | nee | nee |
| 4 | none | **ja** (vacuous) | **ja** (vacuous) |
| 5 | 40, the whole of 2026 | nee (strictly greater) | **ja** |
| 6 | 50 the whole year; one berger with no timeline at all | **ja** | nee |

Cases 1–5 are the five expectations the production toolchain pins for this
form, in a casus of our own. Case 6 is the corpus's own addition and it is the
one a reader is most likely to get wrong: **an absent timeline is not a gap.**
A member with no segments touches no calendar bucket and passes the
whole-period test vacuously (ch. 3.9); a member whose timeline covers only part
of the year touches the 2026 bucket and falls short of its 365 days, so it
fails (case 3). The two sit side by side here so that an executor which
collapses them fails by name rather than by accident.

`asOfDate` is 2026-03-15 in every case and is **irrelevant to the verdict** —
deliberately so. The whole-period fold takes its buckets from the timeline, not
from the evaluation date; a March asOfDate over a January–July timeline would
read "covered" on any point-in-time reading, and reads `onwaar` here.

`dagvergoeding` is declared month-granular while the qualifier's grid is the
YEAR. That independence is ch. 3.9's ("the grid is the qualifier's own") and the
casus carries it rather than hiding it behind a matching pair.
