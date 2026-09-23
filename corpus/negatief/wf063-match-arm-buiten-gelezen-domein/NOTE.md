Violates WF-063 (`spec/IR-SPEC.md` ch. 4.7): a `Match` over a read of
`Berging.vaarwater`, declared with the enumeration domain `Vaarwater`,
carries the arms `Noord`, `Zuid` and `West` of the domain `Kust`. None of
them is a declared value of the domain the scrutinee reads, and the declared
values go uncovered (the arms are checked against the declaration, never
against an inline copy).

    WF-063 FAIL /rules/resolvedRegels/0/waarde: arm 'Noord' is not a declared
    enumeration value of the enumeration domain
    WF-063 FAIL /rules/resolvedRegels/0/waarde: arm 'Zuid' is not a declared
    enumeration value of the enumeration domain

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-063 (D17).** Until
L2 this document was `wf089-attribuutref-enum`, a witness of WF-089 on the
reference-type arm: the reference's own serialized `type` named another
domain than the declaration it reads, and the match arms followed the
annotation. L2 took that annotation off the five reference classes (B13,
IR-SPEC ch. 4.1): the scrutinee's type IS its declaration's, so what remains
is a match whose arms do not cover the domain it actually reads. The
migration (`spec/proposals/v1-migratie/m2.py`) removed the annotation and
nothing else; measured on that translation, the verifier refuses it on
WF-063, the rule this directory now names. `expect.txt` pins the arm (U1).
