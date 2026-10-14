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
