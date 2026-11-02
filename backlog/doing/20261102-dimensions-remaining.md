# dimensions-remaining: wat de kaart dimensions-executed bij naam liet liggen

*Vastgelegd door de hoofdsessie op 2026-11-02 bij de landing van `20261102-dimensions-executed` (Result, "Wat buiten de kaart
blijft"). Elk punt is nu een weigering of grens met naam; geen stille afwijking.*

Goal: elke vorm van een dimensie die RegelSpraak of ALEF schrijft, draait in ref, clingo en beide Drools-branches, of is een
gedocumenteerde normkeuze.

## Punten

1. **`eerste`, `laatste`, `aantal` (en `filledCount` met een selectie) over cellen.** *Besluit Yeb 2026-11-02: bouwen zoals ALEF,*
   positioneel: `eerste` en `laatste` zijn de eerste en laatste gevulde cel in de labelvolgorde van de selectie, `aantal` het aantal
   gevulde cellen (ALEF `Vectorspace.java:195-201`, `tel`; test met cellen 1, leeg, 0, 0, 6, 7: eerste 1, laatste 7). Een bewuste
   afwijking van de typering van de RegelSpraak-specificatie (die beperkt ze tot datums en instanties), vast te leggen in CONSTRUCTS
   rij 28. WF-037 laat ze dan toe op een selectie; uchars eigen `first`/`last` over instanties blijven op waarde.
2. **Een cel of vouw over een rol.** Ref en clingo lezen een cel over een pad; Drools weigert het lezen (DROOLS-189) en een celdoel
   met een pad (DROOLS-100); de frontend schrijft de vorm niet (`grammar/DIVERGENCES.md`, "Scope / limitations"); ALEF heeft het
   (`EenOpMeer_AggregatieDimensies`). Een golden met bron.
3. **Een vouw over een geschreven cel van de onbegrensde vorm** in Drools (DROOLS-296, ruim: ook waar de waarde eindig is); valt
   samen met `20261102-drools-exact-arithmetic` (`ExactSum`).
4. **Het paar initialisatie plus berekening op één cel**: ref en Drools antwoorden in de defaults-lezing, clingo weigert met naam;
   geen corpusgeval en geen Drools-getuige.
5. **Een cel van een ander type dan getal** in clingo (een symbolische cellezing valt op CLINGO-002) en de `emptyFallback` van een
   dimensiesom in clingo (alleen 0).
6. **Frontend.** Te bouwen: de scenariolaag leest een cel (RGS-237, gate 16), zodat een toetsdocument cellen als invoer en
   verwachting schrijft. Blijven als gedocumenteerde grens (`grammar/DIVERGENCES.md`; de taal is daar dubbelzinnig: "en" scheidt een
   opsomming en verbindt labels), voorlopig besluit van de hoofdsessie: labels vóór het attribuut met "en"; een cel met "en" of ","
   in een operandenlijst (RGS-364 met waarschuwing); labels achter het onderwerp (RGS-308, niet de vorm van ALEF); een attribuut
   met de naam van een eenheidswoord na een lidwoord.

## Stappen

- **A** (punten 1, 4, 5): `eerste`/`laatste`/`aantal` over cellen in spec, verifier, ref, clingo, Drools en frontend; het paar
  initialisatie plus berekening op één cel in clingo; een cel van een ander type dan getal en de `emptyFallback` van een
  dimensiesom in clingo.
- **D** (punt 6, scenariolaag), parallel aan A.
- **B** (punt 2, cel over een rol in Drools en frontend, golden), na A.
- Punt 3 gaat mee met `20261102-drools-exact-arithmetic`.

Acceptance (bij oppakken aanscherpen): per punt rood eerst, goldens met absolute verwachtingen in het vierkant, of een
normbesluit met een WF-weigering; `make gates` groen.

May touch: zoals `20261102-dimensions-executed`.
