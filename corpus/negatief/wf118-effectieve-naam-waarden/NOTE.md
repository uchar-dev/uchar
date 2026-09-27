# wf118-effectieve-naam-waarden

Violates WF-118 (d) (`spec/IR-SPEC.md` ch. 2.12): the effective names of an
enumeration's values are unique per vocabulary.

    WF-118 FAIL /model/domains/0/datatype/values/2: values 'barnsteen' and
    'flessenpost' of the enumeration 'Vondstsoort' share the effective name
    'Amber' in the vocabulary 'handboek'

The document is `corpus/golden/jutterswet-vocabulaires` with the `handboek`
designation of the value `flessenpost` written `Amber`, which is
`barnsteen`'s `amber` but for the case. The verifier reports it in one line,
the arm `expect.txt` pins.
