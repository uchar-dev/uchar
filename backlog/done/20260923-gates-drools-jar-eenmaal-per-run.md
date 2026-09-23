# gates: de Drools-jar één keer per run bouwen (gates 8, 11, 13 lezen versheid, gate 12 bouwt)

Goal: een volledige gate-run bouwt de Drools shaded jar nu vier keer
(gates 8 en 11: `mvn -o package` mét de 653 JUnit-tests; gate 12: idem,
by design de suite plus de jar; gate 13: `package -DskipTests`). Drie
van die vier builds zijn dubbel werk zonder extra zekerheid. Maintainer-
sein 2026-09-23 ("laten we efficient werken"). Na deze kaart bouwt
alleen gate 12 onvoorwaardelijk (zijn ontwerp: één `mvn package` draait
de suite én ververst de jar); gates 8, 11 en 13 eisen een VERSE jar en
bouwen alleen als hij ontbreekt of ouder is dan zijn bronnen — de
lens-bewezen stale-jar-flank ("a build-only-when-absent jar can silently
predate the source") blijft gesloten, want de toets is "ouder dan de
bron", niet "afwezig".

Acceptatie:
- (a) `scripts/lib/drools-jar.sh` met één functie `ensure_drools_jar`:
  eist `mvn`; eist precies één `backends/drools/target/uchar-drools-backend-*-shaded.jar`
  na afloop; bouwt met `mvn -o -q -f backends/drools/pom.xml package -DskipTests`
  als de jar ontbreekt of ouder is dan ENIG bestand onder
  `backends/drools/src/**`, `backends/drools/pom.xml` of
  `frontends/regelspraak/src/main/**` (de backend compileert de
  recordtypen van de frontend mee); drukt één regel af: `drools-jar:
  rebuilt (…)` of `drools-jar: fresh (jar newer than N source files)`;
  faalt luid met de mvn-staart als de build rood is. Na afloop een
  interne assertie: jar-mtime ≥ jongste bron-mtime, anders FAIL.
- (b) Gates 8, 11 en 13 roepen de functie aan in plaats van hun eigen
  `mvn package`; hun kopcommentaar en meldingen zeggen "fresh or rebuilt"
  in plaats van "unconditionally". Gate 12 ongewijzigd.
- (c) Zelftest in het script (rood-eerst getoond in het Result): een
  jar ouder maken dan een bron (`touch -t` op de jar) → `rebuilt`; direct
  daarna opnieuw → `fresh`; een bron aanraken → `rebuilt`.
- (d) Meting in het Result: wandkloktijd van een volledige run van gates
  8, 11, 12, 13 (na elkaar) vóór en na, op dezelfde boom.
- (e) Alle veertien gates groen; geen pin, vloer of lijst beweegt.

May touch: `scripts/lib/drools-jar.sh` (nieuw), `scripts/check-rood.sh`,
`scripts/check-toka-parity.sh`, `scripts/check-reftype-derivation.sh`,
kaartverplaatsing. NIET: `scripts/check-drools.sh`, `CLAUDE.md` (de
gate-beschrijvingen 8/11/13 zeggen "rebuilt unconditionally" — de
hoofdsessie werkt CLAUDE.md bij de landing bij), `.github/**`,
`spec/CONSTRUCTS.md`, `spec/PLAN.md`.

Out of scope: `dune build` (incrementeel, goedkoop); de JUnit-suite van
gate 12; CI-configuratie.

## Result (2026-09-23)

**Geland in `card/gates-drools-jar-eenmaal-per-run` (basis main
`16234880`), alle veertien gates groen.** Commits: `b291ba9b` (claim),
`e9b180b3` (`scripts/lib/drools-jar.sh`), `59eaad2a` (gates 8, 11 en 13
op de helper) en de commit die deze kaart naar `done/` zet. Niet
gepusht, niet gemerged. `git diff --stat 16234880` noemt alleen de lib,
de drie gate-scripts en deze kaart; `scripts/check-drools.sh`,
`CLAUDE.md`, `.github/**`, `spec/**` en `WAY-OF-WORKING.md` zijn niet
aangeraakt.

### Wat landde

**(a) `scripts/lib/drools-jar.sh`**, POSIX sh. Eén functie,
`ensure_drools_jar`, met een subshell als body, zodat een gate die het
bestand sourcet geen eigen variabele verliest (getoetst: `n`, `jars`,
`why` en `pom` van de aanroeper staan er na de aanroep nog). De functie

- eist `mvn`, ook als de jar vers is;
- eist precies één `backends/drools/target/uchar-drools-backend-*-shaded.jar`:
  bij twee faalt ze meteen (een build haalt er geen weg), en na een
  build telt ze opnieuw;
- bouwt met `mvn -o -q -f backends/drools/pom.xml package -DskipTests`
  als er geen jar is, of als `find backends/drools/src
  backends/drools/pom.xml frontends/regelspraak/src/main -newer <jar>`
  iets vindt;
- schrijft het jar-pad op stdout en precies één statusregel op stderr:
  `drools-jar: fresh (jar newer than N source files)` of
  `drools-jar: rebuilt (<reden>; <seconden>s)`;
- faalt bij een rode build met de laatste 25 regels mvn-uitvoer en een
  `drools-jar: FAIL — …`-regel;
- sluit af met de assertie dat dezelfde `find … -newer <jar>` leeg is.
  `-newer` is strikt, dus leeg betekent precies "jar-mtime ≥ jongste
  bron-mtime". Is hij niet leeg (bijvoorbeeld een bron met een datum in
  de toekomst), dan faalt de aanroep en noemt ze de jongere bron, in
  plaats van bij elke aanroep opnieuw te bouwen.

Eén punt gaat verder dan de letter van de kaart: behalve bestanden
tellen ook de MAPPEN van de bronset mee. Een verwijderde of hernoemde
bron laat geen nieuwer bestand achter, maar verzet wel de mtime van zijn
map; met alleen bestanden zou een jar die nog de klasse van een
verwijderde bron bevat als vers gelden. De N in de fresh-regel telt
bestanden (142 op deze boom, naast 38 mappen).

Portabiliteit: geen `stat`. Mtimes worden alleen met `find -newer`
vergeleken en alleen met `touch` gezet, beide POSIX; BSD (macOS) en GNU
(Linux-CI) nemen dus dezelfde weg, zonder aparte tak per platform.

**(b) Gates 8, 11 en 13** sourcen de lib en roepen `ensure_drools_jar`
aan in plaats van hun eigen `mvn package`. Hun kopcommentaar zegt "FRESH
OR REBUILT" en noemt de stale-jar-flank, die dicht blijft omdat de toets
"older than the source" is en niet "absent"; hun meldingen zeggen "fresh
or rebuilt". Per gate:

- gate 8 (`check-rood.sh`): de eigen `mvn`-controle van het Drools-been
  blijft (met nieuwe melding), daarna `elif ! JAR=$(ensure_drools_jar)`
  met een leg-lokale FAIL; de SKIPPED-melding zegt "the leg takes the
  jar fresh or rebuilt";
- gate 11 (`check-toka-parity.sh`): hetzelfde, via `fail`;
- gate 13 (`check-reftype-derivation.sh`): hetzelfde, met twee
  aanscherpingen die de helper meebrengt: precies één jar (was
  `ls | head -1`, de lexicografisch eerste) en de staart van een rode
  build (was `>/dev/null 2>&1`).

De JUnit-suite: gates 8 en 11 draaiden `mvn -o package` MET de suite
(653 tests). In die twee vervalt hij bewust, en hun commentaar zegt dat.
De suite draait in gate 12, dat ongewijzigd één onvoorwaardelijke
`mvn -o package` doet (suite plus jar). Gate 13 draaide hem al niet
(`-DskipTests`). Een volledige gate-run bouwt de jar nu één keer, in
gate 12, plus één keer in gate 8 als er sinds de laatste build een bron
is aangeraakt.

### Zelftest (c), rood-eerst

`sh scripts/lib/drools-jar.sh --selftest` (vanaf de repo-wortel) doet de
drie stappen van de kaart en raakt daarna elk ander deel van de bronset
één keer aan, zodat het weglaten van een deel op een benoemde stap
faalt. Elke stap eist status 0, precies één stderr-regel en een bestaand
jar-pad. Elke `touch` wacht eerst een seconde, zodat de stappen ook op
een bestandssysteem met hele-seconde-mtimes gelden.

Rood eerst: dezelfde zelftest tegen mutanten van de lib (kopie in /tmp,
één `sed`, gedraaid vanaf de worktree-wortel met echte mvn).

| mutant | wijziging | eerste rode stap |
|---|---|---|
| M1 | alleen bouwen als de jar ontbreekt (beide `find -newer` vinden niets): de lens-flank | 1: jar met `touch -t` verouderd, toch `fresh` |
| M2 | altijd bouwen: het gedrag vóór deze kaart | 2: direct opnieuw, toch `rebuilt` |
| M3 | alleen bestanden, geen mappen | 6: bronmap aangeraakt, toch `fresh` |
| M4 | zonder `frontends/regelspraak/src/main` | 3: frontendbron aangeraakt, toch `fresh` |
| M5 | zonder `backends/drools/pom.xml` | 5: pom aangeraakt, toch `fresh` |
| M6 | alleen de beslissing blind, de slotassertie intact | 0: de assertie faalt luid |

M1 letterlijk (exit 1):

```
drools-jar selftest 0/7 (the tree as found) -> drools-jar: fresh (jar newer than 142 source files)
drools-jar selftest 1/7 (jar made older than every source: touch -t 200001010000 backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar) -> drools-jar: fresh (jar newer than 142 source files)
drools-jar: fresh (jar newer than 142 source files)
drools-jar selftest: FAIL — step 1 (jar made older than every source: touch -t 200001010000 backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar): expected rebuilt with status 0, one status line and a jar path; got status 0 and the stderr above
```

M2, vanaf stap 2 (exit 1):

```
drools-jar selftest 2/7 (directly again) -> drools-jar: rebuilt (mutant: unconditional; 2s)
drools-jar: rebuilt (mutant: unconditional; 2s)
drools-jar selftest: FAIL — step 2 (directly again): expected fresh with status 0, one status line and a jar path; got status 0 and the stderr above
```

M6 laat zien dat de slotassertie een kapotte versheidsbeslissing al luid
maakt: `drools-jar: FAIL — backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar is still older than 1 source path(s), e.g. backends/drools/pom.xml: jar mtime < youngest source mtime (a source dated in the future?)`.

Groen, de echte lib op deze boom (exit 0, ongeveer 20 s):

```
drools-jar selftest 0/7 (the tree as found) -> drools-jar: fresh (jar newer than 142 source files)
drools-jar selftest 1/7 (jar made older than every source: touch -t 200001010000 backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar) -> drools-jar: rebuilt (180 source path(s) newer than the jar, e.g. backends/drools/pom.xml; 2s)
drools-jar selftest 2/7 (directly again) -> drools-jar: fresh (jar newer than 142 source files)
drools-jar selftest 3/7 (a frontend source touched: frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/emit/IrEmitter.java) -> drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/emit/IrEmitter.java; 2s)
drools-jar selftest 4/7 (a backend source touched: backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java) -> drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java; 5s)
drools-jar selftest 5/7 (the pom touched: backends/drools/pom.xml) -> drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. backends/drools/pom.xml; 3s)
drools-jar selftest 6/7 (a source directory touched, as a deletion or rename does: backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases) -> drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases; 3s)
drools-jar selftest 7/7 (directly again) -> drools-jar: fresh (jar newer than 142 source files)
drools-jar selftest: PASS (aged jar -> rebuilt; again -> fresh; a frontend source, a backend source, the pom and a source directory touched -> rebuilt each; again -> fresh)
```

### Meting (d)

Wandklok van gates 8, 11, 12 en 13 na elkaar, in seconden, op dezelfde
boom (de bronnen van `59eaad2a`) en vanuit dezelfde begintoestand (dune
warm, jar vers). "Vóór" draait de drie gate-scripts van `b291ba9b`
(gelijk aan main `16234880`), in de werkboom teruggezet; "na" draait
`59eaad2a`. Tijdens alle runs liep in `wt/m1` een andere gate-run; de
1-minuut-load op 16 kernen lag tussen 5,5 en 25,8. Paar 3 liep in
omgekeerde volgorde (eerst na, dan vóór).

| run | gate 8 | gate 11 | gate 12 | gate 13 | totaal | 8+11+13 |
|---|---|---|---|---|---|---|
| vóór 1 | 40,4 | 44,7 | 239,4 | 80,4 | 405,0 | 165,5 |
| na 1 | 24,1 | 34,0 | 238,9 | 68,4 | 365,4 | 126,5 |
| vóór 2 | 35,3 | 43,9 | 224,3 | 75,6 | 379,1 | 154,8 |
| na 2 | 60,8 | 35,0 | 222,5 | 70,7 | 388,9 | 166,5 |
| na 3 | 22,7 | 31,5 | 276,9 | 68,5 | 399,6 | 122,7 |
| vóór 3 | 37,0 | 48,7 | 253,0 | 79,0 | 417,7 | 164,7 |
| mediaan vóór | 37,0 | 44,7 | 239,4 | 79,0 | | |
| mediaan na | 24,1 | 34,0 | 238,9 | 68,5 | | |

- Per paar over het totaal: −39,6, +9,8 en −18,1 s; over de drie
  gewijzigde gates −39,0, +11,7 en −42,0 s. Per gate (mediaan) −12,9 s
  (gate 8), −10,7 s (11), −0,5 s (12, ongewijzigd) en −10,5 s (13); de
  som van de medianen gaat van 400,1 naar 365,5 s, −34,6 s (−8,6 %).
- De uitschieter is gate 8 in "na 2": 60,8 s bij een 1-minuut-load van
  19,5, terwijl die gate `drools-jar: fresh` meldde en dus niets bouwde.
  Gate 12 is niet gewijzigd en liep van 222,5 tot 276,9 s; die ruis is
  groter dan de besparing, dus alleen de medianen per gate en de drie
  gewijzigde gates samen zeggen hier iets.
- User-CPU van de vier gates samen per paar: 970,7 → 872,9, 939,4 →
  873,3 en 981,3 → 885,1 s (−98, −66 en −96 s).
- Wat verdween, los gemeten op deze machine: `mvn -o package` met de
  suite (653 tests) 16 s wandklok; `mvn -o -q package -DskipTests`
  zonder bronwijziging 2 s (koud, zonder `target/`, 5 s). De
  versheidscontrole zelf kost 0,07 à 0,08 s. Maven-aanroepen per run:
  vóór 4 (gates 8, 11 en 12 met de suite, gate 13 zonder), na 1 (gate
  12). In alle negen na-aanroepen van gates 8, 11 en 13 meldde de helper
  `fresh`.
- De uitvoer van de vier gates is in alle zes runs regel voor regel
  gelijk aan die van "vóór 1", op de jar-regel en de klokregels van de
  DRL-sweep na.
- Een zevende run, door een fout in mijn meetscript als "vóór"
  gelabeld maar met de nieuwe scripts, is tijdens gate 13 gestopt en
  staat niet in de tabel.

### Portabiliteit en foutwegen

- macOS (Darwin 24.6, BSD find, `/bin/sh` = bash 3.2 in POSIX-modus):
  de echte zelftest hierboven; daarnaast een proef met een stub-`mvn` en
  een synthetische boom onder `/bin/sh`, `/bin/dash` en `/bin/bash`.
- Linux: dezelfde stub-proef in docker op `debian:bookworm-slim`
  (`/bin/sh` is dash 0.5.12, GNU findutils 4.9.0, GNU coreutils 9.1):
  zelftest groen onder dash en onder bash. De echte Maven-build is op
  Linux niet gedraaid; dat deel is één `mvn`-aanroep zonder
  platformafhankelijke vlaggen.
- Foutwegen, op beide platforms met de stub, elk met exit 1 en een eigen
  `drools-jar: FAIL`-regel: een rode build (de mvn-staart, dan
  `drools-jar: FAIL — the shaded-jar build is red (…) — run: mvn -o -f
  backends/drools/pom.xml package -DskipTests`), twee shaded jars, een
  bron met een datum in de toekomst (de slotassertie), geen `mvn` op het
  PATH, en een aanroep buiten de repo-wortel. Verder: een verwijderde
  bron geeft `rebuilt`, een bron met dezelfde mtime als de jar geeft
  `fresh`, en het bestand zonder `--selftest` draaien geeft de usage met
  exit 2.
- Op gate-niveau, met een kopie van de jar als
  `uchar-drools-backend-9-SNAPSHOT-shaded.jar` in `target/`: gates 8, 11
  en 13 falen elk met exit 1, eerst met de helperregel, dan met hun
  eigen: `check-rood: FAIL — Drools leg: no fresh shaded jar (the
  drools-jar line above says why)`, `check-toka-parity: FAIL — no fresh
  Drools shaded jar (the drools-jar line above says why)` en
  `check-reftype-derivation: FAIL (no fresh Drools backend jar — the
  drools-jar line above says why)`. De kopie is daarna verwijderd.

### Veertien gates (e)

Elk als losse aanroep, in volgorde, na `eval $(opam env)`. Vooraf is één
backendbron aangeraakt (alleen de mtime), zodat de run ook de herbouwtak
doorloopt: gate 8 meldde `drools-jar: rebuilt (1 source path(s) newer
than the jar, e.g.
backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java;
4s)`, gates 11 en 13 `drools-jar: fresh (jar newer than 142 source
files)`. Het diff bevat geen pin-, vloer- of lijstregel
(`git diff 16234880 -- scripts/` heeft geen `EXPECTED_`-, holdout-,
exclude-, channel- of survivor-regel), en de tellingen van gates 8, 11,
12 en 13 zijn gelijk aan die van vóór (zie Meting).

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 26 pattern self-test row(s); 285 file(s) validate and round-trip, floor 192)
check-verifier: PASS (285 golden clean; 454 negatief rejected for their declared rule, floor 350)
check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 656 case file(s) validate)
check-oracle: PASS (ref suite 931 worked examples, floor 489; 608 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 523 checks, floor 83; 608 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (571 cases refute a do-nothing executor; 37 pinned survivors pin defeasible standing)
check-rood: Drools leg PASS (11 fault-pinned, 37 not-carried with reason)
check-rood: PASS (48 red cases fault observably in ref and clingo, floor 26; 96 pinned fragments cite none of the 36 listed bottom sentences)
check-catala: PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
check-regelspraak: PASS — JUnit suite green (492 tests, floor 173); 70 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS
  re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: [twee bestandsnamen weggelaten]); TOKA suite 27 tests, floor 27
  schema: 29 replay slice(s) metaschema-valid + byte-round-trip (gate-2 discipline, explicit mode)
  differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences
    ref    : 29 PASS / 0 diverge  (the 100% oracle carries every toka case)
    clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge
    Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR
check-drools: PASS
  played 184  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
  auto-excluded 50  (per-form refusals: loader / transpiler marker / contract / unsupported input)
  value-divergence 1  (listed with reason — the exec-model differs from ref/clingo)
  no-case 2  (goldens without a case file)
  channel floor 53 non-PLAYED goldens pinned by name; played matches the pinned 184
  DRL-text branch played 163  (its OWN count — the narrower branch, not the square; pin 163)
  DRL-text floor 74 non-PLAYED goldens pinned by name; sweep cost 120s wall clock
  JUnit 653 tests, floor 239  (the backend's own suite, green with a non-zero count)
check-reftype-derivation: PASS (131 unit annotations twisted over 27 goldens, floor 74; 128 executor replays reproduce the untouched outcome, 25 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
check-flogic: PASS — frontend suite green (130 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
```

Bij gate 12 staat het PASS-blok hier zonder de uitsplitsing per
weigeringsvorm. Bij gate 11 zijn de twee holdout-bestandsnamen
weggelaten, omdat deze kaart niet uit `toka/` citeert; verder is alles
letterlijk.

### Open

1. **CLAUDE.md (hoofdsessie, voorstel).** De kaart zegt dat de
   gate-beschrijvingen 8, 11 en 13 "rebuilt unconditionally" zeggen. Op
   `16234880` noemen die drie beschrijvingen de jar-bouw niet;
   `grep -n -i 'unconditional\|rebuil' CLAUDE.md` vindt alleen gate 12
   (r. 246: "The gate rebuilds the jar and runs the backend's own JUnit
   suite"), en dat blijft waar. Er is dus niets te corrigeren. Wie de
   nieuwe verdeling wil vastleggen, kan na die zin van gate 12 invoegen:
   "Gates 8, 11 and 13 take the jar fresh or rebuilt through
   `scripts/lib/drools-jar.sh` — rebuilt when it is absent or older than
   any path under `backends/drools/src`, `backends/drools/pom.xml` or
   `frontends/regelspraak/src/main` — so a full run builds it once,
   here, unless a source changed since the last build."
2. **CI, geen actie in deze kaart.** De jobs van gates 11 en 13 bouwen
   de jar in een eigen workflowstap vóór de gate (`mvn -B -f
   backends/drools/pom.xml package`, `gates.yml` r. 217 en 268, met de
   suite). Met de helper leest de gate die jar als vers, dus per job valt
   een build weg zonder dat `gates.yml` verandert; de workflowstap zelf
   draait de suite nog. De gate-8-job heeft geen Maven-stap en geen
   `~/.m2`-cache; dat stond al in het commentaar van gate 8 en verandert
   niet: zonder jar bouwt de helper offline, en zonder warme `~/.m2` is
   dat rood.
3. **Grenzen van de toets, bewust en in de kop van de lib vermeld.**
   Alleen mtimes tellen. Als vers gaan door: een bron met een
   teruggezette mtime (`touch -t`, `cp -p`), een wijziging die wordt
   opgeslagen terwijl mvn loopt, en op een bestandssysteem met
   hele-seconde-mtimes een wijziging in dezelfde seconde als de build.
   `~/.m2` valt buiten de set; de pom pint elke versie.
4. **Ruimer dan nodig, bewust (kaart).** De set omvat ook
   `backends/drools/src/test/**` en de frontendpakketten die de pom
   uitsluit (`resolver`, `emit`). Een wijziging daar kost een overbodige
   build van 2 à 5 s, nooit een oude jar. De zelftest raakt zo'n bestand
   aan (`emit/IrEmitter.java`, het eerste in sorteervolgorde); hij toetst
   de set van de kaart, niet de compileerset van de pom.
5. **De zelftest wijzigt mtimes.** Hij zet de mtime van drie bestanden
   en één map op "nu" (inhoud ongewijzigd, `git status` blijft schoon)
   en laat een verse jar achter.
