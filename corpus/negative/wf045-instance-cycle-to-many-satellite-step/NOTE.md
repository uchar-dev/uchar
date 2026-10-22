Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because the step
after the hop to the predecessor reaches a to-many role.

The verlengde einddag of a Vergunning is its basiseinddag plus the sum of
the overschotten of all termijnen of its vorige vergunning; the overschot
of a Termijn is the number of days from the stopdag to the verlengde
einddag of its houder. A Vergunning has several termijnen ("Eén houder
heeft meerdere termijnen"), so the fold's range after the hop over `vorige
vergunning` is a set. A shifted read lands on one object, the predecessor
or its one satellite; a range over several satellites of the predecessor
is a later extension, as a set of predecessors is
(`RUNTIME-SEMANTICS.md` ch. 6.2).

Measured 2026-10-22: with the role `termijn` made to-one, and nothing else
changed, the document is verifier-clean; the witness fails on this clause
alone.

Written in RegelSpraak, emitted with a helper attribute in the overschot
and the read set back by hand:

```
Feittype vergunningstermijn
    de houder Vergunning
    de termijn (mv: termijnen) Termijn
Eén houder heeft meerdere termijnen

Regel overschot
    geldig altijd
        Het overschot van een Termijn moet berekend worden als de tijdsduur van de stopdag van de houder van de Termijn tot de verlengde einddag van de houder van de Termijn in hele dagen.

Regel verlengde einddag
    geldig altijd
        De verlengde einddag van een Vergunning moet berekend worden als zijn basiseinddag plus de som van de overschotten van alle termijnen van de vorige vergunning van de Vergunning.
```
