# wf115-vast-startpunt-bij-variabel

Violates WF-115 arm (a) (`spec/IR-SPEC.md` ch. 2.8): a `Timeline` with
`variableStart: true` that carries a `fixedStart`.

    WF-115 FAIL /model/timelines/0/fixedStart: timeline 'het stormjaar' has a
    variable start and a fixedStart '--10-01': its periods run from the anchor
    its start-point determination binds per object, and a timeline has one start

A variable start runs the periods from the anchor a start-point determination
binds per object (ch. 3.8); a fixed start point would lay a second grid for the
same timeline. The document
anchors the timeline properly, so no other rule fires: WF-016 and WF-032 find
the determination, on a day-precision member.

The document is `corpus/golden/jutterswet-vast-startpunt` with three edits:
`het stormjaar` set to `variableStart: true` (its `fixedStart` kept), a
day-precision attribute `aanmonsterdatum` added to `Jutter`, and a
`StartPointDetermination` of `stormjaar` for `Jutter` by that member. The two
facets that name the timeline keep `--10-01`, so arm (c) finds them equal to
the declaration. `expect.txt` pins the arm.

Red first (measured 2026-09-26 before the arm existed, with the loader already
reading the field): `verifier: PASS`. Mutation test: with arm (a) removed
from the verifier this witness passes and the two other WF-115 witnesses
still fail on their own arms.
