# wf116-twee-standaardvocabulaires

Violates WF-116 (c) (`spec/IR-SPEC.md` ch. 2.12): at most one vocabulary is
the default.

    WF-116 FAIL /vocabularies/1/default: vocabularies 'handboek' and 'cgm'
    are both the default: a document has at most one default vocabulary

The document is `corpus/golden/jutterswet-vocabulaires` with `handboek`
marked `default: true` beside `cgm`; `handboek` names every declaration as
well, so WF-119 has nothing to say and the arm is the only defect. The
verifier reports it in one line, the arm `expect.txt` pins.
