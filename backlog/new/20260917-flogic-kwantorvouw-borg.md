# flogic-kwantorvouw-borg: een tweede brontaal voor de vouwknoop (optie)

Aanleiding: de Kwantorvouw-lijn (ALLE/GEEN/TEN MINSTE/TEN HOOGSTE/
PRECIES n, filter, heelperiode- en niet-temporele matrix) heeft drie
onafhankelijke UITVOERDERS maar één brontaal: alleen de
RegelSpraak-frontend produceert de knoop; Catala en F-logic dragen hem
niet en de niet-temporele getuigen zijn handgeschreven IR. De
onafhankelijkheidsborg die F-logic voor de subtype-kernel leverde (twee
talen, één betekenis — zie `frontends/flogic/README.md`, "Why F-logic")
ontbreekt voor deze knoop. Deze kaart is een OPTIE, geen noodzaak: de
uitvoerdersborg is de zwaardere van de twee en staat.

De natuurlijke ingang, gemeten aan de slice:

- F-logic-aggregaten zijn de native spelling voor tellingen:
  `?C = count{...}, ?C >= 2`. Alle vijf kwantoren zijn via tellingen
  uitdrukbaar ZONDER negatievorm — GEEN = `count(voldoeners) = 0`,
  ALLE = `count(voldoeners) = count(leden)` — dezelfde
  één-telregel-lezing waarmee de tellende-kwantorenkaart de vijf
  kwantoren dekte. De `\naf`/`\neg`-weigering van de slice hoeft dus
  niet te bewegen.
- De echte slice-uitbreiding is de GEFILTERDE comprehension: de slice
  draagt in `count{?V | ?V = ?X.role}` nu één kale rolhop; de matrix
  (en de die/dat-filter) vergen een conditie in de comprehension-body.

De kernbeslissing van de kaart, expliciet te nemen: lowert de frontend
zulke tellingen naar `Kwantorvouw` (dan borgt hij de VORM — het doel
van deze kaart, analoog aan RegelSpraaks WF-096-promotie), of blijven
zij `Functie AANTAL` + `Vergelijking` (semantisch gelijk, maar dan
borgt de kaart niets nieuws en kan hij beter niet gebouwd worden)?
Buiten bereik, bewust: de heelperiodematrix — de slice heeft geen
tijdlijnen, en die erbij trekken is groter dan de borg waard is.

## Acceptatie (bij oppakken aanscherpen)

- (a) De gefilterde comprehension in de slice, Flora-2-2.2-conform
  (elke vorm parseert in de reasoner), met de lowering naar
  `Kwantorvouw` voor de herkende telvormen en luide weigeringen voor
  de rest.
- (b) Een golden mét `bron.flogic` naast een bestaande
  RegelSpraak-vouwgolden, over één gedeelde set case-verwachtingen —
  het wrakrecht-patroon.
- (c) Alle veertien gates groen; gate 14-vloeren bewegen mee met
  gedateerde reden.
