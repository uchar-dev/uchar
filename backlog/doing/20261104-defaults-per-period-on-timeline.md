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
