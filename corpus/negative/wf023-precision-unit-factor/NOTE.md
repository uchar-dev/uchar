Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a write that restates its value into a coarser unit.
`zijn sleeplengte`, whole metres, is written into the whole `sleepafstand`, in kilometres. Restating into km multiplies by 1/1000, which adds three decimals (RUNTIME-SEMANTICS ch. 2.6, rule 6): 1500 m is 3/2 km. The other direction takes them away: a whole number of kilometres is a whole number of metres.

    WF-023.precision-exceeds FAIL /rules/rules/0/versions/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Eenheidsysteem afstand
        de meter m
        de kilometer km = 1000 m

    Objecttype de Vondst (mv: Vondsten)
        de sleeplengte Numeriek (geheel getal) met eenheid m;
        de sleepafstand Numeriek (geheel getal) met eenheid km;

    Regel sleepafstand
        geldig altijd
            De sleepafstand van een Vondst moet gesteld worden op zijn sleeplengte.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
