# Fase-3-exitverificatie: role bindings, dan de Met-markering

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 1).

Goal: het tweede exitcriterium van PLAN §6 fase 3 — "role bindings
replace bean-field names in the IR" — formeel natrekken: IR-SPEC (de
rol-/navigatievormen, WF-053), het schema, de vertaler (rol/3 op
symbolische namen) en de emitterkant (geen zetVeld/joinVeld/scopeVeld
meer op de wire). Bij bevestiging: de "Met"-markering met datum en
bewijsverwijzingen in PLAN §6 fase 3, naar het model van de fase-1/4-
markeringen. Elke bevinding die het criterium ontkracht gaat als
genummerde vraag naar de maintainer vóór er iets gemarkeerd wordt.

Acceptance: een Result met het bewijs per deelclaim; PLAN §6 fase 3
draagt de Met-markering (of het Result legt uit waarom niet); alle
acht gates groen.

May touch: `spec/PLAN.md`, `backlog/`.

Out of scope: wijzigingen aan spec/schema/vertaler zelf.

Result (2026-08-21): criterium 2 formeel nagetrokken. Bewijs: de
uchar-executors lezen zetVeld/joinVeld nergens (grep ref/ en
backends/clingo — nul treffers buiten de verifier-loader, die ze
alleen voor WF-028 laadt); een bean-only feitcreatie fault luid; de
scopeVeld-fallback is rolnaam-resolutie tegen de gedeclareerde rollen
(role_by_name), levend corpus-gedekt door een kustregister-uniciteit
zonder scopeRol; WF-028/053/054 bestaan als verifier-checks met
negatiefs; alle feitcreaties in het corpus dragen de symbolische vorm
naast de bean-velden. De lens-tegencontrole ving drie echte
correcties vóór de merge: de afleiding rolbinding→veld zit in de
frontend-schema/resolver van regelspraak-java (RuleChecker), niet in
de Drools-transpiler (die consumeert het resultaat); "the forcing
move, made" was te sterk — de v0-wire dráágt de bean-velden nog als
inerte mirror-bagage (de syntactische verwijdering is de getrackte
v1-migratie); IR-SPEC-citatie 5.9→3.6; en het review-trio is
{zetVeld/joinVeld, scopeVeld, backref} — backref landde als de
subject-side rol van RoleMembership (rij 5), navigatie/WF-053 is de
aanvullende vierde. De strikte lezing van "replace ... in the IR"
(syntactische verwijdering) ontkracht het criterium — conform het
kaartprotocol als genummerde vraag aan de maintainer voorgelegd;
de maintainer ratificeerde de semantische lezing (A), waarna de
markering geherformuleerd is met de mirror-status expliciet, de
juiste afleidingsplek en de corpus-scoped-kwalificatie (de
fase-4-huisstijl). PLAN §6 fase 3 draagt nu de Met-markering.
