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
familie — getal óf datum — in **beide** wire-vormen, met een defer op de
onbekend-sentinel en op een operand zonder type. Negatiefzone 281 →
**286**, vier zones 486 → **491**.

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
Clausule aan vs. uit: **10 rapporten, 5 verdicts** — de vijf nieuwe
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
herschreven naar de familie-lezing, met de twee wire-vormen, de defer, de
gemeten uitvoerder-asymmetrie en de reden dat het een regel is en geen
notitie. De tracked deviation in `spec/RUNTIME-SEMANTICS.md` ch. 1.2 is
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
benoemd in tekst én statuskolom, niet stilzwijgend verdwenen. (4)
`spec/ASP-TRANSLATION.md` r. 818 noemt de verbreding nog "tracked";
buiten may-touch, dus niet aangeraakt — een regelbrede opvolgnotitie.

*Prijs.* Nul golden, nul rood, nul parity geraakt; geen pin bewogen; geen
executor, frontend of bestaande golden aangeraakt.

**Gates** (alle dertien):

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
