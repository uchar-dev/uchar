Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at the `times` arm, unit facet: `lengte maal breedte`, two lengths in `m`, whose
annotation says `m^3`. Rule 2 of `RUNTIME-SEMANTICS.md` ch. 2.6 adds the exponents, so the type derived from the operands is
`m^2`. WF-023 fires beside it, because `m^3` does not fit the target in `m^2`; the report of WF-091 is what this witness pins.

Falsifiable: write exponent 2 in the annotation, and the document is the rule `oppervlakte` of `jutterswet-oppervlak`,
verifier-clean.
