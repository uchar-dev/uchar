# date-with-parts — a date and a datum-tijd built from their parts

The date construction "de datum met jaar: …, maand: … en dag: …" (`dateWith`)
and its datum-tijd sibling "de datumtijd met jaar: …, maand: …, dag: …, uur:
…, minuut: …, seconde: … en milliseconde: …" (`dateTimeWith`), RUNTIME-SEMANTICS
ch. 3.8 ("Date construction") and ch. 5.11 (the parts in wire order). A
Schuit has a gewenste dag; four rules build from parts:

| rule | built value | parts |
|---|---|---|
| gewenste maand | a date | het jaar uit and de maand uit the gewenste dag, dag 1 |
| aanlegtijdstip | a datum-tijd | het jaar, de maand and de dag uit the gewenste dag, uur zijn aanleguur, minuut 30, seconde 0, milliseconde 0 |
| schrikkeldag | a date | jaar zijn schrikkeljaar, maand 2, dag 29 |
| ver vooruit | a date in a condition: the Schuit is ver vooruit when it is later than its gewenste dag | jaar zijn verre jaar, maand uit the gewenste dag, dag 1 |

`gewenste maand` is the witness of the card date-with-in-ref-and-clingo: a
gewenste dag of 15-03-2026 gives a gewenste maand of 01-03-2026. The
schrikkeldag names a day only in a leap year; the cases give it leap years
alone, since 29 February of another year names no calendar day and is a
fault (ch. 3.8), never 1 March and never 28 February. `ver vooruit` puts a
date of year 12026 in a condition, so the reading of that condition (ch.
5.12, gate 17) writes a year the case format cannot write: `+12026-03-01`,
in the reference interpreter's trace and in both branches of the Drools
backend alike.

## Hand computation

`case1-gewenste-maand.json`: S1 with gewenste dag 15-03-2026, aanleguur 6,
schrikkeljaar 2024. Jaar uit 15-03-2026 = 2026, maand uit = 3, dag uit = 15.
Gewenste maand: the date (2026, 3, 1) = **01-03-2026**. Aanlegtijdstip: the
instant (2026, 3, 15, 6, 30, 0, 0) = **2026-03-15T06:30:00**. Schrikkeldag:
2024 is divisible by 4 and not by 100, a leap year, so (2024, 2, 29) =
**29-02-2024**. Verre jaar 12026: the date (12026, 3, 1) is later than
15-03-2026, so S1 **is ver vooruit**.

`case2-jaareinde-en-eeuwjaar.json`: S1 with gewenste dag 31-12-2026,
aanleguur 23, schrikkeljaar 2000. Gewenste maand (2026, 12, 1) =
**01-12-2026**. Aanlegtijdstip (2026, 12, 31, 23, 30, 0, 0) =
**2026-12-31T23:30:00**, the last half hour of the year. Schrikkeldag: 2000
is divisible by 400, a leap year although it is divisible by 100, so
(2000, 2, 29) = **29-02-2000**. Verre jaar 2026: (2026, 12, 1) is not
later than 31-12-2026, so S1 is **not** ver vooruit.

`case3-leeg.json`: a leeg part makes the built value leeg (ch. 1.2). S1 with
gewenste dag 31-01-2024 and no aanleguur and no schrikkeljaar: gewenste
maand (2024, 1, 1) = **01-01-2024**; aanlegtijdstip **leeg** (its uur is
leeg); schrikkeldag **leeg** (its jaar is leeg); no verre jaar, so the date
in the condition is leeg, and an ordering comparison with a leeg operand is
false (ch. 1.2): **not** ver vooruit. S2 with no gewenste dag, aanleguur 6,
schrikkeljaar 2028 and verre jaar 12026: jaar, maand and dag uit a leeg
date are leeg, so gewenste maand **leeg** and aanlegtijdstip **leeg**;
schrikkeldag (2028, 2, 29) = **29-02-2028**; the maand of the date in the
condition is leeg, so **not** ver vooruit.
