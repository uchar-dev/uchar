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

## Result (2026-10-15)

`267ff248` maakt in `DroolsBackend` publiek wat de replay tot dan privé deed: de vergelijking van de `expected` van een
case met een sessie. Geen wijziging van spec, schema, corpus of gate-scripts.

- **Ingang** (`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`):
  - `public record Vergelijking(List<ScenarioResultaat.VerwachtingResultaat> verwachtingen, String diagnose)`, met
    `boolean pass()` (elke verwachting `ok`). `diagnose` is de regels van de verwachtingen die niet hielden, verbonden
    met "; ", leeg waar alle hielden; het rapport zet de naam van het casebestand en ": " ervoor.
  - `public static Vergelijking vergelijk(Build build, KieSession sessie, Function<Object, String> label,
    JsonNode verwachtingen)`. Volgorde: `verwachtingen` moet een array zijn (`IllegalArgumentException`);
    `lees(build, sessie, label)`, met diens weigeringen; evaluatiefouten aan het fixpunt geven een
    `IllegalStateException` met het aantal en de eerste fout; een sessie zonder `Teruggezet` ook; dan per beanklasse van
    het model de beans van de sessie met het label dat `label` geeft (null wordt overgeslagen), en een label op twee beans
    geeft een `IllegalArgumentException`; dan de vergelijking.
- **Vergelijking** (`CaseRunner.java`): `static vergelijking(build, sessie, perLabel, typen, verwachtingen)` maakt een
  runner van de build met de labels van de aanroeper en draait de lus die in `runCase` stond, met de bestaande
  `vergelijk`, `bron`, `verwacht` en `werkelijk`. `runCase` roept na `vuurTotFixpunt` (vuur en terugzetting, zoals
  voorheen) `DroolsBackend.vergelijk(bouw, sessie, this::labelVanBean, c.get("expected"))` aan. `attribuut` en
  `kenmerk` vragen het objecttype via `typeVanLabel`, dat een onbekend label weigert.
- **Tekst**: `backends/drools/README.md`, "A session in four calls", stap 4; de klassen-Javadoc van `DroolsBackend`.

Keuzes, door de sessie gemaakt:
1. De ingang leest de beans van een teruggezette sessie, niet de waarden van `lees` over de invoer heen. De beans dragen
   dan het fixpunt (door dezelfde code geschreven als `lees` leest) en zijn wat een afnemer als antwoord geeft; de dienst
   (`Uitvoerder.vuur`) en `CaseSessie` zetten al terug zodra er geen fout staat. Zo blijft de replay de beans na de
   terugzetting vergelijken en blijft gate 12 de terugzetting toetsen; een ingang die uit `lees` leest, zou in de
   replay een fout van de terugzetting verbergen. Prijs: een sessie wordt één keer vergeleken, na haar laatste fire.
2. Het label komt uit een functie bean → label, zoals bij `lees` en de `Waarnemer`; de ingang keert haar om over de
   beans van de sessie. Het contract noemt geen klasse van het beanmodel.
3. De ingang staat in `DroolsBackend`, naast `vuur`, `dekking` en `lees`; de code van de vergelijking blijft in
   `CaseRunner`, die de caselezer (`tijdreeks`, `waardeNaarArg`) ermee deelt.
4. Een verwachting op een attribuut of kenmerk van een label dat geen bean draagt, wordt geweigerd, in de zin die
   `attributeTimeline` al gaf. Dit is de enige verandering van een uitkomst. Gemeten op `7fbab5ec` met golden
   `anchor/jutterswet-kustwacht` case1 en één verwachting op label W9: `attributeEmpty` en `characteristicAbsent`
   gaven PASS, `attributeValue` en `characteristicPresent` FAIL; op `267ff248` geven alle vier een
   `IllegalArgumentException`. Het corpus raakt het niet: ref weigert zo'n case bij het laden.
5. Een lege lijst verwachtingen geeft `pass()`, zoals in de replay: bestaande witness-tests spelen cases met
   `"expected": []`.
6. Een tijdlijn vergelijkt de ingang wel (de bean houdt de segmenten); wat hij niet vergelijkt, weigert hij met de
   `CaseRunner.Excluded` en de zin van de replay, en het rapport geeft die als MARKER.

### Acceptatie

- (a) Gehaald, met de signaturen hierboven.
- (b) Gehaald. Harnas `RapportDump` buiten de gates (één JVM, `CaseRunner.rapport` per golden met case-bestanden, één
  regel `ScenarioResultaat.toString()` per case): de jar van `7fbab5ec` tweemaal en de jar die gate 12 op `267ff248`
  bouwde, geven alle drie 300 goldens, 823 rapporten (695 PASS, 128 MARKER), sha1
  `405dd7c691b6a3db576c697501930023631a1b99`; bytegelijk. Een tweede harnas zette elke case die het rapport speelt (695)
  op beide takken op met `metSessie`, vuurde en zette terug via de publieke ingang met een labelfunctie die elk label
  `L` als `L + "\u0001verzoek"` geeft, en vergeleek met de verwachtingen in die labels: na het weghalen van het
  achtervoegsel 1390 van de 1390 gelijk aan `verwachtingen` en diagnose van het rapport; 128 MARKER-cases overgeslagen.
  Een eerste versie met het voorvoegsel `verzoek/` gaf 4 verschillen (case2 van `anchor/jutterswet-wachtersketen` en
  `jutterswet-wachtersketen-rgs`, beide takken), alleen in de volgorde van de deposito's in `werkelijk`: die worden als
  tekst gesorteerd, en "rangdetector @ verzoek/W3" komt na "rangdetector @ null", waar "W3" ervoor komt.
- (c) Gehaald. Nieuw: `backends/drools/src/test/java/dev/uchar/regelspraak/cases/VergelijkingWitnessTest.java`, 8 tests:
  zeven gespeelde cases die samen elke soort van `expected` dragen (ook `inconsistent` kaal, met regel, en met regel en
  label, waarvan één deposito op een gemaakt object), op beide takken gelijk aan het rapport; de labels van de
  aanroeper (`verzoek/` + caselabel, op `anchor/jutterswet-kustregister` case2) met het onderwerp van een deposito in
  die labels, en de weigering van de caselabels op die sessie; een afwijking (twee verwachtingen van
  `anchor/jutterswet-kustwacht` case1 veranderd) met de diagnose van het rapport voor hetzelfde bestand; een sessie
  niet teruggezet, niet gevuurd, en na de terugzetting bijgewerkt; een evaluatiefout aan het fixpunt
  (`corpus/rood/division-by-numeric-zero`); een label op twee beans en een object in plaats van een array; een
  onbekend label voor elk van de vier soorten; een scalaire lezing van een tijdlijnattribuut en een onbekende soort, als
  `Excluded` en als MARKER van het rapport. Tien tijdelijke mutaties, elk na de run teruggezet: geen
  terugzettoets, geen `lees`, geen fouttoets, geen dubbel-labeltoets, geen arraytoets, onbekend label niet geweigerd,
  `verwacht` en `werkelijk` verwisseld, onderwerp van een deposito zonder label, `pass()` altijd waar, diagnose leeg:
  elk laat minstens één test vallen. `verwacht`/`werkelijk` verwisseld viel eerst alleen in de rij met andere labels,
  want het rapport gaat door dezelfde ingang; daarom pint de afwijkingsrij nu één resultaat letterlijk. Geen bestaand
  testbestand gewijzigd.
- (d) Gehaald.
- (e) Alle 16 gates groen op `267ff248` (boom `fab57ee4`), 22:54:23–23:14:47; de commit daarna raakt alleen `backlog/`:
  1 PASS; 2 348 bestanden valideren en round-trippen, 859 documenten in sleutelvolgorde; 3 348 golden schoon, 511
  negatief afgewezen, 354 op hun arm; 4 873 case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite 2198 checks,
  823 cases; 7 778 cases, 45 survivors; 8 50 rode cases; 9 241 checks, 12 catala-goldens; 10 JUnit 1199 tests, 125
  rgs-goldens bytegelijk; 11 45 TOKA-bronnen bytegelijk, 29 TOKA-cases op ref, clingo en Drools; 12 255 gespeeld op
  beide takken, JUnit 1601 (waarvan de 8 nieuwe), 11:52 min; 13 43 twists over 24 goldens, 122 replays,
  vocabulaireleg 18 replays, herkomstleg 142 replays; 14 150 checks, 2 goldens; 15 266 van 303 goldens vergeleken, 125
  op de bronroute, 70 unittests; 16 30 scenario-goldens, 102 case-bestanden en hun `ir.json` bytegelijk.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: geen construct, wire, schema of
case-formaat verandert.

Open:
- De distributie kan na een pin-bump `GoldenOverHttpTest.vergelijk` en `dienst_replay.vergelijk` vervangen door
  `DroolsBackend.vergelijk`, aangeroepen in `Uitvoerder.vuur` na `zetAfgeleideWaarden`, met `label::get` (de ids van het
  verzoek) en de `expected` in die ids; dat is haar eigen kaart.
- Een `inconsistent`-verwachting met een onbekend label geeft in de replay een afwijking, waar ref de case bij het
  laden weigert. Gelijktrekken verandert `ConsistentieLabelWitnessTest`; niet gedaan.
- Vergelijken tussen twee fires van één sessie kan niet (keuze 1).
