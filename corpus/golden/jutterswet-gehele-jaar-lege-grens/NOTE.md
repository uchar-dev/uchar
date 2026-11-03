# jutterswet-gehele-jaar-lege-grens — "gelijk aan" and "ongelijk aan" against a leeg day under "gedurende het gehele jaar"

The month-granular `wachtdagen` of a Jutter are tested against its scalar `wachtgrens` under the qualifier "gedurende
het gehele jaar": `gelijkpremie` with "gelijk aan", `ongelijkpremie` with "ongelijk aan", and `afwijkpremie` with the
qualifier's own negation of the inequality ("niet gedurende het gehele jaar ongelijk aan").

RUNTIME-SEMANTICS ch. 3.9: each day of a touched year asks the comparison of ch. 1.2, and a day reads leeg where the
`wachtdagen` have no segment (a gap day) and on every day where the `wachtgrens` is leeg. "Gelijk aan" between two
numbers holds on two leeg sides, "ongelijk aan" on exactly one; a year passes where all its days satisfy. With the
`wachtgrens` leeg every filled day satisfies "ongelijk aan" and no gap day does (case 1, case 6); with it filled every
gap day satisfies "ongelijk aan" (case 5). A year that the `wachtdagen` touch holds a filled day, which is never equal
to a leeg `wachtgrens`, so "gelijk aan" against a leeg bound fails every touched year, gap days or not (case 1, case 6).
A timeline without segments touches no year and passes every test, whatever the bound (case 2). This is the reading
the clip of ch. 3.6 gives the same comparison per period.

The bound is an attribute because a case cannot carry a leeg parameter: the case format has no leeg value, and a
scalar parameter the case does not bind is a fault (EF-014).

2025 has 365 days. A timeline here is one segment on [2025-01-01, 2026-01-01), one on [2025-01-01, 2025-02-01) (31
days, and 334 gap days), or none.

| case | `wachtdagen` | `wachtgrens` | days satisfying `=` / `≠` | `gelijkpremie` | `ongelijkpremie` | `afwijkpremie` |
|---|---|---|---|---|---|---|
| case1-lege-grens | 20 all year | leeg | 0 / 365 of 365 | leeg | **10** | leeg |
| case2-lege-grens-geen-tijdlijn | none | leeg | no year touched | **20** | **10** | leeg |
| case3-gevulde-grens-gelijk | 20 all year | 20 | 365 / 0 of 365 | **20** | leeg | **30** |
| case4-gevulde-grens-ongelijk | 25 all year | 20 | 0 / 365 of 365 | leeg | **10** | leeg |
| case5-gatdagen-gevulde-grens | 7 in January | 5 | 0 / 31 + 334 of 365 | leeg | **10** | leeg |
| case6-gatdagen-lege-grens | 7 in January | leeg | 334 / 31 of 365 | leeg | leeg | **30** |

Every case is consistent.
