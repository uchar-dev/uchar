Violates WF-031 (`spec/IR-SPEC.md` ch. 3.7, the purity clause's POSITION
arm, added 2026-09-19): a day-kind condition is evaluated per calendar
day (`RUNTIME-SEMANTICS.md` ch. 3.8), so its predicate must be a function
of THE DAY alone. The one such read admitted there is the bound day of the
definition itself, a `BoundDay`. A SESSION value makes a stratum-0
classification session-dependent: the same document would classify the
same calendar day differently in two runs, which is exactly what the
ch. 5.4 framing denies.

Here `jutterswet-schouwrooster`'s first day-kind comparison reads the
`ContextValue` `evaluationDate` where it read the bound day.

Rood-eerst, and this one has a recorded date: the mutation IS the
measurement of 2026-09-10 (the WF-098 card), which reported
`verifier: PASS` and `ref: oracle: PASS (7 expectations)` — the flank
WF-098 made visible and left open, because whether a session value may
enter a stratum-0 predicate is a semantics question and there was no
executor evaluating the predicate to make it one. With the duration forms
executing, there is.

Arm exactness, both directions. The arm names the two SESSION values
(`evaluationDate`, `evaluationYear`) and matches them through `norm` —
lower-cased and whitespace-collapsed — where WF-098 matches BYTE for byte.
The two rules therefore overlap on purpose rather than by accident: an
`EvaluationDate` in this position reports BOTH, WF-098 because the spelling
is not one of the two, WF-031 because the name it normalises to may not
stand here (measured on the L5 wire, 2026-09-25; the v0 spelling
`rekendatum` normalises to no L5 session value and reports WF-098 alone).
A name outside WF-098's closed set that is no session value stays WF-098's
alone — `wf098-onbekende-contextwaarde-naam` carries `de dagX` in this very
position and still reports WF-098 only — and the EMPTY half, a context
value without a name, cannot be written since L4: `name` is a required
string.

Measured over all 673 documents of the four zones plus the parity zone:
**31** `Constante` nodes sit inside a `resolvedDagsoorten` voorwaarde,
**29** of them `de dag` and the other two the witnesses named above, so the
arm costs nothing.

Since L4 the session value is a `ContextValue` (delta B23), since L5 named
`evaluationDate`, and the bound day of the definition is its own node,
`BoundDay`, which the rule admits; the message says so. The measurement above
counts the v0 `Constante` node in the v0 list `resolvedDagsoorten` (since L5
`dayKindDefinitions`); since L4 that node is a `ContextValue` or, for
`de dag`, a `BoundDay`.
`expect.txt` pins the arm.
