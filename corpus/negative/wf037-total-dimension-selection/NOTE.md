# wf037-total-dimension-selection

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in its
`dimensionSelections` clause: a dimension fold is a `sum`, a `minimum`, a
`maximum`, a `first`, a `last` or a `filledCount`. Here a `total` carries
the selection.

    WF-037.dimension-selection-kind FAIL /rules/rules/0/versions/0/value:
    dimension selection on a kind other than sum, minimum, maximum, first,
    last or filledCount

A total integrates a timeline over time (`RUNTIME-SEMANTICS.md` ch. 3.6),
and a cell carries no timeline (WF-125 (d)), so there is nothing over cells
for it to integrate. A count counts instances, of which a cell is none; the
number of filled cells is the `filledCount` over a selection.

The document is `corpus/golden/jutterswet-seizoenvouw/ir.json` with the kind
of its first rule's fold, the sum `jaargewicht` over all seasons, changed to
`total`. Nothing else fires: the argument is the dimensioned attribute
WF-040 asks for, and a total without a range reads the subject's own
attribute.

`expect.txt` pins the arm.
