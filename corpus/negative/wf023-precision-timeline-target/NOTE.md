Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4 on a rule whose target has a timeline: a quotient by a timeline into a whole timeline.

The `loonverhouding` of a Jutter, a whole number on a day grid, is its strandloon gedeeld door its stormtoeslag, both timelines. The value of each period is the quotient of the two on that period (ch. 3.4), and a quotient by an attribute has no finite bound (ch. 2.3), so the write needs a rounding or the unbounded form. Only the pro-rata write of ch. 3.7 stands outside the gate. The other five rules are those of `corpus/golden/jutterswet-rekenen-per-dag`, whose `loonverhouding` is of the unbounded form.

    WF-023.precision-unbounded FAIL /rules/rules/2/versions/0/value

The RegelSpraak the document denotes (the frontend emits it without a diagnostic: its precision gate leaves a target with a timeline out):

    Objecttype de Jutter (mv: Jutters) (bezield)
        het strandloon Numeriek (geheel getal) voor elke dag;
        de stormtoeslag Numeriek (geheel getal) voor elke dag;
        het loonproduct Numeriek (geheel getal) voor elke dag;
        het nettoloon Numeriek (geheel getal) voor elke dag;
        de loonverhouding Numeriek (geheel getal) voor elke dag;
        de afgekapte loonverhouding Numeriek (getal met 5 decimalen) voor elke dag;
        de strandheffing Numeriek (getal met 2 decimalen) voor elke dag;
        het jaarproduct Numeriek (geheel getal);

    Regel loonproduct van een jutter
        geldig altijd
            Het loonproduct van een Jutter moet berekend worden als zijn strandloon maal zijn stormtoeslag.

    Regel nettoloon van een jutter
        geldig altijd
            Het nettoloon van een Jutter moet berekend worden als zijn strandloon verminderd met zijn stormtoeslag.

    Regel loonverhouding van een jutter
        geldig altijd
            De loonverhouding van een Jutter moet berekend worden als zijn strandloon gedeeld door zijn stormtoeslag.

    Regel afgekapte loonverhouding van een jutter
        geldig altijd
            De afgekapte loonverhouding van een Jutter moet berekend worden als zijn strandloon gedeeld door (ABS) zijn stormtoeslag.

    Regel strandheffing van een jutter
        geldig altijd
            De strandheffing van een Jutter moet berekend worden als 10% van zijn strandloon.

    Regel jaarproduct van een jutter
        geldig altijd
            Het jaarproduct van een Jutter moet gesteld worden op het totaal van zijn loonproduct.

Measured: `verifier: PASS` on `53ab929d5`, before the gate reached a target with a timeline.
