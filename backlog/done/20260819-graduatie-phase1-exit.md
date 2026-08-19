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

Result: geland zoals gesneden. IR-SPEC en RUNTIME-SEMANTICS lezen
zelfstandig en zitten in gate 1 (rood getoond op een geplante
verwijzing, daarna groen); WAY-OF-WORKING §6, CLAUDE.md's
gate-1-beschrijving en het PLAN dragen de nieuwe stand; de
Phase-1-exit is vastgesteld en het public-flip-besluit (PLAN §9,
besluit 10) ligt bij de maintainer, samen met branch-protection.

Verificatievangsten (2 opus-lenzen), gefixt vóór de merge: (1) MAJOR,
door beide lenzen onafhankelijk — de graduatie-headerzin was onwaar
("the pinned v0 source lives in PLAN §5 ... never here" terwijl de
werkpins in § Method staan en §5 ze niet draagt) — herformuleerd: §5
draagt de herkomst (welke toolchain, de reporelatie), de werkpins
staan benoemd in de Method en resolven daar en in de git-historie;
(2) de WF-index-rubric eiste "every row closed" waar de exit op
gemotiveerde OPENs steunt — de rubric benoemt nu de drie resterende
gemotiveerde OPENs (WF-008/010/019-negatief) expliciet; (3) een
hernoemd normatief document zou door de [ -e ]-guard stil groen
skippen — de gate eist nu het bestaan van beide documenten. Bewust
gelaten: kale toolchain-commit-hashes (edecb46/fe46e49/446dfa6) in de
pin-notities — herkomstneutraal en via §5 + git-historie traceerbaar;
de generieke rolwoorden ("the emitter", "the v0 resolver") zijn
toolchain-rollen, geen namen.
