# let-bound-period-arithmetic-total

The total of a timeline that a rule writes by period arithmetic (`RUNTIME-SEMANTICS.md` ch. 3.4, 3.6), where the
rule names its producer through "Daarbij geldt". A `Jutter` has a `strandloon` and a `stormtoeslag`, both whole
numbers on a day grid, which the case supplies, as in `jutterswet-rekenen-per-dag`:

| target | value |
|---|---|
| `loonproduct` (day grid, whole) | het product, where het product is zijn strandloon maal zijn stormtoeslag |
| `jaarproduct` (scalar, whole) | het totaal van zijn loonproduct |

The value of `loonproduct` is a `Let` whose body reads its binder and nothing else. It stands for its bound value
(ch. 10.6), so the rule writes the product per day over all time, as the rule without the `Let` does (ch. 5.12). That
timeline divides all time, and the subject's own total without a clip totals it whole (ch. 3.6, "Which timeline the
subject's own total reads"); no asOfDate bucket enters.

| case | input | outcome |
|---|---|---|
| case1-voorbeeld | strandloon 100 from 2024-01-01 to 2024-07-01, stormtoeslag 25 from 2024-04-01 to 2024-10-01 | loonproduct 0, 2500 from 2024-04-01 to 2024-07-01, 0; jaarproduct **227500** (91 days of 2500) |
| case2-zonder-tijdlijnen | no timelines | loonproduct 0 over all time; jaarproduct **0** |

The outcomes are those of `jutterswet-rekenen-per-dag` for the same targets. An executor that judges the target by the
`Let` instead of its bound value takes the timeline for a case-supplied one and faults, or refuses the total.
