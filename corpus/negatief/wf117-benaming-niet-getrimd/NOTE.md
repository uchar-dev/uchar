# wf117-benaming-niet-getrimd

Violates WF-117 (c) (`spec/IR-SPEC.md` ch. 2.12): a designation is not empty
and trimmed at both ends.

    WF-117 FAIL /model/objectTypes/0/designations/0/designation: designation
    'strandjutter ' in the vocabulary 'handboek' is empty or not trimmed: a
    designation is text without whitespace at either end

The document is `corpus/golden/jutterswet-vocabulaires` with the `handboek`
designation of `Jutter` written with a trailing space, `strandjutter `; the
dialect trims what it reads, so only a hand-written document can carry it.
The verifier reports it in one line, the arm `expect.txt` pins.
