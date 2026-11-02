Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: an unrounded power.
`zijn ribbe tot de macht 2` is written into `zijvlakte`, of 2 decimals, without the rounding the RegelSpraak grammar demands after a power; the document is the IR of `corpus/golden/jutterswet-kistinhoud` with that rounding taken out. A power has no finite bound, as in ALEF.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Wrakkist (mv: Wrakkisten)
        de ribbe Numeriek (getal met 1 decimalen);
        de zijvlakte Numeriek (getal met 2 decimalen);

    Regel zijvlakte
        geldig altijd
            De zijvlakte van een Wrakkist moet berekend worden als zijn ribbe tot de macht 2 naar beneden afgerond op 2 decimalen.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
