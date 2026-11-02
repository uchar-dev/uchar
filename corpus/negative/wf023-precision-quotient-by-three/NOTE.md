Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient by a literal whose reciprocal does not terminate.
`zijn opbrengst gedeeld door 3` is written into `derde deel`, of 2 decimals. Divided by a literal, the bound is that of a product with its reciprocal, and 1/3 has no finite decimals, so the bound is unbounded.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Vondst (mv: Vondsten)
        de opbrengst Numeriek (geheel getal);
        het derde deel Numeriek (getal met 2 decimalen);

    Regel derde deel
        geldig altijd
            Het derde deel van een Vondst moet berekend worden als zijn opbrengst gedeeld door 3.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
