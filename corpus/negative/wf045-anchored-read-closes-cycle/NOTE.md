# wf045-anchored-read-closes-cycle

Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3 and 4.1a): an anchored read of a
derived member of the form subject in the filter of a fold closes a cycle: it
draws the edge the same read draws in the form's own condition (IR-SPEC ch.
5.1), and is not left out of the graph. Its path is empty (its own hops, IR-
SPEC ch. 5.3), the cycle walks no switch relation, so it is an ordinary cycle.

    WF-045.dependency-cycle FAIL /rules/rules/0/versions/0: dependency cycle
    through no recursive creation: hoogste latere tarief -> drempel -> hoogste
    latere tarief

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
`jutterswet-registervoorrang`, with an attribute `drempel` on the Vergunning,
a third rule `drempel` (the drempel of a Vergunning is its hoogste latere
tarief), and a third part in the filter of the first rule: `zijn tarief is
groter dan de drempel van de Vergunning zelf`.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
