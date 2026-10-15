# wf121-herkomst-vorm-onbekend

Violates WF-121 (a) (`spec/IR-SPEC.md` ch. 2.13): an origin's `form` is one
of the five forms, byte for byte.

    WF-121 FAIL
    /model/objectTypes/0/attributes/4/origin/form: the origin form
    'opgaveBovenBerekening' is none of the forms of IR-SPEC ch. 2.13
    ('inputAboveComputation', 'exceptionForSubtype', 'decisionTableRow',
    'stepsAfterSet', 'startPoint')

The document is `corpus/golden/jutterswet-opgave-bedrag` with the `form` of
the origin of the attribute `berekende waarde` of `Vondst` written
`opgaveBovenBerekening`: the frontend's own key for that form, in the case
of the wire's spelling, which the closed set does not admit. Every executor
refuses the same document at load in the same sentence, without the rule's
number. The verifier reports it in one line, the arm `expect.txt` pins.
