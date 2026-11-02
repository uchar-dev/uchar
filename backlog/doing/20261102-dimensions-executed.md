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
