# absence-markers-residue: de zijtabel van de frontend, markers in scenario en cel, en de kopie van een tijdlijn met markers

*Opgesteld door de hoofdsessie op 2026-11-05, uit de vragen van stap B van `absence-markers`. Starten op sein van de eigenaar.*

## Aanleiding

1. De RegelSpraak-frontend typeert een attribuut met markers als zijn payloaddatatype en houdt de markers in een zijtabel
   (`absenceMarkerSites`); de emitter schrijft er de vorm met een sum type uit (`SumType`, `Construct`, `PayloadAccess`), en RGS-376
   toetst de lezing tegen die tabel. Het gedeelde model van frontend en Drools-backend kent de drie knopen sinds stap B, maar de
   resolver bouwt ze niet. De IR is goed (gate 10 houdt de golden `jutterswet-afwezigheidsmarkers` byte-gelijk, de verifier houdt
   WF-127); het gaat om de vorm binnen de frontend: een tweede bron voor wat de typering zou moeten dragen.
2. Een kopie van een tijdlijn met markers (ref's fixture `copy-of-a-timeline`) speelt in ref; clingo en de Drools-backend weigeren haar
   bij naam (Drools: DROOLS-303). Geen golden vraagt er nu om.
3. (Uit de toolchain-kaart `absence-markers-in-service`.) De scenariolaag heeft geen vorm voor een marker in een situatie of een
   verwachting (`ResolvedScenario.ScenarioValue`), zodat een toetsdocument er geen geeft; en de frontend herkent een marker die een
   regel op een cel van een gedimensioneerd attribuut schrijft niet ("Het gewicht in voorjaar van een Vondst moet gesteld worden op
   onbekend" geeft een WARNING `onopgelost`).

Besloten zonder kaart (hoofdsessie, technisch): een `Match` blijft in de Drools-backend geweigerd (DROOLS-021) tot een golden die het
sum type zonder markers draagt erom vraagt; hoe de toolchain een constructorwaarde in de uitleg weergeeft, hoort bij haar pin.

## Goal

- (1) De resolver bouwt en typeert elke lezing van een attribuut met markers als `PayloadAccess`, een geschreven of getoetste marker als
  `Construct`, en zet RGS-376 op die knopen; de zijtabel `absenceMarkerSites` verdwijnt. De emissie blijft byte-gelijk.
- (3) Een marker in een situatie en een verwachting van een scenario (gate 16), en een marker die een regel op een cel schrijft (een
  golden met een gedimensioneerd attribuut met markers, ref, clingo en Drools), of een fout met code waar de vorm niet gedragen wordt.
- (2) clingo en Drools dragen de kopie van een tijdlijn met markers, met een golden die ref, clingo en beide Drools-branches speelt, of
  het blijft een weigering bij naam als het dragen buiten verhouding is (met de gemeten reden in het Result).

## Acceptance

- (a) Rood eerst per punt; per nieuwe bewaking een mutatie die een test rood maakt.
- (b) Bij (1): gate 10 en de frontend-suite groen zonder de zijtabel; geen golden verandert.
- (c) `make gates` groen.

May touch: `frontends/regelspraak/` (ook de scenariolaag), `backends/clingo/`, `backends/drools/**`, `corpus/`, kaartverplaatsing.

## Omvang

(1) middel (resolver en typering); (2) klein tot middel.

## Result van stap A (de frontend)

*Bouwer stap A, 2026-11-05. Niet gemerged, niet gepusht. Logs onder `werk/absence-markers-residue/logs/`, proeven onder
`werk/absence-markers-residue/probes/`, het mutatiescript `werk/absence-markers-residue/scripts/mutations.py` (buiten de
repo). Punt 1 en het toetsdocument van punt 3 zijn gebouwd; de cel van punt 3 en punt 2 zijn stap B (gemeten, onder).*

### Commits

| hash | inhoud |
|---|---|
| `3775fe1ae` | kaart naar `doing/` |
| `acae636ba` | punt 1: de somtypeknopen in het model, de zijtabel `absenceMarkerSites` weg; `Dependencies` leest de base van een `PayloadAccess`; `AbsenceMarkerTest` (+2), `resolver/AbsenceMarkerEdgesTest` (nieuw), DIVERGENCES |
| `93f854e39` | punt 3: een marker als waarde in een situatie en een verwachting; RGS-377 met catalogi; `ScenarioAbsenceMarkerTest` (nieuw); `scenario.rgs` en NOTE bij de golden `jutterswet-afwezigheidsmarkers`; DIVERGENCES (ook de meting van een marker op een cel) |
| `b3f15f29b` | getuige: een woord dat geen marker is blijft op de plaats van een waarde een syntaxfout |
| `17c3537ac` | een objectcreatie schrijft een attribuut met markers als een regel (vondst tijdens de bouw); getuige voor de vouw over de waarden |
| (deze) | Result |

### Wat er veranderde

- **Punt 1, het model.** `ObjectType.sumTypeOf` geeft het somtype van een attribuut met markers (domein
  `<attribuut> van een <Objecttype>`, constructoren `waarde` met het gedeclareerde datatype als payload, dan de
  markers); `attributeMarkers` blijft de declaratie, `attributes` het payloaddatatype. Waar de resolver een regel leest
  bouwt hij een geschreven of getoetste marker als `Construct` (`RuleChecker.absenceMarkerValue`, `absenceMarkerTest`)
  en "een waarde heeft" / "die een waarde hebben" als gevuldtoets van een `PayloadAccess` (ook
  `ExpressionBuilder.elementValueTest`). De slotpas `resolver/AbsenceMarkers` (na `RuleChecker`, dus de checks lezen een
  verwijzing met het payloaddatatype, zoals vóór de kaart) bouwt elke andere lezing van de waarde als `PayloadAccess`,
  wikkelt elke andere geschreven waarde van een regel, een default en een objectcreatie in `Construct waarde(v)`, typeert
  elke verwijzing naar het attribuut met het somtype, en zet RGS-376 op de `PayloadAccess`-knopen zonder toets ervoor
  (dezelfde wachtregel als voorheen). Een node waarin niets verandert blijft dezelfde, dus een vorm zonder markers blijft
  het record van de regelcheck. De emitter schrijft `SumType`, `Construct` en `PayloadAccess` uit het model;
  `writtenValue`, de zijtabelarmen in `expr` en `currentDefaultsAttribute` zijn weg, `foldSourceReference` kijkt door een
  `PayloadAccess`. `Dependencies` las een `PayloadAccess` als blad (de `default`-arm) en zag zo de lezingen van een
  waardetoets niet; nu leest hij de base, zoals de verifier (`dep_expr_at`). Een lezing van een cel van een
  gedimensioneerd attribuut met markers is een `PayloadAccess` over de `DimensionCellRead` en blijft RGS-376 (er is geen
  toets van een cel).
- **Punt 3, het toetsdocument.** De grammatica leest op de plaats van een waarde (`situatieWaarde`) een marker in de
  woorden van de regels, `afwezigheidsWaarde`: `onbekend` en `niet van toepassing`, de twee woorden `onbekend` en
  `toepassing` op hun tekst (zoals `volgens`), dus geen woord wordt sleutelwoord. `ResolvedScenario.ScenarioValue`
  kreeg `constructor` en `payload` (`fromConstructor`); `ScenarioResolver.readAndType` krijgt de markers van het doel en
  schrijft elke waarde van een attribuut met markers als constructorwaarde (een marker zonder payload, een andere
  waarde als payload van `waarde`), ook per segment en per cel; `CaseEmitter` schrijft
  `{"constructor": …, "payload": …}`. Een marker die het doel niet declareert, een marker op een attribuut zonder
  markers en een marker voor een parameter: **RGS-377**. De semantische tokens lezen de marker als één sleutelwoord.
  De golden `jutterswet-afwezigheidsmarkers` kreeg een `scenario.rgs` met tien scenario's die haar tien cases en haar
  `ir.json` byte-gelijk schrijven (gate 16).
- **Geen golden en geen verwachting veranderde.** `git diff e5f52100e -- corpus` raakt alleen de nieuwe `scenario.rgs`
  en de NOTE van die golden.

### Rood eerst (letterlijk)

- **Punt 1 op main** (`e5f52100e`, de nieuwe tests tegen de code van main; `logs/03-red-point1-point3-main.log`):
  `the_model_carries_the_nodes_of_the_sum_type() ✘ Unexpected type, expected: <dev.uchar.regelspraak.ir.Expr.Construct>
  but was: <dev.uchar.regelspraak.ir.Expr.AttributeReference>`; `a_read_of_a_cell_is_refused() ✘ geen PayloadAccess in
  Binary[op=PLUS, left=DimensionCellRead[…]…]`; `AbsenceMarkerEdgesTest.the_graph_of_the_golden_is_the_verifiers() ✘`:
  `Dependencies` gaf 8 van de 26 randen van `verifier --dependency-edges` (de negatieve randen van de waardetoetsen,
  de positieve naar `vindloon naar taxatie` en `getaxeerd totaal` en de strikte naar `kostbare vondst` ontbraken).
- **Punt 3 op main** (`logs/03-…`, `logs/04-red-gate16-main.log`): de lowering van de golden gaf
  `ERROR (corpus/golden/jutterswet-afwezigheidsmarkers/scenario.rgs:17): syntaxfout: no viable alternative at input
  '-detaxatievanV1isonbekend'` en `scenario: de verwachting '- de taxatie van V1 is onbekend' is syntactisch niet
  herkend` (negen regels); in `ScenarioAbsenceMarkerTest` vier tests rood op RGS-216 en
  `a_marker_the_attribute_does_not_declare_is_refused() ✘ expected: <[RGS-377 {…}]> but was: <[]>`.
- **De objectcreatie** (`logs/11-red-creation.log`, op `b3f15f29b`, waar de schrijvers van een creatie als op main
  stonden): `a_creation_writes_a_value_as_a_construct() ✘ Unexpected type, expected: <…Expr.Construct> but was:
  <…Expr.Literal>`; de verifier op die emissie: `WF-023.datatype-family-mismatch FAIL
  /rules/objectCreations/0/versions/0/initialisations/0: a number value is assigned to a sumtype target`, zonder
  diagnostiek van de frontend.

### Mutaties (niet gecommit; één `git worktree add --detach`, opgeruimd; `logs/10-mutations/`, `logs/14-mutations/`)

| mutatie | rood |
|---|---|
| M1 een lezing van de waarde niet als `PayloadAccess` | `the_model_carries_the_nodes_of_the_sum_type`, `a_read_without_the_test_is_refused`, `a_comparison_before_the_test_is_refused`, `a_sum_without_the_subselection_is_refused`; de emissie van de golden wijkt af |
| M2 een geschreven waarde niet in `Construct waarde` | `the_model_carries_…`, `a_marker_the_attribute_does_not_declare_stays_unresolved`; emissie wijkt af |
| M3 een verwijzing niet met het somtype getypeerd | `the_model_carries_…` (`expected: <SumType[…]> but was: <Numeric[…]>`) |
| M4 `Dependencies` zonder de arm `PayloadAccess` | `AbsenceMarkerEdgesTest` |
| M5 `foldSourceReference` niet door een `PayloadAccess` | eerst alleen de emissie (de suite groen); met `a_fold_over_the_values_reads_through_its_range` (`17c3537ac`) rood |
| M6 een waardetoets niet zijn eigen wacht | `a_comparison_before_the_test_is_refused` (twee RGS-376), `the_model_carries_…` (drie RGS-376) |
| M7 een lezing van een cel niet als `PayloadAccess` | `a_read_of_a_cell_is_refused` |
| C1 de waarde van een objectcreatie niet gewikkeld | `a_creation_writes_a_value_as_a_construct` |
| S1 een scenariowaarde niet als payload van `waarde` | `a_marker_and_a_value_are_constructor_values_of_the_case_file`; de cases van de golden wijken af |
| S2 elke marker toegelaten | `a_marker_the_attribute_does_not_declare_is_refused`, `a_marker_on_an_attribute_without_markers_is_refused` |
| S3 `CaseEmitter` zonder payload | `a_marker_and_a_value_are_…`; de cases van de golden wijken af |
| S4 de woorden van een marker niet geclaimd | `the_words_of_a_marker_value_are_keywords` (`kind=WORD, text=toepassing`), `SemanticTokensCorpusTest.every_golden_test_document_is_covered_and_resolved` |
| G1 de grammatica leest elk woord als marker | `a_word_that_is_no_marker_stays_a_syntax_error` (RGS-377 in plaats van RGS-216) |

### Codes, pins en tests (oud → nieuw)

- Nieuw **RGS-377** `scenario-absence-marker-not-admitted` (ERROR, parameters `admitted`, `marker`, `what`), met de
  delen `PART.absence-markers.none` en `PART.absence-markers.only`, in beide catalogi: "het attribuut 'keurwaarde':
  'onbekend' past hier niet als afwezigheidsmarkering: de declaratie laat alleen 'niet van toepassing' toe
  (RUNTIME-SEMANTICS ch. 9.5)". Register 366 codes; geen vloer verschoof (`EXPECTED_MIN_RGS_CODES=347`,
  `EXPECTED_MIN_RGS_MESSAGES=363`). Geen andere code, geen lijst of vloer van een gate veranderde.
- Test die de kaart omdraait: `AbsenceMarkerTest.a_marker_the_attribute_does_not_declare_stays_unresolved` eiste
  `assertTrue(m.absenceMarkerSites.isEmpty())`; de zijtabel is weg, dus nu: de waarde van de regel is `Construct waarde`
  over de onopgeloste naam (`Expr.Unresolved`), dus geen marker. `ScenarioLoweringTest` hoeft niet te veranderen:
  `the_copy_carries_every_set_of_the_model` telt nu 25 verzamelingen (vloer 22), het rijke document houdt zijn attribuut
  met `(of onbekend)`.
- Gemeten: frontend-suite 1515 tests, 0 skipped (was 1505); gate 16 41 goldens en 146 case files (was 40 en 136); gate
  10 197 rgs-goldens; gate 15 routepoot 197 goldens, 79 unit tests.

### Voorgestelde tekst (huidige stand)

**CONSTRUCTS rij 47**, in de kolom v0→v1 delta, na "… an aggregation over members with markers needs a subselection on
the value (decided 2026-11-04)." toevoegen: "A scenario document writes a marker as the value of such an attribute in
a situation and an expectation, as a rule writes it, lowered to the constructor value of the case format; a marker the
target does not declare is refused (RGS-377)." In de laatste kolom "golden `jutterswet-afwezigheidsmarkers`" →
"golden `jutterswet-afwezigheidsmarkers` (with its `scenario.rgs`, gate 16)". Rij 36: geen wijziging (de frontend
schrijft `SumType`, `Construct` en `PayloadAccess`, nu uit zijn model).

**CLAUDE.md** gate 10 en 16: geen wijziging; de tekst noemt de zijtabel niet, en de vloeren blijven ondergrenzen
(gate 16: 41 ≥ 7 en 146 ≥ 31).

### Wat stap B moet doen (gemeten 2026-11-05, `probes/cell-*.rgs`)

1. **Een marker die een regel op een cel schrijft.** "Het gewicht in voorjaar van een Vondst moet gesteld worden op
   onbekend" (op `het gewicht Numeriek (getal met 2 decimalen) gedimensioneerd met seizoen (of onbekend)`) geeft
   `WARNING (line 11): onopgelost: de naam 'onbekend' is geen gedeclareerde parameter, geen variabele uit een 'Daarbij
   geldt:'-deel en geen attribuut van het onderwerp (§11/§13.3.8) — 'onbekend' blijft als marker op de wire staan`, en
   de IR `Construct waarde(Unresolved "onbekend")` op een `DimensionCellRead`-doel (de verifier laat haar toe). Op
   main en na stap A gelijk. Te bouwen: `RuleChecker.absenceMarkerValue` neemt een cel als doel (het somtype van haar
   attribuut).
2. **De toetsen van een cel** ("indien zijn gewicht in najaar een waarde heeft", "… in voorjaar onbekend is") geven
   elk `WARNING … onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp (§8.1.7/§8.1.8)`:
   `absenceMarkerTest` neemt alleen een `AttributeReference`. De wacht van `AbsenceMarkers` heeft een sleutel zonder
   labels (een cel heeft nu sleutel null, dus altijd RGS-376), en WF-127 kent geen wacht voor een base anders dan een
   attribuutreferentie (VB-5 van `absence-markers`): een arm in de verifier (beschermd).
3. **Een waarde in een cel** (`Construct waarde(v)` op een celdoel, `probes/cell-value-write.*`): verifier PASS, ref
   `oracle: PASS (2 expectations)`, beide Drools-branches `PLAYED 1`; clingo `TRANSLATION FAULT: a cellValue
   expectation of waarde(5) is not checked here: this encoding carries a cell of a number, a date, a text, an
   enumeration or a boolean`, zonder registercode vooraan (gate 6 zou zo'n golden niet kunnen lijsten).
4. **Een marker in een objectcreatie** ("met taxatie gelijk aan onbekend") blijft een onopgeloste naam (RGS-092, IR
   `Construct waarde(Unresolved)`); niet gevraagd, wel de volgende vorm van dezelfde soort.
5. **Punt 2** (de kopie van een tijdlijn met markers in clingo en Drools): niet aangeraakt.
6. **Voor de pin-bump van de distributie:** `ScenarioValue` heeft twee componenten erbij (`constructor`, `payload`), en
   een waarde van een attribuut met markers is nu een constructorwaarde. `SituationToRequest.valueToJson` valt daarvoor
   door naar `NF.textNode(w.text())` met `w.text()` null, zonder fout; `ScenarioData.value` gooit `uchars resolver gaf een waarde
   zonder variant`. Beide moeten de constructorwaarde lezen.

### Voorlopige besluiten

- VB-A1 (waar de knopen ontstaan): een marker en de waardetoets bouwt de resolver waar hij de regel leest; elke andere
  lezing, de wikkel van een geschreven waarde en de typering met het somtype in een slotpas over het afgemaakte model.
  Reden: de checks van `RuleChecker` (ongeveer zeventig patronen op `AttributeReference`) zien zo de verwijzing zoals
  voorheen, dus geen diagnostiek verschuift; een lezing in een geschreven waarde of een conditie is pas na de hele check
  zeker een lezing van de waarde.
- VB-A2 (typering): in het afgemaakte model draagt elke verwijzing naar een attribuut met markers het somtype, elke
  `PayloadAccess` het gedeclareerde datatype; `ObjectType.attributes` houdt het payloaddatatype en `attributeMarkers`
  de declaratie, zoals de andere zijkaarten van een lid (granulariteit, dimensies).
- VB-A3 (cel): een lezing van een cel is een `PayloadAccess` over de cel en blijft RGS-376 (zoals op main); de rest van
  de cel is stap B.
- VB-A4 (objectcreatie): ook een objectcreatie wikkelt de waarde in `Construct waarde`, zoals een regel en een default;
  dat was een gat (WF-023 in de verifier, stil in de frontend).
- VB-A5 (spelling in een toetsdocument): de woorden van de regels op de plaats van een waarde, op hun tekst gelezen;
  de marker is één sleutelwoordspan. Een cel of een segment van een attribuut met markers krijgt de constructorvorm
  zoals elke andere waarde ervan.
- VB-A6 (één code): RGS-377 voor elke marker zonder caseform (niet gedeclareerd, attribuut zonder markers, parameter);
  het deel `admitted` zegt welke van de drie.
- VB-A7 (`Dependencies`): een `PayloadAccess` leest zijn base, zoals `dep_expr_at` van de verifier.

### Vragen

1. **WF-062 en een getal dat een regel in een attribuut met markers schrijft** (gemeten, `probes/literal-write.*`):
   "De taxatie van een Vondst moet gesteld worden op 5,00" geeft `WF-062.argument-type-mismatch FAIL
   /rules/rules/0/versions/0/value: argument type number(0 decimals) does not match the declared payload number(2
   decimals)`, terwijl ref (`oracle: PASS`), clingo (`clingo: PASS`) en Drools (`PLAYED 1`) de case spelen en de
   frontend niets meldt; zo sinds stap A van `absence-markers`, en nu ook voor een objectcreatie. De frontend typeert
   de literal naar zijn waarde (5,00 als geheel getal) en de toekenning laat hem toe; WF-062 eist het payloadtype
   precies. Opties: (a) WF-062 leest het argument als een toekenning (WF-023 met de precisie), een wijziging van spec
   en verifier (beschermd); (b) de frontend typeert een literal-argument met het payloadtype (lost alleen literals op:
   een lezing van een smaller attribuut blijft geweigerd); (c) de frontend weigert de zin (RGS), wat een geldige zin
   afwijst. Aanbeveling: (a).

### make gates

Eén run aan het eind in de worktree op `17c3537ac` (`out/gates/20261105-102904/`, log `gates-1.log`); de commit daarna
raakt alleen deze kaart. Gate 10, 15 en 16 liepen daarvoor ook los groen (`logs/09-gate-*.log`, op `93f854e39`).

```
start 2026-11-05 10:29:04 17c3537ac card/absence-markers-residue
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 403 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 2s drools-jar: rebuilt (2 source path(s) newer than the jar, …; 2s)
01-self-contained rc=0 2s     02-ir-schema rc=0 161s    04-case-schema rc=0 9s    03-verifier rc=0 49s
05-oracle rc=0 28s            06-clingo rc=0 334s       07-noop rc=0 16s          08-red rc=0 204s
09-catala rc=0 6s             10-regelspraak rc=0 294s  11-toka-parity rc=0 282s  12-drools rc=0 416s
13-reftype rc=0 378s          14-flogic rc=0 3s         15-graph rc=0 61s         16-scenario rc=0 44s
17-readings rc=0 318s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-05 10:36:02 418s red=0
```

Gate 10: `JUnit suite green (1515 tests, floor 173); 197 rgs golden(s) re-emit byte-identically`, `check-rgs-codes:
PASS — 366 codes, 393 sites`. Gate 16: `41 scenario golden(s) (floor 7) regenerate 146 case file(s) (floor 31) and
their ir.json byte-identically`. Gate 12 en 17 ongewijzigd groen (de Drools-backend las de golden al van de draad).

## Result van stap B (cel, objectcreatie, kopie van een tijdlijn, WF-062)

*Bouwer stap B, 2026-11-05. Niet gemerged, niet gepusht. Logs `werk/absence-markers-residue/logs/b*.log` en
`gates-b1.log`, proeven onder `werk/absence-markers-residue/probes/b/`, scripts onder `werk/absence-markers-residue/scripts/`
(`square.py`, `readings.py`, `mutations-b.py`, `copy_golden.py`, `wf062_negatives.py`; buiten de repo). Alle vier
punten van de opdracht zijn gebouwd; geen punt bleef een weigering.*

### Commits

| hash | inhoud |
|---|---|
| `3d2d4dcbb` | verifier (beschermd, voorstel): WF-062 leest het argument van een `Construct` als toekenning, WF-127 kent een cel; negatieven `wf062-argument-incompatible-units`, `wf062-argument-precision-exceeds`, `wf062-argument-precision-unbounded`, `wf127-cell-guarded-by-another-cell` |
| `52fc6298b` | frontend: een marker op een cel (schrijven, toetsen, de wacht van RGS-376 met de labels) en in een objectcreatie; `AbsenceMarkerTest` +3; DIVERGENCES |
| `3e7a3fdfb` | clingo: de payload per cel, een cel als somoperand, de verwachting van een cel met een constructorwaarde, de kopie van een somtijdlijn; Drools: de kopie op beide branches; `test_clingo` (de kopie speelt), README van Drools |
| `317237418` | corpus: goldens `jutterswet-afwezigheidsmarkers-cel` en `-creatie` (met `source.rgs` en `scenario.rgs`) en `-kopie` (met de hand); ref-suite +11 uitgewerkte voorbeelden (`WV-9.5-cel-*`, `-creatie-*`, `-kopie*`) |
| `6f9b03bf2` | gate 12 (beschermd, voorstel): `EXPECTED_PLAYED` 341 → 344 |
| `482ddf03b` | IR-SPEC ch. 2.1, 4.7 en de index (WF-062, WF-127), RUNTIME-SEMANTICS ch. 9.4 en 9.5, ASP-TRANSLATION (beschermd, voorstel) |
| (deze) | Result |
| (laatste) | kaart naar `done/` |

### Wat er veranderde

- **Punt 1, WF-062 als toekenning** (vraag 1 van stap A, optie (a)). De verifier kruist het argument van een
  `Construct` met de payload zoals WF-023 een schrijfactie kruist: `wf023_assignment` (naar vóór de
  expressiewandeling verplaatst, ongewijzigd) geeft familie, eenheid, teken en keuze- en structdomein; elk gebrek
  daarvan is de ene arm `WF-062.argument-type-mismatch` (parameters `argument`, `payload`, zin "argument type … is not
  assignable to the declared payload …"), en de tekenarm van WF-039 op een literal gaat ongewijzigd door. Op de
  schrijfplaatsen van WF-023 (de waarde van een regelversie, een lid van een objectcreatie, een defaultsgevolg) krijgt
  het argument van de geschreven `Construct` de precisiepoort van WF-023 tegen de decimalen van de payload
  (`payload_gate` in `wf023_precision`): de nieuwe armen `WF-062.argument-precision-exceeds` (`argumentDecimals`,
  `payloadDecimals`) en `WF-062.argument-precision-unbounded` (`payloadDecimals`), aan het argument. "De taxatie van
  een Vondst moet gesteld worden op 5,00" en "met taxatie gelijk aan 12,50" zijn verifier-schoon.
- **Punt 2, een marker op een cel.** `RuleChecker.absenceMarkerValue` en `absenceMarkerTest` nemen een
  `DimensionCellRead` zoals een attribuutreferentie (`markedAttribute`): een marker op een celdoel is de `Construct`
  van de marker, "zijn gewicht in najaar een waarde heeft" een gevuldtoets van de `PayloadAccess` over de cel, "… in
  voorjaar onbekend is" een gelijkheid van de cel met de `Construct`. De sleutel van de wacht van `AbsenceMarkers`
  draagt de labels van de cel, dus een toets van een cel bewaakt de lezing van dezelfde cel en van geen andere. WF-127
  (`payload_base_key`): twee bases zijn hetzelfde als ze dezelfde attribuutlezing zijn, of een `DimensionCellRead` met
  dezelfde labels over zo'n lezing. clingo: een `PayloadAccess` over een cel leest het lid
  `"gewicht[najaar]#waarde"`, waarvoor `payload_projections` per cel van het product van de gedeclareerde labels een
  projectieregel schrijft; `sum_term` leest een cel als somoperand van een vergelijking; `Model.check` controleert een
  `cellValue`-verwachting met een constructorwaarde via `ctor_value_term`. De invoer van een cel met een
  constructorwaarde en het schrijven van `Construct waarde(v)` in een cel speelden al. ref en beide Drools-branches
  speelden de cel zonder wijziging (gemeten, onder).
- **Punt 3, een marker in een objectcreatie.** `makeMemberInit` leest een marker als waarde van een lid met markers,
  van een attribuut en van een cel, vóór het de waarde als expressie bouwt; de typeringslus van de creatie slaat een
  marker over (hij is geen waarde van het gedeclareerde datatype).
- **Punt 4, de kopie van een tijdlijn met markers** (ref's fixture `copy-of-a-timeline`). clingo
  (`translate_period_arithmetic`): op een somtijdlijndoel is naast een constructor van een literal een eigen
  tijdsafhankelijk attribuut van een somtype zonder pad de waarde, gelezen per periode op schaal 0. Drools
  (`periodArithmetic`, `periodMember`): op een somtijdlijndoel is een eigen tijdsafhankelijke attribuutreferentie een
  kopie, heel gelezen zoals een eigen tijdlijn in het rekenen per periode, geknipt op de periodes van de voorwaarde en
  geschreven met `PeriodArithmetic.writtenValues`. Elke andere producent blijft geweigerd: clingo met de zin "… is
  written with a constructor of a literal or a copy of an own timeline of a sum type; another producer … is not
  translated (no corpus case)" (zonder code, zoals voorheen), Drools met DROOLS-303.
- **Corpus.** Drie goldens: `jutterswet-afwezigheidsmarkers-cel` (vier cases: een waarde in de cel in najaar uit de
  schatting, onbekend in voorjaar dat de invoer verdringt, de toetsen en de lezing onder de toets, onbekend in de
  invoer), `-creatie` (vier cases: een creatie met onbekend en met 12,50, een getal 5,00 in een attribuut met markers
  dat ook een invoer onbekend verdringt) en `-kopie` (drie cases: de kopie, de kopie geknipt op "gedurende de tijd dat
  hij verkocht is", en de invoer van de kopie op de dagen waarop de bron leeg is). De kopie heeft geen bron: het
  dialect geeft elk attribuut met markers een eigen domein (VB-3 van `absence-markers`), dus een bron kan geen
  somtijdlijn in een ander attribuut kopiëren.

### Rood eerst (letterlijk)

Op main `fc347f1f3` (een losse worktree `werk/absence-markers-residue/red-main`, gebouwd; `logs/b03-red-main.log`,
`logs/b05-red-frontend-main.log`):

- **Punt 1** (de verifier op de emissie van `-creatie`): `WF-062.argument-type-mismatch FAIL /rules/rules/0/versions/0/value:
  argument type number(0 decimals) does not match the declared payload number(2 decimals)` en `… FAIL
  /rules/objectCreations/1/versions/0/initialisations/0/value: argument type number(1 decimals) does not match the
  declared payload number(2 decimals)`; ref, clingo en beide Drools-branches speelden dezelfde IR (`PASS`, `PLAYED 4`).
- **Punt 2, de frontend** op `-cel/source.rgs`: `WARNING (…:19): onopgelost: de naam 'onbekend' is geen gedeclareerde
  parameter …`, `WARNING (…:25): onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp … 'zijn
  gewicht in najaar een waarde heeft' blijft als marker op de wire staan` (ook regel 30 en 36) en twee keer `ERROR
  (…:24/37): 'zijn gewicht in najaar' leest de waarde van 'gewicht' van Vondst … zonder eerst te toetsen` (RGS-376);
  `AbsenceMarkerTest.a_marker_is_written_and_tested_on_a_cell() ✘ onverwachte diagnostiek: [ERROR (line 17): …,
  WARNING (line 12): onopgelost …]`. **De verifier** op de emissie van de nieuwe frontend: `WF-127.unguarded FAIL
  /rules/rules/2/versions/0/value/left` en `… /rules/rules/4/versions/0/condition/parts/1/left`. **clingo** op elke case:
  `TRANSLATION FAULT: a payload access is translated over an attribute read (no corpus case for more)`, zonder code.
  ref `oracle: PASS` en beide Drools-branches `PLAYED 4` op dezelfde IR (de verifier daarvoor in de nieuwe stand).
- **Punt 3** op `-creatie/source.rgs`: `WARNING (…:29): onopgelost: de naam 'onbekend' …`; op die emissie ref
  `EVALUATION FAULT: EF-019: Unresolved reached in evaluation`, clingo `TRANSLATION FAULT: CLINGO-006: a Construct
  payload other than a literal …`, beide Drools-branches `ERROR ComputeFault: error in the rule "onbekende vondst":
  Unresolved reached in evaluation`; `a_creation_writes_a_marker_as_a_construct() ✘ onverwachte diagnostiek: [WARNING
  (line 21): onopgelost …]`.
- **Punt 4** op `-kopie`: verifier `PASS`, ref `PASS` op de drie cases, clingo `TRANSLATION FAULT: the sum-typed
  timeline "opslagkopie" is written with a constructor of a literal; another producer of a constructor value per
  period is not translated (no corpus case)`, beide Drools-branches `EXCLUDE DROOLS-303: … ('zijn opslagkosten') …`.
- Op main groen, en pas na de bouw betekenisvol: `a_read_of_a_cell_after_the_test_of_another_cell_is_refused` (op main
  werd elke lezing van een cel geweigerd); zijn mutatie F3 hieronder maakt hem rood.

### Mutaties (niet gecommit; in de losse worktree op `482ddf03b`, opgeruimd; `logs/b10-mutations/`, `b11`, `b12`)

| mutatie | rood |
|---|---|
| V1 WF-062: het argument weer precies het payloadtype | de golden `-creatie`: `WF-062.argument-type-mismatch FAIL /rules/rules/0/versions/0/value: argument type number(0 decimals) is not assignable …` |
| V2 geen precisiepoort op het argument | `wf062-argument-precision-exceeds` en `-unbounded` `passed the verifier` |
| V3 WF-062 typeert het argument nooit fout | `wf062-payload-type-mismatch` en `wf062-argument-incompatible-units` `passed the verifier` |
| V4 de sleutel van een cel zonder labels | `wf127-cell-guarded-by-another-cell` `passed the verifier` |
| V5 geen sleutel voor een cel | de golden `-cel`: `WF-127.unguarded FAIL /rules/rules/2/versions/0/value/left` |
| F1 `absenceMarkerValue` niet op een cel | `a_marker_is_written_and_tested_on_a_cell`, `a_creation_writes_a_marker_as_a_construct` (RGS-092) |
| F2 `absenceMarkerTest` niet op een cel | `a_marker_is_written_and_tested_on_a_cell` (RGS-376 en twee RGS-092) |
| F3 de wachtsleutel van `AbsenceMarkers` zonder labels | `a_read_of_a_cell_after_the_test_of_another_cell_is_refused`: `verwachtte één RGS-376, kreeg: []` |
| F4 geen marker in een creatie gelezen | `a_creation_writes_a_marker_as_a_construct` (RGS-092) |
| F5 de typering van een creatie leest de marker | idem: `objectcreatie: toekenning: a sumtype value is assigned to a number target` |
| C1 geen projectie per cel | `-cel/case1`: `MISMATCH: V1.najaarsloon is underived (leeg), expected 12000`, ook case3 |
| C2 geen verwachting van een cel met een constructorwaarde | elke case van `-cel`: `TRANSLATION FAULT: a cellValue expectation of waarde(60) is not checked here …` |
| C3 geen cel als somoperand | elke case van `-cel`: `TRANSLATION FAULT: CLINGO-013: a sum-typed operand here is …` |
| C4 de kopie geweigerd | elke case van `-kopie`: `TRANSLATION FAULT: the sum-typed timeline "opslagkopie" is written with …` |
| C5 de kopie op de schaal van een getal gelezen | elke case van `-kopie`: `TRANSLATION FAULT: unscalable datatype in the ASP translation`; `test_clingo: 2637 checks, 1 FAILURES` |
| D1 de kopie geweigerd | `-kopie` op beide branches: `EXCLUDE DROOLS-303` |
| D2 `periodMember` laat geen somtype door | idem |
| D3 de kopie geschreven als getallen | `-kopie` op beide branches: `ERROR ConsequenceException: … kan geen Breuk maken van …` |

### Codes, pins en tests (oud → nieuw)

- **Verifier:** nieuwe armen `WF-062.argument-precision-exceeds` en `WF-062.argument-precision-unbounded`; de zin van
  `WF-062.argument-type-mismatch` "… does not match the declared payload …" → "… is not assignable to the declared
  payload …" (sleutel en parameters gelijk). Gate 3: 452 goldens schoon (was 449), 628 negatieven op hun regel (was
  624), 473 op hun arm (was 469), 262 armsleutels; geen vloer verschoof.
- **Geen RGS-, CLINGO-, DROOLS- of EF-code nieuw of gepensioneerd.** De zin van CLINGO-013 (een fallback) noemt nu ook
  een cel; de zin van de kopie-weigering in clingo noemt de kopie. Registers ongewijzigd (RGS 366, DROOLS 314 met 25
  retired, CLINGO 35).
- **Gate 12:** `EXPECTED_PLAYED` 341 → 344 (de drie goldens, `PLAYED 4`, `4`, `3` op beide branches); het kanaal
  ongewijzigd (48 goldens bij naam). Geen andere lijst of vloer veranderde.
- **Test die de kaart omdraait:** `test_clingo.ml`, "absence markers: a copy of a timeline with markers": was
  `ref_matches` plus `faults_with … ~needle:"is written with a constructor of a literal"`, nu `both_answer` (ref en
  clingo geven hetzelfde antwoord). De javadoc van `AbsenceMarkerTest.a_read_of_a_cell_is_refused` zegt nu "without a
  test of that cell" in plaats van "no test of a cell's value is lowered"; de bewering is dezelfde.
- **Gemeten:** frontend-suite 1518 tests, 0 skipped (was 1515); ref-suite 2468 (was 2446); clingo-suite 2637
  (gelijk: twee controles werden één `both_answer`, dat er twee telt); Drools-suite 1857, 0 skipped (gelijk; de
  corpusgetuigen lopen de nieuwe goldens mee). Gate 5 1102 cases (was 1091), gate 6 1098 (was 1087), gate 10 199
  rgs-goldens (was 197), gate 16 43 goldens en 154 case files (was 41 en 146), gate 17 345 goldens en 973 case files
  vergeleken (was 342 en 962), 0 afwijkingen; de lezingen van ref en beide branches waren vooraf per case gelijk
  (`scripts/readings.py`, ook op de kopie, waarvan "verkoopkopie" een voorwaarde per periode heeft).

### Voorgestelde tekst (huidige stand)

**IR-SPEC** ch. 2.1 (WF-062 uit de lijst van regels die twee datatypes heel vergelijken), ch. 4.7 (WF-062, WF-127) en
de index (WF-062, WF-127); **RUNTIME-SEMANTICS** ch. 9.4 (de kopie) en 9.5 (een cel, de wacht van een cel, "Executed",
de uitgewerkte voorbeelden `WV-9.5-cel-*`, `-creatie-*`, `-kopie*`); **ASP-TRANSLATION** (de payload per cel, de
kopie): in `482ddf03b`.

**CONSTRUCTS rij 36**, in de kolom v0→v1 delta: "a sum-typed timeline carried in ref, the ASP backend and the Drools
backend for the input, a write of a constructor of a literal and a comparison per period" → "a sum-typed timeline
carried in ref, the ASP backend and the Drools backend for the input, a write of a constructor of a literal, a copy of
an own timeline of the same sum type and a comparison per period"; en achteraan: "**The argument as a write
(2026-11-05, card `absence-markers-residue`):** WF-062 reads the argument of a `Construct` as a write into the declared
payload, with WF-023's assignment typing and, at WF-023's write positions, its precision gate; WF-127 guards the read of
a cell by a test of the same cell. Executed in ref, the ASP backend and both branches of the Drools backend (goldens
`jutterswet-afwezigheidsmarkers-cel`, `-creatie`, `-kopie`)."

**CONSTRUCTS rij 47**, in de kolom v0→v1 delta, na "… a marker the target does not declare is refused (RGS-377).":
"On a dimensioned attribute with markers a rule writes a marker into a cell and a condition tests a cell for a marker
and for a value, and the value of a cell is read under the test of that cell (WF-127, RGS-376); an object creation
writes a marker into a member as a rule writes it." In de laatste kolom: "golden `jutterswet-afwezigheidsmarkers`
(with its `scenario.rgs`, gate 16)" → "goldens `jutterswet-afwezigheidsmarkers`, `-cel` and `-creatie` (each with its
`scenario.rgs`, gate 16) and `-kopie`", "play scalar and per period" → "play scalar, per cell, in an object creation and
per period", en "2026-11-04, card `absence-markers`." → "2026-11-04, card `absence-markers`; 2026-11-05, card
`absence-markers-residue`."

**CLAUDE.md**, gate 3: na "… WF-127 holds a `PayloadAccess` under a test of its constructor (IR-SPEC ch. 4.7), with
one negative per arm." toevoegen: "Since the card `absence-markers-residue` (2026-11-05) WF-062 reads the argument of
a `Construct` as a write into its payload — WF-023's assignment typing, and at WF-023's write positions its precision
gate — and WF-127 guards the read of a cell by a test of the same cell." Gate 12: "on a timeline target of a sum type a
rule writes a constructor of a literal in the periods of its condition" → "on a timeline target of a sum type a rule
writes a constructor of a literal, or a copy of an own timeline of that sum type, in the periods of its condition".
Geen vloer verschuift; `EXPECTED_PLAYED` staat niet in CLAUDE.md.

### Voorlopige besluiten

- VB-B1 (WF-062, de typering): één arm `argument-type-mismatch` voor elk gebrek van de toekenningstypering (familie,
  eenheid, teken, keuze- en structdomein), niet de armen van WF-023 onder WF-062 herhaald; de tekenarm van WF-039 op
  een literal gaat ongewijzigd door, omdat die een eigen regel is.
- VB-B2 (WF-062, de precisie): de poort op het argument alleen waar de `Construct` zelf de geschreven waarde is op een
  schrijfplaats van WF-023; een `Construct` elders (in een vergelijking, een tak) schrijft niets en krijgt alleen de
  typering. Reden: de precisiepoort is een poort op een schrijfactie, en ref legt een payload neer zoals hij is
  (`World.collapse` past geen constructorwaarde aan de decimalen).
- VB-B3 (de wacht van een cel): de sleutel van een cel is die van haar attribuutlezing met haar labels, genormaliseerd,
  in de frontend en de verifier gelijk; een toets van een cel bewaakt alleen die cel.
- VB-B4 (clingo, de payload per cel): een projectieregel per cel van het product van de gedeclareerde labels, niet
  alleen voor de cellen die het document leest, zodat elke lezer het lid vindt.
- VB-B5 (de kopie): clingo en Drools nemen alleen de kopie van een eigen tijdsafhankelijk attribuut zonder pad; WF-023
  houdt het domein van de bron gelijk aan dat van het doel. Geen nieuwe code: de andere producenten houden hun
  weigering (clingo zonder code, zoals voorheen; Drools DROOLS-303).
- VB-B6 (drie goldens): de cel, de creatie (ook de getuige van punt 1) en de kopie apart, elk met één onderwerp; de
  golden `jutterswet-afwezigheidsmarkers` bleef ongewijzigd.

### Vragen

Geen normvraag open. Eén waarneming voor de hoofdsessie: de weigering van een andere producent van een somtijdlijn in
clingo blijft een vertaalfout zonder registercode (geen golden raakt haar), zoals vóór de kaart; gate 6 kan zo'n
golden niet in `EXPECTED_CHANNEL` zetten. Een code van het soort `refusal` volgt zodra een golden erom vraagt.

### make gates

Eén run in de worktree op `482ddf03b` (`out/gates/20261105-111200/`, log `gates-b1.log`); de commits daarna raken alleen
deze kaart.

```
start 2026-11-05 11:12:00 482ddf03b card/absence-markers-residue
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 403 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 565 source files)
01-self-contained rc=0 2s     02-ir-schema rc=0 145s    04-case-schema rc=0 7s    03-verifier rc=0 44s
05-oracle rc=0 26s            06-clingo rc=0 321s       07-noop rc=0 16s          08-red rc=0 186s
09-catala rc=0 5s             10-regelspraak rc=0 284s  11-toka-parity rc=0 264s  12-drools rc=0 412s
13-reftype rc=0 385s          14-flogic rc=0 2s         15-graph rc=0 53s         16-scenario rc=0 46s
17-readings rc=0 303s
after-run rc=0 1s the three builds fresh, no product newer than the end of the build step
end 2026-11-05 11:18:53 413s red=0
```

Gate 12: `played 344` op beide branches, JUnit 1857. Gate 3 meldt in zijn log één regel `scripts/check-verifier.sh:
line 237: printf: write error: Broken pipe` naast `PASS`; die stond niet in de run van stap A. Regel 237 schrijft de
uitvoer van de verifier met `printf` in een pijp naar `grep -q`, dat bij de eerste treffer stopt; de status van de pijp
is die van grep, dus geen bewering hangt aan de regel (niet verder onderzocht).
