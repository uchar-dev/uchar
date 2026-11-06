# tweede-vulling-als-eigen-fouttype: de fout van een tweede vulling heeft een eigen type en staat in de lijst "Faults"

*Vastgelegd door de hoofdsessie op 2026-10-12, uit de eindlens van de toolchainkaart
`20261010-sessie-via-de-ingang-van-uchar`. Regelnummers op uchar `b536bbc9`.*

## Aanleiding

`backends/drools/README.md` ("Faults", rond r. 154) noemt vier typen van een evaluatiefout aan het fixpunt:
`Breuk.DelingDoorNul`, `ComputeFault`, `WritersConflict` en `InputFormFault`. `setDerivedValues` werpt ze; een
afnemer beeldt ze af naar zijn eigen antwoord. De distributie geeft er een 422 met een soort per type voor.

Er is een vijfde. Geeft een feitcreatie een naar-één-rol twee verschillende vullingen, dan zet
`TmsTranspiler.secondFilling` (`transpiler/tms/TmsTranspiler.java` rond r. 2410–2427) een `Fault` met een gewone
`IllegalStateException`:

    de naar-één-rol "…" van een … heeft twee verschillende vullingen; een tweede vulling is een fout, geen overschrijving
    (RUNTIME-SEMANTICS ch. 5.5)

Getuige: `TmsCreationWitnessTest.a_second_filling_of_a_to_one_role_is_a_fault` (r. 552).
- Een afnemer die op het type afbeeldt, kan deze fout niet onderscheiden van een `IllegalStateException` uit de ingang
  zelf, zoals een verkeerd gebruikte sessie.
- De distributie leest de fouten daarom sinds die kaart uit `fixpoint.faults()` en geeft deze als soort `EvaluationFault.Kind.OTHER`.

Daarnaast (gezien door de bouwer van fixronde 2 van die kaart): de `Fault` van een tweede vulling draagt als regel de
naam van het feittype (bijvoorbeeld `havenlid`). De kop van `EvaluationFaults` zegt daardoor "in de regels "havenlid", …",
en die naam bepaalt ook de volgorde van de fouten.

## Wat er gebeurt

- De fout van een tweede vulling krijgt een eigen type in `transpiler/tms`, zoals `WritersConflict`.
- De `Fault` noemt de vorm die haar zet, niet het feittype, of de kop zegt dat het een feittype is.
- De sectie "Faults" van de README noemt elk type dat aan het fixpunt kan staan.
- Komt er later een type bij, dan noemt die sectie het ook.

## Acceptatie (bij oppakken aanscherpen)

- (a) De getuige hierboven pint het nieuwe type.
- (b) Een getuige laat zien dat elke `Fault` die de tak kan zetten een type uit de lijst van de README heeft. Dat kan als
  inventaris van de constructoraanroepen van `Fault`, of met een meting per vorm.
- (c) Alle gates groen.
- De distributie volgt bij haar volgende pinbump: haar soort `OTHER` wordt dan een eigen soort.

## Omvang

Klein. May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`, de README en de tests.

## Result

Gebouwd op branch `card/drools-frontend-cleanup` (opruimronde 2026-11-06). Logs onder
`uchar-dev/werk/java-cleanup/logs/`.

- **Het type.** `transpiler/tms/SecondFillingFault` (een `RuntimeException`, geen `IllegalStateException` meer):
  `code()` EF-017, `params()` `role` en `label` (ch. 5.7b; de twee vullers noemt de fout niet, want de regel ontmoet
  ze als paar in beide volgordes en de fout moet één feit blijven), `named(label)` noemt het object bij zijn
  case-label zoals `OpenSegmentFault`. De zin staat in beide catalogi (`EF-017`, NL en EN); `check-drools-codes.sh`
  houdt de plaatsen vast (`EF_PLACES`). De plan-consequentie `Consequence.SecondFilling` draagt nu de rol in plaats
  van een zin; beide weergaven (PatternDSL en DRL-tekst) bouwen het nieuwe type. `CaseRunner.faultKinds` (rode pad),
  `SessionDump` en het terugzetten met labels kennen het type.
- **De naam in de kop.** De `Fault` van een tweede vulling draagt als regel de naam van het feittype; de kop van
  `EvaluationFaults` noemt hem nu als feittype: `aan het fixpunt staan 2 evaluatiefouten, in de regel "aandeel" en
  het feittype "havenlid"`.
- **(a)** `TmsCreationWitnessTest.a_second_filling_of_a_to_one_role_is_a_fault` pint het type. Rood op de oude
  code: `IllegalStateException: de naar-één-rol "haven" van een Jutter …` (`k2-red.log`); groen
  `SecondFillingFault: de naar-één-rol "haven" van "J1" …`, in de sessie zonder labels met type en identiteit, op
  beide takken en in alle geschudde volgordes (`k2-green.log`). De verwachte zin van de getuige is daarmee gewijzigd;
  dat gedrag verandert de kaart uitdrukkelijk.
- **(b)** `Fault.KINDS` somt de typen op die een `Fault` mag dragen, en de constructor van `Fault` weigert elk ander
  type met een `IllegalArgumentException` (een gebroken contract, geen fout van het fixpunt). Daarmee heeft elke
  `Fault` die de tak zet een type uit de lijst, bij elke constructie; gate 12 drijft die bewaking over elke golden en
  elk rood geval (`BranchDumpsCorpusWitnessTest`). De nieuwe getuige `FaultKindsWitnessTest` houdt `Fault.KINDS`
  gelijk aan de opsomming in de sectie "Faults" van de README (acht typen: de vier oude, `OpenSegmentFault`,
  `CharacteristicPeriodsFault`, `SecondFillingFault` en `Breuk.DelingDoorNul`; de twee middelste ontbraken al), en
  toetst de weigering van een vreemd type en de kop. De javadoc van `DroolsBackend.setDerivedValues` verwijst naar
  die lijst.
- **Twee getuigen bewogen** (gate 12 eerst rood, `k2-gate12.log`): `SessionDumpWitnessTest.a_sentence_with_an_identity_is_refused`
  zette een `Fault` met een `IllegalStateException` met de hand; die draagt nu een `ComputeFault` met dezelfde zin
  (wat de getuige toetst, de weigering van een identiteit in de dump, is ongewijzigd). De vloer
  `DrlPiecesCorpusWitnessTest.EXPECTED_MIN_NON_ASCII_TEXTS` gaat van 49 naar 26: de zin "naar-één-rol" stond in de
  DRL-tekst van 23 goldens en is daar weg (een bewuste verwijdering, de vloer zakt in dezelfde commit; 26 teksten
  houden de UTF-16-plaatsen beproefd).
- **(c)** Gate 12 groen (`k2-gate12b.log`): 350 gespeeld, gelijk aan de pin; JUnit 1869 tests.
- **Voor de distributie** (pinbump, hoofdsessie): de soort `OTHER` voor een tweede vulling wordt een eigen soort,
  bijvoorbeeld `SECOND_FILLING_FAULT`, voor `e instanceof dev.uchar.regelspraak.transpiler.tms.SecondFillingFault`
  (EF-017, `code()`/`params()` zoals `OpenSegmentFault`). Haar regel (`Fault.rule()`) is de naam van een feittype.
