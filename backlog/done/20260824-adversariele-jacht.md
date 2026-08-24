# Adversariële jacht, eerste sessie

Goal: de staande PLAN §8-jacht, eerste uitvoering: vind programma's
waarop het differentieel-vierkant divergeert (ref ≢ clingo) of de
verifier ten onrechte accepteert, gericht op de jonge constructen
(sumtypes, folds, structs, geneste groepen, Let/decimals,
subscope-lowering, kenmerk-init-expressies) en de klassieke flanken
(leeg, randwaarden, schalen, diepe nesting, grote getallen,
tijdvensters). Hand-gerichte constructie, geen blinde fuzzing; elk
vermoeden empirisch door beide executors. Elke vondst wordt
GEMINIMALISEERD en dan gerouteerd: een verifier-vals-accept wordt een
corpus/negatief-voorstel mét rapport; een echte, kleine executor-bug
mag binnen het mandaat gefixt (rood-eerst, smalste wijziging, beide
executors); een grote of ontwerp-rakende vondst blijft een
kaartvoorstel in het rapport. KRITIEK: een onopgeloste
ref↔clingo-divergentie kan NIET als corpus-case worden ingecheckt
(gate 6 zou rood) — die blijft geminimaliseerd in het rapport.

Acceptance: alle negen gates groen op de branch (wat is ingecheckt is
af); het jachtrapport in de kaart somt élk onderzocht spoor op —
ook de sporen die niets vonden (negatieve resultaten zijn data);
twee opus-lenzen verwerkt over de ingecheckte wijzigingen.

May touch: corpus/negatief/, corpus/rood/, ref/ en backends/clingo/
(UITSLUITEND voor kleine mandaat-fixes met rood-eerst; meld elk),
scripts/check-noop.sh (alleen indien nodig), backlog/. Out of scope:
spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/schema/,
spec/verifier/ (een verifier-gat is een negatief-voorstel + rapport,
geen eigen check-wijziging), frontends/, spec/CONSTRUCTS.md.

---

## Jachtrapport — eerste sessie (2026-08-24)

Methode: hand-gerichte constructie rond de jonge constructen en de
klassieke flanken; elk vermoeden empirisch door verifier, ref én clingo
(binaries `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`,
`spec/verifier/_build/default/bin/verifier.exe`). Waar ik geen pin-waarde
wist, zette ik een sentinel-`expected` zodat beide runners hun
GEREKENDE waarde in de MISMATCH-regel drukken — een schoon differentieel
(ref-decimaal vs clingo-geschaalde-int). Baseline vóór de jacht:
gate 5 en gate 6 groen, 204 cases (ref ≡ clingo). Werkbestanden in
`/tmp/hunt/` (buiten de repo).

Hoofdbevinding: dit is een uitzonderlijk goed geharde codebase. De
ontwerpfilosofie is fail-fast ("executors fault, never guess"): elke
niet-encodeerbare vorm faalt LUID in clingo (viol/2 → UNSAT, of een
translation fault), nooit stil. Beide executors delen bovendien
letterlijk de navigatie-/rol-helpers (`Eval.role_by_name`,
`connecting_role`, `bound_role`) en dezelfde fold-source-sentinels.
Stille ref≢clingo-divergentie is daardoor bijna wegontworpen. Ik vond
er geen. Wél één echte verifier-lacune (spoor 7b), luid maar routeerbaar.

`scripts/check-noop.sh` is NIET aangeraakt.

### Sporen die iets vonden

**Spoor 7b — VONDST: leaf-ref-type-leugen in additieve operand-positie.**
(WF-attributie en asymmetrie-framing hieronder GECORRIGEERD in de
lensronde — zie "Lensronde" onderaan; de oorspronkelijke tekst blijft als
log staan.) De verifier controleert `AttribuutRef.type` tegen de gedeclareerde
datatype (WF-023; negatief `wf027-lying-attribuutref-type`), maar voor
een `BinderRef` bestaat die controle niet. WF-064 checkt alleen
binder-resolutie en arm-lichaam-type-overeenkomst
(`spec/verifier/lib/checks.ml:2218`,`:2244`), niet het `type` van het
blad zelf. Gevolg op de aanspraaktelling-fold (`acc + x`):
- decimalen-leugen op `x` (2→4): INERT — verifier/ref/clingo alle PASS,
  zelfde waarde (executors negeren de annotatie);
- soort-leugen op `x` (`Numeriek`→`Tekst`/`Booleaan`): verifier PASS +
  ref PASS (correcte 12.50/2) + clingo `TRANSLATION FAULT: non-numeric
  arithmetic operand is not translated` (uit `numeric_ok`,
  `backends/clingo/lib/translate.ml:383`). Splitst het vierkant, LUID.
Routering: `spec/verifier/` is protected → kaartvoorstel
`backlog/new/20260824-binderref-type-crosscheck.md`. GEEN ingecheckte
negatief-case: de verifier keurt het (nog) niet af, dus een
`corpus/negatief/wf064-*` zou gate 3 groen-blijven-laten = niet-afkeuren.
Repro: `/tmp/hunt/mut_M14_binderref_x_kind_text.json` + `m12-case.json`.

### Sporen die (bewust) LUID fenced bleken — geen stille splitsing

- **Afrondingswaaier × negatieve deler** (`synth-afrondingswaaier`,
  opbrengst −87): clingo `viol("negative operand under rounded division
  is not translated","V1")` → UNSAT; ref rekent (−22/−21/−22/−21/−22/
  −21.8). Fault-asymmetrie by-design.
- **Grote magnitudes** (aanspraaktelling 2×15000000.00; ook additief/
  multiplicatief/#sum/write): clingo `viol("arithmetic range exceeded
  ...")` → UNSAT; ref exact. Elke integer-encoding-pad draagt een
  range_guard (operanden ≤46340 mult, ≤~1e9 add) of `scaled_of_value`-
  bereikcheck. Luid.
- **Kenmerk-init-expressie × navigatie** (verste code, WF-027-relaxatie):
  clingo `bool_atoms` (`translate.ml:2584`) dekt UITSLUITEND een
  eigen-subject platte boolean-attribuutlezing → `heeft(S,a)`; elke
  andere boolean-vorm faalt luid ("no corpus case for more"). Een
  boolean-vergelijking is geen geldig EXPR-knooppunt (loader weigert
  "Vergelijking" in een expr-slot), dus de praktische bredere vorm is een
  GENAVIGEERDE boolean-lezing: ref voert uit, clingo weigert luid. In-code
  erkend; geen corpus-case. Analyse-niveau (genavigeerde IR niet
  hand-geschreven).

### Sporen die niets vonden (negatieve resultaten = data)

- **Afronding-halven, positief** (division-rounding én standalone): de
  clingo-formules `(2t+k)/(2k)` en `(2num+den)/(2den)` = ref's `round_z
  Rekenkundig` (`floor(|x|+½)`) exact voor niet-negatief; 86/4=21.5→22,
  90/4=22.5→23 (half-away, geen banker's), beide gelijk. Alle vijf modi:
  `Richting_nul`/`Naar_beneden` = trunc = floor (niet-neg), `Naar_boven`/
  `Weg_van_nul` = ceil, `Rekenkundig` = half-away; negatief overal luid
  gefenced. Geen divergentie.
- **Geld×tarief cent-afronding** (`translate_money_mult`): bewezen
  `C = qA + (100·rA + M·Rl + 5000)/10000 = round_half_up(M·R/10⁴)`
  exact voor niet-negatief (M·R = 100·A + M·Rl; floor van de
  niet-negatieve teller = integer-deling). Geen tie-divergentie.
- **Record-gelijkheid × veldvolgorde**: ref's Record-`equal` is
  POSITIONEEL, maar de case-loader bouwt elk record in DECLARATIEVOLGORDE
  (`ref/lib/case.ml:132`, opzoeken per veldnaam), dus positioneel ==
  naam-gebaseerd. clingo `struct_value_term` zoekt óók per naam in
  declaratievolgorde. Consistent.
- **Fold/Map/Match/Filter over randlijsten** (aanspraaktelling):
  all-Vervallen → 0.00/0 (beide); enkel Erkend 999999.99 → 99999999
  geschaald (beide); lelem is 1-based in array-volgorde in beide
  (`translate.ml:3354` vs `Value.Lijst`), links-fold in beide. FilterList
  herindexeert contigu in ref / laat gaten in clingo, maar alleen #count
  leest de gaten; een Fold-over-Filter wordt luid geweigerd
  (`no_filter_source`). Geen stille splitsing.
- **Fold-source-sentinel** (row-1 telkwantor-over-rol): ref
  (`eval.ml:379-416`) en clingo (`translate.ml:608-611`,`:688`) gebruiken
  DEZELFDE twee signalen (`set=true` / `typ=Datatype_onbekend`) om een
  fold-source-pseudo-ref van een echte lezing te scheiden; elke andere
  vorm faalt in beide. Consistent.
- **Verifier-mutatiebatterij op aanspraaktelling** (fold/map/match/invoke/
  sumtype), alle CORRECT afgekeurd: fold-init-type (WF-070),
  ontbrekende arm (WF-063), invoke-ariteit (WF-068), ongebonden acc
  (WF-064), dubbele arm (WF-063), alien arm (WF-063), fold 1-param
  (WF-069/064), maplist-elem-type-leugen (WF-070), fold-resultaat-type-
  leugen (WF-070), scrutinee-domein-leugen (WF-063), arm-lichaam-type-
  leugen (WF-064). Alleen de NESTE BinderRef-blad-annotatie glipt door
  (spoor 7b).

### Routering-samenvatting

Eén vondst (spoor 7b) → kaartvoorstel (verifier protected). Geen
ingecheckte corpus/negatief, corpus/rood, ref/ of backends/clingo/
wijziging: er is geen kleine mandaat-fix nodig (de executors gedragen
zich correct; de lacune zit in de poortwachter) en geen zwijgende
divergentie om rood-eerst te vangen. Ingecheckt: dit rapport + het
kaartvoorstel. Alle negen gates in de worktree onveranderd t.o.v.
baseline (de wijzigingen zijn backlog-proza; gate 1 scant backlog niet).

## Lensronde (2026-08-24) — twee opus-lenzen verwerkt

Twee MINOR/CONFIRMED-bevindingen over spoor 7b / het kaartvoorstel; beide
zelf her-geverifieerd (verifier + ref + clingo, herhaalde repro) en beide
GEGROND. Geen weerlegging. De correctie raakt alléén proza (het
kaartvoorstel `backlog/new/20260824-binderref-type-crosscheck.md` en dit
rapport); geen corpus-, ref-, clingo- of scripts/check-noop.sh-wijziging.

Kern van beide lenzen: spoor 7b's WF-attributie en asymmetrie-framing
klopten niet.

- **WF-id.** Wat een soort-leugen (`Numeriek` → `Tekst`/`Booleaan`) op een
  reken-operand afkeurt is **WF-035** ("operand of a numeric operator is
  text/boolean", `spec/verifier/lib/checks.ml:1663`,`:1675`), niet WF-023
  en geen blad-kruiscontrole. WF-023 (`checks.ml:3942`) is de
  toewijzings-doelregel; de resolved-blad-kruiscontrole
  `resolved_attr_datatype` (`checks.ml:741`) is UITSLUITEND in **WF-027**
  bedraad (`checks.ml:791`), vandaar dat `wf027-lying-attribuutref-type`
  WF-027 citeert, niet WF-023.
- **As van de asymmetrie.** De escape was **operator-positie-gedreven**, niet
  BinderRef-specifiek. WF-035 leest `typ_of_expr` uniform voor BinderRef én
  AttribuutRef (`checks.ml:719`), maar vuurt alleen onder numeriek-vereiste
  operatoren; voor `PLUS`/`MIN` is de numericiteits-eis uitgezet
  (`checks.ml:1668`, datum±duur). Her-geverifieerd: BinderRef-`x`-soort-leugen
  onder `MAAL` → `WF-035 FAIL` (afgekeurd); dezelfde onder `PLUS` (spoor 7b's
  `acc + x`) → verifier PASS; een AttribuutRef-operand onder `PLUS` (vindersloon
  `loon`, op `MAAL`→`PLUS`, `waardering` `type`→`{Tekst}`) → verifier PASS, ref
  rekent `loon = 13`, clingo `TRANSLATION FAULT: non-numeric arithmetic
  operand`. De decimalen-leugen is inert en ongecontroleerd voor béide
  blad-soorten (`waardering` `decimalen 0→5`: verifier/ref/clingo alle PASS,
  `loon = 42`).

Gevolg voor het kaartvoorstel: herkaderd van "kruiscontrole van
`BinderRef.type`" naar "leaf-ref-type-leugen in additieve operand-positie"
(leaf-ref-breed, niet BinderRef-only) met de smalste, gedrag-gerichte fix
(WF-035's `PLUS`/`MIN`-uitzondering aanscherpen voor `Tekst`/`Booleaan`) als
aanbeveling. Een BinderRef-only fix zou de parallelle
AttribuutRef-onder-additief-lacune open hebben gelaten.

`scripts/check-noop.sh` NIET aangeraakt.

Result: de eerste jachtsessie vond géén stille ref↔clingo-divergentie
— de fail-fast-architectuur hield stand: elke niet-encodeerbare vorm
faalt luid, en de verdachte formules (round_z, de gesplitste
geldvermenigvuldiging, de cent-afronding) bleken bewijsbaar gelijk.
Wel één echte verifier-lacune: BinderRef.type wordt niet tegen het
binder-datatype gekruist (de kruiscontrole die AttribuutRef wél
heeft) — een soort-leugen op een genest BinderRef-blad is
verifier-schoon, ref rekent door, clingo fault luid: de
vierkant-invariant splitst. Omdat de verifier beschermd pad is, is
dat een kaartvoorstel geworden
(backlog/new/20260824-binderref-type-crosscheck.md), geen eigen
wijziging. Elk onderzocht spoor staat in de kaart, ook de lege. De
lensronde ving tweemaal dezelfde documentaire misattributie
(WF-023-benaming), verwerkt. Open, geregistreerd: de genavigeerde
boolean-kenmerk-init is ref-uitvoerbaar maar clingo-geweigerd (luid,
in-code erkend) — een geauthorde getuige zou de asymmetrie pinnen;
en de vraag of de BinderRef-check ook decimalen moet kruisen hoort
bij het kaartvoorstel.
