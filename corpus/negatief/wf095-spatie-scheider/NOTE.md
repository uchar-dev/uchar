# Violates WF-095 — a space where the canonical lexeme writes `T`

The Gelijkstelling assigns the datum-tijd literal
`"2026-03-15 12:00:00"`: the canonical field widths, but a **space** for
the `T` separator.

It is not the source snippet either — that form is day-first
(`15-03-2026 12:00:00.000`) and is refused on the wire in its own right
since 2026-09-22 (`wf095-bronsnippet`). A lexeme that is ISO-ordered with
a snippet separator was refused even while the rule still admitted the
snippet, rather than guessed at: the literal reader used to take the
space for a `T` and answer an instant the producer had not written.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`.
