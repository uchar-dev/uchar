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

Result (2026-08-21): gate 9 staat — scripts/check-catala.sh bouwt de
plugin, draait de suite GEFORCEERD (een gecachte run print niets) en
verifieert de checks-telling uit de uitvoerregel, en regenereert elke
bron.catala_en-golden byte-identiek via gate 2's eigen serializer
(de slice wordt uit dat script geëxecuteerd — één vorm, één bron;
gate 2 draagt nu een ankercommentaar dat de slice benoemt zónder de
letterlijke zoekteksten — mijn eerste commentaar bevatte ze en brak
de extractie prompt, de voorspelde fragiliteit live gedemonstreerd en
gedicht). Rood-eerst, viermaal getoond met letterlijke meldingen:
een gemuteerde golden → "DIFF ... diverges" (exit 1); een rode suite
→ doorwerking met de MISMATCH-regels (44 checks, 2 failures → FAIL);
een lege golden-set → "no catala goldens found"; en het door de
breker-lens gevonden stille-groen-gat — de kale runtest-alias is een
lege-maar-geldige dune-standaard — is gedicht en rood bewezen: met de
alias hernoemd meldt de gate "the suite produced no '<N> checks, 0
failures' line — it did not run". python3-preflight toegevoegd.
CLAUDE.md draagt punt 9 mét de expliciete local-only-status: CI
handhaaft gates 1–8; de CI-leg is de open maintainersafweging over de
catala-dependency, getrackt in backlog/new/20260821-gate9-ci-leg.md
(de regime-lens wees terecht op de tweemaal uitgestelde,
ongetrackte draad — nu een kaart). Bewust gelaten: geen timeout op
de plugin-aanroep (macOS mist een draagbare timeout; recorded), en
suite.ml's eigen lege-lijst-guard blijft aan de gate-kant afgedekt
(nul checks faalt daar) in plaats van in de suite zelf.
