# Fase 5c, kaart 4 — subscope-calls en de tutorial-exitgetuige

Goal: PHASE5C-TUTORIAL §3 als lowering op bestaande kernel (geen
nieuwe knoop): EScopeCall → ResolvedObjectCreatie van het
callee-objecttype met args als Toekenningen op input-attributen +
verbindendeRol + genavigeerde AttribuutRef voor outputs; de
callee-regels draaien in de caller-fixpoint; per-element-calls,
multi-state en context-inputs blijven luide recorded flanken
(besluit-6's gegenereerde rung geratificeerd-maar-uitgesteld, met de
WF-050-randvoorwaarde). Daarbovenop de exitgetuige (vraag 3: A):
tutorial_end_2_3 als frontend-side E2E-fixture (plugin → verifier →
ref → clingo; het corpus blijft rechtenvrij per besluit 2), naast een
eigen-geschreven Jutterswet-subscope-golden dóór het corpus met de
volle gate-set. Bij groen valt het formele 5a-exitcriterium: PLAN §6
Phase 5 krijgt zijn Met-markering voor dat criterium (PLAN-update in
deze kaart).

Acceptance: alle negen gates groen; de tutorialfixture drievoudig
groen; rood-eerst; herpin; twee opus-lenzen met bevindingen gefixt
vóór de merge; spec/PLAN.md-markering.

May touch: als kaart 1 plus spec/PLAN.md. Out of scope: 2-4
(states/per-element-calls); besluit-6-implementatie.
