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
`corpus/rood/<new>/`, `scripts/check-noop.sh` (the consistent anchor
case is necessarily a no-op survivor — the reasoned survivor entry is
gate-required), `backlog/`.

Out of scope: the toolchain (step 2, own card — a new-card is left in
`backlog/new/`); the same-moment tuple join for multiple temporal
components; scoped unions (still unrepresentable in v0).

Result: the maintainer decision lands as a v1 norm. Both executors
replace the recorded v0-contradiction fault with ever-uniqueness: ref
builds the per-instance tuple set (scalars × every value the one
temporal component ever carries, deduplicated per instance × source so
same-instance redeclaration never self-collides), clingo joins the
key atom on the timeline facts — set semantics deduplicates for free,
and the (instance × source) count keeps v0's self-duplicate-via-two-
sources summation. RS ch. 5.9 carries the norm WITH the worked example
(added on the verify lens's demand — the spec-discipline rule);
CONSTRUCTS row 20 records the decision and its artifacts. Anchored by
jutterswet-nummerregister: the non-overlapping 7/7 collision, the
redeclaration non-collision (a reasoned no-op survivor — the absence
of the channel fact IS the assertion), and the mixed compound tuple
colliding across different moments. Two red edges pinned in both
executors: two temporal components per source (the same-moment tuple
join stays loud) and a scalar-supplied temporal key (the full timeline
is required) — the latter's fragment sharpened to "ever-unique key"
after the lens showed the first pin matched two unrelated fault
families. The semantics lens probed the self-duplicate, empty-scalar,
mixed-decimal and derived-timeline edges without a find. Recorded
pre-existing asymmetry (not introduced here): clingo's union
translation ignores uu_geldigheid where ref gates on it — unreachable
in the corpus (all unions carry null windows). Protected paths touched
(RS, CONSTRUCTS, check-noop.sh survivor entry) under the standing
merge mandate. Step 2 — both v0 Drools routes learn the same reading —
lies in backlog/new/20260820-toolchain-uniek-ooit-conformance.md.
Corpus: 96 running cases, 79 negatiefs, 9 red; ref ≡ clingo everywhere.
