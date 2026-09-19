# drl-geneste-let-duw: de ch.-10.6-herschrijving voor de DRL-tekst-tak

Gemeten (asp-ch102-kaart, 2026-09-19): de DRL-tekst-tak plaatst een
TWEEDE geneste `Let` tussen behoefte en takknoop nog eager, met een
verkeerde wáárde — `verwacht 1.00, werkelijk leeg` — en een tot de arm
verkleinde barrière — `verwacht 6.00, werkelijk 5.00` (op dezelfde
case antwoorden ref, clingo en exec 6,00). Dit is de
ch.-10.6-herschrijving (til-, distributie- en wisselroutes plus de
gemeten capture-guard) die de ASP-tak over twee kaarten heeft
gekregen, nu voor de DRL-tekst-tak. `synth3-armwissel` weigert daar nu
luid bij naam (`TODO takgarde van een voorwaardelijk gelezen behoefte
(ch. 10.2)`); armverdeling/armvangst falen op waarde.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de huidige foute waarden en de luide weigering
  gepind per vorm.
- (b) De herschrijving in de DRL-tak, met dezelfde reikwijdte-pins als
  de ASP-kant (één-arm-lezing gemeten, niet syntactisch afgeleid);
  suite-getuigen + mutatieproeven.
- (c) Corpussweep beide takken; kanaaltellings-gevolg als gemandateerd
  beschermd-padvoorstel; alle veertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + tests),
`scripts/check-drools.sh` uitsluitend kanaaltelling (voorstel),
kaartverplaatsing. NB: serieel met drl-let-meldplek (zelfde bestand;
die kaart eerst — vier goldens spelen pas als beide er zijn).
