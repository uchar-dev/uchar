# waarnemer-vuur: wat een sessie vuurde en welke vormen dat dekt, als publieke ingang van de Drools-backend

*Op verzoek van de eigenaar (2026-10-15). Eerste kaart van een reeks: de RegelSpraak-werkbank laat scenario's via de
echte code van de beslisservice draaien en wil daarvan trace, dekking en fixpunt. Besluit van de eigenaar: de semantiek
daarvan hoort in uchar, de distributie (regelspraak-toolchain) krijgt alleen dunne adapters. Regelnummers op
`56dc218b`.*

Goal: de Drools-backend neemt het vuren van een sessie nu alleen privé waar. `CaseRunner.luisteraar`
(`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java:1369`–`1386`) registreert per vuring de
exacte vormnaam (`TmsTranspiler.vormVan`), de telling en de labels per `RegelSpraakModel.vormnaam`, en `rapport`
(r. 171–220) maakt daaruit `ScenarioResultaat.Vuring`, `Trace` en `DekkingRegel` (GEVUURD / NIET_GERAAKT, via
`gevuurd`, r. 242). `DroolsBackend.vuur(Build, KieSession)` (`DroolsBackend.java:726`) is publiek en heeft geen
waarnemer. De distributie kopieert de listener in haar `CaseSessie` zonder de exacte vormnamen, en kan de dekking
daardoor niet bepalen zonder een derde kopie.

De kaart maakt in `DroolsBackend` publiek:

1. een waarnemer op het vuren: `DroolsBackend.Waarnemer`, gemaakt met een functie bean → label die de aanroeper levert
   (het label van een bean is casusafhankelijk; de distributie heeft haar eigen labeltabel), en
   `DroolsBackend.vuur(Build, KieSession, Waarnemer)`. De waarnemer legt per vuring de exacte vormnaam vast, en per
   `vormnaam` de telling en de labels van de invoerbeans die de match bond. Hij hoort bij één sessie en houdt geen
   gedeelde toestand; zijn listener staat alleen tijdens die `vuur` op de sessie.
2. de dekking als functie van build en gevuurde vormen: `DroolsBackend.dekking(Build, Set<String>)`, één
   `DekkingRegel` per post van `Build.dekking()`, in die volgorde, GEVUURD als de exacte naam van de post bij de gevuurde
   vormen hoort en anders NIET_GERAAKT; de indeling die `rapport` nu gebruikt.

`CaseRunner.rapport` gebruikt beide zelf, zodat het rapport over het corpus laat zien dat er niets verschuift. Geen
nieuwe vormen, geen wijziging van spec, schema of corpus.

Acceptance:
- (a) De publieke ingang. `DroolsBackend.Waarnemer` met de constructor `Waarnemer(Function<Object, String> label)`,
  `List<ScenarioResultaat.Vuring> vuringen()` (per vorm zonder versiesuffix, in de volgorde waarin een regel van de
  vorm het eerst vuurde, met de labels in de volgorde waarin ze het eerst gebonden werden; een regel zonder vorm heeft
  geen rij) en `Set<String> vormen()` (de exacte namen, versiesuffix inbegrepen);
  `DroolsBackend.vuur(Build, KieSession, Waarnemer)` met dezelfde `Vuring`, dezelfde weigeringen en dezelfde fout als
  `vuur(Build, KieSession)`; `DroolsBackend.dekking(Build, Set<String>)`. Een waarnemer die al een andere sessie
  waarnam, wordt geweigerd (`IllegalArgumentException`), en een gevuurde vorm die geen post van het manifest is ook.
  Dat laatste mag het rapport niet raken: (b) meet het over het corpus.
- (b) `CaseRunner.rapport` vuurt via `vuur(Build, KieSession, Waarnemer)` met `this::labelVanBean` en maakt de dekking
  met `DroolsBackend.dekking`; `CaseRunner.luisteraar`, `CaseRunner.gevuurd` en de velden `telling`, `labels` en
  `vormen` van `Waarneming` verdwijnen. Meting buiten de gates: een harnas schrijft `rapport` voor elke golden met
  case-bestanden, één regel per case (`ScenarioResultaat.toString()`), met de jar van `56dc218b` en met die van de
  eindstand. Nulmeting op `56dc218b`: 300 goldens, 823 rapporten (695 PASS, 128 MARKER), twee runs bytegelijk
  (sha1 `405dd7c691b6a3db576c697501930023631a1b99`). De eindstand moet bytegelijk zijn.
- (c) Nieuwe testklasse `backends/drools/src/test/java/dev/uchar/regelspraak/cases/WaarnemerWitnessTest.java`, die de
  sessie opzet zoals een afnemer (`CaseRunner.metSessie`, of zelf met `voegIn`) en alleen de publieke ingang vuurt:
  - op `jutterswet-uitsluiting-rgs` geeft de waarnemer op beide takken de trace, de vormen en de dekking die het
    rapport vastlegt (`defaults K:Jutter.uitgesloten [M] 2`; `penninggerechtigd tenzij uitgesloten` NIET_GERAAKT);
  - op `anchor/jutterswet-jutheffing` noemt `vormen()` de versie die vuurde (`#1`) en niet de andere (`#2`), terwijl
    `vuringen()` de versies in één rij geeft;
  - op een golden met een creatie, die in generaties vuurt, telt de waarnemer de creatievormen mee;
  - twee sessies van één build, op twee threads tegelijk gevuurd, elk met een eigen waarnemer en een eigen
    labelfunctie: elke waarnemer geeft precies wat dezelfde sessie alleen gevuurd geeft, en niets van de andere;
  - de listener staat alleen tijdens `vuur` op de sessie: na `vuur` heeft de sessie dezelfde listeners als ervoor, ook
    na een `vuur` die weigert; een vuring buiten `vuur` (een kale `fireAllRules`) ziet de waarnemer niet;
  - een waarnemer van een andere sessie en een vorm buiten het manifest worden geweigerd, met een zin die zegt wat er
    mis is.
  Eerst rood waar het kan: elke rij valt tegen een tijdelijke mutatie (gemeten, niet bewaard). Geen bestaand testbestand
  gewijzigd.
- (d) `backends/drools/README.md`, "A session in four calls", noemt de waarnemer en de dekking.
- (e) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch; gate 12 met de nieuwe tests in de JUnit-telling.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`, de nieuwe testklasse
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/WaarnemerWitnessTest.java`, `backends/drools/README.md`,
deze kaart.

Out of scope:
- Het fixpunt als onderdeel van de waarneming en de rest van de reeks (trace en dekking in de distributie, de
  werkbank): eigen kaarten.
- `ScenarioResultaat` (de records blijven zoals ze zijn), `Sessiedump` (de naad heeft haar eigen listener), de
  DRL-tekst en de verlaging.
- De distributie en de werkbank: die nemen de ingang over na een pin-bump, op hun eigen kaart.
- `spec/`, het schema, het corpus, de gate-scripts.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (2026-10-15)

`2885aae2` maakt in `DroolsBackend` publiek wat het scenariorapport tot dan privé deed: een waarnemer op het vuren en
de dekking als functie. Geen wijziging van spec, schema, corpus of gate-scripts.

- **Ingang** (`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`):
  - `public static final class Waarnemer`, met `Waarnemer(Function<Object, String> label)`,
    `List<ScenarioResultaat.Vuring> vuringen()` en `Set<String> vormen()`. De listener registreert per vuring van een
    regel met een vorm (`TmsTranspiler.vormVan`) de exacte vormnaam, en per `RegelSpraakModel.vormnaam` de telling en
    de labels die `label` geeft (null: geen label), elk label één keer. De eerste `vuur` bindt de waarnemer aan zijn
    sessie (gesynchroniseerd); een `vuur` van een andere sessie geeft een `IllegalArgumentException`.
  - `public static Vuring vuur(Build, KieSession, Waarnemer)`: zet de listener op de sessie, roept dezelfde run aan als
    `vuur(Build, KieSession)` en haalt de listener in een `finally` weer weg, ook bij een weigering of een fout uit het
    vuren.
  - `public static List<ScenarioResultaat.DekkingRegel> dekking(Build, Set<String>)`: één regel per post van
    `Build.dekking()`, in die volgorde, GEVUURD bij een exacte naamgelijkheid, anders NIET_GERAAKT; een gevuurde vorm
    die geen post is, geeft een `IllegalArgumentException` (vormen van een andere build). `TmsTranspiler.meet` schrijft
    de post en de vorm van elke regel onder dezelfde naam, dus een sessie van de build zelf komt daar niet.
- **Rapport** (`CaseRunner.java`): `rapport` maakt per case een `DroolsBackend.Waarnemer(runner::labelVanBean)`, vuurt
  via `vuurTotFixpunt(sessie, waarnemer)` → `DroolsBackend.vuur(bouw, sessie, waarnemer)`, en neemt trace en dekking
  uit `vuringen()` en `DroolsBackend.dekking(bouw, vormen())`. Weg: `luisteraar`, `gevuurd`, het veld `dekking` en de
  velden `telling`, `labels` en `vormen` van `Waarneming`. De listener stond eerst vanaf vóór `voegInvoerIn` op de
  sessie en nu alleen tijdens `vuur`; buiten `vuur` vuurt de replay niets, dus dat verschil is onzichtbaar (zie (b)).
- **Tekst**: `backends/drools/README.md`, "A session in four calls", stap 3; de klassen-Javadoc van `DroolsBackend`.

### Acceptatie

- (a) Gehaald, met de signaturen hierboven.
- (b) Gehaald. Harnas buiten de gates (één JVM, `CaseRunner.rapport` per golden met case-bestanden, één regel
  `ScenarioResultaat.toString()` per case): de jar van `56dc218b` tweemaal en de jar die gate 12 op `2885aae2` bouwde
  geven alle drie 300 goldens, 823 rapporten (695 PASS, 128 MARKER), sha1
  `405dd7c691b6a3db576c697501930023631a1b99`; bytegelijk. Geen rapport gooit op de manifesttoets van `dekking`.
- (c) Gehaald. Nieuw: `backends/drools/src/test/java/dev/uchar/regelspraak/cases/WaarnemerWitnessTest.java`, 9 tests:
  trace, vormen en dekking op `jutterswet-uitsluiting-rgs` op beide takken en gelijk aan het rapport; de versie in
  `vormen()` op `anchor/jutterswet-jutheffing`; één label per rij op `jutterswet-premiestaffel` (20 vuringen, 14
  labels); de creatie op `jutterswet-wrakboeking`; twee sessies van één build, met eigen beans via `voegIn`, 20 rondes
  tegelijk op twee threads (een `CyclicBarrier` vlak vóór `vuur`), elk gelijk aan de solo-uitkomst; dezelfde
  listeners na `vuur`, en een kale `fireAllRules` na een `update` telt niet mee; dezelfde listeners na een geweigerde
  tweede `vuur` op een creatiemodel; de weigering van een waarnemer van een andere sessie (de tweede sessie vuurt daarna
  gewoon met een eigen waarnemer); de weigering van een vorm buiten het manifest. Acht tijdelijke mutaties van
  `DroolsBackend.java`, elk na de run teruggezet: gedeelde (statische) toestand (o.a. de twee-sessies-test:
  `expected: <[penninggerechtigd tenzij uitgesloten [B] 1]> but was: <[defaults K:Jutter.uitgesloten [A] 2,
  penninggerechtigd tenzij uitgesloten [B] 1]>`), listener niet weggehaald (2 tests), geen sessiebinding, geen
  manifesttoets, vormnaam zonder versie, creatieregels overgeslagen, labels niet ontdubbeld: elk laat minstens één
  test vallen. De rij over premiestaffel kwam erbij nadat de mutatie "labels niet ontdubbeld" de eerste acht rijen
  overleefde (in de bestaande `ScenarioRapportWitnessTest` viel zij wel). Grens van de twee-sessies-test: de fires
  duren een paar vuringen, dus hoeveel ze in de tijd overlappen is niet gemeten; gedeelde toestand valt ook zonder
  overlap. Geen bestaand testbestand gewijzigd.
- (d) Gehaald.
- (e) Alle 16 gates groen op `2885aae2` (boom `0d87f63b`), 15:25:36–15:45:41; deze commit raakt daarna alleen
  `backlog/`: 1 PASS; 2 348 bestanden valideren en round-trippen, 854 documenten in sleutelvolgorde; 3 348 golden
  schoon, 506 negatief afgewezen, 349 op hun arm; 4 873 case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite
  2198 checks, 823 cases; 7 778 cases, 45 survivors; 8 50 rode cases; 9 241 checks, 12 catala-goldens; 10 JUnit 1185
  tests, 125 rgs-goldens bytegelijk; 11 45 TOKA-bronnen bytegelijk, 29 TOKA-cases op ref, clingo en Drools; 12 255
  gespeeld op beide takken, JUnit 1587 (was 1578, plus de 9 nieuwe); 13 43 twists over 24 goldens, 122 replays,
  vocabulaireleg 18 replays (46 gestript, 36 geroteerd); 14 150 checks, 2 goldens; 15 266 van 303 goldens
  vergeleken, 125 op de bronroute, 69 unittests; 16 30 scenario-goldens, 102 case-bestanden en hun `ir.json`
  bytegelijk.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: geen construct, wire, schema of
case-formaat verandert.

Open:
- De distributie kan na een pin-bump de listener in `CaseSessie` (de velden `telling` en `labels`, de
  `addEventListener`, de opbouw van de vuringen en `luisteraar()`) vervangen door een `DroolsBackend.Waarnemer` met
  haar eigen labelfunctie, en de dekking geven met `DroolsBackend.dekking`; dat is haar eigen kaart.
- Het fixpunt als deel van de waarneming: volgende kaart van de reeks.
