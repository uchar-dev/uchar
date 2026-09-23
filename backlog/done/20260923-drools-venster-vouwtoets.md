# drools-venster-vouwtoets: exec te streng onder ALLE, DRL leunt op de verifier

Gemeten door de tijdvenster-kaart (2026-09-23, kwantorvouwmeting): een
periodevenster in de toets van een `Kwantorvouw` speelt onder `ALLE`
in ref, clingo en de DRL-tak, maar de exec-tak weigert hem ook daar —
te streng, een kanaalasymmetrie zonder fout antwoord. Andersom
verlaagde de DRL-tak het venster onder ALLE vijf kwantoren en gaf
onder `GEEN` in een tijdvrije sessie het COMPLEMENT als antwoord;
sinds WF-112 houdt de verifier die documenten tegen, dus de flank is
dicht, maar de DRL-tak zelf draagt geen eigen weigering — wie de
verifier omzeilt (een oude wire, een handgemaakt document) krijgt
weer het complement. Huisbeginsel: elke uitvoerder weigert zelf, de
verifier is het eerste hek en niet het enige.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide flanken (exec-weigering onder ALLE; het
  DRL-complement op een WF-112-document met de verifier omzeild).
- (b) De exec-tak speelt het venster in een vouwtoets onder ALLE en
  DE (gelijk aan ref); de DRL-tak weigert zelf bij naam onder GEEN en
  de tellende kwantoren. Rood-eerst getuigen beide takken.
- (c) Corpussweep: kanaalwissels benoemd (een spelende exec-vorm kan
  pins bewegen — voorstel); alle veertien gates groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh`
(**voorstel**, pins/lijsten), kaartverplaatsing. NB: serieel met
andere Drools-kaarten.

## Aangescherpt bij de claim (2026-09-23, basis main 8dd05e25)

- **Welke kwantoren de wire kent.** Het schema laat op `Kwantorvouw.kwantor`
  vijf waarden toe: `ALLE`, `GEEN`, `TEN_MINSTE`, `TEN_HOOGSTE`, `PRECIES`.
  `DE` is geen vouwkwantor (WF-108 (a); het schema kent hem niet, de loader
  weigert hem bij naam). WF-112 laat het venster alleen onder `ALLE` en `DE`
  toe, dus in een vouw alleen onder `ALLE`, en binnen de toets als lid van
  een `ALLE`- of `DE`-samenstelling op elke diepte. `ref` trekt precies die
  grens: `periode_onder_telkwantor` bij elke samenstelling en elke vouw, met
  de diepe wandeling `bevat_periode` over filter en toets.
- **Posities.** De toets van een `ALLE`-vouw, en ook het FILTER. Het filter
  hoort erbij omdat maintainerbesluit B (ch. 3.12, rij "A time window inside
  the `filter` of an `ALLE` quantifier fold") de exec-SKIP daar met naam aan
  deze kaart toewijst. Beide routes van de vouw: de niet-temporele matrix, en
  voor het filter ook de §8.2-heelperioderoute.
- **Onderwerp en periode meten.** De ploeg (het onderwerp van de vouw) krijgt
  een datumattribuut met dezelfde naam als de bijdrager, met waarden die bij
  een ploeglezing een ander verdict geven. De periode wordt gemeten met `tot`
  tegen `tot en met` op de evaluatiedatum zelf, met een lege grens en met een
  tijdvrije sessie.
- **Weigering.** Beide takken weigeren elke WF-112-positie zelf, met één
  gedeelde zin die construct, kwantor en WF-112 noemt.

## Result (2026-09-23, branch card/drools-venster-vouwtoets)

Beide flanken zijn dicht. De exec-tak speelt het venster in toets en filter
van een `ALLE`-vouw met de waarden van `ref`. Dat geldt ook voor het venster
in het die/dat-filter van een aggregatie: bijvangst in hetzelfde mechanisme,
zie de may-touch-verbreding. Beide Drools-takken weigeren de gemeten
WF-112-posities (in de vouw, als regelvoorwaarde en in een aggregatiefilter)
zelf, bij naam, met één gedeelde zin. Geen golden draagt een van deze vormen,
dus geen corpuskanaal bewoog en `scripts/check-drools.sh` is niet aangeraakt.
Alle veertien gates zijn groen.

### (a) Reproductie, vijf uitvoerders per flank

Proefdocumenten zijn gebouwd uit `jutterswet-schouwploeg`. Er is telkens één
regel gehouden, met `aanstelling` (DatumInDagen) op `Strandwacht` én op
`Schouwploeg`. Er zijn zes ploegen (S1..S6) en acht wachten; de
evaluatiedatum is 2024-06-15 ("binnen") of null ("tijdvrij"). Eén document
komt uit `jutterswet-ploegpremie`, voor de §8.2-route. Alle verwachtingen
zijn vooraf met de hand berekend. Bij `GEEN` en de tellende kwantoren is de
verwachting het antwoord van een naïeve verlaging, zodat PLAYED betekent dat
de tak dat antwoord gaf. "voor" is de jar van main 8dd05e25, "na" die van
deze branch.

| document | case | verifier | ref | clingo | exec voor | drl voor | exec na | drl na |
|---|---|---|---|---|---|---|---|---|
| toets = venster vanaf eigen aanstelling, ALLE | binnen, tijdvrij | PASS | PASS | PASS | EXCL "een conditie van soort Periode" | PLAYED | PLAYED | PLAYED |
| toets = literal venster, ALLE | binnen, tijdvrij | PASS | PASS | PASS | EXCL (idem) | PLAYED | PLAYED | PLAYED |
| toets `tot 15 juni` / `tot en met 15 juni` | binnen | PASS | PASS | PASS | EXCL | PLAYED | PLAYED | PLAYED |
| toets = ALLE[gediplomeerd, venster] en DE[…] | binnen (+ tijdvrij) | PASS | PASS | PASS | EXCL "in de ALLE-samenstelling: …" | PLAYED | PLAYED | PLAYED |
| toets = ALLE[dienstjaren > keurgrens, venster] | keurgrens 5 en 9 | PASS | PASS | PASS | EXCL | PLAYED | PLAYED | PLAYED |
| filter = venster (literal of attribuut), ALLE | binnen, tijdvrij | PASS | PASS | PASS | EXCL "een die/dat-subselectie die deze tak niet op de source kan binden" | PLAYED | PLAYED | PLAYED |
| §8.2-heelperiodevouw, filter = venster | binnen, tijdvrij | PASS | PASS | PASS | EXCL (idem) | PLAYED | PLAYED | PLAYED |
| telling, filter = venster (en ALLE[gediplomeerd, venster]) | binnen, tijdvrij | PASS | PASS | PASS | EXCL "aggregatie buiten Phase-C scope" | PLAYED | PLAYED | PLAYED |
| diezelfde telling als operand van de voorwaarde | binnen, tijdvrij | PASS | PASS | PASS | EXCL "voorwaarde buiten Phase-B scope" | PLAYED | PLAYED | PLAYED |
| toets = venster onder GEEN, TEN_MINSTE 1, TEN_HOOGSTE 1, PRECIES 1 | binnen, tijdvrij | WF-112 | fout WF-112 | fout WF-112 | EXCL "soort Periode" | **PLAYED** | EXCL WF-112 | EXCL WF-112 |
| filter = venster onder dezelfde vier | binnen, tijdvrij | WF-112 | fout WF-112 | fout WF-112 | EXCL "die/dat-subselectie" | **PLAYED** | EXCL WF-112 | EXCL WF-112 |
| toets = GEEN[venster] in een ALLE-vouw | tijdvrij | WF-112 | fout WF-112 | fout WF-112 | EXCL "in de GEEN-samenstelling: …" | EXCL "matrixvorm" | EXCL WF-112 | EXCL WF-112 |
| telling, filter = GEEN[venster] | tijdvrij | WF-112 | fout WF-112 | fout WF-112 | EXCL "aggregatie buiten Phase-C scope" | EXCL "aggregatie AANTAL" | EXCL WF-112 | EXCL WF-112 |
| diezelfde telling als operand van de voorwaarde | tijdvrij | WF-112 | fout WF-112 | fout WF-112 | EXCL "voorwaarde buiten Phase-B scope" | EXCL "TODO voorwaarde" | EXCL WF-112 | EXCL WF-112 |

De DRL-flank ook op het corpusdocument zelf:
`corpus/negatief/wf112-venster-in-geen-vouw/ir.json`, zonder verifier
afgespeeld met `jutterswet-schouwploeg/case1-allen-ervaren.json` (tijdvrij,
pint S1 bevoegd AANWEZIG, het complement). Voor: verifier `WF-112 FAIL`, ref
`EVALUATION FAULT: a period condition under GEEN …`, clingo `TRANSLATION
FAULT: …`, exec `EXCLUDE … een conditie van soort Periode`, drl **`PLAYED 1`**.
Na: exec en drl allebei `EXCLUDE … periodevenster in filter of toets van een
kwantorvouw onder GEEN (WF-112): …`.

### (b) Keuze: bedraden in de exec-tak, weigeren in beide takken

**Het onderwerp is gemeten, niet aangenomen.** In de toets zou een lezing
van de PLOEGdatum {S2, S4, S6} geven (met de hand berekend). `ref`, clingo en
de DRL-tak geven gemeten {S1, S4, S5}. Die twee verschillen op S1, S2, S5 en
S6. De exec-tak geeft na de bedrading gemeten {S1, S4, S5}. De flankrij
(`de_lezing_van_de_ploeg_zelf_…`) laat zien dat beide takken de ploeglezing
als verwachting afwijzen. In het filter zou een ploeglezing {S1, S2, S3, S4,
S5} geven en een filter dat niets laat vallen {S1, S2, S3, S4} (met de hand;
het tweede ook gemeten als mutant M9). Gemeten is {S1, S2, S3, S4, S6}, in
alle vier de uitvoerders. De telling geeft per ploeg 2, 0, 1, 0, 1, 1; een
ploeglezing zou 0, 1, 0, 0, 0, 2 geven. De periode: `tot 15 juni` geeft op
15 juni {S4}, `tot en met 15 juni` alle zes; een lege grens (W5) matcht
nooit; tijdvrij houdt alleen de lege ploeg S4.

**Hoe.** In de exec-tak is de vensterlezing van de regelvoorwaarde nu één
functie, `vensterToets(per, subject)`. De vouwtoets en elk die/dat-filter
gebruiken dezelfde functie, met het ELEMENT als subject. De matrix (`Matrix`)
kreeg een derde argument, de rekendatum. De eval na de
collectList-accumulate bindt die als patroonvariabele, naast een eventuele
parameterglobal (Predicate3). In `filterSource` wordt het venster een
conjunct op de accumulate-bron; dat is het gedeelde filter van vouw en
aggregatie. De registratie van de rekendatum-global (`heeftTijdgebonden`)
keek alleen naar condities en naar de rekendatumconstante. Ze vindt nu ook
een venster in het filter van een aggregatie, in een waarde of in een
operand. De weigering zit in één nieuwe kernklasse,
`transpiler/kern/Vensterpositie`: de diepe wandeling van de verifier en één
zin. Beide takken roepen die als eerste aan in hun vouwlowering. Dezelfde zin
vervangt in beide takken ook de weigering van de SAMENSTELLINGSarm van
WF-112: als regelvoorwaarde, en als aggregatiefilter in een waarde of in een
operand van een voorwaarde (`Vensterpositie.inExpressie` zoekt de filters in
een expressie af). Daar weigerden de takken al, maar de exec-tak met een
algemene scopezin en de DRL-tak zonder regelnummer.

**Waarom de exec-weigering nodig is (mutant M4 hieronder).** Met de
bedrading en zonder de weigering speelt de exec-tak het venster onder GEEN
en de tellende kwantoren. Gemeten met een jar onder M4: op het corpusnegatief
met de tijdvrije case 1 `PLAYED 1`, dus het complement. Ook alle zestien
cellen van de proefdocumenten (toets en filter, vier kwantoren, binnen en
tijdvrij) gaven `PLAYED 1` op de naïeve verwachting.

### Rood-eerst (letterlijk)

Getuige `backends/drools/src/test/java/…/cases/VensterVouwWitnessTest.java`,
14 rijen. Tegen de transpilers van main 8dd05e25 (`git show`, daarna
teruggezet, `cmp` gelijk aan HEAD):

    Tests run: 14, Failures: 4, Errors: 10, Skipped: 0
    …de_wf112_negatieven_van_het_corpus_weigeren_beide_takken_bij_naam exec-model names 'kwantorvouw onder GEEN (WF-112)': transpiler SKIP: … (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een conditie van soort Periode) …
    …het_venster_in_de_toets_leest_de_grens_van_elke_bijdrager » Excluded transpiler SKIP: … een conditie van soort Periode …
    …het_venster_in_het_filter_kiest_de_bijdragers_per_bijdrager » Excluded transpiler SKIP: … (een die/dat-subselectie die deze tak niet op de source kan binden) …
    …het_venster_in_het_filter_van_een_aggregatie_telt_per_element » Excluded transpiler SKIP: regel: wachttal van een schouwploeg — regel (aggregatie buiten Phase-C scope) …
    …de_telling_met_een_venster_als_operand_van_een_voorwaarde » Excluded transpiler SKIP: regel: bevoegd bij diploma — regel (voorwaarde buiten Phase-B scope) …
    …een_venster_onder_geen_in_een_aggregatiefilter_wordt_bij_naam_geweigerd exec-model names 'GEEN-samenstelling (WF-112)': transpiler SKIP: regel: wachttal van een schouwploeg — regel (aggregatie buiten Phase-C scope) …

De DRL-helft liep eerst mee in een tussenstand: de exec-helft was gebouwd,
de DRL-weigering nog niet (toen 9 rijen).

    Tests run: 9, Failures: 2, Errors: 0, Skipped: 0
    drl-tekst — het venster in de toets onder GEEN ==> Expected …CaseRunner.Excluded to be thrown, but nothing was thrown.
    drl-tekst names 'GEEN-samenstelling (WF-112)': drl-transpiler MARKER: … een matrixvorm die deze tak niet IN het elementpatroon kan schrijven (Samengesteld) …

Na de wijziging: `Tests run: 14, Failures: 0, Errors: 0`. Backendsuite 634
tests (620 + 14), 0 failures, 0 skipped.

### Mutatieproeven

Elke mutant is één letterlijke vervanging. Daarna liep de getuigeklasse, en
het bestand ging met `cp` terug, `cmp` identiek (voor elke mutant
bevestigd). M11, M12 en M15 zijn gedraaid met de eindgetuige van 14 rijen,
M3, M9 en M14 met 13 rijen, de andere met 12. De later toegevoegde rijen
raken die andere mutanten niet.

| mutant | uitkomst |
|---|---|
| M1 DRL-vouwweigering uit | 3 failures, alle drl-tekst: `…onder GEEN ==> Expected …Excluded to be thrown, but nothing was thrown` (ook het corpusnegatief) |
| M2 exec-toetsbedrading uit | 6 errors, `Excluded … soort Periode` |
| M3 vensterarm in `filterSource` uit | 4 errors, `Excluded … die/dat-subselectie …`, `… aggregatie buiten Phase-C scope`, `… voorwaarde buiten Phase-B scope` |
| M4 exec-vouwweigering uit | 3 failures, exec-model `Expected …Excluded to be thrown, but nothing was thrown` |
| M5 exec-samenstellingsnaam (regelvoorwaarde) uit | 1 failure: `exec-model names 'GEEN-samenstelling (WF-112)': … regel (voorwaarde buiten Phase-B scope)` |
| M6 DRL-samenstellingszin terug naar de oude | 1 failure: `drl-tekst names 'GEEN-samenstelling (WF-112)': … periodevergelijking onder een tellende kwantor (§8.4.3)` |
| M7 `tot en met` genegeerd in `vensterToets` | 1 failure: `tot en met 15 juni … verwacht aanwezig, werkelijk afwezig` |
| M8 toetsvenster altijd waar | 6 failures op WAARDE (`verwacht afwezig, werkelijk aanwezig`) |
| M9 filtervenster laat iedereen door | 4 failures op WAARDE (S6, de telling, de voorwaardetelling, P1 in de heelperiodevouw) |
| M10 registratie kijkt niet in het aggregatiefilter van een waarde | 1 error, `… aggregatie buiten Phase-C scope` |
| M11 DRL-aggregatienaam (waarde) terug naar de oude | 1 failure: `drl-tekst names 'GEEN-samenstelling (WF-112)': … TODO aggregatie AANTAL` |
| M12 exec-filternaam (WF-112) in `filterSource` uit | 1 failure: `exec-model names 'GEEN-samenstelling (WF-112)': … aggregatie buiten Phase-C scope` |
| M14 registratie kijkt niet in het aggregatiefilter van een voorwaarde | 1 error, `… voorwaarde buiten Phase-B scope` |
| M15 DRL-label van de vergelijking terug naar "voorwaarde" | 1 failure: `drl-tekst names 'GEEN-samenstelling (WF-112)': … TODO voorwaarde: het aantal wachten …` |

(M13 was een tweede spelling van M3 en gaf dezelfde vier rijen.) M8 en M9
kwamen uit een lensronde op de eigen getuige. De eerste filterrij
onderscheidde een weggelaten venster niet van een toegepast venster, omdat
elke wacht buiten het venster gediplomeerd was. Daarom is S6 toegevoegd, met
een wacht buiten het venster die de toets niet haalt (commit 49ef51f6).

### (c) Corpussweep beide takken, vóór en na

235 goldens, elk door exec en drl: de jar van main 8dd05e25 tegen de jar van
de eindcode 57e9fe4f. **Byte-identiek**, 470 statusregels (`diff` leeg). Dat
gold ook voor de tussenjars van 8e0ab9d8 (op commentaar na) en bd352be6.
Gespeeld: exec 182, drl 161, gelijk aan de pins.
Geen golden wisselde van kanaal en geen weigerreden veranderde. De scan
bevestigt dat. Van 748 `ir.json` (corpus en `toka/`) dragen er 12 een
venster; geen enkele heeft er een in een aggregatiefilter. Alleen
`corpus/negatief/wf112-venster-in-geen-vouw` heeft een venster in een vouw.
Een venster onder een GEEN- of tellende samenstelling staat alleen in drie
negatieven (`wf112-periodevenster-tellende-kwantor`,
`wf112-venster-in-alle-binnen-geen`, `wf034-wees-attribuutdimensies`).
Gate 12 en 13 lopen niet over negatieven, en de Drools-poot van gate 8 raakt
er geen. **Pinsom vooraf voorspeld: 0 bij `EXPECTED_PLAYED` en 0 bij
`EXPECTED_DRL_PLAYED`. Zo gemeten. Geen voorstel voor `scripts/check-drools.sh`.**

### (d) Gates, elk los na `eval $(opam env)`, op de eindstand 57e9fe4f

1 self-contained PASS · 2 ir-schema PASS (278 bestanden, vloer 192) ·
3 verifier PASS (278 golden schoon, 440 negatief) · 4 case-schema PASS (638) ·
5 oracle PASS (806 voorbeelden, 595 cases) · 6 clingo PASS (523 checks, 595
cases) · 7 noop PASS (559 + 36 overlevers) · 8 rood PASS (43 rood, 86
fragmenten; Drools-poot 9 fault-pinned, 34 niet gedragen) · 9 catala PASS (201
checks, 12 goldens) · 10 regelspraak PASS (409 tests, 70 goldens) ·
11 toka-parity PASS (29/29/29) · 12 drools PASS (played 182 = pin, DRL 161 =
pin, kanaalvloeren 53 en 74, JUnit 634, DRL-sweep 195 s) · 13 reftype PASS
(131 verdraaiingen over 27 goldens, 128 replays, 25 DRL-gespeeld) · 14 flogic
PASS (130 checks, 2 goldens).

### May-touch-verbreding (maintaineraanwijzing 2026-09-23: bijvangst in hetzelfde mechanisme meteen meenemen)

Alles bleef binnen `backends/drools/**`. De kaarttekst ging alleen over de
vouwTOETS. Verbreed is dit:

1. het vouwFILTER: toets en filter krijgen dezelfde rekendatumbinding, en
   maintainerbesluit B wijst die asymmetrie aan deze kaart toe;
2. het die/dat-filter van een AGGREGATIE, in een waarde en als operand van
   een voorwaarde. Dat is hetzelfde `filterSource` en dezelfde registratie,
   bij het bouwen gemeten als dezelfde asymmetrie. `ref`, clingo en de
   DRL-tak telden; de exec-tak weigerde. Tabel hierboven.
3. de samenstellingsarm van WF-112 (als regelvoorwaarde, en als
   aggregatiefilter in een waarde of in een operand van een voorwaarde) noemt
   nu in beide takken de regel;
4. één nieuwe kernklasse (`transpiler/kern/Vensterpositie.java`) en één
   nieuwe getuige.

De getuige leest twee corpusnegatieven alleen-lezen, naar het voorbeeld van
`HeelperiodeDoelpogingWitnessTest` met `wf109-doelgastheer-periode`.

### Beschermd-pad-voorstellen

Geen. `scripts/check-drools.sh` is niet gewijzigd, want geen pin of kanaallijst
bewoog. Wat de spec over deze posities zegt, staat hieronder als tekst bij de
open punten. `spec/**` valt buiten mijn zone.

### Registerzinnen (Engels, voor `spec/CONSTRUCTS.md`; niet aangeraakt)

Rij 40 (Quantifier fold), achteraan toe te voegen:

> **Time window inside the fold (2026-09-23, card `drools-venster-vouwtoets`):** a ch. 3.10 window in the `toets` or the `filter` of an `ALLE` fold is carried by both Drools branches with the reference interpreter's values, and so is a window in an aggregation's die/dat filter. The exec-model branch now reads every such window through the test a rule-level window uses, with the element as the subject: an attribute bound is read on each contributor, which was measured against a same-named date on the fold's own subject that would give other verdicts. The evaluation date is bound as a pattern variable of the matrix eval, or of the accumulate source for a filter. Under `GEEN` and the counting quantifiers both branches refuse the window by name, in filter and toets and at any depth, with one shared sentence that cites WF-112 (`kern.Vensterpositie`). Before, the DRL-text branch lowered it under all five quantifiers and answered the complement under `GEEN` in a time-free session for any document that did not go through the verifier. The composite arm of WF-112, as a rule condition and inside an aggregation filter (in a value or in a condition operand), now cites the rule in both branches as well. Witness `VensterVouwWitnessTest` (14 rows, red first), which also replays the corpus negatives `wf112-venster-in-geen-vouw` and `wf112-periodevenster-tellende-kwantor` without the verifier. No golden carries these forms, so no gate-12 pin moved.

Rij 11, achter de WF-112-zin "Een venster in een ALLE-vouwfilter blijft OPEN
zonder verruiming: de drie uitvoerders die het dragen antwoorden consistent
vacuüm-waar." als volgende zin:

> Since card `drools-venster-vouwtoets` (2026-09-23) all four executors carry that filter position, the `ALLE` fold's toets beside it, and a window in an aggregation's filter; the OPEN concerns the norm, not the executors. Both Drools branches refuse the WF-112 positions in the fold, in a rule condition and in an aggregation filter themselves, by name — see row 40.

### Open punten

1. **Geen golden voor het venster in een `ALLE`-vouw of een aggregatiefilter.**
   Alle vier de uitvoerders dragen deze posities nu, maar alleen de
   JUnit-getuige pint ze. Een golden zou gate 12 laten tellen:
   `EXPECTED_PLAYED` +1 en `EXPECTED_DRL_PLAYED` +1, als beide takken hem
   spelen. `corpus/**` valt buiten deze kaart. Een handberekende opzet (S1..S6,
   W1..W8, evaluatiedatum 2024-06-15 en tijdvrij) staat in de javadoc van de
   getuige.
2. **Spec-tekst die achterloopt (beschermd, voor de hoofdsessie).**
   (a) RUNTIME-SEMANTICS ch. 3.12, rij "A time window inside the `filter` of
   an `ALLE` quantifier fold": de zin "The Drools exec-model branch's SKIP
   there is a channel asymmetry (card `20260923-drools-venster-vouwtoets`)"
   klopt niet meer. Voorstel: "Since 2026-09-23 (card
   `drools-venster-vouwtoets`) the Drools exec-model branch carries it too,
   so all four executors answer it alike (witness `VensterVouwWitnessTest`;
   no golden)."
   (b) ch. 3.10, de tabel "the window sits": twee rijen, "in the `toets` or
   `filter` of an `ALLE` quantifier fold" en "in an aggregation's die/dat
   filter", elk "executed by all four since 2026-09-23 (the Drools exec-model
   branch since card `drools-venster-vouwtoets`); no golden, witness
   `VensterVouwWitnessTest`". In de GEEN-rij na "their refusals remain as
   fail-fast checks": "— both Drools branches included since the same card,
   by name".
3. **Aanhalingen in andere zones.** `ref/lib/eval.ml` (r. 208) en
   `backends/clingo/lib/translate.ml` (r. 519) citeren de oude DRL-zin
   "periodevergelijking onder een tellende kwantor" als meting van
   2026-09-20. Als meting blijft dat waar, maar de tak zegt nu "periodevenster
   in een GEEN-samenstelling (WF-112): …". Dat zijn zones van parallelle
   kaarten; ik heb ze niet aangeraakt.
4. **De NOTE van `corpus/negatief/wf112-venster-in-geen-vouw`** zegt dat de
   DRL-tak dit document beantwoordde. Dat is als geschiedenis waar. Een zin
   "since card drools-venster-vouwtoets both Drools branches refuse it by
   name" hoort bij de negatief-zone van de parallelle verifier-normkaart;
   niet aangeraakt.
5. **Niet gemeten: de samenstellingsarm van WF-112 op de overige posities**,
   zoals een consistentie-assertie en de doelpoging van een feitcreatie. Of
   beide takken daar weigeren, en met welke reden, is niet vastgesteld.
6. **Niet gemeten: `Tijdrekening.periodeGrensLowerbaar`** (frontend-kern)
   vergelijkt het objecttype van een attribuutgrens op identiteit, waar WF-111
   `S ⊑ O` toelaat. Voor een bijdrager van een subtype werkt de lezing, want
   de getter is geërfd. Een grens die op een supertype van het elementtype is
   gedeclareerd, heb ik niet gemeten. Dat is de frontendzone.
