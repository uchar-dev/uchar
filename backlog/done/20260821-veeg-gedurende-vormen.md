# Veeg: samengestelde gedurende-conditievormen

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 3).

Goal: de gedurende-conditievormen die v0 wel uitdrukt maar WF-055/056
en de vertaling nu buiten houden (samengestelde condities onder
gedurende) — spec-delta (WF-tekst + negatiefs die versmallen),
encodering in de clip/fold-machinerie, ankers differentieel.

Acceptance: rood-eerst; ankers door gates 5–6; verifier-delta met
negatiefs; alle acht gates groen; twee lenzen.

May touch: `spec/`, `backends/clingo/`, `ref/`, `corpus/`,
`backlog/` (ref alleen als de vorm daar ook hekt — expliciet melden).

Out of scope: anchored buckets en rate-restatement (aparte flank).

Result (2026-08-21): de platte composite-ALLE-clip is geland in beide
executors — de intersectie van de leden-ware-perioden (ref:
Timeline.intersect_periods-fold; clingo: paarsgewijze
clN_and-hulppredicaten op de gedeelde vier-gevallen-max/min); WF-055
verruimd met precies die vorm, anker jutterswet-dubbelwacht pint
overlap/disjunct/één-leeg/gespleten absoluut, drie nieuwe negatiefs
(comparison-leaf, nontemporal-leaf, nested). EXPLICIETE MELDING
ref-wijziging (kaartvoorwaarde): ref hekte de vorm vóór de wijziging
— een composite filter viel op de EVALUATION FAULT "unsupported
gedurende condition shape"; de wijziging raakt ref/lib/timeline.ml
(nieuwe intersect_periods) en ref/lib/eval.ml (de recursieve
conditie-arm), rood-eerst getoond op alle drie de poorten. De
lensronde ving twee echte mirror-fouten in mijn eerste versie, beide
vóór de merge hersteld: (1) de geneste composite is GEEN v0-gedrag —
v0 markert geneste bullets als Onopgelost — dus de nesting is uit het
anker en uit WF-055 gehaald (versmald naar plat; de oude nestregel
werd het negatief wf055-composite-nested) in plaats van als stille
v1-herschrijving genormeerd; (2) de motivering "v0's resolver gates
composite gedurende to characteristic members" was onjuist — v0
partitioneert een STATISCH vergelijkingslid juist als value-side
guard (AND-poort onder ALLE) en markert alleen
temporeel-attribuut-vergelijkingen en geneste bullets; de teksten in
RS ch. 3.6, IR-SPEC en de verifier dragen nu de precieze v0-grens en
benoemen de v1-versmalling (composite-leden = kenmerk-checks) als
recorded narrowing. Bewust gelaten, recorded in RS/S14: het
statische-guard-lid, de geneste bullet, de ten-minste-1-unie (alle
luid in beide executors) en het negatie-lid (ref-only, het
enkelblad-regime); WF-056/058 (samengesteld gedurende onder pro-rata
en dagentelling) is niet verruimd — de kaart dekte de clip.
