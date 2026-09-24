# wf034-sentinel-gelest-wratoperand

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a LISTED fold operand names
`vondsten` — the declared plural of the role `vondst` — as an attribute of
`Jutter`. A listed value fold (§5.8.3) has no source; its operands are
ordinary reads, and a role name read as an attribute resolves to nothing.

The document. A `MAXIMUM` with two arguments and a vestigial
`elementType`: operand 0 the subject's `drempelgewicht`, operand 1
`vondsten` on `Jutter`. The name is a count source only as the single
argument of an aggregation (WF-093); here it is the second of two operands, and
nothing reads a relation from it.

    WF-034 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/1: 'vondsten'
    is neither an attribute nor a characteristic of 'Jutter'

**Migrated at L2 (2026-09-24), stamp: herschreven (m), armwissel.** Until L2
the operand carried the unknown-type sentinel on its own `type` — v0's
spelling of a count source — at a position that is no fold source, and
WF-034 refused the sentinel there. Measured on the executors then: `ref`
faulted (`unsupported AttribuutRef shape`), the ASP backend refused the
expression kind, and the Drools backend threw looking for a getter of the
role name on the subject bean. The sentinel left the wire with the
reference's own annotation (B13); the migration's translation
(`spec/proposals/v1-migratie/m2.py`) reads the role name as an ordinary
attribute of `Jutter`, and WF-034 refuses it in its name-resolution arm. The
witness now guards the edge of the count-source recognition: outside the
source position a role name is a name like any other. `expect.txt` pins the
new arm.
