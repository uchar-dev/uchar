# unfreeze-model: het kernmodel en de Drools-feiten worden Engels

*Vervolg op het programma "Engelse namen" (besluit van de eigenaar 2026-10-19 naar aanleiding van bevinding 2 en 11
van de eindreview). Raakt `frontends/regelspraak`, `backends/drools` (buiten de productieruntime), de twee tests onder
`toka/src/test/java` en `CLAUDE.md`; mandaat van de eigenaar.*

Goal: het hernoemgereedschap bevroor namen per spelling. Een naam bleef Nederlands omdat een test onder
`toka/src/test/java` hem las, omdat hij gelijk was aan een parserregel of een v0-spelling, ook als het een gewoon
Java-veld was (`objecttypen`, `Fixpoint(waarden, kenmerken, ...)`, `String naam` 410 keer). Die namen worden
alsnog Engels. De twee tests onder `toka/src/test/java` zijn eigen werk en volgen mechanisch; alleen de 47
TOKA-fragmenten blijven letterlijk.

Wat Nederlands blijft:
- de parserregels en labels van de grammatica en wat ANTLR eruit maakt (`XxxContext`, `ctx.xxx()`, `visitXxx`);
  een gewoon Java-veld of record dat toevallig zo heet wordt Engels;
- RegelSpraak-sleutelwoorden en hun tokens;
- enumwaarden die zelf de v0-spelling zijn en via `name()` in meldingen, dumps of de draad komen;
- waarden die de distributie in JSON laat zien en die uit RegelSpraak komen (`Benoembaar.Soort`, predicaten);
- de productieruntime en de 47 TOKA-fragmenten.

Acceptance:
- (a) Het hernoemgereedschap meldt voor beide zones alleen nog de namen die hierboven blijven staan.
- (b) Frontend, Drools (main, test, `src/source-route`) en `toka/` compileren; hun suites slagen.
- (c) `CLAUDE.md` (Conventions) somt de categorieën van wat Nederlands blijft volledig op en zegt dat de tests onder
  `toka/src/test/java` een hernoeming volgen.
- (d) `verify` bevestigt de inhoudscommit; die staat in `.git-blame-ignore-revs`.
- (e) Alle 16 gates groen op de eindstand.

Gate set: alle gates van `CLAUDE.md`, groen op de eindstand van de branch.
