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
