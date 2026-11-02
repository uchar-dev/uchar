# wf023-kopie-past-niet

Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a read of 2 decimals written into a whole target.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The evaluation fault EF-006 of ch. 2.4 point 1 stands behind the gate, and `case1.json` is the case on which the executors' own suites hold it: `WriteBoundaryWitnessTest` of the Drools backend runs it on both branches. Gate 8 does not replay it, since a red case is verifier-clean. What follows describes the document and its case.

"Het bergloon van een Vondst moet gesteld worden op zijn taxatie", with a `taxatie` of 2
decimals and a whole `bergloon`. V1 with taxatie 1.50: the value 3/2 does not fit 0
decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 5.7b). The value counts, not the
scale it is written at: with taxatie 1.00 the same rule writes 1 and plays. The RegelSpraak
frontend refuses this rule statically (RGS-338), so the document is IR without a source.
