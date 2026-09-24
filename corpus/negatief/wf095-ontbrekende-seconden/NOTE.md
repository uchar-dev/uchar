# Violates WF-095 — the seconds field left unwritten

The Gelijkstelling assigns the millisecond-precision date literal
`{"dateTime": "2026-03-15T12:00"}`. The seconds field is **always written**,
even when it is `00`.

This is the field on which the readers split in the other direction: the
Drools case reader accepted it (`LocalDateTime.parse` fills in `:00`),
where the shared interpreter/ASP case reader faulted `malformed dateTime`
and the literal reader read no value — so "the executors refuse a missing
seconds field" was false of one of them. Each reader now holds the fence
itself, and WF-095 puts it on the wire.

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
