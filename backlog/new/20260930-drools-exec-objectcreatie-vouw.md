# drools-exec-objectcreatie-vouw: een vouw in de toekenning van een objectcreatie rekent stil fout in de exec-tak

*Vondst van de bouwer van drools-exec-datumvouw-over-rol (2026-09-30, open
punt 3: de creatie rekent haar toekenningen nog via het oude pad), bevestigd
door de hoofdsessie op uchar `c16cb4d6` via de distributie (`bin/regelspraak
check|scenario|cases`) en de eigen uitvoerders; reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/creatie-vouw/` (g-pct, g-min-plus,
g-eerste: bron.rgs, scenario.rgs, cases2/; alleen lezen). Twee stille foute
antwoorden en één benoemde weigering: `check` exit 0, ref en clingo halen
de verwachting, de exec-tak niet. Op akkoord van de eigenaar geschreven
(2026-09-30).*

## Aanleiding

De kaart drools-exec-datumvouw-over-rol maakte van een vouw in de waarde
van een regel een eigen accumulate, waarna de waarde de uitkomst leest;
daarmee verdwenen twee stille foute antwoorden (een percentage van een som
werd leeg; het minimum van een lege bron plus 1 werd leeg in plaats van 1).
Een objectcreatie ("Een Jutter heeft een afrekening met heffing gelijk aan
…") verlaagt de waarden van haar toekenningen nog langs het oude pad
(`lowerVal`), zodat dezelfde twee vormen daar fout blijven, en een
eerste/laatste-vouw in een toekenning weigert benoemd waar zij in een
regelwaarde speelt. Ref en clingo geven in alle drie het goede antwoord.
De slots die de regelwaarde nu gebruikt, passen in de consequentie van de
creatie (bouwer, open punt 3).

## Minimale reproductie

`c16cb4d6`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Jutter (mv: Jutters) (bezield)
        de heffing Numeriek (getal met 2 decimalen);
        de afrekendag Datum in dagen;
        de weeggrens Numeriek (geheel getal);
    Objecttype de Afrekening (mv: Afrekeningen)
        de heffing Numeriek (getal met 2 decimalen);
        de afrekendag Datum in dagen;
        de weeggrens Numeriek (geheel getal);
    Objecttype de Vondst (mv: Vondsten)
        de vinddag Datum in dagen;
        het gewicht Numeriek (geheel getal);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten
    Feittype JutterHeeftAfrekening
        de afrekenaar Jutter
        de afrekening Afrekening
    Eén afrekenaar heeft één afrekening

    Regel lees terug
        geldig altijd
            De heffing van een Jutter moet berekend worden als de heffing van de afrekening van de Jutter.

    Scenario: J1 met vondsten van gewicht 4 (vinddag 12-03-2026) en 7 (vinddag 05-03-2026); J2 zonder
    vondsten. Per vorm de regel "maak een afrekening" en de verwachting op de teruggelezen waarde.

    R1  g-pct: "Een Jutter heeft een afrekening met heffing gelijk aan 10% van de som van het gewicht
        van alle vondsten van de Jutter." Verwacht: J1 1,10, J2 0,00.
        nu: check exit 0; exec FAIL "verwachting 1 FOUT: attributeValue J1 heffing (verwacht=1.10,
        werkelijk=leeg)" en "verwachting 2 FOUT: attributeValue J2 heffing (verwacht=0.00,
        werkelijk=leeg)"; ref `oracle: PASS (2 expectations)`; clingo `clingo: PASS (2 expectations)`;
        de DRL-teksttak speelt haar (bouwmeting: `drl: PLAYED 1`).
    R2  g-min-plus: "… met weeggrens gelijk aan (de minimale waarde van het gewicht van alle vondsten
        van de Jutter) plus 1." Verwacht: J1 5, J2 1.
        nu: check exit 0; exec FAIL "verwachting 2 FOUT: attributeValue J2 weeggrens (verwacht=1,
        werkelijk=leeg)" (over een lege bron maakt de regel geen object); ref en clingo PASS; de
        DRL-teksttak faalt hier ook (bouwmeting).
    R3  g-eerste: "… met afrekendag gelijk aan de eerste van de vinddag van alle vondsten van de
        Jutter." Verwacht: J1 05-03-2026.
        nu: check "niet uitvoerbaar in uchar (exit 3): transpiler SKIP: objectcreatie: maak een
        afrekening — objectcreatie (toekenning-waarde buiten scope): maak een afrekening"; scenario
        MARKER met dezelfde zin; ref en clingo PASS.

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: per
  toekenning van een objectcreatie de som, het aantal, het minimum, het
  maximum, de eerste en de laatste over een rol, kaal, als operand
  (percentage, plus een getal of een duur) en in een voorwaarde op de
  creatie; met een lege bron; twee toekenningen met elk een vouw; ook de
  feitcreatie en de begeleiders die dezelfde consequentie delen.
- (b) De objectcreatie rekent haar toekenningen langs dezelfde weg als een
  regelwaarde (de vouw als eigen accumulate, de toekenning leest de
  uitkomst), zodat R1 en R2 het antwoord van ref geven en R3 speelt; wat
  niet meegaat blijft een benoemde SKIP met de vorm in de zin; nooit een
  lege operand als 0, nooit een leeg antwoord waar ref een waarde geeft;
  rood-eerst JUnit-getuige met deze drie vormen.
- (c) Golden (werknaam `jutterswet-afrekening`), handgerekend, in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` (229) en `EXPECTED_DRL_PLAYED`
  (191) in `scripts/check-drools.sh` als **voorstel**; de DRL-teksttak
  volgens haar kanaal.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: de
  alinea over de vouw in een waarde noemt de toekenning van een creatie.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel na de kaart
drools-exec-vouwfilter-latere-afleiding (zelfde transpiler); v2 gebruikt
geen objectcreatie, dus niet nodig vóór de regelset verder gaat.
