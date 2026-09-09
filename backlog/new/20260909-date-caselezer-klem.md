# De date-kant van de Drools-case-lezer heeft geen klem — drie levende routes

Goal: beslissen of de `date`-kant van de case-wire dezelfde patroonklem plus
getuige krijgt die de `dateTime`-kant op 2026-09-06/09 kreeg, of dat de
huidige kale lezing met een reden blijft staan. De kaart levert het besluit
plus, als het besluit "klem" is, de klem en zijn getuige. Het gaat om DRIE
plekken, niet één.

## De stand (gemeten 2026-09-09)

`CaseRunner` leest op drie levende plekken een datum van de case-wire met
een kale `LocalDate.parse(...)`. Geen van de drie heeft een patroonklem of
een eigen melding; de `dateTime`-klem één regel verderop (r. 904) heeft
allebei.

| plek | wat het leest | wat een ongeldige waarde doet |
| --- | --- | --- |
| r. 903 `waardeNaarArg` | een `date`-waarde van een attribuut, parameter, tijdlijnsegment of read-back-verwachting | vormklasse gemeten (zie hieronder); geen replay gemeten |
| r. 600 `asOfDatum` | de `asOfDate` van de case (schema-property `asOfDate`, patroon `^[0-9]{4}-[0-9]{2}-[0-9]{2}$`) | **stil aanvaard**: `ref` faalt hard, Drools meldt `PLAYED 1` |
| r. 721 `grensDatum` | de `from`/`to` van een tijdlijnsegment | **stil aanvaard**: `ref` faalt hard, Drools meldt `PLAYED 1` |

**De twee onderste zijn de ernstige.** Gemeten in lens 2 (2026-09-09), met
`+012026-03-15` als `asOfDate` op `jutterswet-penningjaar` respectievelijk
`-0500-01-01` als segmentgrens op `jutterswet-bakenmeester`: `ref` weigert
allebei hard (`EVALUATION FAULT`), en de Drools-backend meldt gewoon
`PLAYED 1`. De golden passeert dus. Dat is geen smaakverschil in een
foutmelding maar een stille aanvaarding op de plek waar de hele
evaluatiedatum respectievelijk het emmerraster aan hangt: `+012026-03-15`
is jaar +12026.

**De vormklasse zelf is smal maar niet leeg.** `LocalDate.parse` weigert
het meeste wat de wire ook weigert (smal veld `2026-3-15`, zonesuffix
`2026-03-15Z`, basisformaat `20260315`, omringende spatie, een
`dateTime`-lexeem, en de kalender: `2026-02-30`). Het accepteert wél de
twee ISO-vormen met een uitgebreid jaar plus teken die het schemapatroon
uitsluit: `+012026-03-15` (jaar +12026) en `-0500-03-15` (jaar −500).
`ref`'s `date_of_iso` (`ref/lib/value.ml:92`) eist exact vier/twee/twee
cijfers en cijfers alleen en weigert ze allebei — gedraaid in lens 2, niet
alleen uit de code gelezen. Precies de één-wire-twee-antwoorden-klasse
waarvoor de `dateTime`-klem is neergezet ("elke lezer houdt zijn eigen
hek", IR-SPEC ch. 4.1).

Daarnaast: waar deze routes wél weigeren, weigeren ze zonder context. De
`dateTime`-klem noemt de geweigerde waarde, het VELD waarop hij binnenkwam
en de verwachte vorm; `LocalDate.parse` gooit een kale
`DateTimeParseException` ("could not be parsed at index 5"), waar een
producent het veld niet uit kan aflezen. (`grensDatum` op r. 721 heeft die
context wél voor een ONTBREKENDE of niet-tekstuele datum, en verliest hem
één regel later voor een misvormde.)

Er is nog een vierde, DODE parse in dezelfde map: de `datumTijd`-decoder in
`engine/ParametersetLader.java:303` (kale `LocalDateTime.parse`) wordt
alleen vanaf r. 237 in diezelfde klasse aangeroepen, en die klasse heeft in
heel `backends/drools/src` geen enkele gebruiker (main noch test). Hij
leest bovendien een ANDER document (een parameterset, geen
`uchar-case-v0`-casewaarde), dus hij hoort niet bij de klemvraag — maar hij
moet wel benoemd worden, anders leest "de case-routes zijn geklemd" te
ruim. Deze kaart mag hem opruimen of expliciet laten staan; dat is een
tweede, kleine beslissing.

## De vraag die beslist moet worden

Is de tekenjaarvorm een echte producentvorm (dan: klem op alle drie de
routes, met een melding in dezelfde twee takken als de `dateTime`-klem), of
is hij zo onwaarschijnlijk dat de kale lezing met een reden mag blijven
staan? Voor r. 903 zijn beide antwoorden verdedigbaar. Voor r. 600 en
r. 721 ligt het zwaarder: daar is de gemeten uitslag een golden die stil
`PLAYED` meldt terwijl `ref` hem weigert, en dat is per definitie een
divergentie die de differentiaal niet ziet.

## Acceptance

- Het besluit staat in het Result, per route, met de meting erbij.
- Bij "klem": de klem staat er op elke geklemde route, weigert de gemeten
  vormen hard (nooit een `Excluded`), noemt waarde + veld + verwachte vorm,
  en krijgt een getuige in de bestaande witness-suite die rood-eerst is
  getoond — met de spiegelassertie erin (een vormtak die de
  kalenderformulering meedraagt moet vallen) en met de randbreedtes van
  beide kanten gepind, want een klem die alleen van bovenaf is vastgelegd
  laat een verruiming van het patroon ongezien door.
- Bij "geen klem": de reden staat als commentaar op de betreffende regel,
  en de kaart legt vast dat de vormen bekend en aanvaard zijn.
- De `ParametersetLader`-parse is opgeruimd of expliciet met reden gelaten.
- Alle dertien gates groen; gate 12's JUnit-teller stijgt bij "klem",
  en geen andere telling beweegt.

May touch: `backends/drools/**`, `backlog/`, en — alleen als het besluit dat
eist — `spec/schema/uchar-case-v0.schema.json` (protected: voorstel, niet
mergen).

## Out of scope

- De `dateTime`-getuige en zijn klem: die landden 2026-09-06/09 en zijn
  klaar.
- De datum-LITERAL-lezer (WF-084) — andere lezer, eigen regel, eigen
  getuige.
- Kale parses buiten de case-wire van deze backend. Let op: dit sluit NIET
  in dat de overige parses binnen `backends/drools` al gedekt zijn — ze zijn
  hier alleen niet geïnventariseerd. Deze kaart dekt de drie case-wire-
  routes hierboven plus de dode parse; wat er verder in de map staat is
  ongemeten.
