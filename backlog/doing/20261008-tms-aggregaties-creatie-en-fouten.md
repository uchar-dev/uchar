# tms-aggregaties-creatie-en-fouten: de truth-maintenance-tak draagt aggregaties en creaties, en voert een fout als logisch feit

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08): de truth-maintenance-tak wordt uitgebouwd tot vervanging van de
exec-modeltak. Dit is de eerste van vier bouwkaarten, daarna
`20261008-tms-expressievormen-en-rekendatum`,
`20261008-tms-verdeling-en-tijdlijnen` en `20261008-tms-omschakeling`. Het
zijn bouwkaarten, geen haalbaarheidsproeven. Tellingen uit de corpusdekking
van de proeftak in dat `Result:` (bij (e)); het corpus is sindsdien niet
veranderd.*

## Aanleiding

De tak (`DroolsBackend.Branch.TRUTH_MAINTENANCE`, vlag `--tms`, pakket
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`) speelt
66 goldens zonder FAIL en weigert er 226 per vorm. De grootste familie is
`Aggregation`: de eerste weigering van 63 goldens in een regel en van 2 in een
defaults-groep (`jutterswet-premiestaffel`, `jutterswet-sluitdag-gemeld`).
Objectcreatie weigert hij in 8 documenten, feitcreatie in 1. Het `Result:`
noemt dit de grootste risico's, en daarom komen ze vooraan:

- een `accumulate` moet opnieuw rekenen als een lid, of een afgeleide waarde
  van een lid, verandert, en mag daarvoor alleen lezen wat de LHS bindt;
- een logisch ingevoegd object vervalt met zijn rechtvaardiging, en
  recursieve creatie is precies de positieve kring die `Kringen` nu weigert
  (waarneming 5: tien kringen in 322 documenten, alle in documenten met
  objectcreatie);
- de tak heeft geen strata en komt langs tussenstanden die ref niet ziet, zoals
  een aantal dat nog 0 is of een som zonder de laatste term. Een fout die daar
  geworpen wordt, kan truth maintenance niet intrekken (waarneming 2).

Goal: de tak draagt aggregaties, object- en feitcreatie, en voert een
evaluatiefout als logisch feit dat pas aan het fixpunt telt; de goldens die hij
daarop weigerde, spelen of weigeren op een vorm van een volgende kaart.

## Wat de tak erbij krijgt

1. **Aggregaties** (`Aggregation`, ook in de rechtvaardiging en de consequentie
   van een default): een `accumulate` in de LHS over de leden die het pad bindt,
   met de afgeleide waarden van die leden als patronen (ontwerpeis 1 van de
   proefkaart).
2. **Fouten als logisch feit.** Een evaluatiefout, zoals een deling door een
   numerieke nul (RUNTIME-SEMANTICS ch. 1.4), wordt een logisch ingevoegd feit
   met de rechtvaardiging van de regel die hem maakt. De case meldt de fouten
   die aan het fixpunt staan, in een vaste volgorde. Een consequentie werpt
   geen evaluatiefout meer. De deponeringen op het kanaal (ch. 5.6) zijn in de
   tak al logische feiten.
3. **Objectcreatie en feitcreatie** (RUNTIME-SEMANTICS ch. 5.5): één creatie per
   regel per onderwerp. *Gewijzigd op 2026-10-08, besluit van de eigenaar
   tijdens deze kaart:* een creatie vuurt alleen aan een fixpunt van de
   regellaag, in generaties zoals in ref; het gecreëerde object, zijn
   initialisaties en zijn links zijn gestelde feiten, gesleuteld per (regel,
   onderwerp), en vervallen niet. Hier stond eerst dat het object met zijn
   rechtvaardiging vervalt; een logisch ingevoegd object kan zichzelf via zijn
   eigen uitvoer in stand houden, en zijn initialisaties zouden later
   meeveranderen, terwijl ch. 5.5 en ref ze bij het maken uitrekenen. `Kringen` laat een kring door
   die WF-045 toelaat (door een creatielid van een recursieve regelgroep),
   sleutelt op de knopen van IR-SPEC ch. 5.1 (attribuut, kenmerk, kanaal,
   populatie, relatie) en blijft een kring met een negatieve rand weigeren
   (WF-046).
4. **Het beslisblok van de creatie** (eerst de binding, dan de terugval op het
   typenpaar, de guards op de referentievelden, de weigerzinnen en
   `verbindendeFeittypen`) staat nu als privékopie in `ExecModelTranspiler` en
   in `DrlTranspiler`. De tak krijgt geen derde kopie: het blok komt één keer in
   `schema/FactModelGenerator.java`, zoals
   `20260923-drools-rolpaar-ontdubbeling` vraagt. Of de twee andere takken er
   in deze kaart al op overgaan, beslist de bouwer. Doen ze dat, dan blijven hun
   sweeps byte-gelijk en sluit die kaart mee.

## Acceptatie

- (a) Rood-eerst getuigen onder
  `backends/drools/src/test/java/dev/uchar/regelspraak/cases/`, elk gedraaid in
  elke volgorde van de betrokken regels in het model:
  - een aggregatie over een afgeleid lid dat later verandert;
  - een aggregatie in een default;
  - een tijdelijke deling door nul onder een aggregatie die aan het fixpunt
    verdwijnt, en een fout die aan het fixpunt blijft staan;
  - een idempotente niet-recursieve creatie, en een creatie waarvan de
    voorwaarde alleen op een tussenstand geldt en die daarom niet gebeurt
    (gewijzigd op 2026-10-08, met punt 3);
  - een recursieve creatie die WF-045 toelaat, en een kring met een negatieve
    rand die geweigerd blijft.
- (b) De corpusgetuige (`TmsCorpusWitnessTest`) heeft geen FAIL. Zijn vloer
  `EXPECTED_MIN_PLAYED` gaat naar het gemeten aantal, als bewuste verhoging in
  de commit die het bereikt. Elke golden die de tak op `Aggregation`,
  `ObjectCreation` of `FactCreation` weigerde, speelt of weigert met naam op een
  vorm van een volgende kaart; het `Result:` geeft de nieuwe telling per
  familie.
- (c) `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over
  `transpiler/tms/` geeft niets.
- (d) De exec-modeltak en de DRL-teksttak zijn ongewijzigd, op punt 4 na: gate
  12 groen met `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` gelijk; alle gates van
  `CLAUDE.md` groen.

## Raakvlakken

- `20261008-fout-op-tussenstand` vraagt eerst een normbesluit: telt een fout op
  een tussenstand? Deze kaart wacht daar niet op. Punt 2 is nodig omdat déze
  tak tussenstanden bezoekt die ref niet ziet, en op de goldens geven ref en
  clingo al hetzelfde antwoord. Wel bepaalt dat besluit wat er wordt van twee
  weigeringen die de tak nu heeft omdat hij anders speculatief rekent: een
  deling in een default, en een deling in een initialisatie naast een andere
  schrijver (document A van die kaart). Die blijven staan tot het besluit. Het
  `Result:` geeft wat de tak met fouten als feit op de documenten A en B
  antwoordt, in beide volgordes, als meting voor die kaart.
- Waarneming 3 van de proefkaart hoort bij dezelfde vraag: een deponering op een
  tussenstand trekt de tak in, terwijl ref haar houdt (RUNTIME-SEMANTICS ch. 6.1,
  "Recorded residuals").
- `20261006-check-wf046-door-recursieve-creatie`: de frontend laat een negatieve
  rand op een kring door een recursieve creatie door; de tak weigert zo'n kring
  zelf (punt 3).
- `20261008-deling-door-leeg-onder-initialisatie`: de tak geeft daar al het
  goede antwoord; geen werk in deze kaart.

## Review

Zoals in `20261007-drools-logische-afleiding`: een lens na elke bouwstap, een
eindlens over de hele diff vóór het `Result:`, en na elke stap een doorloop van
het hele corpus (de corpusgetuige draait in de suite). In die kaart vond de
corpusdoorloop meer dan de lenzen. Een lensvondst wordt alleen overgenomen met
een concreet faalscenario op echte invoer of een getuige die het laat zien;
anders wordt ze met één zin reden afgewezen in het `Result:`.

## Omvang

Groot. De aggregatie is de grootste familie van het corpus, de creatie raakt
`Kringen` en de bonen, en fouten als feit veranderen de afloop van elke case
in de tak.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`,
`backends/drools/src/main/java/dev/uchar/regelspraak/schema/FactModelGenerator.java`
(punt 4), `cases/CaseRunner.java` en `cases/DroolsBackend.java` (de fouten aan
het fixpunt, de gecreëerde objecten bij het terugzetten), bij punt 4 de
beslisblokken in `ExecModelTranspiler` en `DrlTranspiler`, tests onder
`backends/drools/src/test/java/`, de sectie "The truth-maintenance trial branch"
in `backends/drools/README.md`, kaartverplaatsing. NB: serieel met andere
Drools-kaarten.

Out of scope: de exec-modeltak en de DRL-teksttak behalve punt 4, `scripts/`,
`corpus/`, `spec/`, de toolchain, en de vormen van de volgende kaarten.

## Result (2026-10-08)

*Branch `card/tms-aggregaties-creatie-en-fouten`, vanaf main `73fd91a1`. De gates draaiden op `1f2ad05c`; de commit
van dit Result verandert alleen kaarten. `20260923-drools-rolpaar-ontdubbeling` krijgt een Result en gaat bij de landing
mee naar `done/`.*

| Commit | Inhoud |
|---|---|
| `e1ee0b61` | claim |
| `361ca2ec` | stap 1: een evaluatiefout is een logisch feit dat aan het fixpunt telt |
| `42859692` | stap 2: aggregaties als `accumulate` over logische bijdragen |
| `4345fdc0` | lensfix stap 1: de ERROR-regel van de CLI noemt elke staande fout |
| `a987275c` | lensfix stap 2: een filterfout is de fout van de vorm die de vouw evalueert |
| `21bc5085` | stap 3, punt 4: het beslisblok van de objectcreatie staat één keer |
| `fdeb6dfb` | stap 3: `Kringen` weigert wat WF-045 en WF-046 weigeren, als de verifier |
| `2907fa91` | stap 3: een creatie vuurt aan een fixpunt van de regellaag, in generaties |
| `582b200e` | lensfix: een filterfout noemt het eerste falende lid in de vouwvolgorde |
| `739ca46a` | lensfix stap 3: getuige voor de fasering van objectcreaties |
| `40b75291` | de vloer van de corpusgetuige naar de gemeten 120 |
| `6b4c966f` | README |
| `fdaa52f4` | eindlensfix: creaties vuren per stratum van de referentie-interpreter |
| `1f2ad05c` | lensfix: het budget per stratum telt elke pas met een wijziging |

### Wat er staat, en waarom zo

- **Fouten als logisch feit.** Een deling door een numerieke nul (ch. 1.4) is het logische feit `Fout`, gerechtvaardigd
  door de activering die deelde. Een consequentie werpt geen evaluatiefout meer. In een voorwaarde zegt de toets "geldt
  niet" waar de evaluatie faalt, en een eigen regel met dezelfde LHS voegt de fout in. Aan het fixpunt werpt
  `zetAfgeleideWaarden` de staande fouten vóór het terugzetten: één als de benoemde `DelingDoorNul` (dezelfde
  ERROR-regel als de exec-modeltak), twee of meer als `Evaluatiefouten`, gesorteerd op vormnaam en zin, met een kop die
  elke regel noemt. Andere uitzonderingen (contractbreuk, vuurplafond, een datumstap buiten het bereik van java.time)
  blijven werpen. De weigeringen van een deling in een default en in een initialisatie naast een andere schrijver
  blijven tot het normbesluit van `20261008-fout-op-tussenstand`.
- **Aggregaties.** Per bereik voegt een bijdrageregel per lid dat door het filter komt het logische feit `Bijdrage` in;
  de lezende regel heeft een `accumulate` over die bijdragen in haar LHS, met de telling ernaast, zodat een lege vouw
  een rij geeft. Dit wijkt af van de letter van punt 1 ("de afgeleide waarden van die leden als patronen in die LHS"):
  de afgeleide waarden staan als patronen in de LHS van de bijdrageregel. Reden: filters, lezingen en de foutregel
  blijven gewone regels, en een lid dat via twee routes of twee selecties van een unie bereikt wordt, telt één keer
  omdat gelijke logische feiten samenvallen. Verandert het resultaat van een `accumulate`, dan vuurt dezelfde
  activering opnieuw en trekt Drools de logische feiten van de vorige vuring in (getuige).
- **Filterfout.** Een lid waarvoor het filter faalt, geeft alleen een merkfeit `Filterfout`; de fout ontstaat pas waar
  de evaluatie van de lezende vorm de vouw bereikt, gerechtvaardigd door die vorm. Falen meer leden, dan noemt de fout
  het eerste in de vouwvolgorde van ch. 5.11, zoals ref. Het beanmodel bewaart die volgorde niet, dus de caselezer
  stelt op deze tak per lid een feit `Lijstplaats` (eerst de geschreven leden in caseorde, dan de gespiegelde op
  labelvolgorde, ch. 5.7a). Een vouw met een delend filter over een gecreëerde relatie weigert de tak met naam; geen
  golden heeft er een.
- **Het beslisblok** van de objectcreatie staat één keer, in `FactModelGenerator.creatiekoppeling` en
  `verbindendeFeittypen`. De exec-modeltak en de DRL-teksttak gaan erop over; elk voert de beslissing op zijn eigen
  manier uit. Daarmee is `20260923-drools-rolpaar-ontdubbeling` gedaan (zie (d)).
- **`Kringen`** spiegelt de WF-045/046-toets van de verifier: de knopen van ch. 5.1, de sleuteling van de verifier ook
  bij subtypen, een kring door een creatielid van een recursieve regelgroep toegelaten, een negatieve rand op een
  kring geweigerd. De drie wrakboekingen worden niet meer op een schijnkring geweigerd. Eén bewuste afwijking: het
  foutkanaal van een rekenende regel is een knoop (ontwerpeis 6 van de proefkaart), dus een kring daardoor weigert de
  tak luid waar de verifier haar toelaat (waarneming 7).
- **Creatie** (besluit van de eigenaar, 2026-10-08; punt 3 en acceptatie (a) zijn aangepast). Een creatie vuurt alleen
  aan een fixpunt van de regellaag. Elke creatie krijgt het stratum dat ref haar vorm geeft (ch. 6.1: een lezing door
  een vorm die onherroepelijk schrijft is stratum-strict; ch. 6.3: stratum k tot zijn fixpunt vóór k+1);
  `Kringen.strata` rekent ze zoals ref over de graaf van de verifier, en geeft voor alle 83 creaties in het corpus
  dezelfde strata als ref. De creatieregels staan in twee agendagroepen; de driver loopt de strata oplopend af en
  vuurt per stratum eerst de regellaag tot rust, dan per generatie de feitcreaties en de objectcreaties (de volgorde
  van een pas van ref), met daarna weer de regellaag, tot een generatie niets maakt. Het budget telt per stratum,
  zoals ref: een generatie is een pas, en een netto wijziging van de regellaag na de laatste creatie van het stratum
  telt als nog een pas; de 101e pas met een wijziging is een benoemde fout. Wat een fase maakt, draagt haar nummer, en
  een creatieregel neemt geen object of link van haar eigen fase. Het object, zijn initialisaties, zijn links
  (`Koppeling`) en de merker `Gemaakt(regel, onderwerp)` worden gesteld ingevoegd en vervallen niet. Elke relatie die
  een creatie kan maken, wordt over `Koppeling` gelezen, ook de invoerlinks. Reden van het besluit: een logisch
  ingevoegd object kan zichzelf via zijn eigen uitvoer in stand houden nadat de tussenstand die het maakte is
  ingetrokken (getuige), en zijn initialisaties zouden later meeveranderen, terwijl ch. 5.5 en ref ze bij het maken
  uitrekenen. Een `AgendaFilter` is niet gebruikt: Drools annuleert een activering die het filter afwijst, zodat een
  creatie die tijdens de regellaag wacht nooit meer zou vuren.
- **Publieke vorm.** De publieke signaturen van `DroolsBackend`, `Build`, `Branch` en `CaseRunner` zijn ongewijzigd.
  Nieuw publiek, alleen in het pakket van de tak: `Evaluatiefouten` (met `kop()` en `zinnen()`), `Lijstplaats`,
  `Creatiefase` (nummer, stratum), `TmsTranspiler.FEITCREATIE`, `OBJECTCREATIE`, `creatieregels()`, `creatiestrata()`,
  `lidstratum` en `leestLijstplaatsen()`. De driver (`DroolsBackend.vuur`) en de naad `DroolsBackendMain.voer` zijn
  package-private. `zetAfgeleideWaarden` weigert een sessie van een model met creatieregels die niet via de driver is
  gevuurd. Het contract voor afnemers (driveraanroep,
  `Lijstplaats`, fouten bij het terugzetten) is voor `20261008-tms-omschakeling`.

**Verbreding van "May touch".** `cases/DroolsBackendMain.java` (de ERROR-regel, lens op stap 1). Bestaande getuigen
buiten de nieuwe klassen, elk omdat deze kaart hun verwachting voor de tak verandert: `NuldelerAfnemerWitnessTest`
vraagt alleen nog de twee takken waarvan de fire een fout werpt (hun beweringen ongewijzigd; de tak met truth
maintenance dekt `TmsFoutWitnessTest`); in `TmsInitialisatieWitnessTest` de nieuwe weigerzin, in
`TmsSkeletWitnessTest` een ander voorbeeld van een geweigerde vorm, in `TmsKringWitnessTest` en
`TmsConsistentieWitnessTest` de nieuwe kringtoets, in `TmsAggregatieWitnessTest` lijstplaatsen bij handgebouwde bonen.

### Acceptatie

**(a)** Getuigen onder `backends/drools/src/test/java/dev/uchar/regelspraak/cases/`, elk rood aangetoond vóór de
wijziging (de rode uitvoer staat in de javadoc), in elke documentvolgorde en in gegenereerde volgordes van de regels
(tot 120 per document), en met mutanten die rood worden:
- `TmsFoutWitnessTest` (10) en `TmsVoorwaardefoutWitnessTest` (5): een tijdelijke deling door nul die aan het fixpunt
  verdwijnt, een blijvende, twee in vaste volgorde, de ERROR-regel van de CLI, fouten in voorwaarden van elke vormsoort;
- `TmsAggregatieWitnessTest` (10): een vouw over afgeleide leden die later veranderen, de opnieuw vurende activering,
  een vouw in de rechtvaardiging en de consequentie van een default, een deling door een telling die onderweg 0 is
  (verdwijnt) en een die 0 blijft (staat), een gelijste vouw, een meerstapsbereik met één lid via twee routes;
- `TmsFilterfoutWitnessTest` (8): geen fout waar de lezende regel niet geldt, het eerste falende lid in de vouwvolgorde
  (geschreven lijst, gespiegelde lijst, pad);
- `TmsCreatieWitnessTest` (12): een idempotente creatie met de waarde van het fixpunt, een creatie die alleen op een
  tussenstand geldt en niet gebeurt, een creatie die haar eigen uitvoer leest (de vorm uit het advies) en zichzelf niet
  in stand houdt, de fasering na een creatiefase (rood onder de mutant `AUTO_FOCUS`), een recursieve keten, een
  feitcreatie die elke link één keer legt, een tweede vulling van een naar-één-rol als fout, de generatiegrens voor
  objecten en links, het budget, de drie wrakboekingen;
- `TmsCreatiestratumWitnessTest` (9): een creatie die de uitvoer van een creatie uit een lager stratum leest, in een
  initialisatie (het scenario van de eindlens: penningaantal 1, niet 0), in een voorwaarde met een groeiende operand,
  en via een feitcreatie ertussen; twee creaties in één stratum; het budget per stratum; een pas waarin alleen de
  regellaag verandert telt mee (het scenario van de gerichte lens), met drie controles die ref volgen;
- `TmsKringVerifierWitnessTest` (3) en `TmsKringWitnessTest` (6): een kring die WF-045 toelaat, een negatieve rand die
  geweigerd blijft, de 22 negatieve documenten van WF-045/046/051 geweigerd, geen van de 258 goldens die de lader laadt.

Ref en clingo draaiden elk nieuw document; waar ze verschillen, staat het bij de waarnemingen.

**(b)** De corpusgetuige heeft geen FAIL; `EXPECTED_MIN_PLAYED` gaat van 66 naar 120 (`40b75291`). Eigen doorloop met
een jar uit een bevroren kopie van `1f2ad05c`: 120 gespeeld, 172 geweigerd, geen FAIL of ERROR. Van de 74 goldens die
in de nulmeting eerst op `Aggregation` (65), `ObjectCreation` (8) of `FactCreation` (1) stopten:

| Eindstand | Aantal | Goldens |
|---|---:|---|
| speelt | 47 | 38 van de aggregaties, alle 8 objectcreaties, de feitcreatie |
| weigert op een vorm van `tms-verdeling-en-tijdlijnen` | 12 | zeven `total` over de eigen tijdlijn, drie over een tijdsafhankelijk attribuut, `bergingstoezicht` (verdeling), `seinjaar` (`wholePeriod`) |
| weigert op een vorm van `tms-expressievormen-en-rekendatum` | 11 | `kustregister`, `postwacht` (uniciteit), `drempelvangst` (`Let`), `afrekening` (`percentageOf`), `dubbelverbinding`, `opvolgerversies` (begrensde geldigheid), `opvolgerquorum`, `wrakmelding`, `zwaarste-vondst` (kwantorvouw), `premiepot` (rolcheck), `vroegste-vondst` (builtin) |
| weigert op de §13.3.9-grens van het beanmodel | 4 | `bootberging`, `muntketen`, `gemeenteberging`, `penninguitgifte`: de exec-modeltak weigert ze ook (gate 12, EXCLUDE); dat hoort bij `20260918-drools-veelopveel-beanmodel` |

Daarnaast spelen zeven goldens die in de nulmeting eerst op een `BuiltinCall` stopten (vijf tellingen over een unie,
twee gelijste vouwen), en `anchor/jutterswet-erflijn`, die de exec-modeltak op §13.3.9 weigert. Eerste weigering per
familie, nulmeting → eind: `Aggregation` 65 → 11 (tien tijdlijntotalen en `penninguitgifte`), `ObjectCreation` 8 → 0,
`FactCreation` 1 → 0, loader 37 → 37.

**(c)** `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over `transpiler/tms/` geeft niets.

**(d)** De exec-modeltak en de DRL-teksttak veranderen alleen door punt 4. Eigen meting: hun statusregels over alle 292
goldens zijn gelijk vóór en na het beslisblok (jar van `42859692` tegen `21bc5085`: 0 verschillen per tak) en vóór en
na de creatie (`21bc5085` tegen `2907fa91`: 0). Alle 16 gates van `CLAUDE.md` groen op `1f2ad05c`; gate 12
met played 240 (`EXPECTED_PLAYED`), DRL 194 (`EXPECTED_DRL_PLAYED`), value-divergence 0, JUnit 1231. Ook op `6b4c966f`
en `fdaa52f4` draaide de volledige gateset groen.

### Meting voor `20261008-fout-op-tussenstand`

| Document | Volgorde | Vóór deze kaart | Na | Na, met de weigering uitgezet (niet gecommit) |
|---|---|---|---|---|
| A | voorwaarts | EXCLUDE (deling in een initialisatie naast een andere schrijver) | EXCLUDE | PLAYED, bergloon 30 |
| A | omgekeerd | EXCLUDE | EXCLUDE | PLAYED, bergloon 30 |
| B | voorwaarts | PLAYED | PLAYED | PLAYED |
| B | omgekeerd | ERROR `DelingDoorNul` "aandeel": 90 gedeeld door 0 | PLAYED, aandeel 3 | PLAYED |

Met fouten als feit antwoordt de tak in beide volgordes wat het voorgestelde normbesluit vraagt.

### Waarnemingen

1. **Ref en clingo verschillen** op een initialisatie van een gecreëerd object die een waarde leest die na het maken nog
   groeit: ref 0 (uitgerekend bij het maken), clingo 1 (de eindwaarde in zijn stabiele model). Ch. 6.1 "a value that
   grows" noemt initialisaties niet. De tak volgt ref. Voorstel: een kaart.
2. **De exec-modeltak en de DRL-teksttak** geven een fout uit het filter van een vouw waar de lezende regel niet geldt;
   ref en clingo slagen. Voorstel: een kaart (golden); de fix vervalt voor de exec-modeltak bij de omschakeling en voor
   de DRL-teksttak bij `20261008-drl-teksttak-op-truth-maintenance`.
3. **De Drools-runtime** rekent een datumstap met `aantal.longValue()` (dat boven het bereik van `long` stil omslaat) en
   met het bereik van java.time, terwijl ch. 3.8 "± 2 000 000 000 000 uur" en "+ 60 000 000 000 dagen" een fout noemt;
   alle drie de takken geven een datum. Voorstel: een kaart. Gezien, niet gemeten: een duur uit een niet-afbrekende
   deling zou in de datumrekening een `ClassCastException` geven.
4. **Een initialisatie waarvan de voorwaarde door nul deelt** terwijl het doel in de invoer gevuld is: ref, clingo en de
   exec-modeltak falen, de DRL-teksttak en deze tak spelen. Voorstel: document C bij `20261008-fout-op-tussenstand`.
5. **De exec-modeltak en de DRL-teksttak melden bij twee fouten** de eerste die de agenda bereikt; welke dat is, hangt
   van de regelvolgorde af. Dat vervalt met de omschakeling en de DRL-kaart.
6. **Restpost "conditions that hold on absence"** (ch. 6.1): een creatie die een telling tegen 0 toetst over de uitvoer
   van een andere creatie, beantwoordt de tak in elke volgorde zoals ref met de lezer eerst; clingo geeft het
   gestratificeerde antwoord. Bij een positieve lezing komt de tak op hetzelfde fixpunt, een generatie later. Een keten
   van n feitcreaties kost n generaties, waar ref haar in documentvolgorde in één pas maakt; dat raakt alleen het
   budget.
7. **`Kringen` weigert** een kring door het foutkanaal van een rekenende regel die de verifier toelaat; luid, geen golden.
8. **Clingo vertaalt een gelijste `minimum` niet** ("expression kind not translated"); luid, geen golden.
9. **Clingo telt het creatiebudget anders dan ref**, op twee manieren: een niet-recursieve creatie erft de diepte van
   haar onderwerp, zodat de diepte over strata optelt (50 + 1 + 50 generaties in drie strata: ref speelt, clingo UNSAT
   "generation budget exhausted"), en clingo begrenst de diepte bij de honderdste aanmaak, waar ref pas faalt als de
   101e pas nog iets verandert (een keten tot 101 zonder berekende eigenschap: ref speelt, clingo UNSAT). De tak volgt
   ref. Voorstel: een kaart.
10. **Op de budgetgrens hangt ref van de documentvolgorde af** (gelezen, niet gemeten): een regel die een latere regel
   leest, komt in ref een pas later tot rust, terwijl de regellaag van de tak in één keer tot rust komt. Een lid met
   schrijvers in twee strata telt de tak op het laagste stratum. Er is geen document gemaakt dat dit laat zien;
   `synth2-budgetgrens-onder` ligt op de grens en speelt in de tak zoals in ref en clingo.
11. **Schaal** (advies, vraag 3): de joins zijn niet geïndexeerd en elke constraint heeft een eigen id (geen sharing van
   knopen), `min`/`max` rekenen bij een intrekking alles opnieuw, het vuurplafond van 100 000 is met ongeveer 5 000
   objecten × 20 leden al bereikt, en na het terugzetten zou een tweede fire afgeleide waarden als invoer lezen. Besluit
   van de eigenaar: kaart 4 krijgt een schaalproef met vloer.

### Advies

Op aanbod van de eigenaar las een adviseur (Fable 5.1, alleen lezend, op `42859692`) drie vragen. (1) Ongefundeerde
steun bij logische creatie: ja, met een kleinste vorm; dat werd het besluit over creatie. (2) Twee weergaven: nu geen
volledig rendererneutraal plan, wel in kaart 2–3, waar die methoden toch geraakt worden, een rendererneutrale
leestabel naast `Lezingen`, een kleine gesloten woordenschat van structurele constraints met één mapper naar PatternDSL
(gelijke constraints krijgen een gelijk id), en de IR-knoop naast elke gecompileerde toets. (3) Schaal: zie waarneming 11.

### Lensvondsten

Copilot draaide een inventaris (sol6) en na elke bouwstap een lens op sol6 met effort xhigh (zes runs, inclusief de
gerichte herkeuring van de eindlensfix, elk één premium request), en een eindlens op Opus 5.5 (15 premium requests),
zoals de werkopdracht vraagt.

Aanvaard en gefixt:
- stap 1: de CLI kapte de ERROR-regel na 200 tekens, zodat bij twee fouten de tweede regelnaam wegviel (`4345fdc0`);
- stap 2: een filterfout ontstond ook waar de lezende regel niet geldt (`a987275c`);
- fixes en punt 4: bij meer falende leden de alfabetisch kleinste zin in plaats van het eerste lid in de vouwvolgorde
  (`582b200e`);
- stap 3: getuige G bewees de fasering van objectcreaties niet; de mutant `AUTO_FOCUS` overleefde (`739ca46a`);
- eindlens: alle objectcreaties van een generatie vuurden tegen dezelfde stand, terwijl ref creaties in strata
  ordent; een creatie die "het aantal penningen" van een andere creatie als initialisatie leest, gaf stil 0 waar ref
  1 geeft (`fdaa52f4`);
- gerichte lens op die fix: het budget telde alleen generaties die iets maken, ref elke pas met een wijziging; op de
  grens speelde de tak waar ref faalt (`1f2ad05c`);
- eindlens, advies: `20260923-drools-rolpaar-ontdubbeling` sluit mee (Result in deze commit; naar `done/` bij de
  landing).

Afgewezen, met reden:
- stap 3, "een objectcreatie vuurt binnen haar fase op een telling die nog 0 is" en "de fasestempel van een feitcreatie
  wijkt af van een pas van ref": beide worden pas zichtbaar via een telling die tegen 0 wordt getoetst, de vastgelegde
  restpost "conditions that hold on absence" van ch. 6.1, waar ref zelf van de volgorde afhangt; geen golden raakt het
  (waarneming 6);
- stap 3, "`Kringen` weigert een kring via het foutkanaal van een rekenende regel": bewuste kanaalknoop, luid, geen golden
  (waarneming 7).

De adviseur beschreef ref als één pas over regels, verdelingen, defaults en creaties; dat ref die passen per stratum
draait, vond pas de eindlens.

### Werkwijze

Vijf bouwers (opus55). Na een opmerking van de eigenaar over de omvang van de context kreeg elke stap een verse bouwer;
creatie vroeg er twee, en de fixes van de eindlens een vijfde. De hoofdsessie draaide na elke stap zelf een doorloop
van het hele corpus met een jar uit een bevroren kopie, mat bij het beslisblok en de creatie de byte-gelijkheid van de
twee andere takken, en draaide de volledige gateset op `6b4c966f`, `fdaa52f4` en `1f2ad05c`.

### Open punten (voor de landing)

1. Op main, voorstel van de hoofdsessie:
   - kaarten voor de waarnemingen 1, 2, 3 en 9;
   - document C bij `fout-op-tussenstand` (waarneming 4);
   - in `20261008-tms-omschakeling`: de schaalproef met vloer (besluit van de eigenaar), een vuurplafond dat met de
     invoer meegroeit, herhaald vuren na het terugzetten, het contract voor afnemers (de driver met strata en
     generaties, `Lijstplaats`, de fouten bij het terugzetten), en dat `zetAfgeleideWaarden` gecreëerde links nog niet
     op de referentievelden van de bonen zet;
   - in `20261008-tms-expressievormen-en-rekendatum` de aanbeveling van het advies over de leeslaag.
2. Voorstel voor PLAN §9, bij het besluit van 2026-10-08: "Creation in the truth-maintenance lowering is stated, not
   logical: a creation fires only at a fixpoint of the rule layer, per stratum and in generations as in the reference
   interpreter (owner decision 2026-10-08, card `tms-aggregaties-creatie-en-fouten`); the switchover card carries a
   scale test with a floor (owner decision, same day)."
3. Geen registertekst voor `spec/CONSTRUCTS.md`: deze kaart verandert de spec niet.
