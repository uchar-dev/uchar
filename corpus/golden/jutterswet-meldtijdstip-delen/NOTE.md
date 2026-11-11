# jutterswet-meldtijdstip-delen — year, month and day of a datum-tijd

The part extractors "het jaar uit", "de maand uit" and "de dag uit" on a
datum-tijd (RUNTIME-SEMANTICS ch. 3.8, IR-SPEC WF-038): of a datum-tijd they
read the civil date of its instant, and its clock time changes none of the
three. A Melding has a meldtijdstip (Datum en tijd in millisecondes); three
rules compute its meldjaar, meldmaand and melddag from it. `ir.json` is what
the RegelSpraak frontend emits for `source.rgs`.

## Hand computation

The civil date of an instant is the day of its millisecond count since
1970-01-01T00:00:00 by floor division by 86 400 000 (ch. 3.8), so a clock
time from 00:00:00 through 23:59:59.999 lies on one civil date, also before
1970.

`case1-middag-en-laatste-milliseconde.json`: M1 2026-06-15T14:30:00, M2
2026-06-15T23:59:59.999 (the last millisecond of the day) and M3
2026-06-15T00:00:00 (the first) all lie on 2026-06-15: meldjaar **2026**,
meldmaand **6**, melddag **15** for each. M1 and M2 are the worked examples
of ch. 3.8.

`case2-jaargrens.json`: M1 2025-12-31T23:59:59.999 lies on 2025-12-31:
**2025**, **12**, **31**. M2 2026-01-01T00:00:00, one millisecond later,
lies on 2026-01-01: **2026**, **1**, **1**.

`case3-voor-1970.json`: M1 1969-12-31T23:00:00 is −3 600 000 ms; floor
division gives day −1, 1969-12-31: **1969**, **12**, **31** (a division
toward zero would give day 0, 1970-01-01). M2 1600-02-29T12:00:00 lies on the
leap day of 1600 (divisible by 400): **1600**, **2**, **29**.

`case4-leeg.json`: M1 without a meldtijdstip: the three parts of a leeg date
are leeg (ch. 1.2), meldjaar, meldmaand and melddag **leeg**.

## Executors

Ref, the ASP backend and both branches of the Drools backend play every
case; the ASP backend decomposes the day D of the pair term `dt(D, MS)`.

## Provenance

Own work, 2026-11-11.
