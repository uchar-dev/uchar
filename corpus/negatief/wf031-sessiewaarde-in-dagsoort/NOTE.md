Violates WF-031 (`spec/IR-SPEC.md` ch. 3.7, the purity clause's POSITION
arm, added 2026-09-19): a day-kind voorwaarde is evaluated per calendar
day (`RUNTIME-SEMANTICS.md` ch. 3.8), so its predicate must be a function
of THE DAY alone. The one context value admitted there is `de dag` — the
bound day of the definition itself. A SESSION value makes a stratum-0
classification session-dependent: the same document would classify the
same calendar day differently in two runs, which is exactly what the
ch. 5.4 framing denies.

Here `jutterswet-schouwrooster`'s first day-kind comparison reads
`Rekendatum` where it read `de dag`.

Rood-eerst, and this one has a recorded date: the mutation IS the
measurement of 2026-09-10 (the WF-098 card), which reported
`verifier: PASS` and `ref: oracle: PASS (7 expectations)` — the flank
WF-098 made visible and left open, because whether a session value may
enter a stratum-0 predicate is a semantics question and there was no
executor evaluating the predicate to make it one. With the duration forms
executing, there is.

Arm exactness, both directions. The arm names the two SESSION values
(`Rekendatum`, `Rekenjaar`) and nothing else, so a name outside WF-098's
closed set stays WF-098's alone — `wf098-onbekende-contextwaarde-naam`
carries `de dagX` in this very position and still reports WF-098 only —
and the EMPTY half (`naam: null`) is untouched, the open point ch. 4.1
records. Measured over all 671 documents of the four zones plus the parity
zone: 26 `Constante` nodes sit inside a `resolvedDagsoorten` voorwaarde
and every one of them is `de dag` but the two witnesses named here, so the
arm costs nothing.
