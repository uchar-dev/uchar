# De DRL-teksttak meetbaar: de tweede transpiler onder het harnas

Goal: gate 13 (het annotatie-onverschilligheidsharnas) speelt
uitsluitend de exec-model-tak — de replay-runner bouwt alleen die — en
de ~27 EffectieveEenheid-omzettingen in `DrlTranspiler` zijn daardoor
enkel gecompileerd, gedragen door drie JUnit-getuigen die geen eenheden
raken (B-lens-2-meting 2026-09-04: op het corpus wijken vier call-sites
ooit van de annotatie af, alle vier exec-model). Maak de DRL-tak
meetbaar (maintaineropdracht 2026-09-04, tweede golf).

Aanpak: EERST meten. (a) Kan de replay-runner een DRL-leg krijgen — de
teksttranspiler bestaat; wat kost het om een case door de
gegenereerde DRL te spelen (KieBase uit DRL-tekst i.p.v. exec-model),
en spelen de 106 goldens daar überhaupt (de tekst-tak kan eigen
vorm-grenzen hebben — inventariseer de weigeringen)? (b) Zo niet of te
duur: mutatie-gemeten JUnit-getuigen per omgezette DRL-plek (het
EffectieveEenheidWitnessTest-patroon: per plek een getuige die op de
annotatie-terugval-mutant rood is). Kies op de meting, motiveer in de
kaart, en bouw het gekozen spoor. Een harnas-/gate-13-uitbreiding gaat
als voorstel (scripts/ is beschermd) en de gate mag alleen groeien als
hij groen is.

Acceptance:
- De meting gedocumenteerd (DRL-speelbaarheid per golden of de
  onhaalbaarheidsmotivering).
- Elke omgezette DRL-plek aantoonbaar gedekt: harnas-leg met
  mutant-demonstratie, óf getuigen die op de plekmutant rood zijn.
- Alle dertien gates groen; gate-13-wijzigingen als voorstel, alleen
  groen toegevoegd.

May touch: `backends/drools/src/**` (incl. het kaart-eigen
testbestand), `scripts/check-reftype-derivation.sh` (voorstel),
`backlog/`.

Out of scope: frontends, verifier, spec/IR-SPEC.md; RUNTIME-SEMANTICS
alleen als de gate-vorm normatief verandert — dan eerst de vraag.

## De meting (2026-09-04)

### M1 — is de DRL-teksttak speelbaar?

Ja. Wat ontbrak was geen semantiek maar twee dingen op het bouwpad.
`CaseRunner` bouwde uitsluitend de exec-model-KieBase; de teksttak had
nooit een compileerpad. Gemeten, in deze volgorde:

1. **De exec-model-DRL-route werkt niet.** `KieHelper.build(Executable-
   ModelProject.class)` — de route die géén extra afhankelijkheid vraagt —
   struikelt over de gegenereerde tekst zelf: de mvel-codegen herschrijft
   `$subject.vondstrecht` binnen `java.util.Objects.equals(...)` niet naar
   de getter, en ECJ weigert:

   ```
   text=The field Vondst.vondstrecht is not visible
   ```

2. **De klassieke DRL-route werkt wél**, en precies zoals de
   productietoolchain hem bedoelde (`dialect "mvel"`). Hij vraagt één
   afhankelijkheid die `drools-engine` niet meebrengt:

   ```
   java.lang.RuntimeException: Unable to load Dialect 'mvel'.
   Please add drools-mvel among your dependencies
   ```

   Met `org.drools:drools-mvel` erbij compileert de gegenereerde tekst
   tegen de in-memory bean-classloader zonder verdere ingrepen:
   `KIEBASE OK (classic, in-memory beans): packages=2 rules=1`.

**De volle sweep**, 152 goldens door beide takken (`java -jar $JAR [--drl]
ir.json case*.json`), 2 min 5 s totaal (~0,5 s per golden per tak):

| exec | DRL | aantal |
|---|---|---|
| PLAYED | PLAYED | **69** |
| EXCLUDE | EXCLUDE | 40 |
| PLAYED | EXCLUDE | 28 |
| PLAYED | FAIL | 7 |
| EXCLUDE | **PLAYED** | 3 |
| PLAYED | ERROR | 2 |
| FAIL | FAIL | 1 |
| (geen case) | — | 2 |

Dus: **69 goldens spelen door de teksttak met exact dezelfde gepinde
verwachtingen** die gates 5, 6 en 12 pinnen. Wat de teksttak wél draagt en
de exec-tak niet: drie goldens die de exec-transpiler SKIPt als "waarde
buiten scope" (`vindersloon`, `vondsttarief`, `afrondingscombinaties`).

De vormgrenzen van de teksttak, gemeten:

- **34 loader-weigeringen** — identiek aan de exec-tak (zelfde loader).
- **16 NOT-LOWERED** — de teksttak markert waar de exec-tak verlaagt: het
  ch.-4.10 Let-cluster (9×, "TODO expressie: …"), een SOM-aggregatie (3×),
  een enum-literal, een kenmerkvoorwaarde, een consistentieregel.
- **2 DRL-COMPILE** — `erflijn` en `gemeenteberging`: mvel strict-mode kan
  `erflater == $subject` niet resolven (geen getter op de bean).
- **7 waarde-divergenties** — temporele schrijfacties (`borgdeel`,
  `dagentelling`), de leeg-doorval (`waardering-v0vorm`, `waardestelling`),
  de rekenkamer-objectcreatie (2×), `keurmerk`.
- **2 fire-ERRORs** — de objectcreatie-consequentie in mvel:
  `NullPointerException: Cannot invoke "java.lang.reflect.Method.
  getDeclaringClass()" because "method" is null`.
- **ch. 7 prioritizedDefaults heeft de teksttak niet** — dat is nieuw
  gemeten en het gevaarlijkste van de lijst, want zonder weigering zou de
  KieBase de default stilzwijgend weglaten. De runner weigert die vorm nu
  hardop.

### M2 — de call-site-inventaris van de teksttak

Met een tijdelijke `StackWalker`-sonde op `EffectieveEenheid.van` (daarna
exact hersteld; `git status` schoon), die elke aanroep aan zijn
transpilerframe toeschrijft. Over het HELE corpus, onverdraaid:

| plek | aanroepen | eenheiddragend | goldens |
|---|---|---|---|
| `DrlTranspiler.regel:1281` (schrijfgrens) | 272 | 32 | 63 |
| `DrlTranspiler.conditie:3236` (§5.1 Tabel 32) | 92 | 6 | 28 |
| `DrlTranspiler.binairTerm:1798` (additieve grens) | 70 | 15 | 22 |
| `DrlTranspiler.regel:1120` (temporele waarde) | 10 | **0** | 6 |
| `DrlTranspiler.temporeleWaarde:2388` | 8 | **0** | 5 |
| `DrlTranspiler.concatenatieAggregatie:2136/2142` | 6+6 | **0** | 2 |
| `DrlTranspiler.vergelijkingTijdreeks:2271` | 4 | **0** | 2 |
| `DrlTranspiler.waarde:1883/1884/1885` (Begrensd) | 3+2+2 | **0** | 1 |

Dat is de hele bereikte verzameling: **11 van de 27 omgezette plekken
worden ooit aangeroepen, en slechts 3 zien ooit een eenheiddragende
expressie.** De andere 8 bereikte plekken zijn op dit corpus doorgeefluiken
per constructie, niet per toeval — er is geen eenheid om te herschalen. 16
plekken worden door geen enkele golden bereikt.

Op de VERDRAAIDE kopieën (11 verdraaibare, speelbare goldens) overrulet de
afleiding de annotatie op exact die drie: `regel:1281` 20×,
`binairTerm:1798` 10×, `conditie:3236` 4×. De exec-tak, met dezelfde sonde
hermeten: vijf plekken (`regel:635` 20×, `scalarWaarde:3748` 14×,
`leafPredicate:3187` 2×, `regel:546` 2× en — nieuw sinds de vorige kaart,
door de gegroeide corpus — `voorwaarde:2848` 1×, de attribuut-vs-parameter-
vergelijking).

### De keuze: spoor (a), en waarom

Spoor (a). De meting is niet marginaal: 69 goldens spelen, de leg kost 22
extra JVM-starts (twee per verdraaibare golden) op een gate die in 40 s
klaar is, en — doorslaggevend — de DRL-leg raakt een golden die GEEN andere
leg van dit harnas kan raken
(`jutterswet-vondsttarief`: exec EXCLUDE, DRL PLAYED). Spoor (b) zou voor
diezelfde drie levende plekken handgeschreven documenten hebben opgeleverd
waar het corpus ze al aanlevert.

Spoor (b) is niet weggevallen maar **verplaatst**: het is het enige
overgebleven middel voor de 24 plekken die het corpus niet met een eenheid
bereikt, en dat is nu gemeten in plaats van vermoed (de tabel hierboven).
Wat wél uit (b) is meegenomen — omdat het goedkoop was en gate 12 raakt in
plaats van gate 13 — staat onder "De tekstgetuige".

## Wat er is gebouwd

### 1. De runner-ingang (`--drl`)

`CaseRunner` krijgt een tweede tak-keuze; het gate-12-pad is letterlijk
ongewijzigd (`build(irPath)` delegeert naar `build(irPath, false)` en komt
uit op dezelfde regels). De DRL-tak weigert drie vormen hardop, elk met
reden: een ch.-7-prioritizedDefaults-uitbreiding (die verlaging bestaat
alleen in de exec-tak — zonder weigering zou de KieBase de default stil
weglaten), elk construct dat de teksttak markert (de DRL compileert dan
gewoon, en dat is juist waarom een marker geweigerd moet worden in plaats
van gevuurd), en een DRL die de compiler afkeurt. `DroolsBackendMain`
krijgt de vlag; zonder vlag verandert er niets aan zijn pad.
`backends/drools/pom.xml` krijgt `drools-mvel` erbij, met de reden ter
plaatse — en dat is een **overschrijding van May touch**, hier hardop gemeld:
`backends/drools/pom.xml` valt buiten `backends/drools/src/**`; overschrijding
gemeld en door de hoofdsessie gezegend (2026-09-04) — pom.xml is geen protected
path.

### 2. De DRL-leg in het harnas (voorstel, `scripts/`)

Dezelfde verdraaiingen, dezelfde onverschilligheidseis, en — net als de
exec-leg — vergeleken met zijn EIGEN onverdraaide statusregel, nooit met
`PLAYED`: de teksttak is smaller, en dat herkauwt dit harnas niet.

**Rood-eerst, de mutant-vangst.** `EffectieveEenheid.van()` terug op
`e.type()` (de stand van vóór de vorige kaart) → 19 FAIL-regels, waarvan
**10 van de exec-leg en 9 van de DRL-leg**. De verdeling is het bewijs dat
de legs elkaar niet herhalen:

```
FAIL: jutterswet-vondsttarief — the DRL-text branch answers differently on the lying copy:
  untouched: PLAYED 1
  lying:     FAIL case.json: kenmerk standaardbelast: verwacht aanwezig, werkelijk afwezig
```

— `vondsttarief` staat er ALLEEN met een DRL-regel (de exec-tak sluit hem
uit, in beide runs, en zwijgt dus terecht). Omgekeerd staan `lichterloon`
en `vondstrecht` er alleen met een exec-regel: die markert de teksttak (het
Let-cluster), dus daar is niets te herschalen en niets te vangen.

**De basisklem.** Een onverschilligheidsleg die tegen zijn eigen basis legt,
is groen te houden door een tak die eenheden HELEMAAL niet meer herschaalt:
dan is de basis in beide runs even fout. Voor de exec-tak vangt gate 12 dat;
voor de teksttak vangt niets het — gate 12 speelt alleen de exec-tak. Dus
pint het harnas nu ook de BASIS van de teksttak: die moet PLAYED zijn (de
absolute corpusverwachtingen gehaald) of EXCLUDE (vorm niet gedragen, met
reden); alles anders hoort in `DRL_BASIS_UIT` met zijn reden. Vandaag leeg.

Gedemonstreerd dat hij bijt, met een mutant die precies dat gat maakt — de
schrijfgrens van de teksttak herschaalt niet meer (`if (false && …)` op
`DrlTranspiler.regel:1280`):

```
onverschilligheidsklachten van de DRL-tak: 0
FAIL: jutterswet-sleepbaan — the DRL-text branch neither played nor refused the UNTOUCHED golden, so its
      value is pinned by nothing:
  FAIL case1.json: attribuut sleeplengte: verwacht 2800, werkelijk 2.8
FAIL: jutterswet-vergunningsduur — … verwacht 27, werkelijk 2.25
```

De onverschilligheid bleef dus keurig groen (beide runs even fout) en
alleen de klem vond het. En de verouderingswacht, beide helften, met twee
verzonnen entries in een scriptkopie:

```
FAIL: stale DRL-baseline entry:
  verzonnen-golden — no such golden
  jutterswet-meldloon — the DRL-text branch plays or refuses it again; drop the entry
```

### 3. De tekstgetuige (nieuw testbestand, gate 12)

`DrlEenheidsplekWitnessTest` stelt dezelfde vraag één niveau lager: het ent
de verdraaiing in-memory op elke afgeleide positie van elke golden en eist
dat de UITGEZONDEN DRL byte-identiek is. Winst boven de harnas-leg: het
draait in gate 12 (onvoorwaardelijk en blokkerend, waar gate 13 een aparte
gate is), het noemt de REGEL die verschoof in plaats van een verschoven
getal, en het heeft geen casebestand en geen speelbare golden nodig — alleen
een document dat de loader aanneemt.

Mutatiegemeten met dezelfde `van()`-mutant: rood op 9 goldens, met o.a.

```
jutterswet-sleepbaan — line 15: [ … rsHerschaalExact($subject.getAanloop(),
  BigDecimal("1"), BigDecimal("1000")) … BigDecimal("1000"), BigDecimal("1") … ]
  became [ … BigDecimal("10"), BigDecimal("1000") … BigDecimal("100000000"),
  BigDecimal("10") … ]
```

**Eerlijk over wat het niet toevoegt:** dat zijn dezelfde 9 die de
harnas-leg vangt. De drie andere verdraaibare goldens (`lichterloon`,
`vondstrecht`, `seizoenvondst`) blijven onder de mutant groen, en niet bij
toeval: hun eenheiddragende posities zitten onder een construct dat de
teksttak MARKERT, dus er wordt daar geen herschaalfactor uitgezonden die een
leugen kan verschuiven. Ruimer bereik, identieke vangst — vandaag. Dat staat
zo in de javadoc, niet als belofte maar als meting.

## Wat ongedekt blijft, en waarom dat een meting is

24 van de 27 omgezette DRL-plekken zijn door geen corpusartefact te dekken,
omdat het corpus ze niet met een eenheid bereikt (M2-tabel). Acht daarvan
worden wél aangeroepen maar altijd eenheidloos — de temporele §4.1/§5.1.4-
herschalingen, de Begrensd-waardetak, de §4.6-Eenheid1-fold, de
§8.2-periodevergelijking; zestien worden helemaal niet aangeroepen. Voor die
24 is spoor (b) het enige middel, en dat vraagt handgeschreven documenten:
een verklaring in de declaratie, een converteerbaar zusje op de annotatie,
en een constructie die de plek daadwerkelijk aandoet. Dat is een eigen kaart
waard; het is geen restje van deze.

Het harnas zegt beide getallen nu hardop in zijn hoofd (11 bereikt, 3 ooit
overrulend), zodat een groene regel niet gelezen wordt als dekking die hij
niet heeft.

## Gates (alle dertien groen)

```
1  check-self-contained: PASS
2  check-ir-schema: PASS (schema valid; 174 file(s) validate and round-trip)
3  check-verifier: PASS (174 golden clean; 238 negatief rejected for their declared rule)
4  check-case-schema: PASS (schema valid; 425 case file(s) validate)
5  check-oracle: PASS (403 cases replayed exactly; …)
6  check-clingo: PASS (403 cases replay exactly through clingo — ref = clingo over the corpus)
7  check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors …)
8  check-rood: PASS (22 red cases fault observably in ref and clingo);
                Drools leg PASS (5 fault-pinned, 17 not-carried with reason)
9  check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically
10 check-regelspraak: PASS — JUnit suite green (159 tests); 28 rgs golden(s) re-emit byte-identically
11 check-toka-parity: PASS (ref 29/0, clingo 29/0, Drools 29 PLAYED/0)
12 check-drools: played 106 / auto-excluded 43 / value-divergence 1 / no-case 2 / JUnit 199
13 check-reftype-derivation: PASS (52 unit annotations twisted over 11 goldens; 50 executor
   replays reproduce the untouched outcome, 9 of them PLAYED by the DRL-text branch;
   0 pinned survivors)
```

Gate 12 is op elk getal ongewijzigd (106 / 43 / 1); alleen de JUnit-telling
groeit, **198 → 199**, nul overgeslagen, door de ene nieuwe tekstgetuige.
Gate 13 groeide met de DRL-leg en is groen toegevoegd: 11 gecontroleerde
statusregels extra (39 → 50), elk uit een basis- en een verdraaide DRL-run.

## Voor de hoofdsessie

- `CLAUDE.md`'s gate-13-alinea (beschermd pad, niet aangeraakt) zegt "ref,
  clingo and the Drools backend MUST all reproduce". Dat is nu te smal: het
  zijn ref, clingo en BEIDE Drools-transpilertakken, plus de basisklem op de
  teksttak. Eén zin, als de leg landt.
- De May-touch-overschrijding hierboven: `backends/drools/pom.xml` valt buiten
  `backends/drools/src/**`; overschrijding gemeld en door de hoofdsessie
  gezegend (2026-09-04) — pom.xml is geen protected path. De wijziging is één
  dependency (`drools-mvel`), met de gemeten consequentie in de pom zelf.

## Result (2026-09-04)

Geland: (1) de meting die de kaart vroeg — de DRL-teksttak IS speelbaar: 69
goldens spelen door beide takken met dezelfde gepinde verwachtingen, langs
de KLASSIEKE DRL-route (de exec-model-codegen-route strandt op de eigen
tekst, de klassieke vraagt alleen `drools-mvel`) — met haar vormgrenzen en
de 27-plekken-inventaris (11 bereikt, 3 eenheiddragend); (2)
spoor (a): een `--drl`-ingang in `CaseRunner`/`DroolsBackendMain` naast — niet
in — het gate-12-pad, en een DRL-leg in `scripts/check-reftype-derivation.sh`
(voorstel) die de mutant vangt op negen goldens, waaronder één die de
exec-leg niet kan bereiken; (3) de basisklem, die het gat dicht dat een
onverschilligheidsleg zonder waarde-gate open laat, met een eigen
mutant-demonstratie en verouderingswacht; (4) `DrlEenheidsplekWitnessTest`,
één mutatiegemeten tekstgetuige in gate 12's suite (JUnit 198 → **199**).
Alle dertien gates groen; gate 12 op elk getal ongewijzigd.

Niet aangeraakt: `ref/`, `backends/clingo/`, `spec/**`, `frontends/`,
`corpus/**`, `CLAUDE.md`, `.github/`, bestaande scripts en testbestanden.

Open, met reden: de 24 DRL-plekken die het corpus niet met een eenheid
bereikt (spoor (b), eigen kaart); de 7 waarde-divergenties en 2 fire-ERRORs
van de teksttak over het hele corpus (gate-12-terrein, hier bewust niet
overgedaan); en de twee DRL-compileweigeringen (`erflijn`,
`gemeenteberging`) die op een ontbrekend beanveld wijzen dat de exec-tak
langs een andere weg oplost.

## Fixronde (2026-09-04)

Na lens 1, die alle metingen reproduceerde en één blokkerend punt vond plus
vier kleinere.

### F1 (blokkerend) — de DRL-leg had geen bodem

De basisklem eist PLAYED óf EXCLUDE **per golden**, dus een teksttak die
ÁLLES weigert haalt hem voor élke golden, is per constructie onverschillig
(beide runs weigeren gelijk) en laat de leg stil groen achter terwijl hij
niets meet. Gereproduceerd met precies die mutant — een weigering vooraan de
DRL-tak van `CaseRunner` — vóór er iets gerepareerd werd:

```
check-reftype-derivation: PASS (52 unit annotations twisted over 11 goldens; 50 executor
  replays reproduce the untouched outcome, 0 of them PLAYED by the DRL-text branch;
  0 pinned survivors)                                                        exit 0
```

Negen gespeelde goldens naar nul, en de gate zei PASS. Gerepareerd met een
bodem naast de bestaande `n_twists`-wacht. Dezelfde mutant, mét de bodem:

```
FAIL: the DRL-text branch PLAYED no golden — the leg measured nothing (an empty played
      set is never green)                                                    exit 1
```

Mutant weer weg (`git diff` op `CaseRunner.java` leeg), gate weer groen met
`n_drl` = 9 — de leg meet weer wat hij vóór de mutant mat.

Lens-bevinding 5 staat in het commentaar bij de bodem: geteld wordt de
STATUSKOP, dus een golden waarvan élk casebestand `expectedFault` draagt
speelt niets en drukt tóch "PLAYED 0" af. De zonediscipline die gate 4 pint
(golden draagt `expected`, rood draagt `expectedFault`) maakt die vorm vandaag
onbereikbaar — genoemd zodat de bodem er niet ongemerkt op gaat leunen.

### F2 — het scripthoofd telde verkeerd

"The remaining 16" sloeg acht plekken over: 27 − 3 = **24** ongedekt, en dat
zijn twee verschillende feiten. Acht plekken wórden bereikt maar zijn op dit
corpus altijd eenheidloos (de temporele §4.1/§5.1.4-herschalingen, de
Begrensd-waardetak, de §8.2-tijdreeksvergelijking, de twee
concatenatie-aggregatieplekken); zestien worden door geen enkele golden
bereikt. Het scripthoofd zegt het nu zo, gelijk aan "Wat ongedekt blijft".

### F3 — de May-touch-overschrijding staat er nu

Zie sectie 1 en "Voor de hoofdsessie": `backends/drools/pom.xml` valt buiten
`backends/drools/src/**`, gemeld en gezegend.

### F4 — wat `drools-mvel` werkelijk verwisselt

Hermeten tegen een jar die zonder de dependency is gebouwd, want "alleen de
teksttak" was te mooi. Het artefact registreert **vijf** globale KieServices in
`META-INF/services`, elk één keer per JVM opgelost, dus óók op het vlagloze
exec-pad in werking. Drie hadden helemaal geen provider (`CoreComponentsBuilder`
— wiens `get()` "compile a Drools asset without mvel" wierp —,
`FieldAccessorFactory`, `ClassBuilderFactory`); twee verdringen een ingebouwde
standaard: `CoreComponentFactory` (`DroolsCoreComponentFactory` →
`MVELCoreComponentFactory`, waarmee knowledge packages `MVELKnowledgePackageImpl`
worden mét een mvel-`ClassFieldAccessorStore` waar de standaard er geen had) en
`ConstraintBuilder` (`DummyConstraintBuilder` → `MVELConstraintBuilder`).
Klassenlading gemeten op gate 12's eigen exec-runs (`-Xlog:class+load`): die
eerste verwisseling is levend op **alle 106** gespeelde goldens, en 11 daarvan
raken bovendien mvel's `LambdaIntrospector`.

De aanspraak is dus niet "het exec-pad raakt hem nooit" maar "raken verandert
niets": elk van de 150 goldens met een casebestand drukt een **byte-identieke**
exec-statusregel af mét en zonder het artefact, en gate 12 is op elk getal
gelijk (106 / 43 / 1). Kosten: de shaded jar 13,8 → 15,3 MB. Dat staat nu zo in
de pom, in plaats van de te smalle claim.

### F5 en F6 — twee kleine

`EffectieveEenheid`'s "CaseRunner builds only the exec-model back-end, so that
is the leg every replay takes" is sinds deze kaart onwaar: `CaseRunner` bouwt
beide takken. Herschreven — het gate-12-pad blijft exec-only, de vlag kiest de
teksttak. En `DroolsBackendMain`'s ERROR-regel wordt platgeslagen en begrensd
zoals het DRL-compileerpad het al deed: elke lezer neemt de LAATSTE regel als
statusregel, dus een meerregelig exceptiebericht duwde de ERROR-kop eraf en de
klem-diagnose met zich mee.

### Gates na de fixronde

```
1  check-self-contained: PASS
12 check-drools: PASS — played 106 / auto-excluded 43 / value-divergence 1 /
                 no-case 2 / JUnit 199 (op elk getal ongewijzigd)
13 check-reftype-derivation: PASS (52 unit annotations twisted over 11 goldens;
                 50 executor replays reproduce the untouched outcome, 9 of them
                 PLAYED by the DRL-text branch; 0 pinned survivors)
```
