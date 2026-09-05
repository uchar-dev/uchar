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

**`total_factor` draagt nu ook MAAND** (`~:402`). Dit was een DERDE
blokkade die de kaart niet noemt: de temporele route van deze golden is
maandgranulair en de arm droeg alleen JAAR. Hij kwam pas boven water
nadat het eenhedenhek loskwam — dat hek vuurt eerder. De bucket van een
granulariteit ÍS één hele eenheid, en `Timeline.bucket` bestaat alleen
voor maand en jaar, dus ref weigert de rest in de pas.

### Het temporaliteitsanker: `elementObjecttype`

**Gekozen: het `elementObjecttype`**, vastgelegd in
`fold_operand_otype` (`~:283`): bij een SET-lezing de elementsoort, bij
een gewone lezing het eigen objecttype. Reden: elke lezing in de
set-armen hangt aan de ELEMENT-variabele en elke declaratie wordt op
`el` opgezocht — daar staat het attribuut, dus daar staat ook of het een
tijdlijn is. Het operand-`objecttype` is bij beide executors inert (de
vorige kaart mat dat). Alle armen hangen er nu aan: de gedeelde
set-vouw, de MIN/MAX-guard (hing er al), en de temporele
`Totaal`-weigerarm (`~:1756`, hing aan `aot`).

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
opvoert. De suite pint hem twee keer: het geëmitteerde programma moet de
`_wu`-feiten van de tijdvouw dragen (alleen de temporele route emitteert
ze — met het anker op `aot` zou hij scalaire `val/3`-feiten joinen die
een tijdlijn-case niet heeft, en stil leeg antwoorden), en ref moet
dezelfde 48 geven.

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
leden. Eén restrand blijft, luid en genoteerd: een lid dat door het
`die/dat`-FILTER zou afvallen wordt hier geweigerd waar ref het laat
vallen — een filter is op vertaaltijd niet beslisbaar. Geen corpusgeval.

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
acht nieuwe checks, 24 → 33. Ze discrimineren, gemeten: met het anker
teruggedraaid naar het operand-objecttype en de temporele tak uitgezet
vallen er vier om, waaronder twee "expected a loud fault, got a model".

### Poorten (alle dertien groen)

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
