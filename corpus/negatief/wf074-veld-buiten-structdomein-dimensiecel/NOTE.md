Violates WF-074 (`spec/IR-SPEC.md` ch. 4.9): a `FieldAccess` reads the field
`bedrag` of the cell `voorjaar` of the dimensioned struct attribute
`Berging.kern` (dimension `seizoen`, struct domain `dossier`). `bedrag` is a
field of `logboek`; a cell read is typed by the attribute it reads.

    WF-074 FAIL /rules/resolvedRegels/0/versions/0/value: 'bedrag' is not a
    declared field of struct 'dossier'

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-074, the rule its
translation falls on.** Until L2 this document was `wf090-dimensiecel`, a
witness of WF-090 on the reference-type arm: the cell read's own `type` named
`logboek` while the dimensioned attribute it reads is declared `dossier`, and
the field access followed the annotation. L2 took that annotation off the five
reference classes (B13, IR-SPEC ch. 4.1), so the read's type IS its
declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing else;
measured on that translation, the verifier refuses it on WF-074, the rule this
directory now names. `expect.txt` pins the arm.
