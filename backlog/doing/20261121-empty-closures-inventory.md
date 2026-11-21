# empty-closures-inventory: opsommen waar een document de wereld sluit of een ontkenning op leeg laat rusten

*Opgesteld door een werkbanksessie op 2026-11-21, op verzoek van de eigenaar. Stap 1 van drie uit het advies
"Leeg, onbekend en niet van toepassing" (werkbank, `werknotities/20261121-null-logica/rapport.md`, §3 en §4). Alleen
meten; de semantiek verandert niet.*

## Aanleiding

De eigenaar besloot op 2026-11-20: **uit leeg mag niets afgeleid worden**. Het besluit is algemeen bedoeld
(verduidelijkt op 2026-11-21): het is een eis aan het uchar-dialect, niet een keuze van één regelset; het kwam op bij
v2/herleving-ww (regelspraak-rules). Leeg is "nog niet ingevuld"; pas als een marker (`onbekend`, `niet van toepassing`) is gegeven, of een
object of feittype-relatie ontbreekt, mag worden doorgerekend (bevestigd 2026-11-21). In uchar is leeg de spiegel van
RegelSpraak v0 (RUNTIME-SEMANTICS ch. 1.2–1.4, besluit 2026-08-16): leeg telt in plus/min/maal als 0, een vergelijking
met leeg is onwaar, voorwaarden zijn tweewaardig, een kenmerk is onwaar zolang het niet is toegekend. Dat is in feite
een closed-world assumption. De regelset vangt het nu per plek af (toetsen "heeft een waarde", een hulpkenmerk
`herleving afhankelijk-van-ontbrekende invoer`); reviews vonden steeds nieuwe plekken (regelspraak-rules 8edee01,
inventaris: twaalf plekken en latere vondsten).

Het advies stelt een strikte lezing van leeg voor (Kleene K3 voor voorwaarden, leeg propageert, de markers blijven
gevulde waarden, de wereld sluiten alleen met een uitdrukkelijke vorm: local closed world), per document aan te
zetten. Voor de eigenaar daarover besluit, wil hij weten hoe groot het is.

## Goal

Een commando dat op een bestaand document, zonder iets te veranderen, opsomt:

- **sluitingen:** plekken waar een uitkomst op een lege waarde rust omdat leeg als 0 of als onwaar wordt gelezen
  (coalescerend rekenen met een invoer die leeg kan zijn; een vergelijking of predicaat waar leeg de uitkomst beslist;
  een initialisatie of standaardwaarde; een tak op "is leeg");
- **leeggevoelige ontkenningen:** "heeft geen K", "voldoet aan geen van", een uitzondering waarvan de voorwaarde niet
  kon toetsen, een basisregel onder zo'n uitzondering — plekken waar een ontbrekende invoer via een ontkenning een
  uitkomst beslist.

Per plek: bestand en regel, de soort, en welke invoer leeg kan zijn. Waar het commando hoort (bij `check`, als eigen
subcommando, of in de lint van de werkbank) stelt de bouwer voor.

## Acceptance

- Op regelspraak-rules `229d744` (v2/herleving-ww vóór de ronde van 20-11) staan de twaalf plekken van de inventaris
  (`werkbank/werknotities/20261120-leeg-afleiding/inventaris.md`) en de latere vondsten (review-regels, review-groep2)
  in de uitvoer; op `8edee01` of later zijn de afgevangen plekken herkenbaar (met hun toets of het hulpkenmerk).
- Op het corpus: per golden een telling (sluitingen, ontkenningen), zodat zichtbaar is hoeveel op coalesceren of
  negation as failure rust.
- Geen wijziging in IR, executors of uitkomsten; gates groen.
- WERKBANK.md voor de werkbank met de uitvoer op v2.

## Omvang

Klein: één ronde, alleen analyse. Daarna besluit de eigenaar over stap 2 (voorstel `STRICT-EMPTY.md` en een proef
achter een vlag) en stap 3 (landen).

## May touch

- `spec/verifier/lib/empty_closures.ml` (nieuw), `spec/verifier/bin/verifier.ml`, `spec/verifier/README.md`;
- `spec/verifier/test/empty-closures/` (nieuw): één getuige per soort;
- `scripts/check-verifier.sh` (een poot in gate 3) en de alinea van gate 3 in `CLAUDE.md`: een voorstel, dat de
  hoofdsessie landt;
- deze kaart.

## Out of scope

Het IR-schema, de executors (ref, clingo, Drools), de semantiek en de verlaging van de frontends, het corpus, de
toolchain, de werkbank, regelspraak-rules en de dataset. Geen naam of afspraak van een regelset in code, tests of
README.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21):

- Het commando hoort in uchar zelf, als analyse over de IR; niet in de toolchain en niet in de lint van de werkbank.
  De stappen 2 en 3 van het advies komen in de frontend en de IR van uchar.
- De meting op v2 komt in het Result en in WERKBANK.md; alleen de telling op het corpus is een test.
- Waar de IR geen bestand en regel draagt, geeft het commando de JSON-pointer en de positie die de IR wel draagt, en
  het Result benoemt wat ontbreekt.
- Een afvanging meldt het commando naar haar vorm ("staat onder een toets op gevuld", "afgevangen door een kenmerk
  waarvan een regel op leeg toetst"), zonder namen van een regelset.
- Het principe geldt algemeen, voor het dialect. De eigenaar wil geen closed-world assumption, het onderscheid tussen
  leeg en de twee markers behouden, en een lezing die uitlegbaar is voor engineers en regelanalisten.
- Het advies dat voorligt, nog geen besluit: een regelset leest standaard open en sluit uitdrukkelijk per gegeven of
  relatie (local closed world); geen schakelaar bij het uitrekenen. Deze meting dient dat besluit.

Van de hoofdsessie (voorlopig, ter bevestiging):

- **Vorm.** `verifier --empty-closures <ir.json>`: één tab-gescheiden regel per plek, in documentvolgorde; exit 0 ook
  als er plekken zijn, 2 bij een laadfout. De analyse staat in een eigen module `Empty_closures` van de
  verifier-bibliotheek en leest de afhankelijkheidsgraaf die WF-045 en WF-046 lopen.
- **Maatstaf.** Een plek telt wanneer de tweewaardige lezing van RUNTIME-SEMANTICS ch. 1.2 een vorm laat beslissen
  (een waarde schrijven, een kenmerk toekennen, een consistentiefout melden) in een toestand waarin een waarde die zij
  leest leeg is, terwijl een lezing waarin leeg doorgeeft en een voorwaarde over leeg onbepaald is (het advies, §3.3)
  daar niets zou beslissen. Een vergelijking die op leeg onwaar is en waar verder niets uit volgt, telt dus niet; dezelfde
  vergelijking onder een ontkenning wel.
- **Twee klassen.** `closure`: de waarde- en vormkant (een operand die als 0 telt, een bijdrage die een vouw overslaat,
  een lege grens, een vergelijking die op leeg waar is, een toets op leeg, een initialisatie, een terugvalwaarde).
  `negation`: een kenmerk of voorwaarde in een ontkennende positie, en een default die beslist onder een hogere default
  of uitzonderingsgroep die op leeg niet kon toetsen.
- **Per plek.** De soort, de pointer, de naam van de vorm, de leden die op de plek gelezen worden en leeg kunnen zijn,
  de invoerleden (die geen vorm schrijft) van waaruit leeg de plek kan bereiken, de afvangingen, en het bronfragment.
- **De test.** Een poot in gate 3: het commando loopt op elke golden en rode `ir.json`, elke pointer bestaat in het
  document, de totalen per klasse en het aantal soorten houden een floor, en elke soort heeft een getuige met een
  vaste uitvoer onder `spec/verifier/test/empty-closures/`.
- **Een derde klasse, apart geteld.** `absent-instance`: een plek waar het ontbreken van een instantie of een link een
  uitkomst beslist. Of een relatie open of gesloten gelezen wordt, is een open beslispunt; de meting laat zien hoe groot
  het is, los van de twee klassen van de kaart.
- **Een telling per invoerlid.** Omdat sluiten per gegeven of relatie zou gebeuren, geeft de meting per invoerlid het
  aantal plekken dat het bereikt.
