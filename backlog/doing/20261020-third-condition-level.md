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

## Voorlopige besluiten (2026-10-22, naar het Fable-advies)

Bron: `uchar-dev/research/bruggen-2026-10-22/fable-advies.md`, vraag 3 (B3.1–B3.3). De eigenaar kan de besluiten
omdraaien.

**Een stil fout antwoord hoort bij deze kaart** (gemeten in het advies, `proef/q3/`): een alle-lijst met een geneste
ten-minste-één-groep, elke bullet `-` op één kolom, geeft `diagnostics: []` en `Compound all (2) [leeftijd > 65,
Compound atLeast (3) [Touw, Glas, leeftijd < 80]]`. De laatste bullet, bedoeld als lid van de alle-lijst, staat stil in
de of-groep. De aanname in `RuleChecker` ±5021–5023 en ±5258 ("under ALLE/DE the merge is semantics-preserving") geldt
alleen als de geneste groep zelf een alle-groep is.

- **B3.1 Het teken wint.** De diepte van een bullet is het paar (tekendiepte, kolom), lexicografisch: eerst het
  teken, de kolom alleen tussen bullets met hetzelfde teken. In het corpus, `toka/` en v2 staan `-` en `..` op één
  kolom (`jutterswet-keurcriteria` r. 17 en 51), dus de kolom kan geen primaire sleutel zijn.
- **B3.2 Eén punt per niveau.** `-` = 1, `..` = 2, `...` = 3, `....` = 4; `•` = 1, `••` = 2, `•••` = 3; `▪` als `•`. In
  de lexer `DOTDOT : '..' '.'*`, zodat `...` geen syntaxfout meer is. "Dieper" is strikt groter, dus `-`, `..`, `....`
  (de vorm van deze kaart) speelt ook. Dit wijkt af van het voorstel hierboven (`....` = 3, `...` een ERROR).
- **B3.3** Eén functie bouwt de boom uit de platte bulletlijst, voor voorwaarden, deelselecties en
  consistentiecriteria gelijk: een diepere bullet is kind van de vorige, die dan een groepskop moet zijn; gelijke
  diepte is een zuster; minder diep keert terug naar het open niveau met precies die diepte. Vier benoemde ERRORs
  (nieuwe RGS-codes): een groepskop zonder diepere bullet erachter (dus ook hetzelfde teken op dezelfde kolom na een
  groepskop, onder **elke** kwantor, ook onder alle: het stille geval hierboven); een diepere bullet zonder groepskop
  ervoor; een terugkeer naar een diepte die geen open niveau is; twee tekenfamilies (`-`/`..` naast `•`) in één
  samengestelde voorwaarde. `bulletsInconsistent`, `ambiguousNestedBullets`, `reNestColumns` en
  `RGS-092.counting-deep-bullet` vervallen.
- `SilentMarkerTest` r. ±274–284 en ±293–305 pinnen de oude zinnen; de Result noemt hoe ze veranderen.
- Rood eerst: R1, R2 (`•`-vorm en ingesprongen vorm), R3 (`...`) en de alle/of-zin hierboven als frontendtests; één
  golden op de Jutterswet-getuige van deze kaart (vier casussen) in gates 5, 6, 12 en 17. Als meting, zonder bestand in
  deze repository: of een lijst in de v2-regelset herleving-WW het stille geval draagt.
