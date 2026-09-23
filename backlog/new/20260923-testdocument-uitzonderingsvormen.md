# testdocument-uitzonderingsvormen: scenariotoetsen en werkbank zien de defaults-families niet

*Status: concept, 23 september 2026 — bijvangst van de onderzoekskaart
dialect-defaults-niet-en-nesting (voorstel §9.3). Het grammaticadeel landt
in dialect-niet-nesting-frontend (en wacht dus op de besluiten 1–8
van spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md); het
regelspraak-java-deel hoort volgens het voorgestelde besluit 8 in de
backlog daar.*

Gelezen 2026-09-23 (niet gedraaid):

- `testDocument : ( definitie | regelGroep | regel | beslistabel |
  situatieDefinitie | scenario )* EOF` (`RegelSpraak.g4:88-90`) kent
  `uitzonderingRegel` en `uitzonderingGroep` niet: een scenariodocument met
  een `Uitzondering op`-blok parseert niet. Deze grammatica is die van uchar;
  regelspraak-java vendort haar (`vendor/uchar-frontend/` daar).
- De scenarioharnas van regelspraak-java (`ScenarioResolver`) lost de
  reikwijdte `toetst de regel <naam>` op tegen de regels van het model; een
  regel die in een defaults-groep is geheven staat niet meer in
  `resolvedRegels`.
- De playground-graaf, de uitleg en de dekking in regelspraak-java lopen
  alleen over `model.resolvedRegels` (`RegelGraaf.java:63`, `:87`, `:105`;
  `ProducentConsument.java:89`): defaults-families zijn in de werkbank-UI
  onzichtbaar.

## Werk

- uchar: de twee alternatieven in `testDocument` (in
  dialect-niet-nesting-frontend).
- regelspraak-java: de scenarioreikwijdte lost ook defaults-groepen en
  uitzonderingen op; graaf, uitleg en dekking lezen `prioritizedDefaults`
  (per default: rechtvaardiging, besluit of consequentie, de beslissende
  default in de uitleg — die laatste is ook wat vorm 3 nodig heeft, zie
  invoer-boven-berekening).

## Acceptatie (bij oppakken aanscherpen)

- (a) Een scenariotoets over een document met `Uitzondering op`-blokken
  parseert, draait en slaagt; een toets die een uitzondering bij naam als
  reikwijdte noemt ook.
- (b) De graaf toont de defaults-familie van G2 (tweeling
  ww-herleving-inkomen) met haar vier niveaus.

May touch: uchar `frontends/regelspraak/grammar/RegelSpraak.g4`
(`testDocument`); in regelspraak-java de scenario-, graaf-, uitleg- en
dekkingscode.
