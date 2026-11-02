# wf040-fold-undimensioned-argument

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3), the selection list: the argument
of a dimension fold is the reference of a dimensioned attribute. Here the sum
`jaargewicht` folds `groeigewicht`, which carries no dimension.

The document is `corpus/golden/jutterswet-seizoenvouw/ir.json` with the
first rule's argument changed (mutant g of the dimension research).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on the same mutant in its wire; in
this wire it cannot load it. `expect.txt` pins the arm
`WF-040.selection-undimensioned-argument`.
