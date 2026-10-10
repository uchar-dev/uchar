# tms-omschakeling: de truth-maintenance-verlaging wordt de exec-modeltak, en de destructieve verlaging verdwijnt

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08): de truth-maintenance-tak vervangt de exec-modeltak, ook in de
toolchain. De vierde en laatste bouwkaart van de tak, na
`20261008-tms-verdeling-en-tijdlijnen`. Regelnummers op uchar `3619921b`.*

## Aanleiding

`DroolsBackend.Branch` heeft drie waarden (`cases/DroolsBackend.java` regel 75):
`EXEC_MODEL`, de destructieve verlaging in
`transpiler/model/ExecModelTranspiler.java` (9898 regels), die gate 12 speelt
(`EXPECTED_PLAYED=240`, `scripts/check-drools.sh` regel 1253) en de toolchain
gebruikt; `DRL_TEXT`; en `TRUTH_MAINTENANCE`, de tak achter `--tms` die de drie
bouwkaarten hiervoor uitbouwen. Het `Result:` van de proefkaart: "Ze groeit tot
de dekking van de huidige exec-modeltak (gate 12: 240) en vervangt die dan, ook
in de toolchain."

De toolchain bouwt met `DroolsBackend.Branch.EXEC_MODEL`
(regelspraak-toolchain
`toolchain/src/main/java/dev/uchar/regelspraak/toolchain/engine/RegelSpraakEngine.java`
regel 57, gemeten op 2026-10-07) en vuurt en leest zelf
(`service/.../Uitvoerder.java` regel 69, `CaseSessie`). Met truth maintenance
staan de afgeleide waarden als logische feiten in de sessie; ze komen pas op de
bonen via `DroolsBackend.zetAfgeleideWaarden(Build, KieSession)` (regel 270).
De proeftak riep die na één `fireAllRules` aan; een dienst die invoer in een
lopende sessie verandert en opnieuw vuurt, vraagt ook dat een veld leeg wordt
als zijn feit verviel (open punt 2 van de proefkaart).

Goal: `Branch.EXEC_MODEL` bouwt via de truth-maintenance-verlaging; de
destructieve verlaging, de waarde `TRUTH_MAINTENANCE` en de vlag `--tms`
verdwijnen; gate 12 speelt ten minste dezelfde goldens met dezelfde
verwachtingen; de ingang voor afnemers beschrijft het terugzetten; de toolchain
volgt met een eigen kaart.

## Wat er gebeurt

1. **Voorwaarde.** Een doorloop van beide takken over het hele corpus, op de
   commit waar deze kaart begint: de truth-maintenance-tak speelt elke golden
   die de exec-modeltak speelt, met dezelfde namen en zonder FAIL. Een golden
   die verschilt, gaat terug naar een bouwkaart, niet naar een lijst; gate 12
   mag niet teruglopen (`CLAUDE.md`, "Gate set").
2. **De omschakeling.** `EXEC_MODEL` bouwt via de truth-maintenance-verlaging.
   Het pakket `transpiler/tms/` heet dan niet meer de proeftak; de bouwer kiest
   of het pakket blijft of verhuist. `TRUTH_MAINTENANCE` en `--tms` verdwijnen
   zonder alias.
3. **Wat weggaat.** `ExecModelTranspiler` en wat alleen hij gebruikt; de
   getuigen die de destructieve maatregelen toetsen, of ze worden omgezet naar
   de nieuwe verlaging; het `Result:` noemt elke verwijderde getuige met de
   reden. `Stapels` is daarna de enige samenstelling van ch. 7.10 en geen kopie
   meer. `Stratificatie` blijft tot `20261008-drl-teksttak-op-truth-maintenance`,
   omdat `DrlTranspiler` haar nog gebruikt.
4. **Terugzetten bij herhaald vuren.** `zetAfgeleideWaarden` maakt een veld leeg
   waarvan het feit verviel, zodat een afnemer na elke `fireAllRules` dezelfde
   functie kan aanroepen. Getuige: twee keer vuren in één sessie, met tussendoor
   een invoerwijziging die een afleiding laat vervallen.
5. **Gates** (beschermd pad `scripts/`: **voorstel**). Gate 12: de lijsten van
   de exec-modeltak volgen de nieuwe verlaging; `EXPECTED_PLAYED` loopt niet
   terug; een golden die alleen om een fout van de destructieve verlaging op de
   lijst van waardeverschillen stond, gaat eraf. Gate 13 speelt het pad van gate
   12; zijn tellingen en vloeren gaan mee. De corpusgetuige `TmsCorpusWitnessTest`
   gaat op in gate 12, of blijft met een reden.
6. **README.** De sectie "The truth-maintenance trial branch" wordt de
   beschrijving van de exec-modeltak; "The entry for consumers" beschrijft het
   terugzetten na elke `fireAllRules`. Elke sectie "Recorded: …" die gedrag van
   de destructieve verlaging beschrijft (onder meer "two writers of one target"
   en "a reader after the writers of what it reads"), wordt herschreven voor de
   nieuwe verlaging of verdwijnt, met de reden.
7. **Meting.** `TmsMeting` over alle gespeelde goldens, oud tegen nieuw
   (vuringen, tijd van vuren en terugzetten, bouwtijd), als tabel in het
   `Result:`, voor de dienst van de toolchain. Geen drempel.
8. **De toolchain.** Het `Result:` geeft de tekst van een kaart voor de
   distributie (`uchar-dev/regelspraak-toolchain`): de pin verzetten en na elke
   `fireAllRules` het terugzetten aanroepen in `Uitvoerder` en `CaseSessie`. Die
   kaart landt daar, na deze.

## Aanvulling na `20261008-tms-aggregaties-creatie-en-fouten` (2026-10-09)

Uit het `Result:` van die kaart (waarneming 11, het advies, de open punten) en
het besluit van de eigenaar van 2026-10-08 (PLAN §9):

9. **Schaalproef met vloer** (besluit van de eigenaar). Eén Kustwacht, N
   Wachters met elk 10 Vondsten; (a) een regel per Vondst uit invoer maal een
   parameter; (b) een defaults-groep op de Vondst met een onvoorwaardelijke
   lagere default en een hogere die (a) leest; (c) een som en (d) een maximum per
   Wachter over (b); (e) een som over alle Wachters op de Kustwacht; (f) een
   consistentie per Wachter; (g) een recursieve creatieketen van diepte 3 per
   Wachter. Voor N ∈ {100, 1 000, 10 000}: vuringen tegen het plafond, tijd van
   vuren en terugzetten, piekaantal feiten en heap, bouwtijd, de verhouding met
   de exec-modeltak van vóór de omschakeling, en de helling in log-log; plus een
   tweede fire na één invoerwijziging tegen een verse sessie. **Vloer:** helling
   ten hoogste 1,2 en vuringen onder de helft van het plafond bij N = 10 000.
   Haalt de verlaging dat niet, dan eerst de joins indexeren (`alphaIndexedBy` op
   de lidsleutel, `betaIndexedBy` op het onderwerp, gelijke ids voor gelijke
   constraints, zodat Drools knopen deelt), en dan opnieuw meten. Gelezen door de
   adviseur op `42859692`: elke join is een ongeïndexeerde lambda, elke
   constraint heeft een eigen id (`id()` in `TmsTranspiler`), `min` en `max`
   rekenen bij een intrekking alles opnieuw (`supportsReverse()` is daar false in
   Drools 8.44), en het plafond van 100 000 vuringen
   (`CaseRunner.java` regel 576) is met ongeveer 5 000 objecten × 20 leden al
   bereikt.
10. **Een vuurplafond dat met de invoer meegroeit**, in plaats van de vaste
    100 000 (de driver `DroolsBackend.vuur` krijgt het plafond als argument).
11. **Herhaald vuren na het terugzetten.** Na `zetAfgeleideWaarden` staan de
    afgeleide waarden op de bonen, en de patronen op de bonen lezen ze bij een
    volgende fire als invoer: een initialisatie wordt dan geblokkeerd door een
    waarde "uit de invoer" die zij zelf schreef. Punt 4 lost dat op (niet
    terugzetten tussen twee fires, of de bonen alleen invoer laten dragen), en
    de getuige van punt 4 draait ook een initialisatie.
12. **Het contract voor afnemers**, in de README (punt 6) en in de kaarttekst
    voor de toolchain (punt 8): een model met creaties vuurt via de driver
    `DroolsBackend.vuur` (strata, generaties, de agendagroepen
    `TmsTranspiler.FEITCREATIE` en `OBJECTCREATIE`), die nog niet publiek is;
    deze kaart beslist hoe een afnemer hem aanroept (de javadoc van
    `DroolsBackend` verwijst al hierheen). Tot dan weigert
    `zetAfgeleideWaarden` een sessie van zo'n model die zonder de driver vuurde.
    Bij het vullen van een sessie komen de
    `Lijstplaats`-feiten mee als `TmsTranspiler.leestLijstplaatsen()` dat vraagt;
    `zetAfgeleideWaarden` gooit `DelingDoorNul` bij één fout en
    `Evaluatiefouten` bij twee of meer; en gecreëerde links staan nog niet op de
    referentievelden van de bonen (alleen als `Koppeling`-feit), wat de afnemer
    moet weten of wat deze kaart oplost. Nieuw publiek in het pakket van de tak
    sinds die kaart: `Evaluatiefouten`, `Lijstplaats`, `Creatiefase`, en in
    `TmsTranspiler` `FEITCREATIE`, `OBJECTCREATIE`, `creatieregels()`,
    `creatiestrata()`, `lidstratum(…)` en `leestLijstplaatsen()`.

## Aanvulling na `20261008-tms-expressievormen-en-rekendatum` (2026-10-09)

Uit het `Result:` van die kaart (open punten en bijvangst). De tak speelt daarna 196 van de 242 goldens van de
exec-modeltak; de overige 46 weigert hij eerst op een vorm van `20261008-tms-verdeling-en-tijdlijnen`.

13. **Gedrag dat alleen de destructieve exec-modeltak heeft** (afspraak van de eigenaar van 2026-10-09: geen eigen
    kaart; het verdwijnt met de omschakeling, en de doorloop van punt 1 telt het niet als verlies):
    - een versie met een begrensde geldigheid in een sessie zonder rekendatum speelt, waar ref faalt;
    - de terugvalwaarde van een som in een andere eenheid wordt niet herrekend (1 waar 1000 hoort);
    - een irrationale wortel zonder afronding geeft een benadering, waar ref faalt;
    - een deling door leeg deponeert niet in een voorwaarde, niet bij een operand die geen gelezen attribuut is ("5
      gedeeld door zijn noemer"), en niet in het filter van een vouw;
    - een tekstdeel van een getal of een waarheidswaarde in een tekstsamenstelling schrijft "5.50" en "true"; de tak
      met truth maintenance weigert zo'n deel met naam tot `20261009-tekst-van-een-waarde-in-een-tekstsamenstelling`
      de norm vastlegt.
14. **Voor de schaalproef van punt 9:** uniciteit vergelijkt elke sleutel met elke sleutel, zoals de exec-modeltak;
    punt 5 van die kaart (de leeslaag) is niet gebouwd, dus de gesloten woordenschat met gelijke ids, waarmee Drools
    knopen deelt, is hier het middel als de vloer niet gehaald wordt; de kolom "Los" van `TmsMeting` kan sinds
    `e92d5fee` niet meer gevuld raken.
15. **Voor het contract van punt 12:** `zetAfgeleideWaarden` gooit bij één staande fout ook `Rekenfout` (een
    evaluatiefout van een rekenvorm of van de rekendatum) of `Schrijversconflict` (twee schrijvers van één doel, ch.
    5.7). De schrijversfout noemt een object bij zijn caselabel alleen via
    `TmsTranspiler.zetAfgeleideWaarden(KieSession, Function)`, die de replay gebruikt; een afnemer van de publieke
    ingang ziet type en identiteit. Nieuw publiek in het pakket van de tak sinds die kaart: `Filterdeponering`,
    `Lidtoets`, `Rekendatum`, `Rekenfout`, `Schrijversconflict`, `Uniciteitsschending`, `Uniciteitssleutel`.
16. **README (punt 6):** "Recorded: two writers of one target" zegt dat ref bij drie schrijvers alle drie noemt;
    gemeten noemt ref de twee die het laatst veranderden, en de tak met truth maintenance elke schrijver die aan het
    fixpunt staat.
17. **Nog geweigerd, zonder golden:** een vouw, bereik of stap over een gecreëerde relatie, en een binder van een
    omsluitende `Let` in het filter of de matrix van een vouw (de oude takken weigeren die ook). Een gewone regelset
    van de distributie kan ze bevatten; voorstel daar: eerst een golden.

## Aanvulling na `20261008-tms-verdeling-en-tijdlijnen` (2026-10-09)

Uit het `Result:` van die kaart. De tak met truth maintenance speelt daarna alle 242 goldens die de exec-modeltak speelt,
met dezelfde namen en zonder FAIL (de doorloop van punt 1, gemeten op haar tip).

18. **Gedrag dat alleen de destructieve exec-modeltak heeft** (afspraak van de eigenaar van 2026-10-09: geen eigen
    kaart; het verdwijnt met de omschakeling, en de doorloop van punt 1 telt het niet als verlies):
    - een pot die een regel later afleidt: het restant blijft leeg;
    - een lege pot in gelijke delen of naar rato: een NullPointerException in de consequentie;
    - een verdeling met twee versies (geldigheid) wordt geweigerd als "twee verdelingen schrijven hetzelfde doel";
    - `rsVerdeelOpVolgorde`: een tiegroep met alleen lege gewichten krijgt gelijke delen (ref 0, 0 en de pot gaat door),
      en sleutels als 2019 en 2019.0 vormen twee groepen (ref één); de tak met truth maintenance rekent met
      `Verdeling.bereken`, dus de hulp kan met de exec-modeltak weg of gerepareerd worden;
    - een rekenende regel naast een verdeling op één doel speelt 100 waar ref faalt (ch. 5.7);
    - een tarief waarvan het document de noemer niet declareert: 120 waar ref en clingo falen (ch. 3.6);
    - `CaseRunner` zet een asOfDate-emmerscalair als segment op de boon (bewust, javadoc van `temporeleInvoer`), zodat
      de exec-modeltak antwoordt waar ref en clingo volgens ch. 8.4 falen: een knip (11), een lid van een vouw (135),
      een ooit-sleutel (inconsistent), het eigen totaal over een volledige tijdlijn (126); bij de basis van een pro rata
      (306) schrijven ref en clingo niets;
    - "gelijk aan" en "ongelijk aan" in een knip geven 0 waar ref 50 geeft (`20261009-runtime-tijdconditie-leeg`);
    - een verifier-schone regel die een gedimensioneerd attribuut schrijft ("De omzet van een Jutter moet berekend
      worden als 5"), stopt bij het vuren met een ClassCastException (BigDecimal naar Map), waar de vorm luid geweigerd
      hoort te worden (de DRL-teksttak weigert haar bij het compileren, de tak met truth maintenance met naam).
19. **Voor het contract van punt 12:** een afnemer die zelf een sessie vult, stelt per asOfDate-emmerscalair op een
    tijdsafhankelijk attribuut `new Emmerinvoer(boon, ProducentConsument.elementVan(type, attribuut, false))`; zonder
    dat feit leest de tak elke invoertijdlijn als volledige tijdlijn (ch. 8.4). De `Lijstplaats`-feiten van punt 12
    vraagt sinds die kaart ook een kwantorvouw over een meervoudige rol waarvan de matrix een attribuut vergelijkt of
    toetst onder "gedurende het gehele" (de vorm van `jutterswet-reddingsvloot`), ook zonder één emmerscalair; zonder die
    feiten stopt het vuren met een IllegalStateException. De javadoc van `Lijstplaats` en `leestLijstplaatsen` noemt
    alleen de filterfout als lezer; ook de lidfoutregel en de geordende kwantorvouw lezen hem (tekstvoorstel in het
    verslag van de eindfix van die kaart). Nieuw publiek in het pakket van de
    tak: `Emmerinvoer`, `Emmerscalair`, `Invoervormfout`, `Lidfout`, `Ontvanger`, `Tijdvak`, `Verdeling`.
20. **Voor de schaalproef van punt 9:** een verdeling vuurt per bron opnieuw bij elke verandering van een ontvanger,
    dus O(N²) als N ontvangers één voor één binnenkomen; tijdlijnen en verdelingen zitten niet in het synthetische model.
21. **Nog geweigerd, zonder golden** (de README van de tak somt ze op, met reden): een variabel startpunt, een
    tijdsafhankelijke parameter, een som over tijdlijnen, een tijdlijndoel met een tweede schrijver, een
    tijdsafhankelijk kenmerk als doel, "(on)gelijk aan" in een knip, de ontkenning binnen de kwalificatie op een kenmerk,
    een kwalificatie langs een pad of tussen twee tijdlijnen, een duur in dagsoorten tussen datum-tijden, een
    dagsoortdefinitie met een parameter (`jutterswet-havenrooster`, ook de exec-modeltak), een tweede schrijver van een
    verdelingsdoel, en een vouw per lid of een kwantorvouw met de kwalificatie als matrix over een gecreëerde relatie.

## Acceptatie

- (a) De doorloop van punt 1, als tabel in het `Result:`.
- (b) Getuige van punt 4, rood eerst.
- (c) `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over
  `backends/drools/src/main/java` raakt alleen nog de DRL-teksttak, of niets.
- (d) Gate 12 groen met `EXPECTED_PLAYED` ten minste 240 en dezelfde namen;
  `EXPECTED_DRL_PLAYED` gelijk; alle gates van `CLAUDE.md` groen.
- (e) De meting van punt 7 en de kaarttekst van punt 8 in het `Result:`.
- (f) De schaalproef van punt 9 als tabel in het `Result:`, met de vloer
  gehaald; punt 10 en 11 met een getuige, rood eerst; het contract van punt 12
  in de README en in de kaarttekst van punt 8.

## Raakvlakken

- `20261008-deling-door-leeg-onder-initialisatie`: landt die kaart eerder, dan
  verdwijnt haar deel in de exec-modeltak hier; de nieuwe verlaging gaf daar al
  het goede antwoord.
- `20261008-fout-op-tussenstand`: de Drools-takken van haar (c) zijn na deze
  kaart en de DRL-kaart de takken met truth maintenance.
- `20260918-drools-veelopveel-beanmodel` raakt alle takken. Na deze kaart en de
  DRL-kaart draagt hij er twee in plaats van drie.
- `20260923-drools-rolpaar-ontdubbeling`: de kopie in `ExecModelTranspiler`
  verdwijnt hier.
- Daarna: `20261008-drl-teksttak-op-truth-maintenance`.

## Review

Zoals in de eerste bouwkaart: een lens na elke bouwstap, een eindlens over de
hele diff, en een lensvondst alleen met een concreet faalscenario op echte
invoer of een getuige.

## Omvang

Middel. Het werk zit in het opruimen, de gates en de README; de verlaging zelf
is er dan al.

May touch: `backends/drools/**` behalve de DRL-teksttak,
`scripts/check-drools.sh` en `scripts/check-reftype-derivation.sh`
(**voorstel**), kaartverplaatsing. NB: serieel met andere Drools-kaarten.

Out of scope: de DRL-teksttak (`DrlTranspiler`, `Stratificatie`), de toolchain
zelf (eigen kaart, punt 8), `spec/`, `corpus/`.

## Result (2026-10-11)

*Branch `card/tms-omschakeling`, claim `331b1179` op main `a1c21727` (2026-10-10 09:52), tip `2037905a` (68 commits na
de claim; de tip zoals `git log` hem op 2026-10-11 om 00:07 gaf). De laatste code staat op `1df4affc`; de twee commits
daarna veranderen alleen de README, javadoc en testjavadoc. Gemeten op drie manieren:*
- *`stapklaar.sh` (`/tmp/lens-tms-k4/stapklaar.sh`): per stapcommit een bevroren boom zonder `toka/`, een shaded jar uit
  die boom, en elke golden met cases (294) door de Drools-takken met die jar. Vóór de omschakeling waren dat drie takken:
  de oude exec-model-tak (geen vlag), de DRL-teksttak (`--drl`) en de tak met truth maintenance (`--tms`), elk byte voor
  byte vergeleken met de nulmeting. Daarna twee: de exec-model-tak (geen vlag, nu met truth maintenance) per golden
  tegen `baseline/tms.tsv`, en de DRL-teksttak tegen `baseline/drl.tsv`. Elke doorloop speelt ook de acht
  scenariobestanden van v2 herleving-ww (zie "Het dekkingsverlies").*
- *De nulmeting (`/tmp/lens-tms-k4/baseline/{exec,drl,tms}.tsv`), gemeten op `e1d0d1df`, dezelfde code als main
  `a1c21727` op commentaar en tekst na, en om 09:59 op de claim herhaald: alle drie byte-gelijk.*
- *`gates-kopie.sh`: de volledige gateset van `CLAUDE.md` op een kopie van de stapcommit als eigen git-repo, zodat een
  bouwer intussen in de worktree kon schrijven.*

### Wat de kaart opleverde

De tak met truth maintenance is de exec-model-tak. `Branch.EXEC_MODEL` bouwt via `transpiler/tms/TmsTranspiler`, en
`Branch` heeft nog twee waarden, `EXEC_MODEL` en `DRL_TEXT`. De oude exec-model-tak (`transpiler/model/`, met
`ExecModelTranspiler` van 9950 regels op de claim), `Branch.TRUTH_MAINTENANCE` en de vlag `--tms` zijn weg, zonder
alias.
- **Voortgang tegen de 242.** De exec-model-tak speelt alle 242 goldens die de oude exec-model-tak speelde, met dezelfde
  statusregel, en zes meer: 248 van de 294 goldens met cases, zonder FAIL of ERROR. De zes zijn
  `anchor/jutterswet-erflijn`, `anchor/jutterswet-vindersloon`, `jutterswet-afdrachtstapel`, `jutterswet-klipwacht`,
  `jutterswet-vondsttarief` en `synth2-afrondingscombinaties`. Zo stond het in de nulmeting en na elke stap.
- **Gate 12.** `EXPECTED_PLAYED` gaat van 242 naar 248 (voorstel, `8076d43f`). `EXPECTED_DRL_PLAYED` blijft 196.
- **De ingang voor een afnemer.** `DroolsBackend.voegIn`, `vuur`, `lees` en `zetAfgeleideWaarden`, met een vuurplafond
  `max(10 000, 4 × R × N0)` dat bij elke creatiefase opnieuw wordt uitgerekend. Het contract staat in de README
  ("The entry for consumers") en in de kaarttekst voor de toolchain hieronder.
- **De schaalproef haalt de vloer**, na indexen op de joins: op de tip helling 1,05 en vuringen/plafond 0,016 bij
  N = 10 000. De oude exec-model-tak had op hetzelfde model helling 2,34.
- **Gate 8.** Het Drools-been speelt sinds de omschakeling de exec-model-tak met truth maintenance. Onder mandaat van de
  eigenaar zijn de `drools`-fragmenten, de uitsluitingslijst en de notities gevolgd: 25 gepind, 24 met reden (was 20 en
  29).
- **DRL op truth maintenance is nog niet begonnen.** Dat doet de volgende kaart,
  `20261008-drl-teksttak-op-truth-maintenance`. De DRL-teksttak bleef na elke stap byte-gelijk aan de nulmeting.

### Commits

| Commit | Inhoud |
|---|---|
| `07ab5f5c` | stap 1: het schaalharnas met het model van kaartpunt 9 (`Schaalproef`, `schaal.rgs` via de frontend) |
| `a94b54b1`, `0f4b5f67`, `9325b092` | stap 1: indexen op de identiteitsjoins op een feitveld, de beanveldjoins van een bereik en de rolvuller, en de einden van een `Koppeling` |
| `5ec13d21` | stap 1: waarom de alfaconstraints geen index dragen (javadoc) |
| `1d90f342`, `095a586a` | stap 1: getuige dat de beans `equals` en `hashCode` niet overschrijven; de indexgetuige noemt wat elke toets rood vond |
| `6296b851` | stap 2: een variabel startpunt weigert alleen de vormen op zijn raster (gate 11) |
| `839d28c1`, `423f33c8`, `13ca1615` | stap 2: met naam geweigerd: een vouw over een setbron zonder getallen, een kenmerkinitialisatie zonder waarheidswaarde, een instantie-unie over twee populaties |
| `bc67e553` | stap 2: getuige `TmsZoalsRefWitnessTest`, de waarden van ref waar de destructieve tak afweek |
| `932229dc` | stap 2 (hoofdsessie): twee getuigen volgen de besluiten bij het startpunt en clingo (javadoc) |
| `18f725e1`, `d6dd1b9f` | stap 3: de afnemer vult, vuurt en leest een sessie via de ingang; README, het contract |
| `1af45fdd`, `f4b83d7b` | stap 3: de invoergetuige noemt haar meting in ref en clingo; `voegIn` zoekt het objecttype één keer per beanklasse op |
| `f1287f25` | fixronde stap 3: `lees` en terugzetten alleen na een `vuur` die een fixpunt bereikte |
| `862cb632` | fixronde stap 3: `vuur` weigert een tweede fire op een model met creatie |
| `94cea187` | fixronde stap 3: `voegIn` weigert een gerefereerd object buiten de invoer |
| `ad08da29` | fixronde stap 3: de plafondgetuige onderscheidt het product en elke fase |
| `520e76a4`, `32a649c4` | fixronde stap 3: `lees` met labels wordt publiek; README |
| `3e1cbf62`, `26ce91c8` | stap 4: `TmsMeting` meet oud tegen nieuw over het corpus, en de factor die een case per fase vraagt |
| `0fd7c487`, `a46828c5` | stap 4: het schaalmodel zonder creatieketen; `Schaalproef` onder het plafond, en de tweede fire |
| `637fb1de` | stap 4: de meetnaad `CaseRunner.meet` verdwijnt |
| `928e39d4` | stap 4: FACTOR blijft 4, nu op de meting |
| `2d95c456` | tweede fixronde stap 3: `lees` en terugzetten weigeren wat na `vuur` buiten `vuur` om gebeurde (`Bewaking`) |
| `fb80e8df` | tweede fixronde stap 3: `vuur` weigert een relatie en invoer buiten `voegIn` om |
| `81ac1d6c` | tweede fixronde stap 3: README, de bewaking tussen twee fires |
| `f0b7bbf9` | stap 5a1: buiten `vuur` om alleen een update van een attribuut of kenmerk (relaties, globals, agenda) |
| `3fa9ef63` | stap 5a1: `vuur` en de lezing zoeken hun merkfeiten per klasse |
| `35eb7e6c`, `af19720e`, `77434f56`, `7876b4f2`, `45a3399a` | stap 5a1: de 135 getuigen van de indeling van stap 2 op de nieuwe tak (A, B, C, D, E); vier rijen los van de oude exec-model-tak |
| `92f28966` | stap 5a2: de exec-model-tak bouwt met truth maintenance |
| `4e036240` | stap 5a2: geen vergelijking van de tak met zichzelf |
| `3d50652a` | stap 5a2: de destructieve verlaging weg |
| `b4d45490` | stap 5a2: weigerzinnen zonder de oude exec-model-tak |
| `8076d43f`, `5d1c3acb` | stap 5a2: gate 12 telt de exec-model-tak met truth maintenance; gate 13 noemt de verwijderde verlaging als geschiedenis (voorstel) |
| `ab80752d`, `fad6a24c` | stap 5b1: de ingang zegt bean en beans; de tak noemt zich "deze tak" en `exec-model` |
| `55d178d8` | stap 5b1: de bewaking vergelijkt relaties en kent haar acties |
| `6a773e9a`, `b2637a10`, `1dce6f23` | stap 5b1: drie getuigen pinnen weer wat ze beweren; een getuige voor twee herschreven weigerzinnen; de getuige van twee parameters speelt weer drie regels |
| `e9ed38d8` | stap 5b1: de schrijversluisteraar hangt alleen aan de DRL-teksttak |
| `9906feb7` | stap 5b1: gate 8 pint de zinnen van de exec-model-tak (voorstel, mandaat van de eigenaar) |
| `07a7c4a1` | stap 5b2: het scenariorapport telt per vorm |
| `121c35e5` | stap 5b2: bean in testtekst en `exec-model` in boodschappen |
| `be772617`, `8b4006d2` | stap 5b2: `Schaalproef` faalt als de vloer niet gehaald wordt; `Defaultsgroep` zonder eigenaars |
| `4949b82f` | stap 5b2: de corpusgetuige zegt waarom zij blijft |
| `e99b3091`, `3e5fda63` | stap 5b2: de javadoc en de testjavadoc kennen twee takken |
| `ba0e8264` | stap 5b2: de README beschrijft de exec-model-tak |
| `b79a6ef6` | stap 5b3 (hoofdsessie): de rode notities zeggen wat de exec-model-tak doet (mandaat van de eigenaar) |
| `732009b7` | stap 5b3 (hoofdsessie): de javadoc beschrijft de oude exec-model-tak als geschiedenis |
| `538a2fbd`, `cad4b55b` | fixronde 5b4 (hoofdsessie): het scenariorapport houdt een vormnaam heel; de corpusgetuige zegt wat hij doet |
| `1df4affc` | fixronde 5b5 (hoofdsessie): de dekking van het rapport geldt per versie |
| `cd356720` | eindlens (hoofdsessie): het contract zegt wat de ingang niet ziet (README, javadoc) |
| `2037905a` | eindlens (hoofdsessie): drie getuigen noemen `ExecModelTranspiler` als verleden (testjavadoc) |

### Acceptatie

**(a) De doorloop van punt 1.** Op de claim speelt de tak met truth maintenance elke golden die de oude exec-model-tak
speelt, met dezelfde statusregel en zonder FAIL. Dat bleef zo na elke stap:

| Stap | Commit | Klaar | Oude exec-model-tak | DRL-teksttak | Exec-model-tak met truth maintenance | Van de 242 | Tegen de nulmeting | v2 herleving-ww |
|---|---|---|---|---|---|---:|---|---|
| claim | `331b1179` | 09:59 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | apart gemeten, zie "Het dekkingsverlies" |
| 1 | `095a586a` | 14:12 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | oud 8× EXCLUDE, nieuw 8× PLAYED |
| 2 | `932229dc` | 14:33 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | nieuw 8× PLAYED |
| 3 | `f4b83d7b` | 15:22 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | nieuw 8× PLAYED |
| fixronde 3 | `32a649c4` | 16:03 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | 8× PLAYED |
| 4 | `928e39d4` | 16:34 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | 8× PLAYED |
| tweede fixronde 3 | `81ac1d6c` | 17:01 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | 8× PLAYED |
| 5a1 | `45a3399a` | 17:41 | 242 / 52 | 196 / 96 / 2 FAIL | 248 / 46 | 242 | drie takken gelijk | 8× PLAYED |
| 5a2 | `5d1c3acb` | 21:13 | weg | 196 / 96 / 2 FAIL | 248 / 46 | 242 | DRL gelijk; exec 0 kanaalwissels, 6 weigerregels anders | 8× PLAYED |
| 5b1 | `9906feb7` | 22:07 | weg | idem | 248 / 46 | 242 | idem | 8× PLAYED |
| 5b2, 5b3 | `732009b7` | 23:22 | weg | idem | 248 / 46 | 242 | idem | 8× PLAYED |
| 5b4 | `cad4b55b` | 23:35 | weg | idem | 248 / 46 | 242 | idem | 8× PLAYED |
| 5b5 | `1df4affc` | 23:47 | weg | idem | 248 / 46 | 242 | idem | 8× PLAYED |

Kolommen: PLAYED / EXCLUDE (en FAIL waar die er is); geen ERROR op enig moment. "Van de 242" telt de goldens die de oude
exec-model-tak in de nulmeting speelt en de exec-model-tak met truth maintenance op die commit speelt. De zes regels
die na de omschakeling anders zijn, zijn alle zes EXCLUDE gebleven; alleen hun weigerzin veranderde:
`anchor/jutterswet-bootberging`, `anchor/jutterswet-gemeenteberging`, `jutterswet-havenrooster` en `jutterswet-muntketen`
het voorvoegsel (`tms-transpiler SKIP:` werd `transpiler SKIP:`), `anchor/jutterswet-penninguitgifte` en
`jutterswet-pachtvenster` ook de zin die de oude tak noemde (5a2), en havenrooster en pachtvenster daarna "deze tak"
(5b1). Bewijs: `/tmp/lens-tms-k4/<stap>/corpus/overzicht.txt` per stap. Na `1df4affc` veranderde alleen tekst
(`cd356720`, `2037905a`).

**(b) De getuige van punt 4, rood eerst.** `TmsHerhaaldVurenWitnessTest` (`18f725e1`; 3 rijen bij stap 3, 16 op de
tip). Volgens besluit 1 wijkt de vorm af van de letter van punt 4: tussen twee fires dragen de beans alleen invoer, dus
er hoeft geen veld leeg te worden gemaakt. De getuige vuurt twee keer in één sessie, met tussendoor een `update` van de
invoer. Daarbij vervallen een kenmerk, een voorwaardelijke waarde en een default, de som volgt de nieuwe invoer, en de
initialisatie vuurt opnieuw (vindloon 15 wordt 8). De tweede lezing is gelijk aan die van een verse sessie, een fout
vervalt (noemer 0 wordt 4, aandeel 25) en een fout ontstaat (noemer 5 wordt 0). Na `zetAfgeleideWaarden` weigert `vuur`.
- Rood: de oude vorm (vuren, terugzetten, de beans lezen) op `095a586a` faalt twee keer: "second reading ==> expected:
  <[J1.totaalgewicht = 7, V1.uitkering = 13, V1.vindloon = 8, V2.uitkering = 14, V2.vindloon = 9]> but was:
  <[J1.totaalgewicht = 7, V1 is zwaar, V1.bonus = 7, V1.heffing = 40, V1.uitkering = 13, V1.vindloon = 15,
  V2.uitkering = 14, V2.vindloon = 9]>" en "Expected java.lang.IllegalStateException to be thrown, but nothing was
  thrown." (`stap3/rood/TmsHerhaaldVuren-oude-vorm-op-095a586a.txt`).
- Ref en clingo: verifier PASS op beide documenten; het waardendocument PASS in ref en clingo, 12 verwachtingen per
  ronde; het foutdocument "division by numeric zero" in ref en `viol("division by numeric zero","V")` in clingo bij
  noemer 0, aandeel 20 en 25 in beide bij noemer 5 en 4 (`stap3/docs/metingen.txt`).
- Latere rijen, elk rood eerst: een model met creatie weigert een tweede `vuur` (`862cb632`; de oude vorm gaf
  J1.heffing 70 waar een verse sessie 120 geeft); relatie, global, kale fire en agenda buiten `vuur` om (`fb80e8df`,
  `f0b7bbf9`, `55d178d8`). Zie "Wat weggaat" en "Lensvondsten".

**(c) De grep.** `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen' backends/drools/src/main/java`, zoals
het verslag van 5b2 hem op `ba0e8264` gaf (`stap5b/acceptatie-c.txt`):

```
   1 backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/DrlTranspiler.java
# regels buiten DrlTranspiler/Stratificatie:
       0
```

De ene treffer is `DrlTranspiler.java:1965`, een commentaarregel ("rules over one subject ping-pong forever (NO_LOOP
blocks only the self-retrigger)"), gemeten in 5a2 en 5b1. Eigen controle op `1df4affc`: dezelfde ene regel. De grep
raakt dus alleen nog de DRL-teksttak.

**(d) Gate 12 en de volledige gateset.** Gate 12 op een kopie van de tip `1df4affc` (klaar om 23:58,
`gates-stap5b5.RVAh/uit/12-check-drools.log`):

```
check-drools: PASS
  played 248  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
  auto-excluded 46  (per-form refusals: loader / transpiler marker / contract / unsupported input)
  value-divergence 0  (listed with reason — the exec-model differs from ref/clingo)
  no-case 3  (goldens without a case file)
  channel floor 49 non-PLAYED goldens pinned by name; played matches the pinned 248
  DRL-text branch played 196  (its OWN count — the narrower branch, not the square; pin 196)
  DRL-text floor 101 non-PLAYED goldens pinned by name; sweep cost 142s wall clock
  JUnit 1441 tests, floor 239  (the backend's own suite, green with a non-zero count)
```

`EXPECTED_PLAYED` 248 is ten minste 240 en speelt dezelfde namen (de 242 plus zes); `EXPECTED_DRL_PLAYED` is gelijk
(196). Alle 16 gates van `CLAUDE.md` zijn groen op een kopie van `095a586a`, `932229dc`, `f4b83d7b`, `928e39d4`,
`45a3399a`, `9906feb7`, `732009b7` en `1df4affc` (`gates-stap*/uit/overzicht.txt`). Op `5d1c3acb` (5a2) draaide de
bouwer gate 1, 11, 12, 13 en 15 groen en gate 8 rood (zie "Gate 8, 12 en 13"); 5b1 maakte gate 8 groen. De gaterun op
`cad4b55b` heeft een overzicht tot en met gate 9, alle groen (23:40). Suite op `1df4affc`: 1441 tests, 0 failures,
0 errors, 0 skipped. De laatste gaterun, op de tip `2037905a` (2026-10-11, klaar 00:23,
`gates-stap-tip.ge1Z/uit/overzicht.txt`): alle 16 groen; gate 12 played 248, DRL-tekst 196, JUnit 1441.

**(e) De meting van punt 7** (`TmsMeting`, stap 4, `928e39d4`, gemeten 16:19:19–16:19:30, `stap4/tabel-kaartpunt7.md`).
De 242 goldens die beide exec-model-takken speelden, 661 cases:

| grootheid | som oud | som nieuw | nieuw/oud | mediaan oud | mediaan nieuw | mediaan van nieuw/oud per golden |
|---|---:|---:|---:|---:|---:|---:|
| vuringen | 6 164 | 8 933 | 1,45 | 8 | 12 | 1,02 |
| vuren (ms) | 8,039 | 19,774 | 2,46 | 0,010 | 0,017 | 1,71 |
| terugzetten (ms) | 0,000 | 4,802 | – | 0,000 | 0,007 | – |
| vuren+terugzetten (ms) | 8,048 | 24,579 | 3,05 | 0,010 | 0,024 | 2,61 |
| bouw (ms) | 65,852 | 76,295 | 1,16 | 0,232 | 0,239 | 1,08 |
| R | 789 | 1 779 | 2,25 | 3 | 5 | 2,00 |
| N0 | 1 591 | 1 657 | 1,04 | 5 | 5 | 1,00 |

Op de oude tak was het terugzetten een no-op. N0 is op de nieuwe tak iets hoger, omdat `voegIn` ook de `Emmerinvoer`-
en `Lijstplaats`-feiten invoegt. De zes goldens die alleen de nieuwe tak speelt (13 cases): 89 vuringen, vuren 0,122 ms,
terugzetten 0,044 ms, bouw 2,193 ms. In absolute tijd is de mediaan per golden 24 µs tegen 10 µs (gevolgtrekking van de
bouwer van stap 4). Op het schaalmodel keert de verhouding om (f). De kaarttekst van punt 8 staat hieronder in "De
kaarttekst voor de toolchain".

**(f) De schaalproef van punt 9, punt 10 en 11, en het contract van punt 12.**

Het model (`backends/drools/src/test/resources/schaal/schaal.rgs`, door de frontend geëmitteerd, verifier PASS): één
Kustwacht, N Wachters met elk tien Vondsten, en per onderdeel (a)–(g) van punt 9 één regel. De controle met acht
Wachters (178 verwachtingen) geeft in ref en clingo PASS en in beide exec-model-takken "gelijk aan de verwachting"
(`stap1/controle/`). De eindtabel op de tip: de exec-model-tak via de publieke `vuur`, FACTOR 4, gemeten op `1df4affc`
(2026-10-11 00:04:23–00:05:21, `/tmp/lens-tms-k4/schaal-tip/tabel-kaartpunt9-eind.md`; elk punt een eigen JVM met
`-Xmx16g`, één opwarmer en de mediaan van drie; de commits na `1df4affc` veranderen alleen tekst):

| N | belasting | vuringen | vuren (ms) | terugzetten (ms) | som (ms) | piekfeiten | heap (MB) | bouw (ms) | R | N0 | v/(R×N0) | plafond | vuringen/plafond |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 100 | 5,10 5,48 6,50 | 14 878 | 32,3 | 10,7 | 44,4 | 9 845 | 19 | 10,2 | 24 | 1 101 | 0,563 | 945 024 | 0,016 |
| 1 000 | 4,85 5,42 6,47 | 148 728 | 209,1 | 75,6 | 284,7 | 98 395 | 126 | 13,0 | 24 | 11 001 | 0,563 | 9 445 824 | 0,016 |
| 10 000 | 4,85 5,42 6,47 | 1 487 228 | 2 523,1 | 729,3 | 3 209,3 | 983 895 | 1 235 | 13,0 | 24 | 110 001 | 0,563 | 94 453 824 | 0,016 |

- **De vloer is gehaald:** de helling (vuren plus terugzetten, log-log, N = 1 000 → 10 000) is 1,05, ten hoogste 1,2;
  vuringen/plafond is 0,016 bij N = 10 000, onder de helft. Het kusttotaal en de inconsistenties zijn gelijk aan die
  van stap 1 (28067.00 / 23, 280817.00 / 223, 2808317.00 / 2223). `Schaalproef` eindigt sinds `be772617` met exitcode
  2 als een tak de vloer mist (rood eerst met een mutant: de oude code gaf exit 0); hier exit 0.
- **Ter vergelijking, gedateerd.** Op `928e39d4` (stap 4, vóór de bewaking van 5a1b en vóór de omschakeling; de tak
  toen achter `--tms`): 42,5 / 277,5 / 3 063,0 ms, helling 1,04, vuringen/plafond 0,016. In 5b2, na de omschakeling
  (`stap5b/schaal-nieuw.md`, 22:25): 3 213,8 ms bij N = 10 000, helling 1,05.
- **De oude exec-model-tak** (stap 1, zonder plafond gemeten, 13:36–13:51): 30,3 / 987,5 / 213 613,8 ms, helling 2,34.
  Tegen de meting op `928e39d4` was nieuw/oud voor vuren plus terugzetten 1,40 bij N = 100, 0,28 bij N = 1 000 en
  0,014 bij N = 10 000. Via de publieke `vuur` hield de oude tak zijn vaste plafond van 100 000 en haalde hij bij
  N = 10 000 (682 224 vuringen) geen fixpunt.
- **Vóór de indexen** had de nieuwe tak helling 1,85 tussen N = 100 en 1 000 (155,0 → 10 953,2 ms); N = 10 000 is
  zonder index niet gemeten (de run stopte na 3 uur 18 minuten, zie "Lensvondsten", stap 1).

De tweede fire tegen een verse sessie, op het model zonder (g), op de tip (`1df4affc`, R = 17,
`/tmp/lens-tms-k4/schaal-tip/tabel-kaartpunt9-tweede-fire.md`):

| N | belasting | N0 | vuringen eerste fire | tweede fire | verse sessie | plafond tweede fire | eerste fire (ms) | tweede fire (ms) | verse sessie opzetten (ms) | verse sessie vuren (ms) | tweede/vers (vuren) | regels in de lezing | gewijzigd |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 100 | 5,26 5,48 6,47 | 1 101 | 13 278 | 16 | 13 288 | 540 124 | 22,5 | 1,1 | 16,1 | 19,5 | 0,056 | 2 225 | 11 |
| 1 000 | 5,64 5,55 6,49 | 11 001 | 132 728 | 16 | 132 738 | 5 398 724 | 160,2 | 3,5 | 90,1 | 153,2 | 0,023 | 22 225 | 11 |
| 10 000 | 5,64 5,55 6,49 | 110 001 | 1 327 228 | 16 | 1 327 238 | 53 984 724 | 1 971,1 | 16,4 | 1 047,0 | 1 864,2 | 0,009 | 222 225 | 11 |

"Tweede fire" is de wijziging (`set` en `update`) plus `vuur`. De lezing na de tweede fire is bij elke N gelijk aan
die van de verse sessie; anders faalt het punt luid (rood met een mutant zonder `sessie.update`, stap 4: "de lezing na
de tweede fire (2224 regels) verschilt van die van een verse sessie …"). De wijziging raakt (a)–(f): de waarde van
V1_1 gaat van 46 naar 80, de wachtsom van W1 van 270 naar 349, en W1 wordt inconsistent. Op `928e39d4` kostte de
tweede fire bij N = 10 000 nog 68,2 ms; zie de open punten.

- **Punt 10, getuige `TmsVuurplafondWitnessTest`** (4 rijen; `18f725e1`, `ad08da29`). Het publieke plafond is het
  product waar dat boven de vloer ligt: 24 040 voor J1 met 600 Vondsten (R = 10, N0 = 601). Een latere creatiefase met
  meer feiten zet het plafond: op `jutterswet-dubbelverbinding` N0 = 7, de eerste fase 28, de vierde 40, plafond
  4 × 19 × 40 = 3 040. Rood: de getuige compileert niet op `095a586a`; drie mutanten falen (alleen de vloer: "expected:
  <24040> but was: <10000>"; alleen de eerste fase: "expected: <3040> but was: <2128>"; geen herberekening: "but was:
  <532>", `stap3-fix/rood-3-mutant-{A,B,C}.txt`).
- **Punt 11, getuige `TmsHerhaaldVurenWitnessTest`** (zie (b)): de initialisatie vuurt na de invoerwijziging opnieuw
  (vindloon 15 wordt 8). In de oude vorm las de tweede fire de teruggezette 15 als invoer en bleef zij staan (de rode
  uitvoer bij (b)).
- **Punt 12, het contract**, in `backends/drools/README.md`, "The entry for consumers" (`d6dd1b9f`, `32a649c4`,
  `81ac1d6c`, bijgewerkt in 5a1, 5b1 en `ba0e8264`), en in de kaarttekst hieronder. Getuigen: `TmsAfnemerWitnessTest`
  (12 rijen), `TmsInvoerWitnessTest` (7), `TmsHerhaaldVurenWitnessTest` (16), `TmsVuurplafondWitnessTest` (4).

### De kaarttekst voor de toolchain

Voor de repository `regelspraak-toolchain`. Regelnummers op haar `f9fc44c` (submodule op uchar `0a378588`), alleen
gelezen; de grep sloot alleen de submodule (`./uchar/`) uit.

> **20261010-sessie-via-de-ingang-van-uchar: de pin naar de omschakeling van uchar, en elke sessie vult, vuurt en leest
> via `DroolsBackend`**
>
> *Vastgelegd bij de landing van `20261008-tms-omschakeling` in uchar, uit het `Result:` van die kaart. Deze kaart landt
> na die kaart; de pin mag niet bewegen zonder haar.*
>
> **Aanleiding.** Sinds `20261008-tms-omschakeling` leidt `DroolsBackend.Branch.EXEC_MODEL` af met truth maintenance:
> een afgeleide waarde is een logisch feit in de sessie, en de beans dragen alleen invoer tot
> `DroolsBackend.zetAfgeleideWaarden`. De distributie vuurt vandaag zelf, één keer per verse sessie, en leest daarna de
> beans en de `Inconsistentie`-feiten. Zo doet `Uitvoerder.vuur` het
> (`service/src/main/java/dev/uchar/regelspraak/toolchain/service/Uitvoerder.java` r. 62–74):
>
> ```java
> KieSession sessie = kieBase.newKieSession();
> // … globals: dagsoorten, defaults, parametersets, verzoek, rekendatum
> for (Object bean : wereld.beans().values()) sessie.insert(bean);
> int n = sessie.fireAllRules(100_000);
> if (n >= 100_000) throw new IllegalStateException("regel-herberekening bereikt geen fixpunt …");
> List<Object> inc = leesInconsistenties(sessie);
> return new Uitkomst(wereld.beans(), n, inc);
> ```
>
> Na een pinbump zonder deze kaart geeft elk antwoord van de dienst, van `RegelSpraakEngine` en van de scenario-uitleg
> de invoer terug zonder de afgeleide waarden, en compileert `RegelSpraakEngine` niet meer.
>
> **Wat er gebeurt.**
> 1. **De pin** naar de landing van `20261008-tms-omschakeling` in uchar.
> 2. **`Uitvoerder.vuur`** krijgt de `Build` in plaats van KieBase, beanLoader en dagsoortglobals; daarvoor draagt
>    `BeslisserKern.Entrypoint` (r. 97, gebouwd op r. 225 en r. 301) de `Build`. De globals blijven zoals nu en gaan
>    vóór de eerste `vuur` op de sessie. Daarna:
>
>    ```java
>    DroolsBackend.voegIn(build, sessie, new Invoer().beans(wereld.beans().values()));
>    DroolsBackend.Vuring v = DroolsBackend.vuur(build, sessie);
>    if (v.geenFixpunt() != null) throw new IllegalStateException(v.geenFixpunt());
>    Map<Object, String> label = new IdentityHashMap<>();
>    wereld.beans().forEach((l, bean) -> label.put(bean, l));
>    DroolsBackend.zetAfgeleideWaarden(build, sessie, label::get);  // werpt een staande fout, zie 4
>    List<Object> inc = leesInconsistenties(sessie);                // ongewijzigd, of DroolsBackend.lees(…).inconsistenties()
>    return new Uitkomst(wereld.beans(), v.vuringen(), inc);
>    ```
>
>    De labelfunctie geeft het label van een invoerbean en null voor een gemaakt object; een fout noemt haar object dan
>    zoals ref ("V, een Vondst"). Een gemaakt object is na het terugzetten via de referentievelden te bereiken.
> 3. **`CaseSessie.Speler.speel`** (`toolchain/.../engine/CaseSessie.java` r. 84–103) gaat op dezelfde manier: `voegIn`,
>    `vuur`, `zetAfgeleideWaarden(build, sessie, label)` met de labels van `labelNaarBean`. De volgorde van een lijst
>    naar veel is standaard die van de beans; wil de uitleg de volgorde van uchars replay (eerst de geschreven leden,
>    dan de gespiegelde op label), dan geeft de speler `Invoer.lijst(houder, rol van de houder, leden)`. Die volgorde
>    bepaalt alleen welk lid een fout noemt. De javadoc van `CaseSessie` zegt dat de invoerstap van de replay geen
>    publieke API is; dat is hij nu wel (`voegIn`, `Invoer`).
> 4. **Fouten.** Een evaluatiefout komt niet meer uit `fireAllRules` maar uit `zetAfgeleideWaarden`, dat niets schrijft
>    zolang er een fout staat: één fout als haar eigen exceptie, `Breuk.DelingDoorNul` (een `ArithmeticException`),
>    `Rekenfout` (een `RuntimeException`), `Schrijversconflict` (een `IllegalStateException`) of `Invoervormfout` (een
>    `RuntimeException`); twee of meer als `Evaluatiefouten`, met `fouten()`. `lees(…).fouten()` geeft ze als gegevens.
>    `AntwoordBouwer` en de foutafhandeling van de dienst beelden deze typen af.
> 5. **De naam van een vuring** (besluit 21 van uchar). `CaseSessie` telt een vuring onder
>    `ProducentConsument.basisNaam(regelnaam)` (r. 184), en `ScenarioRunner` r. 166 vergelijkt dat spoor met dat van
>    uchars `CaseRunner.rapport`; bij een verschil houdt de uitleg haar antwoord in. Uchar noemt een vuring sinds de
>    omschakeling naar `TmsTranspiler.vormVan(regelnaam)`, met alleen het versie-achtervoegsel eraf
>    (`RegelSpraakModel.vormnaam`), en geeft een regel zonder vorm (de standen van de verlaging) geen rij. `CaseSessie`
>    volgt dat, of de uitleg wijkt op elk document af. De dekking van het rapport vergelijkt sinds `1df4affc` op de
>    exacte vormnaam, dus per versie. `Playground` (r. 222) toont het spoor van uchar al met `naamgeving.vorm(…)` en
>    verwacht vormnamen.
> 6. **`RegelSpraakEngine.Session`** (`toolchain/.../engine/RegelSpraakEngine.java` r. 155–206) krijgt de `Build`:
>    - `insert` en `insertAll` (r. 164–165) verzamelen de beans tot de eerste `fire()`; die roept `voegIn` en `vuur`
>      aan, een volgende `fire()` alleen `vuur`. `voegIn` vult alleen een verse sessie.
>    - Een nieuwe `update(bean)` (`session.update(session.getFactHandle(bean), bean)`) laat een afnemer tussen twee
>      fires een attribuut of kenmerk van een bean uit `voegIn` veranderen. De `Session` roept `update` aan na elke
>      wijziging die zij doorgeeft: een attribuut of kenmerk dat zonder `update` verandert, ziet de ingang niet, en
>      `lees` geeft dan het oude fixpunt (zie het contract, punt 5). Een nieuw, verwijderd of anders verbonden object,
>      ook met `update`, een andere global, een kale `fireAllRules` en elke bewerking van de agenda vragen een verse
>      sessie; `vuur`, `lees` en `zetAfgeleideWaarden` weigeren ze met naam.
>    - Een nieuwe `lees()` geeft het `Fixpunt` van de laatste `fire()`; een nieuwe `zetTerug()` roept
>      `zetAfgeleideWaarden` aan, waarna `fire()` de sessie weigert.
>    - `fire()` geeft `Vuring.vuringen()` en werpt bij `Vuring.geenFixpunt()`.
>    - `facts(…)` (r. 189–202) leest het werkgeheugen: afgeleide waarden staan daar als logische feiten, niet op de
>      beans; de `Inconsistentie`-feiten blijven daar te lezen.
>    - `setGlobal` (r. 166) hoort vóór de eerste `fire()`: een global staat vast vanaf de eerste `vuur`, en `vuur`,
>      `lees` en `zetAfgeleideWaarden` weigeren een sessie waarvan een global daarna veranderde.
> 7. **`RegelSpraakEngine.overgeslagen()`** (r. 148) leest `build.execModelTranspiler().overgeslagen()`; het type is nu
>    `TmsTranspiler`, dat geen `overgeslagen()` heeft, dus dit compileert niet meer. De methode valt weg, of geeft de
>    niet-verlaagde regels van `build.dekking()` terug, die altijd leeg zijn omdat de build elke andere regel weigert.
>    Aanroepers: `RegelSpraakEngineTest` r. 34 en `PeildatumSessieTest` r. 55 en 113–114.
> 8. **De drie literalen `fireAllRules(100_000)`** (`Uitvoerder` r. 69, `CaseSessie` r. 103, `RegelSpraakEngine`
>    r. 178) en hun zinnen over een kringverwijzing verdwijnen. Het plafond is `max(10 000, 4 × R × N0)`, met R de
>    regels van de KieBase en N0 de feiten vlak vóór de fire, bij elke creatiefase opnieuw uitgerekend; zonder `vuur`
>    vuren de creaties niet. Een kring die WF-045/046 verbiedt, weigert uchar bij de bouw, met
>    "exec-model: kring in de afhankelijkheden …"; `UcharBouw.nietGelowerd` herkent die zin niet als construct, dus
>    `NietGedragen` heeft dan geen construct.
> 9. **Tests die zelf vuren en de beans lezen**: `NegatieStratificatieTest` r. 99–100 (`fireAllRules(10_000)`),
>    `ScenarioOrakelTest` r. 87–92, `PeildatumSessieTest` r. 60, 66, 119, 134 en 143, `VerbalisatorTest` r. 39. Zij
>    gaan via de ingang, of via de nieuwe `Session`.
> 10. **Weigerzinnen.** Het voorvoegsel van een weigering per vorm is weer `transpiler SKIP:` en past op het patroon
>     van `UcharBouw` (r. 40–41). Het commentaar in `VerzoekMapper` r. 582 noemt `ExecModelTranspiler.globalNaam`; de
>     nieuwe tak noemt een parameterglobal met dezelfde formule (`Namen.ident(naam, false)`), dus alleen het commentaar
>     is verouderd.
>
> **Het contract** (uchar `backends/drools/README.md`, "The entry for consumers"):
> 1. Een verse `KieSession` van `build.kieBase()`, met `build.beanLoader()` als context class loader terwijl de beans
>    gemaakt worden en de sessie vuurt; de globals gaan erop vóór de eerste `vuur` en staan daarna vast.
> 2. `DroolsBackend.voegIn(build, sessie, invoer)`, met een `Invoer` die de beans draagt (`bean`, `beans`), en waar
>    nodig `emmerscalair(bean, attribuut)` voor een invoertijdlijn als emmerscalair en `lijst(houder, rol, leden)` voor
>    de volgorde van een lijst. `voegIn` stelt zelf de `Emmerinvoer`- en `Lijstplaats`-feiten, vult alleen een verse
>    sessie, weigert een opgave die niet past vóór het iets invoegt, en weigert een referentieveld naar een object dat
>    niet als bean in de invoer staat.
> 3. `DroolsBackend.vuur(build, sessie)` vuurt tot het fixpunt onder het plafond, een model met creatie per stratum in
>    generaties, en geeft een `Vuring(vuringen, plafond, geenFixpunt)`. Een model met creatie vuurt per sessie één keer.
> 4. `DroolsBackend.lees(build, sessie[, label])` geeft het `Fixpunt` (waarden, kenmerken, gemaakte objecten,
>    koppelingen, fouten, inconsistenties) zonder iets te schrijven, of aan het eind
>    `DroolsBackend.zetAfgeleideWaarden(build, sessie[, label])`, dat het fixpunt eenmalig op de beans zet, met de
>    gemaakte koppelingen op de referentievelden, en de sessie afsluit. Beide lezen alleen een sessie waarvan de laatste
>    `vuur` een fixpunt bereikte en waarin sindsdien niets buiten `vuur` om gebeurde.
> 5. Tussen twee fires verandert een afnemer alleen een attribuut of kenmerk van een bean uit `voegIn`, met `update`.
>    Een bewaking op de sessie (`Bewaking`) weigert al het andere met naam: een relatie die verandert, ook met `update`,
>    een global die verandert, een kale `fireAllRules` en een bewerking van de agenda. Zij leest daarvoor de agenda van
>    Drools (`org.drools.core`), omdat `Agenda.clear` geen gebeurtenis geeft: de distributie raakt de agenda niet aan.
>    **Plicht van de afnemer: na elke wijziging van een attribuut of kenmerk `update` aanroepen.** Een waarde die
>    zonder `update` verandert, ziet de ingang niet: de volgende `vuur` vuurt niets en `lees` geeft het oude fixpunt
>    (eindlens van `20261008-tms-omschakeling`, vondst 1, op het schaalmodel: beloning 1, wachtsom 270 en W1
>    consistent, waar een verse sessie 80, 349 en W1 inconsistent geeft). De README zegt dat sinds `cd356720`, in de
>    lijst "What a consumer can forget".
> 6. Een emmerscalair staat op de bean als het ene segment van de periode van de rekendatum. `voegIn` toetst dat het
>    attribuut op een tijdlijn ligt, niet de vorm van die tijdlijn: een emmerscalair op een lege of langere tijdlijn,
>    zo opgegeven of tussen twee fires zo gezet met `update`, laat de volgende `vuur` stoppen met een
>    `IllegalStateException` in Drools' `ConsequenceException` (README, dezelfde lijst, sinds `cd356720`).
>
> **Gedrag dat verandert** (besluiten 9 en 12 van uchar; de lijsten staan in het `Result:` van
> `20261008-tms-omschakeling`, "Het dekkingsverlies"):
> - Vormen die de oude exec-model-tak droeg en de nieuwe met naam weigert, geen ervan met een golden: een
>   dagsoortpredicaat als voorwaarde; een deling of wortel in een default; een deling of wortel in een
>   ledeninitialisatie van een objectcreatie; een objectcreatie zonder `connectingRole`-binding (de zwaarste post: de
>   frontend laat de binding weg als de zin het type noemt in plaats van een rol; uchar-kaart
>   `20261010-objectcreatie-zonder-connectingrole-binding`); "is gevuurd" in een voorwaarde; een macht met een
>   niet-literale exponent; een som over tijdlijnen als waarde van een tijdlijndoel; een uniciteitssleutel met twee
>   tijdsafhankelijke delen; de producenten op een raster met variabel startpunt. Een regelset met zo'n vorm bouwt niet
>   meer (`UcharBouw.NietGedragen` met de weigerzin).
> - Gedrag dat alleen de oude exec-model-tak had en dat verdwijnt (punten 13 en 18 van die kaart): een versie met een
>   begrensde geldigheid zonder rekendatum, een niet herrekende terugvalwaarde in een andere eenheid, een benaderde
>   irrationale wortel, een deling door leeg die op drie plekken niet deponeerde, "5.50" en "true" in een
>   tekstsamenstelling, de verdeling met een later afgeleide pot, een lege pot, twee versies en de twee punten van
>   `rsVerdeelOpVolgorde`, een rekenende regel naast een verdeling, een tarief zonder gedeclareerde noemer, de
>   asOfDate-emmerscalair als segment, "(on)gelijk aan" in een knip, en een `ClassCastException` bij een
>   gedimensioneerd doel. Waar de oude tak antwoordde en ref faalt, faalt of weigert de nieuwe nu ook.
> - Een leeg startpuntanker: de oude takken gaven via een begeleider "inconsistent", ref, clingo en de nieuwe tak geven
>   consistent.
> - Een factor 1/24: 216,00 sl wordt 9.00000000000000072 tj waar de oude tak 9.000000000000001 gaf (CONSTRUCTS rij 26).
> - Een kring die WF-045/046 verbiedt: geweigerd bij de bouw, waar de oude tak tot het plafond vuurde.
>
> **Meting voor de dienst** (punt 7 en 9 van die kaart): op de 242 goldens kost vuren plus terugzetten per golden in de
> mediaan 2,61 keer zoveel (24 µs tegen 10 µs). Op het schaalmodel met N = 10 000 Wachters (110 001 invoerfeiten) kost
> vuren plus terugzetten op de landing 3 209,3 ms, waar de oude tak 213 613,8 ms nodig had; een tweede fire na één
> wijziging kost daar 16,4 ms (16 vuringen) tegen 1 864,2 ms voor het vuren van een verse sessie.
>
> **Acceptatie.**
> - (a) De pin staat op de landing van `20261008-tms-omschakeling`; `make check-pin` groen.
> - (b) Geen `fireAllRules` meer in `service/src/main` en `toolchain/src/main`; elke sessie gaat via `voegIn`, `vuur`
>   en `zetAfgeleideWaarden` of `lees`.
> - (c) Rood eerst, per vuurplek een getuige die na de pinbump zonder deze kaart faalt en erna een afgeleide waarde
>   leest: een antwoord van de dienst, een `Session` die twee keer vuurt met een `update` ertussen, en een uitleg van de
>   scenario-runner die zonder afwijkend spoor verbaliseert.
> - (d) Een getuige per fouttype van punt 4 in het antwoord van de dienst.
> - (e) `make poorten` groen (de pin, de reactor met zijn vloeren, `check-tweelingen`, `check-dienst-replay` en
>   `scenario-test`), en `make gates` vóór de landing.
>
> **Omvang.** Middel. May touch: de submodule `uchar` (alleen de pin), `service/`, `service-quarkus/` (alleen als de
> adapter de foutafhandeling draagt), `toolchain/`, hun tests, kaartverplaatsing.
>
> **Raakvlakken.** Uchar `20261010-objectcreatie-zonder-connectingrole-binding` haalt de zwaarste post van het
> dekkingsverlies terug; uchar `20261006-bronvolgorde-restpunten` (e) noemt de zinnen over de resolver-WARNING bij de
> literalen van punt 8, die met deze kaart verdwijnen; uchar `20261008-drl-teksttak-op-truth-maintenance` verandert de
> DRL-teksttak, die de distributie alleen toont (CLI `drl`, `Playground`).

### Wat weggaat

**Getuigen, verwijderd of omgezet, met de reden.**
- **5a1** (`77434f56`), zeven tests weg:
  - `TijdlijnWerkBijWitnessTest`, de hele klasse, vijf tests (`een_gevulde_opbrengst_op_een_staand_doel_verdringt_de_invoer`,
    `een_gewijzigde_tijdreeks_propageert_wel`, `een_lege_opbrengst_buiten_het_staande_domein_wist_wel`,
    `een_lege_opbrengst_op_een_staand_doel_schrijft_niets`, `een_ongewijzigde_tijdreeks_propageert_niet`): ze toetsten
    alleen het tijdlijnwerk van de destructieve verlaging (`ExecModelTranspiler.werkBijTijdlijn`).
  - `DatumvouwOverRolWitnessTest.een_vouw_en_een_deling_onder_een_let_weigeren_met_de_vorm` en
    `DefaultsRechtvaardigingWitnessTest.een_kwantorvouw_in_de_rechtvaardiging_weigert_met_naam`: ze pinden alleen de
    weigering van de oude tak; hun documenten staan met de waarde van ref in `TmsZoalsRefWitnessTest`
    (`let-telling-deling`, `defaults-kwantorvouw-rechtvaardiging`).

  De lens op 5a1 bevestigde dat geen van de drie onvervangen gedrag toetste.
- **5a1, omgezet** (`35eb7e6c` … `45a3399a`): de 135 tests die onder het recept van de voormeting rood waren, volgens de
  indeling van stap 2. A (61): de zin van de tak na het voorvoegsel. B (23): via `voegIn`, `vuur`, `lees` en
  `zetAfgeleideWaarden`. C (28): de weigering met naam. D (26): twee weg (hierboven), de rest pint de waarde van ref of
  de weigering. E (1): de cijfers van de tak (`VoorafgerondeFactorWitnessTest`). Van de verdwenen namen zijn er 25
  hernoemingen (32 weg, 26 nieuw, waarvan één echt nieuw:
  `NuldelerAfnemerWitnessTest.de_tak_met_truth_maintenance_geeft_de_zin_bij_het_terugzetten`;
  `stap5a1/testnamen-diff.txt`). Vier rijen die "de andere takken" met de oude tak toetsten, toetsen alleen nog
  `DRL_TEXT` (`45a3399a`). Controle: 190 spelingen van de omgezette klassen tegen ref, met 77 overeenkomsten, 7
  replays PLAYED, 10 evaluatiefouten van dezelfde soort als in ref, 3 bewuste mismatches met de waarde van ref, 1
  `VoorafgerondeFactor`, 91 weigeringen en 1 overgeslagen replay (`stap5a1/dump.WuMY/vergelijking.txt`).
- **5a2** (`4e036240`), zeventien tests weg. Na de omschakeling speelt `EXEC_MODEL` dezelfde tak als de andere rijen van
  de klasse, dus de test vergeleek de tak met zichzelf:
  - `de_exec_modeltak_geeft_dezelfde_eindtoestand` in `TmsAfwezigeLink`, `TmsAggregatie`, `TmsAttribuutketen`,
    `TmsDelingDoorLeeg`, `TmsDoorval`, `TmsFout`, `TmsInitialisatie`, `TmsInvoerOpBerekendDoel`, `TmsKenmerkbesluit`,
    `TmsOntkenningsketen`, `TmsSchrijvers`, `TmsStapel`, `TmsUitzondering` en `TmsVoorwaardefout` (elk
    `…WitnessTest`);
  - `TmsInitialisatieWitnessTest.de_exec_modeltak_geeft_dezelfde_verdringing` en
    `TmsConsistentieWitnessTest.de_exec_modeltak_geeft_dezelfde_schendingen`;
  - `TmsParameterWitnessTest.de_exec_modeltak_geeft_dezelfde_eindtoestand`. Die was geen letterlijke dubbeling (een
    deelverzameling van drie regels); na de lens op 5a2 staat hij terug als
    `elke_volgorde_van_drie_regels_zonder_de_vergelijking_van_de_parameters` (`1dce6f23`).

  Twee tests in `TmsConsistentieWitnessTest` zijn hernoemd, omdat hun naam een vergelijking noemde die weg is. Binnen
  tests verdwenen beweringen die de oude tak vergeleken (`TmsVoorwaardefout`, `TmsFout`, `TmsInitialisatie`,
  `TmsAggregatie`, `TmsSubtype`, `TmsSamengesteld`, `TmsTerugzetting`, `TmsFilterfout`, `TmsConsistentie`). Mechanisch
  omgezet: 332× `TRUTH_MAINTENANCE` → `EXEC_MODEL`, 88× `tmsTranspiler()` → `execModelTranspiler()`, 14×
  `viaTruthMaintenance` → `viaExecModel`.
- **Eerder en later omgezet, niet verwijderd:** de rij "variabel-startpunt" van `TmsTijdlijnWeigeringWitnessTest`
  verliest haar tweede weigering (stap 2, `6296b851`); de hulpen en getuigen die zelf met `fireAllRules(100_000)`
  vuurden, vuren via de naad `DroolsBackend.vuur(b, sessie, 100_000)` en twee rijen van `TmsVuurplafondWitnessTest`
  zijn vervangen (fixronde stap 3); `ScenarioRapportWitnessTest` pint het hele spoor in plaats van drie losse
  beweringen en `ObjectcreatieVouwWitnessTest` de hele weigerzin (5b1, `6a773e9a`); de spoorpin van uitsluiting noemt
  de vorm en negen testmethoden heten met "bean(s)" (5b2); de standrijen vallen uit de pinnen van uitsluiting en
  premiestaffel, waarvan het totaal van 96 naar 70 gaat (5b4).

**Code.**
- Het pakket `transpiler/model/`: `ExecModelTranspiler` (9950 regels op de claim) en `BenoemdeToets` (`3d50652a`); de
  shaded jar bevat geen klasse uit dat pakket meer.
- `DroolsBackend.Branch.TRUTH_MAINTENANCE`, de recordcomponent `Build.tmsTranspiler`, de oude private
  `DroolsBackend.execModelTak` (de vroegere `tmsTak` draagt nu die naam), `DroolsBackend.benoem(ConstraintEvaluationException)`
  met de `catch` in `CaseRunner.vuurTotFixpunt`, de publieke `CaseRunner.viaTruthMaintenance(Path, List<Path>)`, de vlag
  `--tms` van `DroolsBackendMain` en `RegelSpraakRuntime.newInstance(Class<?>)` (5a2).
- `CaseRunner.meet` met het record `Meting` en de teller `vuringen` (stap 4, `637fb1de`); `TmsTranspiler.Defaultsgroep.eigenaars`,
  het record `Plaats` en de hulp `eigenaars(…)` (5b2, `8b4006d2`).
- Blijft, omdat de DRL-teksttak het gebruikt: `Stratificatie`, `DrlTranspiler`, `transpiler/kern/`, `Schrijfconflict`,
  `benoem(ConsequenceException)` en `rsVerdeelOpVolgorde`. `Stapels` is daarmee de enige samenstelling van ch. 7.10.
- README: 1039 regels op de claim, 1189 vóór 5b2, 837 na 5b2 en 848 op de tip (na `cd356720`). Van de secties "Recorded: …" zijn er twee herschreven
  voor de nieuwe tak ("two writers of one target", "a division by a numeric zero") en acht weggehaald, omdat ze
  mechanismen van de destructieve verlaging beschreven die niet meer bestaan ("a condition that reads through a role",
  "a defaults justification that counts or reads further than one role, and a consequence that folds", "a condition
  that puts no test on the subject", "a fold over a set source inside a rule's value or a creation's toekenning", "a
  date fold as an operand of a condition or a justification", "a reader after the writers of what it reads", "a fold
  whose subselection tests a value derived later", "a subselection that can fault"). Hun beweringen over de
  DRL-teksttak gaan naar de DRL-kaart (zie "Raakvlakken").

### Het dekkingsverlies

Vormen zonder golden die de oude exec-model-tak droeg en de nieuwe met naam weigert (verslag van stap 2, met de
nummers van de rode tests van de voormeting). In de pariteitszone (30 documenten onder `toka/`) komt geen van deze vormen
voor; haar twee objectcreaties hebben een `connectingRole`.
1. **Een dagsoortpredicaat als voorwaarde** (#4). Ref en clingo weigeren de vorm buiten een dagsoortdefinitie ook. De
   frontend schrijft haar ("is een <dagsoort>" in een voorwaarde).
2. **Een deling of wortel in een default** (#10, #12–#15, #52). De frontend schrijft haar. De weigering van de deling
   vervalt volgens PLAN §9 (2026-10-09) met `20261008-fout-op-tussenstand`; de wortel vraagt de §6.6-deponering alleen
   waar de default beslist.
3. **Een deling (of wortel) in een ledeninitialisatie van een objectcreatie** (#23, #26, #79): de §6.5-deponering bij
   het maken. De frontend schrijft haar.
4. **Een objectcreatie zonder `connectingRole`-binding** (#18, #63, #68, #72, #77). Eén golden heeft de vorm,
   `anchor/jutterswet-penninguitgifte`, die beide takken eerder op een consistentie weigeren (§13.3.9). De frontend laat
   de binding weg als de zin het type noemt in plaats van een rol (`RegelSpraakModel.benoemdeRolBinding`). De zwaarste
   post voor de toolchain.
5. **"is gevuurd" in een voorwaarde** (#30), een predicaat van het dialect over het fixpunt dat ref en clingo niet
   evalueren. De frontend schrijft het.
6. **Een macht met een niet-literale exponent** (#25); de reële macht is OPEN (RUNTIME-SEMANTICS ch. 2.1).
7. **Een som over tijdlijnen als waarde van een tijdlijndoel** (#118, #119); ref en clingo weigeren de vorm ook.
8. **Een uniciteitssleutel met twee tijdsafhankelijke delen** (#127, #128, #131). De oude tak speelde waar de
   ch. 5.9-fout niet bereikt werd en faalde anders. De frontend verlaagt zo'n sleutel niet.
9. **De producenten op een raster met variabel startpunt** (gevonden bij reparatie R1, geen rode test): totaal, knip,
   naar rato, dagentelling en "gedurende het gehele". Besluit 11. Ref voert zo'n raster niet uit.

Geen verlies tegen ref (#34, #70, #109, #114, #117): de nieuwe tak weigert daar een document dat de verifier afwijst
(WF-109, WF-050, WF-107) of waarop ref ook faalt.

**Besluit van de eigenaar** (2026-10-10, besluit 12): het verlies is aanvaard; de lijst staat in dit `Result:` en in de
kaarttekst voor de toolchain; één vervolgkaart direct na deze, `20261010-objectcreatie-zonder-connectingrole-binding`
(punt 4: een golden en het dragen van de vorm). De rest volgt bestaande kaarten (punt 2: `20261008-fout-op-tussenstand`)
of wacht op een golden.

**Gemeten op v2 herleving-ww** (eigen meting, 10:43–10:44, werkbank `rulesets/regelspraak-rules` `ae99b46`; IR en cases
met de frontend van de claim, beide exec-model-takken met de jar van de claim; de regels staan niet in de repository).
De oude exec-model-tak weigert het hele document op main (een defaults-groep met een vouw waarvan de subselectie kan
falen). De nieuwe tak weigert niets en speelt alle acht scenariobestanden, 160 van de 160 cases (36, 10, 16, 15, 17,
30, 16 en 20). Elke doorloop daarna mat het mee, de laatste op `1df4affc`: 8× PLAYED. Een pinbump zonder omschakeling
zou v2 dus breken.

### Gate 8, 12 en 13

**Gate 12** (`8076d43f`, voorstel):
- `EXPECTED_PLAYED` 242 → 248, met een gedateerde reden.
- Uit `EXPECTED_CHANNEL` gaan de zes nieuwe spelers: `anchor/jutterswet-erflijn` (een aggregatiejoin met twee
  meervoudige kanten), `anchor/jutterswet-vindersloon`, `jutterswet-vondsttarief` en `synth2-afrondingscombinaties`
  (een waarde buiten de scope van de oude tak), `jutterswet-afdrachtstapel` (een vergelijking van percentages) en
  `jutterswet-klipwacht` (de knip van "gedurende" als lid van een samenstelling). De vloer van het kanaal gaat daarmee
  van 55 naar 49 niet-spelers.
- De zes weigeringen die blijven, houden hun kanaal; hun reden volgt de zin van de nieuwe tak:
  `anchor/jutterswet-bootberging`, `anchor/jutterswet-gemeenteberging`, `anchor/jutterswet-penninguitgifte`,
  `jutterswet-havenrooster`, `jutterswet-muntketen` en `jutterswet-pachtvenster`.
- De kop van klasse (A) van `DRL_CHANNEL` zegt 52 in plaats van 46 (52 spelers alleen in de exec-model-tak: de 40 van
  (A), tien van (C), twee van (D); geen golden speelt alleen in de DRL-teksttak), en klasse (C) "VIJF … TIEN" in plaats
  van "ZEVEN … ACHT". Een notitie bij `EXCLUDE_KNOWN` en de kop van het script noemen `ExecModelTranspiler` als de
  verwijderde verlaging.
- `EXPECTED_DRL_PLAYED` (196) en `EXPECTED_MIN_TESTS` (239) blijven.

**Gate 13** (`5d1c3acb`, voorstel): alleen commentaar, namelijk de kop, de gedateerde telling van de plekken van de oude
verlaging en de zin over `jutterswet-vondsttarief`. De tellingen zijn gelijk aan die vóór de omschakeling: 43
annotaties over 24 goldens gedraaid (vloer 38), 122 replays met de onaangeroerde uitkomst, waarvan 22 door de
DRL-teksttak gespeeld (vloer 20); het vocabulairebeen 46 gestript, 46 verwisseld, 36 geroteerd, 18 replays (op
`732009b7` en `1df4affc`).

**Gate 8** (`9906feb7` en `b79a6ef6`; mandaat van de eigenaar, besluiten 15 en 20). Het Drools-been loopt via
`DroolsRoodMain` → `CaseRunner.runRood` → `EXEC_MODEL`, dus sinds `92f28966` via de tak met truth maintenance. Op
`5d1c3acb` was de gate rood: twaalf FAIL-regels, alle in het Drools-been; ref en clingo ongewijzigd.
- Acht vermeldingen op de uitsluitingslijst waren verouderd, want de tak faalt daar nu waarneembaar; ze kregen een
  `drools`-fragment met de eigen zin van de tak: `begrensd-reads-bounds-in-order`, `contextwaarde-de-dag-buiten-dagsoort`,
  `division-by-numeric-zero`, `finite-chain-over-budget`, `jutterswet-budgetgrens-over`, `tijdsduur-reads-van-before-tot`,
  `wf044-onopgelost-guard` en `wf044-onopgelost-reached`.
- `runaway-recursion` pinde "geen fixpunt (kringverwijzing?)" van het vaste plafond en pint nu
  "creation budget exhausted: objectcreatie "werf eeuwig" van stratum 0 zou een object maken op aanmaakdiepte 101",
  zoals ref.
- Drie gepinde fouten zijn nu een weigering bij de bouw en staan met reden op de lijst: `ambiguous-backlink`,
  `uni-two-temporal-keys` en `uu-two-temporal-keys`.
- Zes oudere redenen op de lijst citeerden een weigerzin die de tak niet meer geeft en zijn bijgewerkt na een eigen rode
  replay (`b79a6ef6`): `aantalgevulde-temporele-setbron`, `dagsoortparameter-ongebonden`,
  `dagsoortverwijzing-andere-dag`, `periodevenster-tijdlijnkenmerk`, `som-fallback-always-evaluated` en
  `to-one-second-filler`.
- De kop van het Drools-been noemt de zinnen van de exec-model-tak bij een gemist fixpunt; acht `NOTE.md`'s zeggen wat
  de exec-model-tak doet (drie in `9906feb7`, waaronder de golden `jutterswet-budgetgrens-op`, vijf in `b79a6ef6`).
- Uitkomst: Drools-been 25 gepind, 24 met reden (was 20 en 29); gate 8 PASS (49 rode cases, 98 gepinde fragmenten,
  geen van de 36 bodemzinnen). Ref- en clingo-pinnen onaangeroerd.

**Voorstellen voor de beschermde scripts, ter keuring bij de landing:**
- `scripts/check-drools.sh`: de pin 248, de zes namen uit `EXPECTED_CHANNEL`, de zes bijgewerkte redenen en de koppen
  hierboven. Waarom: de zes spelen met de gepinde waarden (doorloop: 242 van de 242 plus zes, zonder FAIL), een pin die
  stijgt is monotoon, en een golden die speelt maar op de kanaallijst staat, laat de gate falen.
- `scripts/check-reftype-derivation.sh`: alleen commentaar. Waarom: het script noemde de verwijderde verlaging als
  bestaande tak.
- `scripts/check-rood.sh`: de uitsluitingslijst (acht eruit, drie erin, zes redenen bijgewerkt) en de kop van het
  Drools-been. Waarom: het been drijft sinds de omschakeling de exec-model-tak met truth maintenance; een vermelding
  waar de tak nu waarneembaar faalt, laat de gate falen, en een reden die een verdwenen zin citeert, is onwaar.

### Besluiten

Uit `STAND.md`. "Hoofdsessie" is een technisch besluit van de hoofdsessie; "eigenaar" een besluit van de eigenaar.
1. *(hoofdsessie)* Het contract volgt het advies (optie b) en wijkt af van de letter van punt 4: tussen twee fires
   dragen de beans alleen invoer, `vuur` brengt de sessie naar het fixpunt, `lees` geeft het, en `zetAfgeleideWaarden`
   zet het eenmalig op de beans en sluit de sessie af. Terugzetten en later leegmaken zou een schaduwkopie vragen en
   een herstelaanroep die een afnemer kan vergeten.
2. *(hoofdsessie)* Indexeren is een geplande stap, niet voorwaardelijk; het advies mat helling 1,99 zonder en 0,65 met
   index.
3. *(hoofdsessie)* Het plafond is `max(10 000, FACTOR × R × N0)`, per creatiefase herrekend; FACTOR blijft 4, gemeten
   tegen de N die het plafond per fase telt (maximum 1,167), niet de letterlijke 48,7 die 98 zou geven.
4. *(hoofdsessie)* Het pakket `transpiler/tms/` blijft; `Build.execModelTranspiler()` heeft type `TmsTranspiler`;
   `tmsTranspiler()` en `overgeslagen()` verdwijnen.
5. *(hoofdsessie)* De weigerzin van het manifest begint weer met `transpiler SKIP:`, omdat de toolchain dat
   voorvoegsel leest.
6. *(hoofdsessie)* `TmsCorpusWitnessTest` blijft als controle in de suite, met vloer 248 gelijk aan `EXPECTED_PLAYED`
   en de reden in de javadoc.
7. *(hoofdsessie)* Het schaalmodel is RegelSpraak via de frontend; handgeschreven IR alleen waar de frontend een vorm
   niet schrijft (niet nodig gebleken).
8. *(hoofdsessie)* Gemaakte koppelingen staan in het fixpunt en komen bij het terugzetten op de referentievelden.
9. *(hoofdsessie)* Gedrag dat alleen de oude tak had (punten 13 en 18) en het dekkingsverlies komen letterlijk in de
   toolchainkaart.
10. *(hoofdsessie; voorstel, de eigenaar keurt bij de landing)* Gate 12: `EXPECTED_PLAYED` 242 → 248, de zes nieuwe
    spelers uit `EXPECTED_CHANNEL`.
11. *(hoofdsessie)* Gate 11: de tak aanvaardt een `StartPointDetermination` die geen vorm gebruikt, en weigert met
    naam alleen een vorm die een tijdlijn op een raster met variabel startpunt leest of schrijft.
12. *(eigenaar, 2026-10-10)* Het dekkingsverlies is aanvaard; de lijst gaat in het `Result:` en de toolchainkaart; één
    vervolgkaart direct na deze voor de objectcreatie zonder `connectingRole`-binding; de rest volgt bestaande kaarten
    of wacht op een golden.
13. *(hoofdsessie)* `voegIn(Build, KieSession, Invoer)` stelt `Emmerinvoer` en `Lijstplaats` zelf. De volgorde van een
    lijst is standaard die van de beans, `lijst(…)` legt haar vast, en zij bepaalt alleen welk lid een fout noemt. Een
    emmerscalair is een expliciete opgave.
14. *(hoofdsessie)* Geen supertype `Evaluatiefout`: Java vangt geen interface, en een gemeenschappelijke superklasse zou
    de ouder `ArithmeticException` van `DelingDoorNul` veranderen. `lees` geeft de fouten als gegevens.
15. *(eigenaar, 2026-10-10)* Mandaat in deze kaart voor gate 8: de `drools`-fragmenten in `corpus/rood/**/case*.json`,
    de uitsluitingslijst van `scripts/check-rood.sh` (voorstel) en de drie `NOTE.md`'s die `--tms` noemden; ref- en
    clingo-pinnen onaangeroerd.
16. *(hoofdsessie)* De weigerzin `NOG_NIET` wordt "deze tak draagt deze vorm nog niet", wat ook na de DRL-kaart klopt;
    de kringweigering krijgt het label `exec-model:`.
17. *(eigenaar)* Java beans heten in Nederlandse tekst "bean" en "beans"; `Invoer.boon` en `bonen` werden `bean` en
    `beans` voordat de toolchain de ingang gebruikt.
18. *(eigenaar)* Zuinig lezen in de hoofdsessie; het concept van het `Result:` maakt een verse agent uit de bestanden
    van de werkmap.
19. *(hoofdsessie)* Het scenariorapport telt een vuring onder de vorm van het document, één rij per vorm, zoals vóór de
    omschakeling; gerepareerd in deze kaart (5b2), niet in `basisNaam`, die gate 15, `Kringen` en de dekking dient.
20. *(eigenaar, 2026-10-10)* Het mandaat van besluit 15 geldt ook voor de vijf `NOTE.md`'s die door de omschakeling
    onwaar werden, en voor de kop van het Drools-been in `scripts/check-rood.sh` (voorstel).
21. *(hoofdsessie)* Een rij van het rapport heet naar `vormVan` met alleen het versie-achtervoegsel eraf
    (`RegelSpraakModel.vormnaam`), zodat een vormnaam met ": " heel blijft; een regel zonder vorm krijgt geen rij. Dat
    raakt de toolchain (`CaseSessie`, `ScenarioRunner` r. 166).

Daarnaast, zonder nummer: v1 herleving-ww wordt niet gemeten (eigenaar, 2026-10-10).

### Lensvondsten

Twaalf lenzen op sol6, elk één premium request, na elke stap en na elke fixronde. De hoofdsessie mat elke vondst na
met een proefdocument dat de verifier goedkeurt, of met een getuige, voordat zij haar overnam. Samen 18 BLOKKEREND (14
overgenomen, 4 afgewezen) en 8 ADVIES.

| Lens | Op | BLOKKEREND | ADVIES | Overgenomen | Afgewezen, met reden |
|---|---|---:|---:|---|---|
| stap 1 | `095a586a` | 0 | 1 | — | ADVIES "de meting vóór de indexen bij N = 10 000 ontbreekt": kaartpunt 9 vraagt de vloer na de indexen, en de helling vóór staat vast uit N = 100 → 1 000 (1,85) en het advies (1,99). |
| stap 2 | `932229dc` | 0 | 1 | twee oude getuigen noemden een WF-109- en een WF-107-document schoon; die claims gingen niet mee in de omzetting (`7876b4f2`) | — |
| stap 3 | `f4b83d7b` | 3 | 1 | herhaald vuren met creatie (`862cb632`); een gerefereerd object buiten de invoer (`94cea187`); de plafondgetuige (`ad08da29`); van het ADVIES: lezen en terugzetten alleen na een `vuur` met fixpunt (`f1287f25`) | ADVIES-deel "een vergeten emmerscalair": besluit 13, een volledige tijdlijn van één segment is geldige invoer. |
| herkeuring fixronde 3 | `32a649c4` | 2 | 0 | een kale `fireAllRules` na `vuur` (`2d95c456`); een relatie naar een object buiten de invoer tussen twee fires (`fb80e8df`) | — |
| stap 4 | `928e39d4` | 1 | 1 | de exitcode van `Schaalproef` (`be772617`) | BLOKKEREND "FACTOR wijkt af van de letter": besluit 3, het plafond wordt per creatiefase herrekend en de maat per fase is ten hoogste 1,167. |
| herkeuring tweede fixronde 3 | `81ac1d6c` | 2 | 0 | verouderde lijstplaatsen na een relatiewijziging; agenda geleegd of focus verzet buiten de bewaking (`f0b7bbf9`) | — |
| stap 5a1 | `45a3399a` | 4 | 1 | een relatie die zonder `update` verandert, en de onbekende `PropagationEntry` (`55d178d8`); de rapportgetuige, de C-getuige met een halve zin, twee onterechte schoonheidsclaims (`6a773e9a`) | — |
| stap 5a2 | `5d1c3acb` | 0 | 2 | `Schrijfconflict` alleen op de DRL-teksttak (`e9ed38d8`); de rij van drie regels van `TmsParameterWitnessTest` terug (`1dce6f23`) | — |
| stap 5b1 | `9906feb7` | 0 | 0 | — (de hoofdsessie vond daarna zes verouderde redenen op de uitsluitingslijst, `b79a6ef6`) | — |
| stap 5b2 en 5b3 | `732009b7` | 3 | 1 | een vormnaam met ": " (`538a2fbd`); de corpusgetuige beweerde meer dan zij doet (`cad4b55b`); het ADVIES als besluit 21, rijen zonder vorm weg (`538a2fbd`) | BLOKKEREND "defaultsgroepen op één doel in één rij": het rapport vóór de omschakeling gaf dezelfde rij (gemeten met de jar van de claim), en besluit 19 vraagt dat rapport. |
| herkeuring 5b4 | `cad4b55b` | 3 | 0 | een versie die niet vuurt heette GEVUURD (`1df4affc`) | Twee: een gewone regel die "defaults K:Jutter.uitgesloten" heet, en een kenmerk "uitgesloten #2"; beide uiterst onwaarschijnlijke invoer, en het rapport vóór de omschakeling deed hetzelfde. |
| herkeuring 5b5 | `1df4affc` | 0 | 0 | — | — |

**Eindlens (Opus 5.5, op `1df4affc`)**: één BLOKKEREND en vijf ADVIES, eindoordeel "eerst fixen"
(`eindlens/lens.md`). Afhandeling volgens git en de hoofdsessie: de BLOKKEREND en advies 2 met tekst in `cd356720`,
advies 3 in `2037905a`, advies 4 als voorstel bij de landing (`CLAUDE.md` is een beschermd pad), advies 5 in de
kaarttekst hierboven, advies 6 met de meting op de tip (00:04–00:05). De tekst na de eindlens volgt de route die zij
zelf noemde en kreeg geen aparte herkeuring; de gates op de tip draaiden erna.
- BLOKKEREND 1: een attribuut dat zonder `update` verandert, geeft stil het oude fixpunt, terwijl README r. 92,
  `DroolsBackend.java` r. 544 en `TmsTranspiler.java` r. 419 beloven dat `lees` altijd het fixpunt van de invoer zoals
  die staat geeft; de bewaking vergelijkt alleen referentievelden. Faalscenario op het schaalmodel zonder (g): na
  `V1_1.setGewicht(40)` zonder `update` geeft de volgende `vuur` 0 vuringen en `lees` beloning 1, wachtsom 270 en W1
  consistent, waar een verse sessie 80, 349 en W1 inconsistent geeft. Volgens de eindlens volstaat tekst (de README, de
  vergeetlijst en de kaarttekst).
- ADVIES 2: een emmerscalair op een lege of meervoudige tijdlijn ziet `voegIn` niet; `vuur` eindigt dan met een
  `ConsequenceException` (`DroolsBackend.java` r. 423, `TmsTranspiler.java` r. 6104), tegen README r. 175 en r. 148.
- ADVIES 3: `LetDragerWandelingWitnessTest` (r. 22–23, 48–49), `CompanionPadWitnessTest` (r. 46) en
  `EffectieveEenheidWitnessTest` (r. 182) beschrijven `ExecModelTranspiler` nog als iets dat bestaat.
- ADVIES 4: `CLAUDE.md` r. 261, gate 12, "through the ported exec-model back-end"; een voorstel bij de landing.
- ADVIES 5: de kaarttekst voor de toolchain moet uitgaan van 5a1 §7.5, 5a2 §6 en de besluiten 17 en 21, niet van stap 3
  §7 of de tweede fixronde; dit concept doet dat.
- ADVIES 6: de eindtabel van de schaalproef kwam van `928e39d4`; de tabel in (f) is nu die van de tip.

### Open punten en risico's

1. **Wat de ingang niet ziet** (eindlens, vondst 1 en advies 2; beschreven, niet bewaakt). Een attribuut of kenmerk
   dat zonder `update` verandert: `lees` geeft dan het oude fixpunt en `zetAfgeleideWaarden` zet dat naast de nieuwe
   invoer; de afnemer moet na elke wijziging `update` aanroepen. Een emmerscalair op een lege of langere tijdlijn:
   `voegIn` toetst die vorm niet, en de volgende `vuur` breekt af met een `ConsequenceException`. Beide staan sinds
   `cd356720` in de README ("What a consumer can forget") en in de kaarttekst voor de toolchain.
2. **De bewaking leest interne klassen van Drools** (`org.drools.core.common.InternalAgenda`,
   `AgendaGroupQueueImpl.ClearAction` en `SetFocusAction`, `InternalAgendaGroup.getClearedForRecency`), omdat
   `Agenda.clear` geen gebeurtenis geeft. Bij een upgrade van Drools breekt dat luid: een `ClassCastException`, een
   compileerfout, of sinds `55d178d8` een weigering die de klasse van een onbekende actie op de propagatielijst noemt.
   Een gedragswijziging van Drools buiten die drie gevallen is niet getoetst; er is geen andere Drools-versie gemeten.
3. **Het terugzetten kost meer dan in stap 1**: 178 ms (`1d90f342`) tegen 665,2 ms (`928e39d4`) bij N = 10 000, 733,2
   ms in de meting van 5b2 na de omschakeling en 729,3 ms op de tip. Volgens de sonde van stap 4 zit de tijd in de
   lezing van stap 3 (`lees` 600 à 690 ms voor 250 001 waarden), niet in het merkfeit. De vloer blijft gehaald.
4. **De tweede fire groeit met N.** Op `928e39d4` kostte zij bij een vast aantal van 16 vuringen 0,6 / 4,5 / 68,2 ms;
   de tijd zat in twee lineaire doorzoekingen naar de merkfeiten (16,3 en 49,4 ms bij N = 10 000). Na `3fa9ef63`
   zoeken die per klasse en kost een `vuur` zonder werk 8,4–8,9 ms, vooral de referentiecontrole over alle
   invoerbeans. De vergelijking van de relaties (`55d178d8`) maakte de tweede fire bij N = 10 000 11,2 → 16,8 ms. Op
   de tip kost zij 1,1 / 3,5 / 16,4 ms. Een afnemer die op een grote sessie vaak vuurt, betaalt per fire een tijd die
   evenredig is met het aantal beans.
5. **Het plafond telt bij een creatiefase alle feiten, ook de afgeleide**, zo ontworpen: bij het schaalmodel is het
   ongeveer 9 keer zo ruim als `4 × R × N0` (94 453 824 tegen 10 560 096). FACTOR 4 ligt op de maat per fase, niet op
   de letterlijke 48,7 (besluit 3).
6. **Joins zonder index en alfa-indexen.** Zonder index blijven `pad` (een bereik van meer stappen), `plaats-lid`,
   `veld-nabij`, `veld-ver` en `botsing` (uniciteit); een regelset met een bereik van meer stappen blijft daar
   kwadratisch. Alfa-indexen wachten op gelijke ids voor gelijke constraints: in Drools 8.44 laat een alfa-index met de
   eigen ids van deze tak een regel stil niet vuren (stap 1).
7. **`voegIn` vult alleen een verse sessie**: een nieuw, verwijderd of anders verbonden object vraagt een verse sessie.
   Voor de toolchain een grens van het contract; de kosten van het vastleggen van de relaties bij `voegIn` zijn niet
   apart gemeten (5a1).
8. **De voorafgeronde factor** (CONSTRUCTS rij 26): de exec-model-tak geeft bij 1/24 nu 9.00000000000000072, de
   DRL-teksttak 9.000000000000001, ref 9.00; afgerond naar boven 10 in beide takken waar ref 9 geeft. Rij 26 en het
   besluit van stap 2 noemen een eigen kaart om dit te sluiten; die staat niet in de backlog, en deze kaart maakt haar
   niet aan (de afwijking is ouder dan de omschakeling).
9. **De lezing schrijft de lagere default "1,00" van het schaalmodel als `1`** (een `BigDecimal` met schaal 0), zoals de
   oude tak deed. Of een afnemer "1" of "1.00" hoort te tonen, is niet nagegaan (stap 4).
10. **Voor de DRL-kaart**: de grenzen van de DRL-teksttak uit de acht weggevallen README-secties (verslag 5b2 §8.2);
   `DrlTranspiler` noemt `ExecModelTranspiler` op negen plekken, met een `{@link ExecModelTranspiler#stratificatieSalience}`
   op r. 351 die nergens heen wijst, `Stratificatie` r. 142 en `kern/Vouwwaarde` r. 63, 98 en 68–70 (§8.4); en circa
   180 keer "the truth-maintenance branch" als naam van de tak in testjavadoc, dat dubbelzinnig wordt zodra de
   DRL-teksttak ook truth maintenance heeft (§8.5).
11. **Al bestaand, niet van deze kaart**: drie javadocverwijzingen die nergens heen wijzen
    (`engine/RegelSpraakCompileException` r. 7, `engine/BeanCompiler` r. 29, `kern/Dagsoorten` r. 24);
    `RegelSpraakRuntime.num` en `numeriek` zonder aanroeper (5a2 §11); `LetMeldplekWitnessTest` r. 24 ("gate 12
    measures the exec branch alone") is onjuist sinds 2026-09-19 (5b3).
12. **Niet onderzocht**: de vraag "gelijk met/zonder sleutel?" bij `MeerPeriodiekePoort` uit de notities van de eerste
    bouwer van 5a1 (open punt 4 van 5a1).

### Bijvangst en nieuwe kaarten

- **`20261010-clingo-let-binder-vergelijking-over-eenheden`** (nieuwe kaart, bij de landing in `backlog/new`). Bijvangst
  van stap 2: in "Let a = aanloop in (als a groter dan 1 km dan 1,00 anders 2,00)" met een aanloop van 500 m vergelijkt
  clingo 500 met 1 zonder herrekening en antwoordt 1.00, waar ref 2.00 geeft (document `zoals-ref/let-kruiseenheid`,
  verifier PASS). Het
  is een ander gebrek dan dat van `20261009-clingo-let-binder-tussen-onderwerpen` (daar lekt de waarde van een binder
  naar een ander onderwerp), dus een eigen kaart. De naam staat al in de javadoc van `TmsZoalsRefWitnessTest` en
  `LetDragerWandelingWitnessTest`; die verwijzingen kloppen zodra de kaart onder deze naam in `backlog/new` staat.
- **`20261010-objectcreatie-zonder-connectingrole-binding`** (nieuwe kaart, besluit 12). Een golden met een objectcreatie
  zonder `connectingRole`-binding, en de exec-model-tak leidt de terugkoppeling dan uit het typenpaar af met het
  gedeelde beslisblok, zoals de DRL-teksttak. De naam staat al in `ObjectCreatieWitnessTest` en
  `DrlCreatieTakWitnessTest`.
- **Aanvulling op `20261009-vouwfilter-resterende-vormen`** (bijvangst van 5a1b; een sectie onderaan die kaart). Clingo
  deelt in het filter van een vouw voor elke Vondst, ook voor een die bij geen of een niet-actieve Jutter hoort (ref
  PASS, clingo UNSATISFIABLE op V2). Dat is de fout van de eerste twee rijen van die kaart (de guard draagt de join van het lid op het
  onderwerp niet); de aanvulling legt er twee documenten op een ander model bij en zet de kaart op de stand na de
  omschakeling.

Verder gemeten en in deze kaart gerepareerd: het scenariorapport voegde vormen samen onder één basisnaam (5b1 §9,
`07a7c4a1`) en noemde elke defaultsgroep `NIET_GERAAKT` (5b2 §9, zelfde commit); de dekking noemde een niet-geldende
versie GEVUURD (`1df4affc`; het rapport vóór de omschakeling deed hetzelfde). Een `AgendaGroup.clear` die Drools pas
bij de volgende fire uitvoert, breekt die fire af met `UnsupportedOperationException: null`; de ingang weigert dat nu
vóór het vuren (5a1, gedrag van Drools zelf).

### Raakvlakken na de landing

Voorgestelde regels, door de hoofdsessie bij de landing toe te voegen; hier is niets gewijzigd.
- **`20261008-deling-door-leeg-onder-initialisatie`** (`backlog/new`). Voorstel: "Na `20261008-tms-omschakeling`
  (2026-10-10) leidt de exec-model-tak af met truth maintenance en speelt beide cases (getuige
  `TmsInitialisatieWitnessTest.een_deling_door_leeg_deponeert_alleen_waar_de_initialisatie_geldt`, beide op
  `EXEC_MODEL`): (b) geldt nog voor clingo, (c) voor de DRL-teksttak, en `ExecModelTranspiler.java` in May touch bestaat
  niet meer."
- **`20261008-fout-op-tussenstand`** (`backlog/new`). Voorstel: "Na `20261008-tms-omschakeling` (2026-10-10) is de
  exec-model-tak de tak met truth maintenance; de rijen 'Drools, exec-modeltak' in de tabellen beschrijven de verwijderde
  destructieve verlaging. De twee weigeringen die op dit besluit wachten (een deling in een default, een deling in een
  initialisatie naast een andere schrijver) staan nu in de exec-model-tak en zijn dekkingsverlies (punt 2 van de lijst in
  het `Result:` van die kaart); (c) geldt voor de exec-model-tak en de DRL-teksttak."
- **`20260918-drools-veelopveel-beanmodel`** (`backlog/new`). Voorstel: "Na `20261008-tms-omschakeling` (2026-10-10)
  speelt `anchor/jutterswet-erflijn` in de exec-model-tak (gate 12, 248) en weigert alleen de DRL-teksttak hem nog;
  `anchor/jutterswet-bootberging` en `jutterswet-muntketen` weigeren in beide takken, in de exec-model-tak met
  'sourcePath-stap 1 (Boot -> Vondst/Munt) heeft geen beanveld'. De kaart raakt twee takken, `transpiler/tms/` en
  `DrlTranspiler`."
- **`20260923-drools-rolpaar-ontdubbeling`** (`backlog/done`). Geen regel: de kaart sloot met
  `20261008-tms-aggregaties-creatie-en-fouten`, en de kopie van het beslisblok verdween met `ExecModelTranspiler`
  (`3d50652a`).
- **`20261008-drl-teksttak-op-truth-maintenance`** (`backlog/new`). Voorstel: "Na `20261008-tms-omschakeling`
  (2026-10-10): de exec-model-tak is de tak met truth maintenance (`transpiler/tms/`, label `exec-model:`); gate 12 pint
  248 en 196, en klasse (A) van `DRL_CHANNEL` telt 52 spelers alleen in de exec-model-tak. Begininventaris: de grenzen van
  de DRL-teksttak uit de acht weggevallen README-secties (verslag 5b2 §8.2); de negen vermeldingen van
  `ExecModelTranspiler` in `DrlTranspiler`, met de dode `{@link}` op r. 351, `Stratificatie` r. 142 en `kern/Vouwwaarde`
  (§8.4); circa 180 keer 'the truth-maintenance branch' in testjavadoc (§8.5); acceptatie (c) van de omschakeling vond
  alleen `DrlTranspiler.java:1965`."
- **`20261009-vouwfilter-resterende-vormen`** (`backlog/new`): de aanvulling hierboven, als sectie onderaan die kaart.
- **`20261009-clingo-let-binder-tussen-onderwerpen`** (`backlog/new`). Voorstel: "Na `20261008-tms-omschakeling` is 'de
  tak met truth maintenance' de exec-model-tak. Een ander gebrek van clingo bij een Let-binder staat in
  `20261010-clingo-let-binder-vergelijking-over-eenheden`."
- **`20261006-check-wf046-door-recursieve-creatie`** (`backlog/new`, grep op `ExecModelTranspiler`). Voorstel: "Na
  `20261008-tms-omschakeling` (2026-10-10) bestaan `ExecModelTranspiler`, `kringWeigering` en `groepVolgorde` niet meer;
  de exec-model-tak weigert bij de bouw wat WF-045/046 weigeren (`Kringen`, 'exec-model: kring in de afhankelijkheden
  …'). De kolom 'exec-modeltak' van de tabel beschrijft de verwijderde verlaging; wat de huidige exec-model-tak op de
  vijf negatieven doet, is niet nagemeten."
- **`20261009-drools-schrijfgrens`** (`backlog/new`, grep op "drie takken"). Voorstel: "Sinds `20261008-tms-omschakeling`
  (2026-10-10) heeft de Drools-backend twee takken; 'drie takken' is de stand van 2026-10-09."
- **`20261006-bronvolgorde-restpunten`** (`backlog/new`, grep op `ExecModelTranspiler`). Voorstel bij (e): "Na
  `20261008-tms-omschakeling`: het commentaar op r. 548 van `ExecModelTranspiler.java` is met dat bestand weg; de
  exec-model-tak weigert een kring die WF-045/046 verbieden bij de bouw, en de twee meldingen in de toolchain verdwijnen
  met haar kaart voor de omschakeling."
- **`20261008-website-drools-routes-en-faq`** (`backlog/new`, grep op `ExecModelTranspiler`). Voorstel: "Sinds
  `20261008-tms-omschakeling` heet de verlaging van de exec-model-tak `TmsTranspiler` (`transpiler/tms/`, met truth
  maintenance); `ExecModelTranspiler` in het schema bestaat niet meer."
- **`20261010-impliciete-tijd`** (`backlog/new`, ongecommit in de hoofdcheckout; grep op `ExecModelTranspiler`).
  Voorstel: "Na `20261008-tms-omschakeling`: `ExecModelTranspiler.java` bestaat niet meer; de exec-model-tak schrijft
  een tijdlijn alleen met een tijdsevenredig deel of een dagentelling (`backends/drools/README.md` r. 509, sectie 'The
  exec-model branch')."
