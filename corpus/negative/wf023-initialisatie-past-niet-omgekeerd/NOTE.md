# wf023-initialisatie-past-niet-omgekeerd

Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient by an attribute in an initialisation, written into a whole target.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The evaluation fault EF-006 of ch. 2.4 point 1 stands behind the gate, and `case1.json` is the case on which the executors' own suites hold it: `WriteBoundaryWitnessTest` of the Drools backend runs it on both branches. Gate 8 does not replay it, since a red case is verifier-clean. What follows describes the document and its case.

The flank of `corpus/golden/jutterswet-initialisatie-naast-regel`: V6 not gemeld,
opbrengst 100, deelnemers 3. The initialisation decides, and 100/3 does not fit
the whole `bergloon` (EF-006, RUNTIME-SEMANTICS ch. 2.4, 5.7). The rules stand in the reverse order of `jutterswet-initialisatie-past-niet`, with the same expectations.
