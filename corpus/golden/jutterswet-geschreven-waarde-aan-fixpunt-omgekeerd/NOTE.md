# jutterswet-geschreven-waarde-aan-fixpunt-omgekeerd — a written value whose condition turns false

The rule `spoedpremie` writes 1 where the `meldingsgetal` is below 5, and the rule `meldingsgetal` replaces
the input `meldingsgetal` by the `aantal getuigen`. Read first, the input 3 of case 1 and 3 holds the condition on
the state before the `meldingsgetal` is computed; at the fixpoint it is 10, the condition is false, and the rule
does not decide (RUNTIME-SEMANTICS ch. 6.1, ch. 7.8): the `spoedpremie` is what it was before the rule wrote it,
leeg in case 1 and the input 7 in case 3. In case 4 the `aantal getuigen` is leeg, so the `meldingsgetal`, a
standing target (ch. 7.8), keeps its input 3 and the condition holds.

The rules stand in the reverse order of `jutterswet-geschreven-waarde-aan-fixpunt`, with the same expectations.

| case | input | `meldingsgetal` | `spoedpremie` |
|---|---|---|---|
| case1-voorwaarde-vervalt | V1 aantal getuigen 10, meldingsgetal 3 | **10** | **leeg** |
| case2-voorwaarde-blijft | V2 aantal getuigen 2, meldingsgetal 8 | **2** | **1** |
| case3-invoer-staat | V3 aantal getuigen 10, meldingsgetal 3, spoedpremie 7 | **10** | **7** |
| case4-lege-waarde-schrijft-niet | V4 meldingsgetal 3 | **3** | **1** |
