Violates WF-127 (`spec/IR-SPEC.md` ch. 4.7), the guard arm on a cell. A Vondst whose `gewicht` is dimensioned by
`seizoen` (voorjaar, najaar) and a sum type `waarde(getal met 2 decimalen) | onbekend` over the domain `gewicht van een
Vondst`, the lowering of a RegelSpraak attribute with the marker onbekend (`spec/RUNTIME-SEMANTICS.md` ch. 9.5). The rule
"najaarsloon" reads `PayloadAccess` of the cell in najaar under a gevuld test of the access to the cell in voorjaar: the
two bases are cells of one attribute with other labels, so the test guards another read and the read in najaar stands
under none. The golden `jutterswet-afwezigheidsmarkers-cel` writes the same value under the test of the cell in najaar.
