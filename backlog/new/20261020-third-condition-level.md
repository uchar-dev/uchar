# third-condition-level: een derde niveau van geneste voorwaarden onder een tellende kwantor heeft geen schrijfwijze

*Grens gevonden door een regelsetbouwer, 2026-10-02 (cluster G14 van de regelset herleving-WW v2, clusterdocument
G12/G14, V1), nagemeten door de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a` en ref en
clingo uit `_build`. Luid: de WARNING RGS-092 en een fout bij de evaluatie in Drools, ref en clingo; geen fout antwoord.
Dat `check` daarbij exit 0 geeft, is de toolchain-kaart `onopgeloste-naam-weigeren`.*

## Aanleiding

De grammatica leest een bullet als `( DASH | DOTDOT | BULLET )+`, en een geneste groep neemt elke volgende bullet op; de
diepte staat niet in de ontleding. `RuleChecker.buildCompound` hernest op kolom als een lijst bullets op verschillende
kolommen draagt, markeert een lijst met verschillende tekens of kolommen (RGS-092), en markeert onder een tellende
kwantor ("ten minste één", "ten hoogste", "precies") daarnaast elke lijst waarin een geneste groep zelf een geneste groep
draagt (`ambiguousNestedBullets`), ook als elk niveau een eigen teken of een eigen kolom heeft. Zo heeft een derde niveau
onder een of-lijst geen schrijfwijze. RegelSpraak v2.3.0 §13.4.14.7 schrijft de diepte als het aantal bullets
(`("•")+`). De regelset herleving-WW zet de herleefbaarheid na een intrekking daarom als `Afwijking van` naast de
basisregel, in plaats van als tak in de of-lijst.

Getuige:

    Domein Vondstsoort is van het type Enumeratie
        'Touw'
        'Glas'
        'Kurk'
        'Hout'
        'Blik'

    Objecttype de Jutter (mv: Jutters) (bezield)
        de leeftijd Numeriek (geheel getal);
        de soort Vondstsoort;
        de vrijstelling kenmerk (bezittelijk);

    Regel vrijstelling
        geldig altijd
            Een Jutter heeft de vrijstelling
            indien hij aan ten minste één van de volgende voorwaarden voldoet:
            - zijn soort is gelijk aan 'Blik'
            - hij voldoet aan alle volgende voorwaarden:
            .. zijn leeftijd is groter dan 65
            .. hij voldoet aan ten minste één van de volgende voorwaarden:
            .... zijn soort is gelijk aan 'Touw'
            .... zijn soort is gelijk aan 'Glas'
            .... zijn soort is gelijk aan 'Kurk'.

    Casussen: 30 jaar en 'Blik' → vrijstelling; 70 en 'Touw' → vrijstelling; 30 en 'Touw' → geen; 70 en 'Hout' → geen.

    R1  zoals hierboven (`-`, `..`, `....`).
        nu: check exit 0 met "WARNING (line 17): onopgelost: de bullets van de samengestelde voorwaarde staan op
        verschillende kolommen of dragen verschillende tekens, dus welke bullet onder welke hoort ligt niet vast
        (§13.4.14.6) — '…' blijft als marker op de wire staan"; scenario 4× "ERROR ComputeFault: fout in de regel
        "vrijstelling": Unresolved condition reached in evaluation"; ref "EVALUATION FAULT: EF-019: …"; clingo
        "TRANSLATION FAULT: EF-019: …".
    R2  met `•`, `••`, `•••`, of met het derde niveau vier kolommen ingesprongen: als R1.
    R3  met `...`: check exit 1, "syntaxfout: extraneous input '.' expecting {…}".
    R4  drie niveaus onder "alle": "indien hij aan alle volgende voorwaarden voldoet: - zijn leeftijd is groter dan 17
        - hij voldoet aan ten minste één van de volgende voorwaarden: .. zijn soort is gelijk aan 'Blik' .. hij voldoet
        aan alle volgende voorwaarden: .... zijn leeftijd is groter dan 65 .... zijn soort is gelijk aan 'Touw'":
        check exit 0 zonder WARNING, 4/4 PASS in Drools, ref en clingo.
    R5  de omweg: basisregel "indien zijn soort gelijk is aan 'Blik'" en een `Afwijking van` met twee niveaus: check
        exit 0, 4/4 PASS in Drools, ref en clingo.
    Ook gemeten: een geneste groep die niet de laatste bullet is, speelt als zij is ingesprongen (kolomhernesting), en
    geeft RGS-092 als zij niet is ingesprongen.

## Doel

Een derde niveau (en dieper) heeft een schrijfwijze die de frontend onder elke kwantor eenduidig leest. Voorstel, door de
eigenaar te kiezen: de diepte volgt uit het teken (`-`/`•` niveau 1, `..`/`••` niveau 2, `....`/`•••` niveau 3, zoals
§13.4.14.7 de bullets telt) of uit een diepere kolom, en de terugval van `ambiguousNestedBullets` geldt alleen nog als een
geneste lijst hetzelfde teken op dezelfde kolom draagt als de lijst erboven. `...` krijgt een lezing of een benoemde
ERROR in plaats van de ANTLR-syntaxfout. Rood-eerst met R1–R3: check exit 0 zonder WARNING en de vier casussen in ref ≡
clingo ≡ Drools; R4, R5 en de goldens ongewijzigd; DIVERGENCES noemt de schrijfwijze; alle gates groen.
