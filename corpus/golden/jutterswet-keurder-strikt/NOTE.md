# jutterswet-keurder-strikt — contributors of a quantifier fold read without order, in a document that infers nothing from leeg

Three quantifier folds `alle` over the vondsten of a Jutter, each with a compound filter that can be undetermined and a
matrix that can neither fault nor deposit, under the line `Afleiden uit leeg: nee` (grammar/DIVERGENCES.md, "Afleiden
uit leeg"): the frontend writes `inferFromEmpty: false` on the root of `ir.json`, and the document is read as
RUNTIME-SEMANTICS ch. 1.5 lays down. A characteristic of a Vondst holds where the case names it in `characteristics`,
does not hold where it names it in `characteristicsAbsent` (the scenario's "- is niet drijfhout"), and is open where the
case names it in neither; a gewicht the case does not give is open. Own Jutterswet-flavoured casus, rights-free.

- keurder: "het gewicht van alle vondsten van de Jutter die aan ten minste één van de volgende voorwaarden voldoen:
  hij is gemerkt, hij is drijfhout, groter is dan 10,00" (filter F0, matrix M10);
- nauwgezet: the same with "die aan alle volgende voorwaarden voldoen" (filter F1, matrix M10);
- grenskeurder: filter "hij is gemerkt" or "zijn gewicht is groter dan de keurgrens" (F2), matrix M10.

These are rules 0 to 2 and cases 1 to 3 of the worked example "the filter of a contributor" of RUNTIME-SEMANTICS
ch. 5.12, whose document is the fixture `ref/test/fixtures/strict-empty/contributor-filter`; its rule 3, zwaarkeurder,
whose matrix reads the keurgrens and so can fault, is the golden `jutterswet-zwaarkeurder-strikt`. The fixture keeps
its case 4 and its red case: case 4 binds no keurgrens, and the scenario layer refuses a scenario whose rules read a
parameter it gives no value; the red case faults (EF-014), and a golden pins no fault.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand. "does not hold" is a `characteristicAbsent` expectation, "open" a
`characteristicOpen` (the scenario's "staat niet vast").

## How a fold `alle` reads a filter that is undetermined (ch. 1.5, 3.9a)

A member is a contributor where its filter is not false: true or undetermined. A filter "A of B" is true where one part
is true, false where both are false, and undetermined otherwise; "A en B" is false where one part is false, true where
both are true, and undetermined otherwise. A contributor whose filter holds counts with the verdict of its matrix. A
contributor whose filter is undetermined is true where its matrix is true (counted or not, it does not fail the fold)
and undetermined where its matrix is false or undetermined. The fold is false where a contributor whose filter holds
has a false matrix, otherwise undetermined where a contributor is undetermined, otherwise true; without contributors it
is true. The evaluation reads every filter before any matrix and stops at the first contributor after which the verdict
stands, in the order of the vondsten of the Jutter (ch. 5.7a, 5.10).

## Cases (hand-computed, absolute)

The keurgrens is 15,00 in every case. "—" is open.

**case1-voorbeeld** (J1 with V1, V2, V3)

| member | gemerkt | drijfhout | gewicht | F0 | F1 | F2 | M10 |
|---|---|---|---|---|---|---|---|
| V1 | holds | — | 20,00 | true | undetermined | true | true |
| V2 | — | does not hold | 5,00 | undetermined | false | undetermined (— of 5 > 15) | false |
| V3 | does not hold | — | 30,00 | undetermined | false | true (30 > 15) | true |

- keurder: V2's filter is undetermined and its matrix false, so V2 is undetermined; V1 and V3 are true: **open**.
- nauwgezet: V2 and V3 are no contributors (F1 false); V1, with an undetermined filter, has a true matrix: **holds**.
- grenskeurder: V2 is undetermined as under keurder; V3's filter holds through its gewicht: **open**.

**case2-gestopt** (J1 with V1, V2)

| member | gemerkt | drijfhout | gewicht | F0 | F1 | F2 | M10 |
|---|---|---|---|---|---|---|---|
| V1 | holds | does not hold | 5,00 | true | false | true | false |
| V2 | — | does not hold | 20,00 | undetermined | false | true (20 > 15) | true |

- keurder: V1's filter holds and its matrix is false, so the fold is false, and the evaluation stops before V2's
  matrix: **does not hold**.
- nauwgezet: neither is a contributor, the fold without contributors is true: **holds**.
- grenskeurder: V1 fails as under keurder; V2's filter holds: **does not hold**.

**case3-filter-en-toets-open** (J1 with V1, V2)

| member | gemerkt | drijfhout | gewicht | F0 | F1 | F2 | M10 |
|---|---|---|---|---|---|---|---|
| V1 | — | does not hold | — | undetermined | false | undetermined (— of — > 15) | undetermined |
| V2 | holds | does not hold | 20,00 | true | false | true | true |

- keurder, grenskeurder: V1's filter and matrix are both undetermined, so V1 is undetermined; V2 is true: **open**.
- nauwgezet: neither is a contributor: **holds**.

No case deposits a §9.5 fact. ref, clingo and both branches of the Drools backend play the three cases (gates 5, 6
and 12).

## Readings (RUNTIME-SEMANTICS ch. 5.12)

The matrix of none of the three folds can fault or deposit, so the Drools backend reads their members in no order of its
rules and names the fold where it is open; its reading names the first open member and the position of each
contributor in the order of the vondsten of the Jutter, as the reference interpreter does. A contributor whose filter
is undetermined writes `filter undetermined` after its position; one whose filter holds writes nothing there. Gate 17
holds the readings of `runner --trace` to those of the Drools backend on both branches. The contributors of node 0 of
each rule, per case:

| case | keurder | nauwgezet | grenskeurder |
|---|---|---|---|
| 1 | `"V1" true reached; "V2" false reached filter undetermined; "V3" true reached filter undetermined` | `"V1" true reached filter undetermined` | `"V1" true reached; "V2" false reached filter undetermined; "V3" true reached` |
| 2 | `"V1" false reached; "V2" true unreached filter undetermined` | none | `"V1" false reached; "V2" true unreached` |
| 3 | `"V1" undetermined reached filter undetermined; "V2" true reached` | none | as keurder |

V2 of case 2 is not reached, and its filter is undetermined all the same: the filter is read before every matrix. The
undetermined folds name the open read of their first undetermined member, `open "V2" "gemerkt"` in case 1 and
`open "V1" "gemerkt"` in case 3. Seven contributors have an undetermined filter.
