# jutterswet-ploegpremie — the quantifier fold over a linked set

The §8.2 sentence shape, in its own Jutterswet casus:

```
Een Bergingsploeg is premiegerechtigd indien de dagvergoeding van ALLE
bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40.
Een Bergingsploeg is wisselvallig indien de dagvergoeding van GEEN VAN DE
bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40.
```

A forall (and its `none` twin) over the members of a linked set, each member
tested on its own whole-period timeline value against a scalar bound.
`IR-SPEC.md` ch. 4.5 (`QuantifierFold`, WF-108); semantics
`RUNTIME-SEMANTICS.md` ch. 3.9a, whose worked-example table this golden pins
cell for cell.

## Why two rules and not one

`all` and `none` are the two quantifiers this document runs — its counting
sibling `jutterswet-reddingsvloot` runs the other three over the same §8.2
matrix — and the corpus carries both in one document on purpose. They are
not each other's negation: `none` complements the
matrix PER MEMBER, so the cases below can hold a ploeg for which neither
characteristic is attributed (case 2 and 3 — one berger is above the bound, so
`all` fails; another is not, so `none` fails too) and one for which both are
(case 4, the empty set, vacuous under both). A reader that computed `none` as
`not all` fails case 2, case 3 and case 4 alike.

## The seven cells

| case | the ploeg's bergers | premiegerechtigd | wisselvallig |
|---|---|---|---|
| 1 | 50 and 60, each the whole of 2026 | **ja** | nee |
| 2 | 50 and 30, each the whole of 2026 | nee | nee |
| 3 | 50 the whole year; 60 on [2026-01-01, 2026-07-01) only | nee | nee |
| 4 | none | **ja** (vacuous) | **ja** (vacuous) |
| 5 | 40, the whole of 2026 | nee (strictly greater) | **ja** |
| 6 | 50 the whole year; one berger with no timeline at all | **ja** | nee |
| 7 | one berger, no timeline at all | **ja** | nee |

Cases 1–5 are the five expectations the production toolchain pins for this
form, in a casus of our own. Cases 6 and 7 are the corpus's own addition and
they pin the thing a reader is most likely to get wrong: **an absent timeline
is not a gap.** A member with no segments touches no calendar bucket and passes
the whole-period test vacuously (ch. 3.9); a member whose timeline covers only
part of the year touches the 2026 bucket and falls short of its 365 days, so it
fails (case 3 — partial COVERAGE, `[jan, jul)`, which is what the file name
says; not a hole punched in the middle). The two sit side by side here so that
an executor which collapses them fails by name rather than by accident.

The vacuous reading of an absent timeline is **not a free choice made here**:
it is the answer the ported production runtime already gave. `rsGeheelPeriode`
opens with `if (tijdlijn == null) return true;` — no period, nothing to
violate — and ch. 3.9's "a timeline with no segments touches no bucket and
passes vacuously" is that behaviour written down. The corpus pins it rather
than inheriting it silently.

**Case 7 is the cell where vacuous changes sign**, and it is the reason the
pair is two cases and not one. Under `all` an absent timeline is harmless; under
`none` it is fatal, because the member SATISFIES its matrix vacuously and `none`
denies exactly that. So a ploeg whose only berger has no dagvergoeding reads
wisselvallig **nee**, while a ploeg with no bergers at all reads **ja** — two
different vacua, and only the second is the empty conjunction. Case 6 cannot
make that distinction on its own: its berger at 50 already decides `none`
before the empty member is reached.

`asOfDate` is 2026-03-15 in every case and is **irrelevant to the verdict** —
deliberately so. The whole-period fold takes its buckets from the timeline, not
from the evaluation date; a March asOfDate over a January–July timeline would
read "covered" on any point-in-time reading, and reads `onwaar` here.

`dagvergoeding` is declared month-granular while the qualifier's grid is the
YEAR. That independence is ch. 3.9's ("the grid is the qualifier's own") and the
casus carries it rather than hiding it behind a matching pair.
