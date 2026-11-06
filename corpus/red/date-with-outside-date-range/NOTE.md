# date-with-outside-date-range

A date construction (`dateWith`) and an Easter Sunday whose day lies outside
the representable range of a date fault (EF-007), never wrap and never answer
a date (`RUNTIME-SEMANTICS.md` ch. 3.8): the range ends at 146140482-04-24.

## The shape

`ir.json` is what the RegelSpraak frontend (`Resolver --ir`) emits for this
source, and it is verifier-clean:

```
Objecttype de Getijpost (mv: Getijposten) (bezield)
    het bouwjaar Numeriek (geheel getal);
    de bouwmaand Numeriek (geheel getal);
    de bouwdag Numeriek (geheel getal);
    het paasjaar Numeriek (geheel getal);
    de bouwdatum Datum in dagen;
    de paasdatum Datum in dagen;

Regel bouwdatum van een getijpost
    geldig altijd
        De bouwdatum van een Getijpost moet gesteld worden op de datum met jaar: zijn bouwjaar, maand: zijn bouwmaand en dag: zijn bouwdag.

Regel paasdatum van een getijpost
    geldig altijd
        De paasdatum van een Getijpost moet gesteld worden op de eerste paasdag van zijn paasjaar.
```

| case | input | before card `datumrekening-slaat-stil-om` |
|---|---|---|
| case1-jaar-200-miljoen | jaar 200 000 000, maand 1, dag 1 | ref +200000000-01-01 |
| case2-een-dag-na-het-bereik | jaar 146 140 482, maand 4, dag 25 | ref +146140482-04-25 |
| case3-paasjaar-buiten-het-bereik | paasjaar 4 294 969 320 | ref +4294969320-04-21; the Drools runtime's `intValue()` read the year as 2024 until the backend refused years beyond its dates |

Ref and the Drools backend fault with EF-007; the ASP backend refuses as a
limit of its own (gate 8's divergence list: CLINGO-029, the year fence of the
date construction, and CLINGO-001 for a year beyond its integers).

## Provenance

Own work (card `datumrekening-slaat-stil-om`, 2026-11-06); the column "before
the card" is ref of main `17f956d92`, the Drools sentence the card's measurement of 2026-10-09.
