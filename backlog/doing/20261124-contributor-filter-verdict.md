# contributor-filter-verdict: de lezing van een kwantorvouw zegt per lid of zijn filter vaststaat

*Opdracht van de eigenaar, 2026-11-24: vervolgpunt van de toolchain-kaart `one-empty-in-service`; vóór de bouw gelezen
door een extern model (xhigh), de punten hieronder verwerkt. Gebouwd 2026-11-24 op `card/contributor-filter-verdict`;
zie Result.*

Goal: de lezing van een kwantorvouw (RUNTIME-SEMANTICS ch. 5.12, en de lezingen van ref en Drools die gate 17 gelijk
houdt) zegt per bijdrager niet alleen het oordeel van de matrix, maar ook of het filter op dat lid vaststaat. Dan kan
een lezer (de uitleg van de toolchain) een vouw met een open filter exact uitleggen, zonder eigen oordeel over het
filter.

Acceptance: een uitgewerkt voorbeeld in ch. 5.12 met een filter "A of B" (A waar, B open) en een filter dat op een
lid onbepaald is, als test van ref; de lezingen van ref en van Drools (beide takken) gelijk in gate 17, met de nieuwe
tekst; alle gates groen (`make gates`); geen bestaande lezing zonder onbepaald filter verandert (hun pins blijven).

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 5.12 (en ch. 1.5 waar het de lezing noemt), `ref/lib/reading.ml`,
`ref/lib/eval.ml` (`contribution`, en de onbereikte bijdragers), `ref/test/`, `backends/drools/.../transpiler/tms/ConditionReading.java`,
`SessionReaders.java`, `cases/SessionDump.java` (`readingLines`, waar de tekst geschreven wordt) en de tests en pins van
de lezingen, `scripts/check-readings.sh` (parser, vergelijking, tripwire, tellers, zelftestrijen),
`backends/drools/README.md`, deze kaart. Mandaat van de eigenaar voor de beschermde paden die het raakt (spec, scripts).

Out of scope: de toolchain (eigen kaart `quantifier-filter-in-explanation`, één pin-bump); de lezing van een vouw over
een set (som, aantal, …): een open lezing breekt die af en zij heeft geen bijdragers; de semantiek zelf (ch. 1.5 en 3.9a
veranderen niet: het oordeel van de vouw blijft hetzelfde).

## Aanleiding

De bijdragers van een kwantorvouw zijn de leden waarvan het filter waar of onbepaald is; per bijdrager geeft de lezing
het oordeel van de matrix (`true`, `false`, een fout, of `undetermined`) en of de evaluatie het lid bereikte
(ch. 5.12: `contributors [<object> <value|verdict|fault> reached|unreached; …]`; Drools
`ConditionReading.Contributor(member, value, fault, reached, undetermined)`, waar `undetermined` de matrix betreft). Of
het filter op een bijdrager vaststaat, staat er niet in. De uitleg van de toolchain kan daardoor bij "alle" met V2
(filter onbepaald, matrix onwaar) niet zeggen of V2 het oordeel draagt; zij noemt nu alleen het oordeel van de vouw
(toolchain-kaart `one-empty-in-service`, Result). Ref en Drools kennen het filteroordeel per lid wel
(`MemberTest.filter` in Drools; in ref het oordeel van het filter in de vouw).

## Voorstel

1. **Spec, ch. 5.12:** een bijdrager van een kwantorvouw waarvan het filter onbepaald is, krijgt in de canonieke tekst een
   aanduiding, bijvoorbeeld `<object> filter undetermined <verdict|fault> reached|unreached`; een bijdrager met een
   vaststaand waar filter blijft geschreven zoals nu, zodat bestaande pins niet veranderen. De bouwer kiest de precieze
   vorm in de stijl van ch. 5.12 en schrijft het uitgewerkte voorbeeld (Jutterswet).
2. **ref:** `reading.ml` schrijft de aanduiding uit het filteroordeel dat de vouw al heeft.
3. **Drools:** `ConditionReading.Contributor` krijgt het filteroordeel (bijvoorbeeld `boolean filterUndetermined`), gevuld
   uit `MemberTest.filter` in `SessionReaders`, op beide takken; de tekst van de lezing volgt ch. 5.12.
4. **Gate 17:** de lezingen blijven gelijk; vloeren die groeien, niet bumpen; een eigen zelftestrij voor de nieuwe
   aanduiding als de vergelijkingsregel dat vraagt.

## Uit de lezing vóór de bouw (extern model, nagerekend)

- **Bijdragers.** Ch. 5.12 en de javadoc van `ConditionReading` noemen de bijdragers nu "the members of its range that
  pass its filter". Zeg het precies: de leden waarvan het filter niet vaststaand onwaar is (waar of onbepaald). Een lid
  met een vaststaand onwaar filter is geen bijdrager, ook als dat filter een open deel heeft (`A en B` met A onwaar en
  B open); een lid met `A of B` (A waar, B open) heeft een vaststaand waar filter en krijgt geen aanduiding.
- **Fout in het filter.** Een filterfout op enig lid laat de vouw falen zonder bijdragers (ch. 5.12); er is geen derde
  filterstand. Binnen één samengesteld filter vangt het voorlopig doorlezen een fout na een onbepaald deel op tot
  onbepaald.
- **Positie.** `reached`/`unreached` gaat over de matrix: het filter loopt op elk lid vóór elke matrix, dus een
  onbereikte bijdrager kan een onbepaald filter hebben. ref verliest dat nu: `eval.ml` geeft de onbereikte leden als
  `List.map fst parts` (rond regel 3712–3766) en gooit het filteroordeel weg; Drools (`SessionReaders`) houdt het. Neem
  het mee, anders lopen ref en Drools uiteen. Een uitvoerder zonder volgorde schrijft `unordered`; ook daarnaast kan de
  aanduiding staan. De javadoc van `ConditionReading.Contributor` over `reached == null` volgt wat Drools in een
  lezingsbuild met een open filter doet.
- **Tekstvorm.** Kies een vorm zonder "undetermined undetermined" (filter én matrix onbepaald), met `<object>` voorop
  (de sortering). Het bestaande voorbeeld `WV-5.12-open-quantifier-fold` heeft een lid met een open filter en verandert
  dus; elke lezing zonder onbepaald filter blijft gelijk.
- **Gate 17.** De parser van `check-readings.sh` (`scan_value`, `split`, rond regel 378–409) leest een bijdrager als
  `<member> <één token of foutcode> <rest>`; met de nieuwe aanduiding zou hij het oordeel verkeerd lezen, de tripwire op
  `fault` stil overslaan, `unordered` niet herkennen en onbereikte bijdragers niet tellen. Pas parser, vergelijking,
  tripwire en tellers aan, met verplichte zelftestrijen: een onbepaald tegenover een vaststaand filter, `unordered`
  naast de aanduiding, en een tripwire-rij `… fault EF-019 unordered` met de aanduiding. Vloeren die groeien, niet
  bumpen.
- **Tests** (ref, en via gate 17 Drools): een onbereikte bijdrager met een onbepaald filter; filter én matrix
  onbepaald (ook een voorlopig opgevangen matrixfout); een vaststaand onwaar samengesteld filter met een open deel
  (geen bijdrager); een filterfout na een onbepaald filter (geen bijdragers).

## Result

*Bouwer, 2026-11-24. Branch `card/contributor-filter-verdict`, zes commits na de claim en dit Result; niet gemerged
(beschermde paden: `spec/RUNTIME-SEMANTICS.md`, `scripts/check-readings.sh`, met mandaat van de eigenaar).*

**Wat landde.**

- `a7856d33b` spec, ch. 5.12: de bijdragers zijn precies benoemd (de leden waarvan het filter waar is, bij een
  kwantorvouw niet onwaar: waar of onbepaald; een onwaar samengesteld filter met een open deel valt af, een waar
  samengesteld filter met een open deel is gewoon waar); een bijdrager zegt of zijn filter onbepaald is; de positie
  betreft de matrix (elk filter wordt vóór elke matrix gelezen, dus ook een onbereikte bijdrager kan een onbepaald filter
  hebben); een filter heeft geen derde stand (een filterfout laat de vouw falen zonder bijdragers, een fout binnen één
  samengesteld filter na een onbepaald deel wordt voorlopig opgevangen tot onbepaald); een uitvoerder zonder volgorde
  schrijft de aanduiding naast `unordered`. Het voorbeeld `WV-5.12-open-quantifier-fold` krijgt de nieuwe tekst bij V2.
  Nieuw uitgewerkt voorbeeld **"Worked example — the filter of a contributor"** (`WV-5.12-contributor-filter`, Jutterswet:
  keurder "die gemerkt of drijfhout zijn", nauwgezet "die gemerkt en drijfhout zijn", met de tabel per lid en de canonieke
  tekst, plus de uitgangen van casus 2–4 en de rode casus). De alinea's over gate 17 en de grenzen zeggen dat geen golden
  een bijdrager met een onbepaald filter heeft (zie open punt 1; sinds `1ca3c38b6` noemen ze de golden
  `jutterswet-zwaarkeurder-strikt`).
- `090054d6b` ref: `Eval.contribution` krijgt `filter_undetermined`; de kwantorvouw geeft het filteroordeel door aan de
  waarnemer, ook voor de onbereikte leden (`unreached` krijgt nu de paren lid–filteroordeel in plaats van `List.map fst`);
  `Reading.lines` schrijft de aanduiding. Nieuwe fixture `ref/test/fixtures/strict-empty/contributor-filter/` (IR uit
  de RegelSpraak-frontend, vier casussen met handberekende verwachtingen en `red1-filterfout-na-open-filter`), in de
  lijst `WV-1.5-witness-*`. Ref-test `WV-5.12-contributor-filter` met zes rijen: het voorbeeld (regel 0 en 1, casus 1),
  een waar filter naast een onbepaald (regel 2), een onbereikte bijdrager met een onbepaald filter (casus 2), filter én
  matrix onbepaald door een open lezing (casus 3), een voorlopig opgevangen matrixfout EF-014 naast een onbepaald filter
  en een filter dat de keurgrens voorlopig leest (casus 4), en een filterfout na een onbepaald filter, de vouw `fault
  EF-014` zonder bijdragers (rode casus, op de invoerwereld gelezen zoals `WV-5.12-fout-in-een-kwantorvouw`). De pin
  `WV-5.12-open-quantifier-fold (filter)` kreeg de nieuwe tekst (V2 heeft een onbepaald filter).
  `OpenValuesRefWitnessTest` (Drools) noemt de nieuwe witness; telling 2 × 148 → 2 × 152 (vier casussen meer).
- `1d6fc4abe` Drools: `ConditionReading.Contributor` krijgt `boolean filterUndetermined` (zesde component), in
  `SessionReaders.contributors` gevuld uit `MemberTest.filter() != null`, op beide takken; waardevouwen en de ongeordende
  kwantorvouw geven `false` (een filter dat onbepaald kan zijn maakt de vouw `tested`, dus geordend). De record weigert
  `filterUndetermined` zonder positie; de javadoc over `reached == null` zegt nu dat ook een filter dat onbepaald kan zijn
  de vouw geordend leest. `SessionDump.readingLines` schrijft de aanduiding. README-zin bij "The readings of open values".
  `OpenReadingsWitnessTest`: nieuwe test `wv_5_12_contributor_filter` (vier pins, de trace van ref, beide takken) en
  `a_filter_fault_after_an_undetermined_filter` (de rode casus zonder `expectedFault` gespeeld: regel 2 leest
  `0 QuantifierFold fault EF-014` en `FAULTS`, beide takken); de inline verwachting van `case7-filter-onbepaald` kreeg de
  nieuwe tekst; `withoutPositions` neemt de positie ook weg vóór ` filter undetermined`.
- `ae3fc5c83` gate 17: `split` leest per bijdrager (waarde, positie, filter) en weigert een staart die de tekst niet
  schrijft (geen positie, of iets anders dan ` filter undetermined`); `contributors_agree` houdt waarde en filter
  letterlijk, de positie waar beide er een schrijven; tripwire en teller van onbereikte bijdragers lezen de positie uit
  het drietal. Nieuwe rijen: acht van de lezer (positie; aanduiding na de positie; fout onbereikt met aanduiding;
  `unordered` met aanduiding en onbepaalde matrix; een tekstwaarde die de aanduiding spelt; drie weigeringen), zes
  vergelijkingsrijen (onbepaald tegenover waar filter, beide richtingen; gelijke aanduidingen, filter én matrix
  onbepaald; `unordered` naast de aanduiding; `unordered` zonder de aanduiding; onbereikt tegenover bereikt naast de
  aanduiding) en één tripwire-rij (`… fault EF-019 unordered filter undetermined`). Met de functies van `main` vallen elf
  van de nieuwe rijen rood (gemeten door het Python-blok van `main` met de nieuwe rijen te draaien). Het verslag telt
  de bijdragers met een onbepaald filter, zonder vloer.
- `be11046a4` tekstcorrectie in ch. 5.12 en de javadoc van `ConditionReading.Contributor` ("waar of onbepaald, dus niet
  onwaar"; een lid dat de casus niet noemt heet zo); alleen proza en commentaar, de jar bouwt.
- `dc31a7dff` `ref/test/fixtures/strict-empty/NOTE.md` beschrijft de fixture `contributor-filter` (zij komt niet uit
  `spec/proposals/STRICT-EMPTY.md`) en haar rode casus.

**De tekstvorm.** `<object> <value|verdict|fault> reached|unreached|unordered[ filter undetermined]`: de aanduiding
staat achter de positie, zoals ` on <object>` achter de waarde van een knoop. Zo blijven object (de sortering), oordeel
en positie de eerste drie velden en verandert een bijdrager met een waar filter niet; filter én matrix onbepaald is
`"V1" undetermined reached filter undetermined`, zonder "undetermined undetermined". Geen open lezing achter de
aanduiding: het oordeel van de matrix staat er ook zonder, en de open lezing van de vouw noemt al het filter van het
eerste onbepaalde lid.

**Pins.** 29 trace-pins van `OpenReadingsWitnessTest` veranderden, 201 regels, elk alleen door ` filter undetermined`
achter de positie (nagerekend: met de aanduiding weggehaald gelijk aan de oude pin; ref schreef vóór de wijziging alle
127 pins exact na). Verdeling van de aanduiding: `true reached` 111, `undetermined reached` 116, `false reached` 59,
`true unreached` 8, `false unreached` 8, `undetermined unreached` 5, `fault EF-014 unreached` 5. Geen andere pin
veranderde; de goldens van gate 17 veranderden niet.

**Gates** (elk apart, in de worktree, op de stand van `ae3fc5c83`, gate 1 ook op `be11046a4`; logs in
`werk/contributor-filter-verdict/`):

- 1 `check-self-contained: PASS`
- 3 `check-verifier: PASS (… 508 golden clean; 646 negative rejected for their declared rule, floor 568; …)`
- 5 `check-oracle: PASS (ref suite 4232 worked examples, floor 489; 1189 cases replayed exactly, floor 427;
  preservation with complete knowledge: 280 goldens and 785 cases play, …; monotonicity: 804 cases, 2652 runs …)`
- 12 `check-drools: PASS`, `played 385`, `DRL-text branch played 385`, `JUnit 2062 tests, floor 1573`
- 17 `check-readings: PASS (ref traced 1189 golden case files, …; on both branches 386 goldens and 1063 case files
  compared, 8073 readings, 25815 node lines of which 4140 unreached, 1009 with contributors, 1083 with a timeline, 842
  with the object of a read and 621 of a displaced reading, 147 contributors of ref not reached; …; deviations 0,
  end-state deviations 0; 24 comparison rows, 4 tripwire rows, 10 end-state rows; 25 s)`; counted: `… 147 contributors
  of ref not reached, 0 with an undetermined filter`; op `1ca3c38b6` (tweede bouwer, hieronder): `… 148 contributors of
  ref not reached, 4 with an undetermined filter`
- 7 `check-noop: PASS (1145 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)`
- 8 `check-red: PASS (100 red cases fault observably in ref and clingo, floor 44; 177 ref and clingo code checks …;
  293 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)`
- Geen vloer verhoogd of verlaagd; `make gates` draait de hoofdsessie.

**Tweede bouwer, 2026-11-24** (open punt 1, met mandaat van de eigenaar voor `scripts/`, en vier punten uit de externe
review; vijf commits na `f0f8ea577` en dit Result, niet gemerged).

- `17c926ad3` clingo-suite: `contributor-filter` in de lijst `plays` van de strikte witnesses. De suite eist dat elke map
  onder `ref/test/fixtures/strict-empty/` in precies één van haar twee lijsten staat (`present = listed`); de fixture
  stond in geen (gate 6 was niet gedraaid). clingo speelt haar vier casussen.
- `3caecb1e1` de golden `jutterswet-keurder-strikt`: het document van de fixture met haar casussen 1–3 uit `scenario.rgs`
  (byte-gelijk aan de casusbestanden van de fixture), `EXPECTED_PLAYED` 385 → 386, DRL-tekstpin. Casus 4 kan de
  scenariolaag niet uitdrukken: zij weigert een scenario waarvan een regel een parameter zonder waarde leest ("de regel
  'grenskeurder' leest de parameter 'keurgrens', die in de situatie van het scenario 'geen keurgrens' geen waarde
  heeft"). ref, clingo en beide Drools-takken speelden de drie casussen, maar gate 12 viel om (hieronder); vervangen door
  `1ca3c38b6`.
- `a7dae658d` de vier punten van de externe review: ch. 5.12 zegt "any other fold over a set" waar het "a value fold"
  zei; "The four worked examples above" en `WV-5.12-contributor-filter` in "The artifacts"; het kopcommentaar van gate 17
  noemt dat `filter undetermined` letterlijk vergeleken wordt; `unordered_faults` slaat een regel van ref zonder
  bijdragers over (`c is not None`), met een tripwire-rij die op de functies van `f0f8ea577` crasht
  (`AttributeError: 'NoneType' object has no attribute 'values'`, gemeten) en nu slaagt.
- `7e7e94365` `SessionDump`: een `Undetermined` of `OpenRead` in een feit schrijft de dump als
  `Undetermined(OpenMember(subject="V2", member=…))`. Hun zin noemt het object met zijn identiteit, die de dump weigert,
  en de `MemberTest` van een kwantorvouw met een onbepaald filter of matrix draagt er een. Rood gemeten in gate 12 op
  `a7dae658d`: `BranchDumpsCorpusWitnessTest` en `ReadingRulesFixpointWitnessTest`, "the dump writes no identity: the
  sentence "undetermined: open Vondst@573662a8 vondst.gemerkt" names Vondst@573662a8, the object "V2" of the session".
- `1ca3c38b6` de golden heet `jutterswet-zwaarkeurder-strikt` en draagt alleen regel 3 van de fixture (zwaarkeurder:
  filter "gemerkt of drijfhout", matrix "groter dan de keurgrens") met casussen 1–3; `NOTE.md` met de handberekening per
  lid en de drie lezingen; `EXPECTED_PLAYED` blijft 386; DRL-tekstpin vervangen; casus 2 in `SURVIVORS` van gate 7 (de
  afwezigheid van zwaarkeurder is de bewering); ch. 5.12 (voorbeeld en grens), de NOTE van de fixtures en de
  commentaarregels in `check-drools.sh` en `check-readings.sh` noemen haar. De fixture blijft heel: zij pint drie regels,
  casus 4 en de rode casus die de golden niet heeft.

**Waarom alleen regel 3.** Na de dumpfix bleef `ReadingRulesFixpointWitnessTest` rood op `jutterswet-keurder-strikt`. In
een build met lezingen bindt `TmsTranspiler.quantifierContributions` de plaats van een lid dat de vouw open kan laten
(`placed = ordered || positions != null && (filterEnds.open() || matrixEnds.open())`, sinds `9b1986357`), zonder lezingen
niet; de witness houdt de productiefeiten van beide builds gelijk (`CLAUDE.md`, gate 12: "whose production build is
byte-equal to the one without"). Gemeten op de drie casussen, beide takken: alleen de `Contribution`-feiten van keurder,
nauwgezet en grenskeurder verschillen, en alleen in `MemberTest.place` (`[0]` tegenover `[0, k]`). Zwaarkeurder
verschilt niet: zijn matrix leest de keurgrens, kan dus falen, en is in elke build geordend. Een prototype dat de plaats
in elke build bindt (`placed = ordered || filterEnds.open() || matrixEnds.open()`) maakte de golden groen, maar laat de
productiebuild een strikte kwantorvouw over een relatie met twee meervoudige rollen weigeren (DROOLS-318; gemeten op
`jutterswet-ketenkwantor` met `inferFromEmpty: false`, die zonder lezingen nu bouwt, met het prototype niet); voor
waardevouwen zou hetzelfde `OpenReadingsWitnessTest.a_many_to_many_fold_is_refused_with_readings` breken. Niet geland;
patch in `werk/contributor-filter-verdict/gates2/quantifier-place-in-every-build.patch`. Zie open punt 5.

**Gates** (tweede bouwer, elk apart, na elkaar, op `1ca3c38b6`; logs in `werk/contributor-filter-verdict/gates3/`):

- 1 `check-self-contained: PASS`
- 2 `check-ir-schema: PASS (… 509 file(s) validate and round-trip, floor 192; 65476 object(s) in 1155 document(s) in the
  schema's key order, 2 in no subschema)`
- 4 `check-case-schema: PASS (schema valid; 146 self-test row(s); 1292 case file(s) validate)`
- 5 `check-oracle: PASS (ref suite 4232 worked examples, floor 489; 1192 cases replayed exactly, floor 427; preservation
  with complete knowledge: 280 goldens and 785 cases play, floors 279 and 782; … 5 carrying the property already, …
  jutterswet-zwaarkeurder-strikt …; monotonicity: 807 cases, 2667 runs without one input compared, floor 2652, …)`
- 6 `check-clingo: PASS (backend suite 4879 checks, floor 4797, its 272 units each once in 32 shards; 1162 cases replay
  exactly through clingo …; 10 golden(s) in the refusal channel …)`
- 7 `check-noop: PASS (1147 cases refute a do-nothing executor; 45 pinned survivors pin defeasible standing)`
- 10 `check-regelspraak: PASS — JUnit suite green (1716 tests, floor 173); 224 rgs golden(s) re-emit byte-identically
  (floor 80)`
- 12 `check-drools: PASS`, `played 386`, `DRL-text branch played 386`, `JUnit 2062 tests, floor 1573`
- 16 `check-scenario: PASS — 51 scenario golden(s) (floor 7) regenerate 169 case file(s) (floor 31) and their ir.json
  byte-identically`
- 17 `check-readings: PASS (ref traced 1192 golden case files, …; on both branches 387 goldens and 1066 case files
  compared, 8076 readings, 25818 node lines of which 4140 unreached, 1012 with contributors, …, 148 contributors of ref
  not reached; …; deviations 0, end-state deviations 0; 24 comparison rows, 5 tripwire rows, 10 end-state rows; 25 s)`;
  counted: `… 148 contributors of ref not reached, 4 with an undetermined filter`
- Geen vloer verhoogd of verlaagd; `EXPECTED_PLAYED` (een gelijkheid) 385 → 386. Niet gedraaid: 3, 8, 9, 11, 13, 14,
  15 (de verifier is wel los op het nieuwe `ir.json` gedraaid: `verifier: PASS`).

**Open punten.**

1. **Gesloten.** Gate 17 vergelijkt de aanduiding op de golden `jutterswet-zwaarkeurder-strikt`: vier bijdragers met
   een onbepaald filter, bereikt (casus 1, 3), onbereikt (casus 2) en met filter én matrix onbepaald (casus 3), op ref en
   beide Drools-takken gelijk. Alleen het geordende pad van Drools (een matrix die kan falen); regels 0–2 van de fixture
   (het ongeordende pad met plaatsen alleen in een build met lezingen) staan alleen in de fixture en
   `OpenReadingsWitnessTest`, zie punt 5.
2. **Toolchain.** `ConditionReading.Contributor` heeft een zesde component; de toolchain gebruikt alleen de accessors
   (`Verbaliser`, `ExplanationFoldMembersTest`), dus zij compileert ongewijzigd tegen deze stand. Het gebruik in de uitleg
   is de kaart `quantifier-filter-in-explanation` (één pin-bump).
3. **`unordered` naast de aanduiding** staat in de spec en in de rijen van gate 17, maar geen uitvoerder schrijft het:
   ref schrijft altijd een positie, en Drools leest een vouw waarvan het filter onbepaald kan zijn in volgorde (de record
   weigert het andere).
4. Geen rij voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: de lezing is geen wire, en de semantiek (ch. 1.5, 3.9a)
   veranderde niet. De beschrijving van gate 17 in `CLAUDE.md` ("every reading line is compared verbatim … except the
   position") dekt de aanduiding al.
5. **Plaatsen van open leden alleen met lezingen (Drools).** Een build met lezingen bindt de plaats van een lid dat een
   vouw open kan laten, de build zonder niet (kwantorvouw: `MemberTest.place`; vouw over een set: `FoldOpen.place`), zodat
   `ReadingRulesFixpointWitnessTest` rood wordt op elke strikte golden met zo'n vouw waarvan de matrix niet kan falen of
   deponeren, of met een open lid van een vouw over een set. Drie wegen, een keuze van de eigenaar: (a) de witness
   vergelijkt de productiefeiten zonder die plaats (een testwijziging, en de zin van gate 12 in `CLAUDE.md`); (b) de
   productiebuild bindt de plaats ook (prototype hierboven; kost: DROOLS-318 in productie op een strikte vouw over een
   relatie met twee meervoudige rollen); (c) de lezing haalt de volgorde uit eigen feiten, zodat de bijdragen gelijk
   blijven (een herontwerp in `TmsTranspiler`). Daarna kunnen regels 0–2 van de fixture de golden in.

