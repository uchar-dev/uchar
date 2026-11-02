# wf040-selection-duplicate-label

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3), the selection list: a
selection names each of its labels once. Here the selection of the sum
`jaargewicht` is `najaar`, `voorjaar`, `voorjaar`.

The document is `corpus/golden/jutterswet-seizoenvouw/ir.json` with the
first rule's labels changed (mutant k of the dimension research). The order
`najaar` before `voorjaar` is no defect: a selection is read in its own
order (RUNTIME-SEMANTICS ch. 5.8c), and a set written out of the declared
order is a set.

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on the same mutant in its wire; in
this wire it cannot load it. `expect.txt` pins the arm
`WF-040.selection-duplicate-label`.
