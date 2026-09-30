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
