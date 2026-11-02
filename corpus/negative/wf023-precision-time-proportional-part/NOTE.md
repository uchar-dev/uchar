Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: an unrounded time-proportional part.
`het tijdsevenredig deel per maand van zijn onderhoudstarief` is written into the monthly `onderhoudsbijdrage`, of 2 decimals. The part scales by days over the days of the month (ch. 2.8), a quotient, so it has no finite bound.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-338 or RGS-370):

    Objecttype de Bakenmeester (mv: Bakenmeesters) (bezield)
        het onderhoudstarief Numeriek (getal met 2 decimalen) voor elk jaar;
        de onderhoudsbijdrage Numeriek (getal met 2 decimalen) voor elke maand;
        is dienstdoend kenmerk (bijvoeglijk) voor elke maand;

    Regel onderhoudsbijdrage van een bakenmeester
        geldig altijd
            De onderhoudsbijdrage van een Bakenmeester moet gesteld worden op het tijdsevenredig deel per maand van zijn onderhoudstarief gedurende de tijd dat hij dienstdoend is.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
