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
