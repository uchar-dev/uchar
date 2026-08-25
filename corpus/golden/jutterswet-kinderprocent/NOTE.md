# jutterswet-kinderprocent

The closed-body **`Percentage_van`** witness (ASP-TRANSLATION S15/S19, form 1).
`premie` folds `acc + (x% van grondslag)` over the list parameter `kindertallen`,
each element a percentage-of the integer `grondslag`. Per element this is the
multiplicative-family fold carried into the closed body: `(l/100)·r` with the
`/100` folded into the scale (`sl·sr·100`, no division), the rule-level
`Percentage_van` encoding and its 46340 range fence reused verbatim.

## Hand-computed absolutes (case1, `kindertallen = [10, 25]`, `grondslag = 200`)

| x  | x% van 200        | scaled (÷100 = 2 dec) |
|----|-------------------|-----------------------|
| 10 | (10/100)·200 = 20 | **20.00**             |
| 25 | (25/100)·200 = 50 | **50.00**             |

`premie = 0.00 + 20.00 + 50.00 = 70.00`.

case2 (`kindertallen = []`): the fold base fires on `lpres`, so `premie = 0.00`
(the init, a present-but-empty list).

Both cases match exactly in ref and clingo (gate 5 ≡ gate 6). ROOD-EERST: before
the broadening clingo faulted `TRANSLATION FAULT: a closed-body term here is a
bound binder, a literal, a parameter, a +/- or × combination, a constant-divisor
division or a Construct (no corpus case for more)`. The moved 46340 fence fires
loudly on `x = 50000` (`viol("arithmetic range exceeded …",plist("kindertallen"))`,
UNSAT), keyed by the fold's list-id — the fence carried into body context.
