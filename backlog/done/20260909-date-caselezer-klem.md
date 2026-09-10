# De date-kant van de Drools-case-lezer heeft geen klem — drie levende routes

Goal: beslissen of de `date`-kant van de case-wire dezelfde patroonklem plus
getuige krijgt die de `dateTime`-kant op 2026-09-06/09 kreeg, of dat de
huidige kale lezing met een reden blijft staan. De kaart levert het besluit
plus, als het besluit "klem" is, de klem en zijn getuige. Het gaat om DRIE
plekken, niet één.

## De stand (gemeten 2026-09-09)

`CaseRunner` leest op drie levende plekken een datum van de case-wire met
een kale `LocalDate.parse(...)`. Geen van de drie heeft een patroonklem of
een eigen melding; de `dateTime`-klem één regel verderop (r. 904) heeft
allebei.

| plek | wat het leest | wat een ongeldige waarde doet |
| --- | --- | --- |
| r. 903 `waardeNaarArg` | een `date`-waarde van een attribuut, parameter, tijdlijnsegment of read-back-verwachting | vormklasse gemeten (zie hieronder); geen replay gemeten |
| r. 600 `asOfDatum` | de `asOfDate` van de case (schema-property `asOfDate`, patroon `^[0-9]{4}-[0-9]{2}-[0-9]{2}$`) | **stil aanvaard**: `ref` faalt hard, Drools meldt `PLAYED 1` |
| r. 721 `grensDatum` | de `from`/`to` van een tijdlijnsegment | **stil aanvaard**: `ref` faalt hard, Drools meldt `PLAYED 1` |

**De twee onderste zijn de ernstige.** Gemeten in lens 2 (2026-09-09), met
`+012026-03-15` als `asOfDate` op `jutterswet-penningjaar` respectievelijk
`-0500-01-01` als segmentgrens op `jutterswet-bakenmeester`: `ref` weigert
allebei hard (`EVALUATION FAULT`), en de Drools-backend meldt gewoon
`PLAYED 1`. De golden passeert dus. Dat is geen smaakverschil in een
foutmelding maar een stille aanvaarding op de plek waar de hele
evaluatiedatum respectievelijk het emmerraster aan hangt: `+012026-03-15`
is jaar +12026.

**De vormklasse zelf is smal maar niet leeg.** `LocalDate.parse` weigert
het meeste wat de wire ook weigert (smal veld `2026-3-15`, zonesuffix
`2026-03-15Z`, basisformaat `20260315`, omringende spatie, een
`dateTime`-lexeem, en de kalender: `2026-02-30`). Het accepteert wél de
twee ISO-vormen met een uitgebreid jaar plus teken die het schemapatroon
uitsluit: `+012026-03-15` (jaar +12026) en `-0500-03-15` (jaar −500).
`ref`'s `date_of_iso` (`ref/lib/value.ml:92`) eist exact vier/twee/twee
cijfers en cijfers alleen en weigert ze allebei — gedraaid in lens 2, niet
alleen uit de code gelezen. Precies de één-wire-twee-antwoorden-klasse
waarvoor de `dateTime`-klem is neergezet ("elke lezer houdt zijn eigen
hek", IR-SPEC ch. 4.1).

Daarnaast: waar deze routes wél weigeren, weigeren ze zonder context. De
`dateTime`-klem noemt de geweigerde waarde, het VELD waarop hij binnenkwam
en de verwachte vorm; `LocalDate.parse` gooit een kale
`DateTimeParseException` ("could not be parsed at index 5"), waar een
producent het veld niet uit kan aflezen. (`grensDatum` op r. 721 heeft die
context wél voor een ONTBREKENDE of niet-tekstuele datum, en verliest hem
één regel later voor een misvormde.)

Er is nog een vierde, DODE parse in dezelfde map: de `datumTijd`-decoder in
`engine/ParametersetLader.java:303` (kale `LocalDateTime.parse`) wordt
alleen vanaf r. 237 in diezelfde klasse aangeroepen, en die klasse heeft in
heel `backends/drools/src` geen enkele gebruiker (main noch test). Hij
leest bovendien een ANDER document (een parameterset, geen
`uchar-case-v0`-casewaarde), dus hij hoort niet bij de klemvraag — maar hij
moet wel benoemd worden, anders leest "de case-routes zijn geklemd" te
ruim. Deze kaart mag hem opruimen of expliciet laten staan; dat is een
tweede, kleine beslissing.

## De vraag die beslist moet worden

Is de tekenjaarvorm een echte producentvorm (dan: klem op alle drie de
routes, met een melding in dezelfde twee takken als de `dateTime`-klem), of
is hij zo onwaarschijnlijk dat de kale lezing met een reden mag blijven
staan? Voor r. 903 zijn beide antwoorden verdedigbaar. Voor r. 600 en
r. 721 ligt het zwaarder: daar is de gemeten uitslag een golden die stil
`PLAYED` meldt terwijl `ref` hem weigert, en dat is per definitie een
divergentie die de differentiaal niet ziet.

## Acceptance

- Het besluit staat in het Result, per route, met de meting erbij.
- Bij "klem": de klem staat er op elke geklemde route, weigert de gemeten
  vormen hard (nooit een `Excluded`), noemt waarde + veld + verwachte vorm,
  en krijgt een getuige in de bestaande witness-suite die rood-eerst is
  getoond — met de spiegelassertie erin (een vormtak die de
  kalenderformulering meedraagt moet vallen) en met de randbreedtes van
  beide kanten gepind, want een klem die alleen van bovenaf is vastgelegd
  laat een verruiming van het patroon ongezien door.
- Bij "geen klem": de reden staat als commentaar op de betreffende regel,
  en de kaart legt vast dat de vormen bekend en aanvaard zijn.
- De `ParametersetLader`-parse is opgeruimd of expliciet met reden gelaten.
- Alle dertien gates groen; gate 12's JUnit-teller stijgt bij "klem",
  en geen andere telling beweegt.

May touch: `backends/drools/**`, `backlog/`, en — alleen als het besluit dat
eist — `spec/schema/uchar-case-v0.schema.json` (protected: voorstel, niet
mergen).

## Out of scope

- De `dateTime`-getuige en zijn klem: die landden 2026-09-06/09 en zijn
  klaar.
- De datum-LITERAL-lezer (WF-084) — andere lezer, eigen regel, eigen
  getuige.
- Kale parses buiten de case-wire van deze backend. Let op: dit sluit NIET
  in dat de overige parses binnen `backends/drools` al gedekt zijn — ze zijn
  hier alleen niet geïnventariseerd. Deze kaart dekt de drie case-wire-
  routes hierboven plus de dode parse; wat er verder in de map staat is
  ongemeten.

## Result (2026-09-10)

**Besluit: KLEM op alle drie de routes**, naar het patroon van de
`dateTime`-klem: één helper (`datumVanCaseWire`) met dezelfde twee takken,
dezelfde meldingsvorm (waarde + veld + verwachte vorm), harde
`IllegalArgumentException` en nooit een `Excluded`. Grond per route staat
hieronder bij de meting.

### Hermeting op deze HEAD (6a66bd2)

Het zijn er drie, en het zijn dezelfde drie; de regelnummers liggen negen
hoger dan in de kaart:

| kaart | deze HEAD | functie | wat het leest |
| --- | --- | --- | --- |
| r. 903 | r. 912 | `waardeNaarArg`, de `date`-tak | attribuut-, parameter-, segment- en read-back-waarden |
| r. 600 | r. 609 | `asOfDatum` | de `asOfDate` van de case |
| r. 721 | r. 730 | `grensDatum` | de `from`/`to` van een tijdlijnsegment en van een kenmerkperiode |

Buiten `CaseRunner` en buiten de case-wire staan nog twee kale parses in
`loader/IrLoader.java` r. 545–546 (de `Geldigheid` van de **IR**-wire) —
door de kaart buiten scope gelaten, hier niet aangeraakt en nog steeds
ongemeten.

### De twee stille aanvaardingen, gereproduceerd

Beide mutaties zijn kopieën in `/tmp`; er is geen corpusbestand aangeraakt.

`asOfDate` `+012026-03-15` op `jutterswet-penningjaar`:

```
ref     EVALUATION FAULT: malformed date lexeme "+012026-03-15" (expected ISO yyyy-mm-dd)   exit 1
Drools  PLAYED 1                                                                            exit 0
```

segmentgrens `-0500-01-01` op `jutterswet-bakenmeester` (`onderhoudstarief.from`):

```
ref     EVALUATION FAULT: malformed date lexeme "-0500-01-01" (expected ISO yyyy-mm-dd)      exit 1
Drools  PLAYED 1                                                                            exit 0
```

Dat is de grond voor r. 609 en r. 730: een golden die `PLAYED` meldt terwijl
`ref` hem weigert, en de differentiaal ziet het niet, want een case die op
de verkeerde datum evalueert of op het verkeerde emmerraster wordt gesneden
kan alle gepinde verwachtingen nog steeds halen.

### De derde route, apart gemeten

Voor r. 912 is de uitslag zwakker en dat is de eerlijke stand. Met
`+012026-06-21` als `Vondst.vondstdatum` én in de read-back-verwachting van
`jutterswet-jutdatum`:

```
ref     EVALUATION FAULT: malformed date lexeme "+012026-06-21" (expected ISO yyyy-mm-dd)
Drools  FAIL jutdatum-plusjaar.json: kenmerk midzomers: verwacht aanwezig, werkelijk
        afwezig; kenmerk nagekomen: verwacht afwezig, werkelijk aanwezig
```

De lezer aanvaardt de vorm zonder een woord en rekent door met jaar 12026;
dat de golden hier alsnog rood wordt komt doordat de regels op het jaar
letten, niet doordat de lezer iets merkte. Geen `PLAYED` gemeten op deze
route. De klem staat er om twee redenen: consistentie (elke lezer houdt zijn
eigen hek, IR-SPEC ch. 4.1) en omdat de diagnose anders naar de verkeerde
plek wijst — de melding noemde een kenmerk, niet de misvormde datum.

### Rood eerst

De getuige is `backends/drools/src/test/.../cases/CaseWireDatumWitnessTest.java`,
een zusterklasse naast `CaseWireDatumTijdWitnessTest` en in diens vorm
(zelfde helperopzet, zelfde twee-takken-assertie, zelfde
`Excluded`-uitsluiting). Een nieuw bestand in plaats van uitbreiding van de
`dateTime`-getuige, omdat die klasse in naam en javadoc over `dateTime`
gaat en de drie `date`-routes eigen fixtures nodig hebben; de repo houdt één
getuige per onderwerp aan.

Tegen de kale `LocalDate.parse`, vóór de klem:

```
[ERROR] Tests run: 18, Failures: 13, Errors: 0, Skipped: 0
```

De dertien die vallen zijn precies de weiger-asserties; de vijf die al groen
zijn, zijn de tegenmetingen: de canonieke vorm, de twee jaarranden (samen
één testmethode), de schrikkeldag, de geldige `asOfDate` en de geldige
periode — die moeten vóór én na de klem groen zijn.

De dertien splitsen in **vier die `ACCEPTED it` meldden** en negen die
`let it through to a bare java.time.format.DateTimeParseException` meldden.
Die vier zijn de eigenlijke gaten, en twee ervan zitten op de twee
ernstigste routes van deze kaart:

| getuige | wat de lezer ervan maakte |
| --- | --- |
| `de_asOfDate_weigert_dezelfde_vormen_en_noemt_zijn_veld` | `+012026-03-15` aanvaard als jaar 12026, en het emmersegment eromheen gebouwd: `ACCEPTED it and read [Segment@…]` |
| `een_segmentgrens_weigert_dezelfde_vormen_en_noemt_zijn_plek` | `-0500-01-01` als `from` aanvaard, segment eromheen gebouwd: `ACCEPTED it and read [Segment@…]` |
| `een_getekend_uitgebreid_jaar_wordt_geweigerd` | `+012026-06-21` gelezen als `+12026-06-21` |
| `een_negatief_jaar_wordt_geweigerd` | `-0500-01-01` gelezen als jaar −500 |

De negen andere weigerden al, maar met een kale
`DateTimeParseException` die noch het veld noch de verwachte vorm noemt —
de tweede helft van wat de klem repareert.

Na de klem:

```
tests="18" errors="0" skipped="0" failures="0"
```

De aparte rood-commit is door de hoofdsessie in de klem-commit gesquasht,
zodat elke commit op `main` een groene suite draagt; de rood-bewijsvoering
staat hier in deze Result en nergens anders meer.

### Randbreedtes: de klem volgt `ref`, vorm voor vorm

Elke vorm is door `ref` gedraaid (als `asOfDate` op `jutterswet-penningjaar`,
met de echte runner), niet uit de code afgelezen. De kolom "klem" is wat de
getuige pint — met deze precisering: voor 10 van de 17 vormen pint de
getuige exact hetzelfde lexeem, voor de overige 7 pint hij dezelfde
vormklasse op een andere dag (de getuige werkt op `jutterswet-jutdatum` en
schrijft 21 juni waar deze tabel 15 maart schrijft: `2026-6-21` naast
`2026-3-15`, `20260621` naast `20260315`, enzovoort). De vormklasse is wat
de klem beslist — de klem leest de dagcijfers niet vóór het patroon — maar
lexeem-voor-lexeem-identiek is de kolom dus voor 10 rijen, niet voor 17.

| vorm | schemapatroon | `ref` | klem |
| --- | --- | --- | --- |
| `2026-03-15` | matcht | `PASS` | leest |
| `0000-01-01` | matcht | `PASS` | leest |
| `9999-12-31` | matcht | `PASS` | leest |
| `2024-02-29` | matcht | `PASS` | leest |
| `+012026-03-15` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `-0500-01-01` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `10000-01-01` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `2026-3-15` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `2026-03-5` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `2026-03-15Z` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `20260315` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `" 2026-03-15"` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `2026-03-15T12:00:00` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `"rommel 2026-06-21 x"` | matcht niet | malformed date lexeme | weigert, VORM-tak |
| `2023-02-29` | matcht | day out of range | weigert, KALENDER-tak |
| `2026-02-30` | matcht | day out of range | weigert, KALENDER-tak |
| `2026-13-01` | matcht | month out of range | weigert, KALENDER-tak |

Identiek op alle zeventien vormen. De kale `LocalDate.parse` week op precies
twee af — `+012026-03-15` en `-0500-01-01`, allebei aanvaard — en op geen
enkele andere; dat is de hele afwijkingsklasse, gemeten met een los
JDK-programma over dezelfde lijst.

**Wat de klem volgt.** `ref`. Dat spoort met het schemapatroon omdat beide
dezelfde veldgrammatica beschrijven: `^[0-9]{4}-[0-9]{2}-[0-9]{2}$` is
letterlijk wat `Value.date_of_iso` eist (vier/twee/twee, cijfers alleen), en
de kalendercontrole erbovenop is wat een JSON-patroon niet kán en `ref` wél
doet. De klem is dus schemapatroon + kalender = `ref`. Geen
schemawijziging nodig, en de klem is geen tweede definitie naast het
schema maar dezelfde, één veld dieper.

### De klem is aan beide kanten vastgelegd — gemeten met mutanten

Vier mutanten op de klem, elk met de getuige erop. De vallende gevallen
zijn per mutant uit de surefire-XML gelezen, niet uit de consoleregels:

| mutant | uitslag | wie valt |
| --- | --- | --- |
| A: de VORM-tak draagt óók de kalenderformulering | 10 failures | alle acht vormgevallen + beide routegetuigen (`asOfDate` en segmentgrens) |
| B: patroon verruimd tot `[+-]?\d{4,}` | 5 failures | de drie tekenjaar/vijfcijfer-vormgevallen (`een_getekend_uitgebreid_jaar`, `een_negatief_jaar`, `een_jaarveld_van_vijf_cijfers`) + beide routegetuigen (`asOfDate` en segmentgrens) |
| C: patroon vernauwd tot `[1-9]\d{3}` (geen jaar 0000) | 2 errors | `de_jaarranden_die_het_patroon_toelaat_worden_gelezen` en `de_asOfDate_leest_een_geldige_datum_nog_steeds`, beide op `malformed date '0000-01-01'` |
| D: `find()` in plaats van `matches()` | 8 failures | omringende tekst, zonesuffix, `dateTime`-lexeem, de twee tekenjaren, het vijfcijferige jaar + beide routegetuigen |

A is de spiegelassertie die de kaart eist. C is de kant die een klem die
alleen van bovenaf is vastgelegd niet ziet: geen corpuscase zit in de buurt
van jaar 0000 of 9999, dus zonder die twee asserties passeert een vernauwing
elke andere test én elke gate.

**Kanttekening bij de meting zelf, voor het record.** De eerste mutantloop
draaide `git checkout --` op het bronbestand ná elke mutant, terwijl de klem
op dat moment nog niet gecommit was; die revert nam dus ook de klem mee,
waarna B, C en D tegen de ONGEKLEMDE basislijn maten en netjes 13 failures
meldden — een getal dat er plausibel uitziet en niets zegt. Opnieuw gedaan
tegen de gecommitte klem; de cijfers in de tabel hierboven zijn die tweede,
geldige ronde. Bij de fixronde bleek dezelfde tabel bovendien uit de
consoleregels te zijn samengevat: rijen B en D noemden minder gevallen dan
hun eigen telling. Een `grep`-patroon `[a-z_0-9]+` over die consoleregels
verklaart een deel daarvan — het sloeg de getuige `de_asOfDate_…` met zijn
hoofdletter over — maar niet alles: de toenmalige rij C noemde een
hoofdletter-D-naam gewoon voluit, en rij D miste óók
`een_jaarveld_van_vijf_cijfers`, dat volledig uit kleine letters bestaat.
Die tweede omissie heeft een andere oorzaak, die niet is achterhaald;
vermoedelijk een fout in het overschrijven naar proza. Vandaar de
XML-hermeting van alle vier de rijen. De les staat los van de oorzaak: een
telling die stil iets anders meet dan de opsomming ernaast beweert, valt
alleen op als je telling en opsomming tegen elkaar legt.

### De dode parse

`engine/ParametersetLader.java` is **verwijderd** (326 regels). Meting vóór
verwijdering: de klassenaam komt in heel `backends/drools` — src/main,
src/test, resources en `pom.xml` — precies twee keer voor, beide in het
bestand zelf (de declaratie r. 53 en de private constructor r. 60). Nul
gebruikers. De genest gedeclareerde typen (`ParametersetFout`,
`DecodeResultaat`) hebben ook nul verwijzingen buiten het bestand.
`mvn compile` na verwijdering: exit 0. De klasse droeg drie kale parses
(r. 213, 293 en de door de kaart genoemde r. 303) en las bovendien een ander
document (een parameterset-sidecar, geen `uchar-case-v0`-casewaarde). Na
verwijdering staan er in `backends/drools/src/main` nog vier
date-parse-aanroepen: twee binnen de kalendertakken van de twee klemmen
(`CaseRunner` r. 948 en r. 977) en de twee IR-wire-parses in `IrLoader`.

### Na de klem, end-to-end

Dezelfde drie gemuteerde cases, met de herbouwde jar:

```
ERROR IllegalArgumentException: malformed date '+012026-03-15' on 'asOfDate': the
  uchar-case-v0 lexeme is yyyy-mm-dd — fixed field widths, digits only, no sign and
  no extended year                                                             exit 3
ERROR IllegalArgumentException: malformed date '-0500-01-01' on 'onderhoudstarief.from': …
                                                                               exit 3
ERROR IllegalArgumentException: malformed date '+012026-06-21' on 'vondstdatum': …
                                                                               exit 3
```

De ongemuteerde goldens: `PLAYED 1` en `PLAYED 1`, exit 0.

`grensDatum` geeft als veldnaam `<tijdlijn>.<grens>` door, omdat een case
meerdere tijdlijnen kan dragen en elke tijdlijn twee grenzen heeft; een van
de twee coördinaten is niet genoeg om de plek terug te vinden.

### Gates

| gate | uitslag | door | tellingen |
| --- | --- | --- | --- |
| 12 `check-drools.sh` | PASS (exit 0) | bouwsessie | **JUnit 262, vloer 239** (was 244; +18 door de getuige, exact het aantal in de surefire-XML). played **120** = `EXPECTED_PLAYED` 120, ongewijzigd. auto-excluded 44, channel floor 47, value-divergence 1, no-case 2 — alle vier gelijk aan de stand vóór de kaart. |
| 8 `check-rood.sh` | PASS (exit 0) | bouwsessie | 26 rode cases falen zichtbaar in ref én clingo, vloer 26; Drools-poot 6 fault-pinned, 20 not-carried |
| 13 `check-reftype-derivation.sh` | PASS (exit 0) | bouwsessie | 74 annotaties getwist over 15 goldens (vloer 74), 70 replays reproduceren de ongetwiste uitkomst, 13 PLAYED via de DRL-tekst-tak (vloer 13), 0 survivors |
| 1 `check-self-contained.sh` | PASS | bouwsessie, elke ronde | — |
| 11 `check-toka-parity.sh` | PASS | lens 2 | de jar wordt daar ONVOORWAARDELIJK herbouwd en 29 TOKA-cases replayen erdoorheen, dus de klem zit op het pad; gemeten: geen enkele TOKA-case draagt een buiten-patroon-datumlexeem, dus de klem kón geen enkele uitkomst raken — nu gemeten in plaats van aangenomen |
| 2, 3, 4, 5, 6 | PASS | lens 1 | onderdeel van de negen die lens 1 draaide |

**Wie wat draaide, en waarom de rest buiten bereik viel.** De bouwsessie
draaide de vier gates die deze wijziging direct raken: 12 (de backend zelf),
8 (de rode zone mag niet bewegen), 13 (de twist-uitkomsten mogen niet
bewegen) en 1 bij elke commit. Lens 1 draaide er negen, inclusief 2, 3, 4, 5
en 6 — de schema-, verifier-, case-schema-, oracle- en clingo-gates, die
allemaal ongevoelig zijn voor een Java-lezer maar samen aantonen dat de
corpus- en spec-kant onbewogen is. Lens 2 draaide 11, de enige gate die
buiten gate 12 om de backend-jar herbouwt en dus de klem echt uitvoert;
daar zat de laatste open vraag, want de TOKA-zone draagt cases die het
publieke corpus niet kent. Gates 9 en 10 (Catala- en RegelSpraak-frontend)
raken geen backend-jar en zijn daarom buiten bereik gelaten. De volle set
draait de hoofdsessie vóór de merge.

Geen vloer verhoogd, geen `EXPECTED_PLAYED` aangeraakt, geen gate-script
aangeraakt. De vloer 239 is een ondergrens; groei hoeft niet gepind te
worden.

### Wat bewust niet bewoog

- **`spec/schema/uchar-case-v0.schema.json`**: niet aangeraakt. Het patroon
  dekt de wire al precies goed; de klem spiegelt `ref` en dat ís het patroon
  plus de kalender. Geen dwingende reden gemeten om het schema te wijzigen.
- **Het corpus**: geen bestand aangeraakt. De twee gemuteerde cases waren
  `/tmp`-werk.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`**: niet aangeraakt, en er is naar
  mijn oordeel **geen registerrij nodig**. Dit is backend-hygiëne zonder
  wire-wijziging: geen constructie verandert, geen IR-vorm verandert, en
  **geen gepinde verwachting beweegt** — gate 12 speelt dezelfde 120
  goldens met dezelfde uitkomsten. Wat wél verandert is de FOUTVORM op
  documenten die toch al faalden: een lexeem dat het schemapatroon
  toelaat maar de kalender niet (`2026-02-30`) faalde eerst met een kale
  `DateTimeParseException` en faalt nu met de kalendertak van de klem.
  Zelfde verdict, andere melding. En documenten die het schemapatroon NIET
  toelaat worden nu geweigerd in plaats van stil beantwoord.
- **`IrLoader` r. 545–546**: de IR-wire, door de kaart buiten scope gelaten.
  Ongemeten gebleven — en na deze kaart de enige kale date-parses in
  `backends/drools/src/main` naast de kalendertak-parses binnen de klemmen
  zelf. Nu gekaart: `backlog/new/20260910-irloader-datumklem.md`.
- **De grofheid van de getuige**, expliciet: geasserteerd is één helper met
  drie aanroepplaatsen, en alle drie de plaatsen dragen een eigen assertie
  (waarde, `asOfDate`, segmentgrens). Wie een vierde `date`-positie zou
  toevoegen zonder de helper aan te roepen, wordt door deze getuige niet
  gestopt.
