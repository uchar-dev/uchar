Violates WF-127 (`spec/IR-SPEC.md` ch. 4.7), the guard arm. A Vondst whose `taxatie` is a sum type `waarde(getal met 2 decimalen) | onbekend | niet van toepassing` over the domain `taxatie van een Vondst`, the lowering of a RegelSpraak attribute with both absence markers (`spec/RUNTIME-SEMANTICS.md` ch. 9.5).
The rule "vindloon naar taxatie" writes `PayloadAccess taxatie waarde` and its version carries no condition, so no
gevuld test of that access holds before the read: on a Vondst whose taxatie is `onbekend` the access is leeg and the
rule would decide nothing here, but inside `plus` or `maal` the leeg would count as 0. The golden
`jutterswet-afwezigheidsmarkers` writes the same value under "indien zijn taxatie een waarde heeft".
