# semantic-tokens-scenario-layer: de semantic tokens dekken ook een toetsdocument van de scenariolaag

*Op verzoek van de eigenaar van de regelspraak-werkbank (2026-10-29). Gemeten met de toolchain-pin van de werkbank
(`6cdc893a`, uchar `5e327c95`).*

## Aanleiding

De werkbank toont op haar eval-pagina het scenariodocument dat haar replay samenstelt: een `Parameterset`, een of meer
`Situatie`-blokken (ook met `gebaseerd op de situatie …`) en per vraag een `Scenario` met `Met als verwacht resultaat:`.
De kleuring van dat document komt uit `Resolver.tokens`. Die leest alleen een `regelSpraakDocument`; DIVERGENCES.md
("Semantic tokens", **Not covered**) zegt: "A test document of the scenario layer (`--cases`, `ScenarioResolver`): its
scenario blocks are no part of `regelSpraakDocument`, so they are a syntax error here."

Gevolg in de werkbank: boven elk scenariodocument staat `ERROR RGS-091 r. 3: syntaxfout: extraneous input
'Parameterset' …`, en de namen in de situaties en verwachtingen (objecttypen, attributen, rollen, labels, parameters)
krijgen geen soort en dus geen kleur. Het document is goed: `bin/regelspraak scenario` draait het en de toetsen slagen.
Een lezer ziet een rode ERROR bij een document dat niets mankeert.

## Doel

`Resolver.tokens` (en `Resolver --tokens`, dus `bin/regelspraak tokens`) dekt ook een toetsdocument van de
scenariolaag, zoals `--cases` het leest (`ScenarioResolver`):

- geen syntaxfout voor `Parameterset`, `Situatie` en `Scenario`; de diagnoses zijn die van `--cases`;
- spans met een soort voor de namen daarin: het objecttype na `een`, attribuut-, rol- en kenmerknamen in de bullets,
  `de parameter …`, de namen in de verwachtingen, de namen in de scenariokop (situatie, parameterset, de regels of de
  regelgroep van de scope) en de labels;
- een naam in een situatie of verwachting die niets noemt, is `unresolved`, zoals in de regeltekst;
- de dekking ("every token … lies in exactly one span") geldt ook voor deze documenten.

Een keten zonder zo'n blok blijft gelezen zoals `--ir` hem leest, en de uitvoer daarvoor verandert niet. Vorm, soorten
en de plaats in DIVERGENCES.md bepaalt de bouwer van deze kaart; "Not covered" vervalt of wordt kleiner.

## Acceptatie

- (a) Rood eerst: in `SemanticTokensTest` een toetsdocument (een bestand met declaraties en een regel, een tweede met de
  scenariolaag) met een parameterset, twee situaties (een met `gebaseerd op`), een scenario met scope, peildatum,
  parameterset, basis en inline situatie, verwachtingen van elke vorm uit de tabel "case expectation" (DIVERGENCES.md,
  Scenariotoetsen) en één naam die niets noemt. Vóór de bouw een syntaxfout; daarna geen, elke naam met zijn soort en
  `declaredAt`, de ene naam `unresolved` met de diagnose die `--cases` geeft, en de dekking van `SemanticTokensCheck`.
- (b) `SemanticTokensCorpusTest`: elke `corpus/golden/*/scenario.rgs` met haar `source.rgs` als één toetsdocument, met
  de eisen van een productiedocument (dekking, elke verwijzing gekoppeld, geen `unresolved` zonder diagnose, geen
  `word` zonder syntaxfout) en zonder ERROR; vloeren op het aantal documenten en op de nieuwe soorten.
- (c) De getuigen van de productieroute (`SemanticTokensTest`, `SemanticTokensCorpusTest`, `SemanticTokensRecoveryTest`,
  de TOKA-getuige van gate 11) blijven groen zonder dat hun verwachtingen veranderen.
- (d) Het scenariodocument van de werkbank voor s01 (`scenariodocument` uit `eval/runner/cli.py` van de werkbank,
  regelset `v2/herleving-ww`) met de regelbronnen door `Resolver --tokens`: geen ERROR en geen `unresolved`. Gemeten
  buiten de suite; het materiaal van de werkbank komt niet in deze repository.
- (e) `make gates` groen.

## May touch

`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/` (`Resolver`, `ScenarioResolver`,
`SemanticTokens`), `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/SemanticTokens*.java`,
`frontends/regelspraak/grammar/DIVERGENCES.md` (de sectie "Semantic tokens"), deze kaart.

## Out of scope

De werkbank zelf: zij toont wat de tokens geven; nieuwe soorten opnemen in `SOORTEN` (`werkbank/static/kleuren.js`) en
de css doet de werkbank-sessie. Na de landing volgt de pin-bump van uchar in de toolchain (deze sessie) en daarna die
in de werkbank. `bin/regelspraak tokens` verandert niet van vorm.

## Result (2026-10-29)

Geland op tak `card/semantic-tokens-scenario-layer` (`9578a157`): `Resolver.tokens` en `Resolver --tokens` lezen een
keten met een blok van de scenariolaag als toetsdocument, zoals `--cases` hem leest. Een keten zonder zo'n blok wordt
gelezen als voorheen. DIVERGENCES.md, "Semantic tokens": een alinea "A test document", de vier soorten in de lijst en
de LSP-tabel, "Not covered" houdt alleen de zin over de tekst van een benaming.

De keuzes die de kaart aan de bouwer liet:

- **Herkennen, geen vlag.** Een keten is een toetsdocument als er een token `Situatie`, `Parameterset` of `Scenario`
  op het standaardkanaal in staat (`ScenarioResolver.opensScenarioBlock`). Geen van de drie is een woord van
  `regelSpraakDocument`, dus een productiedocument draagt er geen; op de productieroute is zo'n keten een syntaxfout.
  Daardoor veranderen `bin/regelspraak tokens` en de aanroep van de werkbank niet, alleen de pins.
- **De diagnoses van `--cases`.** Boom, model en diagnoses komen uit `ScenarioResolver.read` (nieuw; `resolve` loopt
  erdoorheen): een syntaxfout is RGS-216, de scenarioblokken geven hun eigen ERRORs, een toetsdocument zonder scenario
  RGS-095 (nu op één plek, `Resolver.requireScenario`, ook voor `casesMode`), en de WARNINGs van de emitter gaan over
  het model met de scope van het eerste scenario, zoals `--cases` het schrijft.
- **Vier soorten**: `label`, `situation`, `parameterSet`, `scenario` (LSP: `variable`, `namespace`, `namespace`,
  `function`). De scope van een scenario verwijst als `namedForm` of `ruleGroup`.
- **Labels** volgen de samenvoeging van de scenarioroute: per situatie of scenario de bases, in geschreven volgorde
  uitgevouwen (één keer per naam berekend, zodat een kring van bases geen tegenstrijdige volgorde geeft), dan het eigen
  blok. De eerste opener van een label is zijn declaratie; een latere opener, een rolwaarde en een label in een
  verwachting verwijzen ernaar. Een label dat de samenvoeging niet opent, is `unresolved`.
- **Namen in een bullet** met de opzoeking van de scenarioroute, gedeeld in plaats van nagebouwd:
  `ObjectType.attributeName`, `CharacteristicMatcher.find`, `ScenarioResolver.rolesNamed` (uit `role` gehaald) en
  `ScenarioResolver.parameterByName` (statisch gemaakt). Eén lidwoord vooraan blijft keyword, zoals de scenarioroute
  het afknipt; `geen`/`niet` voor een kenmerk en `volgens` zijn keywords.

Acceptatie:

- (a) `SemanticTokensScenarioTest`, zes tests. Rood vóór de bouw (met alleen de vier soorten in `Kind`): zes van zes
  falend, met twee RGS-091 op `toets.rgs:1`, zoals de werkbank ze zag. Erna groen. De diagnoses van de tokens zijn die
  van `ScenarioResolver.resolve` over dezelfde keten, hier precies de ene RGS-236 van de naam die niets noemt.
- (b) `SemanticTokensCorpusTest.every_golden_test_document_is_covered_and_resolved`: 36 toetsdocumenten, 18871 spans,
  geen enkel probleem (dekking, elke verwijzing gekoppeld, geen ERROR, geen `unresolved`, geen `word`). Vloeren,
  gemeten 2026-10-29: `MIN_TEST_DOCUMENTS=36`, `label` 1558, `situation` 6, `parameterSet` 8, `scenario` 126.
- (c) De getuigen van de productieroute ongewijzigd groen; de corpusgetuige van de productieroute meet files=142,
  spans=21996, met haar vloeren onaangeroerd. Geen productiebron in `corpus/golden` of `toka/casus` draagt een van de
  drie woorden. Frontend-suite: 1318 tests, 0 overgeslagen, 0 gefaald.
- (d) Werkbank s01, gemaakt met `scenariodocument` uit haar `eval/runner/cli.py` (dataset van 2026-10-29 17:07,
  regelset v2/herleving-ww, 33 regelbronnen, 199 regels), met de regelbronnen door `Resolver --tokens`. Vóór: één
  ERROR RGS-091 (`s01.rgs:4`) en 406 spans `word`. Na: 0 diagnoses, 0 `unresolved`, 0 `word`; in `s01.rgs` onder meer
  101 `label`, 68 `attribute`, 44 `objectType`, 41 `role`, 9 `characteristic`, 7 `parameter`, 6 `situation`, 4
  `parameterSet` en 3 `scenario`.
- (e) `make gates` op `9578a157` (de tak op de tip van main): alle zeventien gates groen, `after-run` groen, 403 s
  (run `20261029-171115`, `red=0`).

Geen IR-wijziging, dus geen rij voor `spec/CONSTRUCTS.md` en geen tekst voor `spec/PLAN.md`.

Voor de werkbank (niet in deze kaart): de vier soorten in `SOORTEN` (`werkbank/static/kleuren.js`) en de css, anders
meldt zij ze als onbekend; de opmerking in `KleurMeldingen` dat de tokens de scenariolaag niet lezen, vervalt.
