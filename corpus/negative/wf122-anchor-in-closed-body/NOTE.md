# wf122-anchor-in-closed-body

Violates WF-122 (a) (`spec/IR-SPEC.md` ch. 4.1a and 4.6): an anchored read
stands in the body of a `FunctionDef`, which reads no subject at all; WF-067
refuses the read in the closed body beside it.

    WF-122.anchor-outside-context FAIL
    /model/functionDefs/0/body/anchor: the anchor "form" of this
    AttributeReference stands in a closed function or lambda body, which is no
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
`jutterswet-registervoorrang`, with a `FunctionDef` whose body is an anchored
read of the ingangsdag of the Vergunning.

The verifier reports beside it: `WF-067.inadmissible-in-closed`.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
