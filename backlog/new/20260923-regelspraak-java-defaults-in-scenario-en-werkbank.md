# regelspraak-java-defaults-in-scenario-en-werkbank: scenariotoetsen en de werkbank-UI zien de defaults-families

Besluit 8 van de onderzoekskaart dialect-defaults-niet-en-nesting
(maintainersbesluit 2026-09-23): het volgwerk in regelspraak-java krijgt
eigen kaarten, die per regime in `uchar/backlog` leven. Bron: de bijvangst
van die kaart en het voorstel `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
(kaartbranch). Het frontend is één bron (uchar, gevendord in
regelspraak-java op de pin), dus wat aan de grammatica verandert stroomt met
de pin mee; deze kaart gaat over wat daaromheen zit.

Drie gemeten gaten:

1. De scenariotoetsen (`testDocument`, `RegelSpraak.g4:88–90` in de
   toolchain-grammatica) kennen de uitzonderingsvormen niet, zodat een
   regelset met defaults niet met scenario's te toetsen is. Het
   grammaticadeel rijdt met de uchar-frontendkaarten mee (het voorstel,
   besluit 8); de scenario-harnas zelf (`ScenarioResolver`,
   `ScenarioRunner`, `make scenario-test`) leeft toolchain-lokaal
   (PRIORITIZED-DEFAULTS-EVOLUTIE §6.2) en moet defaults-groepen leren
   spelen.
2. De playground-graaf, de uitleg en de dekking lezen alleen
   `resolvedRegels`; defaults-families (`prioritizedDefaults`) zijn in de
   werkbank-UI onzichtbaar, terwijl de exec-model-vertaling ze wel
   uitvoert.
3. De uitleg per beslissing: welke default besliste (ook `onwaar`) en welke
   werden verdrongen; de rapportkeuze uit het rapport §5.3 (een
   `onwaar`-besluit als zichtbaar feit tegenover "niets besloten").

## Acceptatie (bij oppakken aanscherpen)

- (a) Een regelset met `Uitzondering op` (na de dialectkaarten ook met
  `niet` en nesting) draait in `make scenario-test` met een scenario per
  beslissende default; rood-eerst.
- (b) Graaf, uitleg en dekking tonen de defaults-groep als familie met haar
  voorrang; de uitleg noemt de beslissende default en de verdrongen
  defaults; een `onwaar`-besluit wordt benoemd.
- (c) Conformance groen op de pin; de uchar-gates ongewijzigd groen.

Het `testDocument`-grammaticadeel zit in dialect-niet-in-uitzondering en
bereikt regelspraak-java via de gevendorde slice van
regelspraak-java-dialect-pinbump; het harnasdeel van deze kaart komt dus
na die pin-bump.

May touch: regelspraak-java (harnas, playground, uitleg, dekking);
kaartverplaatsing.

Niet in deze kaart: de dialectvormen; de Drools-lowering (bestaat in de
uchar-backend; de toolchain-vuurroute behoudt haar luide weigering tot haar
eigen lowering er is, PRIORITIZED-DEFAULTS-EVOLUTIE §6.2).
