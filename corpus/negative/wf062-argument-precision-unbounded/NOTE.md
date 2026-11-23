Violates WF-062 (`spec/IR-SPEC.md` ch. 4.7), the unbounded arm of the precision gate. A Vondst whose `vindloonbesluit` is a sum type `Toegekend(getal met 2 decimalen) | Afgewezen` over the domain
`vindloonbesluit`. The rule
"vast vindloonbesluit" writes `Construct Toegekend(gewicht gedeeld door gewicht)`: a quotient by anything but a
literal has no finite precision bound (`spec/RUNTIME-SEMANTICS.md` ch. 2.3), and the payload declares 2
decimals, so the argument is written through an explicit rounding, as under WF-023.
