# jutterswet-voorwaarde-onder-invoer-omgekeerd — a division in the condition of an initialisation the input blocks

Document C of RUNTIME-SEMANTICS ch. 5.7, "Faults and deposits are read at the
fixpoint". "De toeslag van een Vondst moet geïnitialiseerd worden op 5 indien
zijn opbrengst gedeeld door zijn aantal deelnemers groter is dan 10". The rules stand in the reverse order of `jutterswet-voorwaarde-onder-invoer`, with the same expectations.

| case | input | `toeslag` | channel |
|---|---|---|---|
| case1-invoer-blokkeert | V2 gemeld, opbrengst 100, deelnemers 0, `toeslag` 7 | **7** | consistent |
| case2-niet-exact-onder-invoer | V4 gemeld, opbrengst 100, deelnemers 3, `toeslag` 7 | **7** | consistent |
| case3-initialisatie-schrijft | V5, opbrengst 100, deelnemers 5 | **5** | consistent |
| case4-nul-aan-fixpunt | V3 gemeld, opbrengst 100, deelnemers 0 | **leeg** | (standaardtoeslag × V3), EF-005 |

The guard of an initialisation comes before its condition, so a filled input
leaves the division unevaluated in case 1 and 2. In case 4 the condition is
evaluated and false, and its deposit stands.
