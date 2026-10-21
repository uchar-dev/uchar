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
