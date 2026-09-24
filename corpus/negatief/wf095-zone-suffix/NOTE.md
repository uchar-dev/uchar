# Violates WF-095 — a zone suffix on a millisecond-precision date lexeme

The Gelijkstelling assigns the millisecond-precision date literal
`"2026-03-15T12:00:00Z"`. The canonical ISO lexeme carries **no zone
suffix and no offset**: an instant on the wire is a local civil
date-and-time, and a `Z` claims something the millisecond layer does not
represent.

This is the one form all three readers of such a lexeme already
refused (the literal reader and both case readers, measured 2026-09-06) —
and the wire did not: schema-valid, `verifier: PASS`. WF-095 closes that,
so the document is ill-formed ahead of any executor.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The literal's type
is a `Date` with `precision: millisecond` (delta B15 of
`spec/proposals/V1-WIREMIGRATIE.md`), and that precision is what makes WF-095
the lexeme's rule; the document is the translation of
`spec/proposals/v1-migratie/m3.py`. The report is unchanged but for the
name of the date, which the message now gives by its precision. `expect.txt`
pins the arm.
