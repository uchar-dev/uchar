# date-with-in-ref-and-clingo: ref en clingo voeren de datumconstructie "de datum met jaar: …, maand: … en dag: …" niet uit

*Gemeten door een regelsetbouwer, 2026-10-02 (de kernfix na de eval-pilot van de regelset herleving-WW v2, buiten de
poorten), nagemeten door de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a` en ref en clingo
uit `_build`. Vastgelegd als open punt 5 van `done/20261008-tms-expressievormen-en-rekendatum.md`, zonder kaart. Luid;
geen fout antwoord.*

## Aanleiding

RUNTIME-SEMANTICS ch. 5.11 noemt bij `dateWith`/`dateTimeWith`: "ref declines the form; the order is stated, not yet
exercised". De Drools-tak rekent de vorm, zonder onafhankelijke bevestiging. De regelset herleving-WW gebruikt haar in
een uitzondering (de gewenste maand van een herlevingsverzoek), en kan sinds 2026-10-20 ook de subselectie van een vouw
met haar schrijven; ref en clingo kunnen die regels niet nalopen.

    Objecttype de Schuit (mv: Schuiten)
        de gewenste dag Datum in dagen;
        de gewenste maand Datum in dagen;

    Regel gewenste maand
        geldig altijd
            De gewenste maand van een Schuit moet gesteld worden op de datum met jaar: het jaar uit zijn gewenste dag, maand: de maand uit zijn gewenste dag en dag: 1.

    Casus: gewenste dag 15-03-2026 → gewenste maand 01-03-2026.

    nu: check exit 0; Drools PASS; ref "EVALUATION FAULT: REF-007: function kind not implemented in this card";
        clingo "TRANSLATION FAULT: CLINGO-008: expression kind not translated (no corpus case exercises it)".

## Doel

ref en clingo voeren `dateWith` (en `dateTimeWith`) uit volgens RUNTIME-SEMANTICS ch. 2–3; waar de spec voor een datum
die niet bestaat (dag 31 in een maand van 30 dagen) nog niets zegt, eerst die regel. De getuige geeft 01-03-2026 in ref ≡
clingo ≡ Drools, als golden; de regel "ref declines the form" in ch. 5.11 vervalt; alle gates groen.

## Voorlopig besluit van de bouwer (2026-10-22): een datum die niet bestaat

*Voorlopig: genomen door de bouwer van deze kaart, niet door de eigenaar. De spec zei er niets over; de regel staat als
voorstel in RUNTIME-SEMANTICS ch. 3.8 op de kaarttak.*

**De regel.** `dateWith` neemt drie delen (jaar, maand, dag), `dateTimeWith` zeven (plus uur, minuut, seconde,
milliseconde). Elk deel is een geheel getal zonder eenheid; een deel met een breuk of een eenheid is een fout, nooit een
afkapping (zoals bij het paasjaar, ch. 3.8). De uitkomst is de burgerlijke datum van de proleptische gregoriaanse
kalender als de delen er een noemen: maand 1..12, dag 1..de lengte van die maand (schrikkeljaren meegerekend), en bij
`dateTimeWith` uur 0..23, minuut 0..59, seconde 0..59 (geen schrikkelseconde) en milliseconde 0..999, met het tijdstip
binnen het representeerbare millisecondebereik. **Noemen de delen geen dag of geen kloktijd, dan is dat een fout**:
nooit een doorrol naar de volgende maand (31 april is niet 1 mei), nooit een klem op het maandeinde (niet 30 april; de
klem van ch. 3.8 hoort bij datum plus duur, niet bij de constructie), nooit leeg. Een leeg deel geeft leeg (ch. 1.2);
alle delen worden eerst in draadvolgorde uitgerekend (ch. 5.11), daarna de leegtoets, daarna de geldigheid. Een jaar
buiten het machinegetal van de executor is een fout, nooit een omslag.

**Waarom zo.** De spec kent op drie plaatsen dezelfde houding: een datumlexeem van de invoer dat geen dag noemt, is een
fout (EF-003, `2026-02-30`); een datumstap buiten het bereik is een fout, "never a silent wrap-around" (EF-007); een
paasjaar met een breuk of een eenheid is een fout, "never a silent truncation". Een doorrol of klem zou een datum
opleveren die de schrijver niet schreef.

**De code.** Geen bestaande EF-soort past: EF-003 is een lexeem van de invoer op het kanaal `case input` met de
parameters `field` en `lexeme`, deze fout ontstaat bij de evaluatie uit uitgerekende delen; EF-007 is een stap. Een
nieuwe soort krijgt volgens ch. 5.7b haar code samen met haar rode geval, en dat rode geval kan op deze tak niet groen
landen: het Drools-been van gate 8 eist van een geval dat niet in zijn uitsluitingslijst staat een `FAULT` met de
vastgepinde EF-code, en de Drools-backend geeft deze fout zonder code (meting hieronder). Een vermelding in de
uitsluitingslijst helpt niet (een `FAULT` op een vermeld geval faalt als verouderd), en `backends/drools/` en de
gatescripts vallen buiten deze kaart. Dus: de fout is normatief en zonder code ("Kinds without a code", ch. 5.7b); ref en
clingo geven haar met een eigen zin; de ref-voorbeelden van ch. 3.8 pinnen haar. Het rode geval
(`corpus/red/date-with-no-calendar-day`, voorgestelde code EF-032, parameters `year`, `month`, `day`) ligt klaar als
vervolg, samen met de regel in ch. 5.7b, voor de kaart die de Drools-backend de code laat geven.

*Bijgewerkt in de fixronde van 2026-10-22 (mandaat verbreed met `backends/drools/**`): de soort heeft nu de code
EF-032, in alle drie de executors, en het rode geval staat in `corpus/red/`. Zie "Fixronde" in het Result.*

**Meting Drools** (shaded jar van `d6015ecf`, `DroolsRedMain` en de gewone replay, 2026-10-22; model met de delen als
attributen, één regel `dateWith` en één `dateTimeWith`):

| delen | Drools |
|---|---|
| 2026-03-01 10:30 | 2026-03-01 en 2026-03-01T10:30 |
| 2026, 4, 31 | FAULT, `dateWith: Invalid date 'APRIL 31'` (ComputeFault, geen EF-code) |
| 2026, 2, 29 | FAULT, `Invalid date 'February 29' as '2026' is not a leap year` |
| 2024, 2, 29 | 2024-02-29 |
| 2026, 13, 1 / 2026, 0, 1 | FAULT, `Invalid value for MonthOfYear (valid values 1 - 12): 13` / `: 0` |
| 2026, 3, 0 / 2026, 3, −1 | FAULT, `Invalid value for DayOfMonth (valid values 1 - 28/31): 0` / `: -1` |
| uur 24, minuut 60, seconde 60, milliseconde 1000 | FAULT, `Invalid value for HourOfDay (valid values 0 - 23): 24` enz. |
| jaar 0, jaar −5, jaar 10000 | 0000-03-01, -0005-03-01, +10000-03-01 |
| jaar 1 000 000 000 | FAULT, `Invalid value for Year (valid values -999999999 - 999999999)` |
| jaar 200 000 000 | +200000000-03-01 en +200000000-03-01T00:00 (de datum-tijd ligt buiten het millisecondebereik) |

Drools volgt de regel dus op elke dag die niet bestaat (een fout, geen doorrol of klem); het wijkt alleen af in de vorm:
zijn fout draagt geen EF-code (`FAULT EvaluationFaults: …` of `FAULT ComputeFault: …`), en een datum-tijd boven het
millisecondebereik (jaar ruim boven 146 miljoen) controleert het niet.

## Result (2026-10-22)

Tak `card/date-with-in-ref-and-clingo`, vanaf `d6015ecf`, ongemerged en ongepusht, samen met de kaart
`clingo-nested-aggregate-in-fold-filter`. Beschermde paden als voorstel geraakt: `spec/RUNTIME-SEMANTICS.md`,
`spec/ASP-TRANSLATION.md` en in `scripts/check-drools.sh` alleen `EXPECTED_PLAYED`. De kaart blijft in `doing/`; de
hoofdsessie verplaatst haar bij de landing en landt CONSTRUCTS (tekst hieronder).

- `f9a29eca` beide kaarten naar `doing/`.
- `b8e9b013` het voorlopige besluit over een datum die niet bestaat, met de Drools-meting (hierboven).
- `eca8a497` rood eerst: de golden `date-with-parts` (met de twee goldens van de andere kaart), `EXPECTED_PLAYED` 264 → 267.
- `5c807c3a` ref: `Arith.date_construction` (`ref/lib/arith.ml`), de arm in `eval.ml`, 21 voorbeelden `WV-3.8-datum…`
  en het paar `WV-5.11-datummet-deel0-eerst`/`-deel1-later`.
- `276e6d79` clingo: `date_construction` in `translate.ml`, de arm in `expr_term` (`dateWith`) en in `dt_term`
  (`dateTimeWith`); de zin van CLINGO-009 noemt de constructor niet meer als flank; in de suite tien getuigen van een
  dag of kloktijd die niet bestaat (en van jaar 0) en twee waarden in ref en clingo.
- `ffb6d9c2` het spec-voorstel (ch. 1.2, 3.8, 5.7b, 5.11; ASP-TRANSLATION S6a en S19).
- `5d774f2d` en `b9436dc8` twee vondsten, op verzoek van de eigenaar direct op deze tak gefixt (zie Vondsten).
- dit Result.

### Gates

`gates-1` op `ffb6d9c2`: EXIT 0, alle 17 groen (`werk/bruggen/date-with/gates-1.summary.txt`). Daarna kwamen de twee
fixes van vondsten; `gates-2` op `b9436dc8`: EXIT 0, alle 17 groen (`werk/bruggen/date-with/gates-2.summary.txt`):
gate 5 1339 voorbeelden en 856 gevallen, gate 6 856 gevallen, gate 12 played 267 op beide takken, gate 17 267 goldens
en 727 casusbestanden vergeleken, 0 afwijkingen. Na `b9436dc8` veranderde alleen dit Result.

Tussendoor op `276e6d79`: gate 5 PASS (1334 voorbeelden, 856 gevallen), gate 6 PASS (2221 controles, 856 gevallen),
gate 8 PASS (46 rode gevallen), gate 12 `--fast` PASS (played 267).

### Rood eerst en de getuige

Op `eca8a497`: ref `EVALUATION FAULT: REF-007: function kind not implemented in this card` en clingo `TRANSLATION
FAULT: CLINGO-008` op alle drie de gevallen van `date-with-parts`; Drools `PLAYED 3` op beide takken. Na `276e6d79`
spelen ref en clingo ze ook: de getuige van de kaart, gewenste dag 15-03-2026 → gewenste maand 01-03-2026, is
`case1-gewenste-maand` (ref ≡ clingo ≡ Drools, beide takken). De golden draagt verder een datum-tijd
(2026-03-15T06:30:00 en 2026-12-31T23:30:00), de schrikkeldagen van 2024, 2000 en 2028, de leeghoek, en sinds
`b9436dc8` een datum van jaar 12026 in een voorwaarde.

### De regel en wat ref en clingo doen

De regel is het voorlopige besluit hierboven; RUNTIME-SEMANTICS ch. 3.8 ("Date construction") schrijft hem met de
uitgerekende voorbeelden. Gemeten op dezelfde delen als Drools:

| delen | ref | clingo |
|---|---|---|
| 2026, 4, 31 / 2026, 2, 29 / 2026, 13, 1 / 2026, 0, 1 / 2026, 3, 0 | `EVALUATION FAULT: dateWith: year 2026, month 4 and day 31 name no calendar day` (enz.) | `UNSATISFIABLE — … viol("dateWith: the parts name no calendar day","S1")` |
| uur 24, minuut 60, seconde 60, milliseconde 1000 | `dateTimeWith: hour 24, minute 0, second 0 and millisecond 0 name no clock time` (enz.) | `viol("dateTimeWith: the parts name no clock time","S1")` |
| 2024, 2, 29 | 2024-02-29 | dag 19782 (= 2024-02-29) |
| jaar 0 / jaar −5 | 0000-03-01 / -0005-03-01 | weigert luid: `dateWith of a year before 1 is not translated` |
| jaar 10000 | 10000-03-01 (sinds `b9436dc8` geschreven als +10000-03-01) | dag 2932957 (dezelfde) |
| jaar 200 000 000, datum-tijd | `dateTimeWith: the instant lies outside the representable millisecond range` | weigert luid: `… of a year after 5000000 is not translated` |
| jaar 5 000 000, 12, 31 | 5000000-12-31 | dag 1825493337 (dezelfde) |

Drools volgt de regel op elke dag of kloktijd die niet bestaat, maar zonder EF-code, en geeft een datum-tijd buiten het
millisecondebereik (+200000000-03-01T00:00).

### Wat ik niet deed, met reden

1. **Geen rood geval en geen EF-code.** Gate 8 eist van het Drools-been een `FAULT` met de vastgepinde code voor elk
   geval dat niet in zijn uitsluitingslijst staat, en een vermelding van een geval dat faalt is verouderd; de
   Drools-backend geeft deze fout zonder code, en `backends/drools/` en de gatescripts vallen buiten deze kaart. Een
   code komt volgens ch. 5.7b samen met haar getuige, dus de fout staat nu bij "Kinds without a code". Het rode geval
   ligt klaar in `werk/bruggen/date-with/red-date-with-no-calendar-day/` (vijf gevallen, IR, voorgestelde rij EF-032
   met parameters `year`, `month`, `day`, en wat Drools, ref en clingo dan moeten doen; README daar).
2. **Geen bereik op dagprecisie.** Het besluit van de eigenaar van 2026-10-09 (epochdagen ±53 375 995 583) hoort bij
   `datumrekening-slaat-stil-om`; ref begrenst het jaar hier waar zijn machinegetal ophoudt (|jaar| ≤ 2⁶²/400, de
   paasjaar-zin van ch. 3.8), clingo op 1..5 000 000.
3. **Geen `dateWith` in een dagsoortvoorwaarde in clingo.** De ASP-backend rekent een dagsoortvoorwaarde bij de
   vertaling uit en kent daar een genoemde deelverzameling van operanden (ASP-TRANSLATION S6a); `dateWith` hoort er niet
   bij en wordt daar met naam geweigerd, waar ref haar rekent. Een vorm die erbij kan, geen fout antwoord.

### Voorgestelde CONSTRUCTS-tekst

Rij "— `FunctionKind` (date/time)" (ch. 2.1), achteraan in de kolom met de landingen:

> **Landed (2026-10-22, card `date-with-in-ref-and-clingo`):** DATUM_MET/DATUMTIJD_MET (`dateWith`/`dateTimeWith`)
> execute in ref and clingo (RS ch. 3.8 "Date construction"; golden `date-with-parts`; ch. 5.11 pair
> `WV-5.11-datummet-…`). Parts that name no calendar day or no clock time are a fault, never a roll-over or a clamp
> (provisional decision of the card's builder); the kind has no EF code yet, its red case waits on the Drools backend
> reporting one. Delta to v0, in the Easter year's shape: a non-whole or unit-carrying part faults where v0's
> `rsDatumMet`/`rsDatumtijdMet` take `intValue()`. The ASP backend carries years 1..5 000 000 and refuses the rest
> loudly.

### Vondsten (buiten de kaart; de eerste twee op verzoek van de eigenaar hier gefixt)

1. **clingo, eerste paasdag van een groot jaar, stille omslag** (gefixt in `5d774f2d`). Jaar 6 000 000 gaf dagnummer
   −2104231732 waar ref 6000000-04-02 geeft: het dagnummer verlaat clingo's gehele getallen. Nu het jaarhek van de
   datumconstructie, `easterSunday of a year after 5000000 is not translated`. Getuige: een test in de clingo-suite;
   een golden of rood geval kan het niet dragen, want ref en Drools beantwoorden dat jaar en een casus kan de datum
   niet schrijven. ASP-TRANSLATION S6a noemt het hek.
2. **ref, eerste paasdag van een groot jaar, stille omslag** (gefixt in `5d774f2d`). Jaar 10¹⁷ gaf
   −1010939711066219-12-12: ref begrensde het jaar pas bij 2⁶², terwijl `days_from_civil` al vanaf ruim 10¹⁶ omslaat.
   Nu dezelfde grens als `dateWith` (|jaar| ≤ 2⁶²/400); de norm stond al in ch. 3.8 ("a whole year outside the
   executor's machine-integer range"). Getuigen: `WV-3.8-paasdag-jaar-buiten-bereik` en `WV-3.8-paasdag-groot-jaar`
   (10¹⁵ rekent nog). Geen rood geval: de soort heeft geen code, en clingo weigert 10¹⁷ al bij de invoer (CLINGO-001),
   wat een vermelding in de divergentielijst van gate 8 zou vragen.
3. **ref en Drools schrijven een jaar buiten 0000–9999 verschillend** (gefixt in `b9436dc8`, met een *voorlopig
   besluit van de bouwer*). Ch. 5.12 schrijft een datum "as the case format writes them", en het casusformaat kent
   zo'n jaar niet; de datumconstructie bereikt het nu. ref schreef `12026-03-01` en `-005-03-01`, Drools
   `+12026-03-01` en `-0005-03-01` (gemeten in een lezing). Besluit: ISO 8601's uitgebreide vorm, zoals Drools;
   `Value.iso_of_date` schrijft zo (ook in MISMATCH-zinnen en de parameter `from` van EF-009). Getuigen: de regel
   `ver vooruit` in `date-with-parts` (gate 17 vergelijkt `+12026-03-01` in ref en beide Drools-takken) en
   `WV-5.12-jaar-…`. Zin in ch. 5.12, gemarkeerd.
4. **Drools, geen EF-code op de fout van de datumconstructie, en geen millisecondebereik voor `dateTimeWith`**
   (niet gefixt: `backends/drools/` valt buiten deze tak). Zie punt 1 van "Wat ik niet deed" en de README bij het
   klaargelegde rode geval.
5. **Opmerking in de clingo-suite** (meegenomen in `5d774f2d`): het naamtabelblok zei dat `dateWith` en
   `dateTimeWith` in geen document staan; sinds `date-with-parts` wel.

### Fixronde (2026-10-22, op aanwijzing van de eigenaar; mandaat verbreed met `backends/drools/**`, `corpus/red/` en de registers en catalogi)

- `a17b4b8a` EF-032 en EF-007 voor de datumconstructie in ref, clingo en Drools.
- `7fdfec5d` (kaart `clingo-nested-aggregate-in-fold-filter`) de vorm voor de ronde vóór een creatie.
- dit bijgewerkte Result.

**De nieuwe code EF-032** — "the parts of a date construction name no calendar day or no clock time", kanaal `fault`,
parameters `year`, `month`, `day`, `hour`, `minute`, `second`, `millisecond`, spec home ch. 3.8, getuige
`corpus/red/date-with-no-calendar-day`. De soort stond eerst onder "Kinds without a code" in ch. 5.7b; die zin is weg en
de soort is een rij van de tabel. Alle drie de executors geven haar met de delen als parameters:

- ref: `faultc ~params:[…] "EF-032"` op de twee plaatsen in `Arith.date_construction`.
- clingo: de viol-getuige draagt de delen, `viol(("dateWith: the parts name no calendar day",2026,4,31),"S1")`, en
  `viol_code` leest er EF-032 en de parameters uit.
- Drools: `RegelSpraakRuntime.rsDatumMet`/`rsDatumtijdMet` toetsen de delen zelf (maand, dag met schrikkeljaar, uur,
  minuut, seconde, milliseconde) in de zin van ref uit de catalogi (`EF-032.day`, `EF-032.time`, Nederlands en Engels
  gelijk, zoals de andere EF-regels), en nemen de delen met `intValueExact()` in plaats van `intValue()`;
  `ComputeHelpers.dateConstruction` maakt er een `ComputeFault` van, en `ComputeFault.code()`/`params()` lezen EF-032
  en de delen uit de zin. Beide takken, want beide roepen dezelfde helper; `BranchDumpsCorpusWitnessTest` vergelijkt
  ze op de rode gevallen. `FilterFaultWitnessTest` pint sindsdien ook de fout van "de datum met … maand 13" (ref gaf
  daar REF-007).

Het rode geval `corpus/red/date-with-no-calendar-day`: vijf gevallen (31 april, 29 februari 2026, maand 13, dag 0, uur
24), elk met de vastgepinde parameters; groen in ref, clingo en Drools (gate 8: 31 Drools-gevallen met hun code).

**EF-007 voor een datum-tijd buiten het bereik.** De soort heet nu "a datum-tijd step or construction leaves the
representable millisecond range"; ch. 3.8 zegt het in de alinea over het bereik. ref geeft EF-007 (eerst zonder code),
Drools EF-007 (eerst `+200000000-03-01T00:00`), via de bestaande `milliseconden`-toets van de runtime. Getuige: het
rode geval `corpus/red/date-time-with-outside-range`, drie gevallen: jaar 200 000 000, één milliseconde na de bovengrens
(146140482-04-24T15:36:27.904) en één vóór de ondergrens (−146136543-09-08T08:23:32.095); de grenzen zelf zijn
ref-voorbeelden (`WV-3.8-datumtijdmet-bovengrens`, `-ondergrens`) en spelen in Drools zonder fout. clingo weigert alle
drie eerder, op zijn jaarhek, nu met een eigen code `CLINGO-029` (limit, `date-construction-year-range`,
`backends/clingo/codes.tsv`); **gatescript**: `scripts/check-red.sh` krijgt drie regels in zijn divergentielijst
(`date-time-with-outside-range/case…|clingo|CLINGO-029|LIMIT …`), want die lijst eist een regel voor een executor die
een geval om een eigen reden laat vallen.

**Vondst in de spec, gefixt:** ch. 3.8 noemde de ondergrens van het bereik −146136543-09-**09**T08:23:32.096; −2⁶² ms
valt op −146136543-09-**08** (java.time en ref zijn het eens; gevonden met het derde rode geval).

**CONSTRUCTS-tekst, bijgewerkt:** in de voorgestelde zin hierboven "the kind has no EF code yet, its red case waits on
the Drools backend reporting one" vervangen door "the fault kind is EF-032 (red case `date-with-no-calendar-day`), an
instant outside the millisecond range EF-007 (red case `date-time-with-outside-range`), in all three executors; v0's
runtime took the parts with `intValue()` and let java.time throw".

**Punt 5 van de fixronde, `dateWith` in een dagsoortvoorwaarde in clingo: niet gedaan, de benoemde weigering blijft.**
De ASP-backend rekent een dagsoortvoorwaarde bij de vertaling uit, over een venster van dagen rond de datums van de casus
plus de reikwijdte aan beide kanten (ASP-TRANSLATION S6a), en ref leest alleen de dagen die zijn stap bereikt.
`dateWith` is de eerste operand daar die per dag kan falen (29 februari van een jaar uit de dag): uitrekenen in het
venster zou clingo laten falen op dagen die ref nooit leest. Goed doen vraagt een fout per dag in de tabel in plaats
van bij de vertaling, plus een golden die Drools ook draagt; dat is geen kleine wijziging. De weigering blijft "a
day-kind condition carries an expression this backend does not evaluate".

**Gates na de fixronde:** `gates-3` op `7fdfec5d`: EXIT 0, alle 17 groen
(`werk/bruggen/date-with/gates-3.summary.txt`): gate 8 54 rode gevallen en 32 foutsoorten, het Drools-been 31 gevallen
met hun code en 49 parametercontroles; gate 12 de JUnit-suite 1649 tests, played 267; gate 17 0 afwijkingen. Na
`7fdfec5d` veranderde alleen dit Result.
