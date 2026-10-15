# gegenereerde-namen-in-elk-vocabulaire: een element dat de frontend zelf maakt, heeft een benaming in elk vocabulaire waarin zijn bron er een heeft

*Op verzoek van de eigenaar (2026-10-15; besluit: optie C, in uchar). Regelnummers op `99f614b8`.*

Goal: met de opgave-markering op attribuut X van T maakt de RegelSpraak-frontend de attributen `opgegeven X` en
`berekende X`, het kenmerk `afwijkende opgave X` en de vormen `afwijking opgave X`, `controle opgave X` en `opgave boven
berekening X` (`resolver/RuleChecker.java:2394`–`2400`, `grammar/DIVERGENCES.md` §7). Die zes hebben in geen
vocabulaire een benaming: een annotatie heeft er geen plaats, en `Benamingen` leest alleen annotaties
(`resolver/Benamingen.java:176`). Wie het model of de wire in een vocabulaire leest, ziet de benaming van X naast de
canonieke naam van wat eruit voortkomt, en legt het verband niet. Voorbeeld uit de RegelSpraak-werkbank: het attribuut
`beëindigingsdatum` heet in het vocabulaire cgm "Datum einde recht op uitkering", en de uitleg in cgm zegt "R1 heeft
geen afwijkende opgave beëindigingsdatum, want niet geldt: zijn opgegeven beëindigingsdatum (onbekend) gevuld is". In
een standaardvocabulaire weigert WF-119 de markering zelfs, omdat de zes elementen er geen benaming hebben
(`Benamingen.java:254`, "de frontend schreef deze declaratie zelf, zonder plaats voor een annotatie").

De kaart geeft elk van de zes elementen, in elk vocabulaire waarin X een benaming heeft, de benaming die ontstaat als in
zijn naam X vervangen wordt door die benaming. De benaming van X wordt letterlijk overgenomen, ook met een hoofdletter
midden in de naam: "opgegeven Datum einde recht op uitkering", "afwijkende opgave ingangsdatum van de eindiging"
(normvraag 1 van de eigenaar). Heeft X in een vocabulaire geen benaming, dan heeft het element daar ook geen, en is zijn
effectieve naam daar zijn canonieke naam, zoals nu (normvraag 2).

**Waar de benaming leeft: in de zijtabel `benamingen` van het model, en daarmee op de wire.** De zes elementen zijn
declaraties in het IR-document: twee `Attribute`, een `Characteristic`, een `Rule`, een `Consistency` en een
`PrioritizedDefaults` (§7, "Emission"). IR-SPEC ch. 2.12 geeft elk van die knopen `designations`, en de emitter schrijft
die uit `RegelSpraakModel.benamingen` (`emit/IrEmitter.java:2053`). Een benaming die alleen in het model stond, zou de
twee routes naar een model uit elkaar trekken: de Drools-IR-loader leest `benamingen` van de wire, en de distributie
bedient beide routes. Een consument van de wire kan de benaming ook niet zelf samenstellen: de herkomst
(`RegelSpraakModel.gegenereerd`) staat niet op de wire. En de verifier toetst WF-118 en WF-119 op de wire, dus een
botsing en de volledigheid van een standaardvocabulaire worden dan over dezelfde namen getoetst als in de frontend. De
wire, het schema en de regels WF-116 tot WF-120 veranderen niet: de frontend schrijft meer benamingen op declaraties die
er al een mogen dragen. `spec/IR-SPEC.md` verandert daarom niet; de producentenregel staat in `grammar/DIVERGENCES.md`.

Acceptance:
- (a) Na het resolven heeft elk element dat `RegelSpraakModel.gegenereerd` met de sleutel `opgave-boven-berekening` uit
  het attribuut X noemt, in elk vocabulaire v waarin X de benaming b heeft, de benaming `<vaste woorden> b`
  (`RegelSpraakModel.benaming(v, element)`), b letterlijk. In een vocabulaire waarin X geen benaming heeft, heeft het
  element er geen, en `effectieveNaam` geeft daar de canonieke naam. In een vocabulaire dat niet het
  standaardvocabulaire is, wordt een samengestelde benaming die de canonieke naam van het element byte voor byte spelt
  niet geschreven: de effectieve naam is zonder haar dezelfde, en op de wire zou zij een melding vragen (WF-120).
- (b) De emitter schrijft de samengestelde benamingen als `designations` van de gegenereerde attributen, het kenmerk en
  de drie vormen. Geen golden combineert de markering met een vocabulaire, dus gate 10, 11 en 16 laten zien dat elke
  `ir.json` en elk case-bestand bytegelijk blijft.
- (c) Een samengestelde benaming die binnen haar reikwijdte botst met een andere effectieve naam in hetzelfde
  vocabulaire, geeft de bestaande WF-118-fout; de zin noemt dan de benaming waaruit de frontend haar samenstelde, en
  staat op de regel van het gemarkeerde attribuut. Twee gemarkeerde attributen op twee objecttypen met één benaming in
  één vocabulaire botsen zo met hun gegenereerde vormen, die één naamruimte delen (WF-020), zoals hun canonieke vormen
  botsen bij twee attributen van één naam (§7). Een standaardvocabulaire met de markering is volledig als X er een
  benaming heeft; heeft X er geen, dan zegt de WF-119-fout van elk gegenereerd element dat zijn benaming uit die van X
  komt, op de regel van X. Nieuw negatief corpusgeval `corpus/negatief/wf118-effectieve-naam-vormen`: de wire van zo'n
  botsing tussen gegenereerde vormen. De reikwijdte van de benoemde vormen had in het negatieve corpus nog geen geval.
- (d) Nieuwe testklasse `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/GegenereerdeBenamingenTest.java`,
  eerst rood: een attribuut met benamingen in twee vocabulaires en de markering, waarbij elk van de zes elementen de
  samengestelde benaming krijgt, in het model en op de wire; een bron zonder benaming in één vocabulaire, waar de
  elementen dan geen benaming hebben; een botsing tussen twee typen en een botsing met een geschreven attribuut, elk
  met een diagnose; een standaardvocabulaire met en zonder benaming van X; een overbodige benaming van X die niet
  wordt samengesteld; de vormen buiten de kaart zonder benaming.
- (e) `frontends/regelspraak/grammar/DIVERGENCES.md` beschrijft de regel in "Vocabulaires en benamingen", §7 en
  "Gegenereerde elementen", met de vormen die geen benaming krijgen en de reden.

May touch: `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Benamingen.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Resolver.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/model/RegelSpraakModel.java` (alleen Javadoc, als nodig),
`frontends/regelspraak/grammar/DIVERGENCES.md`, de nieuwe testklasse
`frontends/regelspraak/src/test/java/dev/uchar/regelspraak/GegenereerdeBenamingenTest.java`, de nieuwe map
`corpus/negatief/wf118-effectieve-naam-vormen/`, deze kaart.

Out of scope:
- De andere vormen van `Herkomstvorm`, elk met de reden:
  - `uitzondering-voor-subtype`, de groep `B voor S`: haar naam bestaat uit twee namen, die van de basisregel B (de
    bron in de tabel) en die van het subtype S, die elk een eigen benaming kunnen hebben. Welke benaming de groep
    krijgt als B er in een vocabulaire een heeft en S niet, of andersom, beslist normvraag 2 niet; "<benaming van B>
    voor S" zou in één naam een benaming en een canonieke naam mengen, wat WF-119 voor een standaardvocabulaire juist
    uitsluit. Een normvraag voor de eigenaar.
  - `stappen-na-de-verzameling`, het lid en de regel `<lid> via <rol 1>, … en <rol n>`: dezelfde reden, met het lid en
    elke rol als naamdelen.
  - `beslistabelrij`: de bron is de beslistabel, en een beslistabel draagt geen benaming (de frontend weigert er een,
    `Benamingen.java:70`). Normvraag 2 geeft dan geen benaming: de uitkomst is die van de regel.
  - `startpunt`, het anker `startpunt <tijdlijn>`: de naam is gemaakt uit de naam van de tijdlijn, niet uit die van de
    bron (de regel die het anker schrijft), en een tijdlijn draagt geen benaming (IR-SPEC ch. 2.12).
  - `defaultsstapel` en `versie`: namen in de dekking van de Drools-backend, geen declaraties; geen vocabulaire noemt
    ze.
- `spec/IR-SPEC.md`, het schema en de verifier (zie boven: geen nieuw veld en geen nieuwe regel); `spec/CONSTRUCTS.md`.
- De toolchain en de werkbank: die volgen na een pin-bump, op hun eigen kaart.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch; gate 3 met het nieuwe negatieve geval.

## Result (2026-10-15)

`b947d470` geeft de zes elementen van de opgave-markering een benaming in elk vocabulaire waarin het gemarkeerde
attribuut er een heeft; `22b17492` legde de regel eerst vast in `grammar/DIVERGENCES.md`. Geen wijziging van wire,
schema, verifier of `spec/IR-SPEC.md`: de frontend schrijft `designations` op declaraties die er al een mogen dragen.

- **Code** (`resolver/Benamingen.java`, `resolver/Resolver.java`): de nieuwe stap `stelSamen()` draait tussen de
  regelcontrole en `keur()`. Hij leest uit `RegelSpraakModel.gegenereerd` elk element met de sleutel
  `opgave-boven-berekening` en zijn bron X, en zet per vocabulaire waarin X de benaming b heeft `<vaste woorden> b` in
  `RegelSpraakModel.benamingen`; zonder benaming van X niets. In een vocabulaire dat niet het standaardvocabulaire is,
  wordt een samengestelde benaming die de canonieke naam van het element spelt niet geschreven (WF-120). Een element
  dat niet naar zijn bron heet, of dat in een model zonder ERROR al een benaming draagt, geeft een
  `IllegalStateException`. `keur()` beoordeelt de samengestelde benamingen zoals de geschreven: een WF-118-zin over
  een samengestelde benaming eindigt per samengestelde benaming op "; de frontend stelde de benaming van … samen uit
  de benaming '…' van het attribuut '…' van '…'", en een WF-119-zin over een gegenereerd element op "; de frontend
  stelt de benaming van deze declaratie samen uit die van het attribuut '…' van '…', en dat draagt er geen"; beide op
  de regel van X (was regel 0). De emitter is niet veranderd: hij schrijft `benamingen` al per declaratie. De Javadoc
  van `RegelSpraakModel.vocabulaires` noemt de samenstelling.
- **Tekst**: `grammar/DIVERGENCES.md`, "Vocabulaires en benamingen" (Lowering, de WF-118- en WF-119-zinnen, Scope),
  §7 (Emission, Scope) en "Gegenereerde elementen" (nieuwe alinea "Designations", met de vormen die geen benaming
  krijgen).
- **Corpus**: `corpus/negatief/wf118-effectieve-naam-vormen`, de wire van twee gemarkeerde attributen op twee typen
  met één benaming in `handboek`; de verifier geeft drie WF-118-regels, één per gegenereerde vorm. Dezelfde bron met
  verschillende benamingen is verifier-schoon (gemeten, niet als bestand bewaard).

Welke vormen een benaming krijgen: de zes van `opgave-boven-berekening` (`opgegeven X`, `berekende X`, `afwijkende
opgave X`, `afwijking opgave X`, `controle opgave X`, `opgave boven berekening X`; de drie vormen alleen waar de
markering gelowerd is). Geen benaming, met de reden in DIVERGENCES: `uitzondering-voor-subtype` en
`stappen-na-de-verzameling` (een naam uit meer dan één bron; welke benaming bij een gedeeltelijk benoemde naam hoort,
is een normvraag), `beslistabelrij` (de tabel draagt geen benaming), `startpunt` (de naam komt van de tijdlijn, die
geen benaming draagt), `defaultsstapel` en `versie` (geen declaraties).

Gedragsverandering: een document met een standaardvocabulaire en de markering wordt niet meer geweigerd als X er een
benaming heeft (was: zes WF-119-fouten op regel 0, "de frontend schreef deze declaratie zelf"). Twee gemarkeerde
attributen op twee typen met één benaming in één vocabulaire worden nu geweigerd onder WF-118 (hun gegenereerde
vormen delen een naamruimte); vóór de kaart werden ze toegelaten.

### Acceptatie

- (a) en (b) Gehaald; `GegenereerdeBenamingenTest` toont het model en de wire. Gemeten op de v2-regelset herleving-ww
  van de RegelSpraak-werkbank (33 bronnen, alleen gelezen): de frontend van deze branch resolvet haar zonder
  diagnostiek, en `opgegeven beëindigingsdatum` draagt `opgegeven Datum einde recht op uitkering` (cgm) en `opgegeven
  ingangsdatum van de eindiging` (handboek); `opgegeven maxdatum` draagt `opgegeven Datum einde basisuitkering WW bij
  ontslag` (cgm) en in handboek niets, want de maxdatum heeft daar geen benaming.
- (c) Gehaald; de botsingen en de WF-119-zinnen in de testklasse, de wire in het negatieve geval.
- (d) Nieuw: `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/GegenereerdeBenamingenTest.java`, 7 tests:
  benamingen in twee vocabulaires, met een hoofdletter midden in de naam, in het model, terug via `benoemd` en op de
  wire; een bron zonder benaming in één vocabulaire en een bron zonder benaming; twee typen met één benaming (drie
  WF-118-fouten op de regel van het tweede attribuut); een botsing met een geschreven attribuut `opgegeven melddag`;
  een standaardvocabulaire met en zonder benaming van X (zeven WF-119-fouten op de regel van X); een overbodige
  benaming die niet wordt samengesteld, en een die alleen in hoofdletters verschilt en wel; de groep `B voor S` en het
  lid van een vouw zonder benaming. Rood vóór de code: 6 van de 7 vielen (`expected: <[opgegeven Datum melding vondst,
  …]> but was: <[-, -, -, -, -, -]>`; voor het standaardvocabulaire de zes WF-119-fouten op regel 0); de grenstest
  van de andere vormen was groen, want hij legt bestaand gedrag vast. Drie tijdelijke mutaties lieten tests vallen:
  de WF-120-uitzondering weg (de overbodige benaming), de regel van het element zelf in plaats van die van X (twee
  tests, regel 0), de canonieke naam in plaats van de benaming (zes tests). Geen bestaand testbestand gewijzigd, geen
  golden gewijzigd.
- (e) Gehaald. Alle 16 gates groen op `b947d470` (de boom `9c601ede`), 14:14:21–14:32:08; deze commit raakt daarna
  alleen `backlog/`:
  1 PASS; 2 348 bestanden valideren en round-trippen, 854 documenten in sleutelvolgorde; 3 348 golden schoon, 506
  negatief afgewezen (was 505), 349 op hun arm; 4 873 case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite
  2198 checks, 823 cases; 7 778 cases, 45 survivors; 8 50 rode cases; 9 241 checks, 12 catala-goldens; 10 JUnit 1185
  tests (was 1178, plus de 7 nieuwe), 125 rgs-goldens bytegelijk; 11 45 TOKA-bronnen bytegelijk, TOKA-suite 27
  tests, 29 TOKA-cases op ref, clingo en Drools; 12 255 gespeeld op beide takken, JUnit 1578; 13 43 twists over 24
  goldens, 122 replays, vocabulaireleg 18 replays (46 gestript, 36 geroteerd); 14 150 checks, 2 goldens; 15 266 van
  303 goldens vergeleken, 125 op de bronroute, 69 unittests; 16 30 scenario-goldens, 102 case-bestanden en hun
  `ir.json` bytegelijk.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: geen construct, wire, schema of
case-formaat verandert.

Open:
- Normvraag voor de eigenaar: welke benaming krijgen de groep `B voor S` en het lid `<lid> via <rollen>` als hun
  naamdelen in een vocabulaire maar ten dele een benaming hebben? Tot die vraag beslist is, krijgen ze er geen, en
  blijft een standaardvocabulaire ze weigeren.
- De distributie (regelspraak-toolchain) en de werkbank nemen de benamingen over na een pin-bump, op hun eigen kaart;
  tot dan tonen de playground en de uitleg van de werkbank de canonieke namen van de gegenereerde elementen.
