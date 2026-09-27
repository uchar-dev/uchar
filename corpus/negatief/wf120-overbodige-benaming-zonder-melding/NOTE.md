# wf120-overbodige-benaming-zonder-melding

Violates WF-120 (`spec/IR-SPEC.md` ch. 2.12): a redundant designation is
accounted for by a warning.

    WF-120 FAIL
    /model/objectTypes/0/attributes/0/designations/0/designation:
    designation 'naam' in the vocabulary 'handboek' spells the canonical
    name of the attribute 'naam' of 'Jutter', and no diagnostic accounts for
    it: a redundant designation is admitted with a warning whose message
    contains it

The document is `corpus/golden/jutterswet-vocabulaires` with the frontend's
warning removed from `diagnostics`; the `handboek` designation `naam` of the
attribute `naam` of `Jutter`, which spells its canonical name, stays. The
verifier reports it in one line, the arm `expect.txt` pins.
