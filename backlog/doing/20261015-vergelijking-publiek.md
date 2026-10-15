# vergelijking-publiek: de verwachtingen van een case tegen een gevuurde sessie, als publieke ingang van de Drools-backend

*Op verzoek van de eigenaar (2026-10-15). Tweede kaart van de reeks "beslisservice via dienst" (de eerste:
`waarnemer-vuur`): de RegelSpraak-werkbank laat scenario's straks rekenen door de beslisservice van de distributie, en
het oordeel PASS/FAIL moet dan van uchar blijven komen. Regelnummers op `7fbab5ec`.*

Goal: de vergelijking van verwachtingen met wat een sessie na het vuren draagt, is privé in `CaseRunner`
(`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`): `runCase` (r. 511–545) loopt over
`expected`, `vergelijk` (r. 1167) kiest per soort van `uchar-case-v0` de vergelijking (`attribuut`, `tijdlijn`,
`kenmerk`, `consistentie`, r. 1181–1288), en `bron`, `verwacht` en `werkelijk` (r. 1344–1396) maken de
`ScenarioResultaat.VerwachtingResultaat` van het rapport. Ze zoeken een bean op met het caselabel (`labelNaarBean`,
`labelType`), dus een afnemer met een eigen sessie kan ze niet aanroepen. De distributie heeft daarom twee kopieën,
`GoldenOverHttpTest.vergelijk` en `scripts/dienst_replay.py` (`vergelijk`), die op punten afwijken; de dienst zou een
derde nodig hebben.

De kaart maakt in `DroolsBackend` publiek:

1. `DroolsBackend.vergelijk(Build, KieSession, Function<Object, String> label, JsonNode verwachtingen)`: vergelijkt de
   `expected` van een case met een sessie die `zetAfgeleideWaarden` terugzette, en geeft per verwachting een
   `VerwachtingResultaat` plus de diagnose van het rapport. De aanroeper geeft het label van een bean met een functie
   bean → label, zoals bij `lees` en de `Waarnemer`: de ingang zoekt bij elk label van een verwachting de bean van de
   sessie waaraan de functie dat label geeft, dus de labels mogen de ids van een dienstverzoek zijn.
2. `DroolsBackend.Vergelijking(List<ScenarioResultaat.VerwachtingResultaat> verwachtingen, String diagnose)`, met
   `pass()`.

`CaseRunner.runCase`, en daarmee de replay van gate 12 en `CaseRunner.rapport`, vergelijkt via deze ingang. Geen
nieuwe vormen, geen wijziging van spec, schema of corpus.

Keuze over de invoer van de ingang: de beans na de terugzetting. Die dragen het fixpunt van de sessie (de invoer plus
wat `lees` geeft, door dezelfde code geschreven), en het is wat de distributie als antwoord geeft: de dienst en
`CaseSessie` zetten terug zodra er geen evaluatiefout staat. De replay blijft zo de beans na de terugzetting
vergelijken, zodat gate 12 de terugzetting blijft toetsen. De ingang roept `lees` aan voor diens weigeringen, zodat
hij niets vergelijkt van een sessie waarin na de laatste `vuur` iets buiten `vuur` om gebeurde.

Acceptance:
- (a) De publieke ingang, met de signaturen hierboven. Weigeringen, elk met een zin die zegt wat er mis is:
  - wat `lees` weigert (`IllegalStateException` van `lees`): een sessie zonder fixpunt van `vuur`, of waarin daarna
    iets buiten `vuur` om gebeurde;
  - een sessie waarvan het fixpunt evaluatiefouten draagt, en een sessie die niet is teruggezet
    (`IllegalStateException`);
  - `verwachtingen` die geen array is, en een labelfunctie die één label aan twee beans van de sessie geeft
    (`IllegalArgumentException`);
  - een verwachting op een attribuut of kenmerk (`attributeValue`, `attributeEmpty`, `characteristicPresent`,
    `characteristicAbsent`) met een label dat geen bean van de sessie draagt (`IllegalArgumentException`, in de zin
    die `attributeTimeline` daar al gaf). Tot nu toe las de replay dan "leeg" of "afwezig", zodat `attributeEmpty` en
    `characteristicAbsent` op een verkeerd label slaagden; het corpus raakt dat niet, want ref weigert zo'n case bij
    het laden ("expectation references unknown label");
  - wat de ingang niet vergelijkt, met de `CaseRunner.Excluded` en de zin van nu (een gedimensioneerd attribuut, een
    scalaire lezing van een attribuut op een tijdlijn, een rationaal getal, een onbekende soort, …), die het rapport
    als MARKER geeft.
- (b) `runCase` vergelijkt via `DroolsBackend.vergelijk(bouw, sessie, this::labelVanBean, c.get("expected"))`; de lus
  over `expected` in `runCase` verdwijnt. Meting buiten de gates: het harnas `RapportDump` (één JVM,
  `CaseRunner.rapport` per golden met case-bestanden, één regel `ScenarioResultaat.toString()` per case) met de jar
  van `7fbab5ec` en met die van de eindstand. Nulmeting op `7fbab5ec`: 300 goldens, 823 rapporten (695 PASS, 128
  MARKER), twee runs bytegelijk (sha1 `405dd7c691b6a3db576c697501930023631a1b99`). De eindstand moet bytegelijk zijn.
- (c) Nieuwe testklasse `backends/drools/src/test/java/dev/uchar/regelspraak/cases/VergelijkingWitnessTest.java`, die
  de sessie opzet zoals een afnemer (`CaseRunner.metSessie`), vuurt en terugzet met de publieke ingang, en dan
  `vergelijk` aanroept:
  - op goldens die samen elke soort van `expected` dragen (een attribuutwaarde, leeg, een tijdlijn, een kenmerk
    aanwezig en afwezig, consistent, inconsistent met regel en label), op beide takken: dezelfde resultaten en
    dezelfde diagnose als het rapport;
  - een sessie waarvan de labelfunctie andere labels geeft dan de case-JSON (zoals de dienst, met de ids van het
    verzoek), met de verwachtingen in die labels: dezelfde uitkomst als het rapport, op de labels na; met de labels
    van de case-JSON wordt die sessie geweigerd;
  - een verwachting die niet overeenkomt: `pass()` onwaar, de diagnose zoals het rapport haar geeft;
  - elke weigering van (a).
  Eerst rood waar het kan: elke rij valt tegen een tijdelijke mutatie (gemeten, niet bewaard). Geen bestaand
  testbestand gewijzigd.
- (d) `backends/drools/README.md`, "A session in four calls", stap 4, noemt `vergelijk`.
- (e) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch; gate 12 met de nieuwe tests in de
  JUnit-telling.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`, de nieuwe testklasse
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/VergelijkingWitnessTest.java`, `backends/drools/README.md`,
deze kaart.

Out of scope:
- Vergelijken tussen twee fires van één sessie, vóór de terugzetting: dat vraagt de waarden uit `lees` over de invoer
  te leggen, een tweede lezing naast de terugzetting.
- Een `inconsistent`-verwachting met een onbekend label: die faalt nu als afwijking (geen stille PASS), en
  `ConsistentieLabelWitnessTest` legt dat vast; ref weigert haar bij het laden.
- `ScenarioResultaat` (de records blijven zoals ze zijn), de verlaging, de DRL-tekst.
- De distributie en de werkbank: die nemen de ingang over na een pin-bump, op hun eigen kaart.
- `spec/`, het schema, het corpus, de gate-scripts.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.
