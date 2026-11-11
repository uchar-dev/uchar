Violates WF-036 (`spec/IR-SPEC.md` ch. 4.6): the exact-digit test reads the
characters of a text, and its operand here is a number — the attribute
`registratienummer` of golden `jutterswet-registratienummer`, declared
`Number` (whole, non-negative) instead of `Text`.

    WF-036.exact-digits-operand FAIL /rules/rules/0/versions/0/condition:
    exactDigits operand is number, must be text

Derived by hand from the golden, 2026-11-11: the datatype changed, the second
rule and its characteristic left out. `expect.txt` pins the arm.
