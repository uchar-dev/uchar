# vocabulaires-benamingen: benoemde vocabulaires met een benaming per declaratie, als zijtafel zonder semantiek

De begrippenkaart van een uitvoeringsorganisatie kent per begrip een naam
per laag (wet, handboek, bedrijfsregels, canoniek gegevensmodel). Een
regelset spreekt één taal — de gedeclareerde namen — maar de dienst, de
uitleg en de koppelingen willen de andere lagen kunnen spreken, en de
vertaling daartussen moet ondubbelzinnig zijn. Maintainersbesluit
(2026-09-27): dat wordt een taalkenmerk van uchar, geen commentaarconventie
en geen mappingbestand. Prior art (research/vocabulaires-2026-09-27/
prior-art.md, 2026-09-27): het patroon bestaat — ISO 1087 (designations),
SBVR (designations per speech community), OMG MVF 1.1 (vocabulaire per
gemeenschap, actief vocabulaire, vertaling), XBRL met de EFM-regels (één
naam per element per vocabulaire, verplicht standaardvocabulaire,
uniciteit), Gellish, SNOMED (taal- en dialectreferentiesets, keuze per
verzoek) — en wat uchar toevoegt is de combinatie: benamingen als zuivere
metadata op alle declaratiesoorten van een normatieve IR, uniciteit als
verifierregel, en dezelfde vocabulaires voor een dienstcontract en voor
uitleg. Woorden: **vocabulaire** voor de verzameling, **benaming** voor de
afzonderlijke naam (Engels: vocabulary, designation); niet "dialect"
(uchars laag boven de kern), "label" (dimensielabels) of "alias"
(resolverend in SQL, Ada, L4).

## U-besluiten (hoofdsessie, 2026-09-27)

- U1 Syntaxis (RegelSpraak-frontend, uchars dialect, additief en
  strippbaar): een declaratieblok naast de eenhedenstelsels en dagsoorten,
  `Vocabulaire <naam>` met optioneel `(standaard)` en optioneel een
  taalcode en een soort (`code` of `term`; standaard `term`); op een
  declaratie een annotatie `(<vocabulaire>: <benaming>)` na de naam en na
  `(mv: …)`, voor objecttype, attribuut, kenmerk, feittype, rol,
  enumeratiewaarde, parameter, regel en regelgroep; hooguit één benaming
  per declaratie per vocabulaire; een benaming van een code-vocabulaire
  voldoet aan een tekenpatroon dat in OpenAPI en JSON geldig is (de bouwer
  kiest en documenteert het); een document zonder vocabulaires parseert
  en emitteert byte-gelijk als nu. Regels blijven in de canonieke naam
  geschreven: een benaming resolveert NOOIT in regeltekst, ook niet als
  ze toevallig een canonieke naam spelt.
- U2 IR (`uchar-ir-v1`, L5-spelling): op het document `vocabularies`
  (naam, `default`, `language`, `kind`) en per declaratie `designations`
  (vocabulaire → benaming), als zijtafel zonder semantiek; sleutelvolgorde
  canoniek in het schema; ref en clingo lezen en negeren (nooit een
  weigering); de Drools-lader draagt de tafel in het model met een
  publieke leesingang (vocabulaire → benaming van een element, en de
  omkering), zonder wijziging aan beans, DRL of exec-model.
- U3 Verifier, nieuwe WF-regels met arm per oorzaak (nummers na WF-115):
  een benaming in een niet-gedeclareerd vocabulaire; twee benamingen voor
  hetzelfde vocabulaire op één declaratie; uniciteit van de EFFECTIEVE
  naam per vocabulaire (benaming als die er is, anders de canonieke naam)
  met dezelfde reikwijdte als de canonieke namen: attributen en kenmerken
  per objecttype, rollen per feittype, enumeratiewaarden per domein, de
  overige soorten globaal — zodat de omkering per vocabulaire
  deterministisch is, ook voor een onvolledig vocabulaire; een vocabulaire
  met `standaard` is volledig (elke declaratie heeft er een benaming;
  ERROR), twee standaardvocabulaires is een fout; een benaming die de
  canonieke naam van hetzelfde element spelt is toegestaan maar overbodig
  (WARNING).
- U4 Neutraliteit: een been in gate 13 (annotatie-onverschilligheid) naar
  het bestaande model: elke golden met vocabulaires wordt gespeeld met de
  benamingen gestript en met de benamingen verwisseld, en ref, clingo en
  beide Drools-takken geven de gepinde uitkomsten; een lege set is een
  fout; vloer in het script. Voorstel van de bouwer, landing door de
  hoofdsessie (beschermd pad).
- U5 Getuigen: een Jutterswet-golden met de vocabulaires `handboek`
  (term) en `cgm` (code, standaard) op alle declaratiesoorten inclusief
  enumeratiewaarden, rollen en regels; negatieve getuigen per arm van U3
  (`corpus/negatief/wf###-…`, met `expect.txt`); frontendtests voor de
  syntaxis en de weigeringen; `bron.rgs` re-emitteert byte-gelijk (gate
  10); de pariteitszone en de bestaande goldens onveranderd.
- U6 Register: DIVERGENCES.md sectie "Vocabulaires en benamingen"; IR-SPEC
  (hoofdstuk 2: de zijtafel; de WF-rijen) en het schema als diff van de
  bouwer, geland door de hoofdsessie; CONSTRUCTS-rij en PLAN §9-zin in
  het Result (de hoofdsessie landt ze).
- U7 Niet in deze kaart, alleen benoemen: de dienst per vocabulaire (kaart
  in de distributie), de lint tegen de begrippenkaart (werkbank), een
  deprecatiestatus per benaming en meertaligheid (later), een
  strip-uitvoer naar kale RegelSpraak (later), benamingen in het
  caseformaat (cases blijven canoniek).

## Acceptatie

U1–U5 gebouwd met de getuigen; alle zestien gates groen; het Result noemt
de gekozen tekenpatronen, de weigeringszinnen uit echte uitvoer, de
WF-nummers, en de tellingen van gates 3, 10 en 13.

Mag raken: `frontends/regelspraak/` (grammatica, resolver, emitter,
DIVERGENCES, tests), `spec/IR-SPEC.md`, `spec/schema/`, `spec/verifier/`
(als diff, hoofdsessie landt), `backends/drools/` (lader en model, geen
beans/DRL), `ref/` en `backends/clingo/` alleen om de zijtafel te
accepteren, `corpus/golden/` en `corpus/negatief/` (nieuwe getuigen),
`scripts/check-reftype-derivation.sh` (voorstel voor het been). Niet:
`toka/`, de uitvoerende semantiek, het caseformaat.

## Result

*Bouwsessie 2026-09-27, branch `card/vocabulaires-benamingen` (worktree `wt/vocab`, basis `04f71709`), kop
`39e2be76` (vóór de Result-commit). Alle zestien gates groen op de kop (volledige lokale run, tellingen onder "Tellingen"). Niet
gepusht, niet gemerged; `spec/CONSTRUCTS.md`, `spec/PLAN.md` en `CLAUDE.md` niet aangeraakt.*

### Besluiten tijdens de bouw

De drie maintainerbesluiten van 2026-09-27 (Yeb, doorgegeven door de hoofdsessie tijdens de bouw) vervangen
de betreffende zinnen van U1; ze staan als "maintainer decision 2026-09-27" in IR-SPEC ch. 2.12 en in
DIVERGENCES.md, zonder U-nummers. Verwerking:

1. **Geen soort.** Het onderscheid code/term is overal weg: geen `(soort: …)` in de grammatica (een kop die
   het schrijft krijgt de weigering voor een onbekende eigenschap), geen `kind` op `Vocabulary` (schema,
   IR-SPEC, naamtabel: de familie `Vocabulary.kind` en de `kind`-groep weer geschrapt), geen tekenpatroon-arm
   in de verifier. Elke benaming is tekst: niet leeg, aan beide kanten getrimd, spaties erin toegestaan,
   zonder haakjes; uniciteit hoofdletterongevoelig. De eerste commit (`eb8e7dcd`) draagt nog `kind`; de
   tweede (`aa2a5a03`) haalt het weg. Het tekenpatroon voor code-benamingen, oorspronkelijk een keuze van de
   bouwer, is daarmee vervallen; de `cgm`-benamingen in de golden (`VONDST_BEDRAG`) zijn inhoud, geen regel.
2. **Kopregel.** `Vocabulaire <naam>` met `(standaard)` en `(taal: <code>)`, elk in eigen haakjes, in vrije
   volgorde, elk hooguit één keer, beide weglaatbaar. De taalcode gaat als string naar `language`; de enige
   regel erop is "niet leeg, geen witruimte" (WF-116 (d)).
3. **Plaats.** De annotaties staan achteraan de naamgroep, in één vaste positie die de grammatica afdwingt:
   bij een objecttype na `(mv: …)` en `(bezield)`; bij een attribuut na naam en meervoud, vóór het datatype;
   bij een kenmerk vóór `kenmerk`; bij een rol vóór het objecttype; bij feittype, parameter (vóór de dubbele
   punt), enumeratiewaarde, regel en regelgroep direct na de naam. De volgorde van de vocabulaires binnen de
   annotaties is vrij; de emitter schrijft `designations` in de declaratievolgorde van de vocabulaires
   (toets `de_emitter_volgt_de_volgorde_van_de_vocabulaires`).

Eigen technische keuzes, met hun grond:

- **`vocabularies` op de documentwortel, na `format`**, niet onder `model`: U2 zegt "op het document", en de
  benamingen staan op model- én regellaag (regels, regelgroepen); een zijtafel zonder semantiek, zoals
  `diagnostics`.
- **`designations` als lijst van records `{vocabulary, designation}`**, geen map: sinds M1 heeft de wire
  geen mapsleutels, en de arm "twee benamingen voor één vocabulaire" heeft een schrijfbare vorm nodig om een
  getuige te hebben. Altijd het **laatste veld** van zijn knoop.
- **Enumeratiewaarden:** `valueDesignations` op de `Enumeration` op de declaratiepositie, naast `values`, met
  records `{value, vocabulary, designation}`: structureel onmogelijk op een ander datatype en op een
  gebruiksplaats; gesleuteld op `value` (hoofdletterongevoelig, zoals WF-088 waarden vergelijkt).
- **Benoemde vormen:** de zes vormen van ch. 3 én een defaultsgroep (de naamruimte van WF-020) dragen
  benamingen, zodat een regel die in een defaultsgroep opgaat haar benaming op de groep houdt.
- **Vocabulairenaam:** een woord (niet leeg, geen witruimte, haakje of dubbele punt): wat `(<naam>:` kan
  schrijven, naar analogie van het taalbesluit.
- **Lezen:** elke lezer leest de zijtafel structureel (gesloten records, strings, geen lege lijst), de inhoud
  is alleen voor de verifier; geen executor weigert een document om wat een benaming zegt. De verifier leest
  de tafel naast het document (`Loader.vocabulaires`), niet in het `document`-record, omdat de Catala- en
  F-logic-frontends dat record heel bouwen en buiten het "mag raken" vallen.
- **WF-nummering:** WF-116 (de vocabulairedeclaraties, 4 armen), WF-117 (de benamingen, 5 armen), WF-118
  (uniciteit van de effectieve naam, 4 reikwijdte-armen), WF-119 (volledigheid van het standaardvocabulaire),
  WF-120 (een overbodige benaming verantwoord met een waarschuwing).
- **Overbodig** is bytegelijk aan de eigen canonieke naam, in een vocabulaire dat niet het standaard is (in het
  standaard is de benaming verplicht); een verschil in hoofdletters alleen is de spelling van het vocabulaire.
- **Ketenreikwijdte:** attributen en kenmerken per objecttype mét de geërfde, zoals WF-105 WF-002 optilt; een
  botsing wordt gemeld bij het eigen lid van het subtype.

### Syntaxis — de volledige bron van de golden `jutterswet-vocabulaires`

```
Vocabulaire handboek (taal: nl)
Vocabulaire cgm (standaard) (taal: nl)

Domein Vondstsoort is van het type Enumeratie
    'wrakhout' (handboek: hout van een wrak) (cgm: WRAKHOUT)
    'barnsteen' (handboek: amber) (cgm: BARNSTEEN)
    'flessenpost' (handboek: bericht in een fles) (cgm: FLESSENPOST)

Parameter het premietarief (handboek: premie per kilogram) (cgm: PREMIE_TARIEF) : Numeriek (geheel getal)

Objecttype de Jutter (mv: Jutters) (bezield) (handboek: strandjutter) (cgm: JUTTER)
    de naam (handboek: naam) (cgm: NAAM) Tekst;
    het vondstbedrag (handboek: totale premie) (cgm: VONDST_BEDRAG) Numeriek (getal met 2 decimalen);
    het aantal waardevolle vondsten (handboek: aantal vondsten van waarde) (cgm: AANTAL_WAARDEVOL) Numeriek (niet-negatief geheel getal);
    is premiewaardig (handboek: komt in aanmerking voor premie) (cgm: PREMIE_WAARDIG) kenmerk (bijvoeglijk);

Objecttype de Vondst (mv: Vondsten) (handboek: aangespoeld voorwerp) (cgm: VONDST)
    de soort (handboek: soort vondst) (cgm: VONDST_SOORT) Vondstsoort;
    het gewicht (handboek: gewicht in kilogram) (cgm: GEWICHT) Numeriek (getal met 2 decimalen);
    de premie (handboek: premie voor de vondst) (cgm: PREMIE) Numeriek (getal met 2 decimalen);
    is waardevol (handboek: van waarde) (cgm: WAARDEVOL) kenmerk (bijvoeglijk);

Feittype vondstaanmelding (handboek: aanmelding van een vondst) (cgm: VONDST_AANMELDING)
    de aanmelder (handboek: melder) (cgm: AANMELDER) Jutter
    de vondst (mv: vondsten) (handboek: gemelde vondst) (cgm: GEMELDE_VONDST) Vondst
Eén aanmelder heeft meerdere vondsten

Regelgroep premieberekening (handboek: berekening van de premie) (cgm: PREMIEBEREKENING)
Regel premie van een vondst (handboek: premie per vondst) (cgm: R_PREMIE_VONDST)
    geldig altijd
        De premie van een Vondst moet berekend worden als zijn gewicht maal het premietarief.

Regel vondstbedrag van een jutter (handboek: premie per strandjutter) (cgm: R_VONDSTBEDRAG)
    geldig altijd
        Het vondstbedrag van een Jutter moet berekend worden als de som van de premie van alle vondsten van de Jutter of 0 als die er niet zijn.

Regel waardevolle vondst (handboek: vondst van waarde) (cgm: R_WAARDEVOL)
    geldig altijd
        Een Vondst is waardevol indien zijn soort gelijk is aan 'barnsteen'.

Regel aantal waardevolle vondsten (handboek: telling van de waardevolle vondsten) (cgm: R_AANTAL_WAARDEVOL)
    geldig altijd
        Het aantal waardevolle vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die waardevol zijn.

Regel premiewaardigheid (handboek: toets op premie) (cgm: R_PREMIEWAARDIG)
    geldig altijd
        Een Jutter is premiewaardig indien zijn vondstbedrag groter is dan 10.
```

Tokens: `VOCABULAIRE` (`Vocabulaire`), `STANDAARD` (`(standaard)`), `BENAMING_START` (`(<woord>:` tot de dubbele
punt; `MV_START` wint `(mv:`), en in een eigen lexermodus `BENAMING_TEKST` (alles tot het sluithaakje, één
token), `BENAMING_EINDE` en `BENAMING_ONAF` (een regeleinde vóór het sluithaakje). `regelNaam` en de naam van
een beslistabel stoppen bij de opener.

### IR-vorm

- Document: `{format, vocabularies?, model, rules, diagnostics}`; `vocabularies` is een lijst `Vocabulary
  {node, name, default, language?}` (canonieke volgorde = die van het schema), weggelaten zonder vocabulaire.
- `designations` (lijst van `{vocabulary, designation}`, minstens één) als laatste veld op ObjectType,
  Attribute, Characteristic, FactType, Role, Parameter, Rule, Allocation, ObjectCreation, FactCreation,
  Consistency, Uniqueness, PrioritizedDefaults en RuleGroup; weggelaten zonder benaming.
- `valueDesignations` (lijst van `{value, vocabulary, designation}`) als laatste veld van de `Enumeration` in
  `Domain.datatype`, waarde voor waarde in de declaratievolgorde.

```json
"vocabularies": [
  { "node": "Vocabulary", "name": "handboek", "default": false, "language": "nl" },
  { "node": "Vocabulary", "name": "cgm", "default": true, "language": "nl" }
],
...
"datatype": { "node": "Enumeration", "values": ["wrakhout", "barnsteen", "flessenpost"],
  "valueDesignations": [
    { "value": "wrakhout", "vocabulary": "handboek", "designation": "hout van een wrak" },
    { "value": "wrakhout", "vocabulary": "cgm", "designation": "WRAKHOUT" }, ... ] }
...
{ "node": "RuleGroup", "name": "premieberekening", "recursive": false, "members": [ ... ],
  "source": { "snippet": "premieberekening", "line": 28 },
  "designations": [ { "vocabulary": "handboek", "designation": "berekening van de premie" },
                    { "vocabulary": "cgm", "designation": "PREMIEBEREKENING" } ] }
```

(ingekort; de golden schrijft twee spaties inspringing, één sleutel per regel.)

### WF-regels, per arm de verifierzin (echte uitvoer) en de negatieve getuige

| arm | getuige | verifierzin |
|---|---|---|
| WF-116 (a) | `wf116-vocabulairenaam-geen-woord` | `WF-116 FAIL /vocabularies/0/name: vocabulary name 'hand boek' is not a word: it is empty, or carries whitespace, a parenthesis or a colon` |
| WF-116 (b) | `wf116-dubbel-vocabulaire` | `WF-116 FAIL /vocabularies/2/name: vocabularies 'handboek' and 'Handboek' collide case-insensitively` |
| WF-116 (c) | `wf116-twee-standaardvocabulaires` | `WF-116 FAIL /vocabularies/1/default: vocabularies 'handboek' and 'cgm' are both the default: a document has at most one default vocabulary` |
| WF-116 (d) | `wf116-taal-met-witruimte` | `WF-116 FAIL /vocabularies/0/language: language 'nl NL' of vocabulary 'handboek' is not a code: it is empty or carries whitespace` |
| WF-117 (a) | `wf117-onbekend-vocabulaire` | `WF-117 FAIL /model/objectTypes/1/designations/0/vocabulary: designation 'aangespoeld voorwerp' names the vocabulary 'wet', which the document does not declare` |
| WF-117 (b) | `wf117-twee-benamingen-een-vocabulaire` | `WF-117 FAIL /model/objectTypes/0/designations/1: a second designation in the vocabulary 'handboek' on the object type 'Jutter': 'strandjutter' and 'jutter op het strand'` |
| WF-117 (c) | `wf117-benaming-niet-getrimd` | `WF-117 FAIL /model/objectTypes/0/designations/0/designation: designation 'strandjutter ' in the vocabulary 'handboek' is empty or not trimmed: a designation is text without whitespace at either end` |
| WF-117 (d) | `wf117-benaming-met-haakje` | `WF-117 FAIL /model/objectTypes/1/attributes/2/designations/0/designation: designation 'premie (voor de vondst)' in the vocabulary 'handboek' carries a parenthesis, which the source form (<vocabulary>: <designation>) cannot write` |
| WF-117 (e) | `wf117-waardebenaming-onbekende-waarde` | `WF-117 FAIL /model/domains/0/datatype/valueDesignations/6/value: a value designation names 'gruis', which the enumeration 'Vondstsoort' does not declare` |
| WF-118 (a) | `wf118-effectieve-naam-objecttypen` | `WF-118 FAIL /model/objectTypes/1: object types 'Jutter' and 'Vondst' share the effective name 'Strandjutter' in the vocabulary 'handboek'` |
| WF-118 (b) | `wf118-effectieve-naam-attributen` | `WF-118 FAIL /model/objectTypes/0/attributes/2: attributes 'vondstbedrag' and 'aantal waardevolle vondsten' of the object type 'Jutter' share the effective name 'aantal waardevolle vondsten' in the vocabulary 'handboek'` (een benaming tegen een canonieke naam) |
| WF-118 (c) | `wf118-effectieve-naam-rollen` | `WF-118 FAIL /model/factTypes/0/roles/1: roles 'aanmelder' and 'vondst' of the fact type 'vondstaanmelding' share the effective name 'Melder' in the vocabulary 'handboek'` |
| WF-118 (d) | `wf118-effectieve-naam-waarden` | `WF-118 FAIL /model/domains/0/datatype/values/2: values 'barnsteen' and 'flessenpost' of the enumeration 'Vondstsoort' share the effective name 'Amber' in the vocabulary 'handboek'` |
| WF-119 | `wf119-standaardvocabulaire-onvolledig` | `WF-119 FAIL /rules/ruleGroups/0: the rule group 'premieberekening' carries no designation in the default vocabulary 'cgm'` |
| WF-120 | `wf120-overbodige-benaming-zonder-melding` | `WF-120 FAIL /model/objectTypes/0/attributes/0/designations/0/designation: designation 'naam' in the vocabulary 'handboek' spells the canonical name of the attribute 'naam' of 'Jutter', and no diagnostic accounts for it: a redundant designation is admitted with a warning whose message contains it` |

Elke getuige wordt precies op haar arm en met één regel geweigerd; `expect.txt` pint een letterlijk fragment
zonder pointer, `NOTE.md` citeert de verifier. De golden (positieve kant van alle vijf) is verifier-schoon.

### Tekenpatroon voor code-benamingen

Vervallen door maintainerbesluit 1: er is geen code-soort en geen tekenpatroon. Wat overblijft is de
tekstregel van WF-117 (c)/(d) (niet leeg, getrimd, geen haakje), voor een vocabulairenaam WF-116 (a) (een woord:
geen witruimte, haakje of dubbele punt) en voor een taalcode WF-116 (d) (niet leeg, geen witruimte).

### Frontendweigeringen (echte uitvoer van `Resolver --ir`)

Elk een ERROR op de regel van de kop of de annotatie (de controles van de tweede ronde op de regel van de
declaratie):

- `ERROR (line 1): vocabulaire 'mv': die naam is gereserveerd, want '(mv:' opent de meervoudsvorm van een naam, en een benaming in dit vocabulaire zou als meervoud gelezen worden`
- `ERROR (line 2): vocabulaire 'Handboek' is al gedeclareerd als 'handboek'; de namen van de vocabulaires zijn uniek, hoofdletterongevoelig (WF-116)`
- `ERROR (line 1): vocabulaire 'cgm' draagt '(standaard)' twee keer` (en zo ook `'(taal: …)' twee keer`)
- `ERROR (line 1): vocabulaire 'cgm': '(soort: …)' is geen eigenschap van een vocabulaire; een vocabulairekop kent '(standaard)' en '(taal: <code>)'`
- `ERROR (line 1): vocabulaire 'cgm': de taalcode 'nl NL' is leeg of bevat witruimte; een taalcode is één woord (WF-116)`
- `ERROR (line 2): de vocabulaires 'cgm' en 'wet' zijn allebei het standaardvocabulaire; een document heeft er hooguit één (WF-116)`
- `ERROR (line 2): de annotatie '(handboek: strand' bij het objecttype 'Jutter' is niet op haar regel gesloten; een benaming staat tussen haakjes op één regel`
- `ERROR (line 2): de benaming van het objecttype 'Jutter' in het vocabulaire 'handboek' is leeg; een benaming is tekst tussen de dubbele punt en het sluithaakje (WF-117)`
- `ERROR (line 2): de benaming 'strand (jutter' van het objecttype 'Jutter' in het vocabulaire 'handboek' bevat een haakje; een benaming is tekst zonder haakjes, want het sluithaakje sluit de annotatie (WF-117)`
- `ERROR (line 2): de benaming 'jutter' van het objecttype 'Jutter' noemt het vocabulaire 'wet', dat het document niet declareert ('Vocabulaire wet', WF-117)`
- `ERROR (line 2): Het objecttype 'Jutter' draagt twee benamingen in het vocabulaire 'handboek', 'jutter' en 'strandjutter'; een declaratie heeft hooguit één benaming per vocabulaire (WF-117)`
- `ERROR (line 4): beslistabel 'naamtabel' draagt een benaming, en een beslistabel heeft er geen: zij staat voor de regels van haar rijen, die hun namen van de frontend krijgen; schrijf de benaming op een regel`
- `ERROR (line 8): het blok 'jong' draagt een benaming, en een uitzonderings- of afwijkingsblok heeft er geen: het wordt een default in de groep van zijn basisregel, en een default heeft geen naam op de wire; schrijf de benaming op de basisregel`
- `ERROR (line 3): de regel 'havendagdefinitie' draagt een benaming, maar wordt op de wire geen benoemde vorm (een regel, creatie, verdeling, consistentie, uniciteit of defaultsgroep): een dagsoortdefinitie en een startpuntbepaling hebben geen naam, en een verdeling zonder methode wordt niet geschreven; zonder vorm heeft de benaming geen plaats`
- `ERROR (line 4): Het attribuut 'leeftijd' van 'Jutter' draagt geen benaming in het standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119)`; bij een declaratie die de frontend zelf schreef volgt `; de frontend schreef deze declaratie zelf, zonder plaats voor een annotatie` (gemeten op een startpuntanker: `ERROR (line 0): Het attribuut 'startpunt wachtjaar' van 'Schuit' draagt geen benaming …`)
- `ERROR (line 5): Het attribuut 'diepte' van 'Wrakvondst' en het attribuut 'gewicht' van 'Vondst' hebben in het vocabulaire 'handboek' dezelfde effectieve naam 'gewicht'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de weg terug naar de declaratie één antwoord heeft (WF-118)`
- WARNING (het document blijft op schijf): `WARNING (line 3): de benaming 'Jutter' van het objecttype 'Jutter' in het vocabulaire 'handboek' spelt haar canonieke naam en is overbodig: de effectieve naam is dezelfde zonder haar (WF-120)`
- Een benaming lost in regeltekst nooit op: als regeldoel `ERROR (line 6): regeldoel 'De totale premie van een Jutter': 'totale premie' is geen gedeclareerd attribuut van Jutter (WF-022, IR-SPEC ch. 3.1)` (de bestaande weigering); in een lezing de bestaande `onopgelost`-waarschuwing met marker; een benaming die de naam van een attribuut van een ander type spelt krijgt dezelfde WF-022-weigering; een benaming die de canonieke naam van een ander lid van dezelfde reikwijdte spelt, leest dat andere lid (alle vier in `VocabulaireDialectTest`).

### Publieke leesingang (Drools-lader → `RegelSpraakModel`)

De lader vult `RegelSpraakModel.vocabulaires` (`List<Vocabulaire>`) en `.benamingen`
(`Map<Benoembaar, List<Benaming>>`); beans, DRL en exec-model ongewijzigd. Leesingang op `RegelSpraakModel`
(`dev.uchar.regelspraak.model`):

```java
public record Vocabulaire(String naam, boolean standaard, String taal) {}
public record Benaming(String vocabulaire, String tekst) {}
public record Benoembaar(Soort soort, String eigenaar, String naam) {
    public enum Soort { OBJECTTYPE, ATTRIBUUT, KENMERK, FEITTYPE, ROL, ENUMERATIEWAARDE, PARAMETER, VORM, REGELGROEP }
    // fabrieken: objecttype(n), attribuut(ot, n), kenmerk(ot, n), feittype(n), rol(ft, n),
    //            enumeratiewaarde(domein, w), parameter(n), vorm(n), regelgroep(n)
}
public Vocabulaire vocabulaire(String naam);
public Optional<String> benaming(String vocabulaire, Benoembaar element);         // vocabulaire -> benaming
public String effectieveNaam(String vocabulaire, Benoembaar element);             // benaming, anders canoniek
public Optional<Benoembaar> benoemd(String vocabulaire, Benoembaar.Soort soort,   // de omkering: effectieve
                                    String eigenaar, String naam);                //   naam -> declaratie
public List<Benoembaar> benoembaar();                                             // alle benoembare declaraties
public static String vormnaam(String naam);                                       // vormnaam zonder " #i"
```

Namen worden hoofdletterongevoelig vergeleken; `benoemd` zoekt attributen en kenmerken langs de supertypeketen
van `eigenaar`. Getuige: `backends/drools/.../loader/VocabulaireWitnessTest` (6 toetsen).

### Het gate-13-been (voorstel, beschermd pad)

Elke golden met vocabulaires en cases wordt drie keer extra gespeeld tegen zijn eigen onaangeroerde uitkomst
(ref en clingo exit 0; beide Drools-takken hun eigen statusregel): **gestript** (alle vocabulaires en
benamingen weg), **verwisseld** (de namen van de vocabulaires één plaats doorgeschoven, in declaraties en
benamingen gelijk: elke benamingenset houdt de standaardmarkering en taal van haar vocabulaire, dus het document
blijft verifier-schoon; de minimale twist, blind voor een executor die de tafel via de rol van het
standaardvocabulaire leest) en **geroteerd** (binnen elk vocabulaire en elke reikwijdte van WF-118 schuiven de
benamingen één declaratie door; de multiset van effectieve namen blijft gelijk, dus WF-118/119 blijven gelden;
een rotatie die buiten het standaard een overbodige benaming zou maken probeert de volgende verschuiving). De
drie kopieën van `jutterswet-vocabulaires` zijn verifier-schoon (gemeten). Geen overlevenslijst: elke divergentie
faalt; een lege set faalt. Vloeren: `EXPECTED_MIN_VOCABULAIRE_GOLDENS=1`, `EXPECTED_MIN_VOCABULAIRE_GESTRIPT=46`,
`EXPECTED_MIN_VOCABULAIRE_GEROTEERD=36` (gemeten 2026-09-27). Telling: 1 golden, 46 benamingen gestript, 46
verwisseld, 36 geroteerd, 18 executorreplays gelijk aan de onaangeroerde uitkomst. **Rood gezien:** met een
mutant in de gedeelde OCaml-lader (de eerste benaming van een attribuut als zijn naam) faalt het been op de
verwisselde en geroteerde kopie in ref en clingo (8 meldingen); de gestripte blijft groen, zoals hoort.

### Tellingen (volledige run op de kop)

- Gate 1 PASS. Gate 2: 312 bestanden (was 311), 41156 objecten in 813 documenten in schemavolgorde.
- **Gate 3:** 312 goldens schoon; **501 negatief** (was 486, +15), **344 met `expect.txt`** (was 329), **605
  pointers** (was 590); vloeren ongewijzigd (350/260/547).
- Gate 4: 772 casebestanden. Gate 5: 732 cases (ref-suite 1152). Gate 6: 732 cases (backendsuite 1988). Gate 7:
  688 cases weerleggen de no-op. Gate 8: 40 rode cases. Gate 9: 241 checks, 12 goldens. Gate 11: PASS (29/29/29).
- **Gate 10:** **950 JUnit-toetsen** (was 934; 16 nieuw in `VocabulaireDialectTest`), **100 rgs-goldens**
  byte-gelijk (was 99; de 99 oude ongewijzigd).
- **Gate 12:** gespeeld 217 (pin 216 → 217), DRL-tekst-tak 187 (pin 186 → 187), eigen commit met reden; JUnit
  846 (met de 6 nieuwe van `VocabulaireWitnessTest`).
- **Gate 13:** eenheidstwist ongewijzigd: 41 annotaties over 23 goldens (vloer 38), 106 replays, 22 door de
  DRL-tak gespeeld (vloer 20); vocabulaireleg: 1 golden (vloer 1), 46 gestript (vloer 46), 46 verwisseld, 36
  geroteerd (vloer 36), 18 replays.
- Gate 14: 150 checks, 2 goldens. Gate 15: routeleg 100 goldens met `bron.rgs`. Gate 16: 7 goldens, 31 cases.
- `python3 spec/proposals/v1-naamtabel-toets.py`: rc 0.

**Inertie (DIVERGENCES):** 638 verschillende RegelSpraak-bronnen (corpus, pariteitszone, de regelcollecties en
toolchainvoorbeelden buiten deze repo) door de frontend van de basiscommit en van de kop: 193 IR's byte-gelijk,
445 ERROR-documenten met dezelfde diagnostiek op de ANTLR-verwachtingsverzamelingen na (294× `'Vocabulaire'`,
4× `BENAMING_START`), daarna nul verschil. `Vocabulaire` en `(standaard)` komen in geen bron voor; de
openervorm `(<woord>:` 60 keer, alle in `//`-commentaar.

### Voorgestelde CONSTRUCTS-rij (rij 41)

| 41 | Named vocabularies with a designation per declaration | an implementing organisation's concept map, which names one concept once per layer (statute, handbook, business rules, canonical data model); prior art `research/vocabulaires-2026-09-27/prior-art.md` (SBVR designations per speech community, OMG MVF, XBRL with the EDGAR label rules, SNOMED CT language sets) | core (metadata) | neither: a side table without semantics | v0 has none. **Landed (2026-09-27, card `vocabulaires-benamingen`):** extension fields — `vocabularies` on the document (`Vocabulary {name, default, language?}`), `designations` (records `{vocabulary, designation}`) as the last field of an ObjectType, Attribute, Characteristic, FactType, Role, Parameter, named form (the six ch. 3 forms and a defaults group) and RuleGroup, and `valueDesignations` (`{value, vocabulary, designation}`) beside an enumeration's declared values (IR-SPEC ch. 2.12). No executor reads the table for evaluation; every reader reads it structurally; the Drools loader carries it into `RegelSpraakModel` with a public reading entry (`benaming`, `effectieveNaam`, `benoemd`). Five rules with an arm per cause and 15 negatiefs: WF-116 (a vocabulary name is a word, names unique ci, at most one default, a language is a code without whitespace), WF-117 (a designation names a declared vocabulary, one per vocabulary per declaration, is non-empty trimmed text without parentheses, a value designation names a value), WF-118 (effective names unique per vocabulary in the scopes of the canonical names, so the way back is a function), WF-119 (the default vocabulary is complete), WF-120 (a redundant designation outside the default is accounted for by a warning). A vocabulary has no kind: every designation is text (maintainer decision 2026-09-27). The RegelSpraak dialect writes `Vocabulaire <naam> (standaard) (taal: <code>)` and `(<vocabulaire>: <benaming>)` at the end of a name group (`frontends/regelspraak/grammar/DIVERGENCES.md`, "Vocabulaires en benamingen"; inertness measured over 638 sources). Witness `jutterswet-vocabulaires` (ref ≡ clingo ≡ both Drools branches); gate 13's vocabulary leg holds the executors indifferent to the table stripped, exchanged and rotated | out: a service or an explanation per vocabulary (the distribution), the lint against the concept map (workbench), a status per designation, several languages, a strip output to bare RegelSpraak, designations in the case format; a default vocabulary cannot coexist with declarations the frontend writes itself (decision-table rows, the opgave marker's members, a start point's anchor) |

### Voorgestelde PLAN §9-alinea

**Named vocabularies landed (2026-09-27, card `vocabulaires-benamingen`):** a document can declare named vocabularies and give every designatable declaration — object type, attribute, characteristic, fact type, role, enumeration value, parameter, named form, rule group — a name in each, as a side table without semantics (IR-SPEC ch. 2.12; WF-116 to WF-120 with fifteen negatiefs, one per arm), written by the RegelSpraak dialect as `Vocabulaire <naam> (standaard) (taal: <code>)` and `(<vocabulaire>: <benaming>)` at the end of a name group, read structurally by every reader and carried by the Drools loader into `RegelSpraakModel` with a public reading entry; golden `jutterswet-vocabulaires`, gate 12 plays 217 and 187, and gate 13's new vocabulary leg holds ref, clingo and both Drools branches indifferent to the table stripped, exchanged and rotated. Three maintainer decisions of the same day shaped the surface: a vocabulary has no kind (every designation is text), the head's properties stand in their own parentheses, and the annotations close the name group. Open, recorded and not opened as cards: the service and the explanation per vocabulary (a card in the distribution), the lint against the concept map (the workbench), a status per designation and several languages, a strip output to bare RegelSpraak; a default vocabulary cannot coexist with declarations the frontend writes itself (decision-table rows, the opgave marker's members, a start point's anchor), which the completeness rule refuses; and the redundancy warning of WF-120 is accounted for by literal containment, with WF-113 (b)'s weakness.

### Open punten

- Een standaardvocabulaire en door de frontend geschreven declaraties (rijen van een beslistabel, de leden en
  vormen van de opgavemarkering, een startpuntanker) gaan niet samen: die hebben geen plaats voor een annotatie,
  en WF-119 weigert ze (met de zin die dat zegt). Een vocabulaire zonder `(standaard)` heeft die grens niet.
- WF-120 verantwoordt via letterlijke bevatting, met de zwakte van WF-113 (b): een andere melding die de tekst
  bevat verantwoordt de benaming ook.
- `RegelSpraakModel.benoemd` en `benoembaar()` rekenen per aanroep opnieuw (lineair over het model); goed genoeg
  voor een dienst die de tafel één keer omzet, niet voor een hete lus.
- Een bestaand scenariodocument met vocabulaires is niet apart getoetst; de scenarioroute gebruikt dezelfde
  `Resolver.bouw`, dus de extensie werkt daar mee, maar zonder getuige.
- U7 blijft buiten deze kaart: dienst per vocabulaire, lint tegen de begrippenkaart, deprecatiestatus,
  meertaligheid, strip-uitvoer, benamingen in het caseformaat.

### Normvragen (met het gekozen standaardantwoord)

1. **Vorm van de WARNING van WF-120.** De verifier heeft geen waarschuwingskanaal, en een negatieve getuige moet
   geweigerd worden. Standaardantwoord (gebouwd): een overbodige benaming is toegestaan en het document moet
   haar verantwoorden met een diagnostic die haar letterlijk bevat — het patroon van WF-113 (b) voor markers;
   de frontend schrijft die waarschuwing zelf, dus alleen een handgeschreven document zonder melding wordt
   geweigerd. Alternatief: een niet-falend WARN-kanaal in de verifier, zonder negatieve getuige.
2. **Plaats van `vocabularies`.** Standaardantwoord: op de documentwortel na `format` (U2 "op het document";
   de tafel spant model- en regellaag). Alternatief: onder `model` naast `unitSystems` en `dayKinds`, zoals het
   prior-art-rapport voorstelde.
3. **Een defaultsgroep draagt benamingen** (als benoemde vorm in de naamruimte van WF-020), zodat een regel die
   in een groep opgaat haar benaming houdt. Standaardantwoord: ja. Alternatief: alleen de zes vormen van ch. 3,
   en een benaming op zo'n regel weigeren.

### Buiten het "mag raken" (gemeld)

- `spec/proposals/v1-naamtabel.json`: rijen voor de nieuwe schemanamen (`Vocabulary`, `vocabularies`,
  `designations`, `valueDesignations`, `vocabulary`, `designation`, `default`, `language`, en groepen op `name`
  en `value`); zonder die rijen geeft de naamtabeltoets geen rc 0.
- `frontends/regelspraak/src/test/.../ScenarioLoweringTest.java`: het rijke document kreeg een vocabulaire en één
  benaming, omdat de toets `een_scope_die_alles_noemt_is_het_productiedocument` eist dat elke verzameling van het
  model gevuld is (het model kreeg er twee).
- `scripts/check-reftype-derivation.sh` (voorstel, in het "mag raken") en `scripts/check-drools.sh` (de twee
  gelijkheidspinnen, zoals de opdracht voorschreef).

### Commits op de branch

- `63140501` claim: kaart vocabulaires-benamingen naar doing
- `eb8e7dcd` Vocabulaires als zijtafel in schema en IR-SPEC (voorstel)
- `aa2a5a03` Verifier en gedeelde lader lezen de vocabulairezijtafel (voorstel)
- `7609796e` RegelSpraak-dialect: vocabulaires en benamingen
- `e78a15ed` Drools-lader draagt de vocabulairezijtafel in het model
- `6f35db3d` Golden jutterswet-vocabulaires: twee vocabulaires op elke soort
- `64f2b26d` Negatieve getuigen voor WF-116 tot WF-120, één per arm
- `03481678` Frontendtoetsen voor vocabulaires en benamingen
- `9967d986` Gate 13: een vocabulaireleg naast de eenheidstwist (voorstel)
- `165be016` DIVERGENCES: sectie Vocabulaires en benamingen
- `fddf3e28` Gate 12: de nieuwe golden speelt, beide pinnen één omhoog
- `d625d817` DIVERGENCES: de tokens van de vocabulaire-extensie precies geteld
- `39e2be76` Frontendtoets: een benaming die de naam van een ander type spelt
- (deze Result-commit)

## Keuring (hoofdsessie, 2026-09-27)

Geland op main na een eigen run van alle zestien gates op de kop van de
branch (`/tmp/vocab/logs/run1`): groen. Gemeten daar: gate 3 501 negatief,
344 met `expect.txt`, 605 pointers; gate 10 950 toetsen, 100 rgs-goldens
byte-gelijk; gate 12 speelt 217 en 187 (gelijkheidspinnen één omhoog, eigen
commit); gate 13 eenheidstwist 41/23 ongewijzigd en het vocabulairebeen 1
golden, 46 gestript, 46 verwisseld, 36 geroteerd, 18 replays gelijk; gate
16 7/31; naamtabel-toets rc 0.

Gelezen: de IR-SPEC-sectie 2.12 met de vijf WF-regels en hun rijen in de
index; het schema (zijtafel als extensievelden, `designations` laatste
veld, `valueDesignations` naast `values`, sleutelvolgorde canoniek); de
verifier (`Loader.vocabulaires` naast het document, arm per oorzaak, elke
getuige met één regel geweigerd en een pointer die resolveert); de golden
met handberekende pinnen en het randgeval op precies 10; de vijftien
negatieve getuigen met `expect.txt` zonder pointer; het gate-13-been (drie
kopieën verifier-schoon, rood gezien onder een mutant van de gedeelde
lader); de frontendweigeringen uit echte uitvoer; de inertie over 638
bronnen; de tripwires (geen pariteitszone-naam, geen U-nummers, geen
regelnummerstaart in artefacten).

Tijdens de bouw kwamen drie besluiten van de maintainer (Yeb, via de
hoofdsessie) die de betreffende zinnen van U1 vervangen: er is geen soort
(code/term) en dus geen tekenpatroon — elke benaming is tekst, spaties
inbegrepen; de kopregel schrijft `(standaard)` en `(taal: <code>)` elk in
eigen haakjes; de benamingen sluiten de naamgroep, na `(mv: …)` en
`(bezield)`. Het Result beschrijft de verwerking.

De drie normvragen zijn beantwoord met het standaardantwoord van de bouwer,
hier bevestigd als maintainersbesluit: WF-120 verantwoordt een overbodige
benaming met een diagnostic die haar letterlijk bevat (het patroon van
WF-113 (b), zwakte geregistreerd); `vocabularies` staat op de documentwortel
na `format`, omdat de tafel model- en regellaag spant; een defaultsgroep
draagt benamingen als benoemde vorm. Aanvaard buiten het "mag raken":
`spec/proposals/v1-naamtabel.json` (de naamtabeltoets eist de rijen) en
één aanvulling in `ScenarioLoweringTest` (de toets eist elke verzameling
van het model gevuld).

Open, vastgelegd in PLAN §9 en niet als kaart: een standaardvocabulaire
gaat niet samen met declaraties die de frontend zelf schrijft (rijen van
een beslistabel, leden van de opgavemarkering, een startpuntanker) — de
volledigheidsregel weigert zo'n document met een zin die dat zegt; een
vocabulaire zonder `(standaard)` heeft die grens niet. Verder: de
scenarioroute met vocabulaires heeft geen eigen getuige; `benoemd` en
`benoembaar()` rekenen lineair per aanroep. Registerwerk in de commit na
de landing: CONSTRUCTS rij 41, PLAN §9, CLAUDE.md gate 13. Daarna de
pin-bump naar de distributie en de werkbank; de dienstkaart
`dienst-per-vocabulaire` volgt op Yebs sein.
