# defaults-per-period-on-timeline: een defaults-groep op een tijdlijndoel beslist per periode

*Opgesteld door de hoofdsessie op 2026-11-04, op verzoek van de eigenaar (lezing B per periode).*

## Aanleiding

Invoer op een berekend attribuut heeft in uchar twee lezingen (`spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md`, besloten
2026-09-25). Lezing A, de standaard: de invoer is de laagste default boven de initialisatie (RS ch. 7.8, de virtual input default); een
regel die beslist, verdringt haar, en sinds 2026-11-04 per dag en voor elke schrijver (kaarten `writers-per-day-beside-input`,
`empty-write-decides-nothing`). Lezing B, "opgegeven, niet afgeleid", per attribuut met de dialectmarkering `(mag opgegeven worden)`
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7): de frontend verlaagt haar tot een defaults-groep met de opgave boven de berekening.

B kan niet op een tijdsafhankelijk attribuut: de frontend weigert de markering daar (RGS-150), en de verifier weigert elke defaults-groep
op een tijdlijndoel (IR-SPEC WF-086: "een defaults-groep beslist op één fixpunt en heeft geen tijdcoördinaat om op te schrijven";
PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a, niet ingepland). De praktijk vraagt het wel: een vastgesteld dagloon of een correctie van een
behandelaar geldt voor een periode, en daarbuiten de berekening.

**Besluit van de eigenaar (2026-11-04):** B per periode bouwen.

## Goal

Een defaults-groep (`prioritizedDefaults`) op een tijdsafhankelijk attribuut beslist per periode, gelezen per dag zoals heel ch. 3: op elke
dag wint de hoogste default die die dag beslist (zijn rechtvaardiging geldt die dag en zijn waarde leest die dag een waarde); een lege
waarde valt per dag door; de virtual input default leest de invoer van die dag. Op een maand- of jaarraster is dat per emmer, met de
rastereisen van WF-126. De markering `(mag opgegeven worden)` mag dan op een tijdsafhankelijk attribuut en verlaagt zoals op een scalair.
Uitgewerkt voorbeeld (absoluut): `strandloon` per maand met de markering, invoer 10 op elke maand van 2025, een regel die 20 schrijft op
maart–mei: **10** op heel 2025. Dezelfde regel met invoer alleen op januari–februari: **10** op januari–februari, **20** op maart–mei,
leeg daarbuiten.

Wat er vanzelf meekomt, meet en noem je: een uitzondering (`Uitzondering op`, de defaults-tilt van het dialect) op een tijdlijndoel, en de
uitleg van een default per periode (RS ch. 5.12, gate 17).

## Acceptance

- (a) Stap A — spec, verifier, frontend, ref, clingo: RS ch. 7 (7.1, 7.6, 7.8, 7.10 waar ze een tijdlijndoel raken) als huidige stand
  (voorstel), met het uitgewerkte voorbeeld dat `ref/test` letterlijk overneemt; WF-086 houdt alleen de kenmerkarm (een tijdsafhankelijk
  kenmerk blijft buiten, RS ch. 3.12 rij 7), met een nieuwe witness; de attribuutarm van de negatieve `wf086-temporal-default-target`
  wordt een golden of een negatief van een regel die blijft (de kaart verandert die regel uitdrukkelijk; noem oud en nieuw); RGS-150 valt
  weg of houdt alleen wat blijft; een golden met `source.rgs` en de markering op een tijdlijn, met minstens de twee gevallen van het
  voorbeeld, in ref en clingo.
- (b) Stap B — Drools, beide branches: de defaults-stapels per periode, met de lezingen van ch. 5.12; de golden speelt in gate 12 en 17.
- (c) Rood eerst per uitvoerder; per nieuwe bewaking een mutatie die een test rood maakt.
- (d) De corpusmeting: geen bestaande verwachting hoort te veranderen (geen golden draagt nu een defaults-groep op een tijdlijn); meet het.
- (e) Voorgestelde tekst voor CONSTRUCTS (rij 8 en rij 11, delta tegen v0) en zo nodig CLAUDE.md, als huidige stand.
- (f) `make gates` groen, na elke stap.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/verifier/` (voorstel), `frontends/regelspraak/`, `ref/`, `backends/clingo/`,
`backends/drools/**`, `corpus/golden/`, `corpus/negative/` (de WF-086-arm, met reden), `scripts/` (alleen lijsten en vloeren die schuiven;
voorstel), kaartverplaatsing.

Out of scope: een defaults-groep op een tijdsafhankelijk kenmerk; de dienst van de distributie, die nog geen tijdsafhankelijk attribuut als
invoer neemt (een kaart in `regelspraak-toolchain`, daarna een pin-bump).

## Omvang

Middel tot groot; twee stappen, elk landen zodra groen.

## Result

## Result: stap A (bouwer, 2026-11-04)

*De kaart blijft in `doing/`; niet gemerged, niet gepusht. Logs onder `werk/defaults-per-period-on-timeline/logs/` (buiten de
repo), het mutatiescript onder `werk/defaults-per-period-on-timeline/scripts/mutations.py`, proeven onder `…/probes/`.*

### Commits

| hash | inhoud |
|---|---|
| `0492aa27b` | kaart naar `doing/` |
| `f8ae5bcee` | verifier (beschermd, voorstel): WF-086 zonder attribuutarm; WF-126 en de precisiepoort van WF-023 over de defaults van een tijdlijndoel; negatieven |
| `07e722df7` | frontend: de markering op een tijdsafhankelijk attribuut; RGS-150 weg, RGS-375 nieuw; `InputMarkingTest`; DIVERGENCES §7 |
| `c7785148b` | ref: `run_defaults_timeline`; golden `jutterswet-opgave-per-periode`; fixtures `ref/test/fixtures/defaults-per-day/` (4); ref-suite |
| `52b5b3958` | clingo: `translate_defaults_timeline`; clingo-suite; `backends/clingo/README.md` |
| `0264f7895` | verifier (beschermd, voorstel): eigen armsleutels `WF-126.justification-grid` en `WF-126.consequence-grid` |
| `e8ec8ff31` | `scripts/check-drools.sh` (beschermd, voorstel): de golden in `EXPECTED_CHANNEL` |
| `b18d5b55c` | RUNTIME-SEMANTICS en IR-SPEC (beschermd, voorstel) |
| `6061a5539` | fixture `verdrongen-conflict` in beide suites en in RS ch. 7.11 |
| (deze) | Result |

### Wat per laag veranderde

De lezing: op elke dag van een tijdlijndoel wint de hoogste default die die dag beslist. Geen tweede schrijfmechanisme: de groep
schrijft als één schrijver van het doel (ref `write_produced`/`World.write_timeline_part`, clingo `wtl/6` naast `inputtl/5`), dus
de invoer staat op de dagen die zij niet beslist — de virtual input default per dag. Het scalaire pad is in ref en clingo
ongewijzigd (een aparte tak voor een tijdlijndoel).

- **RS** (voorstel, huidige stand): nieuw ch. 7.11 (de regel per dag, het schrijven, de evaluatie, de scalair als één segment,
  welgevormdheid, zes uitgewerkte voorbeelden die `ref/test` letterlijk overneemt); ch. 7.1 (per dag, geen tijdsafhankelijk
  kenmerk), 7.6 punt 4, 7.8 (virtual input default per dag), 7.10 (de effectieve stapel per dag), de statuskop van ch. 7, 3.3
  (rechtvaardiging en consequentie volgen het doel), 3.11 (de defaults-groep als producent), 3.12 rij 7 (WF-086), rij 8a (de
  geldigheid van een groep), nieuwe rij 10 (de lezing van ch. 5.12 per dag, open).
- **IR-SPEC** (voorstel): WF-086 alleen de kenmerkarm, met "Delta (narrowing)"; WF-126 met armen (c) consequentie en (d)
  rechtvaardiging; WF-023 noemt de defaults-consequentie op een tijdlijndoel; beide tabelrijen bijgewerkt.
- **verifier** (voorstel): `wf086` zonder attribuutarm; `wf126` loopt ook over de defaults (met uitzonderingsgroepen) van een
  groep op een tijdlijnattribuut; `wf023_precision` neemt de consequentie van zo'n groep mee, met de pro-rata-uitzondering van een
  regel.
- **frontend**: de markering op een tijdsafhankelijk X verlaagt stuk 1–3: `opgegeven X` en `berekende X` krijgen X's
  granulariteit, telling, tijdlijnnaam en vast startpunt; de groep op X. Stuk 4–5 (afwijkingskenmerk, regel, consistentie) niet
  (VB-1); een regel die `afwijkende opgave X` leest, krijgt RGS-375. RGS-150 is uit register en catalogi (gat in de nummering, zoals
  eerder).
- **ref**: `run_defaults_timeline` met de dagverzamelingen `decide_days`/`reconcile_days`/`own_days`; `run_defaults_group` stuurt
  een tijdlijnattribuut daarheen; de fout "temporal attribute … as a defaults target" is weg, die op een tijdsafhankelijk kenmerk
  blijft (verdediging achter WF-086).
- **clingo**: `translate_chain_days`/`translate_group_days`/`translate_defaults_timeline` (de ketenpredicaten met een periode
  `(S,X,Y)` van de knipset `grpG_d_per/3`); `translate_period_arithmetic ?write:false` levert `p_val/4`; `written_timeline` en
  `timeline_input` kennen de defaults-groep; de ch. 7.7-desugaring van een initialisatie op een tijdlijndoel en een consequentie
  die geen periode-aritmetiek is, worden bij naam geweigerd.
- **Drools**: niet aangeraakt.

### Corpusmeting

- Scan van elke `ir.json` in `corpus/`, `toka/`, `ref/test/fixtures/` en `backends/**/fixtures`: geen golden, rode case, TOKA-document
  of fixture draagt een defaults-groep op een tijdlijnattribuut, en geen initialisatie op een tijdlijndoel. Het enige document was
  de negatief `wf086-temporal-default-target`.
- Geen bestaande verwachting veranderd: in `make gates` replayen gate 5 (1067 cases) en gate 6 (1063 cases) exact, gate 12 speelt
  ongewijzigd 328 (+ 328 DRL-tekst), gate 7 houdt 41 overlevers, gate 17 vergelijkt 329 goldens zonder afwijking. Nieuw in de
  tellingen: de golden (5 cases) en de suites.
- **De negatief (oud → nieuw).** Oud: `corpus/negative/wf086-temporal-default-target`, een groep op `wachtgeld` (per maand) met
  de rechtvaardiging "hij kustwacht is" (scalair kenmerk) en consequentie 30, geweigerd met `WF-086.temporal-attribute-target`. Dat
  document is nu welgevormd en speelt (gemeten: verifier PASS; ref en clingo PASS op een case met J1 kustwacht → 30 op heel de
  tijd, J2 zonder kustwacht → zijn invoer 12 op 2025). Nieuw: `git mv` naar `corpus/negative/wf126-defaults-condition-grid`, met
  `kustwacht` per dag: `WF-126.justification-grid`. Erbij: `wf126-defaults-value-grid` (`WF-126.consequence-grid`),
  `wf023-precision-timeline-default` (`WF-023.precision-unbounded`), en een `expect.txt` voor
  `wf086-temporal-default-characteristic` (`WF-086.temporal-characteristic-target`), de nieuwe witness van de arm die blijft.

### Rood eerst (op main `995fd8606`, in een losse worktree, log `04-red-main-golden.log`)

Op de golden: frontend `ERROR (line 3/4): attribuut 'strandloon' van Jutter: '(mag opgegeven worden)' staat niet op een
tijdsafhankelijk attribuut — … (WF-086) (dialect §7)` (RGS-150, ×2); verifier `WF-086.temporal-attribute-target FAIL
/rules/prioritizedDefaults/0` en `/1`; ref op alle vijf cases `EVALUATION FAULT: temporal attribute "strandloon" as a defaults
target: temporal writes are not implemented`; clingo op alle vijf `TRANSLATION FAULT: temporal attribute "opgegeven strandloon" in a
scalar slot is refused (ch. 3.3)`; Drools (ongewijzigd) op beide takken `EXCLUDE DROOLS-271: transpiler SKIP: defaults: defaults
A:Jutter.strandloon — the time-dependent attribute 'strandloon' of Jutter at a place that reads or writes one value …`. Op de
proef met `Uitzondering op` dezelfde ref-fout en clingo `… temporal attribute "basisloon" in a scalar slot …`.

### Groen

ref-suite 2374 worked examples; clingo-suite 2510 checks, 0 failures; frontend-suite 1496 tests, 0 failed, 0 skipped;
Drools-suite 1842 tests, 0 failures, 0 errors, 0 skipped (los gedraaid, log `12`, en in gate 12).

### Mutaties (niet gecommit, losse `git worktree --detach`, opgeruimd; logs `13-*`)

| mutatie | rood |
|---|---|
| V1 verifier: WF-126 zonder de defaults | `wf126-defaults-condition-grid` en `-value-grid`: `verifier: PASS` |
| V2 verifier: de attribuutarm van WF-086 terug | de golden: `WF-086.temporal-attribute-target FAIL` ×2 |
| V3 verifier: precisiepoort alleen op een scalaire groep | `wf023-precision-timeline-default`: `verifier: PASS` |
| R1 ref: geen verdringing per dag | 4 FAIL (`WV-7.11-initialisatie-per-dag`, `-uitzondering`, `-afwijking`, `-versie`) |
| R2 ref: initialisatie leest de invoer niet | `WV-7.11-initialisatie-per-dag` FAIL |
| R3 ref: geen conflict per dag | `WV-7.11-ongeordend`, `WV-7.11-afwijking` FAIL |
| R4 ref: groep vervangt de hele tijdlijn | 4 FAIL (o.a. `-invoer-op-het-doel`: 99 in juni–december weg) |
| R5 ref: uitzonderingsgroepen over alle tijd | `WV-7.11-verdrongen-conflict` FAIL (60 in april weg, conflict gedeponeerd) |
| C1 clingo: geen verdringing per periode | eerst 0 failures (de keten-uitkomst verdringt ook zonder); na de fixture `verdrongen-conflict`: 1 FAIL ("expected consistent, but the model carries 1 inconsistent fact(s)") |
| C2 clingo: initialisatie zonder invoerknip | case5 FAIL |
| C3 clingo: groep schrijft geen `wtl/6` | 3 FAIL (`uitzondering`, `ongeordend`, case4) |
| C4 clingo: geen knipset van de groep | 9 FAIL |
| F1 frontend: geen tijdlijn op de gegenereerde attributen | `InputMarkingTest`: `expected: <JAAR> but was: <null>` |
| F2 frontend: geen RGS-375 | `InputMarkingTest`: `verwachtte precies één diagnose, kreeg: []` |

### Tests die de kaart omdraait (oud → nieuw)

- `InputMarkingTest.the_marking_on_a_time_dependent_attribute_is_refused` (ERROR RGS-150, regel 3) →
  `on_a_time_dependent_attribute_the_marking_builds_the_two_attributes_and_the_group` (geen diagnose; `opgegeven waarde` en
  `berekende waarde` per jaar; de groep; geen kenmerk, geen regel, geen consistentie) en nieuw
  `on_a_time_dependent_attribute_a_read_of_the_deviation_characteristic_is_refused` (RGS-375, regel 4).
- De negatief `wf086-temporal-default-target` → `wf126-defaults-condition-grid` (hierboven).

### Wat vanzelf meekomt (gemeten)

- **`Uitzondering op` op een tijdlijndoel**: de frontend verlaagt het ongewijzigd; alleen WF-086 weigerde het. Nu per dag in ref en
  clingo (fixture `uitzondering`). Een rasterfout in de uitzondering weigert de frontend al vóór het optillen (RGS-374).
- **`Uitzonderingen ongeordend`** op een tijdlijndoel: conflict per dag, de invoer staat op de conflictdagen, één feit
  (`ongeordend`).
- **De uitleg van een default per periode (ch. 5.12, gate 17)**: de trace van ref leest een default van een tijdlijngroep als op
  een scalair doel. Gemeten op case2 en case5: elke default `HOLDS`, de rechtvaardiging `altijd` `true`, de consequentie
  (`zijn opgegeven strandloon`) `AttributeReference fault -`; geen default `DISPLACED`. Gate 17 is groen omdat Drools de golden
  weigert; RS ch. 3.12 rij 10 zet de lezing per dag open.

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS** en **IR-SPEC**: in `b18d5b55c` en `6061a5539`.

**CONSTRUCTS rij 8**, na "… widened to every computing rule.":

> **A defaults group on a timeline target decides per day (2026-11-04, card `defaults-per-period-on-timeline`, RUNTIME-SEMANTICS
> ch. 7.11):** on every day the highest default that decides that day wins; a consequence that reads leeg on a day falls through
> on that day, the initialisation default and the virtual input default read the input of that day, and unordered members and
> exception groups reconcile per day, a conflict on a day writing nothing that day and depositing one §9.5 fact per key × subject.
> The group is one writer of the target, so its input stands on the days it does not decide. A time-dependent characteristic stays
> no target (WF-086). In ref and clingo (golden `jutterswet-opgave-per-periode`, fixtures `ref/test/fixtures/defaults-per-day`);
> the Drools backend refuses it on both branches (DROOLS-271). Delta against v0: WF-086's attribute arm, ratified 2026-08-29
> (PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a), is gone.

**CONSTRUCTS rij 11**, de landing achteraan:

> 2026-11-04, card `defaults-per-period-on-timeline` (step A): a defaults group on a timeline attribute target decides per day in
> ref and clingo; WF-126 and the precision gate of WF-023 hold for its defaults; the opgave marker on a time-dependent attribute
> (golden `jutterswet-opgave-per-periode`).

**ASP-TRANSLATION** (buiten mijn mandaat, niet aangeraakt): S2 r. 207–209 "The input timeline of a timeline target that a computing
rule writes enters as `inputtl/5` …" → "… that a computing rule or a defaults group writes enters as `inputtl/5` and stands on the
days no writer decides (RS ch. 3.11, 7.11)."; S10 na de alinea "Standing input" een alinea:

> **Per day on a timeline target** (RS ch. 7.11): the chain and group predicates of the scalar lowering carry a period
> `(S,X,Y)` of the group's cut set `grpG_d_per/3`, which holds every bound of a justification's periods (`period_condition`), of
> a consequence's own cut set (the period arithmetic read as `p_val/4`, without its write) and of the input timeline. A default
> decides a period where it is reached, its justification holds and its consequence reads a value; the initialisation default's
> justification is a period the input leaves empty (`grpG_in/3`). The decided periods outside a conflict are written in maximal
> runs as `tl/5` and as the group's `wtl/6`, so the case input stands on every other period (S9). A deposit of a default stands
> where it decides on some period.

**CLAUDE.md**: geen wijziging nodig (geen gate-tekst noemt WF-086, RGS-150 of de defaults op een tijdlijn; de vloeren van gate 3
en 10 blijven ondergrenzen: 251 armsleutels ≥ 239, 364 RGS-codes ≥ 347).

**PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a** (hoofdsessie): de attribuutarm van (a) is op 2026-11-04 vervallen.

### Wat stap B moet doen

- Drools, beide takken: de defaults-stapels per dag. De golden `jutterswet-opgave-per-periode` (5 cases) staat in
  `EXPECTED_CHANNEL` met `EXCLUDE DROOLS-271` omdat beide takken haar weigeren; als ze speelt, gaat de regel weg en wordt
  `EXPECTED_PLAYED` 329. De vijf fixtures van `ref/test/fixtures/defaults-per-day` horen als witnesses in de Drools-suite, zoals
  `writers-per-day` in `TemporalFixpointWitnessTest`.
- De lezing van ch. 5.12 per dag (RS ch. 3.12 rij 10): definiëren in RS en bouwen in de trace van ref en in de readings van
  Drools; anders vergelijkt gate 17 een scalaire lezing zodra de golden speelt.

### Voorlopige besluiten

- VB-1: op een tijdsafhankelijk X verlaagt de markering stuk 1–3; geen afwijkingskenmerk, geen regel, geen consistentie (de
  voorwaarde vergelijkt twee tijdlijnen en toetst gevuld op een tijdlijn, ch. 3.12 rij 5; een scalair kenmerk kan niet gelden op
  sommige dagen). Een lezer van `afwijkende opgave X` krijgt RGS-375 (één nieuwe registercode).
- VB-2: wat een default deponeert, staat waar hij op minstens één dag beslist (zijn evaluatie is die over alle tijd, ch. 3.11), niet
  per dag.
- VB-3: in ref en clingo is de consequentie periode-aritmetiek; het tijdsevenredig deel en de dagentelling in een default worden bij
  naam geweigerd (geen corpuscase). RS noemt de consequentie een producent van ch. 3.11.
- VB-4: de geldigheid van een groep op een tijdlijndoel poort op de rekendatum, zoals een regelversie (`WV-7.11-versie`).
- VB-5: een emmerscalair als invoer van het doel blijft staan (clingo `val/3`, zoals VB-3 van `writers-per-day-beside-input`);
  EF-037 waar de groep een initialisatiedefault heeft, die de invoer per dag leest (ref en clingo).
- VB-6: de oude negatief wordt een WF-126-negatief, geen golden; de waarde- en precisiearm krijgen elk een negatief.
- VB-7: de WF-126-armen van een default heten `justification-grid` en `consequence-grid` (gate 3: één sjabloon per sleutel).
- VB-8: clingo weigert de ch. 7.7-desugaring van een initialisatie op een tijdlijndoel bij naam; ref speelt die v0-vormen native.

### Vragen aan de hoofdsessie

1. VB-1: de markering op een tijdlijn zonder spoor van een afwijkende opgave op het §9.5-kanaal — zo bedoeld, of wacht dat op een
   lezing per dag van gevuld en van twee tijdlijnen (ch. 3.12 rij 5) en een kenmerk zonder venster (rij 7)?
2. De uitzonderingsboom van ch. 7.9 op een tijdlijn (fixtures `afwijking`, `verdrongen-conflict`): gebouwd als vallend onder het
   besluit, met conflict en verdringing per dag en de padsleutels van ch. 7.9. Akkoord?
3. Het versievenster (fixture `geldig-vanaf`): de groep poort op de rekendatum en schrijft dan per dag (VB-4). Akkoord, of hoort dit
   bij ch. 3.12 rij 8a?
4. VB-2: het speculatieve deponeren per default in plaats van per dag — goed genoeg, of per dag?
5. De lezing van ch. 5.12 per dag: in stap B, of een eigen kaart?
6. Een cel van een gedimensioneerd attribuut op een tijdlijn bestaat niet (WF-125.timeline): geen vraag, alleen genoemd.

### make gates

Eén run in de worktree op `6061a5539` (`out/gates/20261104-150032/`); de commit daarna raakt alleen deze kaart.

```
start 2026-11-04 15:00:32 6061a5539 card/defaults-per-period-on-timeline
ocaml rc=0 1s ocaml-build: rebuilt (7 source path(s) newer than the stamp, e.g. backends/clingo/test/test_clingo.ml; 0s)
frontend rc=0 1s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 3s drools-jar: rebuilt (4 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/RuleChecker.java; 2s)
01-self-contained rc=0 3s
02-ir-schema rc=0 235s
04-case-schema rc=0 9s
03-verifier rc=0 39s
05-oracle rc=0 26s
06-clingo rc=0 335s
07-noop rc=0 14s
08-red rc=0 173s
09-catala rc=0 6s
10-regelspraak rc=0 282s
11-toka-parity rc=0 299s
12-drools rc=0 416s
13-reftype rc=0 378s
14-flogic rc=0 2s
15-graph rc=0 53s
16-scenario rc=0 45s
17-readings rc=0 285s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 15:07:31 419s red=0
```

## Result: stap B (bouwer, 2026-11-04)

*Niet gemerged, niet gepusht. Logs onder `werk/defaults-per-period-on-timeline/logs/` (`B*`), het mutatiescript onder
`werk/defaults-per-period-on-timeline/scripts/mutationsB.py`.*

### Commits

| hash | inhoud |
|---|---|
| `544060d6c` | ref: de trace leest een default per dag (`reading.ml`); `Eval.input_empty_days` uit `run_defaults_timeline` gehaald; ref-tests `WV-5.12-default-per-dag` (vijf rijen) en `WV-7.11-gevolg-buiten-de-beurt`, `-fout`, `-deponering`, `-initialisatie-emmerscalair`; documenten `defaults-per-day/lazy-consequence` en `deposit`; cases `defaults-per-day-cases/{turn-on-no-day, justified-on-no-day, bucket-input, lazy-consequence-reached}` |
| `e03b98cf7` | clingo-suite: het document `deposit` |
| `1c6e05ce7` | drools: de stapel per dag (`PeriodDefaults`, `TmsTranspiler.dayStackRules`), de lezing per dag, `decisionsOf` weigert zo'n build; `TmsDefaultsPerDayWitnessTest`; README; drie corpustests (hieronder) |
| `7711cc4fe` | `scripts/check-drools.sh` (beschermd, voorstel): de golden uit `EXPECTED_CHANNEL`, `EXPECTED_PLAYED` 328 → 329 |
| `320612fee` | RUNTIME-SEMANTICS (beschermd, voorstel): ch. 5.12 "A default on a timeline target" met `WV-5.12-default-per-dag`, de statustabel, de artefacten; ch. 3.12 rij 10 weg (beslist); ch. 7.11 de evaluatie en vier uitgewerkte voorbeelden |
| `799bb2f43` | case `defaults-per-day-cases/deposit-dropped` in ref, clingo en de Drools-witness |
| `bb5cbbfd7` | RUNTIME-SEMANTICS (beschermd, voorstel): `WV-7.11-deponering-vervalt` genoemd |
| (deze) | Result |

### Wat er in Drools veranderde

Geen tweede mechanisme: de stapel per dag gebruikt de controlefeiten van de algemene vorm en het schrijven per dag dat er
al was.

- **Uitkomst met dagen.** Elke default en elke verzoening van uitzonderingsgroepen zet een `Outcome` (`DECIDED`) met als
  waarde een `PeriodDefaults.Days`: de beslissende segmenten met hun exacte waarde en per conflictsleutel de dagen waarop
  de leden verschillen. Een default en zijn terugval zetten er elk een.
- **De beurt per dag.** De regel van een default leest via accumulates (`Reads.outcomes`) de uitkomsten van de posities
  boven hem in zijn keten en boven elke default waarvan zijn keten een uitzonderingsgroep is, en matcht alleen waar die
  een dag overlaten (`DAYS_REACH`, toets `DAYS_AT_TURN`); de terugval leest daarnaast de uitkomsten van zijn eigen
  uitzonderingsgroepen (`DAYS_LEFT`).
- **Lui gevolg.** Het besluit is een `Let` per inperking (dagen van de beurt ∩ rechtvaardiging per periode, voor de
  initialisatiedefault eerst ∩ de invoerloze dagen van de bean, `DAYS_INPUT_EMPTY`): een lege inperking geeft null, de
  binder eindigt de `Let` met leeg, en het gevolg (periode-aritmetiek, `periodOperand`) wordt dan niet geëvalueerd. Fout en
  deponering zijn die van `Consequence.Decision`; de deponering staat waar de default op een dag beslist.
- **De wortel is één schrijver.** `…: eens wortel` verzoent de wortelgroepen per dag en schrijft een `Value` (writer =
  stapelnaam) via `PERIOD_WRITTEN` (schrijfgrens per segment); `timelineWriters` kent `dayStackTargets`, dus de standing en
  de write-back zetten de invoer op de andere dagen (`writersUnionRules`, `PeriodCondition.union`). Per mogelijke sleutel
  (`targetForm` bij twee of meer wortelgroepen, `k.key()` bij twee of meer uitzonderingsgroepen) een regel
  `…: verschil <sleutel>` met `Consequence.Inconsistency` (EF-027).
- **Plan, geen nieuwe weergave.** Alleen nieuwe `StaticFunction`s (`DAYS_*`), twee `ReadKind`s (`OUTCOMES`,
  `BUCKET_INPUTS`) en `DefaultsGroup.perDay`; beide branches renderen hetzelfde plan, geen nieuwe `Consequence`-soort.
- **Weigeringen** met bestaande codes: een gevolg dat geen periode-aritmetiek is (DROOLS-105) en een doel dat geen getal is
  (DROOLS-303). Geen nieuwe code; geen code naar `RETIRED` (DROOLS-271 blijft voor elke plek die één waarde houdt).
- **`decisionsOf`** weigert een build met zo'n stapel (`DefaultsDecision` heeft één beslisser per object).

### Wat er in de uitleg veranderde (ch. 5.12, gate 17)

- **ref** (`reading.ml`): de lezing loopt de keten per dag zoals `run_defaults_timeline` (`Eval.reconcile_days`,
  `settled_days`, `days_minus`), leest elke default ook waar zijn beurt leeg is, de rechtvaardiging via `cond_periods`, het
  gevolg via `period_timeline` waar de rechtvaardiging op een dag geldt, en de wortel als `Timeline.clip` van de gevulde
  segmenten op de periodes van de rechtvaardiging.
- **Drools**: de leesregel van zo'n default heeft de accumulates en geen toets; `ReadingRule` draagt `turn` en
  `inputDays`; `readingsOf` geeft `DISPLACED` waar de beurt leeg is en `DISPLACED` zonder knopen waar de invoer het doel elke
  dag vult. Het gevolg in de lezing staat onder een `Choice` (niet bereikt waar de rechtvaardiging op geen dag geldt).
- Gate 17 vergelijkt de golden nu op beide branches; vloeren onaangeroerd.

### Rood eerst (letterlijk)

- Drools op main `dd81d2ae1` (`B01-red-golden-drools.log`), beide branches en `--readings`: `EXCLUDE DROOLS-271: transpiler
  SKIP: defaults: defaults A:Jutter.strandloon — the time-dependent attribute 'strandloon' of Jutter at a place that reads
  or writes one value …`.
- ref-trace op main: `reading /rules/prioritizedDefaults/0/defaults/0 "J2" 1 AttributeReference fault -` en elke default
  `HOLDS`.
- De nieuwe ref-tests tegen de bibliotheek van main (`B21-red-ref-tests.log`): `FAIL WV-5.12-default-per-dag`,
  `-initialisatie`, `-verdrongen`, `-niet-gerechtvaardigd`, `-uitzonderingsgroepen` (5 failure(s)).
- `TmsDefaultsPerDayWitnessTest` tegen main (`B21-red-drools-witness.log`): 4 tests, 2 failures, 2 errors, elk
  `CaseRunner$Excluded: transpiler SKIP: defaults: defaults A:Jutter.strandloon — het tijdsafhankelijke attribuut
  'strandloon' van Jutter op een plek die één waarde leest of schrijft …`.
- Eigen rood onderweg: na de eerste verlaging las Drools `0/defaults/0 "J3" 1 Literal empty` waar ref `unreached` las (de
  barrière van een `Let` noteert de knoop als bereikt en leeg) → `Choice`; de suite (`B06`) gaf
  `RuleOriginCorpusWitnessTest`: "de rol is RECONCILE_AGREEMENT, het label in de naam zegt null" (`…: schrijven`) →
  `eens wortel`/`verschil <sleutel>`, en drie corpustests `IllegalState decisionsOf leest per object en stapel één beslisser`
  → overgeslagen (hieronder).

### Groen

ref-suite 2389 worked examples (was 2374); clingo-suite 2514 checks, 0 failures (was 2510); Drools-suite 1846 tests,
0 failures, 0 errors, 0 skipped (was 1842; `B10`); frontend niet aangeraakt.

### Mutaties (niet gecommit, `git worktree add --detach`, opgeruimd; logs `B20-mutation-*`)

| mutatie | rood |
|---|---|
| D1 geen uitkomsten van hoger (beurt = alle tijd) | `the_documents_play…` (Excluded/EvaluationFaults), lezingen J6 |
| D2 initialisatie leest de invoer niet | `the_documents_play…`: case5 |
| D3 geen conflict per dag | `the_documents_play…`: `ongeordend`, `afwijking` |
| D4 terugval over de hele beurt | `the_documents_play…`: `afwijking` |
| D5 gevolg ook buiten de beurt geëvalueerd | `EvaluationFaults … reads the temporal attribute "kustloon" of "J1"` (`lazy-consequence`) |
| D6 uitkomst zonder dag (deponering blijft) | eerst groen: `inconsistent` toetst alleen aanwezigheid; na de case `deposit-dropped`: `deposit-dropped/case.json exec-model case: verwacht consistent` |
| D7 lezing zonder beurt | lezingen J6: `DISPLACED` verwacht |
| D8 lezing zonder invoerloze dagen | lezingen J6: `…/1/defaults/2 "J6" DISPLACED` zonder knopen verwacht |
| D9 lezing bereikt het gevolg altijd | lezingen J3: `1 Literal unreached` verwacht |
| D10 `decisionsOf` weigert niet | `decisions_of_a_stack_per_day_are_refused`: "keten 0 … laat per object één uitkomst staan" |
| D11 geen emmerscalair-fout | `a_default_faults…`: `expected: <FAULT> but was: <NOFAULT>` |
| D12 geen conflictregels | `the_documents_play…`: `ongeordend`, `afwijking` |
| D13 groep vervangt de invoer (`dayStackTargets` weg) | `the_documents_play…`: case4 en case5 |
| R11 trace zonder beurt | `FAIL WV-5.12-default-per-dag-verdrongen` |
| R12 trace leest een gevulde invoer | `FAIL WV-5.12-default-per-dag-verdrongen` |
| R13 trace bereikt het gevolg altijd | `FAIL WV-5.12-default-per-dag-niet-gerechtvaardigd` |

### Tests die de kaart verandert (oud → nieuw)

- `DefaultsDecisionCorpusWitnessTest` en `StackCompositionCorpusWitnessTest.againstDecisions`: een golden met een stapel
  per dag wordt niet getoetst (oud: zo'n golden speelde niet en viel er zo buiten; nieuw: `perDay(build)` slaat hem over,
  want `decisionsOf` weigert hem). `ReadingsCorpusWitnessTest`: voor zo'n build `decisions = List.of()`, de toetsen van de
  lezingen blijven. Vloeren ongewijzigd en gehaald.
- `ReadingRule` kreeg twee componenten, `DefaultsGroup` één (`perDay`); geen test construeert ze.

### De pins

- Gate 12 (`scripts/check-drools.sh`, voorstel): de regel `jutterswet-opgave-per-periode|EXCLUDE DROOLS-271|…` uit
  `EXPECTED_CHANNEL` (49 → 48 niet-gespeelde goldens bij naam); `EXPECTED_PLAYED` 328 → 329 (met regel in de
  commentaargeschiedenis); DRL-tekst speelt 328 → 329.
- Gate 17: geen vloer geraakt. Gemeten (stap A → B): 329 → 330 goldens, 933 → 938 case files, 7352 → 7387 lezingen,
  24006 → 24081 knoopregels, 858 → 923 met een tijdlijn; 568 knoopregels van 231 verdrongen lezingen ongewijzigd (de golden
  heeft geen `DISPLACED`; dat pinnen `WV-5.12-default-per-dag-verdrongen` en de Drools-witness).

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS**: in `320612fee` en `bb5cbbfd7` (ch. 5.12 "A default on a timeline target", `WV-5.12-default-per-dag`,
de statustabel en de artefacten; ch. 3.12 rij 10 weg; ch. 7.11 "The evaluation" en `WV-7.11-gevolg-buiten-de-beurt`,
`-fout`, `-deponering`, `-deponering-vervalt`, `-initialisatie-emmerscalair`).

**CONSTRUCTS rij 8** (vervangt het voorstel van stap A):

> **A defaults group on a timeline target decides per day (2026-11-04, card `defaults-per-period-on-timeline`,
> RUNTIME-SEMANTICS ch. 7.11):** on every day the highest default that decides that day wins; a consequence that reads leeg
> on a day falls through on that day, the initialisation default and the virtual input default read the input of that
> day, and unordered members and exception groups reconcile per day, a conflict on a day writing nothing that day and
> depositing one §9.5 fact per key × subject. The group is one writer of the target, so its input stands on the days it
> does not decide. A default is evaluated where it is at its turn on some day, its consequence where its justification
> holds on a day of its turn, and its deposit stands where it decides on some day. Its reading at the fixpoint is the
> reading per day of ch. 5.12: its justification per period, its consequence where that holds on some day, `HOLDS` where
> it is at its turn on some day and `DISPLACED` where on none. A time-dependent characteristic stays no target (WF-086).
> In ref, clingo and both branches of the Drools backend (golden `jutterswet-opgave-per-periode`, documents
> `ref/test/fixtures/defaults-per-day`, cases `ref/test/fixtures/defaults-per-day-cases`); the ASP backend faults while it
> translates on a read of the asOfDate-bucket scalar in a consequence that ref and the Drools backend do not evaluate
> (`lazy-consequence`). Delta against v0: WF-086's attribute arm, ratified 2026-08-29 (PRIORITIZED-DEFAULTS-EVOLUTIE §9
> Q12a), is gone.

**CONSTRUCTS rij 11**, de landing achteraan:

> 2026-11-04, card `defaults-per-period-on-timeline` (step B): the Drools backend decides a defaults group on a timeline
> target per day on both branches (`TmsTranspiler.dayStackRules`, `PeriodDefaults`); the golden
> `jutterswet-opgave-per-periode` plays in gate 12 and its readings per day are compared in gate 17 (RUNTIME-SEMANTICS
> ch. 5.12).

**CLAUDE.md gate 12**, na de zinnen van `conditions-per-period` (stap B deel ii):

> Since the card `defaults-per-period-on-timeline` (step B, 2026-11-04) a defaults group on a timeline target decides per
> day on both branches (RUNTIME-SEMANTICS ch. 7.11; `TmsTranspiler.dayStackRules`, `PeriodDefaults`): every default and
> every reconciliation of exception groups puts the days it decides or conflicts on in the value of its `Outcome`, a lower
> default reads those of the defaults above it off an accumulate and decides only on the days they leave, its consequence
> evaluated only where it can decide, and the root of the stack is one writer of the target, beside whose `Value` the input
> stands on the other days (`Consequence.WritersUnion`); `decisionsOf` refuses a build with such a stack.

**CLAUDE.md gate 17**, na de alinea van `readings-of-displaced-defaults`:

> Since the card `defaults-per-period-on-timeline` (step B, 2026-11-04) a default of a group on a timeline target is read
> per day on both sides (ch. 5.12, "A default on a timeline target"): its justification per period, its consequence where
> that holds on some day, its root the filled segments within the justification's periods, the status `HOLDS` where it is
> at its turn on some day and `DISPLACED` where on none; its lines are compared verbatim as every other, and no floor moved.

**ASP-TRANSLATION / clingo README** (buiten mijn mandaat, niet aangeraakt): zie vraag 1.

### Voorlopige besluiten

- VB-B1: de statusregel van een default per dag noemt geen dagen: `HOLDS` waar hij op minstens één dag aan de beurt is,
  `DISPLACED` waar op geen; de knopen over alle tijd, niet gesneden op de beurt. De dagen van de beurt volgen uit de
  lezingen erboven; gate 17 leest de status als het laatste woord van de regel, en `scripts/` mag ik alleen in lijsten en
  vloeren raken.
- VB-B2: de wortel van het gevolg in de lezing is de gevulde tijdlijn binnen de periodes van de rechtvaardiging, exact,
  zonder schrijfgrens: wat de default zou schrijven; de schrijfgrens geldt voor het schrijven van de groep.
- VB-B3: een rechtvaardiging die faalt geeft `FAULTS` waar de default aan de beurt is en `DISPLACED` waar niet (de fout aan
  de knopen); een fout van de invoer van de initialisatiedefault (EF-037) geeft `FAULTS` zonder knopen.
- VB-B4: `decisionsOf` weigert een build met een stapel per dag (fail fast); de drie corpuswitnesses van `decisionsOf`
  laten zo'n golden weg, `ReadingsCorpusWitnessTest` toetst zijn lezingen zonder beslisser.
- VB-B5: hergebruik van `Outcome` met een `Days`-waarde, `Consequence.Decision`, `Consequence.Value` en
  `Consequence.Inconsistency`; luiheid via `Term.Let` met een null-binder; de reconciliatie per dag in Java
  (`PeriodDefaults.reconciled`) als spiegel van `Eval.reconcile_days`.
- VB-B6: nieuwe map- en bestandsnamen Engels (`lazy-consequence`, `deposit`, `defaults-per-day-cases/*`), naast de
  Nederlandse namen van stap A.

### Vragen aan de hoofdsessie

1. clingo faalt bij het vertalen (`TRANSLATION FAULT: EF-037 … "kustloon" …`) op `lazy-consequence`, waar ref en Drools het
   gevolg niet evalueren (ch. 7.11: een gevolg buiten de beurt geeft geen fout); hetzelfde met een ongebonden
   tijdsafhankelijke parameter (EF-014). `backends/clingo/README.md` zegt dat een fail-fast alleen vuurt waar ref zijn plek
   evalueert. Een clingo-kaart (een fout als feit onder de beslissing), of een vastgelegde grens van het ASP-backend in
   ASP-TRANSLATION? Nu staat het als feit in RS ch. 7.11 en in geen lijst; het document zit niet in de clingo-suite.
2. `decisionsOf` per dag (een beslisser per periode) voor de uitleg van de distributie: mee met de toolchain-kaart voor
   tijdsafhankelijke invoer?
3. Moet de statusregel van een lezing per dag de dagen van de beurt dragen (VB-B1)? Dat vraagt een parser in gate 17 die
   een status niet als laatste woord leest.

### make gates

Een run aan het eind in de worktree op `bb5cbbfd7` (`out/gates/20261104-160501/`); de commits daarna raken alleen deze
kaart. (Een eerdere run op `320612fee`, `out/gates/20261104-155400/`, was ook groen.)

```
start 2026-11-04 16:05:01 bb5cbbfd7 card/defaults-per-period-on-timeline
ocaml rc=0 0s ocaml-build: rebuilt (5 source path(s) newer than the stamp, e.g. backends/clingo/test/test_clingo.ml; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 2s drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. backends/drools/src/test/java/dev/uchar/regelspraak/cases/TmsDefaultsPerDayWitnessTest.java; 2s)
01-self-contained rc=0 3s
02-ir-schema rc=0 292s
04-case-schema rc=0 7s
03-verifier rc=0 39s
05-oracle rc=0 25s
06-clingo rc=0 442s
07-noop rc=0 13s
08-red rc=0 182s
09-catala rc=0 5s
10-regelspraak rc=0 378s
11-toka-parity rc=0 367s
12-drools rc=0 563s
13-reftype rc=0 507s
14-flogic rc=0 1s
15-graph rc=0 50s
16-scenario rc=0 39s
17-readings rc=0 358s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 16:14:27 565s red=0
```

Gate 12: `played 329`, DRL-tekst `played 329`. Gate 17: 330 goldens, 938 case files, 7387 lezingen, 24081 knoopregels,
deviations 0.
