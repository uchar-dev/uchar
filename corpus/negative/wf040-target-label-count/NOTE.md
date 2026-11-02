# wf040-target-label-count

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3) at a write position: a cell
target names one label of each dimension of its attribute. Here the target
of `najaarsgewicht van een jutter` is the cell `najaar`, `voorjaar` of
`vondstgewicht`, which has one dimension.

The document is `corpus/golden/jutterswet-seizoenkopie/ir.json` with a second
label on the first rule's target.

Red first: the verifier of main `b906f78ba` cannot load this document (`unknown field 'labels'` at the
creation's member; its rules write cells). `expect.txt` pins the arm
`WF-040.label-count-mismatch`, at the target.
