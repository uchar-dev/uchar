Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a percentage of a value, neither a literal.
`het heffingspercentage van zijn taxatie` is written into `heffing`, of 2 decimals: a percentage of 1 decimal and a taxatie of 2. The percentage is divided by a hundred, so the bound is 1 + 2 + 2 = 5.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Parameter het heffingspercentage : Percentage (getal met 1 decimalen)

    Objecttype de Vondst (mv: Vondsten)
        de taxatie Numeriek (getal met 2 decimalen);
        de heffing Numeriek (getal met 2 decimalen);

    Regel heffing
        geldig altijd
            De heffing van een Vondst moet berekend worden als het heffingspercentage van zijn taxatie.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
