# drools-exec-kwantorvoorwaarde-eigen-operand: een berekening met een kwantorvoorwaarde herziet niet wanneer een eigen operand later wordt afgeleid

*Vondst van een regelsetbouwer, 2026-09-30 (V1 in zijn clusterdocument),
bevestigd door de hoofdsessie op uchar `de809cf7` via de distributie
(`bin/regelspraak scenario`) en de eigen uitvoerders; reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/v1/` (bron.rgs, bron-na.rgs,
bron-plus.rgs, scenario.rgs, scenario-plus.rgs, cases/, cases-plus/; alleen
lezen). Een stil fout antwoord: `check` exit 0, ref en clingo halen de
verwachting, de exec-tak niet.*

## Aanleiding

Een gewone berekening waarvan de voorwaarde een kwantor over een rol is
("indien de bergdag van alle vondsten van de Berging gevuld is") en waarvan
de waarde eigen attributen leest die een andere regel later afleidt, rekent
in de exec-tak één keer: zodra de kwantor waar wordt, met de operand nog
leeg. Wordt de operand daarna geschreven, dan herziet de regel niet. Bij
`maximale waarde van A en B` blijft de uitkomst leeg; bij `A plus 1` rekent
de lege operand als 0 en staat er een getal dat nergens op slaat. Staat de
schrijver van de operand in de bron vóór de berekening, of noemt de
voorwaarde de operand zelf ("zijn laatste bergdag is gevuld"), dan klopt de
uitkomst. Zonder rol in de voorwaarde (een gevuld-toets op een eigen
attribuut) is dezelfde berekening wél reactief op haar operanden (gemeten,
R5). Dezelfde familie als drools-exec-voorwaarde-genavigeerde-afleiding en
drools-exec-vouwfilter-latere-afleiding: een regel die niet opnieuw loopt
wanneer een gelezen waarde later verandert, nu bij de operanden van de
waarde onder een kwantorvoorwaarde. `backends/drools/README.md` ("Recorded:
a condition that reads through a role") noemt de volgordegrens van de
leeg-toets in een voorwaarde; deze vorm valt erbuiten: niet de voorwaarde
loopt te vroeg, de waarde wordt niet opnieuw berekend. De regelset heeft
het omzeild door de operanden in de voorwaarde te noemen (de berekening
"geldt alleen als een van beide operanden gevuld is"), een voorwaarde die de
wet niet stelt.

## Minimale reproductie

`de809cf7`, distributie `bin/regelspraak check|scenario|cases`; ref en
clingo via `_build/default/ref/bin/runner.exe` en
`_build/default/backends/clingo/bin/clingo_runner.exe` op `cases/ir.json`
en het casebestand.

    Objecttype de Berging (mv: Bergingen)
        de laatste bergdag Numeriek (geheel getal);
        de gemelde dag Numeriek (geheel getal);
        de sluitdag Numeriek (geheel getal);
    Objecttype de Vondst (mv: Vondsten)
        de vinddag Numeriek (geheel getal);
        de bergdag Numeriek (geheel getal);
    Feittype BergingBetreftVondst
        de berging Berging
        de vondst (mv: vondsten) Vondst
    Eén berging betreft meerdere vondsten

    Regel sluitdag
        geldig altijd
            De sluitdag van een Berging moet berekend worden als de maximale waarde van zijn laatste bergdag en zijn gemelde dag
            indien de bergdag van alle vondsten van de Berging gevuld is.

    Regel bergdag
        geldig altijd
            De bergdag van een Vondst moet berekend worden als zijn vinddag plus 1
            indien zijn vinddag gevuld is.

    Regel laatste bergdag
        geldig altijd
            De laatste bergdag van een Berging moet berekend worden als de maximale waarde van de bergdag van alle vondsten van de Berging.

    Scenario: een Berging B met één Vondst V (vinddag 30, berging B); verwacht: de laatste
    bergdag van B is 31, de sluitdag van B is 31.

    R1  bron.rgs zoals hierboven.
        nu: check exit 0; scenario FAIL "verwachting 2 FOUT: attributeValue B sluitdag
        (verwacht=31, werkelijk=leeg)", trace: sluitdag, bergdag en laatste bergdag GEVUURD;
        ref `oracle: PASS (2 expectations)`; clingo `TRANSLATION FAULT: expression kind not
        translated (no corpus case exercises it)` (het maximum van twee scalaire operanden; los
        van deze kaart, niet bouwen).
    R2  dezelfde bron met `Regel sluitdag` achteraan (bron-na.rgs).
        nu: PASS.
    R3  de waarde `zijn laatste bergdag plus 1`, verwacht 32 (bron-plus.rgs, scenario-plus.rgs).
        nu: exec FAIL "(verwacht=32, werkelijk=1)": de lege operand rekent als 0; ref PASS,
        clingo `clingo: PASS (2 expectations)`.
    R4  R1 met de operand in de voorwaarde ("hij voldoet aan alle volgende voorwaarden: de
        bergdag van alle vondsten van de Berging is gevuld; hij voldoet aan ten minste één van de
        volgende voorwaarden: zijn laatste bergdag is gevuld, zijn gemelde dag is gevuld").
        nu: PASS (meting van de regelsetbouwer, /tmp/v2/probe/g2-g10/v1-minimaal/).
    R5  zonder rol: `indien zijn invoer gevuld is` met een later afgeleide operand.
        nu: PASS (idem, model.rgs en model2.rgs): de reactiviteit op de operanden bestaat, alleen
        niet onder een kwantorvoorwaarde.

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  kwantor "alle", "ten minste één" en "geen" over een rol als voorwaarde
  (ook met een tweede, eigen voorwaarde ernaast); de operanden van de waarde
  eigen, één rol ver en als vouw; de schrijver van de operand als gewone
  regel vóór en na de berekening, en als defaultsgroep; de waarde als
  maximum of minimum van twee operanden, als som, en als één operand; ook
  `moet gesteld worden op`.
- (b) De exec-tak herziet een berekening met een kwantorvoorwaarde wanneer
  een operand van haar waarde later wordt afgeleid, in elke bronvolgorde,
  met dezelfde uitkomst als ref (en clingo waar clingo de vorm vertaalt), of
  weigert de vorm benoemd waar dat niet lukt; nooit een lege operand als 0;
  rood-eerst JUnit-getuige met deze reproductie.
- (c) Golden (werknaam `jutterswet-sluitdag`), handgerekend, in ref ≡ clingo
  ≡ Drools-exec (de plus-vorm van R3, zodat clingo meespeelt); `EXPECTED_PLAYED`
  (224) en `EXPECTED_DRL_PLAYED` (190) in `scripts/check-drools.sh` als
  **voorstel**; de DRL-teksttak volgens haar kanaal.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: een
  eigen alinea onder "Recorded" over de kwantorvoorwaarde en de reactiviteit
  op de operanden van de waarde, met wat na de kaart nog niet speelt.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: geen andere kaart in bouw;
werk op de kop van main (`de809cf7`).
