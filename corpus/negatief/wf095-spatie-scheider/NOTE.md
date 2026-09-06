# Violates WF-095 — a space where the canonical lexeme writes `T`

The Gelijkstelling assigns the datum-tijd literal
`"2026-03-15 12:00:00"`: the canonical field widths, but a **space** for
the `T` separator.

It is not the source snippet either — that form is day-first
(`15-03-2026 12:00:00`), and the year's field position is exactly what
tells the two admitted spellings apart. A lexeme that is ISO-ordered with
a snippet separator is neither, so it is refused rather than guessed at:
the literal reader used to take the space for a `T` and answer an instant
the producer had not written.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`.
