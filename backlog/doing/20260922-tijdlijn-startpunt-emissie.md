# tijdlijn-startpunt-emissie: "startend op" valt stil van de wire

Gemeten door de datumranden-kaart (2026-09-22, zusterranden-meting):
bij `voor elke week startend op <datum>` (§13.3.6.3) emitteert de
frontend alleen `WEEK`; het startpunt valt volledig weg, met of zonder
tijdcomponent. Stil informatieverlies — de verboden klasse — tenzij de
wire de vorm helemaal niet kent.

Bij oppakken EERST meten: draagt de v0-wire een startpuntveld (de
knoop `ResolvedStartpunt` bestaat — de naamtabel-kaart gaf hem het
naamvoorstel StartPointDetermination), en wat doen ref/clingo/spec met
een startpunt op een tijdlijn? Drie uitkomsten mogelijk: (1) de wire
draagt het veld en de emitter vult het niet → emitterfix + getuigen;
(2) de wire kent de vorm niet → frontend weigert de zin benoemd tot
een wire-kaart hem draagt (fail-fast boven stil verlies); (3) de spec
verklaart het startpunt betekenisloos voor de gedragen semantiek →
vastleggen met bron en een emittergetuige die het bewust weglaten
pint. De meting beslist; stil blijven mag niet.

## Acceptatie (aangescherpt bij oppakken, 2026-09-22)

- (a) Het dossier hieronder legt de meting vast — de v0-wire (schema,
  emitter), de spec (`IR-SPEC.md` ch. 2.8/3.8, `RUNTIME-SEMANTICS.md`
  ch. 3.2) en de uitvoerders (`ref`, clingo) — over ALLE vier de
  vaste-startpuntproducties van de grammatica (§13.3.6.3–.7: de datum-,
  maand-, kwartaal- en jaarvorm, inline op attribuut/kenmerk/parameter
  en in een benoemde Tijdlijn-definitie), niet alleen de weekvorm:
  het is één positie en één verliesmechanisme. De route volgt uit de
  meting, met motivering.
- (b) Rood-eerst: elke nieuwe frontendgetuige (JUnit onder
  `frontends/regelspraak/src/test`) faalt eerst tegen de oude code,
  letterlijke faaluitvoer in dit dossier; dan de fix; dan groen. Na de
  fix emitteert geen vast startpunt dat de wire niet kan dragen nog
  stil: `Resolver --ir` schrijft niets, exit 1, met een ERROR-diagnostic
  die de zin en de sectie noemt; `IrEmitter.emit` weigert dezelfde
  modellen. Waar het weglaten verliesvrij is (een startpunt dat samenvalt
  met de kalenderperiode), pint een emittergetuige het bewuste weglaten
  byte-voor-byte.
- (c) Mutatieproef: de fix teruggedraaid ⇒ de getuigen rood ⇒ fix terug
  ⇒ groen, in één keten uitgevoerd, uitvoer in dit dossier.
- (d) Alle veertien gates groen, elk als losse aanroep
  `bash scripts/check-<naam>.sh` na `eval $(opam env)`; gate 11 blijft
  groen zonder `toka/` te wijzigen.

May touch: `frontends/regelspraak/**`, `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (**voorstel**), `corpus/**`,
kaartverplaatsing.
