# jutterswet-keurtermijn-bereikt — the day-kind walk of a quantifier member the fold reaches

The control of the golden `corpus/golden/jutterswet-keurtermijn` (card
`clingo-day-kind-walk-after-fold-stop`, 2026-10-21): the same document, and a
case in which the fold reaches the member whose day-kind walk faults.

```
Een Ploeg is inzetbaar indien alle wachten van de Ploeg aan alle volgende
voorwaarden voldoen:
  - hij is gekeurd;
  - zijn keurdatum plus zijn keurtermijn ligt na de rekendatum.
```

with keurdag every day of 2026 and no other day. P1 has the wachten W1 and
W2, both gekeurd. W1's step, 2026-03-01 plus 100 keurdagen, is 2026-06-09,
after the evaluation date 2026-06-01, so W1 holds and the fold reads W2. W2's
step starts at 2027-03-01 and finds no keurdag within 40 000 calendar days:
the reach fence of ch. 3.8 faults, EF-009.

Without this case, the golden's consistent cases would also be satisfied by an
executor that never faults on the walk.

## What each executor pins

`expectedFault` pins the code and the parameter `reach` (40000), which all
three executors emit. ref and the Drools backend also emit `from`
(2027-03-01); the ASP backend does not (RUNTIME-SEMANTICS ch. 5.7b), so the
case does not pin it.
