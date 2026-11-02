Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: an allocation whose pot has more decimals than its recipients.
The opbrengst of a Berging, of 2 decimals, is divided in equal shares over the whole `bergingsaandeel` of its bergers, without a rounding. The bound of a share is the rounding of the allocation, and without one the declaration of its pot, 2 (the ALEF form); the division by the number of recipients is not typed, so a share that does not fit with a pot that does stays the evaluation fault EF-006 (`corpus/red/jutterswet-verdeling-past-niet`).

    WF-023.share-precision-exceeds FAIL /rules/allocations/0/versions/0

The RegelSpraak the document denotes (the frontend refuses it with the same verdict, RGS-372 or RGS-370):

    Objecttype de Berging (mv: Bergingen)
        de opbrengst Numeriek (getal met 2 decimalen);
        de onverdeelde rest Numeriek (getal met 2 decimalen);

    Objecttype de Berger (mv: Bergers) (bezield)
        het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal);

    Feittype bergingsdeelname
        de berging Berging
        de berger (mv: bergers) Berger
    Eén berging heeft meerdere bergers

    Regel verdeel de opbrengst van een berging
        geldig altijd
            De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
            - in gelijke delen.
            Als onverdeelde rest blijft de onverdeelde rest van de berging over.

Measured: `verifier: PASS` on `e284bab9a`, before the gate.
