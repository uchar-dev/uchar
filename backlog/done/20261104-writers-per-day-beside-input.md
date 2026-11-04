# writers-per-day-beside-input: invoer en schrijvers per dag op een tijdlijndoel

*Opgesteld door de hoofdsessie op 2026-11-04, na de herschrijving van RUNTIME-SEMANTICS ch. 3 (ch. 3.12 rij 10) en een meting tegen
ALEF (`werk/writers-and-input/verslag.md`, buiten de repo).*

## Aanleiding

Een dag van een tijdlijndoel met invoer uit de case leest nu de invoer of leeg, afhankelijk van het aantal schrijvers:

- één schrijver: de geschreven tijdlijn vervangt het doel helemaal, ook op dagen waar zij leeg leest (RS ch. 3.11 "Leeg in a write",
  ch. 5.7 "On a target of one writer …", ch. 7.8 "the timeline arm", besluit van 2026-08-21: invoer en berekende segmenten mengen nooit);
- twee of meer schrijvers: een schrijver beslist alleen de dagen waarop zijn tijdlijn een waarde leest, en op een dag die geen schrijver
  beslist, staat de invoer (ch. 5.7 "Two writers per day", IMPLICIT-TIME punt 5, VB 5.2).

ALEF voegt elke schrijfactie per moment samen met de huidige waarde, de invoer inbegrepen, ook bij één schrijver. Gemeten: invoer 10 over
2025 en één regel die 20 schrijft in maart–mei geeft in ALEF 10 in januari en juli; uchar geeft daar leeg. De specificatie zwijgt (§9.3).

**Besluit van de eigenaar (2026-11-04):** per dag, ook bij één schrijver (`spec/proposals/IMPLICIT-TIME.md`, punt 5).

## Goal

Op een tijdlijndoel beslist elke schrijver, ook een enige, alleen de dagen waarop zijn tijdlijn een waarde leest. Op een dag die geen
schrijver beslist, staat wat het doel vóór de schrijvers hield: zijn invoer of leeg. Op een dag die een schrijver beslist, staat zijn
waarde; twee schrijvers die een dag verschillend beslissen, blijven EF-016. Dat geldt in ref, clingo en beide Drools-branches, en in RS als
één regel in plaats van drie.

Ongewijzigd: een scalair doel (ch. 1.2, de standing norm van ch. 7.8 voor een scalair), een kenmerkdoel (ch. 3.12 rij 7: een
case-tijdlijn van een kenmerk dat een regel schrijft, is niet gedefinieerd), de ASP-afschermingen die er niet over gaan.

## Acceptance

- (a) Rood eerst, letterlijk: het gemeten geval (invoer 10 over 2025, één schrijver 20 op maart–mei) faalt op main in ref, clingo en
  beide Drools-branches met de verwachting 10, 20, 10 en speelt daarna; hetzelfde met de waarde 10 (de schrijver schrijft wat de invoer
  heeft). Per nieuwe bewaking een mutatie die een test rood maakt.
- (b) Het tweede uitgewerkte voorbeeld van het anker `jutterswet-borgdeel` (`case6-berekend-over-input`: een vouw die jan–feb 12 beslist
  over een invoer met een segment in maart) verandert door het besluit: de invoer staat dan in maart. De nieuwe verwachting staat als
  voorstel in het anker, met oud en nieuw in het Result. Elke andere golden waarvan een verwachting verandert, staat met oud, nieuw en
  reden in het Result; een golden die alleen door de oude regel slaagde of faalde, wordt genoemd.
- (c) RS als huidige stand (voorstel): ch. 3.11 "Leeg in a write" één regel voor het tijdlijndoel; ch. 5.7 de zinnen over één en twee
  schrijvers; ch. 7.8 "the timeline arm" per dag (het besluit van 2026-08-21 herzien op 2026-11-04, in één zin); ch. 3.12 rij 10 weg of
  teruggebracht tot wat open blijft; een uitgewerkt voorbeeld met absolute waarden dat `ref/test` letterlijk overneemt.
- (d) De no-op-lijst van gate 7 en de kanaallijsten van gate 6, 8 en 12 kloppen; een verschuiving staat met reden in het Result.
- (e) Voorgestelde tekst voor CONSTRUCTS (de rij die de standing norm en de tijdlijnschrijver draagt, en rij 11) als huidige stand.
- (f) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `backends/drools/**`, `spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/golden/` (alleen verwachtingen
die het besluit verandert, met reden; `anchor/` als voorstel), `corpus/red/`, `scripts/` (alleen lijsten die verschuiven; voorstel),
kaartverplaatsing.

Out of scope: een defaults-groep op een tijdlijndoel (meet hoe ze nu leest en of het corpus er een heeft; geen wijziging, vraag aan de
hoofdsessie als het besluit haar raakt); de dienst van de distributie (die neemt geen tijdsafhankelijk attribuut als invoer).

## Omvang

Middel.

## Result

*Bouwer, 2026-11-04. De kaart blijft in `doing/`; niet gemerged, niet gepusht. Logs onder
`werk/writers-per-day-beside-input/logs/` (buiten de repo).*

### Commits

| hash | inhoud |
|---|---|
| `081c3367f` | kaart naar `doing/` |
| `582a66f01` | ref; fixtures `ref/test/fixtures/writers-per-day/`; ref-suite; anker `jutterswet-borgdeel` case6 (voorstel) |
| `51da5f608` | clingo; clingo-suite; `backends/clingo/README.md` |
| `98f811b6b` | Drools (beide takken); drie tests omgedraaid of aangepast (hieronder); `backends/drools/README.md` |
| `944b71c73` | RUNTIME-SEMANTICS ch. 3.10, 3.11, 3.12 rij 10, 5.7, 7.8 (voorstel) |
| (deze) | Result |

### Wat per uitvoerder veranderde

De regel is één regel: een schrijver van een tijdlijndoel, ook een enige, beslist de dagen waarop zijn
tijdlijn een waarde leest; het doel is de unie van de schrijvers met daaronder wat het vóór hen hield (invoer
of leeg); EF-016 waar twee een dag verschillend beslissen. Het pad van twee schrijvers draagt nu ook n = 1;
er is geen tweede pad meer. Geen nieuwe registercode.

- **ref** (`ref/lib/eval.ml`, `ref/lib/world.ml`): `write_produced` schrijft altijd via de delen per schrijver
  (`World.write_timeline_part`), ook bij één schrijver; een rekenende regel op een tijdlijndoel die niet beslist,
  geeft zijn dagen terug via `release_timeline_part`. Vervallen: de vervanging van het hele doel
  (`World.write_timeline_by`, de constructor `Held_timeline`) en de lezing van
  `Ir_v0.Desugar.standing_temporal_targets` in ref.
- **clingo** (`backends/clingo/lib/translate.ml`): elke rekenende regel van een tijdlijndoel schrijft zijn segmenten
  ook als `wtl/6` onder zijn index, ook als enige. De invoertijdlijn van zo'n doel komt binnen als `inputtl/5`
  (`written_timeline`); `timeline_input` knipt elk invoersegment op de grenzen van de schrijverssegmenten erbinnen
  (`incut/5`, `inpiece/6`) en een stuk dat geen schrijver dekt (`indecided/4`) is een `tl/5`-segment. Vervallen: de
  staande-tijdlijnarm (`prN_eff`, `adgN_eff`, `paN_eff`: heel of niets) en de weigering "the case supplies input for computed
  target" op een tijdlijndoel. Een asOfDate-emmerscalair op een tijdlijndoel komt binnen als `val/3`, ook naast twee
  schrijvers (VB-3).
- **Drools** (`TmsTranspiler`, `StandingTargets`): `timelineWriters` is waar zodra een rekenende regel het
  tijdlijndoel schrijft, dus de stand (`writersUnionRules`, `Consequence.WritersUnion`) en de write-back
  (`readFixpoint`, `PeriodCondition.union`) zijn bij één schrijver ook de unie met de invoer. Een leeg resultaat op een
  tijdlijndoel voegt geen `Value` toe (`writesEmpty` false): het beslist geen dag. `StandingTargets` verliest
  `standingTemporal` en `temporalTargets`. Beide takken renderen hetzelfde plan, dus de DRL-teksttak volgt.

### Corpusmeting

Scan van elke golden, rode case en fixture op invoer op een tijdlijndoel dat een regel schrijft
(`werk/.../scripts/scan_timeline_targets.py`, log `01-scan-golden.log`): vier golden casebestanden, geen rode case.

| golden / case | oud | nieuw | reden |
|---|---|---|---|
| `anchor/jutterswet-borgdeel/case6-berekend-over-input` (voorstel) | 12 op [2024-01-01, 2024-02-01), 12 op [2024-02-01, 2024-03-01) | dezelfde twee en **99 op [2024-03-01, 2024-04-01)** | de vouw beslist januari en februari; maart beslist hij niet, daar staat de invoer 99 |
| `anchor/jutterswet-borgdeel/case5-input-blijft` | 7 op [2024-03-01, 2024-05-01) | ongewijzigd | leeg resultaat, beslist geen dag; blijft gate-7-overlever |
| `anchor/jutterswet-dagentelling/case4-input-blijft` | 0 op (−∞, +∞) | ongewijzigd | de dagentelling beslist elke dag |
| `jutterswet-dagentelling-rgs/case4-input-blijft` | 0 op (−∞, +∞) | ongewijzigd | idem |

Gemeten op de branch tegen de verwachtingen van main: gate 5 (ref) faalt alleen op case6 (log `09-oracle-ref-branch.log`,
`MISMATCH: J1.borgdeel = [2024-01-01,2024-03-01)=12 [2024-03-01,2024-04-01)=99`); case6 met de oude verwachting geeft
in clingo, Drools exec-model en Drools DRL-tekst dezelfde nieuwe waarde (MISMATCH resp. FAIL). Met alleen case6 aangepast
spelen gates 5, 6 en 12 groen (1062, 1058 en 328 + 328). Geen andere golden slaagde of faalde alleen door de oude regel;
gate 11 (TOKA, 29 cases in ref, clingo en Drools) is ongewijzigd groen. Lijsten: gate 7 ongewijzigd (41 overlevers, case5
blijft), gate 6 `EXPECTED_CHANNEL` ongewijzigd (twee goldens), gate 8 divergentielijst ongewijzigd, gate 12 `EXPECTED_PLAYED`
328 en kanaallijst ongewijzigd. Geen script aangeraakt.

### Rood eerst, groen, mutaties

- **Rood op de code van main**, met de nieuwe fixtures (`een-schrijver`: invoer 10 over 2025, één schrijver 20 op
  maart–mei, verwacht 10, 20, 10; `een-schrijver-gelijk`: schrijver 10, verwacht 10 op heel 2025;
  `een-schrijver-gat`: schrijver "zijn basisloon" met een gat; `twee-schrijvers`: 20 en 30 naast invoer 10):
  ref-runner en ref-suite 3 FAIL (`WV-3.11-per-dag-invoer`, `-gelijk`, `-gat`; logs `04`, `05`); clingo-suite 7 FAIL
  (ref ×3, clingo ×4, alle vier op "the case supplies input for computed target"; log `06`); Drools
  `TemporalFixpointWitnessTest.a_writer_decides_its_own_days_and_the_input_stands_on_the_others_on_both_branches`
  rood op beide takken voor de drie fixtures met één schrijver, bijv. "verwacht [2025-01-01,2025-03-01)=10
  [2025-03-01,2025-06-01)=20 [2025-06-01,2026-01-01)=10, werkelijk [2025-03-01,2025-06-01)=20"; twee schrijvers
  speelden al (log `07`).
- **Groen**: ref-suite 2348 worked examples; clingo-suite 2482 checks, 0 failures; Drools-suite 1841 tests, 0 failures,
  0 skipped (log `17`).
- **Mutaties** (niet gecommit, in een losse `git worktree --detach`, opgeruimd):
  R1 ref, één schrijver vervangt het hele doel: 6 FAIL, o.a. de drie `WV-3.11-per-dag-invoer` (log `18`);
  R2 ref, `World.release` in plaats van `release_timeline_part`: `WV-6.1-written-timeline jaardeel-first` FAIL (log `19`);
  C1 clingo, invoerstukken zonder `not indecided`: 3 FAIL (log `20`);
  C2 clingo, invoer als `tl/5` in plaats van `inputtl/5`: 4 FAIL (log `21`);
  D1 Drools, `timelineWriters` weer pas vanaf twee schrijvers: `TemporalFixpointWitnessTest` 2 FAIL,
  `TmsEverUniquenessWitnessTest` 1 FAIL (log `22`).

### Tests die het besluit omdraait (oud → nieuw)

1. `TemporalFixpointWitnessTest.prorata_with_filled_opbrengst_displaces_the_input_time_sequence` heet nu
   `…_decides_its_own_bucket_beside_the_input`: oud alleen 12 in januari 2024; nieuw 12 in januari en de invoer 99 in maart.
2. `TmsTimelineWitnessTest` C (een versie "geldig vanaf 2020" met een leeg resultaat), nu
   `an_empty_calculation_of_a_version_decides_no_day_and_the_input_stays_on_the_bean`: J1 oud een lege lijst op de bean
   en verwachting leeg; nieuw de invoer 4 op 2024, op de bean en in de verwachting. J2 ongewijzigd 31.
3. `TmsEverUniquenessWitnessTest`: V6 oud 7 in 2026; nieuw 10 in 2023 (invoer, een jaar dat de regel niet beslist) en
   7 in 2026. De invoer van V7 staat nu in 2027 in plaats van 2023: in 2023 zou ze aan het fixpunt blijven staan en met V5
   botsen, en dan meet de toets op een tussenstand (V5 onderweg gemarkeerd, aan het fixpunt niet) niets meer. Het kanaal
   blijft V1, V2, V3, V4, V6. Opnieuw gemeten op de 141 documenten van `main` van deze twee klassen: verifier, ref en clingo
   PASS op alle (log `16`), ook C, dat clingo eerder weigerde; de drie proeven "V5/V7/V8 gemarkeerd" geven in ref en
   clingo MISMATCH.

### Voorgestelde tekst

**RUNTIME-SEMANTICS** (in `944b71c73`, als huidige stand): ch. 3.11 "A writer's own days and change" en "Leeg in a
write" met één regel voor het tijdlijndoel en het uitgewerkte voorbeeld `WV-3.11-per-dag-invoer` (ref-suite letterlijk);
"What 0 means" punt 2 "Beside the input"; ch. 3.10 de lege vensterzin; ch. 3.12 rij 10 teruggebracht tot wat open blijft
(leeg op een scalair doel); ch. 5.7 "Two writers per day" voor één en twee schrijvers; ch. 7.8 "The timeline arm" per dag,
met het besluit van 2026-08-21 herzien op 2026-11-04 in één zin en het anker met de nieuwe waarde.

**CONSTRUCTS rij 8**, na "two that differ, the fault of ch. 5.7 (maintainer decisions 2026-11-02).":

> On a timeline target the same holds per day, for one writer as for two or more: a writer decides the days on which
> its timeline reads a value, and on every other day the target holds its input or leeg; the timeline arm of the
> ch. 7.8 standing norm, decided 2026-08-21 for the whole timeline, reads so since 2026-11-04 (card
> `writers-per-day-beside-input`, RUNTIME-SEMANTICS ch. 3.11, 5.7, 7.8; the anchor `jutterswet-borgdeel`).

**CONSTRUCTS rij 11**, drie vervangingen en een landing:

- "Two writers of one timeline target are judged per day (ch. 5.7)." →
  "A writer of a timeline target, also its only one, decides the days on which its timeline reads a value, and on every
  other day the target holds its input or leeg; two writers are judged per day (ch. 3.11, 5.7)."
- in de ASP-grenzen "two writers that decide one day with one value ([V7]), and a case input on a target of two
  writers;" → "two writers that decide one day with one value ([V7]);"
- bij Drools "two writers per day and a read of their union," → "the writers of a timeline target per day beside its
  input, one or more, and a read of their union,"
- landing: "2026-11-04, card `writers-per-day-beside-input`: a writer of a timeline target, also its only one, decides
  its own days beside the input, in all executors (fixtures `ref/test/fixtures/writers-per-day`; the anchor
  `jutterswet-borgdeel`, case6)."

**CLAUDE.md**, gate 12: "two or more computing rules of one timeline target each write their own `Value`, and the
write-back puts their union with the target's input on the bean" → "the computing rules of one timeline target, one or
more, each write their own `Value`, and the write-back puts their union with the target's input on the bean"; en "the
standing of a timeline target of two writers is their union with its input" → "the standing of a timeline target that
rules write is the union of its writers with its input".

**ASP-TRANSLATION** (buiten mijn mandaat, niet aangeraakt), drie plaatsen beschrijven nog de oude arm:
r. 202–213 (invoerroutering: "`inputtl/5` (timeline)" van een staand doel; voorstel: "the input timeline of a timeline
target that a computing rule writes enters as `inputtl/5` and stands on the days no writer decides (RS ch. 3.11)", en
de weigering alleen voor een scalair rekendoel) en r. 220 (`jutterswet-borgdeel` "(the temporal arm)" → "(a writer
beside its input)"); r. 1511–1519 (de staande arm `prN_eff`/`adgN_eff`; voorstel: "Both producers write their segments
also as the writer's `wtl/6`, beside which the case input timeline (`inputtl/5`) stands on the days no writer decides:
each input segment is cut at the writers' bounds inside it, and a piece no writer covers is a `tl/5` segment (`incut/5`,
`inpiece/6`, `indecided/4`; RS ch. 3.11)"); r. 1705–1711 ("the temporal producers (S9, whole-timeline)" en "(temporal
standing)"). Het naschrift r. 4095 e.v. is gedateerd; daar volstaat "(per dag sinds 2026-11-04, RS ch. 3.11)".

**spec/verifier** (beschermd): `Desugar.standing_temporal_targets` en het commentaar erboven leest geen uitvoerder meer;
voorstel: weghalen.

### Voorlopige besluiten

- VB-1: een schrijver met een versievenster valt onder het besluit; de versie die op de rekendatum geldt, beslist zijn
  dagen. Reden: het besluit noemt elke schrijver, en het pad van twee schrijvers telde versies al mee in ref en Drools.
  Gemeten vóór: ref verving het hele doel, Drools zette een lege lijst op de bean, clingo weigerde (TmsTimelineWitnessTest
  C); na: alle drie houden de invoer. Geen golden draagt een versievenster op een tijdlijnregel (ch. 3.12 rij 8).
- VB-2: clingo met één generieke codering over `wtl/6` voor elke tijdlijnschrijver, alleen uitgezonden als de tekst een
  rekenende regel op een tijdlijndoel heeft.
- VB-3: de asOfDate-emmerscalair op een geschreven tijdlijndoel blijft in clingo een `val/3`-feit (de weg van het oude
  staande doel), ook naast twee schrijvers. Gemeten op een proef (`een-schrijver` met invoer 10 als emmerscalair,
  rekendatum 2025-07-15): ref en clingo lezen de puntwaarde 10 en de tijdlijn 20 op maart–mei; beide Drools-takken weigeren
  de scalaire teruglezing (DROOLS-041). Geen corpuscase.
- VB-4: Drools voegt bij een leeg resultaat op een tijdlijndoel geen `Value` toe; zo blijft een emmerscalair als stand
  staan waar geen schrijver iets schrijft, zoals in ref.
- VB-5: RS ch. 3.12 rij 10 teruggebracht, niet weggehaald: open blijft of een leeg van een niet-staande schrijver op een
  scalair doel ook niets schrijft, zoals ALEF's standaard (lazy) evaluatie het leest (meting geval 1d).
- VB-6: in RS ch. 7.8 is de zin "Multi-writer targets stay fenced on the ASP side …, as do versioned targets" weg: hij
  klopt niet meer voor het tijdlijndoel, en sinds 2026-11-02 ook niet voor scalaire medeschrijvers.

### Vragen aan de hoofdsessie

1. VB-1: valt de versieschrijver onder het besluit, zoals hier gebouwd?
2. Defaults-groep op een tijdlijndoel: gemeten op een proef (`jutterswet-vondstwaardering` met `waardering` en
   `basisbedrag` per maand): de verifier weigert met WF-086, ref weigert ("temporal writes are not implemented"),
   clingo weigert ("temporal attribute … in a scalar slot"); geen golden; ongewijzigd. Het besluit raakt haar niet zolang
   WF-086 staat.
3. Dimensie: de case geeft geen tijdlijn op een gedimensioneerd attribuut (RS ch. 5.7, 5.8c), dus invoer naast een
   schrijver komt daar niet voor; ongewijzigd.
4. Is de invoer per dag naast een enige schrijver een gevolgde v1-delta tegen v0 (CONSTRUCTS rij 11)? Het gedrag van v0
   voor deze vorm heb ik niet gemeten; ik heb de zin daarom onder de huidige stand gezet en niet onder de delta's.
5. Rij 10 van ch. 3.12: teruggebracht houden (VB-5) of weghalen?

### make gates

Eén run in de worktree op `944b71c73` (`out/gates/20261104-113142/`); de commit daarna raakt alleen deze kaart.

```
ocaml rc=0 1s ocaml-build: rebuilt (1 source path(s) newer than the stamp, e.g. backends/clingo/README.md; 0s)
frontend rc=0 1s frontend: rebuilt (0s)
jar rc=0 3s drools-jar: rebuilt (no shaded jar under backends/drools/target; 2s)
01-self-contained rc=0 2s
02-ir-schema rc=0 130s
04-case-schema rc=0 8s
03-verifier rc=0 43s
05-oracle rc=0 25s
06-clingo rc=0 269s
07-noop rc=0 15s
08-red rc=0 160s
09-catala rc=0 5s
10-regelspraak rc=0 246s
11-toka-parity rc=0 223s
12-drools rc=0 339s
13-reftype rc=0 306s
14-flogic rc=0 2s
15-graph rc=0 53s
16-scenario rc=0 41s
17-readings rc=0 255s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 11:37:24 342s red=0
```
