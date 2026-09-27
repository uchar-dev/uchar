# wf118-effectieve-naam-attributen

Violates WF-118 (b) (`spec/IR-SPEC.md` ch. 2.12): the effective names of an
object type's attributes are unique per vocabulary.

    WF-118 FAIL /model/objectTypes/0/attributes/2: attributes 'vondstbedrag'
    and 'aantal waardevolle vondsten' of the object type 'Jutter' share the
    effective name 'aantal waardevolle vondsten' in the vocabulary
    'handboek'

The document is `corpus/golden/jutterswet-vocabulaires` with the collision
of a designation with a CANONICAL name, the case the effective name exists
for: the attribute `aantal waardevolle vondsten` of `Jutter` loses its
`handboek` designation, so its effective name there is its canonical name,
and `vondstbedrag` is designated `Aantal Waardevolle Vondsten` in
`handboek`. `handboek` is not the default, so the missing designation is no
defect of its own. The verifier reports it in one line, the arm `expect.txt`
pins.
