# timeline-initialisation-allocation-creation: initialisatie, verdeling en waarde bij het maken op een tijdlijndoel

*Opgesteld door de hoofdsessie op 2026-11-04, uit vraag 3 van `writers-as-one-defaults-stack` (RUNTIME-SEMANTICS ch. 3.12 rij 10).*

## Aanleiding

Een initialisatie buiten een defaults-groep, een verdeling en de waarde die een creatie bij het maken zet, weigeren alle uitvoerders op
een tijdlijndoel bij naam (ch. 3.12 rij 10); ref schreef daar eerder stil een scalair. Op een scalair doel staan ze in de stapel van
ch. 7.8 (schrijvers, de invoer of de waarde van het maken, de initialisatie). De specificatie zwijgt over een initialisatie op een
tijdsafhankelijk attribuut; ALEF vult bij een initialisatie per moment waar de tijdlijn leeg is (`MLazyTimedProperty.initResult` met
`orElse`, gelezen, niet gemeten).

## Gemeten en besloten (2026-11-05)

ALEF gemeten in alef-bridge (`aa032b5`, kaart `backlog/done/20261105-timeline-writers-in-alef.md`, lezingen in
`alef-models/TimelineWriters/expected.tsv`):

- **Initialisatie** (technisch besluit hoofdsessie, ALEF volgen): ALEF lazy (gegenereerd en interpreter) en gegenereerd eager vullen elke
  dag waarop geen regel beslist en de invoer geen waarde heeft, over alle tijd met open grenzen (`5 [.., ..)`), ook op dagen waar een
  regel leeg geeft. Alleen de eager-interpreter geeft leeg op de dagen van de regel als de initialisatie in een regelgroep `imperatief`
  vóór de regel staat: een volgordeartefact (een `ExceptionBox` na `ITimed.merge`), zoals in de meting `WritersInput`, waar de eigenaar
  de lezing per dag koos. uchar leest de initialisatie per dag als de laagste laag van de stapel van ch. 7.8, over alle tijd.
- **Verdeling** (technisch besluit hoofdsessie, ALEF volgen): ALEF's typesystem weigert een verdeling met een tijdsafhankelijk attribuut
  statisch ("Verdeling kan niet gebruikt worden in combinatie met tijdsafhankelijke attributen."), ook als alleen het aandeel per maand
  is. uchar weigert haar statisch: een WF-regel in de verifier met negatieven, en een RGS-code in de frontend.
- **Waarde bij het maken** (besluit van de eigenaar, 2026-11-05): ALEF's typesystem laat de vorm toe, maar ALEF voert haar niet uit
  (javac-fout bij een scalaire waarde, generatiefout bij een waarde per maand). uchar geeft een eigen lezing per dag: de waarde bij het
  maken is per dag de invoer van het gemaakte object (rang 2 van de stapel); een scalaire waarde geldt op elke dag, een waarde per maand
  per periode; een regel die op een dag beslist, gaat ervoor, de initialisatie vult de rest.

Ook mee te nemen (waarneming uit `absence-markers-residue`, stap B): in clingo geeft een andere producent van een tijdlijn van een sum
type dan de constructor van een literal, de invoer en een kopie een vertaalfout zonder registercode; een initialisatie of een waarde bij
het maken op een tijdlijndoel met markers is zo'n producent. Dragen, of weigeren met een code van het soort `refusal`.

## Goal

Bouwen wat hierboven besloten is: de initialisatie en de waarde bij het maken op een tijdlijndoel per dag in de stapel van ch. 7.8, in
ref, clingo en beide Drools-branches, met goldens naar de gevallen van de meting (i1–i4, c1–c4); de verdeling op een tijdsafhankelijk
attribuut statisch geweigerd (WF-regel, RGS-code). RUNTIME-SEMANTICS ch. 3.12 rij 10 en ch. 7.8 als voorstel in de huidige stand.

## Acceptance

- (a) De meting in alef-bridge: gedaan (`aa032b5`).
- (b) Per vorm: gebouwd per dag in ref, clingo en beide Drools-branches met een golden, of een WF-regel die haar weigert, met reden.
- (c) Rood eerst; `make gates` groen.

May touch: `spec/` (voorstel; ook `spec/verifier/`), `ref/`, `backends/`, `frontends/regelspraak/`, `corpus/` (ook `corpus/negative/`), `scripts/`
(alleen lijsten en pins; voorstel), kaartverplaatsing.

## Omvang

Middel.

## Result

*Bouwer stap A, 2026-11-05. De kaart blijft in `doing/`; niet gemerged, niet gepusht. Logs onder
`werk/timeline-initialisation-allocation-creation/logs/` (buiten de repo), scripts onder `…/scripts/` (`probe.sh`,
`write_cases.py`, `write_negatives.py`, `mutations.py`, `fe-test.sh`), proeven onder `…/probes/`.*

### Commits

| hash | inhoud |
|---|---|
| `50491c2f6` | kaart naar `doing/` |
| `a114e68dc` | verifier WF-128 (`spec/verifier/lib/checks.ml`), IR-SPEC ch. 3.2 en de WF-tabel, zes negatieven `wf128-*` (beschermd, voorstel) |
| `4cc235f2f` | ref: de stapel van een tijdlijndoel per dag (`World.stack`, `run_timeline_initialisation`, de waarde bij het maken, de lezing), WF-128 bij een verdeling; fixtures `writers-stack/timeline-*`; ref-suite; README |
| `6e97917e7` | clingo: de desugaring op een tijdlijndoel door de defaults per dag, de initialisatie buiten de desugaring als laagste laag (`itl/5`, `initcut/5`, `initpiece/6`, `initdecided/4`), de waarde bij het maken als invoertijdlijn, WF-128 bij een verdeling, CLINGO-036; clingo-suite; README |
| `4de98bcd4` | frontend RGS-378 met catalogi en `TimelineAllocationTest` |
| `9c12cbaa2` | drie goldens; `scripts/check-drools.sh` `EXPECTED_CHANNEL` (beschermd, voorstel) |
| `1f9d2118f` | clingo: het totaal van een geïnitialiseerde tijdlijn (`ctx.wire_doc`); fixtures `timeline-initialisation-total`, `-window` |
| `ae97f97ae` | RUNTIME-SEMANTICS ch. 3.11, 3.12 rij 10, 4.5, 5.4, 5.5, 5.12, 7.8 en ASP-TRANSLATION (beschermd, voorstel) |
| `db1bdbbcb` | fixture-case `case-leeg`, de lezing van de initialisatie in de ref-suite, `ref_matches` vangt een fout per frame; RS ch. 7.8 |
| `f3716858d` | clingo: CLINGO-036 vóór de toets op periode-rekenkunde (`sum_timeline_producer`); fixtures `sum-producer-*` |
| `ff3e86c4e` | frontend: de toets van RGS-378 zonder NullPointerException op een verdeling in aanbouw (rood in gate 10, `SemanticTokensRecoveryTest`) |
| (deze) | Result |

### Wat er veranderde, per punt

1. **Initialisatie op een tijdlijndoel.** De laagste laag van de stapel van ch. 7.8, per dag: zij beslist elke dag waarop
   geen schrijver beslist en de invoer geen waarde heeft, over alle tijd, ook op dagen waar een regel leeg geeft. Haar
   waarde is periode-rekenkunde (een scalair op elke dag), geknipt op de periodes van haar voorwaarde; zij leest invoer
   en schrijvers, nooit haar eigen schrijfactie. Waar zij geen dag beslist, wordt haar waarde niet geëvalueerd; wat zij
   deponeert, blijft staan waar zij een dag beslist; over een asOfDate-emmerscalair van haar doel EF-037. ref:
   `World.stack` (voor, schrijvers, initialisatie), `World.init_days`, `Eval.run_timeline_initialisation`; de lezing
   is `DISPLACED` waar schrijvers en invoer elke dag beslissen. clingo: binnen de desugaring van ch. 7.7 (één
   onvoorwaardelijke initialisatie, hoogstens één rekenende regel) de defaults-groep per dag (de weigering van die
   desugaring op een tijdlijndoel is weg); daarbuiten `itl/5` en de stukken die schrijvers (`wtl/6`) en invoer
   (`inputtl/5`) vrijlaten. Bijvangst, gemeten: clingo las het totaal van een tijdlijn die een (gedesugarde)
   initialisatie schrijft als asOfDate-emmer (0) waar ref EF-036 geeft; de vraag welke regels een doel schrijven gaat
   nu naar het document vóór de desugaring.
2. **Waarde bij het maken.** Per dag de invoer van het gemaakte object (rang 2): de waarde als periode-rekenkunde op het
   onderwerp gelezen, een scalair op elke dag, een tijdlijn per periode; een regel die een dag beslist gaat ervoor, de
   initialisatie vult de rest. ref schrijft haar als invoertijdlijn van het gemaakte object; clingo als `inputtl/5` of
   `tl/5` op de term. De frontend schreef de vorm al (gemeten: `timeline-creation` en de proef c2 compileren zonder
   melding); geen wijziging aan die kant.
3. **Verdeling.** WF-128 (één arm `WF-128.time-dependent-attribute`, parameters `field`, `attribute`, `objectType`,
   een negatief per veld: `sourceAttribute`, `targetAttribute`, `remainderAttribute`, `ratioAttribute`,
   `maximumAttribute`, `orderingAttribute`); RGS-378 in de frontend voor dezelfde zes. ref en clingo draaien WF-128
   opnieuw bij een verdeling (zoals ref WF-045/046 doet) in plaats van de weigering bij naam; die weigering had geen
   code. Drools ongewijzigd (stap B): zijn weigering is een plaats van DROOLS-271, dat andere plaatsen houdt, dus geen
   `RETIRED`.
4. **clingo, de waarneming.** Een initialisatie en een waarde bij het maken op een somtijdlijn worden gedragen als een
   constructor van een literal of een kopie van een eigen somtijdlijn (golden `jutterswet-afwezigheidsmarkers-initialisatie`,
   ref-fixture `timeline-creation-marker`). Elke andere producent weigert met CLINGO-036 (`refusal`, parameter
   `attribute`) vooraan: in een initialisatie (gedesugard en natief), een default en een waarde bij het maken. Een
   regel met zo'n waarde weigert daar, gemeten, als voorheen eerder met CLINGO-024.
5. **RUNTIME-SEMANTICS** (`ae97f97ae`, `db1bdbbcb`): ch. 7.8 de stapel per dag met de meting in ALEF en de uitgewerkte
   voorbeelden `WV-7.8-stapel-per-dag-*` (ref-suite letterlijk); ch. 3.11 de initialisatie als producent en "Leeg in a
   write"; ch. 3.12 rij 10 teruggebracht tot wat open blijft (het tijdsevenredig deel en de dagentelling als waarde van
   een initialisatie of een waarde bij het maken, bij naam geweigerd); ch. 4.5 WF-128; ch. 5.4, 5.5, 5.12 een zin per
   dag. ASP-TRANSLATION: de codering per dag en WF-128 in plaats van de weigeringen.

### Goldens en fixtures

- `jutterswet-initialisatie-per-dag` (3 cases): i1–i4 van de meting, i2r en i4r als tweede volgorde, invoer naast regel
  en initialisatie (niet gemeten in ALEF), en een initialisatie "gedurende de tijd dat hij stormwacht is" naast twee
  regels (natief in clingo).
- `jutterswet-creatie-per-dag` (2 cases): c1–c4 van de meting als vier leden van één melding, en een lid met een
  initialisatie.
- `jutterswet-afwezigheidsmarkers-initialisatie` (3 cases): een initialisatie op `onbekend`, gedesugard en voorwaardelijk.
- Fixtures `ref/test/fixtures/writers-stack/`: `timeline-initialisation` (nu met verwachtingen per dag, en
  `case-bucket-scalar`), `timeline-initialisation-deposit` (drie cases), `-total`, `-window`, `timeline-creation` (met
  een som, verwachtingen per dag), `timeline-creation-marker`, `sum-producer-initialisation`, `-conditional`,
  `-creation`; `timeline-allocation` blijft het document van WF-128 (zijn `source.rgs` weigert nu met RGS-378).

Alle goldens: verifier, ref en clingo groen, geen overlever van gate 7, ref's trace zonder fout; Drools weigert ze op
beide takken met dezelfde regel.

### Rood eerst (letterlijk; main `9c275fd8b` met de nieuwe data en tests, losse worktree, opgeruimd)

- Goldens (log `13`): ref op alle acht cases `EVALUATION FAULT: temporal attribute "strandloon" as a target: temporal
  writes are not implemented` (resp. `"aanvulloon"`, `"opslagkosten"`); clingo `TRANSLATION FAULT: temporal attribute
  "stormloon" as a target: …` (resp. `"meldwaarde"`, `"keurkosten"`). Een gedesugard geval (proef i1) gaf in clingo
  `the ch. 7.7 desugaring of an initialisation on the timeline target "strandloon" is not translated (no corpus case)`.
- Negatieven (log `13`): `verifier: PASS` op alle zes.
- ref-suite (logs `15`, `23`): 18 `FAIL WV-7.8-stapel-per-dag-…`, o.a. `-initialisatie (fault: temporal attribute
  "strandloon" as a target: …)`, `-totaal`, `-emmerscalair`, `-verdeling: WF-128`, `-somproducent-sum-producer-creation`,
  daarna een ongevangen fout in `-lezing-verdrongen`.
- clingo-suite (log `24`): `test_clingo: 2663 checks, 29 FAILURES`, o.a. `… case1-zonder-invoer.json: clingo pins the
  values (fault: temporal attribute "stormloon" …)`, `the total of an initialisation over all time: expected a fault at
  solving, got one in translation (the temporal total of "strandloon" needs the evaluation date …)`, `another producer of
  a sum-typed timeline, sum-producer-initialisation (no code: the ch. 7.7 desugaring … is not translated …)`.
- frontend (log `18`): `TimelineAllocationTest` `expected: <[RGS-378 {name=verdeel de opbrengst van een berging,
  attribute=opbrengst, objectType=Berging}]> but was: <[]>`.
- Proef `tot` (bijvangst): ref `EF-036 … meets a segment with an open bound carrying 5`, clingo `clingo: PASS` (0).

### Groen

ref-suite 2496 worked examples; clingo-suite 2663 checks, 0 failures; frontend-suite 1520 tests, 0 skipped; Drools-suite
1857 tests, 0 failures, 0 skipped (gate 12; de Drools-code is niet aangeraakt).

### Mutaties (niet gecommit; losse worktree, opgeruimd; logs `21-mutations/`, `21-mutations.log`, `22-mutations.log`)

| mutatie | rood |
|---|---|
| R1 `init_days` zonder de schrijvers | 5 FAIL, o.a. `-initialisatie (J1.voorjaarsloon = [-inf,+inf)=5 [2025-03-01,2025-06-01)=20 …)` |
| R2 `init_days` zonder de invoer | 6 FAIL, o.a. `-initialisatie-invoer`, `-niet-geevalueerd`, `-lezing-verdrongen` |
| R3 geen EF-037 | `-emmerscalair` |
| R4 geëvalueerd waar zij geen dag beslist | `-niet-geevalueerd (… the channel carries: standaardstrandloon × J1; …)` |
| R5 deposito's niet speculatief | `-leeg` |
| R6 lid op een tijdlijn als scalair geschreven | 5 FAIL (`-creatie`, `-creatie-scalair`, `-creatie-marker`, …) |
| R7 geen WF-128 bij een verdeling | `-verdeling` |
| R8 lezing zonder `DISPLACED` | `-lezing-verdrongen` |
| R9 voorwaarde niet per periode | 4 FAIL (stormloon over alle tijd, `-venster` EF-036) |
| C1, C2 de laag zonder schrijvers, zonder invoer | 4 FAILURES elk (`stormloon`, `timeline-initialisation`, `-window`; `-deposit`) |
| C3 de desugaring op een tijdlijn weer geweigerd | 11 FAIL (`fault: desugared`) |
| C4 lid altijd `tl/5` | 3 FAILURES (`seizoenswaardetotaal … =27`, het gemaakte object met twee segmenten) |
| C5, C6 deposito zonder wacht (natief, gedesugard) | 2 resp. 1 FAILURES (`case-leeg`, `case.json`: `1 inconsistent fact(s)`) |
| C7 `period_arithmetic_target` op het gedesugarde document | `the total of an initialisation over all time` |
| C8 geen EF-037-code voor de zin | `an initialisation over an asOfDate-bucket scalar` |
| C9 geen WF-128 bij een verdeling | `expected a loud fault, got a model` |
| C10, C11 CLINGO-036 niet bij de natieve initialisatie, niet bij een default | `sum-producer-conditional`, `sum-producer-initialisation` (no code) |
| V1 WF-128 zonder de arm `targetAttribute` | gate 3: `wf128-target-attribute … passed the verifier` |
| F (RGS-378 weg) | = de rode run op main |

### Tests die de kaart omdraait (oud → nieuw)

1. ref-suite `WV-7.8-stapel-tijdlijn-timeline-initialisation`, `-timeline-allocation`, `-timeline-creation` (een fout in
   ref's woorden "temporal writes are not implemented") → `WV-7.8-stapel-per-dag-voorwaarde` (5, 10, 5, 20, 5),
   `-creatie-scalair` (terug 7 op elke dag), `-verdeling` (WF-128 bij de verifier, ref weigert met WF-128).
2. clingo-suite `the stack of a target: on a timeline target, …` (drie `faults_with` in ref's woorden) → `both_answer`
   op de fixtures en goldens en `faults_with` op de zin van WF-128.
3. De fixtures `timeline-initialisation` en `timeline-creation` hadden alleen `consistent`; nu verwachtingen per dag
   (`timeline-creation` met een som erbij, `ir.json` opnieuw uit de frontend).
4. `ref_matches` in de clingo-suite vangt een fout van ref per frame (was: de suite stopte); geen verwachting veranderd.

### Codes en pins (oud → nieuw)

- Nieuw: `WF-128.time-dependent-attribute`; RGS-378 `allocation-time-dependent-attribute` (ERROR; register 366 → 367
  codes); CLINGO-036 `sum-timeline-producer` (`refusal`; 35 → 36 codes). Geen code `RETIRED`.
- Weg (zonder code): ref en clingo "temporal attribute … as a target: temporal writes are not implemented" bij een
  initialisatie, een waarde bij het maken en een verdeling (bij een regel met een andere producent blijft ref's zin);
  clingo "the ch. 7.7 desugaring of an initialisation on the timeline target … is not translated".
- Gate 12 `EXPECTED_CHANNEL`: drie regels erbij (`jutterswet-afwezigheidsmarkers-initialisatie|EXCLUDE DROOLS-103`,
  `jutterswet-creatie-per-dag|EXCLUDE DROOLS-118`, `jutterswet-initialisatie-per-dag|EXCLUDE DROOLS-118`, reden "Stap B"),
  48 → 51 genoemd; `EXPECTED_PLAYED` 344 ongewijzigd. Gate 6 `EXPECTED_CHANNEL` (2), gate 7 (41 overlevers), gate 8:
  ongewijzigd. Geen vloer verplaatst.

### Voorgestelde tekst (huidige stand)

**CONSTRUCTS rij 8**, de zin "On a timeline target the writers and the input are read per day; an initialisation outside
a defaults group, an allocation and a creation's member value on a timeline target are refused by name in every
executor (ch. 3.12 row 10)." wordt:

> On a timeline target the stack is read per day: the writers decide their days, the input, the case's or the value of
> the making, stands on the other days, and the initialisation decides the days that leaves leeg, over all time; an
> allocation that names a time-dependent attribute is not well-formed (WF-128). In ref and clingo (goldens
> `jutterswet-initialisatie-per-dag`, `jutterswet-creatie-per-dag`, `jutterswet-afwezigheidsmarkers-initialisatie`;
> documents `ref/test/fixtures/writers-stack`); the Drools backend refuses the initialisation and the member value on a
> timeline target by name (2026-11-05, card `timeline-initialisation-allocation-creation`, step A).

**CONSTRUCTS rij 11**, landing: "2026-11-05, card `timeline-initialisation-allocation-creation` (step A): an
initialisation and a creation's member value on a timeline target are read per day in the stack of ch. 7.8 in ref and
clingo, after the measurement in ALEF; an allocation that names a time-dependent attribute is WF-128."

**CONSTRUCTS rij 17**, achteraan: "An allocation names no time-dependent attribute as its amount, share, rest, weight,
cap or ordering key (WF-128, RGS-378; witnesses `wf128-*`; 2026-11-05, card
`timeline-initialisation-allocation-creation`), as ALEF's typesystem refuses it."

**CLAUDE.md gate 3**, na de zin over WF-127: "Since the card `timeline-initialisation-allocation-creation` (2026-11-05)
WF-128 refuses an allocation that names a time-dependent attribute (IR-SPEC ch. 3.2), with one negative per field."

### Wat stap B moet doen (Drools, beide takken)

Gemeten op `ff3e86c4e`:

1. **De initialisatie op een tijdlijndoel per dag** als laagste laag (RS ch. 7.8): de dagen die de schrijvers
   (`Consequence.WritersUnion`) en de invoer vrijlaten, over alle tijd (`LocalDate.MIN`/`MAX`), haar voorwaarde per
   periode (`PeriodCondition`), haar deposito's waar zij een dag beslist, EF-037 over een emmerscalair van haar doel,
   EF-036 in een totaal over haar open segmenten. Nu: DROOLS-118 alleen (`jutterswet-initialisatie-per-dag`, proef i1),
   DROOLS-103 naast een rekenende regel (proef i2, `jutterswet-afwezigheidsmarkers-initialisatie`).
2. **De waarde bij het maken op een tijdsafhankelijk lid** als invoertijdlijn van de gemaakte bean
   (`PeriodArithmetic` op het onderwerp), naast schrijvers en initialisatie. Nu: DROOLS-271 (proef zonder de sommen),
   en daarvoor DROOLS-212 voor de som over de gemaakte relatie van een tijdsafhankelijk lid (proef zonder de
   initialisatie); beide liggen op de weg van `jutterswet-creatie-per-dag`.
3. **Lezingen** (gate 17): een initialisatie op een tijdlijndoel leest `DISPLACED` waar schrijvers en invoer elke dag
   beslissen, anders haar knopen met de tijdlijn die zij schrijft (ref's `Reading`, RS ch. 5.12).
4. De fixtures van `ref/test/fixtures/writers-stack/timeline-*` en `sum-producer-*` als getuigen in de Drools-suite.
5. De drie regels uit `EXPECTED_CHANNEL`, `EXPECTED_PLAYED` 344 → 347; DROOLS-118 en DROOLS-103 naar `RETIRED` als ze
   geen plaats meer hebben (meten). De verdeling: DROOLS-271 op een verdeling is op verifier-schone invoer
   onbereikbaar (WF-128); een herhaling van WF-128 zoals in ref en clingo, of de plaats laten staan.

### Voorlopige besluiten

- VB-1: de voorwaarde van een initialisatie op een tijdlijndoel leest als die van een regel op een tijdlijndoel
  (ch. 3.10): per periode waar zij een tijdlijn of een venster leest, als poort waar niet. ALEF niet gemeten met een
  voorwaarde. Vraag 1.
- VB-2: waar een initialisatie op een tijdlijndoel geen dag beslist, wordt haar waarde niet geëvalueerd; haar
  deposito's staan waar zij een dag beslist (zoals een default, ch. 7.11), ook gedesugard in clingo; een gedesugarde
  rekenende regel deponeert onder zijn eigen naam, ongewacht, zoals op een scalair doel.
- VB-3: EF-037 voor een initialisatie op een tijdlijndoel over de emmerscalair van dat doel, in de zin van de
  initialisatie-default per dag.
- VB-4: de waarde van een initialisatie en van een lid op een tijdlijndoel is periode-rekenkunde; het tijdsevenredig
  deel en de dagentelling weigeren daar bij naam (RS ch. 3.12 rij 10 open).
- VB-5: een cel van een tijdsafhankelijk attribuut in een creatie weigert bij naam in ref en clingo (geen lezing, geen
  corpusgeval).
- VB-6: WF-128 kijkt naar de zes attribuutvelden, niet naar de voorwaarde (een scalaire plek, die ch. 3.3 al bewaakt);
  ref en clingo herhalen de regel bij een verdeling (snel falen op een niet-geverifieerd document).
- VB-7: CLINGO-036 in één functie (`sum_timeline_producer`) voor periode-rekenkunde, initialisatie, default en lid;
  bij een regel blijft CLINGO-024 eerst.
- VB-8: namen `jutterswet-initialisatie-per-dag`, `jutterswet-creatie-per-dag`,
  `jutterswet-afwezigheidsmarkers-initialisatie`; fixtures Engels.

### Vragen

1. VB-1: een initialisatie met een voorwaarde op een tijdlijndoel (`stormloon`, `timeline-initialisation`, `-window`)
   leest haar voorwaarde per periode, ALEF niet gemeten. Bevestigen, of een meting in alef-bridge eerst?
2. RS ch. 3.12 rij 10 houdt alleen het tijdsevenredig deel en de dagentelling als waarde van een initialisatie of lid:
   zo laten, of weghalen tot er een vraag naar is?

### make gates

Eén groene run in de worktree op `ff3e86c4e` (`out/gates/20261105-125156/`, log `gates-2.log`); de commit daarna raakt
alleen deze kaart. De run ervoor op `f3716858d` (`gates-1.log`) had gate 10 rood: `SemanticTokensRecoveryTest`, 18
NullPointerExceptions in de toets van RGS-378 op afgeknipte bronnen (opgelost in `ff3e86c4e`).

```
start 2026-11-05 12:51:56 ff3e86c4e card/timeline-initialisation-allocation-creation
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 427 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 3s drools-jar: rebuilt (2 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver; 3s)
01-self-contained rc=0 2s
02-ir-schema rc=0 159s
04-case-schema rc=0 10s
03-verifier rc=0 51s
05-oracle rc=0 29s
06-clingo rc=0 340s
07-noop rc=0 17s
08-red rc=0 204s
09-catala rc=0 5s
10-regelspraak rc=0 301s
11-toka-parity rc=0 278s
12-drools rc=0 416s
13-reftype rc=0 383s
14-flogic rc=0 1s
15-graph rc=0 62s
16-scenario rc=0 47s
17-readings rc=0 325s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-05 12:58:55 419s red=0
```

Gate 3: 455 goldens schoon, 634 negatieven. Gate 5: ref-suite 2496, 1110 cases. Gate 6: clingo-suite 2663, 1106 cases, 2
goldens in het weigeringskanaal. Gate 7: 41 overlevers. Gate 10: 1520 tests, 202 rgs-goldens. Gate 12: JUnit 1857,
`played 344` op beide takken, 51 goldens bij naam. Gate 17: 1110 case files getraceerd, deviations 0.
