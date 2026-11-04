# decisions-per-period: wie op welke dagen besliste, per object en tijdlijndoel

*Opgesteld door de hoofdsessie op 2026-11-04, uit het voorstel van de toolchain-kaart `timeline-attribute-input` (toolchain `5184be1e1`).
De dienst geeft op `?explanation=true` bij een defaults-stapel die per dag beslist nu een 400 met naam, omdat `decisionsOf` zo'n build
weigert; deze kaart geeft de API die de uitleg per periode nodig heeft.*

## Goal

 Een consument kan per object en tijdlijndoel lezen wie op welke dagen besliste, zonder de verzoening per dag
zelf te doen. Voor een stapel per dag (RUNTIME-SEMANTICS ch. 7.11) geeft `DroolsBackend.decisionsOf` (of een
zustermethode) per object en stapel de maximale perioden met één uitkomst: de beslissende default (pointer zoals
`DefaultsDecision.Decider`, met de waarde die hij die dagen schreef), een conflict (sleutel en beslissers) of geen
beslisser (de invoer staat, of het doel blijft leeg), over alle tijd met open grenzen; voor een tijdlijndoel van gewone
schrijvers (ch. 3.11) per object en doel de perioden per schrijver (de vorm) en die van de invoer. Het leest de
controlefeiten en de `Outcome`-dagen (`PeriodDefaults.Days`) die de lowering al neerlegt en rekent niets opnieuw.

## Acceptance

- (a) Een publiek record per periode met javadoc in de termen van ch. 7.11 en 3.11; `decisionsOf` weigert zo'n build
  niet meer, of de zustermethode is de weg en `decisionsOf` noemt haar in zijn weigering.
- (b) Uitgewerkt voorbeeld, letterlijk als test: `jutterswet-opgave-per-periode` case4 → [2025-01-01, 2025-03-01)
  default `/defaults/0` (opgave, 10), [2025-03-01, 2025-06-01) `/defaults/1` (berekend, 20), [2025-06-01, 2026-01-01)
  geen beslisser (invoer 99); `anchor/jutterswet-borgdeel` case6 → [2024-01-01, 2024-03-01) de regel 'borgdeel van een
  jutter', [2024-03-01, 2024-04-01) de invoer.
- (c) Een witness over elke golden, rode case en fixture met een stapel per dag (`ref/test/fixtures/defaults-per-day`,
  `-cases`) of een tijdlijndoel met invoer, op beide takken: de beslissers per periode zijn die van ref
  (`run_defaults_timeline`, `decide_days`), en de vereniging van hun waarden met de invoer is de tijdlijn van het
  fixpunt.
- (d) Rood eerst; per bewaking een mutatie die een test rood maakt; `make gates` groen.

Daarna, in de toolchain: `Verbaliser` noemt per periode welke default of regel besliste en op welke dagen de invoer
bleef staan, `Verbaliser.unexplained` en `EXPLANATION_REFUSED` vervallen.

May touch: `backends/drools/**` (de publieke API en haar witness), `spec/RUNTIME-SEMANTICS.md` (alleen als ch. 5.12 of 7.11 een zin over
de beslissers per periode vraagt; voorstel), kaartverplaatsing.

## Omvang

Klein tot middel.

## Result

*Bouwer, 2026-11-04. Branch `card/decisions-per-period`, niet gemerged, niet gepusht. Logs onder
`werk/decisions-per-period/logs/`, het mutatiescript onder `werk/decisions-per-period/scripts/mutations.py`.*

### Commits

| hash | inhoud |
|---|---|
| `8e3075679` | kaart naar `doing/` |
| `3ee250e4e` | drools: `PeriodDecisions`, `DroolsBackend.periodDecisionsOf` / `TmsTranspiler.periodDecisionsOf`; `decisionsOf` laat een stapel per dag weg in plaats van de build te weigeren; `PeriodDecisionsWitnessTest`; de omgedraaide test in `TmsDefaultsPerDayWitnessTest`; de `perDay`-uitzonderingen uit drie witnesses van `decisionsOf`; README |
| `460b144fa` | witness: het document `conditions-per-period/twee-schrijvers-gelijk` (twee regels die één dag gelijk beslissen), met vloer |
| (deze) | Result |

### De API

```java
// dev.uchar.regelspraak.cases.DroolsBackend
public static List<PeriodDecisions> periodDecisionsOf(Build build, KieSession session)

// dev.uchar.regelspraak.transpiler.tms
public record PeriodDecisions(Object object, String member, List<List<String>> rootChains, List<Span> spans) {
    public record Span(LocalDate from, LocalDate to, DefaultsDecision.Decider decider,
                       DefaultsDecision.Conflict conflict, List<DefaultsDecision.Decider> rules, BigDecimal input) {}
}
```

- **Records.** Eén per object van de sessie waarvan het type het lid heeft (invoerbeans en gemaakte objecten) en per
  attribuut op een tijdlijn dat een stapel of rekenende regels schrijven (`timelineWriters`). Waar het concrete type
  een stapel per dag op het lid heeft (ch. 7.10), beslist die; `rootChains` zijn dan die van `DefaultsDecision`. Anders
  beslissen de regels; `rootChains` is leeg.
- **Per periode** (`spans`): de maximale perioden met één uitkomst, samen alle tijd, open grenzen als
  `Period.OPEN_FROM`/`OPEN_TO`. Een periode noemt: de `decider` (de default die die dagen besliste, gevolgd tot zijn eigen
  inhoud zoals `DefaultsDecision.Decider`, met de waarde die hij schreef), of het `conflict` (sleutel, de default waaronder
  het staat, de leden met hun waarden; de invoer staat dan), of de `rules` (elke regelversie die die dagen besliste met
  haar waarde, in documentvolgorde; twee waar ze gelijk beslissen, en waar ze verschillen staat EF-016), of geen
  schrijver; en de `input` die op dagen zonder schrijver staat, null waar de invoer leeg is of de case de emmerscalair
  geeft.
- **Hoe.** Het leest de `Outcome`-dagen (`PeriodDefaults.Days`) van de stapel en volgt ze per dag van de wortel naar
  beneden zoals `decisionsOf` de uitkomsten van een stapel met één waarde eenmaal volgt (eerste conflict gaat door,
  verschillende leden conflicteren, gelijke leden beslissen als de eerste, bij uitzonderingsgroepen de leden waar zij de
  dag beslisten en anders de terugval); elke beslisde dag moet gelijk zijn aan de `Value` die de stapel schreef, tenzij
  een fout van de stapel op het object staat. Voor regels de `Value`s per schrijver; de invoer zoals de stand van het doel
  haar leest (`union`). Het rekent niets opnieuw en weigert de sessies die `fixpointOf` weigert.
- **`decisionsOf`** weigert een build met een stapel per dag niet meer: het geeft een record per object en stapel op een
  doel met één waarde, en een stapel per dag heeft er geen (javadoc van `decisionsOf`, `DefaultsDecision`,
  `DefaultsGroup`).

### Het uitgewerkte voorbeeld (`PeriodDecisionsWitnessTest.the_worked_example`, beide takken)

`jutterswet-opgave-per-periode` case4, J4, strandloon (wortelketen `[/rules/prioritizedDefaults/0/defaults/0,
…/0/defaults/1]`):

```
[- 2025-01-01) leeg
[2025-01-01 2025-03-01) default /rules/prioritizedDefaults/0/defaults/0 10
[2025-03-01 2025-06-01) default /rules/prioritizedDefaults/0/defaults/1 20
[2025-06-01 2026-01-01) invoer 99
[2026-01-01 -) leeg
```

`anchor/jutterswet-borgdeel` case6, J1, borgdeel (geen stapel; `/rules/rules/0` heet 'borgdeel van een jutter'):

```
[- 2024-01-01) leeg
[2024-01-01 2024-03-01) regel /rules/rules/0/versions/0 12
[2024-03-01 2024-04-01) invoer 99
[2024-04-01 -) leeg
```

De regel schrijft twee segmenten van 12 (januari, februari); de periode is één, want zelfde schrijver en waarde. De open
perioden zonder schrijver en invoer horen bij de lijst (alle tijd); de kaart noemde ze niet.

Ter vergelijking, wat de witness op de fixtures leest (exec-model), in de woorden van ch. 7.11: `defaults-per-day/ongeordend`
geeft 10 (`…/0/defaults/0`) op [2025-01-01, 2025-03-01), op [2025-03-01, 2025-05-01) het conflict `Jutter.toelage` (10 en 20)
met de invoer 5, 20 (`…/1/defaults/0`) tot 2025-07-01, de invoer 5 tot 2026-01-01; `afwijking` geeft in april het conflict
`Jutter.strandloon/0` onder `/rules/prioritizedDefaults/0/defaults/0` (40 en 50), leeg, en in februari en maart het lid
`…/0/defaults/0/exceptions/members/0/defaults/0` (40).

### De witness (`PeriodDecisionsWitnessTest.every_record_is_the_reading_per_day_of_the_reference_interpreter`)

Op beide takken van de build met lezingen: elke case van elke golden die de exec-model-tak speelt, elke case van
`corpus/red`, de documenten en cases van `ref/test/fixtures/defaults-per-day` en `-cases`, die van `writers-per-day`, en
`conditions-per-period/twee-schrijvers-gelijk`. Zonder fout aan het fixpunt:

- één record per object en tijdlijndoel van een schrijver, en geen record daarbuiten; de wortelketens van een stapel zijn die
  van haar samenstelling (`TmsTranspiler.stacks`);
- de perioden zijn die van de lezing per dag: per dag de beslissing van ch. 7.1, 7.2, 7.3 en 7.9 met elke rechtvaardiging en
  elk gevolg op die dag gelezen, zoals `run_defaults_timeline` en `decide_days` dat doen, hier uit de lezingen (ch. 5.12):
  een default beslist een dag waarop de wortel van zijn gevolg in zijn lezing (de gevulde segmenten binnen de perioden van
  zijn rechtvaardiging) een waarde leest, de initialisatiedefault alleen waar de invoer leeg is; leden en
  uitzonderingsgroepen verzoenen per dag. Voor regels: per dag de regels waarvan de lezing daar een waarde schrijft. Een
  eigen implementatie in de test, dag voor dag, los van `PeriodDefaults` en `periodDecisionsOf`;
- de vereniging van de waarden van de perioden met de invoer is per dag de tijdlijn van `fixpointOf`, of de invoer waar die
  niets afleidt.

Een rode case heeft geen antwoord; waar zij een fixpunt haalt, worden haar records toch gelezen, en beide takken geven
dezelfde. Gemeten (beide takken gelijk): 398 builds, 272 records (28 van een stapel per dag), perioden met een beslisser 57,
een conflict 2, regels 454 (1 met twee of meer), de invoer 17; 43 rode cases gelezen met een fout aan het fixpunt. Die
getallen zijn de vloeren in de klasse.

### Rood eerst (letterlijk)

- De nieuwe tests tegen main `8e3075679` (`logs/09-red-compile.log`): `COMPILATION ERROR`, 38 regels `cannot find symbol`,
  o.a. `symbol: class PeriodDecisions, location: package dev.uchar.regelspraak.transpiler.tms` en `symbol: method
  periodDecisionsOf(dev.uchar.regelspraak.cases.DroolsBackend.Build,org.kie.api.runtime.KieSession)`.
- Het gedrag tegen main (`logs/10-red-behaviour.log`, de omgedraaide test zonder zijn `periodDecisionsOf`-deel en de drie
  witnesses zonder `perDay`-uitzondering): 8 tests, 4 errors, elk `IllegalState decisionsOf leest per object en stapel één
  beslisser, en de stapel defaults A:Jutter.strandloon op een tijdlijndoel beslist per dag (RUNTIME-SEMANTICS ch. 7.11)`
  (`TmsDefaultsPerDayWitnessTest.a_stack_per_day_has_its_decisions_per_period`, `DefaultsDecisionCorpusWitnessTest`,
  `StackCompositionCorpusWitnessTest`, `ReadingsCorpusWitnessTest`).
- Eigen rood onderweg (`logs/03-tests.log`): de witness las bij `jutterswet-verzuimrente-30-360-afgerond` geen waarde van
  regel 2, omdat de wortel van haar waarde een `Let` is die de lezing `unreached` noteert terwijl de waarde ernaast de
  tijdlijn draagt; de witness leest bij een `Let` die haar eigen binder leest de waarde (`throughLets`). Zie vraag 3.

### Groen

Drools-suite (`logs/08-suite.log`, op `3ee250e4e`): 1849 tests, 0 failures, 0 errors, 0 skipped. De witness met het
document erbij (`logs/13-tests.log`): groen. `make gates`: hieronder.

### Mutaties (niet gecommit; `git worktree add --detach`, opgeruimd; logs `11-mut-*`, samenvatting `11-mutations-summary.log`)

| mutatie | rood |
|---|---|
| M1 geen samenvoegen van gelijke perioden | `the_worked_example`: borgdeel `[2024-01-01 2024-02-01) … 12, [2024-02-01 2024-03-01) … 12`; witness: `golden/anchor/jutterswet-borgdeel/case1.json J1 borgdeel` |
| M2 bij uitzonderingsgroepen de default zelf noemen in plaats van het lid | witness: `defaults-per-day/afwijking J1 strandloon` |
| M3 de beslissers van een conflict omgekeerd | witness: `defaults-per-day/ongeordend J1 toelage` |
| M4 de stapel laat de invoer weg | `the_worked_example` (J4 zonder `invoer 99`); witness: `golden/jutterswet-opgave-per-periode/case4-… J4 strandloon` en de vereniging met het fixpunt |
| M5 de regels in omgekeerde documentvolgorde | eerst groen (geen case met twee regels op één dag) → `460b144fa`; daarna rood: `conditions-per-period/twee-schrijvers-gelijk J1 wachtdagen` (`regel /rules/rules/1/… 30, regel /rules/rules/0/… 30`) |
| M6 `decisionsOf` weigert weer een stapel per dag | 4 errors `IllegalState decisionsOf weigert`: `TmsDefaultsPerDayWitnessTest`, `DefaultsDecisionCorpusWitnessTest`, `StackCompositionCorpusWitnessTest`, `ReadingsCorpusWitnessTest` |
| M7 geen record waar geen regel schreef | witness: `golden/anchor/jutterswet-borgdeel/case1.json J1 jaardeel: geen record`, … |
| M8 wortelketens omgekeerd | witness: `defaults-per-day/ongeordend J1 toelage: wortelketens [[…/1/defaults/0], [/…/0/defaults/0]], de samenstelling …` |
| F1 (test) zonder de fixtures van `writers-per-day` | vloer: `EXEC_MODEL: records 265, onder de vloer 271` |

Niet met een mutatie bewezen: de vergelijking van de twee takken (beide renderen één analyse en lezen dezelfde feiten;
een mutatie van één tak raakt de code van `periodDecisionsOf` niet) en de kruiscontrole tegen de `Value` van de stapel
(een bewaking die alleen een inconsistente sessie raakt).

### Tests die de kaart verandert (oud → nieuw)

- `TmsDefaultsPerDayWitnessTest.decisions_of_a_stack_per_day_are_refused` (`assertThrows(IllegalStateException)`, "op een
  tijdlijndoel beslist per dag") → `a_stack_per_day_has_its_decisions_per_period`: `decisionsOf` geeft `[]` op case2 van
  de golden, `periodDecisionsOf` geeft J2 de stapels `stormtoelage` en `strandloon`.
- `DefaultsDecisionCorpusWitnessTest`: oud slaat een build met een stapel per dag over (`perDay(build)`, verwijderd); nieuw
  toetst ook zo'n golden en slaat alleen het doel op een tijdlijn over, zodat een record van een stapel per dag faalt als
  record op geen stapel. `StackCompositionCorpusWitnessTest.againstDecisions` en `ReadingsCorpusWitnessTest`: de
  `perDay`-uitzondering weg, `decisionsOf` geeft daar geen record. Vloeren ongewijzigd en gehaald.

### Voorlopige besluiten

- VB1: een zustermethode, en `decisionsOf` laat een stapel per dag weg in plaats van te weigeren: met de weigering kreeg een
  build met een stapel per dag én een stapel op een doel met één waarde voor de tweede geen uitleg. Beide methoden samen
  dekken elke stapel.
- VB2: één record voor beide soorten doel (stapel en regels), met perioden over alle tijd; open grenzen als
  `Period.OPEN_FROM`/`OPEN_TO`, zoals `PeriodDefaults.Days` en `PeriodArithmetic.Exact` ze dragen (op een bean is het null).
- VB3: een periode is maximaal over gelijke schrijvers met gelijke waarden (op waarde vergeleken) en gelijke invoer; ook op
  conflictdagen draagt zij de invoer die staat.
- VB4: een regel heet naar haar versiepointer (`/rules/rules/k/versions/v`), zoals `DefaultsDecision` een opgenomen regel
  noemt; de toolchain legt die al op haar document (`positions().versionOf`).
- VB5: alleen attributen. Een tijdsafhankelijk kenmerk dat een regel schrijft heeft één schrijver en geen invoer ernaast
  (ch. 3.11), en `Fixpoint` geeft zijn perioden.
- VB6: de witness leidt de beslissers van ref af uit de lezingen (ch. 5.12: "the days of the turn follow from the readings
  … by the rule of ch. 7.11"): ref geeft geen beslissers per dag en `ref/` valt buiten het mandaat. Voor goldens houdt
  gate 17 de lezingen gelijk aan ref's trace; voor de fixtures pint `TmsDefaultsPerDayWitnessTest` een deel ervan.
- VB7: geen tekst in RUNTIME-SEMANTICS: de API is een leesweg van de backend; ch. 5.12 en 7.11 vragen geen zin.

### Voorgestelde tekst (huidige stand)

**CONSTRUCTS rij 8**, achter de zin van `defaults-per-period-on-timeline` (step B):

> 2026-11-04, card `decisions-per-period`: the Drools backend names who decided a timeline target, per object and per
> period (`DroolsBackend.periodDecisionsOf`, `PeriodDecisions`): for a stack per day the default that decided with the
> value it wrote, the conflict of its members, or no decider and the input that stands; for the computing rules of a
> timeline target the rules that decided and the days of the input. It reads the `Outcome` days and the `Value`s the
> lowering leaves and evaluates nothing again; `decisionsOf` has no record of a stack per day. No change to the semantics.

**CLAUDE.md gate 12**, de zin "`decisionsOf` refuses a build with such a stack." wordt:

> `decisionsOf` has no record of such a stack, and `periodDecisionsOf` gives per object and timeline target who decided
> per period, a stack's default, its conflict, the rules or the input (`PeriodDecisions`; card `decisions-per-period`,
> 2026-11-04, witness `PeriodDecisionsWitnessTest`).

### Wat de distributie daarna doet

- Pin naar de landing; niets breekt op de pin zelf: `Verbaliser.unexplained` leest `DefaultsGroup.perDay` (ongewijzigd) en
  `ScenarioRunner` roept `decisionsOf` daar niet aan.
- `ScenarioRunner` (de handoff) roept `decisionsOf` altijd aan en `periodDecisionsOf` ernaast; `Handed` en
  `CaseSession.Outcome` dragen `List<PeriodDecisions>`.
- `Verbaliser.DefaultsExplanation`: een groep op een tijdlijndoel vindt zijn record in `periodDecisions` (`rootChains` niet
  leeg) en krijgt per periode een zin of zinsdeel: welke default besliste met welke waarde, het conflict, en de dagen
  waarop de invoer bleef staan; de bestaande `decided`/`conflicted`/`undecided` passen per periode, met `rootChains` uit
  het record.
- `ComputationExplanation`: `inStacks` en `stackTargets` ook uit de `rootChains` van `periodDecisions`; een tijdlijndoel van
  regels noemt per periode de regel(s) en de invoer in plaats van "een tijdlijn".
- `Verbaliser.unexplained`, `DecisionCore.withoutExplanation` voor stapels per dag, `EXPLANATION_REFUSED` en het zesde veld
  van `scripts/service_replay.py` vervallen.

### Vragen aan de hoofdsessie

1. VB1: is weglaten (zustermethode ernaast) de gewenste vorm, of moet `decisionsOf` blijven weigeren en de zustermethode in
   zijn zin noemen? Met weigeren heeft een gemengde build geen uitleg van zijn stapel met één waarde.
2. VB5: moet `periodDecisionsOf` ook een tijdsafhankelijk kenmerk van een regel geven, zodat de uitleg één weg heeft?
3. De lezing van `/rules/rules/2/versions/0` in `jutterswet-verzuimrente-30-360-afgerond` (`rentedagen in een gedeeltelijke
   maand`) noteert de wortel `1 Let unreached` en `7 BinderRef unreached`, terwijl `2 BuiltinCall` (de waarde van de `Let`)
   de geschreven tijdlijn draagt. Gate 17 houdt ref en Drools daarin gelijk, dus het is de lezing zoals beide haar geven;
   maar ch. 5.12 noemt een `Let` onbereikt "whose binder no read forces", en hier leest het lichaam de binder. Een
   kaart voor de lezing van een `Let` in een tijdlijnregel?

### make gates

Eén run aan het eind in de worktree op `460b144fa` (`out/gates/20261104-205422/`, `logs/gates-1.log`); de commit daarna
raakt alleen deze kaart.

```
start 2026-11-04 20:54:22 460b144fa card/decisions-per-period
ocaml rc=0 5s ocaml-build: rebuilt (no stamp _build/.gates-built; 5s)
frontend rc=0 5s frontend: rebuilt (5s)
jar rc=0 7s drools-jar: rebuilt (7 source path(s) newer than the jar, e.g. backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms; 7s)
01-self-contained rc=0 2s
02-ir-schema rc=0 135s
04-case-schema rc=0 8s
03-verifier rc=0 43s
05-oracle rc=0 25s
06-clingo rc=0 295s
07-noop rc=0 14s
08-red rc=0 179s
09-catala rc=0 6s
10-regelspraak rc=0 264s
11-toka-parity rc=0 233s
12-drools rc=0 367s
13-reftype rc=0 353s
14-flogic rc=0 2s
15-graph rc=0 51s
16-scenario rc=0 44s
17-readings rc=0 277s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 21:00:36 374s red=0
```

Gate 12: JUnit 1849 tests (vloer 1573), `played 334`, DRL-tekst `played 334`; geen pin of vloer van een gate geraakt.
