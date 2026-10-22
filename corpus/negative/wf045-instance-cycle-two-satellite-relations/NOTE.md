Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because it reaches
its satellite type through two satellite relations.

The saldo of a Vergunning is its inleg plus the overschot of the lopende
termijn of its vorige vergunning (a shifted read over `vorige vergunning`
and the satellite relation `vergunningstermijn`), and the overschot of a
Termijn is the saldo of its reservehouder (a local read over the other
satellite relation, `reservetermijn`). Each relation alone gives a Termijn
at most one owner, but the two together give it two: the Vergunning whose
lopende termijn it is and the one whose reserve it is. Where those are A and
the successor of A, the saldo of the successor reads the overschot of the
Termijn, which reads the saldo of the successor: a cycle on one object.

Measured 2026-10-22: with the verifier's test for two satellite relations of
one satellite type switched off, the document is verifier-clean; the witness
fails on this clause.

Written in RegelSpraak, emitted with a helper attribute in the overschot and
the read set back by hand:

```
Feittype vergunningstermijn
    de houder Vergunning
    de lopende termijn Termijn
Eén houder heeft één lopende termijn

Feittype reservetermijn
    de reservehouder Vergunning
    de reserve Termijn
Eén reservehouder heeft één reserve

Regel saldo
    geldig altijd
        Het saldo van een Vergunning moet berekend worden als zijn inleg plus het overschot van de lopende termijn van de vorige vergunning van de Vergunning.

Regel overschot
    geldig altijd
        Het overschot van een Termijn moet gesteld worden op het saldo van de reservehouder van de Termijn.
```
