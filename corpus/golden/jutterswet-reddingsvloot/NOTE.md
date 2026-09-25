# jutterswet-reddingsvloot — the COUNTING quantifiers over a linked instance set

The third of the quantifier-fold family. `jutterswet-ploegpremie` runs the §8.2
whole-period matrix under `all` and `none`; `jutterswet-schouwploeg` runs an
ordinary condition under the same two; this one runs the same §8.2 matrix under
all three COUNTING quantifiers, each of them at its threshold from both sides.

```
Een Reddingsvloot is inzetbaar indien de vaartijd van TEN MINSTE 2 van de
                               sloepen van de Reddingsvloot gedurende het
                               gehele jaar groter is dan 20.
Een Reddingsvloot is krap      indien de vaartijd van TEN HOOGSTE 1 van de …
Een Reddingsvloot is paraat    indien de vaartijd van PRECIES 1 van de …
Een Reddingsvloot is geschikt  indien de vaartijd van TEN MINSTE 2 van de
                               sloepen van de Reddingsvloot DIE GEKEURD ZIJN
                               gedurende het gehele jaar groter is dan 20.
Het sloeptal van een Reddingsvloot moet berekend worden als het aantal sloepen
                               van de Reddingsvloot.
```

`IR-SPEC.md` ch. 4.5 (`QuantifierFold`, WF-108 (a) and (e), WF-041); semantics
`RUNTIME-SEMANTICS.md` ch. 3.9a, whose counting worked examples are this
document's cells verbatim.

`bron.rgs` is the source: the §8.2-qualified sentence is exactly what
`RuleChecker.kwantorvouw` lowers, and since this card it lowers all five
quantifiers rather than two, carrying the written telwoord as the fold's
`threshold`. Gate 10 re-emits the document byte-identically.

## What each fold is for

`inzetbaar` / `krap` / `paraat` share ONE matrix, so every cell reads all three
of them off the same count — which is what lets a single pair of cases pin every
threshold from both sides at once. `geschikt` repeats `inzetbaar`'s quantifier
and threshold and adds the die/dat subselection, so wherever those two part
company the FILTER is the only thing that moved. `sloeptal` is the plain
instance fold beside them: it states the raw, unfiltered range per cell, which is
what separates an empty SET from a selection that comes out empty.

## The cells, computed by hand before any executor ran

Bound 20, comparison STRICT (`groter dan`), period the whole of 2026. `count` is
the number of sloepen whose vaartijd exceeds 20 for the WHOLE year — the
unfiltered count the first three folds read; `geschikt` counts over the `gekeurd`
survivors only, and its own counts are given in the last column.

| case | vloot | the vloot's sloepen (vaartijd, keuring) | sloeptal | count | inzetbaar (≥2) | krap (≤1) | paraat (=1) | geschikt (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|
| 1 | V1 | 25 gekeurd, 30 gekeurd | 2 | 2 | **ja** | nee | nee | **ja** (2) |
| 2 | V1 | 25 gekeurd, 10 gekeurd | 2 | 1 | nee | **ja** | **ja** | nee (1) |
| 3 | V1 | geen sloepen | 0 | 0 | nee | **ja** | nee | nee (0) |
| 4 | V1 | 25 gekeurd, 30 alleen jan–jun gekeurd | 2 | 1 | nee | **ja** | **ja** | nee (1) |
| 5 | V1 | 25 gekeurd, 30 NIET gekeurd, 10 gekeurd | 3 | 2 | **ja** | nee | nee | nee (1) |
| 5 | V2 | 30 gekeurd | 1 | 1 | nee | **ja** | **ja** | nee (1) |
| 6 | V1 | 25 NIET gekeurd, 30 NIET gekeurd | 2 | 2 | **ja** | nee | nee | nee (0) |

**Cases 1 and 2 pin every threshold from BOTH sides**, and they do it with two
member sets because the three folds share a matrix. A count of 2 sits exactly at
*ten minste 2* and exceeds *ten hoogste 1* by exactly one; a count of 1 turns
both of those over and is exactly what *precies 1* wants. A reader that had the
comparison the wrong way round (`>` for `>=`, or `<` for `<=`) fails case 1 or
case 2 by name.

**Case 3 is the empty set, and it is the cell that shows there is no vacuum rule
here.** `all` and `none` both read waar on an empty contributor set
(`jutterswet-ploegpremie` case 4 pins that); the counting quantifiers simply read
the count, which is 0 — so *krap* holds while *inzetbaar* and *paraat* do not.
No single "an empty set means true" principle produces this row. `sloeptal` is 0
in the same cell, so the cell says out loud that the set really is empty and not
merely unreachable.

**Case 4 is ch. 3.9's gap under a counting quantifier.** The second sloep has a
timeline for January through June only, so it fails the whole-period matrix and
does not COUNT — the difference between a count of 2 and a count of 1, and
therefore between *inzetbaar* and *krap*. Nothing about the gap rule changes
under a threshold; it is the same per-member verdict, tallied instead of folded.

**Case 5's V1 is the cell the FILTER alone decides.** *inzetbaar* and *geschikt*
ask the same question with the same threshold over the same matrix; the sloep at
30 is not `gekeurd`, so the surviving pair (25 and 10) holds one satisfier and
*geschikt* falls where *inzetbaar* stands. Remove `filter` from the `ir.json` and
this is the cell that breaks, and only this one.

**Case 5's V2 pins the RANGE, and the empty-prefix triggers with it.** V2's single
sloep is above the bound, so V2 reads *krap* and *paraat* while V1 reads neither
— on the same run, over the same population. An executor that quantified over
every `Reddingssloep` instead of over the vloot's own sloepen would count 3 for
V2 and answer it the other way; that is the failure mode any implementation
reifying the matrix per element, or counting outside the range join, has to
avoid. It is also where `RUNTIME-SEMANTICS.md` ch. 5.10's empty-prefix triggers
fire on a fold: over ONE contributor, *ten minste 2* is out of reach before the
matrix is read once, and *ten hoogste 1* can no longer be exceeded, so both
answer having read no member at all.

**Case 6 is the empty SELECTION beside a filled range.** Two sloepen, both above
the bound, neither `gekeurd`: *inzetbaar* counts 2 and holds, *geschikt* counts
over ∅ and does not. It is the counting mirror of case 3 — the same count of 0
reached by filtering rather than by an empty relation — and it separates an
executor that drops the filter (which would read *geschikt* as *inzetbaar*) from
one that mishandles the empty count.

`asOfDate` is 2026-03-15 in every case, as in the sibling goldens; the §8.2
qualifier ranges over the whole of the evaluation year regardless of where in it
the evaluation date sits.
