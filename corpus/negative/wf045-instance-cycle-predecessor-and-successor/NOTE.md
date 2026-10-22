Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because its reads
walk one relation to both of its roles.

The saldo of a Vergunning is its inleg plus the overschot of its vorige
vergunning, and the overschot of a Vergunning is the saldo of its volgende
vergunning: both reads take one hop over `opvolging`, one to the role of the
predecessor and one to the role of the successor. Each would be a shifted
read on its own. Together they read, per object, the saldo of V through the
overschot of its predecessor, whose successor is V: a cycle on one object.
An instance-ordered part walks one relation to one role.

Measured 2026-10-22: with the verifier taking every hop over a switch
relation as shifted, whatever role it reaches, the document is
verifier-clean; the witness fails on this clause.

Written in RegelSpraak, emitted with a helper attribute in the overschot and
the read set back by hand:

```
Feittype opvolging
    de volgende vergunning Vergunning
    de vorige vergunning Vergunning
Eén volgende vergunning heeft één vorige vergunning

Regel saldo
    geldig altijd
        Het saldo van een Vergunning moet berekend worden als zijn inleg plus het overschot van de vorige vergunning van de Vergunning.

Regel overschot
    geldig altijd
        Het overschot van een Vergunning moet gesteld worden op het saldo van de volgende vergunning van de Vergunning.
```
