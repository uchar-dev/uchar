# Violates WF-095 — a lexeme with text around it

The Gelijkstelling assigns the datum-tijd literal
`"rommel 2026-03-15T12:00:00 x"`. The lexeme **is** the value, not a
substring inside it: the canonical spelling is matched whole.

This is the measurement that made the case for a numbered rule. Before
WF-095 a document whose datum-tijd literal read like this was
schema-valid and `verifier: PASS` (measured 2026-09-06) — the schema
types a literal's `waarde` as a plain string with no pattern, and nothing
on the wire looked at it. The reader was the only fence, and a reader
that scans for a date and a time ANYWHERE in the value finds one here.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`.
