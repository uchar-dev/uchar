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

Result: landed in full; WF-027 carries its executable golden. Both
executors run non-recursive object creation: a deterministic mint per
(rule × subject) — ref labels `ocN::<subject>` in a load-reserved
namespace, the ASP backend the function term ocN(Subject), both keyed
by the creation INDEX (the verification round's major: keying ref on
the nullable name let two name-less creations collide into one label
while clingo minted both — index keying and the "::" loader refusal
close it). Member initialisations evaluate on the subject; the
back-link lands on both ends of the uniquely-derivable connecting
relation and is now witnessed from the created side (houderscontrole:
a consistency counting the back-role — dropping either end's write
turns case 1 inconsistent in both executors; added after a lens showed
the back-link was entirely unobserved). Both mirrored idempotence
guards are anchor-pinned: the 1:1 linked-instance guard over the input
snapshot (an input-dossier blocks: 1) and the marker-as-mint (an
input-penning sits beside the mint: 2 totalling 12). The dependency
graph learned that a creation reads its subject population (strict):
creation-on-created-type orders correctly and a self-typed creation is
WF-045-rejected before either executor runs (verified by experiment in
the lens round). An ambiguous back-link faults in both executors
(recorded delta from v0's silent drop), red-pinned with the
creation-specific fragment.

Recorded, deliberately accepted: the guard reads subject-side stored
links only, and the snapshot narrowing means two 1:1 creations on one
to-one relation diverge (ref faults, clingo holds both — clingo README
limitation; a candidate WF in the single-writer family would close it
statically); recursive rule groups stay their own card.
