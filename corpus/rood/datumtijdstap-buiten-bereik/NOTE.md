# datumtijdstap-buiten-bereik

A datum-tijd step whose result leaves the representable millisecond range is
a fault, never a silent wrap-around (`RUNTIME-SEMANTICS.md` ch. 3.8). The
four cases are the chapter's worked example, 2024-01-01T00:00:00
± 2 000 000 000 000 uur and + 60 000 000 000 dagen, and a count of
2^64 + 1 days.

## The shape

`ir.json` is what the RegelSpraak frontend (`Resolver --ir`) emits for this
source, and it is verifier-clean:

```
Objecttype de Getijpost (mv: Getijposten) (bezield)
    het meldtijdstip Datum en tijd in millisecondes;
    de wachttijd Numeriek (geheel getal) met eenheid uur;
    de voorlooptijd Numeriek (geheel getal) met eenheid uur;
    de wachtdagen Numeriek (geheel getal) met eenheid dagen;
    het sluittijdstip Datum en tijd in millisecondes;
    het aanlooptijdstip Datum en tijd in millisecondes;
    het verre tijdstip Datum en tijd in millisecondes;

Regel sluittijdstip van een getijpost
    geldig altijd
        Het sluittijdstip van een Getijpost moet berekend worden als zijn meldtijdstip plus zijn wachttijd.

Regel aanlooptijdstip van een getijpost
    geldig altijd
        Het aanlooptijdstip van een Getijpost moet berekend worden als zijn meldtijdstip min zijn voorlooptijd.

Regel verre tijdstip van een getijpost
    geldig altijd
        Het verre tijdstip van een Getijpost moet berekend worden als zijn meldtijdstip plus zijn wachtdagen.
```

Each case carries one Getijpost, G1, with meldtijdstip 2024-01-01T00:00:00
and one of the three durations. The other two are absent, so their rules
write the meldtijdstip unchanged (a leeg duration counts as 0, ch. 1.2) and
exactly one rule can fault.

| case | input | the step |
|---|---|---|
| `case1-plus-uren` | wachttijd 2 000 000 000 000 | plus 7.2 × 10^18 ms, about 228 million years |
| `case2-min-uren` | voorlooptijd 2 000 000 000 000 | the same step back, through `min` |
| `case3-plus-dagen` | wachtdagen 60 000 000 000 | plus 5.184 × 10^18 ms, about 164 million years |
| `case4-telling-buiten-bereik` | wachtdagen 18 446 744 073 709 551 617 | a count of 2^64 + 1 days |

## The range

ch. 3.8 names no number for "the representable millisecond range". `ref`
holds a datum-tijd as an OCaml `int`, so the range is [-2^62, 2^62 - 1] ms
around 1970-01-01T00:00:00, about 146 million years either way, and it
checks a duration's whole count against the same interval before it steps.
The Drools runtime takes that bound over (`RegelSpraakRuntime.MS_MIN` and
`MS_MAX`). java.time reaches about 999 999 999 years, which is why the
backend answered dates for cases 1 to 3 before.

## What each executor says (measured 2026-10-09)

- `ref`, at evaluation: cases 1 to 3 `EVALUATION FAULT: datum-tijd
  arithmetic leaves the representable millisecond range (v0 raises
  DateTimeException)`, case 4 `EVALUATION FAULT: duration count
  18446744073709551617 is out of range`.
- The ASP backend refuses each count at translation, before any date
  arithmetic: `TRANSLATION FAULT: value 2000000000000 at 0 decimals exceeds
  clingo's integer range (grounding would wrap silently)`, and so for the
  other counts. Its integers are 32 bits wide. The pin is that refusal: it
  witnesses that the ASP backend does not answer these cases, not where the
  millisecond range lies.
- The Drools backend, at fire, in the runtime's own sentence:
  `IllegalStateException: datum-tijd arithmetic leaves the representable
  millisecond range` for cases 1 to 3, `IllegalStateException: duration
  count 18446744073709551617 is out of range` for case 4. The two sentences
  are the reference interpreter's, so `ref` and `drools` pin the same
  fragment. Gate 8's Drools leg replays a red case on the exec-model branch
  only; the JUnit witness `DatumstapBereikTakkenWitnessTest` replays these
  four cases through all three branches, which share the step
  (`RegelSpraakRuntime.rsDatumReken`).

## Red first

Measured 2026-10-09 on main 9b9a42ad, before the runtime change: the Drools
red run reported `NOFAULT ran to a model without faulting` on all four cases,
and gate 8's Drools leg failed four times, `Drools did not FAULT (pinned
drools="…"), observed NOFAULT`. The green replay of the same inputs, with an
expectation in place of the fault, showed what the backend computed:
sluittijdstip +228160941-04-01T08:00, aanlooptijdstip
-228156894-10-02T16:00, verre tijdstip +164276444-06-02T00:00, and for
2^64 + 1 days verre tijdstip 2024-01-02T00:00, because
`BigDecimal.longValue` keeps the low 64 bits of the count, which are 1.
