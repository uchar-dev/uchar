Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because the
relation it walks reaches a to-many role.

The einddag of a Vergunning is its basiseinddag plus the sum of the
restanten of all its vorige vergunningen, and the restant of a Vergunning
is the number of days from its stopdag to its einddag. The fold walks
`vorige vergunning` to the role `vorige vergunning`, which is to-many
("Eén vergunning heeft meerdere vorige vergunningen"): an object has a set
of predecessors, not one, so the hop is no hop over a switch relation and
the edge restant -> einddag is an ordinary edge. A fold over several
predecessors (depth one more than the deepest) is a later extension
(`RUNTIME-SEMANTICS.md` ch. 6.2).

Measured 2026-10-22: with the role made to-one (`many: false`) and
nothing else changed, the document is verifier-clean; the witness fails on
this clause alone.

Written in RegelSpraak, emitted by the frontend with the einddag in the
restant spelled as a helper attribute (`check` refuses the cycle before it
emits), then the read set back by hand:

```
Feittype vorige vergunning
    de vergunning Vergunning
    de vorige vergunning (mv: vorige vergunningen) Vergunning
Eén vergunning heeft meerdere vorige vergunningen

Regel restant
    geldig altijd
        Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen.

Regel einddag
    geldig altijd
        De einddag van een Vergunning moet berekend worden als zijn basiseinddag plus de som van de restanten van alle vorige vergunningen van de Vergunning.
```
