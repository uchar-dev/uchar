# jutterswet-verzuimrente-30-360

The anchor of formulation A (30/360) of `spec/proposals/IMPLICIT-TIME.md` §4,
in the Jutterswet: default interest over a late payment, counted in months
of 30 days. It writes a time-dependent characteristic within a window
(`RUNTIME-SEMANTICS.md` ch. 3.10, 3.11), counts days with 0 in a month
without a true day and leaves a leap day uncounted (ch. 3.8), lets two
writers decide one timeline per day (ch. 5.7), reads a condition per period
with the whole-period qualifier per bucket (ch. 3.9, 3.10), and totals the
period arithmetic (ch. 3.4, 3.6).

An `Afdracht` has a whole `afdrachtbedrag` of 10 000 €, a `vervaldatum` and
a `betaaldatum`. The parameter `verzuimrentevoet` is a rate per year (`1/jr`)
on a month grid: 0,04 until 2026-02-01 and 0,05 from then on, both bounds
open; `rentejaar` is 360 dg/jr.

| target | rule | chapter |
|---|---|---|
| `rentedragend` (a characteristic for every day) | van zijn vervaldatum plus 1 dg tot en met zijn betaaldatum | 3.10, 3.11 |
| `rentedagen` (dg/mnd, every month), writer 1 | 30 dg/mnd gedurende de tijd dat hij gedurende de gehele maand rentedragend is | 3.4, 3.9, 3.10 |
| `rentedagen`, writer 2 | de getelde dagen — het aantal dagen in de maand dat hij rentedragend is en de dag geen schrikkeldag is — gedurende de tijd dat hij niet gedurende de gehele maand rentedragend is | 3.8, 3.9, 3.10, 5.7 |
| `maandrente` (€/mnd, every month, 3 decimals) | (de verzuimrentevoet maal zijn rentedagen maal zijn afdrachtbedrag gedeeld door het rentejaar) rekenkundig afgerond op 3 decimalen | 3.4 |
| `verzuimrente` (€, 2 decimals) | (het totaal van zijn maandrente) rekenkundig afgerond op 2 decimalen | 3.6 |

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

Per month the rate times the rentedagen times 10 000 € over 360 dg/jr, in
€/mnd. Exactly, case 1: November 0,04 × 15 × 10 000 / 360 = **16 2/3**,
December and January 0,04 × 30 × 10 000 / 360 = **33 1/3** each, February
0,05 × 30 × 10 000 / 360 = **41 2/3**, March 0,05 × 12 × 10 000 / 360 =
**16 2/3**, 0 elsewhere; the total over the month grid is **141 2/3 €**.
Case 2: February 0,05 × 18 × 10 000 / 360 = **25**, March **41 2/3**, April
0,05 × 4 × 10 000 / 360 = **5 5/9**; total **72 2/9 €**. These are the
values of IMPLICIT-TIME §4; the decision of 7a (0 in a month without a true
day) changes no month of them, since the months it adds read 0 and total 0.

## Why the targets round

The exact values have no terminating decimal expansion, and the ASP backend
holds a number as an integer at a decimal scale, so no target of it holds
16 2/3. The maandrente therefore rounds to three decimals and the
verzuimrente to the cent:

- case 1: 16,667, 33,333 (December and January, one segment), 41,667,
  16,667; total 141,667 → **141,67 €**, the rounding of 141 2/3;
- case 2: 25,000, 41,667, 5,556; total 72,223 → **72,22 €**, the rounding
  of 72 2/9.

Three decimals is the smallest scale on which rounding per month leaves the
total on the cent equal to the rounding of the exact total (with two, case 2
totals 25,00 + 41,67 + 5,56 = 72,23), and it keeps every monthly value below
46 340 at its scale, the bound per factor of the ASP backend's products and
sums. The product is written rate × rentedagen × afdrachtbedrag for the same
bound: rate × afdrachtbedrag would be 50 000 at the scale of the rate.
The exact values are pinned in `ref/`'s suite
(`WV-3.11-formulering-A-onafgerond-*`, the fixture
`conditions-per-period/verzuimrente-onafgerond`: this document with both
targets in the unbounded form and without the roundings), which the ASP
backend refuses by name there.

## Executors

`ref` and the ASP backend play both cases. Both Drools branches play both cases until it carries the form.
