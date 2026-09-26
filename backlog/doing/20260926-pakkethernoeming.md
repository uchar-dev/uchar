# pakkethernoeming: io.github.yhavinga.regelspraak wordt dev.uchar.regelspraak

uchar bezit vanaf nu het Java-pakket `dev.uchar.regelspraak.*`. De opvolger
van yhavinga/regelspraak-java — uchar-dev/regelspraak-toolchain, een
distributie met uchar als git-submodule — krijgt
`dev.uchar.regelspraak.toolchain.*` en gebruikt uitsluitend uchars publieke
API; twee repo's in één pakket (split package) is daarmee uitgesloten. Deze
kaart is puur mechanisch en gaat ALLEEN en EERST: ze verplaatst 228
getrackte bestanden en zou met elke parallelle kaart conflicteren.

Gemeten 2026-09-26 (onderzoek toolchain-verhuizing, rapport in
`../research/toolchain-verhuizing/`): puntvorm `io.github.yhavinga` in
frontends 101 bestanden (601 voorkomens), backends 128 (639), de
pariteitszone 2 (8), scripts 3, spec 1, backlog 9; padvorm
`io/github/yhavinga` in bestandsinhoud: frontends 1, scripts 1, spec 4,
corpus 5, backlog 44. De wire draagt geen pakketnamen: 0 treffers in
`corpus/**` en in de json/txt/drl van de pariteitszone. Gegenereerde DRL
draagt de naam wél (`package …generated;`), maar gate 12 vergelijkt
uitkomsten, geen DRL-tekst.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Mechanisch, in punt- én padvorm. Eén commit met `git mv` van de mappen
  (zodat blame volgt) en één met de inhoud, of één commit voor beide; geen
  enkele andere wijziging — geen herformattering, geen "nu we toch bezig
  zijn".
- U2 Plekken (compleet maken met grep; deze lijst is de ondergrens): alle
  package- en importregels in `frontends/regelspraak/src`,
  `backends/drools/src` en de twee testbestanden in de pariteitszone (daar
  alleen die regels; de zone is verder read-only); `frontends/regelspraak/
  Makefile` (`PACKAGE`, het `ir`-doel met `…resolver.Resolver`);
  `backends/drools/pom.xml` (shade `mainClass`; de include-/exclude-patronen
  van build-helper en compiler controleren); de pakketnamen in STRINGS:
  `DrlTranspiler` (`FACT_PACKAGE` r. 60, `package …generated;` r. 245, de
  DRL-imports r. 303–307), `ExecModelTranspiler.PKG`/`FACTS` (r. 82–83),
  `FactModelGenerator.PAKKET` (r. 34) en elke andere string-treffer;
  `LezerszinGrensTest` r. 48 (bronpad als string); `scripts/
  check-regelspraak.sh` r. 75, `scripts/check-toka-parity.sh` r. 183,
  `scripts/check-rood.sh` r. 405 (FQN's); `scripts/check-self-contained.sh`
  r. 134 (de padallowance voor de replay-API-token op de bestandspaden van
  `CaseRunner` en `DroolsBackendMain`: het pad verhuist mee, de allowance
  blijft even smal); `spec/IR-SPEC.md` r. 4899 en 4901 (padverwijzingen);
  `corpus/negatief/wf037-*/NOTE.md` (vijf, testpad); `backends/drools/
  README.md` r. 45; de voorstellen onder `spec/proposals/` met de oude naam
  (mechanisch bijwerken). NIET: `backlog/**` en `spec/IR-REVIEW.md`
  (geschiedenis).
- U3 Gate 1's privé-repopatroon in `scripts/check-self-contained.sh` krijgt
  `regelspraak-toolchain` erbij: de nieuwe privé-repo mag in artefactzones
  evenmin genoemd worden. Controleer dat de gate daarna groen blijft.
- U4 Klaar: `git grep -n -e 'io\.github\.yhavinga' -e 'io/github/yhavinga'
  -- ':!backlog' ':!spec/IR-REVIEW.md'` geeft niets; de gates 1, 10, 11, 12
  en 13 zijn groen én hun logs op de basis en op de kaart zijn gelijk na
  `sed 's/io\.github\.yhavinga/dev.uchar/g; s#io/github/yhavinga#dev/uchar#g'`
  op beide — geen telling verschuift (JUnit 764 frontend, 814 Drools; gate
  12 speelt 209 en de DRL-tak 179; gate 11 28/29; gate 13 41/23/22). De
  hoofdsessie draait daarna de volledige set op de kaart. Naamtabel-toets
  (`python3 spec/proposals/v1-naamtabel-toets.py`) rc 0.
- U5 Frontend: `make clean` vóór de bouw (`gen/` volgt `PACKAGE`); de
  gegenereerde parser komt in `gen/dev/uchar/regelspraak/parser`. Drools:
  de shaded jar draagt `Main-Class: dev.uchar.regelspraak.cases.
  DroolsBackendMain` (controleer met `unzip -p … META-INF/MANIFEST.MF`).
- U6 Result met de tellingen vóór en na, de logdiff-uitkomst per gate, en
  elke plek buiten U2 die grep nog vond.

## Acceptatie

- (a) U4 volledig; (b) blame volgt de verplaatste bestanden (`git log
  --follow` op een verplaatst bestand toont de oude geschiedenis); (c) geen
  inhoudelijke wijziging: `git diff --stat -M` van de kaart tegen de basis
  bestaat alleen uit hernoemingen en regels met de pakketnaam.

May touch: alles wat de oude naam draagt behalve `backlog/**` en
`spec/IR-REVIEW.md`; `scripts/*` en `spec/IR-SPEC.md` zijn beschermde paden
en deze kaart geeft daarvoor mandaat, uitsluitend voor de naamvervanging en
U3. Niet in deze kaart: elke inhoudelijke wijziging; `CLAUDE.md`,
`WAY-OF-WORKING.md` en `spec/PLAN.md` (hoofdsessie bij de landing).

## Result (2026-09-26, bouwer)

### Ronde 1 — branch `card/pakkethernoeming`, kop: de commit die dit Result toevoegt, op `bc7550ed`

#### Stand
- Basis main `df370d07`; claim `3a9a8f22`; werkboom schoon (`git status --short`
  leeg; `gen/`, `out/`, `lib/`, `target/` en `_build/` zijn gitignored
  bouwproducten). Niets gepusht of gemerged, geen kaartjes gemaakt.
- Commits:
  1. `3a9a8f22` kaart pakkethernoeming: claim.
  2. `645e4bc3` de Java-bomen naar dev/uchar/regelspraak: alleen `git mv` van de vijf
     mappen `…/java/io/github/yhavinga/regelspraak` naar `…/java/dev/uchar/regelspraak`
     (frontend main 34, frontend test 66, Drools main 33, Drools test 93,
     pariteitszone 2): 228 bestanden, alle R100, 0 regels gewijzigd.
  3. `4cc467e3` de pakketnaam wordt dev.uchar.regelspraak: 244 bestanden, 1260 regels.
     Vervanging `perl -pi -e 's/io\.github\.yhavinga/dev.uchar/g; s#io/github/yhavinga#dev/uchar#g'`
     op precies de bestanden uit `git grep -l -e 'io\.github\.yhavinga' -e 'io/github/yhavinga'
     -- ':!backlog' ':!spec/IR-REVIEW.md'`.
  4. `bc7550ed` gate 1: de opvolger van regelspraak-java is ook privé (U3: één
     patroonregel, drie commentaarregels).
  5. de commit die dit Result toevoegt.
- U1-keuze: twee commits (eerst de verplaatsing, dan de inhoud), zodat elke hernoeming
  R100 is en `git log --follow` en blame haar zonder gelijkenisdrempel volgen. Gevolg:
  `645e4bc3` alleen is rood (`LezerszinGrensTest` r. 48 en de padallowance van gate 1
  r. 134 wijzen daar nog naar het oude pad); groen gemeten is de kop.

#### Tellingen vóór en na per zone
Bestanden/voorkomens; vóór = `df370d07`, na = `bc7550ed`.

| zone | oud, puntvorm | oud, padvorm | nieuw, puntvorm (na) | nieuw, padvorm (na) |
|---|---|---|---|---|
| frontends | 101/601 → 0/0 | 1/1 → 0/0 | 101/601 | 1/1 |
| backends | 128/639 → 0/0 | 0/0 | 128/639 | 0/0 |
| pariteitszone (`toka/`) | 2/8 → 0/0 | 0/0 | 2/8 | 0/0 |
| scripts | 3/3 → 0/0 | 1/2 → 0/0 | 3/3 | 1/2 |
| spec/IR-SPEC.md | 0/0 | 1/2 → 0/0 | 0/0 | 1/2 |
| spec/proposals | 1/1 → 0/0 | 2/3 → 0/0 | 1/1 | 2/3 |
| corpus | 0/0 | 5/5 → 0/0 | 0/0 | 5/5 |
| spec/IR-REVIEW.md (uitgezonderd) | 0/0 | 1/1 → 1/1 | — | — |
| backlog (uitgezonderd) | 10/14 → 10/14 | 45/115 → 45/115 | — | — |

Getrackte paden met `io/github/yhavinga`: 228 → 0; met `dev/uchar/regelspraak`: 0 → 228.
De meting in de kaarttekst (backlog 9 en 44) is van vóór deze kaart; het tiende en
vijfenveertigste bestand is de kaart zelf.

#### Acceptatie
- (a) U4.
  - `git grep -n -e 'io\.github\.yhavinga' -e 'io/github/yhavinga' -- ':!backlog' ':!spec/IR-REVIEW.md'`
    geeft niets (rc 1).
  - Gates 1, 10, 11, 12 en 13 groen op `bc7550ed` (elk rc 0); logdiff hieronder.
  - `python3 spec/proposals/v1-naamtabel-toets.py`: rc 0, `v1-naamtabel-toets: OK`,
    uitvoer byte-gelijk aan die op de basis.
- (b) `git log --follow --oneline -3` toont de oude geschiedenis onder `4cc467e3` en
  `645e4bc3`: `DroolsBackendMain.java` → `004b04ed`, `Resolver.java` → `bfdfeb3e`,
  `IrTest.java` → `acc5a4e3`, `CasusTest.java` in de pariteitszone → `b0e3ada9`.
  `git blame -L 20,22` op `Resolver.java` wijst naar `36f261fe9` onder het oude pad.
- (c) `git diff --stat -M df370d07..bc7550ed`: 245 bestanden, 1283+/1280−: 228
  hernoemingen van de Java-bomen (over het hele bereik R079–R099, in `645e4bc3` zelf
  alle R100), de kaart (R100) en 16 gewijzigde bestanden. Per bestand gecontroleerd dat
  `git show df370d07:<oud>` met de vervanging erop byte-gelijk is aan
  `git show bc7550ed:<nieuw>`: dat geldt voor alle 244 bestanden behalve
  `scripts/check-self-contained.sh`, waar het enige verschil de U3-regels r. 37–40 zijn.
  De 1279+/1279− van de vervanging tegenover 1260 regels met de naam: git's
  standaard-diff (Myers) toont in 18 bestanden 19 ongewijzigde regels (lege regels
  tussen het package- en het importblok) als verwijderd en weer toegevoegd; met
  `--diff-algorithm=histogram` of `patience` telt de vervanging precies 1260/1260.

#### Logdiff per gate
Basislogs `/tmp/hernoem/base/<n>.log` (hoofdcheckout op `df370d07`, volgorde 1, 10,
12, 11, 13); kaartlogs `/tmp/hernoem/kaart/<n>.log` (deze worktree op `bc7550ed`, in
dezelfde volgorde, elke gate een losse aanroep: `eval $(opam env)`, dan
`./scripts/<gate>.sh > /tmp/hernoem/kaart/<n>.log 2>&1`). Vergelijking, in `/tmp/hernoem`:

    diff <(sed 's/io\.github\.yhavinga/dev.uchar/g; s#io/github/yhavinga#dev/uchar#g' base/N.log) \
         <(sed 's/io\.github\.yhavinga/dev.uchar/g; s#io/github/yhavinga#dev/uchar#g' kaart/N.log)

- Gate 1: leeg. `check-self-contained: PASS`.
- Gate 10: leeg. 764 tests (floor 173); 93 rgs-goldens byte-identiek (floor 80).
- Gate 11: leeg. Suite van de pariteitszone 27 tests; 45 .rgs byte-identiek
  heremitteerd; 29 slices schema-geldig; 28 verifier-schoon; 29 cases door ref, clingo
  en Drools; in beide logs `drools-jar: fresh (jar newer than 161 source files)`.
- Gate 12: twee regels verschillen, beide duur: r. 4 `DRL-text sweep: 124s wall clock
  over 265 goldens` → `125s`, r. 35 `… sweep cost 124s wall clock` → `125s`. Tellingen
  gelijk: played 209, DRL-tak 179, JUnit 814 (floor 239), auto-excluded 52,
  value-divergence 1, no-case 3, channel floor 56, DRL-text floor 86.
- Gate 13: leeg. 41 annotaties over 23 goldens, 106 replays, 22 door de DRL-tak.

Geen van de tien logs draagt de oude of de nieuwe pakketnaam, dus de sed verandert
niets: de ruwe logs van 1, 10, 11 en 13 zijn byte-gelijk, die van 12 op de twee
duurregels na. Tijdstempels staan niet in deze logs. Duur op de kaart (wandklok): gate
10 21 s, 12 250 s, 11 53 s, 13 63 s. De volgorde van de basis (12 vóór 11 en 13) is
aangehouden, zodat gate 12 de jar bouwt en 11 en 13 haar vers vinden, zoals op de
basis; de jar is in deze ronde één keer gebouwd.

#### Bouw (U5)
- `make clean && make compile && make test` in `frontends/regelspraak`: rc 0, 764 tests
  successful; de parser staat in `gen/dev/uchar/regelspraak/parser`.
- De shaded jar bouwde gate 12 (`mvn -o -f backends/drools/pom.xml package`, suite 814).
  `unzip -p backends/drools/target/*-shaded.jar META-INF/MANIFEST.MF | grep Main-Class`
  → `Main-Class: dev.uchar.regelspraak.cases.DroolsBackendMain`. De inhoudslijst is,
  na dezelfde naamnormalisatie, gelijk aan die van de jar van de basis op de mapregels
  `dev/` (nieuw) en `io/`, `io/github/` (weg) na: 233 items onder `dev/uchar/regelspraak/`,
  0 onder `io/github/`, 0 uit `resolver/` en `emit/` (zoals op de basis).
- De pom-uitsluitingen `**/regelspraak/resolver/**` en `**/regelspraak/emit/**` en
  build-helpers `${frontend.src}` dragen geen pakketvoorvoegsel; ongewijzigd.

#### U3: afwijking van de letterlijke tekst (technische keuze)
Met het kale token `regelspraak-toolchain` in `PRIVATE` wordt gate 1 rood (rc 1):
`corpus/golden/README.md:183:RegelSpraak-toolchain render of the defeasibility defaults that`
→ `FAIL: machine-local or private-repo reference in artifact zone 'corpus/golden'`
(gemeten met een tijdelijke kopie van het script). Die regel gebruikt de samenstelling
"RegelSpraak-toolchain" voor de toolchain in het algemeen; de gate zoekt
hoofdletterongevoelig (`-i`), dus het kale token maakt geen onderscheid met de
repo-naam. De README herschrijven is een inhoudelijke wijziging buiten het mandaat van
deze kaart. Gekozen: het token met org-voorvoegsel, `uchar-dev/regelspraak-toolchain`,
in hetzelfde hoofdletterongevoelige patroon, met drie regels commentaar over het waarom.
Gemeten: schone boom PASS; een tijdelijk toegevoegde regel
`zie uchar-dev/regelspraak-toolchain` in `backends/drools/README.md` gaf FAIL op zone
`backends` (rc 1), daarna teruggezet. Wat het niet vangt: de kale naam zonder org
(open punt 1).

#### Plekken buiten U2 die grep vond
Alle vervangen volgens dezelfde regel.
1. Volledig gekwalificeerde namen buiten package- en importregels: 302 regels in 43
   Java-bestanden. Ruwe indeling op de regeltekst: 229 in code (typen en aanroepen, bv.
   `RegelSpraakRuntime` r. 148 en 1285 naar `transpiler.kern.Namen`, `IrEmitterTest`
   r. 316–317, `AantalUnieTest` r. 180 en 182; de grootste: `DrlTranspiler` 59,
   `ExecModelTranspiler` 49, `RuleChecker` 32, `RegelSpraakModel` 24), 64 in Javadoc
   (`{@link}`) en commentaar, en 9 in strings. Die 9 zijn precies de U2-plekken
   (`FactModelGenerator` r. 34, `DrlTranspiler` r. 60, 245, 303, 305 en 307,
   `ExecModelTranspiler` r. 82–83, `LezerszinGrensTest` r. 48); een andere
   string-treffer is er niet.
2. `LaderWeigeringWitnessTest.java` r. 469: commentaar dat een eerdere JVM-melding
   citeert ("No enum constant <pakket>.model.RegelSpraakModel.Granulariteit.jaar").
   Vervangen volgens de regel (geen backlog, geen IR-REVIEW); het citaat draagt nu de
   naam die de JVM na de hernoeming meldt.
3. Gecontroleerd zonder treffer: de pom-uitsluitingen (zie Bouw); `.github/CODEOWNERS`
   r. 4–9 draagt `@yhavinga`, de GitHub-gebruikersnaam en geen pakketnaam, niet
   aangeraakt. Andere schrijfwijzen (underscore, geëscapete punt, gesplitste string)
   komen niet voor: `git grep -i yhavinga` buiten backlog en IR-REVIEW geeft alleen
   CODEOWNERS.
4. `CLAUDE.md`, `WAY-OF-WORKING.md`, `spec/PLAN.md` en `spec/CONSTRUCTS.md` dragen de
   pakketnaam niet; de gate-1-beschrijving in `CLAUDE.md` ("private-repo references")
   dekt U3 zonder tekstwijziging.

#### Open punten
1. U3-dekking. De gate vangt de opvolger alleen met org-voorvoegsel. Moet ook de kale
   naam `regelspraak-toolchain` vuren, dan moet eerst `corpus/golden/README.md` r. 183
   anders geformuleerd worden (inhoud, voor de hoofdsessie of de maintainer); daarna kan
   het token naar de kale naam. Dezelfde alinea noemt op r. 182 "toolchain `332db4c`",
   een commit van de regelspraak-java-toolchain, die de nieuwe privé-repo met de
   gespiegelde geschiedenis ook draagt; gate 1 laat die regel door omdat ze geen
   repo-naam noemt.
2. Verouderde regelnummers in `spec/IR-SPEC.md` r. 4899, 4901 en 4903, niet door deze
   kaart (de regeltelling van de verplaatste bestanden is ongewijzigd):
   `RegelSpraakModel.java:620` (`heeftLid` staat nu op r. 948),
   `ExpressieBouwer.java:470-481` (de poort met `heeftLid` staat nu op r. 658; r. 470 en
   481 liggen in de datumliteraalcode) en "same file, :1663" (r. 1663 is nu commentaar
   bij de typering van `som van`). Geschreven in `59fd858c` (2026-09-06). Het padgedeelte
   klopt na deze kaart, de regelnummers niet. Niet gefixt: niet mechanisch, en IR-SPEC
   is beschermd.
3. Gate 8 (`scripts/check-rood.sh` r. 405) draagt de hernoemde FQN
   `dev.uchar.regelspraak.cases.DroolsRoodMain`; die gate staat niet op de lijst van
   deze kaart en is niet gedraaid. Gecontroleerd: de klasse staat in de shaded jar en
   start (`usage: DroolsRoodMain <ir.json> <case.json>`). De hoofdsessie draait de
   volledige set.
