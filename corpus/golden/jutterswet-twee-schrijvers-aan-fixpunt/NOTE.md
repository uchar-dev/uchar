# jutterswet-twee-schrijvers-aan-fixpunt — two writers of one target, read at the fixpoint

Two rules write the `spoedpremie`: 1 where the `meldingsgetal` is below 5, the `boete` where it exceeds 20.
The value at the fixpoint is that of the writers that decide there (RUNTIME-SEMANTICS ch. 5.7): none, and the
target keeps its input or stays leeg (case 1, 2, 5); one, and the target takes its value (case 3, 4), also where
that value is leeg (case 6: a writer that fires with a leeg value writes leeg, ch. 1.2g). In case 1, 2, 3 and 6 the
input `meldingsgetal` 3 holds the condition of the first writer on the state before the `meldingsgetal` is
computed, in case 4 and 5 the input 30 that of the second.

The rules stand in the order of the casus; `jutterswet-twee-schrijvers-aan-fixpunt-omgekeerd` has them in the reverse order, with the same expectations.

| case | input | `meldingsgetal` | `spoedpremie` |
|---|---|---|---|
| case1-geen-schrijver-beslist | V1 aantal getuigen 10, meldingsgetal 3 | **10** | **leeg** |
| case2-invoer-staat | V2 aantal getuigen 10, meldingsgetal 3, spoedpremie 7 | **10** | **7** |
| case3-veel-meldingen-beslist | V3 aantal getuigen 30, meldingsgetal 3, boete 4 | **30** | **4** |
| case4-weinig-meldingen-beslist | V4 aantal getuigen 2, meldingsgetal 30, boete 4 | **2** | **1** |
| case5-veel-meldingen-vervalt | V5 aantal getuigen 10, meldingsgetal 30, boete 4, spoedpremie 7 | **10** | **7** |
| case6-lege-boete-beslist | V6 aantal getuigen 30, meldingsgetal 3, spoedpremie 7 | **30** | **leeg** |
