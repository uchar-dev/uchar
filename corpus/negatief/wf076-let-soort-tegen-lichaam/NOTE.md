Violates WF-076 (`spec/IR-SPEC.md` ch. 4.10): a `Let` binds `b` to a read of
`Jutter.basis`, a `Number` attribute, and its body reads `b`, so the body
is a number; the `Let` node is typed `Tekst`.

    WF-076 FAIL /rules/rules/0/versions/0/value/type: the let type
    (text) differs from its body type (number(0 decimals)) — the let type must
    equal the body type

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-076, the rule its
translation falls on.** Until L2 this document was `wf078-binderref-type-lie`,
a witness of WF-078 on the reference-type arm: the binder reference's own
`type` said `Tekst` where its binder reads a `Number` attribute (WF-078, the
soort facet on a binder reference), and the `Let` node type repeated the
annotation. L2 took that annotation off the five reference classes (B13,
IR-SPEC ch. 4.1), so the read's type IS its declaration's and the lie cannot
be written. The migration (`spec/proposals/v1-migratie/m2.py`) removed the
annotation and nothing else; measured on that translation, the verifier
refuses it on WF-076, the rule this directory now names. `expect.txt` pins the
arm.
