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

## Voortgang (2026-08-24)

Drie artefacten geland, geen gedragswijziging (ref gedroeg zich al zo;
clingo's weigering is conform):

- **RS ch. 2.9** — de kleinste normzin "**The scale is a boundary, not a
  step**" (besluit 2026-08-24, vraag 3-A): tussenwaarden zijn
  exact-rationeel (ch. 2.1); een gedeclareerde schaal `S` bindt een
  declaratie en de schrijfgrens die zij bewaakt (ch. 2.4), nooit een
  tussenstap — strikte-`Let`-binder (ch. 10.6) en `Match`-arm (ch. 9.2)
  inbegrepen; de enige collaps naar decimaal is een `Afgerond`-knoop
  (ch. 2.5). WV-getal: `100 ÷ 3` door een strikte Let, `afgerond op 2`
  → **33,33**; dezelfde binder `afgerond op 4` → **33,3333** (nooit de
  33,3300 die een stap-bindende schaal zou forceren).

- **ref/test/test_ref.ml** — het WV-voorbeeld (kaart-gesanctioneerde
  testaanraking), twee asserts `WV-2.9-let-endpoint-round` (33,33) en
  `WV-2.9-let-exact-intermediate` (33,3333). ROOD-EERST: eerst met de
  opzettelijk foute verwachtingen 33,34 en 33,33 gedraaid — letterlijke
  uitvoer `FAIL WV-2.9-let-endpoint-round` / `FAIL
  WV-2.9-let-exact-intermediate` / `2 failure(s)` — daarna gecorrigeerd,
  groen (`test_ref: all 253 worked examples pass`).

- **ASP-TRANSLATION S19** — notitie bij de strikte-`Let`-alinea: clingo
  materialiseert de binder op een eindige integer-schaal, dus de
  geminimaliseerde synthesevondst `100 ÷ 3` faalt luid op de bare-division
  `non-exact division`-fence (100/3 termineert op geen enkele eindige
  schaal), waar ref de exacte breuk tot het eindpunt-`Afgerond` draagt.
  Gedocumenteerde backend-limiet conform de norm; divergentie in
  *expressiviteit*, nooit in *waarde*. S19 blijft de hoogste sectie
  (geen hernummering).

Alle negen gates groen. Nog open (acceptance): twee opus-lenzen verwerkt.
