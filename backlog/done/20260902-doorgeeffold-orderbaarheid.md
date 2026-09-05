# Doorgeeffolds over equality-only-typen: weigeren op de familie-lezing

Goal: EERSTE / LAATSTE / MINIMUM / MAXIMUM zijn ordenende folds; IR-SPEC
ch. 2.4 verklaart Enumeratie en SumType equality-only, en WF-038 draagt
de operandclausule al in tekst ("numeric folds' operands MUST be
numeric …; date folds' (first/last) operands MUST be dates") — maar de
verifier implementeert haar niet, terwijl de zusterroute (de
vergelijkingsoperator) onder WF-036 al dicht is. **Maintainerbesluit
2026-09-04: weigeren, op de familie-lezing** — alle operanden van een
ordenende fold komen uit één vergelijkbare familie (getal óf
datum/datumtijd), equality-only-elementtypen zijn ill-formed;
`Onbekend`-getypeerde en afwezige operanden deferren (één
parity-document hangt daaraan). De letterlijke date-only-tekst van
WF-038 wordt daarbij herschreven naar deze familie-lezing — dat
ratificeert de v1-verbreding die RUNTIME-SEMANTICS (rond r. 148-158) al
als tracked deviation draagt; zonder die herschrijving breekt
`jutterswet-peildatum` (numerieke EERSTE/LAATSTE, 4 knopen).

Meetfase (2026-09-04, volledig; 24 verifier-schone /tmp-documenten):

* **Vorm A, set-bron** (`elementObjecttype` + set): ref speelt één
  bijdrager stil goed en fault op twee-plus met `unordered comparison`
  — óók op drie keer dezelfde waarde (`Zuid vs Zuid`), en gemeten
  datumafhankelijk (een peildatum-filter bepaalt of het document speelt
  of fault). clingo weigert luid (enum-fence resp. `unscalable
  datatype`/`not translated`). Drools: EERSTE/LAATSTE transpiler-SKIP;
  MINIMUM/MAXIMUM over enum een **ERROR** (`IllegalStateException: kan
  accumulate-resultaat niet naar BigDecimal omzetten`, ook bij één
  bijdrager); SumType loader-EXCLUDE.
* **Vorm B, geleed** (twee-plus opgesomde operanden): ref fault; clingo
  weigert (enum), **crasht** op de gelede som-EERSTE/LAATSTE
  (`Division_by_zero` — eigen kaartje
  `20260904-clingo-symbolische-schaal.md`), of weigert (som-MIN/MAX);
  Drools **antwoordt een waarde** — hoofdlettergevoelig ASCII op het
  label, beide transpilertakken — waar ref fault: een ref ≢
  Drools-waardedivergentie, vandaag alleen onzichtbaar omdat geen
  corpusdocument de vorm draagt.
* **v0**: EERSTE/LAATSTE hebben geen instance-fold-vorm
  (`isAggregatieKind`); de frontend weigert equality-only-operanden al
  bij typering; waar het toch bij de runtime komt is de orde
  Java-`Comparable` — alfabetisch, per ongeluk, niemands ontwerp.
* **Corpus**: 0 dragers in golden/rood/parity; de enige vijf
  negatief-dragers zijn al ill-formed op WF-089/WF-090 en krijgen een
  toegestane incidentele regel.

Onder het besluit worden de drie gemeten breuken (Drools-ERROR,
clingo-crash, ref≢Drools-waarde) onbereikbaar via geldige documenten.

Acceptance:
- Rood-eerst per fold-soort en per vorm (set-bron én geleed); daarna de
  WF-038-clausule met de WF-036-machinerie (`is_number_like`/`is_date`,
  familie per fold, defer op `Onbekend`/afwezig), clausule-exact
  mutant-bewezen.
- Negatief per fold-soort; sweeps over de vier zones met nul
  over-afwijzing (0 golden/rood/parity — gemeten verwachting).
- De WF-038-tekst (IR-SPEC ~r. 2699-2701) en de tracked-deviation-
  passage (RUNTIME-SEMANTICS ~r. 148-158) herschreven naar de
  familie-lezing (voorstel); de WF-038-statuskolom ("fold
  unit-compatibility OPEN") en de meetzinnen in de WF-089/WF-090-
  passages (IR-SPEC ~r. 514-522, ~r. 767-775) plus de vier
  NOTE-falsifieerbaarheidsnotities van de doorgeefluik-negatiefs mee.
- Alle dertien gates groen; geen bestaande pin beweegt.

NB (meetfase, buiten dit besluit): de set-bron EERSTE/LAATSTE over wél
ordenbare typen (datums) is frontend-emitteerbaar en verifier-schoon
maar wordt alleen door ref gedragen — clingo en Drools weigeren luid.
Dat is de staande, gedocumenteerde executor-asymmetrie; een
clingo-arm-kaart is optioneel vervolgwerk. En een payload-loos SumType
bestaat niet (WF-087 normaliseert naar Enumeratie) — die cel vervalt.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/negatief/**`,
`backlog/`.

Out of scope: een orde definiëren (verworpen 2026-09-04: ref-signatuur
tot in `scalar_cmp`, ontbrekende clingo-armen, drie Drools-stukken op
twee takken, en een bewuste divergentie van de gemeten
productiesemantiek); de clingo-crash zelf (eigen kaartje); executors.

---

Result (2026-09-05): **geland — de familie-lezing is geratificeerd en
machinaal gedekt.** WF-038 draagt sinds deze kaart de
ordenende-fold-operandclausule: elke operand van
`EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM` komt uit één vergelijkbare
familie — getal óf datum — ongeacht de wire-vorm, want de regel leest de
argumentenlijst en niets anders; met een defer op de onbekend-sentinel
en op een operand zonder type. Negatiefzone 281 → **286**, vier zones
486 → **491**. (De fixronde hieronder verstrakt de datumhelft naar
precisie en zet die tellers op 287 resp. 492.)

*Rood eerst, per vorm × fold-soort.* Vijf mutant-documenten, alle vijf
`verifier: PASS` onder de verifier van de dag ervoor, met de gemeten
uitvoerder-ellende eronder (gate 3 daarop rood, commit `3187f46`;
groen na de clausule, commit `c927fca`):

| document | ref | clingo | Drools |
|---|---|---|---|
| `wf038-minimum-enum-setbron` (set-bron, Enumeratie) | `unordered comparison: Noord vs Zuid`, maar bij één bijdrager stil goed | enum-fence, weigert luid | **ERROR** `IllegalStateException: kan accumulate-resultaat niet naar BigDecimal omzetten: class java.lang.String` — óók bij één bijdrager |
| `wf038-maximum-somtype-setbron` (set-bron, SumType) | `unordered comparison: Vermist vs Geborgen(10)` | `unscalable datatype in the ASP translation` | loader-EXCLUDE `Datatype:SumType` |
| `wf038-laatste-enum-geleed` (geleed, Enumeratie) | `unordered comparison: Noord vs Zuid` | enum-fence | **speelt en antwoordt `Zuid`** (`PLAYED 1`) — alfabetisch op het label, waar ref fault |
| `wf038-eerste-somtype-geleed` (geleed, SumType) | `unordered comparison: Vermist vs Geborgen(10)` | **crasht**: `Fatal error: exception Division_by_zero` | loader-EXCLUDE `Datatype:SumType` |
| `wf038-eerste-gemengde-familie` (geleed, datum + getal) | `unordered comparison: 3 vs 2026-05-01` | weigert bij naam (gemengde operanden) | **ERROR** `ClassCastException: BigDecimal cannot be cast to ChronoLocalDate` |

De kaartmeting is daarmee live gereproduceerd, inclusief de drie breuken.
Buiten de vier gecommitteerde cellen ook gemeten en niet gecommitteerd:
`Tekst` en `Booleaan` in de gelede vorm speelden **beide** in Drools
(`PLAYED 1`) waar ref fault — dezelfde waardedivergentie, wat de reden is
dat de clausule op ordenbaarheid is geschreven en niet op een lijstje
verboden typen.

*De clausule.* `spec/verifier/lib/checks.ml`, in de WF-038-groep van de
`Functie`-arm, twee armen op `passthrough_folds`: per operand een
familie-test (`is_number_like`/`is_date`, de WF-036-machinerie), en over
de operanden heen één familie. Meldingen in huisstijl: `WF-038 FAIL
/…/argumenten/0: operand of the MINIMUM ordering fold is enumeration,
must be a number or a date` en, op de fold-knoop zelf, `WF-038 FAIL
/…/waarde: EERSTE orders operands of two comparable families, numbers and
dates, which share no ordering`.

*Mutant-bewijs, per arm, volledige byte-sweep over de vier zones
(153 golden + 22 rood + 286 negatief + 30 parity = 491 documenten).*
Clausule aan vs. uit: **10 documentrapporten, 5 verdicts** — de tellingen
hieronder zijn documenten, niet regels (in regels: 12 WF-038-regels voor
de hele clausule, en 11/1/1/5 voor de vier armmutanten in dezelfde
volgorde) — de vijf nieuwe
negatiefs, plus één toegestane incidentele WF-038-regel op elk van de
vijf al-ill-formede doorgeefluik-negatiefs
(`wf089-aggregatie-doorgeefluik-enum`, `wf089-foldbron-enum`,
`wf089-minimum-doorgeefluik`, `wf090-doorgeefluik`, `wf090-foldbron`).
**Nul golden, nul rood, nul parity beweegt.** Per arm:

- per-operand-arm → `[]`: 9 rapporten, **4** verdicts — de vier
  equality-only-negatiefs vrij, de gemengde familie blijft rood;
- familie-kruising → `[]`: 1 rapport, **1** verdict — alléén
  `wf038-eerste-gemengde-familie` vrij;
- defer op de sentinel weg: **1 rapport, 0 verdicts** — precies het ene
  parity-document met de ongetypeerde tweede operand krijgt een regel.
  Dat is de parity-defer-meting: het rapport van dat document is
  byte-identiek onder de clausule;
- `family` teruggezet naar de **letter van de oude WF-038-tekst**
  (eerste/laatste eisen datums, min/max getallen): 2 rapporten, **1**
  verdict — `corpus/golden/jutterswet-peildatum` breekt, met vier
  geweigerde operandposities op zijn twee numerieke `EERSTE`/`LAATSTE`-
  knopen. Precies de reden waarom de tekst herschreven moest worden; onder
  de familie-lezing blijft die golden schoon (gates 3, 5, 6 groen).

*Dekking buiten de gecommitteerde vier.* Alle 32 cellen van 4
fold-soorten × 4 equality-only-typen (Enumeratie, SumType, Tekst,
Booleaan) × 2 wire-vormen worden geweigerd; en de drie gemeten breuken
zijn onder de clausule ill-formed invoer — het Drools-ERROR-document
(`wf038-minimum-enum-setbron`, en gemeten ook zijn MAXIMUM-tweeling), het
clingo-crashdocument (`wf038-eerste-somtype-geleed`) en het
ref≢Drools-waardedocument (`wf038-laatste-enum-geleed`, en gemeten ook de
`EERSTE`-variant) worden alle drie afgewezen.

*Teksten (voorstel).* De WF-038-clausule in `spec/IR-SPEC.md` ch. 4.6 is
herschreven naar de familie-lezing, met de vormonafhankelijke lezing, de
defer, de gemeten uitvoerder-asymmetrie en de reden dat het een regel is
en geen notitie. De tracked deviation in `spec/RUNTIME-SEMANTICS.md` ch. 1.2 is
**geratificeerd** (met twee gewerkte voorbeelden van de grens, waaronder
de alfabetische `Comparable`-orde die niet geratificeerd is). De
WF-038-statuskolom noemt de familie-clausule als gate-3-gedekt en de
open helften expliciet; de WF-089- en WF-090-orderbaarheidspassages zijn
van "gemeten gat" naar "gesloten" gebracht; de vijf doorgeefluik-NOTE's
hebben een hermeten falsifieerbaarheidszin gekregen (hun oude zinnen
werden onwaar: onder hun eigen mutant wordt het document nu nog steeds
afgewezen, maar op WF-038 in plaats van op de regel die de directorynaam
verklaart — dát is nu het falsificatiesignaal).

*Afwijkingen van de kaart, expliciet.* (1) De kaart vroeg minstens één
negatief per fold-soort (vier); er zijn er **vijf** — de
familie-kruisingsarm heeft een eigen, arm-exact falsifieerbare getuige
nodig, anders verandert het schrappen ervan geen enkel document. (2) De
kaart noemde *vier* doorgeefluik-NOTE's; **vijf** droegen een zin die de
clausule onwaar maakt, dus alle vijf zijn hermeten. (3) De oude
WF-038-tekst eiste bij eerste/laatste datums *van gelijke precisie*. De
familie-lezing haalt die eis uit de gecontroleerde MUST; hij is daarom —
naast de eenhedenhelft die de kaart al open liet — **expliciet als OPEN**
benoemd in tekst én statuskolom, niet stilzwijgend verdwenen. *(In de
fixronde hieronder alsnog gesloten; alleen de eenhedenhelft blijft
open.)* (4)
`spec/ASP-TRANSLATION.md` r. 818 noemt de verbreding nog "tracked";
buiten may-touch, dus niet aangeraakt — een regelbrede opvolgnotitie.
*(In de fixronde bijgewerkt; may-touch daarvoor verruimd, zie daar.)*

*Prijs.* Nul golden, nul rood, nul parity geraakt; geen pin bewogen; geen
executor, frontend of bestaande golden aangeraakt.

---

**Fixronde (2026-09-05).** Twee lenzen lazen de landing na. Beide
bevestigden de kern — het mutant-bewijs clausule-exact, alle 32 cellen
dicht, nul beweging buiten de negatiefs, de vijf hermeten NOTE-zinnen
waar — en leverden vier punten. Alle vier uitgevoerd, elk hermeten.

*(1) De defer-rechtvaardiging was door de repo weerlegd.* De
landingstekst rechtvaardigde de sentinel-defer met het parity-document:
"weigeren zou documenten weigeren die de toolchain emitteert". Gemeten
is dat onwaar. Dat document is zélf al verifier-afgewezen (33
overtredingen) en draagt op precies die operandpositie al een
WF-034-regel (de ch. 4.3-fallback die niet kan invallen); 28 van de 30
parity-documenten zijn afgewezen; en **geen enkele gate draait de
verifier over de parity-zone** — gate 3 loopt drie corpuszones, de
parity-gate roept de verifier niet aan. De defer-mutant zegt hetzelfde
van de andere kant: 1 rapport, **0** verdicts. Er valt daar dus geen
document te sparen.

Waar de defer wél draagt is de ándere vorm: de ch. 4.3 row-1-wart in een
**instance**-fold-bron. Gemeten op een `MINIMUM` over een kale rolbron
(de sentinel-getypeerde bron van `jutterswet-vondsttelling`, kind
omgezet): `verifier: PASS` — en geen uitvoerder die hem draagt. ref
`EVALUATION FAULT: value aggregation over a bare role source`; clingo
`TRANSLATION FAULT: expression kind not translated (no corpus case
exercises it)`; Drools `ERROR ConstraintEvaluationException`. In de
gelede vorm zónder `elementObjecttype` is de defer daarentegen
**onbereikbaar**: WF-034 weigert de sentinel daar zelf (`unknown-type
sentinel inside a structured node`, gemeten op een gebroken
`jutterswet-hoogwaterpaar`; een operand zónder `type`-veld laadt niet
eens). Spec-zin en spiegelcommentaar in `checks.ml` dragen nu die
eerlijke vorm: dood waar onbereikbaar, dragend waar bereikbaar — en wat
hij draagt is een **open flank**, geen gezond verklaarde vorm. Dat is
als tiende positie toegevoegd aan het bestaande open-vlak van de
foldbron-tak (naast de negen argumenttypes), met de losse regel over de
verifier-loze parity-zone erbij.

*(2) De datumprecisie-cel is dicht, en kostte niets.* De familie-functie
tagt de datumfamilie nu per **representatie** (`Datum_in_dagen` en
`Datum_tijd` apart), en de kruisingsarm splitst zijn melding: een
precisiemix leest niet meer als een getal-vs-datum-mix.

- *Rood eerst*, commit `3c4b777`: `wf038-eerste-gemengde-datumprecisie`,
  een hand-gebroken `jutterswet-hoogwaterpaar` waarin drie velden
  bewegen (de declaratie `Getijdendag.avondhoogwater` van `DatumTijd`
  naar `DatumInDagen` plus de twee waarheidsgetrouwe operandannotaties)
  — één defect, beide foldknopen geweigerd. Vóór de clausule
  `verifier: PASS`, gate 3 daarop rood; en de drieslag eronder: ref
  `EVALUATION FAULT: unordered comparison: 2026-06-21 vs
  @1782022200000ms`, clingo `TRANSLATION FAULT: a non-datum-tijd (or
  mixed-granularity) operand under a datum-tijd eerste/laatste fold has
  no order here`, Drools een `ClassCastException` van `LocalDate` naar
  `ChronoLocalDate`.
- *Ná*, commit `ff00e3f`: `WF-038 FAIL /resolvedRegels/0/waarde: EERSTE
  orders dates of two precisions (day and millisecond), which share no
  order` en dezelfde regel voor `LAATSTE` op knoop 1.
- *Mutant, clausule-exact*: laat beide datumrepresentaties één familie
  delen (de vorm die de clausule bij de landing had) → **1 rapport, 1
  verdict**, precies dit document wordt schoon; geen ander document
  beweegt.
- *Volledige byte-sweep* over de vier zones (153 golden + 22 rood + 287
  negatief + 30 parity = **492** documenten): precies één verschil met
  de sweep van vóór de verstrakking, namelijk het nieuwe negatief zelf.
  **Nul golden, nul rood, nul parity, nul ander negatief.** De
  verstrakking was gratis. Negatiefzone 286 → **287**, vier zones
  491 → **492**.
- De RegelSpraak-frontend dwong deze eis al af bij typering
  (`Typering.datumAggregatie`: "alle datumexpressies moeten dezelfde
  tijd-precisie hebben"), dus er gaat geen emitteerbare vorm verloren —
  de verstrakking brengt wire-hek en frontend in overeenstemming. Dat
  staat nu ook in RS ch. 1.2.

*(3) De WF-036-parenthese was half onwaar.* De landingstekst schreef dat
WF-036 "beide kruist" voor een vergelijking. Gemeten: WF-036 kruist
eenheden bij een numerieke ordening en bij gelijkheid, en datumprecisie
alléén bij **gelijkheid**; zijn datum-ORDENINGsarm kruist geen van beide
— een `LATER_OF_GELIJK` tussen `DatumInDagen` en `DatumTijd` is
`verifier: PASS`, met ref `EVALUATION FAULT: unordered comparison:
2025-03-05 vs @1740816000000ms` en clingo `TRANSLATION FAULT:
unscalable datatype in the ASP translation`. De zin is herschreven, en
die arm heeft nu een eigen kaart:
`backlog/new/20260905-wf036-datumordening-precisie.md`.

*(4) Kleine punten, elk één edit.* (a) `spec/ASP-TRANSLATION.md` r. 818
noemt de verbreding niet meer "tracked" maar "ratified 2026-09-04" —
**may-touch verruimd** met `spec/ASP-TRANSLATION.md`, hier gemeld
(geverifieerd geen beschermd pad). (b) `20260825-fold-geleed.md` kreeg
een gedateerde `Resolved 2026-09-05`-regel onder zijn
ratification-pending-vraag. (c) "10 rapporten" heet nu "10
documentrapporten", met de regeltelling (12/11/1/1/5) als bijzin — de
twee tellingen waren niet uit elkaar te houden. (d) De
twee-vormen-dichotomie is uit het proza: de regel leest `argumenten` en
niets anders, en gemeten dragen **6 van de 14** gelede ordenende folds
in de vier zones óók `elementObjecttype` — vorm is geen dichotomie om op
te sturen, en dat de regel er niet op stuurt is haar kracht. (e) De
"twee negatiefs op deze positie"-zin in ch. 2.4 telt er **drie**
(`wf089-foldbron-enum` hoort erbij; gemeten in de clausule-mutant). (f)
Een eerlijke zin dat de `Percentage`-helft van de getalfamilie
corpus-ongetest is: `Percentage` uit de familie halen beweegt **nul** van
de 492 rapporten. (g) Een zin dat `jutterswet-peildatum` het enige
document is waarvan het verdict de oude letter onderscheidt (gemeten: de
oude-letter-mutant kantelt alleen dat document), dat het geen `bron.rgs`
draagt en ook niet kan — de verbreding is **wire-first**, de
frontend-doc-comment draagt nog de v0-letter, genoteerd als flank. (h)
De lijst-weigeringsmelding gebruikt `datatype_detail`, zodat het
elementtype meekomt ("is a list of enumeration …") in plaats van een kale
"list".

*Wat open bleef.* De eenhedenhelft van WF-038 (geen getuige, geen arm) —
onveranderd open, nu de enige helft. De datum-ORDENINGsarm van WF-036 —
gemeten, benoemd, eigen kaart. De instance-fold-wart onder een ordenende
fold — verifier-schoon, door geen uitvoerder gedragen, als open flank
opgeschreven in spec én commentaar in plaats van weggedefinieerd. En de
frontend-doc-comment die nog de v0-letter draagt: buiten may-touch, dus
alleen genoteerd.

**Gates** (alle dertien, na de fixronde):

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 175 file(s) validate and round-trip)
    check-verifier: PASS (175 golden clean; 286 negatief rejected for their declared rule)
    check-case-schema: PASS (schema valid; 425 case file(s) validate)
    check-oracle: PASS (403 cases replayed exactly; 3 goldens without a case file)
    check-clingo: PASS (403 cases replay exactly through clingo)
    check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
    check-rood: PASS (22 red cases fault observably in ref and clingo; Drools leg 5 fault-pinned, 17 not-carried)
    check-catala: PASS — frontend suite green (201 checks); 12 catala goldens byte-identical
    check-regelspraak: PASS — JUnit suite green (171 tests); 29 rgs goldens byte-identical
    check-toka-parity: PASS (45 .rgs byte-identical, 2 holdouts; 29 ref / 29 clingo / 29 Drools, 0 divergenties; suite 27 tests)
    check-drools: PASS (played 106, auto-excluded 43 with reason, 1 value-divergence, 3 no-case, JUnit 199)
    check-reftype-derivation: PASS (52 unit annotations twisted over 11 goldens; 50 executor replays; 0 pinned survivors)
