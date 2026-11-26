# open-to-one-roles: open versus vaststaand lege `to-one`-rollen in strikte regelsets

*Uitgewerkt op 2026-11-26 tegen `uchar` `8d7a98c9b` (toolchain `a52c277e1`), met expliciet mandaat van de eigenaar voor de geraakte beschermde paden (`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`, `spec/verifier/`, `scripts/`, `spec/CONSTRUCTS.md`).*

Goal: in een strikt document (`inferFromEmpty: false`, `Afleiden uit leeg: nee`) gedraagt elke niet-wederkerige `to-one`-rol (`Role.many == false` op een binair `FactType` met `reciprocal == false` — zowel op `0..1`/`1..1`-feittypen als op de `to-one`-terugpijl van een `1..n`-feittype) zich vanuit het onderwerp 1-op-1 symmetrisch met een scalair attribuut:
1. Zonder aangeleverde koppeling is een ongeschreven `to-one`-rol op een casusinstantie **standaard `open`**, tenzij de roldeclaratie `"complete": true` (`(volledig gegeven)`) draagt of de casus de rol op die instantie in `"rolesEmpty"` als vaststaand leeg aanmerkt.
2. Begint een roltoets (`RoleCheck`), een genavigeerde attribuut- of kenmerklezing, of het `sourcePath` van een `Fold` / `QuantifierFold` met een open `to-one`-rol, dan breekt de evaluatie daar af op een *open read* (`open <object> "<rol>"` in ch. 5.12), en staat de open link in de eindtoestand van hoofdstuk 5.12a (`link "<fact type>" "<role>" <object> "<role>" open` / `... open "<role>" <object>`). Is die eerste schakel vaststaand afwezig (`rolesEmpty` of `complete: true`), dan geldt net als in hoofdstuk 1.2 en 5.8 vaststaand `leeg` / `onwaar` / lege range.
3. De RegelSpraak-frontend ondersteunt `(volledig gegeven)` op een `één`-rol in een `Feittype` én `- de|het <rolnaam> is leeg` in een `Situatie` van een scenariotoets.

Acceptance:
- **IR-schema, `IR-SPEC.md` & verifier (`spec/schema/uchar-ir-v1.schema.json`, `spec/IR-SPEC.md` ch. 2.6, `spec/verifier/`):**
  - `Role` krijgt het optionele kernveld `"complete": { "const": true }` (direct na `many`, vóór `origin`; geschreven `true` of weggelaten, exact zoals `Attribute.complete` en `Characteristic.complete`), meegenomen in verifier-AST, loader, round-trip/canonicalisatie (gate 3) en strip/permutatie van presentatie (gate 13).
  - Nieuwe welgevormdheidsregel `WF-130`: `complete: true` op een `Role` is alléén toegestaan op een niet-wederkerig `FactType` (`reciprocal: false`) waarvan die rol `many: false` heeft; `complete: true` op een rol met `many: true` of op een wederkerig feittype faalt in de verifier met negatieve getuigen onder `corpus/negative/wf130-*`.
- **Caseschema (`spec/schema/uchar-case-v0.schema.json`) & case-weigerregels (identiek in `ref`, `clingo` en `Drools`):**
  - `$defs.instance` krijgt direct na `roles` (vóór `characteristics`) de optionele array `"rolesEmpty"` van niet-lege strings; daarnaast krijgt `$defs.expectation` (indien nuttig naast `attributeOpen`/`characteristicOpen`) of de bestaande verwachtingsunies volledige dekking via `attributeOpen`/`characteristicOpen` en eindtoestand `5.12a` (en optioneel `roleOpen` met `label` en `role`, identiek in alle uitvoerders indien toegevoegd — anders testen via afgeleide `attributeOpen`/`characteristicOpen` én `5.12a`).
  - Resolutie en weigeringen van elk element `key` in `rolesEmpty` op instantie `label` van type `otype` (gelijk aan `roles` in `case.ml`):
    1. `key` noemt geen enkele gedeclareerde rol in het model → `EF-040` (`label`, `name`).
    2. `key` lost vanuit `otype` op naar 0 rollen → case-invoerweigering bij laden (zoals `roles`).
    3. `key` is vanuit `otype` dubbelzinnig over meerdere feittypen/rollen → `EF-001` (`roleKey`, `objectType`), identiek aan `roles`.
    4. De opgeloste verre rol (`far`) heeft `many: true` of hoort bij een wederkerig feittype → case-invoerweigering bij laden met reden (`rolesEmpty` geldt alleen voor een niet-wederkerige `to-one`-rol).
    5. Op dezelfde instantie én dezelfde gerichte rolpositie `(factType, nearRole -> self, farRole)` ligt in de casus óók al een link — hetzij via `self.roles`, hetzij via de omgekeerde rol op de gekoppelde tegeninstantie! — → nieuwe foutcode **`EF-049`** (`label`, `role`: de casus legt op een rol tegelijk een koppeling én `rolesEmpty` vast; kanaal `case input` in ch. 5.7b). Let op zelf-relaties (`Vergunning <-> Vergunning` met `vorige vergunning` en `volgende vergunning`): richtingen zijn onderscheiden op hun rolpositie, dus `V1` mag een `volgende vergunning` hebben terwijl haar `vorige vergunning` in `rolesEmpty` staat! Dubbel noemen van dezelfde rol binnen `rolesEmpty` is idempotent (zoals `attributesEmpty`).
- **Runtime-semantiek (`spec/RUNTIME-SEMANTICS.md` ch. 1.5, ch. 5.7, ch. 5.7b, ch. 5.12, ch. 5.12a) & `ASP-TRANSLATION.md` (S20):**
  - Onder `inferFromEmpty: false` is een niet-wederkerige `to-one`-rol (`far.many == false`) vanuit een casusinstantie `o` (waarvan het type `near.objectType` vult):
    - **gekoppeld** wanneer de casus of een creatie met vaststaand ware voorwaarde een link legt met `o` in `near`;
    - **open** wanneer (a) er een creatie met onbepaalde voorwaarde een link in `far` vanuit `o` zou kunnen leggen, óf (b) geen creatie deze richting voor het type van `o` schrijft, er geen link ligt in de casus, `far` niet `complete: true` is én `rolesEmpty` haar niet op `o` sluit;
    - **vaststaand afwezig** in alle overige gevallen (waaronder: een gemaakte instantie zonder toegekende link; `far` draagt `complete: true` en geen creatie laat haar open; `o` sluit `far` in `rolesEmpty` en geen creatie schrijft haar; of elke creatie die haar schrijft faalt vaststaand). Net als bij `attributesEmpty`/`complete` volgt een door een creatie geschreven rol haar schrijvers en verandert `rolesEmpty` of `complete: true` daar niets.
  - Navigatie over een pad `[r_1, ..., r_k]`, een `RoleCheck`, en een `Fold` of `QuantifierFold` waarvan het bereik (`sourceRole` of de eerste/volgende stap van `sourcePath`) vanuit een bereikt object een open `to-one`-rol leest: breekt op de eerste open schakel af als *open read* op `(dat object, die rol)`! (Bereikt stap 1 wél `B1` en is stap 2 op `B1` open, dan is de open read `(B1, r_2)`!)
  - Hoofdstuk 5.12 (`reading`-regels): `open <object> "<gedeclareerde rolnaam van far>"` (zoals nu al gespecificeerd voor een open relatie uit een creatie).
  - Hoofdstuk 5.12a (`state`-regels): `link "<fact type>" "<role0>" <object> "<role1>" open` respectievelijk `link "<fact type>" "<role0>" open "<role1>" <object>` voor elke open relatie op `<object>` (zowel uit onbepaalde creatie als uit open `to-one`-invoer!).
  - Bestaande `WV-1.5-link-*`-voorbeelden in spec, `test_ref.ml`, `test_clingo.ml` en `OpenValuesWitnessTest.java` dekken zowel vaststaand afwezig (`rolesEmpty` én `complete: true`) als `open` (weggelaten zonder `complete`).
- **Uitvoerders (`ref`, `clingo`, `Drools` op beide weergaven + `DroolsBackend.insert`):**
  - `ref` en `clingo` voeren alle regels hierboven exact uit (inclusief creaties en zelf-relaties).
  - `Drools` draagt open `to-one`-rollen op zowel `PatternDslRendering` als `DrlRendering`, inclusief meerstaps-paden, `RoleCheck` (beide polariteiten), `Fold`/`QuantifierFold` met een `to-one`-stap in `sourceRole`/`sourcePath`, defaults-stapels, `ConditionReading` (`NodeReading.open()` / `Fixpoint.OpenValue` of uitgebreide open-referentie die een rol eenduidig van een attribuut en kenmerk onderscheidt!), en `EndState` (`5.12a` `link ... open`-regels, zodat gate 17 100% regel-voor-regel gelijk blijft aan `ref --trace`!).
  - `Input.roleEmpty(bean, role)` / `DroolsBackend.insert` krijgt een spiegel van `Input.attributeEmpty` met weigeringscode `DROOLS-376` (`INSERT_EMPTY_ROLE`, `Kind.CONTRACT`: niet-in-invoer, niet-gedeclareerd/onbereikbaar, dubbelzinnig, `to-many`/wederkerig, of al gekoppeld via bean-veld — gecontroleerd op beide zijden waar relevant!).
- **RegelSpraak-frontend (`frontends/regelspraak/`):**
  - **Model (`RegelSpraak.g4`, `DefinitionResolver`, `RuleChecker`, `IrEmitter`, `SemanticTokens`):**
    - `rolSpecificatie` accepteert `VOLLEDIG_GEGEVEN?` achter `objectTypeNaam` (`de vinder Jutter (volledig gegeven)`).
    - Weigert `(volledig gegeven)` met eigen RGS-foutcodes op: (a) een `meerdere`-rol of de rol van een `Wederkerig feittype`, en (b) een `to-one`-rol die door een `ObjectCreation` of `FactCreation` geschreven wordt (spiegel van `RGS-464`).
    - Emit `"complete": true` op de `Role` in `ir.json`.
  - **Scenariolaag (`ScenarioResolver`, `CaseEmitter`, `SemanticTokens`, `DIVERGENCES.md`):**
    - `- de|het <rolnaam> is leeg` in een `Situatie` (ook met `gebaseerd op`) schrijft `<rolnaam>` in `rolesEmpty` (direct na `roles`, vóór `characteristics`).
    - Als `<naam>` op het objecttype van de instantie zowel een gedeclareerd attribuut als een bereikbare rol noemt → expliciete RGS-ambiguïteitsfout; als `<naam>` meerdere rollen vanuit dat objecttype noemt → bestaande/spiegelende rol-ambiguïteitsfout; als `<naam>` een `meerdere`-rol of wederkerige rol noemt of een `periode` draagt → eigen RGS-foutcode(s).
    - Binnen één situatieblok niet tegelijk `- de <rol> is <LABEL>` (of vanuit het tegenobject gekoppeld in hetzelfde blok!) én `- de <rol> is leeg` → eigen RGS-foutcode(s) zodat een geldig scenario nooit een ongeldige `EF-049`-case voortbrengt; bij `gebaseerd op` overschrijven een latere koppeling en een latere `is leeg` elkaar wél in beide richtingen (ook wanneer de geërfde koppeling op de tegeninstantie stond!).
    - In een gewoon document zonder `Afleiden uit leeg: nee` → waarschuwing (spiegel van `RGS-469` / `RGS-473`) wanneer `- de|het <rol> is leeg` geen geërfde koppeling uit een eerdere `Situatie` opheft.
- **Gate 5 (`scripts/check-oracle.sh`), corpus & alle 17 gates:**
  - Gate 5 toetst behoud (`--closed-inputs`: behandelt ook elke `to-one`-rol als `complete`) én monotonie (`--end-state`: een gekoppelde `to-one`-rol loskoppelen tot open, of een rol uit `rolesEmpty` weglaten tot open, houdt elke vaststaande uitkomst in de volledige eindtoestand overeind!).
  - Nieuwe strikte en gewone Jutterswet-goldens (met `source.rgs` en `scenario.rgs`, gate 10 en gate 16, o.a. 1-staps en 2-staps pad, roltoets, `(volledig gegeven)` op een rol, zelf-relatie `vorige vergunning`, en `gebaseerd op`-overschrijving) + negatieve getuigen (`wf130-*`) + rode casus voor `EF-049` (in zowel dezelfde als omgekeerde koppelrichting).
  - Volledige `make gates` (17/17 + builds + `after-run`, `red=0`) én `scripts/check-self-contained.sh` groen.

May touch:
- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`
- `spec/schema/uchar-ir-v1.schema.json`, `spec/schema/uchar-case-v0.schema.json`
- `spec/verifier/` (`lib/`, `bin/`, `test/`)
- `ref/` (`lib/`, `bin/`, `codes.tsv` indien nodig, `test/`)
- `backends/clingo/` (`lib/`, `codes.tsv` indien nodig, `test/`, `README.md`)
- `backends/drools/` (`model/`, `loader/`, `graph/`, `cases/`, `transpiler/`, `refusal/`, `message/`, `src/test/`, `README.md`)
- `frontends/regelspraak/` (`grammar/RegelSpraak.g4`, `grammar/DIVERGENCES.md`, `model/`, `resolver/`, `ir/`, `emit/`, `diagnostic-codes.tsv`, `message/`, `src/test/`)
- `corpus/golden/...`, `corpus/negative/wf130-...`, `corpus/red/...`
- `scripts/` (`check-schema.sh`, `check-case-schema.sh`, `check-oracle.sh`, `check-red.sh`, `check-drools.sh`, `check-drools-codes.sh`, `check-rgs-codes.sh`, `check-exec-codes.sh` waar zelf-tests, cataloguslijsten of pinnen meebewegen)
- `backlog/new/20261126-open-to-one-roles.md` → `doing/` → `done/`

Out of scope:
- open versus gesloten `to-many`-collecties (`20261126-open-to-many-roles.md`)
- wederkerige feittypen als open relatie
- creaties onder `inferFromEmpty: false` in Drools (`DROOLS-370` blijft ongewijzigd)
- `regelspraak-toolchain` (eigen vervolgkaart `20261126-open-to-one-roles-in-service.md` na de uchar-pin)
