Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), the **canonical-without-factor**
half, at a **function definition's parameter datatype**: the parameter
`gewicht` of the function `Weegbedrag` is a number in the unit `kg` that names
`canonical: "g"` and carries no `factorToCanonical`. Broken from
`corpus/golden/jutterswet-kamerheffing-catala` by one move: the function
definition `Weegbedrag(gewicht)` is added, returning the literal `0`; no rule
invokes it, and its body does not read the parameter. On the golden's own
function `Kamerbedrag` the unit cannot stand alone: the rule that invokes it
passes a unitless argument, and WF-068 reports `argument type number(0
decimals) does not match the parameter datatype number(0 decimals, kg)`
(measured).

    WF-092 FAIL /model/functionDefs/1/parameters/0/datatype/unit: unit 'kg'
    names canonical unit 'g' but carries no conversion factor into it: a
    half-filled conversion pair is not a unit (both fields, or neither)

The position is the function-definition half of `wf092` in
`spec/verifier/lib/checks.ml`, which walks each parameter's datatype and the
result. Falsifiable: with the parameter walk returning no violation this case
turns clean, and it is the only one of the 438 negative cases that does
(measured 2026-09-25): no other witness has a defective unit on a function
parameter.

The pointer is recorded as measured, because gate 3 compares the rule and the
arm, not the pointer. Until 2026-09-25 the verifier cited this position as
`/model/functionDefs/1/parameters/gewicht/unit` — the parameter by its name,
where the wire has a list; the cited path now resolves in the document.
`expect.txt` pins the arm.
