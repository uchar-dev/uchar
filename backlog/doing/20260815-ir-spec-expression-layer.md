# IR-SPEC ch. 4: the expression and condition layer (proposal)

Goal: the v0 mirror of the expression and condition layer in
`spec/IR-SPEC.md` per the inventory (`CONSTRUCTS.md` ch. 2.1/2.2): the
Functie split (aggregation-with-source vs builtin call), unified
navigation on `AttribuutRef`, the split `Constante`, merged role-existence
conditions (row 5: no backref, no top-level-only), quantified compounds
(counting quantifiers as sugar), `Onopgelost` with structured reason
(row 16 delta), typing rules referencing RUNTIME-SEMANTICS ch. 1–2 —
including the candidate WF from ch. 2.4 (explicit rounding before a
bounded write) as a real WF rule.

Acceptance: every MUST carries a WF-### ID; WF index extended gaplessly;
deviations only where CONSTRUCTS.md records a delta; no reproduced
specification text; gate passes.

May touch: `spec/IR-SPEC.md` (protected — proposal), `spec/CONSTRUCTS.md`
(delta sync only), `backlog/`.

Out of scope: rule layer (own card); dialect lowering chapter; verifier.
