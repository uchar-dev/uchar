Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on the unit clause, the other
one-sided direction: the rule `kopie := gewicht` writes `Jutter.gewicht`,
declared in `kg`, into `Jutter.kopie`, declared unitless.

    WF-023 FAIL /rules/resolvedRegels/0/versions/0: assignment across
    incompatible units

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-023, the rule its
translation falls on.** Until L2 this document was
`wf091-attribuutref-eenheid-ontbreekt`, a witness of WF-091 on the
reference-type arm: the reference's own serialized `type` disagreed with the
declaration it reads. L2 took that annotation off the five reference classes
(B13, IR-SPEC ch. 4.1), so the reference's type IS its declaration's and the
lie cannot be written. The migration (`spec/proposals/v1-migratie/m2.py`)
removed the annotation and nothing else; measured on that translation, the
verifier refuses it on WF-023, the rule this directory now names. `expect.txt`
pins the arm.
