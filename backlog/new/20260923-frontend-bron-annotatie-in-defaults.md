# frontend-bron-annotatie-in-defaults: `// Bron:`-annotaties overleven het liften naar een defaults-groep

Bijvangst van de onderzoekskaart dialect-defaults-niet-en-nesting, gemeten
in het voorstel-concept `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
(M6, kaartbranch): twee geannoteerde blokken (een basis en haar
`Uitzondering op`) geven een IR waarin geen van beide annotatieteksten nog
voorkomt; de verifier zegt niets. De lowering zet `bron: null` op de groep
en op elke default (`RuleChecker.java:331–351`, "no per-group bron", "No
per-default bron"). De werkbank-regelsets hangen hun herleidbaarheid per
wetslid aan precies die annotaties, en de handankers laten zien dat de wire
er ruimte voor heeft: `PrioritizedDefaults.bron` en `Default.bron` dragen
daar per default de wetsverwijzing (`anchor/ww-herleving-inkomen/ir.json`:
"art. 21 lid 2 onderdeel a: …").

## Ontwerp

- De `// Bron:`-annotatie van een gelifte regel landt in `Default.bron` van
  de default die eruit ontstaat; de annotatie van de basisregel bovendien
  (of alleen) in `PrioritizedDefaults.bron`; bij nesting op de default in
  de `ExceptionGroup`. Geen nieuwe wire-velden.
- De `Regel`-stub die per gelifte regel in `regels` achterblijft houdt zijn
  eigen `bron` zoals nu.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst JUnit-getuige met het M6-document: beide annotatieteksten
  staan in de uitgezonden IR op de verwachte plaats.
- (b) De bestaande extensie-goldens (`jutterswet-bergingsdrempel`,
  `jutterswet-wrakrecht`, `jutterswet-wachtgeld`) blijven byte-identiek als
  zij geen annotaties dragen; dragen zij die wel, dan worden hun `ir.json`
  opnieuw uitgezonden en verklaart de kaart het verschil.
- (c) `DIVERGENCES.md` §4 beschrijft de plaats van de annotatie.
- (d) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, de drie extensie-goldens (alleen
heruitzending), kaartverplaatsing.

Niet in deze kaart: annotaties op andere geliftende vormen dan de
defaults-extensie.
