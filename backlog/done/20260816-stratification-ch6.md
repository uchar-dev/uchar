# RUNTIME-SEMANTICS ch. 6: stratification as well-formedness

Goal: write ch. 6 normatively — the attribute/characteristic dependency
graph, acyclicity as a well-formedness condition (the delta ch. 5.2
already records as a candidate WF rule, with the deliberate exception of
creation recursion, ch. 5.5), and negation strata generalising v0's
single boundary (ch. 5.3). Assign the new WF number(s) in
`spec/IR-SPEC.md`, implement the check in `spec/verifier/` (cycle
detection over rule reads/writes; negated reads must sit in a strictly
higher stratum), add at least one passing golden witness and one
negatief case per new rule, and replace `ref/`'s hard-coded staging
(positives → negation consumers → checks → channel readers) by strata
computed from the same graph.

Acceptance: gate 3 green with the new WF check(s) and negatief case(s);
gate 5 green with `ref/` evaluating by computed strata; every new MUST
carries a WF ID with verifier check, golden, and negatief case (spec
discipline).

May touch: `spec/RUNTIME-SEMANTICS.md` (ch. 6), `spec/IR-SPEC.md` (WF
index), `spec/verifier/**`, `corpus/negatief/**`,
`corpus/golden/README.md`, `spec/CONSTRUCTS.md` (row 6 delivery note),
`ref/lib/eval.ml`, `ref/README.md` (staging → strata paragraph),
`ref/test/test_ref.ml` (the ch. 6.4 worked examples as unit tests, per
this card's acceptance), `backlog/**`. Protected paths throughout —
merge under the standing mandate only after green gates plus
adversarial verification.

Out of scope: the ASP-side stratification story (ASP-TRANSLATION.md
already sketches it); ch. 7 defaults.

Result: delivered. RUNTIME-SEMANTICS ch. 6 is normative: the dependency
graph over attribute/characteristic/channel/population nodes with a
three-kind edge taxonomy (positive; stratum-strict for
non-characteristic reads by irrevocable writers, discharged inside
admissible creation cycles; negative — forbidden on any cycle), WF-045
acyclicity with the creation-recursion exception, WF-046 stratified
negation, worked examples. IR-SPEC v0.4 carries the WF bullets and
index rows; the WF-046 golden cell is an honest OPEN (no golden bears a
negative edge; needs a re-emitted negation golden — ref/ unit tests
cover the accept path). Verifier: dep_forms + wf045_046 (cycle
detection minus recursive-creation members; negative-edge-on-a-cycle
via reachability); two negatief cases shown red on main's verifier and
rejected on the branch citing their rule; gate 3 at 40 negatief.
ref/: evaluation by strata computed from the shared dep_forms (no
drift); ch. 6.4 examples land as unit tests verbatim (WV-6.4-a..e:
the 5.3 pair, the 5.6 channel read, the cycle fault, the four-deep
ladder, order-independence of an emptiness read on a derived
attribute). Verification (3 adversarial opus lenses) found 2 blockers +
1 major (XOR double-negation cancellation; positive classification of
non-monotone emptiness/value reads by irrevocable writers — a
demonstrated verifier-clean order-dependence; missing strip_version in
recursive membership) — all fixed and locked with tests. Deliberately
left: a negation-bearing golden (needs re-emission at the pin, tracked
in the WF-046 index cell); the condition-retraction and
transient-deposit residuals (recorded in ch. 6.1, ch. 7 territory);
relation-level dependency nodes (recorded narrowing).
