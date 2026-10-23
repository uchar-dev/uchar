# jutterswet-schone-maand-na-zware-klacht — a fold of the rule subject in the filter of another fold

A Schuit has ladingen (each in a maand, with a gewicht) and klachten (each with
an einddag and a soort). The first clean month after the last heavy complaint
is written as the norm states it: "de eerste van de maand van alle ladingen van
de Schuit die aan alle volgende voorwaarden voldoen: hij heeft geen overlading;
zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten
van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan
'Zwaar'". The inner fold ranges over a relation of the rule subject and stands
in the filter of the outer fold, which is read per lading. Form R1 of the card
fold-in-element-condition (bridge K13 of a rule set for the WW). Own
Jutterswet-flavoured casus, own values, rights-free.

## Casus (own text)

> **Art. 7c (schone maand).** 1. Een lading in een maand is een overlading als
> haar gewicht groter is dan 100. 2. De eerste schone maand van een schuit is de
> eerste maand van een lading van de schuit zonder overlading, waarvan de maand
> na afloop van een maand later valt dan de laatste einddag van de zware
> klachten over de schuit.

## Source (`source.rgs`)

- **overlading** (lid 1): a possessive characteristic of the lading.
- **eerste schone maand vanaf de eindmaand** (lid 2): a `first` fold over the
  ladingen of the Schuit with a two-part filter; the second part compares with
  a `last` fold over the klachten of the Schuit, filtered on the soort.

The inner fold reads the rule subject: the frontend builds it at the Schuit
and writes it with the form anchor, `anchor: "form"` and the range
`sourceRole schuitklacht.klacht` from the Schuit (IR-SPEC ch. 4.1a, WF-122;
`frontends/regelspraak/grammar/DIVERGENCES.md`, "The rule subject in a
subselection — zelf"). Until the card `subselection-reads-subject`
(2026-10-23) it wrote the way back from the lading, `sourcePath [lading
in-een-maand.schuit, schuitklacht.klacht]`, the spelling the element-role
sentence "alle klachten van de schuit van de Lading in-een-maand die …" still
gets (`FoldInElementConditionTest`); the cases did not change. The inner `die`
belongs to "alle klachten van de Schuit" (same file, "One subselection per
chain").

## Hand computation

`case1-twee-zware-klachten-en-een-lichte.json`: zware klachten K1 (einddag
15-03-2026) and K2 (10-02-2026), a lichte K3 (20-05-2026). De laatste einddag
of the zware klachten is 15-03-2026; K3 is outside the filter. L1 (maand
01-02-2026, gewicht 50): 01-02 plus 1 mnd = 01-03-2026, not later than 15-03.
L2 (01-03-2026, 50): 01-04-2026 is later, and 50 is no overlading. L3
(01-04-2026, 150): **overlading**, excluded. Eerste schone maand
**01-03-2026**. Without the soort filter the last einddag would be
20-05-2026, L2's 01-04 would not be later, and the answer would be leeg.

`case2-overlading-in-maart.json`: one zware klacht, einddag 15-03-2026. L1
(01-02, 50): 01-03 is not later. L2 (01-03, 150): **overlading**. L3 (01-04,
50): 01-05-2026 is later. Eerste schone maand **01-04-2026**.

`case3-elke-schuit-haar-eigen-klachten.json`: S1 has a zware klacht with
einddag 15-03-2026, S2 one with 20-05-2026. S1's ladingen in februari and
maart (50 each): maart passes (01-04 is later than 15-03), so S1 **01-03-2026**.
S2's ladingen in maart and mei (50 each): 01-04 is not later than 20-05,
01-06 is, so S2 **01-05-2026**. A fold over every klacht instead of the
lading's own schuit's would give S1 the einddag 20-05 and leave it leeg.

`case4-alleen-een-lichte-klacht.json`: one lichte klacht. The filter selects
no klacht, de laatste einddag over no contributor is leeg (ch. 1.2e), and an
ordering comparison with a leeg operand is false (ch. 1.2), so no lading
passes: eerste schone maand **leeg**. L2 (150) has the **overlading**.

## Executors

ref, clingo and both Drools branches replay every case; the clingo backend
moves the inner fold into a helper predicate per lading (card
clingo-nested-aggregate-in-fold-filter). The inner fold is evaluated per
lading and has no reading of its own (RUNTIME-SEMANTICS ch. 5.12: a node read
once per member keeps its number), a known limit of the explanation.
