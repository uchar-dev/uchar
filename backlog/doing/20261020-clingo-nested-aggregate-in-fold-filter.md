# clingo-nested-aggregate-in-fold-filter: een maximum in het filter van een vouw geeft clingo een aggregaat binnen een aggregaat, en clingo ontleedt het programma niet

*Nevenvondst van de hermeting van brug K13 (regelset herleving-WW v2) door de hoofdsessie, 2026-10-20, op uchar
`60979bf2` via de distributie `f5adbd1a` (`bin/regelspraak cases`), met `clingo_runner` uit `_build` en clingo 5.8.2.
Luid (clingo stopt met een ontleedfout), geen fout antwoord, maar geen benoemde weigering met een code.*

## Aanleiding

    Eenheidsysteem Tijd
        de dag dg
        de maand mnd

    Objecttype de Schuit (mv: Schuiten)
        de afmeerdag Datum in dagen;
        de eerste maand na de afmeerdag Datum in dagen;

    Objecttype de Lading in-een-maand (mv: Ladingen in-een-maand)
        de maand Datum in dagen;

    Feittype lading in-een-maand
        de schuit Schuit
        de lading (mv: ladingen) Lading in-een-maand
    Eén schuit heeft meerdere ladingen

    Regel eerste maand na de afmeerdag
        geldig altijd
            De eerste maand na de afmeerdag van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan de volgende voorwaarde voldoen: zijn maand is later dan de laatste van (de afmeerdag van de Schuit min 1 mnd) en (de afmeerdag van de Schuit min 2 mnd).

    Casus: afmeerdag 15-04-2026; ladingen maart en april → 01-04-2026.

    nu: check exit 0; Drools PASS; ref "oracle: PASS (1 expectations)"; clingo:
        "uchar-clingo….lp:23:204-208: error: syntax error, unexpected #max" (en regel 24), "*** ERROR: (clingo): parsing
        failed". Regel 23 van het programma:
        val(S,"eerste maand na de afmeerdag",M1) :- isa(S,"schuit"), extp73(S), M1 = #min { G2 : role(S,"ladingen",X3),
        isa(X3,"lading in-een-maand"), val(X3,"maand",G2), val(X3,"maand",A4), fl71_def(X3),
        F72 = #max { XF72 : fl71(X3,XF72) }, A4 > F72 }.

De vertaling zet het maximum van het filter (`#max`) in de voorwaarde van een element van de buitenste vouw (`#min`);
clingo aanvaardt geen aggregaat binnen een aggregaat. Dezelfde ontleedfout geeft een vouw via de rol van het element in
het filter ("de laatste van de einddag van alle klachten van de schuit van de Lading in-een-maand"; kaart
`fold-in-element-condition`, R4). Buiten een vouw speelt "de laatste van (…) en (…)" in clingo.

## Doel

De vertaling zet het binnenste aggregaat in een hulppredicaat per element, zodat de getuige in clingo 01-04-2026 geeft,
gelijk aan ref en Drools; tot dan een benoemde TRANSLATION FAULT met een code in plaats van de ontleedfout van clingo.
Een golden met de getuige (ref ≡ clingo ≡ Drools); alle gates groen.
