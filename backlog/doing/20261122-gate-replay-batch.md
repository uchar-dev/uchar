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

`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackendMain.java` en haar tests; `scripts/lib/parallel.sh`, `check-drools.sh`, `check-readings.sh`,
`check-reftype-derivation.sh`, `check-red.sh` (elk in een eigen commit; landt alleen met akkoord van de eigenaar); `CLAUDE.md` als voorstel.

## Daarna

De JUnit-suite van gate 12 profileren (klassen die telkens dezelfde goldens bouwen); gate 2 splitsen (één proces). Elk op een nieuwe meting.
