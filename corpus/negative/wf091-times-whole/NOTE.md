Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Binary`/`times` arm, wholeness facet: `aantal maal aantal`, two whole
operands, annotated whole with 0 decimals. `RUNTIME-SEMANTICS.md` ch. 2.3
makes a product fractional whatever its operands are, so the node's `whole`
claim is a lie. The target `uitaantal` is whole as well, so WF-023 finds
nothing to refuse: the annotation is the only place the lie shows.

    WF-091 FAIL /rules/rules/0/versions/0/value/type: wholeness mismatch:
    the times composite is typed whole, but the type derived from its
    operands is fractional

Why it has to exist. ch. 2.3 stated the wholeness and no artifact tested it:
until 2026-09-25 WF-091 left the facet unclaimed at this node, and this
document was verifier-clean (measured). By then no `times`, `dividedBy` or
`power` in golden, red, negative or the parity zone was annotated whole (0
of 131 nodes), and not one report of the 793 documents in those zones moved
when the claim landed.

Falsifiable: put `wholeness` back in the skip list of the `times` arm and
this case turns clean, while its unit arm (`wf091-times-unit`),
`wf091-division-whole` and `wf091-power-whole` stay red (measured on a copy
of the verifier).
