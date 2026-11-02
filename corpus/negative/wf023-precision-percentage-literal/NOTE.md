Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a literal percentage of a whole value.
`21% van zijn opbrengst` is written into `heffing`, of 1 decimal. The literal is the factor 21/100, which has two decimals, so a whole opbrengst gives a bound of 2. (`20% van` the same opbrengst would give 1: 20/100 = 1/5.)

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de opbrengst Numeriek (geheel getal);
        de heffing Numeriek (getal met 1 decimalen);

    Regel heffing
        geldig altijd
            De heffing van een Vondst moet berekend worden als 21% van zijn opbrengst.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
