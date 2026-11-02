# jutterswet-initialisatie-naast-regel — a division in an initialisation beside a rule that decides

Document A of RUNTIME-SEMANTICS ch. 5.7, "Faults and deposits are read at the
fixpoint". The rule `bergloon` writes the taxatie where the Vondst is erkend,
the initialisation `standaardbergloon` writes opbrengst gedeeld door aantal
deelnemers, rekenkundig afgerond op 0 decimalen (the precision gate of ch. 2.4
wants a rounding on a quotient into a whole target). The rules stand in the order of the casus; `jutterswet-initialisatie-naast-regel-omgekeerd` has them in the reverse order, with the same expectations.

| case | input | `bergloon` | channel |
|---|---|---|---|
| case1-regel-beslist | V1 gemeld, taxatie 30, opbrengst 100, deelnemers 0 | **30** | consistent |
| case2-niet-exact-onder-regel | V2 gemeld, taxatie 30, opbrengst 100, deelnemers 3 | **30** | consistent |
| case3-invoer-staat | V3 gemeld, no taxatie, `bergloon` 40, opbrengst 100, deelnemers 0 | **40** | consistent |
| case4-initialisatie-rekent | V4 not gemeld, opbrengst 100, deelnemers 4 | **25** | consistent |
| case5-nul-aan-fixpunt | V5 not gemeld, taxatie 30, opbrengst 100, deelnemers 0 | **leeg** | (standaardbergloon × V5), EF-005 |

The rule decides in case 1 and 2, so the initialisation does not, and its
division by zero is no outcome of the session. The input blocks it in case 3.
It decides in case 4 and 5, and the deposit of case 5 stands at the fixpoint;
the rounding of the leeg quotient is leeg.
