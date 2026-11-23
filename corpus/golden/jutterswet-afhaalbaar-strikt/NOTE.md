# jutterswet-afhaalbaar-strikt — a status written as a characteristic, in a document that infers nothing from leeg

The rules of `corpus/golden/jutterswet-afhaalbaar` (art. 3i, a status written as a characteristic) under the line
`Afleiden uit leeg: nee` (grammar/DIVERGENCES.md, "Afleiden uit leeg"): the frontend writes `inferFromEmpty: false`
on the root of `ir.json`, and the document is read as RUNTIME-SEMANTICS ch. 1.5 lays down. A member is filled, settled
leeg or open; an input characteristic holds where the case names it in `characteristics`, does not hold where it names
it in `characteristicsAbsent` (the scenario's "- is niet drijfhout"), and is open where the case names it in neither.
Own Jutterswet-flavoured casus, own dates, rights-free.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand. "settled leeg" is an `attributeEmpty` expectation, "does not hold"
a `characteristicAbsent`, "open" an `attributeOpen` or `characteristicOpen` (the scenario's "staat niet vast").

## Cases (hand-computed, absolute)

| case | input | drijfhout | afhaalbaar | afhaaldatum |
|---|---|---|---|---|
| case1-drijfhout | claimdatum 10-03-2026, drijfhout | holds | does not hold: its condition is settled false | settled leeg: the one rule that writes it does not fire |
| case2-drijfhout-open | claimdatum 10-03-2026 | open | open | open: the condition of its rule is open |
| case3-geen-drijfhout | claimdatum 10-03-2026, drijfhout absent | does not hold | holds | 17-03-2026 |
| case4-geen-drijfhout-zonder-claimdatum | drijfhout absent | does not hold | holds | open: the rule fires and reads the open claimdatum |

Case 1 against case 2 is "komt er niet" against "nog niet": drijfhout given makes the afhaaldatum settled leeg, while
drijfhout not given leaves it open. Case 4 against case 3 of the ordinary twin: there a leeg claimdatum gives a leeg
afhaaldatum, here an open one. ref, clingo and both branches of the Drools backend play the four cases.
