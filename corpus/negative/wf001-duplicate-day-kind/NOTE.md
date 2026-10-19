# wf001-duplicate-day-kind

Violates WF-001 (`spec/IR-SPEC.md` ch. 2.1) in the day-kind namespace: two
day kinds whose names differ in case alone. This is row 3 of the collision
table of ch. 2.9 (a name against another day kind's name), which WF-001 covers
and WF-017 does not.

    WF-001 FAIL /model/dayKinds: namespace 'dayKinds': names 'schouwdag' and
    'Schouwdag' collide case-insensitively

Since L3 a day kind is a `DayKind` node with its name and its plural (card
`20260924-l3-typesysteem`), and the message names the namespace by the wire
list `dayKinds`; until the L3 fix round it named the v0 list `dagsoorten`.

The document is `corpus/golden/jutterswet-schouwrooster` with a third day kind
`Schouwdag`, without a plural and without a definition. `expect.txt` pins the
arm.
