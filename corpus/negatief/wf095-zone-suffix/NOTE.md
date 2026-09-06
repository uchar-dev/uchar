# Violates WF-095 — a zone suffix on the datum-tijd lexeme

The Gelijkstelling assigns the datum-tijd literal
`"2026-03-15T12:00:00Z"`. The canonical ISO lexeme carries **no zone
suffix and no offset**: an instant on the wire is a local civil
date-and-time, and a `Z` claims something the millisecond layer does not
represent.

This is the one form all three readers of a datum-tijd lexeme already
refused (the literal reader and both case readers, measured 2026-09-06) —
and the wire did not: schema-valid, `verifier: PASS`. WF-095 closes that,
so the document is ill-formed ahead of any executor.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`.
