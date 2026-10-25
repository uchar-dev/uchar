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

## Result (bouwer, 2026-10-25) — voorstel, niet geland

Raakt beschermde paden (`scripts/`, `CLAUDE.md`): de tak blijft een voorstel tot de eigenaar het reviewt. Logs van alle
runs onder `werk/gates-design/parallel-replays/logs/`.

### Commits op `card/parallel-gate-replays` (op `56a25404`)

- `1a9c32a2` `scripts/lib/parallel.sh` (nieuw); gate 6 verzamelt en beoordeelt.
- `0227febf` gate 12 in twee passen; `--fast` weg (script, kop, het commentaar bij de suite).
- `a5e56ed4` gate 13: de vier benen als vier processen.
- `f08dbbdc` gate 17 verzamelt traces, readings en replays.
- `0dd426a5` `CLAUDE.md`: een zin bij gates 6, 12, 13 en 17; de `--fast`-zinnen van gate 12 weg.
- `22956095` gate 6: de meting in het commentaar. `8dcdeeca` parallel.sh: alinea over de JVM-vlaggen (alleen
  commentaar). Daarna dit Result.

### `scripts/lib/parallel.sh`

POSIX sh, gesourced. `parallel_task DIR KEY STDERR COMMAND ARG…` legt een taak vast (`DIR/KEY.args`, een argument per
regel; STDERR `out` = 2>&1, `err` = eigen bestand, `null`); `parallel_run DIR N` draait alle taken van DIR met `xargs -P
N -n 1`, elk met stdin /dev/null, en schrijft per taak `KEY.out` (en `KEY.err`) en na afloop `KEY.rc`. De shell die een
taak draait eindigt altijd met 0, zodat een status (ook 255) de andere taken nooit stopt; een taak zonder `.rc` na xargs
(zijn shell is gestorven of xargs stopte) of een xargs-status ≠ 0 laat `parallel_run` falen met `parallel: FAIL —` en het
commando van de taak. Verkeerd gebruik (sleutel, STDERR, geen commando, dubbele sleutel, newline in een argument)
beëindigt de gate met 2. `parallel_jobs D` = cores (`getconf _NPROCESSORS_ONLN`) / D, minstens 1, zonder knop.
`PARALLEL_JVM_FLAGS='-XX:TieredStopAtLevel=1 -XX:+UseSerialGC'` voor elke replay-JVM van de vier gates.

**De JVM-meting** (kaartopdracht; 54 goldens, elke zesde met een casebestand, een `java -jar` per golden, 16 cores, load
±7 bij het begin; tabel ook in de kop van `parallel.sh`): exec zonder vlaggen 24,7 s serieel (86 s cpu), 8,3 / 6,7 /
6,7 s op 4 / 8 / 16 processen; met de vlaggen 21,3 s (33 s cpu), 6,1 / 3,6 / 3,3 s. `--drl` zonder 38,5 s (136 s cpu),
12,5 / 10,6 / 10,5 s; met 31,7 s (49 s cpu), 9,1 / 5,5 / 4,6 s. Een replay-JVM houdt zonder vlaggen 3,5 kernen bezig, met
1,5; alle 54 statusregels gelijk met en zonder, op elk aantal processen. De vlaggen gelden dus, en het aantal volgt de
nieuwe meting: D = 2 voor de JVM-lussen.

**Processen per gate**, elk met de meting als reden in het script en één regel in het rapport:

| gate | processen (16 cores) | regel | reden |
|---|---|---|---|
| 6 | cores (16) | `check-clingo: replays on 16 processes (one per core)` | runner en clingo ééndraads; replay-pas 152 s serieel, 51 s op 4, 36 s op 8 en op 16: de langste case (`jutterswet-opvolgerversies/case3-tweede-versie.json`) duurt alleen al 35 s |
| 12 | cores/2 (8) per pas | `  replay: 8 processes per pass (one per two cores)` | replay-JVM met de vlaggen 1,5 kern |
| 13 | 4 (de benen) | `  legs: 4 processes (unit, vocabulary, origin, name)` | vier benen, elk één golden tegelijk; ±6 kernen bezig |
| 17 | traces cores (16), readings en replays cores/2 (8) | `  traces on 16 processes (one per core), readings and replays on 8 (one per two cores)` | runner ééndraads; JVM 1,5 kern |

**Gate 12, twee passen.** Na de suite (ongewijzigd, eerst): pas 1 legt per golden met cases `java $PARALLEL_JVM_FLAGS
-jar "$JAR" "$ir" $cases` vast, draait ze, en de bestaande exec-lus leest `tail -1` van `$work/exec/<i>.out` (i = plaats
in de corpusvolgorde) waar hij `java` startte; alle oordelen, lijsten en pins tot en met `EXPECTED_PLAYED` daarna. Pas 2
(de DRL-sweep, `drl_begin` vóór het verzamelen, dus de sweepkosten zijn de pas) begint pas als pas 1 groen is, en de
bestaande DRL-lus leest `$work/drl/<i>.out`. Een JVM die crasht of niet start geeft een lege uitvoer: de `*)`-arm, bij
naam. `--fast` is weg; de gate weigert argumenten (`usage: scripts/check-drools.sh (no arguments)`, exit 2).
`EXPECTED_PLAYED` en `EXPECTED_CHANNEL` staan waar en zoals ze stonden: de sed/awk van
`regelspraak-toolchain/scripts/check-service-replay.sh` leest uit `56a25404` en uit de tip dezelfde 279 en dezelfde 50
namen (md5 van het blok gelijk). Geen `EXPECTED_`-toewijzing in `scripts/` veranderd (`git diff 56a25404..` toont
alleen de ingesprongen regels van de suite).

**Gate 13, de benen.** De vier lussen staan in functies `leg_twist`, `leg_vocabulary`, `leg_origin`, `leg_name`, elk met
een eigen `CASES`-bestand (het gedeelde bestand zou tussen de benen botsen); ze draaien als `{ ( leg_x ) > $WORK/x.log
2>&1; echo $? > $WORK/x.rc; } &` en `wait`; elk been schrijft aan het eind zijn tellers en `leg_status` naar
`$WORK/x.counts`. Daarna `leg_report` per been in de volgorde van vroeger: de log naar stderr, bij status 2 (een twister
of stripper kon een golden niet lezen) `exit 2` zoals voorheen, bij een andere status of zonder tellingen `FAIL: the <leg>
leg ended with status … — its process crashed or was killed`; dan de vloercontroles van dat been op hun plaats. Per golden
binnen een been is niet geparallelliseerd (voorlopig besluit 2).

### (b) Het rapport vóór en na

Vóór: de vier gates na elkaar in een kopie op `56a25404` (`git worktree add --detach`, opgeruimd). Na: dezelfde vier
na elkaar in de worktree, op de code van `1a9c32a2`…`0dd426a5` (nog ongecommit tijdens de run, ongewijzigd
gecommit). Filter: `sed -e '$d' -e 's/[0-9][0-9]*s wall clock/Ns wall clock/g' -e 's/; [0-9][0-9]* s)$/; N s)/'` (de
sweeptijd van gate 12, de tijd in de PASS-regel van gate 17), dan `diff`. Per gate precies één regel verschil, de
processenregel:

```
gate 06: 1a2  > check-clingo: replays on 16 processes (one per core)
gate 12: 2a3  >   replay: 8 processes per pass (one per two cores)
gate 13: 2a3  >   legs: 4 processes (unit, vocabulary, origin, name)
gate 17: 2a3  >   traces on 16 processes (one per core), readings and replays on 8 (one per two cores)
```

Logs `gate{06,12,13,17}-{before,after}.log`.

### (c) Rood blijft rood (mutaties niet gecommit, in twee kopieën: `56a25404` en `22956095`)

Corpusmutatie: in `jutterswet-vangstklem/case.json` V1.bovengewicht verwacht 250.00 → 251.00; in
`date-with-parts/case2-jaareinde-en-eeuwjaar.json` het invoerattribuut `aanleguur` → `aanleguurX`. Elke gate vóór en na
rood met dezelfde faalregels; de diff is de processenregel (gate 12: alleen het pad van de kopie in een mvn-regel):

- 6: `FAIL: corpus/golden/date-with-parts/case2-jaareinde-en-eeuwjaar.json does not replay through clingo:` / `CASE
  ERROR: attribute "aanleguurX" is not declared on "Schuit"` en `FAIL: corpus/golden/jutterswet-vangstklem/case.json does
  not replay through clingo:` / `MISMATCH: V1.bovengewicht = [25000], expected 25100`.
- 13: `FAIL: corpus/golden/jutterswet-vangstklem/case.json — ref answers differently on the lying copy:` / `MISMATCH:
  V1.bovengewicht = 250, expected 251`, en dezelfde voor clingo.
- 17: `check-readings: FAIL — ref does not trace corpus/golden/date-with-parts/case2-jaareinde-en-eeuwjaar.json: CASE
  ERROR: attribute "aanleguurX" is not declared on "Schuit"` (vóór 67 s, na 74 s: na verzamelt eerst alles).
- 12: de suite speelt zelf elke golden, dus beide rood op de suite: `TmsCorpusWitnessTest` en `DrlTextCorpusWitnessTest`
  op `jutterswet-vangstklem`, `Tests run: 1702, Failures: 2`, `check-drools: FAIL — the backend's build+JUnit suite is
  red`. Om de replay zelf rood te maken een `java` vooraan op PATH (mvn neemt `JAVA_HOME`, de suite blijft groen):
  (c12a) de exec-replay van `jutterswet-vangstklem` antwoordt `FAIL: case.json: V1.bovengewicht verwacht 250.00,
  werkelijk 251.00` → vóór en na `check-drools: FAIL — 1 golden(s) neither play nor carry an exclusion reason:` /
  `jutterswet-vangstklem :: unexpected output: FAIL: case.json: …`; (c12b) de DRL-replay ervan antwoordt `PLAYED 2` → vóór
  en na `jutterswet-vangstklem :: exec-model 'PLAYED 1' | drl-text 'PLAYED 2'`.

Logs `gate*-c-{before,after}.log`, `gate12-java-c12{a,b}-{before,after}.log`; wrappers in
`werk/gates-design/parallel-replays/crash/`.

### (d) Een proces dat crasht of niet start (kopie op `22956095`)

- 6: `clingo_runner.exe` vervangen door een wrapper: case1 van `date-with-parts` schrijft `partial output` en sterft aan
  SIGSEGV, case2 start `/nonexistent/clingo_runner` → `FAIL: …/case1-gewenste-maand.json does not replay through clingo:
  partial output` en `FAIL: …/case2-… : … /nonexistent/clingo_runner: No such file or directory`, exit 1.
- 12: `java`-wrapper: de exec-replay van `jutterswet-vangstklem` sterft zonder uitvoer, die van `date-with-parts` start
  niet (127) → `check-drools: FAIL — 2 golden(s) neither play nor carry an exclusion reason:` / `date-with-parts ::
  unexpected output: ` / `jutterswet-vangstklem :: unexpected output: `, vóór de DRL-pas.
- 13: `java`-wrapper: de replay van de leugenkopie van `jutterswet-vangstklem` schrijft `PLAY` en sterft → `FAIL:
  jutterswet-vangstklem — Drools answers differently on the lying copy:` / `untouched: PLAYED 1` / `lying: PLAY`, en
  dezelfde voor de DRL-tak. Een been gedood (`kill -9` op het procesbeen van vocabulaire, tijdens de run) → `FAIL: the
  vocabulary leg ended with status 137 and without its counts — its process crashed or was killed`, exit 1.
- 17: `runner.exe`-wrapper: de trace van case1 van `date-with-parts` schrijft een regel op stderr en sterft →
  `check-readings: FAIL — ref does not trace corpus/golden/date-with-parts/case1-gewenste-maand.json: a line on stderr`.
  `java`-wrapper: de exec-readings van `ambiguous-fold-role-name` sterven zonder uitvoer → `check-readings: FAIL —
  ambiguous-fold-role-name: the two branches end their readings apart — exec: '', drl: 'READINGS 2'`.
- `parallel.sh` zelf (proef in de scratchpad), op macOS (`sh` = bash 3.2, BSD-xargs) en in `ubuntu:24.04` (dash,
  GNU-xargs 4.9.0): status 3, 139 (SIGSEGV), 127 (start niet), 255 (stopt de rest niet), de drie STDERR-standen,
  argumenten met spaties, aanhalingstekens, `$` en leeg; verkeerd gebruik exit 2; een taak waarvan de shell sterft (`kill
  -9 $PPID`) → `parallel: FAIL — the task 2 has no exit status after xargs ended with status 1` (BSD; GNU: 125), return 1.
  De gates zelf zijn niet op Linux gedraaid.

Bij een crash zet de shell van de taak er één regel vóór (`sh: line 3: <pid> Segmentation fault: 11 "$@" > …`), zoals
bash dat in de seriële lus van gate 17 ook deed bij een voorgrondproces dat aan een signaal sterft.

### (e) `--fast`

Weg uit `scripts/check-drools.sh` (vlag, kop, het commentaar bij de suite en het blok vóór de sweep) en uit `CLAUDE.md`
(gate 12). `git grep -e --fast` vindt daarna alleen de kaart, `backlog/done/` en `spec/PLAN.md:1215` (zie open punten).

### (a) Tijden, met load (`uptime`, 1-minuutsgemiddelde)

| gate | serieel, kaart (`210dfed2`) | vóór, alleen, `56a25404` | na, alleen | `make gates` `28598fb5` (vorige landing) | `make gates` op `22956095` |
|---|---|---|---|---|---|
| 6 | 216 s | 197 s (load 15,6 → 2,7) | 83 s (7,0 → 4,8) | 313 s | 239 s |
| 12 | 760 s | 498 s (6,1 → 9,2; sweep 225 s) | 181 s (10,2 → 18,0; sweep 34 s) | 589 s | 285 s (sweep 33 s) |
| 13 | 204 s | 205 s (2,7 → 5,6) | 77 s (4,8 → 5,2)¹ | 334 s | 221 s |
| 17 | 397 s | 387 s (5,6 → 6,1) | 64 s (5,2 → 10,2) | 528 s | 211 s |
| set | 1812 s | — | — | 595 s | **285 s** (4 min 45 s) |

¹ Tijdens gate 13 "na" draaide ik de unit-suite van gate 6 apart (45 s op één kern) om gate 6 te ontleden.
"Vóór" en "na alleen" liepen elk als enige gate-run (geen andere gate-set; `pgrep` vooraf leeg). `make gates` op
`22956095` (16:50:00–16:54:45): 17 van 17 groen plus `after-run`, `red=0`; load 5,6 bij de start, piek ±105 (16:51:30),
32,6 aan het eind; samenvatting `out/gates/20261025-165000/summary.txt` in de worktree, log `logs/make-gates.log`. De
commits daarna (`8dcdeeca`, dit Result) veranderen alleen commentaar en de kaart. In die run is gate 12 het kritieke pad:
de suite van `check-drools-begin` tot het laatste surefire-rapport 221 s, de exec-pas ±25 s, de DRL-pas 33 s. De
verwachting van de kaart (5–7 min) is gehaald.

### Voorlopige besluiten (niet uit kaart, advies of code te halen)

1. **D = 2 voor de JVM-lussen, niet cores/1,5.** 16 processen winnen solo nog 10–15 % op 8, maar in `make gates` lopen
   gates 12, 13 en 17 naast elkaar; 8 per gate vult de machine al, en het aantal is een eenvoudige deler.
2. **Gate 13 alleen per been.** Solo 77 s, in de set 221 s: klaar vóór gate 12 (285 s), dus per golden helpt niet
   meetbaar aan de set. De benen draaien met `&`/`wait` in het script, niet via `parallel.sh`: het zijn functies van het
   script, xargs start alleen programma's, en een heringang van het script met een beenargument zou een tweede manier
   zijn om de gate te draaien. Vier benen zijn vier processen, ook op een kleinere machine.
3. **Eén processenregel per rapport** ((d) vraagt hem); het is het enige verschil in (b).
4. **Gate 17: de kale replay als derde pas.** Welke goldens hem nodig hebben volgt uit de readings (EXCLUDE of ERROR, nu 47
   goldens); hem voor elke golden speculatief meedraaien kostte 329 JVM's extra.
5. **Een rode gate verzamelt eerst alles.** Rapport en eerste fout zijn die van vroeger, de kosten niet: gate 17 met een
   fout in een vroege golden 74 s tegen 67 s. Alleen gate 12 houdt een kostenvolgorde (exec vóór DRL), zoals de kaart vraagt.
6. **Gate 12 weigert argumenten** (exit 2), zodat een oude aanroep met `--fast` niet stil de volle gate draait.
7. **De suites van gates 6 en 12 lopen nog vóór hun replays**, niet ernaast: buiten de kaart (de dure aanroep per golden
   of case) en, voor gate 12, tegen het DESIGN-commentaar "the suite runs FIRST".
8. **`PARALLEL_JVM_FLAGS` staat in `parallel.sh`**, ook voor gate 13, dat de hulp alleen daarvoor sourcet: de vlaggen en
   het aantal processen rusten op één meting.

### Wat openblijft

- Het kritieke pad van de set is nu de JUnit-suite van gate 12 onder load (221 s van 285 s). Een volgende stap zou de
  exec-pas naast de suite verzamelen (de suite blijft eerst beoordeeld); dat verandert het DESIGN van gate 12 en is een
  eigen besluit.
- Gate 6 duurt in de set 239 s (alleen 83 s): de unit-suite van de clingo-backend draait 45 s op één kern vóór de
  replays; naast de replays zou gate 6 alleen ±45 s duren. Niet op het kritieke pad.
- `spec/PLAN.md:1215` noemt `--fast` (logboek van 2026-10-14; hoofdsessie). Voorstel voor een regel bij de landing:
  "On 2026-10-25 (card `parallel-gate-replays`) `--fast` went: gate 12 takes no arguments and runs in about three
  minutes on its own."
- De gates zelf liepen alleen op macOS; `parallel.sh` ook onder dash met GNU-xargs.
- De `make gates`-landingsrun van de hoofdsessie geeft de "na" voor de set op een rustige machine.
