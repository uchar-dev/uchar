# Geen enkele gate pint een case-TELLING — hoort dat zo?

Goal: beslissen of de gate-set naast zijn niet-leeg-vloeren en zijn
naam-gedragen lijsten ook **absolute tellingen** moet pinnen — hoeveel cases
er gespeeld zijn, hoeveel checks een frontend-suite draaide, hoeveel tests een
JUnit-suite — en zo ja, waar en tegen welke prijs. De vraag komt uit de
gate-12-speelvloer (`backlog/done/20260906-gate-12-speelvloer.md`), die de
eerste absolute telling in het huis introduceerde en daarmee zichtbaar maakte
dat er verder geen enkele is. Die kaart noemde de bredere vraag al expliciet
out of scope; dit is die kaart.

## De vondst, mechanisch nagemeten (2026-09-06, basis `f0161ad`)

Eén enkele absolute constante in de hele gate-set:

    $ grep -nE '^[A-Z_]+=[0-9]+' scripts/*.sh
    scripts/check-drools.sh:229:EXPECTED_PLAYED=109

Alles wat de andere twaalf gates aan tellingen kennen is een
**niet-leeg**-vloer — `-eq 0` of `-gt 0` — nooit een aantal:

| gate | wat het telt (2026-09-06) | de vloer die het draagt |
|---|---|---|
| 2 `check-ir-schema` | 177 bestanden valideren + round-trippen | corpusmodus met `n == 0` faalt |
| 3 `check-verifier` | 177 golden schoon, 320 negatief afgewezen | `n_neg -eq 0` faalt; beide leeg faalt |
| 4 `check-case-schema` | 429 casebestanden (407 golden + 22 rood) | `n -eq 0` faalt |
| 5 `check-oracle` | 407 cases exact herspeeld | `n_cases -eq 0` faalt |
| 6 `check-clingo` | 407 cases via clingo | `n_cases -eq 0` faalt; suite-checks `-eq 0` faalt |
| 7 `check-noop` | 371 cases weerleggen een niets-doener | leeg faalt; 36 overlevers **bij naam** |
| 8 `check-rood` | 22 rode cases falen zichtbaar | `n_cases -eq 0` faalt; `d_fault -eq 0` faalt |
| 9 `check-catala` | 201 checks; 12 catala-goldens byte-identiek | `checks -gt 0`; goldenset niet leeg |
| 10 `check-regelspraak` | 173 tests; 30 rgs-goldens byte-identiek | `tests -gt 0`; goldenset niet leeg |
| 11 `check-toka-parity` | 29 cases; niet-nul tests | `tests`, `casus_n`, `cases_n` alle `-gt 0` |
| 12 `check-drools` | 109 spelers, 283 cases; 207 JUnit-tests | **`played -eq 109`**; `tests -gt 0`; `skipped -eq 0` |
| 13 `check-reftype-derivation` | 68 twists over 14 goldens | `n_twists -eq 0` faalt; `n_drl -eq 0` faalt |

Alle *andere* pins in het huis zijn **naam-gedragen**: de overleverslijst van
gate 7, `EXCLUDE_KNOWN` en `EXPECTED_CHANNEL` van gate 12, de holdoutlijst van
gate 11, de overleverslijst van gate 13. Die falen wanneer ze verouderd zijn,
en dat is precies waarom ze werken. Een naam kan verdwijnen; een aantal niet.

De consequentie: **een suite die stilletjes de helft minder draait, blijft
groen.** Draait gate 9 nog 201 checks of nog 40? Draait gate 10 nog 173 tests?
Herspelen gates 5 en 6 nog 407 cases? Geen enkele gate merkt het verschil
zolang het aantal boven nul blijft. Dat is dezelfde stilte die de
gate-12-speelvloer op het spelerskanaal sloot, één abstractieniveau hoger.

## Het gemeten deelbeeld van gate 12 (waarom een gate-12-only vloer niet genoeg is)

Gate 12 speelt vandaag **283 van de 407** golden-cases. Dat aantal is stabiel
en niet flakerig: `PLAYED n` is een zuivere functie van de bestandsverzameling,
want `expectedFault` is in `corpus/golden` verboden (gate 4's zonediscipline,
`scripts/check-case-schema.sh:81`), dus élke `case*.json` naast de `ir.json`
van een speler wordt herspeeld. De verdeling over de 109 spelers, gemeten
2026-09-06:

| cases per golden | 1 | 2 | 3 | 4 | 5 | 6 | 9 |
|---|---|---|---|---|---|---|---|
| aantal goldens | 40 | 16 | 24 | 14 | 10 | 4 | 1 |

40·1 + 16·2 + 24·3 + 14·4 + 10·5 + 4·6 + 1·9 = **283**. De resterende 124
cases horen bij de 46 niet-spelende goldens.

Een `EXPECTED_CASES=283` in gate 12 alléén zou dus een **deelvloer** zijn: hij
dekt 70% van de golden-cases en zegt niets over de suites van gates 9, 10 en
12, niets over de 407 van gates 5/6, niets over de 320 negatieven van gate 3.
Daarom hoort de vraag in één kaart, repo-breed beantwoord, en niet als
losse toevoeging aan het script dat toevallig als eerste een getal kreeg.

## Het bezwaar dat meegewogen moet worden: elke tellingpin maakt een script tot corpusregister

`EXPECTED_PLAYED=109` maakt `scripts/check-drools.sh` óók een
**registerbestand van `corpus/golden`**: elke kaart die een golden toevoegt of
weghaalt raakt voortaan impliciet dit **beschermde pad**, ook als ze met de
Drools-backend niets te maken heeft. Dat is nu al levensecht — de
gate-12-speelvloer en `card/dagsoort-golden` moeten hun mergevolgorde
afstemmen omdat de tweede die landt het getal op 110 moet zetten.

Vermenigvuldig dat met tien gates en elke corpuskaart wordt een kaart die tien
beschermde scripts aanraakt. Dat is niet automatisch te duur — het getal
verhuist mee met de verandering die het veroorzaakt, en de gate zégt precies
wat er moet gebeuren — maar het is de kern van de afweging en het mag niet
impliciet blijven.

Te wegen alternatieven, minstens deze drie:

1. **Absolute pins per gate** (het `EXPECTED_PLAYED`-model). Maximale
   gevoeligheid, maximale koppeling. Elke corpuskaart raakt elk gepind script.
2. **Eén registerbestand** dat álle verwachte tellingen draagt, door de gates
   gelezen. De koppeling blijft, maar concentreert in één bestand met één
   review-moment; de scripts blijven over hun eigen logica gaan. Weeg of dat
   bestand zelf beschermd moet zijn, en of het niet gewoon `EXPECTED_PLAYED`
   met extra stappen is.
3. **Monotone ondergrenzen** in plaats van gelijkheid (`>= 407`): vangt
   krimp, laat groei ongemoeid, en dwingt geen bump bij elke toevoeging.
   Zwakker (een gelijk blijvende telling die intern verschoof glipt erdoor)
   maar vrijwel koppelingsvrij. Weeg dit expliciet: het is het goedkoopste
   antwoord op de vraag "draait de suite nog evenveel?" en misschien het enige
   dat schaalt.

## Acceptance

- **Eén beslissing voor de hele gate-set**, expliciet gemotiveerd, met een
  genoemd alternatief en wat dat gekost zou hebben. "Per gate apart bekijken"
  is geen antwoord; wél een geldig antwoord is "alleen daar waar de telling
  een suite dekt die de corpus niet dupliceert" — mits beargumenteerd.
- **Het koppelingsbezwaar hierboven wordt in de motivering geadresseerd**, met
  een schatting van hoeveel beschermde scripts een gemiddelde corpuskaart dan
  raakt.
- Landt er een vloer, dan **rood eerst**: een kunstmatig verlaagde telling
  (een case tijdelijk weggehaald, een test tijdelijk uitgezet) maakt de gate
  rood mét naam of getal, en de onaangeroerde kop blijft groen. Per gate die
  een vloer krijgt, één meting.
- Landt er géén vloer, dan is de kaart alsnog af: de motivering waarom
  niet-leeg genoeg is, komt als notitie bij de gate-set-beschrijving in
  `CLAUDE.md` (voorstel — beschermd pad), zodat de vraag niet elk kwartaal
  opnieuw wordt gesteld.
- Alle dertien gates groen; de tellingen in deze kaart worden bij aanvang
  **hermeten** (ze zijn van 2026-09-06 en de corpus groeit).

May touch: `scripts/**` (voorstel — beschermd pad; de maintainer merget),
`CLAUDE.md` (voorstel — idem, alleen de gate-set-beschrijving), `backlog/`.

Out of scope: welke vormen de Drools-backend draagt (de backendkaarten), en
de vraag of de 46 niet-spelende goldens er minder moeten worden — deze kaart
gaat over het **zien** van een telling, niet over de hoogte ervan. De
speelvloer zelf (`EXPECTED_PLAYED`) staat vast en wordt hier niet heropend,
alleen als precedent gewogen.

---

## Result (2026-09-10, kop `e269233` + deze tak)

**Het besluit, uitgevoerd voor de hele set: monotone ondergrenzen** —
alternatief 3 van de kaart, maintainerbesluit 2026-09-09 (genummerde vraag 7).
Zestien `EXPECTED_MIN_<naam>`-constanten in tien scripts, elk met zijn reden
en zijn meetdatum; elke vergelijking faalt met het gemeten aantal **en** de
vloer in de melding, en elke PASS-regel noemt zijn vloer nu ook.

De tellingen zijn bij aanvang hermeten; ze waren alle gegroeid sinds
2026-09-06 (192 documenten, 350 negatieven, 453 cases, 427 replays, 391
weerleggingen, 26 rood, 239 JUnit, 74 twists).

### De vloertabel

| gate | vloer(en) | motivering |
|---|---|---|
| 1 `check-self-contained` | **geen vloer, wel een naampin** | heeft geen telling om te vloeren: een tripwire over élk getrackt bestand, dus geen suite die kan halveren. Zijn fixtureset — de `ZONES`-lijst — is bij **naam** gepind (slotronde F3): een verdwenen of hernoemde zone faalde niet maar werd stil overgeslagen, en faalt nu bij naam |
| 2 `check-ir-schema` | `EXPECTED_MIN_DOCS=192` | de documenttelling van beide artefactzones; alleen in corpusmodus |
| 3 `check-verifier` | `EXPECTED_MIN_NEGATIEF=350` | de enige gate die de negatieven überhaupt ziet — een verdwenen `wf###`-case neemt de falende getuige van een MUST mee. **Geen** vloer op `n_golden`: dat is byte-voor-byte de verzameling die gate 2 al vloert |
| 4 `check-case-schema` | **geen** | zijn telling is de disjuncte som van de twee zonetellingen die gates 5 en 8 elk apart vloeren; een vloer op de sóm is het zwakkere instrument (een groene case geruild tegen een rode houdt de som gelijk) |
| 5 `check-oracle` | `EXPECTED_MIN_EXAMPLES=489`, `EXPECTED_MIN_CASES=427` | de ref-suite (zie de dichting hieronder) en de groene zone |
| 6 `check-clingo` | `EXPECTED_MIN_SUITE_CHECKS=83` | de eigen unit-suite is geen corpusbestand, dus niets anders telt hem. **Geen** vloer op de replaytelling: dezelfde verzameling als gate 5 |
| 7 `check-noop` | **geen** | `n_refuted` = cases − overlevers, en beide termen zijn al harder gepind: de cases door gate 5's vloer, elke overlever bij naam in de reden-dragende lijst die faalt als hij verouderd is |
| 8 `check-rood` | `EXPECTED_MIN_ROOD_CASES=26` | de rode zone; de Drools-leg krijgt geen tweede vloer (zijn niet-gedragen cases staan bij naam in de exclusielijst) |
| 9 `check-catala` | `EXPECTED_MIN_CHECKS=201`, `EXPECTED_MIN_GOLDENS=12` | suite plus de `bron.catala_en`-dekking, die geen andere gate leest (gate 2 vloert de `ir.json` ernaast, dus een verwijderde bron zou stil dekking kosten) |
| 10 `check-regelspraak` | `EXPECTED_MIN_TESTS=173`, `EXPECTED_MIN_GOLDENS=38` | idem voor `bron.rgs` |
| 11 `check-toka-parity` | `EXPECTED_MIN_TESTS=27`, `EXPECTED_MIN_FRAGMENTS=45`, `EXPECTED_MIN_CASES=29` | de zone ligt buiten `corpus/`: geen andere gate leest er één bestand van. De casus-dumptelling is bewust niet gevloerd — die is 1 en de bestaande `-gt 0` ís die vloer. Deze constanten verlaten de gate-set mét deze gate |
| 12 `check-drools` | `EXPECTED_MIN_TESTS=239` | de JUnit-suite, het enige getal van deze gate dat ongezien kon halveren. `EXPECTED_PLAYED` blijft **gelijkheid** en is niet heropend; de replaytelling is een deelverzameling van wat gate 5 vloert plus `EXPECTED_CHANNEL` bij naam |
| 13 `check-reftype-derivation` | `EXPECTED_MIN_TWISTS=74`, `EXPECTED_MIN_DRL_PLAYED=13` | de twistverzameling is de meetkracht van de harnas, geen bestandstelling: een versmalde twisttabel krimpt hem met elk corpusbestand op zijn plek. De DRL-vloer verlengt de bestaande `-eq 0`-bodem, wiens eigen commentaar de gemeten mutant vastlegt die de leg van 9 naar 0 gespeelde goldens bracht — die mutant wordt nu al bij 8 gevangen |

### Het gat dat de lensrondes maten, gedicht

`ref/test/test_ref.ml` — 489 gewerkte voorbeelden, de uitvoerbare vorm van de
voorbeelden die `RUNTIME-SEMANTICS.md` bij elke clausule verschuldigd is —
werd door **geen enkele** gate gedraaid; hermeten en bevestigd (geen script
noemde `test_ref`, `ref/test`, `dune runtest`, `dune test` of een
runtest-alias van `ref`). Dat is niet enkel een ongedraaide suite maar een gat
in de gate-set: het zijn spec-artefacten.

Gedicht in **gate 5**, niet als nieuwe gate. Gate 5 bezit `ref/` al, bouwt het
al (`dune build ref` produceert `_build/default/ref/test/test_ref.exe` zonder
extra stap), en gate 6 doet voor de clingo-backend woordelijk hetzelfde:
suite draaien, de tellingsregel eisen, de telling vloeren. Eén huispatroon, en
geen nieuwe gate — dus geen nieuw CI-been en geen uitbreiding van de gate-set
zelf. `CLAUDE.md`'s beschrijving van gate 5 vermeldt het nu.

De clingo-eigen suite (83 checks) draaide al onder gate 6 — bevestigd in het
script (`SUITE_BIN`, `SUITE_CHECKS`) en hij krijgt zijn vloer; hij rapporteert
zijn telling voortaan ook in de PASS-regel, wat hij niet deed.

### Het koppelingsbezwaar, met de schatting die de acceptance vraagt

Het bezwaar is juist voor **gelijkheidspins** en vrijwel leeg voor
**ondergrenzen**, omdat alleen krimp faalt.

- Een gewone corpuskaart die materiaal **toevoegt** raakt onder dit regime
  **nul** beschermde scripts: 193 ≥ 192, 430 ≥ 427, 39 ≥ 38, 76 ≥ 74. De enige
  uitzondering is de reeds bestaande gelijkheidspin `EXPECTED_PLAYED` van gate
  12 — **één** script, en die stond er al.
- Onder alternatief 1 (gelijkheidspins per gate) zou diezelfde kaart — één
  golden, drie cases, een `bron.rgs`, unit-dragend — de getallen van gates 2,
  4, 5, 6, 7, 10, 12 en 13 moeten bijstellen: **zeven tot negen** beschermde
  scripts per corpuskaart, plus een mergevolgorde-afhankelijkheid tussen elke
  twee gelijktijdige corpuskaarten voor elk gedeeld getal. Dat laatste is niet
  hypothetisch: de kaart noemt het levende geval van twee takken die hun
  volgorde moeten afstemmen om één getal.
- Alternatief 2 (één registerbestand) houdt de koppeling op één bestand met
  één reviewmoment, maar maakt dat bestand een serialisatiepunt waar élke
  corpuskaart met élke andere botst — precies wat `WAY-OF-WORKING.md` §1 als
  "de kaarten waren verkeerd geknipt" bestempelt. Voor de ene gate die al een
  telling heeft is het bovendien `EXPECTED_PLAYED` met extra stappen.
- Een kaart die bewust materiaal **weghaalt** raakt één script: dat waarvan
  hij de telling verlaagt. Daar is de bijstelling het punt, geen belasting —
  de gate zegt precies welk getal, en de melding noemt beide.

Ophogen gebeurt bewust en bij gelegenheid (een kaart die het script toch al
opent, of een gerichte veegkaart), niet automatisch per toevoeging.

### De erkende zwakte, opgenomen en niet verzwegen

Een telling die **gelijk blijft terwijl haar inhoud verschoof** passeert: een
golden-case weggehaald en in dezelfde commit een andere toegevoegd houdt 427
overeind en gate 5's vloer ziet niets. Dat is de prijs van monotonie. Wat die
richting wél dekt is niet de telling maar de naam: gate 7's overleverslijst,
gate 11's holdoutlijst, gate 12's `EXPECTED_CHANNEL` én `EXPECTED_PLAYED`,
gate 13's overleverslijst — alle vier falen wanneer een naam verouderd is —
plus het feit dat een ingeruilde case nog steeds exact aan de gepinde
verwachtingen van gates 5, 6 en 8 moet voldoen. De rest is een beoordeelde
corpuswijziging, geen stille regressie. Deze zin staat ook in `CLAUDE.md` en
in het commentaarblok van elk gevloerd script.

### Rood eerst, per gate één meting (echte output)

| gate | wat kunstmatig verlaagd is | de melding |
|---|---|---|
| 2 | één `ir.json` van een golden opzij | `FAIL: 191 document(s) validated, but EXPECTED_MIN_DOCS pins at least 192` |
| 3 | één negatief-case opzij | `FAIL: 349 negatief case(s) rejected, but EXPECTED_MIN_NEGATIEF pins at least 350` |
| 5 (suite) | één `expect_num`-regel uitgezet in `ref/test/test_ref.ml` | `FAIL: the ref suite ran 488 worked examples, but EXPECTED_MIN_EXAMPLES pins at least 489` |
| 5 (cases) | één golden-casebestand opzij | `FAIL: 426 golden case(s) replayed, but EXPECTED_MIN_CASES pins at least 427` |
| 6 | één `check`-aanroep uitgezet in `backends/clingo/test/test_clingo.ml` | `FAIL: the backend unit suite ran 82 checks, but EXPECTED_MIN_SUITE_CHECKS pins at least 83` |
| 8 | één rood casebestand opzij | `FAIL: 25 red case(s) replayed, but EXPECTED_MIN_ROOD_CASES pins at least 26` |
| 9 | één `bron.catala_en` opzij | `check-catala: FAIL — 11 catala golden(s) regenerated, but EXPECTED_MIN_GOLDENS pins at least 12` |
| 10 | één `bron.rgs` opzij | `check-regelspraak: FAIL — 37 rgs golden(s) re-emitted, but EXPECTED_MIN_GOLDENS pins at least 38` |
| 11 | één testklasse uitgesloten van de run (`--exclude-classname`, tijdelijk in het script; de zone zelf is niet aangeraakt) | `check-toka-parity: FAIL — the suite ran 1 tests, but EXPECTED_MIN_TESTS pins at least 27` |
| 12 | één getuigetestklasse tijdelijk niet meegecompileerd | `check-drools: FAIL — the suite ran 238 tests, but EXPECTED_MIN_TESTS pins at least 239` |
| 13 | de casebestanden van één twistbare golden opzij | `FAIL: 72 annotation(s) twisted, but EXPECTED_MIN_TWISTS pins at least 74` |

| 2 (nulpad) | alle corpuszones hernoemd (n = 0) | `FAIL: 0 document(s) validated, but EXPECTED_MIN_DOCS pins at least 192` |
| 3 (nulpad) | alle corpuszones hernoemd (n = 0) | `FAIL: 0 negatief case(s) rejected, but EXPECTED_MIN_NEGATIEF pins at least 350` |
| 1 (naampin) | één artefactzone hernoemd | `FAIL: artifact zone 'spec/schema' is missing — a vanished or renamed zone stops being swept ...` |

Elke verlaging is onmiddellijk teruggedraaid; de werkboom was na elke meting
schoon (`git status --porcelain` leeg). Niet apart rood getoond, met dezelfde
vergelijkingsvorm en dezelfde melding: gate 9's checks-vloer, gate 10's
tests-vloer, gate 11's fragment- en casesvloer, gate 13's DRL-vloer — de kaart
vraagt één meting per gate.

### Eindkop: alle dertien groen

    gate 1 check-self-contained rc=0    gate 8  check-rood rc=0
    gate 2 check-ir-schema rc=0         gate 9  check-catala rc=0
    gate 3 check-verifier rc=0          gate 10 check-regelspraak rc=0
    gate 4 check-case-schema rc=0       gate 11 check-toka-parity rc=0
    gate 5 check-oracle rc=0            gate 12 check-drools rc=0
    gate 6 check-clingo rc=0            gate 13 check-reftype-derivation rc=0
    gate 7 check-noop rc=0

### Voorstel op beschermde paden

`scripts/**` en `CLAUDE.md` zijn beschermd; deze tak is een **voorstel** en is
niet gemerged. `CLAUDE.md` krijgt twee wijzigingen, beide binnen de
gate-set-beschrijving: één alinea "Counting floors" na gate 13, en één zin bij
gate 5 over de ref-suite. Verder is niets aangeraakt — geen `CONSTRUCTS.md`,
geen `PLAN.md`, geen `toka/`, geen pin, geen `EXPECTED_PLAYED`.

---

## Slotronde vóór de merge (2026-09-10, lens 1)

Vijf punten, alle vijf uitgevoerd; geen nieuwe constanten, dus nog steeds
zestien vloeren over tien scripts.

- **F1 — de vloer stond achter de leeg-corpus-tak.** Gates 2 en 3 hadden hun
  `n == 0 → exit 0`-tak ("corpus not yet populated") vóór de vloerevaluatie
  staan, dus met de zones opzij gaven ze PASS bij nul — precies het geval
  waarvoor de vloer bestaat. De vloer staat nu ervóór; de leeg-corpus-tak is
  alleen nog bereikbaar zolang de vloer nul is. In gate 3 gaat de vloer ook
  vóór de bestaande "geen negatieven"-melding, zodat het nulgeval met **beide**
  getallen wordt gemeld in plaats van met de zwakkere niet-leeg-tekst. Nulpad
  gemeten: zie de twee nieuwe rijen in de roodtabel hierboven.
- **F2 — één formulering.** Gate 4's afwezigheidsmotivering zei dat bij een
  geruilde case "both zone floors would see it". Gemeten vangt er precies één:
  die van de zone die **verloor**; een ondergrens zegt niets over een zone die
  groeide. Herformuleerd, met die reden erbij.
- **F3 — gate 1 claimde te veel.** De zin "no fixture set of its own" was
  onwaar: de `ZONES`-lijst is er een, en `[ -e "$zone" ] || continue` sloeg een
  verdwenen zone stil over (gemeten: `spec/schema` opzij gaf PASS). Gekozen voor
  de route die de zin wáár maakt in plaats van hem af te zwakken: een
  ontbrekende zone faalt nu bij naam, waarmee de lijst de naampin van het huis
  wordt. Geen legitiem pad breekt — alle tien zones bestaan, `toka/` staat
  bewust niet in de lijst, en gate 1 is groen op de onaangeroerde kop. De twee
  latere lussen houden hun stille `continue`, met de reden erbij: het bestaan is
  één lus eerder al bij naam vastgesteld, dus één luide melding in plaats van drie.
- **F4 — de verwijzing klopte niet.** De gate-5-zin in `CLAUDE.md` verwees naar
  "exactly as gate 6 runs the clingo backend's own suite", terwijl de
  gate-6-alinea die suite niet noemde. Gate 6's beschrijving noemt nu zijn eigen
  unit-suite en diens vloer.
- **F5 — de tweeargumentvorm betaalde de hele suite.** Het suiteblok stond boven
  de `exec`, dus wie één paar wilde naspelen draaide eerst 489 voorbeelden.
  Verplaatst naar onder de `exec`: de debugvorm keert nu in 0,04 s terug, de
  gate draait de suite onverminderd. (Gate 6 houdt zijn eigen volgorde; dat
  script is deze ronde niet geopend.)

Herdraaid na de wijzigingen: gates 1, 2, 3, 4 en 5 — alle groen.
