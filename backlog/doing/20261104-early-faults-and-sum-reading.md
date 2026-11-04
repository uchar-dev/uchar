# early-faults-and-sum-reading: de restpunten van clingo's vroege fouten, en de lezing van een falende som-operand in Drools

*Opgesteld door de hoofdsessie op 2026-11-04 uit het Result van `clingo-lazy-bucket-scalar-fault` (vragen 1–4); de eigenaar besloot ze
alle vier te repareren.*

## Aanleiding

`clingo-lazy-bucket-scalar-fault` liet clingo EF-037 en EF-014 van de periode-aritmetiek geven waar de vorm geëvalueerd wordt, zoals ref en
Drools. Bij het meten bleven vier punten over:

1. Twee frames van `backends/clingo/README.md` [L1] geven nog EF-014 tijdens het vertalen, waar ref ze uitrekent: een fout waar de norm er
   geen kent.
2. EF-010, EF-011, EF-018, EF-019, EF-020, EF-021, EF-022 en EF-024 vallen in clingo nog tijdens het vertalen. Op het corpus en de suites
   geven ze hetzelfde als ref, maar geen golden of fixture zet zo'n vorm achter een voorwaarde die niet geldt.
3. Waar het bereik van een lezing afhangt van de volgorde waarin een creatie leden toevoegt, geeft clingo de fout tijdens het vertalen, over
   elke instantie die de emmerscalair geeft.
4. Drools leest een operand die in een som faalt als `unreached`, waar RUNTIME-SEMANTICS ch. 5.12 `fault` zegt.

## Goal

clingo geeft elke EF-code alleen waar ref haar geeft; kan de codering een vorm niet dragen, dan weigert clingo met een code van het soort
`refusal` of `limit`, nooit met een EF-code. Drools leest een falende som-operand als ch. 5.12 zegt. Elk punt met een getuige in het corpus
of de suites, zodat gate 5, 6, 12 of 17 het vasthoudt.

## Acceptance

- (a) Punt 1: de twee [L1]-frames spelen in clingo of weigeren met een code; README [L1] als huidige stand.
- (b) Punt 2: per EF-code een document met de vorm achter een voorwaarde die niet geldt (en een die wel geldt), in ref, clingo en beide
  Drools-branches; elk verschil opgelost zoals in de Goal.
- (c) Punt 3: gemeten met een document waar het verschil zichtbaar zou zijn; opgelost of, als de codering het niet draagt, een weigering
  met code en een zin in ASP-TRANSLATION.
- (d) Punt 4: rood eerst in de suite (of een golden waar gate 17 het ziet), daarna de lezing van ch. 5.12 op beide branches.
- (e) Rood eerst per punt; per nieuwe bewaking een mutatie die een test rood maakt. `make gates` groen.

May touch: `backends/clingo/`, `backends/drools/**`, `ref/` (alleen waar een meting een verschil met ch. 5.12 toont), `corpus/`,
`spec/RUNTIME-SEMANTICS.md` en `spec/ASP-TRANSLATION.md` (voorstel), `scripts/` (alleen lijsten en pins die schuiven; voorstel),
kaartverplaatsing.

## Omvang

Klein tot middel.

## Result

*Bouwer, 2026-11-04. Niet gemerged, niet gepusht. Logs onder `werk/early-faults-and-sum-reading/logs/`, de scripts van
probe, corpus, meting en mutaties onder `werk/early-faults-and-sum-reading/scripts/` (`probe.sh`, `gated.py`,
`corpus_point2.py`, `mutations.py`, `mutations17.py`); de meting gebruikt `instrument.py` en `measure.py` van de vorige
kaart (`werk/clingo-lazy-bucket-scalar-fault/scripts/`).*

### Commits

| hash | inhoud |
|---|---|
| `bec963eab` | kaart naar `doing/` |
| `3319fd48e` | Drools (punt 4): een afgeleide knoop die de tijdlijn zelf geheel leest, draagt de fout van die lezing (`Derived.raises`); test `TmsDefaultsPerDayWitnessTest.a_fault_in_a_consequence_stands_at_the_node_that_raised_it`; golden `jutterswet-gevolg-buiten-de-beurt-som`; `scripts/check-drools.sh` (beschermd, voorstel) `EXPECTED_PLAYED` 330 → 331 |
| `2cb8ab34a` | clingo (punten 1–3): `reach_guard` (de parameterbewaking met haar tweelingen, nu gedeeld), `fault_where_reached`, de dagsoorttabel met foutdagen, het horizonhek, EF-024 per instantie, CLINGO-035; suite; README |
| `a6841d16a` | corpus (punt 2): drie goldens en drie rode casussen; de NOTE's van negen bestaande rode casussen noemen de fase; `EXPECTED_PLAYED` 331 → 334 |
| `76a4851c5` | RUNTIME-SEMANTICS (beschermd, voorstel): ch. 5.7b en 5.12 |
| `78dfd3979` | ASP-TRANSLATION (voorstel): S4, S6a, de periode-aritmetiek, S12 |
| `ee5e87dc9` | suite: de controle op de bijdragers van het horizonhek breekt bij een vertaalfout niet af |
| `a357be8e1` | clingo: een stap die een foutdag bereikt, passeert geen bereikhek (`dkx/1`); README |
| `7f3ab8a7e` | suite: dezelfde controle op het frame waarin elke dag faalt; Result |
| `e8a1b6a42` | clingo: de foutdagen van de tabel in een `Hashtbl` (de ontdubbeling met `List.mem` was kwadratisch in de ~80 000 dagen van een tabel waarin elke dag faalt: gate 6 liep 632 s, gate 8 304 s) |
| `a8f2b44dd` | clingo: de wandeling naar een foutdag als rekensom met de rang van de start (`fp(D,M)`, M het aantal treffers vóór D) in plaats van een `#count` per foutdag; de suite 106 s → 46 s |
| (deze) | Result: de samenvatting van `make gates` |

### Punt 1 — de twee frames van README [L1]

Meting (op `bec963eab`): `hf-cond` en `dk-cond` vielen bij het vertalen met EF-014 waar ref speelt (`pinned_loud`).

Wat er veranderde:
- **Horizonhek**: de vroege EF-014 is weg. Het filter wordt in het per-element-totaal al vertaald met de bewaking van de
  ongebonden parameter (onder het bereik van de vouw en het lid); een verdacht lid (emmerscalair) waarvan het filter bij
  het vertalen met EF-014 faalt, telt niet als bijdrager, zodat naast EF-014 geen EF-037 vuurt (ref faalt in het filter
  voordat het integreert). Een filter dat om een andere reden niet te evalueren is, houdt het lid (zoals voorheen).
- **Dagsoorttabel**: `dk_table` noteert per dag van het venster of de voorwaarde faalt (EF-014 van een ongebonden
  parameter, EF-022 van een geneste dagsoorttoets op een andere dag), als feit `dke<i>_<k>(D)` per zin; zo'n dag is geen
  treffer. `dk_cond` leest een samengestelde voorwaarde nu in de volgorde van ch. 5.10 (de `settled`-regel van ref, eigen
  code), zodat een lid na de stop op die dag niet faalt. Een stap faalt (`viol/2` in ref's zin, onder `rule_reach` van de
  knoop) waar hij zo'n dag bereikt voordat hij zijn aantal treffers heeft; een telling waar zo'n dag in haar interval ligt.
  Een wandeling die een foutdag bereikt, passeert daarna geen bereikhek of venstergrens (`dkx/1`; zonder die voorwaarde gaf
  `day-kind-parameter-unbound` `CODE EF-014` én `CODE EF-009`). Bij elke foutdag staat het aantal treffers ervoor
  (`dke<i>_<k>(D,M)`), zodat "de wandeling bereikt D" een rekensom met de rang van de start is en geen aggregaat per dag.
- README [L1] is weg; de twee frames heten nu `the horizon fence behind a condition that fails (hf-cond)` en `the day-kind
  table behind a condition that fails (dk-cond)` en zijn `both_answer`.

### Punt 2 — EF-010, EF-011, EF-018, EF-019, EF-020, EF-021, EF-022, EF-024

Meting per code, elk document met de vorm achter een voorwaarde die niet geldt (op `bec963eab`;
`probes/01-measure.txt`, `logs/08-red-corpus-point2.log`; "TF" = vertaalfout):

| code | document | ref | clingo vóór | Drools exec / DRL | clingo na |
|---|---|---|---|---|---|
| EF-010 | `bound-day-behind-a-failing-condition` | PASS | TF EF-010 | PLAYED 1 / PLAYED 1 | PASS |
| EF-011 | `context-value-behind-a-failing-condition`; `duration-reads-van-before-tot`, `clamp-reads-bounds-in-order` en het defaults-document met een voorwaarde | PASS | TF EF-011 | PLAYED 1 / PLAYED 1 | PASS |
| EF-019 | `unresolved-behind-a-failing-condition` (expressie, voorwaarde, operand) | PASS | TF EF-019 | PLAYED 1 / PLAYED 1 | PASS |
| EF-021 | gevuld-telling achter 1 > 2 (suite) | PASS | TF EF-021 | EXCLUDE DROOLS-210 / idem | PASS |
| EF-022 | geneste dagsoorttoets achter 1 > 2 (suite) | PASS | TF EF-022 | EXCLUDE DROOLS-180 / idem | PASS |
| EF-024 | twee tijdsafhankelijke sleutels, type zonder instanties (suite) | PASS | TF EF-024 | EXCLUDE DROOLS-127 / idem | PASS |
| EF-018 | `ambiguous-backlink` met voorwaarde 1 > 2, en zonder enige Jutter | **EVALUATION FAULT EF-018** | TF EF-018 | EXCLUDE DROOLS-080 / idem | ongewijzigd, gelijk aan ref |
| EF-020 | sleutel als scalair: ref en clingo vragen het per case-instantie; leeg type: alle drie spelen | gelijk | gelijk | PLAYED | ongewijzigd |

Waar de voorwaarde wel geldt, faalt elke uitvoerder met de code (de rode tweelingen; Drools in de rode run `FAULT EF-010 {}`,
`FAULT EF-011 {}`, `FAULT EF-019 {}`).

Wat er veranderde: `fault_where_reached` maakt van EF-010 (`BoundDay`), EF-011 (`evaluationDate`, `evaluationYear`),
EF-019 (expressie en voorwaarde) en EF-021 een `viol/2`-getuige in ref's zin onder `rule_reach` van de knoop, met de
tweede bewaking naast een gehesen tak (`beside_hoisted_arms`, nu gedeeld met de parameter) en de tweelingen van
`reach_guard`; de term leidt niets af (`#false`). Een `Unresolved`-voorwaarde neemt haar bereik bij een verse
expressie. EF-024 is `viol(…,S) :- isa(S,T).` per instantie van het type (ref vraagt het per instantie, ook een gemaakte),
en dan wordt EF-020 niet meer gevraagd (ref geeft EF-024 eerst). Een clamp waarvan een grens zo'n contextwaarde is, stopt
na die grens (zonder dat gaf `clamp-reads-bounds-in-order` CLINGO-005 op het maximum, waar ref EF-011 geeft; dat kwam
aan het licht toen alle rode casussen door de nieuwe runner gingen, vóór de commit).

Na (`logs/16-measure`, op `ee5e87dc9`): **13 vroege EF-fouten** (was 34): EF-004 ×6, EF-020 ×3, EF-018 ×2, EF-013,
EF-012; op elk faalt ref met dezelfde code (EF-018 in de suite: het frame achter 1 > 2, waar ref ook faalt).

### Punt 3 — een bereik dat aan de volgorde van gemaakte leden hangt

Meting met de documenten van de suiteframes `EF-037, some order` / `every order` / `no order` (`probes/p3`,
`logs/15-red-point3-old.log`): een puntlezing van de peil van een schuit in de matrix "TEN MINSTE 1 [lengte > 5, de peil
op 1 januari 2025 > 0]" van "TEN MINSTE 1 schuiten" van een Werf, case-schuit S1 met de peil als emmerscalair, één
gemaakte schuit per Werf. Vóór: in alle drie `TRANSLATION FAULT: EF-037: the temporal read of "peil" needs the full
timeline; the case supplies none for "S1" …`, ook bij S1 van 8, waar geen volgorde de peil leest en ref speelt. Na: S1 van
3 met een gemaakte 8: `CLINGO-035: clingo: UNSATISFIABLE — … whether the quantifier fold "de schuiten" reaches it depends
on the order of the members a creation adds, which is not normed (ch. 3.9a/5.7a)` (ref faalt in zijn eigen volgorde met
EF-037); met een gemaakte 3: EF-037 in ref's woorden, als ref; S1 van 8: beide spelen.

Wat er veranderde: `bucket_scalar_fault` en de ongebonden tijdsafhankelijke parameter van de periode-aritmetiek gaan door
`reach_guard`; `order_dependent` en de vroege fout zijn weg. De genoemde tweeling van elke bewaking (ook die van de
parameter) is een weigering: `viol_code` leest de staart "depends on the order of the members a creation adds, which is
not normed (ch. 3.9a/5.7a)" als **CLINGO-035** vóór elke EF-arm. Geen zin in ASP-TRANSLATION nodig voor een weigering bij
het vertalen; de periode-aritmetiek noemt de twee vormen.

### Punt 4 — de lezing van een falende som-operand in Drools

De operand van de periode-aritmetiek die een tijdlijn geheel leest (`TmsTranspiler`, de `AttributeReference`) kreeg zijn
lezing afgeleid van de term `complete(…)`; faalde die op de emmerscalair, dan las de knoop `unreached`. `Derived` kreeg
`raises`: is de invoer de eigen lezing van de knoop, dan draagt de knoop haar fout. Beide branches lezen nu
`1 Binary fault EF-037`, `2 AttributeReference fault EF-037`, `3 Literal unreached`, als ref en ch. 5.12. De andere
afgeleide knopen (voorwaarde per periode) blijven zoals ref ze leest; zie vraag 1.

### Rood eerst (letterlijk)

- Punt 4, gate 17 op `bec963eab` met de nieuwe golden (`logs/04-red-gate17.log`): `check-readings: FAIL —
  jutterswet-gevolg-buiten-de-beurt-som/case1-buiten-de-beurt.json (exec): the trace and the readings differ: ref
  /rules/prioritizedDefaults/0/defaults/1 "J2" 2 AttributeReference fault EF-037 / backend … 2 AttributeReference
  unreached`, idem `(drl)`.
- Punt 4, de Drools-test op de oude code (`logs/05-red-drools-test.log`): `Tests run: 5, Failures: 1`,
  `a_fault_in_a_consequence_stands_at_the_node_that_raised_it … EXEC_MODEL: de canonieke tekst ==> expected: <[… "J2" 2
  AttributeReference fault EF-037 …]> but was: <[… "J2" 2 AttributeReference unreached …]>`.
- Punt 2, het nieuwe corpus door de oude runner (`logs/08-red-corpus-point2.log`): ref `oracle: PASS`, Drools `PLAYED 1`
  op beide branches, clingo `TRANSLATION FAULT: EF-010: a BoundDay is the bound day …`, `TRANSLATION FAULT: EF-011: the
  evaluationDate context value …`, `TRANSLATION FAULT: EF-019: Unresolved reached in translation …`.
- Punten 1–3, de nieuwe suite tegen de bibliotheek van `bec963eab` (`logs/17-red-suite-old-lib.log`): `test_clingo: 2576
  checks, 25 FAILURES`, o.a. `the horizon fence behind a condition that fails (hf-cond): clingo pins the values (fault:
  parameter "minimumgewicht" is unbound …)`, `the day-kind table behind a condition that fails (dk-cond): clingo pins the
  values (fault: parameter "grensdatum" …)`, `day-kind step: one day on, found before a day that faults (dk-walk-1): …
  (fault: …)`, `EF-024: a key with two temporal components over a type without instances: clingo pins the values (fault:
  …)`, `EF-011: a default below the one that decides reads no evaluation date: … (fault: …)`, `EF-037, no order: S1 of 8
  settles the fold at itself: clingo pins the values (fault: the temporal read of "peil" …)`, `EF-037, some order: clingo
  refuses in the named sentence: expected a fault at solving, got one in translation (…)`.
- Punt 3, de drie documenten door de oude runner (`logs/15-red-point3-old.log`): drie keer `TRANSLATION FAULT: EF-037`.

### Groen

clingo-suite `test_clingo: 2578 checks, 0 failures` (was 2548), 46 s (`logs/25-suite-timed.log`); ref-suite ongewijzigd
(2391); de Drools-suite in gate 12: `JUnit 1847 tests` (was 1846), nul skips; `make gates` hieronder.

### Mutaties (niet gecommit; `git worktree add --detach`, opgeruimd; `logs/18-mutations/summary.txt`, `logs/23-mutation17`, `logs/19-drools-mutation.log`)

Per mutatie de clingo-suite en de negen corpuscasussen van de kaart door de runner:

| mutatie | rood |
|---|---|
| M01 `fault_where_reached` emit niets | 5 frames (`WF-044 …`, `'de dag' outside …`, `AANTAL_GEVULDE …`, `Rekenjaar …`, `EF-011: the default at its turn …`) en de drie nieuwe rode casussen en `clamp-reads-bounds-in-order` |
| M02 bereik alleen `isa` | `EF-021: … behind a condition that fails`, `EF-011: a default below …`; de drie nieuwe goldens |
| M03 arm `BoundDay` weg | `'de dag' outside …`; rood `bound-day-behind-a-holding-condition` |
| M04 arm `Unresolved`-voorwaarde weg | `WF-044 an Unresolved rule condition is cited` |
| M05 arm gevuld-telling weg | `AANTAL_GEVULDE: the temporal set source is declined …` |
| M06 EF-024 zonder `isa` | `EF-024: … over a type without instances` |
| M07 EF-024 weg | `EF-024: … over an instance`; rood `uniqueness-two-temporal-keys` |
| M08 geen foutdagen | 4 frames (o.a. `unbound parameter in a day-kind condition …`, `dk-walk-2`, `dk-count-2`); rood `day-kind-parameter-unbound` (EF-009 i.p.v. EF-014) |
| M09 stap zonder telgrens | `dk-walk-1` |
| M10 geen kortsluiting in `dk_cond` | `dk-walk-1`, `dk-count-1` |
| M11 telling één dag te ver | `dk-count-1` |
| M12 lid met falend filter telt mee | `horizon fence: a member whose filter faults is no contributor` |
| M13 clamp gaat door na de grens | rood `clamp-reads-bounds-in-order` (CLINGO-005 i.p.v. EF-011; gate 8 houdt het vast) |
| M14 arm CLINGO-035 weg | `EF-037, some order: …` |
| M15 geen tweelingen | 26 frames (`cr-3-8`, `cr-8-3`, … en `EF-037, some order`) |
| M16 EF-022 weer bij het vertalen | `nested day-kind test on a different day …`, `EF-022: … behind a condition that fails` |
| M17 hekken na een foutdag | eerst niets rood (de controle zat op `dk-walk-2`, waar geen hek vuurt); na de controle op het frame waarin elke dag faalt: `unbound parameter in a day-kind condition: the walk passes no reach fence` (`logs/23-mutation17`, en op `a8f2b44dd` `logs/26-mutations-walk`) |
| M08b, M09b, M18 (op `a8f2b44dd`, de rekensom) | geen feiten `fp(D,M)`: 6 frames; de stap zonder rangvoorwaarde: `dk-walk-1`; `dkx` met een verkeerde grens: `… the walk passes no reach fence` (`logs/26-mutations-walk`) |
| D1 Drools `raises` → `false` | `TmsDefaultsPerDayWitnessTest.a_fault_in_a_consequence_stands_at_the_node_that_raised_it` |

### Codes en pins (oud → nieuw)

- **CLINGO-035** nieuw, `refusal`, `creation-order-not-normed`, geen parameters, `viol_code`: de genoemde tweeling van elke
  bewaking. Oud las `viol_code` de tweeling van de parameterbewaking als EF-014 (de zin begint met "parameter … is unbound
  in this case"); EF-037 onder zo'n bereik was een vertaalfout EF-037.
- `scripts/check-drools.sh` (beschermd, voorstel) `EXPECTED_PLAYED` 330 → 334, met regels in de commentaargeschiedenis.
- Geen lijstregel erbij of eraf: gate 6 `EXPECTED_CHANNEL`, gate 8 divergentie- en uitsluitingslijst, gate 12, gate 17
  `DEVIATIONS` ongewijzigd. Geen vloer verschoven (gate 8 telt nu 71 rode casussen en 35 CLINGO-codes, boven de vloeren).

### Tests die de kaart omdraait (oud → nieuw)

Tien frames verwachtten een vertaalfout of een luide weigering; ze verwachten nu de fout bij het oplossen met code
(`faults_at_solving ~code`) of een antwoord van beide:
- `'de dag' outside a day-kind evaluation`: `faults_with` → EF-010;
- `unbound parameter in a day-kind condition fails fail-fast`: `faults_with` → EF-014 `grensdatum`;
- `nested day-kind test on a different day than the bound day`: `faults_with` → EF-022;
- `AANTAL_GEVULDE: the temporal set source is refused by its own arm` → `… declined by its own arm`, EF-021 `gewicht`;
  de tweede controle ("the refusal is not the expr_term bottom sentence") is opgegaan in de code-eis van
  `faults_at_solving`, die strenger is;
- `horizon fence: an unbound parameter in the die/dat filter, in translation` → `…, at solving`, EF-014 `minimumgewicht`;
- `README [L1]: … (hf-cond)` en `README [L1]: … (dk-cond)`: `pinned_loud` → `both_answer`;
- `WF-044 an Unresolved rule condition is cited, not swallowed`: `faults_with` → EF-019, naald "Unresolved condition
  reached in evaluation" (was "… in translation: this rule fails observably");
- `Rekenjaar without rekendatum: clingo refuses`: vertaalfout → `… clingo faults where it is evaluated`, EF-011.

Nieuw (+30 checks): de frames van EF-021, EF-022, EF-024 (leeg type en met instantie), EF-011 in een default onder de
beslissende, EF-018 achter 1 > 2 (ref en clingo falen allebei), de dagsoortstap en -telling (`dk-walk-0/1/2`,
`dk-count-0/1/2`), de twee controles op het bereikhek, de controle op de bijdragers van het horizonhek en de drie frames van
punt 3.

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS** (ch. 5.7b: het voorbeeld van de clamp, de getuigen van EF-010, EF-011, EF-019; ch. 5.12: de grens
van de contributor na de stop, het voorbeeld "-som"): in `76a4851c5`. **ASP-TRANSLATION** (S4, S6a, de
periode-aritmetiek, S12): in `78dfd3979`. **clingo-README**: in `2cb8ab34a` en `a357be8e1`.

**CONSTRUCTS**, rij `ParameterRef` (r. 107): vervang "Loud, a refusal where ref plays: the day-kind table and the §7.1
horizon fence read at translation and refuse there, also behind a failing condition; and a Let barrier that only …" door
"Loud, a refusal where ref plays: a Let barrier that only …"; na "… give the named sentence where ref's words were due."
voeg toe "The named sentence is a refusal of the ASP backend, CLINGO-035, for every guard that has one." en aan de
landingen "2026-11-04, card `early-faults-and-sum-reading`: the day-kind table and the horizon fence fault where ref reads
their parameter; the named sentence is CLINGO-035."

**CONSTRUCTS**, rijen 16, `Onopgelost` (Expr) en `Onopgelost` (Conditie), voeg toe: "Since card
`early-faults-and-sum-reading` (2026-11-04) the ASP backend faults where the evaluation reaches the node, as ref (golden
`unresolved-behind-a-failing-condition`, red `unresolved-behind-a-holding-condition`)."

**CONSTRUCTS**, rij 25 (contextwaarden), voeg toe: "A context value in a session without an evaluation date, and the
bound day outside a day-kind evaluation, fault where the evaluation reaches them in every executor (goldens
`context-value-behind-a-failing-condition`, `bound-day-behind-a-failing-condition`; card `early-faults-and-sum-reading`,
2026-11-04)."

**CONSTRUCTS**, rij 23 (dagsoorten), voeg toe: "The ASP backend records the days on which the condition faults (EF-014,
EF-022), reads its composite in the order of ch. 5.10, and faults where a step or a count reaches such a day (card
`early-faults-and-sum-reading`, 2026-11-04)."

**CLAUDE.md**: geen wijziging nodig; de tekst van gate 6, 8 en 12 noemt geen fout bij het vertalen en geen getal van
`EXPECTED_PLAYED`, en de vloeren van gate 8 staan.

### Voorlopige besluiten

- VB-1: de term van een knoop die faalt, leidt niets af (`#false`) en niet een constante: waar het bereik ref mist (de
  stille [S1] van de Let-verlaging) blijft de waarde dan leeg, zoals bij een ongebonden parameter, en geen getal.
- VB-2: een clampgrens die een contextwaarde zonder rekendatum is, telt als falend, en de clamp stopt erna; anders gaf
  `clamp-reads-bounds-in-order` CLINGO-005 en had gate 8 een divergentieregel nodig.
- VB-3: horizonhek: alleen een filter dat met EF-014 faalt, maakt een lid tot niet-bijdrager; een andere fout houdt het
  lid (de bestaande conservatieve kant).
- VB-4: een `Unresolved`-voorwaarde neemt haar bereik bij een verse expressie, dus zonder Let-barrière (een voorwaarde
  heeft geen plaats in de evaluatievolgorde van een Let); luid in de richting van een fout waar ref een Let eerder
  beëindigt, en geen document komt daar.
- VB-5: één code, CLINGO-035, voor elke genoemde tweeling (parameter, EF-037 en de vormen van punt 2), zonder parameters.
- VB-6: EF-018 en EF-020 ongewijzigd: gemeten gelijk aan ref (vraag 2).
- VB-7: Drools: alleen een knoop waarvan de invoer zijn eigen lezing is (`raises`), draagt de fout; de voorwaarde per
  periode blijft zoals ref en de bestaande test haar lezen (vraag 1).
- VB-8: het bereikhek stopt na een foutdag; waar ref eerst 40 000 dagen zonder dag van de soort zou lopen, faalt clingo op
  de dag en ref op het hek (README, grens; geen document komt daar).
- VB-9: voor de vormen die Drools bij het bouwen weigert (EF-014 in een dagsoort, EF-021, EF-022, EF-024) de getuigen in de
  clingo-suite en niet in het corpus, zodat er geen lijstregel bij komt; voor EF-010, EF-011 en EF-019, die Drools draagt,
  het corpus.
- VB-10: corpusnamen `<vorm>-behind-a-failing-condition` (golden) en `<vorm>-behind-a-holding-condition` (rood), en
  `jutterswet-gevolg-buiten-de-beurt-som`.

### Vragen aan de hoofdsessie

1. Ch. 5.12 tegen ref: een voorwaarde per periode waarvan de operand op de emmerscalair faalt, lezen ref en Drools als
   `0 Comparison unreached`, `1 AttributeReference fault EF-037` (Drools-test
   `TmsConditionPerPeriodWitnessTest.a_condition_per_period_over_the_bucket_scalar_faults`; ref's `read_periods`
   registreert een fout alleen aan knopen die via `around` lopen). Ch. 5.12 zegt dat een knoop de fout draagt die hij
   opwerpt of doorgeeft, dus `0 Comparison fault EF-037`. Ref en Drools aanpassen (een kaart), of bedoelt ch. 5.12 dat hier
   anders?
2. EF-018: ref geeft het overal waar de creatie draait, ook achter een voorwaarde die niet geldt en zonder subject; de
   ASP-backend en Drools (DROOLS-080, bij het bouwen) doen hetzelfde. Is dat de norm (dan is het eigenlijk een
   eigenschap van het document, een WF-regel), of hoort het pas waar een object gemaakt wordt?
3. Buiten de EF-codes gevonden: de uniciteitsunie van `ever-unique-temporal-key-bucket-scalar` over een leeg type geeft in
   clingo `TRANSLATION FAULT: unscalable datatype in the ASP translation`, zonder code, waar ref speelt. Een kaart, of
   noteren?

### make gates

Een run aan het eind in de worktree op `a8f2b44dd` (`out/gates/20261104-200715/`, log `gates-5.log`); de commit daarna
raakt alleen deze kaart. Vier runs ervoor waren ook groen (`gates-1` t/m `gates-4`), op tussenstanden; in `gates-3`
liep gate 6 632 s en gate 8 304 s door de kwadratische ontdubbeling van `e8a1b6a42`.

```
start 2026-11-04 20:07:15 a8f2b44dd card/early-faults-and-sum-reading
ocaml rc=0 0s ocaml-build: rebuilt (1 source path(s) newer than the stamp, e.g. backends/clingo/lib/translate.ml; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 560 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 134s
04-case-schema rc=0 7s
03-verifier rc=0 43s
05-oracle rc=0 25s
06-clingo rc=0 289s
07-noop rc=0 15s
08-red rc=0 177s
09-catala rc=0 5s
10-regelspraak rc=0 258s
11-toka-parity rc=0 229s
12-drools rc=0 363s
13-reftype rc=0 342s
14-flogic rc=0 2s
15-graph rc=0 52s
16-scenario rc=0 43s
17-readings rc=0 274s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 20:13:18 363s red=0
```

Gate 5: ref-suite 2391, 1072 cases. Gate 6: clingo-suite 2578, 1068 cases. Gate 8: 71 rode cases (was 68), 37
foutsoorten, Drools-been 54 met de code van de casus. Gate 12: JUnit 1847 tests, `played 334`, DRL-tekst `played 334`. Gate
17: 335 goldens, 943 case files, 7406 lezingen, 24135 knoopregels, 3906 niet bereikt, deviations 0.
