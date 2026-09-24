# wf046-kwantorvouw-tenhoogste-matrix

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3) through one clause of the quantifier-fold
decision of 2026-09-23: the MATRIX of an `atMost` fold is a negative edge,
comparisons included, because members whose matrix turns true can push the
count over the threshold.

The model is `corpus/golden/jutterswet-opvolgerquorum` with another creation
condition: "werf een opvolger" mints an opvolger while "de rang van de
Wachter is kleiner dan 5 en ten hoogste 1 ploeglid van de Wachter heeft een
merk groter dan 0". The range is the input-only `ploeg` relation; the
matrix reads the merk the rule derives from the creation's own product.

    WF-046 FAIL /rules/objectCreations/0/versions/0: negative dependency edge on
    a cycle: werf een opvolger reads what merk van een wachter produces while
    merk van een wachter is reachable from it (no strata exist)

The case isolates that clause: read the `atMost` matrix positively and
the document is clean (mutation Kl, measured 2026-09-23).

Measured before the quantifier-fold arm (2026-09-23), with W1 (rang 1)
leading A and B (rang 1, no links of their own): `verifier: PASS`. A and B
lead nobody, so each holds its fold (0 ≤ 1) and mints, and each then carries
merk 1. The reference interpreter reached W1 first and let it mint while no
ploeglid had a merk (merk(W1) = 1); the ASP backend has one stable model in
which both ploegleden carry a merk and W1 does not mint (merk(W1) = 0).
