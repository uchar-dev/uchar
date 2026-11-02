Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a product of two decimal values.
`zijn taxatie maal zijn factor`, two values of 2 decimals, is written into `waarde`, of 2 decimals. A product carries the sum of the decimals of its operands, 4.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de taxatie Numeriek (getal met 2 decimalen);
        de factor Numeriek (getal met 2 decimalen);
        de waarde Numeriek (getal met 2 decimalen);

    Regel waarde
        geldig altijd
            De waarde van een Vondst moet berekend worden als zijn taxatie maal zijn factor.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
