# jutterswet-lezer-van-initialisatie-omgekeerd — a reader of a value the initialisation holds for a while

Document B of RUNTIME-SEMANTICS ch. 5.7, "Faults and deposits are read at the
fixpoint". `aandeel` divides the opbrengst by the `bergloon`, rekenkundig afgerond op 0
decimalen; the rule
`bergloon` writes the taxatie where the Vondst is erkend, and the
initialisation writes 0. The rules stand in the reverse order of `jutterswet-lezer-van-initialisatie`, with the same expectations.

| case | input | `bergloon` | `aandeel` | channel |
|---|---|---|---|---|
| case1-regel-beslist | V1 gemeld, taxatie 30, opbrengst 90 | **30** | **3** | consistent |
| case2-nul-van-initialisatie-aan-fixpunt | V2 not gemeld, taxatie 30, opbrengst 90 | **0** | **leeg** | (aandeel × V2), EF-005 |
| case3-nul-van-regel-aan-fixpunt | V3 gemeld, taxatie 0, opbrengst 90 | **0** | **leeg** | (aandeel × V3), EF-005 |

In case 1 the 0 of the initialisation is a state between. An `aandeel` that is
evaluated on it divides by zero, and the next evaluation replaces that outcome.
In case 2 and 3 the zero stands at the fixpoint and so does the deposit.
