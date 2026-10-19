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

## Result

Inhoudscommit `42cd398c3` (355 bestanden, 8241 regels naar 8241), daarna `172a2b632` en een fix voor de berichtsleutels.

- **Tabel `model`**: 129 rijen, 75 vertalingen van een Nederlands woord of samenstelling (`naam` naar `name`, `waarden`
  naar `values`, `objecttypen` naar `objectTypes`, `Numeriek` naar `Numeric`, `GEEN` naar `NONE`, de enumconstanten van
  `FormKind` en `Consequence`) en 54 verbeterde namen (testmethodenamen in volzinnen; velden die een nieuwe betekenis
  kregen, zoals `regel` naar `line` of `rule` naar gelang het scope-type). `check` gaf 0 fouten.
- **Beslissingen over de rest** (8708 voorkomens buiten de declaraties): 940 veranderd (`subject` 283, `name` 179,
  `rule` 88, overige 390), 7739 bewust gelaten: Nederlandse meldingen, testteksten en commentaar die RegelSpraak of een
  Nederlandse bron citeren.
- **Testbestanden die mechanisch volgden**: 236 testbestanden in `backends/drools` en `frontends/regelspraak`, 4 onder
  `backends/drools/src/source-route`; de toka-tests apart: `toka/src/test/java/.../CasusTest.java` en `ModelTest.java`
  (plus `toka/README.md`). Geen verwachting over gedrag veranderde; de 47 TOKA-fragmenten zijn ongemoeid.
- **Handwerk na de inhoudscommit**: berichtsleutels `PART.form-named.<soort>` (de sleutel is de kleine-letterspelling
  van de `FormKind`-constante met `-` voor `_`, zoals gate 12 het patroon eist), de bean-eigenschap `getRule` van de
  gegenereerde inconsistentiebean, `CLAUDE.md` (Conventions) en `toka/README.md`.
- **Wat Nederlands bleef**: parserregels en labels met wat ANTLR eruit maakt, RegelSpraak-sleutelwoorden en tokens,
  enumwaarden in v0-spelling die via `name()` in meldingen of dumps komen (`FormKind.REGEL`), `Benoembaar.Soort` en
  predicaten, de productieruntime en de TOKA-fragmenten; de volledige lijst staat in `CLAUDE.md`, Conventions.
- **verify**: PASS op de inhoudscommit (de boom is de basis `bd0f25161` met de tabel en zijn beslissingslijst; de
  renderings- en gatelogleg niet gedraaid, dus geen landingsbewijs). De hash staat in `.git-blame-ignore-revs`.
- **Mapping voor de distributie**: `werk/engelse-namen/na/unfreeze/MAPPING.tsv`, 168 rijen (publieke en protected
  types en leden van `frontends/regelspraak` en `backends/drools`) in de vorm van `javac_follow.py`.
- **Gates**: eerste run: 15 van 16 groen, gate 12 rood op de berichtsleutels (underscore niet toegestaan in
  `check-drools-codes.sh`); hersteld. Suites: frontend 1210 tests, Drools 1624 tests, 0 fouten of overgeslagen.
