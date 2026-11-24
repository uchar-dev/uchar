# jutterswet-kenmerknamen-strikt — characteristics stated not to hold, with names that are no Java identifier

A Vondst with four input characteristics whose names are written with a space, a hyphen, a word in capitals and an
apostrophe, under the line `Afleiden uit leeg: nee` (grammar/DIVERGENCES.md, "Afleiden uit leeg"): the frontend writes
`inferFromEmpty: false` on the root of `ir.json`, and the document is read as RUNTIME-SEMANTICS ch. 1.5 lays down. An
input characteristic holds where the case names it in `characteristics`, does not hold where it names it in
`characteristicsAbsent` (the scenario's "- is niet …"), and is open where the case names it in neither. Its twin
`corpus/golden/jutterswet-kenmerknamen` carries the same rules in an ordinary document. Own Jutterswet-flavoured casus,
rights-free.

An executor that derives a member's name in its own language from the declared name has to bring these names to a form
of its own: the Drools backend reads `op zee geborgen` as the bean property `opZeeGeborgen`, `ex-VOC` as `exVoc` and
`met foto's vastgelegd` as `metFotoSVastgelegd`.

## Casus (own text)

> **Art. 3j (afhaling na berging).** Een vondst is afhaalbaar indien zij niet op zee is geborgen, niet half vergaan
> is, niet uit een wrak van de VOC komt, en met foto's is vastgelegd.

## Source (`source.rgs`)

A Vondst with the input characteristics op zee geborgen, half-vergaan, ex-VOC and met foto's vastgelegd, and the
characteristic afhaalbaar, and one rule, **afhaalbare vondst**: "Een Vondst is afhaalbaar indien hij aan alle volgende
voorwaarden voldoet", three negated characteristic checks and one characteristic check.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand. "does not hold" is a `characteristicAbsent` expectation, "open" a
`characteristicOpen` (the scenario's "staat niet vast").

## Cases (hand-computed, absolute)

The condition of afhaalbaar is a conjunction, read with Kleene's strong table (RUNTIME-SEMANTICS ch. 1.5): it holds
where every member holds, does not hold where one member does not hold, and is open otherwise.

| case | op zee geborgen | half-vergaan | ex-VOC | met foto's vastgelegd | afhaalbaar |
|---|---|---|---|---|---|
| case1-niets-aan-de-hand | does not hold | does not hold | does not hold | holds | holds: every member holds |
| case2-herkomst-open | does not hold | does not hold | open | holds | open: the member "niet ex-VOC" is open, the others hold |
| case3-op-zee-geborgen-zonder-foto-s | holds | open | open | does not hold | does not hold: the member "niet op zee geborgen" does not hold, and the members after it are not read |

Each case expects the input characteristics it states not to hold as `characteristicAbsent`. Case 1 against case 2:
ex-VOC stated not to hold makes afhaalbaar hold, ex-VOC not named leaves it open. ref, clingo and both branches of the
Drools backend play the three cases.
