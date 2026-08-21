# Veeg: de equality-leeg-encodering in clingo

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 3).

Goal: gelijk/ongelijk over mogelijk-lege operanden encoderen (de
ch. 1.2d-asymmetrie: leeg = leeg is waar onder gelijk-aan, onwaar
onder ordening) met def-helpers en case-analyse, in plaats van de
huidige luide fault; ankers pinnen de asymmetrie differentieel;
ASP-TRANSLATION S6/S14 en de recorded finding bijgewerkt.

Acceptance: rood-eerst (de fault vandaag); ankers door gates 5–6;
alle acht gates groen; twee lenzen.

May touch: `backends/clingo/`, `corpus/`, `spec/ASP-TRANSLATION.md`,
`backlog/`.

Out of scope: ref-wijzigingen (de semantiek staat).
