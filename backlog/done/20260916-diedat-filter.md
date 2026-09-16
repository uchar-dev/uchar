# diedat-filter: de deelselectie op de kwantorvouw bereikt de frontend

Aanleiding: de forall-kaart (`backlog/done/20260916-heelperiode-forall.md`,
afwijking c) stelde vast: de die/dat-deelselectie op een Kwantorvouw
("… van alle bergers DIE AANGEMELD ZIJN van de …") is gespecificeerd
(IR-SPEC ch. 4.5: `filter`, per bijdrager met het element als subject) en
door `ref` gepind, maar de RegelSpraak-frontend produceert hem niet, dus
het corpus draagt hem niet. De asymmetrie-kaart mat intussen dat de
backends de filter al dragen (clingo `fatoms`; Drools in de nieuwe
niet-temporele armen, per lens-probe). De maintainer wil de rand gedicht:
de frontend produceert de filter, en het corpus krijgt zijn drieweg-getuige.

De typerange-kaart parkeerde daarnaast één residu als "filtervraag": het
temporele vertaaltijd-hek moet een door de filter WEGgevallen lid met een
asOfDate-emmerwaarde niet als fout aanmerken (het lid doet niet mee).
Die getuige hoort bij deze kaart.

## Acceptatie

- (a) De frontend (`frontends/regelspraak`) parseert de die/dat-deelselectie
  op de kwantorvouw-positie en emitteert `filter` op de wire, met
  JUnit-getuigen in de bestaande suite-stijl (de gedragen spelling(en),
  de wire-vorm, en minstens één geweigerde/onopgeloste randvorm die luid
  valt). Meet EERST wat de huidige stand is (welke spelling resolvet
  vandaag naar wat — Onopgelost, WF-096, of een fout antwoord) en zet
  die meting als rood-eerst in het Result.
- (b) Minstens één corpus-golden MET `bron.rgs` (Jutterswet-domein) die de
  filter door het vierkant speelt: een heelperiode-matrix met
  deelselectie (drieweg: ref, clingo, Drools-exec; de DRL-tekst-tak
  markeert de heelperiodevorm — kanaalconform), en cellen die de filter
  onderscheidend maken: een lid BUITEN de selectie met een waarde die
  het verdict zou omkeren als hij meetelde; een selectie die leeg
  uitvalt (vacuüm); en de gewone gemengde cel. Handberekend vóór het
  draaien, in NOTE.md en Result. Overweeg een tweede golden of extra
  cellen op de NIET-temporele matrix met filter (die speelt sinds de
  asymmetrie-kaart drieweg, ook in de DRL-tekst-tak).
- (c) Het typerange-residu krijgt zijn getuige: een weggefilterd lid met
  een asOfDate-emmerwaarde laat de temporele vouw ongemoeid (geen
  vertaaltijd-weigering, geen bijdrage) — in de zone die past (cel in
  de golden van (b) of een eigen golden), gemeten in ref én clingo.
- (d) Alle veertien gates groen; gate 12-bewegingen (EXPECTED_PLAYED,
  kanaallijst) met eigen gedateerde stappen — gemandateerd; andere
  scripts niet aanraken.
- (e) Bestaande goldens/negatiefs/tests ongewijzigd; anchor onaangeraakt;
  `spec/CONSTRUCTS.md`/`spec/PLAN.md` niet aanraken — registervoorstel
  (rij 40-aanvulling: de frontendgrens verschuift) als letterlijke
  tekst in het Result. Als IR-SPEC- of RUNTIME-SEMANTICS-tekst
  aantoonbaar moet meebewegen: NIET doen, maar het voorstel met
  letterlijke tekst in het Result zetten (geen mandaat in deze kaart).
- (f) Backends alleen aanraken als een meting een gat toont (de filter
  wordt geacht gedragen te zijn); elke backend-wijziging met de meting
  erbij.

## Result

**De deelselectie bereikt de wire, en de meting vooraf was ernstiger dan de
kaart aannam: de frontend liet haar niet weg, hij gooide haar WEG.** De
bijzin werd geparsed, de §8.2-zin werd naar een Kwantorvouw gelowerd, en
`filter: null` ging de wire op zonder één diagnose. Een zin die over de
AANGEMELDE vondsten kwantificeert leverde een verifier-schoon document dat
over alle vondsten kwantificeert — door alle uitvoerders eensgezind
beantwoord, en fout. Alle veertien poorten groen.

**Na de lensronde is diezelfde stilte op twee buurposities gedicht** (zie "Wat
er gebouwd is", fixronde): de getalpredicaat-bijzin op de vouwpositie — een
doodlopend pad dat de frontend al kende — en de bijzin op een GEWONE lezing,
die pre-existent en zonder één diagnose werd weggegooid. Beide vallen nu
luid; de tweede blokkeert de emissie, want er is geen lezing van dat document
waaronder de weggelaten bijzin ongevaarlijk is.

### Rood-eerst 1 — de frontend, per spelling

Gemeten op de frontend zoals hij op `main` (0e2028a) stond, vóór enige
wijziging. Model: Bergingsploeg met een `premiegerechtigd` kenmerk, Berger
met `dagvergoeding` (per maand) en een `aangemeld` kenmerk.

| spelling | uitkomst vandaag |
|---|---|
| `… van ALLE bergers van de Bergingsploeg DIE AANGEMELD ZIJN gedurende het gehele jaar groter is dan 40` | `Kwantorvouw`, `"filter": null`, **nul diagnoses** |
| `… van GEEN VAN DE bergers … die aangemeld zijn …` | `"kwantor": "GEEN"`, `"filter": null`, **nul diagnoses** |
| `… van alle bergers DIE AANGEMELD ZIJN van de Bergingsploeg …` (bijzin vóór het onderwerp) | `ERROR (line 16): syntaxfout: no viable alternative at input 'dedagvergoedingvanallebergersdieaangemeldzijnvan'` |
| `… die een kind hebben …` (bezitspredicaat, dat de instantievouw luid markeert) | `Kwantorvouw`, `"filter": null`, **nul diagnoses** |

Niet `Onopgelost`, niet WF-096: de bijzin staat nog wél in de bron van het
operand (`"onderwerpBron": "alle bergers van de Bergingsploeg die aangemeld
zijn"`) en is daarna stil verdwenen. Van de drie uitkomsten die de kaart als
mogelijk noemde is dit de ergste — een stil vol bereik.

De derde rij is grammatica, geen resolutie: `onderwerpKeten ( DIE | DAT )
predicaat` hangt de bijzin aan de onderwerpketen, terwijl `onderwerp :
kwantificatie kaleNaam ( VAN onderwerp )?` geen plek voor haar heeft. De
gedragen schrijfwijze is dus dezelfde als bij de instantievouw: de bijzin
staat ACHTER het onderwerp en selecteert de LEDEN.

### Rood-eerst 2 — de backend-filterdracht, per arm

Handgeschreven probe-IR, gemeten vóór de frontendbouw. Elke probe is zo
gekozen dat de verwachting alleen klopt als de filter meetelt.

- **probe T (heelperiodematrix + filter)**: `ploegpremie` met `aangemeld` op
  Berger en de filter op beide vouwen; case B1 (aangemeld, 50 heel 2026), B2
  (niet aangemeld, 30 heel 2026), verwachting `premiegerechtigd` AANWEZIG.
  `ref` PASS · clingo PASS · Drools exec `PLAYED 1` · Drools DRL-tekst
  `EXCLUDE … een §8.2-heelperiodematrix; die wordt alleen in de
  exec-model-tak gelowerd` (kanaalconform).
- **probe N (niet-temporele matrix + filter)**: `schouwploeg` met de filter
  `gediplomeerd` op `ervaren`/`onervaren`; case W1 (gediplomeerd, 8
  dienstjaren), W2 (niet, 3). `ref` PASS · clingo PASS · exec `PLAYED 1` ·
  **DRL-tekst `PLAYED 1`**.
- **En PLAYED is hier geen stil PLAYED.** Op beide probes verwerpt elke
  dragende arm zowel de omgekeerde verwachting als dezelfde case op een IR
  ZONDER filter — vier van de vier, met de naam van het kenmerk in de
  melding.

Dus: geen gat. Acceptatie (f) is gehaald zonder één regel backendwijziging
voor de kwantorvouw-filter; de enige backendwijziging van deze kaart zit in
het typerange-residu hieronder, met zijn eigen meting.

### Rood-eerst 3 — het typerange-residu

Uit bron geschreven: `het totaal van het bewaartarief van alle vondsten van
de Jutter DIE BEWAARD ZIJN`, met één lid binnen de selectie (tijdlijn) en
één ERBUITEN dat een asOfDate-emmerwaarde draagt.

```
ref    : oracle: PASS (2 expectations)
Drools : PLAYED 1
clingo : TRANSLATION FAULT: the per-element temporal fold of "bewaartarief"
         over "V2": the case supplies an asOfDate-bucket scalar, which
         carries no period of its own; a set fold needs each member's own
         timeline (ch. 3.6/§7.1, ch. 8.4)
```

Eén uitvoerder weigert een verifier-schoon document dat de andere twee
beantwoorden, en hij wijst een instantie aan die de vouw nooit vouwt.

### Wat er gebouwd is

**Frontend** — `frontends/regelspraak`:

- `resolver/ExpressieBouwer.java`, nieuwe methode `deelselectie(ParseTree,
  Objecttype, Expr)`: de §5.6-resolutie die `aggregatieFilter` al deed —
  kenmerk (`elementKenmerkFilter`), getalpredicaat (`elementVergelijking`),
  samengestelde voorwaarde (`samengesteldeFilter`) — nu ook bereikbaar vanaf
  de kwantorvouw-positie. Eén machinerie, zodat "welke leden" niet twee
  lezingen kan krijgen.
- `resolver/RuleChecker.java`, `kwantorvouw(…)`: draagt nu de parseboom van
  het GEKWANTIFICEERDE operand mee (`uitToplevel`/`uitNonToplevel` kregen de
  `ExpressieContext` erbij, vier aanroepplaatsen). De bijzin wordt alleen
  gezocht aan de kant waar de kwantor staat; een die aan de andere kant van
  de vergelijking hoort bij die navigatie.
- Dezelfde methode kreeg een VIERDE weigeringsvorm naast de drie die er al
  stonden: een deelselectie die niet bindt laat de vouw ONGELOWERD, houdt de
  WF-096-fout staande en trekt een WARNING die haar noemt. Stil doorlaten is
  precies de fout die het veld moet voorkomen.
- `src/test/java/…/KwantorvouwFilterTest.java` (nieuw, 9 rijen): de
  wire-vorm onder ALLE en GEEN, de elementverankering (geen navigatie), de
  samengestelde bijzin, vier luid vallende randvormen (een kenmerk van het
  ONDERWERP, een bezitspredicaat, een ONTKENDE bijzin, een getalpredicaat) en
  de regressiepin voor een zin zonder bijzin. Vijf van de zes eerste rijen
  vielen op de basiskop; de zesde is de pin.

**Frontend, fixronde** — twee gaten van dezelfde soort, allebei gemeten stil:

- **de getalpredicaat-bijzin op de kwantorvouw weigert nu.**
  `elementVergelijking` kopieert het GEKWANTIFICEERDE operand als linkerzijde
  van het filter, en de §8.2-poort garandeert dat dat operand
  tijdsafhankelijk is — dus elk toegelaten getalpredicaat levert per
  constructie een document dat in alle drie de uitvoerders omvalt op ch. 3.3.
  Toelaten is daar misleidend, dus `deelselectie` weigert het nu, met de as
  erbij. De weigering is een FRONTENDgrens, geen wiregrens, en zegt dat ook:
  de KNOOP draagt de vorm (een handgeschreven `Kwantorvouw` met een
  `Vergelijking`-filter over een NIET-temporeel lid speelt in ref, clingo én
  de Drools-exec-tak). `deelselectie` geeft daarom een `Deelselectie(filter,
  reden)` terug in plaats van een kale conditie, zodat elke weigering de as
  noemt waarop zij valt in plaats van één generieke tekst:

  ```
  WARNING (line 17): de 'die/dat'-deelselectie in 'de dagvergoeding van alle
  bergers van de Bergingsploeg die groter zijn dan 10' wordt niet omgezet naar
  een kwantorvouw-filter: een getalpredicaat over het gekwantificeerde
  attribuut 'dagvergoeding' heeft hier geen lezing: de §8.2-kwalificatie geldt
  alleen over een TIJDSAFHANKELIJK onderwerp, en een tijdlijn in een scalaire
  sleuf heeft er geen (ch. 3.3) — de knoop draagt de vorm wel, deze
  schrijfpositie kan hem niet leveren; de vouw wordt daarom NIET gelowerd —
  een vouw over het volle bereik zou een andere verzameling kwantificeren dan
  de zin noemt, dus de setlezing blijft buiten een vouwbron staan (WF-096)
  ERROR (line 17): setlezing buiten een vouwbron: … (WF-096, IR-SPEC ch. 4.3)
  ```

- **een bijzin op een NIET-vouwpositie valt nu luid.** Gemeten op basis én
  kop, dus pre-existent en geen regressie van deze kaart: `… indien de
  leeftijd van de Jutter DIE BEVOEGD IS groter is dan 5` leverde een
  `Vergelijking` met de bijzin nog in `onderwerpBron`, geen spoor van
  `bevoegd` in de conditie, nul diagnoses — exact dezelfde
  verkeerde-verzameling-fout die deze kaart op de vouwpositie dicht.
  `ExpressieBouwer` houdt nu bij welke `PredicaatContext`en de resolutie
  VERBRUIKT (de instantievouw en de kwantorvouw doen dat allebei, elk op hun
  eigen route), en `RuleChecker.keurDeelselecties` loopt na afloop de hele
  parseboom af en weigert wat overblijft:

  ```
  ERROR (line 8): de 'die/dat'-deelselectie 'bevoegd is' wordt niet omgezet en
  zou stil wegvallen: een deelselectie kiest WELKE exemplaren de zin bedoelt,
  dus haar weglaten beantwoordt een andere vraag dan er staat. De uchar-wire
  draagt haar alleen op een vouwbereik (IR-SPEC ch. 4.3 en ch. 4.5, veld
  'filter'); op deze positie is zij een benoemd uitbreidingspunt en wordt zij
  geweigerd in plaats van genegeerd (§5.6)
  ```

  ERROR en geen WARNING, en dat is de hele keuze: er is geen lezing van het
  geëmitteerde document waaronder de weggelaten bijzin ongevaarlijk is, dus
  de weigering moet de emissie blokkeren. De SEMANTIEK op gewone posities is
  hiermee niet gebouwd maar benoemd — een uitbreidingspunt met luide
  weigering, zoals de tellende kwantoren dat op de vouw zijn.

- **De hermeting die deze weigering draagt:** alle **51** corpusbronnen
  emitteren byte-identiek (poort 10) én alle **45** TOKA-bronnen (poort 11,
  de ©-zone, met dezelfde twee staande holdouts). Geen enkele bron leunde op
  de stilte.
- `src/test/java/…/DeelselectieZonderVouwTest.java` (nieuw, 4 rijen): de
  weigering zelf mét haar ERROR-graad, de twee regressiepinnen dat de
  instantievouw en de kwantorvouw hun EIGEN bijzin houden, en de pin dat een
  door de vouw al geweigerde bijzin niet TWEE keer gemeld wordt (één zin, één
  weigering, en de precieze).

**Backend** — `backends/clingo/lib/translate.ml`, het vertaaltijd-horizonhek
van de per-element temporele vouw:

- het hek loopt nu de BIJDRAGERS af, dus ook door de filter heen. ref stelt
  haar bijdragers volledig vast vóór de integratie (ch. 3.4a), dus een
  weggefilterd lid is deze vouw niet aangaan.
- een filter op vertaaltijd lezen mag alleen zolang het oordeel niet meer
  kan veranderen. Daarom dezelfde vraag die het hek al over het gevouwen
  attribuut stelt (`derived_writer`), nu over de namen die de filter LEEST:
  schrijft een regel, verdeling of defaults-groep er een? Dan blijft het een
  weigering, onder haar eigen reden. `filter_reads` antwoordt `None` voor
  elke vorm die hij niet kan verantwoorden, dus een onbekende lezing wordt
  nooit voor case-data aangezien.
- beide weigeringen worden alleen gesteld over een VERDACHTE — een lid
  zonder tijdlijn met een niet-lege schaalwaarde. Zonder verdachte hangt er
  niets van het filteroordeel af, en weigeren zou documenten treffen waar
  het hek nooit over ging.

Gemeten, alle vier de richtingen:

| probe | ref | clingo |
|---|---|---|
| weggefilterd lid met emmerwaarde (`golden/jutterswet-keurlast`) | PASS | **PASS** (was TRANSLATION FAULT) |
| lid BINNEN de selectie met emmerwaarde | `EVALUATION FAULT … asOfDate-bucket scalar … "V2"` | `TRANSLATION FAULT … asOfDate-bucket scalar … "V2"` — vuurrichting intact |
| filter over een AFGELEID kenmerk, mét verdachte | PASS | `TRANSLATION FAULT: … carries a die/dat subselection over "bewaard", which a rule DERIVES on "Vondst": its verdict is not settled at translate time …` |
| filter over een afgeleid kenmerk, ZONDER verdachte | PASS | PASS — geen overweigering |

`golden/jutterswet-muntbewaring` en `golden/jutterswet-tempovouw`
antwoorden onveranderd.

- `backends/clingo/test/test_clingo.ml` — vier rijen ADDITIEF toegevoegd (geen
  bestaande rij gewijzigd; gemandateerd bij de fixronde). Het corpus pint de
  ANTWOORDENDE kant van het filterbewuste hek; deze rijen pinnen de
  weigerende, die geen rood-corpusplek kan krijgen omdat `ref` beide
  documenten beantwoordt en poort 8 een fout in BEIDE uitvoerders eist. De
  twee gepinde teksten, voluit:

  ```
  the per-element temporal fold of "bewaartarief" carries a die/dat
  subselection over "gekeurd", which a rule DERIVES on "Vondst": its verdict
  is not settled at translate time, and a member without its own timeline
  hangs on the answer (ch. 3.4a/3.6)
  ```

  ```
  the per-element temporal fold of "bewaartarief" carries a die/dat
  subselection this translation cannot settle at translate time (an
  unrecognised read), and a member without its own timeline hangs on the
  answer (ch. 3.4a/3.6)
  ```

  Naast die twee staan twee GRENSrijen die zeggen waar de weigeringen NIET
  gelden: hetzelfde onleesbare filter zonder een lid zonder eigen tijdlijn
  wordt door ref én clingo gewoon beantwoord. Suite 205 → 209 controles.

**Corpus** (beide uit `bron.rgs`, beide gate-10-herémitteerbaar):

- `corpus/golden/jutterswet-keurploeg` — de §8.2-kwantorvouw met
  deelselectie, zeven cellen, ALLE en GEEN in één document, plus een
  ONGEFILTERDE telling (`keurtal`) die per cel zegt hoe groot het ruwe
  bereik is.
- `corpus/golden/jutterswet-keurlast` — de per-element temporele vouw met
  deelselectie; drie jutters, één case.
- `corpus/negatief/wf034-kwantorvouw-filter-onderwerpkenmerk` — een filter
  die een kenmerk van het VOUWONDERWERP leest. De check bestond al (de
  verifier loopt filter en toets door dezelfde walk met de
  elementverankering), dus dit is een getuige en geen verifier-wijziging:
  `WF-034 FAIL /resolvedRegels/0/voorwaarde/filter: 'stilliggend' is not a
  declared characteristic of 'Keurmeester'`.

### De cellen, met de hand berekend vóór enige uitvoerder draaide

**`jutterswet-keurploeg`.** Grens 40, strikt `groter dan`, kalenderjaar 2026
(365 dagen), asOfDate 2026-03-15 en voor geen enkel oordeel relevant. Een
lid is BIJDRAGER als het `aangemeld` is; het voldoet aan de MATRIX als zijn
`keurvergoeding`-tijdlijn heel 2026 boven 40 ligt.

| case | de keurmeesters (aangemeld?, keurvergoeding) | keurtal | bijdragers | premiewaardig | stilliggend |
|---|---|---|---|---|---|
| 1 | K1 (ja, 50 heel jaar), K2 (**nee**, 30 heel jaar) | 2 | {K1} | **ja** | nee |
| 2 | K1 (ja, 50), K2 (ja, 30) | 2 | {K1,K2} | nee | nee |
| 3 | K1 (**nee**, 50), K2 (**nee**, 30) | 2 | {} | **ja** | **ja** |
| 4 | K1 (ja, 40 heel jaar) | 1 | {K1} | nee | **ja** |
| 5 | K1 (ja, 50 heel jaar), K2 (ja, 60 op [jan, jul) ) | 2 | {K1,K2} | nee | nee |
| 6 | geen keurmeesters | 0 | {} | **ja** | **ja** |
| 7 | K1 (**nee**, 50), K2 (ja, 30) | 2 | {K2} | nee | **ja** |

Cel 1 is het lid buiten de selectie dat ALLE zou breken; cel 7 is de
spiegel, het lid buiten de selectie dat GEEN zijn getuige zou geven; cel 3
is de LEGE selectie naast een gevuld bereik (`keurtal` 2), waar beide
kwantoren vacuüm slagen en een filterloze lezer van beide precies het
tegengestelde antwoordt. Cel 2 is de gewone gemengde cel en tegelijk de
`GEEN ≠ ¬ALLE`-cel; cel 4 pint de strikte grens; cel 5 gedeeltelijke dekking
BINNEN de selectie; cel 6 het lege BEREIK, het tweede vacuüm.

**Tegenproef, na afloop gedraaid:** dezelfde zeven cases op een IR waaruit de
filter is weggehaald falen in ref op precies 1, 3 en 7, en de Drools-exec-tak
noemt dezelfde drie bij naam. De andere vier zijn filter-neutrale controles.

**`jutterswet-keurlast`.** `totaal` (§4.20, Tabel 30): per segment
`waarde × hele roostereenheden van zijn eigen span`; bijdragers zijn de
vondsten die `gekeurd` zijn.

| jutter | vondsten (gekeurd?, keurtarief) | bijdragers | keurlast |
|---|---|---|---|
| J1 | V1 (ja, 12 €/mnd op [2026-01-01, 2026-04-01)), V2 (**nee**, emmerwaarde 99) | {V1} | 12 × 3 = **36 €** |
| J2 | V3 (**nee**, emmerwaarde 50) | {} | **0 €** |
| J3 | V4 (ja, 5 €/mnd op [jan, mrt)), V5 (ja, 3 €/mnd op [mrt, apr)) | {V4,V5} | 5 × 2 + 3 × 1 = **13 €** |

J2 scheidt "de deelselectie heeft de bijdragers leeggemaakt" van "de relatie
leverde niets": een leeg `totaal` is 0, niet leeg, en het enige aanwezige lid
is opnieuw een emmerwaarde.

### De veertien poorten (eindstand, alle groen)

```
1  check-self-contained: PASS
2  check-ir-schema: PASS (schema valid; 24 pattern self-test rows; 221 files validate and round-trip, floor 192)
3  check-verifier: PASS (221 golden clean; 392 negatief rejected for their declared rule, floor 350)
4  check-case-schema: PASS (schema valid; 520 case files validate)
5  check-oracle: PASS (ref suite 620 worked examples, floor 489; 494 cases replayed exactly, floor 427)
6  check-clingo: PASS (backend suite 209 checks, floor 83; 494 cases replay exactly through clingo)
7  check-noop: PASS (458 cases refute a do-nothing executor; 36 pinned survivors)
8  check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)
9  check-catala: PASS (frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically)
10 check-regelspraak: PASS (JUnit 276 tests, floor 173; 51 rgs goldens re-emit byte-identically)
11 check-toka-parity: PASS (45 .rgs re-emitted byte-identical, floor 45; 29 cases through ref, clingo, Drools; 0 divergences)
12 check-drools: PASS (played 145 = pinned 145; channel floor 50; value-divergence 1; JUnit 354 tests, floor 239)
13 check-reftype-derivation: PASS (108 annotations twisted over 22 goldens, floor 74; 100 replays, 19 DRL-text PLAYED, floor 13)
14 check-flogic: PASS (frontend suite 81 checks, floor 81; 1 flogic golden re-emits byte-identically)
```

Bewegingen: poort 2 en 3 van 219 naar 221 documenten (en 391 naar 392
negatiefs), poort 4 van 512 naar 520 cases, poort 5 en 6 van 486 naar 494,
poort 6's eigen suite van 205 naar 209 controles, poort 7 van 450 naar 458,
poort 10 van 263 naar 276 tests en van 49 naar 51 rgs-goldens, poort 12's `EXPECTED_PLAYED` van 143 naar 145 (twee eigen
gedateerde, redendragende stappen in `scripts/check-drools.sh`; de
kanaallijst blijft 50, want beide nieuwe goldens spelen), poort 13 van 105
naar 108 verdraaide annotaties over 21 naar 22 goldens en van 18 naar 19
DRL-tekst-spelers — `jutterswet-keurlast` draagt `€/mnd`, dus hij wordt
verdraaid en de DRL-tekst-tak speelt hem. Geen enkele vloer verlaagd.

### De toolchain-naschok, gemeten (alleen gelezen, niets gebouwd)

Na de herpin levert de gevendorde resolver GEVULDE filters aan de
toolchain-transpilers. Gemeten op `regelspraak-java` @ `9ad12b6` (read-only),
en het antwoord is het ongevaarlijke: **beide takken DRAGEN `kv.filter()`, en
allebei weigeren zij bij naam wat zij niet kunnen binden. Geen enkele tak
negeert hem stil.**

| plek | regels | wat er staat |
|---|---|---|
| `transpiler/DrlTranspiler.java`, `kwantorvouwVoorwaarde` | 3459–3463 | `conditie(acc, kv.filter(), elem)` met het ELEMENT als onderwerp; de tekst gaat als extra constraint het accumulate-SOURCE-patroon in naast de join. `fc.marker()` → `kvMarker(kv, "een die/dat-subselectie die deze tak niet op de source kan binden")` |
| `transpiler/model/ExecModelTranspiler.java`, `kwantorvouwVoorwaarde` | 2422–2425 | `filterSource(src, kv.filter(), elem)`; `null` → `kvSkip(kv, "een die/dat-subselectie die deze tak niet op de source kan binden")` |
| `transpiler/Stratificatie.java` | 159–160 | `verzamelReads(kv.filter(), kv.elementObjecttype(), uit)` — de filter telt mee in de stratificatie, met het elementtype als onderwerp |
| `graaf/ProducentConsument.java` | 127–128 | `verzamelConditie(kv.filter(), kv.elementObjecttype(), uit)` — idem in de afhankelijkheidsgraaf |

Vier plekken, vier keer gelezen: de twee die lowert en de twee die de
volgorde bepalen. Een filter die een AFGELEIDE waarde leest komt daar dus ook
in de ordening terecht, wat precies de vraag is waarop het ASP-hek in deze
kaart conservatief werd.

Eén ding is géén naschok maar wel het vermelden waard: beide
toolchain-takken weigeren nog een matrix ZONDER §8.2-kwalificatie
(`DrlTranspiler` r. 3411, `ExecModelTranspiler` r. 2367) — de niet-temporele
arm van de asymmetrie-kaart is daar nog niet geland. Dat raakt de filter
niet, want de frontend levert die vorm sowieso niet uit bron.

### Registervoorstel (letterlijk, voor de hoofdsessie)

**Rij 40, aan te vullen achter de bestaande tekst.** De aanvulling VERSMALT
bovendien de bestaande clausule "the frontend does NOT produce this shape —
measured, both spellings … widening that is a frontend card of its own":
die blijft waar voor de NIET-temporele vorm, maar niet meer voor het
bereikveld waar zij over zwijgt. De aanvulling zegt dat met zoveel woorden,
naar het precedent van de rij-29-correctie in de forall-kaart:

**Landed (2026-09-16, kaart diedat-filter) — the die/dat SUBSELECTION now reaches the wire, and this NARROWS the clause above:** where that clause says the frontend does not produce this shape, it now says so of the §8.2 GATE only. `RuleChecker.kwantorvouw` still lowers the §8.2-QUALIFIED condition and nothing wider — the non-temporal matrix is still hand-written IR, and widening that is still a frontend card of its own — but the fold's RANGE is no longer half-written: `filter` (ch. 4.5) comes out of `bron.rgs`. What the measurement found first was not a missing field but a SILENT one. The frontend PARSED the §5.6 bijzin (it stood in the operand's `bron`), lowered the sentence, and wrote `filter: null` with ZERO diagnostics under both quantifiers — so "de keurvergoeding van alle keurmeesters van de Keurploeg DIE AANGEMELD ZIJN" produced a verifier-clean document quantifying over every keurmeester, which all three executors then answered consistently and wrongly. The subselectie is resolved by exactly the machinery the instance fold's `filter` already used (kenmerk, getalpredicaat, compound), bound against the ELEMENT type, and it is looked for in the parse subtree of the QUANTIFIED operand alone — the bijzin trails the onderwerp but selects the members, so a die on the other side of the comparison belongs to that operand's navigation. The node's lowering gained a FOURTH refusal beside its three: a subselection that does not bind leaves the fold UNLOWERED and keeps the WF-096 ERROR, with a WARNING naming it — never a fold over the full range, which is the one failure this field exists to prevent. Witness `corpus/golden/jutterswet-keurploeg` (seven hand-computed cells, ALLE and GEEN in one document, plus an UNFILTERED count that states the raw range per cell; three cells are decided by the filter alone — the member outside the selection that would break ALLE, its mirror that would hand GEEN a witness, and the selection that comes out EMPTY beside a range of two — and removing the filter from the IR fails exactly those three in ref and in the Drools exec branch), negatief `wf034-kwantorvouw-filter-onderwerpkenmerk` (a filter reading a characteristic of the fold's SUBJECT: the element re-anchoring is what refuses it, through the check that already existed). Measured and NOT changed: all three executors carried `Kwantorvouw.filter` before this card — `ref` filters the members, the ASP backend joins the filter atoms into the witness rule under both quantifiers, the Drools exec branch constrains the accumulate source, and the DRL-text branch writes them into the element pattern; each verified with a hand-written probe plus its mutant and a filter-less counterfactual, so no arm was silently ignoring the field. The DRL-text branch markers this golden for its WHOLE-PERIOD matrix, which is the standing channel split and unchanged | the §8.2 gate is unmoved: a fold whose matrix carries no whole-period qualifier still does not come from source. Two subselectie shapes are refused at the FRONTEND and neither is a wire limit. A getalpredicaat ("die groter zijn dan 10") resolves — §5.6 names no attribute of its own, so the comparison's left side IS the quantified operand — but on THIS position that operand is temporal by construction, and a timeline in a scalar slot has no reading (ch. 3.3), so every such filter would emit a document that falls over by name in all three executors; the NODE carries the shape (a hand-written Kwantorvouw with a Vergelijking filter over a NON-temporal member plays in ref, clingo and the Drools exec branch), this sentence position cannot produce it. A NEGATED kenmerk ("die niet aangemeld zijn") does not bind either, exactly as it does not on the instance fold. **A SUBSELECTION ON AN ORDINARY (NON-FOLD) POSITION is the named extension point of this row's neighbourhood**, and it was measured SILENT on base and head alike — "indien de leeftijd van de Jutter DIE BEVOEGD IS groter is dan 5" produced a Vergelijking with the bijzin still in `onderwerpBron`, no trace of `bevoegd` in the condition and zero diagnostics, the same wrong-set failure this row closes on the fold position. It is now an ERROR that blocks the emission (a WARNING would leave that document on disk, and there is no reading of it under which the omitted subselectie is harmless): the resolver records which subselecties it CONSUMED — the instance fold and the quantifier fold each on their own route — and refuses whatever the parse produced and nothing consumed. What a subselection MEANS on a scalar navigation is a spec question, so this is a loud refusal at an extension point, the posture the counting quantors have in WF-108 (a) — not a semantics. Measured over every source in the repository: all 51 corpus `bron.rgs` and all 45 TOKA sources re-emit byte-identically, so no source leaned on the silence |

**Rij 39, aan te vullen achter de bestaande tekst** (de rij die het
vertaaltijd-hek al draagt):

> De derde plek hierboven — het vertaaltijd-hek van de per-element temporele vouw — had nog één geparkeerde rand, en die is 2026-09-16 (kaart diedat-filter) gesloten: de die/dat-FILTER. Het hek liep de leden af mét het elementtype maar ZONDER de deelselectie, dus een weggefilterd lid met een asOfDate-emmerwaarde werd bij naam geweigerd terwijl ref en de Drools-backend het document gewoon beantwoorden — een vouw stelt haar bijdragers volledig vast, filter en al, vóór de integratie (ch. 3.4a). Een filter op VERTAALtijd lezen mag alleen zolang het oordeel niet meer kan veranderen, dus het laten vallen staat onder dezelfde afgeleide-schrijver-vraag die het hek al over het gevouwen attribuut stelt, nu gesteld over de namen die de filter LEEST; een vorm die de vertaler niet kan verantwoorden telt nooit als case-data. Beide weigeringen worden alleen gesteld waar er iets van afhangt — een lid zonder eigen tijdlijn met een niet-lege schaalwaarde — zodat een onleesbaar filter geen documenten weigert waar het hek nooit over ging. Getuige `golden/jutterswet-keurlast` (36 / 0 / 13, met het weggefilterde lid dat de emmerwaarde draagt); de VUURrichting is gemeten intact gebleven, met hetzelfde lid binnen de selectie.

### Eerlijke afwijkingen en twijfels

**(a) De twee nieuwe ASP-weigeringsredenen hebben hun getuige gekregen —
in een BESTAAND testbestand, additief en met mandaat.** In de eerste ronde
stonden zij alleen met de hand gemeten in dit Result, omdat zij nergens
anders kunnen: een rood-corpusplek past niet (`ref` ANTWOORDT beide
documenten, en poort 8 eist een fout in BEIDE uitvoerders). Bij de fixronde
is dat mandaat gegeven en zijn vier rijen toegevoegd aan
`backends/clingo/test/test_clingo.ml` — twee weigeringen plus twee
GRENSrijen die zeggen waar zij niet gelden. Geen bestaande rij gewijzigd; de
suite gaat van 205 naar 209 controles en `EXPECTED_MIN_SUITE_CHECKS` (83) is
een ondergrens, dus poort 6's script is niet aangeraakt. De teksten staan
voluit onder "Wat er gebouwd is".

**(b) Geen tweede golden op de NIET-temporele matrix met filter.** Overwogen
zoals de kaart vroeg, en niet gedaan. De reden is gemeten, niet aangenomen:
in alle drie de uitvoerders zijn filter en matrix ORTHOGONAAL — `ref`
filtert de leden vóór de kwantificatie, de ASP-backend zet `fatoms` naast de
join in dezelfde getuigenregel ongeacht de matrix, en de Drools-exec-tak
bouwt `filterSource` vóór hij tussen de twee matrixroutes kiest — dus zo'n
golden voegt aan gate 5, 6 en 12 niets toe dat `keurploeg` niet al pint. De
ENIGE plek waar de twee niet orthogonaal zijn is de DRL-tekst-tak, waar
filter en matrix in één elementpatroon worden geschreven en de
heelperiodevorm al eerder markeert; die tak is per probe N gemeten (`PLAYED
1`, mét de filter beslissend), maar een POORT die haar op de kwantorvouw
meet bestaat niet: gate 12 draait alleen de exec-tak, en gate 13 raakt een
golden alleen als die een eenheidsdragende annotatie draagt. Zo'n annotatie
onder een Kwantorvouw zetten zou gate 13's boekhoudklem kunnen laten vallen
("een eenheidsdragende annotatie bij een knoop buiten de verdraaitabel"), en
dat script is beschermd en niet gemandateerd in deze kaart. Vandaar: gemeten
en gemeld, niet gebouwd.

**(c) De getalpredicaat-bijzin wordt nu WEL geweigerd — besluit van de
fixronde.** In de eerste ronde liet ik hem toe en meldde ik de uitkomst: het
document valt bij naam om in alle drie de uitvoerders (`ref`: *"temporal
attribute … in a scalar context is refused (ch. 3.3)"*; clingo: *"… in a
scalar slot is refused (ch. 3.3)"*; Drools exec: `EXCLUDE … een
die/dat-subselectie die deze tak niet op de source kan binden` — en die
laatste noemt bovendien de verkeerde as). Luid, dus niet stil, maar wel een
document waarvan de frontend al weet dat niemand het kan draaien. Bij de
fixronde is besloten dat toelaten hier misleidend is: de §8.2-poort maakt het
gekwantificeerde operand per constructie tijdsafhankelijk, dus dit pad is
doodlopend en niet zeldzaam-fout. De weigering is nu gebouwd, formuleert
zichzelf als FRONTENDgrens en zegt erbij dat de knoop de vorm wél draagt.

**(c2) De twee vouwen reageren VERSCHILLEND op een onbindbare bijzin.**
Gemeten, en hier als feit vastgelegd zonder het te harmoniseren. De
INSTANTIEvouw zet een `Onopgelost`-filter op de wire, trekt een WARNING en
EMITTEERT het document — dat valt dan bij evaluatie om op WF-044. De
KWANTORvouw lowert niet, trekt een WARNING en laat de WF-096-ERROR staan —
dat document komt er niet. Allebei luid, en de asymmetrie heeft een grond:
op de vouwpositie ís er een geldige wire-vorm om een marker in te zetten,
terwijl een kwantorvouw zonder filter een ANDERE verzameling kwantificeert
dan de zin noemt. Of die twee gelijkgetrokken moeten worden is een eigen,
gemeten kaart; deze kaart heeft de bestaande vouwroute niet aangeraakt.

**(d) Twee verouderde commentaren in `KwantorvouwTest` zijn bijgewerkt —
met mandaat van de fixronde, en alleen commentaar.** De javadoc sprak van
"three shapes that decline to lower" (het zijn er vier sinds deze kaart) en
de regel bij `"filter":null` zei *"deze kaart levert geen filter"*. Beide
zijn nu juist en verwijzen naar `KwantorvouwFilterTest`. Geen enkele
ASSERTIE gewijzigd: `"filter":null` blijft de regressiepin voor de zin
zonder bijzin, en die klopte al. Alleen de faalboodschap ernaast is
meegegaan.

**(d2) Eén commit was een schemacorrectie, niet inhoud.** `36fac4c`: de
vacuümcel schreef `"keurmeesters": []` om te zeggen dat het bereik leeg is,
maar het caseschema zet `minItems: 1` op rolarrays. De rol wordt nu
weggelaten — de afwezigheid ÍS de lege verzameling, en zo schrijft
`jutterswet-ploegpremie` zijn lege-set-case ook.

**(e) `Kwantorvouw.filter` was nergens gebroken.** De kaart nam dat aan; het
is nu gemeten in alle vier de armen, mét mutant en tegenproef. Er is dus
geen backendwijziging voor dit deel, en de enige `backends/`-diff van deze
kaart hoort bij het typerange-residu.

**(f) Niets buiten de opdracht aangeraakt.** Geen `spec/`, geen `toka/`,
geen bestaande golden of negatief; van `scripts/` alleen `check-drools.sh`,
en daarin alleen `EXPECTED_PLAYED` met twee gedateerde, redendragende
stappen. Twee BESTAANDE testbestanden zijn wel aangeraakt, allebei met
mandaat van de fixronde en allebei zonder een assertie te wijzigen:
`backends/clingo/test/test_clingo.ml` (vier rijen additief, zie (a)) en
`frontends/regelspraak/…/KwantorvouwTest.java` (alleen commentaar en één
faalboodschap, zie (d)).

**(g) De toolchain-naschok is gemeten en goedaardig.** Zie de eigen sectie
hierboven: beide toolchain-transpilers én beide afhankelijkheidswandelingen
LEZEN `kv.filter()`, en de twee transpilers weigeren bij naam wat zij niet
kunnen binden. Er is dus geen stille negeerder aan de andere kant van de
herpin. De fix van de toolchain is de volgende ronde en niet deze kaart;
`regelspraak-java` is alleen gelezen.
