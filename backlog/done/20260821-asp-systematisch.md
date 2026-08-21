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

Result (2026-08-21, deel 1): Part I staat in ASP-TRANSLATION.md —
S1 programmavorm en de twee faalkanalen (inconsistent/2 als
modelburger, viol/2 als UNSAT-hek), S2 case-input met de
standing-routering, S3 regels en de fitted-schrijfgrens, S4
expressietermen, S5 de drie leeg-encoderingen, S6 rekenkunde met de
volledige hekkentabel, S7 condities, S13 de kanaalsleutels, S14 de
volledige luide-flanken-inventaris. Structuurkeuze gemotiveerd: de
bestaande hoofdstukken 1–7 blijven in situ met hun nummers als
Part II (historical) — Results, kaarten en code-commentaar citeren
ch. 6a–6d; hernummeren zou die ankers breken. S8–S12 (folds,
tijdlijnen, defeasibility, creatie, allocatie/uniciteit) zijn
expliciet aangekondigd, geen stille SKIP; hun luide flanken staan al
volledig in S14. De vervolgkaart 20260821-asp-systematisch-2 draagt
ze. Twee opus-lenzen (tekst-tegen-code, norm-en-getuigen) vonden elf
echte gebreken die alle vóór de merge zijn gefixt, o.a.: de dividend
stond ten onrechte in de coalesce0-whitelist (het is een
default-head op het quotiëntpredicaat), Samengesteld dekt óók de
DE-kwantor, de desugar-claim was omgekeerd (ref replayt v0 natief —
gate 6 getuigt de desugaring zelf), een getuige wees naar het
verkeerde geval, en de kanaal-guard-claim was te breed. De
BLOCKING-vondst (nul-deler als viol vs ch. 1.4's kanaalroute) bleek
bij naslag GEEN open besluit: ch. 1.4 punt 4 legt beide vast — de
v0-mirror hard-fault blijft de spiegel, v1 routeert naar het kanaal —
dus de fix was de ontbrekende tracked-delta-vermelding (S6 en S14),
geen genummerde maintainersvraag. Bewust gelaten: de ch. 1.2a/1.2d-
subletters blijven staan (repo-brede conventie, ook in
code-commentaar en Part II); corpus/rood/division-by-numeric-zero/
NOTE.md attribueert de hard fault aan besluit 7 — dat is ch. 1.4
zelf, dus verdedigbaar, maar de formulering kan preciezer (buiten
may-touch; kaartkandidaat).
