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

Result: four hunters (empty-edge composition, scales and number edges,
structural machinery, verifier surface) ran the first adversarial round;
the fixer triaged, fixed and pinned. The empty-value algebra HELD on
every composition probed (nested coalesce/propagation, channel edges
under conditions, per-subject channels, division in conditions) — an
honest negative. Four real classes were found, all reproduced minimally:
(1) silent unit drops — the additive join, the comparison and the write
boundary applied no conversion factor where ref restates (1 m + 50 cm
grounded as 51; kg-target from a t-expression as 2); fixed by the unit
fence, which after this card's OWN verification round reads the
EFFECTIVE unit derived from declarations (the annotation-based first
cut let a comparison of two eenheid:null-annotated plus composites slip
through — found by the verify lens, fixed before merge); WF-035's
compatibility clause now covers Verminderd_met (mirror-founded against
v0's resolver; negatief wf035-verminderd-met-incompatible-units).
(2) clingo's silent 32-bit wrap — 50000×50000 grounded negative and a
wrapped harmonised threshold flipped a branch; fenced by viol/2 range
guards on every expression-translation multiplication (clip W×N
included), plus input/expectation/scale faults at translation.
(3) the raw Z.Overflow crash on 10^20 inputs AND expectations — both
now reasoned faults. (4) hostile characters in declared names broke the
generated ASP's syntax unfenced — emission now faults; whether the SPEC
should constrain declared names (a WF rule, protecting other backends
too) is an open recommendation for the maintainer.
corpus/golden/synth-grensrand (admitted via scripts/admit.sh) pins the
safe side: the largest translatable product and identical-unit
join/comparison replay exactly. Recorded remainder for a later round:
the allocation recurrence, the uniqueness-union harmonisation and #sum
accumulation share the wrap class unguarded; aggregation/temporal
surfaces were only partially hunted (see the hunters' surfacesHunted
logs in the session record). Corpus stands at 73 running cases, 75
negatiefs, 6 red; ref ≡ clingo everywhere.
