# rood-asofdate-kalender: de kalenderhelft van de asOfDate-weigering pinnen

Voorstel uit de ref-geldigheid-kaart (2026-09-23): het case-schema
weigert een dag-eerst-`asOfDate` maar laat een kalender-ongeldige
door (`2026-13-01`, `2026-02-30` — het patroon toetst de cijfers,
niet de kalender). Sinds die kaart weigert de caselader zulke waarden
benoemd (CASE ERROR, exit 2) en clingo volgt via de gedeelde module;
de weigering is alleen suite-gepind. Een ROOD document met een
kalender-ongeldige asOfDate zou hem corpusbreed pinnen — meet eerst
hoe gate 8's Drools-been een Drools-ERROR op een case telt (de
Drools-takken weigeren de waarde ook, maar met hun eigen zin).

## Acceptatie (bij oppakken aanscherpen)

- (a) Het rode document (`expectedFault {ref, clingo}` met de
  laderzin), rood-eerst tegen de oude lader (die stil antwoordde);
  de Drools-uitsluitings- of fragmentkant van check-rood gemeten en
  zo nodig als **voorstel**.
- (b) Weeg of het schemapatroon de kalender kan leren (gate 2/4,
  spec/schema is **voorstel**) — dan verschuift de getuige naar
  negatief-achtig terrein; motiveer de keuze.
- (c) Alle veertien gates groen; pins bewegen niet.

May touch: `corpus/rood/**`, `scripts/check-rood.sh` (**voorstel**),
`spec/schema/**` (**voorstel**), kaartverplaatsing.

## Acceptatie (aangescherpt bij de claim, 2026-09-23)

Branch `card/rood-duo`, samen met `rood-heeltijdvak-ongenegeerd`.

- (a) `corpus/rood/asofdate-kalender`: `ir.json` is het document van
  golden `jutterswet-vensterheffing` byte voor byte (twee
  geldigheidsvensters, dus de datum doet ertoe), de case heeft
  `asOfDate` `2026-02-30`. Gemeten: gate 4 laat de waarde door. ref en
  clingo weigeren bij het laden. Gepind wordt de volle laderzin, geen
  bodemzin.
- (b) Rood-eerst tegen de lader van `cc0e6a1b` (basis van de
  geldigheidskaart, vóór `6ff39603`): `git archive` naar /tmp,
  `dune build`, en laten zien dat de oude runners stil antwoorden.
- (c) Drools-been gemeten met DroolsRoodMain en met beide takken. Bij
  een FAULT met een eigen zin wordt het fragment gepind. Bij een ERROR
  volgt een open punt en geen uitsluiting.
- (d) Schemaweging gemotiveerd. `spec/schema` blijft ongewijzigd,
  tenzij er een dwingende reden is (dan als voorstel).
- (e) Gates 1–14 groen, elk los. De pins bewegen niet.

## Result (2026-09-23)

**Geland, alle veertien gates groen.** Het document is
`corpus/rood/asofdate-kalender` (commit `728252f7`): `ir.json` is golden
`jutterswet-vensterheffing` byte voor byte (`cmp`), de case is de input
van die golden (M1, meldwaarde 100) met `asOfDate` `2026-02-30`, en
`expectedFault` pint alle drie de uitvoerders. De commentaarregels van
het Drools-been staan in `84ed8713` (**beschermd-pad-voorstel**, zie
onder).

### Waarom 2026-02-30

Februari heeft in geen enkel jaar een dertigste dag, dus de
handcontrole heeft geen schrikkeljaarregel nodig. Als string ligt het
lexeem tussen 2026-02-28 en 2026-03-01, dus een lezer die de string
vergelijkt, geeft een aannemelijk antwoord. `2026-13-01` sorteert na
elke datum in 2026 en geeft een zichtbaar vreemd antwoord. Hetzelfde
lexeem pint gate 2 in zijn patroonzelftest als "THE LAYER BOUNDARY"
van de IR-draad. Dit document is die grens op de casedraad.

### Gate 4 laat hem door (gemeten)

Met de validator van gate 4 (`Draft202012Validator`, zonder format
checker) op de case met één veld vervangen:

```
2026-02-30     ADMITTED
2026-13-01     ADMITTED
2025-02-29     ADMITTED
2026-00-10     ADMITTED
2026-04-31     ADMITTED
30-02-2026     REJECTED: '30-02-2026' is not valid under any of the given schemas
2026-2-3       REJECTED: '2026-2-3' is not valid under any of the given schemas
+02026-02-28   REJECTED: '+02026-02-28' is not valid under any of the given schemas
```

Gate 4 zelf: `PASS (schema valid; 640 case file(s) validate)`, dit
document inbegrepen.

### Pins en metingen (worktree, HEAD)

```
verifier: PASS
oracle: expected fault observed at load (asOfDate "2026-02-30" is not the uchar-case-v0 date lexeme yyyy-mm-dd (day out of range in date lexeme "2026-02-30"))
clingo: expected fault observed at load (asOfDate "2026-02-30" is not the uchar-case-v0 date lexeme yyyy-mm-dd (day out of range in date lexeme "2026-02-30"))
```

De pin voor ref en clingo is de volle laderzin, met de eigen reden van
de lezer, want die reden is de kalenderhelft. Het is één lezer die twee
keer gezien wordt: een regressiehek, geen differentiaal. `ref
--bodemzin`: `PINS 2`, geen HIT. Afgestemd met de parallelle
caselabel-kaart (alleen gelezen, niets aangeraakt): die veralgemeent
de zin tot `"%s %S is not the uchar-case-v0 date lexeme yyyy-mm-dd
(%s)" at s why` met `at = "asOfDate"`, dus byte-gelijk voor deze
positie. `Value.date_of_iso` staat niet in haar diff (stand van die
worktree bij het meten).

### Rood-eerst (oude lader)

De opdracht noemde "het commit vóór de geldigheidslanding e19018c9".
`e19018c9^` = `44a3fcaa` bevat de nieuwe lader al (`grep -c` op de zin
geeft 1), want de landing is `6ff39603`. Daarom is gebouwd vanaf
`cc0e6a1b`, de basis van de geldigheidskaart (0 treffers voor de zin):
`git archive` naar /tmp, `dune build ref backends`.

```
== ref (oude lader, cc0e6a1b)
EXPECTED FAULT DID NOT OCCUR: evaluation succeeded
rc=1
== clingo (oude lader, cc0e6a1b)
EXPECTED FAULT DID NOT OCCUR: clingo found a model
rc=1
```

Het stille antwoord, op een groene kopie met `expected` vensterheffing
125, overgangsheffing leeg en consistent: oud ref `oracle: PASS (3
expectations)`, oud clingo `clingo: PASS (3 expectations)`. Nu geven
beide `CASE ERROR: asOfDate "2026-02-30" is not the uchar-case-v0 date
lexeme yyyy-mm-dd (day out of range in date lexeme "2026-02-30")` met
rc 2. Handberekening in de NOTE: als string ligt "2026-02-30" in het
venster 2026-01-01..2026-12-31 en na 2025-12-31, dus 100 + 25 = 125 en
leeg.

### Het Drools-been

```
-- DroolsRoodMain (rood document), huidige jar
FAULT IllegalArgumentException: malformed date '2026-02-30' on 'asOfDate': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')
rc=0
-- exec-tak (groene kopie)
ERROR IllegalArgumentException: malformed date '2026-02-30' on 'asOfDate': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')
rc=3
-- DRL-tekst-tak (groene kopie)
ERROR IllegalArgumentException: malformed date '2026-02-30' on 'asOfDate': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')
rc=3
-- beide takken op het rode document zelf
PLAYED 0
```

De rode runner geeft een FAULT met een eigen zin, dus die zin is
gepind: `"drools": "malformed date '2026-02-30' on 'asOfDate': the
field widths are right but the calendar is not"`. De pin stopt vóór de
haakjes, want dat deel is de parseertekst van de JDK. Geen ERROR, dus
geen uitsluiting en geen open punt over een crash. Waarom de twee
runners verschillen: `runRood` bouwt eerst de KieBase en leest dan de
case binnen de `catch (RuntimeException)` die ook fire omvat. De
datumlezer werpt een `IllegalArgumentException` (volgens de broncode
een documentgebrek en nooit een `Excluded`), dus rood wordt het FAULT.
De replay-runner vangt dezelfde uitzondering pas in `main` als ERROR.
Op het rode document zelf slaat de replay de case over (`PLAYED 0`).
Gate 8 stuurt alleen de exec-tak aan. De DRL-tekst-tak weigert hier
via dezelfde gedeelde caselezer, maar alleen de replay laat dat zien.

Rood-eerst voor de Drools-pin: dezelfde runner op een jar uit `git
archive 52bb4124` (vlak vóór `b6c8bf21` van 2026-09-10, dat asOfDate
door de benoemde lezer leidde; `mvn -o package`, `Tests run: 244`):

```
FAULT DateTimeParseException: Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30' <- DateTimeException: Invalid date 'FEBRUARY 30'
fragment AFWEZIG -> het been zou falen: Drools faulted but the pinned fragment is absent
```

De pin wijst dus de benoemde weigering van de backend aan, niet om het
even welke parseerfout. Op de jar van `8b685402` is de uitvoer gelijk
aan de huidige.

Gate 8 vóór de beslissing over het been (beide documenten aanwezig,
nog zonder Drools-pin, zonder uitsluiting): exit 1, precies twee
FAIL-regels, waarvan deze:

```
FAIL: asofdate-kalender carries no drools fragment and is not in the Drools exclusion list — pin a fragment or list it with a reason:
FAULT IllegalArgumentException: malformed date '2026-02-30' on 'asOfDate': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')
```

Erna: `check-rood: Drools leg PASS (10 fault-pinned, 35 not-carried
with reason)` en `check-rood: PASS (45 red cases …; 90 pinned fragments
cite none of the 36 listed bottom sentences)`.

### (b) Kan het case-schema de kalender leren? Kan wel, doen we niet.

Technisch kan het. Een patroon van 213 tekens met de maandlengtes en de
Gregoriaanse schrikkelregel (dezelfde als `is_leap` in ref) is gemeten
tegen 4.620.000 lexemen (jaren 0000–9999 × maanden 00–13 × dagen
00–32) en week 0 keer af. `spec/schema` blijft toch ongewijzigd, om
vier redenen:

1. Voor de IR-draad is de lijn al getrokken. In IR-SPEC ch. 4.1 staat
   "Two layers, and only one of them is a shape … A calendar is not a
   shape". De beschrijving van `Geldigheid` in het IR-schema zegt "A
   pattern cannot check the calendar; '2026-02-30' matches here and is
   refused one field deeper", en de zelftest van gate 2 pint dat lexeem
   als laaggrens. De casedraad volgt dezelfde snede, met de caselader
   als tweede laag. Een kalenderpatroon zou de twee draden verschillend
   snijden.
2. De lader moet de poort toch houden. Het schema ziet bestanden, de
   lader elke waarde langs elke route (generator, handmatige run).
   Een kalender in een regex is een derde kalender naast
   `date_of_iso` en Drools' `LocalDate`, en die drie moeten dan gelijk
   blijven.
3. De getuige zou geen plek meer hebben. Gate 4 valideert elke rode
   case, en `corpus/negatief` bevat IR voor de verifier, geen cases.
   Een "negatief-achtig terrein" voor cases bestaat niet: het commentaar
   van gate 2 noemt de tien patronen van uchar-case-v0 "gate 4's to
   close, on a card of its own". De weigering zou dan weer alleen in de
   suites gepind zijn.
4. `format: "date"` helpt niet. Draft 2020-12 behandelt format als
   annotatie, en gate 4 valideert zonder format checker.

### Gates

Elk los, na `eval $(opam env)`, dezelfde runs als in de zusterkaart: 1
PASS · 2 PASS (280 bestanden, vloer 192) · 3 PASS (280 schoon; 440
negatief) · 4 PASS (640 cases) · 5 PASS (806; 595 cases) · 6 PASS
(523; 595 cases) · 7 PASS (559; 36) · 8 PASS (45 rood; 90 fragmenten;
Drools 10/35) · 9 PASS (201; 12) · 10 PASS (409; 70) · 11 PASS
(29/29/29) · 12 PASS (played 182 = pin, DRL-tekst 161 = pin, JUnit 620)
· 13 PASS (131/128/25; 0 overlevers) · 14 PASS (130; 2). Gate 12 en 13
vegen alleen `corpus/golden`, dus de pins bewogen niet.

### Beschermd pad en may-touch-verbreding

- **Voorstel** `scripts/check-rood.sh`, verbreed volgens de aanwijzing
  van de maintainer ("het Drools-been van check-rood"). Het gaat om
  twee commentaarblokken, geen logica. De kop van het been zegt nu dat
  FAULT één stap vóór fire reikt: de rode run leest de case binnen
  dezelfde vangst als fire, dus een harde weigering bij het lezen (een
  lexeem dat geen dag noemt) is ook FAULT. Daarbij staat hoe "a genuine
  fault at fire" in de redenregels gelezen moet worden. Het commentaar
  in de lus is gelijkgetrokken. De redenregels zelf zijn onaangeroerd.
- Geen bestaand corpusdocument of test gewijzigd.
- Fixronde 1, onder mandaat van de hoofdsessie: **voorstel**
  `spec/schema/uchar-case-v0.schema.json` (twee beschrijvingen,
  `e61e6d18`) en de may-touch-verbreding `backends/drools`, alleen
  javadoc (`ab6711fc`).

### Registertekst (hoofdsessie)

`spec/CONSTRUCTS.md`, rij `Geldigheid`: na "… and the shared case loader
refuses a non-date `asOfDate` at load; witnessed in both suites with a
mutation per position;" invoegen:

"the calendar half of that load refusal is pinned corpus-wide since card
`rood-asofdate-kalender` (2026-09-23) by red case
`corpus/rood/asofdate-kalender` (the vensterheffing document, asOfDate
`2026-02-30`, which the case-schema pattern admits): ref and the ASP
backend refuse it at load, and the Drools red runner refuses it with its
own sentence; before 2026-09-23 ref and the ASP backend answered the 2026
window in silence (vensterheffing 125, overgangsheffing leeg);"

### Open punten

Stand na fixronde 1: 1, 2 en 4 afgehandeld; 3 gaat naar de lopende
Drools-lijn.

1. **Afgehandeld in `e61e6d18` (voorstel).**
   `spec/schema/uchar-case-v0.schema.json`, beschrijving van
   `expectedFault` (beschermd, zone spec): "`drools` against the Drools
   backend's fault-at-fire report" en "present only where the v0 Drools
   backend carries the red-path form and faults at fire". Sinds dit
   document is er een `drools`-pin op een weigering bij het lezen.
   Voorstel: "… against the Drools backend's red-run fault report (a
   fault at fire, or a hard refusal while the red run reads the case)"
   en "… carries the red-path form and faults on it".
2. **Afgehandeld in `ab6711fc`** (alleen commentaartekst).
   `backends/drools` (zone van een parallelle kaart), javadoc. In
   `CaseRunner.runRood` staat "A fault the loader or a case-input form
   raises before fire is an EXCLUDE (the form is not carried); a fault
   raised AT fire … is the FAULT this gate leg pins", en in de tabel van
   `DroolsRoodMain` "FAULT … a genuine runtime fault at fire". Gemeten
   hierboven: een harde weigering bij het lezen van de case is FAULT.
   Alleen een `Excluded` wordt EXCLUDE. Voorstel voor een Drools-kaart:
   de javadoc zegt "a form the backend does not carry (an `Excluded` or
   a loader refusal) is an EXCLUDE; anything else raised after the
   build, while reading the case or at fire, is a FAULT".
3. **Naar de lopende Drools-lijn** (hoofdsessie, 2026-09-23; niet in
   deze branch). Drools-zone, geen crash: de replay-runner meldt deze
   benoemde weigering onder hetzelfde token als een echte crash, in
   beide takken. Letterlijk, op een groene kopie van dit document (de
   replay slaat een `expectedFault`-case over):

   ```
   -- exec-tak (groene kopie)
   ERROR IllegalArgumentException: malformed date '2026-02-30' on 'asOfDate': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')
   rc=3
   -- DRL-tekst-tak (groene kopie)
   ERROR IllegalArgumentException: malformed date '2026-02-30' on 'asOfDate': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')
   rc=3
   ```

   De oorzaak: `DroolsBackendMain.main` vangt elke
   `RuntimeException` als `ERROR`, en de datumlezer werpt een
   `IllegalArgumentException`. In gate 12 speelt het niet, want een
   golden met zo'n datum is al rood in gate 5 (`CASE ERROR`, rc 2).
4. **Afgehandeld in `e61e6d18` (voorstel)**, met een eigen formulering
   (zie Fixronde 1). `spec/schema/uchar-case-v0.schema.json`,
   beschrijving van `asOfDate` (alleen de beschrijving, geen validatie;
   zone spec). Voorstel, naar
   het voorbeeld van `Geldigheid` in het IR-schema: "The pattern is a
   shape test and cannot check the calendar: '2026-02-30' matches here
   and is refused one step later, by every executor's case reader at
   load (red witness `corpus/rood/asofdate-kalender`)."

### Fixronde 1 (hoofdsessie, 2026-09-23)

Drie open punten opgelost, onder mandaat van de hoofdsessie, in twee
commits:

- `e61e6d18`, **beschermd-pad-voorstel**
  `spec/schema/uchar-case-v0.schema.json`, alleen
  beschrijvingstekst. Gemeten: de validatiesleutels zijn gelijk
  (`json.load` van oud en nieuw, zonder de descriptions: identiek).
  - `expectedFault`: `drools` wordt gematcht "against the Drools
    backend's red-run fault report: a fault at fire, or a hard refusal
    while that run reads the case (a date lexeme that names no day)",
    en is aanwezig "where the v0 Drools backend carries the red-path
    form and faults on it". Eerst stond er twee keer "at fire".
  - `asOfDate`: "The pattern checks the field widths, not the calendar:
    '2026-02-30' matches it. The calendar is checked one layer deeper,
    by each executor's case reader, which refuses a lexeme that names no
    day when it reads the case (red witness:
    corpus/rood/asofdate-kalender)." Er staat bewust niet "a pattern
    cannot check the calendar", want dat kan wel (zie (b)). Er staat
    wat dit patroon doet.
- `ab6711fc`, **may-touch-verbreding** `backends/drools`, alleen
  commentaartekst in `CaseRunner.java` (de javadoc van `RoodResult` en
  `runRood`) en `DroolsRoodMain.java` (de FAULT-regel in de tabel).
  Nu staat er: EXCLUDE is een loaderweigering of een `Excluded` uit de
  bouw of uit een case-invoervorm. Elke andere runtime-uitzondering na
  de bouw is FAULT, dus ook een harde weigering bij het lezen van de
  case. `RoodResult` viel buiten de letter van het mandaat ("runRood
  en DroolsRoodMain"), maar het is het resultaattype van `runRood` en
  zei hetzelfde "fault at fire", dus het is meegenomen. Gemeten dat
  het alleen commentaar is: beide bestanden met het commentaar
  weggestript zijn gelijk aan HEAD. Tijdens het werk raakte geen
  andere worktree deze twee bestanden (committed of dirty, alle zes
  gemeten).

Gates na de fixronde, elk los na `eval $(opam env)`: 1 PASS · 2 PASS
(280 bestanden, vloer 192) · 4 PASS (640 cases) · 8 PASS (45 rood; 90
fragmenten; Drools-been 10/35) · 12 PASS (played 182 = pin, DRL-tekst
161 = pin, JUnit 620: de suite compileert en draait na de javadoc).
