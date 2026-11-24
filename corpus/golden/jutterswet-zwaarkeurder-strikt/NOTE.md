# jutterswet-zwaarkeurder-strikt — the filter of a contributor of a quantifier fold, in a document that infers nothing from leeg

One quantifier fold `alle` over the vondsten of a Jutter, with a compound filter, under the line
`Afleiden uit leeg: nee` (grammar/DIVERGENCES.md, "Afleiden uit leeg"): the frontend writes `inferFromEmpty: false` on
the root of `ir.json`, and the document is read as RUNTIME-SEMANTICS ch. 1.5 lays down. A characteristic of a Vondst
holds where the case names it in `characteristics`, does not hold where it names it in `characteristicsAbsent` (the
scenario's "- is niet drijfhout"), and is open where the case names it in neither; a gewicht the case does not give is
open. Own Jutterswet-flavoured casus, rights-free.

zwaarkeurder: "een Jutter is zwaarkeurder indien het gewicht van alle vondsten van de Jutter die aan ten minste één van
de volgende voorwaarden voldoen: hij is gemerkt, hij is drijfhout, groter is dan de keurgrens". This is rule 3 of the
document of the worked example "the filter of a contributor" of RUNTIME-SEMANTICS ch. 5.12 (the fixture
`ref/test/fixtures/strict-empty/contributor-filter`), with cases 1 to 3 of that example. Its matrix reads the
keurgrens, a parameter, so it can fault, and the Drools backend reads the contributors in their order in every build.
The other three rules of the example have a matrix that can neither fault nor deposit; for those the backend binds the
order of the contributors in a build with readings alone, which makes their contributions differ from the build
without, and its witness `ReadingRulesFixpointWitnessTest` holds the two builds to the same facts, so they stand in
the fixture only.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand. "does not hold" is a `characteristicAbsent` expectation, "open" a
`characteristicOpen` (the scenario's "staat niet vast").

## How a fold `alle` reads a filter that is undetermined (ch. 1.5, 3.9a)

A member is a contributor where its filter is not false: true or undetermined. The filter "A of B" is true where one
part is true, false where both are false, and undetermined otherwise. A contributor whose filter holds counts with the
verdict of its matrix. A contributor whose filter is undetermined is true where its matrix is true and undetermined
where its matrix is false or undetermined. The fold is false where a contributor whose filter holds has a false
matrix, otherwise undetermined where a contributor is undetermined, otherwise true. The evaluation reads every filter
before any matrix and stops at the first contributor that fails.

## Cases (hand-computed, absolute)

The keurgrens is 15,00 in every case. "—" is open; F is the filter, M the matrix "gewicht groter dan 15,00".

**case1-voorbeeld** (J1 with V1, V2, V3)

| member | gemerkt | drijfhout | gewicht | F | M |
|---|---|---|---|---|---|
| V1 | holds | — | 20,00 | true | true |
| V2 | — | does not hold | 5,00 | undetermined | false |
| V3 | does not hold | — | 30,00 | undetermined | true |

V2's filter is undetermined and its matrix false, so V2 is undetermined; V1 and V3 are true: zwaarkeurder is **open**.

**case2-gestopt** (J1 with V1, V2)

| member | gemerkt | drijfhout | gewicht | F | M |
|---|---|---|---|---|---|
| V1 | holds | does not hold | 5,00 | true | false |
| V2 | — | does not hold | 20,00 | undetermined | true |

V1's filter holds and its matrix is false, so the fold is false, and the evaluation stops before V2's matrix:
zwaarkeurder **does not hold**.

**case3-filter-en-toets-open** (J1 with V1, V2)

| member | gemerkt | drijfhout | gewicht | F | M |
|---|---|---|---|---|---|
| V1 | — | does not hold | — | undetermined | undetermined |
| V2 | holds | does not hold | 20,00 | true | true |

V1's filter and matrix are both undetermined, so V1 is undetermined; V2 is true: zwaarkeurder is **open**.

No case deposits a §9.5 fact. ref, clingo and both branches of the Drools backend play the three cases (gates 5, 6
and 12). Case 2 survives the no-op guard (gate 7): its assertion is the absence of zwaarkeurder.

## Readings (RUNTIME-SEMANTICS ch. 5.12)

A contributor whose filter is undetermined writes `filter undetermined` after its position; one whose filter holds
writes nothing there. Gate 17 holds the readings of `runner --trace` to those of the Drools backend on both branches.
Node 0 of the rule, per case:

| case | status | node 0 |
|---|---|---|
| 1 | `OPEN` | `QuantifierFold undetermined open "V2" "gemerkt" contributors ["V1" true reached; "V2" false reached filter undetermined; "V3" true reached filter undetermined]` |
| 2 | `FAILS` | `QuantifierFold false contributors ["V1" false reached; "V2" true unreached filter undetermined]` |
| 3 | `OPEN` | `QuantifierFold undetermined open "V1" "gemerkt" contributors ["V1" undetermined reached filter undetermined; "V2" true reached]` |

V2 of case 2 is not reached, and its filter is undetermined all the same: the filter is read before every matrix. The
two undetermined folds name the open read of their first undetermined member. Four contributors have an undetermined
filter.
