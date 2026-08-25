# jutterswet-kinderkap

The closed-body **`Gedeeld_door_abs`** witness (ASP-TRANSLATION S15/S19, form 2).
`kaptotaal` folds `acc + (x /abs 3)` over the list parameter `kindertallen`,
each element abs-divided by the compile-time constant `3`. Abs-division (ch. 2.2)
truncates the quotient toward zero at 5 decimals — a defined collapse point,
which gringo's integer `/` computes on both signs, so the round_z-scale-5 form
carried into the body context needs no exactness guard and no negative fence.
The declared 5 decimals of `kaptotaal` are exactly the collapse scale.

## Hand-computed absolutes (case1, `kindertallen = [10, 25]`)

| x  | x /abs 3 (⌊·⌋ toward 0 at 5 dec) |
|----|----------------------------------|
| 10 | 10/3 = 3.33333…  → **3.33333**  |
| 25 | 25/3 = 8.33333…  → **8.33333**  |

`kaptotaal = 0.00000 + 3.33333 + 8.33333 = 11.66666`.

case2 (`kindertallen = []`): the fold base fires on `lpres`, so
`kaptotaal = 0.00000`.

Both cases match exactly in ref and clingo (gate 5 ≡ gate 6). ROOD-EERST: before
the broadening clingo faulted the closed_term catch-all (`… a +/- or ×
combination, a constant-divisor division or a Construct …`). The numerator range
fence moves into body context and fires loudly on `x = 6000`
(`viol("arithmetic range exceeded …",plist("kindertallen"))`, UNSAT; the
boundary `x = 5368` → 1789.33333 still computes, ref ≡ clingo).
