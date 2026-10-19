Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2)
that names no target attribute. `targetAttribute` is where every recipient's
share is written; with the field `null` the write resolves against no
declaration.

    WF-034 FAIL /rules/allocations/0/versions/0/targetAttribute: allocation
    without a target attribute: it resolves against no declaration

It exists separately from its `sourceAttribute` sibling for the reason the
fold-source pair established one wave earlier: each half of a resolving pair
needs its own witness, because a clause that closes one half says nothing
about the other. Here both halves live in the same arm, and the sibling
negative plus this one are what keep that arm from being half a rule.

The consequence differs from the source half's, which is why it is worth its
own document. An allocation is not an expression, so `walk_expr` never
reaches it; the target attribute is additionally the allocation's **write**
position, so the empty field also removes the allocation from the dependency
graph's write side — WF-026's duplicate-target comparison, WF-050's overlap
with a defaults group, and the stratification edge all key on
`(targetType, targetAttribute)` and none of them can see an allocation that
declines to say what it writes. Measured: **11 of 11** golden allocations
stayed verifier-clean with this field emptied, and 0 do after this clause.

Measured before the closure, on this document against the two cases beside
`corpus/golden/jutterswet-gelijkdeling/`:

    ref    : EVALUATION FAULT: missing doelAttribuut in IR node
    clingo : TRANSLATION FAULT: missing doelAttribuut

*Measured dracht, stated exactly because it is not zero:* the 153 golden, 22
red and 30 parity documents carry no allocation with an empty
`doelAttribuut`; **three** negative cases that predate this card do —
`wf020-crossform-collision`, `wf024-complex` (removed at L4, where `COMPLEX` became a load error) and `wf025-max-without-ratio`,
each a minimal hand-written document that spells only the fields its own rule
needs. They were already refused, they stay refused, they still cite the rule
their directory name declares, and the added line is true of them. No
document in any zone changes verdict.

*What this arm did NOT close* is recorded in the sibling NOTE
(`wf034-allocation-without-source-attribute`): a name that resolved against no
declaration, and the two `bronType`/`doelType` fields, were equally emptyable
and equally clean at the time. Later arms of WF-034 closed both; the sibling
NOTE names them and their witnesses.

A hand-broken variant of `corpus/golden/jutterswet-gelijkdeling/ir.json` —
one field emptied, a one-field diff; schema-valid, since the version's
`targetAttribute` is `["string","null"]` on the wire.

Falsifiable, clause-exact: neuter `wf034_verdelingen` and this case and its
`sourceAttribute` sibling turn clean, the four pre-existing negative cases
named above and in the sibling NOTE lose an incidental WF-034 line while
staying refused for their own rule, and nothing else over the four zones
moves.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then (`bronType` →
`sourceType`, `doelType` → `targetType`, `doelAttribuut` → `targetAttribute`).
`expect.txt` pins the arm.
