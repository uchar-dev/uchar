# jutterswet-leeg-deeltal-per-periode — an empty dividend does not read its divisor, per period

The rule of `jutterswet-leeg-deeltal` on day timelines: the loon of a Vondst is its bonus divided by (its opbrengst
divided by its aandeel), with bonus, aandeel and loon on a timeline and opbrengst a scalar. A division over timelines
is read per day with the empty rule of ch. 1.2 (RUNTIME-SEMANTICS ch. 3.4): a leeg dividend makes the period 0
before the divisor is read (ch. 1.4 point 4), so a division by zero or by leeg inside the divisor deposits only in
the periods where the dividend is filled. A division by a numeric zero there is leeg with a §9.5 deposit of kind
EF-005, a filled dividend over that leeg divisor leeg with one of kind EF-031, each code once per rule × subject.

Quarters of 2024: Q1 = [2024-01-01, 2024-04-01), Q2 = [2024-04-01, 2024-07-01), Q3 = [2024-07-01, 2024-10-01).
opbrengst is 90 in every case.

## The hand calculation

| case | period | bonus | aandeel | divisor 90 ÷ aandeel | loon |
|---|---|---|---|---|---|
| case1-nuldeler-in-leeg-kwartaal | before Q1 | leeg | leeg | not read | 0 |
| | Q1 | leeg | 0 | not read | 0 |
| | Q2 | 60 | 3 | 30 | 60 ÷ 30 = **2** |
| | Q3 | leeg | 3 | not read | 0 |
| | from Q3 on | leeg | leeg | not read | 0 |
| case2-nuldeler-in-gevuld-kwartaal | before Q1 | leeg | leeg | not read | 0 |
| | Q1 | 60 | 0 | 90 ÷ 0 = leeg, EF-005 | 60 ÷ leeg = **leeg**, EF-031 |
| | Q2 | 60 | 3 | 30 | **2** |
| | from Q3 on | leeg | leeg | not read | 0 |
| case3-leeg-deeltal-altijd | every day | leeg | 0 in Q1, 3 in Q2 | not read | 0 |

In case 1 aandeel is also 3 in Q3; the divisor is not read there either, and the loon is 0. The written timelines,
leeg periods as gaps:

- case 1: 0 on (−∞, 2024-04-01), 2 on Q2, 0 on [2024-07-01, +∞); the §9.5 channel is empty: consistent.
- case 2: 0 on (−∞, 2024-01-01), a gap on Q1, 2 on Q2, 0 on [2024-07-01, +∞); the channel carries one fact on
  "loon van een vondst" × V1 with EF-005 and EF-031.
- case 3: 0 on (−∞, +∞); the channel is empty: consistent. With bonus leeg on every day the divisor is not evaluated
  at all, as under a leeg scalar dividend; its reading (ch. 5.12) is `unreached`.

Case 1 and case 2 differ in the bonus of Q1 alone: the division by zero in the divisor surfaces only where the
dividend is filled. An executor that reads the divisor over all time deposits EF-005 in case 1 and case 3, and EF-031
too (the divisor is leeg before Q1 under a leeg bonus), and fails them on `consistent`.

## Provenance

Own work, 2026-11-12, the golden `jutterswet-leeg-deeltal` on timelines. Before the executors read the divisor per
period only where the dividend is filled, ref, the ASP backend and both branches of the Drools backend reported an
inconsistency in case 1 and case 3. The same document, also under a rounding of the quotient, is a witness in the
suites of ref and the ASP backend (`ref/test/fixtures/empty-dividend-per-period`) and of the Drools backend
(`EmptyDividendPerPeriodWitnessTest`).
