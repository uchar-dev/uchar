# fold-filter-divides-by-zero

The red twin of `corpus/golden/jutterswet-filterwacht`: the same document,
byte for byte, replayed on `J1` when `J1` IS actief. Both rules then reach
their fold ("het aantal vondsten van de Jutter die aan de volgende
voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan
2"), the filter is evaluated for every member of `J1`'s range, and for `V2`
(gewicht 9, deelnemers 0) it divides by a numeric zero — a fault
(`RUNTIME-SEMANTICS.md` ch. 1.4, point 4), not leeg.

The golden pins that the same division does not happen where the reading
form does not reach the fold (ch. 5.10, ch. 3.4a); this case pins that
where it does, it still faults. A fix that kept the filter from faulting by
reading the division as leeg, or that never evaluated the filter at all,
would pass the golden and fail here.

All three executors fault, each in its own sentence:

| executor | pinned fragment | full report (measured 2026-10-09) |
|----------|-----------------|-----------------------------------|
| ref | `division by numeric zero` | `EVALUATION FAULT: division by numeric zero` |
| clingo | `division by numeric zero` | `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","V2")` |
| Drools (exec-model) | `9 gedeeld door 0 — een numerieke nul als deler is een fout` | `FAULT DelingDoorNul: deling door nul in de regel "drukte": 9 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4) <- DelingDoorNul: Breuk deling door 0` |

Both rules divide 9 by 0, and which of the two the Drools backend names is
the order of its evaluation (before the fix it named "zware vondsten", after
it "drukte"), so the Drools pin is the division and not the rule.
