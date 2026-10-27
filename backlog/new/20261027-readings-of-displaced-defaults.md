# readings-of-displaced-defaults: een verdrongen default krijgt de lezing van zijn voorwaarde en zijn waarde

*Voorwaarde voor deel 2 van de toolchain-kaart `explanation-read-objects-and-displaced` (van de werkbank, op besluit
van de eigenaar, 2026-10-27). Ontwerp: Fable-advies in `uchar-dev/research/uitleg-gelezen-objecten-2026-10-27/`.
Start na de landing van `reading-read-objects` (beide raken ch. 5.12, ref en de lezingen van Drools).*

## Aanleiding

De uitleg van een defaults-groep noemt de varianten na de beslisser alleen bij naam ("verdrongen: 'herleving per
kalendermaand art. 21 lid 1 na onderdeel c', …"). De werkbank wil per verdrongen variant weten of haar voorwaarde gold
en, zo ja, welke waarde zij gaf (bij een attribuut) of wat zij besliste (bij een kenmerk): het antwoordmodel van een
vraag noemt "herleving per 01-07-2025 was mogelijk geweest", de waarde van zo'n variant.

Gemeten (2026-10-27): geen motor rekent dat uit.

- De DRL-verlaging bewaakt een lagere default met de beurt vóór zijn toets: in de DRL van het model van de
  toolchain-test `ExplanationDefaultsTest` heeft `defaults A:Jutter.premie: default 1` (de basis 'premie') in zijn
  linkerkant `not( Decides( stack == "defaults A:Jutter.premie" && index < 1, subject == $onderwerp ) )` vóór
  `eval( toets_… )`, en zijn waarde staat alleen in het gevolg.
- Ch. 5.12: "A form that is not at its turn, or outside its validity, is not evaluated, so it has no nodes. Every
  default below the one that decides is displaced, and with it every default inside its exception groups." De lezing van
  zo'n default is `DISPLACED` zonder knopen.
- De dienst en de playground van de toolchain lezen beide de build met lezingen van uchars Drools-backend
  (`DecisionCore`, `Playground`); een eigen motor hebben zij niet.

## Doel

Ch. 5.12: een default die niet aan de beurt was omdat een hogere default van zijn keten besliste of een
uitzonderingsgroep van hem uitkwam (ch. 7.1, 7.9), en een gewone regel die ch. 7.10 in een stapel opnam en daar zo werd
verdrongen, krijgt de evaluatie van zijn voorwaarde en zijn waarde op de fixpuntwereld, zoals een vorm die aan de beurt
is; dat hij niet aan de beurt was, blijft te lezen. Een initialisatie die de invoer of een andere schrijver verdrong, een
creatie van de 1:1-vorm en een vorm buiten zijn geldigheid blijven zoals ze zijn. ref en de Drools-backend (beide
takken) leveren het, gate 17 vergelijkt het; de lezing verandert niets aan wat de sessie berekent.

## Acceptatie

- (a) Rood eerst: rijen in de getuigen van de lezingen voor een verdrongen attribuutdefault die gold en een waarde gaf,
  een die gold en leeg gaf, een die niet gold, een verdrongen kenmerkdefault, een default in een uitzonderingsgroep onder
  een verdrongen default, en een gewone regel in een stapel (ch. 7.10); elk faalt op de code van nu.
- (b) Gate 17 vergelijkt de nieuwe lezingen; de gemeten aantallen in het Result; vloeren zijn ondergrenzen.
- (c) De clausule in ch. 5.12 als voorstel, met een gewerkt voorbeeld met absolute waarden.
- (d) De productiebuild bytegelijk (`ReadingRulesFixpointWitnessTest`); `make gates` groen.

## Voorlopige besluiten (hoofdsessie, 2026-10-27, naar het Fable-advies)

*Gemarkeerd voorlopig; de eigenaar kan elk besluit omdraaien. Bron: `research/uitleg-gelezen-objecten-2026-10-27/
fable-advies.md` §2, §5, §6.*

- **D5 Vorm.** `DISPLACED` mét knopen: de status zegt dat de default niet aan de beurt was, de knopen wat hij vond. Geen
  nieuwe status en geen extra veld. De rij `DISPLACED` van de statustabel splitst in twee rijen (met en zonder knopen).
  WF-051 maakt de lezing op de fixpuntwereld gelijk aan de speculatieve evaluatie van ch. 7.1.
- **D6 Welke vormen.** Elke default onder de beslisser in zijn keten, aan de wortel of in een uitzonderingsgroep, met
  wat hij in zijn uitzonderingsgroepen draagt; de eigen inhoud van een default waarvan een uitzonderingsgroep besliste
  of door een conflict settelde; een gewone regel die ch. 7.10 in de stapel vouwde, onder de beslisser; de
  initialisatiedefault (ch. 7.3) onder een hogere beslisser. Niet: een initialisatie die de invoer of een andere
  schrijver verdrong, een 1:1-creatie, een vorm buiten zijn geldigheid, een groep-default zonder eigen inhoud, een
  kenmerkdefault zonder rechtvaardiging, een lid van een ongeordende zustergroep (dat is aan de beurt).
- **D7 Fout.** Een fout in de voorwaarde of de waarde van een verdrongen default staat op haar knopen, de status blijft
  `DISPLACED`, een deponering valt weg.
- **Bouw.** Drools: naast de leesregel van een bewaakte default een speculatieve leesregel zonder de controlepatronen
  (`Reading.kind`), zodat de beurt bij truth maintenance blijft; ref: `read_on` in `displace`. Gewerkte voorbeelden:
  `anchor/jutterswet-nestuitzondering` case 1 en `anchor/jutterswet-vondstwaardering` (advies §2.5).

## May touch

`backends/drools/**` buiten de productieruntime, `ref/lib/reading.ml` en wat het trace-pad van ref raakt, `ref/test/`,
een golden; `spec/RUNTIME-SEMANTICS.md` en `scripts/check-readings.sh` als voorstel; deze kaart.
