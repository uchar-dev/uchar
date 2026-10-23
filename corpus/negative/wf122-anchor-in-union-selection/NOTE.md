# wf122-anchor-in-union-selection

Violates WF-122 (a) (`spec/IR-SPEC.md` ch. 4.1a and 4.6): an anchored
characteristic check stands in the filter of a selection of an instance-set
union. That filter is a per-element context, and the anchor would mean there
what it means in a fold's filter; it is not admitted there until a sentence of
the dialect and a golden carry it (IR-SPEC ch. 4.1a, the positions the anchor
is not admitted at).

    WF-122.anchor-outside-context FAIL /rules/rules/0/versions/0/value
    /arguments/0/branches/0/filter/parts/1/anchor: the anchor "form" of this
    CharacteristicCheck stands in the filter of a selection of an instance-set
    union, which is no per-element context the anchor stands in: it reads the
    subject of the form where the filter of an instance fold, or the filter or
    the test of a quantifier fold, has moved the subject to the element
    (IR-SPEC ch. 4.1a)

The document is `corpus/golden/jutterswet-keurunie` with a characteristic
`ervaren` added to the Jutter and the filter of the first selection of its
first rule widened to `gemerkt zijn en waarvan de Jutter zelf ervaren is`: a
composite of the member's check and an anchored check of the Jutter's
`ervaren`.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
