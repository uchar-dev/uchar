# wf053-anchored-own-read-element-type

Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a): an anchored read without a
`path` is the FORM subject's own read, so its `subjectType` is the form
subject type. This one names `Vergunning`, the member's type, in a rule on the
`Register`: a reader that ignored the anchor would admit it as the member's
own read, and read the member where the sentence reads the form subject.
WF-053 judges the anchored read against the form subject and refuses it under
its own arm.

    WF-053.read-other-type FAIL /rules/rules/2/versions/0/value/filter/right:
    an attribute reference without a path is the subject's own read, so its
    subjectType is the subject type 'Register' — 'Vergunning' names another
    type, which only a navigation reaches

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
`jutterswet-registervoorrang`, with an attribute `hoogste tarief` on the
Register and a third rule: the maximum of the tarief of the vergunningen of
the Register (`sourceRole registerinschrijving/vergunning`) whose ingangsdag
is later than an anchored read `ingangsdag` with `subjectType: "Vergunning"`.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
