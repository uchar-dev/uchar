# jutterswet-verzuimrente-30-360

The anchor of formulation A (30/360) of `spec/proposals/IMPLICIT-TIME.md` §4,
in the Jutterswet: default interest over a late payment, counted in months
of 30 days. It writes a time-dependent characteristic within a window
(`RUNTIME-SEMANTICS.md` ch. 3.10, 3.11), counts days with 0 in a month
without a true day and leaves a leap day uncounted (ch. 3.8), lets two
writers decide one timeline per day (ch. 5.7), reads a condition per period
with the whole-period qualifier per bucket (ch. 3.9, 3.10), and totals the
period arithmetic (ch. 3.4, 3.6). The anchor pins the exact values of that
casus.

An `Afdracht` has an `afdrachtbedrag` of 10 000,00 € (2 decimals), a
`vervaldatum` and a `betaaldatum`. The parameter `verzuimrentevoet` is a
rate per year (`1/jr`) on a month grid: 0,04 until 2026-02-01 and 0,05 from
then on, both bounds open; `rentejaar` is 360 dg/jr. The `maandrente` and
the `verzuimrente` are of the unbounded form (ch. 2.4), so no rule rounds.

| target | rule | chapter |
|---|---|---|
| `rentedragend` (a characteristic for every day) | van zijn vervaldatum plus 1 dg tot en met zijn betaaldatum | 3.10, 3.11 |
| `rentedagen` (dg/mnd, every month), writer 1 | 30 dg/mnd gedurende de tijd dat hij gedurende de gehele maand rentedragend is | 3.4, 3.9, 3.10 |
| `rentedagen`, writer 2 | de getelde dagen — het aantal dagen in de maand dat hij rentedragend is en de dag geen schrikkeldag is — gedurende de tijd dat hij niet gedurende de gehele maand rentedragend is | 3.8, 3.9, 3.10, 5.7 |
| `maandrente` (€/mnd, every month, unbounded) | de verzuimrentevoet maal zijn afdrachtbedrag maal zijn rentedagen gedeeld door het rentejaar | 3.4 |
| `verzuimrente` (€, unbounded) | het totaal van zijn maandrente | 3.6 |

The two conditions of `rentedagen` are each other's complement per month,
so no day has two writers. The expectations were computed by hand before
any executor saw the document, and checked with exact fractions.

## The rentedagen

- `case1-voorbeeld`: vervaldatum 2025-11-15, betaaldatum 2026-03-12, so
  rentedragend on [2025-11-16, 2026-03-13). December 2025, January and
  February 2026 are whole: 30 each, one segment [2025-12-01, 2026-03-01).
  November counts 16–30, **15**; March 1–12, **12**; every other month
  counts no day, **0**: (−∞, 2025-11-01) and [2026-04-01, +∞).
- `case2-schrikkeljaar`: vervaldatum 2028-02-10, betaaldatum 2028-04-04, so
  rentedragend on [2028-02-11, 2028-04-05). March 2028 is whole, **30**.
  February counts 11–29, nineteen days, of which 29 February is a
  schrikkeldag: **18**. April counts 1–4, **4**; 0 elsewhere.

## The maandrente and the verzuimrente

Per month the rate times 10 000,00 € times the rentedagen over 360 dg/jr,
in €/mnd. Case 1: November 0,04 × 10 000 × 15 / 360 = **16 2/3** (the
lexeme `50/3`), December and January 0,04 × 10 000 × 30 / 360 = **33 1/3**
each (`100/3`, one segment), February 0,05 × 10 000 × 30 / 360 = **41 2/3**
(`125/3`), March 0,05 × 10 000 × 12 / 360 = **16 2/3**, 0 elsewhere; the
total over the month grid is **141 2/3 €** (`425/3`). Case 2: February
0,05 × 10 000 × 18 / 360 = **25**, March **41 2/3**, April
0,05 × 10 000 × 4 / 360 = **5 5/9** (`50/9`); total **72 2/9 €**
(`650/9`). These are the values of IMPLICIT-TIME §4; the decision of 7a (0
in a month without a true day) changes no month of them, since the months
it adds read 0 and total 0.

## Executors

`ref` plays both cases. The ASP backend holds a number as an integer at a
decimal scale and the unbounded form as a whole number, so 16 2/3 has no
place there (CLINGO-033); and the rate times the afdrachtbedrag, the first
factor of the product in the order of §4, passes the bound 46 340 per
factor that keeps its products within its machine integers (CLINGO-034).
Both guards fire on both cases, and the golden stands with both codes in
gate 6's channel. Both branches of the
Drools backend hold a segment of a timeline as a decimal, so the first
maandrente that does not terminate, 50/3 in November 2025, is a limit of
the branch at the fixpoint (DROOLS-312, gate 12's channel).

The rounded form of this casus, which every executor plays, is the golden
`corpus/golden/jutterswet-verzuimrente-30-360-afgerond`: the same casus with
a whole afdrachtbedrag, the maandrente rounded to three decimals and the
verzuimrente to the cent, 141,67 € and 72,22 €.
