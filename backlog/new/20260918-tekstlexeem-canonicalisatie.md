# tekstlexeem-canonicalisatie: één vorm voor het v0-tekstliteral (optie)

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

De keuze — canonicaliseren bij emissie, of de dubbelheid expliciet in
de spec vastleggen — hoort bij de v1-typed-value-delta en is een
maintainersbesluit; deze kaart is de optie, geen mandaat.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een besluit, als genummerde vraag aan de maintainer: emissie
  canonicaliseren (welke vorm wint), of de dubbelheid als spec-zin
  vastleggen met de verbatim-vergelijkplicht erbij.
- (b) De uitwerking volgt het besluit: emitterwijziging met
  byte-diff-migratie van de geraakte `ir.json`-bestanden, óf een
  IR-SPEC ch. 4.1-verduidelijking; in beide gevallen een
  `spec/CONSTRUCTS.md`-regel (registervoorstel in het Result).
- (c) Alle veertien gates groen; gate 10/11-byte-identiteit bewust
  behandeld (een emitterwijziging raakt de vendored slice en vergt de
  toolchain-herpin).
