Violates WF-039 (`spec/IR-SPEC.md` ch. 4.6, invoked from WF-023): the rule
`toeslag van een drijfhout` writes the literal `0/5` into
`Drijfhout.toeslag`, declared `positive` here. The rational's numerator is
zero, so its value is zero, and zero is not positive. Cut from
`corpus/negative/wf039-literal-under-target-sign` (the declaration's sign
`nonNegative` → `positive`, the literal `-5` → `0/5`).

    WF-039 FAIL /rules/rules/2/versions/0/value/value: literal '0/5' is
    written into a target under a positive sign restriction

The clause reads the sign of the VALUE the lexeme spells (`lexeme_zero` in
`spec/verifier/lib/checks.ml`, over the numerator of a rational). Rood-eerst:
until 2026-09-25 it tested the whole lexeme for zeros, took the `/5` for a
non-zero value and let the document pass (`verifier: PASS`). `expect.txt`
pins the arm.
