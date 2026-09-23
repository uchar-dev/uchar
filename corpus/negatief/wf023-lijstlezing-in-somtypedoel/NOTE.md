Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on the family clause: the rule
writes a read of `Jutter.aanspraken`, declared as a list of the sum type
`oordeel`, into a target declared as the element sum type itself — one
`ListType` layer short. The form is the one
`wf023-schrijf-lijst-geen-domein-waarde` already pins; D17 rewrites this
witness on the rule its translation falls on all the same.

    WF-023 FAIL /rules/resolvedRegels/0: a list value is assigned to a sumtype
    target

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-023 (D17).** Until
L2 this document was `wf091-attribuutref-reststructuur`, a witness of WF-091
on the reference-type arm: the reference's own serialized `type` disagreed
with the declaration it reads. L2 took that annotation off the five
reference classes (B13, IR-SPEC ch. 4.1), so the reference's type IS its
declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing
else; measured on that translation, the verifier refuses it on WF-023, the
rule this directory now names. `expect.txt` pins the arm (U1).
