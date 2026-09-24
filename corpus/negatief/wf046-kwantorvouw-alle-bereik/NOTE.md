# wf046-kwantorvouw-alle-bereik

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3). The RANGE of a quantifier fold is a
negative edge under ALLE, GEEN, TEN_HOOGSTE and PRECIES (maintainer decision
2026-09-23), because the verdict can be lost when the range grows. Here the
fold is an ALLE fold over the successors that the creation itself mints.

The recursive creation "werf de opvolger" (group "estafette") mints a
successor Wachter while "de rang van de Wachter is kleiner dan 5 en de rang
van alle opvolgers van de Wachter is groter dan 5". A Wachter without
successors satisfies the ALLE fold vacuously, the creation mints one of rang
+ 1, and that successor falsifies the fold. The creation writes the
`opvolging` link the fold ranges over, so it reads its own product through
the range.

    WF-046 FAIL /rules/objectCreaties/0/versions/0: negative dependency edge on
    a cycle: werf de opvolger reads what werf de opvolger produces while werf de
    opvolger is reachable from it (no strata exist)

The case isolates the range. Under ALLE the MATRIX is monotone and reads
positively (the comparison of the successor's rang is stratum-strict and its
strictness is discharged on the admissible cycle), so the refusal comes from
the `opvolging` range read alone. Make the range read positive and the
document is clean (measured, mutation of 2026-09-23).

This is the shape of the R5 probe of 2026-09-23. Measured before the arm,
with one input Wachter W1 of rang 1: `verifier: PASS`; the reference
interpreter minted W1 to W5, each creation justified by a vacuous ALLE that
the next generation falsifies; the ASP backend found no stable model.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`.
