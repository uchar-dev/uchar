# wf121-origin-source-unknown

Violates WF-121 (b) (`spec/IR-SPEC.md` ch. 2.13): an origin's `from` names a
declaration of its kind in the document.

    WF-121 FAIL
    /model/objectTypes/0/attributes/4/origin/from: the origin of the
    attribute 'berekende waarde' of 'Vondst' comes from the attribute 'waard'
    of 'Vondst', which the document does not declare

The document is `corpus/golden/jutterswet-opgave-bedrag` with the `from` of
the origin of the attribute `berekende waarde` of `Vondst` naming the
attribute `waard`, a misspelling of the marked attribute `waarde`. No
executor reads the origin, so the document replays as the golden does; only
the verifier sees that the source names nothing. It reports it in one line,
the arm `expect.txt` pins.
