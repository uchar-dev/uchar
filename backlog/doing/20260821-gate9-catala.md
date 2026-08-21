# Gate 9: de Catala-frontend-conformance

Besluit maintainer 2026-08-21 (/goal complete gate 9, na het advies
in de sessie): de plugin-correctheid rust niet langer buiten de
gate-set.

Goal: scripts/check-catala.sh — de gate bouwt de plugin, draait de
volledige plugin-suite (frontends/catala/test), en eist
frontend-conformance: elke corpus/golden/**/bron.catala_en wordt door
de plugin geregenereerd en moet ná canonicalisatie (dezelfde vorm als
gate 2's serializer) byte-identiek zijn aan de ingecheckte ir.json —
het toolchain-conformance-patroon gespiegeld naar de frontend-kant.
Een lege golden-set of een ontbrekende catala-installatie faalt luid,
nooit stil groen. CLAUDE.md's gate-lijst krijgt punt 9 (de
gate-set groeit monotoon).

Acceptance: rood-eerst getoond (een gemuteerde golden en een rode
suite laten de gate falen met de juiste melding); daarna alle NEGEN
gates groen; twee lenzen.

May touch: `scripts/check-catala.sh` (nieuw), `CLAUDE.md`
(gate-lijst — de bekende protected-pad-dekking van het mandaat),
`backlog/`.

Out of scope: CI-opname (.github — de catala-dependency in CI is een
aparte afweging, expliciet recorded); plugin-gedragswijzigingen.
