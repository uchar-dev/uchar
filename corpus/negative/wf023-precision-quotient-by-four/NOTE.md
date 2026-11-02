Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient by a literal that adds two decimals.
`zijn opbrengst gedeeld door 4` is written into `kwart`, of 1 decimal. The reciprocal 1/4 has two decimals, so a whole opbrengst gives a bound of 2.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de opbrengst Numeriek (geheel getal);
        het kwart Numeriek (getal met 1 decimalen);

    Regel kwart
        geldig altijd
            Het kwart van een Vondst moet berekend worden als zijn opbrengst gedeeld door 4.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
