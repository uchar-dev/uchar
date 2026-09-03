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
conventie uit elkaar liep bestaat niet meer.

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
bak-scalair wordt aangeleverd blijft de luide vormfout die hij is.

**Getuigen.** Twaalf in `SleutelconventieWitnessTest`, elke verwachting eerst
door ref en clingo. Ze lezen het §9.5-kanaal via `naFire`, niet via de
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
   getuige; daar is een dertiende getuige voor bijgekomen, die de mutant nu
   doodt.

**Gates.** Alle twaalf groen in de worktree, tellingen ongewijzigd: gate 12
`played 105 / auto-excluded 41 / value-divergence 1 / no-case 1`, JUnit 165
tests zonder skips. Er is nog steeds geen corpusgetuige voor beide vormen —
geverifieerd doordat die tellingen niet bewogen; een synthese-getuige zou een
uitbreiding van May-touch vergen en is niet gedaan.

**Restgat, gemeten.** De ~40 overige `attribuutGranulariteit.containsKey`-
plekken in beide transpilers (temporele-operand-hekken, Phase-F-grenzen) zijn
NIET meegenomen — die vallen buiten de vier sleutelplekken die de kaart noemt.
Eén sonde erop: een regel-doel dat het temporele attribuut in andere kast
noemt (`Borgdeel` op `jutterswet-borgdeel`) is verifier-schoon en speelt in
ref, en Drools weigert de golden dan LUID (`transpiler SKIP: regel (waarde
buiten scope)`) — een eerlijke niet-gedragen uitkomst, geen stille misser en
geen ERROR. De familie als geheel is niet systematisch gemeten.
