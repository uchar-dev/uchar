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
