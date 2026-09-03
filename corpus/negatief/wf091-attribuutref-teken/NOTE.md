Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, SIGN facet: `Jutter.gewicht` is declared `NIET_NEGATIEF` and the
reference that reads it is typed `ONBEPAALD`. Every other facet agrees.

    WF-091 FAIL /resolvedRegels/0/waarde/type: sign mismatch: attribute
    reference 'gewicht' is typed ONBEPAALD, but the declaration it reads
    from is NIET_NEGATIEF

Why it has to exist: WF-023's sign clause refuses a CONTRADICTORY
restriction at a write (negative into non-negative and back), which by
construction says nothing about a widened one — `ONBEPAALD` contradicts
nothing. On the read side the widening is the whole defect: the annotation
drops a guarantee the declaration gives, and a consumer that trusts it
admits a value the declaration excludes.

Falsifiable, facet-exact and arm-exact: neuter the sign facet and this case
degrades to `residual structure mismatch` — still WF-091, still red,
because the residual facet is the backstop that sees every difference; what
the facet pins is the DIAGNOSIS, not the refusal. Disable the
`Attribuut_ref` arm and it turns clean together with the other
`wf091-attribuutref-*` cases, while the binder, parameter, fold-source and
write-target cases stay red.
