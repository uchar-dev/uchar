# wf125-whole-target

Violates WF-125 (`spec/IR-SPEC.md` ch. 2.7), arm (b): a write position names
one cell of a dimensioned attribute. Here a rule version's `target` is the
bare `AttributeReference` of the dimensioned `vondstgewicht`, with the scalar
`voorjaarsgewicht` as its value.

The document is `corpus/golden/jutterswet-seizoenvondst/ir.json` with target
and value of its rule changed (mutant b of the dimension research). With the
rule's own cell `voorjaar` as the value the document was, under the
dependency graph per attribute, a WF-045 cycle (mutant j); with a scalar
value no other rule fires.

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on this document. `expect.txt`
pins the arm `WF-125.whole-write`.
