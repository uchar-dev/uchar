Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4 on a temporal total that can count a part of a period of its grid (ch. 2.3, ch. 3.6): a clip by a comparison of a timeline on a grid of its own start point, into a whole target without a rounding.

The jaarpremie has a timeline for every calendar year, the peilstand one for every storm year from 1 October. Both grids have periods of a year, but a wall of the storm year falls inside a calendar year, so the clip "gedurende de tijd dat zijn peilstand groter is dan 10" can cut a calendar year at 1 October, and a part of a year counts its days over the days of the year. Only a compared timeline on a calendar grid that fits the calendar grid of the total cuts on its walls.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with RGS-370, `PrecisionGateTest`):

    Tijdlijn het stormjaar heeft tijdvakken van een jaar startend op dag 1 van maand 10;

    Objecttype de Jutter (mv: Jutters) (bezield)
        de peilstand Numeriek (geheel getal) voor elke stormjaar;
        de jaarpremie Numeriek (geheel getal) voor elk jaar;
        de premiesom Numeriek (geheel getal);

    Regel premiesom van een jutter
        geldig altijd
            De premiesom van een Jutter moet gesteld worden op het totaal van zijn jaarpremie gedurende de tijd dat zijn peilstand groter is dan 10.

Measured: `verifier: PASS` on `a956aaa45`, where the bound of the total was the bound of its operand.
