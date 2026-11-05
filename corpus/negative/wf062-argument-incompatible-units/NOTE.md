Violates WF-062 (`spec/IR-SPEC.md` ch. 4.7), the typing arm. A Vondst whose `taxatie` is a sum type `waarde(getal met 2 decimalen) | onbekend` over the domain `taxatie van
een Vondst`, the lowering of a RegelSpraak attribute with the marker onbekend (`spec/RUNTIME-SEMANTICS.md`
ch. 9.5). The rule "vaste taxatie" writes
`Construct waarde(gewicht)`, and `gewicht` is declared in `kg` while the payload carries no unit: the
argument is written into the payload, and a value in `kg` is not assignable to a unitless number (WF-023's unit
clause), although the two datatypes are of one family and of the same decimals.
