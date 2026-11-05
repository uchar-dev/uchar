Violates WF-062 (`spec/IR-SPEC.md` ch. 4.7), the unbounded arm of the precision gate. A Vondst whose `taxatie` is a sum type `waarde(getal met 2 decimalen) | onbekend` over the domain `taxatie van
een Vondst`, the lowering of a RegelSpraak attribute with the marker onbekend (`spec/RUNTIME-SEMANTICS.md`
ch. 9.5). The rule
"vaste taxatie" writes `Construct waarde(gewicht gedeeld door gewicht)`: a quotient by anything but a
literal has no finite precision bound (`spec/RUNTIME-SEMANTICS.md` ch. 2.3), and the payload declares 2
decimals, so the argument is written through an explicit rounding, as under WF-023.
