# Een unit-dragende klem-golden, zodat WF-099's convertibele helft in het gespeelde kanaal staat

Goal: één golden toevoegen die een `Begrensd` draagt met een **eenheid** op
basis én grenzen, zodat de convertibiliteitslezing die WF-099 vastlegt door de
uitvoerders wordt gespeeld in plaats van alleen door de verifier te worden
getoetst. Vandaag is `corpus/golden/jutterswet-grensbedrag` de **enige**
`Begrensd`-drager in `corpus/golden` (gemeten 2026-09-10 over 166
golden-`ir.json`), en hij is op basis, minimum én maximum eenheidloos. Daardoor
pint het gespeelde kanaal alleen de toegelaten allebei-eenheidloos-lezing; de
convertibele helft — de lezing waar WF-099 om draait — rust op de uitvoerders
en op de falsifieerbare variant van de negatief-getuige, niet op een gegatet
document. Gate 13 zegt het in zoveel woorden over de huidige corpus: de
`Begrensd` is daar "on this corpus always UNITLESS".

De vorm die de kaart moet leveren: een klem over een basis met eenheid, met
minstens één grens in een **convertibele zustereenheid** (bijvoorbeeld een
`t`-grens op een `kg`-basis binnen één gedeclareerd stelsel, factor 1000 naar
canoniek `kg`) plus de handberekende verwachting die de herschaling vastlegt.
Gemeten 2026-09-10 spelen `ref`, de ASP-backend en beide Drools-takken die vorm
en geven ze alle drie dezelfde waarde, dus de golden hoort speelbaar te zijn in
het volle vierkant.

**Twee beschermde scripts bewegen mee — expliciet als voorstel, nooit als
merge.** Dit is de reden dat de WF-099-kaart deze golden niet zelf heeft
gebouwd:

- `scripts/check-drools.sh` — `EXPECTED_PLAYED` is een **gelijkheid**
  (beheerdersbesluit 2026-09-06, bewust geen vloer). Een nieuwe golden die
  speelt verhoogt hem met één; een die niet speelt hoort met reden in
  `EXPECTED_CHANNEL`. Beide kanten falen vandaag bij een ongewijzigd script.
- `scripts/check-reftype-derivation.sh` — de twist-boekhouding van gate 13
  draagt de vastlegging dat de `Begrensd` op deze corpus altijd eenheidloos is,
  en de klem van stap 6 FAALT zodra een unit-dragende annotatie opduikt op een
  knoopklasse buiten de twist-tabel. Een unit-dragende klem-golden is precies
  dat geval, dus de tabel of de vrijstellingsredenering moet mee.

Acceptance:
- de nieuwe golden ligt in `corpus/golden/` met `ir.json` + `case*.json` en
  handberekende verwachtingen (geen van een uitvoerder overgenomen waarde);
- `bash scripts/check-ir-schema.sh` en `bash scripts/check-verifier.sh` groen
  (de golden is verifier-schoon en WF-099 laat hem door);
- `bash scripts/check-oracle.sh` en `bash scripts/check-clingo.sh` groen: `ref`
  en de ASP-backend spelen hem exact op de vastgepinde waarden;
- `bash scripts/check-drools.sh` groen mét de voorgestelde `EXPECTED_PLAYED`-bump
  (of, als de vorm niet speelt, een reden-dragende `EXPECTED_CHANNEL`-regel);
- `bash scripts/check-noop.sh` groen — de golden hoort onder `--noop` te FALEN
  en dus niet op de survivor-lijst te komen; als hij overleeft, met reden erop;
- `bash scripts/check-reftype-derivation.sh` groen met de voorgestelde
  aanpassing van de twist-tabel/boekhouding;
- de Result noteert welke twee scriptregels bewogen en waarom, zodat de
  beheerder ze los kan beoordelen.

May touch: `corpus/golden/`, en als **voorstel** `scripts/check-drools.sh` en
`scripts/check-reftype-derivation.sh`. Eventueel `spec/IR-SPEC.md` om de zin
"no golden carries a UNIT-BEARING clamp" in WF-099's registerrij bij te werken
zodra hij niet meer waar is.

Out of scope:
- WF-099 zelf — de regel staat en beweegt niet.
- De kruis-stelsel-divergentie: die heeft haar eigen kaart
  (`20260910-kruis-stelsel-eenheid-divergentie.md`) en de golden moet er juist
  **buiten** blijven — één gedeclareerd stelsel, anders bouwt deze kaart een
  document waarvan bekend is dat de ASP-backend het weigert.
- De eenzijdige-eenheidsparen-klasse (ch. 4.1): één besluit voor WF-023,
  WF-035, WF-036 en WF-099 samen, niet hier.
