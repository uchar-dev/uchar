# De graduatie: IR-SPEC en RUNTIME-SEMANTICS lezen zelfstandig; Phase 1 is af

Goal: de vastgelegde slotstap van WAY-OF-WORKING §6. De twee normatieve
documenten verliezen hun autoriteits-pointers naar de private toolchain
(de header-blokken die dat vertrek zelf al aankondigen, de § Method-naam
en de vergelijkingstabel-kolom) — herkomst blijft in het PLAN, de
backlog en de git-historie — en treden toe tot de
zelfstandigheidsgate, zodat gate 1 het blijvend afdwingt. CLAUDE.md's
gate-1-beschrijving en WAY-OF-WORKING §6 worden op de nieuwe stand
gebracht; het PLAN krijgt de Phase-1-exit-vaststelling (alle
exit-criteria staan: de verifier laadt en verifieert zelfstandig, golden
corpus en round-trip gaten CI, elke WF-regel draagt check + golden +
negatief of een gemotiveerde OPEN in de rij, het corpus is rechtenvrij).
Het public-flip-besluit (PLAN §9, besluit 10) blijft aan de maintainer.

Acceptance: alle acht gates groen, met gate 1 nu ook over
spec/IR-SPEC.md en spec/RUNTIME-SEMANTICS.md (aantoonbaar: een
regelspraak-java-verwijzing in een van beide laat gate 1 falen — rood
getoond, dan groen); geen private-repo-naam of machine-lokaal pad in de
twee documenten; PLAN/WAY-OF-WORKING/CLAUDE.md consistent.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md,
scripts/check-self-contained.sh, WAY-OF-WORKING.md, CLAUDE.md,
spec/PLAN.md, backlog/.

Out of scope: de public-flip zelf en branch-protection (maintainer);
inhoudelijke spec-wijzigingen; CONSTRUCTS/IR-REVIEW (werkprose mag de
naam blijven dragen).
