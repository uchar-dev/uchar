Violates WF-003 (`spec/IR-SPEC.md` ch. 2.1): the attribute `aantal
aangemelde vondsten` of `Jutter` is declared as an enumeration of the domain
`Bedragen`, and no domain of that name is declared. An unfindable domain is
a model-layer reference that resolves against nothing.

    WF-003 FAIL /model/objectTypes/0/attributes/0/datatype: enumeration
    references undeclared domain 'Bedragen'

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the
attribute's datatype was the v0 unknown-type node with the reason "domein
Bedragen niet gevonden": the producer's record that it could not resolve the
domain. L2 took that node out of the datatype union (B16), so an unfindable
domain is spelled as what it is — a declaration naming an undeclared domain
(WF-003 since the L2 landing) — and the migration refuses the old node, having
no L2 spelling for it. Written by hand: the declaration names `Bedragen`, and
the rule's condition compares the attribute with a literal of the same domain
for equality, so no ordering or family clause of WF-036 reports beside the one
defect. `expect.txt` pins the arm.
