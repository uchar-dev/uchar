# reading-after-refusal: de build met lezingen sluit de lezing van een geweigerde vorm af

*Vondst van de kaart `ref-trace-and-readings-gate` (2026-10-21), op uchar `15f39170`. Geen fout antwoord: de
productiebuild raakt het niet, en de replay sluit de vier goldens uit; alleen de build met lezingen faalt luid op een
eigen manier.*

## Aanleiding

`TmsTranspiler` opent de lezing van een vorm (`beginReading`) vóór haar verlaging en sluit haar na de verlaging
(`endReading`). Weigert de verlaging de vorm halverwege, dan vangt `meet` de `Refusal`, maar `noted` en `notedUnits`
blijven staan. De volgende vorm faalt dan bij `beginReading` met `IllegalStateException: de lezing van een vorm begint
terwijl die van een andere loopt`. Onder `DroolsBackendMain --readings` (beide takken) eindigen daardoor
`anchor/jutterswet-bootberging`, `jutterswet-ketenkwantor`, `jutterswet-muntketen` (DROOLS-067) en
`jutterswet-havenrooster` (DROOLS-179) in `ERROR`, terwijl de replay ze uitsluit met hun weigering. Gate 17 houdt ze in
`READINGS_ERROR_KNOWN`, met reden; die lijst faalt op veroudering zodra de fout weg is.
`ReadingRulesFixpointWitnessTest` zag het niet, omdat die alleen gespeelde goldens bouwt.

## Doel

`meet` sluit bij een `Refusal` een open lezing af, zodat de build met lezingen een golden die de replay uitsluit met
dezelfde regel uitsluit.

## Acceptatie

- (a) Rood eerst: een getuige dat de build met lezingen voor elke golden die de replay uitsluit dezelfde
  `EXCLUDE`-regel geeft (beide takken); rood vóór de fix, met de vier goldens hierboven.
- (b) `READINGS_ERROR_KNOWN` in `scripts/check-readings.sh` leeg (voorstel; beschermd pad), gate 17 groen, met de vier
  goldens als `EXCLUDE` vergeleken.
- (c) De Drools-suite en gate 12 groen; de productiebuild bytegelijk.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java`, een getuigetest,
`scripts/check-readings.sh` als voorstel, deze kaart.

Out of scope: de weigeringen zelf (DROOLS-067, DROOLS-179).

## Result (2026-10-21)

Tak `card/fixpoint-order-and-refusal-reading`, vanaf `0f45965f`, samen met `fixpoint-inconsistency-order`. Geraakt:
`backends/drools` (buiten de productieruntime), `scripts/check-readings.sh` (voorstel, beschermd pad), de kaart.

- `0cb1525c` de kaart naar `doing/`.
- `e3b7f597` de getuige `ReadingsExclusionWitnessTest` (acceptatie (a)), rood.
- `11ce971a` `meet` sluit de lezing van een geweigerde vorm af; `READINGS_ERROR_KNOWN` leeg (acceptatie (b), voorstel).

**Rood eerst.** `ReadingsExclusionWitnessTest`: per golden met een case-bestand zegt de build zonder lezingen op de
exec-model-tak of de replay hem uitsluit (elke uitsluiting van het corpus is er een van de build: een weigering van de
lader, een vorm die de transpiler niet verlaagt, een botsing van beannamen; gate 12 houdt de regel van de DRL-teksttak
gelijk aan die van de exec-model-tak); voor elke zo uitgesloten golden moet de statusregel van `DroolsBackendMain`
zonder en met `--readings` op beide takken dezelfde `EXCLUDE`-regel zijn. Vloer `EXPECTED_MIN_EXCLUDED=46` (de
`EXCLUDE`-pins van gate 12, gemeten 2026-10-21). Looptijd 5,3 s. Rood op `e3b7f597` (`u4/logs/red-refusal-1.log`):

```
ReadingsExclusionWitnessTest: 46 goldens die de replay uitsluit, op 2 takken vergeleken
[ERROR]   ReadingsExclusionWitnessTest.the_build_with_readings_excludes_every_golden_the_replay_excludes_with_the_same_line:83
  goldens die de build met lezingen anders uitsluit dan de replay ==> expected: <[]> but was: <[
  anchor/jutterswet-bootberging EXEC_MODEL: replay EXCLUDE DROOLS-067: transpiler SKIP: regel: laagste vondstgewicht van
  een jutter — sourcePath-stap 1 (Boot -> Vondst) heeft geen beanveld — … | met lezingen ERROR IllegalStateException: de
  lezing van een vorm begint terwijl die van een andere loopt, …]>
```

Acht verschillen: `anchor/jutterswet-bootberging`, `jutterswet-ketenkwantor`, `jutterswet-muntketen` (DROOLS-067) en
`jutterswet-havenrooster` (DROOLS-179), elk op beide takken. Groen sinds `11ce971a`.

**Wat veranderde.** `TmsTranspiler.meet` onthoudt hoeveel leesregels er staan voordat het een vorm verlaagt, en roept bij
een `Refusal` `abandonReading(keep)` aan: `noted` en `notedUnits` op null, en de leesregels die de vorm vóór de weigering
plande vallen weg (een stapel plant er een per default; een geweigerde vorm heeft geen regel en dus geen leesregel). Dat
laatste was in `DroolsBackend` onzichtbaar, want elke SKIP sluit de hele build uit (`DroolsBackend.analyse`), maar
`TmsTranspiler.withReadings` is publiek en zijn plan hield dan een leesregel van een vorm zonder regel. De productiebuild
verandert niet: zonder lezingen opent `beginReading` niets (`positions == null`), plant geen vorm een leesregel (elke
aanroep van `readingRule` staat achter `noted != null`), en is `abandonReading` dus een lege handeling; niet apart met
twee jars gemeten.

**Gate 17** (`scripts/check-readings.sh`, voorstel): `READINGS_ERROR_KNOWN` is leeg; de lijst, haar redeneis en haar
veroudering blijven als mechanisme, de commentaarregel zegt welke vier er stonden en waarom ze weg zijn. Gate 17 op
`bd74da98`: PASS, 46 goldens uitgesloten (4 DROOLS-002, 16 -008, 7 -009, 6 -010, 2 -021, 2 -023, 2 -054, 3 -067, 1 -070,
1 -104, 1 -161, 1 -179), geen enkele `readings ERROR`, de vier dus als `EXCLUDE` met de regel van de replay vergeleken;
de tellingen gelijk aan die van de voorganger (835 / 261 / 706 / 3826 / 13971 / 3118 / 314 / 76), afwijkingen 0.

**Acceptatie (c).** De Drools-suite groen (1648 tests, was 1646); gate 12 `--fast` groen (played 261, gelijk aan
`EXPECTED_PLAYED`); de volledige gate 12 met de DRL-tekstsweep door de hoofdsessie.

#### Voorgestelde tekst voor `CLAUDE.md` (gate 17)

In gate 17 de zin tussen haakjes na `READINGS_ERROR_KNOWN` vervangen:

```
    `ERROR` fails unless the golden stands in the reason-carrying
    `READINGS_ERROR_KNOWN` (empty since card `reading-after-refusal`,
    2026-10-21: the four goldens it held, whose build with readings
    refused after a form the transpiler refused part-way, are compared
    as `EXCLUDE` since `meet` closes the reading of a refused form).
```

Gate 12: niets nieuws; de getuige telt mee in `EXPECTED_MIN_TESTS` en wordt niet bij naam geëist.

Voor `spec/CONSTRUCTS.md` en `spec/PLAN.md`: niets (een fout van de build met lezingen, geen vorm, geen scope).

#### Gates

De volledige set door de hoofdsessie op `bd74da98` (`werk/defaults-decider/u4/gates-1`): alle zeventien `rc=0`,
`EXIT 0` (gate 12 798 s, played 261 op beide takken, JUnit 1648; gate 13 228 s; gate 17 430 s, afwijkingen 0, geen
`readings ERROR`). Gebouwd door een Opus-bouwer op één tak met de kaart `fixpoint-inconsistency-order`; de hoofdsessie draaide de
set, landde het voorstel voor `scripts/check-readings.sh` en de `CLAUDE.md`-tekst, en schreef dit Result af.
