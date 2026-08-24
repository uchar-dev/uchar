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

## Lensronde (2026-08-24)

**BEVINDING 1 [MINOR/PLAUSIBLE] — CONFIRMED, verwerkt.** De normzin
(RS ch. 2.9, regel 409) stelde "the sole collapse to a decimal is an
explicit `Afgerond` node (ch. 2.5)". Zelf geverifieerd: ch. 2.1
(regels 212–217) somt DRIE collapse-punten op — (1) expliciete afronding
ch. 2.5, (2) abs-deling ch. 2.2, (3) schrijfgrens ch. 2.4 — en de ch. 2.2
tabel (regel 235) bevestigt de abs-variant als "a defined collapse point";
`ref/lib/arith.ml:171` (`div_abs = ... round_q Richting_nul 5`) bevestigt
dat de abs-deling buiten elke `Afgerond`-knoop naar decimaal collabeert.
Het woord "sole" overklaimt dus: het sluit de expliciete `÷abs`-operator
uit en is strak genomen inconsistent met de gezaghebbende opsomming in
ch. 2.1. GEEN gedragsdefect — ref klopt en blijft ongewijzigd; dit is een
precisie-correctie in normatieve tekst.

Fix (kleinste, geen gedragswijziging):
- **RS ch. 2.9** — de collaps-clausule verwijst nu direct naar ch. 2.1's
  opsomming: "a declared scale is never itself a collapse point: those stay
  exactly the three of ch. 2.1 — an explicit `Afgerond` node (ch. 2.5), the
  ch. 2.2 abs-division operator, and the ch. 2.4 write boundary already
  named." De these van de zin ("de schaal is een grens, geen stap") blijft;
  alleen de overklaim is weg.
- **ref/test/test_ref.ml** (kaart-gesanctioneerde testaanraking, gemeld) —
  het WV-commentaar droeg dezelfde formulering; nu op de witness begrensd:
  "the sole collapse in this witness is its endpoint Afgerond node (ch. 2.5;
  ch. 2.1 also admits the ch. 2.2 abs-division, not exercised here)."
  Commentaar-only; de 253 WV's blijven groen.
- **ASP-TRANSLATION S19** — al witness-begrensd ("the single endpoint
  Afgerond collapses it once to 33,33"), geen overklaim; ongewijzigd.

Geen rood-test: zuivere prozacorrectie zonder gedragswijziging; de bestaande
WV-asserts `WV-2.9-let-*` blijven het gedrag pinnen.

Alle negen gates opnieuw groen:
- check-self-contained: PASS
- check-ir-schema: PASS (schema valid; 97 file(s) validate and round-trip)
- check-verifier: PASS (97 golden clean; 138 negatief rejected for their declared rule)
- check-case-schema: PASS (schema valid; 266 case file(s) validate)
- check-oracle: PASS (249 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)
- check-clingo: PASS (249 cases replay exactly through clingo — ref = clingo over the corpus)
- check-noop: PASS (216 cases refute a do-nothing executor; 33 pinned survivors pin defeasible standing)
- check-rood: PASS (17 red cases fault observably in both executors)
- check-catala: PASS — frontend suite green (128 checks); 7 catala golden(s) regenerate byte-identically
