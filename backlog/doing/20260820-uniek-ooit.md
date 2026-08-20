# Temporal union keys read ever-uniqueness (maintainer decision, step 1)

Goal: the recorded v0 contradiction on temporal keys in uniqueness
unions (one Drools route skipped the union, the other checked it) is
resolved by maintainer decision (2026-08-20): a key is an identity,
not a measurement — **ever-uniqueness**. Every value a key timeline
EVER carries is claimed; two instances collide even over
non-overlapping periods; same-instance redeclaration is no collision
(the tuple set deduplicates per instance × source); date-bound
numbering is modelled explicitly instead. Both executors replace their
loud fault with this reading; at most one temporal component per
source executes (the same-moment tuple join stays loud), and a
temporal key supplied as a bucket scalar faults (the full timeline is
required). RS ch. 5.9 carries the norm; an anchor pins the
non-overlapping collision absolutely; red cases pin the two remaining
loud edges in both executors. Step 2 — teaching BOTH v0 Drools routes
the same reading — is toolchain work for its own conformance card.

Acceptance: all eight gates green; the anchor replays differentially
(gates 5–6); both red cases fail observably in both executors with
their pinned fragments (gate 8).

May touch: `ref/lib/eval.ml`, `backends/clingo/lib/translate.ml`,
`spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md`,
`corpus/golden/anchor/jutterswet-nummerregister/`,
`corpus/rood/<new>/`, `backlog/`.

Out of scope: the toolchain (step 2, own card — a new-card is left in
`backlog/new/`); the same-moment tuple join for multiple temporal
components; scoped unions (still unrepresentable in v0).
