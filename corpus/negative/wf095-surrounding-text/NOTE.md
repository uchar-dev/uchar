# Violates WF-095 — a lexeme with text around it

The Gelijkstelling assigns the millisecond-precision date literal
`{"dateTime": "rommel 2026-03-15T12:00:00 x"}`. The lexeme **is** the value,
not a substring inside it: the canonical spelling is matched whole.

This is the measurement that made the case for a numbered rule. Before WF-095
a document whose millisecond-precision date literal read like this was
schema-valid and `verifier: PASS` (measured 2026-09-06) — the schema typed a
literal's `waarde` as a plain string with no pattern, and nothing on the wire
looked at it. The reader was the only fence, and a reader that scans for a
date and a time ANYWHERE in the value finds one here. Since L4 the schema's
`dateTime` variant carries a shape pattern (delta B22); the negative zone is
not validated against the schema.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The literal's type
is a `Date` with `precision: millisecond` (delta B15 of
`spec/proposals/V1-WIREMIGRATIE.md`), and that precision is what makes WF-095
the lexeme's rule; the document is the translation of
`spec/proposals/v1-migratie/m3.py`. The report is unchanged but for the
name of the date, which the message now gives by its precision.

Since L4 the literal's value is typed (delta B22): the ill-formed string sits
in its `dateTime` variant, which the rule reads, and the report cites
`/value`. `expect.txt` pins the arm.
