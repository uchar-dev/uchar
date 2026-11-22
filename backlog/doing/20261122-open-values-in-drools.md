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

## Result van deel 1: de kern

### Wat er is

Commits op `card/open-values-in-drools`: `da6d38aec` (de kern), `94354c020` (README, getuigen voor `complete` en het deel
per instantie), `e2d7f18e5` (open invoer naast de bean, het oordeel in één regel), `1868bf221` (de zin van DROOLS-370),
`d0a15637a` (gate 12: de reden van twee goldens), `f2762d082` (gate 8: de reden van één rode case), `6a8ad5d57` (een
schrijver vraagt altijd of hij iets doet). De hoofdsessie zette er `8cb33afa8` en `5d3e8f054` (het heap-plafond) vóór.

- **Wat open kan zijn** (`OpenableMembers`): één keer per analyse berekend, leeg in een document zonder de eigenschap.
  Openable is een invoerlid dat één waarde heeft, op de objecten van een type waarop geen regel en geen defaults-groep
  het schrijft, zonder `complete` op zijn declaratie; en elk lid dat een vorm schrijft die een openable lid leest, langs
  elke keten (`ProducerConsumer.forms`). Een relatie en een populatie noemt `forms` niet: leest een creatie een openable
  lid, dan telt elke vorm als een die er een leest.
- **De standen.** Een lid met een schrijver leest door zijn standing: `State` en `CharacteristicState` dragen een waarde,
  leeg of waar/onwaar, of het `OpenMember` van het lid op dat object. Een invoerlid zonder schrijver leest van de bean,
  met het ene feit `OpenInput` van de sessie ernaast; langs een pad leest het door zijn `PathState`, wier regel de bean
  aan het eind van het pad als eigen patroon bindt.
- **De feiten.** `LeftOpen(object, lid, schrijver)`, logisch ingevoegd door een schrijver die niet beslist en van wie
  niet vaststaat dat hij niets vult; `OpenInput`, één gesteld feit per sessie met per object de invoerleden die niet
  volledig zijn (`DroolsBackend.insert`).
- **De grens.** `ComputeHelpers.read` en `readInput` breken een evaluatie af met `OpenRead`; gevangen per lid van een
  samengestelde voorwaarde, per operand van een toets op leeg en gevuld (`Term.ClosureOverOpen`), in de toets van een
  regel en in de consequentie van een waarde, telkens met de deponeringen van binnen de grens teruggezet. De
  productieruntime is niet geraakt.
- **Het oordeel als paar.** Een voorwaarde die voor beide vaststaat, antwoordt; elke andere werpt `Undetermined` met
  haar waarheid voor de vorm zelf. `Composite` is de ene lezing van een samengestelde voorwaarde, in elk document: één
  tabel van Kleene (`Composite.truth`), gelezen tot het oordeel voor een ander staat, een lid na een onbepaald lid
  voorlopig. `ComputeHelpers.established` en `endVerdict`, die de kwantorvouw nog gebruikt, vragen dezelfde tabel.
- **De ene functie** is `Reads.conditionTest(Asked, Ends, …)`: de enige plaats die van een voorwaarde van de IR een
  `TestItem` maakt. `Asked` is `DECIDES`, `ACTS` (een schrijver: hij beslist of laat open), `FAULTS`, `DEPOSITS`;
  `Ends` zegt of wat gecompileerd is kan falen en of het een openable lid leest. Elke andere toets is er een die de vorm
  zelf samenstelt (`VORM`).
- **De drie uitkomsten van een schrijver** staan in één regel: haar functie `oordeel_…` geeft waar (beslist), onwaar
  (vult niets) of niets (laat open); de toets in de linkerkant vraagt of zij iets doet, de consequentie
  (`Consequence.Judged`) of zij beslist of haar `LeftOpen` invoegt. Een regel wier voorwaarde niet onbepaald kan zijn,
  houdt haar toets. De consistentie toetst "geen van" over haar bewering waar die onbepaald kan zijn.
- **De case**: `characteristicsAbsent`, `attributeOpen`, `characteristicOpen`, EF-047, `Input.characteristicAbsent` en
  `Input.closedInputs` (in ref `--closed-inputs`), DROOLS-371 voor een `characteristicAbsent` die niet bij de invoer
  past; `Fixpoint.open` noemt de open leden.
- **De weigering** (`UncarriedForms`): de tabel van ch. 1.5, dan `defaults-group`, `quantifier-fold`, `set-fold`,
  `if-then-else`, `object-creation`, `fact-creation`, dan `reading` (de bouw met lezingen).

### Waar `inferFromEmpty` gelezen wordt

Drie plaatsen onder `backends/drools`: `IrLoader` (de wire in het model), `OpenableMembers.of` (wat open kan zijn; de
enige plaats van de analyse, en via `openableMembers()` ook wat de case-invoer open laat) en `DroolsBackend.build` (de
weigering bij naam).

### Elke plaats in `TmsTranspiler` die een voorwaardeterm neemt

| plaats | hoe |
|---|---|
| `characteristicRule`, `writeRule` (gelijkstelling, berekening, initialisatie) | `ACTS` en `judged` |
| `consistencyRules` | `DECIDES` over "geen van" de bewering waar die onbepaald kan zijn, anders over `NIET` |
| `conditionFaultRule`, `depositRule` | `FAULTS`, `DEPOSITS` |
| `compound` | per lid, met de plaats vanaf waar een lid onbepaald kan zijn |
| `timelineInitialisationRule`, `periodCondition` (tijdlijnen) | `DECIDES`, resp. per periode; geweigerd: `timeline-member` |
| `allocation` | `DECIDES`; geweigerd: `allocation` |
| `objectCreation`, `factCreation` | `DECIDES`; geweigerd: `object-creation`, `fact-creation` |
| de default van een stapel (twee plaatsen) | `DECIDES`; geweigerd: `defaults-group` |
| `contributionRules`, `filterDepositRule`, `filterFaultRule`, de emmerscalair van een lid | `DECIDES`, `DEPOSITS`, `FAULTS`, `VORM`; geweigerd: `set-fold` |
| `quantifierContributions` (filter en matrix) | `DECIDES`, waarde; geweigerd: `quantifier-fold` |
| `choice` (`IfThenElse`) | waarde; geweigerd: `if-then-else` |

Vier plaatsen gaan in een document met de eigenschap door de functie en zijn gedragen (de eerste vier rijen), de andere
weigeren bij naam.

### Een document zonder de eigenschap verandert niet

De DRL-tekst van elke golden (433) en elk rood document (77) op `5d3e8f054` naast die van de branch, zonder en met
lezingen, samen 1020 teksten, waarvan 6 (drie documenten) van een document met de eigenschap: **1014 teksten van 507
documenten: 828 byte-gelijk, 186 (520 functies) die alleen verschillen in de romp van een functie die een samengestelde
voorwaarde rekent, 0 overige** (90 ervan zijn een weigering of fout van de analyse, byte-gelijk). De toets herschrijft de
oude romp (`raak`/`vast`, `established`/`endVerdict`) tot de nieuwe (`var lezing = ComputeHelpers.composite(…)`) en
vergelijkt dan byte voor byte, dus ook regels, linkerkanten, consequenties, imports en globals. Van de 386 pins in
`src/test/resources/drl-text` zijn er 81 (230 functies) opnieuw geschreven, met dezelfde toets: 0 overige. De planpins in
`RulePlanWitnessTest` dragen er `open=false` bij (tien waarden en één deponering), en geen andere wijziging.

### De omvang van een document met de eigenschap

Over de 133 goldens die dit deel draagt, met de eigenschap erbij (niet-lege regels DRL-tekst, `rule`s, functies):

| | regels | `rule`s | functies |
|---|---|---|---|
| gewoon | 13 463 | 612 | 677 |
| met de eigenschap op `da6d38aec` | 29 454 | 1595 | 1127 |
| met de eigenschap nu | 18 754 (+39%) | 705 (+15%) | 857 (+27%) |
| met de eigenschap, elke declaratie `complete` | 13 463 | 612 | 677 |

Per soort, met de eigenschap (aantal, regels), eerst op `da6d38aec` en dan nu: standing "uit de invoer" 327, 2943 → 38,
342; "uit de onvolledige invoer" 290, 2610 → 0; kenmerkstandings 197, 1627 → 64, 529; "open door een schrijver" van een
attribuut 37, 333 → gelijk; padstandings 58, 435 → 58, 455; regel "laat open" 178, 1685 → 0; functies `toets` 422, 4441
→ 66, 521 en `oordeel` 0 → 178, 2318; `samengesteld` 96, 2488 → 53, 1329; `sluiting` 76, 608 → 42, 336; `deling` 129, 645
→ 114, 570; regel van de vorm 398, 6306 → 398, 6863; kop 1274 → 1127. Een functie `oordeel` met haar `try` en de tak
"open" in de consequentie zijn wat overblijft.

Regels van de KieBase en feiten aan het fixpunt van de eerste case, zonder en met de eigenschap: de 19 getuigen van ref
die spelen 177 → 216 regels en 119 → 143 feiten; de 21 documenten met de eigenschap van `open-values/` 85 → 96 en 106 →
141; samen 262 → 312 en 225 → 284. Per getuige (regels; feiten): absent-link 22→30; 10→13, absent-link-path 5→8; 4→6, arithmetic 8→8;
10→11, characteristic 7→8; 5→7, closed-input 3→3; 5→6, comparison 22→26; 11→12, compound-all 6→7; 5→6, compound-any 5→6;
4→5, compound-counting 15→18; 8→9, compound-none 6→7; 5→6, consistency 2→2; 3→4, exact-digits 5→6; 4→5, filled-test
11→13; 7→8, initialisation 2→2; 3→4, let 4→5; 5→6, marker 6→7; 4→5, navigated-read 5→8; 5→7, settled-empty 30→36;
15→16, settled-empty-date 13→16; 6→7.

### Acceptance

- **De getuigen van ref** (`OpenValuesRefWitnessTest`, gate 12): 34 getuigen, 19 spelen op beide weergaven (77 cases per
  weergave), 15 weigeren met DROOLS-370 en hun slug: `defaults-group` acht (composite-filled-test,
  defaults-characteristic, defaults-stack, defaults-subtype, filled-test-not-closure, input-above-computation,
  input-above-computation-open, settled-empty-stack), `set-fold` vijf (empty-by-fault, fact-creation, listed-fold,
  set-fold, settled-empty-fold), `object-creation` één, `quantifier-fold` één. `closed-input` speelt: de invoer leest
  `complete`.
- **De principes** (`OpenValuesWitnessTest`, 21 tests, gate 12): 23 documenten uit de Jutterswet onder
  `src/test/resources/open-values/`, 64 cases en twee rode die ook in ref spelen (`runner`, `runner --closed-inputs`),
  elk principe van de lijst van de kaart onder de naam `WV-1.5-…` in de javadoc, met `complete` en het deel per instantie
  erbij; en de wijziging van de bean tussen twee keer vuren, ook langs een pad.
- **Rood eerst**: op de jar van `5d3e8f054` weigert elk van de 52 documenten met de eigenschap met DROOLS-370 en
  `form: document`, en spelen de twee documenten zonder de eigenschap als nu. `OpenableMembersWitnessTest` toetst een
  klasse die er niet was.
- **De weigering**: `InferFromEmptyRefusalWitnessTest` leest de tabel van ch. 1.5 uit de spec en houdt de lijst eraan; de
  bouw met lezingen weigert een gedragen document met `reading`. Over de 151 documenten van de meting hieronder die ref
  weigert of die een vorm van de tabel dragen, is de slug gelijk aan die van ref (REF-014), op de documenten na die de
  lader al weigert.
- **Buiten de gates gemeten** (eenmalig, programma's in de werkmap): elke golden, elke getuige van ref en elk document van
  `open-values/` met de eigenschap erbij, elke case als zij is en met elke gegeven invoer weggelaten (hoogstens 60 per
  document): 173 documenten die dit deel draagt, 3546 runs op de twee weergaven, 2956 met een open lid in de eindstand:
  **0 verschillen met de eindstand van ref** (`runner --end-state`; acht runs falen in beide). Behoud bij volledige
  kennis: 132 goldens spelen met de eigenschap en elk invoerlid volledig hun eigen 335 cases op beide weergaven.

### Voorlopige besluiten

1. **`OpenInput` is één feit per sessie, niet één per bean.** Een regel bindt het als de rekendatum, zonder join; een
   gemaakt object (deel 2) heeft geen eigen feit nodig, want wat het feit niet noemt, is vaststaand leeg. De lezing krijgt
   het object erbij.
2. **De regel van een schrijver houdt een `eval` in de linkerkant**, die vraagt of de schrijver iets doet, en de
   consequentie rekent het oordeel nog eens. Zonder die `eval` zou de regel voor elk object vuren, ook waar de vorm niets
   vult, en zouden `Observer.firings()` en `DroolsBackend.coverage` elke vorm van een document met de eigenschap als
   gevuurd melden. Nu vuurt zij waar de vorm beslist of haar doel open laat. De `eval` kost één regel tekst.
3. **Een functie `established`/`endVerdict` blijft** voor de kwantorvouw en de vouw per periode, op de tabel van
   `Composite`; de route `ESTABLISHED` is vervallen.
4. **De samengestelde voorwaarde krijgt alleen een `try` vanaf het eerste lid dat onbepaald kan zijn**
   (`Term.Compound.openFrom`), en de probe alleen dan.
5. **`var lezing`** in de functietekst, zodat een gewoon document geen import erbij krijgt.
6. **Een lid dat op een type een schrijver heeft, houdt zijn standing op elk type**, ook waar het invoer is; de regel "uit
   de invoer" en de regel "onwaar" lezen daar `OpenInput` in hun consequentie.
7. **De zin van DROOLS-370** zegt dat de backend die lezing niet draagt "voor alles wat gevraagd is (vorm: …)".

### Voorstellen voor beschermde teksten

- `spec/CONSTRUCTS.md` rij 48, achter de stand van ref: "Drools reads it for rules, initialisations, consistency rules and
  conditions without a timeline, a fold, a stack, a creation or an IfThenElse, and refuses any other form by its slug
  (DROOLS-370)." Rij 49: "Drools reads `complete` where it bounds the input that can be open."
- `CLAUDE.md`: geen alinea van een gate verandert.

### Open punten

1. **Buiten de kaart, in gewone documenten**: een wijziging van een invoerattribuut van een bean tussen twee keer vuren
   bereikt een regel die dat attribuut langs een link leest niet (de waarde is aan het patroon van het onderwerp
   gebonden). Kleinste document: `open-values/link` zonder de eigenschap, vindpremie = waarde + bonus van de vinder;
   bonus van J2 van 20 naar 50 met `session.update` laat de vindpremie van V3 op 120 staan (beide weergaven), waar 150
   hoort. In een document met de eigenschap is het 150. De README belooft dat `fire` na een update klopt ("Firing again").
2. `corpus/golden/jutterswet-afleiden-uit-leeg/NOTE.md`, `jutterswet-volledig-gegeven/NOTE.md`,
   `corpus/red/jutterswet-kenmerk-wel-en-niet/NOTE.md` en `frontends/regelspraak/grammar/DIVERGENCES.md` (r. 4984) zeggen
   dat Drools een document met de eigenschap weigert. Dat geldt na dit deel per vorm; die bestanden vallen buiten het
   mandaat.
3. `Observer` telt een regel die haar doel open laat als gevuurd: zij vuurt. Wil de scenariolaag "beslist" en "laat open"
   scheiden, dan is dat een uitbreiding van de waarnemer (deel 3 of 4).
4. De lezingen (deel 3) lezen een `READ` nog niet als "bereikt en open": `RecordingRendering` legt een afgebroken
   knoop niet vast. De bouw met lezingen weigert tot dan.

### Gates

Elk één keer los gedraaid op `6a8ad5d57`, op 1, 8, 10 en 15 na: die op `f2762d082`, waarna alleen
`TmsTranspiler.conditionTest` veranderde (dezelfde DRL-tekst voor de goldens zonder en met de eigenschap, 659 teksten).

- 1 `check-self-contained: PASS`
- 8 `check-red: PASS (102 red cases fault observably in ref and clingo …)`; Drools leg 82 fault-pinned, 20 not-carried, 20
  EXCLUDE met hun code
- 10 `check-rgs-codes: PASS — 414 codes, 447 sites …`
- 12 `check-drools: PASS`: played 384, channel floor 49, DRL-text branch 384, JUnit 2009 tests;
  `check-drools-codes: PASS — 371 codes …`
- 13 `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens …)`
- 15 `check-graph: PASS`
- 17 `check-readings: PASS (ref traced 1211 golden case files … on both branches 385 goldens and 1073 case files compared,
  8468 readings, 27262 node lines …)`, 2 goldens DROOLS-370

Geen verwachting, pin buiten de 81 DRL-teksten en de elf planregels, lijst of floor veranderde, behalve drie redenen
(twee in gate 12, één in gate 8). De JUnit-suite: 2009 tests, geen overgeslagen (was 1979), 2:39 minuten.

### Omvang van de diff

`backends/drools/src/main`: 35 bestanden, 1895 regels erbij en 344 eraf. Grootste: `TmsTranspiler` 492,
`UncarriedForms` 213 (nieuw), `DrlRendering` 147, `ComputeHelpers` 141, `OpenableMembers` 138 (nieuw), `Composite` 114
(nieuw), `ClosureRendering` 106, `CaseRunner` 103, `DroolsBackend` 71, `PatternDslRendering` 68, `Consequence` 65,
`SessionReaders` 49, `OpenInput` 46, `CaseInput` 44, `RecordingRendering` 39, `Undetermined` 38, `LeftOpen` 37, `Route` 37,
`Input` 34; de overige onder de 33. `README.md` 78. Tests: 10 Java-bestanden, 694 erbij en 115 eraf, plus de documenten
van `open-values/`.

## Landing van deel 1

`make gates` in de worktree op `9c4431593`: alle gates groen (681 s, after-run groen). Een onafhankelijke code-review
(Google Neon, alleen lezen, op de diff van `src/main`) vond geen afwijkende uitkomst. Nagekeken: de vier mogelijke fouten
bleken geen fout (een vouw anders dan een telling over een unie weigert de loader; een geneste samenstelling achter een open
lid leest gelijk aan ref; de namen van `characteristicsAbsent` worden op de getter genormaliseerd; de regelbouwer voor een
tijdlijnkenmerk is een vorm die geweigerd wordt). Mee in het volgende bouwdeel: de uitdrukking in `OpenableMembers.of` die op
operatorvolgorde rust, en twee kleine vereenvoudigingen. De kaart blijft in `doing` tot het laatste deel.
