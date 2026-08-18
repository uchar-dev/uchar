# Recursive rule groups execute: the one-generation chain runs to its fixpoint in both executors

Goal: the `recursive` marker gets its neutral meaning (CONSTRUCTS row
14): membership of a recursive group is exactly what WF-045 already
admits — a creation whose output feeds its own condition — and both
executors run the chain to its fixpoint. `ref/` learns to assign
evaluation strata in the presence of the admissible cycle (edges inside
a dependency cycle count positive — ch. 6.3's discharge-by-generation
clause, implemented); its global pass budget is the ch. 5.5 fault. The
ASP backend bounds the otherwise-unbounded grounding with a
per-instance generation depth (`depth/2`, `#const oc_budget`) whose
exhaustion derives a `viol/2` witness — UNSAT exactly where ref/'s
budget faults. Because a self-typed chain cannot name its back-link
role by derivation (both roles fill the same type; the pinned runtime
returns null and drops the link), `ResolvedObjectCreatie` gains the
`verbindendeRol` extension field (wire-entry discipline): a RoleBinding
naming the role the CREATED object's far side... the created-side role
explicitly; the derivation remains the bindingless fallback, and the
self-typed two-role derivation now mirrors v0's null (no link, marker
idempotence) instead of silently picking a role.

Witness: `corpus/golden/anchor/jutterswet-wachtersketen` — a recursive
group whose creation chains Wachter rank n to n+1 while n < 5 over the
`opvolging` relation (bound via the new field); the direct-successor
fold pins generation 2 exactly, a detector consistency pins that
generation 5 is reached (inconsistent), and green-within-budget pins
termination. The red witness `corpus/rood/runaway-recursion` pins the
budget fault in both executors (ref: no fixpoint within the pass
budget; clingo: generation budget exhausted → UNSAT).

Acceptance: all eight gates green; WF-033's row carries the executable
golden; the ch. 5.5 recursive worked example is machine-checked in its
measurable form; the Method extension list and ch. 3.3/3.9 record the
new field and the interpreted marker.

May touch: `spec/verifier/lib/{ast,loader,checks}.ml`,
`spec/schema/regelspraak-ir-v0.schema.json`, `ref/lib/eval.ml`,
`backends/clingo/lib/translate.ml`, `backends/clingo/README.md`,
`corpus/golden/anchor/jutterswet-wachtersketen/` (new),
`corpus/golden/anchor/README.md`, `corpus/rood/runaway-recursion/`
(new), `corpus/negatief/` (one new WF-047 creation-binding case),
`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md`,
`backlog/`.

Out of scope: recursive FACT-creation chains (idempotent, no guard
needed per WF-033 — recorded untested); emitting `verbindendeRol` on
creations from the toolchain (pin-bump territory); channel-content
expectations (the exact end-rank pin beyond the detector's granularity
is recorded as future case-schema work).
