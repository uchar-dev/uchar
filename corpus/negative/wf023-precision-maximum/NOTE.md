Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a maximum over operands of two precisions.
`de maximale waarde van zijn taxatie en zijn opbrengst` is written into the whole `hoogste bod`. Either operand can be the result, so the bound is the higher of the two, the 2 decimals of the taxatie.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-338 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de taxatie Numeriek (getal met 2 decimalen);
        de opbrengst Numeriek (geheel getal);
        het hoogste bod Numeriek (geheel getal);

    Regel hoogste bod
        geldig altijd
            Het hoogste bod van een Vondst moet berekend worden als de maximale waarde van zijn taxatie en zijn opbrengst.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
