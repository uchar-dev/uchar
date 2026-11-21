# open-values-in-drools: Drools draagt een document dat uit leeg niets afleidt

*Opgesteld door de hoofdsessie op 2026-11-21. De lezing staat in `spec/RUNTIME-SEMANTICS.md` ch. 1.5 (de standen, de
open lezing, het oordeel als paar, de drie uitkomsten van een schrijver, de sluitingen, de twee eisen, de vormen zonder
lezing), ch. 5.12 ("Open values": de lezing aan het fixpunt) en ch. 5.12a (de eindstand); de referentie-interpreter
rekent haar (`backlog/done/20261121-open-values-in-ref.md`). De Drools-backend weigert zo'n document nu met
DROOLS-370.*

## Aanleiding

Een regelset kan kiezen dat uit een lege waarde niets volgt (`inferFromEmpty: false`). Een lid is dan gevuld,
vaststaand leeg of open; wat een open waarde leest is open; een voorwaarde die een open waarde leest is onbepaald,
tenzij een ander deel haar beslist. De Drools-backend is de executor die de distributie en de werkbank draaien: zolang
hij zo'n document weigert, kan geen regelset de keuze gebruiken.

Een voorbeeld uit de Jutterswet: "Een Vondst is kostbaar indien zijn taxatie groter is dan 1000", "Het vindloon van
een Vondst is 10 indien hij kostbaar is", "De bewakingstoeslag van een Vondst is 5 indien hij niet kostbaar is". Taxatie
50: kostbaar staat vast als niet, het vindloon is vaststaand leeg, de bewakingstoeslag is 5. Zonder taxatie: kostbaar is
open, vindloon en bewakingstoeslag zijn open, en geen van beide regels vuurt.

## Goal

`backends/drools` bouwt en speelt een document met `inferFromEmpty: false` volgens ch. 1.5 op beide weergaven (het
exec-model en de DRL-tekst, uit één plan), de goldens met de eigenschap spelen in gate 12 met de verwachtingen die
gate 5 en 6 vastleggen, hun lezingen en eindstand zijn in gate 17 woord voor woord die van ref, en wat de backend van
zo'n document niet draagt, weigert hij bij naam met de slug van de vorm.

## De architectuur (bindend)

**Drools volgt de opzet van ref.** In ref is "open" een derde stand van een lid naast gevuld en vaststaand leeg, die
ontstaat waar een schrijver niet beslist, en breekt een open lezing de evaluatie af tot één atomaire voorwaarde of één
waarde. Die opzet past op de verlaging met truth maintenance:

1. **OPEN is een derde stand van de standing.** `State` (een attribuut) krijgt naast een waarde en leeg de stand open;
   `CharacteristicState` wordt geldt, staat vast dat niet, open. Een lid is vaststaand leeg waar geen waarde staat en
   niets het open laat: per object, zonder de schrijvers van een lid in één regel samen te brengen.
2. **Een logisch feit per schrijver die niet beslist.** Waar een schrijver op een object noch beslist, noch vaststaand
   niets vult, voegt hij logisch een feit in (object, lid, schrijver). De generieke standing-regels lezen het: een
   waarde → gevuld; anders zo'n feit → open; anders de invoer of vaststaand leeg. Truth maintenance trekt in wat op een
   tijdelijke stand rustte, zoals zij nu al doet voor de ontkenning van een kenmerk.
3. **De grens ligt in de Java-evaluatie van de term.** Een term die een open standing leest, breekt af; de grens vangt
   dat in de functie van een atomaire voorwaarde en in die van de waarde van een consequentie, en houdt de deponeringen
   van binnen de grens tegen zoals ch. 1.5 het onder "An open read" zegt. De operatoren van de productieruntime
   (`RegelSpraakRuntime`) veranderen niet: geen strikte varianten, geen nieuwe namen daar.
4. **Eén functie in de analyse maakt van een voorwaarde de drie uitkomsten** (beslist, vult vaststaand niets, laat
   open), en elke plaats in `TmsTranspiler` die een voorwaardeterm neemt en er een test van bouwt, gaat erdoor. Het
   onafhankelijke oordeel telde negentien zulke plaatsen; negentien eigen vertalingen van dezelfde driedeling zijn
   uitgesloten. Het paar van ch. 1.5 (het oordeel voor de vorm zelf en voor een ander) is één evaluatie die beide
   geeft, geen twee.
5. **De stapel wacht op een positief feit.** Een lagere default beslist alleen waar van elke hogere vaststaat dat hij
   voorrang geeft; `not Decides` en `not Outcome` over de hogere defaults laten een lagere beslissen onder een hogere
   die open is, en dat mag in een document met de eigenschap niet.
6. **De invoer "niet genoemd" is een feit naast de bean.** Een kenmerk op een bean is een primitieve boolean en kan
   "niet genoemd" niet van "onwaar" scheiden. De case-invoer zegt per object welke invoerleden open zijn (en welke
   kenmerken in `characteristicsAbsent` staan) met feiten naast de beans; de beans en hun API veranderen niet.
7. **Eén plan, twee weergaven.** Wat de analyse beslist, renderen het exec-model en de DRL-tekst zonder eigen keuze;
   de statusregel per golden is op beide gelijk (gate 12), en `BranchDumpsCorpusWitnessTest` houdt ze case voor case
   gelijk.
8. **Eén lezing.** De eigenschap `inferFromEmpty` wordt gelezen waar de case-invoer bepaalt welke afwezige invoer open
   is, en waar de backend weigert wat hij niet draagt; de analyse heeft geen tweede pad voor documenten met de
   eigenschap. In een document zonder de eigenschap is niets open: het bouwt dezelfde regels als nu (de DRL-tekst van
   elke golden byte-gelijk vóór en na, of het verschil per regel verklaard en in elk document gelijk), en gate 8, 12,
   13, 15 en 17 blijven groen zonder dat een verwachting, pin of lijst verandert buiten de goldens met de eigenschap.

Afwijken van 1–6 mag met de reden en de meting in het Result, zolang 7 en 8 blijven staan.

## Bouwdelen

Elk deel een verse bouwer; elk deel eindigt groen, met een Result-sectie en een overdracht.

1. **De kern.** De caselezer voor `characteristicsAbsent`, `attributeOpen` en `characteristicOpen`; de invoerfeiten;
   de drie standen van de standing; het feit per schrijver die niet beslist; de grens in de evaluatie van een term; de
   ene functie voor de drie uitkomsten; de atomaire voorwaarden, de toets op leeg en gevuld met haar sluiting, de
   samengestelde voorwaarde met de tabel van ch. 1.5 en haar leesregel; de regel (gelijkstelling, berekening,
   kenmerktoekenning, initialisatieregel); de consistentieregel; de lezing over een link; `Let`; de weigering bij naam
   van elke andere vorm.
2. **De stapel, de vouwen, de creaties.** De defaults-groep in haar eenvoudige en algemene vorm met het positieve
   "geeft voorrang", de initialisatie als sluiting, de opgave met het veld `complete` van de declaratie, de
   kenmerkstapel, de uitzonderingsgroepen; de kwantorvouw; de vouwen over een verzameling en de sluiting binnen een
   waarde; `IfThenElse`; object- en feitcreatie met een open relatie.
3. **De lezingen en de eindstand.** Ch. 5.12 "Open values" en ch. 5.12a op beide weergaven, woord voor woord die van
   ref (gate 17): de status `OPEN`, `undetermined`, `closure`, de open lezing `open <object> "<lid>"`, `unreached` na
   een open lezing, de stapel onder een open default, de `open`-regels van de eindstand. De goldens met de eigenschap
   uit de kanaallijsten van gate 12 en gate 8.
4. **Het brede differentieel en de staart.** Behoud bij volledige kennis door Drools (elke golden die de backend
   speelt, met de eigenschap erbij en elk invoerlid volledig, speelt haar eigen cases) en het differentieel met open
   leden tegen ref op de eindstand (de runs van de poot voor monotonie van gate 5), als poot binnen een tijdsbudget dat
   de hoofdsessie vaststelt; de README; het Result van de kaart; wat de distributie bij haar pin moet weten.

## Acceptance (deel 1; de volgende delen krijgen de hunne in hun opdracht)

Rood eerst per test.

- **De getuigen van ref spelen in Drools.** Elke getuige onder `ref/test/fixtures/strict-empty/` waarvan de backend de
  vormen in dit deel draagt, speelt in een JUnit-test op beide weergaven met dezelfde casebestanden en dezelfde
  verwachtingen (de test leest de fixtures op hun plaats; geen kopie). Elke andere getuige weigert met DROOLS-370 en de
  slug van haar vorm, en de test noemt per getuige welke van de twee. Twee getuigen (`closed-input`,
  `input-above-computation`) wachten op het veld `complete` van de kaart `complete-member`.
- **Elk principe van ch. 1.5 binnen het bereik van dit deel heeft een test** met de naam van het voorbeeld in de spec
  (`WV-1.5-…` in de naam of de javadoc), op het kleinste document uit de Jutterswet, met de uitkomst naast die van ref:
  de drie standen van een invoerlid; een afgeleid lid dat zijn schrijvers volgt (twee schrijvers; een schrijver op een
  subtype); wat vaststaat rekent als ch. 1.2–1.4; een open lezing geeft geen waarde en geen deponering; de voorbeelden
  van het bereik (deeltal, binder, grens: een deponering vóór de open lezing vervalt, een fout ervoor blijft); de
  tabellen van de samengestelde voorwaarde; een lid dat voorbij een onbepaald lid gelezen wordt; de toets op leeg en
  gevuld met haar tabel, ook over een berekende waarde; de eigen ontkenning van een gevuld-toets; de ontkende
  kenmerkcheck; de regel met haar drie uitkomsten; de initialisatieregel als sluiting; de consistentieregel; de lezing
  over een afwezige en over een aanwezige link; invoer naast een schrijver die niet beslist; een lezer die vóór zijn
  schrijver vuurt en door truth maintenance wordt ingetrokken.
- **De ene functie**: het Result noemt haar, en elke plaats in `TmsTranspiler` die een voorwaardeterm neemt met de
  manier waarop zij erdoor gaat of waarom zij in dit deel geweigerd wordt.
- **De weigering**: één plaats somt op wat de backend in een document met de eigenschap niet draagt; de slugs zijn die
  van de tabel "Forms without the reading" van ch. 1.5 waar het om dezelfde vorm gaat, in die volgorde, en een eigen
  slug voor wat ref wel draagt en de backend nog niet. De bouw met lezingen (`DroolsBackend.build(…, readings)`)
  weigert in dit deel een document met de eigenschap met de slug `reading`.
- **Een document zonder de eigenschap verandert niet**: gate 8, 12, 13, 15 en 17 groen zonder gewijzigde verwachting;
  de DRL-tekst van elke golden vóór en na vergeleken, met het aantal documenten en het aantal verschillen in het
  Result.
- Los groen: gate 1, 8, 10 (het register van de drie RGS-codes van de backend) waar geraakt, 12, 13, 15, 17.
  `make gates` draait de hoofdsessie.

## May touch

- `backends/drools/` (analyse, plan, weergaven, runtime-hulpen buiten `runtime/`, caselezer, register en catalogi,
  tests, README);
- `scripts/check-drools.sh`, `scripts/check-red.sh`, `scripts/check-readings.sh` en de scripts van de codes waar een
  golden of rode case van kanaal wisselt of een floor of woordlijst het vraagt, elk in een eigen commit;
- deze kaart.

Niet: `backends/drools/src/main/java/dev/uchar/regelspraak/runtime/` (de productieruntime; gate 1 herkent een kopie aan
haar namen), `ref/`, `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, de verifier, de frontend, `backends/clingo/`, het
corpus. Vind je in ch. 1.5 of 5.12 een zin die de backend niet kan volgen of die twee lezingen toelaat, of een uitkomst
van ref die de spec tegenspreekt, dan is dat een genummerd open punt in het Result met het kleinste document en beide
uitkomsten; je kiest voor de bouw de lezing van ref en gaat door.

Mandaat voor de beschermde paden: de opdracht van de eigenaar van 2026-11-21 voor dit programma. De hoofdsessie leest
na en landt.

## Out of scope

Tijdlijnen en elke andere vorm van de tabel "Forms without the reading"; de scenariolaag van de frontend; de
distributie en haar pin; `spec/CONSTRUCTS.md` en `spec/PLAN.md` (stel de rij voor); `CLAUDE.md` (stel de tekst voor).

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de lezing met drie standen; de sterke tabel van Kleene, ook na een onbepaald lid; een
initialisatie is een sluiting; de wire draagt het document van de auteur en elke executor draagt de lezing zelf; de
juistheid rust op eigen tests per principe en op het corpus; code blijft schoon en to the point: één lezing per
executor.

Van de hoofdsessie (voorlopig): de architectuur hierboven, naar het onafhankelijke oordeel over de opzet in ref
(voorwaarde 6); de lezing in het plan per regel, vóór de twee weergaven.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon). In niets dat in de repository komt
staat een naam, regel of casus van een regelset buiten deze repository.
