# wf125-dimension-and-timeline

Violates WF-125 (`spec/IR-SPEC.md` ch. 2.7), arm (d): an attribute carries a
dimension or a timeline, not both. RegelSpraak forbids the combination
(specification §3.2.1), and so does ALEF. Here `vondstgewicht` carries the
dimension `seizoen` and a `TimelineFacet` of one year.

The document is `corpus/golden/jutterswet-seizoenvondst/ir.json` with the
facet added (mutant c of the dimension research).

Red first: the verifier of main `b906f78ba` reports `verifier: PASS` on this document. `expect.txt`
pins the arm `WF-125.timeline`.
