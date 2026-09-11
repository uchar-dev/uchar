# Twee executors spreken ch. 3.9 stil tegen (PRIORITEITSSIGNAAL)

Goal: uitzoeken en sluiten waarom `ref/` en `backends/clingo` een clausule die
`spec/RUNTIME-SEMANTICS.md` §3.9 **al geschreven heeft** niet uitvoeren, en in
**drie** van de vier gemeten gevallen stil het tegenovergestelde antwoorden
(`ref` stil in geval a, clingo stil in a, b en c). Dit is uitdrukkelijk **niet**
de vraag "welke lezing kiezen we". De norm staat er; de vraag is waarom de
referentie-implementatie en de ASP-backend hem niet dragen, en wat daarvan in
v0-scope hoort.

## De geschreven norm

`spec/RUNTIME-SEMANTICS.md` §3.9 (r. 1546-1553), letterlijk:

> - A timeline with **no segments at all touches no bucket and passes
>   vacuously** (the qualifier's negation then fails). […]
> - Over a to-many navigation the fold is conjunctive — every element's
>   timeline must pass — and an **empty set passes vacuously**; a failed
>   to-one navigation contributes a null timeline, which passes vacuously
>   (the ch. 1.2 leeg-navigation discipline, lifted).

Twee gevallen zijn dus met zoveel woorden beslist: een MISLUKTE to-één navigatie
en een tijdlijn ZONDER segmenten laten de gehele-periode-toets **slagen**.

## De meting (2026-09-11)

Handgebouwd, verifier-schoon model: `synth-navigatiepremie` met een temporeel
kenmerk `bedrijvig` (MAAND) op Werf, de `toeslag`-regel teruggebracht tot

    De toeslag van een Schuit is de lengte van de Schuit
      indien de werf van de Schuit gedurende het gehele jaar bedrijvig is

`asOfDate` 2026-03-15, S1.lengte 8. Vier cases; de kolom "ch. 3.9" is wat de
geschreven clausule voorschrijft.

| case | ch. 3.9 | ref | clingo | Drools (beide takken) |
|---|---|---|---|---|
| a. geen koppeling geschreven | 8 | **leeg — STIL** | **leeg — STIL** | 8 |
| b. koppeling, geen kenmerktijdlijn | 8 | FAULT ch. 3.3 (luid) | **leeg — STIL** | 8 |
| c. koppeling, tijdlijn heel 2026 | 8 | FAULT ch. 3.3 (luid) | **leeg — STIL** | 8 |
| d. koppeling, tijdlijn jan–jul 2026 | leeg | FAULT ch. 3.3 (luid) | leeg (PASS) | leeg |

De letterlijke uitkomsten:

    a. ref   : MISMATCH: S1.toeslag = leeg, expected 8
       clingo: MISMATCH: S1.toeslag is underived (leeg), expected 800
    b/c/d ref: EVALUATION FAULT: temporal characteristic "bedrijvig" in a scalar
               condition is refused (ch. 3.3)
    b/c clingo: MISMATCH: S1.toeslag is underived (leeg), expected 800
    d  clingo : clingo: PASS (1 expectations)

(clingo's "800" is de interne geschaalde weergave van 8 bij een doel met 2
decimalen; de uitspraak die telt is *underived*.)

**De gevaarlijke cellen zijn de stille.** Een luide weigering (ref in b/c/d) is
een gat dat zichzelf meldt: ref zegt dat een temporeel kenmerk in een scalaire
conditie geweigerd wordt (ch. 3.3) en houdt op. De stille cellen — ref in a,
clingo in a/b/c — leveren een ANTWOORD af dat de geschreven clausule tegenspreekt,
zonder dat er iets misgaat om te zien. Drools is in alle vier de rijen in
overeenstemming met §3.9.

## Status van de tabel: vast

De tabel is **niet** meer voorlopig. Hij is drie keer onafhankelijk gebouwd en
gemeten (2026-09-11) en de drie uitkomsten stemmen overeen. De ene cel waarover
de eerste en de tweede bouw uiteenliepen — `ref` in geval d — is door de derde
beslecht: **`ref` geeft daar de luide ch. 3.3-weigering, niet "leeg"**. De
oorspronkelijk gemelde lezing "alle leeg" voor rij d is daarmee weerlegd en
staat hier alleen nog als aantekening, zodat niemand hem opnieuw invoert.

Wat wél een voorbehoud blijft:

- Het document is **handgebouwd**. Geen enkele golden draagt deze vorm: een
  §8.2-conditie over een GENAVIGEERD temporeel kenmerk komt in `corpus/golden`
  nul keer voor (gemeten — het enige §8.2-golden, `jutterswet-vormenkabinet`,
  toetst een EIGEN attribuut en heeft geen feittypen). Of die vorm corpus-
  materiaal HOORT te worden hangt af van taak 1 hieronder.

## Taken

Herafleiden is gedaan; wat overblijft begint bij de weging.

1. **Wegen of de vorm in v0-scope valt.** ref weigert een temporeel kenmerk in
   een scalaire conditie op grond van ch. 3.3. §3.9 beschrijft juist de
   gehele-periode-toets over zo'n kenmerk. Ofwel ch. 3.3's weigering is te breed
   (ze mag de §8.2-gekwalificeerde lezing niet raken), ofwel §3.9's zin over de
   mislukte to-één navigatie beschrijft een vorm die v0 niet aanbiedt. Dat is
   een spec-vraag en hoort vóór elke implementatie beslecht.
2. Pas daarna: de stille cellen dicht — hetzij door de clausule uit te voeren,
   hetzij door hem net zo luid te weigeren als ref dat in b/c/d doet. Een stil
   afwijkend antwoord is de enige uitkomst die niet mag blijven staan.
3. Randen die nog niet gemeten zijn en die de weging kunnen kantelen: beide
   richtingen van de negatie (`GeheelPeriode.niet`) en `gedurende de gehele
   maand` naast `het gehele jaar`. Die uitbreiding hoort bij taak 1, niet ervoor.

## Acceptance

- Een uitspraak in `spec/RUNTIME-SEMANTICS.md` (**voorstel**, beschermd pad)
  die de §3.9-clausule en ch. 3.3's weigering met elkaar in overeenstemming
  brengt, met een worked example per cel.
- Geen stille cel meer: elke executor antwoordt de norm, of weigert luid.
- De vier cellen als corpus-materiaal of als getuige vastgelegd — welke van de
  twee hangt af van taak 2's uitkomst.
- Gates 2-8, 12 en 13 groen; `EXPECTED_PLAYED` alleen bewogen met een meting
  erbij.

May touch: `ref/`, `backends/clingo`, `spec/RUNTIME-SEMANTICS.md`
(**voorstel**), `corpus/` (na taak 2), `backlog/`.

## Out of scope

- Welk beanveld een genavigeerde hop leest: dat is
  `20260910-drools-ongepoorte-rollezers.md`, gesloten 2026-09-11. Deze kaart
  gaat over de vorm die daar ZONDER verdringing overbleef.
- De §8.2-fold over een NAVIGEERDE VERZAMELING: WF-096 maakt dat document
  onwelgevormd (gemeten, zelfde kaart).

---

## Result (2026-09-11)

**De weging: ch. 3.3's weigering is te breed — horn 1 van taak 1.** De norm
staat driemaal geschreven en noemt de §8.2-kwalificator elke keer als
UITZONDERING op de scalaire weigering, niet als iets dat eronder valt:

- ch. 3.3: *"a time-dependent attribute or parameter in a scalar context is
  refused … **The only ways a timeline becomes a scalar are the dedicated
  collapses: the temporal total (ch. 3.6) and the whole-period predicate
  (ch. 3.9).**"*
- ch. 8.2: *"**It indexes only through the read.** … the only bridges from a
  timeline to a scalar are the dedicated collapses (ch. 3.6, 3.9) and the
  explicit point read."*
- ch. 3.11: *"a **temporal value in a scalar slot** is refused **except
  through the specified collapses** (ch. 3.3)."*

En ch. 3.9 noemt de toets bij naam: *"A value test (ordering, equality,
filled, **characteristic-holds**) demands satisfaction on every day of each
touched bucket."* Een conditie die een `GeheelPeriode` draagt is dus géén
scalaire conditie — hij IS de tweede collapse. `ref`'s luide FAULT in b/c/d
is daarom **niet** correct-geweigerd gedrag: de tekst van de fault ("in a
scalar condition") beschrijft de conditie verkeerd, en hij vuurde alleen
doordat beide `Kenmerk_check`-armen `periode` met `_` wegmatchten. Om
dezelfde reden hoort clingo de clausule **uit te voeren**, niet luid te
weigeren: niets in de spec maakt de collapse per uitvoerder optioneel.

**Hermeting op deze HEAD (63174ad), vóór elke wijziging.** De kaarttabel
reproduceert exact voor `ref` en clingo — en NIET voor Drools:

| cel | ch. 3.9 | ref vóór | clingo vóór | Drools vóór (beide takken) |
|---|---|---|---|---|
| a. geen koppeling | 8 | leeg — STIL | underived/leeg — STIL | EXCLUDE (luid) |
| b. koppeling, geen tijdlijn | 8 | FAULT ch. 3.3 | underived/leeg — STIL | EXCLUDE (luid) |
| c. koppeling, heel 2026 | 8 | FAULT ch. 3.3 | underived/leeg — STIL | EXCLUDE (luid) |
| d. koppeling, jan–jul | leeg | FAULT ch. 3.3 | PASS (leeg) | EXCLUDE (luid) |

De Drools-kolom van de kaart ("8/8/8/leeg") is op deze HEAD **niet
gereproduceerd**. Beide takken weigeren de vorm bij naam: de exec-model-tak
met `transpiler SKIP … (voorwaarde buiten Phase-B scope)`, de DRL-tak met
`drl-transpiler MARKER … TODO §8.2 gehele-periode`. Dat strookt met de
meting van `20260906-gedurende-kwalificatie-stiltes`: Drools draagt §8.2 op
de **Vergelijking**, niet op het kenmerk. De vorm heeft dus vandaag geen
derde uitvoerder — luid, niet stil.

**Ná de conformering** (ref en clingo, zelfde document, zelfde cases):

| cel | ref ná | clingo ná | eens met ch. 3.9 |
|---|---|---|---|
| a | 8 | 8 | ja (null tijdlijn, geen bucket geraakt) |
| b | 8 | 8 | ja (geen segmenten, geen bucket geraakt) |
| c | 8 | 8 | ja (bucket 2026: 365 van 365) |
| d | leeg | leeg | ja (bucket 2026: 181 van 365) |

Taak 3's randen zijn meegenomen en gemeten: **48 metingen** over beide
grids (jaar/maand), beide richtingen van `GeheelPeriode.niet`, beide
richtingen van de binnenste `negatie`, vijf tijdlijnvormen en beide
koppelingstoestanden — `ref ≡ clingo` op alle 48.

**Wat er is gebouwd.**

- `ref/lib/timeline.ml`: `geheel_periode_kenmerk` — de ch. 3.9-vouw over de
  waar-periodes, hergebruikt de `aantal_dagen_in`-vouw zodat "touched"
  hetzelfde betekent als in ch. 3.8.
- `ref/lib/eval.ml`: `whole_period_kenmerk`, en beide `Kenmerk_check`-armen
  splitsen nu op `periode`. De ch. 3.3-weigering blijft ongewijzigd staan
  waar de kwalificator ONTBREEKT. De ontbrekende koppeling levert onder de
  kwalificator een vacuüm-ware uitkomst — bewust afwijkend van de
  ongekwalificeerde v0-asymmetrie.
- `backends/clingo/lib/translate.ml`: een eigen ASP-codering (per geraakte
  bucket een FALEN uit de `ktl`-feiten, via de kalender die de pro-rata al
  legt; de vouw wordt NIET uit `ref` aangeroepen — deze clausule is juist wat
  het differentieel moet vergelijken). `emit_bucket_calendar` en
  `emit_true_days` zijn ongewijzigd omhoog verplaatst omdat de
  conditie-recursie eerder in het bestand staat.
- **De overige stille cellen in dezelfde clausule zijn dicht.** `ref`'s
  `Predicaat` met een `periode` negeerde de kwalificator stil; clingo deed
  dat voor `Vergelijking` én `Predicaat` (ze vielen in de catch-all, luid maar
  zonder de clausule te noemen). Beide weigeren nu bij naam, in dezelfde
  woorden als de andere uitvoerder.
- **De poort die WF-043 niet bereikt.** WF-043 kruist tijdsafhankelijkheid
  alleen op de NIET-genavigeerde check (hij sleutelt op `eigenaar`/onderwerp,
  WF-100). Op de genavigeerde vorm woont het kenmerk op het bereikte type;
  zonder poort zou een niet-temporeel kenmerk over een altijd-lege
  periodeverzameling vacuüm slagen. Beide uitvoerders houden die poort nu
  zelf.

**Pins.** `ref/test/test_ref.ml`: 19 checks `WV-3.9-kenmerk-*`/`WV-3.9-fold-*`
(suite 515 → 534 gemeten, vloer 489 ongemoeid). `backends/clingo/test/test_clingo.ml`:
23 checks, waarvan 11 rijen die ELK door `ref_matches` én `clingo_matches`
gaan — dat maakt ze het differentieel en niet twee losse overtuigingen
(suite 96 → 119, vloer 83 ongemoeid).

**Geen corpus-golden, met de meting erbij.** De kaart vroeg een getuige "die
alle drie spelen". Gemeten: Drools speelt de vorm niet (zie boven), dus het
vierkant sluit niet en een golden zou een NIET-gespeelde toevoeging zijn die
een `EXPECTED_CHANNEL`-regel in `scripts/check-drools.sh` (beschermd pad)
nodig heeft — een regel die meteen muf wordt zodra de Drools-transpilers de
vorm wél dragen. `EXPECTED_PLAYED` is daarom **niet** bewogen (123 blijft
123). De vier cellen liggen als getuige vast in de twee suites, wat de kaart
als tweede optie noemt.

**Voorstel voor wanneer Drools de vorm draagt** (één kaart, niet deze):
`corpus/golden/synth-gehelejaarkenmerk` met het document hierboven en vier
cases (a/b/c/d, `asOfDate` 2026-03-15, S1.lengte 8, verwacht 8/8/8/leeg),
plus `EXPECTED_PLAYED` 123 → 124.

**Wat bewust niet bewoog.**

- `backends/drools/**` — parallel spoor, niet aangeraakt.
- `scripts/**` — geen vloer of lijst bewogen; alle groeiende tellingen
  passeren hun vloer ruim.
- De ch. 3.9-toetsen **ordering/equality** en **leeg/gevuld**: gespecificeerd,
  niet uitgevoerd. Ze weigeren nu in beide uitvoerders bij naam. Geen
  corpuscase draagt ze en de enige golden met een `GeheelPeriode` op een
  `Vergelijking` (`jutterswet-vormenkabinet`) heeft bewust geen case.
- WF-043 verbreden naar het bereikte type: opgeschreven als kandidaat in het
  IR-SPEC-register, niet besloten.

**Voorstelzin voor `spec/CONSTRUCTS.md`** (hoofdsessie, serieel — de rij die
§8.2 / kenmerk-tijdlijnen draagt): *"De §8.2-gehele-periode-kwalificator op
een kenmerkcheck is sinds 2026-09-11 uitgevoerd in `ref` en `backends/clingo`
(eigen en genavigeerd, beide grids, beide negaties), met de drie
vacuüm-passen van ch. 3.9; ch. 3.3's scalaire weigering is expliciet begrensd
tot de vorm ZONDER kwalificator. De toetsen ordering/equality en leeg/gevuld
blijven gespecificeerd-maar-niet-uitgevoerd, nu met een weigering die de
clausule noemt; de Drools-backend draagt §8.2 alleen op de Vergelijking, dus
de kenmerkvorm heeft nog geen derde uitvoerder."*
