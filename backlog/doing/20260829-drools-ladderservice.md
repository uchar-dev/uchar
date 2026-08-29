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
