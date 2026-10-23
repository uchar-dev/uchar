# wf045-instance-cycle-hub-filter-without-anchor

Violates WF-045 (`spec/IR-SPEC.md` ch. 5.1 and 5.3): `eind` reads `som`, and
`som` reads `eind` of the member's vorige vergunning in the filter of a sum
over the Vergunningen of the Vergunning's register. That read starts at the
member, so its path is the fold's range followed by its own hop
(`registerinschrijving/register; registerinschrijving/vergunning; vorige
vergunning/vorige vergunning`): no shifted read over a switch relation, and
the cycle is not instance-ordered.

    WF-045.dependency-cycle FAIL /rules/rules/0/versions/0: dependency cycle
    through no recursive creation: eind -> som -> eind

This is the mirror of the golden `anchor-hub-own-hops`: the same document
with `anchor: "form"` on that read is verifier-clean, because an anchored read
starts at the form subject and its path is its own hop alone (IR-SPEC ch.
4.1a, 5.3), a shifted read over the switch relation `vorige vergunning`. The
pair pins that difference: a verifier that gave the anchored read the range of
the fold around it would refuse the golden as it refuses this document.

The arm `expect.txt` pins is the key and its parameters as `verifier --arms`
prints them. Written in fix round A of the card `subselection-reads-subject`
(2026-10-23), after proof p19 of review A.
