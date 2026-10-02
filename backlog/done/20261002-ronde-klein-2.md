# ronde-klein-2: de nuldeler in een voorwaarde benoemd, twee regels van één naam benoemd geweigerd, de kringtoets van de verifier in check; de kenmerkcheck twee rollen ver wacht op een besluit over de draad

*Kleine ronde op vier vondsten uit cluster G7+G9 (V3, V2) en uit de naronde
G4+G5 (open punten 1 en 5 van `20261002-naronde-klein-g4-g5.md`), opdracht van
de hoofdsessie van 2026-10-02; gebouwd vanaf uchar `70a60dc3` op de tak
`ronde/klein-2`. De metingen van het cluster stonden op neutraal materiaal
onder `/tmp/v2/probe/g7-g9/`; alles hieronder is opnieuw gemeten op eigen
Jutterswet-documenten, met de meetscripts, onder `/tmp/v2/probe/klein-2/`
(per vorm `bron.rgs`, `scenario.rgs`, `voor/meting.txt` en `na/meting.txt`;
`tool/meet.sh`, `tool/Check.java` (het check-pad van de CLI van de
distributie, over de publieke API van uchar), `tool/sweep.sh`,
`tool/rood.sh`, `tool/inertie.sh`, `tool/src/…/Pariteit.java`). Vóór = de bouw
van `70a60dc3`; na = de bouw van de commit die erbij staat. ref en clingo zijn
de arbiter. Geen kaart: dit record legt de ronde vast, zodat de eigenaar er
zijn Keuring bij kan zetten.*

## Aanleiding

**De nuldeler in een voorwaarde (open punt 1 van de naronde).**

    Regel zwaar
        geldig altijd
            Een Vondst is zwaar
            indien zijn gewogen gewicht gedeeld door zijn geschatte gewicht groter is dan 1.

Gemeten op `70a60dc3` met V3 (geschat gewicht 0,00, gewogen gewicht 60,00):
ref `EVALUATION FAULT: division by numeric zero`, clingo `UNSATISFIABLE — a
fail-fast guard fired: viol("division by numeric zero","V3")`, de exectak
`ERROR ConstraintEvaluationException: Error evaluating constraint '' in  and in
more rules`, de DRL-tak `ERROR ConstraintEvaluationException: Error evaluating
constraint 'rsAggVergelijk(rsGedeeld(gewogenGewicht, geschatteGewicht), …)' in
[Rule "zwaar" in file0.drl]`. Zo ook in de voorwaarde van een gewone regel,
onder "alle volgende voorwaarden", met de absolute deling, tegen een
parameter, onder "ten minste één", in de subselectie van een telling, in de
matrix van een kwantorvouw, over een rol en via "Daarbij geldt". Een
voorwaarde wordt in een constraint getoetst en niet in een consequentie; een
lambda van de exectak draagt geen `PredicateInformation`, dus Drools noemde
regel noch voorwaarde, en `DroolsBackend.benoem` zag alleen de
`ConsequenceException`. In de toets van een default was de nuldeler al benoemd
(die wordt in de consequentie van de defaultsregel geëvalueerd).

**Twee regels van één naam in de exectak (open punt 5 van de naronde).** Op
een handgeschreven document (de frontend en de verifier weigeren het) met
"Regel actief: Een Jutter is actief indien zijn startdag gevuld is" en "Regel
actief: Het gewicht van een Jutter moet gesteld worden op 3": verifier
`WF-020 FAIL /rules/rules/1: duplicate name 'actief' in rules`, ref en clingo
`PASS (2 expectations)`, de exectak `FAIL … kenmerk actief: verwacht aanwezig,
werkelijk afwezig` (de tweede regel vervangt de eerste in de KieBase), de
DRL-tak `EXCLUDE drl compile: [… text=Duplicate rule name: actief]`. Een regel
en een consistentieregel van één naam: de exectak `FAIL … attribuut gewicht:
verwacht 3, werkelijk leeg`.

**Een kring die `check` doorlaat (V2).**

    Regel restgewicht
        Het restgewicht van een Vondst moet berekend worden als zijn gewicht min
        zijn bergingsgewicht.
    Regel bergingsgewicht
        Het bergingsgewicht van een Vondst moet gesteld worden op het restgewicht
        van de vorige vondst van de Vondst
        indien het restgewicht van de vorige vondst van de Vondst groter is dan 0.

(Feittype "vorige vondst", met Vondst aan beide kanten.) Gemeten op
`70a60dc3`: `check` exit 0 zonder diagnose; verifier `WF-045 FAIL
/rules/rules/0/versions/0: dependency cycle through no recursive creation:
restgewicht -> bergingsgewicht -> restgewicht`; ref `EVALUATION FAULT: WF-045
…`; clingo `PASS (5 expectations)`; de exectak `PLAYED 1`; de DRL-tak `FAIL …
attribuut bergingsgewicht: verwacht 10, werkelijk leeg; …`. Door een `Afwijking
van` (meldtermijn ← verlenging ← restermijn van de vorige vondst ←
meldtermijn): `check` exit 0; verifier WF-045, WF-046 en `WF-051 … defaults
group 'meldtermijn' …`; ref FAULT; clingo `PASS (7)`; de exectak `PLAYED 1`.
Door een telling met een kenmerk (het zwaartetal van een Jutter telt zijn zware
vondsten; een Vondst is zwaar boven het zwaartetal van zijn jutter): `check`
exit 0; verifier WF-045; ref FAULT; clingo `UNSATISFIABLE — … no viol/2
witness (the input is not stratified?)`; de exectak `PLAYED 1`. De graaf van
IR-SPEC ch. 5.1 heeft één knoop per objecttype en lid, dus een waarde die van
dezelfde waarde van een ander object van het type afhangt, is een kring. De
frontend herhaalt WF-regels van de verifier in diens zin (WF-020, WF-022,
WF-033, WF-050, WF-053, WF-111, WF-112, …), maar niet WF-045; haar WARNING
"kringverwijzing tussen berekeningen" leest alleen de lezingen op het eigen
onderwerp van een regel.

**Een kenmerkcheck twee rollen ver (V3).** "De havendatum van een Vondst moet
gesteld worden op zijn vinddag indien de haven van de jutter van de Vondst
zeehaven is" (en "… het getij heeft", "… geen getij heeft"). Gemeten op
`70a60dc3`: `check` drie keer `WARNING … onopgelost: de geschreven naam bindt
geen kenmerk en geen rol van het onderwerp (§8.1.7/§8.1.8) … blijft als marker
op de wire staan` en `niet uitvoerbaar in uchar (exit 3): transpiler SKIP:
regel: havendatum — regel (voorwaarde buiten Phase-B scope): havendatum`;
verifier `PASS`; ref `EVALUATION FAULT: Unresolved condition reached in
evaluation`; clingo `TRANSLATION FAULT: Unresolved condition reached in
translation …`. Eén rol ver ("indien de jutter van de Vondst ijverig is"):
ref en clingo `PASS (4 expectations)`, de exectak `PLAYED 1`. De oorzaak is de
draad: `CharacteristicCheck.navigation` is één `RoleBinding` (IR-SPEC ch. 4.5,
ch. 4.1a en WF-053), dus de frontend schrijft de zin als `Unresolved`, ref en
clingo falen erop en de exectak weigert de marker. Er is geen arbiter.

## Wat er landde

**Twee regels van één naam (backends/drools).** `DroolsBackend.execModelTak`
weigert vóór `KieBaseBuilder` een exec-model met twee regels van één volle
naam (`weigerDubbeleRegelnaam`): `EXCLUDE exec-model: Duplicate rule name:
actief — twee regels van één naam in pakket defaultpkg; de KieBase houdt één
regel per naam, zodat de tweede de eerste zou vervangen (vormnamen zijn uniek,
WF-020)`. Een regel zonder naam (een naamloze vorm, WF-107) telt niet mee: de
exectak speelt twee naamloze creaties (`TakgelijkheidWitnessTest`, die de eerste
versie van de toets brak). README, "The entry for consumers". Commits
`d05e59e7` (getuige, rood), `dec46c1d` (fix), `d76fb625` (de naamloze regel).

**De nuldeler in een voorwaarde (backends/drools).** Nieuw:
`transpiler/model/BenoemdeToets`. Het predicaat van een voorwaarde-constraint
draagt `PredicateInformation` (de brontekst van de voorwaarde, de regel, het
pakket), zodat Drools beide noemt, en benoemt een `Breuk.DelingDoorNul` zelf naar
de regel, met de operanden (`inDeRegel`). `ExecModelTranspiler.meet` houdt de
regel in bouw bij; de constraints van een voorwaarde (blad, tellende kwantor,
venster, heelperiode, de armen tegen een parameter, `nGlobalGate`), van een
vergeleken aggregatie, van een subselectie (`filterSource`), van een
kwantorvouw, van de schending van een consistentieregel en van de doelguard van
een feitcreatie dragen haar. `Breuk.DelingDoorNul.regel()` is null zolang niets
haar benoemde. `DroolsBackend.benoem(ConstraintEvaluationException)` geeft de
benoemde deling uit de keten door, elke andere uitzondering ongewijzigd;
`CaseRunner.vuurTotFixpunt` roept haar aan naast de consequentie-arm. Geen
handler en geen hook die Drools op klassenaam instantieert. De DRL-tak is
ongewijzigd (zijn eigen MVEL-uitzondering noemt regel en voorwaarde). README,
"Recorded: a division by a numeric zero". Na: `ERROR DelingDoorNul: deling door
nul in de regel "zwaar": 60 gedeeld door 0 — een numerieke nul als deler is een
fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)` in elke gemeten vorm (in de
kwantormatrix "ijverig": 100 gedeeld door 0); een afnemer die zelf vuurt krijgt
`Error evaluating constraint 'zijn gewogen gewicht gedeeld door zijn geschatte
gewicht groter is dan 1' in [Rule "zwaar" in dev.uchar.regelspraak.generated]`
met de benoemde deling in de keten. Commits `15c99900` (getuige, rood),
`5228e843` (fix, README).

**De kringtoets van de verifier in check (frontend).** Nieuw:
`resolver/Afhankelijkheden`. Het leest de graaf van IR-SPEC ch. 5 over het
document dat de emitter schrijft, zoals de verifier: de vormen van `dep_forms`
in haar volgorde, `dep_expr` en `dep_cond`, `member_writes` en `pop_writes`, de
randen in leesvolgorde, de diepte-eerst-wandeling van WF-045 die de eerste
kring noemt, en het bereik van WF-051, elk met de zin van de verifier.
`RuleChecker.keurAfhankelijkheidskringen`, de laatste toets van de frontend,
op een model zonder ERROR, geeft per kring een ERROR op de kop van de vorm waar
de verifier haar meldt:

    regel 'restgewicht' hangt via een kring van zichzelf af; de afhankelijkheden zijn
    kringvrij buiten een recursieve creatie (WF-045): dependency cycle through no
    recursive creation: restgewicht -> bergingsgewicht -> restgewicht

    defaults-groep 'meldtermijn' ligt op een afhankelijkheidskring, en een kring door
    een defaults-groep heeft geen strata (WF-051): a dependency cycle passes through
    defaults group 'meldtermijn' — its displacement reads are negative edges; no
    strata exist

`check` geeft nu exit 1 op de drie kringen van de aanleiding. Twee grenzen,
beide door bestaande tests vastgelegd en niet gewijzigd: een kring die de
WARNING "kringverwijzing tussen berekeningen" al noemt (elke vorm op de kring
een regel of defaults-groep waarvan die WARNING het doel telde) houdt alleen die
WARNING (`RecursieTest`: "cyclisch = WARNING, nooit ERROR"; `ZelflezingKringTest`:
precies één diagnose); een kring door een objectcreatie buiten een recursieve
groep blijft zonder ERROR (`CreatieRolTest`, `SetlezingZonderVouwTest`). Een
melding die de verifier twee keer geeft (twee randen van één paar) staat één
keer. De twee kringen die `grammar/DIVERGENCES.md` §7 noemt als "door de
verifier geweigerd en door de frontend niet gemeld" (x van een Klok uit zijn y,
y van een Ding uit zijn x; een berekening op de Strandvondst die "afwijkende
opgave meldingsdatum" leest) krijgen nu de ERROR. Commits `92643260` (getuige,
rood), `1dbeacc7` (fix).

**De kenmerkcheck twee rollen ver.** Niet gebouwd: zie open punt 1.

## Metingen

**Getuigen**, elk rood vóór de fix:

- `DubbeleRegelnaamWitnessTest` (backends/drools, 4 rijen): rood op `70a60dc3`
  `Tests run: 4, Failures: 2` ("Expected …Excluded to be thrown, but nothing
  was thrown"); na 4/4.
- `NuldelerVoorwaardeWitnessTest` (backends/drools, 10 rijen: zeven vormen,
  rapport en rode route, een afnemer die zelf vuurt, de DRL-flank): rood op
  `70a60dc3` `Tests run: 10, Failures: 9` (`expected: <DelingDoorNul> but was:
  <ConstraintEvaluationException>`); na 10/10. `NuldelerWitnessTest` 7/7 en
  `NuldelerAfnemerWitnessTest` 3/3 ongewijzigd.
- `AfhankelijkheidskringTest` (frontend, 6 rijen: over een rol, door een
  afwijking, door een telling; flanken: eigen onderwerp, lezing over een rol
  zonder kring, recursieve creatie): rood op `70a60dc3` `3 tests failed` (`but
  was: <[]>`); na 6/6.

Suites na: de backend 1094 tests, 0 failures, 0 skipped (was 1080); de
frontend 1158 tests, 0 failures (was 1152). Geen bestaand testbestand gewijzigd.

**Sweep van de exectak.** Alle 291 goldens met cases door beide takken, met de
jar van `70a60dc3` en die van `dec46c1d` en van `5228e843`: 582 statusregels
gelijk (exectak PLAYED 239, DRL-tak PLAYED 193).

**Rode route van de backend.** De 42 rode cases door `DroolsRoodMain`: twee
regels anders, beide met hun vastgepinde fragment. `atmost-reads-until-settled`
van `FAULT ConstraintEvaluationException: Error evaluating constraint '' in  and
in more rules <- … <- DelingDoorNul: Breuk deling door 0` naar `FAULT
DelingDoorNul: deling door nul in de regel "stopteken van een vacuumwacht": 100
gedeeld door 0 — … <- DelingDoorNul: Breuk deling door 0` (pin "deling door 0");
`rekenjaar-zonder-rekendatum` noemt nu voorwaarde en regel (`Error evaluating
constraint 'zijn vondstjaar kleiner of gelijk is aan het Rekenjaar' in [Rule
"aantal verstreken penningjaren" …]`), met de pin "the evaluationYear context
value needs an evaluation date; …" in de keten.

**Pariteit van de kringtoets met de verifier.** Over 883 IR-documenten (golden,
negatief, rood, de parity-zone en de proeven) zijn de randen van
`Afhankelijkheden` gelijk aan `verifier --dependency-edges` (609 randen) en de
WF-045- en WF-051-regels gelijk, ook in volgorde (11 documenten met een kring,
14 regels). Vijf extra kringvormen (twee kringen in één document, drie vormen
over twee rollen met sommen, een subtype, een regel met twee versies `waarde #1
-> waarde #1`): de zin van `check` gelijk aan die van de verifier; de kring op
het eigen onderwerp houdt haar WARNING.

**Inertie van de frontend.** 304 bronnen door de frontend van `70a60dc3` en van
`1dbeacc7` (`--ir`): 149 getrackte `.rgs` van de repo, 47 van `toka/`, 108 van
de distributie buiten haar submodule. Exitcode, uitvoer en IR byte-gelijk.

**Pins** (`scripts/check-drools.sh`): geen golden verschuift en er kwam geen
golden bij, dus `EXPECTED_PLAYED` 239 en `EXPECTED_DRL_PLAYED` 193 blijven; de
JUnit-vloer 239 staat onder de 1094.

**Gates.** De zestien in de volgorde van `gates.yml`, `dune build` vooraf,
elk script onder zijn shebang, op de kop `1dbeacc7` (de laatste codecommit; de
commit met dit record raakt alleen `backlog/`); zeventien regels `exit 0` en
`DONE 06:54:41`:

     1. check-self-contained: PASS
     2. check-ir-schema: PASS (… 334 file(s) validate and round-trip, floor 192)
     3. check-case-schema: PASS (… 842 case file(s) validate)
     4. check-verifier: PASS (334 golden clean; 503 negatief rejected …; 346 on their declared arm …)
     5. check-oracle: PASS (ref suite 1152 worked examples, floor 489; 800 cases replayed exactly, floor 427 …)
     6. check-clingo: PASS (backend suite 1988 checks, floor 83; 800 cases replay exactly through clingo …)
     7. check-noop: PASS (756 cases refute a do-nothing executor; 44 pinned survivors …)
     8. check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26 …; Drools leg 14 fault-pinned, 28 not-carried with reason)
     9. check-catala: PASS (241 checks, floor 201; 12 catala golden(s) …)
    10. check-regelspraak: PASS (JUnit 1158 tests, floor 173; 121 rgs golden(s) re-emit byte-identically, floor 80)
    11. check-toka-parity: PASS
    12. check-drools: PASS (played 239, pin 239; DRL-text branch played 193, pin 193; JUnit 1094 tests, floor 239)
    13. check-reftype-derivation: PASS (43 unit annotations twisted over 24 goldens, floor 38; 122 executor replays …)
    14. check-flogic: PASS (150 checks, floor 81 …)
    15. check-graaf: PASS (spec leg 257 of 294 goldens compared, 194 dependencies equal; route leg 121 goldens, 68 unit tests)
    16. check-scenario: PASS (28 scenario golden(s) regenerate 99 case file(s) …)

## Open punten

1. **De kenmerkcheck twee rollen ver** (V3) vraagt een besluit over de draad:
   `CharacteristicCheck` draagt één rol (`navigation`), `AttributeReference` een
   pad (`path`). Twee wegen. (a) De draad krijgt de keten: een `path` met twee of
   meer `RoleBinding`s naast `navigation` (precies één van beide, zoals
   `sourceRole`/`sourcePath` van een vouw), WF-053 over elke stap (to-one,
   aaneengesloten, het kenmerk op het bereikte type), de randen een relatie per
   stap en het kenmerk op het bereikte type; dan de frontend (de keten in plaats
   van de marker), ref en clingo (een stap per rol, een ontbrekende stap is
   onwaar, ontkend of niet), de exectak (`kenmerkHop` naar een keten, met de
   joins van `navJoins` per object onderweg, zoals de vergelijking), de DRL-tak
   (benoemd weigeren), de graaf, de AST-bouwers van de Catala- en
   F-logic-frontends, een golden (`jutterswet-<werknaam>`, pin +1) en een
   negatief per nieuwe arm; IR-SPEC ch. 4.1a, 4.5, WF-053 en RUNTIME-SEMANTICS
   ch. 5.8 en 6.1 als registertekst. (b) Een dialectverlaging in de frontend
   zonder draadwijziging, zoals "A fold over a set and to-one steps after it — a
   generated member" (`grammar/DIVERGENCES.md`): een gegenereerd kenmerk op het
   tussenobject met één rol ver, een apart kenmerk voor de ontkende vorm, en de
   check één rol ver op dat kenmerk; ref, clingo en de exectak spelen dat al.
   Advies: (a), als eigen werk na een besluit van de eigenaar; de check hoort een
   keten te dragen zoals de lezing, en (b) zet hulpkenmerken in het model van
   iedere afnemer. Tot dan blijft het hulplid van de regelset staan.
2. **De exectak speelt een kring die WF-045 weigert** stil tot een fixpunt
   (drie keer `PLAYED`, ook door een defaults-groep, die WF-046 een negatieve
   kring noemt); alleen een leeg-toets op een kring weigert zij benoemd. De
   frontend weigert zulke documenten nu vóór de bouw, dus een bron van de CLI
   of de dienst bereikt de tak er niet mee, behalve langs de twee grenzen van
   punt 3 en via een handgeschreven document. Advies: zo laten; een benoemde
   weigering in de build kan later uit de graaf van de backend
   (`graaf/ProducentConsument`).
3. **Twee grenzen van de kringtoets in check.** (a) Een kring op het eigen
   onderwerp houdt alleen haar WARNING en `check` geeft exit 0, waar de verifier
   weigert: `RecursieTest.kringverwijzing_is_een_warning_geen_error` en de
   rijen van `ZelflezingKringTest` leggen "WARNING, nooit ERROR" vast. (b) Een
   kring door een objectcreatie buiten een recursieve groep (een creatie die de
   populatie leest die zij laat groeien) krijgt geen ERROR: drie rijen van
   `CreatieRolTest` en `SetlezingZonderVouwTest` leggen zulke documenten zonder
   fout vast, en het casusdocument van `toka/` heeft er één. Advies: met akkoord
   van de eigenaar beide grenzen weghalen en die tests aanpassen; dan geeft
   `check` op elke kring die de verifier weigert exit 1. Zolang zij staan ziet
   een poort die `check` draait deze twee soorten niet.
4. **WF-046 herhaalt check niet.** Een negatieve rand op een kring door een
   recursieve creatie (WF-045 laat die kring toe) gaat door `check`; de exectak
   weigert een leeg-toets op een kring benoemd. Advies: zo laten tot een
   regelset er een raakt; het patroon is er (`Afhankelijkheden` kent de
   randsoorten).
5. **Twee naamloze regels** in het exec-model (naamloze vormen, ongeldig onder
   WF-107) blijven buiten de naamtoets; de exectak speelt ze. Advies: zo laten,
   WF-107 en de frontend weigeren zo'n document.
6. De commentaarzin bij `keurKringverwijzingen` ("The spec does not forbid the
   writing, so this is a WARNING") zegt nog wat vóór WF-045 gold; de WARNING
   staat door punt 3 (a). Niet aangepast, omdat de houding zelf vastligt.
7. De distributie draait op haar pin en kent deze ronde niet; de pin moet na het
   landen mee. Daarna geeft haar `check` exit 1 op een kring die de verifier
   weigert, buiten de twee grenzen van punt 3.
