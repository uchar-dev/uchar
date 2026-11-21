# empty-closures-inventory: opsommen waar een document de wereld sluit of een ontkenning op leeg laat rusten

*Opgesteld door een werkbanksessie op 2026-11-21, op verzoek van de eigenaar. Stap 1 van drie uit het advies
"Leeg, onbekend en niet van toepassing" (werkbank, `werknotities/20261121-null-logica/rapport.md`, §3 en §4). Alleen
meten; de semantiek verandert niet.*

## Aanleiding

De eigenaar besloot op 2026-11-20 voor de regelset v2/herleving-ww (regelspraak-rules): **uit leeg mag niets afgeleid
worden**. Leeg is "nog niet ingevuld"; pas als een marker (`onbekend`, `niet van toepassing`) is gegeven, of een
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
