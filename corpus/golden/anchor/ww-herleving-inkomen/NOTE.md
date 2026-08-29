# ww-herleving-inkomen — het WW-herleving realisme-anker (inkomenspad)

Het eerste WW-realisme-anker: het inkomens-eindigingspad
art. 20 lid 1 onderdeel c → art. 21 lid 1 eerste volzin (geen termijn). Eén
genest kenmerk-defaults-doel `recht op uitkering` — grondtoekenning (art. 15)
→ do-not-attribute op de inkomensgrens (art. 20-1-c) → herleving bij
vervallen omstandigheid (art. 21-1) → do-not-attribute op de blokkade
(art. 21-2-a) — plus drie gewone scalaire rekenregels. Vier cases, elk op een andere default beslist. Handgeschreven, met
absolute verwachtingen; alle getallen en datums zijn eigen casusfeiten.

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

Afgeleide attributen: `inkomensgrens`, `verstreken periode` (in hele dagen),
`nieuwe maxdatum`. Doel-kenmerk van de defaults: `recht op uitkering`.

### De defaults (het genest kenmerk-doel `recht op uitkering`)

De rose tree van art. 15 → 20-1-c → 21-1 → 21-2-a, als één defaults-bundel
met `Default.excepties`-nesting (elke uitzondering wordt eerst beslist en
verdringt haar drager wanneer zij beslist):

```
Default grant  (besluit waar,  als: werkloos)                                          -- art. 15
  exceptie -> Default einde     (besluit onwaar, als: maandinkomen > inkomensgrens)    -- art. 20-1-c
    exceptie -> Default herleving (besluit waar,  als: omstandigheid vervallen)        -- art. 21-1
      exceptie -> Default blokkade (besluit onwaar, als: nieuw recht ontstaan
                                    EN nieuw maandloon > inkomensgrens)                -- art. 21-2-a
```

De hoogste beslissende default wint; een default met een exceptie laat de
exceptie eerst beslissen en valt alleen terug op haar eigen besluit als de
exceptie niets beslist (RUNTIME-SEMANTICS ch. 7.9).

**WF-050** — precies één defaults-bundel, één schrijver, één doel: het
kenmerk `recht op uitkering` (exact één van kenmerk/attribuut). Geen enkele
gewone regel of objectcreatie schrijft dat kenmerk; de drie rekenregels
schrijven elk hun eigen attribuut, één keer.

**WF-051** — de defaults lezen nooit hun eigen doel. De default-condities lezen
`werkloos`, `maandinkomen`, `inkomensgrens`, `omstandigheid vervallen`,
`nieuw recht ontstaan` en `nieuw maandloon` — geen daarvan is `recht op
uitkering`. `inkomensgrens` is een *apart afgeleid* attribuut dat de
default-justificaties voedt (de termijn/grens-toets als eigen afleiding), niet
het defaults-doel. De afhankelijkheidsgraaf blijft acyclisch.

**WF-075** — elke `excepties` draagt precies één exception group (één
`ExceptionGroup`), dus `ongeordend` is op elk niveau onwaar (twee of meer leden
zouden de ongeordend-markering eisen). Elke default is een form-a-default (eigen
besluit *plus* geneste excepties), nooit een contentloze default.

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

## De vier cases — handberekening

In alle vier cases: `maandloon` = 2400,00, dus
`inkomensgrens` = 0,875 × 2400,00 = **2100,00**.

### case1-herleving.json — de herlevingscase (beslist op de herleving-default)

Feiten: werkloos; `maandinkomen` = 2500,00; omstandigheid vervallen; geen
nieuw recht (`nieuw maandloon` = 0,00); `eindigingsdatum` = 2025-05-01;
`vervaldatum` = 2025-09-01; `maxdatum` = 2026-11-30.

- Grond art. 20-1-c: 2500,00 > 2100,00 → de inkomensgrond geldt (het recht
  was geëindigd).
- Herleving art. 21-1: de omstandigheid is vervallen → de herleving-default
  beslist `waar`.
- Blokkade art. 21-2-a: geen nieuw recht (0,00 ≤ 2100,00) → geen blokkade.
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

- Grond art. 20-1-c: 1800,00 ≤ 2100,00 → de inkomensgrond geldt NIET; de
  einde-default beslist niet, en haar herleving/blokkade-excepties beslissen
  evenmin (hun voorwaarden — omstandigheid vervallen, nieuw recht — zijn
  hier onwaar; in de rose tree worden zij wel geëvalueerd, maar niets in de
  keten beslist).
- Basisgrond art. 15: werkloos → de grant-default beslist `waar`.
- Uitkomst: `recht op uitkering` **toegekend** (beslist op de grant-default);
  geen verschuiving.
- `verstreken periode` = leeg (geen `eindigingsdatum`) → **leeg**.
- `nieuwe maxdatum` = 2026-11-30 + leeg = **2026-11-30** (ongewijzigd).

### case3-blokkade.json — de blokkade (beslist op de blokkade-default)

Feiten: werkloos; `maandinkomen` = 2500,00; omstandigheid vervallen; nieuw
recht ontstaan met `nieuw maandloon` = 2300,00; `eindigingsdatum` =
2024-03-01; `vervaldatum` = 2024-06-01; `maxdatum` = 2025-10-15.

- Grond art. 20-1-c: 2500,00 > 2100,00 → de inkomensgrond geldt.
- Herleving art. 21-1: de omstandigheid is vervallen → zou herleven …
- Blokkade art. 21-2-a: nieuw recht ontstaan ÉN 2300,00 > 2100,00 → de
  blokkade geldt; de blokkade-default beslist `onwaar` en verdringt de
  herleving.
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

- Grond art. 20-1-c: 2500,00 > 2100,00 → de inkomensgrond geldt.
- Herleving art. 21-1: de omstandigheid is niet vervallen → de
  herleving-default beslist niet (en haar blokkade-exceptie evenmin).
- De einde-default beslist dus zélf `onwaar` en verdringt de grant.
- Uitkomst: `recht op uitkering` **niet toegekend** (beslist op de
  **einde**-default — onderscheiden van case3, waar de blokkade beslist).
- `verstreken periode` = tijdsduur(2026-02-01, leeg) → **leeg**
  (leeg-propagatie: de `vervaldatum` ontbreekt).
- `nieuwe maxdatum` = 2026-08-31 + leeg = **2026-08-31** (ongewijzigd).

De vier cases beslissen elk op een **andere** default — grant, herleving,
blokkade, einde — geen degenererende verzameling; elke default van de bundel
is ergens de beslisser.

## Bewuste beperking (scope)

De defaults zijn atemporeel: één beslissing op één fixpunt. `maandloon`,
`maandinkomen`, `eindigingsdatum` en `vervaldatum` zijn hier scalaire
invoerfeiten. Het bepalen van de eindigingsmaand uit een maand-variërend
inkomensverloop, en de lifecycle over gebeurtenistijd (lopend → geëindigd →
herleefd), vallen bewust buiten dit anker — dat is de tijdlijn-machinerie
die voor een aparte kaart is uitgesteld. De dagen-lezing van "de periode"
in art. 43 lid 1 is hier als eigen keuze gekozen (kalenderdagen).
