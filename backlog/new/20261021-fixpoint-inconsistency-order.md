# fixpoint-inconsistency-order: de inconsistenties van het fixpunt in een vaste volgorde

*Vondst van de toolchain-kaart `explanation-from-readings` (2026-10-21), nagelopen door de hoofdsessie op uchar
`15f39170`. Geen fout antwoord: de verzameling klopt, alleen de volgorde wisselt.*

## Aanleiding

`TmsTranspiler` (de lezing van het fixpunt, r. ±955–967) sorteert de fouten op de naam van de vorm en de zin, maar geeft
de §9.5-deposito's door in de volgorde van het werkgeheugen: `session.getObjects(…)` over de beans van `Inconsistentie`,
zonder sortering. `Fixpoint.inconsistencies` belooft geen volgorde. De dienst van de toolchain schrijft die lijst
ongewijzigd in het antwoord (`AnswerBuilder`, `inconsistencies` en `inconsistencyDetails`). Gemeten in de toolchain: op
dezelfde productiebuild gaf `jutterswet-keurspelling` tussen twee verzoeken een andere volgorde, en over de 231 goldens
van de replay verschilde de volgorde in 13 tussen twee sessies. De replay-leg van de uitleg vergelijkt de
inconsistenties daarom zonder volgorde.

## Doel

`Fixpoint.inconsistencies` in een vaste volgorde, zoals `faults`: op de regel, dan op het onderwerp (het label van een
invoerobject, de naam van een gemaakt object zoals de dump hem schrijft). De javadoc van `Fixpoint` noemt de volgorde.
`CaseRunner.inconsistencies` en `SessionDump` volgen dezelfde volgorde of laten zien waarom ze er niet van afhangen.
Nagaan of `objects` en `links` hetzelfde probleem hebben voor een gebruiker die ze in volgorde uitschrijft; zo ja, in
dezelfde kaart, met dezelfde toets.

## Acceptatie

- (a) Rood eerst: een getuige die een golden met meer dan één inconsistentie twee keer vuurt in verse sessies, met
  verschillende invoegvolgorde van de invoerobjecten, en dezelfde lijst eist; rood op `15f39170`.
- (b) De Drools-suite en gate 12 groen; de productiebuild verder ongewijzigd.
- (c) In de toolchain, bij de volgende pin-bump: de replay-leg vergelijkt de inconsistenties weer in volgorde.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/` (`TmsTranspiler`, `Fixpoint`),
`cases/CaseRunner.java`, een getuigetest, deze kaart.

Out of scope: de productieruntime; de volgorde van de waarden en kenmerken (die gaan naar de beans en het antwoord
leest ze per object).
