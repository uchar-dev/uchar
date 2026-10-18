# rename-frontend: de RegelSpraak-frontend in het Engels, behalve wat de distributie gebruikt

*Stap 4 van het programma "Engelse namen" (Vervolg in `backlog/done/20261017-engelse-namen-afspraak.md`). Vereist
`hernoemgereedschap` en de lessen van `hernoem-drools-backend` en `drools-word-fixes`. Raakt beschermde paden
(`scripts/`, en via de beslislijst `spec/` en `CLAUDE.md`): de branch blijft een voorstel tot de eigenaar merget.*

Goal: hernoem met `scripts/lib/naming/rename.py` de Nederlandse namen in `frontends/regelspraak` (de hoofdcode, de
tests, de grammatica behalve de RegelSpraak-sleutelwoorden), en de verwijzingen ernaar in `backends/drools`. Er
verandert geen gedrag, geen melding en niets op de draad.

Blijft staan: wat de distributie leest (`scripts/lib/naming/frozen/regelspraak.tsv`, met de parserregels), meldingen,
sleutels en waarden op de draad, v0-spellingen in weigertests, RegelSpraak-tekst, domeininhoud, corpuspaden en slugs.

Werkwijze (eigenaar, 2026-10-18): kleine Haiku-sessies voor het bulkwerk, kleine Sonnet-sessies voor de goede woorden,
een bouwer voor samenvoegen, toepassen, compileren en testen; geen lensronde. Voorbereiding vóór woensdag 2026-10-21
(bevroren lijst, vertaling, woorden, de tabel met `check`); toepassen daarna.

Acceptance:
- (a) `inventory frontends/regelspraak` meldt alleen bevroren namen.
- (b) `check` geeft 0 fouten; `verify` slaagt op de boom.
- (c) De code compileert; de suites van frontend en backend zijn groen met hetzelfde aantal tests; alle 16 gates groen.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

Result (2026-10-18):
- Commits: `89951ffa` (beslislijst met beslissingen: 187 `change`, de rest `keep`), `80cfad5a` (inhoud: `apply --decisions`,
  18.686 identifiers, 829 in commentaar, 187 beslissingen, 113 plekken buiten de zone met rust gelaten, 208 bestanden),
  daarna `.git-blame-ignore-revs`. Daarvoor `3f672e9f`..`95b0e313` (tabel, bestanden, gereedschap).
- Gemeten: javac van frontend, drools (main, test, source-route) en toka zonder fouten; frontend JUnit 1.199 tests groen;
  backend `mvn -o test` 1.619 tests, 0 fouten, 0 overgeslagen; alle 16 gates groen (`gates.sh`, failed=0); `verify` PASS
  (incomplete: no landing proof, weergaven en gate-logs niet gevraagd).
- Open punten:
  1. `inventory frontends/regelspraak` meldt nog 400 namen: de 230 parserregels en labels en de 170 Context-klassen van
     de ANTLR-grammatica (besluit (b), tot stap 6). Acceptance (a) is dus op de ANTLR-namen na gehaald.
  2. De bevroren lijst `frozen/regelspraak.tsv` houdt ook wat de zone zelf als spelling gebruikt: 16 namen die `toka/` leest,
     3 constanten van `ResolvedRegel.Soort` (RuleChecker zet `name()` in meldingen) en 23 constanten van `Aggregation.kind`,
     `BuiltinCall.kind` en `AllocationVersion.method` (`SpellingTest` leest `name()`). Draai `freeze` niet blind opnieuw;
     een kaart die de v0-namen in de enums door een expliciete tabel vervangt maakt ze weer hernoembaar.
  3. Het gereedschap kreeg twee aanpassingen met fixture (bestandsnaam die alleen in hoofdletters verschilt; constante via
     het type van het pakket); de README noemt ze.
  4. Namen die de woordenlijst voor Engels houdt maar Nederlands zijn: alleen een notitie.
  5. De markerhulp `onopgelost` heet `unresolvedMarker`, niet `unresolved` (`MarkerReasonPinTest` zoekt de aanroep in de bronnen).
  6. De 2.714 documentatierijen kregen `keep` zonder Haiku-ronde (besluit hoofdsessie 2026-10-18): een naam in proza zonder
     backticks kan nog de oude spelling hebben.
