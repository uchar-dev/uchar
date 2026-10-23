# wf122-anchor-on-fold-source

Violates WF-122 (a) (`spec/IR-SPEC.md` ch. 4.1a and 4.6): the source of an
instance fold carries the anchor. The source names the member each contributor
gives and is read on the contributor (WF-083), so it has no subject to anchor;
the anchor of a fold stands on the fold.

    WF-122.fold-source-anchored FAIL
    /rules/rules/0/versions/0/value/arguments/0/anchor: the source of a
    maximum aggregation carries anchor "form": a fold source names the member
    each contributor gives and is read on the contributor, never on a subject,
    so it has no subject to anchor; an anchor on the aggregation starts its
    range at the form subject (IR-SPEC ch. 4.1a)

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
`jutterswet-registervoorrang`, with `anchor: "form"` on the source `tarief` of
the maximum of the first rule.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
