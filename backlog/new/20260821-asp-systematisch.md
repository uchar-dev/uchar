# De systematische ASP-vertaling: het fase-3-deliverable

Besluit maintainer 2026-08-21: gekozen in het werkmenu.

Goal: ASP-TRANSLATION.md groeit van handoefeningen naar het
aangekondigde fase-3-deliverable: een systematisch, construct-voor-
construct vertaalhoofdstuk voor de kern-IR (per nodeklasse: de
ASP-vorm, de hekken, de leeg-encodering, de kanaalfeiten), gedistil-
leerd uit wat backends/clingo vandaag doet — de tekst normeert wat de
code al bewijst, met verwijzingen naar de gates als getuigen. Wat de
backend niet vertaalt blijft een expliciete luide-fault-vermelding
(nooit een stille SKIP). Meerdere sessies toegestaan; per sessie een
afgerond hoofdstukdeel.

Acceptance: elk behandeld construct citeert zijn gate-getuigen; geen
normatieve drift t.o.v. RUNTIME-SEMANTICS (de lenzen checken tekst
tegen code); alle acht gates groen.

May touch: `spec/ASP-TRANSLATION.md`, `backlog/`;
`backends/clingo/README.md` voor verwijzingen.

Out of scope: gedragswijzigingen in de backend; nieuwe constructen.
