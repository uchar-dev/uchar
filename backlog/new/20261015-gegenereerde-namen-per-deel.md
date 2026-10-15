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
