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
