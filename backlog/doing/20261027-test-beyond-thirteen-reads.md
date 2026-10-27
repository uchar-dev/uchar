# test-beyond-thirteen-reads: een voorwaarde mag meer dan 13 waarden lezen

*Op verzoek van de eigenaar (2026-10-27), na de vondst bij `readings-of-displaced-defaults`: de v2-groep
'duurverlenging bij-een-nieuw recht' leest precies 13 waarden, de grens van de exec-model-tak.*

## Aanleiding

De Drools-backend weigert een vorm waarvan de voorwaarde meer dan 13 gebonden waarden leest:
`TmsTranspiler.testItem` gooit DROOLS-264 (`TEST_ITEM_MANY_READS`, "de toets van deze tak neemt hoogstens 13 gebonden
waarden") boven `MAX_READS = 13` (`TmsTranspiler` r. 227). De grens komt uit de API die de exec-model-tak gebruikt:
`PatternDslRendering.test` (r. ±646–692) geeft elke gelezen waarde als eigen argument aan `DSL.expr(id, v1, …, vn,
PredicateN)`, en `drools-canonical-model` 8.44 kent functietypen tot en met `Predicate13`. Het is geen grens van de
semantiek (ref en clingo kennen haar niet) en niet van Drools als motor: DRL-tekst roept een Java-functie aan met elk
aantal argumenten. Omdat de analyse beide takken één plan geeft, weigert de DRL-teksttak dezelfde vormen.

Gemeten op de v2-regelset herleving-WW (werkbank, regels `ece012e`): de voorwaarde van
'defaults A:RechtOpUitkeringWwBijOntslag.duurverlenging bij-een-nieuw recht' leest er 13. Eén lezing erbij en uchar
weigert de groep; de werkbank kan dat nu alleen omzeilen door de voorwaarde in de regelset te splitsen (een
hulpkenmerk).

## Doel

De exec-model-tak geeft boven 13 de gelezen waarden aan de toets door in bundels (bijvoorbeeld per hoogstens 13 een
tussenbinding die een array maakt, en de toets over die bundels), zodat een voorwaarde elk aantal waarden mag lezen. De
DRL-teksttak volgt hetzelfde plan. De semantiek verandert niet; een toets met hoogstens 13 waarden wordt weergegeven
zoals nu. DROOLS-264 vervalt (de code blijft in het register met de soort `RETIRED`) of blijft alleen voor een grens die
de bouw dan nog heeft; de bouwer stelt dat vast.

## Acceptatie

- (a) Rood eerst: een golden (of een getuige in de suite) met een vorm waarvan de voorwaarde meer dan 13 waarden leest,
  een paar over een rol, met cases waarin zij geldt en niet geldt; ref en clingo spelen hem al, Drools weigert hem met
  DROOLS-264 op de code van nu.
- (b) Beide Drools-takken spelen hem met de gepinde verwachtingen; gate 12 `EXPECTED_PLAYED` gaat één omhoog; de build
  met lezingen leest hem ook (gate 17), de objecten van kaart `reading-read-objects` inbegrepen.
- (c) Een toets met hoogstens 13 waarden blijft gelijk: de productiefeiten en vuringen ongewijzigd
  (`ReadingRulesFixpointWitnessTest`, `ReadingRulesObserverWitnessTest`, `BranchDumpsCorpusWitnessTest`), en de DRL-tekst
  van de bestaande goldens bytegelijk.
- (d) De v2-regelset bouwt nog, met en zonder lezingen; `make gates` groen.

## May touch

`backends/drools/**` buiten de productieruntime (`PatternDslRendering`, `TmsTranspiler`, `DroolsCode` en de
catalogi), een golden, de tests van `backends/drools`; `scripts/check-drools.sh` (`EXPECTED_PLAYED`, de lijsten) als
voorstel; deze kaart.

## Out of scope

Een grens elders in de backend (het aantal patronen, een accumulate); de werkbank en de regelset.

## Result (2026-10-27)

Tak `card/test-beyond-thirteen-reads`, op `4e158666`. Geraakt: `backends/drools/**` buiten de productieruntime
(`PatternDslRendering`, `TmsTranspiler`, `DroolsCode`, `ReadingsWitnessTest`), een nieuwe golden
`corpus/golden/jutterswet-strandvoogdkeuring`, `scripts/check-drools.sh` (voorstel: beschermd pad, de tak is het
voorstel), deze kaart. Niet geraakt: `CLAUDE.md`, `spec/`, de catalogi (de regels van DROOLS-264 blijven staan, zoals
die van de andere `RETIRED`-codes), de frontend, ref, clingo.

- `686af0b4` de golden `jutterswet-strandvoogdkeuring` (bron, IR uit de frontend, twee cases, NOTE).
- `a0395a68` Drools: de bundels in `PatternDslRendering.test`, de weigering weg uit de analyse, DROOLS-264 `RETIRED`,
  de getuige; gate 12 `EXPECTED_PLAYED` 279 → 280 (voorstel).
- dit Result.

### Wat er veranderde

**De grens zat in de weergave, niet in het plan.** De analyse gaf al één `TestItem` over de slots die de voorwaarde
leest; alleen de weergave in het uitvoerbare model kon er niet meer dan dertien aan één toets geven
(`DSL.expr(id, v1, …, v13, Predicate13)`, `ViewPatternBuilder.createConstraint` kent `Expr1ViewItemImpl` tot en met
`Expr13ViewItemImpl`). De analyse weigerde daarom boven dertien, voor beide takken. Nu:

- `TmsTranspiler`: `testItem` weigert niet meer; `MAX_READS` is weg. De parameter `conditionTerm` van `plan` en
  `testItem` diende alleen de weigering (het fragment in de melding) en is met de 17 aanroepen weg, `positions` ook
  (ongebruikt). De javadoc van `withObject` noemt `MAX_READS` niet meer.
- `PatternDslRendering.test`: tot en met dertien waarden precies de weergave van vóór de kaart (dezelfde `switch`, nu
  `expr(id, v, t)`). Boven dertien gaan de waarden in **bundels** naar de toets (vorm hieronder); `rendering(item)` geeft
  een lijst, zodat een toets zijn bundelpatronen vóór zich meeneemt.
- De DRL-teksttak rendert hetzelfde plan zoals altijd: zijn toetsfunctie krijgt een parameter per waarde
  (`eval( toets_strandvoogdkeuring($ervaren, $diensttijd, …, $strandpostBemand) )`, 18 argumenten). DrlRendering is
  ongewijzigd.
- `DroolsCode.TEST_ITEM_MANY_READS` (DROOLS-264): `Kind.RETIRED`, het nummer blijft, geen plaats noemt de code nog.

**De vorm van de bundels.** Boven dertien waarden: per hoogstens vier opeenvolgende waarden (in de volgorde van de
slots van de toets) een patroon `from` vlak vóór de toets, dat een `Bundle` bindt (een record om een `Object[]`); zijn
de bundels zelf meer dan dertien, dan worden de bundels op dezelfde manier gebundeld, niveau na niveau. De toets neemt
de bundels van het bovenste niveau als argumenten en opent ze in dezelfde volgorde (`opened`, met een luide fout als
het aantal niet klopt). Aantallen: 14 waarden → 4 bundels; 18 (de golden) → 5; 52 → 13; 60 → 15 bundels en daarboven 4
(twee niveaus, in de getuige). Een toets van hoogstens dertien waarden heeft geen bundel. De variabelen heten
`$bundle_<toets-id>_<niveau>_<n>`; een tweede binding van die naam faalt luid.

### Lot van DROOLS-264

`RETIRED`. De exec-model-tak heeft geen grens meer aan het aantal waarden van een toets (de bundels worden gebundeld
zolang het er meer dan dertien zijn). De bouw heeft wel nog een grens, maar niet die van DROOLS-264: in de DRL-teksttak
krijgt elke functie die een regel aanroept een Java-parameter per waarde, en een Java-methode neemt er hoogstens 255.
Gemeten (`werk/test-beyond-thirteen-reads/probe300`, een `alle`-samenstelling van n gelijkheden op eigen attributen,
met `DroolsBackendMain` en `V2BuildProbe`): n = 250 en 255 spelen op beide takken (`PLAYED 1`), 254 bouwt op beide; bij
n = 256 en 300 speelt het exec-model (`PLAYED 1`, ook met lezingen gebouwd) en faalt de bouw van de DRL-tekst luid,
zonder en met lezingen: `ERROR IllegalStateException: drl: de DRL-tekst van de weergave compileert niet, een fout van
de weergave: … text=Unable to Analyse Expression toets_wijdte($waarde1, …`. Zie V4 voor waarom DROOLS-264 daar niet
voor blijft.

### Getuigen, met tellingen

- **Golden `jutterswet-strandvoogdkeuring`** (`source.rgs`, IR uit de frontend; gates 2, 10 en 15 lezen het): "Een
  Jutter is strandvoogdwaardig indien hij aan alle volgende voorwaarden voldoet" met achttien voorwaarden, die achttien
  waarden lezen: een eigen invoerkenmerk, twaalf eigen invoerattributen, een afgeleid attribuut (`rang`, uit "zijn
  diensttijd plus 1", een `State`), drie attributen en een kenmerk over de rol `strandpost`. De drempels stijgen in
  documentvolgorde en een Jutter die alles haalt heeft telkens de waarde één boven de drempel, zodat een waarde op de
  plaats van een andere faalt. Case 1: J1 haalt alles, J2 mist knoopkennis; case 2: J3 zonder strandpost, J4 op een
  onbemande post, J5 op een post met kustlengte 20, J6 niet ervaren; elke case pint de rangen (no-op faalt erop, gate
  7). ref en clingo: PASS (4 en 8 verwachtingen); verifier schoon.
  Drools nu: `PLAYED 2` op beide takken; `--readings` op beide takken `READINGS 2`, de twee uitvoeren bytegelijk
  (339 regels), en gelijk aan `runner --trace` van ref per case (los vergeleken, en in gate 17), met `on "S1"`, `on "S2"`
  of `on "S3"` bij de lezingen over de rol en `empty on none` bij J3 (geen post), waar de samenstelling stopt.
- **`ReadingsWitnessTest.conditions_that_read_more_values_than_one_test_takes`**, beide takken: voor 13, 14 en 60
  waarden (telkens drie over de rol `post`) een `alle`-samenstelling van gelijkheden, elk met een eigen waarde
  (`waarde i = i`), zodat een verwisselde waarde haar lid laat falen. De productiebuild leidt alleen W1 af; de build met
  lezingen geeft per Wachter het record met elke waarde en het object (`@W1`, `@P1`): W1 `HOLDS`, W2 zonder post stopt
  bij de eerste lezing over het pad (`leeg`, zonder object), W3 stopt bij zijn laatste eigen lid (één te hoog), de
  lezingen over het pad niet bereikt. 3 × 3 records per tak, 13 + 14 + 60 = 87 leden.
  De bestaande `a_condition_that_reads_as_many_values_as_a_test_takes` (13) blijft; alleen haar javadoc noemt
  `MAX_READS` niet meer.
- Suite: 1712 → 1713 tests.

### Rood eerst

Kopie op `4e158666` (`git worktree add --detach werk/test-beyond-thirteen-reads/red 4e158666`, daarin de golden uit
`686af0b4` en de getuigeklasse van de tak), jar daar gebouwd (`logs/red-1.log`):

- `DroolsBackendMain` op de golden, zonder vlag, `--drl`, `--readings` en `--readings --drl`, telkens dezelfde regel:
  `EXCLUDE DROOLS-264: transpiler SKIP: regel: strandvoogdkeuring — a condition that reads 18 values ('hij aan alle
  volgende voorwaarden voldoet: - hij is ervaren - … - zijn strandpost is bemand'): the test of this branch takes at
  most 13 bound values`.
- De getuige (`logs/red-witness-1.log`): 13 waarden groen, dan `wideCondition:877 » Excluded transpiler SKIP: regel:
  inzet — een voorwaarde die 14 waarden leest ('all 0'): de toets van deze tak neemt hoogstens 13 gebonden waarden`;
  alleen 60 (`logs/red-witness-60.log`): "een voorwaarde die 60 waarden leest".

### Mutaties (in een kopie op `a0395a68`, `werk/test-beyond-thirteen-reads/mut`, niet gecommit)

- M1, `from` met vier waarden bundelt `{a, b, d, c}` (de laatste twee verwisseld; `logs/mutant-M1b.log`): de golden op
  het exec-model `FAIL case1-benoembaar.json: kenmerk strandvoogdwaardig: verwacht aanwezig, werkelijk afwezig` (een kleiner
  getal bij een hogere drempel), `--drl` `PLAYED 2`; de getuige rood: `EXEC_MODEL, 14 waarden: de productiebuild ==>
  expected: <[W1 inzetbaar]> but was: <[]>`. De variant `{b, a, c, d}` (`logs/mutant-M1.log`) geeft op de golden een
  `ERROR … ClassCastException: BigDecimal cannot be cast to Boolean` (een getal op de plaats van een kenmerk) en
  dezelfde rode getuige.
- M2, `opened` opent hoogstens één niveau (`logs/mutant-M2.log`): de golden speelt (één niveau); de getuige rood bij
  60: `IllegalStateException: de bundels van een toets houden 15 waarden, de toets leest er 60`.
- De DRL-teksttak veranderde alleen doordat de analyse niet meer weigert; de mutatie daarvan is de rode stand.

Beide kopieën opgeruimd met `git worktree remove`.

### De DRL-tekst van de bestaande goldens

`werk/test-beyond-thirteen-reads/drldump` (`DrlDump.java` van de vorige kaart, de productiebuild zonder lezingen van
elke golden): jar van `4e158666` (gebouwd in de rode kopie) tegen die van `a0395a68`. `diff -r`: alleen
`jutterswet-strandvoogdkeuring.drl` verschilt (voor: de weigering, na: de tekst); de 282 andere gebouwde goldens
bytegelijk, de 47 geweigerde met dezelfde weigering (voor: 282 gebouwd, 48 geweigerd; na: 283 en 47).

### v2

IR uit de frontend van de worktree (de 13 fundament- en 20 herleving-ww-bestanden in resolve-volgorde, `--ir`),
gebouwd met `V2BuildProbe` (`werk/test-beyond-thirteen-reads/v2.sh`):

| jar | EXEC_MODEL productie | EXEC_MODEL met lezingen | DRL_TEXT productie | DRL_TEXT met lezingen |
|---|---|---|---|---|
| `4e158666` | 339 regels | 493 | 339 | 493 |
| `a0395a68` | 339 | 493 | 339 | 493 |

### Voorlopige besluiten van de bouwer

- **V1 Bundels via `from`, vier per bundel.** Een tussenbinding van hoogstens dertien die een array maakt (de kaart)
  bestaat niet in de DSL van 8.44: `PatternDef.bind` neemt het patroonobject en hoogstens drie variabelen, `DSL.from`
  hoogstens vier. Alternatief was een toets met elk aantal declaraties onder de DSL door: de motor kan dat
  (`ConstraintEvaluator$InnerEvaluator$_N`), maar dan bouwt de backend de patroonboom van de regel zelf
  (`CompositePatterns`, `EvalImpl`, een eigen `SingleConstraint`), dieper in Drools dan `VariadicConsequence`, en zonder
  bundels, die de kaart vraagt. Met `from` blijft alles publieke API. Prijs: per bundel een `FromNode` per match;
  alleen regels boven dertien betalen hem.
- **V2 De bundels in de weergave, niet in het plan.** Het plan had geen grens; alleen de weergave in het uitvoerbare
  model wel, zoals de `switch` over de ariteit daar al stond. "De DRL-teksttak volgt hetzelfde plan" is zo gelezen: beide
  takken renderen het ene `TestItem`; de DRL-tekst heeft de grens van dertien niet en roept zijn functie met alle
  waarden aan. Bundels in het plan hadden een nieuw `LhsItem` gevraagd en in de DRL-tekst een `from` met een
  bundelfunctie die daar niets oplost.
- **V3 Boven dertien gaat alles in bundels**, in opeenvolgende groepen van vier, niveau na niveau; geen menging van
  losse waarden en bundels (bij veertien waarden had "twaalf los en één bundel van twee" één `from` gekost in plaats van
  vier). Eén vorm voor elk aantal, één regel om te openen; het verschil kost alleen regels boven dertien.
- **V4 DROOLS-264 `RETIRED`, de grens van 255 van de DRL-tekst niet gecodeerd.** Die grens is een andere: zij geldt voor
  elke functie die een DRL-regel aanroept (`DrlRendering.context` geeft elke functie een parameter per slot: ook de
  functie van het gevolg dat een fout van de voorwaarde opnieuw evalueert en die van de lezing), bestond dus vóór deze
  kaart al voor de gevolgen (uit de code, niet gemeten), en heeft niets met de toets van het exec-model te maken. Een weigering ervoor in de analyse zou het exec-model laten weigeren wat het speelt en de
  parameterindeling van de DRL-functies (de sonde, een global per dagsoort) in de analyse herhalen; een weigering in de
  DRL-weergave alleen is een tak-verschil dat gate 12 als fout behandelt. Boven 255 faalt de DRL-tekstbouw luid, met
  de expressie in de melding. v2 leest er hoogstens dertien. Een eigen code voor die grens is een kaart waard als een
  regelset er in de buurt komt.
- **V5 De ongebruikte parameter weg.** `plan(…, Condition conditionTerm)` en `testItem` gaven de voorwaarde alleen door
  voor het fragment van de weigering; met de weigering weg is zij dood en zijn de 17 aanroepen één argument korter;
  `positions(Set)` had geen aanroeper meer.
- **V6 De golden en de getuige zijn zo gemaakt dat een verwisselde waarde faalt** (stijgende drempels met de waarde één
  erboven in de golden, gelijkheden met eigen waarden in de getuige), en de getuige neemt 60 om het tweede niveau te
  raken.
- **V7 De golden als eigen commit vóór de code** (`686af0b4`): op die commit alleen zou gate 12 rood zijn (de golden
  speelt niet en staat niet in `EXPECTED_CHANNEL`); de tak landt als geheel.

### Voor de hoofdsessie

**`CLAUDE.md`, gate 12**, als eigen zin na "…and replays every `case*.json`.":

```
    Since the card `test-beyond-thirteen-reads` (2026-10-27) a condition
    may read any number of values: the exec-model branch passes more
    than thirteen to its test in bundles (`PatternDslRendering.test`),
    the DRL-text branch calls its test function with all of them, and
    DROOLS-264 is retired.
```

Gates 8 en 17: geen tekstwijziging (geen nieuwe vloer; de tellingen groeien). Geen CONSTRUCTS-rij (geen nieuwe vorm).
PLAN §9, voorgesteld: "test-beyond-thirteen-reads (2026-10-27): een voorwaarde mag elk aantal waarden lezen; het
exec-model geeft er boven dertien in bundels van vier (`from`) aan de toets, de DRL-tekst alle; DROOLS-264 RETIRED;
golden jutterswet-strandvoogdkeuring (18 waarden), gate 12 EXPECTED_PLAYED 280; open: de grens van 255 parameters van
een DRL-functie (V4); voorlopige besluiten V1–V7." En in de alinea van PLAN r. ±1365 die DROOLS-264 noemt kan de
hoofdsessie de stand bijwerken.

**Wat de toolchain moet weten.**
- De nieuwe golden `jutterswet-strandvoogdkeuring` speelt nu door haar dienst-replay (beide takken, `PLAYED 2`); een
  telling of pin van gespeelde goldens daar gaat één omhoog.
- DROOLS-264 komt niet meer voor; een plaats in de toolchain die hem noemt of verwacht vervalt.
- De v2-groep 'duurverlenging bij-een-nieuw recht' (13 waarden) bouwt zoals voorheen; de werkbank hoeft een voorwaarde
  boven dertien niet meer te splitsen. Een regel boven dertien heeft in de KieBase van het exec-model `from`-patronen
  vóór zijn toets; het aantal regels verandert niet.

### Kosten

Regels (`git diff --shortstat 4e158666..a0395a68`): Drools main +129/−61 (3 bestanden), Drools tests +90/−4, de golden
+1427 (waarvan 864 het IR-document), gate 12 +6/−1. In de KieBase: per toets boven dertien `ceil(n/4)` patronen
`from` (en daarboven per niveau weer een kwart), alleen in het exec-model; geen extra regel, de DRL-tekst ongewijzigd.

### Gates

Zelf gedraaid, elk alleen, op de eindstand `a0395a68` (logs in `werk/test-beyond-thirteen-reads/logs/`). De machine
stond onder zware last van ander werk (load 125–135), dus de tijden zijn hoog:
- gates 2, 3, 4, 5, 6, 7, 10 en 15 (`gates-quick-1.log`, per gate `gate<n>-final.log`): alle PASS — 2: 374 bestanden
  valideren en round-trippen (87 s); 3: 374 goldens schoon, 538 negatieve geweigerd (38 s); 4: 958 casebestanden
  (18 s); 5: ref-suite 1434 worked examples, 901 cases exact (24 s); 6: 901 cases ref = clingo, backend-suite 2235
  checks (244 s); 7: 856 cases weerleggen de no-op, 45 vastgepinde overlevers (14 s); 10: JUnit 1302 tests, 141
  rgs-goldens bytegelijk (143 s); 15: PASS, route leg over 141 goldens met `source.rgs`, 73 unit tests (33 s).
- gate 12 (`bash scripts/check-drools.sh`, `gate12-1.log`): PASS, played 280 op beide takken (gelijk aan het gepinde
  aantal), elke statusregel van de DRL-teksttak gelijk aan die van het exec-model (sweep 235 s), value-divergence 0,
  50 niet-gespeelde goldens bij naam, JUnit 1713 tests (vloer 1573), 0 skips.
- gate 13 (`bash scripts/check-reftype-derivation.sh`, `gate13-1.log`): PASS, 44 eenheidsannotaties verdraaid over 25
  goldens, 130 replays reproduceren de onaangeraakte uitkomst, 0 overlevers; de vocabulaire-, herkomst- en naambenen
  groen (222 s).
- gate 17 (`opam exec -- bash scripts/check-readings.sh`, `gate17-1.log`): PASS, 901 golden-casebestanden getraceerd,
  per tak 280 goldens en 771 casebestanden vergeleken (was 279 en 769), 6346 lezingen (+12), 20841 knoopregels (3574
  `unreached`, 909 met bijdragers, 319 met een tijdlijn, 758 met een object (+12), 484 onder `DISPLACED`), afwijkingen
  0 (450 s, onder de last).
- de volledige Drools-suite ook los (`mvn -o test`, `mvn-suite-1.log`, vóór de laatste javadoc- en
  zichtbaarheidswijziging): 1713 tests, 0 failures, 0 errors, 0 skips; op de eindstand in gate 12.

De volledige set (`make gates`) niet; die draait de hoofdsessie vóór de landing.
