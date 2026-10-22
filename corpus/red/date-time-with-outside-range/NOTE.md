# date-time-with-outside-range

A date construction whose instant lies outside the representable
millisecond range [−2⁶², 2⁶² − 1] is a fault, EF-007, as a datum-tijd step
that leaves it is (`RUNTIME-SEMANTICS.md` ch. 3.8, "The representable
millisecond range" and "Date construction").

## The shape

`ir.json` is what the RegelSpraak frontend (`Resolver --ir`) emits for this
source, and it is verifier-clean:

```
Objecttype de Schuit (mv: Schuiten)
    het ligjaar Numeriek (geheel getal);
    de ligmaand Numeriek (geheel getal);
    de ligdag Numeriek (geheel getal);
    het liguur Numeriek (geheel getal);
    de ligminuut Numeriek (geheel getal);
    de ligseconde Numeriek (geheel getal);
    de ligmilliseconde Numeriek (geheel getal);
    het ligtijdstip Datum en tijd in millisecondes;

Regel ligtijdstip
    geldig altijd
        Het ligtijdstip van een Schuit moet gesteld worden op de datumtijd met jaar: zijn ligjaar, maand: zijn ligmaand, dag: zijn ligdag, uur: zijn liguur, minuut: zijn ligminuut, seconde: zijn ligseconde en milliseconde: zijn ligmilliseconde.
```

| case | instant | where it lies |
|---|---|---|
| `case1-jaar-200-miljoen` | 200000000-03-01T00:00:00 | some 54 million years past the top |
| `case2-een-milliseconde-na-het-bereik` | 146140482-04-24T15:36:27.904 | one millisecond past the top, 2⁶² − 1 ms = 146140482-04-24T15:36:27.903 |
| `case3-een-milliseconde-voor-het-bereik` | −146136543-09-08T08:23:32.095 | one millisecond before the bottom, −2⁶² ms = −146136543-09-08T08:23:32.096 |

The two ends themselves are instants; `ref/` pins them as worked examples
(`WV-3.8-datumtijdmet-bovengrens`, `-ondergrens`).

## What each executor says (measured 2026-10-22)

- `ref`, at evaluation: `EVALUATION FAULT: EF-007: dateTimeWith: the
  instant lies outside the representable millisecond range`.
- The Drools backend, on both branches, at fire: `FAULT EF-007 {}: … datum-tijd
  arithmetic leaves the representable millisecond range`.
- The ASP backend refuses each case by its own limit, before the range:
  its day numbers hold years 1 to 5 000 000 (`CLINGO-029: clingo:
  UNSATISFIABLE — … viol("dateTimeWith of a year after 5000000 is not
  translated …","S1")`, and `before 1` for case 3). Gate 8 lists the three
  cases in its divergence list with that code.

## Red first

Measured 2026-10-22 on the card branch before the code: the Drools backend
answered case 1 with +200000000-03-01T00:00 and faulted on no case;
`ref` faulted without a code. The bottom end the spec named until then,
−146136543-09-09T08:23:32.096, was one day off: −2⁶² ms falls on
−146136543-09-08 (java.time and `ref` agree), corrected in ch. 3.8.
