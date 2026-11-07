# wf009-undeclared-conversion

Violates WF-009 (`spec/IR-SPEC.md` ch. 2.5): a unit where it occurs names a
system that declares its abbreviation, and carries another conversion pair than
that declaration. The `canonical` is compared byte for byte and the factor on
its value, as WF-091 compares them.

    WF-009.undeclared-conversion FAIL /rules/rules/0/versions/0/value/minimum/type/unit/0:
    the unit 't' of the unit system 'Massa' carries factor 100 to 'kg', and its
    declaration factor 1000 to 'kg': a unit where it occurs carries the
    conversion pair of its declaration

(three times: the type of the literal `1 t` in the minimum of `ondergewicht`,
in the sum of `toeslaggewicht` and in the maximum of `lastgewicht`.)

The document is `corpus/golden/jutterswet-stelselweging` with the factor of the
three occurrences of `t` changed from 1000 to 100; the declaration of `t` in
`Massa` keeps `= 1000 kg`. The three occurrences carry the same fields, so the
units still agree where they meet (WF-091), and the verifier reports only this
arm.

**The harm.** Every executor converts a value by the pair on the factor, not by
its declaration. Measured on the golden's case file (2026-11-07), against the
verifier without this arm, which passed the document: `ref` reports 12
mismatches (`V1.ondergewicht = 250, expected 1000`: a `ruwgewicht` of 250 kg
with a minimum of `1 t`, which the golden reads as 1000 kg and the factor 100
as 100 kg, so the minimum no longer applies), clingo the same 12 in its scaled integers (`[25000], expected 100000`),
and both Drools branches `FAIL` on the same attributes (`expected 1000.00,
actual 250.00`). The executors agree with each other; the document states a
conversion in its unit system that no value goes through.

`expect.txt` pins the arm.
