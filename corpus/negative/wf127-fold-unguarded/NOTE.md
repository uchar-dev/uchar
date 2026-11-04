Violates WF-127 (`spec/IR-SPEC.md` ch. 4.7), the guard arm, in a fold over members. A Vondst whose `taxatie` is a sum type `waarde(getal met 2 decimalen) | onbekend | niet van toepassing` over the domain `taxatie van een Vondst`, the lowering of a RegelSpraak attribute with both absence markers (`spec/RUNTIME-SEMANTICS.md` ch. 9.5).
The sum over the vondsten of a Jutter reads the payload of each contributor's taxatie and carries no filter, so no
member is tested: an `onbekend` taxatie would be skipped as leeg and the sum would compute on as if the vondst had no
value. The golden `jutterswet-afwezigheidsmarkers` carries the filter "die een waarde hebben", a gevuld test of the
same access per member.
