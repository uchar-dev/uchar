# empty-write-decides-nothing: een lege waarde beslist niets, ook op een scalair doel

*Opgesteld door de hoofdsessie op 2026-11-04, na de kaart `writers-per-day-beside-input` (RUNTIME-SEMANTICS ch. 3.12 rij 10) en de
meting tegen ALEF (`werk/writers-and-input/verslag.md`, buiten de repo, geval 1c en 1d).*

## Aanleiding

Op een tijdlijndoel beslist een schrijver de dagen waarop zijn tijdlijn leeg leest niet (besluit 2026-11-04). Op een scalair doel hangt
het nog af van de vorm: een **staande** schrijver (RS ch. 7.8, besluit 2026-08-20: één versievrije rekenende regel als enige schrijver)
schrijft bij een lege waarde niets en de invoer blijft staan; elke andere schrijver (naast een tweede schrijver, of met een
versievenster) schrijft leeg over de invoer (RS ch. 1.2 Assignment, ch. 3.11 "Leeg in a write", ch. 5.7 "a leeg one included where the
writer is not standing"). ALEF's standaardevaluatie slaat een lege waarde altijd over (`merlin/lazy/MLazyProperty.java` r. 53–56):
gemeten met invoer 40 en twee schrijvers, waarvan één met leeg vuurt, geeft ALEF 40 en ref leeg. De specificatie zegt in §9.1 dat een
gelijkstelling aan leeg leeg oplevert, en zwijgt over invoer (§9.3).

**Besluit van de eigenaar (2026-11-04):** een schrijver die met een lege waarde vuurt, beslist niets, op elk doel. Een scalair is één
segment over alle tijd (ch. 3.1), dus dit is de regel van het tijdlijndoel.

## Goal

Op een scalair doel schrijft een schrijver die vuurt met een lege waarde niets: het doel houdt wat het hield, zijn invoer of leeg. Dat geldt
voor elke schrijver: een staande, een naast een tweede schrijver, een met een versievenster. Twee schrijvers waarvan één leeg en één een
waarde geeft, geven die waarde (geen EF-016); twee met verschillende waarden blijven EF-016. In ref, clingo en beide Drools-branches, en in
RS als één regel voor het scalaire en het tijdlijndoel. Waar het staande domein (`Desugar.standing_targets`) dan alleen nog dit punt
droeg, valt het weg; waar het nog iets anders draagt (de routering van invoer in clingo, de desugaring van ch. 7.7), blijft het met die
taak.

Ongewijzigd: een defaults-groep (leeg valt daar al door), een initialisatie (ch. 7.7, §9.6), een kenmerkdoel, een verdeling en een
toekenning bij het maken van een object; meet ze waar het mechanisme gedeeld is, en noem een raakvlak als vraag.

## Acceptance

- (a) Rood eerst, letterlijk: met invoer 40 (1) twee schrijvers, één vuurt met leeg en de ander vuurt niet → 40; (2) een schrijver met een
  versievenster die met leeg vuurt → 40; (3) twee schrijvers, één leeg en één 50 → 50 zonder fout. Elk faalt op main in ref, clingo en
  beide Drools-branches waar uchar nu anders leest, en speelt daarna. Per nieuwe bewaking een mutatie die een test rood maakt.
- (b) De corpusmeting: elke verwachting die verandert (goldens en rode casussen), met oud, nieuw en reden; `anchor/` als voorstel.
- (c) RS als huidige stand (voorstel): ch. 1.2 (Assignment), ch. 3.11 "Leeg in a write", ch. 5.7 de schrijversregel, ch. 7.8 de standing
  norm (het besluit van 2026-08-20 verbreed op 2026-11-04, in één zin), ch. 3.12 rij 10 weg; een uitgewerkt voorbeeld met absolute waarden
  dat `ref/test` letterlijk overneemt.
- (d) De no-op-lijst van gate 7 en de kanaallijsten van gate 6, 8 en 12 kloppen; een verschuiving staat met reden in het Result.
- (e) Voorgestelde tekst voor CONSTRUCTS (de rij van de standing norm en de delta tegen v0, die leeg over de invoer schreef) als huidige
  stand.
- (f) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `backends/drools/**`, `spec/verifier/` (alleen het staande domein; voorstel),
`spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/golden/` (alleen verwachtingen die het besluit verandert; `anchor/` als voorstel),
`corpus/red/`, `scripts/` (alleen lijsten die verschuiven; voorstel), kaartverplaatsing.

Out of scope: de dienst van de distributie; de vormen onder "Ongewijzigd"; een regel die een attribuut bewust leegmaakt of "onbekend"
schrijft (RegelSpraak heeft er geen vorm voor; kaart `20261104-flavours-of-empty`).

## Omvang

Middel.

## Result

*Bouwer, 2026-11-04. De kaart blijft in `doing/`; niet gemerged, niet gepusht. Logs onder
`werk/empty-write-decides-nothing/logs/` (buiten de repo), meetscripts onder `werk/empty-write-decides-nothing/scripts/`.*

### Commits

| hash | inhoud |
|---|---|
| `9d9b92a8d` | kaart naar `doing/` |
| `9856d2729` | ref; fixtures `ref/test/fixtures/empty-write/` (drie documenten); ref-suite; golden `jutterswet-twee-schrijvers-aan-fixpunt` en `-omgekeerd` case6 |
| `0b64b4991` | clingo; clingo-suite; `backends/clingo/README.md`; `spec/verifier/lib/desugar.ml` (beschermd, voorstel) |
| `434dd92cd` | Drools (beide takken); zes witness-tests omgedraaid of aangepast (hieronder); `backends/drools/README.md` |
| `2bb17f347` | RUNTIME-SEMANTICS ch. 1.2, 3.11, 3.12, 5.7, 5.8c, 7.7, 7.8 (voorstel) |
| (deze) | Result |

### Wat per uitvoerder veranderde

Eén regel: een schrijver die met een lege waarde vuurt, beslist niets, op elk doel; het doel houdt de waarde van een
andere schrijver, een initialisatie, zijn invoer of blijft leeg; twee schrijvers met verschillende waarden blijven
EF-016. Geen apart pad meer voor de staande schrijver; geen nieuwe registercode.

- **ref** (`ref/lib/eval.ml`): een rekenende regel op een scalair doel die leeg oplevert, roept altijd `release ()`
  aan (geeft terug wat hij zelf schreef). Vervallen: de lezing van `Ir_v0.Desugar.standing_targets` en
  `desugared_targets` in ref.
- **clingo** (`backends/clingo/lib/translate.ml`): elke rekenende regel leidt zijn waarde ook onder zijn index af
  (`wval/4`, functie `writer_index`, was `co_writer` en alleen vanaf twee schrijvers) en `wdecides(S,a)`; op een
  staand doel staat de invoer met `val(S,a,V) :- inputval(S,a,V), ⟨base⟩, not wdecides(S,a).` De routering van invoer
  (`translate_case`) is één clausule: een staand doel waarop een rekenende regel die op de rekendatum geldt schrijft.
  Het conflict van twee schrijvers is `viol` op twee verschillende `wval/4`-waarden. Vervallen: `stateN_eff`, de
  co-eff-guards, `wfires/3`, `wfired/2`, `wfilled`, `wwrites` (de lege schrijfactie), `comp_fires/2` en het
  divergentiegetuige "no fixpoint: a computing rule evaluating leeg erases what the initialisation … writes". Het
  hek op invoer blijft alleen beside een initialisatie, een verdeling of een schrijfactie bij het maken (bericht nu:
  "the case supplies input for computed target %S, which an initialisation, an allocation or an object creation
  writes as well: not encoded here (no corpus case)"; geen registercode, zoals voorheen).
- **verifier** (beschermd, voorstel): `Desugar.standing_targets` draagt nog één taak, de routering van de invoer in
  clingo, en is verbreed tot "een scalair doel dat alleen rekenende regels schrijven", ongeacht aantal of
  versievenster (geen initialisatie op de sleutel, geen defaults-groep, verdeling of schrijfactie bij het maken).
  `desugared_targets` is weg: geen lezer meer (alleen ref las hem). `chosen_targets`/`document` (de desugaring van
  ch. 7.7, die clingo leest) ongewijzigd.
- **Drools** (`TmsTranspiler`): `computeRule` voegt bij een lege waarde nooit een `Value` in (`writesEmpty` false);
  `writeRule` verliest zijn parameter `writesEmpty`. Vervallen: `transpiler/core/StandingTargets` (geen lezer meer),
  de tweede variant van de initialisatiewacht (die lege schrijvers telde: `ReadKind.EMPTY_WRITERS`,
  `StaticFunction.TARGET_EMPTY`, `ComputeHelpers.targetEmpty`), en de verschilregel naast een initialisatie (alleen nog
  bij twee of meer rekenende regels). Beide takken renderen dezelfde analyse, dus de DRL-teksttak volgt.

### Corpusmeting

ref van main (`fe05b0b11`, in een losse worktree) tegen ref van de branch over elke golden case (1062) en elke rode
case (67), uitslag én `--trace` (`scripts/compare_ref.py`, log `07-ref-main-vs-branch.log`): twee cases verschillen,
geen rode.

| golden / case | oud | nieuw | reden |
|---|---|---|---|
| `jutterswet-twee-schrijvers-aan-fixpunt/case6-lege-boete-beslist` → `case6-lege-boete-beslist-niet` | spoedpremie **leeg** (meldingsgetal 30) | spoedpremie **7**, de invoer (meldingsgetal 30) | de tweede regel vuurt zonder boete en beslist niets |
| `jutterswet-twee-schrijvers-aan-fixpunt-omgekeerd/case6-…` (idem) | leeg | 7 | idem, regels in omgekeerde volgorde |

De NOTE.md van beide goldens zegt het nu zo. Geen anker verandert: `anchor/jutterswet-waardestelling` (40, 40, 40,
50) en `anchor/jutterswet-waardering-v0vorm` case5/case6 (25, 40) lezen al zo. Met alleen deze twee verwachtingen
aangepast zijn gates 5, 6, 8 en 12 groen: clingo en beide Drools-takken gaven op main dus leeg in case6 en geven nu 7,
en geen andere vastgepinde verwachting verschuift in een uitvoerder. Lijsten: gate 7 ongewijzigd (41 overlevers;
case6 overleeft niet, het meldingsgetal 30 tegen de invoer 3 weerlegt de no-op), gate 6 `EXPECTED_CHANNEL`
ongewijzigd (twee goldens), gate 8 divergentielijst ongewijzigd (9), gate 12 `EXPECTED_PLAYED` 328 en kanaallijst
(48) ongewijzigd. Geen script aangeraakt.

### Rood eerst, groen, mutaties

- **Rood op de code van main**, letterlijk (logs `02`, `03`, `04*`), met invoer taxatie 40:
  (1) `one-fires-empty` (twee schrijvers, één vuurt met leeg, de ander niet): ref `MISMATCH: V1.taxatie = leeg,
  expected 40`; clingo `V1.taxatie is underived (leeg), expected 40`; Drools exec-model en DRL-tekst `FAIL case.json:
  attribuut taxatie: verwacht 40, werkelijk leeg`.
  (2) `versioned-fires-empty` ("geldig vanaf 2020", rekendatum 2025-07-01): ref `V1.taxatie = leeg, expected 40`;
  clingo `TRANSLATION FAULT: the case supplies input for computed target "taxatie": ref (like v0) overwrites it, which
  has no monotone encoding here`; beide Drools-takken `verwacht 40, werkelijk leeg`.
  (3) `empty-and-value` (één leeg, één 50): ref `EF-016: no fixpoint within 100 passes … rule "taxatie uit opgave"
  writes leeg and rule "taxatie uit schatting" writes 50`; clingo `EF-016: clingo: UNSATISFIABLE … viol("two writers of
  one target disagree …")`; beide Drools-takken `ERROR EvaluationFaults: … the rule "taxatie uit opgave" writes empty
  and the rule "taxatie uit schatting" writes 50`.
  De suites op main met alleen de nieuwe tests: ref-suite 3 FAIL (`WV-3.11-leeg-beslist-niets`, `-versie`,
  `-naast-waarde`); clingo-suite 5 FAIL en daarna een `Fatal error: exception Uchar_ref.Value.Fault(… no fixpoint
  within 100 passes …)` uit `ref_matches` op `empty-and-value`; Drools
  `StandingScalarNormWitnessTest.a_leeg_write_decides_nothing_beside_a_second_writer_and_in_a_window` `exec-model —
  one-fires-empty ==> expected: <null> but was: <case: attribuut taxatie: verwacht 40, werkelijk leeg>`.
- **De bewering van de kaart** dat leeg naast 50 nu EF-016 geeft: gemeten waar in ref, clingo en beide Drools-takken
  (hierboven). Dat elke niet-staande schrijver nu leeg over de invoer schrijft: waar in ref en Drools; clingo schreef
  naast een tweede schrijver leeg, maar weigerde bij een versievenster (hek) in plaats van leeg te schrijven.
- **Groen**: ref-suite 2354 worked examples; clingo-suite 2490 checks, 0 failures; Drools-suite 1842 tests, 0
  failures, 0 skipped (logs `08`, `18`, `17`).
- **Mutaties** (niet gecommit, in een losse `git worktree --detach`, opgeruimd; `scripts/mutations.py`, log `19-*`):
  R1 ref, een lege waarde weer geschreven: 18 FAIL, o.a. `WV-3.11-leeg-beslist-niets*`, `WV-6.1-written-two-leeg`,
  `WV-5.8c-initialisatie-naast-berekening`; C1 clingo, de invoerregel met `not wdecides` weg: 3 FAIL (de drie
  fixtures); C2 clingo, routering zonder `applies` (geldigheidscontrole): 1 FAIL, de nieuwe check "a writer outside
  its window" (`V1.taxatie is underived (leeg), expected 40`); C3 verifier, `standing_targets` weer alleen
  versievrij: 1 FAIL (`versioned-fires-empty`, het hek); D1 Drools, een rekenende regel op een scalair doel weer
  `writesEmpty`: 12 failures en 3 errors in de vijf aangepaste klassen.

### Tests die het besluit omdraait (oud → nieuw)

1. ref `WV-6.1-written-two-leeg` (6 volgordes): spoedpremie leeg → 7.
2. clingo-suite "initialisation beside a computation that evaluates leeg: no fixpoint" (ref fout op het
   passbudget, clingo-getuige) → "…: the initialisation fills the target", beide 25,00.
3. clingo-suite "an initialisation with a condition beside a computing rule on one cell: no fixpoint where the
   computed value is leeg" (ref EF-016, clingo-getuige) → "…: the initialisation writes where the computed value is
   leeg", najaar 0.00 in ref en clingo.
4. Drools `StandingScalarNormWitnessTest`: `two_rules_that_compute_on_a_target_clear_the_input_value_indeed` (leeg) →
   `beside_a_second_rule_a_leeg_computation_leaves_the_input_value_standing` (77.00); `a_version_target_does_not_come_
   into_the_standing_domain` (leeg) → `a_versioned_rule_that_computes_leeg_leaves_the_input_value_standing` (77.00);
   beide houden een rode tegenrij (leeg verwacht, 77.00 gevonden). `a_defaults_group_gets_the_target_out_of_the_
   standing_domain` heet `a_rule_beside_a_defaults_group_on_its_target_is_refused`, de bewering (WF-050-weigering)
   ongewijzigd. Nieuw: `a_leeg_write_decides_nothing_beside_a_second_writer_and_in_a_window` (de fixtures).
5. `TmsInputOnComputedTargetWitnessTest.outside_the_standing_domain_writes_the_computing_rule_empty` →
   `beside_a_conditional_initialisation_a_computed_leeg_leaves_the_input_standing`: V2 bergloon leeg, uitkering 5 →
   bergloon 7, uitkering 12.
6. `TmsCreationInputWitnessTest.a_rule_outside_the_standing_domain_that_computes_empty_clears_the_value_of_the_
   creation` → `a_rule_that_computes_empty_leaves_the_value_of_the_creation_standing`: terug 0 → 7.
7. `OverlappingWritersWitnessTest.a_writer_that_computes_empty_opposite_one_that_writes_a_value` (EF-016, "schrijft
   leeg") → `a_writer_that_computes_empty_beside_one_that_writes_a_value_decides_nothing`: 2026-03-04, beide takken.
8. `TmsWritersWitnessTest`: eerste document V2 leeg → 99, V7 fout (7 tegen leeg) → speelt 7 (ook in de steekproef
   van gegenereerde volgordes: V2 99); derde document (naast een voorwaardelijke initialisatie) V2 leeg → 9, V5 en V6
   fout → 3 en 9; gegenereerde regels 6 → 5 (geen verschilregel naast een initialisatie).
9. `RulePlanWitnessTest.an_initialisation_beside_a_writer_that_writes_empty` → `an_initialisation_beside_two_
   computing_rules`: de wacht van de initialisatie is die zonder andere schrijver (geen `EMPTY_WRITERS`,
   `TARGET_EMPTY`); in dit plan en in `two_writers_of_a_target` en `a_clamped_validity_and_the_evaluation_date_state`
   `writesEmpty=true` → `false`.
10. `TmsIndexWitnessTest.identityJoins`: de facultatieve join "lege-schrijver-onderwerp" weg (bestaat niet meer).

De documenten die deze Drools-tests spelen (202, via een tijdelijke dumpnaad in `CaseRunner`, niet gecommit) zijn ook
door ref en clingo van main en van de branch gedraaid (`scripts/replay_dump.py`, logs `16`, `22`): op de branch
geeft ref overal de waarde of fout die Drools nu geeft; clingo ook, behalve het hek naast een initialisatie
(hieronder) en de twee bekende verschillen op een gemaakt object (106 en 12, ongewijzigd).

### Raakvlakken met "Ongewijzigd" (gemeten)

De vormen zelf lezen zoals nu; de rekenende regel ernaast schrijft bij leeg niets meer. Uitkomsten main → branch:

- **Voorwaardelijke initialisatie naast een rekenende regel die leeg oplevert** (clingo-suite, `TmsWritersWitnessTest`
  derde document): ref EF-016 op het passbudget, clingo-getuige, Drools EF-016 ("doel volgens b schrijft leeg en
  standaarddoel schrijft 3") → de initialisatie vult: 3 (25,00; op een cel 0.00); met invoer staat de invoer (9; 7)
  in ref en Drools, clingo houdt zijn hek.
- **Schrijfactie bij het maken van een object** met een rekenende regel op dat lid die leeg oplevert
  (`TmsCreationInputWitnessTest`): ref en Drools terug 0 (gewist) → 7 (de waarde van het maken staat); clingo gaf al 7.
- **Verdeling** met een rekenende regel op haar doelattribuut die leeg oplevert (proef
  `werk/.../probes/allocation-beside-rule`, de golden `jutterswet-berging` plus "Het bergingsaandeel van een Berger
  moet gesteld worden op zijn vaste aandeel", verifier PASS; log `21`): ref `EF-016 … rule "vast bergingsaandeel"
  writes leeg and allocation … writes 300` → PASS met 300, 200, 300 en rest 400; clingo PASS op main en branch; beide
  Drools-takken weigeren op main en branch (`DROOLS-142`). Geen golden.
- **Defaults-groep**: ongewijzigd (een lege default valt al door; een regel naast een groep op hetzelfde niveau is
  WF-050). **Kenmerkdoel**: geen lege waarde, ongewijzigd.

### Voorgestelde tekst

**RUNTIME-SEMANTICS** (in `2bb17f347`, als huidige stand): ch. 1.2 Assignment; ch. 3.11 "Leeg in a write" als één
regel voor het scalaire en het tijdlijndoel, met het uitgewerkte voorbeeld `WV-3.11-leeg-beslist-niets`, `-versie`
en `-naast-waarde` (ref-suite letterlijk); ch. 3.12 rij 10 weg; ch. 5.7 "Divergence by two writers" en "The writers
that decide at the fixpoint" (het vijfde geval van het voorbeeld: **7**); ch. 5.8c de celzin; ch. 7.7 (de lezing naast
een voorwaardelijke initialisatie, zonder `desugared_targets`); ch. 7.8 de standing norm voor elke rekenende regel,
met "decided the norm on 2026-08-20 for a version-free rule that is its target's only writer and widened it on
2026-11-04 to every computing rule" en de ASP-zin over `standing_targets`.

**CONSTRUCTS rij 8**, na "… `jutterswet-twee-schrijvers-aan-fixpunt`, each in both rule orders.":

> **A leeg write decides nothing (2026-11-04, card `empty-write-decides-nothing`, RUNTIME-SEMANTICS ch. 1.2, 3.11,
> 5.7, 7.8):** a writer that fires with a leeg value decides nothing, on a scalar target as on a timeline target and
> for every writer — the target's only one, one beside a second writer, one with a validity window, one beside a
> conditional initialisation, a creation-time write or an allocation; two writers with different values stay the
> fault of ch. 5.7. The ch. 7.8 standing norm, decided 2026-08-20 for a version-free single writer, holds for every
> computing rule since. In ref, clingo and both Drools branches (fixtures `ref/test/fixtures/empty-write`; the golden
> `jutterswet-twee-schrijvers-aan-fixpunt` and its reverse, case6: 7). Tracked delta against v0, which writes the
> computed leeg over the input and over another writer's value.

en in dezelfde rij "7.8 standing norm — a bare scalar computing rule decides like a default, a computed leeg never
displaces an input value (v0's erase-on-leeg a tracked delta; anchor `jutterswet-waardestelling`, both executors,
shared domain predicate)" → "7.8 standing norm — every scalar computing rule decides like a default, a computed leeg
never displaces an input value (v0's erase-on-leeg a tracked delta; anchor `jutterswet-waardestelling`)".

**CONSTRUCTS rij 18**: "**standing-target exception (2026-08-20):** a computed leeg does not displace an input value
on a bare single-writer computing target (RS ch. 7.8; row 8)" → "**a leeg write decides nothing (2026-08-20 for a
bare single-writer computing target, 2026-11-04 for every writer):** a writer that fires with a leeg value writes
nothing, so it displaces no input value and no other writer's value (RS ch. 1.2, 3.11, 7.8; row 8)".

**ASP-TRANSLATION** (niet in mijn mandaat, niet aangeraakt), huidige stand:
- r. 202–215 (S2): "a bare single-writer version-free scalar computing rule (the standing norm)" → "a scalar target
  that only computing rules write, where one of them applies (`Ir_v0.Desugar.standing_targets`, the standing norm)";
  de zinnen "Case input on a *non-standing* computed scalar target is refused loudly (ref overwrites; …), except on a
  target of two or more co-writers (S3): … stands where none of them fires (RS ch. 5.7, …)" → "Case input on a computed
  scalar target that an initialisation, an allocation or a creation-time write writes as well is refused loudly."
- r. 281–286 (S3): "A **standing** scalar target (S10) additionally emits an effectiveness atom …" → "Every scalar
  computing rule derives its value under its index in the document as well, `wval(S,a,⟨term⟩,i)`, with the full rule
  body, and `wdecides(S,a) :- wval(S,a,_,i).`; on a standing target the defeasible input rule `val(S,a,V) :-
  inputval(S,a,V), is_a(S,ot), not wdecides(S,a).` lets a writer win exactly where it decides; an empty result
  derives no `wval` and decides nothing, so the input stands (RS ch. 3.11, 7.8). Two writers that share a concrete
  type and derive different `wval` values are the named EF-016 witness."
- r. 303–314 (initialisatie): "and `comp_fires(S,a)` with its base and condition; … Where `comp_fires` holds without
  `comp_decides`, v0 erases … unless the rule is a standing one (S10), which writes nothing on leeg." → "; the head
  needs `not comp_decides(S,a)`, so the computed value wins, and a rule that evaluates leeg decides nothing (RS
  ch. 3.11), so the initialisation writes."
- r. 343–353 (venster, niet vertaald): "including the ch. 6d fence when the case supplies its (never-standing) computed
  target" → "its target in the standing domain like a version-free rule's"; "case input on a non-standing computed
  target that is no co-writer's; a `standing` target outside its own domain;" → "case input on a computed target beside
  an initialisation, an allocation or a creation-time write;".
- r. 1700–1702 (S10): "The standing norm extends this to bare single-writer version-free scalar computing rules (S3)." →
  "The standing norm extends this to every scalar computing rule (S3)."
- r. 2945–2947 (S19): "case input on non-standing SCALAR and TEMPORAL computed targets (the ch. 6d superset-model
  fence), a scalar target of two or more co-writers apart (S2)" → "case input on a SCALAR computed target beside an
  initialisation, an allocation or a creation-time write". Het naschrift r. 4091 e.v. is gedateerd; daar volstaat
  "(since 2026-11-04 every computing rule, RS ch. 3.11)".

**CLAUDE.md**: geen wijziging nodig (de gate-teksten noemen het staande domein of `StandingTargets` niet).

**scripts/check-drools.sh** (beschermd, niet aangeraakt): het commentaar bij `EXCLUDE_KNOWN` (d') noemt nog
`transpiler/kern/StandingTargets` en "with a SECOND computing rule on the target the v0 erase is unretired and a leeg
source still wipes the input (ref agrees) — pinned by StandingScalarNormWitnessTest"; voorstel: die zin vervangen door
"Since 2026-11-04 a computed leeg decides nothing on every scalar target, beside a second writer and with a validity
window too (StandingScalarNormWitnessTest), and the Drools classification is gone."

**spec/verifier** (beschermd, in `0b64b4991`): `standing_targets` verbreed, `desugared_targets` weg.

### Voorlopige besluiten

- VB-1: het besluit geldt ook voor een rekenende regel naast een voorwaardelijke initialisatie, een schrijfactie bij
  het maken en een verdeling; die vormen zelf lezen zoals nu. Reden: de kaart zegt "elke schrijver", en een uitzondering
  zou een tweede pad zijn. Gemeten hierboven.
- VB-2: `Desugar.standing_targets` blijft, met als enige taak de routering van invoer in clingo, verbreed tot elk
  scalair doel dat alleen rekenende regels schrijven; `desugared_targets` weg.
- VB-3: het clingo-hek op invoer naast een initialisatie, verdeling of schrijfactie bij het maken blijft (geen
  corpuscase; "Ongewijzigd"), met een bericht dat de reden van nu noemt.
- VB-4: clingo leidt `wval/4` en `wdecides/2` af voor elke rekenende regel, ook een enige: één pad.
- VB-5: in Drools gaan `StandingTargets`, de tweede initialisatiewacht en de verschilregel naast een initialisatie
  weg, met hun hulpfuncties: geen lezer meer.
- VB-6: de golden-case `case6-lege-boete-beslist` heet `case6-lege-boete-beslist-niet` (de oude naam beschreef de oude
  regel); testnamen die de oude regel beschreven, zijn hernoemd.
- VB-7: RS ch. 3.12 rij 10 is weg, niet teruggebracht: er blijft niets open.

### Vragen aan de hoofdsessie

1. VB-1: de drie raakvlakken (voorwaardelijke initialisatie: fout → de waarde van de initialisatie; gemaakt object:
   0 → 7; verdeling: fout → de aandelen) — zo bedoeld?
2. Het clingo-hek naast een initialisatie: laten staan (VB-3) of opheffen? Met `inputfilled/2` en `wdecides/2` is het
   nu te coderen; ref en Drools beantwoorden die cases (TmsInputOnComputedTarget, TmsWriters derde document).
3. Is de invoer die naast elke rekenende regel blijft staan, en de lege waarde naast een tweede schrijver, de gevolgde
   v1-delta tegen v0 zoals ik hem in rij 8 voorstel?
4. De gate-run deelde de machine met de mutatieruns (dune en Drools-suite in een losse worktree); gate 6 duurde 477 s
   (vorige kaart 269 s). Of de extra `wval/4`-regels voor elke enige schrijver daar iets aan bijdragen, heb ik niet
   apart gemeten.

### make gates

Eén run in de worktree op `2bb17f347` (`out/gates/20261104-123710/`); de commit daarna raakt alleen deze kaart.

```
start 2026-11-04 12:37:10 2bb17f347 card/empty-write-decides-nothing
ocaml rc=0 0s ocaml-build: rebuilt (3 source path(s) newer than the stamp, e.g. backends/clingo/README.md; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 3s drools-jar: rebuilt (20 source path(s) newer than the jar, e.g. backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java; 3s)
01-self-contained rc=0 2s
02-ir-schema rc=0 214s
04-case-schema rc=0 11s
03-verifier rc=0 68s
05-oracle rc=0 32s
06-clingo rc=0 477s
07-noop rc=0 19s
08-red rc=0 294s
09-catala rc=0 6s
10-regelspraak rc=0 451s
11-toka-parity rc=0 400s
12-drools rc=0 584s
13-reftype rc=0 544s
14-flogic rc=0 3s
15-graph rc=0 76s
16-scenario rc=0 57s
17-readings rc=0 473s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 12:46:57 587s red=0
```
