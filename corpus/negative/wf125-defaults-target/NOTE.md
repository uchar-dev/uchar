# wf125-defaults-target

Violates WF-125 (`spec/IR-SPEC.md` ch. 2.7), arm (c): a form that names a
member whole — an allocation, a uniqueness key, a defaults group, a
start-point determination — has no cell to name, so it does not name a
dimensioned attribute. Here the three defaults groups of
`corpus/golden/jutterswet-wachtgeld` target `wachtgeld`, which this document
dimensions by a dimension `dienst` (labels `dag`, `nacht`).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on this document. `expect.txt`
pins the arm `WF-125.whole-member` with `form=a defaults group`; the
verifier reports it once per group.
