# jackson-databind-kwetsbaarheden: jackson-databind van 2.21.2 naar 2.21.7

*Op verzoek van de eigenaar (2026-10-14), na de Dependabot-melding bij de push van `a63ef307`.*

Goal: de Drools-backend bouwt met een `jackson-databind` zonder de bekende kwetsbaarheden. GitHub meldt 16 open
Dependabot-waarschuwingen (6 high, 10 medium) op `com.fasterxml.jackson.core:jackson-databind` in
`backends/drools/pom.xml`, versie 2.21.2 (`jackson.version`), aangemaakt op 2026-10-12. Alle zestien zijn opgelost in
2.21.7, de laatste patch van 2.21: GHSA-cxp5-3px4-pw24 en GHSA-wv8q-qhhj-9h54 pas daar, de andere veertien in 2.21.4 tot
en met 2.21.6. Een patch binnen dezelfde minor is de kleinste wijziging die ze alle oplost.

Acceptance:
- (a) `jackson.version` is 2.21.7, en `mvn dependency:tree` in `backends/drools` toont elk jackson-artefact in de
  versie die bij 2.21.7 hoort, zonder een tweede versie ernaast.
- (b) De 16 gates zijn groen op een kopie van de tip; gate 12 speelt 255 goldens op beide takken.
- (c) Na de push zijn de 16 waarschuwingen gesloten (`gh api repos/uchar-dev/uchar/dependabot/alerts`).

May touch: `backends/drools/pom.xml`.

Out of scope: de toolchain (haar eigen pom en haar pin; de pin-bump naar `a63ef307` loopt in een andere sessie);
jackson 2.22.

## Result (2026-10-14)

`ba7b5f60` zet `jackson.version` in `backends/drools/pom.xml` van 2.21.2 op 2.21.7.

- (a) `mvn dependency:tree`: `jackson-databind` 2.21.7, `jackson-core` 2.21.7, `jackson-annotations` 2.21 (die
  versioneert sinds 2.20 alleen op minor); geen tweede jackson-versie. De shaded jar van de gaterun draagt
  `jackson-databind` en `jackson-core` 2.21.7 (`META-INF/maven/.../pom.properties`).
- (b) De 16 gates groen op een kopie van `ba7b5f60` (de boom `4fc504de`), 15:42:18–16:02:15; gate 12 649 s, 255
  gespeeld op beide takken met elke statusregel gelijk, JUnit 1574 tegen de vloer 1573. Een eerste run (15:34:35–15:41:40)
  was rood op gate 8, 11, 12, 13 en 15: de gates bouwen offline (`mvn -o`) en de jars van 2.21.7 stonden nog niet in de
  lokale Maven-repository, omdat `dependency:tree` alleen de pom's ophaalt. Wie de versie van een afhankelijkheid
  verhoogt, haalt de jars eerst online op (`mvn -f backends/drools/pom.xml dependency:resolve`).
- (c) Volgt na de push: GitHub sluit de waarschuwingen bij de volgende scan van de standaardbranch.

Voor de distributie: `scripts/check-pin.sh` van regelspraak-toolchain vergelijkt haar `jackson.version` met die van
`backends/drools/pom.xml`. Een pin op deze of een latere commit vraagt 2.21.7 in de toolchain-pom; de sessie van de
pin-bump weet dat en pint op de landingscommit van deze kaart.
