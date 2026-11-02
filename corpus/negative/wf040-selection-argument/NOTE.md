# wf040-selection-argument

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3), the selection list: a dimension
fold takes one argument, the reference of a dimensioned attribute. Here the
argument of the sum `jaargewicht` is the literal `1.00`.

The document is `corpus/golden/jutterswet-seizoenvouw/ir.json` with the
first rule's argument replaced.

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on the same mutant in its wire; in
this wire it cannot load it. `expect.txt` pins the arm
`WF-040.selection-argument`.
