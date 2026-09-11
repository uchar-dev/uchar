# De kenmerkTijdlijnAantal-poort is containsKey, niet "twee of meer"

Goal: de Drools-poort op de genavigeerde kenmerktijdreeks (§8.2/ch. 3.9)
weigert vandaag elk document dat `kenmerkTijdlijnAantal` voor dat kenmerk
*draagt*, ook bij de waarde **1** — een wire-legale, verifier-schone,
semantisch enkelvoudige count. Bedoeld is de meerdere-tijdlijnen-poort; wat
er staat is een aanwezigheidspoort. Latente over-afwijzing: vandaag
onzichtbaar omdat de frontend de sleutel bij 1 nooit schrijft, maar elke
handgebouwde of door een andere producent geschreven IR loopt erin.

## De vindplaats

Beide takken, identiek:

- `backends/drools/src/main/java/…/transpiler/DrlTranspiler.java`,
  `navigatieKenmerkTijdreeks`
- `backends/drools/src/main/java/…/transpiler/model/ExecModelTranspiler.java`,
  `navigatieKenmerkTijdreeks`

```java
if (doel == null || !doel.kenmerkGranulariteit.containsKey(k.kenmerk())
        || doel.kenmerkTijdlijnAantal.containsKey(k.kenmerk())) return null;
```

De derde disjunct is de poort. `null` betekent hier: de vorm wordt niet
gelowerd — de exec-model-tak levert een `transpiler SKIP (voorwaarde buiten
Phase-B scope)`, de DRL-tak een `MARKER TODO §8.2 gehele-periode`. Beide
luid, geen van beide onjuist bij een écht meervoudig raster; allebei
onterecht bij `count: 1`.

Waarom het niet opvalt: de emitter schrijft de sleutel alleen bij een andere
waarde dan 1 —
`frontends/regelspraak/src/main/java/…/resolver/DefinitionResolver.java`:

```java
if (aantal != 1) ot.kenmerkTijdlijnAantal.put(naam, aantal);
```

Een frontend-document draagt hem dus nooit bij 1. **Het corpus wél**, en dat
maakt de zaak sterker in plaats van zwakker (geteld 2026-09-11):

| zone | bestand | veld | waarde |
|---|---|---|---|
| negatief | `wf034-wees-kenmerktijdlijnaantal` | `kenmerkTijdlijnAantal` `Vondst.verjaart` | 1 |
| negatief | `wf055-navigated-clip-condition` | `kenmerkTijdlijnAantal` `Post.wachtplichtig` | 1 |
| **golden** | `jutterswet-vormenkabinet` | `attribuutTijdlijnAantal` `Schouwveld.waterstand` | **1** |
| negatief | `wf034-wees-attribuutdimensies` | `attribuutTijdlijnAantal` `Schouwveld.waterstand` | 1 |
| negatief | `wf034-wees-attribuuttijdlijnaantal` | `attribuutTijdlijnAantal` `Vondst.vondstdatum` | 1 |
| negatief | `wf005-count-zero` | `attribuutTijdlijnAantal` `Jutter.aantal aangemelde vondsten` | 0 |

De praktische conclusie overleeft ongeschonden: de negatieve zone bereikt
gate 12 nooit (gate 3 wijst die documenten juist af), en `vormenkabinet` is
het verifier-only kabinet zónder case, dus geen van deze zes speelt vandaag.
Maar twee dingen veranderen. Ten eerste is de sleutel-bij-1 dus een vorm die
in dit repository al geschreven wordt, niet alleen een hypothetische
handbouw. Ten tweede heeft de spiegelvraag hieronder (punt 3) daarmee een
**levende corpusgetuige**: `jutterswet-vormenkabinet` draagt
`attribuutTijdlijnAantal: 1` op precies het attribuut waarover het een
§8.2-gehele-periode-vergelijking stelt. Zodra dat kabinet ooit een case
krijgt — de NOTE beschrijft de voorwaarde — loopt het in de attribuut-kant
van dezelfde poort.

De poort is daarmee onbereikbaar via de toolchain, bereikbaar via de hand, en
al aanwezig in de zones die vandaag niet spelen — precies de asymmetrie die
een latente fout oplevert.

## Hoe het gemeten is (2026-09-11)

Gevonden doordat het een meting op kaart `leegrelatie-ch39-divergentie`
verkéérd liet uitvallen: de handgebouwde fixture droeg
`"kenmerkTijdlijnAantal": {"bedrijvig": 1}`, beide Drools-takken weigerden,
en dat is als "Drools draagt de kenmerkvorm niet" gerapporteerd. Het
tegendeel is waar.

Meetrecept (reproduceert in twee minuten):

1. Neem `corpus/golden/synth-gehelejaarkenmerk/ir.json` — vier cases, beide
   takken PLAYED 4.
2. Zet in `objecttypen.Werf` de sleutel `kenmerkTijdlijnAantal` op
   `{"bedrijvig": 1}`. Verder niets.
3. `scripts/check-verifier.sh <ir>` → PASS (de sleutel is welgevormd).
4. `ref` en clingo → onveranderd PASS op alle vier de cases (de semantiek is
   identiek; beide lezen `kenmerkGranulariteit` en niets anders).
5. `java -jar backends/drools/target/uchar-drools-backend-*-shaded.jar <ir>
   <cases>` en dezelfde aanroep met `--drl` → beide slaan om naar EXCLUDE.

Eén sleutel, één waarde, geen semantisch verschil, twee uitvoerders minder.

## Wat er te beslissen valt

1. Is de poort bedoeld als `>= 2`? Dan is het een eenregelige correctie in
   beide takken, plus een getuige (de fixture uit stap 2 als tweede case-set,
   of een unit-test in de backend-suite).
2. Of is `aantal` bij 1 überhaupt geen wire-vorm? Dan hoort de *schema*- of
   WF-kant hem te weren, niet de backend stil. Let op de spiegelvraag: WF-005
   draagt `aantal` als modellaagveld, en ch. 3.2 verklaart elk multi-period
   raster (`count` >= 2) OPEN — dus "1" is juist de enige gedefinieerde
   waarde, wat voor lezing 1 pleit.
3. Dezelfde `containsKey`-vorm bestaat op de *attribuut*-kant
   (`attribuutTijdlijnAantal`), op zes plaatsen in `DrlTranspiler` —
   meenemen in dezelfde meting, niet aannemen. Hier ligt de getuige al
   klaar: `corpus/golden/jutterswet-vormenkabinet` draagt
   `attribuutTijdlijnAantal: {"waterstand": 1}` op het attribuut waarover
   het een §8.2-vergelijking stelt. Dat kabinet heeft bewust geen case, dus
   de poort wordt daar vandaag niet aangeraakt; wie de attribuut-kant meet
   heeft aan dat document genoeg om de vraag scherp te stellen.

## Acceptance

- Een uitspraak welke van de twee lezingen geldt, met de meting erbij.
- Bij lezing 1: beide takken gecorrigeerd, met een getuige die rood was en
  groen wordt; gate 12 groen met `EXPECTED_PLAYED` ongewijzigd (de bestaande
  golden speelt al).
- Bij lezing 2: de weigering verhuist naar schema/verifier, met een
  negatief-case, en de backends verliezen hun stille tak.
- Het corpus mag de sleutel bij 1 daarna niet stil dragen: wat de beslissing
  ook is, één van beide kanten moet hem opmerken.

May touch: `backends/drools/`, `spec/schema/`, `spec/verifier/` (voorstel),
`corpus/negatief/`, `backlog/`.

## Out of scope

- De ch. 3.9-semantiek zelf: geland op `leegrelatie-ch39-divergentie`
  (2026-09-11), met `corpus/golden/synth-gehelejaarkenmerk` als getuige in
  alle drie de uitvoerders.
- Multi-period rasters (`count` >= 2) semantiek geven: ch. 3.2/3.12 OPEN, een
  eigen kaart.

## Result (2026-09-11)

**Lezing 1 geldt: het periodeaantal is een MEERVOUDIGHEID, en 1 is de
default.** Beide takken gecorrigeerd, getuige rood gedraaid en groen gemaakt,
gate 12 en 13 ongewijzigd groen met `EXPECTED_PLAYED` op 124.

### De uitspraak, met de grond eronder

De spec zegt het op vier plaatsen en de verifier doet het al:

- `IR-SPEC.md` ch. 2.8 stelt de facet als `count | integer >= 1 | periods per
  segment`;
- het uitgewerkte voorbeeld daar adopteert `{granularity: year, count: 1,
  timelineName: "penningjaar"}` — een gewone enkelvoudige facet die de count
  expliciet als 1 schrijft;
- ch. 2.10's v0-migratienoot: "a v0 granularity becomes `{granularity,
  count: 1}`";
- WF-005 verklaart `count < 1` tot de overtreding, niet `count = 1`; en de
  verifier zelf vult in de named-timeline-adoptie van `wf005` een afwezige
  entry aan met `| None -> 1`.

Een expliciete 1 is dus dezelfde facet als geen sleutel, en een raster begint
bij 2. Lezing 2 (de wire-vorm weren in schema/verifier) valt daarmee af: er is
niets te weren — en zou hij wél geweerd worden, dan zou de ch.-2.8-facet met
`count: 1` op de wire onschrijfbaar zijn.

### Gemeten — vóór

Gate 12 en 13 op de onaangeroerde tak (de nulmeting):

    check-drools: PASS
      played 124  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 44 / value-divergence 1 / no-case 2
      channel floor 47 non-PLAYED goldens pinned by name; played matches the pinned 124
      JUnit 310 tests, floor 239  (the backend's own suite, green with a non-zero count)

    check-reftype-derivation: PASS (83 unit annotations twisted over 16 goldens, floor 74;
    74 executor replays reproduce the untouched outcome, 14 of them PLAYED by the DRL-text
    branch, floor 13; 0 pinned survivors)

Daarna de eigen meting, breder dan het meetrecept van de kaart: over ELK van
de 16 goldens met temporele leden én cases, per temporeel lid één expliciete
`1` in de bijbehorende telling-tabel, en beide Drools-takken vergeleken met
hun EIGEN onaangeroerde statusregel (gate-13-patroon — de DRL-tak faalt op
`anchor/jutterswet-borgdeel` ook onaangeroerd, dus een vergelijking met
"PLAYED" zou daar liegen).

**32 van de 46 ledenposities kantelden uit het gespeelde kanaal, over 12 van
de 16 goldens.** In beide takken dezelfde 32. Voorbeelden, letterlijk:

    synth-gehelejaarkenmerk | Werf.bedrijvig (kenmerk) |
      exec: EXCLUDE transpiler SKIP: regel: toeslag van een schuit — regel (voorwaarde buiten Phase-B scope)
      drl:  EXCLUDE drl-transpiler MARKER: regel: toeslag van een schuit — TODO §8.2 gehele-periode

    synth2-clip-over-prorata | Jutter.borgdeel (attribuut) |
      exec: EXCLUDE transpiler SKIP: regel (meer-periodieke temporele doel — §13.3.6.7, geen producent)
      drl:  EXCLUDE drl-transpiler MARKER: TODO meer-periodieke temporele toekenning (§13.3.6.7)

    jutterswet-tempototaal | Jutter.bewaarvergoeding per maand (attribuut) |
      exec: EXCLUDE transpiler SKIP: regel (aggregatie buiten Phase-C scope)
      drl:  EXCLUDE drl-transpiler MARKER: TODO totaal van een meer-periodieke tijdreeks (§13.3.6.7)

De drie andere uitvoerders bewegen op diezelfde documenten niet. Op
`synth-gehelejaarkenmerk` met `Werf.kenmerkTijdlijnAantal {"bedrijvig": 1}` en
op `synth2-clip-over-prorata` met `borgtarief`/`borgdeel`/`borgplichtig` alle
drie op 1:

    verifier: PASS   (beide documenten)
    oracle: PASS (3 expectations)   — elk van de vier, resp. de ene case
    clingo: PASS (3 expectations)   — idem

Eén telling erbij, over alle vier de zones plus de pariteitszone: het hele
repository draagt **zes** count-entries — vijf keer 1 en één keer 0 (de
WF-005-getuige `corpus/negatief/wf005-count-zero`). **Geen enkel document,
waar dan ook, draagt 2 of meer.** De weigeringstakken vuurden dus precies op
de verkeerde populatie: op de vijf enen, en op niets anders.

### Gebouwd

1. `backends/drools/.../transpiler/kern/Tijdlijnen.java` — één gedeeld
   predicaat `meerPeriodiek(tijdlijnAantal, lid)`, `aantal != null && aantal > 1`.
   In `kern/` om dezelfde reden als `Namen`: zo kunnen de takken het niet
   oneens worden. Rauwe `Map.get`, dus hoofdlettergevoelig zoals voorheen —
   die discipline is niet aangeraakt.
2. **24 plekken** omgezet, 12 per tak: 17 `attribuutTijdlijnAantal` + 7
   `kenmerkTijdlijnAantal` — precies de aantallen die `checks.ml` en
   `IR-SPEC.md` ch. 4.6 als lezerstelling noemen. Negaties behouden. Eén
   commentaarregel meegetrokken die de toets als lidmaatschap beschreef
   ("gran set AND in attribuutTijdlijnAantal").
3. `MeerPeriodiekePoortWitnessTest` — 8 checks, additief, geen bestaande test
   aangeraakt. Vijf gepoorte posities (genavigeerde kenmerktijdreeks, eigen
   kenmerktijdreeks, temporeel doel + zijn meer-periodieke markertak,
   tijdsevenredige basis, totaal-van), het predicaat zelf, de
   over-acceptatieflank en de §8.2-meting van punt 3. Elk model wordt in het
   geheugen uit een golden gebouwd; het corpus wordt gelezen, nooit
   geschreven.
4. `corpus/golden/synth-gehelejaarkenmerk/NOTE.md` — de sectie "One key this
   document must not grow" beschreef de poort als een levend risico en wees
   naar `backlog/new/`. Na de fix is dat onwaar. Herschreven. **Dit ligt
   buiten "May touch"**, bewust: het is de directe, feitelijke consequentie
   van deze wijziging op een onbeschermd, niet-test-, niet-scriptbestand, en
   een corpusnotitie die liegt is erger dan een overschrijding van de
   contractlijst. De IR van die golden is NIET aangeraakt.

### Rood, dan groen

Rood op de huidige code (`mvn -o -f backends/drools/pom.xml test
-Dtest=MeerPeriodiekePoortWitnessTest`):

    Tests run: 8, Failures: 0, Errors: 5, Skipped: 0
      de_eigen_kenmerktijdreeks_speelt_bij_aantal_een » Excluded transpiler SKIP: regel: borgdeel van een jutter — regel (temporele toekenning buiten Phase-F scope)
      de_genavigeerde_kenmerktijdreeks_speelt_bij_aantal_een » Excluded transpiler SKIP: regel: toeslag van een schuit — regel (voorwaarde buiten Phase-B scope)
      de_tijdsevenredige_basis_speelt_bij_aantal_een » Excluded transpiler SKIP: regel: borgdeel van een jutter — regel (temporele toekenning buiten Phase-F scope)
      het_temporele_doel_speelt_bij_aantal_een » Excluded transpiler SKIP: regel: borgdeel van een jutter — regel (meer-periodieke temporele doel — §13.3.6.7, geen producent)
      het_totaal_van_een_tijdreeks_speelt_bij_aantal_een » Excluded transpiler SKIP: regel: bewaargeld van een jutter — regel (aggregatie buiten Phase-C scope)

De drie overige checks stonden vóór de fix al groen, en dat is opzet: het
predicaat zelf, de over-acceptatieflank (aantal 2 blijft in beide takken
geweigerd — zonder die check zou "de poort wegstrepen" alle andere tests
halen) en de §8.2-meting hieronder.

Na de fix: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.

### Gemeten — ná

Dezelfde corpusbrede injectie: **0 van de 46 verschillen**. Elk van de 46
posities reproduceert nu in beide takken de eigen onaangeroerde statusregel.

    check-drools: PASS
      played 124  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 44 / value-divergence 1 / no-case 2
      channel floor 47 non-PLAYED goldens pinned by name; played matches the pinned 124
      JUnit 318 tests, floor 239  (the backend's own suite, green with a non-zero count)

    check-reftype-derivation: PASS (83 unit annotations twisted over 16 goldens, floor 74;
    74 executor replays reproduce the untouched outcome, 14 of them PLAYED by the DRL-text
    branch, floor 13; 0 pinned survivors)

    check-self-contained: PASS

Gate 12 verschilt op precies één getal van de nulmeting: 310 -> 318 tests, de
acht nieuwe checks. `played` staat onveranderd op 124; geen golden wisselde
van kanaal, dus aan `EXPECTED_PLAYED` is niets aangepast en niets voor te
stellen.

De rest van de set, dezelfde sessie, alle groen:

    check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 197 file(s) validate and round-trip, floor 192)
    check-verifier: PASS (197 golden clean; 364 negatief rejected for their declared rule, floor 350)
    check-case-schema: PASS (schema valid; 467 case file(s) validate)
    check-oracle: PASS (ref suite 535 worked examples, floor 489; 441 cases replayed exactly, floor 427; …)
    check-clingo: PASS (backend suite 120 checks, floor 83; 441 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (405 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
    check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)

Gate 9 (catala), 10 (RegelSpraak-frontend) en 11 (pariteitszone) zijn niet
gedraaid: geen bestand in `frontends/`, `toka/` of de catala-keten is
aangeraakt, en gate 12 compileert de frontend-bronboom al mee (die build is
groen).

### Punt 3 van de kaart, beantwoord met een eigen meting

**Ja, de attribuutkant draagt dezelfde fout — maar NIET op de plek die
`jutterswet-vormenkabinet` bezet.**

De 17 attribuutplekken zijn dezelfde aanwezigheidspoort en zijn met de rest
omgezet; de meting hierboven laat ze kantelen op negen goldens. Maar de
scalaire §8.2-vergelijking — de vorm waarover het kabinet `waterstand` met
`attribuutTijdlijnAantal: 1` draagt — poort helemaal niet op het aantal:
`DrlTranspiler.isTemporeelAttribuut` en `ExecModelTranspiler.isTemporeel82`
vragen alleen naar `attribuutGranulariteit`. Gemeten in plaats van aangenomen,
op het kabinet zelf: met en zonder de sleutel is de uitgezonden DRL byte voor
byte identiek en is de exec-model-dekking constructie voor constructie gelijk
(`de_gehele_periode_vergelijking_leest_het_aantal_helemaal_niet`, groen vóór
en ná). Als dat kabinet ooit een case krijgt, loopt het dus niet in deze
poort.

Wél in dezelfde §8.2-sectie: de VERZAMELING-arm
(`isTemporeelSetAttribuut` / `isTemporeelSet82`) leest het aantal wel, en is
met de rest gerepareerd. Geen corpusdocument bereikt die arm vandaag.

### Wat bewust niet bewoog

- **`spec/schema/` en `spec/verifier/`.** Onder lezing 1 is er niets te
  weren: `count: 1` is de gedefinieerde wire-vorm en WF-005 dekt `count < 1`
  al. De acceptatie-eis "het corpus mag de sleutel bij 1 daarna niet stil
  dragen" is ingevuld aan de backendkant: de sleutel bij 1 wordt nu GELEZEN
  (waarde, niet aanwezigheid) en dat lezen is vastgepind door acht checks in
  de suite die gate 12 draait.
- **Geen enkele corpus-`ir.json`.** Ook `synth-gehelejaarkenmerk` niet: dat
  document heeft de vorm die de emitter produceert, en een golden pint
  semantiek, geen backendpoort. De getuige mét de sleutel staat in de suite.
- **`scripts/`** (beschermd) — voorstel hieronder, geen edit.
- **`ref/test/test_ref.ml`** — bestaande test, dus niet aangeraakt; voorstel
  hieronder.
- **De frontend.** `ExpressieBouwer` draagt dezelfde `containsKey`-vorm op
  vijf plekken, maar leest het EIGEN model, en `DefinitionResolver` is daar de
  enige schrijver van beide tabellen en schrijft nooit 1 (`if (aantal != 1)`).
  De vorm is daar dus onbereikbaar bij aantal 1 — geteld, niet aangenomen.
  Buiten "May touch"; niet aangeraakt.

### Eén open vraag voor de maintainer (geen edit gedaan)

De loader leest een JSON-`null` als `0` (`v.asInt()`). Het nieuwe predicaat
leest `0` als ENKELVOUDIG, waar de oude aanwezigheidstoets hem als
meer-periodiek las. Zo'n document is door WF-005 verworpen en bereikt via geen
enkele gate een transpiler, dus het verschil is vandaag onwaarneembaar. De
keuze volgt de kaartformulering ("de WAARDE (> 1)") en legt de weigering bij
de regel die hem bezit. Wie liever heeft dat de backend zelf hard faalt op een
count < 1 — fail-fast in plaats van een stille normalisatie naar enkelvoudig —
kan dat in `Tijdlijnen.meerPeriodiek` in één regel toevoegen; dat is een nieuwe
weigeringstak en daarom niet in deze kaart genomen.

### Voorstellen voor beschermde en vreemde paden (letterlijke tekst)

**(a) `scripts/check-drools.sh`, de staart van het `EXPECTED_PLAYED`-blok
(regels 308-314).** Staat er nu:

    # ONE trap is worth recording at the pin, because it is what made an earlier measurement on this
    # same card report the opposite: the golden's Werf carries NO `kenmerkTijdlijnAantal` entry, and a
    # hand-built document that adds `"bedrijvig": 1` — wire-legal, verifier-clean, semantically
    # identical, and never emitted by the frontend, which writes that key only for a count other than 1
    # — flips BOTH branches to a refusal, because their gate is `containsKey` and not "two or more".
    # A future edit that grows that key silently drops this golden out of the played channel; the
    # over-rejection itself is carded (backlog/new/20260911-kenmerktijdlijnaantal-poort.md).

Voorgesteld:

    # ONE trap WAS worth recording at the pin, and is recorded here as closed: the golden's Werf carries
    # no `kenmerkTijdlijnAantal` entry, and until 2026-09-11 a hand-built document that added
    # `"bedrijvig": 1` — wire-legal, verifier-clean, semantically identical, and never emitted by the
    # frontend, which writes that key only for a count other than 1 — flipped BOTH branches to a refusal,
    # because their gate tested the key's PRESENCE and not "two or more". Card
    # backlog/done/20260911-kenmerktijdlijnaantal-poort.md corrected that gate in both branches (the
    # count defaults to 1 per IR-SPEC ch. 2.8/2.10, so a raster starts at 2) and pinned it with eight
    # checks in the backend's own suite; an explicit 1 no longer moves this or any other golden.

**(b) `ref/test/test_ref.ml`, de alinea bij de ch.-3.9-fixture (rond regel
2677).** Staat er nu:

    The fixture below carries NO `kenmerk_tijdlijn_aantal` entry, and that is
    load-bearing rather than tidy. The emitter writes that key only for a
    count OTHER than 1, so a frontend-produced document never carries it at
    1; a hand-built one that does flips both Drools branches into a refusal
    (their gate is `containsKey`, not "two or more") and makes the form look
    unsupported when it is not. That is precisely the mismeasurement this
    card first reported and then withdrew — see
    `backlog/new/20260911-kenmerktijdlijnaantal-poort.md`.

Voorgesteld:

    The fixture below carries NO `kenmerk_tijdlijn_aantal` entry, which is what
    a frontend-produced document looks like: the emitter writes that key only
    for a count OTHER than 1. Until 2026-09-11 that absence was load-bearing —
    a hand-built document that did carry it at 1 flipped both Drools branches
    into a refusal (their gate tested the key's PRESENCE, not "two or more")
    and made the form look unsupported when it is not. That is precisely the
    mismeasurement the ch. 3.9 card first reported and then withdrew; the gate
    itself was corrected in both branches by
    `backlog/done/20260911-kenmerktijdlijnaantal-poort.md`.

**(c) `spec/IR-SPEC.md` ch. 4.6 en `spec/verifier/lib/checks.ml`** noemen dat
de Drools-backend deze tabellen bereikt "through raw `Map.get` /
`Map.containsKey` at 108 sites … `attribuutTijdlijnAantal` 17,
`kenmerkTijdlijnAantal` 7". **Die telling en het
hoofdlettergevoeligheidsargument blijven waar**: de 24 plekken lezen de
tabellen nog steeds, nu via één gedeeld predicaat dat een rauwe, exacte
`Map.get` doet. Alleen het woord `containsKey` is voor die 24 niet langer
letterlijk. Geen wijziging nodig; wie hem toch wil: "raw `Map.get` /
`Map.containsKey`" → "raw, byte-exact `Map` lookups".

**Register.** Geen `CONSTRUCTS.md`-rij en geen `PLAN.md`-wijziging
voorgesteld: dit is een backendfout tegen een reeds geschreven spec-regel
(ch. 2.8/2.10 + WF-005), geen v1-delta en geen scopewijziging.
