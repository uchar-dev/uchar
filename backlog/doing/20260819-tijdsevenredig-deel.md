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
