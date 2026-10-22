Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because the part is
still cyclic without its shifted edge.

The einddag of a Vergunning is its peildag plus the restant of its vorige
vergunning, the peildag is its einddag, and the restant is the number of
days from its stopdag to its einddag. Every read behind an edge of the cycle
is shifted or local: the restant of the predecessor is a shifted read, the
others are own reads. Without the shifted edge restant -> einddag, the edges
einddag -> peildag and peildag -> einddag still close a cycle on one object,
which no order of the objects breaks.

All edges that remain are positive. The levels of ch. 5.3 count a strict or
negative edge as one and a positive edge as zero, so a cycle of positive
edges raises no level. A test on the levels alone took the part as
instance-ordered, the shifted edge left the walk, and the verifier named
only the cycle einddag -> peildag -> einddag. With the clause "acyclic
without its shifted edges" tested on the edges themselves, the part is no
instance-ordered part, the shifted edge stays in the walk, and the cycle
through it is named as well.

Pin: `WF-045.dependency-cycle` with `cycle=restant -> einddag -> restant`,
the cycle that only a walk with the shifted edge finds. Measured 2026-10-22:
the verifier of `0866c03f` reported `einddag -> peildag -> einddag` alone,
so the pin failed there; the Drools backend, whose test of the part looks
for a cycle in the remaining edges, refused the document with the cycle
across all three members.

Written in RegelSpraak, emitted with a helper attribute in the peildag and
the read set back by hand:

```
Eenheidsysteem Tijd
    de dag dg

Objecttype de Vergunning (mv: Vergunningen)
    de stopdag Datum in dagen;
    de basiseinddag Datum in dagen;
    de einddag Datum in dagen;
    het restant Numeriek (geheel getal) met eenheid dg;
    de peildag Datum in dagen;

Feittype vorige vergunning
    de vergunning Vergunning
    de vorige vergunning Vergunning
Eén vergunning heeft één vorige vergunning

Regel restant
    geldig altijd
        Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen.

Regel einddag
    geldig altijd
        De einddag van een Vergunning moet berekend worden als zijn peildag plus het restant van de vorige vergunning van de Vergunning.

Regel peildag
    geldig altijd
        De peildag van een Vergunning moet gesteld worden op zijn einddag.
```
