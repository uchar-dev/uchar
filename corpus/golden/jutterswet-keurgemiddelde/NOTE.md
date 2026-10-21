# jutterswet-keurgemiddelde — a quantifier fold that stops before a member that would deposit

The witness of the card `fold-contributor-verdicts` (2026-10-21): the stop of a
quantifier fold over its contributors (`RUNTIME-SEMANTICS.md` ch. 3.9a, the
trigger table of ch. 5.10 over the contributors), observed on the §9.5
channel, and the table of the matrix that the reading of the fold records
(ch. 5.12).

```
Een Ploeg is inzetbaar indien alle wachten van de Ploeg aan alle volgende
voorwaarden voldoen:
  - hij is gekeurd;
  - zijn keurpunten gedeeld door zijn keuringen naar beneden afgerond op
    0 decimalen is groter dan 6.
Het wachttal van een Ploeg moet berekend worden als het aantal wachten van
de Ploeg.
```

The matrix is a composite, so on a member that is not gekeurd it stops
before the division (ch. 5.10). A member with 0 keuringen divides a filled
dividend by a numeric zero, which deposits a §9.5 fact of kind EF-005
(ch. 1.4 point 4) wherever the evaluation reaches it. `wachttal` states the
range per case and is what makes every case refute a do-nothing executor
(gate 7): a case whose only verdict is an absent kenmerk is satisfied by the
raw input.

## The cells, computed by hand before any executor ran

The members are read in the order of the case's role list (ch. 5.11).

| case | the wachten (gekeurd, keurpunten / keuringen) | per member | fold | channel |
|---|---|---|---|---|
| 1 | W1 (no, 70 / 10); W2 (yes, 30 / 0) | W1 false — stop | **false** | consistent: W2 is never read |
| 2 | W1 (yes, 70 / 10); W2 (yes, 80 / 10) | 7 > 6, 8 > 6 | **true** | consistent |
| 3 | W1 (yes, 70 / 10); W2 (yes, 50 / 10); W3 (yes, 30 / 0) | true; 5 > 6 false — stop | **false** | consistent: W3 is never read |
| 4 | W1 (yes, 70 / 10); W2 (yes, 30 / 0) | true; leeg > 6 false | **false** | inconsistent: EF-005 on `inzetbaar bij keurgemiddelde` × P1 |

Case 4 is the control: without it, a consistent channel in cases 1 and 3
would also be satisfied by a division that never deposits.

## The reading (ch. 5.12, the ref test `WV-5.12-keurgemiddelde`)

The node of the fold carries per contributor the verdict of the matrix on
it, on the fixpoint world, with every deposit dropped, and whether the
evaluation reached it:

| case | contributors |
|---|---|
| 1 | `"W1" false reached; "W2" false unreached` |
| 2 | `"W1" true reached; "W2" true reached` |
| 3 | `"W1" true reached; "W2" false reached; "W3" false unreached` |
| 4 | `"W1" true reached; "W2" false reached` |

The verdict `false` of W2 in case 1 and of W3 in case 3 rests on a deposit
the reading dropped and the session never made (ch. 5.12, "The artifacts").

## Why a deposit and not a fault

The card asked for a member after the stop whose matrix faults (the
evaluation year read in a session without an evaluation date, EF-011), so
that the reading carries `fault EF-011 unreached`. No such golden passes the
square today, measured 2026-10-21: the ASP backend raises EF-011 and EF-019
(`Unresolved`) while it translates the document and EF-009 (the day-kind
walk) for every member of the range, read or not, so the case fails gate 6;
and an unbound parameter (EF-014), which the ASP backend guards lazily, the
Drools backend reads as leeg (gate 8, `aanvoergeld-parameter-unbound`
`NOFAULT`). The fault arms of the table are pinned by the executors' own
tests: `ref/` `WV-5.12-fout-in-een-kwantorvouw` and the Drools backend's
`ReadingsWitnessTest`.
