# readings-of-unconditional-forms: een vorm zonder voorwaarde krijgt een lezing van haar waarde

*Uit het Fable-advies van 2026-10-22 over de uitleg van berekende waarden
(`uchar-dev/research/uitleg-berekende-waarden-2026-10-22/fable-advies.md`, §1.2, §3, §5 U4; beslispunt 3: voorlopig
besluit van de hoofdsessie naar dat advies). Voorwaarde voor de toolchain-kaart `explanation-computed-attributes`.*

## Aanleiding

RUNTIME-SEMANTICS ch. 5.12 legt voor een vorm met een voorwaarde de lezing vast, en daarin ook de knopen van de waarde
(tussenwaarden, de bijdragers van een vouw met hun bijdrage, de eenheid per knoop). Een vorm zonder voorwaarde heeft er
geen ("A form without a condition has none either", r. ±5080): de Drools-backend plant de leesregel van een regel op een
attribuut alleen met een test (`TmsTranspiler` r. ±1670, een default r. ±3285), en ref neemt alleen `(Some condition,
Some objective)` (`ref/lib/reading.ml` r. ±348–352). Een `Regel … moet gesteld worden op …` zonder `indien` heeft dus
geen waarde van haar operanden en geen bijdragers van haar vouw. In de v2-regelset herleving-WW raakt dat drie van de
17 uitkomsten zonder uitlegzin (het maandloon van het recht; de eerste kalendermaand met overschrijding; de eerste
kalendermaand zonder overschrijding na-de-eindiging) en de initialisatie van de maximeringsfactor.

## Doel

Een vorm zonder voorwaarde krijgt een lezing: de status is `HOLDS` wanneer de vorm aan de beurt is, met de knopen van
haar waarde zoals bij een vorm met voorwaarde. Ch. 5.12 voegt dat toe en herschrijft niets voor vormen met voorwaarde.
ref (`--trace`) en de Drools-backend (beide takken, de build met lezingen) leveren haar; gate 17 vergelijkt haar.

## Acceptatie

- (a) Rood eerst: een `ReadingsWitnessTest`-rij voor een regel `gesteld worden op` zonder `indien` (verwacht een
  `HOLDS` met de waarde van de lezing) faalt op de code van nu; gate 17 op een golden met zo'n regel.
- (b) De rijen groen op beide takken; `ReadingsCorpusWitnessTest` en gate 17 groen, met de gemeten aantallen in de
  Result (vloeren zijn ondergrenzen: geen verhoging nodig, wel noemen); de productiebuild bytegelijk
  (`ReadingRulesFixpointWitnessTest`); `EXPECTED_PLAYED` ongewijzigd tenzij een nieuwe golden; de zeventien gates groen.
- (c) De spec-clausule in ch. 5.12 als voorstel, met een gewerkt voorbeeld.

May touch: `backends/drools/**` buiten de productieruntime, `ref/lib/reading.ml`, `ref/test/`, een golden; `spec/` en
`scripts/` als voorstel; deze kaart. Start na de landing van `cycle-across-instances` (beide raken Drools, ref en spec).
