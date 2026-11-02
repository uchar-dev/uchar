Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a sum of two precisions.
`zijn taxatie plus zijn toeslag`, of 2 and 1 decimals, is written into `waarde`, of 1 decimal. A sum carries the higher of the two, 2.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-338 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de taxatie Numeriek (getal met 2 decimalen);
        de toeslag Numeriek (getal met 1 decimalen);
        de waarde Numeriek (getal met 1 decimalen);

    Regel waarde
        geldig altijd
            De waarde van een Vondst moet berekend worden als zijn taxatie plus zijn toeslag.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
