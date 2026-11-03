# jutterswet-maandloon

The witness for `RUNTIME-SEMANTICS.md` ch. 3.4 and 3.5 written by rules,
over all time (ch. 3.1): a timeline divides all time, a segment may have
an open bound, and a scalar holds on every day. A case writes an open
bound by leaving out `from` or `to` (`uchar-case-v0`).

A `Jutter` has a monthly `strandloon` and `stormtoeslag` and is linked to
`Vondsten` (the fact type `aanmelding`), each with a monthly `opbrengst`.
Six rules, none with a condition or a validity window:

| target | value | chapter |
|---|---|---|
| `maandloon` | strandloon plus stormtoeslag | 3.4 |
| `loonverschil` | strandloon min stormtoeslag | 3.4 |
| `opgehoogde strandloon` | 5 plus strandloon | 3.4, a scalar operand |
| `vondstopbrengst` | the sum of the opbrengst of all vondsten | 3.5 |
| `gewaarborgde vondstopbrengst` | the same sum, or 0 where there are none | 3.5, the fallback |
| `jaarloon` (scalar) | the total of the maandloon | 3.6 |

The expectations were computed by hand before any executor saw the
document; every case has no evaluation date, which none of the forms
reads.

## What each case pins

- `case1-voorbeeld`: the worked examples of ch. 3.4 (strandloon 100 on
  the first half of 2024, stormtoeslag 30 on April–September) and the
  first one of ch. 3.5 (opbrengst {jan 50, feb 60} and {feb 10, mrt 10}).
  maandloon 0 until 2024-01-01, 100, 130, 30, 0 from 2024-10-01;
  loonverschil 0, 100, 70, −30, 0; opgehoogde strandloon 5, 105 on the
  first half of 2024, 5; vondstopbrengst 50, 70, 10 and leeg outside;
  the guaranteed one the same with 0 outside. jaarloon = 3×100 + 3×130 +
  3×30 = 780: the two open segments carry 0 and add 0.
- `case2-tussenmaand`: the interior gap of ch. 3.4 (1 in January, 2 in
  March: February is 0) and the second example of ch. 3.5 (40 in
  January, 20 in March: February is leeg, a gap, and 0 under the
  fallback). jaarloon = 1 + 0 + 2 = 3.
- `case3-zonder-tijdlijnen`: no timeline and no vondst. maandloon and
  loonverschil are 0 on the one segment (−∞, +∞), the opgehoogde
  strandloon 5 there; the sum over no member is the empty timeline, and
  0 on all time under the fallback. jaarloon 0.
- `case4-open-invoer`: the strandloon of case 1 delivered with open
  segments of 0 before and after it, and one vondst whose opbrengst is
  50 on an open segment until 2024-02-01. Every day reads as in case 1,
  so the three ch. 3.4 targets and jaarloon are case 1's; the sums are
  50 until 2024-02-01, and 0 after it under the fallback.
- `case5-gelijke-buren`: adjacent periods of one value are one segment.
  strandloon 100 on January–March and stormtoeslag 0 on March–May give
  100 on [2024-01-01, 2024-04-01) and one open segment of 0 from
  2024-04-01; the opbrengst 10 in January and 10 in February is one
  segment of 10. jaarloon 300.

The total over an open segment that carries another value than 0 has no
value (ch. 3.6, EF-036): `corpus/red/jutterswet-totaal-open-segment`.
