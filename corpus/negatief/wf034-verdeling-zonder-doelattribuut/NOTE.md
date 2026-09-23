Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2)
that names no target attribute. `doelAttribuut` is where every recipient's
share is written; with the field empty the write resolves against no
declaration.

    WF-034 FAIL /rules/verdelingen/0/doelAttribuut: allocation without a target
    attribute: it resolves against no declaration

It exists separately from its `bronAttribuut` sibling for the reason the
fold-source pair established one wave earlier: each half of a resolving pair
needs its own witness, because a clause that closes one half says nothing
about the other. Here both halves live in the same arm, and the sibling
negatief plus this one are what keep that arm from being half a rule.

The consequence differs from the source half's, which is why it is worth its
own document. An allocation is not an expression, so `walk_expr` never
reaches it; the target attribute is additionally the allocation's **write**
position, so the empty field also removes the allocation from the dependency
graph's write side — WF-026's duplicate-target comparison, WF-050's overlap
with a defaults group, and the stratification edge all key on
`(doelType, doelAttribuut)` and none of them can see an allocation that
declines to say what it writes. Measured: **11 of 11** golden allocations
stayed verifier-clean with this field emptied, and 0 do after this clause.

Measured before the closure, on this document against the two cases beside
`corpus/golden/jutterswet-gelijkdeling/`:

    ref    : EVALUATION FAULT: missing doelAttribuut in IR node
    clingo : TRANSLATION FAULT: missing doelAttribuut

*Measured dracht, stated exactly because it is not zero:* the 153 golden, 22
rood and 30 parity documents carry no allocation with an empty
`doelAttribuut`; **three** negatief cases that predate this card do —
`wf020-crossform-collision`, `wf024-complex` and `wf025-max-without-ratio`,
each a minimal hand-written document that spells only the fields its own rule
needs. They were already refused, they stay refused, they still cite the rule
their directory name declares, and the added line is true of them. No
document in any zone changes verdict.

*What this arm does NOT close* is recorded in the sibling NOTE
(`wf034-verdeling-zonder-bronattribuut`): a name that resolves against no
declaration, and the two `bronType`/`doelType` fields, are equally emptyable
and equally clean, and stay **open**.

A hand-broken variant of `corpus/golden/jutterswet-gelijkdeling/ir.json` —
one field emptied, a one-field diff; schema-valid, since
`ResolvedVerdeling.doelAttribuut` is `["string","null"]` on the v0 wire.

Falsifiable, clause-exact: neuter `wf034_verdelingen` and this case and its
`bronAttribuut` sibling turn clean, the four pre-existing negatief cases
named above and in the sibling NOTE lose an incidental WF-034 line while
staying refused for their own rule, and nothing else over the four zones
moves.
