Violates WF-070 (`spec/IR-SPEC.md` ch. 4.8), the struct side of
`wf070-lambdaparameter-ander-somdomein`: a `MapList` maps over a read of
`Jutter.dossiers`, declared as a list of the struct domain `dossier`, with a
lambda whose parameter `d` is declared `logboek`.

    WF-070 FAIL /rules/resolvedRegels/1/versions/0/value/list/function: lambda
    parameter 'd' is struct 'logboek', expected struct 'dossier'

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-070, the rule its
translation falls on.** Until L2 this document was `wf090-attribuutref-lijst`,
a witness of WF-090 on the reference-type arm: the reference feeding the
`MapList` was typed `list of logboek` while `Jutter.dossiers` is declared
`list of dossier`, and the lambda parameter followed the annotation. L2 took
that annotation off the five reference classes (B13, IR-SPEC ch. 4.1), so the
read's type IS its declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing else;
measured on that translation, the verifier refuses it on WF-070, the rule this
directory now names. `expect.txt` pins the arm.
