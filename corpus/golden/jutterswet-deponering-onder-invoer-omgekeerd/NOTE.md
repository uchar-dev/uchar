# jutterswet-deponering-onder-invoer-omgekeerd — a deposit in the condition of an initialisation the input blocks

Document D of RUNTIME-SEMANTICS ch. 5.7, "Faults and deposits are read at the
fixpoint". "De heffing van een Jutter moet geïnitialiseerd worden op 7 indien
5 gedeeld door zijn b leeg is", beside a rule of its own on the grondslag, so
that the document has an order. The rules stand in the reverse order of `jutterswet-deponering-onder-invoer`, with the same expectations.

| case | input | `heffing` | channel |
|---|---|---|---|
| case1-invoer-blokkeert | J1, `heffing` 9,00, b leeg | **9,00** | consistent |
| case2-leeg-aan-fixpunt | J2, b leeg | **7,00** | (heffing van een jutter × J2), EF-031 |
| case3-nul-aan-fixpunt | J3, b 0,00 | **7,00** | (heffing van een jutter × J3), EF-005 |
| case4-voorwaarde-onwaar | J4, b 2,00 | **leeg** | consistent |

`grondslag` is **3,00** in every case.
