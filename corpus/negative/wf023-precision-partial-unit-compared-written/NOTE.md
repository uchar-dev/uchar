Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4 on a temporal total that can count a part of a period of its grid (ch. 2.3, ch. 3.6): a clip by a comparison of a timeline a rule writes, into a whole target without a rounding.

Both timelines are monthly, as in `corpus/golden/jutterswet-peilwacht`, but here a rule writes the peilstand: 20 on the days a Jutter is wachtplichtig, a characteristic that holds on periods of days (ch. 3.10). The peilstand can then change inside a month, and so can the clip "gedurende de tijd dat zijn peilstand groter is dan 10". The verifier does not trace where a rule cuts the timeline it writes, so a comparison of a written timeline counts as a clip that can split a period.

    WF-023.precision-unbounded FAIL /rules/rules/1/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with RGS-370, `PrecisionGateTest`):

    Objecttype de Jutter (mv: Jutters) (bezield)
        is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;
        de peilstand Numeriek (geheel getal) voor elke maand;
        de wachtvergoeding Numeriek (geheel getal) voor elke maand;
        de peilsom Numeriek (geheel getal);

    Regel peilstand van een jutter
        geldig altijd
            De peilstand van een Jutter moet gesteld worden op 20 indien hij wachtplichtig is.

    Regel peilsom van een jutter
        geldig altijd
            De peilsom van een Jutter moet gesteld worden op het totaal van zijn wachtvergoeding gedurende de tijd dat zijn peilstand groter is dan 10.

Measured: `verifier: PASS` on `a956aaa45`, where the bound of the total was the bound of its operand.
