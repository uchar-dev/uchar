Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), the **factor-without-canonical**
half, at a **sum-type constructor's payload**: the payload of `Gemeten` is a
number in the unit `m` that carries `factorToCanonical: {"num": "1", "den":
"1"}` and no `canonical`. Broken from `corpus/golden/synth3-letbeslag` by one
move: a sum-type domain `meetkeuze` is added, with the constructors `Gemeten`
(that payload) and `Geschat` (none). No rule reads the domain, so the
half-filled pair is the document's only defect. Put on the golden's own
`regime` payload instead (the domain and the attribute's copy), the unit
reaches the match arm that adds the payload to a unitless amount, and WF-035
reports `additive operands have incompatible units` beside the two WF-092
lines (measured).

    WF-092 FAIL /model/domains/1/datatype/constructors/0/payload/unit: unit 'm'
    carries conversion factor 1 but names no canonical unit to convert into: a
    half-filled conversion pair is not a unit (both fields, or neither)

The position is the sum-type arm of the datatype walk (`check_datatype_units`
in `spec/verifier/lib/checks.ml`). Falsifiable: with that arm returning no
violation this case turns clean, and it is the only one of the 438 negative
cases that does (measured 2026-09-25): no other witness has a unit inside a
constructor payload.

The pointer is recorded as measured, because gate 3 compares the rule and the
arm, not the pointer. Until 2026-09-25 the verifier cited this position as
`/model/domains/1/datatype/constructoren/Gemeten/unit` — the constructor by
its name under a segment the document does not have; the cited path now
resolves in the document. `expect.txt` pins the arm.
