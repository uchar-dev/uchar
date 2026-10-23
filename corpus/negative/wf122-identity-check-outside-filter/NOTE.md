# wf122-identity-check-outside-filter

Violates WF-122 (c) (`spec/IR-SPEC.md` ch. 4.5 and 4.6): an `IdentityCheck`
stands in the form's own condition, where there is no member to compare with
the form subject.

    WF-122.identity-outside-context FAIL
    /rules/rules/0/versions/0/condition: an IdentityCheck in the form's own
    condition or value, which is no per-element context the anchor stands in:
    it compares the element of the filter of an instance fold, or of the
    filter or the test of a quantifier fold, with the subject of the form
    (IR-SPEC ch. 4.5)

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
`jutterswet-registervoorrang`, with the condition `hij is de Vergunning zelf`
on the first rule: an `IdentityCheck` at the form's level.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
