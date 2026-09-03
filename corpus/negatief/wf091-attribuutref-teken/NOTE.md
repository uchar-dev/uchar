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

Falsifiable: drop the sign facet from the engine and this case alone turns
clean.
