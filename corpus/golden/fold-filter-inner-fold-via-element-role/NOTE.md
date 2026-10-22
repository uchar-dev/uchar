# fold-filter-inner-fold-via-element-role — an instance fold in the subselection of another

A Schuit has ladingen (each in a maand, with a gewicht) and klachten (each
with an einddag). The first clean month is "de eerste van de maand van
alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen: hij
heeft geen overlading; zijn maand plus 1 mnd is later dan de laatste van
de einddag van alle klachten van de schuit van de Lading in-een-maand".
The second condition holds an instance fold whose range starts at the
element and runs over its schuit to the klachten (a `sourcePath` of two
steps, IR-SPEC ch. 4.3): form R4 of the card fold-in-element-condition.

The RegelSpraak frontend wrote this IR with a WARNING in `diagnostics`
("keten zonder sourcePath in een vouwbron: … dus de aggregatie blijft
onopgelost"), while the IR carried the fold with its `sourcePath`; since
that card (point (3), 2026-10-22) the type check reads the subselection on
the element and the IR carries no diagnostic. The direct spelling "… alle
klachten van de Schuit …" is written with the same `sourcePath` (golden
`jutterswet-schone-maand-na-zware-klacht`, with a filter on the soort). The ASP backend moves
the inner fold into a helper predicate per lading, since clingo admits no
aggregate inside an aggregate element (card
clingo-nested-aggregate-in-fold-filter; until then a parse error).

A lading has the overlading when its gewicht is greater than 100.

## Hand computation

`case1-twee-klachten.json`: klachten K1 einddag 15-03-2026 and K2 einddag
10-02-2026, so de laatste einddag is 15-03-2026 for every lading of S1.
L1 (maand 01-02-2026, gewicht 50): 01-02 plus 1 mnd = 01-03-2026, not later
than 15-03. L2 (01-03-2026, 50): 01-04-2026 is later, and 50 is no
overlading. L3 (01-04-2026, 150): **overlading**, excluded. Eerste schone
maand **01-03-2026**; L1 and L2 have **no** overlading.

`case2-overlading-in-maart.json`: one klacht, einddag 15-03-2026. L1
(01-02, 50): 01-03 is not later. L2 (01-03, 150): **overlading**. L3
(01-04, 50): 01-05-2026 is later. Eerste schone maand **01-04-2026**.

`case3-geen-klacht.json`: no klachten. De laatste einddag over no
contributor is leeg (ch. 1.2e), and an ordering comparison with a leeg
operand is false (ch. 1.2), so no lading passes. Eerste schone maand
**leeg**; L2 (150) has the **overlading**.
