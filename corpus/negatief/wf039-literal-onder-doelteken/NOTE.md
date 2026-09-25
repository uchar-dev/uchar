Violates WF-039 (`spec/IR-SPEC.md` ch. 4.6, invoked from WF-023): a numeric
literal written into a declaration MUST satisfy the sign restriction the
declaration carries. The rule `toeslag van een drijfhout` writes the literal
`-5` into `Drijfhout.toeslag`, declared `nonNegative`; the literal's own type
carries no sign restriction (`unspecified`). Broken from
`corpus/golden/jutterswet-zustervondst` by that one value (`3` → `-5`).

    WF-039 FAIL /rules/rules/2/versions/0/value/value: negative literal '-5'
    is written into a target under a non-negative sign restriction

Rood-eerst: until 2026-09-25 the verifier let the document pass
(`verifier: PASS`). The clause read a literal's sign only against its own
annotation — the negatief `wf039-negative-under-nonneg` — and WF-023's sign
clause compares two declared restrictions, of which the literal's
(`unspecified`) contradicts none. `ref` and the ASP backend do not refuse the
write either: with the golden's case both write the value (`MISMATCH:
D1.toeslag = -5, expected 3`; clingo `[-5]`), so there is no executor
sentence to share. The target clause stands aside where the literal's own
restriction already refuses the value, so one defect gives one line.
`expect.txt` pins the arm.
