# date-time-step-wrap

Three datum-tijd steps whose arithmetic wrapped in a 63-bit integer before
card `datumrekening-slaat-stil-om` (2026-11-06), where the step leaves the
representable millisecond range of `RUNTIME-SEMANTICS.md` ch. 3.8: each is a
fault (EF-007), never a silent wrap-around.

## The shape

`ir.json` is what the RegelSpraak frontend (`Resolver --ir`) emits for this
source, and it is verifier-clean:

```
Objecttype de Getijpost (mv: Getijposten) (bezield)
    het meldtijdstip Datum en tijd in millisecondes;
    de wachtkwartalen Numeriek (geheel getal) met eenheid kwartalen;
    de voorloopmillis Numeriek (geheel getal) met eenheid millisecondes;
    de voorlooptijd Numeriek (geheel getal) met eenheid uur;
    de springdagen Numeriek (geheel getal) met eenheid dagen;
    het kwartaaltijdstip Datum en tijd in millisecondes;
    het millitijdstip Datum en tijd in millisecondes;
    het aanlooptijdstip Datum en tijd in millisecondes;
    het springtijdstip Datum en tijd in millisecondes;

Regel kwartaaltijdstip van een getijpost
    geldig altijd
        Het kwartaaltijdstip van een Getijpost moet berekend worden als zijn meldtijdstip plus zijn wachtkwartalen.

Regel millitijdstip van een getijpost
    geldig altijd
        Het millitijdstip van een Getijpost moet berekend worden als zijn meldtijdstip min zijn voorloopmillis.

Regel aanlooptijdstip van een getijpost
    geldig altijd
        Het aanlooptijdstip van een Getijpost moet berekend worden als zijn meldtijdstip min zijn voorlooptijd.

Regel springtijdstip van een getijpost
    geldig altijd
        Het springtijdstip van een Getijpost moet berekend worden als zijn aanlooptijdstip min zijn springdagen.
```

| case | input | step | before the card (ref) |
|---|---|---|---|
| case1-plus-kwartalen | meldtijdstip 2024-01-01T00:00:00, 3 074 457 345 618 258 603 kwartalen | three times the count is 2^63 + 1 months | 2024-02-01T00:00:00 (@1706745600000ms) |
| case2-min-kleinste-telling | meldtijdstip 2024-01-01T00:00:00, voorloopmillis −4 611 686 018 427 387 904 | min the count −2^62 is a step of 2^62 ms forward | @−4611684314360187904ms, 2^62 ms before the start |
| case3-bodem-min-een-dag | meldtijdstip 1970-01-01T00:00:00, voorlooptijd 1 281 023 894 007 uur, springdagen 1 | the aanlooptijdstip lies 36 minutes above −2^62 (−146136543-09-08T09:00), one day back leaves the range | @4611686018343175808ms, near the top: the floor division of the civil day overflowed |

Ref and the Drools backend fault with EF-007; the ASP backend refuses each
count at translation (CLINGO-001, gate 8's divergence list).

## Provenance

Own work (card `datumrekening-slaat-stil-om`, 2026-11-06), the corners the
card measured; the column "before the card" is ref of main `17f956d92`.
