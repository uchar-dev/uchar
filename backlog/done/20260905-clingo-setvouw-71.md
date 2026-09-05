# De §7.1 set-vouw vertalen in backends/clingo — en de caseloze golden zijn case geven

Goal: `backends/clingo` weigert de §7.1 per-element-vouw sinds
2026-09-04 luid (`backlog/done/20260904-clingo-setfold-armen.md`). Die
weigering is eerlijk maar tijdelijk: ze staat tussen
`corpus/golden/jutterswet-tempototaal` en het case-bestand dat die
golden hoort te dragen. De NOTE van die golden draagt de handberekende
absolute waarden al — `J1.bewaarlast = 19`, `J1.bewaarsom = 19`,
`J2.bewaarlast = 0` (§4.20: een lege totaal is onvoorwaardelijk 0),
`J2.bewaarsom = leeg` (§4.10: een lege som zonder terugvalwaarde) plus
`consistent` — en zegt er in zoveel woorden bij dat de case op de
executor wacht. Deze kaart is die executor.

**Twee dingen moeten samen landen, anders blijft de golden caseloos.**
De NOTE meet het zelf: clingo weigert *beide* routes van dat document,
elk om een eigen reden.

1. de set-vouw-`Totaal` zelf — vandaag de luide §7.1-weigering;
2. de **niet-identieke schrijf-eenheid** — `TRANSLATION FAULT:
   non-identical units under the write to "bewaargeld" are not
   translated (ref restates with an exact conversion factor this
   integer encoding does not carry)`. Juist bij deze vouw is de
   schrijf-eenheid per constructie níét identiek (`€/mnd` in, `€` uit,
   de §4.20-noemerstrip van de tijdaggregatie).

Eén van de twee alleen laat de golden caseloos; poort 5 telt hem dan
nog steeds bij de "geen case-bestand"-lijst.

**Overneembare metingen** (deze kaart hoeft ze niet over te doen,
alleen te bevestigen dat ze nog staan — geteld over alle 486 `ir.json`
in `corpus/` en `toka/`):

- 79 enkelvoudige `AttribuutRef`-vouwoperanden in totaal (74 op de
  merge-base van de vouwarmen-kaart; de vierde WF-034-golf voegde tien
  negatief-documenten toe waarvan vijf er een dragen);
- 2 niet-temporele set-`TOTAAL` (beide `set = true`, beide zonder
  case-bestand — de twee die deze kaart moet gaan dragen);
- 14 temporele `TOTAAL`, waarvan 11 met filter (de `gedurende`-clip);
  alle 14 vallen vandaag binnen de vernauwde eigen-subject-arm.

**Twee dingen die vóór het pinnen verantwoord moeten zijn.**

*Ten eerste: ref antwoordt 0 op de §7.1-vorm in een minimaal frame.*
Gemeten (Vondst-subject, set-vouw over `bewaartarief` van de leden,
`elementObjecttype` gezet, geen `navigatieRol`):

```
attribuut temporeel     : verifier PASS | ref V1.bewaarlast = 0
                          | clingo luide §7.1-weigering
attribuut niet-temporeel: verifier PASS | ref V1.bewaarlast = 0
                          | clingo catch-all (redenloos)
```

Waar de per-element-tijdvouw 10 zou geven zegt ref 0. Een 0 is geen
leeg en geen weigering; vóór deze kaart een waarde pint moet vaststaan
of dat ref's bedoelde antwoord is (§4.20's onvoorwaardelijke 0 op een
lege bron, hier over een niet-lege bron) of ref's eigen gat. De tweede
regel is bovendien ref ≢ clingo onder een verifier-clean document — de
getuige die `backlog/new/20260905-wf083-vouwbron-anker.md` als derde
opvoert.

*Ten tweede: het temporaliteitsanker.* De §7.1-weigerarm van de
`Totaal` hangt zijn `attr_temporal`-guard aan het operand-objecttype
`aot`; de `Som`- en `Minimum`/`Maximum`-guards hangen hem aan `el`, het
`elementObjecttype`. Op elke frontend-vorm vallen die samen; op een
gemengde annotatie niet. Gemeten getuige, over te nemen: een derde
objecttype dat het gevouwen attribuut níét-temporeel declareert
(`aot` daarheen gedraaid, `elementObjecttype` temporeel, `set = true`)
valt redenloos in de catch-all —
`TRANSLATION FAULT: expression kind not translated (no corpus case
exercises it)`. Welk anker het juiste is volgt uit wat de set-vouw
vertaalt, dus die keuze hoort hier; deze vorm is de getuige die hem
vastlegt.

Acceptance:
- **Rood-eerst:** het case-bestand van de golden
  `jutterswet-tempototaal` landt eerst, met de vier NOTE-waarden plus
  `consistent`, en poort 6 is er meetbaar rood op vóór de vertaalslag.
- `ref` ≡ clingo ≡ Drools spelen die case met exact dezelfde
  verwachtingen (de differentiële vierkant-eis van poorten 5, 6 en 12).
- De keuze van het temporaliteitsanker staat in het Result, met de
  gemengde-annotatievorm als gepinde getuige.
- Alle dertien gates groen; poort 5 telt één caseloze golden minder.

May touch: `backends/clingo/**`,
`corpus/golden/jutterswet-tempototaal/**`, `backlog/`.

Out of scope: `ref/` en `backends/drools` aanpassen — die dragen de
vormen van deze golden al (de NOTE meet `ref: oracle: PASS` op het hele
document), dit is clingo's inhaalslag; de §4.20-noemerstrip (besloten 2026-09-04, de
NOTE is de vastlegging); de `EERSTE`/`LAATSTE`-set-vorm (vier
voorkomens, alle in `corpus/negatief` — een eigen kaartje als er ooit
een executorpad voor komt); en de **genavigeerde `Som`/`Minimum`/
`Maximum`-catch-all**, die blijft zoals hij is. Die weigering is luid
maar redenloos (`expression kind not translated (no corpus case
exercises it)`) en alleen via wire-chirurgie bereikbaar — geen frontend
emitteert de vorm. Wel gemeten en het noemen waard: de verifier vangt
hem maar half. Een geschreven rol die de *operand*-type zelfbenoemt
(`navigatieRol "bewarende jutter"` op `objecttype "Jutter"`) is
verifier-PASS; een die dat niet doet (`"vondst"` op `"Jutter"`) is
WF-083 FAIL. Dat is het anker dat
`backlog/new/20260905-wf083-vouwbron-anker.md` verplaatst, niet deze
kaart.

---

## Result (2026-09-05)

**Geland.** `corpus/golden/jutterswet-tempototaal` heeft zijn case; ref,
clingo en de Drools-backend spelen hem op dezelfde zeven verwachtingen.
De diff raakt `backends/clingo/lib/translate.ml`,
`backends/clingo/test/test_clingo.ml`, de NOTE en de case van die ene
golden, en deze kaart. Verder niets.

*Regelnummerconventie: `~:`-verwijzingen wijzen naar
`backends/clingo/lib/translate.ml` op de merge-base `b019f42`.*

### De handberekening, naast ref en clingo

`J1` bewaart `V1` (bewaartarief 12), `V2` (7), `V3` (geen);
`J2` bewaart niets. `J1` draagt `bewaarvergoeding per maand` = 10 €/mnd;
asOfDate 2026-03-15, dus de MAAND-bucket is [2026-03-01, 2026-04-01) en
telt precies 1 hele maand.

| verwachting | met de hand | ref | clingo | Drools |
|---|---|---|---|---|
| `J1.bewaarlast` | 12 + 7 = **19** (§4.20 set-vouw) | 19 | 19 | 19 |
| `J1.bewaarsom`  | 12 + 7 = **19** (§4.10) | 19 | 19 | 19 |
| `J1.bewaargeld` | 10 €/mnd × 1 mnd = **10 €** (Tabel 30 rij 2) | 10 | 10 | 10 |
| `J2.bewaarlast` | lege totaal = **0**, onvoorwaardelijk | 0 | 0 | 0 |
| `J2.bewaarsom`  | lege som zonder terugval = **leeg** | leeg | leeg | leeg |
| `J2.bewaargeld` | afwezige tijdlijn totaliseert **leeg** (ch. 3.6) | leeg | leeg | leeg |
| `consistent`    | geen consistentieregels | ✓ | ✓ | ✓ |

De eerste vier plus `consistent` staan letterlijk in NOTE.md. De twee
`bewaargeld`-pins zijn de temporele route die diezelfde NOTE in proza
meet ("`bewaargeld` is `10 €` over a one-month asOfDate bucket"); ze
zijn hierboven met de hand nagerekend en toegevoegd, niet aangepast. Dat
is één afwijking van de kaartletter ("de vier NOTE-waarden plus
consistent") en ze staat hieronder ook in de afwijkingenlijst.

### Rood-eerst

Commit `be60009` legt de case neer tegen de ongewijzigde vertaler.
Gemeten op die commit:

```
poort 5 : check-oracle: PASS (404 cases replayed exactly; NOT replayed,
          no case file yet: jutterswet-seizoenvondst jutterswet-vormenkabinet)
poort 6 : FAIL: corpus/golden/jutterswet-tempototaal/case.json does not
          replay through clingo:
          TRANSLATION FAULT: expression kind not translated (no corpus
          case exercises it)
          (exitcode 1)
```

Per route geïsoleerd, dezelfde commit:

```
bewaarlast (set-vouw TOTAAL) : ref PASS | clingo TRANSLATION FAULT:
    expression kind not translated (no corpus case exercises it)
bewaarsom  (set-vouw SOM)    : ref PASS | clingo PASS
bewaargeld (temporele TOTAAL): ref PASS | clingo TRANSLATION FAULT:
    non-identical units under the write to "bewaargeld" are not
    translated (ref restates with an exact conversion factor this
    integer encoding does not carry)
```

### Wat er landde

**Eén tak voor de twee set-vouwen** (`~:1310`, was de `Som`-arm). Ref
heeft er ook één: gedeelde contributor-vergaring, alleen de
lege-set-regel verschilt. TOTAAL op de lege set is onvoorwaardelijk 0
(§4.20) — precies clingo's `#sum` over nul tupels, dus geen
aanwezigheidsguard; SOM zonder terugval blijft ref's leeg en houdt hem.
Een `leegwaarde` op TOTAAL wordt genegeerd zoals ref hem negeert
(`agg_totaal` neemt geen terugval), dezelfde conventie als de
MIN/MAX-arm.

**De §7.1 per-element-tijdvouw**, in diezelfde tak. Per lid:
segmentwaarde × hele roostereenheden (Tabel 30), daarna over de set — in
één `#sum` over (lid × segment), want Σ_leden Σ_segmenten = Σ_paren en
geen van beide vouwen kan leeg zijn op een manier die de ander verbergt.
De kalender rijdt als data (`pe<N>_wu`-feiten uit de gedeelde
Timeline-module, de conventie van de `gedurende`-arm), dus hij kan niet
van ref afdrijven. Horizonregel woord voor woord van ref: een lid met
een asOfDate-bucketscalaar wordt bij naam geweigerd, een afwezig lid
draagt niets. Twee eigen hekken erbij, allebei luid: een AFGELEIDE
ledentijdlijn (grenzen geen casedata → de wu-feiten zijn niet te
berekenen) en de wrap-fence (Σ hele eenheden over alle ledentijdlijnen
≤ 46340 naast de bestaande |W| ≤ 46340).

**Het schrijf-eenheden-hek**, zonder één regel eenhedenconversie:
`eff_unit` levert de GESTRIPTE eenheid waar ref integreert, dus het
bestaande `units_identical_ok` ziet € tegen een gedeclareerde € en laat
door. De strip volgt de INTEGRATIE, niet de operator: de eigen-subject-
totaal én de §7.1-vouw onder som én totaal strippen; de niet-temporele
set-totaal (de som-alias) houdt zijn tarief — precies de rij die deze
golden pint. `grid_strip_ok` bewaakt de exactheid: alleen een noemer die
IDENTIEK is aan de roostereenheid valt met factor 1 weg. Gemeten met
`€/jr` over een maandrooster (declaratiefactor 12):

```
ref    : oracle: PASS  (rekent 12/12 + 7/12 + 5/12 exact)
clingo : TRANSLATION FAULT: the noemer strip of the per-element temporal
         fold of "bewaartarief" restates a EUR/jr value into a mnd grid
         by a declared factor other than 1, which this integer encoding
         does not carry (ref applies the exact factor); only a
         denominator identical to the grid unit cancels here
```

Luid, beide eenheden bij naam, nooit afgerond.

**Het hek is van clingo alleen — de Drools-tak heeft er geen.** Gemeten
op precies het striprichting-frame (operand `€/mnd`, doel `€/mnd`):
verifier WF-091 FAIL, ref `EVALUATION FAULT: incompatible units: EUR/mnd
vs EUR`, clingo de weigering hierboven, **Drools `PLAYED 1`**. Dat is
géén tekortkoming van deze arm en hoort haar ook niet aangerekend: het is
de al gekaarte, pre-existente eenheidsblindheid van de Drools-tak — de
stille factor-één-terugval die van een naamverschil een verkeerd antwoord
maakt in plaats van een fout, opgeschreven als vervolgpunt (b) in
`backlog/done/20260901-eenheden-op-refs.md`. Wat die vorm hier
tegenhoudt is de VERIFIER (WF-091), niet de executor.

**`total_factor` draagt nu ook MAAND** (`~:402`). Dit was een DERDE
blokkade die de kaart niet noemt: de temporele route van deze golden is
maandgranulair en de arm droeg alleen JAAR. Hij kwam pas boven water
nadat het eenhedenhek loskwam — dat hek vuurt eerder. De bucket van een
granulariteit ÍS één hele eenheid, en `Timeline.bucket` bestaat alleen
voor maand en jaar.

*Waar die WEEK/DAG/KWARTAAL-weigering vandaan komt — nagemeten
2026-09-05.* Niet uit deze arm. De GEDEELDE CASELEZER weigert al bij het
inladen: `Case.timeline_of_json` legt elke segmentgrens op het
granulariteitsrooster via `Timeline.bucket`, en die bestaat alleen voor
maand en jaar. Gemeten op een WEEK-gedeclareerd `bewaartarief` met een
tijdlijn in de case:

```
verifier : PASS
ref      : EVALUATION FAULT: bucketing is defined for month and year only (ch. 3.2)
clingo   : TRANSLATION FAULT: bucketing is defined for month and year only (ch. 3.2)
```

Beide executors falen dus op DEZELFDE gedeelde regel, vóór de arm. De
niet-maand/jaar-tak van `total_factor` is daarmee onbereikbare
redundantie: ze heeft geen eigen getuige en kan er ook geen krijgen zolang
de caselezer eerder vuurt. Ze blijft staan als vangnet, maar de kaart
schrijft haar geen discriminerend vermogen toe.

### Het temporaliteitsanker: `elementObjecttype`

**Gekozen: het `elementObjecttype`**, uitgeschreven in
`fold_operand_otype` (`~:283`): bij een SET-lezing de elementsoort, bij
een gewone lezing het eigen objecttype. Reden: elke lezing in de
set-armen hangt aan de ELEMENT-variabele en elke declaratie wordt op
`el` opgezocht — daar staat het attribuut, dus daar staat ook of het een
tijdlijn is. Het operand-`objecttype` is bij beide executors inert (de
vorige kaart mat dat).

*Preciezer dan de eerste formulering.* Niet elke arm ROEPT die helper.
De gedeelde set-vouw leest `attr_temporal ctx ~otype:el` RECHTSTREEKS
(`~:1415`): `el` komt uit het patroon, dus daar staat het anker
uitgeschreven in plaats van aangeroepen. `fold_operand_otype` draagt het
anker op de twee plekken die het operand-`objecttype` wél in handen
krijgen: de eenheidsopzoeking van de §4.20-strip in `eff_unit` (`~:349`)
en de temporele `Totaal`-weigerarm (hing aan `aot`). De MIN/MAX-guard
hing al aan `el`. Eén anker, drie plekken, twee manieren om het te
zeggen — en dat de strip er óók aan hangt is precies waarom het
schrijfhek de verkeerde ankerkeuze NIET vangt (zie de eenheidloze
getuige hieronder).

**De gemengde annotatie is de gepinde getuige.** Een derde objecttype
`Kist` declareert `bewaartarief` níét-temporeel; het operand-`objecttype`
wijst daarheen, `elementObjecttype` blijft de temporele `Vondst`,
`set = true`.

```
vóór : ref PASS (5 verwachtingen) | clingo TRANSLATION FAULT:
       expression kind not translated (no corpus case exercises it)
ná   : ref PASS (5)               | clingo PASS (5)
```

Dat wás dus een ref ≢ clingo onder een verifier-PASS document, precies
de derde getuige die `backlog/new/20260905-wf083-vouwbron-anker.md`
opvoert. De suite pint hem: het geëmitteerde programma moet de
`_wu`-feiten van de tijdvouw dragen (alleen de temporele route emitteert
ze — met het anker op `aot` joint hij scalaire `val/3`-feiten die een
tijdlijn-case niet heeft), en ref moet dezelfde 48 geven.

*Wat dat verkeerde anker dan antwoordt, letterlijk nagemeten
2026-09-05: een **stille 0**, geen stille leeg.* De eerste formulering
zei "leeg" en dat is onwaar: `TOTAAL` over nul tupels is
onvoorwaardelijk 0 (§4.20) en dat is precies wat clingo's `#sum` over
een lege join oplevert. De gemeten omvaller is
`J1.bewaarlast = [0], expected 48` — een WAARDE die geen enkele
aanwezigheidsguard tegenhoudt, en dus gevaarlijker dan een leeg. Sinds
de fixronde staat er ook een `clingo_matches` naast, want "vertaalt het"
zei niets over die 0.

### De drie weigerteksten

| weigering | was | is |
|---|---|---|
| `Som`-guard (`~:1320`) — "the per-element temporal fold over a set (§7.1) is not translated" | luid geweigerd | **WEG.** De vorm wordt gedragen; de guard is een tak geworden. |
| `Min`/`Max`-guard (`~:1417`) | luid geweigerd, anker `el` | **BLIJFT**, ongewijzigd. Ref weigert de vorm zelf ("no defined moment to fold"), dus dragen zou de divergentie omdraaien. Anker-commentaar wijst nu naar het besluit. Nieuw gepind in de suite. |
| `Totaal`-set-weigerarm (`~:1763`) | "the per-element temporal total over a set (§7.1) is not translated: the carried arm totals the SUBJECT's own timeline …" | **VERNAUWD** tot de GENAVIGEERDE set-vouw: de navigatievrije vorm wordt gedragen, de genavigeerde niet (ref weigert een genavigeerde vouwbron outright). De arm hangt zijn temporaliteitsguard nu aan het anker. |

De twee andere redenen van die arm (genavigeerd niet-set, vreemd
objecttype niet-set) staan ongewijzigd en blijven gepind.

### De gemeten 0 op het kale frame — verantwoord

De kaart vraagt om verantwoording van `ref V1.bewaarlast = 0` waar de
per-element-tijdvouw 10 zou geven. Nagemeten op precies dat frame
(Vondst-subject, `elementObjecttype` gezet, geen `navigatieRol`,
`set = true`, J1 draagt 10 als bucketscalaar):

```
attribuut temporeel     : verifier PASS | ref 0 | clingo 0   (was: luide §7.1)
attribuut niet-temporeel: verifier PASS | ref 0 | clingo 0   (was: catch-all)
```

**Het is geen ref-gat.** De vouwbron is leeg: het subject is een
`Vondst`, de vouw zoekt `Jutter`-leden, en de link staat in de case aan
de Jutter-kant (`roles: {vondsten: [V1]}`). `linked` loopt de eigen links
van het subject, vindt er geen, en `agg_totaal []` is onvoorwaardelijk 0
(§4.20). De horizonregel komt niet eens aan bod. De 0 is het juiste
antwoord op een lege bron, en beide executors geven hem nu.

**Dat kostte een correctie aan mijn eerste versie.** Het horizonhek liep
eerst over ELKE instantie van de elementsoort en weigerde dus waar ref
0 antwoordde. Het wandelt nu via ref's eigen `linked` over de ECHTE
leden.

**De restrand is geen GEVAL maar een KLASSE.** De eerste versie noemde
alleen het `die/dat`-filter; de klasse is ruimer: ELKE versmalling die op
vertaaltijd onbeslisbaar is. De vertaler weet niet welke leden de vouw
werkelijk bereiken, dus weigert hij zodra een lid van de elementsoort een
bucketscalaar draagt — ook als dat lid er nooit was gekomen. Twee vormen
nagemeten (2026-09-05), allebei verifier-PASS en allebei
ref-antwoordt/clingo-weigert:

```
die/dat-FILTER dat het bucketscalaar-lid uitsluit
    verifier PASS | ref oracle: PASS | clingo TRANSLATION FAULT (horizonregel)
REGELVOORWAARDE die elk subject uitsluit
    verifier PASS | ref oracle: PASS | clingo TRANSLATION FAULT (horizonregel)
```

Geen corpusgeval, en er kán er ook geen zijn: poort 6 heeft geen
weigerkanaal en poort 8 vraagt een fout in BEIDE executors, terwijl ref
gewoon antwoordt.

**Wat er nog aan schort, expliciet genoteerd:** de weigertekst noemt bij
zo'n versmalde vorm de bucketscalaar ("the case supplies an
asOfDate-bucket scalar"), en dat is niet de échte reden. De échte reden is
dat de VERTALER de versmalling niet kan beslissen; het document is niet
defect. Een tekst die dat zegt hoort erbij. Niet gebouwd in deze ronde —
de fixronde-opdracht is kaarttekst op dit punt en de kleinste wijziging
telt — maar hier vastgelegd zodat de volgende ronde hem kan schrijven in
plaats van hem te moeten herontdekken.

### WIJZIGINGEN AAN BESTAANDE SUITE-CHECKS (voor de maintainer)

Twee commits raken bestaande checks in
`backends/clingo/test/test_clingo.ml`. Ze staan apart en gaan samen:

* `72734b9` — de pin "temporal totaal, set operand (the §7.1 per-element
  fold)" hield vast dát de set-vouw luid geweigerd wordt. Die zin is niet
  meer waar.
* `5bf9211` — de eerlijke eindstand van diezelfde pin. Op dat document
  vindt de gedragen vouw geen leden en antwoordt hij 0, dus de pin is
  `translates_with` + `ref_matches` op precies die 0 geworden: de
  ref ≢ clingo die de redenloze catch-all verborg, nu vastgelegd als
  overeenstemming. `totaal_case` kreeg een optioneel `~bewaarlast`-
  argument; de vier andere aanroepen zijn ongewijzigd.

Alles daarnaast is additief (het maintainer-akkoord van 2026-09-05):
acht nieuwe checks, 24 → 33.

**De discriminatietelling, rechtgezet (fixronde 2026-09-05).** De twee
commit-boodschappen `2439aec` en `5bf9211` schrijven het aantal
omvallers aan de verkeerde knop toe: `5bf9211` zegt "met het anker
teruggedraaid vallen er 4 om", `2439aec` "met het anker teruggedraaid en
de temporele tak uitgezet vallen 5 van de 8 om (plus de omgezette pin)".
De geschiedenis blijft staan; de kaart zet de telling recht. Elke knop
apart nagemeten op de suite van `33f010d` (33 checks):

| knop | omvallers | wat er omvalt |
|---|---|---|
| temporele tak UIT | **4** | twee `_wu`-dragers plus twee "expected a loud fault, got a model" (noemerstrip, horizonregel) |
| anker alleen terug naar `aot` | **1** | alleen `translates, program carries _wu(` — een structurele pin, dus in de VEILIGE richting: hij merkt het, en er glipt geen antwoord door |
| beide knoppen | **4** | identiek aan "temporele tak uit" — het anker voegde niets toe |

Dus: de vier omvallers zijn van de TEMPORELE TAK; het anker droeg er
één, en die was structureel, niet op waarde. Precies dat gat vult de
fixronde met `clingo_matches` (R2b). Dezelfde meting op de suite ná de
fixronde (44 checks): temporele tak uit **10**, anker alleen **3**,
beide **10** — en de drie van het anker bevatten nu twee
WAARDE-omvallers (`clingo pins the values (J1.bewaarlast = [0],
expected 48)`).

### Poorten (alle dertien groen — de EERSTE ronde; de fixronde hieronder draait ze opnieuw)

```
 1 check-self-contained     PASS
 2 check-ir-schema          PASS (175 bestanden valideren en round-trippen)
 3 check-verifier           PASS (175 golden clean; 281 negatief afgewezen)
 4 check-case-schema        PASS (426 case-bestanden valideren; was 425)
 5 check-oracle             PASS (404 cases exact herspeeld; 2 golden zonder
                                 case — was 403 en 3)
 6 check-clingo             PASS (404 cases exact; eigen suite 33 checks,
                                 0 failures — was 403 en 24)
 7 check-noop               PASS (368 weerleggen; 36 gepinde overlevers)
 8 check-rood               PASS (22 rode cases; Drools-been 5 fault-pinned,
                                 17 not-carried)
 9 check-catala             PASS (201 checks; 12 catala-golden byte-identiek)
10 check-regelspraak        PASS (171 JUnit-tests; 29 rgs-golden byte-identiek)
11 check-toka-parity        PASS (ref 29/29, clingo 29/29, Drools 29 PLAYED)
12 check-drools             PASS (107 played — was 106; 1 value-divergence
                                 (anchor/jutterswet-kustwacht, ongewijzigd);
                                 JUnit 199)
13 check-reftype-derivation PASS (61 twists over 12 golden; 54 replays;
                                 0 overlevers — was 52/11/50)
```

Poort 12 speelt de nieuwe golden zonder enige nieuwe weigering of
divergentie: ref ≡ clingo ≡ Drools op de zeven gepinde waarden. Dat is
het differentiële vierkant dat deze kaart moest sluiten.

**Eerlijk over wat poort 6 hier wél en niet differentieel is.** De
vertaler DEELT code met ref: de kalender rijdt als data uit
`Uchar_ref.Timeline` (de `wu`-feiten, `whole_units`, `bucket`), de
eenheidsrekenkunde komt uit `Uchar_ref.Arith` (`convertible`,
`grid_unit`), en de ledenwandeling van het horizonhek is letterlijk
ref's eigen `Eval.linked`. Dat is met opzet zo — een eigen kalender zou
van ref kunnen afdrijven en dan meet poort 6 een verschil dat niets over
de spec zegt — maar de prijs staat er tegenover: **op die assen is poort
6 geen ONAFHANKELIJK differentieel**. Een fout in `whole_units` of in
`convertible` slaat in beide executors identiek toe en poort 6 blijft
groen.

Wat de onafhankelijkheid daar wél draagt: (1) de **Drools-tak**
(poort 12), die zijn eigen kalender en zijn eigen eenheidslogica heeft en
de golden op dezelfde waarden speelt; (2) de **handberekende ankers** —
de 48 en de 0 van de nieuwe golden staan in NOTE.md vóórdat een executor
draaide, en de goldens onder `corpus/golden/anchor/` zijn per
constructie handgeschreven. Structureel is dit dezelfde zorg als de
`ref/`-versus-`spec/verifier/`-scheiding uit CLAUDE.md: gedeelde code is
geen bewijs, aparte implementaties en met de hand geschreven waarden
zijn dat wel. Genoteerd, niet weggepoetst.

### Afwijkingen van de kaart

1. **De kaart noemt de verkeerde weigering voor `bewaarlast`.** Ze
   schrijft "de set-vouw-`Totaal` zelf — vandaag de luide
   §7.1-weigering"; gemeten was het de CATCH-ALL, want `bewaartarief` is
   in deze golden juist NIET temporeel. `bewaarlast` is de som-alias, niet
   de §7.1-vouw. Beide zijn nu gedragen, dus de acceptatie klopt; de
   diagnose in de kaartkop niet.
2. **Er was een derde blokkade**, door geen van beide genoemd:
   `total_factor` droeg alleen JAAR (zie boven).
3. **Twee pins meer dan de kaartletter.** De case pint ook
   `J1.bewaargeld = 10` en `J2.bewaargeld` leeg. Handberekend, en de NOTE
   meet ze; zonder die twee zou de temporele route ongepind meerijden.
4. **De strip geldt NIET voor de niet-temporele set-vouw.** De
   opdrachtformulering suggereerde dat "de som-alias" een gestripte
   eenheid levert; de wire en de NOTE zeggen het omgekeerde (rij 1 en 2
   dragen allebei `€/mnd`). De wire is gevolgd.
5. **Twee bestaande suite-checks gewijzigd** — zie het blok hierboven.
6. **Eén ankerrand bewust NIET van een eigen reden voorzien.**
   `elementObjecttype = null` bij `set = true`: gemeten weigert ref luid
   ("unsupported AttribuutRef shape (set/sentinel outside a fold)") en
   clingo ook (via het eenhedenhek, of anders de catch-all). Beide luid,
   geen stil antwoord, en de vorm heeft geen frontendpad — speculatieve
   code toevoegen zou de kleinste-wijziging-regel breken. Genoteerd, niet
   gebouwd.
7. **Out-of-scope gerespecteerd:** `ref/`, `backends/drools`, de
   frontends, `spec/` en de genavigeerde `Som`/`Min`/`Max`-catch-all zijn
   niet aangeraakt. De `EERSTE`/`LAATSTE`-set-vorm evenmin.

---

## Fixronde (2026-09-05)

Twee lenzen lazen de geland diff terug. Beide bevestigden de zes pins,
de discriminatiematrix en de poortentabel; beide vonden hetzelfde ENE
blokkerende punt — de wrap-hek telde verkeerd — plus een compleet, al
geprototypet recept voor wat de suite miste. Alles hieronder is
uitgevoerd en opnieuw gemeten.

**Gemandateerde may-touch-verruiming, expliciet gemeld.** De kaartletter
gaf `backends/clingo/**`, `corpus/golden/jutterswet-tempototaal/**` en
`backlog/`. De fixronde-opdracht verruimt dat met één nieuwe
golden-map — `corpus/golden/jutterswet-tempovouw/**` — om de vierde
lezing van "het totaal van" een corpusgetuige te geven. Verder is er
niets buiten de oorspronkelijke lijst aangeraakt.

### R1 — de overflow (blokkerend, eerst)

De hektelling liep over `sort_uniq`'te spans terwijl de `#sum` over
`(lid, van, tot)`-tupels sommeert: leden met een IDENTIEKE periode
telden één keer. Gemeten op een verifier-PASS-document met alledaagse
grootheden — 150 leden × 399,99 €/mnd × 360 maanden:

```
VÓÓR (verwachting op de GEWRAPTE waarde −21.350.212,96)
  ref    : MISMATCH: J1.bewaarlast = 21599460, expected -21350212.96
  clingo : clingo: PASS (2 expectations)        exit 0    <- STIL
  (ruw atoom: val("J1","bewaarlast",-2135021296); 150 × 360 × 39999 =
   2.159.946.000 en 2.159.946.000 − 2³² = −2.135.021.296.
   clingo 5.8.2 wrapt zonder een woord, ook bij het gronden.)

VÓÓR (verwachting op de ECHTE waarde 21.599.460,00)
  ref    : oracle: PASS (2 expectations)
  clingo : TRANSLATION FAULT: expected value 21599460 exceeds clingo's
           integer range        <- de VERWACHTING valt om, niet het hek

NÁ
  clingo : TRANSLATION FAULT: the per-element temporal fold of
           "bewaartarief" covers 54000 whole units across the case's
           member timelines, beyond the guarded sum range   exit 1
           (op BEIDE verwachtingen — het hek vuurt vóór elke waarde)
```

De telling houdt nu de multipliciteit; de ontdubbeling is de
FEITEMISSIE'S alleen (`List.sort_uniq` op de emissieplek — `wu`-feiten
zijn kalenderfeiten, één per onderscheiden span, een herhaalde span zou
alleen hetzelfde atoom nog eens afleiden). De normatieve eis staat als
commentaar in de code: `max|g| × Σ n ≤ 2³¹−1` over de
(lid, segment)-tupels, met beide factoren op 46340 = `isqrt(2³¹−1)` — de
vloer, één eenheid onder de echte wortel, dus bewust conservatief; die
grens is ongemoeid gelaten.

**Randgetuige, ΣN = 46340 exact** (20 leden × 2317 hele maanden): speelde
vóór de patch en speelt erna — `ref oracle: PASS`, `clingo: PASS`. Het hek
weigert BOVEN de grens, nooit erop.

**Waardegelijkheid vóór/ná, drie temporele frames**, elk vóór en ná
gemeten met dezelfde binaries: 48 (de suite-vorm), 50 (twee leden die
één IDENTIEKE span delen — precies de vorm die de ontdubbeling verborg)
en 43,50 (twee decimalen door dezelfde vouw). Alle drie `ref PASS |
clingo PASS`, vóór zowel als ná: de patch verandert geen enkele waarde,
alleen het hek.

**Geen corpus/rood-geval mogelijk, gemeten.** Poort 8 eist een
waarneembare fout in BEIDE executors; ref speelt het overflowdocument
gewoon (`oracle: PASS`). De faulttekst is daarom in de suite gepind, met
de randgetuige ernaast. **Zustercontrole:** de gedurende-clip (`~:1892`)
en `emit_bucket_calendar` (`~:4329`) ontdubbelen ENDPOINTS en begrenzen
daarna de ene endpoint-SPAN — nooit een som over ledensegmenten. Beide
tegen dezelfde eis gehouden en sound as written; dat staat er nu ook bij.

### R2a — de nieuwe golden `jutterswet-tempovouw`

De vierde lezing van "het totaal van": de set-lezing van een TEMPOREEL
attribuut. `bron.rgs` door de frontend, `bewaartarief` geheel `€/mnd`
`voor elke maand` op `Vondst`, doel `bewaarlast` `€` op `Jutter` — de
§4.20-noemerstrip volgt de INTEGRATIE, dus hier valt de noemer weg waar
`jutterswet-tempototaal` hem juist houdt. De twee goldens zijn elkaars
tegenvoorbeeld op precies die regel.

Handberekend vóór elke executor: V1 12 €/mnd × 3 hele maanden = 36;
V2 7 + 5 = 12; V3 zonder tarief valt weg; `J1.bewaarlast` = **48 €**;
`J2.bewaarlast` = **0** (lege totaal, §4.20); `consistent`.

```
verifier : PASS
ref      : oracle: PASS (3 expectations)
clingo   : clingo: PASS (3 expectations)
Drools   : PLAYED 1
frontend : her-emissie byte-identiek aan ir.json
noop     : weerlegt (twee MISMATCHes, exit 1)
```

`NOTE.md` is nieuw en volledig geschreven (Engels, self-contained, met
de handberekening en de §4.20-strip). De NOTE van
`jutterswet-tempototaal` verwijst er nu naar en draagt de
restrand-caveat bij "exactly as ref refuses it".

### R2b — de solver in de suite

`run_clingo` staat nu in `backends/clingo/lib/solve.ml`; de
gate-6-runner importeert hem, zodat poort en suite clingo op DEZELFDE
manier aanroepen (`unix` erbij in `lib/dune` en `test/dune`). Nieuwe
helper `clingo_matches`, de spiegel van `ref_matches`: vertaalfout,
ontbrekende clingo, UNSAT en waardeverschil falen elk luid bij naam.
Hij staat naast elke bestaande `ref_matches` in het §7.1-blok, plus bij
de randgetuige van R1, plus bij het nieuwe **eenheidloze** frame.

Dat eenheidloze frame (AN-unitless) is het enige dat de
anker-eigenschap pint zónder hulp van eender welk eenhedenhek — en dat
is nodig, want de §4.20-strip in `eff_unit` hangt ZELF aan het anker
(`fold_operand_otype`), dus met alleen de knop in de vertalende arm
teruggedraaid blijft het schrijfhek correct geankerd en vangt het niets.
Gemeten out-of-band op precies dat document: verifier PASS, ref 48,
clingo 48.

**Rood-eerst, gemeten met de ankerknop op `~:1415` teruggedraaid naar
het operand-objecttype (de helper ongemoeid):**

```
FAIL §7.1 anchor: mixed annotation folds on elementObjecttype:
     translates, program carries _wu(
FAIL §7.1 anchor: clingo answers the mixed annotation identically:
     clingo pins the values (J1.bewaarlast = [0], expected 48)
FAIL §7.1 anchor, unitless: clingo answers 48 (no write fence to help):
     clingo pins the values (J1.bewaarlast = [0], expected 48)
test_clingo: 44 checks, 3 FAILURES
```

Suite 33 → 44 checks. `setvouw_doc`'s `~tarief_noemer` werd
`~tarief_eenheid` (de HELE eenheid, dus ook `"null"`); mechanisch, geen
bestaand frame verandert van betekenis.

### R3 — de zes tekstingrepen

Alle zes staan hierboven, in de secties waar ze thuishoren:

1. **De discriminatietelling** — beide commit-boodschappen schreven de
   omvallers aan de verkeerde knop toe; de matrix per knop staat nu in
   "WIJZIGINGEN AAN BESTAANDE SUITE-CHECKS". De geschiedenis blijft.
2. **Het anker** — de gedeelde set-vouw leest `attr_temporal` RECHTSTREEKS
   op `~:1415`; `fold_operand_otype` draagt het anker voor de
   eenheidsopzoeking en de weigerarm. En "stil leeg" is "stille **0**"
   geworden — gemeten, en gevaarlijker dan een leeg.
3. **De horizonhek-restrand** is een KLASSE, niet één geval: elke op
   vertaaltijd onbeslisbare versmalling. Twee vormen nagemeten (het
   `die/dat`-filter en een alles-uitsluitende regelvoorwaarde), beide
   verifier-PASS, beide ref-antwoordt/clingo-weigert. Met de notitie dat
   de weigertekst bij zo'n vorm de échte reden hoort te geven.
4. **De WEEK/DAG/KWARTAAL-zin** — die weigering komt uit de gedeelde
   CASELEZER (`Timeline.bucket` bij het inladen), niet uit de arm; de
   niet-maand/jaar-tak van `total_factor` is onbereikbare redundantie
   zonder eigen getuige. Nagemeten, beide executors falen op dezelfde
   gedeelde regel.
5. **De ref-afhankelijkheid** bij het differentieel — `Timeline`, `Arith`
   en `Eval.linked` zijn gedeeld, dus op die assen is poort 6 geen
   onafhankelijk differentieel; de Drools-tak en de handberekende ankers
   dragen daar de onafhankelijkheid. Structureel dezelfde zorg als de
   `ref/`-versus-`spec/verifier/`-scheiding.
6. **Het Drools-eenhedenhek** — dat is er niet. Gemeten op het
   striprichting-frame: verifier WF-091 FAIL, ref fault, clingo fault,
   **Drools `PLAYED 1`**. Pre-existente eenheidsblindheid met een eigen
   kaart (`backlog/done/20260901-eenheden-op-refs.md`, vervolgpunt (b));
   niet aan deze arm toe te schrijven.

**Plus één gemeten Drools-regel.** Op het GEMENGDE-annotatieframe (het
operand-`objecttype` wijst naar een niet-temporeel `Kist`,
`elementObjecttype` blijft de temporele `Vondst`) crasht de Drools-tak:
`ERROR ClassCastException: class java.util.ArrayList cannot be cast to
class java.math.BigDecimal`. Dat is de ERROR-klasse van poort 12 — géén
waardeverschil, dus niet lijstbaar — en de poort gaat er rood op zodra
die vorm ooit in het corpus komt. Vandaag is hij alleen via
wire-chirurgie bereikbaar (geen frontend emitteert hem), dus er is niets
te doen; genoteerd zodat de volgende kaart hem niet hoeft te
herontdekken. Het straight frame speelt gewoon (`PLAYED 1`).

### R4 — de probe: één gemeten lek, gedicht

De vraag: de afgeleide-tijdlijn-weigering scande alleen
`resolvedRegels` — kan een `prioritizedDefault` of een `verdeling` op
een temporeel elementattribuut iets in die slot zetten dat `spans` nooit
zag? **Ja, gemeten.** Twee gerichte probes:

```
VERDELING (GELIJKE_DELEN, doelAttribuut = het gevouwen temporele
attribuut; de case levert er GEEN invoer voor)
  verifier : PASS
  ref      : EVALUATION FAULT: the per-element temporal total of
             "bewaartarief" over "V1": the case supplies an
             asOfDate-bucket scalar ...
  clingo   : clingo: PASS (2 expectations)          <- STILLE 0

PRIORITIZED DEFAULT op hetzelfde attribuut
  verifier : WF-086 FAIL (een defaults-groep beslist op één fixpunt en
             kan geen tijdlijn schrijven)
  ref      : EVALUATION FAULT: temporal writes are not implemented
  clingo   : clingo: PASS (2 expectations)          <- STILLE 0
```

Geen van beide leidt een `tl` af — een verdeling schrijft een SCALAR in
de temporele slot — maar ref ontmoet daar een lid zónder horizon en
weigert luid, terwijl deze arm geen `tl`-feit vond en 0 antwoordde. Het
verdelingsgeval is bovendien verifier-PASS: een echt ref ≢ clingo onder
een schoon document. De fix is klein en precies wat de opdracht
voorstelde — dezelfde weigering ook daar: de scan dekt nu elke afgeleide
schrijver van dat lidattribuut (regel, verdeling, defaults-groep, elk
met zijn eigen naam in de tekst). De defaults-arm is defense-in-depth,
want WF-086 weigert die vorm al aan de verifier.

Getuige in de suite, rood-eerst gemeten met de scan terug op alleen
`resolvedRegels`:

```
FAIL §7.1 derived writer: an allocation onto the folded member
     attribute: expected a loud fault, got a model
test_clingo: 44 checks, 1 FAILURES
```

### Poorten na de fixronde (alle dertien groen)

```
 1 check-self-contained     PASS
 2 check-ir-schema          PASS (176 bestanden valideren en round-trippen
                                  — was 175)
 3 check-verifier           PASS (176 golden clean; 281 negatief afgewezen
                                  — was 175)
 4 check-case-schema        PASS (427 case-bestanden valideren — was 426)
 5 check-oracle             PASS (405 cases exact herspeeld — was 404;
                                  2 golden zonder case, ongewijzigd:
                                  seizoenvondst, vormenkabinet)
 6 check-clingo             PASS (405 cases exact — was 404; eigen suite
                                  44 checks, 0 failures — was 33)
 7 check-noop               PASS (369 weerleggen — was 368; 36 gepinde
                                  overlevers, ongewijzigd)
 8 check-rood               PASS (22 rode cases; Drools-been 5
                                  fault-pinned, 17 not-carried)
 9 check-catala             PASS (201 checks; 12 catala-golden
                                  byte-identiek)
10 check-regelspraak        PASS (171 JUnit-tests; 30 rgs-golden
                                  byte-identiek — was 29)
11 check-toka-parity        PASS (ref 29/29, clingo 29/29, Drools 29
                                  PLAYED; 45 .rgs byte-identiek;
                                  TOKA-suite 27 tests)
12 check-drools             PASS (108 played — was 107; 43 auto-excluded;
                                  1 value-divergence
                                  (anchor/jutterswet-kustwacht,
                                  ongewijzigd); JUnit 199)
13 check-reftype-derivation PASS (64 twists over 13 golden — was 61/12;
                                  58 replays waarvan 11 door de
                                  DRL-teksttak — was 54; 0 overlevers)
```

De nieuwe golden telt overal mee: één document erbij in poort 2/3, één
case in 4, één replay in 5/6, één weerlegging in 7, één `bron.rgs` in
10, één PLAYED in 12, en één golden méér in het verdraai-harnas van
poort 13. Geen enkele bestaande telling daalt.
