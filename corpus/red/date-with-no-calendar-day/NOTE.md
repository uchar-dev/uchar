# date-with-no-calendar-day

Parts of a date construction that name no calendar day or no clock time are
a fault, EF-032, never a roll-over into the next month, never a clamp to
the month's end and never leeg (`RUNTIME-SEMANTICS.md` ch. 3.8, "Date
construction"; ch. 5.7b).

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
    de ligdatum Datum in dagen;
    het ligtijdstip Datum en tijd in millisecondes;

Regel ligdatum
    geldig altijd
        De ligdatum van een Schuit moet gesteld worden op de datum met jaar: zijn ligjaar, maand: zijn ligmaand en dag: zijn ligdag.

Regel ligtijdstip
    geldig altijd
        Het ligtijdstip van een Schuit moet gesteld worden op de datumtijd met jaar: zijn ligjaar, maand: zijn ligmaand, dag: zijn ligdag, uur: zijn liguur, minuut: zijn ligminuut, seconde: zijn ligseconde en milliseconde: zijn ligmilliseconde.
```

Each case carries one Schuit, S1, with all seven parts. Where the day does
not exist both rules fault on it; where only the clock time does not, only
`ligtijdstip`.

| case | parts | the fault | pinned params |
|---|---|---|---|
| `case1-31-april` | 2026, 4, 31, 0:00 | April has 30 days | `year` 2026, `month` 4, `day` 31 |
| `case2-29-februari-2026` | 2026, 2, 29, 0:00 | 2026 is no leap year | `year` 2026, `month` 2, `day` 29 |
| `case3-maand-13` | 2026, 13, 1, 0:00 | no month 13 | `year` 2026, `month` 13, `day` 1 |
| `case4-dag-0` | 2026, 3, 0, 0:00 | no day 0 | `year` 2026, `month` 3, `day` 0 |
| `case5-uur-24` | 2026, 3, 1, 24:00 | no hour 24 | `hour` 24, `minute` 0, `second` 0, `millisecond` 0 |

## What each executor says (measured 2026-10-22)

- `ref`, at evaluation: `EVALUATION FAULT: EF-032: dateWith: year 2026,
  month 4 and day 31 name no calendar day`, and for case 5 `dateTimeWith:
  hour 24, minute 0, second 0 and millisecond 0 name no clock time`.
- The ASP backend: `EF-032: clingo: UNSATISFIABLE — a fail-fast guard
  fired: viol(("dateWith: the parts name no calendar day",2026,4,31),"S1")`;
  the witness carries the parts, so it names the same parameters.
- The Drools backend, on both branches, at fire: `FAULT EF-032 {"year":
  "2026", "month": "4", "day": "31"}: … dateWith: year 2026, month 4 and day
  31 name no calendar day`, the reference interpreter's sentence.

## Red first

Measured 2026-10-22 on the card branch before the code: `ref` and the ASP
backend faulted without a code (`EXPECTED FAULT MISMATCH: wanted EF-032`),
and the Drools backend faulted in java.time's words without one, `FAULT
EvaluationFaults: … dateWith: Invalid date 'APRIL 31'`; its runtime took
the parts with `intValue()`.
