Violates WF-070 (`spec/IR-SPEC.md` ch. 4.8): a `MapList` maps over a read of
`Jutter.aanspraken`, declared as a list of the sum domain `beschikking`,
with a lambda whose parameter `a` is declared `oordeel`. The parameter of a
map's function is the source list's element type.

    WF-070 FAIL /rules/resolvedRegels/0/waarde/list/function: lambda parameter
    'a' is sumtype 'oordeel', expected sumtype 'beschikking'

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-070 (D17).** Until
L2 this document was `wf089-attribuutref-lijst-sum`, a witness of WF-089 on
the reference-type arm: the reference feeding the `MapList` was typed `list
of oordeel` while `Jutter.aanspraken` is declared `list of beschikking`, and
the lambda parameter followed the annotation. L2 took that annotation off
the five reference classes (B13, IR-SPEC ch. 4.1), so the read's type IS its
declaration's and the lie cannot be written. The migration
(`spec/proposals/v1-migratie/m2.py`) removed the annotation and nothing
else; measured on that translation, the verifier refuses it on WF-070, the
rule this directory now names. `expect.txt` pins the arm (U1).
