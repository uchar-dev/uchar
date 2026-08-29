# Fase 5c — het ontwerpvoorstel: structs, subscope-calls, geneste groepen, de tutorial-route

Goal: één ontwerpdocument `spec/proposals/PHASE5C-TUTORIAL.md` dat de
resterende weg naar het formele 5a-exitcriterium (de Catala-tutorial
door de plugin, drievoudig geverifieerd) spec-first uitwerkt: (1) de
struct-representatie — de open kernkeuze: eigen record-datatype versus
lowering op objecttypen; (2) subscope-calls langs de al in PLAN §6
benoemde richting (object type + object creation + navigation),
inclusief de geratificeerde top-default-realisatie van besluit 6 als
gegenereerde default; (3) geneste defaults-groepen per de geratificeerde
optie (i) van PHASE4-DEFAULTS §7 (WF-051-randklassen mechanisch
uitgebreid); (4) de besluit-vrije restjes: TRat/decimals op de
onbegrensde Numeriek-precisievorm, de no_stdlib-flip met
Integer.sum-toelating, en `let … in`. Elk deel met nodevormen,
WF-regels (vanaf WF-071), negatiefs, semantiekschets, beide executors
(luide clingo-faults mits geïnventariseerd), case-formaat en
plugin-mapping. De échte kernkeuzes gaan als genummerde vragen naar de
maintainer vóór enig spec-werk merget; de besluiten worden als Decided
vastgelegd.

Acceptance: `spec/proposals/PHASE5C-TUTORIAL.md` bestaat en is door
twee opus-lenzen gehaald met bevindingen gefixt; elke kernkeuze draagt
een maintainer-antwoord (Decided, met datum); de implementatiekaarten
staan in `backlog/new/` met acceptance in gate-termen; gates 1–9
blijven groen (dit voorstel raakt geen artefactzones).

May touch: spec/proposals/PHASE5C-TUTORIAL.md (nieuw), backlog/.

Out of scope: elke wijziging aan IR-SPEC.md, RUNTIME-SEMANTICS.md,
spec/schema/, spec/verifier/, ref/, backends/, frontends/, corpus/ —
die volgen in de implementatiekaarten na ratificatie.

Result: `spec/proposals/PHASE5C-TUTORIAL.md` staat, gelensd en
geratificeerd (2026-08-22, vijf genummerde vragen, alle A): structs
als record-datatype (het SumType/ListType-landingspatroon, met de
WF-065/067/069-verbredingen); de strikte `Let`-knoop met de
conditie-operand-discipline en het WF-069-herbereik; tutorial_end_2_3
als frontend-side exitgetuige (het corpus blijft rechtenvrij per
besluit 2); de gepinde clingo-decimaalschaal; geneste
sub-groep-conflicten propageren naar de root (rij-8-deviatieverruiming).
De verkenning mat de tutorial exact op (de vertakkende
artikel-3-boom zet geneste groepen op het kritieke pad; geen
context-inputs, dus besluit-6-realisatie uitgesteld; de latente
geld-vermenigvuldigingsafronding gevonden). De lensronde (2 opus) ving
één blocking en vier majors, alle verwerkt: de decimals-mapping op de
onbegrensde vorm had géén clingo-encodering (→ vraag 4); de
stdlib-allowlist was misgefundeerd (de tutorial-som is de verouderde
oppervlaktevorm, 5b-gedekt — live geverifieerd); de exitgetuige draagt
zelf een meervoudige basisgroep (→ groep-defaults); de gesloten
body-whitelist miste FieldAccess; het guarded-consequence-werk in
clingo was onderschat; en de corpus-plaatsing van de tutorialtekst
botste met het geratificeerde rechtenhek (→ frontend-side). Vier
implementatiekaarten gesneden (structs, geneste groepen,
Let+decimals, subscopes+tutorial — de laatste sluit het formele
5a-exitcriterium en markeert PLAN §6). Alle negen gates groen (geen
artefactzone geraakt). Bewust gelaten: 2-4's multi-state en
per-element-subscope-calls, de directe attribuut-guard, besluit-6's
gegenereerde default, en de gedeelde basis onder alle leden — alle als
luide recorded flanken in het voorstel.
