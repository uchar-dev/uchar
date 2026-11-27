# role-and-existence-expectations: verwachtingen voor niet-bestaan, roltelling, roldoel en open rollen in scenariotoetsen

*Aangemaakt op 2026-11-26 en aangescherpt bij oppakken op 2026-11-27 tegen `uchar` `c0f5cf20f` (na landing van `open-to-one-roles` en `open-to-many-roles`), met expliciet mandaat van de eigenaar voor de geraakte beschermde paden (`spec/schema/`, `spec/RUNTIME-SEMANTICS.md`, `scripts/`) en het besluit om deze kaart te richten op de vier kernverwachtingen op bestaande labels (`roleEmpty`, `roleCount`, `roleTarget`, `roleOpen`) en globale `instanceCount` en genavigeerde `rolePath`-verwachtingen buiten scope te houden.*

Goal: een scenariotoets en een `uchar-case-v0`-casus kunnen op een bestaand casuslabel rechtstreeks toetsen dat een rol vaststaand leeg is (`roleEmpty`), vaststaand precies $n$ gekoppelde instanties telt (`roleCount`), naar een specifiek casuslabel wijst (`roleTarget`) of onder `Afleiden uit leeg: nee` openstaat (`roleOpen`), zodat (niet-)creatie door `objectcreatie` of `feitcreatie` én open/gesloten invoerrollen rechtstreeks toetsbaar zijn zonder tel-hulpregels in het domeinmodel.

## Ontwerpbesluiten (vastgelegd bij oppakken)

1. **Kernscope (4 expectation kinds op bestaande casuslabels; `instanceCount` en `rolePath` geparkeerd):**
   - `roleEmpty` (`label`, `role`): voor elke rol (`to-one`, `to-many` of wederkerig). Slaagt dan en slechts dan als het gerichte rolslot op `label` op het fixpunt **niet open** is én **0** gekoppelde doelinstanties heeft (noch casusinstanties, noch door `objectcreatie` aangemaakte instanties).
   - `roleCount` (`label`, `role`, `count`, geheel getal $\ge 0$): voor elke rol (`to-one`, `to-many` of wederkerig). Slaagt dan en slechts dan als het gerichte rolslot op `label` op het fixpunt **niet open** is én precies `count` gekoppelde doelinstanties telt (inclusief door `objectcreatie` aangemaakte instanties en door `feitcreatie` gelegde links).
   - `roleTarget` (`label`, `role`, `target`): uitsluitend voor een niet-wederkerige `to-one`-rol (`far.many == false`, `reciprocal == false`), waarbij `target` een bestaand casuslabel is waarvan het type `far.objectType` vult (`⊑`). Slaagt dan en slechts dan als het gerichte rolslot op `label` op het fixpunt gekoppeld is aan casusinstantie `target` (op een `to-one`-rol impliceert dat automatisch dat het rolslot niet open is; is de rol open, leeg, gekoppeld aan een ander label of gekoppeld aan een naamloos aangemaakt object, dan faalt `roleTarget`).
   - `roleOpen` (`label`, `role`): voor elke rol. Slaagt dan en slechts dan als het gerichte rolslot op `label` op het fixpunt **open** is (d.w.z. een `link ... open`-regel draagt in de eindtoestand van hoofdstuk 5.12a: een open `to-one`-invoerrol, een open `to-many`-invoerrol — óók wanneer er al $\ge 1$ bekende elementen gekoppeld zijn! — of een door een `objectcreatie` of `feitcreatie` met onbepaalde voorwaarde open gelaten rol). In een document zonder `inferFromEmpty: false`, en op de ruwe invoer van `--noop` vóór regelsevaluatie, is geen enkel rolslot open en houdt `roleOpen` nergens stand (analoog aan `attributeOpen` en `characteristicOpen`).

2. **Case-validatie bij laden (identiek in `ref`, `clingo` en `Drools`):**
   - `label` moet een in de casus gedeclareerde instantie zijn (anders case-laadfout zonder `EF-*`-code, zoals bij attribuut- en kenmerkverwachtingen).
   - `role` lost vanaf `label`'s `objectType` op naar de verre rol `far` van een feittype waarvan `near` het type van `label` dekt (op enkelvoudige `Role.name` of gedeclareerde `Role.plural`; op een wederkerig feittype op de ene rol): lost zij op naar 0 rollen of naar $\ge 2$ rollen, dan faalt het laden met een case-laadfout zonder `EF-*`-code (zoals bij een onbekend of niet-passend verwachtingsattribuut).
   - Voor `roleTarget` wordt bovendien bij laden gecontroleerd dat `far` een niet-wederkerige `to-one`-rol is, dat `target` een in de casus gedeclareerde instantie is, en dat het type van `target` een subtype (`⊑`) van `far.objectType` is (anders case-laadfout zonder `EF-*`-code).
   - Voor `roleCount` is `count` een niet-negatief geheel getal (`0, 1, 2, ...`).

3. **Scenariotaal in `.rgs` (`Met als verwacht resultaat:`, zonder nieuwe lexer-keywords):**
   - Volledige geschreven naam heeft voorrang (achterwaarts compatibel met bestaande attributen zoals `het aantal penningbesluiten` in `jutterswet-penningbesluit`):
     - Raakt de geschreven naam (zonder lidwoord) zowel een attribuut/cel als een rol vanaf het type van `<LABEL>`, weiger dan met `RGS-476` (ambiguïteit, spiegel van `Situatie`). Raakt zij meerdere rollen, weiger dan met `RGS-247`.
     - **Lege rol (`roleEmpty`):**
       - `- de|het <rol> van <LABEL> is leeg` op een niet-wederkerige `to-one`-rol;
       - `- de|het <rol-mv> van <LABEL> zijn leeg` op een `to-many`- of wederkerige rol;
       - verkeerd werkwoord (`is leeg` op `to-many`/wederkerig, `zijn leeg` op `to-one` of op een attribuut/cel) → `RGS-477` / passende weigering.
     - **Roldoel (`roleTarget`):**
       - `- de|het <rol> van <LABEL> is <LABEL2>` op een niet-wederkerige `to-one`-rol (`RGS-266`/`RGS-267` bij onbekend of type-onverenigbaar doel `<LABEL2>`, en eigen/passende RGS-fout op een attribuut, `to-many`- of wederkerige rol).
     - **Open rol (`roleOpen`, alleen in een document met `Afleiden uit leeg: nee`, anders `RGS-467`):**
       - `- de|het <rol> van <LABEL> staat niet vast` (op een `to-one`-rol) en `- de|het <rol-mv> van <LABEL> staan niet vast` (op een `to-many`-rol; verkeerd werkwoord enkelvoud/meervoud tegen `many` geweigerd zoals bij `is`/`zijn`, wederkerige rol geweigerd of alleen indien ondersteund).
     - **Roltelling (`roleCount`) en telling-staat-niet-vast (`roleOpen`) wanneer de volledige naam géén attribuut/cel of rol noemt en het lidwoord + naam opent met `het aantal <rest>`:**
       - lost `<rest>` vanuit `<LABEL>` op naar een rol (enkelvoud of meervoud; bij rol-ambiguïteit `RGS-247`):
         - `- het aantal <rol-mv> van <LABEL> is <n>` (waarbij `<n>` een niet-negatief geheel getal zonder eenheid en zonder periode is) → `roleCount` (`label`, `role`, `count`); draagt de waarde een eenheid, decimalen, een negatief getal, een niet-getal of een periode, dan volgt een eigen RGS-foutcode;
         - `- het aantal <rol-mv> van <LABEL> staat niet vast` → `roleOpen` (`label`, `role`), onder `RGS-467` geweigerd wanneer het document geen `Afleiden uit leeg: nee` draagt.

## Acceptance

- **Schema & semantiek (`spec/schema/uchar-case-v0.schema.json`, `scripts/check-case-schema.sh`, `spec/RUNTIME-SEMANTICS.md` ch. 1.5 & ch. 5.7):**
  - De 4 nieuwe expectation kinds (`roleEmpty`, `roleCount`, `roleTarget`, `roleOpen`) staan in `uchar-case-v0.schema.json` met kanonieke sleutelvolgorde en zelf-tests in `scripts/check-case-schema.sh`, en hun laadeisen en fixpunt-waarheidsvoorwaarden staan in `RUNTIME-SEMANTICS.md` (met worked examples in `test_ref.ml`).
- **Uitvoerders (`ref`, `clingo`, `Drools`):**
  - `ref` (inclusief `--noop`), `clingo` (`#show role/3.` en `#show ropen/2.` waar nodig + `model.ml`) en `Drools` (`CaseRunner` op zowel `EXEC_MODEL` als `DRL_TEXT`, met heldere mismatch-rendering) laden, valideren en evalueren alle 4 de rolverwachtingen identiek.
- **Scenariolaag (`frontends/regelspraak/`):**
  - `RegelSpraak.g4`, `ScenarioResolver`, `ResolvedScenario`, `CaseEmitter`, `SemanticTokens` en `grammar/DIVERGENCES.md` ondersteunen de nieuwe vormen en weigeringen zonder enige regressie op bestaande `.rgs`- en scenario-goldens.
- **Corpusgetuigen & poorten:**
  - Bestaande `corpus/golden/jutterswet-penningbesluit/` blijft onaangeroerd (gepind door `WV-5.12a-penningbesluit`); twee nieuwe Jutterswet-goldens (`corpus/golden/jutterswet-rolverwachting/` en `corpus/golden/jutterswet-rolverwachting-strikt/`, met `source.rgs` en `scenario.rgs`) toetsen rechtstreeks op (niet-)creatie door `objectcreatie` en `feitcreatie` zonder tel-hulpattributen én op open/gesloten `to-one`- en `to-many`-rollen. *(Let op dat Drools creaties onder `inferFromEmpty: false` weigert met `DROOLS-370`: test in `jutterswet-rolverwachting-strikt` open/gesloten invoerrollen zodat ook Drools haar speelt in Gate 12 en Gate 17, en dek een open creatie met `roleOpen`/`roleEmpty`/`roleCount` af in `test_ref.ml` en `test_clingo.ml`!)*
  - Volledige `make gates` (17/17 poorten + builds + `after-run`, `red=0`) en `scripts/check-self-contained.sh` groen.

May touch:
- `spec/schema/uchar-case-v0.schema.json`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`
- `frontends/regelspraak/**`
- `ref/`, `backends/clingo/`, `backends/drools/`
- `corpus/golden/jutterswet-rolverwachting/`, `corpus/golden/jutterswet-rolverwachting-strikt/`
- `scripts/` (`check-case-schema.sh`, `check-drools.sh`, `check-rgs-codes.sh` waar zelf-tests, catalogi of `EXPECTED_PLAYED` meebewegen)
- deze kaart (`backlog/new/20261126-role-and-existence-expectations.md` → `doing/` → `done/`)

Out of scope:
- globale type-brede tellingen (`instanceCount` / `createdCount`) en genavigeerde eigenschappen op aangemaakte objecten (`rolePath`) — geparkeerd tot een concrete casus erom vraagt
- de werkbank (`werkbank/`, `v2/herleving-ww`, `eval/datasets/herleving-ww`)
- `regelspraak-toolchain` (vervolgkaart `20261126-role-and-existence-expectations-in-service.md` samen met `open-to-many-roles-in-service` in één pin-bump na deze uchar-landing)

## Result (2026-11-27)

- **Case-schema & normatieve semantiek (`spec/schema/uchar-case-v0.schema.json`, `scripts/check-case-schema.sh`, `spec/RUNTIME-SEMANTICS.md` ch. 1.5 & ch. 5.7, `spec/ASP-TRANSLATION.md` S20, `spec/CONSTRUCTS.md` rijen 37 & 48):**
  - Vier rolverwachtingen op bestaande casuslabels toegevoegd aan `$defs.expectation` met kanonieke eigenschapsvolgorde (`roleEmpty`: `kind, label, role`; `roleCount`: `kind, label, role, count`; `roleTarget`: `kind, label, role, target`; `roleOpen`: `kind, label, role`) en dekkende zelf-tests in `check-case-schema.sh` (`214` rijen).
  - Uniforme laadweigeringen zonder `EF-*`-code (onbekend `label`, niet-bestaande/onbereikbare of ambigue `role`, niet-gehele of negatieve `count`, en voor `roleTarget` een `to-many`- of wederkerige rol, onbekend `target`-label of type-onverenigbaar `target`) en fixpuntregels gespecificeerd: elke positieve rolverwachting controleert eerst dat het gerichte rolslot niet open is, waarna `roleEmpty`, `roleCount` en `roleTarget` het doelensemble op het fixpunt toetsen (waarbij naamloze aangemaakte instanties meegeteld worden in `roleCount`, `roleEmpty` uitsluiten, maar nooit matchen met `roleTarget`), en `roleOpen` precies dan slaagt wanneer het gerichte rolslot openstaat.
- **Uitvoerders (`ref/`, `backends/clingo/`, `backends/drools/`):**
  - `ref/lib/case.ml`: `expected_role` laadvalidatie, `role_state` op het fixpunt en onder `--noop` (ruwe casuslinks, nooit open), met worked examples `WV-5.7-rolverwachtingen` en `WV-1.5-rolverwachtingen-strikt` (inclusief open creatie) in `ref/test/test_ref.ml`.
  - `backends/clingo/`: `#show role/3.` en onder `inferFromEmpty: false` `#show ropen/2.` wanneer een rolverwachting aanwezig is, plus extractie en vergelijking in `model.ml` en tests in `test_clingo.ml`.
  - `backends/drools/`: `CaseRunner`, `DroolsBackend.compare` en `EndState` uitgebreid voor zowel `EXEC_MODEL` als `DRL_TEXT` met identieke laadvalidatie, fixpuntcontrole over `links` en `openLinks` (inclusief `ObjectCreation`, `FactCreation`, zelfrelaties en 1:1 wederkerigheid), `PART.mismatch.role` in `messages-drools.{nl,en}.tsv` en `RoleExpectationsWitnessTest`.
- **RegelSpraak-scenariolaag (`frontends/regelspraak/`):**
  - `RegelSpraak.g4` uitgebreid zonder nieuwe lexer-keywords (`( IS | ZIJN ) LEEG`, `verwachtRolDoel`, `( staatWoord | staanWoord ) NIET vastWoord`).
  - Tweelagige resolutie in `ScenarioResolver` en `SemanticTokens` (volledige geschreven naam vóór `het aantal <rest>`, achterwaarts compatibel met bestaande attributen zoals `het aantal penningbesluiten` in `jutterswet-penningbesluit`), emissie in `CaseEmitter`, diagnostiek (`RGS-476`, `RGS-477`, `RGS-481`..`RGS-483`, `RGS-247`, `RGS-266`, `RGS-267`, `RGS-467`), documentatie in `grammar/DIVERGENCES.md` en tests in `ScenarioStrictTest`, `ScenarioRecoveryTest`, `SemanticTokensScenarioTest` en `SemanticTokensRecoveryTest`.
- **Corpusgetuigen & poorten (`corpus/golden/jutterswet-rolverwachting/`, `corpus/golden/jutterswet-rolverwachting-strikt/`, `scripts/check-drools.sh`):**
  - Twee nieuwe rechtenvrije Jutterswet-goldens met `source.rgs`, `scenario.rgs`, `ir.json`, `case*.json` en DRL-tekstpinnen toegevoegd (`jutterswet-penningbesluit` ongewijzigd gelaten); `EXPECTED_PLAYED` in `scripts/check-drools.sh` verhoogd van `402` naar `404`.
  - Volledige `make gates` groen (`17/17` poorten + `3` builds + `after-run`, `red=0`) en `scripts/check-self-contained.sh` groen.
