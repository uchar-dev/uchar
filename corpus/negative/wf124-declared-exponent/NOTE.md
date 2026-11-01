# wf124-declared-exponent

Violates WF-124 (`spec/IR-SPEC.md` ch. 2.5), arm (e): the declaration of `maand` in the unit system `Termijn` with `exponent` 2.

    WF-124 FAIL /eenheidstelsels/Termijn/units/0/exponent: the unit system 'Termijn' declares the unit 'maand' with an exponent: a declaration is one unit, and an exponent belongs to a factor where a unit occurs

The document is `corpus/golden/jutterswet-vergunningsduur` with this one change.
The schema refuses this shape too, so the document holds in this corpus only, where a witness needs to load and not to validate. `expect.txt` pins the arm.
