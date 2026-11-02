# wf125-bare-read

Violates WF-125 (`spec/IR-SPEC.md` ch. 2.7), arm (a): a dimensioned attribute
has a value per cell and none of its own, so a reference to it is read only
as the `attribute` of a `DimensionCellRead` or as the argument of a dimension
fold. Here the value of the rule is the bare `AttributeReference` of the
dimensioned `vondstgewicht`.

The document is `corpus/golden/jutterswet-seizoenvondst/ir.json` with the
`DimensionCellRead` of the rule's value replaced by its own `attribute`
(mutant a of the dimension research, 2026-11-02).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on this document. `expect.txt`
pins the arm `WF-125.bare-read`.
