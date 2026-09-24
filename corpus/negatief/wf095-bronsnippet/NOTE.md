# Violates WF-095 — the day-first source snippet in `waarde`

The Gelijkstelling assigns the millisecond-precision date literal
`"15-03-2026 12:00:00.000"`: the RegelSpraak source spelling, day-first,
where the lexeme is the canonical ISO form `2026-03-15T12:00:00.000`.

Until 2026-09-22 WF-095 admitted this spelling beside the canonical one,
because the repository's own emitter wrote it into `waarde`. The emitter
now writes the ISO lexeme there and keeps the snippet in `bron`, as it
already did for a day-precision date (WF-084), and the rule narrowed to
the one spelling. This document was verifier-clean under the two-spelling
rule (measured 2026-09-22 before the narrowing), which is what makes it
the witness: the other five `wf095-*` documents each carry a defect the
old rule refused too, so readmitting the snippet would silence none of
them and only this one.

The executors' readers still read this lexeme as the same instant as the
canonical one; the refusal is the wire's, not a reader's.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`, whose `bron` carries this
same snippet.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The literal's type
is a `Date` with `precision: millisecond` (delta B15 of
`spec/proposals/V1-WIREMIGRATIE.md`), and that precision is what makes WF-095
the lexeme's rule; the document is the translation of
`spec/proposals/v1-migratie/m3.py`. The report is unchanged but for the
name of the date, which the message now gives by its precision. `expect.txt`
pins the arm.
