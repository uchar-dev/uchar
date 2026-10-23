# wf122-anchored-check-with-host

Violates WF-122 (b) (`spec/IR-SPEC.md` ch. 4.1a and 4.6): an anchored
characteristic check carries a `host`: the anchor names the object the check
reads, so a host names it a second time or names another. Here it names the
form subject's type, so WF-109 finds its one object and only this arm refuses
it.

    WF-122.anchored-check-host FAIL
    /rules/rules/0/versions/0/value/filter/parts/2/host: the characteristic
    check on 'geschorst' carries anchor "form" and the host 'Vergunning': the
    anchor names the form subject as the object the check reads, so a host
    names it a second time, or names another (IR-SPEC ch. 4.1a)

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
`jutterswet-registervoorrang`, with a characteristic `geschorst` on the
Vergunning and a third part in the filter of the first rule: `de Vergunning
zelf is niet geschorst`, a negated anchored check with `host: "Vergunning"`.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
