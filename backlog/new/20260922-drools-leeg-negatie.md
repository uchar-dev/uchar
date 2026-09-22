# drools-leeg-negatie: NIET-LEEG-predicaat stil genegeerd in beide takken

Gemeten door de P1-nalens (2026-09-22, probe buiten het kaartobject):
een `Predicaat LEEG` met `negatie: true` op een verifier-schone
conditie wordt door BEIDE Drools-takken genegeerd — ref PASS, exec en
DRL-tekst spelen en geven het foute antwoord ("verwacht 40, werkelijk
leeg"). Dat is de verboden klasse: geen weigering maar een stil
verkeerd antwoord op een verifier-schoon document. Het gebeurt ook bij
een ongebonden geschreven enkele stap, dus het is ouder dan en staat
los van de P1-port. Corpusdragers: nul (alleen 23× GEVULD met
`negatie: false`).

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer de probe (conditie met genegeerd LEEG-predicaat,
  verifier-schoon) en meet alle vier de uitvoerders vóór de fix.
- (b) Beide Drools-takken dragen de negatie of weigeren benoemd;
  rood-eerst een getuige in de JUnit-suite van beide takken, en een
  kleine golden zodat gates 5/6/12 de vorm blijvend bewaken
  (handberekende verwachtingen; pins bewegen dan — benoemd voorstel).
- (c) Corpussweep beide takken: geen bestaande golden wisselt van
  kanaal of waarde.
- (d) Alle veertien gates groen.

May touch: `backends/drools/**`, `corpus/**`, `scripts/check-drools.sh`
(**voorstel**), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.
