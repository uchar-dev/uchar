# wf124-exponent-zero

Violates WF-124 (`spec/IR-SPEC.md` ch. 2.5), arm (b): an attribute `proefduur` in `maand` with `exponent` 0.

    WF-124 FAIL /model/objectTypes/0/attributes/2/datatype/unit/0/exponent: the factor 'maand' has exponent 0: a factor with exponent 0 is no factor

The document is `corpus/golden/jutterswet-vergunningsduur` with this one change.
The schema refuses this shape too, so the document holds in this corpus only, where a witness needs to load and not to validate. `expect.txt` pins the arm.
