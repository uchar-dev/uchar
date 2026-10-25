# drl-no-trivial-value-functions: een waarde die niets berekent, staat in het then-deel en niet in een functie

*Voorstel van de werkbank (2026-10-25), op besluit van de eigenaar. Niet gecommit tot de bouw. Volgt op
`done/20261025-drl-no-passthrough-functions` en `done/20261025-drl-function-names-from-rule`. Vervolg in de toolchain:
`backlog/new/20261025-drl-value-functions-pin.md`.*

## Aanleiding

Na `drl-no-passthrough-functions` staat een functie voor een voorwaarde er alleen nog waar zij iets toetst. Functies
voor een waarde vielen buiten die kaart: die zette "een term die niet kan falen inline" bewust buiten haar scope
("optie 2", op 2026-10-13 afgewezen met normvraag 1 van Fable-advies 2, "altijd een functie"). Op 2026-10-25 zag de
eigenaar in de werkbank deze functie, en die berekent niets:

```java
function Object afloop_defaultsAEindigingsgrondHerlevingsdatumEens2(Object $waarde) {
    return $waarde;
}
```

De eigenaar besliste (2026-10-25) dat ook een waarde die niets berekent geen eigen functie krijgt. Dat is een smalle
uitzondering op "altijd een functie". Een term die wél iets berekent, blijft een functie, ook als het maar één aanroep
is.

Meting op v2/herleving-ww (regels `ece012e`, uchar `321fb239`, toolchain `cc547050`): 416 functies. Daarvan zijn er 58
van de soort `afloop` of `waarde` waarvan het lichaam, binnen of buiten de standaard-`try`, alleen dit doet:

| lichaam | aantal | soort | `try` | zin |
|---|---|---|---|---|
| `return $x;` (een gebonden variabele) | 8 | `afloop` (alle in een regel `eens …`) | nee | nee |
| `return ComputeHelpers.collapse($x);` | 31 | 26 `afloop`, 5 `waarde` | ja | ja |
| `return Boolean.TRUE;` of `return Boolean.FALSE;` | 18 | `afloop` (defaults van een kenmerk) | nee | nee |
| `return ComputeHelpers.collapse(new java.math.BigDecimal("…"));` | 1 | `waarde` | ja | ja |

Het `then`-deel van de eigen regel roept elk van deze 58 functies aan, en verder niets. In de weergave zijn dat
`decisionOf` (`afloop`, r. 1407) en `value` (`waarde`, r. 1259) van `DrlRendering`, met als term een `Term.Const` of
een `Term.SlotValue`, met of zonder `x.collapse()`. De `try` om `collapse` is nooit nodig: `ComputeHelpers.collapse`
maakt alleen van een `Breuk` een decimaal getal, en `Breuk.naarDecimal()` "never throws".

Voorbeeld, de regel `defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1`:

```java
// zijn berekende datum aanvang eindigingsgrond recht op uitkering
function Object afloop_defaultsAEindigingsgrondDatumAanvangEindigingsgrondRechtOpUitkeringDefault1(Object $berekendeDatumAanvangEindigingsgrondRechtOpUitkering) {
    try {
        return ComputeHelpers.collapse($berekendeDatumAanvangEindigingsgrondRechtOpUitkering);
    } catch (RuntimeException e) {
        return ComputeHelpers.caught(e);
    }
}
…
then
    Object v = afloop_defaultsAEindigingsgrondDatumAanvangEindigingsgrondRechtOpUitkeringDefault1($berekendeDatumAanvangEindigingsgrondRechtOpUitkering);
```

Goal: een waarde die niets berekent, krijgt geen functie. Het `then`-deel draagt haar expressie zelf. Termboom en
semantiek blijven gelijk.

- Is de term van een `Consequence.Decision` of een `Consequence.Value` een constante of een gebonden variabele, met of
  zonder `collapse`, dan schrijft de weergave geen functie. Het `then`-deel krijgt `Object v = <expressie>;`, of
  gebruikt de expressie direct waar dat leesbaarder is. Waar de expressie in het MVEL-dialect van het `then`-deel een
  andere vorm nodig heeft dan in een Java-functie (`mvelConstant`, r. 341), kiest de bouwer die vorm en zegt waarom.
- Kan de expressie niet falen, dan vraagt de bouwer na of de tak `isEvaluationFault(v)` in het `then`-deel nog kan
  lopen. Zo niet, dan mag hij weg; het `Result:` zegt dan waarom. Laat de bouwer de tak staan, dan zegt het `Result:`
  dat ook.
- Er gaat geen RegelSpraak-zin verloren. De zin van de term komt als commentaarregel boven het statement in het
  `then`-deel, en het `Result:` laat zien dat Drools 8.44 die tekst leest.
- `piece` van de regel noemt de vervallen functies niet meer. Dat volgt vanzelf uit de weergave.

Acceptance:
- (a) Een getuige over het corpus, voor elke golden die de backend als DRL-tekst draagt: geen functie waarvan het
  lichaam, binnen of buiten de standaard-`try`, alleen een constante, een gebonden variabele of `collapse` van een van
  beide teruggeeft.
- (b) Rood eerst, gericht: een `afloop` en een `waarde` met een constante, met een gebonden variabele, met `collapse`
  van elk, en met en zonder fout in het plan (`x.fault()`); een waarde met een deposito, als het plan dat bij zo'n term
  kan maken (anders zegt het `Result:` waarom niet); een zin die als commentaarregel in het `then`-deel staat en door
  Drools gelezen wordt; en een term die wél iets berekent (één aanroep), die een functie blijft.
- (c) Geen gedragsverandering: `BranchDumpsCorpusWitnessTest` ongewijzigd groen, `planvorm` en `modelvorm` gelijk aan
  hun nulmeting (het plan verandert niet), gate 12 met dezelfde statusregel per golden en dezelfde `EXPECTED_PLAYED`.
- (d) In het `Result:` de meting op v2/herleving-ww vóór en na: het aantal functies (verwacht 416 − 58 = 358), de
  tellingen van (a), en het stuk van `defaults A:Eindigingsgrond.herlevingsdatum: eens 2` en van `default 1` hierboven
  na de wijziging.
- (e) De vloeren van `DrlPiecesCorpusWitnessTest` en de verwachte functies in `DrlPiecesWitnessTest`, `FormsPlan` en
  `DrlConditionFunctionsWitnessTest` veranderen, omdat deze kaart het aantal functies verandert. Het `Result:` noemt
  elke aangepaste verwachting.
- (f) De javadoc van de klasse en de README-sectie "The DRL-text branch" zeggen dat een waarde die niets berekent geen
  functie is.
- (g) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/DrlRendering.java`; de DRL-getuigen in
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/` en `.../transpiler/tms/`; de sectie "The DRL-text branch"
van `backends/drools/README.md`; deze kaart.

Out of scope:
- Een term die iets berekent, ook als het één aanroep is: `pad` met één `ComputeHelpers.along` (40 op v2), een `toets`
  met één hulpaanroep (`emptyTest`, `characteristic`, `textEqual`), een `waarde` met één berekening. Dat blijft "altijd
  een functie".
- Functies van een andere soort dan `afloop` en `waarde`, ook als ze een constante teruggeven (`lijst`).
- De namen van de functies en hun vorm (`drl-function-names-from-rule`); nummers die verschuiven doordat een functie van
  dezelfde soort wegvalt, volgen de bestaande regel.
- De analyse en het plan (`TmsTranspiler`, `plan/`), de exec-modelweergave, `ComputeHelpers`.
- MVEL-operatoren; besluit 5 blijft staan.
