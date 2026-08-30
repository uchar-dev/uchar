# Drools: vormcontrole op meerdelige temporele uniciteitssleutels

Goal: De Drools-backend handhaaft de vormcontrole die ref en clingo al
dragen: een uniciteitssleutel (uniciteit én uniciteitUnie) met meer dan één
temporeel sleutelcomponent is als vorm ongeldig en moet observeerbaar falen
in plaats van stil tot een model te lopen. Nu lowert `ExecModelTranspiler`
per component een `rsOoitBotst`-predicaat zonder ariteitscontrole, waardoor
twee rood-cases als NOFAULT gelijst staan in de Drools-exclusielijst van
`scripts/check-rood.sh` ("the v0 backend enforces no ARITY restriction on
temporal key components"). Maintainerbesluit 2026-08-30: klein kaartje,
ingepland na de fixpunt/ch-7.8-kaart; niet urgent — de lacune is luid
gelijst en machinaal bewaakt.

Acceptance:
- Rood-eerst: de twee NOFAULT-uitkomsten vóór de fix getoond.
- `sh scripts/check-rood.sh` → PASS; de twee entries verlaten de
  exclusielijst en pinnen elk hun `expectedFault.drools`-fragment
  (ref/clingo-fragmenten onaangeroerd; het script-regime dwingt dit af:
  een gelijste case die nu FAULT is een stale entry).
- Alle twaalf gates groen; gate 12 ongewijzigd (geen golden draagt de
  vorm); geen verwachtingswaarde gewijzigd.

May touch: `backends/drools/src/main/java/**` (de uniciteits-lowering of
een load-tijd-vormcontrole — volg ref's plek en strekking),
`backends/drools/src/test/**`, `scripts/check-rood.sh` (uitsluitend de
twee entries), `corpus/rood/*/case*.json` uitsluitend voor het TOEVOEGEN
van `expectedFault.drools`, `backlog/`.

Out of scope: ref/clingo/verifier/spec/corpusverwachtingen, de overige
rood-exclusies, gate-12-gedrag.
