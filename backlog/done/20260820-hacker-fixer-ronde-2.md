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

Result: four hunters on the timeline machinery; two real classes, both
double-confirmed, fixed and pinned — and the verify round caught a
third instance of the first class inside the fix itself.
(1) The derived-wall zero: a gedurende clip touching a pro-rata-DERIVED
timeline silently totalled 0 in clingo — the wu cutset carried only
case endpoints, never the fold's interior bucket walls. Found on the
clipped source by two hunters independently (ref 24/36 exact, clingo
0); the first fix enriched only the clipped attribute, and the verify
lens reproduced the identical zero through a derived CONDITION
comparison (ref 20, clingo 0) — the enrichment now covers every
derived attribute the clip touches. Pinned by
synth2-clip-over-prorata (clipped side, 24) and
synth2-clip-cond-over-prorata (condition side, 20), both admitted
through the protocol — the latter after gate 2 rolled back a
non-canonical first candidate, the protocol doing its job.
(2) The computed-target input union: a case supplying input for a
Berekening/Gelijkstelling target split the executors — ref (like v0)
overwrites, clingo's monotone model unions case fact and rule head
into a superset. Found independently by three hunters (symmetrically
proven). The translator now refuses loudly; deliberately over-broad
(it also refuses the condition-false-everywhere subcase where the two
agreed) because per-instance non-firing is undecidable at translation
and the firing subcase has no monotone encoding — Phase 4's default
calculus is the principled home (ladder targets already carry
inputval/3). Recorded loud asymmetry: prose-pinned in ch. 6d only, as
with the 6c asymmetries — such cases cannot enter golden (clingo
faults), rood (ref does not fault) or negatief (the document is
well-formed).
Hunted and HELD (honest negatives): the fold's own bucket-wall edges,
base gaps at bucket starts, out-of-range condition periods, shared
condition facts across rules, overlapping/unsorted ctl rejection,
reverse-ordered timeline expectations, adjacent-equal-segment
handling, pro-rata in operand/creation/ladder positions (loud on both
sides), and the year-clip-over-month-derived non-split (whole-units 0
on both sides). Not hunted: granular-parameter bases, the negated
clip complement (ref-only), Temporal_read of derived timelines
(fenced). Corpus: 92 running cases, 79 negatiefs, 6 red; ref ≡ clingo
everywhere.
