# wf091-som-datumparameters

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): a
listed `sum` of two parameters declared `Date`. The arm reads every operand
of a `sum`, the listed ones included, and names a parameter operand by the
parameter's name; it reports once per operand.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the sum of the
    parameter 'peildatum' folds Date values: a total, sum or pro-rata part is
    arithmetic over a Number, and Date values are calendar positions
    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/1: the sum of the
    parameter 'vaststellingsdatum' folds Date values: a total, sum or
    pro-rata part is arithmetic over a Number, and Date values are calendar
    positions

**The document.** One object type, `Jutter`, whose `datumsom` (a `Date`) is
the sum of the parameters `peildatum` and `vaststellingsdatum`, both `Date`
with day precision. No fact type, no set source.

Measured on this document (2026-09-25), with the parameters 2025-01-01 and
2025-12-01: `ref` faults on the addition (`plus: expected a number, got
2025-01-01`), the ASP translation refuses the listed sum as a form it does
not translate, and both Drools branches exclude the rule (the exec branch
"aggregatie buiten Phase-C scope", the DRL-text branch `TODO aggregatie
SOM`). No executor answered.

Red first: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `7816a181b61c7e337e9f2b045877f7b6`) the two rejections above.
Mutation, arm-exact: with the parameter clause removed from the arm's
operand naming, the document is still refused, now as `its operand 0` and
`its operand 1`, and gate 3 fails on this directory alone through
`expect.txt`, which pins the parameter's name.
