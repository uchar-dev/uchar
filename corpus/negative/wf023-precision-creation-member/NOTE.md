Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient in the member initialisation of an object creation.
A Melding creates a Strandrecht with `uitkering gelijk aan zijn omvang gedeeld door zijn aantal vinders`, a quotient by an attribute, written into an uitkering of 2 decimals.

    WF-023.precision-unbounded FAIL /rules/objectCreations/0/versions/0/initialisations/0/value

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-338 or RGS-370):

    Objecttype het Strandrecht (mv: Strandrechten)
        de uitkering Numeriek (niet-negatief getal met 2 decimalen);

    Objecttype de Melding (mv: Meldingen)
        de omvang Numeriek (niet-negatief getal met 2 decimalen);
        het aantal vinders Numeriek (geheel getal);

    Feittype strandmelding
        de melding Melding
        het gemeld strandrecht Strandrecht
    Eén melding meldt één gemeld strandrecht

    Regel maak een strandrecht bij elke melding
        geldig altijd
            Een Melding heeft een gemeld strandrecht met uitkering gelijk aan zijn omvang gedeeld door zijn aantal vinders.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
