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
