# wf117-value-designation-unknown-value

Violates WF-117 (e) (`spec/IR-SPEC.md` ch. 2.12): a value designation names
a value of its enumeration.

    WF-117 FAIL /model/domains/0/datatype/valueDesignations/6/value: a value
    designation names 'gruis', which the enumeration 'Vondstsoort' does not
    declare

The document is `corpus/golden/jutterswet-vocabulaires` with one more entry
in the `valueDesignations` of `Vondstsoort`, for the value `gruis`, which
the enumeration does not declare; added rather than renamed, so no value
loses its designation in `cgm`. The verifier reports it in one line, the arm
`expect.txt` pins.
