# wf117-designation-with-bracket

Violates WF-117 (d) (`spec/IR-SPEC.md` ch. 2.12): a designation carries no
parenthesis.

    WF-117 FAIL
    /model/objectTypes/1/attributes/2/designations/0/designation:
    designation 'premie (voor de vondst)' in the vocabulary 'handboek'
    carries a parenthesis, which the source form (<vocabulary>:
    <designation>) cannot write

The document is `corpus/golden/jutterswet-vocabulaires` with the `handboek`
designation of the attribute `premie` of `Vondst` written `premie (voor de
vondst)`, which the dialect's annotation could not write: its closing
parenthesis would end the annotation. The verifier reports it in one line,
the arm `expect.txt` pins.
