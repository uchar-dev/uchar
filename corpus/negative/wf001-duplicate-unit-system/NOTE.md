# wf001-duplicate-unit-system

Violates WF-001 (`spec/IR-SPEC.md` ch. 2.1) in the unit-system namespace: two
unit systems whose names differ in case alone.

    WF-001 FAIL /model/unitSystems: namespace 'unitSystems': names 'Gewicht' and
    'gewicht' collide case-insensitively

Since L3 a unit system is a `UnitSystem` node with a list of units (card
`20260924-l3-typesysteem`), and the message names the namespace by the wire
list `unitSystems`; until the L3 fix round it named the v0 map
`eenheidstelsels`.

The document is `corpus/golden/jutterswet-vangstklem` with a second unit
system `gewicht`, declaring one self-canonical unit `g`. `expect.txt` pins the
arm.
