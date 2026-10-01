# frontend-datum-plus-gebroken-maanden: een datum plus een duur met een breuk kapt de breuk af zonder melding

*Vondst van een regelsetbouwer, 2026-09-30, opnieuw gemeten door de
kaartschrijver op uchar `b0aab696` via de distributie `f24283cf`
(`bin/regelspraak check|scenario|cases`) en de eigen uitvoerders (ref en
clingo gebouwd uit de boom van `b0aab696`); reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/kaarten-g1/gebroken-maand/`
(a-attribuut-mnd.rgs, b-letterlijk-mnd.rgs, c-omweg-auteur.rgs,
d-letterlijk-jr.rgs, e-letterlijk-wk.rgs, f-richting-nul.rgs,
w-zonder-deling.rgs, v-vormruimte.rgs, x-afronding.rgs met hun scenario's,
cases-*/, uit/; alleen lezen). Stil: `check` exit 0 zonder WARNING, en de
drie uitvoerders geven eensgezind de afgekapte datum. Op verzoek van de
eigenaar geschreven (2026-09-30); geen bouwdatum.*

## Aanleiding

Een auteur schrijft "zijn startdag plus zijn duur" met een duur
`Numeriek (getal met 1 decimalen) met eenheid mnd` van 20,5 mnd en rekent
op de halve maand. De frontend neemt de vorm aan en schrijft de breuk in
het IR: de duur heeft type `whole: false, decimals: 1`, een literal
`20,5 mnd` wordt `{"number": "20.5"}`. De uitvoerders kappen de telling af:
RUNTIME-SEMANTICS ch. 3.8 zegt "A fractional duration count **truncates
toward zero** before the calendar step (1,9 maanden steps 1 month; −1,9
truncates to −1)", en volgens dezelfde sectie pint golden
`jutterswet-vervaltermijn` die afkapping. Gemeten zijn Drools-exec, ref en
clingo het eens (R1–R4; alleen de quotiëntvorm weigert clingo luid):
01-03-2026 plus 20,5 mnd wordt 01-11-2027, plus 1,5 jr wordt 01-03-2027,
plus 2,5 wk wordt 15-03-2026. De breuk verdwijnt dus in de kalenderstap
van elke uitvoerder, zoals ch. 3.8 voorschrijft; ch. 3.8 geeft een deel van
een maand, kwartaal of jaar geen andere betekenis dan dat het wegvalt. Stil
is de frontend: zij neemt een zin aan waarvan de uitkomst iets anders zegt
dan de zin, en `check` meldt niets.

Wie het merkt, schrijft een omweg. De auteur schrijft nu "(zijn duur naar
beneden afgerond op 0 decimalen) plus 15 dg" (R6); die vorm telt 15 dg op
bij elke duur, ook bij een hele. Voor een negatieve duur verschillen de
twee afrondingen: `naar beneden afgerond op 0 decimalen` geeft −21 mnd, de
afkapping van ch. 3.8 −20 mnd; `richting nul afgerond op 0 decimalen` geeft
precies de afkapping (R5).

Standaardweg (eigenaar): een stil afwijkend antwoord mag nooit. Omdat ch.
3.8 de breuk alleen laat wegvallen, weigert de frontend de vorm benoemd,
met de uitdrukbare vorm in de zin, zodat de zin zegt wat de uitvoerders
doen. Alternatief: een gedefinieerde omrekening van de breuk (bijvoorbeeld
naar dagen); die verandert ch. 3.8 en de golden en vraagt eerst een
spec-voorstel.

## Minimale reproductie

`b0aab696`, distributie `f24283cf` (`bin/regelspraak check|scenario|cases`);
ref via `_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`, beide gebouwd op
`b0aab696`.

    Eenheidsysteem Tijd
        de dag dg
        de week wk = 7 dg
        de maand mnd
        het jaar jr = 12 mnd

    Objecttype de Vergunning (mv: Vergunningen)
        de startdag Datum in dagen;
        de duur Numeriek (getal met 1 decimalen) met eenheid mnd;
        de einddag Datum in dagen;

    Regel einddag
        geldig altijd
            De einddag van een Vergunning moet berekend worden als zijn startdag plus zijn duur.

    Scenario: Vergunning R1 (startdag 01-03-2026, duur 20,5 mnd); verwacht: einddag 16-11-2027
    (de halve maand als 15 dg, de lezing van de auteur).

    R1  a-attribuut-mnd.rgs, zoals hierboven.
        nu: check exit 0, geen WARNING ("[BEREKENING] De einddag van een Vergunning := zijn startdag
        plus zijn duur :: Datum in dagen"); scenario "verwachting 1 FOUT: attributeValue R1 einddag
        (verwacht=2027-11-16, werkelijk=2027-11-01)"; ref "MISMATCH: R1.einddag = 2027-11-01, expected
        2027-11-16"; clingo "MISMATCH: R1.einddag = [21123], expected 21138" (21123 is 2027-11-01).
    R2  b-letterlijk-mnd.rgs: `zijn startdag plus 20,5 mnd`.
        nu: identiek aan R1, in check, scenario, ref en clingo.
    R3  d-letterlijk-jr.rgs `zijn startdag plus 1,5 jr` (verwacht 01-09-2027, achttien maanden) en
        e-letterlijk-wk.rgs `zijn startdag plus 2,5 wk` (verwacht 18-03-2026, zeventien dagen).
        nu: check exit 0; scenario "(verwacht=2027-09-01, werkelijk=2027-03-01)" en "(verwacht=2026-03-18,
        werkelijk=2026-03-15)"; ref dezelfde werkelijke datums; clingo [20878] en [20527], dezelfde datums.
    R4  w-zonder-deling.rgs met scenario-w.rgs: elf vormen met de afgekapte datum als verwachting —
        een attribuut met 1 decimaal in mnd, jr, wk en dg; een literal in mnd, jr en wk; een parameter
        in mnd; `min zijn duur`; de omweg van de auteur; een hele duur als controle.
        nu: check exit 0 zonder WARNING; scenario "PASS  afgekapt"; ref "oracle: PASS (11
        expectations)"; clingo "clingo: PASS (11 expectations)". Met een quotiënt als duur (`zijn
        ronde duur gedeeld door 2`, 41 mnd; v-vormruimte.rgs) kappen exec-tak en ref ook af; clingo
        weigert het document luid: "TRANSLATION FAULT: a unit-carrying multiplicative operand is not
        translated (…)".
    R5  x-afronding.rgs, duur −20,5 mnd: de impliciete vorm, `(zijn duur naar beneden afgerond op 0
        decimalen)` en `(zijn duur richting nul afgerond op 0 decimalen)`.
        nu: check exit 0; exec-tak en ref PASS op 01-07-2024, 01-06-2024 en 01-07-2024; clingo
        "UNSATISFIABLE — a fail-fast guard fired: viol("negative operand under rounding is not
        translated","R1")" (luid, alleen door de expliciete afrondingen: de impliciete vorm alleen,
        scenario-a-negatief.rgs, geeft clingo PASS op 01-07-2024).
    R6  c-omweg-auteur.rgs `zijn startdag plus (zijn duur naar beneden afgerond op 0 decimalen) plus
        15 dg` en f-richting-nul.rgs `zijn startdag plus (zijn duur richting nul afgerond op 0
        decimalen)`.
        nu: check exit 0; PASS in exec-tak, ref en clingo op 16-11-2027 en 01-11-2027.

## Acceptatie

- (a) Meet de vormruimte: een datum plus en min een duur met een breuk, als
  attribuut, parameter, literal, quotiënt of andere berekening zonder hele
  uitkomst; in dg, wk, mnd, kw en jr op een `Datum in dagen` en in u, min,
  sec en ms op een datum-tijd; positief en negatief; in de waarde van een
  regel, in een voorwaarde, en in een rechtvaardiging en een consequentie
  van een default; en als vormen die moeten blijven spelen: een hele duur,
  een literal zonder breuk en een duur die op 0 decimalen is afgerond (R6).
- (b) De frontend weigert bij `check` benoemd een datum plus of min een duur
  waarvan de duur statisch een breuk kan dragen (een type dat niet heel is,
  een literal met een breuk, een quotiënt), in de woorden van de auteur: welke
  duur, in welke eenheid, dat de uitvoerders naar nul afkappen (ch. 3.8), en
  de uitdrukbare vormen: `(… richting nul afgerond op 0 decimalen)` voor de
  afkapping, of het deel in een kleinere eenheid erbij (`plus … dg`). De wire
  en de uitvoerders veranderen niet: een handgeschreven IR-document met een
  gebroken telling houdt de betekenis van ch. 3.8. Rood-eerst JUnit-getuige
  met R1, R2 en R3. Kiest de eigenaar de omrekening, dan eerst een
  spec-voorstel op ch. 3.8 en daarna de uitvoerders in eigen kaarten.
- (c) Negatief-cases in het corpus (werknaam `jutterswet-gebroken-maand`)
  voor R1 en R2 met `expect.txt`, zodat gate 4 de weigering pint; de pins
  van gate 4 en 10 als **voorstel** waar zij verschuiven.
- (d) Corpussweep: meet welke bron in het corpus een gebroken duur schrijft
  (golden `jutterswet-vervaltermijn` pint de afkapping, ch. 3.8); zo'n bron
  gaat naar de uitgeschreven vorm met ongewijzigde verwachtingen en
  ongewijzigde uitkomst in de drie uitvoerders, als **voorstel**; verder
  ongewijzigd.
  Alle zestien gates groen (bash waar de shebang dat zegt). DIVERGENCES: een
  sectie over de weigering (wat de frontend weigert, de uitgeschreven
  vormen, de verwijzing naar ch. 3.8 en naar de golden die de afkapping in
  het IR pint).

May touch: `frontends/regelspraak/**`, `corpus/negatief/<nieuw>/`, de
`bron.rgs` van een golden die de vorm schrijft (alleen de omzetting,
**voorstel**), `scripts/check-*.sh` (pins, **voorstel**), kaartverplaatsing.
Niet: `ref/`, `backends/**`, `spec/` (ch. 3.8 blijft; alleen bij de
omrekening eerst een spec-voorstel). NB: geen bouwdatum; de regelset heeft
haar omweg (R6) en wacht niet op deze kaart. Onafhankelijk van de
Drools-kaarten (andere laag). De luide clingo-weigeringen onder R4 en R5
(een quotiënt met eenheid, een negatieve operand onder een afronding) zijn
grenzen van de ASP-vertaling buiten deze kaart.

## Result (2026-10-01, bouwer)

Tak `card/frontend-datum-plus-gebroken-maanden` in de worktree van de kaart, afgetakt van main `a8b9c7e5`; niet
gerebased, niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo: de frontend (`Resolver --ir
out.json bron.rgs` als `check`, en `Resolver --ir … --cases … bron.rgs scenario.rgs`),
`_build/default/spec/verifier/bin/verifier.exe`, `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe` (gebouwd met `dune build` op deze kop) en de Drools-jar van gate
12 (`java -jar <jar> ir.json case…`, `--drl` voor de teksttak). "Vóór" is de frontend van de claim `682ecc37` (code
gelijk aan main), "na" die van `1333c06f`, "eind" die van `82e3537a`, de laatste commit die frontendcode raakt; "na" en
"eind" geven op elk gemeten document dezelfde uitkomst. Generator, meetscripts en tabellen staan buiten de repo:
`/tmp/v2/probe/gbm-bouw/tools/` (`maak.py`, `meet.sh`, `meet1.sh`, `tabel.py`, `tabel2.py`, `sweep.py`), per document
`vorm/<naam>/{voor,na,eind}/meting.txt`, de tabellen `tabel.md` en `tabel-result.md`, de sweep in `sweep/`. De
reproducties zijn uit `/tmp/v2/probe/kaarten-g1/gebroken-maand/` gekopieerd (alleen gelezen) naar
`/tmp/v2/probe/gbm-bouw/repro/` (uitvoer in `uit-voor/` en `uit-na/`).

### De reproductie op deze kop

| | `check` vóór | `check` na | verifier (IR van vóór) | ref (vóór) | clingo (vóór) | exectak (vóór) | DRL-tekst (vóór) |
|---|---|---|---|---|---|---|---|
| R1 a-attribuut-mnd | exit 0, `ir: objecttypen=1 feittypen=0 resolvedRegels=1 geschreven naar …`, `"diagnostics": []` | exit 1, één ERROR (zin hieronder) | `PASS` | `MISMATCH: R1.einddag = 2027-11-01, expected 2027-11-16` | `MISMATCH: R1.einddag = [21123], expected 21138` | `FAIL case1-halve-maand.json: attribuut einddag: verwacht 2027-11-16, werkelijk 2027-11-01` | idem |
| R2 b-letterlijk-mnd | idem | exit 1, één ERROR | `PASS` | idem R1 | idem R1 | idem R1 | idem |
| R3 d-letterlijk-jr | idem | exit 1, één ERROR | `PASS` | `R1.einddag = 2027-03-01, expected 2027-09-01` | `[20878], expected 21062` | `verwacht 2027-09-01, werkelijk 2027-03-01` | idem |
| R3 e-letterlijk-wk | idem | exit 1, één ERROR | `PASS` | `R1.einddag = 2026-03-15, expected 2026-03-18` | `[20527], expected 20530` | `verwacht 2026-03-18, werkelijk 2026-03-15` | idem |
| R4 w-zonder-deling (11 regels, verwacht: afgekapt) | exit 0 | exit 1, 9 ERRORs (de omweg van R6 en de hele duur spelen) | `PASS` | `PASS (11 expectations)` | `PASS (11 expectations)` | `PLAYED 1` | `PLAYED 1` |
| R4 v-vormruimte (12 regels, met het quotiënt) | exit 0 | exit 1, 10 ERRORs | `PASS` | `PASS (12 expectations)` | `TRANSLATION FAULT: a unit-carrying multiplicative operand is not translated (…)` | `PLAYED 1` | `EXCLUDE drl compile: … rsDatumReken(… rsGedeeld(…` |
| R5 x-afronding (−20,5 mnd) | exit 0 | exit 1, één ERROR (de impliciete vorm; beide afrondingen spelen) | `PASS` | `PASS (3 expectations)` | `UNSATISFIABLE — a fail-fast guard fired: viol("negative operand under rounding is not translated","R1")` | `PLAYED 1` | `PLAYED 1` |
| R6 c-omweg-auteur, f-richting-nul | exit 0 | exit 0, IR byte-gelijk aan vóór | `PASS` | `PASS` | `PASS` | `PLAYED 1` | `PLAYED 1` |

De zin voor R1 na de fix, letterlijk:

    ERROR (line 14): datum plus duur 'zijn startdag plus zijn duur': de duur 'zijn duur' is een getal met 1
    decimalen, en de uitvoerders kappen een gebroken telling in mnd naar nul af vóór de kalenderstap
    (RUNTIME-SEMANTICS ch. 3.8: 1,9 mnd telt als 1 mnd en -1,9 mnd als -1 mnd); schrijf de afkapping uit als 'zijn
    startdag plus (zijn duur richting nul afgerond op 0 decimalen)', of neem het deel in een kleinere eenheid mee
    ('zijn startdag plus (zijn duur richting nul afgerond op 0 decimalen) plus … dagen')

en voor R2 `… de duur '20,5 mnd' heeft een breuk, en de uitvoerders kappen een gebroken telling in mnd naar nul af
vóór de kalenderstap (RUNTIME-SEMANTICS ch. 3.8: 20,5 mnd telt als 20 mnd); schrijf de afkapping uit als 'zijn
startdag plus 20 mnd', of neem het deel in een kleinere eenheid mee ('zijn startdag plus 20 mnd plus … dagen')`. Met
het scenario (`--cases`) schrijft de frontend na de fix niets en eindigt met exit 1 op dezelfde ERROR.

### Oorzaak

1. `Typering.reken` geeft een datum plus of min een getal met een tijdseenheid of een dagsoort als eenheid het type van
   de datum, zonder te kijken of de telling heel is. De IR draagt de breuk (`whole: false, decimals: 1`, of
   `{"number": "20.5"}`), de verifier neemt het document aan (RUNTIME-SEMANTICS ch. 3.8 geeft het betekenis) en elke
   uitvoerder kapt de telling af vóór de kalenderstap. Geen controle in de frontend keek ernaar.
2. Ook de statische typering zelf zegt het niet altijd: een som van twee hele getallen heet heel (ch. 2.3), ook waar
   de rechter operand naar een grotere eenheid wordt omgerekend. Gemeten: `zijn startdag plus (1 jr plus 6 mnd)` en
   `zijn startdag plus (2 wk plus 3 dg)` waren stil in `check`; ref en de Drools-exectak gaven 01-03-2027 en 15-03-2026
   (1,5 jr en 2 3/7 wk afgekapt), clingo weigerde luid (`non-identical units under an additive join are not
   translated`). Omgekeerd heet een product van twee hele getallen niet heel, terwijl het heel is.

### Fix en de gekozen weg

`ExpressieBouwer` (+219, −2 regels, commentaar inbegrepen, met de afronding van afwijking 4) en `RuleChecker` (+30,
de WARNING van afwijking 5); de draad, de emitter, de verifier en de uitvoerders zijn niet aangeraakt.

- `binair` keurt, nadat `binairExpr` een datum plus of min een duur getypeerd heeft (de uitkomst is een datum), de
  rechter operand met `gebrokenDuurFout`. Daar staan beide operanden nog als de auteur ze schreef (`src(l)`, `src(r)`).
- `breuk(Expr)`, uitputtend over de verzegelde unie van `Expr`, zoekt op de vorm (zonder casuswaarde) de eerste plek
  die een breuk binnenbrengt: een literal met een breuk (`Rationaal`); een lezing van een attribuut of parameter
  waarvan het type niet heel is (met decimalen, of het onbegrensde "getal"); een deling (ook de abs-variant), een
  percentage, een macht, een afronding op 1 of meer decimalen, een totaal, een tijdsevenredig deel, een wortel; in een
  som, verschil, minimum, maximum, eerste, laatste, absolute waarde of begrenzing de operanden en een omrekening naar de
  eenheid van de uitkomst waarvan de factor niet heel is (`omrekening`; een literal waarvan de omgerekende waarde heel
  is, `12 mnd` in jr, gaat door); in een product de operanden en de omrekening van een hoeveelheid naar de noemer van
  een tarief; via een variabele haar definitie. Heel zijn: een telling, een tijdsduur, een aantal dagen, een
  datumdeel, een afronding op 0 decimalen; een marker maakt geen claim.
- De ERROR noemt de som en de duur in de woorden van de auteur (één paar buitenste haakjes eraf), de plek en de reden
  van de breuk, de eenheid, de afkapping met het voorbeeld van ch. 3.8 in die eenheid (bij een literal: de literal en
  haar hele deel), de afkapping uitgeschreven (`(… richting nul afgerond op 0 decimalen)`, bij een literal het hele
  deel) en het deel in de eerstvolgende kleinere eenheid (jr en kw: `maanden`; mnd en wk: `dagen`; dg op een
  datum-tijd: `uur`; u: `minuten`; minuut: `seconden`; sec: `milliseconden`), of de zin dat er geen kleinere is (dg op
  een Datum in dagen, ms, een dagsoort). Een herhaalde oplossing van dezelfde tekst op dezelfde regel (de twee passen
  van een variabelendeel) geeft één ERROR.

Verworpen: alleen het gedeclareerde type keuren (`geheel`): dat weigert `zijn ronde duur maal 2` en laat
`(1 jr plus 6 mnd)` door; de controle in `Typering.reken`: de matrix ziet alleen datatypen, niet de vorm en niet de
woorden van de auteur; een WARNING: het document gaat dan naar schijf en elke uitvoerder geeft de afgekapte datum, het
stille afwijkende antwoord dat de kaart uitsluit; de waarde van de casus keuren: `check` beslist statisch, en ch. 3.8
blijft de betekenis van een gebroken telling op de draad. De omrekening (alternatief van de kaart) is niet gekozen.

### (a) Vormruimte, vóór en na

136 gegenereerde documenten, 1168 vormen (`maak.py`): per bron van de duur, positie en operator één document met één
regel per eenheid, en twee scenario's (positief en negatief: 2,5 en −2,5; 5 en −5 voor de hele attributen) met als
verwachting de afgekapte datum van ch. 3.8. Eenheden: dg, wk, mnd, kw, jr op `startdag` (Datum in dagen) en u, minuut,
sec, ms op `begintijd` (Datum en tijd in millisecondes), met de stelsels `Tijd` (dg, wk = 7 dg, mnd, kw = 3 mnd, jr =
12 mnd) en `Klok` (ms, sec = 1000 ms, minuut = 60 sec, u = 60 minuut). De minuut heet `minuut`, omdat `min` als
operator gelezen wordt (`met eenheid min` en `30 min.` zijn syntaxfouten; open punt 2). Posities: de waarde (`De
<x>einde van een Vergunning moet berekend worden als <datum> <op> <duur>.`), de voorwaarde (`… moet gesteld worden op 1
indien zijn <x>peil later is dan <datum> <op> <duur>.`), de rechtvaardiging van een default (dezelfde voorwaarde in
`Uitzondering op <x>status`) en de consequentie van een default (de waarde in `Uitzondering op <x>einde`, `indien
<datum> gevuld is`). Operator plus en min. Per bron 8 documenten (4 posities × 2 operatoren), 16 casebestanden.

| bron van de duur | vormen | vóór: check | vóór: ref | vóór: clingo | vóór: exec / DRL | na |
|---|---|---|---|---|---|---|
| attribuut, getal met 1 decimalen | 72 | exit 0 zonder diagnose: 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec PLAYED 8/8; DRL PLAYED 4/8 (EXCLUDE defaults 4) | exit 1, één ERROR per vorm: 8/8 |
| attribuut, getal (onbegrensd) | 72 | idem 8/8 | PASS 16/16 | PASS 0/16; luid: unscalable datatype 4, is not a whole number 12 | exec 8/8; DRL 4/8 (defaults 4) | exit 1, één ERROR per vorm: 8/8 |
| parameter, 1 decimaal | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 4/8 (defaults 4) | exit 1, één ERROR per vorm: 8/8 |
| literal 2,5 | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 4/8 (defaults 4) | exit 1, één ERROR per vorm: 8/8 |
| literal −2,5 | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 4/8 (defaults 4) | exit 1, één ERROR per vorm: 8/8 |
| quotiënt (ronde duur gedeeld door 2) | 72 | idem 8/8 | PASS 16/16 | PASS 0/16; luid: unit-carrying multiplicative operand 16 | exec 6/8 (SKIP 2: deling in een consequentie van een default); DRL 0/8 (defaults 4, compile 4) | exit 1, één ERROR per vorm: 8/8 |
| percentage (50% van ronde duur) | 72 | idem 8/8 | PASS 16/16 | PASS 0/16; luid: unit-carrying multiplicative operand 16 | exec 8/8; DRL 0/8 (defaults 4, compile 4) | exit 1, één ERROR per vorm: 8/8 |
| afgerond op 1 decimalen | 72 | idem 8/8 | PASS 16/16 | PASS 0/16; luid: unscalable datatype 4, is not a whole number 12 | exec 8/8; DRL 4/8 (defaults 4) | exit 1, één ERROR per vorm: 8/8 |
| product met 1,5 | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 0/8 (defaults 4, compile 4) | exit 1, één ERROR per vorm: 8/8 |
| omrekening naar een grotere eenheid (`(zijn ronde jaarduur plus 6 mnd)`, kw + 1 mnd, wk + 3 dg, u + 30 minuut, minuut + 30 sec, sec + 500 ms) | 48 | idem 8/8 | PASS 16/16 | PASS 0/16; luid: non-identical units under an additive join 16 | exec 8/8; DRL 0/8 (defaults 4, compile 4) | exit 1, één ERROR per vorm: 8/8 |
| heel attribuut | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 4/8 (defaults 4) | exit 0, IR byte-gelijk en uitkomsten gelijk: 8/8 |
| literal 20 | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 4/8 (defaults 4) | exit 0, IR en uitkomsten gelijk: 8/8 |
| literal 20,0 | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 4/8 (defaults 4) | exit 0, IR en uitkomsten gelijk: 8/8 |
| richting nul afgerond op 0 decimalen | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 4/8 (defaults 4) | exit 0, IR en uitkomsten gelijk: 8/8 |
| naar beneden afgerond op 0 decimalen (−2,5 geeft −3) | 72 | idem 8/8 | PASS 16/16 | PASS 6/16; luid: unscalable datatype 4, negative operand under rounding 6 | exec 8/8; DRL 4/8 (defaults 4) | exit 0, IR en uitkomsten gelijk: 8/8 |
| product met 2 | 72 | idem 8/8 | PASS 16/16 | PASS 12/16; luid: unscalable datatype 4 | exec 8/8; DRL 0/8 (defaults 4, compile 4) | exit 0, IR en uitkomsten gelijk: 8/8 |
| omrekening naar een kleinere eenheid (`(zijn ronde maandduur plus 1 jr)`, dg + 1 wk, minuut + 1 u, sec + 1 minuut, ms + 1 sec) | 40 | idem 8/8 | PASS 16/16 | PASS 0/16; luid: non-identical units under an additive join 16 | exec 8/8; DRL 0/8 (defaults 4, compile 4) | exit 0, IR en uitkomsten gelijk: 8/8 |

Telling: vóór schreef `check` alle 136 documenten zonder diagnose, en ref gaf op alle 272 casebestanden de afgekapte
datum; clingo en beide Drools-takken gaven dezelfde datum waar zij antwoordden, en weigerden verder luid (de
`unscalable datatype` van clingo geldt een datum-tijd als doel in de consequentie van een default, ook bij een hele
duur: hetzelfde document met alleen de dagprecisie speelt in clingo). Na: de 80 documenten met een bron van een breuk
weigeren met precies één ERROR per vorm (696 vormen, 696 zinnen `datum plus|min duur '…'`); de 56 met een hele vorm
schrijven byte voor byte dezelfde IR en geven in alle uitvoerders dezelfde uitkomst. "Eind" (`82e3537a`) is op alle
136 documenten gelijk aan "na".

Buiten de vier posities, één document per vorm, vóór stil en na geweigerd met dezelfde zin: een cel van een
beslistabel, de toekenning van een objectcreatie (`Een Jutter heeft een vergunning met … einddag gelijk aan zijn
startdag plus zijn duur.`), een consistentie (`De einddag van een Jutter moet later zijn dan zijn startdag plus zijn
duur.`) en een som als duur (`zijn startdag plus de som van de duur van alle vergunningen van de Jutter`). Een
variabele draagt de breuk van haar definitie (getuige).

### (b) Getuige

`GebrokenDuurTest` (frontend, 15 tests, model met `Vergunning` in de klasse): `r1_een_attribuut_met_een_decimaal_in_maanden`,
`r2_een_literal_met_een_breuk_in_maanden`, `r3_een_literal_met_een_breuk_in_jaren_en_in_weken`,
`elke_eenheid_op_haar_datum` (dg, wk, kw, jr op de dag; dg, u, minuut, sec, ms op de datum-tijd; literals in dg en u),
`een_dagsoort_als_eenheid`, `plus_en_min` (ook `-20,5 mnd`), `elke_bron_van_een_breuk` (parameter, onbegrensd getal,
deling, percentage, afronding op 1 decimalen, product met 1,5), `een_omrekening_naar_een_grotere_eenheid`
(`1 jr plus 6 mnd`, `… plus 3 dg` in wk), `een_variabele_draagt_de_breuk_van_haar_definitie`,
`een_definitie_in_twee_passen_is_een_weigering`, `elke_positie` (voorwaarde, rechtvaardiging en consequentie van een
default), `een_afronding_over_een_datum_weigert` (afwijking 4), en drie flankrijen die op de claim al groen waren en
het moeten blijven: `een_hele_duur_blijft_spelen` (heel attribuut, `20 mnd`, `20,0 mnd`, min, product met 2,
`(zijn ronde duur plus 1 jr)`, `(1 jr plus 12 mnd)`, `(2 wk plus 7 dg)`, `zijn startdag plus 1 jr plus 6 mnd`, een
tijdsduur, `plus 26 uur`), `een_op_nul_decimalen_afgeronde_duur_blijft_spelen` (R6) en `de_voorgestelde_vormen_spelen`
(elke vorm die de weigering voorstelt).

Rood op de claim (`b4ed9f21`, de eerste 14 rijen, tegen de klassen van `682ecc37`): 14 tests, 11 rood, elk met
`but was: <[]>`, de drie flankrijen groen; bijvoorbeeld

    r1_een_attribuut_met_een_decimaal_in_maanden() ✘ expected: <[datum plus duur 'zijn startdag plus zijn duur': de
    duur 'zijn duur' is een getal met 1 decimalen, en de uitvoerders kappen een gebroken telling in mnd naar nul af
    vóór de kalenderstap (…)]> but was: <[]>

De vijftiende rij (`f2598667`) was rood op `09a4bb64` (`but was: <[]>`) en is groen na `82e3537a`. Na de fix: de klasse
15 tests, 0 failures; de suite 1044 tests, 0 failures (1025 op de claim).

### (c) Negatief-cases: niet gemaakt (afwijking 1)

Een negatief-case van gate 4 is een `corpus/negatief/wf###-<slug>/ir.json` die de verifier weigert op de regel uit de
mapnaam, met in `expect.txt` een stuk van zijn zin. De IR van R1 en R2 is verifier-schoon: de verifier geeft op de
emissie van vóór `verifier: PASS`, omdat ch. 3.8 de gebroken telling betekenis geeft en de normkeuze de draad en de
uitvoerders ongemoeid laat. Zo'n map zou gate 4 rood maken (`negatief case … passed the verifier`), en er is geen
WF-regel om haar naar te noemen. De weigering is gepind in gate 10 (de getuige hierboven, ook de zin letterlijk); de
betekenis op de draad blijft gepind door `jutterswet-vervaltermijn` in gate 5, 6 en 12.

### Pinvoorstellen

Geen. Gate 4 (`scripts/check-verifier.sh`): `EXPECTED_MIN_NEGATIEF=350`, `EXPECTED_MIN_EXPECT=260`,
`EXPECTED_MIN_POINTERS=547` zijn ondergrenzen; gemeten 503, 346, 607, ongewijzigd. Gate 10
(`scripts/check-regelspraak.sh`): `EXPECTED_MIN_TESTS=173`, `EXPECTED_MIN_GOLDENS=80`; gemeten 1044 tests en 116
rgs-goldens. Gate 12: `EXPECTED_PLAYED=234` en `EXPECTED_DRL_PLAYED=192` gemeten 234 en 192; geen golden veranderde.
Geen beschermd script is aangeraakt.

### (d) Sweep

- `sweep.py` over de 329 ir.json van `corpus/golden` en `corpus/rood`: 32 knopen datum plus of min een duur in 20
  documenten. Eén draagt een breuk: `jutterswet-vervaltermijn`, regel `Berekening respijtdatum`, `zijn aangiftedatum
  min 1,9 maanden` (`{"number": "1.9"}`, `whole: false, decimals: 1`). De andere 31 zijn heel (16 literals, 14
  attributen, 1 aggregatie). In `corpus/negatief` (503 documenten) 11 zulke knopen, geen met een breuk; in de
  pariteitszone 3 in 30 documenten, alle heel.
- `jutterswet-vervaltermijn` heeft geen `bron.rgs` en heeft er nooit een gehad: zij is sinds `8871a5e6` een
  handgeschreven IR-document, sindsdien alleen mechanisch naar de L1–L5-vorm gebracht. Er is dus geen bronvorm om om te
  zetten; zij blijft zoals zij is en pint de afkapping op de draad: 31-01-2024 min 1,9 maanden geeft 31-12-2023
  (`case1-klemmaand`), 31-01-2025 geeft 31-12-2024 (`case2-gewoonjaar`), in ref en clingo `PASS` op alle drie de
  casebestanden, Drools-exec `PLAYED 3`, DRL-tekst `PLAYED 3`. Gate 10 leest haar niet (geen `bron.rgs`); de naam die
  ch. 3.8 noemt verandert niet, dus geen registertekst voor die verwijzing.
- Geen `bron.rgs` in het corpus schrijft een gebroken duur: de 116 rgs-goldens worden byte-gelijk opnieuw uitgegeven
  (gate 10), de 45 fragmenten van de pariteitszone (gate 11) en de 23 scenario-goldens met hun 82 casebestanden en ir.json
  (gate 16) ook. Geen bron omgezet.

### DIVERGENCES (`4fd43491`)

`frontends/regelspraak/grammar/DIVERGENCES.md`, nieuwe sectie "A date plus or min a duration that can carry a fraction —
refused": wat de resolver weigert (de bronnen van een breuk en de posities), de dagsoort als eenheid, de variabele; wat
de weigering zegt en de uitgeschreven vormen per eenheid; wat blijft spelen; de verwijzing naar RUNTIME-SEMANTICS ch.
3.8 en naar `jutterswet-vervaltermijn`, die de afkapping als IR zonder `bron.rgs` pint; de afronding over een waarde die
geen getal is; de meting. De lijst in "Producer refusals without a reader sentence" noemt beide weigeringen. In "An
undeclared attribute read" staat nu, waar de declaratie zonder diagnose won, de WARNING (aanvulling 1).

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates17/run.sh` op de kop `4fd43491` (de laatste
commit vóór dit Result; de commit met dit Result raakt alleen de kaart), met een schone werkboom: `eval $(opam env)`,
`dune build` (exit 0), dan elk script onder zijn shebang. Logs in `/tmp/v2/uchar-gates17/`. Per gate de exitcode en de
PASS-regel:

     1. check-self-contained: exit 0 — check-self-contained: PASS
     2. check-ir-schema: exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 329 file(s) validate and round-trip, floor 192; …)
     3. check-case-schema: exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 825 case file(s) validate)
     4. check-verifier: exit 0 — check-verifier: PASS (329 golden clean; 503 negatief rejected for their declared rule, floor 350; 346 on their declared arm (expect.txt), floor 260; 607 cited pointers resolve in their documents, floor 547)
     5. check-oracle: exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 783 cases replayed exactly, floor 427; NOT replayed, no case file yet: …)
     6. check-clingo: exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 783 cases replay exactly through clingo — ref = clingo over the corpus)
     7. check-noop: exit 0 — check-noop: PASS (739 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
     8. check-rood: exit 0 — check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26; 84 pinned fragments cite none of the 36 listed bottom sentences)
     9. check-catala: exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10. check-regelspraak: exit 0 — check-regelspraak: PASS — JUnit suite green (1044 tests, floor 173); 116 rgs golden(s) re-emit byte-identically (floor 80)
    11. check-toka-parity: exit 0 — check-toka-parity: PASS
    12. check-drools: exit 0 — check-drools: PASS
    13. check-reftype-derivation: exit 0 — check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    14. check-flogic: exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    15. check-graaf: exit 0 — check-graaf: PASS
    16. check-scenario: exit 0 — check-scenario: PASS — 23 scenario golden(s) (floor 7) regenerate 82 case file(s) (floor 31) and their ir.json byte-identically

Gate 12 verder: `played 234`, `auto-excluded 52`, `channel floor 55 non-PLAYED goldens pinned by name; played matches
the pinned 234`, `DRL-text branch played 192 (… pin 192)`, `JUnit 985 tests, floor 239`. Gate 11: `re-emission: 1
casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27`. Gate 15: `route leg —
the graph of the wire equals the graph of the source on all 116 goldens with a bron.rgs (floor 98); 23 unit tests
green (floor 23)` en `spec leg — 252 of 289 goldens compared, 183 dependencies equal to the verifier's ch. 5 edges
(floors 234 / 116), 3 listed difference(s)`.

### Afwijkingen van de kaart

1. (c) Geen negatief-cases: onder de conventies van gate 4 kan het niet (de verifier neemt de IR van R1 en R2 aan, zie
   boven). De weigering is gepind in gate 10.
2. De weigering dekt meer dan "een type dat niet heel is, een literal met een breuk, een quotiënt": ook een percentage,
   een macht, een afronding op decimalen, een totaal, een tijdsevenredig deel, een wortel, een product met een factor
   die een breuk kan dragen, en een omrekening naar een grotere eenheid (`1 jr plus 6 mnd`), die vóór stil was in
   `check`, ref en de Drools-exectak. Een dagsoort als eenheid weigert op dezelfde manier (ch. 3.8 kapt af vóór de
   wandeling). Buiten de vier posities van (a) weigert zij ook in een beslistabel, een objectcreatie, een consistentie
   en via een variabele: alle gaan door dezelfde bouwer.
3. De kleinere eenheid in de voorgestelde vorm is gespeld als woord (`… plus … dagen`, `maanden`, `uur`, `minuten`,
   `seconden`, `milliseconden`), niet als `dg` zoals in de kaart: `min` wordt als operator gelezen, en `dagen` leest ook
   waar het document `dg` niet declareert.
4. Gevonden in de vormruimte, in deze ronde gefikst (regel van de eigenaar), rood eerst: een afronding over een waarde
   die geen getal is. Wie de haakjes van de voorgestelde vorm weglaat (`zijn startdag plus zijn ronde duur richting nul
   afgerond op 0 decimalen`), rondt de datum af. Vóór: `check` exit 0, een `Rounding` over een `Date` op de draad,
   verifier `PASS`; ref `EVALUATION FAULT: afronding: expected a number, got 2026-08-01`, clingo `PASS` met de
   onafgeronde datum, Drools-exec `ERROR ConsequenceException: … kan accumulate-resultaat niet naar BigDecimal omzetten:
   class java.time.LocalDate`, DRL-tekst `EXCLUDE drl compile`. Na: `afronding 'zijn startdag plus zijn ronde duur
   richting nul afgerond op 0 decimalen': 'zijn startdag plus zijn ronde duur' is een datum, en een afronding rondt een
   getal af; rond de duur af: 'zijn startdag plus (zijn ronde duur richting nul afgerond op 0 decimalen)'`
   (`f2598667`, `82e3537a`). Geen golden en geen fragment van de pariteitszone rondt iets anders dan een getal af
   (49 afrondingen in golden, rood en pariteitszone, alle over een getal; gate 10, 11 en 16 byte-gelijk).
5. Aanvulling 1 van de eigenaar (`85afbeb2`, `09a4bb64`): een gedeclareerd attribuut met 'van' in zijn naam dat ook als
   navigatie leesbaar is, krijgt een WARNING; de declaratie blijft winnen. De wandeling is die van `lidFout`; een
   meervoudige stap beëindigt haar. Zin: `lezing 'de premie van de jutter van de Vondst': het gedeclareerde attribuut
   'premie van de jutter' van 'Vondst' wint; dezelfde woorden lezen ook als het attribuut 'premie' van 'Jutter' over de
   rol 'jutter', en die lezing geldt hier niet`. Getuige `DubbeleLezingTest` (4 tests; rood op `1333c06f`: 3 van 4, de
   flankrij groen): waarde, voorwaarde, rechtvaardiging, een en twee rollen ver, en geen waarschuwing waar de woorden
   geen rol noemen, waar de vuller het lid niet heeft, bij de navigatie zelf en bij een meervoudige stap. Gemeten op
   deze kop: geen gedeclareerd attribuut met ' van ' in de 289 golden-documenten; 15 verschillende (type, naam) in de
   pariteitszone, en geen ervan geeft de WARNING (gate 11 byte-gelijk, de WARNING zou in `diagnostics` van de IR staan);
   gate 10 en 16 byte-gelijk. Eén rij van een bestaand testbestand is aangepast:
   `OngebondenLezingTest.een_gedeclareerd_attribuut_met_van_in_de_naam_wint_van_de_navigatie` pinde
   `assertEquals(List.of(), m.diagnostics)`, wat het besluit omkeert; zij pint nu `m.errors()` leeg (de declaratie wint
   zonder weigering), en de WARNING zelf pint `DubbeleLezingTest`. Geen ander bestaand testbestand en geen golden
   gewijzigd.
6. `jutterswet-vervaltermijn` is niet omgezet en niet gesplitst: zij heeft geen `bron.rgs` (zie (d)).

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/RUNTIME-SEMANTICS.md` ch. 3.8, alinea "Operand order and precision", na "A fractional duration count
  **truncates toward zero** before the calendar step (1,9 maanden steps 1 month; −1,9 truncates to −1).": "The
  RegelSpraak frontend refuses a source sentence whose duration count can carry a fraction by its form and names this
  truncation and the forms that write it out (`frontends/regelspraak/grammar/DIVERGENCES.md`, "A date plus or min a
  duration that can carry a fraction"); the wire keeps the truncation, and golden `jutterswet-vervaltermijn`, written as
  IR, pins it."
- Geen tekst voor `spec/IR-SPEC.md` of `spec/CONSTRUCTS.md`: de draad verandert niet.

### Open punten

1. De verifier neemt een `Rounding` over een waarde die geen getal is aan, en de uitvoerders lopen op zo'n document
   uiteen (ref een fout, clingo de onafgeronde datum, Drools-exec een crash; afwijking 4). De frontend schrijft het
   niet meer; een handgeschreven IR-document kan het nog dragen. Normkeuze voor de eigenaar: een arm in de verifier
   (laag `spec/`, buiten deze kaart).
2. `min` staat in de spellingtabel van ch. 3.8 als spelling van de minuut, maar deze frontend leest het als operator:
   `met eenheid min` en `plus 30 min.` zijn syntaxfouten (luid). Normkeuze: hoe "… min …" na een getal leest.
3. Gemeten luide weigeringen van de uitvoerders, buiten deze laag (tabel van (a)): clingo weigert een datum-tijd als doel
   in de consequentie van een default (`unscalable datatype in the ASP translation`), een quotiënt of percentage met
   een eenheid, een omrekening tussen eenheden, een negatieve operand onder naar beneden afronden en een niet-hele
   invoer op een onbegrensd getal; de Drools-exectak slaat een deling in de consequentie van een default over
   (`SKIP`); de DRL-teksttak sluit de defaults en de berekende duren uit die hij niet compileert. Alle luid, geen
   stille afwijking.
4. Voor de distributie (buiten deze kaart): `bin/regelspraak check` toont de weigering en de WARNING pas na het
   verplaatsen van de submodulepin.

### Commits

    682ecc37 claim: frontend-datum-plus-gebroken-maanden
    b4ed9f21 GebrokenDuurTest: datum plus een duur met een breuk, rood
    1333c06f Resolver: datum plus of min een duur die een breuk kan dragen weigert
    85afbeb2 DubbeleLezingTest: gedeclareerd attribuut met 'van' en navigatie, rood
    09a4bb64 RuleChecker: WARNING waar een gedeclareerde 'van'-naam ook navigeert
    f2598667 GebrokenDuurTest: een afronding over een datum, rood
    82e3537a Resolver: een afronding over een waarde die geen getal is weigert
    4fd43491 DIVERGENCES: datum plus een duur die een breuk kan dragen weigert

en de commit die de kaart met dit Result naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.

## Keuring (hoofdsessie, 2026-10-01)

Eigen run van de zestien gates op `78178a6a` in de volgorde van `gates.yml`
(bash waar de shebang dat zegt), `dune build` vooraf; logs in
`/tmp/v2/uchar-gates17/`. Alle zestien exit 0: gate 4 503 negatief-cases,
346 op hun arm; gate 10 JUnit 1044, 116 rgs-goldens; gate 12
`check-drools: PASS`, played 234 (pin 234), DRL 192 (pin 192), JUnit 985;
gate 16 23 scenario-goldens, 82 casebestanden. De fix gelezen:
`ExpressieBouwer.binair` keurt na de typering van een datum plus of min een
duur de rechter operand met `gebrokenDuurFout`; `breuk(Expr)` loopt
uitputtend over de verzegelde unie en vindt de eerste plek die een breuk
binnenbrengt, met de omrekening naar een grotere eenheid als eigen bron
(`1 jr plus 6 mnd`); de zin noemt duur, eenheid, de afkapping van ch. 3.8
en de twee uitgeschreven vormen; `afrondingFout` weigert een afronding over
iets dat geen getal is; `RuleChecker.meldDubbeleLezing` geeft de WARNING
van de eigenaar. Afwijking 1 aanvaard: een negatief-case kan niet, want de
verifier neemt het IR terecht aan (ch. 3.8 geeft het betekenis); gate 10
pint de weigering. Afwijkingen 2–4 aanvaard (bredere weigering op dezelfde
grond; de kleinere eenheid als woord omdat `min` als operator leest; de
afronding over een datum was een stil uiteenlopen van de uitvoerders en is
in de ronde gefikst). Afwijking 5: de WARNING volgt het besluit van de
eigenaar van 2026-10-01; de aangepaste rij in `OngebondenLezingTest` pinde
het tegendeel. Afwijking 6: `jutterswet-vervaltermijn` heeft geen
`bron.rgs` en blijft de pin van de afkapping op de draad. De voorgestelde
registertekst is in dit landingscommit gezet in `spec/RUNTIME-SEMANTICS.md`
ch. 3.8. Open punt 1 (de verifier neemt een afronding over een niet-getal
aan; op een handgeschreven IR-document lopen de uitvoerders uiteen) is een
normkeuze voor de eigenaar, met als advies een WF-arm in de verifier; open
punt 2 (`min` als eenheid tegenover de operator) idem; 3 en 4 geen kaart.
Commitkoppen tot 70 tekens aanvaard.
