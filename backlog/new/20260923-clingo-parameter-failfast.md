# clingo-parameter-failfast: ongebonden parameters zijn stil leeg

Gemeten door de clingo-leeg-negatie-lens (2026-09-23, proeven
E2/E3, bestond al): een ONgebonden parameter is in clingo stil leeg
waar ref fail-fast werpt ("parameter … is unbound in this case
(binding is fail-fast, ch. 5.7)") — bij een GEVULD-test en bij een
gewone waardelezing. De leeg-negatie-kaart dichtte de LEEG-testflank
in haar eigen arm (en `dk_value` weigerde al langer); de overige
leesposities antwoorden nog. Zolang elke corpuscase alle parameters
bindt is er geen corpusdrager, maar de klasse is dezelfde als de
gedichte: een stellig antwoord waar de arbiter faalt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Inventariseer de parameterleesposities in translate.ml; meet
  per positie ref versus clingo op een ongebonden parameter.
- (b) Eén gedeelde fail-fast in refs woorden (het dk_value/leeg-arm-
  precedent veralgemeend), rood-eerst per positie.
- (c) Corpussweep byte-identiek; alle veertien gates groen.

May touch: `backends/clingo/**`, kaartverplaatsing. NB: serieel met
andere translate.ml-kaarten.
