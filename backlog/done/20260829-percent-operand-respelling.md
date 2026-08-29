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

Result: clingo's Percentage_van-arm accepteert nu een bewijsbaar puur-%
linkeroperand (afkorting % ∧ canoniek % ∧ factor exact 1 ∧ geen noemer;
bewijs via declaratie/literal-typen, verborgen eenheid blijft luide
fault); de /100 zat al in de schaalvouw — de rekenweg is byte-ongewijzigd.
Beide handgespelde operanden (vindersloon-anker, synth2) dragen nu de
frontend-getrouwe %-eenheid (byte-identiek aan de
vondsttarief-emitterspelling) i.p.v. het dragende eenheid:null; geen
case- of pinwijziging (diff = één -1/+8-hunk per bestand). Rood-eerst:
nieuwe clingo-testsuite — pure-% faalde aantoonbaar op de oude code en
is groen op de nieuwe; km- en factor-2-negatieven pinnen het hek (eerlijk
gemeld: die waren ook vóór de kaart al fault — staand hek, geen
rood-naar-groen). Lenzen (2 opus, serieel, /tmp-only): acht adversariële
eenheids-spellingen door ref én clingo — elke niet-pure-% spelling fault
luid, silent-wrong structureel onmogelijk (ref negeert de linkereenheid,
clingo's encoding leest eenheid niet); waardebehoud bevestigd; gate-12-
classificatie bewezen ongewijzigd (zelfde EXCLUDE-token op main-versie).
Verwerkte MINOR: fidelity-NOTE in beide golden-mappen (vondsttarief als
getuige — deze goldens dragen geen bron en worden niet
frontend-geregenereerd). Geregistreerd, niet gedaan: de closed-body-arm
is symmetrisch meegenomen maar corpus-onbeoefend (kandidaat voor de
jacht/synthese-golf); gate 2 echo't sleutelvolgorde en pint die niet
(gate-eigenschap, geen kaartdefect); de unit-test toetst vertaling+ref,
de solver-gelijkheid dekt gate 6. Alle twaalf gates groen.
