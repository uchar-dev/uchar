# Violates WF-081 — unit token inside a numeric literal lexeme

The Gelijkstelling assigns the numeric literal `{"number": "5 €"}`: a currency
sign (and the whitespace before it) written INTO the lexeme instead of a bare
number. Every executor refuses such a lexeme at load with the verifier's own
WF-081 sentence (since the L4 fix round; until then the reference interpreter
and the ASP backend faulted at evaluation with "malformed number lexeme" and
the Drools branches read it as 5); WF-081 makes the document ill-formed at
verification time, ahead of any executor.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is any golden numeric literal, e.g.
`corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 the literal's value is typed (delta B22): the ill-formed string sits
in its `number` variant, `{"number": "5 €"}`, which the rule reads, and the
report cites `/value`. `expect.txt` pins the arm.
