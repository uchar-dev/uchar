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
`corpus/rood/asofdate-kalender` (commit `e03dcc30`): `ir.json` is golden
`jutterswet-vensterheffing` byte voor byte (`cmp`), de case is de input
van die golden (M1, meldwaarde 100) met `asOfDate` `2026-02-30`, en
`expectedFault` pint alle drie de uitvoerders. De commentaarregels van
het Drools-been staan in `8f24ad3d` (**beschermd-pad-voorstel**, zie
onder).

### Waarom 2026-02-30

Het sterkste argument (lensadvies, fixronde 2): alleen dit lexeem pint
de maandlengtetabel. Vóór 6ff39603 antwoordde de oude lader ook op
`2026-13-01` stil (gemeten op een variant van dit document: oud ref en
oud clingo `PASS (3 expectations)` met beide velden leeg). Een mutant
van de gedeelde datumlezer die de dag tegen 31 toetst in plaats van
tegen de maandlengte, laat `2026-02-30` door en weigert `2026-13-01`
nog steeds:

```
== mutant E1, asofdate-kalender (2026-02-30)
EXPECTED FAULT DID NOT OCCUR: evaluation succeeded
EXPECTED FAULT DID NOT OCCUR: clingo found a model
== mutant E1, variant 2026-13-01
oracle: expected fault observed at load (asOfDate "2026-13-01" is not the uchar-case-v0 date lexeme yyyy-mm-dd (month out of range in date lexeme "2026-13-01"))
clingo: expected fault observed at load (asOfDate "2026-13-01" is not the uchar-case-v0 date lexeme yyyy-mm-dd (month out of range in date lexeme "2026-13-01"))
```

De maandgrens en de schrikkelregel pint dit document niet; die staan
alleen in de ref-suite (`WV-3.10-caselader-weigert-maand-13-asofdate`,
`WV-3.10-caselader-weigert-29-februari-asofdate`).

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
de lezer, want die reden ("day out of range") is de maandlengtetoets
waarvoor dit document er is. Het is één lezer die twee
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
00–32) en week 0 keer af.

Reproduceerbaar vastgelegd (lensadvies, fixronde 2 punt 7). Het patroon,
213 tekens, in de vorm die een JSON-schema `pattern` zou dragen:

```
^(?:[0-9]{4}-(?:(?:0[13578]|1[02])-(?:0[1-9]|[12][0-9]|3[01])|(?:0[469]|11)-(?:0[1-9]|[12][0-9]|30)|02-(?:0[1-9]|1[0-9]|2[0-8]))|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:[02468][048]|[13579][26])00)-02-29)$
```

Het toetsscript, letterlijk (Python 3, alleen de standaardbibliotheek;
de schrikkelregel is die van `is_leap` in `ref/lib/value.ml`):

```python
import re
# a pattern that knows the proleptic Gregorian calendar for four-digit years
cal = (r'^(?:[0-9]{4}-(?:(?:0[13578]|1[02])-(?:0[1-9]|[12][0-9]|3[01])'
       r'|(?:0[469]|11)-(?:0[1-9]|[12][0-9]|30)'
       r'|02-(?:0[1-9]|1[0-9]|2[0-8]))'
       r'|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:[02468][048]|[13579][26])00)-02-29)$')
rx = re.compile(cal)
def leap(y): return y % 4 == 0 and (y % 100 != 0 or y % 400 == 0)
def dim(y, m): return [31, 29 if leap(y) else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m-1]
bad = 0; n = 0
for y in range(10000):
    for m in range(0, 14):
        for d in range(0, 33):
            s = f'{y:04d}-{m:02d}-{d:02d}'
            want = 1 <= m <= 12 and 1 <= d <= dim(y, m)
            got = rx.match(s) is not None
            n += 1
            if want != got: bad += 1
print('pattern length', len(cal), 'chars; lexemes compared', n, '; disagreements', bad)
for s in ['2026-02-30', '2025-02-29', '2028-02-29', '1900-02-29', '2000-02-29', '2026-13-01']:
    print(s, 'admitted' if rx.match(s) else 'refused')
print(cal)
```

Uitvoer:

```
pattern length 213 chars; lexemes compared 4620000 ; disagreements 0
2026-02-30 refused
2025-02-29 refused
2028-02-29 admitted
1900-02-29 refused
2000-02-29 admitted
2026-13-01 refused
```

(De laatste `print` van het script geeft daarna het patroon zelf,
hierboven al letterlijk.) De toets vergelijkt het patroon met de
Gregoriaanse regel over alle jaren 0000–9999, maanden 00–13 en dagen
00–32, dus ook net buiten elk bereik. Een ECMA-262-motor (het
regex-dialect van JSON Schema) kent dezelfde constructies: groepen
`(?:…)`, tekenklassen en ankers; dat is niet apart gemeten.

`spec/schema` blijft toch ongewijzigd, om vier redenen:

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
  van de maintainer ("het Drools-been van check-rood"). **Vervangen,
  historisch:** dit beschrijft het commentaar uit de eerste ronde, dat
  een scheiding naar fase beschreef. De lens vond die in fixronde 2
  onjuist. Het huidige commentaar (fixronde 2 en 4) deelt in naar
  uitzonderingstype en legt de pinregel vast; zie die secties. Destijds
  stond er: twee commentaarblokken, geen logica. De kop zei dat FAULT
  één stap vóór fire reikt, want de rode run leest de case binnen
  dezelfde vangst als fire, zodat een harde weigering bij het lezen ook
  FAULT is. Daarbij stond hoe "a genuine fault at fire" in de
  redenregels gelezen moest worden.
- Geen bestaand corpusdocument of test gewijzigd.
- Fixronde 1, onder mandaat van de hoofdsessie: **voorstel**
  `spec/schema/uchar-case-v0.schema.json` (twee beschrijvingen,
  `87072df7`) en de may-touch-verbreding `backends/drools`, alleen
  javadoc (`a1b9d590`).
- Fixronde 2: dezelfde drie bestanden opnieuw, alleen tekst
  (`662d75a1` voorstel, `52c642cc` javadoc, `3789e7ce` voorstel), plus
  de NOTE van dit document (`a0db6321`, `2c62ff9e`).
- Fixronde 4: dezelfde drie bestanden nog eens, alleen tekst
  (`450e90fe` voorstel, `8d800461` javadoc, `4a830f4e` voorstel); zie
  de sectie Fixronde 4.

### Registertekst (hoofdsessie)

`spec/CONSTRUCTS.md`, rij `Geldigheid`: na "… and the shared case loader
refuses a non-date `asOfDate` at load; witnessed in both suites with a
mutation per position;" invoegen:

"the month-length check of that load refusal is pinned corpus-wide since
card `rood-asofdate-kalender` (2026-09-23) by red case
`corpus/rood/asofdate-kalender` (the vensterheffing document, asOfDate
`2026-02-30`, which the case-schema pattern admits): ref and the ASP
backend refuse it at load, and the Drools red runner refuses it with its
own sentence; a reader that checks the day against 31 instead of the
length of its month admits the lexeme and fails the case (measured),
while the month range and the leap rule stay pinned in ref's suite
alone; before 6ff39603 (card `ref-geldigheid-datumlezing`) ref and the
ASP backend answered the 2026 window in silence (vensterheffing 125,
overgangsheffing leeg);"

### Open punten

Stand na fixronde 2: 1, 2 en 4 afgehandeld; 3 en 5 gaan naar de
lopende Drools-lijn.

1. **Afgehandeld in `87072df7` (voorstel).**
   `spec/schema/uchar-case-v0.schema.json`, beschrijving van
   `expectedFault` (beschermd, zone spec): "`drools` against the Drools
   backend's fault-at-fire report" en "present only where the v0 Drools
   backend carries the red-path form and faults at fire". Sinds dit
   document is er een `drools`-pin op een weigering bij het lezen.
   Voorstel: "… against the Drools backend's red-run fault report (a
   fault at fire, or a hard refusal while the red run reads the case)"
   en "… carries the red-path form and faults on it".
2. **Afgehandeld in `a1b9d590`** (alleen commentaartekst).
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
4. **Afgehandeld in `87072df7` (voorstel)**, met een eigen formulering
   (zie Fixronde 1). `spec/schema/uchar-case-v0.schema.json`,
   beschrijving van `asOfDate` (alleen de beschrijving, geen validatie;
   zone spec). Voorstel, naar
   het voorbeeld van `Geldigheid` in het IR-schema: "The pattern is a
   shape test and cannot check the calendar: '2026-02-30' matches here
   and is refused one step later, by every executor's case reader at
   load (red witness `corpus/rood/asofdate-kalender`)."

5. **Naar de lopende Drools-lijn, samen met punt 3** (lensadvies,
   fixronde 2 punt 8; geen kaartje). De Drools-caselezer behandelt
   casegebreken op drie manieren: als `Excluded` (een vorm die de backend
   niet draagt), als naamloze crash, of als benoemde weigering. De rode
   run deelt in naar uitzonderingstype, dus het eerste wordt EXCLUDE, en
   het tweede en het derde worden FAULT. ref en clingo weigeren elk van
   deze gebreken bij het laden met een benoemde `CASE ERROR`. Gemeten op
   2026-09-23 met de huidige jar, telkens één gebrek in een case op het
   vensterheffing-document (DroolsRoodMain; ref ter vergelijking):

   ```
   === meldwaarde als {"boolean": true}
   drools: FAULT IllegalArgumentException: argument type mismatch
   ref:    CASE ERROR: M1.meldwaarde: value variant "boolean" does not match the declared datatype
   === kenmerk "verzonnen" niet gedeclareerd
   drools: FAULT RuntimeException: kon setter setVerzonnen(boolean) niet aanroepen op Vondstmelding <- NoSuchMethodException: io.github.yhavinga.regelspraak.facts.Vondstmelding.setVerzonnen(boolean)
   ref:    CASE ERROR: characteristic "verzonnen" is not declared on "Vondstmelding"
   === attribuut "verzonnen" niet gedeclareerd
   drools: EXCLUDE case sets undeclared attribute 'verzonnen' on Vondstmelding
   ref:    CASE ERROR: attribute "verzonnen" is not declared on "Vondstmelding"
   === rolsleutel "melder" (niet gedeclareerd) naar label X9 (onbekend)
   drools: EXCLUDE role 'melder' names unknown label X9
   ref:    CASE ERROR: role key "melder" does not resolve to a declared role opposite "Vondstmelding"
   === asOfDate 2026-02-30 (dit document)
   drools: FAULT IllegalArgumentException: malformed date '2026-02-30' on 'asOfDate': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')
   === corpus/rood/unknown-role-target-label
   drools: EXCLUDE role 'liggers' names unknown label S9
   ```

   Drie dingen vallen op. Twee casegebreken geven een FAULT zonder eigen
   zin, alleen platformtekst; een pin daarop zou een crash pinnen (zie de
   pinregel in de kop van het Drools-been van gate 8). Twee andere
   gebreken worden gemeld als een vorm die de backend niet draagt,
   terwijl het documentgebreken zijn. En bij een onbekende rolsleutel
   noemt de Drools-melding het label en niet de sleutel. De
   datumlezer laat zien hoe het wel kan: een benoemde weigering met
   positie en lexeem, als documentgebrek (`IllegalArgumentException`),
   nooit als `Excluded`. Voorstel voor de Drools-lijn: behandel elk
   casegebrek zo. Samen met punt 3 (het ERROR-token in de replay-runner)
   geeft dat één regel: een casegebrek is een benoemde weigering,
   in de rode run en in de replay.

### Fixronde 1 (hoofdsessie, 2026-09-23)

Drie open punten opgelost, onder mandaat van de hoofdsessie, in twee
commits:

- `87072df7`, **beschermd-pad-voorstel**
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
- `a1b9d590`, **may-touch-verbreding** `backends/drools`, alleen
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

### Fixronde 1b (hoofdsessie, 2026-09-23)

Geen wijziging binnen deze kaart. Fixronde 1b loste de bijvangst uit de
zusterkaart op: de ch.-1.2-zin in `spec/RUNTIME-SEMANTICS.md` noemt
voor de genegeerde heel-tijdvak-leegtest nu alle drie de weigeraars en
de twee rode documenten (`3b628a2b`, voorstel; details in
`20260923-rood-heeltijdvak-ongenegeerd.md`). Gate 1 opnieuw: PASS.
Open punt 3 hier (het ERROR-token van de replay-runner) staat
ongewijzigd en gaat naar de Drools-lijn.

### Fixronde 2 (lens: EERST FIXEN, 2026-09-23)

De punten van deze kaart staan hier. Punt 2, 3 en 6 staan in het Result
van de zusterkaart.

- **Punt 1, blokkerend.** De tekst die ik in fixronde 1 schreef (kop
  van het Drools-been in `8f24ad3d`, javadoc `a1b9d590`, schema
  `87072df7`), zei dat FAULT "een fout bij fire of een harde weigering
  bij het lezen" is. De code deelt echter in naar uitzonderingstype,
  zo sinds `e1f779aa` (2026-08-27). Een loaderweigering of een
  `Excluded` is EXCLUDE. I/O en elke uitzondering tijdens het laden van
  de IR en de bouw van de KieBase zijn ERROR. Elke andere
  runtime-uitzondering na de bouw is FAULT, ook een crash bij het lezen
  van de case. Opnieuw gemeten met de huidige jar (zie open punt 5):
  een boolean in meldwaarde geeft `FAULT IllegalArgumentException:
  argument type mismatch`; een niet-gedeclareerd kenmerk geeft `FAULT
  RuntimeException: kon setter setVerzonnen(boolean) niet aanroepen
  …`; een niet-gedeclareerd attribuut en een onbekend label geven
  EXCLUDE. Hersteld, alleen tekst:
  - `662d75a1` (gate 8, voorstel): de kop beschrijft de indeling naar
    type, met de metingen, en legt de pinregel vast: een
    `drools`-fragment citeert de eigen zin van de backend (positie en
    lexeem), nooit platformtekst. De herlezing van "a genuine fault at
    fire" is geschrapt. De twee luscommentaren zeggen niet meer dat een
    crash ERROR is.
  - `52c642cc` (javadoc, alleen commentaar, code zonder commentaar
    gelijk aan HEAD gemeten): `RoodResult`, `runRood` en de tabel van
    `DroolsRoodMain` zeggen hetzelfde. ERROR is nu "bad invocation, I/O,
    or an exception while the IR is loaded and the KieBase built — a
    crash AFTER the build is a FAULT, not an ERROR".
  - `3789e7ce` (case-schema, voorstel, validatiesleutels gelijk
    gemeten): de `drools`-beschrijving noemt de indeling naar type, de
    crash als FAULT en de pinregel.
  - `f210c278`: dezelfde onjuiste premisse in oudere redenen en NOTEs
    (zie de zusterkaart).
  Dit document voldoet aan de pinregel: het pint de eigen zin tot vóór
  de haakjes met de JDK-parseertekst.
- **Punt 4** (`a0db6321`): de NOTE beschrijft vensterheffing nu als
  twee los gedateerde regels met eigen namen en eigen doelattributen,
  dus een venster per regel en geen versieselectie (IR-SPEC ch. 4.1).
  De handberekening spreekt van vensters die de datum toelaten of
  uitsluiten.
- **Punt 5** (`2c62ff9e`): het sterkste argument voor `2026-02-30`
  staat nu in de NOTE en onder "Waarom 2026-02-30" hierboven, met de
  metingen. De oude lader antwoordde ook op `2026-13-01` stil, maar een
  mutant `d > 31` laat alleen `02-30` door. De CONSTRUCTS-tekst spreekt
  van "the month-length check" in plaats van "the calendar half",
  zegt dat maandgrens en schrikkelregel alleen in de ref-suite gepind
  zijn, en noemt `6ff39603` in plaats van "before 2026-09-23".
- **Punt 7** (`9edcb65f`): het patroon van 213 tekens, het toetsscript
  en de uitvoer staan letterlijk onder (b).
- **Punt 8** (`c1f7bdbf`): open punt 5, voor de Drools-lijn, naast
  punt 3.

Gates na fixronde 2: dezelfde runs als in de zusterkaart. 1 PASS · 2
PASS (281) · 3 PASS (281 / 440) · 4 PASS (641) · 8 PASS (46 rood, 92
fragmenten, Drools 10/36) · 12 PASS (182 / 161, JUnit 620).

### Fixronde 3 (hoofdsessie, 2026-09-23)

Rebase op main `3a140fe4` zonder conflict. Commit-hashes in dit Result
verwijzen naar de branch na de rebase (per commitonderwerp omgezet).
Binnen deze kaart veranderde niets. In dezelfde branch kwamen de
vierde heel-tijdvak-tweeling (zusterkaart) en de kaart
`wf022-waardeclausule-getuige` (eigen Result) mee. Gates na fixronde 3:
1 PASS · 2 PASS (282) · 3 PASS (282 / 441) · 4 PASS (642) · 8 PASS (47
rood, 94 fragmenten, Drools 10/37) · 12 PASS (182 / 161, JUnit 620).
Open punten 3 en 5 (voor de Drools-lijn) staan ongewijzigd.

### Fixronde 4 (lens ronde 2, 2026-09-23)

- **Punt 1, blokkerend.** De tekst uit fixronde 2 zei op drie plekken
  "elke runtime-uitzondering na de bouw is FAULT", zonder de
  uitzondering voor `Excluded`: de schemabeschrijving, de javadoc van
  `RoodResult` en de FAULT-regel in de tabel van `DroolsRoodMain`. Het
  schema sloot bovendien de EXCLUDE van de caselezer nergens uit. Nu
  staat overal de volledige indeling: `Excluded` (ook uit de
  caselezer) is EXCLUDE; IOException en fouten tijdens IR-laden en bouw
  zijn ERROR; elke andere runtime-uitzondering na de bouw is FAULT; en
  de fire-cap zonder fixpunt is een FAULT zonder uitzondering. De
  pinregel eist positie en lexeem alleen bij een weigering tijdens het
  lezen van de case: "Breuk deling door 0", "deling door 0" en "geen
  fixpunt (kringverwijzing?)" zijn eigen zinnen zonder die twee. Deze
  pin (`malformed date '2026-02-30' on 'asOfDate': …`) is wel zo'n
  weigering en noemt beide. Commits: `450e90fe` (gatekop, voorstel),
  `8d800461` (javadoc) en `4a830f4e` (schema, voorstel).
- **Punt 6** (`c9331aa1`): de bullet "Beschermd pad" beschreef het
  commentaar uit de eerste ronde nog als huidige stand. Hij is nu als
  vervangen gemarkeerd.

Gates na fixronde 4: 1 PASS · 2 PASS (282) · 3 PASS (282 / 441) · 4
PASS (642) · 8 PASS (47 rood, 94 fragmenten, Drools 10/37) · 12 PASS
(182 / 161, JUnit 620). Open punten 3 en 5 (voor de Drools-lijn) staan
ongewijzigd.
