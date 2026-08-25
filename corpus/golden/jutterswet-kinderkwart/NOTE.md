# jutterswet-kinderkwart

The scale-fence regression pin for the closed-body constant-divisor division
(ASP-TRANSLATION S15, form 3). A `Fold(+, 0.00)` over the integer list
`kindertallen` folds each element as `acc + kindertal / 4` into the 2-decimal
attribute `kwartheffing` — a **bare integer divisor** (`4`, scale 1) whose
quotient needs two decimals the operand scales alone (max(sl,sr)=1) do not
carry.

The exactness test in `closed_division` must therefore run at the precision the
fold write STORES (the target attribute scale, carried through the fold body in
`ctx.write_scale`), exactly as the rule-level `translate_division` does. Before
the fix it computed `out_scale = max sl sr = 1`, so `1 \ 4 = 1 != 0` fired the
non-exact-division viol and clingo went UNSATISFIABLE while ref accepted `0.25`
— a fallen scale fence. The 4-decimal divisor of `jutterswet-kinderfoldheffing`
(sr = 10000 ≥ write_scale) masked it; this witness makes the write_scale term
load-bearing.

## Hand-computed absolutes

case1 (`kindertallen = [1, 2, 3]`):

| x | x / 4 |
|---|-------|
| 1 | 0.25  |
| 2 | 0.50  |
| 3 | 0.75  |

`kwartheffing = 0.25 + 0.50 + 0.75 = 1.50` — each quotient exact at two
decimals, none needing a rounding.

case2 (`kindertallen = []`): the fold base fires on `lpres`, so the init stands:
`kwartheffing = 0.00`.

Both cases match exactly in ref and clingo (gate 5 ≡ gate 6).
