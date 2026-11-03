# conditions-per-period: voorwaarden per periode en de dagentelling (impliciete tijd, fase 1c en 2c)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` en het voorstel
`spec/proposals/IMPLICIT-TIME.md`, punt 3 tot en met 8. Na `20261103-timeline-arithmetic-per-period`. Besluiten van de eigenaar
2026-11-03: **per emmer** (4): op een regel met een tijdsafhankelijk doel is de gehele-periodekwalificator een
waarheidstijdlijn per emmer van zijn raster, de vouw blijft voor een scalair doel; **0** (7a): de dagentelling geeft 0 in een
emmer zonder ware dag; alle tijd (2). Besloten zonder normvraag: 3, 5, 6, 7b, 7c, 8. De kaart mag in tweeën: 1c-i (3, 4, 5, 8)
en 1c-ii (6, 7).*

## Stap A — spec, ref, clingo, verifier, frontend

Goal: een voorwaarde op een regel met een tijdsafhankelijk doel geldt per periode (de veralgemeende knip van ch. 3.10;
RegelSpraak §10.3), de kwalificator daarin per emmer, twee schrijvers van één tijdlijndoel per dag (fout alleen op een dag waar
beide beslissen en verschillen), een tijdsafhankelijk kenmerk binnen een venster met een dagprecieze datumexpressie als grens, de
dagentelling met 0 in een lege emmer, een dagsoorttoets op de getelde dag en dagen per rastereenheid als eenheid, en een fijnere
operand statisch geweigerd; in ref, clingo, de verifier en de frontend, met formulering A van de ankercasus als anker.

Acceptance:
- (a) Rood eerst: anker-golden `corpus/golden/anchor/jutterswet-verzuimrente-30-360` (beschermd: voorstel) met geval 1 en 2 van
  formulering A (IMPLICIT-TIME §4): rentedragend door de regel van datum tot datum, de twee schrijvers van de rentedagen, de
  maandrente, het totaal; per maand 16 2/3, 33 1/3, 33 1/3, 41 2/3, 16 2/3 en 25, 41 2/3, 5 5/9; totaal **141 2/3 €** en
  **72 2/9 €**. Op main de weigeringen van IMPLICIT-TIME §5 (frontend Z1, Z2; verifier Z3; ref Z4; clingo Z8).
- (b) De frontend schrijft "van zijn vervaldatum plus 1 dg tot en met zijn betaaldatum" (RGS-202 verruimd), crasht niet op een
  variabele met een samengestelde dagentelling (`IrEmitter.unknownType`, `IrEmitter.java:761`; document
  `werk/implicit-time/meting/zoek-crash/`), en schrijft de samengestelde voorwaarde met dagsoorttoets als filter in plaats van
  `Unresolved` (Z17).
- (c) Verifier (voorstel): WF-111 verruimd (VB 6.1), WF-058 verruimd (7b, 7c), een nieuwe WF-regel voor het raster van operand en
  voorwaarde (VB 8.1; nu laten frontend en verifier een operand per dag in een maanddoel door, `forms/p08`); een negatief per
  arm.
- (d) RS ch. 3.3, 3.8, 3.9, 3.10, 3.11, 3.12 en 5.7 (voorstel); de verwachtingen van de goldens met een dagentelling naar 7a: het
  anker `corpus/golden/anchor/jutterswet-dagentelling` (beschermd: voorstel), `jutterswet-dagentelling-rgs`,
  `jutterswet-vast-startpunt` en `jutterswet-pachtvenster`, elk met oud en nieuw in het Result; de rode casus
  `time-window-timeline-characteristic` wordt een golden.
- (e) clingo weigert of draagt een tijdsafhankelijk kenmerk in de voorwaarde van een regel met een tijdsafhankelijk doel, nooit
  stil een lege tijdlijn (nu wel, `forms/p03b`); ref schrijft geen dagentelling zonder eenheid in een doel met eenheid dg/mnd
  (nu wel, `forms/p07b`; ch. 2.6 regel 6).
- (f) De `ref/test`-rijen `WV-3.10-voorwaarde-per-periode`, `WV-3.9-per-emmer`, `WV-5.7-per-dag`, `WV-3.10-kenmerk-venster`,
  `WV-3.8-nul-en-dagsoort` en `WV-3.4-fijner-geweigerd` van het voorstel letterlijk.
- (g) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `spec/verifier/` (voorstel), `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`,
`spec/ASP-TRANSLATION.md` (voorstel), `corpus/golden/anchor/` (voorstel), de goldens met een dagentelling (verwachtingen),
`corpus/negative/<nieuw>/`, `corpus/red/` (de overgang van `time-window-timeline-characteristic`), `frontends/regelspraak/**`,
`scripts/check-drools.sh`, `scripts/check-red.sh` (voorstel), kaartverplaatsing.

Out of scope: het totaal over een gedeeltelijke eenheid (`20261103-total-over-partial-unit`); verschuiven en eenmalig (punt 10);
tijdsafhankelijke feittypen (punt 13).

## Stap B — Drools (direct na de landing van stap A)

Acceptance: (a) rood eerst: de goldens van stap A geven op main DROOLS-102, DROOLS-103, DROOLS-114, DROOLS-271 of de vouw van
Z13 (IMPLICIT-TIME §5); (b) `PLAYED` op beide branches, `EXPECTED_PLAYED` omhoog, kanaalpins weg (voorstel), codes zonder plek
`RETIRED`; de goldens met een dagentelling spelen met hun nieuwe verwachtingen; (c) gate 17 gelijk, een getuige per nieuwe vorm in
elke regelvolgorde; (d) `make gates` groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (voorstel), kaartverplaatsing.

## Omvang

Groot (stap A, eventueel in tweeën), middel tot groot (stap B).

## Result: stap A deel i (bouwer, 2026-11-03)

Deel 1c-i: de punten 3, 4, 5 en 8 van `spec/proposals/IMPLICIT-TIME.md`. Branch `card/conditions-per-period`, gebouwd op
main `02be64433` en gerebased op main `4d3de105c` (stap B van `timeline-arithmetic-by-rule` en de frontend-fixronde; twee
conflicten in de kanaallijsten van gate 8 en 12, een in RS ch. 3.11/3.12 en een in de frontend, zie "Codes"). Niet gemerged,
niet gepusht. Beschermde paden, dus **voorstel**: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/verifier/`,
`scripts/check-drools.sh`, `scripts/check-red.sh`; `spec/ASP-TRANSLATION.md` onder het CODEOWNERS-net. Proefdocumenten,
scripts en logs onder `/Users/yeb/Developer/uchar-dev/werk/conditions-per-period/` (`probe/`, `scripts/`, `logs/`).

### Commits

- `96a093db5` kaart naar `doing/`
- `2b9758971` verifier: WF-126 met twee armen, `Checks.grid_fits`; negatieven `wf126-value-grid`, `wf126-condition-grid`,
  `wf126-whole-period-grid`
- `3131540a1` ref: voorwaarden per periode, de kwalificator per emmer, twee schrijvers per dag, een scalair als hele waarde,
  de lezing van ch. 5.12 voor deze vormen; fixtures `ref/test/fixtures/conditions-per-period/`
- `ab8d3b7a6` clingo: dezelfde vormen (`period_condition`, `period_cuts`, `wtl/6`), README [V7] en [V8]
- `56c7377de` corpus: drie goldens, een rode casus, kanaalregels gate 12 en een uitsluiting gate 8
- `43314b669` frontend: RGS-374 (`FinerTimelineTest`)
- `1305f826f` spec: RS ch. 3.3, 3.4, 3.9, 3.10, 3.11, 3.12, 5.7, 5.12; IR-SPEC WF-126 en de indexrij; ASP-TRANSLATION S9;
  ref weigert het venster als voorwaarde van rekenen per periode, zoals clingo
- `aa4f42b12` gate 12: DROOLS-302 na de rebase (stap B van `timeline-arithmetic-by-rule` geeft die code voor de vorm)
- het commit van dit Result

### De meting op main `02be64433` (letterlijk; `logs/04-red-first-base.log`, `logs/06-red-first-negatives.log`, `logs/10-frontend-red.log`)

- `jutterswet-loon-per-periode`, alle drie de cases, ref: `EVALUATION FAULT: rule "aangesteld loon van een jutter" writes the
  temporal attribute "aangestelde loon" by the period arithmetic of ch. 3.4/3.5 under a condition: a condition per period is
  not executed`; clingo hetzelfde als `TRANSLATION FAULT`.
- `jutterswet-wachtmaanden`, alle drie, ref: `EVALUATION FAULT: temporal attribute "wachtpremie" as a target: temporal writes
  are not implemented`; clingo: `TRANSLATION FAULT: CLINGO-024: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8
  day-count shape (…) or the period arithmetic of ch. 3.4/3.5; no corpus case for more`.
- `jutterswet-wachtdagen-twee-schrijvers`, alle drie, ref: `EVALUATION FAULT: temporal attribute "wachtdagen" as a target:
  temporal writes are not implemented`; clingo: `TRANSLATION FAULT: two computing rules write the temporal target
  "wachtdagen", and a divergence between their timelines is not translated (no corpus case)`.
- Rode casus `jutterswet-wachtdagen-botsing`: in beide `EXPECTED FAULT MISMATCH: wanted EF-016 {…}` met de zinnen hierboven.
- De drie negatieven: `verifier: PASS`. `FinerTimelineTest`: 3 van 4 rood (`one ERROR expected, got []`); de vierde (wat
  moet passeren) groen.
- De vormen van IMPLICIT-TIME §5 opnieuw gemeten op de branch (`logs/17-meting-forms.log`): `p03b` en `p07c` spelen nu in ref
  en clingo (Z10, Z11 en Z13 weg; **bevinding 3 is opgelost**: clingo schrijft de vorm, geen stille lege tijdlijn); `p08`
  geeft WF-126 in de verifier en in ref en clingo de fail-fast-zin, en de frontend RGS-374 (bevinding 4 opgelost). `p04` en
  `p05` verschillen van hun verwachting in `werk/implicit-time/`: die splitst de 30 per maand, het voorstel (WV-5.7-per-dag)
  geeft één segment [2025-12-01, 2026-03-01), en ref en clingo volgen het voorstel; `p05` vraagt verder 7c (deel 1c-ii).

### Wat er veranderde

- **ref** (`eval.ml`, `timeline.ml`, `world.ml`, `reading.ml`). `Eval.reads_timeline` (statisch, ook door clingo gebruikt):
  leest een voorwaarde een tijdlijn (een tijdsafhankelijk kenmerk of attribuut als operand, een tijdsafhankelijke parameter,
  een kwalificator). Zo ja, dan leest `run_rule` haar op een tijdlijndoel per periode: `Eval.cond_periods` geeft de maximale
  ware perioden over alle tijd (kenmerk, ontkenning als complement, vergelijking per dag met ch. 1.2, kwalificator per emmer
  via `Timeline.whole_buckets`, samenstelling per dag met elke kwantor, een knoop zonder tijdlijn op alle tijd of nergens), en
  `clip_periods` knipt het resultaat van elke producent (pro rata, dagentelling, rekenen per periode). Zonder tijdlijn blijft
  het de poort. `is_period_arithmetic` laat een operand ook als hele waarde toe (een scalair, een eigen tijdsafhankelijk
  attribuut, een parameter). Twee of meer schrijvers (`timeline_writers`): `World.write_timeline_part` /
  `release_timeline_part` houden per schrijver zijn segmenten, de tijdlijn is hun vereniging plus de vorige waarde op de dagen
  zonder schrijver; `write_produced` geeft EF-016 op de eerste dag van een botsing (parameters `rules`, `values`, `attribute`,
  `label`, `objectType`, regels in documentvolgorde). `refuse_finer` is WF-126 als fail-fast. De lezing: `Reading.read_periods`.
- **clingo** (`translate.ml`). `period_condition`: één knipverzameling per onderwerp (`#inf`, `#sup`, grenzen van elke
  gelezen `ktl/4` en `tl/5`, wanden van de emmers van elke kwalificator), per knoop een predikaat op het periodebegin, de
  maximale reeksen als `pcN(S,F,T)`; `period_cuts` knipt de segmenten (vier gevallen, ook in `write_segment` en in
  `translate_period_arithmetic`). Een poort zonder tijdlijn in de `paN_per`-regel. `writer_timeline_conflicts`: `wtl/6` per
  schrijver, EF-016 in de zin van de scalaire schrijversfout. `refuse_open_whole_period` en `period_bounds_not_case` kijken niet
  meer naar een kenmerktoekenning, en de tweede noemt een schrijver onder een voorwaarde per periode.
- **verifier**: WF-126 (`WF-126.value-grid`, `WF-126.condition-grid`, parameters `operand`, `operandGrid`, `target`,
  `targetGrid`). Over alle goldens, rode casussen en `toka/` gemeten: geen treffer buiten de drie negatieven.
- **frontend**: RGS-374 in `RuleChecker.gridFit`, op de granulariteiten (een startpunt buiten de kalender laat ze aan de
  verifier).
- **spec** (voorstel): zie de commit hierboven.

### Goldens, rode casus, negatieven

| | cases | inhoud |
|---|---|---|
| `corpus/golden/jutterswet-loon-per-periode` | 3 (24 verwachtingen) | kenmerk, ontkenning, poort, vergelijking per dag, samenstelling, scalair als waarde, dagentelling; case 3 twee aangrenzende perioden als één |
| `corpus/golden/jutterswet-wachtmaanden` | 3 (12) | maandkwalificator per emmer, beide polariteiten, en een jaarkwalificator op een vergelijking |
| `corpus/golden/jutterswet-wachtdagen-twee-schrijvers` | 3 (6) | formulering A zonder eenheden en zonder schrikkeldagtoets: 15, 30 [dec, mrt), 12; 19, 30, 4; 10, 27, 2 |
| `corpus/red/jutterswet-wachtdagen-botsing` | 1 | EF-016 op december 2025 (30 en 31) |
| `corpus/negative/wf126-*` | 3 | waarde (dagparameter in maanddoel), voorwaarde (dagkenmerk), kwalificator (maand in jaardoel) |

Alle verwachtingen met de hand gerekend vóór een uitvoerder ze zag; ref en clingo spelen alles, gate 10 vindt de drie
goldens byte-gelijk.

### Mutaties (niet gecommit; `scripts/mutate.py` op een losse worktree, `logs/13-mutations.log`, `logs/14-mutations-2.log`)

| # | mutatie | getuige | uitkomst |
|---|---|---|---|
| M1 | ref: ontkend kenmerk zonder complement | loon case 1 | MISMATCH vrije loon |
| M2 | ref: kwalificator houdt de perioden (geen emmers) | wachtmaanden case 1 | MISMATCH, 3 doelen |
| M3 | ref: voorwaarde per periode als poort | loon case 1 | de weigering van ch. 3.3 |
| M4 | ref: geen botsing tussen schrijvers | rode casus | EXPECTED FAULT DID NOT OCCUR |
| M5 | ref: gedeelde dag in beide segmenten | ref-suite | `WV-5.7-per-dag-gelijk`, `-invoer` |
| M6 | ref: aangrenzende perioden niet samengevoegd | loon case 3 | MISMATCH, 3 doelen |
| M7 | ref: geen rastertoets | ref-suite | `WV-3.4-fijner-geweigerd` |
| M8 | ref: lezing via de poort | ref-suite | `WV-5.12-voorwaarde-per-periode` |
| C1 | clingo: geen maximale reeksen | loon case 3 | MISMATCH |
| C2 | clingo: een emmer faalt nooit | wachtmaanden case 1 | MISMATCH, 3 doelen |
| C3 | clingo: geen botsing tussen schrijvers | rode casus | clingo found a model |
| C4 | clingo: de poort zonder tijdlijn weg | loon case 2 | MISMATCH erkende loon |
| V1 | verifier: WF-126 niet aangeroepen | `wf126-condition-grid` | `verifier: PASS` |

De frontendbewaking was rood vóór de bouw (zie de meting).

### Codes, floors en pins, oud en nieuw

- Nieuw: WF-126 (twee armen); RGS-374 `timeline-grid-finer` (ERROR; parameters `operand`, `operandGrid`, `target`,
  `targetGrid`). Bij de rebase was RGS-373 bezet door de fixronde (`calendar-family-factor`): mijn code werd RGS-374 in
  register, catalogi, `RuleChecker`, test en commitboodschap. Geen nieuwe EF-, REF-, CLINGO- of DROOLS-code; clingo README
  [V7] (twee schrijvers op één dag met één waarde) en [V8] (het totaal van een tijdlijn onder een voorwaarde per periode).
- Gate 12 `EXPECTED_CHANNEL` (voorstel): + `jutterswet-loon-per-periode|EXCLUDE DROOLS-302`,
  `jutterswet-wachtdagen-twee-schrijvers|EXCLUDE DROOLS-103`, `jutterswet-wachtmaanden|EXCLUDE DROOLS-105`; `EXPECTED_PLAYED`
  blijft 320. Gate 8, Drools-uitsluitingen: + `jutterswet-wachtdagen-botsing|EXCLUDE DROOLS-103`. Geen floor veranderd.
- ref-test die de kaart expliciet omdraait: `WV-3.11-condition` (de fixture `period-arithmetic/condition`, 5 plus strandloon
  onder "zijn leeftijd groter dan 18"): oud een fout "a condition per period is not executed", nieuw speelt hij als poort met
  de verwachting (−∞, 2024-01-01) = 5, [2024-01-01, 2025-01-01) = 15, [2025-01-01, +∞) = 5 (de `case.json` van de fixture
  kreeg die verwachting naast `consistent`). Dezelfde fixture in de clingo-suite: oud een weigering, nieuw `ref_matches` en
  `clingo_matches`.
- Nieuwe ref-voorbeelden: `WV-3.9-per-emmer`, `-negatie`, `-open`; `WV-3.10-voorwaarde-per-periode`;
  `WV-5.7-per-dag-botsing` (+ verifier-clean), `-gelijk`, `-invoer`, `-invoer-een-schrijver`, `-invoer-geen-schrijver`;
  `WV-3.4-fijner-geweigerd` (+ WF-126); `WV-3.6-totaal-onder-voorwaarde`; `WV-5.12-voorwaarde-per-periode`. Suites: ref 2197 →
  2214, clingo 2409 → 2418, frontend + 4 (`FinerTimelineTest`).
- Bestaande goldens en hun verwachtingen: geen verandering (gate 5 en 6 spelen 1050 cases ref = clingo).

### Voorgestelde CONSTRUCTS-tekst (rij 11, achteraan; rij 26 voor WF-126)

> **Landed (2026-11-03, card conditions-per-period, step A part i):** on a rule with a temporal attribute target, a
> condition that reads a timeline restricts the written timeline to the periods of all time in which it holds — ch. 3.10's
> clip generalised (maintainer decision 2026-11-03, `spec/proposals/IMPLICIT-TIME.md` point 3, RegelSpraak §10.3); a
> condition that reads none gates the rule as before. The whole-period qualifier in such a condition is a truth per bucket
> of its grid (point 4); on a scalar target it still folds. Two writers of one timeline target each decide their own
> segments, read per day, and fault (EF-016) only on a day both decide with different values (point 5). A scalar is a
> timeline over all time as a whole value too. A timeline read per period whose grid does not fit the target's is
> ill-formed (`WF-126`, point 8; RegelSpraak typeringen §3). Executed by ref and the ASP backend; witnesses
> `jutterswet-loon-per-periode`, `jutterswet-wachtmaanden`, `jutterswet-wachtdagen-twee-schrijvers`, red case
> `jutterswet-wachtdagen-botsing`, negatives `wf126-*`; the Drools backend refuses the forms (DROOLS-302, -105, -103) until
> step B. Tracked v1 delta: v0 gates a temporal write on one evaluation of its condition, folds the qualifier for every
> target, refuses two timeline writers, and checks no grid.

### Voorlopige besluiten

- VB-C1 — Een voorwaarde wordt per periode gelezen als ze een tijdlijn leest (`Eval.reads_timeline`); een voorwaarde zonder
  tijdlijn blijft de poort van ch. 3.3, ook in de vorm "gedurende de tijd dat". Zo blijft het verschil tussen een afwezige en
  een lege tijdlijn (ch. 3.6, OPEN) waar het was en verandert geen golden.
- VB-C2 — De perioden van een voorwaarde zijn maximaal: twee aangrenzende ware perioden zijn één, dus een segment wordt alleen
  geknipt waar de voorwaarde verandert (golden case 3: [2024-03-01, 2024-05-01) = 25 in één segment).
- VB-C3 — In een samenstelling per periode wordt elk lid gelezen, zonder kortsluiting (ch. 5.10 ordent de leden van één
  oordeel; hier heeft elke dag het zijne); elke kwantor telt per dag.
- VB-C4 — De kwalificator per emmer beoordeelt elke emmer van alle tijd: waar als zijn toets op elke dag geldt (ook gatdagen,
  ch. 1.2), zijn ontkenning in elke andere emmer, ook in een emmer zonder ware dag; "geraakt" doet niet mee.
- VB-C5 — Vormen die ref en clingo bij naam weigeren in een voorwaarde per periode: een genavigeerd of gehost kenmerk, een
  vergelijking van twee tijdlijnen, een leeg-/gevuldtoets, een kwantorvouw, een venster als lid; het venster als voorwaarde
  van rekenen per periode (ook ref nu, zoals clingo). Geen bron of golden vraagt ze.
- VB-C6 — Twee schrijvers: per schrijver zijn segmenten zoals hij ze schreef (geen samenvoeging over schrijvers); een gedeelde
  dag met één waarde ligt in het segment van de schrijver die het document eerst noemt; op een dag zonder schrijver staat wat
  het doel vóór de schrijvers had (de invoer). Eén schrijver schrijft zijn hele tijdlijn zoals eerst. clingo weigert de
  gedeelde dag ([V7]) en, zoals eerst, invoer op zo'n doel.
- VB-C7 — EF-016 op een tijdlijndoel: de regels in documentvolgorde en hun waarden op de eerste dag waarop ze verschillen; de
  fout staat bij de schrijver die als tweede schrijft, dus in elke volgorde dezelfde parameters. clingo noemt `values` niet; de
  rode casus pint de vier gemeenschappelijke parameters.
- VB-C8 — WF-126 vergelijkt rasters: elk raster past in het dagraster; op de kalender een maand-, kwartaal- of jaarraster in
  een doel van die familie waarvan de eenheid de zijne deelt; andere paren alleen als ze gelijk zijn (granulariteit en
  start). Vrij: de producenten met een eigen raster en de operand van de kwalificator; de kwalificator zelf telt met zijn
  maand of jaar. De frontend oordeelt op de granulariteiten alleen.
- VB-C9 — Een scalair is ook als hele waarde van een tijdlijndoel toegelaten (de tijdlijn (−∞, +∞), geknipt door de
  voorwaarde), met en zonder voorwaarde; zonder voorwaarde geeft een totaal erover EF-036.
- VB-C10 — De lezing (ch. 5.12) van een voorwaarde per periode: de vorm geldt (HOLDS); elke knoop heeft het deel van alle tijd
  waarin hij geldt (perioden zonder waarde), een vergelijking de segmenten van haar tijdlijn daar met waarden en de gaten waar
  een lege kant voldoet; de wortel van de waarde de geschreven tijdlijn, de knopen eronder hun gelezen of berekende tijdlijn,
  een scalair zijn waarde. Voor rekenen per periode met een poort (een voorwaarde zonder tijdlijn) leest ref de waarde nu ook
  zo; rekenen per periode zonder voorwaarde leest ref ongewijzigd via de pro-rata-arm (de knopen `unreached`).

### Wat deel 1c-ii moet weten

- 7a (0 in een emmer zonder ware dag) verandert `WV-3.9-per-emmer` (nu 15 en 12; met 7a ook 0 tot 2025-11-01 en vanaf
  2026-04-01), de toetsdagen in de fixture `afdracht`, en in `jutterswet-wachtmaanden` en `jutterswet-wachtdagen-twee-schrijvers`
  de dagentelling (open segmenten van 0 en 0 in elke maand zonder ware dag). Met 7a overlapt de dagentelling niet met een
  tweede schrijver in een gehele maand zolang zijn voorwaarde "niet gedurende de gehele maand" is; een 0 in een gehele maand
  zou wel botsen met 30.
- Een kenmerkdoel binnen een venster en met een voorwaarde per periode (punt 6) staat nog geweigerd (EF-023), net als het
  venster als lid van een samenstelling op een tijdlijndoel en als voorwaarde van rekenen per periode; `cond_periods` en
  `period_condition` dragen het lid "venster" niet. Een venster is een voorwaarde met de perioden [van, tot); de bouwsteen is er.
- De dagsoorttoets in de voorwaarde van een dagentelling (7b) en de eenheid dg/mnd (7c) zijn niet aangeraakt; `p05` laat zien
  dat formulering A met eenheden nog WF-023 geeft.

### Wat stap B (Drools) moet weten

- Drie goldens en een rode casus in de kanaallijsten: `jutterswet-loon-per-periode` (DROOLS-302, de vorm met voorwaarde),
  `jutterswet-wachtmaanden` (DROOLS-105, een scalair als waarde), `jutterswet-wachtdagen-twee-schrijvers` en
  `jutterswet-wachtdagen-botsing` (DROOLS-103). Speelt Drools de drie, dan `EXPECTED_PLAYED` 320 → 323.
- De vormen: een voorwaarde per periode op elke producent (perioden maximaal, VB-C2), per dag met ch. 1.2, de kwalificator per
  emmer (VB-C4), samenstellingen met elke kwantor zonder kortsluiting (VB-C3), een scalair als hele waarde, twee schrijvers per
  dag (VB-C6, VB-C7; de verwachte segmentering is die van elke schrijver), WF-126 als fail-fast.
- **De lezing (gate 17)**, op verzoek van de hoofdsessie: voor een regel met een voorwaarde per periode leest ref de
  voorwaarde zoals VB-C10 zegt. Drools moet daarvoor (1) de status HOLDS geven, ook waar de voorwaarde nergens geldt; (2) op
  elke voorwaardeknoop de perioden van alle tijd waarin hij geldt (een open grens als `-`), op een vergelijking haar segmenten
  met waarden plus de gaten waar een lege kant voldoet; (3) op de wortel van de waarde de geschreven, geknipte tijdlijn en op
  de knopen eronder hun tijdlijnen (operanden heel gelezen, een `Binary` zijn berekende tijdlijn vóór de knip) en scalairen
  hun waarde. De getuige is `WV-5.12-voorwaarde-per-periode` (alle regels van de fixture `afdracht`) en de trace van de drie
  goldens (`runner --trace`). Voor rekenen per periode met een voorwaarde zonder tijdlijn leest ref de waarde nu ook zo
  (`jutterswet-loon-per-periode`, regel `erkend loon`); zonder voorwaarde blijft `unreached` (ongewijzigd, zoals Drools nu).

### `make gates`

Groen op `aa4f42b12` (gerebased op `4d3de105c`): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 344 s
(`out/gates/20261103-135429/`, `logs/gates-1.log`). Gemeten: gate 3 603 negatieven verworpen (was 600), 446 op hun arm;
gate 5 ref-suite 2214, gate 5 en 6 1050 cases ref = clingo (was 1041), clingo-suite 2418; gate 8 63 rode cases (was 62), 19
niet-gedragen in de Drools-poot; gate 10 1483 tests, 184 rgs-goldens byte-gelijk, 364 RGS-codes; gate 12 PASS, played 320 op
beide takken; gate 17 320 goldens en 906 case files vergeleken, 0 afwijkingen. De kaart blijft in `doing/`.

## Result: stap A deel ii (bouwer, 2026-11-03)

Deel 1c-ii: de punten 6 en 7 (7a, 7b, 7c) van `spec/proposals/IMPLICIT-TIME.md` en het anker formulering A. Branch
`card/conditions-per-period`, gebouwd op main `b8fed38a0`, gerebased op `1d7259100` (stap B van
`timeline-arithmetic-per-period`, de fixronde in ref; zonder conflict) en op `fdbec1f1c` (stap B deel i van deze kaart;
één conflict in RS ch. 3.10 en 3.12, beide kanten gehouden: Drools draagt de voorwaarde per periode, ref en clingo het
venster als lid en het kenmerkdoel). Niet gemerged, niet gepusht. Beschermd, dus **voorstel**: `spec/RUNTIME-SEMANTICS.md`,
`spec/IR-SPEC.md`, `spec/verifier/`, `corpus/golden/anchor/` (het nieuwe anker, de verwachtingen van
`jutterswet-dagentelling`, de README), `scripts/check-drools.sh`, `scripts/check-red.sh`, `scripts/check-noop.sh`,
`scripts/check-readings.sh`; `spec/ASP-TRANSLATION.md` onder het CODEOWNERS-net. Buiten de may-touch van stap A en
daarom hier genoemd: `scripts/check-noop.sh` en `scripts/check-readings.sh` (7a maakt vier overlevers van gate 7 en 46
lezingen van gate 17 anders) en drie testklassen onder `backends/drools/src/test` (zie Drools). Proefdocumenten,
scripts en logs onder `/Users/yeb/Developer/uchar-dev/werk/conditions-per-period-ii/` (`scripts/`, `logs/`, `neg/`).

### Commits

- `b4fd47a80` verifier: WF-111 en WF-058 verruimd; vier negatieven, één weg
- `653a41813` ref: het kenmerk binnen een venster, het venster per periode, de dagentelling met 0, dagsoorttoets en
  eenheid, de vouw over een open segment, afronden per segment, de variabele; fixtures en suite-rijen
- `e9ac3597d` clingo: hetzelfde, over een kalender met een jaar marge; README [V8] weg
- `63d4d54c1` frontend: de berekende venstergrens (RGS-202), de samengestelde dagentelling, het type van de variabele
- `28b99c350` corpus: het anker, de golden uit `corpus/red`, de verwachtingen van 7a met hun NOTE's, gate 7 en 8
- `dc72e4dbf` ref en clingo: een kenmerk in een open periode is EF-023, met rode casus; het ch. 5.11-paar van het venster
- `f77689705` spec: RS ch. 3.1, 3.8, 3.9, 3.10, 3.11, 3.12, 5.7b, 5.11; IR-SPEC WF-111, WF-058, BoundDay, TimeWindow;
  ASP-TRANSLATION S9; NOTE van `jutterswet-premievenster`
- `9e49d39c6` gate 12 en 17: zes waardeverschillen, twee kanaalregels, 46 lezingsafwijkingen; `EXPECTED_PLAYED` 325 → 319
- `28c2c180e` frontend: een verwacht segment mag open zijn (scenariolaag), het scenario van `jutterswet-dagentelling-rgs`
- `c75ef4255` drools-tests: de dagentelling zonder haar 0 als bekend verschil in de twee corpusgetuigen
- `de3e2651c` frontend: de semantische tokens van "is altijd <waarde>"
- het commit van dit Result

### De meting op main `b8fed38a0` (letterlijk; `logs/11-red-first.log`, uitvoerders gebouwd in een losse worktree)

- Het anker `anchor/jutterswet-verzuimrente-30-360`, verifier: `WF-023.incompatible-units FAIL /rules/rules/2/versions/0:
  assignment across incompatible units: value unit 'geen', target unit 'dg/mnd'` en `WF-111.bound-form FAIL
  /rules/rules/0/versions/0/condition/from: the from bound of a time window is a computed date expression — a bound is a
  date literal or an own scalar date attribute of the subject; the general date expression is an OPEN wire decision`.
  Beide cases, ref: `EVALUATION FAULT: EF-023: a period condition on the temporal characteristic target "rentedragend" is
  ch. 3.10's CLIP over a characteristic timeline, and no temporal characteristic is written here (ch. 3.11), so there is
  no timeline to cut. Declined at evaluation`; clingo dezelfde zin als `TRANSLATION FAULT`, met `Declined in translation`.
- De frontend op de bron van het anker (Z1): `ERROR (line 32): periodevergelijking (de vanaf-grens 'zijn vervaldatum plus 1
  dg'): the from bound of a time window is a computed date expression — …` en (Z17) `WARNING (line 42): onopgelost: een
  dagentelling telt alleen de dagen waarop het onderwerp zelf één tijdsafhankelijk kenmerk heeft …`. Op
  `werk/implicit-time/meting/a-attribuutgrens` (Z2): `Exception in thread "main" java.lang.IllegalStateException: IrEmitter:
  een onbekend datatype ('niet opgelost') heeft op deze positie geen spelling in uchar-ir-v1; …`; op de branch schrijft
  hij dat document.
- `time-window-timeline-characteristic` (nu golden), beide cases: ref en clingo de EF-023-zin hierboven over "geborgen".
- De goldens met een dagentelling met de verwachtingen van 7a: ref `MISMATCH`, bijvoorbeeld `J1.jaardagen =
  [2024-01-01,2025-01-01)=36, expected [-inf,2024-01-01)=0 [2024-01-01,2025-01-01)=36 [2025-01-01,+inf)=0`; clingo
  `MISMATCH`, bijvoorbeeld `J1.borgdagen timeline = [19783,19814)=99, expected [#inf,#sup)=0`, of in `vast-startpunt`
  `J1.stormpremie is underived (leeg), expected 50`. Eén case van een veranderde golden speelt op main al: `loon-per-
  periode/case2-nooit-aangesteld` (die case veranderde niet).
- De fixtures: `kenmerk-venster` EF-023 (beide), `nul-en-dagsoort` ref `REF-010: unsupported day-count condition shape …`,
  clingo `CLINGO-024: …`, verifier `WF-023.incompatible-units` en `WF-058.condition-not-check`; `venster-als-lid` beide
  `a period condition inside a composite on the temporal target "wachtdagen": … Declined at evaluation` (resp. in
  translation); `venster-rekenen` beide `a period window as the condition of the period arithmetic of "seizoensloon" is not
  executed (no corpus case)` (resp. translated); `afdracht` beide `MISMATCH` op de nullen.
- De vier nieuwe negatieven op main: `wf111-computed-bound-navigated-read` en `wf111-period-boundary-time-dependent-parameter`
  gaven WF-111 met de oude `form` (`a computed date expression`, `the parameter 'peildatum'`), dus niet op hun nieuwe
  parameters; `wf058-day-count-without-characteristic` en `wf058-day-count-target-unit` gaven `WF-023.incompatible-units` en
  `WF-058.condition-not-check`, niet hun arm. De rode casus `time-window-timeline-characteristic-open` faalt op main al met
  EF-023 en dezelfde parameter: de soort die toen elk venster op een kenmerkdoel afwees; nu wijst zij alleen de open
  grens af.

### Wat er veranderde

- **ref** (`eval.ml`, `timeline.ml`, `world.ml`, `reading.ml`). Een tijdsafhankelijk kenmerkdoel met een venster in zijn
  voorwaarde schrijft de maximale perioden van `cond_periods` in `ctls` (`World.write_char_timeline_by`,
  `release_char_timeline`); `characteristic_window_refusals` weigert bij naam vóór elk onderwerp: geen venster, twee
  regels, en per onderwerp een case-tijdlijn van het kenmerk; een open grens is EF-023. `cond_periods` leest het venster
  als knoop (zijn dagen, ook als lid), `read_per_period` = `reads_timeline` of een venster; het venster als hele
  voorwaarde van rekenen per periode knipt. `window_bounds` volgt WF-111 via `Checks.window_bound_misfit`. De dagentelling:
  `day_count_periods` (één kenmerk, de dagsoorttoetsen per dag via `day_kind_matches`; meer kenmerken bij naam
  geweigerd), `day_count_unit` (7c), `Timeline.day_count` (7a). `Timeline.fold_part` laat de gehele-periodevouw een open
  segment op de emmer naast zijn grens lezen. `period_timeline` rondt per segment af. `run_rule` leest de producent door
  een variabele heen (`Checks.let_bound_value`). De lezing van een kenmerkdoel is `read_periods` zonder waarde.
- **clingo** (`translate.ml`). `calendar_span`: de datums van de case (grenzen, scalaire datums, parameters, letterlijke
  venstergrenzen) met 366 dagen marge, zonder datums het jaar rond 1970-01-01; `emit_grid_buckets` legt daarover.
  `translate_characteristic_timeline` leidt `ktl/4` af uit `period_condition` (`?target`); het venster is een knoop met
  zijn grenzen als knips. `translate_day_count`: nullen als maximale reeksen (`adgN_z`, `_run`), de afgewezen dagen van
  een dagsoorttoets als feiten (`adgN_x`, via `dk_holds`), de eenheid, een hek voor afgeleide perioden buiten de kalender.
  Een afronding is een knoop van het rekenen per periode. Het totaal van zo'n doel legt `_wu` ook over de wanden van het
  raster, met een getuige voor een segment ertussen; `period_bounds_not_case` blijft alleen voor het geknipte totaal.
  `refuse_open_whole_period` is weg. `viol_code` leest EF-023 met `characteristic`.
- **verifier**: zie het eerste commit; `Checks.day_count_filter`, `day_count_unit_fits`, `let_bound_value`,
  `window_bound_misfit` zijn de gedeelde vormvragen. WF-126 leest door een afronding.
- **frontend**: `RuleChecker.boundMisfit` (RGS-202 verruimd; de sleutel heet nu `window-bound-not-day-date-expression`,
  de delen `computed` en `parameter-bound` kregen nieuwe tekst), `ExpressionBuilder.dayCountCompound` met
  `ElementaryResolver`, `IrEmitter.letType`; de scenariolaag kent `tot <d>`, `vanaf <d>` en `altijd <waarde>` in een
  verwachting (`verwachtePeriode`, `verwachtTijdlijnAltijd`, `DIVERGENCES.md`).
- **spec** (voorstel): zie het spec-commit.
- **Drools** (alleen tests): `TmsCorpusWitnessTest` en `DrlTextCorpusWitnessTest` noemen de zes goldens bij naam
  (`DAY_COUNT_WITHOUT_ZERO`, rood als er een speelt of een andere faalt); `ScenarioReportWitnessTest` neemt
  `jutterswet-maandloon` als voertuig in plaats van `jutterswet-dagentelling-rgs`. Zonder dat was gate 12 rood (de
  backendsuite), en de JUnit-tests zijn de enige plek die de corpusverandering niet via een gatelijst kon dragen.

### Goldens, negatieven, rode gevallen

| | cases | inhoud |
|---|---|---|
| `corpus/golden/anchor/jutterswet-verzuimrente-30-360` (nieuw, `source.rgs`) | 2 (8 verwachtingen) | formulering A, geval 1 en 2 |
| `corpus/golden/time-window-timeline-characteristic` (uit `corpus/red`) | 2 (4) | `geborgen` op [2024-01-01, 2025-01-01), via `bergdagen` 366; de evaluatiedatum binnen en na het venster |
| `corpus/red/time-window-timeline-characteristic-open` (nieuw) | 1 | EF-023 `{characteristic: geborgen}`, venster "vanaf 1 januari 2024" |
| `corpus/negative/wf111-computed-bound-navigated-read` (nieuw) | — | `WF-111.bound-form`, `form=a navigated read of 'vervaldatum'` |
| `corpus/negative/wf111-period-boundary-time-dependent-parameter` (uit `…-parameter-date`) | — | `WF-111.bound-form`, de parameter voor elke maand |
| `corpus/negative/wf058-day-count-without-characteristic` (nieuw) | — | `WF-058.condition-without-characteristic` |
| `corpus/negative/wf058-day-count-target-unit` (nieuw) | — | `WF-058.target-unit` `target=kale dagen unit=dg bucket=month` |
| `corpus/negative/wf111-period-boundary-parameter-date` | weg | een scalaire datumparameter is sinds VB 6.1 een grens |

Suitefixtures (`ref/test/fixtures/conditions-per-period/`, gedeeld door beide suites, elk met `source.rgs`):
`kenmerk-venster`, `nul-en-dagsoort`, `venster-als-lid`, `venster-rekenen`, `verzuimrente-onafgerond`. Alle verwachtingen
met de hand gerekend vóór een uitvoerder ze zag; ref en clingo spelen alles, gate 10 vindt het anker byte-gelijk, gate 16
maakt de vier casebestanden van `jutterswet-dagentelling-rgs` byte-gelijk uit het scenario dat de verwachtingen apart
schrijft (een tweede, onafhankelijke spelling van dezelfde handwaarden).

**Het anker.** rentedagen geval 1: 0 tot 2025-11-01, 15, 30 op [2025-12-01, 2026-03-01), 12, 0 vanaf 2026-04-01; geval
2: 0 tot 2028-02-01, 18 (19 min de schrikkeldag), 30, 4, 0 vanaf 2028-05-01. Maandrente exact 16 2/3, 33 1/3, 33 1/3,
41 2/3, 16 2/3 en 25, 41 2/3, 5 5/9, totaal **141 2/3 €** en **72 2/9 €** (suite `WV-3.11-formulering-A-onafgerond-*`). De
keuze van het doel: de maandrente rekenkundig op **3 decimalen** (16,667; 33,333; 41,667; 16,667 en 25,000; 41,667;
5,556), de verzuimrente op de cent (**141,67 €**, **72,22 €**), omdat de ASP-codering geen niet-decimale waarde houdt. Drie
decimalen is de kleinste schaal waarop afronden per maand het totaal op de cent gelijk laat aan de afronding van het
exacte totaal (met twee: 25,00 + 41,67 + 5,56 = 72,23), en houdt elke maandwaarde onder 46 340 op zijn schaal. Het product
staat als rentevoet × rentedagen × afdrachtbedrag: rentevoet × afdrachtbedrag is 50 000 op de schaal van de rentevoet,
boven de factorgrens van clingo. Geen besluit maakt een maand anders dan het voorstel rekent: 7a voegt alleen maanden van
0 toe, met maandrente 0.

### Veranderde verwachtingen, oud → nieuw (7a; elke tijdlijn kreeg de open segmenten van 0 en 0 in elke tussenmaand)

- `anchor/jutterswet-dagentelling` (beschermd) en `jutterswet-dagentelling-rgs` (dezelfde vier cases): case1 borgdagen
  [jan)=23 [feb)=13 → +(−∞, 2024-01-01)=0, [2024-03-01, +∞)=0; jaardagen 2024=36 → +(−∞,2024)=0, [2025,+∞)=0. case2
  borgdagen dec=2 jan=2 → +(−∞, 2024-12-01)=0, [2025-02-01,+∞)=0; jaardagen 2024=2 2025=2 → +(−∞,2024)=0, [2026,+∞)=0.
  case3 beide `[]` → (−∞, +∞)=0. case4 (`input-blijft`) borgdagen [2024-03-01, 2024-04-01)=99 (de invoer bleef staan) →
  (−∞, +∞)=0: de telling beslist nu elke dag, dus de invoer wijkt; de naam bleef staan.
- `jutterswet-vast-startpunt`: elke case stormwachtdagen en maandwachtdagen + de open nullen (case4: `[]` → (−∞,+∞)=0);
  case4 `maandpremie` 20 → leeg (0 is niet groter dan 0); case6 en case7 `stormpremie` leeg → 50 (de telling heeft nu op
  elke dag een waarde, ook in 2024).
- `jutterswet-wachtmaanden`: toeslagdagen case1 15, 12 → 0 tot 2025-11-01, 15, 12, 0 vanaf 2026-04-01; case2 19, 4 →
  0 tot 2028-02-01, 19, 4, 0 vanaf 2028-05-01; case3 `[]` → (−∞, +∞)=0.
- `jutterswet-wachtdagen-twee-schrijvers`: case1 15, 30, 12 → + 0 tot 2025-11-01 en vanaf 2026-04-01; case2 19, 30, 4 →
  + 0 tot 2028-02-01 en vanaf 2028-05-01; case3 10, 27, 2 → 0 tot 2024-01-01, 10, **0 in februari**, 27, 2, 0 vanaf
  2024-05-01. De botsing die deel i noemde treedt niet op: de tweede schrijver schrijft een 0 alleen in een maand die niet
  geheel is.
- `jutterswet-loon-per-periode` (gemeten, niet in de opdracht genoemd): wachtdagen case1 29, 9, 20 → 29, 9, **0 in april**,
  20, **0 op [2024-06-01, 2024-08-01)** (de nullen geknipt tot aangesteld); case3 `[]` → 0 op januari en op
  [2024-03-01, 2024-05-01).
- `jutterswet-pachtvenster`: gemeten, **geen verschil** (in elk venster heeft elke emmer een ware dag of het venster is
  leeg).
- Rode casus `jutterswet-wachtdagen-botsing`: ongewijzigd, EF-016 op december 2025.
- ref-tests die de kaart omdraait: `WV-3.9-per-emmer` (de `toetsdagen` van de fixture `afdracht`, gespeeld als
  `WV-3.10-voorwaarde-per-periode`) 15, 12 → + 0 tot 2025-11-01 en vanaf 2026-04-01, en de rentedagen idem;
  `WV-5.7-per-dag-gelijk` [nov)=30 → (−∞, nov)=0, 30, [dec, +∞)=0; `WV-5.12-voorwaarde-per-periode` de twee
  BuiltinCall-regels met de nullen; `WV-3.10-clip-dagtelling-31-gesneden-op-de-vijfde` 31 → 31 en [2025-08-01, +∞)=0.
  Door punt 6 en VB 6.1: `WV-3.10-grens-buiten-de-mirror-note` en `-fasestaart` (een parameter als grens geweigerd → een
  attribuut van een ander type geweigerd), `WV-3.10-clip-als-lid-geweigerd`/`-fasestaart` → `WV-3.10-clip-als-lid` (knipt
  als het hele venster), `WV-3.10-tijdlijnKENMERKdoel-is-de-clip` (EF-023 → de perioden [2024-01-01, 2025-01-01)) plus
  `-open-grens`. Door de open vouw: `WV-3.1-open-bucket-walk` (weigering → waar, plus `-onwaar` en `-buur`) en
  `WV-3.11-open-whole-period` (weigering → speelt, de fixture-case kreeg `loonpremie` 50).
- clingo-tests die de kaart omdraait: `ch3.10 a parameter bound …` (weigering → speelt; de weigering verhuist naar een
  attribuut van een ander type), `ch3.10 the window as a member …` (weigering → `ref_matches`/`clingo_matches`), `ch3.11 …
  has-fact` (andere zin: zonder venster), `ch3.2/3.9 day count on a fixed grid` (vol en maandvol leeg → 301 en 302) en
  `… no day counted` (boven 400 → leeg), `period arithmetic: open-whole-period` en `total-over-pro-rata` (weigering →
  speelt), `README [V8]` (weigering → speelt).
- frontendtests die VB 6.1 omdraait: `PeriodBoundaryTest` (de parameter en de berekende grens gaan van geweigerd naar
  schoon; twee nieuwe geweigerde rijen met "plus 1 dg"), `StructureSentenceTest` (de nieuwe staart van RGS-202).

### Rood en mutaties (niet gecommit; `scripts/mutate.py` op een losse worktree, `logs/32-`, `34-`, `36-mutations*.log`)

Rood eerst: zie de meting. Per bewaking een mutatie, elk rood (een andere fout of een MISMATCH op de getuige):

| # | mutatie | getuige | uitkomst |
|---|---|---|---|
| R1 | ref 7a: geen nullen | anker dagentelling case1 | MISMATCH borgdagen, jaardagen |
| R2 | ref 7b: dagsoorttoets genegeerd | anker 30-360 case2 | MISMATCH maandrente 26.389, verzuimrente 73.61 |
| R3 | ref 7c: telling zonder eenheid | ref-suite | `WV-3.8-nul-en-dagsoort-eenheid`, `-onafgerond-*` |
| R4 | ref 6: venster op alle tijd | fixture kenmerk-venster | EF-023 open grens |
| R5 | ref: open kenmerkperiode niet geweigerd | rode casus open | EXPECTED FAULT MISMATCH |
| R6 | ref: vouw leest open segment heel | vast-startpunt case1 | EVALUATION FAULT (emmerwandeling) |
| R7 | ref WF-111: elke grens toegelaten | ref-suite | `WV-3.10-grens-*` |
| R8 | ref: geen afronding per segment | anker 30-360 case1 | EF-006 50/3 |
| R9 | ref: variabele niet doorzien | anker 30-360 case1 | REF-011 |
| R10 | ref 5.11: tot vóór van | ref-suite | `WV-5.11-venster-*` |
| C1 | clingo 7a: geen nulreeksen | anker dagentelling case1 | MISMATCH |
| C2 | clingo 7b: afgewezen dagen geteld | anker 30-360 case2 | MISMATCH 26389, 7361 |
| C3 | clingo 6: geen afgeleide ktl | fixture kenmerk-venster | MISMATCH |
| C4 | clingo: geen kalendermarge | clingo-suite | `condition per period: kenmerk-venster` UNSAT |
| C5 | clingo: geen rasterwanden in het totaal | anker 30-360 case1 | UNSAT, de nieuwe getuige |
| C6 | clingo: afronding kapt af | anker 30-360 case1 | MISMATCH 16666, 14166 |
| C7 | clingo 6: vensterknoop houdt altijd | fixture venster-als-lid | MISMATCH |
| C8 | clingo: open kenmerkperiode niet geweigerd | rode casus open | EXPECTED FAULT MISMATCH |
| V1 | verifier: geen eenheidsarm | `wf058-day-count-target-unit` | `verifier: PASS` |
| V2 | verifier: samenstelling zonder kenmerk toegelaten | `wf058-day-count-without-characteristic` | andere arm |
| V3 | verifier: geen wandeling in een operator | `wf111-computed-bound-navigated-read` | `verifier: PASS` |
| F1 | frontend: samenstelling blijft marker | `DayCountConditionTest` | 2 rood |
| F2 | frontend: Let houdt onbekend type | `DayCountConditionTest` | 1 rood |
| F3 | frontend: geen wandeling in een operator | `PeriodBoundaryTest` | 2 rood |
| F4 | frontend: open grens als dag | `ScenarioLoweringTest` | 1 rood |

### Codes, floors en pins, oud en nieuw

- Nieuw: de armen `WF-058.condition-without-characteristic` en `WF-058.target-unit` (parameters `target`, `unit`,
  `bucket`). `WF-111.bound-form` krijgt nieuwe `form`-teksten. EF-023 (ch. 5.7b) **versmald**: "a period condition on a
  temporal characteristic target leaves its true periods with an open bound", getuige de nieuwe rode casus in plaats van
  de vertrokken; de Drools-codes DROOLS-104 en DROOLS-268 noemen EF-023 nog als `ef` (stap B moet dat bekijken). Geen
  nieuwe EF-, REF-, CLINGO- of RGS-code; RGS-202 houdt zijn nummer, sleutel `window-bound-not-literal-or-own-attribute`
  → `window-bound-not-day-date-expression`. REF-010 en CLINGO-024 kregen een langere zin. clingo README: [V8] weg.
- Gate 12 (voorstel, op `fdbec1f1c`): `EXPECTED_CHANNEL` + zes `DIVERGE`-regels (`anchor/jutterswet-dagentelling`,
  `jutterswet-dagentelling-rgs`, `jutterswet-loon-per-periode`, `jutterswet-vast-startpunt`,
  `jutterswet-wachtdagen-twee-schrijvers`, `jutterswet-wachtmaanden`), elk met reden in `EXCLUDE_KNOWN` ("7a: 0 in een
  emmer zonder ware dag; Drools volgt in stap B deel ii …"), en + `anchor/jutterswet-verzuimrente-30-360|EXCLUDE
  DROOLS-102`, `time-window-timeline-characteristic|EXCLUDE DROOLS-102`; `EXPECTED_PLAYED` 325 → 319. Gate 8:
  Drools-uitsluiting `time-window-timeline-characteristic` weg, `time-window-timeline-characteristic-open|EXCLUDE
  DROOLS-102` erbij. Gate 7: vier overlevers weg (`dagentelling` en `dagentelling-rgs`, case3 en case4). Gate 17:
  `DEVIATIONS` + 46 regels (de 23 casebestanden van de zes goldens × twee takken). Geen floor veranderd.
- Suites: ref 2262 voorbeelden (na de rebase; deel ii voegde er 18 toe), clingo 2431 checks (was 2418), frontend 1490
  tests, Drools-suite groen.

### Voorgestelde CONSTRUCTS-tekst (rij 11, achteraan; rij 26 voor WF-111 en WF-058)

> **Landed (2026-11-03, card conditions-per-period, step A part ii):** a time-dependent characteristic is written within
> a period window: it holds in the periods of the window, cut by the rest of its condition read per period
> (`spec/proposals/IMPLICIT-TIME.md` point 6, RegelSpraak §10.3, §8.4.3); a window bound is a day-precise date
> expression over the subject, the parameters and literals (`WF-111` widened, VB 6.1); a window is read per period as a
> member of a condition and as the condition of the period arithmetic. The day count is 0 in every bucket without a
> true day over all time (maintainer decision 2026-11-03, 7a), takes a day-kind test of the counted day beside one own
> characteristic (7b) and carries days per unit of its bucket into a target that declares a unit (7c; `WF-058`
> widened). Executed by ref and the ASP backend; witnesses `anchor/jutterswet-verzuimrente-30-360` (formulation A),
> `time-window-timeline-characteristic`, red case `time-window-timeline-characteristic-open` (EF-023, an open period),
> negatives `wf111-computed-bound-navigated-read`, `wf111-period-boundary-time-dependent-parameter`,
> `wf058-day-count-without-characteristic`, `wf058-day-count-target-unit`. The Drools backend refuses the characteristic
> target (DROOLS-102) and writes the day count without its 0 (six value divergences in gate 12) until step B part ii.
> Tracked v1 delta: v0 refuses a temporal characteristic target and a computed bound, and its day count writes no
> segment in a bucket without a true day.

### Voorlopige besluiten

- VB-C11 — De nullen van 7a: elke emmer met een ware dag is één segment, zoals eerst; elke reeks emmers zonder ware dag
  is één segment van 0, ook tussen twee getelde emmers, open waar zij voor de eerste of na de laatste reikt; zonder ware
  dag (−∞, +∞) = 0. ALEF vult de gaten zo; de getelde emmers blijven per emmer omdat de goldens dat pinnen (31, 31).
- VB-C12 — De arm van ch. 7.8 geldt voor de telling alleen nog waar haar voorwaarde haar tot niets knipt: over alle tijd
  beslist zij elke dag. `case4-input-blijft` houdt zijn naam en verwacht nu de 0.
- VB-C13 — 7b: een `all`-samenstelling van precies één eigen tijdsafhankelijk kenmerk en dagsoorttoetsen op `BoundDay`, in
  beide polariteiten; zonder kenmerk de nieuwe arm, met twee kenmerken `condition-not-check` (ref en clingo weigeren dat
  bij naam als faalsnel-hek). De frontend schrijft alleen die vorm; twee kenmerken blijven een marker.
- VB-C14 — 7c: de eenheid van het doel is een dageenheid over de eenheid van de emmer, beide herkend aan de spellingtabel
  van ch. 3.8; WF-023 vergelijkt de eenheid van de telling niet, WF-058 wel.
- VB-C15 — WF-111: toegelaten zijn literal, eigen scalair attribuut, scalaire parameter en `Binary`, `BuiltinCall` zonder
  verzameling, filter of emmer, `Rounding` daarover; geweigerd elke navigatie, tijdlijn, vouw, sessiewaarde en
  `BoundDay`. Een positie zonder onderwerp leest geen attribuut (de oude zin blijft).
- VB-C16 — Een kenmerkdoel eist een venster op enige diepte onder `all`/`the`; de perioden zijn die van `cond_periods`.
  Bij naam geweigerd: geen venster, twee schrijvers, een case-tijdlijn van het kenmerk (ref per onderwerp bij de eerste
  schrijfactie, clingo per case), en een open grens als EF-023.
- VB-C17 — EF-023 blijft bestaan omdat de Drools-codes haar noemen en gate 8 elke rij een getuige laat eisen; haar soort
  is nu de open periode van een kenmerk.
- VB-C18 — Een `Let` waarvan het lichaam zijn binder is, wordt doorzien voor de producenten van een tijdlijndoel en voor
  WF-058 (`Checks.let_bound_value`); een andere `Let` op zo'n doel blijft geweigerd. De frontend schrijft het type van
  zo'n `Let` uit het vaste type van de telling.
- VB-C19 — Rekenen per periode rondt per segment af en voegt daarna gelijke buren samen.
- VB-C20 — De afronding van het anker: zie "Het anker".
- VB-C21 — De gehele-periodevouw over een open segment: ref leest de emmer naast de eindige grens en die ernaast
  (`fold_part`), clingo legt een jaar marge; dat heft VB-A8 op voor ch. 3.9. Een vouw die emmers afloopt voor ch. 3.7 en
  3.8 weigert een open periode nog steeds.
- VB-C22 — De kalender van clingo: de datums van de case met 366 dagen marge, zonder datums het jaar rond 1970-01-01; een
  afgeleide ware periode erbuiten is een benoemde getuige, nooit een stille waarde.
- VB-C23 — Het totaal van een doel van rekenen per periode in clingo legt `_wu` over de case-grenzen, de ware perioden en
  de wanden van het raster (maand, kwartaal, jaar en vaste starts); een segment met andere grenzen is een getuige. Dat
  heft VB-A9 en [V8] op voor dat totaal; het geknipte totaal houdt zijn weigering.
- VB-C24 — De dagsoorttoets in clingo wordt per dag van de kalender uitgerekend met `dk_holds`, zoals de duurvormen doen.
- VB-C25 — De scenariolaag krijgt `tot <d>`, `vanaf <d>` en `altijd <waarde>` alleen in een verwachting; een invoer houdt
  twee grenzen.
- VB-C26 — De drie Drools-tests: zie "Drools"; stap B deel ii haalt de lijst `DAY_COUNT_WITHOUT_ZERO` weer weg.

### Wat stap B (Drools) moet weten (bovenop deel i)

- 7a: beide takken schrijven geen 0 in een emmer zonder ware dag. Zes goldens staan als `DIVERGE` (gate 12), hun 23
  casebestanden × twee takken in gate 17 `DEVIATIONS`, en dezelfde zes in `DAY_COUNT_WITHOUT_ZERO` van de twee
  corpusgetuigen; speelt Drools ze, dan gaan alle drie de lijsten weg en `EXPECTED_PLAYED` 319 → 325. In
  `vast-startpunt` leest de gehele-periodevouw daarna open segmenten: `stormpremie` 50 in case6/7, `maandpremie` leeg in
  case4.
- Nieuwe vormen: een tijdsafhankelijk kenmerk als doel binnen een venster (DROOLS-102 op het anker en op
  `time-window-timeline-characteristic`; de rode casus met een open venster moet EF-023 geven, nu EXCLUDE in gate 8);
  het venster als lid en als voorwaarde van rekenen per periode (DROOLS-104 op `pachtvenster`, ongewijzigd); een
  berekende venstergrens; de dagentelling met een dagsoorttoets (DROOLS-114 volgens de meting van IMPLICIT-TIME §5) en
  met eenheid dg/mnd; een afronding in rekenen per periode; de telling achter een variabele (`Let` met de binder als
  lichaam); het totaal over een tijdlijn met afgeleide grenzen.
- De lezing (gate 17): een kenmerkdoel leest ref als `read_periods` zonder waarde (HOLDS, de knopen hun perioden, het
  venster zijn dagen); in een samengestelde voorwaarde van een dagentelling draagt het kenmerk zijn ware perioden en de
  dagsoorttoets de getelde dagen.
- EF-023 is versmald (zie Codes); DROOLS-104 en DROOLS-268 noemen haar nog als `ef`.

### `make gates`

Groen op `de3e2651c` (gerebased op `fdbec1f1c`): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 329 s
(`out/gates/20261103-154113/`, `logs/gates-3.log`). Twee eerdere runs waren rood en zijn gerepareerd: run 1 op gate 12
(de Drools-suite: de twee corpusgetuigen en het scenariorapport) en gate 16 (het scenario van
`jutterswet-dagentelling-rgs` kon de open segmenten niet schrijven), run 2 op gate 10 (de semantische tokens van de
nieuwe verwachting). Gemeten: gate 3 424 goldens schoon, 606 negatieven verworpen (was 603), 449 op hun arm (was 446);
gate 5 ref-suite 2262, gate 5 en 6 1055 cases ref = clingo; clingo-suite 2431; gate 8 63 rode cases; gate 10 1490 tests,
185 rgs-goldens byte-gelijk (was 184); gate 12 PASS, played 319 op beide takken, 6 waardeverschillen met reden; gate 16 40
scenario-goldens, 136 casebestanden; gate 17 325 goldens en 898 casebestanden vergeleken, 46 afwijkingen met reden. De
kaart blijft in `doing/`.

## Result: stap B deel i (bouwer, 2026-11-03; door de hoofdsessie overgenomen uit `werk/conditions-per-period-drools/verslag.md`, geland op `fdbec1f1c`)

### Result: stap B deel i (bouwer Drools, 2026-11-03)

Branch `card/conditions-per-period-drools`, gebouwd op main `3dda89983` en gerebased op main `1d7259100` (de fixronde
`ref-empty-operand-unit`; zonder conflict). Niet gemerged, niet gepusht; de kaart is niet aangeraakt. Beschermd in deze
stap, dus **voorstel**: `scripts/check-drools.sh`, `scripts/check-red.sh` (de uitsluitingslijst van het Drools-been).
Logs, proefdocumenten en scripts onder `/Users/yeb/Developer/uchar-dev/werk/conditions-per-period-drools/` (`logs/`,
`docs/`, `traces/`, `scripts/`).

#### Commits

- `5c36e2857` drools: voorwaarden per periode, de kwalificator per emmer, twee schrijvers per dag
- `48e793c8e` gates 8 en 12: voorwaarden per periode spelen in Drools (voorstel)
- `1cd7f2018` tests: een lid zonder tijdlijn en de emmerscalair in een voorwaarde per periode
- `90b1d57c1` tests: de tijdlijn rechts in een vergelijking per periode, en de lezing van een fout
- `0012df0ce` tests: twee aangrenzende perioden van een voorwaarde zijn er één

#### Rood eerst (op `3dda89983`, letterlijk; `logs/01-red-first.log`, `logs/15-red-witness.log`)

- `jutterswet-loon-per-periode`, beide takken: `EXCLUDE DROOLS-302: transpiler SKIP: regel: aangesteld loon van een
  jutter — the rule "aangesteld loon van een jutter" writes the time-dependent attribute 'aangestelde loon' of Jutter by
  the period arithmetic of ch. 3.4/3.5 under a condition: a condition per period is not executed (RUNTIME-SEMANTICS ch.
  3.11)`
- `jutterswet-wachtmaanden`, beide takken: `EXCLUDE DROOLS-105: transpiler SKIP: regel: wachtpremie van een jutter — a
  value of the time-dependent target 'wachtpremie' of Jutter that is neither a time-proportional part, a day count nor
  the period arithmetic of an operator of ch. 1.2 other than the power or of a sum per period ('50'): this branch does
  not yet carry this form (RUNTIME-SEMANTICS ch. 3.4, 3.5, 3.11); no golden with cases writes one`
- `jutterswet-wachtdagen-twee-schrijvers`, beide takken: `EXCLUDE DROOLS-103: transpiler SKIP: regel: wachtdagen in een
  volle maand — a time-dependent target with a second writer ('wachtdagen' of Jutter is also written by "wachtdagen in
  een gedeeltelijke maand"): this branch does not yet carry this form; a target with more writers stays fenced
  (RUNTIME-SEMANTICS ch. 7.8), and no golden with cases has one`
- rode casus `jutterswet-wachtdagen-botsing`, `DroolsRedMain`: `EXCLUDE DROOLS-103: … ('wachtdagen' of Jutter is also
  written by "wachtdagen in elke maand") …`, dus geen FAULT EF-016.
- De nieuwe getuige `TmsConditionPerPeriodWitnessTest` op `3dda89983` (alleen dat bestand erbij, in de stand van de eerste
  testcommit, 9 tests): alle 9 rood — de vormen op DROOLS-302 ("… onder een voorwaarde: een voorwaarde per periode wordt
  niet uitgevoerd"), DROOLS-105 en DROOLS-103, de lezing op de weigering, de weigerrijen op DROOLS-302 in plaats van 309.
  De drie rijen die later kwamen (lid zonder tijdlijn, emmerscalair en zijn lezing, tijdlijn rechts) en J3 zijn niet op
  `3dda89983` gedraaid; mutaties M1, M7, M20 en M21 tonen ze rood.

#### Na de bouw

- De drie goldens: `PLAYED 3` op beide takken; de rode casus: `FAULT EF-016 {"rules":"wachtdagen in een volle maand,
  wachtdagen in elke maand","values":"30, 31","attribute":"wachtdagen","label":"J1","objectType":"Jutter"}: …`
  (`logs/05-play.log`). Ook `values` is die van ref (VB-C7: clingo noemt hem niet).
- De lezingen op beide takken regel voor regel gelijk aan `runner --trace` van ref: de negen casebestanden van de drie
  goldens (`logs/06-readings.log`), de fixture `afdracht` van `WV-5.12-voorwaarde-per-periode` (20 regels), en de
  fixtures `totaal-onder-voorwaarde` en `twee-schrijvers-gelijk`; `afdracht-botsing` geeft in `DroolsRedMain` precies de
  vijf parameters van zijn pin, `afdracht-fijner` DROOLS-308 (de fijnere operand van rekenen per periode).

#### Wat er veranderde

- **Runtime, zuivere functies** (nieuwe klasse `PeriodCondition`, naast `PeriodArithmetic`): `truePeriods` en `outside`
  (de ware perioden van een kenmerk, maximaal, en hun complement over alle tijd), `where` (de perioden van alle tijd
  waarop een vergelijking met ch. 1.2 per dag geldt, een gat leeg; tijdlijn en grens in de volgorde van de vergelijking),
  `comparisonReading` (de lezing: gevulde segmenten in de perioden met waarden, plus de gaten waar een lege kant voldoet),
  `wholeBuckets` (de kwalificator per emmer van zijn kalendermaand of -jaar, open grenzen open, negatie als complement
  over alle tijd), `member`/`composite` (elk lid gelezen, de kwantor per dag), `always` (een lid zonder tijdlijn: alle tijd
  of niets), en voor twee schrijvers `clash` (de eerste dag waarop twee beslissen en verschillen) en `union` (de
  segmenten in documentvolgorde, een gedeelde dag bij de eerste, de invoer op de dagen zonder schrijver).
  `PeriodArithmetic.clipped` knipt de tijdlijn van elke producent tot de perioden, de perioden eerst; `Exact` is publiek
  zodat een lezing de tijdlijn van een operator kan noemen. Zeven nieuwe `StaticFunction`s (`PERIOD_CLIPPED`,
  `PERIOD_TRUE_PERIODS`, `PERIOD_OUTSIDE`, `PERIOD_WHERE`, `PERIOD_WHOLE_BUCKETS`, `PERIOD_MEMBER`, `PERIOD_COMPOSITE`,
  `PERIOD_ALWAYS`), elk ook in `ClosureRendering`.
- **Lowering met truth maintenance** (`TmsTranspiler`, één analyse, beide weergaven): `readsTimeline` stelt de vraag van
  `Eval.reads_timeline` (syntax en declaraties). Leest de voorwaarde van een regel met een tijdlijndoel een tijdlijn, dan
  komt er geen test in de linkerkant: `periodCondition` plant haar perioden als term in de consequentie, en de waarde
  (pro rata, dagentelling, rekenen per periode) wordt daartoe geknipt; de fout- en depositoregels van de voorwaarde
  vervallen, want haar fouten en deposito's zijn die van de waarde. Een voorwaarde zonder tijdlijn blijft de poort, nu
  ook op rekenen per periode. `isPeriodArithmetic` neemt ook een literal, een eigen read zonder pad en een parameter als
  hele waarde (zoals `Eval.is_period_arithmetic`), dus `periodArithmeticTarget` ook. `requireFits` is WF-126 als
  fail-fast (`grid_fits` van de verifier). Twee of meer rekenregels van één tijdlijndoel (`timelineWriters`, per
  concreet type, versies van één regel als één schrijver) zijn geen DROOLS-103 meer: elke regel schrijft zijn `Value`,
  `readFixpoint` zet de vereniging met de invoer op de bean (`union`, niet bij een emmerscalair als invoer), en
  `ComputeHelpers.differ` en `WritersConflict.of` vergelijken tijdlijnen per dag. Een lezing van zo'n doel weigert
  `Reads.timeline`/`navigated` bij naam.
- **Lezing** (gate 17): `ReadingRule` draagt de term van de perioden; is die er, dan is de status HOLDS, of FAULTS waar
  de voorwaarde faalt, en daarna wordt de waarde gelezen. Elke voorwaardeknoop heeft zijn perioden, een vergelijking zonder
  kwalificator haar afgeleide lezing (`comparisonReading`); in `periodOperand` noteert elke knoop zijn term (een
  afwezige tijdlijn van een operand leest `[]`, zoals ref); `unreached` alleen nog zonder voorwaarde. `note` laat de
  laatste notitie winnen (de wortel van een operand als hele waarde leest de geschreven tijdlijn). `SessionDump`
  schrijft een `Exact`-segment als `[van tot waarde]`.
- **Catalogi** (nl, en): DROOLS-309, -310, -311 en `PART.desc.period-condition`. **README** van de backend: de alinea's
  "A condition read per period" en "Two writers per day", de lezing bij rekenen per periode, de weigeringen.

#### Codes, floors en pins, oud en nieuw

- `RETIRED`: DROOLS-302 (`period-arithmetic-condition`). Register 308 → 311 codes, 18 → 19 retired.
- Nieuw: DROOLS-309 `period-condition-form` (REFUSAL; `attribute,declarant,snippet`): een genavigeerde of gehoste toets,
  een kwalificator op een kenmerk met één waarde, een vergelijking van twee tijdlijnen, van datums of van geen getallen,
  een leeg-/gevuldtoets en een kwantorvouw in een voorwaarde per periode. DROOLS-310 `period-condition-grid` (REFUSAL;
  `operand,operandGranularity,attribute,targetGranularity`): WF-126. DROOLS-311 `timeline-writers-read` (REFUSAL;
  `attribute,declarant`): een lezing van een tijdlijndoel van twee of meer schrijvers.
- Blijven: DROOLS-103 (een andere schrijver dan een rekenregel naast een andere, een initialisatie, defaults, verdeling),
  DROOLS-104 (het venster, deel ii), DROOLS-105 (de macht en elke andere waarde), DROOLS-271 (een tijdsafhankelijk lid
  in een scalaire positie, ook een genavigeerde tijdsafhankelijke toets als poort, zoals ref haar niet per periode leest).
- Gate 12 (voorstel): `EXPECTED_CHANNEL` − `jutterswet-loon-per-periode|EXCLUDE DROOLS-302|…`,
  − `jutterswet-wachtdagen-twee-schrijvers|EXCLUDE DROOLS-103|…`, − `jutterswet-wachtmaanden|EXCLUDE DROOLS-105|…`;
  `EXPECTED_PLAYED` 322 → 325 met een regel in het commentaarblok. Kanaalvloer 50 → 47 met naam.
- Gate 8 (voorstel): de uitsluiting `jutterswet-wachtdagen-botsing|EXCLUDE DROOLS-103 …` weg; het Drools-been pint de
  casus nu op EF-016 met de vier parameters van de case: 45 fault-gepind (was 44), 18 niet-gedragen (was 19), 16 EXCLUDE
  met code (was 17, floor 16 — precies op de vloer).
- Geen floor gewijzigd. JUnit 1804 → 1816 (floor 1573), nul skips.
- Tests die de kaart omdraait, oud → nieuw:
  - `TmsPeriodArithmeticWitnessTest.every_form_outside_the_period_arithmetic_it_carries_is_refused_by_name_on_both_branches`,
    rij "a condition on the rule" (`maandloon` onder `check("actief")`, DROOLS-302) → weg; de vorm speelt als poort in
    `TmsConditionPerPeriodWitnessTest.a_condition_that_reads_no_timeline_gates_the_period_arithmetic`.
  - `TmsTimelineRefusalWitnessTest`, rij `tweede-schrijver` (twee pro-ratapoorten op `jaardeel`, DROOLS-103 voor elk) →
    weg, met de helper `secondWriter` en de alinea in de javadoc ("the verifier passes all eleven" → "all ten"); het
    document speelt in `…two_writers_of_a_pro_rata_part_each_under_a_gate_in_every_order_on_both_branches` (306 en 31,
    de waarden die ref al gaf).
- Nieuwe getuige `TmsConditionPerPeriodWitnessTest` (12 tests): A voorwaarden per periode op rekenen per periode en een
  scalair (kenmerk, ontkenning, "ten minste één" met "ongelijk aan" over gaten, een scalair onder een vergelijking; J3
  met twee aangrenzende perioden; 24 volgorden × 2 takken); de poort zonder tijdlijn; een lid zonder tijdlijn in een
  samenstelling; de tijdlijn rechts; de emmerscalair (de fout van ch. 8.4 en de lezing FAULTS); B de kwalificator per
  emmer (beide polariteiten, de negatie binnen de kwalificator, een jaarkwalificator op een vergelijking; 24 × 2); C twee
  schrijvers met invoer op een dag die geen beslist, twee die een dag met één waarde delen, twee die verschillen (EF-016
  in beide volgorden, de regels en waarden in documentvolgorde), twee pro-ratapoorten (elk 2 × 2); D de weigeringen 309
  (drie vormen), 310 en 311 op beide takken in één zin met hun code; E de lezingen van A op beide takken, regel voor
  regel de trace van ref. Gemeten (`logs/10-witness-ref.log`, de tien documenten uit `main`): de verifier laat ze alle
  tien door; ref geeft op alle tien de verwachtingen van de getuige (de botsing in beide volgorden als EF-016 met de
  regels en waarden van de getuige, de emmerscalair met dezelfde zin), en zijn trace is op de zeven zonder fout regel voor
  regel die van Drools (`logs/11-witness-readings.log`); clingo geeft de verwachtingen op A, B, `a-actief`,
  `a-tijdlijn-rechts`, de botsing en `c-jaardeel`, weigert bij naam de gedeelde dag ([V7]) en de invoer op een doel van
  twee schrijvers, en **speelt de emmerscalair** (`clingo: PASS`), waar ref en Drools falen: clingo leest daar de
  emmerscalair als geen tijdlijn (open punt 3). Ref schrijft voor een sessie met een fout geen trace, dus de lezing
  FAULTS van de emmerscalair is met de hand naar RS ch. 5.12 gepind.

#### Mutaties (niet gecommit; `scripts/mutate.py` op een losse worktree, `logs/18-mutations.log`, `logs/20-mutations-m1.log`)

| # | mutatie | uitkomst |
|---|---|---|
| M1 | perioden van een toets niet samengevoegd | eerst groen op golden loon (gelijke buren worden één segment); na J3 rood: E (de lezing) |
| M2 | een ontkende toets leest zijn ware perioden | rood: A |
| M3 | een gatdag leest niet leeg in een vergelijking | rood: A |
| M4 | de kwalificator per emmer negeert zijn ontkenning | rood: B |
| M5 | de kwalificator houdt een gedeeltelijke eerste emmer | rood: B |
| M6 | een samenstelling leest elke kwantor als "alle" | rood: A |
| M7 | een lid zonder tijdlijn geldt altijd | rood: lid zonder tijdlijn |
| M8 | de tijdlijn van de producent wordt niet geknipt | rood: A |
| M9 | geen voorwaarde wordt per periode gelezen | rood: A |
| M10 | de vereniging laat de invoer weg | rood: C invoer |
| M11 | een latere schrijver houdt een dag die een eerdere beslist | rood: C gelijk |
| M12 | de terugschrijving vergelijkt tijdlijnen heel | rood: C invoer |
| M13 | twee schrijvers botsen nooit | rood: C botsing |
| M14 | de verschilregel vergelijkt tijdlijnen heel | rood: C invoer |
| M15 | elk raster past | rood: D (310) |
| M16 | een lezing van een doel van twee schrijvers niet geweigerd | rood: D (311) |
| M17 | een afwezige operandtijdlijn leest `empty` in plaats van `[]` | rood: E |
| M18 | de knopen van de waarde onbereikt, ook onder een voorwaarde | rood: E |
| M19 | de lezing van een vergelijking zonder haar gaten | rood: E |
| M20 | een fout in de voorwaarde faalt de lezing niet | rood: emmerscalair (lezing) |
| M21 | de operator niet omgekeerd voor een tijdlijn rechts | rood: tijdlijn rechts |
| M22 | het conflict van twee tijdlijnen noemt hun hele waarden | rood: C botsing |

#### Voorgestelde CONSTRUCTS-tekst (rij 11, in de zin van stap A deel i)

"the Drools backend refuses the forms (DROOLS-302, -105, -103) until step B" →

> since step B (2026-11-03) both branches of the Drools backend carry them too (`PeriodCondition`: the periods of the
> condition in the rule's consequence, the producer's timeline cut to them; the union of two writers' timelines with the
> target's input at the write-back, EF-016 on the first day two decide differently), refusing by name a form of the
> condition it does not read per period (DROOLS-309), a grid that does not fit (DROOLS-310) and a read of a target of two
> writers (DROOLS-311)

#### Voorgestelde tekst elders (beschermd of buiten mijn may-touch, voor de hoofdsessie)

- `CLAUDE.md`, gate 12, na de zin over `timeline-arithmetic-per-period`: "Since the card `conditions-per-period` (step B,
  2026-11-03) a condition that reads a timeline on a rule with a timeline target is read per period in the rule's
  consequence (`PeriodCondition`), the producer's timeline cut to its periods, the whole-period qualifier there per
  bucket; two or more computing rules of one timeline target each write their own `Value`, and the write-back puts
  their union with the target's input on the bean (EF-016 on a day two decide differently)." In de lijst van
  `RETIRED`: "since the card `conditions-per-period` (2026-11-03) DROOLS-302".
- RS ch. 3.10, de alinea "Executed since 2026-09-23": "Both Drools branches refuse the generalised clip by name
  (DROOLS-103, DROOLS-105, DROOLS-302), so its goldens sit in their channel lists." → "Both Drools branches carry the
  generalised clip since step B of the same card, refusing by name a form they do not read per period (DROOLS-309) and
  a grid that does not fit (DROOLS-310)."
- RS ch. 3.11, opsommingspunt rekenen per periode: "Executed by `ref` and the ASP backend, plus, min and the sum per
  period without a condition also by both branches of the Drools backend" → "Executed by `ref`, the ASP backend and both
  branches of the Drools backend".
- RS ch. 3.12: rij "Plus, min and the sum per period …": "and refused by name in both Drools branches" → "and by both
  Drools branches"; rij "A condition on a rule with a timeline target that reads a timeline …": "both Drools branches
  refuse it by name (DROOLS-103, DROOLS-105, DROOLS-302)" → "and both Drools branches"; "Refused by name in both OCaml
  executors: …" → "Refused by name in every executor: …" (Drools: DROOLS-309; het venster DROOLS-104); "The total of a
  timeline written under such a condition is executed by `ref`" → "by `ref` and both Drools branches"; rij "Two or more
  writers of one timeline target": "both Drools branches refuse it by name (DROOLS-103)" → "and both Drools branches,
  which refuse a read of such a target by name (DROOLS-311)"; "and `ref` keeps it on the days no writer decides" → "and
  `ref` and both Drools branches keep it on the days no writer decides".
- RS ch. 5.7, "Two writers per day": geen wijziging nodig; eventueel achter de getuigen "executed by `ref`, the ASP
  backend and both Drools branches".
- `NOTE.md` van de drie goldens en de rode casus noemen Drools niet; niets te doen.

#### Voorlopige besluiten

- VB-D1 — De perioden van een voorwaarde per periode worden in de consequentie gerekend, niet in de linkerkant: de
  regel vuurt altijd, haar fouten en deposito's zijn die van de waarde (geen aparte fout- of depositoregel), en de
  evaluatievolgorde is die van ref: de voorwaarde vóór de waarde (`clipped(periods, timeline)`).
- VB-D2 — Twee schrijvers: elk schrijft zijn eigen `Value`; de vereniging bestaat alleen bij het terugschrijven, dus een
  lezing van zo'n doel weigert de tak bij naam (DROOLS-311; geen golden leest er een). Een emmerscalair als invoer van
  zo'n doel telt als geen invoertijdlijn, zoals ref. Bij drie of meer schrijvers die op dezelfde eerste dag botsen, noemt
  EF-016 het paar dat het document het eerst noemt (ref neemt het paar van de schrijver die als tweede schrijft; met twee
  schrijvers gelijk).
- VB-D3 — De zin van EF-016 voor tijdlijnen is die van de scalaire fout met de waarden van de eerste botsingsdag; de dag
  zelf staat niet in de zin (geen nieuwe catalogusplaats). De parameters zijn die van ref, `values` inbegrepen.
- VB-D4 — WF-126 als fail-fast: raster en vast startpunt van doel en gelezen tijdlijn met de regel van `grid_fits`; de
  kwalificator telt met zijn kalendermaand of -jaar. Een fijnere operand van rekenen per periode blijft DROOLS-308.
- VB-D5 — Een vergelijking per periode herrekent de grens naar de eenheid van de tijdlijn (ch. 2.6) en keert de operator
  om waar de tijdlijn rechts staat; datums en niet-getallen weigert de tak (DROOLS-309; ref vergelijkt ze, geen golden).
- VB-D6 — In de lezing leest een operand van rekenen per periode zonder tijdlijn `[]` en de getoetste tijdlijn van een
  vergelijking `empty`, zoals ref (`Whole (Some [])` tegen `Whole None`).

#### Wat deel ii voor Drools nog nodig heeft

- Het venster (punt 6): DROOLS-104 weigert elk venster in de voorwaarde van een tijdlijndoel. Met `PeriodCondition` is
  een venster een voorwaarde met de perioden [van, tot), dus een term naast `PERIOD_TRUE_PERIODS`; nodig voor
  `jutterswet-pachtvenster` (staat nu als `EXCLUDE DROOLS-104` in gate 12), voor een venster als lid van een samenstelling
  en als voorwaarde van rekenen per periode, en voor een dagprecieze datumexpressie als grens (WF-111 verruimd).
- `time-window-timeline-characteristic` als golden: een tijdsafhankelijk kenmerkdoel. Drools weigert dat doel bij naam
  (DROOLS-102) en heeft geen schrijver van kenmerktijdlijnen; dat is een vorm van zijn eigen, niet alleen een knip.
- 7a (0 in een emmer zonder ware dag): `rsAantalDagenIn` geeft geen segment voor een lege emmer; de dagentelling moet dan
  0 geven in elke emmer van alle tijd zonder ware dag (open segmenten van 0 vóór en na), en de knip en de twee
  schrijvers lezen dat mee (een 0 in een gehele maand botst met 30). Gate 17: de lezing van de dagentelling verandert mee.
- 7b (een dagsoorttoets op de getelde dag in de voorwaarde van de dagentelling) en 7c (de eenheid dg/mnd): `clipTimeline`
  neemt nu alleen een eigen tijdsafhankelijk kenmerk (DROOLS-114 en verwanten); de eenheid van de telling is een nieuwe
  regel in `timelineValue`.
- De goldens met een dagentelling (`anchor/jutterswet-dagentelling`, `jutterswet-dagentelling-rgs`,
  `jutterswet-vast-startpunt`, `jutterswet-pachtvenster`) krijgen nieuwe verwachtingen; zolang Drools de oude telling
  schrijft, worden ze in gate 12 een FAIL of een kanaalregel.

#### Open punten

1. Drie of meer schrijvers die op één dag botsen (VB-D2): geen golden, geen getuige.
2. Een sum per periode onder een voorwaarde per periode: de knoop van de som noteert zijn term, de lezing van zijn
   bijdragers is niet tegen ref gemeten (geen golden).
3. **clingo en ref verschillen op een voorwaarde per periode over een emmerscalair** (document
   `docs/a-emmerscalair`, verifier PASS): ref `EVALUATION FAULT: a condition per period reads the time-dependent
   attribute "strandloon" of "J1" whole; the case supplies only the asOfDate-bucket scalar, which carries no period`,
   Drools op beide takken dezelfde fout (`InputFormFault`), clingo `clingo: PASS (1 expectations)`. Geen golden heeft het;
   een fix in clingo (buiten mijn may-touch).
4. De tekstvoorstellen hierboven zijn niet aangebracht.

#### `make gates`

Groen op `0012df0ce` (gerebased op `1d7259100`): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 349 s
(`out/gates/20261103-150340/`, `logs/gates-1.log`). Gemeten: gate 12 `played matches the pinned 325`, de DRL-tekst-tak
325, kanaalvloer 47 met naam, JUnit 1816 tests (nul skips, floor 1573), `check-drools-codes` 311 codes, 19 retired;
gate 8 Drools-been 45 fault-gepind, 18 niet-gedragen, 16 EXCLUDE met code; gate 17 325 goldens en 921 casebestanden op
beide takken vergeleken, 0 afwijkingen; gate 13 groen.
