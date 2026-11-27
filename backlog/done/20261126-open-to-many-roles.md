# open-to-many-roles: open versus gesloten `0..n`-collecties in strikte regelsets

*Ontparkeerd en aangescherpt op 2026-11-27 na landing van `open-to-one-roles` (`a0e2ff077`) en afronding van de praktijktoetsing `open-collections-impact-analysis` (`948c78ec9`), met expliciet mandaat van de eigenaar voor de geraakte beschermde paden (`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`, `spec/schema/`, `spec/verifier/`, `scripts/`).*

Goal: in een strikt document (`inferFromEmpty: false`, `Afleiden uit leeg: nee`) gedraagt elke niet-wederkerige `to-many`-rol (`Role.many == true` op een binair `FactType` met `reciprocal == false`, zoals `de vondsten` van een `Jutter`) zich per casusinstantie als een verzameling met bekende ondergrens: **open** ($S_{\text{known}} \subseteq S_{\text{actual}}$: er kunnen nog niet-aangeleverde elementen bijkomen, ook wanneer al bekende elementen gekoppeld zijn) tenzij de roldeclaratie `"complete": true` (`(volledig gegeven)`) draagt of de casus de rol op die instantie in `"rolesClosed"` als gesloten/volledig aanmerkt ($S_{\text{known}} = S_{\text{actual}}$, inclusief de vaststaand lege collectie `[]`), met monotone kortsluiting voor kwantoren en roltoetsen en zonder intervalrekenkunde of gepredicateerde deelsluiting.

## Ontwerpbesluiten (vastgelegd bij ontparkeren)

1. **Standaardstand & `(volledig gegeven)` (`Role.complete` & `WF-130`):**
   - Onder `inferFromEmpty: false` is elke niet-wederkerige rol (`reciprocal: false`), zowel `to-one` als `to-many`, op een casusinstantie waar geen creatie haar schrijft standaard **`open`** tenzij haar roldeclaratie `"complete": true` draagt of de casus haar sluit (`rolesEmpty` voor `to-one`, `rolesClosed` voor `to-many`).
   - `Role.complete` wordt toegelaten op elke rol van een niet-wederkerig `FactType` (`reciprocal: false`), dus óók op `many: true`; `WF-130` weigert `complete: true` uitsluitend nog op de rol van een wederkerig feittype (`WF-130.reciprocal-role`, negatieve getuige `corpus/negative/wf130-reciprocal-role/`; `wf130-to-many-role` vervalt).
   - Op een door een `ObjectCreation` gemaakte instantie geeft de casus geen invoer: een niet-wederkerige `to-many`-rol op een gemaakte instantie waar geen creatie een link legt of open laat, is vaststaand gesloten (`[]`), net zoals een `to-one`-rol daar vaststaand afwezig is.
2. **Lokale tweerichtingssamenhang & `rolesClosed`:**
   - Elke gerichte rolpositie `(x, factType, far)` op een niet-wederkerig feittype wordt lokaal beoordeeld op de gematerialiseerde links (ch. 5.7a, dus inclusief links die via de tegenrol zijn aangeleverd). In tegenstelling tot `to-one` sluit een aanwezige link een `to-many`-rol niet: zonder `complete: true` en zonder vermelding in `rolesClosed` blijft `(x, factType, far)` open bovenop haar gematerialiseerde links $S_{\text{known}}$.
   - Sluiting van `J1.vondsten` (`rolesClosed` of `complete: true`) sluit uitsluitend de rolpositie op `J1` en verandert niets aan een losse tegeninstantie `V2` waarvan `V2.vinder` open is (`V2.vinder` blijft open: zij kan in een uitbreiding vaststaand afwezig worden of aan een andere jutter met open `vondsten` gekoppeld worden).
   - In `uchar-case-v0` staat `"rolesClosed"` direct na `"rolesEmpty"` (vóór `"characteristics"`). Anders dan `rolesEmpty` botst `rolesClosed` **niet** met aanwezige links op dezelfde rolpositie: samen met $S_{\text{known}}$ legt zij vast dat $S_{\text{actual}} = S_{\text{known}}$ (met 0 links dus $S_{\text{actual}} = \emptyset$).
3. **Universele bereikwet over `(M, first_open_rel)` (monotoon over alle open bereikstappen):**
   - De bereikwandeling van een `RoleMembership`, `Aggregation` of `QuantifierFold` levert de vaststaand bereikte instanties $M = [m_1, \dots, m_n]$ (gefilterd op `counterpartType` / `elementType`) én de eerste in ch. 5.11-volgorde (breedte-eerst selectie-, stap- en lijstvolgorde) aangetroffen open relatiestap `first_open_rel : (holder, far) option` — ongeacht of die open stap een open `to-one`-rol, een open `to-many`-rol of een open creatie is (een open tussenstap zou anders een al vaststaande `atLeast 1`-kortsluiting verliezen zodra een nieuw tussenobject met een open vervolgstap wordt gekoppeld!):
     - **`RoleMembership` (`hij heeft een <rol>` / `hij heeft geen <rol>`):** bij $M \neq []$ staat het bestaan vast (`true` positief, `false` ontkennend), óók als de relatie open is; bij $M = []$ en `first_open_rel = Some r` zijn beide polariteiten `undetermined` met `open r`; bij $M = []$ en `first_open_rel = None` staat het niet-bestaan vast (`false` positief, `true` ontkennend).
     - **Waardevouwen (`Aggregation`: `count`, `filledCount`, `sum`, `min`, `max`, `first`, `last`):** geen intervalrekenkunde; zodra `first_open_rel = Some r` breekt de waardevouw direct af met `open r` (geen contributors, geen deposits, geen lidfouten).
     - **Kwantorvouwen (`QuantifierFold` met filter $F$ en matrix $T$):**
       - Bij `first_open_rel = None` (gesloten bereik): evalueert exact zoals voorheen over $n = |M|$ leden.
       - Bij `first_open_rel = Some r_range` (open bereik: er kunnen nog een willekeurig aantal extra elementen op willekeurige posities in het bereik bijkomen, $u = \infty$):
         - op de bekende bereikte leden $M$ worden eerst alle filters gelezen, met één cruciale monotonie-eis: omdat de vouw op een open bereik nog onbepaald kan eindigen (waarbij §9.5-deposits binnen de voorwaardegrens vervallen) en omdat tussen bekende leden nog eerdere leden kunnen komen die de matrix-kortsluiting eerder stoppen, worden onder `first_open_rel = Some r_range` ook filter-deposits vastgehouden tot vaststaat dat de `QuantifierFold` een vaststaand oordeel bereikt (eindigt de `QuantifierFold` als `undetermined`, dan vervallen haar filter-deposits!) én wordt elke matrix op $M$ **voorlopig (`provisionally`, met `past = Some r_range`)** gelezen (matrix-deposits vervallen, een matrix-fout maakt dat lid `undetermined` zonder de sessie te breken);
         - kortsluiting onder $u = \infty$:
           - `all` / `the`: vaststaand `false` zodra $\ge 1$ lid $F = \text{true} \land T = \text{false}$ heeft; anders `undetermined`;
           - `none`: vaststaand `false` zodra $\ge 1$ lid $F = \text{true} \land T = \text{true}$ heeft; anders `undetermined`;
           - `atLeast k`: vaststaand `true` wanneer $k \le 0$ of zodra $\ge k$ leden $F = \text{true} \land T = \text{true}$ hebben; anders `undetermined`;
           - `atMost k` en `exactly k`: vaststaand `false` wanneer $k < 0$ of zodra $\ge k + 1$ leden $F = \text{true} \land T = \text{true}$ hebben; anders `undetermined`;
         - blijft de `QuantifierFold` onder `first_open_rel = Some r_range` `undetermined`, dan draagt zij als open read `r_range` (`open <holder> "<farRoleName>"`, conform ch. 5.12), met in haar lezing de `contributors [...]` van de bereikte bekende leden in $M$ (geen contributors wanneer $M = []$).
4. **Scenariosyntaxis in `.rgs` (`Situatie`, zonder nieuwe lexer-keywords):**
   - Open collectie met 0 bekende elementen: rol weglaten.
   - Open collectie met $k \ge 1$ bekende elementen: `- de|het <rol-mv> zijn <LABEL>, ... en <LABEL>`.
   - Gesloten lege collectie (0 elementen): `- de|het <rol-mv> zijn leeg` → schrijft `<rol>` in `rolesClosed` (en niets in `roles`).
   - Gesloten gevulde collectie ($k \ge 1$ elementen): `- de|het <rol-mv> zijn <LABEL>, ... en <LABEL> (volledig gegeven)` → schrijft de labels in `roles` én `<rol>` in `rolesClosed`.

## Acceptance

- **IR-schema, `IR-SPEC.md` & verifier (`spec/schema/uchar-ir-v1.schema.json`, `spec/IR-SPEC.md` ch. 2.6, `spec/verifier/`, `scripts/check-ir-schema.sh`):**
  - `Role.complete` toegestaan op alle niet-wederkerige rollen (`many: false` en `many: true`); `WF-130` controleert alleen nog `not ft.reciprocal` (`WF-130.reciprocal-role`); `corpus/negative/wf130-to-many-role/` verwijderd.
- **Caseschema (`spec/schema/uchar-case-v0.schema.json`, `scripts/check-case-schema.sh`) & case-weigerregels (identiek in `ref`, `clingo` en `Drools`):**
  - `$defs.instance` krijgt direct na `rolesEmpty` (vóór `characteristics`) de optionele array `"rolesClosed"` van niet-lege strings (met zelf-test en kanonieke sleutelvolgorde).
  - Validatie van elk element `key` in `rolesClosed` op instantie `label` van type `otype`:
    1. onbekende rolnaam in model → `EF-040` (`label`, `name`);
    2. onbereikbare rol vanaf `otype` → case-invoerweigering zonder code;
    3. dubbelzinnige rol vanaf `otype` → `EF-001` (`roleKey`, `objectType`);
    4. `far.many == false` of `ft.reciprocal == true` → case-invoerweigering zonder code (`rolesClosed` geldt uitsluitend voor een niet-wederkerige `to-many`-rol);
    5. dubbel noemen binnen `rolesClosed` (enkelvoud/meervoud) is idempotent; aanwezige links op hetzelfde rolslot zijn geldig.
- **Runtime-semantiek (`spec/RUNTIME-SEMANTICS.md` ch. 1.5, 5.7, 5.12, 5.12a), `ASP-TRANSLATION.md` (S20) & uitvoerders (`ref`, `clingo`, `Drools`):**
  - `ref`, `clingo` en `Drools` (zowel `PatternDslRendering` als `DrlRendering` + `RecordingRendering` en `SessionDump`) voeren de open/gesloten `to-many`-standen, de universele bereikwet hierboven en de `link ... open`-regels in `EndState` (ch. 5.12a, naast bestaande concrete links!) exact gelijk uit.
  - `Input.roleClosed(bean, role)` / `DroolsBackend.insert` krijgt een spiegel van `Input.roleEmpty` met weigeringscode **`DROOLS-377`** (`INSERT_CLOSED_ROLE`, `Kind.CONTRACT`: niet-in-invoer, niet-gedeclareerd/onbereikbaar, dubbelzinnig, of niet een niet-wederkerige `to-many`-rol; gekoppelde elementen op het bean-veld zijn toegestaan).
- **RegelSpraak-frontend (`frontends/regelspraak/`):**
  - **Model:** `(volledig gegeven)` op `rolSpecificatie` toegestaan op elke niet-wederkerige rol (`één` en `meerdere`); `RGS-474` weigert alleen nog op wederkerige feittypen; `RGS-475` weigert op elke door creatie geschreven rol.
  - **Scenariolaag:** `- de|het <rol-mv> zijn leeg` en `- de|het <rol-mv> zijn <LABEL>, ... (volledig gegeven)` in `RegelSpraak.g4`, `ScenarioResolver`, `CaseEmitter`, `SemanticTokens` en `DIVERGENCES.md`:
    - `zijn leeg` weigert op attributen/cellen, op `to-one`-rollen en wederkerige rollen (`RGS-477` / eigen arm), met een periode (`RGS-478`), bij attribuut/rol-ambiguïteit (`RGS-476`), en bij een link op hetzelfde rolslot binnen hetzelfde situatieblok vanuit eigen of tegenrichting (`RGS-479`);
    - `zijn <LABEL>, ... (volledig gegeven)` weigert op een wederkerig feittype (`RGS-477`) én wanneer binnen hetzelfde situatieblok vanuit een tegeninstantie een label op dat rolslot wordt gekoppeld dat niet in de `(volledig gegeven)`-lijst op het onderwerp staat (`RGS-479` / eigen foutmelding);
    - wederzijdse `gebaseerd op`-overschrijving tussen open lijst, `(volledig gegeven)`-lijst, `zijn leeg` en tegenkoppelingen (zodat een latere `zijn leeg` of `(volledig gegeven)`-lijst geërfde tegenkoppelingen buiten de nieuwe set ontkoppelt, en een latere tegenkoppeling een geërfde sluiting openbreekt tenzij het latere blok opnieuw sluit);
    - waarschuwing (`RGS-480`) buiten `ir.json` in een gewoon document zonder `Afleiden uit leeg: nee` wanneer `zijn leeg` (zonder gewiste geërfde koppeling) of `(volledig gegeven)` op een roltoewijzing niets verandert.
- **Gate 5 (`scripts/check-oracle.sh`), corpus & alle 17 gates:**
  - `--closed-inputs` sluit ook alle niet-wederkerige `to-many`-invoerrollen.
  - Monotonie (`drop_inputs` + `lost`): laat ook `rolesClosed`-vermeldingen op openbare `to-many`-invoerrollen weg én laat casuslinks op niet-wederkerige feittypen weg wanneer beide einden openbare en niet-gesloten invoerrollen zijn; verscherpt `open_on` zodat een nieuwe link tussen twee casusinstanties vereist dat in de gereduceerde eindtoestand beide rolposities open waren.
  - Bestaande strikte goldens met `to-many`-feittypen die op geslotenheid leunen (`jutterswet-keurder-strikt`, `jutterswet-zwaarkeurder-strikt`, `jutterswet-invoer-rol-strikt`) dragen `(volledig gegeven)` op die `to-many`-rollen zodat hun bestaande getuigenrol intact blijft; nieuwe Jutterswet-goldens (`jutterswet-invoer-collectie-strikt` en `jutterswet-invoer-collectie`) dekken open vs. gesloten collecties van kop tot staart over alle 17 poorten (`make gates` `red=0` + `scripts/check-self-contained.sh` `PASS`).

May touch:
- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`
- `spec/schema/uchar-ir-v1.schema.json`, `spec/schema/uchar-case-v0.schema.json`, `spec/verifier/`
- `ref/`, `backends/clingo/`, `backends/drools/`, `frontends/regelspraak/`
- `corpus/golden/`, `corpus/negative/wf130-to-many-role/`, `corpus/red/`
- `scripts/` (`check-ir-schema.sh`, `check-case-schema.sh`, `check-oracle.sh`, `check-drools.sh`, `check-drools-codes.sh`, `check-rgs-codes.sh` waar zelf-tests, cataloguslijsten of pinnen meebewegen)
- deze kaart (`backlog/new/20261126-open-to-many-roles.md` → `doing/` → `done/`)

Out of scope:
- gedeeltelijke of gefilterde sluiting van collecties (bewuste non-goal)
- intervalrekenkunde op waardevouwen
- wederkerige feittypen
- `regelspraak-toolchain` (eigen vervolgkaart `20261126-open-to-many-roles-in-service.md` na de uchar-pin)

## Result (2026-11-27)

- **IR-schema, `IR-SPEC.md` & verifier (`spec/`, `corpus/negative/`):**
  - `Role.complete` (`"complete": true`) toegestaan op alle niet-wederkerige binaire rollen (`many: false` én `many: true`) in `uchar-ir-v1.schema.json`, `IR-SPEC.md` ch. 2.6 en `spec/verifier/`; `WF-130` versmald naar `WF-130.reciprocal-role` (`corpus/negative/wf130-to-many-role/` verwijderd, `wf130-reciprocal-role/` behouden).
- **Caseschema, runtime-semantiek, `ref` & `clingo` (`spec/`, `ref/`, `backends/clingo/`, `scripts/`):**
  - optionele array `"rolesClosed"` direct na `"rolesEmpty"` op `$defs.instance` in `uchar-case-v0.schema.json` en `RUNTIME-SEMANTICS.md` (ch. 1.5, 5.7, 5.12, 5.12a), met `EF-040` (onbekende rolnaam), `EF-001` (dubbelzinnige rol), case-invoerweigering op onbereikbare, `to-one`- en wederkerige rollen, idempotentie op dubbele vermeldingen en geldigheid naast gekoppelde elementen ($S_{\text{actual}} = S_{\text{known}}$);
  - universele bereikwet over `(M, first_open_rel)` geïmplementeerd in `ref` (`4808` worked examples) en `clingo` (`ASP-TRANSLATION.md` S20, `5211` checks): niet-wederkerige `to-many`-rollen op casusinstanties zonder creatieschrijver blijven standaard open bovenop aangeleverde koppelingen (gemaakte instanties zonder open creatie blijven gesloten), `RoleMembership` beslist op $M \neq []$ en breekt af bij $M = [] \land \text{open}$, waardevouwen inclusief `InstanceSetUnion` breken direct af op `first_open_rel`, `QuantifierFold` sluit monotoon kort onder $u = \infty$ met gebufferde filter-deposits, voorlopige matrixlezing (`past = first_open_rel`) en ch. 5.12-bijdragers, en `EndState` schrijft `link ... open` naast concrete `link`-regels;
  - Gate 5 (`scripts/check-oracle.sh`) uitgebreid met `--closed-inputs`-sluiting van `to-many`-invoerrollen en monotonie-mutaties die `rolesClosed` én koppelingen op niet-gesloten openbare rollen weglaten (`3656` runs); bestaande strikte getuigen met gesloten `to-many`-rollen (`jutterswet-keurder-strikt`, `jutterswet-zwaarkeurder-strikt`, `jutterswet-invoer-rol-strikt`) dragen `"complete": true` / `(volledig gegeven)`.
- **RegelSpraak-frontend (`frontends/regelspraak/`):**
  - `(volledig gegeven)` toegestaan op niet-wederkerige `meerdere`-rollen (`RGS-474` versmald naar wederkerige feittypen, `RGS-475` verbreed naar elke door creatie geschreven rol inclusief meervoudskoppelingen);
  - `- de|het <rol-mv> zijn leeg` en `- de|het <rol-mv> zijn ... (volledig gegeven)` als bronvormen voor `"rolesClosed"` in `RegelSpraak.g4`, `ScenarioResolver`, `CaseEmitter`, `SemanticTokens` en `grammar/DIVERGENCES.md`, met `RGS-476`..`RGS-480` en wederzijdse `gebaseerd op`-overschrijving tussen open lijsten, gesloten lijsten, `zijn leeg` en tegenkoppelingen.
- **Drools-backend, corpus & poorten (`backends/drools/`, `corpus/golden/`, `spec/CONSTRUCTS.md`, `scripts/`):**
  - `OpenableMembers`, `CaseInput` (`rolesClosed`), `Input.roleClosed` met **`DROOLS-377`** (`INSERT_CLOSED_ROLE`, `377` codes), en behoud van `OpenInput` en `EndState.openLinks` op niet-gesloten `to-many`-rollen ook naast aanwezige koppelingen;
  - universele bereikwet op `PatternDslRendering` (`EXEC_MODEL`), `DrlRendering` (`DRL_TEXT`) en `RecordingRendering` (`ROLE_COUNT_OPEN`, `RangeOpen` op `to-many`-stappen, directe afbreking in `beforeFoldOutcome`, en `Composite.openRange` / `quantifierVerdictOverRange` / `quantifierReadOverRange` / `quantifierReadUnorderedOverRange` met gebufferde filter-deposits in `Term.FoldRead`), gedekt door `ClosedRoleInputWitnessTest` en `OpenReadingsWitnessTest` (`2100` JUnit-tests groen);
  - twee nieuwe Jutterswet-goldens met `source.rgs` en `scenario.rgs`: `jutterswet-invoer-collectie-strikt` (7 cases) en `jutterswet-invoer-collectie` (4 cases); `EXPECTED_PLAYED` in `scripts/check-drools.sh` verhoogd van `400` naar `402` op beide takken (`value-divergence 0`; Gate 17 `403` goldens / `1143` cases met `0` deviaties; alle 17 poorten + 3 builds + `after-run` groen met `red=0`).
