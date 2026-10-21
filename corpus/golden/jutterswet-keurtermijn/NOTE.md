# jutterswet-keurtermijn — a quantifier fold that stops before a member whose day-kind walk faults

The witness of the card `clingo-day-kind-walk-after-fold-stop` (2026-10-21): the
stop of a quantifier fold over its contributors (`RUNTIME-SEMANTICS.md`
ch. 3.9a, the trigger table of ch. 5.10 over the contributors) observed as a
fault that does not happen. "A member never reached produces no fault": here
the fault is the reach fence of a day-kind walk (EF-009, ch. 3.8).

```
Een keurdag is een dag waarvoor geldt dat het jaar uit de dag gelijk is aan 2026.
Een Ploeg is inzetbaar indien alle wachten van de Ploeg aan alle volgende
voorwaarden voldoen:
  - hij is gekeurd;
  - zijn keurdatum plus zijn keurtermijn ligt na de rekendatum.
Het wachttal van een Ploeg moet berekend worden als het aantal wachten van
de Ploeg.
```

`keurtermijn` is a number in keurdagen, so `zijn keurdatum plus zijn
keurtermijn` is the §6.11 step: the N-th keurdag after the keurdatum, the
start day not counting. Every day of 2026 is a keurdag and no other day is, so
a step from a date in 2027 walks 40 000 calendar days without finding one and
faults (EF-009). The matrix is a composite, so on a member that is not gekeurd
it stops before the step (ch. 5.10). `wachttal` states the range per case and
is what makes every case refute a do-nothing executor (gate 7).

## The cells, computed by hand before any executor ran

The members are read in the order of the case's role list (ch. 5.11). The
evaluation date is 2026-06-01 in every case.

| case | the wachten (gekeurd, keurdatum + keurtermijn) | per member | fold | channel |
|---|---|---|---|---|
| 1 | W1 (no, 2026-03-01 + 100); W2 (yes, 2027-03-01 + 1) | W1 false — stop | **false** | consistent: W2 is never read |
| 2 | W1 (yes, 2026-03-01 + 100); W2 (yes, 2026-05-01 + 60) | 2026-06-09 and 2026-06-30, both after 2026-06-01 | **true** | consistent |
| 3 | W1 (yes, 2026-03-01 + 100); W2 (yes, 2026-01-10 + 10); W3 (yes, 2027-03-01 + 1) | true; 2026-01-20 not after 2026-06-01, false — stop | **false** | consistent: W3 is never read |
| 4 | W1 (yes, 2026-03-01 + 100); W2 (yes, 2027-03-01 + 1) in no ploeg | true | **true** | consistent: W2 is no contributor |

Cases 1 and 3 are the stop, after the first member and after a member that was
read. Case 4 is the range: W2 is the member of no ploeg, so no fold reaches
it. The control, the same document with W2 reached, faults: it is the red case
`corpus/red/jutterswet-keurtermijn-bereikt`.

## The reading (ch. 5.12)

The node of the fold carries per contributor the verdict of the matrix on
it, or its fault, on the fixpoint world, and whether the evaluation reached
it:

| case | contributors |
|---|---|
| 1 | `"W1" false reached; "W2" fault EF-009 unreached` |
| 2 | `"W1" true reached; "W2" true reached` |
| 3 | `"W1" true reached; "W2" false reached; "W3" fault EF-009 unreached` |
| 4 | `"W1" true reached` |

Cases 1 and 3 are the first goldens whose reading carries a fault of a
contributor after the stop, on both sides of gate 17.

## Why the ASP backend failed it before the card

The ASP backend reads the step's day off the day kind's extension and fires
the reach fence as a `viol/2` witness. Until the card that witness carried
the condition of the member but not the fold's stop, nor its range, so it
fired for every gekeurd Strandwacht: case 1 and 4 at W2 and case 3 at W3,
UNSATISFIABLE where ref and the Drools backend answer. Since the card every
fence of the step carries the reach of the place, here the stop of the fold.
