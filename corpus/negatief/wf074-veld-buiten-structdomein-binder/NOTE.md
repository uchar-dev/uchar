Violates WF-074 (`spec/IR-SPEC.md` ch. 4.9) inside a lambda: the `MapList`
lambda declares its parameter `d` with the struct domain `dossier`
(faithfully, against the list it maps), and its body reads the field
`bedrag` of `d`. `bedrag` is a field of `logboek`; a binder read is typed by
its binder's declaration.

    WF-074 FAIL /rules/resolvedRegels/1/versions/0/value/list/function/body: 'bedrag'
    is not a declared field of struct 'dossier'

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-074 (D17).** Until
L2 this document was `wf090-binderref`, a witness of WF-090 on the
reference-type arm: the binder reference's own `type` named `logboek` while
its binder, the lambda parameter `d`, is declared `dossier`, and the field
access followed the annotation. L2 took that annotation off the five
reference classes (B13, IR-SPEC ch. 4.1), so the read's type IS its
declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing
else; measured on that translation, the verifier refuses it on WF-074, the
rule this directory now names. `expect.txt` pins the arm (U1).
