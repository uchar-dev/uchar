# wf040-selection-empty

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3), the selection list: a selection
names at least one label. Here the selection of the sum `jaargewicht` is
empty.

The document is `corpus/golden/jutterswet-seizoenvouw/ir.json` with the
first rule's labels emptied (mutant l of the dimension research).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on the same mutant in its wire; in
this wire it cannot load it. `expect.txt` pins the arm
`WF-040.selection-empty`.
