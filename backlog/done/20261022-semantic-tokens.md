# semantic-tokens: de frontend geeft per stuk brontekst een soort en een positie, naast het IR

*Besluit van de eigenaar 2026-10-22 ("ik wil dit"), na de vraag of uchar genoeg handvatten biedt om de regels in een
afnemer met kleur te tonen: een naam gekleurd naar wat hij noemt (objecttype, attribuut, rol, parameter,
domeinwaarde), een onopgeloste naam in rood. Richting door de hoofdsessie. De directe afnemer is de werkbank van de
regelset herleving-WW, via de distributie; de uitvoer is algemeen en kent die afnemer niet.*

## Aanleiding

Wat er nu is, gemeten op main `680e5263`:

- **De lexer** (`grammar/RegelSpraakLexer.g4`) geeft sleutelwoorden als tokens, ook woordgroepen
  (`WORDT_BEREKEND_ALS` is "moet berekend worden als"), literals, datums, bullets en leestekens. Commentaar staat op
  het HIDDEN-kanaal (`LINE_COMMENT`, r. 501), witruimte wordt overgeslagen. Namen splitst hij in losse woorden
  (`UPPER_ID`, `LOWER_ID`, kopnoot 2); de parser zet er een naam van meer woorden van, en wat een naam noemt, weet pas
  de resolutie.
- **De draad** draagt per knoop `source.snippet`, en `line` alleen op een consistentieversie en een regelgroep; het
  schema laat `file` en `column` niet toe ("until the card that first writes positions"). Een `Diagnostic` draagt
  alleen een regel. Terecht: het IR is de abstracte vorm (beslistabellen ontsuikerd, gegenereerde namen,
  defaults-groepen), geen weergave van de tekst.
- **Het model in het geheugen** heeft per knoop `ir.Source(file, line, column, snippet)`, maar per knoop en niet per
  naam, en niet naar buiten.
- **De CLI** kent `--ir` en `--cases`. Meerdere bestanden leest hij als één keten tekst; `casesMode` rekent een regel in
  de keten terug naar bestand en regel (`firstLines`).

Een afnemer die een naam naar zijn soort wil kleuren, of een onopgeloste naam wil aanwijzen, heeft dus geen bron. Hij
kan het IR niet gebruiken (geen posities, en een gelowerde vorm is niet de tekst), en de soort van een naam zelf
afleiden zou de resolutie van de frontend nabouwen.

## Doel

Een uitvoer van de frontend naast het IR, niet op de draad, zoals de diagnosecodes (`Diagnostic(severity, code,
params, …)`, kaart `frontend-codes`): per gelezen bestand een lijst spans die de tekst dekt.

- **Dekking.** Elk token van de lexer, op het standaardkanaal en op HIDDEN, valt in precies één span; spans overlappen
  niet; witruimte valt erbuiten. Een naam van meer woorden is één span, ook als hij over een regeleinde loopt.
- **Positie.** Begin en eind van de span: regel (vanaf 1, in het eigen bestand, zoals de diagnostics) en kolom (vanaf
  0, in UTF-16-code-units: de standaardcodering van posities in het Language Server Protocol en de eenheid van een
  JavaScript-string, zodat een browser en een language server de waarde zonder omrekening gebruiken). ANTLR telt
  kolommen in code points; buiten het BMP verschillen die twee.
- **Lexicale soorten**, zonder resolutie: sleutelwoord, commentaar, getal, datum, tekstliteral, leesteken (bullets,
  `;`, `.`, `:`, haakjes). De bouwer kiest de indeling en documenteert haar.
- **Naamsoorten**, uit de resolutie: objecttype, attribuut, kenmerk, rol, feittype, parameter, domein, domeinwaarde,
  eenheid, dimensie en dimensielabel, regelnaam, variabele (`Daarbij geldt:`), en wat de bouwer verder tegenkomt; plus
  `unresolved` voor een naam die niets bindt. De namen van de soorten volgen de naamregel van `CLAUDE.md` (de draad
  waar die een naam heeft: `objectType`, `characteristic`, …). Op de plaats van de declaratie draagt de span
  `declaration: true`; een verwijzing draagt de positie van de span van haar declaratie (bestand, regel, kolom), als die
  in de gelezen bestanden staat.
- **Diagnostics in dezelfde uitvoer**, met code, parameters, tekst, bestand en regel in dat bestand, zodat een afnemer
  één document leest.
- **Een tabel** in de documentatie geeft per soort het semantic-token-type van LSP 3.17 dat er het dichtst bij ligt
  (bijvoorbeeld objecttype → `class`, attribuut → `property`, parameter → `variable` met `readonly`, domeinwaarde →
  `enumMember`, sleutelwoord → `keyword`), zodat een latere language server de uitvoer kan doorgeven. Een language
  server zelf hoort niet bij deze kaart.
- **Toegang.** Een publieke ingang in Java naast `Resolver.resolve`: meerdere bestanden met naam en tekst erin, spans
  per bestand en de diagnostics eruit; de distributie roept die aan. Daarnaast een CLI-modus `Resolver --tokens
  <out.json> <file.rgs…>`. Hij schrijft ook bij ERRORs en syntaxfouten, want dan is de uitvoer het nuttigst: exit 0 als
  het bestand geschreven is, 2 bij verkeerd gebruik. Wat een token krijgt dat buiten een ontlede naam valt na een
  syntaxfout (zijn lexicale soort of `unresolved`), kiest en documenteert de bouwer.
- **Het IR verandert niet**: `--ir` en `--cases` blijven bytegelijk.

Niet in deze kaart: een testdocument van de scenariolaag (`--cases`, `ScenarioResolver`) als invoer, tenzij het met
weinig code meekomt (anders als open punt in de Result); de distributie (subcommando, endpoint, pin-bump) en de
afnemer, elk met een eigen kaart.

## Acceptatie

- (a) Rood eerst: een JUnit-getuige op een eigen Jutterswet-model met elk soort naam, een naam over twee regels, een
  onopgeloste naam, een `// Bron:`-commentaar, twee bestanden met een verwijzing over de bestandsgrens heen, en een
  teken buiten het BMP in een commentaar vóór een naam op dezelfde regel (de UTF-16-kolom).
- (b) Een corpusgetuige in de JUnit-suite, over elke `corpus/golden/**/source.rgs` (128 op `680e5263`) en de 47
  `toka/casus/toka-*.rgs` samen als één document: de dekking (elk token in precies één span, geen overlap), de tekst
  op elke span is de brontekst op die positie, elke verwijzing wijst naar een span met `declaration: true` van dezelfde
  soort, en een document zonder diagnostics heeft geen span `unresolved`. Een uitzondering daarop staat in een lijst
  met reden die faalt als ze verouderd is. Vloeren in de test (bestanden, spans, naamspans per soort), gemeten bij de
  bouw.
- (c) Als meting, niet als getuige en zonder bestand in deze repository: de v2-regelset herleving-WW (fundament en
  rules, alleen lezen) geeft volledige dekking en geen `unresolved`; de uitkomst en de telling per soort in de Result.
- (d) De vorm van de uitvoer, de soorten, de positieconventie en de LSP-tabel staan in `grammar/DIVERGENCES.md`, bij de
  beschrijving van de CLI (r. ±2996) of in een eigen sectie.
- (e) De zeventien gates groen; gate 10, 11 en 16 tonen dat `--ir` en `--cases` bytegelijk bleven.

May touch: `frontends/regelspraak/**`, deze kaart. Geen schema, geen spec, geen gate-script.

## Result (2026-10-22)

Tak `card/semantic-tokens`, vanaf `d6015ecf`. Geraakt: `frontends/regelspraak/**` (een nieuwe klasse, de ingang in
`Resolver`, zeven wachten in de resolutie, de kale naam in `ExpressionBuilder`, vijf testklassen, een sectie in
`grammar/DIVERGENCES.md`) en de kaart. Niet geraakt: het IR-schema, de spec, de gate-scripts, `CLAUDE.md`,
`spec/PLAN.md`, `spec/CONSTRUCTS.md`, `toka/`.

- `13c35864` de kaart naar `doing/`.
- `e2f9aba9` de pas, de ingang `Resolver.tokens`, de CLI `--tokens`, de getuige van (a).
- `196a8392` de corpusgetuige van (b); de soort van een naam volgt zijn plaats in de zin.
- `4cbb106e` zeven wachten in de resolutie op ANTLR's herstel; de herstelgetuige.
- `a84081e5` de documentatie (d); de vloeren van de corpusgetuige.
- `f39bfe9b` vondst buiten de kaart: een kale naam die niets noemt, is een marker met een WARNING (`BareNameTest`).

### Wat er is

**Ingang.** `Resolver.tokens(List<SemanticTokens.SourceFile>)`: bestanden met naam en tekst erin,
`SemanticTokens.Document` eruit, met per bestand de spans in bronvolgorde en de diagnostics; `Document.toJson()`
schrijft de JSON. De bestanden worden één keten zoals `--ir` ze leest (elke tekst plus een regeleinde) en één keer
geresolved; een verwijzing mag over de bestandsgrens. `resolve(source, fileName)` is ongewijzigd van gedrag: het parsen
staat nu in een eigen `parse`, dat `resolve` en `tokens` delen.

**CLI.** `Resolver --tokens <out.json> <file.rgs…>`. Schrijft ook bij een ERROR en een syntaxfout; exit 0 als het
bestand geschreven is, 2 bij verkeerd gebruik (geen uitvoer of bron, of `--tokens` naast `--ir`/`--cases`). De
diagnostics gaan ook naar stderr, elk met bestand en regel. `--ir` en `--cases` zijn bytegelijk op elke golden en elk
TOKA-fragment (gate 10, 11, 16); anders alleen waar `--ir` crashte of een IR schreef dat de verifier weigert (de twee
vondsten onderaan).

**Vorm**, de regel `indien zijn leeftijd kleiner is dan de volwassenleeftijd.` uit de getuige (twee bestanden):

```json
{"line": 5, "column": 8, "endLine": 5, "endColumn": 14, "kind": "keyword", "text": "indien"},
{"line": 5, "column": 15, "endLine": 5, "endColumn": 19, "kind": "keyword", "text": "zijn"},
{"line": 5, "column": 20, "endLine": 5, "endColumn": 28, "kind": "attribute", "text": "leeftijd", "declaredAt": {"file": "jutters.rgs", "line": 22, "column": 7}},
{"line": 5, "column": 29, "endLine": 5, "endColumn": 36, "kind": "keyword", "text": "kleiner"},
…
{"line": 5, "column": 47, "endLine": 5, "endColumn": 64, "kind": "parameter", "text": "volwassenleeftijd", "declaredAt": {"file": "jutters.rgs", "line": 18, "column": 13}},
{"line": 5, "column": 64, "endLine": 5, "endColumn": 65, "kind": "punctuation", "text": "."}
```

Het document is `{"files": [{"file", "spans": [...]}], "diagnostics": [{"severity", "code", "params", "message",
"file", "line"}]}`. Een declaratie draagt `"declaration": true`, een verwijzing `declaredAt` (bestand, regel, kolom van
de span van de declaratie), een naam van een lid dat de frontend maakt (de opgavemarkering, §7) `generatedFrom` (de
declaratie waar het uit komt). Regel vanaf 1 in het eigen bestand, kolom vanaf 0 in UTF-16-code-units, einde exclusief.

**Dekking.** Elk token van de lexer, standaardkanaal en HIDDEN, ligt in precies één span; geen overlap; witruimte
erbuiten, ook aan de randen van een token (de vervolgrij van een tabel begint met een regeleinde). Een naam van meer
woorden en een sleutelwoordfrase zijn één span, ook over een regeleinde; een commentaar binnen een naam splitst die in
twee spans van dezelfde naam. Een tabelrij (één token) wordt gesplitst zoals de resolutie haar splitst: pipes,
scheidingsstreepjes en `n.v.t.` elk één span, elke andere cel opnieuw gelexed en als regeltekst gelezen. Een teken dat
de lexer niet leest (een losse `_`) is geen token en ligt in geen span; de syntaxfout zegt het.

### Ontwerp en waarom

Een pas na de resolutie over de token stream en de parse tree (`resolver/SemanticTokens.java`), geen recorder in de
resolutie. De resolutie bindt een naam op zijn tekst: een `kaleNaam` wordt opnieuw gesplitst tegen het model
(`attributeRef`: de keten "X van de Y van de Z", meervoud, `in <eenheid>`, een geabsorbeerd `geen`), een kenmerk van
meer woorden wordt uit twee parseknopen samengesteld, en veel opzoekingen proberen een lezing en vallen terug. Een
recorder zou tokenposities door al die stringbewerkingen moeten dragen. De pas leest de tekst die er staat en vraagt
het afgemaakte model wat elke woordreeks noemt, met de opzoekingen van de resolutie (`objectTypeByName`,
`attributeName`/`declarantOf`, `PluralMatcher`, `CharacteristicMatcher`, `roleNameMatches`, de opzoekingen van
dimensie, tijdlijn, dagsoort en eenheid). In drie lagen:

1. declaraties uit hun parseknoop (de naam zoals het model hem sleutelt);
2. verwijzingen die de parse plaatst: een objecttype in een kop, een domein als datatype, een eenheidsexpressie, een
   tijdlijn, dimensie en labels in een selectie, een vorm (`regelversie`, `Uitzondering op`), een enumeratiewaarde,
   de vocabulaire van een benaming;
3. regeltekst: per woord de langste reeks die het model declareert. De soort volgt de plaats zoals de resolutie die
   leest: de eerste stap van een navigatie ("DE LEEFTIJD van de Jutter") eerst een lid, een stap erbinnen ("van DE
   MAST van de post") een rol, het onderwerp waar zij eindigt en een naam na `een`/`alle`/een telwoord eerst een type,
   elders eerst een variabele (die een parameter overschaduwt), dan parameter, lid, type. Een navigatie wordt van
   achter gelezen: het type waar een stap heen leidt, kiest welke van meer declaraties van één naam de stap ervoor
   noemt. Buiten een navigatie kiest het onderwerp van de regel, gelezen met de eigen `ExpressionBuilder` van de
   resolutie op `model.copy()` (een diagnose die hij opnieuw maakt, raakt het model van de aanroeper niet). Blijft het
   meer dan één declaratie, dan krijgt de span de soort en geen `declaredAt`: een verkeerde link is erger dan geen.

Prijs van de keuze: de pas kan afwijken van wat de resolutie bond. De corpusgetuige meet het (geen `unresolved` in
een document zonder diagnostics, elke verwijzing gelinkt) en de metingen hieronder ook.

### De soorten met hun LSP-type

| Soort | Wat | LSP 3.17 |
|---|---|---|
| `keyword` | sleutelwoorden, frasen, lidwoorden, voorzetsels, kwantoren, `waar`/`onwaar`, `(mv:`, `(bezield)`, een tijdeenheid als sleutelwoord buiten een eenheidsexpressie, `geen`, `n.v.t.` | `keyword` |
| `comment` | `//`-commentaar, `// Bron:` inbegrepen | `comment` |
| `heading` | een `---`-kopregel | `comment` + `documentation` |
| `number`, `date` | literals | `number` |
| `string` | een tekstliteral van aanhalingsteken tot aanhalingsteken (gesplitst rond `«…»`) | `string` |
| `designation` | de tekst van een benaming | `string` |
| `relationDescription` | de vrije tekst na de rolnaam in de cardinaliteitsregel van een feittype | `string` |
| `punctuation` | haakjes, `;` `.` `:` `,` `-` `..` `•` `/` `=` `%` `^` `€` `'` `«` `»`, tabelpipes | `operator` |
| `word` | een woord buiten elke naam die de parse gaf (alleen na een syntaxfout) | geen |
| `objectType` | | `class` |
| `attribute`, `characteristic`, `role` | | `property` |
| `factType` | | `interface` |
| `parameter` | | `variable` + `readonly` |
| `variable` | `Daarbij geldt`, gedeclareerd daar en gelezen in de eigen regel | `variable` |
| `domain`, `timeline`, `dayKind` | | `type` |
| `enumerationValue`, `dimensionLabel` | | `enumMember` |
| `dimension` | | `enum` |
| `unit` | gedeclareerd door het eenheidswoord; afkortingen en meervoud verwijzen ernaar; een tijdeenheid of teken zonder eenheidsysteem zonder `declaredAt` | `type` (+ `defaultLibrary` zonder `declaredAt`) |
| `unitSystem`, `vocabulary`, `ruleGroup` | `vocabulary` ook de opener `(<vocabulaire>:` van een benaming | `namespace` |
| `namedForm` | regelnaam, beslistabelnaam, naam van een uitzonderingsblok, en de verwijzing ernaar | `function` |
| `unresolved` | een woord op een naamplaats dat niets noemt; een enumeratiewaarde die geen domein declareert | geen (een language server meldt het als diagnose) |

Een span met `"declaration": true` krijgt de modifier `declaration`. Namen van de soorten volgen de draad waar die een
naam heeft (`objectType`, `characteristic`, `enumerationValue`, `namedForm`, `ruleGroup`, `VariableReference` →
`variable`).

### Na een syntaxfout

De parse tree is dan ANTLR's herstel. Regeltekst daarin wordt gelezen als alle regeltekst: een woord dat niets noemt
is `unresolved`. Een woord dat het herstel in geen naam en geen regeltekst plaatste, houdt zijn lexicale soort `word`,
geen `unresolved`: de syntaxfout markeert de plaats, en alles erna rood maken markeert het gevolg in plaats van de
oorzaak. Afgebroken bronnen lieten de resolutie zelf op zeven plaatsen crashen op een knoop zonder kind
(NullPointerException; `--ir` crashte daar ook, ook op main): `DefinitionResolver.unitSystem` (omrekening zonder
getal), `startDate` (week zonder startdatum), `fixedStartPoint` (kwartaal zonder maanden), `RuleChecker.allocation`
(afronding zonder getal), `columnsInconsistentDeep` (geneste voorwaarde zonder bullets), `buildCriteria` (criteria
zonder bullets) en `startPointAnchors`/`ruleSubjectOf`/`startPointDetermination` (startpuntbepaling zonder
onderwerp). Elk slaat de knoop nu over; de syntaxfout staat er al. Dat verandert `--ir` alleen op invoer waar het
crashte: daar meldt het nu de syntaxfout en schrijft niets (exit 1). `SemanticTokensRecoveryTest` pint de zeven
bronnen (rood zonder de wachten, groen met) en veegt elke golden afgebroken op veertig plaatsen en met twintig keer een
regel weg. Gemeten daarna: 33020 afgebroken bronnen over het corpus (200 sneden, 60 regels weg) en 1600 over de
TOKA-fragmenten en de v2-regelset als één tekst: geen uitzondering, de dekking houdt.

### Vloeren

`SemanticTokensCorpusTest` (gemeten bij de bouw, als ondergrens): 127 bestanden, 18813 spans; naamspans per soort
`objectType` 1441, `attribute` 1332, `role` 733, `namedForm` 489, `characteristic` 351, `unit` 243, `factType` 98,
`vocabulary` 48, `parameter` 43, `unitSystem` 33, `variable` 31, `dayKind` 28, `enumerationValue` 17,
`dimensionLabel` 13, `dimension` 9, `domain` 8, `timeline` 5, `ruleGroup` 4. De lijst van goldens zonder diagnostics
die een `unresolved` houden (`UNRESOLVED_KNOWN`) is leeg en faalt als ze verouderd raakt. `SemanticTokensRecoveryTest`:
7620 afgebroken bronnen, waarvan 4699 met een syntaxfout. In het corpus: geen enkele span `unresolved` (125 van de 127
goldens zonder diagnostics), geen verwijzing zonder declaratie, geen `word`.

### Meting (c): v2-regelset herleving-WW

`fundament/rules/*.rgs` gevolgd door `herleving-ww/rules/*.rgs`, 33 bestanden als één invoer, alleen gelezen, niets in
de repository: volledige dekking (`SemanticTokensCheck`), 0 diagnostics, 0 `unresolved`, 0 verwijzingen zonder
declaratie, 0 `word`; 6442 spans, 1,1–1,4 s inclusief de resolutie. Per soort (declaraties / verwijzingen):

| Soort | Decl. | Verw. |
|---|---|---|
| `attribute` | 52 | 363 (+ 4 `generatedFrom`) |
| `objectType` | 18 | 270 |
| `role` | 28 | 179 |
| `enumerationValue` | 44 | 75 |
| `vocabulary` | 2 | 97 |
| `characteristic` | 16 | 59 |
| `unit` | 5 | 61 |
| `namedForm` | 67 | 23 |
| `variable` | 10 | 15 |
| `parameter` | 7 | 13 |
| `domain` | 5 | 5 |
| `factType` | 14 | 0 |
| `unitSystem` | 2 | 0 |

Lexicaal: `keyword` 3326, `punctuation` 807, `comment` 677, `designation` 99, `number` 85, `relationDescription` 14.
985 van de 1160 verwijzingen gaan over een bestandsgrens. De meting stuurde twee keuzes bij: in v2 schreef "De
uitkeringsduur van een Uitkeringsduur" het objecttype als attribuut van dezelfde naam, en "de uitkeringsduur van de
uitkeringsduur van het Recht…" de middelste stap als attribuut; met de soort naar plaats (laag 3) is het eerste het
objecttype, het tweede de rol (28 spans attribuut → rol, 12 → objecttype, 4 attributen een andere eigenaar).

### TOKA, niet in de corpusgetuige

De kaart vraagt de 47 `toka/casus/toka-*.rgs` in de corpusgetuige van de frontend-suite. Dat kan niet zonder gate 1
te breken: de TOKA-tripwire (`scripts/check-self-contained.sh`) faalt op het woord in de zone `frontends`, ook in een
pad in een test. De TOKA-zone (`toka/src/test/java`, gate 11) valt buiten "May touch", en `CLAUDE.md`, `README.md` en
`toka/README.md` noemen daar "the two tests". Daarom gemeten, niet getuigd: de 47 fragmenten als één document geven
volledige dekking, elke verwijzing gelinkt, 0 `word`, 9 diagnostics (2 ERROR RGS-324, 1 ERROR RGS-295, 6 WARNING) en
24 spans `unresolved`, elk een naam die de fragmenten nergens declareren: "daadwerkelijke datum-tijd van vertrek"
(toka-22), "in het hoogseizoen" (toka-26), "de treinmiles van alle passagiers" en "het maximaal aantal te ontvangen
treinmiles" (toka-32 t/m 37), "passagier jonger dan 18 jaar" (toka-41, -45), "de datum van vertrek van de reis"
(toka-43, -44). Voorstel: een getuige in de TOKA-zone, onder gate 11, met mandaat voor `toka/` en de opsomming in de
drie teksten.

### Gate-samenvatting

Twee volledige runs met `werk/gates.sh`. `gates-1` op `a84081e5`: `EXIT 0`. `gates-2` op `f39bfe9b` (na de fix van de
kale naam), de eindstand: `EXIT 0`, alle zeventien rc=0 (2026-10-22 10:06–10:36; clingo 222 s, Drools 754 s, reftype
198 s, readings 375 s). Gate 10: JUnit-suite 1236 tests groen, 127 rgs-goldens bytegelijk heruitgegeven (`--ir`).
Gate 11: 45 TOKA-fragmenten bytegelijk heruitgegeven, de casusdump ongewijzigd, TOKA-suite 27 tests. Gate 16: 32
scenariogoldens geven hun 110 casebestanden en hun `ir.json` bytegelijk (`--cases`).

### Niet gedaan, met reden

- TOKA in de corpusgetuige: zie hierboven (gate 1).
- Een testdocument van de scenariolaag als invoer: zijn scenarioblokken horen niet bij `regelSpraakDocument` en geven
  hier een syntaxfout. Meekomen zou een tweede route vragen (`ScenarioResolver` geeft geen token stream en parse tree
  van de productiedelen terug); open punt.
- Het teken buiten het BMP "in een commentaar vóór een naam op dezelfde regel" (a): een `//`-commentaar loopt tot het
  regeleinde, dus zo'n naam bestaat niet. De getuige zet het teken in een benaming vóór een domeinnaam en in een
  tekstliteral vóór een attribuutnaam (de UTF-16-kolom), en één in een commentaar (de eindkolom).
- De kaart noemt 128 `source.rgs` op `680e5263`; `git ls-files 'corpus/golden/**source.rgs'` telt er 127.
- Geen `make tokens`-doel: de distributie roept de Java-ingang aan, de CLI is er voor de hand.

### Vondsten buiten de kaart

Op aanvulling van de eigenaar (tijdens de bouw): een vondst wordt op deze tak gefikst, met een test als getuige.

- **Gefikst (`f39bfe9b`): een kale naam die niets noemt, ging als `VariableReference` zonder diagnose naar schijf.**
  Een naam zonder lidwoord in een expressie leest de parse als `variabeleNaam` ("… moet berekend worden als
  basisprijs maal 2", ook "kleiner is dan dan 18", waar "dan 18" de naam is). Noemde hij geen variabele en geen
  parameter, dan gaf `ExpressionBuilder.nameRef` een `VariableReference`, `--ir` schreef exit 0, en de verifier weigerde
  (`WF-034.variable-unbound`). Dezelfde naam met lidwoord ("de basisprijs") werd al een marker met een WARNING
  (RGS-092); beide routes geven nu dat: zelfde draad, zelfde melding, geen nieuwe melding nodig. Wat de verifier daarna
  nog weigert (`WF-034.no-static-type`, de vermenigvuldiging rond de marker heeft geen type), weigert hij voor de
  lidwoordroute op main ook: dat is de markerdiscipline van 2026-09-19 (een marker blijft met een WARNING op schijf),
  niet deze vondst. Geen golden en geen TOKA-document droeg de knoop (alleen twee handgeschreven negatieve gevallen van
  de verifier), dus gate 10, 11 en 16 blijven bytegelijk. `BareNameTest`: rood vóór de fix (geen WARNING), groen erna;
  een kale variabele en een kale parameter binden nog.
- **Gefikst (`4cbb106e`): zeven crashes van de resolutie op ANTLR's herstel** (hierboven); ook `--ir` crashte daar op
  main. Er kunnen er meer zijn: de meting zocht met afgebroken bronnen en weggelaten regels, niet met ingevoegde of
  verwisselde tokens.
- **Niet gefikst, met reden: een eigen leesactie van een niet-gedeclareerd attribuut gaat zonder diagnose naar schijf**
  ("de taxatiewaarde van de Vondst"; de verifier weigert, WF-034.member-undeclared). Bekend en beschreven in
  `grammar/DIVERGENCES.md`, "An undeclared attribute read": de TOKA-casus leest zulke namen, een ERROR zou zijn emissie
  stoppen en een WARNING zijn gepinde documenten veranderen, en `toka/` valt buiten "May touch". De semantic tokens
  maken het zichtbaar als `unresolved` in een document zonder diagnostics.

### Open punten (normkeuzes)

1. Of een vrije naam met een woord als `dan` mag beginnen. "indien zijn leeftijd kleiner is dan dan 18" parseert:
   `vrijeNaam` neemt "dan 18" als naam (`naamWoordLower` bevat `DAN`). Na `f39bfe9b` krijgt de schrijver een WARNING
   dat de naam 'dan 18' niets noemt; of de grammatica dit als syntaxfout moet weigeren, is een keuze over de taal.

Voorstellen voor `spec/PLAN.md`/`CONSTRUCTS.md`/`CLAUDE.md`: geen. Het schema verandert niet en er komt geen gate
bij; de nieuwe getuigen draaien in de suite van gate 10. Het IR verandert alleen voor een document met een kale naam
die niets noemt (een `Unresolved` met WARNING waar een `VariableReference` stond); de tekst van IR-SPEC over
`VariableReference` ("survives only as the marker for a variable the resolver could not bind", geweigerd onder WF-034)
blijft juist, alleen schrijft deze frontend de knoop niet meer.

### Vervolg (TOKA-getuige)

`2be0800f` op tak `card/semantic-tokens-toka` (vanaf `8ded3090`), het punt "TOKA, niet in de corpusgetuige" hierboven:
`toka/src/test/java/dev/uchar/regelspraak/SemanticTokensCasusTest.java` leest de 47 `toka/casus/toka-*.rgs` in
naamvolgorde als één document door `Resolver.tokens` en eist volledige dekking (elk token in precies één span, geen
overlap, geen witruimte aan de rand), de tekst op elke span, elke verwijzing naar een span met `declaration` van
dezelfde soort, geen verwijzing zonder declaratie en geen `word`. `SemanticTokensCheck` is een testklasse en gate 11
compileert alleen de hoofdklassen van de frontend; de controles staan daarom opnieuw in de test, met een eigen lexing
van de keten. De spans `unresolved` zijn precies de lijst `UNDECLARED`: zes namen met fragment en reden, elk een lid
dat alle 29 replay-slices declareren en geen fragment ("daadwerkelijke datum-tijd van vertrek" in toka-22, "in het
hoogseizoen" in toka-26, "de treinmiles van alle passagiers" in toka-32 t/m 37, "het maximaal aantal te ontvangen
treinmiles" in toka-35 en -37, "passagier jonger dan 18 jaar" in toka-41 en -45, "de datum van vertrek van de reis" in
toka-43 en -44), per voorkomen de woorden die `unresolved` uitkomen (24 spans). De lijst faalt in beide richtingen: een
span `unresolved` buiten haar, een naam die niet meer in zijn fragment staat, of een voorkomen dat andere woorden
`unresolved` laat. Op kopieën buiten de repository rood gezien: een naam weg, andere woorden, een fragment te veel, een
fragment dat niet bestaat, een span weg, een verwijzing een kolom verschoven, een vloer één hoger.

Telling (2026-10-22, de vloeren in de test): 47 fragmenten, 1667 spans, 9 diagnostics; 403 naamspans, waarvan 143
declaraties en 260 verwijzingen, alle gekoppeld; per soort `attribute` 93, `role` 75, `unit` 65, `objectType` 38,
`parameter` 32, `namedForm` 31, `characteristic` 26, `domain` 14, `variable` 12, `factType` 4, `enumerationValue` 4,
`unitSystem` 3, `timeline` 3, `dayKind` 3; lexicaal `keyword` 903, `punctuation` 287, `number` 34, `string` 12,
`relationDescription` 4; `unresolved` 24. Gate 1 PASS; gate 11 PASS, TOKA-suite 28 tests (vloer
`EXPECTED_MIN_TESTS=27` niet verhoogd: het script is beschermd). `toka/README.md` noemt de test; `CLAUDE.md` en
`README.md` dragen dezelfde opsomming nog met "the two tests" en zijn niet geraakt (voorstel in het verslag aan de
hoofdsessie).
