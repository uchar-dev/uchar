# WF-034, de dagsoort-kruisingsarm: `dagsoortPerEenheid`'s waardehelft

Goal: de arm die de zesde golf gemeten maar niet kón schrijven — **de waarde
van elke `dagsoortPerEenheid`-ingang MOET een gedeclareerde dagsoort noemen**
— met het negatief dat de nieuwe golden één veld verdraait en de arm-exacte
mutant erbij. De getuige die ontbrak bestaat sinds 2026-09-06.

## Waarom deze kaart nu wél kan

De zesde golf (`backlog/done/20260905-wf034-zesde-golf-restvelden.md`) liet
`dagsoortPerEenheid` staan met een expliciete motivering: 60 ingangen, alle 60
in de pariteitszone, 0 daarbuiten — en een negatief bouwen op een vorm die
geen enkele golden draagt is met zoveel woorden verboden, want dat geeft een
clausule die geen echt document ooit passeert.

`backlog/done/20260905-dagsoort-per-eenheid-golden.md` heeft die getuige
geleverd: `corpus/golden/jutterswet-schouwrooster` declareert twee dagsoorten
en draagt vier map-ingangen, waarvan één eenheidsspelling (`schouwdagen`)
dragend is als gedeclareerde `eenheid` op drie attributen en een parameter.
De open-lijstpost in `spec/IR-SPEC.md` ch. 4.6 is daarop bijgewerkt en blijft
OPEN precies tot deze kaart hem sluit.

## De meting (2026-09-06, na de golden-kaart)

**Meetstempel: 2026-09-06, op `f0161ad` + deze golden. HERMEET VÓÓR HET
SCHRIJVEN.** De tellingen hieronder verlopen terwijl deze kaart in `new/`
ligt: `card/zelfnaam-mapsleutel` brengt de negatiefzone naar ~325 en de vier
zones naar ~533 (gemeten op die branch: 155 golden + 22 rood + 325 negatief +
30 pariteit, plus deze golden). Andere kaarten kunnen er nog bij komen. Neem
de getallen dus niet over — draai de telling opnieuw en dateer haar, want de
Acceptance hieronder eist juist *hertelde en gedateerde* aantallen.

| zone | documenten | `dagsoortPerEenheid`-ingangen | `dagsoorten`-declaraties |
| --- | --- | --- | --- |
| golden | 156 | **4** (in 1 document) | 5 |
| rood | 22 | 0 | 0 |
| negatief | 320 | 0 | 8 |
| pariteit | 30 | 60 (in 30 documenten) | 30 |
| **vier zones** | **528** | **64** | 43 |

Twee dingen om bij het schrijven vast te houden. (1) Geen enkel negatief
draagt de map vandaag — dit wordt het eerste, en dus ook de eerste plek waar
een verdraaide ingang een verdict verplaatst. (2) De verifier LEEST de map al,
maar alleen aan de **sleutelkant**: `day_kind_unit` in `spec/verifier/lib/checks.ml`
vraagt of een eenheidsspelling als sleutel voorkomt, om hem naast de negen
tijdseenheden toe te laten in datumrekenkunde en tijdsduur (WF-035/WF-038). De
**waarde** kent vandaag **geen verdict-dragende kruising**: verifier, ref en
clingo kruisen hem op nul posities. De Drools-backend kruist hem wél — beide
transpilerbranches lopen `model.dagsoortVoorEenheid(...)` → `dagsoortGlobaal`
→ de gedeelde `Dagsoorten`-predikaatcompiler, in de §6.11-datumrekenkunde
(`DrlTranspiler.java:1771`) en in de §6.10-tijdsduur
(`DrlTranspiler.java:2026`) — maar bij een waarde die `dagsoorten` niet
declareert levert `dagsoortGlobaal` `null` en zet de transpiler een **marker**
neer, geen verdict: de vorm wordt stilletjes niet gedragen in plaats van
afgewezen. Eén consument verdedigt zich dus al ad hoc, op één van de vier, en
alleen op de twee duurposities. Een ingang die naar een niet-gedeclareerde
dagsoort wijst, laat vandaag dus nog steeds een eenheidsspelling door die naar
niets verwijst; de datumrekenkunde erachter is toegestaan door de verifier en
door geen enkele uitvoerder uit te rekenen.

## De arm

Nieuwe letterpost in de ch. 4.6-armenlijst (de reeks loopt tot (r); dit wordt
(s)): elke waarde in `dagsoortPerEenheid` noemt een dagsoort die
`dagsoorten` declareert, vergeleken met de naam-identiteitsregel van WF-001
(getrimd, hoofdletterongevoelig — dezelfde `norm` die `day_kind_unit` op de
sleutel gebruikt).

Open bij het schrijven te beslissen, en de keuze opschrijven:

- Hoort hier ook de **sleutelkant** bij — moet een sleutel de spelling zijn van
  de dagsoort die hij noemt (enkelvoud of het gedeclareerde meervoud), of is de
  sleutel vrij? De frontend registreert vandaag precies die twee spellingen,
  maar de IR is engine-neutraal en een andere frontend mag afkortingen kennen.
  Als het antwoord "vrij" is: opschrijven dát het vrij is, niet stilzwijgend
  overslaan.
- WF-017 (ch. 2.9) verbiedt al botsing met de ingebouwde kalendereenheden en
  met de spellingen van een andere dagsoort. Deze arm is de derde kant van
  dezelfde driehoek; controleer dat hij WF-017 niet dubbelt.
- **De zusterpost `Constante.naam`** — meenemen of expliciet doorschuiven naar
  een eigen kaart, maar niet stilzwijgend overslaan. Gemeten 2026-09-06 op de
  golden van deze kaart: het veld is **ongecontroleerd**. `spec/IR-SPEC.md`
  ch. 4.1 somt in de rij ContextValue twee namen op (`Rekendatum`,
  `Rekenjaar`); de wire draagt er sinds `jutterswet-schouwrooster` **drie**
  (`de dag`, de dagsoortverwijzing). Verifier, ref en clingo lezen de naam
  niet; de Drools-dagsoortcompiler leest hem als **magische string**
  (`equalsIgnoreCase` op `"de dag"`) — één consument, één letterlijke
  vergelijking. `de dag` → `de dagX` laat alle vier consumenten groen op
  dezelfde waarden. Dat is dezelfde vorm van lek als de arm hierboven — een
  naam op de wire die niets kruist — en of hij hierbij hoort is een
  scope-beslissing die opgeschreven moet worden.

## Acceptance

- Eén negatief onder `corpus/negatief/wf034-<slug>/`, rood-eerst zichtbaar:
  PASS onder de verifier van vóór deze kaart, één regel erna die WF-034 en de
  positie noemt. Bouw hem door **één veld** van `jutterswet-schouwrooster`'s
  `ir.json` te verdraaien — de waarde van één map-ingang naar een dagsoort die
  `dagsoorten` niet declareert — zodat het negatief en de golden aantoonbaar
  hetzelfde document zijn op dat ene veld na.
- De arm-exacte mutant: alleen deze arm uit de verifier weg → alleen dit ene
  document weer schoon, over de volle vier-zone-sweep.
- Nul over-afwijzing: over alle 528 vier-zone-documenten die aan deze kaart
  voorafgaan verplaatst geen enkel verdict behalve dat van het nieuwe negatief;
  rapporteer ook hoeveel rapporten (niet-verdicts) bewegen.
- De open-lijstpost voor `dagsoortPerEenheid` in ch. 4.6 gaat DICHT en de post
  verhuist naar de gesloten-armenopsomming, met hertelde en gedateerde
  aantallen (vier zones, negatiefzone) — de fout die de fixrondes van golf 4
  en 5 moesten herstellen is een golf die zijn eigen tellingen laat verlopen.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

## Out of scope

- **De dagsoort-duurvormen zelf.** `tijdsduur … in hele <dagsoort>` (§6.10) en
  `datum ± N <dagsoort>` (§6.11) worden door de referentie-interpreter en de
  ASP-backend allebei luid geweigerd ("a recorded flank, not executed"), terwijl
  de Drools-backend ze wél lowert. Dat is een gemeten drie-uitvoerder-asymmetrie
  en een eigen semantiekkaart; deze arm is een resolutievraag, geen
  rekenkundige.
- `VerreRollidmaatschap`'s eigen vuller en backref — de andere nul-dracht-post
  van de zesde golf. Die heeft nog steeds geen enkele drager in geen enkele
  zone en wacht op een frontend die de vorm kan emitteren.
- De `Eenheidstelsel.naam`-klasse (de zelfnaam-tegen-mapsleutel-vraag) en
  WF-009's `afkorting`-vraag: eigen posten, eigen kaarten.

## Eén koppeling om te kennen (geen scope, wel een valkuil)

`scripts/check-drools.sh` krijgt via `card/gate12-speelvloer` een
`EXPECTED_PLAYED`-pin op de speelvloer van gate 12. De golden van deze kaart
tilt die vloer met één (109 → 110). Een negatief verandert de vloer **niet**
(gate 12 loopt de goldens), dus deze kaart raakt de pin normaal gesproken
niet — maar als er onderweg toch een golden bijkomt, moet het getal mee, en
`scripts/` is een beschermd pad: dat is een onderhouders-edit, geen sessie-edit.

---

## Results (2026-09-09)

**Geland.** WF-034 krijgt arm **(s)**: elke WAARDE van een
`dagsoortPerEenheid`-ingang moet een dagsoort noemen die `dagsoorten`
declareert, onder de naam-identiteit van ch. 2.1 (getrimd,
hoofdletterongevoelig — dezelfde `norm` die `day_kind_unit` op de sleutel
gebruikt). Vier artefacten:

- `spec/verifier/lib/checks.ml` — `wf034_dagsoort_eenheden`, één guard,
  geregistreerd in `let all`.
- `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort/` — de getuige, een
  één-veld-verdraaiing van `corpus/golden/jutterswet-schouwrooster/ir.json`
  (`"schouwdagen": "schouwdag"` → `"schouwdagen": "havendag"`, een
  één-regel-diff), met NOTE.
- `spec/IR-SPEC.md` — arm (s) in ch. 4.6, de open-lijstpost dicht en verhuisd
  naar de verlaten-lijst, ch. 2.1 (3) en ch. 2.9 mee, en de WF-index-rij
  (regelzin, denominator, getuigen).
- `corpus/golden/jutterswet-vormenkabinet/` — de getuige voor de
  NORMALISATIE-helft van de arm (fixronde), twee map-ingangen die alleen onder
  `norm` resolveren, plus NOTE-sectie.
- `backlog/new/20260909-constante-naam-gesloten-verzameling.md` — de zusterpost,
  doorgeschoven met eigen meting.

### De hermeting (2026-09-09, op `4acc6cb` + deze kaart)

De kaartmeting van 2026-09-06 is **niet** overgenomen; alles hieronder is
opnieuw geteld op de aftakking.

Twee toestanden, apart benoemd — dat was het tweede ernstige lenspunt: mijn
eerste Results gaven de BASELINE-getallen in de tegenwoordige tijd, terwijl het
negatief zelf een ingang toevoegt die bewust niet resolveert.

**BASELINE — de stand die aan de kaart voorafgaat (554 documenten):**

| zone | documenten | `dagsoortPerEenheid`-ingangen | `dagsoorten`-declaraties |
| --- | --- | --- | --- |
| golden | 164 (38 anchor) | 4 (in 1 document) | 5 (in 3 documenten) |
| rood | 23 | 0 | 0 |
| negatief | 337 | 0 | 8 (in 7 documenten) |
| pariteit | 30 | 60 (in 30 documenten) | 30 |
| **vier zones** | **554** | **64** (in 31 documenten) | **43** |

Op die baseline resolveerden **alle 64** waarden, waren er **0** nul- en **0**
lege waarden, en waren **32 van de 64** sleutels géén gedeclareerde
dagsoortnaam.

**LANDED — de stand waarop deze kaart neerkomt (555 documenten, 2026-09-09):**

| zone | documenten | ingangen | declaraties |
| --- | --- | --- | --- |
| golden | 164 | **6** (in 2 documenten) | 7 |
| rood | 23 | 0 | 0 |
| negatief | **338** | **4** (in 1 document) | 10 |
| pariteit | 30 | 60 (in 30 documenten) | 30 |
| **vier zones** | **555** | **70** (in 33 documenten) | **45** |

Daarvan resolveren er **69 van de 70**; de ene die dat niet doet is de getuige
van deze kaart, daar neergezet om afgewezen te worden. **0** nul, **0** leeg,
**35 van de 70** sleutels géén gedeclareerde dagsoortnaam, en **2 van de 70**
resolveren uitsluitend via normalisatie (de nieuwe identiteitsgetuige).
`v "WF-034"` emissiepunten in `checks.ml`: 43 → **45**.

*Kanttekening bij de kaartgetallen:* de kaart noemde 156 golden en 528
vier-zone-documenten; de gates tellen golden en rood samen (gate 2/3 melden
187 = 164 + 23), en dat verschil is waar de verlopen telling vandaan komt.

### Rood-eerst, met echte uitvoer

Verifier van vóór de arm (md5 `8ea45e866dddcf861b357b430d50b4d1`):

    $ /tmp/verifier-before.exe corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort/ir.json
    verifier: PASS
    exit=0

Verifier mét de arm (md5 `3f177f7c867b7c47a010dfd8758ef040` na de fixronde):

    $ verifier.exe corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort/ir.json
    WF-034 FAIL /dagsoortPerEenheid/schouwdagen: day-kind unit 'schouwdagen' names
    day kind 'havendag', which no declaration carries: the registry entry resolves
    against no day kind
    exit=1

    $ verifier.exe corpus/golden/jutterswet-schouwrooster/ir.json
    verifier: PASS

Eén regel, WF-034, met de positie; de gastheer-golden blijft schoon.

### Nul over-afwijzing, direct gemeten

Volledige rapporten (stdout+stderr+exitcode) van beide binaries over alle
**555** vier-zone-documenten, `diff`:

    documents swept: 555
    466,467c466,467
    < ### corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort/ir.json exit=0
    < verifier: PASS
    ---
    > ### .../ir.json exit=1
    > WF-034 FAIL /dagsoortPerEenheid/schouwdagen: …

Eén verdict beweegt — dat van het nieuwe negatief — en **nul** andere
verdicts. Bewegende rapportregels apart geteld: **nul**. Geen enkel bestaand
document krijgt zelfs maar een incidentele regel erbij. De golden die de
fixronde aanpast, `jutterswet-vormenkabinet`, is onder **beide** binaries
`verifier: PASS` — de twee toegevoegde ingangen resolveren, en dat is precies
wat de identiteitsgetuige moet doen.

### De arm-exacte mutant

OCaml-builds zijn niet commentaar-invariant en het buildpad zit in de binary,
dus het RECEPT is de pin en de md5 alleen de bevestiging. Alle drie de mutanten
zijn na de fixronde opnieuw gebouwd tegen de gewapende binary
`3f177f7c867b7c47a010dfd8758ef040` (die md5 verschilde van de eerste ronde
alléén door de commentaarwijziging — precies het punt).

| mutant | recept in `spec/verifier/lib/checks.ml` | md5 | documenten die bewegen | gate 3 |
| --- | --- | --- | --- | --- |
| 1 — arm weg | verwijder de tekst `wf034_dagsoort_eenheden ctx; ` uit `let all` | `2edc880fc37d1bcbb367c4459e12c055` | **1**: het negatief wordt schoon | exit 1 |
| 2 — nul-tak inert | vervang de `\| None ->`-tak door `\| None -> None` | `c8b4068bb840e82c7c5efdc6f7de8a36` | **0** | exit 0 |
| 3 — byte-exacte identiteit | vervang `mem_ci (some_names ctx.doc.dagsoorten) d` door `List.mem d (some_names ctx.doc.dagsoorten)` | `5850549eee799eb245ea2ebe5676c2b6` | **1**: `jutterswet-vormenkabinet` wordt vuil | exit 1 |

Mutant 1 is de arm-exactheid die de kaart eist: exact één document wordt weer
schoon en geen enkele andere verdict- of rapportregel verandert. Mutant 3 is
het derde ernstige lenspunt, opgelost — zie hieronder. Mutant 2 beweegt niets
en dat is een eerlijk gemeten nul: de nul-spelling heeft geen corpusgetuige,
maar is buiten het corpus wel falsifieerbaar (één veld van de gastheer-golden
op `null` is PASS onder die mutant en wordt door de arm geweigerd met
`day-kind unit 'schouwdagen' names no day kind at all`). Na elk herstel is de
binary weer byte-identiek aan `3f177f7c…`.

### Observability van de verdraaiing: geen enkele consument merkt het

Gemeten met de verifier van vóór de arm, tegen de twee cases van de
gastheer-golden:

    verifier  PASS
    ref       oracle: PASS (7 expectations) / oracle: PASS (3 expectations)
    clingo    clingo: PASS (7 expectations) / clingo: PASS (3 expectations)
    drools    PLAYED 2

— byte voor byte wat de onaangeroerde golden in alle vier oplevert. `ref` en de
ASP-backend lezen de map op nul posities. De Drools-backend leest de waarde op
precies twee (`DrlTranspiler` §6.11-datumrekenkunde en §6.10-tijdsduur, plus
dezelfde lezing in `ExecModelTranspiler`) en verdedigt zich daar ad hoc:
`dagsoortGlobaal` geeft `null` en de transpiler zet een `Frag.todo`-marker neer
— de vorm wordt stil niet gedragen in plaats van afgewezen. Deze arm is dus een
wire-arm op het wave-6-`kenmerkSoorten`-argument.

### De drie open beslispunten

**(a) De sleutelkant blijft VRIJ — een beslissing, geen weglating.** Reden,
gemeten: 32 van de 64 sleutels zijn meervoudsspellingen, en de v0-wire draagt
**geen** `plural`-veld voor een dagsoort — ch. 2.9's `plural` staat op de
v1-knoop, en op de wire bestáát het meervoud alleen als tweede sleutel van
precies deze map. Een sleutel heeft dus niets om tegen gekruist te worden, en
een afleiding verzinnen (`-en` erachter) zou een taalspecifieke
morfologieregel in een engine-neutrale IR zetten. Daarbij moet een producent
vrij blijven een derde spelling (een afkorting) te registreren. Wat een sleutel
niet mag is bótsen, en dat is WF-017. Opgeschreven in ch. 4.6 (s), in ch. 2.9
en in de NOTE van het negatief.

**(b) De arm dubbelt WF-017 niet — en WF-017 dekt minder dan zijn eigen tekst
zegt.** Gecontroleerd in de bron en in de positie. WF-017 (`checks.ml`) loopt
`ctx.doc.dagsoorten` en vraagt of een gedeclareerde dagsoortnaam een ingebouwde
kalendereenheid schaduwt; de dagsoort/dagsoort-botsing zit in WF-001's
`ns "dagsoorten"`. Beide zijn **botsingsregels** die niets resolveren; (s) is
een **resolutieregel** die niets vergelijkt. De posities zijn disjunct:
`/dagsoorten` tegenover `/dagsoortPerEenheid/<sleutel>`. Het negatief draagt
één regel, geen tweede citatie.

Het eerste ernstige lenspunt zat in wat ik hierover in ch. 2.9 schréén: mijn
eerste alinea eindigde met "Colliding is what a key may not do, and that is
WF-017" — een claim over de SLEUTELS die de verifier niet waarmaakt. Gemeten
2026-09-09 met de gewapende binary:

    $ verifier.exe e14 (map-sleutel "dagen": "schouwdag")
    verifier: PASS
    $ verifier.exe e15 (gedeclareerde dagsoort "dagen")
    WF-017 FAIL /dagsoorten: day-kind 'dagen' shadows a built-in calendar unit

`wf017` leest de map nergens. De botsingshelft is dus gecontroleerd voor de
gedeclareerde naam en voor géén andere geregistreerde spelling. De
RegelSpraak-frontend handhaaft de sleutelregel wél — `registreerDagsoortEenheid`
weigert een spelling die met een ingebouwde tijdseenheid samenvalt en geeft een
waarschuwing — dus de IR is hier **zwakker dan zijn eigen producent**. ch. 2.9
is herschreven tot een v0-mirror-alinea die de werkelijke dekking spelt en dat
gat bij WF-017 legt, niet bij (s). Dezelfde meting staat nu ook in ch. 4.6 (s)
en in de NOTE van het negatief, zodat (s) niet breder gelezen wordt dan hij is.

**(c) `Constante.naam` is expliciet DOORGESCHOVEN**, met een eigen kaartje
(`backlog/new/20260909-constante-naam-gesloten-verzameling.md`) en met de post
in ch. 4.6's open lijst. Motivering, en die wijkt af van de kaarttekst omdat de
hermeting iets anders liet zien:

1. Het is een **andere vraagklasse**. (s) kruist een naam tegen een
   declaratietabel die het document zelf draagt; `Constante.naam` heeft die
   tabel niet — de toelaatbare namen liggen in de spec vast. De clausule die
   daar hoort is een **opsomming**, en die schrijven is een scope-beslissing
   over wat v1 toelaat.
2. **De kaarttekst klopte niet op één punt.** Zij stelde dat verifier, `ref` en
   clingo de naam niet lezen. Gemeten 2026-09-09: de verifier leest hem
   inderdaad niet, maar `ref` (`eval.ml`) en de ASP-backend
   (`translate.ml`) lezen hem allebei als magische string op `Rekendatum`
   (`ref` ook op `Rekenjaar`), elk met een eigen doorvalregel; en de
   Drools-backend heeft **twee** lezers met **verschillende
   hoofdlettergevoeligheid** (`"Rekendatum".equals(...)` tegenover
   `"de dag".equalsIgnoreCase(...)`). Drie mutanten met echte uitvoer staan in
   het kaartje. Dat is een eigen observability-tabel en eigen getuigen — meer
   dan een smalle arm.
3. Meenemen zou de **arm-exacte mutant onscherp maken**: de acceptance van deze
   kaart eist "alleen deze arm weg → alleen dít ene document weer schoon", en
   een tweede arm met een tweede getuige maakt daar "deze twee" van.

Meting die het kaartje meekrijgt: 13 `Constante`-knopen over 5 documenten in
twee spellingen (`de dag` 11, `Rekendatum` 2), terwijl ch. 4.1 `Rekendatum` en
`Rekenjaar` opsomt — de wire draagt een naam die de rij niet noemt, en de rij
noemt een naam die geen document draagt.

### DRIE spellingen, één arm, twee zonder corpusgetuige

De arm dekt **drie** spellingen met één guard — dat was het kleine lenspunt (b),
want mijn eerste tekst telde er twee. Een waarde die een niet-gedeclareerde
dagsoort noemt; een `null`-waarde; en de **lege string**, die de lens als e05
liet zien:

    WF-034 FAIL /dagsoortPerEenheid/schouwdagen: day-kind unit 'schouwdagen'
    names day kind '', which no declaration carries: …

Reden voor één arm: het schema typeert de waarde `["string","null"]`, dus de nul
is schema-geldig; de lege string is een gewone string die tegen niets resolveert;
en de énige lezer laat de nul met de naamgeving samenvallen (zijn loader leest de
waarde met `asText()`, wat een JSON-null in de string `"null"` verandert en dan
dezelfde lookup mist). Van de 70 ingangen is er **0** nul en **0** leeg, dus
beide stille spellingen zijn in dezelfde arm gevouwen in plaats van eigen
clausules te krijgen — het precedent van de fix-ronde van golf 5. Dat staat zo in
ch. 4.6 (s) en in beide NOTEs.

### De normalisatiehelft: van ongepind naar gepind (fixronde)

Het derde ernstige lenspunt. De arm vergelijkt onder `norm` (whitespace
inklappen, kleine letters) en ch. 4.6 (s) zegt dat ook — maar **niets in het
corpus oefende dat**. Alle 64 baseline-waarden waren byte-identiek aan hun
declaratie, dus de lens-mutant (byte-exacte `List.mem` in plaats van `mem_ci`)
bewoog 0 van 555 documenten en gate 3 bleef groen. Een normatieve zin zonder
machinaal controleerbaar artefact is niet af.

**Wat er is bijgekomen.** `corpus/golden/jutterswet-vormenkabinet` — de
verifier-only getuigenkast van het corpus, die per NOTE al bestaat om
WF-regels positief te getuigen die geen uitvoerend document draagt (WF-017
staat er al in) — declareerde de dagsoort `schouwdag` en registreerde er geen
enkele spelling voor. Nu twee, allebei alleen via `norm` oplosbaar:

    "dagsoortPerEenheid": {
      "schouwdag":   "SchouwDag",
      "schouwdagen": "  schouwdag  "
    }

Eén ingang per helft van de identiteit: kapitalisatie en inklapbare
whitespace. Beide resolveren, dus het document blijft verifier-schoon — het is
een POSITIEVE getuige, en daarom hoort hij in de kast en niet in
`corpus/negatief/`.

**Hermeting van de lens-mutant.** Recept: vervang in
`wf034_dagsoort_eenheden` de tekst
`mem_ci (some_names ctx.doc.dagsoorten) d` door
`List.mem d (some_names ctx.doc.dagsoorten)`; md5
`5850549eee799eb245ea2ebe5676c2b6`. Vóór de getuige: 0 van 555 documenten
bewogen, gate 3 groen. Ná de getuige:

    WF-034 FAIL /dagsoortPerEenheid/schouwdag: day-kind unit 'schouwdag' names
    day kind 'SchouwDag', which no declaration carries: …
    WF-034 FAIL /dagsoortPerEenheid/schouwdagen: day-kind unit 'schouwdagen'
    names day kind '  schouwdag  ', which no declaration carries: …

    check-verifier exit: 1   (FAIL: corpus/golden/jutterswet-vormenkabinet is
                              not verifier-clean)

Exact één document beweegt en gate 3 gaat om. De mutant is dood.

**Waarom in de kast en niet in een eigen golden.** Een NIEUWE golden zou in de
kanaalvloer van gate 12 zijn aangekomen en had daar bij naam gepind moeten
worden (`NOCASE`), en met cases zou `EXPECTED_PLAYED` mee moeten — beide in
`scripts/`, een beschermd pad en een onderhouders-edit. De kast is al
`NOCASE`-gepind, bestaat al voor precies dit doel, en de getuige raakt geen
enkel uitvoerderkanaal. Gate 5 rapporteert zijn no-case-status ongewijzigd:
`NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst
corpus/golden/jutterswet-vormenkabinet`. Gate 12 telt hem ongewijzigd als
`no-case`, gate 13 raakt hem niet (die draait op goldens mét cases). Geen pin
aangepast.

### De sleutelkant-motivering, herschreven (klein punt a)

Mijn eerste formulering ("a key has nothing on the wire to be crossed
against") was te sterk: je zóú een meervoud kunnen afleiden. Het dragende
argument is dat van de brontaal zelf — het meervoud is een **optioneel
auteursveld** (§3.12) en afleiding bij afwezigheid is expliciet buiten de taal
verklaard, dus geen conforme producent is een bepaalde spelling verschuldigd
en een sleutelregel zou morfologie moeten verzinnen die de taal bewust
openlaat. De wire volgt die vorm: het meervoud staat er als tweede SLEUTEL van
deze map en niet als eigen veld. Zo staat het nu in ch. 4.6 (s), in ch. 2.9 en
in de NOTE van het negatief.

### Gemeten sleutelkant-asymmetrie (klein punt d, observatie)

Bij het herschrijven kwam een tweede sleutelkant-bevinding boven die de
verifier vandaag toelaat en die geen enkele regel raakt. De verifier vergelijkt
sleutels onder `norm` (`day_kind_unit`: `norm k = norm a`), maar de
Drools-kant is asymmetrisch: `IrLoader` zet de sleutel **verbatim** weg
(`m.dagsoortPerEenheid.put(k, v.asText())`) terwijl `RegelSpraakModel`
opzoekt met `dagsoortPerEenheid.get(afkorting.trim().toLowerCase())`. Een
document van de wire met sleutel `Schouwdagen` landt dus verbatim en wordt
nooit gevonden. De frontend zelf normaliseert bij het registreren
(`spelling.trim().toLowerCase()`), dus alleen een wire-document kan die vorm
dragen.

Vandaag is dat **inert** en gemeten: e16 (`Schouwdagen` als sleutel) is
`verifier: PASS` en Drools `PLAYED 2` — de opzoeking wordt alleen vanuit de
twee duurvormen (§6.10/§6.11) aangeroepen en dit document draagt die niet.
Buiten scope van deze kaart, hier vastgelegd als observatie: het is een
sleutelkant-vraag, dezelfde familie als het WF-017-gat hierboven.

### Dertien gates groen (2026-09-09)

| gate | uitkomst |
| --- | --- |
| 1 check-self-contained | PASS |
| 2 check-ir-schema | PASS (schema geldig; 187 bestanden valideren en round-trippen) |
| 3 check-verifier | PASS (187 golden schoon; **338** negatief afgewezen op hun eigen regel) |
| 4 check-case-schema | PASS (schema geldig; 442 casebestanden) |
| 5 check-oracle | PASS (419 cases exact herspeeld; NOT replayed, no case file: `jutterswet-seizoenvondst`, `jutterswet-vormenkabinet` — de identiteitsgetuige is verifier-only en gate 5 rapporteert dat ongewijzigd) |
| 6 check-clingo | PASS (419 cases; ref ≡ clingo) |
| 7 check-noop | PASS (383 cases weerleggen een niets-doende uitvoerder; 36 gepinde overlevers) |
| 8 check-rood | PASS (23 rode cases falen waarneembaar in ref én clingo; Drools-been 6 fault-pinned, 17 not-carried) |
| 9 check-catala | PASS (suite 201 checks; 12 catala-goldens byte-identiek) |
| 10 check-regelspraak | PASS (JUnit 173 tests; 37 rgs-goldens byte-identiek) |
| 11 check-toka-parity | PASS (29 cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED, nul stille divergenties) |
| 12 check-drools | PASS (**played 117**, **no-case 2**, kanaalvloer 47 niet-gespeelde goldens bij naam; JUnit 210 tests) |
| 13 check-reftype-derivation | PASS (74 annotaties gedraaid over 15 goldens; 70 replays; 13 PLAYED door de DRL-tekstbranche; 0 gepinde overlevers) |

Gate 12's speelvloer bewoog **niet**: een negatief raakt de goldens niet, en er
is geen golden bíjgekomen — de identiteitsgetuige is in een golden gezet die
al bestond en die al `NOCASE` gepind was, dus hij telt nog steeds als één van
de twee `no-case`-goldens en `played` blijft 117. `EXPECTED_PLAYED` en de
kanaalvloer zijn niet aangeraakt (en `scripts/` is een beschermd pad).

Alle dertien draaiden na de fixronde opnieuw, alle dertien exit 0.

### Scope-uitbreiding in de fixronde, en wat niet aangeraakt is

De kaart noemde als may-touch: `spec/verifier/**`, `spec/IR-SPEC.md`,
`corpus/negatief/**`, `backlog/`. De fixronde voegt daar één bestand aan toe —
`corpus/golden/jutterswet-vormenkabinet/` (twee map-ingangen plus een
NOTE-sectie) — omdat de coördinator daar expliciet om vroeg: de
normalisatiehelft heeft een POSITIEVE getuige nodig en die kan per definitie
niet in `corpus/negatief/` staan. Het is geen anchor, dus geen beschermd pad.

Niet aangeraakt: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `toka/`, `scripts/`
(dus ook `EXPECTED_PLAYED` en de kanaalvloer van gate 12 niet), alle bestaande
tests, en alle bestaande pins. Er is niets gemerged; de branch blijft als
voorstel staan (`spec/IR-SPEC.md` en `spec/verifier/` zijn beschermde paden).
