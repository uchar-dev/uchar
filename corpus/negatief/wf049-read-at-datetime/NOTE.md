Violates WF-049 (`spec/IR-SPEC.md`): the temporal read's `datum`
operand is typed `DatumTijd` (millisecond precision) — the read indexes
day-precision timelines, so its date operand must be a day-precision
date expression.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-aanmeldquotum/ir.json`.
