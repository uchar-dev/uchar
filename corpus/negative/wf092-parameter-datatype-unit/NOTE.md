Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), arm (c), at a **parameter's
datatype**: the unit `EUR` of the parameter `penningbedrag` carries
`factorToCanonical: {"num": "2", "den": "2"}`, a factor of one that is not in
lowest terms. Broken from `corpus/golden/jutterswet-penningjaar` by that one
field; the declaration in the unit system `Munt` keeps `{"num": "1", "den":
"1"}`. No rule reads the parameter, so no other position compares its unit
with that declaration; the report is this one line (measured).

    WF-092 FAIL /model/parameters/1/datatype/unit/factorToCanonical: unit 'EUR'
    carries conversion factor 2/2, which is not in lowest terms: an exact
    factor has one spelling, here 1

The position is the parameter half of `wf092` in
`spec/verifier/lib/checks.ml`, which walks each declared parameter's
datatype. Falsifiable: with that walk returning no violation this case turns
clean, and it is the only one of the 438 negative cases that does (measured
2026-09-25): no other witness has a defective unit on a parameter.

The pointer is recorded as measured, because gate 3 compares the rule and the
arm, not the pointer. Until 2026-09-25 the verifier cited this position as
`/model/parameters/1/unit/factorToCanonical` — the parameter itself, which has
no `unit`; the unit sits under its `datatype`, and the cited path now resolves
in the document. `expect.txt` pins the arm.
