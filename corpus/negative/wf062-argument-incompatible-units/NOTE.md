Violates WF-062 (`spec/IR-SPEC.md` ch. 4.7), the typing arm. A Vondst whose `vindloonbesluit` is a sum type `Toegekend(getal met 2 decimalen) | Afgewezen` over the domain
`vindloonbesluit`. The rule "vast vindloonbesluit" writes
`Construct Toegekend(gewicht)`, and `gewicht` is declared in `kg` while the payload carries no unit: the
argument is written into the payload, and a value in `kg` is not assignable to a unitless number (WF-023's unit
clause), although the two datatypes are of one family and of the same decimals.
