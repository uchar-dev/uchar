# wf045-instance-cycle-hub-beside-anchored-read

Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3 and 4.1a): beside the read of the
member over a hub, the filter of the fold reads a derived member of the form
subject with the anchor. The refusal names the member's read over the hub
(`clause` `hub`) and does not name the anchored read: its path is its own
hops, here none, so it is local (IR-SPEC ch. 5.3). The cycle the verifier
reports first is the shorter one the anchored read closes (`einddag ->
blokkade -> einddag`), and WF-046 adds the negative edge of its leeg-test.

    WF-045.instance-read-outside FAIL /rules/rules/3/versions/0: read outside
    the instance-ordered cycle (ch. 5.3): blokkade reads
    char:vergunning:openstand over [registratie/register;
    registratie/geregistreerde vergunning], which falls on the clause hub

The document is `corpus/negative/wf045-instance-cycle-hub-negation` with a
filter on the quantifier fold of the rule `blokkade`: `de einddag van de
Vergunning zelf gevuld is`, a filled-test of an anchored read of the subject's
derived einddag.

The verifier reports beside it: `WF-045.dependency-cycle`, `WF-046.negative-
edge-cycle`.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Before the card `subselection-reads-subject` (2026-10-23) every
reader refused the document at load, on the field `anchor` or the node
`IdentityCheck`, which the wire did not have.
