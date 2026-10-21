# fixpoint-readings: de motor legt de lezing van elke voorwaarde vast

*Vastgelegd door de hoofdsessie op 2026-10-21, kaart U2 uit het Fable-advies over de waarden in de uitleg
(`research/uitleg-lezingen-2026-10-21/fable-advies-2026-10-21.md` in `uchar-dev/research`, §3–§7). Besluit van de
eigenaar 2026-10-21: weg C (de motor levert de waarden) met ref als getuige (kaart `ref-trace-and-readings-gate`).
Volgt op `undecided-stack-record`; de toolchain-kaart `explanation-from-readings` volgt deze kaart met een pin-bump.*

## Aanleiding

De Verbalisator van de toolchain rekent voorwaarden zelf na en wijkt daarbij op zes plekken van de Drools-backend af
(leeg in `+ − ×`, vergelijking met een lege operand, eenheden, tekst- en enumeratiegelijkheid, de notatie van een
getalliteral, datums; advies §1.1). Wat hij niet kent, heet "niet vast te stellen": 80 keer in de vijf
service-smoke-verzoeken van de regelset herleving-WW v2. De TMS-lowering heeft alles wat een lezing nodig heeft (per
regel een `ReadTable` en een `TestItem` met een `Term`; aan het fixpunt één standingsfeit per onderwerp en lezing), maar
geen lezer.

## Doel

In een build met uitleg (een optie op `DroolsBackend.build`; de productiebuild blijft zonder) krijgt elke `RulePlan`
met een `TestItem` een zusterregel met dezelfde linkerkant zonder de toets, die logisch een `Reading`-feit met de
waardenrij inzet. uchar evalueert de `Term` over die rij opnemend (een derde weergave naast `ClosureRendering`, met
dezelfde functies, dezelfde stopregel en dezelfde volgorde van ch. 5.10 en 5.11), en geeft
`DroolsBackend.readingsOf(Build, KieSession)` naast `fixpointOf` en `decisionsOf`: per vorm (de pointer van
`defaults-decider`) en onderwerp de status (geldt, faalt, fout, verdrongen, buiten geldigheid) en per knoop (pre-orde in
de draadvolgorde) bereikt of niet, oordeel of waarde met eenheid, fout, en de leden van een kwantorvouw. Een lezing
deponeert niets en schrijft niets. Elke vorm met een voorwaarde krijgt een lezing (besluit: ook berekeningen,
consistenties en creaties). De technische keuzes van advies §5 gelden, tenzij de bouwer meet dat het anders moet.

## Aanvullingen van de hoofdsessie (2026-10-21, na vragen van de eigenaar)

- **Naam.** De zusterregel heet in de code een *reading rule*: een `RulePlan` met dezelfde `readings` en `leftSide`
  zonder het `TestItem`, met `Consequence.Reading` als gevolg. Nodig omdat het `TestItem` in de linkerkant staat: een
  regel waarvan de voorwaarde faalt, matcht niet en legt dus niets vast, terwijl de uitleg ook zegt waarom iets niet
  gold. Het alternatief, de toets van elke regel naar haar gevolg verplaatsen, verandert het antwoordpad in elke regel
  en laat elke regel voor elk onderwerp vuren; niet doen.
- **Twee builds (besluit van de eigenaar, 2026-10-21).** De productiebuild blijft zonder reading rules; alleen de
  uitlegbuild (de optie op `DroolsBackend.build`) heeft ze. Getuige (a) "met en zonder gelijk" blijft daarom nodig.
  Reading rules tellen niet mee voor `Observer`, `coverage` en de trace; de fire-cap van de uitlegbuild houdt er
  rekening mee. Meet en noteer in de Result wat ze kosten (regels, vuringen, feiten, tijd van een vuur) over het corpus
  en op de regelset herleving-WW v2.
- **De replay geeft zijn sessie door, alleen als dat de code niet ingewikkelder maakt (eigenaar, 2026-10-21).** uchars
  replay (`CaseRunner`) kan de sessie of het fixpunt met de lezingen doorgeven aan een afnemer, zodat de scenario-runner
  van de toolchain een case niet een tweede keer speelt (`CaseSession.play`, nu bewaakt door een vergelijking van de
  vuringen). Vraagt dat een tweede pad of een uitzondering in `CaseRunner`, dan niet doen en de reden in de Result
  zetten.

## Acceptatie

- (a) Eerst gemeten: met de optie blijven de productiefeiten aan het fixpunt gelijk aan die zonder (dumpvergelijking
  over het corpus, beide takken), vóór iets anders gebouwd wordt.
- (b) Rood eerst met een klein eigen model (Jutterswet-namen) per vormfamilie, op beide takken: een samenstelling
  "alle" die bij het tweede lid stopt (het derde niet bereikt), een "ten minste één"-blok, een telling met filter, een
  kwantorvouw over een rol met twee leden, een vergelijking met een lege operand, een product met een lege operand (0),
  een herschaalde vergelijking, een tijdsduur, een kenmerkcheck over een rol, een default-keten met een beslisser in
  het midden (erboven faalt, eronder verdrongen), een attribuutdefault die geen waarde gaf, een vorm buiten haar
  geldigheid, een initialisatie.
- (c) Over het corpus, op beide takken: de lezing geldt ⇔ de vorm vuurde; elke operand die een lid leest heeft de waarde
  die `fixpointOf` dat lid geeft; een verdrongen default en een vorm buiten geldigheid hebben geen lezing; het stoppunt
  van een samenstelling volgt uit de ledenoordelen; met vloeren op (case, vorm, onderwerp) en op knopen. De
  `Reading`-feiten in `SessionDump`, zodat `BranchDumpsCorpusWitnessTest` de takken ook daarop gelijk houdt. Een vlag
  op `DroolsBackendMain` die per case de lezingen in canonieke tekst print (voor `ref-trace-and-readings-gate`).
- (d) Een zusterregel telt niet mee voor `Observer` en `coverage`. Gate 12 `EXPECTED_PLAYED` en lijsten ongewijzigd;
  gates 5, 6, 12, 13 dezelfde uitkomsten; alle zestien gates groen.
- (e) Result met de gemeten aantallen en de voorgestelde `CLAUDE.md`-tekst (gate 12: de dumpsectie).

May touch: `backends/drools/**` behalve de productieruntime; `scripts/check-drools.sh` alleen als voorstel; de kaart.

Out of scope: de uitleg zelf (toolchain `explanation-from-readings`); de ref-trace en de leesclausule in de spec
(`ref-trace-and-readings-gate`).


## Result (2026-10-21)

Tak `card/fixpoint-readings`, vanaf `26a2fb85`. De bouwer raakte alleen `backends/drools/**` buiten de productieruntime en
deze kaart; hij stopte na `6abe7613` op verzoek van de hoofdsessie (context boven de rem), die de gates draaide, het
voorstel voor `scripts/check-drools.sh` (`3d9dbc59`) en de `CLAUDE.md`-tekst landde en dit Result afschreef uit het
concept van de bouwer.

- `6828e52e` de kaart naar `doing/`.
- `8fb0ab81` de leesregels achter een optie van de build (stap 1, acceptatie (a)).
- `f5fa0ab1` de ingang `readingsOf`, de getuige per vormfamilie en de corpusgetuige (stap 2, acceptatie (b) en (c)).
- `6abe7613` de dumpsectie, de vlag `--readings`, de waarnemer en de dekking, en de doorgifte van de replay (stap 3 en 5,
  acceptatie (c) en (d)).
- `3d9dbc59` (hoofdsessie) gate 12 eist de drie getuigen van de build met lezingen bij naam (het voorstel onderaan).
- dit Result (stap 4, acceptatie (e)).

### Wat er is

**De optie.** `DroolsBackend.build(document, branch, readings)` (en de variant met een pad); `Build.readings()` zegt of
een build lezingen heeft. Zonder de optie is de build die van vóór de kaart: de DRL-tekst van alle 310 goldens is
bytegelijk (264 gebouwd, 46 met dezelfde weigering), gemeten met de jar van `6828e52e` tegen die van `8fb0ab81`.

**De lader.** `IrLoader.loadWithPositions` geeft naast `Loaded` de `NodePositions` van het document: per knoop van de
voorwaarde- en de expressie-unie die de lader las, zijn JSON-pointer, zijn plaats in de pre-orde van het document en zijn
soort zoals de draad hem spelt, en per vormversie haar pointer. `Loaded` zelf is ongewijzigd: `TmsSkeletonWitnessTest`
en `DroolsBackendWitnessTest` houden dat elke publieke ingang die een model neemt elke zijtabel van `Loaded` neemt, en
de posities zijn geen zijtabel van de verlaging. De build met lezingen gaat door `TmsTranspiler.withReadings(Positioned,
ClassLoader)`, die het geladen document heel neemt en dus geen zijtabel kan weglaten; `TmsTranspiler.build` blijft de
ene ingang die een model neemt.

**De leesregel** (de *reading rule* van de aanvulling). Naast de eigen regel van elke vorm met een voorwaarde een
`RulePlan` "`<regel>: lezing`", van geen vorm en in de hoofdgroep, met dezelfde `readings` en de linkerkant van de eigen
regel zonder het voorwaarde-`TestItem`, en `Consequence.Reading` als gevolg: een logisch `Reading(subject, pointer,
values)` met de waarde van elk slot dat die linkerkant bindt. Dezelfde item-instanties, dus dezelfde constraint-id's. Wat
blijft: de wacht van een initialisatie (`TestItem.Role.VORM` en de `not` op een andere schrijver), de controlefeiten van
een default (`not Beslist`, `not Uitkomst`), de 1:1-wacht van een objectcreatie, het geldigheidsvenster. Wat gaat, bij een
creatie: de `CreationPhase`, de creatiewacht en de eigen fout, die haar tegen een tweede making houden (anders verdwijnt
de lezing zodra de creatie maakte); bij een feitcreatie bovendien het pad naar de doelen (één lezing per anker). De
analyse noteert, terwijl de voorwaarde en de waarde van de eigen regel compileren, per IR-knoop de term die hem berekent
(`note`, in de twee dispatchers en op de elf plaatsen die een knoop buiten hen om compileren); een knoop in de regels van
een vouw wordt niet genoteerd. Een knoop zonder term buiten een vouw is een fout van de analyse en wordt luid geweigerd
bij de build met lezingen.

**De opnemende weergave.** `RecordingRendering`, een derde weergave van `Term` naast `ClosureRendering`: dezelfde
functies (`ClosureRendering.implementation`, nu zichtbaar in het pakket), dezelfde stopregel (`established`,
`endVerdict`), dezelfde volgorde, een `Let` op behoefte, precies de tak van een keuze; per term-knoop de uitkomst. Elke
sonde-slot krijgt een verse sonde die niemand leest: een lezing deponeert niets.

**De ingang.** `DroolsBackend.readingsOf(Build, KieSession)`, met dezelfde toets op de sessie als `fixpointOf` en
`decisionsOf`, vóór en na de terugschrijving leesbaar:

```java
public record ConditionReading(Object object, String form, Status status, List<NodeReading> nodes) {
    public enum Status { HOLDS, FAILS, FAULTS, DISPLACED, OUTSIDE_VALIDITY }
    public record NodeReading(int node, String kind, boolean reached, Object value, Unit unit,
                              RuntimeException fault, List<Contributor> contributors) {}
    public record Contributor(Object member, Object value, RuntimeException fault, Boolean reached) {}
}
```

Een record per vorm met een voorwaarde en object waarop zij loopt (de klasseconstraints van het onderwerppatroon: het
concrete type van een stapel, de typen waarop een stapel een regel overnam). De vorm heet naar zijn pointer:
`/rules/rules/k/versions/v`, `/rules/consistencies/…`, `/rules/allocations/…`, `/rules/objectCreations/…`,
`/rules/factCreations/…`, en voor een default de pointer van `defaults-decider` (ook `/rules/rules/k/versions/v` voor
een regel die ch. 7.10 in een stapel opnam). Zonder `Reading`-feit: `OUTSIDE_VALIDITY` waar het venster de rekendatum
niet toelaat, `FAULTS` waar de sessie geen datum heeft en de vorm een gedateerd venster, `DISPLACED` waar de vorm een wacht
op haar beurt heeft; een vorm zonder wacht binnen haar geldigheid zonder lezing is een gebroken contract en wordt luid
geweigerd. De knopen in de pre-orde van het document over de voorwaarde- en de expressie-unie, eerst de voorwaarde
(wortel 0), dan de waarde waar de vorm er een berekent (een regel op een attribuut, een attribuutdefault); een knoop in
het filter, de matrix of de bron van een vouw heeft geen eigen lezing (de knoop van de vouw draagt de leden), maar telt
mee in de nummering. Waarden zoals `Fixpoint.DerivedValue` ze geeft, een exacte breuk ingeklapt zoals de schrijfgrens.

Afwijkingen van advies §5, elk nodig:
- `fault` is de fout zelf, geen zin: de canonieke tekst geeft de EF-code van haar soort (de zinnen verschillen tussen
  uitvoerders), en een `InputFormFault` noemt haar object pas met labels (`named`); `Fixpoint.Fault` draagt de fout ook.
- `kind` is de soort zoals de draad hem spelt (`Comparison`, `Aggregation`, …), geen eigen enum: een lezer legt het
  nummer op zijn eigen boom en kan de soort naast het nummer toetsen.
- `Contributor.value` in plaats van `verdict`: ook de leden van een waardevouw, met hun bijdrage (`null` bij een telling).
- `Contributor.reached` is een `Boolean`: `null` bij een kwantorvouw waarvan de matrix niet kan falen of deponeren. Daar
  toont geen fout of deponering de volgorde, de sessie houdt geen plaatsen bij, en het oordeel is dat over alle leden.
  Waar de matrix wel kan falen of deponeren, staan de leden in hun volgorde met de stopregel van ch. 5.10 herhaald.

Voorbeelden uit `ReadingsWitnessTest` (een regel per record, `-` = niet bereikt):

- een samenstelling "alle" die bij het tweede lid stopt:
  `J1 /rules/rules/0/versions/0 FAILS | 0 Compound false | 1 CharacteristicCheck true | 2 CharacteristicCheck false | 3 CharacteristicCheck -`
- een kwantorvouw over een rol met twee leden: `P1 /rules/rules/1/versions/0 FAILS | 0 QuantifierFold false [W1 true, W2 false]`
- een telling met filter (knopen 2 en 3, bron en filter, per lid): `P1 /rules/rules/0/versions/0 HOLDS | 0 Comparison true | 1 Aggregation 1 [W1 leeg reached] | 4 Literal 0`
- een default-keten met de beslisser in het midden: `J1 P0/defaults/0 FAILS | 0 CharacteristicCheck false`,
  `J1 P0/defaults/1 HOLDS | 0 CharacteristicCheck true`, `J1 P0/defaults/2 DISPLACED`
- een attribuutdefault die geen waarde gaf, en de beslisser eronder: `J2 P1/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 AttributeReference leeg`,
  `J2 P1/defaults/1 HOLDS | 0 CharacteristicCheck true | 1 Literal 10`
- een herschaalde vergelijking: `J1 /rules/rules/5/versions/0 HOLDS | 0 Comparison true | 1 AttributeReference 700 m | 2 Literal 0.5 km`

De canonieke tekst (`SessionDump.readingLines`, de vlag en de dumpsectie), zoals `the_flag_readings_prints_the_canonical_text` hem pint:

```
case lezing….json
reading /rules/rules/0/versions/0 "J1" 0 Compound false
reading /rules/rules/0/versions/0 "J1" 1 CharacteristicCheck true
reading /rules/rules/0/versions/0 "J1" 2 CharacteristicCheck false
reading /rules/rules/0/versions/0 "J1" 3 CharacteristicCheck unreached
reading /rules/rules/0/versions/0 "J1" FAILS
reading /rules/rules/1/versions/0 "J1" 1 AttributeReference 700 "m"
reading /rules/rules/1/versions/0 "P1" 0 QuantifierFold false contributors ["W1" true unordered; "W2" false unordered]
READINGS 1
```

### (a) eerst gemeten

`ReadingRulesFixpointWitnessTest`, vóór de opnemende weergave en de ingang: elke case van elke golden die de
exec-model-tak speelt, op beide takken door de build zonder en die met lezingen; de dumps (`SessionDump`) zonder de
`Reading`-feiten, hun sectie en de vuringen zijn gelijk. Gemeten 2026-10-21 bij `8fb0ab81`: per tak 261 goldens, 706
cases, 3635 lezingen aan de fixpunten, geen verschil; de build met lezingen bouwt elk van de 261 (geen knoop zonder term).
Vloeren `EXPECTED_MIN_GOLDENS=261`, `EXPECTED_MIN_CASES=706`, `EXPECTED_MIN_READINGS=3635`.

### (b) rood eerst

`ReadingsWitnessTest`, vier eigen documenten met Jutterswet-namen, de dertien vormfamilies van (b), op beide takken met
handgeschreven records, en de canonieke tekst van de vlag. Rood op `8fb0ab81` plus alleen de test (`mvn -o -q
test-compile`):

```
[ERROR] …/ReadingsWitnessTest.java:[5,44] cannot find symbol
[ERROR] …/ReadingsWitnessTest.java:[277,30] package ConditionReading does not exist
```

Groen sinds `f5fa0ab1`. Rood met een mutant, niet gecommit: een samenstelling van de opnemende weergave die niet stopt
geeft `3 CharacteristicCheck true` waar `3 CharacteristicCheck -` verwacht is.

### (c) over het corpus

`ReadingsCorpusWitnessTest`, op beide takken van de build met lezingen, per case van elke gespeelde golden:
- de lezing geldt waar de vorm vuurde: een kenmerktoekenning die geldt staat afgeleid, en elk afgeleid kenmerk heeft een
  toekenning die geldt, een beslissende default of een creatie die zijn object maakte; een regel op een attribuut die geldt
  met een gevulde waarde staat als `Value` van die regel, een die faalt of fout geeft niet; een consistentie geldt waar
  haar object geen schending (EF-025) heeft; een objectcreatie geldt waar zij voor haar onderwerp maakte; de default die
  besliste geldt, en waar niemand besliste geldt geen met een besluit;
- elke bereikte attribuutlezing heeft de waarde die het fixpunt dat lid geeft op het object dat de lezing bereikt
  (`DroolsBackend.reached` na de terugschrijving: dan staat ook een gemaakte koppeling op haar veld), en elke kenmerkcheck
  van het object zelf het oordeel van wat daar staat;
- een verdrongen default en een vorm buiten geldigheid hebben geen knopen;
- de stop van elke samenstelling volgt uit de oordelen van haar leden;
- elk nummer noemt de knoop van het document van de soort die de lezing zegt.

Gemeten 2026-10-21, per tak: 3635 (case, vorm, onderwerp) met knopen (2510 geldt, 1125 faalt, 0 fout), 13971 knopen,
3611 operanden, 1199 samenstellingen, 178 verdrongen, 13 buiten geldigheid; per soort 848 kenmerktoekenningen, 498 regels
op een attribuut, 16 initialisaties, 1160 consistenties, 456 objectcreaties, 22 feitcreaties, 3 verdelingen, 190
kenmerkdefaults en 442 attribuutdefaults. Vloeren `EXPECTED_MIN_READINGS=3635`, `EXPECTED_MIN_NODES=13971`,
`EXPECTED_MIN_OPERANDS=3611`, `EXPECTED_MIN_COMPOUNDS=1199`, `EXPECTED_MIN_DISPLACED=178`, `EXPECTED_MIN_OUTSIDE=13`.
Rood met dezelfde mutant: 1582 verschillen, onder meer `anchor/jutterswet-hardheid/case1-standaard.json
/rules/prioritizedDefaults/0/defaults/0 op M: samenstelling 0 bereikte lid 2 na de stop`, op beide takken.

`SessionDump` schrijft voor een build met lezingen de sectie `reading` (de canonieke tekst), naast de `Reading`-feiten
onder `fact`; `BranchDumpsCorpusWitnessTest` vergelijkt nu per case ook de dumps van de twee takken van de build met
lezingen: 17797 regels lezing, vloer `EXPECTED_MIN_READING_LINES=17797`. `DroolsBackendMain --readings [--drl] <ir>
<case>…` print per case `case <bestand>` en de lezingen, en als laatste regel `READINGS <n>`.

### (d) waarnemer en dekking

`ReadingRulesObserverWitnessTest`: per case, op beide takken, zien de waarnemers van de build zonder en met lezingen
dezelfde vormen, dezelfde dekking en dezelfde vuringen per vorm met hun labels (706 cases per tak, vloer 706). Dat
laatste is meer dan "de leesregel hoort bij geen vorm": een extra regel kan de volgorde van de agenda veranderen; gemeten
verandert hij de vuringen van geen enkele vorm.

### Stap 4: de kosten

Over het corpus, 261 goldens en 706 cases (`ReadingCostMeasurement`, niet gecommit; mediaan van 11 vuren na 5 om op te
warmen, per case, opgeteld):

| | exec-model | DRL-tekst |
|---|---|---|
| regels in de KieBase | 1932 → 2389 (+457) | 1932 → 2389 (+457) |
| vuringen | 9368 → 13239 (+41 %) | 9372 → 13243 (+41 %) |
| feiten aan het fixpunt | 10861 → 14496, waarvan 3635 lezingen | gelijk |
| tijd van een vuur | 37,2 ms → 39,9 ms (+7 %) | 54,0 ms → 58,7 ms (+9 %) |
| `readingsOf`, 3826 records | 2,6 ms | 2,3 ms |

Op de regelset herleving-WW v2 (`regels a7a5bdf`, de 13 fundament- en 20 regelbestanden uitgegeven door uchars frontend
in deze tak, zonder scope; de vijf service-smoke-verzoeken omgezet naar cases met de parameters van de service en zonder
rekendatum; exec-model; `ReadingCostV2Measurement`, niet gecommit): regels 335 → 444 (+109), geen evaluatiefout.

| verzoek | vuringen | feiten (lezingen) | vuur | `readingsOf` | records |
|---|---|---|---|---|---|
| detentie | 128 → 243 | 103 → 175 (72) | 3,00 → 3,20 ms | 0,45 ms | 28 geldt, 44 faalt; 823 knopen |
| herkrijging | 158 → 298 | 124 → 203 (79) | 2,25 → 2,53 ms | 0,56 ms | 34 geldt, 45 faalt, 3 verdrongen; 900 knopen |
| inkomen | 220 → 367 | 174 → 256 (82) | 2,12 → 2,34 ms | 0,44 ms | 39 geldt, 43 faalt, 2 verdrongen; 865 knopen |
| intrekking | 116 → 215 | 97 → 167 (70) | 1,16 → 1,46 ms | 0,31 ms | 25 geldt, 45 faalt, 2 verdrongen; 778 knopen |
| restant | 174 → 355 | 141 → 269 (128) | 1,41 → 1,56 ms | 0,04 ms | 43 geldt, 85 faalt, 1 verdrongen; 1478 knopen |

De vuurgrens rekent met de regels van de KieBase (`FACTOR × R × N0`), dus met de leesregels mee; geen case kwam er in
de buurt.

### Stap 5: de replay geeft zijn sessie door

Gedaan, omdat het geen tweede pad en geen uitzondering vraagt: `CaseRunner.report(ir, cases, Handoff)` speelt elke case
op een build met lezingen en geeft de afnemer de sessie direct na het vuur en vóór de terugschrijving, met de labels en de
`Firing`; één aanroep in `fireToFixpointFiring`, op hetzelfde pad als het rapport zonder afnemer. De afnemer leest daar
`fixpointOf`, `decisionsOf` en `readingsOf` van de sessie die het rapport beoordeelde; de beans die hij kreeg dragen na het
rapport de teruggeschreven waarden. Een case waarvan de invoer of het vuur een uitzondering geeft, krijgt hij niet. Getuige
`ReplayHandoffWitnessTest`: over het corpus is het rapport met doorgifte gelijk aan dat zonder (706 cases doorgegeven,
vloer 706), en op `anchor/jutterswet-vondstwaardering` draagt de doorgegeven bean M na het rapport de waardering 50. De
scenario-runner van de toolchain kan daarmee `CaseSession.play` en de vergelijking van de vuringen laten vallen; dat is
de toolchain-kaart.

### Niet gedaan, met reden

- **Een feitcreatie waarvan de voorwaarde een kenmerkcheck op haar doel is**, die de doelen filtert, heeft geen lezing.
  Die voorwaarde is geen toets van de regel maar een constraint op het doelpatroon (`TmsTranspiler.targetGuard`), en
  haar oordeel hoort bij een (anker, doel)-paar, een vorm die het record (vorm, object) niet heeft. In het corpus 4 van
  de 8 feitcreatieversies met een voorwaarde (`jutterswet-bakensein`, `-mastsein`, `-seinhijsing` twee keer); de
  javadoc van `ConditionReading` zegt het.
- **De waarden van de initialisaties van een objectcreatie** staan niet in de lezing, alleen de voorwaarde: zij worden bij
  de making geëvalueerd, niet aan het fixpunt, en het gemaakte object draagt ze als invoer.
- **Een lezing van een vorm buiten de scope van een entrypoint** (normvraag 4 van het advies) is de toolchain: zo'n vorm
  heeft in de build geen regel en dus geen lezing.
- **De tak-gelijkheid van de volgorde van de leden van een ongeordende kwantorvouw**: de canonieke tekst sorteert de leden
  op hun naam, en `reached` is daar `unordered`.

### Vondsten buiten de kaart

- Geen fout van het antwoordpad gevonden: de productiefeiten, de waarnemer, de dekking en de vuringen per vorm zijn met en
  zonder leesregels gelijk over het hele corpus.
- Voor de kaart `ref-trace-and-readings-gate`: (1) de canonieke tekst geeft een getal als zijn decimaal zonder
  nullen achteraan, ook een niet-afbrekende breuk, die de ingang inklapt zoals de schrijfgrens (`Breuk.naarDecimal`,
  DECIMAL64); ref moet daar hetzelfde doen of de gate moet breuken op die precisie vergelijken. (2) De leden van een
  kwantorvouw waarvan de matrix niet kan falen of deponeren hebben in Drools geen volgorde (`unordered`); ref leest ze
  wel in volgorde en stopt, dus de gate vergelijkt daar alleen de leden en hun oordelen, niet `reached`. (3) Een
  gemaakt object heet `«"creatie"»("onderwerp")#k`, de naam van de dump; ref noemt een gemaakt object anders. (4) Een
  fout staat als `fault <EF-code>` of `fault -`, nooit met zijn zin.
- Voor de toolchain-kaart `explanation-from-readings`: de scenario-runner kan `CaseRunner.report(…, Handoff)` gebruiken in
  plaats van `CaseSession.play`; speelt hij toch een eigen sessie op de build met lezingen, dan zijn de vuringen per vorm
  die van het rapport zonder lezingen (gemeten, `ReadingRulesObserverWitnessTest`).

### Gates

De volledige set op `6abe7613` (`werk/defaults-decider/u2/gates-1`): alle zestien `rc=0`, `EXIT 0` (gate 12 696 s,
gate 13 198 s). Na `3d9dbc59` gate 12 opnieuw (`u2/gate12-2.log`): `PASS`, played 261 op beide takken (elke statusregel van de DRL-tekst gelijk aan die van het exec-model), JUnit 1646 tests (vloer 1573), de drie getuigen van de build met lezingen bij naam gedraaid (DRL-tekstsweep 223 s).

### Voorgestelde tekst voor `CLAUDE.md` (gate 12, na de zinnen over `BranchDumpsCorpusWitnessTest`)

```
    Since 2026-10-21 (card `fixpoint-readings`) the backend has a build
    with readings (`DroolsBackend.build(…, readings)`): beside the rule of
    every form with a condition a reading rule, with the left-hand side of
    the form's rule without its test, whose facts `readingsOf` reads per
    form, object and node; the production build is byte-equal to the one
    before. The suite holds it: `ReadingRulesFixpointWitnessTest` (with and
    without readings the facts at the fixpoint are equal, per case, on both
    branches), `ReadingsCorpusWitnessTest` (the readings agree with the
    fixpoint: a reading holds where its form fired, an operand has the
    value of the fixpoint, a composite stops where its members say),
    `ReadingRulesObserverWitnessTest` (the observer, the coverage and the
    firings per form are those of the build without), and the dump of
    `BranchDumpsCorpusWitnessTest` carries a section `reading`, so the two
    branches agree on the readings as well; floors in those classes,
    measured 2026-10-21. `DroolsBackendMain --readings` prints the readings
    per case in the canonical text the reference interpreter's trace is
    held to (card `ref-trace-and-readings-gate`).
```

### Voor `scripts/check-drools.sh` (geland in `3d9dbc59`)

De gate eist nu alleen `BranchDumpsCorpusWitnessTest` bij naam; de drie nieuwe corpusgetuigen tellen alleen mee in
`EXPECTED_MIN_TESTS`. Voorstel, na het blok van `BranchDumpsCorpusWitnessTest`:

```sh
  # card fixpoint-readings: the build with readings leaves the fixpoint, the observer and the coverage alone, and its
  # readings agree with the fixpoint, over every played golden; this build must have run those witnesses, by name
  for witness in ReadingRulesFixpointWitnessTest ReadingsCorpusWitnessTest ReadingRulesObserverWitnessTest; do
    report=backends/drools/target/surefire-reports/TEST-dev.uchar.regelspraak.cases.$witness.xml
    [ -f "$report" ] && [ "$report" -nt "$suite_begin" ] \
      || fail "$witness did not run in this build (no surefire report newer than its start: $report) — the witnesses of the build with readings are required"
  done
```
