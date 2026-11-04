# jutterswet-verzuimrente-dagen-afgerond

The rounded form of the anchor `corpus/golden/anchor/jutterswet-verzuimrente-dagen`
(formulation B of `spec/proposals/IMPLICIT-TIME.md` §4, `RUNTIME-SEMANTICS.md`
ch. 3.4 and ch. 3.6), in a form every executor plays, so the rounding of a
total the calendar integrates stays in the differential square. The anchor
pins the exact values in the unbounded form; this golden pins their
rounding to the cent.

An `Afdracht` has a whole `afdrachtbedrag` in €, a `vervaldatum` and a
`betaaldatum`, and the day-grid characteristic `rentedragend`, which the
case supplies: it holds from the day after the vervaldatum up to and
including the betaaldatum. The parameter `verzuimrentevoet` is a rate per
year (unit `1/jr`) on a month grid: 0,04 until 2026-02-01 and 0,05 from
then on, both bounds open. The unit system `Tijd` declares `dg` and `jr`
and no factor between them: a year has no fixed number of days (ch. 3.6).

| target | value | chapter |
|---|---|---|
| `dagrente` (€/jr, a day grid, 2 decimals) | the verzuimrentevoet maal the afdrachtbedrag | 3.4, per day |
| `verzuimrente` (€, 2 decimals) | the total of the dagrente during the time the Afdracht is rentedragend, rekenkundig rounded to 2 decimals | 3.6, the clip and the calendar |

The expectations were computed by hand before any executor saw the
document, and checked with exact fractions and the civil calendar.

## The dagrente

Per day the rate times 10 000 €: 400 €/jr before 2026-02-01 and 500 €/jr
from that day on, (−∞, 2026-02-01) = 400 and [2026-02-01, +∞) = 500, in
both cases.

## The verzuimrente

The exact totals are those of the anchor: 139 13/73 € (139,178 08…) in
`case1-voorbeeld` and 73 47/61 € (73,770 49…) in `case2-schrikkeljaar`.
The rule rounds them once (ch. 2.5) to **139,18 €** and **73,77 €**.

## Why this form

The ASP backend holds a number as an integer at a decimal scale. No decimal
scale holds the exact totals, so it translates the calendar total only as
the base of a rounding (`backends/clingo/README.md` [V6]). The whole
afdrachtbedrag gives the product per day the bound 2, which fits a dagrente
of 2 decimals (ch. 2.4), and keeps each factor of that backend's products
below 46 340; an afdrachtbedrag of 10 000,00 at the scale 100 would not.

## Executors

`ref`, the ASP backend and both branches of the Drools backend play both
cases.
