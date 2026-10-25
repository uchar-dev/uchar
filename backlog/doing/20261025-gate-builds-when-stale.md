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
