# check-admits-instance-cycle: `check` laat een kring over een schakelrelatie toe en noemt de relatie waar hij weigert

*Uit het Fable-advies van 2026-10-22 (`uchar-dev/research/bruggen-2026-10-22/fable-advies.md`, vraag 1, stappen 0 en
5), de frontendhelft van `cycle-across-instances`. Voorlopig besluit van de hoofdsessie naar dat advies.*

## Aanleiding

Na `cycle-across-instances` laten spec, verifier, ref, clingo en Drools een kring toe die alleen door verschoven randen
over een schakelrelatie gesloten wordt (de afbakening staat in die kaart). De frontend toetst de kringen zelf
(`Dependencies.java`, sinds `kringtoets-volgt-wf045`) en weigert zo'n kring nog met WF-045 vóór de emissie: `check`
weigert dan wat de verifier toelaat. En een kring die buiten de afbakening valt, weigert `check` met een melding die
niet zegt welk feittype twee objecten van één type verbindt.

## Doel

- `Dependencies.java` leest de verschoven randen zoals de verifier (dezelfde afbakening) en laat de kring toe; de
  getuige R1 van `cycle-across-instances` geeft `check` exit 0, en het IR is bytegelijk aan de golden
  `jutterswet-vergunningketen`, die daarmee een `source.rgs` krijgt (gate 10).
- Een kring die buiten de afbakening valt en over een reflexief feittype loopt, weigert `check` met een melding die dat
  feittype noemt en de uitweg (een eigen lid op het object, of de vorm binnen de afbakening).

## Acceptatie

- (a) Rood eerst: frontendtests met R1 (exit 0) en met een vorm buiten de afbakening (de melding noemt het feittype).
- (b) De golden `jutterswet-vergunningketen` emitteert byte-identiek vanuit haar nieuwe `source.rgs`; de zeventien
  gates groen.
- (c) Als meting, zonder bestand in deze repository: de v2-regelset herleving-WW met de verlenging in de maxdatum van
  het recht (zonder brug K12). Zolang de werkbank hww-06 niet herschrijft (normvraag B2.5 van Fable-advies 2), weigert
  `check` alleen de samenlooplezing, met een melding die de lezing noemt zoals `WF-045.instance-cycle-read-outside`
  van de verifier. Na de herschrijving geeft `check` exit 0.

May touch: `frontends/regelspraak/**`, `corpus/golden/jutterswet-vergunningketen/` en
`corpus/golden/jutterswet-vergunningtermijnen/` (alleen `source.rgs`), deze kaart. Start pas als
`instance-cycle-household-reads` geland is (Fable-advies 2, B2.4). Dan bouwt `Dependencies.java` de definitieve
afbakening in één keer.

De snippets van `percentageOf` stonden hier eerst. Ze zijn nu een eigen kaart: `percentage-of-snippets`.
