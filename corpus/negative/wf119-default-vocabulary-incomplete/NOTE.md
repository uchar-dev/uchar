# wf119-default-vocabulary-incomplete

Violates WF-119 (`spec/IR-SPEC.md` ch. 2.12): the default vocabulary names
every designatable declaration.

    WF-119 FAIL /rules/ruleGroups/0: the rule group 'premieberekening'
    carries no designation in the default vocabulary 'cgm'

The document is `corpus/golden/jutterswet-vocabulaires` with the `cgm`
designation of the rule group `premieberekening` removed; `cgm` is the
default vocabulary. The verifier reports it in one line, the arm
`expect.txt` pins.
