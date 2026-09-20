# datum-setbron-breuk: SOM telt dagnummers op waar ref een typefout werpt

Gemeten (kaart eerste-laatste-setvouw-kloof, 2026-09-19, §1; door de
lens onafhankelijk gereproduceerd): op een verifier-SCHOON document met
een DATUM-setbron splijten de niet-ordenende vouwsoorten het vierkant,
en de splijting is per uitvoerder anders:

- **SOM / TOTAAL**: `ref` werpt `plus: expected a number, got
  2024-01-15` (datums sommeren is een typefout), de ASP-backend
  **antwoordt 39520** — de som van de dagnummers 19737+19783 — en de
  Drools exec-tak gooit een `ClassCastException` (`LocalDate` naar
  `BigDecimal`); de DRL-tak weigert op compile. Drie uitvoerders, drie
  gedragingen, waarvan één een stellig getal.
- **MINIMUM / MAXIMUM**: `ref` en clingo zijn het eens (2024-01-15 /
  2024-03-01 — de ordenende lezing is welgedefinieerd op datums), maar
  de Drools exec-tak gooit (`kan accumulate-resultaat niet naar
  BigDecimal omzetten`) en de DRL-tak weigert op compile — een crash is
  geen benoemde weigering.

Er is geen producentroute: de RegelSpraak-frontend typt de zin weg
(`som van vereist numerieke operanden, kreeg Datum in dagen`). De vorm
is dus alleen met de hand op de wire te schrijven — maar hij is
verifier-schoon, en het gate-8-principe zegt dat een stellig fout
antwoord (clingo's dagnummersom) erger is dan een fout.

Kandidaatrichting (te wegen bij oppakken, `ref` is de arbiter): de
ASP-vertaling weigert of fault SOM/TOTAAL over een datum-setbron zoals
`ref` dat doet (rood-getuige mogelijk zodra beide OCaml-uitvoerders
observably falen); de Drools-takken weigeren per construct met reden in
plaats van te crashen; MIN/MAX over datums blijft de gedragen ordenende
lezing (ref ≡ clingo staat er al — een golden kan die helft dichten).

## Acceptatie (aangescherpt bij oppakken, 2026-09-20)

`ref` is de arbiter en blijft onaangeraakt; waar zij faalt mag geen
andere uitvoerder een getal geven, waar zij draagt mag geen uitvoerder
stil crashen.

- (a) De §1-tabel is HERMETEN op de kop van deze kaart — per soort (SOM,
  TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE) en per uitvoerder
  (verifier, `ref`, clingo, Drools exec, Drools `--drl`), op één document
  waarin alleen `kind` verschilt — en de tabel staat in het Result. De
  kaartstand van 2026-09-19 kan verschoven zijn en de gemeten stand
  regeert.
- (b) SOM/TOTAAL over een DATUM-setbron: de ASP-backend geeft geen getal
  meer. Zij weigert met een benoemde, fase-dragende zin die het
  geweigerde CONSTRUCT noemt — geen bodemzin in de zin van gate 8's
  36-lijst — rood-eerst gemeten, met een mutatieproef die de zin
  terugdraait en de getuigen bij naam laat falen.
- (c) Een RODE getuige pint beide OCaml-weigeringen: `expectedFault
  {ref, clingo}` met fragmenten die geen bodemzin citeren, en de
  Drools-uitkomst staat met reden in de uitsluitingslijst van gate 8.
- (d) Beide Drools-takken weigeren ALLE VIER de niet-producentroute-
  soorten over een datum-setbron BENOEMD en met reden, in plaats van te
  crashen (ClassCastException, accumulate-cast) of op DRL-compile te
  vallen. Per tak een getuige in de eigen JUnit-suite, plus een
  mutatieproef: de crash terugzetten laat die getuige bij naam falen.
- (e) MIN/MAX over een datum-setbron blijft de gedragen ordenende lezing
  en krijgt een GOLDEN met handberekende absolute datums, gespeeld door
  `ref` en clingo, met een benoemde regel in BEIDE kanaallijsten van
  gate 12. EERSTE/LAATSTE over dezelfde bron worden expliciet meegewogen
  en hun stand gedocumenteerd — dekt de golden ze mee, of de weigering.
- (f) Corpussweep over beide Drools-takken vóór en ná: geen onverklaarde
  kanaalwisselingen. De gevolgen in `scripts/check-drools.sh` en
  `scripts/check-rood.sh` zijn puur ADDITIEF (een regel plus redenblok,
  geen herordening, geen herschreven bestaande reden) en landen als
  beschermd-padvoorstel.
- (g) Alle veertien poorten los groen, met kerngetallen.

## Hermeting op de kop van de kaart (2026-09-20, HEAD ccf7c793 + claim)

Eén document — `corpus/golden/jutterswet-vondstvenster/ir.json`, beide
regels op dezelfde `kind` gezet — en één casus: J1 met V1 (2024-01-15)
en V2 (2024-03-01). Dagnummers: 2024-01-15 = 19737, 2024-03-01 = 19783.
Alleen `kind` verschilt tussen de rijen.

| kind | verifier | `ref` | clingo | Drools exec | Drools `--drl` |
|---|---|---|---|---|---|
| SOM | PASS | FOUT `plus: expected a number, got 2024-01-15` | **39520** | ERROR `ClassCastException: LocalDate → BigDecimal` | EXCLUDE `drl compile` |
| TOTAAL | PASS | idem | **39520** | ERROR idem | EXCLUDE `drl compile` |
| MINIMUM | PASS | 2024-01-15 | 2024-01-15 | ERROR `kan accumulate-resultaat niet naar BigDecimal omzetten: LocalDate` | EXCLUDE `drl compile` |
| MAXIMUM | PASS | 2024-03-01 | 2024-03-01 | ERROR idem | EXCLUDE `drl compile` |
| EERSTE | PASS | 2024-01-15 | 2024-01-15 | EXCLUDE `transpiler SKIP … waarde buiten scope` | EXCLUDE `MARKER TODO aggregatie EERSTE` |
| LAATSTE | PASS | 2024-03-01 | 2024-03-01 | EXCLUDE `transpiler SKIP … waarde buiten scope` | EXCLUDE `MARKER TODO aggregatie LAATSTE` |

De stand is ongewijzigd ten opzichte van 2026-09-19 met één toevoeging
die de kaart niet noemde: EERSTE/LAATSTE zijn sinds de setvouw-kaart in
clingo gedragen, dus zij zijn géén deel van de breuk meer — ref ≡ clingo
draagt vier van de zes soorten en de ASP-backend antwoordt alleen op de
twee optelsoorten een getal waar de arbiter faalt.

Producentroute, opnieuw gemeten: er is er geen voor ENIGE van de vier
soorten. De frontend typt SOM weg (`som van vereist numerieke operanden,
kreeg Datum in dagen`) en MINIMUM/MAXIMUM net zo (`minimale waarde van
vereist numerieke operanden, kreeg Datum in dagen`). Alleen EERSTE en
LAATSTE hebben een levende bronzin — die van `jutterswet-vondstvenster`.

May touch: `backends/clingo/**`, `backends/drools/**` (weigeringen),
`corpus/golden/**` en `corpus/rood/**`, `scripts/check-drools.sh` en
`scripts/check-rood.sh` uitsluitend telling/lijst (voorstel),
kaartverplaatsing. Out of scope: `ref/` (arbiter), `frontends/**`.
NB: serieel met andere `translate.ml`- en DrlTranspiler-kaarten.

---

## Result (2026-09-20)

**De breuk is gedicht en hij liep niet waar de kaart hem tekende.** De
kaart splitst op SOORT — optellen tegen ordenen — en dat klopt voor de
twee OCaml-uitvoerders. Voor Drools loopt de grens op iets anders: daar
crashte ELKE waardevouw over een niet-numeriek lid, en de soort bepaalde
alleen welke crash. Eén gedeelde vraag, één fase eerder gesteld, sluit
alle vier de soorten én een vijfde vorm die de kaart niet noemde (een
tekstbron). De ASP-kant is wél precies de kaartsplitsing: alleen de
optelsoorten gaven een getal, alleen zij weigeren nu.

### 1. De hermeting

Staat hierboven, vóór de acceptatie. Twee dingen kwamen eruit die de
kaart niet had:

1. **EERSTE en LAATSTE zijn geen deel van de breuk meer.** Sinds de
   setvouw-kaart draagt de ASP-backend ze, dus ref ≡ clingo geldt voor
   vier van de zes soorten. De kaarttabel van 2026-09-19 noemde ze nog
   als TRANSLATION FAULT.
2. **Er is voor GEEN van de vier soorten een producentroute.** De kaart
   meldde dat alleen voor SOM. Gemeten: `minimale waarde van vereist
   numerieke operanden, kreeg Datum in dagen` — de frontend typeert ook
   MINIMUM en MAXIMUM over een datumbron weg. Daarom is de nieuwe golden
   handgeschreven IR zonder `bron.rgs` en heeft poort 10 er niets aan te
   hercontroleren.

### 2. De ASP-weigering, woordelijk

`backends/clingo/lib/translate.ml`, in de bestaande `Som | Totaal`-setarm,
vóór iedere emissie en gevraagd aan de DECLARATIE op het elementtype
(§2.10, zoals elke andere lezing in die arm):

> a SOM fold over the set source "vondstdatum" adds DATE-valued members
> of "Vondst": an additive fold is arithmetic, a date is a calendar
> POSITION, and adding two of them has no meaning — this translation
> would add their day numbers and answer a definite number no calendar
> reads back. Declined in translation

Met `TOTAAL` op de plaats van `SOM` voor die soort. De zin noemt het
geweigerde construct (de soort, de bron, het ledentype) en is dus geen
bodemzin in de zin van gate 8's 36-lijst; de fase-staart staat erin,
zoals bij de ch.-3.10-weigeringen.

**Waarom alleen hier en niet één arm lager.** `#min`/`#max` GEVEN één
bijgedragen waarde terug: een dagnummer gaat erin en hetzelfde dagnummer
komt eruit. De ordenende lezing over een datumbron is welgedefinieerd
(ch. 1.2e) en blijft dragen.

**Waarom `Datum_in_dagen` exact en niet `is_date`.** De arm matcht het
day-getypeerde datumtype precies, want dat is de hele reikwijdte die deze
positie heeft — gemeten, niet aangenomen. Een `DatumTijd`-setbron komt
hier nooit aan: zijn regel loopt één knoop eerder door `dt_term` (het
doel is datum-tijd getypeerd, en een numeriek doel zou WF-091 zijn) en
die arm weigert hem daar al. `is_date` schrijven zou een bereik claimen
dat de arm niet heeft en een tak achterlaten die geen document betreedt.

**De weigering gaat over het DOCUMENT en niet over de casus — en dat
kost twee gemeten asymmetrieën, niet één.** `agg_som`/`agg_totaal`
vouwen met `List.fold_left add first rest`, dus onder TWEE gevulde
bijdragers wordt er nooit opgeteld en antwoordt `ref` zonder de typefout
te raken:

| bijdragers | `ref` (som) | `ref` (totaal) | clingo |
|---|---|---|---|
| 0 | leeg | **0** (§4.20) — een getal in een datumslot | weigert |
| 1 | dat ene lid, een DATUM | dat ene lid, een DATUM | weigert |
| ≥ 2 | FOUT `plus: expected a number` | idem | weigert |

De singletonrij is de scherpste: daar geeft `ref` een DATUM terug, wat
welgetypeerd is (de vouw is er een identiteit), terwijl de nulrij van de
totaal een GETAL in een datumslot zet, wat dat niet is. Beide staan in
`corpus/rood/datumsetbron-totaalvouw/NOTE.md`. De richting is in alle
gevallen de onschadelijke: een weigering tegenover een antwoord dat geen
hoofdstuk definieert. `ref` corrigeren is de arbiter zelf en buiten scope.

**Mutatieproef, uitgevoerd.** Guard uit (`when false && …`), herbouwd:
beide rode casussen melden `EXPECTED FAULT DID NOT OCCUR: clingo found a
model`. Teruggedraaid, herbouwd, beide weer groen.

### 3. De Drools-weigeringen, woordelijk

Eén gedeelde redenfunctie,
`backends/drools/src/main/java/.../transpiler/kern/Vouwwaarde.java`,
gevraagd door beide takken vóór ze iets lowereren — de exec-tak in
`aggregatie`, de DRL-tak in `accumulate`. De kop is gedeeld:

> vouw MINIMUM over de setbron 'vondstdatum' van Vondst: die leden zijn
> van type Datum in dagen en niet getalachtig, terwijl beide takken elke
> waardevouw in BigDecimal accumuleren

en de staart is DRIE-WEGS, gekozen op de SOORT én op het LEDENTYPE, want
het zijn drie verschillende beweringen:

> **optellend, elk niet-getalachtig ledentype** — . Optellen is
> rekenkunde en alleen bedragen tellen op; ref werpt op de optelling zelf
> een typefout. Geen enkele uitvoerder draagt deze vorm (§5.8.2/§4.20)

> **ordenend over een DATUM** — . De ordenende lezing KIEST één
> bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd
> — ref en de ASP-backend dragen haar, getuige
> golden/jutterswet-vondstbereik — maar deze tak heeft haar niet gebouwd.
> Een gat in deze backend, geen semantisch verschil (§5.8.3)

> **ordenend over elk ander niet-getalachtig type** — . De ordenende
> lezing vraagt een ORDENING op de leden en dit type heeft er geen: geen
> enkele uitvoerder draagt de vorm, en de verifier laat het document niet
> eens door (WF-038 eist een getal of een datum onder een ordenende
> vouw). Deze weigering staat als tweede verdedigingslinie (§5.8.3)

Dat onderscheid is de hele reden dat de reden gesplitst is: wie een
poortrapport leest moet "niemand draagt dit" van "wij dragen dit niet"
kunnen onderscheiden. **De derde zin is er omdat de tweede anders liegt.**
De staart werd eerst op de SOORT gekozen, en dan kreeg een TEKST-bron
onder MINIMUM de datumzin ("is over een datumbron welgedefinieerd — ref
en de ASP-backend dragen haar") terwijl gemeten NIEMAND haar draagt: ref
werpt `unordered comparison: "bb" vs "aa"`, clingo weigert, en de
verifier wijst het document af onder WF-038. Een weigering die verkeerd
diagnosticeert wijst haar lezer naar de verkeerde kaart, en dat is de
hele prijs van een foute reden.

**Rood eerst, gemeten op de jar van vóór de wijziging:**

| vorm | exec | `--drl` |
|---|---|---|
| SOM / TOTAAL over `Datum in dagen` | `ERROR ClassCastException: LocalDate → BigDecimal` | `EXCLUDE drl compile: … Unable to Analyse Expression` |
| MINIMUM / MAXIMUM over `Datum in dagen` | `ERROR … kan accumulate-resultaat niet naar BigDecimal omzetten: LocalDate` | idem |
| SOM over `Tekst` (verifier-SCHOON) | `ERROR ClassCastException: String → BigDecimal` | idem |

Alle drie zijn verifier-schone documenten, en geen van de zes uitkomsten
is een weigering: een crash noemt geen vorm, en een DRL-compilefout noemt
de gegenereerde tekst in plaats van het construct.

**De tekstbron is een vondst van deze kaart, en zij hangt aan de SOORT.**
Een volledig `Tekst`-getypeerd document komt langs WF-091 (doel, vouw en
lid zijn alle drie tekst), maar dan beslist WF-038: onder een ORDENENDE
vouw wijst de verifier het af (`operand of the MINIMUM ordering fold is
text, must be a number or a date`), onder SOM en TOTAAL is het
verifier-PASS. De verifier-SCHONE tekstvorm is dus de OPTELLENDE, en die
gaf dezelfde crash met `java.lang.String`. Omdat de Drools-arm op "niet
getalachtig" sluit in plaats van op "datum", is die tweede crash in
dezelfde regel dicht. De ASP-kant heb ik daar NIET verbreed: die weigert
een tekstbron al luid, zij het met een bodemzin
(`a text/enum/boolean operand here is …`, regel 162 van de 36-lijst). Dat
is geen fout getal en dus niet het defect van deze kaart; het is een
vervolgvoorstel in §9.

**Twee mutatieproeven, elk uitgevoerd en teruggedraaid in één keten.**

1. *De wacht zelf weg* — `if (true) return null;` bovenin
   `Vouwwaarde.weigering`, `mvn package`: `DatumSetvouwWitnessTest` valt
   om met 3 van de 4 rijen FAILURE (de versie van vóór de fixronde), elk
   met de crash uit de tabel hierboven eronder — de ordenende rij met
   `IllegalStateException … LocalDate`, de tekstrij met `… String`, de
   optellende rij met `Unexpected exception type thrown, expected:
   CaseRunner.Excluded but was: ClassCastException`. De getalbronrij
   blijft groen, zoals hij hoort.
2. *De staartkeuze terug op de SOORT* — `if (dt.isDatum())` vervangen
   door `if (true)`, `mvn package`: precies ÉÉN rij valt om,
   `de_ordenende_tekstbron_krijgt_de_derde_zin`, en alleen die — de
   andere vier blijven groen, wat laat zien dat de mutant alleen de
   DIAGNOSE raakt en niet de weigering.

Beide teruggedraaid, `mvn package`: **519 tests, 0 failures, 0 errors,
0 skipped**, build rc=0.

### 4. De getuigen

**`corpus/golden/jutterswet-vondstbereik/`** — MINIMUM/MAXIMUM over een
datum-setbron, handgeschreven IR zonder `bron.rgs`, bewust de
soort-tweeling van `jutterswet-vondstvenster`: dezelfde typen, dezelfde
relatie, dezelfde twee casussen met dezelfde leden en dezelfde
handberekende datums. De twee goldens naast elkaar lezen ÍS de claim van
ch. 1.2e dat eerste/laatste min/max onder een andere naam zijn.

Handberekend, `case1-bereik` (9 verwachtingen):

| Jutter | vondsten | vroegste | jongste |
|---|---|---|---|
| J1 | 2024-01-15, 2024-03-01, leeg | **2024-01-15** | **2024-03-01** |
| J2 | (geen) | **leeg** | **leeg** |
| J3 | één vondst zonder datum | **leeg** | **leeg** |
| J4 | 2023-12-31 | **2023-12-31** | **2023-12-31** |

`case2-volgorde` (3 verwachtingen): J5 met V6 2025-06-30, V7 2025-02-14,
V8 2025-11-01, V9 2025-02-14 in die schrijfvolgorde ⇒ vroegste
**2025-02-14**, jongste **2025-11-01**. Het eerste lid (2025-06-30) is
geen van beide uitersten en het laatste lid (2025-02-14) is de vroegste
in plaats van de jongste, dus een positionele lezing faalt op BEIDE
verwachtingen. V9 dupliceert de vroegste waarde zodat het gelijkspel
meeloopt; de vouw geeft een waarde en geen lid, dus welk lid "wint" is
onwaarneembaar. Dagnummers ter controle: 19722, 19737, 19783, 20133,
20269, 20393 — de datums ordenen zoals hun dagnummers, wat `#min`/`#max`
hier getrouw maakt terwijl `#sum` over dezelfde coderingen dat niet is.

`ref` ≡ clingo exact op beide casussen; beide falen onder `--noop`
(geen overleverregel nodig).

**`corpus/rood/datumsetbron-somvouw/`** en
**`corpus/rood/datumsetbron-totaalvouw/`** — `vondstvenster` met de soort
verwisseld, casus J1 met twee vondsten. `expectedFault.ref` =
`plus: expected a number, got 2024-01-15`, `expectedFault.clingo` = de
volle zin uit §2. **De twee leden dragen dezelfde datum met opzet:** de
ref-pin loopt door tot de waarde, en `add` leest zijn twee operanden in
een volgorde waar een getuige niet van mag afhangen; met gelijke leden is
de staart dezelfde welke het ook is. Twee mappen in plaats van twee
casusbestanden omdat de twee soorten op precies één clausule uiteenlopen
— de lege verzameling — en een getuige die één zin pint niets zegt over
de andere.

**Suite-getuigen**, `backends/clingo/test/test_clingo.ml`: 461 → 470
checks. Vier daarvan pinnen MINIMUM/MAXIMUM op HETZELFDE frame waar
EERSTE/LAATSTE al gepind staan (ref én clingo, elk apart), twee de
weigeringen per soort, en drie de asymmetrie: clingo weigert de lege
datum-SOM waar `ref` leeg antwoordt (één document, één casus, beide
kanten apart gecontroleerd), en `ref` geeft bij PRECIES ÉÉN datumlid dat
lid ongewijzigd terug. Die laatste is de scherpste van de drie: hij laat
zien dat de grens niet bij "leeg" ligt maar bij "minder dan twee
bijdragers".

**Suite-getuigen Drools**, `DatumSetvouwWitnessTest` (5 rijen, beide
takken per rij via `BeideTakken`): de ordenende weigering met haar zin,
de optellende weigering met de hare voor SOM én TOTAAL, de tekstbron
onder de OPTELVOUW (de verifier-schone tekstvorm), de tekstbron onder de
ORDENENDE vouw die de derde zin pint — expliciet gelabeld als tweede
verdedigingslinie, want WF-038 wijst dát document af — en de
smalheidsproef: `jutterswet-muntpiek` (MINIMUM/MAXIMUM over een
`kg`-bron) blijft in beide takken SPELEN. Zonder die laatste rij zou een
arm die élke waardevouw weigert alle andere rijen groen laten.

### 5. EERSTE/LAATSTE over een datumbron — gewogen, niet meegedekt

De opdracht vroeg dit expliciet te wegen. Antwoord: **de nieuwe golden
dekt ze niet en de nieuwe weigering raakt ze niet**, en dat is juist.

- In `ref` en clingo rijden ze sinds de setvouw-kaart op de min/max-arm
  en zijn ze gedekt door `jutterswet-vondstvenster` — dezelfde casussen,
  dezelfde absolute datums. De nieuwe golden is hun soort-tweeling, dus
  samen pinnen de twee goldens de gelijkheid.
- In Drools bereiken ze de accumulate-arm NIET: `isAggregatieKind` laat
  EERSTE/LAATSTE niet toe, dus ze vallen eerder uit. De exec-tak meldt
  `regel (waarde buiten scope)`, de DRL-tak `TODO aggregatie EERSTE`.
  Dat is geen crash — het is een weigering, al is de exec-zin generiek —
  en daarom blijft ze ongewijzigd. Verbreden zou de weigering uit een arm
  halen die hem niet stelt.
- Gevolg: over een datum-setbron weigert Drools nu alle ZES vouwsoorten,
  vier bij naam met ledentype en reden, twee op hun bestaande grond.

### 6. Corpussweep

Beide takken over alle 224 goldens, met de jar van vóór en die van ná,
regel voor regel gedift — inclusief de volledige redentekst, niet alleen
het statustoken.

| | exec PLAYED | drl PLAYED | exec EXCLUDE | drl EXCLUDE |
|---|---|---|---|---|
| vóór | 172 | 149 | 45 + 1 ERROR | 71 |
| ná | 172 | 149 | 46 | 72 |

**Precies één regel beweegt in elke tak**, en het is de nieuwe golden:
`ERROR ConsequenceException …` → `EXCLUDE transpiler SKIP … vouw
MINIMUM …` in de exec-tak, `EXCLUDE drl compile: … Unable to Analyse
Expression` → `EXCLUDE drl-transpiler MARKER … vouw MINIMUM …` in de
DRL-tak. Geen enkele andere golden wisselt van kanaal en geen enkele
bestaande reden verandert van tekst.

### 7. De veertien poorten, los gedraaid

*Alle veertien opnieuw gedraaid na de fixronde van 2026-09-20 — de
fixronde raakte `translate.ml`, dus een deelverzameling zou hier niet
volstaan. De getallen hieronder zijn die van de tweede ronde.*

| # | poort | exit | kerngetallen |
|---|---|---|---|
| 1 | `check-self-contained.sh` | 0 | — |
| 2 | `check-ir-schema.sh` | 0 | 265 bestanden valideren + round-trippen, vloer 192; 24 self-testrijen |
| 3 | `check-verifier.sh` | 0 | 265 golden schoon; 420 negatief afgewezen, vloer 350 |
| 4 | `check-case-schema.sh` | 0 | 613 casusbestanden |
| 5 | `check-oracle.sh` | 0 | ref-suite 755 (vloer 489); 572 casussen exact (vloer 427) |
| 6 | `check-clingo.sh` | 0 | suite **470** checks (was 461), vloer 83; 572 casussen exact |
| 7 | `check-noop.sh` | 0 | 536 casussen weerleggen een niets-doener; 36 overlevers |
| 8 | `check-rood.sh` | 0 | **41** rode casussen (was 39), vloer 26; **82** gepinde fragmenten citeren geen van de 36 bodemzinnen; Drools-been 9 fault-pinned / **32** niet-gedragen met reden (was 30) |
| 9 | `check-catala.sh` | 0 | 201 checks, vloer 201; 12 catala-goldens |
| 10 | `check-regelspraak.sh` | 0 | 338 tests, vloer 173; 64 rgs-goldens, vloer 38 |
| 11 | `check-toka-parity.sh` | 0 | ref 29/29, clingo 29/29, Drools 29 PLAYED |
| 12 | `check-drools.sh` | 0 | played **172** = pin 172; kanaalvloer **52** niet-spelers bij naam (was 51); DRL-tak played **149** = pin 149, DRL-vloer **75** (was 74); waardedivergentie 1; no-case 2; JUnit **519** tests, vloer 239, 0 skipped |
| 13 | `check-reftype-derivation.sh` | 0 | 131 twists over 27 goldens, vloer 74; 128 replays, 25 DRL-gespeeld, vloer 13 |
| 14 | `check-flogic.sh` | 0 | 130 checks, vloer 81; 2 flogic-goldens |

Geen enkele telvloer is aangeraakt: alle nieuwe getallen groeien.
`EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` bewegen NIET — een nieuwe
golden die niet speelt is nooit meegeteld — en dat is gemeten, niet
aangenomen.

**Beschermd-padvoorstellen in deze tak** (puur additief, niets herordend,
geen bestaande reden herschreven):

- `scripts/check-rood.sh`: twee regels in de Drools-uitsluitingslijst, op
  hun alfabetische plaats tussen `dagsoortverwijzing-andere-dag` en
  `decimal-nonterminating`.
- `scripts/check-drools.sh`: één regel in `EXPECTED_CHANNEL` (vóór
  `jutterswet-vondsttarief`), één in `DRL_CHANNEL` groep (C) (vóór
  `jutterswet-vondstvenster`), en de kopregel van groep (C) van 11 naar
  12 en van "EIGHT of the eleven" naar "NINE of the twelve" — dat getal
  moet mee of de kop liegt.

### 8. Registervoorstellen (niet zelf geland)

**`spec/CONSTRUCTS.md`, rij 1.** Aan het blok dat begint met
"**Landed (2026-09-19, kaart eerste-laatste-setvouw-kloof)**" toevoegen:

> **Landed (2026-09-20, kaart datum-setbron-breuk):** de DATUM-setbron
> splijt op ORDENEN tegen OPTELLEN, en die grens is nu in alle vier de
> uitvoerders dezelfde. De ordenende vouwen (minimum, maximum, eerste,
> laatste) DRAGEN een datumbron in `ref` en in de ASP-backend — één
> bijgedragen waarde eruit, dus een dagnummer erin is hetzelfde dagnummer
> eruit — getuige `jutterswet-vondstbereik`, de soort-tweeling van
> `jutterswet-vondstvenster` met dezelfde casussen en dezelfde
> handberekende datums. De optellende vouwen (som, totaal) worden door
> NIEMAND gedragen: **vanaf twee gevulde bijdragers** werpt `ref` op de
> optelling zelf `plus: expected a number`, en de ASP-backend — die tot
> deze kaart 19737 + 19783 = 39520 antwoordde onder een verifier-SCHOON
> document — weigert de vorm nu bij naam in de vertaling, met de soort,
> de bron en het ledentype in de zin. Getuigen
> `corpus/rood/datumsetbron-somvouw` en
> `corpus/rood/datumsetbron-totaalvouw`. De kwalificatie is gemeten en
> niet cosmetisch: `ref` vouwt met `fold_left add first rest`, dus onder
> twee bijdragers wordt er niet opgeteld en antwoordt zij — bij precies
> ÉÉN lid dat lid zelf (een DATUM, welgetypeerd) en bij een LEGE bron de
> som leeg maar de totaal **0**, een getal in een datumslot. De
> ASP-weigering hangt aan de DECLARATIE en geldt daar ook, dus op die twee
> randen lopen de OCaml-uitvoerders uiteen; de richting is de
> onschadelijke en beide randen staan in de NOTE van de totaal-getuige.
> De ASP-arm matcht `Datum in dagen` EXACT: een datum-tijd setbron wordt
> één knoop eerder door de datum-tijd-arm geweigerd en bereikt deze nooit
> (gemeten 2026-09-20). De Drools-backend draagt geen van de zes soorten
> over een datumbron, maar CRASHTE op vier ervan (ClassCastException,
> accumulate-cast, en in de DRL-tak een compilefout op gegenereerde
> tekst) waar zij nu bij naam weigert, uit één gedeelde redenfunctie die
> DRIE klassen apart benoemt: "niemand draagt dit" (optellend), "deze
> backend draagt dit niet" (ordenend over een datum, waar ref en clingo
> wél dragen) en "niemand draagt dit en de verifier laat het document
> niet eens door" (ordenend over elk ander niet-getalachtig type, WF-038).
> Dezelfde arm sluit de gemeten TEKST-bron, die op dezelfde plek crashte.
> Er is voor geen van de vier soorten een producentroute — de frontend
> typeert ook `minimale waarde van` over een datumbron weg — dus de golden
> is handgeschreven wire. Gate 12: `EXPECTED_PLAYED` onveranderd 172,
> `EXPECTED_DRL_PLAYED` onveranderd 149, beide kanaallijsten één regel
> langer (52 en 75 bij naam gepinde niet-spelers).

**`spec/ASP-TRANSLATION.md` S8** (beschermd pad, voorstel). In de alinea
**Sum** (r. ±950), na de zin over de fallback-anders-dan-0, toevoegen:

> A set source whose ELEMENT ATTRIBUTE is declared a DAY-TYPED date
> (`Datum in dagen`) is refused BY NAME, naming the kind, the source and
> the member datatype, before anything grounds: the additive fold is
> arithmetic and the day number this encoding gives a date is a POSITION,
> not an amount, so `#sum` would answer a definite number no calendar
> reads back (measured 2026-09-19 on a verifier-clean document:
> 19737 + 19783 = 39520, where `ref` faults on the addition itself). The
> arm matches that ONE datatype and not dates in general, because that is
> its whole reach: a datum-tijd set source is routed through `dt_term` one
> node earlier and refused there (measured 2026-09-20), so a wider match
> would claim reach this position does not have. The question is asked of
> the DECLARATION on the element type (§2.10), so it does not depend on
> what a case supplies: a set with FEWER THAN TWO filled contributors is
> refused too, where `ref` answers without ever performing an addition —
> the single member itself at one contributor, and leeg (som) or 0
> (totaal) at none. Those two edges are where the two executors part;
> both are recorded in `corpus/rood/datumsetbron-totaalvouw`. Witnesses
> `corpus/rood/datumsetbron-somvouw` and its TOTAAL twin.

En in de alinea **Minimum / Maximum / Eerste / Laatste**, na de zin over
de presence guard, toevoegen:

> A DATE element attribute is carried here and refused at the som: these
> folds RETURN one contributed value, so a day number goes in and the
> same day number comes out, and the ordering over day numbers IS the
> ordering over dates. Witness `jutterswet-vondstbereik` (hand-computed
> absolute dates; the same two cases as `jutterswet-vondstvenster`, whose
> EERSTE/LAATSTE are the same fold under another name).

**Geen normatieve MUST beweegt.** WF-091 en WF-096 blijven woord voor
woord staan; wat veranderde is welke uitvoerder wat doet met een vorm die
beide toelaten.

### 9. Open twijfel

- **Dezelfde regel wordt één arm verder nóg geschonden, en die is
  gekaart.** De §7.1 EIGEN-SUBJECT `TOTAAL` over een TIJDLIJN met
  DATUM-waarden loopt niet door de setarm maar door de tijdlijnarm, en
  daar staat het gat open: verifier PASS, `ref` werpt `maal: expected a
  number, got 2024-01-15`, en de ASP-backend **antwoordt 19737** — het
  dagnummer, want de factor "hele roostereenheden" is 1 bij één
  MAAND-segment. Drools exec weigert daar al bij naam (`non-numeric
  timeline value … is not carried`), de DRL-tak valt op compile. Gemeten
  2026-09-20 in de adviesronde, niet veroorzaakt door deze kaart en niet
  erdoor geraakt; `backlog/new/20260920-temporele-datumvouw.md` draagt de
  meting.
- **`ref`'s TOTAAL over een LEGE datum-setbron schrijft 0 in een
  datumslot.** Gemeten, §4.20-conform, en daarmee dezelfde klasse defect
  als het gat dat deze kaart dichtte, één casus verder. `ref` is de
  arbiter en buiten scope; dit is een kaart van de arbiter zelf.
  **Het is NIET uitvoerbaar gepind, en dat is een meting en geen
  verzuim:** de waarde die `ref` daar berekent is niet als verwachting op
  te schrijven — de casuslezer wijst `{"number": "0"}` tegen een `Datum
  in dagen`-declaratie af met `value variant "number" does not match the
  declared datatype`. Wat wél gepind is, staat in de ASP-suite: de
  SOM-helft (clingo weigert, `ref` antwoordt leeg, één document) en de
  SINGLETON-rand (`ref` geeft het ene lid terug). De 0-rand staat alleen
  in `corpus/rood/datumsetbron-totaalvouw/NOTE.md`, bewust en met reden.
- **De ASP-weigering voor een TEKST-setbron is een bodemzin, en die voor
  een DATUM-TIJD-setbron ook.** Beide weigeren (geen fout getal), maar
  `a text/enum/boolean operand here is an own-subject attribute read or a
  literal` is regel 162 van gate 8's 36-lijst en `datum-tijd here is not
  the pair term's arithmetic. Carried: …` is regel 167: beide zinnen
  noemen wat de backend WEL draagt. De Drools-kant heeft beide vormen nu
  wel bij naam (gemeten: `van type Tekst`, `van type Datum-tijd`).
  Voorstel voor een vervolgkaart: `symbolic_term` en `dt_term` krijgen
  elk een benoemde arm voor de niet-numerieke setbron onder een
  waardevouw, met een rode getuige per arm — dezelfde cure die deze fence
  altijd voorschrijft.
- **De exec-tak weigert EERSTE/LAATSTE met een generieke zin**
  (`regel (waarde buiten scope)`) terwijl de DRL-tak de soort noemt. Geen
  poortgevolg (beide zijn EXCLUDE met reden in de kanaallijst), maar het
  is de enige plek waar de twee takken op deze vorm verschillend
  diagnosticeren.
- **De verbreding van de Drools-arm naar "niet getalachtig" is breder dan
  de kaart vroeg.** Bewust: het is de grens die de accumulate ZELF stelt,
  en de tekstbron was dezelfde crash. Gemeten over alle 224 goldens dat
  geen enkele bestaande golden erdoor beweegt, en de smalheidsrij in de
  suite bewaakt de andere kant.
