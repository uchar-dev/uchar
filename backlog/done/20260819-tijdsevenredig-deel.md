# TIJDSEVENREDIG_DEEL executes: the pro-rata fold enters both executors

Goal: the time-proportional part (RUNTIME-SEMANTICS ch. 2.8: per
granularity bucket, result = base × days-true/days-in-bucket as an
exact fraction) stops being a loud fault and executes in ref and in
clingo, mirroring v0's fold exactly — bucket boundaries, the empty
edges (empty base, a bucket without true days, gaps in the condition
timeline), and the mandatory-rounding interaction where the fraction
does not terminate. The condition-timeline machinery of the gedurende
clip (characteristicTimelines, `Timeline.clip`/`true_periods_of`, the
calendar-as-data encoding and the endpoint cutset in clingo) is reused,
not duplicated. An anchor with hand-computed absolute values pins every
bucket edge; an emitter golden pins the wire shape at the working pin;
negatiefs pin any new well-formedness condition the form demands
(WF-037 already carries the granularity facet rule). The ch. 2.8/3
prose gains the worked example verbatim as executed; CONSTRUCTS row
notes record the landing.

Acceptance: all eight gates green (`bash scripts/check-*.sh`); the new
anchor and emitter-golden cases replay differentially (gates 5 and 6);
`dune build @runtest` green; the ref eval fault "granularity-bucketed
aggregation not implemented" is gone for this kind and remains for any
kind the card does not land.

May touch: `ref/lib/eval.ml`, `ref/lib/timeline.ml`, `ref/lib/case.ml`
(only if the case form needs a field), `backends/clingo/lib/translate.ml`,
`spec/verifier/lib/checks.ml` (only for a new WF edge),
`spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md` (index/facet notes),
`spec/CONSTRUCTS.md`, `corpus/golden/anchor/jutterswet-<new>/`,
`corpus/golden/jutterswet-<new>/`, `corpus/negatief/<new>/`,
`corpus/rood/<new>/` (if a red edge emerges), `backlog/`.

Out of scope: AANTAL_DAGEN_IN (same facet, own card); ref/test file
changes without explicit report; toolchain changes; date construction
and the remaining date builtins.

Result: TIJDSEVENREDIG_DEEL executes in both executors as the first
timeline-PRODUCING rule form (RS ch. 3.7 verbatim): one segment per
calendar bucket the gedurende condition touches, value = base(bucket
start) × true-days/bucket-days as an exact fraction — no cross-bucket
summation, no coalescing, no auto-rounding. ref gained
Timeline.pro_rata, World.write_timeline (idempotent, no ch. 2.4
collapse — v0 stores the fold un-rounded) and the run_pro_rata rule
route; clingo grounds the calendar as compile-time bucket facts
(46340-bounded span), intersects condition segments four-case into
per-bucket true days (#sum, count-guarded), and derives tl/5 heads
under an exactness guard — a non-terminating fraction is a loud viol,
so it cannot enter the differential corpus un-rounded (RS note added).
The case format gained the attributeTimeline expectation
(segment-exact; empty pins empty, and a never-written target equals
the empty timeline in BOTH checkers); characteristic periods are
day-precise like the pinned runtime — the grid-alignment
over-restriction on characteristicTimelines is dropped. WF-056 pins
the shape with FOUR negatives (no condition, scalar base,
bucket/target mismatch, non-temporal characteristic). Anchored by
jutterswet-borgdeel: the v0-code-verified 12/18 example (fourteen
segments, feb = 4,50), empty edges beside a real fold, the
base-coverage boundary (nov 6,00 from 15 of 30 days), and case4
pinning true-day ACCUMULATION (6+4 days in one bucket → 10,00) plus
the leap-year JAAR bucket (36,60 × 10/366 = 1,00). The verify round
caught and fixed before merge: a non-temporal gedurende characteristic
was verifier-clean while ref faulted and clingo silently emptied — now
rejected by WF-056, faulted by clingo, pinned by the fourth negative;
the None-vs-empty checker split; and case2's noop-satisfiability
(fixed by pinning a real fold beside the empty edges). Deliberately
left, loud: the rate-restated base and anchored buckets (RS ch. 3.7
delta note; CONSTRUCTS row); comparison-shaped gedurende conditions
and parameter bases (verifier-admissible, runtime-faulted, no corpus
case); unpinned but lens-verified agreeing cells (multi-instance
shared buckets, voorwaarde gating, mixed-decimal scaling). The pinned
emitter types this node with the WF-034 unknown-type sentinel — the
emitter-emitted shape itself replayed exactly in both executors
(bakenmeester, /tmp evidence) but cannot enter the corpus until a
pin-bump card teaches the emitter the type; recorded in CONSTRUCTS as
the next conformance delta. Corpus: 77 running cases, 79 negatiefs,
6 red; ref ≡ clingo everywhere.
