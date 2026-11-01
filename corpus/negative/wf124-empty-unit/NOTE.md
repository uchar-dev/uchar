# wf124-empty-unit

Violates WF-124 (`spec/IR-SPEC.md` ch. 2.5), arm (a): an attribute `proefduur` whose `unit` is the empty list.

    WF-124 FAIL /model/objectTypes/0/attributes/2/datatype/unit: a unit without a factor: a number without a unit carries no unit field

The document is `corpus/golden/jutterswet-vergunningsduur` with this one change.
The schema refuses this shape too, so the document holds in this corpus only, where a witness needs to load and not to validate. `expect.txt` pins the arm.
