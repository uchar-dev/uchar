# timeline-arithmetic-per-period: rekenen per dag, de tijdsafhankelijke parameter en de kalender (impliciete tijd, fase 1b en 2b)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` en het voorstel
`spec/proposals/IMPLICIT-TIME.md`, punt 1, 2 en 11. Na `20261103-timeline-arithmetic-by-rule`. Besluiten van de eigenaar
2026-11-03: alle tijd (2); **de kalender** (11): tussen een eenheid die in dagen telt (dg, wk) en een die in maanden telt (mnd,
kw, jr) integreert het totaal met de werkelijke lengte van de omsluitende kalendereenheid, en een gedeclareerde factor tussen
die families is ill-formed; binnen een familie blijft de factor.*

## Stap A — spec, ref, clingo, verifier, frontend

Goal: maal, gedeeld door (beide varianten), percentage van en verminderd met over tijdlijnen, per dag met de leeg-regels van
ch. 1.2 (punt 1, besloten: spec spreekt); een scalair en een tijdsafhankelijke parameter als tijdlijnoperand (VB 2.3); de
integratie van een rente over een raster van een andere eenheid volgens de kalender; in ref, clingo en de verifier, met
formulering B van de ankercasus als anker.

Acceptance:
- (a) Rood eerst: anker-golden `corpus/golden/anchor/jutterswet-verzuimrente-dagen` (beschermd: voorstel) met geval 1 en 2 van
  formulering B (IMPLICIT-TIME §4): `rentedragend` als invoer, de dagrente (voor elke dag, €/jr) = de verzuimrentevoet maal het
  afdrachtbedrag, de verzuimrente = het totaal van de dagrente gedurende de tijd dat hij rentedragend is; verwachtingen
  **139 13/73 €** en **73 47/61 €** (de kalender, 2028 is een schrikkeljaar). Op main de weigeringen Z4/Z15 (ref), Z7/Z16
  (clingo) van IMPLICIT-TIME §5.
- (b) Het dagraster in de laders van ref en clingo (Z14) en in de ASP-codering; een totaal over een dagraster.
- (c) RS ch. 2.4 (VB 1.4: de precisiepoort ook op een tijdlijndoel met zo'n waarde), 3.4, 3.6 (de kalender; de voorbeelden met
  mnd = 30 dg en hun `ref`-tests gaan mee, met oud en nieuw genoemd), 3.12 en 8.3 (voorstel); WF-023 op een tijdlijndoel; een
  nieuwe WF-arm voor een factor tussen de dag- en de maandfamilie (voorstel); een negatief per nieuwe arm; rood voor EF-031 en
  EF-005 in een periode (VB 1.3: één §9.5-feit per regel × onderwerp × code).
- (d) De `ref/test`-rijen `WV-3.4-per-dag`, `WV-3.4-domein` (als 1a haar nog niet heeft) en `WV-3.6-kalender` van het voorstel
  letterlijk.
- (e) De RegelSpraak-frontend schrijft de vormen van het anker (gate 10, `source.rgs`).
- (f) Meet, alleen lezend, of een regelset onder `/Users/yeb/Developer/uwv/regelspraak-werkbank/rulesets/regelspraak-rules/v2/` of
  een golden een factor tussen de dag- en de maandfamilie declareert (de nieuwe WF-arm zou die weigeren); noem elke vondst.
- (g) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `spec/verifier/` (voorstel), `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`,
`spec/ASP-TRANSLATION.md` (voorstel), `corpus/golden/anchor/<nieuw>/` (voorstel), `corpus/negative/<nieuw>/`,
`corpus/red/<nieuw>/`, `frontends/regelspraak/**` (alleen een gebrek dat het anker blootlegt), `scripts/check-drools.sh`,
`scripts/check-red.sh` (voorstel), kaartverplaatsing.

Out of scope: voorwaarden per periode, de kwalificator, twee schrijvers en de dagentelling (`20261103-conditions-per-period`);
het totaal over een gedeeltelijke eenheid (`20261103-total-over-partial-unit`).

## Stap B — Drools (direct na de landing van stap A)

Acceptance: (a) rood eerst: het anker geeft op main DROOLS-105, DROOLS-030 (tijdsafhankelijke parameter) of DROOLS-228
(integratie over een dagraster); (b) `PLAYED` op beide branches, `EXPECTED_PLAYED` omhoog, kanaalpins weg (voorstel),
codes zonder plek `RETIRED`; (c) gate 17 gelijk, een getuige per nieuwe vorm in elke regelvolgorde; (d) `make gates` groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (voorstel), kaartverplaatsing.

## Omvang

Middel tot groot (stap A), middel (stap B).

## Result: stap A (bouwer, 2026-11-03)

Branch `card/timeline-arithmetic-per-period` op main `53ab929d5`. Niet gemerged, niet gepusht. Beschermde paden in
deze branch, dus **voorstel**: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/verifier/`,
`corpus/golden/anchor/` (het nieuwe anker en de README), `scripts/check-drools.sh`. `spec/ASP-TRANSLATION.md`
valt onder het CODEOWNERS-net (voorstel).

### Commits

- `d79084a33` kaart naar `doing/`
- `eb344c57f` verifier: de kalenderarm van WF-009, WF-023 op een tijdlijndoel, de grens van het kalendertotaal;
  negatieven `wf009-calendar-family-factor` en `wf023-precision-timeline-target`
- `eb0bc2843` ref: rekenen per dag, de tijdsafhankelijke parameter, de kalender, het dagraster in de lader
- `20ab6b8c4` clingo: hetzelfde, met de noemer D onder een afronding (README [V6])
- `42f22a293` corpus: anker `anchor/jutterswet-verzuimrente-dagen`, golden `jutterswet-rekenen-per-dag`,
  anker-README, twee kanaalregels in gate 12
- `27e981fad` spec: RUNTIME-SEMANTICS, IR-SPEC, ASP-TRANSLATION
- `933dcb22c` tests: de schrijfgrens per segment; negatief `wf023-precision-calendar-total`
- `12955890f` spec: die negatief bij WF-023 en ch. 2.3
- `64fb6bdb5` tests: het product met de geschaalde operand rechts
- het commit van dit Result

### De meting op main `53ab929d5` (letterlijk; logs `werk/timeline-arithmetic-per-period/logs/04-*`, `09-*`)

Het anker zoals het nu in de corpus staat, en de golden `jutterswet-rekenen-per-dag`, beide door de frontend
geschreven (schoon, geen diagnose) en verifier-PASS op main:

- ref en clingo, op elke case met een tijdlijn op een dagraster (verwacht of als invoer), bij het laden (Z14):
  `CASE ERROR: expected A1.dagrente: the declared grid is day, which every day boundary sits on, yet this loader
  checks a timeline's grid through the calendar bucketing and v0 defines buckets for month and year only
  (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, not as a defect of the case` (in de golden
  `J1.strandloon` in plaats van `expected A1.dagrente`).
- Zonder tijdlijn op een dagraster (het anker zonder de verwachte dagrente; `jutterswet-rekenen-per-dag` case 3):
  ref `EVALUATION FAULT: temporal attribute "dagrente" as a target: temporal writes are not implemented` (Z4), clingo
  `TRANSLATION FAULT: CLINGO-024: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a
  gedurende condition over an own temporal characteristic) or the period arithmetic of ch. 3.4/3.5; no corpus case for
  more` (Z7).
- Het totaal over het dagraster alleen (variant: `het totaal van zijn dagrente`, dagrente als asOfDate-scalair): ref
  `EVALUATION FAULT: bucketing is defined for month and year only (ch. 3.2)` (Z15); clingo `TRANSLATION FAULT: the
  noemer strip of the temporal total of "dagrente" cannot integrate a EUR/jr value over a dg grid: the declarations
  connect no conversion between its denominator and the grid unit (ch. 3.6); ref refuses the shape too` (de vorm van
  Z16 zonder gedeclareerde factor; met `jr = 365 dg` gaf clingo Z16 zelf).
- Drools, beide takken, beide goldens: `EXCLUDE DROOLS-105: transpiler SKIP: regel: dagrente — a value of the
  time-dependent target 'dagrente' of Afdracht that is neither a time-proportional part nor a day count ('de
  verzuimrentevoet maal zijn afdrachtbedrag'): this branch does not yet carry this form (RUNTIME-SEMANTICS ch. 3.4,
  3.5, 3.11); no golden with cases writes one` (in de golden over `loonproduct van een jutter`); de totaalvariant
  `EXCLUDE DROOLS-228: … over 'dagrente' with the rate EUR/jr on a grid of DAG: the denominator is not the unit of the
  grid, and this branch does not recalculate it (RUNTIME-SEMANTICS ch. 3.6); no golden with cases has it`. DROOLS-030
  kwam niet: het Drools-laadprogramma leest de tijdsafhankelijke parameter.
- De drie negatieven op main: `verifier: PASS`.

### Wat er veranderde

- **ref**: `Eval.period_timeline` past elke binaire operator van ch. 1.2 behalve de macht per periode toe via
  `Timeline.cut_merge` met de scalaire operator en zijn leeg-regel (`Arith.add`, `sub_min`, `sub_reduced`, `mul`,
  `div_`, `div_abs`, `pct_of`); een deling meldt per periode via `err`, en het §9.5-kanaal houdt één feit per regel ×
  onderwerp met de codes als verzameling (VB 1.3, zonder wijziging aan het kanaal). Een tijdsafhankelijke parameter is
  een tijdlijnoperand (zijn tijdlijn uit de case, zonder: EF-014). `Timeline.integrated_segment` integreert tussen de
  families met `calendar_measure` (1/len per dag van de kalendermaand, -kwartaal of -jaar van de noemer) over de hele
  rastereenheden (`whole_units_end`) en weigert een gedeclareerde factor tussen de families bij naam. De lader leest een
  tijdlijn op het dagraster.
- **clingo**: `translate_period_arithmetic` geeft elke knoop zijn eigen schaal (plus/min/verminderd met harmoniseren,
  maal en percentage van vermenigvuldigen de schalen, een quotiënt op de schaal van het doel, de abs-variant op 10^5),
  de leeg-regel per operator, de §9.5-feiten van een deling via `deposit`, een tijdsafhankelijke parameter via
  `param_tl/4`, en één herschaling van de wortel naar het doel (EF-006-getuige `non-exact period arithmetic value …`).
  Het geknipte totaal neemt de grenzen van de parametertijdlijnen mee als het doel rekenen per periode is (anders viel
  een fragment stil weg; mutatie C3) en weigert afgeleide grenzen zoals het ongeknipte totaal (VB-A9). De kalender:
  `grid_strip` geeft de kalendereenheid, `calendar_weights` legt de gewichten D × maat, het totaal staat op schaal ×
  D, en alleen een afronding van dat totaal deelt D weg (`calendar_rounding`, README [V6]).
- **verifier**: `WF-009.calendar-family-factor` (`unit`, `canonical`, `unitSystem`); WF-023 op een regel met een
  tijdlijndoel, de pro rata uitgezonderd; de precisiegrens van het kalendertotaal ∞ (behalve dg over een
  maandraster). Gedeelde helpers in `Checks`: `calendar_family`, `unit_calendar_family`, `calendar_denominator`,
  `fold_grain`.
- **spec** (voorstel): RS ch. 1.2, 2.3 (rij totaal), 2.4 punt 4, 2.6 regel 8, 3.2, 3.4 (alle operatoren per dag,
  `WV-3.4-per-dag`, de parameter, het feit per regel × onderwerp, eenheden en de poort per segment), 3.6 (de kalender,
  de voorbeelden herrekend), 3.11, 3.12 (drie rijen bijgewerkt, één nieuw), 8.3; IR-SPEC ch. 2.5 (de kalenderarm),
  WF-023 (ch. 3.1) en de tabelrijen WF-009 en WF-023; ASP-TRANSLATION S9.
- **frontend**: geen wijziging. Het anker en de golden emitteren schoon en gate 10 vindt ze byte-gelijk; de frontend
  legde geen gebrek bloot. Wel gemeten, niet gerepareerd (buiten "een gebrek dat het anker blootlegt"): de frontend
  emitteert zonder diagnose wat de verifier nu weigert — `jr = 365 dg`, een quotiënt in een geheel tijdlijndoel
  (`Typing.precisionGate` laat een tijdlijndoel buiten de poort) en het kalendertotaal zonder afronding in twee
  decimalen (de grens van het totaal kent de kalender niet). Open punt voor een frontendkaart.
- **gate 12** (voorstel): `EXPECTED_CHANNEL` + `anchor/jutterswet-verzuimrente-dagen|EXCLUDE DROOLS-105|…` en
  `jutterswet-rekenen-per-dag|EXCLUDE DROOLS-105|…`. Verder geen script.

### Het anker en de corpus

- `corpus/golden/anchor/jutterswet-verzuimrente-dagen` (`source.rgs`, 2 regels, 2 cases, 6 verwachtingen; NOTE):
  dagrente (€/jr, dagraster) = verzuimrentevoet (1/jr, maandraster, 0,04 tot 2026-02-01, 0,05 daarna, open grenzen)
  maal afdrachtbedrag (10 000 €, geheel); verzuimrente = (het totaal van de dagrente gedurende de tijd dat hij
  rentedragend is) rekenkundig afgerond op 2 decimalen; `rentedragend` uit de case. Geval 1: 139 13/73 € → **139,18 €**;
  geval 2: 73 47/61 € → **73,77 €**; de dagrente in beide (−∞, 2026-02-01) = 400, [2026-02-01, +∞) = 500. De keuze
  van het doel: een afronding op de cent, omdat de ASP-codering een getal als geheel getal op een decimale schaal houdt
  en 139 13/73 op geen enkele schaal past; de exacte waarden pinnen `WV-3.6-kalender`, `WV-3.6-kalender-geval-1` en
  `WV-3.6-kalender-onafgerond` (de fixture `calendar-total-unrounded`: het anker in de onbegrensde vorm zonder
  afronding, exact 10160/73 in ref, in clingo bij naam geweigerd). Mutatie R6 (elk jaar 365 dagen) geeft 73,97 €.
- `corpus/golden/jutterswet-rekenen-per-dag` (`source.rgs`, 6 regels, 3 cases, 25 verwachtingen; NOTE): maal,
  verminderd met, gedeeld door, gedeeld door (ABS) en 10% van over twee tijdlijnen op een dagraster, en het totaal
  over het dagraster (227 500, 552 000, 0). Case 2 legt de deler in vier perioden leeg en in één op 0: één feit per
  deelregel × J1 met EF-031 en EF-005. Eén handwaarde was fout (jaarproduct van case 2: 547 200, een misgetelde
  dagen); ref gaf 552 000, een aparte telling met `fractions`/`datetime` bevestigde het; de NOTE zegt het.
- Negatieven (3, nieuw): `wf009-calendar-family-factor` (`jr = 365 dg`), `wf023-precision-timeline-target` (een
  quotiënt in een geheel tijdlijndoel), `wf023-precision-calendar-total` (het kalendertotaal zonder afronding in 2
  decimalen); alle drie op main `verifier: PASS`.
- Rode gevallen: geen. EF-031 en EF-005 zijn §9.5-soorten (ch. 5.7b): hun getuigen zijn golden
  `inconsistent`-verwachtingen (golden `jutterswet-rekenen-per-dag` case 1 en 2), geen rode gevallen (VB-B5).
- Suitefixtures (`ref/test/fixtures/period-arithmetic/`, gedeeld door beide suites): `calendar-total-unrounded`,
  `quotient-not-whole` (10/3, `WV-3.4-per-dag` met b = 30), `parameter-unbound`, `period-arithmetic-misfit`,
  `product-right-scale`.

### De meting van (f) (alleen lezend)

Gezocht naar een gedeclareerde factor tussen een eenheid van de dagfamilie (dag/dg, week/wk) en een van de
maandfamilie (maand/mnd, kwartaal/kw, jaar/jr), langs de canonieke keten: **geen vondst** in een regelset van
`regelspraak-rules/v2/` of in een golden. Wel gevonden, alle binnen één familie (de arm laat ze staan):
- werkbank v2: `fundament/rules/fnd-01-vocabulaires-eenheden.rgs`, `herleving-ww/docs/naamproef-modelkaart.rgs` en
  `herleving-ww/out/ir.json`: `wk = 7 dg`, `jr = 12 mnd`; `herleving-ww/README.md` en `docs/MODELKAART.md` noemen
  dezelfde in proza; de treffers met `= … dagen` in `scenarios/toets-g7-g9-restant-en-wekeneis.rgs` zijn commentaar
  (datumrekening), geen eenheden.
- corpus (alle zones): `jaar = 12 maand` (Termijn, 7 goldens, en `24/2` in een negatief van WF-092), `jr = 12 mnd` (1),
  `wk = 7 dg` (3); `toka/`: `jr = 12 mnd`, `kw = 3 mnd`, `wk = 7 dg` en de eenheden onder de dag (`u`, `minuut`, `s`,
  `ms` tegen `dg`), die tot geen familie horen (VB-B3).
Alleen de nieuwe negatief `wf009-calendar-family-factor` declareert `jr = 365 dg`.

### Rood en mutaties (niet gecommit; `logs/12-mutaties*.log`)

Rood eerst: zie de meting op main. Per bewaking een mutatie in een losse worktree, elk rood:

| # | mutatie | getuige | uitkomst |
|---|---|---|---|
| R1 | ref: maal niet als rekenen per periode | golden per-dag case 1 | Z4 terug |
| R2 | ref: deling per periode zonder melding | golden per-dag case 1 | MISMATCH inconsistent EF-031 |
| R3 | ref: parametertijdlijn leeg gelezen | anker case 1 | EVALUATION FAULT (eenheden) |
| R4 | ref: ongebonden parameter leest leeg | ref-suite | `WV-3.4-tijdsafhankelijke-parameter-ongebonden` |
| R5 | ref: lader weigert het dagraster | ref-suite | uncaught exception |
| R6 | ref: kalender telt elk jaar 365 dagen | anker case 2 | MISMATCH 73,97 ≠ 73,77 |
| R7 | ref: factor tussen de families toegelaten | ref-suite | `WV-3.6-kalender-gedeclareerde-factor` |
| R8 | ref: hele maanden zonder hun dagen | ref-suite | `WV-3.6-g-vreemde-noemer`, `-kalender-week-over-maanden` |
| C1 | clingo: schaal van maal alleen links | clingo-suite | `product-right-scale` |
| C2 | clingo: deling zonder EF-031 | golden per-dag case 1 | MISMATCH inconsistent |
| C3 | clingo: clip zonder parametergrenzen | anker case 1 | MISMATCH verzuimrente 0 |
| C4 | clingo: afronding markeert het totaal niet | anker case 1 | TRANSLATION FAULT (de [V6]-weigering) |
| C5 | clingo: kalendertotaal ook zonder afronding | clingo-suite | README [V6] |
| C6 | clingo: gewichten over alle eindpunten | anker case 2 | TRANSLATION FAULT (gewicht 270799) |
| C7 | clingo: factor tussen de families toegelaten | clingo-suite | calendar-family factor |
| C8 | clingo: ongebonden parameter niet geweigerd | clingo-suite | parameter-unbound |
| C9 | clingo: kalenderafronding kapt af | anker case 1 | MISMATCH 13917 ≠ 13918 |
| C10 | clingo: overschot van de wortel zonder getuige | clingo-suite | period-arithmetic-misfit |
| C11 | clingo: verminderd met coalesceert links | golden per-dag case 1 | MISMATCH nettoloon |
| V1 | verifier: geen kalenderarm | negatief wf009 | `verifier: PASS` (gate 3 rood) |
| V2 | verifier: tijdlijndoel buiten de poort | negatief wf023-timeline | `verifier: PASS` |
| V3 | verifier: kalendertotaal begrensd | negatief wf023-calendar | `verifier: PASS` |

C1 was eerst groen (het anker en de golden hebben rechts een geheel getal); daarom kwam de fixture
`product-right-scale`, waarna C1 rood werd.

### Codes, floors en pins, oud en nieuw

- Geen nieuwe EF-, REF-, CLINGO-, RGS- of DROOLS-code. Nieuwe arm `WF-009.calendar-family-factor`. In clingo een
  nieuwe viol-reden `non-exact period arithmetic value …` die `viol_code` op EF-006 leest (naast de pro-rata-reden).
- Gate 12 `EXPECTED_CHANNEL`: + 2 regels (DROOLS-105); `EXPECTED_PLAYED` blijft 319. Geen floor veranderd.
- ref-tests die het besluit expliciet omdraait, oud → nieuw:
  - `WV-3.6-g-vreemde-noemer`: 10 €/dg over 2024 op een maandraster geweigerd ("cannot integrate … connect no
    conversion") → **3660 €** (de kalender, 366 dagen). Het RS-voorbeeld met `mnd = 30 dg` (3600 €) wordt dat ook; met
    die factor is het document ill-formed. Een ref-test die 3600 € pinde bestond niet.
  - `caselader-tijdlijn-dagrooster`: een tijdlijn op het dagraster geweigerd (Z14) → laadt.
- Nieuwe ref-voorbeelden: `WV-3.4-per-dag-maal`, `-verminderd-met`, `-gedeeld-door`, `-gedeeld-door-ef031`,
  `-percentage-van`, `-gedeeld-door-abs`, `-quotient`; `WV-3.4-tijdsafhankelijke-parameter-ongebonden` (+ verifier-clean);
  `WV-3.4-schrijfgrens-per-segment` (+ WF-023-weigering); `WV-3.6-kalender`, `-geval-1`, `-jaargrens`, `-maand`,
  `-kwartaal`, `-week-over-maanden`, `-gedeclareerde-factor`, `-onafgerond`. `WV-3.4-domein` bestond (1a). Suites:
  ref 2176 → 2197 voorbeelden, clingo 2401 → 2409 checks.
- Bestaande goldens: gate 5 en 6 spelen alle andere cases onveranderd; geen verwachting van een bestaande golden
  veranderde. De verifier blijft schoon op elke golden en rode casus en op `toka/` op zijn bekende uitzonderingen na.

### Voorgestelde CONSTRUCTS-tekst (rij 11, achteraan; rij 26 voor de arm)

> **Landed (2026-11-03, card timeline-arithmetic-per-period, step A):** every operator of the arithmetic table of
> RS ch. 1.2 but the power over timelines (RS ch. 3.4), read **per day** with its own empty rule, over all time
> (maintainer decision 2026-11-03, `spec/proposals/IMPLICIT-TIME.md` point 1); a division by leeg or 0 in a period
> deposits one §9.5 fact per rule × subject × code; a time-dependent parameter is a timeline operand like a temporal
> attribute (VB 2.3); the precision gate of WF-023 holds on a rule's timeline target per segment, the pro-rata write
> excepted. The temporal total integrates across the day family (dag, week) and the month family (maand, kwartaal,
> jaar) by **the calendar**, the actual length of the calendar unit (point 11); a declared factor between the two
> families is ill-formed (`WF-009.calendar-family-factor`, row 26). Executed by ref and the ASP backend (the latter
> carries the calendar total under a rounding, `backends/clingo/README.md` [V6]); witnesses
> `anchor/jutterswet-verzuimrente-dagen` and `jutterswet-rekenen-per-dag`; the Drools backend refuses both (DROOLS-105)
> until step B. Tracked v1 delta: v0 refuses the four operators over timelines, and integrated a rate across the
> families by a declared factor; v1 follows spec §5.1.1/§5.1.4 and §7.3.1, and ALEF.

### Voorlopige besluiten

- VB-B1 — Het anker rondt de verzuimrente rekenkundig af op de cent (doel 2 decimalen): de ASP-codering houdt geen
  niet-decimale waarde, en elke golden moet in clingo spelen (gate 6 kent geen weigerkanaal). De exacte waarden staan
  in de NOTE en in de ref-suite.
- VB-B2 — Het afdrachtbedrag is een geheel getal: dan heeft het product per dag grens 2 en past het in een dagrente van
  2 decimalen (VB 1.4), en blijven de bereikgrenzen van clingo (46340 per factor) gehouden; 10 000,00 op schaal 100 zou
  de productgrens overschrijden.
- VB-B3 — De families volgen de spellingtabel van ch. 3.8 op de afkorting: dag/week, maand/kwartaal/jaar. De eenheden
  onder de dag horen tot geen familie (geen besluit noemt ze); een eenheid buiten de tabel (`bm`) ook niet en houdt
  haar factor. De arm vergelijkt een eenheid met het einde van haar canonieke keten.
- VB-B4 — De kalender meet per dag in de eenheid van de noemer zelf (bij `jr = 12 mnd` telt een dag 1/365 of 1/366
  jaar, niet 1/12 van 1/len van zijn maand), over de hele rastereenheden van een segment; een segment korter dan een
  rastereenheid telt 0 zoals nu (de kaart `total-over-partial-unit` gaat daarover).
- VB-B5 — "Rood voor EF-031 en EF-005 in een periode" gelezen als rood-eerst op main; de getuigen zijn golden
  `inconsistent`-verwachtingen, want beide zijn §9.5-soorten sinds `zero-division-channel`.
- VB-B6 — Een tijdsafhankelijke parameter zonder tijdlijn in de case is ongebonden: EF-014, in ref en clingo, met de
  zin van een ongebonden scalaire parameter. De dekkingscontrole van ch. 8.4 blijft alleen voor de vintage-lezing.
- VB-B7 — WF-023 op een tijdlijndoel alleen voor een regelversie; een objectcreatie en een defaults-gevolg schrijven
  geen tijdlijn (de uitvoerders weigeren ze).
- VB-B8 — De grens van het kalendertotaal is ∞, behalve een noemer `dg` over een raster van de maandfamilie (hele
  dagen, de grens van de operand).
- VB-B9 — clingo: D is het kleinste gemene veelvoud van de lengtes in de span van de eindpunten, of, met een eigen
  kenmerk als knipvoorwaarde, in de span van zijn perioden (elk fragment ligt daarin); de grens op W is 2^31 − 1 over
  het gewicht van die span; het totaal alleen als basis van een afronding (`calendar_rounding`, voor die basis alleen).
- VB-B10 — De lader leest een tijdlijn op het dagraster met elke grens; week en kwartaal op de kalender blijven
  geweigerd.
- VB-B11 — Het rekenen per periode van clingo schrijft een quotiënt dat op zijn schaal niet geheel is als de getuige
  van de onbegrensde vorm (de hele waarde in zo'n doel), van een begrensd doel (EF-006) of van een geneste deling
  (CLINGO-031), zoals de scalaire deling.

### Wat stap B (Drools) moet weten

- Twee goldens met `EXCLUDE DROOLS-105` in gate 12: `anchor/jutterswet-verzuimrente-dagen` (2 cases) en
  `jutterswet-rekenen-per-dag` (3 cases). Speelt Drools beide, dan `EXPECTED_PLAYED` 319 → 321 (los van wat stap B van
  `timeline-arithmetic-by-rule` met `jutterswet-maandloon` doet) en de twee kanaalregels weg.
- De vormen: `Binary` `times`, `reducedBy`, `dividedBy`, `dividedByAbs`, `percentageOf` over tijdlijnen, per dag met
  de leeg-regel van ch. 1.2; een `ParameterReference` naar een tijdsafhankelijke parameter als operand (zijn tijdlijn
  uit `parameters.<naam>.timeline`, open grenzen); de §9.5-feiten per regel × onderwerp × code, ook bij meer perioden;
  doelen op een dagraster; de geschreven tijdlijn met lege perioden als gat (VB-A2).
- Het totaal: het geknipte totaal (`gedurende de tijd dat hij rentedragend is`) van een `€/jr`-tarief over een
  dagraster met de kalender (nu DROOLS-228), onder een afronding op 2 decimalen; en het ongeknipte totaal van een
  doel van rekenen per periode over een dagraster (het jaarproduct, 227 500 en 552 000).
- De case-lezer van Drools moet een tijdlijn op een dagraster en een parametertijdlijn met open grenzen lezen.
- Gate 17 vergelijkt geen lezingen van deze regels (geen voorwaarde); het knipfilter van het totaal is een filter van
  de aggregatie.

### `make gates`

Groen op `64fb6bdb5`: 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 347 s
(`out/gates/20261103-125620/`). Gemeten: gate 3 600 negatieven verworpen (was 597), 443 op hun arm; gate 5 ref-suite
2197 voorbeelden, gate 5 en 6 1041 cases ref = clingo (was 1036); clingo-suite 2409 checks; gate 8 62 rode cases
ongewijzigd; gate 10 frontendsuite 1472 tests, 181 rgs-goldens byte-gelijk (was 179); gate 12 PASS met beide nieuwe
goldens als `EXCLUDE DROOLS-105`, `EXPECTED_PLAYED` 319; gate 17 vergelijkt 319 goldens, 0 afwijkingen. De kaart
blijft in `doing/` voor stap B.

## Result: stap B (bouwer, 2026-11-03)

Branch `card/timeline-arithmetic-per-period`, gerebased op main `b8fed38a0` (de frontend-fixronde en stap A deel i van
`conditions-per-period`; zonder conflict). Niet gemerged, niet gepusht. Beschermd in deze stap, dus **voorstel**:
`scripts/check-drools.sh`.

### Commits

- `455db41ae` drools: rekenen per dag, de tijdsafhankelijke parameter en de kalender
- `a1196cf58` gate 12: het verzuimrente-anker en rekenen-per-dag spelen (voorstel)
- `910db8fca` tests: de case-lezer houdt een tijdsafhankelijke parameter aan zijn tijdlijn
- `809b52eb2` tests: een percentage per dag neemt de eenheid van zijn rechteroperand
- `dd137f4fb` drools: de kalenderfamilies uit de gedeelde tabel (`TimeCalculation.calendarFamily`, die main na de start
  kreeg)
- het commit van dit Result

### Rood eerst (op `dd515ad1e`, letterlijk; `werk/timeline-arithmetic-per-period-drools/logs/01-red-first.log`, `16-red-witness.log`)

- `anchor/jutterswet-verzuimrente-dagen`, beide takken: `EXCLUDE DROOLS-105: transpiler SKIP: regel: dagrente — a value
  of the time-dependent target 'dagrente' of Afdracht that is neither a time-proportional part, a day count nor the
  period arithmetic of plus, min or a sum per period ('de verzuimrentevoet maal zijn afdrachtbedrag'): this branch does
  not yet carry this form (RUNTIME-SEMANTICS ch. 3.4, 3.5, 3.11); no golden with cases writes one`
- `jutterswet-rekenen-per-dag`, beide takken: dezelfde zin met `regel: loonproduct van een jutter`, `'loonproduct' of
  Jutter` en `('zijn strandloon maal zijn stormtoeslag')`.
- De nieuwe getuige `TmsPerDayArithmeticWitnessTest` op `dd515ad1e`, met alleen dat bestand erbij: alle 8 tests die hij
  toen had rood, de vormen op DROOLS-105 (`('times')`, `('reducedBy')`, `('dividedBy')`), de emmerroute op
  `… the temporal total of a EUR/dg value: the document declares no mnd unit, so this timeline's grid has no unit to
  integrate against (ch. 3.6)` (DROOLS-227). De twee rijen die later kwamen (de case-lezer van de parameter, het
  percentage) zijn niet op `dd515ad1e` gedraaid; mutaties M19–M21 tonen ze rood. `PeriodArithmeticTest` is niet op
  `dd515ad1e` gedraaid; hij roept functies aan die daar niet bestaan (`reduced`, `times`, `parameter`, `weighed`), dus
  compileert daar niet (gevolgtrekking, niet gemeten).

### Na de bouw

- Beide goldens `PLAYED 2` en `PLAYED 3` op beide takken; hun lezingen zijn op beide takken per regel gelijk aan de
  trace van ref (5 casebestanden; vóór en na de rebase gemeten, `logs/05-*`, `26-*`).
- De drie goldens van `conditions-per-period` op main blijven op beide takken geweigerd met hun gepinde code:
  `jutterswet-loon-per-periode` DROOLS-302, `jutterswet-wachtdagen-twee-schrijvers` DROOLS-103, `jutterswet-wachtmaanden`
  DROOLS-105 (gemeten na de rebase).

### Wat er veranderde

- **Case-lezer** (`CaseRunner`): een tijdlijn op het dagraster van de kalender wordt gelezen, elke dag ligt erop
  (`onGrid`; week en kwartaal blijven DROOLS-037, de emmerscalair op een dagraster blijft DROOLS-035, zoals ref met Z15).
  Een tijdsafhankelijke parameter krijgt zijn tijdlijn, gelezen als die van een attribuut op het raster van de
  parameter (open grenzen inbegrepen, `inputFits` per segment), als `List<Period>` op zijn global; een scalair op zo'n
  parameter en een tijdlijn op een scalaire zijn een gebrekkige case, in de woorden van de lader van ref
  (`IllegalArgumentException`).
- **Lowering met truth maintenance** (`TmsTranspiler`, één analyse, beide weergaven): `isPeriodArithmetic` neemt elke
  binaire operator behalve de macht; `periodOperand` schrijft per operator een functie van `PeriodArithmetic`, een
  deling als `Term.Division` met de sonde van de regel (`reads.depositFlag()`), zodat de `Value`-consequentie één
  §9.5-feit per regel × onderwerp met de codes als verzameling deponeert. `parameterOperand` leest een
  tijdsafhankelijke parameter via `Reads.parameterTimeline` (een global van type `List`, de `ParameterState` zoals bij
  een scalaire). Eenheden per knoop, uit de declaraties (`periodUnitOf`, `periodUnits`, `unitCarrier`). Een doel van
  de onbegrensde vorm wordt toegelaten. `calendar` vervangt het eenheidsdeel van `integrable`: het eigen totaal geeft
  de kalendereenheid mee op alle drie de routes (de emmer via `OWN_TOTAL`, de hele tijdlijn en de klip via
  `PERIOD_TOTAL`); `integrable` (de vouw per lid) weigert een tarief dat de kalender integreert bij naam (DROOLS-228).
- **Runtime, zuivere functies** (`PeriodArithmetic`): de waarden tussen operatoren zijn exact (een interne
  `Exact`-record, decimaal of `Breuk`); `reduced`, `times`, `percentageOf`, `divided`, `dividedAbs` (per periode de
  scalaire operator van de runtime, de delingen via `ComputeHelpers.divided`/`dividedAbs` met de sonde), `parameter`
  (EF-014 zonder tijdlijn), `written` (een segment van de onbegrensde vorm dat niet eindigt: `ComputeFault` bij naam),
  `total` met de kalender, `weighed` en `measure` (de kalender: per dag 1/len van zijn maand, kwartaal of jaar, over
  de hele rastereenheden, die `rsTijdTotaal` telt). `ComputeHelpers.ownTotal` weegt de emmer met de kalender.
  `ComputeFault` herkent de EF-014-zin (code en `parameter`). `StaticFunction`: `PERIOD_REDUCED`, `PERIOD_TIMES`,
  `PERIOD_PERCENTAGE_OF`, `PERIOD_DIVIDED`, `PERIOD_DIVIDED_ABS`, `PERIOD_PARAMETER`; `PERIOD_TOTAL` en `OWN_TOTAL`
  kregen het argument van de kalender.
- **Catalogi** (nl, en): de zinnen van DROOLS-105 en DROOLS-303 noemen de operatoren van ch. 1.2 behalve de macht en
  (303) de tijdsafhankelijke parameter.
- **README** van de backend: de alinea over rekenen per periode, een nieuwe alinea "The calendar", de weigeringen.

### Codes en pins, oud en nieuw

- `RETIRED`: DROOLS-030 (`granular-parameter-timeline`), DROOLS-036 (`day-grid-calendar-bucketing`), DROOLS-304
  (`period-arithmetic-time-parameter`), DROOLS-307 (`period-arithmetic-inexact`). Register: 308 codes, 18 retired (was
  14). Geen nieuwe code. DROOLS-105 blijft (de macht, een scalair als hele waarde, elke andere vorm), DROOLS-228 blijft
  (een gedeclareerde factor naar de rastereenheid, ook tussen de families; de kalender in de vouw per lid),
  DROOLS-303, -306 en -308 blijven.
- Gate 12 (voorstel): `EXPECTED_CHANNEL` − `anchor/jutterswet-verzuimrente-dagen|EXCLUDE DROOLS-105|…` en
  − `jutterswet-rekenen-per-dag|EXCLUDE DROOLS-105|…`; `EXPECTED_PLAYED` 320 → 322 met een regel in het
  commentaarblok (op main ook 320; de drie goldens van `conditions-per-period` blijven gepind). Kanaalvloer 52 → 50.
  Gate 8: ongewijzigd. Geen floor gewijzigd; de JUnit-telling 1787 → 1804 (floor 1573).
- Tests die de kaart omdraait, oud → nieuw:
  - `GridSentenceWitnessTest.a_timeline_on_the_week_and_the_day_of_the_calendar`: de dagrij weigerde met de zin van
    DROOLS-036 → gesplitst: `a_timeline_on_the_week_of_the_calendar` (weigert als voorheen) en
    `a_timeline_on_the_day_of_the_calendar_is_read` (een invoer en een verwachting op dagen, beide takken spelen).
  - `CaseInputWitnessTest.parameterValue` (de consument zet de parameters zelf): een parametertijdlijn gaf
    `Excluded(DROOLS-030)` → wordt de `List<Period>` die de case-lezer zet; het anker speelt nu via de ingangen.
  - `TmsPeriodArithmeticWitnessTest.every_form_outside_the_period_arithmetic_it_carries_is_refused_by_name_on_both_branches`:
    rij "another operator below plus" (`times`, DROOLS-303) → een macht onder plus (DROOLS-303, `('power')`); rij
    DROOLS-304 weg (speelt: `TmsPerDayArithmeticWitnessTest` C); rij DROOLS-307 weg (speelt: B; het segment dat niet
    eindigt: E); rij "another operator as the whole value" (`times`, DROOLS-105) → een macht (DROOLS-105, met de nieuwe
    zin).
- Nieuw: `TmsPerDayArithmeticWitnessTest` (10 tests): A maal, verminderd met, percentage van en het totaal over een
  dagraster (24 volgorden × 2 takken); B beide delingen met EF-031 en EF-005 in één feit en een doel van de onbegrensde
  vorm (24 × 2); C een tijdsafhankelijke parameter en de kalender, geknipt en ongeknipt (24 × 2); de kalender op de
  emmerroute, €/dg en €/wk over februari 2024 (2 × 2); het percentage met de eenheid van rechts; D de weigeringen 303,
  105, 306, 228 (vouw per lid) en 228 (`jr = 365 dg`); E EF-014, het segment dat niet eindigt, EF-006 per segment; de
  case-lezer van de parameter (scalair op een tijdsafhankelijke, tijdlijn op een scalaire, EF-004). `PeriodArithmeticTest`
  (6 tests): `WV-3.4-per-dag`, de exacte waarden tussen operatoren, de sonde één keer per code, EF-014, en de kalender
  (`WV-3.6-g-vreemde-noemer` 3660, week-over-maanden 29, maand 31, kwartaal 91, jaargrens 731/365, anker 4500/61 en
  10160/73, een segment korter dan een eenheid 0).
- Gemeten (`logs/22-witness-ref-clingo.log`, documenten uit `TmsPerDayArithmeticWitnessTest.main`): de verifier laat
  alle vijf door, ref geeft op alle vijf de verwachtingen van de getuige; clingo op A ook, en weigert de andere vier bij
  naam (een eenheiddragende vermenigvuldigende operand; een quotiënt dat niet geheel is in de onbegrensde vorm; de
  kalender op de emmerroute; een kalendergewicht van 298981 voorbij het bewaakte bereik).

### Mutaties (niet gecommit; `logs/24-mutations.log`, `28-mutations.log`, `29-mut-readings.log`)

| # | mutatie | uitkomst |
|---|---|---|
| M1 | verminderd met coalesceert links (`rsMin`) | rood: `PeriodArithmeticTest`, getuige A, B, C |
| M2 | deling zonder sonde | rood: B en twee rijen van `PeriodArithmeticTest` |
| M3 | ABS-deling zonder afkapping | rood: B, `PeriodArithmeticTest` |
| M4 | percentage van met omgedraaide operanden | groen: een gelijkwaardige mutant (l/100 × r = r/100 × l); vervangen door M4b |
| M4b | percentage van zonder deling door 100 (`rsMaal`) | rood: A, het percentage, `PeriodArithmeticTest` |
| M5 | waarden tussen operatoren op 16 cijfers | rood: exacte waarden, het segment dat niet eindigt |
| M6 | ongebonden parameter leest leeg | rood: EF-014 (getuige en eenheidstest) |
| M7 | parametertijdlijn niet op de global | rood: C |
| M8 | totaal zonder kalender | rood: C, kalendertests |
| M9 | elk jaar 365 dagen | rood: C (A1 in 2028), kalendertests |
| M10 | kalender nooit gekozen | rood: C, emmerroute |
| M11 | gedeclareerde factor tussen de families niet geweigerd | rood: D |
| M12 | vouw per lid neemt de kalender | rood: D |
| M13 | emmerroute zonder kalender | rood: emmerroute |
| M14 | segment dat niet eindigt zonder fout | rood: E, exacte waarden |
| M15 | dagraster via de kalenderemmer | rood: `GridSentenceWitnessTest`, 5 rijen van de getuige |
| M16 | factor van een product niet geweigerd | rood: D (306) |
| M17 | herrekening genoemd bij de wortel | rood: `TmsPeriodArithmeticWitnessTest` E (306 noemt `'plus'`) |
| M18 | macht als rekenen per periode | rood: D van beide getuigen |
| M19 | parametervorm in de case niet gehouden | rood: de case-lezer van de parameter |
| M20 | parametertijdlijn zonder invoergrens | rood: idem (EF-004) |
| M21 | percentage neemt de eenheid links | rood: het percentage (`UnitConflict`) |
| M22 | week in de maandfamilie (eigen tabel, vóór de rebase) | rood: emmerroute |
| M22b | noemer per week niet door de kalender (na de rebase) | rood: emmerroute |
| G1 | maal en gedeeld door niet als onbereikt genoteerd | `--readings` op `jutterswet-rekenen-per-dag`: `ERROR IllegalStateException: de lezing van /rules/rules/0/versions/0: de analyse noteerde geen term voor knoop 1 (…)` (gate 17 rood) |

### Voorgestelde CONSTRUCTS-tekst (rij 11, in de zin van stap A)

"the Drools backend refuses both (DROOLS-105) until step B" →

> since step B (2026-11-03) both branches of the Drools backend carry them too (`PeriodArithmetic`: the values between
> two operators exact, a time-dependent parameter's timeline on its global as a `List<Period>`, the calendar on every
> route of the subject's own total and refused by name in the per-element fold over a set, DROOLS-228)

### Voorgestelde tekst elders (beschermd of buiten mijn may-touch, voor de hoofdsessie)

- `CLAUDE.md`, gate 12, na de zin over `timeline-arithmetic-by-rule`: "Since the card `timeline-arithmetic-per-period`
  (step B, 2026-11-03) the lowering reads every operator of ch. 1.2 but the power per day over timelines, a
  time-dependent parameter as a timeline operand (the case runner sets its timeline as a `List<Period>` on its
  global), and the temporal total by the calendar between the day and the month family; the case reader reads a
  timeline on the day grid." In de lijst van `RETIRED`: "since the card `timeline-arithmetic-per-period` (2026-11-03)
  DROOLS-030, DROOLS-036, DROOLS-304 and DROOLS-307".
- RS ch. 3.6, laatste zin van de executor note: "The Drools backend refuses the calendar integration by name
  (DROOLS-228)." → "Both Drools branches integrate by the calendar on every route of the subject's own total (the
  asOfDate bucket, a timeline the period arithmetic writes, the clip) and refuse it by name in the per-element fold
  over a set (DROOLS-228)."
- RS ch. 3.11, opsommingspunt van het rekenen per periode: "Executed by `ref` and the ASP backend, plus, min and the
  sum per period without a condition also by both branches of the Drools backend" → "Executed by `ref` and the ASP
  backend, and without a condition by both branches of the Drools backend".
- RS ch. 3.12, rij "Timeline arithmetic beyond plus/minus": "both Drools branches refuse it by name (DROOLS-105)" →
  "and both Drools branches". Rij "The temporal total across the day and the month family": "both Drools branches
  refuse it by name (DROOLS-228)" → "both Drools branches on every route of the subject's own total, refusing it by
  name in the per-element fold (DROOLS-228)".
- RS ch. 2.4, de alinea over de onbegrensde vorm in Drools, na "a time-proportional part into a timeline of that form
  (ch. 3.7)": "; a segment of the period arithmetic of ch. 3.4 into such a timeline whose value does not terminate
  faults by name".
- `corpus/golden/anchor/jutterswet-verzuimrente-dagen/NOTE.md` (beschermd) en `corpus/golden/jutterswet-rekenen-per-dag/NOTE.md`,
  "Executors": "The Drools backend refuses … (DROOLS-105; gate 12's channel list) …" → "`ref`, the ASP backend and both
  branches of the Drools backend play both cases" (resp. "the three cases").

### Voorlopige besluiten

- VB-C1 — De waarden tussen twee operatoren zijn exact (een interne record met een decimaal of een `Breuk`); alleen de
  geschreven tijdlijn houdt decimalen, de segmentklasse `Period`. Zo geeft `(a gedeeld door 3) maal 3` in Drools wat
  ref geeft.
- VB-C2 — Een tijdlijndoel van de onbegrensde vorm wordt toegelaten; een segment dat niet eindigt faalt bij naam
  (`ComputeFault` zonder EF-code), een grens van deze tak, zoals clingo een niet-gehele waarde in zo'n doel bij naam
  weigert (VB-B11 van stap A). DROOLS-307 is daarmee zonder plek.
- VB-C3 — Eenheden zonder herrekening, per knoop uit de declaraties (nooit de annotatie, ch. 2.10): de operanden van
  een additieve operator in één eenheid, geen factor tussen de operanden van een product of quotiënt, het resultaat
  in de eenheid van het doel; anders DROOLS-306, met de operand wiens eenheid het resultaat draagt in de zin. Een
  percentage neemt de eenheid van zijn rechteroperand (ch. 2.6, ref `pct_of`).
- VB-C4 — Een tijdsafhankelijke parameter: de case-lezer zet zijn tijdlijn als `List<Period>` op zijn global, gelezen op
  het raster van de parameter; een parameter die de case niet bindt, faalt bij het lezen als operand met EF-014 en de
  zin van ref. De scalaire lezing van zo'n parameter blijft DROOLS-269. Een scalaire parameter die de case niet bindt,
  leest nog leeg (de bestaande `NOFAULT`-regel van gate 8, buiten deze kaart).
- VB-C5 — De kalender op alle drie de routes van het eigen totaal; in de vouw per lid bij naam geweigerd (DROOLS-228,
  geen golden). Zoals ref eerst de gedeclareerde factor naar de rastereenheid zoekt: een noemer van de klasse van de
  rastereenheid blijft DROOLS-228, ook tussen de families (WF-009); de kalender vraagt geen gedeclareerde
  rastereenheid, DROOLS-227 alleen waar ook de kalender niet integreert.
- VB-C6 — De dagrastercontrole van de case-lezer: elke dag ligt erop; week en kwartaal en de emmerscalair op een
  dagraster blijven geweigerd zoals in ref.

### Open punten

1. **ref geeft een gecoalesceerde lege operand van maal geen eenheid** (`Arith.coalesce0` → `Unitless`): per periode
   krijgt het product van een eenheiddragende parameter en een tijdlijn met een gat in dat gat de eenheid van één
   operand, en het schrijven of het totaal faalt. Gemeten op een eerste versie van getuige C (dagrente = de rentevoet
   maal een `bedrag` in € als tijdlijn met gaten): ref `EVALUATION FAULT: incompatible units: EUR/jr vs 1/jr`
   (`logs/10-witness-ref-clingo.log`); Drools rekent de statische eenheid en geeft 73,77 en 13,66. Getuige C gebruikt
   daarom een scalair afdrachtbedrag, zoals het anker. Een kaart voor ref (en clingo nagaan); of de scalaire maal met
   een lege operand hetzelfde doet, is niet gemeten.
2. De distributie (`regelspraak-toolchain`) zet de globals van parameters zelf: een tijdsafhankelijke parameter is nu
   een `List<Period>` (open grens `Period.OPEN_FROM`/`OPEN_TO`). Bij de pin-bump nalopen.
3. De kalender in de vouw per lid over een verzameling: ref draagt hem, Drools weigert (DROOLS-228); geen golden.
4. De tekstvoorstellen hierboven zijn niet aangebracht.

### `make gates`

Groen op `dd137f4fb` (gerebased op `b8fed38a0`): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 370 s
(`out/gates/20261103-140648/`):

```
01-self-contained rc=0 2s     02-ir-schema rc=0 202s   04-case-schema rc=0 7s   03-verifier rc=0 38s
05-oracle rc=0 24s            06-clingo rc=0 300s      07-noop rc=0 13s         08-red rc=0 141s
09-catala rc=0 6s             10-regelspraak rc=0 250s 11-toka-parity rc=0 255s 12-drools rc=0 365s
13-reftype rc=0 301s          14-flogic rc=0 2s        15-graph rc=0 47s        16-scenario rc=0 39s
17-readings rc=0 246s         after-run rc=0 1s        end 2026-11-03 14:12:58 370s red=0
```

Gemeten: gate 12 `played matches the pinned 322`, de DRL-tekst-tak 322, kanaalvloer 50 met naam, JUnit 1804 tests
(nul skips, floor 1573); gate 8 Drools-been 44 fault-gepind, 19 niet-gedragen, 17 EXCLUDE met code (`check-red.sh` in
deze stap niet aangeraakt); gate 17 322 goldens en 911 cases op beide takken vergeleken, 0 afwijkingen; `check-drools-codes` 308 codes, 18
retired. De kaart blijft in `doing/`.
