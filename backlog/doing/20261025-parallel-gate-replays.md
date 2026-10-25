# parallel-gate-replays: de dure gates spelen hun goldens over meer processen tegelijk

*Vastgelegd door de hoofdsessie op 2026-10-25, op verzoek van de eigenaar ("30 minuten voor alle gates … ik dacht dat
we rond de 5 minuten zaten") en aangepast na een Fable-advies (`werk/gates-design/`). Derde van drie kaarten; begint
na de landing van `20261025-gate-builds-when-stale` (beide raken de gate-scripts, `WAY-OF-WORKING.md` §1). Raakt
`scripts/`, een beschermd pad: de kaarttak blijft een voorstel tot de eigenaar het reviewt.*

## Aanleiding

De volledige gate-set duurt sinds de komst van gate 17 (2026-10-21) 29 tot 32 minuten op een machine met 16 cores,
tegen 19 tot 20 minuten op 2026-10-20. Gemeten op `210dfed2` (`werk/drl-rule-functions/gates-2.summary.txt`), na
elkaar, in seconden:

| gate | s | waar de tijd zit |
|---|---|---|
| 12 `check-drools.sh` | 760 | de JUnit-suite (±356 s; kaart `drools-suite-forks`), de replay op de exec-model-tak (±150 s, één `java -jar` per golden), de sweep van de DRL-teksttak (230 s, één `java -jar --drl` per golden) |
| 17 `check-readings.sh` | 397 | per golden ref `--trace` en twee keer `java -jar --readings`, daarna de vergelijking in Python |
| 6 `check-clingo.sh` | 216 | per case translate, clingo en de controle, na elkaar (`while read case`) |
| 13 `check-reftype-derivation.sh` | 204 | per golden en per been (twist, vocabulaire, herkomst, naam) ref, clingo en twee keer `java -jar` |
| de andere dertien | ±250 | |

Het corpus groeide van 116 goldens en 338 cases (2026-08-25) naar 329 goldens en 899 cases; elke replay schaalt mee,
per uitvoerder. Elke lus speelt één golden tegelijk. Meting van het Fable-advies (2026-10-25, 24 goldens, één
`java -jar` per golden): na elkaar 9,9 s, met 8 processen 3,4 s, met 16 processen 3,2 s; één replay-JVM gebruikt
gemiddeld 3,2 cores (opstart en JIT, 400–550 MB), dus 4 à 5 JVM's tegelijk vullen 16 cores.

Goal: gates 6, 12, 13 en 17 doen parallel wat parallel kan en na elkaar wat na elkaar moet. **Verzamelen parallel,
oordelen op volgorde:** de dure aanroep per golden of case (`java -jar`, `clingo_runner`, `runner --trace`) draait in
een verzamelpas over meer processen, met de uitvoer en de exitcode per item in een eigen bestand; daarna leest de
bestaande lus die bestanden in de volgorde van nu, in plaats van het proces zelf te starten. Oordelen, lijsten, pins,
tellers, fail-first en de volgorde van het rapport blijven letterlijk zoals ze zijn. Eén hulp
`scripts/lib/parallel.sh` (POSIX sh, want gate 6 is sh; `xargs -P` bestaat in BSD en GNU; cores via `getconf
_NPROCESSORS_ONLN`). Gate 12 doet twee passen (exec-model, dan DRL-tekst), zodat een fout op de exec-model-tak faalt
vóór de kosten van de sweep. Gate 13 draait eerst zijn vier benen als vier processen; per golden alleen als dat nog
meetbaar helpt. Er komt één manier om een gate te draaien: geen vlag, geen variabele en geen seriële modus naast de
parallelle; daarom gaat ook `--fast` van gate 12 weg (besluit van de eigenaar, 2026-10-25), zodra de volle gate rond de
4 minuten zit.

Het aantal processen, zonder knop, met de meting als reden in het script: het aantal cores voor ééndraads uitvoerders
(gate 6: `clingo_runner` en clingo), cores/4 voor de JVM-lussen. Meet in de kaart ook de JVM-vlaggen
`-XX:TieredStopAtLevel=1 -XX:+UseSerialGC` op de replay-JVM's (zij veranderen geen semantiek); verlagen zij het
cores-gebruik per JVM zo dat meer processen helpen, dan gelden ze en volgt het aantal processen de nieuwe meting.

Acceptance:
- (a) Per gate de wandkloktijd vóór (de gate op `main` bij de claim) en na, op dezelfde commit en dezelfde machine,
  zonder andere gate-runs ernaast; `make gates` vóór en na. Verwacht: de set 5–7 minuten (met de forks van
  `drools-suite-forks`); haalt het dat niet, dan zegt het Result waar de tijd blijft.
- (b) Het rapport van elke gate is, op tijdregels na, gelijk aan dat van de gate van vóór op dezelfde commit (een `diff`
  van beide logs, met de tijdregels eruit gefilterd, in het Result).
- (c) Rood blijft rood: per gate een mutatie (een golden met een fout verwachte waarde, een case die een uitvoerder laat
  falen) die de gate van vóór en die van na allebei rood maakt, met dezelfde faalregel bij naam.
- (d) Een fout in een parallel proces (een crash van de JVM, een uitvoerder die niet start) geeft een leeg
  uitvoerbestand of een exitcode ≠ 0 en laat de gate falen met de naam van de golden, in de bestaande arm daarvoor;
  geen proces dat stil verdwijnt. Eén regel in het rapport noemt het aantal processen. Werkt op macOS en Linux
  (`/bin/sh` is op CI dash).
- (e) `--fast` weg uit `scripts/check-drools.sh`, `CLAUDE.md` (gate 12) en het commentaar dat hem noemt.
- (f) `make gates` groen; de vloeren ongewijzigd; de toolchain leest `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` nog uit
  `scripts/check-drools.sh` (`regelspraak-toolchain/scripts/check-service-replay.sh`).

May touch: `scripts/check-clingo.sh`, `scripts/check-drools.sh`, `scripts/check-reftype-derivation.sh`,
`scripts/check-readings.sh`, `scripts/lib/parallel.sh` (nieuw), `CLAUDE.md` (een zin bij de gates 6, 12, 13 en 17, en
`--fast` weg), deze kaart.

Out of scope:
- Wat een gate controleert, zijn lijsten, pins en vloeren.
- De forks van de suite (kaart `20261025-drools-suite-forks`) en het bouwen en de runner (kaart
  `20261025-gate-builds-when-stale`).
- Een golden in een JVM laten draaien die andere goldens al draaide (een batchmodus van `DroolsBackendMain`): dat
  verandert de isolatie tussen goldens en is een eigen besluit.
- De andere dertien gates.
