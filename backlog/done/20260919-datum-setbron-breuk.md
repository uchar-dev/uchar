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

**Waarom de weigering over het DOCUMENT gaat en niet over de casus.** Op
een LEGE bijdragenverzameling bereikt `ref` de optelling nooit: zij
antwoordt leeg (SOM) respectievelijk **0** (TOTAAL, §4.20) — een getal in
een `Datum in dagen`-slot. De vertaling weigert ook daar. Dat is de ene
gemeten asymmetrie van deze kaart, hij staat in
`corpus/rood/datumsetbron-totaalvouw/NOTE.md` en in de suite gepind, en
de richting is de onschadelijke: een weigering tegenover een antwoord dat
geen hoofdstuk definieert. `ref` corrigeren is de arbiter zelf en buiten
scope.

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

en de staart is per klasse verschillend, want het zijn twee verschillende
beweringen:

> **optellend** — . Optellen is rekenkunde en alleen bedragen tellen op;
> ref werpt op de optelling zelf een typefout, want een datum is een
> kalenderPOSITIE en de som van twee posities is er geen. Geen enkele
> uitvoerder draagt deze vorm (§5.8.2/§4.20)

> **ordenend** — . De ordenende lezing KIEST één bijgedragen waarde
> (ch. 1.2e) en is over een datumbron welgedefinieerd — ref en de
> ASP-backend dragen haar, getuige golden/jutterswet-vondstbereik — maar
> deze tak heeft haar niet gebouwd. Een gat in deze backend, geen
> semantisch verschil (§5.8.3)

Dat onderscheid is de hele reden dat de reden gesplitst is: wie een
poortrapport leest moet "niemand draagt dit" van "wij dragen dit niet"
kunnen onderscheiden.

**Rood eerst, gemeten op de jar van vóór de wijziging:**

| vorm | exec | `--drl` |
|---|---|---|
| SOM / TOTAAL over `Datum in dagen` | `ERROR ClassCastException: LocalDate → BigDecimal` | `EXCLUDE drl compile: … Unable to Analyse Expression` |
| MINIMUM / MAXIMUM over `Datum in dagen` | `ERROR … kan accumulate-resultaat niet naar BigDecimal omzetten: LocalDate` | idem |
| SOM over `Tekst` (verifier-SCHOON) | `ERROR ClassCastException: String → BigDecimal` | idem |

Alle drie zijn verifier-schone documenten, en geen van de zes uitkomsten
is een weigering: een crash noemt geen vorm, en een DRL-compilefout noemt
de gegenereerde tekst in plaats van het construct.

**De tekstbron is een vondst van deze kaart.** De kaart gaat over datums;
de meting liet zien dat een volledig `Tekst`-getypeerd document
verifier-PASS is (WF-091 valt weg zodra doel, vouw en lid alle drie tekst
zijn) en dezelfde crash geeft met `java.lang.String`. Omdat de Drools-arm
op "niet getalachtig" sluit in plaats van op "datum", is die tweede crash
in dezelfde regel dicht. De ASP-kant heb ik daar NIET verbreed: die
weigert een tekstbron al luid, zij het met een bodemzin
(`a text/enum/boolean operand here is …`, regel 162 van de 36-lijst). Dat
is geen fout getal en dus niet de defect van deze kaart; het is een
vervolgvoorstel in §8.

**Mutatieproef, uitgevoerd en teruggedraaid in één keten.** Guard uit
(`if (true) return null;` bovenin `Vouwwaarde.weigering`), `mvn package`:
`DatumSetvouwWitnessTest` valt om met 3 van de 4 rijen FAILURE, elk met
de crash uit de tabel hierboven eronder — de ordenende rij met
`IllegalStateException … LocalDate`, de tekstrij met `… String`, de
optellende rij met `Unexpected exception type thrown, expected:
CaseRunner.Excluded but was: ClassCastException`. De vierde rij (de
getalbron) blijft groen, zoals hij hoort. Teruggedraaid, `mvn package`:
**518 tests, 0 failures, 0 errors, 0 skipped**, build rc=0.

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

**Suite-getuigen**, `backends/clingo/test/test_clingo.ml`: 461 → 468
checks. Vier daarvan pinnen MINIMUM/MAXIMUM op HETZELFDE frame waar
EERSTE/LAATSTE al gepind staan (ref én clingo, elk apart), twee de
weigeringen per soort, en één dat de weigering aan het document hangt en
niet aan de casus (lege bron, toch geweigerd).

**Suite-getuigen Drools**, `DatumSetvouwWitnessTest` (4 rijen, beide
takken per rij via `BeideTakken`): de ordenende weigering met haar zin,
de optellende weigering met de hare voor SOM én TOTAAL, de tekstbron, en
de smalheidsproef — `jutterswet-muntpiek` (MINIMUM/MAXIMUM over een
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

| # | poort | exit | kerngetallen |
|---|---|---|---|
| 1 | `check-self-contained.sh` | 0 | — |
| 2 | `check-ir-schema.sh` | 0 | 265 bestanden valideren + round-trippen, vloer 192; 24 self-testrijen |
| 3 | `check-verifier.sh` | 0 | 265 golden schoon; 420 negatief afgewezen, vloer 350 |
| 4 | `check-case-schema.sh` | 0 | 613 casusbestanden |
| 5 | `check-oracle.sh` | 0 | ref-suite 755 (vloer 489); 572 casussen exact (vloer 427) |
| 6 | `check-clingo.sh` | 0 | suite **468** checks (was 461), vloer 83; 572 casussen exact |
| 7 | `check-noop.sh` | 0 | 536 casussen weerleggen een niets-doener; 36 overlevers |
| 8 | `check-rood.sh` | 0 | **41** rode casussen (was 39), vloer 26; **82** gepinde fragmenten citeren geen van de 36 bodemzinnen; Drools-been 9 fault-pinned / **32** niet-gedragen met reden (was 30) |
| 9 | `check-catala.sh` | 0 | 201 checks, vloer 201; 12 catala-goldens |
| 10 | `check-regelspraak.sh` | 0 | 338 tests, vloer 173; 64 rgs-goldens, vloer 38 |
| 11 | `check-toka-parity.sh` | 0 | ref 29/29, clingo 29/29, Drools 29 PLAYED |
| 12 | `check-drools.sh` | 0 | played **172** = pin 172; kanaalvloer **52** niet-spelers bij naam (was 51); DRL-tak played **149** = pin 149, DRL-vloer **75** (was 74); waardedivergentie 1; no-case 2; JUnit **518** tests, vloer 239, 0 skipped |
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
> NIEMAND gedragen: `ref` werpt op de optelling zelf `plus: expected a
> number`, en de ASP-backend — die tot deze kaart 19737 + 19783 = 39520
> antwoordde onder een verifier-SCHOON document — weigert de vorm nu bij
> naam in de vertaling, met de soort, de bron en het ledentype in de zin.
> Getuigen `corpus/rood/datumsetbron-somvouw` en
> `corpus/rood/datumsetbron-totaalvouw`. De Drools-backend draagt geen
> van de zes soorten over een datumbron, maar CRASHTE op vier ervan
> (ClassCastException, accumulate-cast, en in de DRL-tak een
> compilefout op gegenereerde tekst) waar zij nu bij naam weigert, uit
> één gedeelde redenfunctie die de twee klassen apart benoemt: "niemand
> draagt dit" tegenover "deze backend draagt dit niet". Dezelfde arm
> sluit de gemeten TEKST-bron, die op dezelfde plek crashte. Er is voor
> geen van de vier soorten een producentroute — de frontend typeert ook
> `minimale waarde van` over een datumbron weg — dus de golden is
> handgeschreven wire. Gate 12: `EXPECTED_PLAYED` onveranderd 172,
> `EXPECTED_DRL_PLAYED` onveranderd 149, beide kanaallijsten één regel
> langer (52 en 75 bij naam gepinde niet-spelers).

**`spec/ASP-TRANSLATION.md` S8** (beschermd pad, voorstel). In de alinea
**Sum** (r. ±950), na de zin over de fallback-anders-dan-0, toevoegen:

> A set source whose ELEMENT ATTRIBUTE is declared as a date is refused
> BY NAME, naming the kind, the source and the member datatype, before
> anything grounds: the additive fold is arithmetic and the day number
> this encoding gives a date is a POSITION, not an amount, so `#sum`
> would answer a definite number no calendar reads back (measured
> 2026-09-19 on a verifier-clean document: 19737 + 19783 = 39520, where
> `ref` faults on the addition itself). The question is asked of the
> DECLARATION on the element type (§2.10), so it does not depend on what
> a case supplies: an EMPTY member set is refused too, where `ref`
> answers leeg (som) or 0 (totaal) without ever reaching the addition —
> the one place the two part, recorded in
> `corpus/rood/datumsetbron-totaalvouw`. Witnesses
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

- **`ref`'s TOTAAL over een LEGE datum-setbron schrijft 0 in een
  datumslot.** Gemeten, §4.20-conform, en daarmee dezelfde klasse defect
  als het gat dat deze kaart dichtte, één casus verder. `ref` is de
  arbiter en buiten scope; dit is een kaart van de arbiter zelf.
- **De ASP-weigering voor een TEKST-setbron is een bodemzin.** Zij
  weigert (geen fout getal), maar `a text/enum/boolean operand here is an
  own-subject attribute read or a literal` is regel 162 van gate 8's
  36-lijst: de zin noemt wat de backend WEL draagt. De Drools-kant heeft
  die vorm nu wel bij naam. Voorstel voor een vervolgkaart: de
  `symbolic_term`-bodem krijgt een benoemde arm voor de niet-numerieke
  setbron onder een waardevouw, met een rode getuige — dezelfde cure die
  deze fence altijd voorschrijft.
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
