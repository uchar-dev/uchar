Violates WF-023 (`spec/IR-SPEC.md`, the sign clause of assignment typing):
the rule `toeslag van een drijfhout` writes the literal `-5`, typed
`negative`, into `Drijfhout.toeslag`, declared `nonNegative`; the value's
sign restriction contradicts the target's. Cut from
`corpus/negative/wf039-literal-under-target-sign`, whose literal carries no
restriction of its own (`unspecified`), by that one field.

    WF-023 FAIL /rules/rules/2/versions/0: the value's sign restriction
    contradicts the target's

One defect, one report: WF-039's target clause (IR-SPEC ch. 4.6) stands
aside where the literal's own restriction contradicts the target's, because
this clause reports it. Until 2026-09-25 the verifier reported the defect
twice, measured:

    WF-039 FAIL /rules/rules/2/versions/0/value/value: negative literal '-5'
    is written into a target under a non-negative sign restriction
    WF-023 FAIL /rules/rules/2/versions/0: the value's sign restriction
    contradicts the target's

It is also the first negative that reaches WF-023's sign clause at all.
`expect.txt` pins the arm.
