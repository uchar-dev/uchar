Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6), the role arm of the
`RoleMembership` node (B9): the condition "hij is een wachtjutter" names
the fact type `bestaatniet`, which the model does not declare. The node's
reference is the pair of the fact type and the role, and here its first half
resolves against nothing.

    WF-034 FAIL /rules/rules/0/versions/0/condition/factType: role
    check names fact type 'bestaatniet', which is not declared: the check
    resolves against no declaration

**Successor at L2 (2026-09-24), stamp: vervangen, named after the rule it now
pins.** This directory replaces `wf034-rolcheck-onbekend-andertefiller`, whose
defect was the near-side role check's filler type resolving against no fact
type (WF-034 (o)). L2 merged the two role-check nodes into
`RoleMembership {factType, role, negated}` (B9), which names its fact type
instead of deriving it from a filler type; the successor carries the same
undeclared name in that position. The document was written by hand: the
migration refuses the original, because there is no fact type to derive.
`expect.txt` pins the arm.
