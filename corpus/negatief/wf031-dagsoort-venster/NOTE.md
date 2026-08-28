Violates WF-031 (`spec/IR-SPEC.md`): a day-kind definition MUST carry the
ALTIJD validity (vanaf and tot both null). Here the `schouwdag` definition
is well-formed — its condition is present — but its `geldigheid` bounds the
window at `vanaf: 2026-01-01`. No executor consults a day-kind validity
window, so the IR refuses what nothing can honour.

Since `geldigheid` is a required v0 field, the ALTIJD form is the explicit
`vanaf: null, tot: null` object, not an omitted field — as the passing
witness `corpus/golden/jutterswet-vormenkabinet` carries.

A minimal hand-built witness; own material, no casus content.
