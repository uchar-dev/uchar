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
  het recht (zonder brug K12) geeft `check` exit 0.

May touch: `frontends/regelspraak/**`, `corpus/golden/jutterswet-vergunningketen/` (alleen `source.rgs`), deze kaart.
Start pas als `cycle-across-instances` geland is.

## Mee in deze tak: de snippets van `percentageOf` (2026-10-22)

Uit het Fable-advies over de uitleg van berekende waarden (`uchar-dev/research/uitleg-berekende-waarden-2026-10-22/fable-advies.md`,
§1.4, §5 U5, beslispunt 6), op aanwijzing van de eigenaar direct op te lossen in de eerstvolgende frontendtak: beide
operanden van `het percentage X van Y` dragen de brontekst van de hele uitdrukking als snippet
(`ExpressionBuilder.java` r. ±3039–3053: `new Expr.ParameterRef(pct, source)` en `attributeRef(…, source)` krijgen de
`source` van de `Binary`). Een uitlegzin die operanden met hun waarde noemt, noemt dan twee keer de hele frase; het
raakt de herlevingsgrens van v2. De `ParameterRef` krijgt de bron van het eerste naamdeel, de operand V die van de
overige delen met het onderwerp. Rood eerst: een frontendtest die de snippets van beide operanden pint; elke golden met
een `percentageOf` opnieuw uitgegeven (gates 2, 10 en 16); de verifier en de uitvoerders lezen geen snippet.
