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
