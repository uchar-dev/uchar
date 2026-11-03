# jutterswet-gehele-jaar-lege-grens — "gelijk aan" and "ongelijk aan" against a leeg bound under "gedurende het gehele jaar"

The month-granular `wachtdagen` of a Jutter are tested against its scalar `wachtgrens` under the qualifier "gedurende
het gehele jaar": `gelijkpremie` with "gelijk aan", `ongelijkpremie` with "ongelijk aan", and `afwijkpremie` with the
qualifier's own negation of the inequality ("niet gedurende het gehele jaar ongelijk aan").

RUNTIME-SEMANTICS ch. 3.9: "A leeg comparison bound makes the test false, never a fault." The test is the one each
day asks, so with the `wachtgrens` leeg no day satisfies either comparison: not "gelijk aan", and not "ongelijk aan"
either, although ch. 1.2 makes a scalar inequality between a filled value and leeg true. The touched year bucket then
fails both tests and the qualifier's negation holds (case 1). A timeline without segments touches no bucket and
passes every test, whatever the bound (case 2). Cases 3 and 4 are the same document with the bound filled.

The bound is an attribute because a case cannot carry a leeg parameter: the case format has no leeg value, and a
scalar parameter the case does not bind is a fault (EF-014).

2025 has 365 days; every timeline here is one segment on [2025-01-01, 2026-01-01) or none.

| case | `wachtdagen` | `wachtgrens` | days satisfying `=` / `≠` | `gelijkpremie` | `ongelijkpremie` | `afwijkpremie` |
|---|---|---|---|---|---|---|
| case1-lege-grens | 20 | leeg | 0 / 0 of 365 | leeg | leeg | **30** |
| case2-lege-grens-geen-tijdlijn | none | leeg | no bucket touched | **20** | **10** | leeg |
| case3-gevulde-grens-gelijk | 20 | 20 | 365 / 0 of 365 | **20** | leeg | **30** |
| case4-gevulde-grens-ongelijk | 25 | 20 | 0 / 365 of 365 | leeg | **10** | leeg |

Every case is consistent.
