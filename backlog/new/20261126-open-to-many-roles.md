# open-to-many-roles: open versus gesloten `0..n`-collecties in strikte regelsets (geparkeerd)

*Geparkeerd op 2026-11-26 na ontwerpverkenning. Pas oppakken nadat (1) `open-to-one-roles` is geland en (2) uit praktijktoetsing op een strikte regelset blijkt dat sluiting per volledige `0..n`-rol daadwerkelijk meerwaarde biedt boven kwantor-formulering en gerichte rubriekskenmerken in het model. Raakt beschermde paden (`spec/`, `scripts/`): vereist expliciet mandaat bij ontparkeren.*

Goal: in een strikt document (`inferFromEmpty: false`, `Afleiden uit leeg: nee`) kan een niet-wederkerige `to-many`-rol (`many: true` op `Role`, zoals `de vondsten` van een `Jutter`) per instantie **open** zijn ($S_{\text{known}} \subseteq S_{\text{actual}}$: er kunnen nog niet-aangeleverde elementen bestaan) of **gesloten/volledig** ($S_{\text{known}} = S_{\text{actual}}$, inclusief de vaststaand lege collectie `[]`), met monotone kortsluiting voor kwantoren en zonder intervalrekenkunde of gepredicateerde deelsluiting.

Acceptance (bij oppakken aanscherpen):
- **Geen gedeeltelijke of gefilterde sluiting in de engine:** sluiting geldt uitsluitend voor een volledige `(instantie, to-many-rol)`, nooit voor een predicaat-deelverzameling binnen een collectie (geen SMT-/subsumptieredeneren over afgeleide regelfilters). Onafhankelijk uitvraagbare deelrubrieken horen in het model een eigen rol of kenmerk te krijgen.
- **Model & IR (`spec/IR-SPEC.md`, `spec/schema/uchar-ir-v1.schema.json`, `spec/verifier/`):**
  - `(volledig gegeven)` op een `meerdere`-roldeclaratie zet `"complete": true` op die `Role`: op elke instantie is de aangeleverde lijst voor die rol dan standaard gesloten (geen vermelding = vaststaand nul elementen).
  - Weiger `(volledig gegeven)` op een rol die door een `ObjectCreation` of `FactCreation` geschreven kan worden (spiegel van `RGS-464`) en op een wederkerig feittype zolang dat buiten bereik blijft.
- **Case (`spec/schema/uchar-case-v0.schema.json`) & scenariolaag (`frontends/regelspraak/`):**
  - Een instantie kan per `to-many`-rol aangeven dat haar gekoppelde elementen in `roles` volledig zijn (`rolesClosed`: lijst van rolnamen; staat de rol in `rolesClosed` en niet in `roles`, dan staat vast dat er nul elementen zijn).
  - Scenariovorm in `Situatie` voor zowel een vaststaand lege collectie als een volledig opgesomde collectie (spelling bij ontparkeren vastleggen), inclusief overschrijving bij `gebaseerd op` en waarschuwing in een gewoon document.
- **Runtime-semantiek (`spec/RUNTIME-SEMANTICS.md` ch. 1.5 & ch. 5.12) & uitvoerders (`ref`, `clingo`, `Drools` op beide weergaven):**
  - **Kwantoren (`QuantifierFold` met filter $F$ en matrix $M$):** op een open collectie sluit `atLeast k` (`ten minste k`) monotoon kort naar `waar` zodra ten minste $k$ bekende elementen vaststaand aan $F \land M$ voldoen en blijft anders `onbepaald`; `none` (`geen`), `atMost k` (`ten hoogste k`) en `exactly k` (`precies k`) sluiten kort naar `onwaar` zodra meer dan $k$ (respectievelijk ten minste 1) bekende elementen vaststaand aan $F \land M$ voldoen en blijven anders `onbepaald`; `all` (`alle`) sluit kort naar `onwaar` zodra ten minste 1 bekend element vaststaand $F = \text{waar} \land M = \text{onwaar}$ heeft en blijft anders `onbepaald` (ook bij nul bekende elementen).
  - **Waardevouwen (`Fold`: `count`, `filledCount`, `sum`, `min`, `max`, `first`, `last`):** geen intervalrekenkunde; zolang de collectie open is, is elke waardevouw erover `open` (ook `het aantal`), met `open <object> <rol>` in de lezing van hoofdstuk 5.12.
  - **Roltoets (`hij heeft een <rol>` / `hij heeft geen <rol>`):** bij ten minste 1 bekend gekoppeld element is `heeft een` vaststaand `waar` en `heeft geen` vaststaand `onwaar`; bij nul bekende elementen op een open collectie zijn beide `onbepaald`.
- **Gate 5 (`scripts/check-oracle.sh`):**
  - Monotonie: zowel een element toevoegen aan een open collectie als een open collectie sluiten (`rolesClosed`) voegt alleen vaststaande uitkomsten toe en wijzigt er geen.
  - Behoud bij volledige invoer: met alle rollen en invoerleden gesloten geeft een strikt document exact de uitkomst van het gewone document.
- **Corpus & pariteit:** Jutterswet-goldens (bijv. klachten en vondsten van een jutter) en rode cases spelen gelijk in `ref`, `clingo` en beide Drools-takken; alle 17 gates groen.

May touch:
- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`
- `spec/schema/uchar-ir-v1.schema.json`, `spec/schema/uchar-case-v0.schema.json`, `spec/verifier/`
- `ref/`, `backends/clingo/`, `backends/drools/`, `frontends/regelspraak/`
- `corpus/golden/`, `corpus/negative/`, `corpus/red/`, `scripts/`
- deze kaart (`backlog/new/20261126-open-to-many-roles.md` → `doing/` → `done/`)

Out of scope:
- gedeeltelijke of gefilterde sluiting van collecties (bewuste non-goal)
- intervalrekenkunde op waardevouwen (`max(...) > grens` of `aantal(...) > 0` horen in strikte regelsets als `ten minste één`-kwantor geschreven te worden)
- wederkerige feittypen
- `regelspraak-toolchain` (eigen vervolgkaart `20261126-open-to-many-roles-in-service.md` na de uchar-pin)

## Beslispunten bij ontparkeren

1. **Standaardstand van `to-many`-rollen bij `Afleiden uit leeg: nee`:** wordt elke niet-gemarkeerde `to-many`-rol standaard *open* (volledig symmetrisch met attributen, kenmerken en `to-one`-rollen, wat betekent dat bestaande strikte goldens met `to-many`-rollen hun collecties in de case of met `(volledig gegeven)` sluiten waar zij op volledigheid leunen), of opt-in? Symmetrie pleit voor standaard *open* met `(volledig gegeven)`.
2. **Binaire samenhang tussen twee richtingen van één `Feittype`:** als op `Vondst V1` de `to-one`-rol `vinder` naar `Jutter J1` wijst, zit `V1` in $S_{\text{known}}$ van `J1.vondsten`; sluiting van `J1.vondsten` (`rolesClosed`) sluit tegelijk uit dat een losse `Vondst V2` waarvan `V2.vinder` open is nog aan `J1` gekoppeld zou zijn — of verandert die open `V2.vinder` dan niet? Let op wisselwerking met `open-to-one-roles`: zolang `V2.vinder` open is, kan `J1.vondsten` niet tegelijk bewezen gesloten zijn zonder `V2.vinder != J1` vast te leggen, tenzij links uitsluitend bij aanlevering vastliggen en `rolesClosed` op `J1` niet met een open tegenrol botst. Dit moet vóór de bouw wiskundig strak in hoofdstuk 1.5 staan.
3. **Scenariosyntaxis in `.rgs`:** exacte Nederlandse formulering in een `Situatie` voor "deze lijst is volledig" naast "er zijn er geen" (bijv. `- de vondsten zijn volledig: V1 en V2` en `- heeft geen vondsten` / `- de vondsten zijn leeg`).
