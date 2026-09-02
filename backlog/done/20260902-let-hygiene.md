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
other half: a companion the walk must NOT fabricate" bleef in deze ronde
staan — ten onrechte; fixronde 1 (L1-1, onder) haalt hem alsnog om.
Bewijs: `git diff -U0` op commit 2996a44 toont
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
| WF-069 | "an IfThenElse is admissible only inside a lambda, function or **strict** let body" | "… only inside a lambda, function or `Let` body" |

Verder alleen commentaar: `ast.ml` (de Let-node beschrijft nu
call-by-need mét de barrière op de leesplek, en is **emitter-produced**
— door de Catala-plugin al sinds Fase-5c kaart 3 (`ffaebe2`,
2026-08-23), door de RegelSpraak-emitter sinds de let-lowering; de oude
zin "Never emitter-produced" was dus al negen dagen vóór deze kaart
onwaar), `loader.ml` (het woord "strict" daar expliciet in de
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
op alle drie; fixronde 1 voegt er per case één verwachting aan toe, zie
L1-4):

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

**Bewust gelaten.** De open punten van de companion-kaart die géén
commentaarkwestie zijn (`startpuntCompanions` drager-blind, de
DRL-naamdivergentie, companions zonder stratificatie-salience) blijven
staan; die horen bij hun eigen kaarten.

## Fixronde 1 (2026-09-02)

Vier lensbevindingen, alle vier verwerkt; geen ervan weerlegd.

**L1-1 (middel) — de sectiekop was de laatste onware zin, en ik had hem
bewust laten staan. Terecht teruggedraaid.** De kop luidde "a companion
the walk must NOT fabricate" terwijl de bullet er vlak boven zegt dat de
wandelingen wél afdalen en de padsonde de companion stil houdt. De lens
mat het mechanisme direct na (één KieBase, twee cases die alleen in de
data verschillen: de companion bestaat en zwijgt); mijn "klopt nog als
eis" was dus fout — als eis was hij juist onjuist. Kop nu: "4. the other
half: a companion the walk DOES build and the path probe must keep
SILENT". Commentaar-only, binnen het mandaat.

**L1-2 (laag) — verkeerde clausuleverwijzing + spanning met clausule 2.**
"under call-by-need a Let body always runs (ch. 10.6 clause 3)" dekte
twee paden, en op het tweede (de gepasseerde barrière) draait de body
juist niet af — clausule 2 kapt hem op de leesplek. En de afdaling in de
niet-genomen ch. 10.2-tak wordt niet door de Let-clausules gedragen maar
door de statische wandeling. Bullet 4 nu: "a STATIC walk cannot know
which ch. 10.2 branch will run, and under call-by-need a Let body is
always ENTERED (ch. 10.6 clauses 1+3; clause 2 may still cut it short at
a read)". Gecheckt tegen RUNTIME-SEMANTICS r. 3163-3180.

Bij dezelfde bevinding meegenomen (de lens noemde het expliciet als
"geen bevinding, wel het vermelden waard"): in
`een_deling_in_de_niet_genomen_tak_levert_geen_fout` staat nu dat DIE
methode de UITKOMST pint en ook onder een nooit-fabricerende
implementatie zou slagen; wat het mechanisme pint is de zuster
`CompanionPadWitnessTest.een_deling_in_de_genomen_tak_is_inconsistent_en_in_de_niet_genomen_niet`.
Daarmee is het commentaar zelfdragend. Alles commentaar-only: `git diff
-U0` op `*.java` (zowel `main...HEAD` als werkboom) door een parser die
elke +/- regel op `//`, `*`, `/*` toetst → **0 niet-commentaarregels**,
en alleen de twee toegestane bestanden staan erin. Geen testbestand
noemt de replay-API-methode.

**L1-3 (laag) — de BinderRef-type week af van de value-type; nagemeten
en rechtgezet in de golden.** Eigen meting over alle
`corpus/golden/**/ir.json` (toen 26 Lets, BinderRef-type vs value-type):
`jutterswet-bergingstoeslag` was de **enige** afwijking — value
`teken: NIET_NEGATIEF`, BinderRef `teken: ONBEPAALD`. WF-078 kruist
alleen de `datatype_kind`, dus geen poort zag het; de NOTE-zin "exactly
the shape an emitter writes" was in dat detail dus onwaar. Gekozen voor
de sterke variant: BinderRef nu `NIET_NEGATIEF`, gelijk aan de value,
zoals de vijf emitter-geproduceerde dragers het doen. Corpusbrede
hermeting na de fix, mét de tweede Let die L1-4 erbij zette: **27 Lets
in 15 goldens, waarvan 26 met een BinderRef, 0 afwijkend** (de 27e is de
`bergingsforfait`-Let, binder ongelezen — die heeft er geen).

**L1-4 (laag) — de golden onderscheidde Let-vs-inlining, maar niet
L-vs-S binnen de defaults-doorval; die as is nu toegevoegd.** Tweede
attribuut `bergingsforfait` met een eigen `prioritizedDefaults`-groep:
lid 1 voorwaardeloos met `Let bergingsgrond = zijn bergingsgrondslag in
42,00` — binder **ongelezen** — en lid 2 het bodembedrag `0,00`. Onder
ch. 10.6 clausule 3 wordt de value nooit geëvalueerd en is de uitkomst
die van de body: **42,00 op alle drie de cases, óók op case3** waar
`bergingsgrondslag` afwezig is. Onder de strikte binding zou dezelfde
default op de bindplek leeg evalueren, niet beslissen en naar de bodem
doorvallen (0,00).

Niet aangenomen maar gemeten, eerst door `ref/`: de pin 42,00 op alle
drie de cases (runner PASS, 3 verwachtingen per case). En de
S-tegenhanger als surrogaat — dezelfde groep met de body veranderd in
`bergingsgrond plus 0,00 maal bergingsgrond`, die de need forceert
zonder de rekenkunde te verplaatsen — meet door ref **0,00 op case3** en
200,00 op case1, precies de doorval die een strikte uitvoerder hier zou
geven. 42,00 tegen 0,00 is dus de L-vs-S-discriminant; de bestaande
0,00-tegen-100,00 op `bergingstoeslag` blijft de
binding-vs-substitutie-discriminant. NOTE.md draagt beide assen nu
expliciet, inclusief de zin dat case3 op `bergingstoeslag` tegen
SUBSTITUTIE discrimineert en niet tegen de strikte binding.

**Poorten na de fixronde (in de worktree gedraaid).** Gate 1 PASS ·
gate 2 PASS (169 files) · gate 3 PASS (169 golden clean, 206 negatief) ·
gate 4 PASS (410 cases) · gate 5 PASS (388 replayed, vormenkabinet
no-case) · gate 6 PASS (388 door clingo) · gate 7 PASS (352 / 36) ·
gate 8 PASS (22 rode cases; Drools-been 5 fault-pinned, 17 not-carried) ·
gate 10 PASS (159 tests, 26 rgs-goldens) · gate 12 PASS (played **105**,
auto-excluded 40, value-divergence 1, no-case 1, **JUnit 153**). Alle
tellingen gelijk aan de eerste ronde: de extra groep en het extra
attribuut zitten in de bestaande ir.json en de bestaande drie cases, dus
er komt geen bestand bij — alleen drie verwachtingen (zes → negen op
deze golden). Gates 9 en 11 niet opnieuw gedraaid: geen catala-bron,
geen `toka/`-bestand en geen frontend-code geraakt.

## Fixronde 2 (2026-09-02)

Vier lensbevindingen, alle vier laag, alle vier verwerkt; geen ervan
weerlegd. Alle vier raken tekst — kaart, commentaar, meldtekst, NOTE —
en geen enkele raakt gedrag; de acceptatie van ronde 1 staat.

**L2-1 (laag) — de telling in het L1-3-blok was de PRE-fix-telling.**
De zin claimde "0 afwijkende BinderRefs op 26 Lets" te meten ná de fix,
maar 26 was het getal van vóór L1-4, die er een tweede Let bijzette.
Eigen hermeting over alle `corpus/golden/**/ir.json`, met een script dat
per Let de BinderRef in de body opzoekt (velden `name`/`type`, niet
`binder`/`typ` — die naamgeving kostte twee pogingen) en het type kruist:
`lets 27 with binderref 26 noref 1 mismatch 0`, verdeeld over **15
goldens**. De 27e is `bergingsforfait`, wiens binder per constructie
ongelezen is. De nulmelding zelf klopte; het getal ernaast niet. Blok
L1-3 draagt nu de post-fix-toestand en zegt erbij dat het oude getal de
meting van vóór L1-4 was.

**L2-2 (laag) — de emitter-datering in `ast.ml` wees de verkeerde
frontend en de verkeerde mijlpaal aan.** De nieuwe zin luidde
"Emitter-produced since the let-lowering card (the RegelSpraak
variabelendeel)", maar de Catala-plugin bouwt de node zelf al langer:
`frontends/catala/lib/translate.ml:807` construeert `I.Let {...}`, en
`git log --diff-filter=A` op `corpus/golden/jutterswet-tariefrecht-catala/ir.json`
wijst **`ffaebe2`, 2026-08-23** — negen dagen vóór deze kaart. Die
golden en `jutterswet-kamerhuishouden-catala` dragen nu 2 respectievelijk
4 `Let`-knopen naast een `bron.catala_en`, dus gate 9 regenereert ze uit
de bron: het ís emitter-output. De oude zin "Never emitter-produced" was
dus al onwaar vóór de let-lowering, en om een andere reden dan de
vervanger noemde. Nu: "Emitter-produced: by the Catala plugin since
Phase-5c card 3, and by the RegelSpraak emitter since the let-lowering
card (the variabelendeel)." De kaartalinea (2) draagt dezelfde correctie,
met commit en datum.

**L2-3 (laag) — `let body` in kleine letters las als de brontaalvorm, niet
als de IR-nodenaam.** De herschreven WF-069-melding schreef "lambda,
function or let body" terwijl de twee WF-077-meldingen uit dezelfde
commit `Let binding` schrijven en IR-SPEC in de WF-069-rij `Let` voert.
Kleine letters is in dit repo verder alleen de Catala-schrijfwijze. Nu
`Let body`, gelijk aan zijn zusters; de kaarttabel-rij is meegelezen.
Gedragsneutraal: gate 3 matcht blijkens `scripts/check-verifier.sh` op
`WF-%03d` uit de mapnaam, niet op meldtekst, en draait ongewijzigd
**169 golden clean / 206 negatief**.

**L2-4 (laag) — het bodemlid van de forfait-groep is op geen enkele
gepinde case bereikbaar; de NOTE zei niet dat hij dat niet dekt.** Onder
ch. 10.6 clausule 3 beslist `bergingsforfait`'s default 1 altijd met
42,00, dus lid 2 vuurt nooit — het is uitsluitend de landingsplaats van
de contrafeitelijke strikte uitvoerder. Zelf nagemeten, niet aangenomen:
met lid 2 verwijderd blijven alle drie de cases door `ref` **PASS (3
verwachtingen)**. Ter contrast dezelfde proef op de andere groep — met
`bergingstoeslag`'s bodem verwijderd meet ref case1 PASS maar case2 en
case3 `MISMATCH: J1.bergingstoeslag = leeg, expected 0`: dáár is de
doorval wél gepind. NOTE.md draagt nu één alinea die precies dit zegt,
mét beide mutantmetingen, zodat de pin eerlijk is over wat hij niet
dekt. De golden zelf wijzigt niet — alleen de NOTE.

**Poorten na fixronde 2 (in de worktree gedraaid).** Gate 1 PASS ·
gate 2 PASS (169 files) · gate 3 PASS (169 golden clean, 206 negatief) ·
gate 4 PASS (410 cases) · gate 5 PASS (388 replayed, vormenkabinet
no-case) · gate 6 PASS (388 door clingo) · gate 7 PASS (352 / 36) ·
gate 8 PASS (22 rode cases; Drools-been 5 fault-pinned, 17 not-carried) ·
gate 10 PASS (159 tests, 26 rgs-goldens) · gate 12 PASS (played 105,
JUnit **153**). Alle tellingen identiek aan ronde 1 — deze ronde raakt
geen enkel getal, precies zoals een tekstronde hoort. Gates 9 en 11 niet
gedraaid: geen catala-bron, geen `toka/`-bestand geraakt; gate 9 blijft
de pre-merge-plicht op een catala-dragende machine.

Eerlijk gemeld: gate 1 tripte in eerste aanleg op déze Bouwnotitie —
mijn eigen L2-4-kop droeg het pre-rename-woord voor een
prioritizedDefaults-groep. De tripwire deed precies zijn werk, ook op
werkende prosa; kop herschreven naar "groep", daarna PASS.

**Mandaat.** Geen `*.java` gewijzigd in deze ronde: `git diff --stat` op
de rondecommits raakt `spec/verifier/lib/{ast,checks}.ml`, de kaart en
`corpus/golden/jutterswet-bergingstoeslag/NOTE.md`. `spec/verifier/` is
een protected path — deze commits zijn een voorstel, niet iets dat de
sessie zelf mergt.

## Result (hoofdsessie, 2026-09-02)

Drie punten geland, elk met bewijs. (1) De twee testbestanden dragen
ch.-10.6-L-conform commentaar: vier hunks, uitsluitend commentaarregels
(git diff -U0-bewijs in de Bouwnotitie), suite ongewijzigd 153/0/0.
(2) `grep -ri "strict let" spec/verifier/` is leeg; de meldteksten van
WF-077 (beide armen) en WF-069 volgen de spec-terminologie, en de
opruiming ving twee ONWARE commentaren die de kaart niet eens noemde:
ast.ml's "Never emitter-produced" op de Let (onwaar sinds de
Catala-plugin, correct gedateerd na lens 2) en de loader-"Strict" die nu
expliciet sleuteldiscipline betekent, niet semantiek. Verifier-gedrag
byte-gelijk (168→168 vóór de golden). (3) De golden
`jutterswet-bergingstoeslag` pint het corpus-ongedekte pad — een Let in
een prioritizedDefaults-consequentie — en SPEELT in gate 12 (played
104 → 105). De dragende meting: met lege grondslag valt de barrière op
de leesplek en beslist het lid niet (ref 0,00), waar de inline-vorm
coalescet naar 100,00 — het L-gedrag in de defaults-consequentie is nu
door alle drie de executors heen gepind.

Lensvangsten: de pre-L-sectiekop in LetDragerWandelingWitnessTest
(binnen het mandaat gecorrigeerd), een verkeerde clausuleverwijzing, de
emitter-productie-datering, en de dekking van de beslistakken van de
defaults-groep in de golden zelf (fixronde-secties hierboven). Eindstand op de branch: alle
twaalf gates groen; verschuivingen exact de golden — gate 2/3 = 169
goldens (negatief 206 onveranderd), gates 5/6 = 388, gate 7 = 352/36
(survivorlijst niet stale), gate 12 = played 105 / auto-excluded 40 /
value-divergence 1 / no-case 1, JUnit 153.
