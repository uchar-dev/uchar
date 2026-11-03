# total-over-partial-unit: het totaal telt een gedeeltelijke eenheid naar rato (impliciete tijd, punt 12)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` en het voorstel
`spec/proposals/IMPLICIT-TIME.md`, punt 12. Na `20261103-timeline-arithmetic-per-period` (de kalender van punt 11). Besluiten van
de eigenaar 2026-11-03: **naar rato** (12a): het totaal van ch. 3.6 telt een gedeeltelijke eenheid naar rato van haar werkelijke
lengte (dagen gedeeld door de dagen van de omsluitende kalendereenheid; ALEF `timesDuration`, `partialLength`), een gevolgde
afwijking van de vloer per fragment van de gespiegelde runtime; **de gewone operand houden** (12b), gelezen als "per eenheid van
het raster", een gevolgde afwijking van RegelSpraak §7.1. Besloten (spec spreekt, 12c): een afwezige tijdlijn telt 0.*

Goal: ch. 3.6 en alle uitvoerders tellen een gedeeltelijke eenheid naar rato, lezen een gewone operand per rastereenheid en
tellen een afwezige tijdlijn 0, met beide afwijkingen in `spec/CONSTRUCTS.md` rij 11.

Acceptance:
- (a) Rood eerst met de voorbeelden `WV-3.6-naar-rato` van het voorstel: 140 €, 100/61 € en 100 €, waar de vloer 0 geeft.
- (b) Golden `jutterswet-vast-startpunt` case 1: `toelagesom` wordt 100 × 3/366 + 100 × 2/365 = 6090/4453; dat past niet in het
  gehele doel (EF-006), dus het doel krijgt de onbegrensde vorm of een afronding (noem de keuze en oud en nieuw). De `ref`-test
  `verify-clip-per-fragment-floor` gaat mee, met oud en nieuw. Meet elke andere golden met een totaal over een geknipte tijdlijn
  en noem elke verandering.
- (c) RS ch. 3.6 en 3.12 (voorstel), het residu "afwezige tijdlijn telt leeg" vervalt (12c).
- (d) ref, clingo en beide Drools-branches; gate 17 gelijk.
- (e) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `backends/drools/**`, `spec/RUNTIME-SEMANTICS.md` (voorstel),
`corpus/golden/jutterswet-vast-startpunt/` en andere goldens met een totaal over een geknipte tijdlijn (verwachtingen),
`scripts/check-drools.sh` (voorstel), kaartverplaatsing.

Out of scope: de kalenderconventie zelf (in `20261103-timeline-arithmetic-per-period`).

## Omvang

Middel.

## Result: stap A (bouwer, 2026-11-03)

Branch `card/total-over-partial-unit`, gestart op main `8811de8cf`, gerebased op main `5c1ccaa47` (de landing van
`conditions-per-period`) en daarna op `57f1411b5` (vier kaarten in `backlog/new/`), beide zonder conflict. Niet gemerged, niet gepusht. Beschermd in deze branch, dus **voorstel**:
`spec/RUNTIME-SEMANTICS.md`, `scripts/check-drools.sh`, `scripts/check-readings.sh`. Logs onder
`werk/total-over-partial-unit/logs/`.

### Commits

- `97f5cf4fa` kaart naar `doing/`
- `68e43d61b` ref: het totaal telt een gedeeltelijke eenheid naar rato (met `WV-3.6-naar-rato`, fixtures `partial-unit`)
- `7c7046ed2` clingo: lengtes van spannen met een noemer, een afwezig totaal is 0 (README [V8])
- `820cf3d21` golden: vijf goldens (verwachtingen, NOTEs; `jutterswet-vast-startpunt` ook bron en IR)
- `cce192a63` spec: RS ch. 3.1, 3.2, 3.6, 3.12 (voorstel); `corpus/golden/README.md`; twee ref-voorbeelden van de maat
- `234874de5` gates 12 en 17: de waardeverschillen en afwijkingen (voorstel)
- `8ea501344` tests: fixtures `partial-unit/calendar` en `partial-unit/absent-written` in beide suites
- `3c6b1aa0d` drools-tests: hele eenheden en een afwezig totaal leeg als bekend verschil (alleen `src/test`)
- het commit van dit Result

### De meting op main (letterlijk)

- Rood eerst, ref-suite met de drie rijen van `WV-3.6-naar-rato` op de code van `8811de8cf`
  (`logs/03-ref-suite-red.log`): `FAIL WV-3.6-naar-rato-alef (got 0)`, `FAIL WV-3.6-naar-rato-jaar (got 0)`,
  `FAIL WV-3.6-naar-rato-maand (got 0)`, `3 failure(s)`.
- De goldens en fixtures van de branch door ref en clingo van main `5c1ccaa47` (`logs/19-main-measure.log`):
  - `jutterswet-getijdenwacht`: ref `MISMATCH: J4.premiesom = 0, expected 10`; clingo `MISMATCH: J4.premiesom = [0], expected 10`.
  - `jutterswet-kadetarief`: ref `MISMATCH: H2.havengeld per meter = leeg, expected 0`; clingo `… is underived (leeg), expected 0`.
  - `jutterswet-penningjaar`: ref `MISMATCH: J3.totaal aangemelde vondsten = leeg, expected 0`; clingo idem underived.
  - `jutterswet-tempototaal`: ref `MISMATCH: J2.bewaargeld = leeg, expected 0`; clingo idem underived.
  - `jutterswet-vast-startpunt` case 1: ref `MISMATCH: J1.toelagesom = 0, expected 1.37`; clingo `= [0], expected 137`;
    case 4: maandtoelagesom, weektoelagesom, kwartaaltoelagesom `= leeg, expected 0` (clingo underived); case 6:
    maandtoelagesom idem; case 2, 3, 5, 7 PASS.
  - fixture `partial-unit/exact` case 1: ref en clingo `J1.wachtsom = 30, expected 45`, `J1.premiesom = 30, expected 45`;
    case 2: `J1.wachtsom = 0, expected 30`.
  - fixture `partial-unit/unrounded`: ref `MISMATCH: J1.jaarsom = 0, expected 100/61`; clingo-runner
    `TRANSLATION FAULT: expected value does not fit the declared decimals` (de verwachte 100/61 zelf).
  - fixture `partial-unit/calendar`: ref `MISMATCH: J1.dagsom = 0, expected 340`; clingo `= [0], expected 34000`.
  - fixture `partial-unit/absent-written`: ref `MISMATCH: J1.jaarloon = leeg, expected 0`; clingo PASS; Drools `PLAYED 1`.
    Een bestaande afwijking van ref: RS ch. 3.6 zei al 0 voor een afwezige tijdlijn van een doel van ch. 3.4/3.5
    (VB-A4 van `timeline-arithmetic-by-rule`), clingo en Drools gaven 0, ref gaf leeg (`eval.ml`, `None -> Empty`).
- Drools, beide takken, op de veranderde goldens na de wijziging (`logs/17-drools-changed.log`), per golden dezelfde
  regel in beide takken: `FAIL case.json: attribuut premiesom: verwacht 10, werkelijk 0` (getijdenwacht);
  `… havengeld per meter: verwacht 0, werkelijk leeg` (kadetarief); `… totaal aangemelde vondsten: verwacht 0, werkelijk
  leeg` (penningjaar); `… bewaargeld: verwacht 0, werkelijk leeg` (tempototaal); vast-startpunt `case1: toelagesom:
  verwacht 1.37, werkelijk 0.00 | case4: maandtoelagesom, weektoelagesom, kwartaaltoelagesom verwacht 0, werkelijk leeg |
  case6: maandtoelagesom verwacht 0, werkelijk leeg`.

### Wat er veranderde

- **ref** (`Timeline`): `grid_measure` meet een span in eenheden van het raster: een dag telt 1/len van de periode van
  het raster die hem bevat (op de kalender de kalendereenheid via `calendar_measure`, met een vast startpunt de periode
  van `grid_bucket`; een raster van meer perioden of een variabel startpunt weigert `grid_bucket` bij naam).
  `integrated_segment` gebruikt die lengte in plaats van `whole_units`, voor de gewone operand (12b), de noemer van
  het raster en een gedeclareerde factor binnen een familie; de kalender meet de hele span in plaats van de hele
  rastereenheden (`whole_units_end` is weg). `grand_total` krijgt het raster (`Timeline.grid`) in plaats van de
  granulariteit; de vier aanroepen in `Eval` geven `attr_grid`. Een afwezige tijdlijn telt 0 op elke route: het
  rekendoel van ch. 3.4/3.5 (was leeg), de emmer van de peildatum (was leeg) en een lid van de vouw per element (was
  geen bijdrager).
- **clingo**: `span_weights` (vervangt `calendar_weights`) legt de gewichten van alle spannen tussen de eindpunten
  met een meegegeven maat (`grid_measure` of de kalendermaat over de hele span) over D, het kgv van hun noemers; met
  D = 1 dezelfde feiten en grenzen als voorheen. Met D > 1: onder een afronding schaal × D (zoals de kalender), anders
  `exact_span_total`: deling door D met een getuige `viol("… counts a part of a unit of its grid by its days …")` waar
  D het totaal niet deelt (README [V8]); de bovengrens op W is dan 2^31−1 over het gewicht van de hele span (de
  per-gewichtgrens 46340 van de kalender verviel, die grens dekt het product al). Toegepast in de klip en het totaal
  van een rekendoel; de vouw per element weigert een lidsegment dat geen geheel aantal rastereenheden is (onbereikbaar:
  de lader legt elk segment op het raster). Het totaal over de emmer van de peildatum telt een afwezige tijdlijn 0
  (`#sum` over de waarde), en de aanwezigheidswacht van een `de som van` per element telt leden, niet tijdlijnen.
- **spec** (voorstel): RS ch. 3.6 (de lengte, de klip zonder vloer, 12b met de reden, 12c op elke route, de vouw per
  element, de kalender over de hele span, de uitvoerdersnoot, voorbeelden `WV-3.6-naar-rato`), ch. 3.2 job 1 (weging
  op het raster), ch. 3.1 (de v0-noot), ch. 3.12 (rij "Gap-vs-absent" weg, nieuwe rij met de open precisiegrens).
  `corpus/golden/README.md` (penningjaar). `ref/README.md`, `backends/clingo/README.md` ([V6] D = kgv van de noemers,
  [V8] nieuw).
- **corpus**: vijf goldens (hieronder); `jutterswet-vast-startpunt`: `toelagesom` van `geheel getal` naar `getal met 2
  decimalen` en de regel `… rekenkundig afgerond op 2 decimalen`, IR door de frontend opnieuw geschreven.
- **Drools** (alleen `src/test`): `TmsCorpusWitnessTest` en `DrlTextCorpusWitnessTest` kregen de lijst
  `TOTAL_OVER_PARTIAL_UNIT` (de vijf goldens; rood als er een speelt of een andere bijkomt), naar het patroon van
  `c75ef4255`; `MorePeriodicGateWitnessTest.the_total_of_a_time_sequence_plays_at_count_one` en
  `TimelineDateFoldWitnessTest.the_numeric_originals_stay_playing` (beide met de case van tempototaal als voertuig):
  `playBothBranches`/`BothBranches.green` → `BothBranches.red` met de hele regel `case: attribuut bewaargeld: verwacht 0,
  werkelijk leeg` als `assertEquals`. Geen productiecode in `backends/drools`.

### Veranderde verwachtingen, oud → nieuw

- `jutterswet-getijdenwacht` J4.premiesom: 0 → **10** (12a: de twee perioden die 2023 splitsen, 181/365 + 184/365).
- `jutterswet-vast-startpunt` case 1 J1.toelagesom: 0 → **1.37** (12a: 100 × 3/366 + 100 × 2/365 = 6090/4453, afgerond;
  doel `geheel getal` → `getal met 2 decimalen` met een afronding). Case 4 maandtoelagesom, weektoelagesom,
  kwartaaltoelagesom: leeg → **0**; case 6 maandtoelagesom: leeg → **0** (12c).
- `jutterswet-kadetarief` H2.havengeld per meter: leeg → **0**; `jutterswet-penningjaar` J3.totaal aangemelde vondsten:
  leeg → **0** (de drempeltoets blijft onwaar); `jutterswet-tempototaal` J2.bewaargeld: leeg → **0** (12c).
- Gemeten en onveranderd (alle 1058 golden-cases in ref en clingo): de andere totalen over een geknipte of afwezige
  tijdlijn in `anchor/jutterswet-dubbelwacht`, `anchor/jutterswet-verzuimrente-30-360`,
  `anchor/jutterswet-verzuimrente-dagen`, `jutterswet-getijdenwacht` (wachtsom, topsom), `jutterswet-kadetarief`
  (kadeopbrengst), `jutterswet-keurlast`, `jutterswet-klipwacht`, `jutterswet-maandloon`, `jutterswet-peilwacht`,
  `jutterswet-rekenen-per-dag`, `jutterswet-tegenwacht`, `jutterswet-tempovouw`, `synth2-clip-cond-over-prorata`,
  `synth2-clip-over-prorata` (scan `logs/02-scan-totals.log`); rood `jutterswet-totaal-open-segment` onveranderd.
- ref-tests die de kaart omdraait: `verify-clip-per-fragment-floor` 0 → `verify-clip-per-fragment-naar-rato` **100**
  (naam mee, de vloer bestaat niet meer); `WV-3.6-h-afwezig-som` leeg → **0** (VB-2). Nieuw: `WV-3.6-naar-rato-alef`
  (140 €), `-jaar` (100/61 €), `-maand` (100 €), `-vast-startpunt` (6090/4453), `-anderhalve-maand`,
  `-gesplitste-maand`, `-onafgerond`, `-kalender`, `WV-3.6-afwezig-geschreven`, `WV-3.2-weging-maat-vast`,
  `WV-3.2-weging-maat-kalender`. Suites: ref 2287 → 2303 voorbeelden, clingo 2443 → 2455 checks.

### Rood en mutaties (niet gecommit; `logs/22-mutations.log`, `24-*`, `25-*`, `26-*`)

| # | mutatie | getuige | uitkomst |
|---|---|---|---|
| R1 | ref: hele eenheden in plaats van de lengte | ref-suite, getijdenwacht | 8 failures (`WV-3.6-naar-rato-*`, `verify-clip-per-fragment-naar-rato`); `J4.premiesom = 0, expected 10` |
| R2 | ref: elk raster gemeten als de kalender | ref-suite, vast-startpunt case 5 | `WV-3.6-naar-rato-vast-startpunt`, `WV-3.2-weging-maat-vast`, `WV-3.2-vast-peildatum-kwartaal-30`; `EF-006: J1.maandtoelagesom: value 8970/899` |
| R3 | ref: kalender over de hele rastereenheden | ref-suite | 7 failures (`WV-3.6-kalender*`, `WV-3.6-naar-rato-kalender`: dagsom 0) |
| R4 | ref: afwezig rekendoel leeg | ref-suite | `WV-3.6-afwezig-geschreven (J1.jaarloon = leeg, expected 0)`; vóór die fixture groen, daarom kwam ze |
| R5 | ref: afwezige emmer leeg | tempototaal | `MISMATCH: J2.bewaargeld = leeg, expected 0` (de ref-suite bleef groen; de goldens van gate 5 vangen het) |
| R6 | ref: afwezig lid leeg | ref-suite | `WV-3.6-h-afwezig-som (got leeg)` |
| C1 | clingo: klip met hele eenheden | getijdenwacht, exact case 2 | `J4.premiesom = [0], expected 10`; `J1.wachtsom = [0], expected 30` |
| C2 | clingo: geen getuige als D niet deelt | clingo-suite | `README [V8]: … clingo no longer refuses` |
| C3 | clingo: D niet weggedeeld | getijdenwacht, exact case 1 | `J4.premiesom = [1335900]`; `J1.wachtsom = [90]` |
| C4 | clingo: afronding zonder D in de schaal | vast-startpunt case 1 | `J1.toelagesom = [6090000], expected 137` |
| C5 | clingo: rekendoel met hele eenheden | exact case 1 | `J1.premiesom = [30], expected 45` |
| C6 | clingo: afwezige emmer underived | tempototaal, vast-startpunt case 4 | `J2.bewaargeld is underived (leeg)`; de drie sommen idem |
| C7 | clingo: somwacht op tijdlijnen | clingo-suite | `§7.1 per-element som …: J1.bewaarlast is underived (leeg), expected 0` |
| C8 | clingo: kalender over de hele rastereenheden | fixture calendar | `J1.dagsom = [0], expected 34000` |

Niet gemuteerd: de weigering van een lidsegment dat geen geheel aantal eenheden is in de vouw per element van clingo
(onbereikbaar vanuit een case, de lader legt het segment op het raster); de grens op W bij D > 1 (dezelfde formule als
de kalender al had).

### Pins en lijsten, oud → nieuw

- Gate 12 (voorstel): `EXCLUDE_KNOWN` 0 → 5 regels (getijdenwacht "12a: naar rato; Drools volgt in stap B"; kadetarief,
  penningjaar, tempototaal "12c: een afwezige tijdlijn telt 0; Drools volgt in stap B"; vast-startpunt beide);
  `EXPECTED_CHANNEL` 46 → 51 (vijf `DIVERGE`-regels); `EXPECTED_PLAYED` 328 → 323 met een regel in het commentaarblok.
- Gate 17 (voorstel): `DEVIATIONS` 0 → 24 regels: de twaalf casebestanden van de vijf goldens (vast-startpunt case 1–6,
  case 7 leest alle sommen gevuld) en van `jutterswet-muntbewaring` en `jutterswet-tempovouw`, waar een lid zonder
  tijdlijn nu als bijdrager 0 in de trace staat (VB-2), elk op `exec` en `drl`.
- Drools-tests: de lijst `TOTAL_OVER_PARTIAL_UNIT` in twee getuigen, twee getuigen naar `BothBranches.red` (hierboven).
  JUnit 1826 tests, ongewijzigd.
- Geen floor veranderd. Geen nieuwe EF-, REF-, CLINGO-, RGS- of DROOLS-code; de getuige [V8] heeft geen code (een grens
  van de codering, zoals "arithmetic range exceeded").

### Voorgestelde CONSTRUCTS-tekst (rij 11, achteraan)

> **Landed (2026-11-03, card total-over-partial-unit, step A):** the temporal total of RS ch. 3.6 measures a segment in
> units of its grid: a day counts 1/len of the period of the grid that holds it, so a part of a unit counts by its days,
> per clipped fragment, on a fixed start point's own periods (maintainer decision 2026-11-03,
> `spec/proposals/IMPLICIT-TIME.md` point 12a; ALEF `partialLength`); a plain operand reads per unit of the grid (12b);
> an absent timeline totals 0 on every route, a member of the per-element fold included (12c, spec typ. §4.20).
> Executed by ref and the ASP backend (the latter over a denominator D it divides away under a rounding or exactly,
> refusing at solving by name a total D does not divide, `backends/clingo/README.md` [V8]); witnesses
> `jutterswet-getijdenwacht` (J4), `jutterswet-vast-startpunt` and the absent totals of `jutterswet-kadetarief`,
> `jutterswet-penningjaar` and `jutterswet-tempototaal`; suite fixtures `partial-unit`. The Drools backend counts whole
> units per fragment and totals an absent timeline leeg until step B (value divergences in gate 12). Tracked v1 deltas:
> v0 floors whole units per raw clipped fragment and totals an absent timeline leeg; §7.1 and typ. §4.20 ask the operand
> of the total for a unit per time unit, where v1 keeps the plain operand, read per unit of the grid.

### Voorgestelde tekst elders (buiten mijn may-touch, voor de hoofdsessie)

- `spec/ASP-TRANSLATION.md` (r. 1268–1275, de klip): "the calendar as *data* (`clN_wu(V,T,N)`: whole units per clipped
  span)" → "the length of each candidate span as *data* (`clN_wu(V,T,N)`, a part of a unit by its days, over a
  denominator D where a span holds a part; `backends/clingo/README.md` [V8])"; "the clip's presence turns absence into 0
  (the mirrored v0 nuance, RS ch. 3.6)" → "0, as every total of an absent timeline (RS ch. 3.6)"; na "Fences: |W| ≤
  46340, the endpoint span at 46340 whole units": "; with D > 1 the guard on W is 2^31 − 1 over the weight of the full
  span".
- `CLAUDE.md`, gate 17: "(empty since the gate began)" → "(24 entries since card total-over-partial-unit step A, until
  its step B)"; gate 12 noemt geen aantallen van de lijst.

### Voorlopige besluiten

- VB-1 — De lengte meet op de perioden van het raster van het getotaliseerde attribuut: op de kalender de
  kalenderdag, -maand, -kwartaal of -jaar (een week 1/7 per dag, waar ze ook begint), met een vast startpunt de periode
  die het startpunt legt (`jutterswet-vast-startpunt` case 1: het stormjaar van 366 en dat van 365 dagen, niet het
  kalenderjaar). Een raster van meer perioden en een variabel startpunt laat ref door `grid_bucket` bij naam weigeren;
  geen golden totaliseert er een (scan).
- VB-2 — 12c op elke route, ook voor een lid van de vouw per element: een lid zonder tijdlijn draagt 0 bij (het totaal
  van zijn afwezige tijdlijn), dus `de som van` over leden zonder tijdlijn is 0 en alleen over de lege verzameling leeg.
  Gevolg: de trace van ref noemt zo'n lid als bijdrager (gate 17, muntbewaring en tempovouw). Het alternatief, de vouw
  per element ongemoeid (een lid zonder tijdlijn draagt niets bij), verandert geen golden-waarde en geen lezing; dan
  gaan `WV-3.6-h-afwezig-som`, de clingo-somwacht en de vier DEVIATIONS-regels terug.
- VB-3 — `jutterswet-vast-startpunt` krijgt een afronding op 2 decimalen (1,37), niet de onbegrensde vorm: de
  ASP-codering houdt 6090/4453 niet (zoals VB-B1 van `timeline-arithmetic-per-period`).
- VB-4 — Binnen een familie blijft de gedeclareerde factor, toegepast op de lengte in rastereenheden: €/jr over een
  maandraster met jr = 12 mnd telt een deel van een maand als dagen/dagen-van-de-maand × 1/12, niet als dagen/365.
- VB-5 — clingo: D is het kgv van de noemers van de werkelijke spanlengtes (ook voor de kalender, die het kgv van de
  ontmoete lengtes nam; dezelfde uitkomsten, kleinere gewichten). Zonder afronding deelt de codering exact en weigert
  bij het oplossen waar D niet deelt, in plaats van bij de vertaling zoals de kalender (zo speelt getijdenwacht J4 =
  10 zonder afronding).
- VB-6 — De Drools-getuigen die de veranderde goldens spelen, kregen het bekende verschil bij naam of als letterlijke
  regel (het patroon van `c75ef4255` uit `conditions-per-period`), zodat gate 12 groen is tot stap B; geen
  productiecode van Drools aangeraakt.

### Open punten

1. **De precisiegrens** (RS ch. 2.3, tabelrij "totaal over a timeline"; WF-023; de precisiepoort van de frontend) geeft
   een totaal de decimalen van zijn operand, waar een deel van een eenheid elke breuk kan geven. Gemeten:
   `jutterswet-vast-startpunt` case 1 zonder afronding is verifier-PASS en geeft in ref `EVALUATION FAULT: EF-006:
   J1.toelagesom: value 6090/4453 does not fit the declared 0 decimals — an explicit rounding is mandatory (ch. 2.4)`.
   Een kaart voor verifier en frontend (de grens ∞ voor een totaal dat een eenheid kan splitsen, behalve op een
   dagraster); de nieuwe rij van RS ch. 3.12 noemt het OPEN.
2. De emmer van de peildatum vraagt in ref en clingo nog een peildatum vóór ze de afwezigheid ziet ("needs the
   evaluation date"); 12c zou zonder datum 0 kunnen geven. Niet veranderd.
3. De tekstvoorstellen hierboven zijn niet aangebracht.

### Wat stap B (Drools) moet weten

- Vijf goldens in `EXCLUDE_KNOWN`/`EXPECTED_CHANNEL` (`DIVERGE`), met de FAIL-regels van de meting hierboven; speelt
  Drools ze, dan `EXPECTED_PLAYED` 323 → 328, de vijf regels weg en de 24 `DEVIATIONS` van gate 17 weg, en in de
  Drools-tests de lijst `TOTAL_OVER_PARTIAL_UNIT` (twee getuigen) en de twee `BothBranches.red` terug naar groen.
- De vormen: het totaal meet elk (geknipt) segment per dag op de perioden van zijn raster, ook met een vast startpunt
  (`rsTijdTotaal` en `PeriodArithmetic.total`/`measure` tellen nu hele eenheden); de kalender meet de hele span, niet
  alleen de hele rastereenheden; vast-startpunt case 1 vraagt 6090/4453 onder de afronding (Drools gaf 0.00).
- 12c: het totaal over de emmer van de peildatum telt een afwezige tijdlijn 0 (Drools leeg: kadetarief, penningjaar,
  tempototaal, vast-startpunt case 1–6); het rekendoel en de klip gaven al 0; in de vouw per element is een lid zonder
  tijdlijn een bijdrager met 0 (lezingen van muntbewaring en tempovouw) en `de som van` over alleen zulke leden 0.
- Penningjaar leest het totaal ook in een voorwaarde: de lezing van rule 1 voor J3 wordt 0 in plaats van leeg.

### `make gates`

Groen op `6ee5c45d0`, de stand van `3c6b1aa0d` vóór de laatste rebase (die voegt alleen de vier kaarten van
`57f1411b5` in `backlog/new/` toe, die geen gate leest): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 356 s
(`out/gates/20261103-172903/`):

```
01-self-contained rc=0 2s     02-ir-schema rc=0 131s   04-case-schema rc=0 8s   03-verifier rc=0 40s
05-oracle rc=0 23s            06-clingo rc=0 289s      07-noop rc=0 14s         08-red rc=0 155s
09-catala rc=0 5s             10-regelspraak rc=0 250s 11-toka-parity rc=0 225s 12-drools rc=0 354s
13-reftype rc=0 320s          14-flogic rc=0 2s        15-graph rc=0 50s        16-scenario rc=0 37s
17-readings rc=0 264s         after-run rc=0 0s        end 2026-11-03 17:34:59 356s red=0
```

Gemeten: gate 5 ref-suite 2303 voorbeelden, gate 5 en 6 1058 cases ref = clingo; clingo-suite 2455 checks; gate 8 63
rode cases, Drools-been 46 fault-gepind, 15 EXCLUDE met code; gate 12 `played matches the pinned 323`, value-divergence
5, kanaalvloer 51 met naam, JUnit 1826 tests (nul skips, floor 1573); gate 17 328 goldens en 919 casebestanden op beide
takken vergeleken, 24 afwijkingen gelijst. De eerste run (`logs/gates-1.log`, op `234874de5`) was rood in gate 12: vier
Drools-tests speelden de veranderde goldens (`3c6b1aa0d` lost dat op). De kaart blijft in `doing/` voor stap B.
