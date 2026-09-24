# wf046-leegtest-negatie-recursieve-creatie

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3). A leeg/gevuld test is a negative edge in
both polarities (maintainer decision 2026-09-23). This is the witness for
`negated: true`: both tests on the cycle carry it.

The recursive creation "werf de opvolger" (group "estafette") mints a
successor Wachter while "het merk van de Wachter is niet gevuld". The rule
"merk van een wachter" writes merk = 1 when "de rang van de opvolger van de
Wachter is niet gevuld". The creation writes the successor's rang and the
`opvolging` link, which the rule reads; the rule writes the merk that the
creation reads.

    WF-046 FAIL /rules/objectCreations/0/versions/0: negative dependency edge on
    a cycle: werf de opvolger reads what merk van een wachter produces while
    merk van een wachter is reachable from it (no strata exist)
    WF-046 FAIL /rules/rules/0/versions/0: negative dependency edge on
    a cycle: merk van een wachter reads what werf de opvolger produces while
    werf de opvolger is reachable from it (no strata exist)

This is the R3 probe of 2026-09-23. The probe wrote the rule's test as
`empty`, which means the same as `filled` negated; it is spelled here with
`negated: true` so that no test with `negated: false` sits on the cycle.

Measured before the arm (2026-09-23), with one input Wachter W1 of rang 1.
The verifier said `verifier: PASS`. The reference interpreter answered
merk(W1) = 1 and minted nothing. That is what its pass order produces: rules
run before creations, so the rule sees no successor and writes merk, and the
creation then sees a filled merk. Run the creation first and it mints W2, the
rule then sees W2's rang and leaves merk(W1) leeg, and W2 gets merk 1 instead.
The ASP backend found five stable models, one per chain length (in the model
with k Wachters only the k-th carries merk 1), and its runner picked one that
leaves W1's merk leeg (`MISMATCH: W1.merk is underived (leeg), expected 1`).
The meaning of the document depended on the evaluation order.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`.
