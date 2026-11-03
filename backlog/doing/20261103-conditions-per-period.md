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
