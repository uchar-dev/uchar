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

## Acceptatie (aangescherpt bij de claim, 2026-09-22)

- (a) Reproduceer de probe op main db224b04 (na P1–P3): een
  verifier-schone conditie met `Predicaat LEEG, negatie: true`, zowel
  op een eigen attribuut als over een enkele navigatiestap, en het
  zusje `GEVULD, negatie: true` (dezelfde veldnegeer-klasse). Meet
  verifier, ref, exec-tak en DRL-tekst-tak; letterlijke uitvoer in het
  Result. Leg vast welke speczin de negatie normeert (IR-SPEC ch. 4
  Predicate-rij, RUNTIME-SEMANTICS ch. 1.2) en wat ref ermee doet.
- (b) Beide Drools-takken dragen de negatie (voorkeur: de genegeerde
  lege-/gevuldtest is precies de complementaire test, dus mechanisch te
  lowern) of weigeren benoemd waar dat niet exact is (een negatie op
  een geheel-periode-kwalificatie). Rood-eerst een JUnit-getuige via de
  replay-API voor BEIDE takken; een hand-geschreven golden in
  `corpus/golden/` met handberekende verwachtingen in een NOTE.md,
  gate 2/3/4-conform (schema-valide, round-trip byte-identiek, case
  naast ir, verifier-schoon). Gate-12-pins `EXPECTED_PLAYED` en
  `EXPECTED_DRL_PLAYED` elk +1 met redenregel — beschermd-pad-voorstel.
  Negatief- of ref-getuige alleen als de spec er een regel voor draagt.
- (c) Corpussweep beide takken vóór/na over alle goldens en rood: geen
  bestaande golden wisselt van kanaal of waarde, runner-uitvoer
  byte-identiek op de nieuwe golden na.
- (d) Alle veertien gates groen, elk als losse aanroep. Mutatieproef:
  fix terugdraaien ⇒ getuige én golden rood in beide takken ⇒ fix
  terug, `cmp` op het bronbestand.

May touch: `backends/drools/**`, `corpus/**`, `scripts/check-drools.sh`
(**voorstel**), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.
