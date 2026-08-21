# Fase 5a, kaart 1: het Catala-plugin-skelet

Besloten 2026-08-21 (besluitenronde 3, alle zes besluiten A); geen
open besluiten — de kaart is direct uitvoerbaar.

Goal: frontends/catala/ bestaat en emit geldige regelspraak-ir-v0
voor het besloten 5a-fragment. Stappen: (1) de Catala-compiler als
dependency binnenhalen en het aanhechtpunt verifiëren tegen de
actuele codebase (de scopelang→dcalc-grens; PHASE5-CATALA §2 — de
module-namen zijn implementatietijd-werk); (2) het plugin-skelet dat
scopes → objecttypen, scope-variabelen → attributen, platte
default-bomen → ladders/groepen, context-overrides → top-rungs
(besluit 6), geld/datums → exacte rekenkunde en asserties →
consistenties emit; (3) de luide weigeringen van besluit 4; (4) de
Catala-tutorial draait frontend-side in de plugin-testsuite door
verifier én beide executors.

Acceptance: de tutorial-subset emit verifier-schone IR die op ref en
clingo draait (de plugin-suite toont het); alle weigeringen faulten
met reden; alle acht uchar-gates blijven groen; twee lenzen.

May touch: `frontends/catala/` (nieuw), `.github/` alleen als CI het
skelet moet bouwen (expliciet melden), `backlog/`.

Out of scope: het corpus (kaart 2); folds/enums (5b); geneste groepen
(5c).

Aanhechtpunt geverifieerd (2026-08-21, stap 1): catala 1.2.1 + clerk
lokaal; `catala scopelang --no-stdlib` toont het niveau exact zoals
het voorstel het beschrijft (default-bomen per variabele, context als
"reentrant or by default"); de compilerbibliotheken zijn linkbaar via
ocamlfind (catala.driver, catala.scopelang, catala.shared_ast).
Architectuurkeuze: de plugin serialiseert via de bestaande
Ir_v0-bibliotheek (dezelfde als verifier/ref/clingo), zodat
schema-conformiteit en round-trip-stijl by construction gelden.
May-touch-notitie: de registratie van frontends/ als artifact-zone in
scripts/check-self-contained.sh (en de CLAUDE.md-gatetekst) volgt als
expliciete verruiming bij de afronding — gate-onderhoud dat de nieuwe
zone vereist.

## Resultaat (2026-08-21, op branch card/fase5a-plugin-skelet)

`frontends/catala/` bestaat: lib `catala_frontend_lib` (module
`Catala_frontend_lib`, submodules `Translate` + `Emit`) gelinkt tegen
catala.driver/scopelang/shared_ast/desugared/runtime_ocaml + uchar-ir-v0,
en bin `catala_frontend` (`catala_frontend <in.catala_en> <out-ir.json>`).
Aanhechting: `Driver.Passes.scopelang` (scopelang→dcalc-grens, besluit 1).
De plugin bouwt `Ir_v0.Ast`-waarden en serialiseert via `Emit` (nieuw —
uchar-ir-v0 had geen AST→JSON-serializer; gate 2's round-trip is Python).

Vertaalt (door verifier + ref + clingo, tenzij vermeld): scope→objecttype;
niet-boolean var→attribuut, boolean→kenmerk; input→case-attribuut; enkele
onvoorwaardelijke numerieke definitie→BEREKENING; boolean-definitie→
KENMERKTOEKENNING (waarde is de guard); keten→één attribuut-defaultLadder
(rungs hoog→laag); root-ongeordende excepties zonder basis→ongeordende
laddergroep; geld→Numeriek 2 dec (+ Binair); assertie→consistentie;
datum→DatumInDagen + Later/Eerder (ref-only: clingo vertaalt geen
datum-literal in termpositie — bestaande backend-limiet).

Weigert luid (WF-044-stijl): **context/reentrant-variabele** (bevinding:
besluit 6 wil Reading B = caller-override als hoogste rung, maar ch. 7.8
zet de virtuele input-rung ONDER de gewone rungs = Reading A; een
enkel-scope 5a-programma kan Reading B niet realiseren); subscope-call;
sub-root-vertakking en basis-plus-ongeordend; decimal/rational; enums met
payload, folds, functiewaarden, lijsten, structs, if-then-else.

Plugin-suite groen: `dune runtest frontends/catala` → 29 checks, 0 failures.
Alle acht uchar-gates blijven groen (gerichte builds raken frontends/ niet).
Niet gemerged, niet gepusht. Openstaand voor afronding/hoofdsessie:
frontends/-zoneregistratie (scripts/ + CLAUDE.md, protected), eventuele
CI-leg (.github, niet aangeraakt), en de lib-modulenaam-afwijking
(`Catala_frontend_lib` i.p.v. `Catala_frontend`: dune verbiedt exe en lib
met dezelfde modulenaam — de exe-naam `catala_frontend` is behouden).

Result, slot (2026-08-21, na de lensronde): de zoneregistratie van
frontends/ in scripts/check-self-contained.sh en de CLAUDE.md-gatetekst
is gedaan (de aangekondigde verruiming; de gate ving direct één
private-repo-naam in een commentaar — geveegd). De twee opus-lenzen
vingen DRIE stille divergenties, elk tegen catala interpret
gereproduceerd en vóór de merge gedicht met luide weigeringen of een
getrouwe vorm: (1) een boolean in waarde-positie werd een dangling
AttribuutRef (ref stil fout, clingo fault) → weigert nu luid; (2)
Catala leest elke variabele hard (leeg = fataal) waar onze additieve
familie coalesct — een conditioneel-lege operand gaf stil een
gefabriceerde waarde → reken-/vergelijkingsoperanden zijn nu beperkt
tot inputs (recorded aanname: het case-dossier vervult Catala's
input-contract) en onvoorwaardelijk gedefinieerde variabelen, de rest
weigert met het deviatiebesluit expliciet uitgesteld; (3)
geld-deling miste Catala's verplichte cent-afronding (ref faultte op
de rest) → Div_mon_int/rat wikkelt nu in Afgerond(REKENKUNDIG, 2),
tegen catala interpret geverifieerd ($20/3→6.67), door alle drie de
poorten; money/money weigert (decimal). Plus: de
ConflictError-deviatie is nu écht getest (overlappende ongeordende
gronden → inconsistent in beide executors) en de weigeringssuite
dekt alle besluit-4-vormen (44 checks totaal). TUTORIAL-VERZOENING
(acceptatie-eis 4, expliciet): de officiële Catala-tutorial steunt op
structs, decimals, subscope-calls en lijsten — stuk voor stuk
besluit-4-weigeringen — en is dus binnen het 5a-fragment
onuitvoerbaar; de suite draait rechtenvrije eigen fixtures en de
struct/decimal/lijst-weigeringstests zijn het grensbewijs (de
tutorial weigert luid, exit 3, ter plekke gedemonstreerd). De
tutorial-door-de-plugin verschuift daarmee expliciet naar het moment
dat 5b (folds/enums) en de subscope-mapping landen. BESLUIT-6-NOTITIE:
in de praktijk draait 5a optie B (context-variabelen weigeren) — de
door de maintainer gekozen optie A (top-rung) blijkt pas realiseerbaar
zodra subscope-calls vertalen, omdat ch. 7.8's input-rung Reading A
is en een enkel-scope-programma geen caller heeft; vastgelegd op
kaart 2 voor 5c. Rood-eerst is historisch verifieerbaar: commit
6c93b77 draagt de suite met stub-bin (5 checks, 5 failures) zónder
translate.ml. Openstaand, eerlijk: de plugin-suite draait buiten de
acht gates (een CI-leg/gate-9-kaartje is de natuurlijke volgende
stap); datum-vergelijkingen zijn ref-only (clingo vertaalt geen
datum-literal in termpositie — bestaande backend-limiet); de
byte-canonieke serialisatie is corpus-werk voor kaart 2.
