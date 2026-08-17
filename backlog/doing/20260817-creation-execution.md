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

May touch: `ref/lib/eval.ml`, `ref/lib/world.ml`,
`backends/clingo/lib/translate.ml`, `backends/clingo/README.md`,
`corpus/golden/anchor/jutterswet-gemeenteberging/` (new),
`corpus/golden/anchor/README.md` (table row),
`spec/RUNTIME-SEMANTICS.md` (ch. 5.5 worked example),
`spec/IR-SPEC.md` (WF-index row), `spec/CONSTRUCTS.md` (landing note),
`backlog/`.

Out of scope: object creation (both executors keep faulting on
`objectCreaties` — toekenningen, instance minting and creation markers
are their own card); recursive rule groups (WF-033, ch. 3.9); link
expectations in the case schema; any emitter work in the toolchain repo
(pin-bump territory).
