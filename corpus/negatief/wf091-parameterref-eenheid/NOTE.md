Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `ParameterReference`
arm, unit facet: the parameter `vondstdrempel` is declared in `kg` and the
reference that reads it is typed in `t` (same canonical `kg`, factor 1000).

    WF-091 FAIL /resolvedRegels/0/waarde/type: unit mismatch: parameter
    reference 'vondstdrempel' is typed 't' of system 'Gewicht' (factor
    1000 to 'kg'), but the declaration it reads from is 'kg' of system
    'Gewicht' (factor 1 to 'kg')

Why it has to exist: the parameter arm has no positive golden witness (no
golden, rood or parity document carries a unit-bearing
`ParameterReference`), so the negatief is the only thing that holds the arm
in place — the same honesty note WF-089 and WF-090 carry for this arm.

Falsifiable, arm-exact: disable the `Parameter_ref` arm's
`use_site_identity` call and this case turns clean together with
`wf089-parameterref-enum` and `wf090-parameterref`, while the attribute,
binder, fold-source and write-target cases stay red.
