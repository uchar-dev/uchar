# wf040-member-undimensioned-cell

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3) at an object creation's member:
a member with `labels` names a cell of a dimensioned attribute. Here the
member `grondslag` of the created `Strandrecht` carries the label `najaar`,
and `grondslag` carries no dimension.

The document is `corpus/golden/jutterswet-wrakboeking/ir.json` with
`labels` added to its one member.

Red first: the verifier of main `b906f78ba` cannot load this document (`unknown field 'labels'`).
`expect.txt` pins the arm `WF-040.undimensioned-cell`.
