# De ongepoorte rollezers van backends/drools

Goal: elke overgebleven lezer van een §13.3.9 referentieveld in
`backends/drools` meten tegen de naamdeling die
`FactModelGenerator.leden` toelaat (attribuut > kenmerk > referentie), en per
lezer óf de gedeelde poort geven óf met een meting vastleggen dat hij niet
bereikbaar stil fout kan zijn. De klasse is bewezen gevaarlijk: twee lezers uit
deze lijst antwoordden **stil verkeerd** tot ze gepoort werden, en één daarvan
werd pas bij de tweede lens gevonden.

## Waar dit vandaan komt

`FactModelGenerator.leden` draagt één beanveld per identifier, dus een
gedeclareerd attribuut of kenmerk dat de naam van een to-één rol deelt
**verdringt** het referentieveld. Het veld bestaat dan nog steeds — het houdt
alleen een `BigDecimal` of een `boolean`. Elke lezer die alleen de NAAM vraagt
krijgt daardoor een niet-null waarde terug waar de relatie afwezig is.

`spec/IR-SPEC.md` §2.6 legt vast dat zo'n document **welgevormd** is en
beschrijft wat een bean-vormige consument in plaats daarvan doet: de betrokken
constructie per vorm luid weigeren, met het verdrongen referentieveld erbij, in
plaats van het verdringende lid in de plaats te lezen. Geen MUST en geen
WF-nummer — het gedrag wordt gepind door de eigen JUnit-suite van deze backend
en door gate 12, niet door `spec/verifier/`. Gepoort zijn inmiddels, alle via de
gedeelde `FactModelGenerator.heeftReferentieVeld` plus
`geenReferentieveldReden`:

| lezer | sinds |
| --- | --- |
| case-rolinvoer (`CaseRunner.naamdelingsPoort`) | 2026-08-30 |
| objectcreatie-terugkoppeling (`ExecModelTranspiler`, beide einden) | 2026-08-30 |
| aggregatie-join (`ExecModelTranspiler.aggregatie`) | 2026-08-30 |
| genavigeerde lezing: `navReader`, `navKetenReader`, `navComparisonRole`, `navReactiviteit` | 2026-09-10 |
| §8.1.7 rollidmaatschapscheck (beide takken) | 2026-09-10 |
| DRL-tak centraal (`DrlTranspiler.naviResolve`) | 2026-09-10 |

## Waarom dit een kaart is en geen restpunt

De §8.1.7-rollidmaatschapscheck stond niet op de lijst van de kaart die de
navigatie-lezers sloot; hij kwam er pas bij door een tweede lezing. Gemeten
(2026-09-10) op een verifier-schoon en ref-schoon model — `Schuit` met een
attribuut `werf` naast de gelijknamige to-één rol, case zonder rolinvoer, regel
met de voorwaarde "de Schuit een schuit is":

    verifier: PASS
    ref     : oracle: PASS (1 expectations)     — S1.toeslag blijft leeg
    clingo  : clingo: PASS (1 expectations)     — idem
    Drools exec-model : FAIL  verwacht leeg, werkelijk 8
    Drools DRL-tekst  : FAIL  verwacht leeg, werkelijk 8

Die lezer navigeert niet; hij **null-test** het veld (`werf != null`) om te
beslissen of het subject de rol vervult. Het verdringende attribuut is niet-null
zodra het een waarde draagt, dus de check las die waarde als bewijs van
rolvervulling en vuurde een regel die ref en clingo ongevuurd laten. Beide takken
identiek. Dat is precies het patroon dat de rest van deze lijst óók kan dragen:
niet elke lezer navigeert, en de poort hoort dus bij *het veld aanraken*, niet
bij *navigeren*.

## Startlijst (regelnummers op `card/rol-schaduw-definitiefout`, na de poort)

Alle in
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`
tenzij anders vermeld. Geen van deze is gemeten; de nummers zijn een startpunt,
geen bewering.

1. **De far-side rollidmaatschap-join** (~2838, `Conditie.VerreRollidmaatschap`).
   De tegenhanger van de lezer die zojuist gepoort is: hij bindt de filler tegen
   `$subject` via `RegelSpraakRuntime.get(f, backref)`. De backref is een rolnaam
   die net zo goed verdrongen kan zijn.
2. **De aggregatie-bronpatronen buiten de al gepoorte join** (~1197 en ~2608):
   `RegelSpraakRuntime.get(e, backref) == subj`. De join zelf vraagt
   `heeftReferentieVeld`; deze twee bindingen lezen daarna nog eens rechtstreeks,
   en het is ongemeten of ze langs een pad bereikbaar zijn dat de join niet raakt
   (de temporele fold-arm is de verdachte).
3. **De genavigeerde kenmerk-tijdreeks** (~2447,
   `navigatieKenmerkTijdreeks`): `get(s, rol)` en dan `get(rel, kenmerk)` —
   dezelfde twee-staps-lezing als `navReader`, maar op de tijdlijn-arm en met een
   eigen resolutie.
4. **De verdeling** (~5388–5611, `verdeling(...)`): `join` komt uit
   `model.verbindendeRol(doel, bron)` en wordt daarna op zes plekken met
   `RegelSpraakRuntime.get(..., join)` gelezen (~5488, ~5503, ~5552, ~5559, ~5590,
   ~5608).
   Geen enkele vraagt of dat veld de referentie draagt.
5. **De feitcreatie** (~5996, `feitCreatie(...)`): schrijft rolvelden langs een
   pad; de objectcreatie-arm is gepoort, deze niet.
6. **De naam-kopie in `toOneAantalReader`** (~5114): die lezer test de
   naamdeling **zelf**, met `subject.heeftLid(a.attribuut())`, in plaats van de
   gedeelde vraag te stellen. Het antwoord is vandaag toevallig hetzelfde, maar
   het is een tweede afleiding van dezelfde wet — precies wat "één afleiding, één
   vraag" verbiedt, en het drift zodra `leden()` een geval erbij krijgt (het
   zelfpaar, de dimensies). Vervangen door `heeftReferentieVeld` +
   `geenReferentieveldReden`, met behoud van de eigen reden-tekst als die
   scherper is.

## Aanpak

Per lezer, in deze volgorde:

1. **Meten eerst.** Bouw een verifier-schoon, ref-schoon model waarin die lezer
   het verdrongen veld raakt, en draai het door ref, clingo en **beide**
   Drools-takken. Leg de vier uitkomsten vast. Drie afloopmogelijkheden:
   stil fout (zoals §8.1.7), luid maar redenloos (zoals de DRL-compilerfout was),
   of onbereikbaar.
2. **Poorten** waar de meting een stille of redenloze uitkomst laat zien, via
   `heeftVeld(<lezer>, …)` in de exec-tak en `navVeld(<lezer>, …)` in de
   DRL-tak — geen nieuwe afleiding, geen nieuwe redenzin.
3. **Rood eerst** per gepoorte lezer: een assertie in
   `OpslagrichtingWitnessTest` sectie 3, in de vorm van de zeven die er staan,
   met de verwachting eerst door `ref/` gehaald.
4. **Onbereikbaar** is ook een uitkomst — dan één zin in de code die zegt waarom,
   zoals `navReactiviteit` die vandaag draagt, en geen poort.

## Acceptance

- Elk van de zes punten hierboven heeft óf een poort met een rood-eerst-getuige,
  óf een gemeten zin waarom hij geen poort krijgt. Geen enkel punt blijft
  onbesproken.
- Punt 6 (de naam-kopie) is hoe dan ook opgeruimd: die is geen meetkwestie maar
  een tweede afleiding.
- Gate 12 groen met `EXPECTED_PLAYED` **ongewijzigd** — een poort die een golden
  uit het gespeelde kanaal duwt is over-afwijzing en moet met een meting worden
  verantwoord, niet met een kanaalpin.
- Gate 13 groen (beide Drools-takken), gate 11 groen (29/29 TOKA-cases PLAYED —
  het onafhankelijkste over-afwijzingssignaal naast gate 12).
- De JUnit-teller van de backend stijgt; vloer `EXPECTED_MIN_TESTS` ongemoeid.
- `spec/IR-SPEC.md` §2.6's opsomming van gepoorte lezers klopt daarna nog (die
  alinea noemt ze; als de lijst groeit, groeit de zin mee — **voorstel**,
  beschermd pad).

May touch: `backends/drools/**`, `spec/IR-SPEC.md` (**voorstel**), `backlog/`.

## Out of scope

- De welgevormdheidsvraag zelf. `IR-SPEC.md` §2.6 beslist die: het document is
  welgevormd, de weigering is een backend-limiet, en geen WF-regel gaat erover.
- `ref/`, `backends/clingo`, `spec/verifier/**`: die antwoorden gemeten correct
  op elke naamdelingsvorm.
- De rode zone en haar laadfouten: eigen kaart,
  `20260910-rood-zone-laadfouten.md`.
- De **zelfreferentiële** to-één rolmaterialisatie: eigen kaart,
  `20260910-zelfref-rol-materialisatie.md`.

## Result (2026-09-11)

Zeven lezers gemeten, zes gepoort in beide transpilertakken, één met een gemeten
zin afgewezen, en de naam-kopie vervangen door de gedeelde vraag. Elke meting is
gedaan op een zelf gebouwd schaduwdocument, door verifier, `ref/`, clingo en
**beide** Drools-takken, VOOR de poort bestond.

### Per lezer

**1. De far-side rolcheck** (`Conditie.VerreRollidmaatschap`, exec + DRL).
Bereikbaar. De onafgeleide helft is de fout: met beide rollen van het feittype
meervoudig draagt geen van beide kanten een veld.

    verifier: PASS
    ref     : EVALUATION FAULT: condition kind not implemented in this card
    clingo  : TRANSLATION FAULT: condition kind not translated
    exec    : ERROR ConstraintEvaluationException
    DRL     : EXCLUDE drl compile: Unable to Analyse Expression werf == $subject

Nieuw: **ref en clingo dragen deze conditie helemaal niet**. Geen enkel document
met een far-side rolcheck kan dus golden worden, en het differentiële vierkant
kan deze lezer nooit beoordelen. De poort levert hier de diagnose, niet een
waardeverschil. De verdrongen helft is niet bereikbaar met een levende relatie:
`CaseRunner.naamdelingsPoort` weigert de koppeling al bij het schrijven
(gemeten). GEPOORT, getuige `de_verre_rolcheck_weigert_een_terugverwijzing_zonder_veld`.

**2a. De temporele set-sommatie** (`tijdreeksSomAlleSet`, exec + DRL) — de eerste
van de twee aggregatie-bronbindingen. Eigen join, los van de al gepoorte.

    verifier: PASS
    ref     : EVALUATION FAULT: temporal attribute "bewaarlast" as a target:
              temporal writes are not implemented
    clingo  : TRANSLATION FAULT: a temporal target takes only the ch. 3.7 …
    exec    : ERROR ConstraintEvaluationException
    DRL     : EXCLUDE drl compile: Unable to Analyse Expression bewarendeJutter == $subject

Ook hier: ref schrijft geen temporeel doel, dus de vorm kan niet in het corpus
staan. GEPOORT. Daarnaast moest de reden naar de aanroeper: de temporele
schrijfarm drukte "temporele toekenning buiten Phase-F scope" en slikte
`navReden` — nu doorgegeven, zoals de scalaire armen ernaast dat al deden.
Getuige `de_temporele_setsommatie_weigert_een_join_zonder_veld`.

**2b. De §8.2 gehele-periode over een verzameling** (`geheelPeriodeAlle`) — de
tweede bronbinding. **GEEN POORT, gemeten onbereikbaar.** Elke arm komt binnen
op een `set`-lezing (`isTemporeelSet82`), en WF-096 (2026-09-09) laat een
`set`-lezing alleen toe als BRON van een enkelvoudige instantiefold; een
§8.2-operand is een gewone leespositie:

    verifier: WF-096 FAIL /resolvedRegels/0/voorwaarde/links: set reading at an
              ordinary read position
    ref     : EVALUATION FAULT: whole-period comparison is ch. 3 scope

Het document dat een poort zou beschermen is niet welgevormd, dus een poort zou
geen getuige kunnen dragen. Beide takken dragen die zin nu bij de join, met de
regel die moet groeien als WF-096 ooit een set-operand toelaat.

**3. De kenmerk-tijdreeks** (`navigatieKenmerkTijdreeks`, exec + DRL).

    verifier: PASS
    ref     : oracle: PASS (1 expectations)   — toeslag blijft leeg
    clingo  : clingo: PASS (1 expectations)   — idem
    exec    : ERROR ConstraintEvaluationException
    DRL     : EXCLUDE drl compile: Unable to Analyse Expression rsGeheelPeriode((…

Deze lezer HEEFT een scheidsrechter: ref en clingo antwoorden allebei. GEPOORT,
getuige `de_genavigeerde_kenmerktijdreeks_weigert_het_verdrongen_veld`.
Ernaast vastgelegd, en bewust niet opgelost: dezelfde regel ZONDER verdringing
divergeert ook — ref/clingo leeg, beide Drools-takken 8 — omdat §8.2 een lege
relatie als een vacuüm-ware hele periode leest. Dat is een andere vraag dan
welk veld de hop leest en hoort bij wie de §8.2-leegrelatiesemantiek pakt.

**4. De verdeling** (zes leesplekken op één `join`, exec + DRL).

    verifier: PASS
    ref     : oracle: PASS (2 expectations)
    clingo  : clingo: PASS (2 expectations)
    exec    : ERROR ConstraintEvaluationException
    DRL     : EXCLUDE drl compile: Unable to Analyse Expression berging == $src

De bereikbare fout is de onafgeleide helft (ontvangerrol meervoudig, opslag aan
de bronkant). De verdrongen helft is niet bereikbaar met een levende relatie —
de case-invoerpoort weigert de koppeling, en dan vindt de verdeling geen
ontvangers, precies wat ref voor een afwezige relatie rekent (gemeten). Eén
poort dekt beide helften. GEPOORT, getuige
`de_verdeling_weigert_een_ontvangerjoin_zonder_veld`.

**5. De feitcreatie** (drie veldposities, exec + DRL). Elke positie apart
gemeten op een verifier-schoon én ref-schoon model:

    zetVeld verdrongen : ERROR ConsequenceException … ClassCastException:
                         Cannot cast …facts.Kustvak to java.math.BigDecimal
                         (DRL: Unable to Analyse Expression $doel.setSeinvak( $subject ))
    tussenstap zonder veld : ERROR ConstraintEvaluationException
                         (DRL: Unable to Analyse Expression this == $subject.gemeenteVanInschrijving)
    joinVeld verdrongen : de join matcht nooit, de creatie maakt niets — en ref
                         maakt daar ook niets, want elke route die de gelezen
                         relatie zou kunnen leggen is zelf geweigerd.

Alle drie gepoort: het is één veld en één vraag, en de onafgeleide helft is de
crashende. `viaOuderVeld` bleek daarbij de scherpste: die hop leest het veld op
de OUDER, en bij `gemeenteberging` is dat een getter die Gemeente niet heeft.
Getuigen `de_feitcreatie_weigert_de_naamdelende_schrijfrol` en
`de_feitcreatie_weigert_een_tussenstap_zonder_veld`, plus de tegenproef
`de_ongestoorde_feitcreatie_speelt_nog` (drie nieuwe vragen op één constructie
mogen geen schone creatie wegduwen — die speelt in beide takken).

**6. De naam-kopie in `toOneAantalReader`** — vervangen. Het was geen dode
opruiming: de kopie toetste `subject.heeftLid(a.attribuut())`, de GESCHREVEN
naam, VOORDAT de rol werd opgelost, terwijl de lezer het veld van de OPGELOSTE
rol leest. Die twee verschillen in het gewone geval, want het argument van een
AANTAL noemt de elementrol zoals geschreven ("het aantal penningen"), en dat
lost via het meervoud op naar het veld `penning`. Gemeten op een verifier-schoon
én ref-schoon model (penningbezit op 1:1 geklemd, een Numeriek `penning` op
Jutter, consistentie "het aantal penningen van de Jutter is groter of gelijk aan
1", geen koppeling geleverd, het verdringende attribuut op 5):

    verifier: PASS
    ref     : oracle: PASS (1 expectations)   — de consistentie is GESCHONDEN
    clingo  : clingo: PASS (1 expectations)   — idem
    exec    : FAIL verwacht inconsistent (penningtelcontrole), werkelijk consistent

Een WAARDEverschil, geen fout: de backend meldde een geschonden consistentie als
voldaan. De DRL-tak verlaagt deze consistentie helemaal niet ("onopgelost
consistentieregel") — gemeten, en door deze kaart onveranderd. Getuige
`de_aantalassertie_leest_de_opgeloste_rol_niet_de_geschreven_naam`.

### Rood-groen

Alle acht getuigen eerst rood gedraaid met de drie gewijzigde bronbestanden
gestasht: **7 failures, 1 pass**. De ene die groen bleef is de tegenproef, wat
klopt. Elke failure was de gemeten fout zelf — `ConsequenceException` met
`ClassCastException` voor de schrijfrol, `ConstraintEvaluationException` voor de
vier joins/hops, en "Expected … Excluded to be thrown, but nothing was thrown"
voor de AANTAL-drift, die immers stil verkeerd antwoordde in plaats van te
falen. Daarna groen: 8/8.

### Kanaalcontrole en gates

- gate 1: PASS.
- gate 11: PASS — 29/29 TOKA-cases PLAYED, 0 per-vorm EXCLUDE. Het
  onafhankelijke over-afwijzingssignaal beweegt niet.
- gate 12: PASS — **played 122, gelijk aan de gepinde `EXPECTED_PLAYED`**; 44
  auto-excluded, 1 waardedivergentie, 2 no-case; JUnit 303 tests, vloer 239. Geen
  golden verschuift van kanaal; `scripts/check-drools.sh` is niet aangeraakt.
- gate 13: PASS — 74 getwiste annotaties over 15 goldens (vloer 74), 70 replays,
  13 daarvan PLAYED door de DRL-tekst-tak (vloer 13), 0 survivors. Ongewijzigd.

Geen enkele poort verschoof een kanaal, dus de "STOP en leg vast"-clausule van
de opdracht is nergens ingegaan.

### Wat bewust niet bewoog

- `corpus/golden`, `scripts/`, `spec/verifier/`, `spec/CONSTRUCTS.md`,
  `spec/PLAN.md`: niet aangeraakt.
- De §8.2-leegrelatiesemantiek (zie lezer 3): een eigen vraag.
- `DrlTranspiler`'s eigen aggregatie-bereik (`accumulateScope`, de by-name/by-pair
  join achter `Frag.todo("aggregatie-bereik", …)`) stelt de gedeelde vraag NIET,
  terwijl de exec-tegenhanger dat sinds 2026-08-30 wel doet. Gemeten:
  `anchor/jutterswet-erflijn` door de DRL-tak geeft `EXCLUDE drl compile: Unable
  to Analyse Expression erflater == $subject` waar de exec-tak de §13.3.9-zin
  drukt. Die lezer stond op geen enkele lijst — ook niet op deze kaart — en is
  een eigen, kleine kaart waard.

### Voorstellen op beschermde paden

1. `spec/IR-SPEC.md` §2.6, de alinea die de gepoorte lezers opsomt (rond r. 1453).
   De laatste twee zinnen ("That list is an audit trail… carded in
   `backlog/new/20260910-drools-ongepoorte-rollezers.md`") verouderen met deze
   kaart. Voorgestelde vervanging van de opsomming en de slotzin:

   > The Drools backend does exactly that in both of its transpiler branches, at
   > each of the readers audited so far: the case's role input, the object
   > creation's back-link and the aggregation join (2026-08-30); the navigated
   > single-hop, chain, comparison and reactivity reads plus the ch. 8.1.7
   > role-membership check — which does not navigate but null-tests the same
   > field — since 2026-09-10; and since 2026-09-11 the ch. 8.1.7 FAR-side
   > membership join, the temporal set summation's own join, the navigated
   > characteristic timeline, the allocation's recipient join, the fact
   > creation's three field positions, and the presence count of a to-one role.
   > One reader was audited and deliberately left ungated: the whole-period fold
   > over a navigated SET, which WF-096 makes unreachable through a well-formed
   > document. Those refusals are pinned by that backend's own test suite and by
   > gate 12, not by `spec/verifier/` — a verifier that enforced them would be
   > enforcing one target format's shape on every consumer. The audit of that
   > field's readers is complete for the v0 backend.

2. `spec/CONSTRUCTS.md`, de rij-4-zin van de rol-schaduw-kaart ("ongepoort en
   ongemeten blijven…"): die is nu onwaar. Elke lezer van het referentieveld in
   `backends/drools` is gemeten; zes dragen de gedeelde poort, één draagt een
   gemeten zin (WF-096 maakt hem onbereikbaar). Voorgestelde strekking: "sinds
   2026-09-11 is er geen ongemeten lezer meer; de enige ongepoorte draagt zijn
   reden in beide takken".

Eén gedeelde zin is wel veranderd, in `FactModelGenerator.geenReferentieveldReden`:
de tweede oorzaak zei "de **enkelvoudige** rol", wat alleen klopte zolang elke
aanroeper eerst `naarVeelRol` toetste. De nu gepoorte lezers doen dat niet, en
een meervoudige rol is precies de vorm die zij tegenkomen — de zin zou van het
document beweren wat er juist niet waar aan is. Het woord is geschrapt; de zin
zelf is dezelfde gedeelde zin gebleven.
