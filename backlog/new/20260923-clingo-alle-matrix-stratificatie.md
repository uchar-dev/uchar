# clingo-alle-matrix-stratificatie: de kvs-reïficatie heeft hetzelfde cyclusgat

Gemeten door de clingo-leeg-negatie-lens (2026-09-23, proef R5,
bestond al vóór die kaart): de ALLE-matrix-reïficatie (`kvs`) in
translate.ml gebruikt dezelfde `not`-hulpregel als de nieuwe
leeg-arm en heeft hetzelfde stratificatiegat — een ALLE-vouw over een
vergelijking op een WF-045-toegelaten recursieve creatiecyclus is
verifier-schoon, ref en beide Drools-takken spelen, clingo geeft
UNSATISFIABLE. Het "stratified by construction"-commentaar daar
claimt te veel. De wortel ligt bij de verifier
([[20260923-verifier-leegtest-negafhankelijkheid]] — een ALLE-matrix
is semantisch óók een negatieve afhankelijkheid); dit kaartje draagt
de clingo-kant: dezelfde conservatieve benoemde weigering als de
leeg-arm kreeg, of de versmalling zodra de verifier-kaart de cycli
tegenhoudt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer R5; meet welke reïficaties het gat delen (kvs, kvc,
  gv-familie — inventariseer).
- (b) Conservatieve benoemde weigering per reïficatie (huispatroon
  van de leeg-arm) óf de verifier-kaart eerst; kies langs de dan
  geldende stand en motiveer.
- (c) Rood-eerst suitechecks; corpussweep byte-identiek; alle
  veertien gates groen.

May touch: `backends/clingo/**`, kaartverplaatsing. NB: serieel met
andere translate.ml-kaarten.
