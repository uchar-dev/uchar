# jutterswet-kinderdeler

*Since 2026-10-19 (card `zero-division-channel`) a numeric-zero divisor in a
closed body is leeg with a §9.5 fact of kind EF-005, no fault
(`RUNTIME-SEMANTICS.md` ch. 1.4 point 4, ch. 10.1), and
`closed-runtime-division-by-zero` is a golden. What follows about the
numeric-zero fence describes the state before that card.*

The closed-body **runtime-divisor** witness (ASP-TRANSLATION S15/S19, form 5).
`delertotaal` folds `acc + (grondslag / x)` over the list parameter
`kindertallen`, the divisor `x` a bound list element — a RUNTIME divisor, not a
compile-time constant. The rule-level bare division carried into the body
context: a closed-body binder is never leeg, so the empty-operand §9.5 edges
(empty dividend → 0, empty divisor → the channel) cannot arise; the two-factor
form rides with its numeric-zero fence, and the value is guarded EXACT
(`numt \ dent == 0`), so a non-exact per-element quotient derives a `viol/2` and
UNSAT (ref stays exact-rational, then its bounded write refuses — the
differential holds where it runs).

## Hand-computed absolutes (case1, `kindertallen = [2, 3, 4, 5]`, `grondslag = 60`)

| x | 60 / x (exact) |
|---|----------------|
| 2 | **30.00**      |
| 3 | **20.00**      |
| 4 | **15.00**      |
| 5 | **12.00**      |

`delertotaal = 0.00 + 30.00 + 20.00 + 15.00 + 12.00 = 77.00`; every quotient is
exact at the 2-decimal target scale (`6000 \ x == 0`).

case2 (`kindertallen = []`): the fold base fires on `lpres`, so
`delertotaal = 0.00`.

Both cases match exactly in ref and clingo (gate 5 ≡ gate 6). ROOD-EERST: before
the broadening clingo faulted `a closed-body division needs a compile-time
positive constant divisor (no corpus case for a runtime or non-positive
divisor)`. The numeric-zero fence fires in body context — pinned by the golden `corpus/golden/closed-runtime-division-by-zero` (`kindertallen = [2, 0]`, both
executors *"division by numeric zero"*).
