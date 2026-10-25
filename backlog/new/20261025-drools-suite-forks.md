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
