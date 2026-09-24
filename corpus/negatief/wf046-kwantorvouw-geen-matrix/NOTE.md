# wf046-kwantorvouw-geen-matrix

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3). Under GEEN the MATRIX of a quantifier fold
is a negative edge, comparisons included (maintainer decision 2026-09-23).
This case isolates that clause: the fold's range is an input relation that
no form writes, so only the matrix can close the cycle.

The recursive creation "werf de opvolger" (group "estafette") mints a
successor while "de rang van de Wachter is kleiner dan 5 en geen van de
ploegleden van de Wachter heeft een merk groter dan 0". The rule "merk van
een wachter" writes merk as the number of a Wachter's successors. The fold
ranges over `ploeg`, which only the case input fills; its matrix reads the
merk that the rule derives from the creation's own product.

    WF-046 FAIL /rules/objectCreaties/0/versions/0: negative dependency edge on
    a cycle: werf de opvolger reads what merk van een wachter produces while
    merk van een wachter is reachable from it (no strata exist)

Measured before the arm (2026-09-23) with W1 (rang 1) leading W2 (rang 1) in
the `ploeg` relation: `verifier: PASS`. The executors split without either
of them failing. The reference interpreter reaches W1 first, sees merk(W2) =
0, and W1 mints a successor (merk(W1) = 1). The ASP backend has one stable
model in which W2's own creation, justified vacuously (W2 has no ploegleden),
gives W2 a successor, so merk(W2) = 1 and W1 does not mint (merk(W1) = 0).
The ASP runner answered `clingo: PASS` on a case pinning merk(W1) = 0, and
the reference interpreter's runner answered `MISMATCH: W1.merk = 1, expected
0` on the same case.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`.
