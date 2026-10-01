# drools-exec-voorwaarde-twee-rollen-ver: een voorwaarde die twee rollen ver leest, of één rol ver met een datumrekensom, weigert in de exec-tak

*Vondst van een regelsetbouwer, 2026-09-30, opnieuw gemeten door de
kaartschrijver op uchar `b0aab696` via de distributie `f24283cf`
(`bin/regelspraak check|scenario|cases|drl`) en de eigen uitvoerders (ref en
clingo gebouwd uit de boom van `b0aab696`); reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/kaarten-g1/voorwaarde-twee-rollen/`
(minimaal/r1–r6.rgs met hun scenario's en meting-*.txt; de vormruimte als
één map per vorm met bron.rgs, scenario.rgs en meting.txt, gemaakt door
genereer.py; alleen lezen). Luid (exit 3), geen fout antwoord; wel een
hulplid in elke regelset die een datum twee rollen ver toetst. Op verzoek
van de eigenaar geschreven (2026-09-30); vervolg op
drools-exec-voorwaarde-genavigeerde-afleiding (`backlog/done`, één rol ver).*

## Aanleiding

De voorganger liet een voorwaarde die via één to-one rol leest in de
exec-tak spelen: dezelfde join per navigatie als de waarde (`navJoins`) en
de toetsen op het onderwerp dat daarna opnieuw gebonden wordt
(`from $subject`). Haar open punt 4 liet een voorwaarde over twee of meer
rollen bij de weigering "voorwaarde buiten Phase-B scope", en
`backends/drools/README.md` registreert: "A condition over two or more hops
is refused by name, except the whole-period leeg and gevuld test."

Gemeten op 49 vormen (tabel onder (a)):

- twee rollen ver weigert de exec-tak elke vorm: gevuld, leeg, een
  vergelijking zonder rekensom en een rekensom aan elke kant, voor een
  invoerwaarde en een afgeleide waarde gelijk;
- één rol ver komt dezelfde zin zodra de vergelijking een datumrekensom
  draagt (een datum plus of min een duur, ook met een parameter als duur),
  aan de genavigeerde en aan de eigen kant: "zijn vinddag min 6 mnd later is
  dan de vergunningsdag van de jutter van de Vondst". Een numerieke rekensom
  één rol ver speelt (met een literal of een parameter; zo ook rij a8 van de
  voorganger), en een datumrekensom zonder navigatie speelt;
- in de waarde van een regel spelen één en twee rollen, ook met een
  datumrekensom.

ref en clingo spelen alle 49 vormen met de handgerekende uitkomst. De
DRL-teksttak (alleen de lowering gemeten, met `drl`) lowert één rol met een
datumrekensom en weigert twee rollen met "drl-transpiler MARKER: regel:
uitkomst — TODO voorwaarde: <voorwaarde>". Een regelset die een datum twee
rollen ver toetst, of één rol ver tegen een datum plus een duur, schrijft nu
een hulplid: een gewone regel die de waarde naar het onderwerp haalt (de
lezing in de waarde speelt) en een voorwaarde die dat lid leest (R6).

## Minimale reproductie

`b0aab696`, distributie `f24283cf` (`bin/regelspraak check|scenario|cases`);
ref via `_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`, beide gebouwd op
`b0aab696`.

    Eenheidsysteem Tijd
        de dag dg
        de maand mnd

    Objecttype de Haven (mv: Havens)
        de openingsdag Datum in dagen;
        de afgeleide openingsdag Datum in dagen;
    Objecttype de Jutter (mv: Jutters) (bezield)
        de vergunningsdag Datum in dagen;
    Objecttype de Vondst (mv: Vondsten)
        de vinddag Datum in dagen;
        de havenopening Datum in dagen;
        de uitkomst Datum in dagen;
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten
    Feittype HavenHeeftJutter
        de haven Haven
        de havenjutter (mv: havenjutters) Jutter
    Eén haven heeft meerdere havenjutters

    Regel afgeleide openingsdag
        geldig altijd
            De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag.

    Regel uitkomst
        geldig altijd
            De uitkomst van een Vondst moet gesteld worden op zijn vinddag
            indien zijn vinddag later is dan de afgeleide openingsdag van de haven van de jutter van de Vondst.

    Scenario: Haven H (openingsdag 01-01-2026); Jutter J (vergunningsdag 01-03-2026, haven H);
    Vondst V (vinddag 15-09-2026) en Vondst W (vinddag 15-12-2025), beide van J; verwacht per R.

    R1  r1.rgs, zoals hierboven (twee rollen, vergelijking zonder rekensom, afgeleide waarde);
        verwacht V 15-09-2026, W leeg.
        nu: check "niet uitvoerbaar in uchar (exit 3): transpiler SKIP: regel: uitkomst — regel
        (voorwaarde buiten Phase-B scope): uitkomst"; scenario "MARKER  r1" met dezelfde zin; ref
        "oracle: PASS (2 expectations)"; clingo "clingo: PASS (2 expectations)".
    R2  r2.rgs: `indien de openingsdag van de haven van de jutter van de Vondst gevuld is` (twee
        rollen, invoerwaarde); verwacht V 15-09-2026, W 15-12-2025.
        nu: dezelfde zin, exit 3; ref en clingo PASS.
    R3  r3.rgs: `indien zijn vinddag min 6 mnd later is dan de vergunningsdag van de jutter van de
        Vondst` (één rol, datumrekensom aan de eigen kant); verwacht V 15-09-2026, W leeg.
        nu: dezelfde zin, exit 3; ref en clingo PASS.
    R4  r4.rgs: `indien zijn vinddag later is dan de vergunningsdag van de jutter van de Vondst`
        (één rol, zonder rekensom).
        nu: check exit 0; scenario "PASS  r4"; ref en clingo PASS.
    R5  r5.rgs: `De uitkomst van een Vondst moet gesteld worden op de afgeleide openingsdag van de
        haven van de jutter van de Vondst.` (twee rollen in de waarde); verwacht V en W 01-01-2026.
        nu: check exit 0; scenario PASS; ref en clingo PASS.
    R6  r6.rgs, het hulplid: `De havenopening van een Vondst moet gesteld worden op de afgeleide
        openingsdag van de haven van de jutter van de Vondst.` en `indien zijn vinddag later is dan
        zijn havenopening plus 6 mnd`.
        nu: check exit 0; scenario PASS; ref en clingo PASS.

## Acceptatie

- (a) Vormruimte, gemeten door de kaartschrijver in de exec-tak (een map per
  vorm; ref en clingo PASS op elke rij; MARKER is de zin van R1):

  | vorm in de voorwaarde | één rol: invoer / afgeleid | twee rollen: invoer / afgeleid |
  |---|---|---|
  | gevuld | PASS / PASS | MARKER / MARKER |
  | leeg | PASS / PASS | MARKER / MARKER |
  | vergelijking zonder rekensom | PASS / PASS | MARKER / MARKER |
  | datum plus 6 mnd aan de genavigeerde kant | MARKER / MARKER | MARKER / MARKER |
  | datum min 6 mnd aan de eigen kant | MARKER / MARKER | MARKER / MARKER |
  | datumrekensom aan beide kanten | MARKER / MARKER | MARKER / MARKER |
  | datum plus een parameter (mnd) | MARKER / MARKER | MARKER / MARKER |
  | getal plus 2 aan de genavigeerde kant | PASS / PASS | MARKER / MARKER |
  | genavigeerd getal plus 1 groter dan 5 | PASS / PASS | MARKER / MARKER |
  | getal min 2 aan de eigen kant | PASS / PASS | MARKER / MARKER |
  | getal plus een numerieke parameter | PASS / — | MARKER / — |
  | de lezing in de waarde, ook plus 6 mnd | — / PASS | — / PASS |
  | datumrekensom zonder navigatie (drie vormen) | PASS (geen rol) | — |

  Meet daarnaast: drie rollen; een kenmerkcheck twee rollen ver; een
  tijdsduur tussen een eigen en een genavigeerde datum; een to-many rol aan
  het eind van de keten (kwantor, vouw); een ontbrekende relatie op de eerste
  en op de tweede hop; een leeg-toets met de lezer vóór de schrijver (de
  bestaande grens in de README); en de begeleiders, objectcreatie en
  feitcreatie die de voorwaarde van hun gastregel herhalen (de lijst van de
  voorganger).
- (b) De exec-tak lowert een voorwaarde die via een keten van to-one rollen
  leest met een join per hop, zoals de waarde dat doet, en zet de toetsen op
  het onderwerp dat na de laatste join opnieuw gebonden wordt, zoals de
  voorganger voor één rol deed; een datumrekensom in een vergelijking met
  een genavigeerde lezing lowert zoals zonder navigatie; de uitkomst is die
  van ref en clingo. Wat niet meegaat, blijft een benoemde weigering met de
  vorm in de zin in plaats van de generieke "voorwaarde buiten Phase-B
  scope". Rood-eerst JUnit-getuige (bij `GenavigeerdeVoorwaardeWitnessTest`
  of een tweeling) met R1, R2 en R3, en een flank met een ontbrekende
  relatie op de tweede hop.
- (c) Golden (werknaam `jutterswet-havenketen`), handgerekend (R1–R3 in één
  document, plus een Vondst waarvan de jutter geen haven heeft), in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` in
  `scripts/check-drools.sh` als **voorstel**; de DRL-teksttak volgens haar
  kanaal (zij weigert twee rollen met "TODO voorwaarde", en schrijft een
  genavigeerde toets in het patroon van het onderwerp, open punt 1 van de
  voorganger).
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: de zin
  over twee of meer hops in "Recorded: a condition that reads through a
  role" zegt wat nu speelt en wat benoemd weigert, en noemt de
  datumrekensom één rol ver.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel met de andere
Drools-kaarten (zelfde transpiler, `ExecModelTranspiler.regelVoorwaarde`).
Niet nodig vóór de regelset verder gaat (R6 speelt); na de landing kan het
hulplid weg.

## Result (2026-10-01, bouwer)

Tak `card/drools-exec-voorwaarde-twee-rollen-ver` in de worktree van de kaart, afgetakt van main `e33b5c8d`; niet
gerebased, niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo:
`_build/default/ref/bin/runner.exe`, `_build/default/backends/clingo/bin/clingo_runner.exe`,
`_build/default/spec/verifier/bin/verifier.exe`, de frontend (`Resolver --ir … --cases … bron.rgs scenario.rgs`) en de
Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de teksttak). "Vóór" is de jar van de claim `b2d16545`
(backend gelijk aan main), "na" de jar van `800af0af` (de code van de kop `75d4c42d` op één commentaarregel na). Elke
vorm is een eigen Jutterswet-document (bron.rgs + scenario.rgs), door de frontend naar ir.json en cases verlaagd en door
een meetscript gemeten; generator, meetscript en sweepscript staan buiten de repo (`/tmp/v2/probe/vtr-bouw/maak.py`,
`meet.sh`, `sweep.sh`). De 49 vormen en R1–R6 van de kaartschrijver zijn uit
`/tmp/v2/probe/kaarten-g1/voorwaarde-twee-rollen/` gekopieerd (alleen gelezen); de 39 vormen `x-*` zijn van deze bouw.

### De reproductie op deze kop

| | ref | clingo | exectak vóór | exectak na | DRL-tekst (vóór = na) |
|---|---|---|---|---|---|
| R1 (twee rollen, vergelijking, afgeleid) | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | MARKER | `PLAYED 1` | TODO voorwaarde |
| R2 (twee rollen, gevuld, invoer) | idem | idem | MARKER | `PLAYED 1` | TODO voorwaarde |
| R3 (één rol, datumrekensom aan de eigen kant) | idem | idem | MARKER | `PLAYED 1` | `PLAYED 1` |
| R4 (één rol, zonder rekensom) | idem | idem | `PLAYED 1` | `PLAYED 1` | `PLAYED 1` |
| R5 (twee rollen in de waarde) | idem | idem | `PLAYED 1` | `PLAYED 1` | `PLAYED 1` |
| R6 (het hulplid) | idem | idem | `PLAYED 1` | `PLAYED 1` | `PLAYED 1` |

MARKER is overal in dit Result de zin van de kaart, `EXCLUDE transpiler SKIP: regel: <regel> — regel (voorwaarde
buiten Phase-B scope): <regel>`; "TODO voorwaarde" is `EXCLUDE drl-transpiler MARKER: regel: <regel> — TODO voorwaarde:
<voorwaarde>`.

### Oorzaak

1. Een keten. Elk blad van een voorwaarde las een genavigeerde operand over één rol: `navComparisonRole` en
   `datumTemporeelLezer` gaven voor een keten `null`, en `scalarWaarde` las een keten alleen op het waardepad
   (`ketenWaardePad`). Dat was bewust (het commentaar daar: symmetrisch met de marker KETEN van de DRL-tak), en de
   voorwaarde had voor een keten ook geen join die de toets opnieuw liet draaien. Elk blad gaf `null`, en
   `voorwaardeSkipReden` de generieke zin.
2. Een datumrekensom één rol ver. De twee takken voor een datumrekensom in een vergelijking (`leafPredicate`, en in
   `voorwaarde` die met één parameter) verlaagden beide kanten met `scalarWaarde(null, …)`: zonder waardeonderwerp lost
   een genavigeerde lezing niet op. De numerieke takken gaven het onderwerp al mee; daarom speelde "getal plus 2" één
   rol ver en "datum plus 6 mnd" niet.
3. Gevonden tijdens het werk: de waarde van een regel die een keten leest, had één join, op het bereikte object
   (`navReactiviteit`). Een feitcreatie die de relatie in het midden van de keten pas legt, schrijft dat veld op het
   tussenobject en werkt dat object bij; dat object stond niet in de tupel van de regel, dus de regel draaide niet
   opnieuw en de waarde bleef leeg (`x-waarde-relatie-door-feitcreatie`, exectak vóór `FAIL … attribuut uitkomst:
   verwacht 2026-01-01, werkelijk leeg`, ref en clingo PASS). Een stil fout antwoord van vóór deze kaart.

### Fix en de gekozen lowering

`ExecModelTranspiler` (+148, −75 regels, commentaar inbegrepen); `DrlTranspiler`, `FactModelGenerator`, de loader en
de stratificatie zijn niet aangeraakt.

- `navJoins` zet op een keten een join op elk object dat zij passeert, in volgorde, en op het bereikte object; de
  sleutel van een tussenjoin is die van een lezing die daar eindigt, zodat een kortere lezing in dezelfde regel de
  join deelt. Dat geldt voor voorwaarde en waarde (`navReactiviteit`). `ketenStappen` en `langsKeten` delen de wandeling
  tussen `navKetenReader`, `navJoins` en `navDoel`.
- `regelVoorwaarde` zet, terwijl de voorwaarde voor het opnieuw gebonden onderwerp lowert, haar genavigeerde lezingen
  in `gejoindeLezingen` (een identiteitsverzameling). Alleen voor die lezingen lowert een blad een keten (`navDoel`, en
  `scalarWaarde` naast `ketenWaardePad`) en geeft een datumrekensom in een vergelijking het onderwerp mee
  (`gejoindeLezer`, anders blijft het `null` zoals vóór). Buiten die lowering (een assertie, een kwantorvouw, een
  filter, de doelpoging van een feitcreatie) komt geen join, en blijft een keten geweigerd.
- `navDoel` vervangt `navComparisonRole` + `get(s, rol)` in de tien genavigeerde bladen (tekst en getal tegen een
  parameter, leeg/gevuld, elfproef, getal tegen een literal en tegen een eigen getal, tekst en booleaan tegen een
  literal, dagsoort, `datumTemporeelLezer`): één rol leest zoals vóór; een keten leest leeg waar een hop niets bereikt
  (RUNTIME-SEMANTICS ch. 1.2), zoals een ontbrekende enkele relatie.
- Begeleiders (deling, gevuurd), objectcreatie en feitcreatie gaan door `regelVoorwaarde` en nemen de keten mee.

Verworpen: een join alleen op het bereikte object, zoals de waarde had. Mutatieproef (proefbuild, niet gecommit): met
`navJoins` voor de voorwaarde op één join per keten faalt precies één getuigenrij,
`de_tweede_relatie_die_een_feitcreatie_later_legt:102 … <case: attribuut uitkomst: verwacht 2026-09-15, werkelijk
leeg>`. Ook verworpen: de keten in elk blad toelaten (een assertie of vouwfilter heeft geen join en zou op een later
afgeleide waarde blijven staan).

### (a) Vormruimte, vóór en na

De tabel van de kaart (ref en clingo `PASS (3 expectations)` op alle veertig; cel = exectak vóór → na):

| vorm in de voorwaarde | één rol: invoer / afgeleid | twee rollen: invoer / afgeleid |
|---|---|---|
| gevuld | PASS → PASS / PASS → PASS | MARKER → PASS / MARKER → PASS |
| leeg | PASS → PASS / PASS → PASS | MARKER → PASS / MARKER → PASS |
| vergelijking zonder rekensom | PASS → PASS / PASS → PASS | MARKER → PASS / MARKER → PASS |
| datum plus 6 mnd aan de genavigeerde kant | MARKER → PASS / MARKER → PASS | MARKER → PASS / MARKER → PASS |
| datum min 6 mnd aan de eigen kant | MARKER → PASS / MARKER → PASS | MARKER → PASS / MARKER → PASS |
| datumrekensom aan beide kanten | MARKER → PASS / MARKER → PASS | MARKER → PASS / MARKER → PASS |
| datum plus een parameter (mnd) | MARKER → PASS / MARKER → PASS | MARKER → PASS / MARKER → PASS |
| getal plus 2 aan de genavigeerde kant | PASS → PASS / PASS → PASS | MARKER → PASS / MARKER → PASS |
| genavigeerd getal plus 1 groter dan 5 | PASS → PASS / PASS → PASS | MARKER → PASS / MARKER → PASS |
| getal min 2 aan de eigen kant | PASS → PASS / PASS → PASS | MARKER → PASS / MARKER → PASS |
| getal plus een numerieke parameter | PASS → PASS / — | MARKER → PASS / — |
| de lezing in de waarde, ook plus 6 mnd | — / PASS → PASS | — / PASS → PASS |
| datumrekensom zonder navigatie (drie vormen) | PASS → PASS (geen rol) | — |

DRL-teksttak, vóór = na: één rol invoer `PLAYED 1` op alle vormen; één rol afgeleid `FAIL … verwacht …, werkelijk …`
op alle tien (open punt 1 van de voorganger: die tak zet een genavigeerde toets in het patroon van het onderwerp);
twee rollen TODO voorwaarde op alle vormen; de waardelezingen en de vormen zonder navigatie `PLAYED 1`.

De extra vormen. Situatie (één case, tenzij genoemd): V en W (jutter J, haven H met openingsdag 01-01-2026 en kustwacht
K met sluitingsdag 01-06-2026), X (jutter J2, haven H2 zonder openingsdag), Y (geen jutter: de eerste hop ontbreekt),
Z (jutter J3 zonder haven: de tweede hop ontbreekt), U (jutter J4, haven H3 zonder kustwacht: de derde hop ontbreekt);
de afleidingen na de lezer, tenzij genoemd. A2 is "de afgeleide openingsdag van de haven van de jutter van de Vondst",
A3 "de afgeleide sluitingsdag van de kustwacht van de haven van de jutter van de Vondst", H2 "de afgeleide heffing van
de haven van de jutter van de Vondst".

| vorm | voorwaarde (in "De uitkomst … op zijn vinddag indien …", tenzij genoemd) | ref | clingo | exec vóór | exec na | DRL-tekst |
|---|---|---|---|---|---|---|
| x-drie-rollen-vergelijking | zijn vinddag later dan A3 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-drie-rollen-gevuld | A3 gevuld | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-drie-rollen-rekensom | zijn vinddag later dan A3 plus 3 mnd | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-tijdsduur-een-rol | de tijdsduur van de vergunningsdag van de jutter tot zijn vinddag in hele maanden groter dan 5 | PASS (6) | PASS (6) | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| x-tijdsduur-twee-rollen | de tijdsduur van A2 tot zijn vinddag in hele maanden groter dan 6 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-ontbrekend-leeg | A2 leeg (afleiding eerst) | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-ontbrekend-gevuld | A2 gevuld | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-ontbrekend-ongelijk | zijn vinddag ongelijk aan A2 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-ontbrekend-rekensom | zijn vinddag min 6 mnd later dan A2 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-leeg-schrijver-eerst | A2 leeg, afleiding vóór de lezer | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-leeg-lezer-eerst | A2 leeg, lezer vóór de afleiding | PASS (6) | PASS (6) | MARKER | **FAIL** (zin 1) | TODO voorwaarde |
| x-leeg-lezer-eerst-een-rol | de afgeleide vergunningsdag van de jutter leeg, lezer eerst | PASS (6) | PASS (6) | FAIL (zin 1) | FAIL (zin 1) | FAIL |
| x-getal-literal | H2 groter dan 6 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-getal-parameter | H2 groter dan de drempel | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-getal-eigen-gespiegeld | H2 kleiner dan zijn waarde | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-getal-eigen | zijn waarde groter dan H2 | PASS (6) | PASS (6) | MARKER | MARKER | TODO voorwaarde |
| x-getal-eigen-een-rol | zijn waarde groter dan de premie van de jutter | PASS (6) | PASS (6) | MARKER | MARKER | PLAYED 1 |
| x-tekst-literal | de havennaam van de haven van de jutter gelijk aan "Katwijk" | PASS (6) | TRANSLATION FAULT (zin 2) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-datum-parameter | A2 eerder dan de peildatum (datumparameter) | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-datum-parameter-een-rol | de vergunningsdag van de jutter plus 1 mnd eerder dan de peildatum | PASS (6) | PASS (6) | MARKER | MARKER | PLAYED 1 |
| x-datum-parameter-eigen | zijn vinddag min 8 mnd eerder dan de peildatum | PASS (6) | PASS (6) | MARKER | MARKER | PLAYED 1 |
| x-alle | alle: A2 gevuld; zijn vinddag later dan A3 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-ten-minste-een | ten minste één: zijn vinddag later dan A3 plus 6 mnd; H2 groter dan 6 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-waarde-en-voorwaarde | waarde A2, voorwaarde A2 later dan 01-12-2025 | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-veel-som | de som van de premie van alle havenjutters van de haven van de jutter groter dan 4 | PASS (6) | PASS (6) | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| x-veel-aantal | het aantal havenjutters van de haven van de jutter groter dan 1 | MISMATCH (zin 3) | MISMATCH (zin 3) | MARKER | MARKER | aggregatie-join-marker |
| x-veel-kwantor | de premie van ten minste één van de havenjutters van de haven van de jutter groter dan 4 | frontend ERROR (zin 4) | — | — | — | — |
| x-kenmerk-twee-rollen | de haven van de jutter zeehaven is | EVALUATION FAULT (Unresolved) | TRANSLATION FAULT (Unresolved) | MARKER | MARKER | TODO voorwaarde |
| x-kenmerktoekenning | "Een Vondst is gemarkeerd indien A2 gevuld is" | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-initialisatie | "… moet geïnitialiseerd worden op zijn vinddag indien A2 gevuld is" | PASS (6) | PASS (6) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-deling | "zijn waarde gedeeld door zijn deler indien A2 gevuld is", V zonder deler | PASS (4) | PASS (4) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-deling-consistent | idem, elke deler gevuld | PASS (4) | PASS (4) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-gevuurd | "gemarkeerd indien regelversie uitkomst is gevuurd", uitkomst onder A2 gevuld | EVALUATION FAULT ('is gevuurd' not implemented) | TRANSLATION FAULT | MARKER | PLAYED 1 (zin 5) | TODO voorwaarde |
| x-objectcreatie | "Een Vondst heeft een melding … indien A2 gevuld is" | PASS (6) | PASS (6) | EXCLUDE (objectcreatie voorwaarde buiten scope) | PLAYED 1 | TODO voorwaarde |
| x-feitcreatie | aanmelding onder "de afgeleide sluitingsdag van de kustwacht van de haven van de Jutter gevuld" | PASS (5) | PASS (5) | EXCLUDE (feitcreatie niet naar anker of doel) | PLAYED 1 | TODO voorwaarde |
| x-relatie-door-feitcreatie | de openingsdag van de haven van de jutter gevuld; de haven komt uit een feitcreatie | PASS (2) | PASS (2) | MARKER | PLAYED 1 | TODO voorwaarde |
| x-waarde-relatie-door-feitcreatie | waarde "de openingsdag van de haven van de jutter"; de haven uit een feitcreatie | PASS (2) | PASS (2) | **FAIL** (zin 6) | PLAYED 1 | FAIL (zin 6) |
| x-consistentie | "De vinddag van een Vondst moet later zijn dan A2" | PASS (1) | PASS (1) | EXCLUDE (zin 7) | EXCLUDE (zin 7) | onopgeloste-consistentiemarker |
| x-consistentie-een-rol | idem over de vergunningsdag van de jutter | PASS (1) | PASS (1) | PLAYED 1 | PLAYED 1 | PLAYED 1 |

Zin 1: `FAIL … verwacht leeg, werkelijk 2026-09-15; verwacht leeg, werkelijk 2026-05-15; …` (de lezer vuurt op de
tijdelijke leegte, zie open punt 1). Zin 2: `TRANSLATION FAULT: a text/enum/boolean operand here is an own-subject
attribute read or a literal (no corpus case for more)`. Zin 3: `MISMATCH: W.uitkomst = leeg, expected 2026-05-15`
(ref) en `MISMATCH: W.uitkomst is underived (leeg), expected 20588` (clingo): de frontend maakt van "het aantal
havenjutters van de haven van de jutter van de Vondst" een telling over `sourceRole` `JutterHeeftVondst.jutter`, dus de
jutter van de Vondst (1) in plaats van de havenjutters van H (2); open punt 3. Zin 4: `setlezing buiten een vouwbron:
'de premie van ten minste één van de havenjutters van de haven van de jutter van de Vondst' …`. Zin 5: ref en clingo
voeren 'is gevuurd' niet uit; de verwachting volgt IR-SPEC ch. 4.5 (`RuleStatusCheck`: een globale status, waar voor
elk onderwerp zodra de regel voor één onderwerp vuurde). Zin 6: `FAIL … attribuut uitkomst: verwacht 2026-01-01,
werkelijk leeg`. Zin 7: `EXCLUDE transpiler SKIP: consistentie: controle vinddag — consistentie (schending buiten
Phase-D scope): controle vinddag`.

Samengevat over de 93 vormen die de frontend verlaagt (verifier: PASS op alle 93; x-veel-kwantor weigert de frontend):
exectak vóór → na 57 EXCLUDE → PLAYED, 26 PLAYED → PLAYED, 1 FAIL → PLAYED (x-waarde-relatie-door-feitcreatie), 7
EXCLUDE → EXCLUDE, 1 EXCLUDE → FAIL (x-leeg-lezer-eerst), 1 FAIL → FAIL (x-leeg-lezer-eerst-een-rol). Elke PLAYED is met
verwachtingen die ref bevestigt, ook waar clingo faalt (x-tekst-literal), behalve x-gevuurd (geen arbiter). DRL-tak vóór
en na gelijk op alle 93. De zeven die blijven weigeren: x-getal-eigen, x-getal-eigen-een-rol, x-datum-parameter-een-rol
en x-datum-parameter-eigen weigeren over één rol en zonder navigatie in dezelfde zin (de vorm, niet de keten); x-consistentie
is een assertie zonder join; x-kenmerk-twee-rollen en x-veel-aantal komen als Unresolved of als verkeerde telling uit de
frontend.

### (b) Getuige

`KetenVoorwaardeWitnessTest` (exectak, negen rijen, documenten in de klasse op Jutterswet-materiaal, met de waarden van
ref en clingo): `r1_een_vergelijking_twee_rollen_ver_ziet_de_afgeleide_datum` (R1 met de afleiding vóór en na de lezer,
en de DRL-tak weigert met TODO voorwaarde), `r2_gevuld_twee_rollen_ver_op_een_invoerwaarde`,
`r3_een_datumrekensom_aan_de_eigen_kant_een_rol_ver`, de flank `zonder_relatie_op_de_tweede_hop_leest_de_keten_leeg`
(een jutter zonder haven en een vondst zonder jutter, onder de leeg-toets en onder R1),
`de_tweede_relatie_die_een_feitcreatie_later_legt` (de join per hop), `de_waarde_over_de_keten_ziet_de_tweede_relatie_die_een_feitcreatie_later_legt`,
`drie_rollen_ver_met_een_datumrekensom`, `een_datumrekensom_met_een_parameter_twee_rollen_ver` en
`de_deling_door_leeg_onder_een_voorwaarde_twee_rollen_ver` (de §6.5-begeleider). De twaalf documenten die de klasse
speelt (en het eerste van R1 nog eens in de DRL-tak) zijn uit een proefrun van de test geschreven en naast de getuige
gemeten: elk `verifier: PASS`, `oracle: PASS` en `clingo: PASS`.

Rood op de claim (de klasse van de kop tegen de transpiler van `b2d16545`, proefbuild):

    Tests run: 9, Failures: 1, Errors: 8, Skipped: 0
    acht rijen » Excluded transpiler SKIP: regel: uitkomst — regel (voorwaarde buiten Phase-B scope): uitkomst
      (de delingsrij: … regel: uitkomstwaarde — regel (voorwaarde buiten Phase-B scope): uitkomstwaarde)
    de_waarde_over_de_keten_ziet_de_tweede_relatie_die_een_feitcreatie_later_legt:111 de haven komt uit de werving
      ==> expected: <null> but was: <case: attribuut uitkomst: verwacht 2026-01-01, werkelijk leeg>

De eerste acht rijen kwamen rood in `9dbbfe69`, de waarderij in `c62a76b2` (rood tegen `91943c10`, de fix van de
voorwaarde met de waarde nog op één join). Na de fix: de klasse 9 tests, 0 failures; `GenavigeerdeVoorwaardeWitnessTest`
blijft 8/8; de suite 960 tests, 0 failures, 0 skipped (951 op de claim).

### (c) Golden `jutterswet-havenketen` (`7f35e9e5`)

Uit `bron.rgs` en `scenario.rgs`, handgerekend in NOTE.md: havendatum (R1: later dan de afgeleide openingsdag van de
haven van de jutter), registerdatum (R2: de openingsdag van die haven gevuld), wachtdatum (R3: zijn vinddag min 6 mnd
later dan de vergunningsdag van de jutter) en havenopening (de waarde over dezelfde keten); de feitcreatie `werving`
en de afleiding staan na de lezers. Case 1: vier vondsten, V4 met een jutter zonder haven; case 2: geen haven in de
invoer, de werving legt haar tijdens het draaien. De IR uit `--ir bron.rgs` alleen is byte-gelijk aan die met het
scenario.

| case | verwachtingen | ref | clingo | exec vóór | exec na | DRL-tekst | no-op |
|---|---|---|---|---|---|---|---|
| case1-vier-vondsten | 18 | `oracle: PASS (18 expectations)` | `clingo: PASS (18 expectations)` | EXCLUDE (golden) | PASS | EXCLUDE (golden) | `MISMATCH: V4.wachtdatum = leeg, expected 2026-09-15` |
| case2-haven-uit-de-werving | 9 | `oracle: PASS (9 expectations)` | `clingo: PASS (9 expectations)` | idem | PASS | idem | `MISMATCH: V1.havenopening = leeg, expected 2026-01-01` |

Per golden: exectak vóór `EXCLUDE transpiler SKIP: regel: havendatum — regel (voorwaarde buiten Phase-B scope):
havendatum`, na `PLAYED 2`; met de jar van `91943c10` (voorwaarde per hop, waarde op één join) `FAIL
case2-haven-uit-de-werving.json: attribuut havenopening: verwacht 2026-01-01, werkelijk leeg`; DRL-tak `EXCLUDE
drl-transpiler MARKER: regel: havendatum — TODO voorwaarde: zijn vinddag later is dan de afgeleide openingsdag van de
haven van de jutter van de Vondst`. Toegelaten via `scripts/admit.sh` met het gate-12-voorstel en de README in de
werkboom: `admit: ADMITTED corpus/golden/jutterswet-havenketen (2 case(s); verifier-clean, ref = clingo, no-op refuted,
gates green)`.

### Gate-12-voorstel (`e7059404`, `scripts/check-drools.sh`, beschermd pad)

- `EXPECTED_PLAYED`: 231 → 232, redenblok "231 -> 232 (2026-10-01, a condition over a chain of roles)" na dat van
  jutterswet-sluitdag-gemeld.
- `DRL_CHANNEL` klasse (C): `"jutterswet-havenketen|EXCLUDE|MARKER regel: TODO voorwaarde — a read over two roles"` op
  naamvolgorde (vóór jutterswet-klipwacht); kop "— 11" → "— 12" met een historieregel "11 -> 12 (2026-10-01, the
  condition over a chain of roles)"; "SEVEN of the eleven" → "SEVEN of the twelve"; "FOUR are this branch alone" →
  "FIVE" met de regel "jutterswet-havenketen (exec PLAYED 2) — a TODO voorwaarde on a read over two roles". Klasse (A):
  "sitting 40 below" → "sitting 41 below", "The 40 is 44 exec-only players — those 38, jutterswet-vouwrand, …" → "The 41
  is 45 exec-only players — those 38, jutterswet-havenketen, jutterswet-vouwrand, …", en een hertelzin "the gap is 41
  (232 - 191); the six class heads (A 39, B 37, C 12, D 2, E 3, F 3 = 96 lines)". Lijstkop "eighty-nine of the
  ninety-five" → "ninety of the ninety-six".
- `EXPECTED_DRL_PLAYED` blijft 191, `EXPECTED_MIN_TESTS` blijft 239, `EXPECTED_CHANNEL` en `EXCLUDE_KNOWN` ongewijzigd.

### (d) Sweep

Over alle 286 goldens van de claim, elke golden met al zijn cases, statusregel per golden en per tak, jar van de claim
(`b2d16545`) tegen jar van `800af0af`: alle 572 regels gelijk (exectak PLAYED 231, DRL-tak PLAYED 191). Ook de join per
hop in de waarde verschuift geen regel: geen bestaande golden legt een tussenrelatie van een gelezen keten pas tijdens
het draaien. De nieuwe golden: exectak vóór EXCLUDE, na `PLAYED 2`; DRL-tak EXCLUDE. De beangeneratie is niet aangeraakt.

### README (`c1f59d70`)

`backends/drools/README.md`, sectie "Recorded: a condition that reads through a role": de zin "A condition over two or
more hops is refused by name, except the whole-period leeg and gevuld test" is vervangen door wat nu speelt (een join op
elk object van de keten, de toets op het opnieuw gebonden onderwerp; de waarde krijgt dezelfde joins; een vergelijking,
een leeg/gevuld-toets en een datumrekensom over een keten zoals over één rol; de genavigeerde operand van een
datumrekensom één rol ver of meer zoals zonder navigatie) en wat weigert (een keten in een vouw of een consistentie-
assertie; de vergelijkingen die ook over één rol weigeren; de kenmerkcheck over twee rollen die de draad niet draagt;
de DRL-teksttak). "Three limits remain" is "Two limits remain" geworden, en de grens van de leeg-toets noemt nu eigen,
één rol en een keten.

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates13/run.sh` op de kop `75d4c42d` (de laatste
codecommit; de commit met dit Result raakt alleen de kaart), met een schone werkboom: `eval $(opam env)`, `dune build`
(exit 0), dan elk script onder zijn shebang (bash voor catala, regelspraak, toka-parity, drools, flogic, graaf en
scenario; sh voor de negen andere). Logs in `/tmp/v2/uchar-gates13/`. Per gate de exitcode en de PASS-regel:

     1. check-self-contained (sh): exit 0 — check-self-contained: PASS
     2. check-ir-schema (sh): exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 326 file(s) validate and round-trip, floor 192; 43062 object(s) in 827 document(s) in the schema's key order, 1 in no subschema)
     3. check-case-schema (sh): exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 819 case file(s) validate)
     4. check-verifier (sh): exit 0 — check-verifier: PASS (326 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
     5. check-oracle (sh): exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 779 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
     6. check-clingo (sh): exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 779 cases replay exactly through clingo — ref = clingo over the corpus)
     7. check-noop (sh): exit 0 — check-noop: PASS (735 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
     8. check-rood (sh): exit 0 — check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
     9. check-catala (bash): exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10. check-regelspraak (bash): exit 0 — check-regelspraak: PASS — JUnit suite green (1008 tests, floor 173); 114 rgs golden(s) re-emit byte-identically (floor 80)
    11. check-toka-parity (bash): exit 0 — check-toka-parity: PASS
    12. check-drools (bash): exit 0 — check-drools: PASS
    13. check-reftype-derivation (sh): exit 0 — check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    14. check-flogic (bash): exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    15. check-graaf (bash): exit 0 — check-graaf: PASS
    16. check-scenario (bash): exit 0 — check-scenario: PASS — 21 scenario golden(s) (floor 7) regenerate 78 case file(s) (floor 31) and their ir.json byte-identically

Gate 12 verder, regels uit zijn samenvatting:

    played 232  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
    auto-excluded 52  (per-form refusals: loader / transpiler marker / contract / unsupported input)
    channel floor 55 non-PLAYED goldens pinned by name; played matches the pinned 232
    DRL-text branch played 191  (its OWN count — the narrower branch, not the square; pin 191)
    DRL-text floor 96 non-PLAYED goldens pinned by name; sweep cost 129s wall clock
    JUnit 960 tests, floor 239  (the backend's own suite, green with a non-zero count)

Gate 15 ook `check-graaf: route leg — the graph of the wire equals the graph of the source on all 114 goldens with a
bron.rgs (floor 98); 23 unit tests green (floor 23)` en `spec leg — 250 of 287 goldens compared, 175 dependencies equal
to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)`. Gate 11 ook `re-emission: 1 casus dump(s), 45
.rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27`.

Eerder groen: alle gates in de toelating van de golden (`scripts/admit.sh`, met de golden, het gate-12-voorstel en de
README in de werkboom, op de code van `800af0af`).

### Afwijkingen van de kaart

1. Ook de WAARDE van een regel krijgt een join per object van de keten. De kaart vraagt een join per hop voor de
   voorwaarde "zoals de waarde dat doet"; de waarde deed dat niet (één join op het bereikte object) en gaf gemeten een
   stil fout antwoord wanneer een feitcreatie de tussenrelatie later legt (x-waarde-relatie-door-feitcreatie, en case 2
   van de golden met de jar van `91943c10`). Rood-eerst getuigenrij in `c62a76b2`; de sweep verschuift geen regel.
2. De kenmerkcheck twee rollen ver is niet te lowereren: de genavigeerde `CharacteristicCheck` op de draad draagt één
   to-one hop (IR-SPEC ch. 4.5), en de frontend laat "de haven van de jutter van de Vondst zeehaven is" als
   `Unresolved` staan (ref `EVALUATION FAULT`, clingo `TRANSLATION FAULT`); de exectak weigert zoals vóór met de
   generieke zin.
3. Een to-many rol aan het eind: de som over de keten (`sourcePath` met to-one hops ervoor) speelde al vóór de fix; de
   telling maakt de frontend verkeerd (open punt 3); de kwantor weigert de frontend (zin 4).
4. De generieke zin blijft bij vier vormen met een keten of een rol waarvan het BLAD ook zonder keten weigert (een eigen
   getal links van een genavigeerd getal; een datumrekensom tegen een datumparameter): over één rol en zonder navigatie
   geeft de exectak dezelfde zin, dus de keten is niet de oorzaak. Die bladen zijn niet aangeraakt (open punt 4).
5. De getuige heeft meer rijen dan R1–R3 en de flank: de join per hop (voorwaarde en waarde), drie rollen, een
   parameter en de §6.5-begeleider.
6. Geen bestaand testbestand en geen bestaande golden gewijzigd.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 5.5, "Recorded flank (measured 2026-09-23 …)", tweede bullet, na "*Closed in the exec-model
  branch on 2026-09-28 …*": "*Widened in the exec-model branch on 2026-10-01 (golden `jutterswet-havenketen`, witness
  `KetenVoorwaardeWitnessTest`): a condition that reads through a chain of two or more to-one roles is tested after a
  far-side join on every object the chain passes, so a relation in the middle of the chain that a fact creation sets
  while the rules run is seen too; a rule's value over a chain takes the same joins (before, it joined the reached
  object only and read leeg when the middle relation came later). The DRL-text branch refuses such a condition by its
  marker and keeps the one join in a value.*" En in de eerste bullet (de volgordeafhankelijke vorm): "The exec-model
  branch's navigated leeg test, over one role and since 2026-10-01 over a chain, has this form as well (measured
  2026-10-01: with the reader first it writes the vinddag where `ref/` and `backends/clingo` answer leeg)."
- `spec/CONSTRUCTS.md` rij 6, "Recorded open": "both branches act on a transient leeg when an own-operand or filter
  reader precedes the writer" wordt "… when an own-operand, filter or navigated reader (in the exec-model branch over one
  role or a chain) precedes the writer"; de rest ongewijzigd.

### Open punten

1. De leeg-toets met de lezer vóór de schrijver geldt nu ook voor een keten: vóór deze kaart weigerde de exectak de
   keten luid, nu antwoordt zij daar fout zoals over één rol en eigen (x-leeg-lezer-eerst, x-leeg-lezer-eerst-een-rol;
   de bestaande grens in de README en de eerste bullet van de IR-SPEC-flank). Met de schrijver eerst speelt zij
   (x-leeg-schrijver-eerst, x-ontbrekend-leeg). De fix daarvoor blijft een salience onder de schrijvers van de operand
   (open punt 2 van de voorganger).
2. De DRL-teksttak weigert een voorwaarde over twee of meer rollen (TODO voorwaarde) en joint een keten in een waarde
   alleen op het bereikte object: x-waarde-relatie-door-feitcreatie is daar een stil fout antwoord (`FAIL … werkelijk
   leeg`). Herstel raakt `DrlTranspiler.navReactiviteitClausules`.
3. Frontend: "het aantal havenjutters van de haven van de jutter van de Vondst" wordt een telling met `sourceRole`
   `JutterHeeftVondst.jutter` en argument `jutter`, dus de jutter van de Vondst; de verifier laat het document door, en
   ref en clingo antwoorden daarop (1 in plaats van 2) zonder fout. Een telling over een bereik van meer dan één stap
   is volgens WF-102 (c) ongeldig; de frontend kapt het bereik stil af. Reproductie: vorm x-veel-aantal.
4. Exectak, niet aan de keten gebonden: een eigen getal links van een genavigeerd getal ("zijn waarde groter is dan de
   premie van de jutter van de Vondst") en een datumrekensom tegen een datumparameter (ook zonder navigatie) weigeren met
   de generieke zin; de DRL-teksttak speelt beide over één rol. Het eerste zou de omkering dragen die `leafPredicate`
   al voor een literal links doet; het tweede een binding van de datumparameter in de tak voor een datumrekensom.
5. Exectak: een consistentie-assertie over een keten weigert (`schending buiten Phase-D scope`), over één rol speelt
   zij; de assertie heeft geen join.
6. De ASP-backend vertaalt een genavigeerde tekstvergelijking over twee rollen niet (x-tekst-literal, zin 2); ref
   antwoordt, de exectak speelt met de waarden van ref.
7. De frontend lost een kwantor aan het eind van een keten niet op (zin 4) en een kenmerkcheck over twee rollen ook
   niet (Unresolved); de draad draagt voor de kenmerkcheck één hop.
8. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee. Daarna kan
   het hulplid van R6 weg (NB van de kaart).

### Commits

    b2d16545 claim: drools-exec-voorwaarde-twee-rollen-ver
    9dbbfe69 KetenVoorwaardeWitnessTest: voorwaarde over een keten van rollen, rood
    91943c10 Exectak: voorwaarde over een keten van rollen, een join per hop
    c62a76b2 KetenVoorwaardeWitnessTest: de waarde over de keten na een feitcreatie, rood
    800af0af Exectak: ook de waarde joint een keten bij elk object onderweg
    7f35e9e5 golden jutterswet-havenketen: voorwaarde over een keten van rollen
    e7059404 voorstel gate 12: jutterswet-havenketen speelt in de exectak
    c1f59d70 backends/drools/README: een voorwaarde over een keten van rollen
    75d4c42d Exectak: commentaar bij de join op een object onderweg

en de commit die de kaart met dit Result naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.

## Keuring (hoofdsessie, 2026-10-01)

Eigen run van de zestien gates op `2729d69c` in de volgorde van `gates.yml`
(bash waar de shebang dat zegt), `dune build` vooraf; logs in
`/tmp/v2/uchar-gates13/`. Alle zestien exit 0: gate 10 JUnit 1008, 114
rgs-goldens; gate 12 `check-drools: PASS`, played 232 (pin 232), DRL 191
(pin 191), JUnit 960; gate 16 21 scenario-goldens, 78 casebestanden. De fix
gelezen: `navDoel` vervangt in de genavigeerde bladen de ene rol door de
keten als functie van het onderwerp; `navJoins` joint elk object dat de
keten passeert, onder de sleutel van een lezing die daar eindigt;
`gejoindeLezingen` begrenst de ketenlowering tot de lezingen die de
voorwaarde joint, en `gejoindeLezer` geeft een datumrekensom met zo'n
lezing het onderwerp mee; `ketenStappen` en `langsKeten` delen de
wandeling. Afwijking 1 aanvaard: de waarde van een regel krijgt dezelfde
joins, want één join op het bereikte object gaf een stil fout antwoord als
een feitcreatie de tussenrelatie later legt (rood-eerst getuigd, sweep
ongewijzigd). Afwijkingen 2–4 aanvaard: de kenmerkcheck over twee rollen
komt niet op de draad (één hop, IR-SPEC ch. 4.5) en de vier bladen die ook
zonder keten weigeren, blijven buiten deze kaart (open punt 4). De
voorgestelde registertekst is in dit landingscommit gezet in
`spec/IR-SPEC.md` ch. 5.5 (beide bullets van de flank) en
`spec/CONSTRUCTS.md` rij 6. Open punt 3 (de frontend kapt een telling over
een keten stil af naar één stap; ref en clingo rekenen 1 waar 2 hoort) is
een stil fout antwoord in de frontend en staat als aanvulling in de kaart
`frontend-kwantor-over-een-pad` in `backlog/new`. Open punt 1 gelezen: de
leeg-toets met de lezer vóór de schrijver verruimt de bekende grens van
één rol naar de keten; de README en de flank zeggen het, de fix blijft een
plaatsing onder de schrijvers van de operand (open punt 2 van de
voorganger), kandidaat voor een eigen kaart. Commitkoppen tot 76 tekens
aanvaard (ff-only, geen rebase). Open punten 2 en 5–8: geen kaart.

## Vervolgronde lezer-onder-schrijver (2026-10-01, bouwer)

Tak `vervolg/lezer-onder-schrijver` in een eigen worktree, vanaf uchar main `4f00731e`; niet gerebased, niet gepusht,
niet gemerged. Gemeten met de uitvoerders van deze kop: `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`, `_build/default/spec/verifier/bin/verifier.exe`, de frontend
(`Resolver --ir … --cases … bron.rgs scenario.rgs`) en de Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de
teksttak). "Vóór" is de jar van de claim `50a90973` (code gelijk aan main), "na" de jar van `454c9ab5` (de laatste
codecommit). Elke vorm is een eigen Jutterswet-document (bron.rgs + scenario.rgs), door de frontend verlaagd en door een
meetscript gemeten. Generator, meetscripts, sweep en tabellen staan buiten de repo onder `/tmp/v2/probe/los-bouw/`
(`maak.py`, `meet.sh`, `meet-alle.sh`, `sweep.sh`, `dump-getuige.sh`, `tool/ToonSalience.java`; tabellen `meting-*.tsv`,
`sweep-*.tsv`, `salience-*.txt`).

### Gat K-c: de leeg/gevuld-toets (en een vergelijking) met de lezer vóór de schrijver

#### Reproductie op deze kop

| vorm | ref | clingo | exectak op de claim |
|---|---|---|---|
| x-leeg-lezer-eerst (keten, uit de meting van de vorige bouwer) | `oracle: PASS (6 expectations)` | `clingo: PASS (6 expectations)` | `FAIL … verwacht leeg, werkelijk 2026-09-15; verwacht leeg, werkelijk 2026-05-15; verwacht leeg, werkelijk 2026-09-15` |
| x-leeg-lezer-eerst-een-rol | idem | idem | `FAIL … verwacht leeg, werkelijk 2026-09-15` (vier keer) |
| a-eigen-leeg-regel-regel-lezer-eerst (eigen operand) | `oracle: PASS (5 expectations)` | `clingo: PASS (5 expectations)` | `FAIL … verwacht leeg, werkelijk 2026-09-15` |

#### Oorzaak

`groepVolgorde` zette een gewone regel alleen onder (a) de schrijvers van een kenmerk dat zij ontkend leest
(`ontkendVan`: een groep met dat kenmerk als doel, een kenmerktoekenning) en (b) de schrijvers van wat een vouw in haar
negatief leest (`vouwOntkend`, alleen binnen een vouw: `inVouw`). Een leeg- of gevuld-toets buiten een vouw, en wat een
kenmerktoekenning in een vergelijking leest, kregen geen plaats. Drools vuurt één salience in de volgorde waarin de
regels zijn toegevoegd: de gewone regels in gedeclareerde volgorde, daarna onder meer de verdelingen en de creaties, als
laatste de groepen. Een lezer die vóór
zijn schrijver staat, of die leest wat een defaultsgroep of een creatie schrijft, toetste daardoor op de tijdelijke
leegte, en zijn schrijving bleef: een kenmerk is monotoon (ch. 5.3) en een geschreven attribuut wordt niet ingetrokken
als de voorwaarde later onwaar wordt (ch. 6.1, recorded residuals).

#### Fix en gekozen ordening (`17e623b8`)

`ExecModelTranspiler.groepVolgorde` krijgt per gewone regel de verzameling knopen waarna zij vuurt:

- wat zij onder een leeg- of gevuld-toets leest, waar ook in voorwaarde of waarde, ook binnen een vouw, met de relaties
  van een genavigeerde operand (nieuw: `leegLezingen`; RUNTIME-SEMANTICS ch. 6.1: een negatieve rand in beide
  polariteiten);
- voor een kenmerktoekenning, die onherroepelijk schrijft, alles wat zij leest behalve een kenmerk (stratum-strikt, ch.
  6.1): attributen, de populatie van een vouw, de koppelingen van een navigatie of vouw. `conditieLezingen` en
  `exprLezingen` kregen daarvoor de vlag `kenmerken` (onwaar: een kenmerkcheck en de relaties van haar hop tellen niet;
  een positieve kenmerklezing is monotoon, een ontkende is gestratificeerd).

Voor elke schrijver van zo'n lezing (een gewone regel, een defaultsgroep, een object- of feitcreatie) komen de schrijver
en alles waarop hij transitief wacht (`bereik`) in die verzameling. De regel vuurt na elk van die knopen: lager in
salience, of gelijk en eerder toegevoegd (`laadrang`), dezelfde voorwaarde die de creaties al hadden. Een regel die na
zijn schrijver staat, houdt dus zijn salience. Een schrijver die zelf (transitief) op de regel wacht, ligt met haar op
een kring: bij een negatieve lezing weigert de regel met de vorm in de zin (`kringWeigering`, WF-046), bij een
stratum-strikte lezing houdt zij haar plaats (de generaties van een recursieve creatie ordenen haar, ch. 5.5). Een
leeg-toets op het eigen doel van de regel weigert ook: `regel (een leeg- of gevuld-toets op afgeleide registratiedag van
Vondst, dat deze regel zelf schrijft: een kring zonder volgorde (WF-046)): afgeleide registratiedag zelf`; op een kring
van twee regels: `regel (een leeg- of gevuld-toets op afgeleide controledag van Vondst, dat de regel afgeleide
controledag, die op deze regel wacht, schrijft: een kring zonder volgorde (WF-046)): afgeleide registratiedag`.

De begeleiders van een regel (§6.5 deling, §6.6 wortel, leeg startpunt, gevuurd) krijgen de salience van hun regel
(`companionRule` en `gevuurdCompanions`): zij herhalen de voorwaarde van de regel en meldden op salience 0 de tijdelijke
stand (d-begeleider-deling: `verwacht consistent, werkelijk inconsistent (uitkomstwaarde @ V)`, ook na de ordening van
de regel alleen).

Niet gekozen: (1) de band min één, zoals `vouwOntkend` doet (niet gebouwd en niet gemeten; eigen gevolgtrekking: die zet
een lezer ook onder een schrijver die al vóór hem vuurt, dus zakken meer regels in het corpus); (2) de ordening in
`Stratificatie`: die delen beide takken, en de teksttak houdt volgens de opdracht haar kanaal; (3) een vergelijking in
de voorwaarde van een regel die een attribuut schrijft ook ordenen: ch. 6.1 leest die positief, en of zo'n lezing
negatief wordt is de normvraag die ch. 6.1 openlaat (open punt 1).

#### Vormruimte vóór en na (exectak; DRL-tekst vóór = na op alle vormen)

Raster A (72 vormen): operand eigen ("zijn afgeleide registratiedag"), één rol ("de afgeleide vergunningsdag van de
jutter van de Vondst") en keten ("de afgeleide openingsdag van de haven van de jutter van de Vondst") × toets leeg,
gevuld, "ongelijk is aan <de waarde van V>" × lezer gewone regel ("De uitkomst van een Vondst moet gesteld worden op
zijn vinddag indien …") en kenmerktoekenning ("Een Vondst is gemarkeerd indien …") × schrijver regel en defaultsgroep ×
lezer vóór en na de schrijver. Vijf vondsten, elk operand met een gevulde en lege waarde.

| toets | lezer | schrijver | volgorde | ref / clingo | exectak vóór → na (per drie operanden) | DRL-tekst (eigen / rol / keten) |
|---|---|---|---|---|---|---|
| leeg | regel | regel | lezer eerst | PASS / PASS | FAIL → PLAYED (3) | FAIL / FAIL / TODO voorwaarde |
| leeg | regel | regel | schrijver eerst | PASS / PASS | PLAYED → PLAYED (3) | PLAYED / FAIL / TODO voorwaarde |
| leeg | regel | groep | beide | PASS / PASS | FAIL → PLAYED (6) | EXCLUDE (defaults) |
| leeg | kenmerk | regel | lezer eerst | PASS / PASS | FAIL → PLAYED (3) | FAIL / FAIL / TODO voorwaarde |
| leeg | kenmerk | regel | schrijver eerst | PASS / PASS | PLAYED → PLAYED (3) | PLAYED / FAIL / TODO voorwaarde |
| leeg | kenmerk | groep | beide | PASS / PASS | FAIL → PLAYED (6) | EXCLUDE (defaults) |
| gevuld | regel, kenmerk | regel, groep | beide | PASS / PASS | PLAYED → PLAYED (24) | regel: PLAYED / FAIL / TODO voorwaarde; groep: EXCLUDE |
| ongelijk | kenmerk | regel | lezer eerst | PASS / PASS | FAIL → PLAYED (3) | FAIL / FAIL / TODO voorwaarde |
| ongelijk | kenmerk | regel | schrijver eerst | PASS / PASS | PLAYED → PLAYED (3) | PLAYED / FAIL / TODO voorwaarde |
| ongelijk | kenmerk | groep | beide | PASS / PASS | FAIL → PLAYED (6) | EXCLUDE (defaults) |
| ongelijk | regel | regel | lezer eerst | `MISMATCH: V.uitkomst = 2026-09-15, expected leeg` / PASS | FAIL → FAIL (3) | FAIL / FAIL / TODO voorwaarde |
| ongelijk | regel | regel | schrijver eerst | PASS / PASS | PLAYED → PLAYED (3) | PLAYED / FAIL / TODO voorwaarde |
| ongelijk | regel | groep | beide | `MISMATCH` (idem) / PASS | FAIL → FAIL (6) | EXCLUDE (defaults) |

De FAIL-zinnen vóór: `verwacht leeg, werkelijk 2026-09-15` (de gewone regel) en `kenmerk gemarkeerd: verwacht afwezig,
werkelijk aanwezig` (de kenmerktoekenning). De negen ongelijk-vormen met een gewone regel als lezer, de lezer vóór de
schrijver of een groep als schrijver, hebben geen arbiter: ref schrijft op de tijdelijke leegte (het recorded residual
van ch. 6.1), clingo niet; de exectak volgt ref, vóór en na.

Raster B (twee operanden), de schrijver na de lezer:

| vorm | ref / clingo | exectak vóór → na | DRL-tekst |
|---|---|---|---|
| samengesteld, regel: "hij aan alle volgende voorwaarden voldoet: - de afgeleide vergunningsdag van de jutter van de Vondst is leeg - zijn vinddag is gevuld" | PASS / PASS | FAIL `verwacht leeg, werkelijk 2026-09-15` → PLAYED | FAIL |
| samengesteld, kenmerk | PASS / PASS | FAIL `kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig` → PLAYED | FAIL |
| samengesteld, regel, groep | PASS / PASS | FAIL → PLAYED | EXCLUDE (defaults) |
| samengesteld, kenmerk, groep | PASS / PASS | FAIL → PLAYED | EXCLUDE (defaults) |
| twee later afgeleide operanden, kenmerk: "… ongelijk is aan zijn afgeleide controledag" | PASS / PASS | FAIL `kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig` → PLAYED | FAIL |
| twee later afgeleide operanden, regel | PASS / PASS | FAIL `verwacht leeg, werkelijk 2026-09-15` → idem | FAIL |

De laatste rij blijft fout: ref en clingo antwoorden hier gelijk, want ref toetst de lezer pas in de volgende ronde na
beide schrijvers; de exectak toetst opnieuw na de eerste schrijver (één operand gevuld, de andere leeg: ongelijk) en
schrijft. Een vergelijking in een regel die een attribuut schrijft leest positief (ch. 6.1); open punt 1.

Raster D (bijzondere vormen):

| vorm | ref / clingo | exectak vóór → na | DRL-tekst |
|---|---|---|---|
| d-ketting: "uitkomst indien zijn afgeleide controledag leeg is", "afgeleide controledag … indien de afgeleide vergunningsdag van de jutter van de Vondst leeg is", dan de schrijver | PASS / PASS | FAIL `verwacht leeg, werkelijk 2026-08-15` → PLAYED | FAIL |
| d-negatie-na-lezer: "gemeld indien hij niet gemarkeerd is", "gemarkeerd indien zijn afgeleide registratiedag leeg is", schrijver | PASS / PASS | FAIL `kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig` → PLAYED | FAIL |
| d-begeleider-deling: "zijn waarde gedeeld door zijn deler indien zijn afgeleide registratiedag leeg is" | PASS / PASS | FAIL `verwacht consistent, werkelijk inconsistent (uitkomstwaarde @ V)` → PLAYED | FAIL (idem) |
| d-creatie-na-lezer: een creatie leest de uitvoer van een leeg-lezer vóór zijn schrijver | PASS / PASS | FAIL `verwacht leeg, werkelijk 2026-12-31` → PLAYED | EXCLUDE (TODO aggregatie) |
| d-groep-na-lezer: een defaultsgroep leest de uitvoer van een leeg-lezer | PASS / PASS | FAIL `verwacht leeg, werkelijk 2026-09-15` → PLAYED | EXCLUDE (defaults) |
| d-zelf-alleen: "De afgeleide registratiedag … op zijn vinddag indien zijn afgeleide registratiedag leeg is" | verifier `WF-046 FAIL`; ref `EVALUATION FAULT: WF-045`; clingo `UNSATISFIABLE` | PLAYED → EXCLUDE (de weigering hierboven) | PLAYED |
| d-zelf-met-schrijver: idem, naast de gewone schrijver | idem | FAIL `geen fixpunt: twee schrijvers van één doel …` → EXCLUDE (idem) | FAIL (idem) |
| d-kring-leeg: twee regels, elk onder een leeg-toets op het doel van de ander | verifier `WF-046 FAIL`; ref `EVALUATION FAULT: WF-045`; clingo `MISMATCH` | PLAYED → EXCLUDE (de weigering hierboven) | PLAYED |
| d-kring-kenmerk: "gemarkeerd indien zijn afgeleide controledag ongelijk is aan 01-06-2026", "afgeleide controledag … indien hij gemarkeerd is" | verifier `WF-045 FAIL`; ref `EVALUATION FAULT`; clingo `UNSATISFIABLE` | PLAYED → PLAYED (stratum-strikt op een kring: blijft) | PLAYED |

#### Getuige

`LezerOnderSchrijverWitnessTest` (exectak, negentien rijen, documenten in de klasse op Jutterswet-materiaal). Voor K-c:
`een_leegtoets_op_het_eigen_lid_voor_zijn_schrijver` (beide volgordes),
`een_leegtoets_een_rol_ver_op_een_lid_uit_een_defaultsgroep`, `een_leegtoets_over_een_keten_in_een_kenmerktoekenning`,
`een_kenmerk_over_een_vergelijking_met_een_later_afgeleide_waarde`, `een_samengestelde_voorwaarde_met_een_leegtoets`,
`twee_later_afgeleide_operanden_in_een_kenmerktoekenning`, `een_ketting_van_leeglezers`,
`een_ontkende_lezer_van_een_kenmerk_op_een_leegtoets`, `de_begeleider_van_een_deling_onder_een_leegtoets`,
`een_creatie_wacht_op_een_lezer_die_onder_zijn_schrijver_staat`, en de twee weigeringen
`een_leegtoets_op_een_kring_met_zijn_schrijver_weigert_met_de_vorm` en
`een_leegtoets_op_het_eigen_doel_weigert_met_de_vorm`. Rood op de claim (`292f4f34` tegen de transpiler van `50a90973`):
`Tests run: 16, Failures: 16`, met de zinnen van de tabellen en `Expected … CaseRunner.Excluded to be thrown, but
nothing was thrown` voor de weigeringen. De drie rijen van de flank (consistentie, verdeling) rood in `ab1bd924` tegen
`17e623b8`: `Tests run: 19, Failures: 3`. Na `454c9ab5`: 19 van 19 groen; de suite 1019 tests, 0 failures. De twintig
documenten die de klasse speelt, zijn met zachte asserts uitgeschreven en gemeten (`dump-getuige.sh`): de achttien die
moeten spelen `verifier: PASS`, `oracle: PASS` en `clingo: PASS`; de twee weigeringen `WF-046 FAIL` in de verifier en
`EVALUATION FAULT: WF-045` in ref.

### Gat K-f: een lezer van de uitvoer van een creatie die onherroepelijk schrijft

#### Reproductie op deze kop

f-lezer-onder-de-creatie (de vorm van de vorige bouwer: "Een Jutter is onafgerekend indien het aantal afrekeningen van
de Jutter gelijk is aan 0", de creatie onder een basis uit een defaultsgroep): ref `oracle: PASS (8 expectations)`,
clingo `clingo: PASS (8 expectations)`, exectak op de claim `FAIL … kenmerk onafgerekend: verwacht afwezig, werkelijk
aanwezig` (vier keer).

#### Oorzaak

Dezelfde als K-c, voor de lezingen die een creatie schrijft: een creatie schrijft de populatie van het gemaakte type,
haar koppeling en haar toekenningen, en een kenmerktoekenning leest een telling van de gemaakte objecten (populatie en
koppeling) stratum-strikt. De ordening zette een regel alleen onder de schrijvers van een ontkend kenmerk en van een
negatieve vouwlezing; een creatie wordt na de regels toegevoegd, dus de lezer vuurde op salience 0 vóór de creatie op de
telling 0, ook zonder groep.

#### Fix en gekozen ordening

Dezelfde commit `17e623b8`: de stratum-strikte lezingen van een kenmerktoekenning omvatten de populatie en de
koppelingen van een telling, en de creaties zijn al knopen die ze schrijven, dus de lezer komt na de creatie en na alles
waarop de creatie wacht (de groep). Een leeg-toets over de koppeling van een creatie ("de afrekendag van de afrekening
van de Jutter leeg is") is een negatieve lezing van die koppeling en van het gemaakte lid en wacht ook.

#### Vormruimte vóór en na (raster C, 40 vormen; DRL-tekst vóór = na)

Creatie "Een Jutter heeft een afrekening (een bon) met afrekendag (bondag) gelijk aan zijn vergunningsdag indien zijn
vergunningsdag gevuld is", feitcreatie "Een gecontroleerd net van een Jutter is een net van de Jutter"; lezer vóór en na
de creatie in het document.

| vorm | ref / clingo | exectak vóór → na | DRL-tekst |
|---|---|---|---|
| rol: "de afrekendag van de afrekening van de Jutter" leeg, regel en kenmerk (4) | PASS / PASS | FAIL `verwacht leeg, werkelijk 2026-12-31` en `kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig` → PLAYED | FAIL |
| rol, ongelijk 01-03-2026, kenmerk (2) | PASS / PASS | FAIL → PLAYED | FAIL |
| rol, ongelijk, regel (2) | `MISMATCH: J.uitkomst = 2026-12-31` / PASS | FAIL → FAIL (geen arbiter) | FAIL |
| rol, gevuld (4) | PASS / PASS | PLAYED → PLAYED | PLAYED |
| keten: "de afrekendag van de afrekening van de jutter van de Vondst", leeg en ongelijk, kenmerk; leeg, regel (6) | PASS / PASS | FAIL → PLAYED | TODO voorwaarde |
| keten, ongelijk, regel (2) | `MISMATCH` / PASS | FAIL → FAIL (geen arbiter) | TODO voorwaarde |
| keten, gevuld (4) | PASS / PASS | PLAYED → PLAYED | TODO voorwaarde |
| telling: "het aantal bonnen van de Jutter gelijk is aan 0", kenmerk (2) | PASS / PASS | FAIL `kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig` → PLAYED | FAIL |
| telling gelijk aan 0, regel (een gelijkstelling) (2) | `MISMATCH: J3.uitkomst = 2026-12-31` / PASS | FAIL → FAIL (geen arbiter) | FAIL |
| telling groter dan 0 (4) | PASS / PASS | PLAYED → PLAYED | PLAYED |
| feit: "het aantal gecontroleerde netten van de Jutter gelijk is aan 0", kenmerk (2) | PASS / PASS | FAIL → PLAYED | FAIL |
| feit gelijk aan 0, regel (2) | `MISMATCH` / PASS | FAIL → FAIL (geen arbiter) | FAIL |
| feit groter dan 0 (4) | PASS / PASS | PLAYED → PLAYED | PLAYED |
| f-lezer-onder-de-creatie (vorige bouwer) | PASS (8) / PASS (8) | FAIL → PLAYED | EXCLUDE (defaults) |

Een gelijkstelling over een telling van de gemaakte objecten schrijft een attribuut en is geen onherroepelijke lezer:
ref schrijft daar op de tijdelijke 0 en clingo niet, de exectak volgt ref, vóór en na (de normvraag van open punt 1).

#### Getuige

In dezelfde klasse: `een_kenmerk_over_het_aantal_gemaakte_objecten`,
`een_kenmerk_over_de_koppeling_die_een_feitcreatie_legt`, `een_leegtoets_over_de_koppeling_van_een_creatie`,
`een_kenmerk_op_een_creatie_die_op_een_defaultsgroep_wacht`; rood en groen zoals hierboven.

### In dezelfde laag gevonden en gemaakt (`ab1bd924`, `454c9ab5`)

| vorm | ref / clingo | exectak vóór → na | DRL-tekst |
|---|---|---|---|
| d-consistentie-groep: "De vinddag van een Vondst moet later zijn dan zijn afgeleide registratiedag", dat lid uit een groep | PASS / PASS | FAIL `verwacht consistent, werkelijk inconsistent (controle vinddag @ V)` → PLAYED | EXCLUDE (defaults) |
| e-consistentie-creatie: "De vergunningsdag van een Jutter moet eerder zijn dan de afrekendag van de afrekening van de Jutter", met een creatie | PASS / PASS | FAIL `verwacht consistent, werkelijk inconsistent (controle vergunningsdag @ J)` → PLAYED | PLAYED |
| d-consistentie-regel: idem, het lid uit een gewone regel | PASS / PASS | PLAYED → PLAYED | PLAYED |
| e-verdeling-leeg: "Een Berger is gemarkeerd indien zijn bergingsaandeel leeg is" vóór en na de verdeling (2) | PASS / PASS | FAIL `kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig` → PLAYED | FAIL |
| f-vouw-verdeling-waar: "het aantal bergers van de Berging die aan de volgende voorwaarde voldoen: zijn bergingsaandeel is leeg groter is dan 0" | PASS / PASS | FAIL `verwacht leeg, werkelijk 1` → PLAYED | FAIL |
| f-vouw-creatie: dezelfde vouw over de bonnen van een creatie | PASS / PASS | PLAYED → PLAYED (sinds de vorige ronde) | FAIL |
| e-uniciteit-groep: "De afgeleide registratiedagen van alle Vondsten moeten uniek zijn", het lid uit een groep | PASS / PASS | PLAYED → PLAYED | EXCLUDE (defaults) |
| e-verdeling-bron-groep: een verdeling waarvan een groep de bron schrijft | `EVALUATION FAULT: empty source amount in equal-shares allocation` / PASS | `ERROR ConsequenceException … NullPointerException` → idem | EXCLUDE (defaults) |

Een consistentieregel schrijft een kanaal dat nooit wordt ingetrokken en leest stratum-strikt (ch. 6.1); zij werd
toegevoegd na de regels en vóór de verdelingen, creaties en groepen, zonder salience. Een verdeling was geen knoop, dus
een lezer van wat zij schrijft kreeg geen plaats na haar. `454c9ab5`: een verdeling is een knoop (zij leest haar bron,
haar voorwaarde op de bron, de rato-, maximum- en volgordeleden op de ontvangers, hun populatie en haar koppeling; zij
schrijft het doel van de ontvangers en de rest van de bron) en houdt haar eigen plaats; `laadrang` kent haar plek (na de
regels, vóór de creaties). Een consistentieregel komt na elke schrijver van wat haar assertie leest en wat die schrijver
wacht (`consistentieSalience`, toegepast in `consistentie()`). Getuigerijen
`een_consistentie_over_een_lid_uit_een_defaultsgroep`, `een_consistentie_over_de_koppeling_van_een_creatie`,
`een_leegtoets_op_wat_een_verdeling_schrijft`.

### Breder nagemeten

De 94 vormen van de vorige bouwer van de keten (`/tmp/v2/probe/vtr-bouw/vorm/`) en de 63 van de creatievolgorde
(`/tmp/v2/probe/ocv-vervolg/vorm/`), met beide jars in beide takken: alleen x-leeg-lezer-eerst,
x-leeg-lezer-eerst-een-rol en f-lezer-onder-de-creatie veranderen, FAIL → PLAYED; de teksttak is gelijk; drie vormen
weigert de frontend, zoals vóór. De drie vormen van `/tmp/v2/probe/v1/` (eigen operand onder een kwantor) spelen vóór en
na.

### Sweep

Over alle 289 goldens, elke golden met al zijn cases, statusregel per golden en per tak, jar van de claim tegen de jar
na `17e623b8` en na `454c9ab5`: alle 578 regels gelijk, exectak PLAYED 234, DRL-tak PLAYED 192. Geen gepinde uitkomst
verandert.

Saliences (`tool/ToonSalience.java` op beide jars, alle regels van alle goldens): tien goldens krijgen een lagere
salience voor een lezer die nu na zijn schrijver komt; geen status verandert.

| golden | regel: oud → nieuw | waarom |
|---|---|---|
| jutterswet-havenketen | registerdatum 0 → -1 | gevuld-toets over een keten waarvan de feitcreatie werving de relatie legt |
| jutterswet-meldingsketen | aanmelddatum van een vondst 0 → -1; erkenning van de melder 0 → -1; tijdige aanmelding 0 → -1; ongemelde vondst -1 → -2 | gevuld-toets en kenmerkvergelijking op de erkende meldingsdatum uit een groep; de ontkenning van erkend volgt |
| jutterswet-opgave-subtype | afwijking opgave meldingsdatum 0 → -1; controle opgave meldingsdatum 0 → -1 | de kenmerktoekenning en de consistentieregel die de opgavemarkering maakt, lezen de berekende waarde uit een groep |
| jutterswet-opgave-vormen | afwijking opgave waarde/wachtgeld 0 → -1; controle opgave waarde/wachtgeld 0 → -1 | idem |
| jutterswet-sluitdag-gemeld | afwijking opgave uiterste meldingsdag 0 → -1; controle opgave uiterste meldingsdag 0 → -1 | idem |
| jutterswet-overzeelezing-catala | kenmerktoekenning overzees 0 → -1; defaults A:Vondstaanslag.heffing 0 → -1 | het kenmerk vergelijkt de registratiezone die de creatie zet; de groep die het kenmerk leest volgt |
| jutterswet-zwaarste-vondst | gedateerd 0 → -1; losse gewicht -1 → -2 | gevuld-toets op de afgeleide dag uit een groep; de ontkenning van gedateerd volgt |
| anchor/jutterswet-wachtersketen, jutterswet-wachtersketen-rgs, synth2-budgetgrens-onder | rangdetector 0 → -1 | de consistentieregel leest de rang die de recursieve creatie zet |

### README (`8c220ac8`)

`backends/drools/README.md`: een nieuwe sectie "Recorded: a reader after the writers of what it reads" (welke lezingen
de exectak ordent, welke schrijvers, de weigering op een kring, de begeleiders en de consistentieregel, wat open blijft
en het kanaal van de teksttak). De leeg-toets in "Recorded: a condition that reads through a role", de grenzen in
"Recorded: a fold whose subselection tests a value derived later" en de alinea over de creatie in `groepVolgorde` wijzen
ernaar en zeggen wat sinds deze ronde gesloten is. De teksttak, gemeten en beschreven: een leeg-toets en een
vergelijking in een kenmerktoekenning vóór hun schrijver vuren op de tijdelijke stand; een toets één rol ver ziet de
verre kant niet; een telling van gemaakte objecten of van de koppelingen van een feitcreatie leest 0; een leeg-toets op
het doel van een verdeling vuurt eerst; een voorwaarde over twee rollen weigert zij met haar marker en een document met
een defaultsgroep aan de deur; een consistentieregel over de koppeling van een creatie speelt daar.

### Gates

Alle zestien met `/tmp/v2/uchar-gates20/run.sh` op de kop `535c1487` (de laatste commit vóór dit Result; de
Result-commit raakt alleen de kaart), `dune build` exit 0, elk script onder zijn shebang; logs in
`/tmp/v2/uchar-gates20/`. Alle zestien exit 0. Eerder ook op `8c220ac8` (vóór de commentaarcommit; logs in
`/tmp/v2/uchar-gates20/kop-8c220ac8/`), met dezelfde tellingen. Gate 10: `JUnit suite green (1044 tests, floor 173); 116
rgs golden(s) re-emit byte-identically (floor 80)`. Gate 12: `check-drools: PASS`, `played 234` (pin 234), `DRL-text
branch played 192` (pin 192), `JUnit 1019 tests, floor 239`. Gate 16: `23 scenario golden(s) (floor 7) regenerate 82
case file(s) (floor 31) and their ir.json byte-identically`.

### Golden en pinnen

Geen golden: elke gemeten vorm die deze ronde sluit, pint een getuigerij met de waarden van ref en clingo, en geen vorm
valt erbuiten. Geen voorstel voor `scripts/check-drools.sh`: `EXPECTED_PLAYED=234` en `EXPECTED_DRL_PLAYED=192` blijven.

### Afwijkingen van de opdracht

1. De ordening raakt drie vormen buiten de genoemde vormruimte, in dezelfde laag en met hetzelfde stille foute antwoord:
   de begeleiders van een regel (zij volgen de salience van hun regel), de consistentieregel (zij komt na haar
   schrijvers) en de verdeling als schrijver (zij is een knoop). Elk met een rood-eerst getuigerij.
2. Tien goldens krijgen een andere salience (tabel onder Sweep); de opdracht vroeg te controleren dat geen golden dat
   doet. Het is de gekozen ordening: elk is een lezer onder een leeg- of gevuld-toets, of een kenmerktoekenning of
   consistentieregel, na de schrijver van wat zij leest (een groep, een creatie, een feitcreatie), en wat daarop volgt.
   Geen statusregel verschuift. Eigen gevolgtrekking, niet gemeten: met de band min één in plaats van de laadvolgorde
   zouden meer regels zakken.
3. Een vergelijking in de voorwaarde van een regel die een attribuut schrijft, wordt niet geordend (de polariteit van
   ch. 6.1); b-twee-operanden-regel blijft daardoor fout waar ref en clingo gelijk antwoorden (open punt 1).
4. Een leeg-toets op een kring met de schrijver, of op het eigen doel, weigert nu benoemd; vóór speelden d-kring-leeg en
   d-zelf-alleen in de exectak een antwoord dat ref en clingo niet geven (beide weigert de verifier met WF-046).

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 5.5, "Recorded flank", eerste bullet, na de zin "The exec-model branch's navigated leeg test,
  over one role and since 2026-10-01 over a chain, has this form as well (…)": "*Closed in the exec-model branch on
  2026-10-01 for a test anywhere in a condition or a value (witness `LezerOnderSchrijverWitnessTest`): a rule is placed
  after every writer of what it reads under a leeg or gevuld test, and a characteristic assignment after every writer of
  what it reads stratum-strict (RUNTIME-SEMANTICS ch. 6.1), with every node such a writer waits on — an ordinary rule
  declared after it, a defaults group, an object or fact creation, an allocation — strictly below it in salience or
  level with it and added before the rule; a consistency rule takes the same placement, and a rule's companions take the
  rule's salience. A leeg or gevuld test on a cycle with its writer, its own target included, is refused by name
  (WF-046). A comparison in the condition of an attribute-writing rule reads positively and is not ordered: with two
  operands derived later, `ref/` and `backends/clingo` answer leeg where the branch writes, since it tests again after
  the first writer (RUNTIME-SEMANTICS ch. 6.1, conditions that hold on absence). The DRL-text branch keeps the form
  described here.*" En in de alinea erna, achter "and a salience below the operand's writers for the order-dependent
  form": "(in the exec-model branch: the far-side join since 2026-09-28, the salience since 2026-10-01)".
- `spec/IR-SPEC.md` ch. 5.5, de recorded-alinea van 2026-10-01 (creaties): "What stays open: in the exec-model branch a
  reader of a creation's output that writes irrevocably, … absent); the DRL-text branch orders no creation, so a later
  creation and a fact creation still read 0 there, a coarser order than the graph without a loud refusal." wordt
  "*Closed in the exec-model branch the same day (witness `LezerOnderSchrijverWitnessTest`):* a reader of a creation's
  output that writes irrevocably, such as a characteristic assignment over a count of the created objects or over the
  links a fact creation lays, and a leeg test on what a creation writes, wait on the creation; before, a characteristic
  was attributed where `ref/` and `backends/clingo` leave it absent. What stays open: the DRL-text branch orders no
  creation, so a later creation and a fact creation still read 0 there and a reader of their output acts on that 0, a
  coarser order than the graph without a loud refusal."
- `spec/CONSTRUCTS.md` rij 6, "Recorded open": het deel "both branches act on a transient leeg when an own-operand,
  filter or navigated reader (in the exec-model branch over one role or a chain) precedes the writer of what it tests,
  and, … ; the DRL-text branch fires an object creation before a later creation or a fact creation that writes what it
  reads, where RUNTIME-SEMANTICS ch. 6.1 places it strictly above them, and in the exec-model branch, which places a
  creation below those writers since 2026-10-01, a reader of a creation's output that writes irrevocably can still act
  before the creation (IR-SPEC ch. 5.5)." wordt "the DRL-text branch acts on a transient leeg when an own-operand,
  filter or navigated reader precedes the writer of what it tests, and on a transient value when a characteristic
  assignment or a consistency rule does, which the exec-model branch orders since 2026-10-01 (rules, defaults groups,
  creations and allocations as writers), and, in the DRL-text branch only since 2026-09-28, a navigated read in a
  condition does not react to a later far-side write in either order; the DRL-text branch fires an object creation
  before a later creation or a fact creation that writes what it reads, where RUNTIME-SEMANTICS ch. 6.1 places it
  strictly above them, and a reader of a creation's output before the creation; in the exec-model branch a comparison in
  the condition of an attribute-writing rule is not ordered, the norm question of RUNTIME-SEMANTICS ch. 6.1 (IR-SPEC ch.
  5.5)."

### Open punten

1. Normvraag voor de eigenaar: een vergelijking in de voorwaarde van een regel die een attribuut schrijft (ook een
   gelijkstelling over een telling van gemaakte objecten) leest positief (ch. 6.1, "conditions on attribute-writing
   forms read positively here"), en de exectak ordent haar niet. Twee meetbare gevolgen: (a) met een vergelijking die op
   leeg waar is ("ongelijk aan", "gelijk aan 0") vóór haar schrijver schrijven ref en de exectak, clingo niet (17 vormen
   van rasters A en C, geen arbiter); (b) met twee later afgeleide operanden antwoorden ref en clingo gelijk en schrijft
   de exectak (b-twee-operanden-regel), omdat Drools de lezer na de eerste schrijver opnieuw toetst. Wordt zo'n lezing
   negatief of stratum-strikt, dan dekt dezelfde ordening haar met één regel in `groepVolgorde` (de lezingen van de
   voorwaarde van elke regel bij `strikt`); de prijs is een lagere salience voor meer regels in het corpus.
2. De DRL-teksttak, ongewijzigd: zie de README-sectie en de DRL-kolommen; herstel zou in die tak een eigen ordening
   vragen (saliences uit haar stratificatie voor leeg-lezers, kenmerktoekenningen, creaties en consistentieregels).
3. Een verdeling waarvan een defaultsgroep de bron schrijft: ref `EVALUATION FAULT: empty source amount in equal-shares
   allocation (v0 mirror hole, ch. 4.4)`, de exectak `ERROR ConsequenceException … NullPointerException` (luid, geen
   stil fout antwoord), clingo 30 en 30. Eigen gevolgtrekking uit de NullPointerException in `BigDecimal.divide`: de
   verdeling vuurt vóór de groep op een lege bron. Niet in deze ronde.
4. Niet gemeten: een regelversiecheck ("regelversie X is inconsistent") als lezer van het kanaal van een
   consistentieregel; ref voert "is gevuurd" niet uit. De uniciteitsregel speelde in de gemeten vorm; zij is geen knoop.
5. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee.

### Commits

    50a90973 claim: vervolg lezer-onder-schrijver
    292f4f34 LezerOnderSchrijverWitnessTest: lezer vóór zijn schrijver, rood
    17e623b8 Exectak: een lezer wacht op de schrijvers van wat hij negatief leest
    ab1bd924 LezerOnderSchrijverWitnessTest: consistentie en verdeling, rood
    454c9ab5 Exectak: verdeling als schrijver, consistentie na haar schrijvers
    8c220ac8 backends/drools/README: een lezer na de schrijvers van wat hij leest
    535c1487 Exectak: commentaar bij de leeg-lezingen en de verdeling als knoop

en de commit "Vervolgronde lezer-onder-schrijver: Result", die deze sectie onderaan de done-kaart
`20260930-drools-exec-voorwaarde-twee-rollen-ver.md` zet. Niet gepusht, niet gemerged.
