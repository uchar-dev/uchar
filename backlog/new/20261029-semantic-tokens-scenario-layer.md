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
