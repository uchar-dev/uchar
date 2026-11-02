# dimensions-executed: een gedimensioneerd attribuut draait in ref, clingo en Drools

*Vastgelegd door de hoofdsessie op 2026-11-02, op verzoek van Yeb: RegelSpraak (specificatie §3.6, §13.3.7) en ALEF kennen
dimensies, dus uchar moet ze uitvoeren, niet alleen beschrijven. Aanleiding: bij `20261009-drools-schrijfgrens` bleek dat EF-004
op een dimensiecel geen test kan krijgen.*

Goal: een attribuut met een of meer dimensies (geordende lijsten labels, IR-SPEC ch. 2.7) houdt per combinatie van labels een
waarde, en het lezen van een cel, het schrijven van een cel en de vouw (sum, minimum, maximum) over een selectie van cellen
draaien in het vierkant ref ≡ clingo ≡ Drools, op goldens met cases en absolute verwachtingen.

## Stand op main `e77dbf2b7`

| Laag | Dimensies |
|---|---|
| IR en schema | `Dimension {name, plural?, preposition?, labels}`, `DimensionCellRead {attribute, labels}`, `Aggregation.dimensionSelection {dimension, labels}`; een regel kan geen cel schrijven (`RuleVersion.target` is een `AttributeReference`) |
| verifier | WF-014, WF-040, WF-089 tot en met WF-091; laat zes documenten door die onder de huidige norm fout zijn, en weigert één goed (advies §1) |
| frontend | schrijft cel en vouw; goldens `jutterswet-seizoenvondst`, `jutterswet-seizoenvouw`, `jutterswet-vormenkabinet`, alle zonder cases |
| case-formaat | geen invoer of verwachting voor een cel |
| ref, clingo | weigeren cel en vouw (`REF-002`, `CLINGO-008`), en nemen een gewone waarde op een gedimensioneerd attribuut stil aan (`ref/lib/case.ml:450-469` leest `attribute_dimensions` niet) |
| Drools | kent cel en vouw over één dimensie, maar weigert invoer (DROOLS-047), dus speelt alleen met lege cellen |

## Onderzoek en besluiten

- Onderzoek: `/Users/yeb/Developer/uchar-dev/research/dimensies-2026-11-02/rapport.md` met `notes/` (RegelSpraak v2.3.0, ALEF,
  geporte runtime, metingen en mutanten).
- Ontwerpadvies: `/Users/yeb/Developer/uchar-dev/werk/dimensions-executed/fable/advies.md` (toets van het rapport, besluitpunten,
  stappenplan met acceptatie per stap). Dit advies is de bron voor de bouw.
- **Besluit Yeb (2026-11-02): waar de RegelSpraak-specificatie zwijgt, is ALEF de normbron** (een cel schrijven, een vouw over meer
  dimensies, een cel van een ander type dan getal). Waar de typering van de specificatie ALEF tegenspreekt (`eerste`, `laatste`,
  `aantal` over cellen), bouwt deze kaart niets. Vastleggen in CONSTRUCTS rij 28 (hoofdsessie) en IR-SPEC ch. 4.3.
- Voorlopige besluiten volgens het advies §2 (technisch): B1 een `DimensionCellRead` is toegestaan als schrijfdoel, plus
  `labels?` op `MemberInit`; B2 labels per positie, in de gedeclareerde volgorde van de dimensies; B3 `dimensionSelections` als lijst,
  één per dimensie, onder die nieuwe naam; B4 afhankelijkheden per cel; B5 alleen sum, minimum, maximum; B6 `cells` als invoer,
  `cellValue` en `cellEmpty` als verwachting, twee nieuwe EF-codes (onbekend of misplaatst label, dubbele cel), een scalar of
  tijdlijn op een gedimensioneerd attribuut een `CASE ERROR`; B7 een cel over een rol in ref en clingo in stap 2.

## Stappen (elke stap `make gates` groen; beschermde paden als voorstel)

De vijf stappen van het advies §3, met daar de acceptatie per stap:

1. Spec, verifier, IR- en case-schema, frontend-emitter (`dimensionSelections`), de lezersgrens in de case-lezer en de tekst van
   ch. 5.12 voor een cel. Rood eerst: de verifier-negatieven met `expect.txt`.
2. Ref en clingo lezen en vouwen; cases op de bestaande goldens; rood EF-004 en de twee nieuwe codes op een cel.
3. Schrijven naar een cel in ref en clingo; golden `jutterswet-seizoenboeking` met bron; rood EF-006 op een cel.
4. Drools: bean per cel, invoer en terugleggen, lezen en vouwen over n dimensies, graaf per cel (gate 15), lezingen (gate 17);
   DROOLS-040, -047, -184, -191 `RETIRED`; `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` met naam.
5. Frontend met meer dimensies; golden `jutterswet-strandseizoen`.

Parallel na stap 1: stap 2 en het Drools-leesdeel van stap 4; stap 3 na 2; het Drools-schrijfdeel na 3; stap 5 naast 3 en 4.

Buiten deze kaart (advies §4): `eerste`/`laatste`/`aantal` over cellen (vervolgkaart met een normkeuze), dimensie plus tijdlijn
(een WF-weigering in stap 1), de scenariolaag, `GedimensioneerdeLiteral`. De distributie volgt met de pin-bump na stap 4.

May touch: `spec/schema/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` (voorstel), `spec/verifier/`, `ref/`, `backends/clingo/`,
`backends/drools/`, `frontends/regelspraak/`, `corpus/`, `scripts/` (pins en vloeren, voorstel), `ref/README.md`.

## Result

### Stap 1

Branch `card/dimensions-executed`, worktree op main `b906f78ba`. Commits:

- `37b5a591d` kaart naar `doing/`;
- `3ee5d2892` draad, verifier, IR-schema, corpus (migratie, negatieven, goldens), emitter, Drools-loader en de
  naamswijziging in ref, clingo, catala en flogic;
- `cf981a3cf` case-schema, case-lezer van ref (en dus clingo), `ref/test`-rijen, twee rode gevallen, gates 4 en 8;
- `a60668183` voorstel spec: IR-SPEC, RUNTIME-SEMANTICS, `spec/schema/README.md`, `spec/ASP-TRANSLATION.md`;
- het commit van dit Result (en het commit met de samenvatting van de laatste `make gates`, onderaan).

Beschermde paden (voorstel, niet gemerged): `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`,
`spec/verifier/`, `scripts/check-ir-schema.sh`, `check-case-schema.sh`, `check-verifier.sh`, `check-red.sh`,
`check-drools.sh`; `spec/ASP-TRANSLATION.md` valt onder het CODEOWNERS-net van `/spec/`.

**Buiten de may-touch-set**, met reden: `frontends/catala/lib/translate.ml`, `frontends/flogic/lib/emit.ml` en
`lower.ml` bouwen de gedeelde OCaml-AST (`Function_call`, `assignment`) zelf; het hernoemde veld
`dimension_selections` en het nieuwe `to_labels` moeten daar gespeld worden, anders bouwt de werkruimte niet (gates
9 en 14). Alleen die spelling veranderde. `spec/ASP-TRANSLATION.md`: drie plaatsen noemen het veld bij naam.

**Begintoets van het advies** (gemeten, verifier en ref/clingo van `b906f78ba`; mutanten herbouwd uit de kern van
`mutate.py` in `notes/metingen.md`, omdat het script zelf niet in `notes/` staat:
`/Users/yeb/Developer/uchar-dev/werk/dimensions-executed/mutants/mutate.py`): a, b, c, d, e, f, g, k, l
`verifier: PASS`; h `WF-040.dimension-pin-count FAIL … pins dimension 'herkomst' 2 times`; i met een scalar 3.00 en
`attributeValue 3.00`: `oracle: PASS (1 expectations)`, `clingo: PASS (1 expectations)`. Het advies klopt op de
beginstand. Logs: `/Users/yeb/Developer/uchar-dev/werk/dimensions-executed/logs/stap1-red-*.log`.

#### Wat er veranderde

- **IR (schema, voorstel).** `RuleVersion.target` is `anyOf [AttributeReference, DimensionCellRead]` (B1);
  `MemberInit.labels` (lijst van strings, minItems 1, tussen `member` en `value`); `Aggregation.dimensionSelections`
  (lijst van `DimensionSelection`, minItems 1) vervangt `dimensionSelection` (B3). Migratie: `jutterswet-seizoenvouw`
  en `wf037-filled-count-dimension-selection`, met de serialisator van gate 2.
- **Case (schema, voorstel).** `$defs/cellsValue` (`cells`: per cel `labels` en `value`, minItems 1) als derde
  variant van een attribuutinvoer; verwachtingen `cellValue` en `cellEmpty` (oneOf-index 7 en 8, zodat de
  patroonposities van gate 4 niet schuiven).
- **Verifier.** `WF-040` positioneel en met de selectielijst (`wf040_cell` voor lezing, celdoel en
  `MemberInit.labels`; `wf040_selections`); nieuwe regel **WF-125** met vier armen (`bare-read`, `whole-write`,
  `whole-member`, `timeline`); WF-022 leest een celdoel via de verwijzing erin; WF-079 sleutelt een celdoel op de
  cel; de afhankelijkheidsgraaf heeft een knoop per cel (`n_cell`, `selected_cells`) en ref en clingo nemen die graaf
  over. De loader leest `dimensionSelections` zoals geladen (lege lijst inbegrepen), een `DimensionCellRead` als doel
  en `MemberInit.labels`.
- **Spec (voorstel).** IR-SPEC ch. 2.2 (rij `dimensions`), ch. 2.7 (de cel, de normbron ALEF, WF-125), ch. 3.1
  (doel, WF-022, WF-079), ch. 3.3 (`MemberInit.labels`), ch. 4 (laaguitzonderingen), ch. 4.3 (`dimensionSelections`,
  `filledCount`, `DimensionCellRead`), WF-037, WF-040, ch. 5.1 (celknoop), index WF-040, WF-079 en nieuw WF-125.
  RUNTIME-SEMANTICS: rij "dimension folds", ch. 5.7 stap 3, nieuw **ch. 5.8c** (toestand, lezen, schrijven, vouwen,
  graaf, case-vorm, uitgewerkte voorbeelden met absolute uitkomsten, waar de uitvoerders staan), ch. 5.7b EF-034 en
  EF-035, ch. 5.11 (de rij van de dimensievouw), ch. 5.12 (de cel als knoop, met voorbeeld; de grens van gate 17
  herschreven). De rekensommen van het advies (c1–c3: 5.00, 3.00, 3.00, 2.00; 1.25 viermaal; seizoenboeking 4.50 /
  6.50 / 0.00; 1.875 op 2 decimalen) staan erin, plus een tweedimensionaal voorbeeld (3.00, 5.00, 8.00, minimum
  1.00).
- **Frontend-emitter.** `IrEmitter` schrijft `dimensionSelections` als lijst van de ene selectie die de resolver
  bouwt; gate 10 houdt `seizoenvouw/ir.json` byte-gelijk.
- **Case-lezer (ref, gedeeld met clingo).** Scalar of tijdlijn op een gedimensioneerd attribuut, `cells` op een
  ongedimensioneerd, en `attributeValue`/`attributeEmpty`/`attributeTimeline` op een gedimensioneerd: `CASE ERROR`
  zonder code. `cells` op een gedimensioneerd attribuut: per cel EF-034 (labels noemen geen cel), EF-035 (cel twee
  keer), de waarde via `value_of_json`; daarna een `CASE ERROR` omdat geen van beide uitvoerders een cel evalueert.
- **Drools.** De loader leest `dimensionSelections`: één selectie in het bestaande model, meer selecties een
  `Refusal` **DROOLS-297** (`dimension-fold-multi-dimension`, REFUSAL), een lege lijst een `DocumentError`. Een
  celdoel en `MemberInit.labels` zijn een `Refusal` **DROOLS-298** (`dimension-cell-write`, REFUSAL) in plaats van een
  `DocumentError`: het schema laat ze nu toe. Catalogi nl/en kregen beide regels; register 296 → 298 codes.

#### Rood en groen per negatief en per rij

Verifier van `b906f78ba` ("oud") tegenover die van de branch ("nieuw"). Voor de vouw-mutanten gemeten in de oude
draadvorm (`dimensionSelection`, één knoop) en in de nieuwe; de oude verifier laadt de nieuwe vorm niet
(`LOAD ERROR … unknown field 'dimensionSelections'`).

| negatief (`corpus/negative/`) | mutant | oud | nieuw (`expect.txt`) |
|---|---|---|---|
| `wf125-bare-read` | a | PASS | `WF-125.bare-read` |
| `wf125-whole-target` | b | PASS | `WF-125.whole-write` |
| `wf125-dimension-and-timeline` | c | PASS | `WF-125.timeline` |
| `wf040-cell-labels-reversed` | d | PASS | `WF-040.misplaced-label` (positie 1 en 2) |
| `wf040-fold-foreign-dimension` | e | PASS (oude vorm) | `WF-040.selection-dimension-position` |
| `wf040-fold-dimension-unpinned` | f | PASS (oude vorm) | `WF-040.selection-count` (viermaal) |
| `wf040-fold-undimensioned-argument` | g | PASS (oude vorm) | `WF-040.selection-undimensioned-argument` |
| `wf040-selection-duplicate-label` | k | PASS (oude vorm) | `WF-040.selection-duplicate-label` |
| `wf040-selection-empty` | l | PASS (oude vorm) | `WF-040.selection-empty` |
| `wf040-selection-argument` | (nieuw: literal als argument) | PASS (oude vorm) | `WF-040.selection-argument` |
| `wf040-target-label-count` | (nieuw) | LOAD ERROR (`labels`) | `WF-040.label-count-mismatch` |
| `wf040-member-undimensioned-cell` | (nieuw) | LOAD ERROR (`labels`) | `WF-040.undimensioned-cell` |
| `wf125-member-without-labels` | (nieuw) | LOAD ERROR (celdoel); zonder de twee regels PASS | `WF-125.whole-write` |
| `wf125-defaults-target` | (nieuw) | PASS | `WF-125.whole-member`, `form=a defaults group` |
| `wf079-two-writers-one-cell` | (nieuw) | LOAD ERROR (`labels`) | `WF-079.computing-same-target`, `attribute=vondstgewicht[najaar]` |

Positief: h staat in `jutterswet-vormenkabinet` (dimensie `herkomst` {voorjaar, zee}, attribuut `aanspoeling`
[seizoen, herkomst], regel `zeecijfer uit voorjaarsaanspoeling` leest de cel `voorjaar`, `zee`): oud
`WF-040.dimension-pin-count FAIL /rules/rules/1/versions/0/value`, nieuw `verifier: PASS`. Nieuwe golden
`jutterswet-seizoenkopie` (celdoel, `MemberInit.labels`, twee schrijvers van twee cellen): oud `LOAD ERROR`, nieuw
`verifier: PASS`, `--dependency-edges` geeft de ene rand over `attr:jutter:vondstgewicht[najaar]`; per attribuut was
mutant j een WF-045-kring (`notes/metingen.md` M2 j).

Case-lezer, mutant i met `scalar-expect.json`: oud `oracle: PASS`, `clingo: PASS`; nieuw beide `CASE ERROR:
j1.vondstgewicht: a dimensioned attribute takes its input per cell ("cells"), never a scalar or a timeline` (exit
2). Rijen in `ref/test/test_ref.ml` (`WV-5.8c-caselezer-*`, negen): scalar, tijdlijn, `attributeValue`,
`attributeEmpty` op een gedimensioneerd attribuut, `cells` op een scalair, omgekeerde labels en te weinig labels
(EF-034), dubbele cel (EF-035), geldige cellen (geweigerd na de toetsen); elk pint de hele zin, de code en de
parameters. Rode gevallen: `corpus/red/jutterswet-cel-onbekend-label` (EF-034) en `jutterswet-cel-dubbel` (EF-035):
ref en clingo `expected fault observed at load` met de gepinde parameters; Drools `EXCLUDE DROOLS-184` (de cel over
twee dimensies, bij de bouw, vóór de case), in de lijst van gate 8.

#### Aangepaste verwachtingen, pins en vloeren (oud → nieuw)

- Gate 3 (`scripts/check-verifier.sh`, voorstel): `EXPECTED_MIN_NEGATIVE` 350 → 568, `EXPECTED_MIN_EXPECT` 354 →
  411, `EXPECTED_MIN_ARMS` 213 → 239, `EXPECTED_MIN_PARAM_LINES` 572 → 686, `EXPECTED_MIN_REPORT_KEYS` 318 → 340,
  `EXPECTED_MIN_ARM_SITES` 421 → 442, `EXPECTED_MIN_POINTERS` 619 → 706 (alle gemeten 2026-11-02). `CLAUDE.md` noemt
  bij gate 3 nog de vloeren van 2026-10-19; niet aangepast (buiten de kaart, voor de hoofdsessie).
- WF-040-sleutels: `WF-040.dimension-pin-count` vervalt (opgevolgd door `WF-040.misplaced-label`),
  `WF-040.undimensioned-cell-read` heet `WF-040.undimensioned-cell` (tekst "cell of undimensioned attribute");
  `label-count-mismatch` zegt "cell carries" in plaats van "cell read carries"; `WF-022.target-not-attribute` zegt
  "neither an attribute reference nor a cell over one". Geen `expect.txt` pinde een van die sleutels.
- `wf037-filled-count-dimension-selection`: document gemigreerd; de pin `WF-037.dimension-selection-kind` blijft; het
  rapport heeft er nu `WF-040.selection-undimensioned-argument` naast (NOTE bijgewerkt).
- Zin van de gedeelde OCaml-loader en de Drools-loader: "the target of a rule version is an AttributeReference" →
  "… an AttributeReference or a DimensionCellRead"; omgezet in `backends/clingo/test/test_clingo.ml` en
  `RuleAndExpressionLayerRefusalWitnessTest.the_target_is_an_attribute_reference`.
- Draadvorm in tests (de kaart verandert de draad): `DimensionFoldTest` (vier verwachtingen) en
  `SubtypeDialectTest` (één) verwachten `"dimensionSelections":[{…}]`; Drools `RenamingRefusalWitnessTest`,
  `SourceReferenceRefusalWitnessTest` en `UnknownFieldWitnessTest` wijzen naar `/dimensionSelections/0`,
  `RuleAndExpressionLayerRefusalWitnessTest` noemt `dimensionSelections` als veld dat een `BuiltinCall` niet draagt;
  `TmsBuiltinWitnessTest` en de clingo-frames schrijven de lijst.
- Gate 2: basisdocumenten en de rij "BuiltinCall carries dimensionSelections" in de lijstvorm, `_DSEL` wijst naar
  `[0]`; tien nieuwe vormrijen `CELL_SHAPE_ROWS` (517 → 527 vormrijen).
- Gate 4: dertien nieuwe zelftestrijen `CELL_ROWS` (121 → 134).
- Gate 8: twee `EXCLUDE DROOLS-184`-regels in de Drools-lijst; ch. 5.7b 33 → 35 rijen (vloer 31).
- Gate 12: `EXPECTED_CHANNEL` + `jutterswet-seizoenkopie|NOCASE`; `EXPECTED_PLAYED` blijft 300.
- Gate 5: ref-suite 1807 → 1816 voorbeelden; vier goldens niet gespeeld (+ `jutterswet-seizoenkopie`).
- Gate 15: `jutterswet-seizoenkopie` staat bij de goldens die de loader weigert (38).

#### Voorgestelde tekst voor CONSTRUCTS (hoofdsessie)

Rij 28, achter de bestaande `Landed`-noten:

> **Landed (stap 1 van de kaart `dimensions-executed`, 2026-11-02, `3ee5d2892`, `cf981a3cf`, `a60668183`):** de
> normbron — waar RegelSpraak v2.3.0 zwijgt (een cel als schrijfplaats, een vouw over meer dimensies, een cel van een
> ander type dan getal) volgt het dialect ALEF (besluit Yeb, 2026-11-02); waar de typering van de specificatie ALEF
> tegenspreekt (`first`, `last`, `count` over cellen) laat het dialect geen vouw toe. Een cel is overal een eigen
> sleutel: labels per positie in gedeclareerde volgorde (WF-040), `RuleVersion.target` mag een `DimensionCellRead`
> zijn, `MemberInit.labels?`, `Aggregation.dimensionSelections` met een selectie per dimensie (vervangt
> `dimensionSelection`), WF-125 (geen kale lezing, geen hele schrijving, geen vorm die het attribuut heel noemt, geen
> dimensie naast een tijdlijn), de afhankelijkheidsgraaf per cel (IR-SPEC ch. 5.1); het case-formaat draagt `cells`,
> `cellValue`, `cellEmpty`, met EF-034 en EF-035 (RUNTIME-SEMANTICS ch. 5.8c). Golden `jutterswet-seizoenkopie`
> (verifier-only). Geen uitvoerder evalueert een cel: ref en clingo toetsen een celinvoer en weigeren haar, Drools
> weigert een celschrijving (DROOLS-298) en een vouw over meer dimensies (DROOLS-297).

Rij `DimensieSelectie` (:102), achter de L5-noot: "**Landed (stap 1 van `dimensions-executed`, 2026-11-02):**
`Aggregation.dimensionSelections`, een lijst met een selectie per dimensie (B3)." Rij `DimensieCel` (:106): "**Landed
(stap 1 van `dimensions-executed`, 2026-11-02):** labels per positie (WF-040, B2); ook regeldoel (B1), en
`MemberInit.labels` voor een objectcreatie." Rij :204 ("Undimensioned read of a dimensioned attribute; dimensiecel
limits"): "gesloten door WF-125 en WF-040 (stap 1 van `dimensions-executed`)".

#### Voorlopige besluiten

1. **EF-034 en EF-035 met rode getuigen al in stap 1** (afwijking van de kaart, die de rode gevallen in stap 2 zet).
   Reden: gate 8 eist bij elke rij van ch. 5.7b een getuige (`scripts/check-red.sh`, de rij-regex; RUNTIME-SEMANTICS
   ch. 5.7b "Witnesses"), en de case-lezer is stap 1. Stap 2 hoeft ze niet meer te maken, wel EF-004 op een cel.
2. **Een geldige celinvoer is in stap 1 een `CASE ERROR`** na de toetsen, in plaats van haar in een wereld te leggen
   die geen cel leest. Stap 2 vervangt die weigering door de opslag (celsleutel in `World.attrs`) en het
   precisiehek EF-004 per cel. `cellValue`/`cellEmpty` leest de ref-lezer in stap 1 nog niet (onbekende soort, de
   bestaande fout); voorstel voor stap 2: labels die geen cel noemen in een verwachting zijn een fout zonder code,
   zoals een verwachting op een onbekend attribuut.
3. **Parameters van EF-034/EF-035**: `label` (het instantielabel, zoals bij EF-005/EF-016), `attribute`, `labels` (de
   labels van de cel, gescheiden door `, `). De `place` van een cel (EF-004, EF-006) is
   `<label>.<attribuut>[<l1>, <l2>]`, dezelfde spelling als de celknoop en de WF-079-melding.
4. **Java-model blijft één selectie.** `Expr.FunctionCall.dimensionSelection` (gedeeld door frontend en Drools) houdt
   één selectie; de emitter schrijft een lijst van één, de Drools-loader leest een lijst van één en weigert meer
   (DROOLS-297). De lijst in het model hoort bij stap 4 (Drools) en stap 5 (frontend).
5. **Twee nieuwe Drools-codes** DROOLS-297 en DROOLS-298 (soort REFUSAL); in stap 4 worden ze `RETIRED` zodra Drools de
   vorm draagt, naast 040, 047, 184, 191.
6. **Spelling van de celknoop** `attr:<type>:<attribuut>[<l1>, <l2>]`: de projectie van gate 15 knipt op `:` en leest
   de cel als lid; de Drools-graaf moet in stap 4 hetzelfde lid spellen.
7. **Lezing (ch. 5.12):** de `DimensionCellRead` is de knoop, de verwijzing eronder en het argument van een
   dimensievouw hebben geen eigen lezing; een dimensievouw draagt geen bijdragers. `ref/lib/reading.ml:112` loopt nog
   in de verwijzing; onbereikbaar zolang ref een cel met REF-002 weigert, voor stap 2.
8. **Een selectie mag haar labels in eigen volgorde noemen** (geen volgorde-eis, wel geen herhaling): de vouw leest in
   die volgorde, de frontend schrijft een interval in gedeclareerde volgorde.
9. **WF-125 als één nieuwe regel met vier armen**, WF-040 voor de cel en de selectielijst; de arm `timeline` wijst naar
   `/model/objectTypes/i/attributes/j/dimensions`.
10. **Nieuwe golden `jutterswet-seizoenkopie`** (met de hand, zonder bron) als positieve getuige van het celdoel, van
    `MemberInit.labels` en van de graaf per cel, in plaats van een celdoel in het vormenkabinet: de Drools-loader
    weigert een document met een celdoel, en het kabinet is de enige golden met een `StartPointDetermination`, die de
    sweep van `UnknownFieldWitnessTest` dan zou verliezen. De golden kan opgaan in `jutterswet-seizoenboeking` van
    stap 3.
11. **Schema-strengheid:** `dimensionSelections`, `MemberInit.labels`, `cells` en de labels van een cel minItems 1
    (afwezig is de enige spelling van "geen"); de labels van een `DimensionSelection` en een `DimensionCellRead` bleven
    zonder minItems (WF-040 weigert de lege selectie, `wf040-unpinned-cell` de lege cel).

#### Wat niet lukte of buiten stap 1 bleef

- `cellValue`/`cellEmpty` in de lezer van ref, de uitvoering van cellen in ref/clingo en Drools: stap 2–4.
- `IR-SPEC.md` ch. 4.6 (g) "a cell read is declined as a FORM by both OCaml executors" ongewijzigd: nog waar.
- Geen poging die mislukte.

#### `make gates`

Eerste run (op `cf981a3cf` plus de spectekst die daarna in `a60668183` landde): alle zeventien gates groen,
`red=0`, 317 s. Laatste run op `acf766830` (dit Result zonder deze alinea; de kaart is geen invoer van een gate),
`out/gates/20261102-141929/summary.txt`:

```
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 217 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 536 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 127s
04-case-schema rc=0 7s
03-verifier rc=0 37s
05-oracle rc=0 22s
06-clingo rc=0 260s
07-noop rc=0 13s
08-red rc=0 157s
09-catala rc=0 5s
10-regelspraak rc=0 216s
11-toka-parity rc=0 219s
12-drools rc=0 313s
13-reftype rc=0 255s
14-flogic rc=0 2s
15-graph rc=0 46s
16-scenario rc=0 39s
17-readings rc=0 234s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 14:24:42 313s red=0
```

Uit de gate-logs: gate 2 527 vormrijen, 407 bestanden, 54515 objecten in 975 documenten in schemavolgorde; gate 3
407 golden schoon, 568 negatief op hun regel, 411 op hun arm, 239 armsleutels, 686 parameterregels, 340 sleutels,
442 sleutelplaatsen, 706 wijzers (alle op hun vloer); gate 4 134 zelftestrijen, 1041 cases; gate 5 1816 voorbeelden,
972 cases, vier goldens zonder case; gate 6 2267 checks, 972 cases; gate 8 69 rode gevallen, 129 codechecks, 203
parameterchecks, 35 soorten in ch. 5.7b, Drools-leg 47 met fout, 22 met reden; gate 10 1364 tests, 161 rgs-goldens;
gate 12 298 codes, 300 gespeeld op beide takken; gate 15 313 van 351 goldens vergeleken, 38 door de loader geweigerd;
gate 17 300 goldens, 842 case-bestanden, afwijkingen 0.

### Stap 2

Branch `card/dimensions-executed`, worktree op `574faa1a7`. Commits:

- `a6d36acee` ref en clingo lezen en vouwen cellen: `ref/` (case-lezer, evaluatie, lezing, README),
  `backends/clingo/` (vertaling, model), beide suites, de cases van drie goldens, het rode geval, de NOTEs,
  `corpus/golden/README.md`, de pins van gate 8 en 12;
- `6022b6fe2` voorstel spec: RUNTIME-SEMANTICS, IR-SPEC, ASP-TRANSLATION;
- `6765aea05` getuigen voor twee gelijke cellen, het bereikhek van clingo en de kale lezing in clingo;
- `1b312ada1` voorstel spec: twee gelijke cellen in het voorbeeld van ch. 5.8c;
- `2af4632ba` de lezing van een cel over een pad (`WV-5.12-cel-over-een-pad`);
- `1e0a3bfec` voorstel spec: die lezing in ch. 5.12;
- het commit van dit Result.

Beschermde paden (voorstel, niet gemerged): `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`scripts/check-drools.sh`, `scripts/check-red.sh`; `spec/ASP-TRANSLATION.md` onder het net van `/spec/`. Buiten de
may-touch-set: niets.

**Begintoets van het advies** (stap 2): de voorspelling van gate 12 klopt voor `jutterswet-seizoenvondst` en
`jutterswet-seizoenvouw` (`EXCLUDE DROOLS-047`, beide takken); `jutterswet-vormenkabinet` weigert Drools eerder, bij de
bouw (`EXCLUDE DROOLS-184`, de cel over twee dimensies), dus die pin noemt DROOLS-184. Het kabinet kan cases dragen:
gemeten op `574faa1a7` speelt het document zonder zijn twee celregels in ref en clingo (`oracle: PASS (3
expectations)`, `clingo: PASS (3 expectations)`, met `StartPointDetermination` en `TimeWindow`); zie besluit 7.

#### Wat er veranderde

- **ref.** `World.cell` geeft een cel een eigen naam in `World.attrs`, `vondstgewicht[voorjaar, zuid]` (labels in
  gedeclareerde volgorde, in de sleutel genormaliseerd), dezelfde spelling als de celknoop van de verifier en de
  `place`; daarmee lezen `World.attr` en de schrijffuncties een cel ongewijzigd. De case-lezer legt `cells` cel voor
  cel neer (na EF-034/EF-035) in plaats van te weigeren, en leest `cellValue`/`cellEmpty`: labels die geen cel noemen
  en een celverwachting op een ongedimensioneerd attribuut zijn een `CASE ERROR` zonder code. `Eval.member_holder`
  (het object dat een verwijzing bereikt, eigen of over een pad) is uit de arm van `AttributeReference` gehaald en
  dient ook de cel en de vouw. De `DimensionCellRead` leest haar cel (ook over een pad, B7); `eval_dimension_fold`
  vouwt sum/minimum/maximum over `Ir_v0.Checks.selected_cells` (dezelfde productvolgorde als de graaf), lege cel
  overgeslagen, geheel leeg = leeg, `emptyFallback` van een som na de cellen. EF-004 per celwaarde
  (`validate_input_precision` vindt de declaratie via `Eval.member_attribute`), `place` `j1.vondstgewicht[voorjaar]`.
  Een gedimensioneerd attribuut buiten een cel wordt geweigerd in de zin van WF-125 (tweede lijn). REF-002 en de
  `Value.fault` op de vouw zijn weg voor wat nu gedragen wordt (REF-002 blijft voor `VariableReference` en
  `ContextValue`). `ref/lib/reading.ml`: de verwijzing onder een cel en het argument van een dimensievouw zijn
  `own:false` (besluit 7 van stap 1); een cel over een pad schrijft `on "<object>"`. `ref/README.md` bijgewerkt.
- **clingo.** `member_read` (uit de arm van `AttributeReference` gehaald) leest een lid met de `val/3` van een
  attribuut, eigen of over een pad; een cel is `val(S,"vondstgewicht[voorjaar]",A)` (geen eigen predicaat), via
  `Eval.cell_member`. De dimensievouw: per geselecteerde cel een hulpatoom `dimN(S,I,A)` met de celindex, `#sum
  { A,I }` / `#min` / `#max`, een definitieatoom voor geheel leeg, een som met `emptyFallback` 0 als kale `#sum`,
  een ander `emptyFallback` geweigerd zoals bij de setvouwen, per cel van een som het bereikhek `|A| > 2^31/n`. De
  invoer schaalt een cel naar de declaratie van haar attribuut. `Model.check` leest `Cell_value` (een getal) en
  `Cell_empty`. CLINGO-008 is weg voor cel en vouw; de kale lezing in de zin van WF-125. Het commentaar bij
  CLINGO-007 bijgewerkt.
- **Spec (voorstel).** RUNTIME-SEMANTICS ch. 5.8c: nieuw voorbeeld "Reading over a path" (12.50, 16.50; leeg, leeg),
  twee gelijke cellen in het tweedimensionale voorbeeld (4.00, minimum 2.00, cel leeg), "Where the executors stand"
  herschreven; ch. 5.7b EF-004 + `corpus/red/jutterswet-cel-overprecisie` en ch. 5.8c; ch. 5.11 de rij van de
  dimensievouw ("`ref` and the ASP backend evaluate the form"); ch. 5.12 het voorbeeld van de cel aangevuld (J2,
  de vouw, over een pad) en de grens van gate 17 herschreven. IR-SPEC ch. 4.6 (g): de zin "declined as a FORM by
  both OCaml executors" vervangen. ASP-TRANSLATION: S2 (een cel als `val/3`), S4 (cellezing, kale lezing), S8
  (dimensievouw), S19 (de bodem: de dimensiesom heeft een eigen arm; de vervallen bewering dat clingo de
  `TimeWindow` van het kabinet weigert, geschrapt, want het kabinet speelt nu in clingo).
- **Corpus.** Cases (handmatig berekend): `jutterswet-seizoenvondst/case1-voorjaarscel.json` (J1 12.50, J2 leeg; 3
  verwachtingen), `jutterswet-seizoenvouw/case1-twee-seizoenen.json` (5.00, 3.00, 3.00, 2.00; 5) en
  `case2-leeg-en-zomer.json` (J2 viermaal leeg, J3 viermaal 1.25; 9), `jutterswet-vormenkabinet/case1-cellen.json`
  (S1 vakcijfer 7, zeecijfer 4 uit (`voorjaar`, `zee`) naast (`voorjaar`, `voorjaar`) 9, peiling 1; S2 drie keer
  leeg; 7). Rood `corpus/red/jutterswet-cel-overprecisie` (document van seizoenvondst, cel `voorjaar` 1.005, EF-004
  `{value: 1.005, decimals: 2}`). NOTEs van de drie goldens en `corpus/golden/README.md` (getuigenkast) beschrijven
  de nieuwe stand; gate 5 noemt nog één golden zonder case (`jutterswet-seizoenkopie`).
- **Suites.** ref/test: `WV-5.8c-lezen`, `-vouw-een-dimensie-J1`, `-J2-J3` (de goldens), `-vouw-twee-dimensies`
  (J1 5.00/3.00/5.00/8.00/1.00, J3 twee gelijke cellen), `-cel-over-een-pad`, `-invoer-ef004` (code en drie
  parameters), `-kale-lezing`, `-caselezer-geldige-cellen`, `-verwachting-afwijkende-cel`,
  `-verwachting-weigert-omgekeerde-labels`, `-verwachting-weigert-cel-op-scalair`, `WV-5.12-cel`,
  `WV-5.12-cel-over-een-pad`. clingo-suite: de frames twee dimensies (met cellValue/cellEmpty, J2 leeg, J3 gelijke
  cellen) en pad, elk door ref en de solver; EF-004 op een cel; het bereikhek; de kale lezing.

#### Rode stand op `574faa1a7` en mutaties

Op `574faa1a7` (binaries van stap 1, `logs/stap2-red-cases.log`): de vier golden-cases geven in ref en clingo `CASE
ERROR: <label>.<attr>: a cells input is checked and then refused: neither executor that shares this reader evaluates
a cell` (exit 2), Drools `EXCLUDE DROOLS-047` (vondst, vouw) en `EXCLUDE DROOLS-184` (kabinet), beide takken; het rode
geval in ref en clingo `EXPECTED FAULT MISMATCH` met die `CASE ERROR`. ref/test met de nieuwe rijen op de oude code
(`logs/stap2-red-reftest.log`): twaalf FAIL, elk met de geweigerde case of de oude zin; clingo-suite
(`logs/stap2-red-clingotest.log`): drie FAIL (twee dimensies, pad, EF-004). De rijen die na de bouw kwamen (J3,
bereikhek, kale lezing in clingo, `WV-5.12-cel-over-een-pad`) zijn met de mutanten hieronder rood gezien.

Mutanten (`/Users/yeb/Developer/uchar-dev/werk/dimensions-executed/mutants/stap2/mutate2.py`, in een `git worktree add
--detach` die daarna verwijderd is; `logs/stap2-mutants-1.log` tot `-4.log`), elk één vervanging:

| | bewaking | mutatie | rood |
|---|---|---|---|
| a | EF-004 per cel | `validate_input_precision` zoekt de declaratie met de celnaam zelf (geen) | `WV-5.8c-invoer-ef004`; het rode geval in ref: `CODE EF-006 {"place":"j1.voorjaarsgewicht",…}` in plaats van EF-004 |
| b | geheel lege vouw is leeg | `agg_total` in plaats van `agg_sum` | `-J2-J3` (0 voor leeg), `-twee-dimensies`, `-cel-over-een-pad` |
| c | het product van de selecties | alleen de eerste cel | `-J1` (3 voor 5), `-J2-J3`, `-twee-dimensies`, `-pad`, `WV-5.12-cel` |
| d | cel over een pad | de cel op het subject zelf | `-cel-over-een-pad` (leeg voor 12.5) |
| e | celverwachting noemt een cel | toets weg | `-verwachting-weigert-omgekeerde-labels` (geladen) |
| f | kale lezing (ref) | toets weg | `-kale-lezing` (geen fout) |
| g | de cel is de knoop | verwijzing onder de cel `own` | `WV-5.12-cel` |
| g2 | vouwargument zonder lezing | `own_parts` zonder het conjunct | `WV-5.12-cel` |
| m | object van de cellezing | `object_reached` op de verwijzing i.p.v. de cel | suite stopt na `WV-5.12-cel` op de struikeldraad van `reading.ml` ("read a member over a path without walking it"), exit 2 |
| h | definitieatoom (clingo) | altijd gedefinieerd | twee dimensies (J2 `[0]` en `[#sup]` voor leeg), pad (`[0]`) |
| i | celindex in de `#sum` | index weg | twee dimensies (J3 200 voor 400) |
| j | bereikhek | hek weg | "past clingo's integer range" (clingo geeft een model) |
| k | kale lezing (clingo) | toets weg | "WF-125: …" (een model) |

#### Aangepaste verwachtingen, pins en vloeren (oud → nieuw)

- ref/test `WV-5.8c-caselezer-weigert-geldige-cellen` (een geldige celinvoer geeft `a cells input is checked and then
  refused…`) → `WV-5.8c-caselezer-geldige-cellen` (de invoer laadt, cellValue/cellEmpty kloppen): de kaart draait dit
  om.
- clingo-suite "WF-037 arm bounds: the dimension-cell sum holds the bottom" (CLINGO-008, `expression kind not
  translated`) → "… has its own arm" (vertaalt) plus de vergelijking door ref en de solver op dat frame; het commentaar
  bij die armgrens volgt.
- Gate 12 `EXPECTED_CHANNEL`: `jutterswet-seizoenvondst` en `jutterswet-seizoenvouw` `NOCASE` → `EXCLUDE DROOLS-047`,
  `jutterswet-vormenkabinet` `NOCASE` → `EXCLUDE DROOLS-184`, met reden; `EXPECTED_PLAYED` blijft 300.
- Gate 8, Drools-lijst: + `jutterswet-cel-overprecisie|EXCLUDE DROOLS-047 …`.
- Geen vloer veranderd. Tellingen: gate 4 1041 → 1046 cases; gate 5 1816 → 1834 voorbeelden, 972 → 976 cases, vier
  → één golden zonder case; gate 6 2267 → 2277 checks, 972 → 976 cases; gate 7 927 → 931 weerleggende cases, 45
  overlevers; gate 8 69 → 70 rode gevallen, 129 → 131 codechecks, 203 → 207 parameterchecks, Drools-leg 22 → 23 met
  reden; gate 13 57 → 61 getwiste annotaties over 29 → 30 goldens (de vier vouwen van seizoenvouw), 154 → 160
  replays; gate 17 972 → 976 getraceerde case-bestanden, vergeleken onveranderd (300 goldens, 842 bestanden).

#### Voorgestelde tekst voor CONSTRUCTS (hoofdsessie)

Rij 28, achter de noot van stap 1:

> **Landed (stap 2 van `dimensions-executed`, 2026-11-02, `a6d36acee` … `1e0a3bfec`):** ref en de ASP-backend
> lezen een cel, op het subject en over een pad van rollen, en vouwen sum, minimum en maximum over het product van
> de selecties (RUNTIME-SEMANTICS ch. 5.8c); een cel is in beide een lid van haar eigen (`vondstgewicht[najaar]`).
> Cases op `jutterswet-seizoenvondst`, `jutterswet-seizoenvouw` en `jutterswet-vormenkabinet`; rood EF-004 op een cel.
> Drools weigert de celinvoer (DROOLS-047) en de cel over twee dimensies (DROOLS-184), dus die pins zijn een
> ref = clingo-paar tot stap 4.

#### Voorlopige besluiten

1. **De naam van een cel** is `World.cell attr labels` = `attr[l1, l2]`, in ref de sleutel in `World.attrs` en in
   clingo de naam in `val/3`, geen eigen predicaat. Het attribuut van een lid zoekt `Eval.member_attribute` op het
   voorvoegsel `attr[`; past dat op meer dan één gedimensioneerd attribuut (alleen bij een attribuutnaam met `[`),
   dan is het een fout, geen keuze.
2. **De pin van het rode geval** noemt `value` en `decimals`, zoals de vier bestaande EF-004-getuigen: clingo's
   EF-004 draagt geen `place`. De `place` van een cel in ref pint `WV-5.8c-invoer-ef004`.
3. **Kale lezing**: ref en clingo weigeren een gedimensioneerd attribuut buiten een cel in de zin van WF-125, in
   plaats van leeg te lezen (tweede lijn achter de verifier).
4. **Tweede lijn in ref** zonder code: labels die geen cel noemen, een vouw waarvan het aantal selecties niet het aantal
   dimensies is, een vouw op een andere soort dan sum/minimum/maximum, een vouw met `elementType`, bereik, filter of
   granulariteit. De verifier (WF-040, WF-037) weigert die eerst.
5. **`emptyFallback`** van een dimensievouw: bij een som na de cellen geëvalueerd (ref) en in clingo alleen 0
   gedragen (zoals de setvouwen); bij minimum en maximum niet geëvalueerd (WF-037 laat hem daar niet toe).
6. **Cel van een ander type dan getal**: ref draagt elk type; clingo leest een cel als getal, een `cellValue` van een
   ander type weigert `Model.check` met naam, een symbolische cellezing valt op CLINGO-002. Geen corpusgeval.
7. **Het vormenkabinet draagt een case** (afwijking van zijn eerdere regel "geen case"): elke constructie die het
   houdt, draait in ref en clingo (gemeten), dus het is geen verifier-only golden meer. De rijen blijven: een cel over
   twee dimensies met een gedeeld label staat in geen andere golden; krimpen is een eigen kaart. NOTE en README
   beschrijven de nieuwe stand.
8. **Geen invoercel als verwachting** (gate 7); een instantie zonder enige cel deelt haar case met een gevulde, anders
   overleeft de case de no-op.
9. **Een dimensievouw over een pad noemt geen object** in haar lezing (ze is geen van de knopen die ch. 5.12 een
   object laat noemen); een cellezing over een pad wel.

#### Wat niet lukte of buiten stap 2 bleef

- Geen poging die mislukte.
- Cel als schrijfdoel (REF-012, CLINGO-026) en `MemberInit.labels`: stap 3; `jutterswet-seizoenkopie` blijft zonder
  case. Drools: stap 4.
- `CLAUDE.md` noemt bij gate 5 geen lijst van goldens zonder case; niets aan te passen. De vloeren van gate 3 in
  `CLAUDE.md` (stap 1) staan nog open voor de hoofdsessie.

#### `make gates`

Eerste run op `6022b6fe2` (`out/gates/20261102-145657/`): alle zeventien gates groen, `red=0`, 362 s. Laatste run op
`1e0a3bfec` (dit Result zonder deze alinea; de kaart is geen invoer van een gate),
`out/gates/20261102-151130/summary.txt`:

```
ocaml rc=0 0s ocaml-build: rebuilt (3 source path(s) newer than the stamp, e.g. backends/clingo/test/test_clingo.ml; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 536 source files)
01-self-contained rc=0 1s
02-ir-schema rc=0 160s
04-case-schema rc=0 9s
03-verifier rc=0 37s
05-oracle rc=0 22s
06-clingo rc=0 288s
07-noop rc=0 13s
08-red rc=0 166s
09-catala rc=0 4s
10-regelspraak rc=0 236s
11-toka-parity rc=0 245s
12-drools rc=0 336s
13-reftype rc=0 277s
14-flogic rc=0 1s
15-graph rc=0 49s
16-scenario rc=0 39s
17-readings rc=0 248s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 15:17:06 336s red=0
```

Uit de gate-logs: gate 5 1834 voorbeelden, 976 cases, één golden zonder case; gate 6 2277 checks, 976 cases; gate 7
931 weerleggende cases, 45 overlevers; gate 8 70 rode gevallen, 131 codechecks, 207 parameterchecks, Drools-leg 47
met fout, 23 met reden; gate 12 300 gespeeld op beide takken, 51 niet-gespeelde goldens met naam; gate 13 61
annotaties over 30 goldens, 160 replays; gate 17 976 getraceerd, 300 goldens en 842 case-bestanden vergeleken,
afwijkingen 0.

### Stap 4a (Drools lezen)

Branch `card/dimensions-drools`, worktree `/Users/yeb/Developer/uchar-dev/wt/dimensions-drools`, gebouwd vanaf
`574faa1a7` (stap 1) en daarna met `git rebase` op `card/dimensions-executed` `d1c54e405` (stap 2) gezet. Commits na
de rebase (vóór de rebase `c5fd49f58` en `d8c79877f`):

- `91f5760fa` het leesdeel: gedeeld model, loader, bean, case-lezer, teruglezen, transpiler (beide branches uit één
  plan), lezingen, regelgraaf, codes, catalogi, witnesses, README van de Drools-backend, de Drools-lijst van gate 8;
- `3eff2671c` dit Result vóór de rebase;
- `bd58ee8ae` voorstel pins: gate 8 en gate 12 (hieronder);
- `8a9ec4529` voorstel spec en NOTEs: RUNTIME-SEMANTICS ch. 5.8c ("Where the executors stand"), ch. 5.11 (de rij van de
  dimensievouw: "All three executors evaluate the form") en ch. 5.12 (de grens van gate 17: alleen een cel over een
  pad blijft erbuiten, DROOLS-189); de NOTEs van `jutterswet-seizoenvondst`, `-seizoenvouw`, `-vormenkabinet`,
  `corpus/red/jutterswet-cel-onbekend-label`, `-cel-dubbel`, `-cel-overprecisie` en de getuigenkast van
  `corpus/golden/README.md` noemden Drools nog als weigeraar (DROOLS-047, -184) en noemen nu de gemeten uitkomst;
- het commit van deze bijwerking.

Conflicten bij de rebase: `scripts/check-red.sh` (stap 4a schrapte de twee regels van `cel-onbekend-label` en
`cel-dubbel`, stap 2 voegde `cel-overprecisie` toe; beide gehouden, daarna vervalt de derde in `bd58ee8ae`) en de kaart
(de Results van stap 2 en 4a staan allebei, in die volgorde).

#### Wat er veranderde

- **Gedeeld model.** `Expr.FunctionCall.dimensionSelection` is `dimensionSelections`, een lijst van
  `DimensionSelection`, null zonder selectie en nooit leeg (de compacte constructor weigert een lege lijst). De
  frontend alleen voor naam en type: `IrEmitter` schrijft elke selectie van de lijst, `ExpressionBuilder` verpakt zijn
  ene selectie in een lijst, `RuleChecker` leest de lijst; meer dimensies in de frontend blijft stap 5. Gate 10 houdt
  `seizoenvouw/ir.json` byte-gelijk.
- **Loader.** Leest alle selecties; de weigering DROOLS-297 vervalt.
- **Bean.** Een gedimensioneerd attribuut is `Map<List<String>, Object>`: per cel de waarde onder haar sleutel, de
  labels per positie in hun gedeclareerde spelling (`FactModelGenerator.cellKey`; een label vergeleken als naam,
  `Namen.norm`), zodat case-invoer, verwachting en lezing van het document één cel treffen, en een cel van een ander
  type dan getal een plaats heeft.
- **Case-lezer** (`CaseInput`). `cells` op een gedimensioneerd attribuut: eerst per cel de labels (EF-034), dan
  dubbele cellen (EF-035, ook in twee spellingen van een label), dan per cel EF-004 (place
  `J1.vondstgewicht[voorjaar, zuid]`) en de variant van de waarde; dezelfde volgorde als ref (laadfouten vóór de
  evaluatiegrens). Nieuwe foutsoort `CaseInput.CellFault` in `CaseRunner.faultKinds`, parameters `label`,
  `attribute`, `labels`. Een scalar of tijdlijn op een gedimensioneerd attribuut, `cells` op een ongedimensioneerd en
  een celwaarde van een andere variant: `IllegalArgumentException` zonder code. DROOLS-047 vervalt.
- **Teruglezen** (`CaseRunner`). `cellValue` en `cellEmpty`; `attributeValue`/`attributeEmpty` op een gedimensioneerd
  attribuut, een celverwachting op een ongedimensioneerd en labels die geen cel noemen: fout zonder code (voorstel 2
  van stap 1). DROOLS-040 vervalt.
- **Transpiler.** Een cel over n dimensies is `CELL(map, [labels])` (`ComputeHelpers.cell(Object, List<?>)`;
  DROOLS-184 vervalt); een vouw is `rsDimSom/Min/Max` over de lijst van celsleutels van het product van de selecties,
  de eerste selectie buitenste lus (`ProducerConsumer.selectedCells`, dezelfde volgorde als de verifier). Nieuw
  **DROOLS-299** (CONTRACT, `dimension-labels-name-no-cell`): labels van een cel, of selecties die niet per positie de
  dimensies van het argument noemen, breken WF-040 en worden met naam geweigerd in plaats van als lege cel gelezen.
- **Lezingen** (ch. 5.12, besluit 7). De `DimensionCellRead` en de dimensievouw zijn de knoop; de verwijzing eronder
  en het argument van de vouw krijgen geen regel en houden hun nummer (`readingRule`, `withoutReading`); de vouw draagt
  geen bijdragers. Het object van een cellezing is het subject (eigen lid; de tekst schrijft het niet).
- **Regelgraaf** (besluit 6). `ProducerConsumer.cellOf`: een cel is een lid `vondstgewicht[voorjaar, zuid]` op het
  declarerende type; een vouw leest elke cel van het product.
- **DROOLS-191 blijft.** Een gedimensioneerd attribuut wordt alleen per cel geschreven (WF-125), en elke celschrijving
  weigert de loader al (DROOLS-298) voordat de transpiler loopt; 191 is daarmee onbereikbaar tot het schrijfdeel en is
  daar de wacht voor het lezen van een cel die een vorm schrijft (de TMS-`Value` per cel).
- Catalogi nl/en: DROOLS-299, EF-034, EF-035. `backends/drools/README.md`: invoer, vergelijking, berekening en
  weigeringen voor cellen.

`RETIRED`: DROOLS-040, DROOLS-047, DROOLS-184, DROOLS-297 (register 298 → 299 codes, 5 → 9 retired).

#### Rood en groen

Witness `backends/drools/src/test/java/dev/uchar/regelspraak/cases/DimensionCellWitnessTest.java`, negen rijen op
beide branches, over de goldens `jutterswet-seizoenvondst` en `jutterswet-seizoenvouw` en een tweedimensionaal document
(het uitgewerkte voorbeeld van ch. 5.8c: 5.00, 3.00, 5.00, 8.00, minimum 1.00). Op `574faa1a7` met alleen de nieuwe
tests (`logs/stap4a-red-2.log`, `stap4a-red-3.log`): alle negen rood — elke case met cellen `EXCLUDE DROOLS-047`
("invoer van een attribuut met dimensie 'vondstgewicht' …"), ook waar een lezing of een fout zonder code verwacht
werd; het tweedimensionale document door de loader geweigerd met DROOLS-297 waar een play, EF-034/EF-035/EF-004 of
DROOLS-299 verwacht werd; de regelgraaf van `seizoenvouw` met `[vondstgewicht]` waar
`[vondstgewicht[voorjaar], vondstgewicht[zomer]]` verwacht werd. De omgedraaide `TmsBuiltinWitnessTest`-rijen rood:
`a_cell_over_two_dimensions_reads_its_cell` (DROOLS-184 bij de bouw), `a_dimension_cell_and_a_dimension_fold_…`
(tweede kwartaal 1 in plaats van 21: de kaart met lijstsleutels werd met één label gelezen). Op de branch alle
groen.

Mutaties, elk apart, niet gecommit (`mutants/stap4a-mutate.py`, `logs/stap4a-mutants-1.log`), elk rood:

| mutatie | rode rij |
|---|---|
| M1 EF-034 weg | `the_case_reader_refuses…`: NOFAULT |
| M2 EF-035 weg | idem, EF-035-rij: NOFAULT |
| M3 EF-004 per cel weg | idem, EF-004-rij: EF-006 van de schrijfgrens in plaats van EF-004 |
| M4 label exact i.p.v. als naam | `a_label_compares_as_a_name` en de EF-035-rij met `Voorjaar, ZUID` (EF-034) |
| M5 selectie-dimensietoets weg | `labels_that_name_no_cell…`: verwisselde selecties PLAYED |
| M6 verwijzing onder de cel krijgt een lezing | `the_cell_and_the_fold…`: "noteerde geen term voor knoop 2" |
| M7a celknoop wordt attribuutknoop | `the_rule_graph…`: `[vondstgewicht]` |
| M7b vouw leest het hele attribuut | idem, `seizoenvouw` |
| M8 cellEmpty leest niet terug | `a_cell_of_one_dimension…`: PLAYED waar FAIL verwacht |
| M9 attributeValue op gedimensioneerd toegelaten | `a_case_that_does_not_fit…` |
| M10 variant van een celwaarde niet getoetst | idem: ConsequenceException |
| M11 scalar op gedimensioneerd toegelaten | idem: NullPointerException |
| M12 `cells` op ongedimensioneerd toegelaten | idem: geen fout |

#### Aangepaste tests, pins en vloeren (oud → nieuw)

- `TmsBuiltinWitnessTest.a_cell_over_two_dimensions_is_refused_by_name` (pinde de zin van DROOLS-184) →
  `a_cell_over_two_dimensions_reads_its_cell` (de cel k2, noord: 20): door de kaart omgedraaid.
- `TmsBuiltinWitnessTest.a_dimension_cell_and_a_dimension_fold_beside_a_later_derived_toeslag`: de met de hand gezette
  kaart `Map<String, BigDecimal>` met sleutel `"k1"` → `Map<List<String>, Object>` met sleutel `List.of("k1")`; de
  verwachtingen (21, 36, 120 volgordes) ongewijzigd.
- Naam `dimensionSelection()` → `dimensionSelections()`: `RuleAndExpressionLayerRefusalWitnessTest`,
  `FilledCountDialectTest`, `SubtypeDialectTest`, `IrTest` (daar nu `get(0)` met een assert dat er één selectie is).
- `ReadingsCorpusWitnessTest` uitgebreid: een `DimensionCellRead` is een lidlezing; zijn object en zijn waarde (de cel in
  de kaart van de bean) worden gehouden zoals bij een `AttributeReference`.
- Plandump- en DRL-tekstpins: geen veranderd (de synthetische `FormsPlan` gebruikt `RS_DIM_SUM` met een lijstconstante,
  waarvan de handtekening niet veranderde).
- Gate 8 (`scripts/check-red.sh`, voorstel): `jutterswet-cel-onbekend-label` en `jutterswet-cel-dubbel` uit de
  Drools-lijst (waren `EXCLUDE DROOLS-184`); Drools-leg 47 → 49 met fout, 70 → 76 parameterchecks, 22 → 20 met reden,
  20 → 18 EXCLUDE met code (vloer `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=18` ongewijzigd).
- Gate 12: suite 1743 → 1752 tests; `EXPECTED_PLAYED` 300 en `EXPECTED_CHANNEL` ongewijzigd tot de rebase (geen golden
  met cellen had een case).

Na de rebase op stap 2 (`bd58ee8ae`, beschermde paden, voorstel; oud is de stand van stap 2 `d1c54e405`):

- Gate 8 (`scripts/check-red.sh`): `jutterswet-cel-overprecisie|EXCLUDE DROOLS-047 …` uit de Drools-lijst; de Drools-leg
  leest het geval nu als `FAULT EF-004 {"place":"j1.vondstgewicht[voorjaar]","value":"1.005","decimals":"2"}`, de
  gepinde `value` en `decimals` erin. Drools-leg 47 → 50 met fout (alle 50 met de code van het geval), 70 → 78
  parameterchecks (vloer 28), 23 → 20 met reden, 21 → 18 EXCLUDE met code (vloer `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=18`,
  ongewijzigd, nu precies gehaald).
- Gate 12 (`scripts/check-drools.sh`): `jutterswet-seizoenvondst` en `jutterswet-seizoenvouw` (`EXCLUDE DROOLS-047`) en
  `jutterswet-vormenkabinet` (`EXCLUDE DROOLS-184`) uit `EXPECTED_CHANNEL`, 51 → 48 niet-gespeelde goldens met naam
  (47 EXCLUDE, 1 NOCASE: `jutterswet-seizoenkopie`); `EXPECTED_PLAYED` 300 → 303, met reden in het commentaar
  (PLAYED 1, 2 en 1 op beide takken); DRL-teksttak 300 → 303 gespeeld; suite 1752 tests (vloer 1573); codes 298 → 299.
- Gate 13, 15, 17: geen pin of vloer bewoog (gemeten in de run hieronder, oud uit de laatste run van stap 2
  `out/gates/20261102-151130`). Gate 13: 61 annotaties over 30 goldens en 160 replays ongewijzigd, waarvan door de
  DRL-teksttak gespeeld 28 → 29 (`jutterswet-seizoenvouw` met de vier getwiste vouwen), 0 overlevers. Gate 15: 313 van
  351 goldens vergeleken, 273 afhankelijkheden, 3 lijstverschillen, ongewijzigd (de regelgraaf leest de cel per cel,
  zoals de verifier; een golden met cellen stond al in de vergelijking). Gate 17: vergeleken 300 → 303 goldens, 842 →
  846 case-bestanden, 6812 → 6832 lezingen, 22331 → 22361 knoopregels, 3797 → 3800 `unreached`; `DEVIATIONS` en
  `READINGS_ERROR_KNOWN` blijven leeg, afwijkingen 0.
- Geen verschil tussen ref, clingo en Drools op een golden of een lezing gezien; niets in `backends/drools/` gerepareerd.

#### Voorlopige besluiten

1. **Het model draagt de selectielijst** (null zonder selectie, nooit leeg), in afwijking van besluit 4 van stap 1,
   dat de lijst naar stap 4 en 5 verschoof.
2. **De celsleutel spelt de labels zoals gedeclareerd**; een label uit case of document wordt als naam vergeleken, en
   EF-035 vergelijkt de genormaliseerde sleutel, zoals ref.
3. **DROOLS-299 is een CONTRACT-code** voor labels of selecties die geen cel noemen, zoals DROOLS-185 en -188 andere
   WF-garanties bewaken.
4. **Volgorde in de case-lezer**: alle labels (EF-034), dan dubbele cellen (EF-035), dan per cel EF-004 en de variant.
5. **Een cel met waarde `null` is een lege cel** (niet in de kaart).
6. **EF-034 en EF-035 zonder regel in `EF_PLACES`** van `check-drools-codes.sh`, zoals EF-033.
7. **`Cycles` leest een cel nog per attribuut**: zonder celschrijver geen waarneembaar verschil; per cel hoort bij het
   schrijfdeel, met DROOLS-298 en -191.

Gevolg buiten uchar: de distributie leest de bean; een gedimensioneerd lid is `Map<List<String>, Object>`, voor de
service-JSON bij de pin-bump.

#### Gates vóór de rebase

`make gates` op de werkboom van `c5fd49f58` zonder de laatste twee rijen van de witness (`out/gates/20261102-145129`):
alle zeventien groen, `red=0`, 322 s; gate 12 299 codes, 300 gespeeld op beide takken, JUnit 1752; gate 8 Drools-leg
49 met fout; gate 15 313 van 351 vergeleken; gate 17 300 goldens, afwijkingen 0. De rijen kwamen daarna in de witness
(groen, 9 tests). Proef op `card/dimensions-executed` `6022b6fe2` (een cherry-pick in een losse werkboom, opgeruimd):
`seizoenvondst` PLAYED 1, `seizoenvouw` PLAYED 2, `vormenkabinet` PLAYED 1 op beide takken; `runner --trace` en
`--readings` gelijk op alle vier case-bestanden en beide takken; de drie rode cellen `FAULT EF-034`, `EF-035`, `EF-004`
met de gepinde parameters; één conflict, in `scripts/check-red.sh` (de drie regels van de cellen vervallen).

#### `make gates` na de rebase

Drools-suite eerst (`mvn -o test`, `logs/stap4a-finish-suite-1.log`): 1752 tests, 0 fouten, 0 overgeslagen. Daarna één
`make gates` op de werkboom van `bd58ee8ae` (de pins nog niet gecommit bij de start, vandaar `3eff2671c` in de
startregel; `logs/stap4a-finish-gates-1.log`), `out/gates/20261102-152228/summary.txt`:

```
start 2026-11-02 15:22:28 3eff2671c card/dimensions-drools
ocaml rc=0 4s ocaml-build: rebuilt (15 source path(s) newer than the stamp, e.g. backends/clingo/lib; 4s)
frontend rc=0 3s frontend: rebuilt (3s)
jar rc=0 3s drools-jar: rebuilt (37 source path(s) newer than the jar, e.g. backends/drools/src/main/java/dev/uchar/regelspraak/cases; 3s)
01-self-contained rc=0 1s
02-ir-schema rc=0 133s
04-case-schema rc=0 6s
03-verifier rc=0 36s
05-oracle rc=0 21s
06-clingo rc=0 270s
07-noop rc=0 12s
08-red rc=0 163s
09-catala rc=0 4s
10-regelspraak rc=0 222s
11-toka-parity rc=0 225s
12-drools rc=0 326s
13-reftype rc=0 268s
14-flogic rc=0 1s
15-graph rc=0 45s
16-scenario rc=0 36s
17-readings rc=0 245s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 15:27:58 330s red=0
```

Uit de gate-logs: gate 4 1046 cases; gate 5 1834 voorbeelden, 976 cases, één golden zonder case
(`jutterswet-seizoenkopie`); gate 6 2277 checks, 976 cases; gate 7 931 weerleggende cases, 45 overlevers; gate 8 70
rode gevallen, 131 codechecks, 207 parameterchecks, Drools-leg 50 met fout, 78 parameterchecks, 20 met reden; gate 12
299 codes (9 retired), 303 gespeeld op beide takken, 48 niet-gespeelde goldens met naam, JUnit 1752; gate 13 61
annotaties over 30 goldens, 160 replays; gate 15 313 van 351 vergeleken; gate 17 976 getraceerd, 303 goldens en 846
case-bestanden vergeleken, afwijkingen 0. Losse replay (`logs/stap4a-finish-cells-1.log`): de drie goldens PLAYED 1,
2 en 1 op beide takken; de drie rode cellen `FAULT EF-035`, `EF-034`, `EF-004` met de gepinde parameters.

Na de run kwam alleen `8a9ec4529` (proza in `corpus/**/NOTE.md`, `corpus/golden/README.md` en RUNTIME-SEMANTICS
buiten de tabel van ch. 5.7b, die gate 8 en de codetoetsen lezen); gate 1, de enige gate die die tekst leest, daarop
apart groen (`logs/stap4a-finish-gate1-1.log`). Niet gemerged, niet gepusht.

Voor de hoofdsessie: de CONSTRUCTS-tekst van stap 2 eindigt met "Drools weigert de celinvoer (DROOLS-047) en de cel
over twee dimensies (DROOLS-184), dus die pins zijn een ref = clingo-paar tot stap 4"; na deze branch spelen de drie
goldens in het vierkant, dus die zin vervalt bij de landing van beide.

### Stap 3

Branch `card/dimensions-executed`, worktree op `6c397de93` (stap 1, 2 en 4a). Commits:

- `dfa7666e5` de frontend schrijft een cel als doel: `RuleChecker` (celdoel, `MemberInit` met labels, RGS-308),
  `IrEmitter`, `ResolvedObjectCreation.MemberInit.labels`, `Dependencies` per cel, register en catalogi, tests,
  `grammar/DIVERGENCES.md`;
- `e14a39298` ref en clingo schrijven een cel: `ref/lib/eval.ml`, `ref/lib/reading.ml`,
  `spec/verifier/lib/desugar.ml`, `backends/clingo/lib/translate.ml`, beide suites, `ref/README.md`,
  `backends/clingo/README.md`;
- `004536a30` Drools laadt een celschrijving en weigert haar bij de bouw: `IrLoader`, `DroolsBackend`,
  `ProducerConsumer`, `DimensionCellWitnessTest`, `backends/drools/README.md`;
- `215b371de` golden `jutterswet-seizoenboeking`, de case van `jutterswet-seizoenkopie`, rood
  `jutterswet-cel-schrijfgrens`, NOTEs, `corpus/golden/README.md`, de pins van gate 8 en 12;
- `8da463967` voorstel spec: RUNTIME-SEMANTICS ch. 5.7b, 5.8c, 5.12, 7.7; ASP-TRANSLATION S3, S11, S19;
- `936f67a94` ref: de aparte weigering van een kenmerk met labels in `init_member` weg (zonder getuige; `cell_member`
  faalt daar al, een kenmerk heeft geen dimensies);
- het commit van dit Result (en dat met de samenvatting van de laatste `make gates`).

Beschermde paden (voorstel, niet gemerged): `spec/RUNTIME-SEMANTICS.md`, `spec/verifier/lib/desugar.ml`,
`scripts/check-drools.sh`, `scripts/check-red.sh`; `spec/ASP-TRANSLATION.md` onder het net van `/spec/`. Buiten de
may-touch-set: niets.

**Begintoets en afwijkingen van het advies** (gemeten op de werkboom van `6c397de93` met de nieuwe golden):

- *De verwachting "J3 zonder invoer: najaar leeg"* (rapport §3.5 c3, en de tekst van stap 1 in ch. 5.8c, "a leeg
  operand empties the product") klopt niet: ch. 1.2 laat `maal` een lege operand coalesceren naar 0. Gemeten: ref
  `MISMATCH: J3.vondstgewicht[najaar] = 0, expected leeg`, clingo `= [0]`. Golden en spec zeggen nu najaar 0.00 (en
  jaargewicht 0.00 bleef). Mijn eerste case3 (J4 met invoer `najaar` 5.00 en geen `voorjaar`) verwachtte dezelfde fout
  (5.00 blijft staan); de case pint nu 0.00: de berekende 0 verdringt de invoer (ch. 7.8).
- *RGS-308 blijft een weigering* (de opdracht en het advies zeggen "RGS-307/308 worden emissie"). ALEF schrijft een cel
  als `DimAttribuutSelector`, met de editor "de" + attributieve labels + attribuut + predicatieve labels, en het
  onderwerp via de keten erboven (`languages/regelspraak/languageModels/editor.mps:13470` e.v.): de labels staan
  tussen attribuut en "van <onderwerp>", de vorm van RGS-307. De vorm van RGS-308 ("De premie van een Jutter in
  voorjaar") is niet die van ALEF en niet die van §3.6 (de labels na het attribuut); zij blijft geweigerd, met een
  tekst die de schrijfplaats noemt. RGS-307 is emissie en uit het register.
- *Drools weigert bij de bouw, niet in de lader.* Gemeten: met de golden en de weigering in de lader faalt de
  routetoets van gate 15 (`GraphSourceRouteTest`, `jutterswet-seizoenboeking`:
  `IrLoader$Refusal: a write of a cell …`, 161 groen, 1 rood), omdat die van elke golden met een `source.rgs` de
  graaf van beide routes vergelijkt. De lader draagt nu celdoel en `MemberInit.labels` in het model,
  `DroolsBackend.build` weigert met DROOLS-298 voordat er een bean is, en de regelgraaf schrijft per cel; beide
  goldens zijn daardoor in de specpoot van gate 15 vergeleken (315 van 352, was 313 van 351).

#### Wat er veranderde

- **ref.** `Eval.target_member` geeft van een regeldoel het type, het gedeclareerde attribuut en het lid in de
  feitenbasis (het attribuut, of de cel `vondstgewicht[najaar]`); `run_rule` schrijft het lid met de declaratie van
  het attribuut, dus de schrijfgrens (EF-006, `place` `j1.vondstgewicht[najaar]`), de idempotentie en de
  initialisatiewacht gelden per cel. `Eval.init_member`: een `MemberInit` met labels schrijft de cel van het gemaakte
  object. Een kenmerktoekenning op een cel faalt in de zin van WF-022. `reading.ml` leest een regel met een celdoel
  als een regel op een attribuut, de initialisatie `DISPLACED` waar haar cel gevuld is.
- **Gedeelde domeinen** (`Ir_v0.Desugar`, voorstel). `target_of` sleutelt een celdoel op de cel, zodat een cel een
  eigen doel is in de staande norm (ch. 7.8) en in het domein van ch. 7.7; `other_writer` kent een `MemberInit` met
  labels. `Desugar.document` herschrijft het paar op een cel niet naar een defaults-groep (zo'n groep noemt haar
  attribuut heel, WF-125); ref leest het paar inheems in de defaults-lezing (`desugared_targets`).
- **clingo.** `find_decl`: elke declaratie-opzoeking per lid vindt bij een cel de declaratie van haar attribuut
  (decimalen, eenheid, datatype). `translate_rule` schrijft het celdoel onder de naam van de cel; `init_writers`,
  `co_writer`, `writer_conflicts` en `translate_case` (`inputfilled/2`) sleutelen op `Desugar.target_of`; de
  objectcreatie schrijft `val(ocI(S),"vangstgewicht[najaar]",…)`. Geweigerd met naam: een initialisatie naast een
  berekening op één cel, en een kenmerktoekenning op een cel. CLINGO-026 blijft voor andere doelvormen.
- **Frontend.** `RuleChecker.cellTarget`: een doel dat een cel noemt (label tussen attribuut en onderwerp) wordt een
  `DimensionCellRead`; `makeMemberInit`: een toekenning die een cel noemt krijgt `labels`. `checkRuleTargets` toetst
  de verwijzing onder een celdoel. `IrEmitter` schrijft het celdoel en `MemberInit.labels` (tussen `member` en
  `value`). `Dependencies` (de WF-045-spiegel van de frontend) heeft een knoop per cel zoals de verifier: een
  cellezing, een vouw over het product van haar selecties, een celdoel en een lid met labels; daardoor meldt de
  frontend een cel uit zichzelf als WF-045 en een cel uit een andere cel van hetzelfde attribuut niet. RGS-307 weg
  (register 355 → 354 codes), RGS-308 nieuwe tekst (nl/en).
- **Drools.** Zie de afwijking hierboven; DROOLS-298 blijft de code, nu als `Excluded` uit `refuseCellWrites`. Zonder
  die weigering weigert de transpiler het document met RULE_TARGET_UNRESOLVED (mutant D1), dus de weigering noemt de
  reden en niet een gevolg.
- **Corpus.** Golden `jutterswet-seizoenboeking` met `source.rgs`: regel `najaar` := `voorjaar` maal 2 (een cel uit
  een andere cel van hetzelfde attribuut), initialisatie `zomer` op 0 kg, `jaargewicht` als som over alle seizoenen,
  objectcreatie met `vangstgewicht in najaar`, en twee regels die de cellen van het gemaakte object terugbrengen naar
  de jutter (een verwachting noemt alleen een invoerlabel). Cases (met de hand): `case1-voorjaar-en-zomer` (J1, J2; 9
  verwachtingen), `case2-zonder-invoer` (J3; 6), `case3-invoer-in-een-geschreven-cel` (J4, J5; 8).
  `jutterswet-seizoenkopie` krijgt `case1-kopie-en-staande-invoer` (5): de staande norm per cel (J2 `zomer` 7.00
  blijft staan). Rood `corpus/red/jutterswet-cel-schrijfgrens`: het najaar-document met "maal 1,5", J1 `voorjaar`
  1.25 → EF-006, pin `{}` zoals de andere EF-006-getuigen (ref `place` `j1.vondstgewicht[najaar]`, clingo `place`
  `vondstgewicht[najaar]`; beide in de suites gepind).
- **Suites.** ref/test (1834 → 1850 voorbeelden): `WV-5.8c-schrijven-J1-J2` (met `WV-5.8c-objectcreatie-cel` J1, J2
  op de cellen van de gemaakte Vangst), `-schrijven-J3`, `-schrijven-invoer-verdrongen`,
  `-schrijven-staande-invoer`, `-schrijfgrens-ef006`, `-initialisatie-naast-berekening`, `-celdoel-kenmerk`,
  `WV-5.12-celdoel`. clingo-suite (2277 → 2280): de schrijfgrens van een cel, de weigering van het paar op een cel,
  de weigering van een kenmerk op een cel. Frontend: `DimensionTargetTest` (6 rijen) en nieuw `CellEdgesTest` (2; de
  graaf van de golden gelijk aan die van de verifier, de kring van een cel uit zichzelf). Drools
  `DimensionCellWitnessTest` + 2 rijen (lader en graaf; weigering bij de bouw, beide takken, ook met lezingen).
- **Spec (voorstel).** RUNTIME-SEMANTICS ch. 5.8c punt 2 (een cel als eigen doel in ch. 7.7 en 7.8), het voorbeeld
  "Writing" (J3 gecorrigeerd, J4/J5, de cel van het gemaakte object, het rode geval) en een nieuw voorbeeld "The
  standing norm and the pair on a cell", "Where the executors stand" herschreven; ch. 5.7b EF-006 (+ ch. 5.8c, + het
  rode geval); ch. 5.12 (de lezing van een regel met een celdoel); ch. 7.7 (de cel). ASP-TRANSLATION S3 (een cel als
  doel), S11 (een lid met labels), S19 (de weigering van het paar op een cel). IR-SPEC ongewijzigd.

#### Rood op `6c397de93`

In een losse werkboom op `6c397de93` met de nieuwe corpusbestanden en testbestanden (opgeruimd):

- de vier golden-cases in ref `EVALUATION FAULT: REF-012: rule "najaarsgewicht van een jutter": unsupported target
  shape`, in clingo `TRANSLATION FAULT: CLINGO-026: unsupported rule target`; het rode geval in beide `EXPECTED FAULT
  MISMATCH` (REF-012, CLINGO-026) (`logs/stap3-red-cases.log`);
- ref/test: de acht nieuwe rijen rood, alle op REF-012 (`logs/stap3-red-reftest.log`); clingo-suite: de drie nieuwe
  frames rood, `unsupported rule target` (`logs/stap3-red-clingotest.log`);
- frontend: `DimensionTargetTest` compileert niet (`MemberInit` heeft geen `labels()`); zonder die regel 7 van 8 rood
  (RGS-307, RGS-189, de oude tekst van RGS-308), alleen `reading_a_cell_stays_clean` groen
  (`logs/stap3-red-frontend*.log`);
- Drools: de broncode van de witness compileert niet (`labels()`); zonder die regel beide nieuwe rijen rood: de lader
  weigert met `IrLoader.Refusal` waar een model en een `Excluded` verwacht worden (`logs/stap3-red-drools.log`);
- de oude frontend op `source.rgs`: RGS-307 tweemaal en RGS-189 (`objectcreatie: 'vangstgewicht in najaar' is not a
  declared attribute of 'Vangst'`); de routetoets van gate 15 rood zoals hierboven.

#### Mutaties

`/Users/yeb/Developer/uchar-dev/werk/dimensions-executed/mutants/stap3/mutate3.py`, elk één vervanging in een `git
worktree add --detach` op `8da463967` (opgeruimd); `logs/stap3-mutants-1.log` (M3, M6; de eerste poging van M1, M2
en M4 compileerde niet en is overgedaan), `-2.log`, `-3.log`:

| | bewaking | mutatie | rood |
|---|---|---|---|
| M1 | ref: het celdoel schrijft de cel | lid = het attribuut | 10 rijen, o.a. `-schrijven-J1-J2` (najaar leeg), `-schrijfgrens-ef006` (`place` `j1.vondstgewicht`), `WV-5.12-celdoel` |
| M2 | ref: `MemberInit.labels` | lid = het attribuut | `-objectcreatie-cel` J1, J2 (alle cellen leeg) en de drie geboekte gewichten |
| M3 | ref: kenmerk op een cel | weigering weg | `-celdoel-kenmerk` (geen fout) |
| M4 | ref-lezing: initialisatiewacht per cel | wacht vraagt het attribuut | `WV-5.12-celdoel` |
| M5 | domeinen per cel (`target_of`) | sleutel op het attribuut | `-staande-invoer` (J2 zomer leeg), `-initialisatie-naast-berekening` (geen vast punt); clingo op seizoenkopie: "the case supplies input for computed target" |
| M6 | geen herschrijving van een cel | filter weg | clingo op seizoenboeking: `TRANSLATION FAULT: default value scale 1 does not match target scale 100` |
| M7 | clingo: paar op één cel | weigering weg | frame "…beside a computing rule…": een model |
| M8 | clingo: kenmerk op een cel | weigering weg | frame "a characteristic attributed to a cell": een model |
| M9 | clingo: `find_decl` | geen declaratie voor een cel | seizoenboeking: `unknown attribute "vondstgewicht[najaar]" on "Jutter"` |
| M10 | clingo: `inputfilled/2` per cel | alleen attribuutdoelen | seizoenboeking J2: `vondstgewicht[zomer] = [200;0]` |
| F1 | frontend: celdoel | geen `cellTarget` | 5 rijen (RGS-113-zin "is geen gedeclareerd attribuut") |
| F2 | frontend: lid met labels | tak weg | 3 rijen (RGS-189) |
| F3 | frontend-graaf: celdoel per cel | attribuutknoop | `CellEdgesTest` beide rijen |
| F4 | frontend-graaf: cellezing per cel | attribuutknoop | `CellEdgesTest` beide rijen |
| D1 | Drools: weigering bij de bouw | aanroep weg | `a_cell_write_is_refused…`: RULE_TARGET_UNRESOLVED |
| D2 | Drools-graaf: celdoel | schrijft niets | `the_loader_carries…`: `[]` voor `[vondstgewicht[najaar]]` |
| D3 | Drools-graaf: lid met labels | attribuut | idem: `[vangstgewicht]` |

#### Aangepaste verwachtingen, pins en vloeren (oud → nieuw)

- `DimensionTargetTest.a_cell_assignment_refuses_with_the_dimension` (beide vormen geweigerd met de cel genoemd) →
  `a_cell_target_is_written_as_the_cell` (de vorm "De premie in voorjaar van een Jutter" schrijft de cel) en
  `labels_after_the_subject_are_refused_with_the_cell` (RGS-308, de hele nieuwe zin); door de kaart omgedraaid.
- Register en catalogi: RGS-307 weg (354 codes, vloer 347); RGS-308 nl/en nieuwe tekst.
- Gate 12 `EXPECTED_CHANNEL` (voorstel): + `jutterswet-seizoenboeking|EXCLUDE DROOLS-298`;
  `jutterswet-seizoenkopie|NOCASE` → `EXCLUDE DROOLS-298`; 48 → 49 niet-gespeelde goldens met naam;
  `EXPECTED_PLAYED` blijft 303.
- Gate 8 Drools-lijst (voorstel): + `jutterswet-cel-schrijfgrens|EXCLUDE DROOLS-298`; niet gedragen met reden 20 → 21,
  EXCLUDE met code 18 → 19.
- De zin van DROOLS-298 is ongewijzigd; de statusregel verliest het voorvoegsel `loader:` (`EXCLUDE DROOLS-298: a
  write of a cell …`), omdat de bouw weigert.
- Geen vloer veranderd. Tellingen (oud uit de laatste run van stap 4a, `out/gates/20261102-152228`): gate 2 407 → 410
  bestanden, 54515 → 54682 objecten in 975 → 978 documenten; gate 3 407 → 410 golden schoon; gate 4 1046 → 1051
  cases; gate 5 1834 → 1850 voorbeelden, 976 → 980 cases, één → geen golden zonder case; gate 6 2277 → 2280 checks,
  976 → 980 cases; gate 7 931 → 935 weerleggende cases, 45 overlevers; gate 8 70 → 71 rode gevallen, 131 → 133
  codechecks, 207 → 209 parameterchecks; gate 10 1364 → 1369 tests, 161 → 162 rgs-goldens; gate 12 JUnit 1752 →
  1754; gate 13 61 → 64 getwiste annotaties over 30 → 31 goldens, 160 → 168 replays; gate 15 313 → 315 van 351 → 352
  vergeleken, 273 → 279 afhankelijkheden, 38 → 37 door de lader geweigerd, route 161 → 162 goldens; gate 17 976 →
  980 getraceerd, vergeleken ongewijzigd (303 goldens, 846 bestanden), 2 goldens `EXCLUDE DROOLS-298`.

#### Voorgestelde tekst voor CONSTRUCTS (hoofdsessie)

Rij 28, achter de noot van stap 2:

> **Landed (stap 3 van `dimensions-executed`, 2026-11-02, `dfa7666e5` … `936f67a94`):** ref en de ASP-backend schrijven
> een cel met een regel, een initialisatie en een objectcreatie (`MemberInit.labels`), met de schrijfgrens per cel
> (EF-006); een cel is een eigen doel in de staande norm en het domein van ch. 7.7, en de desugaring herschrijft het
> paar op een cel niet. De RegelSpraak-frontend schrijft een celdoel in de vorm van ALEF ("Het vondstgewicht in najaar
> van een Jutter"; RGS-307 vervalt, RGS-308 blijft voor de labels na het onderwerp). Golden
> `jutterswet-seizoenboeking` met bron, cases op `jutterswet-seizoenkopie`, rood `jutterswet-cel-schrijfgrens`. Drools
> laadt de vormen en weigert bij de bouw (DROOLS-298), dus die pins zijn een ref = clingo-paar tot het schrijfdeel van
> stap 4.

Rij `DimensieCel` (:106): "**Landed (stap 3):** als schrijfdoel uitgevoerd in ref en clingo en door de frontend
geschreven."

#### Voorlopige besluiten

1. **RGS-308 blijft een weigering**, RGS-307 wordt emissie (afwijking van opdracht en advies; reden hierboven).
2. **Een cel is een eigen doel in ch. 7.7 en 7.8, maar de desugaring herschrijft het paar op een cel niet**; ref leest
   het paar inheems in de defaults-lezing, clingo weigert het paar op één cel met naam (geen corpusgeval; zonder het
   filter faalt clingo op de golden, M6).
3. **Drools weigert bij de bouw** met DROOLS-298 (`Excluded`), de lader draagt de vormen en de graaf schrijft per cel
   (afwijking van "Drools weigert een celschrijving" in de lader; reden hierboven). De distributie die de lader
   gebruikt, ziet nu een `DimensionCellRead` als doel en `MemberInit.labels`.
4. **`jutterswet-seizoenkopie` blijft een eigen golden** met een case in plaats van op te gaan in
   `jutterswet-seizoenboeking`: drie negatieven zijn er van afgeleid (`wf040-target-label-count`,
   `wf079-two-writers-one-cell`, `wf125-member-without-labels`), en haar twee kopieën dragen WF-079 per cel en de
   staande norm per cel.
5. **De cellen van een gemaakt object** zijn in een golden alleen te zien via een regel die ze naar een invoerobject
   terugbrengt (een verwachting noemt geen gemaakt object); ref/test leest ze rechtstreeks (`oc0::J1`).
6. **EF-006 op een cel pint `{}`**: ref en clingo spellen `place` verschillend; de plaats van ref pint
   `WV-5.8c-schrijfgrens-ef006`, die van clingo het frame van de suite.
7. **Een kenmerktoekenning op een cel** faalt in ref en clingo in de zin van WF-022 (tweede lijn achter de verifier);
   een kenmerkinitialisatie met labels faalt in `cell_member` ("cell of undimensioned attribute").
8. **De WF-045-spiegel van de frontend is per cel**, zoals de verifier; stap 5 raakt de cellezing en de vouw van de
   frontend, dus wie als tweede landt rebaset `Dependencies.java` en `RuleChecker.java`.
9. **De frontend schrijft een celdoel alleen voor een attribuut van één dimensie met voorzetsel**, de reikwijdte van
   de cellezing van de frontend; meer dimensies is stap 5.

#### Wat niet lukte of buiten stap 3 bleef

- Geen poging die mislukte, behalve de eerste ronde van M1, M2 en M4 (een ongebruikte variabele brak de dev-build;
  overgedaan met een mutant die compileert).
- Het schrijfdeel van Drools (beans, regels, TMS-`Value` per cel, `Cycles` per cel, DROOLS-191): stap 4b.
- De ref/test-rij en het clingo-frame voor het paar op één cel tonen een divergentie per ontwerp (ref antwoordt,
  clingo weigert); geen corpusgeval.

#### `make gates`

Eerste run op `8da463967` (`out/gates/20261102-161148`): alle zeventien gates groen, `red=0`, 364 s. Daarna kwam alleen
`936f67a94` (een arm in `init_member` weg). Laatste run op `936f67a94` (dit Result zonder deze alinea; de kaart is geen
invoer van een gate), `out/gates/20261102-162255/summary.txt`:

```
ocaml rc=0 0s ocaml-build: rebuilt (1 source path(s) newer than the stamp, e.g. ref/lib/eval.ml; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 537 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 137s
04-case-schema rc=0 7s
03-verifier rc=0 39s
05-oracle rc=0 22s
06-clingo rc=0 285s
07-noop rc=0 13s
08-red rc=0 165s
09-catala rc=0 5s
10-regelspraak rc=0 227s
11-toka-parity rc=0 233s
12-drools rc=0 343s
13-reftype rc=0 281s
14-flogic rc=0 2s
15-graph rc=0 49s
16-scenario rc=0 38s
17-readings rc=0 251s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 16:28:38 343s red=0
```

Uit de gate-logs: de tellingen onder "Aangepaste verwachtingen" hierboven; gate 8 Drools-leg 50 met fout, 78
parameterchecks, 21 met reden, 19 EXCLUDE met code; gate 12 299 codes, 303 gespeeld op beide takken, 49 niet-gespeelde
goldens met naam, JUnit 1754; gate 15 315 van 352 vergeleken, 279 afhankelijkheden, route 162 goldens, 75 unit tests;
gate 17 980 getraceerd, 303 goldens en 846 case-bestanden vergeleken, afwijkingen 0. Niet gemerged, niet gepusht.
