# date-step-outside-range

A step on a date without a time (day precision) whose result leaves the
representable range of a date is a fault, never a silent wrap-around
(`RUNTIME-SEMANTICS.md` ch. 3.8): the range is the civil days whose midnight
lies in the representable millisecond range, epoch days −53 375 995 583
(−146136543-09-09) to 53 375 995 583 (146140482-04-24).

## The shape

`ir.json` is what the RegelSpraak frontend (`Resolver --ir`) emits for this
source, and it is verifier-clean:

```
Objecttype de Getijpost (mv: Getijposten) (bezield)
    de meldtijd Datum in dagen;
    de wachtdagen Numeriek (geheel getal) met eenheid dagen;
    de voorloopdagen Numeriek (geheel getal) met eenheid dagen;
    de wachtweken Numeriek (geheel getal) met eenheid weken;
    de wachtmaanden Numeriek (geheel getal) met eenheid maanden;
    de wachtjaren Numeriek (geheel getal) met eenheid jaren;
    de sluitdag Datum in dagen;
    de aanloopdag Datum in dagen;
    de weekdag Datum in dagen;
    de maanddag Datum in dagen;
    de jaardag Datum in dagen;

Regel sluitdag van een getijpost
    geldig altijd
        De sluitdag van een Getijpost moet berekend worden als zijn meldtijd plus zijn wachtdagen.

Regel aanloopdag van een getijpost
    geldig altijd
        De aanloopdag van een Getijpost moet berekend worden als zijn meldtijd min zijn voorloopdagen.

Regel weekdag van een getijpost
    geldig altijd
        De weekdag van een Getijpost moet berekend worden als zijn meldtijd plus zijn wachtweken.

Regel maanddag van een getijpost
    geldig altijd
        De maanddag van een Getijpost moet berekend worden als zijn meldtijd plus zijn wachtmaanden.

Regel jaardag van een getijpost
    geldig altijd
        De jaardag van een Getijpost moet berekend worden als zijn meldtijd plus zijn wachtjaren.
```

Each case carries one Getijpost, G1, with meldtijd 2024-01-01 and one of the
durations; the others are absent, so their rules write the meldtijd unchanged
(a leeg duration counts as 0, ch. 1.2) and exactly one rule can fault.

| case | step | before card `datumrekening-slaat-stil-om` (ref) |
|---|---|---|
| case1-plus-dagen | + 53 375 975 861 dagen, one day past 146140482-04-24 | +146140482-04-25 |
| case2-min-dagen | − 53 376 015 307 dagen, one day before −146136543-09-09 | −146136543-09-08 |
| case3-plus-weken | + 700 000 000 000 000 000 weken | −11836990593520844-11-04 (a wrap) |
| case4-plus-maanden | + 2 000 000 000 maanden | +166668690-09-01; the ASP backend wrapped to day number 744227579 |
| case5-plus-jaren | + 999 997 975 jaren | +999999999-01-01; the ASP backend named "a year before 0000-03" |

Every case expects EF-007. Ref and the Drools backend fault with it; the ASP
backend holds the day numbers up to 5000000-12-31 and refuses each case as a
limit of its own (gate 8's divergence list: CLINGO-001 at translation for a
count beyond its integers, CLINGO-034 for the count fence of a date step).

## Provenance

Own work (card `datumrekening-slaat-stil-om`, 2026-11-06), the corners the
card measured; the column "before the card" is ref (and where named the ASP
backend) of main `17f956d92`.
