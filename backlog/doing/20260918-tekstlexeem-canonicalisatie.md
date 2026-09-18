# tekstlexeem-canonicalisatie: één vorm voor het v0-tekstliteral

Uit de tekstlexeem-kaart (2026-09-18): v0 draagt twee geldige
lexeemfamilies voor dezelfde tekstwaarde — rauw (`Flevoland`) en met
eigen buitenste aanhalingstekens (`"Flevoland"`) — omdat `waarde` per
IR-SPEC ch. 4.1 de onontlede bronstring is. De splitsing is
emittergedrag, geen corpus-toeval: `ExpressieBouwer.java:381` schrijft
de bronsnippet inclusief buitenste aanhalingstekens, waar
handgeschreven goldens rauwe lexemen dragen. Sinds de tekstlexeem-kaart
vergelijken alle vier consumenten verbatim, dus beide families spelen —
maar twee documenten met dezelfde bedoeling zijn niet byte-gelijk, en
een rgs-afgeleide golden met tekstliteral speelt alleen als zijn
case-invoer de quotes óók draagt.

Maintainersbesluit 2026-09-18 (genummerde vraag): **canonicaliseren
bij emissie** — de emitter schrijft voortaan de geparste waarde, niet
de bronsnippet met buitenste aanhalingstekens. Uitvoering direct na
het lopende zes-kaarten-pariteitsprogramma; gemandateerde kaart, geen
optie meer.

## Acceptatie (bij oppakken aanscherpen)

- (a) `ExpressieBouwer` schrijft voor `Tekst`-literalen de geparste
  waarde (buitenste delimiters weg, ingebedde tekens verbatim); de
  bronsnippet blijft via de `bron`-annotatie beschikbaar.
- (b) De geraakte `ir.json`-bestanden regenereren met verantwoorde
  byte-diff. Meting kaart tekstlexeem-lexeem (2026-09-18): geen
  publieke rgs-golden draagt een tekstliteral; de quote-omsloten
  vormen zitten in de `toka/`-zone. Bijbehorende quote-dragende
  casewaarden bewegen mee — meten bij oppakken.
- (c) Aandachtspunt gate 11: canonicaliseren is een bewuste
  emissie-divergentie van de toolchain (zoals besluit 16Y bij
  toka-45); de gepinde `toka/**/ir.json` regenereren in dezelfde
  kaart en de divergentie documenteren in het gate-script-commentaar
  dan wel de holdout-verantwoording (beschermd-padvoorstel indien
  scripttekst wijzigt).
- (d) `spec/CONSTRUCTS.md`-regel (registervoorstel in het Result) en
  de IR-SPEC ch. 4.1-consequentie meenemen.
- (e) Alle veertien gates groen; de emitter is vendored slice, dus
  toolchain-herpin na de merge — te combineren met de
  programma-eindherpin.
