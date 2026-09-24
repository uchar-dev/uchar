Violates WF-036 (`spec/IR-SPEC.md` ch. 4.6), the mirror direction of
`wf036-gelijkheid-somtype-tegen-enumeratie`: the comparison `vaarwater =
Wad` reads `Berging.vaarwater`, declared as an ENUMERATION of the domain
`Vaarwater`, and compares it with a literal typed as a SUM TYPE of a domain
of the same name.

    WF-036 FAIL /rules/rules/0/versions/0/condition: equality across
    families: enumeration vs sumtype

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-036, the rule its
translation falls on.** Until L2 this document was `wf089-som-over-enum`, a
witness of WF-089 on the reference-type arm: the reference's own `type`
spelled the attribute as a sum type of the domain `Vaarwater`, the variant the
value it is compared with also carries. L2 took that annotation off the five
reference classes (B13, IR-SPEC ch. 4.1), so the read's type IS its
declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing else;
measured on that translation, the verifier refuses it on WF-036, the rule this
directory now names. `expect.txt` pins the arm.
