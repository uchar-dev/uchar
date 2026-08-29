# Adopt the Phase-4 standing norm: the input default executes

Goal: execute the maintainer-adopted core of
`spec/proposals/PHASE4-DEFAULTS.md` (decisions 1–5 and 7 per its §10;
decision 6 waits for a pin-bump card). Concretely: (a) the v1 norm "a
version-free computing rule that is its target's only writer decides
like a default — a leeg evaluation writes nothing, the input value stands"
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

Result: the standing norm executes. Landed: RS ch. 7.8 (the virtual
input default + the norm, worked examples verbatim from the new anchor);
conflict-fact readability closed for v1 (RS ch. 7.2, IS ch. 6.1);
the ch. 1.2 assignment exception; CONSTRUCTS rows 8/18 delta entries
(equal-decisions and observable-not-fatal named as tracked Catala
deviations; the Phase-5 nested-group direction recorded); the shared
domain predicates `Ir_v0.Desugar.standing_targets`/`desugared_targets`;
ref's no-write-on-leeg over both domains; clingo's generalised
`inputval/3` with a value-carrying effectiveness atom (`standN_eff`);
the ch. 6d fence retired for standing targets, retained for
multi-writer/versioned/temporal; a NEW fence on allocation-target case
input (red-first: `[999;60]` in one model, ref green over it — the
latent superset gap); the ch. 6d postscript; anchors:
`jutterswet-waardestelling` (4 cases, red-first: ref erased the input
on the leeg corner, clingo refused all three input cases) and two
additive `jutterswet-waardering-v0vorm` cases pinning the
fire-but-leeg corner the breaker lens reproduced (ref faulted its pass
budget where clingo answered — now both give 25/40, the RS ch. 7.7
prose pin graduated to a differential witness). Three reason-carrying
gate-7 survivors added. All eight gates green (106 cases, 9 survivors).

Honest corrections: the anchor was placed DIRECTLY, not through
scripts/admit.sh as the acceptance text assumed — admit.sh targets
corpus/golden/ (not the anchor zone), refuses nested names, and
refuses no-op survivors by design; the gate set is the admission
evidence, per the hand-written-anchor rule. "May touch" was widened
mid-card (recorded in the card) with additive cases for the
waardering-v0vorm anchor.

Deliberately left: temporal computing targets stay fenced (recorded in
RS ch. 7.8 — scalar-first per decision 3); the toolchain untouched
(decision 6 waits for a pin-bump card; the standing delta separates v1
from v0 there and is tracked in CONSTRUCTS row 8); the over-precision
loader divergence (ref accepts 40.005 on a 2-decimal attribute, clingo
faults) is the new card backlog/new/20260820-input-precision-fence.md;
the hardship anchor is its own later card.
