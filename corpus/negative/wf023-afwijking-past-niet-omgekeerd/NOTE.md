# wf023-afwijking-past-niet-omgekeerd

Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient by an attribute in the consequence of a default, written into a target of 2 decimals.

    WF-023.precision-unbounded FAIL /rules/prioritizedDefaults/1/defaults/0/consequence

The evaluation fault EF-006 of ch. 2.4 point 1 stands behind the gate, and `case1.json` is the case on which the executors' own suites hold it: `WriteBoundaryWitnessTest` of the Drools backend runs it on both branches. Gate 8 does not replay it, since a red case is verifier-clean. What follows describes the document and its case.

The flank of `corpus/golden/jutterswet-deling-in-afwijking`, case 6: V6 not
gemerkt, geschat 30, grond 100. The deviation of `toeslag` decides, and 100/30
plus 5,00 does not fit its two decimals (EF-006, RUNTIME-SEMANTICS ch. 2.4, 5.7). The rules stand in the reverse order of `jutterswet-afwijking-past-niet`, with the same expectations.
