# reading-read-objects: de lezing noemt bij een gelezen lid het object waarop zij het las

*Voorwaarde voor deel 1 van de toolchain-kaart `explanation-read-objects-and-displaced` (van de werkbank, op besluit
van de eigenaar, 2026-10-27). Ontwerp: Fable-advies in `uchar-dev/research/uitleg-gelezen-objecten-2026-10-27/`.*

## Aanleiding

De uitleg van de toolchain zet bij elke waarde die een voorwaarde of een berekening las, de waarde uit de lezing van
RUNTIME-SEMANTICS ch. 5.12 (`DroolsBackend.readingsOf`). De werkbank wil bij elke gelezen waarde de zin vinden die de
schrijver van die waarde uitlegt; die zin begint met het object en de naam van het lid. Bij een lezing van een eigen lid
is dat object het object van de lezing. Bij een lezing over een rolpad ("de herlevingsgrens van het recht van het
Inkomen", "de jutter van de Vondst ingezetene is") is het het object dat het pad bereikte, en dat staat nergens: een
knoop van ch. 5.12 draagt een oordeel of een waarde met eenheid, en bij een vouw zijn bijdragers, maar geen object
(`ConditionReading.NodeReading`). De toolchain heeft geen navigatiesemantiek van zichzelf (haar `CLAUDE.md`), dus kan
zij het pad niet zelf lopen.

Gemeten op de vijf service-verzoeken van v2/herleving-ww en s01 (toolchain `b4f3ab94`, uchar `c6a45d89`, regels
`ece012e`; `werk/toolchain-2026-10-27/read-objects/meting/`): 224 attribuutwaarden over een pad (155 in een voorwaarde,
69 in een berekening) en 17 kenmerkchecks over een rol, naast 425 lezingen van een eigen lid.

## Doel

Ch. 5.12: een knoop die een lid van één object leest — een attribuutlezing, ook over een pad, en een kenmerkcheck, ook
over een rol; de lijst van soorten stelt het ontwerp vast — en die de evaluatie bereikte, noemt het object waarop zij het
lid las. Waar een link op het pad ontbreekt, is er geen object (de lezing is leeg, ch. 5.8 regel 1). De canonieke tekst
schrijft het, ref (`runner --trace`) en de Drools-backend (beide takken, de build met lezingen) leveren het, en gate 17
vergelijkt het. `ConditionReading.NodeReading` draagt het als bean, zoals een bijdrager zijn lid draagt.

Een knoop die per lid van een vouw gelezen wordt, heeft geen eigen lezing en blijft zo; de bijdragers van een vouw
noemen hun leden al.

## Acceptatie

- (a) Rood eerst: rijen in de getuigen van de lezingen (ref `ref/test/`, Drools `ReadingsWitnessTest` of een eigen
  klasse) voor een eigen lid, een pad van één stap, een pad van meer stappen, een afwezige link, een kenmerkcheck over
  een rol, een lezing in de waarde van een berekening, en een pad over een relatie die een creatie legt; elk faalt op de
  code van nu.
- (b) Gate 17 vergelijkt het object per knoop tussen ref en beide takken; een getuige over het corpus houdt het object
  vast aan het fixpunt (het lid op het genoemde object heeft daar de waarde van de knoop). De gemeten aantallen in het
  Result; vloeren zijn ondergrenzen.
- (c) De clausule in ch. 5.12 als voorstel, met een gewerkt voorbeeld met absolute waarden.
- (d) De productiebuild bytegelijk (`ReadingRulesFixpointWitnessTest`); `make gates` groen.

## Voorlopige besluiten (hoofdsessie, 2026-10-27, naar het Fable-advies)

*Gemarkeerd voorlopig; de eigenaar kan elk besluit omdraaien. Bron: `research/uitleg-gelezen-objecten-2026-10-27/
fable-advies.md` §1, §5, §6.*

- **D1 Route.** Het object hoort in ch. 5.12 (de canonieke tekst, ref, beide Drools-takken, gate 17), niet alleen als
  veld in de Drools-API. `DroolsBackend.reached` (dat bestaat, maar geen kenmerkcheck neemt en beans na de
  terugschrijving leest) blijft de getuige-API van `ReadingsCorpusWitnessTest`, met een overload voor een kenmerkcheck.
- **D2 Welke knopen.** `AttributeReference` (eigen lid, over een pad, als tijdlijn gelezen, onder een
  `DimensionCellRead`) en `CharacteristicCheck` (eigen lid, over een rol, onder de geheel-periodekwalificatie). Niet
  `RoleMembership`, `IdentityCheck`, een parameter, een binder, een vouwknoop of een knoop zonder eigen lezing.
- **D3 Eigen lid.** `NodeReading.object` draagt altijd het object (bij een eigen lid het object van de lezing); de
  canonieke tekst schrijft het alleen waar de knoop navigeert, zodat geen bestaande regel en geen van de drie
  uitgewerkte voorbeelden verandert.
- **D4 Afwezige link.** De tekst ` on none`, de API null; geen aparte enum. Een knoop die niet bereikt is of een fout
  draagt, noemt geen object.
- **D11 Productiebuild.** `PathState` krijgt een component `reached` in beide builds (de padstand-DRL één argument
  meer); de getuigen houden feiten en vuringen gelijk (`ReadingRulesFixpointWitnessTest`,
  `ReadingRulesObserverWitnessTest`, `BranchDumpsCorpusWitnessTest`). Geen aparte feitfamilie in de build met lezingen.

De canonieke tekst: `<form> <object> <number> <kind> <found> ["<unit>"] on <object>|none`, alleen bij een knoop die
navigeert. Gewerkt voorbeeld: `anchor/jutterswet-kustwacht` case 2 (advies §1.6).

## May touch

`backends/drools/**` buiten de productieruntime, `ref/lib/reading.ml` en wat het trace-pad van ref raakt, `ref/test/`,
een golden; `spec/RUNTIME-SEMANTICS.md` en `scripts/check-readings.sh` als voorstel; deze kaart.

## Result (2026-10-27)

Tak `card/reading-read-objects`, op `9b4939ab`. Geraakt: `backends/drools/**` buiten de productieruntime,
`ref/lib/eval.ml` en `ref/lib/reading.ml`, `ref/test/`, `spec/RUNTIME-SEMANTICS.md` en `scripts/check-readings.sh`
(voorstel: beschermde paden, de tak is het voorstel), deze kaart. Niet geraakt: `CLAUDE.md`, `spec/PLAN.md`,
`spec/CONSTRUCTS.md`, de corpus.

- `24e3a6ce` Drools: `PathState.reached`, het objectslot in de build met lezingen, `NodeReading.object`, de staart in
  `readingLines`, `DroolsBackend.reached` voor een kenmerkcheck, de getuigen.
- `926e5312` ref: de waarnemer-callback `reached`, de staart in `Reading.lines`, `WV-5.12-kustwacht` en
  `WV-5.12-gelezen-objecten`.
- `ad2cf6d3` RUNTIME-SEMANTICS ch. 5.12 (voorstel): "The object of a read", de canonieke staart, het gewerkte voorbeeld
  kustwacht, "The artifacts" en de grens van de `DimensionCellRead`.
- `e73f8a63` gate 17 (voorstel): de vloer `EXPECTED_MIN_OBJECT_LINES=746`, twee rijen van de vergelijkingsregel en vijf
  van de objectlezer.
- dit Result.

### Wat er is

**De clausule** (ch. 5.12, "The object of a read"). Een knoop die een lid van één object leest — `AttributeReference`
(eigen, over een pad, als tijdlijn gelezen, onder een `DimensionCellRead`) en `CharacteristicCheck` (eigen, ook met
`host`, over een rol, onder de geheel-periodekwalificatie) — noemt het object waarop hij het las, waar de evaluatie hem
bereikte zonder fout. Eigen lid: het object van de lezing; over een pad: wat de hops bereikten; afwezige link: geen. De
canonieke tekst schrijft het alleen bij een pad, na waarde en eenheid: ` on "<object>"` of ` on none`. Geen bestaande
regel van de tekst verandert, en geen van de drie uitgewerkte voorbeelden.

**Drools.** `PathState(subject, path, member, value, reached)`: de padstandregel "link aanwezig" geeft het object mee
dat zij al bond (`$bereikt` over invoerlinks, het laatste ketenpatroon over een gelegde relatie), "link afwezig" `null`.
In de build met lezingen bindt `Reads.navigated` in de eigen regel van een vorm naast het lid een slot `PATH_OBJECT`
(`$bereikt_<n>`): bij `PATH_STATE` het veld `reached` van hetzelfde padstandpatroon, bij `INPUT_PATH` de
referentievelden van de onderwerpbean tot het laatste. De analyse noteert per genavigeerde knoop dat slot (`noteObject`,
op de vier plaatsen die `navigated` aanroepen: `attributeRead`, `characteristicCheck`, `wholePeriodCharacteristicCheck`,
`wholePeriodPredicate`); `Site.objectSlot`. `NodeReading(…, fault, Object object, contributors)`: over een pad
`w[objectSlot]`, een eigen `AttributeReference`/`CharacteristicCheck` het object van de lezing, elke andere knoop null,
en null waar de knoop niet bereikt is of een fout draagt. `TmsTranspiler.navigates(form, node)` zegt of een knoop over
een pad leest; `SessionDump.readingLines` schrijft daarmee de staart. `DroolsBackend.reached` houdt zijn signatuur; de
nieuwe vorm `reached(model, subject, subjectType, Condition.CharacteristicCheck check, walk)` loopt dezelfde wandeling
met de weigeringen DROOLS-280/281/282.

**ref.** `Eval.observer` krijgt `reached : navigating -> string option -> unit` (`Navigated_read e`, `Navigated_check
c`), aangeroepen op de drie plaatsen die een pad lopen (`eval_expr_node` en `whole_period_operand` na `nav_target`, de
genavigeerde kenmerkcheck na `role_linked_instances`). `reading.ml`: `node.target`, gezet door de waarnemer; `lines`
schrijft de staart waar de knoop een waarde, oordeel of tijdlijn vond, en faalt luid waar een genavigeerde knoop iets
vond zonder dat zijn pad gelopen is, of waar een pad twee keer gelopen wordt.

### Getuigen, met tellingen

- `ReadingsWitnessTest.the_objects_a_reading_read_its_members_on`, beide takken: een eigen document
  (`src/test/resources/readings/read-objects`, uit de frontend met `source.rgs`), zeven vormen op Kustvakken, Posten en
  Wachters: eigen lid (diensttijd), pad van één stap in een voorwaarde, pad van twee stappen als waarde, kenmerkcheck
  over een rol van een invoerkenmerk en van een afgeleid kenmerk, een lezing in de waarde van een berekening met
  voorwaarde (niet bereikt waar de voorwaarde faalt), en een pad over de relatie die de feitcreatie `vakbewaking` legt;
  afwezige link op de eerste stap (W3) en op de tweede (W4). 20 records met objecten per tak en de canonieke tekst (61
  leesregels).
- ref `WV-5.12-gelezen-objecten`: hetzelfde document, dezelfde 61 regels. ref `WV-5.12-kustwacht`: de tabel van advies
  §1.6, uit de golden gelezen, alle 32 regels (ook vakklasse en gevarenwacht, die het advies wegliet). Suite 1423 →
  1425.
- `ReadingsCorpusWitnessTest`: per knoop die een lid leest is het object identiek aan `DroolsBackend.reached` over zijn
  wandeling na de terugschrijving (attribuutlezing en kenmerkcheck, eigen en over een pad), en geen andere knoop noemt
  er een. Gemeten 2026-10-27, per tak gelijk: 5961 objecten getoetst, 746 over een pad, waarvan 113 zonder object.
  Vloeren `EXPECTED_MIN_OBJECTS=5961`, `EXPECTED_MIN_NAVIGATED=746`.
- Gate 17: 746 knoopregels met ` on ` per tak, waarvan 633 met een object en 113 `on none`; vloer
  `EXPECTED_MIN_OBJECT_LINES=746`. `DEVIATIONS` en `READINGS_ERROR_KNOWN` leeg. Ref traceert alle 899
  golden-casebestanden zonder fout (746 regels met ` on `, 113 `on none`).

Ongewijzigd groen: `ReadingRulesFixpointWitnessTest`, `ReadingRulesObserverWitnessTest`, `BranchDumpsCorpusWitnessTest`
(de dumps van beide takken dragen nu `PathState` met `reached` en de staart in de sectie `reading`).

Tests die veranderden (gedrag dat de kaart verandert): `StatePlanWitnessTest.path_states_over_two_and_three_steps` en
`CreationPlanWitnessTest.the_path_states_over_a_chain_with_a_made_link` pinnen het plan van de padstandregels, dat nu
`reached=$bereikt`, `reached=$stap_keten_26`/`_44` of `reached=null` draagt; `FormsPlan` bouwt `InsertPathState` met de
nieuwe component. Geen test verzwakt.

### Rood eerst

Kopie op `9b4939ab` met alleen de tests (`werk/reading-read-objects/red`, `git worktree add --detach`):
- Drools, `mvn -o -q test-compile`: `ReadingsWitnessTest.java:[653,29] cannot find symbol: method object()` (location
  `ConditionReading.NodeReading`), dezelfde in `ReadingsCorpusWitnessTest.java:[228,72]`, en `[427,67] incompatible
  types: Condition.CharacteristicCheck cannot be converted to Expr.AttributeReference` (de vorm van `reached` voor een
  kenmerkcheck bestond niet).
- ref, `test_ref.exe`: `FAIL WV-5.12-kustwacht`, `FAIL WV-5.12-gelezen-objecten`, "2 failure(s)": geen regel had ` on`
  (`… "W1" 0 AttributeReference 12`).

### Mutaties (in een kopie, `werk/reading-read-objects/mut`, niet gecommit)

- M1, Drools: `reading()` geeft een genavigeerde knoop het onderwerp als object (`s.navigates() ? o : …`).
  `ReadingsWitnessTest.the_objects_…` rood (EXEC_MODEL: `@W1` waar `@P1` staat); `ReadingsCorpusWitnessTest` rood met
  1492 verschillen (alle 746 genavigeerde knopen op beide takken, "knoop 0 noemt het object W1, en de wandeling bereikt
  P1" op `anchor/jutterswet-kustwacht/case1.json`).
- M2, Drools: `linkPathStateRules` geeft het eerste ketenpatroon mee in plaats van het laatste. `ReadingsWitnessTest`
  rood (vakklasse van W1); `ReadingsCorpusWitnessTest` rood met 748 verschillen in 14 goldens over gelegde relaties
  ("noemt het object een aangemaakte RelationLink, en de wandeling bereikt een aangemaakte Rekenkamer",
  `anchor/jutterswet-rekenkamer`, `jutterswet-havenketen`, `jutterswet-budgetgrens-op`, …).
- M3, ref: de genavigeerde attribuutlezing meldt het onderwerp (`Some self.label`): `WV-5.12-kustwacht` en
  `WV-5.12-gelezen-objecten` rood (`… "W1" 0 AttributeReference 12 on "W1"`).
- M4, ref: `lines` schrijft bij een afwezige link niets: beide ref-tests rood.
- Gate 17 met M2 (alleen Drools gemuteerd, de jar herbouwd; `werk/reading-read-objects/logs/gate17-mutant-M2.log`): exit
  1, 66 (case, tak)-paren verschillen in dezelfde 14 goldens, het eerste
  `anchor/jutterswet-rekenkamer-overzee/case1-overzees.json (exec)`: ref `… "J1" 0 AttributeReference 6000 on «"richt de
  rekenkamer op"»("J1")#1`, backend `… on RelationLink(relation=String("taxatieopdracht"), …)`; daarnaast vallen de
  tellingen onder hun vloeren, `EXPECTED_MIN_OBJECT_LINES` met 324 tegen 746.

Beide kopieën opgeruimd met `git worktree remove`.

### De DRL-tekst van de productiebuild

Gemeten met de jar van `9b4939ab` en die van de tak (`werk/reading-read-objects/drldump`, de productiebuild zonder
lezingen van alle 329 goldens, 282 gebouwd, 47 met dezelfde weigering): `diff -r` geeft 80 regels in 24 goldens, alle in
de gevolgen van de padstandregels, elk één argument meer — 40 families, `insertLogical(new PathState($onderwerp,
"<pad>", <lid>, $v, $bereikt))` over invoerlinks (17) of `…, $v, $stap_keten_<n>))` over een gelegde relatie (23), en
`…, null, null))` in de regel "link afwezig". Geen andere regel, geen regel meer of minder. In de build met lezingen
krijgt de eigen regel van een vorm met een genavigeerde lezing daarnaast één binding per genavigeerd lid; geen regel en
geen feitfamilie meer.

### Kosten

Regels (`git diff --shortstat 9b4939ab..e73f8a63`): Drools main +214/−61 (9 bestanden), Drools tests +876/−18 (waarvan
±700 het IR-document van de getuige), ref lib +66/−8, ref tests +104/−1, spec +93/−4, gate 17 +50/−10. Gate 17 alleen op
16 kernen: 83 s (`werk/reading-read-objects/logs/gate17-1.log`). Volledige Drools-suite 1709 tests, 0 failures, 0 skips
(`werk/reading-read-objects/logs/mvn-suite-1.log`).

### Voorlopige besluiten van de bouwer

- **V1 `navigates(form, node)` op `TmsTranspiler`, geen veld op `NodeReading`.** De canonieke tekst moet "geen object"
  (afwezige link) onderscheiden van "eigen lid" en van een object dat toevallig het onderwerp is (een pad dat
  terugkeert); D4 houdt het record bij één `object`. Dus leest `readingLines` de navigatie uit de lezing van de build
  (het objectslot van de `Site`), zoals een afnemer haar uit de wandeling leest. Alternatief: een `boolean navigated` op
  het record; niet gedaan, omdat de eigenaar het record klein wilde (D4).
- **V2 Het objectslot alleen in de eigen regel van een vorm, terwijl haar lezing genoteerd wordt** (niet in elke regel
  van de build met lezingen, zoals advies §1.4.2). Gemeten: met de binding ook in de regels van een vouw was
  `ReadingRulesObserverWitnessTest` rood op de DRL-teksttak, `fold-filter-subject-two-spellings` case 2 en 3: de extra
  binding in de filterregel ("de afmeerdag van de schuit van de Lading") veranderde de volgorde van de vuringen (labels
  `[S2, S1, L3, L4]` tegen `[S2, S1, L4, L3]`). Een regel van een vouw heeft geen eigen lezing, dus daar is het slot
  niet nodig.
- **V3 Eigen lid naar de knoopsoort.** Een bereikte `AttributeReference` of `CharacteristicCheck` zonder objectslot
  noemt het object van de lezing; zo dragen ook de geknipte kenmerkcheck van een temporeel totaal en de
  `AttributeReference` onder een `DimensionCellRead` (in Drools altijd eigen, DROOLS-189) het onderwerp, zonder een
  notitie op elke plaats die een eigen lid compileert.
- **V4 De vorm van `reached` voor een kenmerkcheck** neemt de check zelf (`reached(model, subject, subjectType,
  CharacteristicCheck, walk)`), symmetrisch met de bestaande vorm, met de beschrijving uit het fragment van de check en
  het eindtype dat de wandeling bereikt (`navigationWalk` zonder eindtype, zoals de transpiler een check loopt), in
  plaats van advies §1.2's `(walk, endpoint, Msg)`: geen `Msg` in de publieke API, en de toolchain heeft de check en
  zijn wandeling al (`navigationBindings`).
- **V5 `InsertPathState` draagt de variabele van het object (`String reached`), geen slot**, zoals `subject`; de
  padstandregels krijgen geen extra `ReadKind`. De variabele van het objectslot in de eigen regel heet `$bereikt_<n>`,
  `n` het slot van het lid: uniek zonder `readName`, waarvan de namen geen onderliggend streepje hebben.
- **V6 Geen getuige voor de `DimensionCellRead`.** Geen casuslezer draagt dimensie-invoer en beide goldens met een cel
  hebben geen casusbestanden; de grens staat in ch. 5.12 "The artifacts". Het object van die knoop in de API (het
  onderwerp) volgt uit V3.
- **V7 Het getuigedocument komt uit de frontend** (`source.rgs` ernaast) en staat in ref inline, zoals
  `WV-5.12-gedurende-clip`.
- **V8 ref faalt luid** op een genavigeerde knoop met een uitkomst maar zonder gelopen pad, en op een pad dat twee keer
  gelopen wordt in één lezing, zoals de bestaande "evaluated twice".

### Voor de hoofdsessie

**`CLAUDE.md`, gate 17, na de zin over `EXPECTED_MIN_UNREACHED_CONTRIBUTORS`:**

```
    Since the card `reading-read-objects` (2026-10-27) a node that reads
    a member over a path of roles writes, after its value, the object the
    path reached (`on "<object>"`, `on none` where a link is absent;
    ch. 5.12, "The object of a read"), compared verbatim with the rest of
    its line; floor `EXPECTED_MIN_OBJECT_LINES=746` (measured 2026-10-27),
    and the reader of that object passes five rows of its own.
```

In dezelfde gate-tekst wordt "the comparison rule passes twelve rows of its own" "fourteen rows" (de twee rijen van een
object ernaast). Geen CONSTRUCTS-rij: geen nieuwe vorm, geen afwijking van v0. PLAN §9, voorgesteld:
"reading-read-objects (2026-10-27): een knoop die een lid van één object leest noemt het object (ch. 5.12); `PathState`
draagt het bereikte object, de padstand-DRL één argument meer; voorlopige besluiten V1–V8."

**Wat de toolchain-kaart moet weten.** `ConditionReading.NodeReading.object()` is de bean (of null); bij een eigen lid
het object van de lezing, bij een afwezige link null, bij een knoop die geen lid leest null — de toolchain onderscheidt
die twee aan de knoopsoort en de wandeling (`navigationBindings`). `DroolsBackend.reached(…, CharacteristicCheck, walk)`
bestaat voor een eigen getuige. De canonieke tekst (`--readings`, de dump) heeft de staart alleen bij een pad.

### Gates

Zelf gedraaid, elk alleen (geen andere gate-set liep; een eerste start van gate 12 die samenviel met de gate-set van de
toolchain heb ik op PID gestopt en na afloop daarvan opnieuw gestart):
- gate 17 (`opam exec -- bash scripts/check-readings.sh`, `werk/reading-read-objects/logs/gate17-1.log`): PASS, 899
  golden-casebestanden getraceerd, per tak 279 goldens en 769 casebestanden vergeleken, 6334 lezingen, 20033 knoopregels
  (3483 `unreached`, 900 met bijdragers, 319 met een tijdlijn, 746 met een object, waarvan 113 `on none`), 128
  bijdragers van ref niet bereikt, afwijkingen 0, 14 vergelijkingsrijen en 3 struikeldraadrijen; 83 s.
- gate 12 (`bash scripts/check-drools.sh`, `werk/reading-read-objects/logs/gate12-2.log`): PASS, played 279 op beide
  takken (gelijk aan het gepinde aantal), elke statusregel van de DRL-teksttak gelijk aan die van het exec-model, JUnit
  1709 tests (vloer 1573), 0 skips.
- de suite van `ref/test/` (`dune build @ref/runtest` en `test_ref.exe` vanaf de root): 1425 worked examples.
- de volledige Drools-suite (`mvn -o test`, `werk/reading-read-objects/logs/mvn-suite-1.log`): 1709 tests, 0 failures, 0
  errors, 0 skips.

De volledige set (`make gates`) niet; die draait de hoofdsessie vóór de landing.
