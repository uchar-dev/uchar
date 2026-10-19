Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on the family clause: the rule
writes a read of `Jutter.gewicht`, declared `Number`, into
`Jutter.etiket`, declared `Text` — the mirror of `wf023-text-into-numeric`.

    WF-023 FAIL /rules/rules/0/versions/0: a number value is assigned
    to a text target

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-023, the rule its
translation falls on.** Until L2 this document was `wf091-attribuutref-soort`,
a witness of WF-091 on the reference-type arm: the reference's own serialized
`type` disagreed with the declaration it reads. L2 took that annotation off
the five reference classes (B13, IR-SPEC ch. 4.1), so the reference's type IS
its declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing else;
measured on that translation, the verifier refuses it on WF-023, the rule this
directory now names. `expect.txt` pins the arm.
