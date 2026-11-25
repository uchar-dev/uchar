# settled-empty-input: een casus kan zeggen dat een attribuut vaststaand leeg is

*Vervolg op de geparkeerde kaart van 2026-11-22 in `uchar/backlog/new/20261122-settled-empty-input.md`; ontparkeerd en uitgewerkt op 2026-11-25 tegen `uchar` `4d6bbe6be` (toolchain `04e48241b`), met expliciet mandaat van de eigenaar voor de geraakte beschermde paden (`spec/` en `scripts/`).*

Goal: in een strikt document (`inferFromEmpty: false`, `Afleiden uit leeg: nee`) kan een casus voor een scalair, niet-tijdsafhankelijk, niet-gedimensioneerd attribuut expliciet aangeven dat het **vaststaand leeg** is — de symmetrische tegenhanger van `characteristicsAbsent` voor kenmerken:
1. Het caseschema (`uchar-case-v0`) krijgt per instantie de optionele lijst `attributesEmpty` (direct na `attributes`, vóór `roles`).
2. `ref`, `clingo` en `Drools` (executable model en DRL-tekst, plus `Input.attributeEmpty` / `DroolsBackend.insert`) lezen `attributesEmpty` volgens RUNTIME-SEMANTICS ch. 1.5: in een strikt document is zo'n invoerattribuut vaststaand leeg in plaats van open (en naast een schrijver volgt het de regels voor een invoerwaarde: open zolang een schrijver open is, de schrijver waar die vult, en vaststaand leeg waar elke schrijver niets vult; bij alleen een initialisatie is de initialisatie aan de beurt). In een gewoon document verandert het veld geen uitkomst.
3. De scenariolaag van de RegelSpraak-frontend schrijft `attributesEmpty` uit `- de <attr> is leeg` en `- het <attr> is leeg` in een `Situatie` (inclusief overschrijving in beide richtingen bij `gebaseerd op`, geldigheids- en conflictcontroles en semantic tokens).

Acceptance:
- **Schema & spec:**
  - `spec/schema/uchar-case-v0.schema.json` definieert `attributesEmpty` (optionele array van niet-lege strings) op `$defs.instance` direct na `attributes`, en werkt de beschrijving van `attributes` bij (verwijder de zin *"an input never carries an explicit empty"*).
  - `spec/RUNTIME-SEMANTICS.md` (ch. 1.5 `"The case"` en ch. 5.7b), `spec/ASP-TRANSLATION.md` (sectie S20) en `spec/CONSTRUCTS.md` documenteren `attributesEmpty`, inclusief gedrag naast een schrijver, naast alleen een initialisatie, naast `(volledig gegeven)`, in een gewoon document en bij monotonie.
- **Case-weigerregels (identiek in `ref`, `clingo` en `Drools`):**
  1. Niet-gedeclareerd attribuut op het objecttype van de instantie → `EF-040` (`EF-040.attribute`).
  2. Zelfde attribuut genoemd in zowel `attributes` als `attributesEmpty` (onder `Namen.norm`) → nieuwe foutcode `EF-048` (spiegel van `EF-047` met parameters `label` en `attribute`).
  3. Tijdsafhankelijk attribuut (`attribuutGranulariteit`) of gedimensioneerd attribuut (`dimensions`) in `attributesEmpty` → geweigerd bij het laden van de case, met de reden (net als een tijdsafhankelijk kenmerk in `characteristicsAbsent`).
- **`DroolsBackend.insert` / `Input.attributeEmpty`:**
  - `Input.attributeEmpty(bean, attribute)` / `Input.Empty` (spiegel van `Input.characteristicAbsent` / `Input.Absent`) lost de naam eerst onder `Namen.norm` op tot de gedeclareerde naam op de declarant (`CaseRunner.declaredAttribute`), weigert niet-in-invoer / niet-gedeclareerd / tijdsafhankelijk / gedimensioneerd / al gevuld op de bean (gelezen via `Accessors.leesEigenschap(bean, Namen.property(declared))`, dus ook veilig voor namen met spaties, streepjes, apostrof, hoofdletterwoorden zoals `"EU"`, begincijfers of superscripts — zelfde les als `absent-characteristic-property`), en haalt `ProducerConsumer.elementOf(declarant, declared, false)` uit `OpenInput`.
- **RegelSpraak-scenariolaag (`frontends/regelspraak/`):**
  - `- de <attr> is leeg` en `- het <attr> is leeg` in een `Situatie` (ook `gebaseerd op`):
    - schrijft `<attr>` in `attributesEmpty` van de case (in schemavolgorde: na `attributes`, vóór `roles`);
    - bij `gebaseerd op`: `- de X is leeg` heft een geërfde waarde van `X` op, en `- de X is <waarde>` heft een geërfde `is leeg` van `X` op (in beide richtingen, ook bij meerdere bases, zonder `EF-048`);
    - hetzelfde attribuut met zowel een waarde als `is leeg` in één blok (ook over twee openers van hetzelfde label) → eigen RGS-foutcode (spiegel van `RGS-465`);
    - `is leeg` met een periode (`van … tot …`), op een tijdsafhankelijk attribuut, of op een gedimensioneerd attribuut → eigen RGS-foutcode(s) (spiegel van `RGS-466` / `RGS-468`);
    - in een document zonder `Afleiden uit leeg: nee` → waarschuwing dat `is leeg` daar niets verandert (spiegel van `RGS-469`), terwijl de case het wel mag bevatten;
    - `SemanticTokens`: `leeg` als `keyword` en de attribuutnaam gebonden aan zijn declaratie;
    - afgebroken/foutieve regels breken recovery niet (geen NPE, zie `ScenarioRecoveryTest` / `SemanticTokensRecoveryTest`);
    - `frontends/regelspraak/grammar/DIVERGENCES.md` ("Scenariotoetsen") bijgewerkt.
- **Corpus, rood eerst en pariteit (`ref` = `clingo` = `Drools` exec-model = `Drools` DRL-tekst):**
  - Een nieuwe strikte Jutterswet-golden (met `source.rgs` en `scenario.rgs`, gate 10 en gate 16) die minimaal de volgende casussen dekt:
    1. invoerattribuut weggelaten → afhankelijke `is gevuld`-afwijking blijft open, uitkomst `attributeOpen`;
    2. invoerattribuut in `attributesEmpty` (`- de X is leeg`) → `is gevuld`-afwijking valt vaststaand af, lagere regel/variant beslist; ook `is leeg`-voorwaarde en `attributeEmpty`-verwachting slagen;
    3. invoerattribuut gevuld → afwijking beslist;
    4. afgeleid attribuut in `attributesEmpty`: naast open schrijver blijft open, naast niet-vurende schrijver vaststaand leeg, naast vurende schrijver wint de schrijver;
    5. attribuut met alleen een initialisatie in `attributesEmpty` → initialisatie komt aan de beurt (zoals `(volledig gegeven)`);
    6. attribuut met een spatie/streepje in de naam in `attributesEmpty` (naast unit-test voor lastige Java-eigenschapsnamen zoals in `AbsentCharacteristicNameWitnessTest`);
    7. `gebaseerd op`-overschrijving in beide richtingen (waarde → `is leeg` en `is leeg` → waarde).
  - Een gewone-document-golden (of casus) die bevestigt dat `attributesEmpty` daar geen uitkomst verandert.
  - Een rode casus onder `corpus/red/` voor een attribuut in zowel `attributes` als `attributesEmpty` (`EF-048`), rood in `ref`, `clingo` en `Drools`.
  - Gate 5 monotonie (`ref`): weglaten uit `attributesEmpty` laat het attribuut open en schendt monotonie niet.
- **Alle 17 gates groen (`make gates`)**, met bijgewerkte DRL-tekstpins en `EXPECTED_PLAYED` alleen waar nieuwe goldens dat vragen.

May touch:
- `spec/schema/uchar-case-v0.schema.json`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`
- `ref/` (`lib/case.ml`, `lib/world.ml`, `lib/eval.ml`, `codes.tsv` voor zover nodig, `bin/`, `test/`)
- `backends/clingo/` (`lib/`, `codes.tsv` voor zover nodig, `test/`, `README.md`)
- `backends/drools/` (`cases/`, `transpiler/`, `refusal/`, `message/`, `src/test/`)
- `frontends/regelspraak/` (`grammar/RegelSpraak.g4`, `grammar/DIVERGENCES.md`, `resolver/`, `ir/`, `emit/`, `diagnostic-codes.tsv`, `message/`, `src/test/`)
- `corpus/golden/...` (nieuwe golden(s)), `corpus/red/...` (nieuwe rode casus)
- `scripts/` (`check-case-schema.sh`, `check-oracle.sh`, `check-red.sh`, `check-drools.sh`, `check-drools-codes.sh`, `check-rgs-codes.sh`, `check-exec-codes.sh` waar zelf-tests, cataloguslijsten of pinnen meebewegen)
- `backlog/new/20261122-settled-empty-input.md` → `doing/` → `done/`

Out of scope:
- `regelspraak-toolchain` (HTTP-verzoek `{"state": "empty"}`, `OpenValues`, `RequestMapper`, `FixpointGivens`, `OpenApiEmitter`, `ScenarioData`, `SituationToRequest`, `playground.html`, `service_replay.py`: eigen vervolgkaart `20261125-settled-empty-input-in-service.md` na de uchar-pin)
- tijdsafhankelijke of gedimensioneerde attributen in `attributesEmpty` (bewust geweigerd, zoals `characteristicsAbsent` geen tijdlijnen draagt)
- parameterwaarden als vaststaand leeg (parameters zijn al niet open)
- de werkbank en externe regelsets

## Aanleiding (waarom nu ontparkeerd)

De geparkeerde kaart van 2026-11-22 noemde expliciet als voorwaarde om haar weer op te pakken: *"een aanvrager die zeker weet dat er niets is bij een attribuut"* (of een strikt antwoord `{"state": "empty"}` dat als verzoek naar een volgende regelset gaat).

Precies het eerste doet zich nu voor in een strikte regelset met een optionele datum op een aanvraag:
- Op een aanvraag is de aanvraagdatum ingevuld, maar een optionele gewenste aanvangsdatum is door de aanvrager bewust niet opgegeven (het vakje op het formulier is leeg gelaten).
- Een hogere afwijking toetst `de gewenste aanvangsdatum van de aanvraag is gevuld` boven een basisregel per kalendermaand.
- In strikte modus (`Afleiden uit leeg: nee`) laat het weglaten van het attribuut dat veld **open** (niet gegeven of er iets op het formulier stond), waardoor de hogere afwijking en de einduitkomsten open blijven staan.
- Het alternatief — het attribuut globaal `(volledig gegeven)` verklaren — vernietigt voor elke andere casus het onderscheid tussen *"nog niet uitgevraagd"* (open) en *"aanvrager liet het veld leeg"* (vaststaand leeg). Hetzelfde geldt voor andere optionele datums, zoals een nog lopende einddatum.
- Bovendien kan een scenario met `- de <attr> is leeg` in een `Situatie gebaseerd op ...:` een geërfde waarde gericht opheffen als compacte diff.

## Aandachtspunten en ontwerpdetails

- **Grammatica (`RegelSpraak.g4:206-211`, `233-243`):**
  `situatieWaarde` bevat bewust geen `LEEG` (*"`leeg` is NOT a situatieWaarde"*), en `verwachting` heeft al `# verwachtAttribuutLeeg` (`… IS LEEG`). Voeg aan `toewijzing` een eigen alternatief toe vóór `# attribuutToewijzing`:
  `| bullet bepaaldLidwoord toewijzingNaam IS LEEG periode? DOT? # attribuutLeegToewijzing`
  `LEEG` is al een lexer-token; `situatieWaarde` accepteert `LEEG` niet, `rolToOneToewijzing` vraagt `UPPER_ID`, en `kenmerkToewijzing` begint direct na de bullet met `IS | HEEFT`. De optionele `periode?` vangt `- de X is leeg van … tot …` af in de resolver met een duidelijke RGS-fout in plaats van een vage syntaxfout (spiegel van `RGS-466`). Let ook op een attribuut dat zelf `leeg` in de naam heeft of een cel/dimensie draagt!
- **Schemavolgorde (`uchar-case-v0.schema.json` en `CaseEmitter.java:96-140`):**
  `label`, `objectType`, `attributes`, `attributesEmpty` (alleen geschreven als niet leeg), `roles`, `characteristics`, `characteristicsAbsent`, `characteristicTimelines`.
- **`DroolsBackend.openInput` (`DroolsBackend.java:465-495`):**
  Dezelfde structuur als `input.absent()`: bouw per bean een set `ElementKey`s (`ProducerConsumer.elementOf(declarant, declared, false)`) en haal die van `incomplete` af. Controleer de bestaande waarde op de bean via `Accessors.leesEigenschap(e.bean(), Namen.property(declared)) != null` (en nooit met de ruwe gedeclareerde naam!). Weiger met een eigen `DroolsCode` (spiegel van `DROOLS-371` / `INSERT_ABSENT_CHARACTERISTIC`).
- **Geen verandering aan `EndState` nodig:**
  Een attribuut uit `attributesEmpty` is óf niet open (en staat niet in `EndState.open`), óf naast een open schrijver wel open met `given == null` — precies zoals `characteristicsAbsent` ook niet in `EndState.Open.given()` staat maar uit de invoer (`CaseInput` / `RequestMapper`) komt.
- **Vervolg in `regelspraak-toolchain`:**
  Zie de companion-kaart `20261125-settled-empty-input-in-service.md` (op te pakken zodra deze kaart op `uchar` `main` landt en in de toolchain gepind wordt).
