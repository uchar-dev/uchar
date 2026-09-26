# scenariolaag-in-uchar: scenariotoetsen als frontend-extensie, gelowerd naar casebestanden

De "scenariotoetsen"-extensie van de oude toolchain (Situatie-definities en
Scenario-blokken in RegelSpraak-stijl; `docs/SCENARIOTOETSEN.md` in
uchar-dev/regelspraak-toolchain, 264 regels: doel, oppervlak, semantiek,
runner, gates) hoort in uchar (maintainer, 2026-09-26): het is semantiek
over de IR, geen presentatie. uchars grammatica parseert de blokken al
(`RegelSpraak.g4` r. 101–191: `situatieDefinitie`, `scenario`,
`scenarioScope`, `scenarioPeildatum`, `scenarioBasis`, `situatieBlok`,
`situatieWaarde`); de resolver negeert ze. In de oude toolchain:
`ScenarioResolver` (785 regels, resolverpakket; gebruikt zeven
package-private leden van de resolver), `ResolvedScenario` (102),
`ScenarioRunner` (571), `ScenarioRunnerCli` (274), `ScenarioResultaat`
(61; staat al in uchar, ongebruikt), `Verbalisator` (433, uitlegtekst),
`ParametersetLader` (326, parametersets per peildatum voor de dienst),
`RegelSpraakEngine` (187, compile→session→fire); tests
`ScenarioTypeGateTest`, `ScenarioParseTest`, `ScenarioMutatieTest`,
`ParametersetScenarioTest`, `ScenarioRunnerTest`, `ScenarioOrakelTest`,
`ScenarioMutatieRunTest`. uchars tegenhanger van een scenario is het
caseformaat (`spec/schema/uchar-case-v0.schema.json`: `asOfDate`,
`input.instances`, `input.parameters`, verwachtingen `attributeValue`,
`attributeEmpty`, `attributeTimeline`, `characteristicPresent`,
`characteristicAbsent`, `consistent`, `inconsistent`), dat de drie
uitvoerders al spelen (gates 5, 6, 12). Ontwerp: de scenariolaag wordt een
frontend-extensie met een LOWERING naar casebestanden; de replay is de
runner. Rechten: de code komt uit de mirror (Apache-2.0, eigen werk); de
fixtures van de oude toolchain (`examples/toka-scenarios`) zijn ©-materiaal
en komen nergens in uchar — alle getuigen zijn Jutterswet.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 `ScenarioResolver` en `ResolvedScenario` komen in `frontends/
  regelspraak` (resolver- en ir-pakket, op uchars grammatica en records);
  hun package-private toegang is daarmee gewoon. De resolver-diagnoses
  volgen het regime: Engelse lezerszinnen waar de verifier een zin heeft,
  anders eigen zinnen van de extensie, vastgelegd in `grammar/
  DIVERGENCES.md` (nieuwe sectie "Scenariotoetsen"), waar ook
  `SCENARIOTOETSEN.md` in opgaat (of ernaast als eigen document — bouwer).
- U2 Lowering `ResolvedScenario` → casedocumenten (`uchar-case-v0`, in de
  canonieke sleutelvolgorde van gate 2): situatie → `input` (instanties met
  attributen, kenmerken, rollen; parameters), peildatum → `asOfDate`,
  verwachtingen → `expectations`; `gebaseerd op de situatie` wordt
  uitgevouwen; de scenarionaam bepaalt de bestandsnaam
  (`case<n>-<slug>.json`, bouwer legt de slugregel vast). Een
  `scenarioScope` (toetst de regel/regels/regelgroep) lowert naar het
  IR-document: de scope bepaalt welke regels het geëmitteerde `ir.json`
  draagt — geen wijziging van het caseformaat, geen uitvoerder verandert.
  Wat niet in het caseformaat past, is een WEIGERING met zin, nooit stil
  verlies; de bouwer meldt elke vorm uit `SCENARIOTOETSEN.md` die zo
  weigert in het Result.
- U3 `Resolver` krijgt een modus die naast `--ir` de casebestanden schrijft
  (`--cases <map>`, bouwer). De frontendsuite (`make test`) draagt de
  geporteerde scenariotests op Jutterswet-fixtures: parse, typegate,
  mutatie (verkeerd type, onbekend attribuut, dubbele situatie, kringen
  in `gebaseerd op`), elk rood-eerst.
- U4 Nieuwe gate (**voorstel**, `scripts/check-scenario.sh`, protected —
  de hoofdsessie landt haar en schrijft de CLAUDE.md-regel): elke golden
  met een scenariodocument (`scenario.rgs` naast `bron.rgs`, bouwer legt
  de naam vast) regenereert haar casebestanden byte-identiek en, bij een
  scope, haar `ir.json`; floor `EXPECTED_MIN_SCENARIO_GOLDENS`; een lege
  verzameling faalt. Gate 4 blijft de schemapoort; gates 5/6/12 blijven de
  replay.
- U5 Getuigen: scenariodocumenten voor ten minste vijf bestaande goldens,
  waaronder de drie dialect-tweelingen (`ww-herleving-inkomen-rgs`,
  `jutterswet-uitsluiting-rgs`, `jutterswet-nestkenmerk-rgs`), waarvan de
  lowering de GEPINDE casebestanden byte-identiek reproduceert (het
  sterkste bewijs: de handgeschreven verwachtingen zijn het anker); elke
  verwachtingssoort van het caseformaat komt ten minste eenmaal voor; één
  golden met een scope.
- U6 De runner: `ScenarioRunner`, `ScenarioRunnerCli`, `RegelSpraakEngine`
  komen NIET naar uchar (de replay is de runner; de distributie bouwt haar
  CLI op frontend-lowering + de replay-facade van de kaart
  publieke-api-distributie). `ScenarioResultaat` blijft in
  `backends/drools` als het rapportrecord dat de replay vult (status per
  verwachting, vuringen, dekking) — de bouwer meet wat de oude runner
  rapporteert boven wat `CaseRunner` al berekent en port dát;
  `Verbalisator` (uitleg) en `ParametersetLader` zijn presentatie en
  dienst-infrastructuur: distributie. Markerexit en trace: Result.
- U7 Klaar: alle gates groen inclusief de nieuwe; gate 10 byte-identiek
  op alle bestaande goldens; naamtabel-toets rc 0; geen D-/U-nummers in
  code, scripts, DIVERGENCES; de pariteitszone alleen tellen.

## Acceptatie

- (a) U1–U7; (b) `make ir` en de casemodus op de vijf getuigen: `cmp` leeg
  tegen de gepinde bestanden; (c) een scenario met een niet-lowerbare vorm
  eindigt in een diagnose met zin.

May touch: `frontends/regelspraak/**`, `corpus/golden/*/scenario.rgs`
(nieuw, bouwer), `scripts/check-scenario.sh` (**voorstel**),
`backends/drools/**` (alleen `ScenarioResultaat` en het rapport van de
replay), `spec/schema/` alleen als een verwachtingssoort ontbreekt
(**voorstel**, met verifier/ref/clingo/Drools-dekking — liever niet).
Niet in deze kaart: `CLAUDE.md`, `WAY-OF-WORKING.md`, `spec/PLAN.md`
(hoofdsessie bij de landing).

## Result (2026-09-26, bouwer)

**Stand.** Basis main `b88a8ccc`; claim `61e037cc`; de werk-commits `db949e73` t/m `0417d20a`, daarop deze
Result-commit als kop. Werkboom schoon na de Result-commit. Niets gepusht of gemerged.

**Commits.**
1. `61e037cc` claim.
2. `db949e73` scenariolaag: scenario's gelowerd naar casebestanden en IR-document — grammatica (alleen onder
   `testDocument`), `Resolver.bouw`, `ir/ResolvedScenario`, `resolver/ScenarioResolver`, `emit/CaseEmitter`,
   `--cases <map>` naast `--ir`, `make ir … CASES=<map>`.
3. `c86182fb` zes goldens krijgen een `scenario.rgs`.
4. `d7b0184a` 64 frontendtests (Parse 8, TypeGate 35, Mutatie 10, Lowering 11); een geschreven breuk blijft een breuk.
5. `ad99891c` DIVERGENCES-sectie "Scenariotoetsen" (het contract; `SCENARIOTOETSEN.md` gaat erin op).
6. `9278a2bd` `scripts/check-scenario.sh` (**voorstel**, beschermd pad).
7. `0417d20a` de replay vult `ScenarioResultaat` (`CaseRunner.rapport`) + `ScenarioRapportWitnessTest` (3).

### Per U-besluit

**U1 — resolver en IR-record in de frontend.** `ScenarioResolver` (resolverpakket, pakketprivé toegang tot
`Resolver.bouw`, `KenmerkMatcher`, `Typering`, `DefinitionResolver`, de bronlezers van `ExpressieBouwer`) en
`ResolvedScenario` (ir-pakket) op uchars grammatica, model en pakketnaam `dev.uchar.regelspraak`. Aanpassingen
t.o.v. de bron: het niet-scenariodeel gaat door dezelfde pijplijn als de productieroute (`Resolver.bouw`, uit
`Resolver.resolve` gelicht: definities, syntaxdiagnoses, RuleChecker mét tokenstroom, markeersweep), dus ook de
defaults-blokken van het dialect (de oude route weigerde die); datums via de hele-match-bronlezers (de oude
`find()`-lezer las rommel als datum); exacte breuken in plaats van DECIMAL64; rollen opgelost zoals de caselezers
een rolsleutel oplossen (naam of meervoud, subtype-bewust). Zinnen: eigen Nederlandse zinnen van de extensie met
prefix `scenario: `, en waar een waarde niet typeert de zin van `Typering.toekenning` (die al verifierzinnen
spreekt); `LezerszinGrensTest` groen over de nieuwe resolverliterals. `SCENARIOTOETSEN.md` is opgegaan in
`grammar/DIVERGENCES.md` § "Scenariotoetsen" (bouwerskeuze: één register), met alle zinnen letterlijk;
`'§Scenariotoetsen'` is uit de lijst ontbrekende secties in de inleiding.

**U2 — lowering.** `CaseEmitter.emit` schrijft `uchar-case-v0` met de serialisator van de emitter (`IrEmitter.Json`
pakketzichtbaar gemaakt: één vorm voor IR en case). Situatie → `input` (instanties met attributen, rollen,
kenmerken, `characteristicTimelines`; parameters), peildatum → `asOfDate` (anders `null`), verwachtingen →
`expected`; `gebaseerd op` uitgevouwen (situaties mogen nu ook op situaties voortbouwen). Bestandsnaam
`case<n>-<slug>.json`: n = 1-gebaseerde positie in het document, slug = naam zonder diakrieten, kleine letters, elke
reeks tekens buiten a-z0-9 één `-`, niet aan de randen; een lege slug is een weigering. Scope → IR-document
(`CaseEmitter.metScope`): declaraties, dagsoortdefinities, startpunten en diagnoses heel; van de benoemde vormen
alleen wat de scope noemt (regel zonder versiesuffix, beslistabel bij naam = al haar rijen, defaultsgroep bij de
naam van haar basisregel, regelgroep = haar leden); een regelgroep houdt haar gedragen leden en valt weg zonder;
zonder scope is het IR-document dat van de productieroute, byte-gelijk. Het caseformaat en de uitvoerders
veranderen niet. Sleutelvolgorde: die van het schema, behalve `characteristicTimelines` ná `characteristics` —
dat is de corpusvolgorde (gemeten: alle 51 instanties die het veld dragen), anders reproduceert
`jutterswet-dagentelling-rgs` niet (open punt 1).
Vormen uit het oude `SCENARIOTOETSEN.md` die hier WEIGEREN (elk met zin): (1) een peildatum met tijd (asOfDate is
een dag); (2) scenario's met verschillende scope in één document (één casemap, één `ir.json`); (3) een scope in een
document met een startpuntbepaling; (4) een scope die een uitzonderingsblok of lid van een ongeordend blok noemt
(noem de basisregel). Daarnaast zonder casevorm: gedimensioneerd attribuut, tekstwaarde met «expressie»,
periodegrens met tijd. Afwijkend van het oude contract (geen weigering): de scope is optioneel en filtert het
IR-document in plaats van alleen dekking/markerpoort/parameterpoort te sturen; situaties zijn volgorde-
onafhankelijk (declaratie-vóór-gebruik vervalt, kringen worden geweigerd); `is niet <kenmerk>` naast `is geen`;
enumeratiewaarden in gedeclareerde spelling.
Nieuwe vormen (alleen `testDocument`, geen nieuw token): `van <datum> tot <datum>` op attribuut, kenmerk,
parameter en verwachting; `is altijd leeg`; `inconsistent volgens "<regel>" [voor <LABEL>]` (`volgens` via een
predicaat op tekst, want een token zou de lexing wijzigen van een bron in de pariteitszone die het woord draagt);
`Situatie <naam> gebaseerd op de situatie <naam>…:`; rollijst met komma.

**U3 — casemodus en tests.** `Resolver --ir <out.json> --cases <map> <file.rgs…>` (en `make ir FILE="…" OUT=…
CASES=<map>`): schrijft IR-document én casebestanden; bij een ERROR niets, elke diagnose met `[bestand:regel]`.
Frontendsuite: `ScenarioParseTest` 8, `ScenarioTypeGateTest` 35, `ScenarioMutatieTest` 10 (verkeerd type in
situatie en verwachting, onbekend attribuut in beide, onbekend label, dubbele situatie, kring `basis → vervolg →
basis`, kring op zichzelf, onbekende basis), `ScenarioLoweringTest` 11 (met de hand uit het schema geschreven
casebestand als anker) — alle op een eigen Jutterswet-model (`ScenarioFixture`), geen ©-fixture.
Rood-eerst, gemeten met proefmutaties (logs (buiten de repo)): A weigeringen uit → 42/64 rood;
B lowering stuk (scope niet gefilterd, schemavolgorde) → 3 rood; C basisgrammatica b88a8ccc → de 5 tests van
nieuwe vormen rood; C2 hekmutatie (predicaat weg, `tot en met` toegelaten) → de 2 hektests rood; D semantiek (eerste
wint, geen omrekening, altijd enkelvoudssleutel, geen tijdlijnvouw, parameterpoort dicht, geen declaraties) → 55
rood; E slug zonder diakriet-stripping → 3 rood. Nooit rood: alleen `elke_scopevorm` (bewaakt vormen die de
basisgrammatica al kende). Eén echte rode tijdens de bouw: `3/4 vadem` werd `0.75`; nu blijft een geschreven breuk
een gereduceerde breuk.

**U4 — gate (voorstel).** `scripts/check-scenario.sh`: bouwt de frontend; per golden met `scenario.rgs` naast
`bron.rgs` moeten de gelowerde casebestanden exact de gepinde set zijn (geen ontbrekend, geen extra) en elk
byte-gelijk, en `ir.json` byte-gelijk (voor ELKE scenariogolden, niet alleen bij scope: sterker, en het bewijst dat
de scenarioroute hetzelfde model bouwt); lege verzameling faalt; vloeren `EXPECTED_MIN_SCENARIO_GOLDENS=6`,
`EXPECTED_MIN_SCENARIO_CASES=29` (gemeten 2026-09-26). Rood gezien op: gemuteerde verwachting (DIFF), ontbrekend
scenario (SET DIFF), lege verzameling, vloer goldens 7, vloer cases 30; daarna groen.

**U5 — getuigen.** Zes goldens, `cmp` leeg op alle 29 casebestanden en 6 `ir.json` (via `make ir … CASES=`,
log `acceptatie-b.log`): `ww-herleving-inkomen-rgs` 10/10, `jutterswet-uitsluiting-rgs` 4/4,
`jutterswet-nestkenmerk-rgs` 4/4 (`inconsistent volgens "Jutter.vrijgesteld/0" voor M`),
`jutterswet-dagentelling-rgs` 4/4 (tijdlijnen, `is altijd leeg`), `jutterswet-rubriektabel-rgs` 3/3 (DE scope-
golden: `toetst de regels rubriek van een aanspoelsel en weegplicht bij dubbele borging`, beslistabel bij naam),
`jutterswet-dubbelverbinding` 4/4 (gedeelde `Situatie`, rollen van beide kanten met meervoudssleutel `liggers`,
peildatum). Elke verwachtingssoort komt voor: attributeValue, attributeEmpty, attributeTimeline,
characteristicPresent, characteristicAbsent, consistent, inconsistent (met rule en label; de kale vorm in
`ScenarioLoweringTest`). De scope filtert werkelijk: met alleen de tabel genoemd draagt het IR-document 2 regels,
met alleen de regel 1 (proef op rubriektabel, en `de_scope_draagt_alleen_wat_zij_noemt`).

**U6 — runner.** `ScenarioRunner`, `ScenarioRunnerCli`, `RegelSpraakEngine`, `Verbalisator`, `ParametersetLader`
niet geport. Gemeten verschil oude runner vs `CaseRunner`: de oude runner gaf per verwachting verwacht/werkelijk/ok
(ook bij een match), een vuringentrace (regel, gebonden labels, aantal), dekking per scoperegel
(GEVUURD/NIET_GERAAKT/MARKER), status PASS/FAIL/MARKER met diagnose, en uitleg; `CaseRunner` gaf alleen een
foutregel per mismatch en PLAYED/FAIL/EXCLUDE per golden. Geport is dat verschil minus de uitleg:
`CaseRunner.rapport(irPad, casePaden)` → één `ScenarioResultaat` per case, uit dezelfde exec-model-replay; de
verdictregel van de replay is ongewijzigd (gate 12 pint 209/179 zoals voorheen). `ScenarioResultaat` aangepast
aan wat de replay vult: `VerwachtingResultaat.positie` (1-gebaseerde plaats in `expected`) i.p.v. een bronregel;
`DekkingSoort` = GEVUURD | NIET_GERAAKT (een niet-gelowerde vorm laat de replay het document weigeren vóór er een
case speelt; de casestatus zegt dan MARKER met de reden). Gemeten op `jutterswet-uitsluiting-rgs` case1:
`Vuring[regel=defaults K:Jutter.uitgesloten, labels=[M], aantal=1]`, dekking `penninggerechtigd tenzij
uitgesloten` NIET_GERAAKT, `defaults K:Jutter.uitgesloten` GEVUURD; case4 beide GEVUURD.
Markerexit: de oude runner gaf exit 3 bij een markerende scoperegel. In uchar is er geen CLI (U6); het rapport
draagt per case status MARKER met reden (`anchor/jutterswet-erflijn`: `transpiler SKIP: …`), zodat de CLI van de
distributie MARKER → 3, FAIL → 1, PASS → 0 kan afbeelden zoals het oude contract. Trace: altijd gevuld (waarde
leeg, zoals bij de oude runner); het afdrukken bij FAIL is de CLI van de distributie.

**U7 — klaar.** Gates die de kaart raakt, elk los (logs (buiten de repo)): gate 1 PASS (vóór elke
commit); gate 10 PASS — 828 tests (vloer 173), 93 rgs-goldens byte-gelijk (vloer 80); check-scenario PASS — 6
goldens (vloer 6), 29 casebestanden (vloer 29) en hun `ir.json` byte-gelijk; gate 11 PASS — 29/29 in ref, clingo
en Drools; gate 12 PASS — played 209 (pin 209), DRL-tak 179 (pin 179), JUnit 817 (vloer 239); gate 13 PASS — 41
twists over 23 goldens, 106 replays, 22 via de DRL-tak; gate 8 PASS — 43 rode cases (vloer 26), Drools-poot 11
vastgepind en 32 niet gedragen met reden; naamtabel-toets rc 0 (`v1-naamtabel-toets: OK`). Geen U-/D-nummers en
geen verboden woorden in de diff (grep leeg). De pariteitszone is alleen geteld. Gates 2–7, 9 en 14 niet gedraaid:
geen van hun invoerbestanden veranderde (de nieuwe `scenario.rgs` vallen buiten hun globs); de hoofdsessie draait
de volle set op de unie.

**Acceptatie (c).** Een scenario met niet-lowerbare vormen (peildatum met tijd; afwijkende scope), via
`make ir … CASES=…`: niets geschreven, exit 1 (make: 2), met de zinnen
`scenario: de peildatum '01-03-2026 09:00:00.000' draagt een tijd, maar een casebestand draagt als peildatum alleen
een dag (asOfDate) [<tmp>/scenario.rgs:2]` en `scenario: de scenario's van één document lopen tegen één
IR-document, maar het scenario 'kaal' toetst andere regels dan het scenario 'geweigerd'; zet scenario's met een
andere scope in een eigen document [<tmp>/scenario.rgs:32]`.

**De zinnen.** Letterlijk in `grammar/DIVERGENCES.md` § "Scenariotoetsen", onder "Refusals" (de volledige lijst,
prefix `scenario: `, plaatshouders tussen punthaken), plus de CLI-zin `scenario: het document draagt geen scenario, dus er
valt geen casebestand te schrijven`. Typefouten dragen de zin van de regelpijplijn na de positie, bv.
`scenario: het attribuut 'vondstgewicht': toekenning: assignment across incompatible units`.

### Voorgestelde registertekst

**`spec/CONSTRUCTS.md`, rij 37 (Notes), toevoegen:** "**Landed in this repository (2026-09-26, card
`scenariolaag-in-uchar`):** the scenario layer is a documented extension of the RegelSpraak frontend
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Scenariotoetsen"), reachable only through `testDocument`: a
scenario document lowers to `uchar-case-v0` case files beside the IR document they run against, and its scope
lowers to that IR document (the named forms it carries), never to the case format — no schema and no executor
changes. Six goldens carry a `scenario.rgs` whose lowering reproduces their pinned case files and `ir.json`
byte-identically (gate `check-scenario`). The runner is the replay: the Drools backend's replay fills the report
record `ScenarioResultaat` (per-expectation verdicts, fired rules, coverage, MARKER)."

**`spec/PLAN.md`, in de alinea over de golf vóór de verhuizing, na "(`scenariolaag-in-uchar`; maintainer decision:
it is semantics over the IR, so it lives here)":** "— landed 2026-09-26: a scenario is a case file written in
RegelSpraak, lowered by the frontend's `--cases` mode, its scope a filter on the IR document; six goldens regenerate
their pinned cases through it (gate 15), and the replay fills `ScenarioResultaat`, so the distribution's scenario
CLI is the frontend lowering plus the replay facade".

**`CLAUDE.md`, gate 15 (nieuw):** "15. `scripts/check-scenario.sh` — the scenariotoetsen extension of the
RegelSpraak frontend (`frontends/regelspraak/grammar/DIVERGENCES.md`, "Scenariotoetsen"): every corpus golden
carrying a `scenario.rgs` beside its `bron.rgs` regenerates, through the frontend's `--cases` mode, exactly its
pinned case files (the same names, none missing and none extra) byte-identically, and its `ir.json`
byte-identically — without a scope the production route's IR document, with one the forms the scope carries; the
handwritten expectations are the anchor, never the lowering. An empty scenario-golden set is a failure; floors
`EXPECTED_MIN_SCENARIO_GOLDENS` and `EXPECTED_MIN_SCENARIO_CASES` (measured 2026-09-26: 6 and 29). Gate 4 stays
the schema gate of those case files and gates 5, 6 and 12 their replay. CI leg unconditional and blocking
(jar-only, like gate 10)."

**`.github/workflows/gates.yml` (beschermd), nieuwe job:**
```yaml
  scenario:
    name: "Gate 15: RegelSpraak scenario documents (unconditional, blocking)"
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - name: cache the ANTLR + JUnit tool jars
        uses: actions/cache@v4
        with:
          path: frontends/regelspraak/lib
          key: regelspraak-jars-antlr-4.13.2-junit-1.10.2
      - name: check-scenario
        run: bash scripts/check-scenario.sh
```

### Open punten

1. **Sleutelvolgorde van het caseschema.** `spec/schema/uchar-case-v0.schema.json` zet `characteristicTimelines`
   vóór `characteristics`; alle 51 corpusinstanties met het veld schrijven het erna, en gate 2 houdt casebestanden
   niet aan de volgorde (alleen `ir.json`). De lowering volgt het corpus. Voorstel (beschermd pad, geen bestand
   verandert): de twee `properties` in het schema omwisselen, zodat "de canonieke volgorde van gate 2" voor
   casebestanden het corpus is; daarna zou de gate-2-volgordetoets ook over casebestanden kunnen lopen.
2. **Bestandsnamen buiten de slugregel.** 74 `case.json` en de ongesluggede `case<n>.json` in het corpus zijn met
   `case<n>-<slug>.json` niet te reproduceren; zulke goldens krijgen pas een scenariodocument na hernoeming.
3. **Datum-tijd-lexeem.** De lowering schrijft altijd drie fractiecijfers (de lexeemvorm van een IR-literal); 31
   van de 50 gepinde dateTime-waarden laten een nulfractie weg. Goldens met zulke waarden reproduceren niet
   byte-gelijk zonder regelwijziging.
4. **Diagnoses in een gescoped IR-document.** `metScope` neemt alle waarschuwingen van het volle model mee; een
   markeerwaarschuwing van een weggelaten regel blijft dan staan (WF-113 (b) eist alleen marker → waarschuwing, dus
   de verifier weigert het niet). Geen getuige raakt het; filteren vraagt een koppeling diagnose → vorm die het
   model niet heeft.
5. **Niet geport, met reden.** `ScenarioOrakelTest` (64 triples uit de oude executiesuites): in uchar zijn de
   gepinde casebestanden het orakel en gates 5/6/12 de replay. `ParametersetScenarioTest` en de `externeParameters`-
   omweg van de parameterpoort: parametersets zijn distributie (U6). De proefmutaties van de rood-eerst-meting zijn
   niet als tests vastgelegd (logs in (buiten de repo)).
6. **`ScenarioResultaat` veranderde van vorm** (`positie` i.p.v. `line`, `DekkingSoort` zonder MARKER): de
   distributie leest het record in de nieuwe vorm.

## Lens en fixronde (2026-09-26, hoofdsessie)

Lens frontend (unie cfa643d0): EERST FIXEN. Gemeten: de regeneratie van de zes goldens herhaald (29 cases en 6
ir.json byte-gelijk); een eigen scenario met elke verwachtingssoort en `gebaseerd op` (latere waarde vervangt de
eerdere; ref, clingo en beide Drools-takken spelen het); een kring en een onbekende situatie worden geweigerd; wat
niet in het caseformaat past weigert met zin; de grammatica raakt geen productiedocument (gate 10: 95 goldens
byte-gelijk; de pariteitszone alleen geteld); de gate-mutanten (waarde, set, scenario weg, vloeren) alle rood;
rood-eerst op 137 tests met acht mutaties. BLOKKEREND B1: de lowering toetste periodegrenzen niet aan het raster
van de declaratie en schreef casebestanden die ref en clingo weigeren ("segment boundaries must sit on the declared
granularity grid") — in de fixronde (branch fix/frontend): elke grens getoetst op granulariteit, aantal en vast
startpunt, weigering met zin, getuigen per soort. Adviezen in de fixronde: de scope-kopie bewaakt (A1), de
scope-getuige benoemd (A2), diagnoses van weggelaten vormen (A3), de casemodus schrijft pas na de emissie en ruimt
op (A4), één regelnummer per melding (A5), geen tweede melding na een geweigerde peildatum (A6), tellingen in
DIVERGENCES met de anchor-deelverzameling (A7), één getalnormalisatie (A9), velden zonder lezer weg (A10),
dubbele helpers (A11), zeventien weigerzinnen zonder getuige (A12); A13 (het Drools-rapport stopte bij één
RuntimeException) op branch fix/graaf; A18 het gatenummer wordt 16 (hoofdsessie). Datum-tijd (open 3): de
grammatica eist in de bron altijd drie fractiecijfers, dus de lowering volgt de auteur; handgeschreven cases zonder
fractie komen daarom niet byte-gelijk uit een scenario — vastgelegd in DIVERGENCES.
Uitkomst fixronde (branch fix/frontend, 29 commits, gate 10 met 895 tests en 98 goldens byte-gelijk; nakomer fix/frontend2): B1 gefixt (periodegrenzen aan het raster via `kern/VastStartpunt.opWand`; kenmerkperioden blijven dag-precies, standaardantwoord); A1–A7, A9–A12 gefixt; A13 en het backenddeel van A10 op branch fix/graaf (het rapport faalt per case; `uitleg`, `metUitleg` en `Vuring.waarde` weg). Onderweg vier stille fouten gefixt: een herschaling die gedeclareerde breuken afrondde, twee dagsoorten met dezelfde spelling, de eenheid in "Bedrag met eenheid €" die nooit gelezen werd, een telkwantor over twee relaties. Open (featurevragen): een afwezige parameter heeft geen scenariovorm; casebestandsnamen buiten de slugregel.
