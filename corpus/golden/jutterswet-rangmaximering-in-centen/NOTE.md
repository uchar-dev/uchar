# jutterswet-rangmaximering-in-centen — cap, share and rest in another unit than the pot

The document of `jutterswet-rangmaximering`: a Penningkas distributes its penningbudget of 1800 EUR over five
Wachters in order of wachtjaar, ties split by voorranggewicht, each share capped by the Wachter's penningmaximum and
floored at whole units of the pot; what is left goes to the onuitgekeerd budget. Here the pot stays in EUR, and the
penningmaximum, the penningtoewijzing (the share) and the onuitgekeerd budget (the rest) are declared in the sister
unit `ct` (1 ct = 1/100 EUR).

The recurrence runs in the unit of the pot (RUNTIME-SEMANTICS ch. 4.2): every cap is restated into EUR before the
`min` (ch. 2.6, rule 5), the floor is at whole EUR, and each share and the rest are written through the assignment
gate (ch. 2.4 point 1), which restates them from EUR into ct (ch. 2.6, rule 6). The golden
`jutterswet-rangmaximering-maximum-in-centen` holds the order of restating and flooring with a cap that is not a
whole number of EUR; this one holds the way back into the target's unit.

## The hand calculation

| group (wachtjaar) | pot at entry (EUR) | Wachter | weight | raw share (EUR) | cap (ct → EUR) | share (EUR) | written (ct) |
|---|---|---|---|---|---|---|---|
| 2010 | 1800 | W1 | 3 of 3 | 1800 | 50000 → 500 | 500 | **50000** |
| 2015 | 1300 | W2 | 2 of 3 | 866,67 | 50000 → 500 | 500 | **50000** |
| 2015 | 1300 | W3 | 1 of 3 | 433,33 | 50000 → 500 | 433 | **43300** |
| 2020 | 367 | W4 | 3 of 4 | 275,25 | 25000 → 250 | 250 | **25000** |
| 2020 | 367 | W5 | 1 of 4 | 91,75 | 25000 → 250 | 91 | **9100** |

Rest: 1800 − 1774 = 26 EUR, written as **2600** ct. The allocation is consistent.

Read without the units, the caps of 50000 and 25000 would not bind and W1 would get the whole pot; written without
the restatement, the shares would stand as 500/500/433/250/91 ct and the rest as 26 ct.

## Provenance

Own work, 2026-11-11, the worked example "Units in the recurrence" of ch. 4.6 with cap, share and rest in ct. Measured
at admission: verifier PASS; ref, the ASP backend and both branches of the Drools backend play the case.
