# Drools-ladderservice: tiers 0–4 (reduce-rule-port van ref)

Goal: volledige ladderservice in `backends/drools` per LADDER-EVOLUTIE §5 /
Q3+Q4 (geratificeerd 2026-08-29). Lowering-optie B: één reduce-rule per
(laddergroep, subject) — LHS het subjectpatroon (class-reactief +
navigatie-joins), RHS een directe Java-port van refs
`run_ladder_group`-loop (top-down sportwandeling, leeg-doorval, recursie in
`excepties`, groepsreconciliatie, één idempotente schrijf of één
`Inconsistentie`). Loader: `defaultLadders` in de TOP_LEVEL-allowlist,
nieuwe `DefaultLadder`/`Rung`/`SubLadder`-records in het gedeelde frontend
`ir/`-pakket (ná de omdraaiing — de typen landen één keer), lege lijst
expliciet geaccepteerd. Ladder-lokale salience: strikt onder elke producent
van elke sport-justificatie (verplicht vóór tier 2 — monotone kenmerken).
Luide `Dekking.skip`-fences: temporele laddertargets (ref-pariteit: beide
weigeren), kanaal-deponerende consequenties (deling — geen getuige),
niet-reduceerbare justificaties. `CaseRunner.consistentie` toetst voortaan
de §9.5-conflictsleutel (padsleutel) én het subject (Q4). Flip-volgorde
risico-oplopend: tier 0 (drie lege-lijst-goldens) → tier 1 attribuut
(vondstwaardering e.a.) → tier 2 kenmerk (uitsluiting, hardheid,
penningrecht-catala) → tier 3 ongeordend+conflict (toelagegronden) →
tier 4 genest (nestuitzondering, nestdiepte, nestkenmerk,
tariefrecht-catala, synth3-nestkorting).

Acceptance: gate 12 groen met de vijftien flip-goldens (67 cases) verplaatst
van auto-exclusie naar played — een miswaarde is een bug, nooit een
EXCLUDE_KNOWN-entry; de resterende exclusies dragen elk hun reden; alle
overige gates groen; `mvn -q -o -f backends/drools/pom.xml package` in elke
checkout vóór de gate-run. Bij de landing amendeert de hoofdsessie serieel
de gate-12-charterzin in CLAUDE.md ("the ported exec-model back-end" →
implementeert ook spec ch. 7 voorbij de geporteerde productiesemantiek).

May touch: backends/drools/ (loader, transpiler, runtime-aanroep, cases),
frontends/regelspraak/src/main/java/.../ir/ (alleen de drie nieuwe
records), scripts/check-drools.sh (exclusielijst-krimp), backlog/.

Out of scope: temporele laddertargets bouwen; de WF-hekwerk-kaart (Q12a);
frontend-emissie; toolchain-werk (de omdraaiing-kaart gaat vóór).

Result: gate 12 speelt 64 (was 53): tiers 1/2/4 volledig geflipt (tien van
de vijftien kaartkandidaten) plus het verse ww-herleving-inkomen-anker (4
cases, het 4-laags geneste kenmerk-ladder — speelt schoon, de vierkant
sluit erop). Lowering: reduce-rule-port van refs run_ladder_group
(besluit/gevuld-doorval, exceptie-eerst-recursie, reconciliatie,
pad-gesleutelde Inconsistentie, nooit-leeg-schrijven), records
DefaultLadder/Rung/SubLadder in het gedeelde frontend-ir-pakket,
ladder-lokale salience (min van producent-salienties − 1; de lens bewees
de dekking van de hele producentenkegel via de monotonie van de
stratificatiediepte), CaseRunner toetst nu conflictsleutel én subject
(Q4; regelnaam-sleutels alleen op sleutel — het exec-model hangt daar
legitiem een ander subject aan). EERLIJKE AFWIJKING van de
kaartacceptatie (vijftien beloofd, tien geflipt): drie kandidaatklassen
blokkeren op ladder-vreemde weigeringen, elk luid en reden-dragend —
(a) toelagegronden: het doel deelt als kenmerk zijn naam met een
attribuut en het bean-model draagt één veld per naam (nieuwe fence);
tier 3 (ongeordend+conflict) heeft daarmee GEEN corpus-getuige in gate
12 — de reconciliatie- en sleutelsemantiek is wél door de lens tegen ref
gekruisvalideerd op eigen boobytraps; een naamdeel-vrije publieke
ongeordend-getuige is geregistreerd vervolgwerk (jacht/synthese-golf);
(b) tariefrecht-catala: pre-existente Let-weigering; (c) de drie
tier-0-goldens: lege-lijst-acceptatie is gebouwd maar
ListType/SumType/StructType weigeren eerst. Regressielens: exact +11,
géén verlies onder de 53, de vier value-divergenties ongewijzigd,
EXCLUDE_KNOWN faalt-bij-veroudering aangetoond, scope strikt binnen may
touch, gate 10 byte-identiek (de records raken de emitter niet).
Lens-MINORs verwerkt: bevatDeling kijkt nu ook in Functie.leegwaarde
(defense-in-depth) en de salience-invariant van het conflictpad staat als
comment op ladderGroep. Alle twaalf gates groen. CLAUDE.md-charterzin en
toolchain-re-vendor+herpin: seriële landingsstappen van de hoofdsessie.
