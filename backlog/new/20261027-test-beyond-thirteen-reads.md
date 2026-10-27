# test-beyond-thirteen-reads: een voorwaarde mag meer dan 13 waarden lezen

*Op verzoek van de eigenaar (2026-10-27), na de vondst bij `readings-of-displaced-defaults`: de v2-groep
'duurverlenging bij-een-nieuw recht' leest precies 13 waarden, de grens van de exec-model-tak.*

## Aanleiding

De Drools-backend weigert een vorm waarvan de voorwaarde meer dan 13 gebonden waarden leest:
`TmsTranspiler.testItem` gooit DROOLS-264 (`TEST_ITEM_MANY_READS`, "de toets van deze tak neemt hoogstens 13 gebonden
waarden") boven `MAX_READS = 13` (`TmsTranspiler` r. 227). De grens komt uit de API die de exec-model-tak gebruikt:
`PatternDslRendering.test` (r. ±646–692) geeft elke gelezen waarde als eigen argument aan `DSL.expr(id, v1, …, vn,
PredicateN)`, en `drools-canonical-model` 8.44 kent functietypen tot en met `Predicate13`. Het is geen grens van de
semantiek (ref en clingo kennen haar niet) en niet van Drools als motor: DRL-tekst roept een Java-functie aan met elk
aantal argumenten. Omdat de analyse beide takken één plan geeft, weigert de DRL-teksttak dezelfde vormen.

Gemeten op de v2-regelset herleving-WW (werkbank, regels `ece012e`): de voorwaarde van
'defaults A:RechtOpUitkeringWwBijOntslag.duurverlenging bij-een-nieuw recht' leest er 13. Eén lezing erbij en uchar
weigert de groep; de werkbank kan dat nu alleen omzeilen door de voorwaarde in de regelset te splitsen (een
hulpkenmerk).

## Doel

De exec-model-tak geeft boven 13 de gelezen waarden aan de toets door in bundels (bijvoorbeeld per hoogstens 13 een
tussenbinding die een array maakt, en de toets over die bundels), zodat een voorwaarde elk aantal waarden mag lezen. De
DRL-teksttak volgt hetzelfde plan. De semantiek verandert niet; een toets met hoogstens 13 waarden wordt weergegeven
zoals nu. DROOLS-264 vervalt (de code blijft in het register met de soort `RETIRED`) of blijft alleen voor een grens die
de bouw dan nog heeft; de bouwer stelt dat vast.

## Acceptatie

- (a) Rood eerst: een golden (of een getuige in de suite) met een vorm waarvan de voorwaarde meer dan 13 waarden leest,
  een paar over een rol, met cases waarin zij geldt en niet geldt; ref en clingo spelen hem al, Drools weigert hem met
  DROOLS-264 op de code van nu.
- (b) Beide Drools-takken spelen hem met de gepinde verwachtingen; gate 12 `EXPECTED_PLAYED` gaat één omhoog; de build
  met lezingen leest hem ook (gate 17), de objecten van kaart `reading-read-objects` inbegrepen.
- (c) Een toets met hoogstens 13 waarden blijft gelijk: de productiefeiten en vuringen ongewijzigd
  (`ReadingRulesFixpointWitnessTest`, `ReadingRulesObserverWitnessTest`, `BranchDumpsCorpusWitnessTest`), en de DRL-tekst
  van de bestaande goldens bytegelijk.
- (d) De v2-regelset bouwt nog, met en zonder lezingen; `make gates` groen.

## May touch

`backends/drools/**` buiten de productieruntime (`PatternDslRendering`, `TmsTranspiler`, `DroolsCode` en de
catalogi), een golden, de tests van `backends/drools`; `scripts/check-drools.sh` (`EXPECTED_PLAYED`, de lijsten) als
voorstel; deze kaart.

## Out of scope

Een grens elders in de backend (het aantal patronen, een accumulate); de werkbank en de regelset.
