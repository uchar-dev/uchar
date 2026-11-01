# jutterswet-fout-op-tussenstand — an evaluation fault on a state between

The hard-fault variant of document B of RUNTIME-SEMANTICS ch. 5.7, "Faults and
deposits are read at the fixpoint". The initialisation writes 7, and 90 gedeeld
door 7 fits no whole target (EF-006). The rules stand in the order of the casus; `jutterswet-fout-op-tussenstand-omgekeerd` has them in the reverse order, with the same expectations.

| case | input | `bergloon` | `aandeel` | channel |
|---|---|---|---|---|
| case1-past-aan-fixpunt | V1 gemeld, taxatie 30, opbrengst 90 | **30** | **3** | consistent |
| case2-initialisatie-past | V2 not gemeld, opbrengst 70 | **7** | **10** | consistent |

Where the reader is evaluated on the 7 of case 1, its fault is an outcome that
the next evaluation replaces. Where the 90/7 stands, the session faults:
`corpus/red/jutterswet-fout-aan-fixpunt`.
