Violates WF-062 (`spec/IR-SPEC.md` ch. 4.7), the precision arm. A Vondst whose `taxatie` is a sum type `waarde(getal met 2 decimalen) | onbekend` over the domain `taxatie van
een Vondst`, the lowering of a RegelSpraak attribute with the marker onbekend (`spec/RUNTIME-SEMANTICS.md`
ch. 9.5). The rule "vaste taxatie" writes
`Construct waarde(gewicht)`, and `gewicht` is declared with 3 decimals: the argument is written into the
payload, which declares 2, so its precision bound (`spec/RUNTIME-SEMANTICS.md` ch. 2.3) exceeds the payload's, as
the same read written into an attribute of 2 decimals exceeds it under WF-023. The RegelSpraak frontend refuses
the source with RGS-372.
