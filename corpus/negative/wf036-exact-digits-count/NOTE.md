Violates WF-036 (`spec/IR-SPEC.md` ch. 4.6): the exact-digit test counts at
least one digit (`RUNTIME-SEMANTICS.md` ch. 1.2), and this one counts zero.
The document is golden `jutterswet-registratienummer` with `digits` 0 on the
test "zijn registratienummer is numeriek met exact … cijfers".

    WF-036.exact-digits-count FAIL /rules/rules/0/versions/0/condition:
    exactDigits counts 0 digits, must be at least 1

Derived by hand from the golden, 2026-11-11: `digits` 9 changed to 0, the
second rule and its characteristic left out. `expect.txt` pins the arm and
its parameter.
