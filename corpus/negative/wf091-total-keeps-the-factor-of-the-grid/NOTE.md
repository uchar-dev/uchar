Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at the `total` arm, unit facet: a rate with two negative factors, `EUR/m/mnd`,
totalled over its grid of months. `RUNTIME-SEMANTICS.md` ch. 2.6, rule 8 raises the factor of the grid, so the fold is
`EUR/m`, but the node repeats the rate. WF-023 fires beside it, because the annotation does not fit the target `EUR/m`; the
report of WF-091 is what this witness pins. The verifier reads the grid from the granularity of the attribute the operand
reads, and without that it could claim nothing for two negative factors.

Falsifiable: write `EUR/m` as the annotation, and the document is `jutterswet-kadetarief` with one rule, verifier-clean.
