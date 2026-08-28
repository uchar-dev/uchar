# jutterswet-grensbedrag

The **Begrensd (clamp)** witness (ASP-TRANSLATION S6/S19). A `Jutter`'s
2-decimal `ruwbedrag` is clamped three ways, one per term-position the encoding
gained: `ondergrens` (min-only), `bovengrens` (max-only), `bandbreedte`
(min+max). The integer-literal bounds sit at scale 10^0 while the base rides
10^2, so the scale harmonisation (bound × 100) is exercised on every arm. The
clamp uses the integer identities max(a,b)=(a+b+|a−b|)/2, min(a,b)=(a+b−|a−b|)/2
on the harmonised scale; minimum is applied first, then maximum.

## Hand-computed absolutes (all at 2 decimals)

| Jutter | ruwbedrag | ondergrens = max(x,10) | bovengrens = min(x,100) | bandbreedte = min(max(x,10),100) |
|--------|-----------|------------------------|-------------------------|----------------------------------|
| J1     | 5.00      | **10.00**              | **5.00**                | min(10.00,100) = **10.00**       |
| J2     | 250.00    | **250.00**             | **100.00**              | min(250,100) = **100.00**        |
| J3     | 42.00     | **42.00**              | **42.00**               | **42.00**                        |
| J4     | (leeg)    | leeg                   | leeg                    | leeg                             |

J4 pins the **empty-base propagation**: the base's `val` atom is unbound, so the
reading rule is underivable and all three clamps stay leeg (ref clamp Empty →
Empty). ref = clingo over every expectation (gate 5 ≡ gate 6). NOOP-REFUTING:
every clamped value is computed and absent from the raw input.
