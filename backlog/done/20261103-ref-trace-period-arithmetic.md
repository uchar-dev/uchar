# ref-trace-period-arithmetic: de lezing van ref leest rekenen per periode, en Drools kopieert het gebrek niet meer

*Opgesteld door de hoofdsessie op 2026-11-03 uit de architectuurreview van Fable (`werk/review-fable-2026-11-03/advies.md`, §3 "Drools" en
"Per-dag-lifting", advies 3 en 4) en open punt 1 van stap B van `backlog/done/20261103-timeline-arithmetic-by-rule.md`.*

## Aanleiding

1. De trace van ref (`ref/lib/reading.ml`) leest de knopen van een waarde van rekenen per periode zonder voorwaarde als `unreached`: hij
   rekent via `run_pro_rata` en ziet geen knoop, terwijl RUNTIME-SEMANTICS ch. 5.12 hun waarden vraagt. Drools bootst dat na
   (`TmsTranspiler.unreached`, de boekhouding in `writeRule`) om gate 17 groen te houden. Een backend die een gebrek van de referentie
   nabouwt, is het omgekeerde van wat gate 17 moet bewijzen.
2. `run_rule` in `ref/lib/eval.ml` houdt een eigen pad voor een venster dat de hele voorwaarde is (`clip`, met `window_of` dat `(0, 0)`
   geeft bij een lege grens) naast de `Time_window`-arm van `cond_periods`, die `[]` geeft. Twee implementaties van één regel.

## Goal

Ref leest de knopen van een waarde van rekenen per periode volgens ch. 5.12 (de knopen hun tijdlijnen, de wortel de geschreven tijdlijn,
zoals voor een regel met een voorwaarde per periode al gebeurt); Drools leest ze ook zo, en `TmsTranspiler.unreached` met zijn pins
verdwijnt. Het `clip`-pad van `run_rule` verdwijnt ten gunste van `cond_periods`, zonder dat een uitkomst verandert.

## Acceptance

- (a) Rood eerst: een readings-getuige op een golden van rekenen per periode zonder voorwaarde (`jutterswet-maandloon`) die op main
  `unreached` geeft (letterlijk).
- (b) Gate 17 vergelijkt de nieuwe lezingen op beide branches zonder afwijking; floors die bewust stijgen met oud en nieuw.
- (c) Het `clip`-pad weg; gates 5, 6, 12 en 17 onveranderd op elke golden (geen semantiek beweegt); de ref-tests die het pad pinnen
  blijven groen of worden met reden herschreven.
- (d) `make gates` groen.

May touch: `ref/lib/reading.ml`, `ref/lib/eval.ml`, `ref/test/`, `backends/drools/**`, `scripts/check-readings.sh` (voorstel), kaartverplaatsing.

## Omvang

Klein tot middel.

## Result (bouwer, 2026-11-04)

Gebouwd op main `b55b6b6d7`, tweemaal gerebased op verzoek van de hoofdsessie: op `457debf65` (total-precision-bound) en op
`87a89d44f` (clingo-refusal-channel). Logs onder `werk/ref-trace-period-arithmetic/logs/`.

### Commits

- `cc9c29296` kaart naar doing
- `39c1ae92f` ref: de lezing leest rekenen per periode ook zonder voorwaarde (met de bijdragers van de som per periode)
- `ccb9486c7` ref: een venster als hele voorwaarde gaat door `cond_periods` (het `clip`-pad en `read_clip` weg)
- `6d346165b` drools: geen `unreached` meer voor rekenen per periode zonder voorwaarde
- `40ee4461f` tests: de lezingsgetuigen volgen de afgeronde jaarloon (na de rebase op `457debf65`)
- `18ec967ff` spec: RS ch. 5.12, de lezing van rekenen per periode (voorstel)
- `4ecb72566` drools: een operand van rekenen per periode leest zijn tijdlijn met eenheid (vondst van gate 17)
- `a0a353027` tests: de eenheid van een heel gelezen operand, in ref en Drools
- `d49e919ac` gate 17: de vloer van de tijdlijnregels stijgt naar 850 (voorstel)

### Rood eerst (letterlijk)

- `runner --trace` op `jutterswet-maandloon`, alle vijf cases, op `b55b6b6d7` (`logs/01-red-ref-trace-maandloon.log`): 60 regels
  `unreached`, bijvoorbeeld `reading /rules/rules/0/versions/0 "J1" 0 Binary unreached`,
  `reading /rules/rules/0/versions/0 "J1" 1 AttributeReference unreached`,
  `reading /rules/rules/3/versions/0 "J1" 0 Aggregation unreached`. `--readings` op beide Drools-takken: dezelfde 60 regels
  (`logs/03-red-drools-*-maandloon.log`).
- De nieuwe ref-getuige `WV-5.12-maandloon` op de oude `ref/lib` (`logs/05-red-ref-suite.log`):
  `FAIL WV-5.12-maandloon case1-voorbeeld.json`, `got: /rules/rules/0/versions/0 "J1" 0 Binary unreached`.
- De nieuwe Drools-getuige F (`TmsPeriodArithmeticWitnessTest.the_readings_without_a_condition_are_those_of_the_reference_interpreter`)
  op de oude backend (`logs/14-red-drools-old.log`): `but was: <[case case1-voorbeeld.json, reading /rules/rules/0/versions/0 "J1" 0 Binary unreached`.

### Wat er veranderde in ref

- `reading.ml`: de waarde van een regel op een tijdlijndoel wordt gelezen via `run_period_arithmetic` waar zij rekenen per periode is,
  met of zonder voorwaarde (de voorwaarde `condition <> None` is weg); de pro-rata-arm leest alleen nog pro rata en dagentelling.
  `read_clip` is weg: een venster als hele voorwaarde gaat door `read_periods`, zoals elke voorwaarde per periode.
- `eval.ml`: in `run_rule` zijn `clip` en `window_of` weg; `per_period` omvat het venster als hele voorwaarde (`read_per_period`
  telde het al mee), en de perioden komen uit de `Time_window`-arm van `cond_periods`. De waarnemer kreeg `period_members` en
  `member_found` de variant `Member_timeline`: de som per periode (ch. 3.5) meldt per lid de tijdlijn die het bijdraagt; een lid
  zonder tijdlijn draagt niets bij.

### Wat er veranderde in Drools

- `TmsTranspiler`: `unreached`, `perMember` en de aanroep in `writeRule` zijn weg; de javadoc van `periodArithmetic` en
  `noteUnreached` zegt de huidige stand. Geen test pinde `unreached`; de spiegel hield alleen gate 17 groen.
- `periodOperand`: een eigen attribuut dat heel gelezen wordt, draagt in de lezing de eenheid van zijn declaratie
  (`notedUnits`), zoals ref en ch. 5.12 haar schrijven. Dit was onzichtbaar zolang de knopen `unreached` lazen.
- README van de backend: de alinea over rekenen per periode zegt dat de knopen met en zonder voorwaarde gelezen worden.

### Welke lezingen veranderden

Gemeten met de trace van ref, oud tegen nieuw, over elk golden-casebestand (`logs/26-trace-diff-87a89d44f.log`, op main
`87a89d44f`): 354 regels in 18 cases van 5 goldens, elk `unreached` → een waarde.

| golden | cases | regels |
|---|---|---|
| `jutterswet-maandloon` | 5 | 60 |
| `jutterswet-rekenen-per-dag` | 7 | 266 |
| `anchor/jutterswet-verzuimrente-30-360` | 2 | 16 |
| `anchor/jutterswet-verzuimrente-dagen` | 2 | 6 |
| `jutterswet-verzuimrente-dagen-afgerond` | 2 | 6 |

Voorbeeld, `jutterswet-maandloon` case 1:

```
oud: /rules/rules/0/versions/0 "J1" 0 Binary unreached
nieuw: /rules/rules/0/versions/0 "J1" 0 Binary [[- 2024-01-01 0], [2024-01-01 2024-04-01 100], [2024-04-01 2024-07-01 130], [2024-07-01 2024-10-01 30], [2024-10-01 - 0]]
oud: /rules/rules/3/versions/0 "J1" 0 Aggregation unreached
nieuw: /rules/rules/3/versions/0 "J1" 0 Aggregation [[2024-01-01 2024-02-01 50], [2024-02-01 2024-03-01 70], [2024-03-01 2024-04-01 10]] contributors ["V1" [[2024-01-01 2024-02-01 50], [2024-02-01 2024-03-01 60]] reached; "V2" [[2024-02-01 2024-03-01 10], [2024-03-01 2024-04-01 10]] reached]
```

en `anchor/jutterswet-verzuimrente-dagen` case 1: `0 Binary unreached` → `0 Binary [[- 2026-02-01 400], [2026-02-01 - 500]] "EUR/jr"`.

### Vondsten

1. **De som per periode en haar bijdragers.** Na de eerste stap verschilden ref en Drools alleen op de knoop van de som per
   periode: Drools schreef de bijdragers met hun tijdlijnen, ref geen (`logs/10-*`). Ch. 5.12 zegt dat de knoop van een vouw over
   een verzameling haar bijdragers draagt; ref volgt nu (VB-1). Een lid zonder tijdlijn telt in Drools niet mee (gemeten op een
   kopie van case 1 met V2 zonder opbrengst); ref doet hetzelfde, gepind met de fixture
   `ref/test/fixtures/period-arithmetic/sum-member-without-timeline`.
2. **De eenheid van een heel gelezen operand.** Gate 17 alleen, na de eerste Drools-stap (`logs/16-gate17.log`): 18 case-takken
   rood op `jutterswet-rekenen-per-dag` en `anchor/jutterswet-verzuimrente-30-360`, bijvoorbeeld ref
   `/rules/rules/6/versions/0 "J1" 1 AttributeReference [[2024-02-01 2024-05-01 2]] "1/dg"` tegen backend
   `... 1 AttributeReference [[2024-02-01 2024-05-01 2]]`. Opgelost in Drools (`4ecb72566`, VB-2), daarna gepind in beide suites
   (`WV-5.12-rekenen-per-dag-eenheid`, `an_operand_read_whole_reads_its_timeline_in_the_unit_of_its_declaration`).
3. **Een fout in een venstergrens** (uit de code, niet gemeten; geen golden): `read_clip` liet zo'n fout uit de trace vallen,
   `read_periods` geeft de lezing `FAULTS` met de fout aan haar knopen, zoals voor elke voorwaarde per periode.

### Het `clip`-pad: de meting

Varianten van de fixture `conditions-per-period/venster-rekenen` met als grens een eigen scalair datumattribuut, gevuld of leeg, aan
elke kant (`logs/06-clip-measure.log`; documenten in het kladblok, verifier PASS), met de runner van het oude en van het nieuwe pad:

| variant | oud | nieuw |
|---|---|---|
| van-grens leeg | geschreven `[]`; lezing `0 TimeWindow []`, `1 AttributeReference empty`, `3 Binary []` | gelijk |
| tot-grens leeg | geschreven `[]`; lezing `0 TimeWindow []`, `2 AttributeReference empty`, `3 Binary []` | gelijk |
| van-grens 2024-04-01 | `3 Binary [[2024-04-01 2024-06-01 15]]` | gelijk |
| tot-grens 2024-05-01 | `3 Binary [[2024-03-01 2024-05-01 15]]` | gelijk |

`window_of` gaf `(0, 0)` bij een lege grens en `run_rule` maakte daar `Some []` van; de `Time_window`-arm geeft `[]`: beide schrijven
niets. Geen verschil. De vier nieuwe rijen van `WV-3.10-clip` (`-lege-van-grens`, `-lege-tot-grens`, `-gevulde-van-grens`,
`-gevulde-tot-grens`, via `run_rule`) zijn ook groen op het oude pad (`logs/08-old-ref-suite-with-new-tests.log`: alleen
`WV-5.12-maandloon` rood). Gates 5, 6, 12 en 17 groen op elke golden; de bestaande `WV-3.10-clip`-rijen ongewijzigd groen.

### Mutaties (niet gecommit; `logs/19-*`, `22-*`, `23-*`, `25-*`)

| # | mutatie | uitkomst |
|---|---|---|
| M1 | ref leest rekenen per periode zonder voorwaarde weer via de pro-rata-arm | `WV-5.12-maandloon` rood (3 rijen); door gate 17: 32 case-takken rood en `550 timelines compared, but EXPECTED_MIN_TIMELINE_LINES pins at least 850` |
| M2 | ref meldt ook een lid zonder tijdlijn als bijdrager | `WV-5.12-maandloon case.json` rood |
| M3 | ref meldt geen bijdragers van de som per periode | `WV-5.12-maandloon` rood (2 rijen) |
| M4 | een lege venstergrens geeft heel de tijd in `cond_periods` | `WV-3.10-clip-lege-van-grens`, `-lege-tot-grens` en `WV-3.10-kenmerk-venster` rood |
| M5 | Drools met `unreached` (de backend van `b55b6b6d7`) | getuige F rood (`logs/14-red-drools-old.log`) |
| M6 | Drools zonder de eenheid van een heel gelezen operand | `an_operand_read_whole_reads_its_timeline_in_the_unit_of_its_declaration` rood op de exec-model-tak, die de test als eerste toetst; door gate 17: 18 case-takken rood (`logs/16-gate17.log`) |
| M8 | ref schrijft geen eenheid na een heel gelezen tijdlijn | `WV-5.12-voorwaarde-per-periode` en `WV-5.12-rekenen-per-dag-eenheid` rood |

### Floors en pins, oud en nieuw

- Gate 17 (voorstel, `scripts/check-readings.sh`): `EXPECTED_MIN_TIMELINE_LINES` 76 → 850. Gemeten met gate 17 alleen op main
  `457debf65` (`logs/18-gate17-main.log`) en op de kaart daarop (`logs/21-gate17.log`): tijdlijnregels 598 → 850, `unreached`
  4232 → 3884, regels met bijdragers 935 → 947, lezingen 7348 en knoopregels 23994 gelijk, 0 afwijkingen. In `make gates` op
  `87a89d44f`: 858 tijdlijnregels, 3884 `unreached`, 947 met bijdragers, 933 cases van 329 goldens. De vloeren
  `EXPECTED_MIN_UNREACHED` (3118) en `EXPECTED_MIN_CONTRIBUTOR_LINES` (314) blijven: beide liggen onder de nieuwe tellingen.
- Geen kanaalpin, geen code en geen register veranderde; `TmsTranspiler.unreached` had geen eigen pin.
- Tests die meebewegen met main, niet met deze kaart: in `WV-5.12-maandloon` en getuige F lezen de regels van `jaarloon` sinds
  `457debf65` `0 Rounding 780`, `1 Aggregation 780`, `2 AttributeReference …` (eerder geschreven als `0 Aggregation 780`,
  `1 AttributeReference …`).
- Ref-suite 2327 voorbeelden (floor 489), Drools-suite 1833 tests (floor 1573), nul skips.

### Voorgestelde tekst (huidige stand)

RS ch. 5.12 staat als voorstel in `18ec967ff`:

> The value of a rule on a timeline target is the timeline it writes; under period arithmetic (ch. 3.4, 3.5), with a condition or
> without one, each node below the value's root has the timeline it reads or computes — an operand read whole its timeline, `[]`
> where it is absent — or its value where it is a scalar (`ref/` test `WV-5.12-maandloon`).

> … for the sum per period over a set (ch. 3.5) each member with the timeline it contributes, where a member without a timeline
> contributes nothing; …

en in de artefacten: "`WV-5.12-maandloon` (the golden `jutterswet-maandloon`, period arithmetic without a condition: the timeline
of each node, and the contributors of the sum per period, a member without a timeline among them) pins the reading of period
arithmetic in `ref/`, and the backend's `TmsPeriodArithmeticWitnessTest` the same lines on both its branches."

Voor de hoofdsessie, `CLAUDE.md` gate 17 (beschermd): "`EXPECTED_MIN_TIMELINE_LINES=76` (measured 2026-10-21)" →
"`EXPECTED_MIN_TIMELINE_LINES=850` (measured 2026-11-04, card `ref-trace-period-arithmetic`; first set 76, 2026-10-21)". Geen
CONSTRUCTS-delta: er verandert geen semantiek.

### Voorlopige besluiten

- VB-1 — De knoop van de som per periode draagt haar bijdragers: elk lid met de tijdlijn die het bijdraagt, zonder eenheid zoals
  een waarde van een bijdrager, en een lid zonder tijdlijn niet (ch. 5.12, vouwen over een verzameling). Drools deed dat al; ref
  volgt.
- VB-2 — Een eigen attribuut dat rekenen per periode heel leest, schrijft in de lezing de eenheid van zijn declaratie na zijn
  tijdlijn, ook `[]` (ch. 5.12, de eenheid na een waarde). Ref deed dat al; Drools volgt.
- VB-3 — Alleen de vloer van de tijdlijnregels stijgt; zij bewaakt rechtstreeks dat de knopen niet terugvallen naar `unreached`
  (M1). De vloer van de bijdragers stijgt niet: de som per periode voegt er 12 toe op 947.

### `make gates`

Groen op `d49e919ac` (gerebased op `87a89d44f`): 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 434 s
(`out/gates/20261104-082337/`, log `logs/gates-1.log`):

```
01-self-contained rc=0 2s   02-ir-schema rc=0 146s    03-verifier rc=0 40s      04-case-schema rc=0 7s
05-oracle rc=0 23s          06-clingo rc=0 350s       07-noop rc=0 14s          08-red rc=0 192s
09-catala rc=0 5s           10-regelspraak rc=0 315s  11-toka-parity rc=0 281s  12-drools rc=0 427s
13-reftype rc=0 398s        14-flogic rc=0 2s         15-graph rc=0 50s         16-scenario rc=0 41s
17-readings rc=0 324s       after-run rc=0 0s         end 2026-11-04 08:30:51 434s red=0
```

Gemeten: gate 12 `played matches the pinned 328`, JUnit 1833 tests; gate 5 2327 voorbeelden en 1060 cases; gate 17 329 goldens en
933 cases op beide takken, 0 afwijkingen. De kaart blijft in `doing/`.
