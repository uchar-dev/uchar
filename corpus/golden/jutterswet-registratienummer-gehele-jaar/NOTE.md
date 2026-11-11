# jutterswet-registratienummer-gehele-jaar — the exact-digit test per day

A Jutter carries a `registratienummer`, a text on a month timeline. Four rules test it with "numeriek met exact 9
cijfers" under the whole-period qualifier, each setting a characteristic:

| rule (characteristic) | condition |
|---|---|
| `doorlopend geregistreerd` | zijn registratienummer gedurende het gehele jaar numeriek is met exact 9 cijfers |
| `doorlopend afwijkend` | … gedurende het gehele jaar **niet** numeriek is met exact 9 cijfers (the inner negation, `negated`) |
| `onderbroken geregistreerd` | … **niet** gedurende het gehele jaar numeriek is met exact 9 cijfers (the qualifier's negation) |
| `maandelijks geregistreerd` | … gedurende de gehele **maand** numeriek is met exact 9 cijfers (the month grid) |

RUNTIME-SEMANTICS ch. 3.9 reads every condition of ch. 1.2 per day, on the value of that day: a day satisfies the test
when its value is a text of exactly 9 digits, and a leeg day (a gap) fails it, as a leeg operand does in ch. 1.2. The
inner negation is per day, so a leeg day satisfies the negated test. A bucket is touched where the timeline reads a
value; the qualifier holds when every touched bucket has all its days satisfying; a timeline without segments touches
no bucket and passes; the qualifier's negation complements the folded verdict.

## The hand calculation

2026 has 365 days; [2026-01-01, 2026-07-01) holds 181 of them and the rest of the year 184.

| case | `registratienummer` | geregistreerd | afwijkend | onderbroken | maandelijks |
|---|---|---|---|---|---|
| case1-geen-tijdlijn | absent | **present** (no bucket touched) | **present** (no bucket touched) | absent | **present** |
| case2-heel-jaar-geldig | `123456789` on 2026 | **present** (365 of 365) | absent (0 of 365) | absent | **present** |
| case3-half-jaar-geldig | `123456789` on [2026-01-01, 2026-07-01) | absent (181 of 365) | absent (the 184 gap days of 365) | **present** | **present** (January–June, each in full) |
| case4-geldig-dan-te-kort | as case3, then `12345678` on [2026-07-01, 2027-01-01) | absent (181 of 365) | absent (184 of 365) | **present** | absent (July: 0 of 31) |
| case5-heel-jaar-spatie | `1234 5678` on 2026 | absent (0 of 365) | **present** (365 of 365) | **present** | absent |
| case6-half-jaar-spatie | `1234 5678` on [2026-01-01, 2026-07-01) | absent (0 of 365) | **present** (181 days with the value and 184 gap days) | **present** | absent (January: 0 of 31) |

Every case is consistent. The rows are the worked example `WV-3.9-exact` of RUNTIME-SEMANTICS ch. 3.9, in the order a–f.

## Provenance

Own work, 2026-11-11, written as `source.rgs` and emitted by the RegelSpraak frontend. Measured at admission: verifier
PASS; ref and both Drools branches play all six cases (on the code before, ref faulted with REF-008 and Drools refused
with DROOLS-248 and, at the case load, with DROOLS-038 on a text timeline). The ASP backend refuses the golden
(`CLINGO-046`: the test reads the characters of a text, and the encoding has no string functions), listed in gate 6's
`EXPECTED_CHANNEL`.
