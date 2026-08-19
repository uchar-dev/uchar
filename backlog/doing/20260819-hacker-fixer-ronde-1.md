# The adversarial hacker–fixer session, round 1

Goal: the standing PLAN §8 adversarial job runs for the first time. A
hunting round of independent attackers searches for programs on which
the differential square diverges — a verifier-clean document plus case
where ref and clingo both produce an outcome but DISAGREE on it (a
silent split, the one thing the gate set cannot see coming) — or which
the verifier wrongly accepts (an admissible document that crashes an
executor without a fenced fault). A loud, reasoned fault on an
untranslated form is NOT a find: those are the recorded asymmetries.
Every find is minimized to the smallest document + case exhibiting it,
triaged (translation bug, ref bug, spec gap, verifier gap), FIXED, and
pinned: golden witnesses enter through `scripts/admit.sh`, red paths
through `corpus/rood/`, verifier gaps through `corpus/negatief/`. A
round that finds nothing is also a result — recorded honestly, not
padded.

Acceptance: every triaged find has a fix commit plus a pinned witness
(admitted golden, red, or negatief case); all eight gates green
(`bash scripts/check-*.sh`); the card's Result records each find with
its minimal witness and verdict, and the round's coverage (which attack
surfaces were hunted, what was not).

May touch: `backends/clingo/lib/translate.ml` and
`backends/clingo/bin/clingo_runner.ml` (fixes), `ref/lib/` (only if a
ref bug is proven — mirror discipline: v0 decides), `spec/verifier/lib/`
(verifier-gap fixes), `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`,
`spec/IR-SPEC.md`/`spec/RUNTIME-SEMANTICS.md` (only if a spec gap is
proven), `corpus/golden/synth-*/` (via admit.sh), `corpus/rood/<new>/`,
`corpus/negatief/<new>/`, `scripts/` (only a gate regression), `backlog/`.

Out of scope: ref/test file changes without explicit report; the
toolchain repo; new operator families (hunting happens INSIDE the
translated fragment plus the verifier's admissible surface); scale —
this is round 1 of a standing job, not an exhaustive sweep.
