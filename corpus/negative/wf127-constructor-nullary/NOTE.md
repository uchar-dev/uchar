Violates WF-127 (`spec/IR-SPEC.md` ch. 4.7), the nullary arm. A Vondst whose `taxatie` is a sum type `waarde(getal met 2 decimalen) | onbekend | niet van toepassing` over the domain `taxatie van een Vondst`, the lowering of a RegelSpraak attribute with both absence markers (`spec/RUNTIME-SEMANTICS.md` ch. 9.5).
The access names `onbekend`, a nullary constructor: it carries no payload to read. A marker is tested with an equality
against `Construct onbekend` (ch. 9.3), not read.
