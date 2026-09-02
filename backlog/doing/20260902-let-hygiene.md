# Hygiëne: de Let-restpunten (testcommentaar, verifier-terminologie, defaults-golden)

Goal: Drie kleine, elk al gemeten restpunten uit de Let-ronde in één
veegbeurt. (1) Twee bestaande testbestanden dragen nog commentaar in de
oude STRIKTE Let-semantiek: de klassejavadoc van
`LetDragerWandelingWitnessTest` (de zinnen die de Bouwnotitie van de
companion-kaart exact opsomt: "the two paths a walk must NOT see
through", "descending there fabricates a companion for code that never
executes", en verwanten) en `LetClusterWitnessTest` rond r. 93 ("the
body never runs" — onder ch. 10.6-L draait de body wél; de lezing van de
binder forceert de lege need en de barrière valt op de LEESPLEK).
Maintainer-mandaat 2026-09-02: UITSLUITEND commentaar/javadoc in exact
deze twee bestanden; geen assertie, methode of pin wijzigt. (2)
`spec/verifier` draagt nog "strict Let"-vocabulaire in meldteksten
(o.a. de WF-077-melding "route state reads through a strict Let
binding") — sinds de L-unificatie is de Let call-by-need; de meldingen
horen de spec-terminologie te volgen zonder gedragswijziging. (3) De
prioritizedDefaults-consequentie draagt Lets gemeten-werkend maar
corpus-ongedekt: geen golden oefent een Let in een defaults-consequentie
door de gates — één kleine golden dicht dat (Jutterswet-sfeer, eigen
werk).

Acceptance:
- De twee testbestanden ch. 10.6-L-conform in hun commentaar; `git diff`
  toont UITSLUITEND commentaar-/javadocregels in die twee bestanden;
  de JUnit-suite draait ongewijzigd groen met dezelfde telling.
- `grep -ri "strict let" spec/verifier/` leeg; de verifier wijst exact
  dezelfde negatiefs af met dezelfde WF-IDs (gate 3 telling ongewijzigd
  op de nieuwe golden na); meldtekst-diffs opgesomd in de kaart.
- `corpus/golden/<nieuwe naam>/` met ir.json + minstens één case waarvan
  de consequentie van een prioritizedDefaults-groep een Let draagt;
  verwachtingen EERST door ref gemeten, door clingo bevestigd (gates
  5/6); gate-7-gedrag en de gate-12-status (played of reden-dragend
  auto-excluded) gemeten en gerapporteerd, nooit aangenomen.
- Alle twaalf gates groen; tellingsverschuivingen expliciet.

May touch: `backends/drools/src/test/java/**` (ALLEEN commentaar in de
twee genoemde bestanden), `spec/verifier/**` (alleen meldteksten,
proposal), `corpus/golden/<de nieuwe golden>/**`, `backlog/`.

Out of scope: asserties of testgedrag; verifier-semantiek; IR-SPEC- en
RUNTIME-SEMANTICS-teksten; bestaande goldens en pins; de
DrlTranspiler.

## Bouwnotitie (subagent, 2026-09-02)

Drie punten, drie commits, alles in de worktree gemeten.

**(1) Testcommentaar — vier plekken, uitsluitend commentaar.** In
`LetDragerWandelingWitnessTest`: de openingszin van de klassejavadoc
("the two paths a walk must NOT see through" → "the two paths on which a
companion must NOT FIRE"), bullet 4 (de wandelingen dálen nu wél in
beide paden af — onder call-by-need draait de body altijd, dus
blindheid dáár zou een echte fout missen; wat de companion stil houdt is
de sonde die de drager her-evalueert), en het commentaar in
`een_deling_in_de_niet_genomen_tak_levert_geen_fout` (de twee zinnen die
de Bouwnotitie van de companion-kaart opsomt: "would fire anyway … cannot
see which branch ran" en "the walk descends only into the CERTAINLY
evaluated children"). In `LetClusterWitnessTest` r. 93: "the body never
runs" → de body draait wél, maar de lezing van `ink` forceert de lege
need en de barrière valt op die LEESPLEK (clause 2). De sectiekop "4. the
other half: a companion the walk must NOT fabricate" is blijven staan —
die klopt nog als eis. Bewijs: `git diff -U0` op commit 2996a44 toont
uitsluitend `//`- en ` *`-regels; geen assertie, methode, helper of pin
beweegt; de suite draait ongewijzigd **153 tests, 0 failures, 0 errors,
0 skipped** (BUILD SUCCESS).

**(2) Verifier-meldteksten — drie meldingen, negen commentaarplekken.**
`grep -ri "strict let" spec/verifier/` is nu leeg. De meldteksten
(voor → na):

| # | voor | na |
|---|---|---|
| WF-077 operand | "… an attribute, field or characteristic read must pass through a **strict** Let binding first" | "… must pass through a Let binding first" |
| WF-077 conditie | "… route state reads through a **strict** Let binding)" | "… route state reads through a Let binding)" |
| WF-069 | "an IfThenElse is admissible only inside a lambda, function or **strict** let body" | "… only inside a lambda, function or let body" |

Verder alleen commentaar: `ast.ml` (de Let-node beschrijft nu
call-by-need mét de barrière op de leesplek, en is **emitter-produced**
sinds de let-lowering — de oude zin "Never emitter-produced" was óók
onwaar geworden), `loader.ml` (het woord "strict" daar expliciet in de
loader-betekenis gezet, want dat is de betekenis die de rest van dat
bestand eraan geeft), `checks.ml` (`body_pos`-doc, `Let_pos`, de
WF-077-kop, de twee closure-commentaren, de WF-076-kop, de WF-069-kop en
`dep_expr` — die laatste met de toevoeging dat call-by-need de
afhankelijkheidsgraaf niet versmalt: ch. 6.1 is een statische
overbenadering). Geen enkele negatief pint een meldtekst (de WF-id komt
uit de mapnaam), gecontroleerd met een repo-brede grep. Gate 3 vóór:
**168 golden clean / 206 negatief**; na de tekstronde (zonder de nieuwe
golden) exact hetzelfde; met de golden erbij 169 / 206.

**(3) De golden — `corpus/golden/jutterswet-bergingstoeslag`.**
Handgeschreven `ir.json`, geen `bron.rgs`. Eén
`prioritizedDefaults`-groep op `bergingstoeslag` met twee leden in
aflopende prioriteit: lid 1 met de emitter-vorm `Let bergingsgrond =
zijn bergingsgrondslag in bergingsgrond plus 100,00` als CONSEQUENTIE,
gerechtvaardigd door `aantal geborgen wrakken >= 2`; lid 2
voorwaardeloos (`Samengesteld ALLE` over nul onderdelen) met `0,00`,
zodat de groep altijd beslist en een doorval ergens landt.

Drie cases, elke verwachting **eerst door ref** gemeten (runner exit 0
op alle drie):

| case | grondslag | wrakken | lid 1 | bergingstoeslag |
|---|---|---|---|---|
| case1-grond-gelezen | 200,00 | 3 | beslist | **300,00** |
| case2-onder-de-drempel | 200,00 | 1 | ongerechtvaardigd | **0,00** |
| case3-lege-grondslag | *leeg* | 3 | gerechtvaardigd, maar leeg | **0,00** |

Case3 is de dragende en is niet vrijblijvend: dezelfde case met de `Let`
WEG (de lezing kaal in de `plus`, de inline-vorm) meet door ref
**100,00** — de coalesce. Barrière 0,00 tegen coalesce 100,00, dus een
substituerende uitvoerder antwoordt hier fout in plaats van grof.

Gemeten gedrag van de twee gevraagde poorten:
- **gate 7**: alle drie de cases FALEN onder `--noop` (per case
  nagemeten), dus geen van hen komt op de survivorlijst; de poort blijft
  PASS met 352 weerleggende cases / 36 ongewijzigde survivors.
- **gate 12**: de golden **SPEELT** (geen auto-exclude, geen
  value-divergence — met een grep op de gate-uitvoer gecontroleerd dat
  de naam in géén van die lijsten staat). Baseline apart gemeten door de
  goldenmap tijdelijk weg te zetten: **played 104 → 105**, auto-excluded
  40, value-divergence 1, no-case 1 ongewijzigd, JUnit 153.

**Poorten (alle twaalf, in de worktree gedraaid, niet aangenomen).**
Gate 1 PASS · gate 2 PASS (169 files) · gate 3 PASS (169 golden clean,
206 negatief) · gate 4 PASS (410 cases) · gate 5 PASS (388 replayed;
`jutterswet-vormenkabinet` blijft de bekende no-case) · gate 6 PASS (388
door clingo) · gate 7 PASS (352 / 36) · gate 8 PASS (22 rode cases;
Drools-been 5 fault-pinned, 17 not-carried) · gate 9 PASS (201 checks,
12 catala-goldens) · gate 10 PASS (159 tests, 26 rgs-goldens) · gate 11
PASS · gate 12 PASS (105/40/1/1, JUnit 153). Tellingsverschuivingen
t.o.v. de baseline: **+1 ir.json** (gates 2, 3), **+3 cases** (gates 4,
5, 6, 7), **+1 played** (gate 12); al het andere ongewijzigd.

**Bewust gelaten.** De sectiekop "4. the other half …" in
`LetDragerWandelingWitnessTest` (klopt nog als eis, alleen niet meer als
mechanisme — en het is een kop, geen bewering). De open punten van de
companion-kaart die géén commentaarkwestie zijn (`startpuntCompanions`
drager-blind, de DRL-naamdivergentie, companions zonder
stratificatie-salience) blijven staan; die horen bij hun eigen kaarten.
