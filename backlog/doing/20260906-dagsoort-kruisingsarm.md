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
- `backlog/new/20260909-constante-naam-gesloten-verzameling.md` — de zusterpost,
  doorgeschoven met eigen meting.

### De hermeting (2026-09-09, op `4acc6cb` + deze kaart)

De kaartmeting van 2026-09-06 is **niet** overgenomen; alles hieronder is
opnieuw geteld op de aftakking.

| zone | documenten | `dagsoortPerEenheid`-ingangen | `dagsoorten`-declaraties |
| --- | --- | --- | --- |
| golden | 164 (38 anchor) | **4** (in 1 document) | 5 (in 3 documenten) |
| rood | 23 | 0 | 0 |
| negatief | 337 → 338 | 0 → 1 document | 8 (in 7 documenten) |
| pariteit | 30 | 60 (in 30 documenten) | 30 |
| **vier zones** | **554 → 555** | **64** (in 31 documenten) | 43 |

Alle 64 waarden resolveren; **0** dragen `null`. **32 van de 64** sleutels zijn
géén gedeclareerde dagsoortnaam (de meervoudsspellingen). `v "WF-034"`
emissiepunten in `checks.ml`: 43 → **45**.

*Kanttekening bij de kaartgetallen:* de kaart noemde 156 golden en 528
vier-zone-documenten; de gates tellen golden en rood samen (gate 2/3 melden
187 = 164 + 23), en dat verschil is waar de verlopen telling vandaan komt.

### Rood-eerst, met echte uitvoer

Verifier van vóór de arm (md5 `8ea45e866dddcf861b357b430d50b4d1`):

    $ /tmp/verifier-before.exe corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort/ir.json
    verifier: PASS
    exit=0

Verifier mét de arm (md5 `6591e4da76602a4be81c704d051104de`):

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
document krijgt zelfs maar een incidentele regel erbij.

### De arm-exacte mutant

Recept (de pin, want OCaml-builds zijn niet commentaar-invariant en het
buildpad zit in de binary): verwijder de letterlijke tekst
`wf034_dagsoort_eenheden ctx; ` uit `let all` in `spec/verifier/lib/checks.ml`
en bouw met `dune build --root spec/verifier`. Mutant-md5:
`0aaabcf9a3696817cd6b0827f264aebb`.

Over dezelfde 555 documenten tegen de gewapende binary: **exact één** document
wordt weer schoon — `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort`
— en geen enkele andere verdict- of rapportregel verandert. Na herstel is de
binary weer byte-identiek aan de gewapende (`6591e4da…`).

Tweede mutant, de nul-tak alleen (`| None -> None`), md5
`0b4698d31a62b12b899411e0d5487009`: over de 555 documenten beweegt **niets** —
de nul-spelling heeft geen corpusgetuige. Wel falsifieerbaar buiten het corpus:
één veld van de gastheer-golden op `null` is PASS onder die mutant en wordt
door de arm geweigerd (`day-kind unit 'schouwdagen' names no day kind at all`).

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

**(b) De arm dubbelt WF-017 niet.** Gecontroleerd in de bron en in de
positie. WF-017 (`checks.ml`) loopt `ctx.doc.dagsoorten` en vraagt of een
gedeclareerde dagsoortnaam een ingebouwde kalendereenheid schaduwt; de
dagsoort/dagsoort-botsing zit in WF-001's `ns "dagsoorten"`. Beide zijn
**botsingsregels** die niets resolveren; (s) is een **resolutieregel** die
niets vergelijkt. De posities zijn disjunct: `/dagsoorten` tegenover
`/dagsoortPerEenheid/<sleutel>`. Het negatief draagt één regel, geen tweede
citatie. (Terzijde, opgemerkt maar niet in scope: WF-017's ingebouwde-eenheid-arm
loopt alleen over `dagsoorten`, dus een sleutel `dagen` zou vandaag geen
WF-017-verdict opleveren. Dat is WF-017's sleutelkant, niet die van (s).)

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

### De nul-waarde: gedekt, één arm, zonder corpusgetuige

De arm dekt beide spellingen — een waarde die een niet-gedeclareerde dagsoort
noemt én een `null`-waarde — met één guard. Reden: het schema typeert de waarde
`["string","null"]`, dus de nul is schema-geldig, en de énige lezer laat ze
samenvallen (zijn loader leest de waarde met `asText()`, wat een JSON-null in de
string `"null"` verandert en dan dezelfde lookup mist). De nul-helft heeft geen
corpusgetuige (0 van 64) en is daarom in dezelfde arm gevouwen in plaats van een
eigen clausule te krijgen — het precedent van de fix-ronde van golf 5. Dat staat
zo in ch. 4.6 (s) en in de NOTE.

### Dertien gates groen (2026-09-09)

| gate | uitkomst |
| --- | --- |
| 1 check-self-contained | PASS |
| 2 check-ir-schema | PASS (schema geldig; 187 bestanden valideren en round-trippen) |
| 3 check-verifier | PASS (187 golden schoon; **338** negatief afgewezen op hun eigen regel) |
| 4 check-case-schema | PASS (schema geldig; 442 casebestanden) |
| 5 check-oracle | PASS (419 cases exact herspeeld) |
| 6 check-clingo | PASS (419 cases; ref ≡ clingo) |
| 7 check-noop | PASS (383 cases weerleggen een niets-doende uitvoerder; 36 gepinde overlevers) |
| 8 check-rood | PASS (23 rode cases falen waarneembaar in ref én clingo; Drools-been 6 fault-pinned, 17 not-carried) |
| 9 check-catala | PASS (suite 201 checks; 12 catala-goldens byte-identiek) |
| 10 check-regelspraak | PASS (JUnit 173 tests; 37 rgs-goldens byte-identiek) |
| 11 check-toka-parity | PASS (29 cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED, nul stille divergenties) |
| 12 check-drools | PASS (**played 117**, kanaalvloer 47 niet-gespeelde goldens bij naam; JUnit 210 tests) |
| 13 check-reftype-derivation | PASS (74 annotaties gedraaid over 15 goldens; 70 replays; 13 PLAYED door de DRL-tekstbranche; 0 gepinde overlevers) |

Gate 12's speelvloer bewoog **niet**: een negatief raakt de goldens niet, en er
is geen golden bijgekomen. `EXPECTED_PLAYED` is dus niet aangeraakt (en
`scripts/` is een beschermd pad).

### Niet aangeraakt

`spec/CONSTRUCTS.md`, `spec/PLAN.md`, `toka/`, `scripts/`, alle bestaande
tests en pins. Er is niets gemerged; de branch blijft als voorstel staan
(`spec/IR-SPEC.md` en `spec/verifier/` zijn beschermde paden).
