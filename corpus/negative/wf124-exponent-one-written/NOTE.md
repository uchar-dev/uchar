# wf124-exponent-one-written

Violates WF-124 (`spec/IR-SPEC.md` ch. 2.5), arm (b): an attribute `proefduur` in `maand` that writes `exponent` 1.

    WF-124 FAIL /model/objectTypes/0/attributes/2/datatype/unit/0/exponent: the factor 'maand' writes the exponent 1: exponent 1 is written by leaving the field out

The document is `corpus/golden/jutterswet-vergunningsduur` with this one change.
The schema refuses this shape too, so the document holds in this corpus only, where a witness needs to load and not to validate. `expect.txt` pins the arm.
