# wf124-factors-unsorted

Violates WF-124 (`spec/IR-SPEC.md` ch. 2.5), arm (d): an attribute `proefduur` whose unit writes `maand` before `jaar`.

    WF-124 FAIL /model/objectTypes/0/attributes/2/datatype/unit: the factors of this unit are not in the canonical order (positive exponents first, then by abbreviation and system): here jaar·maand

The document is `corpus/golden/jutterswet-vergunningsduur` with this one change.
The schema admits this shape: it cannot state this arm, so the verifier does. `expect.txt` pins the arm.
