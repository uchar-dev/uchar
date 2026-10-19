# wf115-fixed-start-point-negated-adopted

Violates WF-115 arm (c) (`spec/IR-SPEC.md` ch. 2.8): an attribute facet that
names a timeline without carrying that timeline's fixed start point.

    WF-115 FAIL /model/objectTypes/0/attributes/0/timeline/timelineName:
    named-timeline adoption is partial: fixed start point none on the facet,
    '--10-01' on timeline 'stormjaar'

A facet adopts its named timeline whole — granularity and count (WF-005), and
the fixed start point. The executors read a member's grid from its facet, so a
facet without the start point would lay the calendar grid under a timeline
declared to start on 1 October. The pointer is the facet's `timelineName`: the
field the rule compares against is absent here.

The document is `corpus/golden/jutterswet-vast-startpunt` with the
`fixedStart` removed from the facet of `stormwachtdagen`. `expect.txt` pins
the arm.

Red first (measured 2026-09-26 before the arm existed): `verifier: PASS`.
Mutation test: with arm (c) removed from the verifier this witness passes and
the two other WF-115 witnesses still fail on their own arms.
