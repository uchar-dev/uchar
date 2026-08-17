# Object creation executes in both executors

Goal: `ResolvedObjectCreatie` (non-recursive) becomes executable. For
each subject whose condition holds, `ref/` mints a deterministic
instance of the created type, applies the member initialisations
(attribute values evaluated on the subject; characteristic constants),
and links the new object to its subject over the uniquely-derivable
connecting relation (both ends) — mirroring the pinned runtime's
idempotence contract: a 1:1 shape guards on the absence of an already
linked instance (a case-supplied linked instance blocks the creation),
the to-many/linkless shapes guard per (rule × subject) via the
deterministic label. The ASP backend mints the instance as a
deterministic function term per subject, so idempotence is set
semantics. The dependency graph learns that an object creation reads
its subject *population* (strict) — a self-typed creation becomes a
WF-045 cycle instead of a diverging fixpoint in ref/ or an unbounded
grounding in clingo, and creation-after-creation chains order
correctly. Witness: a hand-written anchor where creations mint objects
that folds over the connecting relation count and sum from the
subjects, with a consistency reading a characteristic set on the
created objects. WF-027's index row gets its executable golden.

Acceptance: all eight gates green; the new anchor replays exactly in
both executors; ch. 5.5 carries a machine-checked worked example for
non-recursive object creation; the WF-027 row no longer says OPEN.

May touch: `ref/lib/eval.ml`, `backends/clingo/lib/translate.ml`,
`backends/clingo/lib/model.ml` (function-term arguments in the atom
parser), `backends/clingo/README.md`, `spec/verifier/lib/checks.ml`
(dep_forms: subject-population read; connecting-relation write),
`corpus/golden/anchor/jutterswet-penninguitgifte/` (new),
`corpus/golden/anchor/README.md`, `corpus/rood/` (the
ambiguous-connecting-relation fault path, if it lands cleanly),
`spec/RUNTIME-SEMANTICS.md` (ch. 5.5 worked example + ch. 6.1 row),
`spec/IR-SPEC.md` (WF-index row), `spec/CONSTRUCTS.md` (row 21 landing
note), `backlog/`.

Out of scope: recursive rule groups (`regelGroepen` keep faulting —
WF-033/ch. 3.9 execution is its own card); ref/test files; new WF
rules; emitter work.
