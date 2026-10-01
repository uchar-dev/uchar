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

## Result (2026-10-01, bouwer)

Tak `card/drools-exec-objectcreatie-vouw` in de worktree van de kaart, afgetakt van main `3fc145d1`; niet gerebased,
niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo: `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`, `_build/default/spec/verifier/bin/verifier.exe`, de frontend
(`Resolver --ir … --cases … bron.rgs scenario.rgs`) en de Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de
teksttak). "Vóór" is de jar van de claim `c045f500` (backend gelijk aan main), "na" de jar van `4916cc6b` (de code van
de kop `bc65caa4` op twee gebroken regels na). Elke vorm is een eigen Jutterswet-document (bron.rgs + scenario.rgs), door
een generator geschreven, door de frontend naar ir.json en cases verlaagd en door een meetscript gemeten; vier vormen
zijn een handgeschreven ir.json (minimum en maximum over een datum, die spelling typeert de frontend weg). Generator,
meetscripts en sweepscript staan buiten de repo (`/tmp/v2/probe/ocv-bouw/maak.py`, `meet.sh`, `meet-ir.sh`,
`sweep.sh`; per vorm een map met `meting.txt` onder `vorm/` en `mvorm/`). De reproducties van de kaart zijn uit
`/tmp/v2/probe/creatie-vouw/` gekopieerd (alleen gelezen) en opnieuw verlaagd. De pinnen stonden bij de claim op
`EXPECTED_PLAYED=232` en `EXPECTED_DRL_PLAYED=191` (de kaart noemt 229/191, de stand van toen).

### De reproductie op deze kop

| | ref | clingo | exectak vóór | exectak na | DRL-tekst (vóór = na) |
|---|---|---|---|---|---|
| R1 g-pct (10% van een som) | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | `FAIL case1-creatie.json: attribuut heffing: verwacht 1.10, werkelijk leeg; attribuut heffing: verwacht 0.00, werkelijk leeg` | `PLAYED 1` | `PLAYED 1` |
| R2 g-min-plus (minimum plus 1) | idem | idem | `FAIL case1-creatie.json: attribuut weeggrens: verwacht 1, werkelijk leeg` | `PLAYED 1` | `FAIL case1-creatie.json: attribuut weeggrens: verwacht 1, werkelijk leeg` |
| R3 g-eerste (eerste vinddag) | idem | idem | `EXCLUDE transpiler SKIP: objectcreatie: maak een afrekening — objectcreatie (toekenning-waarde buiten scope): maak een afrekening` | `PLAYED 1` | `EXCLUDE drl-transpiler MARKER: objectcreatie: maak een afrekening — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter` |

### Oorzaak

1. Een toekenning met een getalvouw (`containsAggregatie`: som, totaal, aantal, minimum, maximum, aantal gevulde) ging
   door een eigen evaluator, `lowerVal`, naast `scalarWaarde`. Die kende alleen getalliteralen, eigen attributen,
   parameters, vier rekenoperatoren, afronden en wortel. `percentage van` viel in haar `switch` op `default -> null`, dus
   R1 werd leeg voor elke Jutter. Een minimum of maximum bond zij zonder telling (`aggregatie(…, false)`): over een lege
   bron gaf de accumulate geen rij, de creatie vuurde niet, en het object ontbrak, ook voor een constante toekenning
   ernaast (d-kaal-leeg-en-constante); dat is R2. Een begrenzing, een genavigeerde lezing en datumrekenkunde weigerde zij
   (`aggregatie-toekenning buiten scope`).
2. Een eerste/laatste-vouw (en een minimum of maximum over een datum) is geen `isAggregatieKind`, dus de toekenning ging
   naar `scalarWaarde` zonder vouwslots; die geeft voor een vouw over een setbron dan `null` en de creatie weigerde
   (`toekenning-waarde buiten scope`); dat is R3.
3. Gevonden in de vormruimte, los van een vouw: (a) de scalaire toekenning werd zonder herschaling naar de eenheid van het
   lid geschreven; het commentaar zei dat `scalarWaarde` dat zelf deed, maar die kent het doel niet (f-eenheid-kaal:
   `verwacht 2000, werkelijk 2`, ref restate't op de schrijfgrens, `World.collapse`). (b) Een creatie heeft geen
   §6.5/§6.6-begeleider: een deling door leeg of een wortel van een negatief getal in een toekenning gaf de waarde leeg en
   de case `consistent` waar ref `inconsistent` zegt (h-deling-eigen-door-leeg, h-deling-door-lege-som,
   h-wortel-van-een-negatieve-som). (c) Een datumvouw in de voorwaarde van een creatie weigerde in de generieke zin
   `voorwaarde buiten scope`.

### Fix en de gekozen lowering

`ExecModelTranspiler` (+77, −155 regels over `4916cc6b` en `bc65caa4`, commentaar inbegrepen); `DrlTranspiler`,
`FactModelGenerator`, de loader, de stratificatie en de beangeneratie zijn niet aangeraakt.

- `objectCreatie` verlaagt elke toekenning met `scalarWaarde` terwijl `vouwslots`, `vouwOnderwerp` en `ketenWaardePad`
  aan staan, zoals `regel` dat voor een regelwaarde doet: elke vouw over een setbron wordt een accumulate (`vouwslot`,
  dezelfde `aggregatie` met telling naast een ordenende vouw en een som), gejoind aan `$subject`; de vouwen van alle
  toekenningen delen één lijst, dus de sleutels zijn uniek in de creatie. De consequentie bindt `[onderwerp, globals…,
  per vouw telling (waar gebonden) en uitkomst]` en vult de slots met `vulVouwslots` voordat de toekenningen rekenen. De
  accumulates staan op de regel na het patroon, de voorwaarde en de idempotentiewacht. Zo geldt rond de vouw elke
  operator van de scalaire waarde: percentage, plus en min met de lege operand van ch. 1.2, verminderd met (links leeg
  blijft leeg), datumrekenkunde, een tijdsduur, een begrenzing, een genavigeerde lezing, en de ordenende vouw over een
  datum. Over een lege bron leest de vouw leeg en de creatie munt haar object toch.
- Elke niet-kenmerktoekenning wordt op de schrijfgrens herschaald naar de gedeclareerde eenheid van het lid
  (`wf.herschaald(effType(waarde), nieuw.attribuut(lid), …)`, RUNTIME-SEMANTICS ch. 2.4), zoals een regelwaarde naar haar
  doel.
- Een toekenning met een §6.5/§6.6-plek weigert met de vorm: `objectcreatie (kanaal-deponerende toekenning (deling): een
  creatie heeft geen begeleider die de Fout meldt (ch. 1.2, ch. 5.6): <toekenning>)`, met `foutplekSoort`, dezelfde
  omheining als de consequentie van een default.
- De weigering van de voorwaarde van een creatie gebruikt `voorwaardeSkipReden`, zoals een regel; de zin van de
  ordenende datumvouw (daar en in `aggregatie`) noemt nu de drie plekken waar de tak zo'n vouw verlaagt (de waarde van
  een regel, de consequentie van een default, de toekenning van een creatie).
- `lowerVal` en de aggregatiehelft van `TkLower` zijn weg; de commentaren die naar `lowerVal` verwezen zijn bijgewerkt.

Verworpen: `lowerVal` uitbreiden met percentage, telling en datum (een tweede evaluator naast `scalarWaarde`; de twee
foute antwoorden waren precies haar afwijking ervan, zoals drools-exec-datumvouw-over-rol voor de regelwaarde vaststelde);
een begeleider voor de creatie bouwen in plaats van de omheining (een begeleider moet vuren precies wanneer de creatie
haar toekenning uitrekent, dus onder dezelfde idempotentiewacht en vóór de creatie die wacht onwaar maakt; dat is een
eigen constructie, open punt 2); en de creatie onder de schrijvers van wat zij leest plaatsen (open punt 1: een eigen ordeningsvraag, niet
de lowering van een vouw).

### (a) Vormruimte, vóór en na

Situatie van elke gegenereerde vorm (één case): J1 met V1 (vinddag 12-03-2026, gewicht 4), V2 (05-03-2026, 7) en V3
(geen waarden), in die volgorde; J2 zonder vondsten; J3 met alleen V4 (geen waarden); J4 met V5 (20-03-2026, 9). De
creatie is "Regel maak een afrekening: Een Jutter heeft een afrekening met <toekenningen> [indien <voorwaarde>]", één
afrekenaar heeft één afrekening (e-*: meerdere, de creatiemarkering als wacht), en de Jutter leest elke toekenning terug
("De X van een Jutter moet berekend worden als de X van de afrekening van de Jutter"). a-*: de vouw kaal; b-*: als operand;
c-*: in de voorwaarde van de creatie; d-*: meer toekenningen met een vouw; e-*: een rol naar meer afrekeningen; f-*:
flanken (subselectie, later afgeleid lid, navigatie, eenheid, de volgorde van de schrijvers); g-*: een feitcreatie onder
een vouw ("Een gecontroleerd net van een Jutter is een net van de Jutter indien …"); h-*: de begeleiders, een deling of
wortel in een toekenning; m-*: het minimum en maximum over een datum, een handgeschreven ir.json. SKIP is
`EXCLUDE transpiler SKIP: objectcreatie: maak een afrekening — objectcreatie (…): maak een afrekening`, MARKER is
`EXCLUDE drl-transpiler MARKER: … — <marker>: …`; "idem" in de kolom exec na: gelijk aan vóór. De DRL-kolom is vóór en
na gelijk in elke rij. Alle verlaagde documenten zijn verifier-schoon.

| vorm | omschrijving | ref | clingo | exec vóór | exec na | DRL-tekst |
|---|---|---|---|---|---|---|
| a-aantal | aantal kaal | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| a-eerste | eerste kaal (datum) | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO aggregatie EERSTE |
| a-eerste-getal | eerste over een getal | frontend: `eerste van vereist datumexpressies, kreeg Numeriek` | — | — | — | — |
| a-laatste | laatste kaal (datum) | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO aggregatie LAATSTE |
| a-laatste-getal | laatste over een getal | frontend: `laatste van vereist datumexpressies, kreeg Numeriek` | — | — | — | — |
| a-maximum | maximum kaal | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| a-minimum | minimum kaal | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| a-som | som kaal | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| b-afgerond | ((som) gedeeld door 3) rekenkundig afgerond op 2 decimalen | PASS | PASS | PLAYED 1 | SKIP (kanaal-deponerende toekenning (deling) …) | PLAYED 1 |
| b-begrensd | (aantal) min 2, met een minimum van 0 | PASS | PASS | SKIP (aggregatie-toekenning buiten scope) | PLAYED 1 | PLAYED 1 |
| b-duur-eerste | (eerste) plus 30 dagen | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO datum-rekenkunde |
| b-duur-laatste | (laatste) plus 30 dagen | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO datum-rekenkunde |
| b-eigen-plus-vouw | een eigen attribuut van de Jutter plus de som | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| b-gedeeld-som | (som) gedeeld door 4 | PASS | PASS | PLAYED 1 | SKIP (kanaal-deponerende toekenning (deling) …) | PLAYED 1 |
| b-maal-som | (som) maal 2 | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| b-min-minimum | (minimum) min 1: een leeg minimum telt als 0 | PASS | PASS | FAIL `telling: verwacht -1, werkelijk leeg` (+1) | PLAYED 1 | FAIL `telling: verwacht -1, werkelijk leeg` (+1) |
| b-minduur-eerste | (eerste) min 7 dagen | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO datum-rekenkunde |
| b-minduur-laatste | (laatste) min 7 dagen | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO datum-rekenkunde |
| b-pct-aantal | 10% van aantal | PASS | PASS | FAIL `heffing: verwacht 0.30, werkelijk leeg` (+3) | PLAYED 1 | PLAYED 1 |
| b-pct-maximum | 10% van maximum | PASS | PASS | FAIL `heffing: verwacht 0.70, werkelijk leeg` (+3) | PLAYED 1 | FAIL `heffing: verwacht 0.00, werkelijk leeg` (+1) |
| b-pct-minimum | 10% van minimum | PASS | PASS | FAIL `heffing: verwacht 0.40, werkelijk leeg` (+3) | PLAYED 1 | FAIL `heffing: verwacht 0.00, werkelijk leeg` (+1) |
| b-pct-som | 10% van som | PASS | PASS | FAIL `heffing: verwacht 1.10, werkelijk leeg` (+3) | PLAYED 1 | PLAYED 1 |
| b-plus-aantal | (aantal) plus 1 | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| b-plus-maximum | (maximum) plus 1 | PASS | PASS | FAIL `weeggrens: verwacht 1, werkelijk leeg` (+1) | PLAYED 1 | FAIL `weeggrens: verwacht 1, werkelijk leeg` (+1) |
| b-plus-minimum | (minimum) plus 1 | PASS | PASS | FAIL `weeggrens: verwacht 1, werkelijk leeg` (+1) | PLAYED 1 | FAIL `weeggrens: verwacht 1, werkelijk leeg` (+1) |
| b-plus-som | (som) plus 1 | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| b-tijdsduur | de tijdsduur van eerste tot laatste in hele dagen | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO tijdsduur |
| b-twee-vouwen-een-toekenning | aantal plus som in één toekenning | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| b-verminderd-som | (som) verminderd met 1: een lege som blijft leeg | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| c-voorwaarde-aantal | aantal in de voorwaarde van de creatie | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| c-voorwaarde-eerste | eerste in de voorwaarde van de creatie | PASS | PASS | SKIP (voorwaarde buiten scope) | SKIP (voorwaarde met een ordenende vouw over een datumbron …) | MARKER TODO voorwaarde |
| c-voorwaarde-en-toekenning | som in de voorwaarde en in de toekenning | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| c-voorwaarde-laatste | laatste in de voorwaarde van de creatie | PASS | PASS | SKIP (voorwaarde buiten scope) | SKIP (voorwaarde met een ordenende vouw over een datumbron …) | MARKER TODO voorwaarde |
| c-voorwaarde-maximum | maximum in de voorwaarde van de creatie | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| c-voorwaarde-minimum | minimum in de voorwaarde van de creatie | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| c-voorwaarde-som | som in de voorwaarde van de creatie | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| d-getal-en-datum | een getalvouw en een datumvouw | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO aggregatie EERSTE |
| d-kaal-leeg-en-constante | een kaal minimum en een constante: het object bestaat ook over een lege bron | PASS | PASS | FAIL `telling: verwacht 5, werkelijk leeg` (+1) | PLAYED 1 | FAIL `telling: verwacht 5, werkelijk leeg` (+1) |
| d-twee-toekenningen | twee toekenningen met elk een vouw (10% van som; minimum plus 1) | PASS | PASS | FAIL `heffing: verwacht 1.10, werkelijk leeg` (+5) | PLAYED 1 | FAIL `heffing: verwacht 0.00, werkelijk leeg` (+3) |
| d-vier-vouwen | vier toekenningen met een ordenende vouw (acht gebonden variabelen) | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO aggregatie EERSTE |
| d-vijf-vouwen | vijf toekenningen met een vouw (te veel gebonden variabelen) | PASS | PASS | SKIP (toekenning-waarde buiten scope) | SKIP (te veel gebonden variabelen: globals en vouwen) | MARKER TODO aggregatie EERSTE |
| e-meervoudig-min-plus | (minimum) plus 1, één afrekenaar heeft meerdere afrekeningen | PASS | PASS | FAIL `weeggrens: verwacht 1, werkelijk leeg` (+1) | PLAYED 1 | FAIL `weeggrens: verwacht 1, werkelijk leeg` (+1) |
| e-meervoudig-pct | 10% van som, één afrekenaar heeft meerdere afrekeningen | PASS | PASS | FAIL `heffing: verwacht 1.10, werkelijk leeg` (+3) | PLAYED 1 | PLAYED 1 |
| f-creatie-keten | telling over de netten die een creatie na deze creatie maakt | PASS | PASS | FAIL `telling: verwacht 1, werkelijk 0` (+3) | idem | FAIL `telling: verwacht 1, werkelijk 0` (+3) |
| f-eenheid | som in kg naar een toekenning in g | PASS | TRANSLATION FAULT (eenheid onder een initialisatie) | PLAYED 1 | idem | FAIL `massa: verwacht 11000, werkelijk 11` (+1) |
| f-eenheid-kaal | een eigen attribuut in kg naar een toekenning in g (geen vouw) | PASS | TRANSLATION FAULT (eenheid onder een initialisatie) | FAIL `massa: verwacht 2000, werkelijk 2` (+3) | PLAYED 1 | FAIL `massa: verwacht 2000, werkelijk 2` (+3) |
| f-eigen-door-default | een eigen attribuut dat een defaultsgroep schrijft (geen vouw) | PASS | PASS | FAIL `telling: verwacht 7, werkelijk leeg` (+3) | idem | EXCLUDE (defaults) |
| f-feitcreatie-keten | telling over de relatie die een feitcreatie legt | PASS | PASS | FAIL `telling: verwacht 1, werkelijk 0` (+3) | idem | FAIL `telling: verwacht 1, werkelijk 0` (+3) |
| f-genavigeerd-plus-vouw | de bonus van de strandvoogd van de Jutter plus de som | PASS | PASS | SKIP (aggregatie-toekenning buiten scope) | PLAYED 1 | PLAYED 1 |
| f-kenmerk-later-afgeleid | laatste over de zware vondsten, zwaar door een regel na de creatie | PASS | PASS | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO aggregatie LAATSTE |
| f-lid-door-default | som over een nettogewicht dat een defaultsgroep schrijft | PASS | PASS | FAIL `telling: verwacht 15, werkelijk leeg` (+2) | idem | EXCLUDE (defaults) |
| f-lid-later-afgeleid | som over een gewicht dat een regel na de creatie afleidt | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| f-subselectie | laatste over de vondsten met gewicht groter dan 5 | PASS | ERROR (onveilige variabele bij het gronden) | SKIP (toekenning-waarde buiten scope) | PLAYED 1 | MARKER TODO aggregatie LAATSTE |
| g-feit-aantal | feitcreatie onder het aantal vondsten groter dan 1 | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| g-feit-eerste | feitcreatie onder de eerste vinddag later dan 01-03-2026 | PASS | PASS | SKIP feitcreatie (voorwaarde niet naar anker of doel te lowereren: …) | idem | MARKER TODO voorwaarde niet naar anker of doel te lowereren — regel inert i.p.v. over-creatie |
| g-feit-minimum | feitcreatie onder het minimum groter dan 3 | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| g-feit-som | feitcreatie onder de som groter dan 10 | PASS | PASS | PLAYED 1 | idem | PLAYED 1 |
| h-deling-door-lege-som | 100 gedeeld door de som, afgerond (J2, J3: deling door leeg) | PASS | PASS | FAIL `verwacht inconsistent, werkelijk consistent` | SKIP (kanaal-deponerende toekenning (deling) …) | FAIL `verwacht inconsistent, werkelijk consistent` |
| h-deling-door-lege-som-consistent | idem, zonder J2 en J3 (elke som gevuld) | PASS | PASS | PLAYED 1 | SKIP (kanaal-deponerende toekenning (deling) …) | PLAYED 1 |
| h-deling-eigen-door-leeg | zijn basis gedeeld door zijn deler (geen vouw), J2 zonder deler | PASS | PASS | FAIL `verwacht inconsistent, werkelijk consistent` | SKIP (kanaal-deponerende toekenning (deling) …) | FAIL `verwacht inconsistent, werkelijk consistent` |
| h-wortel-van-een-negatieve-som | de wortel van ((som) min 10), afgerond (J2, J3: min 10 negatief) | PASS | TRANSLATION FAULT (afgeronde wortel) | FAIL `verwacht inconsistent, werkelijk consistent` | SKIP (kanaal-deponerende toekenning (wortel) …) | FAIL `verwacht inconsistent, werkelijk consistent` |
| m-maximum | het maximum over een datum, kaal | PASS | PASS | SKIP (aggregatie-toekenning buiten scope) | PLAYED 1 | MARKER vouw MAXIMUM (zin van Vouwwaarde) |
| m-minimum | het minimum over een datum, kaal | PASS | PASS | SKIP (aggregatie-toekenning buiten scope) | PLAYED 1 | MARKER vouw MINIMUM (zin van Vouwwaarde) |
| m-minimum-plus-duur | (het minimum over een datum) plus 30 dagen | PASS | PASS | SKIP (aggregatie-toekenning buiten scope) | PLAYED 1 | MARKER vouw MINIMUM (zin van Vouwwaarde) |
| m-minimum-voorwaarde | het minimum over een datum in de voorwaarde (later dan 01-03-2026) | PASS | PASS | SKIP (voorwaarde buiten scope) | SKIP (voorwaarde met een ordenende vouw over een datumbron …) | MARKER vouw MINIMUM (zin van Vouwwaarde) |

Samengevat over de 63 vormen die een document hebben (59 uit de frontend, 4 handgeschreven; a-eerste-getal en
a-laatste-getal weigert de frontend): ref `oracle: PASS` op alle 63; clingo `clingo: PASS` op 59, `TRANSLATION FAULT`
op f-eenheid en f-eenheid-kaal (een eenheid onder een initialisatie) en h-wortel-van-een-negatieve-som (een afgeronde
wortel), en een grondfout op f-subselectie (open punt 4). Exectak vóór → na: 16 EXCLUDE → PLAYED, 12 FAIL → PLAYED, 20
PLAYED → PLAYED, 3 FAIL → benoemde SKIP (de drie h-vormen die consistent antwoordden waar ref inconsistent zegt), 3
PLAYED → benoemde SKIP (b-gedeeld-som, b-afgerond en h-deling-door-lege-som-consistent: een deling door een literaal of
door een gevulde som, die de omheining ook weigert, afwijking 2), 5 EXCLUDE → EXCLUDE (c-voorwaarde-eerste,
c-voorwaarde-laatste en m-minimum-voorwaarde nu met de vorm in de zin, d-vijf-vouwen met `te veel gebonden variabelen:
globals en vouwen`, g-feit-eerste ongewijzigd), en 4 FAIL → FAIL (f-eigen-door-default, f-lid-door-default,
f-creatie-keten, f-feitcreatie-keten: de creatie vuurt vóór een latere schrijver van wat zij leest, open punt 1; twee
van de vier zonder vouw). Elke PLAYED is met de verwachtingen die ref bevestigt, ook waar clingo faalt. DRL-tak vóór en na
gelijk op alle 63: 27 PLAYED, 15 FAIL, 21 EXCLUDE.

De gemeten FAIL-zinnen van de vier die blijven: f-lid-door-default `attribuut telling: verwacht 15, werkelijk leeg;
attribuut telling: verwacht -1, werkelijk leeg; attribuut telling: verwacht 8, werkelijk leeg` (een defaultsgroep schrijft
het nettogewicht), f-eigen-door-default `attribuut telling: verwacht 7, werkelijk leeg; …` (een defaultsgroep schrijft
een eigen attribuut, geen vouw), f-creatie-keten en f-feitcreatie-keten `attribuut telling: verwacht 1, werkelijk 0` voor
elke Jutter (een telling over netten die een later gedeclareerde creatie munt, of over een relatie die een feitcreatie
legt; de DRL-tak antwoordt hetzelfde).

### (b) Getuige

`ObjectcreatieVouwWitnessTest` (exectak, vijftien rijen, documenten in de klasse op Jutterswet-materiaal, met de waarden
van ref en clingo; de twee documenten van de eenheidsrij met die van ref alleen): `r1_tien_procent_van_een_som` (en de DRL-tak speelt haar ook),
`r2_het_minimum_van_een_lege_bron_plus_een`, `r3_de_eerste_vinddag` (en de DRL-tak weigert met `TODO aggregatie
EERSTE`), `de_getalvouwen_als_operand_lezen_een_lege_bron_als_leeg` (10% van aantal, minimum en maximum; minimum min 1;
som verminderd met 1; maximum plus 1), `een_datumvouw_plus_of_min_een_duur`,
`een_kaal_minimum_over_een_lege_bron_munt_het_object_toch`, `twee_toekenningen_met_elk_een_vouw`,
`de_markering_als_wacht_bij_meer_afrekeningen`, `een_begrenzing_een_tijdsduur_en_een_genavigeerde_lezing_rond_een_vouw`,
`een_subselectie_op_een_later_afgeleid_kenmerk`, `de_toekenning_wordt_herschreven_in_de_eenheid_van_het_lid` (ref
alleen), drie rijen die een benoemde weigering pinnen (`een_deling_in_een_toekenning_weigert_met_de_vorm`,
`vijf_vouwen_binden_te_veel`, `een_datumvouw_in_de_voorwaarde_van_de_creatie_weigert_met_de_vorm`) en de flank
`de_getalvouwen_in_de_voorwaarde_van_de_creatie`. De 28 documenten die de klasse speelt (R1 en R3 ook in de DRL-tak) zijn uit een proefrun van de test geschreven en naast de getuige gemeten: elk `verifier: PASS` en `oracle:
PASS`, en `clingo: PASS` behalve de twee eenheidsdocumenten (`TRANSLATION FAULT: non-identical units under the
initialisation of "massa" are not translated …`); de refusal-rijen dragen ook de verwachtingen van ref.

Rood op de claim (`290c465b` tegen de transpiler van `c045f500`):

    Tests run: 15, Failures: 10, Errors: 4, Skipped: 0
    r1_tien_procent_van_een_som:55 exectak ==> expected: <null> but was: <case: attribuut heffing: verwacht 1.10, werkelijk leeg; …>
    r2_het_minimum_van_een_lege_bron_plus_een:65 … <case: attribuut weeggrens: verwacht 1, werkelijk leeg; attribuut weeggrens: verwacht 1, werkelijk leeg>
    r3_de_eerste_vinddag:74 » Excluded transpiler SKIP: objectcreatie: maak een afrekening — objectcreatie (toekenning-waarde buiten scope): maak een afrekening
    de_toekenning_wordt_herschreven_in_de_eenheid_van_het_lid:214 een eigen attribuut in kg ==> … <case: attribuut massa: verwacht 2000, werkelijk 2; …>
    een_kaal_minimum_over_een_lege_bron_munt_het_object_toch:125 … <case: attribuut telling: verwacht 5, werkelijk leeg; attribuut telling: verwacht 5, werkelijk leeg>
    een_deling_in_een_toekenning_weigert_met_de_vorm:241 Expected …CaseRunner.Excluded to be thrown, but nothing was thrown.
    een_datumvouw_in_de_voorwaarde_van_de_creatie_weigert_met_de_vorm:282 transpiler SKIP: … objectcreatie (voorwaarde buiten scope): maak een afrekening ==> expected: <true> but was: <false>
    vijf_vouwen_binden_te_veel:268 transpiler SKIP: … objectcreatie (toekenning-waarde buiten scope): … ==> expected: <true> but was: <false>
    een_begrenzing_een_tijdsduur_en_een_genavigeerde_lezing_rond_een_vouw:163 » Excluded … (aggregatie-toekenning buiten scope) …
    een_datumvouw_plus_of_min_een_duur:111 en een_subselectie_op_een_later_afgeleid_kenmerk:197 » Excluded … (toekenning-waarde buiten scope) …
    de_getalvouwen_als_operand_lezen_een_lege_bron_als_leeg:101, de_markering_als_wacht_bij_meer_afrekeningen:147 en
      twee_toekenningen_met_elk_een_vouw:135 … werkelijk leeg …

De flank was groen. Na de fix: de klasse 15 tests, 0 failures; de suite 975 tests, 0 failures, 0 skipped (960 op de
claim).

### (c) Golden `jutterswet-afrekening` (`862e60ea`)

Uit `bron.rgs` en `scenario.rgs`, handgerekend in NOTE.md: de creatie "maak een afrekening" met vier toekenningen die elk
over de vondsten van de Jutter vouwen (heffing 10% van de som van het gewicht, weeggrens het minimum van het gewicht plus
1, afrekendag de eerste vinddag, telling het aantal vondsten), en de creatie "geef een zeebrief" over een rol naar meer
zeebrieven met slotdag "(de laatste van de vinddag van alle vondsten van de Jutter die zwaar zijn) plus 30 dagen" onder
"het aantal vondsten van de Jutter groter is dan 0"; zwaar door een regel na beide creaties; de Jutter leest alles terug,
de zeebrieven met een laatste-vouw en een telling. Case 1: de vier bronvormen; case 2: een volgorde die niet die van de
waarden is, een gelijkspel, en een afrekening die de case aan J6 koppelt, zodat de 1:1-wacht de munt tegenhoudt. De IR uit
`--ir bron.rgs` alleen is byte-gelijk aan die met het scenario.

| case | verwachtingen | ref | clingo | exec vóór | exec na | DRL-tekst | no-op |
|---|---|---|---|---|---|---|---|
| case1-vier-bronvormen | 24 | `oracle: PASS (24 expectations)` | `clingo: PASS (24 expectations)` | EXCLUDE (golden) | PASS | EXCLUDE (golden) | 20 MISMATCH |
| case2-volgorde-gelijkspel-en-een-ingevoerde-afrekening | 12 | `oracle: PASS (12 expectations)` | `clingo: PASS (12 expectations)` | idem | PASS | idem | 11 MISMATCH |

Per golden: exectak vóór `EXCLUDE transpiler SKIP: objectcreatie: maak een afrekening — objectcreatie
(toekenning-waarde buiten scope): maak een afrekening`, na `PLAYED 2`; DRL-tak `EXCLUDE drl-transpiler MARKER: regel:
slotdag — TODO aggregatie LAATSTE: de laatste van de slotdag van alle zeebrieven van de Jutter` (achter die marker
markert zij ook beide creaties: `TODO aggregatie EERSTE` in "maak een afrekening", `TODO datum-rekenkunde` in "geef een
zeebrief", gemeten op afgeslankte kopieën). Toegelaten via `scripts/admit.sh` met het gate-12-voorstel en de README in de
werkboom: `admit: ADMITTED corpus/golden/jutterswet-afrekening (2 case(s); verifier-clean, ref = clingo, no-op refuted,
gates green)`. Niet in de golden, met reden in NOTE.md: de herschaling naar de eenheid van het lid en een wortel (clingo
vertaalt ze niet), een subselectie die een lid vergelijkt (clingo grondt haar niet in een toekenning), een deling (de
exectak weigert haar), een datumvouw in de voorwaarde (geweigerd) en een latere schrijver van wat de creatie leest (open
punt 1).

### Gate-12-voorstel (`1b331e97`, `scripts/check-drools.sh`, beschermd pad)

- `EXPECTED_PLAYED`: 232 → 233, redenblok "232 -> 233 (2026-10-01, a fold in the toekenning of an object creation)" na
  dat van jutterswet-havenketen.
- `DRL_CHANNEL` klasse (C): `"jutterswet-afrekening|EXCLUDE|MARKER regel: TODO aggregatie LAATSTE over a set source — in
  the rule that reads the zeebrieven back; behind it the branch markers both creations as well (…)"` op naamvolgorde
  (vóór jutterswet-havenketen); kop "— 12" → "— 13" met een historieregel "12 -> 13 (2026-10-01, a fold in the toekenning
  of an object creation)"; "SEVEN of the twelve" → "SEVEN of the thirteen"; "FIVE are this branch alone" → "SIX" met de
  regel "jutterswet-afrekening (exec PLAYED 2) — the ordering fold over a date set source, in the rule that reads the
  zeebrieven back, and behind it in the toekenningen of both creations". Klasse (A): "sitting 41 below" → "sitting 42
  below", "The 41 is 45 exec-only players — those 38, jutterswet-havenketen, …" → "The 42 is 46 exec-only players — those
  38, jutterswet-afrekening, jutterswet-havenketen, …", en een hertelzin "the gap is 42 (233 - 191); the six class heads
  (A 39, B 37, C 13, D 2, E 3, F 3 = 97 lines)". Lijstkop "ninety of the ninety-six" → "ninety-one of the ninety-seven".
- `EXPECTED_DRL_PLAYED` blijft 191, `EXPECTED_MIN_TESTS` blijft 239, `EXPECTED_CHANNEL` en `EXCLUDE_KNOWN` ongewijzigd.

### (d) Sweep

Over alle 287 goldens van de claim, elke golden met al zijn cases, statusregel per golden en per tak, jar van de claim
(`c045f500`) tegen jar van `4916cc6b`: alle 574 regels gelijk (exectak PLAYED 232, DRL-tak PLAYED 191). Geen bestaande
golden draagt een vouw, een deling of een wortel in een toekenning, en geen statusregel draagt een van de twee
herschreven weigerzinnen. Ook de herschaling op de schrijfgrens verschuift niets: in de zeventien bestaande goldens
met een objectcreatie draagt geen toekenning en geen lid van een toekenning een eenheid. De nieuwe golden: exectak
vóór EXCLUDE, na `PLAYED 2`; DRL-tak EXCLUDE. De beangeneratie is niet aangeraakt.

### README (`a7db211b`)

`backends/drools/README.md`, de sectie over een vouw in een waarde heet nu "Recorded: a fold over a set source inside a
rule's value or a creation's toekenning". De zin "An object creation still lowers a fold in a toekenning through
`lowerVal` …" is weg ("Three limits remain" → "Two limits remain"), en een eigen alinea zegt wat de toekenning van een
creatie nu doet (de vouwen als accumulates na de wacht, de herschaling naar de eenheid van het lid, golden en getuige),
wat daarvoor fout ging, wat met naam weigert (een deling of wortel, te veel vouwen, een datumvouw in de voorwaarde), de
gemeten grens van open punt 1, en de DRL-tak.

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates14/run.sh` op de kop `bc65caa4` (de laatste
codecommit; de commit met dit Result raakt alleen de kaart), met een schone werkboom: `eval $(opam env)`, `dune build`
(exit 0), dan elk script onder zijn shebang (bash voor catala, regelspraak, toka-parity, drools, flogic, graaf en
scenario; sh voor de negen andere). Logs in `/tmp/v2/uchar-gates14/`. Per gate de exitcode en de PASS-regel:

     1. check-self-contained (sh): exit 0 — check-self-contained: PASS
     2. check-ir-schema (sh): exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 327 file(s) validate and round-trip, floor 192; …)
     3. check-case-schema (sh): exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 821 case file(s) validate)
     4. check-verifier (sh): exit 0 — check-verifier: PASS (327 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), …)
     5. check-oracle (sh): exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 781 cases replayed exactly, floor 427; NOT replayed, no case file yet: …)
     6. check-clingo (sh): exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 781 cases replay exactly through clingo — ref = clingo over the corpus)
     7. check-noop (sh): exit 0 — check-noop: PASS (737 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
     8. check-rood (sh): exit 0 — check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
     9. check-catala (bash): exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10. check-regelspraak (bash): exit 0 — check-regelspraak: PASS — JUnit suite green (1008 tests, floor 173); 115 rgs golden(s) re-emit byte-identically (floor 80)
    11. check-toka-parity (bash): exit 0 — check-toka-parity: PASS
    12. check-drools (bash): exit 0 — check-drools: PASS
    13. check-reftype-derivation (sh): exit 0 — check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    14. check-flogic (bash): exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    15. check-graaf (bash): exit 0 — check-graaf: PASS
    16. check-scenario (bash): exit 0 — check-scenario: PASS — 22 scenario golden(s) (floor 7) regenerate 80 case file(s) (floor 31) and their ir.json byte-identically

Gate 12 verder, regels uit zijn samenvatting:

    played 233  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
    auto-excluded 52  (per-form refusals: loader / transpiler marker / contract / unsupported input)
    channel floor 55 non-PLAYED goldens pinned by name; played matches the pinned 233
    DRL-text branch played 191  (its OWN count — the narrower branch, not the square; pin 191)
    DRL-text floor 97 non-PLAYED goldens pinned by name; sweep cost 129s wall clock
    JUnit 975 tests, floor 239  (the backend's own suite, green with a non-zero count)

Gate 15 ook `check-graaf: route leg — the graph of the wire equals the graph of the source on all 115 goldens with a
bron.rgs (floor 98); 23 unit tests green (floor 23)` en `spec leg — 251 of 288 goldens compared, 181 dependencies equal
to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)`. Gate 11 ook `re-emission: 1 casus dump(s), 45
.rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27`.

Eerder groen: alle gates in de toelating van de golden (`scripts/admit.sh`, met de golden, het gate-12-voorstel en de
README in de werkboom, op de code van `4916cc6b`).

### Afwijkingen van de kaart

1. De herschaling op de schrijfgrens geldt voor elke toekenning, niet alleen voor een met een vouw. De kaart vraagt de
   weg van een regelwaarde, en die herschaalt naar het doel; zonder vouw schreef de creatie 2 waar ref 2000 g schrijft
   (f-eenheid-kaal, een stil fout antwoord van vóór deze kaart). Rood-eerst getuigenrij in `290c465b`; de sweep
   verschuift geen regel.
2. Een deling of wortel in een toekenning weigert nu met de vorm in de zin. Zonder begeleider antwoordde de creatie
   `consistent` waar ref `inconsistent` zegt (drie h-vormen, ook zonder vouw). De omheining is die van de consequentie
   van een default (`foutplekSoort`) en vraagt niet of de deler leeg kan zijn: b-gedeeld-som en b-afgerond (gedeeld door
   een literaal) en h-deling-door-lege-som-consistent speelden met de goede waarde en weigeren nu ook. Geen golden en
   geen bestaande getuige draagt de vorm (sweep, suite).
3. De weigering van de voorwaarde van een creatie noemt nu de vorm (`voorwaardeSkipReden`, zoals een regel), en twee
   weigerzinnen over de ordenende datumvouw noemen de drie plekken waar de tak haar verlaagt. Alleen
   `een ordenende vouw over een datumbron` was gepind (`DatumvouwOverRolWitnessTest`) en staat er nog; geen statusregel
   van een golden draagt de zinnen.
4. De getuige heeft meer rijen dan R1–R3: de vormen van (a) die de lowering raken, de schrijfgrens, en drie rijen die een
   benoemde weigering pinnen.
5. De golden draagt niet elke vorm die speelt: de herschaling naar een eenheid, een wortel en een subselectie die een lid
   vergelijkt vertaalt of grondt clingo niet; die staan in de getuige (de eerste) of alleen in (a).
6. De kaart noemt de pinnen 229/191; bij de claim stonden ze op 232/191, het voorstel is 233/191.
7. Geen bestaand testbestand en geen bestaande golden gewijzigd.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 5.5, onder "Recorded flank", een nieuwe bullet: "*Recorded (measured 2026-10-01, card
  `drools-exec-objectcreatie-vouw`, open).* A creation reads stratum-strict (RUNTIME-SEMANTICS ch. 6.1): it evaluates
  strictly above the producers of what its condition and its toekenningen read. Both Drools branches fire an object
  creation once per subject, at the default salience, after the ordinary rules and before the fact creations and (in
  the exec-model branch) the defaults groups, so a producer that comes later on the agenda is not seen: with a toekenning
  that reads a value a defaults group writes, the exec-model branch writes leeg where `ref/` and `backends/clingo` write
  7 and 15; with a count over the objects a creation declared after it mints, or over a relation a fact creation lays,
  both branches write 0 where they write 1. Neither branch refuses these documents, so on this flank the backend realises
  a coarser order than the graph without a loud refusal."
- `spec/CONSTRUCTS.md` rij 6, "Recorded open", aan het eind: "…; and both Drools branches fire an object creation before
  a later writer of what it reads — a defaults group (exec-model branch; the DRL-text branch refuses the document), a
  creation declared after it, a fact creation — where RUNTIME-SEMANTICS ch. 6.1 places it strictly above them (IR-SPEC
  ch. 5.5)."
- Geen tekst voor de vouw zelf: RUNTIME-SEMANTICS ch. 5.5 ("values evaluated on the subject") en ch. 1.2/1.2e schrijven
  haar al voor; de exectak voldoet er nu aan.

### Open punten

1. De creatie vuurt vóór een latere schrijver van wat zij leest (f-lid-door-default, f-eigen-door-default,
   f-creatie-keten, f-feitcreatie-keten, tabel (a)): een defaultsgroep (de groepen staan na de creaties in het model en
   op een salience ≤ 0), een creatie die na haar gedeclareerd is, een feitcreatie (na de objectcreaties in het model);
   zij vuurt één keer per subject en ziet de latere waarde niet. RUNTIME-SEMANTICS ch. 6.1 plaatst een creatie strikt
   boven de schrijvers van wat zij leest, en IR-SPEC ch. 5.5 verbiedt een stille verkeerde volgorde. Vóór deze kaart
   gold dit al voor een scalaire toekenning en voor de getalvouwen via `lowerVal`; nu ook voor de datumvouwen, die
   daarvoor weigerden. Herstel vraagt de creatie als knoop in de ordening (`groepVolgorde`: onder de band van haar
   schrijvers, en de feit- en objectcreaties onderling op hun lees-schrijfrelatie), of een benoemde weigering zolang die
   er niet is. Kandidaat voor een eigen kaart; de beslissing (ordenen of weigeren) ligt bij de eigenaar.
2. Een begeleider voor de creatie (§6.5/§6.6) zou de geweigerde delingen en wortels laten spelen. Daarnaast, buiten de
   creatie gemeten: een deling door een lege vouw in een gewone regelwaarde meldt de inconsistentie ook niet, in beide
   takken (`/tmp/v2/probe/ocv-bouw/extra/r-deling-door-lege-som`: "(100 gedeeld door (de som …)) rekenkundig afgerond op 2
   decimalen", ref en clingo `PASS (5 expectations)` met `inconsistent`, exectak en DRL-tak `FAIL … verwacht
   inconsistent, werkelijk consistent`): `kwalificeertOperand` kwalificeert alleen een eigen of genavigeerd attribuut als
   operand van een begeleider. Een stil fout antwoord van vóór deze kaart, in een gewone regel; kandidaat voor een kaart.
3. Meer dan vier ordenende vouwen (of minder, met globals) in één creatie binden meer dan negen variabelen en weigeren
   met naam (`te veel gebonden variabelen: globals en vouwen`), zoals in een defaultsgroep (`Block10`).
4. De ASP-backend: een subselectie die een lid vergelijkt ("die aan de volgende voorwaarde voldoen: zijn gewicht is
   groter dan 5") in de toekenning van een creatie grondt niet (`error: unsafe variables … 'S' is unsafe`, `grounding
   stopped because of errors`, f-subselectie); een herschaling onder een initialisatie en een afgeronde wortel vertaalt
   zij niet (`TRANSLATION FAULT`). Luid, buiten deze kaart (`backends/clingo/`).
5. De DRL-teksttak, ongewijzigd: zij markert een datumvouw in een toekenning, laat de creatie over een lege bron niet
   vuren waar een toekenning een minimum of maximum draagt (b-pct-minimum/maximum, b-plus-minimum/maximum,
   d-kaal-leeg-en-constante, d-twee-toekenningen, e-meervoudig-min-plus: `werkelijk leeg`), schrijft zonder herschaling
   naar de eenheid van het lid (f-eenheid, f-eenheid-kaal) en leest een deling door leeg als consistent. Herstel raakt
   `DrlTranspiler.objectCreatie`.
6. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee.

### Commits

    c045f500 claim: drools-exec-objectcreatie-vouw
    290c465b ObjectcreatieVouwWitnessTest: vouw in de toekenning van een creatie, rood
    4916cc6b Exectak: een vouw in de toekenning van een creatie wordt een accumulate
    862e60ea golden jutterswet-afrekening: vouwen in de toekenning van een creatie
    1b331e97 voorstel gate 12: jutterswet-afrekening speelt in de exectak
    a7db211b backends/drools/README: een vouw in de toekenning van een creatie
    bc65caa4 Exectak: twee lange regels in de lowering van de creatie gebroken

en de commit die de kaart met dit Result naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.

## Keuring (hoofdsessie, 2026-10-01)

Eigen run van de zestien gates op `9fe4956a` in de volgorde van `gates.yml`
(bash waar de shebang dat zegt), `dune build` vooraf; logs in
`/tmp/v2/uchar-gates14/`. Alle zestien exit 0: gate 10 JUnit 1008, 115
rgs-goldens; gate 12 `check-drools: PASS`, played 233 (pin 233), DRL 191
(pin 191), JUnit 975; gate 16 22 scenario-goldens, 80 casebestanden. De fix
gelezen: `objectCreatie` verlaagt elke toekenning met `scalarWaarde` terwijl
`vouwslots` en `vouwOnderwerp` aan staan, de vouwen van alle toekenningen
in één lijst, hun accumulates na de idempotentiewacht en `vulVouwslots`
vóór het rekenen; `lowerVal` en de aggregatiehelft van `TkLower` zijn weg.
Afwijking 1 aanvaard: de herschaling op de schrijfgrens voor elke
toekenning is de weg van een regelwaarde en haalt een stil fout antwoord
weg (2 waar 2000 g hoort). Afwijking 2 aanvaard met aantekening: een
deling of wortel in een toekenning weigert nu benoemd, omdat een creatie
geen begeleider heeft die de Fout meldt; drie vormen die met de goede
waarde speelden weigeren mee, geen golden of getuige droeg ze; een
begeleider voor de creatie is een vervolg (open punt 2). Afwijking 3
aanvaard. De voorgestelde registertekst is in dit landingscommit gezet in
`spec/IR-SPEC.md` ch. 5.5 (eigen recorded-alinea vóór de flank van de
ASP-backend) en `spec/CONSTRUCTS.md` rij 6. Open punt 1 (een creatie vuurt
vóór een latere schrijver van wat zij leest: een defaultsgroep, een later
gedeclareerde creatie, een feitcreatie; leeg of 0 waar ref een waarde
geeft; van vóór deze kaart) en open punt 2 (een deling door een lege vouw
in een gewone regelwaarde meldt de inconsistentie in geen van beide takken)
zijn stille foute antwoorden en kandidaten voor eigen kaarten op het sein
van de eigenaar; de keuze ordenen of weigeren voor punt 1 ligt bij hem.
Open punten 3–6: geen kaart. Commitkoppen tot 73 tekens aanvaard (ff-only,
geen rebase).

## Vervolgronde (2026-10-01, bouwer)

Eén afgebakende ronde op drie vondsten van dit Result: open punt 2 (tweede helft, dan eerste helft) en open punt 1. Tak
`vervolg/objectcreatie-vouw` in een nieuwe worktree, afgetakt van main `98d79e12` (deze kaart geland als `b784bd98`,
daarna overlappende-voorwaardelijke-schrijvers en twee frontendkaarten); niet gerebased, niet gepusht, niet gemerged.
Gemeten met de eigen uitvoerders van deze repo (ref, clingo, verifier, frontend, Drools-jar, `--drl` voor de
teksttak). "Vóór" is de jar van `98d79e12`, "na" de jar van de kop. Metingen, generatoren en hulpmiddelen staan onder
`/tmp/v2/probe/ocv-vervolg/` (`maak1.py` en `p1/` voor punt 1, `maak.py` en `vorm/` voor de vormruimte van de kaart met
de nieuwe flanken, `dump-getuige.sh` voor de documenten van de getuigen, `tool/ToonSalience.java` voor de saliences);
de reproductie van punt 1 is `/tmp/v2/probe/ocv-vervolg/extra/r-deling-door-lege-som` (kopie van die onder
`ocv-bouw`). Elke rij van de drie getuigen is naast de getuige gemeten: verifier-schoon, ref het verwachte antwoord,
clingo hetzelfde behalve waar het staat.

### Punt 1: een deling of wortel met een vouw als operand in een regelwaarde

Oorzaak. Een §6.5-deling of §6.6-wortel in een regelwaarde meldt haar Fout via een begeleider: een aparte regel die
toetst of het deeltal gevuld en de deler leeg is (of de radicand negatief). In beide takken kwalificeerde een operand
alleen als eigen of genavigeerd attribuut (`ExecModelTranspiler.kwalificeertOperand`/`companionOperand`,
`DrlTranspiler.companionDeel`); een vouw, ook in een samengestelde operand, kreeg geen begeleider en de case bleef
`consistent` waar ref en clingo de Fout op het kanaal zetten (een lege som of een leeg minimum is leeg, ch. 1.2e).

Fix. Exectak (`a7ef6b8c`): `vouwCompanion` voor een plek buiten elke ch. 4.10-knoop waarvan een operand een vouw
draagt. De begeleider verlaagt de operanden zoals de gastregel (`scalarWaarde` met vouwslots: elke vouw een eigen
accumulate met de telling gebonden, lege bron leeg, telling 0), met de joins die de gastregel voor een genavigeerde
lezing neemt, onder de voorwaarde en het geldigheidsvenster van de gastregel, en beslist in zijn consequentie (gevuld
deeltal en lege deler; radicand kleiner dan 0). Hij rekent de operanden en niet de deling, dus een deler 0 blijft de
fout van de gastregel bij het vuren (ref: `division by numeric zero`, clingo dezelfde guard). DRL-teksttak
(`36d2aea2`): dezelfde begeleider in tekst: de vouw als accumulate in de eigen when van de begeleider (de telling
gebonden via `Acc.telLeeg`, zodat ook een leeg minimum een rij geeft), de toets in een `eval`, `@watch` op de eigen
lezingen. Een deling of wortel onder een Let of keuze die ook een vouw draagt, weigert de teksttak met de zin van de
exectak (`regel (een vouw over een setbron en een deling of wortel onder één Let of IfThenElse, ch. 4.10)`): haar
begeleider leest behoeften en takken als patroon en een vouw heeft geen patroonvorm; de exectak weigerde die vorm al.

| vorm (regel "heffing") | ref | clingo | exectak vóór → na | DRL-tekst vóór → na |
|---|---|---|---|---|
| r-som-deler: (100 gedeeld door (de som …)) afgerond | `PASS (5)` | `PASS (5)` | FAIL `verwacht inconsistent, werkelijk consistent` → `PLAYED 1` | idem → `PLAYED 1` |
| r-min-deler: 100 gedeeld door het minimum | `PASS (5)` | `PASS (5)` | FAIL (idem) → `PLAYED 1` | FAIL (idem) → `PLAYED 1` |
| r-som-deeltal: (de som …) gedeeld door zijn deler | `PASS (5)` | `PASS (5)` | FAIL (idem) → `PLAYED 1` | FAIL (idem) → `PLAYED 1` |
| r-som-vm-deler: 100 gedeeld door ((de som …) verminderd met 1) | `PASS (5)` | `PASS (5)` | FAIL (idem) → `PLAYED 1` | FAIL (idem) → `PLAYED 1` |
| r-wortel-som: de wortel van ((de som …) min 10) | `PASS (5)` | `TRANSLATION FAULT` (afgeronde wortel) | FAIL (idem) → `PLAYED 1` | FAIL (idem) → `PLAYED 1` |
| r-som-deler-voorwaarde (J2 met drempel) | `PASS (5)` | `PASS (5)` | FAIL (idem) → `PLAYED 1` | FAIL (idem) → `PLAYED 1` |
| r-som-deler-voorwaarde-consistent (J2 weg) | `PASS (4)` | `PASS (4)` | `PLAYED 1` → `PLAYED 1` | `PLAYED 1` → `PLAYED 1` |
| r-som-deler-gevuld (alleen J1, J4) | `PASS (3)` | `PASS (3)` | `PLAYED 1` → `PLAYED 1` | `PLAYED 1` → `PLAYED 1` |
| r-aantal-deler: 100 gedeeld door het aantal (J2: 0) | `EVALUATION FAULT: division by numeric zero` | `UNSATISFIABLE … viol("division by numeric zero","J2")` | `ERROR … ArithmeticException: Breuk deling door 0` → idem | `ERROR … cannot invoke method: rsWaarde` → idem |
| r-let-som-deler: Daarbij geldt: de vangst is de som; 100 gedeeld door de vangst | `PASS (5)`, consistent | `PASS (5)` | SKIP (Let-zin) → idem | `PLAYED 1` → MARKER (Let-zin) |
| r-let-som-deeltal: … de vangst gedeeld door zijn deler | `PASS (5)`, inconsistent | `PASS (5)` | SKIP (Let-zin) → idem | FAIL (idem) → MARKER (Let-zin) |

Bij r-let-som-deler is de case consistent: een lege vangst beëindigt de Let vóór de deling (ch. 10.6), dus ref zet
niets op het kanaal; de teksttak speelde haar goed en weigert haar nu mee, want zij kan r-let-som-deeltal (gevulde
vangst, lege deler) niet onderscheiden en antwoordde daar consistent.

Getuige: `VouwBegeleiderWitnessTest` (negen rijen, beide takken): de lege som en het lege minimum als deler, de som als
deeltal, een vouw in een samengestelde deler, de wortel van een negatieve som (ref alleen), de voorwaarde van de
gastregel (inconsistent met J2, consistent zonder), de Let-weigering in beide takken, en twee flanken (een gevulde som
meldt niets; een deler 0 faalt in elke uitvoerder). Rood op main (`040f287d`): `Tests run: 8, Failures: 6`, elk
`verwacht inconsistent, werkelijk consistent`; de flanken groen. De Let-rij (`827d72a8`) kwam pas na de fix van de
teksttak en is rood gemeten tegen de teksttak van `a7ef6b8c`: `Expected … Excluded to be thrown, but nothing was
thrown`.

### Punt 2: een deling of wortel in de toekenning van een objectcreatie

Oorzaak. Een creatie had geen begeleider, en de kaart weigerde daarom een deling of wortel in een toekenning met naam
(`kanaal-deponerende toekenning`), ook waar de deler een literaal of een gevulde som is.

Fix (`bac200db`). Geen aparte begeleiderregel: een creatie rekent haar toekenningen één keer, in de consequentie die
munt, onder haar eigen idempotentiewacht. Die evaluatie krijgt de sonde (`Sonde`) die de deling- en wortelarm van
`scalarWaarde` al voeden; meldt een plek een Fout, dan voegt dezelfde consequentie de inconsistentie in voor de
creatie en haar subject, zoals ref (`World.insert_error` met de creatie als regel). Zo geldt de melding precies onder
de wacht en in dezelfde vuring die de wacht onwaar maakt. De omheining met `foutplekSoort` is weg uit `objectCreatie`.

| vorm (vormruimte van de kaart) | ref | clingo | exectak vóór → na | DRL-tekst (vóór = na) |
|---|---|---|---|---|
| h-deling-eigen-door-leeg | `PASS (5)` | `PASS (5)` | SKIP (kanaal-deponerende toekenning) → `PLAYED 1` | FAIL `verwacht inconsistent, werkelijk consistent` |
| h-deling-door-lege-som | `PASS (5)` | `PASS (5)` | SKIP → `PLAYED 1` | FAIL (idem) |
| h-deling-door-lege-som-consistent | `PASS (3)` | `PASS (3)` | SKIP → `PLAYED 1` | `PLAYED 1` |
| h-wortel-van-een-negatieve-som | `PASS (5)` | `TRANSLATION FAULT` (afgeronde wortel) | SKIP → `PLAYED 1` | FAIL (idem) |
| b-gedeeld-som: (de som …) gedeeld door 4 | `PASS (4)` | `PASS (4)` | SKIP → `PLAYED 1` | `PLAYED 1` |
| b-afgerond: ((de som …) gedeeld door 3) afgerond | `PASS (4)` | `PASS (4)` | SKIP → `PLAYED 1` | `PLAYED 1` |

Getuige: in `ObjectcreatieVouwWitnessTest` is de rij `een_deling_in_een_toekenning_weigert_met_de_vorm` vervangen door
`een_deling_of_wortel_in_een_toekenning_meldt_de_fout` (lege deler, lege som, gevulde som, literaal, wortel; ref en
clingo, de wortel ref alleen), en de klassecommentaar zegt dat. Rood tegen de omheining (`4f50cd9a`): `Tests run: 15,
Errors: 1` met `Excluded … kanaal-deponerende toekenning (deling)`. Een gepinde uitkomst van de eigen getuige die
terecht verandert; geen golden droeg de vorm.

### Punt 3: een creatie vóór een latere schrijver van wat zij leest

Oorzaak. Een creatie leest stratum-strict (RUNTIME-SEMANTICS ch. 6.1) en munt één keer. De exectak voegde de
creaties toe na de regels en vóór de feitcreaties en de defaultsgroepen, zonder salience; Drools vuurt één salience in
de volgorde waarin de regels zijn toegevoegd. Een defaultsgroep (salience 0 of lager, later toegevoegd), een regel die
de stratificatie lager zet (een ontkenning), een later gedeclareerde creatie en een feitcreatie vuurden daardoor na de
creatie, en de creatie schreef wat zij toen las: leeg of 0.

Fix (`963094c7`). De object- en feitcreaties zijn knopen in `groepVolgorde`, naast de regels en de groepen. Een
objectcreatie leest haar voorwaarde, haar toekenningen en de populatie van haar onderwerp, en schrijft haar
toekenningen op het gemaakte type, de populatie daarvan en haar koppeling (de connectingRole, anders het ene
verbindende feittype); een feitcreatie leest haar voorwaarde (over anker en doel), de populatie van haar anker en de
koppelingen van haar hops, en schrijft haar koppeling en de populatie van het doel (de tabel van ch. 6.1). De
lezingen kregen daarvoor twee soorten: `populatie(type)` (een vouw of kwantor over de instanties, de iteratie van een
creatie) en `relatie(feittype)` (een vouw over een rol, een navigatie, een kenmerk over een rol, een hop); alleen
creaties schrijven ze, dus in een document zonder creatie verandert niets. Een creatie zakt onder elke knoop waarop
zij (transitief) wacht en die anders na haar vuurt: lager in salience, of gelijk en later toegevoegd (`laadrang`). Een
knoop op een kring met de creatie (een recursieve creatie, ch. 5.5) telt niet mee; die blijft op haar generaties. Een
groep of regel die leest wat een creatie schrijft, wacht via de band en de negatieve vouwlezingen op haar. De
salience komt op de regel van de creatie (`creatieSalience`).

| flank (vormruimte) | ref | clingo | exectak vóór → na | DRL-tekst (vóór = na) |
|---|---|---|---|---|
| f-eigen-door-default: telling = zijn basis, basis uit een defaultsgroep | `PASS (4)` | `PASS (4)` | FAIL `verwacht 7, werkelijk leeg` (+3) → `PLAYED 1` | EXCLUDE (defaults) |
| f-lid-door-default: de som van het nettogewicht uit een groep | `PASS (4)` | `PASS (4)` | FAIL `verwacht 15, werkelijk leeg` (+2) → `PLAYED 1` | EXCLUDE (defaults) |
| f-lid-door-ontkenning: het aantal lichte vondsten, licht = niet zwaar | `PASS (4)` | `PASS (4)` | FAIL `verwacht 2, werkelijk 0` (+1) → `PLAYED 1` | FAIL (idem) |
| f-creatie-keten: het aantal netten, gemunt door een latere creatie | `PASS (4)` | `PASS (4)` | FAIL `verwacht 1, werkelijk 0` (+3) → `PLAYED 1` | FAIL (idem) |
| f-feitcreatie-keten: het aantal gecontroleerde netten uit een feitcreatie | `PASS (4)` | `PASS (4)` | FAIL `verwacht 1, werkelijk 0` (+3) → `PLAYED 1` | FAIL (idem) |
| f-lid-later-afgeleid (flank): een gewone regel na de creatie | `PASS (4)` | `PASS (4)` | `PLAYED 1` → `PLAYED 1` | `PLAYED 1` |
| f-lezer-onder-de-creatie: een kenmerk over het aantal afrekeningen | `PASS (8)` | `PASS (8)` | FAIL `kenmerk onafgerekend: verwacht afwezig, werkelijk aanwezig` (+3) → idem | EXCLUDE (defaults) |

Getuige: `CreatieVolgordeWitnessTest` (zes rijen, exectak): de vijf rode flanken en de gewone regel na de creatie als
flank. Rood op main (`86d80904`): `Tests run: 6, Failures: 5`, met de zinnen hierboven; de flank groen.

De saliences van alle regels van de 27 goldens met een object- of feitcreatie zijn vóór en na gelijk
(`tool/ToonSalience.java` op beide jars; vier van de 27 bouwen in beide niet, met dezelfde weigering): geen golden
krijgt een andere volgorde.

### Sweep en pinnen

Over alle 289 goldens van main, elke golden met al zijn cases, statusregel per golden en per tak, jar van `98d79e12`
tegen de jar na elk punt (na punt 1, na punt 2, na punt 3): alle 578 regels gelijk, exectak PLAYED 234, DRL-tak PLAYED
192. Geen golden draagt een deling of wortel met een vouw als operand (in een regel, kaal of onder een Let), geen golden
een deling in een toekenning, en geen golden krijgt een andere salience. De pinnen blijven `EXPECTED_PLAYED=234` en
`EXPECTED_DRL_PLAYED=192`; er is geen voorstel voor `scripts/check-drools.sh` en geen nieuwe golden (de drie punten
zijn met getuigen gepind; zie open punt 4).

### README (`8d21b311`)

`backends/drools/README.md`, sectie "Recorded: a fold over a set source inside a rule's value or a creation's
toekenning": een alinea over de begeleider met een vouw als operand in beide takken en de Let-weigering in de teksttak;
in de alinea over de creatie speelt een deling of wortel nu met de Fout-melding; een nieuwe alinea zegt dat een creatie
in `groepVolgorde` onder de schrijvers van wat zij leest staat en wat open blijft; de alinea over de DRL-teksttak zegt
dat die tak geen creatie ordent.

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates19/run.sh` op de kop `e7b683ec` (de laatste
codecommit; de commit met dit Result raakt alleen de kaart), `dune build` exit 0, elk script onder zijn shebang; logs in
`/tmp/v2/uchar-gates19/`. Alle zestien exit 0. Gate 10: `JUnit suite green (1044 tests, floor 173); 116 rgs golden(s)
re-emit byte-identically`. Gate 12: `check-drools: PASS`, `played 234` (pin 234), `DRL-text branch played 192` (pin
192), `JUnit 1000 tests, floor 239`. Gate 16: `23 scenario golden(s) (floor 7) regenerate 82 case file(s)`.

### Afwijkingen van de opdracht

1. Punt 2 bouwt geen aparte begeleiderregel maar laat de creatie de Fout van haar eigen evaluatie melden, met de sonde
   die de deling- en wortelarm al voeden. Een aparte regel zou precies vóór de creatie moeten vuren, onder dezelfde
   wacht; de creatie zelf rekent die toekenningen één keer en is daarmee precies de evaluatie die ref doet.
2. Punt 1 in de teksttak weigert een deling of wortel onder een Let of keuze die ook een vouw draagt, met de zin van
   de exectak. Eén vorm die de teksttak goed speelde, gaat daardoor van `PLAYED` naar een benoemde weigering
   (r-let-som-deler, consistent omdat een lege vangst de Let beëindigt); de tak kan haar niet onderscheiden van
   r-let-som-deeltal, waar zij consistent antwoordde en ref inconsistent zegt. Geen golden draagt de vorm.
3. De rij die de omheining van de kaart pinde (`een_deling_in_een_toekenning_weigert_met_de_vorm`) is vervangen door
   de rij die de Fout-melding pint: een gepinde uitkomst die de fix terecht verandert.
4. De Let-rij van `VouwBegeleiderWitnessTest` kwam na de fix van de teksttak en is daarna rood gemeten tegen de
   teksttak van de commit ervoor; de overige rijen zijn vóór hun fix rood gecommit.
5. Drie commitkoppen waren langer dan 72 tekens en zijn vóór dit Result herschreven met `git filter-branch
   --msg-filter` (niet gepusht; boom ongewijzigd, `git diff` tussen oud en nieuw leeg); de hashes hieronder zijn de
   nieuwe.

### Wat open blijft, en waarom

1. Exectak: een lezer van de uitvoer van een creatie die onherroepelijk schrijft — een kenmerktoekenning over het
   aantal gemaakte objecten — wordt niet onder de creatie gezet en kan op de voorlopige afwezigheid handelen
   (f-lezer-onder-de-creatie: `kenmerk onafgerekend: verwacht afwezig, werkelijk aanwezig`, vóór en na gelijk). De
   stratificatie zet een regel alleen onder de schrijvers van een kenmerk dat zij ontkend leest en van wat een vouw in
   haar negatief leest; een kenmerktoekenning die een telling positief leest valt daarbuiten, ook zonder creatie.
   Herstel is een ordening van de kenmerktoekenningen als stratum-strikte lezers (ch. 6.1) in `groepVolgorde`, buiten
   de drie punten van deze ronde.
2. DRL-teksttak, ongewijzigd: zij ordent geen creatie (f-creatie-keten, f-feitcreatie-keten en f-lid-door-ontkenning
   antwoorden 0 waar ref 1 en 2 geeft; een document met een defaultsgroep weigert zij), en een deling door leeg in de
   toekenning van een creatie leest zij als consistent (h-deling-*). Beide vragen in die tak wat de exectak nu doet:
   saliences voor de creaties uit haar eigen stratificatie, en een melding in de `then` van de creatie.
3. De begeleider met een vouw dekt een plek buiten elke ch. 4.10-knoop; onder een Let of keuze met een vouw weigeren
   beide takken benoemd (punt 1). Een samengestelde operand zonder vouw (een parameter, een genest product) kwalificeert
   nog steeds niet voor een begeleider: dat is de bestaande, in `kwalificeertOperand` vastgelegde grens.
4. Geen golden in deze ronde: elk van de drie punten is gepind door een getuige met de waarden van ref (en clingo,
   behalve de afgeronde wortel), en een golden zou `EXPECTED_PLAYED` laten stijgen. Een golden met een deling door een
   lege vouw in een regel en een creatie die op een defaultsgroep wacht, speelt in ref, clingo en de exectak (gemeten op
   de vormen hierboven) en kan bij de landing volgen als de eigenaar dat wil.
5. De distributie-CLI draait op een oudere pin; de pin moet na het landen mee.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 5.5, de alinea "*Recorded (measured 2026-10-01, card `drools-exec-objectcreatie-vouw`,
  open).*" vóór "*Recorded flank, the ASP backend …*" wordt: "*Recorded (measured 2026-10-01, card
  `drools-exec-objectcreatie-vouw`; closed in the exec-model branch the same day, open in the DRL-text branch).* A
  creation reads stratum-strict (RUNTIME-SEMANTICS ch. 6.1): it evaluates strictly above the producers of what its
  condition and its toekenningen read. Both Drools branches fired an object creation once per subject, at the default
  salience, after the ordinary rules and before the fact creations and (in the exec-model branch) the defaults groups,
  so a producer later on the agenda was not seen: with a toekenning that reads a value a defaults group writes, the
  exec-model branch wrote leeg where `ref/` and `backends/clingo` write 7 and 15; with a count over the objects a
  creation declared after it mints, or over a relation a fact creation lays, both branches wrote 0 where they write 1.
  *Closed in the exec-model branch on 2026-10-01 (witness `CreatieVolgordeWitnessTest`):* the object and the fact
  creations are nodes of the order that already placed the defaults groups; a creation reads its condition, its
  toekenningen, the population of its subject and the links its folds, quantifiers, navigations and hops follow, writes
  its toekenningen, the created type's population and its link, and goes below every node it waits on that would
  otherwise fire after it, a rule the stratification places lower and a creation declared after it included; a
  recursive creation stays on its generations (RUNTIME-SEMANTICS ch. 5.5). What stays open: in the exec-model branch a
  reader of a creation's output that writes irrevocably, such as a characteristic assignment over a count of the
  created objects, is not placed below the creation and can act on the transient absence (measured: a characteristic
  attributed where `ref/` and `backends/clingo` leave it absent); the DRL-text branch orders no creation, so a later
  creation and a fact creation still read 0 there, a coarser order than the graph without a loud refusal."
- `spec/CONSTRUCTS.md` rij 6, "Recorded open": het deel "; and both Drools branches fire an object creation before a
  later writer of what it reads — a defaults group (exec-model branch; the DRL-text branch refuses the document), a
  creation declared after it, a fact creation — where RUNTIME-SEMANTICS ch. 6.1 places it strictly above them (IR-SPEC
  ch. 5.5)." wordt "; the DRL-text branch fires an object creation before a later creation or a fact creation that
  writes what it reads, where RUNTIME-SEMANTICS ch. 6.1 places it strictly above them, and in the exec-model branch,
  which places a creation below those writers since 2026-10-01, a reader of a creation's output that writes
  irrevocably can still act before the creation (IR-SPEC ch. 5.5)."
- Voor punt 1 en 2 geen registertekst: RUNTIME-SEMANTICS ch. 1.2 en 5.6 schrijven de melding voor; de README van de
  Drools-backend legt vast hoe de takken haar dragen.

### Commits

    040f287d VouwBegeleiderWitnessTest: deling door een lege vouw in een regel, rood
    a7ef6b8c Exectak: een begeleider leest een vouw als operand van een deling
    36d2aea2 DRL-teksttak: een begeleider leest een vouw als operand van een deling
    827d72a8 VouwBegeleiderWitnessTest: een vouw onder een Let naast een deling
    4f50cd9a ObjectcreatieVouwWitnessTest: deling in toekenning meldt de Fout, rood
    bac200db Exectak: een creatie meldt de Fout van haar eigen toekenning
    86d80904 CreatieVolgordeWitnessTest: creatie vóór een latere schrijver, rood
    963094c7 Exectak: een creatie wacht op de schrijvers van wat zij leest
    8d21b311 backends/drools/README: vouw in begeleiders, Fout en volgorde creaties
    e7b683ec Exectak: commentaar bij de sonde van een creatie en bij groepVolgorde

en de commit "Vervolgronde objectcreatie-vouw: Result", die deze sectie toevoegt. Niet gepusht, niet gemerged.
