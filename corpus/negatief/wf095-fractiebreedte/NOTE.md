# Violates WF-095 — a fraction that is not three digits

The Gelijkstelling assigns the millisecond-precision date literal
`{"dateTime": "2026-03-15T12:00:00.1"}`. The fractional part is optional, but
when it is written it is **exactly three digits**, read as whole milliseconds.

This is the field the readers disagreed about, and the disagreement is
the reason for the width. The same three characters `.1` read as **one**
millisecond in the shared interpreter/ASP case reader, as **a hundred**
in the Drools case reader (which reads the fraction positionally), and as
no value at all in the literal reader — one wire, three instants.
`.100000` was worse: a hundred THOUSAND milliseconds against a hundred, a
minute and forty seconds apart. Three digits is the only width every
reader agrees on, which is why the grammar admits no other.

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
