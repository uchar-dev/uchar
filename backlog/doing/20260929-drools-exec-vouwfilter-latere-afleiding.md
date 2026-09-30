# drools-exec-vouwfilter-latere-afleiding: een vouw met een subselectie op afwezigheid herziet niet na een latere afleiding

*Vondst van een regelsetbouwer, 2026-09-29 (v2-skelet-vervolg), bevestigd door
de hoofdsessie op uchar `26ca6b0b` via de distributie (`bin/regelspraak
scenario`) en de eigen uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/vouwfilter/` (bron.rgs, scenario.rgs, cases/; alleen lezen).
Een stil fout antwoord: `check` exit 0, ref en clingo halen de verwachting,
de exec-tak niet.*

## Aanleiding

Een vouw over een rol met een subselectie die op afwezigheid toetst ("die aan
de volgende voorwaarde voldoen: zijn afgeleide dag is leeg") neemt in de
exec-tak een element op zolang die waarde nog niet is afgeleid, en herziet
de vouw niet wanneer een andere regel de waarde later schrijft. Met een
gewone regel als schrijver hangt de uitkomst af van de bronvolgorde
(schrijver vóór de vouw goed, erna fout); met een defaultsgroep als
schrijver is zij altijd fout; een ontkend kenmerk in de subselectie in
plaats van de leeg-toets gedraagt zich hetzelfde (meting van de bouwer op
`d59dc37`). Dezelfde familie als de kaart
drools-exec-voorwaarde-genavigeerde-afleiding (een toets die niet opnieuw
loopt wanneer de geteste waarde later verandert), nu in het filter van een
vouw. `backends/drools/README.md` noemt de volgordegrens van de leeg-toets
voor een voorwaarde; voor de subselectie van een vouw geldt hij ook, en
zonder de volgorde te helpen bij een defaultsgroep.

## Minimale reproductie

`26ca6b0b`, distributie `bin/regelspraak check|scenario|cases`.

    Objecttype de Vondst (mv: Vondsten)
        de vinddag Datum in dagen;
        de opgegeven dag Datum in dagen;
        de afgeleide dag Datum in dagen;
        het gewicht Numeriek (geheel getal);
        het zwaarste gewicht Numeriek (geheel getal);
    Objecttype de Berging (mv: Bergingen)
    Feittype BergingBetreftVondst
        de berging Berging
        de vondst (mv: vondsten) Vondst
    Eén berging betreft meerdere vondsten

    Regel afgeleid
        geldig altijd
            De afgeleide dag van een Vondst moet gesteld worden op de vinddag van de Vondst
            indien zijn vinddag gevuld is.

    Afwijking van afgeleid: opgave gaat voor
        geldig altijd
            De afgeleide dag van een Vondst moet gesteld worden op zijn opgegeven dag
            indien zijn opgegeven dag gevuld is.

    Regel zwaarste
        geldig altijd
            Het zwaarste gewicht van een Vondst moet berekend worden als de maximale waarde van het gewicht van alle vondsten van de berging van de Vondst die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg.

    Scenario: een Berging B met vondsten V1 (gewicht 10, geen vinddag) en V2 (vinddag 01-03-2026,
    gewicht 20); verwacht: de afgeleide dag van V2 is 01-03-2026, het zwaarste gewicht van V1 is 10.

    nu: check exit 0; scenario FAIL "attributeValue V1 zwaarste gewicht (verwacht=10, werkelijk=20)";
        cases + ref `oracle: PASS (2 expectations)`, clingo `clingo: PASS (2 expectations)`.

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  leeg-toets en de gevuld-toets in de subselectie, een ontkend en een
  bevestigd kenmerk, een vergelijking op een later afgeleide waarde; de
  schrijver als gewone regel vóór en na de vouw, en als defaultsgroep; de vouw
  in een waarde en in een voorwaarde (kwantor); een en twee rollen ver.
- (b) De exec-tak herziet een vouw wanneer een waarde die haar subselectie
  toetst later verandert (dezelfde far-side-reactiviteit als de kaart
  drools-exec-voorwaarde-genavigeerde-afleiding gaf aan een voorwaarde over
  een rol), of weigert de vorm benoemd waar dat niet lukt; rood-eerst
  JUnit-getuige met deze reproductie.
- (c) Golden (werknaam `jutterswet-zwaarste-vondst`), handgerekend, in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` in `scripts/check-drools.sh` als
  **voorstel**; de DRL-teksttak volgens haar kanaal.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: de
  alinea over de leeg-toets noemt de subselectie van een vouw.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel na de kaart
drools-exec-defaults-justificatie-vouw (zelfde regelevaluatie).
