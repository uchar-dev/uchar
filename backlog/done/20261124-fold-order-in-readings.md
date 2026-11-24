# fold-order-in-readings: de lezing haalt de volgorde van een vouw uit eigen feiten

*Besluit van de eigenaar, 2026-11-24: weg (c) uit open punt 5 van `contributor-filter-verdict`, op advies van een extern
model (xhigh); vóór de bouw door hetzelfde model gelezen, de punten onderaan verwerkt. In uitvoering sinds 2026-11-24.*

Goal: in de Drools-backend verandert een build met lezingen (`DroolsBackend.build(…, readings)`) de productieregels en
-feiten niet meer, ook niet voor een vouw in een document met `inferFromEmpty: false`; de volgorde van de leden die de
lezing van ch. 5.12 nodig heeft (`reached`/`unreached`), haalt de lezing uit eigen leesfeiten of uit het leespad. Daarmee
kan de hele fixture `ref/test/fixtures/strict-empty/contributor-filter/` (vier regels) een golden zijn die gate 17
vergelijkt.

Acceptance: `ReadingRulesFixpointWitnessTest` ongewijzigd en groen, ook op een strikte golden met een ongeordende vouw
(de belofte van gate 12 in `CLAUDE.md`: "whose production build is byte-equal to the one without"); geen nieuwe weigering
in productie (in het bijzonder speelt `jutterswet-ketenkwantor`, en `jutterswet-muntketen`, met `inferFromEmpty: false`
zoals nu); de golden `jutterswet-zwaarkeurder-strikt` krijgt de andere drie regels van de fixture (`keurder`,
`nauwgezet`, `grenskeurder`) met hun casussen, of een nieuwe golden draagt ze, en gate 17 vergelijkt hun lezingen op
beide takken; `make gates` groen.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/` (`TmsTranspiler`, `SessionReaders`,
`MemberTest`, `FoldOpen`, `FilterFault`, `Watch`, `ComputeHelpers`, `plan/` en beide renderings, en wat de lezing verder
raakt), `backends/drools/.../cases/SessionDump.java` en `DroolsBackend.java`, `refusal/DroolsCode.java` (een code die
vervalt wordt `RETIRED`), de tests en pins van de lezingen en de DRL-tekstpins, `corpus/golden/jutterswet-zwaarkeurder-strikt/`
(of een nieuwe golden), `scripts/check-drools.sh` (`EXPECTED_PLAYED`, een gelijkheid, als er een golden bijkomt),
`scripts/check-noop.sh` als een casus een survivor is, `backends/drools/README.md`, de proza die nu de oude beperking noemt (`spec/RUNTIME-SEMANTICS.md` rond 8373, `ref/test/fixtures/strict-empty/NOTE.md`, commentaar in `scripts/check-readings.sh` en `check-drools.sh`), deze kaart. Mandaat van de eigenaar voor de beschermde paden die het raakt (`scripts/`, de proza in `spec/`).

Out of scope: de spec (ch. 5.12 en 5.11 zeggen al dat de volgorde van een ongeordende vouw onzichtbaar is voor de uitkomst
en wat de lezing schrijft); ref en clingo; de toolchain.

## Aanleiding

Gemeten door de bouwer van `contributor-filter-verdict` en nagekeken door het externe model:

- Sinds `9b1986357` ("Drools: een kwantorvouw onder een open filter, en de geankerde lezing") zet `TmsTranspiler` in een
  build met lezingen (`positions != null`, `readings()`) de volgorde van de leden van een vouw vast in productieregels en
  -feiten: `MemberTest.place` (`[0]` tegenover `[0, k]`), en volgens de review ook `listFields()` (dus `ListPlace`-feiten in
  de invoer en de feiten van de dump), `FoldOpen.place`, bij een pad met twee routes het aantal `Contribution`- en
  `FoldOpen`-feiten (`range.perRoute()` in plaats van `range.join()`), en bij geneste open vouwen het `OpenMember` dat de
  binnenste vouw omhoog geeft (rond regel 6710: `placed = positions != null && !openable.isEmpty()`; rond 7677:
  `placed = ordered || positions != null && (filterEnds.open() || matrixEnds.open())`).
- Op een vouw waarvan de matrix niet kan falen of deponeren, verschilt de productie daardoor met en zonder lezingen, en
  wordt `ReadingRulesFixpointWitnessTest` rood. Daarom staat in `jutterswet-zwaarkeurder-strikt` alleen de regel van de
  fixture waarvan de matrix kan falen.
- Weg (a), de witness zonder die volgorde laten vergelijken, verzwakt een test en maakt de belofte van gate 12 onwaar.
  Weg (b), de volgorde ook in productie vastleggen, laat productie `jutterswet-ketenkwantor` met `inferFromEmpty: false`
  weigeren (DROOLS-318), waar de uitkomst volgens ch. 5.11 niet van de volgorde afhangt.

## Voorstel (weg c)

1. De productieregels en -feiten hangen niet meer af van `positions`: wat alleen de lezing nodig heeft, staat in
   leesregels of leesfeiten die de witness als leesfeit herkent (zoals de bestaande leesfeiten), of de lezing leidt de
   volgorde af uit wat de sessie al kent (`SessionReaders`, de invoer `Input.lists()`), zonder `ListPlace`-feiten in de
   productie-invoer die productie niet leest.
2. Geneste ongeordende open vouwen (`fold-in-a-filter`, `reading-nested`): het `OpenMember` dat een binnenste vouw omhoog
   geeft, mag niet van de lezingsbuild afhangen; ontwerp dat mee.
3. **Mee in deze kaart:** `FilterFault` bewaart `fault.getMessage()` als `sentence`; bij een vouw over een set waarvan het
   filter kan falen en onbepaald kan zijn (`filterFaultRule` met `breaksOff = true`, rond regel 6842) is `fault` een
   `OpenRead` en bevat die zin de JVM-identiteit (`read of open Vondst@<hex> …`), die `SessionDump` als gewone tekst
   schrijft. Schrijf daar het object bij zijn naam, zoals `SessionDump` dat sinds `7e7e94365` voor `Undetermined` en
   `OpenRead` doet; met een test.
4. De andere drie regels van de fixture als golden (zie Acceptance), met handberekening; de fixture blijft voor wat de
   golden niet pint (casus 4, de rode casus).

## Uit de lezing vóór de bouw (extern model, nagerekend) — eisen

- **Wat de witness filtert.** `ReadingRulesFixpointWitnessTest` filtert alleen `fact Reading(` en de secties `firings`,
  `trace-`, `facts`, `reading`, niet een ander feittype en niet de invoer of een global. Wat alleen de lezing nodig heeft,
  mag dus geen nieuw feittype, geen `ListPlace` in de invoer en geen global zijn. Voorkeur: houd de lijstvolgorde bij op
  `Watch` (de sessie-listener, die de dump niet ziet en die `SessionReaders` al leest), uit `Input.lists()` die
  `DroolsBackend` in elke build al berekent; leesregels, als die toch nodig zijn, inserten alleen `Reading`-feiten.
- **Wat blijft.** `FoldOpen.Part` (filter vóór waarde) en het selectienummer van een unie hangen niet van de lijstvolgorde
  af en blijven op `FoldOpen`, in productie en lezing gelijk. Voor een kwantorvouw die niet geordend gelezen wordt
  (`!tested`) ordent `SessionReaders` de `FoldOpen`- en `MemberTest`-feiten vóór de rendering en de bijdragers.
- **Geneste ongeordende open vouwen** (`fold-in-a-filter`, `reading-nested`): het open lid dat een binnenste vouw omhoog
  geeft, mag niet van de agendavolgorde afhangen, anders verschilt het tussen exec-model en DRL-tekst al zonder lezingen
  (`BranchDumpsCorpusWitnessTest`); de lezing leest de binnenste vouw in vouwvolgorde uit het leespad. Ontwerp dat mee.
- **Veel-op-veel.** `OpenReadingsWitnessTest.a_many_to_many_fold_is_refused_with_readings` (DROOLS-318 met lezingen op
  `jutterswet-muntketen` strikt) blijft groen: de weigering met lezingen blijft waar zij nodig is. Valt een code weg
  (DROOLS-372, `LIST_PLACE_MISSING_OPEN_MEMBER`), dan krijgt zij `Kind.RETIRED` (gate 12 houdt het register).
- **Goal precies.** "Productie verandert niet" betekent: productiefeiten en -invoer met en zonder lezingen gelijk (de
  witness van gate 12 en `ReadingRulesObserverWitnessTest`); `Reads.navigated` (rond 8724) legt in een lezingsbuild extra
  bindingen op een productieregel zonder productiefeiten te veranderen; verplaats die naar het leesplan als dat kan, of
  noem het in het `Result:`.
- **Extra acceptatie.** (1) Een JUnit-witness die de dump-gelijkheid met en zonder lezingen ook op de fixtures van
  `open-values/` en `open-readings/` toetst (het corpus dekt waardevouwen, unies, ongeordende kwantorvouwen, twee routes
  en geneste vouwen niet), en bij geneste vouwen ook beide takken; (2) een test voor `jutterswet-ketenkwantor` met
  `inferFromEmpty: false` (speelt in productie); (3) `FilterFault` met een `OpenRead` schrijft in de dump geen
  objectidentiteit, op beide takken gelijk (`SessionDumpWitnessTest`; let op `FilterFault.compareTo`, dat de zin als
  tie-breaker gebruikt).
- **De golden.** Komen de drie regels bij `jutterswet-zwaarkeurder-strikt`, dan verandert de verwachting van casus 2 en
  vervalt zijn regel in `scripts/check-noop.sh`; een nieuwe golden zet `EXPECTED_PLAYED` 386 → 387. Kies en noem het.

## Result

**Bouwer, 2026-11-24** (met mandaat van de eigenaar voor `scripts/` en de proza in `spec/RUNTIME-SEMANTICS.md`; niet
gemerged, de kaart blijft in `doing/`). Ontwerp, meting en de lezing van het externe model ervoor:
`werk/fold-order-in-readings/ONTWERP.md` (§5: wat overgenomen is en wat niet, met de reden),
`werk/neon-review/antwoorden/xhigh-ii-fold-order-design.md`.

Commits:

- `1f7420cfb` `FilterFault`: een `OpenRead` bewaart geen zin meer (die noemde het object bij zijn JVM-identiteit en de
  dump schreef haar als tekst); `equals` vergelijkt haar open lid, `compareTo` zet een ontbrekende zin vóór. Test
  `SessionDumpWitnessTest.a_filter_fault_of_an_open_read_writes_no_identity` (beide takken, rood op `62c393c94`).
- `78baf8abb` de kern. Productie bindt geen lijstplaats meer voor een open lid (`contributionRules`: `placed` weg;
  `quantifierContributions`: `placed = ordered`); een `FoldOpen` draagt in elke build `[deel, selectie]`. Een vouw die
  de regels zonder volgorde lezen, noemt bij een onbepaald oordeel de vouw op haar onderwerp: `OpenMember` kreeg een
  component `fold` (`OpenMember.ofFold`), gegooid door `standingCount`, `quantifierVerdictOverOpen` en de nieuwe
  `QUANTIFIER_READ_UNORDERED` (`tested && !ordered`; de `MemberTest` van zo'n vouw draagt vouw en onderwerp,
  `ComputeHelpers.filtered`). Zo hangt het open lid dat een binnenste vouw omhoog geeft niet meer van de agenda af
  (gemeten: `reading-nested/case3` verschilde zonder lezingen tussen de takken, `K5`/`K6`, `facts 434`/`435`). De
  lezing lost een vouwoorzaak op uit de feiten van die vouw, recursief (`SessionReaders.named`), in vouwvolgorde: deel,
  selectie, dan de plaats in het bereik dat `Watch.range` loopt over de lijsten die `DroolsBackend.insert` de watch geeft
  (`TmsTranspiler.readingListFields`, `noteFoldOrder`/`FoldOrder` met kwantor en drempel); de bijdragers van zo'n
  kwantorvouw in die volgorde. Geen `ListPlace` meer in de invoer van een build met lezingen. `DROOLS-318` met lezingen
  blijft (de wandeling van een veel-op-veel-bereik weigert zoals `perRoute`). `DROOLS-372` → `RETIRED`; de routes
  `CONTRIBUTED` en `JUDGED` vervielen (een open lid heeft altijd zijn selectie). `SessionDump` sorteert een lijst
  `MemberTest`s bij gelijke plaats op tekst. Nieuwe witness `OpenFixturesFixpointWitnessTest`; test
  `OpenReadingsWitnessTest.a_strict_quantifier_fold_over_a_many_to_many_relation_plays_without_readings`
  (`jutterswet-ketenkwantor` strikt speelt zijn vier casussen op beide takken, met lezingen `DROOLS-318`).
  Bijgewerkte tests (de taak verandert wat ze toetsen): `FormsPlan` (de open regels met hun selectie, de varianten "met
  plaats" van de oude build met lezingen weg, `contributedAt`/`judgedAt`/`contributionAt`), `RouteWitnessTest` (routes
  `CONTRIBUTED`, `JUDGED` weg, `FILTERED` met vouw en onderwerp), DRL-pin `jutterswet-zwaarkeurder-strikt` (één regel:
  `filtered(…, "zwaarkeurder: vouw 0", $vouwonderwerp, …)`). `backends/drools/README.md` beschrijft de huidige stand.
- `096a57797` `Reads.navigated`: de binding van het bereikte object én zijn slot staan alleen in de leesregel
  (`readingOnly`, `readingSlots`), zodat de regel van de vorm ook in slotnummers die zonder lezingen is. Nieuwe witness
  `ReadingRulesPlanWitnessTest`: de DRL-tekst van de regels zonder leesregels is met en zonder lezingen gelijk (vóór
  deze stap 8 documenten verschillend, na 0).
- `6dfdaf810` golden `jutterswet-keurder-strikt`: regels 0–2 van de fixture (keurder, nauwgezet, grenskeurder) met
  casussen 1–3 uit `scenario.rgs`, handberekening en lezingen in `NOTE.md` (nagerekend met `runner --trace`), DRL-pin;
  `EXPECTED_PLAYED` 388 → 389 (main stond al op 388); proza in `spec/RUNTIME-SEMANTICS.md` (het voorbeeld en "The
  artifacts"), `ref/test/fixtures/strict-empty/NOTE.md`, het commentaar in `check-readings.sh`. `jutterswet-zwaarkeurder-strikt`
  en `check-noop.sh` ongewijzigd.
- `25d8668f2` fixture `open-readings/reading-shared-field` met pins van ref: vondsten en klachten onder één veld
  `vinder`, lijsten in een andere volgorde dan de objecten; beide takken noemen `"V2"`/`"K2"` zoals ref (in
  objectvolgorde zou het `V1`/`K1` zijn).

Goal en acceptatie: productiefeiten en -invoer met en zonder lezingen gelijk (`ReadingRulesFixpointWitnessTest`
ongewijzigd groen, nu ook op de nieuwe golden; `OpenFixturesFixpointWitnessTest` op 95 fixtures), de productieregels
ook (`ReadingRulesPlanWitnessTest`); geen nieuwe weigering in productie; gate 17 vergelijkt de drie regels op beide
takken.

Getallen (oud → nieuw): `EXPECTED_PLAYED` 388 → 389; JUnit gate 12 2066 (main, afgeleid: 2070 op `6dfdaf810` min de vier nieuwe tests daar) → 2071; nieuwe vloeren
`OpenFixturesFixpointWitnessTest` 95 documenten, 417 casussen, 2451 lezingen per tak, `ReadingRulesPlanWitnessTest`
487 documenten; `ReadingRulesFixpointWitnessTest` vergelijkt 389 goldens, 1073 casussen, 8399 lezingen per tak (vloeren
261/706/3635 ongewijzigd); gate 17: 390 goldens en 1075 casusbestanden per tak, 150 bijdragers van ref niet bereikt
(was 148), 11 met een onbepaald filter (was 4). Geen vloer verlaagd.

Gates (elk apart, na elkaar; logs in `werk/fold-order-in-readings/gates/`):

- 1 `check-self-contained: PASS`
- 2 `check-ir-schema: PASS (… 513 file(s) validate and round-trip, floor 192; 65659 object(s) in 1159 document(s) in the
  schema's key order, 2 in no subschema)` (1–8 en 10 op `6dfdaf810`, 13–17 op `25d8668f2`)
- 3 `check-verifier: PASS (… 513 golden clean; 646 negative rejected for their declared rule, floor 568; …)`
- 4 `check-case-schema: PASS (schema valid; 146 self-test row(s); 1302 case file(s) validate)`
- 5 `check-oracle: PASS (ref suite 4232 worked examples, floor 489; 1201 cases replayed exactly, floor 427; preservation
  …: 281 goldens and 787 cases play, …; 7 carrying the property already, … jutterswet-keurder-strikt …; monotonicity:
  816 cases, 2697 runs without one input compared, floor 2652, …)`
- 6 `check-clingo: PASS (backend suite 4879 checks, floor 4797, its 272 units each once in 32 shards; 1171 cases replay
  exactly through clingo …; 10 golden(s) in the refusal channel …)`
- 7 `check-noop: PASS (1156 cases refute a do-nothing executor; 45 pinned survivors pin defeasible standing)`
- 8 `check-red: PASS (101 red cases fault observably in ref and clingo, floor 44; …)`, Drools leg PASS
- 10 `check-regelspraak: PASS — JUnit suite green (1716 tests, floor 173); 227 rgs golden(s) re-emit byte-identically
  (floor 80)`
- 12 (op `25d8668f2`) `check-drools: PASS`, `played 389`, `DRL-text branch played 389`, `JUnit 2071 tests, floor 1573`;
  `check-drools-codes: PASS — 373 codes, numbered 001..373, every one raised in the sources but the 41 retired, …`
- 13 `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays
  reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, …; 0 pinned survivors)`
- 15 `check-graph: PASS` (route leg: 227 goldens with a source.rgs, 86 unit tests)
- 16 `check-scenario: PASS — 54 scenario golden(s) (floor 7) regenerate 178 case file(s) (floor 31) and their ir.json
  byte-identically`
- 17 `check-readings: PASS (ref traced 1201 golden case files, …; on both branches 390 goldens and 1075 case files
  compared, 8091 readings, 25857 node lines of which 4143 unreached, 1019 with contributors, …, 150 contributors of ref
  not reached; …; deviations 0, end-state deviations 0; …)`
- Niet gedraaid: 9 (catala), 11 (toka), 14 (flogic): deze kaart raakt hun bronnen niet. `make gates` draait de
  hoofdsessie.

**Herstelronde, 2026-11-24**, na de review van het externe model (`werk/neon-review/antwoorden/xhigh-jj-fold-order-build.md`,
bevindingen 1 en 2 en het punt over `FormsPlan`), vanaf `0b1e18fc8`. Commits:

- `449e09ff7` een sessie die `DroolsBackend.insert` niet vulde, heeft geen lijsten op haar watch. Zij vuurt tot het
  fixpunt zolang elk lid dat de regels lezen gevuld is (`ComputeHelpers.input` faalt alleen op een lid zonder waarde),
  ook in een strikt document met een kwantorvouw die de regels zonder volgorde lezen; `readingsOf` brak dan af in
  `SessionReaders.rangeOrder` met een `IllegalStateException` zonder registercode, met een zin over een open lid. Nu
  weigert het met de nieuwe code `DROOLS-374` (`READINGS_WITHOUT_INPUT_LISTS`, `Kind.CONTRACT`, parameter `fold`), met
  meldingen in beide catalogi, zoals `DROOLS-351` een verwijderde watch weigert; het commentaar daar, de javadoc van
  `readingsOf` (`DroolsBackend`, `SessionReaders`) en `backends/drools/README.md` zeggen het. Test
  `OpenReadingsWitnessTest.the_readings_refuse_a_session_insert_did_not_fill` (`jutterswet-keurder-strikt`, beans met
  `session.insert`, alle leden gevuld, beide takken): de fire haalt het fixpunt, `readingsOf` weigert met `DROOLS-374`;
  rood op `0b1e18fc8` met de oude zin. `rangeOrder` is de enige plek die `Watch.range` en dus de lijsten van `insert`
  leest (via `openInOrder` en `inOrder`); de `ListPlace`-feiten die productie leest, hebben al hun codes
  (`DROOLS-357`..`359`).
- `6fdf5cbad` `FormsPlan` draagt naast `QUANTIFIER_READ` een `QUANTIFIER_READ_UNORDERED` in dezelfde regel; `FORMS`
  toetst beide aanroepen (`ComputeHelpers.quantifierRead(`, `ComputeHelpers.quantifierReadUnordered(`). Twee
  kwantorlezingen in één regel nummeren hun functienamen, dus het fragment `kwantorlezing_alleTermen(` werd
  `kwantorlezing1_alleTermen(` en `kwantorlezing2_alleTermen(`.
- `0d93c7454` fixture `open-readings/reading-nested-quantifier` (strikt document, Jutterswet-namen): de kwantorvouw
  "de score van alle keuringen van de Vondst die aan de volgende voorwaarde voldoen: hij is officieel groter is dan 5"
  (filter open, matrix kan niet falen of deponeren) in het filter van de som `keurgewicht` en van de kwantorvouw
  `streng`, beide zonder volgorde gelezen; vier casussen met vondsten en keuringen in een andere volgorde dan hun
  objecten. Ref noemt `"K3"` (casus 1) en `"K4"` (casus 2) open, in objectvolgorde zou het `"K2"` en `"K1"` zijn;
  casus 4 stopt bij `"V1"` na het open filter van `"V2"`, `"V3"` `unreached`. Pins van `runner --trace`; de IR is
  gemaakt uit `source.rgs` en `scenario.rgs` met de frontend, waarna de vouw van een hulpregel in de twee filters is
  gezet (`werk/fold-order-in-readings/reading-nested-quantifier/`, `splice.py`), verifier-schoon. Loopt mee in
  `OpenReadingsWitnessTest.wv_5_12_open_quantifier_fold` (beide takken gelijk aan ref) en in
  `OpenFixturesFixpointWitnessTest`. Daarmee loopt de tak `order.quantifier() != null` van `SessionReaders.named` ook
  genest; het vroegere open punt 1 (geen fixture met een geneste kwantorvouw zonder volgorde) vervalt.
- `ONTWERP.md` §5: de bewering dat een sessie zonder `insert` onbereikbaar is, is eruit; §2b punt 4 noemt de code. Het
  `Result:` hierboven bevatte die bewering niet.

Getallen (oud → nieuw): `OpenFixturesFixpointWitnessTest` 95 → 96 documenten, 417 → 421 casussen, 2451 → 2459
lezingen per tak (vloeren mee verhoogd); `ReadingRulesPlanWitnessTest` 487 → 488 documenten (vloer mee); JUnit gate 12
2071 → 2072; `check-drools-codes` 373 → 374 codes; `OpenReadingsWitnessTest` 16 → 17 tests. Geen vloer verlaagd.

Gates (elk apart, na elkaar, op `0d93c7454`; logs in `werk/fold-order-in-readings/gates2/`):

- 1 `check-self-contained: PASS`
- 8 `check-red: PASS (101 red cases fault observably in ref and clingo, floor 44; 179 ref and clingo code checks report
  the case's code, floor 82, 23 listed as divergence; 297 pinned parameter checks confirmed, floor 120; 47 fault kinds
  in RUNTIME-SEMANTICS ch. 5.7b, floor 31)`; `Drools leg PASS (82 fault-pinned, 82 of them reporting the case's code,
  134 pinned parameter checks confirmed, floor 28; 19 not-carried with reason, 19 of them EXCLUDE pinned by their
  DROOLS code)`
- 12 `check-drools: PASS`, `played 389`, `DRL-text branch played 389`, `JUnit 2072 tests, floor 1573`;
  `check-drools-codes: PASS — 374 codes, numbered 001..374, every one raised in the sources but the 41 retired, …`
- 17 `check-readings: PASS (ref traced 1201 golden case files, …; on both branches 390 goldens and 1075 case files
  compared, 8091 readings, 25857 node lines of which 4143 unreached, 1019 with contributors, …, 150 contributors of ref
  not reached; …; deviations 0, end-state deviations 0; …)`
- Niet opnieuw gedraaid: 2–7, 9–11, 13–16: de herstelronde raakt alleen de lezingen van de Drools-backend, haar
  register en haar tests; de fixture staat buiten het corpus (de verifier van de workspace noemt haar schoon).

Open punten:

1. De dump schrijft elke `OpenMember` nu met `fold=empty` (een component meer); geen pin bevat er een.
2. Toolchain: `ConditionReading` en de publieke API veranderden niet; `OpenMember` kreeg een component, `Watch.op` een
   argument (de lijsten). De toolchain gebruikt `DroolsBackend.insert`, dus haar lezingen krijgen de lijsten, en zij roept
   `OpenMember` en `Watch.op` nergens zelf aan (nagekeken door de hoofdsessie en de review). Een directe beller die de beans zelf in de
   sessie zet, krijgt bij zo'n vouw nu `DROOLS-374`.
3. Geen rij voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: geen wire en geen semantiek veranderde.

**Landing (hoofdsessie, 2026-11-24).** Vóór de bouw las een extern model (xhigh) de kaart en daarna het ontwerp van de
bouwer; na de bouw vond het geen semantische fout, wel twee punten (een lezing van een sessie die `insert` niet vulde,
faalde zonder registercode, en een geneste ongeordende kwantorvouw had geen fixture), die de herstelronde oploste
(`DROOLS-374`, `open-readings/reading-nested-quantifier`); na de herstelronde: akkoord. `make gates` op `1226fd6b0`: alle
zeventien gates groen in 363 s (red=0).
