# wf118-effective-name-object-types

Violates WF-118 (a) (`spec/IR-SPEC.md` ch. 2.12): the effective names of the
object types are unique per vocabulary.

    WF-118 FAIL /model/objectTypes/1: object types 'Jutter' and 'Vondst'
    share the effective name 'Strandjutter' in the vocabulary 'handboek'

The document is `corpus/golden/jutterswet-vocabulaires` with the `handboek`
designation of `Vondst` written `Strandjutter`, which is `Jutter`'s
`strandjutter` but for the case: in `handboek` the name would go back to two
object types. The verifier reports it in one line, the arm `expect.txt`
pins.
