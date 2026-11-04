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

## Result: stap B (bouwer, 2026-11-04)

Branch `card/total-over-partial-unit`, gestart op main `a956aaa45`, gerebased op main `7e16cb14c` (de fixronde
time-followups; één conflict in `ClosureRendering`, de regels van `OWN_TOTAL` en `COMPLETE`, beide wijzigingen
gehouden). Niet gemerged, niet gepusht. Beschermd in deze branch, dus **voorstel**: `scripts/check-drools.sh`,
`scripts/check-readings.sh`, `spec/RUNTIME-SEMANTICS.md`. Logs onder `werk/total-over-partial-unit-drools/logs/`.

### Commits

- `a259d724a` drools: het totaal meet een deel van een eenheid naar zijn dagen (productiecode, tests, README)
- `c599c699f` gates 12 en 17: Drools telt een deel van een eenheid en een afwezig totaal mee (voorstel)
- `080eabf30` spec: RS ch. 3.6 en 3.12, Drools telt het totaal als ref (voorstel)
- het commit van dit Result

### Rood eerst (op `a956aaa45`, `logs/01-red-five-goldens.log`, beide takken dezelfde regel)

| golden | DIVERGE-regel van gate 12 (letterlijk) | uitkomst van beide takken |
|---|---|---|
| `jutterswet-getijdenwacht` | `12a: naar rato; Drools volgt in stap B (EXCLUDE_KNOWN)` | `FAIL case.json: attribuut premiesom: verwacht 10, werkelijk 0` |
| `jutterswet-kadetarief` | `12c: een afwezige tijdlijn telt 0; Drools volgt in stap B (EXCLUDE_KNOWN)` | `FAIL case.json: attribuut havengeld per meter: verwacht 0, werkelijk leeg` |
| `jutterswet-penningjaar` | `12c: een afwezige tijdlijn telt 0; Drools volgt in stap B (EXCLUDE_KNOWN)` | `FAIL case.json: attribuut totaal aangemelde vondsten: verwacht 0, werkelijk leeg` |
| `jutterswet-tempototaal` | `12c: een afwezige tijdlijn telt 0; Drools volgt in stap B (EXCLUDE_KNOWN)` | `FAIL case.json: attribuut bewaargeld: verwacht 0, werkelijk leeg` |
| `jutterswet-vast-startpunt` | `12a: naar rato; Drools volgt in stap B; 12c: een afwezige tijdlijn telt 0; Drools volgt in stap B (EXCLUDE_KNOWN)` | `FAIL case1-stormjaargrens.json: attribuut toelagesom: verwacht 1.37, werkelijk 0.00 \| case4-geen-wacht.json: attribuut maandtoelagesom: verwacht 0, werkelijk leeg; attribuut weektoelagesom: verwacht 0, werkelijk leeg; attribuut kwartaaltoelagesom: verwacht 0, werkelijk leeg \| case6-heel-kalenderjaar.json: attribuut maandtoelagesom: verwacht 0, werkelijk leeg` |

Na de wijziging (`logs/05-green-goldens.log`), beide takken: `PLAYED 1`, `1`, `1`, `1`, `7`; `jutterswet-muntbewaring`
en `jutterswet-tempovouw` `PLAYED 1`, en hun `--readings` zijn op de kaderregels na gelijk aan de trace van ref.

### Wat er veranderde

- **De lengte** (`PeriodArithmetic`): `weighed(segments, granularity, fixed, calendar)` telt elk segment als waarde ×
  lengte, exact (een decimaal waar die eindigt, anders een `Breuk`). De lengte is `gridMeasure(granularity, fixed, …)`:
  op de kalender `measure` (dag 1, week 1/7 per dag, maand, kwartaal en jaar 1/len van de kalendereenheid), met een
  vast startpunt per periode van `FixedStartPoint.period`, dezelfde functie waarmee `CaseRunner.bucket` de emmer legt.
  Met een kalendernoemer meet `measure(calendar, …)` de hele span in plaats van tot het eind van de hele rastereenheden.
  Een open segment in `weighed` is een gebroken contract (`IllegalStateException`): `total` en de bijdrage halen ze er
  eerst uit.
- **De routes**: `PeriodArithmetic.total` (de klip en de tijdlijn die rekenen per periode schrijft) en
  `ComputeHelpers.ownTotal` (de emmer van de peildatum) krijgen het vaste startpunt als constante mee
  (`TmsTranspiler.ownTotal`); `ownTotal` geeft 0 voor een afwezige invoer (was leeg). De vouw per element:
  `Consequence.Input.MemberValue` kreeg de component `fixed`, `ComputeHelpers.contribution` meet met `weighed` en geeft
  een lid zonder tijdlijn de bijdrage 0 (was geen bijdrage), zodat `de som van` over alleen zulke leden 0 is (VB-2 van
  stap A). `DrlRendering` en `PatternDslRendering` geven `fixed` door; `ClosureRendering` en `StaticFunction` volgen de
  signaturen.
- **De runtime**: `rsTijdTotaal` en `heleMaanden` zijn weg uit `RegelSpraakRuntime`, met `StaticFunction.RS_TIME_TOTAL`
  en zijn regel in `ClosureRendering` (VB-B1).
- **Documentatie**: javadoc van `TmsTranspiler` (vouw, eigen totaal, klip, `integrable`, `calendar`), `CaseInput`,
  `Period`; `backends/drools/README.md` (nieuwe alinea "The length of a segment", de kalender, de vouw per element).
- **Spec** (voorstel, in de branch): RS ch. 3.6 uitvoerdersnoot en de rij van ch. 3.12 (tekst hieronder).
- Geen nieuwe weigercode: niets in deze kaart weigert de tak.

### Tests, oud → nieuw

- Door de kaart omgedraaid:
  - `PeriodArithmeticTest.the_calendar_integrates_between_the_day_and_the_month_family`: 10 €/dg over
    [2024-01-10, 2024-01-20) op een maandraster: **0 → 100** (de kalender over de hele span); "without a calendar the
    whole units of the grid" heet nu "the length in units of the grid" (120, onveranderd).
  - `TimeTotalClampWitnessTest` verhuisd van `runtime/` naar `transpiler/tms/` en meet nu met `PeriodArithmetic.weighed`
    op het raster van het vaste startpunt (`--01-31`, `--05-31`, `--02-29`) in plaats van `rsTijdTotaal` zonder raster;
    de twee tests van een periode tussen geklemde muren blijven 10, 10, 10 en 30, 100, die van de duur 0 en 10. `a_day_too_short_is_no_whole_unit`
    → `a_part_of_a_period_counts_its_days`: [2024-01-31, 2024-02-28) **0 → 280/29**, [2024-02-29, 2025-02-27)
    **0 → 7280/73**, twee perioden 20 → 20, en nieuw de kalendermaand: 8970/899.
  - `PeriodTest.the_runtime_computes_with_a_period_as_segment_class`: de regel `rsTijdTotaal(timeline, "MAAND") = 21`
    → `rsOoitWaarden(timeline) = {10, 11}` (de runtime leest de waarde van een `Period`; `rsTijdTotaal` is weg).
  - `TmsInputFormWitnessTest.the_own_total_without_clip_takes_the_bucket_scalar`: J4 (geen bedrag) topsom **leeg → 0**.
  - `TmsTotalEvaluationDateWitnessTest`: J3 (geen bedrag, met datum) topsom **leeg → 0**.
  - `TmsTimelineFoldWitnessTest.a_total_and_a_sum_per_member_over_a_derived_timeline…`: J3 (alleen V4 zonder tarief)
    som **leeg → 0** (VB-2); de javadoc noemde dit vroeger de mutatie die rood moest maken. Ref op de documenten die
    `main` schrijft: `oracle: PASS (11 expectations)` (`logs/08-ref-on-witness-docs.log`, ook de twee andere getuigen:
    `PASS (4 expectations)` en `PASS (3 expectations)`).
  - Signaturen: `FormsPlan` (`MemberValue` met `fixed`, DRL-fragment `"\"MAAND\", null, true, false)"`),
    `RouteWitnessTest` (`contribution[…, String, String, boolean, boolean]`).
- Terug naar groen (de pins van stap A): de lijst `TOTAL_OVER_PARTIAL_UNIT` uit `TmsCorpusWitnessTest` en
  `DrlTextCorpusWitnessTest`; `MorePeriodicGateWitnessTest.the_total_of_a_time_sequence_plays_at_count_one` en
  `TimelineDateFoldWitnessTest.the_numeric_originals_stay_playing` weer `playBothBranches`/`BothBranches.green`. De vier
  bestanden zijn gelijk aan hun stand vóór `3c6b1aa0d`.
- Nieuw: `PeriodArithmeticTest.a_part_of_a_unit_counts_by_its_days` (`WV-3.6-naar-rato`: 140, 100/61, 100, 45,
  6090/4453 op het stormjaar, een week van het weekraster vanaf 2024-09-30, 3/7 van een kalenderweek) en
  `an_absent_timeline_totals_zero` (`total`, `ownTotal`, `contribution`);
  `TmsTimelineFoldWitnessTest.a_total_and_a_sum_per_member_on_a_fixed_start_grid_in_every_order_of_the_rules` (de vouw
  per element over leden op het stormjaarraster `--10-01`, beide takken: J1 22/22, J2 met één lid zonder tijdlijn 0/0,
  J3 zonder leden 0/leeg; verifier `PASS`, ref `oracle: PASS (7 expectations)`).
- JUnit: gate 12 meldt na de rebase 1831 tests, nul skips (1826 op `a956aaa45`; deze branch voegt er drie toe, de
  rest komt van main).

### Mutaties (niet gecommit; `logs/12-mutations.log`, `logs/mut-*`, in een losse worktree)

| # | mutatie | rood |
|---|---|---|
| M1 | kalenderraster: hele eenheden (de lengte naar beneden afgerond) | `PeriodArithmeticTest.a_part_of_a_unit_counts_by_its_days: expected: <140/1> but was: <0/1>`; getijdenwacht beide takken `FAIL case.json: attribuut premiesom: verwacht 10, werkelijk 0` |
| M2 | het vaste startpunt genegeerd, elk raster als de kalender | `PeriodArithmeticTest` (`expected: <6090/4453> but was: <250/183>`), alle vier `TimeTotalClampWitnessTest` (`expected: <10/1> but was: <8970/899>` …), de nieuwe vouwgetuige (`J1.last: de waarde 1469398/66795 past niet …`); vast-startpunt beide takken `ERROR WriteBoundaryFault: … J1.maandtoelagesom: value 8970/899 does not fit the declared 0 decimals` |
| M3 | de kalender meet 0 van een segment korter dan een rastereenheid | `the_calendar_integrates_between_the_day_and_the_month_family: expected: <100/1> but was: <0/1>` |
| M4 | eigen totaal van een afwezige invoer leeg | `TmsInputFormWitnessTest` (`case: attribuut topsom: verwacht 0, werkelijk leeg`), `TmsTotalEvaluationDateWitnessTest`; tempototaal, kadetarief, penningjaar beide takken hun FAIL-regel van hierboven |
| M5 | een lid zonder tijdlijn draagt niets bij | beide vouwgetuigen `case: attribuut som: verwacht 0, werkelijk leeg` (elke volgorde, beide takken), `an_absent_timeline_totals_zero` (NPE) |
| M6 | het vaste startpunt niet in de bijdrage (`fixed = null` in `contributionRules`) | de nieuwe vouwgetuige: `J1.last: de waarde 1469398/66795 past niet in de gedeclareerde 0 decimalen` |
| M7 | het vaste startpunt niet in het eigen totaal (`fixed = null` in `ownTotal`) | vast-startpunt beide takken `ERROR WriteBoundaryFault: … J1.maandtoelagesom: value 8970/899 …` |

### Pins en lijsten, oud → nieuw (voorstel)

- Gate 12: `EXCLUDE_KNOWN` 5 → **0** regels (leeg met zijn bewaking); `EXPECTED_CHANNEL` 51 → **46** (de vijf
  `DIVERGE`-regels weg); `EXPECTED_PLAYED` 323 → **328**, met een regel in het commentaarblok die de vijf goldens en hun
  PLAYED (1, 1, 1, 1, 7) noemt.
- Gate 17: `DEVIATIONS` 24 → **0**; `scripts/check-readings.sh` is weer byte-gelijk aan main ("Empty since the gate
  began" klopt op main: de 24 regels landden nooit).
- Drools-tests: `TOTAL_OVER_PARTIAL_UNIT` (twee getuigen) weg, twee `BothBranches.red` weer groen (hierboven).
- Geen floor veranderd. Geen nieuwe EF-, REF-, CLINGO-, RGS- of DROOLS-code.

### Voorgestelde tekst

- **RS ch. 3.6**, uitvoerdersnoot (in de branch): "They still count whole grid units per fragment and total an absent
  timeline leeg, so the five goldens … (card `20261103-total-over-partial-unit`, step B)." → "On every route, the
  per-element fold included, they measure a segment in units of its grid, a part of a unit by its days on the calendar
  or from a fixed start point, and total an absent timeline 0."
- **RS ch. 3.12**, de rij van het totaal (in de branch): "Both Drools branches count whole units per fragment and total
  an absent timeline leeg (value divergences in gate 12, card `20261103-total-over-partial-unit` step B)." → "Both
  Drools branches on every route, exactly (a decimal or a rational)."
- **CONSTRUCTS rij 11**, in de voorgestelde tekst van stap A: de zin "The Drools backend counts whole units per
  fragment and totals an absent timeline leeg until step B (value divergences in gate 12)." vervalt, en na de zin van
  de ASP-backend komt "Both Drools branches execute it on every route, the per-element fold included.".
- **CLAUDE.md**: geen wijziging; het voorstel van stap A voor gate 17 ("24 entries … until its step B") vervalt, "empty
  since the gate began" klopt weer.
- Buiten mijn may-touch: `frontends/regelspraak/.../resolver/ExpressionBuilder.java` r. 3124 noemt in commentaar
  "rsTijdTotaal at the attribute's granularity" → "the total over time on the attribute's own grid".

### Voorlopige besluiten

- VB-B1 — `rsTijdTotaal` (met `heleMaanden` en `StaticFunction.RS_TIME_TOTAL`) is uit de runtime verwijderd in plaats
  van omgebouwd: geen route van het totaal riep het nog aan, het kende geen raster met een vast startpunt (de runtime
  kent `FixedStartPoint` niet, en een BigDecimal houdt 100/61 niet), en een functie die hele eenheden en een afwezig
  totaal leeg telt, zou ch. 3.6 tegenspreken. Geen code buiten de submodule roept haar aan (gezocht in de toolchain).
- VB-B2 — Het vaste startpunt gaat als derde constante mee naar `total` en `ownTotal` en als component `fixed` van
  `MemberValue`; de periode komt van `FixedStartPoint.period`, zodat de emmerscalair op zo'n raster precies 1 meet.
  Een raster van meer perioden of een variabel startpunt bereikt het totaal niet: `timeMember` weigert het al.
- VB-B3 — Een afwezige invoer zonder peildatum blijft in het eigen totaal de fout "needs the evaluation date" geven,
  zoals ref en clingo (open punt 2 van stap A ongemoeid).

### Open punten

1. De drie open punten van stap A blijven (de precisiegrens, de peildatum vóór de afwezigheid, de tekstvoorstellen).
2. Het commentaar in `ExpressionBuilder` (hierboven).

### Gates

Losse gates op `080eabf30` (na de rebase): gate 12 `check-drools: PASS` (`logs/11-gate12.log`), gate 13
`check-reftype-derivation: PASS` (`logs/14-gate13.log`), gate 17 `check-readings: PASS` met `deviations 0`
(`logs/15-gate17.log`; de eerste run, `logs/13-gate17.log`, stopte op "Another Dune instance is currently running"
omdat ik gate 13 en 17 tegelijk startte op een verouderde dune-stempel).

`make gates` groen op `080eabf30` (`logs/gates-1.log`, `out/gates/20261104-074126/`), 361 s:

```
01-self-contained rc=0 2s     02-ir-schema rc=0 133s   04-case-schema rc=0 8s   03-verifier rc=0 39s
05-oracle rc=0 23s            06-clingo rc=0 291s      07-noop rc=0 13s         08-red rc=0 158s
09-catala rc=0 5s             10-regelspraak rc=0 260s 11-toka-parity rc=0 233s 12-drools rc=0 361s
13-reftype rc=0 322s          14-flogic rc=0 2s        15-graph rc=0 48s        16-scenario rc=0 40s
17-readings rc=0 269s         after-run rc=0 0s        end 2026-11-04 07:47:27 361s red=0
```

Gemeten: gate 12 `played matches the pinned 328`, DRL-tekst 328, value-divergence 0, kanaalvloer 46 met naam, JUnit
1831 tests (nul skips, floor 1573); gate 17 328 goldens en 931 casebestanden op beide takken vergeleken, 7348
lezingen, 0 afwijkingen; gate 5 ref-suite 2317 voorbeelden en 1058 cases, gate 6 clingo-suite 2462 checks en 1058 cases
ref = clingo. De kaart blijft in `doing/`.
