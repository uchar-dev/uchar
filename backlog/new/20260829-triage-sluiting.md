# Triage-sluiting: redenregel, keurmerk-fix, Onbekend-getuige, ladder-WF-hek

Goal: de sluitkaart van LADDER-EVOLUTIE §9 + Q11/Q12/Q13 (geratificeerd
2026-08-29), vier stukken klein maar precies werk. (1) De
`waardering-v0vorm`-redenregel in scripts/check-drools.sh herschreven:
exec-model-non-terminatie, DRL-oracle ≡ ref — geen semantische divergentie
(herclassificatie alleen als de CaseRunner-taxonomie ooit opnieuw
opengaat). (2) De keurmerk-fix: de exec-model-tekstvergelijking in
backends/drools draagt voortaan een ingebed aanhalingsteken correct; de
keurmerk-golden verhuist van value-divergence naar played (de quote-vrije
variant speelde al — de fix is precies het ingebedde teken). (3) De
Onbekend-flip-getuige: één publieke Jutterswet-golden die de
numeric_ok-rekenkunderecursie uitoefent (een Onbekend-getypeerde operand
bewijsbaar numeriek in een Binair), ref ≡ clingo met handgepinde waarden —
het flip-checklistpunt; arm én golden gaan samen met pensioen bij de
v1-WF-003-promotie. (4) Het WF-hek voor temporele laddertargets (Q12a):
een wire-`DefaultLadder` op een temporeel attribuut wordt
document-niveau-geweigerd (nieuwe WF-regel + verifier-check), ter
vervanging van de huidige executor-only ref-fault; rood-eerst met een
negatief.

Acceptance: gate 12 groen met keurmerk in de played-telling en de
herschreven redenregel (lijst faalt bij veroudering — telling klopt);
gates 3/5/6 groen met de nieuwe Onbekend-golden en het
wf-temporele-ladder-negatief (rood-eerst getoond); alle overige gates
groen; geen toka-verwijzingen in spec-tekst.

May touch: scripts/check-drools.sh, backends/drools/ (string-encoding),
spec/IR-SPEC.md (de nieuwe WF-regel), spec/verifier/, corpus/golden/
(Onbekend-golden), corpus/negatief/ (het temporele-ladder-negatief),
backlog/. Beschermde paden: onder het mergemandaat na gates+lenzen.

Out of scope: temporele laddertargets bouwen; het allocatie-invoerhek
(wacht op de §9-executorkaart, per Q12b); gedeelde sub-root-basis (blijft
geweigerd, per Q12c); CONSTRUCTS/PLAN (hoofdsessie serieel).
