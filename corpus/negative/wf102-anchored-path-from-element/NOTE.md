# wf102-anchored-path-from-element

Violates WF-102 (d) (`spec/IR-SPEC.md` ch. 4.3 and 4.1a): the range of an
anchored fold starts at the form subject, and this `sourcePath` starts at the
member's type: its first step leaves a `Vergunning`, while the anchored fold
stands on the `Register`, the form subject. WF-102 judges the path from the
form subject and refuses its first join.

    WF-102.path-not-connected FAIL
    /rules/rules/2/versions/0/value/filter/right/sourcePath/0: the opposite
    role 'vergunning' fills 'Vergunning', but this step starts at 'Register'
    (the fold subject) — a sourcePath is a connected walk

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
`jutterswet-registervoorrang`, with an attribute `hoogste uitschieter` on the
Register and a third rule: the maximum of the tarief of the vergunningen of
the Register whose tarief is greater than an anchored maximum over
`registerinschrijving/register; registerinschrijving/vergunning`, the path of
the hub rules, which starts at a Vergunning.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
