# wf091-sum-date-literals

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): a
listed `sum` of two date literals. An operand that is neither an attribute
nor a parameter reference is named by its position; the arm reports once
per operand.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the sum of its
    operand 0 folds Date values: a total, sum or pro-rata part is arithmetic
    over a Number, and Date values are calendar positions
    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/1: the sum of its
    operand 1 folds Date values: a total, sum or pro-rata part is arithmetic
    over a Number, and Date values are calendar positions

**The document.** `wf091-sum-date-parameters` with the two parameter
references replaced by the literals 2025-01-01 and 2025-12-01, the parameter
declarations removed, and the source snippets of the operands and the fold
following the literals; nothing else differs.

Measured on this document (2026-09-25): `ref` faults on the addition (`plus:
expected a number, got 2025-01-01`), the ASP translation refuses the listed
sum as a form it does not translate, and both Drools branches exclude the
rule (the exec branch "aggregatie buiten Phase-C scope", the DRL-text branch
`TODO aggregatie SOM`). No executor answered.

Red first: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `7816a181b61c7e337e9f2b045877f7b6`) the two rejections above.
Mutation, arm-exact: with the positional name changed from `its operand <i>`
to `operand <i>`, the document is still refused and gate 3 fails on this
directory alone through `expect.txt`.
