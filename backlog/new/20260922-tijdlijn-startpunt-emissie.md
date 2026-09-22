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

## Acceptatie (bij oppakken aanscherpen)

- (a) De meting (wire, spec, uitvoerders) in het dossier; de gekozen
  route met motivering.
- (b) Rood-eerst getuigen voor de gekozen route; geen stil verlies
  meer op deze positie.
- (c) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `spec/**` (**voorstel**),
`corpus/**`, kaartverplaatsing.
