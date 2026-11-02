Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: an unrounded time-proportional part.
`het tijdsevenredig deel per maand van zijn onderhoudstarief` is written into `onderhoudsbijdrage`, of 2 decimals and without a timeline. The part scales by days over the days of the month (ch. 2.8), a quotient, so it has no finite bound. A target with a timeline stands outside the gate (RUNTIME-SEMANTICS ch. 2.4): no executor carries a rounding around the pro-rata write of ch. 3.7, the one computed write such a target takes; `corpus/golden/anchor/jutterswet-borgdeel` writes the part unrounded into its monthly timeline, verifier-clean.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Bakenmeester (mv: Bakenmeesters) (bezield)
        het onderhoudstarief Numeriek (getal met 2 decimalen) voor elk jaar;
        de onderhoudsbijdrage Numeriek (getal met 2 decimalen);
        is dienstdoend kenmerk (bijvoeglijk) voor elke maand;

    Regel onderhoudsbijdrage van een bakenmeester
        geldig altijd
            De onderhoudsbijdrage van een Bakenmeester moet gesteld worden op het tijdsevenredig deel per maand van zijn onderhoudstarief gedurende de tijd dat hij dienstdoend is.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
