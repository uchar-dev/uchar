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

## Result stap B

*Bouwer stap B (Drools), 2026-11-05. Niet gemerged, niet gepusht. Logs onder
`werk/timeline-initialisation-allocation-creation/logs/` (`30-*` tot en met `42-*`, `gates-3.log`), scripts
`dprobe.sh`, `readings_cmp.py`, `dmutations.py`, proeven `probes/b-lazy` en `probes/b-made-link`.*

### Commits

| hash | inhoud |
|---|---|
| `2602c4da3` | Drools, beide takken: de initialisatie op een tijdlijndoel per dag, de waarde bij het maken als invoertijdlijn, de som per periode over gemaakte objecten, WF-128 bij een verdeling; `TmsTimelineStackWitnessTest`, twee eigen documenten onder `src/test/resources/timeline-stack`; README |
| `965badb73` | `scripts/check-drools.sh`: drie regels uit `EXPECTED_CHANNEL`, `EXPECTED_PLAYED` 344 → 347 (beschermd, voorstel) |
| (deze) | Result, kaart naar `done/` |

### Wat er in Drools veranderde

1. **Initialisatie op een tijdlijndoel** (`TmsTranspiler.timelineInitialisationRule`). Haar regel leest de `Value`s van de
   andere schrijvers van het doel (een accumulate met `writer != eigen naam`, `Reads.otherValues`) en de invoer op de
   bean, nooit haar eigen schrijfactie; `PeriodDefaults.initialisationDays` geeft de dagen die zij vrijlaten, over alle
   tijd, en EF-037 over de emmerscalair van het doel (de zin van ref). Haar voorwaarde leest per periode waar zij een
   tijdlijn leest of een venster houdt, anders als poort (VB-1 van stap A). Een `Let` op de beslissende dagen beëindigt
   de evaluatie waar geen dag over is, zodat haar waarde daar niet geëvalueerd wordt; `PeriodDefaults.initialised` (of
   `initialisedValues` op een somtype) knipt de gevulde segmenten van haar waarde op die dagen, onder de schrijfgrens van
   die segmenten. Het gevolg is een `Consequence.Decision` met één `StackFact.Value`: zonder beslisdag voegt zij niets in
   en vervalt wat haar waarde deponeerde. Haar `Value` draagt alleen de dagen die zij beslist, dus de vereniging
   (`writersUnionRules`, `readFixpoint`) en de botsingstoets per dag bleven ongewijzigd; `timelineWriters` en
   `periodArithmeticTarget` tellen de initialisatie mee (het totaal leest haar tijdlijn heel, EF-036 over een open
   segment), en `timelineRule` laat een initialisatie naast een rekenende regel toe.
2. **Waarde bij het maken op een tijdsafhankelijk lid** (`madeTimeline`). De waarde is periode-rekenkunde gelezen op het
   onderwerp van de creatie (een scalair op elke dag) of de constructor van een literal; een `Construction.Setting` met
   `timeline` zet haar via de route `SET_TIMELINE` (`ComputeHelpers.setTimeline`) als segmenten op de nieuwe bean; een
   lege tijdlijn laat het lid leeg. Zo is zij rang 2 van de stapel: een regel die een dag beslist gaat voor, de
   initialisatie vindt haar gevuld.
3. **Som per periode over de gemaakte relatie** (DROOLS-212 bij `periodSum`). Waar geen feitcreatie de relatie maakt en
   elke objectcreatie die haar maakt het gemaakte object in de rol van het lid koppelt (`madeOnlyIn`), leest de som de
   leden via de koppelingen, en de lidfout van een emmerscalair de plaatsen van de koppelingen van de invoer: een gemaakt
   object heeft geen invoer. Elders blijft DROOLS-212 (getuige `made-link-to-input`).
4. **Verdeling.** `overTimelines` herhaalt WF-128 over de zes velden in de volgorde van de verifier, vóór de andere
   toetsen: DROOLS-315 (`contract`, parameters `allocation`, `field`, `attribute`, `objectType`), met een zin die WF-128
   noemt. Gekozen: één eigen registercode met de parameters van de arm van de verifier, zodat de weigering één code en
   één zin heeft; de plaats van DROOLS-271 is op een verdeling daarachter onbereikbaar, DROOLS-271 houdt zijn andere
   plaatsen.
5. **Lezingen** (ch. 5.12, gate 17). `DISPLACED` zonder knopen waar schrijvers en invoer geen dag vrijlaten (de term van
   de vrije dagen als `inputDays` van de leesregel); anders haar voorwaarde en, waar de periodes van haar voorwaarde een
   vrije dag houden, haar waarde, de wortel de gevulde segmenten van haar waarde binnen die periodes
   (`DAYS_FILLED_WITHIN`). Regel voor regel gelijk aan ref's trace op de drie goldens (log `40`, beide takken) en op de
   documenten van `writers-stack` (`TmsTimelineStackWitnessTest`).
6. **Getuigen** (`TmsTimelineStackWitnessTest`, 7 tests, beide takken): de fixtures `timeline-initialisation`,
   `-deposit` (drie cases), `-window`, `timeline-creation`, `timeline-creation-marker` en de drie goldens spelen; EF-037
   (`case-bucket-scalar`) en EF-036 (`-total`); het gemaakte object houdt `onbekend`, `waarde(10)`, `onbekend`; de
   lezingen van ref; DROOLS-316 op `sum-producer-initialisation`, `-conditional`, `-creation` en DROOLS-315 op
   `timeline-allocation`; twee eigen documenten: een initialisatie die geen dag beslist en waarvan de waarde EF-037 zou
   geven speelt (ref en clingo ook), en een som over een gemaakte koppeling naar een invoerobject weigert met DROOLS-212.

### Rood eerst (letterlijk, per tak)

- De drie goldens op `85d90f25d` (verse jar, `dprobe.sh`), exec-model en DRL-tekst gelijk:
  `jutterswet-initialisatie-per-dag` `EXCLUDE DROOLS-118: transpiler SKIP: regel: standaardstrandloon — an
  initialisation of the time-dependent member 'strandloon' of Jutter: this branch does not yet carry this form; no
  golden with cases has one`; `jutterswet-creatie-per-dag` `EXCLUDE DROOLS-118: … regel: standaardaanvulloon — an
  initialisation of the time-dependent member 'aanvulloon' of Melding …`; `jutterswet-afwezigheidsmarkers-initialisatie`
  `EXCLUDE DROOLS-103: transpiler SKIP: regel: opslag in het voorjaar — a time-dependent target with a second writer
  ('opslagkosten' of Vondst is also written by "opslag onbekend") …`.
- `TmsTimelineStackWitnessTest` op de oude code (log `31`, `31-red-plays.txt`): 5 tests, 4 failures, 1 error. Alle 30
  plays (15 cases × 2 takken) `transpiler SKIP`, o.a. `timeline-initialisation/case.json exec-model transpiler SKIP:
  regel: strandloon in het voorjaar — een tijdsafhankelijk doel met een tweede schrijver …` en hetzelfde met
  `drl-tekst`; `timeline-creation/case.json exec-model … regel: terug van een jutter — een SOM per lid over het
  tijdsafhankelijke attribuut 'waarde' in een vouw over de gecreëerde relatie 'jutmelding' …`;
  `timeline-creation-marker/case.json drl-tekst … objectcreatie: onbekende berging — het tijdsafhankelijke attribuut
  'bergkosten' van Vondst op een plek die één waarde leest of schrijft …`. Fouten: `expected: <FAULT> but was:
  <EXCLUDE>`. Lezingen: `EXEC_MODEL: de canonieke tekst ==> expected: <[case case.json, reading
  /rules/rules/0/versions/0 "J1" DISPLACED, …]> but was: <[EXCLUDE DROOLS-118: …]>`. Weigeringen: `expected:
  <DROOLS-316> but was: <DROOLS-118>`. Gemaakt object: `Excluded transpiler SKIP: objectcreatie: onbekende berging —
  het tijdsafhankelijke attribuut 'bergkosten' …` (DROOLS-271).
- De twee tests op eigen documenten kwamen na de bouw: `not-evaluated` weigerde op de oude code met DROOLS-118;
  `made-link-to-input` was daar al groen (DROOLS-212), een bewaking tegen een te ruime opheffing (rood onder D11).

### Groen

Drools-suite 1864 tests, 0 failures, 0 skipped (log `41`; was 1857, +7). Frontend 1520, ref 2496, clingo 2663 (gates 10,
5, 6; die code is niet aangeraakt). `make gates` groen, zie onder.

### Mutaties (niet gecommit; losse worktree, opgeruimd; `42-dmutations.log`, `42-dmutations/`)

| mutatie | rood in `TmsTimelineStackWitnessTest` |
|---|---|
| D1 vrije dagen zonder de andere schrijvers | plays: `WritersConflict geen fixpunt: … de regel "strandloon in het voorjaar" schrijft 20 en de regel "standaardstrandloon" …` |
| D2 vrije dagen zonder de invoer | 3 tests: plays, lezing `DISPLACED`, `not-evaluated` (`InputFormFault … "kustloon"`) |
| D3 geen EF-037 | `faults: ran to a model without faulting ==> expected: <FAULT> but was: <NOFAULT>` |
| D4 waarde geëvalueerd waar geen dag over is | `not-evaluated`: `InputFormFault fout in de regel "standaardstrandloon" …` |
| D5 een besluit zonder dag blijft staan met zijn deponering | plays (`-deposit/case-leeg`) |
| D6 lezing niet verdrongen waar geen dag over is | lezingen (`DISPLACED` verwacht) |
| D7 voorwaarde altijd een poort | plays en lezingen (`-window`) |
| D8 het gemaakte object krijgt geen tijdlijn | plays en het gemaakte object (`[[2025-03-01, 2025-06-01)=wa…`) |
| D9 geen WF-128 bij een verdeling | weigering: DROOLS-271-zin in plaats van DROOLS-315 |
| D10 geen weigering van een andere producent | weigering: DROOLS-303-zin (`deze operand van het rekenen per periode …`) |
| D11 `madeOnlyIn` altijd waar | `made-link-to-input`: de zin van DROOLS-232 (`een Aggregation over de enkelvoudige elementrol 'melder' …`) |
| D12 `madeOnlyIn` altijd onwaar | plays (`timeline-creation`, `jutterswet-creatie-per-dag`) |
| D13 `timelineRule` omheint een initialisatie ernaast | 3 tests (DROOLS-103) |
| D14 het totaal leest een geïnitialiseerde tijdlijn niet heel | `-total`: de zin van DROOLS-216 (`een totaal over de eigen tijdlijn … zonder knip …`) |
| D15 een initialisatie alleen is geen schrijver van een tijdlijndoel | plays |

### Tests die de kaart omdraait (oud → nieuw)

1. `TmsTimelineRefusalWitnessTest`, rij `initialisatie` (een initialisatie met een tijdsevenredig deel als waarde): de
   zin van DROOLS-118 → die van DROOLS-316 (`de waarde van een initialisatie op het tijdsafhankelijke attribuut
   'jaardeel' van Jutter is geen rekenen per periode van ch. 3.4 en 3.5 en geen constructor van een literal ('het
   tijdsevenredig deel per year van zijn jaartarief') …`); de javadoc citeert ref's huidige zin.
2. `PeriodDecisionsWitnessTest`: een initialisatie in een record was een verschil (`… is een initialisatie of schrijft
   over een pad, wat deze getuige niet per dag leest`) → de getuige leest haar per dag als laagste laag: een dag die geen
   regel beslist en waarop de invoer leeg is, uit de wortel van haar lezing. `periodDecisionsOf` noemt de initialisatie
   op haar dagen als regel.
3. `RouteWitnessTest`: `SET_TIMELINE: void ComputeHelpers.setTimeline[Object, String, Object]` erbij.

### Codes en pins (oud → nieuw)

- DROOLS-118 `initialisation-of-time-attribute`: `refusal` → `RETIRED`, gemeten: geen plaats meer
  (`check-drools-codes`: 26 retired, was 25).
- Nieuw: DROOLS-315 `allocation-time-dependent-attribute` (`contract`) en DROOLS-316 `time-target-producer-value`
  (`refusal`, parameters `form`, `attribute`, `declarant`, `snippet`, delen `.initialisation` en `.member`); register
  314 → 316 codes, catalogi vier regels per taal erbij.
- Blijven, gemeten: DROOLS-103 (een tweede schrijver van een tijdsafhankelijk kenmerk; een verdeling of defaults-groep
  naast een regel op een tijdlijndoel, op verifier-schone invoer onbereikbaar door WF-128 en WF-050), DROOLS-212 (de
  vouw per element over een gemaakte relatie; de som per periode waar een gemaakte koppeling een invoerobject bereikt),
  DROOLS-271 (andere plaatsen), DROOLS-105 (rekenende regels).
- Gate 12: `EXPECTED_CHANNEL` 51 → 48 bij naam; `EXPECTED_PLAYED` 344 → 347, de DRL-tekst-tak ook 347. Gate 17: 345 →
  348 goldens en 973 → 981 case files vergeleken, 7658 → 7722 lezingen, deviations 0. Geen vloer verplaatst
  (`EXPECTED_MIN_DROOLS_CODES=289`, `EXPECTED_MIN_DROOLS_MESSAGES=325`, de vloeren van gate 17 en
  `ReachabilityWitnessTest`).

### Voorgestelde tekst (huidige stand)

**CONSTRUCTS rij 8**, de zin "In ref and clingo (goldens `jutterswet-initialisatie-per-dag`, `jutterswet-creatie-per-dag`,
`jutterswet-afwezigheidsmarkers-initialisatie`; documents `ref/test/fixtures/writers-stack`); the Drools backend refuses
the initialisation and the member value on a timeline target by name (2026-11-05, card
`timeline-initialisation-allocation-creation`, step A)." wordt:

> In ref, clingo and both Drools branches (goldens `jutterswet-initialisatie-per-dag`, `jutterswet-creatie-per-dag`,
> `jutterswet-afwezigheidsmarkers-initialisatie`; documents `ref/test/fixtures/writers-stack`; 2026-11-05, card
> `timeline-initialisation-allocation-creation`).

**CONSTRUCTS rij 11**, de zin "2026-11-05, card `timeline-initialisation-allocation-creation` (step A): an initialisation
and a creation's member value on a timeline target are read per day in the stack of ch. 7.8 in ref and clingo, after the
measurement in ALEF; an allocation that names a time-dependent attribute is WF-128." wordt:

> 2026-11-05, card `timeline-initialisation-allocation-creation`: an initialisation and a creation's member value on a
> timeline target are read per day in the stack of ch. 7.8 in ref, clingo and both Drools branches, after the measurement
> in ALEF; an allocation that names a time-dependent attribute is WF-128.

**CLAUDE.md gate 12**, in de lijst van `RETIRED`, na "since the card `writers-as-one-defaults-stack` (2026-11-04)
DROOLS-142,": "since the card `timeline-initialisation-allocation-creation` (2026-11-05) DROOLS-118,". En na de zin over
`periodDecisionsOf` ("… witness `PeriodDecisionsWitnessTest`)."):

> Since the card `timeline-initialisation-allocation-creation` (step B, 2026-11-05) an initialisation on a timeline
> target is the lowest layer of its stack per day on both branches (RUNTIME-SEMANTICS ch. 7.8;
> `TmsTranspiler.timelineInitialisationRule`, `PeriodDefaults.initialisationDays`): it reads the `Value`s of the other
> writers and the input on the bean, never its own write, and decides, within the periods of its condition, the days
> they leave on which its value reads one; the member value of an object creation on a time-dependent attribute is the
> input of the made object per day (`ComputeHelpers.setTimeline`); and an allocation that names a time-dependent
> attribute is refused as WF-128 (DROOLS-315).

**CLAUDE.md gate 17**, na de alinea over `defaults-per-period-on-timeline`:

> Since the card `timeline-initialisation-allocation-creation` (step B, 2026-11-05) an initialisation on a timeline
> target is read on both sides as ch. 5.12 reads it: `DISPLACED` without nodes where the writers and the input leave it
> no day, and else its condition and its value, the root the filled segments of its value within the periods of its
> condition; no floor moved.

### Voorlopige besluiten

- VB-B1: het gevolg van een initialisatie op een tijdlijndoel is een `Consequence.Decision`, zoals een default per dag:
  haar `Value` draagt alleen de dagen die zij beslist, en wat haar waarde deponeert staat alleen waar zij een dag
  beslist (ch. 7.8, 7.11).
- VB-B2: de schrijfgrens geldt op de segmenten die zij beslist, zoals in ref (`World.fits … decided`); de wortel van haar
  lezing zijn de gevulde segmenten binnen de periodes van haar voorwaarde, ongeknipt op de vrije dagen, zoals ref's trace.
- VB-B3: WF-128 in Drools als eigen `contract`-code DROOLS-315 met de parameters van de arm van de verifier en de naam
  van de verdeling, in de veldvolgorde van de verifier (de keuze die de opdracht vroeg te noteren).
- VB-B4: één code, DROOLS-316, voor de waarde van een initialisatie en van een lid op een tijdlijndoel die geen
  periode-rekenkunde en geen constructor van een literal is; een deel noemt de vorm. DROOLS-105 blijft de code van
  rekenende regels, die het tijdsevenredig deel en de dagentelling wel dragen.
- VB-B5: DROOLS-212 alleen opgeheven voor de som per periode waarvan elke gemaakte koppeling een gemaakt object in de rol
  van het lid zet; de lidfout leest dan de plaatsen van de koppelingen van de invoer.
- VB-B6: een lid van een creatie met een lege tijdlijn blijft leeg op de bean (geen tijdlijn), zoals ref niets schrijft.

### Vragen

Geen die raken wat een regel betekent.

### make gates

Eén groene run in de worktree op `965badb73` (`out/gates/20261105-134356/`, log `gates-3.log`); de commit daarna raakt
alleen deze kaart.

```
start 2026-11-05 13:43:56 965badb73 card/timeline-initialisation-allocation-creation
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 427 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 572 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 179s
04-case-schema rc=0 10s
03-verifier rc=0 61s
05-oracle rc=0 34s
06-clingo rc=0 366s
07-noop rc=0 20s
08-red rc=0 219s
09-catala rc=0 6s
10-regelspraak rc=0 332s
11-toka-parity rc=0 311s
12-drools rc=0 457s
13-reftype rc=0 421s
14-flogic rc=0 2s
15-graph rc=0 71s
16-scenario rc=0 57s
17-readings rc=0 350s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-05 13:51:33 457s red=0
```

Gate 12: `played 347` op beide takken, 48 goldens bij naam, JUnit 1864. Gate 17: 348 goldens, 981 case files, 7722
lezingen, deviations 0. Gate 3: 455 goldens schoon, 634 negatieven. Gate 5: 2496, 1110 cases. Gate 6: 2663, 1106 cases.
Gate 10: 1520 tests, 202 rgs-goldens.
