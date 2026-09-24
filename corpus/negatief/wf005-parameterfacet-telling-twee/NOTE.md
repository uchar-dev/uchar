# wf005-parameterfacet-telling-twee

Violates WF-005 (`spec/IR-SPEC.md` ch. 2.2) on the timeline facet of a
parameter, which WF-018 (ch. 2.10) defers to this rule: a period count 2.

    WF-005 FAIL /model/parameters/0/timeline/count: the timeline facet of
    parameter 'penningbedrag' has count 2 > 1: a parameter's timeline is one
    period while multi-period grids are open (RUNTIME-SEMANTICS ch. 3.12)

A parameter's facet has count 1 while `RUNTIME-SEMANTICS.md` ch. 3.12 leaves
multi-period grids open. No executor evaluates a parameter's count: `ref` and
the ASP backend take the granularity alone and replay the host's case on this
document unchanged (measured 2026-09-24: `oracle: PASS (2 expectations)`,
`clingo: PASS (2 expectations)`), and the Drools loader refused a count other
than 1 as a form it does not carry. So two executors treated a two-period
facet as a one-period one and the third refused it, while the verifier passed
it. Since this arm the Drools loader refuses the document in the verifier's
own sentence, at the same pointer. The sibling
`wf005-parameterfacet-telling-nul` pins the count below 1.

The document is `corpus/golden/anchor/jutterswet-penningvintage` with the
count of the facet of `penningbedrag` set to 2. `expect.txt` pins the arm.
