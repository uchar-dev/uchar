# reading-read-objects: de lezing noemt bij een gelezen lid het object waarop zij het las

*Voorwaarde voor deel 1 van de toolchain-kaart `explanation-read-objects-and-displaced` (van de werkbank, op besluit
van de eigenaar, 2026-10-27). Ontwerp: Fable-advies in `uchar-dev/research/uitleg-gelezen-objecten-2026-10-27/`.*

## Aanleiding

De uitleg van de toolchain zet bij elke waarde die een voorwaarde of een berekening las, de waarde uit de lezing van
RUNTIME-SEMANTICS ch. 5.12 (`DroolsBackend.readingsOf`). De werkbank wil bij elke gelezen waarde de zin vinden die de
schrijver van die waarde uitlegt; die zin begint met het object en de naam van het lid. Bij een lezing van een eigen lid
is dat object het object van de lezing. Bij een lezing over een rolpad ("de herlevingsgrens van het recht van het
Inkomen", "de jutter van de Vondst ingezetene is") is het het object dat het pad bereikte, en dat staat nergens: een
knoop van ch. 5.12 draagt een oordeel of een waarde met eenheid, en bij een vouw zijn bijdragers, maar geen object
(`ConditionReading.NodeReading`). De toolchain heeft geen navigatiesemantiek van zichzelf (haar `CLAUDE.md`), dus kan
zij het pad niet zelf lopen.

Gemeten op de vijf service-verzoeken van v2/herleving-ww en s01 (toolchain `b4f3ab94`, uchar `c6a45d89`, regels
`ece012e`; `werk/toolchain-2026-10-27/read-objects/meting/`): 224 attribuutwaarden over een pad (155 in een voorwaarde,
69 in een berekening) en 17 kenmerkchecks over een rol, naast 425 lezingen van een eigen lid.

## Doel

Ch. 5.12: een knoop die een lid van één object leest — een attribuutlezing, ook over een pad, en een kenmerkcheck, ook
over een rol; de lijst van soorten stelt het ontwerp vast — en die de evaluatie bereikte, noemt het object waarop zij het
lid las. Waar een link op het pad ontbreekt, is er geen object (de lezing is leeg, ch. 5.8 regel 1). De canonieke tekst
schrijft het, ref (`runner --trace`) en de Drools-backend (beide takken, de build met lezingen) leveren het, en gate 17
vergelijkt het. `ConditionReading.NodeReading` draagt het als bean, zoals een bijdrager zijn lid draagt.

Een knoop die per lid van een vouw gelezen wordt, heeft geen eigen lezing en blijft zo; de bijdragers van een vouw
noemen hun leden al.

## Acceptatie

- (a) Rood eerst: rijen in de getuigen van de lezingen (ref `ref/test/`, Drools `ReadingsWitnessTest` of een eigen
  klasse) voor een eigen lid, een pad van één stap, een pad van meer stappen, een afwezige link, een kenmerkcheck over
  een rol, een lezing in de waarde van een berekening, en een pad over een relatie die een creatie legt; elk faalt op de
  code van nu.
- (b) Gate 17 vergelijkt het object per knoop tussen ref en beide takken; een getuige over het corpus houdt het object
  vast aan het fixpunt (het lid op het genoemde object heeft daar de waarde van de knoop). De gemeten aantallen in het
  Result; vloeren zijn ondergrenzen.
- (c) De clausule in ch. 5.12 als voorstel, met een gewerkt voorbeeld met absolute waarden.
- (d) De productiebuild bytegelijk (`ReadingRulesFixpointWitnessTest`); `make gates` groen.

## Voorlopige besluiten (hoofdsessie, 2026-10-27, naar het Fable-advies)

*Gemarkeerd voorlopig; de eigenaar kan elk besluit omdraaien. Bron: `research/uitleg-gelezen-objecten-2026-10-27/
fable-advies.md` §1, §5, §6.*

- **D1 Route.** Het object hoort in ch. 5.12 (de canonieke tekst, ref, beide Drools-takken, gate 17), niet alleen als
  veld in de Drools-API. `DroolsBackend.reached` (dat bestaat, maar geen kenmerkcheck neemt en beans na de
  terugschrijving leest) blijft de getuige-API van `ReadingsCorpusWitnessTest`, met een overload voor een kenmerkcheck.
- **D2 Welke knopen.** `AttributeReference` (eigen lid, over een pad, als tijdlijn gelezen, onder een
  `DimensionCellRead`) en `CharacteristicCheck` (eigen lid, over een rol, onder de geheel-periodekwalificatie). Niet
  `RoleMembership`, `IdentityCheck`, een parameter, een binder, een vouwknoop of een knoop zonder eigen lezing.
- **D3 Eigen lid.** `NodeReading.object` draagt altijd het object (bij een eigen lid het object van de lezing); de
  canonieke tekst schrijft het alleen waar de knoop navigeert, zodat geen bestaande regel en geen van de drie
  uitgewerkte voorbeelden verandert.
- **D4 Afwezige link.** De tekst ` on none`, de API null; geen aparte enum. Een knoop die niet bereikt is of een fout
  draagt, noemt geen object.
- **D11 Productiebuild.** `PathState` krijgt een component `reached` in beide builds (de padstand-DRL één argument
  meer); de getuigen houden feiten en vuringen gelijk (`ReadingRulesFixpointWitnessTest`,
  `ReadingRulesObserverWitnessTest`, `BranchDumpsCorpusWitnessTest`). Geen aparte feitfamilie in de build met lezingen.

De canonieke tekst: `<form> <object> <number> <kind> <found> ["<unit>"] on <object>|none`, alleen bij een knoop die
navigeert. Gewerkt voorbeeld: `anchor/jutterswet-kustwacht` case 2 (advies §1.6).

## May touch

`backends/drools/**` buiten de productieruntime, `ref/lib/reading.ml` en wat het trace-pad van ref raakt, `ref/test/`,
een golden; `spec/RUNTIME-SEMANTICS.md` en `scripts/check-readings.sh` als voorstel; deze kaart.
