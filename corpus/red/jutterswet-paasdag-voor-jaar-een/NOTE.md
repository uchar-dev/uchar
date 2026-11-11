# jutterswet-paasdag-voor-jaar-een

Easter Sunday is defined from the year 1 (`RUNTIME-SEMANTICS.md` ch. 3.8).
Before it the formula of ch. 3.8 does not follow the calendar, so a year
below 1 is a fault of the range's kind, EF-007, never a computed day.

## The shape

`ir.json` is what the RegelSpraak frontend (`Resolver --ir`) emits for this
source, and it is verifier-clean:

```
Objecttype de Vondst (mv: Vondsten)
    het vondstjaar Numeriek (geheel getal);
    de paasdatum Datum in dagen;

Regel paasdatum van een vondst
    geldig altijd
        De paasdatum van een Vondst moet gesteld worden op de eerste paasdag van zijn vondstjaar.
```

| case | vondstjaar | what the formula gives |
|---|---|---|
| case1-jaar-nul | 0 | 0000-04-09 |
| case2-jaar-min-een | −1 | −0001-03-20 |
| case3-jaar-min-twee | −2 | the 31st of February, a day the month does not have |

Every case expects EF-007 without parameters. Ref, the ASP backend and the
Drools backend fault with it, each with the sentence "easterSunday is
defined from the year 1; a year before it has no Easter Sunday".

## Provenance

Own work, 2026-11-11.
