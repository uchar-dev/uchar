Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Binary`/`power` arm, wholeness facet: `aantal tot de macht aantal`, two
whole operands, annotated whole with 0 decimals. `RUNTIME-SEMANTICS.md` ch.
2.3 makes a power fractional whatever its operands are, so the node's
`whole` claim is a lie. The target `uitaantal` is whole as well, so WF-023
finds nothing to refuse: the annotation is the only place the lie shows.

    WF-091 FAIL /rules/rules/0/versions/0/value/type: wholeness mismatch:
    the power composite is typed whole, but the type a power derives is
    fractional

Why it has to exist. ch. 2.3 stated the wholeness and no artifact tested it:
until 2026-09-25 WF-091 left the facet unclaimed at this node, and this
document was verifier-clean (measured). By then no `times`, `dividedBy` or
`power` in golden, red, negative or the parity zone was annotated whole (0
of 131 nodes), and not one report of the 793 documents in those zones moved
when the claim landed.

Falsifiable: remove the `power` half of the quotient-and-power arm and this
case turns clean, while `wf091-division-whole` and `wf091-times-whole` stay
red (measured on a copy of the verifier).
