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
naar 84 played / 3 value-divergence *(bij het schrijven stond hier 82, op
de verouderde nulmeting 79 — de werkelijke nulmeting is 81, zie de NB
onder Acceptance; 81+3=84, geamendeerd bij de landing)*.

Maintainerratificaties 2026-08-30, hier verankerd: de bannerklassen
(c)/(d) in `EXCLUDE_KNOWN` zijn geratificeerd zoals geland (deze kaart
ruimt de drie entries op); de package-private seams
(`CaseRunner.invoerBeans`/`speelCase`) zijn de geratificeerde testroute —
gate 1's allowance voor de publieke replay-methodenaam blijft file-scoped
op de twee bestaande bestanden (testbestanden noemen die naam dus nooit).

Acceptance:
- Rood-eerst: de drie huidige FAIL-uitkomsten (2× geen fixpunt, 1×
  overschreven input-tijdlijn) vóór de fix getoond.
- `bash scripts/check-drools.sh` → PASS met played 84, value-divergence 3
  (uitsluitend de drie pre-existente v0-divergentie-entries), auto-excluded
  50, no-case 1; geen verwachtingswaarde gewijzigd. NB: de nulmeting is
  81/50/6/1 sinds de wederkerig-self-relation-kaart (2026-08-30); het
  eerdere "82" hier was een rekenfout op de verouderde nulmeting 79
  (geamendeerd bij de landing: 81+3=84).
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

Result (2026-08-30): Geland op main na één bouwronde en twee seriële
adversariële lensrondes met elk een fixronde. (c) TERMINATIE: één gedeelde
`werkBijTijdlijn`-helper vervangt vier losse tijdlijn-writes; de
`rsTijdreeksGelijk`-idempotentiegarde geldt nu voor elke tijdlijn-write
(de `geankerd &&`-scoping is weg) — bewijsbaar semantiek-neutraal: de
garde slaat alleen segment-voor-segment-gelijke herberekeningen over
(lens 1 verifieerde veldcompleetheid van de vergelijking; lens 2 de
null≡[]-flank). De verwante, corpusloze `rsTijdSomAlle`/`Segment.equals`-
bug loopt door dezelfde helper en is op unit-niveau gepind (geen
corpus-golden draagt die vorm — een corpusgetuige vergt een nieuwe golden
door frontend+ref+clingo, bewust gelaten). (d) DE CH.-7.8-TIJDLIJN-ARM:
`staandeTemporeleDoelen()` poort ref's `Desugar.standing_temporal_targets`
clausule-voor-clausule (beide lenzen legden ref er regel voor regel naast;
lens 1 draaide bovendien een eigen ref-probe op de niet-staande flank:
ref wist daar, Drools ook); op een staand doel schrijft een lege opbrengst
niets en blijft de case-input staan — beide kanten witness-gepind én door
lens 2 als losse cases door ref bevestigd. De kenmerk-callsite krijgt de
arm expliciet nooit (WF-002-naamdeling, lens-2-vondst; corpus-neutraal,
het domein draagt alleen attribuut-doelen). GATE-12-TESTTELLING: de gate
bouwt jar én suite in één onvoorwaardelijke `mvn package` (de
stale-jar-flank die lens 1 vond is daarmee dicht), parseert de
surefire-samenvatting niveau-onafhankelijk en eist tests>0 én skipped==0
(rood bewezen: skip-vorm, nul-vorm, geen-samenvatting-vorm); de
auto-exclusieredenen worden nu gegroepeerd geprint (de dode AUTO_REASONS
uit de landing van de gate, lens-2-vondst).

Gate 12: 81/50/6/1 → 84 played / 50 auto-excluded / 3 value-divergence /
1 no-case; de drie resterende divergenties zijn exact waardestelling,
waardering-v0vorm en kustwacht — de drie geratificeerde entries van vóór
de temporele poort. Backend-JUnit 32 → 45/0 (8 corpus- + 5
unit-witnesses). Alle twaalf gates integraal groen (160/168, 396, 374,
374, 338+36, 22, 169+11, 144+23, toka 28/1/0); beide lenzen
reproduceerden de tellingen onafhankelijk. Geen corpus-, rood-, toka- of
spec-bestand geraakt; geen verwachting, pin of WF-nummer bewogen.
CLAUDE.md's gate-12-paragraaf is feitelijk bijgewerkt (gedragen vormen,
testtelling, geprinte redenen, underivable-role-arm) — beschermd pad,
onder het mergemandaat na de lensronde. De kaarttekst zelf is geamendeerd:
het oorspronkelijke "82" was een rekenfout op een verouderde nulmeting
(81+3=84, lens-1-MAJOR).

Bewust gelaten en vastgelegd: de `count==1`-schrijverspoort en
`andereSchrijver` zijn corpus-onfalsifieerbaar (geen golden draagt twee
schrijvers op één temporeel doel — lens-2-NOTE); de zes resterende
temporele weigeringsvormen in de case-inputlaag treffen geen golden; de
attribuut/kenmerk-naamdeling op één beanveld blijft een pre-existente,
gemaskeerde limiet. Nieuw kaartvoorstel uit de bouwronde, door lens 2
onderschreven: `backlog/new/20260830-drools-scalaire-standing-norm.md` —
de scalaire ch.-7.8-poort die waardestelling (nu verkeerd geklasseerd als
v1-delta-kandidaat) en mogelijk waardering-v0vorm uit de divergentielijst
haalt.
