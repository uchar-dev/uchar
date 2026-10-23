# wf122-anchor-on-rule-target

Violates WF-122 (a) (`spec/IR-SPEC.md` ch. 4.1a and 4.6): the target of a rule
version carries the anchor: a target names what the form writes on its subject
and is no read. The schema admits the field there, because the target is an
`AttributeReference`; the verifier refuses it.

    WF-122.anchor-outside-context FAIL
    /rules/rules/0/versions/0/target/anchor: the anchor "form" of this
    AttributeReference stands in the target of a rule version, which is no
    per-element context the anchor stands in: it reads the subject of the form
    where the filter of an instance fold, or the filter or the test of a
    quantifier fold, has moved the subject to the element (IR-SPEC ch. 4.1a)

The document is the hub model: a `Vergunning` (`ingangsdag`, `tarief`,
`hoogste latere tarief`, the characteristic `voorrang`) and a `Register`,
related by the fact type `registerinschrijving` (one register, many
vergunningen), with two rules on the Vergunning: `hoogste latere tarief`, the
maximum of the tarief of the other vergunningen of its register that start
later than the Vergunning itself (an `IdentityCheck` and an anchored read in
the filter of a fold over `registerinschrijving/register;
registerinschrijving/vergunning`), and `voorrang`, a quantifier `all` over the
same range with the identity check in its filter and the anchored read in its
test. That document is verifier-clean; it is the draft of the golden
`jutterswet-registervoorrang`, with `anchor: "form"` on the target of the
first rule.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
