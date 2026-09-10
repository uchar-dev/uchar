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

`spec/IR-SPEC.md` §2.6 legt vast dat zo'n document **welgevormd** is en dat een
bean-vormige consument hem per constructie luid moet weigeren. Gepoort zijn
inmiddels, alle via de gedeelde `FactModelGenerator.heeftReferentieVeld` plus
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
4. **De verdeling** (~5388–5603, `verdeling(...)`): `join` komt uit
   `model.verbindendeRol(doel, bron)` en wordt daarna op zes plekken met
   `RegelSpraakRuntime.get(..., join)` gelezen (~5488, ~5503, ~5552, ~5559 …).
   Geen enkele vraagt of dat veld de referentie draagt.
5. **De feitcreatie** (~5996, `feitCreatie(...)`): schrijft rolvelden langs een
   pad; de objectcreatie-arm is gepoort, deze niet.
6. **De naam-kopie in `toOneAantalReader`** (~5109–5111): die lezer test de
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
   `OpslagrichtingWitnessTest` sectie 3, in de vorm van de zes die er staan,
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
