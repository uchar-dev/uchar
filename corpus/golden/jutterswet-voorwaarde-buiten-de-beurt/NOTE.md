# jutterswet-voorwaarde-buiten-de-beurt — a justification read per period that faults in a reading

A defaults group on the timeline target `strandloon`, whose middle default is
justified by a composite read per period over the time-dependent `kustloon`.
The highest default, "indien hij vrijgesteld is", reads no timeline: on a
vrijgesteld Jutter it decides every day, so the middle default is at its turn
on no day and the session does not evaluate its justification
(`spec/RUNTIME-SEMANTICS.md` ch. 7.11). The reading of ch. 5.12 reads it all
the same, and there the comparison over the asOfDate-bucket scalar faults and
passes the fault on, as a scalar condition does. Own Jutterswet-flavoured
casus, own dates, rights-free.

## Casus (own text)

> **Art. 3n (strandloon per maand).** 1. Het strandloon van een jutter bedraagt
> 5. 2. In afwijking van het eerste lid bedraagt het strandloon van een jutter
> wiens kustloon hoger is dan 30 en lager dan 100, 7. 3. In afwijking van het
> tweede lid bedraagt het strandloon van een vrijgestelde jutter 60.

## Case (hand-computed, absolute)

`case1-buiten-de-beurt`, evaluation date 2025-07-01:

- J1, vrijgesteld, the kustloon given as the asOfDate-bucket scalar 40: the
  strandloon is **60** on all time. The vrijstelling decides every day, so
  the kustloon default is at its turn on no day and its justification is not
  evaluated.
- J2, not vrijgesteld, the kustloon 40 on 2025: **5** on (−∞, 2025-01-01),
  **7** on [2025-01-01, 2026-01-01), **5** on [2026-01-01, +∞).
- No fault, and the channel is consistent.

## The reading (ch. 5.12)

The kustloon default (`/rules/prioritizedDefaults/0/defaults/1`) numbers its
nodes 0 `Compound`, 1 `Comparison` (groter dan 30), 2 `AttributeReference`
(the kustloon), 3 `Literal` (30), 4 `Comparison` (kleiner dan 100), 5
`AttributeReference`, 6 `Literal` (100), and 7 `Literal` (the consequence 7).
On J1 the read of the kustloon faults on the bucket scalar (EF-037), the
first comparison and the composite pass the fault on, and nothing after it
is reached; the default is at its turn on no day, so its status is
`DISPLACED`:

```
/rules/prioritizedDefaults/0/defaults/1 "J1" 0 Compound fault EF-037
/rules/prioritizedDefaults/0/defaults/1 "J1" 1 Comparison fault EF-037
/rules/prioritizedDefaults/0/defaults/1 "J1" 2 AttributeReference fault EF-037
/rules/prioritizedDefaults/0/defaults/1 "J1" 3 Literal unreached
/rules/prioritizedDefaults/0/defaults/1 "J1" 4 Comparison unreached
/rules/prioritizedDefaults/0/defaults/1 "J1" 5 AttributeReference unreached
/rules/prioritizedDefaults/0/defaults/1 "J1" 6 Literal unreached
/rules/prioritizedDefaults/0/defaults/1 "J1" 7 Literal unreached
/rules/prioritizedDefaults/0/defaults/1 "J1" DISPLACED
```

On J2 the same default holds on 2025 and reads its periods:

```
/rules/prioritizedDefaults/0/defaults/1 "J2" 0 Compound [[2025-01-01 2026-01-01 empty]]
/rules/prioritizedDefaults/0/defaults/1 "J2" 1 Comparison [[2025-01-01 2026-01-01 40]]
/rules/prioritizedDefaults/0/defaults/1 "J2" 2 AttributeReference [[2025-01-01 2026-01-01 40]]
/rules/prioritizedDefaults/0/defaults/1 "J2" 3 Literal 30
/rules/prioritizedDefaults/0/defaults/1 "J2" 4 Comparison [[2025-01-01 2026-01-01 40]]
/rules/prioritizedDefaults/0/defaults/1 "J2" 5 AttributeReference [[2025-01-01 2026-01-01 40]]
/rules/prioritizedDefaults/0/defaults/1 "J2" 6 Literal 100
/rules/prioritizedDefaults/0/defaults/1 "J2" 7 Literal [[2025-01-01 2026-01-01 7]]
/rules/prioritizedDefaults/0/defaults/1 "J2" HOLDS
```

Gate 17 holds the trace of `ref/` and the readings of both branches of the
Drools backend to these lines.
