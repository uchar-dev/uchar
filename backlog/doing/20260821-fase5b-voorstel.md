# Fase 5b — het kernontwerpvoorstel: sum types (rij 36) en folds/function values (rij 10)

Goal: één ontwerpdocument `spec/proposals/PHASE5B-KERNEL.md` dat beide
kernel-uitbreidingen spec-first uitwerkt — (1) sum types met payload en
compile-time-totale match, (2) general folds en function values (DMN's
BKM+invocation onder hetzelfde ontwerp) — elk met: de nodevormen en hun
plaats in IR-SPEC, de WF-regels (nieuwe nummers vanaf WF-060) met hun
negatiefs, de RUNTIME-SEMANTICS-normtekst in schets met worked examples,
de executor-strategie voor ref én clingo (luide clingo-faults toegestaan
mits als S14-item benoemd), de case-formaat-uitbreiding, de
CONSTRUCTS-delta's voor rijen 10/36, en de plugin-mapping vanaf
scopelang. De échte kernkeuzes gaan als genummerde vragen naar de
maintainer vóór enig spec-werk merget (het WF-059-precedent); de genomen
besluiten worden in het document als Decided vastgelegd.

Acceptance: `spec/proposals/PHASE5B-KERNEL.md` bestaat en is door twee
opus-lenzen gehaald met bevindingen gefixt; elke kernkeuze draagt een
maintainer-antwoord (Decided, met datum); de implementatiekaarten
`20260821-fase5b-sumtypes.md` en `20260821-fase5b-folds.md` staan in
`backlog/new/` met acceptance in gate-termen; gates 1–9 blijven groen
(dit voorstel raakt geen artefactzones).

May touch: spec/proposals/PHASE5B-KERNEL.md (nieuw), backlog/.

Out of scope: elke wijziging aan IR-SPEC.md, RUNTIME-SEMANTICS.md,
spec/schema/, spec/verifier/, ref/, backends/, frontends/, corpus/ —
die volgen in de implementatiekaarten na ratificatie.
