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
