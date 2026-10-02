# naronde-klein-g4-g5: de nuldeler benoemd in de exectak, de telling over een pad benoemd geweigerd, twee vormen met één naam geweigerd

*Kleine naronde op drie vondsten uit de rondes G4+G5 van 2026-10-01 en
2026-10-02 (open punt 3 van `20261001-ronde-exec-vouwfilter-far-side.md`,
open punten 5 en 9 van `20261001-ronde-frontend-g4-g5.md`), opdracht van de
hoofdsessie van 2026-10-02; gebouwd vanaf uchar `da03d480` op de tak
`ronde/naronde-g4-g5`. Reproducties en metingen op Jutterswet-materiaal in
`/tmp/v2/probe/naronde-g4-g5/` (de nuldeler, de telling, de namen; alleen
gelezen), naast de reproducties van de rondes (`/tmp/v2/probe/g4-g5-exec/v4/`,
`/tmp/v2/probe/vtr-bouw/proef/veel-aantal/`,
`/tmp/v2/probe/g4-g5-frontend/bijvangst/dubbele-regelnaam.rgs`). De nuldeler
was luid zonder naam; de telling over een pad was een stil fout antwoord in
ref en clingo; twee vormen met één naam passeerden `check` en vielen pas bij
de verifier. Geen kaart: dit record legt de naronde vast, zodat de eigenaar er
zijn Keuring bij kan zetten.*

## Aanleiding

**De nuldeler in de exectak.**

    Regel verhouding
        De verhouding van een Vondst moet gesteld worden op 1,00.
    Afwijking van verhouding: gewogen
        De verhouding van een Vondst moet berekend worden als zijn gewogen
        gewicht gedeeld door zijn geschatte gewicht indien zijn gewogen
        gewicht gevuld is.

Gemeten op `da03d480` met V3 (geschat gewicht 0,00, gewogen gewicht 60,00): ref
`EVALUATION FAULT: division by numeric zero` (exit 1), clingo `UNSATISFIABLE — a
fail-fast guard fired: viol("division by numeric zero","V3")` (exit 1), de
exectak `ERROR ConsequenceException: Exception executing consequence for rule
"defaults A:Vondst.verhouding" in defaultpkg: java.lang.ArithmeticException:
Breuk deling door 0`; zo ook in de waarde van een gewone regel, in de absolute
deling, in de toets van een default en onder "Daarbij geldt", en het
scenariorapport droeg die tekst. RUNTIME-SEMANTICS ch. 1.4 punt 4 en ch. 2.2:
een harde fout in v0, besloten dat v1 haar naar het §9.5-kanaal leidt, "the v0
hard fault and the parse-time rejection remain the mirror"; ch. 5.6 noemt de
nuldeler niet onder de producenten van het kanaal, en de rode case
`division-by-numeric-zero` legt vast dat de uitvoerders de delta samen nemen.
Dus een benoemde fout, zoals ref, en geen inconsistentie.

**De telling over een pad.**

    De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien het
    aantal havenjutters van de haven van de jutter van de Vondst groter is
    dan 0.

De naam van een lezing loopt over elke inwendige "van", dus de telling kreeg één
naam, "havenjutters van de haven van de jutter", gelezen op de Vondst; de emitter
schreef het bereik op de kop of op het typepaar van onderwerp en element
(`JutterHeeftVondst.jutter`), en de andere stappen vielen zonder melding weg.
Gemeten op `da03d480`: `check` exit 0 met `diagnostics: []`, verifier PASS, en
met een Haven met twee havenjutters ref `MISMATCH: V.aantal havengenoten = 1,
expected 2` en clingo `= [1], expected 2`. Dezelfde stille afkapping in een
voorwaarde, een kenmerktoekenning, onder een subselectie, achter een bezittelijk
anker ("van de haven van zijn jutter"), met het meervoud van het type ("het
aantal Jutters van …"), onder "plus 1", in de toets en de consequentie van een
default, met een anker van een ander type ("het aantal havenjutters van de
Haven" op een Vondst), en als selectie van een unie ("het aantal vondsten van de
Jutter en vondsten van de havenjutters van de haven van de Jutter": beide
selecties over `JutterHeeftVondst.vondst`). Luid waren al: een telling waarvan
onderwerp en element geen relatie delen (ERROR "vouw … zonder bereik"), een
geschreven stap op de bron, de setlezing, en "het aantal vondsten van alle
havenjutters van de Haven" (WARNING en marker).

**Twee vormen met één naam.**

    Regel actief
        Een Jutter is actief indien zijn startdag gevuld is.
    Regel actief
        Het gewicht van een Jutter moet gesteld worden op 3.

Gemeten op `da03d480`: `check` exit 0 zonder melding, de verifier `WF-020 FAIL
/rules/rules/1: duplicate name 'actief' in rules`, ref en clingo `PASS (2
expectations)`, de exectak `FAIL … kenmerk actief: verwacht aanwezig, werkelijk
afwezig` (het gewicht klopt: de tweede regel vervangt kennelijk de eerste), de
DRL-tak `EXCLUDE drl compile … Duplicate rule name: actief`. Zo ook de naam in
een andere schrijfwijze, een regel naast een consistentieregel (`name 'gewicht'
collides with 'gewicht' in rules`), een lid van een Regelgroep naast een regel,
een beslistabelrij naast een regel, en twee blokken van één naam met elk één
versie; met elk twee versies eindigde het in de uitzondering van de emitter.

## Wat er landde

**De nuldeler (backends/drools).** `Breuk.delen` gooit `Breuk.DelingDoorNul`, een
`ArithmeticException` met de melding die er altijd stond ("Breuk deling door 0")
en het deeltal. Elke KieBase die `DroolsBackend` bouwt, op beide takken, draagt
een consequentiehandler (`DroolsBackend.Nuldelingmelder`, via
`ConsequenceExceptionHandlerOption`) die een `DelingDoorNul` in de oorzaakketen
omzet in een tweede, benoemde `DelingDoorNul` met de regelnaam en de operanden en
de eerste als oorzaak; elke andere uitzondering gaat eruit zoals de
standaardhandler van Drools haar gooit. De replay meldt nu `ERROR DelingDoorNul:
deling door nul in de regel "verhouding": 60 gedeeld door 0 — een numerieke nul
als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)`, het
scenariorapport `case…: ERROR DelingDoorNul: …`, en elke afnemer die de KieBase
vuurt krijgt dezelfde uitzondering; een defaultsgroep draagt de naam die deze
backend haar geeft (`defaults A:Vondst.verhouding`). De vastgepinde fragmenten
van gate 8 ("Breuk deling door 0", "deling door 0") blijven in de keten. README,
sectie "Recorded: a division by a numeric zero". Commits `daf89248` (getuige,
rood), `178668fc` (fix).

**De telling over een pad (frontend).** `ExpressieBouwer.bouwFunctie`: waar de
emitter voor een telling nog een bereik zou schrijven (`IrEmitter.vouwweigering`
geeft niets) en de bron geen setlezing is, telt `telbronStappen` de geschreven
stappen: de delen van de naam bij elke "van" (een naam die als geheel een rol of
objecttype noemt is één deel), dan die van het onderwerp, met het anker weg waar
het het onderwerp aanduidt en het zelfstandig naamwoord achter "zijn" als stap.
Bij meer dan één stap een ERROR:

    telling 'het aantal havenjutters van de haven van de jutter van de Vondst' is
    een telling over een pad van 3 stappen (jutter, haven, havenjutters): deze
    frontend telt alleen over één relatie vanaf het onderwerp (IR-SPEC ch. 4.3,
    WF-102 (c)); schrijf de telling op het bereikte object of noem één relatie

`bouwInstantieUnie` weigert een selectie met meer dan één geschreven stap in de
bestaande unieweigering ("… geen unie van selecties: selectie 2 ('…') is een
telling over een pad van 3 stappen (haven, havenjutters, vondsten): een selectie
telt over één relatie vanaf het onderwerp (IR-SPEC ch. 4.3, WF-110 (e)); …").
Twee afwijkingen van het standaardantwoord, gemotiveerd: de verwijzing is WF-102
(c) in ch. 4.3, waar het bereik van een telbron één stap is, en niet ch. 4.5, de
kwantorvouw; en de weigering staat waar de emitter nog een bereik zou schrijven,
zodat de vormen die al luid weigerden hun zin houden (een eerste versie die elke
telling met meer dan één stap weigerde, brak vijf bestaande tests die die zinnen
vastleggen en voegde een null-marker toe die `MarkerRedenPinTest` niet kent; geen
bestaande test is gewijzigd). Commits `1f2b028e` (getuige, eerste versie, rood),
`db58d875` (getuige, grens op de stille vormen, rood), `499b2e68` (fix),
`cc8a1265` (de tweede Aanvulling van de kaart `frontend-kwantor-over-een-pad`).

**Twee vormen met één naam (frontend).** `RuleChecker.keurVormnamen`, de laatste
toets van `check`: de benoemde vormen worden na de lift gelezen zoals de emitter
ze schrijft (een vorm met versies is één vorm; een tweede "#1" begint een tweede),
in de zeven lijsten van WF-020, met de naamidentiteit van de verifier
(`IrEmitter.norm`). Waar twee vormen één naam dragen, staat een ERROR op de
tweede `Regel`-kop of beslistabelrij die de naam draagt, met de regel van de
eerste, in de zin van de verifier:

    regel 'actief' is al gedeclareerd op regel 6; vormnamen zijn uniek (WF-020):
    duplicate name 'actief' in rules

De naam van een `Uitzondering op`- of `Afwijking van`-blok bereikt de draad niet
(`Default` en `ExceptionGroup` dragen geen naam; de groep heet naar haar basis,
een ongeordend lid naar zichzelf) en blijft buiten de toets, zoals een kop die
niets op de draad zet; een botsing met een gegenereerde vorm weigert waar die
vorm ontstaat. Commits `5f457bdf` (getuige, rood), `460fbcb1` (fix).

## Metingen

Gemeten met de bouw van de worktree (frontendklassen, backendjar, `_build/default`
van ref, clingo en de verifier) tegen een kopie van de bouw van `da03d480`; elke
vorm een eigen Jutterswet-document, ref en clingo als arbiter.

**Getuigen**, elk rood vóór de fix: `NuldelerWitnessTest` (backends/drools, 7
rijen; rood 6 van 7, `expected: <DelingDoorNul> but was: <ConsequenceException>`),
`TellingOverEenPadTest` (frontend, 29 rijen; rood 14 van 29 op `da03d480`, elke
weigerrij `kreeg: []`), `DubbeleVormnaamTest` (frontend, 10 rijen; rood 7 van
10). Na: de suite van de backend 1077 tests, 0 failures, 0 skipped (was 1070);
die van de frontend 1152 tests, 0 failures (was 1113).

**De nuldeler**, vóór → na in de exectak (ref en clingo ongewijzigd FAULT):
default, gewone regel, absolute deling, toets van een default, "Daarbij geldt":
`ERROR ConsequenceException … Breuk deling door 0` → `ERROR DelingDoorNul: deling
door nul in de regel "…": 60 gedeeld door 0 — …`; de DRL-tak van `… cannot invoke
method: rsWaarde` naar dezelfde zin; een lege teller boven een nuldeler blijft 0
(PLAYED, gelijk aan ref en clingo); een deling in een voorwaarde blijft `ERROR
ConstraintEvaluationException: Error evaluating constraint '' in  and in more
rules` (open punt 1). Gate 8, Drools-been: `aantalgevulde-ongewaakte-deling` en
`division-abs-by-numeric-zero` na `FAULT DelingDoorNul: deling door nul in de
regel "…": … <- DelingDoorNul: Breuk deling door 0`, `atmost-reads-until-settled`
ongewijzigd met `… <- DelingDoorNul: Breuk deling door 0`.

**Sweep van de exectak.** Alle 291 goldens met cases door beide takken, met de jar
van `da03d480` en die van `178668fc` (582 statusregels): geen verschil; exectak
PLAYED 239, DRL-tak PLAYED 193.

**De telling**, na de fix: de stille vormen exit 1 met de padweigering als enige
melding; de unies exit 1 met de unieweigering plus de bestaande ERROR over de
genavigeerde lezing van de geweigerde operand; de luide vormen ongewijzigd; de
vormen over één relatie (één relatie, vanaf de Haven, met subselectie,
zelfaanduidend anker, typemeervoud, unie over één relatie, in een voorwaarde)
schoon met dezelfde IR.

**De namen**, na de fix: de zeven stille vormen exit 1 met de naamweigering (twee
blokken met twee versies nu een ERROR in plaats van de uitzondering van de
emitter); de vormen die de lift al weigerde houden haar melding, en waar zij
faalt op een naam die de draad zou bereiken (basisregel naast een gelijknamige
regel, twee ongeordende leden van één naam) staat de naamweigering ernaast; één
regel met twee versies, twee namen en een niet omgezette regel naast een regel
van dezelfde naam blijven schoon. De naamruimte op de corpora: geen van de 304
bronbestanden van repo en distributie draagt twee koppen met één naam, ook niet
met de koppen van uitzonderings- en afwijkingsblokken erbij, en geen van de 202
geschreven IR's geeft WF-020.

**Inertie van de frontend.** 332 runs per frontend, in één JVM: elke getrackte
`.rgs` van de repo los (196), elke scenario-golden als testdocument met `--cases`
(28), elke `.rgs` van de distributie buiten haar uchar-submodule los (108):
exitcode, uitvoer, IR (202 bestanden) en casebestanden (99) vóór en na beide
fixes byte-gelijk.

**Pins** (`scripts/check-drools.sh`): geen golden verschuift, dus
`EXPECTED_PLAYED` 239 en `EXPECTED_DRL_PLAYED` 193 blijven; de JUnit-vloer 239
staat onder de 1077.

**Gates.** De zestien in de volgorde van `gates.yml`, `dune build` vooraf, elk script
onder zijn shebang, op de kop `460fbcb1` (de laatste codecommit; de commit met dit
record raakt alleen `backlog/`); zeventien regels `exit 0` en `DONE`:

     1. check-self-contained: PASS
     2. check-ir-schema: PASS (… 334 file(s) validate and round-trip, floor 192)
     3. check-case-schema: PASS (… 842 case file(s) validate)
     4. check-verifier: PASS (334 golden clean; 503 negatief rejected …)
     5. check-oracle: PASS (ref suite 1152 worked examples, floor 489; 800 cases replayed exactly, floor 427 …)
     6. check-clingo: PASS (backend suite 1988 checks, floor 83; 800 cases replay exactly through clingo …)
     7. check-noop: PASS (756 cases refute a do-nothing executor; 44 pinned survivors …)
     8. check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26 …; Drools leg 14 fault-pinned, 28 not-carried with reason)
     9. check-catala: PASS (241 checks, floor 201; 12 catala golden(s) …)
    10. check-regelspraak: PASS (JUnit 1152 tests, floor 173; 121 rgs golden(s) re-emit byte-identically, floor 80)
    11. check-toka-parity: PASS
    12. check-drools: PASS (played 239, pin 239; DRL-text branch played 193, pin 193; JUnit 1077 tests, floor 239)
    13. check-reftype-derivation: PASS (43 unit annotations twisted over 24 goldens, floor 38 …)
    14. check-flogic: PASS (150 checks, floor 81 …)
    15. check-graaf: PASS
    16. check-scenario: PASS (28 scenario golden(s) regenerate 99 case file(s) …)

## Open punten

1. Een nuldeler in een voorwaarde: de exectak faalt met `ConstraintEvaluationException:
   Error evaluating constraint '' in  and in more rules`, zonder regel of constraint
   (de DRL-tak noemt beide); `DelingDoorNul` staat in de keten, maar een
   consequentiehandler ziet de voorwaarde niet. Luid, niet benoemd. Advies: de
   predicaten van de voorwaarde in de exectak hun regelnaam meegeven
   (`PredicateInformation`), in een kleine exec-ronde.
2. De luide tellingsvormen houden hun eigen zin, en die van een telling over twee
   stappen zonder typepaar noemt het pad niet ("vouw … zonder bereik (geen relatie
   verbindt Jutter met Jutter)"); "van alle …" blijft een marker met WARNING
   (`check` van de distributie exit 3, niet 1). Advies: zo laten, of ze onder de
   padweigering brengen; dat verlegt de vastgelegde zinnen in
   `NavigatiekeuzeTest`, `AantalSetbronTest` en `StructuurZinTest` en vraagt het
   akkoord van de eigenaar.
3. Een geweigerde unieselectie geeft twee meldingen: de unieweigering en de
   bestaande ERROR over de genavigeerde lezing van die operand. Advies: zo laten.
4. Waar de lift faalt op een naam die de draad zou bereiken, staan twee meldingen:
   die van de lift ("… heeft meer dan één regelversie …") en de naamweigering.
   Advies: de zin van de lift laten zeggen dat het om twee blokken met één naam
   gaat, of zo laten.
5. Bijvangst, laag backends/drools: een document met twee regels van één naam
   (ongeldig onder WF-020; de frontend en de verifier weigeren het nu) speelt de
   exectak stil fout, omdat de tweede regel de eerste in het pakket vervangt; de
   DRL-tak weigert benoemd. Advies: een benoemde weigering in de exectak, zoals de
   DRL-tak haar heeft.
6. De distributie draait op haar pin en kent deze naronde niet; de pin moet na het
   landen mee.

## Keuring (hoofdsessie, 2026-10-02)

- Gates: `/tmp/v2/uchar-gates24/summary.txt` op de laatste codecommit 460fbcb1:
  dune build en zestien gates "exit 0", `DONE`; gate 12 played 239, DRL-tekst
  193, JUnit 1077; pins ongewijzigd. Het done-record (64b97045) raakt alleen
  `backlog/`.
- Gelezen: het Result per vondst (getuigen rood-vóór/groen-na, de sweep over
  de goldens en de inertie over 332 bronnen), de twee afwijkingen van het
  standaardantwoord bij de telling (de zin verwijst naar ch. 4.3/WF-102 (c);
  vormen die al luid weigerden houden hun zin) — beide aanvaard, want geen
  bestaande test hoefde te wijzigen en de stille vormen zijn nu luid.
- Registertekst geplaatst in de commit na de Keuring (de Keuring-commit 21d530b4 miste haar door een afgebroken script): DIVERGENCES §13.4.17.47 (de
  subsectie "A count over a written path — refused") en "Namen in
  declaratiekoppen", "Fail-fast refusals" (de alinea over twee vormen met één
  naam); RUNTIME-SEMANTICS en IR-SPEC ongewijzigd (de tekst zegt het al).
- Open punten: 1 (de nuldeler in een voorwaarde faalt luid maar niet
  benoemd) en 5 (twee gelijknamige regels spelen de exec-tak stil fout; de
  frontend en de verifier weigeren zo'n document nu) gaan samen naar een
  volgende kleine exec-ronde; 2, 3 en 4 aanvaard zoals geadviseerd (geen
  bestaande zinnen verleggen zonder besluit van de eigenaar); 6 (de
  distributiepin) volgt nu.
