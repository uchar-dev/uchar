# wf046-kwantorvouw-precies-matrix

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3) through one clause of the quantifier-fold
decision of 2026-09-23: the MATRIX of an `exactly` fold is a negative edge,
comparisons included, because a member whose matrix turns true can take the
count past the exact number.

The model is `corpus/golden/jutterswet-opvolgerquorum` with another creation
condition: "werf een opvolger" mints an opvolger while "de rang van de
Wachter is kleiner dan 5 en precies 1 ploeglid van de Wachter heeft een merk
groter dan 0". The range is the `ploeg` relation, which only the case input
fills, so only the matrix can close the cycle: merk is the number of a
Wachter's opvolgers, derived by the rule "merk van een wachter" from what
the creation mints.

    WF-046 FAIL /rules/objectCreations/0/versions/0: negative dependency edge on
    a cycle: werf een opvolger reads what merk van een wachter produces while
    merk van een wachter is reachable from it (no strata exist)

The case isolates that clause: read the `exactly` matrix positively and the
document is clean (mutation Ki, measured 2026-09-23).

Measured before the quantifier-fold arm (2026-09-23), rang 1 everywhere: W1
leads A and B, A leads A1 (A1 has an input opvolger Y), B leads B1, and B1
leads B2 (B2 has an input opvolger X). `verifier: PASS`. The executors split
without either failing. The reference interpreter let W1 mint while exactly
one of its ploegleden had a merk (merk(W1) = 1). The ASP backend has one
stable model in which both A and B end with a merk, so W1 does not mint
(merk(W1) = 0).
