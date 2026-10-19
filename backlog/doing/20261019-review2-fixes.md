# review2-fixes

*Op verzoek van de eigenaar (2026-10-19), na de tweede eindreview van het programma "Engelse namen" (bevinding M3, M5, M6, L9, L10 en de punten 1, 5, 10, 11, 12 van de vorige review).*

Goal: de namen die het hernoemen nog miste zijn Engels, en documentatie en commentaar noemen alleen namen die bestaan. Er verandert geen gedrag.

## Punten

1. Gemiste identifiers in Java, OCaml, Python en shell (`huidigSubject`, `oud`, `kern`, `neem`, `huidig`, `noteer*`, `opgegeven`, `category` als parameter van een `Kind`, de Python van gate 13, `tekst` in de grammatica), met een kleine scan naar meer.
2. Testdumps die Nederlandse labels schrijven worden Engels; de pinnende verwachting volgt.
3. Documentatie en commentaar: Drools-README, ASP-predicaten `role`/`inputrole`, `{@code}` naar `{@link}` of de juiste naam, verwijzingen Java naar OCaml, CONSTRUCTS.
4. `Inconsistentie` en `Segment` in "Not renamed", javadoc en `toString` van de generator noemen de echte klasse.
5. `.git-blame-ignore-revs`: "wordt", geen pad buiten de repository.
6. De naamregel vindbaar in `README.md`; Conventions aangevuld.

## Acceptance

- Frontend en Drools compileren (main, test, source-route), suites groen, alle gates groen op de eindstand.
