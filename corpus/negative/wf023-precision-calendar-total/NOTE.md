Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4 on a temporal total the calendar integrates (ch. 2.3, ch. 3.6): a rate per year over a day grid totalled into a target of 2 decimals without a rounding.

The anchor `corpus/golden/anchor/jutterswet-verzuimrente-dagen` without its rounding. A day weighs 1/365 or 1/366 of its year, so the total has no finite bound (139 13/73 € in the anchor's case 1); the anchor rounds it to the cent.

    WF-023.precision-unbounded FAIL /rules/rules/1/versions/0/value

The RegelSpraak the document denotes (the frontend emits it without a diagnostic: its bound of the total does not know the calendar):

    Eenheidsysteem Munt
        de euro (mv: euros) EUR €

    Eenheidsysteem Tijd
        de dag (mv: dagen) dg
        het jaar (mv: jaren) jr

    Parameter de verzuimrentevoet : Numeriek (getal met 2 decimalen) met eenheid 1/jr voor elke maand;

    Objecttype de Afdracht (mv: Afdrachten)
        het afdrachtbedrag Numeriek (geheel getal) met eenheid €;
        de vervaldatum Datum in dagen;
        de betaaldatum Datum in dagen;
        is rentedragend kenmerk (bijvoeglijk) voor elke dag;
        de dagrente Numeriek (getal met 2 decimalen) met eenheid €/jr voor elke dag;
        de verzuimrente Numeriek (getal met 2 decimalen) met eenheid €;

    Regel dagrente
        geldig altijd
            De dagrente van een Afdracht moet berekend worden als de verzuimrentevoet maal zijn afdrachtbedrag.

    Regel verzuimrente
        geldig altijd
            De verzuimrente van een Afdracht moet berekend worden als het totaal van zijn dagrente gedurende de tijd dat hij rentedragend is.

Measured: `verifier: PASS` on `53ab929d5`, where the bound of the total kept the bound of its operand.
