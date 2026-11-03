Violates WF-009 (`spec/IR-SPEC.md` ch. 2.5), the calendar arm of RUNTIME-SEMANTICS ch. 3.6: a unit of the month family declared as a multiple of a unit of the day family.

The unit system `Tijd` declares `jr = 365 dg`. A year has no fixed number of days, so between the day family (dag, week) and the month family (maand, kwartaal, jaar) no factor is fixed; the temporal total integrates a rate across the two by the actual length of the calendar unit (the anchor `corpus/golden/anchor/jutterswet-verzuimrente-dagen`, the same document without the factor: 500 €/jr over 54 days of 2028 is 73 47/61 €, where this factor would give 73 71/73 €). Within a family a factor stays (`jr = 12 mnd`, `wk = 7 dg`).

    WF-009.calendar-family-factor FAIL /model/unitSystems/1/units/1

The RegelSpraak the document denotes (the frontend emits it without a diagnostic):

    Eenheidsysteem Munt
        de euro (mv: euros) EUR €

    Eenheidsysteem Tijd
        de dag (mv: dagen) dg
        het jaar (mv: jaren) jr = 365 dg

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
            De verzuimrente van een Afdracht moet berekend worden als (het totaal van zijn dagrente gedurende de tijd dat hij rentedragend is) rekenkundig afgerond op 2 decimalen.

Measured: `verifier: PASS` on `53ab929d5`, before the arm.
