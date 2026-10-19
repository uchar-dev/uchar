Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), the **factor-without-canonical**
half, at a **lambda parameter's datatype**: the element parameter `x` of the
fold in `premie van een jutter` is a number in the unit `st` that carries
`factorToCanonical: {"num": "1", "den": "1"}` and no `canonical`. Broken from
`corpus/golden/jutterswet-kinderprocent` by that one field.

    WF-070 FAIL /rules/rules/0/versions/0/value/function: lambda parameter 'x'
    is number(0 decimals, st), expected number(0 decimals)
    WF-092 FAIL /rules/rules/0/versions/0/value/function/parameters/1/datatype/unit:
    unit 'st' carries conversion factor 1 but names no canonical unit to
    convert into: a half-filled conversion pair is not a unit (both fields, or
    neither)

The WF-070 line comes with the position. A lambda parameter's datatype must
equal the datatype the fold hands it — the list element's for `x`, the fold's
own `type` for the accumulator — and both of those are WF-092 positions
themselves. So a half-filled unit on a lambda parameter comes either with
WF-070 or with a second WF-092 position: carrying the unit on the element of
the parameter `kindertallen` as well gives two WF-092 lines and no WF-070
(measured). This case takes the WF-070, so that the lambda parameter is the
only WF-092 position in the document.

The position is the lambda-parameter walk of `check_fold_fn` in
`spec/verifier/lib/checks.ml` — the expression walk enters the lambda's body,
never its parameter list. Falsifiable: with that walk returning no violation
only the WF-070 line remains and gate 3 reports this case rejected, but not
for WF-092; it is the only one of the 438 negative cases that moves (measured
2026-09-25).

The pointer is recorded as measured, because gate 3 compares the rule and the
arm, not the pointer. Until 2026-09-25 the verifier cited this position as
`/rules/rules/0/versions/0/value/function/parameters/x/unit` — the parameter by
its name, where the wire has a list; the cited path now resolves in the
document. `expect.txt` pins the arm.
