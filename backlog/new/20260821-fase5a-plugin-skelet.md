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
