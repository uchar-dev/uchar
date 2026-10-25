# drools-suite-forks: de JUnit-suite van de Drools-backend draait in meer forks

*Vastgelegd door de hoofdsessie op 2026-10-25, op besluit van de eigenaar na een Fable-advies (`werk/gates-design/`).
Tweede van drie kaarten (zie `20261025-gate-builds-when-stale`); staat los van de andere twee en mag ervóór of
ernaast.*

## Aanleiding

De suite van `backends/drools` (1699 tests in 248 klassen) draait in gate 12 in één JVM en kost ±356 s van de 760 s
van die gate. De langste klasse is `TmsCreationStratumWitnessTest` (64 s), daarna 35, 28, 20, 18, 18, 17 en 16 s
(`target/surefire-reports`, 2026-10-25). Volgens het advies schrijven de tests alleen in eigen tijdelijke mappen,
dumps alleen onder een expliciete property, zonder vaste poorten; statische toestand is per JVM.

Goal: surefire draait de suite in `forkCount` 0.5C met `reuseForks` true (8 forks op een machine met 16 cores, 1 op 2
cores), met dezelfde tests, dezelfde telling en dezelfde uitkomsten. Geen knop: het aantal volgt uit de machine.

Acceptance:
- (a) Drie volledige runs van de suite (`mvn -o -f backends/drools/pom.xml test`) met forks: elk dezelfde telling als
  zonder, nul fouten, nul overgeslagen, `BranchDumpsCorpusWitnessTest` met beide rijen; de wandkloktijd vóór en na in
  het Result (verwacht 70–100 s).
- (b) Gate 12 groen met zijn vloer en de getuigen bij naam; zijn tijd vóór en na.
- (c) Een test die toch toestand deelt (een volgorde-afhankelijkheid die pas in een fork zichtbaar wordt) wordt
  gerepareerd in die test, niet uitgesloten; het Result noemt hem.
- (d) De toolchain blijft groen (`mvn -B -o verify` in een worktree van `regelspraak-toolchain` met de pin op de
  kaarttip): haar reactor draait deze suite en erft de forks.
- (e) Eén zin bij gate 12 in `CLAUDE.md` (beschermd: de maintainer landt hem); de volledige gate-set groen.

May touch: `backends/drools/pom.xml`, testklassen van `backends/drools` die toestand blijken te delen, `CLAUDE.md` (de
zin bij gate 12), deze kaart.

Out of scope:
- `TmsCreationStratumWitnessTest` splitsen (de ondergrens van de suite); de replays en de gates zelf.

## Result (2026-10-25)

Tak `card/drools-suite-forks`, vanaf main `321fb239`, de kaart naar `doing/` in `099e96e7`.

- `96043b20` surefire in `backends/drools/pom.xml`: `<forkCount>0.5C</forkCount>` en `<reuseForks>true</reuseForks>`,
  met een commentaar dat zegt waarom en wat een testklasse ervoor nodig heeft.
- `602874da` het commentaar noemt de gemeten 1,4 core per JVM als reden voor de helft van de cores (alleen
  commentaar).
- dit Result, in het commit daarna.

May touch gevolgd: alleen `backends/drools/pom.xml` en deze kaart. Geen testklasse gewijzigd, geen beschermd pad,
geen delta voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`.

### De instelling en waarom

`forkCount` 0.5C geeft op deze machine (16 cores) 8 JVM's, op een runner met 2 cores 1. Met `reuseForks` neemt elke
JVM de volgende testklasse uit één wachtrij tot die leeg is; de suite duurt dan ongeveer zo lang als
`TmsCreationStratumWitnessTest` of haar totaal gedeeld door 8, wat het langst is. De helft van de cores en niet alle:
één JVM van de suite houdt gemiddeld 1,4 core bezig (de run zonder forks hieronder: user+sys 697 s over 489 s
wandklok). Er is geen knop.

De systeemproperty `uchar.messages.locale=nl` en de omgeving (`JAVA_TOOL_OPTIONS`, voor de kind-JVM van
`SeamWitnessTest`) gelden in elke fork: in elke run met forks draagt elk van de 248 surefire-rapporten
`<property name="uchar.messages.locale" value="nl"/>`, de rapporten noemen 8 verschillende booter-jars (8 forks), en
de log heeft 8 keer `Picked up JAVA_TOOL_OPTIONS` (zonder forks 1 keer).

### Metingen

Elke run: `mvn -o -f backends/drools/pom.xml test` (gate 12: `bash scripts/check-drools.sh`), wandklok van start tot
einde, load (`uptime`, 1-minuutgemiddelde) bij start en einde. Tijdens alle metingen draaiden andere sessies gates
(de load liep op tot 125), dus de tijden zijn onder wisselende load gemeten; de paren zijn zo dicht mogelijk bij
elkaar genomen.

| run | commit | tijd | load start → einde |
|---|---|---|---|
| suite zonder forks | `099e96e7` | 366 s | 10,8 → 6,7 |
| gate 12 zonder forks | `099e96e7` | 976 s, PASS | 5,6 → 17,4 (piek 55) |
| suite met forks, run 1 | `96043b20` | 153 s | 15,9 → 73,8 |
| suite met forks, run 2 | `96043b20` | 151 s | 48,7 → 64,7 |
| suite met forks, run 3 | `96043b20` | 146 s | 55,3 → 49,4 |
| gate 12 met forks | `96043b20` | 742 s, PASS | 33,7 → 30,6 (piek 125) |
| suite met forks, klassen in willekeurige volgorde (`-Dsurefire.runOrder=random`) | `96043b20` | 167 s | 23,5 → 65,4 |
| paar: suite zonder forks (de pom van `099e96e7` naast de nieuwe) | `96043b20` | 489 s | 51,0 → 29,5 |
| paar: suite met forks, direct erna | `96043b20` | 169 s | 29,5 → 53,3 |

Elke run, met en zonder forks: 1702 tests in 248 klassen, 0 fouten, 0 mislukt, 0 overgeslagen; de lijst van
`klasse#test` uit de surefire-rapporten is in elke run met forks gelijk aan die van de run zonder forks (0 regels
diff); `BranchDumpsCorpusWitnessTest` met beide rijen (`every_case_of_every_played_golden_has_the_same_dump_on_both_branches`,
`every_red_case_gives_the_same_red_result_on_both_branches`). Gate 12 met forks: `JUnit 1702 tests, floor 1573`,
played 279, DRL-tekst 279, de vier getuigen bij naam gevonden.

De verwachting van de kaart (70–100 s) is onder deze load niet gemeten: `TmsCreationStratumWitnessTest` kost in de
runs met forks 126 s, tegen 66 s in de rustige run zonder forks. Het paar onder vergelijkbare load geeft 489 → 169 s,
een factor 2,9. Gate 12 kost met forks 234 s minder (976 → 742 s), terwijl zijn DRL-sweep in die run onder hogere load
74 s langer duurde (317 → 391 s). De logs staan in `werk/gates-design/suite-forks/logs/`.

### Gerepareerde tests

Geen. Drie runs met forks, een run met de klassen in willekeurige volgorde en de run van het paar gaven elk dezelfde
1702 tests groen. De tests schrijven alleen in eigen tijdelijke bestanden en mappen (`Files.createTempFile`,
`Files.createTempDirectory`); de enige test die een systeemproperty zet (`BackendMessageLanguageTest`) zet hem na elke
test terug; de niet-finale statics (`TmsIndexWitnessTest.patternConstraints`, `Accessors.lmfFallbacks`) en de caches
van de backend vult elke klasse zelf.

### Voorgestelde zin voor `CLAUDE.md`, gate 12

Achter "The gate rebuilds the jar and runs the backend's own JUnit suite, which must pass with a non-zero, zero-skip
test count (maintainer decision 2026-08-30).":

> Since 2026-10-25 (card `drools-suite-forks`) surefire runs that suite in `forkCount` 0.5C JVMs with `reuseForks`
> (`backends/drools/pom.xml`; 8 on 16 cores, 1 on 2), which take the test classes from one queue: the count, the
> floor and the witnesses by name are those of a run in one JVM, the system property and the environment of the pom
> hold in every fork, and a test class relies on no state another class leaves in its JVM.

Acceptatie (d), de toolchain, vervalt (opdracht van de hoofdsessie): de toolchain draait deze suite straks niet meer
zelf (toolchain-kaart `gates-own-tests-in-parallel`). De volledige gate-set draait de hoofdsessie bij de landing.

### Landing (hoofdsessie)

- De volledige gate-set (`werk/gates.sh`) op `eba21fbe` (herbaseerd op main `8b5664c1`): 17 van 17 groen in 25
  minuten; gate 12 in 493 s (op `5adb0386` zonder forks 733 s), 13 in 200 s, 17 in 385 s. Samenvatting
  `werk/gates-design/suite-forks/gates-1.summary.txt`. Geland op `main` fast-forward.
- De voorgestelde zin voor `CLAUDE.md` bij gate 12 staat in de `CLAUDE.md`-tekst van kaart
  `gate-builds-when-stale`, die de alinea van gate 12 herschrijft en na deze kaart landt.
