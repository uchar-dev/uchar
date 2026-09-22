# ref-geldigheid-datumlezing: asOfDate en geldigheid als opake strings vergeleken

Gemeten door de lezerszone-lens (2026-09-23): `Eval.applies` in ref
vergelijkt `asOfDate` en `geldigheid.vanaf`/`tot` LEXICOGRAFISCH als
ondoorzichtige strings, zonder de datumlezer. Gevolg, gemeten op
vensterheffing met `asOfDate "01-06-2026"` (dag-eerst): ref en clingo
antwoorden `overgangsheffing = 140` waar `leeg` hoort, zonder melding;
Drools geeft daar een ERROR. Een misvormde `geldigheid.vanaf` passeert
ref en clingo eveneens. Het hek is vandaag het schemapatroon (gates 2
en 4) — de corpuszones dragen alleen canoniek — maar de norm van de
lezerszone-kaarten ("een lezer die aanvaardt wat geen verifier
toelaat is ongetuigd gedrag") geldt hier onverkort, en de
stringvergelijking geeft op een niet-canonieke waarde een STIL ander
antwoord in twee van de drie uitvoerders.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide metingen (asOfDate dag-eerst; misvormde
  geldigheidsgrens) op alle uitvoerders, letterlijke uitvoer in het
  dossier.
- (b) ref leest de drie posities door de strikte ISO-lezer en weigert
  misvormd benoemd (clingo volgt via refs casemodule); rood-eerst per
  positie. Weeg of het schemapatroon dan volstaat als tweede hek of
  dat een casusgetuige blijvend hoort te pinnen.
- (c) Corpussweep: geen bestaande replay beweegt (alle corpuswaarden
  zijn canoniek); alle veertien gates groen.

May touch: `ref/**`, `backends/clingo/**` (volgt), kaartverplaatsing.
NB: raakt de ref-caselaag — serieel met kaarten die ref/lib raken.
