# wf117-onbekend-vocabulaire

Violates WF-117 (a) (`spec/IR-SPEC.md` ch. 2.12): a designation names a
declared vocabulary.

    WF-117 FAIL /model/objectTypes/1/designations/0/vocabulary: designation
    'aangespoeld voorwerp' names the vocabulary 'wet', which the document
    does not declare

The document is `corpus/golden/jutterswet-vocabulaires` with the `handboek`
designation of the object type `Vondst` moved to the vocabulary `wet`, which
the document does not declare. The verifier reports it in one line, the arm
`expect.txt` pins.
