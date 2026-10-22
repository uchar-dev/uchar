Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because a form
writes the relation it walks.

The einddag of a Vergunning is its basiseinddag plus the restant of its
vorige vergunning, and the restant is the number of days from its stopdag
to its einddag: the shape of `corpus/golden/jutterswet-vergunningketen`.
Here the fact creation "vergunningen" also links every opvolger of a
Vergunning as one of its vergunningen, so the relation `vorige vergunning`
has a writer. Its links are then not the case's alone, the order of the
chain is known only after the evaluation, and the relation is no switch
relation; the edge restant -> einddag is an ordinary edge.

Measured 2026-10-22: without the fact creation the document is
verifier-clean; the witness fails on this clause alone.

Written in RegelSpraak, emitted with a helper attribute in the restant and
the read set back by hand:

```
Feittype vorige vergunning
    de vorige vergunning Vergunning
    de vergunning (mv: vergunningen) Vergunning
Eén vorige vergunning heeft meerdere vergunningen

Feittype opvolging
    de voorganger Vergunning
    de opvolger (mv: opvolgers) Vergunning
Eén voorganger heeft meerdere opvolgers

Regel restant
    geldig altijd
        Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen.

Regel einddag
    geldig altijd
        De einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het restant van de vorige vergunning van de Vergunning.

Regel vergunningen
    geldig altijd
        Een vergunning van een Vergunning is een opvolger van de Vergunning.
```
