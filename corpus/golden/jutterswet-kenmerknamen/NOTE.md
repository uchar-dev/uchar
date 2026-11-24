# jutterswet-kenmerknamen — characteristics stated not to hold in an ordinary document

The rules of `corpus/golden/jutterswet-kenmerknamen-strikt` (art. 3j, four input characteristics whose names are
written with a space, a hyphen, a word in capitals and an apostrophe) in an ordinary document, without the line
`Afleiden uit leeg: nee`. Own Jutterswet-flavoured casus, rights-free.

The document reads a missing value as RUNTIME-SEMANTICS ch. 1.2 lays down: a characteristic the case does not name does
not hold. A case may still name a characteristic in `characteristicsAbsent` (the scenario's "- is niet …"); in this
document that changes no outcome (ch. 1.5), and the frontend warns at each such line of `scenario.rgs` that it changes
nothing here.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.

## Cases (hand-computed, absolute)

| case | input | op zee geborgen, half-vergaan, ex-VOC | afhaalbaar |
|---|---|---|---|
| case1-niets-genoemd | met foto's vastgelegd | do not hold: not named | holds: every member of the condition holds |
| case2-vast-niet-gegeven | met foto's vastgelegd; the other three in `characteristicsAbsent` | do not hold | holds |

The two cases have the same expectations: stating that a characteristic does not hold is what not naming it already
says. ref, clingo and both branches of the Drools backend play the two cases.
