Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, unit facet, the mirror one-sided arm: the declaration has NO unit and
the reference invents one. `Jutter.gewicht` is declared as a bare number;
the reference that reads it is typed in `kg`. The target `kopie` is
declared in `kg` and the value's type is `kg`, so WF-023's unit clause is
satisfied.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: attribute
    reference 'gewicht' is typed 'kg' of system 'Gewicht' (factor 1 to
    'kg'), but the declaration it reads from is no unit

Why it has to exist beside `wf091-attribuutref-eenheid-ontbreekt`: the two
one-sided directions are separate — a rule that only refused a DROPPED
unit would admit this one, and an invented unit is what lets an unqualified
number enter a dimensioned computation as if it had always been a weight.

Falsifiable, facet-exact and arm-exact: make the unit facet fire only when
the DECLARATION carries a unit and this case degrades to `residual
structure mismatch` — still WF-091, still red, because the residual facet
is the backstop that sees every difference — while
`wf091-attribuutref-eenheid-ontbreekt` keeps its own `unit mismatch`. What
the one-sided arm pins is the DIAGNOSIS, not the refusal. Disable the
`Attribuut_ref` arm and it turns clean together with the other
`wf091-attribuutref-*` cases, while the binder, parameter, fold-source and
write-target cases stay red.
