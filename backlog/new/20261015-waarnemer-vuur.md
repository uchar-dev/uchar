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
