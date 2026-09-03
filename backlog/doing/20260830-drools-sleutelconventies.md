# Drools: uniciteitssleutel-conventies (naamnormalisatie + kruis-bron gemengde unie)

Goal: Twee pre-existente lacunes in de uniciteits-lowering van
`backends/drools`, blootgelegd door de lenzen op de
temporele-sleutelvorm-kaart (2026-08-30). (1) NAAMNORMALISATIE: alle vier
sleutelsites (de twee joins in `ExecModelTranspiler` en de twee in de
sluimerende `DrlTranspiler`) toetsen temporaliteit met een exacte
String-match (`attribuutGranulariteit.containsKey`), waar ref en clingo
normaliseren (`Checks.norm`) — een anders gekapitaliseerde sleutel
passeert de verifier, telt hier als scalair, en levert op een GELDIG
één-component-model een stil verkeerd antwoord (gemiste botsingen die
ref/clingo vinden; lens-bewezen). De ref-getrouwe spiegel
(`temporeelAttribuut`) bestaat al in het bestand — trek de conventie op
alle sites gelijk, met witnesses. (2) KRUIS-BRON GEMENGDE UNIE: een
uniesleutel die temporeel is op bron A en scalair op bron B laat Drools
crashen (`ERROR ConstraintEvaluationException`) waar ref ≡ clingo gewoon
spelen (lens-bewezen op een verifier-schoon model); een ERROR is onder het
gate-12-regime nooit een geldige niet-gedragen uitkomst — draag de vorm of
weiger hem luid per vorm.

Acceptance:
- Rood-eerst per lacune (de lens-modellen als startpunt: de
  gekapitaliseerde-sleutel-variant met gemiste botsing; de
  kruis-bron-unie met de crash).
- Nieuwe witnesses pinnen beide kanten; ref is de scheidsrechter (elke
  verwachting eerst door ref).
- Alle twaalf gates groen met ongewijzigde tellingen (geen corpusgetuige
  voor beide vormen — verifieer dat dat zo blijft of voeg
  synthese-getuigen toe via het volledige differentiaalvierkant).

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `backlog/`.

Out of scope: ref/clingo/verifier/spec/corpusverwachtingen, de
vuur-tijd-plaatsing van de ariteitsguard (geland en gepind), gate-scripts.

## Bouwnotitie

**Reproductie, rood-eerst, elk eerst door ref (2026-09-03).** Beide lacunes
zijn gereproduceerd op modellen die in de getuige-test in het geheugen op een
corpus-`ir.json` worden geschreven; het corpus is niet aangeraakt. Alle vijf
modellen zijn verifier-schoon en ref ≡ clingo op alle vijf.

| vorm | ref | clingo | Drools vóór |
|---|---|---|---|
| enkelvoudig, sleutel `Vondstnummer` | 2 botsingen (S1, S2) | idem | `consistent` — stil gemist |
| unie, bronsleutel `Vondstnummer` | 2 botsingen (S1, S2) | idem | `consistent` — stil gemist |
| ariteitsguard, `Vondstnummer` + `kavelnummer` | `EVALUATION FAULT: a uniqueness key list carries at most one temporal key component (…)` | dezelfde tekst bij vertaling | speelt door — de guard zwijgt |
| kruis-bron gemengde unie, botsend | 2 botsingen (S1, W1) | idem | `ERROR ConstraintEvaluationException` |
| kruis-bron gemengde unie, niet botsend | `consistent` | idem | dezelfde `ERROR` |

De crash letterlijk, met oorzaakketen:

```
ERROR ConstraintEvaluationException: Error evaluating constraint '' in  and in more rules
  <- java.lang.IllegalStateException: rsOoitWaarden: een temporele sleutel moet
     een List<Segment> zijn, kreeg java.math.BigDecimal — dit had onbereikbaar
     moeten zijn
       at RegelSpraakRuntime.rsOoitBotst(RegelSpraakRuntime.java:669)
       at ExecModelTranspiler.lambda$uniciteitUnie$…(ExecModelTranspiler.java:5171)
```

**Waarom de misser stil was.** Een mis-gekapitaliseerde sleutel viel uit de
granulariteitskaart, waarna de scalaire tak twee `List<Segment>` met `equals`
vergeleek. `Segment` heeft geen `equals`, dus zelfs de instantie zelf matchte
haar eigen sleutel niet: de telling bleef op 0 staan en niets werd gevlagd.

**Fix (a): één toets, vijf plekken.** De temporaliteitstoets woont nu op één
plek — `Namen.bevatNaam` (ref's `Checks.norm`), in het gedeelde naamcontract
dat allebei de backends al importeren. `ExecModelTranspiler.temporeelAttribuut`
delegeert ernaar; de twee joins en de ariteitsguard daar roepen die spiegel
aan, en de twee joins in de sluimerende `DrlTranspiler` roepen `bevatNaam`
rechtstreeks aan. Vijf plekken, nul lokale afleidingen — de manier waarop de
TEMPORALITEITSTOETS uit elkaar liep bestaat niet meer. (De ariteitsguard stond
na deze fix nog uitsluitend in het exec-model; fixronde 1 heeft hem alsnog in
de tweeling gezet — zie fix (d).)

**Fix (a′), fixronde 1: de norm zelf moest smaller.** De eerste `Namen.norm`
claimde ref's `Checks.norm` te zijn maar was ruimer: `toLowerCase(Locale.ROOT)`
tegen ref's `String.lowercase_ascii`, en `\s+` tegen ref's `collapse_ws`, dat
alleen op spatie en tab splitst. Een naamgelijkheid die WIJDER is dan ref's is
geen fix maar een nieuwe divergentie, en zij was gemeten schadelijk: drie
verifier-schone documenten die ref én clingo spelen kregen een kale
`ERROR ConstraintEvaluationException` of een VALSE ch.-5.9-vormfout (de
transpiler las "temporeel" waar ref "scalair" leest, en `FactModelGenerator`
gaf het beanveld dus een `BigDecimal`). `norm` is nu byte-exact ref
(`checks.ml:23-28`): splitsen op `[ \t]+`, alleen A-Z verlagen. Gemeten na de
fix, elk eerst door ref:

| model | verifier | ref | clingo | Drools |
|---|---|---|---|---|
| granulariteit `COÖRDINAAT` bij attribuut `coördinaat`, sleutel scalair 7/7 | PASS | 2 botsingen (S1, S2) | idem | `PLAYED 1` (was ERROR) |
| granulariteit `vondst\nnummer` bij attribuut `vondst nummer`, 7/7 | PASS | 2 botsingen | idem | `PLAYED 1` (was ERROR) |
| sleutellijst `coördinaat` + `kavelnummer` (één temporele component) | PASS | `consistent` | idem | `PLAYED 1` (was een valse vormfout) |

**Fix (c), fixronde 1: de sleutel gaat gedeclareerd naar de bean.** De
genormaliseerde temporaliteitstoets was maar de helft: de sleutel ging daarna
RAUW naar `RegelSpraakRuntime.get`, die via `pascal()` een getter zoekt, en de
bean draagt het veld dat uit de DECLARATIE is gegenereerd. Van vijf
kastvarianten van dezelfde sleutel speelden er daardoor twee en gaven er drie
een kale ERROR (`geen getter voor 'vondstNummer' op …Strandvondst`) — de
lacune die de kaart als KLASSE formuleert was als deelverzameling gedicht.
`Namen.gedeclareerdeNaam` is nu het tweede stuk van het naamcontract; alle
zeven sleutelplekken (vier in het exec-model, drie in de tweeling) resolveren
ermee. Gemeten op `jutterswet-strandregister` met de onaangeraakte `case1`:
alle vijf spellingen (`VONDSTNUMMER`, `Vondstnummer`, `vondstNummer`,
`VOndstnummer`, `vondstnummeR`) zijn verifier-schoon, ref en clingo geven op
alle vijf `PASS (2 expectations)`, en Drools speelt nu alle vijf (`PLAYED 1`);
de uniesleutel-variant idem.

**Fix (d), fixronde 1: de ariteitsguard staat nu ook in de tweeling.** De DRL-
tweeling rendeerde op `corpus/rood/uni-two-temporal-keys` twee `rsOoitBotst`-
joins waar ref FAULT, clingo `TRANSLATION FAULT` en het exec-model een
vormfout geven. De tekstroute heeft geen vuurmoment, dus zij weigert in HAAR
luide idiom: een `// onopgelost`-clausule, waardoor `meet` het construct als
MARKER met reden boekt (plus waarschuwing) in plaats van een join te renderen
die een door alle drie de executors verworpen vorm nabootst. Zelfde guard per
BRON in de unie (gemeten model: unie-sleutellijst `vondstnummer` +
`volgnummer`, beide temporeel op beide bronnen — ref `EVALUATION FAULT`,
clingo `TRANSLATION FAULT`, exec-model vormfout op
`eenmalig vondstnummer (Strandvondst 1)`).

**Opruiming, fixronde 1.** `ExecModelTranspiler.defaultsNorm` was een tweede,
byte-identieke kopie van dezelfde norm; hij delegeert nu naar `Namen.norm`. Eén
implementatie, zodat een correctie van de norm de doel-/defaults-sleutels
meeneemt in plaats van ze stil te laten afwijken. Zonder eigen getuige: het
verschil zou een defaults-doel vergen wiens objecttype- of attribuutnaam
alleen in een niet-ASCII-kast of een newline van zijn verwijzing verschilt —
een vorm die geen corpus-golden draagt (gate 12 telt onveranderd).

**Fix (b): dragen, niet weigeren.** Elke bron van een `verenigd met`-unie
declareert haar EIGEN sleutellijst, dus een component kan temporeel zijn op
bron A en scalair op bron B. De temporaliteit is dus een eigenschap van het
PAAR (sleutel van deze bron, sleutel van de subjectbron), en alle vier
combinaties komen voor; ref speelt ze alle vier (`ever_tuples` draait per
bron). Gedragen is hier de smalste CORRECTE kant: drie takken erbij plus één
runtime-predicaat (`rsOoitBevat` — de scalaire kant claimt precies haar eigen
waarde, membership in de ooit-verzameling), tegenover een weigering die drie
takken en een reden-tekst kost én een vorm die ref én clingo gewoon spelen uit
het differentiaalvierkant snijdt. `rsOoitBevat` is scale-ongevoelig zoals de
temporele kant (7 en 7.00 botsen, en ref's `lexeme_of_q` doet hetzelfde omdat
Num een rationaal is), een niet-numerieke scalair claimt niets (ref's Tekst-
lexeem staat tussen aanhalingstekens en is nooit een getal), en de
tijdlijn-kant blijft fail-fast: een gedeclareerd-temporele sleutel die als
bak-scalair wordt aangeleverd wordt niet stil doorgespeeld (fixronde 1 heeft
gemeten hóe hij faalt: ref met een ch.-5.9-fault, Drools met een ERROR — zie
restgat 1).

**Getuigen.** Twaalf in `SleutelconventieWitnessTest` en tien in
`NaamNormWitnessTest` (fixronde 1), elke verwachting eerst door ref en clingo. Ze lezen het §9.5-kanaal via `naFire`, niet via de
case-comparator: die matcht een regelnaam-sleutel op de NAAM ALLEEN, dus een
half gevonden botsing komt daar groen doorheen (mutant 3a bewees dat: met
alleen de spiegel-tak weg verdwijnt S1 en blijft W1 staan — de comparator zou
dat niet zien). Vóór de fix: 4 kale tweelingen groen, 3 naamnormalisatie rood,
2 gemengde unie rood.

**Mutanten (in een /tmp-kopie, elk gedood):**

1. exacte match terug in `Namen.bevatNaam` → 4 doden (de drie
   naamnormalisatie-getuigen plus de DRL-renderingsgetuige);
2. de gemengde takken uit `uniciteitUnie` → de `ConstraintEvaluationException`
   is letterlijk terug op beide gemengde getuigen;
3. alleen de spiegel-tak weg → `[S1, W1]` wordt `[W1]` (de stille misser
   zonder de crash);
4. de gemengde takken uit de DRL-tweeling → de renderingsgetuige valt;
5. `containsKey` terug op alleen de DRL-UNIE-plek → **overleefde** elke
   getuige; daar is de getuige `de_drl_tweeling_normaliseert_ook_de_uniesleutel`
   voor bijgekomen (de twaalfde), die de mutant nu doodt.

**Mutanten fixronde 1 (dezelfde /tmp-kopie, elk gedood):**

6. de brede `norm` terug (`trim` + `\s+` + `Locale.ROOT`) → 5 doden: de twee
   norm-contractgetuigen, de diakriet- en de newline-getuige, en de
   ariteitsgetuige (plus een dode in `SleutelconventieWitnessTest`);
7. `gedeclareerdeNaam` teruggebracht tot identiteit → 8 doden (het contract
   draagt ook `bevatNaam`);
8. alleen de sleutel-RESOLUTIE weg (de twee `sleutelVeld`-spiegels naar
   identiteit, `bevatNaam` intact) → 3 doden: de vijf-spellingen-getuige, de
   uniesleutel-getuige en de DRL-veldnaamgetuige;
9. beide DRL-ariteitsguards weg → 2 doden (enkelvoudig en unie).

**Gates.** Alle twaalf groen in de worktree, corpustellingen ongewijzigd:
gate 12 `played 105 / auto-excluded 41 / value-divergence 1 / no-case 1`;
JUnit 175 tests zonder skips (was 165 — de tien getuigen van fixronde 1). Gate
1 PASS. Er is nog steeds geen corpusgetuige voor beide vormen — geverifieerd
doordat die tellingen niet bewogen; een synthese-getuige zou een uitbreiding
van May-touch vergen en is niet gedaan. Óók de vijf sleutelconventie-vormen
van fixronde 1 leven uitsluitend in getuigen, niet in het corpus.

**Restgat 1, gemeten: het beanveld en de case-invoer typeren nog exact.**
`FactModelGenerator` (het `List<Segment>`-veld) en `CaseRunner.temporeel` (de
invoerkant) lezen `attribuutGranulariteit` nog met een EXACTE `containsKey`,
terwijl de sleutelplekken via het naamcontract lezen. Na de norm-fix valt dat
gat samen met precies één vorm: een granulariteitskaart die de naam in een
andere ASCII-KAST spelt dan `attributen`. Gemeten sonde (`Vondstnummer` in de
kaart, `vondstnummer` als attribuut, scalaire invoer 7/7): verifier PASS, ref
`EVALUATION FAULT: the ever-unique key "vondstnummer" needs the full timeline;
"S1" supplies only a scalar (ch. 5.9)`, Drools `ERROR
ConstraintEvaluationException`. Beide executors weigeren dus, maar de VORM
verschilt (fault vs ERROR). Bewust gelaten: het is de invoer-/beanmodelas, niet
een van de sleutelplekken die de kaart noemt, en de vorm heeft geen
corpusgetuige (ref speelt hem niet, dus hij zou naar `corpus/rood` moeten — een
corpuswijziging buiten May-touch). Levert de invoerkant wél een tijdlijn, dan
weigert `CaseRunner` LUID en per vorm (`EXCLUDE timeline value on
vondstnummer …`).

**Restgat 2, gemeten.** De ~40 overige `attribuutGranulariteit.containsKey`-
plekken in beide transpilers (temporele-operand-hekken, Phase-F-grenzen) zijn
NIET meegenomen — die vallen buiten de sleutelplekken die de kaart noemt.
Eén sonde erop: een regel-doel dat het temporele attribuut in andere kast
noemt (`Borgdeel` op `jutterswet-borgdeel`) is verifier-schoon en speelt in
ref, en Drools weigert de golden dan LUID (`transpiler SKIP: regel (waarde
buiten scope)`) — een eerlijke niet-gedragen uitkomst, geen stille misser en
geen ERROR. De familie als geheel is niet systematisch gemeten.
