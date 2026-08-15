# The first Jutterswet golden corpus case

Goal: the first entry in `corpus/golden/`: a minimal, rights-free
Jutterswet RegelSpraak source (own fiction: a kern model with an object
type, a parameter, and at least one rule exercising attribute reference,
parameter reference, comparison, and characteristic attribution) that
resolves with zero errors at the pinned toolchain commit `c51652a`, plus
its `ir.json` emitted by `--ir` — giving gate 2 its first in-repo
instance (validate + byte-identical round-trip).

Acceptance: `scripts/check-ir-schema.sh` reports 1 file validating and
round-tripping; `scripts/check-self-contained.sh` passes (zone rules: no
private-repo names, no machine-local paths, no TOKA); the source resolves
with zero resolver errors at the pin (command + output shown).

May touch: `corpus/golden/` (NOT `corpus/golden/anchor/` — protected),
`backlog/`.

Out of scope: expected-outcome files (they need the scenario-format
decision, `CONSTRUCTS.md` row 37); the anchor subset; negatief-corpus
cases; quantitative extension articles.

Result: `corpus/golden/jutterswet-penningdrempel/` — the first golden
case: a minimal Jutterswet source (object type Jutter with a bijvoeglijk
kenmerk and a non-negative whole-number attribute, parameter
vondstdrempel, one kenmerktoekenning rule "groter of gelijk aan") that
resolves with zero errors and zero warnings at pin `c51652a`, plus its
`--ir` dump. The IR exercises AttribuutRef, ParameterRef, Vergelijking
(GROTER_OF_GELIJK), and characteristic attribution. Gate 2 now has its
first in-repo instance: 1 file validates and round-trips byte-identically;
self-containedness clean (no paths, no private names, no TOKA —
independently grepped). Authored in one iteration; grammar-register
lessons (vragende vs stellende comparison forms, member termination,
parameter colon) recorded in the session report for future corpus
authoring. Deliberately left: expected-outcome files (await the
scenario-format decision), anchor cases, negatief cases.
