# jutterswet-verzuimrente-dagen

The anchor of `RUNTIME-SEMANTICS.md` ch. 3.4 (an operator of ch. 1.2 read
per day, with a time-dependent parameter as an operand) and ch. 3.6 (the
calendar between the day family and the month family of time units). The
casus is formulation B of `spec/proposals/IMPLICIT-TIME.md` §4, in the
Jutterswet: default interest over a late payment.

An `Afdracht` has a whole `afdrachtbedrag` in €, a `vervaldatum` and a
`betaaldatum`, and the day-grid characteristic `rentedragend`, which the
case supplies: it holds from the day after the vervaldatum up to and
including the betaaldatum. The parameter `verzuimrentevoet` is a rate per
year (unit `1/jr`) on a month grid: 0,04 until 2026-02-01 and 0,05 from
then on, both bounds open. The unit system `Tijd` declares `dg` and `jr`
and no factor between them: a year has no fixed number of days (ch. 3.6).

| target | value | chapter |
|---|---|---|
| `dagrente` (€/jr, a day grid) | the verzuimrentevoet maal the afdrachtbedrag | 3.4, per day |
| `verzuimrente` (€, 2 decimals) | the total of the dagrente during the time the Afdracht is rentedragend, rekenkundig rounded to 2 decimals | 3.6, the clip and the calendar |

The expectations were computed by hand before any executor saw the
document, and checked with exact fractions and the civil calendar.

## The dagrente

Per day the rate times 10 000 €: 400 €/jr before 2026-02-01 and 500 €/jr
from that day on. The parameter's two segments have open bounds, so the
written timeline has them too: (−∞, 2026-02-01) = 400 and
[2026-02-01, +∞) = 500, in both cases.

## The verzuimrente

The total integrates a rate per year over a day grid. A day weighs 1/len of
the calendar year it lies in, len its actual length: 365 days, or 366 in a
leap year.

- `case1-voorbeeld`: vervaldatum 2025-11-15, betaaldatum 2026-03-12, so
  rentedragend on [2025-11-16, 2026-03-13). The clip cuts the dagrente into
  77 days at 400 €/jr ([2025-11-16, 2026-02-01): 15 + 31 + 31) and 40 days
  at 500 €/jr ([2026-02-01, 2026-03-13): 28 + 12), all in years of 365 days:
  400 × 77/365 + 500 × 40/365 = 50 800/365 = **139 13/73 €**
  (139,178 08…), rounded to **139,18 €**.
- `case2-schrikkeljaar`: vervaldatum 2028-02-10, betaaldatum 2028-04-04, so
  rentedragend on [2028-02-11, 2028-04-05): 19 + 31 + 4 = 54 days at
  500 €/jr in the leap year 2028: 500 × 54/366 = **73 47/61 €**
  (73,770 49…), rounded to **73,77 €**. With a declared factor `jr = 365 dg`
  it would be 500 × 54/365 = 73 71/73 € (73,97 €); such a factor is
  ill-formed (WF-009, `corpus/negative/wf009-calendar-family-factor`).

## Why the target rounds

The exact totals, 139 13/73 € and 73 47/61 €, have no terminating decimal
expansion. The reference interpreter holds them exactly in a target of the
unbounded form (`ref/` tests `WV-3.6-kalender` and `WV-3.6-kalender-geval-1`
pin both), but the ASP backend holds a number as an integer at a decimal
scale, and no decimal scale holds them. The target therefore rounds, in the
rule, to the cent; the rounding rounds the exact value once (ch. 2.5).

## Executors

`ref`, the ASP backend and both branches of the Drools backend play both
cases.
