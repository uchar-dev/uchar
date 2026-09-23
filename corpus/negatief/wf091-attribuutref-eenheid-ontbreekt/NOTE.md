Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, unit facet, one-sided: the declaration HAS a unit and the reference
has none. `Jutter.gewicht` is declared in `kg`; the reference that reads it
is typed as a bare number. The target `kopie` is declared unitless and the
value's type is unitless too, so WF-023's unit clause is satisfied.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: attribute
    reference 'gewicht' is typed no unit, but the declaration it reads
    from is 'kg' of system 'Gewicht' (factor 1 to 'kg')

Why it has to exist beside the convertible case: one-sidedness is its own
arm of the facet, as it is for WF-089 and WF-090's domains, and it is the
half a weakening that only compares two PRESENT units would let through.
A dropped unit is what turns a quantity into a bare number downstream —
the annotation stops saying which quantity was read.

Falsifiable, facet-exact and arm-exact: make the unit facet compare only
the case where both sides carry a unit and this case degrades to `residual
structure mismatch` together with `wf091-attribuutref-eenheid-erbij` —
still WF-091, still red, because the residual facet is the backstop that
sees every difference — while the convertible case keeps its own `unit
mismatch`. What the one-sided arms pin is the DIAGNOSIS, not the refusal.
Disable the `Attribuut_ref` arm and it turns clean together with the other
`wf091-attribuutref-*` cases, while the binder, parameter, fold-source and
write-target cases stay red.
