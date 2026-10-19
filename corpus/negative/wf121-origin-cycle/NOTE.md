# wf121-origin-cycle

Violates WF-121 (c) (`spec/IR-SPEC.md` ch. 2.13), its second arm: following
`from` through the declarations that carry an origin never comes back.

    WF-121 FAIL
    /model/objectTypes/0/attributes/3/origin/from: following 'from' from the
    attribute 'opgegeven waarde' of 'Vondst' through the attribute
    'berekende waarde' of 'Vondst' comes back to it: the origins form a cycle

The document is `corpus/golden/jutterswet-opgave-bedrag` with the origins of
two attributes of `Vondst` pointing at each other: `opgegeven waarde` comes
from `berekende waarde`, and `berekende waarde` from `opgegeven waarde`.
Each `from` resolves under (b), and neither names its own declaration. A
`from` may name a made declaration (ch. 2.13), so the chain is followed, and
the cycle is reported once, at its member that comes first in the document.
The verifier reports it in one line, the arm `expect.txt` pins.
