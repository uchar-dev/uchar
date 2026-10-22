Violates WF-045 (`spec/IR-SPEC.md` ch. 5.3): a dependency cycle through no
recursive creation that is not instance-ordered either, because a read
behind one of its edges crosses a hub: a relation whose role on the side of
the cycle's type is to-many.

The einddag of a Vergunning is its basiseinddag when its vorige vergunning
has no blokkade (a shifted read under a negation); a Vergunning has the
openstand when its einddag is leeg; and a Vergunning has the blokkade when at
least one of the geregistreerde vergunningen of its register has the
openstand. The register registers several Vergunningen, so the read of the
openstand goes to the register and back to every Vergunning it holds, the
successor of the subject included. That read leaves the household of the
subject; the depth of the chain does not order it.

Clause: `hub` (`verifier --arms`: `WF-045.instance-read-outside`, form
`blokkade`, path `registratie/register; registratie/geregistreerde
vergunning`). Measured 2026-10-22: with the role `geregistreerde vergunning`
made to-one, and nothing else changed, the read is local and the document is
verifier-clean; the witness fails on this clause.

Why the clause holds. Case: A without a predecessor, B with vorige vergunning
A, both registered in register X (A basiseinddag 2026-06-01, B basiseinddag
2026-12-01). The chain A → B ends, so EF-033 does not apply. The ASP
backend's program for this document and case (`backends/clingo`, the program
its runner hands to clingo, run with `clingo -n 0`, measured 2026-10-22) has
two stable models:

1. A einddag 2026-06-01; B openstand; A and B blokkade; B einddag leeg.
2. A einddag 2026-06-01; B einddag 2026-12-01; no openstand, no blokkade.

In the first, the blokkade of A rests on the openstand of B, which rests on
the leeg einddag of B, which rests on the blokkade of A. No order of the
objects by their depth settles that: B's openstand is read before B's
einddag exists. The verifier refuses the form, so no executor has to choose.

Own material: the miniature of a read over the samenloop of rights in a
statute rule set, made for the review of 2026-10-22 that kept the hub outside
ch. 5.3. Written in RegelSpraak, emitted with a helper attribute in the
openstand and the read set back by hand:

```
Objecttype de Vergunning (mv: Vergunningen)
    de basiseinddag Datum in dagen;
    de einddag Datum in dagen;
    de blokkade kenmerk (bezittelijk);
    de openstand kenmerk (bezittelijk);

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

Regel einddag
    geldig altijd
        De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
        indien de vorige vergunning van de Vergunning geen blokkade heeft.

Regel einddag zonder vorige
    geldig altijd
        De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
        indien de basiseinddag van de vorige vergunning van de Vergunning leeg is.

Regel openstand
    geldig altijd
        Een Vergunning heeft de openstand
        indien zijn einddag leeg is.

Regel blokkade
    geldig altijd
        Een Vergunning heeft de blokkade
        indien ten minste één van de geregistreerde vergunningen van het register van de Vergunning de openstand heeft.
```
