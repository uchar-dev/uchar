# jutterswet-vondsttempo — a number of finds per day, and back again

The unit of a unitless number divided by a duration is the reciprocal of that
duration (`spec/IR-SPEC.md` ch. 2.5, point 5; `spec/RUNTIME-SEMANTICS.md` ch. 2.6).
A jutter's `vondstental` (no unit) divided by `zoekduur` (dg) is `vondsttempo`
(`met eenheid 1/dg`). That rate is compared with the parameter `meldingsdrempel`
(`met eenheid 1/wk`) and with the literal `30 1/wk`, and multiplied by a duration it is
a number without a unit again. With `wk = 7 dg`, the factor of `1/dg` into `1/wk` is 7.

Hand-computed values, `case.json`, `meldingsdrempel` = 14,00 per week:

| | vondsten | zoekduur | seizoensduur | tempo (per dg, naar beneden op 2 dec.) | per week | tempo × zoekduur | tempo × seizoensduur (× 7) | ijverig (> 14) | drempelhoudend (= 14) | onvermoeibaar (> 30) |
|---|---|---|---|---|---|---|---|---|---|---|
| J1 | 10 | 4 dg | 3 wk | 2,50 | 17,50 | 10,00 | 2,50 × 21 = 52,50 | ja | nee | nee |
| J2 | 4 | 2 dg | 1 wk | 2,00 | 14,00 | 4,00 | 2,00 × 7 = 14,00 | nee | ja | nee |
| J3 | 3 | 6 dg | 2 wk | 0,50 | 3,50 | 3,00 | 0,50 × 14 = 7,00 | nee | nee | nee |
| J4 | 15 | 3 dg | 1 wk | 5,00 | 35,00 | 15,00 | 5,00 × 7 = 35,00 | ja | nee | ja |
| J5 | 1 | 3 dg | 1 wk | 0,33 | 2,31 | 0,99 | 0,33 × 7 = 2,31 | nee | nee | nee |

`case2-lage-drempel.json`, `meldingsdrempel` = 1,00 per week:

| | vondsten | zoekduur | seizoensduur | tempo | per week | tempo × zoekduur | tempo × seizoensduur | ijverig (> 1) | drempelhoudend |
|---|---|---|---|---|---|---|---|---|---|
| J1 | 1 | 1 dg | 1 wk | 1,00 | 7,00 | 1,00 | 7,00 | ja | nee |
| J2 | 1 | 7 dg | 1 wk | 0,14 | 0,98 | 0,98 | 0,98 | nee | nee |

What the values tell apart. J1 of `case.json` and of the second case compare a rate that
is below the threshold in bare numbers (2,50 against 14; 1,00 against 1,00) and above it
once restated. J2 of `case.json` is the exact equality: 2,00 per day is 14 per week, which
a factor rounded to a decimal 1/7 does not reach. J4 against the literal is the same
question for `30 1/wk`. `seizoensoogst` is the product across the conversion: without it,
J1 reads 7,50.

The attribute `vondsttempo` carries the rounding because the quotient of J5 does not
terminate; the two products of a two-decimal rate and a whole number fit their
two-decimal targets (ch. 2.3).
