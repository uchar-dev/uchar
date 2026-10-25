# gate-builds-when-stale: een gate bouwt alleen wat ouder is dan zijn bronnen, en `make gates` draait de set parallel

*Vastgelegd door de hoofdsessie op 2026-10-25, op besluit van de eigenaar na een Fable-advies (`werk/gates-design/`,
eindantwoord in de sessie; vraag in `fable-vraag.md`). Eerste van drie kaarten; daarna
`20261025-drools-suite-forks` (los hiervan, mag ervóór of ernaast) en `20261025-parallel-gate-replays` (na deze).
Raakt `scripts/` en `.github/`, beschermde paden: de kaarttak blijft een voorstel tot de eigenaar het reviewt.*

## Aanleiding

De gate-set duurt 29–32 minuten en draait na elkaar (`werk/gates.sh`, buiten de repository). Gates naast elkaar in
één checkout botsen, omdat elke gate zelf bouwt: twee `dune build` tegelijk in één `_build` falen op de lock
(`Error: Unexpected contents of build directory global lock file (_build/.lock)`, dune 3.24.2, gemeten 2026-10-25),
`make -C frontends/regelspraak compile` begint met `rm -rf out`, en gate 12 schrijft met een onvoorwaardelijke `mvn
package` de jar die gates 8, 11, 13, 15 en 17 lezen. Het bouwen zelf kost weinig (een tweede `dune build` 2 s, de
frontend 3 s, de jar-controle onder 1 s); het staat het parallel draaien in de weg.

De eigenaar: "We zijn op zoek naar de eenvoudigste oplossing, die snel is, elegant, geen POLA violation" — zoals make:
bouwen als en alleen als een bron nieuwer is dan het product. En: "ik wil geen opties introduceren van meerdere
manieren om de gates te runnen. parallel wat parallel kan, sequentieel wat sequentieel moet."

Goal: elke gate bouwt wat hij nodig heeft alleen als een bron nieuwer is dan het product, en roept anders geen
bouwgereedschap aan; `make gates` in de root bouwt eerst de drie bomen (OCaml, frontend, Drools-jar) en draait daarna
de 17 gates parallel in dezelfde checkout. Eén manier om de set te draaien (`make gates`), één manier om een gate te
draaien (zijn script). Wat een gate controleert, zijn lijsten, pins, vloeren en rapportregels veranderen niet.

- **OCaml.** `scripts/lib/ocaml-build.sh` met `ensure_ocaml`, naar het model van `scripts/lib/drools-jar.sh` (bronnen
  en mappen tellen mee, daarna een assert, een selftest). Dune is zelf al incrementeel; het probleem is de lock, die
  ook een aanroep zonder werk neemt. Daarom een stempel `_build/.gates-built`, aangeraakt na één geslaagde `dune build`
  van `ref backends spec/verifier frontends/catala frontends/flogic`; is geen bron nieuwer dan de stempel, dan geen
  dune-aanroep. De invariant is "dune slaagde na de jongste bronwijziging", niet "exe nieuwer dan bron" (de early
  cutoff van dune laat een exe legitiem ouder dan een bron). De elf dune-gates (3, 5, 6, 7, 8, 9, 11, 13, 14, 15, 17)
  roepen `ensure_ocaml` aan. Gates 3, 11 en 15 nemen de verifier uit de workspace-`_build` in plaats van een tweede
  boom (`--root spec/verifier`); gates 9 en 14 draaien hun suite-exe rechtstreeks met de argumenten van hun
  runtest-regel, zoals 5 en 6 hun exe draaien.
- **Frontend.** `frontends/regelspraak/Makefile` krijgt echte doelen (de gegenereerde parser uit `grammar/*.g4` en de
  ANTLR-jar; `out/` uit `src/main/java`, `src/main/resources` en de parser), zodat `compile` niets doet als niets
  nieuwer is. `rm -rf out` blijft in het recept en draait alleen bij een verouderde boom. Gates 10, 11, 15 en 16
  veranderen niet.
- **Drools-jar.** Gate 12 neemt de jar met `ensure_drools_jar`, zoals de vijf andere jar-gates sinds 2026-09-23, en
  draait de suite met `mvn -o -f backends/drools/pom.xml test` op dezelfde `target/`. Dat keert de DESIGN-keuze van
  2026-08-30 (één onvoorwaardelijke `mvn package`) om, op besluit van de eigenaar (2026-10-25), met de les van die dag
  gedekt: een replay op een jar ouder dan de bron vangt de controle van `drools-jar.sh` (bron of map nieuwer → herbouw,
  daarna jar ≥ jongste bron); de suite toetst de klassen waaruit de jar komt (`mvn test` compileert incrementeel in
  dezelfde `target/classes`, de bronset van `drools-jar.sh` omvat `src/test`); de suite telt zoals nu (boven nul, nul
  overgeslagen, vloer, de getuigen bij naam, het rapport nieuwer dan `suite_begin`). Het DESIGN-commentaar zegt dat.
  `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` blijven waar en zoals ze staan: de toolchain leest ze
  (`scripts/check-service-replay.sh`).
- **De runner.** Een `Makefile` in de root: `make gates` bouwt `ocaml`, `frontend` en `jar` (drie onafhankelijke bomen,
  tegelijk), daarna `$(MAKE) -k -j<cores>` over 17 gatedoelen; elk gatedoel roept het script ongewijzigd aan, met een
  log per gate onder `out/gates/<run>/` en één regel `<naam> rc=<n> <s>s` in een samenvatting, plus de regels van de
  bouwdoelen (`fresh`/`rebuilt`); exit ≠ 0 als er een rood is, en een gate zonder regel is rood. Het aantal cores uit
  de machine, zonder knop. Werkt met GNU make 3.81 (macOS) en nieuwer. Dat `make 12-drools` ook werkt, is een gevolg
  van de doelen, geen tweede interface; de kop van de Makefile zegt dat een gate alleen met zijn script draait.

Acceptance:
- (a) `make gates` groen op een gebouwde checkout en op een schone (`git clone` of `git worktree add`, zonder `_build`,
  `out/`, `target/`), met de wandkloktijden en per gate de tijd in het Result. Verwacht ±13 minuten (gate 12 is het
  langste pad).
- (b) Gelijktijdigheid: twee gates met de hand tegelijk op een gebouwde checkout (een dune-gate en een jar-gate, en twee
  dune-gates), beide groen, zonder dune-aanroep in hun log.
- (c) Per boom één bron aangeraakt (een `.ml`, een frontend-`.java`, een backend-`.java` en een testbron van de
  backend): precies die boom herbouwt bij de volgende `make gates`, de andere melden `fresh`.
- (d) De mutatie van 2026-08-30, rood eerst: na het bouwen van de jar een bron van de backend wijzigen zodat één test
  en één golden breken; gate 12 herbouwt en wordt rood op beide. En: een bron met een compileerfout laat de gate falen
  op de build, niet op een oude binary.
- (e) De selftest van `ocaml-build.sh` (zoals `drools-jar.sh --selftest`) groen.
- (f) `CLAUDE.md`: de kop van "Gate set" zegt dat de set met `make gates` draait (eerst de drie bomen, dan de gates
  parallel; logs onder `out/gates/`), dat een gate alleen met zijn script draait, en dat elke gate bouwt wat hij nodig
  heeft alleen als een bron nieuwer is; bij gate 12 de nieuwe bouw en suite; bij gates 3, 11, 15 de verifier uit de
  workspace-`_build`; `Makefile` bij de beschermde paden. `WAY-OF-WORKING.md` §6: "the full gate set before every
  landing" wordt "`make gates` before every landing". `.github/CODEOWNERS` claimt `/Makefile`.
  `.github/workflows/gates.yml` noemt `make gates` in zijn kop.
- (g) De toolchain blijft groen op deze commit (`make check-pin` en `make gates` in een worktree van
  `regelspraak-toolchain` met de pin op de kaarttip), want zij leest `scripts/check-drools.sh` en draait de suite van
  de backend in haar reactor.

May touch: `Makefile` (nieuw, root), `scripts/lib/ocaml-build.sh` (nieuw), `scripts/check-*.sh` (alleen hun bouw- en
binary-aanroepen, het DESIGN-commentaar van gate 12), `scripts/lib/drools-jar.sh` (alleen als gate 12 er iets van
vraagt), `frontends/regelspraak/Makefile`, `.gitignore`, `.github/CODEOWNERS`, `.github/workflows/gates.yml` (de kop),
`CLAUDE.md`, `WAY-OF-WORKING.md` §6, deze kaart.

Out of scope:
- Parallel binnen een gate (kaart `20261025-parallel-gate-replays`) en de forks van de suite (kaart
  `20261025-drools-suite-forks`).
- Wat een gate controleert, zijn lijsten, pins, vloeren en rapportregels; `--fast` van gate 12 (gaat bij kaart
  `parallel-gate-replays`).
- `werk/gates.sh` en `werk/gates-parallel.sh` staan buiten de repository; de hoofdsessie haalt ze weg bij de landing.

## Result (bouwer, 2026-10-25) — voorstel, niet geland

Raakt beschermde paden (`scripts/`, `.github/`, `CLAUDE.md`, `WAY-OF-WORKING.md`, de nieuwe root-`Makefile`): de tak
blijft een voorstel tot de eigenaar het reviewt. Acceptatie (g), de toolchain op deze commit, doet de hoofdsessie.

### Commits op `card/gate-builds-when-stale` (op `37434eeb`)

- `5757079d` dune alleen als een bron nieuwer is: `scripts/lib/ocaml-build.sh` (nieuw), de elf dune-gates.
- `111db646` echte doelen in `frontends/regelspraak/Makefile`.
- `b7c69dae` gate 12 neemt de jar met `ensure_drools_jar` en draait de suite met `mvn -o test`; kop van
  `scripts/lib/drools-jar.sh`; de commentaarzin over gate 12 in gates 8 en 11.
- `3da05f78` de root-`Makefile` (`make gates`), `CLAUDE.md`, `WAY-OF-WORKING.md` §6, `.github/CODEOWNERS`, de kop van
  `.github/workflows/gates.yml`.
- `a4ed905d` de zin van kaart `drools-suite-forks` in de alinea van gate 12 (opdracht hoofdsessie).
- dit Result.

### De drie bouwregels

**OCaml, `ensure_ocaml` (`scripts/lib/ocaml-build.sh`).** Bronset: `ref`, `backends/clingo`, `spec/verifier`,
`frontends/catala`, `frontends/flogic` en `dune-workspace`; bestanden en mappen (een verwijdering of hernoeming
verandert de mtime van de map); mappen die dune overslaat (`_*`, `.*`, zoals een oude `spec/verifier/_build`) slaat
de regel ook over. Stempel `_build/.gates-built`: geen pad van de bronset nieuwer dan de stempel en de stempel voor
dezelfde lijst projecten → geen dune-aanroep (`ocaml-build: fresh (…)`); anders één `dune build ref backends/clingo
spec/verifier frontends/catala frontends/flogic` (`ocaml-build: rebuilt (<waarom>; Ns)`). De stempel krijgt de tijd
van het begin van de build (een marker van vóór dune, na een groene build naar de stempel verplaatst) en bevat de
lijst projecten. Daarna de assert stempel ≥ jongste bron, zodat een bron die tijdens de build verandert of in de
toekomst gedateerd is de aanroep laat falen. Een rode build raakt de stempel niet. Selftest:
`sh scripts/lib/ocaml-build.sh --selftest`. De elf dune-gates (3, 5, 6, 7, 8, 9, 11, 13, 14, 15, 17) roepen
`ensure_ocaml` aan; gates 3, 11 en 15 nemen de verifier uit `_build/default/spec/verifier/`; gates 9 en 14 draaien
`_build/default/frontends/{catala,flogic}/test/suite.exe` rechtstreeks, in de bronmap van hun test, met de argv van
hun runtest-regel (dezelfde tellingen als voorheen: 241 en 150 checks). Gate 11 roept `ensure_ocaml` één keer aan
(stap 3c) en neemt ref en clingo daaruit.

**Frontend, `frontends/regelspraak/Makefile`.** `gen/.generated` hangt af van `grammar/*.g4` en de ANTLR-jar;
`out/.compiled` van `gen/.generated`, `src/main/java/**/*.java`, alle bestanden onder `src/main/resources`, alle
mappen onder `src/main/java` en `src/main/resources`, en de ANTLR-jar. `parser` en `compile` zijn no-ops als niets
nieuwer is; `rm -rf out` staat nog in het recept (de reden erbij) en draait alleen bij een verouderde boom. Ook hier
krijgt een stempel de tijd van het begin van zijn recept (`$@.begin`, aan het eind `mv`). `make test` compileert de
testklassen naar `out-test/` (eerst geleegd) en draait de launcher met `out:out-test` op het klassenpad. Gates 10,
11, 15 en 16 zijn ongewijzigd.

**Drools-jar, `ensure_drools_jar` (`scripts/lib/drools-jar.sh`, ongewijzigd op de kop na).** Bronset
`backends/drools/src`, `backends/drools/pom.xml`, `frontends/regelspraak/src/main`; product de shaded jar. Gate 12
neemt nu ook de jar daarmee en draait daarna `mvn -o -f backends/drools/pom.xml test` (alleen zonder `--fast`), met de
tellingen van nu: `Tests run` > 0, 0 overgeslagen, vloer 1573, `BranchDumpsCorpusWitnessTest` met beide rijen en de
drie getuigen van `fixpoint-readings` bij naam, elk rapport nieuwer dan `suite_begin` (dat nu ná de jar en vóór de
suite wordt aangeraakt). `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` staan waar en zoals ze stonden. Het
DESIGN-commentaar zegt hoe de drie flanken van 2026-08-30 gedekt blijven (advies §4).

### De vorm van `make gates`

`make gates` (ook `make` zonder doel) maakt `out/gates/<jjjjmmdd-uummss>/`, draait `$(MAKE) -k -j3 ocaml frontend jar`
en, als die drie groen zijn, `$(MAKE) -k -j<cores> <17 gatedoelen>`; het aantal cores uit `getconf
_NPROCESSORS_ONLN`, zonder knop. Elk doel draait zijn commando (dat van `gates.yml`: de dune-gates onder
`opam exec --`, sh of bash zoals het script vraagt) zonder de MAKEFLAGS van de root-make, schrijft
`out/gates/<run>/<doel>.log` en één regel `<doel> rc=<n> <s>s`, een bouwdoel met zijn statusregel erachter
(`fresh (…)`, `rebuilt (…)` of `FAIL — …`). `out/gates/<run>/summary.txt` heeft een `start`-regel (tijd, commit,
tak), de drie bouwregels, de 17 gateregels in de volgorde van `gates.yml` (een doel zonder regel: `<doel> did not
run`, rood), en `end <tijd> <s>s red=<n>`; de samenvatting gaat ook naar stdout. Exit 0 alleen als alles groen is.
GNU make 3.81 (`/usr/bin/make` op macOS) draait het.

### Tijden

Alle runs liepen op een machine die met andere sessies werd gedeeld (de toolchain, `drools-suite-forks` met acht
forks, de docs-tak); de load staat erbij. De definitieve meting vóór/na doet de hoofdsessie op een rustige machine.

| gate | serieel `5adb0386` (`werk/gates.sh`) | run 1, worktree | run 2, worktree (gate 1 rood gemaakt) | run 3, schoon |
|---|---|---|---|---|
| bouwstap ocaml / frontend / jar | (in de gates) | 0 / 0 / 0 s, fresh | 0 / 0 / 0 s, ocaml rebuilt | 4 / 4 / 7 s, alle drie rebuilt |
| 01-self-contained | 1 s | 2 s | 1 s (rood: de mutatie) | 1 s |
| 02-ir-schema | 25 s | 61 s | 65 s | 48 s |
| 04-case-schema | 2 s | 6 s | 8 s | 6 s |
| 03-verifier | 12 s | 28 s | 29 s | 28 s |
| 05-oracle | 10 s | 16 s | 17 s | 15 s |
| 06-clingo | 210 s | 337 s | 344 s | 251 s |
| 07-noop | 4 s | 9 s | 10 s | 9 s |
| 08-red | 36 s | 72 s | 75 s | 60 s |
| 09-catala | 3 s | 4 s | 5 s | 5 s |
| 10-regelspraak | 53 s | 108 s | 113 s | 84 s |
| 11-toka-parity | 49 s | 107 s | 111 s | 81 s |
| 12-drools | 733 s | 1271 s | 1147 s | 828 s |
| 13-reftype | 201 s | 360 s | 372 s | 273 s |
| 14-flogic | 1 s | 1 s | 2 s | 2 s |
| 15-graph | 14 s | 26 s | 28 s | 24 s |
| 16-scenario | 11 s | 21 s | 24 s | 18 s |
| 17-readings | 398 s | 609 s | 654 s | 463 s |
| **wandklok** | **1763 s (29 min 23 s)** | **1271 s (21 min 11 s)** | **1148 s (19 min 8 s)** | **835 s (13 min 55 s)** |
| load (`uptime`) | niet vastgelegd | 50–133 (tijdens de run) | 46–113 (tijdens de run) | 9,5 vóór, ±22 midden, 7,6 na |

Run 1 (13:28–13:49, de code van `3da05f78`, toen nog niet gecommit; load 50–133): alle bouwdoelen `fresh`, geen gate
bouwde (elf keer `ocaml-build: fresh`, zes keer `drools-jar: fresh` in de logs), alles groen, exit 0. Run 2 (13:52–14:12
op `3da05f78`, load 46–113): `ocaml rebuilt` na een aangeraakte `.ml`, gate 1 rood door een opzettelijke mutatie,
`red=1`, exit 2. Run 3 (14:33:52–14:47:47, een schone `git worktree add --detach` op `a4ed905d` met alleen de twee
tool-jars in `frontends/regelspraak/lib/`, zonder `_build`, `out/` en `target/`; geen andere gate-set liep): de drie
bouwdoelen `rebuilt` (4, 4 en 7 s), daarna in de gatelogs elf keer `ocaml-build: fresh` en zes keer `drools-jar:
fresh`, alles groen, exit 0; daarna opgeruimd met `git worktree remove`. Gate 12 is het kritieke pad (828 s, waarvan de
DRL-sweep 221 s). Logs: `werk/gates-design/builds-when-stale/logs/make-gates-{1,2,3-clean}.log`.

### Rode standen (recept en uitkomst; geen mutatie gecommit)

- **Gelijktijdigheid (b)**, op de gebouwde worktree: 05-oracle naast 15-graph (13:50:58, 9 s en 16 s) en 03-verifier
  naast 07-noop (13:51:14, 15 s en 6 s), alle vier PASS, in elk log alleen `ocaml-build: fresh` (en bij 15
  `drools-jar: fresh`), geen dune-aanroep. In run 1 en 2 liepen alle 17 naast elkaar zonder bouwen.
- **Per boom een bron aangeraakt (c).** Een `.ml` (`ref/lib/alloc.ml`, `touch`) vóór run 2: `ocaml rebuilt (1 source
  path(s) newer than the stamp, e.g. ref/lib/alloc.ml)`, frontend en jar `fresh`. De drie andere met de bouwstap van
  `make gates` (`make -k -j3 ocaml frontend jar`, hetzelfde recept en dezelfde regels): een frontend-`.java`
  (`resolver/Typing.java`) → frontend `rebuilt (6s)` én jar `rebuilt` (de bronset van de jar omvat
  `frontends/regelspraak/src/main`, bedoeld), ocaml `fresh`; een backend-`.java` (`runtime/RegelSpraakRuntime.java`) →
  alleen jar `rebuilt`; een testbron van de backend (`cases/TmsBuiltinWitnessTest.java`) → alleen jar `rebuilt`.
- **De mutatie van 2026-08-30 (d)**, na het bouwen van de jar: in `RegelSpraakRuntime.rsEerstePaasdag` `% 31) + 1` →
  `% 31) + 2`. `bash scripts/check-drools.sh`: `drools-jar: rebuilt (2 source path(s) newer than the jar …)`, dan de
  suite rood (`Tests run: 1702, Failures: 2, Errors: 7`; o.a.
  `TmsBuiltinWitnessTest` "verwacht 2026-04-05, werkelijk 2026-04-06" en de corpusgetuigen op
  `jutterswet-paasvondst`), `check-drools: FAIL — the backend's build+JUnit suite is red`. Daarna `--fast` op dezelfde
  bron: `drools-jar: fresh` (de jar van de vorige run, met de mutatie), de replay rood op de golden:
  `jutterswet-paasvondst :: ERROR … DateTimeException: Invalid value for DayOfMonth … 32`. Logs:
  `werk/gates-design/builds-when-stale/logs/d-mutation-gate12-{full,fast}.log`.
- **Een kapotte bron faalt op de build, niet op een oude binary.** OCaml: `let broken = = 1` achter `ref/lib/alloc.ml`
  → gate 5 `ocaml-build: FAIL — the dune build is red …` en `check-oracle: FAIL (build)`, de stempel ongewijzigd
  (13:52); `make gates` → `ocaml rc=1`, alle 17 gates `did not run`, `red=18`, exit 2. Frontend: `class Broken { int
  x = ; }` achter `Typing.java` → gate 16 `check-scenario: FAIL — the frontend does not build`, in `out/` geen
  `.compiled`; een tweede run faalt opnieuw op de build. Backend: dezelfde regel achter `RegelSpraakRuntime.java` →
  gate 12 `drools-jar: FAIL — the shaded-jar build is red …` en `check-drools: FAIL — no fresh Drools shaded jar`,
  gate 15 net zo; de oude jar (14:22) bleef in `target/` en geen van beide gebruikte hem. Na het terugzetten van de
  drie bronnen bouwde de eerstvolgende aanroep elke boom opnieuw (`ocaml-build: rebuilt (… e.g. ref/lib …)` in gate
  15, daarna in de bouwstap `frontend: rebuilt` en `drools-jar: rebuilt`).
- **De selftest van `ocaml-build.sh` (e)**: PASS, twaalf stappen (verouderde stempel → rebuilt; opnieuw → fresh; per
  project een `.ml`, `dune-workspace` en een bronmap → rebuilt; een stempel voor een andere lijst → rebuilt; een bron
  in de toekomst → FAIL). Rood eerst met vier mutanten van de lib (kopie in de scratchpad, gedraaid vanaf de
  worktree): flogic uit de bronset → stap 7 faalt; de assert weg → stap 11 faalt; de lijstcontrole weg → stap 10
  faalt; altijd bouwen → stap 2 faalt.
- **De tak zonder catala**: met een PATH zonder `catala` → `ocaml-build: rebuilt (the stamp is for '… frontends/catala
  …', the build is for 'ref backends/clingo spec/verifier frontends/flogic'; …; frontends/catala left out: no catala on
  PATH, gate 9 fails on its own)`, daarna `fresh`; terug met catala → `rebuilt` voor de volle lijst.

### Voorlopige besluiten (niet uit kaart of advies te halen)

1. **Catala buiten de build zonder catala op PATH.** Kaart en advies bouwen altijd vijf projecten. Dan eist elke
   dune-gate de catala-bibliotheken, en kan een machine zonder catala geen enkele OCaml-gate meer draaien, terwijl het
   besluit van 2026-08-24 catala alleen aan gate 9 bindt. `ensure_ocaml` laat `frontends/catala` weg als `catala` niet
   op PATH staat, zegt dat in de statusregel, en zet de lijst in de stempel, zodat catala dat later verschijnt bij de
   volgende aanroep gebouwd wordt. Gate 9 faalt zonder catala zoals altijd.
2. **De stempel krijgt de tijd van het begin van de build**, en de assert faalt als een bron tijdens de build
   veranderde. Strenger dan `drools-jar.sh` (die zo'n bewerking als vastgelegde grens laat passeren); goedkoop, en het
   sluit "dune slaagde na de jongste bronwijziging" letterlijk. Voor de frontend-stempels hetzelfde, met de
   hele-seconden-grens van make 3.81 erbij vermeld.
3. **Testklassen naar `out-test/`.** Met echte doelen leegt `compile` `out/` niet meer bij elke `make test`; testklassen
   in `out/` zouden blijven staan na het verwijderen van een test, en in `out/` liggen terwijl gates 11, 15 en 16 dat
   als klassenpad lezen. `out/` houdt nu precies de klassen waar `out/.compiled` voor staat.
4. **Bij een rode bouwstap draaien de gates niet** (elk `did not run`, rood). Gates op een rode boom zouden elk zelf
   gaan bouwen en op de dune-lock botsen.
5. **Commando's per gate zoals `gates.yml` en `werk/gates.sh`** (`opam exec --` voor de dune-gates), elk zonder de
   MAKEFLAGS van de root-make, zodat een script zich gedraagt als alleen.
6. **De samenvatting staat in de rundirectory** (`out/gates/<run>/summary.txt`, naast de logs; `out/` is al
   gitignored); `make` zonder doel is `make gates`.

### Voorgestelde tekst voor `CLAUDE.md` en `WAY-OF-WORKING.md` (in de tak gewijzigd)

- `CLAUDE.md`, Protected paths: "`scripts/` (gate scripts), `WAY-OF-WORKING.md`, and this file" → "`scripts/` (gate
  scripts), the root `Makefile` (`make gates`), `WAY-OF-WORKING.md`, and this file".
- `CLAUDE.md`, Gate set: een nieuwe alinea "**How the set runs (maintainer decision 2026-10-25, card
  `gate-builds-when-stale`).**" na "Where the gates run": `make gates` bouwt eerst de drie bomen naast elkaar, draait
  dan de gates parallel (`make -k -j<cores>`), logs onder `out/gates/<run>/`, samenvatting in de volgorde van
  `gates.yml`, exit ≠ 0 bij een rode bouwstap, een rode gate of een gate zonder regel; een gate alleen draait met
  zijn script; elke gate bouwt alleen als een bron nieuwer is (per boom genoemd).
- `CLAUDE.md`, gate 3: "builds" → "builds, in the workspace `_build` (`scripts/lib/ocaml-build.sh`), the verifier
  gates 11 and 15 run as well". Gate 11: "(the verifier from the workspace `_build`, as gate 3 builds it)". Gate 15:
  "(`verifier --dependency-edges` from the workspace `_build`, …)".
- `CLAUDE.md`, gate 12: "The gate rebuilds the jar and runs the backend's own JUnit suite, which must pass … (maintainer
  decision 2026-08-30)" → "The gate takes the jar fresh or rebuilt (`scripts/lib/drools-jar.sh`), as the other five
  jar gates do, and runs the backend's own JUnit suite with `mvn -o test` on the same `target/`, which must pass …
  (maintainer decisions 2026-08-30 and 2026-10-25)", gevolgd door de zin van kaart `drools-suite-forks` over
  `forkCount` 0.5C; `--fast` "rebuilds the jar without the JUnit suite" → "takes the jar fresh or rebuilt, does not
  run the JUnit suite".
- `WAY-OF-WORKING.md` §6: "and the full gate set before every landing" → "and `make gates` before every landing".
- `.github/CODEOWNERS`: `/Makefile @yhavinga`. `.github/workflows/gates.yml`, kop: lokaal draait de set met `make
  gates` (eerst de drie bouwstappen, dan de gates parallel; besluit 2026-10-25).

### Wat openblijft

- De tijden hierboven zijn onder load van andere sessies gemeten; de vergelijking vóór/na op een rustige machine doet
  de hoofdsessie bij de landing. Het kritieke pad is gate 12 (suite plus twee sweeps); de kaarten
  `drools-suite-forks` en `parallel-gate-replays` korten dat in.
- Gate 3 bouwt de verifier niet meer als eigen werkruimte (`--root spec/verifier`), zoals de kaart vraagt: een
  afhankelijkheid van `spec/verifier` op een andere bibliotheek van de werkruimte ziet gate 3 daardoor niet meer.
- De jobs per gate in `gates.yml` (alleen de kop is gewijzigd) installeren per job alleen wat die gate vroeger bouwde;
  elke dune-job bouwt nu de hele werkruimte en heeft dus ook zarith nodig (job 3 installeert alleen yojson). De
  workflow draait niet; bij een herleving moeten die stappen mee.
- `scripts/admit.sh` (buiten May touch) noemt in commentaar nog "the --root build some gates make" en valt terug op
  `spec/verifier/_build`; de werking klopt (hij bouwt zelf de werkruimte), het commentaar is verouderd.
- `make -n gates` draait de sub-makes (zij bevatten `$(MAKE)`) en de samenvattingslus schrijft dan naar een map die
  niet bestaat: cosmetisch, geen gevolg voor `make gates`.
- Acceptatie (g) (de toolchain met de pin op de kaarttip) en het weghalen van `werk/gates.sh` en
  `werk/gates-parallel.sh` doet de hoofdsessie.
