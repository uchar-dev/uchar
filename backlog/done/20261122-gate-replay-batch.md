# gate-replay-batch: de replays van de Drools-backend in een paar JVM's

*Gevraagd door de eigenaar op 2026-11-22 ("1229 s is veel te lang om iets routinematig te doen"; later: dit pas nadat afleiden uit leeg geland is).*

## Aanleiding, gemeten

`make gates` duurt 592 s (2026-11-22, 8430 CPU-s, 14 kernen bezig van 16). Ongeveer de helft van die CPU-tijd zijn JVM-starts: gates 8, 12, 13 en 17 spelen
ieder document in een eigen `java -jar` (samen rond 3850). Dezelfde 433 goldens, met dezelfde statusregel per golden (0 verschillen):

| replay | een JVM per document, acht naast elkaar (nu) | acht JVM's, elk een achtste |
|---|---|---|
| executable model | 40 s, 400 CPU-s | 2,4 s, 29 CPU-s |
| DRL-tekst | 55 s, 562 CPU-s | 4,8 s, 54 CPU-s |

## Goal

De gates spelen dezelfde documenten, met dezelfde oordelen, lijsten, pins en meldingen, in een paar JVM's. Geen stille groene gate, en geen zwakkere
isolatie dan nu: een document dat de JVM laat crashen, vastlopen of vollopen, faalt als dat document, en een document beïnvloedt geen ander.

## Het mechanisme

1. `DroolsBackendMain --batch <taken>`: per regel van `<taken>` een taak, `<sleutel>` en de argumenten van de CLI van nu (`--readings`, `--drl`, ir, cases). Per taak
   schrijft de JVM de standaarduitvoer naar `<map>/<sleutel>.out` en na het sluiten daarvan, in één beweging, de exitcode naar `<map>/<sleutel>.rc`: precies wat
   een eigen JVM had geschreven. De uitvoer loopt langs een `PrintStream` die de CLI meekrijgt, niet langs `System.out`. Een `RuntimeException` is de regel die
   de CLI al heeft; een `Error` of `System.exit` eindigt de JVM.
2. `scripts/lib/parallel.sh` krijgt naast `parallel_run` een `parallel_batch`: de taken worden in lijsten van hoogstens ±50 over een lege map verdeeld, n JVM's
   tegelijk (n = kernen / 2, zoals nu), elk met de vlaggen van nu en een eigen logboek voor de standaardfout. Een lijst van 50 begrenst wat taken in één JVM
   kunnen ophopen (klassen, geheugen).
3. **Een JVM die eindigt voordat zij al haar taken deed, maakt de gate rood**, met de taak die liep, de exitcode, het eind van haar standaardfout en de taken die zij
   daarvoor deed (de eerste taak zonder `.rc` is vaak het slachtoffer, niet de dader). Geen herstart en geen doorgaan: rood is rood. Een taak zonder `.rc` en een
   dubbele sleutel zijn rood. Wie het document zoekt, draait het alleen met de CLI van nu.
4. De gate leest de uitvoer en de exitcode per sleutel zoals nu; haar oordelen, lijsten, pins en de volgorde van het verslag veranderen niet.
5. De vier poten van gate 13 die document na document een JVM starten, schrijven hun taken eerst op en lezen daarna, zodat ook zij in de batch gaan; gate 8
   speelt haar documenten per case met dezelfde hoofdklasse (als een gepinde fout daar een `Error` of `System.exit` is, blijft gate 8 zoals zij is).

## Acceptance

- Per gate (8, 12, 13, 17): de uitvoer vóór en na regel voor regel gelijk (eenmalig, elk verslag van een groene en van elke bestaande rode
  mutant), op de regel over het aantal processen na.
- De rode rijen van `parallel_batch`, elk met zijn eigen zin: een taak die de JVM laat eindigen (`System.exit`), een die het geheugen volloopt, een ontbrekende
  `.rc`, een dubbele sleutel, een taak die een regel te veel schrijft.
- **Geen lek tussen documenten**, eenmalig gemeten: alle taken van gates 12 en 17 in de gewone, de omgekeerde en drie willekeurige volgordes (vaste zaadwaarden),
  en elk alleen, geven dezelfde uitvoer en exitcode; `System.properties`, `Locale` en `TimeZone` zijn na elke taak wat zij ervoor waren (een toets in
  `--batch` die anders eindigt).
- `make gates` naast de vorige: de tijd per gate en de CPU-tijd; doel gate 12 ≤ 480 s, gate 13 en 17 elk ≤ 200 s, gate 8 ≤ 200 s, de set rond 450 s.
- Geen test, vloer, pin of lijst verandert.

## May touch

`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackendMain.java` en haar tests; `DroolsRedMain.java` ernaast, alleen haar `main` door een `run` die
de batch aanroept (gate 8 start die hoofdklasse, niet `DroolsBackendMain`, anders dan deze kaart zei); `scripts/lib/parallel.sh`, `check-drools.sh`, `check-readings.sh`,
`check-reftype-derivation.sh`, `check-red.sh` (elk in een eigen commit; landt alleen met akkoord van de eigenaar); `CLAUDE.md` als voorstel.

## Daarna

De JUnit-suite van gate 12 profileren (klassen die telkens dezelfde goldens bouwen); gate 2 splitsen (één proces). Elk op een nieuwe meting.

## Result

*Gebouwd 2026-11-22 op `card/gate-replay-batch`, vanaf `af54fac21`. Gemeten op de machine van de eigenaar, 16 kernen, zonder andere gate of suite (achtergrond:
Spotlight en mediaanalyse, samen ongeveer één kern). Alleen deze kaart; `make gates` draait de hoofdsessie.*

**Wat er staat.** `DroolsBackendMain --batch <taken> <map>` speelt een lijst aanroepen na elkaar (`batch`): per taak `<sleutel>.out` en, nadat dat bestand gesloten is,
in één verplaatsing `<sleutel>.rc`. `parallel_batch` en `parallel_batch_task` in `scripts/lib/parallel.sh` verdelen de taken om en om over lijsten van hoogstens 50, draaien
er n tegelijk en houden elke taak aan haar `.rc`. Gates 8, 12, 13 en 17 schrijven hun replays op en lezen ze daarna, met de oordelen, lijsten en pins die zij hadden.

### Acceptance

1. **Verslag regel voor regel gelijk.** Groen: de vier verslagen (standaarduitvoer en standaardfout) en exitcodes zijn gelijk op de tijden in gates 12 en 17, de testtelling in
   gate 12 (2055 naar 2066, elf nieuwe tests) en het aantal bronbestanden in de regel `drools-jar: fresh` na; de regel over het aantal processen verandert niet. Per taak zijn
   uitvoer en exitcode gelijk aan een eigen JVM: gate 12 866, gate 17 912, gate 13 2028 (de vijf poten), gate 8 102 taken, 0 verschillen. In gate 8 is de vergelijking op de
   volgorde van de parameters in een `FAULT`-regel na, die ook de kale CLI van vroeger per JVM-start wisselt (6 tegen 4 van 10 starts op `as-of-date-calendar`; `Map.of`): de
   gate leest JSON. Rood: dezelfde fout op de gate van vroeger en op die van nu, in een kopie van de boom (gates 17 en 13 op 59 of 60 goldens, met de vloeren op 0): gate 12
   10, gate 17 13, gate 13 7 en gate 8 10 rode mutanten (een gewijzigde verwachting, een afgebroken document, een verouderde of ontbrekende regel van elke lijst, een andere
   statusregel, lege uitvoer, een `ERROR`, een andere code, een vloer), en vier groene proeven (een regel vóór de statusregel, een overlever, groen): elk hetzelfde verslag en dezelfde exitcode. De mutanten die de JVM raken (`System.exit`, vol geheugen, een
   `Error`, een gezette eigenschap, een regel buiten de taak) zijn rood met de eigen zin van `parallel_batch`; de laatste twee waren vroeger groen. In gate 13 is de exitcode
   van zo'n rood 2 in plaats van 1, zoals die poten al eindigden als `parallel_run` faalde.
2. **Rode rijen van `parallel_batch`**, in de zelftoets `sh scripts/lib/parallel.sh --selftest` (0,6 s, gate 12 draait haar voor haar replay): een taak die haar proces
   eindigt, de taken die het gedaan had, vol geheugen, een ontbrekende `.rc`, een proces dat rood eindigt na zijn laatste taak, een regel te veel, een `.rc` van geen taak, een
   dubbele sleutel, een map die niet leeg is, en de verdeling van 120 taken over vier lijsten. Elf mutanten van `parallel_batch` maken haar rood. De JVM zelf staat in de suite:
   `BatchWitnessTest` (zes tests, `System.exit` en een vol geheugen in een kind-JVM met de vlaggen van de gates; zeven mutanten van `batch` rood) en `DroolsRedMainWitnessTest`.
3. **Geen lek tussen documenten.** Alle taken van gates 12 en 17 (1778 aanroepen, 1732 verschillende) door elkaar, gewoon, omgekeerd en met de zaadwaarden 20261122, 12 en 17:
   0 verschillen met een eigen JVM, per volgorde. Ook alle 1732 in één JVM: 0 verschillen. De 102 rode cases in dezelfde vijf volgordes: 0 verschillen. `batch` vergelijkt na
   elke taak de systeemeigenschappen, de standaardtaal (ook per categorie) en de tijdzone, en stopt anders met een zin en exitcode 4. Wat zij vond: de MVEL-taal van Drools
   schrijft bij haar eerste gebruik `mvel.debugger=false`, de waarde die een afwezige daar heeft en die één klasse leest; `batch` zet haar daarom vooraf. Verder onder `run`: alleen
   de gebruiksregel gaat naar `System.err`, niets naar `System.out`; geen threads die de JVM openhouden (getoetst); statische toestand is `BeanCompiler.CACHE` (op de
   inhoud van de bronnen), de getters van de runtime per klasse, de catalogi (constant), `Boundary` (gesloten in een `try`) en de `KieRepository`, waar de DRL-tak per document
   de ene module van het standaardadres vervangt en bij een fout vóór de container stopt.
4. **Tijden**, elke gate alleen (`/usr/bin/time -p`, eind van de kaart):

   | gate | vóór | CPU vóór | nu | CPU nu |
   |---|---|---|---|---|
   | 8 | 56 s | 173 s | 12 s | 23 s |
   | 12 | 217 s | 2545 s | 157 s | 1769 s (vooral de JUnit-suite) |
   | 13 | 204 s | 2421 s | 76 s | 474 s |
   | 17 | 89 s | 957 s | 25 s | 126 s |
   | samen | 566 s | 6096 s | 270 s | 2392 s |

   Lijstlengte, twee keer gemeten met acht JVM's (vroeger, een JVM per golden: exec 35 s en 378 CPU-s, DRL-tekst 50 s en 542 CPU-s): grens 25 exec 4,0 s en 50 CPU-s, DRL 7,0 s en
   89; grens 50 3,1 s en 40, 5,6 s en 73; grens 100 2,4 s en 29, 4,3 s en 52. Geheugen van een batch-JVM: grootste heap na een collectie 26 tot 52 MB, grootste RSS
   605 tot 795 MB (de start-heap is 2 GB); met alle 1732 taken in één JVM 89 MB heap en 955 MB RSS.
5. Geen test, vloer, pin of lijst is veranderd; elf tests zijn erbij.

**Gate 8.** De kaart zei dat zij dezelfde hoofdklasse start; dat is `DroolsRedMain`. Met akkoord van de hoofdsessie is haar `main` `System.exit(run(args, System.out))`, en een taak
met `--red` roept `run` aan. De kale CLI is byte-gelijk op de parametervolgorde na (alle 102 cases; `DroolsRedMainWitnessTest` pint vijf volledige regels met hun exitcode en is
ook groen op de hoofdklasse van vroeger). Geen rode case geeft een `Error` of `System.exit`: `runRed` vangt alleen een `RuntimeException`, alle 102 cases geven met `--red` een
statusregel en exitcode 0 (83 `FAULT`) of 2 (19 `EXCLUDE`), en een `Error` of exit zou hier de batch rood maken. Gate 8 gebruikte geen `PARALLEL_JVM_FLAGS`; met die
vlaggen is elke statusregel gelijk aan die zonder.

### Voorlopige besluiten

1. Een eigen opschrijver `parallel_batch_task` in plaats van de taken van `parallel_task` terug te lezen: één regel per taak, geen proces per taak.
2. De grens is 50, zoals de kaart zegt; omdat het aantal lijsten een veelvoud van n is, zijn de lijsten van gate 12 28 taken en die van gate 17 37. Een grens van 60 zou
   gate 12 acht lijsten van 55 geven en ongeveer 30 CPU-s schelen.
3. Een batch-JVM krijgt naast de vlaggen van nu `-XX:+ExitOnOutOfMemoryError`: een vol geheugen eindigt de JVM ook als code eronder het opvangt, zodat gate 13 nooit twee
   `ERROR`-regels door geheugengebrek gelijk vindt.
4. Gate 13 speelt onveranderde documenten en kopieën in gescheiden batches, en elk per weergave, zodat een kopie nooit in de JVM van haar origineel speelt en een weergave nooit
   in die van de andere, met wie zij vergeleken wordt.
5. De vier poten van gate 13 houden één JVM tegelijk, zoals zij hadden; gate 8 speelt op één proces per twee kernen, waar zij een case tegelijk speelde.
6. Een `RuntimeException` buiten de armen van `run` (een pad dat geen pad is) geeft zijn taak exitcode 1 en een stacktrace op de standaardfout, zoals een eigen JVM; een `Error`, een
   kapotte lijst en een veranderde JVM geven exitcode 4.
7. Een regel op de standaarduitvoer van de JVM zelf is rood: zij staat in geen uitvoer van een taak.

### Open punten

- De volgorde van de parameters in een `FAULT`-regel wisselt per JVM-start (`Map.of`); bestaand, en geen gate leest haar.
- Een document dat niet eindigt, houdt de gate op, zoals nu; de proceslijst noemt het document niet meer, de eerste taak zonder `.rc` in `<map>/.batch/<lijst>.tasks` wel.
- Gate 11 start voor haar slices een JVM per case; zij stond niet op de kaart.
- Wat gate 12 nu duurt, is de JUnit-suite (de kaart "Daarna" noemt haar al).

### Voorgestelde tekst voor CLAUDE.md

- *How the set runs*, de zin over de replays: "Replays run through `scripts/lib/parallel.sh` (one process per two cores, in gate 6 one per core) and are judged in corpus order;
  those of the Drools backend (gates 8, 12, 13, 17) run in batches of at most 50 to a JVM, with its replay flags, and a JVM that ends before each of its replays has left
  its exit code makes the gate red with the replay it ran; no gate takes arguments."
- *Gate 8*, na de zin over de Drools-poot: "It replays its cases in batches (`DroolsBackendMain --batch`, a task `--red`); one case alone is `DroolsRedMain`."
- *Gate 12*, bij "The replay runs in two passes": "each in batches (`DroolsBackendMain --batch`), after the self-test of `scripts/lib/parallel.sh`, whose rows hold
  that no task of a batch is missed;".
- *Gate 13*, in plaats van "The legs run as five processes, the presentation leg's calls through `scripts/lib/parallel.sh`;": "The legs run as five processes; their replays in
  the backend run in batches, the untouched documents and the copies in JVMs of their own, the other calls of the presentation leg through `scripts/lib/parallel.sh`;".
- *Gate 17*, "Traces run on one process per core": "Traces run on one process per core, the readings and the replays of the backend in batches."

### Nagerekend door de hoofdsessie (2026-11-22)

`make gates` op `f12b13b57`: groen in 299 s (was 592 s) en 4001 CPU-s (was 8430). De verslagen van gates 8, 12, 13 en 17 naast die van de vorige set: gelijk op tijden en de testtelling na. Een tweede lezing van de diff (extern
model, hoogste stand) vond geen weg naar een stille groene gate; haar twee "brekers" (awk met meer dan zeventien bestanden, `"$@"` zonder argumenten onder `set -u` in bash 3.2) zijn op deze machine weerlegd (60 bestanden,
sh 3.2.57 en dash). Overgenomen: de scheiding per weergave in gate 13 (gate 13 alleen 80 s en 517 CPU-s, was 76 en 474; verslag gelijk) en het opruimen van een tijdelijk bestand in gate 8 (11,8 s).
