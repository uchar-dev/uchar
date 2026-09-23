Violates WF-074 (`spec/IR-SPEC.md` ch. 4.9): a `FieldAccess` reads the field
`bedrag` of the parameter `grondslag`, declared with the struct domain
`dossier`. `bedrag` is a field of `logboek`.

    WF-074 FAIL /rules/resolvedRegels/5/waarde: 'bedrag' is not a declared
    field of struct 'dossier'

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-074 (D17).** Until
L2 this document was `wf090-parameterref`, a witness of WF-090 on the
reference-type arm: the parameter reference's own `type` named `logboek`
while the parameter is declared `dossier`, and the field access followed the
annotation. L2 took that annotation off the five reference classes (B13,
IR-SPEC ch. 4.1), so the read's type IS its declaration's and the lie cannot
be written. The migration (`spec/proposals/v1-migratie/m2.py`) removed the
annotation and nothing else; measured on that translation, the verifier
refuses it on WF-074, the rule this directory now names. `expect.txt` pins
the arm (U1).
