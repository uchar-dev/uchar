Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a read of the unbounded form into a bounded target.
`zijn factor`, declared `Numeriek (getal)`, the unbounded form, is written into `bedrag`, of 2 decimals. The unbounded form has no bound; the value is written through a rounding.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-338 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de factor Numeriek (getal);
        het bedrag Numeriek (getal met 2 decimalen);

    Regel bedrag
        geldig altijd
            Het bedrag van een Vondst moet gesteld worden op zijn factor.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
