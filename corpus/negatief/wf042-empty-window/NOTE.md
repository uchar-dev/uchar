Violates WF-042 (`spec/IR-SPEC.md`): A time window without any bound.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L5 a `TimeWindow` writes a bound in `from` or `to` only where it has one
(delta B6), so this window carries neither field; on the v0 wire both were
`null`. `expect.txt` pins the arm.
