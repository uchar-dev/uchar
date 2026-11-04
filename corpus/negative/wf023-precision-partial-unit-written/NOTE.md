Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4 on a temporal total that can count a part of a period of its grid (ch. 2.3, ch. 3.6): the total of a timeline a rule writes, into a whole target without a rounding.

The wachttoelage, a timeline for every month, is 30 on the days a Jutter is wachtplichtig (ch. 3.10): a characteristic holds on periods of days, so the rule can write a part of a month, and the total counts that part by its days, 30 × 15/30 for the second half of April. The verifier does not trace where a rule cuts the timeline it writes: every total of a written timeline over a grid of periods longer than a day counts as one that can split a period. The suite fixture `ref/test/fixtures/partial-unit/exact` writes the same total into the unbounded form.

    WF-023.precision-unbounded FAIL /rules/rules/1/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with RGS-370, `PrecisionGateTest`):

    Objecttype de Jutter (mv: Jutters) (bezield)
        is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;
        de wachttoelage Numeriek (geheel getal) voor elke maand;
        de wachtsom Numeriek (geheel getal);

    Regel wachttoelage van een jutter
        geldig altijd
            De wachttoelage van een Jutter moet gesteld worden op 30 indien hij wachtplichtig is.

    Regel wachtsom van een jutter
        geldig altijd
            De wachtsom van een Jutter moet gesteld worden op het totaal van zijn wachttoelage.

Measured: `verifier: PASS` on `a956aaa45`, where the bound of the total was the bound of its operand.
