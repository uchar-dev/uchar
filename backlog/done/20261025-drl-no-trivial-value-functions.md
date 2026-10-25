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

## Result (2026-10-25)

Tak `card/drl-no-trivial-value-functions`, vanaf main `13988112`, de kaart naar `doing/` in `4707a183`.

- `d5d97d05` de weergave (`DrlRendering`), de README-sectie "The DRL-text branch", de corpusgetuige uitgebreid, twee
  gerichte getuigen nieuw (`cases/DrlValueFunctionsWitnessTest`, `transpiler/tms/DrlCarriedValueWitnessTest`), de
  verwachtingen van `DrlPiecesWitnessTest`, `DrlConditionFunctionsWitnessTest` en `FormsPlan` aangepast.
- `9dde1500` de getuige van een waarde met een sonde leest de golden `jutterswet-ploegaandeel` in plaats van een eigen
  document (de golden toont de vorm).
- `9047f68a` in `DrlCarriedValueWitnessTest` heeft de naam van elke regel over een constante een spatie vóór een punt
  (zie "De vorm van de expressie", mutatie M2b).
- dit Result, in het commit daarna.

May touch gevolgd: `git diff 4707a183 -- backends/drools/src/main` raakt alleen `DrlRendering.java`; verder
`backends/drools/README.md` (alleen de sectie "The DRL-text branch"), DRL-getuigen in `.../cases/` en
`.../transpiler/tms/`, en deze kaart. Geen beschermd pad, geen delta voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: de
draad, de spec, het plan, het exec-model en `ComputeHelpers` veranderen niet.

### Wat de weergave nu schrijft

Alleen in `DrlRendering.Rule`, in `value(Consequence.Value, …)` en `decisionOf`: is de term een `Term.Const` of een
`Term.SlotValue` (`computesNothing`), met of zonder `collapse`, dan reserveert de weergave geen plaats en schrijft zij
geen functie. `carried` schrijft de zin van de term (`t.source()`, door `Namen.commentSafe`) als commentaarregel en
daaronder `Object v = <expressie>;`: de variabele die de binding bindt (de context van de slots, zoals de functie haar
kreeg, dus een slot dat de positie niet doorgeeft faalt zoals voorheen), of de constante (`consequenceConstant`), met
`ComputeHelpers.collapse(…)` waar het plan `collapse` zegt. Daarna:

- bij een besluit `if (v != null) {` en de feiten van de stapel, zonder tak voor een evaluatiefout, ook waar het plan
  een `fault` heeft;
- bij een waarde de sonde vóór de expressie (`Object sonde = ComputeHelpers.depositFlag();`) waar het plan een
  `Deposit` heeft, dan `if (ComputeHelpers.reported(sonde))` met de inconsistentie, dan de waarde (onder
  `if (!ComputeHelpers.isEmpty(v))` of zonder, naar `writesEmpty`), alles op één inspringing, zonder tak voor een
  evaluatiefout.

Een term die iets berekent (ook één aanroep) houdt zijn functie en het `then`-deel zoals het was. Waar geen regel van
een tekst meer een `Fault` invoegt, valt `import dev.uchar.regelspraak.transpiler.tms.Fault;` uit de kop: dat volgt uit
`typeName`, dat alleen importeert wat de tekst noemt (33 goldens).

De javadoc van de klasse (een zin in de tweede bullet) en van `value`, `decisionOf`, `computesNothing`, `carried`,
`consequenceConstant` en `consequenceText`, en de README-sectie "The DRL-text branch" zeggen welke termen geen functie
zijn en hoe een tekst in het gevolg staat.

### De vorm van de expressie in het then-deel

Een constante staat in het gevolg zoals `constant` haar in een functie schrijft (`Boolean.TRUE`,
`new java.math.BigDecimal("…")`, `java.time.LocalDate.parse("…")`, `Integer.valueOf(5)`, `Long.valueOf(5L)`,
`Breuk.van(…)`, het volledige enumlabel, `Period.class`, `java.util.Arrays.asList(…)`, `null`), op een tekst na. Niet
`mvelConstant`:
die schrijft alleen null, tekst, `Boolean`, `Integer`, `ElementKey` en enumlabels, en `Boolean` en `Integer` als `true`
en `5`; één schrijfwijze voor functie en gevolg houdt de constante voor de lezer gelijk, en MVEL 2.5.2 leest elke vorm
van `constant` als dezelfde waarde van dezelfde klasse (gemeten met een probe in een worktree op `4707a183`, log
`werk/drl-no-trivial-value-functions/logs/mvel-probe-1.log`, en vastgelegd in `DrlCarriedValueWitnessTest`, die elke
klasse op beide takken afvuurt).

Een tekst (`consequenceText`, ook in een lijst) wijkt op twee plaatsen af van `Namen.drlString`:

1. Een backslash staat als de Unicode-escape `\u005C` (backslash, `u`, `005C`). Drools 8.44 bereidt een MVEL-gevolg voor
   met `MVELConsequenceBuilder.delimitExpressions` (gedecompileerd uit `drools-mvel-8.44.0.Final.jar`): die wisselt bij
   elk `"` dat niet direct na een backslash staat tussen "in een literal" en "erbuiten", en ziet dus het sluitende `"`
   van `"…\\"` niet. Daarna leest zij de rest van het gevolg verkeerd om: een spatie of tab vóór een punt in een
   volgende literal valt weg (zij houdt die literal voor code), en een commentaarregel of `#` erna neemt zij voor een
   deel van een literal. Gemeten met mutatie M2b (backslash als `\\`): de schrijver en de stapel van elke regel met zo'n
   tekst, met een naam als `waarde .2`, worden `waarde.2`, zonder foutmelding. MVEL leest in `\uXXXX` alleen
   hoofdletters A–F (`ParseTools.handleEscapeSequence`); `\u005c` geeft "illegal unicode escape sequence" (mutatie M4).
2. Een reeks U+0000 staat in een eigen literal, met `+` aan de rest gekoppeld (`"n" + "\0" + "\"z"`). MVEL
   (`ParseTools.handleStringEscapes`) zet een verwerkte escape op U+0000 en haalt daarna elke U+0000 uit een literal met
   ten minste één escape, ook de echte: `"n\0\"z"` wordt `n"z\0` (gemeten, mutatie M2 en M3). Een literal zonder escape
   houdt U+0000.

Een aanhalingsteken, CR en LF blijven `\"`, `\r`, `\n`. De commentaarregel gaat door `Namen.commentSafe` (CR en LF
worden spaties, `*/` wordt `* /`), zonder de vervanging van `\u` die `sentences` in een functielichaam doet: MVEL leest
geen Unicode-escape in commentaar; de zin van de gerichte getuigen bevat `\u005c` en `\u12`, wat de Java-compiler
weigert, en Drools leest erlangs.

Het `then`-deel krijgt altijd `Object v = <expressie>;` en niet de expressie direct in de feiten: de feiten van een
stapel lezen `v` (`stackFact`, ook `(String) v`), de commentaarregel staat boven één statement, en `v` is in elk gevolg
de waarde (voorlopig besluit 3).

### De tak `isEvaluationFault(v)`

Hij verdween bij een term die niets berekent, bij een besluit en bij een waarde.

- Kan de expressie falen? Nee. Een gebonden variabele lezen faalt niet; een constante wordt gemaakt uit haar eigen
  tekst (`new BigDecimal(d.toString())`, `LocalDate.parse(d.toString())`, `Breuk.van` met een positieve noemer,
  `Integer.valueOf`, een literal, een enumlabel, een klasse, `Arrays.asList`); `ComputeHelpers.collapse(v)` is
  `v instanceof Breuk b ? b.naarDecimal() : v`, en `Breuk.naarDecimal()` geeft het exacte decimaal waar de breuk
  eindigt en anders het DECIMAL64-quotiënt, "Never throws on a non-terminating value" (javadoc in `Breuk`).
- Wat zegt `x.fault()`? Bij een waarde staat er altijd een (`TmsTranspiler.writeRule`, r. 2108); bij een besluit één
  bij een berekend attribuut-default (`collapse` waar) en geen bij een kenmerk-default en bij `eens` (r. 3636, 3758,
  3768). De fout van het plan betekent: waar de evaluatie een evaluatiefout gooit, voeg de `Fault` in in plaats van de
  waarde. Het exec-model doet dat met een `catch` om de evaluatie (`PatternDslRendering.value`, `decisionOf`); de
  DRL-tekst deed het met de `try` en `ComputeHelpers.caught(e)` in de functie, die de uitzondering als uitkomst gaf, en
  de tak in het gevolg die haar herkende. Zonder functie is er geen `try` (MVEL heeft er geen) en gooit de expressie
  niets. `isEvaluationFault(v)` zou dan alleen vragen of de waarde zelf een `Throwable` is; het exec-model vraagt dat
  niet en zou zo'n waarde schrijven. De tak kan dus niet lopen voor een waarde van het plan, en waar hij zou lopen,
  week hij af van het exec-model.
- `collapse` met een `Breuk` maakt er een `BigDecimal` van (exact of DECIMAL64) en faalt niet; met elke andere waarde
  geeft hij die waarde terug.

De sonde van het plan blijft (voorlopig besluit 2).

### Getuigen

(a) `DrlPiecesCorpusWitnessTest`, uitgebreid. `ownWork` toetst per functie van de soort `afloop` of `waarde`
(`VALUE_KINDS`) dat haar lichaam, zonder commentaarregels en zonder de regels van de standaard-`try`
(`VALUE_FRAME`), niet alleen `return E;` is met `E` een constante (`CONSTANT`, de vormen die de tekst voor een constante
schrijft, in een functie en in een gevolg), een parameter van de functie, of `ComputeHelpers.collapse` van een van
beide. Een functie van een andere soort met zo'n lichaam (de sleutel van een `conflict`) telt apart en mag. Nieuw
`carriedValue`: per regel elke regel `Object v = E;` van het gevolg met `E` een constante, een variabele die de regel
bindt (`$…` in het deel vóór `then`) of `collapse` van een van beide, geteld, apart met de commentaarregel erboven; zo'n
gevolg met `ComputeHelpers.isEvaluationFault(v)` is een afwijking. Tellingen per bouw, zonder en met lezingen gelijk:

| telling | op `4707a183` | op `9047f68a` |
|---|---|---|
| functies van een waarde die niets berekent (fout) | 330 (182 `afloop`, 148 `waarde`) | 0 |
| functies van een andere soort die niets berekenen (`conflict`, mag) | 19 | 19 |
| gevolgen die hun waarde dragen | 0 | 330 |
| gevolgen die hun waarde dragen, met de zin erboven | 0 | 258 |

De 330 per bouw: `afloop` 62 `collapse` van een constante (met `try` en zin), 48 `collapse` van een variabele (idem),
41 een constante en 31 een variabele (zonder `try` en zin); `waarde` 72 `collapse` van een constante en 76 van een
variabele (met `try` en zin). Van de 330 gevolgen hebben er 10 een sonde (o.a. `jutterswet-ploegaandeel`,
`jutterswet-seinwacht`). Rood op `4707a183` (de getuigen van de tip in een worktree op `4707a183`, log
`werk/drl-no-trivial-value-functions/logs/red-3.log`): "functies zonder eigen werk: 660" (330 per bouw).

(b) Gericht, twee klassen.

`cases/DrlValueFunctionsWitnessTest`, vijf tests; per regel de tabel van zijn stuk (`DrlPiecesWitnessTest.table`) en
het gevolg letterlijk; de cases van elke gebruikte golden spelen op beide takken (`CaseRunner.viaExecModel` en
`viaDrlText`):

1. `a_decision_that_computes_nothing_is_carried_by_its_consequence`: een constante zonder fout in het plan
   (`jutterswet-uitsluiting-rgs`, `defaults K:Jutter.uitgesloten: default 0.0.0`, `Object v = Boolean.TRUE;`), een
   constante met `collapse` en fout (`anchor/jutterswet-nestdiepte`, `defaults A:Jutter.heffing: default 0.0.0`,
   `// 10.00` en `Object v = ComputeHelpers.collapse(new java.math.BigDecimal("10"));`), een gebonden variabele zonder
   fout (`eens 0.1.0` daar, `Object v = $waarde;`), een gebonden variabele met `collapse` en fout
   (`anchor/jutterswet-strandrechtketen`, `defaults A:Strandrecht.uitkering: default 1`).
2. `a_value_that_computes_nothing_is_carried_by_its_consequence`: een constante (`registratiekosten van een
   strandrecht`), een gebonden variabele langs een pad (`anchor/jutterswet-kustwacht`, `uitkijkbonus van een wachter`),
   een tekst met aanhalingstekens onder een zin met aanhalingstekens (`jutterswet-keurmerk`, `afslag van een gekeurde
   vondst`).
3. `a_value_with_a_probe_that_computes_nothing_keeps_its_probe`: `jutterswet-ploegaandeel`, `bergingsaandeel van een
   jutter`: de voorwaarde deelt, dus de regel heeft een sonde en het plan geeft die ook aan het waardegevolg
   (`reads.depositFlagSlot()`, r. 2102); de constante `1` meldt er niets aan; het gevolg houdt
   `Object sonde = …` en `if (ComputeHelpers.reported(sonde))`.
4. `a_value_that_computes_something_keeps_its_function`: een besluit van één aanroep
   (`defaults A:Wrakrecht.uitkering: default 2`, `rsMaal`) en een waarde van één aanroep
   (`anchor/jutterswet-leegrekening`, `bruto van een vondst`, `rsPlus`) houden hun functie met `try` en het gevolg met
   `isEvaluationFault(v)`. Groen op `4707a183`: hij pint de vorm die blijft.
5. `a_carried_text_and_its_sentence_are_read_as_written`: een document per tekst, de golden `jutterswet-keurmerk` met
   een andere tekst als afslag en een andere zin erboven (`zin met 'apostrof', "aanhalingsteken", {accolade, (haakje,
   \u005c, \u12, end, then, #, // en /* en */` en een regeleinde); teksten: eindigend op een backslash, `n\0"z`,
   `a\0\`, `\u0041` als tekst, `h//x #y /*z*/ end`. De case van de golden, met die afslag, speelt op beide takken; de
   commentaarregel en de expressie staan er letterlijk (`"eindigt op een backslash \u005C"`, `"n" + "\0" + "\"z"`,
   `"a" + "\0" + "\u005C"`, `"\u005Cu0041"`).

Voor (b) "met en zonder fout in het plan": de analyse maakt bij een besluit een fout alleen samen met `collapse`
(attribuut-default) en bij een waarde altijd een fout met `collapse`; die vormen staan op goldens. De andere
combinaties (een besluit over een constante met fout zonder `collapse`, een waarde zonder `collapse`) laat het record
toe en maakt de analyse niet; die staan in de tweede klasse. Een waarde zonder fout in het plan kan niet: `value` leest
`x.fault().form()`, ook vóór de kaart.

`transpiler/tms/DrlCarriedValueWitnessTest`, één test: een plan met de hand over de Wachter van
`anchor/jutterswet-kustwacht`, met per constante van `CONSTANTS` (26: null, elf teksten waaronder `""`, een backslash
aan het eind, U+0000 naast een escape en `€ë日𝔘`, `Boolean.TRUE`/`FALSE`, `Integer`, `Long`, `BigDecimal`, `BigInteger`,
`Breuk`, `LocalDate`, `LocalDateTime`, twee enums, `Period.class`, een lege lijst en een lijst met een tekst, null, een
`Boolean` en een getal) een waardegevolg met en zonder `collapse` en een besluit met en zonder `collapse` en fout; de
gebonden uitkijkbonus in dezelfde vormen; en een waardegevolg met een sonde over een constante en over de gebonden
waarde. 164 regels, elk met de zin van de term (dezelfde tekens als hierboven). De tekst bevat geen `function ` en geen
`isEvaluationFault`, elke regel heeft de commentaarregel boven `Object v = `, en het plan vuurt via het exec-model
(`PatternDslRendering.model`) en via de DRL-tekst (`KieHelper`) op een Wachter met en een zonder uitkijkbonus: beide
geven dezelfde feiten, en die het plan zegt (de waarde van dezelfde klasse, gelijk aan de constante, ingeklapt waar
het plan inklapt; een besluit over null niets). De naam van elke regel over een constante heeft een spatie vóór een
punt (`waarde .2`), zodat een gevolg dat de voorbereiding verkeerd om leest een andere schrijver geeft.

Rood op `4707a183` (`logs/red-3.log`): 11 van de 29 tests in de zes klassen: `DrlValueFunctionsWitnessTest` 1, 2, 3 en 5
(tabel met `afloop_…`/`waarde_…`), `DrlCarriedValueWitnessTest` (de tekst declareert functies), de corpusgetuige,
twee van `DrlPiecesWitnessTest`, twee van `DrlConditionFunctionsWitnessTest` en
`DrlRenderingWitnessTest.every_form_of_the_defaults_compiles` (fragmenten van `FormsPlan`).

Mutaties van de wijziging in `DrlRendering.java`, in een worktree op `9047f68a`
(`werk/drl-no-trivial-value-functions/mut`, niet gecommit; na elke mutatie `git checkout`; recept
`werk/drl-no-trivial-value-functions/mutations/run.py` met de vervangen tekst per mutatie, logs
`werk/drl-no-trivial-value-functions/logs/mutations/mutation-M*.log`, overzicht `mutations-3.log`). Gedraaid: de
corpusgetuige, de twee nieuwe klassen, `DrlPiecesWitnessTest`, `DrlConditionFunctionsWitnessTest`,
`DrlRenderingWitnessTest` en `TextLexemeWitnessTest` (40 tests). Zonder mutatie is alles groen.

| mutatie | rood |
|---|---|
| M1 elke waarde een functie, zoals vóór de kaart (`computesNothing` geeft false) | 11: de corpusgetuige ("functies zonder eigen werk: 660"), `DrlValueFunctionsWitnessTest` 1, 2, 3, 5, `DrlCarriedValueWitnessTest`, de vier aangepaste verwachtingen van (e), `every_form_of_the_defaults_compiles` |
| M2 een tekst zoals `Namen.drlString` hem schrijft | `DrlValueFunctionsWitnessTest` 5 (`n\0"z` leest als `n"z\0`), `DrlCarriedValueWitnessTest` (de teksten met U+0000 en de schrijvers van de teksten met een backslash aan het eind) |
| M2b alleen de backslash als `\\` | `DrlValueFunctionsWitnessTest` 5 (de letterlijke expressie), `DrlCarriedValueWitnessTest`: schrijver en stapel `waarde .2` worden `waarde.2` bij de constanten 2, 3, 6 en 25 |
| M3 U+0000 niet in een eigen literal | `DrlValueFunctionsWitnessTest` 5, `DrlCarriedValueWitnessTest` (U+0000 verschuift naar het eind) |
| M4 `\u005c` met kleine hexcijfers | 3 fouten: de tekst compileert niet ("illegal unicode escape sequence"): `DrlValueFunctionsWitnessTest` 5, `DrlCarriedValueWitnessTest`, `TextLexemeWitnessTest.a_value_that_ends_with_a_backslash_plays_in_both_branches` |
| M5 een besluit dat zijn waarde draagt houdt de tak van een fout | de corpusgetuige ("functies zonder eigen werk: 364", "draagt zijn waarde … en toetst haar op een fout", 182 per bouw), `DrlValueFunctionsWitnessTest` 1, `DrlCarriedValueWitnessTest`, `DrlPiecesWitnessTest` (terugval), `every_form_of_the_defaults_compiles` |
| M6 geen zin boven de expressie | de corpusgetuige (eerst de vloer van de teksten buiten ASCII: 48, vloer 49), `DrlValueFunctionsWitnessTest` 1, 2, 3, 5, `DrlCarriedValueWitnessTest`, `DrlConditionFunctionsWitnessTest.a_default_without_a_condition_has_no_test` |
| M7 geen sonde bij een waarde die haar waarde draagt | `DrlValueFunctionsWitnessTest` 3 |
| M8 een term van één aanroep berekent niets | `DrlValueFunctionsWitnessTest` 4, `every_form_of_the_defaults_compiles`, en drie fouten op het corpus: "een plaats in factor van een jutter meldt aan slot 1, niet aan de sonde van haar positie" (de context van `carried` geeft geen sonde door) |
| M9 de zin zonder `commentSafe` | `DrlCarriedValueWitnessTest` en drie fouten: de tekst compileert niet waar de zin een regeleinde heeft (`DrlValueFunctionsWitnessTest` 5, twee van `TextLexemeWitnessTest`) |

(c) Geen gedragsverandering.

- `BranchDumpsCorpusWitnessTest` is ongewijzigd (`git diff 4707a183` raakt hem niet) en groen in de suite (2 tests).
- De dumps "planvorm" en "modelvorm" bestaan niet als gereedschap (zie het Result van `drl-no-passthrough-functions`);
  in plaats daarvan raakt `git diff 4707a183 -- backends/drools/src/main` alleen `DrlRendering.java`: de analyse, het
  plan en het exec-model zijn dezelfde.
- Alleen de functies van een waarde die niets berekent en hun gevolgen veranderen: met een probe-testklasse (niet
  gecommit, dezelfde als in de twee vorige kaarten, `werk/drl-no-trivial-value-functions/probe/`) de tekst en de
  stukken van elke golden op de tak `DRL_TEXT`, zonder en met lezingen, op `4707a183` en op de tip. `compare.py`
  bouwt per stuk de verwachte nieuwe tekst uit de oude: de functies min de functies `afloop`/`waarde` waarvan het
  lichaam niets berekent, en het gevolg met de aanroep vervangen door de teruggegeven expressie, de commentaarregel
  van de functie erboven en de tak van een fout eruit (het `else`-blok een stap terug). Corpus: 564 teksten, 660
  functies weg, 214 teksten veranderen, 350 gelijk, 66 koppen missen alleen `import …Fault;` (en die tekst noemt
  `Fault` niet meer), 94 weigeringen bytegelijk, 0 andere verschillen. v2: 58 functies weg, 0 andere verschillen.
- Gate 12: zie hieronder.

(e) Aangepaste verwachtingen, oud → nieuw:

- `DrlPiecesCorpusWitnessTest.EXPECTED_MIN`, gemeten 2026-10-25: `functies` 2201 → 1871, `functies met commentaar`
  1680 → 1422, `functies met lezingen` 3158 → 2828, `stukken zonder functies` 763 → 879; nieuw `stukken zonder functies
  met lezingen` 879, `gevolgen die hun waarde dragen` 330 (en met lezingen 330), `gevolgen die hun waarde dragen, met
  de zin erboven` 258 (en met lezingen 258). Ongewijzigd: `stukken` 2124, `stukken met lezingen` 3046, `functies die
  een functie van hun stuk aanroepen` 241, de namen met een nummer 261 en 265, de goldens 282, de teksten buiten ASCII
  49, `EXPECTED_UNREACHED` (de binder van `bergingsforfait: default 0`, een `Let`, blijft). De javadoc noemt de nieuwe
  check en de meting op `4707a183`.
- `DrlPiecesWitnessTest.a_composite_condition_is_the_body_of_its_test`: de tabel van `defaults K:Jutter.uitgesloten:
  default 0.0.0` was `toets_…Default000`, `afloop_defaultsKJutterUitgeslotenDefault000`, regel roept beide; is
  `toets_…Default000`, regel roept `toets_…Default000`.
- `DrlPiecesWitnessTest.the_fall_back_of_a_default_carries_the_functions_of_its_test_and_its_value` heet nu
  `…_carries_the_function_of_its_test_and_its_value_in_its_consequence`; de tabel van `terugval 0` was
  `toets_…Terugval0`, `afloop_defaultsKJutterUitgeslotenTerugval0`, regel roept beide; is `toets_…Terugval0`, regel
  roept `toets_…Terugval0`. De letterlijke functie `function Object afloop_…Terugval0() { return Boolean.FALSE; }` is
  het letterlijke gevolg `Object v = Boolean.FALSE;` met `if (v != null)` en de `Outcome`. De javadoc van de klasse
  zegt dat de waarde van die terugval een constante is die het gevolg draagt.
- `DrlConditionFunctionsWitnessTest.a_test_over_a_composite_is_one_function_where_its_role_asks_the_verdict`: de
  tabel van `default 0.0.0` verliest `afloop_defaultsKJutterUitgeslotenDefault000`.
  `a_default_without_a_condition_has_no_test`: de tabel van `defaults A:Jutter.bergingstoeslag: default 1` was
  `afloop_defaultsAJutterBergingstoeslagDefault1 | // 0.00`, regel roept haar; is `regel | roept []`, en het gevolg
  begint met `// 0.00` en `Object v = ComputeHelpers.collapse(new java.math.BigDecimal("0"));` (nieuwe bewering).
- `FormsPlan.DEFAULTS_FORMS`: weg `Object afloop_default0(Object $uitkijkbonus)`, de `try` van `afloop_default0`,
  `Object afloop_default1() {…return Boolean.TRUE;…}` en `Object afloop_default2() {…return Boolean.FALSE;…}`; nieuw de
  gevolgen `Object v = ComputeHelpers.collapse($uitkijkbonus);` met `Value` en `Decides 0`, `Object v = Boolean.TRUE;`
  met `HasCharacteristic`, `Object v = Boolean.FALSE;` met `Decides 2`, `Object v = $waarde;` met de `Outcome`. Het
  plan `defaults` kreeg de regel `default 3`, een berekend besluit van één aanroep (`ComputeHelpers.fallback`) met
  `collapse` en fout, zodat de vorm met functie, `try` en de tak `isEvaluationFault(v)` in het defaultsplan blijft; de
  fragmenten daarvan (`// zijn uitkijkbonus of 0`, `function Object afloop_default3(Object $uitkijkbonus) {`, de `try`,
  de aanroep met de tak van een fout, `Decides 3`) vervangen die van `default 0`. Ongewijzigd: `conflict_verschil0`, de
  `Outcome` van een conflict, de inconsistentie EF-027. De javadoc van `defaults` zegt het.
- `DrlFunctionNamesWitnessTest`, `DrlTextWitnessTest`, `KeyConventionWitnessTest`, `TextLexemeWitnessTest` en de andere
  vormenplannen veranderen niet.

### (d) De meting op v2/herleving-ww

Regels `ece012e`; bronnen in resolve-volgorde (`fundament/rules/*.rgs` gesorteerd, dan `herleving-ww/rules/*.rgs`
gesorteerd); het IR gemaakt met het frontend van deze worktree (`make ir`), bytegelijk aan
`werk/drl-function-names-from-rule/v2/ir.json`; de tekst met de probe, zonder lezingen. Niets geschreven in de werkbank.

- 339 regels; functies 416 vóór, 358 na. Weg: 52 `afloop` en 6 `waarde`, zoals de tabel van de kaart: 26 `afloop`
  `collapse` van een variabele (met `try` en zin), 18 `afloop` `Boolean.TRUE`/`FALSE`, 8 `afloop` `return $waarde;`
  (`eens`), 5 `waarde` `collapse` van een variabele, 1 `waarde` `collapse` van `new java.math.BigDecimal("1")`.
- Tellingen van (a) (`probe/trivial.py`, dezelfde toetsen als de corpusgetuige; op het corpus geeft het dezelfde
  getallen): vóór 58 functies van een waarde die niets berekent, 0 van een andere soort, 0 gevolgen die hun waarde
  dragen; na 0, 0, 58 gevolgen die hun waarde dragen, waarvan 32 met de zin erboven en 0 met een sonde.
- `compare.py`: 58 functies weg, verder niets anders.

Het stuk van `defaults A:Eindigingsgrond.herlevingsdatum: eens 2` na de wijziging (vóór: de functie
`afloop_defaultsAEindigingsgrondHerlevingsdatumEens2` met `return $waarde;` en `Object v = afloop_…Eens2($waarde);`):

```java
rule "defaults A:Eindigingsgrond.herlevingsdatum: eens 2"
    dialect "mvel"
when
    $besluit : Outcome( stack == "defaults A:Eindigingsgrond.herlevingsdatum" && kind == dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.DECIDED && chain >= 1 && chain < 2, $onderwerp : subject, $waarde : value )
    not( $hoger_1222 : Outcome( stack == "defaults A:Eindigingsgrond.herlevingsdatum" && chain == 0 && position < 2, subject != null, subject == $onderwerp ) )
then
    Object v = $waarde;
    if (v != null) {
        insertLogical(new Outcome($onderwerp, "defaults A:Eindigingsgrond.herlevingsdatum", 0, 2, dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.DECIDED, v, null));
    }
end
```

Het stuk van `defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1` na de wijziging
(vóór: zoals in de Aanleiding van deze kaart):

```java
rule "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1"
    dialect "mvel"
when
    $onderwerp : dev.uchar.regelspraak.facts.Eindigingsgrond( ComputeHelpers.classIs(this, "dev.uchar.regelspraak.facts.Eindigingsgrond") )
    $stand_0 : State( attribute == ElementKey.of("eindigingsgrond", "berekende datum aanvang eindigingsgrond recht op uitkering", false), subject == $onderwerp, $berekendeDatumAanvangEindigingsgrondRechtOpUitkering : value )
    not( $hoger_1250 : Decides( stack == "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering" && index < 1, subject == $onderwerp ) )
then
    // zijn berekende datum aanvang eindigingsgrond recht op uitkering
    Object v = ComputeHelpers.collapse($berekendeDatumAanvangEindigingsgrondRechtOpUitkering);
    if (v != null) {
        insertLogical(new Value($onderwerp, ElementKey.of("eindigingsgrond", "datum aanvang eindigingsgrond recht op uitkering", false), v, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering"));
        insertLogical(new Decides($onderwerp, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering", 1));
    }
end
```

Drools 8.44 leest beide teksten: de probe bouwt de tak `DRL_TEXT` met `KieHelper`, zonder fout, op v2 en op elke
gedragen golden; gate 12 speelt de 279 goldens op die tak.

### Voorlopige besluiten

1. De tak `isEvaluationFault(v)` verdwijnt bij een term die niets berekent, ook waar het plan een fout heeft (zie
   boven). Hem laten staan zou een vraag in de tekst zetten die het exec-model niet stelt.
2. De sonde blijft bij een waarde die haar waarde draagt: het plan geeft haar (`Deposit`), en het exec-model maakt haar
   ook en vraagt `reported`. Zij hoort niets (een constante of variabele heeft geen plaats die deponeert), dus de tak
   `if (ComputeHelpers.reported(sonde))` loopt nooit; de weergave schrijft wat het plan zegt, zoals zij een
   samengestelde voorwaarde over geen lid niet bij het renderen invult. Op het corpus 10 gevolgen, op v2 geen.
3. Het gevolg krijgt `Object v = <expressie>;` en houdt `if (v != null)`, ook bij een constante die niet null is: de
   weergave rekent de waarde niet uit bij het renderen ("optie 2"), en de feiten van de stapel lezen `v`.
4. `collapse` blijft staan waar het plan het zegt, ook om een constante die geen `Breuk` is
   (`ComputeHelpers.collapse(new java.math.BigDecimal("5"))`, `ComputeHelpers.collapse("veiling \"Zee\"")`): de
   weergave schrijft het plan.
5. Een tekst in het gevolg: backslash als `\u005C`, U+0000 in een eigen literal (zie boven). Alternatief was een functie
   houden voor een tekst die MVEL anders leest; dat zou de regel "een constante heeft geen functie" een uitzondering
   geven, terwijl MVEL elke tekst kan lezen in deze vorm.
6. De commentaarregel in het gevolg gaat door `commentSafe` zonder de `\u`-vervanging van een functielichaam: het
   gevolg bereikt de Java-compiler niet.
7. De corpusgetuige toetst alleen de soorten `afloop` en `waarde`; de 19 functies `conflict` per bouw die een
   constante of gebonden sleutel teruggeven, vallen buiten de kaart en tellen apart.
8. De gerichte getuige van een waarde met een sonde leest een golden (`jutterswet-ploegaandeel`) in plaats van een
   document, omdat de opdracht een document alleen vraagt waar geen golden de vorm toont. Op het corpus staat geen
   golden met een besluit over een constante met fout zonder `collapse` of een waarde zonder `collapse`; de analyse
   maakt die niet, en `DrlCarriedValueWitnessTest` toont ze op een plan met de hand.

### Voor de toolchain-kaart `drl-value-functions-pin`

`PlaygroundDrlRulesTest` pint 24 functies en per regel zijn functies; `afloop_defaultsAVondstWaardeEens0` en
`waarde_melding` vallen weg als hun lichaam niets berekent. `MIN_FUNCTIONS` (2201) telt de functies van het corpus
zonder lezingen; hier zijn dat er nu 1871. Het voorbeeld in `docs/ENGINE.md`
(`afloop_defaultsAEindigingsgrondHerlevingsdatumTerugval2`) berekent een datum en blijft. De velden van `drlRules`
veranderen niet.

### Suite en gate 12

- De JUnit-suite van `backends/drools` (`mvn -B -o test`) op `d5d97d05`: 1708 tests, 0 fouten, 0 overgeslagen; de 1702
  van `4707a183` plus vijf in `DrlValueFunctionsWitnessTest` en één in `DrlCarriedValueWitnessTest`.
  `BranchDumpsCorpusWitnessTest` (2), `DrlRenderingWitnessTest` (9), `TextLexemeWitnessTest` (11) groen. Log
  `werk/drl-no-trivial-value-functions/logs/suite-1.log`. De commits erna veranderen alleen getuigen; hun suite liep
  in gate 12.
- Gate 12 (`scripts/check-drools.sh`) op `d5d97d05` en opnieuw op `9047f68a`: PASS, beide keren. 279 goldens gespeeld
  op de exec-model-tak, gelijk aan `EXPECTED_PLAYED=279`; kanaalvloer 50 niet-gespeelde goldens bij naam; de
  DRL-teksttak speelt 279, elke statusregel gelijk aan die van de exec-model-tak (sweep 31 s); de suite van de gate 1708
  tests (vloer 1573), nul overgeslagen; `check-drools-codes` PASS (294 codes). Het rapport is regel voor regel gelijk
  aan dat van een eerdere run van gate 12 van vandaag (`werk/gates-design/suite-forks/gates-1/12-drools.log`, 15:07),
  op de wandkloktijd van de sweep (222 s daar, vóór de parallelle replay) en het aantal tests (1702 daar) na:
  dezelfde weigeringen met dezelfde codes per vorm, dezelfde aantallen. Logs
  `werk/drl-no-trivial-value-functions/logs/gate12-1.log` en `gate12-2.log`.
- De volledige gate-set draaide hier niet; die draait de hoofdsessie vóór de landing (acceptatie (g)). Gate 13 en 17
  gebruiken de DRL-teksttak ook; gate 13 vergelijkt statusregels, en de lezingen van gate 17 komen uit het plan
  (`Consequence.Reading`), niet uit de functies van een waarde; `BranchDumpsCorpusWitnessTest` vergelijkt ook de
  lezingen van beide takken en is groen.
- De commits na `9047f68a` veranderen alleen de kaart.

### Landing (hoofdsessie)

- `make gates` op `0dc84233`: 17 van 17 groen plus `after-run`, in 287 s (acceptatie (g)). Log
  `werk/drl-no-trivial-value-functions/landing-make-gates.log`.
- Nagegaan door de hoofdsessie, niet gecommit: `DrlCarriedValueWitnessTest` met als zin één aanhalingsteken
  (`zin met een enkel " aanhalingsteken`) en met een backslash vóór een aanhalingsteken en aan het eind blijft groen;
  de regelnamen met een spatie vóór een punt komen ongeschonden door, dus de voorbereiding van een gevolg leest de
  commentaarregel niet als begin van een literal.
