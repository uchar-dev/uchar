Violates WF-039 (`spec/IR-SPEC.md` ch. 4.6, invoked from WF-023): the rule
`toeslag van een drijfhout` writes the literal `-0` into `Drijfhout.toeslag`,
declared `negative` here. The value is zero, and zero is not negative,
whatever the lexeme's leading minus. Cut from
`corpus/negatief/wf039-literal-onder-doelteken` (the declaration's sign
`nonNegative` → `negative`, the literal `-5` → `-0`).

    WF-039 FAIL /rules/rules/2/versions/0/value/value: literal '-0' is
    written into a target under a negative sign restriction

The clause reads the sign of the VALUE the lexeme spells (`lexeme_negative`
and `lexeme_zero` in `spec/verifier/lib/checks.ml`). Rood-eerst: until
2026-09-25 it read the lexeme's first character, took `-0` for a negative
value and let the document pass (`verifier: PASS`). The golden
`jutterswet-nulspelling` pins the other side: `-0` written into a
`nonNegative` target is clean. `expect.txt` pins the arm.
