# jutterswet-kinderfoldheffing

The fold-body analogue of `jutterswet-kinderweging` (which lands the same
`grondslag × (1,0000 + kindertal / 2,0000)` rounded to the cent at RULE level).
Here the computation lives inside a closed fold body, exercising the broadened
closed-body multiplicative forms (ASP-TRANSLATION S15):

- **`heffing`** — `Fold(+, 0.00)` over the list parameter `kindertallen`, each
  element folded as `acc + REKENKUNDIG₂(grondslag × (1,0000 + x / 2,0000))`.
  Per element this is the money × rate split encoding (S18(c), form 1b), the
  constant-divisor division `x / 2,0000` (form 3), and the integer→rational
  widening the division harmonises for free (the ToRat pass-through, form 2).
- **`puntentotaal`** — `Fold(+, 0)` of `acc + x × 2`, the general Maal (form 1a,
  the scaled product with its 46340 range fence).

## Hand-computed absolutes (case1, `kindertallen = [1, 2]`, `grondslag = $123.45`)

| x | rate = 1 + x/2 | grondslag × rate | round₂ (REKENKUNDIG) |
|---|---|---|---|
| 1 | 1.5 | 185.175 | **185.18** (half-away up) |
| 2 | 2.0 | 246.90 | **246.90** |

`heffing = 185.18 + 246.90 = 432.08`; `puntentotaal = 1·2 + 2·2 = 6`.

case2 (`kindertallen = []`): the fold base fires on `lpres`, so `heffing = 0.00`
and `puntentotaal = 0` — the init, a present-but-empty list.

Both cases match exactly in ref and clingo (gate 5 ≡ gate 6). The x=1 rung pins
the cent rounding actually engaging (185.175 → 185.18), so the split encoding's
REKENKUNDIG half-away is witnessed, not merely wired.
