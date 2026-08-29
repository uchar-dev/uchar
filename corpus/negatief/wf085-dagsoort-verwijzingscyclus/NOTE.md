Violates WF-085 (`spec/IR-SPEC.md` par. 5 / ch. 3.7): the day-kind
reference graph MUST be acyclic. A day-kind condition may reference other
day kinds (a `Predicaat` of soort `DAGSOORT`), but mutually recursive
definitions have no fixpoint. Here `vloeddag` references `ebdag` and
`ebdag` references `vloeddag` — a two-node cycle, refused loudly.

This is the diverge-from-v0 delta recorded on WF-031: the v0 toolchain
permits the references and resolves the cycle to a *silent false* (the
§9.10 prohibition unenforced). WF-085 makes the refusal structural.

Rood-eerst: before the check landed the pre-card verifier accepted this
document; the WF-085 reference-graph walk now rejects it, naming the
cycle.

A minimal hand-built document carrying only the two mutually referencing
day-kind definitions.
