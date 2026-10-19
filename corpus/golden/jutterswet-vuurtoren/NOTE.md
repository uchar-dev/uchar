# jutterswet-vuurtoren — the counting quantifiers over a NON-temporal matrix

The fourth of the quantifier-fold family, and the cell of its grid that
`jutterswet-reddingsvloot` leaves open. That golden runs the three counting
quantifiers over the §8.2 WHOLE-PERIOD matrix; `jutterswet-schouwploeg` runs an
ordinary, non-temporal condition under `all` and `none`. This one crosses the
two: counting quantifiers over a matrix with no period qualifier anywhere in the
document.

```
Een Vuurtoren is helder       indien de sterkte van TEN MINSTE 2 van de
                              lampen van de Vuurtoren groter is dan 5.
Een Vuurtoren is zuinig       indien de sterkte van TEN HOOGSTE 1 van de …
Een Vuurtoren is enkelvoudig  indien de sterkte van PRECIES 1 van de …
Een Vuurtoren is keurig       indien de sterkte van TEN MINSTE 2 van de
                              lampen van de Vuurtoren DIE GEKEURD ZIJN
                              groter is dan 5.
Het lampental van een Vuurtoren moet berekend worden als het aantal lampen
                              van de Vuurtoren.
```

`IR-SPEC.md` ch. 4.5 (`QuantifierFold`, WF-108); semantics
`RUNTIME-SEMANTICS.md` ch. 3.9a.

## This ir.json was written by hand; its source.rgs arrived later

For the same reason `jutterswet-schouwploeg` was, measured again and unchanged
at the time: `RuleChecker.quantifierFold` opened with "only the §8.2-QUALIFIED
condition" and declined everything else, so the unqualified comparison was
refused (`setlezing buiten een vouwbron`, a message that cited WF-096 until L2;
since L2 the same refusal cites WF-053, measured 2026-09-24 on this document's
first sentence with "ten minste 0 van de lampen", a form the lowering still
declines) and the characteristic spelling resolved to `Onopgelost`. The counting quantifiers did not move that gate.
The frontend card that did move it landed on 2026-09-19, and `source.rgs` now
sits beside this document and re-emits it byte for byte. The hand-written IR
is unchanged; gate 10 sees it.

## What it covers that the other three do not

Both executor branches that carry a fold have TWO matrix routes, and the
counting arms are separate code in each of them. This golden is the only
corpus document whose counting quantifier reaches the NON-TEMPORAL route: the
Drools exec branch counts over a `collectList` accumulate of the CONTRIBUTORS
instead of over their timelines, and the DRL-text branch writes a `Number(
intValue <op> N ) from accumulate( ... count(...) )` over the element pattern
— a count over a pattern, where its whole-period sibling folds a collected
list through `rsGeheelPeriodeTelling`. `jutterswet-reddingsvloot` pins the
other route in both branches (it has since 2026-09-18, when the DRL-text
branch gained the whole-period fold); without THIS golden the non-temporal
counting route has no corpus witness in either of them, and the two routes
could drift apart unseen.

## The cells, computed by hand before any executor ran

Bound 5, comparison STRICT (`groter dan`). `count` is the number of lampen whose
`sterkte` exceeds 5; `keurig` counts over the `gekeurd` survivors only, and its
own count is in the last column.

| case | toren | the toren's lampen (sterkte, keuring) | lampental | count | helder (≥2) | zuinig (≤1) | enkelvoudig (=1) | keurig (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|
| 1 | T1 | 8 gekeurd, 6 gekeurd | 2 | 2 | **ja** | nee | nee | **ja** (2) |
| 2 | T1 | 8 gekeurd, 3 gekeurd | 2 | 1 | nee | **ja** | **ja** | nee (1) |
| 3 | T1 | geen lampen | 0 | 0 | nee | **ja** | nee | nee (0) |
| 4 | T1 | 8 gekeurd, 9 NIET gekeurd, 3 gekeurd | 3 | 2 | **ja** | nee | nee | nee (1) |
| 4 | T2 | 9 gekeurd | 1 | 1 | nee | **ja** | **ja** | nee (1) |
| 5 | T1 | leeg gekeurd, 3 gekeurd | 2 | 0 | nee | **ja** | nee | nee (0) |

Cases 1 and 2 pin each threshold from both sides off one shared matrix, case 3
the empty set (a count of 0 answers all three, and they do not agree — which is
what shows there is no vacuum rule to apply here), case 4's T1 the filter and its
T2 the range, exactly as the sibling golden's cells do.

**Case 5 is the one this document adds on its own.** L1 has no `sterkte` at all.
An ordering comparison over leeg is onwaar (ch. 1.2d), so L1 fails the matrix and
does not count — `lampental` is 2 while `count` is 0. It is the counterpart of
`jutterswet-ploegpremie` case 7, where an ABSENT TIMELINE passes its whole-period
matrix vacuously and therefore counts; here the absent value fails. Under a
threshold the difference is arithmetic rather than a flipped verdict, which is
why it is worth a cell of its own: `zuinig` holds because the count is 0, and it
would hold for the opposite reason if leeg passed.

`asOfDate` is null in every case: nothing in this document is time-dependent,
which is exactly what makes the matrix non-temporal.
