# wf118-effective-name-roles

Violates WF-118 (c) (`spec/IR-SPEC.md` ch. 2.12): the effective names of a
fact type's roles are unique per vocabulary.

    WF-118 FAIL /model/factTypes/0/roles/1: roles 'aanmelder' and 'vondst'
    of the fact type 'vondstaanmelding' share the effective name 'Melder' in
    the vocabulary 'handboek'

The document is `corpus/golden/jutterswet-vocabulaires` with the `handboek`
designation of the role `vondst` written `Melder`, which is the role
`aanmelder`'s `melder` but for the case. The verifier reports it in one
line, the arm `expect.txt` pins.
