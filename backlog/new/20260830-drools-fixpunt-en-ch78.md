# Drools: exec-model-fixpunt op ongeankerde temporele writes + de ch.-7.8-standing-target-arm

Goal: De twee defectklassen die de temporele input-poort blootlegde (kaart
20260830-drools-temporale-inputpoort; de (c)/(d)-entries in `EXCLUDE_KNOWN`
van `scripts/check-drools.sh`) worden gebouwd, gepaard omdat
`anchor/jutterswet-borgdeel` alleen met beide groent. (c) TERMINATIE: een
ongeankerde temporele write krijgt in `ExecModelTranspiler` de
§9.9-freshness-expr en blijft class-reactief, terwijl de
`rsTijdreeksGelijk`-idempotentiegarde alleen geankerde writes dekt (de
`geankerd &&` op `ExecModelTranspiler.java:442/452`) — siblings heractiveren
elkaar voorbij de 100k-cap (`anchor/jutterswet-borgdeel`,
`synth2-prorata-breed`; de waarden zelf zijn niet in geschil). Verwant,
zonder corpusgetuige: de `rsTijdSomAlle`-tak voedt `werkBij` een
`List<Segment>` waarop `Objects.equals` altijd ongelijk is (`Segment` heeft
geen `equals`) — zelfde klasse, andere plek; meenemen of expliciet
uitsluiten. (d) IMPLEMENTATIEGAP: de ch.-7.8-standing-target-arm — op een
staand temporeel doel schrijft een lege foldopbrengst niets en blijft de
case-geleverde input-tijdlijn staan — is besloten spec (ref:
`ref/lib/eval.ml` over `Desugar.standing_temporal_targets`; clingo draagt
hem ook), maar `backends/drools` heeft er geen spoor van
(`anchor/jutterswet-dagentelling` case4; `anchor/jutterswet-borgdeel`
case5-input-blijft). Na beide fixes verlaten de drie entries de
value-divergence-lijst (het stale-regime dwingt dat af) en stijgt gate 12
naar 82 played / 3 value-divergence.

Maintainerratificaties 2026-08-30, hier verankerd: de bannerklassen
(c)/(d) in `EXCLUDE_KNOWN` zijn geratificeerd zoals geland (deze kaart
ruimt de drie entries op); de package-private seams
(`CaseRunner.invoerBeans`/`speelCase`) zijn de geratificeerde testroute —
gate 1's allowance voor de publieke replay-methodenaam blijft file-scoped
op de twee bestaande bestanden (testbestanden noemen die naam dus nooit).

Acceptance:
- Rood-eerst: de drie huidige FAIL-uitkomsten (2× geen fixpunt, 1×
  overschreven input-tijdlijn) vóór de fix getoond.
- `bash scripts/check-drools.sh` → PASS met played 82, value-divergence 3
  (uitsluitend de drie pre-existente v0-divergentie-entries), auto-excluded
  50, no-case 1; geen verwachtingswaarde gewijzigd. NB: de nulmeting is
  81/50/6/1 sinds de wederkerig-self-relation-kaart (2026-08-30).
- Gate 12 eist voortaan een niet-nul testtelling van de backend-JUnit-suite
  (het gate-10-model; maintainerbesluit 2026-08-30): het script draait de
  suite of verifieert een verse run, en een nul-testrun of overgeslagen
  suite is een failure, nooit stil groen — rood-eerst getoond.
- Alle twaalf gates groen; de JUnit-suite van `backends/drools` groen met
  nieuwe witness-tests voor beide fixes.

May touch: `backends/drools/src/main/java/**` (ExecModelTranspiler,
runtime-helpers), `backends/drools/src/test/**`, `scripts/check-drools.sh`
(het verwijderen van de drie entries plus de testtelling-eis), `backlog/`.

Out of scope: de DRL-transpiler (convergeert al), ref/clingo/spec/corpus,
de CaseRunner-taxonomie-herziening (geratificeerd uitstel bij
`jutterswet-waardering-v0vorm` — die entry blijft), objectcreatie,
wederkerig self-relation.
