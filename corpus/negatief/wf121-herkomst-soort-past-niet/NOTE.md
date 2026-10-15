# wf121-herkomst-soort-past-niet

Violates WF-121 (d) (`spec/IR-SPEC.md` ch. 2.13): the kind of an origin's
`from` is the one its form names.

    WF-121 FAIL
    /model/objectTypes/0/attributes/4/origin/from/kind: the origin of the
    attribute 'berekende waarde' of 'Vondst' has the form
    'inputAboveComputation', which comes from a source of kind 'attribute',
    and its source is of kind 'namedForm'

The document is `corpus/golden/jutterswet-opgave-bedrag` with the `from` of
the origin of the attribute `berekende waarde` of `Vondst` naming the rule
`afwijking opgave waarde`, a named form the document declares, so (b)
holds; but an `inputAboveComputation` origin comes from the marked
attribute. The verifier reports it in one line, the arm `expect.txt` pins.
