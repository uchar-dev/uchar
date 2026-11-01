# wf124-duplicate-factor

Violates WF-124 (`spec/IR-SPEC.md` ch. 2.5), arm (c): an attribute `proefduur` whose unit names `maand` twice.

    WF-124 FAIL /model/objectTypes/0/attributes/2/datatype/unit/1: the factor 'maand' stands twice in one unit: an atom is one factor, with its exponents added

The document is `corpus/golden/jutterswet-vergunningsduur` with this one change.
The schema admits this shape: it cannot state this arm, so the verifier does. `expect.txt` pins the arm.
