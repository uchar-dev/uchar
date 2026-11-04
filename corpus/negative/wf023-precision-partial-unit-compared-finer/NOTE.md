Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4 on a temporal total that can count a part of a period of its grid (ch. 2.3, ch. 3.6): a clip by a comparison of a timeline whose grid does not fit the grid of the total, into a whole target without a rounding.

The wachtvergoeding has a timeline for every month, the peilstand one for every day. The clip "gedurende de tijd dat zijn peilstand groter is dan 10" cuts where the peilstand changes, which can be any day, so it can cut a month, and a part of a month counts its days over the days of the month. A comparison clip cuts on the walls of the grid of the total only where the compared timeline lies on a calendar grid whose walls are walls of that grid, as in `corpus/golden/jutterswet-peilwacht`, where both timelines are monthly.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with RGS-370, `PrecisionGateTest`):

    Objecttype de Jutter (mv: Jutters) (bezield)
        de peilstand Numeriek (geheel getal) voor elke dag;
        de wachtvergoeding Numeriek (geheel getal) voor elke maand;
        de peilsom Numeriek (geheel getal);

    Regel peilsom van een jutter
        geldig altijd
            De peilsom van een Jutter moet gesteld worden op het totaal van zijn wachtvergoeding gedurende de tijd dat zijn peilstand groter is dan 10.

Measured: `verifier: PASS` on `a956aaa45`, where the bound of the total was the bound of its operand.
