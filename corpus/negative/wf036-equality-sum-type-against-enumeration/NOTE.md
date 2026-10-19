Violates WF-036 (`spec/IR-SPEC.md` ch. 4.6): the rule `uitkering := 5.00
indien beschikking = Afgewezen` compares a read of `Jutter.beschikking`,
declared as a SUM TYPE of the domain `beschikking`, with a literal typed as
an ENUMERATION of a domain of the same name. The two variants of the
keuzetype family are two families: `domains` is one namespace, and one name
declares one variant.

    WF-036 FAIL /rules/rules/0/versions/0/condition: equality across
    families: sumtype vs enumeration

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-036, the rule its
translation falls on.** Until L2 this document was `wf089-enum-over-som`, a
witness of WF-089 on the reference-type arm: the reference's own `type`
spelled the attribute as an enumeration of the domain `beschikking`, the
variant the literal it is compared with also carries. L2 took that annotation
off the five reference classes (B13, IR-SPEC ch. 4.1), so the read's type IS
its declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing else;
measured on that translation, the verifier refuses it on WF-036, the rule this
directory now names. `expect.txt` pins the arm.
