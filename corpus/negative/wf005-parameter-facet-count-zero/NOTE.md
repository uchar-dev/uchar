# wf005-parameter-facet-count-zero

Violates WF-005 (`spec/IR-SPEC.md` ch. 2.2) on the timeline facet of a
parameter, which WF-018 (ch. 2.10) defers to this rule: a period count 0.

    WF-005 FAIL /model/parameters/0/timeline/count: the timeline facet of
    parameter 'penningbedrag' has count 0 < 1

Since L3 a time-dependent parameter carries the same `TimelineFacet` node as an
attribute, without a timeline name (delta B18 of
`spec/proposals/V1-WIREMIGRATIE.md`, CONSTRUCTS row 11). Until then it carried
a bare granularity and no count, so this clause had nothing to read on a
parameter. The count is the facet's own field, so its rule is the facet's.

The document is `corpus/golden/anchor/jutterswet-penningvintage` with the count of the
facet of `penningbedrag` set to 0. `expect.txt` pins the arm.
