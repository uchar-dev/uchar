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

### Commits op `card/gate-builds-when-stale` (herbaseerd op `8b5664c1`)

- `c7d530d2` dune alleen als een bron nieuwer is: `scripts/lib/ocaml-build.sh` (nieuw), de elf dune-gates.
- `ae547727` echte doelen in `frontends/regelspraak/Makefile`.
- `2d613d5a` gate 12 neemt de jar met `ensure_drools_jar` en draait de suite met `mvn -o test`; kop van
  `scripts/lib/drools-jar.sh`; de commentaarzin over gate 12 in gates 8 en 11.
- `bdd1fe47` de root-`Makefile` (`make gates`), `CLAUDE.md`, `WAY-OF-WORKING.md` §6, `.github/CODEOWNERS`, de kop van
  `.github/workflows/gates.yml`.
- `439a147a` de zin van kaart `drools-suite-forks` in de alinea van gate 12 (opdracht hoofdsessie).
- `d88e8480` dit Result.
- De commits van de fixronde staan onder "Review (fixronde)". De hashes en runs hieronder die `3da05f78` of `a4ed905d`
  noemen, zijn die van vóór de herbasering (`3da05f78` = `bdd1fe47`, `a4ed905d` = `439a147a`).

### De drie bouwregels

**OCaml, `ensure_ocaml` (`scripts/lib/ocaml-build.sh`).** Bronset: `ref`, `backends/clingo`, `spec/verifier`,
`frontends/catala`, `frontends/flogic` en `dune-workspace`; bestanden en mappen (een verwijdering of hernoeming
verandert de mtime van de map); mappen die dune overslaat (`_*`, `.*`, zoals een oude `spec/verifier/_build`) slaat
de regel ook over. Stempel `_build/.gates-built`: geen pad van de bronset nieuwer dan de stempel en de stempel voor
dezelfde lijst projecten → geen dune-aanroep (`ocaml-build: fresh (…)`); anders één `dune build ref backends/clingo
spec/verifier frontends/catala frontends/flogic` (`ocaml-build: rebuilt (<waarom>; Ns)`). De stempel krijgt de tijd
van het begin van de build: een marker, vóór dune geschreven met de lijst projecten erin, na een groene build met `mv`
naar de stempel verplaatst. Daarna de assert stempel ≥ jongste bron, zodat een bron die tijdens de build verandert of in de
toekomst gedateerd is de aanroep laat falen. Een rode build raakt de stempel niet. Selftest:
`sh scripts/lib/ocaml-build.sh --selftest`. De elf dune-gates (3, 5, 6, 7, 8, 9, 11, 13, 14, 15, 17) roepen
`ensure_ocaml` aan; gates 3, 11 en 15 nemen de verifier uit `_build/default/spec/verifier/`; gates 9 en 14 draaien
hun suite met `run_suite` (ook in `ocaml-build.sh`): de exe uit `_build/default/frontends/{catala,flogic}/test/`, in de
bronmap van hun test, met de argv die `run_suite` uit hun runtest-regel leest (dezelfde tellingen als voorheen: 241 en
150 checks). `scripts/admit.sh` bouwt ook met `ensure_ocaml`. Gate 11 roept `ensure_ocaml` één keer aan
(stap 3c) en neemt ref en clingo daaruit.

**Frontend, `frontends/regelspraak/Makefile`.** `gen/.generated` hangt af van `grammar/*.g4`, de ANTLR-jar en de
Makefile zelf; `out/.compiled` van `gen/.generated`, `src/main/java/**/*.java`, alle bestanden onder
`src/main/resources`, alle mappen onder `src/main/java` en `src/main/resources`, de ANTLR-jar en de Makefile zelf. `parser` en `compile` zijn no-ops als niets
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

`make gates` (een kale `make` toont alleen hoe de set draait) vergelijkt eerst de gate-scripts van zijn doelen met
`gates.yml` (ook de volgorde) en met de lijst in `CLAUDE.md` en stopt met de naam van het script als ze verschillen;
dan maakt hij `out/gates/<jjjjmmdd-uummss>/`, draait `$(MAKE) -k -j3 ocaml frontend jar` en, als die drie groen zijn,
`$(MAKE) -k -j<cores> <17 gatedoelen>`, en daarna de stap `after-run` (de drie bouwregels opnieuw, elk `fresh`, en geen
product nieuwer dan het eind van de bouwstap); het aantal cores uit `getconf _NPROCESSORS_ONLN`, met `override`, zodat
een opdrachtregel het niet zet. Een doel zonder commando laat make stoppen bij het lezen van de Makefile. Elk doel draait zijn commando (dat van `gates.yml`: de dune-gates onder
`opam exec --`, sh of bash zoals het script vraagt) zonder de MAKEFLAGS van de root-make, schrijft
`out/gates/<run>/<doel>.log` en één regel `<doel> rc=<n> <s>s`, een bouwdoel met zijn statusregel erachter
(`fresh (…)`, `rebuilt (…)` of `FAIL — …`). `out/gates/<run>/summary.txt` heeft een `start`-regel (tijd, commit,
tak), de drie bouwregels, de 17 gateregels in de volgorde van `gates.yml` (een doel zonder regel: `<doel> did not
run`, rood), de regel `after-run rc=<n> <s>s <bevinding>`, en `end <tijd> <s>s red=<n>`; de samenvatting gaat ook naar
stdout. Een regel is groen als hij met `<doel> rc=0` begint; exit 0 alleen als alles groen is.
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
  (13:52); `make gates` → `ocaml rc=1`, alle 17 gates `did not run`, `red=18`, exit 2 (met `after-run`, die bij een
  rode bouwstap ook niet draait, nu `red=19`). Frontend: `class Broken { int
  x = ; }` achter `Typing.java` → gate 16 `check-scenario: FAIL — the frontend does not build`, in `out/` geen
  `.compiled`; een tweede run faalt opnieuw op de build. Backend: dezelfde regel achter `RegelSpraakRuntime.java` →
  gate 12 `drools-jar: FAIL — the shaded-jar build is red …` en `check-drools: FAIL — no fresh Drools shaded jar`,
  gate 15 net zo; de oude jar (14:22) bleef in `target/` en geen van beide gebruikte hem. Na het terugzetten van de
  drie bronnen bouwde de eerstvolgende aanroep elke boom opnieuw (`ocaml-build: rebuilt (… e.g. ref/lib …)` in gate
  15, daarna in de bouwstap `frontend: rebuilt` en `drools-jar: rebuilt`).
- **De selftest van `ocaml-build.sh` (e)**: PASS, dertien stappen (verouderde stempel → rebuilt; opnieuw → fresh; per
  project een `.ml`, `dune-workspace` en een bronmap → rebuilt; een stempel voor een andere lijst → rebuilt; een bron
  bewaard terwijl dune draait → FAIL, sinds de fixronde; een bron in de toekomst → FAIL). Rood eerst met vier mutanten van de lib (kopie in de scratchpad, gedraaid vanaf de
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
   gitignored); `make` zonder doel toont hoe de set draait (review punt 10).

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
- Fixronde: `CLAUDE.md`, "How the set runs": de vergelijking van de gatelijsten vooraf, de twee voorwaarden van "no two
  gates write the same file" en de stap `after-run`; gate 3: "and builds on its own: no dune file under
  `spec/verifier` names a library another directory of the workspace defines …". `WAY-OF-WORKING.md` §4: een nieuwe
  gate krijgt een doel in de root-`Makefile`, een job in `gates.yml` en een regel in de lijst van `CLAUDE.md`.
- `.github/CODEOWNERS`: `/Makefile @yhavinga`. `.github/workflows/gates.yml`, kop: lokaal draait de set met `make
  gates` (eerst de drie bouwstappen, dan de gates parallel; besluit 2026-10-25).

### Wat openblijft

- De tijden hierboven zijn onder load van andere sessies gemeten; de vergelijking vóór/na op een rustige machine doet
  de hoofdsessie bij de landing. Het kritieke pad is gate 12 (suite plus twee sweeps); de kaarten
  `drools-suite-forks` en `parallel-gate-replays` korten dat in.
- `scripts/admit.sh` draaide in stap 6 de set als een lus over `scripts/check-*.sh`: een tweede manier om de set te
  draaien. De hoofdsessie liet stap 6 `make gates` draaien (commit na `d60ebfb6`).
- `make -n gates` draait de sub-makes (zij bevatten `$(MAKE)`) en de samenvattingslus schrijft dan naar een map die
  niet bestaat: cosmetisch, geen gevolg voor `make gates`.
- Acceptatie (g) (de toolchain met de pin op de kaarttip) en het weghalen van `werk/gates.sh` en
  `werk/gates-parallel.sh` doet de hoofdsessie.

## Review (fixronde, 2026-10-25)

De review (Opus, eindoordeel "eerst fixen") vond 13 punten; alle 13 zijn verwerkt, geen afgewezen. Proeven in een
kopie (`git worktree add --detach` onder `werk/gates-design/builds-when-stale/fix/`, daarna `git worktree remove`) of
op de tak; logs onder `werk/gates-design/builds-when-stale/fix/logs/`, de stub-proeven van de root-Makefile met
`fix/tools/stub.py` en `fix/tools/makefile-proofs.sh` (log `p2-3-6-10-makefile.log`). "Vóór" is de Makefile van
`d88e8480`, de tip vóór de fixronde. Geen volledige `make gates`: die draait de hoofdsessie bij de landing. Op de tak
gedraaid: gate 1, 3, 9 en 14 groen, de selftest van `ocaml-build.sh` groen.

1. **fout, de stempel droeg de eindtijd** — `4114d428`. De projectlijst gaat vóór dune in de marker (`mktemp`, dan
   `printf`), na een groene build `mv` naar de stempel; `mv` laat de mtime staan. Selftest-stap 11: een `dune` op PATH
   die een seconde na de start `ref/bin/runner.ml` aanraakt en dan de echte dune draait; verwacht de FAIL van de
   assertie. Rood: dezelfde selftest tegen een mutant met de oude volgorde (`fix/mut1/ocaml-build.sh`): `step 11 …
   expected fail; got status 0` (`ocaml-build: rebuilt (…)`). Groen: PASS, dertien stappen, stap 11 `FAIL —
   _build/.gates-built is older than 1 source path(s), e.g. ref/bin/runner.ml …`. Logs `p1-selftest-mutant.log`,
   `p1-selftest.log`.
2. **risico, een gatedoel zonder commando gaf rc=0** — `3011f780`. Afwijking van het voorstel: de controle staat niet
   in het recept maar bij het lezen van de Makefile (`$(foreach …$(error no command CMD.<doel> …))`), zodat make stopt
   vóór er iets draait, ook bij `make gates`. Rood (stubs, `CMD.13-reftype` weg): vóór `make 13-reftype` →
   `13-reftype rc=0 0s`, exit 0; na → `Makefile:82: *** no command CMD.13-reftype for the target 13-reftype.  Stop.`,
   exit 2, ook voor `make gates`.
3. **risico, drie gatelijsten zonder controle** — `87dcef0b`. Het `gates`-recept vergelijkt eerst de gate-scripts van
   de doelen (volgorde van `GATES`) met de scripts op de niet-commentaarregels van `gates.yml` (eerste voorkomen, ook
   de volgorde: die van de samenvatting) en met de genummerde lijst van `CLAUDE.md` (als verzameling: `CLAUDE.md`
   nummert 3 vóór 4, `gates.yml` niet), en stopt met exit 2, de naam van het script en "nothing ran".
   `WAY-OF-WORKING.md` §4 noemt de drie plaatsen bij een nieuwe gate. Afwijkingen: `CLAUDE.md` is meegenomen, omdat de
   review de lijst ook daar telde; één bron is het niet geworden, want `gates.yml` houdt per job zijn eigen omgeving en
   `CLAUDE.md` beschrijft per gate, en één bron zou YAML-parsing of -generatie vragen. Rood (stubs): vóór draait de set
   door als `gates.yml` `check-scenarios.sh` noemt (red=0, exit 0); na → `.github/workflows/gates.yml runs
   scripts/check-scenarios.sh, no target of the Makefile does` en `the Makefile runs scripts/check-scenario.sh,
   .github/workflows/gates.yml does not`, exit 2; net zo voor `check-scenery.sh` als gate 16 in `CLAUDE.md`, en 03 en 04
   omgewisseld in `GATES` geeft `GATES is not in the order of .github/workflows/gates.yml …`. Groen: de stub-run op de
   echte lijsten loopt door.
4. **risico, de frontend-Makefile was geen bron van zijn stempels** — `054ad22e`. `Makefile` is prerequisite van
   `gen/.generated` en `out/.compiled`. Rood: met de frontend-Makefile van `3318e2d3` zegt `make -q compile` rc=0 na een
   gewijzigde `DOCLINT`; met de nieuwe rc=1, `make compile` bouwt opnieuw, daarna rc=0. Log
   `p4-frontend-makefile.log`.
5. **risico, niets hield de verifier zelfstandig bouwbaar** — `9541fe22`. Gate 3, controle 1b: python leest de
   dune-bestanden, zonder dune-aanroep; elke bibliotheek in een `libraries`-veld onder `spec/verifier` is door
   `spec/verifier` zelf gedefinieerd (`ir_v0`, `uchar-ir-v0`) of door geen andere map van de werkruimte (dan van opam
   of de compiler; nu `yojson`). De toegestane lijst is afgeleid, niet vast: de eigenschap die `--root spec/verifier`
   afdwong is "geen bibliotheek van een ander project van de werkruimte", en een vaste lijst opam-pakketten zou bij
   elke nieuwe (`unix`, `str`) rood worden zonder dat die eigenschap verandert. Een scan zonder `libraries` onder
   `spec/verifier` of zonder bibliotheek elders faalt. De PASS-regel noemt `3 names (ir_v0 yojson)`; `CLAUDE.md`
   gate 3 noemt de controle. Rood (kopie, `uchar-ref` in `spec/verifier/bin/dune`): de oude gate 3 PASS; `dune build
   --root spec/verifier` faalt (`Library "uchar-ref" not found`); de nieuwe gate 3 `FAIL — spec/verifier/bin/dune names
   uchar-ref, a library of ref/lib/dune …`, exit 1. Groen: gate 3 op de tak PASS. Logs `p5-*.log`.
6. **risico, "no two gates write the same file" zonder voorwaarde** — `570986e8`, `9dace4fd`. Na de gates draait
   `make gates` de stap `after-run`: de drie bouwregels opnieuw (logs onder `<run>/after/`), elk moet `fresh` zeggen,
   en `_build/.gates-built`, `frontends/regelspraak/out/.compiled` en de shaded jar mogen niet nieuwer zijn dan
   `<run>/.built`, aangeraakt na de bouwstap; anders `after-run rc=1 … — a source changed or a gate built during the
   run …`, rood. De Makefile-kop en `CLAUDE.md` noemen de twee voorwaarden (geen bron die tijdens de run verandert;
   geen andere gate, bouw of `make gates` in dezelfde checkout, met de gedeelde paden van scenario B); scenario B zelf
   wordt niet gecontroleerd (luid rood op de gedeelde paden; een parallelle run die een product herbouwt, vangt
   `after-run` wel). Rood (stubs, gate 12 raakt de jar aan): vóór red=0, exit 0; na `after-run rc=1 0s newer than the
   end of the build step: backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar …`, red=1, exit 2. Met de
   echte bouwregels en gestubde gates: niets verandert → `after-run rc=0`; gate 5 raakt `ref/lib/alloc.ml` aan →
   `after-run rc=1 0s ocaml-build: rebuilt (1 source path(s) newer than the stamp, e.g. ref/lib/alloc.ml; 0s); newer
   than the end of the build step: _build/.gates-built …`, exit 2. Bij die proef telde de samenvatting een rode
   `after-run` als groen (red=0, exit 0), omdat de toets `*' rc=0 '*` de overgenomen regel `ocaml rc=0 …` vond;
   `9dace4fd` toetst het begin van de regel (`<doel> rc=0 `) en neemt alleen de statusregel van de bouw over.
7. **opmerking, foutmeldingen** — `e288bc47`. Gates 9, 14, 15 en 17: `the OCaml build is red (the ocaml-build line
   above says why)`, de zin van gate 11; gate 13 `FAIL (build)` zoals 3, 5, 6, 7 en 8 (was `(ref/clingo build)`).
   Proef (kopie, `let broken = = 1` in `ref/lib/alloc.ml`): de vijf gates geven die regels na de ocaml-build-FAIL. Log
   `p7-messages-broken-ml.log`.
8. **opmerking, de forkzin van `drools-suite-forks`** — niets veranderd; de zin staat bij gate 12.
9. **opmerking, job 3 miste zarith** — `94757679`. Job 3 installeert `yojson zarith`. Nagegaan: de andere dune-jobs (5,
   6, 7, 8, 11, 13, 14, 15, 17) installeren beide al; gate 9 bouwt in de eigen switch van de runner, die catala en
   daarmee zarith draagt; de werkruimte vraagt verder alleen `unix` van de compiler; de jar-jobs bouwen de jar vóór de
   gate zoals voorheen. PyYAML en Ruby lezen het bestand: 17 jobs.
10. **opmerking, een kale `make` en `CORES=`** — `5b694ba2`. `.DEFAULT_GOAL := help` (één regel die `make gates`
    noemt), `override CORES :=`. Rood (stubs): vóór draait een kale `make` de set en houdt make `CORES = 1` bij
    `make CORES=1`; na toont een kale `make` de hulp en blijft `CORES := 16`.
11. **opmerking, dubbele code** — `1e30551a`. `run_suite DIR` in `ocaml-build.sh` leest de argv uit de
    `(run %{exe:suite.exe} …)`-vorm van `DIR/dune` en draait de suite in de bronmap met de paden onder
    `_build/default/DIR`; gates 9 en 14 roepen het aan, en hun `test/dune` noemt gate en `run_suite`, zodat de argv op
    één plek staat. In `ocaml-build.sh` staat de prune-`find` één keer (`scan`); de ongebruikte `paths` is weg (de
    foutcontrole van `find` blijft, op de bestandslijst). Proef: gates 9 en 14 op de tak PASS met 241 en 150 checks,
    zoals voorheen; in een kopie geeft een hernoemd argument in de regel `run_suite: FAIL — ../bin/flogic_frontend2.exe
    of frontends/flogic/test/dune is not an executable under _build/default/frontends/flogic/test`, en `%{dep:…}` in
    plaats van `%{exe:…}` `run_suite: FAIL — no '(run %{exe:suite.exe} <path> ...)' form …`. Logs `p11-*.log`.
12. **opmerking, `admit.sh`** — `600eda29`. `admit.sh` bouwt met `ensure_ocaml`; de terugval op
    `spec/verifier/_build` is weg. Proef: een kandidaat met het `ir.json` van `corpus/negative/wf001-duplicate-day-kind`
    → `ocaml-build: fresh`, `admit: REFUSED — the verifier rejects …`, geen map in `corpus/golden`. Log
    `p12-admit-refusal.log`.
13. **opmerking, geschiedenis in statische tekst** — `3318e2d3`. Gates 8 en 11 zeggen alleen dat de suite van de
    backend daar niet draait en dat gate 12 hem draait; de README noemt `make gates` en de root-`Makefile` bij
    `scripts/`.

Daarnaast `4ad3a8c5`: de Makefile-kop zegt wanneer een regel groen is, dat een doel zonder commando make stopt en dat
een kale `make` toont hoe de set draait; de kop van `ocaml-build.sh` noemt `admit.sh`.

### Landing (hoofdsessie)

- Akkoord van de eigenaar (2026-10-25), met een Opus-review vooraf (13 punten, alle verwerkt in de fixronde
  hierboven) en één punt van de hoofdsessie: `scripts/admit.sh` draait stap 6 met `make gates`.
- Herbaseerd op main `38983e6b` (met `drools-suite-forks`). `make gates` op `28598fb5`: 17 van 17 groen plus
  `after-run`, in 595 s (load1 4,0 bij de start, piek ±165); de seriële set (`werk/gates.sh`) duurde 1763 s op
  `5adb0386`. Langste gates in deze run: 12 589 s, 17 528 s, 13 334 s, 6 313 s. Samenvatting
  `out/gates/20261025-152715/summary.txt` in de worktree, log `werk/gates-design/builds-when-stale/landing-make-gates.log`.
- Acceptatie (g), de toolchain op deze commit: de toolchain pint alleen commits van uchar's main (sinds
  `gates-own-tests-in-parallel`), dus dit volgt bij de eerstvolgende pin-bump (kaart `drl-value-functions-pin`).
  Wat de toolchain van uchar leest, `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` in `scripts/check-drools.sh`, is
  ongewijzigd.
- `werk/gates.sh` (buiten de repository) is weggehaald.
