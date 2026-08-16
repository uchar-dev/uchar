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
