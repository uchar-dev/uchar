Violates WF-127 (`spec/IR-SPEC.md` ch. 4.7), the guard arm, on the order of an `all`. A Vondst whose `taxatie` is a sum type `waarde(getal met 2 decimalen) | onbekend | niet van toepassing` over the domain `taxatie van een Vondst`, the lowering of a RegelSpraak attribute with both absence markers (`spec/RUNTIME-SEMANTICS.md` ch. 9.5).
The characteristic rule "kostbare vondst" reads the payload in its first member, a comparison with 1000,00, and tests
it in its second. The members of an `all` are read in order and the first that fails stops the rest
(`spec/RUNTIME-SEMANTICS.md` ch. 5.10), so the comparison is read before the test: on an `onbekend` taxatie the
access is leeg and `leeg > 1000,00` is onwaar without the author having tested for it. The golden
`jutterswet-afwezigheidsmarkers` writes the two members the other way round.
