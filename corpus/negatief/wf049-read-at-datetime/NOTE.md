Violates WF-049 (`spec/IR-SPEC.md`): the temporal read's `datum`
operand is typed `Date` with `precision: millisecond` — the read indexes
day-precision timelines, so its date operand must be a day-precision
date expression.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-aanmeldquotum/ir.json`.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The two date
representations are one `Date` node with a `precision`, `day` or
`millisecond` (delta B15 of `spec/proposals/V1-WIREMIGRATIE.md`); the
document is the translation of `spec/proposals/v1-migratie/m3.py`, and the
arm reads the precision. `expect.txt` pins the arm.
