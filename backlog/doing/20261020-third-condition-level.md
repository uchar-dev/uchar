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

## Voorlopig besluit van de bouwer (2026-10-22): een bullet na een gebullette deelselectie

*Gemarkeerd voorlopig besluit; het advies beslist dit niet. De eigenaar kan het omdraaien.*

Een gebullette deelselectie die een bullet van een buitenste lijst afsluit ("- zijn controle is later dan de laatste
van de einddag van alle klachten van de Schuit die aan alle volgende voorwaarden voldoen: .. … .. …"), krijgt van de
grammatica elke bullet erna, ook een volgende bullet van de buitenste lijst ("- zijn dagen is groter dan 10"). Die staat
dan in de ontleding binnen de expressie van de buitenste bullet. Besluit: haar bullets staan dieper dan haar bullet
(anders RGS-353 op die bullet), en een bullet die niet dieper staat dan haar bullet, is niet van haar en wordt
geweigerd: RGS-355 met de uitleg en de twee uitwegen (de bullet met de deelselectie als laatste van haar lijst, of de
deelselectie tussen haakjes; beide gemeten, beide spelen). Niet gekozen: de bullet teruggeven aan de buitenste lijst.
Dat vraagt het opsplitsen van een ontlede expressie: de typecontrole (`conditions`) loopt de ontleding af met het
element als onderwerp (`inFilterScope`), en de bronfragmenten van alle knopen tussen de buitenste bullet en de
deelselectie lopen tot het einde van de lijst. Tot deze kaart werd zo'n bullet stil een lid van het filter, gelezen op
het element (vondst 1 in de Result).

## Result (2026-10-22)

Tak `card/third-condition-level` op main `5d8a06de`. Geraakt: `frontends/regelspraak/**` (de lexer, de opmerking bij
`bullet`, `RuleChecker`, het register en beide catalogi, `grammar/DIVERGENCES.md`, een nieuwe testklasse en vier
aangepaste), de golden `corpus/golden/jutterswet-vrijstelling-drie-niveaus/`, `EXPECTED_PLAYED` in
`scripts/check-drools.sh` en deze kaart. Niet geraakt: `spec/`, `ref/`, `backends/`, `toka/`, de andere gate-scripts.

- `6d234b4c` de kaart naar `doing/`.
- `ab0fe23f` de diepte van een bullet: lexer, boombouwer, vier ERRORs, DIVERGENCES ("§13.4.14.6–7 The depth of a
  bullet"), `BulletDepthTest`, de aangepaste tests.
- `4f19ef95` de golden en `EXPECTED_PLAYED` 268 → 269.

**Draagt v2 het stille geval? Nee** (meting 2, zonder bestand in deze repository). De v2-regelset herleving-WW
(fundament en herleving-ww, werkbank `c53dfd9`) geeft met deze tak exit 0, `diagnostics: []` en een IR dat bytegelijk is
aan dat van main; de acht scenariodocumenten geven bytegelijke casebestanden. Een regelscan over dezelfde bronnen vond 58
voorwaardenlijsten, 15 met een groepskop, en geen die onder de nieuwe regel anders leest of een van de vier ERRORs geeft.
In uchar lezen het corpus (131 rgs-goldens, gate 10), `toka/` (gate 11) en de 33 scenario-goldens met 114 casebestanden
(gate 16) bytegelijk; de rgs-bronnen onder `backends/drools/src/test/resources` geven een bytegelijk IR. De enige lijst
met een hernesting op kolom (`toka-45`, `-` en ingesprongen `..`) leest onder "teken wint" zoals de oude
kolomhernesting: de TOKA-casusdump, die haar draagt, komt bytegelijk terug (gate 11).

**De regel** (B3.1–B3.3). `DOTDOT : '..' '.'*`. De diepte van een bullet is (aantal tekens, kolom), eerst het teken; de
kolom telt alleen tussen bullets met evenveel tekens. Eén boombouwer (`RuleChecker.bulletTree`, gevoed door
`flattenBullets`) bouwt de boom voor voorwaarden, deelselecties en consistentiecriteria; een geneste groep krijgt als
bronfragment de tekst van haar kop tot haar laatste bullet in de boom (gelijk aan de oude tekst waar boom en ontleding
samenvallen). R1 (`-`, `..`, `....`), R2 (`•`, `••`, `•••` en de ingesprongen vorm) en R3 (`...`) geven dezelfde boom,
`atLeast 1 [Blik, all [leeftijd > 65, atLeast 1 [Touw, Glas, Kurk]]]`, zonder diagnostic; alle vier spelen de vier
casussen in ref, clingo en beide Drools-takken. R4 en R5 geven een bytegelijk IR.

**De vier ERRORs**, elk op de regel van de bullet die zij noemt (zinnen zoals de frontend ze nu schrijft, ingekort):

- RGS-353 `group-header-without-deeper-bullet`, de alle/of-zin van `proef/q3/`: "de bullet '- hij voldoet aan ten minste
  één van de volgende voorwaarden:' opent een groep, maar de bullet erna, '- zijn soort is gelijk aan 'Touw'', staat niet
  dieper: een bullet onder een groepskop staat dieper, met meer tekens (…) of, bij hetzelfde teken, op een diepere kolom
  (…)". Onder elke kwantor; ook een kop zonder bullet erna, en een deelselectie in een bullet waarvan de eerste bullet
  niet dieper staat dan die bullet.
- RGS-354 `deeper-bullet-without-group-header`, `- zijn leeftijd is groter dan 65` en dan `.. zijn soort is gelijk aan
  'Touw'.`: "de bullet '.. zijn soort is gelijk aan 'Touw'.' staat dieper dan de bullet ervoor, '- zijn leeftijd is
  groter dan 65', maar die opent geen groep (…)".
- RGS-355 `bullet-returns-to-no-open-level`, `-` kop, twee `....`, dan `..`: "de bullet '.. zijn soort is gelijk aan
  'Blik'.' staat minder diep dan de bullet ervoor, maar niet even diep als een open niveau van de lijst (…)". Met een
  tweede deel na een gebullette deelselectie (het voorlopig besluit hierboven): "… staat niet dieper dan de bullet '-
  zijn controle is later dan … die aan alle volgende voorwaarden voldoen:', waarvan de deelselectie de bullets ervoor
  draagt, dus hij hoort bij de lijst van die bullet; (…) Zet de bullet met de deelselectie als laatste in zijn lijst, of
  zet de deelselectie tussen haakjes".
- RGS-356 `two-bullet-sign-families`, `-` en `•` in één lijst: "de lijst mengt de bullettekens '-' en '•': de diepte
  telt de tekens van één familie (…)".

Na zo'n ERROR is de voorwaarde een marker zonder reden (de ERROR is de diagnose); een consistentieregel stopt erna, zonder
RGS-180 ernaast. Vervallen: `reNestParts`, `reNestColumns`, `columnsInconsistentDeep`, `buildFromNodes`,
`bulletsInconsistent`, `ambiguousNestedBullets`, de terugval `RGS-092.counting-deep-bullet` in de deelselectie,
`buildNested` en `buildCriterion`, en de catalogusdelen `RGS-092.bullets-inconsistent`, `RGS-092.renested-bullet` en
`RGS-092.counting-deep-bullet`. Geen registerregel verviel (RGS-092 heeft andere delen); het register telt 356 codes.

**Hoe de tests veranderden.**

- `SilentMarkerTest`: `inconsistent_bullets_report_the_unclear_nesting` (`-` en daarna `..` zonder groepskop; pinde de
  marker met de WARNING "staan op verschillende kolommen of dragen verschillende tekens") heet nu
  `a_deeper_bullet_without_a_group_header_is_an_error` en pint RGS-354. `a_counting_quantifier_over_deeper_nesting_is_reported`
  (een deelselectie onder 'ten minste 2', alles `..` op één kolom, een groepskop gevolgd door `..`; pinde de marker "een
  tellende kwantor over een deelselectie") heet nu `a_group_header_without_a_deeper_bullet_is_an_error` en pint
  RGS-353. Beide eisen precies één ERROR en geen sweep-melding. `a_missing_expression_and_an_unbindable_primary_are_reported`
  bereikte het foutherstel via de syntaxfout van `...`; dat is nu niveau 3, dus de invoer schrijft `.. .`, dezelfde
  tokens als `...` vóór deze kaart, met dezelfde meldingen (gemeten op beide lexers).
- `WindowQuantifierTest.a_window_under_no_within_all_refuses` schreef `•` ingesprongen onder een `-`-kop, twee
  tekenfamilies die de kolom tot nu hernestte; de toets schrijft nu `..` en houdt haar onderwerp (WF-112). De gemengde
  vorm is nu RGS-356 (`BulletDepthTest.two_sign_families_are_an_error`).
- `MarkerReasonPinTest` en `MarkerReportTest`: drie nieuwe plekken met reden null (`buildCompound`, `buildCriteria`,
  `compoundElementFilterBare`), ingedeeld als ERROR-route zonder document.
- Nieuw `BulletDepthTest` (16 tests). Rood eerst gemeten: R1, R2 (beide vormen), R3 en de alle/of-zin faalden vóór de
  bouw (R1 en beide R2-vormen een marker met de WARNING, R3 de syntaxfout, de alle/of-zin zonder diagnostic).

**De golden** `jutterswet-vrijstelling-drie-niveaus`: de getuige van deze kaart in de vorm R1, vier casussen uit
`scenario.rgs` (gate 16): 30 jaar en Blik; 70 en Touw (met 70 en Kurk); 30 en Touw (met 70 en Touw); 70 en Hout (met 70
en Glas). De tweede Jutter geeft elke casus een vrijstelling, zodat geen ervan een uitvoerder die niets doet overleeft
(gate 7; een lijst van overlevers mocht niet groeien). ref, clingo en beide Drools-takken: 4 van 4; `EXPECTED_PLAYED`
268 → 269.

**Meting 5** (zonder bestand in deze repository): een kopie van v2 in de werkmap met in `hww-02` de herleefbaarheid na
een intrekking als vijftiende tak van de of-lijst van "indicatie kan herleefd worden art. 21 lid 1" (`- hij voldoet aan
alle volgende voorwaarden:`, `..` de aanvraag vóór de bezwaartermijn, `..` een of-groep met `...` de codes 14, 15 en 17)
en zonder de `Afwijking van`: `--ir` exit 0, `diagnostics: []`, 41 resolved regels (de regel nu als gewone regel in
plaats van een defaultsgroep). De 160 casebestanden van de acht scenariodocumenten zijn bytegelijk aan die van de
ongewijzigde v2; ref: 160 van 160 PASS op beide IR's, met gelijke uitvoer per casus; Drools (exec-model): PLAYED 160 op
beide. clingo vertaalt v2 niet (TRANSLATION FAULT op een eenheiddragende vermenigvuldiging), op beide gelijk. Afwijking
22 kan dus vervallen; dat is stap 2 van het advies, voor de werkbank.

**Gates.** Tijdens de bouw: de frontendsuite groen (1275 tests), gates 10, 11 en 16 PASS. De zeventien gates op de
eindstand van de code (`4f19ef95`, main `5d8a06de` onbewogen): `EXIT 0`, failed=0 (samenvatting
`werk/bruggen/third-level/gates-1.summary.txt`); gate 12 speelt 269 goldens op beide takken, gate 17 vergelijkt 269
goldens zonder afwijking.

**Vondsten.**

1. Een bullet van een buitenste lijst na een gebullette deelselectie werd stil een lid van haar filter, gelezen op het
   element: in een alle-lijst met `- zijn controle is later dan de laatste van de einddag van alle klachten van de Schuit
   die aan alle volgende voorwaarden voldoen: .. … .. …` en daarna `- zijn dagen is groter dan 10` las main de laatste
   bullet als de dagen van de Klacht, met `diagnostics: []` (gemeten). Een tweede stil fout antwoord van dezelfde
   oorzaak; nu RGS-355 met de uitwegen (voorlopig besluit hierboven, getuige
   `BulletDepthTest.a_bullet_after_a_bulleted_subselection_is_refused_with_the_ways_out`).
2. `DecisionTableDesugar` weigert een tabelrij met twee of-cellen (RGS-300), omdat "two nested OR-lists would silently
   swallow each other's bullets". Onder de dieptediscipline is dat niet meer zo (de desugaring schrijft `•` en `••`); de
   weigering blijft staan, buiten deze kaart.
3. `EXPECTED_PLAYED`: de parallelle bouwer in `wt/cycle-across-instances` telt mogelijk ook een golden bij; de tweede
   landing herrekent bij de rebase.
4. Een lijst waarvan de eerste bullet op de regel van de openingszin staat ("… voldoet: - a", dan "- b" op de volgende
   regel) heeft bullets op verschillende kolommen met hetzelfde teken: vroeger een marker, nu RGS-355. Geen bron schrijft
   zo (gates 10, 11, 16 en v2).
5. De sectie in DIVERGENCES heet "§13.4.14.6–7 The depth of a bullet", Engels zoals de rest van dat bestand.

**Niet gedaan.** Een bullet na een gebullette deelselectie teruggeven aan de buitenste lijst (voorlopig besluit
hierboven). Geen CONSTRUCTS-rij: de wire (`Compound`) en WF-041 veranderen niet. Geen PLAN-tekst nodig.
