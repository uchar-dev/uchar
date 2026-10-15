# gegenereerde-elementen-dragen-herkomst: een element dat de frontend zelf maakt, zegt waaruit

*Op verzoek van de eigenaar (2026-10-15). Regelnummers op `c7191c58` (de branch `card/scenario-rol-draagt-feittype`,
gekeurd en nog niet geland; deze kaart bouwt erop voort).*

Goal: de RegelSpraak-frontend en de Drools-backend maken namen die nergens in de bron staan. Een consument (de
playground van regelspraak-toolchain en de RegelSpraak-werkbank daarachter) ziet in het model `berekende maxdatum`,
`afwijkende opgave maxdatum` en `opgave boven berekening maxdatum` staan, en in de dekking en de trace `defaults
A:RechtOpUitkeringWwBijOntslag.maxdatum`. Hij kan niet zien dat uchar die namen maakte, en ook niet uit welk
gedeclareerd element. Op de naam afleiden mag hij niet: "berekende" of " voor " kan ook in een geschreven naam staan
(golden `jutterswet-wachtgeld` heeft de geschreven regel `wachtgeld voor kustwachters`). uchar weet het op de plek waar
het de naam maakt, maar bewaart het niet: de opgave-markering houdt haar gegevens in de private lijsten `opgaven` en
`gelowerd` van `RuleChecker` (`resolver/RuleChecker.java:2304`, `:2317`, `:2320`), de rijen van een beslistabel in de
private kaart `rijRegels` (r. 64), het gegenereerde lid van een vouw in het record `Hulplid` van `ExpressieBouwer`
(`resolver/ExpressieBouwer.java:809`), en de effectieve defaultsstapel van de backend in het pakket-private
`Stapels.Stapel` (`backends/drools/…/transpiler/tms/Stapels.java:38`). `RegelSpraakModel.Benoembaar` heeft alleen
soort, eigenaar en naam (`model/RegelSpraakModel.java:1299`), en de IR heeft geen herkomstveld: `source` is
brontekst (`spec/IR-SPEC.md`, het record `Source`).

De kaart legt de herkomst vast als een zijtabel zonder semantiek, zoals `benamingen`: per gegenereerd element het
gedeclareerde element waaruit het komt en een vaste sleutel voor de vorm. Het IR-document, het schema en de spec
veranderen niet; geen transpiler leest de tabel.

Acceptance:
- (a) `RegelSpraakModel` krijgt het record `GegenereerdUit(Benoembaar bron, Herkomstvorm vorm)`, de enum
  `Herkomstvorm` met per waarde een vaste sleutel (`opgave-boven-berekening`, …), de publieke kaart
  `Map<Benoembaar, GegenereerdUit> gegenereerd` en de opzoeker `gegenereerdUit(Benoembaar)` (naamidentiteit van
  IR-SPEC ch. 2.1, zoals `benaming`). Na het resolven staat in die kaart precies elk element van `benoembaar()` dat de
  frontend zelf maakte, onder de sleutel uit `benoembaar()`:
  - de opgave-markering op attribuut X van T: `opgegeven X`, `berekende X`, `afwijkende opgave X`, `afwijking opgave
    X`, `controle opgave X` en `opgave boven berekening X`, uit het attribuut X van T;
  - de groep `B voor S` van een blok voor een subtype S onder basisregel B (dialect §4, de erflijn), uit de vorm B;
  - elke rij van een beslistabel B, uit B (als vorm benoemd; de tabel zelf is geen element van het model);
  - het lid en de regel `<lid> via <rol 1>, … en <rol n>` van een vouw over een verzameling met stappen erna, uit het
    bereikte attribuut zoals gedeclareerd;
  - het anker `startpunt <tijdlijn>` van een startpuntbepaling, uit de regel die het schrijft, behalve waar de auteur
    een lid van die naam zelf declareerde.
  Een element met een geschreven naam staat er niet in, ook niet als het een groep is die naar haar basisregel of
  naar een lid van een ongeordend blok heet.
- (b) De TMS-transpiler van de backend geeft per naam in zijn manifest die geen gedeclareerd element noemt de herkomst
  (`TmsTranspiler.gegenereerd()`): een effectieve defaultsstapel (`defaults A:T.m`, `defaults K:T.m`) uit het lid m
  zoals gedeclareerd, op het type in de keten van T dat het declareert; een versie van een vorm met meer versies
  (`R #k`) uit de vorm R.
- (c) Nieuwe testklassen, eerst rood: een in de frontend (elke vorm van (a), een geschreven naam met "berekende" en met
  " voor " zonder herkomst, een ingetrokken markering zonder herkomst, elke sleutel een element van `benoembaar()`), en
  een in de backend (stapels op een type en zijn subtype, op een attribuut en een kenmerk, een versie, en elke
  dekkingnaam is een vorm van het model of staat in `gegenereerd()`).
- (d) `frontends/regelspraak/grammar/DIVERGENCES.md` beschrijft de tabel. Alle 16 gates van `CLAUDE.md` groen op de
  eindstand van de branch; gate 10 en 16 laten zien dat elke `ir.json` en elk case-bestand bytegelijk blijft.

May touch: `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/model/RegelSpraakModel.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/RuleChecker.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/ExpressieBouwer.java`,
`frontends/regelspraak/grammar/DIVERGENCES.md`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java`, de nieuwe testklassen
`frontends/regelspraak/src/test/java/dev/uchar/regelspraak/GegenereerdUitTest.java` en
`backends/drools/src/test/java/dev/uchar/regelspraak/transpiler/tms/GegenereerdeNamenWitnessTest.java`, deze kaart.

Out of scope: het IR-document, het schema en `spec/IR-SPEC.md` (de tabel is geen wire-gegeven; een IR-veld zou een
voorstel zijn); de namen in het fixpunt die geen vorm zijn (de sleutel van een defaultsconflict, de relatie van een
tweede vulling); de Drools-regelnamen binnen een vorm (`<vorm>: …`), die de dekking en de trace niet tonen; de
toolchain (die neemt de tabel over na een pin-bump, op haar eigen kaart).

## Result (2026-10-15)

`e90ab2d4` legt de herkomst van gegenereerde elementen vast, in het model en in de dekking van de backend. Geen
wire-, schema- of specwijziging: de tabel is een zijtabel zoals `benamingen`.

- **Model** (`model/RegelSpraakModel.java`): het record `GegenereerdUit(Benoembaar bron, Herkomstvorm vorm)`, de enum
  `Herkomstvorm` met de vaste sleutels `opgave-boven-berekening`, `uitzondering-voor-subtype`, `beslistabelrij`,
  `stappen-na-de-verzameling`, `startpunt`, `defaultsstapel` en `versie`, de kaart `Map<Benoembaar, GegenereerdUit>
  gegenereerd` en de opzoeker `gegenereerdUit(Benoembaar)` (naamidentiteit van IR-SPEC ch. 2.1). `kopie()` neemt de
  kaart mee zonder eigen regel (reflectie over de publieke verzamelingen).
- **Frontend** (`resolver/RuleChecker.java`): de nieuwe laatste pas `gegenereerdeElementen()` vult de kaart uit de
  boekhouding die de passes al hadden (`opgaven`, `gelowerd`, `rijRegels`, `bouwer.hulpleden()`) en uit twee nieuwe
  private lijsten: de groepen `B voor S` met hun basis, en de ankers `startpunt <tijdlijn>` die `startpuntAnkers` zelf
  declareerde (een anker dat de auteur declareerde, krijgt geen herkomst), met de eerste regel die het schrijft. Alleen
  elementen die dan nog in `benoembaar()` staan, krijgen een ingang, gespeld zoals die lijst ze spelt; het bronelement
  ook, behalve een beslistabel, die geen element is. Twee verschillende herkomsten voor één element, of een bron die
  geen element is, geven in een model zonder ERROR een `IllegalStateException`.
- **Backend** (`transpiler/tms/TmsTranspiler.java`): `gegenereerd()` geeft per dekkingnaam die geen declaratie noemt de
  herkomst: een effectieve stapel uit het lid zoals gedeclareerd op de keten van haar eigenaar (`stapeldoel`, faalt
  als de keten het lid niet declareert), een versie `R #k` uit vorm R (in `meet`, via `RegelSpraakModel.vormnaam`).
- **Tekst**: `grammar/DIVERGENCES.md`, nieuwe sectie "Gegenereerde elementen".

Niet gedekt, met reden in DIVERGENCES: de Drools-regelnamen binnen een vorm (`<vorm>: …`, geen dekking- of
tracerij) en de namen in het fixpunt die geen vorm zijn (de sleutel van een defaultsconflict, de relatie bij een
tweede vulling). Een groep die naar haar basisregel heet, en een groep per lid van een `Uitzonderingen ongeordend:`
zonder drager, dragen een geschreven naam en krijgen geen ingang.

### Acceptatie

- (a) Gehaald; `GegenereerdUitTest` toont elke vorm.
- (b) Gehaald; `GegenereerdeNamenWitnessTest`.
- (c) Nieuw: `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/GegenereerdUitTest.java` (9 tests: de zes
  elementen van de opgave-markering en een geschreven `berekende taxatie` zonder herkomst, een ingetrokken markering,
  de groep `meldtermijn voor Strandjutter` uit `meldtermijn`, een geschreven `wachtgeld voor kustwachters` zonder
  herkomst, de rijen `Bonustabel 1` en `2` uit `Bonustabel`, het lid `gewicht via vondst en weging` uit
  `Meting.gewicht` (gedeclareerd op het supertype van het bereikte type), het startpuntanker met en zonder eigen
  declaratie, een document zonder gegenereerde namen, de sleutels van de enum) en
  `backends/drools/src/test/java/dev/uchar/regelspraak/transpiler/tms/GegenereerdeNamenWitnessTest.java` (4 tests op
  de goldens `jutterswet-opgave-subtype`, `jutterswet-nestkenmerk-rgs` en `anchor/jutterswet-jutheffing`: vijf
  stapels op drie typen, waarvan twee op `berekende meldingsdatum`, alle uit `Vondst`; een kenmerkstapel; twee
  versies; elke dekkingnaam is een vorm of gegenereerd; een model uit de wire heeft een lege tabel).
  Rood vóór de wijziging: beide klassen compileren niet (`cannot find symbol` op `gegenereerd`, `GegenereerdUit`,
  `Herkomstvorm`; de frontend 16 fouten). Drie tijdelijke mutaties lieten een test vallen: het anker altijd als
  gegenereerd (`expected: <{}> but was: <{…startpunt penningjaar…}>`), de bron van het vouwlid op het bereikte type in
  plaats van het declarerende (`IllegalStateException … geen element van het model`), de stapelbron op de eigenaar
  zelf (`de stapel defaults A:Strandvondst.berekende meldingsdatum beslist een lid dat de keten van Strandvondst niet
  declareert`). Geen bestaand testbestand gewijzigd.
- (d) `grammar/DIVERGENCES.md` beschrijft de tabel. Alle 16 gates groen op `e90ab2d4` (de boom `466d5768`),
  10:25:35–10:43:25; deze commit raakt daarna alleen `backlog/`:
  1 PASS; 2 348 bestanden valideren en round-trippen; 3 348 golden schoon, 505 negatief afgewezen; 4 873
  case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite 2198 checks, 823 cases; 7 778 cases, 45 survivors;
  8 50 rode cases; 9 241 checks, 12 catala-goldens; 10 JUnit 1178 tests (was 1169, plus de 9 nieuwe), 125
  rgs-goldens bytegelijk; 11 45 TOKA-bronnen bytegelijk, TOKA-suite 27 tests, 29 TOKA-cases op ref, clingo en Drools;
  12 608 s, 255 gespeeld op beide takken, JUnit 1578 (was 1574, plus de 4 nieuwe); 13 43 twists over 24 goldens, 122
  replays, vocabulaireleg 18 replays; 14 150 checks, 2 goldens; 15 266 van 303 goldens vergeleken, 125 op de
  bronroute, 69 unittests; 16 30 scenario-goldens, 102 case-bestanden en hun `ir.json` bytegelijk.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: de kaart verandert geen construct,
geen wire, geen schema en geen case-formaat, alleen de API van het frontend-model en van de TMS-transpiler.

Voor de distributie: de playground van regelspraak-toolchain geeft de tabel door op haar kaart
`playground-geeft-gegenereerd-uit`, met een pin op deze branch. Die pin kan pas landen als deze kaart op origin
staat; een rebase vóór de landing verandert de commit en dan de pin.
