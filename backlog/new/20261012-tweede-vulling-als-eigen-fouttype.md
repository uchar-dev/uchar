# tweede-vulling-als-eigen-fouttype: de fout van een tweede vulling heeft een eigen type en staat in de lijst "Faults"

*Vastgelegd door de hoofdsessie op 2026-10-12, uit de eindlens van de toolchainkaart
`20261010-sessie-via-de-ingang-van-uchar`. Regelnummers op uchar `b536bbc9`.*

## Aanleiding

`backends/drools/README.md` ("Faults", rond r. 154) noemt vier typen van een evaluatiefout aan het fixpunt:
`Breuk.DelingDoorNul`, `Rekenfout`, `Schrijversconflict` en `Invoervormfout`. `zetAfgeleideWaarden` werpt ze; een
afnemer beeldt ze af naar zijn eigen antwoord. De distributie geeft er een 422 met een soort per type voor.

Er is een vijfde. Geeft een feitcreatie een naar-één-rol twee verschillende vullingen, dan zet
`TmsTranspiler.tweedeVulling` (`transpiler/tms/TmsTranspiler.java` rond r. 2410–2427) een `Fout` met een gewone
`IllegalStateException`:

    de naar-één-rol "…" van een … heeft twee verschillende vullingen; een tweede vulling is een fout, geen overschrijving
    (RUNTIME-SEMANTICS ch. 5.5)

Getuige: `TmsCreatieWitnessTest.een_tweede_vulling_van_een_naar_een_rol_is_een_fout` (r. 552).
- Een afnemer die op het type afbeeldt, kan deze fout niet onderscheiden van een `IllegalStateException` uit de ingang
  zelf, zoals een verkeerd gebruikte sessie.
- De distributie leest de fouten daarom sinds die kaart uit `lees(…).fouten()` en geeft deze als soort `ANDERE`.

Daarnaast (gezien door de bouwer van fixronde 2 van die kaart): de `Fout` van een tweede vulling draagt als regel de
naam van het feittype (bijvoorbeeld `havenlid`). De kop van `Evaluatiefouten` zegt daardoor "in de regels "havenlid", …",
en die naam bepaalt ook de volgorde van de fouten.

## Wat er gebeurt

- De fout van een tweede vulling krijgt een eigen type in `transpiler/tms`, zoals `Schrijversconflict`.
- De `Fout` noemt de vorm die haar zet, niet het feittype, of de kop zegt dat het een feittype is.
- De sectie "Faults" van de README noemt elk type dat aan het fixpunt kan staan.
- Komt er later een type bij, dan noemt die sectie het ook.

## Acceptatie (bij oppakken aanscherpen)

- (a) De getuige hierboven pint het nieuwe type.
- (b) Een getuige laat zien dat elke `Fout` die de tak kan zetten een type uit de lijst van de README heeft. Dat kan als
  inventaris van de constructoraanroepen van `Fout`, of met een meting per vorm.
- (c) Alle gates groen.
- De distributie volgt bij haar volgende pinbump: haar soort `ANDERE` wordt dan een eigen soort.

## Omvang

Klein. May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`, de README en de tests.
