Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient by an attribute.
`zijn opbrengst gedeeld door zijn aantal deelnemers` is written into the whole `aandeel`. A quotient by anything but a literal has no finite precision bound (RUNTIME-SEMANTICS ch. 2.3): 100/4 fits, 100/3 does not, and the two have one type.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de opbrengst Numeriek (geheel getal);
        het aantal deelnemers Numeriek (geheel getal);
        het aandeel Numeriek (geheel getal);

    Regel aandeel
        geldig altijd
            Het aandeel van een Vondst moet berekend worden als zijn opbrengst gedeeld door zijn aantal deelnemers.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
