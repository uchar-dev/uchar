Violates WF-031 (`spec/IR-SPEC.md`): a day-kind definition MUST carry the
always validity (neither `from` nor `to` written). Here the `schouwdag`
definition is well-formed — its condition is present — but its `validity`
bounds the window at `from: 2026-01-01`. No executor consults a day-kind
validity window, so the IR refuses what nothing can honour.

Since `validity` is a required field, the always form is a `Validity` node
with neither bound written, not an omitted field — as the passing
witnesses `corpus/golden/jutterswet-schouwkalender` and
`corpus/golden/jutterswet-schouwrooster` carry, both of them replayed by
gates 5–6. (Until 2026-09-19 this sentence named
`corpus/golden/jutterswet-vormenkabinet`; the day-kind definitions left
that cabinet for those two when the cabinet shrank, and its `dayKinds`
now stand there declared and registered but undefined.)

A minimal hand-built witness; own material, no casus content.
