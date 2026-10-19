# frontend-codes: een melding van de RegelSpraak-frontend krijgt een code, los van haar tekst

*Vervolgkaart 4 van `spec/proposals/MESSAGE-CODES.md` (§7), stap 7 van het programma "Engelse namen". Niet normatief:
raakt geen `spec/`, geen schema, geen `scripts/` behalve het voorstel in het slot. Landt ná kaart 3
(`drools-refusal-codes`), omdat beide `backends/drools/` bewerken; de bouwer herbaseert dan.*

Goal: `RegelSpraakModel.Diagnostic(severity, message, line)` is alleen tekst. Deze kaart voegt `code` (`RGS-###`) en
`params` toe aan dat record, kent op alle plekken die er een maken een code en benoemde parameters toe, en laat de
tekst ongewijzigd (ontwerp §2.4). De code staat NIET op de draad: IR-SPEC ch. 2 `diagnostics` (`node, severity,
message, line`, WF-113) blijft zoals het is. Reden en vervolg: kaart 5 haalt de fragmentpins weg, kaart 6 rendert de
tekst uit een catalogus; beide hebben een code per sjabloon nodig.

Modellen: Haiku kent de sleutels toe (twee partijen van 154 regels, 308 invoerregels), Sonnet bouwt. De
voorbereiding (de lijst van plekken, de partijen en de opdracht) staat in de werkmap van de hoofdsessie, buiten de
repository.

## Wat er staat (gemeten op `719a2377a`, 2026-10-19)

- **252 plekken**: 248 in `frontends/regelspraak` (`new Diagnostic` 127, aanroepen van `error(...)` 115 en van
  `refuse(...)` 6; RuleChecker 96, ScenarioResolver 69, DefinitionResolver 32, ExpressionBuilder 27,
  Designations 18, Resolver 5, IrEmitter 1) en 4 in `backends/drools` (`Coverage`, `PeriodCoverage`,
  `RegelSpraakCompileException.contract`, `IrLoader.diagnostic`).
- Wat het ontwerp niet telde: 22 plekken hebben geen letterlijke tekst en nemen de zin van een hulpmethode of
  variabele; daarachter staan 34 methoden die een meldingszin als `String` teruggeven en 22 aanroepen
  `Result.error(...)` in `Typing.java` (56 hulpzinnen). Elk sjabloon krijgt een code; de drager over een
  `String`-hulpmethode heen is een ontwerpkeuze van de bouwer (PLAN, stap 4).
- 17 plekken in drie testbestanden bouwen een `Diagnostic` met drie argumenten.

## Besluiten

- Record: `Diagnostic(Severity severity, String code, Map<String, String> params, String message, int line)`, met
  daarnaast de constructor `Diagnostic(severity, message, line)` voor een melding zonder code (`code == null`,
  `params` leeg). Reden: een `Diagnostic` van de draad draagt geen code (`IrLoader.diagnostic` leest er een) en de
  kaart wijzigt geen testbestand; de bouwer gebruikt hem in de frontend zelf niet. `toString()` en
  `isError()` blijven.
- Code als `RGS-###`, oplopend vanaf 001 in volgorde van het register, nooit hergebruikt (ontwerp §3.2). Eén code
  per meldingssjabloon, nooit één code voor twee sjablonen. Dezelfde zin op twee plekken mag één code delen als de
  parameters gelijk zijn.
- Het register: een TSV als resource van `frontends/regelspraak` (`code`, `slug`, `severity`, parameternamen), met de
  sleutel van Haiku als de leesbare naam. Waarden van parameters zijn tekenreeksen: een gedeclareerde naam
  zoals gedeclareerd, een waarde als lexeem (ontwerp §3.3).
- Geen wijziging van de tekst: ook geen opmaak, geen volgorde, geen spaties.

## Acceptance

- (a) `Diagnostic` heeft `code` en `params` en de constructor zonder code; `RegelSpraakModel` is het enige
  bestand van `model/` dat verandert. Het schema, IR-SPEC en de verifier zijn onaangeroerd:
  `git diff main -- spec/ corpus/` is leeg.
- (b) Alle 248 plekken van de frontend en de 3 coderende Drools-plekken geven een code en `params` mee; alleen
  `IrLoader.diagnostic` blijft zonder code. Machinaal: tijdelijk gooit de constructor zonder code een uitzondering,
  waarna de volledige JUnit-suite van `frontends/regelspraak` en gate 12 een plek zonder code als fout melden; de
  tripwire gaat er vóór de commit weer uit. Een tweede, blijvende controle: de lijst van de plekken uit
  het meetscript van de voorbereiding (`sites.py`) heeft na de bouw nul `new Diagnostic(` met drie argumenten buiten `IrLoader`.
- (c) Het register en de plekken kloppen in beide richtingen: elke code die een plek meegeeft staat in het register
  en elke registerregel wordt door minstens één plek gebruikt; de parameters van een plek zijn precies de
  parameters van haar registerregel. Een script (door de bouwer geschreven, en als
  voorstel voor een been van gate 10) toetst dit. Vloer `EXPECTED_MIN_RGS_CODES`: het gemeten aantal codes (bovengrens
  308, het aantal sjablonen vóór het delen).
- (d) De tekst is gelijk: de berichten (`message()` en `line`) van elk document in de JUnit-corpus en van elke golden
  met een `bron.rgs`, vóór en na de bouw gedumpt, zijn byte-identiek; er verandert geen bestaande toets en geen
  bestaande verwachting, en gates 10, 11 en 16 zijn groen.
- (e) De vier Drools-plekken volgen: `Coverage`, `PeriodCoverage` en `RegelSpraakCompileException.contract` dragen
  een `RGS`-code, de `IrLoader` leest `code == null`. `BranchDumpsCorpusWitnessTest` en gate 12 groen.
- (f) Alle gates van `CLAUDE.md` groen op de eindstand van de branch.

May touch: `frontends/regelspraak/src/main` (de zeven bestanden hierboven, `RegelSpraakModel`, het register als
resource), `backends/drools/src/main` (de vier plekken), deze kaart. Een nieuw testbestand voor het register alleen na toestemming van de eigenaar; zonder toestemming draagt het
script van (c) de toets.

Out of scope:
- De catalogi, de vertaling en de renderer (kaart 6); de standaardtaal van de Drools-backend.
- De codes van de weigeringen van gate 12 (kaart 3) en het opruimen van de fragmenten (kaart 5).
- Een code op de draad: schema, IR-SPEC, verifier, WF-113.
- De refusals van de frontends `flogic` en `catala` en de 53 `LOAD ERROR`-plekken van de loader (ontwerp §2.5).
- Een wijziging van de tekst van een melding, ook als die onhandig is.
- Bestaande testbestanden: geen wijziging.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

Voorstel voor de hoofdsessie (beschermd, `scripts/`): een been van `scripts/check-regelspraak.sh` dat het script van
(c) draait, met een vloer.

## Result

Gebouwd op `card/frontend-codes`, op main `0ba90bff5` (kaart 3 stond er): `7feb3cb5a` (de frontend: record, drager, 248
plekken, register) en `aa35227ef` (de drie Drools-plekken, `scripts/check-rgs-codes.sh`, het been in gate 10), plus dit
Result. Alle 16 gates groen in één volledige run op `aa35227ef` (`failed=0`).

**Wat er staat.** `Diagnostic(severity, code, params, message, line)` met de constructor zonder code voor de draad en voor de
17 testplekken (`params(...)` is er een statische hulp van: naam/waarde-paren, de waarde zoals de tekst haar rendert). De
tekst is nergens veranderd. Het register `diagnostic-codes.tsv` (resource van de frontend; kop `code slug severity params`):
**347 codes** `RGS-001..347`, 344 van de frontend en 3 van `backends/drools` (`fact-model-contract-violation`,
`coverage-verdict`, `period-coverage-hole`); `IrLoader.diagnostic` blijft zonder code. 248 plekken in de frontend en 3 in
Drools geven code en `params` mee; `git diff main -- spec/ corpus/` is leeg. De bovengrens 308 van (c) bleek te laag: zie
Haiku.

**De drager over een `String`-hulpmethode (PLAN stap 4).** Eén vorm voor alle hulpzinnen: `Typing.Coded(code, params, text)`
met `prefixed(...)`, `framed(...)` en `at(severity, line)`. Elke hulpmethode die een zin teruggaf geeft nu een `Coded`
(in ExpressionBuilder en RuleChecker, de 22 `Result.error` en alle openbare zinfuncties in `Typing`; een hulpmethode die alleen een naamfragment teruggeeft, zoals `memberSentence`, bleef een `String` en gaat als parameter mee);
`Typing.Result(type, failure)` houdt `error()` als tekst, dus `TypingTest` bleef ongewijzigd (zijn `assignment`-aanroepen
zijn `assertNull/assertNotNull`). `RuleChecker`, `ScenarioResolver` en de beslistabel-sink kregen een overload die een
`Coded` aanneemt; `DecisionTableDesugar.desugarCoded` draagt de logica en `desugar(..., Consumer<String>)` blijft als
adapter voor de zes testaanroepen met `warns::add`. Elke plek met een sjabloon dat een tak (`? :`) kiest, is in aparte
aanroepen gesplitst, zodat een code altijd direct voor zijn `params(...)` staat en de controle ze kan lezen.

**Haiku en de controle.** `join_answers` gaf 11 meldingen op 9 regels (een doorgifte van de draad, een naamfragment, hulp- en
`Typing`-regels met `delegates` zonder eigen sjabloon); alle negen zijn legitiem, hun zinnen kregen hun code van de
hulpmethode. `registry.py`: 307 sleutels, **0 botsingen** (geen sleutel met twee parametersets). Daarna handwerk, omdat de
meting van de voorbereider de zinnen miste die `String` teruggeven zonder naam op `...Error`/`...Sentence`:
- 45 nieuwe sleutels: de openbare zinfuncties van `Typing` (`begrenzing`, `clampBase`, `numberAggregation`,
  `dateAggregation`, `assignment`, `allocationAssignment`, `unitsCompatible`, `perSide`, `dateComputeSentence`,
  `orderingFoldSentence`; 22 sleutels), de zes `warn`-zinnen van `DecisionTableDesugar` (`table-row-*`, ernst WARNING), en
  hulpmethoden in `RuleChecker` die de lijst niet noemde (`unknownCarrier`, `negationWithoutCharacteristic`,
  `negationAfterArticle`, `periodBoundaryWithoutReader`, de dagsoort-zuiverheid, de takken van `checkRuleTargets`,
  `inputDeclarationError`, `inputWriterError`).
- 5 sleutels van Haiku vervielen: drie `ordering-fold-*` en `to-many-step-in-scalar-slot` en `absolute-duration-unit-error`
  (vervangen door de sleutels van de hulpmethode die hun zin leverde). `clamp-unit-mismatch` dient nu twee plekken met
  dezelfde zin en verschillende ernst (ERROR in `Typing.begrenzing`, WARNING in `reportClamp`; het register noemt beide).
- Eén sleutel per sjabloon: 13 codes staan op meer dan één plek; ik vergeleek hun tekstfragmenten en alle zijn dezelfde zin
  (drie verschillen waren alleen een voorvoegsel dat de aanroeper zet). Vijf Nederlandse woorden uit sleutels en
  parameters vervangen (`onderwerp` werd `subject`; de sleutels met `die-dat`, `een` en `heeft` kregen Engelse woorden).
- 347 = 307 sleutels van Haiku, min 5 vervallen, plus 45 nieuwe; de drie van Drools zaten al in Haiku's 307.

**Tekstgelijkheid (d).** Een tijdelijke haak in de constructor schreef bij elke `Diagnostic` `severity`, `line` en `message`
weg. Op main `0ba90bff5` en op de eindstand van de branch, elk met de volledige JUnit-suite (1199 tests, beide groen) en met
`Resolver --ir` over elke `bron.rgs` en elke `toka-*.rgs`: **1127 meldingen uit de suite en 305 regels uit de corpus,
byte-identiek** (`cmp` leeg); de haak staat niet in de commits. Daarna een tripwire in de constructor zonder code, die
gooide voor een aanroeper in `resolver`, `emit`, `transpiler` of `CompileException`: JUnit groen (1199) en de corpus-run
zonder `TRIPWIRE`; ook die staat niet in de commits. De blijvende controle is `check-rgs-codes.sh`: nul `new Diagnostic(` met
drie argumenten buiten `IrLoader`. 306 van de 344 frontendcodes komen in suite en corpus voor; de 38 overige (alle
`table-row-*` behalve één, enkele `Typing`-sjablonen, een paar zeldzame takken) heb ik per plek in de bron gelezen.

**Het been van gate 10 (c).** `scripts/check-rgs-codes.sh` (nieuw, door `check-regelspraak.sh` als eerste aangeroepen; vloer
`EXPECTED_MIN_RGS_CODES=347`, gemeten 2026-10-19): elke code die een plek meegeeft staat in het register en elke regel wordt
gebruikt; de parameternamen van elke plek zijn die van haar regel (`params(...)` direct na de code, of de vaste set van de
ene bouwer `perSide`); een letterlijke ernst bij de constructor is er een die de regel noemt; codes en sleutels uniek en
oplopend; geen `new Diagnostic(` met drie argumenten buiten `IrLoader`; een lege scan faalt. Mutaties op een kopie van de
bronnen, elk rood met de reden en op de ongewijzigde stand groen (`exit 0`, 347 codes, 363 plekken):
- een parameternaam op een plek veranderd (`what` in `wat`): `RGS-277 passes ['wat'], the register has ['what']`;
- een plek met een code die het register niet kent (`RGS-999`): `is not in the register`, en de regel die ze verloor is
  `used by no site`;
- een registerregel die niemand gebruikt (`RGS-348`): `is used by no site`;
- een plek zonder code (de drie-argumentsvorm in `ScenarioResolver.error`): `new Diagnostic( with three arguments`;
- de vloer op 999: `the register holds 347 codes, EXPECTED_MIN_RGS_CODES pins at least 999`;
- de ernst van `RGS-200` in het register op WARNING: `RGS-200 passes ERROR, the register has ['WARNING']`.

**Testbestanden.** Geen enkel testbestand aangepast. Wat meeging zonder wijziging: de 17 plekken met de drie-argumentsconstructor,
`TypingTest` (zie boven), `DecisionTableDesugarTest` (de adapter).

**Open.**
- Een voorvoegsel dat de aanroeper voor een `Typing`-zin zet (`verdeling: `, `objectcreatie: `, `voorwaarde: `,
  `startpuntbepaling (regel 'x'): `, `<soort>: `, `scenario: `) is geen parameter: de code noemt de zin, niet de plek. Kaart 6
  moet daar een `context`-parameter of een eigen code voor kiezen (`startpuntbepaling` verliest zo de regelnaam in `params`).
  `unit-on-one-operand-only` en `units-not-convertible` hebben bij een verdeling `operation` leeg.
- Een bericht dat zinnen met `; ` samenvoegt is één sjabloon met optionele stukken: `perSide`-sjablonen (`left`/`right`
  leeg waar de operand goed is), `ordering-fold-operands-refused` (`kinds`, `mixture`), `arithmetic-fold-over-non-number`,
  `duration-operand-not-date`, `navigation-over-to-many-role` en `to-many-hop-in-scalar-slot` (`hops`, `roles`).
- Parameters met een Nederlands zinsdeel (`reason`, `where`, `range`, `sentence`, `suffix`, `cause`, `spelling` en
  `problem` op ruim twintig plekken) dragen een fragment dat een catalogus niet kan vertalen; kaart 6 geeft die fragmenten
  een eigen code of laat ze vervallen. De waarde van een parameter is `String.valueOf`, dus een `null` staat daar als
  "null", zoals in de tekst.
- Niets leest het register nog: `src/main/resources` zit niet in de jar-only `Makefile`; kaart 6 leest het.
- De drie Drools-meldingen dragen hun `reason`/`violation` als genest zinsdeel (de `DROOLS-`code van kaart 3 staat niet in
  `params`); `contract(...)` heeft in dit repository geen aanroeper, de distributie geeft de zinnen mee.

**Voorstel voor `CLAUDE.md`, gate 10** (aan het eind van de alinea): "Since the card `frontend-codes` (2026-10-19) the gate
calls `scripts/check-rgs-codes.sh` first: every message the frontend raises, and the three the Drools backend raises itself,
carries a code `RGS-###` and named parameters beside its Dutch text (`Diagnostic(severity, code, params, message, line)`; the
code is not on the wire, IR-SPEC ch. 2 is unchanged), from one register,
`frontends/regelspraak/src/main/resources/dev/uchar/regelspraak/diagnostic-codes.tsv`. The script holds register and
sources together in both directions (every code a site passes is registered, every registered code is passed, the
parameter names agree, no `new Diagnostic(` with three arguments outside `IrLoader`) and carries
`EXPECTED_MIN_RGS_CODES=347` (measured 2026-10-19); an empty scan is a failure." En in de lijst van floors/gates geen
andere wijziging.

**Gates.** Eén volledige run op `aa35227ef`, 02:30 tot 02:48, `failed=0`: gate 10 groen (JUnit 1199 tests, vloer 173; 125
goldens met `bron.rgs` re-emitteren byte-identiek; `check-rgs-codes`: 347 codes, 363 plekken), gate 11 (29 ref en 29 clingo),
gate 12 (JUnit 1619 tests, vloer 1573, 255 gespeeld in beide takken, `BranchDumpsCorpusWitnessTest` gedraaid), gate 16 en alle
andere groen. Eerder per stuk: gate 12 `--fast` groen na de Drools-plekken. Gate 1 draaide na de commit nog eens: groen.
