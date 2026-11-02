# dimensions-remaining: wat de kaart dimensions-executed bij naam liet liggen

*Vastgelegd door de hoofdsessie op 2026-11-02 bij de landing van `20261102-dimensions-executed` (Result, "Wat buiten de kaart
blijft"). Elk punt is nu een weigering of grens met naam; geen stille afwijking.*

Goal: elke vorm van een dimensie die RegelSpraak of ALEF schrijft, draait in ref, clingo en beide Drools-branches, of is een
gedocumenteerde normkeuze.

## Punten

1. **`eerste`, `laatste`, `aantal` (en `filledCount` met een selectie) over cellen.** Normkeuze vooraf: de typering van de
   RegelSpraak-specificatie beperkt ze tot instanties en datums, ALEF kiest positioneel (de eerste of laatste gevulde cel in
   labelvolgorde; `Vectorspace.java:195-201`). Nu weigert WF-037 ze. Advies §4 van `werk/dimensions-executed/fable/advies.md`:
   `filledCount` met selectie is de goedkoopste terugkeer.
2. **Een cel of vouw over een rol.** Ref en clingo lezen een cel over een pad; Drools weigert het lezen (DROOLS-189) en een celdoel
   met een pad (DROOLS-100); de frontend schrijft de vorm niet (`grammar/DIVERGENCES.md`, "Scope / limitations"); ALEF heeft het
   (`EenOpMeer_AggregatieDimensies`). Een golden met bron.
3. **Een vouw over een geschreven cel van de onbegrensde vorm** in Drools (DROOLS-296, ruim: ook waar de waarde eindig is); valt
   samen met `20261102-drools-exact-arithmetic` (`ExactSum`).
4. **Het paar initialisatie plus berekening op één cel**: ref en Drools antwoorden in de defaults-lezing, clingo weigert met naam;
   geen corpusgeval en geen Drools-getuige.
5. **Een cel van een ander type dan getal** in clingo (een symbolische cellezing valt op CLINGO-002) en de `emptyFallback` van een
   dimensiesom in clingo (alleen 0).
6. **Frontend**: labels vóór het attribuut met "en"; een cel met "en" of "," in een operandenlijst (splitst nu, RGS-364 met
   waarschuwing); labels achter het onderwerp (RGS-308); een attribuut met de naam van een eenheidswoord na een lidwoord; de
   scenariolaag leest geen cel (RGS-237, gate 16).

Acceptance (bij oppakken aanscherpen): per punt rood eerst, goldens met absolute verwachtingen in het vierkant, of een
normbesluit met een WF-weigering; `make gates` groen.

May touch: zoals `20261102-dimensions-executed`.
