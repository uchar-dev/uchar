Violates WF-062 (`spec/IR-SPEC.md` ch. 4.7), the precision arm. A Vondst whose `vindloonbesluit` is a sum type `Toegekend(getal met 2 decimalen) | Afgewezen` over the domain
`vindloonbesluit`. The rule "vast vindloonbesluit" writes
`Construct Toegekend(gewicht)`, and `gewicht` is declared with 3 decimals: the argument is written into the
payload, which declares 2, so its precision bound (`spec/RUNTIME-SEMANTICS.md` ch. 2.3) exceeds the payload's, as
the same read written into an attribute of 2 decimals exceeds it under WF-023.
