# jutterswet-kindermacht

The closed-body **`Tot_de_macht`** witness (ASP-TRANSLATION S15/S19, form 4).
`machttotaal` folds `acc + (x tot de macht 3)` over the list parameter
`kindertallen`, each element cubed. A compile-time non-negative integer exponent
unfolds to **repeated Maal** under the same 46340 multiplicative fence — one
factor per exponent step, each partial product bounded so the running
accumulator never wraps 2^31 (`const_pow_exponent` reads the exponent, shared
with the rule-level power). A runtime, negative or fractional exponent faults
with its reason (ref-only behaviour, S19).

## Hand-computed absolutes (case1, `kindertallen = [2, 3]`)

| x | x^3 |
|---|-----|
| 2 | **8**  |
| 3 | **27** |

`machttotaal = 0 + 8 + 27 = 35`.

case2 (`kindertallen = []`): the fold base fires on `lpres`, so
`machttotaal = 0`.

Both cases match exactly in ref and clingo (gate 5 ≡ gate 6). ROOD-EERST: before
the broadening clingo faulted the closed_term catch-all. The unfolded Maal fence
fires loudly on `x = 400` (`x^2 = 160000 > 46340` →
`viol("arithmetic range exceeded …",plist("kindertallen"))`, UNSAT) — the
conservative repeated-Maal bound (`t^(e-1) ≤ 46340`) is tighter than the
rule-level base bound but always loud.
