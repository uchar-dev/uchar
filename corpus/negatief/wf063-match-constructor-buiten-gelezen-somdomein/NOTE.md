Violates WF-063 (`spec/IR-SPEC.md` ch. 4.7), the sum-type twin of
`wf063-match-arm-buiten-gelezen-domein`: the `Match` reads
`Jutter.beschikking`, declared with the sum domain `beschikking`
(`Toegekend(bedrag)`, `Afgewezen`, `Aangehouden`), and its arms are the
constructors `Goedgekeurd`, `Verworpen` and `Uitgesteld` of the domain
`oordeel`.

    WF-063 FAIL /rules/resolvedRegels/0/versions/0/value: arm 'Goedgekeurd' is
    not a declared constructor of the sum type
    WF-063 FAIL /rules/resolvedRegels/0/versions/0/value: arm 'Verworpen' is not
    a declared constructor of the sum type

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-063, the rule its
translation falls on.** Until L2 this document was `wf089-attribuutref-sum`, a
witness of WF-089 on the reference-type arm: the reference's own serialized
`type` named another domain than the declaration it reads, and the match arms
followed the annotation. L2 took that annotation off the five reference
classes (B13, IR-SPEC ch. 4.1): the scrutinee's type IS its declaration's, so
what remains is a match whose arms do not cover the domain it actually reads.
The migration (`spec/proposals/v1-migratie/m2.py`) removed the annotation and
nothing else; measured on that translation, the verifier refuses it on WF-063,
the rule this directory now names. `expect.txt` pins the arm.
