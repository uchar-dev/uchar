# drools-enumnaam-identiteit: condities vergelijken enumlabels nog byte-exact

Gemeten bij kaart `20260920-drools-enumlabel-lezer` (dossier §"Niet
gebouwd, wel gemeten"): in CONDITIES vergelijken beide Drools-takken
enumlabels byte-exact waar `ref` (`Checks.norm`) en de ASP-backend
(genormaliseerde opslag) naamidentiteit hanteren — een enumlabel is een
gedeclareerde naam. De plekken: de twee literal-conditie-arms en de
assertie-arm van `ExecModelTranspiler` (`rhs.equals(...)`) en de
literal-verlaging van `DrlTranspiler` die naar een mvel-`==` rendert.
De casusvergelijker is bij die kaart al op naamidentiteit gezet; de
conditiekant bleef bewust staan.

Geen corpusdrager: precies één enumwaarde in de hele zone wijkt van
haar declaratie af (`jutterswet-keurstaat`, casusinvoer) en haar
vergelijkingspartner verschilt onder beide lezingen, dus geen document
kan het verschil vandaag tonen. Optrekken raakt vier arms plus de
geëmitteerde mvel en de uniciteitssleutels, en wijkt af van de
productiesemantiek — dat is een NORMBESLUIT (volgt de conditielezing de
naamidentiteit van ref/clingo, of blijft byte-exact de Drools-lezing
met een genormeerde divergentie?), geen techniekkeuze.

## Acceptatie (bij oppakken aanscherpen)

- (a) De normvraag als genummerde maintainersvraag met de meting erbij
  (een verifier-schoon document met een norm-afwijkend gespelde
  enumvergelijking splijt de uitvoerders — construeer hem als bewijs).
- (b) Bij naamidentiteit: de vier arms + mvel-emissie + getuigen in
  beide takken, mutatieproef, en de productiesemantiek-divergentie in
  de eerstvolgende herpin-conformance-notitie.
- (c) Bij byte-exact-als-norm: de spec-zin die de divergentie draagt en
  een negatief/rood-getuige die haar pint.
- (d) Alle veertien gates groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh`
(**voorstel**), `spec/**` (**voorstel**), `corpus/**`,
kaartverplaatsing. NB: serieel met andere Drools-kaarten.
