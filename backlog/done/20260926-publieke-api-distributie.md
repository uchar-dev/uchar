# publieke-api-distributie: één facade voor het gate-pad, en wat de distributie van uchar leest

De opvolger van yhavinga/regelspraak-java (uchar-dev/regelspraak-toolchain,
een distributie met uchar als git-submodule, pakket
`dev.uchar.regelspraak.toolchain.*`) leest uchar uitsluitend via de
publieke API. Het onderzoek van 2026-09-26 (rapport in
`../research/toolchain-verhuizing/onderzoek-2026-09-26.md`, §2 G1 en §4)
mat wat daar nu misgaat: de dienst, de scenariorunner en de engine-facade
van de oude toolchain bouwen met `ExecModelTranspiler.build(model, cl)`, en
die overload delegeert in uchar met LEGE zijtafels (defaults, rolbindingen,
lets, bronpaden, navigatiebindingen), waar de kopie in de oude toolchain de
defaults zelf uit het model las. Gemeten: de uitsluiting-tweeling noemt via
HTTP een uitgesloten jutter penninggerechtigd (4 van 4 fout, status 200,
geen marker). Geen van de zes aanroepers in uchar zelf gebruikt die
overload (`CaseRunner:269` en vijf getuigen geven alle zijtafels door). De
prelude van `CaseRunner.build` (lader → rekenjaarcheck → wederkerigheids-
checks → `FactModelGenerator` → `BeanCompiler` → `ContractValidator` →
`ExecModelTranspiler.build` met alle zijtafels → dekkingscheck → KieBase; de
DRL-tak ernaast) is privé, zodat een consument haar alleen kan nabouwen.
De scenariolaag (resolver, runner, `ScenarioResultaat`) en de regelgraaf
horen in uchar (maintainer, 2026-09-26): de scenariolaag komt met een eigen
kaart als frontend-extensie, de graaf wordt in een eigen kaart samengevoegd
en gegate. Deze kaart raakt ze niet.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Eén publieke facade in `backends/drools` voor het gate-pad, van
  wire-document tot KieBase: invoer het geladen document (`JsonNode` of
  pad), uitvoer een record met model, beanlader, `ExecModelTranspiler`
  (of DRL-tak, per vlag), dekking en KieBase; dezelfde weigeringen als de
  replay (`Excluded` met dezelfde zinnen). `CaseRunner.build` gebruikt de
  facade — één prelude, geen tweede kopie — zodat gate 12 haar draait.
  Naam en vorm kiest de bouwer; javadoc noemt de consument (de toolchain).
- U2 De overload `ExecModelTranspiler.build(model, cl)` verdwijnt (nul
  aanroepers in uchar): stil weglaten van zijtafels moet een compilefout
  zijn, geen runtimegok. Elke andere overload die een zijtafel weglaat die
  het document kan dragen, gaat mee of weigert luid; hetzelfde voor
  `DrlTranspiler.build`. De getuigentests die een kortere overload
  gebruiken, geven de zijtafels expliciet (lege lijsten waar hun document
  er geen heeft).
- U3 `engine/ScenarioResultaat`, `graaf/` en de scenariolaag blijven
  onaangeroerd (eigen kaarten). Wat de distributie verder van de frontend
  leest — `Resolver.resolve`, `IrEmitter.emit`, de diagnoses — is al
  publiek; de bouwer meet met `jdeps` welke typen de facade naar buiten
  brengt en houdt die lijst in het Result.
- U4 (vervallen: de graaf blijft in uchar.)
- U5 Klaar: alle veertien gates groen; gate 12 speelt 209 en de DRL-tak
  179 (geen pin verschuift); de getuigentest voor U1: op elke golden met
  een `bron.rgs` geeft de facade dezelfde regeltelling in de KieBase als
  de replay (of: de replay ís de facade — dan een test dat de facade op
  `jutterswet-uitsluiting-rgs` twee regels in de KieBase zet, één regel en
  één defaults-familie); naamtabel-toets rc 0.
- U6 Geen spec-wijziging; `backends/drools/README.md` krijgt één alinea
  over de facade als de ingang voor consumenten. Zinnen: bestaande
  weigerzinnen ongewijzigd, geen regelnummerstaarten, L5-spelling.

## Acceptatie

- (a) U1, U2, U5 en U6 volledig; (b) `git grep -n "build(model, cl)"`
  leeg; (c) Result met de gekozen namen en de signatuur van de facade.

May touch: `backends/drools/**`, `frontends/regelspraak/src/**`. Niet in
deze kaart: spec, scripts, corpus (behalve dat gate 12's pins gelijk
blijven), `CLAUDE.md` (de hoofdsessie werkt "Repo relationships" bij de
landing bij).

## Result (2026-09-26, bouwer)

**Stand.** Basis `b88a8ccc`; claim `7f09e72c`; werkcommits `be5cd14b`, `19082fce`, `06d67637`,
`319d2a83`, `91e0e3f5`; de kop is de commit die dit Result toevoegt (direct na `91e0e3f5`); de
werkboom is daarna schoon. Niet gepusht, niet gemerged; de kaart blijft in `doing/`.

**Commits.**
1. `7f09e72c` claim.
2. `be5cd14b` drools: één publieke facade voor het gate-pad — nieuw
   `backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`; de prelude van
   `CaseRunner.build` (lader, evaluatiejaar- en wederkerigheidsweigeringen, `FactModelGenerator`,
   `BeanCompiler`, `ContractValidator`, lowering met alle zijtafels, dekkingscheck, KieBase; de DRL-tak
   ernaast) is letterlijk daarheen verhuisd, met haar commentaar; `CaseRunner.build(Path, boolean)` is nu
   `DroolsBackend.build(irPath, viaDrl ? DRL_TEXT : EXEC_MODEL)` plus de constructor; `CaseRunner.Excluded`
   is `public` (constructor blijft package-private).
3. `19082fce` transpilers: alleen nog de bouwingang met alle zijtafels (U2, zie onder) plus de negen
   getuigentests die een kortere ingang gebruikten.
4. `06d67637` getuige `DroolsBackendWitnessTest` (5 tests).
5. `319d2a83` README-alinea (U6).
6. `91e0e3f5` javadoc-correctie in `CaseRunner.build` ("every gate path" klopte niet meer: gate 12 telt
   sinds 2026-09-19 ook de DRL-tak). Alleen commentaar: de zes `CaseRunner*.class` zijn met `cmp`
   byte-gelijk aan die in de jar die gate 12 op `319d2a83` bouwde.

Alles valt binnen `backends/drools/**` plus de kaartverplaatsing (`git diff --stat b88a8ccc HEAD`: 16
bestanden). `frontends/regelspraak/src/**` is niet aangeraakt: U3 bevestigt dat wat de distributie daar
leest al publiek is, en de backend compileert `resolver`/`emit` niet.

**U1 — de facade: naam, vorm, signatuur (acceptatie (c)).** Pakket `dev.uchar.regelspraak.cases`, naast
`CaseRunner`, zodat de facade `CaseRunner.Excluded` met de bestaande constructor kan gooien. Uit
`javap -public` (echte uitvoer, ingekort tot de signaturen):

```
public final class dev.uchar.regelspraak.cases.DroolsBackend {
  public static DroolsBackend$Build build(java.nio.file.Path, DroolsBackend$Branch) throws java.io.IOException;
  public static DroolsBackend$Build build(com.fasterxml.jackson.databind.JsonNode, DroolsBackend$Branch);
}
public final class DroolsBackend$Branch extends java.lang.Enum<DroolsBackend$Branch> { EXEC_MODEL; DRL_TEXT; }
public final class DroolsBackend$Build extends java.lang.Record {
  public DroolsBackend$Build(RegelSpraakModel, ClassLoader, ExecModelTranspiler, DrlTranspiler,
                             java.util.List<Dekking>, org.kie.api.KieBase);
  public DroolsBackend$Branch branch();
  public java.util.Map<String, java.util.function.Predicate<java.time.LocalDate>> dagsoortGlobalen();
  public RegelSpraakModel model(); public ClassLoader beanLoader();
  public ExecModelTranspiler execModelTranspiler(); public DrlTranspiler drlTranspiler();
  public java.util.List<Dekking> dekking(); public org.kie.api.KieBase kieBase();
}
public final class dev.uchar.regelspraak.cases.CaseRunner$Excluded extends java.lang.RuntimeException {}
```

Keuzes van de bouwer: `Branch` als enum in plaats van de boolean van `CaseRunner` (leesbaar aan de
aanroepplek, null geeft een NPE met de zin `branch`); in `Build` staat precies één transpiler (de compacte
constructor weigert geen of beide); `dekking` is een onveranderlijke kopie en bevat alleen LOWERED, omdat
de facade elk ander verdict weigert; `dagsoortGlobalen()` is afgeleid (leeg op de exec-tak). Weigeringen:
`IrLoader.Refusal` en `IrLoader.Documentfout` gaan ongewijzigd door (de replay maakt van een Refusal
`loader: …`, zoals voorheen); alles daarna is `CaseRunner.Excluded` met de zinnen van de replay. De
javadoc noemt de consument als "the toolchain distribution — CLI, decision service, scenario runner —
that builds this repository as a git submodule" (de reponaam mag niet in een artefactzone, gate 1).
`Excluded` bleef in `CaseRunner` omdat twintig testplekken hem als `CaseRunner.Excluded` noemen.

Gemeten (U1 en U2 los): een sweep over elke golden met cases (exec en `--drl`, 262 goldens) en elke rode
case (43) met de statusregel van de jar, basis tegen facade tegen U2:
(buiten de repo) <jar> <uit>`; `diff sweep-base.tsv sweep-u1.tsv` en
`diff sweep-base.tsv sweep-u2.tsv` leeg — 567 van 567 regels byte-gelijk, dus elke weigerzin ongewijzigd.
Verdeling (beide gelijk): exec 209 PLAYED / 52 EXCLUDE / 1 FAIL; DRL 179 PLAYED / 81 EXCLUDE / 2 FAIL;
rood 11 FAULT / 8 NOFAULT / 24 EXCLUDE.

**U2 — de korte overloads.** Weg uit `ExecModelTranspiler`: `build(m, cl)`, `build(m, cl, defaults,
rolBindingen, lets)`, `build(m, cl, defaults, rolBindingen, lets, bronPaden)`. Weg uit `DrlTranspiler`:
`build` met 1, 2, 3, 4 en 5 argumenten, `transpile(model)` en de vier korte publieke constructors; de
vijfde constructor is privé (een zo gemaakte instantie had niets gelowerd en gaf een leeg manifest, dus
"alles gelowerd"). De parameterdocumentatie van die constructors staat nu op de ene `build`. Beide
`build`s weigeren een null-zijtafel met een NPE (voorheen stond null stil voor "leeg"). Negen getuigentests
geven de zijtafels van de lader expliciet door (`BronRolVouw`, `DagsoortOpzoekvolgorde`, `GemengdeUnieRand`,
`HeelperiodeKwantorvouw`, `MeerPeriodiekePoort` (nu `loadAll` i.p.v. `load`), `NaamNorm`,
`Sleutelconventie`, `DrlEenheidsplek`, `Eenheidsconflict`); `BronRolVouw` houdt de rolbinding bewust
achter en doet dat nu met `java.util.Map.of()`. Geen enkele assertie veranderde van uitkomst.
Metingen: acceptatie (b) `git grep -n "build(model, cl)"` geeft alleen drie regels van deze kaart zelf
(regels 9, 34 en 59; het criterium citeert de string); `git grep -n "build(model, cl)" -- ':!backlog'`
is leeg (rc 1). `git diff b88a8ccc 19082fce -- backends/drools/src/test | grep -cE '^[+-].*@(Test|…)'` = 0,
dus de suite telde vóór de getuige 814 tests, zoals op de basis.

**U3 — jdeps.** `jdeps -verbose:class -filter:none -cp <shaded jar>` op `DroolsBackend.class`,
`DroolsBackend$Branch.class`, `DroolsBackend$Build.class`: 57 typen, waarvan 3 van de facade zelf, 29 uit
`java.*` en deze 25 andere:
`com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}`; `dev.uchar.regelspraak.cases.{CaseRunner,
CaseRunner$Excluded}`; `engine.BeanCompiler`; `ir.{Spelling, Spelling$Familie}`; `loader.{IrLoader,
IrLoader$Loaded}`; `model.{RegelSpraakModel, RegelSpraakModel$Objecttype}`; `schema.{ContractValidator,
FactModelGenerator}`; `transpiler.DrlTranspiler`; `transpiler.kern.{Dekking, Dekking$Verdict, Rekenjaar}`;
`transpiler.model.ExecModelTranspiler`; `org.drools.kiesession.rulebase.InternalKnowledgeBase`;
`org.drools.model.Model`; `org.drools.modelcompiler.KieBaseBuilder`; `org.kie.api.{KieBase,
conf.KieBaseOption, io.ResourceType}`; `org.kie.internal.utils.KieHelper`.
Wat de facade naar buiten brengt (publieke signaturen, `javap -public`): `Path`, `IOException`,
`JsonNode`, `Branch`, `Build`, `RegelSpraakModel`, `ClassLoader`, `ExecModelTranspiler`, `DrlTranspiler`,
`List<Dekking>`, `KieBase`, `Map<String, Predicate<LocalDate>>`; via de gedocumenteerde weigeringen
`IrLoader.Refusal`, `IrLoader.Documentfout`, `CaseRunner.Excluded`; via de publieke leden van de
blootgestelde transpilers verder `org.drools.model.Model` en `Dekking.Verdict`. Logs:
(buiten de repo), `javap-public.txt`, `javap-transpilers.txt`.

**U5 — klaar-criteria.** Gate 12 speelt 209 (pin 209) en de DRL-tak 179 (pin 179); geen pin verschoof,
`scripts/` is niet aangeraakt. De replay is de facade, dus geldt de tweede vorm van de getuige:
`DroolsBackendWitnessTest` (echte uitvoer van de eerste run: 5 tests, 0 fouten):
- `de_ingang_zet_de_regel_en_de_defaultsfamilie_in_de_kiebase`: op `jutterswet-uitsluiting-rgs` zijn de
  regels in de KieBase `[defaults K:Jutter.uitgesloten, penninggerechtigd tenzij uitgesloten]`, het
  manifest `[defaults: defaults K:Jutter.uitgesloten, regel: penninggerechtigd tenzij uitgesloten]`, alles
  LOWERED;
- `zonder_de_defaults_mist_de_familie_en_zegt_het_manifest_niets`: dezelfde transpiler met een expliciet
  lege defaults-lijst zet één regel en noemt zijn manifest geheel gelowerd (de fout uit het onderzoek);
- `de_drl_tak_weigert_in_de_zin_van_de_replay`: facade en replay weigeren de DRL-tak met dezelfde zin,
  de exec-replay speelt 4;
- `elke_publieke_ingang_die_een_model_neemt_neemt_elke_zijtafel`: reflectie; per transpiler geen publieke
  constructor en elke publieke statische methode die een `RegelSpraakModel` neemt, neemt elk
  componenttype van `IrLoader.Loaded` (als multiset); er is er precies één, `build`;
- `een_ontbrekende_zijtafel_wordt_luid_geweigerd`: elke zijtafel null gemaakt geeft per transpiler vijf
  keer de NPE met de zin hieronder.
Mutatieproeven (elk teruggezet met `git checkout`, daarna 5/5 groen): (1) facade geeft `List.of()` voor
de defaults → 2 fouten, `expected: <[defaults K:Jutter.uitgesloten, penninggerechtigd tenzij uitgesloten]>
but was: <[penninggerechtigd tenzij uitgesloten]>` en de exec-replay `FAIL` op case1 en case3
(`kenmerk uitgesloten: verwacht aanwezig, werkelijk afwezig; kenmerk penninggerechtigd: verwacht afwezig,
werkelijk aanwezig`); (2) een teruggezette `DrlTranspiler.build(model, lets)` → `…build(RegelSpraakModel,
java.util.List) does not take java.util.List<dev.uchar.regelspraak.ir.PrioritizedDefaults>`; (3) de
null-terugval voor `lets` in `ExecModelTranspiler` terug → `Expected
java.lang.reflect.InvocationTargetException to be thrown, but nothing was thrown.`
Naamtabel-toets: `python3 spec/proposals/v1-naamtabel-toets.py` → `v1-naamtabel-toets: OK`, rc 0.

**U6.** Geen spec-wijziging. `backends/drools/README.md` kreeg de sectie "The entry for consumers" met
één alinea. Bestaande weigerzinnen ongewijzigd (sweep hierboven), geen regelnummerstaarten.

**Zinnen letterlijk.** Nieuw (programmeursfouten, geen IR-weigeringen, dus zonder tegenhanger in
verifier, ref of clingo; ze noemen de Java-parameter):
- `side table 'defaults' is null — pass an empty list for a document without one` (zo ook `'lets'`);
- `side table 'rolBindingen' is null — pass an empty map for a document without one` (zo ook
  `'bronPaden'` en `'navigatieBindingen'`) — in beide transpilers dezelfde tekst;
- `a Build carries exactly one transpiler, got none` / `… got both`;
- `branch` (NPE bij een null-tak).
Ongewijzigd verhuisd naar de facade: `Rekenjaar.WEIGERING`, de twee zinnen over de wederkerige
zelfrelatie, `contract: …`, `transpiler <verdict>: <sleutel> — <reden>`, `drl: ch. 7 prioritizedDefaults
— the DRL-text branch has no defaults lowering`, `drl-transpiler …`, `drl compile: …`.

**Gates** (elk als losse aanroep, `eval $(opam env)` vooraf; logs in (buiten de repo)):
- 1 `check-self-contained.sh`: PASS (ook vóór elke commit, met de nieuwe bestanden gestaged).
- 8 `check-rood.sh`: PASS — 43 rode cases, vloer 26; Drools-been 11 fault-gepind, 32 niet-gedragen met
  reden; 86 gepinde fragmenten, geen bodemzin.
- 11 `check-toka-parity.sh`: PASS — 45 bronnen byte-gelijk heruitgegeven (vloer 45, 2 holdouts), suite
  27 tests (vloer 27), 29 slices schema-geldig, 28 verifier-schoon (vloer 28), differentiaal 29 cases:
  ref 29, clingo 29, Drools 29 PLAYED / 0 EXCLUDE / 0 FAIL-of-ERROR.
- 12 `check-drools.sh`: PASS — played 209 (pin), auto-excluded 52, value-divergence 1, no-case 3,
  kanaalvloer 56; DRL-tak 179 (pin), DRL-vloer 86, sweep 156 s; JUnit 819 tests (814 + 5), vloer 239,
  0 overgeslagen; totaal 5 min 27 s.
- 13 `check-reftype-derivation.sh`: PASS — 41 twists over 23 goldens (vloer 38), 106 replays, 22 door de
  DRL-tak gespeeld (vloer 20), 0 survivors.
Niet door mij gedraaid: 2–7, 9, 10, 14. Geen daarvan leest een bestand dat deze kaart wijzigde (alleen
`backends/drools/**`); de hoofdsessie draait de volledige set op de unie. U5's "alle veertien gates
groen" is dus op deze tak voor vijf gemeten en voor negen niet.

**Voorgestelde registertekst.**
- CONSTRUCTS: geen rij — geen IR-construct en geen v1-delta, alleen de API van de backend.
- PLAN §9, na de alinea "The pin bump became the toolchain move": "**The distribution builds through one
  public entry (2026-09-26, card `publieke-api-distributie`):** `backends/drools` exposes
  `DroolsBackend.build(document, branch)` — a loaded `uchar-ir-v1` document (a `JsonNode` or a path) in, a
  record with the model, the bean loader, the branch's transpiler, its coverage manifest and the `KieBase`
  out — and `CaseRunner` builds every golden through it, so gate 12 plays what a consumer fires. It
  refuses in the replay's sentences. The short overloads of `ExecModelTranspiler.build` and
  `DrlTranspiler.build`, `DrlTranspiler.transpile(model)` and its public constructors are gone: each
  transpiler has one build that takes every side table `IrLoader.Loaded` returns, a witness holds that by
  reflection, and a null side table is refused. Gate 12 unchanged: 209 and 179."
- CLAUDE.md, "Repo relationships" (de hoofdsessie bij de landing; geen gate erbij, dus geen gateregel):
  "`uchar-dev/regelspraak-toolchain` (separate private repository) is the distribution — CLI, service,
  scenario runner, docker/helm — and consumes this repository as a git submodule, the submodule commit
  being the pin. It fires rule sets through `backends/drools`' public entry `DroolsBackend`, the replay's
  own prelude, never through a transpiler build of its own."

**Open punten.**
1. Botsing in de unie met `rekenjaar-als-waarde` (gezien in die worktree, alleen gelezen): die kaart
   verwijdert de evaluatiejaarweigering uit `CaseRunner.build`, `draagtRekenjaar` en de imports van
   `Spelling` en `Rekenjaar`, en schrapt `kern/Rekenjaar.java`. Hier staat die code nu in
   `DroolsBackend.java`: bij de unie daar weghalen (het commentaar "Rekenjaar is admitted and not
   executed…", de regel `if (draagtRekenjaar(document)) throw new Excluded(Rekenjaar.WEIGERING);`, de
   methode `draagtRekenjaar`, de imports `dev.uchar.regelspraak.ir.Spelling` en
   `dev.uchar.regelspraak.transpiler.kern.Rekenjaar`) en in de javadoc van `build` de woorden "the
   evaluation year," uit de `@throws`-lijst. Het tekstuele conflict in `CaseRunner.java` lost zich op met
   de versie van deze kaart. Hun hunks in de transpilers (DRL rond regel 100, exec vanaf regel 4333)
   raken de bouwingangen niet.
2. Elke nieuwe aanroep van een verwijderde ingang in een parallelle kaart is in de unie een compilefout
   (zo bedoeld); vervangen door de volle `build` of door `DroolsBackend`. Gemeten tijdens deze sessie:
   geen van de zes andere worktrees had zo'n aanroep toegevoegd.
3. Negen testbestanden kregen kleine wijzigingen (lijst onder U2); raakt een parallelle kaart dezelfde
   helper, dan is dat een tekstueel conflict van een paar regels.
4. `bronPaden` en `navigatieBindingen` hebben hetzelfde generieke type en de parametervolgorde verschilt
   tussen de twee `build`s, dus een verwisseling compileert. Voor consumenten is dat opgevangen doordat
   de facade de enige ingang is; een `build` die `IrLoader.Loaded` in zijn geheel neemt zou het sluiten,
   maar de kaart koos voor expliciete zijtafels. Standaardantwoord toegepast: zo laten.
5. De dienst van de distributie compileerde de beans één keer per dienst en bouwde daarna per scope;
   de facade compileert de beans per build. Moet de distributie één beanlader over scopes delen, dan is
   een tweede ingang nodig (document plus bestaande beanlader, nog steeds met alle zijtafels) — niet
   gebouwd, want de kaart legde de invoer vast. Standaardantwoord: eerst per scope via de facade.
6. `IrLoader.load(root)` (alleen het model) blijft publiek; wie daarmee bouwt, moet nu lege zijtafels
   expliciet meegeven. Geen actie.

## Lens en fixronde (2026-09-26, hoofdsessie)

Lens backend (unie cfa643d0 + samenvoegfix e6844fba): MERGEN, geen blokkade. Gemeten: één prelude (buiten
`DroolsBackend` roept geen hoofdcode een stap van de oude prelude aan); de facade geeft op zeven (tak, document)-paren
dezelfde weigerzin als de replay; de replay-uitvoer van de basis-jar en de api-jar is byte-gelijk over het corpus
(1134 regels, md5 gelijk); de zeven korte overloads compileren tegen de basis en geven zeven compilefouten tegen de
unie; de getuige wordt rood als de facade de defaults weglaat. Adviezen A1 (`@throws` noemt het rekenjaar nog), A2
(commentaar `CaseRunner.drlTak`), A11 (README "plays exactly what a consumer fires") zijn in de fixronde gefixt
(branch fix/graaf); A8 (verwisselbare zijtafels: gate 12 vangt een verwisseling op beide takken, 22 en 20 goldens
veranderen van uitkomst) blijft bewust zo — de facade is de enige ingang; A10 (tellingen in dit Result verschoven
door parallelle kaarten: gate 12 speelt op de unie 212 en de DRL-tak 182, JUnit 828).
Uitkomst fixronde (branch fix/graaf): A1, A2, A11 gefixt; A13 het scenariorapport faalt per case in plaats van te stoppen.
