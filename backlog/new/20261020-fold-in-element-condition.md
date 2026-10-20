# fold-in-element-condition: een vouw met filter over een relatie van het regelonderwerp, in de elementvoorwaarde van een andere vouw

*Grens gevonden door een regelsetbouwer, 2026-10-02 (brug K13 van de regelset herleving-WW v2, kernfix 3 na de eval),
nagemeten door de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a` en ref en clingo uit
`_build`. Luid: de vorm met filter geeft check exit 1; de vorm zonder filter een WARNING en een fout bij de evaluatie;
geen fout antwoord.*

## Aanleiding

De norm: de eerste maand zonder overschrijding, niet eerder dan de maand van het laatste einde van een bepaalde soort
gronden. De directe zin is een vouw (de laatste einddag van de zware klachten van de schuit) in het filter van een andere
vouw (de eerste maand van de ladingen van de schuit). De regelset herleving-WW zet daarom het laatste einde in de
voorwaarden van de regel en vouwt over de maanden vanaf de beëindigingsdatum (brug K13); wat die brug niet ziet, staat
in haar open punt 42. Verwant: open punt 5 van `done/20260926-frontend-breuk-en-navigatiekeuze.md`, "een `sourcePath`
vanaf een ander anker dan het onderwerp spelt de frontend niet (featurevraag)".

Getuige (kop):

    Eenheidsysteem Tijd
        de dag dg
        de maand mnd

    Domein Klachtsoort is van het type Enumeratie
        'Zwaar'
        'Licht'

    Objecttype de Schuit (mv: Schuiten)
        de eerste schone maand vanaf-de-eindmaand Datum in dagen;

    Objecttype de Lading in-een-maand (mv: Ladingen in-een-maand)
        de maand Datum in dagen;
        het gewicht Numeriek (geheel getal);
        de overlading kenmerk (bezittelijk);

    Objecttype de Klacht (mv: Klachten)
        de einddag Datum in dagen;
        de soort Klachtsoort;

    Feittype lading in-een-maand
        de schuit Schuit
        de lading (mv: ladingen) Lading in-een-maand
    Eén schuit heeft meerdere ladingen

    Feittype schuitklacht
        de beklaagde schuit Schuit
        de klacht (mv: klachten) Klacht
    Eén beklaagde schuit heeft meerdere klachten

    Regel overlading
        geldig altijd
            Een Lading in-een-maand heeft de overlading
            indien zijn gewicht groter is dan 100.

    R1  Regel eerste schone maand vanaf de eindmaand
            geldig altijd
                De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
                - hij heeft geen overlading
                - zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'.
        nu: check exit 1, "de 'die/dat'-deelselectie 'aan alle volgende voorwaarden voldoen: - hij heeft geen overlading
        - zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de Schuit' wordt niet omgezet
        en zou stil wegvallen: …", met WARNINGs dat 'zijn soort' en ''Zwaar'' onopgelost zijn: de binnenste "die …
        voldoen:" komt niet bij "alle klachten".
    R2  de binnenste vouw tussen haakjes, of als variabele: check exit 1, "vouw '…' zonder bereik (… een ander anker dan
        het onderwerp …)" (de variabele: kaart `rule-variable-in-subselection`).
    R3  R1 zonder filter: check exit 0 met "WARNING: tussenstap in een vouwbron: 'alle klachten van de Schuit' bereikt de
        elementen via Schuit, maar een vouwbron draagt precies één relatie vanaf het regelonderwerp Lading in-een-maand
        (IR-SPEC ch. 4.3: één elementType plus één sourceRole) — de tussenstap zou spoorloos wegvallen, dus de aggregatie
        blijft onopgelost (§13.4.1/§13.4.17)"; scenario "ERROR ComputeFault: … Unresolved reached in evaluation: this
        rule fails observably".
    R4  R3 via de rol van het element ("alle klachten van de schuit van de Lading in-een-maand"): check exit 0 met
        "WARNING: keten zonder sourcePath in een vouwbron: … dus de aggregatie blijft onopgelost (§13.4.17)", terwijl het
        IR de vouw met sourcePath [lading in-een-maand.schuit, schuitklacht.klacht] draagt en Drools en ref de
        verwachte uitkomst geven; clingo: kaart `clingo-nested-aggregate-in-fold-filter`. Met filter: als R1.
    R5  de brug: "De laatste einddag van een Schuit moet berekend worden als de laatste van de einddag van alle klachten
        van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'", en R1 leest "de laatste
        einddag van de Schuit": check exit 0; PASS in Drools, ref en clingo.

    Casussen: zware klachten met einddag 15-03-2026 en 10-02-2026, een lichte met 20-05-2026; ladingen februari (50),
    maart (50), april (150) → 01-03-2026. Eén zware klacht 15-03-2026; februari (50), maart (150), april (50) →
    01-04-2026.

## Doel

R1, de zin van de norm, geeft check exit 0 zonder WARNING en de uitkomsten van R5 (01-03-2026 en 01-04-2026) in ref ≡
clingo ≡ Drools. Daarvoor: (1) de "die … voldoen:" van de binnenste vouw hoort bij haar eigen verzameling; (2) een vouw over
een relatie van het regelonderwerp in een filter krijgt een bereik vanaf het regelonderwerp (via het pad van het element
naar het onderwerp, zoals R4 al krijgt, of via een `Let` op de regel; zie `rule-variable-in-subselection` voor de
afweging); (3) geen diagnostic zegt "blijft onopgelost" waar het IR de vouw gelowerd draagt (R4). Rood-eerst met R1, R3 en
R4; alle gates groen.
