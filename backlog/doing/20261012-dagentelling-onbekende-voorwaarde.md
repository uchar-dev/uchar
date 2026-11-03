# dagentelling-onbekende-voorwaarde: de frontend laat een voorwaarde van een dagentelling niet meer stil weg

*Vastgelegd door de hoofdsessie op 2026-10-12, uit de bijvangst van de toolchainkaart
`20261010-sessie-via-de-ingang-van-uchar` (fixronde 1). Een bestaande fout in de frontend krijgt een eigen kaart.
Gemeten op uchar `b536bbc9`, met de verifier, ref en clingo uit `_build` van de hoofdcheckout. Nagemeten op
`95947ae0f` (2026-11-03) met de verifier, ref, clingo en de Drools-jar uit de eigen worktree; de tabel hieronder
draagt de meting van `95947ae0f`.*

## Aanleiding

Een kopie van de golden `jutterswet-dagentelling-rgs` waarin de regel `borgdagen` een niet-gedeclareerd kenmerk noemt:

    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat hij borgvrij is.

(in de golden: "borgplichtig"). De frontend (`bin/regelspraak check`, en `make ir`) geeft geen fout en geen
waarschuwing over `borgvrij`. Ze schrijft een `dayCount` zonder voorwaarde in het IR: één filter in plaats van twee, en
geen `Unresolved`-knoop. Hetzelfde gebeurt met een voorwaarde die geen kenmerk is ("dat zijn heffing gevuld is").

Het IR zegt daarmee iets anders dan de bron. Een stil fout antwoord geeft het niet: elke uitvoerder weigert het
document.

| Uitvoerder | Uitkomst op `95947ae0f` (case `case1-voorbeeld.json`) |
|---|---|
| frontend (`make ir`) | exit 0, geen diagnose; `"kind": "dayCount", "arguments": []` zonder `filter` |
| verifier | `WF-058.day-count-condition FAIL /rules/rules/0/versions/0/value: dayCount without a gedurende condition` |
| ref | `EVALUATION FAULT: REF-011: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a gedurende condition over an own temporal characteristic); no corpus case for more` |
| clingo | `TRANSLATION FAULT: CLINGO-024:` met dezelfde zin |
| Drools, beide takken | `EXCLUDE DROOLS-114: transpiler SKIP: regel: borgdagen van een jutter — a day count ('het aantal dagen in de maand dat hij borgvrij is') with a condition that is not a characteristic of the subject (no condition): this branch does not yet carry this form; …` |

"dat zijn heffing gevuld is" (in die kopie geen attribuut van de Jutter) geeft dezelfde vijf regels.

De afnemer krijgt dus een weigering die over een ontbrekende voorwaarde gaat, en niet de echte oorzaak: een onbekende
naam in zijn bron.

Na te maken: een kopie van de golden met in regel `borgdagen` het kenmerk `borgvrij` in plaats van `borgplichtig`,
met `make -C frontends/regelspraak ir FILE=… OUT=…` en de case `case1-voorbeeld.json` van de golden.

## Wat er gebeurt

De frontend meldt een onbekende of ongeldige voorwaarde van een dagentelling bij de resolutie. Ze geeft een diagnose
met de naam, of een `Unresolved`-knoop zoals bij andere onopgeloste namen, en laat de voorwaarde nooit stil weg. Het
IR van een bron zonder diagnose is verifier-schoon.

## Acceptatie (aangescherpt bij het oppakken)

- (a) Rood eerst: een frontendtest met de bron hierboven, die de diagnose pint met code en naam
  (`DayCountConditionTest.a_name_that_binds_no_characteristic_is_named`).
- (b) Gate 10 groen, en elke golden met een `source.rgs` emitteert byte-gelijk.
- (c) Alle gates van `CLAUDE.md` groen (`make gates` in de worktree).
- (d) De verwante vormen (een tijdsevenredig deel, een totaal "gedurende de tijd dat") nagemeten; een stille
  weglating op dezelfde plek in de resolver mee gerepareerd, met een test.

## Omvang

Klein. May touch: `frontends/regelspraak/**` (resolver en tests), kaartverplaatsing.

## Result

Bouwer, 2026-11-03, branch `card/frontend-time-bugs` op main `95947ae0f`, samen met de kaart
`frontend-heelperiode-in-aantalfilter`. Niet gemerged, niet gepusht.

### Commits

- `cfc929699` beide kaarten naar doing
- `099c53112` frontend: de voorwaarde van een dagentelling valt niet meer stil weg
- `764c718ec` frontend: een clip leest het kenmerk van een ander object niet als eigen
- en de commit van deze Results

### De meting op main (`95947ae0f`)

De tabel onder "Aanleiding". Daarnaast, met kopieën van `jutterswet-dagentelling-rgs`, `jutterswet-getijdenwacht`,
`jutterswet-bakenmeester` en `jutterswet-tegenwacht`, door `make ir` en de verifier:

| Vorm | Frontend op `95947ae0f` | Verifier |
|---|---|---|
| dagentelling, "dat hij vrijgesteld is" (geen tijdsafhankelijk kenmerk) | geen diagnose, `dayCount` zonder filter | `WF-058.day-count-condition` |
| dagentelling, "dat zijn heffing gevuld is" / "dat zijn heffing groter is dan 3" (tijdsafhankelijk attribuut) | geen diagnose, `dayCount` zonder filter | `WF-058.day-count-condition` |
| dagentelling, "dat hij niet borgplichtig is" | geen diagnose, `CharacteristicCheck` met `negated: true` | `WF-058.condition-not-check` |
| dagentelling, "dat hij aan alle volgende voorwaarden voldoet: …" | geen diagnose, `Compound` | `WF-058.condition-not-check` |
| dagentelling, "dat zijn ploeg borgplichtig is" (Ploeg en Jutter allebei `borgplichtig`, "Eén ploeg omvat meerdere jutters") | geen diagnose, de toets op het eigen `borgplichtig` van de Jutter | `verifier: PASS` |
| dagentelling, "dat hij gedurende de gehele maand borgplichtig is" | geen diagnose, de toets zonder kwalificatie | `verifier: PASS` |
| totaal, "gedurende de tijd dat hij gedurende het gehele jaar wachtplichtig is" | geen diagnose, de toets zonder kwalificatie | `verifier: PASS` |
| tijdsevenredig deel, "gedurende de tijd dat hij gedurende de gehele maand dienstdoend is" | geen diagnose, de toets zonder kwalificatie | `verifier: PASS` |
| totaal, "… dat zijn wachtvergoeding per maand gedurende het gehele jaar groter is dan 10" | geen diagnose, de vergelijking zonder kwalificatie | `verifier: PASS` |
| totaal, bullet "- hij is gedurende het gehele jaar borgplichtig" | geen diagnose, het lid zonder kwalificatie | `verifier: PASS` |
| totaal, "gedurende de tijd dat zijn ploeg bemand is" (Ploeg en Jutter allebei `bemand`) | geen diagnose, de toets op het eigen `bemand` van de Jutter (gemeten op `e9ce472f7`, waar die route nog die van main is; de test is rood op `95947ae0f`) | niet gedraaid |
| tijdsevenredig deel, "gedurende de tijd dat hij dienstvrij is" (onbekende naam) | `RGS-070`-WARNING en een `Unresolved`-filter | `verifier: PASS` |
| totaal, "gedurende de tijd dat hij wachtvrij is" (onbekende naam) | `RGS-070`-WARNING en een `Unresolved`-filter | `WF-055.during-condition-shape` |

De onbekende naam valt dus in de verwante vormen niet stil weg: het totaal en het tijdsevenredig deel schrijven al de
marker met `RGS-070`. Dezelfde helpers (`temporalCharacteristicCheckFromOperator`, `duringComparison`,
`duringNavigationCharacteristicCheck`, `nonTemporalComparison`, `duringCompound`) lieten wel twee andere dingen stil
vallen: de kwalificatie "gedurende de/het gehele …" en het onderwerp van een lidmaatschap. Beide zijn mee gerepareerd,
omdat het dezelfde plek in de resolver is.

### Wat er in de frontend veranderde

`ExpressionBuilder.dayCountCondition` leest de voorwaarde van een dagentelling. Alleen een positieve toets op één
eigen, enkelvoudig tijdsafhankelijk kenmerk van het onderwerp wordt het filter (RUNTIME-SEMANTICS ch. 3.8; WF-058
weigert al het andere, ook een ontkende toets en een samengestelde voorwaarde; ref weigert elke andere vorm met
`REF-010`, gelezen in `ref/lib/eval.ml` regels 3060–3069 en gemeten voor een `Unresolved`-filter). Elke andere voorwaarde blijft als `Unresolved`-filter met een reden op de wire
staan; de survivor-sweep (`Resolver.reportUnresolved`) meldt haar als `RGS-092`-WARNING, die de voorwaarde letterlijk
citeert (WF-113). Een naam die geen kenmerk van het onderwerp bindt, krijgt de reden `RGS-092.day-count-name-unbound`
met de parameters `name` en `subject`; elke andere vorm krijgt `RGS-092.day-count-condition-form`. Voor de bron met
`borgvrij`:

    WARNING (line 8): onopgelost: de voorwaarde van een dagentelling noemt 'borgvrij', en dat is geen kenmerk van het
    onderwerp 'Jutter'; een dagentelling telt de dagen waarop het onderwerp een kenmerk heeft (§13.4.17.57) — 'hij
    borgvrij is' blijft als marker op de wire staan

Het IR draagt `"filter": {"node": "Unresolved", "source": {"snippet": "hij borgvrij is"}}`, en de verifier zegt
`WF-058.condition-not-check FAIL /rules/rules/0/versions/0/value/filter: the day-count gedurende condition is an own
temporal characteristic check`.

In de clips van een totaal en een tijdsevenredig deel, en in de voorwaarde van een dagentelling:

- een kwalificatie "gedurende de/het gehele …" op een kenmerktoets, een vergelijking, een genavigeerde toets of een
  statisch lid van een samengestelde voorwaarde maakt de voorwaarde een marker (een clip-lid draagt geen kwalificatie,
  WF-097); voor een totaal en een tijdsevenredig deel is dat de `RGS-070`-marker die de gedurende-route al schrijft;
- een lidmaatschap leest het eigen kenmerk van het onderwerp alleen waar het onderwerp het regelonderwerp noemt
  (`denotesSubject`: "hij" of de naam van het type), in de enkele voorwaarde, in elke bullet en in het onderwerp van
  een samengestelde voorwaarde; elk ander onderwerp wordt de marker van zijn route. De navigatietoets die er al stond,
  blijft voor de genavigeerde clip (`duringNavigationCharacteristicCheck`), die ongewijzigd lowert.

Na de verandering, door `make ir` en de verifier over dezelfde kopieën: elke dagentellingsvorm uit de tabel hierboven
geeft één `RGS-092`-WARNING en `WF-058.condition-not-check`; de clips met een kwalificatie geven `RGS-070` en, voor
het totaal, `WF-055.during-condition-shape`, voor het tijdsevenredig deel `verifier: PASS` (zie Open punten). De
vormen zonder kwalificatie en met "hij" als onderwerp emitteren als voorheen: gate 10 houdt de 175 goldens met een
`source.rgs` byte-gelijk.

`temporalCharacteristicCheckFromOperator` haalt de naambinding uit een nieuwe helper, `boundCharacteristic`, die
`dayCountCondition` ook gebruikt om een ongebonden naam van een gebonden naam van de verkeerde vorm te scheiden.

### Nieuwe codes

Geen. Twee nieuwe delen van `RGS-092` in beide catalogi (`messages-frontend.nl.tsv`, `messages-frontend.en.tsv`):
`RGS-092.day-count-name-unbound` en `RGS-092.day-count-condition-form`. `diagnostic-codes.tsv` is niet veranderd;
`check-rgs-codes.sh`: `362 codes, 384 sites`.

### Rood en mutaties

Rood eerst: de testklasse `DayCountConditionTest` (22 tests) op de code van `95947ae0f`, in een losse worktree met
alleen de twee nieuwe testbestanden erbij: 18 rood, 4 groen (de vier die een vorm pinnen die de frontend blijft
omzetten: de eigen toets, het onderwerp bij de naam van zijn type, en twee keer de clips zonder kwalificatie).
Letterlijk, voor de bron met `borgvrij`:

    a_name_that_binds_no_characteristic_is_named() ✘ [] ==> expected: <1> but was: <0>

De lege lijst is de lijst van WARNINGs: de frontend zegt niets. De andere zeventien rode tests falen met precies
dezelfde regel. Op de branch: 22 groen.

Mutaties (niet gecommit, in een losse worktree, elk alleen):

| Mutatie | Rood |
|---|---|
| M1 de vormmarker wordt weer `null` (de stille weglating) | 8 tests, onder meer `a_predicate_is_no_characteristic`, `a_comparison_is_no_characteristic`, `a_negated_characteristic_is_refused_by_its_form` |
| M2 een ontkende toets wordt weer het filter | `a_negated_characteristic_is_refused_by_its_form` |
| M3 het onderwerp van de dagentelling wordt niet getoetst | `a_characteristic_of_another_object_is_not_read_as_the_subjects_own`, `a_characteristic_of_an_object_over_a_role_by_type_is_not_read_as_the_subjects_own` |
| M4 een ongebonden naam krijgt de vormreden | `a_name_that_binds_no_characteristic_is_named` |
| M5 de kwalificatie op een kenmerktoets valt weer weg | `a_whole_period_qualifier_is_refused_by_its_form`, `a_total_keeps_no_clip_whose_qualifier_it_drops`, `a_pro_rata_part_keeps_no_clip_whose_qualifier_it_drops`, `a_compound_clip_keeps_no_member_whose_qualifier_it_drops` |
| M6 de kwalificatie op een clipvergelijking valt weer weg | `a_comparison_clip_keeps_no_qualifier_it_drops` |
| M7 de kwalificatie op een genavigeerde clip valt weer weg | `a_navigated_clip_keeps_no_qualifier_it_drops` |
| M8 de kwalificatie op een statisch lid valt weer weg | `a_static_member_of_a_compound_clip_keeps_no_qualifier_it_drops` |
| M12 het onderwerp van een enkel clip-lidmaatschap wordt niet getoetst | `a_clip_does_not_read_another_objects_characteristic_as_the_subjects_own` |
| M13 het onderwerp van een lidmaatschapsbullet wordt niet getoetst | `a_compound_clip_does_not_read_another_objects_characteristic_in_a_bullet` |
| M14 het onderwerp van een samengestelde voorwaarde wordt niet getoetst | `a_compound_clip_over_another_object_is_not_the_subjects` |

(M9–M11 horen bij de kaart `frontend-heelperiode-in-aantalfilter`.)

### Floors en pins

Niet veranderd. `MarkerReasonPinTest` is niet aangeraakt: de nieuwe markers dragen een reden.

### CONSTRUCTS

Geen rij verandert van vorm; het wire-formaat is hetzelfde. Voorgestelde aanvulling op rij 29 staat in het Result
van `frontend-heelperiode-in-aantalfilter`.

### Voorlopige besluiten

1. **Marker met reden, geen nieuwe code.** Een onopgeloste naam in een regelvoorwaarde ("indien hij borgvrij is")
   wordt een `Unresolved`-knoop met een reden die de sweep als `RGS-092` meldt
   (`RGS-092.no-characteristic-no-role`). De voorwaarde van een dagentelling volgt die vorm, met twee eigen redenen,
   zodat de naam als parameter in de reden staat. Een WARNING en geen ERROR, zoals bij de markers van de gedurende-route
   (`RGS-069`, `RGS-070`): het document wordt geschreven en de verifier weigert het (`WF-058.condition-not-check`), en
   de afnemer leest in de diagnose de echte oorzaak.
2. **Een ontkende en een samengestelde voorwaarde van een dagentelling worden geweigerd** in plaats van uitgeschreven:
   de verifier weigert beide (`WF-058.condition-not-check`), ref volgens zijn code ook (`REF-010`), en geen golden
   draagt ze.
3. **De kwalificatie en het onderwerp in de gedurende-clips** zijn mee gerepareerd, omdat ze in dezelfde helpers zitten
   als de voorwaarde van de dagentelling en het IR anders iets anders zegt dan de bron zonder één diagnose.
4. **Het onderwerp van een lidmaatschap** telt als het regelonderwerp bij "hij" en bij de naam van het type, zoals
   `RuleChecker.subjectHead` het leest, zonder de kop van een meerwoordig kenmerk die `subjectHead` nog
   reconstrueert: geen golden schrijft die vorm in een clip.

### Open punten

- Een `timeProportionalPart` met een `Unresolved`-filter is `verifier: PASS` (de kopie van `jutterswet-bakenmeester`
  met "dienstvrij", gemeten op `95947ae0f` en op de branch); WF-056 weigert alleen een ontbrekend filter. De frontend
  meldt de marker met `RGS-070`; of de verifier hem ook moet weigeren, zoals WF-055 en WF-058 dat doen, is een vraag
  voor `spec/verifier/` (beschermd pad, buiten deze kaart).

### `make gates`

Eén run in de worktree, op 2026-11-03 09:46–09:52, op de tree van `764c718ec` (gedraaid toen de tip nog
`9249e6011` heette; daarna zijn alleen twee onderwerpregels ingekort, de trees zijn gelijk). Geen andere gate-set
liep bij de start. De samenvatting:

    ocaml rc=0 0s ocaml-build: fresh (stamp newer than 229 source files)
    frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
    jar rc=0 3s drools-jar: rebuilt (5 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver; 3s)
    01-self-contained rc=0 1s
    02-ir-schema rc=0 123s
    04-case-schema rc=0 7s
    03-verifier rc=0 40s
    05-oracle rc=0 23s
    06-clingo rc=0 272s
    07-noop rc=0 14s
    08-red rc=0 144s
    09-catala rc=0 5s
    10-regelspraak rc=0 234s
    11-toka-parity rc=0 222s
    12-drools rc=0 329s
    13-reftype rc=0 288s
    14-flogic rc=0 2s
    15-graph rc=0 48s
    16-scenario rc=0 40s
    17-readings rc=0 250s
    after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
    end 2026-11-03 09:52:29 332s red=0
    logs: out/gates/20261103-094657/
