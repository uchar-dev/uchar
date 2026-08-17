# Creation execution: the fact-creation form runs in both executors

Goal: the symbolic fact-creation form (`ResolvedFeitCreatie` with the
WF-028 extension fields, on the wire since the role-binding card) becomes
executable: `ref/` navigates the join chain and writes idempotent role
links into the World (ch. 5.5 — an already-present link is a no-op);
`backends/clingo` derives `rol/3` facts from the same chain as a body
join. The witness is a hand-written anchor
(`corpus/golden/anchor/jutterswet-gemeenteberging`) in which a creation
lays links and som/aantal folds *over the created relation* produce
absolute numbers — expressible in the existing case schema without link
expectations. WF-028's index row gets its executable golden.

Acceptance: all six gates green (`scripts/check-*.sh`); the new anchor
replays exactly in `ref/` (gate 5) and clingo (gate 6); the ch. 5.5
relation-creation clause carries a machine-checked worked example; the
WF-028 row in `spec/IR-SPEC.md` no longer says OPEN.

May touch: `ref/lib/eval.ml`, `ref/lib/world.ml`, `ref/lib/case.ml`
(split-key refusal, a verification finding),
`backends/clingo/lib/translate.ml`, `backends/clingo/README.md`,
`corpus/golden/anchor/jutterswet-gemeenteberging/` (new),
`corpus/golden/anchor/README.md` (table row),
`spec/verifier/lib/checks.ml` (dep_forms relation nodes — the
verification round's major: the ch. 6.1 traversal reads were specified
but not implemented), `spec/RUNTIME-SEMANTICS.md` (ch. 5.5 worked
example; ch. 6.1/6.2 relation-node text), `spec/IR-SPEC.md` (WF-index
row), `spec/CONSTRUCTS.md` (landing note), `backlog/`.

Out of scope: object creation (both executors keep faulting on
`objectCreaties` — toekenningen, instance minting and creation markers
are their own card); recursive rule groups (WF-033, ch. 3.9); link
expectations in the case schema; any emitter work in the toolchain repo
(pin-bump territory).

Result: landed in full, plus three verification-round hardenings. Both
executors run the symbolic fact-creation form: `ref/` navigates the
join chain over existing links and writes idempotent role links on
both ends (`World.add_link`, keyed by the readers' plural-then-name
preference; a second distinct filler on a to-one role faults), and the
ASP backend derives `rol/3` for both ends with explicit to-one
cardinality constraints (UNSAT exactly where ref/ faults — the
allocation-guard pattern, added after a verification lens showed the
silent divergence). The anchor `jutterswet-gemeenteberging` pins the
chain walk (one tussenstap + join hop), the idempotence (a vondst
reached along two jutters counts once: 51/4), the empty-chain readings
(0/0), and — added in the verification round — the far-end witness (an
aantal fold on the to-one `bergende gemeente` end: 1 per geborgen
vondst, 0 otherwise). WF-028's index row carries the golden.

The verification round's major: the ch. 6.1 table specified join-chain
traversal reads that dep_forms never implemented. Fixed by introducing
the relation node (`rel:feittype`): creations write it and read it per
hop (strict), bound folds/allocations read it; ch. 6.1/6.2 text
reconciled (WF-045 now names creation members of either kind). Also
fixed: the shared case loader refuses split-key role delivery (the
same role under name and plural keys made ref/ read half the links
while clingo unioned them — an empirically confirmed silent
divergence on loader-accepted input).

Deliberately left: object creation (both executors still fault on
`objectCreaties`); recursive-group execution; a golden for the to-one
red path (fault paths are not gate material until the expected-fault
case form lands — harness-guards card); clingo's condition subset for
creation guards stays fail-fast (a Predicaat/quantor guard faults the
translator, never silently skips).
