# wf023-halvering-past-niet

Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a whole opbrengst times 0,5 written into a whole target: the literal factor adds one decimal.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The evaluation fault EF-006 of ch. 2.4 point 1 stands behind the gate, and `case1.json` is the case on which the executors' own suites hold it: `WriteBoundaryWitnessTest` of the Drools backend runs it on both branches. Gate 8 does not replay it, since a red case is verifier-clean. What follows describes the document and its case.

"Het veilingdeel van een Jutter moet berekend worden als zijn opbrengst maal 0,5", with
a whole `veilingdeel`. J1 with opbrengst 1001: the product is 1001/2, which does not fit
0 decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 5.7b). With opbrengst 1000 the
same rule writes 500 and plays. The RegelSpraak frontend refuses this rule statically
(RGS-338: the declared precision of the product, 1 decimal, exceeds the target's 0), so
the document is IR without a source.
