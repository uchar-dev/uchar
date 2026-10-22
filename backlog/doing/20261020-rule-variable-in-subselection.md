# rule-variable-in-subselection: een variabele van de regel, gelezen in een subselectie, bindt haar genavigeerde lezing vanaf het element

*Gevonden door een regelsetbouwer, 2026-10-02 (de kernfixes na de eval van de regelset herleving-WW v2), nagemeten door
de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a` en ref en clingo uit `_build`. Luid
(`check` exit 1), geen fout antwoord.*

## Aanleiding

Dezelfde lezing speelt inline in de subselectie van een vouw, en weigert als variabele van de regel (`Daarbij geldt:`,
§13.4.2.10) die in dezelfde subselectie wordt gelezen. De frontend lost de variabele op waar zij gelezen wordt, met het
element als onderwerp. Een lezing van één stap vanaf het regelonderwerp ("de gewenste dag van de Schuit") krijgt zo het
pad `[lading in-een-maand.schuit]` en speelt; een lezing van twee stappen niet, en een vouw over een relatie van het
regelonderwerp ook niet. Inline krijgt dezelfde lezing van twee stappen het pad `[lading in-een-maand.schuit,
schuitverzoek.verzoek]`. Buiten de subselectie gelezen speelt de variabele. In de regelset herleving-WW verhindert dit de
directe vorm van brug K13 als variabele.

Getuige:

    Eenheidsysteem Tijd
        de dag dg
        de maand mnd

    Objecttype de Schuit (mv: Schuiten)
        de eerste schone maand Datum in dagen;

    Objecttype het Verzoek (mv: Verzoeken)
        de gewenste dag Datum in dagen;

    Objecttype de Lading in-een-maand (mv: Ladingen in-een-maand)
        de maand Datum in dagen;
        het gewicht Numeriek (geheel getal);

    Feittype lading in-een-maand
        de schuit Schuit
        de lading (mv: ladingen) Lading in-een-maand
    Eén schuit heeft meerdere ladingen

    Feittype schuitverzoek
        de verzochte schuit Schuit
        het verzoek Verzoek
    Eén verzochte schuit heeft één verzoek

    Regel eerste schone maand
        geldig altijd
            De eerste schone maand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
            - zijn gewicht is kleiner dan 100
            - zijn maand plus 1 mnd is later dan de peildag.
            Daarbij geldt:
                de peildag is de gewenste dag van het verzoek van de Schuit.

    Casus: gewenste dag 15-03-2026; ladingen februari (50), maart (150), april (50) → 01-04-2026.

    R1  zoals hierboven.
        nu: check exit 1, "ERROR (line 31): genavigeerde lezing 'de gewenste dag van het verzoek van de Schuit' zonder
        binding (de geschreven keten is vanaf Lading in-een-maand niet stap voor stap te binden): an attribute reference
        without a path is the subject's own read, so its subjectType is the subject type 'Lading in-een-maand' —
        'Verzoek' names another type, which only a navigation reaches".
    R2  inline: "- zijn maand plus 1 mnd is later dan de gewenste dag van het verzoek van de Schuit".
        nu: check exit 0; scenario PASS (01-04-2026); ref en clingo PASS.
    R3  de variabele buiten de subselectie ("De eerste schone maand van een Schuit moet gesteld worden op de peildag
        indien de peildag gevuld is"): check exit 0.
    R4  een vouw met filter als variabele: "de laatste einddag is de laatste van de einddag van alle klachten van de
        Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'", gelezen in de subselectie van
        een vouw over de ladingen: check exit 1, "vouw '…' zonder bereik (de geschreven bron '…' loopt via 'de Schuit die
        aan de volgende voorwaarde voldoen: …', een ander anker dan het onderwerp, en een bereik vanaf zo'n anker schrijft
        deze frontend niet)". Buiten de subselectie gelezen: PASS.

## Doel

Een variabele van de regel betekent in een subselectie hetzelfde als daarbuiten: haar expressie hoort bij het onderwerp
van de regel. R1 geeft check exit 0 en de uitkomst van R2 in ref ≡ clingo ≡ Drools; R4 de uitkomst van de vorm met een
eigen lid (kaart `fold-in-element-condition`). Twee wegen, door de eigenaar te kiezen: de frontend schrijft het pad van het
element naar het regelonderwerp vóór de keten van de variabele, zoals hij nu doet voor een lezing van één stap en voor
dezelfde keten inline; of hij schrijft de variabele als `Let` op de regel en leest de binder in het filter. Bij de eerste
weg is het pad van het element terug alleen het regelonderwerp als de vouw over een relatie van dat onderwerp loopt; de
tweede weg raakt open punt 6 van `done/20261008-tms-expressievormen-en-rekendatum.md` ("Een binder van een omsluitende
`Let` in het filter of de matrix van een vouw draagt de tak niet") en `new/20261009-clingo-let-binder-tussen-onderwerpen.md`.
Rood-eerst met R1 en R4; alle gates groen.

## Voorlopig besluit (2026-10-22, naar het Fable-advies)

De eerste weg: de frontend schrijft het pad van het element naar het regelonderwerp vóór de keten van de variabele, als
padprefix op de bij declaratie gebouwde `Expr` (`uchar-dev/research/bruggen-2026-10-22/fable-advies.md`, vraag 2a,
B2.1). Deze kaart wordt samen met `fold-in-element-condition` gebouwd; de besluiten staan daar.
