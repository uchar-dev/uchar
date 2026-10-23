# wf122-anchor-in-gedurende-clip

Violates WF-122 (a) (`spec/IR-SPEC.md` ch. 4.1a and 4.6): an anchored
characteristic check stands in the condition of a gedurende clip, whose
subject is the subject of the clip itself (WF-097's three clips): the anchor
there is a second spelling of the check.

    WF-122.anchor-outside-context FAIL
    /rules/rules/0/versions/0/value/filter/anchor: the anchor "form" of this
    CharacteristicCheck stands in the condition of a gedurende clip, which is
    no per-element context the anchor stands in: it reads the subject of the
    form where the filter of an instance fold, or the filter or the test of a
    quantifier fold, has moved the subject to the element (IR-SPEC ch. 4.1a)

The document is `corpus/golden/jutterswet-getijdenwacht` with `anchor: "form"`
on the clip's check `wachtplichtig` in the temporal total of its first rule.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
