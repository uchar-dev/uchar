# Violates WF-081 — unit token inside a numeric literal lexeme

The Gelijkstelling assigns the numeric literal `"5 €"`: a currency sign
(and the whitespace before it) written INTO the lexeme instead of a bare
number. Both executors fault on such a lexeme at evaluation time
("malformed number lexeme"); WF-081 makes the document ill-formed at
verification time, ahead of any executor.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is any golden numeric literal, e.g.
`corpus/golden/jutterswet-penningdrempel/ir.json`.
