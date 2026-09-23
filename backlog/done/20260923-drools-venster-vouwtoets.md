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

## Result (2026-09-23, branch card/drools-venster-vouwtoets; eerste oplevering 08469603, fixronde 1 bea46cf1, fixronde 2 6f885ced)

Beide flanken zijn dicht. De exec-tak speelt het venster in toets en filter
van een `ALLE`-vouw met de waarden van `ref`; dat geldt ook voor het venster
in het die/dat-filter van een aggregatie en, sinds fixronde 1, voor het
venster als `ALLE`-lid van een consistentie-assertie (daar speelt nu ook de
DRL-tak). Beide Drools-takken weigeren elke gemeten WF-112-positie zelf, bij
naam, met één gedeelde zin uit `kern.Vensterpositie`: in de vouw, als
regelvoorwaarde, in een aggregatiefilter, in een consistentie-assertie en in
een creatieguard, ook genest in een `ALLE`- of `DE`-samenstelling. Golden
`corpus/golden/jutterswet-wachtvenster` pint de drie vouw- en aggregatieposities;
gate 12 gaat van 182 naar 183 en van 161 naar 162 (beschermd-pad-voorstel).
Bijvangst, ook gedicht: de loader weigert een kenmerktoekenning met `onwaar`
(WF-022), en beide takken weigeren een doelgastheer binnen een samenstelling
of achter een hop (WF-109), waar de DRL-tak de samengestelde vorm speelde. Alle veertien gates
zijn groen.

### Eerste oplevering (tip 08469603)

#### (a) Reproductie, vijf uitvoerders per flank

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

#### (b) Keuze: bedraden in de exec-tak, weigeren in beide takken

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

#### Rood-eerst (letterlijk)

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

#### Mutatieproeven

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

#### (c) Corpussweep beide takken, vóór en na

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

#### (d) Gates, elk los na `eval $(opam env)`, op de eindstand 57e9fe4f

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

### Fixronde 1 (hoofdsessie, 2026-09-23)

Vijf punten van de hoofdsessie en één extra item van de rood-kaart, elk een
eigen commit.

#### (1) Golden `jutterswet-wachtvenster` en de pins (c86c57b5, ac1e71e4)

Handgeschreven IR, gebouwd naar het model van `jutterswet-schouwploeg`:
`paraat` (venster in de toets van een `ALLE`-vouw), `bevoegd` (venster in het
filter, toets `gediplomeerd`), `dienstwachttal` (venster in het filter van een
`AANTAL`) en `wachttal` als controle. De grens is de eigen `aanstelling` van de
wacht; elke ploeg heeft een gelijknamige eigen datum aan de andere kant van de
evaluatiedatum. Geen `bron.rgs`: de frontend emitteert geen van de drie
posities, gemeten met `make ir` (venster als gekwantificeerd predicaat:
`syntaxfout: mismatched input 'het' expecting {'..', BULLET, '-'}`; venster in
de vouwsubselectie: geweigerd, "de 'die/dat'-deelselectie … wordt niet
omgezet en zou stil wegvallen"; venster in de subselectie van een telling:
`Onopgelost`). De NOTE draagt de handberekening, vóór de eerste run gemaakt, en
een tabel van zes verkeerde lezingen met de cel waar elk afwijkt.

- ref en clingo: `PASS (17 expectations)` in alle drie de cases bij de eerste
  run; verifier `PASS`; gate-2-roundtrip byte-identiek; nietsdoener
  weerlegd in elke case (`MISMATCH: S1 lacks characteristic "paraat"` resp.
  `"bevoegd"`).
- Rood-eerst op de jar van main 8dd05e25:

      exec: EXCLUDE transpiler SKIP: regel: paraat bij aanstelling — kwantorvouw ALLE over Strandwacht (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een conditie van soort Periode): alle wachten van de Schouwploeg vanaf hun aanstelling in dienst zijn; regel (voorwaarde buiten Phase-B scope): paraat bij aanstelling
      drl:  PLAYED 3

  Na: beide takken `PLAYED 3`.
- Pinsom vooraf voorspeld 182 → 183 en 161 → 162; gemeten: gate 12 `played 183`
  en `DRL-text branch played 162`. Geen bestaand golden bewoog (sweep hieronder).
- Mutatieproef op golden-niveau, exec-jar met de mutant, teruggezet met `cmp`:
  toetsvenster altijd waar → `FAIL case1-medio-2024.json: kenmerk paraat:
  verwacht afwezig, werkelijk aanwezig; …`; filtervenster laat iedereen door →
  `FAIL case1-medio-2024.json: kenmerk bevoegd: verwacht aanwezig, werkelijk
  afwezig; attribuut dienstwachttal: verwacht 1, werkelijk 2; …`.

#### (2) RUNTIME-SEMANTICS, voorstel (bea46cf1)

ch. 3.10, tabel "the window sits": een rij voor toets en filter van een
`ALLE`-vouw en het aggregatiefilter (met de golden), het `ALLE`-lid van een
consistentie-assertie erbij (JUnit-getuige, geen golden), en in de GEEN-rij de
weigeringen van beide Drools-takken bij naam. ch. 3.12: de rij over het
vouwfilter zegt niet langer dat de exec-SKIP een asymmetrie is, maar dat alle
vier gelijk antwoorden (de golden pint de vacuüm-lezing in `case3-tijdvrij`);
de WF-112-rij noemt de weigeringen van beide takken bij naam.

#### (3) De NOTE van het negatief (df9588ff)

`corpus/negatief/wf112-venster-in-geen-vouw/NOTE.md`: een alinea dat beide
Drools-takken het document sinds deze kaart zelf weigeren, met de case waarop
dat gemeten is. Het document zelf is ongewijzigd.

#### (4) WF-112 in een consistentie-assertie en in de doelpoging van een feitcreatie (b16429dd)

Gemeten op documenten uit `jutterswet-premievenster` (assertie) en
`jutterswet-bakensein` (guard; anker `Kustvak`, doel `Sein` ⊑ `Baken`):

| vorm | verifier | ref | clingo | exec voor | drl voor | exec na | drl na |
|---|---|---|---|---|---|---|---|
| assertie `ALLE[geborgen, venster]` | PASS | PASS | PASS | EXCL "schending buiten Phase-D scope" | EXCL "onopgelost consistentieregel" | zie noot | zie noot |
| assertie venster vanaf eigen `meldatum`; `ALLE[meldatum gevuld, venster]` | PASS | PASS | PASS | EXCL (idem) | EXCL (idem) | PLAYED | PLAYED |
| assertie `GEEN[…, venster]`, `TEN_MINSTE 1[…]` | WF-112 | fout | fout | EXCL, algemeen | EXCL, algemeen | EXCL WF-112 | EXCL WF-112 |
| guard `GEEN[gehesen, venster]` | WF-112 | fout | fout | EXCL, algemeen | EXCL "voorwaarde niet naar anker of doel …" | EXCL WF-112 | EXCL WF-112 |
| guard `ALLE[gehesen (anker), venster]` (controle) | PASS | 2 / 0 | 2 / 0 | PLAYED | PLAYED | PLAYED | PLAYED |
| guard `ALLE[gehesen host Baken, idem]` | WF-109 | fout | fout | EXCL, algemeen | **PLAYED 1** | EXCL WF-109 | EXCL WF-109 |
| negatieven `wf109-doelgastheer-samengesteld`, `-genavigeerd` | WF-109 | fout | fout | EXCL, algemeen | EXCL, algemeen | EXCL WF-109 | EXCL WF-109 |

Noot: een kenmerk als criterium in een assertie draagt de DRL-tak helemaal
niet, ook zonder venster (gate 12 pint dat al voor
`anchor/jutterswet-penninguitgifte`); de getuige gebruikt daarom "de meldatum
is gevuld" naast het venster. Niet gewijzigd.

Gedicht: het venster als (lid van een `ALLE`-)assertie in beide takken, via de
gedeelde vensterlezing en als schending de ontkenning; WF-112 bij naam in de
assertie (beide takken) en in de guard (DRL); het WF-109-hek voor een
doelgastheer binnen een samenstelling of achter een hop in beide takken,
één zin in `kern.Gastheer` (`DOELPOGING_GENEST`). Getuigen
`VensterAssertieWitnessTest` (3) en `CreatieguardPositieWitnessTest` (4),
handverwachtingen gelijk in ref en clingo. Rood-eerst tegen de bronnen van
vóór de commit: `Tests run: 7, Failures: 4, Errors: 2` (alleen de controlerij
groen). Mutanten, elk teruggezet met `cmp`: A1 exec-vensterroute uit (2
errors), A2 DRL-vensterarm uit (2 errors), A3 exec-assertienaam uit (1), A4
DRL-assertienaam uit (1), A5 exec-schending zonder negatie (2, op waarde:
`verwacht consistent, werkelijk inconsistent`), G1 DRL-guardnaam uit (1), G2
WF-109-hek uit in beide (2), G3 alleen in DRL uit (2, waarvan één `Expected
…Excluded to be thrown, but nothing was thrown` — de DRL-tak speelt weer).

#### WF-022: een kenmerktoekenning met `onwaar` (330b219a)

Op het negatief `wf022-kenmerk-onwaar` van de rood-kaart (inline kopie; het
staat nog niet op deze basis), zonder verifier, met R1 `nieuw recht
ontstaan` en R2 zonder:

    verifier: WF-022 FAIL /resolvedRegels/0/waarde: characteristic target's value must be the boolean constant true
    ref:      oracle: PASS (3 expectations)
    clingo:   TRANSLATION FAULT: characteristic attribution with a non-waar value
    voor exec: FAIL case1.json: kenmerk herleefd: verwacht afwezig, werkelijk aanwezig
    voor drl:  PLAYED 1
    na exec en drl: EXCLUDE loader: form not carried by the v0 Drools backend: ResolvedRegel.waarde (de kenmerktoekenning 'geen herleving' kent het kenmerk 'herleefd' 'onwaar' toe; WF-022 eist de booleaanse constante waar, en een negatief besluit over een kenmerk bestaat alleen in een defaultsgroep)

De weigering zit in de loader, die beide takken delen. Getuige
`KenmerkwaardeWitnessTest` (2 rijen, de tweede speelt dezelfde regel met
`waar` in beide takken). Rood-eerst: `Tests run: 2, Failures: 1 …
exec-model hoort de waarde onwaar te weigeren, niet te spelen: Result[status=FAIL,
… caseFailures=[casus.json: kenmerk herleefd: verwacht afwezig, werkelijk
aanwezig] …]`. Mutant W1 (weigering uit): dezelfde failure; teruggezet, `cmp`
identiek. Scan: 748 documenten in corpus en `toka/`, 233
kenmerktoekenningen, alle 233 `Literal waar/Booleaan`.

#### (5) Open punten 3 en 6

Blijven open, zie de lijst onderaan (de hoofdsessie geeft ze door aan de
ref-, clingo- en frontendkaarten).

#### Gates op de tip van fixronde 1 (bea46cf1), elk los

1 self-contained PASS · 2 ir-schema PASS (279 bestanden) · 3 verifier PASS (279
golden schoon, 440 negatief) · 4 case-schema PASS (641) · 5 oracle PASS (806
voorbeelden, 598 cases) · 6 clingo PASS (523 checks, 598 cases) · 7 noop PASS
(562 + 36) · 8 rood PASS (43, Drools-poot 9 + 34) · 9 catala PASS (201, 12) ·
10 regelspraak PASS (409, 70) · 11 toka-parity PASS (29/29/29) · 12 drools
PASS (played 183 = pin, DRL 162 = pin, JUnit 643) · 13 reftype PASS (131
verdraaiingen, 128 replays, 25 DRL-gespeeld) · 14 flogic PASS (130, 2).

### Fixronde 2 (lens: MERGEN, met adviezen; elk een eigen commit)

#### Advies 1 — WF-112 bij naam op geneste posities in de DRL-tak (748d3126)

Gemeten op de vijf lensdocumenten (uit `jutterswet-schouwploeg`, regel
*bevoegd bij diploma*, tijdvrij):

| document | verifier | ref / clingo | main exec | main drl | na fixronde 1 exec | na fixronde 1 drl | nu, beide takken |
|---|---|---|---|---|---|---|---|
| `ALLE[ervaren, GEEN-vouw(venster)]` | WF-112 | fout / fout | EXCL "soort Periode" | **PLAYED 1** | EXCL WF-112 | EXCL "TODO voorwaarde" | EXCL WF-112 |
| `DE[ervaren, GEEN-vouw(venster)]` | WF-112 | fout / fout | EXCL "soort Periode" | **PLAYED 1** | EXCL WF-112 | EXCL "TODO voorwaarde" | EXCL WF-112 |
| `ALLE[ervaren, GEEN[venster]]` | WF-112 | fout / fout | EXCL algemeen | EXCL algemeen | EXCL WF-112 | EXCL "TODO voorwaarde" | EXCL WF-112 |
| `ALLE[niet ervaren, SOM(… die GEEN[venster]) > 3]` | WF-112 | PASS¹ / fout | EXCL algemeen | EXCL algemeen | EXCL WF-112 | EXCL "TODO voorwaarde" | EXCL WF-112 |
| `GEEN[SOM(… die GEEN[venster]) > 3]` | WF-112 | fout / fout | EXCL algemeen | EXCL "aggregatie-bullet onder tellende kwantor" | EXCL WF-112 | idem | EXCL WF-112 |

¹ ref antwoordt hier (het `niet ervaren`-lid beslist de `ALLE` voordat het
tweede lid gelezen wordt, ch. 5.10); de verifier en clingo weigeren. Buiten deze
kaart; genoteerd bij de open punten.

Oorzaak: de samenstellingsarm van de DRL-tak vroeg `Vensterpositie` alleen
bij een niet-conjunctieve kwantor, en een `ALLE`/`DE`-samenstelling zette dan
haar eigen marker over de reden van het lid. Nu vraagt de arm het bij elke
samenstelling, en `Vensterpositie` gaat ook in de operanden om daar
samenstellingen in aggregatiefilters te vinden (niet om een venster in een
operand zelf te tellen; zo doet de verifier het ook). Getuige
`VensterNestingWitnessTest`. Rood-eerst tegen de bronnen van bea46cf1:
`Tests run: 4, Failures: 4` (alle vier `drl-tekst names …: drl-transpiler
MARKER: … TODO voorwaarde: de volgende voorwaarden`).

#### Advies 2 — de gesloten flank benoemd

Een GEEN-vouw met een venster als LID van een `ALLE`- of `DE`-samenstelling
beantwoordde de DRL-tak op main met `PLAYED 1`, in een tijdvrije sessie het
complement (de eerste twee rijen hierboven). Gesloten door de vouwweigering
van de eerste oplevering, sinds advies 1 ook bij naam; getuigerijen
`een_geen_vouw_in_een_alle_samenstelling_…` en `…_de_samenstelling_…`.

#### Advies 3 — registertekst rij 11 in het Nederlands

Zie de registerzinnen onderaan.

#### Advies 4 — de weigerzin noemt de positie (dd6907f2)

De zin noemt nu welke positie ("de toets", "het filter", "het filter en de
toets"), de kwantor en het elementtype van de vouw die misvormd is. Voor het
filter luidt de reden "het venster kiest de leden waarover de kwantor telt of
ontkent" in plaats van de toetsreden. De weigering staat niet meer in het
kader van de vouw die haar vond: een GEEN-vouw over `Brevet` in de toets van
een ALLE-vouw over `Strandwacht` heet nu "… onder GEEN over Brevet (WF-112)"
zonder "ALLE over Strandwacht" eromheen. Het DRL-kader zegt "§8.2
kwantorvouw" alleen nog bij een heelperiodematrix. Mijn eigen getuigen van
deze kaart volgen de nieuwe zin (`VensterVouwWitnessTest`: twee fragmenten;
geen bestaande test van vóór de kaart geraakt). Rood-eerst tegen de bronnen
van 748d3126: `Tests run: 20, Failures: 6`.

#### Advies 5 (lensadvies 6) — bestaande asymmetrieën, gemeten

Documenten uit `jutterswet-schouwploeg` met een `Brevet` dat de wacht draagt
(to-many), gedateerd 2024-06-15; per vorm een controle zonder venster:

| vorm | verifier | ref / clingo | exec | drl | controle zonder venster | oordeel |
|---|---|---|---|---|---|---|
| geneste vouw, venster in de binnenste toets | PASS | PASS / PASS | EXCL "conditie van soort Kwantorvouw" | EXCL matrixvorm (Kwantorvouw) | idem geweigerd | structureel: geneste vouwen niet gedragen |
| `AANTAL` met vensterfilter in een vouwtoets | PASS | PASS / PASS | EXCL "vergelijking van een AANTAL-functie …" | EXCL matrixvorm (Vergelijking) | ook met kenmerkfilter en zonder filter geweigerd | structureel: aggregatie in een vouwtoets niet gedragen |
| GEEN-vouw over zo'n `AANTAL` | PASS | PASS / PASS | idem | idem | idem | structureel, zelfde grond |
| assertie met een vouw (venster als toets) | PASS | PASS / PASS | EXCL "schending buiten Phase-D scope" | EXCL "onopgelost consistentieregel" | vouw met kenmerktoets ook geweigerd | structureel: vouw in een assertie niet gedragen |
| assertie met `AANTAL` met vensterfilter | PASS | PASS / PASS | EXCL "AANTAL-assertie buiten Phase-D scope" | EXCL "onopgelost consistentieregel" | met kenmerkfilter en zonder filter ook geweigerd | structureel: telling over een meervoudige rol in een assertie niet gedragen |
| assertie met een WF-112-vouw | WF-112 | fout / fout | EXCL WF-112 | EXCL WF-112 | — | gedicht in fixronde 1 |

Geen van de vijf asymmetrieën hangt aan het venster: de controle zonder
venster wordt telkens even hard geweigerd. Benoemd, niet gedicht.

#### Advies 6 (lensadvies 8) — de WF-111-zin in een vouw (6f885ced)

Een venster dat een tak in toets of filter van een vouw weigert, weigert hij
nu om de grens: beide takken zeggen "een periodevenster met een grens die geen
datumliteral en geen eigen scalair datumattribuut van het element is
(WF-111)" (`Vensterpositie.GRENSVORM`, getest met een datumparameter als
grens). Voorheen DRL "een matrixvorm die deze tak niet IN het elementpatroon
kan schrijven (Periode)" en exec bij het filter alleen "een die/dat-subselectie
die deze tak niet op de source kan binden". Rood-eerst tegen de bronnen van
dd6907f2: `Tests run: 6, Failures: 1` (de exec-filterregel).

#### Advies 7 (lensadvies 5) — open normpunt

Zie open punt 7.

#### Mutaties fixronde 2 (op de eindstand, elk teruggezet met `cmp`)

N1 DRL-samenstellingsarm alleen bij niet-ALLE/DE → 4 failures; N2
`Vensterpositie` niet in operanden → 1; N3 DRL-vouwweigering weer in het kader
van de buitenste vouw → 1 ("kadert de weigering niet als de buitenste vouw");
N4 filterreden gelijk aan de toetsreden → 1; N5 DRL-grensweigering uit → 1; N6
DRL-kader altijd §8.2 → 1; N7 exec-grensweigering uit → 1.

#### Corpussweep en gates na fixronde 2

Sweep over alle goldens, beide takken, jar van de eindstand tegen die van main
8dd05e25: de 470 bestaande statusregels byte-identiek; de nieuwe golden
`PLAYED 3` in beide takken. Backendsuite 649 tests, 0 failures, 0 skipped.
Gates los op 6f885ced: 1 PASS · 8 rood PASS (43, Drools-poot 9 + 34) · 11
toka-parity PASS (29/29/29) · 12 drools PASS (played 183 = pin, DRL 162 = pin,
JUnit 649) · 13 reftype PASS (131, 128, 25). De overige gates liepen groen op
bea46cf1; fixronde 2 raakte alleen `backends/drools/**` en deze kaart.

### May-touch-verbreding

Binnen `backends/drools/**` (maintaineraanwijzing: bijvangst in dezelfde zone
meteen meenemen): het vouwfilter, het aggregatiefilter, het venster in een
consistentie-assertie, de WF-112-naam op elke positie en in elke nesting, de
WF-111-zin in een vouw, het WF-109-doelhek (DRL speelde het), de WF-022-weigering
in de loader; nieuwe kernklasse `transpiler/kern/Vensterpositie.java` en vijf
nieuwe getuigen (`VensterVouwWitnessTest`, `VensterAssertieWitnessTest`,
`CreatieguardPositieWitnessTest`, `VensterNestingWitnessTest`,
`KenmerkwaardeWitnessTest`). Buiten `backends/drools/**`, onder het mandaat van
fixronde 1: `corpus/golden/jutterswet-wachtvenster/**`, de NOTE van
`corpus/negatief/wf112-venster-in-geen-vouw`, en als voorstellen
`scripts/check-drools.sh` en `spec/RUNTIME-SEMANTICS.md`. De getuigen lezen
corpusdocumenten alleen-lezen.

### Beschermd-pad-voorstellen

- `scripts/check-drools.sh` (ac1e71e4): `EXPECTED_PLAYED` 182 → 183 en
  `EXPECTED_DRL_PLAYED` 161 → 162, elk met redenblok; verder niets.
- `spec/RUNTIME-SEMANTICS.md` (bea46cf1): ch. 3.10, tabel "the window sits";
  ch. 3.12, de rijen over WF-112 en over het vouwfilter.

### Registerzinnen (voor `spec/CONSTRUCTS.md`; niet aangeraakt)

Rij 40 (Quantifier fold), achteraan, Engels zoals de rij:

> **Time window inside the fold (2026-09-23, card `drools-venster-vouwtoets`):** a ch. 3.10 window in the `toets` or the `filter` of an `ALLE` fold is carried by both Drools branches with the reference interpreter's values, and so is a window in an aggregation's die/dat filter. The exec-model branch reads every such window through the test a rule-level window uses, with the element as the subject: an attribute bound is read on each contributor, measured against a same-named date on the fold's own subject that would give other verdicts. Golden `corpus/golden/jutterswet-wachtvenster` pins the three positions in all four executors (gate 12: 182 → 183, 161 → 162). Under `GEEN` and the counting quantifiers both branches refuse the window by name, at every position and nesting WF-112 names, with one shared sentence (`kern.Vensterpositie`) that names the position, the quantifier and the element type of the fold that is ill-formed; a bound WF-111 refuses is named as such. Before, the DRL-text branch lowered the window under all five quantifiers and answered the complement under `GEEN` in a time-free session, also for a `GEEN` fold nested in an `ALLE` or `DE` composite. Witnesses `VensterVouwWitnessTest` and `VensterNestingWitnessTest`. Measured and left as named asymmetries: a nested fold, an aggregation in a fold's toets, and a fold or a to-many count in a consistency assertion are refused by both Drools branches with or without a window.

Rij 11, achter de WF-112-zin "Een venster in een ALLE-vouwfilter blijft OPEN
zonder verruiming: de drie uitvoerders die het dragen antwoorden consistent
vacuüm-waar." als volgende zin, Nederlands zoals de recente landingen in die
rij:

> Sinds kaart `drools-venster-vouwtoets` (2026-09-23) dragen alle vier de uitvoerders die filterpositie, de toets van een ALLE-vouw ernaast, een venster in het filter van een aggregatie en een venster als ALLE-lid van een consistentie-assertie; golden `jutterswet-wachtvenster` pint de vouw- en aggregatieposities, en het OPEN geldt de norm, niet de uitvoerders. Beide Drools-takken weigeren elke WF-112-positie zelf, bij naam, ook genest in een ALLE- of DE-samenstelling — zie rij 40.

Rij 2 (Assignment rules), achteraan, Engels:

> **Characteristic value (2026-09-23, card `drools-venster-vouwtoets`):** the Drools loader refuses a `KENMERKTOEKENNING` whose value is not the boolean constant `waar` (WF-022), by name, for both branches. Measured on the negatief `wf022-kenmerk-onwaar` with the verifier bypassed: the exec-model branch wrote the characteristic present, the DRL-text branch left it absent, the ASP backend refused. All 233 characteristic attributions in the corpus and the parity zone write `waar`. Witness `KenmerkwaardeWitnessTest`.

Rij 22 (Fact creation), achteraan, Engels:

> **Nested doel host (2026-09-23, card `drools-venster-vouwtoets`):** both Drools branches refuse the doel attempt of a creation guard that names a host the doel satisfies inside a composite or on a navigating check — WF-109's first two doel fences — by name, one sentence in `kern.Gastheer`. The DRL-text branch played such a composite on the doel pattern where the verifier, `ref`, clingo and the exec-model branch refuse it. Witness `CreatieguardPositieWitnessTest`.

### Open punten

1. **Aanhalingen in andere zones** (was 3). `ref/lib/eval.ml` (r. 208) en
   `backends/clingo/lib/translate.ml` (r. 519) citeren de oude DRL-zin
   "periodevergelijking onder een tellende kwantor" als meting van
   2026-09-20. Als meting blijft dat waar; de takken zeggen nu
   "periodevenster in … (WF-112): …". Voor de lopende ref- en clingokaarten.
2. **`Tijdrekening.periodeGrensLowerbaar`** (was 6; frontend-kern) vergelijkt
   het objecttype van een attribuutgrens op identiteit, waar WF-111 `S ⊑ O`
   toelaat. Voor een bijdrager van een subtype werkt de lezing (de getter is
   geërfd); een grens die op een supertype van het elementtype is
   gedeclareerd, is niet gemeten. Voor de frontendkaart.
3. **De genoemde asymmetrieën van advies 5**: geneste vouwen, een aggregatie
   in een vouwtoets, een vouw of een telling over een meervoudige rol in een
   assertie. Structureel, niet aan het venster gebonden; elk een eigen
   uitbreiding van de Drools-takken.
4. **Een kenmerk als assertiecriterium in de DRL-tak**: niet gedragen, ook
   zonder venster (gate 12 pint het al voor `anchor/jutterswet-penninguitgifte`).
5. **`ALLE[niet ervaren, SOM(… die GEEN[venster]) > 3]`**: verifier WF-112 en
   clingo weigeren, `ref` antwoordt (kortsluiting vóór het tweede lid). Een
   `ref`-vraag over fail-fast bij een WF-112-positie in een niet gelezen lid;
   voor de ref-kaart.
6. **WF-112's samenstellingsarm op de overige posities** (verdeling, objectcreatie,
   uniciteit): niet gemeten.
7. **Normpunt, het expressieniveau-broertje van maintainerbesluit B.**
   `GEEN[SOM(dienstjaren | venster) > 3]`, een venster als het filter van een
   aggregatie die onder GEEN staat, is verifier-schoon: noch de WF-112-wandeling
   van de verifier noch `Vensterpositie` gaat expressies in om daar een venster
   te tellen. Alle vier de uitvoerders antwoorden gelijk (gedateerd
   {P1, P4, P5, P6}, tijdvrij iedereen bevoegd, gemeten door de lens); de
   exec-tak speelt dit sinds deze kaart. Of het venster in een operand onder
   een tellende kwantor valt, is een normvraag voor de spec-lijn.
