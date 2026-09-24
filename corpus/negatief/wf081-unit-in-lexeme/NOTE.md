# Violates WF-081 — unit token inside a numeric literal lexeme

The Gelijkstelling assigns the numeric literal `{"number": "5 €"}`: a currency
sign (and the whitespace before it) written INTO the lexeme instead of a bare
number. Both executors fault on such a lexeme at evaluation time ("malformed
number lexeme"); WF-081 makes the document ill-formed at verification time,
ahead of any executor.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is any golden numeric literal, e.g.
`corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 the literal's value is typed (delta B22): the ill-formed string sits
in its `number` variant, `{"number": "5 €"}`, which the rule reads, and the
report cites `/value`. `expect.txt` pins the arm.
