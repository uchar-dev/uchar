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

Result: alle vier stukken geland op de branch. (1) De
waardering-v0vorm-redenregel stelt nu onomwonden:
exec-model-non-terminatie, DRL-oracle ≡ ref ≡ clingo op 25 — geen
semantische divergentie. (2) De keurmerk-fix: de exec-model-tekstlezing
is nu VOLLEDIG verbatim (de ref-spiegel; corpus-breed gedragsgelijk —
de lens bewees dat de gepaarde-delimiter-strip op geen enkel bestaand
golden vuurde en ving het residu dat een geheel-omquote-de waarde alsnog
zou strippen); keurmerk speelt tegen ongewijzigde pins,
EXCLUDE_KNOWN 4→3, beide staleness-richtingen geboobytrapt. (3) De
publieke Onbekend-getuige jutterswet-stukopbrengst (190/112,
handberekend) doorloopt aantoonbaar de numeric_ok-rekenkunderecursie
(break-the-arm-bewijs) en speelt het volle vierkant — MAAR vergde een
WF-034-verruiming (onbekend_numeriek: een bewijsbaar-numerieke
Onbekend-Binair is toegelaten, een veilige DEELverzameling van clingo's
toelating; alle wf034/wf003-negatiefs blijven verwerpen) die NIET in de
geratificeerde besluitenset zat — als genummerde vraag aan de maintainer
voorgelegd vóór de merge. (4) WF-086 weigert temporele laddertargets op
document-niveau, ná de lensvangst verbreed naar BEIDE armen: attribuut
én kenmerk (het temporele kenmerk gleed er stil langs en zou atemporeel
evalueren) — twee rood-eerst-negatiefs, ref draagt beide
defense-in-depth-faults. Lens-formuleringsfix: de
"admits exactly"-claims versmald naar safe-subset. Gate 12: played 69,
value-divergence 3. Alle twaalf gates groen op de branch.
