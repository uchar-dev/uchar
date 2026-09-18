# jutterswet-loodsgrens — a PARAMETER as the matrix bound

The sixth of the quantifier-fold family, and the one cell of its grid where the
matrix reads something that is not on the contributor: its bound is a
`ParameterRef`. `jutterswet-schouwploeg` and `jutterswet-vuurtoren` compare the
element's own attribute with a LITERAL; here the same comparison is made against
a parameter whose value arrives with the case.

```
Parameter: de keurgrens : Numeriek (geheel getal)

Een Loodspost is bekwaam     indien de ervaring van alle loodsen van de
                             Loodspost groter is dan de keurgrens.
Een Loodspost is onbekwaam   indien de ervaring van geen van de loodsen van de
                             Loodspost groter is dan de keurgrens.
Een Loodspost is merendeels  indien de ervaring van ten minste 2 van de loodsen
                             van de Loodspost groter is dan de keurgrens.
Het loodstal van een Loodspost moet berekend worden als het aantal loodsen van
                             de Loodspost.
```

`IR-SPEC.md` ch. 4.5 (`QuantifierFold`, WF-108); semantics
`RUNTIME-SEMANTICS.md` ch. 3.9a.

## Why this ir.json is hand-written and carries no bron.rgs

The same reason the rest of the non-temporal family is: `RuleChecker.kwantorvouw`
lowers only the §8.2-QUALIFIED condition, so the unqualified comparison raises
WF-096 (`setlezing buiten een vouwbron`). This document is written by hand
against the schema; gate 10 does not see it.

## What it covers that the others do not

Three cases carry the SAME three loodsen and differ ONLY in the parameter value.
That is the point of the document: an executor that ignores the bound — reading
it as leeg, as zero, or as whatever a stale binding holds — answers those three
cases identically, and they do not agree on a single column. So the parameter is
not merely PRESENT in the matrix here, it is the thing the cells measure.

The three fold quantifiers are on one matrix, so each case also says what the
other two make of the same satisfier count.

## The cells, computed by hand before any executor ran

`count` is the number of loodsen of the post whose `ervaring` exceeds the
keurgrens; the comparison is STRICT (`groter dan`).

| case | keurgrens | post | de loodsen van de post (ervaring) | loodstal | count | bekwaam (alle) | onbekwaam (geen) | merendeels (≥2) |
|---|---|---|---|---|---|---|---|---|
| 1 | 4 | L1 | 8, 6, 5 | 3 | 3 | **ja** | nee | **ja** |
| 2 | 6 | L1 | 8, 6, 5 | 3 | 1 | nee | nee | nee |
| 3 | 9 | L1 | 8, 6, 5 | 3 | 0 | nee | **ja** | nee |
| 4 | 5 | L1 | — | 0 | 0 | **ja** | **ja** | nee |
| 5 | 5 | L1 | 8, 6 | 2 | 2 | **ja** | nee | **ja** |
| 5 | 5 | L2 | 3, leeg | 2 | 0 | nee | **ja** | nee |

Reading the cells:

- **cases 1–3 are one instance set under three bounds**, and every column moves:
  at 4 all three loodsen clear it, at 6 only one does, at 9 none does. Case 2
  also pins the STRICT comparison — the loods with exactly 6 does not clear a
  bound of 6 — which a reader using `groter of gelijk` would get wrong in that
  case alone.
- **case 4 is the vacuum, and the three quantifiers part company on it**
  (ch. 3.9a's vacuous clause is about ALLE and GEEN; a count of 0 answers the
  threshold on its own and answers it **nee**).
- **case 5 pins the RANGE and the leeg member together**: two posts in one run
  read opposite on every column, and L2's second loods has no `ervaring` at all.
  An ordering comparison over leeg is onwaar (ch. 1.2d), so that member does not
  count — `loodstal` is 2 while `count` is 0.

`asOfDate` is null in every case: the keurgrens is a scalar parameter with no
granularity, and nothing in this document is time-dependent.
