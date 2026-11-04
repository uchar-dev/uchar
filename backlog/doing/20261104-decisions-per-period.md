# decisions-per-period: wie op welke dagen besliste, per object en tijdlijndoel

*Opgesteld door de hoofdsessie op 2026-11-04, uit het voorstel van de toolchain-kaart `timeline-attribute-input` (toolchain `5184be1e1`).
De dienst geeft op `?explanation=true` bij een defaults-stapel die per dag beslist nu een 400 met naam, omdat `decisionsOf` zo'n build
weigert; deze kaart geeft de API die de uitleg per periode nodig heeft.*

## Goal

 Een consument kan per object en tijdlijndoel lezen wie op welke dagen besliste, zonder de verzoening per dag
zelf te doen. Voor een stapel per dag (RUNTIME-SEMANTICS ch. 7.11) geeft `DroolsBackend.decisionsOf` (of een
zustermethode) per object en stapel de maximale perioden met één uitkomst: de beslissende default (pointer zoals
`DefaultsDecision.Decider`, met de waarde die hij die dagen schreef), een conflict (sleutel en beslissers) of geen
beslisser (de invoer staat, of het doel blijft leeg), over alle tijd met open grenzen; voor een tijdlijndoel van gewone
schrijvers (ch. 3.11) per object en doel de perioden per schrijver (de vorm) en die van de invoer. Het leest de
controlefeiten en de `Outcome`-dagen (`PeriodDefaults.Days`) die de lowering al neerlegt en rekent niets opnieuw.

## Acceptance

- (a) Een publiek record per periode met javadoc in de termen van ch. 7.11 en 3.11; `decisionsOf` weigert zo'n build
  niet meer, of de zustermethode is de weg en `decisionsOf` noemt haar in zijn weigering.
- (b) Uitgewerkt voorbeeld, letterlijk als test: `jutterswet-opgave-per-periode` case4 → [2025-01-01, 2025-03-01)
  default `/defaults/0` (opgave, 10), [2025-03-01, 2025-06-01) `/defaults/1` (berekend, 20), [2025-06-01, 2026-01-01)
  geen beslisser (invoer 99); `anchor/jutterswet-borgdeel` case6 → [2024-01-01, 2024-03-01) de regel 'borgdeel van een
  jutter', [2024-03-01, 2024-04-01) de invoer.
- (c) Een witness over elke golden, rode case en fixture met een stapel per dag (`ref/test/fixtures/defaults-per-day`,
  `-cases`) of een tijdlijndoel met invoer, op beide takken: de beslissers per periode zijn die van ref
  (`run_defaults_timeline`, `decide_days`), en de vereniging van hun waarden met de invoer is de tijdlijn van het
  fixpunt.
- (d) Rood eerst; per bewaking een mutatie die een test rood maakt; `make gates` groen.

Daarna, in de toolchain: `Verbaliser` noemt per periode welke default of regel besliste en op welke dagen de invoer
bleef staan, `Verbaliser.unexplained` en `EXPLANATION_REFUSED` vervallen.

May touch: `backends/drools/**` (de publieke API en haar witness), `spec/RUNTIME-SEMANTICS.md` (alleen als ch. 5.12 of 7.11 een zin over
de beslissers per periode vraagt; voorstel), kaartverplaatsing.

## Omvang

Klein tot middel.

## Result
