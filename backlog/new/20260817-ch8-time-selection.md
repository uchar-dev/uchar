# Ch. 8 — time selection and the Parameterset data plane

Goal: write `spec/RUNTIME-SEMANTICS.md` ch. 8 as normative draft and
land its executable half. Content: the single time axis (PLAN §9,
decision 2 — valuation = calculation = vintage selection; name the axis
and the extension point for a knowledge-time axis), how the evaluation
date gates rules (version validity, the ch. 3.10 period condition)
versus how it indexes (never implicitly — indexing is the `TemporalRead`
node, ch. 4.3a), and the **vintage read**: the decided row-11 reading of
a granular parameter is the same node with the evaluation date as index
(the `Constante`/ContextValue evaluation-date operand, which `ref/`
currently faults on — this card implements it). The data-plane contract:
a Parameterset (the case's parameter timelines) MUST cover the session's
evaluation window without gap or overlap for every granular parameter a
document reads at the evaluation date — a violation is a
**session-start error**, raised before any rule fires (the ch. 5.7
fail-fast division of labour), never a leeg mid-evaluation. `ref/`
implements the session-start check; `backends/clingo` mirrors it at
translation time (the convention-guard precedent of the temporal-read
card). Witness: an anchor case reading a granular parameter at the
evaluation date (asOfDate set), with the coverage check demonstrably
reachable (a deliberately uncovered variant documented in the card's
verification, since a session-start error cannot sit in the golden set).

Acceptance: ch. 8 normative draft with worked examples; the
evaluation-date operand evaluates in ref and clingo; gates 1–6 green
with the new anchor case replayed by gates 5 and 6; the session-start
coverage check shown red on an uncovered variant (outside the corpus)
and green on the anchor.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md` (the
ContextValue row, if its wire shape needs the extension discipline),
`spec/CONSTRUCTS.md` (rows 11/25), `spec/schema/**`, `spec/verifier/**`,
`corpus/golden/anchor/**`, `corpus/negatief/**`, `ref/**`,
`backends/clingo/**`, `backlog/**`.

Out of scope: the second (knowledge-time) axis beyond naming the
extension point; multi-period grids (`count` ≥ 2, §3.12 OPEN);
granularity conversion; the emitter (pin-bump).
