Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because a read
behind one of its edges is neither shifted nor local.

The saldo of a Vergunning is its inleg plus the overschot of its vorige
vergunning (a shifted read), and the overschot of a Vergunning is the sum of
the saldi of all geregistreerde vergunningen of its register: two hops, to
the register and back to every Vergunning it registers. That read lands on
other objects of the cycle's type, whose depth the order of the chain does
not bound. Where the successor of V is registered with V, the overschot of V
reads the saldo of its successor, which reads the overschot of V: a cycle per
object that no order of the objects breaks. The edge saldo -> overschot is no
shifted edge, so the cycle stays; were the read counted as local, the graph
without the shifted edge would be acyclic and the verifier would admit the
document.

Measured 2026-10-22: with the class of such a read relaxed to local in the
verifier, the document is verifier-clean; the witness fails on this clause.

Since 2026-10-22 (card `instance-cycle-household-reads`) the verifier names
the read beside the cycle: `WF-045.instance-read-outside`, form
`overschot`, path `registratie/register; registratie/geregistreerde
vergunning`, clause `hub` (the register is a hub: the role of the Vergunning
in `registratie` is to-many). The pin is that arm.

Written in RegelSpraak, emitted with a helper attribute in the overschot and
the read set back by hand:

```
Objecttype de Vergunning (mv: Vergunningen)
    de inleg Numeriek (geheel getal);
    het saldo (mv: saldi) Numeriek (geheel getal);
    het overschot Numeriek (geheel getal);

Objecttype het Register (mv: Registers)
    het nummer Numeriek (geheel getal);

Feittype vorige vergunning
    de vergunning Vergunning
    de vorige vergunning Vergunning
Eén vergunning heeft één vorige vergunning

Feittype registratie
    het register Register
    de geregistreerde vergunning (mv: geregistreerde vergunningen) Vergunning
Eén register heeft meerdere geregistreerde vergunningen

Regel saldo
    geldig altijd
        Het saldo van een Vergunning moet berekend worden als zijn inleg plus het overschot van de vorige vergunning van de Vergunning.

Regel overschot
    geldig altijd
        Het overschot van een Vergunning moet berekend worden als de som van de saldi van alle geregistreerde vergunningen van het register van de Vergunning.
```
