# jutterswet-rekenen-per-dag

The witness for `RUNTIME-SEMANTICS.md` ch. 3.4 beyond plus and min: maal,
verminderd met, gedeeld door, gedeeld door (ABS) and percentage van over
timelines, each read per day over all time with its own empty rule of
ch. 1.2, written by rules. A `Jutter` has a `strandloon` and a
`stormtoeslag`, both whole numbers on a day grid, which the case supplies.

| target (day grid) | value | empty rule per day |
|---|---|---|
| `loonproduct` (whole) | strandloon maal stormtoeslag | both coalesce |
| `nettoloon` (whole) | strandloon verminderd met stormtoeslag | the left propagates, the right coalesces |
| `loonverhouding` (unbounded) | strandloon gedeeld door stormtoeslag | the dividend coalesces; a leeg or 0 divisor is leeg and a §9.5 fact |
| `afgekapte loonverhouding` (5 decimals) | strandloon gedeeld door (ABS) stormtoeslag | as above |
| `strandheffing` (2 decimals) | 10% van zijn strandloon | both coalesce |
| `jaarproduct` (scalar, whole) | the total of the loonproduct | ch. 3.6 over a day grid |

The second half of the document carries units: a `vondstwaarde` and a
`vondstkorting` in €, an `afdrachttarief` in 1/dg and a `zoekduur` in dg,
all whole numbers on a day grid. Where an operand is leeg in a period, the
empty rule of ch. 1.2 decides its value there and its declaration its unit
(ch. 2.10): a product with a leeg factor carries the unit of both factors
(ch. 2.6, rule 2), per period as well (ch. 3.4).

| target (day grid) | value | unit |
|---|---|---|
| `dagafdracht` (whole) | afdrachttarief maal vondstwaarde | €/dg |
| `nettoafdracht` (whole) | (vondstwaarde verminderd met vondstkorting) maal afdrachttarief | €/dg |
| `teruggerekende vondstwaarde` (unbounded) | (vondstwaarde gedeeld door zoekduur) maal zoekduur | € |
| `afdrachttotaal` (scalar, whole) | the total of the dagafdracht | €, ch. 3.6 over a day grid |

A period where a value is leeg is a gap of the written timeline (ch. 3.1).
The expectations were computed by hand before any executor saw the
document. One was wrong: the jaarproduct of case 2 was written down as
547 200 (a miscount of the days); the reference interpreter answered
552 000, and a separate count with exact fractions and the civil calendar
gave 4800 × 70 + 7200 × 30 = 552 000, which the case now pins.

## What each case pins

- `case1-voorbeeld`: the worked example of ch. 3.4 with a stormtoeslag of
  25 instead of 30, so that every quotient is whole: strandloon 100 on the
  first half of 2024, stormtoeslag 25 on April–September. loonproduct 0
  until 2024-04-01, 2500, 0 from 2024-07-01; nettoloon 100 in the first
  quarter and 75 in the second, leeg elsewhere; loonverhouding 0 before
  2024, 4 in the second quarter, 0 from 2024-07-01, and leeg in the first
  quarter, where the stormtoeslag is leeg under a filled strandloon: one
  §9.5 fact EF-031 for each division rule × J1; strandheffing 0, 10 in the
  first half of 2024, 0; jaarproduct 2500 × 91 days = 227 500.
- `case2-nuldeler`: boundaries off the month walls (the day grid), and a
  stormtoeslag of 40 on [2024-02-05, 2024-04-15), 0 on
  [2024-06-01, 2024-08-01) and 60 on [2024-09-03, 2024-10-03) under a
  strandloon of 120 on [2024-01-10, 2024-12-20). The divisor is leeg in four
  periods and 0 in one: each division rule × J1 carries one §9.5 fact with
  the codes EF-031 and EF-005, however many periods deposit them. The
  quotient is 3 and 2 where the divisor is filled and not 0, 0 before
  2024-01-10 and from 2024-12-20; jaarproduct 4800 × 70 + 7200 × 30 =
  552 000.
- `case3-zonder-tijdlijnen`: no timeline. Every coalescing operator is 0 on
  the one segment (−∞, +∞), a quotient leeg ÷ leeg is 0 there too, and the
  nettoloon (leeg verminderd met leeg is leeg) has no segment; no §9.5
  fact; jaarproduct 0, since the open segment carries 0 (ch. 3.6).
- `case4-gat-met-eenheid`: the operands with a unit, each with a gap.
  vondstwaarde 600 € on [2024-01-01, 2024-03-01) and [2024-04-01,
  2024-06-01), so leeg in March; afdrachttarief 2 /dg on [2024-02-01,
  2024-05-01); zoekduur 30 dg on [2024-01-01, 2024-06-01); vondstkorting
  100 € on [2024-01-01, 2024-03-01). dagafdracht 0 until 2024-02-01 (the
  tarief leeg), 1200 in February, 0 in March (the vondstwaarde leeg), 1200
  in April, 0 from 2024-05-01. nettoafdracht: the same, but 500 × 2 = 1000
  in February; in March the difference is leeg (its left operand is), and
  the product coalesces it into €. teruggerekende vondstwaarde 0 before
  2024, 600 in January and February, 0 in March (leeg ÷ 30 dg is 0 in
  €/dg, times 30 dg is 0 €), 600 in April and May, 0 from 2024-06-01.
  afdrachttotaal 1200 × 29 days + 1200 × 30 days = 70 800 €. No §9.5
  fact.

## Executors

`ref`, the ASP backend and both branches of the Drools backend play the
four cases.
