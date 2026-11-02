# jutterswet-kenmerk-aan-fixpunt-omgekeerd — a characteristic whose attribution does not decide at the fixpoint

Two rules attribute `spoedeisend`, one where the `meldingsgetal` is below 5 and one where the `boete` exceeds
10, and the rule `spoedpremie` reads the characteristic. A characteristic holds at the fixpoint where an attribution
that grants it decides there, or where the input sets it (RUNTIME-SEMANTICS ch. 5.3): case 1 has the input
`meldingsgetal` 3, which the computation replaces by 10, so neither attribution decides and the `spoedpremie` is
leeg; in case 3 the other attribution decides, and in case 4 the input sets the characteristic.

The rules stand in the reverse order of `jutterswet-kenmerk-aan-fixpunt`, with the same expectations.

| case | input | `meldingsgetal` | `spoedeisend` | `spoedpremie` |
|---|---|---|---|---|
| case1-voorwaarde-vervalt | V1 aantal getuigen 10, meldingsgetal 3 | **10** | absent | **leeg** |
| case2-voorwaarde-blijft | V2 aantal getuigen 2, meldingsgetal 8 | **2** | present | **1** |
| case3-andere-toekenning-beslist | V3 aantal getuigen 10, meldingsgetal 3, boete 20 | **10** | present | **1** |
| case4-invoer-staat | V4 aantal getuigen 10, meldingsgetal 3, spoedeisend | **10** | present | **1** |
