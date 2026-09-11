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

### De cellen, vóór en ná

| cel | ch. 3.9 | ref vóór | clingo vóór | Drools vóór | ref ná | clingo ná | Drools ná |
|---|---|---|---|---|---|---|---|
| a. geen koppeling | 8 | leeg — STIL | leeg — STIL | **8, beide takken** | 8 | 8 | **8** |
| b. koppeling, geen tijdlijn | 8 | FAULT ch. 3.3 | leeg — STIL | **8, beide takken** | 8 | 8 | **8** |
| c. koppeling, heel 2026 | 8 | FAULT ch. 3.3 | leeg — STIL | **8, beide takken** | 8 | 8 | **8** |
| d. koppeling, jan–jul | leeg | FAULT ch. 3.3 | PASS (leeg) | **leeg, beide takken** | leeg | leeg | **leeg** |

De Drools-kolom staat zoals de kaart hem schreef: **PLAYED op alle vier de
cellen, in beide takken.** De kwalificator-negatie speelt 4/4 en het
maandraster 2/2. Wat wél bij naam weigert is (i) de **binnenste** negatie
("gedurende het gehele jaar NIET bedrijvig") en (ii) het **verdrongen veld**
(een lid dat de rolnaam verdringt). Binnen de VERGELIJKINGSvorm speelt de
eigen operand en markert de genavigeerde. De vormsplitsing loopt dus **niet**
langs Vergelijking-versus-kenmerk.

**Correctie op een eerdere versie van deze Result (ingetrokken).** Die meldde
dat beide Drools-takken alle vier de cellen luid weigeren en concludeerde
"geen derde uitvoerder". Dat was een **meetfout, geen gedragsverschil**: de
handgebouwde fixture droeg `"kenmerkTijdlijnAantal": {"bedrijvig": 1}`. De
emitter schrijft die sleutel alleen bij een andere waarde dan 1
(`DefinitionResolver`), dus geen enkel frontend-document draagt hem bij 1;
beide takken poorten op `containsKey` en niet op "twee of meer"
(`navigatieKenmerkTijdreeks`, identiek in `DrlTranspiler` en
`ExecModelTranspiler`), zodat één wire-legale, verifier-schone, semantisch
identieke sleutel beide takken omslaat naar precies de twee weigeringen die
geciteerd werden. Op het wire-echte formaat spelen ze alles. De
"geen derde uitvoerder"-conclusie **vervalt**; de vormsplitsing is de
`kenmerkTijdlijnAantal`-poort. Zelf nagemeten op deze HEAD, beide takken,
vóór en ná het verwijderen van die ene sleutel; de over-afwijzing zelf is
gekaart als `backlog/new/20260911-kenmerktijdlijnaantal-poort.md`, met het
meetrecept en de vindplaatsen.

Taak 3's randen zijn meegenomen: **48 metingen** in ref en clingo over beide
grids, beide richtingen van `GeheelPeriode.niet`, beide richtingen van de
binnenste `negatie`, vijf tijdlijnvormen en beide koppelingstoestanden —
`ref ≡ clingo` op alle 48 — plus **12 metingen** in beide Drools-takken over
dezelfde randen.

### Wat er is gebouwd

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
- `emit_bucket_calendar`'s **lege geval** veranderde van `()` naar
  `#defined <p>_b/3.` — geen atoom erbij, geen semantiek erbij, wel het einde
  van een info-regel op stderr die als defect leest. Zonder tijdlijn in de
  case is er geen kalender, terwijl de consumenten het bucketpredicaat wel
  noemen; dat is precies de situatie die cellen a en b maken.

### De getuige

`corpus/golden/synth-gehelejaarkenmerk` — handgebouwd, synth-stijl, geen
`bron.rgs`. Twee regels, dezelfde kwalificator in beide richtingen:

```
De toeslag van een Schuit is de lengte van de Schuit
  indien de werf van de Schuit gedurende het gehele jaar bedrijvig is
De korting van een Schuit is de lengte van de Schuit
  indien de werf van de Schuit niet gedurende het gehele jaar bedrijvig is
```

Handberekening — rekendatum 2026-03-15, `S1.lengte` 8, jaarbucket
[2026-01-01, 2027-01-01) = **365 dagen** (2026 is geen schrikkeljaar):

| case | koppeling | waar-periodes | geraakte bucket | voldaan / nodig | toeslag | korting |
|---|---|---|---|---|---|---|
| case1-geen-koppeling | geen | — | geen (null tijdlijn) | vacuüm | **8** | leeg |
| case2-geen-tijdlijn | W1 | geen | geen (geen segmenten) | vacuüm | **8** | leeg |
| case3-heel-jaar | W1 | [2026-01-01, 2027-01-01) | 2026 | 365 / 365 | **8** | leeg |
| case4-half-jaar | W1 | [2026-01-01, 2026-07-01) | 2026 | 181 / 365 | leeg | **8** |

181 = 31 + 28 + 31 + 30 + 31 + 30 (januari t/m juni 2026). `{"kind":
"consistent"}` sluit elke verwachtingslijst af. De tweede regel is er niet
voor de sier: zonder hem pint case4 alléén een `attributeEmpty`, wat de
`--noop`-uitvoerder overleeft (gate 7) en een overlever-regel in een
beschermd script zou vragen. Nu beslist elke case precies één afgeleide
waarde en weerlegt alle vier de niets-doen-uitvoerder.

Gemeten: `ref` PASS 4, clingo PASS 4, Drools **PLAYED 4 in beide takken** —
het vierkant sluit op deze vier waarden. Kanaalgevolg gebouwd:
`EXPECTED_PLAYED` 123 → 124 in `scripts/check-drools.sh`, met de reden en
met de `kenmerkTijdlijnAantal`-val bij de pin opgeschreven (beschermd pad,
dus **voorstel**).

Het `ir.json` draagt de spurieuze sleutel **niet**; de NOTE zegt waarom dat
load-bearing is en niet netjes.

### Pins

`ref/test/test_ref.ml`: 19 checks `WV-3.9-kenmerk-*`/`WV-3.9-fold-*` (suite
515 → 534 gemeten, vloer 489 ongemoeid). `backends/clingo/test/test_clingo.ml`:
23 checks, waarvan 11 rijen die ELK door `ref_matches` én `clingo_matches`
gaan (suite 96 → 119, vloer 83 ongemoeid). Beide fixtures dragen de
spurieuze sleutel niet meer en beide suites zijn erna hermeten: 534 en 119,
onveranderd groen. De binnenste negatie blijft suite-werk en gaat niet het
corpus in: Drools weigert die bij naam, dus een golden zou het vierkant niet
sluiten.

### Registerwerk voor de hoofdsessie (serieel, beschermde paden)

- **`spec/CONSTRUCTS.md` rij 29 / de §8.2-rij** — voorstelzin: *"De
  §8.2-gehele-periode-kwalificator op een kenmerkcheck is sinds 2026-09-11
  uitgevoerd in `ref` en `backends/clingo` (eigen en genavigeerd, beide
  grids, de kwalificator-negatie), met de drie vacuüm-passen van ch. 3.9;
  `backends/drools` droeg de vorm al in beide takken, zodat
  `corpus/golden/synth-gehelejaarkenmerk` het vierkant sluit op vier
  handberekende cellen (8/8/8/leeg). ch. 3.3's scalaire weigering is
  expliciet begrensd tot de vorm ZONDER kwalificator. De binnenste negatie
  ("gedurende … NIET") draait in de twee OCaml-uitvoerders en wordt door
  beide Drools-takken bij naam geweigerd; de toetsen ordering/equality en
  leeg/gevuld blijven gespecificeerd-maar-door-geen-OCaml-uitvoerder-
  uitgevoerd, nu met een weigering die de clausule noemt."*
- **`spec/CONSTRUCTS.md` rij 4 draagt een dode verwijzing**: de slotzin noemt
  het prioriteitssignaal als `backlog/new/20260911-leegrelatie-ch39-
  divergentie.md`; dat pad wordt `backlog/done/…`. Meenemen in dezelfde
  seriële ronde.
- **Verbreding van de May touch**: `spec/IR-SPEC.md` stond niet in de
  "May touch" van deze kaart en is toch aangeraakt (WF-043 krijgt de
  opgetekende scope-beperking tot de niet-genavigeerde vorm). Hier
  geregistreerd zoals de huislijn wil, als voorstel op een beschermd pad —
  niet stil meegenomen. Idem `scripts/check-drools.sh` (de
  `EXPECTED_PLAYED`-bump), dat evenmin in de lijst stond.

### Wat bewust niet bewoog

- `backends/drools/**` — parallel spoor, geen regel aangeraakt. De
  `containsKey`-poort is gekaart, niet gerepareerd.
- De ch. 3.9-toetsen **ordering/equality** en **leeg/gevuld**: gespecificeerd,
  door geen OCaml-uitvoerder uitgevoerd. Ze weigeren nu in beide bij naam.
  Geen corpuscase draagt ze en de enige golden met een `GeheelPeriode` op een
  `Vergelijking` (`jutterswet-vormenkabinet`) heeft bewust geen case.
- WF-043 verbreden naar het bereikte type: opgeschreven als kandidaat in het
  IR-SPEC-register, niet besloten.
