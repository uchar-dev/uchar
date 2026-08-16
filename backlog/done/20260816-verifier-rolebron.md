# Verifier: accept the v0 role-source aggregation encoding

Goal: the first quantitative golden exposed a verifier false positive:
a v0 instance-fold count encodes its source as a pseudo-AttribuutRef
naming a *role* (plural) with the unknown-type sentinel — legitimate v0
(the row-1 overloaded-Functie wart), rejected by WF-034's sentinel and
member-resolution clauses. Except exactly this position: an aggregation
argument, with `elementObjecttype` present, whose reference resolves to
a role name or plural of a declared fact type; record the exception in
the WF-034 index cell.

Acceptance: the vondsttelling dump (aggregation over a role) passes;
all existing golden and negatief cases keep their gate-3 verdicts;
gates 1–3 green.

May touch: `spec/verifier/lib/checks.ml` (protected — merge mandate),
`spec/IR-SPEC.md` (protected; WF-034 cells only), `backlog/`.

Out of scope: loader; other WF rules; corpus.

Result: aggregation source arguments (elementObjecttype present) now
resolve against declared roles and tolerate the systematic v0 sentinel;
WF-034 index cell records the exception. Witnessed by the vondsttelling
golden (landed by the goldens card); existing corpus verdicts unchanged.
