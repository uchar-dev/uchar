# ref-caselabel-datumfout: de misvormde inputdatum meldt veld noch fase

Gemeten door de ref-geldigheid-kaart (2026-09-23, op
jutterswet-bergingsklok): een misvormde `date`-waarde in de
case-INPUT wordt luid geweigerd maar verkeerd gelabeld —
`EVALUATION FAULT: malformed date lexeme "10-03-2026"` zonder
attribuutnaam en zonder laadfase, waardoor de eigen
`malformed %s date`-checks van de caselader onbereikbaar zijn. Nooit
een stil fout antwoord, wel een melding die de auteur laat zoeken.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer; traceer waarom de laderzin onbereikbaar is (welke
  lezer wint) en welke posities hetzelfde hebben.
- (b) De weigering draagt veld, waarde en fase (laadtijd); de dode
  laderchecks gaan werken of verdwijnen (fail-fast, geen dode armen).
  Rood-eerst suitegetuige; mutatieproef.
- (c) Corpussweep: geen replay beweegt; alle veertien gates groen.

May touch: `ref/**`, kaartverplaatsing. NB: serieel met andere
ref-caselaagkaarten.
