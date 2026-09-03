Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, WHOLENESS facet: `Jutter.gewicht` is declared fractional (`geheel:
false`, zero decimals — one of WF-007's three precision forms) and the
reference that reads it is typed whole. Decimals agree, so the decimals
facet is silent and wholeness carries the refusal alone.

    WF-091 FAIL /resolvedRegels/0/waarde/type: wholeness mismatch:
    attribute reference 'gewicht' is typed whole, but the declaration it
    reads from is fractional

Why it has to exist beside `wf091-attribuutref-decimalen`: WF-007 admits
three precision forms, and two of them share a decimal count — whole with
zero decimals and fractional with zero decimals. A rule that crossed only
the decimal count would call those two identical, which is exactly the
weakening this case refutes.

Falsifiable, facet-exact and arm-exact: neuter the wholeness facet and this
case degrades to `residual structure mismatch` — still WF-091, still red,
because the residual facet is the backstop that sees every difference; what
the facet pins is the DIAGNOSIS, not the refusal, and
`wf091-attribuutref-decimalen` keeps its own `decimals mismatch`. Disable
the `Attribuut_ref` arm and it turns clean together with the other
`wf091-attribuutref-*` cases, while the binder, parameter, fold-source and
write-target cases stay red.
