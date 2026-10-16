# case-invoer-publiek: de invoerstap van een case als publieke ingang van de Drools-backend

*Op verzoek van de eigenaar (2026-10-16). Kaart F van de reeks "beslisservice via dienst" (voorgangers
`waarnemer-vuur` en `vergelijking-publiek`): de werkbank laat scenario's rekenen door de beslisservice van de
distributie, en hoe een invoer beans wordt, moet dan van uchar komen. Regelnummers op `a3ac41fe`.*

Goal: de invoerstap van een case is privé in `CaseRunner`
(`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`): `beanVoor` (r. 576),
`zetAttributenEnKenmerken` (r. 624), `zetRollen` (r. 679), `spiegelWederkerigeRollen` (r. 925), de lijstvolgorde
(`Lijst`, `lijst`, `voegInvoerIn`, r. 777–816) en de tijdlijninvoer (`temporeleInvoer`, `kenmerkTijdreeks`). Elke
ingang van de replay roept die vier stappen achter elkaar aan (`invoerBeans`, `naFire`, `metSessie`, `fireRood`,
`runCase`). Een afnemer kan ze niet aanroepen. De distributie heeft daarom een kopie van het rolschrijven
(`RolSchrijver`, gebruikt door `CaseSessie` en door de dienst via `VerzoekMapper`): het zusterveld, het verre veld van
een 1:1-relatie, de spiegel van een wederkerige relatie en de lijstvolgorde van RUNTIME-SEMANTICS ch. 5.7a, buiten
uchar. Kaart G (distributie) schrapt die kopie; deze kaart maakt de ingang waarmee dat kan.

De kaart maakt publiek, in de nieuwe klasse `backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseInvoer.java`:

1. `CaseInvoer.van(DroolsBackend.Build build, JsonNode c)`: leest `asOfDate` en `input.instances` van een
   `uchar-case-v0`-case op verse beans van het beanmodel van de build, zoals de replay dat nu doet.
2. `beans()` (label → bean, in de volgorde van de case), `typen()` (label → objecttype) en `invoer()` (een
   `Invoer` voor `DroolsBackend.voegIn`: de beans in die volgorde, de emmerscalairs en elke to-many-lijst in de volgorde
   van de replay).
3. Weigeringen van een rollink met de instantie (`label`), de rol zoals de case haar schrijft (`rol`) en het genoemde
   doel (`doel`), zodat een afnemer met een eigen verzoek er een veldpad van kan maken.

De code van de invoerstap verhuist uit `CaseRunner` naar `CaseInvoer`; de waardelezer die de vergelijking ermee deelt
(`waardeNaarArg`, `tijdreeks`, `rooster`, …) blijft in `CaseRunner`. Elke ingang van de replay roept `CaseInvoer.van`
aan. Geen nieuwe vormen, geen wijziging van spec, schema of corpus.

Keuze over de invoer van de ingang: het invoerdeel van een `uchar-case-v0`-case, ook voor de dienst. De dienst
valideert zijn verzoek zelf (namen, typen, eenheden; een 400 met veldpad) en zet het gevalideerde verzoek om naar
`instances` met de ids als labels. Het alternatief, een ingang die beans aanneemt die de aanroeper maakte, laat het
maken van beans en het zetten van attributen in de distributie en geeft uchar twee invoercontracten.

Acceptance:
- (a) De publieke ingang: `public static CaseInvoer van(DroolsBackend.Build, JsonNode)`, `Map<String, Object> beans()`,
  `Map<String, Objecttype> typen()`, `Invoer invoer()`. Weigeringen, elk door `van`, dus vóór er iets in een sessie
  gaat:
  - een tweede, ander doel op een to-one referentieveld (WF-013): voorwaarts, op het verre veld van een 1:1-relatie, op
    het zusterveld van een to-many rol, en bij de spiegel van een wederkerige rol: `CaseInvoer.Rolbotsing`, een
    `IllegalArgumentException`, met `label()`, `rol()` en `doel()` van de link die de case schreef;
  - een link die het beanmodel niet kan dragen (een onbekend doellabel, geen to-one referentieveld, de
    naamdelingsgrens): `CaseInvoer.Rolweigering`, een `CaseRunner.Excluded`, met dezelfde drie velden;
  - beide met de zin van nu, zodat het rapport en de replay dezelfde reden geven; ze delen de interface
    `CaseInvoer.Rolfout`.
- (b) `CaseRunner` leest elke case via `CaseInvoer.van`; de vier stappen en de lijstvolgorde staan niet meer in
  `CaseRunner`. Meting buiten de gates: het harnas `RapportDump` (één JVM, `CaseRunner.rapport` per golden met
  case-bestanden, één regel `ScenarioResultaat.toString()` per case) met de jar van `a3ac41fe` en met die van de
  eindstand. Nulmeting op `a3ac41fe`: 300 goldens, 823 rapporten (695 PASS, 128 MARKER), twee runs bytegelijk (sha1
  `405dd7c691b6a3db576c697501930023631a1b99`). De eindstand moet bytegelijk zijn.
- (c) Gate 12 groen met ongewijzigde pins en lijsten (het script blijft ongewijzigd); `ZusterrolInvoerWitnessTest`,
  `OpslagrichtingWitnessTest` en `WederkerigZelfpaarWitnessTest` ongewijzigd groen.
- (d) Nieuwe testklasse `backends/drools/src/test/java/dev/uchar/regelspraak/cases/CaseInvoerWitnessTest.java`, die
  alleen de publieke ingang en `DroolsBackend` gebruikt: de gevallen van ch. 5.7a (één poot, beide poten, de to-one
  botsing op elk van de vier plaatsen, wederkerig, de lijstvolgorde), elke weigering met haar velden, labels die ids van
  een verzoek zijn, en `invoer()` tegen `voegIn` en de replay. Eerst rood waar het kan: elke rij valt tegen een
  tijdelijke mutatie (gemeten, niet bewaard). Geen bestaand testbestand gewijzigd.
- (e) `backends/drools/README.md`, "A session in four calls", stap 2, noemt `CaseInvoer`.
- (f) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch; gate 12 met de nieuwe tests in de
  JUnit-telling.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`, de nieuwe klasse
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseInvoer.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java` (alleen Javadoc), de nieuwe testklasse
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/CaseInvoerWitnessTest.java`, `backends/drools/README.md`,
deze kaart.

Out of scope:
- De parameters en de rekendatum als globals (`zetGlobals`): die zet de aanroeper op de sessie; `CaseInvoer` leest
  alleen de instanties.
- Een andere rolresolutie of lijstvolgorde: de kaart verhuist, ze verandert niets.
- De distributie (`RolSchrijver`, `CaseSessie`, `VerzoekMapper`, `Wereld`): kaart G, na een pin-bump.
- `spec/`, het schema, het corpus, de gate-scripts.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (2026-10-16)

`523c8715` maakt de invoerstap van een case publiek in de nieuwe klasse `CaseInvoer`; `CaseRunner` leest elke case
daar. Geen wijziging van spec, schema, corpus of gate-scripts.

- **Ingang** (`backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseInvoer.java`):
  - `public static CaseInvoer van(DroolsBackend.Build build, JsonNode c)`: leest `asOfDate` (met de strikte lezer van
    de replay) en dan `input.instances` op verse beans van `build.beanLoader()`, in vier stappen over alle instanties
    (bean, attributen en kenmerken, rollen, de omgekeerde poot van een wederkerige rol). Houdt tijdens het lezen
    `build.beanLoader()` als contextklassenlader en geeft de vorige terug, ook na een weigering. Een case zonder array
    `input.instances` geeft een `IllegalArgumentException` (was een `NullPointerException`; het schema laat zo'n case
    niet toe).
  - `public Map<String, Object> beans()` en `public Map<String, Objecttype> typen()`: per label, in de volgorde van de
    case, niet te wijzigen.
  - `public Invoer invoer()`: bij elke aanroep een verse `Invoer` met de beans in casevolgorde, elke emmerscalair en
    elke to-many-lijst in de volgorde van de replay (geschreven leden in schrijfvolgorde, dan de gespiegelde op label
    in bytevolgorde, een lid in beide één keer, waar het geschreven werd).
  - `public interface Rolfout { String label(); String rol(); String doel(); }`: de link die de case schreef, de
    instantie, de rolsleutel zoals de case haar schrijft, en het genoemde label.
  - `public static final class Rolbotsing extends IllegalArgumentException implements Rolfout`: een tweede, ander doel
    op een naar-één-veld (WF-013): op het eigen veld, op het verre veld van een 1:1-relatie, op het veld waarin een
    to-many rol staat, en bij de spiegel van een wederkerige rol. Bij de spiegel noemt zij de link die het veld van de
    gespiegelde bean schreef; daarvoor onthoudt elk naar-één-veld zijn eerste schrijver.
  - `public static final class Rolweigering extends CaseRunner.Excluded implements Rolfout`: een onbekend doellabel,
    een rol zonder naar-één-referentieveld, een verdrongen referentieveld. `CaseRunner.Excluded` is daarvoor niet meer
    `final`; de constructor blijft package-private.
  - De zinnen van alle weigeringen zijn die van `a3ac41fe`. Een afnemer vangt beide in één clausule
    (`catch (CaseInvoer.Rolbotsing | CaseInvoer.Rolweigering e)`, `e.label()`).
- **Replay** (`CaseRunner.java`): `runCase`, `fireRood`, `metSessie`, `naFire` lezen via `leesInvoer` →
  `CaseInvoer.van(bouw, c)` en geven `invoer.invoer()` aan `DroolsBackend.voegIn`; `invoerBeans` geeft een kopie van
  `CaseInvoer.van(...).beans()`. Verhuisd naar `CaseInvoer`: `beanVoor`, `zetAttributenEnKenmerken`, `zetRollen`,
  `Lijst`, `lijst`, de lijstopbouw van `voegInvoerIn`, `geldigRolveld`, `naamdelingsPoort`, `beschreven`,
  `zetToOne`, `veldklasse`, `spiegelWederkerigeRollen`, `objecttypeVanBean`, `temporeleInvoer`, `kenmerkTijdreeks`,
  en de velden `lijsten` en `emmers`. In `CaseRunner` gebleven en package-private geworden, omdat de vergelijking ze
  deelt: `eigenVeld`, `temporeel`, `gedimensioneerd`, `attribuutnaam`, `kenmerknaam`, `asOfDatum`, `tijdreeks`,
  `Rooster`, `rooster`, `emmer`, `segmentWaarde`, `grensDatum`, `segment`, `waardeNaarArg`, `argType`, `roep`,
  `lees`; nieuw `static CaseRunner lezer(Build)`.
- **Tekst**: `backends/drools/README.md`, "A session in four calls", stap 2; de klassen-Javadoc van `DroolsBackend`.

Keuze uit de kaart, door de sessie gemaakt: het invoercontract is het invoerdeel van `uchar-case-v0`, ook voor de
dienst. Reden: één lezer van een invoer, wie haar ook levert. Een ingang die beans van de aanroeper aanneemt, laat het
maken van beans, het zetten van attributen onder hun gedeclareerde naam, de emmerconventie en de kenmerktijdlijnen in
de distributie, en geeft uchar een tweede contract naast het gepubliceerde schema; het beanmodel zou dan ook
invoercontract zijn. Prijs voor de dienst: zij zet haar gevalideerde Java-waarden terug naar case-waarden, en een
rolbotsing komt pas na haar eigen validatie, één tegelijk.

### Acceptatie

- (a) Gehaald, met de signaturen hierboven.
- (b) Gehaald. Harnas `RapportDump` buiten de gates (één JVM, `CaseRunner.rapport` per golden met case-bestanden, één
  regel `ScenarioResultaat.toString()` per case): de jar van `a3ac41fe` tweemaal en de jar die gate 12 op `523c8715`
  bouwde: alle drie 300 goldens, 823 rapporten (695 PASS, 128 MARKER), sha1
  `405dd7c691b6a3db576c697501930023631a1b99`; bytegelijk. Looptijd van het harnas gelijk (4,9 s).
- (c) Gehaald. Gate 12 groen op `523c8715` met `scripts/check-drools.sh` ongewijzigd: 255 gespeeld op beide takken,
  de pin van 255 en de 48 niet-gespeelde goldens bij naam gelijk; JUnit 1619 tests (was 1601, plus de 18 nieuwe), nul
  overgeslagen. `ZusterrolInvoerWitnessTest` (5), `OpslagrichtingWitnessTest` (15) en `WederkerigZelfpaarWitnessTest`
  (12) ongewijzigd groen; een WF-013-botsing blijft een `IllegalArgumentException` uit de invoerstap, vóór `voegIn`.
- (d) Gehaald. Nieuw: `backends/drools/src/test/java/dev/uchar/regelspraak/cases/CaseInvoerWitnessTest.java`, 18
  tests, alleen via `CaseInvoer` en `DroolsBackend` (en voor de lijstvolgorde de package-private `Invoer.lijsten()` en
  `Invoer.emmers()`): ketenband case1–3 (1:N, één poot of twee) en kavelgrens case1–2 (1:1, het verre veld) op
  beanniveau; maatschap (wederkerig, één poot en twee poten); de botsing op elk van de vier plaatsen met label, rol en
  doel (maatschap J1 `maat` [J2, J3]; kavelgrens K1 en K3 met `zuidbuur` K2; ketenband W2 `voorligger` W3 vóór W1
  `opvolgers` [W2]; maatschap J1→J2, J2→J3 in twee volgordes); de drie weigeringen met hun zin en velden (onbekend
  label W9, de sleutel `buur`, een attribuut `voorligger` dat het referentieveld verdringt); ketenband case2 met
  verzoekids `wachter/1..3`, gespeeld via de ingangen, gelijk aan het rapport op de labels na, en een botsing met ids;
  de lijstvolgorde [W9, W3, W10, W2] en de vouwfout die zij kiest (geschreven V9, V10: "4 gedeeld door 0"; gespiegeld:
  "9 gedeeld door 0"); de emmerscalair op penningjaar en geen bij een volledige tijdlijn; een verse `Invoer` per
  aanroep en niet te wijzigen maps; de contextklassenlader na `van` en na een weigering; een case zonder instances; en
  het hele corpus (300 goldens, 823 cases) gespeeld via `van`, `voegIn`, `vuur`, `zetAfgeleideWaarden` en `vergelijk`,
  per case gelijk aan `CaseRunner.rapport`. Achttien tijdelijke mutaties, elk na de run teruggezet (script
  `mutaties.py` buiten de repo): velden van de botsing verwisseld; spiegelbotsing met de link van de partner; geen
  verre veld; geen spiegel; gespiegelde leden in invoegvolgorde; gespiegelde leden eerst; een lid dubbel; één gedeelde
  `Invoer`; `beans()` wijzigbaar; contextklassenlader niet terug; onbekend label, geen referentieveld en verdrongen veld
  als kale `Excluded`; laatste schrijver wint; geen emmerscalair; geen instances-toets; geen zusterveld; en in
  `CaseRunner.runCase` een `Invoer` zonder emmers en lijsten. Elke mutatie laat minstens één test vallen, en elke
  test valt onder minstens één mutatie. Geen bestaand testbestand gewijzigd.
- (e) Gehaald.
- (f) Alle 16 gates groen op `523c8715` (boom `109256ba`), 10:27:04–10:46:31; de commit daarna raakt alleen
  `backlog/`: 1 PASS; 2 348 bestanden valideren en round-trippen, 859 documenten in sleutelvolgorde; 3 348 golden
  schoon, 511 negatief afgewezen, 354 op hun arm; 4 873 case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite
  2198 checks, 823 cases; 7 778 cases, 45 survivors; 8 50 rode cases (Drools: 26 met fout, 24 niet gedragen); 9 241
  checks, 12 catala-goldens; 10 JUnit 1199 tests, 125 rgs-goldens bytegelijk; 11 45 TOKA-bronnen bytegelijk, 29
  TOKA-cases op ref, clingo en Drools; 12 255 gespeeld op beide takken, JUnit 1619, 10:25 min; 13 43 twists over 24
  goldens, 122 replays, vocabulaireleg 18 replays, herkomstleg 142 replays; 14 150 checks, 2 goldens; 15 266 van 303
  goldens vergeleken, 125 op de bronroute, 70 unittests; 16 30 scenario-goldens, 102 case-bestanden en hun `ir.json`
  bytegelijk. Het harnas van (b) op de jar die gate 12 bouwde: bytegelijk.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: geen construct, wire, schema of
case-formaat verandert.

Open:
- Eén zichtbare verandering buiten het corpus: een case met een WF-013-botsing gaf in het rapport en op de rode route
  "IllegalArgumentException: …", en geeft nu "Rolbotsing: …", want de ERROR-regel noemt de klasse; de zin is gelijk.
  Geen golden, rode case of TOKA-case heeft zo'n botsing (gemeten: het rapport is bytegelijk, gate 8 en 11 groen).
- Commentaar dat de verhuisde methode bij haar oude plaats noemt: `ZusterrolInvoerWitnessTest` r. 28
  (`{@link CaseRunner#zetRollen}`), `OpslagrichtingWitnessTest` r. 30 en 81 (`CaseRunner.zetRollen`), en twee
  dateringen in `scripts/check-drools.sh` (r. 359, 368, beschermd pad). Alleen commentaar; een patch voor de twee
  testbestanden ligt bij het verslag, niet toegepast.
- De lezer voegt twee instanties met hetzelfde label stil samen (de eerste bean krijgt ook de attributen van de
  tweede), en een rolwaarde die geen array is, leest hij als leeg. Zo was het in `CaseRunner`; het schema laat beide
  niet toe. Weigeren verandert de lezer en hoort bij een eigen kaart.
- De parameters en `asOfDate` als globals zet de aanroeper (`zetGlobals` blijft in `CaseRunner`); de distributie
  houdt daarvoor een eigen waardelezer.
- Kaart G (distributie) kan na een pin-bump `RolSchrijver` schrappen; zie het verslag van deze sessie voor de schets.
