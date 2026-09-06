# Violates WF-095 — a fraction that is not three digits

The Gelijkstelling assigns the datum-tijd literal
`"2026-03-15T12:00:00.1"`. The fractional part is optional, but when it
is written it is **exactly three digits**, read as whole milliseconds.

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
