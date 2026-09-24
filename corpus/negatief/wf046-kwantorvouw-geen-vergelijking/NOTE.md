# wf046-kwantorvouw-geen-vergelijking

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3). Under GEEN both parts of a quantifier fold
are negative edges (maintainer decision 2026-09-23): the RANGE, because a new
member can falsify the verdict, and the MATRIX, comparisons included,
because GEEN is the negation of the matrix.

The recursive creation "werf de opvolger" (group "estafette") mints a
successor Wachter while "de rang van de Wachter is kleiner dan 5 en geen van
de opvolgers van de Wachter heeft een rang groter dan 0". Without successors
the fold holds; the minted successor has a rang of at least 2, so it
falsifies the fold. The creation reads its own product twice: the
`opvolging` range and the successor's rang in the matrix.

    WF-046 FAIL /rules/objectCreaties/0/versions/0: negative dependency edge on
    a cycle: werf de opvolger reads what werf de opvolger produces while werf de
    opvolger is reachable from it (no strata exist)

Measured before the arm (2026-09-23), with one input Wachter W1 of rang 1:
`verifier: PASS`; the reference interpreter minted W1 to W5; the ASP backend
found no stable model.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`.
