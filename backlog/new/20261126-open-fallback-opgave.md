# open-fallback-opgave: onderscheid tussen open en vaststaand leeg waar geen berekening geldt bij opgave (geparkeerd, optie)

*Geparkeerd op 2026-11-26 als optie uit de ontwerpverkenning. Voorkeursroute is eerst toetsen of een expliciet domeinkenmerk op het objecttype (bijv. `is beëindigd` / `is vastgesteld`) het geval in het regelmodel zelf al schoner en transparanter oplost; alleen ontparkeren wanneer modelleren met een kenmerk aantoonbaar tekortschiet.*

## Aanleiding

Bij `(mag opgegeven worden)` op een attribuut `X` markeert de RegelSpraak-frontend zowel het gegenereerde invoerattribuut `opgegeven X` als `berekende X` als `(volledig gegeven)` (`complete: true`) en bouwt zij op `X` de stapel `[opgegeven X, berekende X, (init)]`:
- Voor een gewone override boven een volledige beslisregel (zoals `het bergloon van een Vondst (mag opgegeven worden)` met `bergloon = 25 indien kostbaar`) moet `opgegeven X` inderdaad `complete: true` zijn: zonder opgave moet de berekening beslissen, en bij een niet-kostbare vondst waar geen rekenregel geldt, moet `bergloon` vaststaand `leeg` zijn ("geen recht op bergloon") in plaats van open te wachten op een opgave.
- Anders ligt het wanneer één attribuut op een algemeen type voor *sommige* gevallen een berekende regel heeft (bijv. een einddatum die bij één reden uit een ander feit volgt), maar voor *andere* gevallen géén rekenregel heeft en daar feitelijk verplichte handmatige invoer is waarbij *vaststaand leeg* een inhoudelijke betekenis heeft ("duurt nog voort") en *weggelaten* eigenlijk *open* ("nog niet ingevuld") zou moeten betekenen. Omdat zowel `opgegeven X` als `berekende X` bij niet-vurende regels vaststaand `leeg` zijn, sluit de wortel van de stapel daar nu naar vaststaand `leeg`.

## Waarom eerst in het regelmodel toetsen (voorkeur)

De engine kan aan `(mag opgegeven worden)` niet zien of "geen rekenregel geldt" betekent *"er is rechtens geen waarde"* (`bergloon` bij een niet-kostbare vondst) of *"voor dit geval ontbreekt een rekenregel en is invoer nodig"*. Met een expliciet kenmerk op het objecttype (bijv. `is afgelopen`, afgeleid waar een rekenregel geldt en anders een open invoerkenmerk) werkt dit vandaag al zonder taaluitbreiding, en krijgt zowel de uitleg als `?openInputs=true` een inhoudelijk begrijpelijk kenmerk terug.

## Als toch een taaluitbreiding gewenst blijkt (richting bij ontparkeren)

- Verander **niet** het standaardgedrag van `(mag opgegeven worden)` (dat zou elke gewone override bij weglaten blokkeren).
- Overweeg alleen een expliciete annotatie naast `(mag opgegeven worden)` (of een aparte defaults-worteleigenschap) waarbij een weggelaten `opgegeven X` de berekening `berekende X` wél doorlaat wanneer `berekende X` een waarde vult, maar de wortel van de stapel **open** laat wanneer `berekende X` niets vult én `opgegeven X` niet expliciet in `attributesEmpty` (`- de X is leeg` / `{"state": "empty"}`) is gesloten.
- Te bewijzen vóór bouw: dat deze vijf-standen-wortelregel onder zowel invullen van een waarde voor `opgegeven X`, sluiten met `attributesEmpty` op `opgegeven X`, als invullen van invoer onder `berekende X` strikt monotoon blijft (gate 5) en eenduidig verwoord wordt in `ConditionReading`, `DefaultsDecision` en `OpenReads`.
