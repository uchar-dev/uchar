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
