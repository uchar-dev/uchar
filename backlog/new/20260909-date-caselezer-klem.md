# De date-tak van de Drools-case-lezer heeft geen klem

Goal: beslissen of de `date`-kant van de case-wire dezelfde patroonklem plus
getuige krijgt die de `dateTime`-kant op 2026-09-06/09 kreeg, of dat de
huidige kale lezing met een reden blijft staan. De kaart levert het besluit
plus, als het besluit "klem" is, de klem en zijn getuige.

## De stand (gemeten 2026-09-09)

`CaseRunner.waardeNaarArg` r. 903 leest een `date`-waarde met een kale
`LocalDate.parse(...)`. Daar staat, anders dan één regel lager op r. 904,
geen patroonklem en geen eigen melding voor.

Twee dingen zijn gemeten, niet beredeneerd:

- **De vormklasse is smal maar niet leeg.** `LocalDate.parse` weigert de
  meeste vormen die de wire ook weigert (smal veld `2026-3-15`, zonesuffix
  `2026-03-15Z`, basisformaat `20260315`, omringende spatie, een
  `dateTime`-lexeem, en de kalender: `2026-02-30`). Het accepteert wél twee
  vormen die het schemapatroon
  (`^[0-9]{4}-[0-9]{2}-[0-9]{2}$`, `uchar-case-v0`) uitsluit: het ISO
  uitgebreide jaar met teken — `+012026-03-15` wordt jaar +12026 en
  `-0500-03-15` wordt jaar −500.
- **Daar loopt de lezersvergelijking uiteen.** `ref`'s `date_of_iso`
  (`ref/lib/value.ml:92`) eist exact vier/twee/twee cijfers en cijfers
  alleen, dus beide vormen falen daar luid. Gelezen uit de code, niet
  gedraaid; een `case*.json` met zo'n waarde komt gate 4 niet door, dus de
  divergentie kan alleen via een andere route binnenkomen — precies de route
  waarvoor de `dateTime`-klem is neergezet ("elke lezer houdt zijn eigen
  hek", IR-SPEC ch. 4.1).

Daarnaast: waar de date-tak wél weigert, weigert hij zonder context. De
`dateTime`-klem noemt de geweigerde waarde, het VELD waarop hij binnenkwam
en de verwachte vorm; `LocalDate.parse` gooit een kale
`DateTimeParseException` ("could not be parsed at index 5"), waar een
producent het veld niet uit kan aflezen.

Er is nog een derde lezer in het spel die geen enkele route heeft: de
`datumTijd`-decoder in `engine/ParametersetLader.java:303` (kale
`LocalDateTime.parse`) wordt alleen vanaf r. 237 in diezelfde klasse
aangeroepen, en die klasse heeft in heel `backends/drools/src` geen enkele
gebruiker. Die parse leest bovendien een ANDER document (een parameterset,
geen `uchar-case-v0`-casewaarde). Hij hoort dus niet bij de klemvraag, maar
hij moet wel benoemd worden, anders leest "alle routes zijn geklemd" te
ruim. Deze kaart mag hem opruimen of expliciet laten staan; dat is een
tweede, kleine beslissing.

## De vraag die beslist moet worden

Is de tekenjaarvorm een echte producentvorm (dan: klem, met een melding in
dezelfde twee takken als de `dateTime`-klem), of is hij zo onwaarschijnlijk
dat de kale lezing met een reden in de code mag blijven staan? Beide
antwoorden zijn verdedigbaar; wat er niet mag zijn, is dat het onbeslist
blijft terwijl één regel hoger het tegenovergestelde besluit staat.

## Acceptance

- Het besluit staat in het Result, met de meting erbij.
- Bij "klem": de klem staat er, weigert de twee gemeten vormen hard (nooit
  een `Excluded`), noemt waarde + veld + verwachte vorm, en krijgt een
  getuige in de bestaande witness-suite die rood-eerst is getoond — met de
  spiegelassertie erin (een vormtak die de kalenderformulering meedraagt
  moet vallen), want zonder die assertie ziet de getuige zo'n melding niet.
- Bij "geen klem": de reden staat als commentaar op r. 903, en de kaart legt
  vast dat de twee vormen bekend en aanvaard zijn.
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
- Elke andere kale parse buiten `backends/drools`.
