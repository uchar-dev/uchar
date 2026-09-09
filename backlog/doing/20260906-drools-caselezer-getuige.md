# De weigerkant van de Drools-case-lezer heeft nul regressiedekking

Goal: één JUnit-getuige naast de bestaande witness-suite die vastlegt wat de
eigen case-lezer van de Drools-backend WEIGERT — het halve contract dat de
DatumTijd-kaart (2026-09-06) wel invoerde maar niet vastpinde.

## De stand

De klem zit in `CaseRunner.datumTijdVanCaseWire` (aangeroepen vanuit
`waardeNaarArg`): een `dateTime`-waarde die niet het hele
`uchar-case-v0`-lexeem is — `yyyy-mm-ddThh:mm:ss[.fff]`, vaste
veldbreedtes, `T`, het secondenveld geschreven, geen zone of offset, een
fractie die optioneel is maar exact drie cijfers — wordt HARD geweigerd
(een `IllegalArgumentException`, nooit een `Excluded`, want een uitsluiting
zou een kapotte case stil groen maken). Vóór die kaart stond er
`LocalDateTime.parse(...)`, die `…T12:00` accepteerde en een fractie van
elke breedte POSITIONEEL las (`.1` = honderd ms, `.1234` = 123,4 ms), terwijl
de gedeelde lezer van interpreter + ASP dezelfde cijfers anders las (`.1` =
één ms, `.100000` = honderdduizend ms) en de literal-lezer ze weigerde. Eén
wire, drie antwoorden.

Het weiger-half is gemeten door de jar zelf, per vorm, en het staat in het
Result van `backlog/done/20260906-datumtijd-differentiaaldekking.md`. Wat
er NIET is: één regel testcode die het vasthoudt. Concreet, geteld
2026-09-06:

- **Het niet-over-afwijzen is wél mechanisch bewaakt.** Gates 11 en 12 lezen
  30 conforme `dateTime`-waarden; een klem die te ruim naar binnen slaat
  maakt die gates rood.
- **Het weigeren is door niets bewaakt**, en dat volgt uit diezelfde
  telling: geen van die 30 waarden valt buiten het patroon, dus de klem
  vuurt op geen enkele corpuscase. Haal hem weg en er is per constructie
  geen gate die het merkt — de drie lezers lopen weer uiteen en niets zegt
  er iets van. (De kaart meet dat expliciet na in zijn rood-eerst-stap, in
  plaats van het af te leiden.)
- **De ref/ASP-kant heeft zijn getuige wel** (`ref/test`: elke geweigerde
  veldklasse, met de positie in de melding, plus dat de CASE-wire smaller is
  dan de literal-wire). De Java-kant is de enige lezer zonder.

`DatumTijdLexeemWitnessTest` staat er al en pint de LITERAL-lezer
(`Literalen.parseDatumTijd`) op beide transpilertakken. Dat is een andere
lezer met een andere grammatica: de literal-wire laat twee spellingen toe
(canoniek ISO én het day-first bronsnippet), de case-wire maar één. Precies
dat verschil is vandaag nergens vastgelegd, en het is de vorm die het
makkelijkst per ongeluk gelijkgetrokken wordt.

## Wat de kaart oplevert

Eén nieuw testbestand in de bestaande witness-suite (`…/cases/`), gebouwd
zoals zijn buren: een handgeschreven case-JSON door de lezer, met de melding
als assertie.

**De acht veldklassen** die de patroonklem weigert, elk met zijn eigen
assertie en zijn eigen reden in de code:

1. een zone-suffix (`…T12:00:00Z`);
2. een numerieke offset (`…T12:00:00+01:00`);
3. een spatie waar het lexeem `T` schrijft (`2026-03-15 12:00:00`);
4. een smal DATUM-veld (`2026-3-15T09:00:00` — één cijfer maand of dag);
5. een smal TIJD-veld (`…T9:00:00` — één cijfer uur);
6. een fractie van een andere breedte dan drie (`.1`, `.1234`, `.100000` —
   de drie die de drie lezers uit elkaar dreven);
7. een ontbrekend secondenveld (`…T12:00`);
8. omringende tekst (de heel-string-eis: het lexeem IS de waarde, geen
   deelstring erin).

**De twee kalendervormen** erbij, want de veldbreedtes alleen laten ze door
en de klem vangt ze in zijn tweede helft (de `DateTimeParseException` die
tot een eigen melding wordt omgezet):

9. `2026-02-30T12:00:00.000` — vaste breedtes, bestaande maand, niet
   bestaande dag;
10. `2026-03-15T25:00:00.000` — vaste breedtes, uur 25.

Elk van die tien assertes eist niet alleen dát er geweigerd wordt maar ook
dat de melding de VORM noemt (klasse 1–8) respectievelijk de KALENDER
(klasse 9–10) — een weigering die niet zegt wat hij verwachtte, is een
weigering waar een producent niets mee kan, en dat is precies waarom de
melding in twee takken uiteenvalt.

**Eén tegenmeting erbij** (het niet-over-afwijzen, lokaal en niet alleen via
de gates): de canonieke vorm mét en zonder fractie wordt gelezen en levert
het verwachte instant.

**En de asymmetrie zelf:** het day-first bronsnippet
(`15-03-2026 12:00:00.000`) is een geldig LITERAL-lexeem (WF-095) en géén
geldige case-waarde. Eén assertie die beide kanten in één test zet, zodat
"de twee lezers gelijktrekken" niet stilletjes kan gebeuren.

## Rood-eerst

De verplichte volgorde, en hier is hij goedkoop: zet de klem uit (vervang
de aanroep in `waardeNaarArg` weer door `LocalDateTime.parse`), draai de
nieuwe test, en laat zien welke assertes vallen en met welke waarden ze
vallen — dat is meteen de meting die de tabel in `spec/IR-SPEC.md` ch. 4.1
draagt. Zet hem terug en de test is groen. Leg beide uitslagen vast in het
Result; een getuige die nooit rood is geweest, pint niets.

## Acceptance

- Het nieuwe bestand hoort bij de suite en draait mee in gate 12's
  JUnit-leg, met een niet-nul, nul-geskipt testaantal (de teller stijgt
  zichtbaar ten opzichte van 207).
- Rood-eerst getoond: met de klem uit valt elk van de tien assertes, met de
  gemeten verkeerde waarde of de kale exceptie erbij; met de klem aan is
  alles groen.
- De tegenmeting en de asymmetrie-assertie zijn er allebei, dus de test
  bewaakt beide richtingen.
- Alle dertien gates groen, elk met zijn echte exitcode; gates 11 en 12
  bewegen geen enkele bestaande telling.
- Geen bestaande test gewijzigd, geen productiecode gewijzigd (de klem is er
  al; deze kaart legt hem alleen vast).

May touch: `backends/drools/src/test/**`, `backlog/`.

## Out of scope

- De klem zelf en elke andere productiecode — die landde 2026-09-06 en wordt
  hier niet aangeraakt. Blijkt uit de rood-eerst-meting dat er een gat in de
  klem zit, dan is dat een bevinding voor een eigen kaart, geen sluipende
  uitbreiding van deze.
- De LITERAL-lezer: die heeft zijn getuige al.
- De emitterkant (een `DatumTijd`-literal krijgt nog het day-first snippet in
  `waarde` én `bron`) — eigen kaart, met een herpin van de emitter-goldens.
- De `waarde: null`-flank van WF-095 (schema-valide, twee luide weigeringen
  en één dereferentie) — benoemd in ch. 4.1, eigen vraag.

## Result (2026-09-09, branch `card/drools-caselezer-getuige`)

**Wat er landde.** Eén nieuw bestand,
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/CaseWireDatumTijdWitnessTest.java`,
met twaalf tests: de tien weigerklassen van de kaart, de tegenmeting en de
asymmetrie. Het model is de corpusgolden `jutterswet-meldstip` van de wire
(`Aanmelding.meldstip` is een `DatumTijd`-attribuut); de cases zijn hier
geschreven en geen corpusverwachting wordt gelezen of aangeraakt. De reads
lopen via `CaseRunner.invoerBeans`, de invoernaad vóór er een regel vuurt,
zodat wat de lezer van een waarde máákte zelf de assertie is.

Elke weigering wordt op drie dingen vastgelegd: dat er hard geweigerd wordt
(een `IllegalArgumentException`, en de helper faalt met de naam van de klasse
als het iets anders is — een `Excluded` zou een misvormde case onder
"not carried" wegschrijven en de gate groen laten), dat de melding de
geweigerde waarde en het veld citeert, en welke TAK sprak: de acht
patroonklassen moeten de VORM noemen (`yyyy-mm-ddThh:mm:ss[.fff]`), de twee
kalendervormen de KALENDER — en die laatste mogen de veldbreedtes juist
NIET verwijten, want die kloppen en de producent zou verkeerd gaan zoeken.

**Rood-eerst, meting 1: de klem uit.** De aanroep in `waardeNaarArg` werd
tijdelijk vervangen door `LocalDateTime.parse(...)`. Uitslag: 12 tests,
**11 failures, 0 skipped**. Alleen de tegenmeting bleef groen — die meet de
leeskant, en die veranderde niet. Wat er per klasse gemeten werd:

| lexeem | met de klem uit |
| --- | --- |
| `2026-03-15T12:00:00Z` | kale `DateTimeParseException`: unparsed text found at index 19 |
| `2026-03-15T12:00:00+01:00` | kale `DateTimeParseException`: unparsed text found at index 19 |
| `2026-03-15 12:00:00` | kale `DateTimeParseException`: could not be parsed at index 10 |
| `2026-3-15T09:00:00` | kale `DateTimeParseException`: could not be parsed at index 5 |
| `2026-03-15T9:00:00` | kale `DateTimeParseException`: could not be parsed at index 11 |
| `2026-03-15T12:00:00.1` | **geaccepteerd** als `2026-03-15T12:00:00.100` |
| `2026-03-15T12:00:00.1234` | **geaccepteerd** als `2026-03-15T12:00:00.123400` |
| `2026-03-15T12:00:00.100000` | **geaccepteerd** als `2026-03-15T12:00:00.100` |
| `2026-03-15T12:00` | **geaccepteerd** als `2026-03-15T12:00` |
| `rommel 2026-03-15T12:00:00.000 x` | kale `DateTimeParseException`: could not be parsed at index 0 |
| `2026-02-30T12:00:00.000` | kale `DateTimeParseException`: Invalid date 'FEBRUARY 30' |
| `2026-03-15T25:00:00.000` | kale `DateTimeParseException`: Invalid value for HourOfDay (valid values 0 - 23): 25 |
| `15-03-2026 12:00:00.000` | kale `DateTimeParseException`: could not be parsed at index 0 |

De drie fractiebreedtes zijn de kern: `.1` en `.100000` werden hier allebei
honderd milliseconden, waar de gedeelde interpreter/ASP-lezer één respectievelijk
honderdduizend leest; `.1234` werd 123,4 ms. Drie stille, verschillende
antwoorden op dezelfde cijfers. (De laatste twee fractiebreedtes vallen in
de groene volgorde achter de eerste assertie; ze zijn los door dezelfde
lezer gemeten met een tijdelijk meetbestand, dat daarna is verwijderd.)

**Rood-eerst, meting 2: merkt een gate het?** De kaart vroeg dit te meten in
plaats van af te leiden. Met de klem uit én het nieuwe getuigebestand
tijdelijk opzijgezet: **gate 12 PASS** (played 117, auto-excluded 44,
value-divergence 1, no-case 2, kanaalvloer 47, JUnit 210) en **gate 11
PASS** (45 `.rgs` byte-identiek, 27 tests, 29 replays, nul divergenties).
Beide groen. De weigerkant was inderdaad door geen enkele gate bewaakt; de
30 conforme `dateTime`-waarden liggen alle binnen het lexeem, dus de klem
vuurt op geen enkele corpuscase.

**Groen.** Klem teruggezet met `git checkout` van het bestand:
`git diff main -- backends/drools/src/main` is leeg, productiecode dus
byte-identiek aan `main`. De suite draait dan 12 tests, 0 failures,
0 skipped.

**Geen gat in de klem gevonden.** Nagelopen omdat de kaart erom vroeg: alle
vier de routes waarlangs een `dateTime` de backend binnenkomt lopen door
`waardeNaarArg` en dus door de klem — attribuutinvoer (r. 444),
parameter/global-invoer (r. 588), de waarde van een tijdlijnsegment (r. 711)
en de verwachte waarde van een read-back (r. 767). Het patroon van de klem
is bovendien letterlijk het patroon van `spec/schema/uchar-case-v0.schema.json`
(`^[0-9]{4}-...(\.[0-9]{3})?$`), dus de twee kunnen niet uit elkaar lopen
zonder dat gate 4 of deze getuige het zegt. Wat het schema NIET dekt is de
kalender: `2026-02-30T12:00:00.000` is schema-valide en wordt door de tweede
tak van de klem geweigerd. Dat is de bedoelde taakverdeling, geen gat — en
het is nu vastgelegd. Er is dus geen vervolgkaart voorgesteld.

**Gates, alle dertien vanuit de worktree, elk met zijn echte exitcode.**
1 PASS · 2 PASS (187 files) · 3 PASS (187 golden clean, 337 negatief) ·
4 PASS (442 casebestanden) · 5 PASS (419 cases) · 6 PASS (419 cases) ·
7 PASS (383 weerleggen de no-op, 36 gepinde survivors) · 8 PASS (23 rode
cases, Drools-been 6 fault-pinned + 17 not-carried) · 9 PASS (201 checks,
12 catala-goldens) · 10 PASS (173 tests, 37 rgs-goldens) · 11 PASS (45
`.rgs` byte-identiek, 2 holdouts, 27 tests, 29 replays, nul divergenties) ·
12 PASS (played 117, kanaalvloer 47, auto-excluded 44, value-divergence 1,
no-case 2, **JUnit 210 → 222**) · 13 PASS (74 twists over 15 goldens, 0
survivors). De JUnit-teller is de enige die beweegt; elke andere telling
staat gelijk aan de meting vóór deze kaart.

**Wat bewust bleef liggen.** De klem zelf en elke andere productiecode; de
LITERAL-lezer (heeft zijn eigen getuige, hier alleen aangeraakt in de
asymmetrietest); de emitterkant; de `waarde: null`-flank van WF-095.
