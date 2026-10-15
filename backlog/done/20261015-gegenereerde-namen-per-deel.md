# gegenereerde-namen-per-deel: een gegenereerde naam uit meer dan één bron heeft een benaming per deel

*Op verzoek van de eigenaar (2026-10-15; normbesluit: per deel terugvallen). Regelnummers op `56dc218b`.*

Goal: de kaart `gegenereerde-namen-in-elk-vocabulaire` gaf de zes elementen van de opgave-markering een benaming in
elk vocabulaire waarin het gemarkeerde attribuut er een heeft (`resolver/Benamingen.java:250`). Twee gegenereerde namen
liet zij zonder benaming, omdat hun naam uit meer dan één gedeclareerde naam bestaat (`grammar/DIVERGENCES.md`,
"Gegenereerde elementen", regel 3412 en verder):

- de groep `B voor S` (`uitzondering-voor-subtype`, `resolver/RuleChecker.java:1763`): de eigen uitzonderingen van
  subtype S onder basisregel B;
- het lid en de regel `<lid> via <rol 1>, … en <rol n>` (`stappen-na-de-verzameling`,
  `resolver/ExpressieBouwer.java:890`) van een vouw over een verzameling met stappen erna.

Het normbesluit van de eigenaar (2026-10-15): **per deel terugvallen.** Elk deel van de naam dat in een vocabulaire een
benaming heeft, wordt in de benaming van het element die benaming, letterlijk; elk ander deel blijft zijn canonieke
naam, gespeld zoals het in de naam staat; de vaste woorden (` voor `, ` via `, `, ` en ` en `) blijven staan. Voorbeeld
van de eigenaar: "Bedrag maandloon via dagloon en maandloon" als alleen het lid een benaming heeft. Heeft geen enkel deel
een benaming in een vocabulaire, dan krijgt het element daar geen. Dat volgt de keuze van de vorige kaart om geen
benaming te schrijven die de canonieke naam spelt (WF-120).

**Welke delen een benaming kunnen dragen** (IR-SPEC ch. 2.12, de lijst van declaraties die `designations` dragen, en de
zijtabel `RegelSpraakModel.benamingen`). B is een benoemde vorm: op de wire de defaultsgroep die de naam van de
basisregel draagt, met de benaming die op de basisregel geschreven is. S is een objecttype, het lid een attribuut, elke
rol een rol van een feittype. Alle vier soorten kunnen een benaming dragen. Een deel kan zelf een element zijn dat de
frontend maakte, en dan hangt het af van dat element: een rij van een beslistabel (als B) en het anker `startpunt
<tijdlijn>` (als lid) dragen nooit een benaming (de vorige kaart, Out of scope), dus zo'n deel blijft altijd canoniek;
`opgegeven X` en `berekende X` (als lid) dragen de benaming die de vorige kaart samenstelt, en die wordt dan het deel.
De vaste woorden zijn geen declaratie en blijven altijd staan.

**Waar de benaming leeft**: zoals bij de vorige kaart in de zijtabel van het model en daarmee op de wire, als
`designations` van de `PrioritizedDefaults` `B voor S` en van de `Attribute` en de `Rule` `<lid> via …`. Een lezer van
de wire kan haar niet samenstellen: de delen staan er niet op. Wire, schema, verifier en WF-116 tot WF-120 veranderen
niet; `spec/IR-SPEC.md` verandert niet.

**In een standaardvocabulaire** draagt elk deel dat een benaming kan dragen er een, of het wordt zelf onder WF-119
geweigerd; een deel dat er nooit een draagt, is een element dat de frontend zelf schreef, en ook dat weigert WF-119. Een
gemengde benaming staat daarom nooit in een toegelaten document met een standaardvocabulaire.

Acceptance:
- (a) Na het resolven heeft de groep `B voor S` in elk vocabulaire waarin B of S een benaming heeft, de benaming
  `<B of zijn benaming> voor <S of zijn benaming>`; het lid en de regel `<lid> via <rol 1>, … en <rol n>` hebben in
  elk vocabulaire waarin het lid of een rol een benaming heeft, de naam met elk benoemd deel vervangen door zijn
  benaming. Zonder benaming van enig deel heeft het element er geen, en is zijn effectieve naam daar zijn canonieke
  naam. In een vocabulaire dat niet het standaardvocabulaire is, wordt een samengestelde benaming die de canonieke naam
  byte voor byte spelt niet geschreven (WF-120). De opgave-familie houdt haar gedrag.
- (b) De emitter schrijft de samengestelde benamingen als `designations` van de `PrioritizedDefaults`, de `Attribute`
  en de `Rule`. De enige golden met vocabulaires, `jutterswet-vocabulaires`, heeft geen groep van een subtype en geen
  vouw met stappen na de verzameling, dus gate 10, 11 en 16 laten zien dat elke `ir.json` en elk case-bestand
  bytegelijk blijft.
- (c) Een samengestelde benaming die binnen haar reikwijdte botst met een andere effectieve naam in hetzelfde
  vocabulaire, geeft de bestaande WF-118-fout. De zin noemt de benamingen waaruit de frontend haar samenstelde, en
  staat op de regel van het eerste deel met een bronregel (B, of het lid). In een standaardvocabulaire waarin geen deel
  een benaming heeft, krijgen de groep en het lid een WF-119-fout die hun delen noemt, op diezelfde regel (was: regel
  0, "de frontend schreef deze declaratie zelf").
- (d) Nieuwe testklasse `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/GegenereerdeNamenPerDeelTest.java`,
  eerst rood, met beide vormen: alle delen met een benaming, een deel zonder, geen enkel deel, en een botsing onder
  WF-118; daarnaast het standaardvocabulaire, in het model en op de wire.
- (e) `frontends/regelspraak/grammar/DIVERGENCES.md` beschrijft de regel in "Gegenereerde elementen" en "Vocabulaires
  en benamingen", met welke delen een benaming kunnen dragen en welke altijd canoniek blijven, en verwijst ernaar vanuit
  §4 ("The inheritance line") en de vouw met stappen na de verzameling.
- (f) Gemeten op de v2-regelset van de RegelSpraak-werkbank (fundament en herleving-ww, alleen gelezen, als kopie door
  de frontend van deze branch): welke `B voor S` en welke `… via …` er een benaming krijgen, en welke.

May touch: `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Benamingen.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/RuleChecker.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Resolver.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/model/RegelSpraakModel.java` (alleen Javadoc, als nodig),
`frontends/regelspraak/grammar/DIVERGENCES.md`, de nieuwe testklasse, deze kaart.

Out of scope:
- Bestaande tests en goldens. `GegenereerdeBenamingenTest.de_andere_gegenereerde_vormen_krijgen_geen_benaming` legt
  het gedrag van vóór deze kaart vast (de groep en het lid zonder benaming, terwijl hun delen er een hebben) en valt
  door deze kaart. Op aanwijzing van de eigenaar blijft dat bestand ongewijzigd; de kaart levert een patch ernaast, en
  het Result noemt hem.
- `beslistabelrij`, `startpunt`, `defaultsstapel` en `versie`: ongewijzigd, met de redenen van de vorige kaart.
- `spec/IR-SPEC.md`, het schema en de verifier (geen nieuw veld en geen nieuwe regel); `spec/CONSTRUCTS.md` en
  `spec/PLAN.md`.
- De toolchain en de werkbank: die volgen na een pin-bump, op hun eigen kaart.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch, met de patch op de bestaande test
toegepast; zonder die patch valt gate 10 op precies die ene test.

## Result (2026-10-15)

`d6944ab3` geeft de groep `B voor S` en het lid en de regel `<lid> via <rol 1>, … en <rol n>` een benaming per deel,
in elk vocabulaire waarin ten minste één deel er een heeft; `27ae916b` legde de regel eerst vast in
`grammar/DIVERGENCES.md`. Geen wijziging van wire, schema, verifier of `spec/IR-SPEC.md`: de frontend schrijft
`designations` op declaraties die er al een mogen dragen.

- **Code**:
  - `resolver/RuleChecker.java`: `gegenereerdeElementen` geeft per groep en per lid en regel van een vouw de stukken
    van de naam door (`naamdelen()`): B en S met ` voor ` ertussen; het lid zoals gedeclareerd en per stap de rol die
    de emitter voor die stap bindt (`IrEmitter.navigatieBindingen` vanaf het type waarop het lid staat), met ` via `,
    `, ` en ` en `. De groep onthoudt daarvoor haar subtype; de lezing van de gegenereerde regel staat nu in
    `hulpwaarde`, die `hulpregels` en de stukken delen. Elk deel wordt gespeld zoals `benoembaar()` het spelt. Een deel
    dat geen element van het model is, of een stap zonder rol, geeft in een model zonder ERROR een
    `IllegalStateException`.
  - `resolver/Benamingen.java`: `stelSamen(naamdelen)` stelt per vocabulaire de naam samen uit de stukken, met elk
    benoemd deel vervangen door zijn benaming. Zonder benoemd deel komt er geen benaming, en in een vocabulaire dat
    niet het standaardvocabulaire is ook geen die de canonieke naam spelt (WF-120). De opgave-familie loopt door
    dezelfde samenstelling, als naam van één deel, en gaat eerst, omdat haar leden deel van een andere naam kunnen
    zijn. De WF-118-zin eindigt op "; de frontend stelde de benaming van … samen uit de benaming '…' van …, … en …"
    (de benoemde delen); de WF-119-zin van een naam uit meer delen op "; de frontend stelt de benaming van deze
    declaratie samen uit die van <deel 1>, … en <deel n>, en geen daarvan draagt er een". Beide staan op de regel van
    het eerste deel dat de bron declareert (was regel 0 voor de groep en het lid). De zinnen van de opgave-familie
    zijn ongewijzigd.
  - `resolver/Resolver.java` geeft de stukken van de regelcontrole door; de Javadoc van
    `RegelSpraakModel.vocabulaires` noemt de samenstelling per deel.
- **Tekst**: `grammar/DIVERGENCES.md`, "Gegenereerde elementen" (nieuwe alinea's "Part by part", welke delen een
  benaming kunnen dragen, het standaardvocabulaire; de twee uitsluitingen zijn weg), "Vocabulaires en benamingen"
  (Lowering, de WF-118- en WF-119-zinnen, Scope), en een verwijzing in §4 ("The inheritance line") en in "A fold over
  a set and to-one steps after it".

Welke delen een benaming kunnen dragen: B (een benoemde vorm, op de wire de defaultsgroep met de naam en de benaming
van de basisregel), S (een objecttype), het lid (een attribuut) en elke rol (de rol van het feittype van haar stap);
alle vier zijn soorten die IR-SPEC ch. 2.12 `designations` geeft. Altijd canoniek: de vaste woorden, een rij van een
beslistabel als B en het anker `startpunt <tijdlijn>` als lid; die twee dragen nooit een benaming. `opgegeven X` of
`berekende X` als lid draagt de benaming die de vorige kaart samenstelt, en die wordt het deel.

Gedragsverandering: in een document met vocabulaires dragen de groep en het lid met zijn regel benamingen waar hun
delen er hebben, ook op de wire, en zo'n benaming kan onder WF-118 botsen. Een standaardvocabulaire weigert de groep
en het lid niet meer als een deel er een benaming heeft (was: WF-119 op regel 0, "de frontend schreef deze declaratie
zelf"); een deel zonder benaming wordt daar zelf geweigerd, dus een gemengde benaming komt in een toegelaten document
met een standaardvocabulaire niet voor.

### Acceptatie

- (a) en (b) Gehaald; `GegenereerdeNamenPerDeelTest` toont model en wire. De wire van de twee bronnen van de
  testklasse met alle delen benoemd is verifier-schoon (gemeten, niet als bestand bewaard).
- (c) Gehaald: de botsingen en de WF-119-zinnen in de testklasse.
- (d) Nieuw: `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/GegenereerdeNamenPerDeelTest.java`, 5 tests,
  elk voor de groep `meldtermijn voor Strandjutter` en het lid en de regel `gewicht via vondst en weging` (twee
  stappen; de rol `vondst` van een tweede feittype draagt eigen benamingen, zodat de test de rol van de stap toetst):
  alle delen met een benaming (in twee vocabulaires, terug via `benoemd`, op de wire als `designations` van de
  `PrioritizedDefaults`, de `Attribute` en de `Rule`); een deel zonder benaming (`termijn van melding voor
  Strandjutter`, `massa via vangst en weging`, en alleen het lid: `Bedrag gewicht via vondst en weging`); geen enkel
  deel (geen benaming, canonieke effectieve naam, niets op de wire; een benaming die haar deel spelt geeft geen
  benaming en één WF-120-waarschuwing); een botsing onder WF-118 voor elk van beide (met een geschreven regel, op de
  regel van B; met de benaming van een geschreven attribuut, op de regel van het lid, de zin met drie delen); het
  standaardvocabulaire (volledig: schoon; B zonder benaming: alleen B geweigerd, de groep draagt `meldtermijn voor
  STRANDJUTTER`; geen deel: drie WF-119-fouten, die van de groep met haar delen op de regel van B). Rood vóór de
  code: 4 van de 5 vielen (`expected: <termijn van melding voor strandloper> but was: <->`; voor het
  standaardvocabulaire de WF-119-fout op regel 0); de test zonder benamingen was groen, want hij legt bestaand gedrag
  vast. Vier tijdelijke mutaties lieten tests vallen: de rollen niet als deel (3 tests), ook samenstellen zonder
  benoemd deel (de standaardtest), de regel van het element zelf in plaats van die van het eerste deel (2 tests), de
  rol van het feittype van de laatste stap voor elke stap (4 tests).
- (e) Gehaald.
- (f) Gemeten op de v2-regelset van de RegelSpraak-werkbank (`regelspraak-rules` op `e7d825f`, fundament en
  herleving-ww, 33 bronnen, als kopie): één groep `B voor S`, `herlevingstermijn art. 21 lid 3 en 5 voor
  RechtOpUitkeringWwBijOntslag`. B heeft in geen vocabulaire een benaming; S heet in cgm `Recht op uitkering WW bij
  ontslag` en heeft in handboek geen. Na de kaart draagt de groep in cgm `herlevingstermijn art. 21 lid 3 en 5 voor
  Recht op uitkering WW bij ontslag` en in handboek niets. Geen `… via …`: de regelset heeft geen vouw met stappen na
  de verzameling. De IR van deze branch verschilt van die van `56dc218b` alleen in die ene `designations`-entry, draagt
  geen diagnostiek en is verifier-schoon; de 42 elementen van de opgave-familie houden hun benamingen.

Bestaande test: `GegenereerdeBenamingenTest.de_andere_gegenereerde_vormen_krijgen_geen_benaming` legt het gedrag van
vóór de kaart vast en valt (`expected: <-> but was: <termijn van melding voor strandloper>`). Het bestand is op de
branch ongewijzigd. De patch is buiten de repository aan de eigenaar geleverd: hij hernoemt de test naar
`de_groep_van_een_subtype_en_het_lid_van_een_vouw_krijgen_een_benaming_per_deel` en verwacht `termijn van melding
voor strandloper` voor de groep en `massa via vangst` voor het lid en de regel `gewicht via vondst`. Geen golden
gewijzigd.

Gates. Met de patch toegepast (ongecommitteerd op `d6944ab3`, de boom `758471d3`) alle 16 groen, 15:02:44–15:22:06;
deze commit raakt daarna alleen `backlog/`:
1 PASS; 2 348 bestanden valideren en round-trippen, 854 documenten in sleutelvolgorde; 3 348 golden schoon, 506
negatief afgewezen, 349 op hun arm; 4 873 case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite 2198 checks, 823
cases; 7 778 cases, 45 survivors; 8 50 rode cases; 9 241 checks, 12 catala-goldens; 10 JUnit 1190 tests (was 1185,
plus de 5 nieuwe), 125 rgs-goldens bytegelijk; 11 45 TOKA-bronnen bytegelijk, TOKA-suite 27 tests, 29 TOKA-cases op
ref, clingo en Drools; 12 255 gespeeld op beide takken, JUnit 1578; 13 43 twists over 24 goldens, 122 replays,
vocabulaireleg 18 replays (46 gestript, 36 geroteerd); 14 150 checks, 2 goldens; 15 266 van 303 goldens vergeleken,
125 op de bronroute, 69 unittests; 16 30 scenario-goldens, 102 case-bestanden en hun `ir.json` bytegelijk.
Zonder de patch, op `d6944ab3`: gate 10 rood, 1190 tests, 1189 groen, 1 gefaald, precies de test hierboven.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: geen construct, wire, schema of
case-formaat verandert.

Open:
- De patch op `GegenereerdeBenamingenTest` moet vóór of met de merge landen; zonder hem is gate 10 rood.
- `stelSamen` stelt de namen uit meer delen samen in de volgorde van `benoembaar()`. Is het lid van een vouw zelf het
  lid van een andere vouw, op een type dat eerder gedeclareerd is, dan is diens benaming nog niet samengesteld en
  blijft dat deel canoniek. Daarvoor moet een regel de gegenereerde naam zelf lezen; geen bron in het corpus of de
  v2-regelset doet dat, en de kaart bouwt er niets voor.
- De distributie (regelspraak-toolchain) en de werkbank nemen de benamingen over na een pin-bump, op hun eigen kaart.
