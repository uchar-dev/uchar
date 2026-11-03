# jutterswet-loon-per-periode

The witness for a condition read per period on a rule with a timeline
target (`RUNTIME-SEMANTICS.md` ch. 3.10, the clip generalised): a
condition that reads a timeline restricts the written timeline to the
periods in which it holds; one that reads none gates the rule as on a
scalar target (ch. 3.3).

A `Jutter` has a monthly `strandloon` and `stormtoeslag`, the monthly
characteristic `aangesteld`, the daily characteristic `wachtplichtig` and
the scalar characteristic `erkend`. Seven rules on monthly targets:

| target | value | condition | reading |
|---|---|---|---|
| `aangestelde loon` | strandloon plus stormtoeslag | gedurende de tijd dat hij aangesteld is | the true periods of a characteristic |
| `vrije loon` | the same | … niet aangesteld is | their complement over all time |
| `erkende loon` | the same | indien hij erkend is | a scalar characteristic: the gate |
| `hoge loon` | the same | indien zijn strandloon groter is dan 100 | a comparison per day |
| `aangestelde hoge loon` | the same | alle volgende voorwaarden: aangesteld, strandloon > 100 | a composite: per day both |
| `aanstellingspremie` | 25 | gedurende de tijd dat hij aangesteld is | a scalar as the whole value |
| `wachtdagen` | het aantal dagen in de maand dat hij wachtplichtig is | gedurende de tijd dat hij aangesteld is | a day count cut by its condition |

The value strandloon plus stormtoeslag divides all time (ch. 3.4). The
expectations were computed by hand before any executor saw the
document; no case has an evaluation date, which none of the forms reads.

## What each case pins

- `case1-voorbeeld`: strandloon 100 on January–March 2024 and 150 on
  April–September, stormtoeslag 30 on March–May, aangesteld February–July,
  wachtplichtig 2024-01-20 up to 2024-03-10 and 2024-05-05 up to
  2024-05-25, erkend. The sum is 0 until 2024-01-01, 100, 130 (March),
  180 (April–May), 150 (June–September) and 0 from 2024-10-01.
  aangestelde loon 100, 130, 180, 150 on February–July; vrije loon 0 until
  2024-01-01, 100 in January, 150 in August–September, 0 from 2024-10-01;
  erkende loon the whole sum; hoge loon 180 and 150 on April–September,
  where strandloon > 100 (a gap reads leeg, and no ordering holds on it);
  aangestelde hoge loon 180 and 150 on April–July; aanstellingspremie 25 on
  [2024-02-01, 2024-08-01), one segment; wachtdagen February 29 (2024 is a
  leap year), March 9 and May 20, January's 12 cut away.
- `case2-nooit-aangesteld`: no aangesteld periods, not erkend, strandloon
  120 on January–February. Every target under aangesteld is empty, the
  vrije loon is the whole sum (0, 120, 0), the erkende loon is not
  written, the hoge loon 120 on January–February.
- `case3-aaneengesloten-perioden`: aangesteld on January, and on March and
  April as two adjacent periods, which are one period of the condition: the
  premie is 25 on January and 25 on [2024-03-01, 2024-05-01), one segment;
  the vrije loon 200 in February between its two open segments of 0.
