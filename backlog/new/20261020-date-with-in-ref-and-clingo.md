# date-with-in-ref-and-clingo: ref en clingo voeren de datumconstructie "de datum met jaar: …, maand: … en dag: …" niet uit

*Gemeten door een regelsetbouwer, 2026-10-02 (de kernfix na de eval-pilot van de regelset herleving-WW v2, buiten de
poorten), nagemeten door de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a` en ref en clingo
uit `_build`. Vastgelegd als open punt 5 van `done/20261008-tms-expressievormen-en-rekendatum.md`, zonder kaart. Luid;
geen fout antwoord.*

## Aanleiding

RUNTIME-SEMANTICS ch. 5.11 noemt bij `dateWith`/`dateTimeWith`: "ref declines the form; the order is stated, not yet
exercised". De Drools-tak rekent de vorm, zonder onafhankelijke bevestiging. De regelset herleving-WW gebruikt haar in
een uitzondering (de gewenste maand van een herlevingsverzoek), en kan sinds 2026-10-20 ook de subselectie van een vouw
met haar schrijven; ref en clingo kunnen die regels niet nalopen.

    Objecttype de Schuit (mv: Schuiten)
        de gewenste dag Datum in dagen;
        de gewenste maand Datum in dagen;

    Regel gewenste maand
        geldig altijd
            De gewenste maand van een Schuit moet gesteld worden op de datum met jaar: het jaar uit zijn gewenste dag, maand: de maand uit zijn gewenste dag en dag: 1.

    Casus: gewenste dag 15-03-2026 → gewenste maand 01-03-2026.

    nu: check exit 0; Drools PASS; ref "EVALUATION FAULT: REF-007: function kind not implemented in this card";
        clingo "TRANSLATION FAULT: CLINGO-008: expression kind not translated (no corpus case exercises it)".

## Doel

ref en clingo voeren `dateWith` (en `dateTimeWith`) uit volgens RUNTIME-SEMANTICS ch. 2–3; waar de spec voor een datum
die niet bestaat (dag 31 in een maand van 30 dagen) nog niets zegt, eerst die regel. De getuige geeft 01-03-2026 in ref ≡
clingo ≡ Drools, als golden; de regel "ref declines the form" in ch. 5.11 vervalt; alle gates groen.
