# Fase 5c — het ontwerpvoorstel: structs, subscope-calls, geneste groepen, de tutorial-route

Goal: één ontwerpdocument `spec/proposals/PHASE5C-TUTORIAL.md` dat de
resterende weg naar het formele 5a-exitcriterium (de Catala-tutorial
door de plugin, drievoudig geverifieerd) spec-first uitwerkt: (1) de
struct-representatie — de open kernkeuze: eigen record-datatype versus
lowering op objecttypen; (2) subscope-calls langs de al in PLAN §6
benoemde richting (object type + object creation + navigation),
inclusief de geratificeerde top-rung-realisatie van besluit 6 als
gegenereerde rung; (3) geneste ladder-groepen per de geratificeerde
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
