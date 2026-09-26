# graaf-samenvoeging-en-gate: één regelgraaf in uchar, met de subtypesleutel, en een gate die haar vasthoudt

De regelgraaf (`backends/drools/src/main/java/…/graaf/RegelGraaf.java`,
233 regels; `ProducentConsument.java`, 122) is een lezing van de IR: welke
regel produceert en consumeert welk (objecttype, attribuut of kenmerk),
door de subtypeketen heen. IR-SPEC ch. 5 definieert de
afhankelijkheidsgraaf en haar drie randsoorten voor de stratificatie; de
verifier rekent die uit. De maintainer besloot op 2026-09-26 dat de graaf
in uchar blijft ("eigenaarschap volgt de IR-semantiek, niet de gatedekking
van vandaag"). Vandaag zijn er twee lijnen: uchars versie draagt het
navigatiepredicaat uit L2 (`genavigeerd`), de toolchainversie
(uchar-dev/regelspraak-toolchain, commit c333bea5 "Een element sleutelt op
zijn declarant, niet op wie het aanraakt": `elementVan`/`declarant`, de
⊑-sleutel; `ScopeKetenTest` in de dienst, 232 regels) sleutelt een element
op zijn declarant. Gemeten: 91 diffregels in `ProducentConsument`, 10 in
`RegelGraaf`; in uchar geen test en geen gate die de graaf raakt (alleen
een javadocregel in `CaseRunner` r. 1022). De distributie roept
`RegelGraaf.bouw`, `ProducentConsument.elementVan` en `verbruikt` aan
(vier compilefouten tegen uchars huidige API — onderzoeksrapport §4.3).

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Eén graaf: de ⊑-sleutel uit de toolchain samengevoegd met uchars
  navigatiepredicaat; de API die de distributie aanroept (`bouw`,
  `elementVan`, `verbruikt` met predicaat) is de samengevoegde API; de
  bouwer legt de signaturen in het Result vast. De graaf leest de
  `RegelSpraakModel`-records zoals de lader ze uit de wire terugbouwt, dus
  ze werkt van `ir.json` uit (een consument zonder bronnen krijgt dezelfde
  graaf) — de bouwer bewijst dat met een test: graaf(bron-route) ≡
  graaf(wire-route) op elke golden met `bron.rgs`.
- U2 Tests: de `GraafTest` van de oude toolchain en het graafdeel van
  `ScopeKetenTest` geporteerd op Jutterswet-fixtures (de fixtures van de
  oude toolchain zijn ©-materiaal waar ze TOKA-domeinwoorden dragen; de
  bouwer schrijft ze over): de ⊑-keten (element op supertype, lezing op
  subtype en andersom), de genavigeerde lezing, de kenmerklezing, de
  vouwbron.
- U3 Gate (**voorstel**, `scripts/check-graaf.sh`, protected; de
  hoofdsessie landt haar): de graaf wordt over elke golden's `ir.json`
  gebouwd via de lader (nooit de bron) en vergeleken met wat de spec bezit:
  de verifier krijgt een uitvoermodus die de ch. 5-afhankelijkheidsranden
  van een document afdrukt (**voorstel**, `spec/verifier`), de graaf drukt
  dezelfde projectie af, en de gate eist gelijkheid per golden, met een
  redendragende lijst voor goldens waar de twee lezingen bewust verschillen
  en een floor op het aantal vergeleken goldens. Blijkt de projectie niet
  scherp te definiëren (randsoorten, granulariteit), dan pint de gate in
  plaats daarvan per golden de knoop- en randtellingen in een lijst in het
  script (het check-noop-patroon, stale-failing) — de bouwer meet, kiest
  en verantwoordt.
- U4 Geen spec-wijziging (de graaf is een lezing van ch. 5, geen nieuw
  normatief feit); `backends/drools/README.md` één alinea over de graaf en
  haar gate.
- U5 Klaar: alle gates groen inclusief de nieuwe; gate 12's pins
  ongewijzigd; naamtabel-toets rc 0; geen D-/U-nummers in scripts en
  code.

## Acceptatie

- (a) U1–U5; (b) de vier aanroepvormen van de distributie compileren tegen
  de samengevoegde API (bouwer toont het met een kleine proef buiten de
  repo of een test); (c) de gate faalt aantoonbaar op een mutant (één rand
  weggelaten).

May touch: `backends/drools/**`, `spec/verifier/**` (alleen de
uitvoermodus, **voorstel**), `scripts/check-graaf.sh` (**voorstel**). Niet in
deze kaart: de frontend, het corpus, `CLAUDE.md` (hoofdsessie bij de
landing).

## Result (2026-09-26, bouwer)

### Stand

- Basis main `b88a8ccc`; claim `3d129688`; kop: de commit die dit Result toevoegt (gemeld in het eindbericht);
  werkboom schoon na die commit. Niets gepusht of gemerged.
- Beschermde paden in deze tak, als **voorstel**: `spec/verifier/` (de uitvoermodus) en `scripts/check-graaf.sh`
  (de nieuwe gate). `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md` en `.github/` zijn niet aangeraakt; hun
  tekst staat hieronder.

### Commits

- `2dc1bd47` verifier: de afhankelijkheidsranden van ch. 5 als uitvoer (voorstel)
- `df3bd52b` graaf: één regelgraaf, met de subtypesleutel, als lezing van ch. 5
- `7626e342` gate 15 (voorstel): de regelgraaf tegen de randen van de verifier
- `e29e7c44` drools-README: de regelgraaf en haar gate in één alinea
- `67aa9ecd` graaf: afhankelijkheden in één doorgang over de randen
- en de Result-commit.

### U1 — één graaf

De ⊑-sleutel uit de toolchain (`elementVan`/`declarant`, commit `c333bea5` daar) is samengevoegd met uchars
navigatiepredicaat. De samengevoegde API (pakket `dev.uchar.regelspraak.graaf`):

```java
public static RegelGraaf RegelGraaf.bouw(RegelSpraakModel model, Predicate<Conditie.Kenmerk> genavigeerd)
public static ElementSleutel ProducentConsument.elementVan(RegelSpraakModel.Objecttype houder, String naam, boolean kenmerk)
public static Set<ElementSleutel> ProducentConsument.verbruikt(RegelSpraakModel model, ResolvedRegel r,
                                                              Predicate<Conditie.Kenmerk> genavigeerd)
public static Map<ElementSleutel, Set<String>> ProducentConsument.producentIndex(RegelSpraakModel model)   // ongewijzigd
public static String ProducentConsument.basisNaam(String naam)                                             // ongewijzigd
public List<RegelGraaf.Afhankelijkheid> RegelGraaf.afhankelijkheden()                                       // nieuw
public record RegelGraaf.Afhankelijkheid(String producent, ElementSleutel element, String consument)        // nieuw
```

Het navigatieantwoord per route: wire `k -> geladen.navigatieBindingen().containsKey(k)`, bron
`k -> k.navigatie() != null`. Geen eenargumentvormen van `bouw` en `verbruikt`: een vaste gok zou elke
genavigeerde check van de andere route als eigen kenmerk lezen (getuige
`het_navigatiepredicaat_van_de_andere_route_leest_de_check_als_eigen_kenmerk`).

Eigen keuzes, vastgelegd:

1. **`verbruikt` kreeg het model erbij.** Een genavigeerde kenmerkcheck leest het kenmerk van het bereikte type,
   en het record noemt dat type niet (op de wire rijdt de hop naast het model, in de bron noemt het record de rol).
   De sleutel heeft alleen de declarant nodig: declareert precies één type in het model een kenmerk met die naam,
   dan is dat de sleutel en is de lezing exact; declareren er meer, dan leest de check het kenmerk van elk
   (meer melden, niet minder, zoals de ketensleutel). Daarvoor is het model nodig. Het predicaat van U1 blijft.
2. **De leeswandeling is herschreven** als uitputtende switch over `Expr` en `Conditie` die het onderwerp meedraagt,
   zoals `dep_expr`/`dep_cond` van de verifier. Gemeten met de nieuwe gate miste de rechte samenvoeging vier
   goldens: de genavigeerde kenmerkcheck (`anchor/jutterswet-kustwacht`, 2 randen), de kenmerkcheck in een
   vouwfilter (`jutterswet-strandgoed`, 1) en in een tak van een instance-set-unie (`jutterswet-uniekwantor`,
   `jutterswet-uniestratum`, elk 2).
3. **Een telbron is geen lezing van een lid.** De bron van een telling noemt de rol van de relatie; de rechte
   samenvoeging tekende die als invoerknoop, op de bronroute als `duinkavels · jutter` en op de wire als
   `duinkavel · jutter`, dus met een andere graaf per route op `jutterswet-kaveltelling` en
   `jutterswet-muntpeiling` (gemeten met de bronroutetoets tegen de rechte samenvoeging).
4. **De sleutelnormalisatie is `Namen.norm`** (ref's `norm`: ASCII-kleine letters, samengevoegde witruimte) in
   plaats van `toLowerCase(Locale.ROOT)` + `trim`, zodat de elementidentiteit de knoopidentiteit van ch. 5.1 is.
   Gemeten: geen naam in `corpus/golden` draagt een niet-ASCII-hoofdletter, dus geen uitkomst bewoog.

**Bewijs graaf(bronroute) ≡ graaf(wireroute)**: `GraafBronrouteTest` rendert voor elke golden met `bron.rgs` de graaf
van `Resolver.resolve(bron.rgs)` en van `IrLoader.loadAll(ir.json)` en eist byte-gelijke `mermaid()`: 93 van 93
gelijk. Gevoeligheid gemeten: met `k -> false` als wireantwoord valt `jutterswet-postwacht` om; met de rechte
samenvoeging vallen `jutterswet-kaveltelling` en `jutterswet-muntpeiling` om.

**Acceptatie (b)**, proef buiten de repo: één klasse in pakket `dev.uchar.regelspraak.toolchain.proef` met de vier
aanroepvormen van de distributie (`RegelSpraakCli:82`, `Playground:131`, `BeslisserKern:477` en `:484`), met `javac`
gecompileerd tegen de shaded jar (en, voor de eerste regel, tegen de graafbronnen van `b88a8ccc`) plus de klassen
van de frontend. De vormen met het navigatieantwoord:

```java
static RegelGraaf cli(RegelSpraakModel m) { var graaf = RegelGraaf.bouw(m, k -> k.navigatie() != null); return graaf; }
static String playground(RegelSpraakModel model) { return RegelGraaf.bouw(model, k -> k.navigatie() != null).mermaid(); }
static ElementSleutel invoerElement(RegelSpraakModel.Objecttype houder, String lid, boolean kenmerk) {
    return ProducentConsument.elementVan(houder, lid, kenmerk);
}
static int verbruik(RegelSpraakModel model, ResolvedRegel r) {
    int n = 0;
    for (ElementSleutel consumed : ProducentConsument.verbruikt(model, r, k -> k.navigatie() != null)) n++;
    return n;
}
```

Uitkomst:

- zoals de distributie ze nu spelt, tegen de graaf van `b88a8ccc`: `4 errors` (de vier uit onderzoeksrapport §4.3);
- zoals nu gespeld, tegen de samengevoegde API: `3 errors` (`elementVan` bestaat nu; `bouw` twee keer en
  `verbruikt` vragen bewust het navigatieantwoord);
- met het antwoord (`bouw(m, k -> k.navigatie() != null)`, `verbruikt(model, r, k -> k.navigatie() != null)`,
  `elementVan` ongewijzigd): compileert, rc 0.

De distributie past dus drie aanroepen aan en laat haar kopie van `graaf/` en haar `GraafTest` vallen.

### U2 — toetsen

Onder `backends/drools/src/bronroute/java/dev/uchar/regelspraak/graaf/` (buiten de Maven-build, die geen parser
meebouwt; de gate compileert ze tegen de klassen van de frontend en de jar). Elke fixture is Jutterswet en wordt via
`TweeRoutes.lees` langs beide routes gelezen; elke bewering geldt op beide grafen en de twee grafen moeten
byte-gelijk renderen.

- `GraafTest` (6): de vijf toetsen van de oude `GraafTest` op een Vondst-model (`gewicht` invoer, `zwaar` kenmerk,
  `toeslag` attribuut, `bergloon` eind-output), plus de afhankelijkheden als paden regel → element → regel.
- `GraafKetenTest` (5): het graafdeel van de oude `ScopeKetenTest` op Klipjutter ⊑ Jutter: sleutel op de declarant,
  soortbewuste sleutel (beide routes), het lid één keer getekend (producent op het supertype, lezing op het
  subtype) en andersom, vlak model onveranderd.
- `GraafLezingTest` (7): de genavigeerde kenmerkcheck en attribuutlezing (Wachter/Post), het navigatieantwoord van
  de andere route, een kenmerk met twee declaranten, de kenmerkcheck in een vouwfilter, de bron van een waardevouw
  en de bron van een telling.
- `GraafBronrouteTest` (93 dynamisch): het U1-bewijs over het corpus.

Rood eerst, gemeten met de commando's van de routepoot (jar gebouwd, `javac` tegen frontend en jar, JUnit-console)
op twee mutanten van `ProducentConsument`:

- mutant A (de rechte samenvoeging: de oude wandeling via `Expr.bevat`): 6 rood — de genavigeerde kenmerkcheck, twee
  declaranten, de telbron, de kenmerkcheck in een vouwfilter, en de routes van `jutterswet-kaveltelling` en
  `jutterswet-muntpeiling`;
- mutant B (sleutel op het houdertype, zonder declarant): 4 rood — de vier ketentoetsen.

### U3 — gate `scripts/check-graaf.sh` (voorstel)

De projectie bleek scherp te definiëren, dus de gate vergelijkt met de verifier en pint geen tellingen per golden.

- **Verifier-uitvoermodus** (`verifier --dependency-edges <ir.json>`): elke rand van de graaf die WF-045/046
  lopen, één tabgescheiden regel per rand, gesorteerd en zonder herhaling. WF-045/046 en het rapport lezen één
  lijst (`Checks.dep_edges`): `wf045_046` bouwt zijn opvolgerlijsten nu uit die lijst, in dezelfde volgorde, zodat
  de eerst gemelde cyclus dezelfde blijft. De gewone uitvoer is byte-identiek over alle 818 `ir.json` in `corpus/` en
  de pariteitszone (md5 `b070bf33e89a017bd4900744c1e00eaa` voor en na, `diff -r` leeg); ref en de ASP-backend, die
  `wf045_046` en `dep_forms` uit dezelfde bibliotheek linken, zijn groen in gates 5, 6 en 8.
- **Specpoot**: over elke `corpus/golden/**/ir.json` de graaf via de lader (`GraafProjectie`), tegen de
  verifierranden geprojecteerd op: beide vormen uit de `rules`-lijst, knoop `attr:` of `char:`, zonder randsoort
  en zonder het type in de knoop (de graaf sleutelt op de declarant, de verifier op het gespelde type met de
  schrijfsluiting over vergelijkbare typen). Gelijkheid per golden. `VERSCHIL_BEWUST` (één regel per
  geprojecteerde afhankelijkheid met reden, veroudert in beide richtingen) is leeg: geen golden verschilt.
- **Routepoot**: `GraafBronrouteTest` moet precies één keer per `bron.rgs`-golden draaien (gelijkheid, geen vloer),
  de unittoetsen met een vloer.
- **Vloeren** (gemeten 2026-09-26): `EXPECTED_MIN_GOLDENS=228`, `EXPECTED_MIN_AFHANKELIJK=45`,
  `EXPECTED_MIN_BRONROUTE=93`, `EXPECTED_MIN_TESTS=18`.

Uitkomst op de kop (letterlijk):

```
drools-jar: fresh (jar newer than 167 source files)
check-graaf: spec leg — 228 of 265 goldens compared, 45 dependencies equal to the verifier's ch. 5 edges (floors 228 / 45), 0 listed difference(s)
check-graaf: not comparable — 37 golden(s) the loader refuses, carrying 13 verifier dependencies nobody compared:
    15 Datatype:ListType
    7 Datatype:StructType
    6 Datatype:SumType
    4 functionDefs
    2 Expr:TemporalRead
    2 Expr:Match
    1 Kwantorvouw.bronPad
    anchor/jutterswet-vondstdossier: 1 dependencies (Datatype:StructType)
    jutterswet-kamerhuishouden-catala: 3 dependencies (functionDefs)
    jutterswet-vaarwatertoeslag-catala: 1 dependencies (Expr:Match)
    jutterswet-vangstwaardering: 2 dependencies (Datatype:StructType)
    synth3-besluitschakel: 2 dependencies (Datatype:SumType)
    synth3-nestwaarde: 2 dependencies (Datatype:StructType)
    synth3-vondststaat: 2 dependencies (Datatype:StructType)
check-graaf: route leg — the graph of the wire equals the graph of the source on all 93 goldens with a bron.rgs (floor 93); 18 unit tests green (floor 18)
check-graaf: PASS
```

**Acceptatie (c)**, rood op een mutant: één consumentrand weggelaten in `RegelGraaf.bouw` (de rand naar
`gevarenwacht bij risicopost`), uitvoer letterlijk:

```
drools-jar: rebuilt (2 source path(s) newer than the jar, e.g. backends/drools/src/main/java/dev/uchar/regelspraak/graaf/GraafProjectie.java; 5s)
the graph and the verifier's ch. 5 edges differ (golden, side that draws it, producer, node, member, consumer):
  anchor/jutterswet-kustwacht	verifier	risicopost bij verraderlijk vak	char	risicopost	gevarenwacht bij risicopost
check-graaf: FAIL — the rule graph disagrees with IR-SPEC ch. 5 on 1 dependency line(s) — fix the graph, or list a deliberate difference in VERSCHIL_BEWUST with its reason
```

Verder rood gemeten: een `VERSCHIL_BEWUST`-regel voor een verschil dat niet bestaat → `VERSCHIL_BEWUST is stale:
the entries above no longer differ (or their golden is gone or refused) — drop them`; mutant B → specpoot groen,
routepoot `the graph's unit tests are red (above), or skipped one`. In de eerste opzet van de gate slikte een
mislukte `sed` (een goldenpad als vervanging) elk verschil in; de mutantproef liet dat zien vóór de commit, en elke
stap die een verschil draagt controleert nu zijn eigen status.

### U4 — geen spec-wijziging

Geen tekst in `spec/IR-SPEC.md` of `spec/RUNTIME-SEMANTICS.md`; de uitvoermodus voegt aan de verifier alleen een
rapport toe. `backends/drools/README.md` kreeg één alinea ("The rule graph and gate 15").

### U5 — klaar

| gate | uitkomst |
|---|---|
| 1 `check-self-contained.sh` | PASS |
| 3 `check-verifier.sh` | PASS (307 golden clean; 481 negatief rejected, floor 350; 324 on their declared arm, floor 260; 584 pointers, floor 547) |
| 5 `check-oracle.sh` | PASS (ref suite 1047, floor 489; 709 cases, floor 427) |
| 6 `check-clingo.sh` | PASS (backend suite 1878 checks, floor 83; 709 cases replay exactly) |
| 8 `check-rood.sh` | PASS (43 red cases, floor 26; Drools leg 11 fault-pinned, 32 not-carried with reason) |
| 12 `check-drools.sh` | PASS (played 209 = pin; auto-excluded 52; value-divergence 1; DRL-text 179 = pin; JUnit 814, floor 239) |
| 15 `check-graaf.sh` (nieuw) | PASS (zie U3) |
| `python3 spec/proposals/v1-naamtabel-toets.py` | rc 0 (`v1-naamtabel-toets: OK`) |

Gate 12's pins zijn ongewijzigd (`EXPECTED_PLAYED`, `EXPECTED_DRL_PLAYED`, de lijsten); geen script behalve het
nieuwe is aangeraakt. Geen D-/U-nummers in scripts en code (grep over de hele diff: geen treffer).

### De zinnen letterlijk

Geen nieuwe weigerzin in verifier, ref, clingo of de Drools-lader. Nieuw:

- verifier, gebruik: `usage: verifier <ir.json> | verifier --dependency-edges <ir.json>`
- verifier, rapportregel: `<positive|strict|negative>\t<producer pointer>\t<producer name>\t<node>\t<consumer pointer>\t<consumer name>`
- `GraafProjectie`: `document\t<path>`, `rand\t<producent>\t<attr|char>\t<lid>\t<consument>`, of `weigering\t<de
  Refusal-zin van de lader>`
- gate, de dragende zinnen: `the rule graph disagrees with IR-SPEC ch. 5 on N dependency line(s) — fix the graph, or
  list a deliberate difference in VERSCHIL_BEWUST with its reason`; `VERSCHIL_BEWUST is stale: …`; `the route
  comparison is red: the graph of a golden's ir.json differs from the graph of its bron.rgs (above)`; `the route
  comparison ran N goldens, but M carry a bron.rgs — every one of them must be compared`; en de vloerzinnen in het
  patroon van de andere gates.

### Voorgestelde registertekst

**CONSTRUCTS** — geen nieuwe rij: de wire en de semantiek veranderen niet. Voorstel, één zin achter de Notes van rij
39 (overerving): "The Drools backend's rule graph (`backends/drools`, `graaf/`) keys a member on its DECLARING type,
the shorter rewrite IR-SPEC ch. 5.1 declines for the stratification graph, deliberately: it is a presentation and
scope reading that reports more rather than less; gate 15 holds its dependencies to the verifier's ch. 5 edges, and
two sibling subtypes sharing a member written on one and read on the other are its recorded difference (no golden
exercises it)."

**PLAN** (§9, na de alinea over de verhuizing): "**The rule graph merged and gated (2026-09-26, card
`graaf-samenvoeging-en-gate`):** one graph in `backends/drools/graaf`, a member keyed on its declaring type and the
caller answering whether a characteristic check navigates, read from the model the loader rebuilds from `ir.json`.
Gate 15 holds it to IR-SPEC ch. 5: its dependencies equal the verifier's edges (new: `verifier --dependency-edges`)
on every golden the loader reads (228 of 265, 45 dependencies), and its wire graph equals its source graph on the 93
goldens with a `bron.rgs`. The merge measured four reads the graph missed (a navigated characteristic check, and a
characteristic check in a fold filter and in a union branch) and a count source read as a member, which drew a
different graph per route on two goldens; the distribution adapts three call sites."

**CLAUDE.md**, gate 15 (na gate 14):

> 15. `scripts/check-graaf.sh` — the rule graph (`backends/drools`, `graaf/`) holds to IR-SPEC ch. 5, in two legs.
>     The SPEC leg reads every `corpus/golden/**/ir.json` through the backend's loader (never a source), prints the
>     graph's dependencies (`GraafProjectie`: rule → member → rule) and compares them per golden with the
>     verifier's ch. 5 edges (`verifier --dependency-edges`, the list WF-045/046 walk) projected onto forms of the
>     `rules` list and attribute/characteristic nodes, without the edge kind and without the type in the node (the
>     graph keys a member on its declaring type); a difference fails unless it is listed in `VERSCHIL_BEWUST`, one
>     projected line per entry with its reason, stale-failing in both directions; a golden the loader refuses is
>     reported with the refused form and the verifier dependencies nobody compared there. The ROUTE leg compiles
>     the tests under `backends/drools/src/bronroute/` against the frontend's classes and the backend jar: the
>     graph of every golden with a `bron.rgs` renders byte-identically from the source and from the wire (exactly
>     once per such golden), and the graph's unit tests pass. Floors in the script: `EXPECTED_MIN_GOLDENS=228`,
>     `EXPECTED_MIN_AFHANKELIJK=45`, `EXPECTED_MIN_BRONROUTE=93`, `EXPECTED_MIN_TESTS=18`, measured 2026-09-26.
>     An empty set anywhere is a failure, never silently green.

**CI**, voorstel voor `.github/workflows/gates.yml` (beschermd, niet aangeraakt):

```yaml
  graaf:
    # Gate 15 — the rule graph against the ch. 5 edges of the verifier. The spec
    # leg needs the verifier (dune + yojson) and the backend's shaded jar; the
    # route leg compiles backends/drools/src/bronroute against the frontend's
    # classes and runs the JUnit console, so it needs gate 10's tool jars too.
    name: "Gate 15: rule graph against IR-SPEC ch. 5 (unconditional, blocking)"
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - uses: ocaml/setup-ocaml@v3
        with:
          ocaml-compiler: "5"
      - name: install yojson
        run: opam install -y yojson
      - name: cache the ANTLR + JUnit tool jars
        uses: actions/cache@v4
        with:
          path: frontends/regelspraak/lib
          key: regelspraak-jars-antlr-4.13.2-junit-1.10.2
      - name: build the shaded Drools-runtime uber-jar
        run: mvn -B -f backends/drools/pom.xml package -DskipTests
      - name: check-graaf
        run: opam exec -- bash scripts/check-graaf.sh
```

### Open punten

1. **De specpoot ziet de ⊑-sleutel niet.** Geen golden heeft een regel→regel-afhankelijkheid over een subtypeketen:
   mutant B (sleutel op het houdertype) laat de specpoot groen (228/228) en valt alleen in de routepoot. Gemeten ter
   informatie: over `corpus/rood` en de negatiefdocumenten die de lader leest zijn 317 documenten vergelijkbaar, 0
   verschillen, en `corpus/negatief/wf045-erfattribuut-spelling-cyclus` is er de ketengetuige die mutant B vangt.
   Twee wegen: een golden met een afhankelijkheid over de keten in beide richtingen (corpuskaart), of de specpoot
   verbreden naar die zones (gatewijziging, maintainer).
2. **37 goldens niet vergelijkbaar**: de lader weigert ze (v1-vormen: ListType 15, StructType 7, SumType 6,
   functionDefs 4, TemporalRead 2, Match 2, een meerstapsbereik van een kwantorvouw 1); in 7 daarvan tekent de
   verifier 13 afhankelijkheden die niemand vergelijkt. Dat volgt de dekking van de lader, niet van de graaf.
3. **De graaf tekent alleen de vormen van de `rules`-lijst.** Verdelingen, object- en feitcreaties, consistenties,
   uniciteiten en defaultsgroepen zijn vormen van ch. 5 die ze niet tekent; de projectie beperkt de verifier daarom
   tot `/rules/rules/`. Een bredere graaf is een eigen kaart.
4. **Genavigeerde check op een kenmerk met meer declaranten**: de graaf leest het kenmerk van elke declarant (meer,
   niet minder; getoetst). Exact sleutelen vraagt het type dat de hop bereikt, en dat kan een predicaat niet
   dragen; een rijker navigatieantwoord (een functie naar het bereikte type: op de wire uit `navigatieBindingen`,
   in de bron uit de rol) maakt het exact, maar vervangt het predicaat dat U1 vastlegde. Besluitvraag voor de
   maintainer, omdat ze U1 raakt; standaardantwoord toegepast: het predicaat blijft, de overschatting staat
   vastgelegd. In grote
   regelsets met gelijknamige kenmerken op meerdere typen kan dit een scopecontrole in de distributie laten melden
   waar niets ontbreekt.
5. **Zustersubtypen**: de declarantsleutel verbindt een schrijver op het ene en een lezer op het andere zustertype;
   de vergelijkbaarheidswandeling van de verifier niet. Tweede bekende, bewuste verschilklasse; geen golden.
6. **CI**: gate 15 heeft nog geen job (`.github/` is beschermd; voorstel hierboven). Tot die landt, draait geen
   CI-job een toets van de graaf: de Maven-suite van gate 12 bevat er geen.
7. **Gatenummer 15 is voorlopig**; parallelle kaarten kunnen ook een gate toevoegen. Het nummer staat in de kop van
   het script en in de README-kop; de hoofdsessie nummert bij de unie.
8. **De distributie** past drie aanroepen aan (zie U1), laat haar `graaf/`-kopie en `GraafTest` vallen, en het
   dienstdeel van haar `ScopeKetenTest` roept voortaan `verbruikt(model, r, k -> k.navigatie() != null)` aan.
9. **`basisNaam`** stript een groepsvoorvoegsel ("groep: ") en een beslistabelachtervoegsel (" (n)"), conventies uit
   v0. Geen van de 553 regelvormen in `corpus/golden` draagt ze (gemeten); draagt een golden ze ooit, dan valt de
   specpoot luid, omdat de graaf dan samenvoegt wat de verifier scheidt.
