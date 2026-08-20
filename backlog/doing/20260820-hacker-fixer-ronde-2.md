# The adversarial hacker–fixer session, round 2: the timeline machinery

Goal: the standing PLAN §8 adversarial job runs its second round,
aimed at the youngest code — the timeline machinery that landed since
round 1: the pro-rata fold (`Timeline.pro_rata`, `run_pro_rata`,
`translate_pro_rata`), the `attributeTimeline` checkers on both sides,
the day-precise characteristic-timeline loader (the alignment
relaxation), the gedurende clip sharing the ctl machinery, and their
interactions (pro-rata and clip on one document, multiple rules
writing temporal targets, timeline reads feeding scalar rules). A find
is a verifier-clean document + case where the executors DISAGREE
silently, an unfenced crash (raw exception or broken generated ASP),
or a verifier gap; a loud reasoned fault is not a find. Every find is
minimized, triaged, FIXED and pinned (golden via `scripts/admit.sh`,
red via `corpus/rood/`, verifier gap via `corpus/negatief/`). A round
that finds nothing on a surface is recorded honestly.

Acceptance: every triaged find has a fix commit plus a pinned witness;
all eight gates green (`bash scripts/check-*.sh`); `dune build
@runtest` green; the Result records each find with its minimal witness
and verdict plus the round's coverage (surfaces hunted, surfaces not).

May touch: `backends/clingo/lib/` (fixes), `ref/lib/` (only a proven
ref bug — mirror discipline: v0 decides), `spec/verifier/lib/`
(verifier-gap fixes), `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`,
`spec/IR-SPEC.md`/`spec/RUNTIME-SEMANTICS.md` (proven spec gaps),
`corpus/golden/synth-*/` and `corpus/golden/synth2-*/` (via admit.sh),
`corpus/rood/<new>/`, `corpus/negatief/<new>/`, `backlog/`.

Out of scope: ref/test changes without explicit report; the toolchain;
new operator families or the TemporalRead-derived-timeline extension
(hunting happens INSIDE the executable fragment plus the verifier's
admissible surface); scale — one round of a standing job.
