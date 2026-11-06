# wf125-member-without-labels

Violates WF-125 (`spec/IR-SPEC.md` ch. 2.7), arm (b), at its second write
position: an object creation's member that names a dimensioned attribute
carries `labels`, the one cell it assigns (IR-SPEC ch. 3.3). Here the
member `vangstgewicht` of the created `Vangst` is dimensioned and the
`MemberAssignment` carries no labels.

The document is `corpus/golden/jutterswet-seizoenkopie/ir.json` with the
`labels` of its one member removed.

Red first: the verifier of main `b906f78ba` cannot load this document (`LOAD ERROR … the target of a
rule version is an AttributeReference`: its two rules write a cell). Without
those two rules it reports `verifier: PASS`. `expect.txt` pins the arm
`WF-125.whole-write` with the member's attribute and type.
