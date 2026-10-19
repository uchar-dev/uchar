# wf116-duplicate-vocabulary

Violates WF-116 (b) (`spec/IR-SPEC.md` ch. 2.12): vocabulary names are
unique, case-insensitively.

    WF-116 FAIL /vocabularies/2/name: vocabularies 'handboek' and 'Handboek'
    collide case-insensitively

The document is `corpus/golden/jutterswet-vocabulaires` with a third
vocabulary `Handboek`, which differs from `handboek` in case alone: a
designation that names `handboek` would name either. The verifier reports it
in one line, the arm `expect.txt` pins.
