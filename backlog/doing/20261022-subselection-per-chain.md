# subselection-per-chain: een keten draagt ten hoogste één deelselectie, en die hoort bij de verzameling vlak vóór haar

*Uit het Fable-advies van 2026-10-22 (`uchar-dev/research/bruggen-2026-10-22/fable-advies.md`, vraag 2b, B2.3), de
eerste stap vóór `fold-in-element-condition`. Voorlopig besluit van de hoofdsessie naar dat advies; de eigenaar kan het
omdraaien.*

## Aanleiding

`onderwerpKeten` in `grammar/RegelSpraak.g4` (r. 696–704) is links-recursief in `(DIE|DAT) predicaat`, dus een keten
kan twee deelselecties dragen. In R1 van `fold-in-element-condition` ("… alle ladingen van de Schuit die aan alle
volgende voorwaarden voldoen: … de laatste van de einddag van alle klachten van de Schuit die aan de volgende voorwaarde
voldoen: …") hangen beide `die` als twee predicaten aan het buitenste anker `de Schuit`; `findSubselectionPredicate`
(`ExpressionBuilder` ±1690) neemt het predicaat van de buitenste context als filter van de ladingen, en het binnenste
blijft onverbruikt (RGS-099). Gemeten in het advies (`proef/k13/`): met `onderwerpKeten : onderwerpKetenKern ((DIE|DAT)
predicaat)?` ontleedt R1 precies als de haakjesvorm R2, zonder syntaxfout.

## Doel

- De grammatica laat per keten ten hoogste één `die/dat`-predicaat toe; een deelselectie hoort bij de dichtstbijzijnde
  verzameling vóór haar. Een tweede `die` op één keten ontleedt niet meer: een syntaxfout met een hint (twee eisen aan
  één verzameling staan in één "aan alle volgende voorwaarden voldoen"-lijst). Dat is een vernauwing tegenover de
  grammatica; `grammar/DIVERGENCES.md` noemt haar.
- Een gebullette binnenste deelselectie in een bullet van een buitenste lijst volgt de dieptediscipline van
  `third-condition-level`; tot die kaart landt, blijft wat nu geldt.

## Acceptatie

- (a) Rood eerst: frontendtests waarin R1 en R2 van `fold-in-element-condition` dezelfde boom geven (de binnenste
  deelselectie aan "alle klachten van de Schuit"), en een tweede `die` op één keten de syntaxfout met de hint geeft.
- (b) Geen bron in het corpus, `toka/` of de scenario-goldens draagt twee `die` op één keten (gemeten in het advies):
  gates 10, 11 en 16 byte-identiek; de zeventien gates groen.
- (c) Als meting, zonder bestand in deze repository: de v2-regelset herleving-WW ontleedt zonder nieuwe fout.

May touch: `frontends/regelspraak/**`, deze kaart.
