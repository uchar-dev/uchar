Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because the
object it reads beside the predecessor is shared by several Vergunningen.

The verlengde einddag of a Vergunning is its basiseinddag plus the
overschot of the termijn of its vorige vergunning; the overschot of a
Termijn is the sum of the restanten of all its houders; the restant of a
Vergunning is the number of days from its stopdag to its verlengde
einddag. One Termijn has several houders ("Eén termijn heeft meerdere
houders"): the role back from the Termijn to the Vergunning is to-many, so
`vergunningstermijn` is no satellite relation and the Termijn belongs to
more than one object of the cycle. With a Termijn shared by A and B, the
verlengde einddag of the successor of B reads the overschot that A's
restant feeds, which the depth of B does not order.

Measured 2026-10-22: with the role `houder` made to-one, and nothing else
changed, the document is verifier-clean (the hop to the termijn is then a
satellite step and the fold over the one houder a local read); the witness
fails on this clause alone.

Written in RegelSpraak, emitted with a helper attribute in the restant and
the read set back by hand:

```
Feittype vergunningstermijn
    de termijn Termijn
    de houder (mv: houders) Vergunning
Eén termijn heeft meerdere houders

Regel restant
    geldig altijd
        Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn verlengde einddag in hele dagen.

Regel overschot
    geldig altijd
        Het overschot van een Termijn moet berekend worden als de som van de restanten van alle houders van de Termijn.

Regel verlengde einddag
    geldig altijd
        De verlengde einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het overschot van de termijn van de vorige vergunning van de Vergunning.
```
