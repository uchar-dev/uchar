# wf046-leegtest-recursieve-creatie

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3). A leeg/gevuld test is a negative edge in
both polarities (maintainer decision 2026-09-23), and here two of them sit on
a cycle that WF-045 admits.

The recursive creation "werf de opvolger" (member of the recursive group
"estafette") mints a successor Wachter while "het merk van de Wachter is
leeg". The rule "merk van een wachter" writes merk = 1 when "de rang van de
opvolger van de Wachter is gevuld". The creation writes the successor's rang
and the `opvolging` link; the rule reads both under `filled`; the merk the rule
writes is what the creation tests under `empty`. Both tests carry
`negated: false`, so this is the witness for that polarity.

    WF-046 FAIL /rules/objectCreations/0/versions/0: negative dependency edge on
    a cycle: werf de opvolger reads what merk van een wachter produces while
    merk van een wachter is reachable from it (no strata exist)
    WF-046 FAIL /rules/rules/0/versions/0: negative dependency edge on
    a cycle: merk van een wachter reads what werf de opvolger produces while
    werf de opvolger is reachable from it (no strata exist)

WF-045 holds, because the cycle passes through the recursive creation, and the
creation is conditional (WF-033), so the case isolates WF-046.

Measured before the arm (2026-09-23), with one input Wachter W1 of rang 1.
The verifier said `verifier: PASS`. The reference interpreter minted W1 to W5
(rang 1 to 5) and wrote merk 1 on the first four, so each of those four
creations was justified by a leeg merk that its own final world fills. The ASP
backend found no stable model (`clingo: UNSATISFIABLE — a fail-fast guard
fired: no viol/2 witness (the input is not stratified?)`). This is the R1
probe of 2026-09-23 as a corpus document.

Falsifiable in one edit: drop the merk test from the creation's condition
and the document is clean (measured: `verifier: PASS`). The rule still reads
the successor under `filled`, but nothing on the creation's side reads the
rule's merk any more, so no negative edge lies on a cycle.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`.
