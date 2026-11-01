# jutterswet-sleeptarief — a list of factors in every direction, and a number over a unit as a value

A towing tariff in `€/dg/m` has two negative factors. `sleeptarief maal sleepdagen maal sleeplengte` takes both away,
one by one, into `€` (`spec/RUNTIME-SEMANTICS.md` ch. 2.6, rule 2). `sleeptarief maal sleeplengte` takes one, into `€/dg`, and
`daggeld gedeeld door sleepdagen` adds a power, into `€/dg^2` (rule 3). `sleepkosten maal sleeplengte` has no factor to
cancel, so its unit is a product, and the attribute that holds it is declared `met eenheid €·m`, the spelling of
`grammar/DIVERGENCES.md`; the threshold of `zwaar` is spelled `200 m*€`, which is one list with `€·m`.

`2/dg` is a value: the literal 2 in the unit `1/dg` (`grammar/DIVERGENCES.md`, "A number over a unit"). It stands in a
rule that writes `normtempo`, in the comparison of `vlot`, in the scenario input `het opgegeven tempo is 3/dg` and in the
expectation of `normtempo`; beside them `2 1/dg` stands as the same value. Every factor is 1, which the integer encoding of
the ASP backend needs. `scenario.rgs` is the source of the two case files (gate 16).

| | lange sleep | sleep op de norm |
|---|---|---|
| sleeptarief | 5 `€/dg/m` | 4 `€/dg/m` |
| sleepdagen | 2 dg | 5 dg |
| sleeplengte | 3 m | 5 m |
| sleepkosten | 5 × 2 × 3 = 30 € | 4 × 5 × 5 = 100 € |
| daggeld | 5 × 3 = 15 `€/dg` | 4 × 5 = 20 `€/dg` |
| daggeldstijging, naar beneden | 15 ÷ 2 = 7,5, dus 7 `€/dg^2` | 20 ÷ 5 = 4 `€/dg^2` |
| normtempo | 2 `1/dg` | 2 `1/dg` |
| opgegeven tempo | 3 `1/dg` | 2 `1/dg` |
| vlot (> 2 `1/dg`) | ja | nee: 2 is niet groter dan 2 |
| lengtegeld | 30 × 3 = 90 `€·m` | 100 × 5 = 500 `€·m` |
| zwaar (> 200 `€·m`) | nee | ja |
