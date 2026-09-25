# ww-herleving-inkomen — het WW-herleving realisme-anker (inkomenspad)

Het eerste WW-realisme-anker: het inkomens-eindigingspad
art. 20 lid 1 onderdeel c → art. 21 lid 1 eerste volzin (geen termijn). Eén
genest kenmerk-defaults-doel `recht op uitkering` — grondtoekenning (art. 15)
→ do-not-attribute op de inkomensgrens (art. 20-1-c) → herleving bij
vervallen omstandigheid (art. 21-1) → do-not-attribute op de blokkade
(art. 21-2-a) — plus drie gewone scalaire rekenregels. Elke geneste default
draagt naast haar eigen voorwaarde die van haar drager. Tien cases: vier
die elk op een andere default beslissen (case1–case4), vier invoeren waarop
een boom zonder dragervoorwaarden tegen de wettekst in zou beslissen (case5,
case6, case8, case9), één case met het doelkenmerk al in de invoer (case7),
en één die het eigen conjunct `nieuw maandloon > inkomensgrens` van de
blokkade los pint (case10). Handgeschreven, met absolute verwachtingen; alle
getallen en datums zijn eigen casusfeiten.

## Wetteksten (verbatim)

Alle citaten uit de officiële BWB-toestand-XML: **Werkloosheidswet,
BWB-identificatie `BWBR0004045`, toestand `2026-07-01`** (rechtenvrij,
art. 11 Auteurswet). Alleen wettekst; geen handboek-, uitvoerings- of
beleidsmateriaal.

**Art. 15 (de poortzin — grondtoekenning):**

> Met inachtneming van de artikelen 16 tot en met 21 en de daarop
> berustende bepalingen heeft de werknemer die werkloos is recht op
> uitkering.

**Art. 16 lid 1 (werkloos — kern):**

> Werkloos wordt de werknemer die:
> a. in een kalenderweek ten minste vijf arbeidsuren minder heeft dan
> zijn gemiddeld aantal arbeidsuren per kalenderweek of een aantal
> arbeidsuren heeft dat ten hoogste gelijk is aan de helft van zijn
> gemiddeld aantal arbeidsuren per kalenderweek; en
> b. beschikbaar is om arbeid te aanvaarden.

**Art. 20 lid 1, onderdeel c (eindiging — inkomensgrond):**

> Het recht op uitkering eindigt:
> c. met ingang van de eerste dag van de kalendermaand waarin de
> werknemer niet meer werkloos is omdat hij inkomen geniet dat, na
> vermenigvuldiging met de factor C / D, bedoeld in artikel 47, eerste
> lid, onderdelen a en b, meer dan 87,5% van het maandloon bedraagt;

**Art. 21 lid 1, eerste volzin (herleving van rechtswege — a/c-eindiging):**

> Indien het recht op uitkering op grond van artikel 20, eerste lid,
> onderdeel a of c, is geëindigd en vervolgens de omstandigheid die tot
> dat eindigen heeft geleid heeft opgehouden te bestaan, herleeft het
> recht op uitkering met inachtneming van het tweede lid, de in artikel
> 8 en het derde lid genoemde termijnen, het vierde lid en de op grond
> van het vijfde lid gestelde regels.

**Art. 21 lid 2 (blokkades):**

> Het recht herleeft niet indien:
> a. een nieuw recht op uitkering ingevolge dit hoofdstuk ontstaat,
> waarvan het maandloon meer dan 87,5% bedraagt van het maandloon van de
> eerdere uitkering; of
> b. een recht op uitkering op grond van de Wet werk en inkomen naar
> arbeidsvermogen ontstaat.

**Art. 43 lid 1 (verschuiving van de maxdatum na herleving):**

> Telkens nadat het recht op uitkering na eindiging van dat recht is
> herleefd op grond van artikel 21, eindigt de uitkering zoveel later
> dan de in artikel 42, eerste, tweede en derde lid, bedoelde periode
> als de periode tussen de eindiging en herleving van het recht op
> uitkering heeft geduurd.

## Het model (`ir.json`)

Eén objecttype `Werknemer`, alles scalair:

Invoerattributen (scalaire feiten): `maandloon`, `maandinkomen` (het
inkomen in de eindigingsmaand), `nieuw maandloon` (het maandloon van een
eventueel nieuw recht), `eindigingsdatum`, `vervaldatum` (de dag waarop de
omstandigheid vervalt / de herlevingsdatum), `maxdatum` (de oorspronkelijke
maxdatum). Invoerkenmerken: `werkloos`, `omstandigheid vervallen`, `nieuw
recht ontstaan`.

`werkloos` is de grond van art. 15: de werknemer was werkloos toen het recht
op uitkering ontstond. Het kenmerk staat daarom ook aan in de cases waarin
het recht daarna op de inkomensgrond eindigt (case1, case3, case4), al
beschrijft art. 20 lid 1 onderdeel c dan een werknemer die "niet meer
werkloos" is. Zonder `werkloos` is er geen recht ontstaan, en dus ook geen
recht dat kan eindigen of herleven.

Afgeleide attributen: `inkomensgrens`, `verstreken periode` (in hele dagen),
`nieuwe maxdatum`. Doel-kenmerk van de defaults: `recht op uitkering`.

Het doelkenmerk is bezittelijk (`kind: possessive` op zijn `Characteristic`;
tot L1 `kenmerkSoorten`, B5), de drie invoerkenmerken zijn bijvoeglijk. De
soort stuurt alleen de spelling in een RegelSpraak-bron:
`heeft (geen) recht op uitkering` in plaats van
`is (niet) recht op uitkering`. Ze staat zo omdat een RegelSpraak-tweeling van
dit anker die spelling schrijft en byte-identiek op dit anker moet uitkomen.
De uitvoering hangt er niet van af: met `adjectival` en met `possessive`
geven ref, clingo en de Drools-exectak dezelfde uitkomsten op alle tien cases,
krijgt clingo hetzelfde ASP-programma en krijgt de Drools-bean hetzelfde veld.

### De defaults (het genest kenmerk-doel `recht op uitkering`)

De rose tree van art. 15 → 20-1-c → 21-1 → 21-2-a, als één defaults-bundel
met `Default.exceptions`-nesting (elke uitzondering wordt eerst beslist en
verdringt haar drager wanneer zij beslist). De voorwaarde van elke geneste
default is één `Compound` met kwantor `all`: eerst de conjuncten van
haar drager in dezelfde volgorde, dan die van haar eigen voorwaarde, plat,
zonder geneste `all`:

```
Default grant  (besluit waar,  als: werkloos)                                  -- art. 15
  exceptie -> Default einde     (besluit onwaar, als: werkloos
                                 EN maandinkomen > inkomensgrens)              -- art. 20-1-c
    exceptie -> Default herleving (besluit waar,  als: werkloos
                                   EN maandinkomen > inkomensgrens
                                   EN omstandigheid vervallen)                 -- art. 21-1
      exceptie -> Default blokkade (besluit onwaar, als: werkloos
                                    EN maandinkomen > inkomensgrens
                                    EN omstandigheid vervallen
                                    EN nieuw recht ontstaan
                                    EN nieuw maandloon > inkomensgrens)        -- art. 21-2-a
```

De hoogste beslissende default wint; een default met een exceptie laat de
exceptie eerst beslissen en valt alleen terug op haar eigen besluit als de
exceptie niets beslist (RUNTIME-SEMANTICS ch. 7.9).

**De dragervoorwaarde.** Ch. 7.9 beslist de uitzonderingsgroepen van een
default vóór en los van diens eigen rechtvaardiging: een uitzondering die
beslist, levert het besluit van haar drager, ook waar de voorwaarde van die
drager niet geldt. Ch. 7.1 schrijft daarom voor dat een geneste laag een
zelfstandige rechtvaardiging krijgt door conjunctie met die van de
omhullende laag, en zo staat het hier op de wire. De wet veronderstelt die
insluiting op elk niveau: art. 20 lid 1 laat een bestaand recht eindigen
("Het recht op uitkering eindigt"), art. 21 lid 1 noemt zijn drager
("Indien het recht op uitkering op grond van artikel 20, eerste lid,
onderdeel a of c, is geëindigd"), en art. 21 lid 2 beperkt alleen een
herleving ("Het recht herleeft niet indien"). De nesting blijft staan: zij
legt de voorrang vast en volgt de opbouw van de wet.

Deze boom heeft één pad, dus beslist de diepste default waarvan de
voorwaarde geldt (ch. 7.9); door de conjuncties geldt die voorwaarde alleen
als ook die van al haar dragers geldt. Schrijf W = `werkloos`, T = `maandinkomen > inkomensgrens`, V = `omstandigheid
vervallen` en N = `nieuw recht ontstaan` én `nieuw maandloon >
inkomensgrens`. Zonder het doelkenmerk in de invoer geldt dan:

```
recht op uitkering aanwezig  ⇔  W ∧ (¬T ∨ (V ∧ ¬N))
```

Een boom waarin elke geneste default alleen haar eigen voorwaarde draagt
(einde: T; herleving: V; blokkade: N), beslist op vier van de zestien
combinaties van W, T, V en N anders dan de wet. Elk van die vier is een case:

| Invoer | Eigen voorwaarde alleen | Met dragervoorwaarde (de wet) | Case |
|---|---|---|---|
| W, ¬T, ¬V, N | afwezig — de blokkade beslist `onwaar` | aanwezig — art. 15 | case5 |
| ¬W, ¬T, V, ¬N | aanwezig — de herleving beslist `waar` | afwezig — niets beslist | case6 |
| ¬W, T, V, ¬N | aanwezig — de herleving beslist `waar` | afwezig — niets beslist | case8 |
| W, ¬T, V, N | afwezig — de blokkade beslist `onwaar` | aanwezig — art. 15 | case9 |

case8 is ook de enige combinatie waarop een boom zonder het conjunct
`werkloos` (einde: T; herleving: T ∧ V; blokkade: T ∧ V ∧ N) van de wet
afwijkt: daar zou de herleving `waar` beslissen voor een werknemer bij wie
nooit een recht is ontstaan.

**WF-050** — precies één defaults-bundel, één schrijver, één doel: het
kenmerk `recht op uitkering` (exact één van kenmerk/attribuut). Geen enkele
gewone regel of objectcreatie schrijft dat kenmerk; de drie rekenregels
schrijven elk hun eigen attribuut, één keer.

**WF-051** — de defaults lezen nooit hun eigen doel. De default-condities lezen
`werkloos`, `maandinkomen`, `inkomensgrens`, `omstandigheid vervallen`,
`nieuw recht ontstaan` en `nieuw maandloon` — geen daarvan is `recht op
uitkering`; de conjuncties lezen dezelfde zes leden opnieuw en voegen er
geen toe. `inkomensgrens` is een *apart afgeleid* attribuut dat de
default-justificaties voedt (de termijn/grens-toets als eigen afleiding), niet
het defaults-doel. De afhankelijkheidsgraaf blijft acyclisch.

**WF-075** — elke `exceptions` draagt precies één exception group (één
`ExceptionGroup`), dus `unordered` is op elk niveau onwaar (twee of meer leden
zouden de ongeordend-markering eisen). Grant, einde en herleving zijn
form-a-defaults (eigen besluit *plus* geneste excepties); de blokkade is een
gewone default zonder excepties. Geen enkele default is contentloos.

### De gewone rekenregels (scalair, één schrijver elk)

1. `inkomensgrens` = het maandloon maal 0,8750, rekenkundig afgerond op
   2 decimalen (art. 20-1-c / 21-2-a: de grens van 87,5% van het maandloon).
2. `verstreken periode` = de tijdsduur van `eindigingsdatum` tot
   `vervaldatum` in hele dagen (art. 43 lid 1: "de periode tussen de
   eindiging en herleving").
3. `nieuwe maxdatum` = `maxdatum` plus `verstreken periode` (datum plus
   duur — de verschuiving van art. 43 lid 1).

Een ontbrekende `eindigingsdatum` óf `vervaldatum` laat `verstreken
periode` leeg (leeg-propagatie); `maxdatum` plus leeg is `maxdatum` (geen
verschuiving).

Vereenvoudiging bij art. 20-1-c: `maandinkomen` is hier het inkomen ná
vermenigvuldiging met de factor C / D uit art. 47 — de factor is in deze
casus 1, zodat de vergelijking direct tegen 87,5% van het maandloon loopt.

## De tien cases — handberekening

In alle tien cases: `maandloon` = 2400,00, dus
`inkomensgrens` = 0,875 × 2400,00 = **2100,00**.

| Case | W | T | V | N | Beslist | `recht op uitkering` |
|---|---|---|---|---|---|---|
| case1-herleving | ja | ja (2500,00) | ja | nee | herleving (art. 21-1) | aanwezig |
| case2-geen-eindiging | ja | nee (1800,00) | nee | nee | grant (art. 15) | aanwezig |
| case3-blokkade | ja | ja (2500,00) | ja | ja (2300,00) | blokkade (art. 21-2-a) | afwezig |
| case4-einde | ja | ja (2500,00) | nee | nee | einde (art. 20-1-c) | afwezig |
| case5-geen-eindiging-wel-nieuw-recht | ja | nee (1800,00) | nee | ja (2300,00) | grant (art. 15) | aanwezig |
| case6-niet-werkloos-wel-vervallen | nee | nee (1800,00) | ja | nee | niets | afwezig |
| case7-invoer-staat-tegen-blokkade | ja | ja (2500,00) | ja | ja (2300,00) | blokkade (art. 21-2-a); het doelkenmerk staat in de invoer | aanwezig |
| case8-niet-werkloos-boven-grens-vervallen | nee | ja (2500,00) | ja | nee | niets | afwezig |
| case9-geen-eindiging-wel-vervallen-en-nieuw-recht | ja | nee (1800,00) | ja | ja (2300,00) | grant (art. 15) | aanwezig |
| case10-nieuw-recht-onder-grens | ja | ja (2500,00) | ja | nee (2000,00) | herleving (art. 21-1) | aanwezig |

Tussen haakjes bij T het `maandinkomen`, bij N het `nieuw maandloon`; "nee"
zonder bedrag bij N betekent: geen nieuw recht, `nieuw maandloon` = 0,00.
case10 heeft wel een nieuw recht, maar het nieuwe maandloon (2000,00) ligt
niet boven de grens. Elke default is ergens de beslisser (grant: case2,
case5, case9; einde: case4; herleving: case1, case10; blokkade: case3,
case7); in case6 en case8 beslist niets.

Per case lopen de defaults in de volgorde van ch. 7.9, van diep naar ondiep
(blokkade, herleving, einde, grant): de eerste waarvan de voorwaarde geldt,
beslist, en haar besluit is ook dat van elke drager erboven.

### case1-herleving.json — de herlevingscase (beslist op de herleving-default)

Feiten: werkloos; `maandinkomen` = 2500,00; omstandigheid vervallen; geen
nieuw recht (`nieuw maandloon` = 0,00); `eindigingsdatum` = 2025-05-01;
`vervaldatum` = 2025-09-01; `maxdatum` = 2026-11-30.

- Blokkade art. 21-2-a: geen nieuw recht (en 0,00 ≤ 2100,00) → beslist niet.
- Herleving art. 21-1: werkloos, 2500,00 > 2100,00 (de inkomensgrond geldt,
  het recht was geëindigd) en de omstandigheid is vervallen → de
  herleving-default beslist `waar`; dat besluit is ook dat van einde en
  grant.
- Uitkomst: `recht op uitkering` **toegekend** (beslist op de
  herleving-default).
- `verstreken periode` = 2025-05-01 → 2025-09-01 =
  31 (mei) + 30 (juni) + 31 (juli) + 31 (augustus) = **123** dagen.
- `nieuwe maxdatum` = 2026-11-30 + 123 dagen:
  +31 → 2026-12-31 (rest 92); +31 → 2027-01-31 (rest 61);
  +28 → 2027-02-28 (rest 33); +31 → 2027-03-31 (rest 2); +2 →
  **2027-04-02**.

### case2-geen-eindiging.json — grens niet overschreden (beslist op de grant-default)

Feiten: werkloos; `maandinkomen` = 1800,00; geen nieuw recht
(`nieuw maandloon` = 0,00); `maxdatum` = 2026-11-30; geen `eindigingsdatum`,
geen `vervaldatum`.

- Blokkade, herleving en einde eisen alle drie `maandinkomen` >
  `inkomensgrens`; 1800,00 ≤ 2100,00, dus de inkomensgrond geldt niet en
  geen van de drie beslist.
- Basisgrond art. 15: werkloos → de grant-default beslist `waar`.
- Uitkomst: `recht op uitkering` **toegekend** (beslist op de grant-default);
  geen verschuiving.
- `verstreken periode` = leeg (geen `eindigingsdatum`) → **leeg**.
- `nieuwe maxdatum` = 2026-11-30 + leeg = **2026-11-30** (ongewijzigd).

### case3-blokkade.json — de blokkade (beslist op de blokkade-default)

Feiten: werkloos; `maandinkomen` = 2500,00; omstandigheid vervallen; nieuw
recht ontstaan met `nieuw maandloon` = 2300,00; `eindigingsdatum` =
2024-03-01; `vervaldatum` = 2024-06-01; `maxdatum` = 2025-10-15.

- Blokkade art. 21-2-a: werkloos, 2500,00 > 2100,00, de omstandigheid is
  vervallen, er is een nieuw recht ontstaan en 2300,00 > 2100,00 → de
  blokkade-default beslist `onwaar`; dat besluit gaat via herleving en
  einde naar de grant, en de herleving komt niet aan bod.
- Uitkomst: `recht op uitkering` **niet toegekend** (beslist op de
  blokkade-default). (Het nieuwe maandloon 2300,00 ligt onder het oude
  2400,00 maar boven de grens 2100,00 — de blokkade hangt aan de grens, niet
  aan het oude maandloon.)
- `verstreken periode` = 2024-03-01 → 2024-06-01 =
  31 (maart) + 30 (april) + 31 (mei) = **92** dagen.
- `nieuwe maxdatum` = 2025-10-15 + 92 dagen:
  +16 → 2025-10-31 (rest 76); +30 → 2025-11-30 (rest 46);
  +31 → 2025-12-31 (rest 15); +15 → **2026-01-15**. (De verschuiving wordt
  wel berekend maar is hier zonder gevolg: het recht herleeft niet.)

### case4-einde.json — de kale eindiging (beslist op de einde-default)

Feiten: werkloos; `maandinkomen` = 2500,00; de omstandigheid is NIET
vervallen (geen `vervaldatum`); geen nieuw recht (`nieuw maandloon` =
0,00); `eindigingsdatum` = 2026-02-01; `maxdatum` = 2026-08-31.

- Blokkade en herleving eisen een vervallen omstandigheid; die ontbreekt →
  geen van beide beslist.
- Einde art. 20-1-c: werkloos en 2500,00 > 2100,00 → de einde-default
  beslist `onwaar` en verdringt de grant.
- Uitkomst: `recht op uitkering` **niet toegekend** (beslist op de
  **einde**-default — onderscheiden van case3, waar de blokkade beslist).
- `verstreken periode` = tijdsduur(2026-02-01, leeg) → **leeg**
  (leeg-propagatie: de `vervaldatum` ontbreekt).
- `nieuwe maxdatum` = 2026-08-31 + leeg = **2026-08-31** (ongewijzigd).

### case5-geen-eindiging-wel-nieuw-recht.json — een nieuw recht zonder eindiging (beslist op de grant-default)

Feiten: die van case2 plus een nieuw recht: werkloos; `maandinkomen` =
1800,00; geen vervallen omstandigheid; nieuw recht ontstaan met `nieuw
maandloon` = 2300,00; `maxdatum` = 2026-11-30; geen `eindigingsdatum`, geen
`vervaldatum`.

- Blokkade art. 21-2-a: er is een nieuw recht en 2300,00 > 2100,00, maar de
  blokkade eist ook 1800,00 > 2100,00 en een vervallen omstandigheid →
  beslist niet.
- Herleving en einde: 1800,00 ≤ 2100,00 → beslissen niet.
- Basisgrond art. 15: werkloos → de grant-default beslist `waar`.
- Uitkomst: `recht op uitkering` **toegekend** (beslist op de
  grant-default). Het recht is nooit geëindigd, en art. 21 lid 2 beperkt
  alleen een herleving.
- `verstreken periode` = leeg (geen `eindigingsdatum`) → **leeg**.
- `nieuwe maxdatum` = 2026-11-30 + leeg = **2026-11-30** (ongewijzigd).
- Met alleen haar eigen voorwaarde zou de blokkade hier `onwaar` beslissen,
  en via herleving en einde zou dat het besluit van de grant worden:
  afwezig.

### case6-niet-werkloos-wel-vervallen.json — een vervallen omstandigheid zonder recht (niets beslist)

Feiten: niet werkloos; `maandinkomen` = 1800,00; omstandigheid vervallen met
`vervaldatum` = 2025-09-01; geen nieuw recht (`nieuw maandloon` = 0,00);
`maxdatum` = 2026-11-30; geen `eindigingsdatum`. De `vervaldatum` en de
`maxdatum` horen bij een recht dat volgens de lezing van `werkloos` als grond
van art. 15 nooit ontstond; ze staan er bewust, als invoer voor de
rekenregels, en een datum in de invoer betekent in dit model niet dat er
een recht was.

- Blokkade, herleving en einde eisen alle drie `werkloos` én 1800,00 >
  2100,00; geen van beide geldt → geen van de drie beslist.
- Basisgrond art. 15: niet werkloos → de grant-default beslist niet.
- Uitkomst: niets beslist, de groep schrijft niets: `recht op uitkering`
  **afwezig**. Zonder werkloosheid is er geen recht ontstaan (art. 15), en
  bij een inkomen onder de grens is er ook niets geëindigd (art. 20 lid 1
  onderdeel c): er is niets om te herleven.
- `verstreken periode` = tijdsduur(leeg, 2025-09-01) → **leeg**
  (leeg-propagatie: de `eindigingsdatum` ontbreekt — het spiegelbeeld van
  case4, waar de `vervaldatum` ontbreekt).
- `nieuwe maxdatum` = 2026-11-30 + leeg = **2026-11-30** (ongewijzigd).
- Met alleen haar eigen voorwaarde zou de herleving hier `waar` beslissen:
  aanwezig.

### case7-invoer-staat-tegen-blokkade.json — het doelkenmerk in de invoer (de blokkade beslist, de invoer blijft staan)

Feiten: die van case3, plus het kenmerk `recht op uitkering` zelf in de
invoer.

- De boom beslist als in case3: de blokkade-default beslist `onwaar`.
- Een `onwaar`-besluit verdringt de lagere defaults en laat de groep niets
  afleiden; het neemt geen kenmerk weg dat de invoer zet (RUNTIME-SEMANTICS
  ch. 7.1: "not a retraction"; het monotone schrijfregime van ch. 5.3).
- Uitkomst: `recht op uitkering` **aanwezig** — geen wetslezing maar de
  invoerregel van de IR: een `onwaar`-besluit verdringt geen kenmerkinvoer.
- Deze verwachting is voorlopig: RUNTIME-SEMANTICS ch. 7.8 zegt niets over
  een kenmerkdoel in de richting onwaar, en de kaart
  kenmerkinvoer-en-invoer-boven-berekening beslist dat nog — haar optie 7-i
  legt dit gedrag vast, haar optie 7-ii laat de verwachting omslaan naar
  afwezig.
- De rekenregels rekenen als in case3 (`inkomensgrens` 2100,00, `verstreken
  periode` 92, `nieuwe maxdatum` 2026-01-15); die waarden pint case3. Deze
  case pint alleen het kenmerk en de consistentie. Een uitvoerder die niets
  doet, haalt haar daarom ook; zij staat met reden in de overleverslijst van
  gate 7 (`scripts/check-noop.sh`). Wat zij onderscheidt, is een uitvoerder
  die een `onwaar`-besluit een invoerkenmerk laat wegnemen.

### case8-niet-werkloos-boven-grens-vervallen.json — herlevingsfeiten zonder recht (niets beslist)

Feiten: die van case1 zonder `werkloos`: `maandinkomen` = 2500,00;
omstandigheid vervallen; geen nieuw recht (`nieuw maandloon` = 0,00);
`eindigingsdatum` = 2025-05-01; `vervaldatum` = 2025-09-01; `maxdatum` =
2026-11-30. De `eindigingsdatum`, `vervaldatum` en `maxdatum` horen bij een
recht dat volgens de lezing van `werkloos` als grond van art. 15 nooit
ontstond; ze staan er bewust, omdat case8 case1 is met één verschil, en een
`eindigingsdatum` in de invoer betekent in dit model niet dat er een recht
was.

- Blokkade art. 21-2-a: geen nieuw recht → beslist niet.
- Herleving art. 21-1: 2500,00 > 2100,00 en de omstandigheid is vervallen,
  maar de werknemer is niet werkloos → beslist niet.
- Einde en grant: niet werkloos → beslissen niet.
- Uitkomst: niets beslist, de groep schrijft niets: `recht op uitkering`
  **afwezig**. Zonder werkloosheid is er geen recht ontstaan (art. 15), en
  art. 21 lid 1 laat alleen een recht herleven dat "op grond van artikel
  20, eerste lid, onderdeel a of c, is geëindigd".
- `verstreken periode` = **123** dagen en `nieuwe maxdatum` = **2027-04-02**,
  berekend als in case1 (de verschuiving is hier zonder gevolg).
- Met alleen haar eigen voorwaarde zou de herleving hier `waar` beslissen,
  en ook met alleen de conjuncten T en V: aanwezig.

### case9-geen-eindiging-wel-vervallen-en-nieuw-recht.json — een nieuw recht en een vervallen omstandigheid zonder eindiging (beslist op de grant-default)

Feiten: die van case5, plus omstandigheid vervallen met `vervaldatum` =
2025-09-01: werkloos; `maandinkomen` = 1800,00; nieuw recht ontstaan met
`nieuw maandloon` = 2300,00; `maxdatum` = 2026-11-30; geen
`eindigingsdatum`.

- Blokkade, herleving en einde: 1800,00 ≤ 2100,00 → geen van de drie
  beslist, ook al zijn de omstandigheid vervallen en het nieuwe recht er.
- Basisgrond art. 15: werkloos → de grant-default beslist `waar`.
- Uitkomst: `recht op uitkering` **toegekend** (beslist op de
  grant-default). Er is niets geëindigd, dus ook niets om te herleven of te
  blokkeren.
- `verstreken periode` = tijdsduur(leeg, 2025-09-01) → **leeg**.
- `nieuwe maxdatum` = 2026-11-30 + leeg = **2026-11-30** (ongewijzigd).
- Met alleen haar eigen voorwaarde zou de blokkade hier `onwaar` beslissen:
  afwezig.

### case10-nieuw-recht-onder-grens.json — een nieuw recht onder de grens (beslist op de herleving-default)

Feiten: die van case3, met een `nieuw maandloon` van 2000,00 in plaats van
2300,00: werkloos; `maandinkomen` = 2500,00; omstandigheid vervallen; nieuw
recht ontstaan met `nieuw maandloon` = 2000,00; `eindigingsdatum` =
2024-03-01; `vervaldatum` = 2024-06-01; `maxdatum` = 2025-10-15.

- Blokkade art. 21-2-a: werkloos, 2500,00 > 2100,00, de omstandigheid is
  vervallen en er is een nieuw recht, maar 2000,00 ≤ 2100,00: het
  maandloon van het nieuwe recht is niet meer dan 87,5% van het maandloon
  van de eerdere uitkering → de blokkade beslist niet.
- Herleving art. 21-1: werkloos, 2500,00 > 2100,00 en de omstandigheid is
  vervallen → de herleving-default beslist `waar`; dat besluit is ook dat
  van einde en grant.
- Uitkomst: `recht op uitkering` **toegekend** (beslist op de
  herleving-default).
- `verstreken periode` = **92** dagen en `nieuwe maxdatum` = **2026-01-15**,
  berekend als in case3; hier heeft de verschuiving wel gevolg, want het
  recht herleeft.
- Zonder het conjunct `nieuw maandloon > inkomensgrens` zou de blokkade hier
  `onwaar` beslissen: afwezig. case10 is de enige case die dat conjunct los
  pint; in case3 ligt het nieuwe maandloon boven de grens. Het andere eigen
  conjunct van de blokkade, `nieuw recht ontstaan`, pint geen case: het is
  alleen waarneembaar met een nieuw maandloon boven de grens zonder nieuw
  recht, een invoer die in deze casus niets betekent.

## Bewuste beperking (scope)

De defaults zijn atemporeel: één beslissing op één fixpunt. `maandloon`,
`maandinkomen`, `eindigingsdatum` en `vervaldatum` zijn hier scalaire
invoerfeiten. Het bepalen van de eindigingsmaand uit een maand-variërend
inkomensverloop, en de lifecycle over gebeurtenistijd (lopend → geëindigd →
herleefd), vallen bewust buiten dit anker — dat is de tijdlijn-machinerie
die voor een aparte kaart is uitgesteld. De dagen-lezing van "de periode"
in art. 43 lid 1 is hier als eigen keuze gekozen (kalenderdagen).
