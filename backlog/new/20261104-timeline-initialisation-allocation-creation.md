# timeline-initialisation-allocation-creation: initialisatie, verdeling en waarde bij het maken op een tijdlijndoel

*Opgesteld door de hoofdsessie op 2026-11-04, uit vraag 3 van `writers-as-one-defaults-stack` (RUNTIME-SEMANTICS ch. 3.12 rij 10).*

## Aanleiding

Een initialisatie buiten een defaults-groep, een verdeling en de waarde die een creatie bij het maken zet, weigeren alle uitvoerders op
een tijdlijndoel bij naam (ch. 3.12 rij 10); ref schreef daar eerder stil een scalair. Op een scalair doel staan ze in de stapel van
ch. 7.8 (schrijvers, de invoer of de waarde van het maken, de initialisatie). De specificatie zwijgt over een initialisatie op een
tijdsafhankelijk attribuut; ALEF vult bij een initialisatie per moment waar de tijdlijn leeg is (`MLazyTimedProperty.initResult` met
`orElse`, gelezen, niet gemeten).

## Goal

Meet in `alef-bridge` (alle vier de routes) hoe ALEF een initialisatie, een verdeling en een waarde bij het maken op een tijdsafhankelijk
attribuut leest; volgt uchar ALEF waar de specificatie zwijgt (besluit van de eigenaar), dan leest uchar ze per dag in de stapel van
ch. 7.8: een initialisatie vult de dagen die schrijvers en invoer leeg laten, een verdeling en de waarde van het maken per periode zoals de
meting zegt. Spreekt ALEF zichzelf tegen of de gelande semantiek van uchar, dan is het een normvraag voor de eigenaar, met een aanbeveling.

## Acceptance

- (a) De meting in alef-bridge (een testmodel naar het voorbeeld van `alef-models/WritersInput`), met de uitkomst per route.
- (b) Per vorm: gebouwd per dag in ref, clingo en beide Drools-branches met een golden, of een WF-regel die haar weigert, met reden.
- (c) Rood eerst; `make gates` groen.

May touch: `spec/`, `ref/`, `backends/`, `corpus/`, `scripts/` (alleen lijsten; voorstel), alef-bridge (een meetmodel), kaartverplaatsing.

## Omvang

Middel.

## Result
