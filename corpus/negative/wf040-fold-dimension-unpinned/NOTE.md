# wf040-fold-dimension-unpinned

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3), the selection list: a dimension
fold carries one selection per dimension of its argument, a dimension it
does not range over selected with one label. Here `vondstgewicht` carries
the dimensions `seizoen` and `strand`, and each of the four folds selects on
`seizoen` alone, so the cells it would read have no label of `strand`.

The document is `corpus/golden/jutterswet-seizoenvouw/ir.json` with a second
dimension on `vondstgewicht` (mutant f of the dimension research).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on the same mutant in its wire; in
this wire it cannot load it. The Drools backend of 2026-11-01 played that
mutant with empty input (`PLAYED 1`). `expect.txt` pins the arm
`WF-040.selection-count`; the verifier reports it once per fold.
