# dagsoortstap-buiten-bereik

A step of one day of a kind on a datum-tijd that lands past the last day of
the representable millisecond range is a fault (`RUNTIME-SEMANTICS.md`
ch. 3.8). One walk is only a few days long, but its start can already lie at
the edge of the range, computed by another rule. This case composes the two:
the sister case `datumtijdstap-buiten-bereik` covers the calendar and clock
steps, this one the day kind's step.

## The shape

`ir.json` is what the RegelSpraak frontend (`Resolver --ir`) emits for this
source, and it is verifier-clean. The day kind is the springtijdag of golden
`jutterswet-getijklok`, the 1st and the 15th of a month:

```
Dagsoort de springtijdag (mv: springtijdagen);

Objecttype de Getijpost (mv: Getijposten) (bezield)
    het meldtijdstip Datum en tijd in millisecondes;
    de wachtdagen Numeriek (geheel getal) met eenheid dagen;
    de wachttermijn Numeriek (geheel getal) met eenheid springtijdagen;
    het verre tijdstip Datum en tijd in millisecondes;
    het sluittijdstip Datum en tijd in millisecondes;

Regel Dagsoortdefinitie springtijdag
    geldig altijd
        Een dag is een springtijdag
        indien er aan ten minste 1 van de volgende voorwaarden wordt voldaan:
        - de dag uit de dag gelijk is aan 1
        - de dag uit de dag gelijk is aan 15.

Regel verre tijdstip van een getijpost
    geldig altijd
        Het verre tijdstip van een Getijpost moet berekend worden als zijn meldtijdstip plus zijn wachtdagen.

Regel sluittijdstip van een getijpost
    geldig altijd
        Het sluittijdstip van een Getijpost moet berekend worden als zijn verre tijdstip plus zijn wachttermijn.
```

The case carries one Getijpost, G1: meldtijdstip 2024-01-01T00:00:00,
wachtdagen 53 375 975 860, wachttermijn 1. The verre tijdstip is
146140482-04-24T00:00:00, 4 611 686 018 371 200 000 ms: the last day of the
range [-2^62, 2^62 - 1] ms that `ref` holds a datum-tijd in, and a value, not
a fault. One springtijdag further is 146140482-05-01, seven days past that
day, so the sluittijdstip leaves the range.

## What each executor says (measured 2026-10-09)

- `ref`, at evaluation: `EVALUATION FAULT: datum-tijd arithmetic leaves the
  representable millisecond range (v0 raises DateTimeException)`. It tests the
  day its walk lands on, with the time of day riding through, before it
  builds the instant.
- The ASP backend refuses the count of days at translation, before any date
  arithmetic: `TRANSLATION FAULT: value 53375975860 at 0 decimals exceeds
  clingo's integer range (grounding would wrap silently)`. As in the sister
  case, the pin witnesses that the ASP backend does not answer, not where
  the range lies.
- The Drools backend, at fire, in the runtime's own sentence:
  `IllegalStateException: datum-tijd arithmetic leaves the representable
  millisecond range`, from `RegelSpraakRuntime.rsDatumTijdPlusDagsoort`.
  Gate 8's Drools leg replays the exec-model branch; the JUnit witness
  `DatumstapBereikTakkenWitnessTest` replays the DRL-text branch as well,
  and pins the truth-maintenance branch's refusal of day kinds.

## Red first

Measured 2026-10-09 on the card branch at 16905a6d, where the day kind's
step put the time of day back without testing the day: the Drools red run
reported `NOFAULT ran to a model without faulting`, gate 8's Drools leg
failed with `Drools did not FAULT (pinned drools="…"), observed NOFAULT`,
and the green replay of the same input showed sluittijdstip
+146140482-05-01T00:00 in both the exec-model and the DRL-text branch.
