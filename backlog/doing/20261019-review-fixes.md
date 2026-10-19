# review-fixes: namen in documentatie, javadoc en teksten rechtgezet; gemiste namen hernoemd

*Op verzoek van de eigenaar (2026-10-19), na de eindreview van het hernoemprogramma (bevinding 1, 5, 6, 10, 11).*

Goal: documentatie, javadoc, `toString` en exceptieteksten noemen alleen namen die bestaan, en de namen die het
hernoemen miste zijn Engels. Er verandert geen gedrag.

## Punten

1. Javadoc: `-Xdoclint:reference` over frontend en Drools (main en test); `@param`/`{@link}` rechtgezet; blijvend aan in de build
   als dat niets breekt.
2. `toString` en exceptieteksten met een oude klassen- of methodenaam; pinnende tests volgen mechanisch. Cataloguteksten blijven.
3. Documentatie: `Type.lid`-verwijzingen en `corpus/`-paden die niet bestaan, getoetst met een klein script.
4. Gemiste namen (`Stapels`, `ALTIJD`, `Kandidaat`, `hersteld`, `goed`, ...), het veld `category` wordt `kind`, de fabrieksmethoden
   `van(...)` worden `of(...)`; oud naar nieuw in `MAPPING.tsv` voor de distributie.

## Wat blijft

RegelSpraak-sleutelwoorden, ANTLR-namen, cataloguteksten, v0-namen waar ze als v0-naam staan, `backlog/done/`, het gedrag.

## Acceptance

- Doclint op `reference` is schoon.
- De suites en alle gates van `CLAUDE.md` groen op de eindstand (één volledige run).
