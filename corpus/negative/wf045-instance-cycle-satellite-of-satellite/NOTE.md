Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because a read
behind one of its edges walks from a satellite to a satellite of its own.

The verlengde einddag of a Vergunning is its basiseinddag plus the overschot
of the deeltermijn of the termijn of its vorige vergunning, and the overschot
of a Deeltermijn is the number of days from the stopdag to the verlengde
einddag of the houder of its hoofdtermijn. A Termijn is a satellite of the
Vergunning (`vergunningstermijn`), a Deeltermijn a satellite of the Termijn
(`onderverdeling`), not of the Vergunning. The depth of a Deeltermijn is
derivable (that of the owner of its owner), but the instance-ordered part
records one layer of satellites: the depth of a satellite is read over the
one hop to its owner, and "one satellite relation per satellite type" is a
clause over the relations of the cycle's type. Both reads of the cycle cross
that second layer.

Clause: `satellite-of-satellite` (`verifier --arms`:
`WF-045.instance-read-outside`, once for each of the two forms).
Measured 2026-10-22: with a hop between two types that are not comparable
with Vergunning counted as a household hop in the verifier, the document is
verifier-clean; the witness fails on this clause.

Written in RegelSpraak, emitted with a helper attribute in the overschot and
the read set back by hand:

```
Objecttype de Vergunning (mv: Vergunningen)
    de stopdag Datum in dagen;
    de basiseinddag Datum in dagen;
    de verlengde einddag Datum in dagen;

Objecttype de Termijn (mv: Termijnen)
    het nummer Numeriek (geheel getal);

Objecttype de Deeltermijn (mv: Deeltermijnen)
    het overschot Numeriek (geheel getal) met eenheid dg;

Feittype vorige vergunning
    de vergunning Vergunning
    de vorige vergunning Vergunning
Eén vergunning heeft één vorige vergunning

Feittype vergunningstermijn
    de houder Vergunning
    de termijn Termijn
Eén houder heeft één termijn

Feittype onderverdeling
    de hoofdtermijn Termijn
    de deeltermijn Deeltermijn
Eén hoofdtermijn heeft één deeltermijn

Regel overschot
    geldig altijd
        Het overschot van een Deeltermijn moet berekend worden als de tijdsduur van de stopdag van de houder van de hoofdtermijn van de Deeltermijn tot de verlengde einddag van de houder van de hoofdtermijn van de Deeltermijn in hele dagen.

Regel verlengde einddag
    geldig altijd
        De verlengde einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het overschot van de deeltermijn van de termijn van de vorige vergunning van de Vergunning.
```
