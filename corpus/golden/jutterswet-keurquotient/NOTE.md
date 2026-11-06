# jutterswet-keurquotient — a bare non-exact division in a condition

```
Objecttype de Jutter (mv: Jutters) (bezield)
    de keurpunten Numeriek (geheel getal);
    de keuringen Numeriek (geheel getal);
    is bekwaam kenmerk;

Regel bekwame jutter
    geldig altijd
        Een Jutter is bekwaam
        indien zijn keurpunten gedeeld door zijn keuringen groter is dan 6.
```

The quotient of two whole numbers is compared and never written, so it is compared exactly (RUNTIME-SEMANTICS ch.
1.2, ch. 2.4: only a write meets the declared decimals of its target). A quotient that the comparison reads at the
scale of its operands would read 13 / 2 as 6 and give the wrong verdict.

## The cells, computed by hand before any executor ran

| case | jutter | keurpunten / keuringen | quotient | groter dan 6 |
|---|---|---|---|---|
| 1 | J1 | 13 / 2 | 6.5 | **bekwaam** |
| 1 | J2 | 59 / 10 | 5.9 | not bekwaam |
| 1 | J3 | 70 / 10 | 7 | **bekwaam** |
| 2 | J1 | 25 / 4 | 6.25 | **bekwaam** |
| 2 | J2 | 30 / 5 | 6 | not bekwaam |

Every case also pins `consistent`. Every case has a quotient that is not whole, so the ASP backend refuses each case
by name with the limit CLINGO-039 (its encoding holds a term as an integer at the scale of its operands, which 6.5
and 6.25 are not); the golden stands in the refusal channel of gate 6 with that code. The reference interpreter and
both branches of the Drools backend play both cases.

## Provenance

Own work (card `clingo-nonexact-division-in-condition`, 2026-11-06). The IR is the RegelSpraak frontend's emission
of the rule above (`--ir`); the cases are written by hand. Measured at admission: verifier PASS; ref plays both
cases, and so do both Drools branches (`PLAYED 1` per case); the ASP backend reported the sentence of EF-006, the
fault of a write, until this card.
