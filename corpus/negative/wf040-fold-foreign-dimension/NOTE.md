# wf040-fold-foreign-dimension

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3), the selection list: selection i
of a dimension fold is over dimension i of its argument. Here the sum
`jaargewicht` selects `noord`, `zuid` of the declared dimension `strand`,
while its argument `vondstgewicht` carries the dimension `seizoen` alone.

The document is `corpus/golden/jutterswet-seizoenvouw/ir.json` with a
dimension `strand` declared and the first rule's selection moved onto it
(mutant e of the dimension research).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on the same mutant in the wire of
that verifier (`dimensionSelection`, one node); in this wire it cannot load
it (`unknown field 'dimensionSelections'`). `expect.txt` pins the arm
`WF-040.selection-dimension-position`.
