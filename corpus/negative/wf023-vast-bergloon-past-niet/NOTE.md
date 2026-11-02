# wf023-vast-bergloon-past-niet

Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: the literal 1,5 written into a whole target.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The evaluation fault EF-006 of ch. 2.4 point 1 stands behind the gate, and `case1.json` is the case on which the executors' own suites hold it: `WriteBoundaryWitnessTest` of the Drools backend runs it on both branches. Gate 8 does not replay it, since a red case is verifier-clean. What follows describes the document and its case.

"Het bergloon van een Vondst moet gesteld worden op 1,5", with a whole `bergloon`. The value
3/2 does not fit 0 decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 5.7b); the
literal 1,50 afgerond op 0 decimalen rekenkundig would write 2. The RegelSpraak frontend
refuses this rule statically (RGS-372), so the document is IR without a source.
