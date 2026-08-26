# Violates WF-084 — the raw source snippet as the lexeme

The Gelijkstelling assigns the date literal `"dd. 31-01-2024"`: the raw
RegelSpraak source snippet, `dd. ` de-dato prefix and day-first order
and all, written straight into the value string — exactly what the
emitter produced before its fix. The executors parse only the ISO
`yyyy-mm-dd` form and fault on this lexeme at evaluation (ref) or
translation (the ASP backend) time; WF-084 makes such a document
ill-formed at verification time, ahead of any executor. The lexeme
belongs in ISO form; the source snippet belongs in `bron`.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the ISO date literal in
`corpus/golden/jutterswet-vondstverjaring/ir.json`.
