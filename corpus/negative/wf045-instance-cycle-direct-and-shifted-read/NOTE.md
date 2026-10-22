Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because the rule
reads the restant of the predecessor and also its own restant.

The einddag of a Vergunning is its basiseinddag plus the restant of its
vorige vergunning plus its own restant, and the restant is the number of
days from its stopdag to its einddag. The edge restant -> einddag has a
shifted read and a direct one, so it is an ordinary edge, and with the
local edge einddag -> restant it closes a cycle on one object: the einddag
of V reads the restant of V, which reads the einddag of V. Taken as a
shifted edge, the edge would leave the graph and the verifier would admit
a cycle that no order of the objects breaks.

Written in RegelSpraak, emitted with a helper attribute in the restant and
the read set back by hand:

```
Regel restant
    geldig altijd
        Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen.

Regel einddag
    geldig altijd
        De einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het restant van de vorige vergunning van de Vergunning plus zijn restant.
```
