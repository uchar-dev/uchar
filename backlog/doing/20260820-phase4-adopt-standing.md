# Adopt the Phase-4 standing norm: the input rung executes

Goal: execute the maintainer-adopted core of
`spec/proposals/PHASE4-DEFAULTS.md` (decisions 1–5 and 7 per its §10;
decision 6 waits for a pin-bump card). Concretely: (a) the v1 norm "a
version-free computing rule that is its target's only writer decides
like a rung — a leeg evaluation writes nothing, the input value stands"
lands in `RUNTIME-SEMANTICS.md` ch. 7.8 with the ch. 7.7/1.2 updates,
executed for scalar targets in both executors (ref: a no-write on leeg
in the rule arm over the shared domain predicate; clingo: `inputval/3`
generalised to these targets with an effectiveness atom carrying the
value bindings, retiring the ch. 6d fence there); (b) conflict-fact
readability closed (RS ch. 7.2, IR-SPEC ch. 6.1); (c) the CONSTRUCTS
row-8/18 delta entries (equal-decisions deviation, observable-not-fatal
conflict, the leeg-standing delta, the Phase-5 nested-group direction);
(d) a new fence on allocation-target case input (the latent superset
gap); (e) the anchor `jutterswet-waardestelling` (the proposal's
appendix example, four cases) admitted through `scripts/admit.sh`, with
the two standing survivors pinned in `scripts/check-noop.sh`. Temporal
computing targets stay fenced (recorded; scalar-first per decision 3).

Acceptance: red-first demonstrations recorded (the clingo fence refusing
the flank case; ref erasing input on a computed leeg; the allocation
superset unfenced); after the change all eight gates green including the
new anchor; ref ≡ clingo on all four new cases; two adversarial opus
lenses ran and their findings are fixed.

May touch: `backlog/`, `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`,
`spec/CONSTRUCTS.md`, `spec/ASP-TRANSLATION.md`,
`spec/verifier/lib/desugar.ml`, `ref/lib/eval.ml`,
`backends/clingo/lib/translate.ml`, `backends/clingo/README.md`,
`scripts/check-noop.sh`, `corpus/golden/anchor/jutterswet-waardestelling/`,
and (widened in the verification round, additive cases only)
`corpus/golden/anchor/jutterswet-waardering-v0vorm/` — the breaker lens
found the fire-but-leeg corner divergent there and the fix pins it.

Out of scope: the toolchain (decision 6, pin-bump card); timeline
execution of the norm (recorded, follow-up); the hardship anchor (own
card); any schema change (the proposal adds zero wire surface).
