# timeline-arithmetic-by-rule: ch. 3.4 en 3.5 via een regel, over alle tijd (impliciete tijd, fase 1a en 2a)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` (fase 0) en het
voorstel `spec/proposals/IMPLICIT-TIME.md`, punt 2 en §5. Besluit van de eigenaar 2026-11-03 (normvraag 2): **alle tijd**. Een
tijdlijn deelt de hele tijd in, leeg buiten haar perioden; een scalair geldt altijd (RegelSpraak §5.1.1, §5.1.4.1; ALEF
`ITimed`, `Always`).*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.4 en 3.5 specificeren plus, min en de som per periode over tijdlijnen, maar geen uitvoerder schrijft
ze via een regel: ref weigert een tijdsafhankelijk attribuutdoel buiten pro rata en dagentelling, clingo met CLINGO-024, Drools
met DROOLS-105 (IMPLICIT-TIME §5). Het domein van ch. 3.4 is nu de span van de operanden; de eigenaar koos alle tijd, en dat
vraagt open segmenten.

## Stap A — spec, ref, clingo, frontend

Goal: een regel met een tijdsafhankelijk attribuutdoel waarvan de waarde plus, min of de som per periode van ch. 3.4 en 3.5 is,
draait in ref en clingo over alle tijd, gepind door een golden met absolute verwachtingen.

Acceptance:
- (a) Rood eerst: golden `jutterswet-<naam>` (eigen casus, met `source.rgs`) met de uitgewerkte voorbeelden van ch. 3.4 en
  3.5 via een regel, en een scalair naast een tijdlijn (`WV-3.4-domein`: `5 plus a` geeft 5 vóór, 105 binnen, 5 na); op main
  weigeren ref en clingo met de zinnen van IMPLICIT-TIME §5.
- (b) RS ch. 3.1 (open segmenten: een segment mag een open begin of einde hebben; een gat is een leeg segment), 3.4, 3.5 en
  3.12 (voorstel) zeggen alle tijd, met de voorbeelden van ch. 3.4 buiten de span herrekend. VB 2.1 en 2.2 van het voorstel:
  het case-schema `uchar-case-v0` laat `from` of `to` weg voor een open grens (beschermd: voorstel), de laders van ref en
  clingo lezen dat, een verwachting vergelijkt de hele opdeling; de ASP-codering van een open grens (`spec/ASP-TRANSLATION.md`,
  voorstel). VB 2.4: een totaal (ch. 3.6) over een open segment met waarde 0 telt 0, met een andere waarde is het een fout met
  een nieuwe EF-code (ch. 5.7b, rood in beide uitvoerders).
- (c) ref en clingo spelen de golden; gate 10 emitteert haar byte-gelijk; bestaande goldens en hun verwachtingen veranderen niet
  (meet het en noem elk verschil).
- (d) De golden staat met naam in de kanaallijst van `scripts/check-drools.sh` (voorstel) tot stap B.
- (e) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `corpus/golden/<nieuw>/`, `corpus/red/<nieuw>/`, `spec/RUNTIME-SEMANTICS.md`
(voorstel), `spec/schema/uchar-case-v0.schema.json` (voorstel), `spec/ASP-TRANSLATION.md`, `scripts/check-drools.sh` en
`scripts/check-red.sh` (voorstel), `frontends/regelspraak/**` (alleen een gebrek dat de golden blootlegt), kaartverplaatsing.

Out of scope: andere operatoren (`20261103-timeline-arithmetic-per-period`), voorwaarden per periode
(`20261103-conditions-per-period`).

## Stap B — Drools (direct na de landing van stap A)

Goal: de lowering met truth maintenance (`transpiler/tms`, één analyse in exec-model en DRL-tekst) draagt de vormen van stap A
als zuivere functies over segmentlijsten in de runtime (precedent `Allocation.compute`), open segmenten inbegrepen.

Acceptance: (a) rood eerst: de golden van stap A geeft op main DROOLS-105; (b) `PLAYED` op beide branches, `EXPECTED_PLAYED`
omhoog en de kanaalpin weg (voorstel), elke code die geen plek meer noemt `RETIRED`; (c) de lezingen gelijk aan de trace van
ref (gate 17), een getuige per nieuwe vorm in elke regelvolgorde; (d) de case-lezer van Drools leest een open grens; (e)
`make gates` groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (voorstel), kaartverplaatsing.

## Omvang

Middel (stap A), klein tot middel (stap B).

## Result: stap A (bouwer, 2026-11-03)

Branch `card/timeline-arithmetic-by-rule`, gerebased op main `f6ad326c5` (de tweede ronde van
`ref-gehele-periode-lege-grens`; zonder conflict). Niet gemerged, niet gepusht. Beschermde paden in deze branch, dus
**voorstel**: `spec/RUNTIME-SEMANTICS.md`, `spec/schema/uchar-case-v0.schema.json`, `scripts/check-drools.sh`,
`scripts/check-red.sh`.

### Commits

- `1571d7cb5` kaart naar `doing/`
- `5e24d3da9` ref: plus, min en de som per periode over alle tijd
- `787e3eba7` clingo: rekenen per periode over alle tijd, open grenzen als `#inf`/`#sup`
- `bde7b54a9` golden `jutterswet-maandloon`, rode casus en de open grens in de case (schema, kanaalregel gate 12,
  uitsluiting gate 8)
- `ec040c323` spec: tijdlijnen over alle tijd, open segmenten, EF-036 (RS, ASP-TRANSLATION; de weigerzin CLINGO-024
  noemt de nieuwe vorm)
- `364aae765` tests: de weigeringen rond rekenen per periode (drie fixtures, gedeeld door beide suites)
- `3178e8772` golden: `case5-gelijke-buren` en de NOTE's
- het commit van dit Result

### De meting op main `a9d572d59` (letterlijk; logs onder `werk/timeline-arithmetic-by-rule/logs/02-*`, `06-*`)

Met een verwachting die alleen gesloten segmenten noemt (dus tot de regel), op de eerste regel die ch. 3.4 schrijft:

- ref: `EVALUATION FAULT: temporal attribute "maandloon" as a target: temporal writes are not implemented` (Z4)
- clingo: `TRANSLATION FAULT: CLINGO-024: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape
  (a gedurende condition over an own temporal characteristic); no corpus case for more` (Z7)
- Drools, beide takken: `EXCLUDE DROOLS-105: transpiler SKIP: regel: maandloon van een jutter — a value of the
  time-dependent target 'maandloon' of Jutter that is neither a time-proportional part nor a day count ('zijn
  strandloon plus zijn stormtoeslag'): this branch does not yet carry this form (RUNTIME-SEMANTICS ch. 3.4, 3.5, 3.11);
  no golden with cases writes one` (D105)

De golden zelf, met haar open segmenten, in ref en clingo op main: `CASE ERROR: expected
J1.maandloon.timeline[0]: missing field "from"` (case 1–3; case 4 al op de invoer: `CASE ERROR:
J1.strandloon.timeline[0]: missing field "from"`). De rode casus op main: ref Z4 (`CODE -`), clingo `CODE CLINGO-024`;
beide `EXPECTED FAULT MISMATCH: wanted EF-036`.

### De vorm van een open segment

- **Op de IR-wire**: geen wijziging. Een tijdlijnwaarde staat niet op de wire van `uchar-ir-v1`, alleen in de case.
- **In de case** (`uchar-case-v0`, voorstel): een segment van `timelineValue` en van de verwachting
  `attributeTimeline` eist alleen nog `value`; zonder `from` begint het vóór elke dag, zonder `to` loopt het na elke
  dag door; beide weg is het segment (−∞, +∞). Een segment met beide grenzen is wat het was: elke bestaande case en
  tijdlijn blijft byte-gelijk (gate 2 en 4 groen op het hele corpus). `characteristicTimelines` is ongewijzigd.
  Voorbeeld: `{ "to": "2024-01-01", "value": { "number": "0" } }`.
- **In ref**: de uiteinden van het integerbereik (`Timeline.open_from = min_int`, `open_to = max_int`; `reading.ml`
  gebruikte die conventie al voor een open venstergrens), zodat `read`, de knip, `clip` en `complement` ongewijzigd
  zijn.
- **In clingo**: de termen `#inf` en `#sup` in `tl/5`, `inputtl/5` en `param_tl/4` (ASP-TRANSLATION S9).

### Wat er veranderde

- **ref**: `Timeline.cut_merge` (plus, min) knipt over alle tijd; `summed` vult met de terugval ook buiten de
  bijdragers, en bij nul bijdragers met terugval (−∞, +∞); `grand_total` telt een open segment van 0 als 0 en geeft
  `EF-036` (`attribute`, `label`) op elke andere waarde; `count_days_in_grid` weigert een open periode bij naam (elke
  vouw per emmer loopt erdoor). `Eval.period_timeline` en `run_period_arithmetic` schrijven een regel met plus, min of
  de som over een verzameling op een tijdsafhankelijk attribuutdoel; `Eval.is_period_arithmetic` en
  `period_arithmetic_target` zijn de statische vragen die clingo ook stelt. Het eigen totaal van zo'n doel leest de
  geschreven tijdlijn heel. De lader leest een weggelaten grens als open.
- **clingo**: `translate_period_arithmetic` (één knip per subject met `#inf`/`#sup`, operanden per periodebegin,
  maximale reeksen als kop, de ch. 7.8-arm); een totaal-arm voor zo'n doel met `_wu`-feiten tussen de eindige
  case-grenzen; `open_total_guard` (EF-036) op dat totaal, op de gedurende-clip en op de per-element-vouw;
  `refuse_open_whole_period` bij elke gehele-periodevouw; `period_bounds_not_case` (zie VB-A9); de case-feiten en
  `Model.check` lezen `#inf`/`#sup`; de eindpunten van de kalenderfeiten laten open grenzen weg.
- **spec** (voorstel): RS ch. 3.1 (opdeling van alle tijd, open grens, een scalair is (−∞, +∞), een geschreven
  tijdlijn houdt alleen gevulde segmenten), 3.4 (alle tijd, scalair, voorbeelden herrekend + `WV-3.4-a-min` en
  `WV-3.4-domein`), 3.5 (alle tijd, nul bijdragers, voorbeeld herrekend), 3.6 (open segment, EF-036, eigen totaal van
  zo'n doel), 3.11 (de vorm als schrijver, wat geweigerd blijft), 3.12 (drie rijen), 5.7b (rij EF-036);
  `spec/ASP-TRANSLATION.md` S9 (de open grens en het rekenen per periode).
- **frontend**: geen wijziging; de golden emitteert schoon en gate 10 vindt haar byte-gelijk. Geen gebrek gevonden.

### Golden en rode casus

- `corpus/golden/jutterswet-maandloon` (`source.rgs`, 6 regels, 5 cases, 35 verwachtingen; handberekening in de
  NOTE): plus, min, `5 plus` een tijdlijn, de som over de vondsten met en zonder terugval, en het totaal (780, 3, 0,
  780, 300). Case 1 en 2 dragen de uitgewerkte voorbeelden van ch. 3.4 en 3.5, case 3 de lege wereld (één segment
  (−∞, +∞)), case 4 open invoersegmenten, case 5 het samenvoegen van gelijke buren. ref en clingo: PASS.
- `corpus/red/jutterswet-totaal-open-segment` (1 case): het totaal van `5 plus` een tijdlijn; ref en clingo `CODE
  EF-036 {"attribute":"opgehoogde strandloon","label":"J1"}`.

### Rood en mutaties (niet gecommit; `logs/08-mutations.log`, `logs/09-mutations-2.log`)

Rood eerst: zie de meting hierboven. Per bewaking een mutatie, elk rood:

| # | mutatie | getuige | uitkomst |
|---|---|---|---|
| M1 | ref: knip zonder open uiteinden | golden case 1 | MISMATCH loonverschil |
| M2 | ref: terugval zonder open uiteinden | golden case 1 | MISMATCH gewaarborgde vondstopbrengst |
| M3 | ref: open segment telt altijd 0 | rode casus | EXPECTED FAULT DID NOT OCCUR |
| M4 | ref: lader weigert een weggelaten grens | golden case 1 | CASE ERROR missing field "from" |
| M5 | ref: emmerwandeling zonder weigering | ref-suite | hangt (time-out na 120 s) |
| M6 | ref: voorwaarde niet geweigerd | ref-suite | 1 failure (`WV-3.11-condition`) |
| M7 | ref: eigen totaal valt naar de bucket-arm | golden case 1 | EVALUATION FAULT "over a case-supplied full timeline" |
| M8 | clingo: knip zonder `#inf` | golden case 1 | MISMATCH |
| M9 | clingo: open-segmentbewaking leeg | rode casus | clingo found a model |
| M10 | clingo: gehele-periodevouw over een open tijdlijn toegelaten | clingo-suite | 1 FAILURE |
| M11 | clingo: grenzen van een afgeleide operand niet bewaakt | clingo-suite | 1 FAILURE |
| M12 | clingo: voorwaarde niet geweigerd | clingo-suite | 1 FAILURE |
| M13 | clingo: model leest `#inf` niet | golden case 1 | TRANSLATION FAULT (bound that is no day) |
| M14 | clingo: maximale reeksen niet samengevoegd | golden case 5 | MISMATCH |
| M15 | clingo: invoer zonder `#inf` | golden case 4 | MISMATCH |
| M16 | schema eist `from` weer | gate 4 | `'from' is a required property` |

### Codes en pins, oud en nieuw

- **EF-036** nieuw (ch. 5.7b): een temporeel totaal raakt een segment met een open grens en een waarde ≠ 0;
  kanaal fault; parameters `attribute`, `label`; getuige de rode casus. Geen nieuwe REF-, CLINGO- of DROOLS-code.
- CLINGO-024: de zin noemt nu ook "or the period arithmetic of ch. 3.4/3.5" (code ongewijzigd).
- Gate 12 `EXPECTED_CHANNEL`: + `jutterswet-maandloon|EXCLUDE DROOLS-105|…`; `EXPECTED_PLAYED` blijft 319.
- Gate 8, Drools-uitsluitingen: + `jutterswet-totaal-open-segment|EXCLUDE DROOLS-105 …`. Geen floor verhoogd.
- ref-tests die het besluit (alle tijd) expliciet omdraait, oud → nieuw:
  - `WV-1.2f-tl-1`: [jan, feb) = 4 → (−∞, jan) = 0, [jan, feb) = 4, [feb, +∞) = 0
  - `WV-1.2f-tl-3`: [jan, feb) = 0 → (−∞, +∞) = 0
  - `WV-1.2f-tl-4-fallback`: [jan, feb) = 0 → (−∞, +∞) = 0
  - `WV-3.4-a`: 100, 130, 30 → 0 tot 2024-01-01, 100, 130, 30, 0 vanaf 2024-10-01
  - `WV-3.4-b`: 1, 0, 2 → (−∞, jan) = 0, 1, 0, 2, [apr, +∞) = 0
  - `WV-3.5-b-fallback`: 40, 0, 20 → (−∞, jan) = 0, 40, 0, 20, [apr, +∞) = 0
  - `WV-3.5-c-empty-set`: `summed ~fallback:0 [] = []` → zonder terugval `summed [] = []` (zelfde naam) en met
    terugval (−∞, +∞) = 0 (nieuw: `WV-3.5-c-empty-set-fallback`)
- Nieuwe ref-voorbeelden: `WV-3.4-a-min`, `WV-3.4-domein`, `WV-3.6-open-zero` (780), `WV-3.6-open-nonzero` (EF-036),
  `WV-3.1-open-bucket-walk`, `WV-3.11-open-whole-period`, `WV-3.11-condition`, `WV-3.6-total-over-pro-rata` (240).
  Suites: ref 2176 voorbeelden, clingo 2401 checks.
- Bestaande goldens: gate 5 en 6 spelen alle andere cases onveranderd; geen verwachting van een bestaande golden
  veranderde.

### Voorgestelde CONSTRUCTS-tekst (rij 11, achteraan)

> **Landed (2026-11-03, card timeline-arithmetic-by-rule, step A):** plus, min and the sum per period (RS ch. 3.4,
> 3.5) as the value of a rule on a temporal attribute target, over **all time** (maintainer decision 2026-11-03,
> `spec/proposals/IMPLICIT-TIME.md` point 2): a timeline divides all time, a segment may have an open bound, a scalar
> operand holds on every day; the case format omits `from`/`to` for an open bound; the temporal total counts an open
> segment of 0 as 0 and faults on any other value (EF-036). Executed by ref and the ASP backend (`#inf`/`#sup`);
> witness `jutterswet-maandloon`, red case `jutterswet-totaal-open-segment`; the Drools backend refuses the form
> (DROOLS-105) until step B. Tracked v1 delta: the v0 mirror's span domain (a gap outside the operands' span, and an
> empty timeline for a fallback sum over no contributor) is replaced by all time, following spec §5.1.4.1 and ALEF.

### Voorlopige besluiten

- VB-A1 — De open grens: in de case een weggelaten `from`/`to`; in ref de uiteinden van het integerbereik; in clingo
  `#inf`/`#sup`. Alleen het eerste segment mag een open begin, alleen het laatste een open einde hebben (de lader
  houdt dat via de volgordecontrole).
- VB-A2 — Een tijdlijn die een regel van ch. 3.4/3.5 schrijft, houdt alleen gevulde segmenten, als maximale reeksen;
  een lege periode is een gat. Zo is de verwachting segment-exact en kan clingo haar dragen.
- VB-A3 — De som van ch. 3.5 zonder terugval krijgt geen open segmenten (buiten de bijdragers is zij leeg, een gat);
  met terugval vult zij alle tijd, ook bij nul bijdragers ((−∞, +∞) = terugval). Dat draait de nul-bijdragerclausule
  van ch. 3.5 om; reden: de oude clausule leunde op de span ("geen knip om 0 op te zetten"), die het besluit afschaft.
- VB-A4 — Het eigen totaal van een doel van ch. 3.4/3.5 leest de geschreven tijdlijn heel (geen asOfDate-bucket) en
  telt een afwezige tijdlijn 0 (punt 12c). De conventie voor een door de case geleverde tijdlijn blijft (geweigerd bij
  het eigen totaal zonder clip).
- VB-A5 — Toegelaten operanden: plus/min (genest), een eigen tijdsafhankelijk attribuut, de som van een
  tijdsafhankelijk attribuut over een verzameling zonder die/dat-filter, een literal, een eigen scalair attribuut, een
  scalaire parameter. Bij naam geweigerd in ref en clingo: een tijdsafhankelijke parameter (VB 2.3, voor kaart 1b),
  een filter op de som, een navigatie, elke andere operator (kaart `timeline-arithmetic-per-period`), en een voorwaarde
  op de regel (punt 3, kaart `conditions-per-period`). Een scalair als hele waarde van een tijdsafhankelijk doel blijft
  geweigerd.
- VB-A6 — `characteristicTimelines` krijgen geen open grens: de pro-rata en de dagentelling lopen per emmer over die
  perioden.
- VB-A7 — EF-036 met `attribute` en `label`, kanaal fault. In clingo een viol/2 tijdens het oplossen (de tijdlijn is
  afgeleid), ook op de gedurende-clip en, met het lid als label, op de per-element-vouw over open case-segmenten.
- VB-A8 — Een vouw per emmer over een periode met een open grens wordt bij naam geweigerd: in ref in
  `count_days_in_grid`, in clingo bij elke gehele-periodevouw over een attribuut dat een open grens kan dragen (een
  case-tijdlijn met een open segment, of een doel van rekenen per periode; op attribuutnaam, conservatief).
- VB-A9 — clingo weigert het totaal van een doel van rekenen per periode waarvan een operand (transitief) een
  tijdlijn leest die een pro-rata of dagentelling afleidt: haar grenzen zijn geen case-data, dus de `_wu`-feiten
  passen er niet op. ref beantwoordt die vorm (240 op de fixture). Een gevolgde asymmetrie, luid.

### Wat stap B (Drools) moet weten

- De golden heeft 5 cases met open grenzen in invoer (case 4) en verwachtingen; de case-lezer van Drools
  (`CaseRunner.boundDate`, en de weergave rond `CaseRunner.java` r. 1171) eist nu `from` en `to`.
- De vormen: `Binary` plus/min (genest) over eigen tijdlijnen en een scalair; `Aggregation` `sum` met `elementType` en
  `sourceRole` over tijdsafhankelijke ledenattributen, met en zonder `emptyFallback`; het eigen `total` (zonder filter)
  over zo'n doel, heel gelezen, met EF-036 (`attribute`, `label`) op een open segment ≠ 0. Geen van de zes regels heeft
  een voorwaarde, dus gate 17 vergelijkt er geen lezingen.
- De geschreven tijdlijn: maximale reeksen, lege perioden als gat (VB-A2); bij nul vondsten met terugval één segment
  (−∞, +∞) = 0, zonder terugval de lege tijdlijn (de ch. 7.8-arm: een staand doel, dus `release`).
- Daarna: de kanaalregel van `jutterswet-maandloon` uit `EXPECTED_CHANNEL`, `EXPECTED_PLAYED` 319 → 320, de
  uitsluiting van `jutterswet-totaal-open-segment` in gate 8 wordt een FAULT-pin op EF-036; DROOLS-105 `RETIRED` als
  geen plek de code meer noemt (`p01`–`p09` van IMPLICIT-TIME §5 geven hem ook voor maal en voorwaarden, dus
  waarschijnlijk blijft hij).

### `make gates`

Groen op `3178e8772` (gerebased op `f6ad326c5`): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 376 s
(`out/gates/20261103-115529/`). Gemeten: gate 6 1036 cases ref = clingo, clingo-suite 2401 checks; gate 8 62 rode
cases, 36 foutsoorten in ch. 5.7b, Drools-poot 19 niet-gedragen met reden; gate 10 179 rgs-goldens byte-gelijk; gate
12 PASS met `jutterswet-maandloon` als `EXCLUDE DROOLS-105`. De kaart blijft in `doing/` voor stap B.

## Result: stap B (bouwer, 2026-11-03)

Branch `card/timeline-arithmetic-by-rule`, gerebased op main `02be64433` (stap A van
`timeline-arithmetic-per-period`; zonder conflict). Niet gemerged, niet gepusht. Beschermd in deze stap, dus
**voorstel**: `scripts/check-drools.sh`, `scripts/check-red.sh`.

### Commits

- `8f549a67d` drools: rekenen per periode over alle tijd met truth maintenance
- `283d0afbc` gates 8 en 12: maandloon speelt, de rode casus pint EF-036 (voorstel)
- `eaac6ce19` tests: de schrijfgrens per segment van rekenen per periode
- `ab00aa269` tests: een lid met een open einde in de vouw per lid, en de rode stand
- het commit van dit Result

### Rood eerst (op `53ab929d5`, letterlijk; `werk/timeline-arithmetic-by-rule-drools/logs/00-red-first.log`)

- golden, beide takken: `EXCLUDE DROOLS-105: transpiler SKIP: regel: maandloon van een jutter — a value of the
  time-dependent target 'maandloon' of Jutter that is neither a time-proportional part nor a day count ('zijn
  strandloon plus zijn stormtoeslag'): this branch does not yet carry this form (RUNTIME-SEMANTICS ch. 3.4, 3.5,
  3.11); no golden with cases writes one`
- rode casus, `DroolsRedMain`: `EXCLUDE DROOLS-105: transpiler SKIP: regel: opgehoogd strandloon van een jutter — …
  ('5 plus zijn strandloon') …`, dus geen FAULT EF-036.
- De nieuwe getuige `TmsPeriodArithmeticWitnessTest` op `53ab929d5` (met alleen de testbestanden erbij): alle elf
  tests rood, de vormen op die DROOLS-105-weigering en de klip, de vouw per lid en de gehele-periodekwalificator
  op de case-lezer, `segment op '…' mist een 'from'-datum` (`logs/19-red-witness.log`).

### Na de bouw

- golden `jutterswet-maandloon`: `PLAYED 5` op beide takken; rode casus: `FAULT EF-036
  {"attribute":"opgehoogde strandloon","label":"J1"}: OpenSegmentFault: error in the rule "opgehoogd jaarloon van een
  jutter": the temporal total of "opgehoogde strandloon" on "J1" meets a segment with an open bound carrying 5: …`
- de lezingen van de vijf cases zijn per regel gelijk aan de trace van ref, op beide takken.
- de twee goldens van `timeline-arithmetic-per-period` stap A blijven op beide takken `EXCLUDE DROOLS-105`
  (`dagrente`, `loonproduct van een jutter`: maal, een andere operator dan de kaart draagt), dus hun
  kanaalregels houden.

### Wat er veranderde

- **Case-lezer** (`CaseRunner.timelineSegments`): een weggelaten `from` of `to` van een segment van een invoer en van
  een verwachte tijdlijn is een open grens, op de `Segment`-bean null; alleen het eerste segment mag open beginnen en
  alleen het laatste open eindigen (de volgordecontrole), en een open grens ligt op geen raster. De verdictregel
  schrijft `-inf`/`+inf`. `characteristicTimelines` eist beide grenzen (VB-A6). Een case zonder open grens leest en
  vergelijkt als voorheen.
- **`Period`**: een open grens is `Period.OPEN_FROM` (`LocalDate.MIN`) of `Period.OPEN_TO` (`LocalDate.MAX`);
  `Period.list` leest een null-grens van een bean als open, `Period.beans` schrijft hem als null terug.
- **Runtime, zuivere functies** (`PeriodArithmetic`, precedent `Allocation.compute`): `scalar`, `plus`, `minus`
  (knip over alle tijd, leeg → 0), `sum` (bijdragers overslaan, terugval over alle tijd, ook bij nul bijdragers),
  `written` (gevulde segmenten als maximale reeksen, leeg een gat, schrijfgrens per segment, EF-006), `total` (open 0
  telt 0, open ≠ 0 geeft `OpenSegmentFault`, EF-036, `attribute`, `label`), `openNonZero`, `withoutOpenZeros`,
  `openSegmentFault`. `OpenSegmentFault` is een evaluatiefout, wordt bij het terugschrijven naar het caselabel
  genoemd en is een foutsoort voor de rode route (`CaseRunner.faultKinds`) en de dump.
- **Lowering met truth maintenance** (`TmsTranspiler`, één analyse, beide weergaven): `timelineRule` laat plus/min en
  de som met elementtype toe (`isPeriodArithmetic`, de vraag van `Eval.is_period_arithmetic`); `periodArithmetic`,
  `periodOperand`, `scalarOperand`, `periodSum`. De som is een vouw waarvan elk lid zijn hele tijdlijn bijdraagt
  (`Consequence.Input.MemberValue.whole`, accumulate `LIST`); een lid als emmerscalair geeft een lidfout in de woorden
  van ref. Het eigen totaal van een doel van rekenen per periode (`periodArithmeticTarget`) leest de tijdlijn heel
  (`PERIOD_TOTAL`, afwezig → 0); de klip loopt ook via `PERIOD_TOTAL`; de vouw per lid krijgt een derde lidfoutregel
  (`OPEN_NONZERO`, `MemberFault` met `openAttribute`). De gehele-periodekwalificator faalt op een open segment
  (`ComputeFault`, de zin van ref).
- **Lezingen**: de knopen van een waarde van rekenen per periode zijn `unreached`, zoals de trace van ref ze leest
  (zie open punt 1); `SessionDump` schrijft een open grens als `-`, zoals ref.
- **README** van de backend: de alinea "Period arithmetic over all time" en de weigeringen.

### Codes en pins, oud en nieuw

- Nieuw, beide catalogi: `DROOLS-302` (`period-arithmetic-condition`, REFUSAL), `DROOLS-303` (`…-operand`, REFUSAL),
  `DROOLS-304` (`…-time-parameter`, REFUSAL), `DROOLS-305` (`period-sum-subselection`, REFUSAL), `DROOLS-306`
  (`…-restatement`, LIMIT), `DROOLS-307` (`…-inexact`, LIMIT), `DROOLS-308` (`…-finer-operand`, REFUSAL); `EF-036` en
  `PART.desc.period-arithmetic` in beide catalogi. Register 301 → 308 codes; geen floor gewijzigd.
- `DROOLS-105` blijft (niet `RETIRED`): het geeft elke andere waarde van een tijdlijndoel, onder meer maal, en de twee
  goldens van `timeline-arithmetic-per-period` rusten erop. De zin noemt nu "nor the period arithmetic of plus, min or
  a sum per period". `DROOLS-216` blijft, voor het totaal zonder klip over een tijdlijn van een pro rata of een
  dagentelling.
- Gate 12 (voorstel): `EXPECTED_CHANNEL` − `jutterswet-maandloon|EXCLUDE DROOLS-105|…`; `EXPECTED_PLAYED` 319 → 320,
  met een regel in het commentaarblok.
- Gate 8 (voorstel): de uitsluiting `jutterswet-totaal-open-segment|EXCLUDE DROOLS-105 …` weg; het Drools-been pint de
  casus nu op `EF-036` met `attribute` en `label`: 44 fault-gepind (was 43), 18 niet-gedragen (was 19), 16 EXCLUDE met
  code (floor 16).
- Tests die de kaart omdraait, oud → nieuw:
  - `TmsTimelineRefusalWitnessTest`, rij `som-over-tijdlijnen`: DROOLS-105 → weg; hetzelfde document speelt in
    `TmsPeriodArithmeticWitnessTest.the_sum_per_period_that_the_branch_refused_plays` (50, 70, 10).
  - `TemporalSetFoldRangeWitnessTest`: `the_temporal_set_fold_over_a_unique_pair_is_refused` (DROOLS-105) →
    `…_plays` (19 / 17 / 12, beide takken); `the_temporal_set_fold_reads_the_wire_binding_on_a_repeated_pair`:
    geweigerd → speelt.
  - `RoleFieldReaderWitnessTest.the_temporal_set_summation_refuses_a_join_without_field`: verwachte zin DROOLS-105 →
    de zin van de bereikafleiding ("een Aggregation over de enkelvoudige elementrol 'vondst' wiens zusterrol
    'bewarende jutter' meervoudig is …"), nog steeds gelijk op beide takken.
  - Plangetuigen: `FoldPlanWitnessTest` drukt `whole`, `open` en `timeline` af en pint de nieuwe lidfoutregel bij
    `jutterswet-keurlast` en de plannen van de som per periode (`a_sum_per_period_with_its_member_fault`, nieuw);
    `TestTermWitnessTest` de toets `OPEN_NONZERO`; `RouteWitnessTest` de route `OPEN_SEGMENT_FAULT` en de extra
    `boolean` van `contribution`; `FormsPlan` twee vormen erbij en het fragment `"MAAND", true)` →
    `"MAAND", true, false)`.
- Nieuwe getuige `TmsPeriodArithmeticWitnessTest` (11 tests): plus en min genest met een scalair en het totaal erover
  (24 volgorden × 2 takken), de som per periode met en zonder terugval over afgeleide ledentijdlijnen (24 × 2), het
  geweigerde document dat nu speelt, EF-036 op het eigen totaal (2 × 2), de klip en de vouw per lid (open begin én open
  einde), de emmerscalair als operand en als lid, de gehele-periodewandeling, de schrijfgrens, de case-lezer en de
  weigeringen 302–308 en 105 op beide takken in één zin. Gemeten: verifier, ref en clingo laten de vier speelbare
  documenten van `main` door (`logs/08-ref-clingo-witness.log`).

### Mutaties (niet gecommit; `logs/18-mutations.log`, `logs/21-mutations.log`)

| # | mutatie | uitkomst |
|---|---|---|
| M1 | knip zonder open uiteinden | 3 tests rood (A, B, D) |
| M2 | terugval zonder open uiteinden | B rood |
| M3 | open segment telt altijd 0 in het totaal | eigen totaal en klip rood |
| M4 | gelijke buren niet samengevoegd | A en B rood |
| M5 | geen schrijfgrens per segment | schrijfgrens rood |
| M6 | `Period.list` leest null niet als open | klip, gehele periode, vouw per lid rood |
| M7 | terugschrijven zet het datumuiterste op de bean | A, B, D rood |
| M8 | case-lezer eist `from` en `to` | 6 tests rood |
| M9 | open begin na het eerste segment toegelaten | D rood |
| M10 | bijdrage negeert een open segment ≠ 0 | eerst groen (alleen een open begin, de lidfout dekte het); getuige uitgebreid met een open einde (`ab00aa269`), daarna rood |
| M11 | gehele-periodewandeling zonder weigering | de JVM van surefire valt om: `OutOfMemoryError` in `rsGeheelPeriode` |
| M12 | voorwaarde niet geweigerd | E rood |
| M13 | knopen niet als onbereikt genoteerd | `--readings` op de golden: `ERROR IllegalStateException: … de analyse noteerde geen term voor knoop 1` (gate 17 rood) |
| M14 | lezing schrijft een open grens als datum | lezing van regel 5 wijkt af van ref (gate 17 rood) |
| M15 | EF-036 niet uit de fout gelezen | `DroolsRedMain`: `FAULT OpenSegmentFault: …` zonder code (gate 8 rood) |
| M16 | klip-totaal via `rsTijdTotaal` | klip rood |
| M17 | eigen totaal van een doel van rekenen per periode geweigerd | A en eigen totaal rood |
| M18 | geen lidfoutregel over een open segment | vouw per lid rood |

### Voorgestelde CONSTRUCTS-tekst (rij 11, vervangt het voorstel van stap A)

> **Landed (2026-11-03, card timeline-arithmetic-by-rule):** plus, min and the sum per period (RS ch. 3.4, 3.5) as
> the value of a rule on a temporal attribute target, over **all time** (maintainer decision 2026-11-03,
> `spec/proposals/IMPLICIT-TIME.md` point 2): a timeline divides all time, a segment may have an open bound, a scalar
> operand holds on every day; the case format omits `from`/`to` for an open bound; the temporal total counts an open
> segment of 0 as 0 and faults on any other value (EF-036). Executed by ref, the ASP backend (`#inf`/`#sup`) and both
> branches of the Drools backend (`PeriodArithmetic`, an open bound `LocalDate.MIN`/`MAX` in a `Period` and null on a
> bean); witness `jutterswet-maandloon`, red case `jutterswet-totaal-open-segment`. Tracked v1 delta: the v0 mirror's
> span domain (a gap outside the operands' span, and an empty timeline for a fallback sum over no contributor) is
> replaced by all time, following spec §5.1.4.1 and ALEF.

### Voorgestelde tekst elders (beschermd of buiten mijn may-touch, voor de hoofdsessie)

- `CLAUDE.md`, gate 12, na de zin over `subtype-characteristic-in-fold-filter`: "Since the card
  `timeline-arithmetic-by-rule` (2026-11-03) the lowering writes the period arithmetic of RUNTIME-SEMANTICS ch. 3.4
  and 3.5 over all time (`PeriodArithmetic`): an open bound is `LocalDate.MIN`/`LocalDate.MAX` in a `Period` and null
  on a bean, and a total over an open segment that is not 0 faults with EF-036 (`OpenSegmentFault`)."
- RS ch. 3.11, eerste opsommingspunt: "Executed by `ref` and the ASP backend" → "Executed by `ref`, the ASP backend
  and both branches of the Drools backend". RS ch. 3.12, rij "Plus, min and the sum per period": "both Drools branches
  refuse it by name (DROOLS-105)" → "and both Drools branches"; "refused by name in both OCaml executors" → "in every
  executor".
- `corpus/red/jutterswet-totaal-open-segment/NOTE.md`, laatste zin: "The Drools backend refuses the rule that writes
  the timeline (DROOLS-105), which the gate-8 exclusion list records." → "The Drools backend faults alike on both
  branches."

### Voorlopige besluiten

- VB-B1 — Een open grens is in `Period` het uiterste van het datumbereik, zodat `rsTijdKnip`, `rsTijdKnipNiet`,
  `rsTijdConditie`, de pro rata (die de basis op een emmerbegin leest) en de vergelijkingen ongewijzigd blijven; alleen
  een telling van eenheden en een emmerwandeling moeten ernaar vragen. Op de bean is hij null, de vorm van de case.
  Gevolg voor de distributie: een `Segment` van een geschreven tijdlijn kan nu `van` of `tot` null hebben.
- VB-B2 — De knopen van een waarde van rekenen per periode lezen `unreached`, gelijk aan de trace van ref, zodat gate
  17 vergelijkt (zie open punt 1).
- VB-B3 — Eenheden: geen herrekening (DROOLS-306, LIMIT); een eenheidloze kant neemt de andere over (ch. 2.6). Een doel
  of scalaire operand van de onbegrensde vorm, en een literal die niet eindigt, weigert de tak (DROOLS-307), omdat een
  segment een decimaal houdt. Een operand op een fijner raster dan het doel weigert de tak bij naam (DROOLS-308), naar de
  regel van ch. 3.4; ref en clingo controleren dat niet (bevinding 4 van IMPLICIT-TIME §5).
- VB-B4 — EF-036 ook op de klip en de vouw per lid (RS ch. 3.6, zoals ref en clingo): `PERIOD_TOTAL` voor de klip,
  een lidfoutregel per lid met een open segment ≠ 0 voor de vouw, het lid als label; de bijdrage van zo'n lid valt weg,
  ook met een open einde, waar `rsTijdTotaal` anders een `DateTimeException` gooit.
- VB-B5 — De gehele-periodekwalificator over een segment met een open grens faalt met een `ComputeFault` in de zin van
  ref (geen EF-code, zoals in ref).
- VB-B6 — Een doel van rekenen per periode waarvan de case de invoer als emmerscalair geeft en dat de regel niet
  schrijft (een staand doel met een lege som), faalt bij het eigen totaal met een `InputFormFault` (ch. 8.4). Geen
  golden heeft het; ref telt daar leeg.
- VB-B7 — Het eigen totaal van een afwezige tijdlijn van zo'n doel is 0 (VB-A4, RS ch. 3.6). Gemeten op een document
  buiten de repo (de som zonder terugval over nul vondsten, totaal): ref, clingo en Drools geven alle drie 0.
- VB-B8 — Een lid als emmerscalair faalt in de som per periode vóór de terugval wordt geëvalueerd; ref evalueert de
  terugval eerst. Verschil alleen waar beide falen.

### Open punten

1. **De trace van ref leest de knopen van een waarde van rekenen per periode als `unreached`**: `ref/lib/reading.ml`
   (de arm van een tijdsafhankelijk doel) rekent de waarde via `Eval.run_pro_rata`, niet via
   `run_period_arithmetic`, dus de waarnemer ziet geen knoop. RS ch. 5.12 vraagt hun waarden. De Drools-lezingen volgen
   ref, zodat gate 17 groen is; een kaart die de trace de tijdlijnen laat lezen, moet `TmsTranspiler.unreached`
   tegelijk weghalen.
2. De distributie (`regelspraak-toolchain`) leest de beans: een `Segment` met een null-grens bij de pin-bump nalopen.
3. De drie tekstvoorstellen hierboven (RS ch. 3.11/3.12, NOTE van de rode casus, CLAUDE.md) zijn niet aangebracht.

### `make gates`

Groen op `ab00aa269` (gerebased op `02be64433`): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 326 s
(`out/gates/20261103-130727/`):

```
01-self-contained rc=0 2s     02-ir-schema rc=0 127s   04-case-schema rc=0 7s   03-verifier rc=0 38s
05-oracle rc=0 22s            06-clingo rc=0 266s      07-noop rc=0 13s         08-red rc=0 147s
09-catala rc=0 6s             10-regelspraak rc=0 233s 11-toka-parity rc=0 217s 12-drools rc=0 326s
13-reftype rc=0 281s          14-flogic rc=0 2s        15-graph rc=0 47s        16-scenario rc=0 38s
17-readings rc=0 244s         after-run rc=0 0s        end 2026-11-03 13:12:53 326s red=0
```

Gemeten: gate 12 `played matches the pinned 320`, de DRL-tekst-tak 320, kanaalvloer 49 met naam, JUnit 1787 tests
(nul skips, floor 1573); gate 8 Drools-been 44 fault-gepind, 18 niet-gedragen, 16 EXCLUDE met code; gate 17 320 goldens
en 906 cases op beide takken vergeleken, 0 afwijkingen. De kaart blijft in `doing/`.
