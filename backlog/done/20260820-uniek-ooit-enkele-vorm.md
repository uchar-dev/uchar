# Ever-uniqueness for the single form — compositionally, one shared tuple function

Goal: the maintainer extends the 2026-08-20 decision (POLA,
compositionality): the SINGLE uniqueness form ("de vondstnummers van
alle Strandvondsten moeten uniek zijn") reads temporal keys exactly
like the union — ever-uniqueness — and does so through SHARED code,
not a parallel implementation. In ref one `ever_tuples` function
carries both forms (the single form composes it with its scope
machinery: scope parent × ever-tuples); in clingo one shared key-atom
helper emits the tl-join and both guards for either form. RS ch. 5.9
states the norm for both forms; a new anchor pins the single-form
ever collision (disjoint periods, one type), the redeclaration
non-collision, and the scope × ever composition; a red case pins the
two-temporal guard on the single form. The toolchain follows in the
same card (both Drools routes join the single form on the shared
`rsOoitBotst`; the resolver fence covers the single compound key; the
existing List-identity fixture's expectation CHANGES to the ever
reading — deliberately, that is the point). Separately landed first:
`rsOoitWaarden` now fails fast on the structurally unreachable
non-List input (toolchain `b188b35`) — impossible states scream.

Acceptance: all eight uchar gates green; the anchor replays
differentially (gates 5–6); the red case fails observably in both
executors (gate 8); toolchain `make test` and the differential suite
green with the single-form fixtures on the ever reading; both repos
pushed; CONSTRUCTS row 20 names the final pin.

May touch: `ref/lib/eval.ml`, `backends/clingo/lib/translate.ml`,
`spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md`,
`corpus/golden/anchor/jutterswet-strandregister/`,
`corpus/rood/<new>/`, `scripts/check-noop.sh` (a consistent-only case
is necessarily a reasoned survivor), `backlog/`; toolchain:
`RegelSpraakRuntime`, `DrlTranspiler`, `ExecModelTranspiler`,
`RuleChecker`, tests.

Out of scope: the same-moment tuple join (stays loud in both forms);
scoped unions (still unrepresentable in v0); the parent-scoped
temporal SINGLE form in the toolchain if v0's scoped emission cannot
carry it (record honestly if so).

Result: ever-uniqueness now reads the same wherever a temporal key
appears — compositionally, through shared code on every side. ref:
one `ever_tuples` function carries both uniqueness forms; the single
form composes it with its scope machinery (scope parent × ever-tuples,
deduplication inside the tuple function). clingo: one
`uni_key_guards`/`uni_key_atom` pair emits the tl-join and both loud
edges for either form. Toolchain (`0913a2d`, pushed; preceded by
`b188b35`, the maintainer-ordered fail-fast on the structurally
unreachable non-List — impossible states scream): both Drools routes
join single-form and parent-scoped temporal keys on the shared
`rsOoitBotst`, the `telTemporeel` fence covers the single compound
key, and the single-form differential fixture's expectation MOVED to
the ever reading — the content-equal timeline the old List-identity
join silently missed now flags, which is the point; 950 tests green.
Anchored by `jutterswet-strandregister` (the one-type ever collision
over disjoint years; the redeclaration non-collision; scope × ever
both ways — colliding under one kust, consistent under two), with the
two consistent-only cases as reasoned no-op survivors. Red-pinned:
`uni-two-temporal-keys` (the guard on the single form's own path) and
`uni-temporal-key-scalar` — the latter born from this card's verify
round, which caught a REAL introduced divergence: the first ref cut
decided scope participation before the key-shape guard, so a
scope-less instance with a scalar-supplied temporal key passed ref
while clingo faulted at translation. Fixed by the precedence the
maintainer's own fail-fast line implies — the form error screams
before participation is decided, in both executors — stated in RS
ch. 5.9 and pinned by the red case. The verify round also caught
CONSTRUCTS row 20 omitting the single-form conformance pin (added)
and the missing per-form scalar witness (added, per the round-1
precedent). Recorded anchor-strength limitation: {kind:inconsistent}
cannot pin WHICH rule fired — case3's discriminating power rests on
its distinct vondstnummers, noted for a future case-format decision.
Corpus: 100 running cases, 79 negatiefs, 12 red; ref ≡ clingo
everywhere; toolchain suite 950 green at `0913a2d`.
