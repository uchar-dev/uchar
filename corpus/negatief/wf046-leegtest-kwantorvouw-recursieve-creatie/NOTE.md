# wf046-leegtest-kwantorvouw-recursieve-creatie

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3). A leeg/gevuld test is a negative edge in
both polarities (maintainer decision 2026-09-23). Here the test sits in the
matrix of a quantifier fold, and this case pins that the rule reaches it
there.

The recursive creation "werf de opvolger" (group "estafette") mints a
successor Wachter while "de rang van alle opvolgers van de Wachter is leeg":
a `Kwantorvouw` under ALLE over the `opvolging` successors whose matrix is a
LEEG test on the member's rang. The creation writes the successor's rang, and
its own condition reads that rang under the test, so the creation depends
negatively on itself.

    WF-046 FAIL /rules/objectCreaties/0: negative dependency edge on a cycle: werf de opvolger reads what werf de opvolger produces while werf de opvolger is reachable from it (no strata exist)

Measured before the arm (2026-09-23), with one input Wachter W1 of rang 1.
The verifier said `verifier: PASS`. The reference interpreter minted W1 to
W5: every creation fires while its subject has no successor yet (ALLE over an
empty range holds), and the successor it mints carries a rang that falsifies
the condition afterwards. The ASP backend without its conservative refusal
found no stable model.

What this case does NOT cover: the same fold over a COMPARISON ("de rang van
alle opvolgers is groter dan 5") is still verifier-clean and splits the
executors the same way. The fold's range and an attribute compared in its
matrix are not negative edges; that is recorded open in
`RUNTIME-SEMANTICS.md` ch. 6.1. The arm catches this document through the
leeg test in the matrix, not through the fold.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`.
