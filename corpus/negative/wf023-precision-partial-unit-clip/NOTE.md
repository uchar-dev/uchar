Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4 on a temporal total that can count a part of a period of its grid (ch. 2.3, ch. 3.6): a clip by a characteristic, totalled into a whole target without a rounding.

The total of a stormtoelage on the grid of the storm year from 1 October, clipped to the weeks a Jutter is wachtplichtig. A characteristic holds on periods of days whatever its granularity, so the clip can cut a storm year, and a part of a storm year counts its days over the days of that storm year: 100 over 2024-09-28 to 2024-10-03 is 100 × 3/366 + 100 × 2/365 = 6090/4453, which no number of decimals holds. `corpus/golden/jutterswet-vast-startpunt` writes the same total through a rounding to 2 decimals; without it, its case 1 faults with EF-006 at the write.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with RGS-370, `PrecisionGateTest`):

    Tijdlijn het stormjaar heeft tijdvakken van een jaar startend op dag 1 van maand 10;

    Objecttype de Jutter (mv: Jutters) (bezield)
        is wachtplichtig kenmerk (bijvoeglijk) voor elke week startend op 30-9-2024;
        de stormtoelage Numeriek (geheel getal) voor elke stormjaar;
        de toelagesom Numeriek (geheel getal);

    Regel toelagesom van een jutter
        geldig altijd
            De toelagesom van een Jutter moet gesteld worden op het totaal van zijn stormtoelage gedurende de tijd dat hij wachtplichtig is.

Measured: `verifier: PASS` on `a956aaa45`, where the bound of the total was the bound of its operand.
