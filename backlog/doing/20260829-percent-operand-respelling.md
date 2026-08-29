# Percent-operand: clingo laat puur-% toe + ankerherspelling

Goal: de korte-termijnkaart uit LADDER-EVOLUTIE §8.3 / Q10 (geratificeerd
2026-08-29), v0-compatibel: clingo's `Percentage_van`-arm accepteert een
linkeroperand waarvan de eenheid bewijsbaar puur `%` is (factor 1,
canoniek `%`) door de ÷100 in de bestaande schaalvouw op te nemen — elke
andere eenheid blijft luid geweigerd. Daarna respellen de twee
handgeschreven operanden naar de frontend-getrouwe vorm:
`anchor/jutterswet-vindersloon` en `synth2-afrondingscombinaties` krijgen
`eenheid:%` in plaats van het dragende `eenheid:null` (ref herspeelt
ongewijzigd — `pct_of` negeert de linkereenheid al; de gepinde waarden
veranderen niet).

Acceptance: gates 2/3/5/6 groen met de herspelde goldens (byte-roundtrip,
verifier-schoon, ref exact, clingo exact — clingo nu over de
frontend-getrouwe spelling); rood-eerst: een `Percentage_van` met een
niet-%-eenheid op links blijft aantoonbaar luid faulten (bestaande of
nieuwe negatieve toets in de clingo-testsuite); alle overige gates groen.
ASP-TRANSLATION-sectie over Percentage_van bijgewerkt (geen
toka-verwijzingen).

May touch: backends/clingo/lib/translate.ml (+tests),
corpus/golden/anchor/jutterswet-vindersloon/ir.json,
corpus/golden/synth2-afrondingscombinaties/ir.json,
spec/ASP-TRANSLATION.md, backlog/. Ankerpad = beschermd: onder het
mergemandaat na gates+lenzen.

Out of scope: de rij-27-collapse zelf (v1-wire-werk); de
normalisatiepas-kaart; ref- of verifier-wijzigingen (niet nodig).
