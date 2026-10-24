# drl-rule-functions: de DRL-tekst zegt per regel welke functies bij hem horen

*Voorstel van de werkbank (2026-10-24). De eigenaar vroeg de werkbank-sessie deze kaart te schrijven en zelf te bouwen;
zij begint na de landing van `drl-rule-part`, omdat `Rendered` en `originOf` ernaast liggen. Niet gecommit tot de bouw.
Vervolg in de toolchain: `backlog/new/20261024-drl-rule-functions-fields.md`.*

## Aanleiding

De werkbank toont op de kaart "Verlaagde DRL" per definitie haar DRL-regels, als de stukken `rule "…" … end` van `drl`
(`/playground/analyse`). Wat een regel toetst en berekent, staat daar niet in: elke toets en elke waarde van het plan is
een functie in het Java-dialect, die de regel aanroept (`DrlRendering`, javadoc r. ±60–76). Een lezer ziet zo niet waar
een waarde gemaakt wordt. Voorbeeld van de eigenaar, v2/herleving-ww (regels `ece012e`, toolchain `b0b3f849`): de
uitzondering `herleving per kalendermaand art. 21 lid 1 na onderdeel c` berekent "de datum met jaar: het jaar uit (zijn
einddatum-van-een-eindigingsgrond plus 1 mnd), maand: … en dag: 1". Haar regel
`defaults A:Eindigingsgrond.herlevingsdatum: terugval 2` roept `r118_toets0($einddatumVanEenEindigingsgrond, …)` en
`r118_afloop2($einddatumVanEenEindigingsgrond)` aan; de dag 1 staat alleen in de functie `r118_afloop2`, en de vier
voorwaarden (drie geërfd van de basisregel, dialect §4) in `r118_samengesteld1`. De DRL-tekst van die bundel heeft 339
regels en 562 functies.

`DrlRendering.Rule` weet welke functies van een regel zijn (`functions`, namen `r<n>_<soort><plaats>`) en zet ze direct
vóór de regel, elk met de RegelSpraak-zin van haar knoop als commentaar (`fill`). De werkbank leidt die toewijzing niet
af uit de tekst: zij leest geen namen en knipt de tekst alleen waar de toolchain zegt.

## Doel

- `DrlRendering.Rendered` (of een ingang ernaast, bereikbaar zoals `originOf` via de build) zegt per regel van de tekst
  welk stuk tekst bij hem hoort: zijn functies en de regel zelf, als plaatsen in `text` (bijvoorbeeld het begin van zijn
  eerste functie, het begin van `rule "…"` en het einde na `end`), of als de namen van zijn functies met hun plaatsen.
  De bouwer kiest de vorm en zegt waarom; eis is dat een lezer zonder de tekst te ontleden per regel zijn functies kan
  tonen, met hun commentaar.
- Roept een regel een functie aan die niet van hem is (als dat voorkomt), dan noemt het antwoord die ook.
- Geen gedragsverandering: `text` bytegelijk, het exec-model ongewijzigd (`BranchDumpsCorpusWitnessTest` ongewijzigd).

## Acceptatie

- (a) Een getuige over het corpus: voor elke golden die de backend als DRL-tekst draagt, dekken de stukken van de regels
  samen precies de tekst na de kop (package, imports, globals), zonder overlap; elk stuk begint met zijn functies (of met
  `rule` als hij er geen heeft) en eindigt na zijn `end`; elke functie die een regel aanroept, staat in zijn eigen stuk
  of in de opgave van de vorige bullet.
- (b) Gericht: een regel zonder functies, een regel met een samengestelde voorwaarde (een functie die een andere
  functie van dezelfde regel aanroept), en een regel van een stapel met een terugval.
- (c) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

## Daarna (geen deel van deze kaart)

De toolchain geeft het per vermelding van `drlRules` door (kaart `drl-rule-functions-fields`); de kaart "Verlaagde DRL"
van de werkbank toont boven elke DRL-regel zijn functies, met de RegelSpraak-zin uit hun commentaar.

## Result (2026-10-24)

Tak `card/drl-rule-functions`, vanaf main `42ed425d`: de kaart in `152f0643`, naar `doing/` in `5c6c337e`. Geen
beschermd pad geraakt; geen delta voor `spec/CONSTRUCTS.md` of `spec/PLAN.md` (geen spec-wijziging, een ingang van de
backend). De draad, de spec, het schema en het frontend veranderen niet.

- `aa543653` de stukken van de DRL-tekst per regel (`TmsTranspiler.drlPieces`) en de twee getuigen.
- dit Result, in het commit na `aa543653`; de kaart naar `done/` in het commit daarna.

### De ingang

```java
public synchronized List<DrlPiece> drlPieces()                                  // TmsTranspiler
public record DrlPiece(String rule, int start, int ruleStart, int end,
                       List<DrlPiece.DrlFunction> functions) {                   // TmsTranspiler.DrlPiece
    public record DrlFunction(String name, int start, int declaration, int end) {}
}
```

`drlPieces()` geeft per regel van het plan, in de volgorde van de tekst, het stuk van `drl()` dat bij hem hoort. De
tekst is de kop (package, imports, globals) en daarna deze stukken, aaneengesloten tot het einde van de tekst, zonder
gat en zonder overlap. Gerenderd met de tekst, één keer, in `drl()`; geweigerd zoals `drl()` weigert (`DrlRefusal`).
Bereikbaar via de build, zoals `originOf`: `build.execModelTranspiler().drlPieces()`. Op de tak `DRL_TEXT` is
`build.drl()` de tekst van `execModelTranspiler().drl()` (de constructor van `Build` eist dat), dus de plaatsen liggen
in `build.drl()`.

- `rule`: de naam van de regel zonder escapes, zoals de KieBase, `formOf` en `originOf` hem hebben; de regel `rule "…"`
  spelt hem als DRL-stringliteral (`Namen.drlString`).
- `start`: het begin van het stuk: van zijn eerste functie (van haar commentaar, als zij er een heeft), of `ruleStart`
  bij een regel zonder functies. Gelijk aan het einde van het vorige stuk, of van de kop bij het eerste stuk.
- `ruleStart`: het begin van de regel `rule "…"`.
- `end`: het einde van het stuk, na de regel `end` en de lege regel erna: het begin van het volgende stuk, of het einde
  van de tekst.
- `functions`: de functies van de regel in de volgorde van de tekst; de eerste begint op `start`, elke volgende waar de
  vorige eindigt, de laatste eindigt op `ruleStart`. Leeg bij een regel zonder functies.
- `DrlFunction`: `name` zoals de regel en de functies die haar aanroepen hem spellen; `start` het begin van de functie
  met haar commentaar; `declaration` het begin van de regel `function …`; `end` na de regel met de sluitaccolade en de
  lege regel erna. Waar `start` vóór `declaration` ligt, staat daartussen precies één regel: `// `, de RegelSpraak-zin
  van de knoop van de functie (het `source.snippet` van het IR, met een regeleinde als spatie en `*/` als `* /`,
  `Namen.commentSafe`) en een regeleinde. Een knoop zonder zin geeft `start == declaration`.

Eenheid: een plaats is een index in de tekst zoals een Java-`String` telt, in UTF-16-code-units, zoals JavaScript een
string telt; geen bytes van UTF-8 en geen code points. Een `ë` telt één, een teken buiten het Basic Multilingual Plane
twee. Een begin telt mee, een einde niet: `drl().substring(start, end)` is het stuk.

De constructors weigeren een stuk waarvan de functies niet aaneensluiten van `start` tot `ruleStart`, een stuk dat niet
na `ruleStart` eindigt, en een functie met `declaration` vóór `start` of een `end` dat niet na `declaration` ligt
(`IllegalArgumentException`).

Waarom deze vorm:

1. Plaatsen, en niet alleen namen. De werkbank knipt de tekst waar de toolchain het zegt en leest geen namen; met alleen
   de namen van de functies moest zij elke functie in de tekst opzoeken.
2. Per functie haar eigen plaatsen, naast die van het stuk. De werkbank wil boven elke regel zijn functies tonen,
   ingeklapt, met de zin uit hun commentaar zichtbaar (toolchain `backlog/new/20261024-drl-rule-functions-fields.md`,
   "Daarna"). `declaration` scheidt de commentaarregel van de code, zodat ook die grens niet uit de tekst gelezen hoeft
   te worden. `name` erbij, zodat een lezer een aanroep in de regel aan een functie kan koppelen.
3. Eén lijst in de volgorde van de tekst, met de naam van de regel in het record, en geen `Map` op naam. De stukken
   dekken de tekst in die volgorde, en de toolchain loopt nu al over de regels in de volgorde van de tekst
   (`Playground.drlRules` met `drlRuleNames`). Wie op naam zoekt, maakt de map zelf; de namen zijn uniek, want
   `DrlRendering.render` weigert twee regels van één naam.
4. Geen veld voor aanroepen van functies van een andere regel, omdat die niet kunnen voorkomen. De weergave noemt een
   functie naar de plaats van haar regel in het plan (`r<n>_<soort><plaats>`), en alleen de weergave van die regel
   (`DrlRendering.Rule`) schrijft een aanroep van haar, in een functie van dezelfde regel of in de regel zelf. Gemeten
   op het corpus: geen enkele aanroep van een functie van een ander stuk (zie (a)). Komt het later toch voor, dan faalt
   de corpusgetuige met de naam van de regel en van de functie; een veld komt er dan bij, en het record verandert.

Hoe: `DrlRendering.Rule` noteert bij elke plaats van een functie haar naam (in `name`, dat bij elke functie precies één
keer wordt aangeroepen, direct na `reserveer`; een tweede naam faalt) en de lengte van haar commentaar (in `fill`). In
`renderRule` noteert zij de plaatsen terwijl zij de functies en de regel aan de tekst toevoegt, geteld vanaf het begin
van de regels; `render` telt er de lengte van de kop bij op, die pas bekend is als elke regel zijn imports heeft
genoemd. Niets wordt uit de tekst teruggelezen. De tekst en het plan veranderen niet.

### Gemeten

Op de 282 goldens die de backend draagt, zonder lezingen (de bouw waarvan de toolchain de tekst toont):

- 2124 stukken, waarvan 763 zonder functies; 2499 functies, waarvan 1944 met commentaar; 481 functies die een functie
  van hun eigen stuk aanroepen (een toets die de functie van haar samengestelde voorwaarde aanroept, een waarde die die
  van een `Let` aanroept, een `Let` die haar binder aanroept, …). Met lezingen 3046 stukken en 3456 functies.
- Een regel die een functie van een ander stuk aanroept: geen, zonder en met lezingen.
- Een functie die haar regel niet bereikt, direct of via andere functies van het stuk: één, in beide bouwen, in
  `jutterswet-bergingstoeslag`: `defaults A:Jutter.bergingsforfait: default 0` heeft `r2_binder4`, de binder van een
  `Let` ("de bergingsgrond is zijn bergingsgrondslag") waarvan de verlaagde body de variabele niet leest
  (`return new java.math.BigDecimal("42")`). De functie staat in het stuk van die regel, want zij is van hem; de
  werkbank toont dan bij die regel een functie die hij niet aanroept. Niet in deze kaart veranderd.
- 49 teksten hebben een teken buiten ASCII; geen tekst heeft een teken buiten het Basic Multilingual Plane, dus het
  corpus maakt geen verschil tussen UTF-16 en code points. Dat verschil toetst een document in (b).

### Getuigen

(a) `DrlPiecesCorpusWitnessTest`, nieuw. Elke golden die de backend draagt, op de tak `DRL_TEXT`, zonder en met
lezingen. Per tekst, met drie orakels van de getuige zelf die de weergave niet raadpleegt (de koppen `rule "…"` zoals de
parser ze leest, de declaraties `function <type> <naam>(` en de aanroepen `<naam>(` van een gedeclareerde functie):

- de stukken noemen de regels van de KieBase, elk één keer, in de volgorde van de koppen van de tekst;
- vóór het eerste stuk alleen regels `package`, `import`, `global` en lege regels; daarna sluiten de stukken aan tot het
  einde van de tekst;
- op `ruleStart` staat `rule ` met de naam als DRL-stringliteral; de regel eindigt met zijn enige regel `end` en een
  lege regel, aan het einde van het stuk, en declareert geen functie;
- een functie begint met één commentaarregel `// …` of met haar declaratie; op `declaration` staat de declaratie van
  precies die functie (de naam van het record); zij eindigt met haar sluitaccolade en een lege regel, en declareert geen
  andere functie; elke declaratie van de tekst ligt in een stuk;
- elke aanroep van een gedeclareerde functie in een stuk (buiten commentaar- en declaratieregels) is een aanroep van een
  functie van dat stuk;
- elke functie van een stuk wordt vanuit de regel bereikt, behalve een binder (de naam eindigt op `_binder<n>`, een
  vierde orakel), zoals de ene hierboven.

Vloeren, gemeten 2026-10-24: 282 goldens, 49 teksten met een teken buiten ASCII, en de tellingen hierboven.
Ongeveer 20 s.

(b) `DrlPiecesWitnessTest`, nieuw, vier tests. Drie op `jutterswet-uitsluiting-rgs`, dat alle drie de vormen heeft; per
regel de tabel van zijn stuk: per functie haar naam, haar commentaarregel (de tekst tussen `start` en `declaration`) en
de functies die zij aanroept, en welke functies de regel aanroept (gelezen uit de tekst van het stuk, een orakel van de
test):

- een regel zonder functies: `kenmerkstand Jutter.uitgesloten: waar uit de invoer`: geen functies, `start == ruleStart`,
  en het stuk is letterlijk de regel tot en met `end` en de lege regel;
- een samengestelde voorwaarde: `defaults K:Jutter.uitgesloten: default 0.0.0`: `r1_toets0` (roept `r1_samengesteld1`
  aan) en `r1_samengesteld1`, beide met het commentaar "hij vergunning geweigerd is en hij weigering herroepen is en hij
  herroeping vervallen is", en `r1_afloop2` zonder commentaar; de regel roept `r1_toets0` en `r1_afloop2` aan. Het stuk
  begint met de commentaarregel van `r1_toets0`;
- de terugval van een default met een uitzonderingsgroep, dezelfde vorm als het voorbeeld van v2 in de Aanleiding:
  `defaults K:Jutter.uitgesloten: terugval 0` (`originOf`: `DEFAULT` `fallback`): `r3_toets0` (roept `r3_samengesteld1`
  aan), `r3_samengesteld1`, en `r3_afloop2` zonder commentaar, met de body `return Boolean.FALSE;` (de hele functie
  letterlijk in de test); de regel roept `r3_toets0` en `r3_afloop2` aan. Het stuk sluit aan op dat van `eens 0` ervoor
  en `default 1` erna;
- de eenheid: een document in de test met twee regels, de eerste met de naam `penninggerechtigd 𝔘` (U+1D518, twee
  UTF-16-code-units, één code point). Op `ruleStart` van de tweede regel staat `rule "gemeld"`, en het aantal code
  points tot daar is `ruleStart - 1`.

Rood, met mutaties van `DrlRendering` in een kopie van de worktree (niet gecommit;
`werk/drl-rule-functions/mutations/run.py`, logs `werk/drl-rule-functions/logs/mutation-M*.log`; elk tegen de code van
`aa543653`, daarna teruggezet):

| mutatie | corpusgetuige | gerichte tests die rood worden |
|---|---|---|
| M1 de lengte van het commentaar niet genoteerd (`declaration == start`) | 3888 functies (1944 per bouw) waar op `declaration` niet de declaratie staat | samengestelde voorwaarde, terugval, eenheid |
| M2 de plaatsen vanaf het begin van de regels geteld, zonder de kop | 564 teksten (282 per bouw) waarvan het laatste stuk niet aan het einde eindigt | alle vier |
| M3 de plaatsen in code points in plaats van UTF-16-code-units | groen (het corpus heeft geen teken buiten het BMP) | eenheid |
| M4 de functies van een regel bij het stuk van de regel ervoor | 5081 aanroepen van een functie van een ander stuk; 3104 regels die vanaf `ruleStart` een functie declareren en niet met hun enige `end` eindigen; 505 teksten waarvan de stukken minder functies noemen dan de tekst declareert | samengestelde voorwaarde, terugval, eenheid (die laatste met een uitzondering, want de tweede regel heeft geen functies meer) |
| M5 de namen van de functies van een regel in omgekeerde volgorde | 3170 functies waar op `declaration` een andere functie staat | samengestelde voorwaarde, terugval |

Zonder mutatie zijn beide klassen groen (5 tests).

### Probe van de DRL-tekst

Een testklasse in de werkmap (niet gecommit, daarna verwijderd; dezelfde als in `drl-rule-part`) schreef de DRL-tekst
van alle 329 goldens, zonder en met lezingen, naar bestanden: 658 bestanden, waarvan 564 teksten van de 282 gedragen
goldens en 94 keer de weigering. Eerst op de code van `5c6c337e` (gelijk aan main `42ed425d`), daarna op die van
`aa543653` (in de kopie van de worktree): `diff -r` is leeg. Het exec-model rendert hetzelfde plan, want de analyse
verandert niet; `BranchDumpsCorpusWitnessTest` is ongewijzigd en groen in de suite.

### Suite en gate-set

Op `aa543653`:

- de JUnit-suite van `backends/drools` (`mvn -B -o test`): 1689 tests, 0 fouten, 0 overgeslagen; de 1684 van `42ed425d`
  plus één in `DrlPiecesCorpusWitnessTest` en vier in `DrlPiecesWitnessTest`;
- de volledige gate-set (`werk/gates.sh`): 17 van 17 groen in 30 minuten (12 767 s, 13 208 s, 17 427 s); samenvatting
  `werk/drl-rule-functions/gates-1.summary.txt`. Gate 12: PASS; 279 goldens gespeeld op de exec-model-tak en op de
  DRL-teksttak, elke statusregel gelijk; kanaalvloer 50 niet-gespeelde goldens bij naam; de suite van de gate 1689
  tests (vloer 1573), nul overgeslagen; `check-drools-codes` PASS (294 codes). De commits na `aa543653` veranderen
  alleen de kaart.

### Wat de toolchain ermee doet (kaart `drl-rule-functions-fields`)

- In `Playground.drlRules`: `List<TmsTranspiler.DrlPiece> pieces = build.execModelTranspiler().drlPieces()`, op dezelfde
  build van de tak `DRL_TEXT` als `build.drl()`. De stukken staan in de volgorde van de koppen van de tekst en noemen
  dezelfde namen, zonder escapes (de corpusgetuige houdt dat vast); de toolchain kan per vermelding het stuk op volgorde
  nemen, of op `p.rule()`, en `drlRuleNames(build.drl())` kan dan door `p.rule()` vervangen worden.
- Per vermelding de plaatsen, in sleutels die de toolchain kiest, bijvoorbeeld `"piece": {"start": …, "ruleStart": …,
  "end": …}` en `"functions": [{"name": …, "start": …, "declaration": …, "end": …}]`. De werkbank knipt dan
  `drl.slice(start, end)` voor het stuk, `drl.slice(f.start, f.declaration)` voor de commentaarregel van een functie
  (leeg als zij geen zin heeft) en `drl.slice(f.declaration, f.end)` voor haar code.
- Eenheid in `docs/ENGINE.md`: UTF-16-code-units van `drl`, zoals JavaScript telt. Jackson schrijft de Java-`String`, en
  `JSON.parse` geeft dezelfde reeks UTF-16-code-units terug, dus de plaatsen gelden ongewijzigd voor `drl` in de
  werkbank.
- Zonder DRL (`drlError`): `UcharBuild.assemble` gooit voordat er een build is, `drlRules` blijft leeg, en `drlPieces()`
  wordt niet gevraagd. `drlPieces()` gooit `DrlRefusal` waar `drl()` dat doet; na een geslaagde build van de tak
  `DRL_TEXT` gooit het niet, want die build heeft `drl()` al gerenderd.
- Voor acceptatie (a) van de toolchain-kaart verwacht ik bij `defaults A:Eindigingsgrond.herlevingsdatum: terugval 2` de
  functies `r118_toets0`, `r118_samengesteld1` en `r118_afloop2`, in die volgorde, met `r118_toets0` die
  `r118_samengesteld1` aanroept, zoals in de Aanleiding en zoals `terugval 0` van `jutterswet-uitsluiting-rgs`. Op v2
  heb ik het niet gemeten; die regelset staat buiten uchar.

### Voorlopige besluiten

1. De namen `drlPieces`, `DrlPiece` en `DrlFunction`: "stuk" is het woord van de kaart. `DrlFunction` en niet
   `Function`, omdat `TmsTranspiler` `java.util.function.Function` importeert en een consument die vaak ook gebruikt.
2. Een stuk en een functie eindigen ná de lege regel die op `end` en op de sluitaccolade volgt. De kaart zegt "eindigt
   na zijn `end`" en "dekken samen precies de tekst na de kop, zonder overlap"; dat kan alleen allebei als de lege regel
   bij het stuk hoort. Een lezer die de lege regel niet wil tonen, laat de witruimte aan het einde weg.
3. De RegelSpraak-zin van een functie staat niet als string in het record, maar als plaatsen: de regel tussen `start` en
   `declaration`, in de vaste vorm hierboven. Zo zegt het record alleen waar iets staat, en is wat de werkbank toont de
   tekst zelf. Wil de toolchain de zin zonder `// ` en het regeleinde doorgeven, dan zijn dat de plaatsen `start + 3` en
   `declaration - 1`; dat zou ik dan liever als twee plaatsen in uchar toevoegen dan de vorm in de toolchain laten
   kennen.
4. `DrlRendering.Rule.name` noteert de naam van de functie bij haar plaats. Het alternatief was een `reserveer` met de
   soort als argument, op de 22 plaatsen waar `reserveer` wordt aangeroepen; dat verandert veel meer regels voor
   hetzelfde.
5. De corpusgetuige laat een functie toe die haar regel niet bereikt als het een binder is (zie Gemeten). Of de
   verlaging zo'n binder weg moet laten, is een vraag over het plan, niet over de weergave; zij valt buiten deze kaart.
