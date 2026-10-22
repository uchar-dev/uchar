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

## Result (2026-10-22)

Tak `card/fold-in-element-condition`, samen met `fold-in-element-condition` en `rule-variable-in-subselection`.
Geraakt: `frontends/regelspraak/**` (de grammatica, `Resolver`, vier plaatsen in `ExpressionBuilder`, het register en
beide catalogi, een testklasse, een sectie in `grammar/DIVERGENCES.md`) en de kaart. Niet geraakt: de spec, het
IR-schema, de gate-scripts, `toka/`.

- `6d973cc9` de drie kaarten naar `doing/`.
- `b082d7b5` de grammatica-vernauwing, de hint RGS-351, DIVERGENCES ("One subselection per chain"),
  `SubselectionPerChainTest`.

**De grammatica.** `onderwerpKeten : ( selector VAN onderwerp | universeelOnderwerp | onderwerpVerwijzing | ZIJN
kaleNaam ) ( ( DIE | DAT ) predicaat )?`: een keten draagt ten hoogste één deelselectie, en die hoort bij de verzameling
vlak vóór haar. Geen nieuwe regelnaam: de vorm met een ingebedde groep houdt de toegangen van `OnderwerpKetenContext`
gelijk. Vier methoden in `ExpressionBuilder` daalden af in de geneste keten van een deelselectie (`possessive`,
`referenceWithOrdinal`, `navigationNoun`, `primaryNameChain`) en lezen nu dezelfde knoop; drie lazen de kern van een
keten direct en zagen bij een deelselectie niets (`subjectAsValue`, `anchorName`, `subjectPossessive`); die kregen een
wacht `predicaat() == null`, zodat hun gedrag gelijk blijft. R1 van
`fold-in-element-condition` ontleedt nu als R2 met haakjes (rood eerst gemeten: het binnenste predicaat hing aan "de
Schuit" van de ladingen).

**Een tweede `die`.** Anders dan de kaart verwachtte, is het niet overal een syntaxfout. Waar geen keten om de
verzameling staat (de gekwantificeerde verzameling van een voorwaarde) ontleedt het niet (RGS-091). In een
vouwoperand staat er wel een keten omheen ("de maand van alle ladingen van de Schuit die …", `selector VAN onderwerp`),
en die draagt het tweede predicaat; geen vouw verbruikt het, dus RGS-099. Dat de keten eromheen een predicaat mag
dragen, is gewoon: "het gewicht van alle ladingen die zwaar zijn" hangt het predicaat daar, omdat "alle ladingen" zelf
geen keten is. Het verbieden zou een semantisch predicaat in de grammatica vragen, dat ANTLR tijdens de voorspelling
met de verkeerde context kan evalueren, of een tweede familie van `onderwerp`-regels. Beide vormen krijgen de hint
RGS-351 (ERROR) op de regel van het tweede woord, met de eerste deelselectie en de uitweg (twee eisen in één lijst "die
aan alle volgende voorwaarden voldoen:", een bullet per eis): `Resolver.subselectionHints` herkent het woord direct na
een ontlede deelselectie en, na een syntaxfout, een `die`/`dat` dat geen ontlede deelselectie opent en na een ander in
dezelfde zin staat (de `dat` van "het aantal dagen in de maand dat" telt niet).

**Acceptatie.** (a) `SubselectionPerChainTest`: R1 en R2 geven dezelfde deelselecties met hun verzameling, R1 geen
RGS-099 en geen syntaxfout, de tweede `die` in een voorwaarde de syntaxfout met de hint, in een vouwoperand (twee
vormen) RGS-099 met de hint, een losse `die` zonder eerdere deelselectie geen hint. (b) Gates 10, 11 en 16
byte-identiek (127 rgs-goldens, de TOKA-zone, 32 scenario-goldens met 110 casebestanden), gemeten na deze stap; de
volledige gateset op de eindstand van de tak: `EXIT 0` (Result van `fold-in-element-condition`). (c) De onveranderde v2-regelset herleving-WW
(fundament en herleving-ww, 36 bronnen, werkbank `c53dfd9`) geeft met deze tak en met main `8ded3090` exit 0, geen
diagnostic en een bytegelijk IR.

**Wat bleef.** Een gebullette binnenste deelselectie in een bullet van een buitenste lijst hangt nu aan haar eigen
verzameling (gemeten met `..`-bullets; tot nu nam de buitenste keten haar); welke volgende bullets van haar zijn, is
de dieptevraag van `third-condition-level`.
