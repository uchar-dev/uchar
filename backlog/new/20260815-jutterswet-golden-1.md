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
