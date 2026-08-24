# De afrondingsnorm: exact tot het afrondingspunt (besluit 3-A)

Goal: het maintainersbesluit van 2026-08-24 (vraag 3: A) normeren:
tussenwaarden zijn exact-rationeel; de gepinde schaal S (RS ch. 2.9)
bindt declaraties en schrijfgrenzen, nooit tussenstappen; afronding
gebeurt uitsluitend waar een Afgerond-knoop staat. De kleinste
RS-zin die dit vastlegt (ref gedraagt zich al zo — de synthesevondst
33.33 door een Let/Match-kruisende niet-terminerende deling), plus
een WV-werkvoorbeeld in ref/test dat het pint
(kaart-gesanctioneerde testaanraking, melden), plus de
ASP-TRANSLATION-notitie die clingo's materialisatiegrens als
gedocumenteerde LUIDE backend-limiet vastlegt (de geminimaliseerde
vondst als voorbeeld in S19; clingo blijft die vorm weigeren — dat
is conform, geen bug; S19 blijft de hoogste sectie).

Acceptance: alle negen gates groen; het WV-voorbeeld draait; de
RS-zin en S19-notitie consistent; twee opus-lenzen verwerkt.

May touch: spec/RUNTIME-SEMANTICS.md (de kleinste normzin),
spec/ASP-TRANSLATION.md, ref/test/ (het WV-voorbeeld, gemeld),
backlog/. GEEN spec/CONSTRUCTS.md. Out of scope: alle code in
ref/lib en backends (gedrag wijzigt NIET — dit is normering van
bestaand gedrag), spec/IR-SPEC.md, spec/verifier/, frontends/.
