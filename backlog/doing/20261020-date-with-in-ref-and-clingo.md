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

## Voorlopig besluit van de bouwer (2026-10-22): een datum die niet bestaat

*Voorlopig: genomen door de bouwer van deze kaart, niet door de eigenaar. De spec zei er niets over; de regel staat als
voorstel in RUNTIME-SEMANTICS ch. 3.8 op de kaarttak.*

**De regel.** `dateWith` neemt drie delen (jaar, maand, dag), `dateTimeWith` zeven (plus uur, minuut, seconde,
milliseconde). Elk deel is een geheel getal zonder eenheid; een deel met een breuk of een eenheid is een fout, nooit een
afkapping (zoals bij het paasjaar, ch. 3.8). De uitkomst is de burgerlijke datum van de proleptische gregoriaanse
kalender als de delen er een noemen: maand 1..12, dag 1..de lengte van die maand (schrikkeljaren meegerekend), en bij
`dateTimeWith` uur 0..23, minuut 0..59, seconde 0..59 (geen schrikkelseconde) en milliseconde 0..999, met het tijdstip
binnen het representeerbare millisecondebereik. **Noemen de delen geen dag of geen kloktijd, dan is dat een fout**:
nooit een doorrol naar de volgende maand (31 april is niet 1 mei), nooit een klem op het maandeinde (niet 30 april; de
klem van ch. 3.8 hoort bij datum plus duur, niet bij de constructie), nooit leeg. Een leeg deel geeft leeg (ch. 1.2);
alle delen worden eerst in draadvolgorde uitgerekend (ch. 5.11), daarna de leegtoets, daarna de geldigheid. Een jaar
buiten het machinegetal van de executor is een fout, nooit een omslag.

**Waarom zo.** De spec kent op drie plaatsen dezelfde houding: een datumlexeem van de invoer dat geen dag noemt, is een
fout (EF-003, `2026-02-30`); een datumstap buiten het bereik is een fout, "never a silent wrap-around" (EF-007); een
paasjaar met een breuk of een eenheid is een fout, "never a silent truncation". Een doorrol of klem zou een datum
opleveren die de schrijver niet schreef.

**De code.** Geen bestaande EF-soort past: EF-003 is een lexeem van de invoer op het kanaal `case input` met de
parameters `field` en `lexeme`, deze fout ontstaat bij de evaluatie uit uitgerekende delen; EF-007 is een stap. Een
nieuwe soort krijgt volgens ch. 5.7b haar code samen met haar rode geval, en dat rode geval kan op deze tak niet groen
landen: het Drools-been van gate 8 eist van een geval dat niet in zijn uitsluitingslijst staat een `FAULT` met de
vastgepinde EF-code, en de Drools-backend geeft deze fout zonder code (meting hieronder). Een vermelding in de
uitsluitingslijst helpt niet (een `FAULT` op een vermeld geval faalt als verouderd), en `backends/drools/` en de
gatescripts vallen buiten deze kaart. Dus: de fout is normatief en zonder code ("Kinds without a code", ch. 5.7b); ref en
clingo geven haar met een eigen zin; de ref-voorbeelden van ch. 3.8 pinnen haar. Het rode geval
(`corpus/red/date-with-no-calendar-day`, voorgestelde code EF-032, parameters `year`, `month`, `day`) ligt klaar als
vervolg, samen met de regel in ch. 5.7b, voor de kaart die de Drools-backend de code laat geven.

**Meting Drools** (shaded jar van `d6015ecf`, `DroolsRedMain` en de gewone replay, 2026-10-22; model met de delen als
attributen, één regel `dateWith` en één `dateTimeWith`):

| delen | Drools |
|---|---|
| 2026-03-01 10:30 | 2026-03-01 en 2026-03-01T10:30 |
| 2026, 4, 31 | FAULT, `dateWith: Invalid date 'APRIL 31'` (ComputeFault, geen EF-code) |
| 2026, 2, 29 | FAULT, `Invalid date 'February 29' as '2026' is not a leap year` |
| 2024, 2, 29 | 2024-02-29 |
| 2026, 13, 1 / 2026, 0, 1 | FAULT, `Invalid value for MonthOfYear (valid values 1 - 12): 13` / `: 0` |
| 2026, 3, 0 / 2026, 3, −1 | FAULT, `Invalid value for DayOfMonth (valid values 1 - 28/31): 0` / `: -1` |
| uur 24, minuut 60, seconde 60, milliseconde 1000 | FAULT, `Invalid value for HourOfDay (valid values 0 - 23): 24` enz. |
| jaar 0, jaar −5, jaar 10000 | 0000-03-01, -0005-03-01, +10000-03-01 |
| jaar 1 000 000 000 | FAULT, `Invalid value for Year (valid values -999999999 - 999999999)` |
| jaar 200 000 000 | +200000000-03-01 en +200000000-03-01T00:00 (de datum-tijd ligt buiten het millisecondebereik) |

Drools volgt de regel dus op elke dag die niet bestaat (een fout, geen doorrol of klem); het wijkt alleen af in de vorm:
zijn fout draagt geen EF-code (`FAULT EvaluationFaults: …` of `FAULT ComputeFault: …`), en een datum-tijd boven het
millisecondebereik (jaar ruim boven 146 miljoen) controleert het niet.
