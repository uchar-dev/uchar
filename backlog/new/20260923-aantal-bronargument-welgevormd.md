# aantal-bronargument-welgevormd: mét elementtype, zonder bron, niemand voert uit

Gemeten door de aantal-zonder-bron-kaart (2026-09-23, open punt 1):
een `AANTAL` MÉT elementtype maar ZONDER bron-argument (ariteit 0, of
één `ListLiteral` als argument) is verifier-schoon, en alle vier de
uitvoerders weigeren hem — ref met een bodemzin. Er is geen kloof
tussen de uitvoerders, maar wel een welgevormd document dat niemand
kan uitvoeren: een groene deelverzameling die geen groen is. De
WF-037-arm van die kaart dekt alleen de vorm ZONDER elementtype; deze
tweeling (elementtype gezet, bron ontbreekt) viel buiten het besluit.

## Acceptatie (bij oppakken aanscherpen)

- (a) Meet de vormen (ariteit 0 met elementtype; ListLiteral met
  elementtype) op alle vier; leg de bodemzin-route vast.
- (b) Normkeuze: de verifier eist een bron-argument bij een
  elementtype (WF-uitbreiding, negatief per spelling) — de lijn van
  de zusterbesluiten — of de vorm krijgt een lezing. Weigering ligt
  in lijn; motiveer verbatim.
- (c) Rood-eerst per negatief; mutatieproef; refs bodemzin wordt een
  benoemde arm of een invariantnet (fail-fast).
- (d) Alle veertien gates groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**),
`corpus/negatief/**`, `ref/**`, kaartverplaatsing.
