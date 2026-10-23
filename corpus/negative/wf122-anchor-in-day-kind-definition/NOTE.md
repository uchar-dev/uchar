# wf122-anchor-in-day-kind-definition

Violates WF-122 (a) (`spec/IR-SPEC.md` ch. 4.1a and 4.6): an anchored
characteristic check stands in the condition of a day kind's definition, a
form without a subject: there is no form subject for the anchor to read.

    WF-122.anchor-outside-context FAIL
    /rules/dayKindDefinitions/0/condition/parts/2/anchor: the anchor "form" of
    this CharacteristicCheck stands in a form without a subject, which is no
    per-element context the anchor stands in: it reads the subject of the form
    where the filter of an instance fold, or the filter or the test of a
    quantifier fold, has moved the subject to the element (IR-SPEC ch. 4.1a)

The document is `corpus/golden/jutterswet-schouwrooster` with a third part in
the condition of its first day-kind definition (`schouwdag`): `de
Strandmeester zelf is schouwvrij`, an anchored check of the Strandmeester's
characteristic `schouwvrij`.

The verifier reports beside it: `WF-031.condition-reads-state` (a day-kind
condition reads no object state, anchored or not).

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. It is the one value of `position` of WF-122 that had no witness
until fix round A of the card `subselection-reads-subject` (2026-10-23).
