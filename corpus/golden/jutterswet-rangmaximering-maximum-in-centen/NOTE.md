# jutterswet-rangmaximering-maximum-in-centen — a cap in another unit than the pot

The document of `jutterswet-rangmaximering`: a Penningkas distributes its penningbudget of 1800 EUR over five
Wachters in order of wachtjaar, ties split by voorranggewicht, each share capped by the Wachter's penningmaximum and
floored at whole euros; what is left goes to the onuitgekeerd budget. Here the penningmaximum is declared in the
sister unit `ct` (1 ct = 1/100 EUR); pot, share and rest stay in EUR.

The recurrence runs in the unit of the pot (RUNTIME-SEMANTICS ch. 4.2): a cap in another unit is restated into EUR by
its exact factor before the `min` and so before the floor, as a comparison restates (ch. 2.6, rule 5). The first cap
is 50099 ct = 500,99 EUR, which tells that order apart: restated first, min(1800; 500,99) floors to 500.

## The hand calculation

| group (wachtjaar) | pot at entry | Wachter | weight | raw share | cap (ct → EUR) | share |
|---|---|---|---|---|---|---|
| 2010 | 1800 | W1 | 3 of 3 | 1800 | 50099 → 500,99 | floor(500,99) = **500** |
| 2015 | 1300 | W2 | 2 of 3 | 866,67 | 50000 → 500 | **500** |
| 2015 | 1300 | W3 | 1 of 3 | 433,33 | 50000 → 500 | floor(433,33) = **433** |
| 2020 | 367 | W4 | 3 of 4 | 275,25 | 25000 → 250 | **250** |
| 2020 | 367 | W5 | 1 of 4 | 91,75 | 25000 → 250 | floor(91,75) = **91** |

Rest: 1800 − (500 + 500 + 433 + 250 + 91) = **26** EUR. The outcome equals the golden with the caps in EUR, and the
allocation is consistent.

Read without the unit, every cap would be a hundred times too large: W1 gets the whole 1800 and the others 0, rest 0.

## Provenance

Own work, 2026-11-11, the worked example "Units in the recurrence" of ch. 4.6 with the caps in ct. Before the
executors restated units in an allocation, ref and the ASP backend answered 1800/0/0/0/0 and rest 0. Measured at
admission: verifier PASS; ref, the ASP backend and both branches of the Drools backend play the case.
