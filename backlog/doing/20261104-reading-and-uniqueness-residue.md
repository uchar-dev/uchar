# reading-and-uniqueness-residue: twee restpunten uit early-faults-and-sum-reading

*Opgesteld door de hoofdsessie op 2026-11-04, uit de vragen van `early-faults-and-sum-reading`. Na `absence-markers` (overlap in
`backends/clingo` en de lezingen).*

## Aanleiding

1. RUNTIME-SEMANTICS ch. 5.12 zegt dat een knoop de fout draagt die hij doorgeeft; ref en beide Drools-branches lezen een voorwaarde per
   periode met een falende operand als `0 Comparison unreached`. De twee uitvoerders zijn het eens (gate 17 groen), de spec zegt iets
   anders.
2. Een uniciteitsunie met een tekstsleutel over een leeg type geeft in clingo een vertaalfout zonder registercode, waar ref speelt.
3. (Uit `decisions-per-period`.) De lezing noteert een `Let` die haar eigen binder leest als `unreached` (in
   `jutterswet-verzuimrente-30-360-afgerond`), waar ch. 5.12 dat alleen zegt van een `Let` waarvan geen lezing de binder dwingt.
4. (Uit `writers-as-one-defaults-stack`.) Bij EF-016 tussen een regel en een verdeling noemt Drools de verdeling "de regel", ref
   `allocation "…"`; de parameters en de zin horen in alle uitvoerders dezelfde schrijver te noemen.

Besloten zonder kaart (hoofdsessie, technisch): ref geeft EF-018 (een dubbelzinnige terugverwijzing van een gemaakt object, ch. 5.5) ook
achter een voorwaarde die niet geldt en zonder subject. Dat is een fout van de vorm van de creatie, die uit de typen volgt en niet uit een
waarde; zij staat waar de vorm staat. Geen wijziging.

## Goal

- (1) Eén lezing: ch. 5.12 en de uitvoerders zeggen hetzelfde over een voorwaarde per periode met een falende operand. Meet wat ch. 5.12
  voor de scalaire voorwaarde zegt en wat ref daar doet; kies de lezing die het scalaire geval per periode volgt, en pas de kant aan die
  afwijkt (spec als voorstel, of ref en Drools met een golden die gate 17 vergelijkt).
- (3) Eén lezing voor een `Let` die haar binder leest, in ch. 5.12, ref en Drools.
- (4) Eén naam voor een verdeling in EF-016, in ref, clingo en Drools.
- (2) clingo speelt de uniciteitsunie met een tekstsleutel over een leeg type, of weigert met een code van het soort `refusal`; een golden
  of suitecheck houdt het vast.

## Acceptance

- (a) Rood eerst per punt; per nieuwe bewaking een mutatie die een test rood maakt.
- (b) `make gates` groen.

May touch: `spec/RUNTIME-SEMANTICS.md` (voorstel), `ref/`, `backends/drools/**`, `backends/clingo/`, `corpus/`, kaartverplaatsing.

## Omvang

Klein.

## Result

*Bouwer, 2026-11-05. Worktree `wt/reading-and-uniqueness-residue`, branch `card/reading-and-uniqueness-residue`, vanaf main
`36f661756`. Logs onder `werk/reading-and-uniqueness-residue/logs/`.*

### Commits

| commit | wat |
|---|---|
| `91235402b` | kaart naar `doing/` |
| `179a5fb85` | punt 1: ref (`Eval.cond_periods`) en Drools (de vergelijking per periode) dragen de fout aan elke knoop die haar opwerpt of doorgeeft; golden `jutterswet-voorwaarde-buiten-de-beurt`; ref-test `WV-5.12-default-per-dag-voorwaarde-faalt`; Drools-test omgedraaid |
| `ecfc750b9` | punt 3: de `Let` en de lezing van haar binder hebben de lezing van de producent (ref `Reading.through_lets`, Drools `note` in plaats van `noteUnreached`); ref-test `WV-5.12-let-op-tijdlijndoel` |
| `47fce0b50` | punt 4: EF-016 noemt een verdeling bij haar naam in `rules` (ref) en als verdeling in de zin (clingo, Drools); rode case `jutterswet-verdeling-botsing` |
| `9d0a68581` | punt 2: clingo speelt een uniciteitsunie met een tekstsleutel; golden `jutterswet-uniciteit-reeks`, twee frames in de clingo-suite |
| `453889657` | `scripts/check-drools.sh` (beschermd, voorstel): `EXPECTED_PLAYED` 339 → 341 |
| `d679e75cf` | `spec/RUNTIME-SEMANTICS.md` (voorstel): ch. 5.12 en ch. 5.7b |
| `c4c8272de` | Drools: de afgeleide lezing van de vergelijking per periode neemt haar periodenterm als eerste invoer (geen zoekslag over de invoer) |

### Punt 1 — een voorwaarde per periode met een falende operand

Gemeten vóór de bouw (`logs/05`, `logs/06`, en de golden door de oude uitvoerders): ch. 5.12 zegt van elke knoop "the
evaluation fault the node raised or passed on", en ref leest een scalaire voorwaarde zo (`WV-5.12-fout-in-een-lezing`:
`0 Compound fault EF-019`, `4 Unresolved fault EF-019`). Per periode las ref `0 Compound unreached`, `1 Comparison
unreached`, `2 AttributeReference unreached` (de knopen van `cond_periods` liepen niet door de waarnemer), en Drools
`0 Compound fault EF-037`, `1 Comparison unreached`, `2 AttributeReference fault EF-037` (de vergelijking was een afgeleide
knoop zonder `raises`). Gekozen: de lezing van het scalaire geval, `0 Compound fault EF-037`, `1 Comparison fault EF-037`,
`2 AttributeReference fault EF-037`, de rest `unreached`. De spec zei dat al; ref en Drools zijn aangepast en ch. 5.12 zegt
het nu uitdrukkelijk, met het voorbeeld.

- **ref** (`eval.ml`): `cond_periods` registreert een fout van de knoop of eronder aan de knoop zelf (via `o.cond`), en
  de lezing van de vergeleken tijdlijn die op de emmerscalair faalt, aan die `AttributeReference` (via `o.expr`).
- **Drools** (`TmsTranspiler.periodComparison`): de afgeleide lezing van de vergelijking draagt de fout van haar invoer
  (`raises`), met de periodenterm, die tijdlijn en grens leest, als eerste invoer.
- **Golden** `jutterswet-voorwaarde-buiten-de-beurt`: een default "indien hij vrijgesteld is" beslist elke dag van de
  vrijgestelde J1; de default eronder, "zijn kustloon is groter dan 30 en kleiner dan 100", is op geen dag aan de beurt,
  en J1's kustloon is de emmerscalair. De sessie faalt niet; de lezing faalt aan de knopen, status `DISPLACED`. Gate 17
  vergelijkt het op beide branches; J2 (kustloon als tijdlijn) leest `HOLDS` met perioden.

### Punt 3 — een `Let` die haar binder leest

Gemeten (`jutterswet-verzuimrente-30-360-afgerond`, regel 2, case 1): ref en Drools lazen `1 Let unreached`, `7 BinderRef
unreached`, `2 BuiltinCall [[- 2025-11-01 0], …]`. Ch. 5.12 noemt alleen een `Let` "whose binder no read forces" niet
bereikt; hier leest het lichaam de binder, en in het scalaire geval dragen `Let` en `BinderRef` een waarde. Nu: `1 Let`,
`2 BuiltinCall` en `7 BinderRef` elk de tijdlijn die de regel schrijft. ref: `Reading.through_lets` geeft de `Let`-keten
de uitkomst van de producent (ook een fout of niet bereikt); Drools noteert de `Let` en haar lichaam met de term van de
producent. Ch. 5.12 zegt het nu, met het gemeten geval als voorbeeld.

### Punt 4 — de naam van een verdeling in EF-016

Gemeten (`ref/test/fixtures/writers-stack/allocation-conflict`): ref `rules` = `vast bergingsaandeel, allocation
"verdeel de opbrengst van een berging"`, clingo en Drools `vast bergingsaandeel, verdeel de opbrengst van een berging`;
de zin van clingo `rules "vast bergingsaandeel" and "verdeel …"`, van Drools `de regel "verdeel …" schrijft 50`. Nu:
`rules` overal de gedeclareerde namen (ch. 5.7b: "declared names … untranslated"); de zin van ref `allocation "…" writes
50` (ongewijzigd), van clingo `rule "vast bergingsaandeel" and allocation "verdeel …" write different values`, van
Drools `de verdeling "verdeel …" schrijft 50` / `the allocation "…" writes 50`. ref: `rule_name_of` neemt de naam tussen
de aanhalingstekens bij elke soort; clingo: de zin en de lezer van de parameter, die nu de geciteerde namen tussen
"disagree: " en " write different values" neemt; Drools: `WritersConflict` krijgt de namen van de verdelingen van het
model en de catalogusdelen `EF-016.write-allocation` (nl, en). Rode case `jutterswet-verdeling-botsing` pint `rules`,
`attribute`, `label`, `objectType` (clingo noemt `values` niet).

### Punt 2 — de uniciteitsunie met een tekstsleutel in clingo

Gemeten: niet alleen over een leeg type; elke uniciteitsunie met een tekstsleutel viel in clingo met `TRANSLATION FAULT:
unscalable datatype in the ASP translation` (ook met instanties van beide typen), waar ref en Drools spelen. Oorzaak:
`translate_uniqueness_union` vroeg voor elke sleutelpositie de decimalen van elk type. Nu krijgt alleen een numerieke
sleutel (getal, percentage, datum in dagen) een schaal (`union_key_scale`); een tekst, enumeratie of datum-tijd vergelijkt
als de term die hij is, zoals in de enkelvoudige uniciteit. clingo speelt; geen code. Golden `jutterswet-uniciteit-reeks`
(botsing over twee typen, botsing binnen één type naast een leeg type) en twee frames in de clingo-suite op het document
van `corpus/red/ever-unique-temporal-key-bucket-scalar` (tekstsleutel naast een tijdlijnnummer: over een type zonder
instanties, en over twee typen).

### Rood eerst (letterlijk)

Op `91235402b` met de nieuwe tests en het nieuwe corpus (worktree `red-1`, opgeruimd):
- ref-suite (`logs/26-red1-ref-suite.log`): `FAIL WV-7.8-stapel-verdeling-botsing`, `FAIL
  WV-5.12-default-per-dag-voorwaarde-faalt` (got `… "J1" 0 Compound unreached`, `1 Comparison unreached`, `2
  AttributeReference unreached`), `FAIL WV-5.12-let-op-tijdlijndoel` (got `1 Let unreached`, `7 BinderRef unreached`),
  `3 failure(s)`.
- clingo-suite (`logs/27`): `FAIL the stack of a target: a computing rule and an allocation with different values faults
  at solving (viol("two writers of one target disagree: rules "vast bergingsaandeel" and "verdeel de opbrengst van een
  berging" …`, `FAIL a uniqueness union with a text key over a type without instances: clingo pins the values (fault:
  unscalable datatype in the ASP translation)`, idem `… across two types`, `test_clingo: 2637 checks, 3 FAILURES`.
- gate 17 (`logs/28`): `check-readings: FAIL — jutterswet-voorwaarde-buiten-de-beurt/case1-buiten-de-beurt.json (exec):
  the trace and the readings differ: ref … 0 Compound unreached / ref … 2 AttributeReference unreached / backend … 0
  Compound fault EF-037 / backend … 2 AttributeReference fault EF-037`, idem `(drl)`.
- gate 8 (`logs/29`): `FAIL: jutterswet-verdeling-botsing/case1: ref emits {"rules": "vast bergingsaandeel, allocation
  \"verdeel de opbrengst van een berging\"", …} for EF-016 with another value, but the case pins rules "vast
  bergingsaandeel, verdeel de opbrengst van een berging"`.
- Drools (`logs/06`, `logs/17`, oude code): `TmsConditionPerPeriodWitnessTest.a_condition_per_period_over_the_bucket_scalar_faults
  … expected: <[… "J1" 0 Comparison fault EF-037 …]> but was: <[… "J1" 0 Comparison unreached …`;
  `TmsAllocationWitnessTest.h_a_second_writer_of_a_target_stands_in_its_stack … expected: <{regel=… en de verdeling
  "verdeel de opbrengst naar rato" schrijft 100 …}> but was: <{regel=… en de regel "verdeel de opbrengst naar rato"
  schrijft 100 …`.
- Punt 2 door de oude runners (`logs/22`): ref `oracle: PASS (2 expectations)`, Drools `PLAYED 1` op beide branches,
  clingo `TRANSLATION FAULT: unscalable datatype in the ASP translation` op beide cases.

### Mutaties (niet gecommit; worktree `mut`, opgeruimd; `logs/33-mutations/summary.txt`)

| mutatie | rood |
|---|---|
| M1 ref: `cond_periods` registreert geen fout aan de knoop | `FAIL WV-5.12-default-per-dag-voorwaarde-faalt` |
| M2 ref: de lezing van de vergeleken tijdlijn registreert haar fout niet | idem |
| M3 Drools: de vergelijking per periode zonder `raises` | `TmsConditionPerPeriodWitnessTest`, 1 failure |
| M5 ref: `through_lets` doet niets | `FAIL WV-5.12-let-op-tijdlijndoel` |
| M6 Drools: de `Let`-keten weer `noteUnreached` | gate 17: `FAIL — jutterswet-verzuimrente-30-360-afgerond/case1-voorbeeld.json (exec)`, idem case 2 |
| M7 ref: `rule_name_of` stript alleen `rule "…"` | `FAIL WV-7.8-stapel-verdeling-botsing` |
| M8 clingo: de zin van regel tegen verdeling weer `rules "…" and "…"` | `the stack of a target: a computing rule and an allocation …`, 1 failure |
| M9 clingo: de lezer van `rules` weer `between "rules "` | idem (de code-eis van `faults_at_solving`), 1 failure |
| M10 Drools: elke schrijver `EF-016.write` | `TmsAllocationWitnessTest`, 1 failure |
| M11 clingo: elke sleutel van een unie een schaal | de twee nieuwe frames, 2 failures |

### Groen

ref-suite `test_ref: all 2446 worked examples pass` (was 2444: +2); clingo-suite `test_clingo: 2637 checks, 0 failures`
(was 2633: +4); Drools-suite `Tests run: 1857, Failures: 0, Errors: 0, Skipped: 0` (`logs/34`); frontend-suite in gate 10;
`make gates` hieronder.

### Codes en pins (oud → nieuw)

- Geen registercode nieuw of weg (REF, CLINGO, DROOLS, RGS). Nieuw catalogusdeel `EF-016.write-allocation` in beide
  Drools-catalogi (een deel van de EF-016-zin, geen code; `check-drools-codes.sh` PASS).
- `scripts/check-drools.sh` (beschermd, voorstel, buiten de may-touch van de kaart): `EXPECTED_PLAYED` 339 → 341, de twee
  nieuwe goldens; de gelijkheidspin dwingt het.
- Geen lijstregel erbij of eraf: gate 6 `EXPECTED_CHANNEL`, gate 7 survivors, gate 8 divergentie- en uitsluitingslijst,
  gate 12, gate 17 `DEVIATIONS` ongewijzigd. Geen vloer verschoven.

### Tests die de kaart omdraait (oud → nieuw)

- `TmsConditionPerPeriodWitnessTest.a_condition_per_period_over_the_bucket_scalar_faults`: `0 Comparison unreached` →
  `0 Comparison fault EF-037`.
- `TmsAllocationWitnessTest.h_a_second_writer_of_a_target_stands_in_its_stack`: `… en de regel "verdeel de opbrengst naar
  rato" schrijft 100 …` → `… en de verdeling "verdeel de opbrengst naar rato" schrijft 100 …`.
- Strenger, niet omgedraaid: ref `WV-7.8-stapel-verdeling-botsing` eist ook `rules` en de zin; de clingo-frame van
  regel tegen verdeling eist de zin `rule "…" and allocation "…" write different values`.

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS**: in `d679e75cf` (ch. 5.12: de fout aan een knoop per periode, de `Let` op een tijdlijndoel, het
voorbeeld van de nieuwe golden, de grens over fouten aan de knopen van een default; ch. 5.7b: de schrijver bij zijn naam).

**CONSTRUCTS**, rij 20 (uniciteit), voeg toe: "The ASP backend harmonises the scale of a numeric key position of a union
across its sources and compares a text, an enumeration or a date-time key as its term, as the single uniqueness does
(golden `jutterswet-uniciteit-reeks`; card `reading-and-uniqueness-residue`, 2026-11-05)."

**CONSTRUCTS**, rij 17 (verdeling), voeg toe: "EF-016 between a writer and an allocation names the allocation by its
declared name in `rules` and as an allocation in its sentence, in every executor (red `jutterswet-verdeling-botsing`;
card `reading-and-uniqueness-residue`, 2026-11-05)."

**CONSTRUCTS**, rij 24 (lokale variabelen), voeg toe: "On a timeline target a `Let` whose body reads its binder and
nothing else has, with that read, the reading of the producer it binds, the timeline the rule writes (RUNTIME-SEMANTICS
ch. 5.12; card `reading-and-uniqueness-residue`, 2026-11-05)."

**CLAUDE.md**, gate 17, na de alinea van `defaults-per-period-on-timeline`: "Since the card
`reading-and-uniqueness-residue` (2026-11-05) a node of a condition read per period carries the fault it raises or
passes on, as in a scalar condition, and a `Let` through which a rule on a timeline target reads its producer has, with
the read of its binder, the producer's timeline (ch. 5.12); the golden `jutterswet-voorwaarde-buiten-de-beurt` reads such
a fault in a default at its turn on no day, compared verbatim, and no floor moved." Gate 12 noemt het getal van
`EXPECTED_PLAYED` niet; gate 8 en 6 geen wijziging.

### Voorlopige besluiten

- VB-1 (punt 1): de knoop die opwerpt of doorgeeft, draagt de fout, als in het scalaire geval; de status volgt de
  bestaande ch. 5.12 (`FAULTS`, of `DISPLACED` voor een default die op geen dag aan de beurt is).
- VB-2 (punt 1): de golden loopt via een default die op geen dag aan de beurt is: de voorwaarde van een regel evalueert de
  sessie altijd, dus een fout daar is een rode case en geen golden.
- VB-3 (punt 3): `Let`, `BinderRef` en producent hebben één lezing, de tijdlijn die de regel schrijft (binnen de perioden
  van de voorwaarde), niet de ongeknipte dagtelling: de producent is aan de wortel de waarde van de regel
  (`let_bound_value`), en de binding evalueert haar waarde eenmaal (ch. 10.6).
- VB-4 (punt 4): de parameter de naam alleen, de zin met de soort; de volgorde blijft die van de IR (regels vóór
  verdelingen), waarin de drie uitvoerders al gelijk waren.
- VB-5 (punt 2): spelen, geen weigering; alleen een numerieke sleutel krijgt een schaal.
- VB-6 (punt 1, Drools): de periodenterm als eerste invoer van de afgeleide lezing in plaats van een zoekslag over de
  invoer: die term leest tijdlijn en grens, dus elke fout van de vergelijking staat daar, en geen tak zonder getuige.
- VB-7: namen `jutterswet-voorwaarde-buiten-de-beurt`, `jutterswet-uniciteit-reeks`, `jutterswet-verdeling-botsing`;
  ref-tests `WV-5.12-default-per-dag-voorwaarde-faalt`, `WV-5.12-let-op-tijdlijndoel`.

### Vragen aan de hoofdsessie

1. EF-016 noemt de schrijvers "in the order of the document" (ch. 5.7); alle drie de uitvoerders nemen de volgorde van de
   IR, regels vóór verdelingen, ook waar de bron de verdeling eerst schrijft (zo in `jutterswet-verdeling-botsing`).
   Bedoelt ch. 5.7 die volgorde, of die van de bron? Het raakt alleen het verslag.
2. Geen andere: geen keuze van deze kaart raakt wat een regel betekent.

### make gates

Eén run aan het eind in de worktree op `c4c8272de` (`out/gates/20261105-093247/`, `logs/gates-1.log`); de commits daarna
raken alleen deze kaart.

```
start 2026-11-05 09:32:47 c4c8272de card/reading-and-uniqueness-residue
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 403 source files)
frontend rc=0 0s frontend: rebuilt (0s)
jar rc=0 0s drools-jar: fresh (jar newer than 565 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 153s
04-case-schema rc=0 12s
03-verifier rc=0 60s
05-oracle rc=0 38s
06-clingo rc=0 311s
07-noop rc=0 23s
08-red rc=0 200s
09-catala rc=0 6s
10-regelspraak rc=0 291s
11-toka-parity rc=0 261s
12-drools rc=0 381s
13-reftype rc=0 352s
14-flogic rc=0 2s
15-graph rc=0 67s
16-scenario rc=0 51s
17-readings rc=0 296s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-05 09:39:08 381s red=0
```

Uit de logs: gate 5 `ref suite 2446 worked examples`; gate 6 `backend suite 2637 checks`, `1087 cases replay exactly`;
gate 8 `74 red cases`; gate 10 `JUnit suite green (1505 tests)`; gate 12 `played 341`, `DRL-text branch played 341`,
`JUnit 1857 tests`; gate 17 `342 goldens and 962 case files compared`.
