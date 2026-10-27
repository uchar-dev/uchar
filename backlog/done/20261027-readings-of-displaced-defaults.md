# readings-of-displaced-defaults: een verdrongen default krijgt de lezing van zijn voorwaarde en zijn waarde

*Voorwaarde voor deel 2 van de toolchain-kaart `explanation-read-objects-and-displaced` (van de werkbank, op besluit
van de eigenaar, 2026-10-27). Ontwerp: Fable-advies in `uchar-dev/research/uitleg-gelezen-objecten-2026-10-27/`.
Start na de landing van `reading-read-objects` (beide raken ch. 5.12, ref en de lezingen van Drools).*

## Aanleiding

De uitleg van een defaults-groep noemt de varianten na de beslisser alleen bij naam ("verdrongen: 'herleving per
kalendermaand art. 21 lid 1 na onderdeel c', …"). De werkbank wil per verdrongen variant weten of haar voorwaarde gold
en, zo ja, welke waarde zij gaf (bij een attribuut) of wat zij besliste (bij een kenmerk): het antwoordmodel van een
vraag noemt "herleving per 01-07-2025 was mogelijk geweest", de waarde van zo'n variant.

Gemeten (2026-10-27): geen motor rekent dat uit.

- De DRL-verlaging bewaakt een lagere default met de beurt vóór zijn toets: in de DRL van het model van de
  toolchain-test `ExplanationDefaultsTest` heeft `defaults A:Jutter.premie: default 1` (de basis 'premie') in zijn
  linkerkant `not( Decides( stack == "defaults A:Jutter.premie" && index < 1, subject == $onderwerp ) )` vóór
  `eval( toets_… )`, en zijn waarde staat alleen in het gevolg.
- Ch. 5.12: "A form that is not at its turn, or outside its validity, is not evaluated, so it has no nodes. Every
  default below the one that decides is displaced, and with it every default inside its exception groups." De lezing van
  zo'n default is `DISPLACED` zonder knopen.
- De dienst en de playground van de toolchain lezen beide de build met lezingen van uchars Drools-backend
  (`DecisionCore`, `Playground`); een eigen motor hebben zij niet.

## Doel

Ch. 5.12: een default die niet aan de beurt was omdat een hogere default van zijn keten besliste of een
uitzonderingsgroep van hem uitkwam (ch. 7.1, 7.9), en een gewone regel die ch. 7.10 in een stapel opnam en daar zo werd
verdrongen, krijgt de evaluatie van zijn voorwaarde en zijn waarde op de fixpuntwereld, zoals een vorm die aan de beurt
is; dat hij niet aan de beurt was, blijft te lezen. Een initialisatie die de invoer of een andere schrijver verdrong, een
creatie van de 1:1-vorm en een vorm buiten zijn geldigheid blijven zoals ze zijn. ref en de Drools-backend (beide
takken) leveren het, gate 17 vergelijkt het; de lezing verandert niets aan wat de sessie berekent.

## Acceptatie

- (a) Rood eerst: rijen in de getuigen van de lezingen voor een verdrongen attribuutdefault die gold en een waarde gaf,
  een die gold en leeg gaf, een die niet gold, een verdrongen kenmerkdefault, een default in een uitzonderingsgroep onder
  een verdrongen default, en een gewone regel in een stapel (ch. 7.10); elk faalt op de code van nu.
- (b) Gate 17 vergelijkt de nieuwe lezingen; de gemeten aantallen in het Result; vloeren zijn ondergrenzen.
- (c) De clausule in ch. 5.12 als voorstel, met een gewerkt voorbeeld met absolute waarden.
- (d) De productiebuild bytegelijk (`ReadingRulesFixpointWitnessTest`); `make gates` groen.

## Voorlopige besluiten (hoofdsessie, 2026-10-27, naar het Fable-advies)

*Gemarkeerd voorlopig; de eigenaar kan elk besluit omdraaien. Bron: `research/uitleg-gelezen-objecten-2026-10-27/
fable-advies.md` §2, §5, §6.*

- **D5 Vorm.** `DISPLACED` mét knopen: de status zegt dat de default niet aan de beurt was, de knopen wat hij vond. Geen
  nieuwe status en geen extra veld. De rij `DISPLACED` van de statustabel splitst in twee rijen (met en zonder knopen).
  WF-051 maakt de lezing op de fixpuntwereld gelijk aan de speculatieve evaluatie van ch. 7.1.
- **D6 Welke vormen.** Elke default onder de beslisser in zijn keten, aan de wortel of in een uitzonderingsgroep, met
  wat hij in zijn uitzonderingsgroepen draagt; de eigen inhoud van een default waarvan een uitzonderingsgroep besliste
  of door een conflict settelde; een gewone regel die ch. 7.10 in de stapel vouwde, onder de beslisser; de
  initialisatiedefault (ch. 7.3) onder een hogere beslisser. Niet: een initialisatie die de invoer of een andere
  schrijver verdrong, een 1:1-creatie, een vorm buiten zijn geldigheid, een groep-default zonder eigen inhoud, een
  kenmerkdefault zonder rechtvaardiging, een lid van een ongeordende zustergroep (dat is aan de beurt).
- **D7 Fout.** Een fout in de voorwaarde of de waarde van een verdrongen default staat op haar knopen, de status blijft
  `DISPLACED`, een deponering valt weg.
- **Bouw.** Drools: naast de leesregel van een bewaakte default een speculatieve leesregel zonder de controlepatronen
  (`Reading.kind`), zodat de beurt bij truth maintenance blijft; ref: `read_on` in `displace`. Gewerkte voorbeelden:
  `anchor/jutterswet-nestuitzondering` case 1 en `anchor/jutterswet-vondstwaardering` (advies §2.5).

## May touch

`backends/drools/**` buiten de productieruntime, `ref/lib/reading.ml` en wat het trace-pad van ref raakt, `ref/test/`,
een golden; `spec/RUNTIME-SEMANTICS.md` en `scripts/check-readings.sh` als voorstel; deze kaart.

## Result (2026-10-27)

Tak `card/readings-of-displaced-defaults`, op `e4764491`. Geraakt: `backends/drools/**` buiten de productieruntime,
`ref/lib/reading.ml`, `ref/test/`, `spec/RUNTIME-SEMANTICS.md` en `scripts/check-readings.sh` (voorstel: beschermde
paden, de tak is het voorstel), deze kaart. Niet geraakt: `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md`, de corpus.

- `f5d668de` reading-read-objects (regressie): het objectslot buiten de toets (vondst bij kaart 1, onder).
- `c54bd6f9` ref: `displace` leest een verdrongen default; drie ref-tests.
- `7cd7a072` Drools: de speculatieve leesregel, `Reading.Kind`, `readingsOf`; de getuigen.
- `b8414644` RUNTIME-SEMANTICS ch. 5.12 (voorstel): twee rijen `DISPLACED`, "A displaced default is read", twee gewerkte
  voorbeelden, "The artifacts".
- `180abcc8` gate 17 (voorstel): de telling van de knoopregels onder `DISPLACED`, vloer en twee rijen van de teller.
- dit Result.

### Vondst bij kaart 1 (`reading-read-objects`, geland als `f0ff72ab`): DROOLS-264 op v2 in de build met lezingen

Gevonden door de hoofdsessie: de toolchain-dienst startte niet meer op de v2-regelset; de build met lezingen weigerde
`defaults A:RechtOpUitkeringWwBijOntslag.duurverlenging bij-een-nieuw recht` met DROOLS-264 ("een voorwaarde die 17
waarden leest …: de toets van deze tak neemt hoogstens 13 gebonden waarden"). Oorzaak: `Reads.allocateSlot` voegt elk
slot ook aan `use` toe, de set waaruit `testItem` de invoer van de toets maakt (`positions(read)`) en de DRL-teksttak de
parameters van zijn toetsfunctie, en `withObject` reserveerde het `PATH_OBJECT`-slot met `allocateSlot` terwijl de
voorwaarde compileerde. Elke lezing over een pad kostte de toets zo één waarde meer dan in de productiebuild. Herstel
(vorm naar besluit van de eigenaar): `reserveSlot` reserveert en bindt een slot zonder `use` aan te vullen;
`allocateSlot` roept haar aan en vult `use` aan; `withObject` gebruikt `reserveSlot`. Alleen de lezing, die elk gebonden
slot draagt, leest het object. De productiebuild verandert niet (zonder lezingen bindt `navigated` geen object).

- Getuige `ReadingsWitnessTest.a_condition_that_reads_as_many_values_as_a_test_takes`, beide takken: een regel op
  Wachters waarvan de voorwaarde een `alle`-samenstelling van dertien vergelijkingen is, tien over eigen attributen en
  drie over de post (hoogte, breedte, diepte); de productiebuild draagt haar (W1 inzetbaar), de build met lezingen geeft
  de lezingen met objecten (W1 `HOLDS`, elke lezing over de post `@P1`; W2 zonder post `FAILS`, de samenstelling stopt bij
  de eerste lezing over het pad, `leeg` zonder object). Rood op `f0ff72ab` (kopie met alleen de test,
  `werk/readings-of-displaced-defaults/logs/red-k1-1.log`): `CaseRunner$Excluded: transpiler SKIP: regel: inzet — een
  voorwaarde die 16 waarden leest ('all 0'): de toets van deze tak neemt hoogstens 13 gebonden waarden`, na de geslaagde
  productiebuild.
- v2-controle (`werk/readings-of-displaced-defaults/probe/V2BuildProbe.java`, de 13 fundament- en 20 regelbestanden
  in resolve-volgorde door uchars frontend, `--ir`, zonder scope): op `f0ff72ab` productie 339 regels op beide takken en
  met lezingen op beide takken `Excluded: … duurverlenging bij-een-nieuw recht — a condition that reads 17 values`; na de
  fix 452 regels met lezingen, en op de eindstand van de tak 493 (de speculatieve leesregels erbij), beide takken.
- `ReadingRulesFixpointWitnessTest`, `ReadingRulesObserverWitnessTest`, `BranchDumpsCorpusWitnessTest`,
  `ReadingsCorpusWitnessTest` groen op de fixcommit alleen (`logs/k1fix-tests-2.log`).

### Wat er is

**Spec** (ch. 5.12, voorstel). De statustabel heeft twee rijen `DISPLACED`: een default die niet aan de beurt was omdat
een hogere default van zijn keten besliste of settelde (onder de beslisser, aan de wortel of in een uitzonderingsgroep,
met alles in zijn uitzonderingsgroepen; de eigen inhoud van een default waarvan de uitzonderingsgroepen settelden; een
gewone regel die ch. 7.10 in de stapel vouwde, onder de beslisser; de initialisatiedefault onder de beslisser waar de
invoer het doel leeg laat) heeft de knopen van zijn voorwaarde, en van zijn waarde waar die geldt; een initialisatie
waarvan de invoer of een andere schrijver het doel vult en een 1:1-creatie met een gekoppeld onderwerp hebben er geen.
De alinea "A displaced default is read": gelezen zoals aan de beurt, op het niveau dat hem declareerde; de status blijft
`DISPLACED`; WF-051 maakt de lezing op de fixpuntwereld gelijk aan de speculatie van ch. 7.1; een fout staat op de
knopen; een deponering valt weg; het besluit van een kenmerkdefault staat op de draad. Gewerkte voorbeelden
`jutterswet-nestuitzondering` case 1 en 9 en `jutterswet-vondstwaardering` alle zes cases. In het voorbeeld
`opgave-bedrag` leest case 1 nu `DISPLACED: 25,00 €` (de tabelcel; het blok met de canonieke tekst van case 5 is
letterlijk gebleven).

**ref.** `displace` emitteert per verdrongen default `{ (read_on …) with status = Displaced }` met de `env` van het
declarerende niveau, recursief door de uitzonderingsgroepen (`displace_all`), en de `settled`-arm leest de eigen inhoud
van de default zo; een initialisatiedefault waarvan de invoer het doel vult blijft `without Displaced`
(`input_filled`, nu gedeeld met `own`).

**Drools.** `lowerOwnRule` plant naast de leesregel van een default waarvan `control` niet leeg is een speculatieve
leesregel `<regel>: lezing buiten de beurt`, `readingPlan(List.of(), false)`: dezelfde linkerkant zonder de
controlepatronen; de invoertoets van een initialisatiedefault zit in het onderwerppatroon en blijft. Haar gevolg is
`Consequence.Reading(…, speculative = true)`, een `Reading` met `kind = SPECULATIVE` (`equals`/`hashCode` met de soort;
de leesregel van de beurt houdt zijn 3-argumentconstructor, dus zijn DRL-tekst). `readingsOf` sleutelt de feiten op
(soort, vorm, object): de lezing van de beurt waar die staat, anders de speculatieve met status `DISPLACED` (ook waar de
voorwaarde een fout geeft), anders `withoutReading` zoals voorheen. De speculatieve regel staat in de regellijst direct
na de leesregel van de beurt, heeft de herkomst van de regel die hij leest en de rol `READING`. De javadoc van
`ConditionReading` (`Status.DISPLACED`, `nodes`), `readingsOf` en `DroolsBackend.build`/`readingsOf` beschrijven de stand.

### Getuigen, met tellingen

- `ReadingsWitnessTest.displaced_defaults_are_read`, beide takken, een eigen document (Jutters, Strandrecht/Wrakrecht):
  een verdrongen attribuutdefault die gold en een waarde gaf (`AttributeReference 7`), een die gold en leeg gaf, een die
  niet gold (`false`, waarde `-`), een kenmerkdefault die gold en een die niet gold, een form-a-default onder de
  beslisser met de default in zijn uitzonderingsgroep, de eigen inhoud van een form-a-default waarvan de
  uitzonderingsgroep besliste, de eigen regel van een Wrakrecht die ch. 7.10 onder de geërfde uitzondering vouwde, en de
  initialisatiedefault onder de beslisser (`0 Literal 3`) naast die waarvan de invoer het doel vult (zonder knopen);
  22 records en de canonieke tekst van de keten (18 regels).
- `ReadingsWitnessTest.a_displaced_default_whose_justification_faults` (D7), met ref
  `WV-5.12-fout-in-een-verdrongen-default` op hetzelfde document: een rechtvaardiging die op een `Unresolved` eindigt;
  `0 Compound fault EF-019`, `2 Unresolved fault EF-019`, status `DISPLACED`; op J2 stopt de samenstelling ervoor.
- ref `WV-5.12-nestuitzondering-verdrongen` (case 1 en 9, uit de golden) en `WV-5.12-vondstwaardering-verdrongen` (zes
  cases). Suite 1425 → 1434.
- `ReadingsCorpusWitnessTest`: een verdrongen lezing met knopen moet een default (of een gevouwen regel) zijn, de wortel
  van haar voorwaarde bereikt hebben en de wortel van haar waarde precies waar de voorwaarde geldt; haar operanden en
  samenstellingen worden gehouden zoals elke lezing; een verdrongen default zonder knopen moet een initialisatie zijn
  waarvan de invoer het doel vult. Gemeten 2026-10-27, per tak gelijk (exec-model en DRL-tekst): 230 verdrongen, waarvan
  207 met knopen (484 knopen) en 23 zonder; 5864 operanden getoetst. Vloer `EXPECTED_MIN_DISPLACED_READ=207`;
  `EXPECTED_MIN_DISPLACED=178` blijft (telt statussen).
- Gate 17: 484 knoopregels onder een `DISPLACED` per tak (207 lezingen), letterlijk vergeleken; vloer
  `EXPECTED_MIN_DISPLACED_NODE_LINES=484`, de teller met twee eigen rijen. Knoopregels 20033 → 20517, `unreached`
  3483 → 3504, met bijdragers 900 → 909. Geen verdrongen lezing in het corpus draagt een fout (ref, alle 899
  golden-casebestanden), dus D7 zit alleen in de twee getuigen hierboven; dat staat in "The artifacts".

Tests die veranderden (gedrag dat de kaart verandert): `ReadingsWitnessTest.a_chain_of_defaults` (de vijf verdrongen
records hebben nu knopen) en `forms_without_a_condition` (P1: de initialisatiedefault onder de beslisser `DISPLACED | 0
Literal 3`); ref `WV-5.12-opgave-bedrag` case 1 (`DISPLACED` en `0 Literal 25 "EUR"`); `RuleOriginWitnessTest.
a_subtype_stack_names_the_ordinary_rule_it_took_in` (een regel meer: `default 1: lezing buiten de beurt -> DEFAULT
reading /rules/rules/0/versions/0`) en `RuleOriginCorpusWitnessTest` (het naamorakel kent de achtervoeging `: lezing
buiten de beurt`); `ReadingsCorpusWitnessTest` (de regel "een verdrongen default heeft geen knopen" is vervangen, zie
boven). Geen test verzwakt.

Ongewijzigd groen: `ReadingRulesFixpointWitnessTest` (met en zonder lezingen dezelfde feiten), `ReadingRulesObserverWitnessTest`
(dezelfde vuringen per vorm), `BranchDumpsCorpusWitnessTest` (de dumps van de twee takken, nu met de speculatieve
`Reading`-feiten en de knoopregels onder `DISPLACED`).

### Rood eerst

Kopie op `e4764491` met alleen de tests (`werk/readings-of-displaced-defaults/red-k1`, `git worktree add --detach`):
- Drools (`logs/red-k2-drools.log`): `ReadingsWitnessTest` 4 failures en 1 error — `displaced_defaults_are_read`,
  `a_chain_of_defaults`, `forms_without_a_condition`, `a_displaced_default_whose_justification_faults` (geen knopen
  onder `DISPLACED`), en `a_condition_that_reads_as_many_values_as_a_test_takes` (DROOLS-264, kaart 1);
  `ReadingsCorpusWitnessTest`: "verdrongen met knopen 0, onder de vloer 207".
- ref (`logs/red-k2-ref.log`): 7 failures — `WV-5.12-opgave-bedrag case1`, `WV-5.12-nestuitzondering-verdrongen` case 1
  en 9, `WV-5.12-vondstwaardering-verdrongen` case 1, 2 en 6, `WV-5.12-fout-in-een-verdrongen-default`.

### Mutaties (in een kopie, `werk/readings-of-displaced-defaults/mut`, niet gecommit)

- R1, ref: `read_displaced` zonder de arm `input_filled`: `WV-5.12-vondstwaardering-verdrongen case6` rood (de
  initialisatiedefault onder de beslisser met een gevuld doel krijgt `0 Literal 25`).
- R2, ref: de `settled`-arm emitteert weer `without Displaced`: `WV-5.12-nestuitzondering-verdrongen case1` rood.
- R3, ref: de status niet op `Displaced` gedwongen: 7 rood, onder meer `-fout-in-een-verdrongen-default` (`FAULTS`).
- D1, Drools: `reading()` dwingt de status niet op `DISPLACED`: `ReadingsWitnessTest` 4 rood; `ReadingsCorpusWitnessTest`
  rood op de vloer ("verdrongen 23, onder de vloer 178"). Met de twee vloeren van verdrongen op 0 is de corpusgetuige
  groen: een verdrongen default met de status `HOLDS` of `FAILS` ziet hij alleen aan de telling; `ReadingsWitnessTest`
  en gate 17 (ref zegt `DISPLACED`) houden de status.
- D2, Drools: geen speculatieve regel voor een kenmerkdefault: `ReadingsWitnessTest` 3 rood; de corpusgetuige rood op de
  vloer (153 tegen 207), en met de vloeren op 0 rood met 108 verschillen (54 per tak), "…/defaults/1 op M: DISPLACED
  zonder knopen, en een default die een hogere verdrong heeft de knopen van zijn voorwaarde en zijn waarde"
  (`anchor/jutterswet-hardheid/case2-hardheid.json`). Gate 17 met D2 (`logs/gate17-mutant-D2.log`, jar herbouwd): exit 1,
  66 (case, tak)-paren verschillen, het eerste `anchor/jutterswet-hardheid/case2-hardheid.json (exec)`: ref `…/defaults/1
  "M" 0 Comparison false`, `1 AttributeReference 6`, `2 Literal 12`, de backend geen knoopregel; en
  `EXPECTED_MIN_DISPLACED_NODE_LINES` valt (338 tegen 484).
- D3, Drools: de waarde ook lezen waar de verdrongen voorwaarde faalt: `ReadingsWitnessTest` 2 rood; de corpusgetuige
  rood met 38 verschillen, "…/defaults/1/exceptions/members/0/defaults/0 op M: DISPLACED las zijn waarde, waar zijn
  voorwaarde niet geldt" (`anchor/jutterswet-nestuitzondering/case1-alleen-y.json`).
- D4, Drools: geen speculatieve regel voor een initialisatiedefault: `ReadingsWitnessTest` 2 rood; de corpusgetuige rood
  op de vloer (176 tegen 207), en met de vloeren op 0 met 62 verschillen (31 per tak), "…/defaults/2 op M: DISPLACED
  zonder knopen, en de invoer laat het doel waardering van de initialisatie leeg"
  (`anchor/jutterswet-vondstwaardering/case1-erkend.json`).
- Kaart 1: de rode stand op `f0ff72ab` is de mutatie (het objectslot in `use`).

De mutaties liepen op een kopie van `197e4cde`, de tak vóór de omzetting van de fix van kaart 1 naar `reserveSlot`; de
kaart-2-code daar is die van de eindstand. De kopieën `red-k1` en `mut` zijn na afloop opgeruimd met
`git worktree remove`.

### De productiebuild

De DRL-tekst van de build zonder lezingen van alle 329 goldens (282 gebouwd, 47 met dezelfde weigering), jar van
`f0ff72ab` tegen die van de eindstand (`werk/readings-of-displaced-defaults/drldump`, `DrlDump.java` van kaart 1):
`diff -r` leeg. Productieregels en -vuringen op het corpus gelijk (onder).

### Kosten

Gemeten met `werk/readings-of-displaced-defaults/cost/ReadingCostProbe` (niet gecommit): per tak de regels van de
KieBase, de vuringen over elke case en de feiten aan het fixpunt; "voor" is de jar van de fix van kaart 1 in haar eerste
vorm (`3577b884`, gelijk in gedrag aan `f5d668de`), "na" die van `197e4cde` (de kaart-2-code van de eindstand). Corpus: de 279 goldens die het exec-model speelt, 769 cases. v2: de acht scenariodocumenten van
`herleving-ww/scenarios` door uchars frontend (`--ir --cases`), 171 cases.

| | voor | na |
|---|---|---|
| corpus, productie: regels / vuringen (exec-model; DRL-tekst) | 2116 / 10476; 10481 | gelijk |
| corpus, met lezingen: regels | 3031 | 3124 (+93) |
| corpus, met lezingen: vuringen (exec-model; DRL-tekst) | 17464; 17469 | 17952; 17957 (+488, +2,8 %) |
| corpus, met lezingen: `Reading`-feiten | 6085 | 6571 (+486) |
| v2, productie: regels / vuringen | 2712 / 24543 | gelijk |
| v2, met lezingen: regels | 3616 (452 per document) | 3944 (493 per document, +41) |
| v2, met lezingen: vuringen | 46831 | 54877 (+8046, +17 %) |
| v2, met lezingen: `Reading`-feiten | 14379 | 19700 (+5321) |

De speculatieve regel vuurt ook waar de default aan de beurt is: op het corpus 486 speculatieve feiten, waarvan 207
voor een verdrongen default gelezen worden. Gate 17 alleen: 62 s (`logs/gate17-3.log`).

### Voorlopige besluiten van de bouwer

- **V1 Een speculatieve regel alleen waar `control` niet leeg is.** De bovenste default van een keten en een
  initialisatiedefault die alleen de invoertoets bewaakt hebben geen controlepatroon, en hun speculatieve regel zou
  gelijk zijn aan die van de beurt. De invoertoets staat in het onderwerppatroon en blijft dus in de speculatieve regel:
  een initialisatiedefault waarvan de invoer het doel vult heeft ook onder een beslisser geen knopen (advies §2.2).
- **V2 `Reading.kind` in het feit, geen tweede feitklasse.** `equals`/`hashCode` nemen de soort mee, zodat truth
  maintenance een lezing van de beurt en een speculatieve met dezelfde waarden niet samenvoegt. De leesregel van de beurt
  houdt zijn constructor zonder soort: zijn DRL-tekst in de build met lezingen is ongewijzigd; alleen de speculatieve
  regel schrijft `Reading.Kind.SPECULATIVE`.
- **V3 De speculatieve regel vuurt ook aan de beurt.** Een `not Reading(TURN)` in haar linkerkant zou een regel laten
  lezen wat een leesregel invoegt; `readingsOf` neemt waar beide staan de lezing van de beurt. Kosten zie boven (v2 +17 %
  vuringen in de build met lezingen; de productiebuild ongewijzigd).
- **V4 De naam `<regel>: lezing buiten de beurt`**, direct na de leesregel van de beurt in de regellijst; herkomst die van
  de gelezen regel, rol `READING`. `ReadingRulesObserverWitnessTest` toont dat de vuringen per vorm niet verschuiven.
- **V5 D7 met een eigen getuige in beide uitvoerders**, buiten de rijen van (a): geen golden heeft een verdrongen default
  met een fout, dus anders had de zin van de spec geen artefact.
- **V6 Het voorbeeld opgave-bedrag** in ch. 5.12: alleen de tabelcel van case 1 verandert (`DISPLACED: 25,00 €`) en de
  zin eronder; het blok met de canonieke tekst (case 5) blijft letterlijk. Het advies noemde 25 "EUR" voor
  `jutterswet-nestuitzondering`; die golden heeft geen eenheid, de tekst schrijft `Literal 40`.
- **V7 De corpusgetuige houdt een verdrongen default zonder knopen** aan "een initialisatie waarvan de invoer het doel
  vult" (op de bean vóór de terugschrijving), en een met knopen aan de bereikte wortels; de status zelf houden
  `ReadingsWitnessTest` en gate 17 (mutatie D1).
- **V8 De vondst bij kaart 1 als eigen commit vóór de kaart-2-commits**, in de vorm die de eigenaar koos (`reserveSlot`);
  de tak is daarvoor opnieuw opgebouwd op `e4764491` (eerst `3577b884` met een tijdelijk geleegde `use`, vervangen).

### Voor de hoofdsessie

**`CLAUDE.md`, gate 17, na de zin over `EXPECTED_MIN_OBJECT_LINES`:**

```
    Since the card `readings-of-displaced-defaults` (2026-10-27) a
    default that a higher default of its chain displaced is read as at
    its turn and keeps the status DISPLACED (ch. 5.12, "A displaced
    default is read"); its node lines are compared verbatim like every
    other, and floor `EXPECTED_MIN_DISPLACED_NODE_LINES=484` (measured
    2026-10-27, 207 readings) counts them, its counter passing two rows
    of its own.
```

**`CLAUDE.md`, gate 12, na "…whose facts `readingsOf` reads per form, object and node;":** "beside the rule of a default
whose turn the control facts of its stack guard, a speculative reading rule without them, whose readings `readingsOf`
reads as DISPLACED where the default was not at its turn (card `readings-of-displaced-defaults`);". Geen CONSTRUCTS-rij.
PLAN §9, voorgesteld: "readings-of-displaced-defaults (2026-10-27): een default die een hogere verdrong wordt gelezen
zoals aan de beurt, status DISPLACED met knopen (ch. 5.12); speculatieve leesregel in Drools, `displace` in ref;
vondst bij kaart 1 (objectslot in de toets, DROOLS-264 op v2) hersteld in `f5d668de`; voorlopige besluiten V1–V8."

**Wat de toolchain moet weten.**
- `ConditionReading.Status.DISPLACED` kan nu knopen hebben: met knopen is het een default die een hogere verdrong
  (knoop 0 het oordeel van zijn rechtvaardiging, de wortel van de waarde wat hij gegeven had, `null` = leeg); zonder
  knopen een initialisatie met invoer of een 1:1-creatie. Een kenmerkdefault: het besluit uit `Default.decision`.
  Een fout op een knoop van een verdrongen lezing is de fout van de lezing, niet van de sessie.
- De build met lezingen heeft per bewaakte default een regel meer (`<regel>: lezing buiten de beurt`, rol `READING`,
  `formOf` null) en `Reading`-feiten met `kind`; een afnemer die regels of vuringen van die build telt ziet er meer (v2
  +41 regels per document, +17 % vuringen). De productiebuild is ongewijzigd.
- De v2-regelset bouwt weer met lezingen (kaart-1-vondst); de pin moet minstens `f5d668de` zijn.

### Gates

Zelf gedraaid, elk alleen (een eerste start van gate 17 viel samen met de gate-set van de toolchain en is op PID gestopt;
na afloop daarvan opnieuw gestart):
- gate 12 (`bash scripts/check-drools.sh`, `werk/readings-of-displaced-defaults/logs/gate12-1.log`, eindstand
  `180abcc8`): PASS, played 279 op beide takken (gelijk aan het gepinde aantal), elke statusregel van de DRL-teksttak
  gelijk aan die van het exec-model, JUnit 1712 tests (vloer 1573), 0 skips; ±7 min.
- gate 17 (`opam exec -- bash scripts/check-readings.sh`, `logs/gate17-4.log`, eindstand): PASS, 899
  golden-casebestanden getraceerd, per tak 279 goldens en 769 casebestanden vergeleken, 6334 lezingen, 20517
  knoopregels (3504 `unreached`, 909 met bijdragers, 319 met een tijdlijn, 746 met een object, 484 onder een
  `DISPLACED`), afwijkingen 0, 14 vergelijkingsrijen en 3 struikeldraadrijen; 63 s. `DEVIATIONS` en
  `READINGS_ERROR_KNOWN` leeg.
- de suite van `ref/test/` (`dune build @ref/runtest` en `test_ref.exe` vanaf de root): 1434 worked examples.
- de volledige Drools-suite (`mvn -o test`, `logs/mvn-suite-2.log`, op de kaart-2-code vóór de omzetting van de fix van
  kaart 1 naar `reserveSlot`; op de eindstand in gate 12): 1712 tests, 0 failures, 0 errors, 0 skips.

De volledige set (`make gates`) niet; die draait de hoofdsessie vóór de landing.
