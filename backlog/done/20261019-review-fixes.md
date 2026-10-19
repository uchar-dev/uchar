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

## Result

Gebouwd op main `9fdc291ae`, branch `card/review-fixes`; niet gepusht. Commits: javadoc `0412ddca6`, teksten `106843788`, documentatie
`0224c8aa7`, hernoemingen `d17df0398` (category), `350519feb` (van), `1b7526f51` (gemiste namen) en herstel `77fac1ea9`; alle vier in
`.git-blame-ignore-revs`. Oud naar nieuw voor de distributie: `werk/engelse-namen/na/docs/MAPPING.tsv` (25 rijen). Gereedschap van de
controles: `werk/engelse-namen/na/docs/tool/` (`refcheck.py`, `ticks.py`, `stale.py`), niet in de repo.

**1. Javadoc.** `-Xdoclint:reference` over frontend en Drools (main, test, source-route, toka) vond 49 + 72 fouten: ongeveer 90
`@param`-namen van oude componenten (`ir/*`, `RulePlan`, `Fixpoint`, `ScenarioResult`, ...) en ruim 25 `{@link}`-verwijzingen
(`Rational.DelingDoorNul` is `Breuk.DelingDoorNul`, `DroolsBackend#read` is `fixpointOf`, `membershipOpslag`, `Reads#carrier`, een
niet-bestaande `Route`/`ScenarioRunner`). Alle rechtgezet. **Doclint staat blijvend aan**: `DOCLINT` in `frontends/regelspraak/Makefile`
(main en test) en `compilerArgs` in `backends/drools/pom.xml`. Daarvoor werden zeven `{@link}`s uit de frontend naar resolver- en
testklassen `{@code}`, omdat de Drools-bouw de resolver uitsluit en `-Xdoclint:reference` die anders via het sourcepath binnenhaalt.
Geen gate brak.

**2. toString en exceptieteksten.** 26 `toString`-methoden van de Drools-feiten (+ `Inconsistentie[` in de gegenereerde bean) noemen de
huidige klasse en veldnamen; 61 `requireNonNull`-namen zijn de parameternaam; de exceptieteksten van `DroolsBackend`/`TmsTranspiler`/
`Watch` noemen `fire`, `fixpointOf`, `setDerivedValues`, `insert`, `CaseInput.of`, `Input`. Meldingsproza uit de catalogus onaangeroerd.
Tests die mechanisch volgden: `TmsRepeatedFireWitnessTest`, `TmsInputWitnessTest`, `ComparisonWitnessTest`, `CaseInputWitnessTest`.

**3. Documentatie.** `refcheck.py` toetst `Type.lid` en `corpus/`-paden, `ticks.py` backtick-namen in md. Rechtgezet: README van de
Drools-backend (`vuur`/`lees`/`dekking`/`vergelijk`/`GEVUURD`/`NIET_GERAAKT`, `TmsFilterErrorWitnessTest`), IR-SPEC (`ExpressieBouwer`,
`heeftLid`, `eenheidOf`, `HerkomstOpDeDraadTest`), CONSTRUCTS (vier testklassen/matchers), RUNTIME-SEMANTICS, DIVERGENCES, 12 NOTE's
(`corpus/red/...` naar `corpus/golden/...`, `wf044-unresolved-*`, vervallen slugs zonder zonepad), scripts (`GraphProjection`,
`BranchDumpsCorpusWitnessTest`). Blijft staan met reden: paden van verwijderde getuigen in de verleden tijd (`wf091-dimensiecel-eenheid`,
`wf053-bindingless-ambiguous`, `wf034-dagsoorteenheid-...`), v0-/wire-namen, `spec/proposals/`, `backlog/done/`. De controle zit niet in
een gate.

**4. Gemiste namen.** `category` naar `kind` in 13 recordcomponenten van een soort (`Read`, `Aggregation`, `Outcome` x2, `Form` x2,
`Origin`, `CoverageRule`, `ResolvedRule`, `Predicate`, `Expectation`, `Designatable`; ook de DRL-eigenschap van `Outcome`);
`van(...)` naar `of(...)` bij 13 fabrieksmethoden (niet `Breuk.van`, runtime) met 237 aanroepen; `Stapels`/`Stacks`, `ALTIJD`,
`Kandidaat`, `verklaard`, `hersteld`, `citeer`, `opgegeven()`, `houder`, `van` (Restate, Edge, Period, UnitConflict), OCaml `goed`,
shell `drl_kosten`/`RAKERS`. Bewust gelaten: `Modelplan`, `Namen` (en de runtime), domeinwoorden in tests, DRL-variabelen `$houder`
(uitvoer van het plan), de param-sleutel `category` van meldingscodes.

**Gates.** Frontend 1210 tests groen; Drools-suite 1624 tests, 0 mislukt, na herstel van de DRL-tekst (`Breuk.van` in gegenereerde tekst) en
de meldingsproza; alle 16 gates groen op `0c7559478` (`werk/engelse-namen/na/docs/gates-1.summary.txt`). Protected paden geraakt, dus een
voorstel dat het maintainer-oordeel nodig heeft: `scripts/check-drools.sh`, `check-graph.sh`, `check-reftype-derivation.sh` (namen in
meldingen en commentaar), `spec/IR-SPEC.md`, `RUNTIME-SEMANTICS.md`, `CONSTRUCTS.md`, `spec/verifier/lib/checks.ml` (commentaar).
