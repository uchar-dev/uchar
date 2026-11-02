# wf040-cell-labels-reversed

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3), the positional cell: label i of
a cell is a label of the attribute's dimension i. Here `vondstgewicht`
carries the dimensions `seizoen` and `strand`, and the cell read names
`zuid`, `voorjaar` — both labels declared, in the reversed order.

The document is `corpus/golden/jutterswet-seizoenvondst/ir.json` with a
second dimension `strand` (labels `noord`, `zuid`) and the cell's labels
reversed (mutant d of the dimension research).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS`: it counted per dimension how many
labels of the cell belong to it, in any order. `expect.txt` pins the arm
`WF-040.misplaced-label` at position 1; the verifier reports position 2 as
well.
