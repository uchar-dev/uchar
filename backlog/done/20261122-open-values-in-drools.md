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

## Result van deel 2a: de tekst van een strikt document, de stapel

### Wat er is

Commits op `card/open-values-in-drools`: `620ab3cf9` (de strikte tekst), `522130475` (de stapel), `a3e262584` (gate 12),
`b70d1aa4e` (gate 8), `6a288667e` (gate 17), `6f7f5599f` (een update langs een pad), `ff9a739e8` (drie notities van het
corpus), `b5e232e9b` (wie besliste, onder een open initialisatie), `d62e78065` (ref en ch. 1.5: elke initialisatie van een
stapel sluit) en `6c0246149` (Drools volgt dat).

- **De strikte tekst vangt zelf niets.** Elke grens van de lezing is één functie die haar deel als lambda krijgt:
  `Composite.read`, `ComputeHelpers.emptyTestOverOpen`, `dividedLazily` (en `dividedAbsLazily`), `judge`, `holdsForItself`,
  `faulted`, `deposited`, `valued`, `decided`. De knoop is `Term.Deferred`. Zij neemt een `Callable` en geen `Supplier`: een
  functie van een DRL-tekst declareert `Exception`, en een lambda die er een aanroept dus ook.
- **`OpenInput` is een global** (`open_invoer`), gezet door `DroolsBackend.insert`, in een document waarin iets open kan zijn.
  De naam heeft een onderstreping, zodat geen parameter hem kan dragen.
- **`Consequence.Writer`** zegt wat een schrijver doet die zijn doel kan openlaten, voor een regel en een default gelijk: wat
  hij beslist en wat hij invoegt waar hij open is (`LeavesOpen`). `Consequence.Value` heeft geen veld `open` meer; de elf
  planregels van deel 1 hebben weer hun oude tekst, op één (`ConditionDeposit`) na.
- **De stapel.** Een open default is in de eenvoudige vorm een `Decides` met `open` naast de `LeftOpen` van het doel, in de
  algemene vorm een `Outcome` van de soort `OPEN`. De linkerkant van een lagere default is die van elke stapel. Alleen een
  initialisatie vraagt naar wat niet open staat (`closesOverOpen`), ook een tweede in één stapel. De verzoening "eens" staat onder `not` over een open lid; één regel erbij
  (`reconcile-open`) maakt de default, of aan de wortel het doel, open. Een stapel zonder openable lezing houdt zijn regels.
- **Twee initialisaties in één stapel** (ch. 7.10, blok 4). `closing` in `ref/lib/eval.ml` stopte bij de eerste initialisatie
  die niet besliste; nu gaat zij verder, en de lezing aan het fixpunt (`ref/lib/reading.ml`, dezelfde wandeling, buiten de
  genoemde bestanden van het mandaat) met haar. Ch. 1.5 heeft het voorbeeld `WV-1.5-stack-two-initialisations`. Waar de default
  en de geërfde initialisatie allebei open zijn, beslist de eigen: niets erboven beslist. In Drools verviel de extra wacht.
- **Een update langs een pad** (open punt 1 van deel 1): `fire` geeft elke bean van de invoer die langs verwijzingen bij een
  bijgewerkte bean uitkomt een update (`Watch.reachingUpdated`). De regels en hun tekst veranderen niet.

### De omvang

Niet-lege regels, `rule`s en functies van de DRL-tekst:

| | regels | `rule`s | functies |
|---|---|---|---|
| de 133 goldens van deel 1, gewoon | 13 463 | 612 | 677 |
| strikt na deel 1 | 18 754 (+39,3%) | 705 | 857 |
| strikt nu | 14 352 (+6,6%) | 705 | 701 |
| strikt, elke declaratie volledig | 13 463 | 612 | 677 |
| de 37 goldens met een stapel erbij, gewoon | 6 724 | 374 | 295 |
| strikt | 7 999 (+19,0%) | 449 (+20,1%) | 327 (+10,8%) |
| strikt, elke declaratie volledig | 6 724 | 374 | 295 |
| samen 170, gewoon en strikt | 20 187 en 22 351 (+10,7%) | 986 en 1154 | 972 en 1028 |

De grootste groei van één stapelgolden is +36,1% (`jutterswet-wrakbasis-vervangen-basis-groep`, 122 naar 166 regels).

Per soort over de 133 goldens (aantal, regels), gewoon, strikt na deel 1, strikt nu: regel van de vorm 398, 5313 / 6863 /
6302; functie `oordeel` 0 / 178, 2318 / 178, 1258; functie `waarde` 271, 2168 / 281, 2248 / 281, 1180; `samengesteld` 53,
797 / 1329 / 797; `toets` 244, 1794 / 66, 521 / 66, 251; `deling` 1, 5 / 114, 570 / 0; `sluiting` 0 / 42, 336 / 0;
`deponering` 15, 180 / 180 / 105; kop 855 / 1127 / 1257; "open door een schrijver" 0 / 53, 477 / 53, 477; padstandings 18,
135 / 58, 455 / 58, 435. Wat een strikt document meer heeft dan het gewone: de kop (twee imports en een global), de tak
"open" in een consequentie en de standings die open kunnen zijn.

**Bouwtijd** van de vijf stapelgoldens met de meeste regels (`jutterswet-opgave-vormen`, `-deling-in-afwijking-omgekeerd`,
`-deling-in-afwijking`, `-opgave-subtype`, `-opgave-boven-berekening`), mediaan van vijf in één warme JVM, `-Xmx3g`, gewoon
naar strikt: exec-model 12 naar 12 ms (+6%), DRL-tekst 202 naar 241 ms (+19%); de hoogste van één golden +29%.

### Een document zonder de eigenschap verandert niet

De DRL-tekst van elke golden en elk rood document op `977b89a24` naast die van nu, zonder en met lezingen: **1014 teksten van
507 documenten byte-gelijk, 0 andere** (90 ervan een weigering of fout van de analyse). Er zijn twee pins van
`drl-text` bij (de twee goldens die nu spelen); geen bestaande veranderde.

### Buiten de gates gemeten (eenmalig, programma's in de werkmap)

Elke golden, elke getuige van ref en elk document van `open-values/` met de eigenschap erbij, elke case als zij is en met elke
gegeven invoer weggelaten (hoogstens 60 per document): **234 documenten, 5632 runs op de twee weergaven, 4826 met een open
lid: 0 verschillen met de eindstand van ref** (`stack-two-initialisations` na de wijziging in ref opnieuw: 26 runs, 0
verschillen). Behoud: 168 goldens spelen met de eigenschap en elk invoerlid volledig hun
eigen cases op beide weergaven (de twee goldens die de eigenschap zelf dragen, verwachten een open lid en tellen niet mee).

### De stapel per vorm

| vorm | uitkomst |
|---|---|
| eenvoudige keten, met open rechtvaardiging of open waarde, ook de onvoorwaardelijke | gedragen |
| voorrang door een vaststaand lege waarde; alles geeft voorrang; invoer naast voorrang en naast open | gedragen |
| initialisatie onder een open default, met open waarde, onder de invoer; geërfde en eigen initialisatie | gedragen |
| opgave, ook volledig verklaard | gedragen |
| kenmerkstapel (toegekend, niet toegekend, open, door de case genoemd) | gedragen |
| uitzonderingsgroepen, genest met terugval, met een initialisatie eronder; ongeordende groep | gedragen |
| effectieve stapel van een subtype; groep buiten haar venster | gedragen |
| ongeordende groepen waarvan er één een initialisatie heeft | geweigerd, slug `defaults-group` |
| stapel op een tijdlijndoel | geweigerd, slug `timeline-member` (tabel van ch. 1.5) |

De weigering van de ongeordende groepen met een initialisatie: of de initialisatie over haar eigen open default sluit, hoort
per groep gevraagd te worden, en de verzoening vraagt het over alle groepen. Geen golden heeft de vorm.

### Acceptance

- **De getuigen van ref**: van de 34 spelen er 27 op beide weergaven (120 cases per weergave), 7 weigeren met hun slug:
  `set-fold` vijf, `object-creation` en `quantifier-fold` elk één. De acht met een stapel spelen nu.
- **De voorbeelden van ch. 1.5** (`OpenValuesWitnessTest`, 31 tests): 15 documenten `stack-*` uit de Jutterswet, 63 cases die
  ook in ref spelen: `WV-1.5-stack-first-decides`, `-gives-way`, `-settled-leeg`, `-initialisation`, `-two-initialisations`, `-opgave`,
  `-characteristic`, `-exception-groups`, `-unordered`, `-type-chain`; het deponeren van een default, een groep buiten haar
  venster, een wijziging van de bean tussen twee keer vuren en wie besliste (`decisionsOf`). Vier documenten zijn dat van de
  frontend met een initialisatiedefault erbij, die het dialect alleen naast een opgave schrijft.
- **Rood eerst**: op `620ab3cf9` weigert elk document `stack-*` met DROOLS-370 en `form: defaults-group`. De getuige van de
  update langs een pad (`TmsRepeatedFireWitnessTest`) geeft op `6a288667e` na de wijziging 120 in plaats van 150, op beide
  weergaven en over één en twee links.
  In ref (`test_ref.ml`) faalden op de code ervoor drie rijen: de eigen initialisatie onder een open default met een geërfde
  die voorrang geeft (open in plaats van 5), onder een open default en een open geërfde (open in plaats van 5), en hun lezing
  aan het fixpunt (`DISPLACED` in plaats van `HOLDS`). In Drools faalden op de jar ervoor de twee eerste in
  `stack-two-initialisations`.
- **Geen test verzwakt.** Aangepast: de tests die routes, termen en functies van het plan opsommen; de weigertest neemt
  `set-fold` als document (de golden die zij had, speelt); acht corpustests slaan in hun deel met lezingen een document met de
  eigenschap over (`TmsDocuments.hasReadings`), tot deel 3 de lezingen draagt.

### Voorlopige besluiten

1. **`Callable` in plaats van `Supplier`**, om de reden hierboven.
2. **De global heet `open_invoer`**: `openinvoer` is een naam die een parameter kan hebben.
3. **Het oordeel gaat op in de functie van de waarde** (`decided(oordeel_…, () -> …)`): één uitkomst met één tak "open" in de
   consequentie. Een constante waarde en een kenmerk houden `if (decides(…))`.
4. **`valued`, `faulted`, `holdsForItself` en `deposited` nemen ook een lambda**, naast de vijf plaatsen van de opdracht: het is
   dezelfde opmaak.
5. **Gate 17 krijgt een lijst `READINGS_REFUSED`** (golden, code, reden): een golden die speelt en waarvan de lezingen geweigerd
   worden. Zij faalt zodra de golden gelezen wordt of niet meer speelt; deel 3 maakt haar leeg.
6. **Ch. 1.5 heeft er een zin bij** die twee initialisaties noemt, met het voorbeeld; de regel zelf stond er.
7. **De rol `reconcile-open`** (`RuleOrigin.Role`) is nieuw en komt alleen in een document met de eigenschap voor.

### Voorstellen voor beschermde teksten

- `CLAUDE.md`, gate 17, achter de zin over `TRACE_REFUSED`: "`READINGS_REFUSED` names a golden the replay plays and whose
  build with readings the backend refuses, with the DROOLS code: its readings end in that `EXCLUDE` on both branches and it is
  compared with nothing." en "The four lists" wordt "The five lists". Zodra deel 3 landt, is de lijst leeg en kan de zin weg.
- `spec/CONSTRUCTS.md` rij 48: "Drools reads it for rules, initialisations, consistency rules, conditions and defaults groups
  without a timeline, a fold, a creation or an IfThenElse, and refuses any other form by its slug (DROOLS-370); its build with
  readings refuses every such document."

### Open punten

1. `frontends/regelspraak/grammar/DIVERGENCES.md` r. 4983 zegt dat een executor die een vorm niet draagt het document weigert;
   dat klopt, en noemt Drools niet meer bij naam.
2. `Reads.readingRulePlan` geeft een leesregel de slots die zijn linkerkant bindt; het slot van `open_invoer` bindt niets en
   hoort er in deel 3 bij.
3. De distributie ziet bij haar pin: `insert` zet de global `open_invoer` (een consument die zelf beans invoegt, krijgt een
   `IllegalStateException` bij de eerste lege invoer), `fire` geeft beans een update vóór het vuren, en de rol
   `reconcile-open`.
4. De ASP-encoding (`backends/clingo`): de norm voor twee initialisaties staat nu in ch. 1.5 en het voorbeeld in de suite van
   ref.

### Gates

Elk los gedraaid, vóór de rebase op `0c27516df`: 8, 12 en 13 op wat nu `6f7f5599f` is, waarna alleen de lezing van de
besluiten, tests en de wacht van een tweede initialisatie veranderden; 1, 10 en 15 op wat nu `b5e232e9b` is. Gate 5, gate 17 en
de suite van ref op `6c0246149`. De DRL-tekst van de 170 gedragen goldens, gewoon, strikt en volledig, is vóór en na `6c0246149`
byte-gelijk.

- 1 `check-self-contained: PASS`
- 5 `check-oracle: PASS (ref suite 4239 worked examples …; 1211 cases replayed exactly …; preservation with complete knowledge:
  279 goldens and 782 cases play …)`
- 8 `check-red: PASS (102 red cases fault observably in ref and clingo …)`; Drools leg 83 fault-pinned (was 82), 19
  not-carried (was 20), 19 EXCLUDE met hun code
- 10 `check-rgs-codes: PASS — 414 codes, 447 sites …`
- 12 `check-drools: PASS`: played 386 (was 384), channel floor 47 (was 49), DRL-text branch 386, JUnit 2021 tests;
  `check-drools-codes: PASS — 371 codes …`
- 13 `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens …)`
- 15 `check-graph: PASS`
- 17 `check-readings: PASS (ref traced 1211 golden case files … on both branches 385 goldens and 1073 case files compared, 8468
  readings, 27262 node lines …)`, 2 goldens DROOLS-370 (`READINGS_REFUSED`)

De JUnit-suite op `6c0246149`: 2024 tests, geen overgeslagen (2009 na deel 1, twaalf van dit deel, drie met `main`). Geen floor veranderde; `EXPECTED_PLAYED` ging van 384
naar 386 en twee `EXCLUDE`-regels van gate 12 en één van gate 8 vervielen.

## Landing van deel 2a

`make gates` op `61676e09d`: alle 17 gates groen (704 s, after-run groen). De hoofdsessie rekende de omvang na over 170
goldens (gewoon 20 187, strikt 22 351 niet-lege regels, +10,7%; met een stapel +19,0%; alles volledig gelijk aan gewoon;
393 gewone documenten met dezelfde tekst). Een tweede lezer (Google Neon, alleen lezen, effort high) vond geen openstaand
punt: de melding over twee initialisaties was al hersteld (`6c0246149`), de andere vormen weigert de analyse of sluit de
verifier uit (WF-050, WF-075). De kaart blijft in `doing` tot het laatste deel.

## Result van deel 2b: de vouwen en de kwantorvouw

### Wat er is

Commits op `card/open-values-in-drools`: `844e34299` (de vouwen en de kwantorvouw) en `ed453f529` (de volgorde: een filter of
toets die kan falen of deponeren boven een open lid, besluit van de hoofdsessie tijdens de bouw).

- **Een vouw over open leden.** De bijdrageregel van een lid blijft één regel. Wat het lid de vouw geeft en niet vaststaat, voegt
  zij logisch in als `FoldOpen(vouw, onderwerp, lid, own, lezing)`: `own` leeg waar het filter onbepaald is of de waarde open,
  waar of onwaar waar een sluiting het besliste. Is het lid vast, dan is er alleen de `Contribution` van elke vouw. Kan het filter
  onbepaald zijn, dan vraagt de toets en rekent de consequentie zijn oordeel zoals een schrijver dat van zijn voorwaarde doet
  (`Consequence.FilteredContribution`, `ComputeHelpers.judge`).
- **De eigenaar** heeft er één accumulate bij, alleen waar een lid open kan zijn, die de `FoldOpen`-feiten van de vouw opsomt. De
  telling van de bijdragen gaat erdoor (`ComputeHelpers.standingCount`), na de fouten van het filter en vóór de terugval: een
  onbepaald lid is daar een open lezing, een sluiting wordt gemeld aan de grens eromheen.
- **De grens van een sluiting** (`Boundary`) is één per waarde (`valued`), per toets op leeg en gevuld en per atomaire voorwaarde
  waarin een vouw sluiten kan (`StaticFunction.ATOM`, om de vergelijking heen en om geen operator). Een voorwaarde die sloot,
  staat voor de vorm zelf en niet voor een ander; een waarde die leeg is na een sluiting laat haar doel open en deponeert wat zij
  deponeerde (zoals ref: de evaluatie is niet afgebroken).
- **De kwantorvouw** zonder filter of met een filter dat vaststaat, met een toets die niet kan falen of deponeren: Kleene over de
  leden waarop de toets vaststaat (geteld door de accumulate van elke kwantorvouw) en de `FoldOpen`-feiten van de andere
  (`ComputeHelpers.quantifierVerdictOverOpen`).
- **De volgorde.** Waar een filter kan falen of onbepaald zijn, neemt de regel van de `FilterFault` een lid van beide soorten,
  met de `OpenRead` als fout van het onbepaalde lid (`TestItem.Role.BREAKS_OFF`): de eigenaar werpt de eerste in de volgorde, zoals
  ref. Een open waarde komt na elk filter. Waar een toets kan falen of deponeren, draagt de `MemberTest` ook een open lezing of een
  oordeel dat niet staat, en `quantifierRead` leest de leden met `Composite`, dus met de voorlopige leden van ch. 1.5; dat is in elk
  document de ene lezing van een kwantorvouw op volgorde. Geen nieuw feit en geen tweede lezing.
- **De weigering**: `UncarriedForms` heeft drie slugs van de analyse achteraan (`quantifier-fold`, `set-fold`, `anchored-read`);
  zij weigert de vorm waar zij haar verlaagt, met dezelfde code, want of een deel een openable lid leest en kan falen, weet
  alleen zij. Elke andere slug komt vóór deze drie en weigert vóór de bouw, zoals nu.
- **Niet geraakt**: de productieruntime, `Contribution`, `exactSum`, `count`, de plannen en de tekst van een gewoon document.

### Per onderdeel

| onderdeel | uitkomst |
|---|---|
| som, totaal, aantal, minimum, maximum, eerste, laatste, gevuld-telling over leden die open kunnen zijn | gedragen |
| lijstvouw over zulke vouwen; een terugval die vaststaat | gedragen |
| filter dat een openable lid leest, ook op gevuld of leeg, en ook waar het kan falen of deponeren (een deling, een macht, een parameter, de rekendatum, datumrekening); een geankerde lezing van een afgeleid lid | gedragen |
| unie van instantieverzamelingen | gedragen |
| vouw over een afgeleid lid, over een vouw, langs een pad, in een stapel, een initialisatie, een `Let`, een consistentieregel | gedragen |
| kwantorvouw zonder filter of met een vast filter; een toets die sluit, faalt of deponeert; een vouw in haar toets | gedragen |
| een vouw die open kan zijn in een filter; een terugval die een openable lid leest | geweigerd, `set-fold` |
| kwantorvouw met een filter dat een openable lid leest | geweigerd, `quantifier-fold` |
| geankerde lezing van openable invoer | geweigerd, `anchored-read` |
| `IfThenElse`, objectcreatie, feitcreatie | geweigerd, `if-then-else`, `object-creation`, `fact-creation` |

Van de goldens met de eigenschap erbij weigeren er drie met `set-fold` (een vouw in het filter), drie met `quantifier-fold` en vier
met `anchored-read`. `jutterswet-loodsgrens` (een toets tegen een parameter) wordt gedragen.

### De omvang

Niet-lege regels, `rule`s en functies van de DRL-tekst, over de 227 goldens die de backend draagt (168 na deel 2a; de twee goldens
die de eigenschap zelf dragen niet meegerekend):

| | regels | `rule`s | functies |
|---|---|---|---|
| 227 goldens, gewoon | 30 059 | 1516 | 1470 |
| strikt | 32 991 (+9,8%) | 1739 (+14,7%) | 1547 (+5,2%) |
| strikt, elke declaratie volledig | 30 059 | 1516 | 1470 |
| de 59 nieuwe, gewoon | 10 082 | 541 | 516 |
| strikt | 10 898 (+8,1%) | 598 (+10,5%) | 538 (+4,3%) |
| de 168 van eerder, gewoon en strikt | 19 977 en 22 093 (+10,6%) | 975 en 1141 | 954 en 1009 |

De grootste groei van één nieuwe golden is +64,5% (`jutterswet-sluitsom`, 31 naar 51 regels), daarna +34,1%
(`jutterswet-zwaarste-weging`, 314 naar 421).

**Bouwtijd** van de vijf goldens met de meeste vouwen (`jutterswet-zinkende-jutter`, `-vroegste-vondst`, `-sluitdag-gemeld`,
`-schouwploeg`, `-redderkeur`), mediaan van vijf in één warme JVM, `-Xmx3g`, gewoon naar strikt: exec-model 12 naar 13 ms (+5%),
DRL-tekst 178 naar 227 ms (+27%); de hoogste van één golden op de DRL-tekst +34%.

### Een document zonder de eigenschap verandert niet

De DRL-tekst van elke golden en elk rood document op `977b89a24` (de tekst van `0a79d621d`) naast die van nu, zonder en met
lezingen: **1014 teksten van 507 documenten byte-gelijk, 0 andere** (90 ervan een weigering of fout van de analyse). Van de 388
pins veranderen er twee, die van de goldens met de eigenschap: het oordeel heet er `Object` en `valued`/`decided` nemen de sonde
mee; het aantal regels blijft gelijk.

### Buiten de gates gemeten (eenmalig, programma's in de werkmap)

- Elke golden, elke getuige van ref en elk document van `open-values/` met de eigenschap erbij, elke case als zij is en met elke
  gegeven invoer weggelaten (hoogstens 60 per document): **309 documenten, 7790 runs op de twee weergaven, 6794 met een open lid:
  0 verschillen met de eindstand van ref**.
- De 77 daarvan met een vouw, elke case veertig keer met een willekeurig deel van de invoer weggelaten, soms ook parameters en de
  rekendatum: **15 200 runs, 12 528 met een open lid, 176 fouten, 0 verschillen**. De dertien documenten waar de volgorde zichtbaar
  is (een parameter, een deling, een macht, de rekendatum of datumrekening in filter of toets), elke case tachtig keer: **8640 runs,
  186 fouten, 0 verschillen**. Een fout van ref is hier een fout van de backend en geen andere afloop.
- Behoud: 227 goldens spelen met de eigenschap en elk invoerlid volledig hun eigen cases op beide weergaven (de twee goldens die
  de eigenschap zelf dragen, verwachten een open lid en tellen niet mee).

### Acceptance

- **De getuigen van ref**: van de 34 spelen er 31 op beide weergaven (137 cases per weergave); drie weigeren met hun slug:
  `fact-creation`, `object-creation` en `quantifier-fold` (haar regel met een filter op een open kenmerk). `empty-by-fault`,
  `listed-fold`, `set-fold` en `settled-empty-fold` spelen nu.
- **De voorbeelden van ch. 1.5** (`OpenValuesWitnessTest`, 42 tests): twaalf documenten, 80 cases en 10 rode cases die ook in ref
  spelen: `set-fold` (`WV-1.5-set-fold*`), `closure-in-a-value` (`WV-1.5-closure-in-a-value`, de gevuld-telling),
  `fold-in-a-binder` (`WV-1.5-closure-before-a-barrier` en `-reached` over een vouw), `quantifier-fold`
  (`WV-1.5-quantifier-fold-*` zonder filter), `fold-chain` (`-open-passes-on`), `fold-subtype` (`-input-per-object`),
  `past-undetermined-fold` (`WV-1.5-past-undetermined-fold`, de deling, ook in een vouwfilter); een wijziging van de bean van een
  lid tussen twee keer vuren; en de volgorde:
  - `fold-over-a-parameter`: de grens tegen een parameter in een kwantorvouw zonder filter en in een vouwfilter; EF-014 zonder een
    onbepaald lid ervoor (fout) en met een onbepaald lid ervoor (open);
  - `fold-over-the-date`: de rekendatum, EF-011 op dezelfde twee manieren;
  - `fold-value-after-filters`: een open waarde van een eerder lid tegenover een filterfout van een later lid: ref leest elk filter
    vóór elke waarde en faalt (EF-014), de backend ook;
  - `fold-over-a-union`: een onbepaald filter van de eerste selectie tegenover een fout van de tweede;
  - `fold-in-a-date-stack`: "de eerste van de maand van alle ladingen … die aan alle volgende voorwaarden voldoen: hij heeft geen
    overlading; zijn maand is later dan de sluitdatum van de Schuit min de wachttijd van de Schuit" als consequentie van een default en
    als operand in de rechtvaardiging van een andere van dezelfde stapel, met de sluitdatum afgeleid: alles gevuld, overlading open
    vóór en na een lid dat telt, maand open, sluitdatum open en vaststaand leeg, en EF-007 met en zonder een onbepaald lid ervoor
    (de wachttijd is een volledig lid in plaats van het vaste "1 mnd", omdat een kalendermaand van een casedatum niet buiten bereik
    komt). De geankerde lezing leest het onderwerp van de regel: haar `State` is gejoind op `$vouwonderwerp`.
  Zes documenten zijn dat van de frontend met een deel van een van hun vormen elders, waar het dialect dat niet schrijft.
  `WV-1.5-closure-of-a-binder-*` en `-closure-before-a-fault` hebben een `IfThenElse` en zijn geweigerd.
- **Elke smalle slug heeft een getuige** (`InferFromEmptyRefusalWitnessTest`): per vorm een document dat weigert, op beide
  weergaven, met de regel in de zin, en hetzelfde document met volledige invoer, dat bouwt; en zeven goldens met een deling, een
  macht of een parameter in filter of toets bouwen met de eigenschap.
- **Rood eerst**: op de jar van `6c0246149` weigeren de zes eerste documenten met `set-fold` (drie) en `quantifier-fold` (drie); op
  die van `844e34299` de zes van de volgorde (vier met `set-fold`, twee met `quantifier-fold`).
- **Geen test verzwakt.** Aangepast: de opsommingen van routes, vormen en vouwinvoer (`RouteWitnessTest`, `FormsPlan`,
  `FoldPlanWitnessTest`), één cast (`PeriodArithmeticTest`), de getuige van de weigertest (`object-creation` in plaats van
  `set-fold`, die speelt) en `a_let_read_in_a_quantifier_fold_is_named`, waarvan het document met een gewone vouw geen slug van
  `UncarriedForms` meer heeft.

### Voorlopige besluiten

1. **Eén feit `FoldOpen` in plaats van `FoldOpen` en `FoldClosure`**: zij verschillen in één veld (`own`), en de eigenaar heeft zo
   één patroon erbij in plaats van twee. Het draagt de open lezing, die deel 3 nodig heeft; de volgorde van de leden niet.
2. **De grens is een object per thread** (`Boundary`), geopend in `valued`, `emptyTestOverOpen` en `atom`: een sluiting gebeurt
   diep in een term, en een waarde kan haar niet dragen zonder een omhulsel om elke operator.
3. **`judge` geeft het oordeel** (waar, onwaar of de `Undetermined` met zijn waarheid voor de vorm zelf) in plaats van drie
   uitkomsten: een filter en een toets hebben het paar nodig, een schrijver leest er dezelfde drie uitkomsten uit.
4. **Een lege waarde na een sluiting deponeert wat zij deponeerde**, zoals ref (`value_outcome`): de grens gaf haar deponeringen
   door, en het doel blijft open. Een default deponeert alleen waar hij beslist, ook in ref.
5. **Een filter dat onbepaald kan zijn en kan falen, voegt voor een onbepaald lid beide feiten in** (`FoldOpen` en `FilterFault`): de
   tweede gaat voor, de eerste is dan overbodig en verandert geen uitkomst. Eén bijdrageregel blijft zo voor elke vouw gelijk.

### Voorstellen voor beschermde teksten

- `spec/CONSTRUCTS.md` rij 48: "Drools reads it for rules, initialisations, consistency rules, conditions, defaults groups, folds
  over a set and quantifier folds without a timeline, a creation or an IfThenElse, and refuses any other form by its slug
  (DROOLS-370), among them a quantifier fold whose filter, and a fold whose filter holds a fold, that can be open; its build with
  readings refuses every such document."
- `CLAUDE.md`: geen alinea van een gate verandert.

### Open punten

1. **Een kwantorvouw met een filter dat een openable lid leest** kan met dezelfde middelen: elk filter vóór elke toets, met de toets
   van een lid voorlopig waar een filter niet vaststaat (ch. 1.5). Drie goldens weigeren hierop.
2. **Een vouw genest in een filter en een geankerde lezing van open invoer** weigeren door een toets in de analyse. De eerste speelt zonder
   verdere code (de bijdrageregel leest de geneste vouw als elke regel): in een proef buiten de repo, zonder die toets, spelen twee
   van de drie goldens met 0 verschillen op 128 runs en weigert de derde op `anchored-read`. De tweede vraagt het onderwerp van de
   vorm als slot.
3. Voor deel 3: de lezing van een open vouw (`Aggregation undetermined open …`) en van een kwantorvouw noemt het eerste lid in de
   volgorde; `FoldOpen` draagt de lezing en geen plaats. Een knoop `ATOM` en `STANDING_COUNT` heeft geen eigen IR-knoop.

### Gates

Elk los gedraaid op `ed453f529`; daarna veranderde geen code.

- 1 `check-self-contained: PASS`
- 8 `check-red: PASS (102 red cases fault observably in ref and clingo …)`; Drools leg 83 fault-pinned, 19 not-carried, 19 EXCLUDE
  met hun code
- 10 `check-rgs-codes: PASS — 414 codes, 447 sites …`
- 12 `check-drools: PASS`: played 386, DRL-text branch 386, JUnit 2039 tests; `check-drools-codes: PASS — 371 codes …`
- 13 `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens …)`
- 15 `check-graph: PASS`
- 17 `check-readings: PASS (ref traced 1211 golden case files … on both branches 385 goldens and 1073 case files compared, 8468
  readings, 27262 node lines …)`, 2 goldens DROOLS-370

De JUnit-suite: 2039 tests, geen overgeslagen (was 2024). Geen floor, lijst of verwachting van een gate veranderde. De diff van
`backends/drools/src/main`: 15 bestanden, 565 regels erbij en 159 eraf (`TmsTranspiler` 151, `ComputeHelpers` 116, `DrlRendering`
70, `PatternDslRendering` 50, `FoldOpen` 47 en `Boundary` 38 nieuw).

## Result van deel 3: de lezingen en de eindstand

### Wat er is

Commits op `card/open-values-in-drools`: `51fff2be2` (de lezing en de eindstand), `3c106cb3d` (gate 17) en `1222cdd28` (een vouw
in een filter, de pins).

- **De opname.** `RecordingRendering` rekent met de functies van de regel, dus opent zij de grenzen die de uitvoering opent
  (`ATOM`, `emptyTestOverOpen`, de waarde in `ComputeHelpers.valued`). Elke knoop die een open lezing of een oordeel dat niet
  staat passeert, krijgt het (`Outcome.judgement`): onbepaald met de open lezing, of een waarheid door een sluiting. Een lid van
  een samengestelde voorwaarde of van een kwantorvouw dat voorlopig gelezen wordt en faalt, is onbepaald door de lezing ervoor
  (`Composite.past`); de knopen erbinnen houden hun fout. `ATOM` en `STANDING_COUNT` hebben geen knoop.
- **De status.** `Status.OPEN` volgt de drie uitkomsten van ch. 1.5; een consistentie volgt haar bewering voor zichzelf
  (`ReadingRule.assertion`).
- **De eerste in de volgorde.** In een build met lezingen draagt `FoldOpen` de plaats van zijn lid: het filter (of de toets) vóór de
  waarde, dan de selectie, dan de plaats per to-many stap. De bijdrageregel bindt haar zoals de filterfoutregel, alleen waar het lid
  open kan zijn. `standingCount` en `quantifierVerdictOverOpen` lezen de lijst in die volgorde, ook in een vouw in een filter; een
  kwantorvouw telt een lid dat langs twee routes bereikt wordt één keer. Zonder lezingen: geen plaats, dezelfde regels.
- **De stapel** vroeg geen code: de leesregel van de beurt staat onder de feiten van elke stapel (`Decides.open`, `Outcome OPEN`), die
  buiten de beurt leest de verdrongen default, en de initialisatie sluit, ook de onderste van twee.
- **De eindstand**: `EndState.EndObject.open`, gelijk aan de `open`-regels van ref; `link … open` hoort bij een creatie (geweigerd).
- **De records**: `NodeReading` heeft `closure` en `open` (`Fixpoint.OpenValue`), `Contributor` heeft `undetermined`. De vier
  begrippen staan in de README ("Open values") en in de javadoc van `Status`, `NodeReading` en `EndState`.
- **De weigering**: de slug `reading` is weg. Een vouw in een filter wordt gedragen; `set-fold` is alleen nog een som met een
  terugval die open kan zijn. Nieuw in de build met lezingen: een vouw die open kan zijn over een relatie met twee to-many rollen
  (DROOLS-318), want haar leden hebben hier geen plaats; zonder lezingen speelt zij.
- **Gate 17**: `READINGS_REFUSED` leeg, `OPEN` als status, vijf rijen voor de vergelijkingsregel en de teller, en twee vloeren
  (`EXPECTED_MIN_UNDETERMINED_LINES` 65, `EXPECTED_MIN_STATE_OPEN` 79). `TmsDocuments.hasReadings` en de acht overslagen zijn weg;
  `ReadingsCorpusWitnessTest` houdt een open lezing aan de open leden van het fixpunt en `OPEN` aan de `LeftOpen` van de vorm.

### Gemeten (eenmalig, programma's in de werkmap)

Elke regel die gate 17 vergelijkt, met haar vergelijkingsregel, en de eindstand, op beide weergaven, tegen `runner --trace`; en van elke
run de eindstand van de build met lezingen gelijk aan die van de build zonder (een lezing deponeert en schrijft niets).

| wat | documenten | runs | leesregels (per weergave) | verschillen |
|---|---|---|---|---|
| elke golden, getuige en elk document van `open-values/` met de eigenschap, elke case als zij is en met elke invoer weggelaten | 308 | 7808 | 131 827 | 0 |
| dezelfde, willekeurig deel van invoer, parameters en rekendatum weggelaten | 308 | 14 400 | 165 198 | 0 |
| een vouw in een filter: sluiting, open waarde, fout en deponering (EF-005) in de binnenste, kwantorvouw op volgorde, unie van drie overlappende selecties, twee niveaus | 1 | 3200 | 239 700 | 0 |
| samengestelde voorwaarden en kwantorvouwen in elkaar; een lid langs twee routes (ruit) | 2 | 2000 | 69 950 | 0 |

In de eerste rij: 19 762 onbepaalde knopen, 1384 sluitingen, 1743 afgebroken vouwen, 800 onbepaalde kwantorvouwen, 1181 onbepaalde
bijdragen, 2618 verdrongen lezingen, 9434 keer `OPEN`, 15 793 onbereikte knopen; in de laatste 166 knopen met een fout in een
voorlopig lid. Een fout van ref is een fout van de backend (8, 138, 650 en 428 runs).

Van de goldens met de eigenschap weigeren alleen met lezingen: `anchor/jutterswet-bootberging`, `jutterswet-ketenkwantor` en
`jutterswet-muntketen` (DROOLS-318).

### Een document zonder de eigenschap, en zonder lezingen

De DRL-tekst van elke golden en elk rood document, zonder en met lezingen: **1014 teksten van 507 documenten byte-gelijk, 0 andere**. De
tekst zonder lezingen van de 478 documenten van de meting met de eigenschap: 474 byte-gelijk aan die van het begin van dit deel; de
vier andere hebben een vouw in een filter, die toen weigerde. Over de 228 goldens die met lezingen gebouwd worden: gewoon zonder en met
lezingen 30 641 en 43 486 regels (1545 en 2491 `rule`s), strikt 33 555 en 48 696 (1770 en 2716): met lezingen +12,0% regels en +9,0%
`rule`s tegenover gewoon met lezingen (zonder lezingen +9,5% regels).

### Acceptance

- **De voorbeelden** (`OpenReadingsWitnessTest`, 12 tests): `wv_5_12_afleiden_uit_leeg` (ook `WV-5.12a-`), `-open-composite`,
  `-open-past-undetermined`, `-open-quantifier-fold` (zonder filter), `-open-fold-over-a-set`, `-open-stack`, `-open-path`,
  `-open-status`; `-open-creation` en `WV-5.12a-open-link` als weigering. De pins zijn 76 cases met de regels van `runner --trace`
  (`src/test/resources/open-readings/`). `-open-shared-name`: de backend weigert het document (DROOLS-054, één bean-eigenschap voor
  beide leden); leest geen regel het kenmerk, dan zijn lezing en eindstand die van ref (los gemeten).
- **Een vouw in een filter**: `open-values/fold-in-a-filter`, vier cases met met de hand gerekende verwachtingen, en een wijziging van
  een binnenste lid tussen twee keer vuren (`a_value_on_the_bean_between_two_fires`).
- **Rood eerst**: op de jar van `e0461147e` weigert de build met lezingen elk document met `form: reading`, en
  `fold-in-a-filter` met `form: set-fold`.
- **Geen test verzwakt.** Vervallen: de test dat de build met lezingen weigert, en de regel van een golden met een vouw in een filter in
  de weigertest, die nu in de test van de gedragen documenten staat.

### Voorlopige besluiten

1. De plaats staat op `FoldOpen` en alleen in een build met lezingen (ontwerpnotitie, door de hoofdsessie en een tweede lezer gelezen).
2. Een vouw over een relatie met twee to-many rollen die open kan zijn, weigert met lezingen (DROOLS-318) in plaats van een lid te kiezen.
3. De open leden van de eindstand zijn een eigen lijst naast `members`, zodat een afnemer die `members` leest alleen waarden ziet.
4. Code DROOLS-372: een lid dat open kan zijn zonder `ListPlace` (contractbreuk, zoals 357–359).
5. Twee eigen vloeren in gate 17, want de bestaande tellen de onbepaalde regels niet apart.
6. De pins zijn de trace van het orakel als tekst; de suite draait ref niet.

### Voorstellen voor beschermde teksten

- `CLAUDE.md`, gate 17: de vloeren `EXPECTED_MIN_UNDETERMINED_LINES` en `EXPECTED_MIN_STATE_OPEN` achter
  `EXPECTED_MIN_STATE_MADE`; de zin over `READINGS_REFUSED` kan blijven (de lijst is leeg).
- `spec/CONSTRUCTS.md` rij 48: "… among them a quantifier fold whose filter can be open and a sum whose fallback can be open; its
  build with readings gives the readings and the end state of ch. 5.12 and 5.12a, and refuses a fold that can be open over a
  relation of two to-many roles (DROOLS-318)."

### Open punten

1. De volgorde van de leden van een relatie met twee to-many rollen (ch. 5.7a) stelt de case-invoer niet; met haar vervalt punt 2
   van de besluiten, en DROOLS-318 in het algemeen.
2. Blijft geweigerd: een kwantorvouw met een open filter, een geankerde lezing van open invoer, creaties, `IfThenElse`, ongeordende
   groepen met een initialisatie, een terugval die open kan zijn.
3. Een attribuut en een kenmerk op één bean-eigenschap, door geen regel gelezen: gemeten met een kenmerk naast een getal; de eindstand
   van een booleaans attribuut naast zo'n kenmerk is niet gemeten.
4. Voor de distributie bij haar pin: `Status` heeft `OPEN` (een `switch` zonder default compileert niet meer), `NodeReading`,
   `Contributor` en `EndObject` hebben componenten erbij, en een afnemer die `NodeReading.value` null als leeg toont, vraagt eerst
   `undetermined()`.

### Gates

Elk los gedraaid op `1222cdd28`; daarna veranderde geen code.

- 1 `check-self-contained: PASS`
- 8 `check-red: PASS (102 red cases fault observably in ref and clingo …)`
- 12 `check-drools: PASS`: played 386, DRL-text branch 386, JUnit 2050 tests; `check-drools-codes: PASS — 372 codes …`
- 13 `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens …)`
- 15 `check-graph: PASS`
- 17 `check-readings: PASS (ref traced 1211 golden case files … on both branches 387 goldens and 1085 case files compared, 8587
  readings, 27572 node lines …)`, 65 onbepaald, 79 open leden

De JUnit-suite: 2050 tests, geen overgeslagen (was 2039). Geen verwachting, pin of bestaande vloer van een gate veranderde.

## Result van deel 4: de laatste vormen, behoud als getuige, de staart

### Wat er is

Commits op `card/open-values-in-drools`: `46629ae1d` (de kwantorvouw onder een open filter, de geankerde lezing), `98ebb1f41`
(behoud als getuige) en `4335bfab7` (de boodschap van DROOLS-370, de javadoc, de README).

- **De kwantorvouw onder een filter dat open kan zijn** leest per lid één `MemberTest`, in elke kwantorvouw van zo'n filter, ook waar de
  toets kan falen noch deponeren. De bijdrageregel blijft één regel (`Consequence.FilteredContribution` met een `MemberTest`): zij vraagt
  of het filter voor een ander niet onwaar is (`ACTS`), en het `MemberTest` draagt het oordeel van het filter naast hoe de toets
  eindigde, en het lid. Geen nieuw feit en geen nieuwe regel. `Composite.read(filter, toets)` telt het lid voor de vorm zelf en voor een
  ander naar Kleenes tabel van ch. 1.5 en noemt per stand de lezing van het filter waar dat onbepaald is en anders die van de toets.
  Staat een filter van de vouw niet vast, ook dat van een later lid, dan leest `Composite` elke toets voorlopig (`past`).
- **Wat een filter doet.** Een onbepaald filter breekt een kwantorvouw niet af, dus de regel van de `FilterFault` neemt hier alleen fouten
  (`filterFaultRule(…, breaksOff)`); een fout van een filter is de fout van de vouw, ook na een filter dat open is, en de eerste in de
  volgorde telt. Een deponering van een filter staat naast een ander dat open is. Een lid met een vaststaand onwaar filter geeft niets.
- **De plaats.** Kan de toets falen of deponeren, dan heeft een lid zijn plaats zoals elders. Kan hij dat niet, dan bindt alleen een build met
  lezingen haar; een lid langs twee routes heeft dan een toets per route en wordt op zijn eerste plaats gelezen (`MemberTest.inOrder`).
- **De geankerde lezing van open invoer**: `Reads.own` onder `atForm` geeft `READ_INPUT` het onderwerp van de vorm, het slot dat de
  identiteitstoets heeft. Een geankerde knoop in een vouw per lid van een andere vouw blijft geweigerd (DROOLS-293), in elk document.
- **De lezing**: de bijdragers van zo'n vouw zijn de leden waarvan het filter niet vaststaand onwaar is, elk met de waarheid van zijn toets,
  met hun positie; `SessionReaders.contributors` herspeelt ze met dezelfde `Composite`.
- **De weigering**: `UncarriedForms` heeft `quantifier-fold` en `anchored-read` niet meer; van de analyse is alleen `set-fold` over (een som
  met een terugval die open kan zijn).
- **Behoud** (`CompleteInputPreservationWitnessTest`): elke golden zonder de eigenschap, met `inferFromEmpty: false` erbij, speelt haar eigen
  cases op beide weergaven in een sessie die elke invoer volledig acht: 237 goldens en 691 cases per weergave; 153 goldens weigeren met
  DROOLS-370, en elke andere weigering is die van de golden zonder de eigenschap. 13,9 s alleen, in één JVM.
- **De teksten**: DROOLS-370 zegt "een lezing waarin niets volgt uit een waarde die open is, niet gevuld en niet vaststaand leeg"; de javadoc zegt
  "een document met `inferFromEmpty: false`"; "Open values" in de README is één beschrijving, met de vier begrippen vooraan en
  wat geweigerd blijft aan het eind. De zin van `IrLoader` bij een verkeerde waarde blijft: zij is de zin van de OCaml-loader.
- **Niet geraakt**: de productieruntime, `FoldOpen`, de plannen en de tekst van een gewoon document en van een strikt document dat al
  gedragen werd.

### Gemeten (eenmalig, programma's in de werkmap)

Elke regel die gate 17 vergelijkt en de eindstand, op beide weergaven, tegen `runner --trace`; van elke run de eindstand van de build met lezingen
gelijk aan die van de build zonder; van een fout ook de EF-codes.

| wat | documenten | runs | leesregels (per weergave) | fouten | verschillen |
|---|---|---|---|---|---|
| elke golden, getuige en elk document van `open-values/` met de eigenschap, elke case als zij is en met elke invoer weggelaten | 318 | 8364 | 140 426 | 8 | 0 |
| dezelfde, willekeurig deel van invoer, parameters en rekendatum weggelaten | 318 | 11 872 | 139 774 | 130 | 0 |
| de zes eigen documenten, elke invoer weggelaten | 6 | 394 | 2284 | 0 | 0 |
| willekeurige kwantorvouwen: kwantor × filter × toets (open, sluiting, deponering, fout, geankerd, genest), in een samengestelde voorwaarde en in een vouwfilter; een ruit met een filter dat deponeert | 39 en 71 | 7200 en 11 600 | 215 685 en 291 075 | 1136 en 2602 | 0 |
| één kwantorvouw per document, met filters die met twee codes falen, ook naast een deponering en vóór en na een filter dat open is | 123 | 14 560 | 85 433 | 2876 | 0 |

In de vierde rij samen: 22 422 onbepaalde kwantorvouwen, 24 217 onbepaalde bijdragen, 7601 sluitingen, 150 knopen of bijdragen met een fout in een
voorlopig gelezen toets. In de vijfde zijn de codes van elke fout gelijk aan die van ref (EF-011 en EF-014, 1404 cases); een document met meer
vormen kan in de backend meer codes geven, omdat ref na de eerste ophoudt. De meting ziet drie ingebrachte fouten: zonder `past` 110
verschillen, zonder de waarheid van de toets onder een open filter 864, met de lezing van het filter waar die van de toets hoort 220.

De gevallen van de tweede lezer staan in de getuigen: een filter dat open is of een sluiting over een toets die sluit, deponeert of faalt, onder
`all`, `none`, `atLeast`, `atMost` en `exactly` (`open-readings/reading-quantifier-filter`); twee filterfouten met verschillende codes en
een fout vóór en na een open filter (`quantifier-fold-filter-faults`); beslist vóór het eerste lid over een toets die faalt
(`quantifier-fold-matrix-faults`); twee routes onder `atLeast 2` en `exactly 1` met een filterdeponering, met en zonder lezingen
(`quantifier-fold-two-routes`). Een drempel 0 bestaat niet (WF-041).

### Een document zonder de eigenschap, en wat al gedragen werd

De DRL-tekst van elke golden en elk rood document, zonder en met lezingen, naast die van deel 3: alle gelijk (510 documenten, de 388 pins onder
hen). Van de 1032 teksten van strikte documenten zijn er 1006 gelijk; de 26 andere zijn van dertien documenten waarvan de backend een
regel weigerde.

### De omvang

Niet-lege regels, `rule`s en functies van de DRL-tekst, over de 234 goldens die met lezingen gebouwd worden (de twee met de eigenschap zelf niet
meegerekend):

| | zonder lezingen | met lezingen |
|---|---|---|
| gewoon | 32 903, 1627, 1674 | 46 152, 2599, 2681 |
| strikt | 36 056 (+9,6%), 1859 (+14,3%), 1757 (+5,0%) | 51 985 (+12,6%), 2831 (+8,9%), 3061 (+14,2%) |
| de 8 nieuwe, gewoon | 2732, 109, 198 | 3380, 153, 242 |
| strikt | 2745 (+0,5%), 116 (+6,4%), 208 (+5,1%) | 3777 (+11,7%), 160 (+4,6%), 330 (+36,4%) |

De grootste groei van een nieuwe is +27,6% (`jutterswet-vuurtoren`, 145 naar 185 regels); vier krimpen, omdat een toets die niet kan falen of
een oordeel dat `ComputeHelpers.judge` geeft zijn try en catch niet in de tekst heeft. **Bouwtijd** van de vijf grootste nieuwe (`anchor-forms`,
`fold-filter-nested-aggregate`, `jutterswet-zwaarste-vondst`, `fold-filter-subject-two-spellings`, `anchor-once-per-member`), mediaan van vijf in
één warme JVM, gewoon naar strikt: exec-model 12 naar 12 ms, DRL-tekst 233 naar 275 ms (+18%), de hoogste +31%.

### Acceptance

- **Getuigen** (`OpenValuesWitnessTest`): `quantifier-fold-filter` (`WV-1.5-quantifier-fold-*`, keurder; 11 cases), `past-undetermined-filter`
  (`WV-1.5-past-undetermined-filter`; 8), `quantifier-fold-matrix-faults` (4 en 1 rode), `quantifier-fold-filter-faults` (3 en 4 rode),
  `quantifier-fold-two-routes` (5), `anchored-read` (8), met de hand gerekende verwachtingen die ref ook speelt; een wijziging van de bean
  van het onderwerp van de vorm tussen twee keer vuren. `WV-1.5-past-undetermined-fold` stond er.
- **De lezing** (`OpenReadingsWitnessTest`): `WV-5.12-open-quantifier-fold` met het filter, op de getuige van ref zelf en woord voor woord de regel
  van ch. 5.12; de zes documenten; `reading-quantifier-filter`, vijf kwantoren maal vijf paren van filter en toets.
- **De getuigen van ref**: 32 van de 34 spelen (144 cases per weergave); `object-creation` en `fact-creation` weigeren.
- **Rood eerst**: op de jar van deel 3 weigeren de zes documenten met `form: quantifier-fold` (vijf) en `form: anchored-read`; de
  behoudstest is rood in een sessie die haar invoer niet volledig acht (148 goldens).
- **Geen test verzwakt.** Vervallen: de twee tests die `quantifier-fold` en `anchored-read` als weigering vroegen (hun goldens staan nu in de test
  van de gedragen documenten); de zin van DROOLS-370 in `InferFromEmptyRefusalWitnessTest` volgt de nieuwe tekst.

### Voorlopige besluiten

1. Een kwantorvouw onder een open filter leest `MemberTest`s in plaats van `FoldOpen`s: de lezing noemt per bijdrager de waarheid van de toets,
   en een lid kan voor zichzelf door zijn toets en voor een ander door zijn filter onbepaald zijn (ontwerpnotitie, akkoord).
2. De zin van `IrLoader` bij een verkeerde waarde van `inferFromEmpty` blijft die van de OCaml-loader en ref.
3. De behoudstest staat op de build zonder lezingen, met `Input.closedInputs`; elke andere weigering dan DROOLS-370 moet die van de golden zonder
   de eigenschap zijn (strenger dan een lijst van codes).

### Voorstellen voor beschermde teksten

- `spec/CONSTRUCTS.md` rij 48: "… and refuses a document with another form by its slug (`DROOLS-370`), a sum whose fallback can be open
  (`set-fold`) among them; its build with readings …" (de twee andere slugs vervallen). Gate 12: geen tekst.
- `CLAUDE.md`: geen alinea van een gate verandert.

### Voor de pin van de distributie

Elke wijziging van de publieke API van `backends/drools` sinds `4b9c58943~1`, met wat een afnemer doet.

| wat | een afnemer |
|---|---|
| `Input.characteristicAbsent(bean, kenmerk)` nieuw | geeft door wat een gebruiker uitdrukkelijk "nee" zegt; alleen onder `inferFromEmpty: false` van belang (een boolean op de bean zegt dat niet) |
| `Input.closedInputs()` nieuw | niet nodig; een meting |
| `EndState.EndObject.open` (component erbij, `List<EndState.Open>`) | toont deze leden als "staat niet vast"; wie alleen `members` leest, toont een open lid als leeg, en dat is het niet. Constructor: een argument erbij |
| `EndState.Open(characteristic, name, given)` nieuw | `given` is invoer naast een schrijver die open is; zij staat niet, dus niet als uitkomst tonen |
| `Fixpoint.open` (component erbij, `List<Fixpoint.OpenValue>`) | hetzelfde per afgeleid fixpunt; constructor: een argument erbij |
| `Fixpoint.OpenValue(object, member, characteristic)` nieuw | het lid van een open lezing |
| `ConditionReading.Status.OPEN` nieuw | een `switch` zonder default compileert niet meer; `OPEN` is geen `FAILS`: de vorm laat haar doel open |
| `NodeReading.closure` en `.open` (componenten erbij), `.undetermined()` | vraag `undetermined()` vóór `value()`: een null is leeg én staat vast, tenzij de knoop onbepaald is; `closure()` is een waarheid voor de vorm zelf die een andere invoer kan veranderen |
| `Contributor.undetermined` (component erbij) | `value()` null met `undetermined()` is geen onwaar |
| `CharacteristicState.holds()` geeft `Object` (was `boolean`) | een feit van de sessie; een lezer van feiten vergelijkt met `Boolean.TRUE`, of leest `Fixpoint` |
| `Decides.open` (component erbij), `Outcome.Kind.OPEN`, `RuleOrigin.Role.RECONCILE_OPEN` nieuw | een lezer van de regels van een stapel (de rolnaam in de JSON) kent één rol erbij |
| `TmsTranspiler.openableMembers()`, `OpenableMembers` nieuw | zegt vóór een sessie welke leden open kunnen zijn |
| `LeftOpen`, `OpenInput`, `OpenMember`, `OpenRead`, `FoldOpen`, `Undetermined`, `Composite` nieuw | feiten en seinen van de sessie; een afnemer leest ze niet |
| `Watch.reachingUpdated()`, `ProducerConsumer.wireNavigation(model, navigaties, geankerd)` nieuw | geen werk; de bestaande overloads blijven |
| de DRL-tekst van zo'n document vraagt de global `open_invoer` | `DroolsBackend.insert` zet haar; wie zelf invoegt, roept `insert` aan |
| DROOLS-370 met `form` een slug van `UncarriedForms` (was altijd `document`), in een vorm die de analyse weigert achter "transpiler SKIP: regel: … — " | toon de zin; de slug is de vorm |
| DROOLS-371 (`insert`: `characteristicAbsent` op een vreemde bean, een niet-gedeclareerd kenmerk of een dat geldt), DROOLS-372 (een lid dat open kan zijn zonder `ListPlace`) nieuw | contractbreuken van de afnemer |
| DROOLS-318 ook voor een vouw die open kan zijn, in een build met lezingen | de build zonder lezingen speelt |
| casevorm: `characteristicsAbsent`, `attributeOpen`, `characteristicOpen`; EF-047 (kenmerk in beide lijsten); `PART.value.open` "openstaand" | een scenariolezer geeft ze door |

**"Open" aan een gebruiker tonen.** Welke uitkomst ontbreekt: de leden in `EndState.EndObject.open`. Welke invoer haar openhoudt: de lezingen van
de vormen die dat lid schrijven (`ConditionReading` met status `OPEN`, en bij een stapel de default die `OPEN` is) noemen per onbepaalde knoop
`NodeReading.open()`, object en lid; is dat lid zelf afgeleid, dan volgt men zijn vormen, tot een lid dat geen vorm schrijft: dat is de vraag
aan de gebruiker. Een knoop noemt de eerste open lezing die de evaluatie tegenkwam, dus na een antwoord kan een volgende vraag komen. Zegt
een gebruiker "geen" of "nee", dan staat dat alleen vast bij een lid dat `(volledig gegeven)` is of, voor een kenmerk, met
`Input.characteristicAbsent`; een attribuut zonder waarde dat niet volledig is, blijft open. Een uitkomst met `closure()` in haar lezing staat, maar
kan door een gegeven invoer veranderen.

### Open punten

1. Blijft geweigerd: creaties, `IfThenElse`, ongeordende groepen met een initialisatie, een terugval die open kan zijn, en met lezingen een vouw
   die open kan zijn over een relatie met twee to-many rollen.
2. Een kwantorvouw waarvan de toets kan falen of deponeren, over een pad met twee routes, blijft geweigerd (`QUANTIFIER_MEMBER_TWO_ROUTES`), in elk
   document; `MemberTest` draagt het lid nu alleen onder een open filter, en met het lid in elk `MemberTest` vervalt die weigering.

### Gates

Elk los gedraaid op `4335bfab7`; daarna veranderde geen code.

- 1 `check-self-contained: PASS`
- 8 `check-red: PASS (102 red cases fault observably in ref and clingo …)`
- 12 `check-drools: PASS`: played 386, DRL-text branch 386, JUnit 2055 tests; `check-drools-codes: PASS — 372 codes …`
- 13 `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens …)`
- 15 `check-graph: PASS`
- 17 `check-readings: PASS (ref traced 1211 golden case files … on both branches 387 goldens and 1085 case files compared, 8587
  readings, 27572 node lines …)`

De JUnit-suite: 2055 tests, geen overgeslagen (was 2050). Geen pin, vloer, lijst of verwachting van een gate veranderde.

## Landing van deel 4 en afsluiting (hoofdsessie, 2026-11-22)

Nagelezen: `MemberTest` met het oordeel van zijn filter, `Composite.read(filter, toets)` met de lezing die elke toets voorlopig maakt, de
weergaven, `SessionReaders.contributors` en de geankerde lezing. Nagerekend: 238 goldens bouwen strikt (33.223 naar 36.164 regels, +8,9%; 1643
naar 1875 `rule`s), 387 gewone teksten gelijk, 230 strikte teksten gelijk aan deel 3, 238 volledige gelijk aan gewoon. Een tweede lezer keek naar
de ontwerpen van deel 3 en 4; zijn toetsgevallen staan in de getuigen. De kaart is af: Drools rekent de lezing van ch. 1.5 en geeft de lezingen en de
eindstand van ch. 5.12 en 5.12a, op de vormen na die zij bij naam weigert (Open punten hierboven). De pin van de distributie is haar eigen kaart.
