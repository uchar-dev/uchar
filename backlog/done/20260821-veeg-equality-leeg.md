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

Result (2026-08-21): de numerieke ch. 1.2d-gelijkheidsasymmetrie is
geëncodeerd — per vergelijking een eqN-helper (beide operanden binden
en de geharmoniseerde waarden matchen, óf geen van beide bindt);
ongelijk-aan is NAF over de helper, het exacte complement dat de norm
alleen numeriek toestaat. Rood-eerst: de conditie faultte terwijl ref
alle vier hoeken al draaide. Anker jutterswet-sluitregister pint
gelijk/verschil/half-leeg/beide-leeg differentieel — case4 (leeg =
leeg WAAR waar elke ordeningsrij onwaar zegt) is de asymmetrie zelf.
De vergelijk_sym-fault versmalt tot de clip-route. De lenzen: de
breker bevestigde de encodering op alle geoefende routes (schalen,
coalesce0-interactie — een gecoalescete operand bindt altijd, dus de
beide-leeg-tak is daar terecht dood, differentieel gelijk aan ref —,
datums, consistenties, ladder-rungs, element-filters, meervoudige
gelijkheden) en kon alleen divergeren op verifier-VERWORPEN input
(eigen-target-cykels, gemengde typen): de bestaande, nu expliciet
vastgelegde vertrouwensaanname dat clingo verifier-schone input
krijgt — gate 3 bewaakt dat corpus-breed. De norm-lens ving een echte
versmalling in mijn eerste S14-herschrijving: de NIET-numerieke
gelijkheidstak (tekst/enum/boolean — élke lege operand onwaar, dus
geen complement) fault nog als unscalable en staat nu weer expliciet
in de recorded-lijst plus gescoopt in S5. Bewust gelaten: de clip-
segmentgelijkheid en de niet-numerieke tak (beide luid, recorded).
