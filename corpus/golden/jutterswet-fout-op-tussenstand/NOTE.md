# jutterswet-fout-op-tussenstand — an evaluation fault on a state between

The hard-fault variant of document B of RUNTIME-SEMANTICS ch. 5.7, "Faults and
deposits are read at the fixpoint". The initialisation writes a bergtermijn of
30, and the date with year 2025, month 2 and day 30 names no calendar day
(EF-032, ch. 3.8). The rules stand in the order of the casus; `jutterswet-fout-op-tussenstand-omgekeerd` has them in the reverse order, with the same expectations.

| case | input | `bergtermijn` | `vervaldag` | channel |
|---|---|---|---|---|
| case1-past-aan-fixpunt | V1 gemeld, opgegeven termijn 28, vervalmaand 2 | **28** | **2025-02-28** | consistent |
| case2-initialisatie-past | V2 not gemeld, vervalmaand 3 | **30** | **2025-03-30** | consistent |

Where the reader `vervaldag` is evaluated on the 30 of case 1, its fault is an
outcome that the next evaluation replaces. Where a day that is no calendar day
stands, the session faults: `corpus/red/jutterswet-fout-aan-fixpunt`.
